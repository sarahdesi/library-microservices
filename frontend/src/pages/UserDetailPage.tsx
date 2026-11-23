import type { ReactNode } from "react"
import { useParams } from "react-router"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { BookMarked, Receipt } from "lucide-react"

import { getUserFines, payFine } from "@/api/fines"
import { getUserLoans, getUserPending } from "@/api/loans"
import { getUser } from "@/api/users"
import { EmptyState } from "@/components/EmptyState"
import { PageHeader } from "@/components/layout/PageHeader"
import { LoansTable } from "@/components/loans/LoansTable"
import { Meter } from "@/components/Meter"
import { Monogram } from "@/components/Monogram"
import { Notice } from "@/components/Notice"
import { ErrorState } from "@/components/QueryState"
import { TableSkeleton } from "@/components/TableSkeleton"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useBooksById } from "@/hooks/useBooksById"
import { formatCurrency, plural } from "@/lib/format"
import { roleLabels, userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { FINE_PER_DAY, userTypeRules } from "@/lib/rules"
import { showError, showSuccess } from "@/lib/toast"
import type { Fine } from "@/types/fine"
import type { Loan } from "@/types/loan"
import { isMember, type Member } from "@/types/user"

const usersCrumb = { label: "Usuários", to: "/usuarios" }

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-2">
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="text-right tabular-nums">{children}</dd>
    </div>
  )
}

function CountTag({ value, tone = "neutral" }: { value: number; tone?: "neutral" | "warning" }) {
  return (
    <span
      className={
        tone === "warning"
          ? "rounded-[4px] bg-warning-bg px-1.5 text-xs text-warning tabular-nums"
          : "rounded-[4px] bg-muted px-1.5 text-xs text-muted-foreground tabular-nums"
      }
    >
      {value}
    </span>
  )
}

export function UserDetailPage() {
  const { id } = useParams()
  const userId = Number(id)
  const validId = Number.isInteger(userId) && userId > 0

  const userQuery = useQuery({ queryKey: queryKeys.user(userId), queryFn: () => getUser(userId), enabled: validId })

  if (!validId) {
    return (
      <>
        <PageHeader breadcrumbs={[usersCrumb]} title="Usuário não encontrado" />
      </>
    )
  }

  if (userQuery.isPending) {
    return (
      <div role="status" aria-label="Carregando">
        <Skeleton className="mb-4 h-3 w-40" />
        <Skeleton className="mb-2 h-6 w-64" />
        <Skeleton className="mb-10 h-3 w-48" />
        <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
          <Skeleton className="h-72" />
          <Skeleton className="h-72" />
        </div>
      </div>
    )
  }

  if (userQuery.isError) {
    return (
      <>
        <PageHeader breadcrumbs={[usersCrumb]} title="Usuário" />
        <div className="rounded-md border bg-card">
          <ErrorState error={userQuery.error} onRetry={() => userQuery.refetch()} />
        </div>
      </>
    )
  }

  const user = userQuery.data
  if (!isMember(user)) {
    // The librarian is a user too, but has no type, loans or fines
    return (
      <>
        <PageHeader breadcrumbs={[usersCrumb, { label: user.name }]} title={user.name} description={user.email} />
        <div className="rounded-md border bg-card p-5 text-muted-foreground">
          Conta de {roleLabels[user.role].toLowerCase()}. Não toma livros emprestados.
        </div>
      </>
    )
  }

  return <MemberDetail member={user} />
}

function MemberDetail({ member }: { member: Member }) {
  const userId = member.id
  const pendingQuery = useQuery({ queryKey: queryKeys.userPending(userId), queryFn: () => getUserPending(userId) })
  const loansQuery = useQuery({ queryKey: queryKeys.userLoans(userId), queryFn: () => getUserLoans(userId) })
  const finesQuery = useQuery({ queryKey: queryKeys.userFines(userId), queryFn: () => getUserFines(userId) })
  const { booksById, isPending: booksPending } = useBooksById()

  const rules = userTypeRules[member.type]
  const pending = pendingQuery.data
  const blocked = pending && (pending.hasPendingFines || pending.hasOverdueLoans)
  const loans = loansQuery.data ?? []
  const fines = finesQuery.data ?? []
  const loansById = new Map<number, Loan>(loans.map((loan) => [loan.id, loan]))
  const activeLoans = loans.filter((loan) => loan.status === "ACTIVE").length
  const pendingFines = fines.filter((fine) => !fine.paid)
  const pendingAmount = pendingFines.reduce((sum, fine) => sum + fine.amount, 0)

  return (
    <>
      <PageHeader
        breadcrumbs={[usersCrumb, { label: member.name }]}
        title={member.name}
        description={member.email}
      />

      {blocked && (
        <div className="mb-6">
          <Notice title="Este usuário não pode fazer novos empréstimos.">
            {[
              pending.hasPendingFines && `Há multa pendente (${formatCurrency(pendingAmount)}).`,
              pending.hasOverdueLoans && "Há livro atrasado.",
            ]
              .filter(Boolean)
              .join(" ")}
          </Notice>
        </div>
      )}

      <div className="grid items-start gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        {/* Library card */}
        <aside className="rounded-md border bg-card">
          <div className="flex items-center gap-3 border-b p-5">
            <Monogram name={member.name} size="lg" />
            <div className="min-w-0">
              <p className="truncate font-medium">{member.name}</p>
              <div className="mt-1 flex items-center gap-1.5">
                <Badge variant="neutral">{userTypeLabels[member.type]}</Badge>
                {pending &&
                  (blocked ? <Badge variant="danger">Com pendências</Badge> : <Badge variant="success">Regular</Badge>)}
              </div>
            </div>
          </div>

          <div className="p-5">
            <p className="mb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Em uso</p>
            <div className="flex items-baseline justify-between">
              <span className="font-heading text-xl font-semibold tabular-nums">
                {loansQuery.isPending ? "–" : activeLoans}
                <span className="font-sans text-sm font-normal text-muted-foreground"> de {rules.maxBooks}</span>
              </span>
              <span className="text-[13px] text-muted-foreground">
                {activeLoans >= rules.maxBooks ? "limite atingido" : `${rules.maxBooks - activeLoans} livre${rules.maxBooks - activeLoans === 1 ? "" : "s"}`}
              </span>
            </div>
            <Meter
              className="mt-2"
              value={activeLoans}
              max={rules.maxBooks}
              tone={activeLoans >= rules.maxBooks ? "full" : "default"}
              label={`${activeLoans} de ${rules.maxBooks} livros em uso`}
            />

            <p className="mt-6 mb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">
              Regras do tipo
            </p>
            <dl className="divide-y">
              <Row label="Máx. de livros">{rules.maxBooks}</Row>
              <Row label="Prazo">{plural(rules.loanDays, "dia")}</Row>
              <Row label="Renovações">até {rules.maxRenewals}</Row>
              <Row label="Multa por atraso">{formatCurrency(FINE_PER_DAY)} / dia</Row>
            </dl>
          </div>
        </aside>

        <Tabs defaultValue="loans">
          <TabsList>
            <TabsTrigger value="loans">
              Empréstimos {!loansQuery.isPending && <CountTag value={loans.length} />}
            </TabsTrigger>
            <TabsTrigger value="fines">
              Multas{" "}
              {!finesQuery.isPending && (
                <CountTag value={pendingFines.length > 0 ? pendingFines.length : fines.length} tone={pendingFines.length > 0 ? "warning" : "neutral"} />
              )}
            </TabsTrigger>
          </TabsList>

          <TabsContent value="loans">
            <div className="rounded-md border bg-card">
              {loansQuery.isPending || booksPending ? (
                <TableSkeleton columns={6} rows={3} />
              ) : loansQuery.isError ? (
                <ErrorState error={loansQuery.error} onRetry={() => loansQuery.refetch()} />
              ) : loans.length === 0 ? (
                <EmptyState icon={BookMarked} title="Nenhum empréstimo registrado." />
              ) : (
                <LoansTable loans={loans} booksById={booksById} maxRenewals={rules.maxRenewals} />
              )}
            </div>
          </TabsContent>

          <TabsContent value="fines">
            <div className="rounded-md border bg-card">
              {finesQuery.isPending ? (
                <TableSkeleton columns={4} rows={2} />
              ) : finesQuery.isError ? (
                <ErrorState error={finesQuery.error} onRetry={() => finesQuery.refetch()} />
              ) : fines.length === 0 ? (
                <EmptyState icon={Receipt} title="Nenhuma multa registrada." />
              ) : (
                <FinesTable
                  fines={fines}
                  bookTitleForLoan={(loanId) => {
                    const loan = loansById.get(loanId)
                    return loan ? booksById.get(loan.bookId)?.title : undefined
                  }}
                />
              )}
            </div>
            {pendingFines.length > 0 && (
              <p className="mt-3 text-right text-[13px] text-muted-foreground">
                Total pendente: <span className="text-foreground tabular-nums">{formatCurrency(pendingAmount)}</span>
              </p>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}

type FinesTableProps = {
  fines: Fine[]
  bookTitleForLoan: (loanId: number) => string | undefined
}

function FinesTable({ fines, bookTitleForLoan }: FinesTableProps) {
  const queryClient = useQueryClient()

  const payMutation = useMutation({
    mutationFn: (fine: Fine) => payFine(fine.id),
    onSuccess: (paid) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.userFines(paid.userId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.userPending(paid.userId) })
      showSuccess("Multa paga.")
    },
    onError: showError,
  })

  return (
    <Table>
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead>Livro</TableHead>
          <TableHead className="text-right">Dias de atraso</TableHead>
          <TableHead className="text-right">Valor</TableHead>
          <TableHead>Situação</TableHead>
          <TableHead className="w-0">
            <span className="sr-only">Ações</span>
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {fines.map((fine) => {
          const title = bookTitleForLoan(fine.loanId)
          return (
            <TableRow key={fine.id}>
              <TableCell className="whitespace-normal">
                {title ?? <span className="text-muted-foreground">Empréstimo nº {fine.loanId}</span>}
              </TableCell>
              <TableCell className="text-right tabular-nums">{fine.daysLate}</TableCell>
              <TableCell className="text-right tabular-nums">{formatCurrency(fine.amount)}</TableCell>
              <TableCell>
                <Badge variant={fine.paid ? "success" : "warning"}>{fine.paid ? "Paga" : "Pendente"}</Badge>
              </TableCell>
              <TableCell>
                {!fine.paid && (
                  <div className="flex justify-end">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={payMutation.isPending && payMutation.variables?.id === fine.id}
                      onClick={() => payMutation.mutate(fine)}
                    >
                      Pagar
                    </Button>
                  </div>
                )}
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
