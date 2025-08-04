package com.todoker.todokerbackend.controller

import com.todoker.todokerbackend.common.response.ApiResponse
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.request.LoginRequest
import com.todoker.todokerbackend.dto.request.SignUpRequest
import com.todoker.todokerbackend.dto.response.LoginResponse
import com.todoker.todokerbackend.dto.response.UserResponse
import com.todoker.todokerbackend.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 인증 관련 API 컨트롤러
 * 로그인, 회원가입, 토큰 갱신, 로그아웃 등의 인증 기능을 제공
 */
@Tag(name = "Authentication", description = "인증 관리 API")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 사용자 로그인
     * 사용자명과 비밀번호를 검증하고 JWT 토큰을 발급
     */
    @Operation(summary = "로그인", description = "사용자 로그인을 수행하고 JWT 토큰을 발급합니다.")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<LoginResponse> {
        logger.info("Login attempt for user: ${request.username}")
        
        val loginResponse = authService.login(request.username, request.password)
        
        logger.info("Login successful for user: ${request.username}")
        return ApiResponse.success(loginResponse)
    }
    
    /**
     * 사용자 회원가입
     * 새로운 사용자 계정을 생성
     */
    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: SignUpRequest): ApiResponse<UserResponse> {
        logger.info("Registration attempt for user: ${request.username}")
        
        val userResponse = authService.register(
            username = request.username,
            email = request.email,
            password = request.password,
            nickname = request.nickname
        )
        
        logger.info("Registration successful for user: ${request.username}")
        return ApiResponse.success(userResponse)
    }
    
    /**
     * Access Token 갱신
     * Refresh Token을 사용하여 새로운 Access Token을 발급
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ApiResponse<LoginResponse> {
        logger.debug("Token refresh attempt with refresh token")
        
        val loginResponse = authService.refreshAccessToken(request.refreshToken)
        
        logger.debug("Token refresh successful")
        return ApiResponse.success(loginResponse)
    }
    
    /**
     * 로그아웃
     * 현재 사용자의 Refresh Token을 무효화
     */
    @Operation(summary = "로그아웃", description = "현재 사용자를 로그아웃하고 토큰을 무효화합니다.")
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal user: User): ApiResponse<Nothing> {
        logger.info("Logout request for user: ${user.getUsername()}")
        
        authService.logout(user)
        
        logger.info("Logout successful for user: ${user.getUsername()}")
        return ApiResponse.success()
    }
    
    /**
     * 현재 인증된 사용자 정보 조회
     */
    @Operation(summary = "사용자 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal user: User): ApiResponse<UserResponse> {
        logger.debug("Getting current user info for: ${user.getUsername()}")
        
        val userResponse = UserResponse.from(user)
        return ApiResponse.success(userResponse)
    }
    
    /**
     * 모든 기기에서 로그아웃
     * 사용자의 모든 Refresh Token을 무효화하여 모든 기기에서 로그아웃
     */
    @Operation(summary = "모든 기기에서 로그아웃", description = "사용자의 모든 세션을 무효화하여 모든 기기에서 로그아웃합니다.")
    @PostMapping("/logout-all")
    fun logoutAll(@AuthenticationPrincipal user: User): ApiResponse<Nothing> {
        logger.info("Logout all devices request for user: ${user.getUsername()}")
        
        authService.invalidateAllUserSessions(user)
        
        logger.info("All sessions invalidated for user: ${user.getUsername()}")
        return ApiResponse.success()
    }
    
    /**
     * 토큰 유효성 검증
     * 현재 토큰이 유효한지 확인 (실제로는 이 엔드포인트에 접근할 수 있다면 토큰이 유효함)
     */
    @Operation(summary = "토큰 검증", description = "현재 토큰의 유효성을 검증합니다.")
    @GetMapping("/validate")
    fun validateToken(@AuthenticationPrincipal user: User): ApiResponse<Map<String, Any>> {
        logger.debug("Token validation for user: ${user.getUsername()}")
        
        val response = mapOf(
            "valid" to true,
            "username" to user.getUsername(),
            "roles" to user.authorities.map { it.authority }
        )
        
        return ApiResponse.success(response)
    }
    
    /**
     * 클라이언트 IP 및 User-Agent 정보 조회 (보안 로그용)
     */
    private fun getClientInfo(request: HttpServletRequest): String {
        val clientIp = getClientIpAddress(request)
        val userAgent = request.getHeader("User-Agent") ?: "Unknown"
        return "IP: $clientIp, User-Agent: $userAgent"
    }
    
    /**
     * 클라이언트 실제 IP 주소 추출
     * 프록시나 로드밸런서를 거친 경우에도 실제 IP를 가져옴
     */
    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        val xForwardedProto = request.getHeader("X-Forwarded-Proto")
        
        return when {
            xForwardedFor?.isNotBlank() == true -> xForwardedFor.split(",")[0].trim()
            xRealIp?.isNotBlank() == true -> xRealIp
            else -> request.remoteAddr
        }
    }
}

/**
 * Refresh Token 요청 DTO
 */
data class RefreshTokenRequest(
    val refreshToken: String
)