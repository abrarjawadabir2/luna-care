# 1. Application Load Balancer Security Group
resource "aws_security_group" "alb" {
  name        = "lunacare-${var.environment}-alb-sg"
  description = "Security group for LunaCare public ALB: allows inbound HTTPS"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Allow HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow HTTP for redirect to HTTPS"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description     = "Allow backend port 3000 traffic to application instances only"
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = {
    Name = "lunacare-${var.environment}-alb-sg"
  }
}

# 2. Application Backend Security Group (EC2 / ECS)
# CRITICAL: PORT 22 IS NEVER OPEN. Zero public ingress.
resource "aws_security_group" "app" {
  name        = "lunacare-${var.environment}-app-sg"
  description = "Security group for LunaCare backend: accepts traffic ONLY from ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Accept Fastify backend traffic strictly from ALB security group"
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description     = "Allow outbound PostgreSQL connection strictly to database security group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.db.id]
  }

  egress {
    description = "Allow outbound HTTPS for AWS APIs, SSM Session Manager, and CloudWatch"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "lunacare-${var.environment}-app-sg"
  }
}

# 3. Database Security Group (RDS PostgreSQL)
# CRITICAL: ZERO PUBLIC ACCESS. Accessible ONLY on port 5432 from app_sg.
resource "aws_security_group" "db" {
  name        = "lunacare-${var.environment}-db-sg"
  description = "Security group for PostgreSQL RDS: allows access ONLY from app instances"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL access strictly from LunaCare backend application security group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  # Zero egress rules required for RDS
  tags = {
    Name = "lunacare-${var.environment}-db-sg"
  }
}
