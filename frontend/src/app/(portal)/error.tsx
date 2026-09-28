"use client";

export default function PortalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main className="mx-auto max-w-3xl px-6 py-16" role="alert">
    <h1 className="text-3xl font-semibold text-slate-950">This page could not be loaded.</h1>
    <p className="mt-3 text-slate-600">Try again. No saved certificate data was changed.</p>
    <button type="button" onClick={reset} className="mt-6 rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white">Try again</button>
  </main>;
}
