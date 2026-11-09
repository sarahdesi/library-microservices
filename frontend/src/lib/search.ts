/** Lowercase and without accents, so "joao" matches "João" */
export function normalizeSearch(text: string): string {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim()
}

export function matchesSearch(query: string, ...fields: string[]): boolean {
  const normalized = normalizeSearch(query)
  return fields.some((field) => normalizeSearch(field).includes(normalized))
}
