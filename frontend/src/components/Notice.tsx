import type { ReactNode } from "react"
import { TriangleAlert } from "lucide-react"

import { cn } from "@/lib/utils"

type NoticeProps = {
  variant?: "danger" | "warning"
  title: string
  children?: ReactNode
}

export function Notice({ variant = "danger", title, children }: NoticeProps) {
  return (
    <div
      role="alert"
      className={cn(
        "flex gap-3 rounded-md border px-4 py-3",
        variant === "danger" ? "border-danger/25 bg-danger-bg text-danger" : "border-warning/25 bg-warning-bg text-warning",
      )}
    >
      <TriangleAlert className="mt-0.5 size-4 shrink-0" />
      <div>
        <p className="font-medium">{title}</p>
        {children && <div className="mt-0.5 text-[13px]">{children}</div>}
      </div>
    </div>
  )
}
