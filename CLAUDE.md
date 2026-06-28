# CLAUDE.md - Instruções para Agentes de IA

> **Projeto:** English Learning App (MVP)
> **Stack:** Java 21 + Kotlin 1.9 + Spring Boot 3.3 + PostgreSQL 16 + Redis 7
> **Público:** Brasileiros aprendendo inglês (nível A1-B2)
> **Fase atual:** MVP - Módulo 1 (Verbo TO BE)

Este arquivo é a **fonte única de verdade** para qualquer agente de IA trabalhando neste repositório. Leia-o antes de gerar, modificar ou revisar código.

---

## 1. Contexto do Projeto

Aplicativo de ensino de inglês com:
- Trilha pedagógica estruturada (módulos → lições → exercícios)
- Exercícios de múltipla escolha, fill-in-the-blank e tradução
- Correção automática determinística (gabarito hard-coded, não IA)
- Áudios MP3 pré-gerados (não TTS em runtime)
- Progresso do aluno com gamificação básica (XP, streak)

**Documentação complementar** (consulte quando necessário):
- `docs/requirements.md` - Escopo do MVP (o que está IN/OUT)
- `docs/design.md` - Arquitetura, DDL completo, endpoints REST
- `docs/GLOSSARY.md` - Linguagem ubíqua do domínio (Module, Lesson, Exercise, Attempt)
- `docs/decisions/` - ADRs (Decisões Arquiteturais)

---

## 2. Stack Tecnológica (NÃO SUGERIR ALTERNATIVAS)

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | **Kotlin** (padrão) + Java (apenas integrações) | 1.9 + 21 |
| Framework | Spring Boot | 3.3.x |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Migrações | Flyway | 10.x |
| Banco | PostgreSQL (com JSONB) | 16 |
| Cache | Redis | 7.x |
| Testes | JUnit 5 + **MockK** (NÃO Mockito) + Testcontainers | - |
| Build | Gradle (Kotlin DSL) | 8.x |
| Auth | Spring Security + JWT | - |

### Tecnologias PROIBIDAS (não sugerir, não usar):
- ❌ Python no core da API (apenas scripts batch offline em `/scripts`)
- ❌ MongoDB, Neo4j, Elasticsearch, Kafka, RabbitMQ
- ❌ Mockito (usar MockK)
- ❌ TTS/STT em runtime (usar MP3s pré-gerados)
- ❌ LLMs gerando conteúdo pedagógico (apenas feedbacks dinâmicos em Fase 2)

---

## 3. Regras Absolutas (NUNCA QUEBRAR)

### 3.1 Identificadores
- **Todos os IDs são UUIDs** (nunca `Long` ou `int` para IDs de domínio)
- Gerados via `uuid_generate_v4()` no Postgres
- Tipo Kotlin: `java.util.UUID`

### 3.2 Imutabilidade de Conteúdo Pedagógico
- **Exercícios (`exercises`) são IMUTÁVEIS após publicação**
- Nunca dar `UPDATE` em exercício publicado
- Para alterar: criar nova linha com `version = version + 1`, `is_active = true`, desativar anterior
- Tentativas (`exercise_attempts`) sempre gravam `exercise_version` do momento

### 3.3 API REST
- Todos os endpoints versionados: `/api/v1/...`
- Recursos identificados por UUID: `/lessons/{id}` (nunca `/lessons/123`)
- Response envelope: `{"data": ..., "meta": ...}` para listas
- Timestamps sempre em **UTC ISO 8601** (`TIMESTAMPTZ` no Postgres)
- Paginação obrigatória em listas: `?page=0&size=20`
- Erros no formato: `{"error": {"code": "...", "message": "..."}}`

### 3.4 Transações
- Operações que escrevem em múltiplas tabelas (`exercise_attempts` + `lesson_progress` + `user_profiles.xp`) **devem ser uma única transação `@Transactional`**
- Nunca dividir em múltiplas chamadas não-transacionais

### 3.5 Validação
- Validar entrada com `@Valid` + Bean Validation (`jakarta.validation`)
- Validar `question_payload` (JSONB) via JSON Schema ou classes Kotlin data no service
- Nunca confiar em dados vindos do frontend

---

## 4. Estrutura de Pastas (Monolítico Modular)

english-api/
├── src/main/kotlin/com/englishapp/
│ ├── auth/ # Feature: autenticação
│ │ ├── api/ # Controllers REST
│ │ ├── application/ # Services (casos de uso)
│ │ ├── domain/ # Entities, value objects
│ │ └── infrastructure/ # Repositories, JWT provider
│ ├── users/ # Feature: usuários/perfis
│ ├── curriculum/ # Feature: módulos, lições, exercícios
│ ├── progress/ # Feature: tentativas, progresso
│ ├── ai/ # Feature: integrações LLM (Fase 2)
│ └── common/ # Compartilhado (exceptions, DTOs, configs)
│
├── src/main/resources/
│ ├── db/migration/ # Flyway: V1__create_users.sql, V2__...
│ └── application.yml
│
├── src/test/kotlin/ # Testes espelhando estrutura main
├── scripts/ # Scripts Python (geração de MP3, seeds)
├── docs/ # Documentação (requirements, design, ADRs)
└── docker-compose.yml


**Regra:** Uma feature NÃO importa classes de outra feature diretamente. Se precisar, use eventos de domínio ou interfaces em `common/`.

---

## 5. Convenções de Código Kotlin

### 5.1 Injeção de Dependência
```kotlin
// ✅ CORRETO: Constructor injection
@Service
class LessonService(
    private val lessonRepository: LessonRepository,
    private val cacheManager: CacheManager
) { ... }

// ❌ ERRADO: Field injection
@Service
class LessonService {
    @Autowired lateinit var lessonRepository: LessonRepository
}

5.2 Variáveis

// ✅ Em services e domains: sempre `val` (imutável)
val lesson = lessonRepository.findById(id)

// ⚠️ `var` apenas em builders, acumuladores locais ou loops

5.3 Null Safety

// ✅ Prefira tipos não-nuláveis + early returns
fun findLesson(id: UUID): LessonDTO {
    val lesson = lessonRepository.findById(id).orElseThrow { 
        EntityNotFoundException("Lesson $id not found") 
    }
    return lesson.toDTO()
}

// ❌ Evite `!!` (non-null assertion) em produção

5.4 Data Classes (DTOs)

// ✅ Para DTOs, use data class com validação
data class CreateAttemptRequest(
    @field:NotNull val lessonId: UUID,
    @field:NotEmpty val attempts: List<AttemptItem>
)

// ⚠️ NÃO use data class para Entities JPA (problema com equals/hashCode lazy)

5.5 Entities JPA

@Entity
@Table(name = "lessons")
class Lesson(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    var title: String,
    
    @Column(name = "content_jsonb", columnDefinition = "jsonb")
    @Type(JsonBinaryType::class)
    var content: Map<String, Any> = emptyMap()
) {
    // Entities usam `var` quando mutáveis, mas preferir imutabilidade
}

6. Banco de Dados (PostgreSQL)
6.1 Regras de Schema
Tabelas no plural (users, lessons, exercises)
Colunas em snake_case
Foreign keys nomeadas: fk_{tabela}_{tabela_ref}
Índices nomeados: idx_{tabela}_{coluna}
Sempre adicionar created_at e updated_at (trigger para updated_at)
6.2 JSONB
Usar para: exercises.question_payload, exercises.correct_answer, lessons.content_jsonb
Nunca usar JSONB para dados que precisam de índice ou FK
Validar estrutura no service antes de salvar
6.3 Particionamento
exercise_attempts é particionada por mês (PARTITION BY RANGE (created_at))
Criar partições futuras via migration antes de virar o mês
Queries devem sempre filtrar por created_at para aproveitar particionamento
6.4 Migrations (Flyway)
Nome: V{N}__{descricao_curta}.sql (ex: V3__add_lesson_progress_table.sql)
Toda migration deve ser idempotente (usar IF NOT EXISTS, CREATE OR REPLACE)
Nunca alterar migration já aplicada em produção - criar nova
Separar DDL de DML (dados de seed vão em migrações separadas V{N}__seed_*.sql)

7. Padrões de Teste
7.1 Pirâmide de Testes
Unitários (70%): Services, validadores, mappers. Usar MockK.
Integração (25%): Controllers + banco real via Testcontainers (Postgres + Redis).
E2E (5%): Fluxos críticos (signup → complete lesson → view progress).
7.2 Nomenclatura

@Test
fun `should return 404 when lesson does not exist`() { ... }

@Test
fun `should award XP when lesson completed with score above 80 percent`() { ... }

7.3 MockK (NÃO Mockito)

// ✅ MockK
val repository = mockk<LessonRepository>()
every { repository.findById(any()) } returns Optional.of(lesson)
verify(exactly = 1) { repository.save(any()) }

// ❌ Não usar Mockito neste projeto

7.4 Testcontainers para Integração

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
class LessonControllerIT {
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16")
    }
}

8. Comandos Úteis

# Rodar ambiente local (Postgres + Redis)
docker-compose up -d

# Rodar aplicação (dev)
./gradlew bootRun

# Rodar todos os testes
./gradlew test

# Rodar apenas testes unitários
./gradlew test --tests "*Test"

# Rodar apenas testes de integração
./gradlew test --tests "*IT"

# Gerar relatório de cobertura
./gradlew jacocoTestReport

# Aplicar migrations manualmente
./gradlew flywayMigrate

# Build do JAR
./gradlew build -x test

9. Anti-Padrões (NUNCA FAZER ISSO)
9.1 Código
❌ Field injection (@Autowired em field)
❌ Retornar Entity JPA direto do Controller (sempre usar DTO)
❌ Lógica de negócio em Controllers ou Repositories
❌ catch (Exception e) genérico - sempre capturar exceção específica
❌ Logs com System.out.println ou string concatenada (usar log.info("msg {}", arg))
❌ var em services ou domains sem justificativa
❌ !! em código de produção
9.2 Banco de Dados
❌ SELECT * em produção (listar colunas explicitamente)
❌ UPDATE em exercises publicados (criar nova versão)
❌ FKs sem ON DELETE CASCADE ou ON DELETE RESTRICT definido
❌ N+1 queries (usar @EntityGraph ou JOIN FETCH)
❌ Salvar arquivos binários (MP3, imagens) no Postgres (usar S3/R2)
9.3 API
❌ Endpoints sem paginação em listas
❌ Retornar 200 OK para erros (usar 4xx/5xx adequados)
❌ Expor stack traces em responses de produção
❌ IDs sequenciais (/users/123) - sempre UUIDs
❌ Misturar português e inglês em nomes de campos JSON (usar inglês)
9.4 Pedagógico/Domínio
❌ Gerar exercícios ou regras gramaticais via LLM em runtime (risco de alucinação)
❌ Usar STT genérico (Whisper) para avaliar pronúncia (dá falso positivo)
❌ TTS em tempo real (custo proibitivo) - usar MP3s pré-gerados
❌ Exercícios sem feedback_on_error explicativo


11. Decisões Arquiteturais (ADRs)
Antes de questionar uma decisão de design, leia o ADR correspondente em docs/decisions/:
ADR-001 - Stack Java/Kotlin (por que não Python)
ADR-002 - PostgreSQL + JSONB (por que não MongoDB)
ADR-003 - Monolítico Modular (por que não microsserviços)
ADR-004 - MP3 pré-gerado (por que não TTS em runtime)
ADR-005 - Exercícios imutáveis com versionamento
ADR-006 - Particionamento de exercise_attempts
Se você (agente) acredita que uma decisão deve ser revista, não mude o código. Proponha um novo ADR em docs/decisions/ com justificativa.

12. Fluxo de Trabalho Sugerido
Quando receber uma tarefa:
Leia este arquivo inteiro (principalmente seções 3 e 9)
Identifique a feature afetada (auth, users, curriculum, progress, ai, common)
Verifique se há ADR relevante para a mudança
Escreva testes PRIMEIRO (TDD) seguindo padrões da seção 7
Implemente seguindo convenções das seções 5 e 6
Rode todos os testes (./gradlew test) antes de propor a mudança
Atualize docs se mudar API ou schema



