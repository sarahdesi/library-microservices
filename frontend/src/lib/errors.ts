import { ApiError, NetworkError } from "@/api/client"

const exactMessages: Record<string, string> = {
  "User not found": "Usuário não encontrado.",
  "Book not found": "Livro não encontrado.",
  "Loan not found": "Empréstimo não encontrado.",
  "Fine not found": "Multa não encontrada.",
  "Email already registered": "Este e-mail já está cadastrado.",
  "ISBN already registered": "Já existe um livro com este ISBN.",
  "No copies available": "Não há exemplares disponíveis deste livro.",
  "User already has a copy of this book": "Este usuário já está com um exemplar deste livro.",
  "User has pending fines or overdue loans": "O usuário tem multa pendente ou livro atrasado.",
  "Loan already returned": "Este empréstimo já foi devolvido.",
  "Returned loans cannot be renewed": "Empréstimos devolvidos não podem ser renovados.",
  "Overdue loans cannot be renewed": "Empréstimos atrasados não podem ser renovados.",
  "Fine already paid": "Esta multa já foi paga.",
  "Cannot delete a book with copies on loan": "Não é possível remover um livro com exemplares emprestados.",
  "Provide title, author or category to search": "Informe título, autor ou categoria para buscar.",
  "Validation failed": "Verifique os campos do formulário.",
  "User service unavailable": "O serviço de usuários está indisponível.",
  "Catalog service unavailable": "O serviço de catálogo está indisponível.",
}

/** Messages carrying a number, recognized by their beginning */
const numberedMessages: { prefix: string; translate: (n: string) => string }[] = [
  { prefix: "Loan limit reached", translate: (n) => `Limite de empréstimos atingido (${n} livros).` },
  { prefix: "Renewal limit reached", translate: (n) => `Limite de renovações atingido (${n}).` },
  {
    prefix: "Total copies cannot be less than copies on loan",
    translate: (n) => `O total não pode ser menor que os exemplares emprestados (${n}).`,
  },
]

/** Translates a known backend message; unknown messages are returned unchanged */
export function translateApiMessage(message: string): string {
  const text = message.trim()
  if (exactMessages[text]) return exactMessages[text]

  for (const { prefix, translate } of numberedMessages) {
    if (text.startsWith(prefix)) {
      const number = text.slice(prefix.length).match(/\d+/)?.[0]
      if (number) return translate(number)
    }
  }

  return text
}

export const NETWORK_ERROR_MESSAGE = "Não foi possível conectar ao servidor."
export const SERVICE_UNAVAILABLE_MESSAGE = "Serviço temporariamente indisponível. Tente novamente em instantes."

export type ErrorDescription = { message: string; details: string[] }

/** Turns any thrown value into a message ready for the UI */
export function describeError(error: unknown): ErrorDescription {
  if (error instanceof NetworkError) {
    return { message: NETWORK_ERROR_MESSAGE, details: [] }
  }

  if (error instanceof ApiError) {
    const translated = translateApiMessage(error.message)
    // Gateway 503 (service down or not registered) has no specific message worth showing
    if (error.status === 503 && translated === error.message) {
      return { message: SERVICE_UNAVAILABLE_MESSAGE, details: [] }
    }
    return { message: translated || `Erro ${error.status}.`, details: error.details }
  }

  return { message: "Ocorreu um erro inesperado.", details: [] }
}
