# Financial Intelligence API

AI-Powered Financial Intelligence Platform API.

## Getting Started

### Prerequisites
- Java 17 or higher
- Docker (optional, for PostgreSQL)
- Maven (or use provided instructions)

### Running with PostgreSQL (Docker)
The easiest way to run the production configuration is using Docker Compose:

```bash
docker compose up -d --build
```

This will:
1. Start a PostgreSQL 15 container.
2. Build the Spring Boot application.
3. Start the API on [http://localhost:8080](http://localhost:8080).

### Running Locally (with H2 Database)
If you don't have PostgreSQL or Docker, you can run with the `dev` profile which uses an in-memory H2 database:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### API Documentation
Once the application is running, you can access:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (sa / no password)

## Features
- Clean Architecture (Controller/Service/Repository/Entity/DTO).
- UUID Primary Keys for all entities.
- Global Exception Handling.
- Transaction validation and category-based spending analysis.
- AI Analysis Placeholder for fraud risk detection.
