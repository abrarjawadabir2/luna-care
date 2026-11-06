# Random password generation for master database user (stored in state only, never committed to git)
resource "aws_db_instance" "postgres" {
  identifier                  = "lunacare-${var.environment}-postgres"
  engine                      = "postgres"
  engine_version              = "16.3"
  instance_class              = var.db_instance_class
  allocated_storage           = var.db_allocated_storage
  max_allocated_storage       = 100
  storage_type                = "gp3"
  storage_encrypted           = true

  db_name                     = "lunacare"
  username                    = "lunacare_admin"
  manage_master_user_password = true

  db_subnet_group_name        = aws_db_subnet_group.rds.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false

  backup_retention_period     = 7
  backup_window               = "03:00-04:00"
  maintenance_window          = "sun:04:30-sun:05:30"
  auto_minor_version_upgrade  = true
  copy_tags_to_snapshot       = true
  deletion_protection         = var.environment == "prod" ? true : false
  skip_final_snapshot         = false
  final_snapshot_identifier   = "lunacare-${var.environment}-final-snapshot"

  tags = {
    Name = "lunacare-${var.environment}-postgres"
  }
}
