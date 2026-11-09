import { toast } from "sonner"

import { describeError } from "./errors"

export function showSuccess(message: string) {
  toast.success(message)
}

export function showError(error: unknown) {
  const { message, details } = describeError(error)
  toast.error(message, details.length > 0 ? { description: details.join("\n") } : undefined)
}
