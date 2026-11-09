import { useMutation, useQueryClient } from "@tanstack/react-query"

import { getUserFines } from "@/api/fines"
import { renewLoan, returnLoan } from "@/api/loans"
import { formatDate } from "@/lib/format"
import { queryKeys } from "@/lib/queryKeys"
import { showError, showSuccess } from "@/lib/toast"
import type { Loan } from "@/types/loan"

/** Return and renew, refreshing every list affected (loans, pending, fines, book availability) */
export function useLoanActions() {
  const queryClient = useQueryClient()

  const refresh = (userId: number) => {
    queryClient.invalidateQueries({ queryKey: ["loans"] })
    queryClient.invalidateQueries({ queryKey: queryKeys.userFines(userId) })
    queryClient.invalidateQueries({ queryKey: queryKeys.books })
  }

  const returnMutation = useMutation({
    mutationFn: async (loan: Loan) => {
      const returned = await returnLoan(loan.id)
      // The loan response does not say whether a fine was generated: look it up
      let fineGenerated: boolean
      try {
        const fines = await getUserFines(returned.userId)
        queryClient.setQueryData(queryKeys.userFines(returned.userId), fines)
        fineGenerated = fines.some((fine) => fine.loanId === returned.id)
      } catch {
        fineGenerated = returned.returnDate !== null && returned.returnDate > returned.dueDate
      }
      return { returned, fineGenerated }
    },
    onSuccess: ({ returned, fineGenerated }) => {
      refresh(returned.userId)
      showSuccess(fineGenerated ? "Livro devolvido com atraso. Multa gerada." : "Livro devolvido.")
    },
    onError: showError,
  })

  const renewMutation = useMutation({
    mutationFn: (loan: Loan) => renewLoan(loan.id),
    onSuccess: (renewed) => {
      refresh(renewed.userId)
      showSuccess(`Empréstimo renovado. Novo prazo: ${formatDate(renewed.dueDate)}.`)
    },
    onError: showError,
  })

  /** Id of the loan currently being returned or renewed, to disable its buttons */
  const busyLoanId = returnMutation.isPending
    ? returnMutation.variables?.id
    : renewMutation.isPending
      ? renewMutation.variables?.id
      : undefined

  return {
    returnLoan: (loan: Loan) => returnMutation.mutate(loan),
    renewLoan: (loan: Loan) => renewMutation.mutate(loan),
    busyLoanId,
  }
}
