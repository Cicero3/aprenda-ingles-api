# Skills - Procedimentos Operacionais do Projeto

> Catálogo de habilidades acionáveis por agentes de IA neste repositório.
> Cada skill é um procedimento passo-a-passo para tarefas recorrentes.
> Leia o `CLAUDE.md` antes de executar qualquer skill.

---

## 📚 Índice de Skills

| ID | Skill | Quando Usar |
|----|-------|-------------|
| SK-01 | Criar nova lição pedagógica | Ao adicionar conteúdo ao currículo |
| SK-02 | Criar exercício | Ao criar questões de qualquer tipo |
| SK-03 | Criar migration Flyway | Ao alterar schema do banco |
| SK-04 | Adicionar endpoint REST | Ao criar nova rota da API |
| SK-05 | Gerar teste unitário (MockK) | Ao testar services/domains |
| SK-06 | Gerar teste de integração | Ao testar controllers + banco |
| SK-07 | Gerar MP3 via Edge-TTS | Ao criar áudio para lição/exercício |
| SK-08 | Invalidar cache Redis | Ao atualizar conteúdo publicado |
| SK-09 | Revisar Pull Request | Antes de merge em main |
| SK-10 | Debugar erro JSONB | Ao falhar validação de payload |
| SK-11 | Versionar exercício existente | Ao corrigir exercício publicado |
| SK-12 | Seed data para ambiente dev | Ao preparar ambiente local |

---

## SK-01: Criar Nova Lição Pedagógica

**Quando usar:** Adicionar nova lição ao currículo (Módulo 1 ou futuros).

**Pré-requisitos:**
- Saber o `module_id` alvo
- Ter o conteúdo teórico pronto (Markdown/HTML)
- Ter os exercícios definidos (usar SK-02)

**Passos:**

1. **Escolher ordem:** Verificar última `order_index` do módulo em `lessons`
2. **Criar migration de seed:**
   ```sql
   -- V{N}__seed_lesson_{slug}.sql
   INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
   VALUES (
     gen_random_uuid(),
     '{module_id}',
     '{title}',
     {order_index},
     {estimated_minutes},
     '{content_json}'::jsonb,
     true
   );

Estruturar content_jsonb:

{
  "theory_html": "<p>Explicação...</p>",
  "conjugation_table": {...},
  "examples": [
    {"en": "I am happy.", "pt": "Eu estou feliz."}
  ],
  "tips": ["Dica 1", "Dica 2"]
}


Gerar MP3s dos exemplos usando SK-07
Criar exercícios via SK-02 vinculados ao lesson_id
Invalidar cache: POST /admin/cache/invalidate?pattern=module:list

SK-02: Criar Exercício
Quando usar: Adicionar nova questão a uma lição existente.
Pré-requisitos:
lesson_id da lição alvo
Tipo definido: multiple_choice | fill_blank | translation
Passos:
Escolher order_index: Próximo disponível na lição
Construir payload JSON conforme tipo (ver design.md seção 5):
Multiple Choice:

{
  "prompt": "Qual a forma correta de 'Ele está'?",
  "audio_url": "https://cdn.../m1_l1_q5.mp3",
  "options": [
    {"index": 0, "text": "He was"},
    {"index": 1, "text": "He are"},
    {"index": 2, "text": "He is"},
    {"index": 3, "text": "He were"}
  ]
}

Correct answer: {"selected_index": 2}
Fill-in-the-Blank:

{
  "sentence_template": "She ___ a teacher.",
  "audio_url": "https://cdn.../m1_l1_fib1.mp3",
  "accepted_answers": ["is", "IS", "Is"],
  "hint": "Verbo TO BE no presente para SHE"
}

Correct answer: {"text": "is"}
Translation:

{
  "source_text": "Nós estávamos cansados.",
  "source_audio_url": "...",
  "target_audio_url": "...",
  "accepted_answers": ["We were tired.", "We were tired"],
  "case_sensitive": false,
  "ignore_punctuation": true
}

Escrever feedback_on_error explicativo em PT-BR (obrigatório)
Criar migration de seed:

INSERT INTO exercises (
  id, lesson_id, order_index, type,
  question_payload, correct_answer,
  feedback_on_error, version, is_active
) VALUES (
  gen_random_uuid(), '{lesson_id}', {order}, '{type}',
  '{payload}'::jsonb, '{answer}'::jsonb,
  '{feedback}', 1, true
);

SK-03: Criar Migration Flyway
Quando usar: Qualquer alteração no schema do PostgreSQL.
Regras (do CLAUDE.md):
Nome: V{N}__{descricao_curta}.sql
Idempotente (pode rodar múltiplas vezes sem quebrar)
Nunca alterar migration já aplicada em produção
DDL e DML em arquivos separados
Passos:
Identificar próximo número: Listar src/main/resources/db/migration/
Criar arquivo:

src/main/resources/db/migration/V{N}__{descricao}.sql

Escrever SQL idempotente:

-- ✅ CORRETO (idempotente)
CREATE TABLE IF NOT EXISTS new_table (...);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS new_col TEXT;
CREATE INDEX IF NOT EXISTS idx_new ON table(col);

-- ❌ ERRADO (não idempotente)
CREATE TABLE new_table (...);  -- falha na 2ª execução
ALTER TABLE lessons ADD COLUMN new_col TEXT;

Testar localmente

docker-compose down -v  # limpa banco
docker-compose up -d
./gradlew flywayMigrate

Commit:

git add src/main/resources/db/migration/V{N}__*.sql
git commit -m "db: add {descricao}"

SK-04: Adicionar Endpoint REST
Quando usar: Criar nova rota na API.
Convenções obrigatórias:
Path versionado: /api/v1/...
UUIDs em paths (nunca IDs numéricos)
Response envelope: {"data": ..., "meta": ...}
Timestamps em UTC ISO 8601
Passos:
Criar DTO de request/response em feature/api/dto/

data class CreateXRequest(
    @field:NotNull val fieldA: UUID,
    @field:NotBlank val fieldB: String
)

data class XResponse(
    val id: UUID,
    val fieldA: UUID,
    val fieldB: String,
    val createdAt: Instant
)

Criar Controller em feature/api/

@RestController
@RequestMapping("/api/v1/x")
@Tag(name = "X")
class XController(private val xService: XService) {
    
    @PostMapping
    @Operation(summary = "Create X")
    fun create(
        @Valid @RequestBody request: CreateXRequest,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<XResponse>> {
        val result = xService.create(request, user.id)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = result))
    }
}

Criar Service em feature/application/
Adicionar testes (SK-06 para controller, SK-05 para service)
Atualizar api-contracts.md com exemplo de request/response

SK-05: Gerar Teste Unitário (MockK)
Quando usar: Testar services, validators, mappers.
Template base:

@ExtendWith(MockKExtension::class)
class LessonServiceTest {
    
    @MockK
    private lateinit var repository: LessonRepository
    
    @MockK
    private lateinit var cacheManager: CacheManager
    
    @InjectMockKs
    private lateinit var service: LessonService
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }
    
    @Test
    fun `should return lesson when exists`() {
        // Given
        val id = UUID.randomUUID()
        val lesson = Lesson(id = id, title = "Test")
        every { repository.findById(id) } returns Optional.of(lesson)
        
        // When
        val result = service.findById(id)
        
        // Then
        assertThat(result.id).isEqualTo(id)
        verify(exactly = 1) { repository.findById(id) }
    }
    
    @Test
    fun `should throw when lesson not found`() {
        // Given
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns Optional.empty()
        
        // When/Then
        assertThatThrownBy { service.findById(id) }
            .isInstanceOf(EntityNotFoundException::class.java)
    }
}

Regras:
Nomenclatura: should {comportamento} when {condição}
Máximo 1 assertion lógica por teste (múltiplos asserts relacionados OK)
Padrão Given/When/Then comentado
Nomes de variáveis descritivos

SK-06: Gerar Teste de Integração (Testcontainers)
Quando usar: Testar controllers + banco + Redis reais.
Template base:

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LessonControllerIT {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")
        
        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
    }
    
    @Test
    fun `should return 200 and lesson when authenticated`() {
        // Given
        val token = createTestUserAndGetToken()
        val lessonId = seedLesson()
        
        // When
        val response = restTemplate.exchange(
            "/api/v1/lessons/$lessonId",
            HttpMethod.GET,
            HttpEntity<Void>(headersWith(token)),
            LessonResponse::class.java
        )
        
        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.id).isEqualTo(lessonId)
    }
    
    @Test
    fun `should return 401 when no token`() {
        val response = restTemplate.getForEntity(
            "/api/v1/lessons/${UUID.randomUUID()}",
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}

SK-07: Gerar MP3 via Edge-TTS
Quando usar: Criar áudio para teoria, exemplos, enunciados de exercícios.
Passos:
Listar vozes disponíveis:

python scripts/list_voices.py | grep en-US

Vozes recomendadas:
en-US-JennyNeural (feminina, clara)
en-US-GuyNeural (masculina, natural)
en-GB-SoniaNeural (sotaque britânico)

Gerar áudio:

python scripts/generate_audio.py \
  --text "I am a student." \
  --output assets/audio/m1_l1_ex1.mp3 \
  --voice en-US-JennyNeural

Upload para storage:

python scripts/upload_to_r2.py \
  --local assets/audio/m1_l1_ex1.mp3 \
  --key m1/l1/ex1.mp3

Atualizar reference no DB: Colocar URL pública no question_payload ou content_jsonb
Script base (scripts/generate_audio.py):

import argparse
import asyncio
import edge_tts

async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--voice", default="en-US-JennyNeural")
    args = parser.parse_args()
    
    communicate = edge_tts.Communicate(args.text, args.voice)
    await communicate.save(args.output)
    print(f"Generated: {args.output}")

if __name__ == "__main__":
    asyncio.run(main())

SK-08: Invalidar Cache Redis
Quando usar: Após atualizar conteúdo publicado (lição, exercício).
Comandos:

# Invalidar lição específica
redis-cli DEL "lesson:{lesson_id}"

# Invalidar todas as lições
redis-cli --scan --pattern "lesson:*" | xargs redis-cli DEL

# Invalidar lista de módulos
redis-cli DEL "module:list"

# Via endpoint admin (produção)
curl -X POST "https://api.app.com/admin/cache/invalidate?pattern=lesson:*" \
  -H "Authorization: Bearer {admin_token}"

SK-09: Revisar Pull Request
Checklist obrigatório (ler antes de aprovar):
9.1 Convenções
Segue CLAUDE.md seções 3 (regras absolutas) e 9 (anti-padrões)?
IDs são UUIDs?
Endpoints são versionados /api/v1/?
Timestamps em UTC?
9.2 Segurança
Input validado com @Valid?
Auth necessária nos endpoints protegidos?
Sem dados sensíveis nos logs?
SQL injection prevenido (JPA/parameterized)?
9.3 Performance
Sem N+1 queries (verificar JOIN FETCH)?
Listas paginadas?
Cache Redis usado onde apropriado?
Índices adicionados para novas queries?
9.4 Testes
Testes unitários para lógica nova (SK-05)?
Testes de integração para endpoint novo (SK-06)?
Cobertura > 80% nos arquivos modificados?
9.5 Banco de Dados
Migration idempotente (SK-03)?
Não altera migration existente?
Foreign keys com ON DELETE apropriado?
9.6 Domínio
Não dá UPDATE em exercício publicado (SK-11 em vez disso)?
feedback_on_error presente em novos exercícios?
Não usa LLM para gerar conteúdo pedagógico?

SK-10: Debugar Erro JSONB
Sintomas comuns:
PSQLException: ERROR: invalid input syntax for type json
JsonProcessingException no service
Validação falhando ao salvar exercício/lição
Passos de debug:
Verificar JSON bruto:


SELECT question_payload::text FROM exercises WHERE id = '{id}';

Validar JSON externamente: Colar em https://jsonlint.com/
Verificar aspas escapadas: Strings com aspas internas precisam de \"
Testar cast no Postgres:

SELECT '{"key": "value"}'::jsonb;  -- deve funcionar

Validar no Kotlin antes de salvar

val mapper = jacksonObjectMapper()
val payload: Map<String, Any> = mapper.readValue(jsonString)
// Se lançar exceção, JSON é inválido

SK-11: Versionar Exercício Existente
Quando usar: Corrigir erro em exercício já publicado (tem tentativas de alunos).
⚠️ NUNCA dar UPDATE em exercises publicado. Isso corrompe histórico de tentativas.
Passos:
Desativar versão antiga:

UPDATE exercises 
SET is_active = false 
WHERE id = '{exercise_id}' AND version = {current_version};

Inserir nova versão

INSERT INTO exercises (
  id, lesson_id, order_index, type,
  question_payload, correct_answer,
  feedback_on_error, version, is_active
)
SELECT 
  gen_random_uuid(),  -- NOVO id
  lesson_id, order_index, type,
  '{new_payload}'::jsonb,  -- payload corrigido
  '{new_answer}'::jsonb,
  '{new_feedback}',
  version + 1,  -- versão incrementada
  true
FROM exercises
WHERE id = '{exercise_id}' AND version = {current_version};

Invalidar cache da lição pai (SK-08)

redis-cli DEL "lesson:{lesson_id}"

Documentar mudança em CHANGELOG_EXERCISES.md:

## 2026-07-15 - Exercise {old_id} → {new_id}
- Reason: Typo in option (b)
- Old: "He were"
- New: "He are"

SK-12: Seed Data para Ambiente Dev
Quando usar: Popular banco local com dados de teste após docker-compose up.
Passos:
Criar migration de seed (apenas para dev):

src/main/resources/db/migration/V999__seed_dev_data.sql

Conteúdo mínimo:
1 user admin (admin@test.com / admin123)
1 user aluno (aluno@test.com / aluno123)
Módulo 1 completo (10 lições com exercícios dos PDFs)
Algum progresso (algumas tentativas, algumas lições completas)
Perfil dev only:

# application-dev.yml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/seed

Reset completo

docker-compose down -v
docker-compose up -d
./gradlew bootRun --args='--spring.profiles.active=dev'

