/** Mirrors the backend: digits only (10 or 13, ISBN-10 may end in X) and a valid check digit */
export const ISBN_PATTERN = /^(\d{9}[\dX]|\d{13})$/

export function hasValidIsbnCheckDigit(isbn: string): boolean {
  if (isbn.length === 10) {
    const sum = isbn.split("").reduce((acc, char, i) => acc + (char === "X" ? 10 : Number(char)) * (10 - i), 0)
    return sum % 11 === 0
  }
  if (isbn.length === 13) {
    const sum = isbn.split("").reduce((acc, char, i) => acc + Number(char) * (i % 2 === 0 ? 1 : 3), 0)
    return sum % 10 === 0
  }
  return false
}

export const ISBN_MAX_LENGTH = 13

/** Removes hyphens (and spaces), uppercases the ISBN-10 "X" check digit */
export function stripIsbnSeparators(value: string): string {
  return value.replace(/[-\s]/g, "").toUpperCase()
}

/** Applied while typing/pasting: no separators and at most 13 characters */
export function normalizeIsbnInput(value: string): string {
  return stripIsbnSeparators(value).slice(0, ISBN_MAX_LENGTH)
}
