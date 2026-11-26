# IP-based edge limits complement (and do not replace) backend quotas.
# Avoid interactive managed challenges on Android/native API responses.
# Review Cloudflare plan limits and cellular NAT impact before enabling.
resource "cloudflare_ruleset" "rate_limiting" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-rate-limiting"
  description = "Conservative edge limits for authentication and AI endpoints"
  kind        = "zone"
  phase       = "http_ratelimit"

  rules {
    ref         = "auth_post_edge_limit"
    action      = "block"
    description = "Rate limit sensitive auth operations by network IP"
    expression  = "(http.request.uri.path in {\"/api/v1/auth/login\" \"/api/v1/auth/register\" \"/api/v1/auth/password-reset\" \"/api/v1/auth/otp/request\" \"/api/v1/auth/otp/verify\"} and http.request.method eq \"POST\")"
    enabled     = false

    ratelimit {
      characteristics     = ["cf.colo.id", "ip.src"]
      period              = 60
      requests_per_period = 60
      mitigation_timeout  = 60
    }
  }

  rules {
    ref         = "ai_chat_edge_limit"
    action      = "block"
    description = "Conservative edge protection for AI requests"
    expression  = "(http.request.uri.path eq \"/api/v1/ai/chat\" and http.request.method eq \"POST\")"
    enabled     = false

    ratelimit {
      characteristics     = ["cf.colo.id", "ip.src"]
      period              = 60
      requests_per_period = 120
      mitigation_timeout  = 120
    }
  }
}
