# Project Structure Map - English Learning App (MVP)

## 📁 Current Project Structure (Actual Files)

```
aprenda_ingles/
├── .gradle/                          # Gradle cache (auto-generated)
├── build/                            # Build output (auto-generated)
├── src/
│   └── main/
│       ├── kotlin/com/englishapp/
│       │   └── EnglishAppApplication.kt      # ✅ Spring Boot Entry Point
│       └── resources/
│           ├── application.yml               # ✅ Base config (profile=dev)
│           ├── application-dev.yml           # ✅ Dev config (Postgres + Redis)
│           └── db/migration/
│               └── V1__init_extensions.sql   # ✅ Flyway V1 (UUID extensions)
├── .kilo/                               # Kilo config
├── docker-compose.yml                   # ✅ Postgres 16 + Redis 7
├── build.gradle.kts                     # ✅ Spring Boot 3.3 + Kotlin 1.9 + Java 21
├── settings.gradle.kts                  # ✅ Project name: english-app
├── design.md                            # ✅ Design Doc (v1.0 Draft)
├── Requirements.md                      # ✅ Requirements Doc (v1.0)
├── CLAUDE.md                            # ✅ Context for AI
├── SKILLS.md                            # ✅ Skills reference
└── PROJECT_STRUCTURE.md                 # ✅ This file
```

---

## 🏗️ Designed Architecture (from design.md - Target Structure)

```
english-api/
├── src/main/kotlin/com/englishapp/
│   ├── auth/                 # 📦 Auth Module (PENDING)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── dto/
│   │   └── security/
│   ├── users/                # 📦 Users Module (PENDING)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── entity/
│   ├── curriculum/           # 📦 Curriculum Module (PENDING)
│   │   ├── module/
│   │   ├── lesson/
│   │   ├── exercise/
│   │   └── dto/
│   ├── progress/             # 📦 Progress Module (PENDING)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── entity/
│   ├── ai/                   # 📦 AI Module (Phase 2 - PENDING)
│   └── common/               # 📦 Shared/Common (PENDING)
│       ├── config/
│       ├── exception/
│       ├── dto/
│       └── security/
│
├── src/main/resources/
│   ├── db/migration/         # Flyway Migrations
│   │   ├── V1__init_extensions.sql     ✅ EXISTS
│   │   ├── V2__auth_users.sql          📋 PLANNED
│   │   ├── V3__curriculum.sql          📋 PLANNED
│   │   └── V4__progress.sql            📋 PLANNED
│   └── application.yml
│
└── docker-compose.yml        ✅ EXISTS (Postgres + Redis)
```

---

## 🗄️ Database Schema (Designed in design.md)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            POSTGRESQL 16                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐       ┌──────────────────┐                                │
│  │    users     │ 1:1   │  user_profiles   │                                │
│  ├──────────────┤◀──────┤                  │                                │
│  │ id (PK, UUID)│       │ user_id (PK, FK) │                                │
│  │ email (UK)   │       │ display_name     │                                │
│  │ password_hash│       │ current_level    │                                │
│  │ google_id    │       │ total_xp         │                                │
│  │ created_at   │       │ streak_days      │                                │
│  │ updated_at   │       │ last_active_at   │                                │
│  │ deleted_at   │       │ created_at       │                                │
│  └──────────────┘       └──────────────────┘                                │
│         │                        │                                           │
│         │ 1:N                    │ 1:N                                       │
│         ▼                        ▼                                           │
│  ┌──────────────────┐    ┌──────────────────┐                               │
│  │ lesson_progress  │    │ exercise_attempts│ (PARTITIONED BY MONTH)        │
│  ├──────────────────┤    ├──────────────────┤                               │
│  │ user_id (PK, FK) │    │ id (BIGSERIAL)   │                               │
│  │ lesson_id (PK,FK)│    │ user_id (FK)     │                               │
│  │ status           │    │ exercise_id      │                               │
│  │ best_score       │    │ exercise_version │                               │
│  │ attempts_count   │    │ user_answer JSONB│                               │
│  │ completed_at     │    │ is_correct       │                               │
│  │ updated_at       │    │ time_spent_ms    │                               │
│  └──────────────────┘    │ created_at (PK)  │                               │
│                          └──────────────────┘                               │
│                                    ▲                                        │
│                                    │ N:1                                    │
│                          ┌─────────┴─────────┐                             │
│                          ▼                   ▼                             │
│                   ┌──────────────┐    ┌──────────────┐                    │
│                   │   lessons    │    │  exercises   │                    │
│                   ├──────────────┤    ├──────────────┤                    │
│                   │ id (PK, UUID)│    │ id (PK, UUID)│                    │
│                   │ module_id FK │    │ lesson_id FK │                    │
│                   │ title        │    │ order_index  │                    │
│                   │ order_index  │    │ type         │                    │
│                   │ estimated_min│    │ question_pld │                    │
│                   │ content_jsonb│    │ correct_ans  │                    │
│                   │ is_published │    │ feedback_err │                    │
│                   │ created_at   │    │ version      │                    │
│                   │ updated_at   │    │ is_active    │                    │
│                   └──────────────┘    └──────────────┘                    │
│                          ▲                       ▲                         │
│                          │ 1:N                   │ 1:N                      │
│                   ┌──────┴──────┐         ┌──────┴──────┐                  │
│                   │   modules   │         │  (versioned)│                  │
│                   ├─────────────┤         └─────────────┘                  │
│                   │ id (PK, UUID)│                                        │
│                   │ title        │                                        │
│                   │ level        │ level (A1..C2)                                 │
│                   │ order_index  │                                        │
│                   │ is_published │                                        │
│                   └──────────────┘                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Tech Stack (Configured)

| Layer | Technology | Version | Status |
|-------|------------|---------|--------|
| **Language** | Kotlin + Java | 1.9 / 21 | ✅ Configured |
| **Framework** | Spring Boot | 3.3.0 | ✅ Configured |
| **Build** | Gradle (Kotlin DSL) | 8.x | ✅ Configured |
| **Database** | PostgreSQL | 16 | ✅ Docker Compose |
| **Cache** | Redis | 7.x | ✅ Docker Compose |
| **ORM** | Spring Data JPA + Hibernate | - | ✅ Configured |
| **Migrations** | Flyway | 10.x | ✅ Configured |
| **Auth** | Spring Security + JWT | - | 📋 Planned |
| **API Docs** | SpringDoc OpenAPI | 2.x | 📋 Planned |
| **Testing** | JUnit 5 + Testcontainers + MockK | - | ✅ Configured |

---

## 🚀 Current State Summary

| Component | Status | Notes |
|-----------|--------|-------|
| **Project Setup** | ✅ Done | Gradle + Spring Boot + Kotlin configured |
| **Database** | ✅ Running | Postgres + Redis via Docker Compose |
| Flyway Migrations | 🔄 V1 Done, V2 In Progress | Strategy: V1=extensions, V2=auth, V3=curriculum, V4=progress, V999=seed |
| **Application Config** | ✅ Done | Dev profile active |
| **Entry Point** | ✅ Done | EnglishAppApplication.kt |
| **Auth Module** | 📋 Planned | Register/Login + JWT (design.md §4.2.1) |
| **Curriculum Module** | 📋 Planned | Modules/Lessons/Exercises (design.md §4.2.2) |
| **Progress Module** | 📋 Planned | Attempts/Progress/XP (design.md §4.2.3) |
| **Database Schema** | 📋 Planned | Full DDL in design.md §3.2 |
| **API Endpoints** | 📋 Planned | Defined in design.md §4.2 |
| **Exercise Types** | 📋 Planned | MCQ, Fill-blank, Translation (design.md §5) |
| **Audio/MP3 Strategy** | 📋 Planned | Edge-TTS offline generation (design.md §7) |
| **Frontend** | ❌ Not Started | Decision pending (PWA vs React Native) |

---

## 📋 Next Implementation Steps (from design.md §10)

```
2. □ Run Flyway V1: ./gradlew flywayMigrate (extensions only)
3. □ Create Flyway V2: V2__create_auth_users_tables.sql (users + user_profiles)
4. □ Implement Auth Module (Register + Login + JWT)
5. □ Create Flyway V3: V3__create_curriculum_tables.sql (modules + lessons + exercises)
6. □ Seed Module 1 data (via V999__seed_module_1.sql)
7. □ Generate MP3s for Module 1 (Python + Edge-TTS)
8. □ Create Flyway V4: V4__create_progress_tables.sql (lesson_progress + exercise_attempts)
9. □ Implement GET /lessons/{id} endpoint
10. □ Implement POST /attempts endpoint
```

---

## 🔑 Key Architectural Decisions (from design.md)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Architecture** | Modular Monolith | Simple deploy, easy debugging, extract later if needed |
| **Organization** | By Feature (not layers) | Auth, Users, Curriculum, Progress, AI, Common |
| **IDs** | UUID (v4) | Immutable, unique, distributed-friendly |
| **Content** | JSONB (lessons, exercises) | Flexible schema for pedagogical content |
| **Exercises** | Versioned (never UPDATE) | Immutable history, audit trail |
| **Attempts Table** | Partitioned by Month | Time-series scale, retention policy |
| **Audio** | Pre-generated MP3s on CDN | Zero runtime cost, consistent quality |
| **Cache** | Redis (lessons, modules, sessions) | 24h TTL for lessons, 7d for sessions |
| **Auth** | JWT Stateless | Standard, scalable, no server sessions |

---

## 📍 Quick Start Commands

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Run migrations
./gradlew flywayMigrate

# 3. Run application
./gradlew bootRun

# 4. Verify health
curl http://localhost:8080/actuator/health
```

---

*Generated: 2026-06-27 | Project: aprenda_ingles | Stack: Kotlin + Spring Boot 3.3 + PostgreSQL 16*