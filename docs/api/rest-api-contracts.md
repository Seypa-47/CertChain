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
| `GET` | `/api/dashboard` | Counts and recent certificates from real data. |
| `POST` | `/api/certificates` | Create a draft. Server allocates the certificate ID. |
| `GET` | `/api/certificates?page=0&size=20&query=&lifecycle=&status=` | Paginated tenant-scoped list/search. |
| `GET` | `/api/certificates/{id}` | Full authorized details by internal UUID. |
| `PATCH` | `/api/certificates/{id}` | Update mutable draft fields only. |
| `GET` | `/api/certificates/{id}/issuance` | Read issuance journal and chain metadata for an owned certificate. |
| `POST` | `/api/certificates/{id}/issue` | Start or resume issuance; an unknown receipt returns pending state without another submission. |
| `POST` | `/api/certificates/{id}/reconcile` | Check the receipt or on-chain event for a pending or failed attempt. |
| `POST` | `/api/certificates/{id}/revoke` | Revoke an issued certificate with confirmation. |
| `GET` | `/api/certificates/{id}/revocation` | Read the private revocation journal state. |
| `POST` | `/api/certificates/{id}/revocation/reconcile` | Recheck a pending or failed revocation without resubmitting. |
| `GET` | `/api/certificates/{id}/pdf` | Download the generated PDF after issuance. |

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
  "guidance": "Proof confirmed on chain."
}
```

`GET /api/certificates/{id}/issuance` is safe to poll. Proof-relevant fields cannot be changed after issuance starts. Issuance does not generate PDF or email in this phase, and public status is derived later rather than stored.

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
  "blockchainVerified": true,
  "issuedAt": "2026-09-20T08:00:00Z",
  "revokedAt": null,
  "network": "sepolia",
  "chainId": 11155111,
  "contractAddress": "0x..."
}
```

Unknown or malformed public certificate IDs return `404`. Public responses never include email, internal UUIDs, password-related data, failure internals, or private revocation notes.

`GET /api/public/certificates/{certificateId}` currently returns an issued certificate only when its recomputed hash, on-chain hash, issuance time, expiry, and revocation flag agree. It includes public names, dates, derived `status`, `blockchainVerified`, and network identity. A mismatch returns `409 PROOF_MISMATCH`; unavailable RPC returns `503`. Status is computed on each request: `REVOKED` wins over `EXPIRED`, and expiry begins at 00:00 UTC on the day after `expiryDate`.

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

