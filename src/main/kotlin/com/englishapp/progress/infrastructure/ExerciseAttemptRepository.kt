package com.englishapp.progress.infrastructure

import com.englishapp.progress.domain.ExerciseAttempt
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ExerciseAttemptRepository : JpaRepository<ExerciseAttempt, Long> {

    /**
     * Erros recentes do usuário. Filtra por createdAt para aproveitar o
     * particionamento por mês (CLAUDE.md §6.3).
     */
    @Query(
        """
        SELECT a FROM ExerciseAttempt a
        WHERE a.userId = :userId
          AND a.createdAt >= :since
          AND a.isCorrect = false
        ORDER BY a.createdAt DESC
        """
    )
    fun findRecentErrors(
        @Param("userId") userId: UUID,
        @Param("since") since: Instant,
        pageable: Pageable
    ): List<ExerciseAttempt>
}
