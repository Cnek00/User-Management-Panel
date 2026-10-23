# User Management Panel (UMP)

A comprehensive user management system built with Spring Boot and PostgreSQL. It features a robust backend architecture with Docker support for easy deployment and scaling.

## Tech Stack
- **Backend:** Spring Boot (Java), Spring Data JPA, Hibernate
- **Database:** PostgreSQL 16
- **Frontend:** Thymeleaf + Tailwind CSS
- **Containerization:** Docker & Docker Compose

## Features
- User authentication and authorization.
- Full CRUD operations for user management.
- Robust database integration with PostgreSQL.
- Easy deployment using Docker Compose.

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17+ (if running locally without Docker)

### Running with Docker
1. Copy the example environment file and set your own database password:
   ```bash
   cp .env.example .env
   # edit .env and replace "change-me" with a strong password
   ```
2. Build and start the services:
   ```bash
   docker compose up -d --build
   ```
3. The application will be available at `http://localhost:<SPRING_PORT>`.
4. The PostgreSQL database will be accessible on your configured `${DB_PORT}`.

> If you previously started the database with a different password, remove the old volume once with `docker compose down -v` so PostgreSQL re-initialises with the new credentials.

### Local Development
To run the Spring Boot application locally without Docker, ensure your local PostgreSQL instance is running and export `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` (or use the same `.env` values), then run the backend from the `backend` directory:
```bash
cd backend
./mvnw spring-boot:run
```

