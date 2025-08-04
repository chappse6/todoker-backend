package com.todoker.todokerbackend.config

import com.todoker.todokerbackend.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

/**
 * 스케줄링 설정
 * 정기적으로 실행되어야 하는 작업들을 관리
 */
@Configuration
@EnableScheduling
class SchedulingConfig(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 만료된 Refresh Token 정리 작업
     * 매일 새벽 2시에 실행하여 만료된 토큰들을 데이터베이스에서 제거
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 오전 2시
    fun cleanupExpiredRefreshTokens() {
        logger.info("Starting cleanup of expired refresh tokens")
        
        try {
            authService.cleanupExpiredTokens()
            logger.info("Completed cleanup of expired refresh tokens")
        } catch (e: Exception) {
            logger.error("Failed to cleanup expired refresh tokens", e)
        }
    }
    
    /**
     * 시스템 상태 체크 (선택사항)
     * 매 30분마다 실행하여 시스템 상태를 로깅
     */
    @Scheduled(fixedRate = 1800000) // 30분마다
    fun systemHealthCheck() {
        logger.debug("System health check - Application is running")
    }
}