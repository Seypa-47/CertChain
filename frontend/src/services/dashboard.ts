import { ApiRequestError } from "./auth";
import type { CertificateLifecycle, PublicStatus, TransactionStatus } from "./certificates";

export type { PublicStatus } from "./certificates";
export type DashboardData = {
  totalIssued: number;
  valid: number;
  expired: number;
  revoked: number;
  recentCertificates: Array<{
    id: string; certificateId: string; recipientName: string; programName: string;
    lifecycle: CertificateLifecycle; publicStatus: PublicStatus | null; issueDate: string; createdAt: string;
  }>;
  recentTransactions: Array<{
    certificateId: string; publicCertificateId: string; type: "ISSUE" | "REVOKE";
    status: TransactionStatus; transactionHash: string | null; network: string;
    createdAt: string; confirmedAt: string | null;
  }>;
};

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

export async function getDashboard(): Promise<DashboardData> {
  const response = await fetch(`${apiBase}/dashboard`, { credentials: "include", cache: "no-store" });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { error?: string; message?: string };
    throw new ApiRequestError(response.status, body.error ?? "REQUEST_FAILED",
      body.message ?? "Dashboard could not be loaded.");
  }
  return (await response.json()) as DashboardData;
}
