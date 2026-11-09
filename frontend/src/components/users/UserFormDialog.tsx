import { useEffect } from "react"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"

import { createUser, updateUser } from "@/api/users"
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { userTypeLabels } from "@/lib/labels"
import { queryKeys } from "@/lib/queryKeys"
import { showError, showSuccess } from "@/lib/toast"
import type { User, UserRequest, UserType } from "@/types/user"

const userTypes = Object.keys(userTypeLabels) as UserType[]

const userSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome."),
  email: z.string().trim().min(1, "Informe o e-mail.").pipe(z.email("Informe um e-mail válido.")),
  type: z.enum(userTypes as [UserType, ...UserType[]], { error: "Selecione o tipo." }),
})

type UserFormValues = z.infer<typeof userSchema>

type UserFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When present, the form edits this user */
  user?: User | null
}

export function UserFormDialog({ open, onOpenChange, user }: UserFormDialogProps) {
  const queryClient = useQueryClient()
  const isEditing = Boolean(user)

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UserFormValues>({ resolver: zodResolver(userSchema) })

  useEffect(() => {
    if (open) {
      reset(user ? { name: user.name, email: user.email, type: user.type } : { name: "", email: "", type: undefined })
    }
  }, [open, user, reset])

  const mutation = useMutation({
    mutationFn: (data: UserRequest) => (user ? updateUser(user.id, data) : createUser(data)),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users })
      queryClient.setQueryData(queryKeys.user(saved.id), saved)
      showSuccess(isEditing ? "Usuário atualizado." : "Usuário cadastrado.")
      onOpenChange(false)
    },
    onError: showError,
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar usuário" : "Novo usuário"}</DialogTitle>
          <DialogDescription>
            {isEditing ? "Altere os dados do usuário." : "Preencha os dados para cadastrar o usuário."}
          </DialogDescription>
        </DialogHeader>

        <form id="user-form" className="grid gap-4" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <FormField id="user-name" label="Nome" error={errors.name?.message}>
            <Input id="user-name" aria-invalid={!!errors.name} {...register("name")} />
          </FormField>
          <FormField id="user-email" label="E-mail" error={errors.email?.message}>
            <Input id="user-email" type="email" aria-invalid={!!errors.email} {...register("email")} />
          </FormField>
          <FormField id="user-type" label="Tipo" error={errors.type?.message}>
            <Controller
              control={control}
              name="type"
              render={({ field }) => (
                <Select value={field.value ?? ""} onValueChange={field.onChange}>
                  <SelectTrigger id="user-type" className="w-full" aria-invalid={!!errors.type} onBlur={field.onBlur}>
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                  <SelectContent>
                    {userTypes.map((type) => (
                      <SelectItem key={type} value={type}>
                        {userTypeLabels[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </FormField>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" form="user-form" disabled={mutation.isPending}>
            {mutation.isPending ? "Salvando…" : "Salvar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
