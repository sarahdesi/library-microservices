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
}

export function LoansTable({ loans, booksById, maxRenewals }: LoansTableProps) {
  const { returnLoan, renewLoan, busyLoanId } = useLoanActions()

  return (
    <Table>
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead>Livro</TableHead>
          <TableHead>Empréstimo</TableHead>
          <TableHead>Prazo</TableHead>
          <TableHead>Devolução</TableHead>
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
              <TableCell className="font-medium whitespace-normal">
                {book ? book.title : <span className="font-normal text-muted-foreground">Livro não encontrado</span>}
              </TableCell>
              <TableCell className="tabular-nums">{formatDate(loan.loanDate)}</TableCell>
              <TableCell className={loan.overdue ? "tabular-nums text-danger" : "tabular-nums"}>
                {formatDate(loan.dueDate)}
              </TableCell>
              <TableCell className="tabular-nums">{formatDate(loan.returnDate)}</TableCell>
              <TableCell className="tabular-nums">
                {maxRenewals !== undefined ? `${loan.renewalCount} de ${maxRenewals}` : loan.renewalCount}
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1.5">
                  <Badge variant={isActive ? "success" : "neutral"}>{loanStatusLabels[loan.status]}</Badge>
                  {loan.overdue && <Badge variant="danger">{OVERDUE_LABEL}</Badge>}
                </div>
              </TableCell>
              <TableCell>
                {isActive && (
                  <div className="flex justify-end gap-2">
                    <Button variant="outline" size="sm" disabled={isBusy} onClick={() => renewLoan(loan)}>
                      Renovar
                    </Button>
                    <Button variant="outline" size="sm" disabled={isBusy} onClick={() => returnLoan(loan)}>
                      Devolver
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
