// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { ApiRequestError } from "@/services/auth";
import { verifyPublicCertificate, type PublicVerification } from "@/services/certificates";
import VerificationPage from "./page";

vi.mock("next/navigation", () => ({ useParams: () => ({ certificateId: "CERT-2026-000001" }) }));
vi.mock("@/services/certificates", () => ({ verifyPublicCertificate: vi.fn() }));
afterEach(() => { cleanup(); vi.resetAllMocks(); });

const result = (status: PublicVerification["status"], proofResult: PublicVerification["proofResult"])
  : PublicVerification => ({ certificateId: "CERT-2026-000001", recipientName: "Ada Example",
    programName: "Workshop", organizationName: "Example Academy", issueDate: "2026-09-28",
    expiryDate: null, status, proofResult, blockchainVerified: proofResult === "VERIFIED",
    issuedAt: "2026-09-28T10:00:00Z", revokedAt: null, network: "local", chainId: 31337, contractAddress: null,
    transactionHash: null, blockNumber: null, blockTimestamp: null, explorerUrl: null });

describe("public verification states", () => {
  it.each([
    ["VALID", "VERIFIED", "Valid"], ["EXPIRED", "VERIFIED", "Expired"],
    ["REVOKED", "VERIFIED", "Revoked"], [null, "PROOF_MISMATCH", "Proof mismatch"],
    [null, "VERIFICATION_UNAVAILABLE", "Verification unavailable"],
  ] as const)("shows %s / %s as %s", async (status, proof, heading) => {
    vi.mocked(verifyPublicCertificate).mockResolvedValue(result(status, proof));
    render(<VerificationPage />);
    expect(await screen.findByRole("heading", { name: heading })).toBeTruthy();
    expect(screen.queryByText("recipient@example.test")).toBeNull();
  });

  it("shows the same not-found state for unknown and unpublished certificates", async () => {
    vi.mocked(verifyPublicCertificate).mockRejectedValue(
      new ApiRequestError(404, "CERTIFICATE_NOT_FOUND", "Certificate not found"));
    render(<VerificationPage />);
    expect(await screen.findByRole("heading", { name: "Not found" })).toBeTruthy();
  });
});
