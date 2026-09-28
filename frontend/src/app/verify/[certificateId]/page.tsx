"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { verifyPublicCertificate, type PublicVerification } from "@/services/certificates";

export default function VerificationPage() {
  const { certificateId } = useParams<{ certificateId: string }>();
  const [result, setResult] = useState<PublicVerification | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const refresh = () => verifyPublicCertificate(certificateId)
      .then((value) => { if (active) { setResult(value); setError(""); } })
      .catch(() => { if (active) {
        setResult(null);
        setError("Certificate proof could not be verified. Please try again later or check the ID.");
      } })
      .finally(() => { if (active) setLoading(false); });
    void refresh();
    const timer = window.setInterval(() => { void refresh(); }, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [certificateId]);

  if (loading) return <main className="mx-auto max-w-2xl p-8" role="status">Checking certificate proof…</main>;
  if (!result) return <main className="mx-auto max-w-2xl p-8" role="alert">{error}</main>;

  const status = result.status === "REVOKED" ? "✕ Revoked"
    : result.status === "EXPIRED" ? "◷ Expired" : "✓ Valid";
  return <main className="mx-auto max-w-2xl space-y-6 p-8">
    <h1 className="text-3xl font-semibold text-slate-950">Certificate verification</h1>
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" aria-label="Verified certificate">
      <p className="text-xl font-semibold text-slate-950" role="status">{status}</p>
      <p className="mt-2 text-sm text-slate-700">Blockchain proof verified</p>
      <dl className="mt-6 grid gap-4 sm:grid-cols-2">
        <div><dt className="text-sm text-slate-600">Certificate ID</dt><dd className="font-medium">{result.certificateId}</dd></div>
        <div><dt className="text-sm text-slate-600">Organization</dt><dd>{result.organizationName}</dd></div>
        <div><dt className="text-sm text-slate-600">Recipient</dt><dd>{result.recipientName}</dd></div>
        <div><dt className="text-sm text-slate-600">Program</dt><dd>{result.programName}</dd></div>
        <div><dt className="text-sm text-slate-600">Issue date</dt><dd>{result.issueDate}</dd></div>
        <div><dt className="text-sm text-slate-600">Expiry date</dt><dd>{result.expiryDate ?? "No expiry"}</dd></div>
        {result.revokedAt && <div><dt className="text-sm text-slate-600">Revoked at</dt><dd>{result.revokedAt}</dd></div>}
      </dl>
    </section>
  </main>;
}
