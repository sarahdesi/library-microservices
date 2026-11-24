import type { Session } from "@/types/auth"

const STORAGE_KEY = "session"

/** Seconds since epoch from the JWT "exp" claim (the payload is only read, never trusted for authorization) */
export function tokenExpiresAt(token: string): number | null {
  try {
    const payload = token.split(".")[1]
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
    const exp = JSON.parse(json).exp
    return typeof exp === "number" ? exp : null
  } catch {
    return null
  }
}

export function isExpired(token: string): boolean {
  const exp = tokenExpiresAt(token)
  return exp === null || exp * 1000 <= Date.now()
}

/** Storage can be unavailable (private mode, blocked site data): then the session lasts only while the tab is open */
let memorySession: Session | null = null

export function readSession(): Session | null {
  let session = memorySession
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) session = JSON.parse(raw) as Session
  } catch {
    // keep the in-memory copy
  }
  if (!session || typeof session.token !== "string" || isExpired(session.token)) return null
  return session
}

export function saveSession(session: Session) {
  memorySession = session
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // in-memory only
  }
}

export function clearSession() {
  memorySession = null
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // nothing stored
  }
}
