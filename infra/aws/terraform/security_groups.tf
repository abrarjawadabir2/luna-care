# Declare groups independently of their referenced rules. Inline references
# previously created a Terraform dependency cycle ALB -> app -> DB -> ALB.
# No default outbound rule: only the explicit egress rules below are allowed.

resource "aws_security_group" "alb" {
  name        = "lunacare-${var.environment}-alb-sg"
  description = "Public ALB: HTTP redirects to HTTPS"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "lunacare-${var.environment}-alb-sg"
  }
}

resource "aws_security_group" "app" {
  name        = "lunacare-${var.environment}-app-sg"
  description = "Private backend: only ALB may connect to port 3000"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "lunacare-${var.environment}-app-sg"
  }
}

resource "aws_security_group" "db" {
  name        = "lunacare-${var.environment}-db-sg"
  description = "Private RDS: only app may connect to port 5432"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "lunacare-${var.environment}-db-sg"
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "Public HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "alb_http_redirect" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP only for redirect to HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_app" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.app.id
  description                  = "Backend port from load balancer"
  from_port                    = 3000
  to_port                      = 3000
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id
  description                  = "Only the ALB may contact backend"
  from_port                    = 3000
  to_port                      = 3000
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "app_to_db" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.db.id
  description                  = "Backend PostgreSQL connection"
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "app_https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS to approved AWS endpoints and external services"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.app.id
  description                  = "PostgreSQL only from trusted backend"
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

# RDS SG has NO outbound rule. Neither app nor DB accepts public ingress.
