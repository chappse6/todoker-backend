package com.todoker.todokerbackend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger/OpenAPI 3.0 설정
 * API 문서 자동 생성을 위한 설정
 */
@Configuration
class SwaggerConfig {
    
    /**
     * OpenAPI 설정
     * JWT 인증을 포함한 API 문서 구성
     */
    @Bean
    fun openAPI(): OpenAPI {
        // JWT 보안 스키마 이름
        val jwtSchemeName = "bearerAuth"
        
        // JWT 보안 요구사항
        val securityRequirement = SecurityRequirement()
            .addList(jwtSchemeName)
        
        // JWT 보안 스키마 정의 및 공통 응답 스키마 추가
        val components = Components()
            .addSecuritySchemes(
                jwtSchemeName,
                SecurityScheme()
                    .name(jwtSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 입력하세요 (Bearer 키워드는 자동으로 추가됩니다)")
            )
            // 공통 에러 응답 스키마
            .addSchemas("ErrorResponse", 
                Schema<Any>()
                    .type("object")
                    .title("에러 응답")
                    .description("모든 에러에 대한 공통 응답 형식")
                    .addProperty("success", Schema<Any>().type("boolean").example(false))
                    .addProperty("error", 
                        Schema<Any>()
                            .type("object")
                            .addProperty("code", Schema<Any>().type("string").description("에러 코드").example("A002"))
                            .addProperty("message", Schema<Any>().type("string").description("에러 메시지").example("아이디 또는 비밀번호가 올바르지 않습니다"))
                            .addProperty("details", Schema<Any>().type("object").description("에러 세부 정보 (선택사항)"))
                    )
                    .addProperty("timestamp", Schema<Any>().type("string").format("date-time").example("2025-01-14T10:30:00.123456"))
            )
            // 검증 에러 응답 스키마
            .addSchemas("ValidationErrorResponse",
                Schema<Any>()
                    .type("object")
                    .title("검증 에러 응답")
                    .description("입력값 검증 실패 시 응답")
                    .addProperty("success", Schema<Any>().type("boolean").example(false))
                    .addProperty("error",
                        Schema<Any>()
                            .type("object")
                            .addProperty("code", Schema<Any>().type("string").example("V001"))
                            .addProperty("message", Schema<Any>().type("string").example("입력값 검증에 실패했습니다"))
                            .addProperty("details", 
                                Schema<Any>()
                                    .type("object")
                                    .example(mapOf(
                                        "email" to "올바르지 않은 이메일 형식입니다",
                                        "password" to "비밀번호는 8자 이상이어야 합니다"
                                    ))
                            )
                    )
                    .addProperty("timestamp", Schema<Any>().type("string").format("date-time"))
            )
            // 공통 응답 스키마들 추가
            .addResponses("BadRequest", 
                ApiResponse()
                    .description("잘못된 요청")
                    .content(Content().addMediaType("application/json", 
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse"))
                    ))
            )
            .addResponses("Unauthorized",
                ApiResponse()
                    .description("인증 필요")  
                    .content(Content().addMediaType("application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse"))
                    ))
            )
            .addResponses("Forbidden",
                ApiResponse()
                    .description("접근 권한 없음")
                    .content(Content().addMediaType("application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse")) 
                    ))
            )
            .addResponses("NotFound",
                ApiResponse()
                    .description("리소스를 찾을 수 없음")
                    .content(Content().addMediaType("application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse"))
                    ))
            )
            .addResponses("ValidationError",
                ApiResponse()
                    .description("입력값 검증 실패")
                    .content(Content().addMediaType("application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ValidationErrorResponse"))
                    ))
            )
            .addResponses("InternalServerError",
                ApiResponse()
                    .description("서버 내부 오류")
                    .content(Content().addMediaType("application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse"))
                    ))
            )
        
        return OpenAPI()
            .info(apiInfo())
            .addServersItem(Server().url("/").description("Default Server"))
            .addSecurityItem(securityRequirement)
            .components(components)
    }
    
    /**
     * API 정보 설정
     */
    private fun apiInfo(): Info {
        return Info()
            .title("Todoker Backend API")
            .description("""
                Todoker 할 일 관리 애플리케이션의 REST API 문서입니다.
                
                ## 주요 기능
                - 사용자 인증 (JWT 기반)
                - 할 일(Todo) 관리
                - 카테고리 관리
                - 포모도로 타이머
                - 통계 및 분석
                
                ## 인증
                대부분의 API는 JWT 토큰이 필요합니다.
                1. `/api/auth/login` 또는 `/api/auth/register`로 토큰을 발급받으세요.
                2. 우측 상단의 'Authorize' 버튼을 클릭하여 토큰을 입력하세요.
                3. 토큰은 자동으로 모든 요청에 포함됩니다.
            """.trimIndent())
            .version("1.0.0")
            .contact(
                Contact()
                    .name("Todoker Team")
                    .email("support@todoker.com")
                    .url("https://todoker.com")
            )
            .license(
                License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")
            )
    }
}