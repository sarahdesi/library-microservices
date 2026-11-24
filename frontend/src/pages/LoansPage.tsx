import { useMemo } from "react"
import { Link } from "react-router"
import { Controller, useForm, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

import { BookMarked, CircleCheck, TriangleAlert, UserRound } from "lucide-react"

import { createLoan, getUserLoans, getUserPending } from "@/api/loans"
import { Autocomplete } from "@/components/Autocomplete"
import { EmptyState } from "@/components/EmptyState"
import { FormField } from "@/components/FormField"
import { PageHeader } from "@/components/layout/PageHeader"
import { LoansTable } from "@/components/loans/LoansTable"
import { Meter } from "@/components/Meter"
import { Monogram } from "@/components/Monogram"
import { ErrorState } from "@/components/QueryState"
import { TableSkeleton } from "@/components/TableSkeleton"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useBooksById } from "@/hooks/useBooksById"
import { useMembers } from "@/hooks/useMembers"
import { formatDate, plural } from "@/lib/format"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { userTypeRules } from "@/lib/rules"
import { matchesSearch } from "@/lib/search"
import { showError, showSuccess } from "@/lib/toast"
import type { Book } from "@/types/book"
import type { Loan, LoanRequest, Pending } from "@/types/loan"
import type { Member } from "@/types/user"

const userKey = (user: Member) => String(user.id)
const userLabel = (user: Member) => user.name
const userMatches = (user: Member, query: string) => matchesSearch(query, user.name, user.email)

const bookKey = (book: Book) => book.id
const bookLabel = (book: Book) => book.title
const bookMatches = (book: Book, query: string) => matchesSearch(query, book.title, book.author)

const loanSchema = z.object({
  userId: z.string().min(1, "Selecione o usuário."),
  bookId: z.string().min(1, "Selecione o livro."),
})

type LoanFormValues = z.infer<typeof loanSchema>

export function LoansPage() {
  const queryClient = useQueryClient()

  const usersQuery = useMembers()
  const { booksById, data: books, isPending: booksPending, isError: booksError } = useBooksById()

  const {
    control,
    handleSubmit,
    resetField,
    formState: { errors },
  } = useForm<LoanFormValues>({ resolver: zodResolver(loanSchema), defaultValues: { userId: "", bookId: "" } })

  const selectedUserId = Number(useWatch({ control, name: "userId" })) || null
  const selectedUser = usersQuery.members.find((user) => user.id === selectedUserId) ?? null

  const availableBooks = useMemo(
    () =>
      (books ?? [])
        .filter((book) => book.availableCopies > 0)
        .sort((a, b) => a.title.localeCompare(b.title, "pt-BR")),
    [books],
  )

  const sortedUsers = usersQuery.members

  const pendingQuery = useQuery({
    queryKey: queryKeys.userPending(selectedUserId ?? 0),
    queryFn: () => getUserPending(selectedUserId!),
    enabled: selectedUserId !== null,
  })

  const loansQuery = useQuery({
    queryKey: queryKeys.userLoans(selectedUserId ?? 0),
    queryFn: () => getUserLoans(selectedUserId!),
    enabled: selectedUserId !== null,
  })

  const loanMutation = useMutation({
    mutationFn: (data: LoanRequest) => createLoan(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["loans"] })
      queryClient.invalidateQueries({ queryKey: queryKeys.books })
      showSuccess("Empréstimo registrado.")
      resetField("bookId")
    },
    onError: showError,
  })

  const onSubmit = (values: LoanFormValues) =>
    loanMutation.mutate({ userId: Number(values.userId), bookId: values.bookId })

  const pending = pendingQuery.data
  const rules = selectedUser ? userTypeRules[selectedUser.type] : null
  const activeLoans = (loansQuery.data ?? []).filter((loan) => loan.status === "ACTIVE").length
  const limitReached = rules !== null && activeLoans >= rules.maxBooks

  return (
    <>
      <PageHeader title="Empréstimos" description="Registre empréstimos e consulte os empréstimos de cada usuário." />

      <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="rounded-md border bg-card p-5">
          <h2 className="mb-1 text-[15px] font-medium">Novo empréstimo</h2>
          <p className="mb-5 text-[13px] text-muted-foreground">Só aparecem livros com exemplar disponível.</p>

          {usersQuery.isError ? (
            <ErrorState error={usersQuery.error} onRetry={() => usersQuery.refetch()} />
          ) : (
            <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="grid gap-4 md:grid-cols-2">
                <FormField id="loan-user" label="Usuário" error={errors.userId?.message}>
                  <Controller
                    control={control}
                    name="userId"
                    render={({ field }) => (
                      <Autocomplete
                        id="loan-user"
                        items={sortedUsers}
                        value={field.value}
                        onChange={field.onChange}
                        onBlur={field.onBlur}
                        getKey={userKey}
                        getLabel={userLabel}
                        matches={userMatches}
                        renderItem={(user) => (
                          <>
                            {user.name} <span className="text-muted-foreground">· {userTypeLabels[user.type]}</span>
                            <span className="block text-[13px] text-muted-foreground">{user.email}</span>
                          </>
                        )}
                        placeholder={usersQuery.isPending ? "Carregando…" : "Digite o nome ou e-mail"}
                        emptyText="Nenhum usuário encontrado."
                        disabled={usersQuery.isPending}
                        invalid={!!errors.userId}
                      />
                    )}
                  />
                </FormField>

                <FormField
                  id="loan-book"
                  label="Livro"
                  error={errors.bookId?.message ?? (booksError ? "Não foi possível carregar os livros." : undefined)}
                >
                  <Controller
                    control={control}
                    name="bookId"
                    render={({ field }) => (
                      <Autocomplete
                        id="loan-book"
                        items={availableBooks}
                        value={field.value}
                        onChange={field.onChange}
                        onBlur={field.onBlur}
                        getKey={bookKey}
                        getLabel={bookLabel}
                        matches={bookMatches}
                        renderItem={(book) => (
                          <>
                            {book.title}
                            <span className="block text-[13px] text-muted-foreground">
                              {book.author} · <span className="tabular-nums">{book.availableCopies}</span>{" "}
                              {book.availableCopies > 1 ? "disponíveis" : "disponível"}
                            </span>
                          </>
                        )}
                        placeholder={
                          booksPending
                            ? "Carregando…"
                            : availableBooks.length === 0
                              ? "Nenhum livro disponível"
                              : "Digite o título ou autor"
                        }
                        emptyText="Nenhum livro disponível encontrado."
                        disabled={booksPending || availableBooks.length === 0}
                        invalid={!!errors.bookId}
                      />
                    )}
                  />
                </FormField>
              </div>

              <div className="flex flex-col-reverse gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-[13px] text-muted-foreground">
                  {selectedUser && rules
                    ? `Prazo de ${plural(rules.loanDays, "dia")}: devolução até ${formatDate(addDaysIso(rules.loanDays))}.`
                    : "Escolha o usuário para ver o prazo."}
                </p>
                <Button type="submit" disabled={loanMutation.isPending}>
                  {loanMutation.isPending ? "Registrando…" : "Emprestar"}
                </Button>
              </div>
            </form>
          )}
        </section>

        <UserSummary
          user={selectedUser}
          activeLoans={activeLoans}
          loansPending={loansQuery.isPending}
          pending={pending}
          limitReached={limitReached}
        />
      </div>

      <section className="mt-10">
        <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-[15px] font-medium">
            {selectedUser ? `Empréstimos de ${selectedUser.name}` : "Empréstimos do usuário"}
          </h2>
          {selectedUser && (
            <Link
              to={`/usuarios/${selectedUser.id}`}
              className="text-[13px] text-primary transition-colors duration-150 hover:text-primary-hover hover:underline"
            >
              Ver ficha completa
            </Link>
          )}
        </div>

        <div className="rounded-md border bg-card">
          {selectedUserId === null ? (
            <EmptyState
              icon={UserRound}
              title="Selecione um usuário para ver os empréstimos."
              description="Não existe uma listagem geral: os empréstimos são consultados por usuário."
            />
          ) : loansQuery.isPending || booksPending ? (
            <TableSkeleton columns={6} rows={3} />
          ) : loansQuery.isError ? (
            <ErrorState error={loansQuery.error} onRetry={() => loansQuery.refetch()} />
          ) : loansQuery.data.length === 0 ? (
            <EmptyState icon={BookMarked} title="Nenhum empréstimo registrado." />
          ) : (
            <LoansTable loans={sortActiveFirst(loansQuery.data)} booksById={booksById} maxRenewals={rules?.maxRenewals} />
          )}
        </div>
      </section>
    </>
  )
}

/** Today + n days, as "YYYY-MM-DD" (local calendar date) */
function addDaysIso(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

/** Active loans on top, keeping the backend order (most recent first) inside each group */
function sortActiveFirst(loans: Loan[]): Loan[] {
  return [...loans].sort((a, b) => Number(b.status === "ACTIVE") - Number(a.status === "ACTIVE"))
}

type UserSummaryProps = {
  user: Member | null
  activeLoans: number
  loansPending: boolean
  pending: Pending | undefined
  limitReached: boolean
}

/** Shows, before submitting, whether the loan would be refused (limit, pending fines, overdue books) */
function UserSummary({ user, activeLoans, loansPending, pending, limitReached }: UserSummaryProps) {
  if (!user) {
    return (
      <aside className="rounded-md border border-dashed p-5 text-[13px] text-muted-foreground">
        O resumo do usuário aparece aqui: livros em uso, limite do tipo e pendências.
      </aside>
    )
  }

  const rules = userTypeRules[user.type]
  const blocked = pending && (pending.hasPendingFines || pending.hasOverdueLoans)

  return (
    <aside className="rounded-md border bg-card">
      <div className="flex items-center gap-3 border-b p-4">
        <Monogram name={user.name} />
        <div className="min-w-0">
          <p className="truncate font-medium">{user.name}</p>
          <p className="truncate text-[13px] text-muted-foreground">{user.email}</p>
        </div>
        <Badge variant="neutral" className="ml-auto">
          {userTypeLabels[user.type]}
        </Badge>
      </div>

      <div className="grid gap-4 p-4">
        <div>
          <div className="mb-1.5 flex items-baseline justify-between text-[13px]">
            <span className="text-muted-foreground">Livros em uso</span>
            {loansPending ? (
              <Skeleton className="h-3 w-10" />
            ) : (
              <span className="tabular-nums">
                {activeLoans} de {rules.maxBooks}
              </span>
            )}
          </div>
          <Meter
            value={loansPending ? 0 : activeLoans}
            max={rules.maxBooks}
            tone={limitReached ? "full" : "default"}
            label={`${activeLoans} de ${rules.maxBooks} livros em uso`}
          />
        </div>

        {pending === undefined ? (
          <Skeleton className="h-4 w-3/4" />
        ) : blocked || limitReached ? (
          <div className="flex gap-2 text-[13px] text-warning">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <div>
              <p className="font-medium">O empréstimo será recusado.</p>
              <ul className="mt-0.5 text-warning/90">
                {pending.hasPendingFines && <li>Há multa pendente.</li>}
                {pending.hasOverdueLoans && <li>Há livro atrasado.</li>}
                {limitReached && <li>Limite de {plural(rules.maxBooks, "livro")} atingido.</li>}
              </ul>
            </div>
          </div>
        ) : (
          <p className="flex items-center gap-2 text-[13px] text-success">
            <CircleCheck className="size-4" />
            Sem pendências. Pode emprestar.
          </p>
        )}
      </div>
    </aside>
  )
}
