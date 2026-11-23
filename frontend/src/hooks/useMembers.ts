import { useMemo } from "react"
import { useQuery } from "@tanstack/react-query"

import { getUsers } from "@/api/users"
import { queryKeys } from "@/lib/queryKeys"
import { isMember, type Member } from "@/types/user"

/** Library members sorted by name. The librarian is also a user but never borrows books. */
export function useMembers() {
  const query = useQuery({ queryKey: queryKeys.users, queryFn: getUsers })
  const members = useMemo<Member[]>(
    () => (query.data ?? []).filter(isMember).sort((a, b) => a.name.localeCompare(b.name, "pt-BR")),
    [query.data],
  )
  return { ...query, members }
}
