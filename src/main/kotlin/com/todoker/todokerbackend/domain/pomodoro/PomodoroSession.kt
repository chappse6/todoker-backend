package com.todoker.todokerbackend.domain.pomodoro

import com.todoker.todokerbackend.domain.common.BaseEntity
import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "pomodoro_sessions",
    indexes = [
        Index(name = "idx_pomodoro_user", columnList = "user_id"),
        Index(name = "idx_pomodoro_todo", columnList = "todo_id"),
        Index(name = "idx_pomodoro_completed", columnList = "user_id, completedAt")
    ]
)
class PomodoroSession(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: PomodoroType,
    
    @Column(nullable = false)
    var durationMinutes: Int,
    
    @Column(nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    var completedAt: LocalDateTime? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id")
    var todo: Todo? = null,
    
    @Column(nullable = false)
    var isCompleted: Boolean = false,
    
    @Column(nullable = false)
    var isInterrupted: Boolean = false,
    
    @Column
    var notes: String? = null
) : BaseEntity() {
    
    fun complete() {
        if (!isCompleted) {
            isCompleted = true
            completedAt = LocalDateTime.now()
            todo?.addPomodoroTime(durationMinutes)
        }
    }
    
    fun interrupt(notes: String? = null) {
        if (!isCompleted) {
            isInterrupted = true
            completedAt = LocalDateTime.now()
            notes?.let { this.notes = it }
        }
    }
    
    fun getActualDurationMinutes(): Int {
        val endTime = completedAt ?: LocalDateTime.now()
        val minutes = java.time.Duration.between(startedAt, endTime).toMinutes().toInt()
        return minOf(minutes, durationMinutes)
    }
    
    fun isActive(): Boolean {
        return !isCompleted && !isInterrupted && completedAt == null
    }
}

enum class PomodoroType {
    WORK,
    SHORT_BREAK,
    LONG_BREAK
}