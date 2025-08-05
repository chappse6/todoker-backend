# DB 쿼리 최적화 설계안

## 🎯 최적화 목표
- N+1 문제 해결 (50-90% 쿼리 수 감소 예상)
- 중복 메서드 제거 및 통합
- 조건부 fetch 전략 구현
- 캐싱 레이어 추가

## 📊 현재 문제점 분석

### 🚨 주요 문제점
1. **N+1 Problem**: Category, PomodoroSession 조회 시 개별 쿼리 실행
2. **중복 메서드**: 유사한 기능의 Repository 메서드들이 여러 개 존재
3. **비효율적 조건문**: Controller에서 복잡한 조건 분기
4. **통계 쿼리 분산**: 별도 쿼리로 통계 계산

### 📈 예상 개선 효과
- **쿼리 수**: 현재 10-20개 → 1-3개로 감소
- **로딩 시간**: 50-80% 개선
- **메모리 사용량**: 30-50% 감소

## 🛠️ 최적화 구현 계획

### 1. Repository 통합 및 조건부 Fetch

#### 기존 (문제)
```kotlin
// 여러 개의 분산된 메서드들
findByUserAndDateOrderByDisplayOrderAsc()
findByUserAndDateWithCategory() 
findByUserAndDateWithCategoryAndPomodoro()
findByFilters()
findByUserAndDateBetween()
```

#### 개선안 (통합)
```kotlin
// 단일 메서드로 통합 + 조건부 fetch
@Query("""
    SELECT DISTINCT t FROM Todo t
    LEFT JOIN FETCH t.category c
    LEFT JOIN FETCH t.pomodoroSessions ps
    WHERE t.user = :user
    AND (:date IS NULL OR t.date = :date)
    AND (:startDate IS NULL OR t.date >= :startDate)
    AND (:endDate IS NULL OR t.date <= :endDate)
    AND (:categoryId IS NULL OR c.id = :categoryId)
    AND (:completed IS NULL OR t.completed = :completed)
    AND (:keyword IS NULL OR LOWER(t.text) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY t.date ASC, t.displayOrder ASC
""")
fun findTodosOptimized(
    @Param("user") user: User,
    @Param("date") date: LocalDate? = null,
    @Param("startDate") startDate: LocalDate? = null,
    @Param("endDate") endDate: LocalDate? = null,
    @Param("categoryId") categoryId: Long? = null,
    @Param("completed") completed: Boolean? = null,
    @Param("keyword") keyword: String? = null
): List<Todo>
```

### 2. DTO Projection 활용

#### 경량 조회용 DTO Projection
```kotlin
// 목록 조회 시 필요한 최소 정보만
interface TodoSummaryProjection {
    val id: Long
    val text: String
    val completed: Boolean
    val date: LocalDate
    val priority: TodoPriority
    val categoryName: String?
    val categoryColor: String?
    val pomodoroCount: Int
}

@Query("""
    SELECT t.id as id, t.text as text, t.completed as completed,
           t.date as date, t.priority as priority,
           c.name as categoryName, c.color as categoryColor,
           COUNT(ps.id) as pomodoroCount
    FROM Todo t
    LEFT JOIN t.category c
    LEFT JOIN t.pomodoroSessions ps
    WHERE t.user = :user AND t.date = :date
    GROUP BY t.id, t.text, t.completed, t.date, t.priority, c.name, c.color
    ORDER BY t.displayOrder ASC
""")
fun findTodoSummariesByDate(
    @Param("user") user: User, 
    @Param("date") date: LocalDate
): List<TodoSummaryProjection>
```

### 3. 통계 쿼리 통합

#### 기존 (문제)
```kotlin
// 3개의 별도 쿼리 실행
countByUserIdAndDate()         // 전체 수
countCompletedByUserIdAndDate() // 완료 수
// 완료율은 애플리케이션에서 계산
```

#### 개선안 (통합)
```kotlin
@Query("""
    SELECT new com.todoker.todokerbackend.dto.response.TodoStatsDto(
        t.date,
        COUNT(t.id),
        SUM(CASE WHEN t.completed = true THEN 1 ELSE 0 END),
        ROUND(AVG(CASE WHEN t.completed = true THEN 100.0 ELSE 0.0 END), 0)
    )
    FROM Todo t 
    WHERE t.user.id = :userId 
    AND (:date IS NULL OR t.date = :date)
    AND (:startDate IS NULL OR t.date >= :startDate)
    AND (:endDate IS NULL OR t.date <= :endDate)
    GROUP BY t.date
    ORDER BY t.date ASC
""")
fun getTodoStatsOptimized(
    @Param("userId") userId: Long,
    @Param("date") date: LocalDate? = null,
    @Param("startDate") startDate: LocalDate? = null,
    @Param("endDate") endDate: LocalDate? = null
): List<TodoStatsDto>
```

### 4. 캐싱 전략

#### Redis 캐싱 레이어
```kotlin
@Service
@Transactional(readOnly = true)
class TodoService {
    
    @Cacheable(value = ["todos"], key = "#user.id + ':' + #date")
    fun getTodosByDateCached(user: User, date: LocalDate): List<TodoResponse> {
        return todoRepository.findTodoSummariesByDate(user, date)
            .map { TodoResponse.fromProjection(it) }
    }
    
    @CacheEvict(value = ["todos"], key = "#user.id + ':' + #todo.date")
    @Transactional
    fun createTodo(user: User, todo: Todo): Todo {
        return todoRepository.save(todo)
    }
}
```

### 5. QueryDSL 도입 (선택사항)

#### 동적 쿼리 최적화
```kotlin
@Repository
class TodoRepositoryCustomImpl : TodoRepositoryCustom {
    
    fun findTodosDynamic(
        user: User,
        filters: TodoSearchFilters
    ): List<Todo> {
        return queryFactory
            .selectFrom(todo)
            .leftJoin(todo.category, category).fetchJoin()
            .leftJoin(todo.pomodoroSessions, pomodoroSession).fetchJoin()
            .where(
                todo.user.eq(user),
                dateCondition(filters.date, filters.startDate, filters.endDate),
                categoryCondition(filters.categoryId),
                completedCondition(filters.completed),
                keywordCondition(filters.keyword)
            )
            .orderBy(todo.date.asc(), todo.displayOrder.asc())
            .distinct()
            .fetch()
    }
}
```

## 📈 성능 개선 예상치

### Before (현재)
```
Todo 목록 조회 (10개 항목):
- Todo 조회: 1 query
- Category 조회: 10 queries (N+1)
- PomodoroSession 조회: 10 queries (N+1)
- 통계 조회: 2 queries
Total: 23 queries, ~200ms
```

### After (최적화 후)
```
Todo 목록 조회 (10개 항목):
- 통합 조회: 1 query (fetch join)
- 통계 조회: 1 query (집계)
- 캐시 히트 시: 0 queries
Total: 2 queries, ~50ms (75% 개선)
```

## 🔄 마이그레이션 계획

### Phase 1: Repository 통합
- [ ] 통합 쿼리 메서드 구현
- [ ] 기존 메서드 deprecated 마킹
- [ ] 단위 테스트 작성

### Phase 2: Service 레이어 최적화
- [ ] 캐싱 레이어 추가
- [ ] DTO Projection 적용
- [ ] 통계 쿼리 통합

### Phase 3: 성능 측정 및 튜닝
- [ ] JPA 쿼리 로깅 활성화
- [ ] 성능 테스트 실행
- [ ] 인덱스 최적화

### Phase 4: 정리
- [ ] 사용하지 않는 메서드 제거
- [ ] 문서 업데이트
- [ ] 코드 리뷰 및 배포

## 🎯 핵심 최적화 포인트

1. **N+1 해결**: `LEFT JOIN FETCH`로 연관 엔티티 한 번에 로딩
2. **쿼리 통합**: 여러 조건을 하나의 쿼리로 처리
3. **Projection 활용**: 필요한 데이터만 선택적 조회
4. **캐싱 적용**: 자주 조회되는 데이터 메모리 캐싱
5. **인덱스 최적화**: 복합 인덱스로 쿼리 성능 향상

## 📋 측정 지표

- **쿼리 수**: 현재 대비 80-90% 감소 목표
- **응답 시간**: 50-75% 개선 목표  
- **메모리 사용량**: 30-50% 감소 목표
- **캐시 히트율**: 80% 이상 목표