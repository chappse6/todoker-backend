package com.todoker.todokerbackend.dto.response

import com.todoker.todokerbackend.domain.category.Category
import java.time.LocalDateTime

/**
 * 카테고리 응답 DTO
 */
data class CategoryResponse(
    val id: Long,
    val name: String,
    val color: String,
    val displayOrder: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val todoCount: Int = 0,
    val completedTodoCount: Int = 0,
    val incompleteTodoCount: Int = 0,
    val completionRate: Int = 0
) {
    companion object {
        fun from(category: Category): CategoryResponse {
            val todoCount = category.getTodoCount()
            val completedCount = category.getCompletedTodoCount()
            val incompleteCount = category.getIncompleteTodoCount()
            val completionRate = if (todoCount > 0) {
                (completedCount * 100 / todoCount)
            } else 0
            
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                color = category.color,
                displayOrder = category.displayOrder,
                isActive = category.isActive,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt,
                todoCount = todoCount,
                completedTodoCount = completedCount,
                incompleteTodoCount = incompleteCount,
                completionRate = completionRate
            )
        }
    }
}

/**
 * 카테고리 요약 응답 DTO (할 일에서 사용)
 */
data class CategorySummaryResponse(
    val id: Long,
    val name: String,
    val color: String
) {
    companion object {
        fun from(category: Category): CategorySummaryResponse {
            return CategorySummaryResponse(
                id = category.id!!,
                name = category.name,
                color = category.color
            )
        }
    }
}