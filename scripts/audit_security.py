#!/usr/bin/env python3
"""
LunaCare Security Audit Script
===============================

Reads audit logs from a JSON export (or a SQLite database export) and performs:
  1. Suspicious role-change detection
  2. Failed auth spike detection
  3. Unusual mass-delete events
  4. Admin access pattern monitoring
  5. Summary report export

SECURITY RULES:
  - Never prints decrypted medical content
  - Hashes all IPs before logging
  - Exits with code 1 on critical findings
  - Dry-run mode available for CI

Usage:
  python scripts/audit_security.py [--dry-run] [--input audit_export.json] [--output report.json]

Note: In the Android local-only app, audit logs are stored in the Room 'audit_logs' table.
      Export them to JSON first using ADB or a debug export tool, then run this script.
      In a Supabase-backed version, use the service role to read the audit_logs table.
"""

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from typing import Any

# ==========================================
# CONFIGURATION
# ==========================================

CRITICAL_ACTIONS = {
    "delete",
    "export",
    "account.deleted",
    "account.delete.requested",
    "journal.entry.deleted",
    "account.export.requested",
}
DELETE_ACTIONS = {
    "delete",
    "account.deleted",
    "account.delete.requested",
    "journal.entry.deleted",
}
EXPORT_ACTIONS = {
    "export",
    "account.export.requested",
}
AUTH_FAILURE_ACTIONS = {
    "login_failed",
    "auth.login.failure",
    "auth.otp.failure",
}
PROHIBITED_METADATA_KEYS = {
    "password",
    "passwordhash",
    "password_hash",
    "otp",
    "token",
    "secret",
    "pin",
    "securitypin",
    "notes",
    "notesencrypted",
    "notes_encrypted",
    "latitude",
    "longitude",
    "rawip",
    "raw_ip",
}
SUSPICIOUS_ROLES = {"ADMIN", "SUPER_ADMIN"}
DELETE_SPIKE_THRESHOLD = 5    # More than 5 deletes in one session = suspicious
AUTH_FAIL_THRESHOLD = 3       # More than 3 failed logins = suspicious


def _event_action(log: dict[str, Any]) -> str:
    """Return normalized action or eventType from local Room or backend audit records."""
    raw = log.get("action") or log.get("eventType") or ""
    return str(raw).strip()


def _event_timestamp(log: dict[str, Any]) -> Any:
    return log.get("createdAt") or log.get("timestamp")

# ==========================================
# UTILITY FUNCTIONS
# ==========================================

def hash_value(value: str) -> str:
    """SHA-256 hash a value (e.g. IP or user-agent). Never log raw values."""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()

def load_audit_logs(filepath: str) -> list[dict[str, Any]]:
    """Load a real local audit export; never silently substitute synthetic data."""
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise RuntimeError("Audit export unavailable or invalid; no events were checked.") from error

    logs = data if isinstance(data, list) else data.get("logs") if isinstance(data, dict) else None
    if not isinstance(logs, list) or not all(isinstance(item, dict) for item in logs):
        raise ValueError("Expected a list of audit events; no events were checked.")
    if not logs:
        raise ValueError("Audit export contains no events; coverage is unknown.")
    return logs

def _sample_audit_logs() -> list[dict[str, Any]]:
    """Sample logs for dry-run mode when no export file is available."""
    now = datetime.now(timezone.utc).isoformat()
    return [
        {"id": 1, "actorRole": "USER", "action": "insert", "resourceType": "period_logs", "resourceId": None, "createdAt": now, "metadata": "{}"},
        {"id": 2, "actorRole": "USER", "action": "insert", "resourceType": "behaviour_logs", "resourceId": None, "createdAt": now, "metadata": "{}"},
        {"id": 3, "actorRole": "USER", "action": "read", "resourceType": "profile", "resourceId": "1", "createdAt": now, "metadata": "{}"},
    ]

# ==========================================
# AUDIT CHECKS
# ==========================================

def check_critical_actions(logs: list[dict]) -> list[dict]:
    """Flag delete and export actions for review."""
    findings = []
    for log in logs:
        action = _event_action(log)
        if action in CRITICAL_ACTIONS:
            findings.append({
                "severity": "WARNING",
                "rule": "CRITICAL_ACTION",
                "message": f"Critical action '{action}' on '{log.get('resourceType')}' by role '{log.get('actorRole', 'UNKNOWN')}'",
                "logId": log.get("id") or log.get("eventId"),
                "timestamp": _event_timestamp(log)
            })
    return findings

def check_delete_spike(logs: list[dict]) -> list[dict]:
    """Flag if more than DELETE_SPIKE_THRESHOLD deletes occur."""
    findings = []
    deletes = [l for l in logs if _event_action(l) in DELETE_ACTIONS]
    if len(deletes) > DELETE_SPIKE_THRESHOLD:
        findings.append({
            "severity": "CRITICAL",
            "rule": "DELETE_SPIKE",
            "message": f"Unusual number of delete events: {len(deletes)} (threshold: {DELETE_SPIKE_THRESHOLD})",
            "count": len(deletes)
        })
    return findings

def check_auth_failures(logs: list[dict]) -> list[dict]:
    """Flag if more than AUTH_FAIL_THRESHOLD authentication failures occur."""
    findings = []
    failures = [
        l for l in logs
        if _event_action(l) in AUTH_FAILURE_ACTIONS or (
            _event_action(l).startswith("auth.") and l.get("result") in {"failure", "denied"}
        )
    ]
    if len(failures) > AUTH_FAIL_THRESHOLD:
        findings.append({
            "severity": "CRITICAL",
            "rule": "AUTH_FAILURE_SPIKE",
            "message": f"Failed authentication spike detected: {len(failures)} events (threshold: {AUTH_FAIL_THRESHOLD})",
            "count": len(failures)
        })
    return findings

def check_admin_activity(logs: list[dict]) -> list[dict]:
    """Flag any activity by admin/super-admin roles for review."""
    findings = []
    admin_logs = [l for l in logs if l.get("actorRole") in SUSPICIOUS_ROLES]
    if admin_logs:
        findings.append({
            "severity": "INFO",
            "rule": "ADMIN_ACTIVITY",
            "message": f"Admin activity detected: {len(admin_logs)} events",
            "roles": list({l.get("actorRole") for l in admin_logs})
        })
    return findings

def check_export_events(logs: list[dict]) -> list[dict]:
    """Flag data export events — highest risk in health apps."""
    findings = []
    exports = [l for l in logs if _event_action(l) in EXPORT_ACTIONS]
    for exp in exports:
        findings.append({
            "severity": "CRITICAL",
            "rule": "DATA_EXPORT",
            "message": f"Data export event detected: resource '{exp.get('resourceType')}' at {_event_timestamp(exp)}",
            "logId": exp.get("id") or exp.get("eventId")
        })
    return findings

def check_metadata_for_sensitive_content(logs: list[dict]) -> list[dict]:
    """
    Ensure no decrypted medical content or prohibited secret/PII keys leak into audit metadata.
    Flags oversized metadata strings/objects and prohibited sensitive key names.
    """
    findings = []
    for log in logs:
        log_id = log.get("id") or log.get("eventId")
        metadata = log.get("metadata", "{}")
        serialized = metadata if isinstance(metadata, str) else json.dumps(metadata, default=str)

        if len(serialized) > 500:
            findings.append({
                "severity": "WARNING",
                "rule": "METADATA_TOO_LARGE",
                "message": "Audit log metadata may contain sensitive content — metadata should be minimal",
                "logId": log_id
            })

        parsed_meta: dict[str, Any] | None = None
        if isinstance(metadata, dict):
            parsed_meta = metadata
        elif isinstance(metadata, str):
            try:
                candidate = json.loads(metadata)
                if isinstance(candidate, dict):
                    parsed_meta = candidate
            except json.JSONDecodeError:
                parsed_meta = None

        if parsed_meta:
            leaked_keys = [
                k for k in parsed_meta.keys()
                if str(k).lower().replace("-", "_") in PROHIBITED_METADATA_KEYS
            ]
            if leaked_keys:
                findings.append({
                    "severity": "CRITICAL",
                    "rule": "SENSITIVE_METADATA_KEY",
                    "message": f"Prohibited sensitive key(s) found in audit metadata: {sorted(leaked_keys)}",
                    "logId": log_id
                })
    return findings

# ==========================================
# REPORT GENERATION
# ==========================================

def generate_report(findings: list[dict], total_logs: int) -> dict:
    """Generate audit summary report."""
    critical = [f for f in findings if f.get("severity") == "CRITICAL"]
    warnings = [f for f in findings if f.get("severity") == "WARNING"]
    infos = [f for f in findings if f.get("severity") == "INFO"]

    return {
        "report_generated_at": datetime.now(timezone.utc).isoformat(),
        "total_audit_events": total_logs,
        "total_findings": len(findings),
        "critical_count": len(critical),
        "warning_count": len(warnings),
        "info_count": len(infos),
        "pass": len(critical) == 0,
        "findings": findings,
        "safety_notes": [
            "Only the supplied events were checked; missing events and uncollected systems are outside coverage.",
            "This report does not establish that an application or device is secure.",
            "Do not upload exports containing private medical records or credentials."
        ]
    }

def save_report(report: dict, output_path: str) -> None:
    """Save report to JSON."""
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
    print(f"[INFO] Report saved to: {output_path}")

# ==========================================
# MAIN
# ==========================================

def main():
    parser = argparse.ArgumentParser(
        description="LunaCare Security Audit Script — checks audit logs for suspicious activity"
    )
    parser.add_argument("--dry-run", action="store_true", help="Run with sample data (no real export needed)")
    parser.add_argument("--input", default="audit_export.json", help="Path to audit log JSON export")
    parser.add_argument("--output", default="audit_report.json", help="Path to save the audit report")
    args = parser.parse_args()

    print("=" * 60)
    print("  LunaCare Security Audit")
    print("  Educational app — no medical data processed here")
    print("=" * 60)

    if args.dry_run:
        logs = _sample_audit_logs()
        print("[INFO] Synthetic dry run only; no real events were inspected.")
    else:
        try:
            logs = load_audit_logs(args.input)
        except (RuntimeError, ValueError) as error:
            print(f"[COVERAGE GAP] {error}", file=sys.stderr)
            sys.exit(1)
    print(f"[INFO] Loaded {len(logs)} audit log entries")

    # Run all checks
    all_findings = []
    all_findings.extend(check_critical_actions(logs))
    all_findings.extend(check_delete_spike(logs))
    all_findings.extend(check_auth_failures(logs))
    all_findings.extend(check_admin_activity(logs))
    all_findings.extend(check_export_events(logs))
    all_findings.extend(check_metadata_for_sensitive_content(logs))

    # Generate report
    report = generate_report(all_findings, len(logs))
    report["coverage"] = "synthetic-dry-run" if args.dry_run else "provided-events-only"
    save_report(report, args.output)

    # Print summary
    print()
    print(f"  Total events:   {report['total_audit_events']}")
    print(f"  Critical:       {report['critical_count']}")
    print(f"  Warnings:       {report['warning_count']}")
    print(f"  Info:           {report['info_count']}")
    print()

    if report["pass"]:
        print("✅ DRY-RUN VALIDATION PASSED (synthetic data only)" if args.dry_run else
              "✅ AUDIT EVENT REVIEW COMPLETED — no critical patterns in supplied events; unobserved systems remain unchecked")
        sys.exit(0)
    else:
        print("❌ AUDIT FAILED — critical issues detected")
        for finding in all_findings:
            if finding.get("severity") == "CRITICAL":
                print(f"  CRITICAL: {finding['message']}")
        sys.exit(1)

if __name__ == "__main__":
    main()
