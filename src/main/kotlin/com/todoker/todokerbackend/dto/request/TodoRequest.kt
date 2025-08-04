package com.todoker.todokerbackend.dto.request

import com.todoker.todokerbackend.domain.todo.TodoPriority
import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 할 일 생성 요청 DTO
 */
data class CreateTodoRequest(
    @field:NotBlank(message = "할 일 내용은 필수입니다")
    @field:Size(max = 500, message = "할 일 내용은 500자 이하여야 합니다")
    val text: String,
    
    @field:NotNull(message = "날짜는 필수입니다")
    val date: LocalDate,
    
    val categoryId: Long? = null,
    
    @field:NotNull(message = "우선순위는 필수입니다")
    val priority: TodoPriority = TodoPriority.MEDIUM,
    
    @field:Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    val description: String? = null,
    
    val dueDateTime: LocalDateTime? = null,
    
    @field:Min(value = 1, message = "예상 시간은 1분 이상이어야 합니다")
    @field:Max(value = 1440, message = "예상 시간은 1440분(24시간) 이하여야 합니다")
    val estimatedMinutes: Int? = null
)

/**
 * 할 일 수정 요청 DTO
 */
data class UpdateTodoRequest(
    @field:Size(max = 500, message = "할 일 내용은 500자 이하여야 합니다")
    val text: String? = null,
    
    val categoryId: Long? = null,
    
    val priority: TodoPriority? = null,
    
    @field:Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    val description: String? = null,
    
    val dueDateTime: LocalDateTime? = null,
    
    @field:Min(value = 1, message = "예상 시간은 1분 이상이어야 합니다")
    @field:Max(value = 1440, message = "예상 시간은 1440분(24시간) 이하여야 합니다")
    val estimatedMinutes: Int? = null
)

/**
 * 할 일 날짜 이동 요청 DTO
 */
data class MoveTodoRequest(
    @field:NotNull(message = "이동할 날짜는 필수입니다")
    val newDate: LocalDate
)

/**
 * 할 일 순서 변경 요청 DTO
 */
data class ReorderTodosRequest(
    @field:NotEmpty(message = "할 일 ID 목록은 필수입니다")
    val todoIds: List<Long>
)

/**
 * 할 일 필터링 요청 DTO
 */
data class TodoFilterRequest(
    val date: LocalDate? = null,
    val categoryId: Long? = null,
    val completed: Boolean? = null,
    
    @field:Size(max = 100, message = "검색어는 100자 이하여야 합니다")
    val keyword: String? = null,
    
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) {
    /**
     * 날짜 범위 검증
     */
    @AssertTrue(message = "시작 날짜는 종료 날짜보다 이전이어야 합니다")
    private fun isValidDateRange(): Boolean {
        return startDate == null || endDate == null || !startDate.isAfter(endDate)
    }
}