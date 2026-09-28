import { ApiRequestError, getCsrfToken } from "./auth";

export type OrganizationProfile = {
  id: string; name: string; email: string; walletAddress: string | null; logoUrl: string | null;
};
export type OrganizationUpdate = Pick<OrganizationProfile, "name" | "email" | "walletAddress">;

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

async function checked(response: Response): Promise<Response> {
  if (response.ok) return response;
  const body = (await response.json().catch(() => ({}))) as { error?: string; message?: string };
  throw new ApiRequestError(response.status, body.error ?? "REQUEST_FAILED",
    body.message ?? "Organization profile could not be saved.");
}

export async function getOrganization(): Promise<OrganizationProfile> {
  const response = await checked(await fetch(`${apiBase}/organization`, {
    credentials: "include", cache: "no-store",
  }));
  return (await response.json()) as OrganizationProfile;
}

export async function updateOrganization(values: OrganizationUpdate): Promise<OrganizationProfile> {
  const csrf = await getCsrfToken();
  const response = await checked(await fetch(`${apiBase}/organization`, {
    method: "PATCH", credentials: "include", cache: "no-store",
    headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf },
    body: JSON.stringify(values),
  }));
  return (await response.json()) as OrganizationProfile;
}
