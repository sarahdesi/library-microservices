import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from "react"

import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

type AutocompleteProps<T> = {
  id: string
  items: T[]
  /** Selected item key ("" when nothing is selected) */
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  getKey: (item: T) => string
  /** Text shown in the field once the item is selected */
  getLabel: (item: T) => string
  matches: (item: T, query: string) => boolean
  renderItem?: (item: T) => ReactNode
  placeholder?: string
  emptyText?: string
  disabled?: boolean
  invalid?: boolean
  maxResults?: number
}

export function Autocomplete<T>({
  id,
  items,
  value,
  onChange,
  onBlur,
  getKey,
  getLabel,
  matches,
  renderItem,
  placeholder,
  emptyText = "Nenhum resultado.",
  disabled,
  invalid,
  maxResults = 8,
}: AutocompleteProps<T>) {
  const listId = useId()
  const [query, setQuery] = useState("")
  const [open, setOpen] = useState(false)
  const [highlighted, setHighlighted] = useState(0)
  /** Set when the selection is cleared by typing, so the text the user typed is kept */
  const clearedByTyping = useRef(false)

  const selected = useMemo(() => items.find((item) => getKey(item) === value) ?? null, [items, value, getKey])

  // Keep the text in sync with selections and external resets (e.g. after submitting the form)
  useEffect(() => {
    if (clearedByTyping.current) {
      clearedByTyping.current = false
      return
    }
    setQuery(selected ? getLabel(selected) : "")
  }, [selected, getLabel])

  const results = useMemo(() => {
    if (!query.trim() || selected) return []
    return items.filter((item) => matches(item, query)).slice(0, maxResults)
  }, [items, query, selected, matches, maxResults])

  const showList = open && query.trim() !== "" && !selected

  const select = (item: T) => {
    onChange(getKey(item))
    setQuery(getLabel(item))
    setOpen(false)
  }

  const handleType = (text: string) => {
    setQuery(text)
    setHighlighted(0)
    setOpen(true)
    if (value) {
      clearedByTyping.current = true
      onChange("")
    }
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (!showList) return
    if (event.key === "ArrowDown") {
      event.preventDefault()
      setHighlighted((index) => Math.min(index + 1, results.length - 1))
    } else if (event.key === "ArrowUp") {
      event.preventDefault()
      setHighlighted((index) => Math.max(index - 1, 0))
    } else if (event.key === "Enter") {
      if (results[highlighted]) {
        event.preventDefault()
        select(results[highlighted])
      }
    } else if (event.key === "Escape") {
      setOpen(false)
    }
  }

  return (
    <div className="relative">
      <Input
        id={id}
        role="combobox"
        aria-expanded={showList}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={showList && results[highlighted] ? `${listId}-${highlighted}` : undefined}
        aria-invalid={invalid}
        autoComplete="off"
        placeholder={placeholder}
        disabled={disabled}
        value={query}
        onChange={(event) => handleType(event.target.value)}
        onFocus={() => setOpen(true)}
        onBlur={() => {
          setOpen(false)
          onBlur?.()
        }}
        onKeyDown={handleKeyDown}
      />

      {showList && (
        <ul
          id={listId}
          role="listbox"
          className="absolute top-full z-50 mt-1 max-h-64 w-full overflow-y-auto rounded-md border bg-popover p-1 shadow-md"
        >
          {results.length === 0 ? (
            <li className="px-2 py-1.5 text-muted-foreground">{emptyText}</li>
          ) : (
            results.map((item, index) => (
              <li
                key={getKey(item)}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={index === highlighted}
                className={cn(
                  "cursor-pointer rounded-sm px-2 py-1.5 transition-colors duration-150",
                  index === highlighted && "bg-muted",
                )}
                // mousedown instead of click: runs before the input loses focus
                onMouseDown={(event) => {
                  event.preventDefault()
                  select(item)
                }}
                onMouseEnter={() => setHighlighted(index)}
              >
                {renderItem ? renderItem(item) : getLabel(item)}
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}
