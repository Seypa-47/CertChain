"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { loginSchema, type LoginValues } from "@/lib/auth-schema";
import { ApiRequestError, login } from "@/services/auth";

export default function LoginPage() {
  const router = useRouter();
  const [serverError, setServerError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) });

  async function onSubmit(values: LoginValues) {
    setServerError("");
    try {
      await login(values);
      router.replace("/dashboard");
      router.refresh();
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        setServerError("Email or password is incorrect.");
      } else {
        setServerError("Sign in is unavailable right now. Please try again.");
      }
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 px-4 py-12">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-2xl sm:p-10">
        <Link href="/" className="inline-flex items-center gap-3 text-lg font-semibold text-slate-900">
          <span className="grid size-10 place-items-center rounded-xl bg-teal-800 text-white">C</span>
          CertChain
        </Link>
        <h1 className="mt-10 text-3xl font-semibold tracking-tight text-slate-950">Organization sign in</h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          Access your organization&apos;s certificate workspace.
        </p>
        <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-slate-800">Email address</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? "email-error" : undefined}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 outline-none focus:border-teal-700 focus:ring-2 focus:ring-teal-200"
              {...register("email")}
            />
            {errors.email && <p id="email-error" className="mt-1 text-sm text-red-700">{errors.email.message}</p>}
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-slate-800">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? "password-error" : undefined}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 outline-none focus:border-teal-700 focus:ring-2 focus:ring-teal-200"
              {...register("password")}
            />
            {errors.password && <p id="password-error" className="mt-1 text-sm text-red-700">{errors.password.message}</p>}
          </div>
          <p role="alert" aria-live="polite" className="min-h-5 text-sm text-red-700">{serverError}</p>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl bg-teal-800 px-4 py-3 font-semibold text-white transition hover:bg-teal-900 focus:outline-none focus:ring-2 focus:ring-teal-300 disabled:cursor-wait disabled:opacity-60"
          >
            {isSubmitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
      </div>
    </main>
  );
}
