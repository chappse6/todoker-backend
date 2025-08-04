#!/bin/bash

# Todoker Frontend TypeScript 타입 생성 스크립트
# 
# 사용법: ./scripts/generate-frontend-types.sh [output-directory]
# 예시: ./scripts/generate-frontend-types.sh ../todoker-frontend/src/api/generated

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 기본 출력 디렉토리
OUTPUT_DIR=${1:-"./frontend-types"}

# API 서버 URL
API_URL="http://localhost:8080/v3/api-docs"

echo -e "${YELLOW}🚀 Todoker Frontend TypeScript 타입 생성 시작${NC}"
echo "================================================"

# 서버 실행 확인
echo -e "\n${YELLOW}1. 백엔드 서버 상태 확인 중...${NC}"
if curl -s -o /dev/null -w "%{http_code}" $API_URL | grep -q "200"; then
    echo -e "${GREEN}✅ 백엔드 서버가 실행 중입니다.${NC}"
else
    echo -e "${RED}❌ 백엔드 서버가 실행되지 않았습니다.${NC}"
    echo -e "${YELLOW}다음 명령어로 서버를 실행하세요: ./gradlew bootRun${NC}"
    exit 1
fi

# OpenAPI Generator 설치 확인
echo -e "\n${YELLOW}2. OpenAPI Generator 설치 확인 중...${NC}"
if ! command -v openapi-generator-cli &> /dev/null; then
    echo -e "${YELLOW}OpenAPI Generator가 설치되지 않았습니다. 설치 중...${NC}"
    npm install -g @openapitools/openapi-generator-cli
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ OpenAPI Generator 설치 실패${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ OpenAPI Generator가 설치되어 있습니다.${NC}"

# 출력 디렉토리 생성
echo -e "\n${YELLOW}3. 출력 디렉토리 준비 중...${NC}"
mkdir -p "$OUTPUT_DIR"
echo -e "${GREEN}✅ 출력 디렉토리: $OUTPUT_DIR${NC}"

# TypeScript 타입 생성
echo -e "\n${YELLOW}4. TypeScript 타입 생성 중...${NC}"
openapi-generator-cli generate \
    -i $API_URL \
    -g typescript-axios \
    -o "$OUTPUT_DIR" \
    --additional-properties=supportsES6=true,npmVersion=10.0.0,withSeparateModelsAndApi=true

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ TypeScript 타입 생성 완료!${NC}"
    
    # 생성된 파일 목록
    echo -e "\n${YELLOW}5. 생성된 파일:${NC}"
    echo "- $OUTPUT_DIR/api.ts (API 클라이언트)"
    echo "- $OUTPUT_DIR/models/ (타입 정의)"
    echo "- $OUTPUT_DIR/api/ (API 메소드)"
    
    # 사용 예제
    echo -e "\n${YELLOW}6. 사용 예제:${NC}"
    cat << EOF
import { Configuration, AuthenticationApi, TodosApi } from '$OUTPUT_DIR';

// API 설정
const config = new Configuration({
    basePath: 'http://localhost:8080',
    accessToken: localStorage.getItem('access_token') || undefined,
});

// API 인스턴스 생성
const authApi = new AuthenticationApi(config);
const todosApi = new TodosApi(config);

// 사용 예제
const login = async (username: string, password: string) => {
    const response = await authApi.authLoginPost({ username, password });
    localStorage.setItem('access_token', response.data.data.access_token);
};
EOF
    
    echo -e "\n${GREEN}🎉 타입 생성이 완료되었습니다!${NC}"
    echo -e "${YELLOW}생성된 타입은 $OUTPUT_DIR 에서 확인하세요.${NC}"
else
    echo -e "${RED}❌ TypeScript 타입 생성 실패${NC}"
    exit 1
fi