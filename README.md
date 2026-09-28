# CertChain

CertChain is a blockchain-based digital certificate issuing and verification platform for educational and training organizations. PostgreSQL stores operational certificate data, generated PDFs live in application or object storage, and Ethereum stores a minimal immutable proof. The blockchain is a trust layer, not the application database.

## Current status

Phase 2 adds the PostgreSQL schema and backend domain foundation. The detailed phased checklist is in [`docs/implementation-checklist.md`](docs/implementation-checklist.md).

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
- PDFBox, ZXing, JavaMailSender (introduced in their implementation phases)

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

3. Run the backend:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

4. Run the frontend in another terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. Compile and test the smart contract workspace:

   ```bash
   cd blockchain
   npm install
   npm test
   ```

The frontend defaults to `http://localhost:3000`, the backend to `http://localhost:8080`, and the backend health endpoint to `http://localhost:8080/actuator/health`.

## Environment and secret policy

Only `.env.example` templates are committed. Real database passwords, JWT keys, SMTP credentials, RPC credentials, deployment keys, and backend signing keys must remain in local or deployment secret stores. The Ethereum private key is used only by backend/deployment processes and is never prefixed with `NEXT_PUBLIC_` or sent to the browser.

## Development commands

```bash
# Frontend
cd frontend && npm run lint && npm run build

# Backend
cd backend && ./mvnw test

# Blockchain
cd blockchain && npm test
```

Backend tests include a Docker-independent H2 context smoke test. PostgreSQL integration tests use Testcontainers to start a fresh PostgreSQL container, apply Flyway migrations, and run Hibernate schema validation before checking database constraints, tenant queries, and concurrent public ID allocation. Start Docker Desktop before running `cd backend && ./mvnw test` (on Windows, `cd backend; .\mvnw.cmd test`). The PostgreSQL tests skip when Docker is unavailable, so check the Surefire reports in `backend/target/surefire-reports/` and confirm `PostgresDomainIntegrationTests` reports zero skipped tests. The main application runs Flyway on startup against PostgreSQL. To apply migrations locally, start PostgreSQL with `docker compose up -d postgres`, then run `cd backend && ./mvnw spring-boot:run`.

## Assignment deliverables

The project will retain diagrams, screenshots, implementation notes, contribution evidence, and deployment details under `docs/` so the final report can cover the required system overview, architecture, flows, ER diagram, blockchain and contract design, UI, implementation summary, public repository, contributions, and demo video.
