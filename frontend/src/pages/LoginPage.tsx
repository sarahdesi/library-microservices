import { useState } from "react"
import { Link, useLocation, useNavigate } from "react-router"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"

import { AuthLayout } from "@/components/auth/AuthLayout"
import { FormField } from "@/components/FormField"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/hooks/useAuth"
import { describeError } from "@/lib/errors"
import { homePath } from "@/lib/roles"

const loginSchema = z.object({
  email: z.string().trim().min(1, "Informe o e-mail."),
  password: z.string().min(1, "Informe a senha."),
})

type LoginValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: "", password: "" } })

  const onSubmit = async (values: LoginValues) => {
    setError(null)
    try {
      const session = await login(values.email.trim(), values.password)
      // back to the page that asked for login, unless it belongs to another profile
      const target = from && from !== "/" ? from : homePath(session.user.role)
      navigate(target, { replace: true })
    } catch (e) {
      setError(describeError(e).message)
    }
  }

  return (
    <AuthLayout
      title="Entrar"
      description="Use o e-mail e a senha cadastrados."
      footer={
        <>
          Ainda não tem acesso?{" "}
          <Link to="/cadastro" className="text-primary transition-colors duration-150 hover:text-primary-hover hover:underline">
            Criar conta
          </Link>
        </>
      }
    >
      <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <FormField id="login-email" label="E-mail" error={errors.email?.message}>
          <Input id="login-email" type="email" autoComplete="email" autoFocus aria-invalid={!!errors.email} {...register("email")} />
        </FormField>
        <FormField id="login-password" label="Senha" error={errors.password?.message}>
          <Input
            id="login-password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            {...register("password")}
          />
        </FormField>

        {error && (
          <p role="alert" className="rounded-md border border-danger/25 bg-danger-bg px-3 py-2 text-[13px] text-danger">
            {error}
          </p>
        )}

        <Button type="submit" className="mt-1 w-full" disabled={isSubmitting}>
          {isSubmitting ? "Entrando…" : "Entrar"}
        </Button>
      </form>
    </AuthLayout>
  )
}
