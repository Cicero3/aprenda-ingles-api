package com.englishapp.common.grading

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraderTest {

    private val grader = Grader()
    private val mapper = ObjectMapper()

    private fun json(raw: String) = mapper.readTree(raw)

    // ===== multiple_choice =====

    @Test
    fun `should accept multiple choice with matching index`() {
        val correct = grader.isCorrect(
            type = "multiple_choice",
            payload = json("""{"options":[]}"""),
            correctAnswer = json("""{"selected_index":2}"""),
            userAnswer = json("""{"selected_index":2}""")
        )
        assertTrue(correct)
    }

    @Test
    fun `should reject multiple choice with wrong index`() {
        val correct = grader.isCorrect(
            type = "multiple_choice",
            payload = json("""{"options":[]}"""),
            correctAnswer = json("""{"selected_index":2}"""),
            userAnswer = json("""{"selected_index":1}""")
        )
        assertFalse(correct)
    }

    @Test
    fun `should reject multiple choice with missing index`() {
        val correct = grader.isCorrect(
            type = "multiple_choice",
            payload = json("""{}"""),
            correctAnswer = json("""{"selected_index":2}"""),
            userAnswer = json("""{}""")
        )
        assertFalse(correct)
    }

    // ===== fill_blank =====

    @Test
    fun `should accept fill blank ignoring case and surrounding spaces`() {
        val payload = json("""{"accepted_answers":["is","IS","Is"]}""")
        val answer = json("""{"correct_answer":{"text":"is"}}""").path("correct_answer")
        assertTrue(
            grader.isCorrect("fill_blank", payload, answer, json("""{"text":"  IS  "}"""))
        )
    }

    @Test
    fun `should reject fill blank with wrong word`() {
        val payload = json("""{"accepted_answers":["is"]}""")
        assertFalse(
            grader.isCorrect("fill_blank", payload, json("""{"text":"is"}"""), json("""{"text":"are"}"""))
        )
    }

    // ===== translation =====

    @Test
    fun `should accept translation ignoring punctuation and case`() {
        val payload = json(
            """{"accepted_answers":["We were tired."],"case_sensitive":false,"ignore_punctuation":true}"""
        )
        assertTrue(
            grader.isCorrect("translation", payload, json("""{"text":"We were tired."}"""), json("""{"text":"we were tired"}"""))
        )
    }

    @Test
    fun `should reject translation with grammatical error`() {
        val payload = json(
            """{"accepted_answers":["We were tired."],"case_sensitive":false,"ignore_punctuation":true}"""
        )
        assertFalse(
            grader.isCorrect("translation", payload, json("""{"text":"We were tired."}"""), json("""{"text":"We was tired"}"""))
        )
    }

    @Test
    fun `should throw on unknown exercise type`() {
        try {
            grader.isCorrect("speaking", json("{}"), json("{}"), json("{}"))
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }
}
