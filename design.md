# Design - English Learning App (MVP)

> **Status:** Draft v1.0
> **Baseado em:** requirements.md v1.0
> **Stack:** Java 21 + Kotlin + Spring Boot 3.x
> **Última atualização:** 27/06/2026

---

## 1. Arquitetura Geral

### 1.1 Padrão Arquitetural

**Monolítico Modular** organizado por *features* (não por camadas técnicas):

```text
english-api/
├── src/main/kotlin/com/englishapp/
│   ├── auth/              # Módulo de autenticação
│   ├── users/             # Gestão de usuários/perfis
│   ├── curriculum/        # Módulos, lições, exercícios
│   ├── progress/          # Tentativas, progresso, gamificação
│   ├── ai/                # Integrações com LLMs (Fase 2)
│   └── common/            # Configs, exceptions, DTOs compartilhados
│
├── src/main/resources/
│   ├── db/migration/      # Flyway migrations
│   └── application.yml
│
└── docker-compose.yml     # Postgres + Redis + App

1.2 Diagrama de Componentes
┌─────────────────┐      ┌──────────────────┐      ┌─────────────┐
│  Frontend (PWA) │─────▶│   Spring Boot    │─────▶│ PostgreSQL  │
│  React + Vite   │◀─────│   Monolito       │◀─────│   (Core)    │
└─────────────────┘      └──────────────────┘      └─────────────┘
                                  │
                                  ▼
                         ┌──────────────┐
                         │    Redis     │
                         │  (Cache/Sess)│
                         └──────────────┘
                                  │
                                  ▼
                         ┌──────────────┐
                         │  CDN/R2/S3   │
                         │   (MP3s)     │
                         └──────────────┘

2. Stack Tecnológica

Camada                     Tecnologia                         Versão                   Justificativa
Linguagem                  Kotlin + Java                      1.9 + 21                 Stack principal do time; Virtual Threads (Java 21)
Framework                  Spring Boot                        3.3.x                    Ecossistema maduro, produtividade
ORM                        Spring Data JPA + Hibernate        -                        Produtividade + performance
Migrações                  Flyway                             10.x                     Versionamento de schema
Banco Principal            PostgreSQL                         16                       JSONB, particionamento, ACID
Cache                      Redis                              7.x                      Sessões + cache de conteúdo
API Docs                   SpringDoc OpenAPI                  2.x                      Documentação automática
Auth                       Spring Security + JWT              -                        Stateless, padrão de mercado
Containers                 Docker + Compose                   -                        Ambiente reprodutível
Build                      Gradle (Kotlin DSL)                8.x                      Performance + type-safe

3. Modelo de Dados (PostgreSQL)
3.1 Decisões de Modelagem
UUIDs para todos os IDs (conforme Backend.pdf: "imutáveis e únicos")
JSONB para conteúdo flexível (exercícios, lições)
Particionamento por mês na tabela de tentativas (séries temporais)
Versionamento de exercícios (coluna version)
3.2 DDL Completo

### 3.2 DDL Completo

> **⚠️ Nota de implementação:** Este DDL representa o **schema final consolidado**. 
> Para melhor rastreabilidade e deploy incremental, ele é dividido em **múltiplas migrations Flyway**:
> - `V1__init_extensions.sql` → Extensões (uuid-ossp, pgcrypto)
> - `V2__create_auth_users_tables.sql` → Tabelas `users` e `user_profiles`
> - `V3__create_curriculum_tables.sql` → Tabelas `modules`, `lessons`, `exercises`
> - `V4__create_progress_tables.sql` → Tabelas `lesson_progress`, `exercise_attempts` (particionada)
> - `V999__seed_module_1.sql` → Seed data (apenas em ambiente dev)
> 
> O DDL abaixo serve como **referência única** do schema esperado ao final de todas as migrations.


-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- MÓDULO: AUTH & USERS
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    google_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    current_level VARCHAR(10) DEFAULT 'A1',
    total_xp INTEGER NOT NULL DEFAULT 0,
    streak_days INTEGER NOT NULL DEFAULT 0,
    last_active_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================
-- MÓDULO: CURRICULUM (Conteúdo Pedagógico)
-- ============================================

CREATE TABLE modules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    level VARCHAR(10) NOT NULL, -- A1, A2, B1...
    order_index INTEGER NOT NULL UNIQUE,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    order_index INTEGER NOT NULL,
    estimated_minutes INTEGER NOT NULL DEFAULT 15,
    content_jsonb JSONB NOT NULL, -- Teoria, tabelas, exemplos
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(module_id, order_index)
);

CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'multiple_choice', 'fill_blank', 'translation'
    question_payload JSONB NOT NULL, -- Pergunta + opções + áudio
    correct_answer JSONB NOT NULL, -- Gabarito determinístico
    feedback_on_error TEXT, -- Explicação hard-coded
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(lesson_id, order_index, version)
);

-- ============================================
-- MÓDULO: PROGRESS (Dados do Aluno)
-- ============================================

CREATE TABLE lesson_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'not_started',
        -- not_started, in_progress, completed, mastered
    best_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    attempts_count INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, lesson_id)
);

-- Tabela PARTICIONADA por mês (séries temporais)
CREATE TABLE exercise_attempts (
    id BIGSERIAL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL,
    exercise_version INTEGER NOT NULL,
    user_answer JSONB NOT NULL,
    is_correct BOOLEAN NOT NULL,
    time_spent_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Criar partições automaticamente (exemplo para 2026)
CREATE TABLE exercise_attempts_2026_06 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE exercise_attempts_2026_07 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

-- Índices críticos
CREATE INDEX idx_exercises_lesson ON exercises(lesson_id, is_active);
CREATE INDEX idx_attempts_user_date ON exercise_attempts(user_id, created_at DESC);
CREATE INDEX idx_lessons_module ON lessons(module_id, order_index);

3.3 Diagrama ER Simplificado

users (1) ──── (1) user_profiles
  │
  ├──── (N) lesson_progress (N) ──── lessons (N) ──── (1) modules
  │
  └──── (N) exercise_attempts (N) ──── exercises (N) ──── (1) lessons

4. API REST
4.1 Convenções (baseadas no Backend.pdf)
Recursos identificados por UUID (ex: /lessons/{id})
Respostas em JSON
Imutabilidade: exercícios versionados, nunca sobrescritos
Verbos HTTP padrão: GET, POST, PUT, PATCH, DELETE
Paginação: ?page=0&size=20 (padrão Spring)
Fuso horário: todos os timestamps em UTC (ISO 8601)
4.2 Endpoints Principais
4.2.1 Auth

POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "aluno@email.com",
  "password": "senha123"
}

Response: 201 Created

{
  "user_id": "2e3990d1-b01f-47bb-bd10-42cc6e0f40f0",
  "access_token": "eyJhbGc..."
}

POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "aluno@email.com",
  "password": "senha123"
}

Response: 200 OK

{
  "access_token": "eyJhbGc...",
  "expires_in": 86400
}

4.2.2 Curriculum (Leitura)
GET /api/v1/modules
Authorization: Bearer {token}

Response: 200 OK

{
  "data": [
    {
      "id": "uuid-modulo-1",
      "title": "Verbo TO BE - Fundamentos",
      "level": "A1",
      "lessons_count": 10,
      "completed_lessons": 0,
      "progress_percent": 0
    }
  ]
}

GET /api/v1/modules/{moduleId}/lessons
Authorization: Bearer {token}

Response: 200 OK

{
  "data": [
    {
      "id": "uuid-lesson-1-1",
      "title": "TO BE - Presente Afirmativo",
      "order": 1,
      "estimated_minutes": 15,
      "status": "not_started",
      "best_score": null
    }
  ]
}

GET /api/v1/lessons/{lessonId}
Authorization: Bearer {token}

Response: 200 OK

{
  "id": "uuid-lesson-1-1",
  "title": "TO BE - Presente Afirmativo",
  "content": {
    "theory_html": "<p>O verbo TO BE significa...</p>",
    "conjugation_table": {
      "I": "am",
      "He": "is",
      "She": "is",
      "It": "is",
      "You": "are",
      "We": "are",
      "They": "are"
    },
    "examples": [
      { "en": "I am a student.", "pt": "Eu sou um estudante." }
    ]
  },
  "exercises": [
    {
      "id": "uuid-ex-1",
      "type": "multiple_choice",
      "payload": { "...": "ver seção 5" }
    }
  ],
  "audio_urls": {
    "intro": "https://cdn.app.com/m1_l1_intro.mp3",
    "examples": ["ex1.mp3", "ex2.mp3"]
  }
}

4.2.3 Progress (Escrita)

POST /api/v1/attempts
Authorization: Bearer {token}
Content-Type: application/json

{
  "lesson_id": "uuid-lesson-1-1",
  "attempts": [
    {
      "exercise_id": "uuid-ex-1",
      "exercise_version": 1,
      "user_answer": { "selected_index": 2 },
      "time_spent_ms": 4500
    },
    {
      "exercise_id": "uuid-ex-2",
      "exercise_version": 1,
      "user_answer": { "text": "I am happy" },
      "time_spent_ms": 8200
    }
  ]
}

Response: 200 OK

{
  "results": [
    {
      "exercise_id": "uuid-ex-1",
      "is_correct": true,
      "xp_earned": 10,
      "feedback": null
    },
    {
      "exercise_id": "uuid-ex-2",
      "is_correct": false,
      "xp_earned": 0,
      "correct_answer": { "text": "I am happy." },
      "feedback": "Não esqueça o ponto final."
    }
  ],
  "lesson_progress": {
    "status": "in_progress",
    "current_score": 50.0,
    "exercises_remaining": 8
  }
}

GET /api/v1/users/me/progress
Authorization: Bearer {token}

Response: 200 OK

{
  "user_id": "2e3990d1-b01f-47bb-bd10-42cc6e0f40f0",
  "total_xp": 150,
  "streak_days": 3,
  "current_level": "A1",
  "modules": [
    {
      "module_id": "uuid-modulo-1",
      "title": "Verbo TO BE",
      "progress_percent": 25,
      "next_lesson_id": "uuid-lesson-1-3"
    }
  ],
  "recent_errors": [
    {
      "exercise_id": "uuid-ex-5",
      "lesson_title": "TO BE - Presente",
      "your_answer": "He are",
      "correct_answer": "He is"
    }
  ]
}

5. Contratos JSON dos Exercícios
5.1 Multiple Choice
Baseado no gabarito do PDF (ex: "Qual a maneira correta de afirmar: 'Ele está'?"):

{
  "id": "uuid-ex-5",
  "type": "multiple_choice",
  "payload": {
    "prompt": "Qual a maneira correta de afirmar: 'Ele está'?",
    "audio_url": "https://cdn.app.com/m1_l1_q5.mp3",
    "options": [
      { "index": 0, "text": "He was" },
      { "index": 1, "text": "He are" },
      { "index": 2, "text": "He is" },
      { "index": 3, "text": "He were" }
    ]
  },
  "correct_answer": { "selected_index": 2 },
  "feedback_on_error": "Lembre-se: para HE/SHE/IT usamos IS no presente. Exemplo: 'She is happy.'"
}

5.2 Fill-in-the-Blank

{
  "id": "uuid-ex-15",
  "type": "fill_blank",
  "payload": {
    "sentence_template": "She ___ a teacher.",
    "audio_url": "https://cdn.app.com/m1_l1_fib1.mp3",
    "accepted_answers": ["is", "IS", "Is"],
    "hint": "Use o verbo TO BE no presente para SHE"
  },
  "correct_answer": { "text": "is" },
  "feedback_on_error": "Para SHE no presente, usamos IS. 'She IS a teacher.'"
}

5.3 Translation PT→EN

{
  "id": "uuid-ex-20",
  "type": "translation",
  "payload": {
    "source_text": "Nós estávamos cansados.",
    "source_audio_url": "https://cdn.app.com/m1_l5_trans1_pt.mp3",
    "target_audio_url": "https://cdn.app.com/m1_l5_trans1_en.mp3",
    "accepted_answers": [
      "We were tired.",
      "We were tired",
      "we were tired."
    ],
    "case_sensitive": false,
    "ignore_punctuation": true
  },
  "correct_answer": { "text": "We were tired." },
  "feedback_on_error": "'Nós estávamos' = WE WERE (passado de TO BE para plural). Lembre-se: WAS é para I/He/She/It, WERE para You/We/They."
}

6. Estratégia de Cache (Redis)
6.1 O que cachear

Chave                          TTL                  Conteúdo
lesson:{id}                    24h                  JSON completo da lição (teoria + exercícios)
module:list                    1h                   Lista de módulos publicados
session:{userId}               7d                   Dados de sessão do usuário
user:xp:{userId}               5min                 Cache do XP total (evita recalcular)


6.2 Padrão de implementação

@Cacheable(value = ["lessons"], key = "#id", unless = "#result == null")
fun getLesson(id: UUID): LessonDTO {
    return lessonRepository.findById(id).toDTO()
}

6.3 Invalidação
Quando um exercício é versionado (nova version), invalidar cache da lição pai
Endpoint admin: POST /admin/cache/invalidate?pattern=lesson:*
7. Estratégia de Áudio (MP3s)
7.1 Geração Offline (Script Python)

# scripts/generate_audio.py (executado localmente/CI)
import edge_tts
import asyncio

async def generate(text: str, output: str, voice: str = "en-US-JennyNeural"):
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(output)

# Gera MP3s para todos os exemplos do CSV de conteúdo
# Upload para Cloudflare R2 / AWS S3
# Salva URLs no content_jsonb das lições

7.2 Servir em Produção
CDN: Cloudflare R2 (mais barato) ou AWS CloudFront + S3
URLs públicas (sem auth) - MP3s são conteúdo pedagógico público
Compressão: OGG/Opus para mobile (fallback de MP3)
7.3 Não fazer
❌ Não gerar TTS em tempo real (custo proibitivo)
❌ Não usar Web Speech API do navegador (qualidade inconsistente)
❌ Não salvar áudio no Postgres (usar storage externo)
8. Decisões Pendentes (Críticas)
Estas decisões impactam a arquitetura e precisam ser resolvidas antes da implementação avançar.
PD-01: Frontend
Status: Pendente
Default assumido: PWA com React + Vite + Tailwind
Impacto se mudar: Se for React Native, APIs permanecem as mesmas, mas autenticação (OAuth) e storage de sessão mudam
PD-02: Monetização
Status: Pendente
Default assumido: Gratuito na Fase 1 (sem paywall)
Impacto se mudar: Se for freemium, adicionar tabela subscriptions e lógica de bloqueio de módulos
PD-03: Hosting
Status: Pendente
Default assumido: Railway (Postgres + Redis + Spring Boot em um clique)
Impacto se mudar: Se for AWS, precisamos de configuração de VPC, IAM, RDS
PD-04: Observabilidade
Status: Não discutido
Recomendação: Adicionar Sentry (erros) + Logs estruturados (Logback JSON) desde o dia 1
9. Pontos de Atenção e Riscos Técnicos
9.1 Risco: Explosão de exercise_attempts
Mitigação: Particionamento por mês já implementado no DDL
Monitorar: Tamanho das partições mensalmente
Plano de retenção: Após 6 meses, arquivar partições antigas em S3
9.2 Risco: Versionamento de exercícios mal gerenciado
Regra: Nunca dar UPDATE em exercício publicado
Fluxo: Para alterar exercício, criar nova linha com version = version + 1 e is_active = true (desativar anterior)
Tentativas antigas: Preservam exercise_version da época da resposta
9.3 Risco: Inconsistência entre lesson_progress e exercise_attempts
Mitigação: Transação única ao processar POST /attempts
Inserir tentativas
Recalcular score da lição
Atualizar lesson_progress
Atualizar total_xp do usuário
Commit
9.4 Risco: JSONB sem validação
Mitigação: Validar question_payload no backend antes de salvar (JSON Schema ou classes Kotlin data)
Exemplo: Usar @JsonIgnoreProperties(ignoreUnknown = false) em DTOs

10. Próximos Passos Imediatos
1. ~~Criar repositório Git~~ ✅ (feito)
2. ~~Subir Docker Compose~~ ✅ (feito)
3. ~~Executar Flyway V1 (extensions)~~ ✅ (feito)
4. Criar migration `V2__create_auth_users_tables.sql`
5. Implementar módulo `auth` (registro + login JWT + SecurityConfig)
6. Criar migration `V3__create_curriculum_tables.sql`
7. Criar script Python para gerar MP3s das 10 lições do Módulo 1 (Edge-TTS)
8. Criar migration `V999__seed_module_1.sql` (seed data do Módulo 1 baseado nos PDFs)
9. Implementar GET `/api/v1/lessons/{id}` (primeiro endpoint de leitura)
10. Criar migration `V4__create_progress_tables.sql`
11. Implementar POST `/api/v1/attempts` (primeiro endpoint de escrita)

11. Referências
Backend.pdf (padrões REST, UUIDs, JSON)
Ampliando a Visão.pdf (conteúdo pedagógico TO BE)
Atividade + Gabarito (exercícios canônicos)
requirements.md v1.0 (escopo do MVP)



