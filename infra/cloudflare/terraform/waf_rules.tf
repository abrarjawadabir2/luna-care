# Restrict common sensitive paths without paid-plan regex operators.
# Application authentication and input validation remain mandatory.
resource "cloudflare_ruleset" "waf_custom" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-waf-ruleset"
  description = "Block sensitive path access and unsupported methods"
  kind        = "zone"
  phase       = "http_request_firewall_custom"

  rules {
    ref         = "block_sensitive_paths"
    action      = "block"
    description = "Block exposed config and common control-panel probes"
    expression  = "(starts_with(http.request.uri.path, \"/.env\") or starts_with(http.request.uri.path, \"/.git\") or starts_with(http.request.uri.path, \"/.aws\") or starts_with(http.request.uri.path, \"/.ssh\") or starts_with(http.request.uri.path, \"/wp-admin\") or starts_with(http.request.uri.path, \"/phpmyadmin\") or starts_with(http.request.uri.path, \"/actuator\"))"
    enabled     = true
  }

  rules {
    ref         = "block_admin_paths"
    action      = "block"
    description = "Block public access to internal admin paths"
    expression  = "(starts_with(http.request.uri.path, \"/admin\"))"
    enabled     = true
  }

  rules {
    ref         = "reject_unexpected_api_methods"
    action      = "block"
    description = "Reject methods not supported by the API"
    expression  = "(starts_with(http.request.uri.path, \"/api/\") and not (http.request.method in {\"GET\" \"POST\" \"PATCH\" \"PUT\" \"DELETE\" \"OPTIONS\" \"HEAD\"}))"
    enabled     = true
  }
}
