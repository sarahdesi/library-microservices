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
