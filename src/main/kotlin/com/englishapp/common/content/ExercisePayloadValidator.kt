package com.englishapp.common.content

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

/**
 * Valida a estrutura de question_payload + correct_answer por tipo de exercício
 * (CLAUDE.md §3.5: nunca confiar em conteúdo sem validar o JSONB).
 *
 * Puro e stateless: opera sobre JsonNode, sem acessar banco. O contrato completo
 * está documentado em docs/content-contract.md.
 *
 * Uso atual: travar o seed no CI (ContentIntegrityIT). Pronto para reuso num
 * futuro endpoint de autoria/admin antes de persistir um exercício.
 */
@Component
class ExercisePayloadValidator {

    /** Lança se houver qualquer violação. Útil em pontos de escrita. */
    fun requireValid(type: String, payload: JsonNode, correctAnswer: JsonNode) {
        val violations = validate(type, payload, correctAnswer)
        require(violations.isEmpty()) {
            "Conteúdo de exercício inválido ($type): ${violations.joinToString("; ")}"
        }
    }

    /** Retorna a lista de violações (vazia = válido). */
    fun validate(type: String, payload: JsonNode, correctAnswer: JsonNode): List<String> {
        val exerciseType = ExerciseType.fromCode(type)
            ?: return listOf("tipo desconhecido: '$type'")

        return when (exerciseType) {
            ExerciseType.MULTIPLE_CHOICE -> validateMultipleChoice(payload, correctAnswer)
            ExerciseType.FILL_BLANK -> validateFillBlank(payload, correctAnswer)
            ExerciseType.TRANSLATION -> validateTranslation(payload, correctAnswer)
        }
    }

    private fun validateMultipleChoice(payload: JsonNode, correctAnswer: JsonNode): List<String> {
        val violations = mutableListOf<String>()
        requireNonBlankText(payload, "prompt", violations)

        val options = payload.path("options")
        if (!options.isArray || options.size() < 2) {
            violations += "options deve ser um array com ao menos 2 opções"
            return violations // sem opções válidas, checar selected_index não agrega
        }

        val indices = mutableSetOf<Int>()
        options.forEachIndexed { i, option ->
            val index = option.path("index")
            if (!index.isInt) {
                violations += "options[$i].index deve ser inteiro"
            } else if (!indices.add(index.asInt())) {
                violations += "options[$i].index duplicado: ${index.asInt()}"
            }
            if (!option.path("text").isTextualNonBlank()) {
                violations += "options[$i].text é obrigatório"
            }
        }

        val selected = correctAnswer.path("selected_index")
        when {
            !selected.isInt -> violations += "correct_answer.selected_index deve ser inteiro"
            selected.asInt() !in indices -> violations +=
                "correct_answer.selected_index (${selected.asInt()}) não corresponde a nenhuma opção"
        }
        return violations
    }

    private fun validateFillBlank(payload: JsonNode, correctAnswer: JsonNode): List<String> {
        val violations = mutableListOf<String>()
        val template = payload.path("sentence_template")
        if (!template.isTextualNonBlank()) {
            violations += "sentence_template é obrigatório"
        } else if (!template.asText().contains("___")) {
            violations += "sentence_template deve conter a lacuna '___'"
        }
        requireNonBlankText(correctAnswer, "text", violations, prefix = "correct_answer.")
        validateAcceptedAnswers(payload, violations)
        return violations
    }

    private fun validateTranslation(payload: JsonNode, correctAnswer: JsonNode): List<String> {
        val violations = mutableListOf<String>()
        requireNonBlankText(payload, "source_text", violations)
        requireNonBlankText(correctAnswer, "text", violations, prefix = "correct_answer.")
        requireBooleanIfPresent(payload, "case_sensitive", violations)
        requireBooleanIfPresent(payload, "ignore_punctuation", violations)
        validateAcceptedAnswers(payload, violations)
        return violations
    }

    private fun validateAcceptedAnswers(payload: JsonNode, violations: MutableList<String>) {
        val accepted = payload.path("accepted_answers")
        if (accepted.isMissingNode || accepted.isNull) return
        if (!accepted.isArray) {
            violations += "accepted_answers deve ser um array de textos"
            return
        }
        accepted.forEachIndexed { i, node ->
            if (!node.isTextualNonBlank()) violations += "accepted_answers[$i] deve ser texto não-vazio"
        }
    }

    private fun requireNonBlankText(
        node: JsonNode,
        field: String,
        violations: MutableList<String>,
        prefix: String = ""
    ) {
        if (!node.path(field).isTextualNonBlank()) {
            violations += "$prefix$field é obrigatório"
        }
    }

    private fun requireBooleanIfPresent(node: JsonNode, field: String, violations: MutableList<String>) {
        val value = node.path(field)
        if (!value.isMissingNode && !value.isNull && !value.isBoolean) {
            violations += "$field, se presente, deve ser booleano"
        }
    }

    private fun JsonNode.isTextualNonBlank(): Boolean = isTextual && asText().isNotBlank()
}
