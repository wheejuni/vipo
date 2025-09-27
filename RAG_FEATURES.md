# RAG 기능 문서

## 개요
AI 채팅 플랫폼에 내부 문서를 위한 RAG(Retrieval-Augmented Generation) 기능, 서버 전송 이벤트(SSE) 스트리밍, 그리고 포괄적인 API 문서화 기능이 추가되었습니다.

## 새로운 기능

### 1. 서버 전송 이벤트(SSE) 스트리밍
- **엔드포인트**: `POST /api/conversations/stream`
- **설명**: 실시간 스트리밍 AI 응답
- **Content-Type**: `text/event-stream`

#### 사용 예시:
```javascript
const eventSource = new EventSource('/api/conversations/stream', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer your-jwt-token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    question: "인공지능이란 무엇인가요?",
    model: "gpt-3.5-turbo",
    temperature: 0.7
  })
});

eventSource.onmessage = function(event) {
  console.log('수신됨:', event.data);
};
```

### 2. RAG (검색 증강 생성)

#### 문서 업로드
- **엔드포인트**: `POST /api/rag/documents`
- **Content-Type**: `multipart/form-data`
- **지원 형식**: PDF, TXT, MD, DOCX (설정 가능)

#### 문서 쿼리
- **엔드포인트**: `POST /api/rag/query`
- **설명**: AI를 사용하여 업로드된 문서 검색

#### 스트리밍 문서 쿼리
- **엔드포인트**: `POST /api/rag/query/stream`
- **Content-Type**: `text/event-stream`

#### 문서 관리
- **문서 목록**: `GET /api/rag/documents`
- **문서 삭제**: `DELETE /api/rag/documents/{id}`

#### RAG 쿼리 예시:
```json
{
  "query": "재택근무에 대한 회사 정책은 무엇인가요?",
  "maxResults": 5,
  "threshold": 0.7
}
```

#### 응답 예시:
```json
{
  "answer": "회사 문서에 따르면, 재택근무는 허용됩니다...",
  "sources": [
    {
      "documentId": 1,
      "title": "인사 정책",
      "excerpt": "재택근무 가이드라인...",
      "relevanceScore": 0.9,
      "filename": "hr-policies.pdf"
    }
  ],
  "confidence": 0.9,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 3. API 문서화
- **Swagger UI**: `/swagger-ui.html`에서 이용 가능
- **OpenAPI 스펙**: `/v3/api-docs`에서 이용 가능
- **기능**: 대화형 API 테스트, 포괄적인 엔드포인트 문서화

## 설정

### 환경 변수
```bash
# RAG 설정
RAG_MAX_FILE_SIZE=10MB
RAG_SUPPORTED_FORMATS=pdf,txt,md,docx
RAG_CHUNK_SIZE=1000
RAG_CHUNK_OVERLAP=200

# OpenAI 설정 (AI 기능에 필수)
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-3.5-turbo
OPENAI_EMBEDDING_MODEL=text-embedding-ada-002

# API 문서화
SPRINGDOC_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

### 애플리케이션 속성
```yaml
app:
  rag:
    max-file-size: ${RAG_MAX_FILE_SIZE:10MB}
    supported-formats: ${RAG_SUPPORTED_FORMATS:pdf,txt,md,docx}
    chunk-size: ${RAG_CHUNK_SIZE:1000}
    chunk-overlap: ${RAG_CHUNK_OVERLAP:200}

springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:true}
  swagger-ui:
    enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:true}
```

## WebFlux 호환성

모든 컨트롤러가 이제 리액티브 타입을 반환합니다:
- 단일 응답을 위한 `Mono<ResponseEntity<T>>`
- 스트리밍 응답을 위한 `Flux<String>`
- 리액티브 스트림을 사용한 적절한 오류 처리

## 테스트

### 단위 테스트
- 모든 컨트롤러 테스트가 `StepVerifier` 사용으로 업데이트됨
- 리액티브 테스트 패턴 구현
- 서비스를 위한 Mock 기반 테스트

### 통합 테스트
- RAG 통합 테스트 포함
- WebFlux 테스트 클라이언트 지원
- 리액티브 리포지토리 테스트

## 보안

- 모든 엔드포인트에 JWT 인증 필요
- 사용자 범위 문서 접근
- 입력 검증 및 무해화
- 속도 제한 지원

## 성능 고려사항

- 인메모리 벡터 스토어 (개발에 적합)
- 프로덕션 환경에서는 다음을 고려:
  - Redis 기반 벡터 스토어
  - Elasticsearch 통합
  - 데이터베이스 기반 문서 저장
  - 문서 전달을 위한 CDN

## 향후 개선사항

1. **고급 벡터 스토어**: Pinecone, Weaviate, 또는 Chroma와의 통합
2. **문서 처리**: OCR, 고급 텍스트 추출
3. **의미 검색**: 더 나은 임베딩 모델과 유사도 알고리즘
4. **캐싱**: 자주 접근하는 문서를 위한 Redis 기반 캐싱
5. **분석**: 문서 사용 분석 및 검색 인사이트

## 문제 해결

### 일반적인 문제

1. **벡터 스토어 의존성 오류**:
   - 해결책: 커스텀 인메모리 구현 사용
   - 프로덕션용: 적절한 벡터 스토어 구현

2. **OpenAI API 키 누락**:
   - `OPENAI_API_KEY` 환경 변수 설정
   - API 키에 충분한 크레딧이 있는지 확인

3. **파일 업로드 문제**:
   - 파일 크기 제한 확인
   - 지원되는 파일 형식 확인
   - 적절한 멀티파트 설정 확인

### 로그
- 디버그 로깅 활성화: `logging.level.com.hightemplar.vipo=DEBUG`
- 애플리케이션 로그에서 RAG 작업 모니터링
- 액추에이터 헬스 엔드포인트 확인: `/actuator/health`

## API 예시

### 문서 업로드
```bash
curl -X POST "http://localhost:8080/api/rag/documents" \
  -H "Authorization: Bearer your-jwt-token" \
  -F "file=@document.pdf" \
  -F "title=회사 정책" \
  -F "description=인사 및 회사 정책 문서"
```

### 문서 쿼리
```bash
curl -X POST "http://localhost:8080/api/rag/query" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "휴가 정책은 무엇인가요?",
    "maxResults": 5,
    "threshold": 0.7
  }'
```

### 스트리밍 대화
```bash
curl -X POST "http://localhost:8080/api/conversations/stream" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "머신러닝에 대해 설명해주세요",
    "model": "gpt-3.5-turbo",
    "temperature": 0.7
  }'
```