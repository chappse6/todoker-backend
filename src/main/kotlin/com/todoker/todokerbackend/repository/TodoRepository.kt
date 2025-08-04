package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {
    
    fun findByIdAndUser(id: Long, user: User): Optional<Todo>
    
    fun findByUserOrderByDisplayOrderAsc(user: User): List<Todo>
    
    fun findByUserAndDateOrderByDisplayOrderAsc(user: User, date: LocalDate): List<Todo>
    
    fun findByUserAndDateBetweenOrderByDateAscDisplayOrderAsc(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Todo>
    
    fun findByUserAndCategoryOrderByDisplayOrderAsc(user: User, category: Category): List<Todo>
    
    fun findByUserAndCompletedOrderByCompletedAtDesc(user: User, completed: Boolean): List<Todo>
    
    @Query("""
        SELECT t FROM Todo t 
        WHERE t.user = :user 
        AND (:date IS NULL OR t.date = :date)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (:completed IS NULL OR t.completed = :completed)
        ORDER BY t.displayOrder ASC
    """)
    fun findByFilters(
        @Param("user") user: User,
        @Param("date") date: LocalDate?,
        @Param("categoryId") categoryId: Long?,
        @Param("completed") completed: Boolean?
    ): List<Todo>
    
    fun findByUserAndTextContainingIgnoreCaseOrderByDateDescDisplayOrderAsc(
        user: User,
        keyword: String
    ): List<Todo>
    
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId AND t.completed = true AND t.date = :date")
    fun countCompletedByUserIdAndDate(@Param("userId") userId: Long, @Param("date") date: LocalDate): Long
    
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId AND t.date = :date")
    fun countByUserIdAndDate(@Param("userId") userId: Long, @Param("date") date: LocalDate): Long
    
    @Query("""
        SELECT t.date, COUNT(t), SUM(CASE WHEN t.completed = true THEN 1 ELSE 0 END)
        FROM Todo t 
        WHERE t.user.id = :userId AND t.date BETWEEN :startDate AND :endDate
        GROUP BY t.date
        ORDER BY t.date
    """)
    fun getStatsByDateRange(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Array<Any>>
    
    @Query("SELECT MAX(t.displayOrder) FROM Todo t WHERE t.user.id = :userId AND t.date = :date")
    fun findMaxDisplayOrderByUserIdAndDate(@Param("userId") userId: Long, @Param("date") date: LocalDate): Int?
    
    @Modifying
    @Query("""
        UPDATE Todo t SET t.displayOrder = t.displayOrder + 1 
        WHERE t.user.id = :userId AND t.date = :date AND t.displayOrder >= :startOrder
    """)
    fun incrementDisplayOrderFrom(
        @Param("userId") userId: Long,
        @Param("date") date: LocalDate,
        @Param("startOrder") startOrder: Int
    )
    
    fun deleteByIdAndUser(id: Long, user: User)
    
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.category WHERE t.user = :user AND t.date = :date ORDER BY t.displayOrder")
    fun findByUserAndDateWithCategory(@Param("user") user: User, @Param("date") date: LocalDate): List<Todo>
    
    fun findByUserAndDueDateTimeBetween(
        user: User,
        startDateTime: java.time.LocalDateTime,
        endDateTime: java.time.LocalDateTime
    ): List<Todo>
}