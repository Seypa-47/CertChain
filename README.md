# CertChain

CertChain is a blockchain-based digital certificate issuing and verification platform for educational and training organizations. PostgreSQL stores operational certificate data, generated PDFs live in application or object storage, and Ethereum stores a minimal immutable proof. The blockchain is a trust layer, not the application database.

## Current status

The portal supports tenant-scoped draft management, issuance, revocation, PDF download, recipient email delivery, a live organization dashboard, and an editable issuer profile. Public visitors can verify issued certificates at `/verify`. A local Hardhat chain can exercise issuance and revocation. The [Sepolia contract](https://sepolia.etherscan.io/address/0x293597447c1e39825Df82fc3791BA7AB3Eb9719f#code) is deployed and source-verified. A [hosted demo certificate](https://cert-chain-gold.vercel.app/verify/CERT-2026-000001) was issued and publicly verified against Sepolia on 2026-10-06; email and hosted revocation still need validation. The detailed phased checklist is in [`docs/implementation-checklist.md`](docs/implementation-checklist.md).

## Architecture documentation

- [`docs/architecture/system-design.md`](docs/architecture/system-design.md) - boundaries, final structure, hashing, security, and consistency.
- [`docs/architecture/external-services.md`](docs/architecture/external-services.md) - services, environment variables, and secret classification.
- [`docs/database/er-model.md`](docs/database/er-model.md) - ER model, entities, enums, constraints, and indexes.
- [`docs/api/rest-api-contracts.md`](docs/api/rest-api-contracts.md) - protected and public API contracts.
- [`docs/blockchain/certificate-registry-design.md`](docs/blockchain/certificate-registry-design.md) - storage, contract interface, rules, and tests.
- [`docs/flows/certificate-flows.md`](docs/flows/certificate-flows.md) - issuance, verification, and revocation sequences.

## Technology

- Next.js, React, TypeScript, Tailwind CSS
- Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, Maven
- PostgreSQL
- Solidity, Hardhat, OpenZeppelin, Ethereum Sepolia, web3j
- PDFBox, ZXing, JavaMailSender
- Bundled Noto Sans fonts under the SIL Open Font License for certificate PDFs

## Prerequisites

- Node.js 22 LTS or newer
- Java 21 or newer (the backend targets Java 21 bytecode)
- Docker with Docker Compose
- Git

Maven does not need to be installed globally; the repository includes the Maven wrapper.

## Local setup

1. Create local environment files without committing them:

   ```bash
   cp .env.example .env
   cp frontend/.env.example frontend/.env.local
   cp backend/.env.example backend/.env
   cp blockchain/.env.example blockchain/.env
   ```

2. Start PostgreSQL and the development email inbox:

   ```bash
   docker compose up -d postgres mailpit
   ```

   Mailpit is available at `http://localhost:8025`.

3. Generate a local JWT signing secret with at least 32 random bytes, for example `openssl rand -base64 32`, and set `JWT_SECRET_BASE64` in the backend process environment. To create a development organization and admin, also set `SPRING_PROFILES_ACTIVE=dev`, `DEV_BOOTSTRAP_ENABLED=true`, and all `DEV_ORGANIZATION_*` and `DEV_ADMIN_*` values from `backend/.env.example`. The admin password must have at least 12 characters. The bootstrap runs only with both the `dev` profile and explicit flag; it is idempotent. Never use it to configure a production account.

   The example `.env` files are templates; Spring Boot does not automatically load `backend/.env`. Export the values in your shell or configure them in your run configuration. For local HTTP, use `AUTH_COOKIE_SECURE=false`; production requires HTTPS and `AUTH_COOKIE_SECURE=true`.

4. Run the backend:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

5. Run the frontend in another terminal:

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

6. Compile and test the smart contract workspace:

   ```bash
   cd blockchain
   npm ci
   npm test
   ```

The frontend defaults to `http://localhost:3000`, the backend to `http://localhost:8080`, and the backend health endpoint to `http://localhost:8080/actuator/health`.

Certificate PDFs use `STORAGE_ROOT` for private local storage and `FRONTEND_BASE_URL` to build the exact public verification URL inside the QR code. Set the latter to your HTTPS frontend origin in deployment. The local storage key stays in PostgreSQL; files are served only through the authenticated, tenant-scoped PDF endpoint. Recipient/public PDF downloads are disabled. The renderer accepts a small PNG/JPEG data URI as an optional organization logo; it does not fetch external logo URLs. The test fixture in [`docs/screenshots/sample-certificate.pdf`](docs/screenshots/sample-certificate.pdf) contains synthetic data only.

Set `MAIL_ENABLED=true` in the backend process environment to enable recipient delivery. Local SMTP defaults to Mailpit at `localhost:1025`; inspect messages at `http://localhost:8025`. Production requires `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS`, and `MAIL_FROM` for the configured provider. Set `MAIL_TIMEOUT_MS` for bounded SMTP operations. A confirmed issuance with a ready PDF creates one delivery journal and sends a plain-text plus HTML email with the PDF attached. No public download URL is sent. SMTP failures leave the certificate issued; up to three automatic attempts occur with at least two minutes between them. An authenticated admin can resend, including an already sent message, up to five total attempts. A pending attempt is treated as in flight for two minutes, then eligible for recovery. SMTP delivery is at least once: a process crash after acceptance but before marking `SENT` can cause a duplicate; the journal prevents routine duplicate sends.

Mailpit smoke test: start `docker compose up -d postgres mailpit`, run the backend with `MAIL_ENABLED=true` and local development bootstrap enabled, issue a certificate using a local chain, then inspect `http://localhost:8025` for one message addressed to the stored recipient. Confirm its plain-text and HTML parts, verification URL, and PDF attachment. `GET /api/certificates/{id}/email` should report `SENT` and `attemptCount: 1`. Resend through the admin details page to verify the manual action; never enter a recipient address in that action.

For a transport-only smoke test with Mailpit listening on loopback ports 1025 and 8025, run `cd backend && MAILPIT_SMOKE=true ./mvnw -Dtest=MailpitSmokeIT test` (PowerShell: `$env:MAILPIT_SMOKE='true'; .\mvnw.cmd '-Dtest=MailpitSmokeIT' test`). This sends synthetic data and the checked-in sample PDF, then checks Mailpit's API for the recipient, public ID, and attachment. It does not exercise PostgreSQL journaling or an on-chain issuance. Mailpit can also run from its official Windows binary when Docker Desktop is unavailable.

The login page is at `/login`. The backend sets a short-lived HttpOnly JWT cookie and never returns the token in JSON. The browser calls `GET /api/auth/csrf` before login, logout, and later unsafe writes, then sends the returned token as `X-XSRF-TOKEN` with credentials. The portal performs a server-side `/api/auth/me` check and a client recheck; backend authorization remains authoritative. Stateless logout clears the cookie. A copied token remains valid until its short expiry, so protect the signing key and use HTTPS. For separate frontend and API subdomains, configure the cookie domain and SameSite mode to match the deployment; `SameSite=None` requires Secure. Set `CORS_ALLOWED_ORIGINS` to the exact frontend origin(s), never `*`.

## Environment and secret policy

Only `.env.example` templates are committed. Real database passwords, JWT keys, SMTP credentials, RPC credentials, deployment keys, and backend signing keys must remain in local or deployment secret stores. The Ethereum private key is used only by backend/deployment processes and is never prefixed with `NEXT_PUBLIC_` or sent to the browser.

For the free-tier deployment record and provisioning, smoke-test, backup, and rollback runbook, see [`docs/deployment/free-tier.md`](docs/deployment/free-tier.md). The [Vercel frontend](https://cert-chain-gold.vercel.app) and [Render API](https://certchain-api-06gv.onrender.com/actuator/health) are live with Neon PostgreSQL. Production admin login, draft creation, [Sepolia issuance](https://sepolia.etherscan.io/tx/0x3e690ae42fda4f90066c922bc3310e4b2384160cd47c98645fdb1f15819c6690), and [public valid verification](https://cert-chain-gold.vercel.app/verify/CERT-2026-000001) were verified. The portal reports the hosted PDF ready; its downloaded contents and QR remain uninspected. SMTP is disabled, and the full revoke demo remains pending. Hosted PDFs use `STORAGE_TYPE=s3` with a private bucket; local development continues to use `STORAGE_TYPE=local`. The hosted frontend uses a same-origin `/api` rewrite and the backend continues to enforce authentication, tenancy, and CSRF.

## Development commands

```bash
# Frontend
cd frontend && npm run lint && npm run build && npm test

# Backend
cd backend && ./mvnw test

# Blockchain
cd blockchain && npm run typecheck && npm test && npm run coverage
```

Backend tests include a Docker-independent H2 context smoke test. PostgreSQL integration tests use Testcontainers to start a fresh PostgreSQL container, apply Flyway migrations, and run Hibernate schema validation before checking database constraints, tenant queries, concurrent public ID allocation, and authentication/tenant security. Start Docker Desktop before running `cd backend && ./mvnw test` (on Windows, `cd backend; .\mvnw.cmd test`). Check the Surefire reports in `backend/target/surefire-reports/` and confirm `PostgresDomainIntegrationTests`, `AuthIntegrationTests`, and `DevBootstrapIntegrationTests` each report zero skipped tests. The main application runs Flyway on startup against PostgreSQL. To apply migrations locally, start PostgreSQL with `docker compose up -d postgres`, then run `cd backend && ./mvnw spring-boot:run`.

When Docker Desktop is unavailable, the opt-in `ExternalPostgresIntegrationIT` and `ExternalSecurityIntegrationIT` exercise migration, Hibernate validation, PostgreSQL constraints, concurrency, authentication, CSRF/CORS, and tenant isolation against an **isolated disposable PostgreSQL database**. Set `CERTCHAIN_EXTERNAL_POSTGRES=true` plus `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` for that database, then run `cd backend && ./mvnw '-Dtest=ExternalPostgresIntegrationIT,ExternalSecurityIntegrationIT' test` (Windows: `.\mvnw.cmd '-Dtest=ExternalPostgresIntegrationIT,ExternalSecurityIntegrationIT' test`). These tests insert synthetic records and should never point at a shared or production database. They supplement the Testcontainers suite; a skipped Testcontainers test is still a skipped test.

The issuance tests use a fake gateway for failures, concurrency, and recovery. `LocalChainIntegrationIT` is an opt-in end-to-end test using a local Hardhat JSON-RPC node and a fresh PostgreSQL Testcontainer; no Sepolia funds are needed. Follow the local node and Ignition deployment instructions in [`blockchain/README.md`](blockchain/README.md), then set `CERTCHAIN_LOCAL_RPC`, `CERTCHAIN_LOCAL_CONTRACT`, and `CERTCHAIN_LOCAL_ISSUER_KEY` in the test process environment. The issuer key must match the local signer configured in Ignition. Run `cd backend && ./mvnw -Dtest=LocalChainIntegrationIT test` (Windows: `.\mvnw.cmd '-Dtest=LocalChainIntegrationIT' test`). Use `BLOCKCHAIN_ENABLED=true`, the node RPC URL, deployed address, local chain ID `31337`, and issuer key only in a local backend process when testing the UI. Set `BLOCKCHAIN_DEPLOYMENT_BLOCK` to the deployment block and use a low confirmation threshold for a local node. Never commit the key or a local deployment address.

For the same local chain with external PostgreSQL, set the disposable database variables above and the three `CERTCHAIN_LOCAL_*` variables, then run `cd backend && ./mvnw '-Dtest=ExternalLocalChainIntegrationIT' test` (Windows: `.\mvnw.cmd '-Dtest=ExternalLocalChainIntegrationIT' test`). Use a fresh local Hardhat deployment when recreating the database, since certificate keys already issued on a persistent local chain cannot be issued again. The Phase 12 synthetic walkthrough and sanitized screenshots are in [`docs/screenshots/phase12-local-evidence.md`](docs/screenshots/phase12-local-evidence.md).

The checked-in `CertificateRegistry` Java wrapper was generated from the compiled `blockchain/artifacts/contracts/CertificateRegistry.sol/CertificateRegistry.json` ABI and bytecode using the pinned web3j 5.0.3 code generator. If the contract ABI changes, recompile the contract, export its `abi` and `bytecode` fields to `backend/target/codegen/CertificateRegistry.abi` and `.bin`, regenerate with `cd backend && ./mvnw org.codehaus.mojo:exec-maven-plugin:3.6.3:java -Dexec.mainClass=org.web3j.codegen.SolidityFunctionWrapperGenerator -Dexec.classpathScope=test -Dexec.args="-b target/codegen/CertificateRegistry.bin -a target/codegen/CertificateRegistry.abi -o target/codegen/generated -p com.certchain.blockchain.generated"`, then replace the checked-in wrapper and rerun backend/local-chain tests.

Revocation tests also cover an issued certificate on the local chain. Admins confirm a private reason on the certificate details page; the backend records a `REVOKE` transaction and updates `revokedAt` only after validating its receipt and event. Public `/verify/{certificateId}` reads the current proof and computes `REVOKED`, `EXPIRED`, or `VALID` on demand. An uncertain transaction stays pending for reconciliation.

## Assignment report and demonstration

The evidence-based [assignment report draft](docs/report/assignment-report-draft.md) follows the 11 required sections. The [demo recording script](docs/report/demo-recording-script.md) gives the required action order and a publication checklist. The original team repository is [github.com/Thna17/CertChain](https://github.com/Thna17/CertChain); the free deployment uses [github.com/Seypa-47/CertChain](https://github.com/Seypa-47/CertChain). The public frontend, API, verified [Sepolia contract](https://sepolia.etherscan.io/address/0x293597447c1e39825Df82fc3791BA7AB3Eb9719f#code), and synthetic [issue](https://sepolia.etherscan.io/tx/0xfda5f9db110021bd64a4e3934ff54a157ae89f03f62c47db29425f66f2ab284f) and [revoke](https://sepolia.etherscan.io/tx/0x2101d7ca403142f5dc3e6469bb4c602b8cd0b5f993dc75f5966744bf0f949ede) transactions are available. A public demo video and full production smoke evidence remain pending; the report stays a draft.

Implemented features include organization login and tenant isolation, draft certificate management, a live dashboard, deterministic SHA-256 proofs, role-controlled issuance and revocation, transaction reconciliation, public proof verification, PDF/QR generation, private PDF download, and bounded recipient email delivery. The [system diagrams](docs/architecture/system-design.md), [ER model](docs/database/er-model.md), and [issuance/verification/revocation flows](docs/flows/certificate-flows.md) document the trust boundaries. PostgreSQL stores operational and private data, private storage holds PDFs, and the Ethereum contract stores only proof and revocation state.

The [local browser evidence](docs/screenshots/phase12-local-evidence.md) contains synthetic valid, expired, revoked, not-found, and proof-mismatch verification screenshots. A [login screenshot](docs/screenshots/phase14-login-local.png) was captured from the running frontend, and the [sample PDF/QR render](docs/screenshots/sample-certificate.png) is a synthetic fixture. Dashboard, create, admin details, transaction, and explorer captures remain to be made from a controlled run. Do not use local-chain hashes as public Sepolia evidence.

Environment templates are in [frontend/.env.example](frontend/.env.example), [backend/.env.example](backend/.env.example), and [blockchain/.env.example](blockchain/.env.example). For hosting, `API_PROXY_ORIGIN` is the backend HTTPS origin, `NEXT_PUBLIC_API_BASE_URL=/api`, and `API_INTERNAL_BASE_URL` is the backend `/api` URL. `STORAGE_TYPE=s3` selects private object storage with the `STORAGE_S3_*` variables; local development uses `STORAGE_TYPE=local` and `STORAGE_ROOT`. Backend-only secrets include `DATABASE_PASSWORD`, `JWT_SECRET_BASE64`, `BLOCKCHAIN_RPC_URL`, `BLOCKCHAIN_ISSUER_PRIVATE_KEY`, and SMTP credentials. The [free-tier runbook](docs/deployment/free-tier.md) covers one-time admin provisioning, Sepolia deployment, verification, backup, rollback, and smoke testing without committing secrets.

At the report evidence cutoff (`80ab457`), Git history records [Hong Than Brathna's foundation commit](https://github.com/Thna17/CertChain/commit/8b67919e9efa48c301b57214065fac6002ad2e80) and 14 later implementation/testing/deployment-preparation commits under the `Seypa47` identity. The report describes those authored diffs without assigning unsupported effort percentages. The public demo video URL is pending; insert and verify it before issuing the final PDF.
