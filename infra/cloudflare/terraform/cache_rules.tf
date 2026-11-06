# Cloudflare Cache Rules: Enforce Zero Edge Caching on API and Health Data
resource "cloudflare_ruleset" "cache_rules" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-cache-rules"
  description = "Strictly bypass edge cache on all private API and user health routes"
  kind        = "zone"
  phase       = "http_request_cache_settings"

  # CRITICAL CACHE SECURITY RULE: Bypass Edge Cache on ALL API and User Health Routes
  # Cloudflare must NEVER cache private medical journals, period logs, moods, or tokens!
  rules {
    action      = "set_cache_settings"
    description = "Strict cache bypass on all API routes"
    expression  = "(http.host eq \"api.${var.domain}\" or http.request.uri.path starts_with \"/api/\")"
    enabled     = true

    action_parameters {
      cache = false
    }
  }

  # Cache Public Static Landing Page & SEO Assets
  rules {
    action      = "set_cache_settings"
    description = "Edge caching for public landing page and SEO robots/sitemap"
    expression  = "(http.host eq var.domain and http.request.uri.path in {\"/\", \"/index.html\", \"/robots.txt\", \"/sitemap.xml\"})"
    enabled     = true

    action_parameters {
      cache = true

      edge_ttl {
        mode    = "override_origin"
        default = 7200 # 2 hours
      }

      browser_ttl {
        mode    = "override_origin"
        default = 3600 # 1 hour
      }
    }
  }
}
