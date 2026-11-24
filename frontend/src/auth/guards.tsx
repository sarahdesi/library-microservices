import { useEffect } from "react"
import { Navigate, Outlet, useLocation } from "react-router"
import { toast } from "sonner"

import { useAuth } from "@/hooks/useAuth"
import { homePath, NO_PERMISSION_PAGE_MESSAGE } from "@/lib/roles"
import type { Role } from "@/types/user"

/** Without a session: go to /login, remembering where the user wanted to go */
export function RequireAuth() {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}

/** Login and registration are only for visitors */
export function PublicOnly() {
  const { session } = useAuth()
  if (session) return <Navigate to={homePath(session.user.role)} replace />
  return <Outlet />
}

/** Page of another profile: back to the start page with a toast (the backend enforces it anyway) */
export function RequireRole({ role }: { role: Role }) {
  const { session } = useAuth()
  const allowed = session?.user.role === role
  if (!session) return <Navigate to="/login" replace />
  if (!allowed) return <Forbidden to={homePath(session.user.role)} />
  return <Outlet />
}

function Forbidden({ to }: { to: string }) {
  useEffect(() => {
    toast.error(NO_PERMISSION_PAGE_MESSAGE, { id: "no-permission" })
  }, [])
  return <Navigate to={to} replace />
}

export function HomeRedirect() {
  const { session } = useAuth()
  return <Navigate to={session ? homePath(session.user.role) : "/login"} replace />
}
