import { Monitor, Moon, Sun } from "lucide-react"

import { useTheme } from "@/hooks/useTheme"
import type { ThemeMode } from "@/lib/theme"
import { cn } from "@/lib/utils"

const options: { mode: ThemeMode; label: string; icon: typeof Sun }[] = [
  { mode: "light", label: "Claro", icon: Sun },
  { mode: "dark", label: "Escuro", icon: Moon },
  { mode: "system", label: "Sistema", icon: Monitor },
]

/** Segmented control: light, dark or follow the system */
export function ThemeToggle() {
  const { mode, setMode } = useTheme()

  return (
    <div role="radiogroup" aria-label="Tema" className="grid grid-cols-3 gap-0.5 rounded-md border bg-background p-0.5">
      {options.map(({ mode: option, label, icon: Icon }) => {
        const selected = mode === option
        return (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={selected}
            title={label}
            onClick={() => setMode(option)}
            className={cn(
              "flex h-7 items-center justify-center gap-1.5 rounded-[4px] text-xs transition-colors duration-150 outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
              selected
                ? "bg-card text-foreground shadow-[0_0_0_1px_var(--border)]"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            <Icon className="size-3.5" />
            <span>{label}</span>
          </button>
        )
      })}
    </div>
  )
}
