"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { listCertificates, type CertificateLifecycle, type CertificatePage } from "@/services/certificates";

export default function CertificatesPage() {
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [lifecycle, setLifecycle] = useState<CertificateLifecycle | "">("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<CertificatePage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    listCertificates({ page, size: 20, query, lifecycle })
      .then((data) => { if (active) { setResult(data); setError(""); } })
      .catch(() => { if (active) { setResult(null); setError("Certificates could not be loaded. Try again."); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, query, lifecycle, retry]);

  const rows = result?.content ?? [];
  return <main className="mx-auto max-w-6xl space-y-7 px-6 py-10">
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">Organization portal</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-950">Certificates</h1>
        <p className="mt-2 text-slate-600">Search and manage certificates owned by your organization.</p>
      </div>
      <Link href="/certificates/new" className="rounded-lg bg-teal-800 px-4 py-2.5 font-medium text-white">New certificate</Link>
    </div>

    <form className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-[1fr_auto_auto]" onSubmit={(event) => {
      event.preventDefault(); setPage(0); setLoading(true); setQuery(searchInput.trim()); setRetry((value) => value + 1);
    }}>
      <div>
        <label htmlFor="certificate-search" className="block text-sm font-medium text-slate-700">Search by recipient, program, or ID</label>
        <input id="certificate-search" value={searchInput} maxLength={200} onChange={(event) => setSearchInput(event.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-950" />
      </div>
      <div>
        <label htmlFor="lifecycle-filter" className="block text-sm font-medium text-slate-700">Lifecycle</label>
        <select id="lifecycle-filter" value={lifecycle} onChange={(event) => {
          setLifecycle(event.target.value as CertificateLifecycle | ""); setPage(0); setLoading(true);
        }} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-950">
          <option value="">All</option><option value="DRAFT">Draft</option><option value="ISSUING">Issuing</option>
          <option value="ISSUED">Issued</option><option value="ISSUE_FAILED">Issue failed</option>
        </select>
      </div>
      <button type="submit" className="self-end rounded-lg border border-teal-800 px-4 py-2 font-medium text-teal-900">Search</button>
    </form>

    {loading ? <p role="status" className="text-slate-600">Loading certificates…</p> : error ?
      <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-800">
        {error} <button type="button" className="ml-2 underline" onClick={() => { setLoading(true); setRetry(retry + 1); }}>Retry</button>
      </div> : rows.length === 0 ?
      <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
        <p className="font-medium text-slate-900">No certificates found</p>
        <p className="mt-2 text-sm text-slate-600">Adjust the filters or create your first draft.</p>
      </div> : <>
        <div className="hidden overflow-x-auto rounded-2xl border border-slate-200 bg-white sm:block">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-700"><tr>
              <th scope="col" className="px-4 py-3">Certificate ID</th><th scope="col" className="px-4 py-3">Recipient</th>
              <th scope="col" className="px-4 py-3">Program</th><th scope="col" className="px-4 py-3">Issue date</th>
              <th scope="col" className="px-4 py-3">Lifecycle</th>
            </tr></thead>
            <tbody>{rows.map((item) => <tr key={item.id} className="border-t border-slate-100">
              <td className="px-4 py-3 font-medium"><Link className="text-teal-800 underline" href={`/certificates/${item.id}`}>{item.certificateId}</Link></td>
              <td className="px-4 py-3">{item.recipientName}</td><td className="px-4 py-3">{item.programName}</td>
              <td className="px-4 py-3">{item.issueDate}</td><td className="px-4 py-3">{item.lifecycle.replaceAll("_", " ")}</td>
            </tr>)}</tbody>
          </table>
        </div>
        <div className="space-y-3 sm:hidden">{rows.map((item) => <Link key={item.id} href={`/certificates/${item.id}`}
          className="block rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <span className="font-semibold text-teal-800">{item.certificateId}</span>
          <span className="mt-2 block text-slate-950">{item.recipientName}</span>
          <span className="block text-sm text-slate-600">{item.programName}</span>
          <span className="mt-2 block text-xs text-slate-600">Lifecycle: {item.lifecycle.replaceAll("_", " ")}</span>
        </Link>)}</div>
      </>}

    {result && result.totalPages > 1 && <nav aria-label="Certificate pages" className="flex items-center justify-between gap-3">
      <button type="button" disabled={page === 0 || loading} onClick={() => { setLoading(true); setPage(page - 1); }}
        className="rounded-lg border px-4 py-2 disabled:opacity-50">Previous</button>
      <span className="text-sm text-slate-700">Page {page + 1} of {result.totalPages} · {result.totalElements} certificates</span>
      <button type="button" disabled={page + 1 >= result.totalPages || loading} onClick={() => { setLoading(true); setPage(page + 1); }}
        className="rounded-lg border px-4 py-2 disabled:opacity-50">Next</button>
    </nav>}
  </main>;
}
