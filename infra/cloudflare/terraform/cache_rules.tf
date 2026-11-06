# Cache only public static pages. Never cache API or personal health responses.
resource "cloudflare_ruleset" "cache_rules" {
  zone_id     = var.cloudflare_zone_id
  name        = "lunacare-${var.environment}-cache-rules"
  description = "Bypass cache for all API requests; cache static public files only"
  kind        = "zone"
  phase       = "http_request_cache_settings"

  rules {
    ref         = "bypass_private_api_cache"
    action      = "set_cache_settings"
    description = "Never edge-cache private API data"
    expression  = "(http.host eq \"api.${var.domain}\" or starts_with(http.request.uri.path, \"/api/\"))"
    enabled     = true

    action_parameters {
      cache = false
    }
  }

  rules {
    ref         = "public_static_cache"
    action      = "set_cache_settings"
    description = "Cache public landing page and indexing resources"
    expression  = "(http.host eq \"${var.domain}\" and http.request.uri.path in {\"/\" \"/index.html\" \"/robots.txt\" \"/sitemap.xml\"})"
    enabled     = true

    action_parameters {
      cache = true

      edge_ttl {
        mode    = "override_origin"
        default = 7200
      }

      browser_ttl {
        mode    = "override_origin"
        default = 3600
      }
    }
  }
}
