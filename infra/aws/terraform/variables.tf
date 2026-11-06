variable "aws_region" {
  description = "The AWS region to deploy resources into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name (e.g. dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "CIDR block for the LunaCare VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "instance_type" {
  description = "EC2 instance type (Graviton2 ARM64)"
  type        = string
  default     = "t4g.small"
}

variable "db_instance_class" {
  description = "RDS PostgreSQL instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB for RDS"
  type        = number
  default     = 20
}

variable "budget_limit_usd" {
  description = "Monthly budget threshold in USD for billing alerts"
  type        = string
  default     = "30"
}

variable "alert_email" {
  description = "Email address for AWS Budget cost and health alerts"
  type        = string
  default     = "security-alerts@example.com"
}
