import { useState } from "react"
import { NavLink, Outlet } from "react-router"
import { ArrowLeftRight, BookOpen, Library, Menu, Users } from "lucide-react"

import { ThemeToggle } from "@/components/theme/ThemeToggle"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetDescription, SheetTitle } from "@/components/ui/sheet"
import { cn } from "@/lib/utils"

const navItems = [
  { to: "/livros", label: "Livros", icon: BookOpen },
  { to: "/usuarios", label: "Usuários", icon: Users },
  { to: "/emprestimos", label: "Empréstimos", icon: ArrowLeftRight },
]

const APP_NAME = "Biblioteca Universitária"

function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="Principal" className="flex flex-col gap-0.5 px-3">
      <p className="px-2.5 pb-2 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Balcão</p>
      {navItems.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "relative flex items-center gap-2.5 rounded-md px-2.5 py-2 transition-colors duration-150",
              // Active item: muted background and a 2px bar in the accent color
              "before:absolute before:top-2 before:bottom-2 before:left-0 before:w-0.5 before:rounded-full before:bg-primary before:opacity-0 before:transition-opacity before:duration-150",
              isActive
                ? "bg-muted font-medium text-foreground before:opacity-100 [&>svg]:text-primary"
                : "text-muted-foreground hover:bg-muted hover:text-foreground",
            )
          }
        >
          <Icon className="size-4" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

function Brand() {
  return (
    <div className="flex items-start gap-2.5">
      <span className="mt-0.5 inline-flex size-7 shrink-0 items-center justify-center rounded-md border bg-background text-primary">
        <Library className="size-4" strokeWidth={1.75} />
      </span>
      <div className="min-w-0">
        <p className="font-heading text-[15px] leading-tight font-semibold">{APP_NAME}</p>
        <p className="mt-0.5 text-xs text-muted-foreground">Sistema de empréstimos</p>
      </div>
    </div>
  )
}

function SidebarFooter() {
  return (
    <div className="mt-auto border-t px-3 py-4">
      <p className="px-0.5 pb-2 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Tema</p>
      <ThemeToggle />
    </div>
  )
}

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="min-h-svh md:flex">
      {/* Desktop sidebar */}
      <aside className="hidden w-60 shrink-0 border-r bg-sidebar md:sticky md:top-0 md:flex md:h-svh md:flex-col">
        <div className="px-5 pt-6 pb-7">
          <Brand />
        </div>
        <SidebarNav />
        <SidebarFooter />
      </aside>

      {/* Mobile top bar */}
      <header className="sticky top-0 z-40 flex h-14 items-center gap-2 border-b bg-sidebar px-3 md:hidden">
        <Button variant="ghost" size="icon" onClick={() => setMobileOpen(true)}>
          <Menu />
          <span className="sr-only">Abrir menu</span>
        </Button>
        <span className="font-heading text-[15px] font-semibold">{APP_NAME}</span>
      </header>

      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="flex w-64 flex-col gap-0 bg-sidebar">
          <SheetTitle className="sr-only">{APP_NAME}</SheetTitle>
          <SheetDescription className="sr-only">Menu de navegação</SheetDescription>
          <div className="px-5 pt-6 pb-7">
            <Brand />
          </div>
          <SidebarNav onNavigate={() => setMobileOpen(false)} />
          <SidebarFooter />
        </SheetContent>
      </Sheet>

      <main className="min-w-0 flex-1">
        <div className="mx-auto max-w-[1200px] px-4 py-6 md:px-10 md:py-10">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
