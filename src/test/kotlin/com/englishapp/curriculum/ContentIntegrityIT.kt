package com.englishapp.curriculum

import com.englishapp.AbstractIntegrationTest
import com.englishapp.common.content.ExercisePayloadValidator
import com.englishapp.curriculum.infrastructure.ExerciseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Trava de integridade do conteúdo: todo exercício ativo do seed (db/seed) precisa
 * passar no contrato de payload/gabarito. É o "validar na escrita" do MVP — o conteúdo
 * é escrito via seed/migration, então este IT roda no CI e barra conteúdo malformado.
 */
class ContentIntegrityIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var exerciseRepository: ExerciseRepository

    @Autowired
    private lateinit var validator: ExercisePayloadValidator

    @Test
    fun `every active seeded exercise conforms to the payload contract`() {
        val active = exerciseRepository.findAll().filter { it.isActive }
        assertThat(active).isNotEmpty()

        val problems = active.flatMap { exercise ->
            validator.validate(exercise.type, exercise.questionPayload, exercise.correctAnswer)
                .map { "exercise ${exercise.id} (${exercise.type}): $it" }
        }

        assertThat(problems)
            .withFailMessage("Exercícios do seed violam o contrato:\n%s", problems.joinToString("\n"))
            .isEmpty()
    }
}
