import { useMemo } from "react"
import { Link } from "react-router"
import { Controller, useForm, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

import { createLoan, getUserLoans, getUserPending } from "@/api/loans"
import { getUsers } from "@/api/users"
import { Autocomplete } from "@/components/Autocomplete"
import { FormField } from "@/components/FormField"
import { PageHeader } from "@/components/layout/PageHeader"
import { LoansTable } from "@/components/loans/LoansTable"
import { Notice } from "@/components/Notice"
import { ErrorState, LoadingState } from "@/components/QueryState"
import { Button } from "@/components/ui/button"
import { useBooksById } from "@/hooks/useBooksById"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { userTypeRules } from "@/lib/rules"
import { matchesSearch } from "@/lib/search"
import { showError, showSuccess } from "@/lib/toast"
import type { Book } from "@/types/book"
import type { LoanRequest } from "@/types/loan"
import type { User } from "@/types/user"

const userKey = (user: User) => String(user.id)
const userLabel = (user: User) => user.name
const userMatches = (user: User, query: string) => matchesSearch(query, user.name, user.email)

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

  const usersQuery = useQuery({ queryKey: queryKeys.users, queryFn: getUsers })
  const { booksById, data: books, isPending: booksPending, isError: booksError } = useBooksById()

  const {
    control,
    handleSubmit,
    resetField,
    formState: { errors },
  } = useForm<LoanFormValues>({ resolver: zodResolver(loanSchema), defaultValues: { userId: "", bookId: "" } })

  const selectedUserId = Number(useWatch({ control, name: "userId" })) || null
  const selectedUser = usersQuery.data?.find((user) => user.id === selectedUserId) ?? null

  const availableBooks = useMemo(
    () =>
      (books ?? [])
        .filter((book) => book.availableCopies > 0)
        .sort((a, b) => a.title.localeCompare(b.title, "pt-BR")),
    [books],
  )

  const sortedUsers = useMemo(
    () => [...(usersQuery.data ?? [])].sort((a, b) => a.name.localeCompare(b.name, "pt-BR")),
    [usersQuery.data],
  )

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
  const blocked = pending && (pending.hasPendingFines || pending.hasOverdueLoans)
  const rules = selectedUser ? userTypeRules[selectedUser.type] : null
  const activeLoans = (loansQuery.data ?? []).filter((loan) => loan.status === "ACTIVE").length

  return (
    <>
      <PageHeader title="Empréstimos" description="Registre empréstimos e consulte os empréstimos de cada usuário." />

      <section className="rounded-md border bg-card p-5">
        <h2 className="mb-4 text-[15px] font-medium">Novo empréstimo</h2>

        {usersQuery.isError ? (
          <ErrorState error={usersQuery.error} onRetry={() => usersQuery.refetch()} />
        ) : (
          <form
            className="grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-start"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            <FormField
              id="loan-user"
              label="Usuário"
              error={errors.userId?.message}
              hint={
                selectedUser && rules
                  ? `${userTypeLabels[selectedUser.type]}: até ${rules.maxBooks} livro${rules.maxBooks > 1 ? "s" : ""}, prazo de ${rules.loanDays} dias.`
                  : undefined
              }
            >
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

            <Button type="submit" className="md:mt-[22px]" disabled={loanMutation.isPending}>
              {loanMutation.isPending ? "Registrando…" : "Emprestar"}
            </Button>
          </form>
        )}

        {blocked && (
          <div className="mt-4">
            <Notice variant="warning" title="O usuário tem multa pendente ou livro atrasado.">
              Novos empréstimos serão recusados até a situação ser regularizada.
            </Notice>
          </div>
        )}
      </section>

      <section className="mt-8">
        <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-[15px] font-medium">
            {selectedUser ? `Empréstimos de ${selectedUser.name}` : "Empréstimos do usuário"}
          </h2>
          {selectedUser && rules && (
            <p className="text-[13px] text-muted-foreground">
              <span className="tabular-nums">
                {activeLoans} de {rules.maxBooks}
              </span>{" "}
              em uso ·{" "}
              <Link
                to={`/usuarios/${selectedUser.id}`}
                className="text-primary transition-colors duration-150 hover:text-primary-hover hover:underline"
              >
                Ver detalhes
              </Link>
            </p>
          )}
        </div>

        <div className="rounded-md border bg-card">
          {selectedUserId === null ? (
            <p className="py-10 text-center text-muted-foreground">Selecione um usuário para ver os empréstimos.</p>
          ) : loansQuery.isPending || booksPending ? (
            <LoadingState />
          ) : loansQuery.isError ? (
            <ErrorState error={loansQuery.error} onRetry={() => loansQuery.refetch()} />
          ) : loansQuery.data.length === 0 ? (
            <p className="py-10 text-center text-muted-foreground">Nenhum empréstimo registrado.</p>
          ) : (
            <LoansTable loans={loansQuery.data} booksById={booksById} maxRenewals={rules?.maxRenewals} />
          )}
        </div>
      </section>
    </>
  )
}
