package com.todoker.todokerbackend.exception

import org.springframework.http.HttpStatus

/**
 * Todoker 애플리케이션의 공통 예외 클래스
 * 모든 비즈니스 로직 예외는 이 클래스를 상속받아 사용
 */
open class TodoException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null,
    val details: Map<String, Any>? = null
) : RuntimeException(message, cause) {

    /**
     * HTTP 상태 코드 반환
     */
    val status: HttpStatus
        get() = errorCode.status

    /**
     * 에러 코드 문자열 반환
     */
    val code: String
        get() = errorCode.code

    companion object {
        /**
         * ErrorCode를 사용한 예외 생성
         */
        fun of(errorCode: ErrorCode): TodoException {
            return TodoException(errorCode)
        }

        /**
         * ErrorCode와 커스텀 메시지를 사용한 예외 생성
         */
        fun of(errorCode: ErrorCode, message: String): TodoException {
            return TodoException(errorCode, message)
        }

        /**
         * ErrorCode와 세부 정보를 사용한 예외 생성
         */
        fun of(errorCode: ErrorCode, details: Map<String, Any>): TodoException {
            return TodoException(errorCode, details = details)
        }

        /**
         * ErrorCode, 커스텀 메시지, 세부 정보를 사용한 예외 생성
         */
        fun of(errorCode: ErrorCode, message: String, details: Map<String, Any>): TodoException {
            return TodoException(errorCode, message, details = details)
        }
    }
}

/**
 * 인증 관련 예외
 */
class AuthException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun invalidCredentials(): AuthException {
            return AuthException(ErrorCode.INVALID_CREDENTIALS)
        }

        fun tokenExpired(): AuthException {
            return AuthException(ErrorCode.TOKEN_EXPIRED)
        }

        fun invalidToken(): AuthException {
            return AuthException(ErrorCode.INVALID_TOKEN)
        }

        fun refreshTokenExpired(): AuthException {
            return AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        fun refreshTokenNotFound(): AuthException {
            return AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        }

        fun unauthorized(): AuthException {
            return AuthException(ErrorCode.UNAUTHORIZED)
        }
    }
}

/**
 * 사용자 관련 예외
 */
class UserException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun notFound(): UserException {
            return UserException(ErrorCode.USER_NOT_FOUND)
        }

        fun alreadyExists(): UserException {
            return UserException(ErrorCode.USER_ALREADY_EXISTS)
        }

        fun usernameAlreadyExists(username: String): UserException {
            return UserException(
                ErrorCode.USERNAME_ALREADY_EXISTS,
                details = mapOf("username" to username)
            )
        }

        fun emailAlreadyExists(email: String): UserException {
            return UserException(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                details = mapOf("email" to email)
            )
        }

        fun invalidPassword(): UserException {
            return UserException(ErrorCode.INVALID_PASSWORD)
        }

        fun disabled(): UserException {
            return UserException(ErrorCode.USER_DISABLED)
        }
    }
}

/**
 * Todo 관련 예외
 */
class TodoItemException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun notFound(id: Long): TodoItemException {
            return TodoItemException(
                ErrorCode.TODO_NOT_FOUND,
                details = mapOf("id" to id)
            )
        }

        fun accessDenied(id: Long): TodoItemException {
            return TodoItemException(
                ErrorCode.TODO_ACCESS_DENIED,
                details = mapOf("id" to id)
            )
        }

        fun alreadyCompleted(id: Long): TodoItemException {
            return TodoItemException(
                ErrorCode.TODO_ALREADY_COMPLETED,
                details = mapOf("id" to id)
            )
        }

        fun alreadyPending(id: Long): TodoItemException {
            return TodoItemException(
                ErrorCode.TODO_ALREADY_PENDING,
                details = mapOf("id" to id)
            )
        }

        fun titleRequired(): TodoItemException {
            return TodoItemException(ErrorCode.TODO_TITLE_REQUIRED)
        }

        fun titleTooLong(maxLength: Int): TodoItemException {
            return TodoItemException(
                ErrorCode.TODO_TITLE_TOO_LONG,
                details = mapOf("maxLength" to maxLength)
            )
        }
    }
}

/**
 * 카테고리 관련 예외
 */
class CategoryException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun notFound(id: Long): CategoryException {
            return CategoryException(
                ErrorCode.CATEGORY_NOT_FOUND,
                details = mapOf("id" to id)
            )
        }

        fun alreadyExists(name: String): CategoryException {
            return CategoryException(
                ErrorCode.CATEGORY_ALREADY_EXISTS,
                details = mapOf("name" to name)
            )
        }

        fun accessDenied(id: Long): CategoryException {
            return CategoryException(
                ErrorCode.CATEGORY_ACCESS_DENIED,
                details = mapOf("id" to id)
            )
        }

        fun hasTodos(id: Long, todoCount: Int): CategoryException {
            return CategoryException(
                ErrorCode.CATEGORY_HAS_TODOS,
                details = mapOf("id" to id, "todoCount" to todoCount)
            )
        }

        fun nameRequired(): CategoryException {
            return CategoryException(ErrorCode.CATEGORY_NAME_REQUIRED)
        }
    }
}

/**
 * 뽀모도로 관련 예외
 */
class PomodoroException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun notFound(id: Long): PomodoroException {
            return PomodoroException(
                ErrorCode.POMODORO_NOT_FOUND,
                details = mapOf("id" to id)
            )
        }

        fun alreadyRunning(): PomodoroException {
            return PomodoroException(ErrorCode.POMODORO_ALREADY_RUNNING)
        }

        fun notRunning(): PomodoroException {
            return PomodoroException(ErrorCode.POMODORO_NOT_RUNNING)
        }

        fun accessDenied(id: Long): PomodoroException {
            return PomodoroException(
                ErrorCode.POMODORO_ACCESS_DENIED,
                details = mapOf("id" to id)
            )
        }

        fun invalidDuration(duration: Int): PomodoroException {
            return PomodoroException(
                ErrorCode.INVALID_POMODORO_DURATION,
                details = mapOf("duration" to duration)
            )
        }
    }
}

/**
 * 검증 관련 예외
 */
class ValidationException(
    errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    details: Map<String, Any>? = null
) : TodoException(errorCode, message, cause, details) {

    companion object {
        fun failed(fieldErrors: Map<String, String>): ValidationException {
            return ValidationException(
                ErrorCode.VALIDATION_FAILED,
                details = fieldErrors
            )
        }

        fun missingRequiredField(fieldName: String): ValidationException {
            return ValidationException(
                ErrorCode.MISSING_REQUIRED_FIELD,
                details = mapOf("field" to fieldName)
            )
        }

        fun invalidEmail(email: String): ValidationException {
            return ValidationException(
                ErrorCode.INVALID_EMAIL_FORMAT,
                details = mapOf("email" to email)
            )
        }

        fun invalidPassword(): ValidationException {
            return ValidationException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }

        fun invalidUsername(username: String): ValidationException {
            return ValidationException(
                ErrorCode.INVALID_USERNAME_FORMAT,
                details = mapOf("username" to username)
            )
        }

        fun valueTooShort(fieldName: String, minLength: Int): ValidationException {
            return ValidationException(
                ErrorCode.VALUE_TOO_SHORT,
                details = mapOf("field" to fieldName, "minLength" to minLength)
            )
        }

        fun valueTooLong(fieldName: String, maxLength: Int): ValidationException {
            return ValidationException(
                ErrorCode.VALUE_TOO_LONG,
                details = mapOf("field" to fieldName, "maxLength" to maxLength)
            )
        }
    }
}