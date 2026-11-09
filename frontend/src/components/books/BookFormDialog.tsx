import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

import { createBook, updateBook } from "@/api/books"
import { FormField } from "@/components/FormField"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { hasValidIsbnCheckDigit, ISBN_PATTERN, normalizeIsbnInput, stripIsbnSeparators } from "@/lib/isbn"
import { queryKeys } from "@/lib/queryKeys"
import { showError, showSuccess } from "@/lib/toast"
import type { Book, BookRequest } from "@/types/book"

const required = (message: string) => z.string().trim().min(1, message)

const bookSchema = z.object({
  title: required("Informe o título."),
  author: required("Informe o autor."),
  category: required("Informe a categoria."),
  isbn: z
    .string()
    .transform(stripIsbnSeparators)
    .pipe(
      required("Informe o ISBN.")
        .regex(ISBN_PATTERN, "Use 10 ou 13 dígitos.")
        .refine(hasValidIsbnCheckDigit, "Dígito verificador do ISBN inválido."),
    ),
  totalCopies: z
    .number({ error: "Informe o total de exemplares." })
    .int("Use um número inteiro.")
    .min(1, "O total deve ser de pelo menos 1 exemplar."),
})

type BookFormValues = z.infer<typeof bookSchema>

const emptyValues: BookFormValues = { title: "", author: "", category: "", isbn: "", totalCopies: 1 }

type BookFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When present, the form edits this book */
  book?: Book | null
}

export function BookFormDialog({ open, onOpenChange, book }: BookFormDialogProps) {
  const queryClient = useQueryClient()
  const isEditing = Boolean(book)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<BookFormValues>({ resolver: zodResolver(bookSchema), defaultValues: emptyValues })

  useEffect(() => {
    if (open) {
      reset(
        book
          ? {
              title: book.title,
              author: book.author,
              category: book.category,
              isbn: book.isbn,
              totalCopies: book.totalCopies,
            }
          : emptyValues,
      )
    }
  }, [open, book, reset])

  const mutation = useMutation({
    mutationFn: (data: BookRequest) => (book ? updateBook(book.id, data) : createBook(data)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books })
      showSuccess(isEditing ? "Livro atualizado." : "Livro cadastrado.")
      onOpenChange(false)
    },
    onError: showError,
  })

  const isbnField = register("isbn")

  const onLoan = book ? book.totalCopies - book.availableCopies : 0

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar livro" : "Novo livro"}</DialogTitle>
          <DialogDescription>
            {isEditing ? "Altere os dados do livro." : "Preencha os dados do livro para incluí-lo no acervo."}
          </DialogDescription>
        </DialogHeader>

        <form id="book-form" className="grid gap-4" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <FormField id="book-title" label="Título" error={errors.title?.message}>
            <Input id="book-title" aria-invalid={!!errors.title} {...register("title")} />
          </FormField>
          <FormField id="book-author" label="Autor" error={errors.author?.message}>
            <Input id="book-author" aria-invalid={!!errors.author} {...register("author")} />
          </FormField>
          <FormField id="book-category" label="Categoria" error={errors.category?.message}>
            <Input id="book-category" aria-invalid={!!errors.category} {...register("category")} />
          </FormField>
          <div className="grid gap-4 sm:grid-cols-[1fr_140px]">
            <FormField id="book-isbn" label="ISBN" error={errors.isbn?.message} hint="Até 13 caracteres, sem hífens.">
              <Input
                id="book-isbn"
                inputMode="numeric"
                className="tabular-nums"
                aria-invalid={!!errors.isbn}
                {...isbnField}
                onChange={(event) => {
                  event.target.value = normalizeIsbnInput(event.target.value)
                  return isbnField.onChange(event)
                }}
              />
            </FormField>
            <FormField
              id="book-total"
              label="Exemplares"
              error={errors.totalCopies?.message}
              hint={onLoan > 0 ? `${onLoan} emprestado${onLoan > 1 ? "s" : ""} agora.` : undefined}
            >
              <Input
                id="book-total"
                type="number"
                min={1}
                step={1}
                className="tabular-nums"
                aria-invalid={!!errors.totalCopies}
                {...register("totalCopies", { valueAsNumber: true })}
              />
            </FormField>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="book-form" disabled={mutation.isPending}>
            {mutation.isPending ? "Salvando…" : "Salvar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
