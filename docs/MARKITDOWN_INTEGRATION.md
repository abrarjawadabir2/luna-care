# Microsoft MarkItDown Integration Guide (LunaCare)

## 1. Repository Feasibility & Architecture Decision

An audit of LunaCare's upload and AI services shows that **public user-document upload and conversion is NOT ready for production**:

- `backend/src/services/uploads.service.ts` (`UploadService.inspectAndStore`) decodes `base64Data` into a transient in-memory `Buffer`, validates the 5 MB limit and magic bytes (`image/png`, `image/jpeg`, `image/webp`, `application/pdf`), and records metadata (`UploadRecord`) in `InMemoryDatabase` (`backend/src/database/client.ts`).
- The raw file bytes are discarded after inspection — there is no durable encrypted object storage, no document retrieval endpoint, and no owner-scoped document deletion pipeline.
- `backend/src/services/ai.service.ts` enforces crisis detection, monthly quotas, and `sanitizeAiOutput()`, and has zero access to uploaded files, database handles, or MCP tools.
- `app/src/main/java/com/example/` is a local-first Kotlin/Compose Android app using Room SQLite and Android Keystore AES-256-GCM encryption; no Python runtime is bundled into the Android APK.

**Decision:** Rather than exposing a broken or unsafe user-facing upload feature, LunaCare integrates Microsoft MarkItDown (`markitdown[pdf,docx,pptx,xlsx]==0.1.9`) as an **optional, hardened local developer CLI and conversion adapter** ([scripts/convert_document.py](file:///Users/abrar/antigravity/LunaCare/scripts/convert_document.py)).

---

## 2. Evaluated Use Cases

| Use Case | Status | Scope & Guardrails |
|---|---|---|
| **A. Developer documentation** | **Approved (Local CLI)** | Convert approved local `.pdf`, `.docx`, `.pptx`, `.xlsx` docs into Markdown for local workflows. |
| **B. Educational content** | **Approved (Local CLI)** | Convert authorized educational drafts into Markdown for editorial review before publication. |
| **C. AI knowledge preparation** | **Approved (Local CLI)** | Prepare nonsensitive reference docs with source provenance (`source_filename`, `source_sha256`, `owner_id`) and untrusted-data framing. |
| **D. User document import** | **Blocked / Not Approved** | Requires encrypted durable object storage, owner-scoped retrieval/deletion, retention enforcement, and explicit user consent. |

Converted documents must **never** be used automatically for model training.

---

## 3. Local Developer Setup & Usage

MarkItDown requires Python >= 3.10. Always install it in an isolated virtual environment (never globally and never into the Android APK):

```sh
python3.12 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-markitdown.txt
```

Run the local converter on an approved, nonsensitive file inside the workspace:

```sh
.venv/bin/python scripts/convert_document.py \
  --input docs/example.pdf \
  --output .artifacts/example.md \
  --allowed-root . \
  --json-meta
```

Run the 16 synthetic-document security and conversion tests:

```sh
.venv/bin/python -m unittest discover -s scripts -p 'test_convert_document.py' -v
```

---

## 4. Privacy & Security Protections

- **Local & Offline Only:** Outbound socket connections (`socket.socket.connect`, `socket.create_connection`) and `urllib.request.urlopen` are blocked during conversion (`disable_network_access`).
- **Cloud Services Disabled:** `MarkItDown(enable_plugins=False)` is invoked without `llm_client`, `llm_model`, OCR services, Azure Document Intelligence, or Azure Content Understanding.
- **Strict Input Validation:**
  - Rejects remote URLs (`http://`, `https://`, `ftp://`, `file://`, `data:`, `//`).
  - Rejects path traversal (`..`), null bytes, symlinks, and files outside `--allowed-root`.
  - Blocks sensitive paths (`.env`, `.env.*`, `*.jks`, `*.keystore`, `*.pem`, `*.key`, `local.properties`, `docs-private/`, `docs/private/`, and `*secret*`/`*token*`/`*credential*` filenames).
  - Enforces 5 MiB default size limit (`DEFAULT_MAX_BYTES`) and 15s conversion timeout (`DEFAULT_TIMEOUT_SECONDS`).
  - Validates binary signatures (`%PDF-`/`%%EOF` for PDF; `PK\x03\x04`, required OOXML parts, XML DTD/ENTITY blocking, ZipSlip, and ZIP bomb ratio/size limits for `.docx`/`.pptx`/`.xlsx`).
  - Uses `MarkItDown.convert_stream(..., file_extension=...)` rather than the permissive URL-capable `convert()`.
- **Restricted Temp Storage:** Stages files in `0o700` temporary directories with `0o600` file permissions and deletes them in a `finally:` block.
- **No Content Logging:** Structured audit logs emit only `conversion_id`, `format`, `size_bytes`, `sha256_prefix`, `status`, and `reason_code` — never document contents or raw paths.
- **XSS & Prompt-Injection Defenses:** `sanitize_converted_markdown()` (and `sanitizeConvertedMarkdown()` in [backend/src/security/xss.ts](file:///Users/abrar/antigravity/LunaCare/backend/src/security/xss.ts)) strips dangerous HTML/event handlers, neutralizes tracking images and unsafe links (`javascript:`, `data:`, `vbscript:`, `file:`, `http://`), escapes remaining raw `<`/`>`, flags prompt-injection directives (`PROMPT_INJECTION_DETECTED`), and frames output inside an untrusted-data provenance header.
- **Cross-User Isolation:** `InMemoryConversionStore.get_result()` enforces `requester_id == record.owner_id` and raises `CrossUserAccessError` on mismatch.

---

## 5. Optional `markitdown-mcp` Evaluation

The official `markitdown-mcp` server is **not** enabled by default in [mcp/config/servers.example.json](file:///Users/abrar/antigravity/LunaCare/mcp/config/servers.example.json):

- `markitdown-mcp` accepts `file:`, `http:`, `https:`, and `data:` URIs, creating SSRF and arbitrary local file-read risks if misconfigured.
- If ever used for trusted local development, it **must** use `STDIO` transport only (never public HTTP/SSE), run under restricted filesystem permissions, and remain blocked from `.env`, GitHub/AWS/Cloudflare tokens, database secrets, Android signing keys (`*.jks`, `*.keystore`), and `docs-private/`.
- [mcp/policies/filesystem-policy.json](file:///Users/abrar/antigravity/LunaCare/mcp/policies/filesystem-policy.json) and [mcp/policies/tool-allowlist.json](file:///Users/abrar/antigravity/LunaCare/mcp/policies/tool-allowlist.json) explicitly prohibit sensitive secret paths and `markitdown_convert_remote_uri`.

---

## 6. Future User-Facing UI Gate (Use Case D)

A user-facing document import flow (`Select document` -> `Validate document` -> `Convert safely` -> `Preview extracted text` -> `Save or discard`, with supported types, honest processing state, cancellation, clear errors, and privacy notice) **must remain disabled** until the backend implements:

1. Encrypted, owner-scoped durable object storage (e.g., private S3/R2 bucket with KMS + per-user authorization).
2. Explicit retention & cascading deletion wired into `DELETE /api/v1/users/me`.
3. Explicit user consent and offline/local-only processing guarantees.
