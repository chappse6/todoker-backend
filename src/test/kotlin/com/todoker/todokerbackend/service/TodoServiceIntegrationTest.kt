package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.todo.TodoPriority
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserRole
import com.todoker.todokerbackend.repository.CategoryRepository
import com.todoker.todokerbackend.repository.TodoRepository
import com.todoker.todokerbackend.repository.UserRepository
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * TodoService 통합 테스트
 * 실제 데이터베이스와 연동하여 전체 플로우를 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TodoService 통합 테스트")
class TodoServiceIntegrationTest {
    
    @Autowired
    private lateinit var todoService: TodoService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var categoryRepository: CategoryRepository
    
    @Autowired
    private lateinit var todoRepository: TodoRepository
    
    private lateinit var testUser: User
    private lateinit var testCategory: Category
    
    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성 및 저장
        testUser = User(
            usernameValue = "testuser",
            email = "test@example.com",
            password = "password123!",
            nickname = "테스트유저",
            role = UserRole.USER
        )
        testUser = userRepository.save(testUser)
        
        // 테스트용 카테고리 생성 및 저장
        testCategory = Category(
            name = "업무",
            color = "#FF5722",
            user = testUser
        )
        testCategory = categoryRepository.save(testCategory)
    }
    
    @Test
    @DisplayName("할 일 생성 및 조회 - 통합 테스트")
    fun `should create and retrieve todo successfully`() {
        // given
        val text = "통합 테스트 할 일"
        val date = LocalDate.now()
        val priority = TodoPriority.HIGH
        
        // when - 할 일 생성
        val createdTodo = todoService.createTodo(
            user = testUser,
            text = text,
            date = date,
            categoryId = testCategory.id,
            priority = priority
        )
        
        // then - 생성된 할 일 검증
        assertThat(createdTodo.id).isNotNull()
        assertThat(createdTodo.text).isEqualTo(text)
        assertThat(createdTodo.date).isEqualTo(date)
        assertThat(createdTodo.priority).isEqualTo(priority)
        assertThat(createdTodo.category?.id).isEqualTo(testCategory.id)
        assertThat(createdTodo.user.id).isEqualTo(testUser.id)
        assertThat(createdTodo.completed).isFalse()
        
        // when - 생성된 할 일 조회
        val retrievedTodo = todoService.getTodoById(createdTodo.id!!, testUser)
        
        // then - 조회된 할 일 검증
        assertThat(retrievedTodo.id).isEqualTo(createdTodo.id)
        assertThat(retrievedTodo.text).isEqualTo(text)
    }
    
    @Test
    @DisplayName("할 일 수정 - 통합 테스트")
    fun `should update todo successfully`() {
        // given - 할 일 생성
        val originalTodo = todoService.createTodo(
            user = testUser,
            text = "원본 할 일",
            date = LocalDate.now()
        )
        
        val newText = "수정된 할 일"
        val newPriority = TodoPriority.LOW
        
        // when - 할 일 수정
        val updatedTodo = todoService.updateTodo(
            todoId = originalTodo.id!!,
            user = testUser,
            text = newText,
            priority = newPriority
        )
        
        // then - 수정된 할 일 검증
        assertThat(updatedTodo.id).isEqualTo(originalTodo.id)
        assertThat(updatedTodo.text).isEqualTo(newText)
        assertThat(updatedTodo.priority).isEqualTo(newPriority)
        assertThat(updatedTodo.updatedAt).isAfterOrEqualTo(originalTodo.updatedAt)
    }
    
    @Test
    @DisplayName("할 일 완료 토글 - 통합 테스트")
    fun `should toggle todo completion successfully`() {
        // given - 미완료 할 일 생성
        val todo = todoService.createTodo(
            user = testUser,
            text = "토글 테스트 할 일",
            date = LocalDate.now()
        )
        
        assertThat(todo.completed).isFalse()
        assertThat(todo.completedAt).isNull()
        
        // when - 완료로 토글
        val completedTodo = todoService.toggleTodo(todo.id!!, testUser)
        
        // then - 완료 상태 검증
        assertThat(completedTodo.completed).isTrue()
        assertThat(completedTodo.completedAt).isNotNull()
        
        // when - 다시 미완료로 토글
        val incompleteTodo = todoService.toggleTodo(todo.id!!, testUser)
        
        // then - 미완료 상태 검증
        assertThat(incompleteTodo.completed).isFalse()
        assertThat(incompleteTodo.completedAt).isNull()
    }
    
    @Test
    @DisplayName("날짜별 할 일 조회 - 통합 테스트")
    fun `should retrieve todos by date successfully`() {
        // given - 여러 날짜의 할 일들 생성
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        
        val todayTodo = todoService.createTodo(testUser, "오늘 할 일", today)
        val yesterdayTodo = todoService.createTodo(testUser, "어제 할 일", yesterday)
        val tomorrowTodo = todoService.createTodo(testUser, "내일 할 일", tomorrow)
        
        // when - 오늘 날짜로 조회
        val todayTodos = todoService.getTodosByDate(testUser, today)
        
        // then - 오늘 할 일만 조회되는지 검증
        assertThat(todayTodos).hasSize(1)
        assertThat(todayTodos[0].id).isEqualTo(todayTodo.id)
        assertThat(todayTodos[0].text).isEqualTo("오늘 할 일")
    }
    
    @Test
    @DisplayName("할 일 순서 변경 - 통합 테스트")
    fun `should reorder todos successfully`() {
        // given - 여러 할 일 생성
        val date = LocalDate.now()
        val todo1 = todoService.createTodo(testUser, "첫 번째 할 일", date)
        val todo2 = todoService.createTodo(testUser, "두 번째 할 일", date)
        val todo3 = todoService.createTodo(testUser, "세 번째 할 일", date)
        
        // 원래 순서: todo1(0), todo2(1), todo3(2)
        val originalTodos = todoService.getTodosByDate(testUser, date)
        assertThat(originalTodos.map { it.id }).containsExactly(todo1.id, todo2.id, todo3.id)
        
        // when - 순서 변경 (3, 1, 2 순서로)
        val newOrder = listOf(todo3.id!!, todo1.id!!, todo2.id!!)
        val reorderedTodos = todoService.reorderTodos(testUser, date, newOrder)
        
        // then - 새로운 순서로 정렬되었는지 검증
        assertThat(reorderedTodos).hasSize(3)
        assertThat(reorderedTodos[0].id).isEqualTo(todo3.id)
        assertThat(reorderedTodos[0].displayOrder).isEqualTo(0)
        assertThat(reorderedTodos[1].id).isEqualTo(todo1.id)
        assertThat(reorderedTodos[1].displayOrder).isEqualTo(1)
        assertThat(reorderedTodos[2].id).isEqualTo(todo2.id)
        assertThat(reorderedTodos[2].displayOrder).isEqualTo(2)
    }
    
    @Test
    @DisplayName("할 일 통계 조회 - 통합 테스트")
    fun `should return todo statistics successfully`() {
        // given - 여러 할 일 생성 (일부는 완료)
        val date = LocalDate.now()
        val todo1 = todoService.createTodo(testUser, "할 일 1", date)
        val todo2 = todoService.createTodo(testUser, "할 일 2", date)
        val todo3 = todoService.createTodo(testUser, "할 일 3", date)
        val todo4 = todoService.createTodo(testUser, "할 일 4", date)
        
        // 2개 완료
        todoService.completeTodo(todo1.id!!, testUser)
        todoService.completeTodo(todo2.id!!, testUser)
        
        // when - 통계 조회
        val stats = todoService.getTodoStats(testUser, date)
        
        // then - 통계 검증
        assertThat(stats["total"]).isEqualTo(4L)
        assertThat(stats["completed"]).isEqualTo(2L)
        assertThat(stats["incomplete"]).isEqualTo(2L)
        assertThat(stats["completionRate"]).isEqualTo(50) // 2/4 * 100 = 50%
    }
    
    @Test
    @DisplayName("할 일 삭제 - 통합 테스트")
    fun `should delete todo successfully`() {
        // given - 할 일 생성
        val todo = todoService.createTodo(
            testUser,
            "삭제될 할 일",
            LocalDate.now()
        )
        
        val todoId = todo.id!!
        
        // 할 일이 존재하는지 확인
        val existingTodo = todoService.getTodoById(todoId, testUser)
        assertThat(existingTodo).isNotNull()
        
        // when - 할 일 삭제
        todoService.deleteTodo(todoId, testUser)
        
        // then - 할 일이 삭제되었는지 확인
        assertThatThrownBy {
            todoService.getTodoById(todoId, testUser)
        }.isInstanceOf(NoSuchElementException::class.java)
    }
}