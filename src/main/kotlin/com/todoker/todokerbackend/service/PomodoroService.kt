package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.pomodoro.PomodoroSession
import com.todoker.todokerbackend.domain.pomodoro.PomodoroType
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.repository.PomodoroSessionRepository
import com.todoker.todokerbackend.repository.TodoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PomodoroService(
    private val pomodoroSessionRepository: PomodoroSessionRepository,
    private val todoRepository: TodoRepository
) {
    
    fun getSessionById(id: Long): PomodoroSession {
        return pomodoroSessionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Pomodoro session not found with id: $id") }
    }
    
    fun getSessionsByUser(user: User): List<PomodoroSession> {
        return pomodoroSessionRepository.findByUserOrderByStartedAtDesc(user)
    }
    
    fun getCompletedSessionsByUser(user: User): List<PomodoroSession> {
        return pomodoroSessionRepository.findByUserAndIsCompletedOrderByCompletedAtDesc(user, true)
    }
    
    fun getSessionsByDateRange(
        user: User,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<PomodoroSession> {
        return pomodoroSessionRepository.findByUserAndStartedAtBetweenOrderByStartedAtAsc(
            user,
            startDateTime,
            endDateTime
        )
    }
    
    fun getActiveSession(user: User): PomodoroSession? {
        return pomodoroSessionRepository.findActiveSession(user).orElse(null)
    }
    
    @Transactional
    fun startSession(
        user: User,
        type: PomodoroType,
        durationMinutes: Int,
        todoId: Long? = null
    ): PomodoroSession {
        // Check if there's already an active session
        getActiveSession(user)?.let {
            throw IllegalStateException("There is already an active pomodoro session")
        }
        
        val todo = todoId?.let {
            todoRepository.findByIdAndUser(it, user)
                .orElseThrow { NoSuchElementException("Todo not found with id: $it") }
        }
        
        val session = PomodoroSession(
            user = user,
            type = type,
            durationMinutes = durationMinutes,
            todo = todo
        )
        
        return pomodoroSessionRepository.save(session)
    }
    
    @Transactional
    fun completeSession(sessionId: Long, user: User): PomodoroSession {
        val session = getSessionById(sessionId)
        
        if (session.user.id != user.id) {
            throw IllegalArgumentException("Session does not belong to user")
        }
        
        if (!session.isActive()) {
            throw IllegalStateException("Session is not active")
        }
        
        session.complete()
        
        return pomodoroSessionRepository.save(session)
    }
    
    @Transactional
    fun interruptSession(sessionId: Long, user: User, notes: String? = null): PomodoroSession {
        val session = getSessionById(sessionId)
        
        if (session.user.id != user.id) {
            throw IllegalArgumentException("Session does not belong to user")
        }
        
        if (!session.isActive()) {
            throw IllegalStateException("Session is not active")
        }
        
        session.interrupt(notes)
        
        return pomodoroSessionRepository.save(session)
    }
    
    fun getTodayStats(user: User): Map<String, Any> {
        val today = LocalDateTime.now()
        val startOfDay = today.toLocalDate().atStartOfDay()
        val endOfDay = today.toLocalDate().atTime(23, 59, 59)
        
        val workSessions = pomodoroSessionRepository.countCompletedByTypeAndDate(
            user.id!!,
            PomodoroType.WORK,
            today
        )
        
        val shortBreaks = pomodoroSessionRepository.countCompletedByTypeAndDate(
            user.id!!,
            PomodoroType.SHORT_BREAK,
            today
        )
        
        val longBreaks = pomodoroSessionRepository.countCompletedByTypeAndDate(
            user.id!!,
            PomodoroType.LONG_BREAK,
            today
        )
        
        val totalWorkMinutes = pomodoroSessionRepository.getTotalWorkMinutesByDate(user.id!!, today) ?: 0
        
        return mapOf(
            "date" to today.toLocalDate(),
            "workSessions" to workSessions,
            "shortBreaks" to shortBreaks,
            "longBreaks" to longBreaks,
            "totalSessions" to (workSessions + shortBreaks + longBreaks),
            "totalWorkMinutes" to totalWorkMinutes,
            "totalWorkHours" to String.format("%.1f", totalWorkMinutes / 60.0)
        )
    }
    
    fun getStatsByDateRange(
        user: User,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<Map<String, Any>> {
        val stats = pomodoroSessionRepository.getStatsByDateRange(user.id!!, startDateTime, endDateTime)
        
        val groupedStats = stats.groupBy { it[0] } // Group by date
        
        return groupedStats.map { (date, sessions) ->
            val workCount = sessions.find { it[2] == PomodoroType.WORK }?.get(1) as? Long ?: 0
            val shortBreakCount = sessions.find { it[2] == PomodoroType.SHORT_BREAK }?.get(1) as? Long ?: 0
            val longBreakCount = sessions.find { it[2] == PomodoroType.LONG_BREAK }?.get(1) as? Long ?: 0
            
            mapOf(
                "date" to date,
                "workSessions" to workCount,
                "shortBreaks" to shortBreakCount,
                "longBreaks" to longBreakCount,
                "totalSessions" to (workCount + shortBreakCount + longBreakCount)
            )
        }
    }
    
    fun getHourlyDistribution(user: User): Map<Int, Long> {
        val distribution = pomodoroSessionRepository.getHourlyDistribution(user.id!!)
        
        return distribution.associate {
            (it[0] as Int) to (it[1] as Long)
        }
    }
    
    fun getTotalStats(user: User): Map<String, Any> {
        val totalSessions = pomodoroSessionRepository.countByUserAndIsCompleted(user, true)
        val completedSessions = pomodoroSessionRepository.countByUserAndIsCompleted(user, true)
        
        val sessions = pomodoroSessionRepository.findByUserAndIsCompletedOrderByCompletedAtDesc(user, true)
        val totalWorkMinutes = sessions
            .filter { it.type == PomodoroType.WORK }
            .sumOf { it.durationMinutes }
        
        return mapOf(
            "totalSessions" to totalSessions,
            "completedSessions" to completedSessions,
            "totalWorkMinutes" to totalWorkMinutes,
            "totalWorkHours" to String.format("%.1f", totalWorkMinutes / 60.0),
            "averageSessionsPerDay" to if (sessions.isNotEmpty()) {
                val days = sessions.map { it.completedAt!!.toLocalDate() }.distinct().size
                String.format("%.1f", totalSessions.toDouble() / days)
            } else "0.0"
        )
    }
}