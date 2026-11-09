import type { Book, BookRequest, BookSearchField } from "@/types/book"
import { request } from "./client"

export const getBooks = () => request<Book[]>("/books")

export const getBook = (id: string) => request<Book>(`/books/${encodeURIComponent(id)}`)

/** The backend accepts a single criterion per search */
export const searchBooks = (field: BookSearchField, term: string) =>
  request<Book[]>(`/books/search?${field}=${encodeURIComponent(term)}`)

export const createBook = (data: BookRequest) => request<Book>("/books", { method: "POST", body: data })

export const updateBook = (id: string, data: BookRequest) =>
  request<Book>(`/books/${encodeURIComponent(id)}`, { method: "PUT", body: data })

export const deleteBook = (id: string) => request<void>(`/books/${encodeURIComponent(id)}`, { method: "DELETE" })

// PATCH /books/{id}/availability is internal (used by loan-service only) and intentionally not exposed.
