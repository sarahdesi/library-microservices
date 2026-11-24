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
import type { Member, UserType } from "@/types/user"

const userTypes = Object.keys(userTypeLabels) as UserType[]

const CPF_PATTERN = /^\d{11}$/

const baseSchema = {
  name: z.string().trim().min(1, "Informe o nome."),
  email: z.string().trim().min(1, "Informe o e-mail.").pipe(z.email("Informe um e-mail válido.")),
  type: z.enum(userTypes as [UserType, ...UserType[]], { error: "Selecione o tipo." }),
}

// New member: CPF required. Edit: the API never returns the CPF, so empty keeps the current one.
const createSchema = z.object({
  ...baseSchema,
  cpf: z.string().regex(CPF_PATTERN, "O CPF deve ter 11 dígitos."),
})
const editSchema = z.object({
  ...baseSchema,
  cpf: z.string().refine((value) => value === "" || CPF_PATTERN.test(value), "O CPF deve ter 11 dígitos."),
})

type UserFormValues = z.infer<typeof createSchema>

/** Keeps digits only, at most 11 (accepts a pasted "123.456.789-01") */
function normalizeCpf(value: string): string {
  return value.replace(/\D/g, "").slice(0, 11)
}

type UserFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** When present, the form edits this member */
  user?: Member | null
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
  } = useForm<UserFormValues>({ resolver: zodResolver(isEditing ? editSchema : createSchema) })

  useEffect(() => {
    if (open) {
      reset(
        user
          ? { name: user.name, email: user.email, type: user.type, cpf: "" }
          : { name: "", email: "", type: undefined, cpf: "" },
      )
    }
  }, [open, user, reset])

  const mutation = useMutation({
    mutationFn: (values: UserFormValues) =>
      user
        ? updateUser(user.id, { ...values, cpf: values.cpf || undefined })
        : createUser(values),
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
            {isEditing
              ? "Altere os dados do usuário."
              : "Cadastro feito no balcão. O usuário poderá criar o próprio acesso depois, com o mesmo CPF e e-mail."}
          </DialogDescription>
        </DialogHeader>

        <form id="user-form" className="grid gap-4" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
          <FormField id="user-name" label="Nome" error={errors.name?.message}>
            <Input id="user-name" autoComplete="off" aria-invalid={!!errors.name} {...register("name")} />
          </FormField>
          <FormField id="user-email" label="E-mail" error={errors.email?.message}>
            <Input id="user-email" type="email" autoComplete="off" aria-invalid={!!errors.email} {...register("email")} />
          </FormField>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              id="user-cpf"
              label="CPF"
              error={errors.cpf?.message}
              hint={isEditing ? "Em branco mantém o atual." : "Só números."}
            >
              <Controller
                control={control}
                name="cpf"
                render={({ field }) => (
                  <Input
                    id="user-cpf"
                    inputMode="numeric"
                    autoComplete="off"
                    placeholder={isEditing ? "•••••••••••" : "00000000000"}
                    className="tabular-nums"
                    aria-invalid={!!errors.cpf}
                    value={field.value ?? ""}
                    onChange={(event) => field.onChange(normalizeCpf(event.target.value))}
                    onBlur={field.onBlur}
                  />
                )}
              />
            </FormField>
            <FormField id="user-type" label="Tipo" error={errors.type?.message}>
              <Controller
                control={control}
                name="type"
                render={({ field }) => (
                  <Select value={field.value ?? ""} onValueChange={field.onChange}>
                    <SelectTrigger id="user-type" className="w-full" aria-invalid={!!errors.type} onBlur={field.onBlur}>
                      <SelectValue placeholder="Selecione" />
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
          </div>
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
