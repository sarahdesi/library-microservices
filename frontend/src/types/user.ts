export type UserType = "STUDENT" | "TEACHER" | "EXTERNAL"

export type Role = "MEMBER" | "LIBRARIAN"

/** type is null only for LIBRARIAN. Responses never include password or CPF. */
export type User = {
  id: number
  name: string
  email: string
  type: UserType | null
  role: Role
}

/** A library member: the only kind of user that borrows books */
export type Member = User & { type: UserType; role: "MEMBER" }

export function isMember(user: User): user is Member {
  return user.role === "MEMBER" && user.type !== null
}

/** Counter registration: cpf is required (11 digits) */
export type UserRequest = {
  name: string
  email: string
  type: UserType
  cpf: string
}

/** Counter edit: the API never returns the CPF, so it is optional (absent keeps the current one) */
export type UserUpdateRequest = {
  name: string
  email: string
  type: UserType
  cpf?: string
}
