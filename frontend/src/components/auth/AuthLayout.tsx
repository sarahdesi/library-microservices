import type { ReactNode } from "react"
import { Library } from "lucide-react"

import { ThemeToggle } from "@/components/theme/ThemeToggle"

/** Centered card for login and registration: no hero, just the form */
export function AuthLayout({ title, description, children, footer }: {
  title: string
  description: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2.5">
          <span className="inline-flex size-8 items-center justify-center rounded-md border bg-card text-primary">
            <Library className="size-4" strokeWidth={1.75} />
          </span>
          <div>
            <p className="font-heading text-[15px] leading-tight font-semibold">Biblioteca Universitária</p>
            <p className="text-xs text-muted-foreground">Sistema de empréstimos</p>
          </div>
        </div>

        <div className="rounded-md border bg-card p-6">
          <h1 className="font-heading text-xl font-semibold">{title}</h1>
          <p className="mt-1 mb-6 text-[13px] text-muted-foreground">{description}</p>
          {children}
        </div>

        {footer && <div className="mt-4 text-center text-[13px] text-muted-foreground">{footer}</div>}

        <div className="mx-auto mt-10 w-60">
          <ThemeToggle />
        </div>
      </div>
    </div>
  )
}
