# CertificateRegistry Smart Contract Design

## Storage model

```solidity
struct CertificateRecord {
    bytes32 certificateHash;
    uint64 issuedAt;
    uint64 expiresAt;
    bool revoked;
    address issuer;
}

mapping(bytes32 certificateKey => CertificateRecord) private certificates;
bytes32 public constant ISSUER_ROLE = keccak256("ISSUER_ROLE");
```

`certificateKey` is `keccak256` of the normalized public certificate ID. The mapping does not store the string ID, recipient identity, email, program, organization details, PDF, or revocation reason.

## Interface

```solidity
interface ICertificateRegistry {
    event CertificateIssued(
        bytes32 indexed certificateKey,
        bytes32 indexed certificateHash,
        address indexed issuer,
        uint64 issuedAt,
        uint64 expiresAt
    );

    event CertificateRevoked(
        bytes32 indexed certificateKey,
        address indexed revokedBy,
        uint64 revokedAt
    );

    function issueCertificate(
        bytes32 certificateKey,
        bytes32 certificateHash,
        uint64 expiresAt
    ) external;

    function revokeCertificate(bytes32 certificateKey) external;

    function getCertificate(bytes32 certificateKey)
        external
        view
        returns (CertificateRecord memory);

    function verifyCertificate(
        bytes32 certificateKey,
        bytes32 expectedHash
    ) external view returns (
        bool exists,
        bool hashMatches,
        bool revoked,
        bool expired
    );
}
```

## Rules

- `issueCertificate` and `revokeCertificate` require `ISSUER_ROLE`.
- Constructor grants `DEFAULT_ADMIN_ROLE` to an explicit admin address and `ISSUER_ROLE` to an explicit backend signer address; neither is inferred accidentally from a proxy or deployment helper.
- OpenZeppelin `AccessControlDefaultAdminRules` limits the default admin to one account and requires a two-step transfer after a one-day delay. The admin and issuer may be separate addresses.
- Zero keys and hashes are rejected.
- A nonzero stored `issuedAt` means the key already exists; duplicate issuance reverts and never overwrites.
- `expiresAt` is zero for no expiry or strictly greater than the block timestamp at issuance.
- Only existing, non-revoked certificates can be revoked.
- Reads are public.
- `verifyCertificate` computes expiry from `block.timestamp` and never mutates storage.
- Unknown keys return a zero-valued record and `(false, false, false, false)` from verification. For existing records, expiry is true only when `expiresAt != 0 && block.timestamp > expiresAt`; the application displays revoked status first if both flags are true.
- Custom errors keep failure causes clear and gas usage smaller than long revert strings.

## Required tests

- constructor rejects zero admin or issuer;
- admin and issuer roles are assigned correctly;
- authorized issuance stores the exact hash and emits the event;
- unauthorized issuance reverts;
- duplicate, zero-key, zero-hash, and invalid-expiry issuance revert;
- retrieval of existing and unknown records is deterministic;
- matching and non-matching proof verification;
- active and expired verification;
- authorized revocation updates state and emits the event;
- unauthorized, missing, and duplicate revocation revert;
- revoked remains revoked even after expiration.

Deployment uses `blockchain/ignition/modules/CertificateRegistry.ts` and explicit `admin` and `issuer` parameters from an ignored JSON file. Local and Sepolia commands are in `blockchain/README.md`. Sepolia deployment remains pending.

