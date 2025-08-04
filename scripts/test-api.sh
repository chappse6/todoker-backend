#!/bin/bash

# API 테스트 스크립트
# 사용법: ./scripts/test-api.sh

BASE_URL="http://localhost:8080"
USERNAME="testuser_$(date +%s)"
EMAIL="${USERNAME}@example.com"
PASSWORD="Test1234!"

echo "🚀 Todoker API 테스트 시작"
echo "================================"

# 1. 회원가입
echo -e "\n1️⃣ 회원가입 테스트"
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${USERNAME}\",
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\",
    \"nickname\": \"테스트 사용자\"
  }")

echo "응답: ${REGISTER_RESPONSE}"

# 2. 로그인
echo -e "\n2️⃣ 로그인 테스트"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${USERNAME}\",
    \"password\": \"${PASSWORD}\"
  }")

echo "응답: ${LOGIN_RESPONSE}"

# 토큰 추출
ACCESS_TOKEN=$(echo ${LOGIN_RESPONSE} | grep -o '"access_token":"[^"]*' | sed 's/"access_token":"//')

if [ -z "$ACCESS_TOKEN" ]; then
  echo "❌ 로그인 실패: 토큰을 받지 못했습니다."
  exit 1
fi

echo "✅ 토큰 획득 성공!"
echo "토큰: ${ACCESS_TOKEN:0:20}..."

# 3. 인증된 요청 테스트 - 사용자 정보 조회
echo -e "\n3️⃣ 사용자 정보 조회 테스트"
USER_INFO=$(curl -s -X GET "${BASE_URL}/auth/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "응답: ${USER_INFO}"

# 4. Todo 생성 테스트
echo -e "\n4️⃣ Todo 생성 테스트"
TODO_CREATE=$(curl -s -X POST "${BASE_URL}/todos" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"테스트 할 일\",
    \"description\": \"Swagger 테스트용 할 일입니다\",
    \"priority\": \"HIGH\",
    \"due_date\": \"2025-12-31T23:59:59\"
  }")

echo "응답: ${TODO_CREATE}"

# 5. Todo 목록 조회
echo -e "\n5️⃣ Todo 목록 조회 테스트"
TODO_LIST=$(curl -s -X GET "${BASE_URL}/todos" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "응답: ${TODO_LIST}"

echo -e "\n================================"
echo "✅ API 테스트 완료!"
echo ""
echo "📝 Swagger UI에서 테스트하려면:"
echo "1. http://localhost:8080/swagger-ui/index.html 접속"
echo "2. 우측 상단 'Authorize' 버튼 클릭"
echo "3. 다음 토큰 입력: ${ACCESS_TOKEN}"
echo ""
echo "테스트 계정:"
echo "- Username: ${USERNAME}"
echo "- Password: ${PASSWORD}"