import { createBrowserRouter, RouterProvider } from "react-router"

import { HomeRedirect, PublicOnly, RequireAuth, RequireRole } from "@/auth/guards"
import { RootLayout } from "@/auth/RootLayout"
import { AppLayout } from "@/components/layout/AppLayout"
import { BooksPage } from "@/pages/BooksPage"
import { LoansPage } from "@/pages/LoansPage"
import { LoginPage } from "@/pages/LoginPage"
import { MyAccountPage } from "@/pages/MyAccountPage"
import { NotFoundPage } from "@/pages/NotFoundPage"
import { RegisterPage } from "@/pages/RegisterPage"
import { UserDetailPage } from "@/pages/UserDetailPage"
import { UsersPage } from "@/pages/UsersPage"

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      // visitors only
      {
        element: <PublicOnly />,
        children: [
          { path: "login", element: <LoginPage /> },
          { path: "cadastro", element: <RegisterPage /> },
        ],
      },
      // everything else needs a session
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { index: true, element: <HomeRedirect /> },
              // catalog: full for the librarian, read-only for members
              { path: "livros", element: <BooksPage /> },
              {
                element: <RequireRole role="LIBRARIAN" />,
                children: [
                  { path: "usuarios", element: <UsersPage /> },
                  { path: "usuarios/:id", element: <UserDetailPage /> },
                  { path: "emprestimos", element: <LoansPage /> },
                ],
              },
              {
                element: <RequireRole role="MEMBER" />,
                children: [{ path: "minha-conta", element: <MyAccountPage /> }],
              },
              { path: "*", element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
