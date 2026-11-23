import type { LoanStatus } from "@/types/loan"
import type { Role, UserType } from "@/types/user"

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

export const roleLabels: Record<Role, string> = {
  MEMBER: "Membro",
  LIBRARIAN: "Bibliotecária",
}
