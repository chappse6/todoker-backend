package com.todoker.todokerbackend.controller

import com.todoker.todokerbackend.common.response.ApiResponse
import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.dto.response.CategoryResponse
import com.todoker.todokerbackend.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 카테고리 관리 API 컨트롤러
 * 카테고리의 CRUD, 순서 변경, 통계 조회 등의 기능을 제공
 */
@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequestMapping("/categories")
class CategoryController(
    private val categoryService: CategoryService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * 카테고리 목록 조회
     * 활성화된 카테고리만 조회하거나 모든 카테고리 조회 가능
     */
    @Operation(summary = "카테고리 목록 조회", description = "사용자의 카테고리 목록을 조회합니다.")
    @GetMapping
    fun getCategories(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "활성화된 카테고리만 조회할지 여부 (기본값: true)")
        @RequestParam(defaultValue = "true") activeOnly: Boolean
    ): ApiResponse<List<CategoryResponse>> {
        logger.debug("Getting categories for user: ${user.getUsername()}, activeOnly: $activeOnly")
        
        val categories = if (activeOnly) {
            categoryService.getActiveCategoriesByUser(user)
        } else {
            categoryService.getCategoriesByUser(user)
        }
        
        val response = categories.map { CategoryResponse.from(it) }
        return ApiResponse.success(response)
    }
    
    /**
     * 특정 카테고리 상세 조회
     */
    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    fun getCategory(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "카테고리 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<CategoryResponse> {
        logger.debug("Getting category: id=$id, user=${user.getUsername()}")
        
        val category = categoryService.getCategoryById(id, user)
        return ApiResponse.success(CategoryResponse.from(category))
    }
    
    /**
     * 새로운 카테고리 생성
     */
    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: CreateCategoryRequest
    ): ApiResponse<CategoryResponse> {
        logger.info("Creating category for user: ${user.getUsername()}, name: ${request.name}")
        
        val category = categoryService.createCategory(
            user = user,
            name = request.name,
            color = request.color
        )
        
        return ApiResponse.success(CategoryResponse.from(category))
    }
    
    /**
     * 카테고리 수정
     */
    @Operation(summary = "카테고리 수정", description = "기존 카테고리의 정보를 수정합니다.")
    @PutMapping("/{id}")
    fun updateCategory(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "카테고리 ID", required = true)
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): ApiResponse<CategoryResponse> {
        logger.info("Updating category: id=$id, user=${user.getUsername()}")
        
        val category = categoryService.updateCategory(
            categoryId = id,
            user = user,
            name = request.name,
            color = request.color
        )
        
        return ApiResponse.success(CategoryResponse.from(category))
    }
    
    /**
     * 카테고리 순서 변경 (드래그 앤 드롭)
     */
    @Operation(summary = "카테고리 순서 변경", description = "카테고리들의 순서를 변경합니다.")
    @PutMapping("/reorder")
    fun reorderCategories(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: ReorderCategoriesRequest
    ): ApiResponse<List<CategoryResponse>> {
        logger.info("Reordering categories for user: ${user.getUsername()}, count: ${request.categoryIds.size}")
        
        val categories = categoryService.reorderCategories(user, request.categoryIds)
        val response = categories.map { CategoryResponse.from(it) }
        
        return ApiResponse.success(response)
    }
    
    /**
     * 카테고리 삭제 (비활성화)
     * 실제로는 soft delete를 수행하여 관련 데이터를 보존
     */
    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다. (실제로는 비활성화)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "카테고리 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<Nothing> {
        logger.info("Deleting category: id=$id, user=${user.getUsername()}")
        
        categoryService.deleteCategory(id, user)
        return ApiResponse.success()
    }
    
    /**
     * 카테고리 활성화
     * 삭제(비활성화)된 카테고리를 다시 활성화
     */
    @Operation(summary = "카테고리 활성화", description = "비활성화된 카테고리를 다시 활성화합니다.")
    @PatchMapping("/{id}/activate")
    fun activateCategory(
        @AuthenticationPrincipal user: User,
        @Parameter(description = "카테고리 ID", required = true)
        @PathVariable id: Long
    ): ApiResponse<CategoryResponse> {
        logger.info("Activating category: id=$id, user=${user.getUsername()}")
        
        val category = categoryService.activateCategory(id, user)
        return ApiResponse.success(CategoryResponse.from(category))
    }
    
    /**
     * 카테고리별 통계 조회
     * 각 카테고리의 할 일 개수, 완료율 등을 조회
     */
    @Operation(summary = "카테고리 통계", description = "카테고리별 할 일 통계를 조회합니다.")
    @GetMapping("/stats")
    fun getCategoryStats(
        @AuthenticationPrincipal user: User
    ): ApiResponse<List<Map<String, Any>>> {
        logger.debug("Getting category stats for user: ${user.getUsername()}")
        
        val stats = categoryService.getCategoryStats(user)
        return ApiResponse.success(stats)
    }
}

/**
 * 카테고리 생성 요청 DTO
 */
data class CreateCategoryRequest(
    @field:NotBlank(message = "카테고리 이름은 필수입니다")
    @field:Size(max = 50, message = "카테고리 이름은 50자 이하여야 합니다")
    val name: String,
    
    @field:Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "올바른 hex 색상 코드가 아닙니다")
    val color: String = "#4CAF50"
)

/**
 * 카테고리 수정 요청 DTO
 */
data class UpdateCategoryRequest(
    @field:Size(max = 50, message = "카테고리 이름은 50자 이하여야 합니다")
    val name: String? = null,
    
    @field:Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "올바른 hex 색상 코드가 아닙니다")
    val color: String? = null
)

/**
 * 카테고리 순서 변경 요청 DTO
 */
data class ReorderCategoriesRequest(
    @field:NotEmpty(message = "카테고리 ID 목록은 필수입니다")
    val categoryIds: List<Long>
)