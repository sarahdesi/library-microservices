import { Skeleton } from "@/components/ui/skeleton"

/** Placeholder rows while a table loads (widths vary so it reads like text) */
export function TableSkeleton({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) {
  const widths = ["w-2/3", "w-1/2", "w-3/4", "w-1/3", "w-2/5"]
  return (
    <div role="status" aria-label="Carregando" className="divide-y">
      {Array.from({ length: rows }, (_, row) => (
        <div key={row} className="flex items-center gap-6 px-3 py-3.5">
          {Array.from({ length: columns }, (_, column) => (
            <div key={column} className="flex-1">
              <Skeleton className={`h-3 ${widths[(row + column) % widths.length]}`} />
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
