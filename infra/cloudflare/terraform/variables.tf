variable "cloudflare_api_token" {
  description = "Scoped Cloudflare API token with Zone.DNS, Zone.Settings, and Pages permissions"
  type        = string
  sensitive   = true
  default     = "REPLACE_WITH_SCOPED_CLOUDFLARE_API_TOKEN"
}

variable "cloudflare_account_id" {
  description = "Cloudflare Account ID"
  type        = string
  default     = "REPLACE_WITH_CLOUDFLARE_ACCOUNT_ID"
}

variable "cloudflare_zone_id" {
  description = "Cloudflare Zone ID for the target domain"
  type        = string
  default     = "REPLACE_WITH_CLOUDFLARE_ZONE_ID"
}

variable "domain" {
  description = "Apex domain name for LunaCare"
  type        = string
  default     = "lunacare.example"
}

variable "environment" {
  description = "Environment name (prod, staging, dev)"
  type        = string
  default     = "prod"
}

variable "alert_email" {
  description = "Notification email address for security and certificate alerts"
  type        = string
  default     = "security-alerts@example.com"
}
