package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.todo.TodoPriority
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.repository.CategoryRepository
import com.todoker.todokerbackend.repository.TodoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository
) {
    
    fun getTodoById(id: Long, user: User): Todo {
        return todoRepository.findByIdAndUserWithPomodoro(id, user)
            .orElseThrow { NoSuchElementException("Todo not found with id: $id") }
    }
    
    fun getTodosByUser(user: User): List<Todo> {
        return todoRepository.findByUserOrderByDisplayOrderAsc(user)
    }
    
    fun getTodosByDate(user: User, date: LocalDate): List<Todo> {
        return todoRepository.findTodosOptimized(user = user, date = date)
    }
    
    fun getTodosByDateRange(user: User, startDate: LocalDate, endDate: LocalDate): List<Todo> {
        return todoRepository.findTodosOptimized(user = user, startDate = startDate, endDate = endDate)
    }
    
    fun getTodosByFilters(
        user: User,
        date: LocalDate? = null,
        categoryId: Long? = null,
        completed: Boolean? = null
    ): List<Todo> {
        return todoRepository.findTodosOptimized(
            user = user,
            date = date,
            categoryId = categoryId,
            completed = completed
        )
    }
    
    fun searchTodos(user: User, keyword: String): List<Todo> {
        return todoRepository.findTodosOptimized(user = user, keyword = keyword)
    }
    
    /**
     * 통합 최적화 메서드 - 모든 조건을 하나의 메서드로 처리
     */
    fun getTodosOptimized(
        user: User,
        date: LocalDate? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        categoryId: Long? = null,
        completed: Boolean? = null,
        keyword: String? = null
    ): List<Todo> {
        return todoRepository.findTodosOptimized(
            user = user,
            date = date,
            startDate = startDate,
            endDate = endDate,
            categoryId = categoryId,
            completed = completed,
            keyword = keyword
        )
    }
    
    @Transactional
    fun createTodo(
        user: User,
        text: String,
        date: LocalDate = LocalDate.now(),
        categoryId: Long? = null,
        priority: TodoPriority = TodoPriority.MEDIUM,
        description: String? = null,
        dueDateTime: LocalDateTime? = null,
        estimatedMinutes: Int? = null
    ): Todo {
        val category = categoryId?.let {
            categoryRepository.findByIdAndUser(it, user)
                .orElseThrow { NoSuchElementException("Category not found with id: $it") }
        }
        
        val maxOrder = todoRepository.findMaxDisplayOrderByUserIdAndDate(user.id!!, date) ?: -1
        
        val todo = Todo(
            text = text,
            user = user,
            date = date,
            category = category,
            priority = priority,
            description = description,
            dueDateTime = dueDateTime,
            estimatedMinutes = estimatedMinutes,
            displayOrder = maxOrder + 1
        )
        
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun updateTodo(
        todoId: Long,
        user: User,
        text: String? = null,
        categoryId: Long? = null,
        priority: TodoPriority? = null,
        description: String? = null,
        dueDateTime: LocalDateTime? = null,
        estimatedMinutes: Int? = null
    ): Todo {
        val todo = getTodoById(todoId, user)
        
        val category = categoryId?.let {
            categoryRepository.findByIdAndUser(it, user)
                .orElseThrow { NoSuchElementException("Category not found with id: $it") }
        }
        
        todo.update(text, category, priority, description, dueDateTime, estimatedMinutes)
        
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun toggleTodo(todoId: Long, user: User): Todo {
        val todo = getTodoById(todoId, user)
        todo.toggle()
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun completeTodo(todoId: Long, user: User): Todo {
        val todo = getTodoById(todoId, user)
        todo.complete()
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun uncompleteTodo(todoId: Long, user: User): Todo {
        val todo = getTodoById(todoId, user)
        todo.uncomplete()
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun moveTodoToDate(todoId: Long, user: User, newDate: LocalDate): Todo {
        val todo = getTodoById(todoId, user)
        
        val maxOrder = todoRepository.findMaxDisplayOrderByUserIdAndDate(user.id!!, newDate) ?: -1
        
        todo.moveToDate(newDate)
        todo.updateOrder(maxOrder + 1)
        
        return todoRepository.save(todo)
    }
    
    @Transactional
    fun reorderTodos(user: User, date: LocalDate, todoIds: List<Long>): List<Todo> {
        val todos = todoRepository.findByUserAndDateOrderByDisplayOrderAsc(user, date)
        val todoMap = todos.associateBy { it.id }
        
        val reorderedTodos = mutableListOf<Todo>()
        
        todoIds.forEachIndexed { index, todoId ->
            todoMap[todoId]?.let { todo ->
                todo.updateOrder(index)
                reorderedTodos.add(todoRepository.save(todo))
            }
        }
        
        return reorderedTodos
    }
    
    @Transactional
    fun deleteTodo(todoId: Long, user: User) {
        val todo = getTodoById(todoId, user)
        todoRepository.delete(todo)
    }
    
    fun getTodoStats(user: User, date: LocalDate): Map<String, Any> {
        val stats = todoRepository.getTodoStatsOptimized(user.id!!, date = date)
        
        return if (stats.isNotEmpty()) {
            val row = stats[0]
            val total = (row[1] as Number).toLong()
            val completed = (row[2] as Number).toLong()
            val completionRate = (row[3] as Number).toInt()
            
            mapOf(
                "total" to total,
                "completed" to completed,
                "incomplete" to (total - completed),
                "completionRate" to completionRate
            )
        } else {
            mapOf(
                "total" to 0L,
                "completed" to 0L,
                "incomplete" to 0L,
                "completionRate" to 0
            )
        }
    }
    
    fun getTodoStatsByDateRange(user: User, startDate: LocalDate, endDate: LocalDate): List<Map<String, Any>> {
        val stats = todoRepository.getTodoStatsOptimized(user.id!!, startDate = startDate, endDate = endDate)
        
        return stats.map { row ->
            val date = row[0] as LocalDate
            val total = (row[1] as Number).toLong()
            val completed = (row[2] as Number).toLong()
            val completionRate = (row[3] as Number).toInt()
            
            mapOf(
                "date" to date,
                "total" to total,
                "completed" to completed,
                "incomplete" to (total - completed),
                "completionRate" to completionRate
            )
        }
    }
}