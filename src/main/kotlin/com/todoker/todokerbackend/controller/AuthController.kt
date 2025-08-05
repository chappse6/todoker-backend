package com.todoker.todokerbackend.controller

import com.todoker.todokerbackend.common.response.ApiResponse
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.request.FindUsernameRequest
import com.todoker.todokerbackend.dto.request.LoginRequest
import com.todoker.todokerbackend.dto.request.ResetPasswordConfirmRequest
import com.todoker.todokerbackend.dto.request.ResetPasswordRequest
import com.todoker.todokerbackend.dto.request.SignUpRequest
import com.todoker.todokerbackend.dto.response.LoginResponse
import com.todoker.todokerbackend.dto.response.UserResponse
import com.todoker.todokerbackend.service.AuthService
import com.todoker.todokerbackend.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    private val authService: AuthService,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 사용자 로그인
     * 사용자명과 비밀번호를 검증하고 JWT 토큰을 발급
     */
    @Operation(summary = "로그인", description = "사용자 로그인을 수행하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패 - 아이디 또는 비밀번호가 올바르지 않습니다 (A002)", ref = "#/components/responses/Unauthorized"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
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
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "201", description = "회원가입 성공"),
        SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 사용자명 (U003) 또는 이메일 (U004)", ref = "#/components/responses/BadRequest"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
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
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        SwaggerApiResponse(responseCode = "401", description = "리프레시 토큰 만료 (A006) 또는 유효하지 않은 토큰 (A008)", ref = "#/components/responses/Unauthorized"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
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
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
        SwaggerApiResponse(responseCode = "401", description = "인증 필요 (A001)", ref = "#/components/responses/Unauthorized"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
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
     * 아이디 중복 확인
     * 회원가입 시 아이디(사용자명)의 중복 여부를 확인
     */
    @Operation(summary = "아이디 중복 확인", description = "회원가입 시 아이디(사용자명)의 중복 여부를 확인합니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "중복 확인 완료"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 - 아이디가 비어있거나 형식이 올바르지 않음 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
    @GetMapping("/check-username")
    fun checkUsernameAvailability(@RequestParam username: String): ApiResponse<Map<String, Any>> {
        logger.debug("Username availability check for: $username")
        
        // 기본 유효성 검사
        if (username.isBlank()) {
            throw IllegalArgumentException("아이디는 비어있을 수 없습니다.")
        }
        
        val isAvailable = !userService.existsByUsername(username)
        val response = mapOf(
            "username" to username,
            "available" to isAvailable,
            "message" to if (isAvailable) "사용 가능한 아이디입니다." else "이미 사용 중인 아이디입니다."
        )
        
        logger.debug("Username availability check result for '$username': available=$isAvailable")
        return ApiResponse.success(response)
    }
    
    /**
     * 패스워드 재설정 요청
     * 이메일 주소로 패스워드 재설정 토큰을 발송
     */
    @Operation(summary = "패스워드 재설정 요청", description = "등록된 이메일 주소로 패스워드 재설정 토큰을 발송합니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "재설정 이메일 발송 완료"),
        SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
    @PostMapping("/reset-password")
    fun requestPasswordReset(@Valid @RequestBody request: ResetPasswordRequest): ApiResponse<Map<String, String>> {
        logger.info("Password reset request for email: ${request.email}")
        
        val message = authService.requestPasswordReset(request.email)
        val response = mapOf("message" to message)
        
        logger.info("Password reset request processed for email: ${request.email}")
        return ApiResponse.success(response)
    }
    
    /**
     * 패스워드 재설정 확인
     * 토큰을 사용하여 새로운 패스워드로 변경
     */
    @Operation(summary = "패스워드 재설정 확인", description = "재설정 토큰을 사용하여 새로운 패스워드로 변경합니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "패스워드 변경 완료"),
        SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패 (V001) 또는 유효하지 않은/만료된 토큰 (A008)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
    @PostMapping("/reset-password/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: ResetPasswordConfirmRequest): ApiResponse<Map<String, String>> {
        logger.info("Password reset confirmation for token: ${request.token}")
        
        val message = authService.confirmPasswordReset(request.token, request.newPassword)
        val response = mapOf("message" to message)
        
        logger.info("Password reset confirmation successful for token: ${request.token}")
        return ApiResponse.success(response)
    }
    
    /**
     * 재설정 토큰 유효성 확인
     * 토큰이 유효한지 확인 (선택적 기능)
     */
    @Operation(summary = "재설정 토큰 검증", description = "패스워드 재설정 토큰의 유효성을 확인합니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "토큰 검증 완료"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 - 토큰이 비어있음 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
    @GetMapping("/reset-password/validate")
    fun validateResetToken(@RequestParam token: String): ApiResponse<Map<String, Any>> {
        logger.debug("Reset token validation for: $token")
        
        if (token.isBlank()) {
            throw IllegalArgumentException("토큰은 비어있을 수 없습니다.")
        }
        
        val isValid = authService.validateResetToken(token)
        val response = mapOf(
            "token" to token,
            "valid" to isValid,
            "message" to if (isValid) "유효한 토큰입니다." else "유효하지 않거나 만료된 토큰입니다."
        )
        
        logger.debug("Reset token validation result for '$token': valid=$isValid")
        return ApiResponse.success(response)
    }
    
    /**
     * 아이디 찾기
     * 이메일 주소로 등록된 계정의 아이디를 마스킹하여 반환
     */
    @Operation(summary = "아이디 찾기", description = "등록된 이메일 주소로 계정의 아이디를 찾습니다. 보안을 위해 마스킹 처리됩니다.")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "아이디 찾기 완료 (찾은 경우와 못 찾은 경우 모두)"),
        SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패 (V001)", ref = "#/components/responses/ValidationError"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류 (C006)", ref = "#/components/responses/InternalServerError")
    ])
    @PostMapping("/find-username")
    fun findUsername(@Valid @RequestBody request: FindUsernameRequest): ApiResponse<Map<String, Any>> {
        logger.info("Username lookup request for email: ${request.email}")
        
        val result = authService.findUsername(request.email)
        
        logger.info("Username lookup processed for email: ${request.email}, found: ${result["found"]}")
        return ApiResponse.success(result)
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