# Cloudflare Edge HTTP Rate Limiting Ruleset
resource "cloudflare_ruleset" "rate_limiting" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-rate-limiting"
  description = "Edge rate limiting for sensitive authentication and AI routes"
  kind        = "zone"
  phase       = "http_ratelimit"

  # Rule 1: Edge Rate Limit on Authentication Endpoints
  # Uses managed_challenge to protect cellular NAT / shared public IPs from permanent lockouts
  rules {
    action      = "managed_challenge"
    description = "Edge rate limit on sensitive authentication endpoints (login, register, reset, OTP)"
    expression  = "(http.request.uri.path in {\"/api/v1/auth/login\", \"/api/v1/auth/register\", \"/api/v1/auth/password-reset\", \"/api/v1/auth/otp/request\", \"/api/v1/auth/otp/verify\"} and http.request.method eq \"POST\")"
    enabled     = true

    ratelimit {
      characteristics     = ["cf.unique_visitor_id"]
      period              = 60
      requests_per_period = 10
      mitigation_timeout  = 60
    }
  }

  # Rule 2: Edge Rate Limit on AI Inference Endpoint
  rules {
    action      = "managed_challenge"
    description = "Edge rate limit on expensive AI inference endpoint"
    expression  = "(http.request.uri.path eq \"/api/v1/ai/chat\" and http.request.method eq \"POST\")"
    enabled     = true

    ratelimit {
      characteristics     = ["cf.unique_visitor_id"]
      period              = 60
      requests_per_period = 20
      mitigation_timeout  = 120
    }
  }
}
