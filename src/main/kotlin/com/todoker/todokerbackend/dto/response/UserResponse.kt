package com.todoker.todokerbackend.dto.response

import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserPreference
import com.todoker.todokerbackend.domain.user.UserRole
import java.time.LocalDateTime

/**
 * 로그인 응답 DTO
 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

/**
 * 사용자 정보 응답 DTO
 */
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val nickname: String,
    val role: UserRole,
    val isEnabled: Boolean,
    val createdAt: LocalDateTime,
    val preference: UserPreferenceResponse? = null
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                username = user.getUsername(),
                email = user.email,
                nickname = user.nickname,
                role = user.role,
                isEnabled = user.isEnabled(),
                createdAt = user.createdAt,
                preference = user.preference?.let { UserPreferenceResponse.from(it) }
            )
        }
    }
}

/**
 * 사용자 설정 응답 DTO
 */
data class UserPreferenceResponse(
    val themeBackground: String,
    val themeElement: String,
    val pomodoroWorkTime: Int,
    val pomodoroBreakTime: Int,
    val pomodoroLongBreakTime: Int,
    val pomodoroSessionsBeforeLongBreak: Int,
    val enableNotifications: Boolean,
    val enableSoundEffects: Boolean,
    val language: String,
    val timezone: String,
    val startOfWeek: Int
) {
    companion object {
        fun from(preference: UserPreference): UserPreferenceResponse {
            return UserPreferenceResponse(
                themeBackground = preference.themeBackground,
                themeElement = preference.themeElement,
                pomodoroWorkTime = preference.pomodoroWorkTime,
                pomodoroBreakTime = preference.pomodoroBreakTime,
                pomodoroLongBreakTime = preference.pomodoroLongBreakTime,
                pomodoroSessionsBeforeLongBreak = preference.pomodoroSessionsBeforeLongBreak,
                enableNotifications = preference.enableNotifications,
                enableSoundEffects = preference.enableSoundEffects,
                language = preference.language,
                timezone = preference.timezone,
                startOfWeek = preference.startOfWeek
            )
        }
    }
}