import { createContext } from "react"

import type { ResolvedTheme, ThemeMode } from "@/lib/theme"

export type ThemeContextValue = {
  mode: ThemeMode
  resolved: ResolvedTheme
  setMode: (mode: ThemeMode) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)
