package com.englishapp.common.grading

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

/**
 * Correção determinística (gabarito hard-coded, NUNCA IA — CLAUDE.md §1).
 * Stateless e puro: dado o tipo, o payload, o gabarito e a resposta do aluno,
 * decide se está correta. Não acessa banco nem outras features.
 */
@Component
class Grader {

    fun isCorrect(
        type: String,
        payload: JsonNode,
        correctAnswer: JsonNode,
        userAnswer: JsonNode
    ): Boolean = when (type) {
        "multiple_choice" -> gradeMultipleChoice(correctAnswer, userAnswer)
        "fill_blank" -> gradeText(payload, correctAnswer, userAnswer, caseSensitive = false, ignorePunctuation = false)
        "translation" -> gradeTranslation(payload, correctAnswer, userAnswer)
        else -> throw IllegalArgumentException("Tipo de exercício desconhecido: $type")
    }

    private fun gradeMultipleChoice(correctAnswer: JsonNode, userAnswer: JsonNode): Boolean {
        val expected = correctAnswer.path("selected_index")
        val given = userAnswer.path("selected_index")
        return expected.isInt && given.isInt && expected.asInt() == given.asInt()
    }

    private fun gradeTranslation(payload: JsonNode, correctAnswer: JsonNode, userAnswer: JsonNode): Boolean {
        val caseSensitive = payload.path("case_sensitive").asBoolean(false)
        val ignorePunctuation = payload.path("ignore_punctuation").asBoolean(true)
        return gradeText(payload, correctAnswer, userAnswer, caseSensitive, ignorePunctuation)
    }

    private fun gradeText(
        payload: JsonNode,
        correctAnswer: JsonNode,
        userAnswer: JsonNode,
        caseSensitive: Boolean,
        ignorePunctuation: Boolean
    ): Boolean {
        val given = userAnswer.path("text")
        if (!given.isTextual) return false
        val normalizedGiven = normalize(given.asText(), caseSensitive, ignorePunctuation)

        val accepted = buildList {
            payload.path("accepted_answers").forEach { if (it.isTextual) add(it.asText()) }
            if (correctAnswer.path("text").isTextual) add(correctAnswer.path("text").asText())
        }
        return accepted.any { normalize(it, caseSensitive, ignorePunctuation) == normalizedGiven }
    }

    private fun normalize(value: String, caseSensitive: Boolean, ignorePunctuation: Boolean): String {
        var result = value.trim().replace(Regex("\\s+"), " ")
        if (!caseSensitive) result = result.lowercase()
        if (ignorePunctuation) result = result.replace(Regex("[\\p{Punct}]"), "")
        return result.trim()
    }
}
