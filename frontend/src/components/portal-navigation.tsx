"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const destinations = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/certificates", label: "Certificates" },
  { href: "/certificates/new", label: "New certificate" },
  { href: "/profile", label: "Organization profile" },
];

export function PortalNavigation() {
  const path = usePathname();
  return <nav aria-label="Portal navigation" className="border-b border-slate-200 bg-white">
    <div className="mx-auto grid max-w-6xl grid-cols-2 gap-x-2 px-4 sm:flex sm:gap-1 sm:px-6">
      {destinations.map((item) => {
        const active = item.href === "/certificates" ? path === item.href ||
          (path.startsWith("/certificates/") && !path.startsWith("/certificates/new")) : path === item.href;
        return <Link key={item.href} href={item.href} aria-current={active ? "page" : undefined}
          className={`border-b-2 px-2 py-2.5 text-sm font-medium transition-colors sm:px-3 sm:py-3 ${active
            ? "border-teal-800 text-teal-950" : "border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-950"}`}>
          {item.label}</Link>;
      })}
    </div>
  </nav>;
}
