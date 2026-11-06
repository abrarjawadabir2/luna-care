# Generate 32-byte random secret for Cloudflare Tunnel authentication
resource "random_id" "tunnel_secret" {
  byte_length = 32
}

# Cloudflare Tunnel definition for EC2 Backend
resource "cloudflare_tunnel" "ec2_backend" {
  account_id = var.cloudflare_account_id
  name       = "lunacare-${var.environment}-ec2-tunnel"
  secret     = random_id.tunnel_secret.b64_std
}

# Cloudflare Tunnel Ingress Routing Configuration
resource "cloudflare_tunnel_config" "ec2_ingress" {
  account_id = var.cloudflare_account_id
  tunnel_id  = cloudflare_tunnel.ec2_backend.id

  config {
    # Ingress Rule 1: Route api.lunacare.example to local Fastify backend
    ingress_rule {
      hostname = "api.${var.domain}"
      service  = "http://localhost:3000"

      origin_request {
        connect_timeout          = "10s"
        no_tls_verify            = false
        keep_alive_timeout       = "60s"
        http_host_header         = "api.${var.domain}"
        origin_server_name       = "api.${var.domain}"
      }
    }

    # Catch-all rule for unmatched hostnames
    ingress_rule {
      service = "http_status:404"
    }
  }
}
