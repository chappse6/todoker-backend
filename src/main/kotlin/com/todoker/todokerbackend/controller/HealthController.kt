package com.todoker.todokerbackend.controller

import com.todoker.todokerbackend.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 헬스 체크 컨트롤러
 * 애플리케이션 상태 확인을 위한 엔드포인트 제공
 */
@Tag(name = "Health", description = "헬스 체크 API")
@RestController
@RequestMapping("/health")
class HealthController {
    
    /**
     * 기본 헬스 체크
     * 애플리케이션이 정상적으로 실행 중인지 확인
     */
    @Operation(summary = "헬스 체크", description = "애플리케이션의 기본 상태를 확인합니다.")
    @GetMapping
    fun health(): ApiResponse<Map<String, Any>> {
        val healthInfo = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "application" to "todoker-backend",
            "version" to "1.0.0"
        )
        
        return ApiResponse.success(healthInfo)
    }
    
    /**
     * 준비 상태 확인
     * 애플리케이션이 요청을 받을 준비가 되었는지 확인
     */
    @Operation(summary = "준비 상태 확인", description = "애플리케이션이 요청을 처리할 준비가 되었는지 확인합니다.")
    @GetMapping("/ready")
    fun ready(): ApiResponse<Map<String, Any>> {
        val readyInfo = mapOf(
            "status" to "READY",
            "timestamp" to LocalDateTime.now(),
            "message" to "Application is ready to serve requests"
        )
        
        return ApiResponse.success(readyInfo)
    }
}