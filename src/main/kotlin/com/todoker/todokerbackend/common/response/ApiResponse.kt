package com.todoker.todokerbackend.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

/**
 * API 응답을 위한 공통 래퍼 클래스
 * 모든 API 응답은 이 형식으로 통일됨
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun <T> success(data: T? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data
            )
        }
        
        /**
         * 실패 응답 생성
         */
        fun <T> error(
            code: String,
            message: String,
            details: Map<String, Any>? = null
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorResponse(code, message, details)
            )
        }
        
        /**
         * HTTP 상태 코드와 함께 실패 응답 생성
         */
        fun <T> error(
            status: HttpStatus,
            message: String,
            details: Map<String, Any>? = null
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorResponse(
                    code = status.name,
                    message = message,
                    details = details
                )
            )
        }
    }
}

/**
 * 에러 정보를 담는 클래스
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

/**
 * 페이지네이션 응답을 위한 래퍼 클래스
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
)