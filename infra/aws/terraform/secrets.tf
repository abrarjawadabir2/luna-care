resource "aws_secretsmanager_secret" "lunacare_config" {
  name                    = "lunacare/${var.environment}/backend-config"
  description             = "Runtime credentials and configuration for LunaCare backend"
  recovery_window_in_days = 7

  tags = {
    Name = "lunacare-${var.environment}-backend-config"
  }
}

# Initial placeholder structure (Real secrets must be set out-of-band via AWS Console or CLI)
resource "aws_secretsmanager_secret_version" "initial_template" {
  secret_id = aws_secretsmanager_secret.lunacare_config.id
  secret_string = jsonencode({
    NODE_ENV          = "production"
    PORT              = "3000"
    HOST              = "0.0.0.0"
    CORS_ORIGIN       = "https://lunacare.example"
    JWT_SECRET        = "REPLACE_WITH_MINIMUM_32_CHAR_CRYPTOGRAPHIC_SECRET"
    SERVER_SECRET     = "REPLACE_WITH_MINIMUM_32_CHAR_CRYPTOGRAPHIC_SECRET"
    AUDIT_HMAC_SECRET = "REPLACE_WITH_MINIMUM_32_CHAR_CRYPTOGRAPHIC_SECRET"
    DATABASE_URL      = "postgresql://postgres:REPLACE_WITH_STRONG_PASSWORD@${aws_db_instance.postgres.endpoint}/lunacare?sslmode=verify-full"
    GEMINI_API_KEY    = "REPLACE_WITH_RESTRICTED_GOOGLE_CLOUD_API_KEY"
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
