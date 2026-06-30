package com.englishapp.common.content

/**
 * Tipos canônicos de exercício suportados pela correção determinística (Grader)
 * e pelo contrato de conteúdo. O `code` é o valor persistido em exercises.type
 * e exposto na API.
 */
enum class ExerciseType(val code: String) {
    MULTIPLE_CHOICE("multiple_choice"),
    FILL_BLANK("fill_blank"),
    TRANSLATION("translation");

    companion object {
        fun fromCode(code: String): ExerciseType? = entries.find { it.code == code }
    }
}
