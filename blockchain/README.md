# CertChain Blockchain

Hardhat 3 workspace for the minimal on-chain certificate proof registry. `CertificateRegistry` implements the existing ABI with an explicit admin and issuer. The admin uses OpenZeppelin's single-admin, delayed-transfer rules; only `ISSUER_ROLE` may issue or revoke.

```bash
cp ignition/parameters.example.json ignition/parameters.json
npm ci
npm run typecheck
npm test
npm run coverage
```

Edit `ignition/parameters.json` with two nonzero addresses: an admin that manages issuer roles and a backend issuer signer. Keep the admin key separate from the issuer key. The parameter file is ignored by Git. The contract stores only a mapping key and the proof hash, timestamps, revoked flag, and issuing address; it never stores recipient email, full certificate data, PDF content, or private revocation reasons. `getCertificate` returns a zero-valued record for an unknown key. `verifyCertificate` returns `(exists, hashMatches, revoked, expired)`; expiry turns true only after `expiresAt`, and applications give revocation priority when displaying status.

## Local deployment

For a quick deployment smoke test, use the in-process network. It discards deployment state when the command exits:

```bash
npx hardhat ignition deploy ignition/modules/CertificateRegistry.ts --network hardhatMainnet --parameters ignition/parameters.json
```

For a persistent local JSON-RPC node, run `npx hardhat node` in one terminal. Copy two addresses from its printed local accounts into `ignition/parameters.json`, then run in another terminal:

```bash
npx hardhat ignition deploy ignition/modules/CertificateRegistry.ts --network localhost --parameters ignition/parameters.json
```

Record the local address from the output only in local configuration. The deployment journal under `ignition/deployments` is ignored by Git. The local node and its accounts are development-only.

## Sepolia procedure (not yet deployed)

1. Provision a Sepolia RPC URL and a funded deployment wallet. Keep its private key and the backend issuer private key in a secret manager. Export `SEPOLIA_RPC_URL` and `SEPOLIA_DEPLOYER_PRIVATE_KEY` in the deployment shell; the `.env` example is a template and is not loaded automatically.
2. Put the intended admin and issuer public addresses in ignored `ignition/parameters.json`. Confirm they are distinct, nonzero, and controlled by the correct parties. The admin can rotate `ISSUER_ROLE`; a default-admin transfer uses a one-day delay and two steps.
3. Run `npm run typecheck && npm test && npm run coverage`, review the constructor parameters, then deploy with:

   ```bash
   npx hardhat ignition deploy ignition/modules/CertificateRegistry.ts --network sepolia --parameters ignition/parameters.json
   ```

4. Save the resulting contract address, chain ID, and deployment transaction in deployment records. Verify the deployed bytecode and read `defaultAdmin()`, `defaultAdminDelay()`, and `hasRole(ISSUER_ROLE, issuer)` before configuring the backend. Do not commit the address, keys, or deployment journal yet.

No Sepolia deployment is performed by the project setup or tests.

