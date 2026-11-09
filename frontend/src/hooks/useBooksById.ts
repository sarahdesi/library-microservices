import { useMemo } from "react"
import { useQuery } from "@tanstack/react-query"

import { getBooks } from "@/api/books"
import { queryKeys } from "@/lib/queryKeys"
import type { Book } from "@/types/book"

/** Loans only carry the book id: this loads the catalog to show titles */
export function useBooksById() {
  const query = useQuery({ queryKey: queryKeys.books, queryFn: getBooks })
  const booksById = useMemo(() => new Map<string, Book>((query.data ?? []).map((book) => [book.id, book])), [query.data])
  return { ...query, booksById }
}
