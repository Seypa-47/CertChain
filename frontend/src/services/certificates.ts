import { ApiRequestError, getCsrfToken } from "./auth";

export type CertificateLifecycle = "DRAFT" | "ISSUING" | "ISSUED" | "ISSUE_FAILED";
export type TransactionStatus = "CREATED" | "SUBMITTED" | "CONFIRMED" | "FAILED";

export type CertificateDetails = {
  id: string;
  certificateId: string;
  recipientName: string;
  recipientEmail: string;
  programName: string;
  description: string | null;
  issueDate: string;
  expiryDate: string | null;
  lifecycle: CertificateLifecycle;
  certificateHash: string | null;
  issuedAt: string | null;
  revokedAt: string | null;
  organization: { id: string; name: string; logoUrl: string | null };
  transactions: Array<{
    transactionHash: string | null;
    status: TransactionStatus;
    blockNumber: number | null;
    confirmedAt: string | null;
  }>;
};

export type CertificateListItem = Pick<CertificateDetails,
  "id" | "certificateId" | "recipientName" | "programName" | "issueDate" | "expiryDate" | "lifecycle"> & {
  createdAt: string;
};

export type CertificatePage = {
  content: CertificateListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type DraftCertificateInput = {
  recipientName: string;
  recipientEmail: string;
  programName: string;
  description: string | null;
  issueDate: string;
  expiryDate: string | null;
};

export type IssueProgress = {
  id: string;
  certificateId: string;
  lifecycle: CertificateLifecycle;
  certificateHash: string | null;
  transactionStatus: TransactionStatus | null;
  transactionHash: string | null;
  network: string;
  chainId: number;
  contractAddress: string;
  blockNumber: number | null;
  blockTimestamp: string | null;
  explorerUrl: string | null;
  failureReason: string | null;
  guidance: string;
};

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

async function checked(response: Response): Promise<Response> {
  if (response.ok) return response;
  const error = (await response.json().catch(() => ({}))) as { error?: string; message?: string };
  throw new ApiRequestError(response.status, error.error ?? "REQUEST_FAILED",
    error.message ?? "The request could not be completed.");
}

export async function getCertificate(id: string): Promise<CertificateDetails> {
  const response = await checked(await fetch(`${apiBase}/certificates/${encodeURIComponent(id)}`, {
    credentials: "include", cache: "no-store",
  }));
  return (await response.json()) as CertificateDetails;
}

export async function listCertificates(params: {
  page: number; size: number; query?: string; lifecycle?: CertificateLifecycle | "";
}): Promise<CertificatePage> {
  const search = new URLSearchParams({ page: String(params.page), size: String(params.size),
    query: params.query ?? "", sort: "createdAt", direction: "desc" });
  if (params.lifecycle) search.set("lifecycle", params.lifecycle);
  const response = await checked(await fetch(`${apiBase}/certificates?${search}`, {
    credentials: "include", cache: "no-store",
  }));
  return (await response.json()) as CertificatePage;
}

async function writeDraft(path: string, method: "POST" | "PATCH", values: DraftCertificateInput): Promise<CertificateDetails> {
  const csrf = await getCsrfToken();
  const response = await checked(await fetch(`${apiBase}${path}`, {
    method, credentials: "include", cache: "no-store",
    headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf },
    body: JSON.stringify(values),
  }));
  return (await response.json()) as CertificateDetails;
}

export function createDraft(values: DraftCertificateInput): Promise<CertificateDetails> {
  return writeDraft("/certificates", "POST", values);
}

export function updateDraft(id: string, values: DraftCertificateInput): Promise<CertificateDetails> {
  return writeDraft(`/certificates/${encodeURIComponent(id)}`, "PATCH", values);
}

export async function getIssueProgress(id: string): Promise<IssueProgress> {
  const response = await checked(await fetch(`${apiBase}/certificates/${encodeURIComponent(id)}/issuance`, {
    credentials: "include", cache: "no-store",
  }));
  return (await response.json()) as IssueProgress;
}

async function postIssueAction(id: string, action: "issue" | "reconcile"): Promise<IssueProgress> {
  const csrf = await getCsrfToken();
  const response = await checked(await fetch(
    `${apiBase}/certificates/${encodeURIComponent(id)}/${action}`,
    { method: "POST", credentials: "include", cache: "no-store", headers: { "X-XSRF-TOKEN": csrf } },
  ));
  return (await response.json()) as IssueProgress;
}

export function issueCertificate(id: string): Promise<IssueProgress> {
  return postIssueAction(id, "issue");
}

export function reconcileCertificate(id: string): Promise<IssueProgress> {
  return postIssueAction(id, "reconcile");
}

export type RevokeProgress = {
  id: string;
  certificateId: string;
  revokedAt: string | null;
  transactionStatus: TransactionStatus | null;
  transactionHash: string | null;
  network: string;
  chainId: number;
  contractAddress: string;
  blockNumber: number | null;
  blockTimestamp: string | null;
  explorerUrl: string | null;
  failureReason: string | null;
  guidance: string;
};

export async function getRevokeProgress(id: string): Promise<RevokeProgress> {
  const response = await checked(await fetch(`${apiBase}/certificates/${encodeURIComponent(id)}/revocation`, {
    credentials: "include", cache: "no-store",
  }));
  return (await response.json()) as RevokeProgress;
}

export async function revokeCertificate(id: string, reason: string): Promise<RevokeProgress> {
  const csrf = await getCsrfToken();
  const response = await checked(await fetch(`${apiBase}/certificates/${encodeURIComponent(id)}/revoke`, {
    method: "POST", credentials: "include", cache: "no-store",
    headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf },
    body: JSON.stringify({ reason }),
  }));
  return (await response.json()) as RevokeProgress;
}

export async function reconcileRevocation(id: string): Promise<RevokeProgress> {
  const csrf = await getCsrfToken();
  const response = await checked(await fetch(`${apiBase}/certificates/${encodeURIComponent(id)}/revocation/reconcile`, {
    method: "POST", credentials: "include", cache: "no-store", headers: { "X-XSRF-TOKEN": csrf },
  }));
  return (await response.json()) as RevokeProgress;
}

export type PublicVerification = {
  certificateId: string;
  recipientName: string;
  programName: string;
  organizationName: string;
  issueDate: string;
  expiryDate: string | null;
  status: "VALID" | "EXPIRED" | "REVOKED";
  blockchainVerified: boolean;
  issuedAt: string;
  revokedAt: string | null;
  network: string;
  chainId: number;
  contractAddress: string;
};

export async function verifyPublicCertificate(id: string): Promise<PublicVerification> {
  const response = await checked(await fetch(`${apiBase}/public/certificates/${encodeURIComponent(id)}`, {
    cache: "no-store",
  }));
  return (await response.json()) as PublicVerification;
}
