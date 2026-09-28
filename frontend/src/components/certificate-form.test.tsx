// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { CertificateForm } from "./certificate-form";

afterEach(cleanup);

describe("draft certificate form", () => {
  it("associates validation errors with fields and blocks an invalid expiry", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    render(<CertificateForm onSave={save} submitLabel="Save draft" />);
    fireEvent.click(screen.getByRole("button", { name: "Save draft" }));
    expect(await screen.findByText("Recipient name is required.")).toBeTruthy();
    expect(screen.getByLabelText("Recipient name").getAttribute("aria-invalid")).toBe("true");
    expect(save).not.toHaveBeenCalled();
  });

  it("normalizes optional values and saves a valid draft", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    render(<CertificateForm onSave={save} submitLabel="Save draft" />);
    fireEvent.change(screen.getByLabelText("Recipient name"), { target: { value: " Ada Example " } });
    fireEvent.change(screen.getByLabelText("Recipient email"), { target: { value: "ada@example.test" } });
    fireEvent.change(screen.getByLabelText("Program name"), { target: { value: " Workshop " } });
    fireEvent.change(screen.getByLabelText("Issue date"), { target: { value: "2026-09-28" } });
    fireEvent.click(screen.getByRole("button", { name: "Save draft" }));
    await waitFor(() => expect(save).toHaveBeenCalledWith({
      recipientName: "Ada Example", recipientEmail: "ada@example.test", programName: "Workshop",
      description: null, issueDate: "2026-09-28", expiryDate: null,
    }));
  });
});
