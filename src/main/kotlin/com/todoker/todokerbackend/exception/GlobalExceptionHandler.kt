package com.todoker.todokerbackend.exception

import com.todoker.todokerbackend.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 전역 예외 처리 핸들러
 * 애플리케이션 전체에서 발생하는 예외를 일관된 형식으로 처리
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Business exception: ${e.message}")
        
        return ResponseEntity
            .status(e.status)
            .body(ApiResponse.error(e.errorCode, e.message ?: "Business error occurred"))
    }
    
    /**
     * 리소스를 찾을 수 없는 경우
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFoundException(e: NoSuchElementException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Resource not found: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("RESOURCE_NOT_FOUND", e.message ?: "Resource not found"))
    }
    
    /**
     * 잘못된 인자 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid argument: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_ARGUMENT", e.message ?: "Invalid argument"))
    }
    
    /**
     * 상태 예외 처리
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid state: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("INVALID_STATE", e.message ?: "Invalid state"))
    }
    
    /**
     * 검증 실패 예외 처리
     * @Valid 어노테이션으로 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.allErrors
            .associate { 
                val fieldName = (it as? FieldError)?.field ?: "unknown"
                fieldName to (it.defaultMessage ?: "Invalid value")
            }
        
        logger.warn("Validation failed: $errors")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(
                "VALIDATION_FAILED",
                "Validation failed",
                errors
            ))
    }
    
    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        val message = "Invalid type for parameter '${e.name}'. Expected: ${e.requiredType?.simpleName}"
        logger.warn("Type mismatch: $message")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("TYPE_MISMATCH", message))
    }
    
    /**
     * 인증 실패 예외 처리
     */
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Authentication failed: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("AUTHENTICATION_FAILED", "Invalid credentials"))
    }
    
    /**
     * 사용자를 찾을 수 없는 경우
     */
    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFoundException(e: UsernameNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("User not found: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("USER_NOT_FOUND", "User not found"))
    }
    
    /**
     * 접근 권한이 없는 경우
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Access denied: ${e.message}")
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("ACCESS_DENIED", e.message ?: "Access denied"))
    }
    
    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error occurred", e)
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later."
            ))
    }
}

/**
 * 비즈니스 로직 예외를 위한 커스텀 예외 클래스
 */
open class BusinessException(
    val errorCode: String,
    override val message: String?,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)

/**
 * 접근 권한 예외
 */
class AccessDeniedException(message: String? = "Access denied") : RuntimeException(message)