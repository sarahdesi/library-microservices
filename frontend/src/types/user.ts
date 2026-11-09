export type UserType = "STUDENT" | "TEACHER" | "EXTERNAL"

export type User = {
  id: number
  name: string
  email: string
  type: UserType
}

export type UserRequest = {
  name: string
  email: string
  type: UserType
}
