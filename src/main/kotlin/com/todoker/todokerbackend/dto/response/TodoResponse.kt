package com.todoker.todokerbackend.dto.response

import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.todo.TodoPriority
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 할 일 응답 DTO
 */
data class TodoResponse(
    val id: Long,
    val text: String,
    val date: LocalDate,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
    val displayOrder: Int,
    val priority: TodoPriority,
    val description: String?,
    val dueDateTime: LocalDateTime?,
    val estimatedMinutes: Int?,
    val actualMinutes: Int?,
    val category: CategorySummaryResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val pomodoroCount: Int = 0,
    val totalPomodoroMinutes: Int = 0
) {
    companion object {
        fun from(todo: Todo): TodoResponse {
            return TodoResponse(
                id = todo.id!!,
                text = todo.text,
                date = todo.date,
                completed = todo.completed,
                completedAt = todo.completedAt,
                displayOrder = todo.displayOrder,
                priority = todo.priority,
                description = todo.description,
                dueDateTime = todo.dueDateTime,
                estimatedMinutes = todo.estimatedMinutes,
                actualMinutes = todo.actualMinutes,
                category = todo.category?.let { CategorySummaryResponse.from(it) },
                createdAt = todo.createdAt,
                updatedAt = todo.updatedAt,
                pomodoroCount = todo.pomodoroSessions.size,
                totalPomodoroMinutes = todo.getTotalPomodoroMinutes()
            )
        }
    }
}

/**
 * 할 일 목록 응답 DTO (간소화된 정보)
 */
data class TodoSummaryResponse(
    val id: Long,
    val text: String,
    val completed: Boolean,
    val priority: TodoPriority,
    val category: CategorySummaryResponse?,
    val dueDateTime: LocalDateTime?
) {
    companion object {
        fun from(todo: Todo): TodoSummaryResponse {
            return TodoSummaryResponse(
                id = todo.id!!,
                text = todo.text,
                completed = todo.completed,
                priority = todo.priority,
                category = todo.category?.let { CategorySummaryResponse.from(it) },
                dueDateTime = todo.dueDateTime
            )
        }
    }
}

/**
 * 할 일 통계 응답 DTO
 */
data class TodoStatsResponse(
    val date: LocalDate,
    val total: Long,
    val completed: Long,
    val incomplete: Long,
    val completionRate: Int
)