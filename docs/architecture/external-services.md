# Environment Variables and External Services

## External services

| Service | Required when | Purpose | Local development |
|---|---|---|---|
| PostgreSQL | Phase 2 onward | Operational source of truth | Docker Compose `postgres` service. |
| SMTP provider | Phase 10 onward | Recipient notification | Docker Compose `mailpit`; UI on port 8025. |
| Ethereum JSON-RPC | Phase 5 onward | Deploy, issue, read, and revoke on a compatible chain | Hardhat simulated network first, Sepolia later. |
| Ethereum explorer | Sepolia deployment | Independent transaction inspection and optional source verification | Sepolia Etherscan-compatible URLs. |
| File/object storage | Phase 8 onward | Generated PDFs and optional logos | Local filesystem in development; managed object storage in production. |
| Vercel or Node host | Deployment | Next.js frontend | `npm run dev` locally. |
| Java application host | Deployment | Spring Boot API and server-side signer | `./mvnw spring-boot:run` locally. |
| Managed PostgreSQL | Deployment | Durable production database | Local Docker is not a production database. |

## Frontend variables

| Variable | Exposure | Meaning |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Public/browser | Backend API base URL ending in `/api`. |
| `NEXT_PUBLIC_APP_URL` | Public/browser | Canonical frontend origin used for verification links. |
| `API_INTERNAL_BASE_URL` | Server-only | API base URL for the Next.js portal guard. |

No database, JWT, SMTP, RPC credential, or private key may use the `NEXT_PUBLIC_` prefix.

## Backend variables

| Variable | Secret | Meaning |
|---|---:|---|
| `DATABASE_URL` | Sometimes | PostgreSQL JDBC URL. Treat as secret if it embeds credentials. |
| `DATABASE_USERNAME` | Yes | Database login. |
| `DATABASE_PASSWORD` | Yes | Database password. |
| `SERVER_PORT` | No | API listen port. |
| `FRONTEND_BASE_URL` | No | Canonical URL placed into QR and email links. |
| `CORS_ALLOWED_ORIGINS` | No | Explicit comma-separated browser origin allowlist. |
| `JWT_SECRET_BASE64` | Yes | High-entropy symmetric signing key for access tokens. |
| `JWT_ISSUER` | No | Required JWT issuer claim and validation value. |
| `JWT_ACCESS_TOKEN_TTL` | No | ISO-8601 duration, initially `PT15M`. |
| `AUTH_COOKIE_NAME`, `AUTH_COOKIE_DOMAIN` | No | Authentication cookie name and optional shared domain. |
| `AUTH_COOKIE_SECURE`, `AUTH_COOKIE_SAME_SITE` | No | Cookie transport and cross-site policy. Secure must be true in production; SameSite None requires Secure. |
| `SPRING_PROFILES_ACTIVE` | No | Set to `dev` only for local bootstrap. |
| `DEV_BOOTSTRAP_ENABLED` | No | Explicit flag for the development bootstrap; default false. |
| `DEV_ORGANIZATION_NAME`, `DEV_ORGANIZATION_EMAIL`, `DEV_ADMIN_NAME`, `DEV_ADMIN_EMAIL` | No | Development bootstrap identities, required when enabled. |
| `DEV_ADMIN_PASSWORD` | Yes | Development bootstrap password, required when enabled; never commit a real value. |
| `STORAGE_ROOT` | No | Local artifact directory in development. |
| `MAIL_HOST`, `MAIL_PORT` | No | SMTP connection. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Yes | SMTP credentials when required. |
| `MAIL_AUTH`, `MAIL_STARTTLS` | No | SMTP security settings. |
| `MAIL_FROM` | No | Sender address. |
| `BLOCKCHAIN_ENABLED` | No | Safe feature switch; defaults to false. |
| `BLOCKCHAIN_NETWORK`, `BLOCKCHAIN_CHAIN_ID` | No | Expected network identity. |
| `BLOCKCHAIN_RPC_URL` | Yes | RPC endpoint, often containing a provider credential. |
| `BLOCKCHAIN_CONTRACT_ADDRESS` | No | Deployed registry address. |
| `BLOCKCHAIN_ISSUER_PRIVATE_KEY` | Yes, critical | Backend signing key holding only `ISSUER_ROLE`. |
| `BLOCKCHAIN_CONFIRMATIONS` | No | Receipt confirmation threshold. |

## Contract deployment variables

| Variable | Secret | Meaning |
|---|---:|---|
| `SEPOLIA_RPC_URL` | Yes | Deployment RPC endpoint. |
| `SEPOLIA_DEPLOYER_PRIVATE_KEY` | Yes, critical | Deployment wallet key; keep separate from the backend issuer when possible. |
| `CONTRACT_ADMIN_ADDRESS` | No | Address receiving `DEFAULT_ADMIN_ROLE`. |
| `ISSUER_ADDRESS` | No | Backend signer address receiving `ISSUER_ROLE`. |
| `ETHERSCAN_API_KEY` | Yes | Optional contract source verification credential. |

Production secrets belong in hosting-provider secret storage. Example files contain names and nonfunctional placeholders only.

