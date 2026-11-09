export type LoanStatus = "ACTIVE" | "RETURNED"

export type Loan = {
  id: number
  userId: number
  bookId: string
  /** ISO date without time, e.g. "2026-11-09" */
  loanDate: string
  dueDate: string
  /** null while not returned */
  returnDate: string | null
  renewalCount: number
  status: LoanStatus
  /** computed by the backend: active and past due date */
  overdue: boolean
}

export type LoanRequest = {
  userId: number
  bookId: string
}

export type Pending = {
  hasPendingFines: boolean
  hasOverdueLoans: boolean
}
