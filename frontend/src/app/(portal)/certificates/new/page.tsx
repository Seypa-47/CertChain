"use client";

import Link from "next/link";
import { useState } from "react";
import { CertificateForm } from "@/components/certificate-form";
import { createDraft, type CertificateDetails } from "@/services/certificates";

export default function NewCertificatePage() {
  const [created, setCreated] = useState<CertificateDetails | null>(null);
  return <main className="mx-auto max-w-3xl space-y-6 px-6 py-10">
    <div>
      <Link href="/certificates" className="text-sm font-medium text-teal-800 underline">← Certificates</Link>
      <p className="mt-5 text-sm font-semibold uppercase tracking-widest text-teal-800">Certificates</p>
      <h1 className="mt-2 text-3xl font-semibold text-slate-950">Create draft</h1>
      <p className="mt-2 text-slate-600">Review the details before issuing. The public ID is generated when you save.</p>
    </div>
    {created ? <div role="status" className="rounded-2xl border border-teal-200 bg-teal-50 p-6">
      <p className="text-lg font-semibold text-teal-950">Draft created</p>
      <p className="mt-2 text-teal-900">Public ID: {created.certificateId}</p>
      <Link href={`/certificates/${created.id}`} className="mt-4 inline-block font-medium text-teal-900 underline">
        View certificate
      </Link>
    </div> : <CertificateForm submitLabel="Save draft" cancelHref="/certificates" onSave={async (values) => {
      setCreated(await createDraft(values));
    }} />}
  </main>;
}
