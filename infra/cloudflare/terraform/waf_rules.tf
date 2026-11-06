# Cloudflare Custom WAF Ruleset for LunaCare Perimeter
resource "cloudflare_ruleset" "waf_custom" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-waf-ruleset"
  description = "Perimeter WAF rules blocking exploit scanners, sensitive file probing, and method violations"
  kind        = "zone"
  phase       = "http_request_firewall_custom"

  # Rule 1: Block Sensitive Path & Credential Probing (.env, .git, .aws, admin consoles)
  rules {
    action      = "block"
    description = "Block sensitive path and credential probing"
    expression  = "(http.request.uri.path matches \"^/(\\\\.(env|git|aws|ssh)|wp-admin|phpmyadmin|actuator|telescope)\")"
    enabled     = true
  }

  # Rule 2: Block Probing of Internal Admin Interfaces
  rules {
    action      = "block"
    description = "Block probing of nonexistent internal admin paths"
    expression  = "(http.request.uri.path matches \"^/admin\")"
    enabled     = true
  }

  # Rule 3: Block Automated Security Scanners & Reconnaissance Bots
  rules {
    action      = "block"
    description = "Block recognized automated vulnerability scanners"
    expression  = "(http.user_agent matches \"(?i)(sqlmap|nikto|acunetix|nessus|masscan|zgrab|nmap)\")"
    enabled     = true
  }

  # Rule 4: Restrict HTTP Methods on API Routes
  rules {
    action      = "block"
    description = "Block dangerous or unapproved HTTP verbs on API routes"
    expression  = "(http.request.uri.path starts_with \"/api/\" and not http.request.method in {\"GET\", \"POST\", \"PATCH\", \"PUT\", \"DELETE\", \"OPTIONS\", \"HEAD\"})"
    enabled     = true
  }
}
