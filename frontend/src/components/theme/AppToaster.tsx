import { Toaster } from "sonner"

import { useTheme } from "@/hooks/useTheme"

/** Toasts follow the theme and use the same surfaces and borders as the rest of the app */
export function AppToaster() {
  const { resolved } = useTheme()
  return (
    <Toaster
      theme={resolved}
      position="bottom-right"
      toastOptions={{
        classNames: {
          toast: "!font-sans !text-sm !rounded-md !border !border-border !bg-popover !text-popover-foreground !shadow-sm",
          description: "!text-muted-foreground",
          success: "[&_[data-icon]]:!text-success",
          error: "[&_[data-icon]]:!text-danger",
        },
      }}
    />
  )
}
