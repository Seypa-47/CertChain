import { describe, expect, it } from "vitest";
import { loginSchema } from "./auth-schema";

describe("login validation", () => {
  it("requires a valid email and a password", () => {
    expect(loginSchema.safeParse({ email: "bad", password: "" }).success).toBe(false);
    expect(loginSchema.safeParse({ email: "admin@example.com", password: "secret" }).success).toBe(true);
  });
});
