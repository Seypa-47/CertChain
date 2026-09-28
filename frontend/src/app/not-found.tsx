import Link from "next/link";

export default function NotFound() {
  return <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center px-6 py-16">
    <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">404 · Page not found</p>
    <h1 className="mt-3 text-4xl font-semibold text-slate-950">This page is not available.</h1>
    <p className="mt-4 text-slate-600">Check the address or return to the CertChain home page.</p>
    <Link href="/" className="mt-7 self-start rounded-lg bg-teal-800 px-5 py-3 font-semibold text-white">Go home</Link>
  </main>;
}
