#!/usr/bin/env bash
set -euo pipefail

# Optional developer tooling. Review the official ECC source before running.
# This does not install ECC during app builds or in production.
if ! command -v node >/dev/null || ! command -v npx >/dev/null; then
  echo "Node.js and npm/npx are required; install them using the official installer." >&2
  exit 1
fi

echo "ECC version requested: 2.2.3"
npm view ecc-universal@2.2.3 version
npx --yes ecc-universal@2.2.3 install --profile minimal --target antigravity
echo "Check Antigravity Settings > Customizations, then start a new conversation."
