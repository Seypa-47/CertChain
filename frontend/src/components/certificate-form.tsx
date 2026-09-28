"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { draftCertificateSchema, type DraftCertificateValues } from "@/lib/certificate-schema";
import type { DraftCertificateInput } from "@/services/certificates";

type Props = {
  initial?: DraftCertificateValues;
  onSave: (values: DraftCertificateInput) => Promise<void>;
  cancelHref?: string;
  onCancel?: () => void;
  submitLabel: string;
};

const emptyValues: DraftCertificateValues = {
  recipientName: "", recipientEmail: "", programName: "", description: "",
  issueDate: new Date().toISOString().slice(0, 10), expiryDate: "",
};

export function CertificateForm({ initial, onSave, cancelHref, onCancel, submitLabel }: Props) {
  const [serverError, setServerError] = useState("");
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<DraftCertificateValues>({
    resolver: zodResolver(draftCertificateSchema), defaultValues: initial ?? emptyValues,
  });

  async function submit(values: DraftCertificateValues) {
    setServerError("");
    try {
      await onSave({ recipientName: values.recipientName.trim(), recipientEmail: values.recipientEmail.trim(),
        programName: values.programName.trim(), description: values.description.trim() || null,
        issueDate: values.issueDate, expiryDate: values.expiryDate || null });
    } catch {
      setServerError("Certificate could not be saved. Check the fields and try again.");
    }
  }

  const fieldClass = "mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-950 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-200";
  return <form className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" noValidate onSubmit={handleSubmit(submit)}>
    <div>
      <label htmlFor="recipientName" className="block text-sm font-medium">Recipient name</label>
      <input id="recipientName" autoComplete="name" className={fieldClass} aria-invalid={Boolean(errors.recipientName)}
        aria-describedby={errors.recipientName ? "recipientName-error" : undefined} {...register("recipientName")} />
      {errors.recipientName && <p id="recipientName-error" className="mt-1 text-sm text-red-700">{errors.recipientName.message}</p>}
    </div>
    <div>
      <label htmlFor="recipientEmail" className="block text-sm font-medium">Recipient email</label>
      <input id="recipientEmail" type="email" autoComplete="email" className={fieldClass}
        aria-invalid={Boolean(errors.recipientEmail)} aria-describedby={errors.recipientEmail ? "recipientEmail-error" : undefined}
        {...register("recipientEmail")} />
      {errors.recipientEmail && <p id="recipientEmail-error" className="mt-1 text-sm text-red-700">{errors.recipientEmail.message}</p>}
    </div>
    <div>
      <label htmlFor="programName" className="block text-sm font-medium">Program name</label>
      <input id="programName" className={fieldClass} aria-invalid={Boolean(errors.programName)}
        aria-describedby={errors.programName ? "programName-error" : undefined} {...register("programName")} />
      {errors.programName && <p id="programName-error" className="mt-1 text-sm text-red-700">{errors.programName.message}</p>}
    </div>
    <div>
      <label htmlFor="description" className="block text-sm font-medium">Description (optional)</label>
      <textarea id="description" rows={4} className={fieldClass} aria-invalid={Boolean(errors.description)}
        aria-describedby={errors.description ? "description-error" : undefined} {...register("description")} />
      {errors.description && <p id="description-error" className="mt-1 text-sm text-red-700">{errors.description.message}</p>}
    </div>
    <div className="grid gap-5 sm:grid-cols-2">
      <div>
        <label htmlFor="issueDate" className="block text-sm font-medium">Issue date</label>
        <input id="issueDate" type="date" className={fieldClass} aria-invalid={Boolean(errors.issueDate)}
          aria-describedby={errors.issueDate ? "issueDate-error" : undefined} {...register("issueDate")} />
        {errors.issueDate && <p id="issueDate-error" className="mt-1 text-sm text-red-700">{errors.issueDate.message}</p>}
      </div>
      <div>
        <label htmlFor="expiryDate" className="block text-sm font-medium">Expiry date (optional)</label>
        <input id="expiryDate" type="date" className={fieldClass} aria-invalid={Boolean(errors.expiryDate)}
          aria-describedby={errors.expiryDate ? "expiryDate-error" : undefined} {...register("expiryDate")} />
        {errors.expiryDate && <p id="expiryDate-error" className="mt-1 text-sm text-red-700">{errors.expiryDate.message}</p>}
      </div>
    </div>
    <p role="alert" aria-live="polite" className="text-sm text-red-700">{serverError}</p>
    <div className="flex flex-wrap gap-3">
      <button disabled={isSubmitting} type="submit" className="rounded-lg bg-teal-800 px-5 py-2.5 font-medium text-white disabled:opacity-50">
        {isSubmitting ? "Saving…" : submitLabel}
      </button>
      {onCancel ? <button type="button" onClick={onCancel}
        className="rounded-lg border border-slate-300 px-5 py-2.5 font-medium text-slate-700">Cancel</button>
        : <Link href={cancelHref ?? "/certificates"}
          className="rounded-lg border border-slate-300 px-5 py-2.5 font-medium text-slate-700">Cancel</Link>}
    </div>
  </form>;
}
