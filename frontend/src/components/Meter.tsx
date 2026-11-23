import { cn } from "@/lib/utils"

type MeterProps = {
  value: number
  max: number
  /** "full" turns the bar into the warning color (no copies left, limit reached) */
  tone?: "default" | "full"
  className?: string
  label: string
}

/** Thin 4px bar, e.g. copies available or books in use */
export function Meter({ value, max, tone = "default", className, label }: MeterProps) {
  const percent = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0
  return (
    <div
      role="meter"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      className={cn("h-1 w-full overflow-hidden rounded-full bg-meter-track", className)}
    >
      <div
        className={cn("h-full rounded-full transition-[width] duration-150", tone === "full" ? "bg-warning" : "bg-primary")}
        style={{ width: `${percent}%` }}
      />
    </div>
  )
}
