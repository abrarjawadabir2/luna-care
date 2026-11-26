#!/usr/bin/env python3
"""Run real Chromium interactions against LunaCare's static public website.

No network destinations, private test data, accounts, or healthcare records
are used. Screenshots contain public landing-page content only.
"""
from pathlib import Path
from urllib.parse import urlparse

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[2]
HTML = ROOT / "web" / "index.html"
SHOTS = ROOT / "test-results" / "web-ux"


def run():
    SHOTS.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch()
        try:
            for width in [320, 390, 768, 1440]:
                page = browser.new_page(viewport={"width": width, "height": 800}, device_scale_factor=1)
                errors = []
                page.on("pageerror", lambda error: errors.append(str(error)))
                page.goto(HTML.as_uri(), wait_until="load")

                assert page.title().startswith("LunaCare"), f"{width}px: page title missing"
                assert page.locator("h1").count() == 1, f"{width}px: expected one page heading"
                assert page.get_by_role("main").count() == 1, f"{width}px: main landmark missing"
                assert page.locator("nav").count() >= 1, f"{width}px: navigation missing"

                geometry = page.evaluate("""() => ({
                  scroll: document.documentElement.scrollWidth,
                  viewport: window.innerWidth
                })""")
                assert geometry["scroll"] <= geometry["viewport"] + 1, (
                    f"{width}px: horizontal scrolling {geometry}"
                )

                # Keyboard: public website must offer an early skip-to-content link.
                page.keyboard.press("Tab")
                focused = page.evaluate("document.activeElement?.getAttribute('href')")
                assert focused == "#main", f"{width}px: skip link is not keyboard-first ({focused})"

                privacy = page.get_by_role("link", name="Privacy", exact=True)
                privacy.click()
                assert page.url.endswith("#privacy"), f"{width}px: Privacy navigation did not work"

                # FAQ is a native disclosure control and should be usable without JS.
                faq = page.locator("details").first
                summary = faq.locator("summary")
                summary.click()
                assert faq.get_attribute("open") is not None, f"{width}px: FAQ did not expand"
                summary.click()
                assert faq.get_attribute("open") is None, f"{width}px: FAQ did not collapse"

                for anchor in page.locator("a[href^='#']").all():
                    href = anchor.get_attribute("href")
                    if href and len(href) > 1:
                        assert page.locator(f"[id='{href[1:]}']").count() > 0, (
                            f"{width}px: broken in-page link {href}"
                        )

                for anchor in page.locator("a[href^='http']").all():
                    destination = urlparse(anchor.get_attribute("href"))
                    assert destination.scheme == "https", "External links must use HTTPS"

                assert not errors, f"{width}px: browser runtime errors: {errors}"
                page.screenshot(path=str(SHOTS / f"lunacare-{width}px.png"), full_page=True)
                page.close()
            print("Real Chromium landing-page journeys passed at 320, 390, 768 and 1440px.")
        finally:
            browser.close()


if __name__ == "__main__":
    run()
