# VIPO AI 채팅 플랫폼 API 문서

## 개요
VIPO는 AI 기반 채팅 플랫폼으로, 사용자 인증, 대화 관리, RAG(Retrieval-Augmented Generation), 피드백 시스템, 그리고 분석 기능을 제공합니다.

## 기본 정보
- **Base URL**: `http://localhost:8080` (개발 환경), `https://api.vipo.hightemplar.com` (운영 환경)
- **Content-Type**: `application/json`
- **인증 방식**: JWT Bearer Token

## 인증

### 1. 사용자 등록
**POST** `/api/auth/register`

새로운 사용자를 등록합니다.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "name": "홍길동",
  "role": "MEMBER"
}
```

### 2. 사용자 로그인
**POST** `/api/auth/login`

사용자 로그인을 수행합니다.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "name": "홍길동",
  "role": "MEMBER"
}
```

## 대화 관리

### 1. 새 대화 생성
**POST** `/api/conversations`

AI와 새로운 대화를 시작합니다.

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "question": "안녕하세요, AI에게 질문하고 싶습니다.",
  "model": "gpt-3.5-turbo",
  "temperature": 0.7,
  "isStreaming": false
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "threadId": 1,
  "question": "안녕하세요, AI에게 질문하고 싶습니다.",
  "answer": "안녕하세요! 무엇을 도와드릴까요?",
  "model": "gpt-3.5-turbo",
  "isStreaming": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### 2. 스트리밍 대화 생성
**POST** `/api/conversations`

스트리밍 응답으로 대화를 생성합니다.

**Request Body:**
```json
{
  "question": "긴 답변을 원합니다.",
  "model": "gpt-3.5-turbo",
  "temperature": 0.7,
  "isStreaming": true
}
```

**Response (200 OK):**
```
Content-Type: text/event-stream

data: 첫 번째 응답 청크
data: 두 번째 응답 청크
...
```

### 3. 사용자의 모든 대화 조회
**GET** `/api/conversations`

사용자의 모든 대화를 페이지네이션으로 조회합니다.

**Query Parameters:**
- `page` (int, default: 0): 페이지 번호 (0부터 시작)
- `size` (int, default: 20): 페이지 크기
- `sortBy` (string, default: "createdAt"): 정렬 필드
- `sortDirection` (string, default: "desc"): 정렬 방향 (asc/desc)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "threadId": 1,
      "question": "질문 내용",
      "answer": "답변 내용",
      "model": "gpt-3.5-turbo",
      "isStreaming": false,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 4. 특정 대화 조회
**GET** `/api/conversations/{conversationId}`

특정 대화의 상세 정보를 조회합니다.

**Response (200 OK):**
```json
{
  "id": 1,
  "threadId": 1,
  "question": "질문 내용",
  "answer": "답변 내용",
  "model": "gpt-3.5-turbo",
  "isStreaming": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

## 스레드 관리

### 1. 사용자의 스레드 목록 조회
**GET** `/api/conversations/threads`

사용자의 스레드 목록을 조회합니다.

**Query Parameters:**
- `page` (int, default: 0): 페이지 번호
- `size` (int, default: 20): 페이지 크기
- `sortBy` (string, default: "lastActivityAt"): 정렬 필드
- `sortDirection` (string, default: "desc"): 정렬 방향
- `includeConversations` (boolean, default: false): 대화 포함 여부

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "스레드 제목",
      "lastActivityAt": "2024-01-01T10:00:00",
      "createdAt": "2024-01-01T09:00:00",
      "conversations": [] // includeConversations=true일 때만 포함
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 2. 특정 스레드의 대화 조회
**GET** `/api/conversations/threads/{threadId}`

특정 스레드의 모든 대화를 조회합니다.

**Query Parameters:**
- `page` (int, default: 0): 페이지 번호
- `size` (int, default: 20): 페이지 크기
- `sortBy` (string, default: "createdAt"): 정렬 필드
- `sortDirection` (string, default: "asc"): 정렬 방향

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "threadId": 1,
      "question": "첫 번째 질문",
      "answer": "첫 번째 답변",
      "model": "gpt-3.5-turbo",
      "isStreaming": false,
      "createdAt": "2024-01-01T09:00:00",
      "updatedAt": "2024-01-01T09:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 3. 스레드 삭제
**DELETE** `/api/conversations/threads/{threadId}`

스레드와 관련된 모든 대화를 삭제합니다.

**Response (200 OK):**
```json
{
  "message": "Thread deleted successfully"
}
```

## RAG (문서 검색) 기능

### 1. 문서 검색
**POST** `/api/rag/query`

내부 문서를 검색하여 답변을 생성합니다.

**Request Body:**
```json
{
  "query": "검색하고 싶은 내용",
  "maxResults": 5,
  "threshold": 0.7
}
```

**Response (200 OK):**
```json
{
  "answer": "검색 결과를 바탕으로 한 답변",
  "sources": [
    {
      "id": 1,
      "title": "문서 제목",
      "content": "관련 내용",
      "score": 0.85
    }
  ],
  "confidence": 0.85
}
```

### 2. 스트리밍 문서 검색
**POST** `/api/rag/query/stream`

스트리밍 방식으로 문서 검색을 수행합니다.

**Response (200 OK):**
```
Content-Type: text/event-stream

data: 첫 번째 답변 청크
data: 두 번째 답변 청크
...
```

### 3. 문서 업로드
**POST** `/api/rag/documents`

RAG 인덱싱을 위한 문서를 업로드합니다.

**Content-Type:** `multipart/form-data`

**Form Data:**
- `file`: 업로드할 파일
- `title` (optional): 문서 제목
- `description` (optional): 문서 설명

**Response (201 Created):**
```json
{
  "id": 1,
  "filename": "document.pdf",
  "status": "success",
  "message": "Document uploaded and indexed successfully"
}
```

### 4. 사용자 문서 목록 조회
**GET** `/api/rag/documents`

사용자가 업로드한 문서 목록을 조회합니다.

**Query Parameters:**
- `page` (int, default: 0): 페이지 번호
- `size` (int, default: 20): 페이지 크기

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "filename": "document.pdf",
      "title": "문서 제목",
      "description": "문서 설명",
      "uploadedAt": "2024-01-01T10:00:00",
      "status": "indexed"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 5. 문서 삭제
**DELETE** `/api/rag/documents/{documentId}`

문서와 관련된 임베딩을 삭제합니다.

**Response (200 OK):**
```json
{
  "message": "Document deleted successfully"
}
```

## 피드백 시스템

### 1. 피드백 생성
**POST** `/api/feedback`

대화에 대한 피드백을 생성합니다.

**Request Body:**
```json
{
  "conversationId": 1,
  "rating": 5,
  "comment": "매우 도움이 되었습니다."
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "conversationId": 1,
  "rating": 5,
  "comment": "매우 도움이 되었습니다.",
  "status": "PENDING",
  "createdAt": "2024-01-01T10:00:00"
}
```

### 2. 피드백 목록 조회
**GET** `/api/feedback`

피드백 목록을 조회합니다. 일반 사용자는 자신의 피드백만, 관리자는 모든 피드백을 볼 수 있습니다.

**Query Parameters:**
- `page` (int, default: 0): 페이지 번호
- `size` (int, default: 20): 페이지 크기
- `sortBy` (string, default: "createdAt"): 정렬 필드
- `sortDirection` (string, default: "desc"): 정렬 방향
- `status` (string, optional): 피드백 상태 필터 (PENDING, APPROVED, REJECTED)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "conversationId": 1,
      "rating": 5,
      "comment": "매우 도움이 되었습니다.",
      "status": "PENDING",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 3. 특정 피드백 조회
**GET** `/api/feedback/{feedbackId}`

특정 피드백의 상세 정보를 조회합니다.

**Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 1,
  "rating": 5,
  "comment": "매우 도움이 되었습니다.",
  "status": "PENDING",
  "createdAt": "2024-01-01T10:00:00"
}
```

### 4. 피드백 상태 업데이트 (관리자 전용)
**PUT** `/api/feedback/{feedbackId}/status`

피드백의 상태를 업데이트합니다. (관리자 권한 필요)

**Request Body:**
```json
{
  "status": "APPROVED"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 1,
  "rating": 5,
  "comment": "매우 도움이 되었습니다.",
  "status": "APPROVED",
  "createdAt": "2024-01-01T10:00:00"
}
```

### 5. 피드백 생성 가능 여부 확인
**GET** `/api/feedback/can-create/{conversationId}`

특정 대화에 대한 피드백 생성 가능 여부를 확인합니다.

**Response (200 OK):**
```json
{
  "canCreate": true
}
```

## 분석 및 리포팅 (관리자 전용)

### 1. 활동 지표 조회 (최근 24시간)
**GET** `/api/analytics/activity`

최근 24시간의 활동 지표를 조회합니다.

**Response (200 OK):**
```json
{
  "registrationCount": 10,
  "loginCount": 25,
  "conversationCount": 150,
  "uniqueActiveUsers": 20,
  "timeRange": "2024-01-01T00:00:00 to 2024-01-01T23:59:59"
}
```

### 2. 사용자 정의 활동 지표 조회
**GET** `/api/analytics/activity/custom`

사용자가 지정한 기간의 활동 지표를 조회합니다.

**Query Parameters:**
- `startTime` (string): 시작 시간 (ISO 형식: yyyy-MM-ddTHH:mm:ss)
- `endTime` (string): 종료 시간 (ISO 형식: yyyy-MM-ddTHH:mm:ss)

**Response (200 OK):**
```json
{
  "registrationCount": 5,
  "loginCount": 15,
  "conversationCount": 75,
  "uniqueActiveUsers": 12,
  "timeRange": "2024-01-01T00:00:00 to 2024-01-02T00:00:00"
}
```

### 3. 대화 리포트 통계 조회 (최근 24시간)
**GET** `/api/analytics/reports/conversations/statistics`

최근 24시간의 대화 리포트 통계를 조회합니다.

**Response (200 OK):**
```json
{
  "statistics": {
    "totalConversations": 150,
    "uniqueUsers": 20,
    "uniqueThreads": 45,
    "modelUsage": {
      "gpt-3.5-turbo": 100,
      "gpt-4": 50
    },
    "streamingConversations": 80,
    "nonStreamingConversations": 70,
    "timeRange": "2024-01-01T00:00:00 to 2024-01-01T23:59:59"
  },
  "generatedAt": "2024-01-01T23:59:59",
  "csvDownloadUrl": "/api/analytics/reports/conversations/csv"
}
```

### 4. 대화 리포트 CSV 다운로드 (최근 24시간)
**GET** `/api/analytics/reports/conversations/csv`

최근 24시간의 대화 리포트를 CSV 형식으로 다운로드합니다.

**Response (200 OK):**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="conversations_report_24h.csv"

conversation_id,user_email,question,answer,model,created_at
1,user@example.com,질문,답변,gpt-3.5-turbo,2024-01-01T10:00:00
...
```

### 5. 사용자 정의 대화 리포트 통계 조회
**GET** `/api/analytics/reports/conversations/statistics/custom`

사용자가 지정한 기간의 대화 리포트 통계를 조회합니다.

**Query Parameters:**
- `startTime` (string): 시작 시간 (ISO 형식)
- `endTime` (string): 종료 시간 (ISO 형식)

### 6. 사용자 정의 대화 리포트 CSV 다운로드
**GET** `/api/analytics/reports/conversations/csv/custom`

사용자가 지정한 기간의 대화 리포트를 CSV 형식으로 다운로드합니다.

**Query Parameters:**
- `startTime` (string): 시작 시간 (ISO 형식)
- `endTime` (string): 종료 시간 (ISO 형식)

### 7. 분석 서비스 헬스 체크
**GET** `/api/analytics/health`

분석 서비스의 상태를 확인합니다.

**Response (200 OK):**
```json
{
  "status": "healthy",
  "service": "analytics",
  "timestamp": "2024-01-01T10:00:00"
}
```

## 에러 응답

### 일반적인 에러 응답 형식
```json
{
  "error": "에러 메시지",
  "status": 400,
  "timestamp": "2024-01-01T10:00:00"
}
```

### HTTP 상태 코드
- `200 OK`: 요청 성공
- `201 Created`: 리소스 생성 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 부족
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 내부 오류

## 인증 및 권한

### JWT 토큰 사용
모든 보호된 엔드포인트는 `Authorization` 헤더에 JWT 토큰을 포함해야 합니다:

```
Authorization: Bearer {jwt_token}
```

### 사용자 역할
- `MEMBER`: 일반 사용자 (기본 권한)
- `ADMIN`: 관리자 (모든 권한 + 분석 기능)

### 권한별 접근 가능한 엔드포인트

**일반 사용자 (MEMBER):**
- 인증: `/api/auth/*`
- 대화 관리: `/api/conversations/*`
- RAG 기능: `/api/rag/*`
- 피드백: `/api/feedback/*` (자신의 피드백만)

**관리자 (ADMIN):**
- 모든 일반 사용자 권한
- 분석 및 리포팅: `/api/analytics/*`
- 모든 사용자의 피드백 관리: `/api/feedback/*`

## 제한사항

### 요청 제한
- 질문 길이: 최대 10,000자
- 모델명 길이: 최대 50자
- 온도 값: 0.0 ~ 1.0
- RAG 최대 결과 수: 1 ~ 20개
- RAG 임계값: 0.0 ~ 1.0

### 파일 업로드 제한
- 지원 형식: PDF, TXT, DOC, DOCX 등
- 최대 파일 크기: 설정에 따라 다름

## 예제 사용법

### 1. 사용자 등록 및 로그인
```bash
# 사용자 등록
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "홍길동"
  }'

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 2. AI 대화 생성
```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "question": "안녕하세요! 오늘 날씨는 어떤가요?",
    "model": "gpt-3.5-turbo",
    "temperature": 0.7,
    "isStreaming": false
  }'
```

### 3. RAG 문서 검색
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "query": "회사 정책에 대해 알려주세요",
    "maxResults": 5,
    "threshold": 0.7
  }'
```