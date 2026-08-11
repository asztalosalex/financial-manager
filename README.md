# Financial Manager

A full-stack personal finance management application. Users track income and expenses, organize spending into categories, set monthly budgets, and view analytical reports (spending trends, category breakdowns, savings rate, budget status).

Backend: Spring Boot REST API. Frontend: React + TypeScript SPA. Both are containerized behind an HTTPS-terminating nginx reverse proxy for local development.

## Project Structure

```
financial-manager/
├── backend/                    # Spring Boot REST API
│   ├── src/main/java/hu/financial/
│   │   ├── controller/          # REST endpoints
│   │   ├── service/              # Business logic
│   │   ├── repository/           # Spring Data JPA repositories + specifications
│   │   ├── model/                 # JPA entities
│   │   ├── dto/                   # Request/response DTOs per domain
│   │   ├── security/              # JWT + CSRF cookie handling
│   │   └── exception/              # Domain exceptions + global handler
│   ├── src/main/resources/db/migration/  # Flyway SQL migrations
│   ├── src/test/                 # Unit + integration tests (Testcontainers)
│   └── Dockerfile
├── frontend/                    # React + TypeScript SPA
│   ├── src/
│   │   ├── pages/                 # Route-level views (Dashboard, Transactions, Categories, Settings, Login, Register)
│   │   ├── components/            # Reusable UI + layout components
│   │   ├── api/                    # Typed fetch client per domain
│   │   └── auth/                    # Auth context/provider, route guarding
│   └── Dockerfile
├── nginx.conf                   # HTTPS reverse proxy for frontend + backend
├── docker-compose.yml           # Full-stack orchestration (db, backend, frontend, nginx)
└── README.md
```

## Features

- **Authentication** — registration, login, logout; JWT stored in an `httpOnly` cookie with double-submit CSRF protection (not header-based bearer tokens).
- **Transactions** — CRUD, filterable and paginated listing.
- **Categories** — CRUD, scoped per user.
- **Budgets** — CRUD, filterable listing.
- **Reports** — income/expense summary, category breakdown, spending trend over time, and budget-status tracking.
- **User profile** — view/update profile, change password.

## Backend

Layered Spring Boot REST API (`controller → service → repository`) with DTOs and mappers isolating the persistence model from the API contract.

### Technologies

- Java 21
- Spring Boot 3.2.5 (Web, Security, Data JPA, Validation)
- PostgreSQL + Flyway migrations
- JWT (jjwt) issued as an `httpOnly` cookie, with CSRF double-submit cookie protection
- springdoc-openapi (Swagger UI)
- JUnit 5, Spring Security Test, Testcontainers (PostgreSQL) for integration tests
- Docker

### Running the Backend

1. **Using Docker Compose (recommended)**:
   ```bash
   docker-compose up -d
   ```

2. **Using Maven** (requires JDK 21 and Maven 3.9+ installed, PostgreSQL running locally):
   ```bash
   cd backend
   mvn spring-boot:run
   ```

The API is available at `http://localhost:8080` (or `https://backend.fmanager.local` behind the nginx proxy, see [TLS Certificates](#tls-certificates)).

### API Documentation

Swagger UI is available at `/swagger-ui.html` on the backend host.

### Tests

```bash
cd backend
mvn test           # unit tests
mvn verify          # unit + Testcontainers integration tests
```

## Frontend

A React SPA built with TypeScript and Vite. Uses the native `fetch` API through a small typed client (`src/api/client.ts`) rather than an HTTP library — it attaches the CSRF header automatically and centralizes 401 handling.

### Technologies

- React 19 + TypeScript
- Vite (build tool and dev server)
- React Router (routing, incl. route guarding for authenticated pages)
- Plain CSS (no UI framework)
- Vitest + React Testing Library
- Docker & nginx (production image)

### Running the Frontend

1. **Using Docker Compose (recommended)**:
   ```bash
   docker-compose up -d
   ```

2. **Development mode** (proxies `/api` to `https://backend.fmanager.local`, see [TLS Certificates](#tls-certificates)):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### Tests & Linting

```bash
cd frontend
npm test          # vitest run, writes test-results.json
npm run typecheck  # tsc --noEmit
npm run lint       # eslint
```

Once the stack is running behind nginx, the app is available at `https://frontend.fmanager.local` and the API at `https://backend.fmanager.local`.

## Development

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker and Docker Compose
- PostgreSQL (if running the backend locally without Docker)

### TLS Certificates

The project uses HTTPS locally via nginx. Generate self-signed certificates with [mkcert](https://github.com/FiloSottile/mkcert).

1. **Install mkcert**:
   ```bash
   # macOS
   brew install mkcert

   # Linux (Arch)
   sudo pacman -S mkcert

   # Linux (Debian/Ubuntu)
   sudo apt install mkcert
   ```

2. **Install the local CA**:
   ```bash
   mkcert -install
   ```

3. **Generate the certificates**:
   ```bash
   mkdir -p certs
   mkcert -cert-file certs/frontend.fmanager.local+1.pem \
          -key-file certs/frontend.fmanager.local+1-key.pem \
          frontend.fmanager.local localhost

   mkcert -cert-file certs/backend.fmanager.local.pem \
          -key-file certs/backend.fmanager.local-key.pem \
          backend.fmanager.local
   ```

4. **Add the domains to your hosts file** (`/etc/hosts`):
   ```
   127.0.0.1 frontend.fmanager.local
   127.0.0.1 backend.fmanager.local
   ```

### Environment Variables

Copy `.env.example` to `.env` in the root directory and fill in the values:

```env
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_password
POSTGRES_DB=financial_manager

SECURITY_JWT_SECRET_KEY=your_secret_key
SECURITY_JWT_EXPIRATION_TIME=3600
```

Generate a strong `SECURITY_JWT_SECRET_KEY` with `openssl rand -hex 32`. `SECURITY_JWT_EXPIRATION_TIME` is the token lifetime in seconds.

## License

This project is licensed under the MIT License.
