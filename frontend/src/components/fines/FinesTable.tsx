import { useMutation, useQueryClient } from "@tanstack/react-query"

import { payFine } from "@/api/fines"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatCurrency } from "@/lib/format"
import { queryKeys } from "@/lib/queryKeys"
import { showError, showSuccess } from "@/lib/toast"
import type { Fine } from "@/types/fine"

type FinesTableProps = {
  fines: Fine[]
  /** Paying is a counter operation (librarian only) */
  canPay?: boolean
  bookTitleForLoan: (loanId: number) => string | undefined
}

export function FinesTable({ fines, bookTitleForLoan, canPay = false }: FinesTableProps) {
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
          {canPay && (
            <TableHead className="w-0">
              <span className="sr-only">Ações</span>
            </TableHead>
          )}
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
              {canPay && (
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
              )}
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
