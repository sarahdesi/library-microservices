import { useState } from "react"
import { Link, useNavigate } from "react-router"
import { Controller, useForm, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"

import { registerExternal, registerStudent, registerTeacher } from "@/api/auth"
import { AuthLayout } from "@/components/auth/AuthLayout"
import { FormField } from "@/components/FormField"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { describeError } from "@/lib/errors"
import { showSuccess } from "@/lib/toast"
import { cn } from "@/lib/utils"
import type { UserType } from "@/types/user"

const typeOptions: { type: UserType; label: string }[] = [
  { type: "STUDENT", label: "Aluno" },
  { type: "TEACHER", label: "Professor" },
  { type: "EXTERNAL", label: "Externo" },
]

const numberLabels: Record<Exclude<UserType, "EXTERNAL">, { label: string; hint: string }> = {
  STUDENT: { label: "Matrícula", hint: "Número de matrícula na universidade." },
  TEACHER: { label: "MASP", hint: "Número de servidor (MASP)." },
}

const registerSchema = z
  .object({
    type: z.enum(["STUDENT", "TEACHER", "EXTERNAL"]),
    name: z.string().trim(),
    registrationNumber: z.string().trim(),
    cpf: z.string().regex(/^\d{11}$/, "O CPF deve ter 11 dígitos."),
    email: z.string().trim().min(1, "Informe o e-mail.").pipe(z.email("Informe um e-mail válido.")),
    password: z.string().min(8, "A senha deve ter pelo menos 8 caracteres."),
    confirmPassword: z.string(),
  })
  .superRefine((values, ctx) => {
    if (values.type === "EXTERNAL" && !values.name) {
      ctx.addIssue({ code: "custom", path: ["name"], message: "Informe o nome." })
    }
    if (values.type !== "EXTERNAL" && !values.registrationNumber) {
      ctx.addIssue({
        code: "custom",
        path: ["registrationNumber"],
        message: values.type === "STUDENT" ? "Informe a matrícula." : "Informe o MASP.",
      })
    }
    // the confirmation exists only in the frontend
    if (values.confirmPassword !== values.password) {
      ctx.addIssue({ code: "custom", path: ["confirmPassword"], message: "As senhas não conferem." })
    }
  })

type RegisterValues = z.infer<typeof registerSchema>

function digitsOnly(value: string, max: number) {
  return value.replace(/\D/g, "").slice(0, max)
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<{ message: string; details: string[] } | null>(null)

  const {
    register,
    control,
    handleSubmit,
    setValue,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { type: "STUDENT", name: "", registrationNumber: "", cpf: "", email: "", password: "", confirmPassword: "" },
  })

  const type = useWatch({ control, name: "type" })

  const onSubmit = async (values: RegisterValues) => {
    setError(null)
    const common = { cpf: values.cpf, email: values.email.trim(), password: values.password }
    try {
      if (values.type === "EXTERNAL") {
        await registerExternal({ ...common, name: values.name.trim() })
      } else {
        const data = { ...common, registrationNumber: values.registrationNumber.trim() }
        await (values.type === "STUDENT" ? registerStudent(data) : registerTeacher(data))
      }
      showSuccess("Cadastro realizado. Entre com seu e-mail e senha.")
      navigate("/login", { replace: true })
    } catch (e) {
      setError(describeError(e))
    }
  }

  return (
    <AuthLayout
      title="Criar conta"
      description={
        type === "EXTERNAL"
          ? "Para quem não tem vínculo com a universidade."
          : "Seus dados são conferidos com os registros da universidade."
      }
      footer={
        <>
          Já tem acesso?{" "}
          <Link to="/login" className="text-primary transition-colors duration-150 hover:text-primary-hover hover:underline">
            Entrar
          </Link>
        </>
      }
    >
      <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div role="radiogroup" aria-label="Tipo de cadastro" className="grid grid-cols-3 gap-0.5 rounded-md border bg-background p-0.5">
          {typeOptions.map((option) => {
            const selected = type === option.type
            return (
              <button
                key={option.type}
                type="button"
                role="radio"
                aria-checked={selected}
                onClick={() => {
                  setValue("type", option.type)
                  clearErrors()
                  setError(null)
                }}
                className={cn(
                  "h-8 rounded-[4px] text-[13px] transition-colors duration-150 outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
                  selected
                    ? "bg-card font-medium text-foreground shadow-[0_0_0_1px_var(--border)]"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                {option.label}
              </button>
            )
          })}
        </div>

        {type === "EXTERNAL" ? (
          <FormField id="register-name" label="Nome" error={errors.name?.message}>
            <Input id="register-name" autoComplete="name" aria-invalid={!!errors.name} {...register("name")} />
          </FormField>
        ) : (
          <FormField
            id="register-number"
            label={numberLabels[type].label}
            error={errors.registrationNumber?.message}
            hint={`${numberLabels[type].hint} O nome vem do cadastro da universidade.`}
          >
            <Input
              id="register-number"
              autoComplete="off"
              className="tabular-nums"
              aria-invalid={!!errors.registrationNumber}
              {...register("registrationNumber")}
            />
          </FormField>
        )}

        <FormField id="register-cpf" label="CPF" error={errors.cpf?.message} hint="Só números.">
          <Controller
            control={control}
            name="cpf"
            render={({ field }) => (
              <Input
                id="register-cpf"
                inputMode="numeric"
                autoComplete="off"
                className="tabular-nums"
                aria-invalid={!!errors.cpf}
                value={field.value}
                onChange={(event) => field.onChange(digitsOnly(event.target.value, 11))}
                onBlur={field.onBlur}
              />
            )}
          />
        </FormField>

        <FormField id="register-email" label="E-mail" error={errors.email?.message}>
          <Input id="register-email" type="email" autoComplete="email" aria-invalid={!!errors.email} {...register("email")} />
        </FormField>

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField id="register-password" label="Senha" error={errors.password?.message} hint="Mínimo de 8 caracteres.">
            <Input
              id="register-password"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.password}
              {...register("password")}
            />
          </FormField>
          <FormField id="register-confirm" label="Confirmar senha" error={errors.confirmPassword?.message}>
            <Input
              id="register-confirm"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.confirmPassword}
              {...register("confirmPassword")}
            />
          </FormField>
        </div>

        {error && (
          <div role="alert" className="rounded-md border border-danger/25 bg-danger-bg px-3 py-2 text-[13px] text-danger">
            <p>{error.message}</p>
            {error.details.length > 0 && (
              <ul className="mt-1 list-disc pl-4">
                {error.details.map((detail) => (
                  <li key={detail}>{detail}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        <Button type="submit" className="mt-1 w-full" disabled={isSubmitting}>
          {isSubmitting ? "Cadastrando…" : "Criar conta"}
        </Button>
      </form>
    </AuthLayout>
  )
}
