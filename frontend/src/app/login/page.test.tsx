// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiRequestError, login } from "@/services/auth";
import LoginPage from "./page";

const replace = vi.fn();
const refresh = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, refresh }) }));
vi.mock("@/services/auth", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/services/auth")>();
  return { ...original, login: vi.fn() };
});

afterEach(() => { cleanup(); vi.clearAllMocks(); });

describe("organization login", () => {
  it("associates validation errors and does not submit invalid credentials", async () => {
    render(<LoginPage />);
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByText("Enter a valid email address")).toBeTruthy();
    expect(screen.getByLabelText("Email address").getAttribute("aria-invalid")).toBe("true");
    expect(login).not.toHaveBeenCalled();
  });

  it("redirects after successful login", async () => {
    vi.mocked(login).mockResolvedValue({ id: "user", organizationId: "org", name: "Admin",
      email: "admin@example.test", role: "ORG_ADMIN" });
    render(<LoginPage />);
    await userEvent.type(screen.getByLabelText("Email address"), "admin@example.test");
    await userEvent.type(screen.getByLabelText("Password"), "correct-password");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("button", { name: "Sign in" })).toBeTruthy();
    expect(login).toHaveBeenCalledWith({ email: "admin@example.test", password: "correct-password" });
    expect(replace).toHaveBeenCalledWith("/dashboard");
    expect(refresh).toHaveBeenCalled();
  });

  it("distinguishes invalid credentials from a server failure", async () => {
    vi.mocked(login).mockRejectedValueOnce(new ApiRequestError(401, "INVALID_CREDENTIALS", "Invalid"))
      .mockRejectedValueOnce(new Error("network unavailable"));
    render(<LoginPage />);
    await userEvent.type(screen.getByLabelText("Email address"), "admin@example.test");
    await userEvent.type(screen.getByLabelText("Password"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("alert")).toHaveProperty("textContent", "Email or password is incorrect.");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("alert")).toHaveProperty("textContent",
      "Sign in is unavailable right now. Please try again.");
    expect(replace).not.toHaveBeenCalled();
  });
});
