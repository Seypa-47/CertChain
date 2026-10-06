# Implementation Checklist

## Phase 1 - Foundation

- [x] Read and reconcile the assignment brief with the product specification.
- [x] Define architecture, ER model, API contracts, smart contract interface, and workflows.
- [x] Initialize Git repository and root ignore rules.
- [x] Scaffold Next.js/TypeScript/Tailwind frontend.
- [x] Scaffold Spring Boot/Maven backend with health check and test profile.
- [x] Scaffold Hardhat/TypeScript blockchain project.
- [x] Configure local PostgreSQL with Docker Compose.
- [x] Add environment templates and setup documentation.
- [x] Run baseline frontend, backend, and blockchain checks.

## Phase 2 - Database and domain

- [x] Add Flyway migrations, constraints, and indexes.
- [x] Implement entities, enums, repositories, DTOs, and mappers.
- [x] Implement concurrency-safe certificate ID allocation.
- [x] Add repository and container-backed integration test code.
- [ ] Execute the complete PostgreSQL Testcontainers suite without skips. The 3 October run still skipped Docker-dependent classes; separate external PostgreSQL runs passed in September.

## Phase 3 - Authentication and tenancy

- [x] Add organization/user bootstrap flow for development.
- [x] Implement BCrypt authentication and short-lived JWT access tokens.
- [x] Configure stateless Spring Security and strict CORS.
- [x] Enforce organization ownership in implemented protected operations.
- [x] Build login and protected portal layout.
- [x] Test invalid credentials, disabled users, token validation, and cross-tenant denial.

## Phase 4 - Draft certificate management

- [x] Create/list/view/search/update draft APIs.
- [x] Freeze proof-relevant fields once issuance begins.
- [x] Build create, list, details, loading, empty, and error states.
- [x] Test validation, pagination, filtering, and authorization.

## Phase 5 - Smart contract

- [x] Implement `CertificateRegistry.sol` with role-based access control.
- [x] Implement custom errors, events, issue/read/verify/revoke functions.
- [x] Complete contract unit tests.
- [x] Add and smoke-test a local Ignition deployment module.
- [x] Generate the Java wrapper for backend blockchain integration.
- [x] Document the Sepolia deployment procedure without committing secrets.
- [ ] Test deployment on Sepolia.

## Phase 6 - Issuance and blockchain integration

- [x] Implement versioned canonicalization and SHA-256 service.
- [x] Test deterministic hashing, changed fields, normalization, escaping, and ordering.
- [x] Implement web3j client behind a blockchain gateway interface.
- [x] Implement issuance lifecycle, transaction journal, receipt/event validation, and reconciliation.
- [x] Test failed, timed-out, duplicated, and recovered submissions with PostgreSQL and a local Hardhat chain.

## Phase 7 - Public verification

- [x] Implement the public verification DTO and API.
- [x] Recompute local hash and compare database/on-chain proof.
- [x] Implement dynamic `REVOKED > EXPIRED > VALID` status.
- [x] Build `/verify` and `/verify/[certificateId]` with accessible states.
- [x] Add authoritative explorer links and unavailable/mismatch handling.

## Phase 8 - PDF and QR

- [x] Implement storage abstraction.
- [x] Generate verification QR using ZXing.
- [x] Generate professional PDF using PDFBox and embed QR.
- [x] Implement authorized PDF download and artifact retry.
- [x] Add PDF content/render checks.

## Phase 9 - Revocation and expiration

- [x] Implement confirmed on-chain revocation workflow and journal.
- [x] Persist private reason, confirmed time, and transaction metadata.
- [x] Add admin confirmation UI and public revoked state.
- [x] Test expired, revoked-and-expired, unauthorized, duplicate, and failed revocations.

## Phase 10 - Email

- [x] Implement templated issuance email with the generated PDF attachment.
- [x] Record delivery attempts without rolling back issuance.
- [x] Add bounded retry, authorized resend, and development SMTP configuration.
- [ ] Complete successful and failed delivery testing against Mailpit and PostgreSQL. Successful local issuance-to-Mailpit delivery with a PDF attachment was observed on 29 September; the full delivery failure/recovery integration gate remains open while Docker is unavailable.

## Phase 11 - Dashboard and UI polish

- [x] Add tenant-scoped database counts and bounded recent activity.
- [x] Complete responsive navigation and validated organization profile.
- [x] Audit keyboard focus, status text, loading, empty, error, and confirmation states at mobile, tablet, and desktop widths.

## Phase 12 - Verification and security review

- [ ] Run the full backend suite with PostgreSQL Testcontainers. Unit tests and opt-in external PostgreSQL/local-chain integration tests passed, but the Docker-backed classes skipped while Docker Desktop was unavailable.
- [x] Run contract tests and coverage.
- [x] Run frontend component and main-flow tests.
- [x] Execute the unknown, issue/valid, expired, revoke/reverify, and tamper demo scenarios with synthetic data.
- [x] Review secret handling, logging, CORS, authorization, validation, and dependency risks; record the remaining limitations in the Phase 12 evidence.

## Phase 13 - Deployment

- [x] Provision Neon PostgreSQL and private object storage; Flyway and a generated PDF object were observed in production.
- [x] Deploy and verify the contract on Sepolia, including roles, source, and issue/revoke events.
- [ ] Configure backend secrets and HTTPS deployment.
- [x] Deploy the Vercel frontend on Hobby and configure its production API origin.
- [ ] Run production smoke tests and backup/recovery checks.

## Phase 14 - Assignment documentation

- [ ] Capture architecture, flow, ER, blockchain, contract, and UI evidence.
- [x] Write implementation summary and contribution report from Git evidence (draft; update after publication).
- [ ] Add public GitHub and demo URLs.
- [ ] Record focused demo flow.
- [ ] Render and inspect the final submission PDF.
