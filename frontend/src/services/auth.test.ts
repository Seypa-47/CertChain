import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiRequestError, getCurrentUser, login, logout } from "./auth";

afterEach(() => vi.unstubAllGlobals());

describe("cookie authentication requests", () => {
  it("obtains a CSRF token and sends it with credentialed login", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-value" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: "user-1", role: "ORG_ADMIN" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    await login({ email: "admin@example.com", password: "secret" });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", cache: "no-store" });
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf-value" },
    });
  });

  it("sends CSRF protection for logout", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-value" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    await logout();
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "POST", credentials: "include", headers: { "X-XSRF-TOKEN": "csrf-value" },
    });
  });

  it("surfaces unauthorized current-user responses", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ error: "UNAUTHORIZED", message: "Authentication required" }), { status: 401 })));
    await expect(getCurrentUser()).rejects.toBeInstanceOf(ApiRequestError);
  });
});
