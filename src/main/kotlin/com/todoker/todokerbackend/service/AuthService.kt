package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.user.RefreshToken
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.response.LoginResponse
import com.todoker.todokerbackend.dto.response.UserResponse
import com.todoker.todokerbackend.exception.AuthException
import com.todoker.todokerbackend.exception.ErrorCode
import com.todoker.todokerbackend.repository.RefreshTokenRepository
import com.todoker.todokerbackend.security.jwt.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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
    private val authenticationManager: AuthenticationManager,
    private val redisTemplate: RedisTemplate<String, String>
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
            throw AuthException.invalidCredentials()
        } catch (e: Exception) {
            logger.error("Login error for user: $username", e)
            throw AuthException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "로그인 중 오류가 발생했습니다"
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
        logger.debug("Refresh token request received. Token: ${refreshTokenValue.takeLast(10)}...")
        
        // Refresh Token 유효성 검증
        val isValid = jwtTokenProvider.validateRefreshToken(refreshTokenValue)
        logger.debug("Refresh token validation result: $isValid")
        
        if (!isValid) {
            logger.warn("Refresh token validation failed. Token: ${refreshTokenValue.takeLast(10)}...")
            throw AuthException(ErrorCode.INVALID_REFRESH_TOKEN)
        }
        
        // 데이터베이스에서 Refresh Token 조회
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow { AuthException.refreshTokenNotFound() }
        
        // Refresh Token 만료 확인
        if (refreshToken.isExpired()) {
            // 만료된 토큰 삭제
            refreshTokenRepository.delete(refreshToken)
            throw AuthException.refreshTokenExpired()
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
            ?: throw AuthException.unauthorized()
        
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
        val token = jwtTokenProvider.createRefreshToken(user.getUsername())
        logger.debug("Created refresh token for user: ${user.getUsername()}, token: ${token.takeLast(10)}...")
        return token
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
    
    /**
     * 패스워드 재설정 요청
     * 이메일로 재설정 토큰을 발송 (실제 이메일 발송은 추후 구현)
     */
    @Transactional
    fun requestPasswordReset(email: String): String {
        logger.info("Password reset requested for email: $email")
        
        // 이메일로 사용자 조회
        val user = try {
            userService.findByEmail(email)
        } catch (e: NoSuchElementException) {
            // 보안상 이유로 사용자가 존재하지 않아도 성공한 것처럼 응답
            logger.warn("Password reset requested for non-existent email: $email")
            return "재설정 링크가 이메일로 발송되었습니다."
        }
        
        // 6자리 랜덤 토큰 생성
        val resetToken = generateResetToken()
        val redisKey = "password_reset:$resetToken"
        
        // Redis에 토큰과 사용자 이메일 저장 (15분 만료)
        redisTemplate.opsForValue().set(redisKey, email, 15, TimeUnit.MINUTES)
        
        logger.info("Password reset token generated for user: ${user.getUsername()}, token: $resetToken")
        
        // TODO: 실제 이메일 발송 로직 구현
        // emailService.sendPasswordResetEmail(email, resetToken)
        
        return "재설정 링크가 이메일로 발송되었습니다."
    }
    
    /**
     * 패스워드 재설정 확인 및 새 패스워드 설정
     */
    @Transactional
    fun confirmPasswordReset(token: String, newPassword: String): String {
        logger.info("Password reset confirmation for token: $token")
        
        val redisKey = "password_reset:$token"
        val email = redisTemplate.opsForValue().get(redisKey)
            ?: throw AuthException(ErrorCode.INVALID_REFRESH_TOKEN, "유효하지 않거나 만료된 재설정 토큰입니다.")
        
        // 토큰 사용 후 즉시 삭제 (일회성)
        redisTemplate.delete(redisKey)
        
        // 이메일로 사용자 조회
        val user = userService.findByEmail(email)
        
        // 새 패스워드로 업데이트 (UserService에서 암호화 처리)
        userService.updatePassword(user.id!!, newPassword)
        
        // 보안을 위해 모든 세션 무효화
        invalidateAllUserSessions(user)
        
        logger.info("Password reset successful for user: ${user.getUsername()}")
        return "패스워드가 성공적으로 변경되었습니다."
    }
    
    /**
     * 재설정 토큰 유효성 확인
     */
    fun validateResetToken(token: String): Boolean {
        val redisKey = "password_reset:$token"
        return redisTemplate.hasKey(redisKey)
    }
    
    /**
     * 아이디 찾기 (이메일 기반)
     * 보안상 부분적으로 마스킹된 아이디를 반환
     */
    fun findUsername(email: String): Map<String, Any> {
        logger.info("Username lookup requested for email: $email")
        
        val user = try {
            userService.findByEmail(email)
        } catch (e: NoSuchElementException) {
            // 보안상 이유로 사용자가 존재하지 않아도 성공한 것처럼 응답
            logger.warn("Username lookup requested for non-existent email: $email")
            return mapOf(
                "found" to false,
                "message" to "해당 이메일로 등록된 계정을 찾을 수 없습니다."
            )
        }
        
        // 아이디 마스킹 처리 (예: john_doe -> j***_**e)
        val maskedUsername = maskUsername(user.getUsername())
        
        logger.info("Username found for email: $email, masked username: $maskedUsername")
        
        return mapOf<String, Any>(
            "found" to true,
            "username" to maskedUsername,
            "email" to email,
            "message" to "아이디를 찾았습니다.",
            "registrationDate" to user.createdAt.toLocalDate().toString()
        )
    }
    
    /**
     * 아이디 마스킹 처리
     * 예: john_doe -> j***_**e, admin -> a***n
     */
    private fun maskUsername(username: String): String {
        return when {
            username.length <= 2 -> username.first() + "*".repeat(username.length - 1)
            username.length <= 4 -> username.first() + "*".repeat(username.length - 2) + username.last()
            else -> {
                val firstChar = username.first()
                val lastChar = username.last()
                val middleLength = username.length - 2
                
                // 특수문자 위치 보존 (언더스코어, 하이픈 등)
                val masked = StringBuilder()
                masked.append(firstChar)
                
                for (i in 1 until username.length - 1) {
                    when (username[i]) {
                        '_', '-', '.', '@' -> masked.append(username[i])
                        else -> masked.append('*')
                    }
                }
                
                masked.append(lastChar)
                masked.toString()
            }
        }
    }
    
    /**
     * 6자리 랜덤 숫자 토큰 생성
     */
    private fun generateResetToken(): String {
        val random = SecureRandom()
        return String.format("%06d", random.nextInt(1000000))
    }
}