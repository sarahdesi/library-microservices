import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { BookOpen, Pencil, SearchX, Trash2 } from "lucide-react"

import { getBooks, searchBooks } from "@/api/books"
import { BookFormDialog } from "@/components/books/BookFormDialog"
import { DeleteBookDialog } from "@/components/books/DeleteBookDialog"
import { EmptyState } from "@/components/EmptyState"
import { PageHeader } from "@/components/layout/PageHeader"
import { Meter } from "@/components/Meter"
import { ErrorState } from "@/components/QueryState"
import { SearchInput } from "@/components/SearchInput"
import { TableSkeleton } from "@/components/TableSkeleton"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useBooksById } from "@/hooks/useBooksById"
import { useCurrentUser } from "@/hooks/useAuth"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { plural } from "@/lib/format"
import { queryKeys } from "@/lib/queryKeys"
import type { Book, BookSearchField } from "@/types/book"

const searchFieldLabels: Record<BookSearchField, string> = {
  title: "Título",
  author: "Autor",
  category: "Categoria",
}

function CatalogSummary({ books }: { books: Book[] }) {
  const copies = books.reduce((sum, book) => sum + book.totalCopies, 0)
  const onLoan = books.reduce((sum, book) => sum + (book.totalCopies - book.availableCopies), 0)
  const unavailable = books.filter((book) => book.availableCopies === 0).length
  return (
    <p>
      {plural(books.length, "título")} · {plural(copies, "exemplar", "exemplares")} · {onLoan} emprestado
      {onLoan === 1 ? "" : "s"}
      {unavailable > 0 && <> · <span className="text-warning">{plural(unavailable, "indisponível", "indisponíveis")}</span></>}
    </p>
  )
}

export function BooksPage() {
  // Members see the catalog read-only: no create, edit or delete (the gateway refuses them anyway)
  const canManage = useCurrentUser().role === "LIBRARIAN"
  const [searchField, setSearchField] = useState<BookSearchField>("title")
  const [searchText, setSearchText] = useState("")
  const term = useDebouncedValue(searchText.trim())

  const [formOpen, setFormOpen] = useState(false)
  const [editingBook, setEditingBook] = useState<Book | null>(null)
  const [deletingBook, setDeletingBook] = useState<Book | null>(null)

  // Whole catalog for the summary line; the table shows the search result
  const catalog = useBooksById()
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
  const total = catalog.data?.length

  return (
    <>
      <PageHeader
        title={canManage ? "Livros" : "Catálogo"}
        description={
          canManage
            ? "Acervo da biblioteca e exemplares disponíveis."
            : "Consulte os livros e os exemplares disponíveis. Os empréstimos são feitos no balcão."
        }
        meta={catalog.data && catalog.data.length > 0 ? <CatalogSummary books={catalog.data} /> : undefined}
        action={canManage ? <Button onClick={openCreate}>Novo livro</Button> : undefined}
      />

      <div className="rounded-md border bg-card">
        {/* Toolbar inside the card */}
        <div className="flex flex-col gap-2 border-b p-3 sm:flex-row sm:items-center">
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
          <SearchInput
            className="sm:w-80"
            aria-label="Buscar livros"
            placeholder={`Buscar por ${searchFieldLabels[searchField].toLowerCase()}`}
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
          {term && !booksQuery.isPending && total !== undefined && (
            <p className="text-[13px] text-muted-foreground tabular-nums sm:ml-auto">
              {books.length} de {plural(total, "título")}
            </p>
          )}
        </div>

        {booksQuery.isPending ? (
          <TableSkeleton columns={6} />
        ) : booksQuery.isError ? (
          <ErrorState error={booksQuery.error} onRetry={() => booksQuery.refetch()} />
        ) : books.length === 0 ? (
          term ? (
            <EmptyState
              icon={SearchX}
              title={`Nenhum livro encontrado para “${term}”.`}
              description={`A busca considera só o campo ${searchFieldLabels[searchField].toLowerCase()}.`}
              action={
                <Button variant="outline" size="sm" onClick={() => setSearchText("")}>
                  Limpar busca
                </Button>
              }
            />
          ) : (
            <EmptyState
              icon={BookOpen}
              title="Nenhum livro cadastrado."
              action={
                canManage ? (
                  <Button variant="outline" size="sm" onClick={openCreate}>
                    Cadastrar livro
                  </Button>
                ) : undefined
              }
            />
          )
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Título</TableHead>
                <TableHead className="hidden md:table-cell">Categoria</TableHead>
                <TableHead className="hidden md:table-cell">ISBN</TableHead>
                <TableHead className="w-36 md:w-44">Disponibilidade</TableHead>
                {canManage && (
                  <TableHead className="w-0">
                    <span className="sr-only">Ações</span>
                  </TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {books.map((book) => {
                const none = book.availableCopies === 0
                return (
                  <TableRow key={book.id} className="group">
                    <TableCell className="whitespace-normal">
                      <p className="font-medium">{book.title}</p>
                      <p className="text-[13px] text-muted-foreground">{book.author}</p>
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">{book.category}</TableCell>
                    <TableCell className="hidden text-muted-foreground tabular-nums md:table-cell">{book.isbn}</TableCell>
                    <TableCell>
                      <div className="flex items-center justify-between gap-2">
                        <span className="tabular-nums">
                          {book.availableCopies} de {book.totalCopies}
                        </span>
                        {none && <Badge variant="warning">Indisponível</Badge>}
                      </div>
                      <Meter
                        className="mt-1.5"
                        value={book.availableCopies}
                        max={book.totalCopies}
                        tone={none ? "full" : "default"}
                        label={`${book.availableCopies} de ${book.totalCopies} exemplares disponíveis`}
                      />
                    </TableCell>
                    {canManage && (
                      <TableCell>
                        <div className="flex justify-end gap-1 opacity-70 transition-opacity duration-150 group-hover:opacity-100 focus-within:opacity-100">
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
                    )}
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        )}
      </div>

      {canManage && (
        <>
          <BookFormDialog open={formOpen} onOpenChange={setFormOpen} book={editingBook} />
          <DeleteBookDialog book={deletingBook} onOpenChange={(open) => !open && setDeletingBook(null)} />
        </>
      )}
    </>
  )
}
