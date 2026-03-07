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
config              → Spring configuration classes
security            → Authentication, authorization, JWT, RBAC
api/controller      → REST controllers (thin, delegates to services)
service             → Business logic, @Transactional boundaries
service/dto         → Data Transfer Objects
service/mapper      → MapStruct mappers (Entity ↔ DTO)
repository          → Spring Data JPA repositories
domain              → JPA entities, base classes
domain/enumeration  → Enum types (ClubRole, AttendanceStatus, etc.)
domain/converter    → JPA AttributeConverters for enums
common/exception    → Custom exceptions, ExceptionTranslator
common/validation   → Custom business validators
common/util         → Shared utilities
```

### Package Structure

```
ee.finalthesis.clubmanagement
├── ClubManagementApplication.java
├── api/
│   └── controller/
│       ├── AuthController.java
│       ├── ClubController.java
│       ├── TeamController.java
│       ├── UserController.java
│       ├── TrainingSessionController.java
│       ├── PitchController.java
│       ├── AttendanceController.java
│       └── ConversationController.java
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
│   └── UserDetailsServiceImpl.java
├── service/
│   ├── AuthService.java
│   ├── ClubService.java
│   ├── TeamService.java
│   ├── UserService.java
│   ├── TrainingSessionService.java
│   ├── PitchService.java
│   ├── AttendanceService.java
│   ├── ConversationService.java
│   ├── MessageService.java
│   ├── dto/
│   │   ├── auth/
│   │   ├── club/
│   │   ├── team/
│   │   ├── user/
│   │   ├── training/
│   │   ├── pitch/
│   │   ├── attendance/
│   │   └── chat/
│   └── mapper/
│       ├── ClubMapper.java
│       ├── TeamMapper.java
│       ├── UserMapper.java
│       ├── TrainingSessionMapper.java
│       ├── PitchMapper.java
│       ├── ConversationMapper.java
│       └── MessageMapper.java
├── repository/
│   ├── UserRepository.java
│   ├── ClubRepository.java
│   ├── TeamRepository.java
│   ├── TeamMemberRepository.java
│   ├── TrainingSessionRepository.java
│   ├── PitchRepository.java
│   ├── AttendanceRepository.java
│   ├── ConversationRepository.java
│   ├── ConversationParticipantRepository.java
│   ├── MessageRepository.java
│   └── ConversationReadStatusRepository.java
├── domain/
│   ├── AbstractAuditingEntity.java
│   ├── User.java
│   ├── Club.java
│   ├── Team.java
│   ├── TeamMember.java
│   ├── TrainingSession.java
│   ├── Pitch.java
│   ├── Attendance.java
│   ├── Conversation.java
│   ├── ConversationParticipant.java
│   ├── Message.java
│   ├── ConversationReadStatus.java
│   ├── enumeration/
│   │   ├── ClubRole.java
│   │   ├── AttendanceStatus.java
│   │   ├── TrainingSessionStatus.java
│   │   └── ConversationType.java
│   └── converter/          (JPA AttributeConverters for enums)
└── common/
    ├── exception/
    │   ├── BadRequestException.java
    │   ├── ResourceNotFoundException.java
    │   ├── ConflictException.java
    │   └── ExceptionTranslator.java (ControllerAdvice)
    ├── validation/          (custom business validators, e.g. pitch booking conflicts)
    └── util/
        └── SecurityUtils.java
```

---

## Database Model (Entities & Relationships)

### Design Rationale

**User = single entity** for both authentication and personal data. No separate Member table —
all club members (players, coaches, parents) are Users. One user = one role = one account.
If a person is both a coach and a player, they use two accounts (e.g. work email for coaching,
personal email for playing). TeamMember is a simple join table (team_id, user_id) with no role —
the user's ClubRole already determines their function. Recurrence is modeled via a shared
`recurrence_group_id` rather than per-row metadata, enabling clean batch operations on recurring
training series.

### Auth Flow

1. User self-registers (email, password, personal info) → account with no club, no role
2. Club admin searches for unaffiliated users and adds them to club with a ClubRole
3. Admin or coach assigns users to teams via TeamMember

### Core Entities

```
User (single entity — auth + personal data + club role)
├── id: UUID (PK)
├── email: String (UNIQUE, used for login)
├── password_hash: String
├── first_name: String
├── last_name: String
├── date_of_birth: LocalDate
├── phone: String
├── photo_url: String (nullable — profile photo URL)
├── role: ClubRole (ADMIN, COACH, PLAYER, PARENT) — nullable until admin assigns
├── club_id: FK → Club (nullable — null until added to a club)
├── parents: Set<User> via user_parent join table (ManyToMany, for minors — multiple parents supported)
├── children: Set<User> inverse side of parents (ManyToMany mappedBy)
├── active: boolean (default true)
└── audit fields (created_at, updated_at, created_by, last_modified_by)

user_parent (join table for parent-child relationships)
├── child_id: FK → User (PK part)
├── parent_id: FK → User (PK part)
├── composite PK (child_id, parent_id)

Club
├── id: UUID (PK)
├── name: String
├── registration_code: String (nullable)
├── address: String (nullable)
├── contact_email: String
├── contact_phone: String (nullable)
└── audit fields

Team
├── id: UUID (PK)
├── name: String (e.g. "U-12", "U-15", "Esindus")
├── club_id: FK → Club
├── age_group: String (nullable)
├── season: String (nullable)
└── audit fields

TeamMember (join table — row exists = user is in team, no role needed)
├── id: UUID (PK)
├── team_id: FK → Team
├── user_id: FK → User
├── joined_date: LocalDate (nullable)
├── UNIQUE(team_id, user_id)
└── audit fields

Pitch (training ground / football pitch)
├── id: UUID (PK)
├── name: String
├── address: String (nullable)
├── club_id: FK → Club
├── surface_type: String (nullable — grass, artificial, indoor)
├── capacity: Integer (nullable)
└── audit fields

TrainingSession
├── id: UUID (PK)
├── team_id: FK → Team
├── pitch_id: FK → Pitch (nullable — pitch may be TBD when scheduling)
├── date: LocalDate
├── start_time: LocalTime
├── end_time: LocalTime
├── recurrence_group_id: UUID (nullable — shared ID linking all sessions in a recurring series)
├── status: TrainingSessionStatus (SCHEDULED, CANCELLED)
├── notes: String (nullable)
└── audit fields

Attendance
├── id: UUID (PK)
├── training_session_id: FK → TrainingSession
├── user_id: FK → User
├── status: AttendanceStatus (PENDING, CONFIRMED, DECLINED)
├── UNIQUE(training_session_id, user_id)
└── audit fields

Conversation (chat room — team or direct)
├── id: UUID (PK)
├── type: ConversationType (DIRECT, TEAM)
├── team_id: FK → Team (nullable, only for TEAM type)
├── club_id: FK → Club
├── last_message_text: String (nullable, denormalized for preview)
├── last_message_time: Instant (nullable, denormalized)
├── last_message_sender_id: FK → User (nullable, denormalized)
└── audit fields

ConversationParticipant (who is in the chat)
├── id: UUID (PK)
├── conversation_id: FK → Conversation
├── user_id: FK → User
├── UNIQUE(conversation_id, user_id)
└── audit fields

Message
├── id: UUID (PK)
├── conversation_id: FK → Conversation
├── sender_id: FK → User
├── text: String (TEXT)
├── created_at: Instant
└── audit fields

ConversationReadStatus (unread tracking)
├── id: UUID (PK)
├── conversation_id: FK → Conversation
├── user_id: FK → User
├── unread_count: int (default 0)
├── last_read_at: Instant (nullable)
├── UNIQUE(conversation_id, user_id)
└── audit fields
```

### Enumerations

```
ClubRole:             ADMIN, COACH, PLAYER, PARENT
TrainingSessionStatus: SCHEDULED, CANCELLED
AttendanceStatus:     PENDING, CONFIRMED, DECLINED
ConversationType:     DIRECT, TEAM
```

### Key Relationships
- Club 1:N Teams
- Club 1:N Pitches
- Club 1:N Users
- Team N:1 Club
- Team N:M Users (via TeamMember join table, UNIQUE constraint)
- TrainingSession N:1 Team
- TrainingSession N:1 Pitch (nullable)
- Attendance N:1 TrainingSession (UNIQUE with user_id)
- Attendance N:1 User
- Conversation N:1 Club
- Conversation N:1 Team (nullable, for team chats)
- Conversation N:M Users (via ConversationParticipant)
- Message N:1 Conversation
- Message N:1 User (sender)
- ConversationReadStatus N:1 Conversation
- ConversationReadStatus N:1 User
- User N:M User (parents/children via user_parent join table)

### Key Indexes (for Liquibase migrations)
- `users.email` — UNIQUE
- `users.club_id` — FK index
- `team_member(team_id, user_id)` — UNIQUE composite
- `training_sessions.team_id` — FK index
- `training_sessions.pitch_id` — FK index
- `training_sessions.date` — for schedule queries
- `training_sessions.recurrence_group_id` — for batch operations on series
- `attendance(training_session_id, user_id)` — UNIQUE composite
- `conversation_participant(conversation_id, user_id)` — UNIQUE composite
- `message.conversation_id` — FK index for message queries
- `message.created_at` — for chronological ordering
- `conversation_read_status(conversation_id, user_id)` — UNIQUE composite

---

## REST API Design

### Base path: `/api`

### Authentication
```
POST   /api/auth/register          # Self-register (no club, no role)
POST   /api/auth/login             # Login, returns JWT
POST   /api/auth/refresh           # Refresh JWT token
GET    /api/auth/me                # Get current user profile
```

### Club Management (ADMIN only)
```
GET    /api/clubs/{clubId}                 # Get club details
PUT    /api/clubs/{clubId}                 # Update club details
```

### User Management (ADMIN manages club members)
```
GET    /api/clubs/{clubId}/users                   # List club users (ADMIN: all, COACH: own teams)
POST   /api/clubs/{clubId}/users                   # Add user to club and assign role (ADMIN)
GET    /api/clubs/{clubId}/users/{userId}           # Get user details
PUT    /api/clubs/{clubId}/users/{userId}           # Update user (ADMIN)
DELETE /api/clubs/{clubId}/users/{userId}           # Remove user from club (ADMIN)
GET    /api/users/unaffiliated                      # Search users without a club (ADMIN)
```

### Team Management
```
GET    /api/clubs/{clubId}/teams                    # List teams (ADMIN: all, COACH: own)
POST   /api/clubs/{clubId}/teams                    # Create team (ADMIN)
GET    /api/clubs/{clubId}/teams/{teamId}            # Get team details
PUT    /api/clubs/{clubId}/teams/{teamId}            # Update team (ADMIN)
DELETE /api/clubs/{clubId}/teams/{teamId}            # Delete team (ADMIN)
GET    /api/clubs/{clubId}/teams/{teamId}/members     # List team members (ADMIN, COACH of team)
POST   /api/clubs/{clubId}/teams/{teamId}/members     # Add user to team (ADMIN, COACH)
DELETE /api/clubs/{clubId}/teams/{teamId}/members/{userId}  # Remove from team
```

### Pitch Management
```
GET    /api/clubs/{clubId}/pitches                    # List pitches
POST   /api/clubs/{clubId}/pitches                    # Create pitch (ADMIN)
GET    /api/clubs/{clubId}/pitches/{pitchId}           # Get pitch details
PUT    /api/clubs/{clubId}/pitches/{pitchId}           # Update pitch (ADMIN)
DELETE /api/clubs/{clubId}/pitches/{pitchId}           # Delete pitch (ADMIN)
GET    /api/clubs/{clubId}/pitches/{pitchId}/schedule  # Get pitch usage schedule
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
PUT    /api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}  # Confirm/decline (PLAYER, PARENT)
GET    /api/clubs/{clubId}/trainings/{trainingId}/attendance/summary    # Attendance summary (COACH)
```

### Chat
```
GET    /api/clubs/{clubId}/conversations                        # List my conversations
POST   /api/clubs/{clubId}/conversations                        # Create direct conversation
GET    /api/clubs/{clubId}/conversations/{conversationId}/messages  # Get messages (paginated)
POST   /api/clubs/{clubId}/conversations/{conversationId}/messages  # Send message
PUT    /api/clubs/{clubId}/conversations/{conversationId}/read      # Mark as read
GET    /api/clubs/{clubId}/conversations/unread-count               # Total unread count
```

---

## RBAC (Role-Based Access Control)

### Roles & Permissions (from thesis section 4.7.3)

| Action                              | ADMIN | COACH | PLAYER | PARENT |
|--------------------------------------|-------|-------|--------|--------|
| Manage club settings                | Yes   | No    | No     | No     |
| Manage all teams                    | Yes   | No    | No     | No     |
| Manage own team roster              | Yes   | Yes   | No     | No     |
| Add/remove users to club            | Yes   | No    | No     | No     |
| Manage pitches                      | Yes   | No    | No     | No     |
| View pitch schedule (all teams)     | Yes   | No    | No     | No     |
| Create training (own team)          | Yes   | Yes   | No     | No     |
| Edit/cancel training (own team)     | Yes   | Yes   | No     | No     |
| View own team trainings             | Yes   | Yes   | Yes    | Yes    |
| Send message in team chat           | Yes   | Yes   | Yes    | Yes    |
| Send direct message                 | Yes   | Yes   | Yes    | Yes    |
| View own conversations              | Yes   | Yes   | Yes    | Yes    |
| Confirm attendance (self)           | No    | No    | Yes    | No     |
| Confirm attendance (child)          | No    | No    | No     | Yes    |
| View attendance summary             | Yes   | Yes   | No     | No     |

### Implementation approach
- Spring Security `@PreAuthorize` annotations on controller methods
- Custom `ClubMembershipChecker` to verify user belongs to club
- Custom `TeamAccessChecker` to verify coach is in the team
- JWT token contains: userId, email, role, clubId

---

## Development Phases

### Phase 0: Project Bootstrap
**Goal:** Transform bare Gradle project into a fully configured Spring Boot application

- [x] Configure `build.gradle.kts` with all dependencies (Spring Boot, Security, JPA, PostgreSQL, MapStruct, Lombok, Liquibase, SpringDoc, Spotless, TestContainers, ArchUnit)
- [x] Create Spring Boot main application class
- [x] Set up `application.yaml` (base), `application-dev.yml`, `application-prod.yml`
- [x] Set up `docker-compose.yaml` with PostgreSQL 17
- [x] Create base package structure (`ee.finalthesis.clubmanagement.*`)
- [x] Configure Spotless (Google Java Format)
- [x] Create `AbstractAuditingEntity` base class with audit fields
- [x] Set up Liquibase master changelog
- [x] Create all domain entities and Liquibase migrations
- [x] Verify application starts and connects to DB

### Phase 1: Authentication & User Management
**Goal:** JWT-based auth with RBAC, user CRUD

- [x] Add jjwt dependencies to `build.gradle.kts`
- [x] Add JWT config properties to `application.yml` (secret, token validity 24h, refresh 30d)
- [x] Create `SecurityProperties` (@ConfigurationProperties for JWT config)
- [x] Create `CorsProperties` (@ConfigurationProperties for CORS)
- [x] Create `UserRepository` (findByEmail, existsByEmail)
- [x] Create `TeamMemberRepository` (existsByTeamIdAndUserId)
- [x] Create `UserPrincipal` (custom UserDetails with id, email, role, clubId)
- [x] Create `JwtTokenProvider` (generate access/refresh tokens, validate, parse)
- [x] Create `JwtAuthenticationFilter` (OncePerRequestFilter with HandlerExceptionResolver — wallet pattern)
- [x] Create `UserDetailsServiceImpl` (load by email, check active status)
- [x] Create `SecurityUtils` (getCurrentUserId, getCurrentUserEmail, getCurrentUserRole, getCurrentUserClubId)
- [x] Create `SpringSecurityAuditorAware` (feeds createdBy/lastModifiedBy audit fields)
- [x] Create `SecurityConfiguration` (stateless JWT, CORS, public/auth endpoints, @EnableMethodSecurity)
- [x] Create `ClubMembershipChecker` (@clubSecurity — isMemberOfClub)
- [x] Create `TeamAccessChecker` (@teamSecurity — canAccessTeam, canManageTeam)
- [x] Create custom exceptions: `BadRequestException`, `ResourceNotFoundException`, `ConflictException`
- [x] Create `ExceptionTranslator` (@ControllerAdvice, RFC 7807, i18n via MessageSource)
- [x] Create i18n: `messages.properties`, `messages_et.properties`, `messages_en.properties`
- [x] Create `LocaleConfiguration` (AcceptHeaderLocaleResolver, default Estonian)
- [x] Create auth DTOs: LoginRequestDTO, RegisterRequestDTO, RefreshTokenRequestDTO, AuthResponseDTO, UserDTO
- [x] Create `UserMapper` (MapStruct)
- [x] Create `AuthService` (register, login, refreshToken, getCurrentUser — all i18n-aware)
- [x] Create `AuthController` (POST register/login/refresh, GET me)
- [x] Seed default admin user via Liquibase (admin@club.ee / admin123, dev context only)
- [x] Write integration tests for auth endpoints (18 tests: register, login, refresh, me + error cases)
- [x] Set up TestContainers infrastructure (@IntegrationTest, @EmbeddedSQL, PostgreSqlTestContainer)
- [x] Create ArchUnit architecture test (enforces layered architecture)
- [x] Fix SecurityConfiguration: add AuthenticationEntryPoint for proper 401 responses

### Phase 2: Club & Team Management
**Goal:** Club structure, team CRUD, team roster management

- [ ] Create DTOs (ClubDTO, TeamDTO, TeamMemberDTO, UserDTO)
- [ ] Create MapStruct mappers
- [ ] Create repositories
- [ ] Create `ClubService`, `TeamService`, `UserService`
- [ ] Create `ClubController`, `TeamController`, `UserController`
- [ ] Implement admin flow: search unaffiliated users, add to club with role
- [ ] Implement team membership: add/remove users to teams
- [ ] Implement RBAC: admin sees all, coach sees own teams only
- [ ] Write integration tests

### Phase 3: Pitch & Training Session Management
**Goal:** Pitch CRUD, training scheduling with recurrence and conflict detection

- [ ] Create DTOs
- [ ] Create MapStruct mappers
- [ ] Create repositories with custom queries (pitch availability, schedule overlap)
- [ ] Create `PitchService` with availability checking
- [ ] Create `TrainingSessionService` with:
  - Single training creation
  - Recurring training generation (weekly, until end date)
  - Conflict detection (pitch double-booking)
  - Training update/cancellation with notification trigger
- [ ] Create `PitchController`, `TrainingSessionController`
- [ ] Implement pitch schedule view (all teams, for admin coordination)
- [ ] Write integration tests

### Phase 4: Attendance Management
**Goal:** Training attendance confirmation by players and parents

- [ ] Create DTOs (AttendanceDTO, AttendanceSummaryDTO)
- [ ] Create `AttendanceService` with:
  - Auto-create PENDING attendance records when training is created
  - Confirm/decline by player (self) or parent (for child)
  - Attendance summary for coach
- [ ] Create `AttendanceController`
- [ ] RBAC: player confirms own, parent confirms child's
- [ ] Write integration tests

### Phase 5: Chat System
**Goal:** Team and direct messaging with unread tracking

- [ ] Create Conversation, ConversationParticipant, Message, ConversationReadStatus entities (already done in Phase 0)
- [ ] Create DTOs
- [ ] Create MapStruct mappers
- [ ] Create repositories with custom queries (user conversations, paginated messages)
- [ ] Create `ConversationService` with:
  - Team chat auto-created when team is created (future: auto-updates participants when team membership changes)
  - Direct chat creation (get-or-create between two users)
  - Parent auto-included in child's team chats
- [ ] Create `MessageService` with:
  - Send message + update conversation preview (denormalized fields)
  - Increment unread_count for all participants except sender
  - Mark conversation as read (reset unread_count to 0)
- [ ] Create `ConversationController`
- [ ] RBAC: users can only see conversations they participate in
- [ ] Write integration tests
- [ ] Future: WebSocket (STOMP) for real-time message delivery

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
- All endpoints require authentication except `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`
- RBAC via `@PreAuthorize` with `@clubSecurity.isMemberOfClub()` and `@teamSecurity.canAccessTeam()`

### Internationalization (i18n)
- Properties-based: `src/main/resources/i18n/messages[_locale].properties`
- Supported locales: Estonian (et, default), English (en)
- Locale resolved from `Accept-Language` header via `AcceptHeaderLocaleResolver`
- All error messages in ExceptionTranslator and services use `MessageSource`
- Message keys follow dot-notation: `error.auth.emailExists`, `title.badRequest`

### Testing
- Integration tests: `*IT.java` with `@SpringBootTest` + TestContainers
- Unit tests: `*Test.java` with Mockito
- Architecture tests: ArchUnit enforcing layer dependencies
- Test profile with isolated PostgreSQL container

---

## Current Status

**Phase:** 1 (Authentication & User Management) — COMPLETE
**Next action:** Phase 2 — Club & Team Management
