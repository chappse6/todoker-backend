package com.todoker.todokerbackend.config

import com.todoker.todokerbackend.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
 * JWT 기반 인증과 CORS 설정을 포함한 보안 구성
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    
    /**
     * 비밀번호 암호화를 위한 BCrypt 인코더
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    
    /**
     * 인증 매니저 설정
     */
    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }
    
    /**
     * Security Filter Chain 설정
     * HTTP 보안, 인증, 인가 규칙을 정의
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF 비활성화 (JWT 사용으로 불필요)
            .csrf { it.disable() }
            
            // CORS 설정 적용
            .cors { it.configurationSource(corsConfigurationSource()) }
            
            // 세션 사용 안함 (JWT 기반 인증)
            .sessionManagement { 
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            }
            
            // HTTP 요청 인가 규칙
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트 (인증 불필요)
                    .requestMatchers(
                        "/auth/login",
                        "/auth/register", 
                        "/auth/refresh",
                        "/auth/check-username",
                        "/auth/find-username",
                        "/auth/reset-password",
                        "/auth/reset-password/confirm",
                        "/auth/reset-password/validate",
                        "/health",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                    ).permitAll()
                    
                    // OPTIONS 요청 허용 (CORS preflight)
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    
                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            
            // 예외 처리
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { _, response, _ ->
                        response.status = 401
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""
                            {
                                "success": false,
                                "error": {
                                    "code": "UNAUTHORIZED",
                                    "message": "인증이 필요합니다"
                                }
                            }
                        """.trimIndent())
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.status = 403
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""
                            {
                                "success": false,
                                "error": {
                                    "code": "ACCESS_DENIED",
                                    "message": "접근 권한이 없습니다"
                                }
                            }
                        """.trimIndent())
                    }
            }
        
        return http.build()
    }
    
    /**
     * CORS 설정
     * Frontend와의 통신을 위한 CORS 정책 설정
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // 허용할 Origin 설정 (개발 환경)
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:3000",
            "http://localhost:3001", 
            "http://localhost:5173",
            "http://127.0.0.1:*",
            "https://*.vercel.app",
            "https://*.netlify.app"
        )
        
        // 허용할 HTTP 메서드
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        )
        
        // 허용할 헤더
        configuration.allowedHeaders = listOf("*")
        
        // 인증 정보 포함 허용
        configuration.allowCredentials = true
        
        // 노출할 헤더 (JWT 토큰 등)
        configuration.exposedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        )
        
        // Preflight 요청 캐시 시간 (1시간)
        configuration.maxAge = 3600L
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        
        return source
    }
}