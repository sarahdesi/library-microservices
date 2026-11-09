import type { UserType } from "@/types/user"

/** Business rules shown to the librarian (validation happens in the backend) */
export type UserTypeRules = { maxBooks: number; loanDays: number; maxRenewals: number }

export const userTypeRules: Record<UserType, UserTypeRules> = {
  STUDENT: { maxBooks: 3, loanDays: 7, maxRenewals: 5 },
  TEACHER: { maxBooks: 5, loanDays: 15, maxRenewals: 5 },
  EXTERNAL: { maxBooks: 1, loanDays: 5, maxRenewals: 1 },
}

export const FINE_PER_DAY = 2
