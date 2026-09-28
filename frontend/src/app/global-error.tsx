"use client";
import Link from "next/link";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <html lang="en"><body className="bg-slate-50 font-sans text-slate-950">
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center px-6 py-16" role="alert">
      <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">CertChain</p>
      <h1 className="mt-3 text-3xl font-semibold">Something went wrong.</h1>
      <p className="mt-3 text-slate-600">We could not load this page. Your saved certificates are unaffected.</p>
      <div className="mt-6 flex gap-3"><button type="button" onClick={reset}
        className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white">Try again</button>
        <Link href="/" className="rounded-lg border border-slate-300 px-5 py-2.5 font-semibold">Go home</Link></div>
    </main>
  </body></html>;
}
