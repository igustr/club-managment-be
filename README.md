# Club Management Backend

REST API backend for managing Estonian football clubs — members, training schedules, field bookings, attendance tracking, and team chat with role-based access control.

**Bachelor's thesis:** "Eesti jalgpalliklubide haldamine veebipõhise rakenduse abil" (TalTech, 2026)

## Tech Stack

- **Java 21** / **Spring Boot 3.5.x** / **Gradle Kotlin DSL**
- **PostgreSQL 17** with **Liquibase** migrations
- **Spring Security 6** + **JWT** authentication
- **MapStruct** + **Lombok**
- **JUnit 5** + **TestContainers** + **ArchUnit** for testing
- **Spotless** (Google Java Format) for code quality
- **Docker Compose** for local development

## Prerequisites

- Java 21+
- Docker & Docker Compose

## Getting Started

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8080/api`

API docs (Swagger UI): `http://localhost:8080/swagger-ui.html`

## Development

```bash
# Run tests
./gradlew test

# Format code (Google Java Format)
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Full build
./gradlew build
```

## Project Structure

```
src/main/java/ee/finalthesis/clubmanagement/
├── api/controller/       # REST controllers
├── config/               # Spring configuration
├── security/             # JWT, authentication, RBAC
├── service/              # Business logic
│   ├── dto/              # Data Transfer Objects
│   └── mapper/           # MapStruct mappers
├── repository/           # Spring Data JPA repositories
├── domain/               # JPA entities
│   ├── enumeration/      # Enum types
│   └── converter/        # JPA AttributeConverters
└── common/
    ├── exception/        # Error handling (RFC 7807)
    └── validation/       # Custom validators
```

## Testing

```bash
# Run all tests (unit + integration)
./gradlew test

# Tests include:
# - Unit tests (*Test.java): Mockito-based service layer tests
# - Integration tests (*IT.java): Full Spring Boot + TestContainers PostgreSQL
# - Architecture tests: ArchUnit layered architecture enforcement
```

## Roles

| Role      | Description                                  |
|-----------|----------------------------------------------|
| `ADMIN`   | Full club management — users, teams, pitches |
| `COACH`   | Manages own teams, trainings, chat           |
| `PLAYER`  | Views schedule, confirms own attendance      |
| `PARENT`  | Views child's schedule, confirms attendance  |
