import { useMutation, useQueryClient } from "@tanstack/react-query"

import { deleteBook } from "@/api/books"
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { queryKeys } from "@/lib/queryKeys"
import { showError, showSuccess } from "@/lib/toast"
import type { Book } from "@/types/book"

type DeleteBookDialogProps = {
  book: Book | null
  onOpenChange: (open: boolean) => void
}

export function DeleteBookDialog({ book, onOpenChange }: DeleteBookDialogProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => deleteBook(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books })
      showSuccess("Livro removido.")
      onOpenChange(false)
    },
    onError: showError,
  })

  return (
    <AlertDialog open={book !== null} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Remover livro</AlertDialogTitle>
          <AlertDialogDescription>
            “{book?.title}” será removido do acervo. Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          {/* Plain button (not AlertDialogAction) so the dialog stays open if the backend refuses */}
          <Button variant="destructive" disabled={mutation.isPending} onClick={() => book && mutation.mutate(book.id)}>
            {mutation.isPending ? "Removendo…" : "Remover"}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
