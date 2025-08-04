package com.todoker.todokerbackend.security.jwt

import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserRole
import com.todoker.todokerbackend.service.UserService
import io.jsonwebtoken.JwtException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UserDetailsService

/**
 * JwtTokenProvider 단위 테스트
 * JWT 토큰 생성, 검증, 정보 추출 기능을 테스트
 */
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {
    
    private val userDetailsService = mockk<UserDetailsService>()
    private val secretKey = "test-secret-key-for-jwt-token-provider-unit-test-very-long-key"
    private val accessTokenValidityMs = 30000L // 30초
    private val refreshTokenValidityMs = 60000L // 60초
    
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var testUser: User
    
    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(
            userDetailsService = userDetailsService,
            secretKey = secretKey,
            accessTokenValidityMs = accessTokenValidityMs,
            refreshTokenValidityMs = refreshTokenValidityMs
        )
        
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
    }
    
    @Nested
    @DisplayName("토큰 생성")
    inner class TokenCreation {
        
        @Test
        @DisplayName("Access Token 생성 - 성공")
        fun `should create access token successfully`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            
            // when
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // then
            assertThat(token).isNotNull()
            assertThat(token).isNotEmpty()
            assertThat(token.split(".")).hasSize(3) // JWT는 3부분으로 구성
        }
        
        @Test
        @DisplayName("Refresh Token 생성 - 성공")
        fun `should create refresh token successfully`() {
            // given
            val username = "testuser"
            
            // when
            val token = jwtTokenProvider.createRefreshToken(username)
            
            // then
            assertThat(token).isNotNull()
            assertThat(token).isNotEmpty()
            assertThat(token.split(".")).hasSize(3)
        }
    }
    
    @Nested
    @DisplayName("토큰 정보 추출")
    inner class TokenInfoExtraction {
        
        @Test
        @DisplayName("토큰에서 사용자명 추출 - 성공")
        fun `should extract username from token`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val extractedUsername = jwtTokenProvider.getUsernameFromToken(token)
            
            // then
            assertThat(extractedUsername).isEqualTo(username)
        }
        
        @Test
        @DisplayName("토큰에서 역할 정보 추출 - 성공")
        fun `should extract roles from token`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER", "ROLE_ADMIN")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val extractedRoles = jwtTokenProvider.getRolesFromToken(token)
            
            // then
            assertThat(extractedRoles).containsExactlyElementsOf(roles)
        }
        
        @Test
        @DisplayName("토큰 타입 확인 - Access Token")
        fun `should return access token type`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val tokenType = jwtTokenProvider.getTokenType(token)
            
            // then
            assertThat(tokenType).isEqualTo("access")
        }
        
        @Test
        @DisplayName("토큰 타입 확인 - Refresh Token")
        fun `should return refresh token type`() {
            // given
            val username = "testuser"
            val token = jwtTokenProvider.createRefreshToken(username)
            
            // when
            val tokenType = jwtTokenProvider.getTokenType(token)
            
            // then
            assertThat(tokenType).isEqualTo("refresh")
        }
        
        @Test
        @DisplayName("만료 시간 추출 - 성공")
        fun `should extract expiration time from token`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val expiration = jwtTokenProvider.getExpirationFromToken(token)
            val expirationSeconds = jwtTokenProvider.getExpirationTimeInSeconds(token)
            
            // then
            assertThat(expiration).isNotNull()
            assertThat(expirationSeconds).isPositive()
            assertThat(expirationSeconds).isLessThanOrEqualTo(accessTokenValidityMs / 1000)
        }
    }
    
    @Nested
    @DisplayName("토큰 검증")
    inner class TokenValidation {
        
        @Test
        @DisplayName("유효한 Access Token 검증 - 성공")
        fun `should validate valid access token`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val isValid = jwtTokenProvider.validateToken(token)
            
            // then
            assertThat(isValid).isTrue()
        }
        
        @Test
        @DisplayName("유효한 Refresh Token 검증 - 성공")
        fun `should validate valid refresh token`() {
            // given
            val username = "testuser"
            val token = jwtTokenProvider.createRefreshToken(username)
            
            // when
            val isValid = jwtTokenProvider.validateRefreshToken(token)
            
            // then
            assertThat(isValid).isTrue()
        }
        
        @Test
        @DisplayName("잘못된 토큰 검증 - 실패")
        fun `should reject invalid token`() {
            // given
            val invalidToken = "invalid.jwt.token"
            
            // when
            val isValid = jwtTokenProvider.validateToken(invalidToken)
            
            // then
            assertThat(isValid).isFalse()
        }
        
        @Test
        @DisplayName("잘못된 타입의 토큰 검증 - 실패")
        fun `should reject wrong token type for access validation`() {
            // given - Refresh Token을 Access Token 검증에 사용
            val username = "testuser"
            val refreshToken = jwtTokenProvider.createRefreshToken(username)
            
            // when
            val isValid = jwtTokenProvider.validateToken(refreshToken)
            
            // then
            assertThat(isValid).isFalse()
        }
        
        @Test
        @DisplayName("잘못된 타입의 토큰으로 Refresh 검증 - 실패")
        fun `should reject wrong token type for refresh validation`() {
            // given - Access Token을 Refresh Token 검증에 사용
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val accessToken = jwtTokenProvider.createAccessToken(username, roles)
            
            // when
            val isValid = jwtTokenProvider.validateRefreshToken(accessToken)
            
            // then
            assertThat(isValid).isFalse()
        }
    }
    
    @Nested
    @DisplayName("Authentication 객체 생성")
    inner class AuthenticationCreation {
        
        @Test
        @DisplayName("토큰으로부터 Authentication 생성 - 성공")
        fun `should create authentication from token`() {
            // given
            val username = "testuser"
            val roles = listOf("ROLE_USER")
            val token = jwtTokenProvider.createAccessToken(username, roles)
            
            every { userDetailsService.loadUserByUsername(username) } returns testUser
            
            // when
            val authentication = jwtTokenProvider.getAuthentication(token)
            
            // then
            assertThat(authentication).isNotNull()
            assertThat(authentication.principal).isEqualTo(testUser)
            assertThat(authentication.isAuthenticated).isTrue()
        }
    }
    
    @Nested
    @DisplayName("예외 처리")
    inner class ExceptionHandling {
        
        @Test
        @DisplayName("빈 토큰으로 정보 추출 시도 - 예외 발생")
        fun `should throw exception for empty token`() {
            // given
            val emptyToken = ""
            
            // when & then
            assertThrows<JwtException> {
                jwtTokenProvider.getUsernameFromToken(emptyToken)
            }
        }
        
        @Test
        @DisplayName("null 토큰으로 정보 추출 시도 - 예외 발생")
        fun `should throw exception for malformed token`() {
            // given
            val malformedToken = "not.a.valid.jwt.token.format"
            
            // when & then
            assertThrows<JwtException> {
                jwtTokenProvider.getUsernameFromToken(malformedToken)
            }
        }
    }
}