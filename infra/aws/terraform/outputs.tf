output "vpc_id" {
  description = "The ID of the LunaCare VPC"
  value       = aws_vpc.main.id
}

output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_security_group_id" {
  description = "Security Group ID of the public Application Load Balancer"
  value       = aws_security_group.alb.id
}

output "app_security_group_id" {
  description = "Security Group ID of the private backend application tier"
  value       = aws_security_group.app.id
}

output "db_security_group_id" {
  description = "Security Group ID of the private database tier"
  value       = aws_security_group.db.id
}

output "iam_instance_role_arn" {
  description = "ARN of the least-privilege IAM instance role for backend instances"
  value       = aws_iam_role.ec2_role.arn
}

output "secrets_manager_secret_arn" {
  description = "ARN of the AWS Secrets Manager container for application secrets"
  value       = aws_secretsmanager_secret.lunacare_config.arn
}
