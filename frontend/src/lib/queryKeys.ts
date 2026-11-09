import type { BookSearchField } from "@/types/book"

export const queryKeys = {
  books: ["books"] as const,
  bookSearch: (field: BookSearchField, term: string) => ["books", "search", field, term] as const,
  users: ["users"] as const,
  user: (id: number) => ["users", id] as const,
  userLoans: (userId: number) => ["loans", "user", userId] as const,
  userPending: (userId: number) => ["loans", "pending", userId] as const,
  userFines: (userId: number) => ["fines", "user", userId] as const,
}
