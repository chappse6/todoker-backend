package com.todoker.todokerbackend

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * 애플리케이션 기본 테스트
 * Spring Context가 정상적으로 로드되는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Todoker Backend Application 테스트")
class TodokerBackendApplicationTests {

    @Test
    @DisplayName("Spring Context 로드 테스트")
    fun contextLoads() {
        // Spring Context가 정상적으로 로드되면 테스트 통과
        // 이 테스트는 애플리케이션의 모든 빈이 정상적으로 생성되는지 확인합니다
    }
}
