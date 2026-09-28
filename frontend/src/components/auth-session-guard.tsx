"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { ApiRequestError, getCurrentUser } from "@/services/auth";

export function AuthSessionGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  useEffect(() => {
    let active = true;
    getCurrentUser().catch((error: unknown) => {
      if (active && error instanceof ApiRequestError && error.status === 401) {
        router.replace("/login");
        router.refresh();
      }
    });
    return () => { active = false; };
  }, [router]);
  return <>{children}</>;
}
