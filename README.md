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
1. Configure your environment variables in a `.env` file (e.g., `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SPRING_PORT`, `DB_PORT`).
2. Build and start the services:
   ```bash
   docker-compose up -d --build
   ```
3. The application will be available at `http://localhost:<SPRING_PORT>`.
4. The PostgreSQL database will be accessible on your configured `${DB_PORT}`.

### Local Development
To run the Spring Boot application locally without Docker, ensure your local PostgreSQL instance is running and update the `application.properties` or environment variables accordingly, then run the backend from the `backend` directory.
