import type { LoanStatus } from "@/types/loan"
import type { UserType } from "@/types/user"

export const userTypeLabels: Record<UserType, string> = {
  STUDENT: "Aluno",
  TEACHER: "Professor",
  EXTERNAL: "Externo",
}

export const loanStatusLabels: Record<LoanStatus, string> = {
  ACTIVE: "Ativo",
  RETURNED: "Devolvido",
}

export const OVERDUE_LABEL = "Atrasado"
