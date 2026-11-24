export type ThemeMode = "light" | "dark" | "system"
export type ResolvedTheme = "light" | "dark"

const STORAGE_KEY = "theme"
const DARK_QUERY = "(prefers-color-scheme: dark)"

/** Storage can be unavailable (private mode, blocked site data): the app works without it */
export function readStoredMode(): ThemeMode {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    return value === "light" || value === "dark" || value === "system" ? value : "system"
  } catch {
    return "system"
  }
}

export function storeMode(mode: ThemeMode) {
  try {
    localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    // preference simply is not remembered
  }
}

export function systemPrefersDark(): boolean {
  return window.matchMedia(DARK_QUERY).matches
}

export function resolveTheme(mode: ThemeMode): ResolvedTheme {
  if (mode === "system") return systemPrefersDark() ? "dark" : "light"
  return mode
}

export function applyTheme(theme: ResolvedTheme) {
  document.documentElement.classList.toggle("dark", theme === "dark")
}

export function onSystemThemeChange(callback: () => void): () => void {
  const media = window.matchMedia(DARK_QUERY)
  media.addEventListener("change", callback)
  return () => media.removeEventListener("change", callback)
}
