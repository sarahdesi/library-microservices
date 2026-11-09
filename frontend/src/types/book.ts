export type Book = {
  /** MongoDB id (string, not number) */
  id: string
  title: string
  author: string
  category: string
  isbn: string
  totalCopies: number
  availableCopies: number
}

/** availableCopies is controlled by the backend and never sent */
export type BookRequest = {
  title: string
  author: string
  category: string
  isbn: string
  totalCopies: number
}

export type BookSearchField = "title" | "author" | "category"
