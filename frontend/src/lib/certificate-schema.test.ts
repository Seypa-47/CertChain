import { describe, expect, it } from "vitest";
import { draftCertificateSchema } from "./certificate-schema";

const valid = { recipientName: "Ada", recipientEmail: "ada@example.com", programName: "Course",
  description: "", issueDate: "2026-01-01", expiryDate: "" };

describe("draft certificate form", () => {
  it("accepts a draft without expiry", () => {
    expect(draftCertificateSchema.safeParse(valid).success).toBe(true);
  });

  it("rejects invalid fields and expiry before issue date", () => {
    expect(draftCertificateSchema.safeParse({ ...valid, recipientName: " " }).success).toBe(false);
    expect(draftCertificateSchema.safeParse({ ...valid, recipientEmail: "invalid" }).success).toBe(false);
    expect(draftCertificateSchema.safeParse({ ...valid, description: "a".repeat(2001) }).success).toBe(false);
    expect(draftCertificateSchema.safeParse({ ...valid, issueDate: "2026-02-30" }).success).toBe(false);
    expect(draftCertificateSchema.safeParse({ ...valid, expiryDate: "2025-12-31" }).success).toBe(false);
  });
});
