# AGENTS.md - Context & Architecture Guide for AI Agents & Developers

Welcome to **P01 Backend** (OJT Enterprise Project). This document serves as the single source of truth for AI Agents (Antigravity, Continue, Copilot, Cursor, Aider) and human developers to understand, maintain, and extend this codebase.

> **Project Identity:** The official name of this project is **P01** (IT Helpdesk & Service Request Management System).

---

## 1. Project Overview & Tech Stack
* **Project Name:** **P01 Backend**
* **Language & Runtime:** Java 21 LTS
* **Framework:** Spring Boot 3.3.4 (Spring Framework 6)
* **Security:** Spring Security 6 + JJWT (0.12.6) with Access Token & Refresh Token + Blacklisting / Revocation
* **Persistence:** Spring Data JPA + Hibernate
* **Database:** H2 In-Memory (Dev/Test) | PostgreSQL 16 ready (Production)
* **Documentation:** OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
* **Architecture Pattern:** **Hexagonal (Ports & Adapters) + Clean Architecture**

---

## 2. Installed Agent Skills (`.agents/skills/`)

This project is pre-configured with two industry-standard Agent Skill repositories to give AI Agents full code comprehension and high-accuracy Spring Boot generation:

### 1. `Amplicode/spring-skills` (`.agents/skills/spring-skills/skills/`)
Curated by the Spring and Amplicode teams to eliminate hallucinations in enterprise Java:
* **`spring-explore`**: Scans the project layout, identifies Spring configuration, beans, entity models, and controllers.
* **`spring-planning`**: Breaks down backend tasks into phased implementation blueprints before modifying files.
* **`crud-rest-controller`**: Implements RESTful endpoints adhering to REST conventions, response wrappers, and validation.
* **`dto-creator`**: Generates clean DTO records/classes with Bean Validation annotations (`@Valid`, `@NotBlank`, `@Size`).
* **`mapper-creator`**: Implements bidirectional mappers between Domain POJOs, DTOs, and JPA entities.
* **`spring-data-jpa`**: Enforces best practices for `@Entity`, Spring Data Repositories, and lazy-loading safety.
* **`spring-security-configuration`**: Configures SecurityFilterChain, role hierarchies, and stateless JWT filters.

### 2. `mattpocock/skills` (`.agents/skills/mattpocock-skills/skills/`)
Engineering-discipline skills for architectural clarity and robust delivery:
* **`improve-codebase-architecture`**: Audits package boundaries, prevents circular dependencies, and validates hexagonal isolation.
* **`grill-with-docs`**: Socratic analysis of business requirements, ensuring no ambiguity remains before coding.
* **`to-spec` & `to-tickets`**: Converts high-level user stories into actionable, testable technical tasks.

---

## 3. Directory Structure & Architecture Boundaries

```
src/main/java/com/ojt/p01/
│
├── common/                                 # Shared utilities & cross-cutting concerns
│   ├── api/
│   │   └── ApiResponse.java                # Unified REST response wrapper { code, message, data, timestamp }
│   └── exception/
│       ├── AppException.java               # Custom business exceptions
│       ├── ErrorCode.java                  # Enum of error codes and HTTP statuses
│       └── GlobalExceptionHandler.java     # Centralized exception handler (@RestControllerAdvice)
│
├── domain/                                 # [LAYER 1: DOMAIN CORE] (Zero Framework Dependencies)
│   ├── model/                              # Pure business entities/POJOs (User, Role, Permission)
│   └── port/                               # Ports define boundaries:
│       ├── in/                             # Inbound Ports (Use Cases invoked by Web/CLI)
│       │   ├── AuthenticationUseCase.java
│       │   └── UserManagementUseCase.java
│       └── out/                            # Outbound Ports (SPI invoked by Services to reach DB/IO)
│           ├── LoadUserPort.java
│           ├── SaveUserPort.java
│           └── TokenPort.java
│
├── application/                            # [LAYER 2: APPLICATION SERVICES]
│   └── service/                            # Use Case implementations (orchestration, validation, tx)
│       ├── AuthenticationService.java
│       └── UserService.java
│
└── infrastructure/                         # [LAYER 3: ADAPTERS & INFRASTRUCTURE]
    ├── adapter/
    │   ├── in/web/                         # Driving/Inbound Adapters (REST Controllers, DTOs)
    │   │   ├── AuthController.java         # /api/v1/auth/**
    │   │   ├── UserController.java         # /api/v1/users/**
    │   │   ├── ManagementController.java   # /api/v1/management/**
    │   │   └── dto/                        # Request / Response DTOs
    │   └── out/persistence/                # Driven/Outbound Adapters (JPA Entities, Repositories)
    │       ├── entity/                     # UserJpaEntity, TokenJpaEntity
    │       ├── repository/                 # SpringDataUserJpaRepository, SpringDataTokenJpaRepository
    │       └── adapter/                    # UserPersistenceAdapter (implements Outbound Ports)
    ├── config/                             # Security, OpenApi, ApplicationConfig, DataInitializer
    └── security/                           # LogoutService (Token revocation)
```

---

## 4. Core Architectural Rules for AI Agents

When implementing any new business feature in this project, **STRICTLY follow this 5-step flow**:

1. **Step 1 - Define the Domain Model (`domain/model/`)**:
   Create plain Java classes (no JPA `@Entity` or `@Table` annotations here). Domain logic belongs here.
2. **Step 2 - Define Ports (`domain/port/`)**:
   * Create an Inbound Port interface in `domain/port/in/` (e.g. `CreateTicketUseCase.java`).
   * Create Outbound Port interfaces in `domain/port/out/` (e.g. `SaveTicketPort.java`, `LoadTicketPort.java`).
3. **Step 3 - Implement the Application Service (`application/service/`)**:
   * Implement the Inbound Port.
   * Inject Outbound Ports (NOT JPA Repositories directly!).
   * Handle transactional boundaries (`@Transactional`) and business validation.
4. **Step 4 - Implement Driven/Persistence Adapter (`infrastructure/adapter/out/persistence/`)**:
   * Create JPA Entity in `entity/` (e.g. `TicketJpaEntity.java`).
   * Create Spring Data Repository interface in `repository/`.
   * Create Adapter class implementing Outbound Ports and map between Domain Model and JPA Entity.
5. **Step 5 - Implement Driving/Web Adapter (`infrastructure/adapter/in/web/`)**:
   * Create Request/Response DTOs in `dto/`.
   * Create `@RestController` injecting the Inbound Port (Use Case).
   * Return responses wrapped in `ApiResponse<T>`.

---

## 5. Default Seed Accounts (For Local Development)

When the application starts, `DataInitializer` pre-loads the following accounts into H2:
* **Admin:** `admin@p01.ojt` / `admin123` (Roles: `ADMIN`, all CRUD permissions)
* **Manager:** `manager@p01.ojt` / `manager123` (Roles: `MANAGER`)
* **User:** `user@p01.ojt` / `user123` (Roles: `USER`)

---

## 6. Key Endpoints

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register new user | Public |
| `POST` | `/api/v1/auth/authenticate` | Login, get Access & Refresh Token | Public |
| `POST` | `/api/v1/auth/refresh-token` | Refresh Access Token | Bearer Refresh Token |
| `POST` | `/api/v1/auth/logout` | Revoke active token | Bearer Token |
| `GET` | `/api/v1/users/me` | Current user profile | Authenticated |
| `GET` | `/api/v1/users` | List all users | `ADMIN` only |
| `GET` | `/api/v1/management` | Management demo | `ADMIN` or `MANAGER` |
| `GET` | `/swagger-ui.html` | Swagger / OpenAPI UI | Public |
| `GET` | `/h2-console` | In-memory DB console | Public (JDBC URL: `jdbc:h2:mem:p01db`) |
