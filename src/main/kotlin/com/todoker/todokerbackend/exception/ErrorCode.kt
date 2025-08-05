package com.todoker.todokerbackend.exception

import org.springframework.http.HttpStatus

/**
 * 에러 코드를 관리하는 enum
 * 모든 비즈니스 에러에 대한 코드, 메시지, HTTP 상태를 중앙 관리
 */
enum class ErrorCode(
    val code: String,
    val message: String,
    val status: HttpStatus
) {
    // ========================================
    // Common Errors (1000번대)
    // ========================================
    INVALID_INPUT_VALUE("C001", "잘못된 입력값입니다", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("C002", "잘못된 타입입니다", HttpStatus.BAD_REQUEST),
    ENTITY_NOT_FOUND("C003", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_STATE("C004", "잘못된 상태입니다", HttpStatus.CONFLICT),
    ACCESS_DENIED("C005", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR("C006", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========================================
    // Authentication & Authorization (2000번대)
    // ========================================
    UNAUTHORIZED("A001", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("A002", "아이디 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("A003", "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("A004", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("A005", "토큰을 찾을 수 없습니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("A006", "리프레시 토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("A007", "리프레시 토큰을 찾을 수 없습니다", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("A008", "유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED),

    // ========================================
    // User Management (3000번대)
    // ========================================
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("U003", "이미 사용 중인 사용자명입니다", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("U004", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    INVALID_PASSWORD("U005", "현재 비밀번호가 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    USER_DISABLED("U006", "비활성화된 사용자입니다", HttpStatus.FORBIDDEN),
    INVALID_USER_STATUS("U007", "잘못된 사용자 상태입니다", HttpStatus.BAD_REQUEST),

    // ========================================
    // Todo Management (4000번대)
    // ========================================
    TODO_NOT_FOUND("T001", "할 일을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    TODO_ALREADY_COMPLETED("T002", "이미 완료된 할 일입니다", HttpStatus.CONFLICT),
    TODO_ALREADY_PENDING("T003", "이미 대기 중인 할 일입니다", HttpStatus.CONFLICT),
    INVALID_TODO_STATUS("T004", "잘못된 할 일 상태입니다", HttpStatus.BAD_REQUEST),
    TODO_ACCESS_DENIED("T005", "해당 할 일에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),
    TODO_TITLE_REQUIRED("T006", "할 일 제목은 필수입니다", HttpStatus.BAD_REQUEST),
    TODO_TITLE_TOO_LONG("T007", "할 일 제목이 너무 깁니다", HttpStatus.BAD_REQUEST),
    INVALID_TODO_PRIORITY("T008", "잘못된 우선순위입니다", HttpStatus.BAD_REQUEST),
    INVALID_DUE_DATE("T009", "잘못된 마감일입니다", HttpStatus.BAD_REQUEST),

    // ========================================
    // Category Management (5000번대)
    // ========================================
    CATEGORY_NOT_FOUND("CT001", "카테고리를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS("CT002", "이미 존재하는 카테고리입니다", HttpStatus.CONFLICT),
    CATEGORY_ACCESS_DENIED("CT003", "해당 카테고리에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),
    CATEGORY_NAME_REQUIRED("CT004", "카테고리 이름은 필수입니다", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_TOO_LONG("CT005", "카테고리 이름이 너무 깁니다", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_TODOS("CT006", "카테고리에 할 일이 있어서 삭제할 수 없습니다", HttpStatus.CONFLICT),
    INVALID_CATEGORY_COLOR("CT007", "잘못된 카테고리 색상입니다", HttpStatus.BAD_REQUEST),

    // ========================================
    // Pomodoro Management (6000번대)
    // ========================================
    POMODORO_NOT_FOUND("P001", "뽀모도로 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    POMODORO_ALREADY_RUNNING("P002", "이미 실행 중인 뽀모도로 세션이 있습니다", HttpStatus.CONFLICT),
    POMODORO_NOT_RUNNING("P003", "실행 중인 뽀모도로 세션이 없습니다", HttpStatus.CONFLICT),
    POMODORO_ACCESS_DENIED("P004", "해당 뽀모도로 세션에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_POMODORO_DURATION("P005", "잘못된 뽀모도로 시간입니다", HttpStatus.BAD_REQUEST),
    INVALID_POMODORO_STATUS("P006", "잘못된 뽀모도로 상태입니다", HttpStatus.BAD_REQUEST),

    // ========================================
    // Validation Errors (9000번대)
    // ========================================
    VALIDATION_FAILED("V001", "입력값 검증에 실패했습니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("V002", "필수 입력값이 누락되었습니다", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT("V003", "올바르지 않은 이메일 형식입니다", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT("V004", "비밀번호 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_FORMAT("V005", "사용자명 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    VALUE_TOO_SHORT("V006", "값이 너무 짧습니다", HttpStatus.BAD_REQUEST),
    VALUE_TOO_LONG("V007", "값이 너무 깁니다", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT("V008", "올바르지 않은 날짜 형식입니다", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_FORMAT("V009", "올바르지 않은 숫자 형식입니다", HttpStatus.BAD_REQUEST);

    companion object {
        /**
         * 에러 코드로 ErrorCode를 찾는 함수
         */
        fun findByCode(code: String): ErrorCode? {
            return values().find { it.code == code }
        }

        /**
         * HTTP 상태별로 기본 에러 코드를 반환하는 함수
         */
        fun getDefaultByStatus(status: HttpStatus): ErrorCode {
            return when (status) {
                HttpStatus.BAD_REQUEST -> INVALID_INPUT_VALUE
                HttpStatus.UNAUTHORIZED -> UNAUTHORIZED
                HttpStatus.FORBIDDEN -> ACCESS_DENIED
                HttpStatus.NOT_FOUND -> ENTITY_NOT_FOUND
                HttpStatus.CONFLICT -> INVALID_STATE
                HttpStatus.INTERNAL_SERVER_ERROR -> INTERNAL_SERVER_ERROR
                else -> INTERNAL_SERVER_ERROR
            }
        }
    }
}