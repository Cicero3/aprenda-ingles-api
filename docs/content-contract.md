# Contrato de Conteúdo — Exercícios

> Fonte de verdade dos formatos JSON de `exercises.question_payload` e
> `exercises.correct_answer`, por tipo. Consumido pelo **frontend** (renderização)
> e pelo **Grader** (correção determinística). Validado por
> [`ExercisePayloadValidator`](../src/main/kotlin/com/englishapp/common/content/ExercisePayloadValidator.kt)
> e travado no CI por `ContentIntegrityIT`.

## Regras gerais

- `question_payload` é **exposto ao aluno** em `GET /lessons/{id}` — **nunca** coloque o
  gabarito nele. A resposta certa vive só em `correct_answer` (não exposta na leitura;
  revelada apenas na correção quando o aluno erra).
- `correct_answer` e `question_payload` são imutáveis após publicação (CLAUDE.md §3.2):
  para corrigir conteúdo, criar nova versão do exercício.
- Campos `*_audio_url` são opcionais; hoje são placeholders (MP3s pré-gerados virão dos
  scripts offline, CLAUDE.md §1).
- `type` ∈ `multiple_choice` | `fill_blank` | `translation`
  (ver [`ExerciseType`](../src/main/kotlin/com/englishapp/common/content/ExerciseType.kt)).

---

## `multiple_choice`

**question_payload**
```jsonc
{
  "prompt": "Qual a forma correta de \"Ele está\"?",   // obrigatório, não-vazio
  "audio_url": "https://cdn.app.com/q1.mp3",            // opcional
  "options": [                                          // array, >= 2 opções
    { "index": 0, "text": "He was" },                   // index inteiro único; text não-vazio
    { "index": 1, "text": "He is" }
  ]
}
```
**correct_answer**
```jsonc
{ "selected_index": 1 }   // inteiro; precisa casar com um options[].index
```
Correção: `selected_index` do aluno == `selected_index` do gabarito.

---

## `fill_blank`

**question_payload**
```jsonc
{
  "sentence_template": "They ___ my friends.",  // obrigatório; precisa conter a lacuna "___"
  "audio_url": "https://cdn.app.com/q2.mp3",     // opcional
  "hint": "THEY é plural...",                    // opcional
  "accepted_answers": ["are"]                    // opcional; array de textos não-vazios
}
```
**correct_answer**
```jsonc
{ "text": "are" }   // obrigatório, não-vazio
```
Correção (case-insensitive, **mantém** pontuação): normaliza espaços + caixa e compara
contra `correct_answer.text` e quaisquer `accepted_answers`.

---

## `translation`

**question_payload**
```jsonc
{
  "source_text": "Eu sou um professor.",          // obrigatório, não-vazio
  "source_audio_url": "...mp3",                    // opcional
  "target_audio_url": "...mp3",                    // opcional
  "case_sensitive": false,                         // opcional (boolean; default false)
  "ignore_punctuation": true,                      // opcional (boolean; default true)
  "accepted_answers": ["I'm a teacher."]           // opcional; array de textos não-vazios
}
```
**correct_answer**
```jsonc
{ "text": "I am a teacher." }   // obrigatório, não-vazio
```
Correção: normaliza espaços; por padrão ignora caixa e pontuação; compara contra
`correct_answer.text` + `accepted_answers`. Use `accepted_answers` para variações válidas
(ex.: contrações).
