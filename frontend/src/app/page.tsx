import Link from "next/link";

export default function Home() {
  return (
    <main className="flex min-h-screen items-center bg-[radial-gradient(circle_at_top_left,_#dff5ee_0,_transparent_32rem)] px-6 py-16 sm:px-10">
      <div className="mx-auto w-full max-w-6xl">
        <header className="mb-20 flex items-center justify-between">
          <div className="flex items-center gap-3" aria-label="CertChain home">
            <span className="grid size-10 place-items-center rounded-xl bg-teal-800 text-lg font-bold text-white shadow-sm">
              C
            </span>
            <span className="text-lg font-semibold tracking-tight text-slate-950">
              CertChain
            </span>
          </div>
          <Link href="/login" className="rounded-full border border-teal-900/10 bg-white/80 px-4 py-2 text-sm font-medium text-teal-900 shadow-sm backdrop-blur hover:bg-teal-50">
            Organization sign in
          </Link>
        </header>

        <section className="grid items-end gap-12 lg:grid-cols-[1.2fr_0.8fr]">
          <div>
            <p className="mb-5 text-sm font-semibold uppercase tracking-[0.18em] text-teal-800">
              Trusted digital credentials
            </p>
            <h1 className="max-w-4xl text-5xl font-semibold leading-[1.04] tracking-[-0.04em] text-slate-950 sm:text-7xl">
              Certificates people can verify, not just view.
            </h1>
            <p className="mt-7 max-w-2xl text-lg leading-8 text-slate-600">
              CertChain helps educational organizations issue professional PDF
              certificates backed by an immutable Ethereum proof and a simple
              public verification experience.
            </p>
          </div>

          <aside className="rounded-3xl border border-slate-200/80 bg-white/90 p-7 shadow-[0_20px_70px_-35px_rgba(15,23,42,0.35)] backdrop-blur">
            <p className="text-sm font-medium text-slate-500">Architecture principle</p>
            <p className="mt-3 text-2xl font-semibold tracking-tight text-slate-950">
              Blockchain is the trust layer, not the database.
            </p>
            <dl className="mt-8 grid gap-4 text-sm">
              <div className="flex items-center justify-between border-t border-slate-100 pt-4">
                <dt className="text-slate-500">Certificate data</dt>
                <dd className="font-medium text-slate-900">PostgreSQL</dd>
              </div>
              <div className="flex items-center justify-between border-t border-slate-100 pt-4">
                <dt className="text-slate-500">PDF and QR</dt>
                <dd className="font-medium text-slate-900">File storage</dd>
              </div>
              <div className="flex items-center justify-between border-t border-slate-100 pt-4">
                <dt className="text-slate-500">Immutable proof</dt>
                <dd className="font-medium text-teal-800">Ethereum</dd>
              </div>
            </dl>
          </aside>
        </section>
      </div>
    </main>
  );
}
