# Fetch latest official Amazon Linux 2023 Minimal AMI for ARM64 (Graviton)
data "aws_ami" "al2023_arm64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-minimal-2023.*-arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_launch_template" "backend" {
  name_prefix   = "lunacare-${var.environment}-lt-"
  image_id      = data.aws_ami.al2023_arm64.id
  instance_type = var.instance_type

  iam_instance_profile {
    name = aws_iam_instance_profile.lunacare_profile.name
  }

  network_interfaces {
    associate_public_ip_address = false
    security_groups             = [aws_security_group.app.id]
  }

  # CRITICAL SECURITY REQUIREMENT: IMDSv2 Enforcement
  # Mitigates SSRF metadata exfiltration attacks
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    instance_metadata_tags      = "enabled"
  }

  # CRITICAL SECURITY REQUIREMENT: Encrypted Root EBS Storage
  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = 30
      volume_type           = "gp3"
      encrypted             = true
      delete_on_termination = true
    }
  }

  # Hardened User Data: Non-root process, systemd unit, and AWS Systems Manager agent
  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -euo pipefail

    # 1. Update OS packages and install security patches
    dnf upgrade -y --security

    # 2. Create unprivileged system user for application execution
    useradd --system --no-create-home --shell /sbin/nologin -u 1001 lunacare

    # 3. Create app directory with strict permissions
    mkdir -p /opt/lunacare
    chown -R lunacare:lunacare /opt/lunacare
    chmod 750 /opt/lunacare

    # 4. Systemd service definition with least-privilege sandboxing
    cat <<'SERVICE' > /etc/systemd/system/lunacare.service
    [Unit]
    Description=LunaCare Backend API Service
    After=network.target

    [Service]
    Type=simple
    User=lunacare
    Group=lunacare
    WorkingDirectory=/opt/lunacare
    ExecStart=/usr/bin/node /opt/lunacare/dist/server.js
    Restart=always
    RestartSec=5s

    # Security Sandboxing
    ProtectSystem=strict
    ProtectHome=true
    NoNewPrivileges=true
    PrivateTmp=true
    PrivateDevices=true
    ProtectKernelTunables=true
    ProtectControlGroups=true
    RestrictSUIDSGID=true

    [Install]
    WantedBy=multi-user.target
    SERVICE

    systemctl daemon-reload
  EOF
  )

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name = "lunacare-${var.environment}-instance"
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "backend" {
  name_prefix         = "lunacare-${var.environment}-asg-"
  vpc_zone_identifier = [aws_subnet.private_app_a.id, aws_subnet.private_app_b.id]
  target_group_arns   = [aws_lb_target_group.app.arn]

  min_size         = 1
  max_size         = 2
  desired_capacity = 1

  health_check_type         = "ELB"
  health_check_grace_period = 300

  launch_template {
    id      = aws_launch_template.backend.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "lunacare-${var.environment}-backend"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = true
  }
}
