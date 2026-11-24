import { Fragment, type ReactNode } from "react"
import { Link } from "react-router"
import { ChevronRight } from "lucide-react"

export type Crumb = { label: string; to?: string }

type PageHeaderProps = {
  title: ReactNode
  description?: ReactNode
  /** Small line of context below the description, e.g. "128 títulos · 12 indisponíveis" */
  meta?: ReactNode
  action?: ReactNode
  breadcrumbs?: Crumb[]
}

export function PageHeader({ title, description, meta, action, breadcrumbs }: PageHeaderProps) {
  return (
    <div className="mb-8">
      {breadcrumbs && breadcrumbs.length > 0 && (
        <nav aria-label="Trilha" className="mb-3 flex items-center gap-1 text-[13px] text-muted-foreground">
          {breadcrumbs.map((crumb, index) => (
            <Fragment key={`${crumb.label}-${index}`}>
              {index > 0 && <ChevronRight className="size-3.5" />}
              {crumb.to ? (
                <Link to={crumb.to} className="transition-colors duration-150 hover:text-foreground">
                  {crumb.label}
                </Link>
              ) : (
                <span aria-current="page" className="text-foreground">
                  {crumb.label}
                </span>
              )}
            </Fragment>
          ))}
        </nav>
      )}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="min-w-0">
          <h1 className="font-heading text-2xl leading-tight font-semibold">{title}</h1>
          {description && <p className="mt-1.5 text-[13px] text-muted-foreground">{description}</p>}
          {meta && <div className="mt-3 text-[13px] text-muted-foreground tabular-nums">{meta}</div>}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
    </div>
  )
}
