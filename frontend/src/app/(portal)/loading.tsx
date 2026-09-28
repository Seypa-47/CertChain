export default function PortalLoading() {
  return <main role="status" aria-label="Loading portal" className="mx-auto max-w-6xl space-y-5 px-6 py-10">
    <div className="h-8 w-56 rounded bg-slate-200" />
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[1, 2, 3, 4].map((item) =>
      <div key={item} className="h-32 rounded-2xl border border-slate-200 bg-white" />)}</div>
    <span className="sr-only">Loading portal…</span>
  </main>;
}
