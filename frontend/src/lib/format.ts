const currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })

/** "2026-11-09" → "09/11/2026", parsed from the text to avoid timezone shifts */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—"
  const [year, month, day] = value.slice(0, 10).split("-")
  if (!year || !month || !day) return value
  return `${day}/${month}/${year}`
}

/** 6 → "R$ 6,00" */
export function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

/** Today's date in the user's timezone, as "YYYY-MM-DD" */
export function todayIso(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${now.getFullYear()}-${month}-${day}`
}

/** Whole days from today to an ISO date (negative when in the past). Compared as calendar dates, no timezone shift. */
export function daysFromToday(value: string, today: string = todayIso()): number {
  const toUtc = (iso: string) => {
    const [year, month, day] = iso.slice(0, 10).split("-").map(Number)
    return Date.UTC(year, month - 1, day)
  }
  return Math.round((toUtc(value) - toUtc(today)) / 86_400_000)
}

/** 1 → "1 livro", 3 → "3 livros" */
export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : pluralForm}`
}

/** "Ana Maria Souza" → "AS" */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return "?"
  const first = parts[0][0]
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ""
  return (first + last).toUpperCase()
}
