import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const configured = process.env.API_PROXY_ORIGIN;
    if (!configured) return [];
    const origin = new URL(configured);
    if (origin.protocol !== "https:" || origin.origin !== configured.replace(/\/$/, "")) {
      throw new Error("API_PROXY_ORIGIN must be an HTTPS origin without a path");
    }
    if (process.env.NEXT_PUBLIC_API_BASE_URL !== "/api"
      || process.env.API_INTERNAL_BASE_URL !== `${origin.origin}/api`) {
      throw new Error("Hosted API proxy requires matching browser and server API base URLs");
    }
    return [{ source: "/api/:path*", destination: `${origin.origin}/api/:path*` }];
  },
};

export default nextConfig;
