import { createContext } from "react"

import type { Session } from "@/types/auth"

export type AuthContextValue = {
  session: Session | null
  login: (email: string, password: string) => Promise<Session>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
