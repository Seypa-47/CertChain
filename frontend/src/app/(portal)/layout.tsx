import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import type { ReactNode } from "react";
import { AuthSessionGuard } from "@/components/auth-session-guard";
import { LogoutButton } from "@/components/logout-button";
import type { AuthUser } from "@/services/auth";

export default async function PortalLayout({ children }: { children: ReactNode }) {
  const cookieHeader = (await cookies()).toString();
  const apiBase = process.env.API_INTERNAL_BASE_URL
    ?? process.env.NEXT_PUBLIC_API_BASE_URL
    ?? "http://localhost:8080/api";
  const response = await fetch(`${apiBase}/auth/me`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  }).catch(() => null);
  if (!response?.ok) redirect("/login");
  const user = (await response.json()) as AuthUser;

  return (
    <AuthSessionGuard>
      <div className="min-h-screen bg-slate-50">
        <header className="border-b border-slate-200 bg-white">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
            <Link href="/dashboard" className="text-xl font-semibold text-teal-900">CertChain</Link>
            <div className="flex items-center gap-5">
              <span className="hidden text-sm text-slate-600 sm:inline">{user.name}</span>
              <LogoutButton />
            </div>
          </div>
        </header>
        {children}
      </div>
    </AuthSessionGuard>
  );
}
