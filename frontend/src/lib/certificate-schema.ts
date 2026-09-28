import { z } from "zod";

const requiredText = (label: string, max: number) => z.string()
  .refine((value) => value.trim().length > 0, `${label} is required.`)
  .refine((value) => value.trim().length <= max, `${label} is too long.`);

const date = /^\d{4}-\d{2}-\d{2}$/;
const validDate = (value: string) => {
  if (!date.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
};

export const draftCertificateSchema = z.object({
  recipientName: requiredText("Recipient name", 200),
  recipientEmail: z.string().refine((value) => z.email().safeParse(value.trim()).success,
    "Enter a valid email address.").refine((value) => value.trim().length <= 320, "Email is too long."),
  programName: requiredText("Program name", 300),
  description: z.string().max(2000, "Description is too long."),
  issueDate: z.string().refine(validDate, "Enter an issue date."),
  expiryDate: z.string().refine((value) => !value || validDate(value), "Enter a valid expiry date."),
}).refine((value) => !value.expiryDate || value.expiryDate >= value.issueDate, {
  path: ["expiryDate"], message: "Expiry date must be on or after issue date.",
});

export type DraftCertificateValues = z.infer<typeof draftCertificateSchema>;
