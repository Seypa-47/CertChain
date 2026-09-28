"use client";

import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  getCertificate, getIssueProgress, issueCertificate, reconcileCertificate,
  type CertificateDetails, type IssueProgress,
} from "@/services/certificates";

export default function CertificateDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const [certificate, setCertificate] = useState<CertificateDetails | null>(null);
  const [progress, setProgress] = useState<IssueProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState("");
  const confirmButton = useRef<HTMLButtonElement>(null);

  const reload = useCallback(async () => {
    const details = await getCertificate(id);
    setCertificate(details);
    try { setProgress(await getIssueProgress(id)); }
    catch { setProgress(null); }
  }, [id]);

  useEffect(() => {
    let active = true;
    getCertificate(id).then(async (details) => {
      if (!active) return;
      setCertificate(details);
      try { const state = await getIssueProgress(id); if (active) setProgress(state); }
      catch { if (active) setProgress(null); }
    }).catch(() => { if (active) setError("Certificate details could not be loaded."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  useEffect(() => { if (confirming) confirmButton.current?.focus(); }, [confirming]);

  useEffect(() => {
    if (progress?.lifecycle !== "ISSUING") return;
    const timer = window.setInterval(() => {
      getIssueProgress(id).then((state) => {
        setProgress(state);
        if (state.lifecycle !== "ISSUING") void getCertificate(id).then(setCertificate);
      }).catch(() => {});
    }, 5000);
    return () => window.clearInterval(timer);
  }, [id, progress?.lifecycle]);

  async function act(action: "issue" | "reconcile") {
    setConfirming(false);
    setWorking(true);
    setError("");
    try {
      const state = action === "issue" ? await issueCertificate(id) : await reconcileCertificate(id);
      setProgress(state);
      await reload();
    } catch {
      setError(action === "issue"
        ? "Issuance could not be completed. Check the journal before retrying."
        : "Reconciliation is unavailable. The current state is preserved; try again later.");
      try { await reload(); } catch { /* Keep the last known state. */ }
    } finally { setWorking(false); }
  }

  if (loading) return <main className="mx-auto max-w-4xl px-6 py-12" role="status">Loading certificate…</main>;
  if (!certificate) return <main className="mx-auto max-w-4xl px-6 py-12" role="alert">{error}</main>;

  const lifecycle = progress?.lifecycle ?? certificate.lifecycle;
  return (
    <main className="mx-auto max-w-4xl space-y-8 px-6 py-12">
      <div>
        <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">Certificate details</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-950">{certificate.certificateId}</h1>
        <p className="mt-2 text-slate-600">{certificate.organization.name}</p>
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" aria-labelledby="proof-heading">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="proof-heading" className="text-xl font-semibold text-slate-950">Proof and issuance</h2>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-800">{lifecycle.replaceAll("_", " ")}</span>
        </div>
        <p className="mt-4 text-sm text-slate-600" role="status" aria-live="polite">
          {progress?.guidance ?? "Blockchain status is currently unavailable."}
        </p>
        {progress?.failureReason && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{progress.failureReason}</p>}
        {error && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{error}</p>}
        <dl className="mt-5 grid gap-4 text-sm sm:grid-cols-2">
          <div><dt className="text-slate-500">Transaction</dt><dd className="break-all font-mono text-slate-900">{progress?.transactionHash ?? "Not submitted"}</dd></div>
          <div><dt className="text-slate-500">Transaction state</dt><dd className="text-slate-900">{progress?.transactionStatus ?? "Not started"}</dd></div>
          <div><dt className="text-slate-500">Block</dt><dd className="text-slate-900">{progress?.blockNumber ?? "Pending"}</dd></div>
          <div><dt className="text-slate-500">Confirmed at</dt><dd className="text-slate-900">{progress?.blockTimestamp ?? "Pending"}</dd></div>
        </dl>
        {progress?.explorerUrl && <a className="mt-5 inline-block text-sm font-medium text-teal-800 underline" href={progress.explorerUrl} target="_blank" rel="noreferrer">View transaction in explorer</a>}
        <div className="mt-6 flex flex-wrap gap-3">
          {(lifecycle === "DRAFT" || lifecycle === "ISSUE_FAILED") &&
            <button type="button" disabled={working || !progress} onClick={() => setConfirming(true)}
              className="rounded-lg bg-teal-800 px-4 py-2 font-medium text-white disabled:opacity-50">
              {working ? "Working…" : lifecycle === "ISSUE_FAILED" ? "Review retry" : "Issue certificate"}
            </button>}
          {(lifecycle === "ISSUING" || lifecycle === "ISSUE_FAILED") &&
            <button type="button" disabled={working || !progress} onClick={() => void act("reconcile")}
              className="rounded-lg border border-slate-300 px-4 py-2 font-medium text-slate-800 disabled:opacity-50">
              Reconcile chain state
            </button>}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" aria-labelledby="details-heading">
        <h2 id="details-heading" className="text-xl font-semibold text-slate-950">Certificate</h2>
        <dl className="mt-4 grid gap-4 sm:grid-cols-2">
          <div><dt className="text-sm text-slate-500">Recipient</dt><dd className="font-medium text-slate-900">{certificate.recipientName}</dd></div>
          <div><dt className="text-sm text-slate-500">Program</dt><dd className="font-medium text-slate-900">{certificate.programName}</dd></div>
          <div><dt className="text-sm text-slate-500">Issue date</dt><dd className="text-slate-900">{certificate.issueDate}</dd></div>
          <div><dt className="text-sm text-slate-500">Expiry date</dt><dd className="text-slate-900">{certificate.expiryDate ?? "No expiry"}</dd></div>
        </dl>
      </section>

      {confirming && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 px-4">
        <div role="dialog" aria-modal="true" aria-labelledby="confirm-title" className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
          <h2 id="confirm-title" className="text-xl font-semibold text-slate-950">Confirm issuance</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">Proof fields will be frozen and a blockchain transaction submitted. Confirm the recipient, program, and dates before continuing.</p>
          <div className="mt-6 flex justify-end gap-3">
            <button type="button" onClick={() => setConfirming(false)} className="rounded-lg border px-4 py-2">Cancel</button>
            <button ref={confirmButton} type="button" onClick={() => void act("issue")}
              className="rounded-lg bg-teal-800 px-4 py-2 font-medium text-white">Confirm issue</button>
          </div>
        </div>
      </div>}
    </main>
  );
}
