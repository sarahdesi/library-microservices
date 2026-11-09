# Frontend — Biblioteca Universitária

Web interface for the university library system. It talks only to the API Gateway; see the [root README](../README.md) for the full architecture.

**Stack:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query, react-hook-form + zod, sonner and lucide-react.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | API Gateway URL, as seen by the browser |

Copy `.env.example` to `.env` to change it. Vite embeds the value at build time.

## Development

Requires Node.js 22 and the backend running (gateway on port 8080).

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
├── api/          HTTP client and one module per resource (books, users, loans, fines)
├── types/        TypeScript types mirroring the backend DTOs
├── components/   ui/ (shadcn), layout/ (sidebar) and feature components
├── hooks/        shared data hooks (loan actions, books lookup, debounce)
├── lib/          formatting, enum labels, error translation, business rules
├── pages/        BooksPage, UsersPage, UserDetailPage, LoansPage
└── App.tsx       routes
```

| Route | Page |
|---|---|
| `/livros` | Books: list, search, create, edit, delete |
| `/usuarios` | Users: list, create, edit |
| `/usuarios/:id` | User detail: rules, pending alert, loan history, fines |
| `/emprestimos` | Loans: new loan, return and renew per user |
