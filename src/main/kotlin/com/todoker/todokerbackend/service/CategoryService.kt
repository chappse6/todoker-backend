package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    
    fun getCategoryById(id: Long, user: User): Category {
        return categoryRepository.findByIdAndUser(id, user)
            .orElseThrow { NoSuchElementException("Category not found with id: $id") }
    }
    
    fun getCategoriesByUser(user: User): List<Category> {
        return categoryRepository.findByUserOrderByDisplayOrderAsc(user)
    }
    
    fun getActiveCategoriesByUser(user: User): List<Category> {
        return categoryRepository.findByUserAndIsActiveOrderByDisplayOrderAsc(user, true)
    }
    
    @Transactional
    fun createCategory(
        user: User,
        name: String,
        color: String = "#4CAF50"
    ): Category {
        if (categoryRepository.existsByUserAndName(user, name)) {
            throw IllegalArgumentException("Category with name '$name' already exists")
        }
        
        val maxOrder = categoryRepository.findMaxDisplayOrderByUserId(user.id!!) ?: -1
        
        val category = Category(
            name = name,
            color = color,
            user = user,
            displayOrder = maxOrder + 1
        )
        
        return categoryRepository.save(category)
    }
    
    @Transactional
    fun updateCategory(
        categoryId: Long,
        user: User,
        name: String? = null,
        color: String? = null
    ): Category {
        val category = getCategoryById(categoryId, user)
        
        name?.let {
            if (it != category.name && categoryRepository.existsByUserAndName(user, it)) {
                throw IllegalArgumentException("Category with name '$it' already exists")
            }
        }
        
        category.update(name, color)
        
        return categoryRepository.save(category)
    }
    
    @Transactional
    fun reorderCategories(user: User, categoryIds: List<Long>): List<Category> {
        val categories = categoryRepository.findByUserOrderByDisplayOrderAsc(user)
        val categoryMap = categories.associateBy { it.id }
        
        val reorderedCategories = mutableListOf<Category>()
        
        categoryIds.forEachIndexed { index, categoryId ->
            categoryMap[categoryId]?.let { category ->
                category.update(displayOrder = index)
                reorderedCategories.add(categoryRepository.save(category))
            }
        }
        
        return reorderedCategories
    }
    
    @Transactional
    fun deleteCategory(categoryId: Long, user: User) {
        val category = getCategoryById(categoryId, user)
        
        // Soft delete - just deactivate
        category.deactivate()
        categoryRepository.save(category)
    }
    
    @Transactional
    fun activateCategory(categoryId: Long, user: User): Category {
        val category = getCategoryById(categoryId, user)
        category.activate()
        return categoryRepository.save(category)
    }
    
    fun getCategoryStats(user: User): List<Map<String, Any>> {
        val categories = getActiveCategoriesByUser(user)
        
        return categories.map { category ->
            mapOf(
                "id" to category.id!!,
                "name" to category.name,
                "color" to category.color,
                "totalTodos" to category.getTodoCount(),
                "completedTodos" to category.getCompletedTodoCount(),
                "incompleteTodos" to category.getIncompleteTodoCount(),
                "completionRate" to if (category.getTodoCount() > 0) {
                    (category.getCompletedTodoCount().toDouble() / category.getTodoCount() * 100).toInt()
                } else 0
            )
        }
    }
}