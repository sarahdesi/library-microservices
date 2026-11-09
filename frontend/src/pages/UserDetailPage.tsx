import type { ReactNode } from "react"
import { Link, useParams } from "react-router"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ChevronLeft } from "lucide-react"

import { getUserFines, payFine } from "@/api/fines"
import { getUserLoans, getUserPending } from "@/api/loans"
import { getUser } from "@/api/users"
import { PageHeader } from "@/components/layout/PageHeader"
import { LoansTable } from "@/components/loans/LoansTable"
import { Notice } from "@/components/Notice"
import { ErrorState, LoadingState } from "@/components/QueryState"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useBooksById } from "@/hooks/useBooksById"
import { formatCurrency } from "@/lib/format"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { FINE_PER_DAY, userTypeRules } from "@/lib/rules"
import { showError, showSuccess } from "@/lib/toast"
import type { Fine } from "@/types/fine"
import type { Loan } from "@/types/loan"

function BackLink() {
  return (
    <Link
      to="/usuarios"
      className="mb-3 inline-flex items-center gap-1 text-[13px] text-muted-foreground transition-colors duration-150 hover:text-foreground"
    >
      <ChevronLeft className="size-4" />
      Usuários
    </Link>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-8">
      <h2 className="mb-3 text-[15px] font-medium">{title}</h2>
      {children}
    </section>
  )
}

function InfoItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 tabular-nums">{value}</dd>
    </div>
  )
}

export function UserDetailPage() {
  const { id } = useParams()
  const userId = Number(id)
  const validId = Number.isInteger(userId) && userId > 0

  const userQuery = useQuery({ queryKey: queryKeys.user(userId), queryFn: () => getUser(userId), enabled: validId })
  const pendingQuery = useQuery({
    queryKey: queryKeys.userPending(userId),
    queryFn: () => getUserPending(userId),
    enabled: validId,
  })
  const loansQuery = useQuery({
    queryKey: queryKeys.userLoans(userId),
    queryFn: () => getUserLoans(userId),
    enabled: validId,
  })
  const finesQuery = useQuery({
    queryKey: queryKeys.userFines(userId),
    queryFn: () => getUserFines(userId),
    enabled: validId,
  })
  const { booksById, isPending: booksPending } = useBooksById()

  if (!validId) {
    return (
      <>
        <BackLink />
        <p className="text-danger">Usuário não encontrado.</p>
      </>
    )
  }

  if (userQuery.isPending) return <LoadingState />
  if (userQuery.isError) {
    return (
      <>
        <BackLink />
        <ErrorState error={userQuery.error} onRetry={() => userQuery.refetch()} />
      </>
    )
  }

  const user = userQuery.data
  const rules = userTypeRules[user.type]
  const pending = pendingQuery.data
  const loansById = new Map<number, Loan>((loansQuery.data ?? []).map((loan) => [loan.id, loan]))
  const activeLoans = (loansQuery.data ?? []).filter((loan) => loan.status === "ACTIVE").length

  return (
    <>
      <BackLink />
      <PageHeader title={user.name} description={user.email} />

      {pending && (pending.hasPendingFines || pending.hasOverdueLoans) && (
        <div className="mb-6">
          <Notice title="Este usuário não pode fazer novos empréstimos.">
            {[pending.hasPendingFines && "Há multa pendente.", pending.hasOverdueLoans && "Há livro atrasado."]
              .filter(Boolean)
              .join(" ")}
          </Notice>
        </div>
      )}

      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 rounded-md border bg-card p-5 sm:grid-cols-3 lg:grid-cols-5">
        <InfoItem label="Tipo" value={<Badge variant="neutral">{userTypeLabels[user.type]}</Badge>} />
        <InfoItem label="Livros emprestados" value={`${activeLoans} de ${rules.maxBooks}`} />
        <InfoItem label="Prazo" value={`${rules.loanDays} dias`} />
        <InfoItem label="Renovações" value={`até ${rules.maxRenewals}`} />
        <InfoItem label="Multa por atraso" value={`${formatCurrency(FINE_PER_DAY)} por dia`} />
      </dl>

      <Section title="Empréstimos">
        <div className="rounded-md border bg-card">
          {loansQuery.isPending || booksPending ? (
            <LoadingState />
          ) : loansQuery.isError ? (
            <ErrorState error={loansQuery.error} onRetry={() => loansQuery.refetch()} />
          ) : loansQuery.data.length === 0 ? (
            <p className="py-10 text-center text-muted-foreground">Nenhum empréstimo registrado.</p>
          ) : (
            <LoansTable loans={loansQuery.data} booksById={booksById} maxRenewals={rules.maxRenewals} />
          )}
        </div>
      </Section>

      <Section title="Multas">
        <div className="rounded-md border bg-card">
          {finesQuery.isPending ? (
            <LoadingState />
          ) : finesQuery.isError ? (
            <ErrorState error={finesQuery.error} onRetry={() => finesQuery.refetch()} />
          ) : finesQuery.data.length === 0 ? (
            <p className="py-10 text-center text-muted-foreground">Nenhuma multa registrada.</p>
          ) : (
            <FinesTable
              fines={finesQuery.data}
              bookTitleForLoan={(loanId) => {
                const loan = loansById.get(loanId)
                return loan ? booksById.get(loan.bookId)?.title : undefined
              }}
            />
          )}
        </div>
      </Section>
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
