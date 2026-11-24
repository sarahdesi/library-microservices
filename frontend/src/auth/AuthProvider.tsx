import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { useNavigate } from "react-router"
import { useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { login as loginRequest } from "@/api/auth"
import { setUnauthorizedHandler } from "@/api/client"
import { SESSION_EXPIRED_MESSAGE } from "@/lib/roles"
import { clearSession, readSession, saveSession, tokenExpiresAt } from "@/lib/session"
import type { Session } from "@/types/auth"
import { AuthContext } from "./AuthContext"

/** Rendered inside the router, so it can redirect to /login when the session ends */
export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(readSession)

  const endSession = useCallback(
    (expired: boolean) => {
      clearSession()
      setSession(null)
      // cached data belongs to the previous user
      queryClient.clear()
      if (expired) toast.error(SESSION_EXPIRED_MESSAGE, { id: "session-expired" })
      navigate("/login", { replace: true })
    },
    [navigate, queryClient],
  )

  // A 401 on any authenticated request ends the session
  useEffect(() => {
    setUnauthorizedHandler(() => endSession(true))
    return () => setUnauthorizedHandler(null)
  }, [endSession])

  // The token lasts 1 hour and there is no refresh token: log out when it expires
  useEffect(() => {
    if (!session) return
    const exp = tokenExpiresAt(session.token)
    if (exp === null) return
    const timer = window.setTimeout(() => endSession(true), Math.max(0, exp * 1000 - Date.now()))
    return () => window.clearTimeout(timer)
  }, [session, endSession])

  const value = useMemo(
    () => ({
      session,
      login: async (email: string, password: string) => {
        const response = await loginRequest({ email, password })
        const next = { token: response.token, user: response.user }
        queryClient.clear()
        saveSession(next)
        setSession(next)
        return next
      },
      logout: () => endSession(false),
    }),
    [session, endSession, queryClient],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
