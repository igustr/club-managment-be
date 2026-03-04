# Football Club Management Backend - Development Plan

> Based on bachelor's thesis "Eesti jalgpalliklubide haldamine veebipõhise rakenduse abil"
> Reference projects: emde-2-be, wallet

---

## Technology Stack (Final)

| Component          | Technology                          | Source/Rationale                          |
|--------------------|-------------------------------------|-------------------------------------------|
| Language           | Java 21                             | emde-2-be pattern, LTS                    |
| Framework          | Spring Boot 3.5.x                   | Thesis TOA result (score 100/100)         |
| Build              | Gradle 8.x (Kotlin DSL)            | emde-2-be pattern                         |
| Database           | PostgreSQL 17                       | Thesis TOA result (score 90/90)           |
| Migrations         | Liquibase                           | emde-2-be pattern (structured changelogs) |
| ORM                | Hibernate 6 + JPA 3.1               | Both reference projects                   |
| Mapping            | MapStruct 1.5.x                     | Both reference projects                   |
| Auth               | Spring Security 6 + JWT             | Thesis req + wallet pattern               |
| Validation         | Jakarta Validation (Bean Validation) | Both reference projects                   |
| API Docs           | SpringDoc OpenAPI 2.x               | Both reference projects                   |
| Code Quality       | Spotless (Google Java Format)       | Both reference projects                   |
| Testing            | JUnit 5 + TestContainers + ArchUnit | emde-2-be pattern                         |
| Boilerplate        | Lombok                              | Both reference projects                   |
| Containerization   | Docker Compose                      | Both reference projects                   |

---

## Architecture

### Layered Architecture (enforced by ArchUnit)

```
config          → Spring configuration classes
security        → Authentication, authorization, JWT, RBAC
api/controller  → REST controllers (thin, delegates to services)
service         → Business logic, @Transactional boundaries
service/dto     → Data Transfer Objects
service/mapper  → MapStruct mappers (Entity ↔ DTO)
repository      → Spring Data JPA repositories
domain          → JPA entities, enums, base classes
common          → Shared utilities, exceptions, constants
```

### Package Structure

```
ee.taltech.clubmanagement
├── ClubManagementApplication.java
├── api/
│   └── controller/
│       ├── AuthController.java
│       ├── ClubController.java
│       ├── TeamController.java
│       ├── MemberController.java
│       ├── TrainingSessionController.java
│       ├── FieldController.java
│       ├── AttendanceController.java
│       └── NotificationController.java
├── config/
│   ├── SecurityConfiguration.java
│   ├── JacksonConfiguration.java
│   ├── OpenApiConfiguration.java
│   ├── WebConfig.java
│   └── DatabaseConfiguration.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityUtils.java
│   ├── AppRole.java (enum)
│   └── UserDetailsServiceImpl.java
├── service/
│   ├── AuthService.java
│   ├── ClubService.java
│   ├── TeamService.java
│   ├── MemberService.java
│   ├── TrainingSessionService.java
│   ├── FieldService.java
│   ├── AttendanceService.java
│   ├── NotificationService.java
│   ├── dto/
│   │   ├── auth/
│   │   ├── club/
│   │   ├── team/
│   │   ├── member/
│   │   ├── training/
│   │   ├── field/
│   │   ├── attendance/
│   │   └── notification/
│   └── mapper/
│       ├── ClubMapper.java
│       ├── TeamMapper.java
│       ├── MemberMapper.java
│       ├── TrainingSessionMapper.java
│       ├── FieldMapper.java
│       └── NotificationMapper.java
├── repository/
│   ├── UserRepository.java
│   ├── ClubRepository.java
│   ├── TeamRepository.java
│   ├── MemberRepository.java
│   ├── TeamMemberRepository.java
│   ├── TrainingSessionRepository.java
│   ├── FieldRepository.java
│   ├── AttendanceRepository.java
│   └── NotificationRepository.java
├── domain/
│   ├── AbstractAuditingEntity.java
│   ├── User.java
│   ├── Club.java
│   ├── Team.java
│   ├── Member.java
│   ├── TeamMember.java
│   ├── TrainingSession.java
│   ├── Field.java
│   ├── Attendance.java
│   ├── Notification.java
│   ├── NotificationRecipient.java
│   └── enumeration/
│       ├── AppRole.java
│       ├── AttendanceStatus.java
│       ├── NotificationType.java
│       ├── RecipientGroup.java
│       └── DayOfWeek.java
└── common/
    ├── exception/
    │   ├── BadRequestException.java
    │   ├── ResourceNotFoundException.java
    │   ├── ConflictException.java
    │   └── ExceptionTranslator.java (ControllerAdvice)
    └── util/
        └── SecurityUtils.java
```

---

## Database Model (Entities & Relationships)

### Core Entities

```
User (auth)
├── id: UUID (PK)
├── email: String (unique)
├── password_hash: String
├── first_name, last_name: String
├── phone: String
├── role: AppRole (ADMIN, COACH, PLAYER, PARENT)
├── club_id: FK → Club
├── active: boolean
└── audit fields (created_at, updated_at)

Club
├── id: UUID (PK)
├── name: String
├── registration_code: String
├── address: String
├── contact_email, contact_phone: String
└── audit fields

Team
├── id: UUID (PK)
├── name: String (e.g., "U-12", "U-15", "Esindus")
├── club_id: FK → Club
├── coach_id: FK → User (role=COACH)
├── age_group: String
├── season: String
└── audit fields

Member (player or person in a team context)
├── id: UUID (PK)
├── user_id: FK → User (nullable, may not have login)
├── club_id: FK → Club
├── first_name, last_name: String
├── date_of_birth: LocalDate
├── email: String
├── phone: String
├── parent_id: FK → User (role=PARENT, for minors)
└── audit fields

TeamMember (join table)
├── id: UUID (PK)
├── team_id: FK → Team
├── member_id: FK → Member
├── joined_date: LocalDate
├── active: boolean
└── audit fields

Field (training ground / pitch)
├── id: UUID (PK)
├── name: String
├── address: String
├── club_id: FK → Club
├── surface_type: String (grass, artificial, indoor)
├── capacity: Integer
└── audit fields

TrainingSession
├── id: UUID (PK)
├── team_id: FK → Team
├── field_id: FK → Field
├── date: LocalDate
├── start_time: LocalTime
├── end_time: LocalTime
├── recurring: boolean
├── recurrence_day: DayOfWeek (if recurring)
├── recurrence_end_date: LocalDate (if recurring)
├── notes: String
├── cancelled: boolean
└── audit fields

Attendance
├── id: UUID (PK)
├── training_session_id: FK → TrainingSession
├── member_id: FK → Member
├── status: AttendanceStatus (CONFIRMED, DECLINED, PENDING)
├── confirmed_by_user_id: FK → User (parent or self)
├── confirmed_at: Instant
└── audit fields

Notification
├── id: UUID (PK)
├── club_id: FK → Club
├── team_id: FK → Team (nullable, club-wide if null)
├── sender_id: FK → User
├── title: String
├── content: String
├── notification_type: NotificationType (GENERAL, TRAINING_CHANGE, ATTENDANCE_REQUEST)
├── recipient_group: RecipientGroup (ALL, PLAYERS_ONLY, PARENTS_ONLY)
├── sent_at: Instant
└── audit fields

NotificationRecipient
├── id: UUID (PK)
├── notification_id: FK → Notification
├── user_id: FK → User
├── read: boolean
├── read_at: Instant
└── audit fields
```

### Key Relationships
- Club 1:N Teams
- Club 1:N Members
- Club 1:N Fields
- Club 1:N Users
- Team N:1 Club
- Team N:1 User (coach)
- Team N:M Members (via TeamMember)
- TrainingSession N:1 Team
- TrainingSession N:1 Field
- Attendance N:1 TrainingSession
- Attendance N:1 Member
- Notification N:1 Team
- Notification N:1 User (sender)
- NotificationRecipient N:1 Notification
- NotificationRecipient N:1 User
- Member N:1 User (parent, for minors)

---

## REST API Design

### Base path: `/api`

### Authentication
```
POST   /api/auth/register          # Register new user (admin creates users)
POST   /api/auth/login             # Login, returns JWT
POST   /api/auth/refresh           # Refresh JWT token
GET    /api/auth/me                # Get current user profile
```

### Club Management (ADMIN only)
```
GET    /api/clubs/{clubId}                 # Get club details
PUT    /api/clubs/{clubId}                 # Update club details
```

### Team Management
```
GET    /api/clubs/{clubId}/teams                    # List teams (ADMIN: all, COACH: own)
POST   /api/clubs/{clubId}/teams                    # Create team (ADMIN)
GET    /api/clubs/{clubId}/teams/{teamId}            # Get team details
PUT    /api/clubs/{clubId}/teams/{teamId}            # Update team (ADMIN)
DELETE /api/clubs/{clubId}/teams/{teamId}            # Delete team (ADMIN)
GET    /api/clubs/{clubId}/teams/{teamId}/members     # List team members (ADMIN, COACH of team)
POST   /api/clubs/{clubId}/teams/{teamId}/members     # Add member to team (ADMIN, COACH)
DELETE /api/clubs/{clubId}/teams/{teamId}/members/{memberId}  # Remove from team
```

### Member Management
```
GET    /api/clubs/{clubId}/members                  # List all members (ADMIN: all, COACH: own teams)
POST   /api/clubs/{clubId}/members                  # Register new member (ADMIN)
GET    /api/clubs/{clubId}/members/{memberId}        # Get member details
PUT    /api/clubs/{clubId}/members/{memberId}        # Update member (ADMIN)
DELETE /api/clubs/{clubId}/members/{memberId}        # Deactivate member (ADMIN)
```

### Field Management
```
GET    /api/clubs/{clubId}/fields                   # List fields
POST   /api/clubs/{clubId}/fields                   # Create field (ADMIN)
GET    /api/clubs/{clubId}/fields/{fieldId}          # Get field details
PUT    /api/clubs/{clubId}/fields/{fieldId}          # Update field (ADMIN)
DELETE /api/clubs/{clubId}/fields/{fieldId}          # Delete field (ADMIN)
GET    /api/clubs/{clubId}/fields/{fieldId}/schedule  # Get field usage schedule
```

### Training Session Management
```
GET    /api/clubs/{clubId}/trainings                         # List trainings (filtered by role)
POST   /api/clubs/{clubId}/teams/{teamId}/trainings           # Create training (ADMIN, COACH)
GET    /api/clubs/{clubId}/trainings/{trainingId}             # Get training details
PUT    /api/clubs/{clubId}/trainings/{trainingId}             # Update training (ADMIN, COACH)
DELETE /api/clubs/{clubId}/trainings/{trainingId}             # Cancel training (ADMIN, COACH)
POST   /api/clubs/{clubId}/teams/{teamId}/trainings/recurring  # Create recurring training
```

### Attendance
```
GET    /api/clubs/{clubId}/trainings/{trainingId}/attendance           # Get attendance list
PUT    /api/clubs/{clubId}/trainings/{trainingId}/attendance/{memberId} # Confirm/decline (PLAYER, PARENT)
GET    /api/clubs/{clubId}/trainings/{trainingId}/attendance/summary    # Attendance summary (COACH)
```

### Notifications
```
GET    /api/clubs/{clubId}/notifications                    # List notifications (filtered by role)
POST   /api/clubs/{clubId}/teams/{teamId}/notifications      # Send notification (COACH, ADMIN)
GET    /api/clubs/{clubId}/notifications/{notificationId}    # Get notification detail
PUT    /api/clubs/{clubId}/notifications/{notificationId}/read # Mark as read
```

---

## RBAC (Role-Based Access Control)

### Roles & Permissions (from thesis section 4.7.3)

| Action                              | ADMIN | COACH | PLAYER | PARENT |
|--------------------------------------|-------|-------|--------|--------|
| Manage club settings                | Yes   | No    | No     | No     |
| Manage all teams                    | Yes   | No    | No     | No     |
| Manage own team roster              | Yes   | Yes   | No     | No     |
| Create/edit all users               | Yes   | No    | No     | No     |
| Manage fields                       | Yes   | No    | No     | No     |
| View field schedule (all teams)     | Yes   | No    | No     | No     |
| Create training (own team)          | Yes   | Yes   | No     | No     |
| Edit/cancel training (own team)     | Yes   | Yes   | No     | No     |
| View own team trainings             | Yes   | Yes   | Yes    | Yes    |
| Send notification (own team)        | Yes   | Yes   | No     | No     |
| Send to players only / parents only | Yes   | Yes   | No     | No     |
| View own team notifications         | Yes   | Yes   | Yes    | Yes    |
| Confirm attendance (self)           | No    | No    | Yes    | No     |
| Confirm attendance (child)          | No    | No    | No     | Yes    |
| View attendance summary             | Yes   | Yes   | No     | No     |

### Implementation approach
- Spring Security `@PreAuthorize` annotations on controller methods
- Custom `ClubMembershipChecker` to verify user belongs to club
- Custom `TeamAccessChecker` to verify coach owns the team
- JWT token contains: userId, email, role, clubId

---

## Development Phases

### Phase 0: Project Bootstrap
**Goal:** Transform bare Gradle project into a fully configured Spring Boot application

- [ ] Configure `build.gradle.kts` with all dependencies (Spring Boot, Security, JPA, PostgreSQL, MapStruct, Lombok, Liquibase, SpringDoc, Spotless, TestContainers, ArchUnit)
- [ ] Create Spring Boot main application class
- [ ] Set up `application.yaml` (base), `application-dev.yml`, `application-prod.yml`
- [ ] Set up `docker-compose.yaml` with PostgreSQL 17
- [ ] Create base package structure (`ee.taltech.clubmanagement.*`)
- [ ] Configure Spotless (Google Java Format)
- [ ] Create `AbstractAuditingEntity` base class with audit fields
- [ ] Set up Liquibase master changelog
- [ ] Verify application starts and connects to DB

### Phase 1: Authentication & User Management
**Goal:** JWT-based auth with RBAC, user CRUD

- [ ] Create `User` entity with role enum (ADMIN, COACH, PLAYER, PARENT)
- [ ] Liquibase migration for `users` table
- [ ] Configure Spring Security (SecurityConfiguration)
- [ ] Implement JWT token provider (generate, validate, parse)
- [ ] Implement JWT authentication filter
- [ ] Create `UserDetailsServiceImpl`
- [ ] Create auth DTOs (LoginRequest, RegisterRequest, AuthResponse, UserDTO)
- [ ] Create `AuthController` (login, register, me, refresh)
- [ ] Create `AuthService`
- [ ] Create `UserMapper` (MapStruct)
- [ ] Seed default admin user via Liquibase
- [ ] Configure CORS
- [ ] Create `ExceptionTranslator` (@ControllerAdvice, RFC 7807)
- [ ] Write integration tests for auth endpoints

### Phase 2: Club & Team Management
**Goal:** Club structure, team CRUD, team roster management

- [ ] Create `Club` entity
- [ ] Create `Team` entity with coach relationship
- [ ] Create `Member` entity with parent relationship
- [ ] Create `TeamMember` join entity
- [ ] Liquibase migrations for clubs, teams, members, team_members
- [ ] Create DTOs (ClubDTO, TeamDTO, MemberDTO, TeamMemberDTO)
- [ ] Create MapStruct mappers
- [ ] Create repositories
- [ ] Create `ClubService`, `TeamService`, `MemberService`
- [ ] Create `ClubController`, `TeamController`, `MemberController`
- [ ] Implement RBAC: admin sees all, coach sees own teams only
- [ ] Write integration tests

### Phase 3: Field & Training Session Management
**Goal:** Field CRUD, training scheduling with recurrence and conflict detection

- [ ] Create `Field` entity
- [ ] Create `TrainingSession` entity with recurrence support
- [ ] Liquibase migrations for fields, training_sessions
- [ ] Create DTOs
- [ ] Create MapStruct mappers
- [ ] Create repositories with custom queries (field availability, schedule overlap)
- [ ] Create `FieldService` with availability checking
- [ ] Create `TrainingSessionService` with:
  - Single training creation
  - Recurring training generation (weekly, until end date)
  - Conflict detection (field double-booking)
  - Training update/cancellation with notification trigger
- [ ] Create `FieldController`, `TrainingSessionController`
- [ ] Implement field schedule view (all teams, for admin coordination)
- [ ] Write integration tests

### Phase 4: Attendance Management
**Goal:** Training attendance confirmation by players and parents

- [ ] Create `Attendance` entity
- [ ] Liquibase migration for attendance table
- [ ] Create DTOs (AttendanceDTO, AttendanceSummaryDTO)
- [ ] Create `AttendanceService` with:
  - Auto-create PENDING attendance records when training is created
  - Confirm/decline by player (self) or parent (for child)
  - Attendance summary for coach
- [ ] Create `AttendanceController`
- [ ] RBAC: player confirms own, parent confirms child's
- [ ] Write integration tests

### Phase 5: Notification System
**Goal:** Group-based notifications with recipient filtering

- [ ] Create `Notification` entity
- [ ] Create `NotificationRecipient` entity
- [ ] Liquibase migrations
- [ ] Create DTOs
- [ ] Create `NotificationService` with:
  - Send to entire team
  - Send to players only
  - Send to parents only
  - Auto-notification on training changes
  - Mark as read
- [ ] Create `NotificationController`
- [ ] RBAC: coach sends to own team only, sees own team messages only
- [ ] Write integration tests

### Phase 6: Testing & Quality
**Goal:** Comprehensive test suite and code quality checks

- [ ] ArchUnit test (enforce layered architecture, like emde-2-be)
- [ ] Integration tests for all controllers (TestContainers PostgreSQL)
- [ ] Unit tests for services (business logic)
- [ ] Security tests (unauthorized access, role escalation)
- [ ] Spotless verification (CI-ready)
- [ ] OpenAPI spec generation and validation
- [ ] Seed realistic test data via Liquibase (dev/faker context)

### Phase 7: Docker & DevOps
**Goal:** One-command local setup, CI-ready

- [ ] Finalize `docker-compose.yaml` (PostgreSQL + app)
- [ ] Create Dockerfile for Spring Boot app
- [ ] Document startup instructions in README
- [ ] Environment-based configuration (dev/prod profiles)

---

## Conventions (from reference projects)

### Naming
- **Entities**: PascalCase (`TrainingSession`, `TeamMember`)
- **Tables**: snake_case (auto-converted by Hibernate)
- **REST endpoints**: kebab-case (`/api/clubs/{clubId}/training-sessions`)
- **Services**: `*Service.java`
- **Repositories**: `*Repository.java`
- **DTOs**: `*DTO.java`
- **Mappers**: `*Mapper.java`
- **Controllers**: `*Controller.java`

### Error Handling (from emde-2-be pattern)
- `ExceptionTranslator` as `@ControllerAdvice`
- RFC 7807 Problem Details format
- Custom exceptions: `BadRequestException`, `ResourceNotFoundException`, `ConflictException`
- Field validation errors in `fieldErrors` array

### Database
- UUID primary keys (using `@GeneratedValue(strategy = GenerationType.UUID)`)
- Liquibase changelogs in `src/main/resources/config/liquibase/changelog/`
- Timestamp naming: `YYYYMMDDHHMMSS_description.xml`
- `ddl-auto: none` (Liquibase manages all schema)
- Timezone: Europe/Tallinn

### Security
- JWT in Authorization header: `Bearer <token>`
- bcrypt password hashing
- CORS configured for frontend origin
- Input validation on DTOs (@Valid, @NotBlank, @Size, etc.)
- All endpoints require authentication except `/api/auth/login`

### Testing
- Integration tests: `*IT.java` with `@SpringBootTest` + TestContainers
- Unit tests: `*Test.java` with Mockito
- Architecture tests: ArchUnit enforcing layer dependencies
- Test profile with isolated PostgreSQL container

---

## Current Status

**Phase:** 0 (Project Bootstrap) — NOT STARTED
**Next action:** Configure build.gradle.kts with Spring Boot dependencies
