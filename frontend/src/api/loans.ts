import type { Loan, LoanRequest, Pending } from "@/types/loan"
import { request } from "./client"

export const createLoan = (data: LoanRequest) => request<Loan>("/loans", { method: "POST", body: data })

export const returnLoan = (id: number) => request<Loan>(`/loans/${id}/return`, { method: "POST" })

export const renewLoan = (id: number) => request<Loan>(`/loans/${id}/renew`, { method: "POST" })

export const getLoan = (id: number) => request<Loan>(`/loans/${id}`)

/** Most recent first. There is no endpoint listing all loans. */
export const getUserLoans = (userId: number) => request<Loan[]>(`/loans/user/${userId}`)

export const getUserPending = (userId: number) => request<Pending>(`/loans/user/${userId}/pending`)
