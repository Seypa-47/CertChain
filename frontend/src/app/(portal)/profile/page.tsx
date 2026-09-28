"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { getOrganization, updateOrganization, type OrganizationProfile } from "@/services/organization";

const schema = z.object({
  name: z.string().trim().min(1, "Organization name is required").max(200, "Use 200 characters or fewer"),
  email: z.email("Enter a valid contact email").max(320, "Use 320 characters or fewer"),
  walletAddress: z.union([z.literal(""), z.string().regex(/^0x[0-9a-fA-F]{40}$/, "Enter a valid Ethereum address")]),
});
type Values = z.infer<typeof schema>;

export default function ProfilePage() {
  const [profile, setProfile] = useState<OrganizationProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [saved, setSaved] = useState(false);
  const [revision, setRevision] = useState(0);
  const { register, reset, handleSubmit, formState: { errors, isSubmitting, isDirty } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { name: "", email: "", walletAddress: "" },
  });

  useEffect(() => {
    let active = true;
    getOrganization().then((result) => {
      if (active) { setProfile(result); reset({ name: result.name, email: result.email,
        walletAddress: result.walletAddress ?? "" }); setLoadError(""); }
    }).catch(() => { if (active) setLoadError("Organization profile could not be loaded."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reset, revision]);

  async function save(values: Values) {
    setSaveError(""); setSaved(false);
    try {
      const updated = await updateOrganization({ name: values.name.trim(), email: values.email.trim(),
        walletAddress: values.walletAddress || null });
      setProfile(updated);
      reset({ name: updated.name, email: updated.email, walletAddress: updated.walletAddress ?? "" });
      setSaved(true);
    } catch { setSaveError("The profile could not be saved. Review the fields and try again."); }
  }

  return <main className="mx-auto max-w-4xl space-y-7 px-5 py-8 sm:px-6 sm:py-10">
    <div><Link href="/dashboard" className="text-sm font-medium text-teal-800 underline">← Dashboard</Link>
      <p className="mt-5 text-sm font-semibold uppercase tracking-widest text-teal-800">Organization</p>
      <h1 className="mt-2 text-3xl font-semibold text-slate-950">Profile</h1>
      <p className="mt-2 text-slate-600">Keep your public issuer details and contact information current.</p>
    </div>

    {loading ? <div role="status" className="rounded-2xl border border-slate-200 bg-white p-6">
      <div className="h-5 w-44 rounded bg-slate-200" /><div className="mt-7 h-11 rounded bg-slate-100" />
      <div className="mt-5 h-11 rounded bg-slate-100" /><span className="sr-only">Loading profile…</span>
    </div> : loadError ? <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-900">
      {loadError} <button type="button" className="ml-2 font-semibold underline" onClick={() => {
        setLoading(true); setRevision((value) => value + 1);
      }}>Retry</button>
    </div> : profile && <form noValidate onSubmit={handleSubmit(save)}
      className="space-y-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
      <div><h2 className="text-lg font-semibold text-slate-950">Issuer details</h2>
        <p className="mt-1 text-sm text-slate-600">Your organization name appears on certificates and public verification.</p></div>
      {saved && <p role="status" className="rounded-lg border border-teal-200 bg-teal-50 p-3 text-sm text-teal-900">✓ Profile saved.</p>}
      {saveError && <p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-900">{saveError}</p>}
      <div><label htmlFor="org-name" className="block text-sm font-semibold text-slate-800">Organization name</label>
        <input id="org-name" {...register("name")} autoComplete="organization" maxLength={200}
          aria-invalid={!!errors.name} aria-describedby={errors.name ? "org-name-error" : undefined}
          className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-950" />
        {errors.name && <p id="org-name-error" className="mt-1 text-sm text-rose-700">{errors.name.message}</p>}</div>
      <div><label htmlFor="org-email" className="block text-sm font-semibold text-slate-800">Contact email</label>
        <input id="org-email" type="email" {...register("email")} autoComplete="email" maxLength={320}
          aria-invalid={!!errors.email} aria-describedby={errors.email ? "org-email-error" : "org-email-help"}
          className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-950" />
        <p id="org-email-help" className="mt-1 text-xs text-slate-600">This is the organization contact address, not a recipient address.</p>
        {errors.email && <p id="org-email-error" className="mt-1 text-sm text-rose-700">{errors.email.message}</p>}</div>
      <div><label htmlFor="org-wallet" className="block text-sm font-semibold text-slate-800">Organization wallet address <span className="font-normal text-slate-500">(optional)</span></label>
        <input id="org-wallet" {...register("walletAddress")} autoComplete="off" maxLength={42} spellCheck={false}
          aria-invalid={!!errors.walletAddress} aria-describedby={errors.walletAddress ? "org-wallet-error" : "org-wallet-help"}
          className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 font-mono text-sm text-slate-950" />
        <p id="org-wallet-help" className="mt-1 text-xs text-slate-600">Public profile only. Blockchain signing uses a separate backend account.</p>
        {errors.walletAddress && <p id="org-wallet-error" className="mt-1 text-sm text-rose-700">{errors.walletAddress.message}</p>}</div>
      <div className="flex flex-wrap items-center justify-end gap-3 border-t border-slate-100 pt-5">
        <button type="button" onClick={() => reset({ name: profile.name, email: profile.email,
          walletAddress: profile.walletAddress ?? "" })} disabled={!isDirty || isSubmitting}
          className="rounded-lg border border-slate-300 px-4 py-2.5 font-medium text-slate-800 disabled:opacity-50">Discard changes</button>
        <button type="submit" disabled={isSubmitting || !isDirty}
          className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white disabled:opacity-50">
          {isSubmitting ? "Saving…" : "Save profile"}</button>
      </div>
    </form>}
  </main>;
}
