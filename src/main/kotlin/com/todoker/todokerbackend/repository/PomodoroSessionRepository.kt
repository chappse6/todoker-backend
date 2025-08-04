package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.pomodoro.PomodoroSession
import com.todoker.todokerbackend.domain.pomodoro.PomodoroType
import com.todoker.todokerbackend.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface PomodoroSessionRepository : JpaRepository<PomodoroSession, Long> {
    
    fun findByUserOrderByStartedAtDesc(user: User): List<PomodoroSession>
    
    fun findByUserAndIsCompletedOrderByCompletedAtDesc(user: User, isCompleted: Boolean): List<PomodoroSession>
    
    fun findByUserAndStartedAtBetweenOrderByStartedAtAsc(
        user: User,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<PomodoroSession>
    
    @Query("""
        SELECT p FROM PomodoroSession p 
        WHERE p.user = :user 
        AND p.isCompleted = false 
        AND p.isInterrupted = false 
        AND p.completedAt IS NULL
        ORDER BY p.startedAt DESC
    """)
    fun findActiveSession(@Param("user") user: User): Optional<PomodoroSession>
    
    @Query("""
        SELECT COUNT(p) FROM PomodoroSession p 
        WHERE p.user.id = :userId 
        AND p.isCompleted = true 
        AND p.type = :type
        AND DATE(p.completedAt) = DATE(:date)
    """)
    fun countCompletedByTypeAndDate(
        @Param("userId") userId: Long,
        @Param("type") type: PomodoroType,
        @Param("date") date: LocalDateTime
    ): Long
    
    @Query("""
        SELECT DATE(p.completedAt) as date, COUNT(p) as count, p.type
        FROM PomodoroSession p 
        WHERE p.user.id = :userId 
        AND p.isCompleted = true
        AND p.completedAt BETWEEN :startDate AND :endDate
        GROUP BY DATE(p.completedAt), p.type
        ORDER BY DATE(p.completedAt)
    """)
    fun getStatsByDateRange(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
    
    @Query("""
        SELECT SUM(p.durationMinutes) FROM PomodoroSession p 
        WHERE p.user.id = :userId 
        AND p.isCompleted = true
        AND p.type = 'WORK'
        AND DATE(p.completedAt) = DATE(:date)
    """)
    fun getTotalWorkMinutesByDate(
        @Param("userId") userId: Long,
        @Param("date") date: LocalDateTime
    ): Long?
    
    @Query("""
        SELECT p FROM PomodoroSession p 
        LEFT JOIN FETCH p.todo t
        WHERE p.user = :user 
        AND p.todo IS NOT NULL
        ORDER BY p.startedAt DESC
    """)
    fun findByUserWithTodo(@Param("user") user: User): List<PomodoroSession>
    
    fun countByUserAndIsCompleted(user: User, isCompleted: Boolean): Long
    
    @Query("""
        SELECT HOUR(p.completedAt), COUNT(p)
        FROM PomodoroSession p
        WHERE p.user.id = :userId
        AND p.isCompleted = true
        AND p.type = 'WORK'
        GROUP BY HOUR(p.completedAt)
        ORDER BY HOUR(p.completedAt)
    """)
    fun getHourlyDistribution(@Param("userId") userId: Long): List<Array<Any>>
}