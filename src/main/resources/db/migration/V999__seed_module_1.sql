-- V999__seed_module_1.sql
-- Seed mínimo do Módulo 1 (Verbo TO BE) para dev/testes.
-- UUIDs fixos para serem referenciáveis. Idempotente via ON CONFLICT.
-- NOTA: áudios (audio_url) são placeholders; MP3s reais virão dos scripts offline.

INSERT INTO modules (id, title, description, level, order_index, is_published)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Verbo TO BE - Fundamentos',
    'Aprenda a usar o verbo TO BE no presente e no passado.',
    'A1',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO lessons (id, module_id, title, order_index, estimated_minutes, content_jsonb, is_published)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'TO BE - Presente Afirmativo',
    1,
    15,
    '{
      "theory_html": "<p>O verbo TO BE significa SER ou ESTAR.</p>",
      "conjugation_table": {
        "I": "am", "He": "is", "She": "is", "It": "is",
        "You": "are", "We": "are", "They": "are"
      },
      "examples": [
        { "en": "I am a student.", "pt": "Eu sou um estudante." },
        { "en": "She is happy.", "pt": "Ela está feliz." }
      ],
      "audio_urls": { "intro": "https://cdn.app.com/m1_l1_intro.mp3" }
    }'::jsonb,
    true
) ON CONFLICT (id) DO NOTHING;

-- Exercício 1: multiple_choice
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    1,
    'multiple_choice',
    '{
      "prompt": "Qual a maneira correta de afirmar: ''Ele está''?",
      "audio_url": "https://cdn.app.com/m1_l1_q5.mp3",
      "options": [
        { "index": 0, "text": "He was" },
        { "index": 1, "text": "He are" },
        { "index": 2, "text": "He is" },
        { "index": 3, "text": "He were" }
      ]
    }'::jsonb,
    '{ "selected_index": 2 }'::jsonb,
    'Lembre-se: para HE/SHE/IT usamos IS no presente. Exemplo: ''She is happy.''',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- Exercício 2: fill_blank
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    '22222222-2222-2222-2222-222222222222',
    2,
    'fill_blank',
    '{
      "sentence_template": "She ___ a teacher.",
      "audio_url": "https://cdn.app.com/m1_l1_fib1.mp3",
      "accepted_answers": ["is", "IS", "Is"],
      "hint": "Use o verbo TO BE no presente para SHE"
    }'::jsonb,
    '{ "text": "is" }'::jsonb,
    'Para SHE no presente, usamos IS. ''She IS a teacher.''',
    1,
    true
) ON CONFLICT (id) DO NOTHING;

-- Exercício 3: translation
INSERT INTO exercises (id, lesson_id, order_index, type, question_payload, correct_answer, feedback_on_error, version, is_active)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    '22222222-2222-2222-2222-222222222222',
    3,
    'translation',
    '{
      "source_text": "Nós estávamos cansados.",
      "source_audio_url": "https://cdn.app.com/m1_l5_trans1_pt.mp3",
      "target_audio_url": "https://cdn.app.com/m1_l5_trans1_en.mp3",
      "accepted_answers": ["We were tired.", "We were tired", "we were tired."],
      "case_sensitive": false,
      "ignore_punctuation": true
    }'::jsonb,
    '{ "text": "We were tired." }'::jsonb,
    '''Nós estávamos'' = WE WERE (passado de TO BE para plural).',
    1,
    true
) ON CONFLICT (id) DO NOTHING;
