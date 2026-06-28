package com.englishapp.progress.api

import com.englishapp.auth.security.UserPrincipal
import com.englishapp.common.dto.ApiResponse
import com.englishapp.progress.api.dto.ProgressDashboardResponse
import com.englishapp.progress.api.dto.SubmitAttemptsRequest
import com.englishapp.progress.api.dto.SubmitAttemptsResponse
import com.englishapp.progress.application.ProgressQueryService
import com.englishapp.progress.application.SubmitAttemptsUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ProgressController(
    private val submitAttemptsUseCase: SubmitAttemptsUseCase,
    private val progressQueryService: ProgressQueryService
) {
    @PostMapping("/attempts")
    fun submitAttempts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: SubmitAttemptsRequest
    ): ResponseEntity<ApiResponse<SubmitAttemptsResponse>> {
        val result = submitAttemptsUseCase.execute(principal.id, request)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    @GetMapping("/users/me/progress")
    fun myProgress(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ProgressDashboardResponse>> {
        val dashboard = progressQueryService.getDashboard(principal.id)
        return ResponseEntity.ok(ApiResponse(data = dashboard))
    }
}
