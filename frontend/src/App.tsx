import { createBrowserRouter, Navigate, RouterProvider } from "react-router"

import { AppLayout } from "@/components/layout/AppLayout"
import { BooksPage } from "@/pages/BooksPage"
import { LoansPage } from "@/pages/LoansPage"
import { NotFoundPage } from "@/pages/NotFoundPage"
import { UserDetailPage } from "@/pages/UserDetailPage"
import { UsersPage } from "@/pages/UsersPage"

const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/livros" replace /> },
      { path: "livros", element: <BooksPage /> },
      { path: "usuarios", element: <UsersPage /> },
      { path: "usuarios/:id", element: <UserDetailPage /> },
      { path: "emprestimos", element: <LoansPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
