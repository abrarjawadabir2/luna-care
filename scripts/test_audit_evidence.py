"""Regression tests for trustworthy, explicitly scoped audit evidence."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("audit_security.py")


class AuditEvidenceTests(unittest.TestCase):
    def invoke(self, *args):
        return subprocess.run(
            [sys.executable, str(SCRIPT), *map(str, args)],
            capture_output=True, text=True, check=False,
        )

    def test_missing_export_never_returns_clean_audit(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = Path(directory) / "no-events.json"
            output = Path(directory) / "report.json"
            result = self.invoke("--input", missing, "--output", output)
            self.assertEqual(result.returncode, 1)
            self.assertIn("COVERAGE GAP", result.stderr)
            self.assertNotIn("AUDIT PASSED", result.stdout)
            self.assertFalse(output.exists())

    def test_invalid_and_empty_exports_are_coverage_gaps(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "events.json"
            for data in ["not-json", "[]", '{"logs":{}}']:
                file.write_text(data, encoding="utf-8")
                result = self.invoke("--input", file, "--output", Path(directory) / "report.json")
                self.assertEqual(result.returncode, 1)
                self.assertIn("COVERAGE GAP", result.stderr)

    def test_real_events_label_coverage_and_are_not_comprehensive_certification(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "events.json"
            output = Path(directory) / "result.json"
            file.write_text(json.dumps([{
                "id": 1, "actorRole": "USER", "action": "read",
                "resourceType": "public_info", "createdAt": "2026-11-03T00:00:00Z"
            }]), encoding="utf-8")
            result = self.invoke("--input", file, "--output", output)
            self.assertEqual(result.returncode, 0, result.stderr)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(report["coverage"], "provided-events-only")
            self.assertIn("unobserved systems remain unchecked", result.stdout)

    def test_dry_run_uses_only_labeled_synthetic_events(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "sample.json"
            result = self.invoke("--dry-run", "--output", output)
            self.assertEqual(result.returncode, 0, result.stderr)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(report["coverage"], "synthetic-dry-run")
            self.assertIn("synthetic data only", result.stdout)

    def test_auth_failure_spike_and_sensitive_metadata_fail_audit(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "events.json"
            output = Path(directory) / "result.json"
            events = [
                {"eventId": f"e{i}", "eventType": "auth.login.failure", "result": "failure", "timestamp": "2026-11-04T00:00:00Z", "metadata": {}}
                for i in range(4)
            ] + [
                {"eventId": "e5", "eventType": "journal.entry.created", "result": "success", "timestamp": "2026-11-04T00:01:00Z", "metadata": {"notesEncrypted": "secret-blob"}}
            ]
            file.write_text(json.dumps(events), encoding="utf-8")
            result = self.invoke("--input", file, "--output", output)
            self.assertEqual(result.returncode, 1)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertFalse(report["pass"])
            rules = {f["rule"] for f in report["findings"]}
            self.assertIn("AUTH_FAILURE_SPIKE", rules)
            self.assertIn("SENSITIVE_METADATA_KEY", rules)


if __name__ == "__main__":
    unittest.main()
