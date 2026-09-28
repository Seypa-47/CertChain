"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiRequestError } from "@/services/auth";
import { verifyPublicCertificate, type PublicVerification } from "@/services/certificates";

type DisplayState = "VALID" | "EXPIRED" | "REVOKED" | "NOT_FOUND" | "PROOF_MISMATCH" | "VERIFICATION_UNAVAILABLE";

const treatments: Record<DisplayState, { icon: string; label: string; className: string; description: string }> = {
  VALID: { icon: "✓", label: "Valid", className: "border-emerald-300 bg-emerald-50 text-emerald-900",
    description: "The certificate data matches the confirmed blockchain proof." },
  EXPIRED: { icon: "◷", label: "Expired", className: "border-amber-300 bg-amber-50 text-amber-900",
    description: "The proof matches, but the certificate expiry date has passed." },
  REVOKED: { icon: "✕", label: "Revoked", className: "border-red-300 bg-red-50 text-red-900",
    description: "The proof matches and the issuer has revoked this certificate." },
  NOT_FOUND: { icon: "?", label: "Not found", className: "border-slate-300 bg-slate-50 text-slate-900",
    description: "No issued certificate was found for this public ID." },
  PROOF_MISMATCH: { icon: "!", label: "Proof mismatch", className: "border-red-300 bg-red-50 text-red-900",
    description: "The stored certificate and blockchain proof do not agree. Do not rely on this certificate." },
  VERIFICATION_UNAVAILABLE: { icon: "…", label: "Verification unavailable",
    className: "border-amber-300 bg-amber-50 text-amber-900",
    description: "The proof could not be checked right now. Try again later." },
};

function trustedExplorer(result: PublicVerification): string | null {
  if (!result.explorerUrl || !result.transactionHash || result.proofResult !== "VERIFIED") return null;
  try {
    const url = new URL(result.explorerUrl);
    const host = result.network === "sepolia" && result.chainId === 11155111
      ? "sepolia.etherscan.io" : result.network === "mainnet" && result.chainId === 1
        ? "etherscan.io" : null;
    return url.protocol === "https:" && url.hostname === host && !url.port
      && url.pathname === `/tx/${result.transactionHash}` && !url.search && !url.hash
      ? url.toString() : null;
  } catch { return null; }
}

export default function VerificationPage() {
  const { certificateId } = useParams<{ certificateId: string }>();
  const [result, setResult] = useState<PublicVerification | null>(null);
  const [state, setState] = useState<DisplayState | null>(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    verifyPublicCertificate(certificateId)
      .then((value) => { if (active) {
        setResult(value);
        setState(value.proofResult === "VERIFIED"
          ? value.status ?? "PROOF_MISMATCH" : value.proofResult);
      } })
      .catch((error) => { if (active) {
        setResult(null);
        setState(error instanceof ApiRequestError && error.status === 404
          ? "NOT_FOUND" : "VERIFICATION_UNAVAILABLE");
      } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [certificateId, retry]);

  async function copy(value: string, label: string) {
    try { await navigator.clipboard.writeText(value); setCopied(`${label} copied`); }
    catch { setCopied("Copy was unavailable"); }
  }

  const display = state ? treatments[state] : null;
  const explorer = result ? trustedExplorer(result) : null;
  return <main className="mx-auto max-w-3xl space-y-6 px-6 py-12">
    <Link href="/verify" className="text-sm font-medium text-teal-800 underline">← Search another certificate</Link>
    <h1 className="text-3xl font-semibold text-slate-950">Certificate verification</h1>
    {loading ? <p role="status" className="rounded-xl bg-white p-6 text-slate-700">Checking certificate proof…</p>
      : display && <section role="status" className={`rounded-2xl border p-6 ${display.className}`}>
        <h2 className="flex items-center gap-3 text-2xl font-semibold">
          <span aria-hidden="true" className="grid size-9 place-items-center rounded-full border border-current">{display.icon}</span>
          {display.label}
        </h2>
        <p className="mt-2">{display.description}</p>
        {(state === "VERIFICATION_UNAVAILABLE" || state === "PROOF_MISMATCH") &&
          <button type="button" onClick={() => { setLoading(true); setRetry((value) => value + 1); }}
            className="mt-4 rounded-lg border border-current px-3 py-2 font-medium">Check again</button>}
      </section>}
    {!loading && result && <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
      aria-label="Certificate details">
      <dl className="grid gap-5 sm:grid-cols-2">
        <div><dt className="text-sm text-slate-600">Certificate ID</dt><dd className="break-all font-mono font-medium">{result.certificateId}</dd></div>
        <div><dt className="text-sm text-slate-600">Organization</dt><dd>{result.organizationName}</dd></div>
        <div><dt className="text-sm text-slate-600">Recipient</dt><dd>{result.recipientName}</dd></div>
        <div><dt className="text-sm text-slate-600">Program</dt><dd>{result.programName}</dd></div>
        <div><dt className="text-sm text-slate-600">Issue date</dt><dd>{result.issueDate}</dd></div>
        <div><dt className="text-sm text-slate-600">Expiry date</dt><dd>{result.expiryDate ?? "No expiry"}</dd></div>
        {result.network && <div><dt className="text-sm text-slate-600">Network</dt><dd>{result.network} (chain {result.chainId})</dd></div>}
        {result.transactionHash && <div><dt className="text-sm text-slate-600">Transaction</dt><dd className="break-all font-mono text-xs">{result.transactionHash}</dd></div>}
        {result.contractAddress && <div><dt className="text-sm text-slate-600">Contract</dt><dd className="break-all font-mono text-xs">{result.contractAddress}</dd></div>}
        {result.blockNumber && <div><dt className="text-sm text-slate-600">Block</dt><dd>{result.blockNumber}</dd></div>}
        {result.blockTimestamp && <div><dt className="text-sm text-slate-600">Block timestamp</dt><dd>{result.blockTimestamp}</dd></div>}
      </dl>
      <div className="mt-6 flex flex-wrap gap-3">
        <button type="button" onClick={() => void copy(result.certificateId, "Certificate ID")}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium">Copy ID</button>
        {result.transactionHash && <button type="button" onClick={() => void copy(result.transactionHash!, "Transaction hash")}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium">Copy transaction</button>}
        {explorer && <a href={explorer} target="_blank" rel="noopener noreferrer"
          className="rounded-lg border border-teal-700 px-3 py-2 text-sm font-medium text-teal-800">View transaction in explorer</a>}
      </div>
      <p role="status" aria-live="polite" className="mt-3 text-sm text-slate-600">{copied}</p>
    </section>}
  </main>;
}
