import type { User } from "./user"

export type LoginRequest = { email: string; password: string }

/** The token expires in 1 hour; there is no refresh token */
export type LoginResponse = { token: string; user: User }

export type RegisterExternalRequest = { name: string; email: string; cpf: string; password: string }

/** Student: registrationNumber = matrícula. Teacher: registrationNumber = MASP. No name: it comes from the university. */
export type RegisterUniversityRequest = { registrationNumber: string; cpf: string; email: string; password: string }

export type Session = { token: string; user: User }
