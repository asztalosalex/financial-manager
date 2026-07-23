# Financial Manager

A full-stack personal finance management application with Spring Boot backend and future frontend implementation.

## Project Structure

```
financial-manager/
├── backend/                    # Spring Boot REST API
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   └── ...
├── frontend/                   # Frontend application (to be implemented)
│   └── (empty for now)
├── docker-compose.yml          # Full-stack orchestration
└── README.md
```

## Backend

The backend is a Spring Boot application providing REST APIs for:

- User authentication and authorization (JWT)
- Budget management
- Transaction tracking
- Category management

### Technologies

- Java 21
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT Authentication
- Docker

### Running the Backend

1. **Using Docker Compose (Recommended)**:
   ```bash
   docker-compose up -d
   ```

2. **Using Maven**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`

### API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html`

## Frontend

The frontend is a React application built with TypeScript and Vite, providing a modern user interface for managing personal finances.

### Technologies

- React 18 with TypeScript
- Vite (build tool)
- React Router (routing)
- Axios (HTTP client)
- Tailwind CSS (styling)
- Docker & Nginx (production)

### Features

- User authentication (login/register)
- Dashboard with financial overview
- Transaction management (planned)
- Budget tracking (planned)
- Category management (planned)

### Running the Frontend

1. **Using Docker Compose (Recommended)**:
   ```bash
   docker-compose up -d
   ```

2. **Development mode**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

The frontend will be available at `http://localhost:3000`

## Development

### Prerequisites

- Java 21
- Maven 3.6+
- Docker and Docker Compose
- PostgreSQL (if running locally)

### TLS Certificates

The project uses HTTPS locally via nginx. You need to generate self-signed certificates with [mkcert](https://github.com/FiloSottile/mkcert).

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

After this, the app is available at `https://frontend.fmanager.local` and the API at `https://backend.fmanager.local`.

### Environment Variables

Create a `.env` file in the root directory:

```env
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_password
POSTGRES_DB=financial_manager
```

## License

This project is licensed under the MIT License.
