# Frontend — Biblioteca Universitária

Web interface for the university library system. It talks only to the API Gateway; see the [root README](../README.md) for the full architecture.

Two profiles share the app: the **librarian** runs the counter (books, users, loans, returns, fines) and **members** (students, teachers and external users) see the catalog and their own account, where they can renew their loans. Light, dark and system themes are available on every screen.

**Stack:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query, react-hook-form + zod, sonner and lucide-react.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | API Gateway URL, as seen by the browser |

Copy `.env.example` to `.env` to change it. Vite embeds the value at build time.

## Development

Requires Node.js 22 and the backend running (gateway on port 8080). Log in with the librarian credentials set in the backend (`LIBRARIAN_EMAIL` / `LIBRARIAN_PASSWORD`), or create a member account at `/cadastro` using the university test data listed in the root README.

```bash
npm install
npm run dev       # http://localhost:5173
npm run build     # type-check and production build
npm run lint
```

## Docker

The `Dockerfile` builds the app with `node:22-alpine` and serves `dist/` with nginx on port 3000. `nginx.conf` falls back to `index.html`, so reloading a route like `/usuarios/1` works.

From the repository root:

```bash
docker compose up --build frontend
```

Or on its own:

```bash
docker build --build-arg VITE_API_URL=http://localhost:8080 -t library-frontend .
docker run -p 3000:3000 library-frontend
```

`VITE_API_URL` stays `http://localhost:8080` inside Docker because the code runs in the user's browser, not in the container.

## Structure

```
src/
├── api/          HTTP client (adds the Bearer token, handles 401) and one module per resource
├── auth/         session provider, route guards (RequireAuth, RequireRole, PublicOnly)
├── types/        TypeScript types mirroring the backend DTOs
├── components/   ui/ (shadcn), layout/ (sidebar) and feature components
├── hooks/        shared hooks (auth, theme, loan actions, members, books lookup, debounce)
├── lib/          formatting, labels, error translation, business rules, session storage, theme
├── pages/        Login, Register, MyAccount, Books, Users, UserDetail, Loans
└── App.tsx       routes
```

| Route | Who | Page |
|---|---|---|
| `/login` | visitors | Login |
| `/cadastro` | visitors | Self-registration: student (matrícula), teacher (MASP) or external |
| `/` | everyone | Redirects to the start page of the profile |
| `/livros` | both | Librarian: books with create, edit, delete. Member: read-only catalog |
| `/usuarios` | librarian | Members: search, type filter, create, edit |
| `/usuarios/:id` | librarian | Member card: usage, type rules, pending alert, loans and fines |
| `/emprestimos` | librarian | New loan with a summary of the member, return and renew |
| `/minha-conta` | member | Own data, rules, loans (renew only) and fines (read-only) |

## Authentication

- `POST /auth/login` returns a JWT (1 hour, no refresh token). The session (token and user) is kept in `localStorage`, wrapped in `try/catch`; if storage is blocked it lasts only while the tab is open.
- Every request sends `Authorization: Bearer <token>`. A `401` ends the session and goes to `/login` with "Sua sessão expirou. Entre novamente."; the session also ends when the token expires.
- A page of the other profile sends the user back to their start page with "Você não tem permissão para acessar esta página." Hiding buttons is only a convenience: the API Gateway and the services enforce the permissions.

## Theme

Light, dark or system, chosen in the sidebar footer (and on the login page). The choice is saved in `localStorage`; an inline script in `index.html` applies the `.dark` class before the first paint, so the dark theme never flashes white.
