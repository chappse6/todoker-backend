package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.user.RefreshToken
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.response.LoginResponse
import com.todoker.todokerbackend.dto.response.UserResponse
import com.todoker.todokerbackend.exception.BusinessException
import com.todoker.todokerbackend.repository.RefreshTokenRepository
import com.todoker.todokerbackend.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 * 로그인, 로그아웃, 토큰 갱신 등의 인증 기능을 제공
 */
@Service
@Transactional(readOnly = true)
class AuthService(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authenticationManager: AuthenticationManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 사용자 로그인
     * 사용자명과 비밀번호를 검증하고 JWT 토큰을 발급
     */
    @Transactional
    fun login(username: String, password: String): LoginResponse {
        try {
            // Spring Security를 통한 인증 수행
            val authToken = UsernamePasswordAuthenticationToken(username, password)
            val authentication = authenticationManager.authenticate(authToken)
            
            val user = authentication.principal as User
            logger.info("User login successful: ${user.getUsername()}")
            
            // JWT 토큰 생성
            val accessToken = createAccessToken(user)
            val refreshToken = createRefreshToken(user)
            
            // Refresh Token을 데이터베이스에 저장
            storeRefreshToken(user, refreshToken)
            
            return LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = UserResponse.from(user)
            )
            
        } catch (e: BadCredentialsException) {
            logger.warn("Login failed for user: $username - Invalid credentials")
            throw BusinessException(
                "INVALID_CREDENTIALS",
                "아이디 또는 비밀번호가 올바르지 않습니다",
                HttpStatus.UNAUTHORIZED
            )
        } catch (e: Exception) {
            logger.error("Login error for user: $username", e)
            throw BusinessException(
                "LOGIN_ERROR",
                "로그인 중 오류가 발생했습니다",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
    
    /**
     * 사용자 회원가입
     */
    @Transactional
    fun register(username: String, email: String, password: String, nickname: String?): UserResponse {
        logger.info("User registration attempt: $username")
        
        val user = userService.createUser(username, email, password, nickname)
        logger.info("User registration successful: ${user.getUsername()}")
        
        return UserResponse.from(user)
    }
    
    /**
     * 로그아웃
     * 데이터베이스에서 Refresh Token 제거
     */
    @Transactional
    fun logout(user: User) {
        logger.info("User logout: ${user.getUsername()}")
        
        // 사용자의 모든 Refresh Token 삭제
        val deletedCount = refreshTokenRepository.deleteByUser(user)
        logger.info("Deleted $deletedCount refresh tokens for user: ${user.getUsername()}")
        
        // Security Context 클리어
        SecurityContextHolder.clearContext()
    }
    
    /**
     * Refresh Token을 사용하여 새로운 Access Token 발급
     */
    @Transactional
    fun refreshAccessToken(refreshTokenValue: String): LoginResponse {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenValue)) {
            throw BusinessException(
                "INVALID_REFRESH_TOKEN",
                "유효하지 않은 리프레시 토큰입니다",
                HttpStatus.UNAUTHORIZED
            )
        }
        
        // 데이터베이스에서 Refresh Token 조회
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow {
                BusinessException(
                    "REFRESH_TOKEN_NOT_FOUND",
                    "리프레시 토큰을 찾을 수 없습니다",
                    HttpStatus.UNAUTHORIZED
                )
            }
        
        // Refresh Token 만료 확인
        if (refreshToken.isExpired()) {
            // 만료된 토큰 삭제
            refreshTokenRepository.delete(refreshToken)
            throw BusinessException(
                "REFRESH_TOKEN_EXPIRED",
                "리프레시 토큰이 만료되었습니다",
                HttpStatus.UNAUTHORIZED
            )
        }
        
        val user = refreshToken.user
        logger.info("Refreshing access token for user: ${user.getUsername()}")
        
        // 새로운 토큰 발급
        val newAccessToken = createAccessToken(user)
        val newRefreshToken = createRefreshToken(user)
        
        // 기존 Refresh Token 업데이트
        refreshToken.updateToken(
            newRefreshToken,
            jwtTokenProvider.getExpirationAsLocalDateTime(newRefreshToken)
        )
        refreshTokenRepository.save(refreshToken)
        
        return LoginResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            user = UserResponse.from(user)
        )
    }
    
    /**
     * 현재 인증된 사용자 정보 조회
     */
    fun getCurrentUser(): UserResponse {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw BusinessException(
                "NOT_AUTHENTICATED",
                "인증되지 않은 사용자입니다",
                HttpStatus.UNAUTHORIZED
            )
        
        val user = authentication.principal as User
        return UserResponse.from(user)
    }
    
    /**
     * Access Token 생성
     */
    private fun createAccessToken(user: User): String {
        val roles = user.authorities.map { it.authority }
        return jwtTokenProvider.createAccessToken(user.getUsername(), roles)
    }
    
    /**
     * Refresh Token 생성
     */
    private fun createRefreshToken(user: User): String {
        return jwtTokenProvider.createRefreshToken(user.getUsername())
    }
    
    /**
     * Refresh Token을 데이터베이스에 저장
     * 기존 토큰이 있으면 업데이트, 없으면 새로 생성
     */
    private fun storeRefreshToken(user: User, tokenValue: String) {
        val expirationTime = jwtTokenProvider.getExpirationAsLocalDateTime(tokenValue)
        
        // 기존 Refresh Token 조회
        val existingToken = refreshTokenRepository.findByUser(user)
        
        if (existingToken.isPresent) {
            // 기존 토큰 업데이트
            existingToken.get().updateToken(tokenValue, expirationTime)
            refreshTokenRepository.save(existingToken.get())
        } else {
            // 새로운 토큰 생성
            val refreshToken = RefreshToken(
                user = user,
                token = tokenValue,
                expiresAt = expirationTime
            )
            refreshTokenRepository.save(refreshToken)
        }
    }
    
    /**
     * 만료된 Refresh Token 정리 (스케줄링으로 주기적 실행)
     */
    @Transactional
    fun cleanupExpiredTokens() {
        val deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now())
        if (deletedCount > 0) {
            logger.info("Cleaned up $deletedCount expired refresh tokens")
        }
    }
    
    /**
     * 특정 사용자의 모든 세션 무효화 (모든 기기에서 로그아웃)
     */
    @Transactional
    fun invalidateAllUserSessions(user: User) {
        logger.info("Invalidating all sessions for user: ${user.getUsername()}")
        val deletedCount = refreshTokenRepository.deleteByUser(user)
        logger.info("Invalidated $deletedCount sessions for user: ${user.getUsername()}")
    }
}