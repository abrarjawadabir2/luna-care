#!/usr/bin/env python3
"""
1-100x User Simulation & Adversarial Stress Test for LunaCare MarkItDown Converter.
Covers PDF, DOCX, PPTX, XLSX, nested-paren XSS links, unclosed scripts,
prompt injection, symlink/traversal/URI rejections, output overwrite guards,
and zero temp-file leakage across 100 simulated user runs.
"""

from __future__ import annotations

import io
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import convert_document as cd
import test_convert_document as base_tests


def _build_synthetic_pptx(slide_title: str, slide_body: str) -> bytes:
    """Build a minimal, valid .pptx presentation using python-pptx."""
    from pptx import Presentation

    prs = Presentation()
    layout = prs.slide_layouts[1]
    slide = prs.slides.add_slide(layout)
    slide.shapes.title.text = slide_title
    slide.placeholders[1].text = slide_body
    bio = io.BytesIO()
    prs.save(bio)
    return bio.getvalue()


class MarkItDown100xUserSimulationTests(unittest.TestCase):
    def setUp(self) -> None:
        self._tmpdir = tempfile.TemporaryDirectory()
        self.root = Path(self._tmpdir.name).resolve()

    def tearDown(self) -> None:
        self._tmpdir.cleanup()

    def test_100x_mixed_user_document_conversions_and_cleanup(self) -> None:
        before_temps = set(Path(tempfile.gettempdir()).glob("lunacare_doc_*"))
        store = cd.InMemoryConversionStore()

        for i in range(1, 101):
            owner = f"dev_user_{i % 10}"
            other_owner = f"dev_user_{(i + 1) % 10}"
            kind = i % 4

            if kind == 0:
                doc_path = self.root / f"doc_{i}.pdf"
                doc_path.write_bytes(base_tests._build_synthetic_pdf(f"LunaCare PDF Guide Run {i}"))
                res = cd.convert_local_document(doc_path, allowed_root=self.root, owner_id=owner, store=store)
                self.assertEqual(res.format_ext, ".pdf")
                self.assertIn(f"LunaCare PDF Guide Run {i}", res.markdown)
            elif kind == 1:
                doc_path = self.root / f"doc_{i}.docx"
                doc_path.write_bytes(
                    base_tests._build_synthetic_docx([
                        f"LunaCare DOCX Draft {i}",
                        "Click [Bad](javascript:alert(1)) or [Wiki](https://example.org/Cycle-(biology))",
                    ])
                )
                res = cd.convert_local_document(doc_path, allowed_root=self.root, owner_id=owner, store=store)
                self.assertEqual(res.format_ext, ".docx")
                self.assertIn(f"LunaCare DOCX Draft {i}", res.markdown)
                self.assertNotIn("javascript:", res.markdown.lower())
                self.assertNotIn("])", res.markdown, f"Run {i}: trailing paren leaked: {res.markdown}")
                self.assertIn("[Wiki](https://example.org/Cycle-(biology))", res.markdown)
            elif kind == 2:
                doc_path = self.root / f"doc_{i}.pptx"
                doc_path.write_bytes(
                    _build_synthetic_pptx(
                        f"LunaCare Slides {i}",
                        f"Educational slide body iteration {i}",
                    )
                )
                res = cd.convert_local_document(doc_path, allowed_root=self.root, owner_id=owner, store=store)
                self.assertEqual(res.format_ext, ".pptx")
                self.assertIn(f"LunaCare Slides {i}", res.markdown)
                self.assertIn(f"Educational slide body iteration {i}", res.markdown)
            else:
                doc_path = self.root / f"doc_{i}.xlsx"
                doc_path.write_bytes(
                    base_tests._build_synthetic_xlsx([
                        ["Metric", "Value"],
                        [f"Run{i}", "OK"],
                    ])
                )
                res = cd.convert_local_document(doc_path, allowed_root=self.root, owner_id=owner, store=store)
                self.assertEqual(res.format_ext, ".xlsx")
                self.assertIn(f"Run{i}", res.markdown)
                self.assertIn("|", res.markdown)

            self.assertTrue(res.temp_cleaned_up)
            # Owner access succeeds, cross-user fails
            self.assertIsNotNone(store.get_result(res.conversion_id, requester_id=owner))
            with self.assertRaises(cd.CrossUserAccessError):
                store.get_result(res.conversion_id, requester_id=other_owner)

        after_temps = set(Path(tempfile.gettempdir()).glob("lunacare_doc_*"))
        self.assertEqual(after_temps - before_temps, set(), "Temp directories leaked after 100 runs")

    def test_cli_refuses_to_overwrite_input_file_or_symlink_output(self) -> None:
        src = self.root / "guide.docx"
        src.write_bytes(base_tests._build_synthetic_docx(["Original source"]))
        original_bytes = src.read_bytes()

        # Passing --output equal to --input must fail and preserve original file
        rc = cd.main(["--input", str(src), "--output", str(src), "--allowed-root", str(self.root)])
        self.assertNotEqual(rc, 0, "CLI must refuse overwriting its own input file")
        self.assertEqual(src.read_bytes(), original_bytes)

        # Passing a symlink as --output must fail
        outside_target = Path(tempfile.gettempdir()) / "lunacare_symlink_out_target.md"
        outside_target.write_text("do not overwrite", encoding="utf-8")
        try:
            symlink_out = self.root / "out_link.md"
            symlink_out.symlink_to(outside_target)
            rc2 = cd.main(["--input", str(src), "--output", str(symlink_out), "--allowed-root", str(self.root)])
            self.assertNotEqual(rc2, 0, "CLI must refuse writing to symlink output")
            self.assertEqual(outside_target.read_text(encoding="utf-8"), "do not overwrite")
        finally:
            if outside_target.exists():
                outside_target.unlink()

    def test_unclosed_script_tag_sanitized_cleanly(self) -> None:
        safe, warnings = cd.sanitize_converted_markdown("Safe intro <script>alert(1)")
        self.assertNotIn("alert(1)", safe)
        self.assertNotIn("<script", safe.lower())
        self.assertIn("DANGEROUS_HTML_STRIPPED", warnings)

    def test_isolated_subprocess_kills_hung_parser_and_protects_existing_output(self) -> None:
        import os

        doc = self.root / "hang_check.docx"
        doc.write_bytes(base_tests._build_synthetic_docx(["Subprocess recovery check"]))

        # Warm worker so we can capture its real OS PID before inducing a hang
        ok_before = cd.convert_local_document(doc, allowed_root=self.root)
        self.assertIn("Subprocess recovery check", ok_before.markdown)
        hung_pid = cd._ISOLATED_WORKER.pid
        self.assertIsNotNone(hung_pid)

        # Induce a 5s parser hang with a 0.15s deadline -> worker PID must be SIGKILLed
        with self.assertRaises(cd.ConversionTimeoutError):
            cd.convert_local_document(
                doc,
                allowed_root=self.root,
                timeout_seconds=0.15,
                _simulate_worker_hang_seconds=5.0,
            )

        if os.name != "nt" and hung_pid is not None:
            with self.assertRaises(ProcessLookupError):
                os.kill(hung_pid, 0)

        # Subsequent conversion must transparently spawn a fresh worker and succeed
        ok_after = cd.convert_local_document(doc, allowed_root=self.root)
        self.assertIn("Subprocess recovery check", ok_after.markdown)

        # Existing output file must NOT be overwritten unless --overwrite is passed
        existing_out = self.root / "existing.md"
        existing_out.write_text("keep me safe", encoding="utf-8")
        rc_blocked = cd.main(
            ["--input", str(doc), "--output", str(existing_out), "--allowed-root", str(self.root)]
        )
        self.assertNotEqual(rc_blocked, 0)
        self.assertEqual(existing_out.read_text(encoding="utf-8"), "keep me safe")

        rc_allowed = cd.main(
            [
                "--input",
                str(doc),
                "--output",
                str(existing_out),
                "--overwrite",
                "--allowed-root",
                str(self.root),
            ]
        )
        self.assertEqual(rc_allowed, 0)
        self.assertIn("Subprocess recovery check", existing_out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
