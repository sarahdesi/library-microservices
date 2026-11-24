import { daysFromToday, formatDate } from "@/lib/format"
import { cn } from "@/lib/utils"

/** "09/11/2026" with a readable hint below: "vence em 3 dias", "vence hoje", "atrasado há 2 dias" */
export function DueDate({ date, active }: { date: string; active: boolean }) {
  if (!active) return <span className="tabular-nums">{formatDate(date)}</span>

  const days = daysFromToday(date)
  const overdue = days < 0
  const hint =
    days === 0
      ? "vence hoje"
      : days === 1
        ? "vence amanhã"
        : days > 1
          ? `vence em ${days} dias`
          : days === -1
            ? "atrasado há 1 dia"
            : `atrasado há ${-days} dias`

  return (
    <div className="leading-tight">
      <span className={cn("tabular-nums", overdue && "text-danger")}>{formatDate(date)}</span>
      <span className={cn("block text-xs", overdue ? "text-danger" : days <= 2 ? "text-warning" : "text-muted-foreground")}>
        {hint}
      </span>
    </div>
  )
}
