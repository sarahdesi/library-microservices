# Biblioteca Universitária — microsserviços

Sistema de gestão de uma biblioteca universitária, construído em arquitetura de microsserviços. A bibliotecária cadastra livros e usuários, registra empréstimos, devoluções e renovações e controla as multas por atraso. Alunos, professores e usuários externos criam a própria conta, acompanham os seus empréstimos e podem renová-los.

Trabalho acadêmico e projeto de portfólio.

## Arquitetura

```
Navegador → frontend (React) → API Gateway :8080 (valida o JWT) → Eureka :8761 descobre os serviços
                                   ├── /users/**, /auth/**  → user-service    :8081 (MariaDB)
                                   ├── /books/**            → catalog-service :8082 (MongoDB)
                                   └── /loans/**, /fines/** → loan-service    :8083 (MariaDB)

user-service ──(Eureka, rede interna)──→ university-service :8084 (MariaDB)   ← sem rota no gateway
```

| Módulo | Porta | Responsabilidade | Banco |
|---|---|---|---|
| `service-registry` | 8761 | Servidor Eureka: os serviços se registram nele e são encontrados pelo nome | — |
| `api-gateway` | 8080 | Porta de entrada única: valida o JWT, aplica as permissões por perfil, roteia e configura o CORS | — |
| `user-service` | 8081 | Usuários, login (emite o JWT) e cadastro de membros | MariaDB `user_service` |
| `catalog-service` | 8082 | Catálogo de livros, busca e controle de exemplares disponíveis | MongoDB `catalog_service` |
| `loan-service` | 8083 | Empréstimos, devoluções, renovações e multas | MariaDB `loan_service` |
| `university-service` | 8084 | Simula o sistema acadêmico: bases de alunos (matrícula) e professores (MASP) | MariaDB `university_service` |
| `frontend` | 3000 (Docker) / 5173 (dev) | Interface web da bibliotecária | — |

- O frontend fala **somente com o gateway**. O gateway usa `lb://NOME-DO-SERVICO` e descobre o endereço de cada serviço pelo Eureka.
- O `loan-service` consulta o `user-service` e o `catalog-service` para validar o usuário e o livro, e pede ao catálogo que atualize a disponibilidade quando um livro é emprestado ou devolvido.
- Cada serviço tem o próprio banco (*database per service*). Livros ficam no MongoDB; usuários e empréstimos, no MariaDB.
- O `user-service` consulta o `university-service` para confirmar que um aluno ou professor existe, está ativo e que o CPF confere.
- Só o gateway (8080), o Eureka (8761) e o frontend (3000) publicam porta no Docker. Os serviços de 8081 a 8084 só são acessíveis pela rede interna.

## Autenticação e perfis

- **Login com JWT** (HS256, expira em 1 hora). O `user-service` gera o token e o **gateway** o valida em toda requisição, exceto `POST /auth/login` e `POST /auth/register/**`. A chave fica só na variável `JWT_SECRET`, compartilhada entre os dois.
- O gateway descarta os headers `X-User-Id` e `X-User-Role` enviados pelo cliente e coloca os valores do token. Os serviços usam esses headers para saber quem chama e conferem o dono dos dados ("só o próprio").
- **Perfis:**
  - `LIBRARIAN` (bibliotecária): opera o balcão — cadastros, empréstimos, devoluções, renovações e pagamento de multas. A primeira é criada na inicialização a partir de `LIBRARIAN_EMAIL` e `LIBRARIAN_PASSWORD`; não existe endpoint para criar outra.
  - `MEMBER` (aluno, professor ou externo): consulta o catálogo, os próprios dados, empréstimos, pendências e multas, e **renova os próprios empréstimos**. Empréstimo, devolução e pagamento continuam só no balcão.
- **Cadastro do membro:**
  - Externo: nome, CPF, e-mail e senha.
  - Aluno (matrícula) e professor (MASP): número, CPF, e-mail e senha. A biblioteca confere no `university-service` se o número existe **na base do tipo escolhido**, está ativo e se o CPF bate — por isso um aluno não consegue se cadastrar como professor. O nome vem da universidade.
  - Quem já foi cadastrado no balcão ativa a própria conta pelo cadastro público, com o mesmo CPF, e-mail e tipo.
- **O vínculo com a universidade é conferido de novo a cada login** de aluno e professor: quem se formou ou saiu não recebe novo token. Universidade fora do ar: `503`.
- Senhas com BCrypt. As respostas nunca trazem senha, hash nem CPF.

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

Todas as rotas passam pelo gateway em `http://localhost:8080` e, exceto login e cadastro, exigem `Authorization: Bearer <token>`.

### Autenticação

| Método | Rota | Quem | Descrição |
|---|---|---|---|
| POST | `/auth/login` | público | `email`, `password` → `{ token, user }` |
| POST | `/auth/register/external` | público | `name`, `email`, `cpf`, `password` |
| POST | `/auth/register/student` | público | `registrationNumber` (matrícula), `cpf`, `email`, `password` |
| POST | `/auth/register/teacher` | público | `registrationNumber` (MASP), `cpf`, `email`, `password` |
| GET | `/users/me` · `/loans/me` · `/loans/me/pending` · `/fines/me` | membro | Dados do usuário logado |

### Permissões

| Rota | Bibliotecária | Membro |
|---|---|---|
| `GET /books/**` | sim | sim |
| `POST`, `PUT`, `DELETE` em `/books` | sim | não |
| `GET /users`, `POST /users`, `PUT /users/{id}` | sim | não |
| `GET /users/{id}`, `/loans/{id}`, `/loans/user/{id}/**`, `/fines/user/{id}` | sim | só os próprios |
| `POST /loans`, `/loans/{id}/return`, `/fines/{id}/pay` | sim | não |
| `POST /loans/{id}/renew` | sim | só os próprios |
| `PATCH /books/{id}/availability` | bloqueado no gateway (uso interno do `loan-service`) | bloqueado |

Sem token ou com token inválido: `401`. Perfil sem permissão: `403`. Os dois no mesmo formato de erro dos serviços.

### Usuários

| Método | Rota | Descrição |
|---|---|---|
| GET | `/users` | Lista os usuários |
| GET | `/users/{id}` | Busca um usuário |
| POST | `/users` | Cadastra no balcão (`name`, `email`, `type`, `cpf`), sem senha |
| PUT | `/users/{id}` | Atualiza (`cpf` opcional: vazio mantém o atual) |

### Livros

| Método | Rota | Descrição |
|---|---|---|
| GET | `/books` | Lista os livros |
| GET | `/books/{id}` | Busca um livro |
| GET | `/books/search?title=` · `?author=` · `?category=` | Busca por um critério de cada vez |
| POST | `/books` | Cadastra (`title`, `author`, `category`, `isbn`, `totalCopies`) |
| PUT | `/books/{id}` | Atualiza |
| DELETE | `/books/{id}` | Remove |
| PATCH | `/books/{id}/availability` | Uso interno do `loan-service` (bloqueado no gateway) |

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

Os serviços e o próprio gateway (em `401` e `403`) respondem os erros no mesmo formato:

```json
{ "status": 400, "message": "Validation failed", "details": ["email: Email must be valid"] }
```

Quando o próprio gateway falha (por exemplo, um serviço fora do ar), a resposta segue o formato padrão do Spring, com o texto no campo `error`.

## Como executar

### Com Docker (tudo de uma vez)

Requisito: Docker com Docker Compose.

1. Copie o `.env.example` da raiz para `.env` e defina os valores (o `.env` não vai para o git):
   ```
   JWT_SECRET=uma-chave-com-pelo-menos-32-caracteres
   LIBRARIAN_EMAIL=bibliotecaria@exemplo.com
   LIBRARIAN_PASSWORD=uma-senha-com-8-ou-mais
   ```
   Sem `JWT_SECRET`, o `docker compose` para com uma mensagem pedindo a variável.
2. Suba tudo:
   ```bash
   docker compose up --build
   ```

| Endereço | O que é |
|---|---|
| http://localhost:3000 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Painel do Eureka |

Os bancos são criados automaticamente (`docker/init.sql`, e o `university_service` também pela URL JDBC com `createDatabaseIfNotExist=true`) e os dados ficam nos volumes `mariadb-data` e `mongo-data`. Os serviços levam alguns segundos para se registrar no Eureka; até lá, o gateway pode responder `503`.

A bibliotecária inicial só é criada se ainda não existir nenhuma: trocar `LIBRARIAN_EMAIL` depois não cria outra.

### Em desenvolvimento

Requisitos: Java 17, Node.js 22, MariaDB em `localhost:3306` e MongoDB em `localhost:27017`.

1. Crie os bancos `user_service` e `loan_service` no MariaDB (veja `docker/init.sql`). O `university_service` é criado sozinho.
2. Em `user-service/`, `loan-service/` e `university-service/`, crie um arquivo `.env` a partir do `.env.example` (ele não vai para o git). O do `user-service` também leva `JWT_SECRET`, `LIBRARIAN_EMAIL` e `LIBRARIAN_PASSWORD`.
3. O `api-gateway` precisa da mesma `JWT_SECRET` como variável de ambiente (no IntelliJ: *Run configuration → Environment variables*).
4. Suba os serviços nesta ordem, cada um no seu diretório, com `./mvnw spring-boot:run` (ou pelo IntelliJ):
   `service-registry` → `university-service`, `user-service`, `catalog-service`, `loan-service` → `api-gateway`.

   Em desenvolvimento as portas 8081 a 8084 ficam abertas na sua máquina e aceitam chamadas sem token: a proteção vale para o acesso pelo gateway e para o Docker, onde essas portas não são publicadas.
5. Suba o frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   e abra http://localhost:5173.

Outra opção é subir só o backend pelo Docker e rodar apenas o frontend com `npm run dev`:

```bash
docker compose up --build api-gateway user-service catalog-service loan-service university-service
```

## Dados de teste da universidade

O `university-service` cria estes registros (fictícios) na primeira inicialização. Use-os para testar o cadastro de aluno e professor:

| Tipo | Número | CPF | Nome | Curso / departamento | Situação |
|---|---|---|---|---|---|
| Aluno | matrícula `2024001` | `11111111111` | Ana Beatriz Souza | Ciência da Computação | ativo |
| Aluno | matrícula `2024002` | `22222222222` | Bruno Henrique Lima | Engenharia Civil | ativo |
| Aluno | matrícula `2024003` | `33333333333` | Carla Mendes Rocha | Pedagogia | ativo |
| Aluno | matrícula `2019050` | `44444444444` | Diego Ferreira Alves | Direito | **inativo** |
| Professor | MASP `10203040` | `55555555555` | Eduardo Castro Nunes | Departamento de Computação | ativo |
| Professor | MASP `10203041` | `66666666666` | Fernanda Rocha Dias | Departamento de Letras | ativo |
| Professor | MASP `90807060` | `77777777777` | Gustavo Almeida Reis | Departamento de Matemática | **inativo** |

Para simular que alguém se formou: `UPDATE university_service.university_student SET active = false WHERE registration_number = '2024001';` — o próximo login dessa pessoa é recusado.

## Roteiro de teste

1. Entrar como bibliotecária (credenciais do `.env`). Cadastrar um usuário e um livro.
2. Emprestar o livro e conferir que a disponibilidade diminuiu.
3. Tentar passar do limite de empréstimos do tipo de usuário e ver o erro.
4. Renovar e depois devolver.
5. Para testar multa: alterar o vencimento direto no banco e devolver o livro.
   ```sql
   UPDATE loan_service.loans SET due_date = '2026-01-01' WHERE id = 1;
   ```
   A multa aparece no detalhe do usuário, onde pode ser paga.
6. Cadastrar-se como aluno com a matrícula `2024001` e o CPF `11111111111`, entrar e conferir que só aparecem os próprios dados. Tentar com a matrícula inativa `2019050` e ver o erro de dados que não conferem.

## Testes

```bash
cd <serviço> && ./mvnw test
```

- Os testes de integração usam **Testcontainers** (MariaDB e MongoDB em containers descartáveis): precisam do Docker rodando, mas não dos bancos locais.
- `api-gateway`: matriz de permissões, `401`/`403`, headers falsificados, bloqueio do `PATCH` de disponibilidade e CORS, com um serviço falso no lugar dos reais.
- `user-service`: login, cadastros (externo, aluno, professor), ativação de conta do balcão, conferência do vínculo no login e "só o próprio".
- `loan-service`: concorrência (último exemplar, devolução dupla, limite do mesmo usuário) e "só o próprio" (renovar, consultar).
- **Ponta a ponta** contra o stack do Docker, desligados por padrão (o `AuthFlowE2ETests` precisa de uma base da universidade ainda não usada; rode num stack descartável, por exemplo com `docker compose -p library-e2e ...` e `down -v` no fim):
  ```bash
  cd loan-service
  ./mvnw test -Dtest='*E2ETests' -De2e=true -De2e.librarianEmail=... -De2e.librarianPassword=...
  ```

## Limitações conhecidas

- **Concorrência:** a reserva de exemplar é atômica no catálogo e a devolução e a renovação travam o empréstimo. Restam: não há transação distribuída (falha de rede depois da reserva, ou na compensação, deixa a disponibilidade errada, só registrada em log); o ISBN único e o pagamento de multa não têm proteção contra requisições simultâneas; os locks ficam presos durante as chamadas HTTP entre serviços.
- O token JWT fica no navegador e não há refresh token: depois de 1 hora é preciso entrar de novo. "Sair" só apaga o token no navegador; um token roubado vale até expirar.
- Um token já emitido continua valendo até expirar mesmo que o vínculo com a universidade acabe nesse meio-tempo. Os empréstimos em si não reconferem o vínculo.
- Não há confirmação de e-mail nem SSO da universidade: a validação é matrícula/MASP mais CPF. O CPF é validado só pelo formato, sem dígito verificador.
- A base da universidade é simulada por um microsserviço com dados de teste.
- A comunicação interna entre serviços não é autenticada: a proteção é a rede do Docker não ser exposta. Em desenvolvimento, as portas 8081 a 8084 ficam abertas.

## Estrutura do repositório

```
library-microservices/
├── service-registry/   ← Eureka Server
├── api-gateway/        ← Spring Cloud Gateway
├── user-service/       ← usuários (MariaDB)
├── catalog-service/    ← livros (MongoDB)
├── loan-service/       ← empréstimos e multas (MariaDB)
├── university-service/ ← sistema acadêmico simulado (MariaDB)
├── frontend/           ← React + TypeScript (veja frontend/README.md)
├── docker/init.sql     ← criação dos bancos do MariaDB
└── docker-compose.yml
```

Cada serviço Java segue a organização em camadas `controller` → `service` → `repository`, com `dto`, `model` e um `GlobalExceptionHandler` para padronizar os erros.
