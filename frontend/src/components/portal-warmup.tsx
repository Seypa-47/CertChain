"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { ApiRequestError, getCurrentUser } from "@/services/auth";

export function PortalWarmup() {
  const router = useRouter();

  useEffect(() => {
    let active = true;
    let retry: ReturnType<typeof setTimeout>;

    async function checkAgain() {
      try {
        await getCurrentUser();
        if (active) router.refresh();
      } catch (error) {
        if (!active) return;
        if (error instanceof ApiRequestError && (error.status === 401 || error.status === 403)) {
          router.replace("/login");
          return;
        }
        retry = setTimeout(checkAgain, 10_000);
      }
    }

    retry = setTimeout(checkAgain, 10_000);
    return () => { active = false; clearTimeout(retry); };
  }, [router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-5 py-12">
      <section role="status" aria-live="polite" className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-8 text-slate-900 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-widest text-teal-800">CertChain</p>
        <h1 className="mt-3 text-2xl font-semibold">Starting your workspace</h1>
        <p className="mt-3 text-slate-600">The free demo API is waking up. This can take a minute or two; this page will retry automatically.</p>
        <button type="button" onClick={() => router.refresh()} className="mt-6 rounded-lg bg-teal-800 px-4 py-2 font-semibold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-800">Try again now</button>
      </section>
    </main>
  );
}
