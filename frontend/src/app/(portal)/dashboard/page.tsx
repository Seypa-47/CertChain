import Link from "next/link";

export default function DashboardPage() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">Organization portal</p>
      <h1 className="mt-3 text-4xl font-semibold tracking-tight text-slate-950">Dashboard</h1>
      <p className="mt-4 max-w-2xl leading-7 text-slate-600">
        Create drafts and manage certificates owned by your organization.
      </p>
      <Link href="/certificates" className="mt-6 inline-block rounded-lg bg-teal-800 px-5 py-3 font-medium text-white">
        View certificates
      </Link>
    </main>
  );
}
