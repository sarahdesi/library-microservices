import { useState } from "react"
import { NavLink, Outlet } from "react-router"
import { ArrowLeftRight, BookOpen, Menu, Users } from "lucide-react"

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
    <nav className="flex flex-col gap-0.5 px-3">
      {navItems.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "flex items-center gap-2.5 rounded-md px-2.5 py-2 transition-colors duration-150",
              isActive
                ? "bg-muted font-medium text-primary"
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

function AppName() {
  return <span className="font-heading text-lg font-semibold leading-tight">{APP_NAME}</span>
}

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="min-h-svh md:flex">
      {/* Desktop sidebar */}
      <aside className="hidden w-56 shrink-0 border-r bg-sidebar md:sticky md:top-0 md:flex md:h-svh md:flex-col">
        <div className="px-5 pt-6 pb-6">
          <AppName />
        </div>
        <SidebarNav />
      </aside>

      {/* Mobile top bar */}
      <header className="sticky top-0 z-40 flex h-14 items-center gap-2 border-b bg-sidebar px-3 md:hidden">
        <Button variant="ghost" size="icon" onClick={() => setMobileOpen(true)}>
          <Menu />
          <span className="sr-only">Abrir menu</span>
        </Button>
        <AppName />
      </header>

      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="w-56 gap-0 bg-sidebar">
          <SheetTitle className="px-5 pt-6 pb-6 text-lg font-semibold">{APP_NAME}</SheetTitle>
          <SheetDescription className="sr-only">Menu de navegação</SheetDescription>
          <SidebarNav onNavigate={() => setMobileOpen(false)} />
        </SheetContent>
      </Sheet>

      <main className="min-w-0 flex-1">
        <div className="mx-auto max-w-[1200px] px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
