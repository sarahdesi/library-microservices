import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import "./index.css"
import App from "./App.tsx"
import { AppToaster } from "@/components/theme/AppToaster"
import { ThemeProvider } from "@/components/theme/ThemeProvider"

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
  },
})

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <App />
        <AppToaster />
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
