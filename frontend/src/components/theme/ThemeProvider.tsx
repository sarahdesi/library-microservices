import { useEffect, useMemo, useState, type ReactNode } from "react"

import { applyTheme, onSystemThemeChange, readStoredMode, resolveTheme, storeMode, type ThemeMode } from "@/lib/theme"
import { ThemeContext } from "./ThemeContext"

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(readStoredMode)
  const [resolved, setResolved] = useState(() => resolveTheme(mode))

  useEffect(() => {
    const update = () => {
      const next = resolveTheme(mode)
      setResolved(next)
      applyTheme(next)
    }
    update()
    // "Sistema" follows the OS while the page is open
    return mode === "system" ? onSystemThemeChange(update) : undefined
  }, [mode])

  const value = useMemo(
    () => ({
      mode,
      resolved,
      setMode: (next: ThemeMode) => {
        storeMode(next)
        setModeState(next)
      },
    }),
    [mode, resolved],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
