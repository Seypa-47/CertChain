import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import type { ReactNode } from "react";
import { AuthSessionGuard } from "@/components/auth-session-guard";
import { LogoutButton } from "@/components/logout-button";
import { PortalNavigation } from "@/components/portal-navigation";
import { PortalWarmup } from "@/components/portal-warmup";
import type { AuthUser } from "@/services/auth";

export default async function PortalLayout({ children }: { children: ReactNode }) {
  const cookieHeader = (await cookies()).toString();
  const apiBase = process.env.API_INTERNAL_BASE_URL
    ?? process.env.NEXT_PUBLIC_API_BASE_URL
    ?? "http://localhost:8080/api";
  const response = await fetch(`${apiBase}/auth/me`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
    signal: AbortSignal.timeout(8_000),
  }).catch(() => null);
  if (response?.status === 401 || response?.status === 403) redirect("/login");
  if (!response?.ok) return <PortalWarmup />;
  const user = (await response.json()) as AuthUser;

  return (
    <AuthSessionGuard>
      <div className="min-h-screen bg-slate-50">
        <a href="#main-content" className="sr-only rounded-lg bg-white px-4 py-2 text-teal-950 focus:not-sr-only focus:absolute focus:left-4 focus:top-2 focus:z-50">Skip to content</a>
        <header className="border-b border-slate-200 bg-white">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-4 sm:px-6">
            <Link href="/dashboard" className="flex items-center gap-2 text-xl font-semibold text-teal-900">
              <span aria-hidden="true" className="grid size-9 place-items-center rounded-lg bg-teal-800 text-base text-white">C</span>CertChain</Link>
            <div className="flex items-center gap-5">
              <span className="hidden text-sm text-slate-600 sm:inline">{user.name}</span>
              <LogoutButton />
            </div>
          </div>
        </header>
        <PortalNavigation />
        <div id="main-content" tabIndex={-1}>{children}</div>
      </div>
    </AuthSessionGuard>
  );
}
