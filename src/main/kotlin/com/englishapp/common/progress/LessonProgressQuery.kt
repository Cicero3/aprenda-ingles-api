package com.englishapp.common.progress

import java.util.UUID

/**
 * Porta de leitura do progresso do aluno exposta a outras features (ex.: curriculum),
 * para enriquecer listagens com status/score por lição sem importar a feature progress
 * diretamente (CLAUDE.md §4). Implementada por um adapter em progress.
 */
interface LessonProgressQuery {
    /**
     * Snapshot do progresso do usuário nas lições informadas.
     * Lições sem registro de progresso ficam AUSENTES do mapa (o chamador assume not_started).
     */
    fun snapshotsFor(userId: UUID, lessonIds: Collection<UUID>): Map<UUID, LessonProgressSnapshot>
}

/** Espelha os estados de progress.domain.LessonStatus, mas vive em common para não vazar a feature. */
enum class LessonProgressStatus {
    not_started,
    in_progress,
    completed,
    mastered;

    /** Lição concluída o suficiente para liberar a próxima na trilha. */
    val isCompleted: Boolean
        get() = this == completed || this == mastered
}

data class LessonProgressSnapshot(
    val lessonId: UUID,
    val status: LessonProgressStatus,
    val bestScore: Double
)
