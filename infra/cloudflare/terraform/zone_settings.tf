resource "cloudflare_zone_settings_override" "lunacare_settings" {
  zone_id = var.cloudflare_zone_id

  settings {
    # CRITICAL: Full (strict) TLS ensures end-to-end encryption between Cloudflare edge and origin
    # Flexible SSL is strictly forbidden because it transmits unencrypted traffic over the internet.
    ssl = "strict"

    # Minimum TLS version 1.2; TLS 1.0 and 1.1 are completely disabled
    min_tls_version = "1.2"
    tls_1_3         = "on"

    # Enforce HTTPS redirects
    always_use_https         = "on"
    automatic_https_rewrites = "on"

    # Disable 0-RTT to eliminate replay attacks on state-changing API endpoints
    zero_rtt = "off"

    # Perimeter security level and automated browser integrity checks
    security_level = "medium"
    browser_check  = "on"

    # Compression and modern protocol support
    brotli = "on"
    http2  = "on"
    http3  = "on"
  }
}
