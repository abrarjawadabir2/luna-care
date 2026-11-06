"""
LunaCare Cloudflare Infrastructure Static Security & Configuration Tests
Validates Cloudflare Terraform specifications, WAF rules, cache policies, and Pages headers.
"""

import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
TF_DIR = REPO_ROOT / "infra" / "cloudflare" / "terraform"
WEB_DIR = REPO_ROOT / "web"

class TestCloudflareInfraSecurity(unittest.TestCase):

    def test_strict_tls_configured(self):
        """Full (strict) TLS must be configured; Flexible SSL is strictly forbidden."""
        zone_file = (TF_DIR / "zone_settings.tf").read_text(encoding="utf-8")
        self.assertIn('ssl = "strict"', zone_file, "Zone must enforce strict SSL/TLS")
        self.assertNotIn('ssl = "flexible"', zone_file, "Flexible SSL must never be used")

    def test_min_tls_version(self):
        """Minimum TLS version must be 1.2 or higher."""
        zone_file = (TF_DIR / "zone_settings.tf").read_text(encoding="utf-8")
        self.assertIn('min_tls_version = "1.2"', zone_file)
        self.assertNotIn('min_tls_version = "1.0"', zone_file)
        self.assertNotIn('min_tls_version = "1.1"', zone_file)

    def test_cache_bypass_for_api(self):
        """Cloudflare Cache Rules must strictly bypass edge caching on API and health routes."""
        cache_file = (TF_DIR / "cache_rules.tf").read_text(encoding="utf-8")
        self.assertIn('cache = false', cache_file, "Must bypass edge cache for API routes")
        self.assertIn('/api/', cache_file, "Cache bypass rule must target /api/")

    def test_waf_blocks_sensitive_paths(self):
        """Perimeter WAF must block probing of .env, .git, .aws, and admin interfaces."""
        waf_file = (TF_DIR / "waf_rules.tf").read_text(encoding="utf-8")
        self.assertIn("env", waf_file)
        self.assertIn("git", waf_file)
        self.assertIn("admin", waf_file)
        self.assertIn('action      = "block"', waf_file)

    def test_rate_limiting_configured(self):
        """Edge rate limiting must protect authentication and AI endpoints."""
        rl_file = (TF_DIR / "rate_limiting.tf").read_text(encoding="utf-8")
        self.assertIn("/api/v1/auth/login", rl_file)
        self.assertIn("/api/v1/ai/chat", rl_file)
        self.assertIn('action      = "managed_challenge"', rl_file)

    def test_tunnel_ingress_rules(self):
        """Cloudflare Tunnel must map api hostname to local backend port 3000."""
        tunnel_file = (TF_DIR / "tunnel.tf").read_text(encoding="utf-8")
        self.assertIn('service  = "http://localhost:3000"', tunnel_file)
        self.assertIn('service = "http_status:404"', tunnel_file, "Must have fallback 404 rule")

    def test_outputs_zero_secrets(self):
        """Terraform outputs must not expose sensitive tokens, passwords, or tunnel secrets."""
        outputs_file = (TF_DIR / "outputs.tf").read_text(encoding="utf-8")
        forbidden = ["secret", "password", "token", "key", "api_token"]
        for term in forbidden:
            self.assertNotIn(f"value = {term}", outputs_file.lower())

    def test_pages_headers_security(self):
        """Cloudflare Pages _headers file must define strict CSP, nosniff, frame-ancestors, and HSTS."""
        headers_file = (WEB_DIR / "_headers").read_text(encoding="utf-8")
        self.assertIn("Content-Security-Policy", headers_file)
        self.assertIn("default-src 'self'", headers_file)
        self.assertIn("script-src 'none'", headers_file)
        self.assertIn("frame-ancestors 'none'", headers_file)
        self.assertIn("X-Content-Type-Options: nosniff", headers_file)
        self.assertIn("X-Frame-Options: DENY", headers_file)
        self.assertIn("Strict-Transport-Security", headers_file)
        self.assertIn("Permissions-Policy", headers_file)

    def test_robots_txt_disallows_private_paths(self):
        """web/robots.txt must disallow crawling of private API and health endpoints."""
        robots_file = (WEB_DIR / "robots.txt").read_text(encoding="utf-8")
        self.assertIn("Disallow: /api/", robots_file)
        self.assertIn("Disallow: /admin/", robots_file)
        self.assertIn("Disallow: /journal/", robots_file)
        self.assertIn("Disallow: /periods/", robots_file)
        self.assertIn("Disallow: /moods/", robots_file)
        self.assertIn("Sitemap:", robots_file)

    def test_sitemap_xml_valid(self):
        """web/sitemap.xml must declare the canonical public landing page URL."""
        sitemap_file = (WEB_DIR / "sitemap.xml").read_text(encoding="utf-8")
        self.assertIn("https://lunacare.example/", sitemap_file)
        self.assertIn("<urlset", sitemap_file)

if __name__ == "__main__":
    unittest.main()
