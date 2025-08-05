package com.todoker.todokerbackend.security.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성 및 검증을 담당하는 컴포넌트
 * Access Token과 Refresh Token을 생성하고 검증하는 기능을 제공
 */
@Component
class JwtTokenProvider(
    @Lazy private val userDetailsService: UserDetailsService,
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.access-token-validity:1800000}") private val accessTokenValidityMs: Long,
    @Value("\${jwt.refresh-token-validity:604800000}") private val refreshTokenValidityMs: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // JWT 서명을 위한 비밀키 생성
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
    }
    
    /**
     * Access Token 생성
     * 사용자 인증 정보를 담은 단기간 유효한 토큰
     */
    fun createAccessToken(username: String, roles: List<String>): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenValidityMs)
        
        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }
    
    /**
     * Refresh Token 생성
     * Access Token 재발급을 위한 장기간 유효한 토큰
     */
    fun createRefreshToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenValidityMs)
        
        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }
    
    /**
     * 토큰에서 사용자명 추출
     */
    fun getUsernameFromToken(token: String): String {
        return getClaimsFromToken(token).subject
    }
    
    /**
     * 토큰에서 만료 시간 추출
     */
    fun getExpirationFromToken(token: String): Date {
        return getClaimsFromToken(token).expiration
    }
    
    /**
     * 토큰에서 역할 정보 추출
     */
    fun getRolesFromToken(token: String): List<String> {
        val claims = getClaimsFromToken(token)
        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }
    
    /**
     * 토큰 타입 확인 (access/refresh)
     */
    fun getTokenType(token: String): String {
        return getClaimsFromToken(token)["type"] as? String ?: "unknown"
    }
    
    /**
     * 토큰에서 Claims 추출
     */
    private fun getClaimsFromToken(token: String): Claims {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            logger.error("Failed to parse JWT token", e)
            throw JwtException("Invalid JWT token")
        }
    }
    
    /**
     * 토큰 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            val now = Date()
            
            // 만료 시간 확인
            if (claims.expiration.before(now)) {
                logger.debug("JWT token is expired")
                return false
            }
            
            // 토큰 타입 확인 (Access Token만 인증에 사용)
            val tokenType = claims["type"] as? String
            if (tokenType != "access") {
                logger.debug("Invalid token type: $tokenType")
                return false
            }
            
            true
        } catch (e: JwtException) {
            logger.debug("Invalid JWT token: ${e.message}")
            false
        } catch (e: Exception) {
            logger.error("JWT token validation error", e)
            false
        }
    }
    
    /**
     * Refresh Token 유효성 검증
     */
    fun validateRefreshToken(token: String): Boolean {
        return try {
            logger.debug("Validating refresh token: ${token.takeLast(10)}...")
            val claims = getClaimsFromToken(token)
            val now = Date()
            
            logger.debug("Refresh token claims: subject=${claims.subject}, expiration=${claims.expiration}, type=${claims["type"]}")
            
            // 만료 시간 확인
            if (claims.expiration.before(now)) {
                logger.warn("Refresh token is expired: expiration=${claims.expiration}, now=$now")
                return false
            }
            
            // 토큰 타입 확인
            val tokenType = claims["type"] as? String
            if (tokenType != "refresh") {
                logger.warn("Invalid refresh token type: expected=refresh, actual=$tokenType")
                return false
            }
            
            logger.debug("Refresh token validation successful")
            true
        } catch (e: Exception) {
            logger.warn("Invalid refresh token: ${e.message}", e)
            false
        }
    }
    
    /**
     * 토큰으로부터 Authentication 객체 생성
     */
    fun getAuthentication(token: String): Authentication {
        val username = getUsernameFromToken(token)
        val userDetails = userDetailsService.loadUserByUsername(username)
        
        return UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )
    }
    
    /**
     * 토큰 만료까지 남은 시간 (초)
     */
    fun getExpirationTimeInSeconds(token: String): Long {
        val expiration = getExpirationFromToken(token)
        val now = Date()
        return (expiration.time - now.time) / 1000
    }
    
    /**
     * 토큰 만료 시간을 LocalDateTime으로 반환
     */
    fun getExpirationAsLocalDateTime(token: String): LocalDateTime {
        val expiration = getExpirationFromToken(token)
        return expiration.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}