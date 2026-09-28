# CertChain System Design

## 1. Scope and architectural decisions

CertChain is a modular monolith with three deployable parts:

- a Next.js web application for the organization portal and public verification pages;
- a Spring Boot REST API that owns authentication, authorization, certificate workflows, persistence, artifact generation, email, and blockchain signing;
- an Ethereum smart contract that stores the minimum immutable proof needed to verify issuance and revocation.

PostgreSQL is the operational source of truth. Ethereum is the immutable proof and revocation layer. PDF files are generated artifacts, not authoritative records.

Two separate state concepts prevent partial failures from being presented as successful certificates:

- `CertificateLifecycle`: `DRAFT`, `ISSUING`, `ISSUED`, `ISSUE_FAILED`.
- Public `CertificateStatus`: derived as `REVOKED`, then `EXPIRED`, then `VALID`; it exists only for an `ISSUED` certificate.

The first release uses one backend-managed Ethereum signer with `ISSUER_ROLE`. Application authorization still isolates every administrator to their organization. Supporting one on-chain wallet per organization can be added later without exposing keys to the browser.

## 2. Final folder structure

```text
CertChain/
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── (public)/
│   │   │   │   ├── page.tsx
│   │   │   │   └── verify/
│   │   │   │       ├── page.tsx
│   │   │   │       └── [certificateId]/page.tsx
│   │   │   ├── login/page.tsx
│   │   │   └── (portal)/
│   │   │       ├── dashboard/page.tsx
│   │   │       ├── certificates/
│   │   │       │   ├── page.tsx
│   │   │       │   ├── new/page.tsx
│   │   │       │   └── [id]/page.tsx
│   │   │       └── profile/page.tsx
│   │   ├── components/
│   │   │   ├── certificates/
│   │   │   ├── layout/
│   │   │   └── ui/
│   │   ├── lib/
│   │   ├── services/
│   │   └── types/
│   ├── public/
│   ├── package.json
│   └── .env.example
├── backend/
│   ├── src/main/java/com/certchain/
│   │   ├── auth/
│   │   ├── blockchain/
│   │   ├── certificate/
│   │   ├── common/
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   └── security/
│   │   ├── organization/
│   │   ├── transaction/
│   │   └── user/
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   └── application.yml
│   ├── src/test/java/com/certchain/
│   ├── pom.xml
│   └── .env.example
├── blockchain/
│   ├── contracts/CertificateRegistry.sol
│   ├── ignition/modules/CertificateRegistry.ts
│   ├── test/CertificateRegistry.ts
│   ├── hardhat.config.ts
│   ├── package.json
│   └── .env.example
├── docs/
│   ├── api/
│   ├── architecture/
│   ├── blockchain/
│   ├── database/
│   ├── flows/
│   └── screenshots/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

## 3. Component responsibilities

```mermaid
flowchart LR
    Public[Public verifier] --> Web[Next.js]
    Admin[Organization admin] --> Web
    Web -->|REST/JSON over HTTPS| API[Spring Boot API]
    API --> DB[(PostgreSQL)]
    API --> Files[(Local or object storage)]
    API --> Mail[SMTP provider]
    API -->|web3j + server signer| RPC[Ethereum RPC]
    RPC --> Contract[CertificateRegistry]
    Web --> Explorer[Sepolia explorer]
```

- The frontend renders views, validates for usability, and calls the API. It never decides authorization and never receives signing keys.
- The backend owns canonicalization and hashing. Clients cannot submit an authoritative certificate hash or transaction hash.
- The database stores private and operational data, including recipient email and revocation reason.
- The contract stores only a hashed certificate key, a SHA-256 certificate hash, timestamps, revocation state, and issuer address.
- File storage starts as a configurable local directory for development and is replaceable with object storage through a storage interface.

## 4. Security boundaries

- Public endpoints expose only fields required to verify a certificate. Recipient email, internal IDs, and revocation reason are excluded.
- Organization endpoints require a verified JWT in an HttpOnly cookie and enforce organization ownership in the repository query using the typed principal, not client-supplied identifiers. The current token includes issuer, subject/user ID, organization ID, role, issue time, and expiry; signature, algorithm, issuer, and expiry are validated. A disabled or changed user loses access.
- Passwords use BCrypt. A development organization/admin bootstrap runs only with the `dev` profile and an explicit flag; credentials come from environment variables. JWT secrets, RPC URLs, SMTP credentials, database credentials, and wallet keys are environment variables.
- Authentication cookies use configurable Secure, SameSite, and domain attributes. Stateless sessions retain CSRF protection for unsafe methods with a CSRF cookie and `X-XSRF-TOKEN` header. CORS accepts only configured origins with credentials. Production uses HTTPS. Error responses do not expose stack traces. The frontend guards portal routes for navigation; backend authorization is authoritative.
- The signing wallet is server-side and receives only `ISSUER_ROLE`; the deployment/admin wallet should be separate in production.
- Contract calls are idempotent by deterministic `certificateKey`; duplicate issuance reverts.

## 5. Consistency and failure recovery

Issuance crosses PostgreSQL, Ethereum, file storage, and email, so it cannot be one database transaction. CertChain uses a state machine and transaction journal:

1. Lock the draft certificate and change lifecycle from `DRAFT` or retryable `ISSUE_FAILED` to `ISSUING`.
2. Freeze immutable issue fields, build canonical data, and persist its hash.
3. Create a `BlockchainTransaction` row with type `ISSUE` and state `CREATED`.
4. Submit the contract call, then store the transaction hash and state `SUBMITTED` immediately.
5. Wait briefly for a successful receipt and validate the chain, destination contract, transaction status, confirmation count, issued event, proof fields, signer, block metadata, and on-chain record. A reverted or invalid receipt records failure. A missing receipt remains `ISSUING` for reconciliation and never sets `ISSUED`.
6. Persist the block metadata, transition the certificate to `ISSUED`, and commit.
7. Generate QR/PDF and store their location. Artifact generation can be retried without another blockchain transaction.
8. Send email after issuance. Email failure is logged and retryable; it does not undo an immutable issuance.

The `CREATED` journal commits before the RPC submission; no database transaction stays open while waiting for Ethereum. If the process crashes after submission, reconciliation looks up the known transaction hash or the deterministic certificate key/event and repairs local state. A request that finds an existing on-chain key does not resubmit. The contract rejects duplicate keys, so a late transaction cannot create a second on-chain certificate. Operators must investigate a `CREATED` attempt with no discoverable event before forcing another submission, since a pending transaction may still mine. Proof fields remain frozen from the first `ISSUING` transition onward, including after a failed attempt. Expiry is the final UTC second of the stated expiry date; a null date maps to zero.

Revocation follows the same journal pattern. A private, trimmed reason is saved before submission, but `revokedAt` is updated only after a successful receipt, required confirmations, a validated `CertificateRevoked` event, and an on-chain revoked record. Missing receipts remain pending; reconciliation searches by transaction hash or indexed certificate key after a crash. A failed revert can be retried after checking chain state. An uncertain submission is not automatically sent again. Public verification compares the recomputed hash, issuance and expiry timestamps, and revocation flag with the chain before returning a status. Status is derived on each read using the injected UTC clock: `REVOKED`, then `EXPIRED` when the current UTC date is after `expiryDate`, then `VALID`. No scheduled status mutation is needed.

## 6. Certificate identity and canonical hash

- Public ID format: `CERT-YYYY-NNNNNN`.
- One global database-backed sequence row per year allocates numbers safely across all organizations using a PostgreSQL upsert; `COUNT(*) + 1` is forbidden. Gaps are acceptable because numbers identify certificates rather than count completed issuances.
- Contract key: `keccak256(UTF-8(uppercase(trim(certificateId))))`.
- Certificate proof: `SHA-256(UTF-8(canonicalData))`, stored as `bytes32`.

Canonical data version 1 uses an explicit field order and escaping rules:

```text
v1|certificateId=<value>|recipientName=<value>|programName=<value>|organizationId=<uuid>|issueDate=<yyyy-MM-dd>|expiryDate=<yyyy-MM-dd-or-empty>
```

Text values are Unicode-normalized with NFC, trimmed, internal whitespace collapsed to a single ASCII space, and reserved characters (`\\`, `|`, `=`) escaped. Certificate IDs are uppercased with `Locale.ROOT`; names and program titles retain case. Dates are ISO-8601 calendar dates. The version prefix allows a future algorithm without silently changing old proofs.

## 7. Verification trust model

The backend loads the certificate from PostgreSQL, rebuilds the canonical representation, recomputes SHA-256, and reads the on-chain record using the deterministic key. Blockchain verification is true only when:

- an on-chain record exists;
- the locally recomputed hash equals the database hash;
- the locally recomputed hash equals the on-chain hash;
- the database and on-chain revocation flags agree.

The response separately reports `blockchainVerified`. Public status is evaluated with revocation priority and the current clock. A database-only record, mismatched hash, failed RPC read, or unconfirmed issuance is never presented as blockchain verified.

