# VIPO AI 채팅 플랫폼

VIPO는 AI 기반 채팅 플랫폼으로, 사용자 인증, 대화 관리, RAG(Retrieval-Augmented Generation), 피드백 시스템, 그리고 분석 기능을 제공하는 포괄적인 솔루션입니다.

## 🚀 주요 기능

- **🤖 AI 대화**: OpenAI GPT 모델을 활용한 지능형 채팅
- **📄 RAG 기능**: 내부 문서 검색 및 질의응답
- **🔄 실시간 스트리밍**: Server-Sent Events(SSE)를 통한 실시간 응답
- **👥 사용자 관리**: JWT 기반 인증 및 권한 관리
- **💬 스레드 관리**: 대화 스레드 생성 및 관리
- **⭐ 피드백 시스템**: 사용자 피드백 수집 및 관리
- **📊 분석 및 리포팅**: 포괄적인 사용자 활동 분석
- **📚 API 문서화**: Swagger UI를 통한 대화형 API 문서

## 🛠 기술 스택

- **Backend**: Spring Boot 3.x, Kotlin
- **Database**: PostgreSQL
- **AI Service**: OpenAI API
- **Authentication**: JWT
- **Documentation**: OpenAPI 3.0, Swagger UI
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Gradle

## 📋 문서

### 📖 API 문서
- **[API 문서](./API_DOCUMENTATION.md)** - 완전한 API 참조 가이드
  - 인증 및 사용자 관리
  - 대화 및 스레드 관리
  - RAG 기능 (문서 검색)
  - 피드백 시스템
  - 분석 및 리포팅 (관리자 전용)

### 🚀 개발 및 배포
- **[개발 환경 설정](./docs/development-setup.md)** - Docker를 사용한 개발 환경 구축
  - 사전 요구사항
  - 빠른 시작 가이드
  - Docker 서비스 구성
  - API 테스트 예시
  - 문제 해결 가이드

- **[환경 변수 설정](./docs/environment-variables.md)** - 애플리케이션 설정 가이드
  - 필수 환경 변수
  - 선택적 설정 옵션
  - 환경별 구성 (개발/운영/테스트)
  - 보안 고려사항

### 🔧 기능별 문서
- **[RAG 기능](./RAG_FEATURES.md)** - 검색 증강 생성 기능 상세 가이드
  - 문서 업로드 및 관리
  - 벡터 검색 및 임베딩
  - 스트리밍 응답
  - 성능 최적화
  - 문제 해결

## 🚀 빠른 시작

### 1. 저장소 클론
```bash
git clone <repository-url>
cd vipo
```

### 2. 환경 변수 설정
```bash
cp .env.example .env
# .env 파일을 편집하여 OpenAI API 키 설정
```

### 3. Docker로 실행
```bash
# 개발 환경 시작
docker-compose up -d

# 선택적 서비스 포함 (Redis, pgAdmin)
docker-compose --profile with-redis --profile with-pgadmin up -d
```

### 4. 애플리케이션 확인
- **애플리케이션**: http://localhost:8080
- **API 문서**: http://localhost:8080/swagger-ui.html
- **헬스 체크**: http://localhost:8080/actuator/health

## 🔑 API 사용 예시

### 사용자 등록 및 로그인
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

### AI 대화 생성
```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "question": "안녕하세요! 오늘 날씨는 어떤가요?",
    "model": "gpt-3.5-turbo",
    "temperature": 0.7,
    "isStreaming": false
  }'
```

### RAG 문서 검색
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "query": "회사 정책에 대해 알려주세요",
    "maxResults": 5,
    "threshold": 0.7
  }'
```

## 🏗 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   AI Service    │
│   (React/Vue)   │◄──►│   (Spring Boot) │◄──►│   (OpenAI API)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   PostgreSQL    │
                       │   Database      │
                       └─────────────────┘
```

## 🔒 보안

- **JWT 인증**: 모든 API 엔드포인트에 JWT 토큰 기반 인증
- **역할 기반 접근 제어**: 사용자(MEMBER) 및 관리자(ADMIN) 권한 분리
- **CORS 설정**: 허용된 도메인에서만 API 접근 가능
- **입력 검증**: 모든 사용자 입력에 대한 검증 및 무해화
- **속도 제한**: API 남용 방지를 위한 요청 제한

## 📊 모니터링

- **헬스 체크**: `/actuator/health`
- **메트릭**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **애플리케이션 로그**: 구조화된 로깅 지원

## 🧪 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest

# 모든 테스트 실행
./gradlew check
```

## 🚀 배포

### 개발 환경
```bash
docker-compose up -d
```

### 운영 환경
```bash
# 환경 변수 설정
export POSTGRES_PASSWORD=your-secure-password
export JWT_SECRET=your-secure-jwt-secret
export OPENAI_API_KEY=your-production-api-key

# 운영 환경 배포
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 🤝 기여하기

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 독점 라이선스 하에 있습니다. 자세한 내용은 [라이선스](https://hightemplar.com/license)를 참조하세요.

## 📞 지원

- **이메일**: dev@hightemplar.com
- **웹사이트**: https://hightemplar.com
- **문서**: 위의 문서 링크들을 참조하세요

## 🔄 버전 히스토리

- **v1.0.0** - 초기 릴리스
  - 기본 AI 채팅 기능
  - 사용자 인증 및 관리
  - RAG 기능
  - 피드백 시스템
  - 분석 및 리포팅

---

**VIPO AI 채팅 플랫폼**으로 더 나은 AI 경험을 시작하세요! 🚀
