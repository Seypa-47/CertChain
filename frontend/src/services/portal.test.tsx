import { afterEach, describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { CertificateStatus } from "../components/certificate-status";
import { getDashboard } from "./dashboard";
import { getOrganization, updateOrganization } from "./organization";

afterEach(() => vi.unstubAllGlobals());

describe("tenant portal requests", () => {
  it("loads dashboard data with the authentication cookie", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ totalIssued: 2, valid: 1,
      expired: 1, revoked: 0, recentCertificates: [], recentTransactions: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    expect((await getDashboard()).totalIssued).toBe(2);
    expect(fetchMock.mock.calls[0][0]).toContain("/dashboard");
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", cache: "no-store" });
  });

  it("loads and saves only allowed organization fields with CSRF", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ name: "School" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-profile" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ name: "New School" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    expect((await getOrganization()).name).toBe("School");
    expect((await updateOrganization({ name: "New School", email: "office@example.test",
      walletAddress: null })).name).toBe("New School");
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ method: "PATCH", credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf-profile" },
      body: JSON.stringify({ name: "New School", email: "office@example.test", walletAddress: null }) });
  });
});

describe("certificate status component", () => {
  it("names lifecycle and public validity separately with a text icon", () => {
    const issued = renderToStaticMarkup(<CertificateStatus lifecycle="ISSUED" status="EXPIRED" />);
    expect(issued).toContain("ISSUED");
    expect(issued).toContain("Expired");
    expect(issued).toContain("aria-hidden");
    const draft = renderToStaticMarkup(<CertificateStatus lifecycle="DRAFT" status={null} />);
    expect(draft).toContain("DRAFT");
    expect(draft).not.toContain("Valid");
  });
});
