"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CertificateStatus } from "@/components/certificate-status";
import { getDashboard, type DashboardData } from "@/services/dashboard";

const metrics = [
  { key: "totalIssued", label: "Issued certificates", symbol: "▣", detail: "Confirmed on chain" },
  { key: "valid", label: "Currently valid", symbol: "✓", detail: "Issued and active" },
  { key: "expired", label: "Expired", symbol: "◷", detail: "Past expiry date" },
  { key: "revoked", label: "Revoked", symbol: "✕", detail: "Permanently revoked" },
] as const;

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;
    getDashboard().then((result) => { if (active) { setData(result); setError(""); } })
      .catch(() => { if (active) setError("Dashboard data is unavailable. Your certificates are unchanged."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [revision]);

  return <main className="mx-auto max-w-6xl space-y-8 px-5 py-8 sm:px-6 sm:py-10">
    <div className="flex flex-wrap items-end justify-between gap-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">Organization portal</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">Dashboard</h1>
        <p className="mt-2 text-slate-600">A current view of your certificate activity.</p>
      </div>
      <Link href="/certificates/new" className="inline-flex min-h-11 items-center rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white shadow-sm hover:bg-teal-900">
        + Issue certificate
      </Link>
    </div>

    {error && <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-900">
      <span>{error}</span><button type="button" onClick={() => { setLoading(true); setRevision((value) => value + 1); }}
        className="rounded-md border border-rose-400 px-3 py-1.5 font-medium">Retry</button>
    </div>}

    {loading ? <div role="status" aria-label="Loading dashboard" className="space-y-7">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{metrics.map((metric) => <div key={metric.key}
        className="h-36 rounded-2xl border border-slate-200 bg-white p-5"><div className="h-4 w-28 rounded bg-slate-200" />
        <div className="mt-7 h-10 w-16 rounded bg-slate-200" /></div>)}</div>
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
      <span className="sr-only">Loading dashboard data…</span>
    </div> : data && <>
      <section aria-label="Certificate statistics" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {metrics.map((metric) => <div key={metric.key} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-start justify-between gap-3"><p className="text-sm font-medium text-slate-600">{metric.label}</p>
            <span aria-hidden="true" className="grid size-9 place-items-center rounded-lg bg-teal-50 text-lg text-teal-800">{metric.symbol}</span></div>
          <p className="mt-5 text-4xl font-semibold tabular-nums tracking-tight text-slate-950">{data[metric.key].toLocaleString()}</p>
          <p className="mt-2 text-xs text-slate-500">{metric.detail}</p>
        </div>)}
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.3fr_1fr]">
        <section aria-labelledby="recent-certificates" className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
            <h2 id="recent-certificates" className="text-lg font-semibold text-slate-950">Recent certificates</h2>
            <Link href="/certificates" className="text-sm font-semibold text-teal-800 underline">View all</Link>
          </div>
          {data.recentCertificates.length === 0 ? <div className="px-5 py-12 text-center">
            <p className="font-medium text-slate-900">No certificates yet</p>
            <p className="mt-2 text-sm text-slate-600">Create a draft to start your first certificate.</p>
          </div> : <ul className="divide-y divide-slate-100">{data.recentCertificates.map((item) =>
            <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
              <div className="min-w-0"><Link href={`/certificates/${item.id}`} className="font-semibold text-teal-900 underline">
                {item.certificateId}</Link><p className="mt-1 truncate text-sm text-slate-700">{item.recipientName} · {item.programName}</p></div>
              <CertificateStatus lifecycle={item.lifecycle} status={item.publicStatus} />
            </li>)}</ul>}
        </section>

        <section aria-labelledby="recent-transactions" className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4"><h2 id="recent-transactions" className="text-lg font-semibold text-slate-950">Blockchain activity</h2></div>
          {data.recentTransactions.length === 0 ? <div className="px-5 py-12 text-center">
            <p className="font-medium text-slate-900">No transactions yet</p>
            <p className="mt-2 text-sm text-slate-600">Issuance activity will appear here.</p>
          </div> : <ul className="divide-y divide-slate-100">{data.recentTransactions.map((item, index) =>
            <li key={`${item.certificateId}-${item.type}-${index}`} className="px-5 py-4">
              <div className="flex items-center justify-between gap-3"><Link href={`/certificates/${item.certificateId}`}
                className="font-semibold text-teal-900 underline">{item.publicCertificateId}</Link>
                <span className="text-xs font-semibold text-slate-700">{item.type} · {item.status}</span></div>
              <p className="mt-1 text-xs text-slate-600">{item.network} · {new Date(item.createdAt).toLocaleString()}</p>
            </li>)}</ul>}
        </section>
      </div>
    </>}
  </main>;
}
