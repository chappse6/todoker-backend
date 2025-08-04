package com.todoker.todokerbackend.service

import com.todoker.todokerbackend.domain.category.Category
import com.todoker.todokerbackend.domain.todo.Todo
import com.todoker.todokerbackend.domain.todo.TodoPriority
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserRole
import com.todoker.todokerbackend.repository.CategoryRepository
import com.todoker.todokerbackend.repository.TodoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

/**
 * TodoService 단위 테스트
 * 모든 의존성을 Mock으로 대체하여 비즈니스 로직만을 테스트
 */
@DisplayName("TodoService 테스트")
class TodoServiceTest {
    
    private val todoRepository = mockk<TodoRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val todoService = TodoService(todoRepository, categoryRepository)
    
    private lateinit var testUser: User
    private lateinit var testCategory: Category
    private lateinit var testTodo: Todo
    
    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
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
        
        // 테스트용 카테고리 생성
        testCategory = Category(
            name = "업무",
            color = "#FF5722",
            user = testUser
        ).apply { 
            // Use reflection to set protected id field for testing
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        
        // 테스트용 할 일 생성
        testTodo = Todo(
            text = "테스트 할 일",
            user = testUser,
            date = LocalDate.now(),
            category = testCategory,
            priority = TodoPriority.HIGH
        ).apply { 
            // Use reflection to set protected id field for testing
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
    }
    
    @Nested
    @DisplayName("할 일 조회")
    inner class GetTodos {
        
        @Test
        @DisplayName("ID로 할 일 조회 - 성공")
        fun `should return todo when valid id provided`() {
            // given
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            
            // when
            val result = todoService.getTodoById(1L, testUser)
            
            // then
            assertThat(result).isEqualTo(testTodo)
            verify { todoRepository.findByIdAndUser(1L, testUser) }
        }
        
        @Test
        @DisplayName("존재하지 않는 ID로 할 일 조회 - 실패")
        fun `should throw exception when todo not found`() {
            // given
            every { todoRepository.findByIdAndUser(999L, testUser) } returns Optional.empty()
            
            // when & then
            assertThrows<NoSuchElementException> {
                todoService.getTodoById(999L, testUser)
            }
        }
        
        @Test
        @DisplayName("날짜별 할 일 조회")
        fun `should return todos by date`() {
            // given
            val targetDate = LocalDate.now()
            val todos = listOf(testTodo)
            every { todoRepository.findByUserAndDateWithCategory(testUser, targetDate) } returns todos
            
            // when
            val result = todoService.getTodosByDate(testUser, targetDate)
            
            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(testTodo)
        }
        
        @Test
        @DisplayName("필터를 사용한 할 일 조회")
        fun `should return filtered todos`() {
            // given
            val categoryId = 1L
            val completed = false
            val todos = listOf(testTodo)
            every { todoRepository.findByFilters(testUser, null, categoryId, completed) } returns todos
            
            // when
            val result = todoService.getTodosByFilters(testUser, null, categoryId, completed)
            
            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(testTodo)
        }
    }
    
    @Nested
    @DisplayName("할 일 생성")
    inner class CreateTodo {
        
        @Test
        @DisplayName("기본 할 일 생성 - 성공")
        fun `should create todo successfully`() {
            // given
            val text = "새로운 할 일"
            val date = LocalDate.now()
            val savedTodo = slot<Todo>()
            
            every { todoRepository.findMaxDisplayOrderByUserIdAndDate(testUser.id!!, date) } returns 5
            every { todoRepository.save(capture(savedTodo)) } returnsArgument 0
            
            // when
            val result = todoService.createTodo(testUser, text, date)
            
            // then
            assertThat(result.text).isEqualTo(text)
            assertThat(result.date).isEqualTo(date)
            assertThat(result.user).isEqualTo(testUser)
            assertThat(result.displayOrder).isEqualTo(6) // maxOrder + 1
            
            verify { todoRepository.save(any()) }
        }
        
        @Test
        @DisplayName("카테고리와 함께 할 일 생성 - 성공")
        fun `should create todo with category successfully`() {
            // given
            val text = "카테고리가 있는 할 일"
            val date = LocalDate.now()
            val categoryId = 1L
            
            every { categoryRepository.findByIdAndUser(categoryId, testUser) } returns Optional.of(testCategory)
            every { todoRepository.findMaxDisplayOrderByUserIdAndDate(testUser.id!!, date) } returns null
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.createTodo(testUser, text, date, categoryId)
            
            // then
            assertThat(result.text).isEqualTo(text)
            assertThat(result.category).isEqualTo(testCategory)
            assertThat(result.displayOrder).isEqualTo(0) // null이면 0으로 설정
        }
        
        @Test
        @DisplayName("존재하지 않는 카테고리로 할 일 생성 - 실패")
        fun `should throw exception when category not found`() {
            // given
            val text = "할 일"
            val categoryId = 999L
            
            every { categoryRepository.findByIdAndUser(categoryId, testUser) } returns Optional.empty()
            
            // when & then
            assertThrows<NoSuchElementException> {
                todoService.createTodo(testUser, text, categoryId = categoryId)
            }
        }
    }
    
    @Nested
    @DisplayName("할 일 수정")
    inner class UpdateTodo {
        
        @Test
        @DisplayName("할 일 내용 수정 - 성공")
        fun `should update todo text successfully`() {
            // given
            val newText = "수정된 할 일"
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.updateTodo(1L, testUser, text = newText)
            
            // then
            assertThat(result.text).isEqualTo(newText)
            verify { todoRepository.save(testTodo) }
        }
        
        @Test
        @DisplayName("할 일 카테고리 수정 - 성공")
        fun `should update todo category successfully`() {
            // given
            val newCategory = Category(
                name = "새 카테고리", 
                color = "#123456", 
                user = testUser
            ).apply { 
                // Use reflection to set protected id field for testing
                val idField = this::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 2L)
            }
            
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            every { categoryRepository.findByIdAndUser(2L, testUser) } returns Optional.of(newCategory)
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.updateTodo(1L, testUser, categoryId = 2L)
            
            // then
            assertThat(result.category).isEqualTo(newCategory)
        }
    }
    
    @Nested
    @DisplayName("할 일 상태 변경")
    inner class ToggleTodo {
        
        @Test
        @DisplayName("할 일 완료 토글 - 미완료에서 완료로")
        fun `should toggle todo from incomplete to complete`() {
            // given
            testTodo.completed = false
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.toggleTodo(1L, testUser)
            
            // then
            assertThat(result.completed).isTrue()
            assertThat(result.completedAt).isNotNull()
        }
        
        @Test
        @DisplayName("할 일 완료 토글 - 완료에서 미완료로")
        fun `should toggle todo from complete to incomplete`() {
            // given
            testTodo.complete() // 먼저 완료 상태로 만듦
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.toggleTodo(1L, testUser)
            
            // then
            assertThat(result.completed).isFalse()
            assertThat(result.completedAt).isNull()
        }
    }
    
    @Nested
    @DisplayName("할 일 순서 변경")
    inner class ReorderTodos {
        
        @Test
        @DisplayName("할 일 순서 변경 - 성공")
        fun `should reorder todos successfully`() {
            // given
            val date = LocalDate.now()
            val todo1 = testTodo.apply { 
                val idField = this::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
                displayOrder = 0 
            }
            val todo2 = Todo("두번째 할 일", testUser, date).apply { 
                val idField = this::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 2L)
                displayOrder = 1 
            }
            val todo3 = Todo("세번째 할 일", testUser, date).apply { 
                val idField = this::class.java.superclass.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 3L)
                displayOrder = 2 
            }
            val todos = listOf(todo1, todo2, todo3)
            val newOrder = listOf(3L, 1L, 2L) // 순서 변경
            
            every { todoRepository.findByUserAndDateOrderByDisplayOrderAsc(testUser, date) } returns todos
            every { todoRepository.save(any()) } returnsArgument 0
            
            // when
            val result = todoService.reorderTodos(testUser, date, newOrder)
            
            // then
            assertThat(result).hasSize(3)
            assertThat(result[0].id).isEqualTo(3L) // 세번째가 첫번째로
            assertThat(result[0].displayOrder).isEqualTo(0)
            assertThat(result[1].id).isEqualTo(1L) // 첫번째가 두번째로
            assertThat(result[1].displayOrder).isEqualTo(1)
            assertThat(result[2].id).isEqualTo(2L) // 두번째가 세번째로
            assertThat(result[2].displayOrder).isEqualTo(2)
            
            verify(exactly = 3) { todoRepository.save(any()) }
        }
    }
    
    @Nested
    @DisplayName("할 일 삭제")
    inner class DeleteTodo {
        
        @Test
        @DisplayName("할 일 삭제 - 성공")
        fun `should delete todo successfully`() {
            // given
            every { todoRepository.findByIdAndUser(1L, testUser) } returns Optional.of(testTodo)
            every { todoRepository.delete(testTodo) } returns Unit
            
            // when
            todoService.deleteTodo(1L, testUser)
            
            // then
            verify { todoRepository.delete(testTodo) }
        }
    }
    
    @Nested
    @DisplayName("할 일 통계")
    inner class TodoStats {
        
        @Test
        @DisplayName("일별 할 일 통계 조회")
        fun `should return daily todo stats`() {
            // given
            val date = LocalDate.now()
            every { todoRepository.countByUserIdAndDate(testUser.id!!, date) } returns 10L
            every { todoRepository.countCompletedByUserIdAndDate(testUser.id!!, date) } returns 7L
            
            // when
            val result = todoService.getTodoStats(testUser, date)
            
            // then
            assertThat(result["total"]).isEqualTo(10L)
            assertThat(result["completed"]).isEqualTo(7L)
            assertThat(result["incomplete"]).isEqualTo(3L)
            assertThat(result["completionRate"]).isEqualTo(70) // 7/10 * 100
        }
        
        @Test
        @DisplayName("할 일이 없는 날의 통계 조회")
        fun `should return zero stats when no todos`() {
            // given
            val date = LocalDate.now()
            every { todoRepository.countByUserIdAndDate(testUser.id!!, date) } returns 0L
            every { todoRepository.countCompletedByUserIdAndDate(testUser.id!!, date) } returns 0L
            
            // when
            val result = todoService.getTodoStats(testUser, date)
            
            // then
            assertThat(result["total"]).isEqualTo(0L)
            assertThat(result["completed"]).isEqualTo(0L)
            assertThat(result["incomplete"]).isEqualTo(0L)
            assertThat(result["completionRate"]).isEqualTo(0)
        }
    }
}