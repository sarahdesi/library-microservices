import { Link } from "react-router"

export function NotFoundPage() {
  return (
    <div>
      <h1 className="font-heading text-2xl font-semibold">Página não encontrada</h1>
      <p className="mt-1 text-[13px] text-muted-foreground">
        O endereço acessado não existe.{" "}
        <Link to="/livros" className="text-primary hover:text-primary-hover hover:underline">
          Voltar para Livros
        </Link>
      </p>
    </div>
  )
}
