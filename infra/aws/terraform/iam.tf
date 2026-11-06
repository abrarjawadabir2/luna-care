resource "aws_iam_role" "ec2_role" {
  name = "lunacare-${var.environment}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "lunacare-${var.environment}-ec2-role"
  }
}

# Attach SSM Managed Policy (Allows secure Session Manager access without open SSH port 22)
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Attach CloudWatch Agent Policy (Allows sending sanitized metrics and logs)
resource "aws_iam_role_policy_attachment" "cloudwatch" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# Scoped Secrets Manager Access Policy (Read-only access to LunaCare secret ARN ONLY)
resource "aws_iam_policy" "secrets_access" {
  name        = "lunacare-${var.environment}-secrets-access"
  description = "Allows LunaCare instances to read application secret from AWS Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = aws_secretsmanager_secret.lunacare_config.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "secrets" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.secrets_access.arn
}

resource "aws_iam_instance_profile" "lunacare_profile" {
  name = "lunacare-${var.environment}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}
