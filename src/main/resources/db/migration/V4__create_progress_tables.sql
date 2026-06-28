-- V4__create_progress_tables.sql
-- Progresso do aluno: progresso por lição e tentativas (particionadas por mês)

CREATE TABLE IF NOT EXISTS lesson_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'not_started',
    best_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    attempts_count INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, lesson_id),
    CONSTRAINT chk_lesson_progress_status
        CHECK (status IN ('not_started', 'in_progress', 'completed', 'mastered'))
);

CREATE INDEX IF NOT EXISTS idx_lesson_progress_user ON lesson_progress(user_id);

-- Tabela PARTICIONADA por mês (séries temporais de alto volume).
-- A PK precisa incluir a chave de particionamento (created_at).
CREATE TABLE IF NOT EXISTS exercise_attempts (
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

CREATE INDEX IF NOT EXISTS idx_attempts_user_date ON exercise_attempts(user_id, created_at DESC);

-- Partições mensais. Criar novas via migration antes de virar o mês (CLAUDE.md §6.3).
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_06 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_07 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_08 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_09 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_10 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_11 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE IF NOT EXISTS exercise_attempts_2026_12 PARTITION OF exercise_attempts
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
