# Todoker Backend API 통합 가이드

## 목차
- [시작하기](#시작하기)
- [인증](#인증)
- [API 엔드포인트](#api-엔드포인트)
- [에러 처리](#에러-처리)
- [TypeScript 타입 생성](#typescript-타입-생성)
- [예제 코드](#예제-코드)

## 시작하기

### 서버 정보
- **개발 서버**: http://localhost:8080
- **API 문서 (Swagger UI)**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 요청/응답 형식
- **Content-Type**: `application/json`
- **Response Format**: JSON (snake_case)

### CORS 설정
다음 origin들이 허용됩니다:
- http://localhost:3000
- http://localhost:3001
- http://localhost:5173
- https://*.vercel.app
- https://*.netlify.app

## 인증

### JWT 토큰 기반 인증
모든 API 요청(인증 제외)에는 JWT 토큰이 필요합니다.

#### 토큰 획득 방법
1. 회원가입 → 로그인
2. 로그인 시 `access_token`과 `refresh_token` 발급

#### 토큰 사용 방법
```javascript
// 헤더에 토큰 추가
headers: {
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json'
}
```

#### 토큰 갱신
Access Token 만료 시 Refresh Token으로 갱신:
```javascript
POST /auth/refresh
{
  "refresh_token": "your_refresh_token"
}
```

## API 엔드포인트

### 인증 API

#### 회원가입
```http
POST /auth/register
```
```json
{
  "username": "john_doe",
  "email": "user@example.com",
  "password": "Password123!",
  "nickname": "John"
}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "user@example.com",
    "nickname": "John",
    "created_at": "2025-08-04T10:00:00"
  }
}
```

#### 로그인
```http
POST /auth/login
```
```json
{
  "username": "john_doe",
  "password": "Password123!"
}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "user@example.com",
      "nickname": "John"
    }
  }
}
```

#### 로그아웃
```http
POST /auth/logout
Authorization: Bearer {access_token}
```

### Todo API

#### Todo 목록 조회
```http
GET /todos
Authorization: Bearer {access_token}
```

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)
- `sort`: 정렬 기준 (예: created_at,desc)
- `completed`: 완료 여부 필터 (true/false)
- `category_id`: 카테고리 ID 필터

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "프로젝트 완성하기",
        "description": "Todoker 백엔드 개발",
        "completed": false,
        "priority": "HIGH",
        "due_date": "2025-08-15T23:59:59",
        "category": {
          "id": 1,
          "name": "업무",
          "color": "#FF5733"
        },
        "created_at": "2025-08-04T10:00:00",
        "updated_at": "2025-08-04T10:00:00"
      }
    ],
    "page_number": 0,
    "page_size": 20,
    "total_elements": 1,
    "total_pages": 1,
    "last": true
  }
}
```

#### Todo 생성
```http
POST /todos
Authorization: Bearer {access_token}
```
```json
{
  "title": "새로운 할 일",
  "description": "설명",
  "priority": "MEDIUM",
  "due_date": "2025-08-20T15:00:00",
  "category_id": 1
}
```

#### Todo 수정
```http
PUT /todos/{id}
Authorization: Bearer {access_token}
```
```json
{
  "title": "수정된 할 일",
  "description": "수정된 설명",
  "completed": true,
  "priority": "LOW"
}
```

#### Todo 삭제
```http
DELETE /todos/{id}
Authorization: Bearer {access_token}
```

### 카테고리 API

#### 카테고리 목록 조회
```http
GET /categories
Authorization: Bearer {access_token}
```

#### 카테고리 생성
```http
POST /categories
Authorization: Bearer {access_token}
```
```json
{
  "name": "개인",
  "color": "#00FF00",
  "description": "개인 일정"
}
```

## 에러 처리

### 에러 응답 형식
모든 에러는 다음과 같은 일관된 형식으로 응답됩니다:

```json
{
  "success": false,
  "error": {
    "code": "A002",
    "message": "아이디 또는 비밀번호가 올바르지 않습니다",
    "details": {
      "username": "john_doe"
    }
  },
  "timestamp": "2025-01-14T10:30:00.123456"
}
```

### 에러 코드 체계

#### 🔐 인증 & 권한 (A001-A999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `A001` | 401 | 인증이 필요합니다 | 토큰이 없거나 만료됨 |
| `A002` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다 | 로그인 실패 |
| `A003` | 401 | 토큰이 만료되었습니다 | Access Token 만료 |
| `A004` | 401 | 유효하지 않은 토큰입니다 | 잘못된 토큰 형식 |
| `A005` | 401 | 토큰을 찾을 수 없습니다 | 토큰 누락 |
| `A006` | 401 | 리프레시 토큰이 만료되었습니다 | Refresh Token 만료 |
| `A007` | 401 | 리프레시 토큰을 찾을 수 없습니다 | DB에서 토큰 없음 |
| `A008` | 401 | 유효하지 않은 리프레시 토큰입니다 | 잘못된 Refresh Token |

#### 👤 사용자 관리 (U001-U999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `U001` | 404 | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 |
| `U002` | 409 | 이미 존재하는 사용자입니다 | 중복 사용자 |
| `U003` | 409 | 이미 사용 중인 사용자명입니다 | 중복 username |
| `U004` | 409 | 이미 사용 중인 이메일입니다 | 중복 email |
| `U005` | 400 | 현재 비밀번호가 올바르지 않습니다 | 비밀번호 변경 시 |
| `U006` | 403 | 비활성화된 사용자입니다 | 계정 비활성화 |
| `U007` | 400 | 잘못된 사용자 상태입니다 | 잘못된 상태값 |

#### ✅ Todo 관리 (T001-T999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `T001` | 404 | 할 일을 찾을 수 없습니다 | 존재하지 않는 Todo |
| `T002` | 409 | 이미 완료된 할 일입니다 | 중복 완료 처리 |
| `T003` | 409 | 이미 대기 중인 할 일입니다 | 중복 대기 처리 |
| `T004` | 400 | 잘못된 할 일 상태입니다 | 잘못된 상태값 |
| `T005` | 403 | 해당 할 일에 접근할 권한이 없습니다 | 소유자가 아님 |
| `T006` | 400 | 할 일 제목은 필수입니다 | 제목 누락 |
| `T007` | 400 | 할 일 제목이 너무 깁니다 | 제목 길이 초과 |
| `T008` | 400 | 잘못된 우선순위입니다 | 잘못된 priority 값 |
| `T009` | 400 | 잘못된 마감일입니다 | 잘못된 due_date 형식 |

#### 📁 카테고리 관리 (CT001-CT999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `CT001` | 404 | 카테고리를 찾을 수 없습니다 | 존재하지 않는 카테고리 |
| `CT002` | 409 | 이미 존재하는 카테고리입니다 | 중복 카테고리명 |
| `CT003` | 403 | 해당 카테고리에 접근할 권한이 없습니다 | 소유자가 아님 |
| `CT004` | 400 | 카테고리 이름은 필수입니다 | 이름 누락 |
| `CT005` | 400 | 카테고리 이름이 너무 깁니다 | 이름 길이 초과 |
| `CT006` | 409 | 카테고리에 할 일이 있어서 삭제할 수 없습니다 | 연관 Todo 존재 |
| `CT007` | 400 | 잘못된 카테고리 색상입니다 | 잘못된 색상 형식 |

#### 🍅 뽀모도로 관리 (P001-P999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `P001` | 404 | 뽀모도로 세션을 찾을 수 없습니다 | 존재하지 않는 세션 |
| `P002` | 409 | 이미 실행 중인 뽀모도로 세션이 있습니다 | 중복 실행 |
| `P003` | 409 | 실행 중인 뽀모도로 세션이 없습니다 | 세션 없음 |
| `P004` | 403 | 해당 뽀모도로 세션에 접근할 권한이 없습니다 | 소유자가 아님 |
| `P005` | 400 | 잘못된 뽀모도로 시간입니다 | 잘못된 duration |
| `P006` | 400 | 잘못된 뽀모도로 상태입니다 | 잘못된 상태값 |

#### ✔️ 검증 오류 (V001-V999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `V001` | 400 | 입력값 검증에 실패했습니다 | @Valid 검증 실패 |
| `V002` | 400 | 필수 입력값이 누락되었습니다 | 필수 필드 누락 |
| `V003` | 400 | 올바르지 않은 이메일 형식입니다 | 이메일 형식 오류 |
| `V004` | 400 | 비밀번호 형식이 올바르지 않습니다 | 비밀번호 규칙 위반 |
| `V005` | 400 | 사용자명 형식이 올바르지 않습니다 | username 규칙 위반 |
| `V006` | 400 | 값이 너무 짧습니다 | 최소 길이 미만 |
| `V007` | 400 | 값이 너무 깁니다 | 최대 길이 초과 |
| `V008` | 400 | 올바르지 않은 날짜 형식입니다 | 날짜 형식 오류 |
| `V009` | 400 | 올바르지 않은 숫자 형식입니다 | 숫자 형식 오류 |

#### ⚠️ 공통 오류 (C001-C999)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|---------|------|
| `C001` | 400 | 잘못된 입력값입니다 | 일반적인 입력 오류 |
| `C002` | 400 | 잘못된 타입입니다 | 타입 불일치 |
| `C003` | 404 | 요청한 리소스를 찾을 수 없습니다 | 리소스 없음 |
| `C004` | 409 | 잘못된 상태입니다 | 상태 충돌 |
| `C005` | 403 | 접근 권한이 없습니다 | 권한 부족 |
| `C006` | 500 | 서버 내부 오류가 발생했습니다 | 서버 오류 |

### 에러 처리 JavaScript 예제

#### 기본 에러 처리
```javascript
// API 호출 시 에러 처리
try {
  const response = await api.post('/auth/login', credentials);
  return response.data.data;
} catch (error) {
  if (error.response?.data?.error) {
    const apiError = error.response.data.error;
    console.error(`[${apiError.code}] ${apiError.message}`);
    
    // 에러 코드별 처리
    switch (apiError.code) {
      case 'A002':
        showError('로그인 정보를 확인해주세요');
        break;
      case 'U003':
        showError('이미 사용 중인 사용자명입니다');
        break;
      case 'V001':
        handleValidationErrors(apiError.details);
        break;
      default:
        showError(apiError.message);
    }
  }
  throw error;
}
```

#### 검증 에러 처리
```javascript
function handleValidationErrors(details) {
  // details 예시: {"email": "올바르지 않은 이메일 형식입니다", "password": "비밀번호는 8자 이상이어야 합니다"}
  Object.entries(details || {}).forEach(([field, message]) => {
    const inputElement = document.querySelector(`[name="${field}"]`);
    if (inputElement) {
      showFieldError(inputElement, message);
    }
  });
}
```

#### React 에러 처리 Hook
```typescript
// hooks/useApiError.ts
import { useState } from 'react';

interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export const useApiError = () => {
  const [error, setError] = useState<ApiError | null>(null);

  const handleError = (error: any) => {
    if (error.response?.data?.error) {
      const apiError = error.response.data.error;
      setError(apiError);
      
      // 자동 로그아웃 처리
      if (['A001', 'A003', 'A004', 'A006', 'A007', 'A008'].includes(apiError.code)) {
        localStorage.clear();
        window.location.href = '/login';
      }
    } else {
      setError({
        code: 'C006',
        message: '네트워크 오류가 발생했습니다'
      });
    }
  };

  const clearError = () => setError(null);

  return { error, handleError, clearError };
};
```

#### TypeScript 타입 정의
```typescript
// types/error.ts
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export interface ErrorResponse {
  success: false;
  error: ApiError;
  timestamp: string;
}

// 에러 코드 enum
export enum ErrorCode {
  // 인증 & 권한
  UNAUTHORIZED = 'A001',
  INVALID_CREDENTIALS = 'A002',
  TOKEN_EXPIRED = 'A003',
  INVALID_TOKEN = 'A004',
  
  // 사용자 관리
  USER_NOT_FOUND = 'U001',
  USERNAME_ALREADY_EXISTS = 'U003',
  EMAIL_ALREADY_EXISTS = 'U004',
  
  // Todo 관리
  TODO_NOT_FOUND = 'T001',
  TODO_ACCESS_DENIED = 'T005',
  
  // 검증 오류
  VALIDATION_FAILED = 'V001',
  INVALID_EMAIL_FORMAT = 'V003',
  
  // 공통 오류
  INTERNAL_SERVER_ERROR = 'C006'
}
```

### 에러 복구 전략

#### 토큰 만료 자동 처리
```javascript
// axios 인터셉터에서 자동 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.data?.error?.code === 'A003' && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refresh_token');
        const response = await api.post('/auth/refresh', {
          refresh_token: refreshToken
        });
        
        const { access_token } = response.data.data;
        localStorage.setItem('access_token', access_token);
        
        originalRequest.headers.Authorization = `Bearer ${access_token}`;
        return api(originalRequest);
      } catch (refreshError) {
        // 리프레시 실패 - 로그아웃
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);
```

#### 재시도 로직
```javascript
// 특정 에러에 대한 자동 재시도
async function apiWithRetry(apiCall, maxRetries = 3) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await apiCall();
    } catch (error) {
      const errorCode = error.response?.data?.error?.code;
      
      // 재시도 가능한 에러 코드
      const retryableCodes = ['C006']; // 서버 내부 오류
      
      if (retryableCodes.includes(errorCode) && attempt < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
        continue;
      }
      
      throw error;
    }
  }
}

## TypeScript 타입 생성

### OpenAPI Generator 사용
```bash
# OpenAPI Generator 설치
npm install -g @openapitools/openapi-generator-cli

# TypeScript 타입 생성
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o ./src/api/generated
```

### 수동 타입 정의 예제
```typescript
// types/auth.ts
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
  user: User;
}

export interface User {
  id: number;
  username: string;
  email: string;
  nickname?: string;
  created_at: string;
}

// types/todo.ts
export interface Todo {
  id: number;
  title: string;
  description?: string;
  completed: boolean;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  due_date?: string;
  category?: Category;
  created_at: string;
  updated_at: string;
}

export interface Category {
  id: number;
  name: string;
  color: string;
  description?: string;
}

// types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
  timestamp: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}

export interface ErrorResponse {
  success: false;
  error: ApiError;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page_number: number;
  page_size: number;
  total_elements: number;
  total_pages: number;
  last: boolean;
}
```

## 예제 코드

### Axios 인터셉터 설정
```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request 인터셉터 - 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response 인터셉터 - 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refresh_token');
        const response = await axios.post('/auth/refresh', {
          refresh_token: refreshToken,
        });

        const { access_token } = response.data.data;
        localStorage.setItem('access_token', access_token);

        originalRequest.headers.Authorization = `Bearer ${access_token}`;
        return api(originalRequest);
      } catch (refreshError) {
        // 리프레시 실패 - 로그인 페이지로 이동
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

### API 서비스 클래스
```typescript
// services/AuthService.ts
import api from '../config/axios';
import { LoginRequest, LoginResponse } from '../types/auth';

class AuthService {
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post('/auth/login', data);
    const loginData = response.data.data;
    
    // 토큰 저장
    localStorage.setItem('access_token', loginData.access_token);
    localStorage.setItem('refresh_token', loginData.refresh_token);
    
    return loginData;
  }

  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      localStorage.clear();
    }
  }

  async register(data: RegisterRequest): Promise<User> {
    const response = await api.post('/auth/register', data);
    return response.data.data;
  }
}

export default new AuthService();
```

```typescript
// services/TodoService.ts
import api from '../config/axios';
import { Todo, CreateTodoRequest, UpdateTodoRequest } from '../types/todo';
import { PageResponse } from '../types/api';

class TodoService {
  async getTodos(params?: {
    page?: number;
    size?: number;
    completed?: boolean;
    category_id?: number;
  }): Promise<PageResponse<Todo>> {
    const response = await api.get('/todos', { params });
    return response.data.data;
  }

  async getTodo(id: number): Promise<Todo> {
    const response = await api.get(`/todos/${id}`);
    return response.data.data;
  }

  async createTodo(data: CreateTodoRequest): Promise<Todo> {
    const response = await api.post('/todos', data);
    return response.data.data;
  }

  async updateTodo(id: number, data: UpdateTodoRequest): Promise<Todo> {
    const response = await api.put(`/todos/${id}`, data);
    return response.data.data;
  }

  async deleteTodo(id: number): Promise<void> {
    await api.delete(`/todos/${id}`);
  }

  async toggleComplete(id: number): Promise<Todo> {
    const response = await api.patch(`/todos/${id}/toggle`);
    return response.data.data;
  }
}

export default new TodoService();
```

### React Hook 예제
```typescript
// hooks/useTodos.ts
import { useState, useEffect } from 'react';
import TodoService from '../services/TodoService';
import { Todo } from '../types/todo';

export const useTodos = () => {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTodos = async () => {
    try {
      setLoading(true);
      const response = await TodoService.getTodos();
      setTodos(response.content);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTodos();
  }, []);

  const createTodo = async (data: CreateTodoRequest) => {
    const newTodo = await TodoService.createTodo(data);
    setTodos([...todos, newTodo]);
    return newTodo;
  };

  const updateTodo = async (id: number, data: UpdateTodoRequest) => {
    const updatedTodo = await TodoService.updateTodo(id, data);
    setTodos(todos.map(todo => 
      todo.id === id ? updatedTodo : todo
    ));
    return updatedTodo;
  };

  const deleteTodo = async (id: number) => {
    await TodoService.deleteTodo(id);
    setTodos(todos.filter(todo => todo.id !== id));
  };

  return {
    todos,
    loading,
    error,
    createTodo,
    updateTodo,
    deleteTodo,
    refetch: fetchTodos,
  };
};
```

## 테스트 환경

### Postman Collection
Swagger UI에서 직접 테스트하거나, OpenAPI 스펙을 Postman으로 import 가능:
1. Postman 열기
2. Import → Link → `http://localhost:8080/v3/api-docs`
3. Collection 생성 및 테스트

### cURL 예제
```bash
# 로그인
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"Password123!"}'

# Todo 조회
curl -X GET http://localhost:8080/todos \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 문의 및 지원

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **팀 연락처**: support@todoker.com

---

*최종 업데이트: 2025-01-14*