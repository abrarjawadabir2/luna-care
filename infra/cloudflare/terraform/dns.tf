# 1. Apex Domain: Points to Cloudflare Pages project (Proxied)
resource "cloudflare_record" "apex" {
  zone_id = var.cloudflare_zone_id
  name    = "@"
  value   = "${cloudflare_pages_project.website.name}.pages.dev"
  type    = "CNAME"
  proxied = true
  ttl     = 1 # Automatic when proxied
  comment = "LunaCare public landing page hosted on Cloudflare Pages"
}

# 2. WWW Subdomain: Points to apex domain (Proxied)
resource "cloudflare_record" "www" {
  zone_id = var.cloudflare_zone_id
  name    = "www"
  value   = var.domain
  type    = "CNAME"
  proxied = true
  ttl     = 1
  comment = "WWW subdomain redirecting to apex"
}

# 3. Secure API Subdomain: Routed through Cloudflare Tunnel to private EC2 (Proxied)
# CRITICAL: Proxied through Cloudflare Tunnel; zero origin IP exposure
resource "cloudflare_record" "api" {
  zone_id = var.cloudflare_zone_id
  name    = "api"
  value   = "${cloudflare_tunnel.ec2_backend.id}.cfargotunnel.com"
  type    = "CNAME"
  proxied = true
  ttl     = 1
  comment = "Trusted LunaCare API gateway routed via Cloudflare Tunnel"
}
