import { expect } from "chai";
import { network } from "hardhat";

const { ethers, networkHelpers } = await network.create();
const KEY = ethers.keccak256(ethers.toUtf8Bytes("CERT-2026-000001"));
const HASH = ethers.sha256(ethers.toUtf8Bytes("canonical certificate proof"));
const OTHER_HASH = ethers.sha256(ethers.toUtf8Bytes("different proof"));
const ZERO = ethers.ZeroHash;

async function deployRegistry() {
  const [deployer, admin, issuer, stranger, secondIssuer] = await ethers.getSigners();
  const registry = await ethers.deployContract("CertificateRegistry", [admin.address, issuer.address]);
  return { registry, deployer, admin, issuer, stranger, secondIssuer };
}

describe("CertificateRegistry", function () {
  it("assigns distinct admin and issuer roles with delayed admin transfer", async function () {
    const { registry, deployer, admin, issuer, stranger, secondIssuer } =
      await networkHelpers.loadFixture(deployRegistry);
    const issuerRole = await registry.ISSUER_ROLE();
    const adminRole = await registry.DEFAULT_ADMIN_ROLE();

    expect(await registry.defaultAdmin()).to.equal(admin.address);
    expect(await registry.defaultAdminDelay()).to.equal(86400n);
    expect(await registry.hasRole(adminRole, admin.address)).to.equal(true);
    expect(await registry.hasRole(issuerRole, issuer.address)).to.equal(true);
    expect(await registry.hasRole(adminRole, deployer.address)).to.equal(false);
    expect(await registry.hasRole(issuerRole, admin.address)).to.equal(false);

    await expect(registry.connect(stranger).grantRole(issuerRole, secondIssuer.address))
      .to.be.revertedWithCustomError(registry, "AccessControlUnauthorizedAccount")
      .withArgs(stranger.address, adminRole);
    await registry.connect(admin).grantRole(issuerRole, secondIssuer.address);
    expect(await registry.hasRole(issuerRole, secondIssuer.address)).to.equal(true);
    await expect(registry.connect(admin).grantRole(adminRole, stranger.address))
      .to.be.revertedWithCustomError(registry, "AccessControlEnforcedDefaultAdminRules");
  });

  it("rejects zero constructor addresses", async function () {
    const [, admin, issuer] = await ethers.getSigners();
    await expect(ethers.deployContract("CertificateRegistry", [ethers.ZeroAddress, issuer.address]))
      .to.be.revertedWithCustomError(await ethers.getContractFactory("CertificateRegistry"), "ZeroAddress");
    await expect(ethers.deployContract("CertificateRegistry", [admin.address, ethers.ZeroAddress]))
      .to.be.revertedWithCustomError(await ethers.getContractFactory("CertificateRegistry"), "ZeroAddress");
  });

  it("issues a proof once, stores exact values, and emits its event", async function () {
    const { registry, issuer } = await networkHelpers.loadFixture(deployRegistry);
    const nextTimestamp = (await networkHelpers.time.latest()) + 10;
    await networkHelpers.time.setNextBlockTimestamp(nextTimestamp);
    await expect(registry.connect(issuer).issueCertificate(KEY, HASH, 0))
      .to.emit(registry, "CertificateIssued")
      .withArgs(KEY, HASH, issuer.address, nextTimestamp, 0);

    const record = await registry.getCertificate(KEY);
    expect(record.certificateHash).to.equal(HASH);
    expect(record.issuedAt).to.equal(BigInt(nextTimestamp));
    expect(record.expiresAt).to.equal(0n);
    expect(record.revoked).to.equal(false);
    expect(record.issuer).to.equal(issuer.address);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, false, false]);

    await expect(registry.connect(issuer).issueCertificate(KEY, OTHER_HASH, 0))
      .to.be.revertedWithCustomError(registry, "CertificateAlreadyIssued").withArgs(KEY);
    expect((await registry.getCertificate(KEY)).certificateHash).to.equal(HASH);
  });

  it("rejects unauthorized issuance and invalid proof inputs", async function () {
    const { registry, issuer, stranger } = await networkHelpers.loadFixture(deployRegistry);
    await expect(registry.connect(stranger).issueCertificate(KEY, HASH, 0))
      .to.be.revertedWithCustomError(registry, "AccessControlUnauthorizedAccount")
      .withArgs(stranger.address, await registry.ISSUER_ROLE());
    await expect(registry.connect(issuer).issueCertificate(ZERO, HASH, 0))
      .to.be.revertedWithCustomError(registry, "ZeroCertificateKey");
    await expect(registry.connect(issuer).issueCertificate(KEY, ZERO, 0))
      .to.be.revertedWithCustomError(registry, "ZeroCertificateHash");
    const expired = await networkHelpers.time.latest();
    await expect(registry.connect(issuer).issueCertificate(KEY, HASH, expired))
      .to.be.revertedWithCustomError(registry, "InvalidExpiry").withArgs(expired);
  });

  it("returns deterministic missing and mismatched verification results", async function () {
    const { registry, issuer } = await networkHelpers.loadFixture(deployRegistry);
    const missing = await registry.getCertificate(KEY);
    expect(missing.certificateHash).to.equal(ZERO);
    expect(missing.issuedAt).to.equal(0n);
    expect(missing.issuer).to.equal(ethers.ZeroAddress);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([false, false, false, false]);

    await registry.connect(issuer).issueCertificate(KEY, HASH, 0);
    expect(await registry.verifyCertificate(KEY, OTHER_HASH)).to.deep.equal([true, false, false, false]);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, false, false]);
  });

  it("expires only after the expiry timestamp", async function () {
    const { registry, issuer } = await networkHelpers.loadFixture(deployRegistry);
    const expiry = (await networkHelpers.time.latest()) + 100;
    await registry.connect(issuer).issueCertificate(KEY, HASH, expiry);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, false, false]);

    await networkHelpers.time.increaseTo(expiry);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, false, false]);
    await networkHelpers.time.increaseTo(expiry + 1);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, false, true]);
  });

  it("revokes only an existing proof and emits the revocation event", async function () {
    const { registry, issuer, stranger } = await networkHelpers.loadFixture(deployRegistry);
    await expect(registry.connect(stranger).revokeCertificate(KEY))
      .to.be.revertedWithCustomError(registry, "AccessControlUnauthorizedAccount")
      .withArgs(stranger.address, await registry.ISSUER_ROLE());
    await expect(registry.connect(issuer).revokeCertificate(KEY))
      .to.be.revertedWithCustomError(registry, "CertificateNotFound").withArgs(KEY);

    await registry.connect(issuer).issueCertificate(KEY, HASH, 0);
    const nextTimestamp = (await networkHelpers.time.latest()) + 10;
    await networkHelpers.time.setNextBlockTimestamp(nextTimestamp);
    await expect(registry.connect(issuer).revokeCertificate(KEY))
      .to.emit(registry, "CertificateRevoked").withArgs(KEY, issuer.address, nextTimestamp);
    expect((await registry.getCertificate(KEY)).revoked).to.equal(true);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, true, false]);
    await expect(registry.connect(issuer).revokeCertificate(KEY))
      .to.be.revertedWithCustomError(registry, "CertificateAlreadyRevoked").withArgs(KEY);
  });

  it("preserves revoked state after expiry", async function () {
    const { registry, issuer } = await networkHelpers.loadFixture(deployRegistry);
    const expiry = (await networkHelpers.time.latest()) + 100;
    await registry.connect(issuer).issueCertificate(KEY, HASH, expiry);
    await registry.connect(issuer).revokeCertificate(KEY);
    await networkHelpers.time.increaseTo(expiry + 1);
    expect(await registry.verifyCertificate(KEY, HASH)).to.deep.equal([true, true, true, true]);
  });
});
