package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.user.User
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class StatsService(
    private val todoService: TodoService,
    private val categoryService: CategoryService,
    private val pomodoroService: PomodoroService
) {
    
    fun getDashboardStats(user: User): Map<String, Any> {
        val today = LocalDate.now()
        val todayStats = todoService.getTodoStats(user, today)
        val pomodoroTodayStats = pomodoroService.getTodayStats(user)
        val categoryStats = categoryService.getCategoryStats(user)
        
        return mapOf(
            "today" to todayStats,
            "pomodoro" to pomodoroTodayStats,
            "categories" to categoryStats,
            "streak" to calculateStreak(user)
        )
    }
    
    fun getWeeklyStats(user: User): Map<String, Any> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        
        val dailyStats = todoService.getTodoStatsByDateRange(user, startOfWeek, endOfWeek)
        val pomodoroStats = pomodoroService.getStatsByDateRange(
            user,
            startOfWeek.atStartOfDay(),
            endOfWeek.atTime(23, 59, 59)
        )
        
        return mapOf(
            "startDate" to startOfWeek,
            "endDate" to endOfWeek,
            "dailyTodos" to dailyStats,
            "dailyPomodoros" to pomodoroStats,
            "weeklyCompletion" to calculateWeeklyCompletion(dailyStats),
            "mostProductiveDay" to (findMostProductiveDay(dailyStats) ?: emptyMap())
        )
    }
    
    fun getMonthlyStats(user: User): Map<String, Any> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
        
        val dailyStats = todoService.getTodoStatsByDateRange(user, startOfMonth, endOfMonth)
        
        return mapOf(
            "month" to today.month,
            "year" to today.year,
            "dailyStats" to dailyStats,
            "monthlyCompletion" to calculateMonthlyCompletion(dailyStats),
            "totalCompleted" to dailyStats.sumOf { 
                (it["completed"] as? Number)?.toLong() ?: 0L 
            },
            "totalTodos" to dailyStats.sumOf { 
                (it["total"] as? Number)?.toLong() ?: 0L 
            },
            "activeDays" to dailyStats.count { 
                ((it["total"] as? Number)?.toLong() ?: 0L) > 0 
            }
        )
    }
    
    fun getHeatmapData(user: User, months: Int = 12): List<Map<String, Any>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(months.toLong())
        
        val dailyStats = todoService.getTodoStatsByDateRange(user, startDate, endDate)
        
        return dailyStats.map { stat ->
            val completed = (stat["completed"] as? Number)?.toLong() ?: 0L
            val total = (stat["total"] as? Number)?.toLong() ?: 0L
            val date = stat["date"] ?: LocalDate.now()
            
            mapOf<String, Any>(
                "date" to date,
                "count" to completed,
                "level" to calculateHeatmapLevel(completed, total)
            )
        }
    }
    
    fun getProductivityAnalysis(user: User): Map<String, Any> {
        val hourlyDistribution = pomodoroService.getHourlyDistribution(user)
        val categoryStats = categoryService.getCategoryStats(user)
        val totalPomodoroStats = pomodoroService.getTotalStats(user)
        
        return mapOf(
            "hourlyDistribution" to hourlyDistribution,
            "mostProductiveHours" to findMostProductiveHours(hourlyDistribution),
            "categoryDistribution" to categoryStats,
            "focusMetrics" to totalPomodoroStats,
            "recommendations" to generateProductivityRecommendations(
                hourlyDistribution,
                categoryStats,
                totalPomodoroStats
            )
        )
    }
    
    private fun calculateStreak(user: User): Map<String, Any> {
        var currentStreak = 0
        var maxStreak = 0
        var tempStreak = 0
        var checkDate = LocalDate.now()
        
        // Check backwards from today
        while (true) {
            val stats = todoService.getTodoStats(user, checkDate)
            val completed = (stats["completed"] as? Number)?.toLong() ?: 0L
            val total = (stats["total"] as? Number)?.toLong() ?: 0L
            
            if (total > 0 && completed > 0) {
                tempStreak++
                if (checkDate == LocalDate.now() || checkDate == LocalDate.now().minusDays(1)) {
                    currentStreak = tempStreak
                }
                maxStreak = maxOf(maxStreak, tempStreak)
            } else if (total > 0) {
                // Had todos but didn't complete any - streak broken
                if (checkDate.isBefore(LocalDate.now())) {
                    break
                }
            }
            
            checkDate = checkDate.minusDays(1)
            
            // Stop checking after 365 days
            if (checkDate.isBefore(LocalDate.now().minusYears(1))) {
                break
            }
        }
        
        return mapOf(
            "current" to currentStreak,
            "max" to maxStreak,
            "startDate" to if (currentStreak > 0) {
                LocalDate.now().minusDays(currentStreak.toLong() - 1)
            } else {
                LocalDate.now()
            }
        )
    }
    
    private fun calculateWeeklyCompletion(dailyStats: List<Map<String, Any>>): Double {
        return calculateCompletionRate(dailyStats)
    }
    
    private fun calculateMonthlyCompletion(dailyStats: List<Map<String, Any>>): Double {
        return calculateCompletionRate(dailyStats)
    }
    
    private fun calculateCompletionRate(dailyStats: List<Map<String, Any>>): Double {
        val totalCompleted = dailyStats.sumOf { 
            (it["completed"] as? Number)?.toLong() ?: 0L 
        }
        val totalTodos = dailyStats.sumOf { 
            (it["total"] as? Number)?.toLong() ?: 0L 
        }
        
        return if (totalTodos > 0) {
            (totalCompleted.toDouble() / totalTodos * 100)
        } else 0.0
    }
    
    private fun findMostProductiveDay(dailyStats: List<Map<String, Any>>): Map<String, Any>? {
        return dailyStats.maxByOrNull { 
            (it["completed"] as? Number)?.toLong() ?: 0L 
        }
    }
    
    private fun calculateHeatmapLevel(completed: Long, total: Long): Int {
        if (total == 0L) return 0
        
        val completionRate = completed.toDouble() / total
        
        return when {
            completionRate >= 1.0 -> 4
            completionRate >= 0.75 -> 3
            completionRate >= 0.5 -> 2
            completionRate >= 0.25 -> 1
            else -> 0
        }
    }
    
    private fun findMostProductiveHours(hourlyDistribution: Map<Int, Long>): List<Int> {
        if (hourlyDistribution.isEmpty()) return emptyList()
        
        val maxCount = hourlyDistribution.values.maxOrNull() ?: 0
        return hourlyDistribution.filter { it.value == maxCount }.keys.toList()
    }
    
    private fun generateProductivityRecommendations(
        hourlyDistribution: Map<Int, Long>,
        categoryStats: List<Map<String, Any>>,
        pomodoroStats: Map<String, Any>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Hour-based recommendations
        val productiveHours = findMostProductiveHours(hourlyDistribution)
        if (productiveHours.isNotEmpty()) {
            recommendations.add("Your most productive hours are around ${productiveHours.joinToString(", ")}:00. Try to schedule important tasks during these times.")
        }
        
        // Category-based recommendations
        val lowCompletionCategories = categoryStats.filter { 
            val completionRate = when (val rate = it["completionRate"]) {
                is Number -> rate.toDouble()
                else -> 0.0
            }
            val totalTodos = when (val total = it["totalTodos"]) {
                is Number -> total.toLong()
                else -> 0L
            }
            completionRate < 50.0 && totalTodos > 0 
        }
        if (lowCompletionCategories.isNotEmpty()) {
            val categoryNames = lowCompletionCategories.mapNotNull { it["name"] as? String }
            if (categoryNames.isNotEmpty()) {
                recommendations.add("Consider focusing more on: ${categoryNames.joinToString(", ")}")
            }
        }
        
        // Pomodoro-based recommendations
        val avgSessionsPerDay = when (val avg = pomodoroStats["averageSessionsPerDay"]) {
            is Number -> avg.toDouble()
            is String -> avg.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        if (avgSessionsPerDay < 4.0) {
            recommendations.add("Try to increase your daily Pomodoro sessions to improve focus and productivity.")
        }
        
        return recommendations
    }
}