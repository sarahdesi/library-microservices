import { DueDate } from "@/components/DueDate"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useLoanActions } from "@/hooks/useLoanActions"
import { formatDate } from "@/lib/format"
import { loanStatusLabels, OVERDUE_LABEL } from "@/lib/labels"
import type { Book } from "@/types/book"
import type { Loan } from "@/types/loan"

type LoansTableProps = {
  loans: Loan[]
  booksById: Map<string, Book>
  /** Renewal limit for the user's type, shown as "1 de 5" */
  maxRenewals?: number
  /** "counter": return and renew (librarian). "member": renew only, the return happens at the counter. */
  mode?: "counter" | "member"
}

export function LoansTable({ loans, booksById, maxRenewals, mode = "counter" }: LoansTableProps) {
  const { returnLoan, renewLoan, busyLoanId } = useLoanActions()

  return (
    <Table>
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead>Livro</TableHead>
          <TableHead>Empréstimo</TableHead>
          <TableHead>Prazo</TableHead>
          <TableHead>Renovações</TableHead>
          <TableHead>Situação</TableHead>
          <TableHead className="w-0">
            <span className="sr-only">Ações</span>
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {loans.map((loan) => {
          const book = booksById.get(loan.bookId)
          const isActive = loan.status === "ACTIVE"
          const isBusy = busyLoanId === loan.id

          return (
            <TableRow key={loan.id}>
              <TableCell className="min-w-44 whitespace-normal">
                {book ? (
                  <>
                    <p className="font-medium">{book.title}</p>
                    <p className="text-[13px] text-muted-foreground">{book.author}</p>
                  </>
                ) : (
                  <span className="text-muted-foreground">Livro não encontrado</span>
                )}
              </TableCell>
              <TableCell>
                {/* Loan date with the return date below, instead of a separate column */}
                <div className="leading-tight">
                  <span className="tabular-nums">{formatDate(loan.loanDate)}</span>
                  {loan.returnDate && (
                    <span className="block text-xs text-muted-foreground tabular-nums">
                      devolvido em {formatDate(loan.returnDate)}
                    </span>
                  )}
                </div>
              </TableCell>
              <TableCell>
                <DueDate date={loan.dueDate} active={isActive} />
              </TableCell>
              <TableCell className="tabular-nums">
                {maxRenewals !== undefined ? `${loan.renewalCount} de ${maxRenewals}` : loan.renewalCount}
              </TableCell>
              <TableCell>
                {/* "Atrasado" already implies an active loan */}
                {loan.overdue ? (
                  <Badge variant="danger">{OVERDUE_LABEL}</Badge>
                ) : (
                  <Badge variant={isActive ? "success" : "neutral"}>{loanStatusLabels[loan.status]}</Badge>
                )}
              </TableCell>
              <TableCell>
                {isActive && (
                  <div className="flex justify-end gap-2">
                    <Button variant="outline" size="sm" disabled={isBusy} onClick={() => renewLoan(loan)}>
                      Renovar
                    </Button>
                    {mode === "counter" && (
                      <Button variant="outline" size="sm" disabled={isBusy} onClick={() => returnLoan(loan)}>
                        Devolver
                      </Button>
                    )}
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
