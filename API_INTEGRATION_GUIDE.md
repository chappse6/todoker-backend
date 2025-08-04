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
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "field": "필드명 (validation 에러 시)"
  },
  "timestamp": "2025-08-04T10:00:00"
}
```

### 주요 에러 코드
- `UNAUTHORIZED` (401): 인증 필요
- `ACCESS_DENIED` (403): 권한 없음
- `NOT_FOUND` (404): 리소스 없음
- `VALIDATION_ERROR` (400): 입력값 검증 실패
- `INTERNAL_SERVER_ERROR` (500): 서버 에러

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
  error?: {
    code: string;
    message: string;
    field?: string;
  };
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

*최종 업데이트: 2025-08-04*