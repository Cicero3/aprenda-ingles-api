package com.englishapp.curriculum.api

import com.englishapp.curriculum.api.dto.LessonDetail
import com.englishapp.curriculum.api.dto.LessonSummary
import com.englishapp.curriculum.api.dto.ModuleSummary
import com.englishapp.curriculum.application.CurriculumQueryService
import com.englishapp.auth.security.UserPrincipal
import com.englishapp.common.dto.ApiResponse
import com.englishapp.common.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Curriculum", description = "Catálogo: módulos, lições e exercícios (com progresso do aluno)")
@RestController
@RequestMapping("/api/v1")
class CurriculumController(
    private val curriculumQueryService: CurriculumQueryService
) {
    @GetMapping("/modules")
    fun listModules(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<ModuleSummary>>> {
        val result = curriculumQueryService.listModules(principal.id, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse(data = result.content, meta = PageMeta.of(result)))
    }

    @GetMapping("/modules/{moduleId}/lessons")
    fun listLessons(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable moduleId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<LessonSummary>>> {
        val result = curriculumQueryService.listLessons(moduleId, principal.id, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse(data = result.content, meta = PageMeta.of(result)))
    }

    @GetMapping("/lessons/{lessonId}")
    fun getLesson(
        @PathVariable lessonId: UUID
    ): ResponseEntity<ApiResponse<LessonDetail>> {
        val lesson = curriculumQueryService.getLesson(lessonId)
        return ResponseEntity.ok(ApiResponse(data = lesson))
    }
}
