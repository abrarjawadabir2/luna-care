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

# Required for the HTTPS listener. There is intentionally no fake default.
variable "alb_certificate_arn" {
  description = "ACM certificate ARN for the ALB HTTPS listener, in the selected AWS region"
  type        = string

  validation {
    condition     = can(regex("^arn:aws:acm:[a-z0-9-]+:[0-9]{12}:certificate/[a-zA-Z0-9-]+$", var.alb_certificate_arn))
    error_message = "Provide a real ACM certificate ARN in the AWS region selected."
  }
}

variable "backend_runtime_verified" {
  description = "Set true only when verified bootstrapping installs a supported Node runtime, a pinned app artifact and service wiring"
  type        = bool
  default     = false
}

variable "deployment_authorized" {
  description = "Set to true only after explicit owner authorization for billable AWS provisioning"
  type        = bool
  default     = false
}

variable "backend_persistence_verified" {
  description = "Set to true only when a persistent backend adapter, deployment artifact and health-data safeguards are verified"
  type        = bool
  default     = false
}
