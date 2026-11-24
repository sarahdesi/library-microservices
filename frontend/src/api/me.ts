import type { Fine } from "@/types/fine"
import type { Loan, Pending } from "@/types/loan"
import type { User } from "@/types/user"
import { request } from "./client"

/** Routes of the logged-in user: the backend takes the user from the token, the id is never sent */
export const getMe = () => request<User>("/users/me")

export const getMyLoans = () => request<Loan[]>("/loans/me")

export const getMyPending = () => request<Pending>("/loans/me/pending")

export const getMyFines = () => request<Fine[]>("/fines/me")
