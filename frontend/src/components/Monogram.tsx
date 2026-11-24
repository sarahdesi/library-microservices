import { initials } from "@/lib/format"
import { cn } from "@/lib/utils"

/** Initials in a small square with discreet corners (like a card label, not an avatar bubble) */
export function Monogram({ name, size = "sm", className }: { name: string; size?: "sm" | "lg"; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-md border bg-muted font-medium text-muted-foreground",
        size === "sm" ? "size-7 text-[11px]" : "size-11 font-heading text-base",
        className,
      )}
    >
      {initials(name)}
    </span>
  )
}
