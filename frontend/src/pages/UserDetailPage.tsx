import { useParams } from "react-router"

import { PageHeader } from "@/components/layout/PageHeader"

export function UserDetailPage() {
  const { id } = useParams()
  return <PageHeader title="Detalhe do usuário" description={`Usuário ${id}`} />
}
