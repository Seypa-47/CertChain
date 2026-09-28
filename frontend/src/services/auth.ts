import type { LoginValues } from "@/lib/auth-schema";

export type AuthUser = {
  id: string;
  organizationId: string;
  name: string;
  email: string;
  role: "ORG_ADMIN";
};

type ApiErrorBody = { error?: string; message?: string };

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

export class ApiRequestError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

async function checked(response: Response): Promise<Response> {
  if (response.ok) return response;
  const body = (await response.json().catch(() => ({}))) as ApiErrorBody;
  throw new ApiRequestError(
    response.status,
    body.error ?? "REQUEST_FAILED",
    body.message ?? "The request could not be completed.",
  );
}

export async function getCsrfToken(): Promise<string> {
  const response = await checked(
    await fetch(`${apiBase}/auth/csrf`, {
      credentials: "include",
      cache: "no-store",
    }),
  );
  const data = (await response.json()) as { token: string };
  return data.token;
}

export async function login(values: LoginValues): Promise<AuthUser> {
  const token = await getCsrfToken();
  const response = await checked(
    await fetch(`${apiBase}/auth/login`, {
      method: "POST",
      credentials: "include",
      cache: "no-store",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": token },
      body: JSON.stringify(values),
    }),
  );
  return (await response.json()) as AuthUser;
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await checked(
    await fetch(`${apiBase}/auth/me`, {
      credentials: "include",
      cache: "no-store",
    }),
  );
  return (await response.json()) as AuthUser;
}

export async function logout(): Promise<void> {
  const token = await getCsrfToken();
  await checked(
    await fetch(`${apiBase}/auth/logout`, {
      method: "POST",
      credentials: "include",
      cache: "no-store",
      headers: { "X-XSRF-TOKEN": token },
    }),
  );
}
