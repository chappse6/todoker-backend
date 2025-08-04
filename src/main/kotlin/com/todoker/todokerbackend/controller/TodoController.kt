package com.todoker.todokerbackend.controller

import com.todoker.todokerbackend.common.response.ApiResponse
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.request.*
import com.todoker.todokerbackend.dto.response.TodoResponse
import com.todoker.todokerbackend.dto.response.TodoStatsResponse
import com.todoker.todokerbackend.service.TodoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 할 일 관리 API 컨트롤러
 * 할 일의 CRUD, 순서 변경, 완료 상태 토글 등의 기능을 제공
 */
@Tag(name = "Todo", description = "할 일 관리 API")
@RestController
@RequestMapping("/todos")
class TodoController(
    private val todoService: TodoService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 할 일 목록 조회 (필터링 지원)
     * 날짜, 카테고리, 완료 상태, 키워드로 필터링 가능
     */
    @Operation(summary = "할 일 목록 조회", description = "사용자의 할 일 목록을 조회합니다. 다양한 필터링 옵션을 지원합니다.")
    @GetMapping
    fun getTodos(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "특정 날짜의 할 일만 조회")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @Parameter(description = "특정 카테고리의 할 일만 조회")
        @RequestParam categoryId: Long?,
        @Parameter(description = "완료 상태로 필터링 (true: 완료된 할 일, false: 미완료 할 일)")
        @RequestParam completed: Boolean?,
        @Parameter(description = "검색 키워드")
        @RequestParam keyword: String?,
        @Parameter(description = "날짜 범위 시작")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @Parameter(description = "날짜 범위 끝")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ApiResponse<List<TodoResponse>> {
        logger.debug("Getting todos for user: ${user.getUsername()}, filters: date=$date, categoryId=$categoryId, completed=$completed")
        
        val todos = when {
            // 키워드 검색이 있는 경우
            !keyword.isNullOrBlank() -> todoService.searchTodos(user, keyword.trim())
            
            // 날짜 범위가 지정된 경우
            startDate != null && endDate != null -> todoService.getTodosByDateRange(user, startDate, endDate)
            
            // 필터링 조건이 있는 경우
            date != null || categoryId != null || completed != null -> 
                todoService.getTodosByFilters(user, date, categoryId, completed)
            
            // 특정 날짜 조회
            date != null -> todoService.getTodosByDate(user, date)
            
            // 모든 할 일 조회 (기본값: 오늘)
            else -> todoService.getTodosByDate(user, LocalDate.now())
        }
        
        val response = todos.map { TodoResponse.from(it) }
        return ApiResponse.success(response)
    }
    
    /**
     * 특정 할 일 상세 조회
     */
    @Operation(summary = "할 일 상세 조회", description = "특정 할 일의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    fun getTodo(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "할 일 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<TodoResponse> {
        logger.debug("Getting todo: id=$id, user=${user.getUsername()}")
        
        val todo = todoService.getTodoById(id, user)
        return ApiResponse.success(TodoResponse.from(todo))
    }
    
    /**
     * 새로운 할 일 생성
     */
    @Operation(summary = "할 일 생성", description = "새로운 할 일을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTodo(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateTodoRequest
    ): ApiResponse<TodoResponse> {
        logger.info("Creating todo for user: ${user.getUsername()}, text: ${request.text}")
        
        val todo = todoService.createTodo(
            user = user,
            text = request.text,
            date = request.date,
            categoryId = request.categoryId,
            priority = request.priority,
            description = request.description,
            dueDateTime = request.dueDateTime,
            estimatedMinutes = request.estimatedMinutes
        )
        
        return ApiResponse.success(TodoResponse.from(todo))
    }
    
    /**
     * 할 일 수정
     */
    @Operation(summary = "할 일 수정", description = "기존 할 일의 정보를 수정합니다.")
    @PutMapping("/{id}")
    fun updateTodo(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "할 일 ID", required = true)
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTodoRequest
    ): ApiResponse<TodoResponse> {
        logger.info("Updating todo: id=$id, user=${user.getUsername()}")
        
        val todo = todoService.updateTodo(
            todoId = id,
            user = user,
            text = request.text,
            categoryId = request.categoryId,
            priority = request.priority,
            description = request.description,
            dueDateTime = request.dueDateTime,
            estimatedMinutes = request.estimatedMinutes
        )
        
        return ApiResponse.success(TodoResponse.from(todo))
    }
    
    /**
     * 할 일 완료/미완료 토글
     */
    @Operation(summary = "할 일 완료 상태 토글", description = "할 일의 완료 상태를 토글합니다.")
    @PatchMapping("/{id}/toggle")
    fun toggleTodo(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "할 일 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<TodoResponse> {
        logger.info("Toggling todo: id=$id, user=${user.getUsername()}")
        
        val todo = todoService.toggleTodo(id, user)
        return ApiResponse.success(TodoResponse.from(todo))
    }
    
    /**
     * 할 일을 다른 날짜로 이동
     */
    @Operation(summary = "할 일 날짜 이동", description = "할 일을 다른 날짜로 이동합니다.")
    @PatchMapping("/{id}/move")
    fun moveTodo(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "할 일 ID", required = true)
        @PathVariable id: Long,
        @Valid @RequestBody request: MoveTodoRequest
    ): ApiResponse<TodoResponse> {
        logger.info("Moving todo: id=$id, newDate=${request.newDate}, user=${user.getUsername()}")
        
        val todo = todoService.moveTodoToDate(id, user, request.newDate)
        return ApiResponse.success(TodoResponse.from(todo))
    }
    
    /**
     * 할 일 순서 변경 (드래그 앤 드롭)
     */
    @Operation(summary = "할 일 순서 변경", description = "특정 날짜의 할 일들의 순서를 변경합니다.")
    @PutMapping("/reorder")
    fun reorderTodos(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "날짜", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @Valid @RequestBody request: ReorderTodosRequest
    ): ApiResponse<List<TodoResponse>> {
        logger.info("Reordering todos for date: $date, user: ${user.getUsername()}, count: ${request.todoIds.size}")
        
        val todos = todoService.reorderTodos(user, date, request.todoIds)
        val response = todos.map { TodoResponse.from(it) }
        
        return ApiResponse.success(response)
    }
    
    /**
     * 할 일 삭제
     */
    @Operation(summary = "할 일 삭제", description = "할 일을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTodo(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "할 일 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<Nothing> {
        logger.info("Deleting todo: id=$id, user=${user.getUsername()}")
        
        todoService.deleteTodo(id, user)
        return ApiResponse.success()
    }
    
    /**
     * 특정 날짜의 할 일 통계 조회
     */
    @Operation(summary = "일별 할 일 통계", description = "특정 날짜의 할 일 통계를 조회합니다.")
    @GetMapping("/stats/daily")
    fun getDailyStats(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "통계를 조회할 날짜")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ApiResponse<TodoStatsResponse> {
        val targetDate = date ?: LocalDate.now()
        logger.debug("Getting daily stats for user: ${user.getUsername()}, date: $targetDate")
        
        val stats = todoService.getTodoStats(user, targetDate)
        val response = TodoStatsResponse(
            date = targetDate,
            total = stats["total"] as Long,
            completed = stats["completed"] as Long,
            incomplete = stats["incomplete"] as Long,
            completionRate = stats["completionRate"] as Int
        )
        
        return ApiResponse.success(response)
    }
    
    /**
     * 기간별 할 일 통계 조회
     */
    @Operation(summary = "기간별 할 일 통계", description = "특정 기간의 일별 할 일 통계를 조회합니다.")
    @GetMapping("/stats/range")
    fun getRangeStats(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "시작 날짜", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "종료 날짜", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<List<TodoStatsResponse>> {
        logger.debug("Getting range stats for user: ${user.getUsername()}, range: $startDate ~ $endDate")
        
        // 날짜 범위 검증
        if (startDate.isAfter(endDate)) {
            throw IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다")
        }
        
        val stats = todoService.getTodoStatsByDateRange(user, startDate, endDate)
        val response = stats.map { stat ->
            TodoStatsResponse(
                date = stat["date"] as LocalDate,
                total = stat["total"] as Long,
                completed = stat["completed"] as Long,
                incomplete = stat["incomplete"] as Long,
                completionRate = stat["completionRate"] as Int
            )
        }
        
        return ApiResponse.success(response)
    }
}