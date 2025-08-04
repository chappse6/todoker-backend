package com.todoker.todokerbackend.domain.user

import com.todoker.todokerbackend.domain.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "user_preferences")
class UserPreference(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(length = 7)
    var themeBackground: String = "#1a1a1a",
    
    @Column(length = 7)
    var themeElement: String = "#2d2d2d",
    
    @Column(nullable = false)
    var pomodoroWorkTime: Int = 25,
    
    @Column(nullable = false)
    var pomodoroBreakTime: Int = 5,
    
    @Column(nullable = false)
    var pomodoroLongBreakTime: Int = 15,
    
    @Column(nullable = false)
    var pomodoroSessionsBeforeLongBreak: Int = 4,
    
    @Column(nullable = false)
    var enableNotifications: Boolean = true,
    
    @Column(nullable = false)
    var enableSoundEffects: Boolean = true,
    
    @Column(length = 10)
    var language: String = "ko",
    
    @Column(length = 50)
    var timezone: String = "Asia/Seoul",
    
    @Column(nullable = false)
    var startOfWeek: Int = 1
) : BaseEntity() {
    
    fun updateTheme(background: String? = null, element: String? = null) {
        background?.let { themeBackground = it }
        element?.let { themeElement = it }
    }
    
    fun updatePomodoroSettings(
        workTime: Int? = null,
        breakTime: Int? = null,
        longBreakTime: Int? = null,
        sessionsBeforeLongBreak: Int? = null
    ) {
        workTime?.let { pomodoroWorkTime = it }
        breakTime?.let { pomodoroBreakTime = it }
        longBreakTime?.let { pomodoroLongBreakTime = it }
        sessionsBeforeLongBreak?.let { pomodoroSessionsBeforeLongBreak = it }
    }
    
    fun updateNotificationSettings(enableNotifications: Boolean? = null, enableSoundEffects: Boolean? = null) {
        enableNotifications?.let { this.enableNotifications = it }
        enableSoundEffects?.let { this.enableSoundEffects = it }
    }
    
    fun updateLocalizationSettings(language: String? = null, timezone: String? = null, startOfWeek: Int? = null) {
        language?.let { this.language = it }
        timezone?.let { this.timezone = it }
        startOfWeek?.let { this.startOfWeek = it }
    }
}