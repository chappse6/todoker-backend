package com.todoker.todokerbackend.domain.user

import com.todoker.todokerbackend.domain.common.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Refresh Token 엔티티
 * JWT Refresh Token을 데이터베이스에 저장하여 토큰 무효화 및 관리를 수행
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_token", columnList = "token", unique = true),
        Index(name = "idx_refresh_token_user", columnList = "user_id"),
        Index(name = "idx_refresh_token_expires", columnList = "expiresAt")
    ]
)
class RefreshToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false, length = 500)
    var token: String,
    
    @Column(nullable = false)
    var expiresAt: LocalDateTime
) : BaseEntity() {
    
    /**
     * 토큰이 만료되었는지 확인
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }
    
    /**
     * 토큰 갱신
     */
    fun updateToken(newToken: String, newExpiresAt: LocalDateTime) {
        this.token = newToken
        this.expiresAt = newExpiresAt
    }
    
    /**
     * 만료까지 남은 시간 (초)
     */
    fun getSecondsUntilExpiration(): Long {
        val now = LocalDateTime.now()
        return if (now.isBefore(expiresAt)) {
            java.time.Duration.between(now, expiresAt).seconds
        } else {
            0L
        }
    }
}