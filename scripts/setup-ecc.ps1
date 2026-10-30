$ErrorActionPreference = "Stop"

# Optional Antigravity developer tooling, not a production dependency.
if (-not (Get-Command node -ErrorAction SilentlyContinue) -or -not (Get-Command npx -ErrorAction SilentlyContinue)) {
    throw "Node.js and npm/npx must be installed using an official installer."
}

npm view ecc-universal@2.2.3 version
if ($LASTEXITCODE -ne 0) { throw "ECC package verification failed." }
npx --yes ecc-universal@2.2.3 install --profile minimal --target antigravity
if ($LASTEXITCODE -ne 0) { throw "ECC installation failed." }
Write-Host "Check Antigravity Settings > Customizations, then start a new conversation."
