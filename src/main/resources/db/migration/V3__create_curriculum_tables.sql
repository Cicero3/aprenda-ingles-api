-- V3__create_curriculum_tables.sql
-- Conteúdo pedagógico: módulos, lições e exercícios (imutáveis com versionamento)

CREATE TABLE IF NOT EXISTS modules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    level VARCHAR(10) NOT NULL,
    order_index INTEGER NOT NULL UNIQUE,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_modules_level CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);

CREATE TABLE IF NOT EXISTS lessons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    order_index INTEGER NOT NULL,
    estimated_minutes INTEGER NOT NULL DEFAULT 15,
    content_jsonb JSONB NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_lessons_module_order UNIQUE (module_id, order_index)
);

CREATE TABLE IF NOT EXISTS exercises (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    question_payload JSONB NOT NULL,
    correct_answer JSONB NOT NULL,
    feedback_on_error TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_exercises_lesson_order_version UNIQUE (lesson_id, order_index, version),
    CONSTRAINT chk_exercises_type CHECK (type IN ('multiple_choice', 'fill_blank', 'translation'))
);

CREATE INDEX IF NOT EXISTS idx_lessons_module ON lessons(module_id, order_index);
CREATE INDEX IF NOT EXISTS idx_exercises_lesson ON exercises(lesson_id, is_active);
