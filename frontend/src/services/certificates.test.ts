import { afterEach, describe, expect, it, vi } from "vitest";
import { getCertificate, issueCertificate, reconcileCertificate, revokeCertificate,
  reconcileRevocation, verifyPublicCertificate } from "./certificates";

afterEach(() => vi.unstubAllGlobals());

describe("certificate issuance requests", () => {
  it("reads tenant-protected details with cookies", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "cert-1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    await getCertificate("cert-1");
    expect(fetchMock.mock.calls[0][0]).toContain("/certificates/cert-1");
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", cache: "no-store" });
  });

  it("sends CSRF and cookies when issuing or reconciling", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-1" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ lifecycle: "ISSUING" }), { status: 202 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-2" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ lifecycle: "ISSUED" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    expect((await issueCertificate("cert-1")).lifecycle).toBe("ISSUING");
    expect((await reconcileCertificate("cert-1")).lifecycle).toBe("ISSUED");
    expect(fetchMock.mock.calls[1][0]).toContain("/certificates/cert-1/issue");
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "POST", credentials: "include", headers: { "X-XSRF-TOKEN": "csrf-1" },
    });
    expect(fetchMock.mock.calls[3][0]).toContain("/certificates/cert-1/reconcile");
    expect(fetchMock.mock.calls[3][1]).toMatchObject({
      method: "POST", credentials: "include", headers: { "X-XSRF-TOKEN": "csrf-2" },
    });
  });
});

describe("certificate revocation requests", () => {
  it("sends a private reason with CSRF and credentials, then reconciles", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-1" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ transactionStatus: "SUBMITTED" }), { status: 202 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-2" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ transactionStatus: "CONFIRMED" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    await revokeCertificate("cert-1", "Private correction");
    await reconcileRevocation("cert-1");
    expect(fetchMock.mock.calls[1][0]).toContain("/certificates/cert-1/revoke");
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "POST", credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf-1" },
      body: JSON.stringify({ reason: "Private correction" }),
    });
    expect(fetchMock.mock.calls[3][0]).toContain("/certificates/cert-1/revocation/reconcile");
  });

  it("reads public proof without private credentials or reason", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ status: "REVOKED" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    expect((await verifyPublicCertificate("CERT-2099-000001")).status).toBe("REVOKED");
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ cache: "no-store" });
    expect(fetchMock.mock.calls[0][1]).not.toHaveProperty("credentials");
  });
});
