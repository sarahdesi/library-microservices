import type { Role } from "@/types/user"

export const NO_PERMISSION_PAGE_MESSAGE = "Você não tem permissão para acessar esta página."
export const SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre novamente."

/** Start page of each profile */
export function homePath(role: Role): string {
  return role === "LIBRARIAN" ? "/livros" : "/minha-conta"
}
