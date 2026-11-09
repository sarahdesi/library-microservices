const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "")

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
}

async function parseError(response: Response): Promise<ApiError> {
  let body: unknown = null
  try {
    body = await response.json()
  } catch {
    // empty or non-JSON body
  }

  if (body && typeof body === "object") {
    const { status, error, details } = body as Record<string, unknown>
    return new ApiError(
      typeof status === "number" ? status : response.status,
      typeof error === "string" ? error : response.statusText,
      Array.isArray(details) ? details.filter((d): d is string => typeof d === "string") : [],
    )
  }

  return new ApiError(response.status, response.statusText)
}

export async function request<T>(path: string, { method = "GET", body }: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new NetworkError()
  }

  if (!response.ok) {
    throw await parseError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
