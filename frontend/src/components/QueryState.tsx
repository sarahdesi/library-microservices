import { Button } from "@/components/ui/button"
import { describeError } from "@/lib/errors"

export function LoadingState({ text = "Carregando…" }: { text?: string }) {
  return <p className="py-10 text-center text-muted-foreground">{text}</p>
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { message } = describeError(error)
  return (
    <div className="flex flex-col items-center gap-3 py-10 text-center">
      <p className="text-danger">{message}</p>
      {onRetry && (
        <Button variant="outline" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}
