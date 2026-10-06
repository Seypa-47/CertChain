// @vitest-environment jsdom
import { act, cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiRequestError, getCurrentUser } from "@/services/auth";
import { PortalWarmup } from "./portal-warmup";

const replace = vi.fn();
const refresh = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, refresh }) }));
vi.mock("@/services/auth", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/services/auth")>();
  return { ...original, getCurrentUser: vi.fn() };
});

afterEach(() => { cleanup(); vi.useRealTimers(); vi.clearAllMocks(); });

describe("portal wakeup", () => {
  it("keeps a transient outage retryable, then refreshes after the API recovers", async () => {
    vi.mocked(getCurrentUser).mockRejectedValueOnce(new Error("API waking"))
      .mockResolvedValueOnce({ id: "user", organizationId: "org", name: "Admin",
        email: "admin@example.test", role: "ORG_ADMIN" });
    vi.useFakeTimers();
    render(<PortalWarmup />);
    expect(screen.getByRole("status").textContent).toContain("Starting your workspace");
    await act(async () => { await vi.advanceTimersByTimeAsync(10_000); });
    expect(replace).not.toHaveBeenCalled();
    expect(refresh).not.toHaveBeenCalled();
    await act(async () => { await vi.advanceTimersByTimeAsync(10_000); });
    expect(refresh).toHaveBeenCalledOnce();
  });

  it("redirects when a retry confirms the session is unauthorized", async () => {
    vi.mocked(getCurrentUser).mockRejectedValue(new ApiRequestError(401, "UNAUTHORIZED", "Sign in"));
    vi.useFakeTimers();
    render(<PortalWarmup />);
    await act(async () => { await vi.advanceTimersByTimeAsync(10_000); });
    expect(replace).toHaveBeenCalledWith("/login");
    expect(refresh).not.toHaveBeenCalled();
  });

  it("offers a manual retry", async () => {
    render(<PortalWarmup />);
    await userEvent.click(screen.getByRole("button", { name: "Try again now" }));
    expect(refresh).toHaveBeenCalledOnce();
  });
});
