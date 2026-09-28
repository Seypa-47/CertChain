// SPDX-License-Identifier: MIT
pragma solidity ^0.8.34;

import {AccessControlDefaultAdminRules} from
  "@openzeppelin/contracts/access/extensions/AccessControlDefaultAdminRules.sol";
import {ICertificateRegistry} from "./ICertificateRegistry.sol";

contract CertificateRegistry is ICertificateRegistry, AccessControlDefaultAdminRules {
  bytes32 public constant ISSUER_ROLE = keccak256("ISSUER_ROLE");

  error ZeroAddress();
  error ZeroCertificateKey();
  error ZeroCertificateHash();
  error CertificateAlreadyIssued(bytes32 certificateKey);
  error InvalidExpiry(uint64 expiresAt);
  error CertificateNotFound(bytes32 certificateKey);
  error CertificateAlreadyRevoked(bytes32 certificateKey);

  mapping(bytes32 certificateKey => CertificateRecord) private certificates;

  constructor(address admin, address issuer)
    AccessControlDefaultAdminRules(1 days, _nonZero(admin))
  {
    if (issuer == address(0)) revert ZeroAddress();
    _grantRole(ISSUER_ROLE, issuer);
  }

  function issueCertificate(bytes32 certificateKey, bytes32 certificateHash, uint64 expiresAt)
    external
    onlyRole(ISSUER_ROLE)
  {
    if (certificateKey == bytes32(0)) revert ZeroCertificateKey();
    if (certificateHash == bytes32(0)) revert ZeroCertificateHash();
    if (certificates[certificateKey].issuedAt != 0) {
      revert CertificateAlreadyIssued(certificateKey);
    }
    if (expiresAt != 0 && expiresAt <= block.timestamp) revert InvalidExpiry(expiresAt);

    uint64 issuedAt = uint64(block.timestamp);
    certificates[certificateKey] = CertificateRecord({
      certificateHash: certificateHash,
      issuedAt: issuedAt,
      expiresAt: expiresAt,
      revoked: false,
      issuer: msg.sender
    });
    emit CertificateIssued(certificateKey, certificateHash, msg.sender, issuedAt, expiresAt);
  }

  function revokeCertificate(bytes32 certificateKey) external onlyRole(ISSUER_ROLE) {
    CertificateRecord storage record = certificates[certificateKey];
    if (record.issuedAt == 0) revert CertificateNotFound(certificateKey);
    if (record.revoked) revert CertificateAlreadyRevoked(certificateKey);

    record.revoked = true;
    emit CertificateRevoked(certificateKey, msg.sender, uint64(block.timestamp));
  }

  function getCertificate(bytes32 certificateKey)
    external
    view
    returns (CertificateRecord memory)
  {
    return certificates[certificateKey];
  }

  function verifyCertificate(bytes32 certificateKey, bytes32 expectedHash)
    external
    view
    returns (bool exists, bool hashMatches, bool revoked, bool expired)
  {
    CertificateRecord storage record = certificates[certificateKey];
    exists = record.issuedAt != 0;
    if (!exists) return (false, false, false, false);

    hashMatches = record.certificateHash == expectedHash;
    revoked = record.revoked;
    expired = record.expiresAt != 0 && block.timestamp > record.expiresAt;
  }

  function _nonZero(address account) private pure returns (address) {
    if (account == address(0)) revert ZeroAddress();
    return account;
  }
}
