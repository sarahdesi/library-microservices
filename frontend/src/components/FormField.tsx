import type { ReactNode } from "react"

import { Label } from "@/components/ui/label"

type FormFieldProps = {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}

export function FormField({ id, label, error, hint, children }: FormFieldProps) {
  return (
    <div className="grid gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error ? (
        <p className="text-[13px] text-danger">{error}</p>
      ) : (
        hint && <p className="text-[13px] text-muted-foreground">{hint}</p>
      )}
    </div>
  )
}
