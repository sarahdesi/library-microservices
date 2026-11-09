import { useState } from "react"
import { Link } from "react-router"
import { useQuery } from "@tanstack/react-query"
import { Pencil } from "lucide-react"

import { getUsers } from "@/api/users"
import { PageHeader } from "@/components/layout/PageHeader"
import { ErrorState, LoadingState } from "@/components/QueryState"
import { UserFormDialog } from "@/components/users/UserFormDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import type { User } from "@/types/user"

export function UsersPage() {
  const [formOpen, setFormOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)

  const usersQuery = useQuery({ queryKey: queryKeys.users, queryFn: getUsers })
  const users = usersQuery.data ?? []

  const openCreate = () => {
    setEditingUser(null)
    setFormOpen(true)
  }

  const openEdit = (user: User) => {
    setEditingUser(user)
    setFormOpen(true)
  }

  return (
    <>
      <PageHeader
        title="Usuários"
        description="Alunos, professores e usuários externos cadastrados."
        action={<Button onClick={openCreate}>Novo usuário</Button>}
      />

      <div className="rounded-md border bg-card">
        {usersQuery.isPending ? (
          <LoadingState />
        ) : usersQuery.isError ? (
          <ErrorState error={usersQuery.error} onRetry={() => usersQuery.refetch()} />
        ) : users.length === 0 ? (
          <div className="flex items-center justify-center gap-3 py-10">
            <p className="text-muted-foreground">Nenhum usuário cadastrado.</p>
            <Button variant="outline" size="sm" onClick={openCreate}>
              Cadastrar usuário
            </Button>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Nome</TableHead>
                <TableHead>E-mail</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead className="w-0">
                  <span className="sr-only">Ações</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id}>
                  <TableCell className="font-medium whitespace-normal">{user.name}</TableCell>
                  <TableCell className="text-muted-foreground">{user.email}</TableCell>
                  <TableCell>
                    <Badge variant="neutral">{userTypeLabels[user.type]}</Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-3">
                      <Button variant="ghost" size="icon-sm" title="Editar" onClick={() => openEdit(user)}>
                        <Pencil />
                        <span className="sr-only">Editar {user.name}</span>
                      </Button>
                      <Link
                        to={`/usuarios/${user.id}`}
                        className="whitespace-nowrap text-primary transition-colors duration-150 hover:text-primary-hover hover:underline"
                      >
                        Ver detalhes
                      </Link>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <UserFormDialog open={formOpen} onOpenChange={setFormOpen} user={editingUser} />
    </>
  )
}
