import { Outlet } from "react-router"

import { AuthProvider } from "./AuthProvider"

export function RootLayout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
