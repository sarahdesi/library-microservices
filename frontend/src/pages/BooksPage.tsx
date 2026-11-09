import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Pencil, Search, Trash2 } from "lucide-react"

import { getBooks, searchBooks } from "@/api/books"
import { BookFormDialog } from "@/components/books/BookFormDialog"
import { DeleteBookDialog } from "@/components/books/DeleteBookDialog"
import { PageHeader } from "@/components/layout/PageHeader"
import { ErrorState, LoadingState } from "@/components/QueryState"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { queryKeys } from "@/lib/queryKeys"
import type { Book, BookSearchField } from "@/types/book"

const searchFieldLabels: Record<BookSearchField, string> = {
  title: "Título",
  author: "Autor",
  category: "Categoria",
}

export function BooksPage() {
  const [searchField, setSearchField] = useState<BookSearchField>("title")
  const [searchText, setSearchText] = useState("")
  const term = useDebouncedValue(searchText.trim())

  const [formOpen, setFormOpen] = useState(false)
  const [editingBook, setEditingBook] = useState<Book | null>(null)
  const [deletingBook, setDeletingBook] = useState<Book | null>(null)

  const booksQuery = useQuery({
    queryKey: term ? queryKeys.bookSearch(searchField, term) : queryKeys.books,
    queryFn: () => (term ? searchBooks(searchField, term) : getBooks()),
    placeholderData: (previous) => previous,
  })

  const openCreate = () => {
    setEditingBook(null)
    setFormOpen(true)
  }

  const openEdit = (book: Book) => {
    setEditingBook(book)
    setFormOpen(true)
  }

  const books = booksQuery.data ?? []

  return (
    <>
      <PageHeader
        title="Livros"
        description="Acervo da biblioteca e exemplares disponíveis."
        action={<Button onClick={openCreate}>Novo livro</Button>}
      />

      <div className="mb-4 flex flex-col gap-2 sm:flex-row">
        <Select value={searchField} onValueChange={(value) => setSearchField(value as BookSearchField)}>
          <SelectTrigger className="sm:w-36" aria-label="Buscar por">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {Object.entries(searchFieldLabels).map(([value, label]) => (
              <SelectItem key={value} value={value}>
                {label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="relative sm:w-80">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            className="pl-8"
            placeholder={`Buscar por ${searchFieldLabels[searchField].toLowerCase()}`}
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
        </div>
      </div>

      <div className="rounded-md border bg-card">
        {booksQuery.isPending ? (
          <LoadingState />
        ) : booksQuery.isError ? (
          <ErrorState error={booksQuery.error} onRetry={() => booksQuery.refetch()} />
        ) : books.length === 0 ? (
          term ? (
            <p className="py-10 text-center text-muted-foreground">Nenhum livro encontrado para “{term}”.</p>
          ) : (
            <div className="flex items-center justify-center gap-3 py-10">
              <p className="text-muted-foreground">Nenhum livro cadastrado.</p>
              <Button variant="outline" size="sm" onClick={openCreate}>
                Cadastrar livro
              </Button>
            </div>
          )
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Título</TableHead>
                <TableHead>Autor</TableHead>
                <TableHead>Categoria</TableHead>
                <TableHead>ISBN</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead>Disponíveis</TableHead>
                <TableHead className="w-0">
                  <span className="sr-only">Ações</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {books.map((book) => (
                <TableRow key={book.id}>
                  <TableCell className="font-medium whitespace-normal">{book.title}</TableCell>
                  <TableCell className="whitespace-normal">{book.author}</TableCell>
                  <TableCell>{book.category}</TableCell>
                  <TableCell className="tabular-nums text-muted-foreground">{book.isbn}</TableCell>
                  <TableCell className="text-right tabular-nums">{book.totalCopies}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <span className="w-6 text-right tabular-nums">{book.availableCopies}</span>
                      {book.availableCopies === 0 && <Badge variant="warning">Indisponível</Badge>}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon-sm" title="Editar" onClick={() => openEdit(book)}>
                        <Pencil />
                        <span className="sr-only">Editar {book.title}</span>
                      </Button>
                      <Button variant="ghost" size="icon-sm" title="Remover" onClick={() => setDeletingBook(book)}>
                        <Trash2 />
                        <span className="sr-only">Remover {book.title}</span>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <BookFormDialog open={formOpen} onOpenChange={setFormOpen} book={editingBook} />
      <DeleteBookDialog book={deletingBook} onOpenChange={(open) => !open && setDeletingBook(null)} />
    </>
  )
}
