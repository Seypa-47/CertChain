# CertChain demo recording script and checklist

No public demo video exists yet. This script is ready for a controlled local environment or a verified future Sepolia deployment. State the network shown on screen; do not describe local Hardhat hashes as public explorer transactions. Use synthetic recipient names and emails. Do not record passwords, recovery phrases, private keys, RPC URLs with keys, SMTP credentials, or hidden browser settings.

## Preparation

- Choose a recorded network (`local` with chain ID `31337`, or Sepolia with chain ID `11155111`). Start the matching API, database, frontend, and contract. Confirm the API health endpoint, role configuration, receipt confirmations, and private PDF storage work.
- Prepare one fresh synthetic draft for issue and one already issued synthetic certificate for revocation. Have a separate expired issued certificate available. Keep the private revocation reason short and nonpersonal.
- Set the browser to a readable desktop width, enlarge text if needed, close notifications, and make sure network, transaction hashes, and certificate IDs can be read. For a public Sepolia explorer segment, open only a real confirmed transaction from this deployment.
- Begin screen recording before the first action. Capture a short audio level test and confirm the microphone and screen are both present.

## Ordered walkthrough

1. **Unknown verification (20-30 s).** Open `/verify`, enter a well-formed unused ID, and show the not-found response. Say: "A missing or unpublished ID does not reveal private drafts."
2. **Login (20 s).** Open `/login`; describe the organization admin role. Enter the demo credentials with the password hidden, submit, and show successful navigation. Say: "The JWT is in an HttpOnly cookie; the backend enforces tenant access."
3. **Dashboard (20-30 s).** Show actual counts and recent records. Explain that valid, expired, and revoked are derived from issued records for this organization.
4. **Create (30 s).** Open `/certificates/new`, enter a synthetic recipient and program, choose valid dates, and save. Show the server-assigned `CERT-YYYY-NNNNNN` ID and `DRAFT` lifecycle.
5. **Issue (35-50 s).** On the details page, confirm issuance. Show `ISSUING` if visible, then `ISSUED` after the validated receipt. Explain the `CREATED`/`SUBMITTED`/`CONFIRMED` journal and why a timeout remains pending instead of falsely succeeding.
6. **Transaction / explorer (25 s).** Show the issue transaction hash, block, contract, network, and issuer. Open the explorer only if this is a real public Sepolia transaction. On a local chain, show local transaction metadata and explicitly say that it is not an external explorer link.
7. **PDF / QR (25 s).** Download the authorized PDF, show the public ID and readable QR, and scan or decode the QR to the exact `/verify/<ID>` URL. Note that the PDF is stored privately and is generated only after confirmed issuance.
8. **Valid verify (25 s).** Open the QR URL without an account. Show `VALID`, `blockchainVerified`, recipient, program, organization, dates, and proof metadata. Explain that backend recomputation, stored hash, and chain proof must agree.
9. **Revoke (35 s).** Return to the admin details page, enter a synthetic private reason, and explicitly confirm. Show a pending state if the receipt is not yet final. Explain that the reason stays in PostgreSQL and never appears in the public result or contract.
10. **Revocation transaction (20 s).** Show its confirmed hash/event and block. Open the real Sepolia explorer only when available; otherwise identify it as local-chain evidence.
11. **Revoked verify (25 s).** Reopen the public page and show `REVOKED`. Mention that this takes priority over expiry and that the private reason is absent.
12. **Tamper-hash explanation (30 s).** Use the sanitized local proof-mismatch capture or a disposable local record, not production data. Explain that changing a proof-relevant database field changes the recomputed SHA-256 and yields `PROOF_MISMATCH`, never `VALID`. Restore the local fixture after demonstration.

Optional short cutaway: show the expired local capture and explain that expiry begins the day after the stated UTC expiry date. Keep the main order above unchanged.

## Review before publishing

- Replay the entire video with sound. Verify all 12 steps are visible, readable, in order, and use the correct network label.
- Pause on hashes and certificate IDs long enough to inspect them; verify any public explorer URL resolves to the shown transaction and contract.
- Check that the recording exposes no email beyond synthetic demo data, passwords, credentials, wallet keys, private revocation reasons from real users, or unrelated browser tabs.
- Add captions or a transcript where possible; ensure narration distinguishes app status from proof result and local from public-chain evidence.
- Publish with a URL accessible without sign-in. Open the link in a private browser window and verify playback. Insert the exact URL in the report's section 11 and README, then generate and visually inspect the final PDF.
