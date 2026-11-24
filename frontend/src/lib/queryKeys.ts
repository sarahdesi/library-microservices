import type { BookSearchField } from "@/types/book"

export const queryKeys = {
  books: ["books"] as const,
  bookSearch: (field: BookSearchField, term: string) => ["books", "search", field, term] as const,
  users: ["users"] as const,
  user: (id: number) => ["users", id] as const,
  userLoans: (userId: number) => ["loans", "user", userId] as const,
  userPending: (userId: number) => ["loans", "pending", userId] as const,
  userFines: (userId: number) => ["fines", "user", userId] as const,
  // logged-in member (prefixes shared with the lists above, so invalidating "loans"/"fines" refreshes them too)
  me: ["users", "me"] as const,
  myLoans: ["loans", "me"] as const,
  myPending: ["loans", "me", "pending"] as const,
  myFines: ["fines", "me"] as const,
}
