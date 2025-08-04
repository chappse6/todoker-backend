package com.todoker.todokerbackend.domain.category

import com.todoker.todokerbackend.domain.common.BaseEntity
import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.user.User
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_category_user", columnList = "user_id"),
        Index(name = "idx_category_order", columnList = "user_id, displayOrder")
    ],
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "name"])
    ]
)
class Category(
    @Column(nullable = false, length = 50)
    var name: String,
    
    @Column(nullable = false, length = 7)
    var color: String = "#4CAF50",
    
    @Column(nullable = false)
    var displayOrder: Int = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false)
    var isActive: Boolean = true
) : BaseEntity() {
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    val todos: MutableSet<Todo> = mutableSetOf()
    
    fun update(name: String? = null, color: String? = null, displayOrder: Int? = null) {
        name?.let { this.name = it }
        color?.let { this.color = it }
        displayOrder?.let { this.displayOrder = it }
    }
    
    fun deactivate() {
        this.isActive = false
    }
    
    fun activate() {
        this.isActive = true
    }
    
    fun getTodoCount(): Int = todos.size
    
    fun getCompletedTodoCount(): Int = todos.count { it.completed }
    
    fun getIncompleteTodoCount(): Int = todos.count { !it.completed }
}