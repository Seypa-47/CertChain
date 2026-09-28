import type { CertificateLifecycle, PublicStatus } from "@/services/certificates";

const statusClass: Record<PublicStatus, string> = {
  VALID: "border-emerald-200 bg-emerald-50 text-emerald-900",
  EXPIRED: "border-amber-200 bg-amber-50 text-amber-950",
  REVOKED: "border-rose-200 bg-rose-50 text-rose-900",
};
const statusIcon: Record<PublicStatus, string> = { VALID: "✓", EXPIRED: "◷", REVOKED: "✕" };

export function CertificateStatus({ lifecycle, status }: {
  lifecycle: CertificateLifecycle; status: PublicStatus | null;
}) {
  return <span className="inline-flex flex-wrap items-center gap-2">
    <span className="text-xs text-slate-600">{lifecycle.replaceAll("_", " ")}</span>
    {status && <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass[status]}`}>
      <span aria-hidden="true">{statusIcon[status]}</span>{status.charAt(0) + status.slice(1).toLowerCase()}
    </span>}
  </span>;
}
