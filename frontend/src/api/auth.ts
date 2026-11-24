import type { LoginRequest, LoginResponse, RegisterExternalRequest, RegisterUniversityRequest } from "@/types/auth"
import type { User } from "@/types/user"
import { request } from "./client"

export const login = (data: LoginRequest) =>
  request<LoginResponse>("/auth/login", { method: "POST", body: data, auth: false })

export const registerExternal = (data: RegisterExternalRequest) =>
  request<User>("/auth/register/external", { method: "POST", body: data, auth: false })

export const registerStudent = (data: RegisterUniversityRequest) =>
  request<User>("/auth/register/student", { method: "POST", body: data, auth: false })

export const registerTeacher = (data: RegisterUniversityRequest) =>
  request<User>("/auth/register/teacher", { method: "POST", body: data, auth: false })
