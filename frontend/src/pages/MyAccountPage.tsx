import type { ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { BookMarked, Receipt } from "lucide-react"

import { getMe, getMyFines, getMyLoans, getMyPending } from "@/api/me"
import { EmptyState } from "@/components/EmptyState"
import { FinesTable } from "@/components/fines/FinesTable"
import { PageHeader } from "@/components/layout/PageHeader"
import { LoansTable } from "@/components/loans/LoansTable"
import { Meter } from "@/components/Meter"
import { Monogram } from "@/components/Monogram"
import { Notice } from "@/components/Notice"
import { ErrorState } from "@/components/QueryState"
import { TableSkeleton } from "@/components/TableSkeleton"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useBooksById } from "@/hooks/useBooksById"
import { useCurrentUser } from "@/hooks/useAuth"
import { formatCurrency, plural } from "@/lib/format"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { FINE_PER_DAY, userTypeRules } from "@/lib/rules"
import type { Loan } from "@/types/loan"

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-2">
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="text-right tabular-nums">{children}</dd>
    </div>
  )
}

/** Member area: own data, rules, loans (renew only) and fines (read only) */
export function MyAccountPage() {
  const sessionUser = useCurrentUser()
  const meQuery = useQuery({ queryKey: queryKeys.me, queryFn: getMe })
  const pendingQuery = useQuery({ queryKey: queryKeys.myPending, queryFn: getMyPending })
  const loansQuery = useQuery({ queryKey: queryKeys.myLoans, queryFn: getMyLoans })
  const finesQuery = useQuery({ queryKey: queryKeys.myFines, queryFn: getMyFines })
  const { booksById, isPending: booksPending } = useBooksById()

  // the session already has the user: show it while /users/me refreshes it
  const user = meQuery.data ?? sessionUser
  const rules = user.type ? userTypeRules[user.type] : null
  const pending = pendingQuery.data
  const blocked = pending && (pending.hasPendingFines || pending.hasOverdueLoans)
  const loans = loansQuery.data ?? []
  const fines = finesQuery.data ?? []
  const loansById = new Map<number, Loan>(loans.map((loan) => [loan.id, loan]))
  const activeLoans = loans.filter((loan) => loan.status === "ACTIVE")
  const pendingFines = fines.filter((fine) => !fine.paid)
  const pendingAmount = pendingFines.reduce((sum, fine) => sum + fine.amount, 0)
  const overdue = activeLoans.filter((loan) => loan.overdue).length

  return (
    <>
      <PageHeader
        title="Minha conta"
        description="Seus dados, empréstimos e multas. Você pode renovar os empréstimos ativos; devoluções e pagamentos são feitos no balcão."
      />

      {blocked && (
        <div className="mb-6">
          <Notice title="Você não pode fazer novos empréstimos no momento.">
            {[
              pending.hasPendingFines && `Há multa pendente (${formatCurrency(pendingAmount)}), a pagar no balcão.`,
              pending.hasOverdueLoans && `Há ${plural(overdue || 1, "livro atrasado", "livros atrasados")}: devolva no balcão.`,
            ]
              .filter(Boolean)
              .join(" ")}
          </Notice>
        </div>
      )}

      <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="rounded-md border bg-card">
          <div className="flex items-center gap-3 border-b p-5">
            <Monogram name={user.name} size="lg" />
            <div className="min-w-0">
              <p className="truncate font-medium">{user.name}</p>
              <p className="truncate text-[13px] text-muted-foreground">{user.email}</p>
              <div className="mt-1.5 flex items-center gap-1.5">
                {user.type && <Badge variant="neutral">{userTypeLabels[user.type]}</Badge>}
                {pending &&
                  (blocked ? <Badge variant="danger">Com pendências</Badge> : <Badge variant="success">Regular</Badge>)}
              </div>
            </div>
          </div>

          {rules && (
            <div className="p-5">
              <p className="mb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Em uso</p>
              <div className="flex items-baseline justify-between">
                {loansQuery.isPending ? (
                  <Skeleton className="h-6 w-16" />
                ) : (
                  <span className="font-heading text-xl font-semibold tabular-nums">
                    {activeLoans.length}
                    <span className="font-sans text-sm font-normal text-muted-foreground"> de {rules.maxBooks}</span>
                  </span>
                )}
              </div>
              <Meter
                className="mt-2"
                value={activeLoans.length}
                max={rules.maxBooks}
                tone={activeLoans.length >= rules.maxBooks ? "full" : "default"}
                label={`${activeLoans.length} de ${rules.maxBooks} livros em uso`}
              />

              <p className="mt-6 mb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">
                Suas regras
              </p>
              <dl className="divide-y">
                <Row label="Máx. de livros">{rules.maxBooks}</Row>
                <Row label="Prazo">{plural(rules.loanDays, "dia")}</Row>
                <Row label="Renovações">até {rules.maxRenewals}</Row>
                <Row label="Multa por atraso">{formatCurrency(FINE_PER_DAY)} / dia</Row>
              </dl>
              <p className="mt-3 text-xs text-muted-foreground">Empréstimos atrasados não podem ser renovados.</p>
            </div>
          )}
        </aside>

        <Tabs defaultValue="loans">
          <TabsList>
            <TabsTrigger value="loans">Empréstimos</TabsTrigger>
            <TabsTrigger value="fines">
              Multas
              {pendingFines.length > 0 && (
                <span className="rounded-[4px] bg-warning-bg px-1.5 text-xs text-warning tabular-nums">
                  {pendingFines.length}
                </span>
              )}
            </TabsTrigger>
          </TabsList>

          <TabsContent value="loans">
            <div className="rounded-md border bg-card">
              {loansQuery.isPending || booksPending ? (
                <TableSkeleton columns={5} rows={3} />
              ) : loansQuery.isError ? (
                <ErrorState error={loansQuery.error} onRetry={() => loansQuery.refetch()} />
              ) : loans.length === 0 ? (
                <EmptyState
                  icon={BookMarked}
                  title="Você ainda não tem empréstimos."
                  description="Os empréstimos são registrados no balcão da biblioteca."
                />
              ) : (
                <LoansTable loans={loans} booksById={booksById} maxRenewals={rules?.maxRenewals} mode="member" />
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
                <EmptyState icon={Receipt} title="Nenhuma multa." />
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
                Total pendente: <span className="text-foreground tabular-nums">{formatCurrency(pendingAmount)}</span> ·
                pagamento no balcão
              </p>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}
