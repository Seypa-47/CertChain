"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function VerifySearchPage() {
  const router = useRouter();
  const [id, setId] = useState("");
  const [error, setError] = useState("");
  return <main className="mx-auto max-w-2xl px-6 py-16">
    <Link href="/" className="text-sm font-medium text-teal-800 underline">CertChain home</Link>
    <h1 className="mt-8 text-4xl font-semibold text-slate-950">Verify a certificate</h1>
    <p className="mt-3 text-slate-600">Enter the public ID printed on the certificate. No account is needed.</p>
    <form className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" onSubmit={(event) => {
      event.preventDefault();
      const value = id.trim().toUpperCase();
      if (!/^CERT-[0-9]{4}-(?!000000)[0-9]{6}$/.test(value)) {
        setError("Enter an ID in the format CERT-YYYY-NNNNNN."); return;
      }
      setError(""); router.push(`/verify/${encodeURIComponent(value)}`);
    }}>
      <label htmlFor="public-id" className="block font-medium text-slate-900">Certificate ID</label>
      <input id="public-id" value={id} onChange={(event) => setId(event.target.value)}
        autoComplete="off" maxLength={16} placeholder="CERT-2026-000001"
        aria-invalid={Boolean(error)} aria-describedby={error ? "public-id-error" : undefined}
        className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-3 font-mono text-slate-950 focus:ring-2 focus:ring-teal-600" />
      {error && <p id="public-id-error" role="alert" className="mt-2 text-sm text-red-700">{error}</p>}
      <button type="submit" className="mt-5 rounded-lg bg-teal-800 px-5 py-2.5 font-medium text-white">Verify certificate</button>
    </form>
  </main>;
}
