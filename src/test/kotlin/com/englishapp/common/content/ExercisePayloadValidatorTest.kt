package com.englishapp.common.content

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExercisePayloadValidatorTest {

    private val validator = ExercisePayloadValidator()
    private val mapper = ObjectMapper()

    private fun json(raw: String) = mapper.readTree(raw)

    private fun validate(type: String, payload: String, answer: String) =
        validator.validate(type, json(payload), json(answer))

    // ---------- tipo ----------

    @Test
    fun `tipo desconhecido vira violacao`() {
        val violations = validate("speaking", """{}""", """{}""")
        assertThat(violations).anyMatch { it.contains("tipo desconhecido") }
    }

    // ---------- multiple_choice ----------

    @Test
    fun `multiple_choice valido nao tem violacoes`() {
        val payload = """
            { "prompt": "Qual a forma correta?",
              "options": [ {"index":0,"text":"He are"}, {"index":1,"text":"He is"} ] }
        """
        val violations = validate("multiple_choice", payload, """{ "selected_index": 1 }""")
        assertThat(violations).isEmpty()
    }

    @Test
    fun `multiple_choice exige ao menos duas opcoes`() {
        val payload = """{ "prompt": "x", "options": [ {"index":0,"text":"a"} ] }"""
        val violations = validate("multiple_choice", payload, """{ "selected_index": 0 }""")
        assertThat(violations).anyMatch { it.contains("ao menos 2 opções") }
    }

    @Test
    fun `multiple_choice acusa selected_index fora das opcoes`() {
        val payload = """
            { "prompt": "x", "options": [ {"index":0,"text":"a"}, {"index":1,"text":"b"} ] }
        """
        val violations = validate("multiple_choice", payload, """{ "selected_index": 5 }""")
        assertThat(violations).anyMatch { it.contains("não corresponde a nenhuma opção") }
    }

    @Test
    fun `multiple_choice acusa indices duplicados e prompt ausente`() {
        val payload = """
            { "options": [ {"index":0,"text":"a"}, {"index":0,"text":"b"} ] }
        """
        val violations = validate("multiple_choice", payload, """{ "selected_index": 0 }""")
        assertThat(violations).anyMatch { it.contains("prompt") }
        assertThat(violations).anyMatch { it.contains("duplicado") }
    }

    // ---------- fill_blank ----------

    @Test
    fun `fill_blank valido nao tem violacoes`() {
        val payload = """{ "sentence_template": "They ___ my friends.", "hint": "plural" }"""
        assertThat(validate("fill_blank", payload, """{ "text": "are" }""")).isEmpty()
    }

    @Test
    fun `fill_blank exige lacuna no template`() {
        val payload = """{ "sentence_template": "They are my friends." }"""
        val violations = validate("fill_blank", payload, """{ "text": "are" }""")
        assertThat(violations).anyMatch { it.contains("'___'") }
    }

    @Test
    fun `fill_blank exige correct_answer text`() {
        val payload = """{ "sentence_template": "They ___ here." }"""
        val violations = validate("fill_blank", payload, """{ }""")
        assertThat(violations).anyMatch { it.contains("correct_answer.text") }
    }

    // ---------- translation ----------

    @Test
    fun `translation valido com flags booleanas nao tem violacoes`() {
        val payload = """
            { "source_text": "Eu sou um professor.", "case_sensitive": false, "ignore_punctuation": true }
        """
        assertThat(validate("translation", payload, """{ "text": "I am a teacher." }""")).isEmpty()
    }

    @Test
    fun `translation acusa flag nao-booleana e accepted_answers invalido`() {
        val payload = """
            { "source_text": "x", "case_sensitive": "yes", "accepted_answers": [ "ok", 3 ] }
        """
        val violations = validate("translation", payload, """{ "text": "x" }""")
        assertThat(violations).anyMatch { it.contains("case_sensitive") }
        assertThat(violations).anyMatch { it.contains("accepted_answers[1]") }
    }

    // ---------- requireValid ----------

    @Test
    fun `requireValid lanca em conteudo invalido`() {
        val ex = assertThrows<IllegalArgumentException> {
            validator.requireValid("translation", json("""{ }"""), json("""{ }"""))
        }
        assertThat(ex.message).contains("inválido")
    }
}
