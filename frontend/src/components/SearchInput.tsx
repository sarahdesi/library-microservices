import { forwardRef, useEffect, useImperativeHandle, useRef } from "react"
import { Search } from "lucide-react"

import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

type SearchInputProps = Omit<React.ComponentProps<"input">, "type"> & {
  /** Focus with the "/" key (ignored while typing in another field) */
  shortcut?: boolean
}

export const SearchInput = forwardRef<HTMLInputElement, SearchInputProps>(function SearchInput(
  { className, shortcut = true, ...props },
  forwardedRef,
) {
  const ref = useRef<HTMLInputElement>(null)
  useImperativeHandle(forwardedRef, () => ref.current!)

  useEffect(() => {
    if (!shortcut) return
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      const typing = target && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName))
      if (event.key === "/" && !typing && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault()
        ref.current?.focus()
      }
    }
    document.addEventListener("keydown", onKeyDown)
    return () => document.removeEventListener("keydown", onKeyDown)
  }, [shortcut])

  return (
    <div className={cn("relative", className)}>
      <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input ref={ref} type="search" className="peer pr-8 pl-8" {...props} />
      {shortcut && (
        <kbd
          aria-hidden
          className="pointer-events-none absolute top-1/2 right-2 hidden -translate-y-1/2 rounded-[4px] border bg-muted px-1.5 font-sans text-[11px] leading-4 text-muted-foreground peer-focus:opacity-0 sm:block"
        >
          /
        </kbd>
      )}
    </div>
  )
})
