# Biblioteca Universitária — microsserviços

Sistema de gestão de uma biblioteca universitária, construído em arquitetura de microsserviços. Ele é usado pela bibliotecária para cadastrar livros e usuários, registrar empréstimos, devoluções e renovações e controlar as multas por atraso.

Trabalho acadêmico e projeto de portfólio.

## Arquitetura

```
Navegador → frontend (React) → API Gateway :8080 → Eureka :8761 descobre os serviços
                                   ├── /users/**            → user-service    :8081 (MariaDB)
                                   ├── /books/**            → catalog-service :8082 (MongoDB)
                                   └── /loans/**, /fines/** → loan-service    :8083 (MariaDB)
```

| Módulo | Porta | Responsabilidade | Banco |
|---|---|---|---|
| `service-registry` | 8761 | Servidor Eureka: os serviços se registram nele e são encontrados pelo nome | — |
| `api-gateway` | 8080 | Porta de entrada única: roteia as requisições e configura o CORS | — |
| `user-service` | 8081 | Cadastro de usuários (aluno, professor, externo) | MariaDB `user_service` |
| `catalog-service` | 8082 | Catálogo de livros, busca e controle de exemplares disponíveis | MongoDB `catalog_service` |
| `loan-service` | 8083 | Empréstimos, devoluções, renovações e multas | MariaDB `loan_service` |
| `frontend` | 3000 (Docker) / 5173 (dev) | Interface web da bibliotecária | — |

- O frontend fala **somente com o gateway**. O gateway usa `lb://NOME-DO-SERVICO` e descobre o endereço de cada serviço pelo Eureka.
- O `loan-service` consulta o `user-service` e o `catalog-service` para validar o usuário e o livro, e pede ao catálogo que atualize a disponibilidade quando um livro é emprestado ou devolvido.
- Cada serviço tem o próprio banco (*database per service*). Livros ficam no MongoDB; usuários e empréstimos, no MariaDB.
- Não há autenticação: o sistema é de uso interno da biblioteca.

## Tecnologias

**Backend:** Java 17, Spring Boot 4, Spring Cloud Gateway (WebFlux), Netflix Eureka, Spring Data JPA, Spring Data MongoDB, Bean Validation, MariaDB 11 e MongoDB.

**Frontend:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query, react-hook-form + zod, sonner e lucide-react.

**Infraestrutura:** Docker com builds multi-stage e Docker Compose. O frontend é servido por nginx.

## Funcionalidades

| Requisito | Descrição |
|---|---|
| RF01–RF03 | Cadastrar, editar e remover livros |
| RF08 | Buscar livros por título, autor ou categoria |
| RF04 | Cadastrar e editar usuários |
| RF05 | Registrar empréstimos |
| RF06 | Registrar devoluções (gera multa quando há atraso) |
| RF07 | Renovar empréstimos |
| RF09 | Consultar o histórico de empréstimos de um usuário |
| RF10 | Consultar e pagar multas |

## Regras de negócio

| Tipo de usuário | Máx. de livros | Prazo (dias) | Renovações |
|---|---|---|---|
| Aluno (`STUDENT`) | 3 | 7 | 5 |
| Professor (`TEACHER`) | 5 | 15 | 5 |
| Externo (`EXTERNAL`) | 1 | 5 | 1 |

- Multa de **R$ 2,00 por dia de atraso**, gerada no momento da devolução.
- Usuário com multa pendente ou livro atrasado não pode fazer novos empréstimos.
- O mesmo usuário não pode estar com dois exemplares do mesmo livro.
- Empréstimo atrasado ou já devolvido não pode ser renovado.
- Livro com exemplar emprestado não pode ser removido, e o total de exemplares não pode ficar abaixo da quantidade emprestada.
- O ISBN é único e precisa ter 10 ou 13 dígitos, sem hífens, com dígito verificador válido.
- O e-mail do usuário é único.

## API

Todas as rotas passam pelo gateway em `http://localhost:8080`.

### Usuários

| Método | Rota | Descrição |
|---|---|---|
| GET | `/users` | Lista os usuários |
| GET | `/users/{id}` | Busca um usuário |
| POST | `/users` | Cadastra (`name`, `email`, `type`) |
| PUT | `/users/{id}` | Atualiza |

### Livros

| Método | Rota | Descrição |
|---|---|---|
| GET | `/books` | Lista os livros |
| GET | `/books/{id}` | Busca um livro |
| GET | `/books/search?title=` · `?author=` · `?category=` | Busca por um critério de cada vez |
| POST | `/books` | Cadastra (`title`, `author`, `category`, `isbn`, `totalCopies`) |
| PUT | `/books/{id}` | Atualiza |
| DELETE | `/books/{id}` | Remove |
| PATCH | `/books/{id}/availability` | Uso interno do `loan-service` |

### Empréstimos e multas

| Método | Rota | Descrição |
|---|---|---|
| POST | `/loans` | Empresta (`userId`, `bookId`) |
| POST | `/loans/{id}/return` | Devolve |
| POST | `/loans/{id}/renew` | Renova |
| GET | `/loans/{id}` | Busca um empréstimo |
| GET | `/loans/user/{userId}` | Histórico do usuário (mais recente primeiro) |
| GET | `/loans/user/{userId}/pending` | Indica se há multa pendente ou atraso |
| GET | `/fines/user/{userId}` | Multas do usuário |
| POST | `/fines/{id}/pay` | Paga uma multa |

### Formato de erro

Os três serviços respondem os erros no mesmo formato:

```json
{ "status": 400, "error": "Validation failed", "details": ["email: Email must be valid"] }
```

## Como executar

### Com Docker (tudo de uma vez)

Requisito: Docker com Docker Compose.

```bash
docker compose up --build
```

| Endereço | O que é |
|---|---|
| http://localhost:3000 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Painel do Eureka |

Os bancos são criados automaticamente (`docker/init.sql`) e os dados ficam nos volumes `mariadb-data` e `mongo-data`. Os serviços levam alguns segundos para se registrar no Eureka; até lá, o gateway pode responder `503`.

### Em desenvolvimento

Requisitos: Java 17, Node.js 22, MariaDB em `localhost:3306` e MongoDB em `localhost:27017`.

1. Crie os bancos `user_service` e `loan_service` no MariaDB (veja `docker/init.sql`).
2. Em `user-service/` e `loan-service/`, crie um arquivo `.env` com a senha do MariaDB (ele não vai para o git):
   ```
   DB_PASSWORD=sua-senha
   ```
3. Suba os serviços nesta ordem, cada um no seu diretório, com `./mvnw spring-boot:run` (ou pelo IntelliJ):
   `service-registry` → `user-service`, `catalog-service`, `loan-service` → `api-gateway`.
4. Suba o frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   e abra http://localhost:5173.

Outra opção é subir só o backend pelo Docker e rodar apenas o frontend com `npm run dev`:

```bash
docker compose up --build api-gateway user-service catalog-service loan-service
```

## Roteiro de teste

1. Cadastrar um usuário e um livro.
2. Emprestar o livro e conferir que a disponibilidade diminuiu.
3. Tentar passar do limite de empréstimos do tipo de usuário e ver o erro.
4. Renovar e depois devolver.
5. Para testar multa: alterar o vencimento direto no banco e devolver o livro.
   ```sql
   UPDATE loan_service.loans SET due_date = '2026-01-01' WHERE id = 1;
   ```
   A multa aparece no detalhe do usuário, onde pode ser paga.

## Estrutura do repositório

```
library-microservices/
├── service-registry/   ← Eureka Server
├── api-gateway/        ← Spring Cloud Gateway
├── user-service/       ← usuários (MariaDB)
├── catalog-service/    ← livros (MongoDB)
├── loan-service/       ← empréstimos e multas (MariaDB)
├── frontend/           ← React + TypeScript (veja frontend/README.md)
├── docker/init.sql     ← criação dos bancos do MariaDB
└── docker-compose.yml
```

Cada serviço Java segue a organização em camadas `controller` → `service` → `repository`, com `dto`, `model` e um `GlobalExceptionHandler` para padronizar os erros.
