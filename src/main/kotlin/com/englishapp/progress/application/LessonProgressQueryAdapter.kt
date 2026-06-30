package com.englishapp.progress.application

import com.englishapp.common.progress.LessonProgressQuery
import com.englishapp.common.progress.LessonProgressSnapshot
import com.englishapp.common.progress.LessonProgressStatus
import com.englishapp.progress.infrastructure.LessonProgressRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Implementa a porta [LessonProgressQuery] sobre o repositório de progresso.
 * Permite que curriculum leia status/score por lição sem importar progress (CLAUDE.md §4).
 */
@Component
@Transactional(readOnly = true)
class LessonProgressQueryAdapter(
    private val lessonProgressRepository: LessonProgressRepository
) : LessonProgressQuery {

    override fun snapshotsFor(
        userId: UUID,
        lessonIds: Collection<UUID>
    ): Map<UUID, LessonProgressSnapshot> {
        if (lessonIds.isEmpty()) return emptyMap()
        return lessonProgressRepository
            .findByIdUserIdAndIdLessonIdIn(userId, lessonIds)
            .associate { lp ->
                lp.id.lessonId to LessonProgressSnapshot(
                    lessonId = lp.id.lessonId,
                    status = LessonProgressStatus.valueOf(lp.status.name),
                    bestScore = lp.bestScore.toDouble()
                )
            }
    }
}
