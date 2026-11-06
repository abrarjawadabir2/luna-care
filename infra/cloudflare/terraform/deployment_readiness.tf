# Cloudflare can create public DNS, WAF, and a tunnel to an unconfigured API.
# Require explicit consent AND a tested, reachable private origin before plan/apply.
resource "terraform_data" "deployment_readiness_gate" {
  input = {
    approved     = var.deployment_authorized
    origin_ready = var.api_origin_verified
  }

  lifecycle {
    precondition {
      condition     = var.deployment_authorized && var.api_origin_verified
      error_message = "Cloudflare deployment is blocked until owner approval and a tested running private API/cloudflared origin exist."
    }
  }
}
