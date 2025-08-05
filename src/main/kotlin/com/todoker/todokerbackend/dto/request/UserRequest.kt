package com.todoker.todokerbackend.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 회원가입 요청 DTO
 */
@Schema(description = "회원가입 요청")
data class SignUpRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 3, max = 20, message = "사용자명은 3-20자 사이여야 합니다")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 언더스코어만 사용 가능합니다")
    @Schema(description = "사용자명 (3-20자, 영문/숫자/언더스코어)", example = "john_doe")
    val username: String,
    
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "이메일 주소", example = "user@example.com")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    @Schema(description = "비밀번호 (8자 이상, 영문/숫자/특수문자 포함)", example = "Password123!")
    val password: String,
    
    @field:Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    @Schema(description = "닉네임 (선택사항, 최대 50자)", example = "John", required = false)
    val nickname: String? = null
)

/**
 * 로그인 요청 DTO
 */
@Schema(description = "로그인 요청")
data class LoginRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @Schema(description = "사용자명", example = "john_doe")
    val username: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "비밀번호", example = "Password123!")
    val password: String
)

/**
 * 프로필 업데이트 요청 DTO
 */
@Schema(description = "프로필 업데이트 요청")
data class UpdateProfileRequest(
    @field:Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    val nickname: String? = null,
    
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String? = null
)

/**
 * 비밀번호 변경 요청 DTO
 */
@Schema(description = "비밀번호 변경 요청")
data class ChangePasswordRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    val currentPassword: String,
    
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    val newPassword: String
)

/**
 * 패스워드 재설정 요청 DTO (이메일 기반)
 */
@Schema(description = "패스워드 재설정 요청")
data class ResetPasswordRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "등록된 이메일 주소", example = "user@example.com")
    val email: String
)

/**
 * 패스워드 재설정 확인 DTO (토큰 기반)
 */
@Schema(description = "패스워드 재설정 확인")
data class ResetPasswordConfirmRequest(
    @field:NotBlank(message = "재설정 토큰은 필수입니다")
    @Schema(description = "이메일로 받은 재설정 토큰", example = "abc123def456")
    val token: String,
    
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    @Schema(description = "새 비밀번호 (8자 이상, 영문/숫자/특수문자 포함)", example = "NewPassword123!")
    val newPassword: String
)

/**
 * 아이디 찾기 요청 DTO (이메일 기반)
 */
@Schema(description = "아이디 찾기 요청")
data class FindUsernameRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "등록된 이메일 주소", example = "user@example.com")
    val email: String
)

/**
 * 사용자 설정 업데이트 요청 DTO
 */
@Schema(description = "사용자 설정 업데이트 요청")
data class UpdatePreferenceRequest(
    @field:Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "올바른 hex 색상 코드가 아닙니다")
    val themeBackground: String? = null,
    
    @field:Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "올바른 hex 색상 코드가 아닙니다")
    val themeElement: String? = null,
    
    @field:Size(min = 1, max = 60, message = "작업 시간은 1-60분 사이여야 합니다")
    val pomodoroWorkTime: Int? = null,
    
    @field:Size(min = 1, max = 30, message = "휴식 시간은 1-30분 사이여야 합니다")
    val pomodoroBreakTime: Int? = null,
    
    @field:Size(min = 1, max = 60, message = "긴 휴식 시간은 1-60분 사이여야 합니다")
    val pomodoroLongBreakTime: Int? = null,
    
    @field:Size(min = 1, max = 10, message = "세션 수는 1-10 사이여야 합니다")
    val pomodoroSessionsBeforeLongBreak: Int? = null,
    
    val enableNotifications: Boolean? = null,
    val enableSoundEffects: Boolean? = null,
    
    @field:Pattern(regexp = "^(ko|en|ja|zh)$", message = "지원하지 않는 언어입니다")
    val language: String? = null,
    
    val timezone: String? = null,
    
    @field:Size(min = 0, max = 6, message = "시작 요일은 0-6 사이여야 합니다")
    val startOfWeek: Int? = null
)