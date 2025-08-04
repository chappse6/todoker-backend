package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.user.RefreshToken
import com.todoker.todokerbackend.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    
    fun findByToken(token: String): Optional<RefreshToken>
    
    fun findByUser(user: User): Optional<RefreshToken>
    
    fun existsByToken(token: String): Boolean
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    fun deleteByUser(@Param("user") user: User): Int
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    fun deleteByToken(@Param("token") token: String): Int
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(@Param("now") now: LocalDateTime): Int
    
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user")
    fun countByUser(@Param("user") user: User): Long
}