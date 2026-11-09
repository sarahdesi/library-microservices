import type { User, UserRequest } from "@/types/user"
import { request } from "./client"

export const getUsers = () => request<User[]>("/users")

export const getUser = (id: number) => request<User>(`/users/${id}`)

export const createUser = (data: UserRequest) => request<User>("/users", { method: "POST", body: data })

export const updateUser = (id: number, data: UserRequest) =>
  request<User>(`/users/${id}`, { method: "PUT", body: data })
