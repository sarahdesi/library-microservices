import type { Fine } from "@/types/fine"
import { request } from "./client"

export const getUserFines = (userId: number) => request<Fine[]>(`/fines/user/${userId}`)

export const payFine = (id: number) => request<Fine>(`/fines/${id}/pay`, { method: "POST" })
