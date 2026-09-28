# REST API Contracts

Base path: `/api`. JSON uses camelCase. Dates use `YYYY-MM-DD`; timestamps use ISO-8601 UTC. Internal resources use UUIDs while public verification uses the human-readable certificate ID.

## Authentication

Authentication uses a short-lived JWT in a `Secure`, `HttpOnly` cookie (`CERTCHAIN_AUTH` by default). Browser JavaScript cannot read the JWT. The cookie name, Secure flag, SameSite mode, and domain are configurable. All browser requests use `credentials: include`; unsafe requests send `X-XSRF-TOKEN` from the CSRF endpoint. Only configured origins may make credentialed cross-origin requests. No authentication route accepts an organization ID from the client.

### `GET /api/auth/csrf`

Public. Returns `200` with `{ "token": "..." }` and sets the CSRF cookie. Fetch this before login and before other unsafe requests. The CSRF token is separate from the JWT.

### `POST /api/auth/login`

Public, but requires the CSRF header and cookie. Invalid email, password, unknown user, or disabled user produce the same `401 INVALID_CREDENTIALS` response.

Request:

```json
{
  "email": "admin@kit.edu.kh",
  "password": "user-entered-password"
}
```

Response `200`:

```json
{
  "id": "uuid",
  "organizationId": "uuid",
  "name": "Organization Admin",
  "email": "admin@kit.edu.kh",
  "role": "ORG_ADMIN"
}
```

The response also sets the authentication cookie. It does not return an access token in JSON.

### `GET /api/auth/me`

Requires authentication. Returns the same current-user object as login. An absent, expired, tampered, or disabled-user token returns `401 UNAUTHORIZED`.

### `POST /api/auth/logout`

Requires authentication and CSRF. Returns `204` and expires the authentication cookie. Stateless tokens already copied elsewhere remain usable until expiry; the configured lifetime is at most one hour and defaults to 15 minutes.

## Organization portal

All routes below require the authentication cookie and `ORG_ADMIN` role. Unsafe methods also require CSRF. The backend derives tenant identity from the verified token and applies it in repository queries. Requests cannot select a different organization.

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/organization` | Current organization profile. |
| `PATCH` | `/api/organization` | Update allowed profile fields. |
| `GET` | `/api/dashboard` | Tenant counts and the five most recent certificates and blockchain transactions. |
| `POST` | `/api/certificates` | Create a draft. Server allocates the certificate ID. |
| `GET` | `/api/certificates?page=0&size=20&query=&lifecycle=&sort=createdAt&direction=desc` | Paginated tenant-scoped list/search. |
| `GET` | `/api/certificates/{id}` | Full authorized details by internal UUID. |
| `PATCH` | `/api/certificates/{id}` | Update mutable draft fields only. |
| `GET` | `/api/certificates/{id}/issuance` | Read issuance journal and chain metadata for an owned certificate. |
| `POST` | `/api/certificates/{id}/issue` | Start or resume issuance; an unknown receipt returns pending state without another submission. |
| `POST` | `/api/certificates/{id}/reconcile` | Check the receipt or on-chain event for a pending or failed attempt. |
| `POST` | `/api/certificates/{id}/revoke` | Revoke an issued certificate with confirmation. |
| `GET` | `/api/certificates/{id}/revocation` | Read the private revocation journal state. |
| `POST` | `/api/certificates/{id}/revocation/reconcile` | Recheck a pending or failed revocation without resubmitting. |
| `GET` | `/api/certificates/{id}/pdf` | Download the generated PDF after issuance. |
| `GET` | `/api/certificates/{id}/pdf/status` | Read artifact state without triggering generation. |
| `POST` | `/api/certificates/{id}/pdf/retry` | Regenerate a missing or failed artifact without resubmitting a chain transaction. |
| `GET` | `/api/certificates/{id}/email` | Read issued-certificate delivery status and attempt count. |
| `POST` | `/api/certificates/{id}/email/resend` | Send to the stored recipient only; requires CSRF and has a bounded attempt limit. |

`GET /api/dashboard` returns `totalIssued`, `valid`, `expired`, `revoked`, `recentCertificates`, and `recentTransactions`. Counts are computed in database queries for the authenticated organization and include only `ISSUED` certificates. Valid means not revoked and expiry is null or today/later; expired means not revoked and expiry precedes today; revoked takes priority. Recent lists are capped at five and include internal certificate UUIDs only within the authenticated portal. The date boundary uses the backend's injected UTC clock.

`GET /api/organization` returns `{ "id": "uuid", "name": "...", "email": "...", "walletAddress": null, "logoUrl": null }`. `PATCH /api/organization` accepts only `name` (1–200 characters), `email` (valid, at most 320 characters), and an optional 42-character Ethereum `walletAddress`; it requires authentication and CSRF. The backend normalizes name and email and selects the organization from the verified principal. The logo remains read-only in this phase. Invalid fields return `400 VALIDATION_ERROR`.

Create request:

```json
{
  "recipientName": "Hong Thanbrathna",
  "recipientEmail": "recipient@example.com",
  "programName": "Blockchain Fundamentals",
  "description": "Successfully completed the training program.",
  "issueDate": "2026-09-20",
  "expiryDate": "2029-09-20"
}
```

`POST /api/certificates` returns `201`, a `Location` header, and the full `CertificateResponse`. The server generates `certificateId` using the global yearly sequence; clients cannot submit or change it. `PATCH /api/certificates/{id}` accepts the same fields and returns the updated response only while lifecycle is `DRAFT`; otherwise it returns `409 CERTIFICATE_NOT_DRAFT`. Unknown or cross-tenant UUIDs return the same `404 CERTIFICATE_NOT_FOUND` envelope.

Names and program are required and limited to 200 and 300 characters. Recipient email must be valid and at most 320 characters. Description is optional and limited to 2000 characters. Issue date is required and cannot be in the future; expiry may be null but cannot precede issue date. The backend trims and normalizes name/program text, lowercases email, and trims description. Invalid requests return `400 VALIDATION_ERROR` with field issues. Draft create/update does not compute a proof hash.

The list returns `{ "content": [...], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`. Each item contains internal UUID, public ID, recipient, program, issue/expiry dates, lifecycle, and creation time. `page` starts at 0, `size` must be 1–100, and `query` is at most 200 characters. Search matches recipient name, program, and public ID case-insensitively, treating `%` and `_` literally. `lifecycle` filters stored lifecycle only; public validity is a separate derived concept. Sort fields are limited to `createdAt`, `certificateId`, `recipientName`, `programName`, `issueDate`, and `lifecycle`; direction is `asc` or `desc`. Invalid list parameters return `400`.

Issue, reconciliation, and progress responses use the same shape. `POST /issue` and `POST /reconcile` return `202` while `ISSUING`, or `200` for a terminal state. The backend may return `503` when the blockchain gateway is disabled or unavailable. A chain timeout is `ISSUING`, not `ISSUED`.

```json
{
  "id": "uuid",
  "certificateId": "CERT-2026-000001",
  "lifecycle": "ISSUED",
  "certificateHash": "64-lowercase-hex-characters",
  "transactionStatus": "CONFIRMED",
  "transactionHash": "0x...",
  "network": "sepolia",
  "chainId": 11155111,
  "contractAddress": "0x...",
  "blockNumber": 123,
  "blockTimestamp": "2026-09-20T08:00:00Z",
  "explorerUrl": "https://sepolia.etherscan.io/tx/0x...",
  "failureReason": null,
  "guidance": "Proof confirmed on chain.",
  "artifactStatus": "READY",
  "artifactError": null
}
```

`GET /api/certificates/{id}/issuance` is safe to poll. Proof-relevant fields cannot be changed after issuance starts. Once issuance is confirmed, PDF generation runs separately; a PDF failure leaves blockchain issuance intact and appears as `artifactStatus: "FAILED"`. `GET /pdf/status` returns `{ "status": "PENDING|READY|FAILED", "error": null|string }`. `POST /pdf/retry` needs CSRF and is idempotent when the file exists. `GET /pdf` returns an attachment only for the owning authenticated organization and an issued certificate with a ready artifact. Unknown and cross-tenant IDs return 404; drafts return `409 CERTIFICATE_NOT_ISSUED`; unavailable artifacts return `409 ARTIFACT_NOT_READY`. Recipient and public PDF download endpoints remain unavailable; the recipient receives the PDF by email attachment after successful generation. The public verification page remains available without a PDF.

`GET /email` returns `{ "enabled": true, "status": "PENDING|SENT|FAILED|null", "attemptCount": 1, "sentAt": null, "failureReason": null, "canResend": false }`. Failure reasons are generic codes, never raw SMTP responses. `POST /email/resend` accepts no body or recipient address and always uses the certificate's stored recipient. It returns the updated status. The endpoint rejects drafts with `CERTIFICATE_NOT_ISSUED`, missing PDFs with `ARTIFACT_NOT_READY`, a recent pending send with `EMAIL_IN_PROGRESS`, exhausted attempts with `EMAIL_RETRY_LIMIT`, and disabled delivery with `EMAIL_DISABLED`. Cross-tenant IDs return 404. SMTP failure records `FAILED` without changing issuance.

Revoke request:

```json
{ "reason": "Issued to the wrong recipient" }
```

The private `reason` is required, nonblank after trimming, and at most 1000 characters. It is stored in PostgreSQL and never sent to the contract or public API. A successful `POST /revoke` returns confirmed transaction metadata and `revokedAt`; a pending transaction returns `202` with `revokedAt: null`; a validated revert returns `200` with `transactionStatus: "FAILED"`. The response shape matches the issuance progress fields, with `revokedAt` and revocation guidance. Draft or failed issuance returns `409 CERTIFICATE_NOT_ISSUED`; already revoked returns `409 CERTIFICATE_ALREADY_REVOKED`; a chain proof mismatch returns `409 CHAIN_PROOF_MISMATCH`. Unknown and cross-tenant IDs both return `404 CERTIFICATE_NOT_FOUND`.

## Public verification

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/public/certificates/{certificateId}` | One verification response; performs hash and chain checks. |

Response `200`:

```json
{
  "certificateId": "CERT-2026-000001",
  "recipientName": "Hong Thanbrathna",
  "programName": "Blockchain Fundamentals",
  "organizationName": "KIT Training Center",
  "issueDate": "2026-09-20",
  "expiryDate": "2029-09-20",
  "status": "VALID",
  "proofResult": "VERIFIED",
  "blockchainVerified": true,
  "issuedAt": "2026-09-20T08:00:00Z",
  "revokedAt": null,
  "network": "sepolia",
  "chainId": 11155111,
  "contractAddress": "0x...",
  "transactionHash": "0x...",
  "blockNumber": 123,
  "blockTimestamp": "2026-09-20T08:00:00Z",
  "explorerUrl": "https://sepolia.etherscan.io/tx/0x..."
}
```

Unknown or malformed public certificate IDs return `404`. Public responses never include email, internal UUIDs, password-related data, failure internals, or private revocation notes.

The endpoint accepts only exact uppercase `CERT-YYYY-NNNNNN` IDs with nonzero sequence. Draft, unknown, and malformed IDs all return the same 404 response. For issued certificates it recomputes the canonical SHA-256, checks the stored hash and version, confirms journal chain/contract metadata, and compares the on-chain hash, issuer, issue time, expiry, and revocation state. A trusted result returns `proofResult: "VERIFIED"` with authoritative confirmed transaction metadata. A disagreement returns `PROOF_MISMATCH`; unavailable RPC returns `VERIFICATION_UNAVAILABLE`. Both have `blockchainVerified: false`, `status: null`, and no transaction metadata. Public names and dates may still be displayed with a clear failed-proof warning. No response claims `VALID` unless the proof passes. Revoked status takes priority over expired; expiry begins at 00:00 UTC on the day after `expiryDate`. Explorer links are built only for recognized network/chain combinations and validated transaction hashes. The public read endpoint limits each directly connected IP address to 120 requests per minute (single-instance in-memory limit; configure an edge limit for multi-instance deployments).

## Error envelope

```json
{
  "timestamp": "2026-09-20T08:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/certificates",
  "details": [
    { "field": "recipientEmail", "message": "must be a valid email address" }
  ],
  "traceId": "server-generated-correlation-id"
}
```

Expected stable error codes include `VALIDATION_ERROR`, `INVALID_CREDENTIALS`, `ACCESS_DENIED`, `CERTIFICATE_NOT_FOUND`, `CERTIFICATE_ALREADY_ISSUED`, `CERTIFICATE_ALREADY_REVOKED`, `BLOCKCHAIN_TRANSACTION_FAILED`, `BLOCKCHAIN_VERIFICATION_FAILED`, `PDF_GENERATION_FAILED`, and `INTERNAL_ERROR`.

Phase 3 uses `UNAUTHORIZED` for missing/invalid authentication and `CSRF_INVALID` for missing/invalid CSRF. Error responses contain no stack traces or credentials. Routes listed for later phases remain contracts until implemented.

