package com.todoker.todokerbackend.domain.todo

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.common.BaseEntity
import com.todoker.todokerbackend.domain.pomodoro.PomodoroSession
import com.todoker.todokerbackend.domain.user.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "todos",
    indexes = [
        Index(name = "idx_todo_user", columnList = "user_id"),
        Index(name = "idx_todo_date", columnList = "user_id, date"),
        Index(name = "idx_todo_category", columnList = "category_id"),
        Index(name = "idx_todo_completed", columnList = "user_id, completed"),
        Index(name = "idx_todo_order", columnList = "user_id, displayOrder")
    ]
)
class Todo(
    @Column(nullable = false, length = 500)
    var text: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,
    
    @Column(nullable = false)
    var completed: Boolean = false,
    
    @Column
    var completedAt: LocalDateTime? = null,
    
    @Column(nullable = false)
    var displayOrder: Int = 0,
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var priority: TodoPriority = TodoPriority.MEDIUM,
    
    @Column(length = 1000)
    var description: String? = null,
    
    @Column
    var dueDateTime: LocalDateTime? = null,
    
    @Column
    var estimatedMinutes: Int? = null,
    
    @Column
    var actualMinutes: Int? = null
) : BaseEntity() {
    
    @OneToMany(mappedBy = "todo", fetch = FetchType.LAZY)
    val pomodoroSessions: MutableSet<PomodoroSession> = mutableSetOf()
    
    fun toggle(): Boolean {
        completed = !completed
        completedAt = if (completed) LocalDateTime.now() else null
        return completed
    }
    
    fun complete() {
        if (!completed) {
            completed = true
            completedAt = LocalDateTime.now()
        }
    }
    
    fun uncomplete() {
        if (completed) {
            completed = false
            completedAt = null
        }
    }
    
    fun update(
        text: String? = null,
        category: Category? = null,
        priority: TodoPriority? = null,
        description: String? = null,
        dueDateTime: LocalDateTime? = null,
        estimatedMinutes: Int? = null
    ) {
        text?.let { this.text = it }
        category?.let { this.category = it }
        priority?.let { this.priority = it }
        description?.let { this.description = it }
        dueDateTime?.let { this.dueDateTime = it }
        estimatedMinutes?.let { this.estimatedMinutes = it }
    }
    
    fun updateOrder(newOrder: Int) {
        this.displayOrder = newOrder
    }
    
    fun moveToDate(newDate: LocalDate) {
        this.date = newDate
    }
    
    fun addPomodoroTime(minutes: Int) {
        actualMinutes = (actualMinutes ?: 0) + minutes
    }
    
    fun getTotalPomodoroMinutes(): Int {
        return pomodoroSessions.sumOf { it.durationMinutes }
    }
}

enum class TodoPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}