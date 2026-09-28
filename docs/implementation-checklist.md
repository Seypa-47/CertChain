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
- [x] Add repository and container-backed integration tests.

## Phase 3 - Authentication and tenancy

- [x] Add organization/user bootstrap flow for development.
- [x] Implement BCrypt authentication and short-lived JWT access tokens.
- [x] Configure stateless Spring Security and strict CORS.
- [x] Enforce organization ownership in implemented protected operations.
- [x] Build login and protected portal layout.
- [x] Test invalid credentials, disabled users, token validation, and cross-tenant denial.

## Phase 4 - Draft certificate management

- [ ] Create/list/view/search/update draft APIs.
- [ ] Freeze proof-relevant fields once issuance begins.
- [ ] Build create, list, details, loading, empty, and error states.
- [ ] Test validation, pagination, filtering, and authorization.

## Phase 5 - Smart contract

- [ ] Implement `CertificateRegistry.sol` with role-based access control.
- [ ] Implement custom errors, events, issue/read/verify/revoke functions.
- [ ] Complete contract unit tests.
- [ ] Add local deployment module and generated Java wrapper process.
- [ ] Document and test Sepolia deployment without committing secrets.

## Phase 6 - Issuance and blockchain integration

- [ ] Implement versioned canonicalization and SHA-256 service.
- [ ] Test deterministic hashing, changed fields, normalization, escaping, and ordering.
- [ ] Implement web3j client behind a blockchain gateway interface.
- [ ] Implement issuance lifecycle, transaction journal, receipt/event validation, and reconciliation.
- [ ] Test failed, timed-out, duplicated, and recovered submissions.

## Phase 7 - Public verification

- [ ] Implement the public verification DTO and API.
- [ ] Recompute local hash and compare database/on-chain proof.
- [ ] Implement dynamic `REVOKED > EXPIRED > VALID` status.
- [ ] Build `/verify` and `/verify/[certificateId]` with accessible states.
- [ ] Add authoritative explorer links and unavailable/mismatch handling.

## Phase 8 - PDF and QR

- [ ] Implement storage abstraction.
- [ ] Generate verification QR using ZXing.
- [ ] Generate professional PDF using PDFBox and embed QR.
- [ ] Implement authorized PDF download and artifact retry.
- [ ] Add PDF content/render checks.

## Phase 9 - Revocation and expiration

- [ ] Implement confirmed on-chain revocation workflow and journal.
- [ ] Persist private reason, confirmed time, and transaction metadata.
- [ ] Add admin confirmation UI and public revoked state.
- [ ] Test expired, revoked-and-expired, unauthorized, duplicate, and failed revocations.

## Phase 10 - Email

- [ ] Implement templated issuance email and PDF/download delivery choice.
- [ ] Record delivery attempts without rolling back issuance.
- [ ] Add retry and development SMTP configuration.
- [ ] Test successful and failed delivery behavior.

## Phase 11 - Dashboard and UI polish

- [ ] Add real aggregate counts and recent activity.
- [ ] Complete responsive navigation and organization profile.
- [ ] Audit accessibility, loading, empty, error, and confirmation states.

## Phase 12 - Verification and security review

- [ ] Run backend unit/integration/security tests.
- [ ] Run contract tests and coverage.
- [ ] Run frontend component and main-flow tests.
- [ ] Execute all five required demo scenarios.
- [ ] Review secret handling, logging, CORS, authorization, validation, and dependency risks.

## Phase 13 - Deployment

- [ ] Provision managed PostgreSQL and file/object storage.
- [ ] Deploy and verify the contract on Sepolia.
- [ ] Configure backend secrets and HTTPS deployment.
- [ ] Deploy frontend and configure production origin/API URL.
- [ ] Run production smoke tests and backup/recovery checks.

## Phase 14 - Assignment documentation

- [ ] Capture architecture, flow, ER, blockchain, contract, and UI evidence.
- [ ] Write implementation summary and contribution report.
- [ ] Add public GitHub and demo URLs.
- [ ] Record focused demo flow.
- [ ] Render and inspect the final submission PDF.
