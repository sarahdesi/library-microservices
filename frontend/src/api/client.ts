import { readSession } from "@/lib/session"

const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "")

/** Called when a request that carried a token gets 401 (expired or invalid session) */
let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

/** HTTP error response from the gateway or one of the services */
export class ApiError extends Error {
  readonly status: number
  readonly details: string[]

  constructor(status: number, message: string, details: string[] = []) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.details = details
  }
}

/** The request never reached the server (gateway down, no network, CORS) */
export class NetworkError extends Error {
  constructor() {
    super("Network error")
    this.name = "NetworkError"
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE"
  body?: unknown
  /** false for login and public registration: no token is sent */
  auth?: boolean
}

async function parseError(response: Response): Promise<ApiError> {
  let body: unknown = null
  try {
    body = await response.json()
  } catch {
    // empty or non-JSON body
  }

  if (body && typeof body === "object") {
    // Services send the text in "message"; the gateway's own errors use "error"
    const { status, message, error, details } = body as Record<string, unknown>
    const text = typeof message === "string" && message ? message : typeof error === "string" ? error : response.statusText
    return new ApiError(
      typeof status === "number" ? status : response.status,
      text,
      Array.isArray(details) ? details.filter((d): d is string => typeof d === "string") : [],
    )
  }

  return new ApiError(response.status, response.statusText)
}

export async function request<T>(path: string, { method = "GET", body, auth = true }: RequestOptions = {}): Promise<T> {
  const token = auth ? readSession()?.token : undefined
  const headers: Record<string, string> = {}
  if (body !== undefined) headers["Content-Type"] = "application/json"
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new NetworkError()
  }

  if (!response.ok) {
    const error = await parseError(response)
    // Decided by the status, not the text (the gateway answers 401 itself)
    if (response.status === 401 && token) {
      unauthorizedHandler?.()
    }
    throw error
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
