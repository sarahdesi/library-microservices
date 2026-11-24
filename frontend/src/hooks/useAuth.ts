import { useContext } from "react"

import { AuthContext } from "@/auth/AuthContext"

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth must be used inside AuthProvider")
  return context
}

/** Logged-in user; only for pages behind RequireAuth */
export function useCurrentUser() {
  const { session } = useAuth()
  if (!session) throw new Error("useCurrentUser requires a session")
  return session.user
}
