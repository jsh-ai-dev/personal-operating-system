# Personal Operating System mk1

개인 지식과 업무 메모를 저장하고, 검색하고, AI로 요약하는 Spring Boot 기반 노트 서비스입니다.
`mk1`은 전체 Personal Operating System 프로젝트의 노트/문서 저장 도메인을 담당하며, 이후 `mk2`의 인증/일정 시스템과 `mk3`의 AI 대화/요약 시스템으로 확장되는 MSA 구조의 첫 번째 서비스입니다.

## 프로젝트 목표

- 개인이 매일 남기는 메모, 학습 자료, PDF 문서를 장기적으로 검색 가능한 지식 저장소로 관리
- 단순 CRUD가 아니라 인증, 소유권 분리, 파일 업로드, AI 요약, 캐시, 검색 인덱스, 배포 구성을 포함한 실사용 가능한 백엔드 서비스 구현
- Clean Architecture와 포트/어댑터 구조를 적용해 저장소, 검색 엔진, AI Provider를 교체 가능한 형태로 설계

## 핵심 기능

- 노트 작성, 수정, 삭제, 상세 조회
- 태그, 공개 범위, 북마크 관리
- 사용자별 노트 소유권 분리
- 페이지네이션, 정렬, 키워드 검색
- `.txt`, `.pdf` 파일 업로드 및 다운로드
- PDF 원본 저장 및 PDFBox 기반 텍스트 추출
- Gemini/OpenAI 기반 AI 요약 생성 및 노트에 저장
- Redis 기반 세션 저장 및 노트 목록 캐시
- Elasticsearch 기반 검색 인덱싱, 하이라이트, DB 검색 fallback
- Thymeleaf 웹 UI와 REST API 동시 제공
- Docker Compose 및 Kubernetes 배포 구성

## 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Kotlin 2.1, Java 21 |
| Backend | Spring Boot 3.5, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, PostgreSQL, H2 test runtime |
| Cache/Session | Redis, Spring Session |
| Search | Elasticsearch, Spring Data Elasticsearch |
| AI | Gemini API, OpenAI API |
| View | Thymeleaf, CSS, JavaScript |
| File Processing | Apache PDFBox |
| Infra | Docker, Docker Compose, Kubernetes, Kustomize |
| Test | JUnit 5, Spring Boot Test, Spring Security Test, Mockito Kotlin |

## 시스템 맥락

이 저장소는 Personal Operating System 시리즈의 `mk1` 서비스입니다.

| 프로젝트 | 역할 | 주요 기술 |
|---|---|---|
| mk1 | 노트, 파일, AI 요약, 검색 | Kotlin, Spring Boot, PostgreSQL, Redis, Elasticsearch |
| mk2 | 인증, 일정, 목표, 프론트 통합 게이트웨이 | Next.js, React, NestJS, Prisma, PostgreSQL |
| mk3 | AI 대화 저장, 요약, 퀴즈, 외부 AI 서비스 연동 | FastAPI, Nuxt, MongoDB, Qdrant |

`mk1`은 자체 세션 로그인도 지원하지만, `mk2`의 auth-service가 발급한 JWT를 Bearer 토큰 또는 httpOnly 쿠키로 받아 동일한 사용자 `sub` 기준으로 노트 소유권을 분리할 수 있게 설계했습니다.

## 아키텍처

```text
src/main/kotlin/com/jsh/pos
├─ domain
│  └─ note                 # Note, Visibility 등 순수 도메인 모델
├─ application
│  ├─ port/in              # 유스케이스 입력 포트
│  ├─ port/out             # 저장소, 검색, 캐시, AI 출력 포트
│  └─ service              # 유스케이스 구현
├─ adapter
│  ├─ in/web               # REST API, Thymeleaf Controller
│  └─ out
│     ├─ ai                # Gemini/OpenAI 요약 어댑터
│     ├─ persistence       # InMemory/JPA 저장소 어댑터
│     └─ search            # Elasticsearch 검색 어댑터
└─ infrastructure
   ├─ cache                # Redis 목록 캐시
   ├─ config               # Security, Cache, Clock 설정
   ├─ search               # 검색 인덱스 재색인 Runner
   └─ security             # JWT 인증 필터, 원격 로그인 연동
```

설계상 도메인과 유스케이스는 Spring MVC, JPA, Elasticsearch, AI SDK에 직접 의존하지 않습니다.
외부 시스템은 포트 뒤의 어댑터로 격리해 테스트와 교체가 쉬운 구조를 유지했습니다.

## 주요 구현 포인트

### 1. 노트 도메인 모델

`Note`는 불변 `data class`로 설계했고, 생성/수정 시 제목과 본문 공백 검증, 태그 정규화, 북마크 상태, AI 요약 저장 규칙을 도메인 메서드에 모았습니다.

- `Note.create(...)`: 생성 시 입력 검증과 trim/filter 처리
- `update(...)`: 본문 수정 시 `updatedAt` 갱신
- `bookmark()` / `unbookmark()`: 콘텐츠 수정 시간과 북마크 변경을 분리
- `updateSummary(...)`: AI 요약 저장 시 빈 요약 방지

### 2. REST API와 Thymeleaf UI 병행

외부 서비스 연동을 위한 REST API와 브라우저에서 바로 사용할 수 있는 Thymeleaf UI를 함께 제공합니다.

대표 API:

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/v1/notes` | 노트 목록, 검색어/북마크/정렬/페이지 조건 지원 |
| POST | `/api/v1/notes` | 노트 생성 |
| POST | `/api/v1/notes/upload` | txt/pdf 파일 업로드 |
| GET | `/api/v1/notes/{id}` | 노트 상세 조회 |
| PUT | `/api/v1/notes/{id}` | 노트 수정 |
| DELETE | `/api/v1/notes/{id}` | 노트 삭제 |
| POST | `/api/v1/notes/{id}/bookmark` | 북마크 등록 |
| DELETE | `/api/v1/notes/{id}/bookmark` | 북마크 해제 |
| GET | `/api/v1/notes/{id}/download` | 원본 파일 또는 텍스트 다운로드 |
| POST | `/api/v1/notes/{id}/summary/generate` | AI 요약 생성 |
| POST | `/api/v1/notes/{id}/summary/save` | 생성된 요약 저장 |

웹 UI:

| Endpoint | 설명 |
|---|---|
| `/login` | 로그인 화면 |
| `/notes` | 노트 목록, 검색, 북마크 필터, 페이지 이동 |
| `/notes/new` | 노트 작성 |
| `/notes/{id}` | 노트 상세, AI 요약 생성/저장 |
| `/notes/{id}/edit` | 직접 작성한 노트 수정 |
| `/summary` | 파일 업로드 기반 단독 요약 화면 |

### 3. 검색과 캐시

목록 조회는 Redis 캐시를 사용합니다.

- 사용자, 조회 모드, 검색어 hash, 정렬, 페이지, 사이즈를 포함한 캐시 키 구성
- 전체 목록, 검색 목록, 북마크 목록을 mode별로 인덱싱
- 노트 생성/수정/삭제/북마크/요약 저장 시 해당 사용자의 관련 캐시 무효화
- TTL 기반 자동 만료

검색은 Elasticsearch를 선택적으로 사용합니다.

- `title`, `tags`, `aiSummary`, `content` 필드에 가중치 적용
- 검색 결과 하이라이트 제공
- Elasticsearch 비활성화 또는 장애 시 JPA 기반 DB 검색으로 fallback
- 시작 시 기존 노트 재색인 옵션 지원

### 4. AI 요약

AI 요약 기능은 `AiSummaryPort` 뒤에 격리되어 있습니다.

- Gemini provider: `gemini-2.5-flash`, `gemini-2.5-pro` 기본 매핑
- OpenAI provider: 환경변수로 모델 교체 가능
- UI에서 `flash` / `pro` 모델 tier 선택
- txt/pdf 업로드 문서 요약
- 노트 상세 화면에서 요약 생성 후 저장
- AI API 오류를 `AiSummaryException`으로 감싸 컨트롤러에서 명확한 사용자 오류로 변환

### 5. 인증과 사용자 분리

- 웹 화면은 Spring Security 세션 기반 인증을 사용합니다.
- 세션은 Redis에 저장해 애플리케이션 재시작과 다중 인스턴스 구성을 고려했습니다.
- REST API는 stateless JWT 인증을 지원합니다.
- `Authorization: Bearer <JWT>` 또는 설정된 httpOnly 쿠키에서 토큰을 읽습니다.
- JWT subject를 사용자 식별자로 사용해 `ownerUsername` 기준으로 노트 접근을 제한합니다.
- 다른 사용자의 노트는 존재 여부를 노출하지 않도록 404로 처리합니다.

## 실행 방법

### 1. 환경 파일 준비

```powershell
Copy-Item .env.example .env
```

주요 환경변수:

```text
POS_DB_URL=jdbc:postgresql://localhost:5432/pos_mk1
POS_DB_USERNAME=pos
POS_DB_PASSWORD=pos
POS_REDIS_HOST=localhost
POS_REDIS_PORT=6379
POS_AI_PROVIDER=gemini
GEMINI_API_KEY=
OPENAI_API_KEY=
POS_JWT_SECRET=
```

### 2. 인프라와 애플리케이션 함께 실행

```powershell
.\bootRun.ps1
```

이 스크립트는 Docker Compose로 PostgreSQL, Redis, Elasticsearch를 올린 뒤 Gradle `bootRun`을 실행합니다.

### 3. 인프라만 실행

```powershell
.\bootRun.ps1 -InfraOnly
```

### 4. 이미 인프라가 떠 있을 때 애플리케이션만 실행

```powershell
.\bootRun.ps1 -SkipInfra
```

### 5. Docker Compose 전체 실행

```powershell
docker compose up -d --build
```

기본 접속 주소:

- Web UI: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Elasticsearch: `http://localhost:9200`

## Elasticsearch 활성화

기본값은 DB 검색입니다. Elasticsearch 검색을 사용하려면 다음 값을 설정합니다.

```text
POS_ELASTICSEARCH_URIS=http://localhost:9200
POS_SEARCH_ELASTICSEARCH_ENABLED=true
POS_SEARCH_ELASTICSEARCH_INDEX_NAME=notes-mk1
POS_SEARCH_ELASTICSEARCH_REINDEX_ON_STARTUP=true
```

Elasticsearch 호출 실패 시 서비스가 중단되지 않고 DB 검색으로 fallback합니다.

## 테스트

```powershell
.\gradlew.bat test
```

현재 테스트는 도메인, 유스케이스, JPA Repository, 웹 컨트롤러, 인증 흐름, AI 응답 파서 등을 포함합니다.

주요 테스트 범위:

- `NoteTest`: 도메인 생성/수정/북마크/요약 규칙
- `CreateNoteServiceTest`, `UpdateNoteServiceTest`, `DeleteNoteServiceTest`: 유스케이스 검증
- `SearchNotesServiceTest`, `GetNoteListPageServiceTest`: 검색/목록 조건 처리
- `JpaNoteRepositoryTest`, `JpaNotePersistenceAdapterTest`: JPA 매핑과 저장소 동작
- `NoteControllerTest`, `NotePageControllerTest`, `AuthenticationFlowTest`: API/UI/인증 흐름
- `GeminiModelResolverTest`, `GeminiResponseExtractorTest`: AI 어댑터 보조 로직

## 배포 구성

### Docker

멀티 스테이지 Dockerfile을 사용합니다.

- build stage: `eclipse-temurin:21-jdk-alpine`에서 `bootJar` 생성
- runtime stage: `eclipse-temurin:21-jre-alpine`
- non-root `app` 사용자로 실행
- 8080 포트 노출

### Kubernetes

`k8s` 디렉터리에 Kustomize 기반 배포 구성이 포함되어 있습니다.

```text
k8s/
├─ base
│  ├─ app.yaml
│  ├─ postgres.yaml
│  ├─ redis.yaml
│  ├─ configmap.yaml
│  ├─ secret.example.yaml
│  └─ ingress.yaml
└─ overlays/aws
   ├─ configmap.aws.patch.yaml
   ├─ ingress.aws.patch.yaml
   └─ secret.aws.example.yaml
```

로컬/기본 배포와 AWS 배포 오버레이를 분리해 환경별 설정 차이를 Kustomize patch로 관리합니다.

## 개발 과정에서 중점적으로 다룬 문제

- 단일 CRUD 앱을 넘어 인증, 캐시, 검색, AI, 배포까지 포함한 서비스 경계 설계
- JPA 저장소와 Elasticsearch 검색을 포트로 분리해 장애 시 fallback 가능한 구조 구성
- Redis 캐시 키와 무효화 범위를 사용자/조회모드 단위로 설계
- 세션 인증과 JWT 인증을 함께 지원해 mk2 auth-service와 연동 가능한 구조 구현
- 파일 업로드 보안 검증, PDF 텍스트 추출, 원본 다운로드 흐름 구현
- 테스트 가능한 계층 분리를 위해 도메인/유스케이스/어댑터 책임을 분리

## 관련 문서

- `docs/architecture-draft.md`: 초기 아키텍처 설계
- `docs/mvp-v1.md`: MVP 범위
- `docs/LEARNING_GUIDE.md`: 학습/설계 메모
- `docs/ec2-docker-compose.md`: EC2 Docker Compose 배포 메모
- `docs/secrets-commit-checklist.md`: 민감정보 커밋 방지 체크리스트
- `k8s/README.md`: Kubernetes 적용 방법
