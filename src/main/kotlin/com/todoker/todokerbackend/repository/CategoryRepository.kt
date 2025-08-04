package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    
    fun findByUserOrderByDisplayOrderAsc(user: User): List<Category>
    
    fun findByUserAndIsActiveOrderByDisplayOrderAsc(user: User, isActive: Boolean): List<Category>
    
    fun findByIdAndUser(id: Long, user: User): Optional<Category>
    
    fun existsByUserAndName(user: User, name: String): Boolean
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.isActive = true ORDER BY c.displayOrder")
    fun findActiveByUserId(@Param("userId") userId: Long): List<Category>
    
    @Query("SELECT MAX(c.displayOrder) FROM Category c WHERE c.user.id = :userId")
    fun findMaxDisplayOrderByUserId(@Param("userId") userId: Long): Int?
    
    @Modifying
    @Query("UPDATE Category c SET c.displayOrder = c.displayOrder + 1 WHERE c.user.id = :userId AND c.displayOrder >= :startOrder")
    fun incrementDisplayOrderFrom(@Param("userId") userId: Long, @Param("startOrder") startOrder: Int)
    
    @Query("SELECT COUNT(c) FROM Category c WHERE c.user.id = :userId AND c.isActive = true")
    fun countActiveByUserId(@Param("userId") userId: Long): Long
}