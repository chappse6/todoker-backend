package com.todoker.todokerbackend.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.todoker.todokerbackend.common.response.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * JWT 토큰 인증 필터
 * HTTP 요청에서 JWT 토큰을 추출하고 검증하여 Spring Security Context에 인증 정보를 설정
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
    
    /**
     * 요청마다 실행되는 필터 로직
     * Authorization 헤더에서 JWT 토큰을 추출하고 검증
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 요청에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)
            
            if (token != null) {
                // 토큰 유효성 검증
                if (jwtTokenProvider.validateToken(token)) {
                    // 유효한 토큰인 경우 인증 정보를 Spring Security Context에 설정
                    val authentication = jwtTokenProvider.getAuthentication(token)
                    SecurityContextHolder.getContext().authentication = authentication
                    
                    logger.debug("JWT authentication successful for user: ${authentication.name}")
                } else {
                    logger.debug("Invalid JWT token")
                    handleInvalidToken(response, "Invalid or expired token")
                    return
                }
            }
            
            // 다음 필터로 요청 전달
            filterChain.doFilter(request, response)
            
        } catch (e: Exception) {
            logger.error("JWT authentication error", e)
            handleAuthenticationError(response, e.message ?: "Authentication failed")
        }
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     * Authorization: Bearer <token> 형식에서 토큰 부분만 추출
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
    
    /**
     * 유효하지 않은 토큰에 대한 응답 처리
     */
    private fun handleInvalidToken(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        
        val errorResponse = ApiResponse.error<Nothing>(
            "INVALID_TOKEN",
            message
        )
        
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
    
    /**
     * 인증 처리 중 발생한 예외에 대한 응답 처리
     */
    private fun handleAuthenticationError(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        
        val errorResponse = ApiResponse.error<Nothing>(
            "AUTHENTICATION_ERROR",
            message
        )
        
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
    
    /**
     * 필터를 적용하지 않을 요청 경로 확인
     * 인증이 필요 없는 공개 엔드포인트는 필터를 건너뜀
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val method = request.method
        
        // 공개 엔드포인트 목록
        val publicPaths = listOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/health",
            "/api/actuator",
            "/api/swagger-ui",
            "/api/v3/api-docs"
        )
        
        // OPTIONS 요청은 CORS preflight이므로 인증 불필요
        if (method == "OPTIONS") {
            return true
        }
        
        // 공개 경로인지 확인
        return publicPaths.any { publicPath ->
            path.startsWith(publicPath)
        }
    }
}