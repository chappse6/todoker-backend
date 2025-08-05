package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserRole
import com.todoker.todokerbackend.exception.AuthException
import com.todoker.todokerbackend.repository.RefreshTokenRepository
import com.todoker.todokerbackend.security.jwt.JwtTokenProvider
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime
import java.util.*

/**
 * AuthService 단위 테스트
 * 인증 관련 비즈니스 로직을 테스트
 */
@DisplayName("AuthService 테스트")
class AuthServiceTest {
    
    private val userService = mockk<UserService>()
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val authenticationManager = mockk<AuthenticationManager>()
    private val redisTemplate = mockk<RedisTemplate<String, String>>()
    
    private val authService = AuthService(
        userService,
        jwtTokenProvider,
        refreshTokenRepository,
        authenticationManager,
        redisTemplate
    )
    
    private lateinit var testUser: User
    private lateinit var testRefreshToken: com.todoker.todokerbackend.domain.user.RefreshToken
    
    @BeforeEach
    fun setUp() {
        testUser = User(
            usernameValue = "testuser",
            email = "test@example.com",
            password = "password123!",
            nickname = "테스트유저",
            role = UserRole.USER
        ).apply { 
            // Use reflection to set protected id field for testing
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        
        testRefreshToken = com.todoker.todokerbackend.domain.user.RefreshToken(
            user = testUser,
            token = "refresh-token-value",
            expiresAt = LocalDateTime.now().plusDays(7)
        ).apply { 
            // Use reflection to set protected id field for testing
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
    }
    
    @Nested
    @DisplayName("로그인")
    inner class Login {
        
        @Test
        @DisplayName("정상 로그인 - 성공")
        fun `should login successfully with valid credentials`() {
            // given
            val username = "testuser"
            val password = "password123!"
            val accessToken = "access-token"
            val refreshToken = "refresh-token"
            
            val authentication = mockk<Authentication>()
            every { authentication.principal } returns testUser
            
            every { authenticationManager.authenticate(any()) } returns authentication
            every { jwtTokenProvider.createAccessToken(testUser.getUsername(), any()) } returns accessToken
            every { jwtTokenProvider.createRefreshToken(testUser.getUsername()) } returns refreshToken
            every { jwtTokenProvider.getExpirationAsLocalDateTime(refreshToken) } returns LocalDateTime.now().plusDays(7)
            every { refreshTokenRepository.findByUser(testUser) } returns Optional.empty()
            every { refreshTokenRepository.save(any()) } returnsArgument 0
            
            // when
            val result = authService.login(username, password)
            
            // then
            assertThat(result.accessToken).isEqualTo(accessToken)
            assertThat(result.refreshToken).isEqualTo(refreshToken)
            assertThat(result.user.username).isEqualTo(username)
            
            verify { authenticationManager.authenticate(any()) }
            verify { refreshTokenRepository.save(any()) }
        }
        
        @Test
        @DisplayName("잘못된 인증 정보로 로그인 - 실패")
        fun `should throw exception for invalid credentials`() {
            // given
            val username = "testuser"
            val password = "wrongpassword"
            
            every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")
            
            // when & then
            val exception = assertThrows<AuthException> {
                authService.login(username, password)
            }
            
            assertThat(exception.errorCode.code).isEqualTo("A002")
            assertThat(exception.message).contains("아이디 또는 비밀번호가 올바르지 않습니다")
        }
    }
    
    @Nested
    @DisplayName("회원가입")
    inner class Register {
        
        @Test
        @DisplayName("정상 회원가입 - 성공")
        fun `should register user successfully`() {
            // given
            val username = "newuser"
            val email = "new@example.com"
            val password = "password123!"
            val nickname = "새유저"
            
            every { userService.createUser(username, email, password, nickname) } returns testUser
            
            // when
            val result = authService.register(username, email, password, nickname)
            
            // then
            assertThat(result.username).isEqualTo(testUser.getUsername())
            assertThat(result.email).isEqualTo(testUser.email)
            
            verify { userService.createUser(username, email, password, nickname) }
        }
    }
    
    @Nested
    @DisplayName("로그아웃")
    inner class Logout {
        
        @Test
        @DisplayName("정상 로그아웃 - 성공")
        fun `should logout successfully`() {
            // given
            every { refreshTokenRepository.deleteByUser(testUser) } returns 1
            
            // when
            authService.logout(testUser)
            
            // then
            verify { refreshTokenRepository.deleteByUser(testUser) }
        }
    }
    
    @Nested
    @DisplayName("토큰 갱신")
    inner class RefreshToken {
        
        @Test
        @DisplayName("정상 토큰 갱신 - 성공")
        fun `should refresh access token successfully`() {
            // given
            val refreshTokenValue = "valid-refresh-token"
            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            
            every { jwtTokenProvider.validateRefreshToken(refreshTokenValue) } returns true
            every { refreshTokenRepository.findByToken(refreshTokenValue) } returns Optional.of(testRefreshToken)
            every { jwtTokenProvider.createAccessToken(testUser.getUsername(), any()) } returns newAccessToken
            every { jwtTokenProvider.createRefreshToken(testUser.getUsername()) } returns newRefreshToken
            every { jwtTokenProvider.getExpirationAsLocalDateTime(newRefreshToken) } returns LocalDateTime.now().plusDays(7)
            every { refreshTokenRepository.save(any()) } returnsArgument 0
            
            // when
            val result = authService.refreshAccessToken(refreshTokenValue)
            
            // then
            assertThat(result.accessToken).isEqualTo(newAccessToken)
            assertThat(result.refreshToken).isEqualTo(newRefreshToken)
            assertThat(result.user.username).isEqualTo(testUser.getUsername())
            
            verify { jwtTokenProvider.validateRefreshToken(refreshTokenValue) }
            verify { refreshTokenRepository.save(any()) }
        }
        
        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 - 실패")
        fun `should throw exception for invalid refresh token`() {
            // given
            val invalidRefreshToken = "invalid-refresh-token"
            
            every { jwtTokenProvider.validateRefreshToken(invalidRefreshToken) } returns false
            
            // when & then
            val exception = assertThrows<AuthException> {
                authService.refreshAccessToken(invalidRefreshToken)
            }
            
            assertThat(exception.errorCode.code).isEqualTo("A004")
        }
        
        @Test
        @DisplayName("데이터베이스에 없는 Refresh Token으로 갱신 - 실패")
        fun `should throw exception for non-existent refresh token`() {
            // given
            val refreshTokenValue = "non-existent-token"
            
            every { jwtTokenProvider.validateRefreshToken(refreshTokenValue) } returns true
            every { refreshTokenRepository.findByToken(refreshTokenValue) } returns Optional.empty()
            
            // when & then
            val exception = assertThrows<AuthException> {
                authService.refreshAccessToken(refreshTokenValue)
            }
            
            assertThat(exception.errorCode.code).isEqualTo("A005")
        }
        
        @Test
        @DisplayName("만료된 Refresh Token으로 갱신 - 실패")
        fun `should throw exception for expired refresh token`() {
            // given
            val refreshTokenValue = "expired-refresh-token"
            val expiredToken = com.todoker.todokerbackend.domain.user.RefreshToken(
                user = testUser,
                token = refreshTokenValue,
                expiresAt = LocalDateTime.now().minusDays(1) // 이미 만료됨
            ).apply { 
                // Use reflection to set protected id field for testing
                val idField = this::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }
            
            every { jwtTokenProvider.validateRefreshToken(refreshTokenValue) } returns true
            every { refreshTokenRepository.findByToken(refreshTokenValue) } returns Optional.of(expiredToken)
            every { refreshTokenRepository.delete(expiredToken) } just runs
            
            // when & then
            val exception = assertThrows<AuthException> {
                authService.refreshAccessToken(refreshTokenValue)
            }
            
            assertThat(exception.errorCode.code).isEqualTo("A006")
            verify { refreshTokenRepository.delete(expiredToken) }
        }
    }
    
    @Nested
    @DisplayName("세션 관리")
    inner class SessionManagement {
        
        @Test
        @DisplayName("모든 사용자 세션 무효화 - 성공")
        fun `should invalidate all user sessions successfully`() {
            // given
            every { refreshTokenRepository.deleteByUser(testUser) } returns 2
            
            // when
            authService.invalidateAllUserSessions(testUser)
            
            // then
            verify { refreshTokenRepository.deleteByUser(testUser) }
        }
        
        @Test
        @DisplayName("만료된 토큰 정리 - 성공")
        fun `should cleanup expired tokens successfully`() {
            // given
            every { refreshTokenRepository.deleteExpiredTokens(any()) } returns 5
            
            // when
            authService.cleanupExpiredTokens()
            
            // then
            verify { refreshTokenRepository.deleteExpiredTokens(any()) }
        }
    }
}