import { useMemo, useState } from "react"
import { Link } from "react-router"
import { Pencil, SearchX, Users } from "lucide-react"

import { EmptyState } from "@/components/EmptyState"
import { PageHeader } from "@/components/layout/PageHeader"
import { Monogram } from "@/components/Monogram"
import { ErrorState } from "@/components/QueryState"
import { SearchInput } from "@/components/SearchInput"
import { TableSkeleton } from "@/components/TableSkeleton"
import { UserFormDialog } from "@/components/users/UserFormDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useMembers } from "@/hooks/useMembers"
import { plural } from "@/lib/format"
import { userTypeLabels } from "@/lib/labels"
import { matchesSearch } from "@/lib/search"
import { cn } from "@/lib/utils"
import type { Member, UserType } from "@/types/user"

type TypeFilter = UserType | "ALL"

const filterLabels: Record<TypeFilter, string> = {
  ALL: "Todos",
  STUDENT: "Alunos",
  TEACHER: "Professores",
  EXTERNAL: "Externos",
}

export function UsersPage() {
  const [formOpen, setFormOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<Member | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("ALL")

  const usersQuery = useMembers()
  const { members } = usersQuery

  const counts = useMemo(() => {
    const result: Record<TypeFilter, number> = { ALL: members.length, STUDENT: 0, TEACHER: 0, EXTERNAL: 0 }
    members.forEach((member) => result[member.type]++)
    return result
  }, [members])

  const visible = useMemo(
    () =>
      members.filter(
        (member) =>
          (typeFilter === "ALL" || member.type === typeFilter) &&
          (!search.trim() || matchesSearch(search, member.name, member.email)),
      ),
    [members, typeFilter, search],
  )

  const openCreate = () => {
    setEditingUser(null)
    setFormOpen(true)
  }

  const openEdit = (user: Member) => {
    setEditingUser(user)
    setFormOpen(true)
  }

  const filtering = typeFilter !== "ALL" || search.trim() !== ""

  return (
    <>
      <PageHeader
        title="Usuários"
        description="Alunos, professores e usuários externos cadastrados."
        meta={
          members.length > 0 ? (
            <p>
              {plural(counts.ALL, "usuário")} · {plural(counts.STUDENT, "aluno")} ·{" "}
              {plural(counts.TEACHER, "professor", "professores")} · {plural(counts.EXTERNAL, "externo")}
            </p>
          ) : undefined
        }
        action={<Button onClick={openCreate}>Novo usuário</Button>}
      />

      <div className="rounded-md border bg-card">
        <div className="flex flex-col gap-3 border-b p-3 lg:flex-row lg:items-center">
          <SearchInput
            className="lg:w-80"
            aria-label="Buscar usuários"
            placeholder="Buscar por nome ou e-mail"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <div role="radiogroup" aria-label="Filtrar por tipo" className="flex flex-wrap gap-1">
            {(Object.keys(filterLabels) as TypeFilter[]).map((filter) => {
              const selected = typeFilter === filter
              return (
                <button
                  key={filter}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => setTypeFilter(filter)}
                  className={cn(
                    "inline-flex h-7 items-center gap-1.5 rounded-md border px-2.5 text-[13px] transition-colors duration-150 outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
                    selected
                      ? "border-foreground/20 bg-muted text-foreground"
                      : "border-transparent text-muted-foreground hover:bg-muted hover:text-foreground",
                  )}
                >
                  {filterLabels[filter]}
                  <span className="text-xs text-muted-foreground tabular-nums">{counts[filter]}</span>
                </button>
              )
            })}
          </div>
          {filtering && !usersQuery.isPending && (
            <p className="text-[13px] text-muted-foreground tabular-nums lg:ml-auto">
              {visible.length} de {counts.ALL}
            </p>
          )}
        </div>

        {usersQuery.isPending ? (
          <TableSkeleton columns={3} />
        ) : usersQuery.isError ? (
          <ErrorState error={usersQuery.error} onRetry={() => usersQuery.refetch()} />
        ) : members.length === 0 ? (
          <EmptyState
            icon={Users}
            title="Nenhum usuário cadastrado."
            action={
              <Button variant="outline" size="sm" onClick={openCreate}>
                Cadastrar usuário
              </Button>
            }
          />
        ) : visible.length === 0 ? (
          <EmptyState
            icon={SearchX}
            title="Nenhum usuário encontrado."
            action={
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setTypeFilter("ALL")
                }}
              >
                Limpar filtros
              </Button>
            }
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Nome</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead className="w-0">
                  <span className="sr-only">Ações</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((user) => (
                <TableRow key={user.id} className="group">
                  <TableCell className="whitespace-normal">
                    <div className="flex items-center gap-3">
                      <Monogram name={user.name} />
                      <div className="min-w-0">
                        <Link
                          to={`/usuarios/${user.id}`}
                          className="font-medium transition-colors duration-150 hover:text-primary"
                        >
                          {user.name}
                        </Link>
                        <p className="truncate text-[13px] text-muted-foreground">{user.email}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="neutral">{userTypeLabels[user.type]}</Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-3">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        title="Editar"
                        className="opacity-70 transition-opacity duration-150 group-hover:opacity-100 focus-visible:opacity-100"
                        onClick={() => openEdit(user)}
                      >
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
