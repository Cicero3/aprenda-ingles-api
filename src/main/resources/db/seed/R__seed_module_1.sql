-- R__seed_module_1.sql  (migration REPETÍVEL do Flyway)
-- Seed completo do Módulo 1 (Verbo TO BE) para dev/testes.
-- Baseado no roteiro "O Mestre dos Disfarces", fatiado em 5 lições para
-- reduzir carga cognitiva (A1 não empilha 3 tempos verbais numa aula só).
--
-- POR QUE REPETÍVEL (R__): mudanças de conteúdo NÃO quebram o boot. Migrations
-- versionadas (V) validam checksum e falham se o arquivo muda após aplicado; uma
-- repetível apenas RE-EXECUTA quando seu checksum muda. Roda após as V e fica só
-- em dev/teste (prod não carrega db/seed).
--
-- Estratégia de conflito:
--   * módulos e lições: ON CONFLICT DO UPDATE (upsert) — editar título/teoria
--     reflete no DB na próxima subida, sem reset.
--   * exercícios: ON CONFLICT DO NOTHING — exercícios são IMUTÁVEIS após publicação
--     (CLAUDE.md §3.2). Para alterar um exercício, crie uma nova versão (novo id).
--
-- Convenções aplicadas:
--   * UUIDs fixos e legíveis (referenciáveis por testes).
--   * question_payload NÃO contém o gabarito (é exposto ao aluno em GET /lessons/{id}).
--     O acerto é resolvido por correct_answer.text + normalização case-insensitive
--     do Grader, então não precisamos (nem devemos) repetir a resposta no payload.
--   * feedback_on_error endereça o erro CLÁSSICO do brasileiro em cada exercício.
--   * audio_url são placeholders; MP3s reais virão dos scripts offline (CLAUDE.md §1).
--
-- Mapa de lições:
--   L1  Pronomes + Presente (am/is/are)        -> base
--   L2  Negativa + Contrações (presente)        -> posição do NOT, isn't/aren't, I'm not
--   L3  Passado (was/were)                       -> redução de 3 para 2 "fantasias"
--   L4  Futuro (will be)                         -> forma universal
--   L5  Revisão mista                            -> exercícios cruzados (recuperação ativa)

-- =====================================================================
-- MÓDULO
-- =====================================================================
INSERT INTO modules (id, title, description, level, order_index, is_published)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'O Mestre dos Disfarces — Verbo TO BE',
    'O verbo TO BE como um ator que troca de fantasia conforme o pronome e o tempo. Presente, passado, futuro, negativa e contrações.',
    'A1',
    1,
    true
) ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title, description = EXCLUDED.description, level = EXCLUDED.level,
    order_index = EXCLUDED.order_index, is_published = EXCLUDED.is_published;

-- =====================================================================
-- LIÇÃO 1 — Pronomes + Presente (am/is/are)
-- =====================================================================
INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222001',
    '11111111-1111-1111-1111-111111111111',
    'Os Protagonistas e o Agora (Presente)',
    1,
    15,
    '{
      "theory_html": "<p>O verbo TO BE significa <b>SER</b> ou <b>ESTAR</b>. Pense nele como um ator com superpoderes: ele troca de \"fantasia\" dependendo de quem fala. No presente ele tem 3 disfarces: <b>AM</b>, <b>IS</b>, <b>ARE</b>.</p>",
      "pronouns": [
        { "en": "I", "pt": "Eu" },
        { "en": "You", "pt": "Você / Vocês (mesma palavra; o contexto decide)" },
        { "en": "He", "pt": "Ele" },
        { "en": "She", "pt": "Ela" },
        { "en": "It", "pt": "Isso (animais, objetos, clima, situações — nunca pessoa)" },
        { "en": "We", "pt": "Nós" },
        { "en": "They", "pt": "Eles / Elas (plural universal)" }
      ],
      "conjugation_table": {
        "I": "am", "He": "is", "She": "is", "It": "is",
        "You": "are", "We": "are", "They": "are"
      },
      "examples": [
        { "en": "I am happy.", "pt": "Eu estou feliz / Eu sou feliz." },
        { "en": "She is smart.", "pt": "Ela é inteligente." },
        { "en": "We are ready.", "pt": "Nós estamos prontos." },
        { "en": "It is cold.", "pt": "Está frio. (note: o inglês EXIGE o sujeito It)" }
      ],
      "ser_vs_estar": "I am a teacher (SER = identidade) vs. I am tired (ESTAR = estado temporário). A frase inteira deixa óbvio qual é.",
      "common_errors": [
        "Omitir o sujeito com clima/horas: ERRADO \"Is cold\" -> CERTO \"It is cold\".",
        "\"I are/is\": I tem roupa exclusiva, sempre AM."
      ],
      "pronunciation": {
        "tip_th": "THEY: ponta da língua entre os dentes para o som do TH.",
        "minimal_pairs": [
          { "a": "is /ɪz/", "b": "ease /iːz/" },
          { "a": "it /ɪt/", "b": "eat /iːt/" },
          { "a": "this /ðɪs/", "b": "these /ðiːz/" }
        ]
      },
      "practice_template": [
        "I am ___.",
        "My city is ___.",
        "My friends are ___."
      ],
      "audio_urls": { "intro": "https://cdn.app.com/m1_l1_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO UPDATE SET
    module_id = EXCLUDED.module_id, title = EXCLUDED.title, order_index = EXCLUDED.order_index,
    estimated_minutes = EXCLUDED.estimated_minutes, content_jsonb = EXCLUDED.content_jsonb,
    is_published = EXCLUDED.is_published, updated_at = now();

-- L1 ex1: multiple_choice — IS para terceira pessoa
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000101',
    '22222222-2222-2222-2222-222222222001',
    1,
    'multiple_choice',
    '{
      "prompt": "Qual a forma correta de \"Ele está\"?",
      "audio_url": "https://cdn.app.com/m1_l1_q1.mp3",
      "options": [
        { "index": 0, "text": "He was" },
        { "index": 1, "text": "He are" },
        { "index": 2, "text": "He is" },
        { "index": 3, "text": "He am" }
      ]
    }'::jsonb,
    '{ "selected_index": 2 }'::jsonb,
    'Para HE/SHE/IT (terceira pessoa) usamos IS no presente. Ex.: "She is happy."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L1 ex2: fill_blank — ARE para plural
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000102',
    '22222222-2222-2222-2222-222222222001',
    2,
    'fill_blank',
    '{
      "sentence_template": "They ___ my friends.",
      "audio_url": "https://cdn.app.com/m1_l1_q2.mp3",
      "hint": "THEY é plural — qual das três fantasias do presente ele usa?"
    }'::jsonb,
    '{ "text": "are" }'::jsonb,
    'THEY é plural, então usa ARE: "They are my friends."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L1 ex3: multiple_choice — It como sujeito obrigatório (clima)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000103',
    '22222222-2222-2222-2222-222222222001',
    3,
    'multiple_choice',
    '{
      "prompt": "Como se diz \"Está frio hoje\"?",
      "audio_url": "https://cdn.app.com/m1_l1_q3.mp3",
      "options": [
        { "index": 0, "text": "Is cold today." },
        { "index": 1, "text": "It is cold today." },
        { "index": 2, "text": "Cold today." },
        { "index": 3, "text": "It are cold today." }
      ]
    }'::jsonb,
    '{ "selected_index": 1 }'::jsonb,
    'Com clima, horas e distância o inglês EXIGE um sujeito, mesmo quando o português não usa: "It is cold today."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L1 ex4: translation — SER (identidade)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000104',
    '22222222-2222-2222-2222-222222222001',
    4,
    'translation',
    '{
      "source_text": "Eu sou um professor.",
      "source_audio_url": "https://cdn.app.com/m1_l1_t1_pt.mp3",
      "target_audio_url": "https://cdn.app.com/m1_l1_t1_en.mp3",
      "case_sensitive": false,
      "ignore_punctuation": true
    }'::jsonb,
    '{ "text": "I am a teacher." }'::jsonb,
    '"Eu sou" = I AM. Aqui TO BE é SER (identidade). Não esqueça o artigo "a": "I am a teacher."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- LIÇÃO 2 — Negativa + Contrações (presente)
-- =====================================================================
INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222002',
    '11111111-1111-1111-1111-111111111111',
    'O Lado do Não e o Caminho Curto (Negativa e Contrações)',
    2,
    15,
    '{
      "theory_html": "<p>Para negar, chamamos o guarda-costas <b>NOT</b>. No presente, ele fica SEMPRE <b>depois</b> do verbo TO BE. Na fala rápida, as palavras se colam: as <b>contrações</b>.</p>",
      "negative_table": {
        "I": "I am not", "He": "He is not", "She": "She is not", "It": "It is not",
        "You": "You are not", "We": "We are not", "They": "They are not"
      },
      "contractions": [
        { "full": "I am not", "short": "I''m not", "note": "I am NUNCA vira amn''t" },
        { "full": "is not", "short": "isn''t" },
        { "full": "are not", "short": "aren''t" }
      ],
      "examples": [
        { "en": "He is not / He isn''t at home.", "pt": "Ele não está em casa." },
        { "en": "We are not / We aren''t tired.", "pt": "Nós não estamos cansados." }
      ],
      "common_errors": [
        "ORDEM: o NOT vem DEPOIS do verbo. ERRADO \"He no is\" -> CERTO \"He is not\". Não traduza palavra por palavra do português (\"Ele não é\").",
        "I am não tem contração com not no verbo: usa-se I''m not (não existe \"amn''t\")."
      ],
      "usage_note": "Forma completa (I am not!) soa enfática; contração (isn''t) é o padrão casual.",
      "practice_template": [
        "I am not ___.",
        "He isn''t ___.",
        "They aren''t ___."
      ],
      "audio_urls": { "intro": "https://cdn.app.com/m1_l2_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO UPDATE SET
    module_id = EXCLUDED.module_id, title = EXCLUDED.title, order_index = EXCLUDED.order_index,
    estimated_minutes = EXCLUDED.estimated_minutes, content_jsonb = EXCLUDED.content_jsonb,
    is_published = EXCLUDED.is_published, updated_at = now();

-- L2 ex1: fill_blank — posição do NOT (o erro nº1 do brasileiro)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000201',
    '22222222-2222-2222-2222-222222222002',
    1,
    'fill_blank',
    '{
      "sentence_template": "She is ___ a doctor.",
      "audio_url": "https://cdn.app.com/m1_l2_q1.mp3",
      "hint": "\"Ela NÃO é médica.\" Onde entra a palavra de negação no inglês?"
    }'::jsonb,
    '{ "text": "not" }'::jsonb,
    'No inglês o NOT vem DEPOIS do verbo TO BE: "She is NOT a doctor." (no português é antes: "Ela NÃO é").',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L2 ex2: multiple_choice — escolher a contração correta (aren't)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000202',
    '22222222-2222-2222-2222-222222222002',
    2,
    'multiple_choice',
    '{
      "prompt": "Contração de \"We are not\":",
      "audio_url": "https://cdn.app.com/m1_l2_q2.mp3",
      "options": [
        { "index": 0, "text": "We isn''t" },
        { "index": 1, "text": "We amn''t" },
        { "index": 2, "text": "We aren''t" },
        { "index": 3, "text": "We weren''t" }
      ]
    }'::jsonb,
    '{ "selected_index": 2 }'::jsonb,
    '"are not" vira "aren''t". "isn''t" é só para is, e "weren''t" é passado.',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L2 ex3: fill_blank — I'm not (a exceção)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000203',
    '22222222-2222-2222-2222-222222222002',
    3,
    'fill_blank',
    '{
      "sentence_template": "I ___ not ready. (use a contração de \"I am\")",
      "audio_url": "https://cdn.app.com/m1_l2_q3.mp3",
      "hint": "I am tem uma contração especial e fica antes do not."
    }'::jsonb,
    '{ "text": "I''m" }'::jsonb,
    '"I am" vira "I''m" e o not vem depois: "I''m not ready." Não existe "amn''t".',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- LIÇÃO 3 — Passado (was / were)
-- =====================================================================
INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222003',
    '11111111-1111-1111-1111-111111111111',
    'A Máquina do Tempo (Passado)',
    3,
    12,
    '{
      "theory_html": "<p>No passado o ator ficou com preguiça: reduziu de três fantasias para apenas <b>duas</b>. Tudo que era AM/IS vira <b>WAS</b>; tudo que era ARE vira <b>WERE</b>.</p>",
      "conjugation_table": {
        "I": "was", "He": "was", "She": "was", "It": "was",
        "You": "were", "We": "were", "They": "were"
      },
      "mapping": "am/is -> was   |   are -> were",
      "examples": [
        { "en": "I was at home.", "pt": "Eu estava em casa." },
        { "en": "They were happy.", "pt": "Eles estavam felizes." }
      ],
      "negative": [
        { "full": "was not", "short": "wasn''t" },
        { "full": "were not", "short": "weren''t" }
      ],
      "common_errors": [
        "Misturar com o presente: ERRADO \"They was\" -> CERTO \"They were\" (plural usa WERE).",
        "\"I were\" não existe no passado afirmativo padrão: use \"I was\"."
      ],
      "practice_template": [
        "Yesterday I was ___.",
        "Last year they were ___."
      ],
      "audio_urls": { "intro": "https://cdn.app.com/m1_l3_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO UPDATE SET
    module_id = EXCLUDED.module_id, title = EXCLUDED.title, order_index = EXCLUDED.order_index,
    estimated_minutes = EXCLUDED.estimated_minutes, content_jsonb = EXCLUDED.content_jsonb,
    is_published = EXCLUDED.is_published, updated_at = now();

-- L3 ex1: multiple_choice — WERE para plural
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000301',
    '22222222-2222-2222-2222-222222222003',
    1,
    'multiple_choice',
    '{
      "prompt": "\"Eles estavam aqui.\"",
      "audio_url": "https://cdn.app.com/m1_l3_q1.mp3",
      "options": [
        { "index": 0, "text": "They was here." },
        { "index": 1, "text": "They were here." },
        { "index": 2, "text": "They are here." },
        { "index": 3, "text": "They is here." }
      ]
    }'::jsonb,
    '{ "selected_index": 1 }'::jsonb,
    'No passado, o plural (They/We/You) usa WERE: "They were here."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L3 ex2: fill_blank — WAS para I
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000302',
    '22222222-2222-2222-2222-222222222003',
    2,
    'fill_blank',
    '{
      "sentence_template": "I ___ at school yesterday.",
      "audio_url": "https://cdn.app.com/m1_l3_q2.mp3",
      "hint": "am/is no presente viram a MESMA palavra no passado."
    }'::jsonb,
    '{ "text": "was" }'::jsonb,
    'Tudo que era AM/IS vira WAS no passado: "I was at school yesterday."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L3 ex3: translation — passado plural
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000303',
    '22222222-2222-2222-2222-222222222003',
    3,
    'translation',
    '{
      "source_text": "Nós estávamos cansados.",
      "source_audio_url": "https://cdn.app.com/m1_l3_t1_pt.mp3",
      "target_audio_url": "https://cdn.app.com/m1_l3_t1_en.mp3",
      "case_sensitive": false,
      "ignore_punctuation": true
    }'::jsonb,
    '{ "text": "We were tired." }'::jsonb,
    '"Nós estávamos" = WE WERE (passado plural de TO BE).',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- LIÇÃO 4 — Futuro (will be)
-- =====================================================================
INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222004',
    '11111111-1111-1111-1111-111111111111',
    'A Varinha Mágica (Futuro)',
    4,
    10,
    '{
      "theory_html": "<p>O futuro é democrático: a palavra <b>will</b> é uma varinha mágica que dá a MESMA roupa para todo mundo. Não importa o pronome — todos usam <b>WILL BE</b>.</p>",
      "conjugation_table": {
        "I": "will be", "He": "will be", "She": "will be", "It": "will be",
        "You": "will be", "We": "will be", "They": "will be"
      },
      "examples": [
        { "en": "They will be here.", "pt": "Eles estarão aqui." },
        { "en": "I will be a doctor.", "pt": "Eu serei médico." }
      ],
      "negative": [
        { "full": "will not be", "short": "won''t be", "note": "will not vira o \"alienígena\" won''t" }
      ],
      "extra": "\"will\" joga QUALQUER verbo para o futuro, não só o TO BE: I will work = Eu trabalharei.",
      "common_errors": [
        "Conjugar o be depois de will: ERRADO \"She will is\" -> CERTO \"She will be\".",
        "Negativa: \"will not\" contrai para won''t (não \"willn''t\")."
      ],
      "practice_template": [
        "Tomorrow I will be ___.",
        "Next year we will be ___."
      ],
      "audio_urls": { "intro": "https://cdn.app.com/m1_l4_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO UPDATE SET
    module_id = EXCLUDED.module_id, title = EXCLUDED.title, order_index = EXCLUDED.order_index,
    estimated_minutes = EXCLUDED.estimated_minutes, content_jsonb = EXCLUDED.content_jsonb,
    is_published = EXCLUDED.is_published, updated_at = now();

-- L4 ex1: fill_blank — will be invariável
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000401',
    '22222222-2222-2222-2222-222222222004',
    1,
    'fill_blank',
    '{
      "sentence_template": "She will ___ a great teacher.",
      "audio_url": "https://cdn.app.com/m1_l4_q1.mp3",
      "hint": "Depois de WILL, o verbo TO BE não muda — fica na forma base."
    }'::jsonb,
    '{ "text": "be" }'::jsonb,
    'Depois de WILL, o TO BE fica sempre BE (forma base): "She will be a great teacher." Nunca "will is".',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L4 ex2: multiple_choice — won't be (negativa do futuro)
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000402',
    '22222222-2222-2222-2222-222222222004',
    2,
    'multiple_choice',
    '{
      "prompt": "Contração de \"will not be\":",
      "audio_url": "https://cdn.app.com/m1_l4_q2.mp3",
      "options": [
        { "index": 0, "text": "willn''t be" },
        { "index": 1, "text": "won''t be" },
        { "index": 2, "text": "wasn''t be" },
        { "index": 3, "text": "don''t be" }
      ]
    }'::jsonb,
    '{ "selected_index": 1 }'::jsonb,
    '"will not" vira o irregular "won''t": "I won''t be late." (não existe "willn''t").',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L4 ex3: translation — futuro universal
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000403',
    '22222222-2222-2222-2222-222222222004',
    3,
    'translation',
    '{
      "source_text": "Eles estarão aqui.",
      "source_audio_url": "https://cdn.app.com/m1_l4_t1_pt.mp3",
      "target_audio_url": "https://cdn.app.com/m1_l4_t1_en.mp3",
      "case_sensitive": false,
      "ignore_punctuation": true
    }'::jsonb,
    '{ "text": "They will be here." }'::jsonb,
    '"Estarão" = WILL BE (igual para todos os pronomes): "They will be here."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- LIÇÃO 5 — Revisão mista (recuperação ativa, tempos cruzados)
-- =====================================================================
INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222005',
    '11111111-1111-1111-1111-111111111111',
    'Campo de Treinamento (Revisão Mista)',
    5,
    15,
    '{
      "theory_html": "<p>Hora de cruzar os três tempos. Tente resolver SEM olhar as anotações — forçar o cérebro a recuperar a informação fixa muito mais do que reler.</p>",
      "summary_table": {
        "present": { "I": "am", "he/she/it": "is", "you/we/they": "are" },
        "past":    { "I/he/she/it": "was", "you/we/they": "were" },
        "future":  { "all": "will be" }
      },
      "challenge": "Crie 3 frases sobre a sua vida: uma no presente, uma no passado e uma no futuro.",
      "mindset": "Quanto mais você suar no treino, menos vai sangrar na conversa real. Repita em voz alta até a fala fluir.",
      "audio_urls": { "intro": "https://cdn.app.com/m1_l5_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO UPDATE SET
    module_id = EXCLUDED.module_id, title = EXCLUDED.title, order_index = EXCLUDED.order_index,
    estimated_minutes = EXCLUDED.estimated_minutes, content_jsonb = EXCLUDED.content_jsonb,
    is_published = EXCLUDED.is_published, updated_at = now();

-- L5 ex1: multiple_choice — distinguir tempo certo
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000501',
    '22222222-2222-2222-2222-222222222005',
    1,
    'multiple_choice',
    '{
      "prompt": "\"Amanhã eu estarei pronto.\"",
      "audio_url": "https://cdn.app.com/m1_l5_q1.mp3",
      "options": [
        { "index": 0, "text": "Tomorrow I am ready." },
        { "index": 1, "text": "Tomorrow I was ready." },
        { "index": 2, "text": "Tomorrow I will be ready." },
        { "index": 3, "text": "Tomorrow I are ready." }
      ]
    }'::jsonb,
    '{ "selected_index": 2 }'::jsonb,
    '"Amanhã" pede futuro: WILL BE. "Tomorrow I will be ready."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L5 ex2: fill_blank — passado plural sob revisão
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000502',
    '22222222-2222-2222-2222-222222222005',
    2,
    'fill_blank',
    '{
      "sentence_template": "Last night we ___ at the party.",
      "audio_url": "https://cdn.app.com/m1_l5_q2.mp3",
      "hint": "\"Last night\" é passado. WE é plural."
    }'::jsonb,
    '{ "text": "were" }'::jsonb,
    'Passado + plural (WE) = WERE: "Last night we were at the party."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- L5 ex3: translation — negativa no presente
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-0000-0000-0000-000000000503',
    '22222222-2222-2222-2222-222222222005',
    3,
    'translation',
    '{
      "source_text": "Ela não está em casa.",
      "source_audio_url": "https://cdn.app.com/m1_l5_t1_pt.mp3",
      "target_audio_url": "https://cdn.app.com/m1_l5_t1_en.mp3",
      "case_sensitive": false,
      "ignore_punctuation": true
    }'::jsonb,
    '{ "text": "She is not at home." }'::jsonb,
    'O NOT vem DEPOIS do verbo TO BE: "She is not at home."',
    1,
    true
) ON CONFLICT (id) DO NOTHING;
