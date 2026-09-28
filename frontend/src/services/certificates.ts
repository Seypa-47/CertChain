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
  organization: { id: string; name: string; logoUrl: string | null };
  transactions: Array<{
    transactionHash: string | null;
    status: TransactionStatus;
    blockNumber: number | null;
    confirmedAt: string | null;
  }>;
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
