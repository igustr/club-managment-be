# Club Management Backend

Estonian football club management web application backend (bachelor's thesis project).

## IMPORTANT: Before Starting Any Implementation

**Before writing any code or making architectural decisions, ALWAYS read these sources first:**

1. **Thesis (requirements & architecture):** Read `final_thesis_Igor_Ustritski.pdf` in the project root — it contains all functional requirements (section 4.6), non-functional requirements (section 4.7), RBAC rules, database model, and architectural decisions.

2. **Development plan:** Read `.claude/development-plan.md` — it contains the phased plan, database model, API design, and RBAC matrix derived from the thesis.

3. **Reference project — emde-2-be:** Browse `/Users/igorustritski/IdeaProjects/emde-2-be` for patterns on:
   - `build.gradle.kts` — Gradle config, dependency management, Spotless setup
   - `src/main/java/**/config/` — SecurityConfiguration, JacksonConfig, OpenAPI config
   - `src/main/java/**/domain/` — AbstractAuditingEntity, entity patterns
   - `src/main/java/**/common/exception/` — ExceptionTranslator (RFC 7807)
   - `src/main/resources/config/liquibase/` — Liquibase changelog conventions
   - `src/test/java/**/archunit/` — ArchUnit architecture tests

4. **Reference project — wallet:** Browse `/Users/igorustritski/IdeaProjects/wallet` for patterns on:
   - `src/main/java/**/config/SecurityConfig.java` — JWT security filter chain
   - `src/main/java/**/biz/service/TokenAuthenticationService.java` — JWT token handling
   - `src/main/java/**/dto/` — DTO organization patterns
   - `src/main/java/**/biz/service/` — Service layer patterns, handler/checker approach
   - `src/main/java/**/exception/` — GenericExceptionHandler pattern

**When implementing a new phase, read the relevant files from BOTH reference projects to match their production patterns.**

## Project Overview

A Spring Boot REST API for managing Estonian football clubs — members, training schedules, field bookings, attendance tracking, and internal notifications with role-based access control.

**Thesis:** "Eesti jalgpalliklubide haldamine veebipõhise rakenduse abil" (TalTech, 2026)

## Development Plan

See `.claude/development-plan.md` for the full phased development plan, database model, API design, and RBAC matrix.

## Tech Stack

- **Java 21** / **Spring Boot 3.5.x** / **Gradle Kotlin DSL**
- **PostgreSQL 17** with **Liquibase** migrations
- **Spring Security 6** + **JWT** authentication
- **MapStruct** for entity ↔ DTO mapping
- **Lombok** for boilerplate reduction
- **SpringDoc OpenAPI** for API documentation
- **Docker Compose** for local development
- **JUnit 5** + **TestContainers** + **ArchUnit** for testing
- **Spotless** (Google Java Format) for code formatting

## Architecture

Enforced layered architecture (follows emde-2-be patterns):

```
config              → Spring configuration classes
security            → Authentication, authorization, JWT, RBAC
api/controller      → REST controllers (thin, delegates to services)
service             → Business logic, @Transactional boundaries
service/dto         → Data Transfer Objects
service/mapper      → MapStruct mappers (Entity ↔ DTO)
repository          → Spring Data JPA repositories
domain              → JPA entities, base classes
domain/enumeration  → Enum types
domain/converter    → JPA AttributeConverters for enums
common/exception    → Custom exceptions, ExceptionTranslator
common/validation   → Custom business validators
common/util         → Shared utilities
```

Base package: `ee.finalthesis.clubmanagement`

## Key Conventions

### Code Style
- Google Java Format (enforced by Spotless on build)
- Lombok for getters/setters/constructors/builders
- MapStruct for all entity ↔ DTO conversions (no manual mapping)
- Jakarta Validation annotations on DTOs (`@Valid`, `@NotBlank`, `@Size`)

### Naming
- Entities: PascalCase (`TrainingSession`)
- DB tables: snake_case (Hibernate auto-converts)
- REST endpoints: kebab-case (`/api/clubs/{clubId}/training-sessions`)
- Classes: `*Controller`, `*Service`, `*Repository`, `*DTO`, `*Mapper`

### Database
- UUID primary keys
- All schema managed by Liquibase (ddl-auto: none)
- Changelogs: `src/main/resources/config/liquibase/changelog/YYYYMMDDHHMMSS_description.xml`
- Timezone: Europe/Tallinn
- Base entity: `AbstractAuditingEntity` (createdAt, updatedAt, createdBy, lastModifiedBy)

### REST API
- Base path: `/api`
- Resources nested under club: `/api/clubs/{clubId}/teams`, `/api/clubs/{clubId}/trainings`
- Error responses: RFC 7807 Problem Details
- Pagination: Spring Data Pageable (page, size, sort params)
- All endpoints require JWT except `/api/auth/login`

### Security / RBAC
- 4 roles: `ADMIN`, `COACH`, `PLAYER`, `PARENT`
- JWT in `Authorization: Bearer <token>` header
- `@PreAuthorize` on controller methods
- Coach sees/manages only own teams
- Parent confirms attendance for own child only
- bcrypt password hashing

### Testing
- Integration tests (`*IT.java`): `@SpringBootTest` + TestContainers PostgreSQL
- Unit tests (`*Test.java`): Mockito
- Architecture tests: ArchUnit (enforces layer boundaries)
- Never mock the database in integration tests — use real PostgreSQL

### Error Handling
- `ExceptionTranslator` as `@ControllerAdvice`
- `BadRequestException` → 400
- `ResourceNotFoundException` → 404
- `ConflictException` → 409
- Spring Security handles 401/403 automatically

## Commands

```bash
# Start PostgreSQL
docker compose up -d

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Build
./gradlew build
```

## Frontend

The React frontend for this project is at: `/Users/igorustritski/club-managment-fe`

## Reference Projects

Patterns borrowed from these production projects:
- **emde-2-be**: ArchUnit enforcement, Liquibase conventions, ExceptionTranslator, AbstractAuditingEntity, Spotless config, SecurityConfiguration patterns
- **wallet**: JWT auth flow, layered service architecture, DTO patterns, handler/checker patterns for conditional logic
