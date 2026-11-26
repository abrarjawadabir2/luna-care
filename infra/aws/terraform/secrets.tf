resource "aws_secretsmanager_secret" "lunacare_config" {
  name                    = "lunacare/${var.environment}/backend-config"
  description             = "Runtime credentials and configuration for LunaCare backend"
  recovery_window_in_days = 7

  tags = {
    Name = "lunacare-${var.environment}-backend-config"
  }
}

# Intentionally do not create aws_secretsmanager_secret_version here.
# Creating a version with placeholder JWT/database/Gemini credentials gives the
# appearance of a configured backend while writing unusable values to AWS.
# After deployment is authorized, populate the secret out of band via your
# approved secret manager and never place plaintext values in Terraform state.
