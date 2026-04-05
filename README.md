# personal-operating-system

AI 기반의 개인용 지식 관리 및 업무 자동화 시스템입니다.

## 프로젝트 방향

이 프로젝트는 두 가지 목표를 동시에 달성합니다:

- 이력서에 포트폴리오로 첨부할 수 있는 데모급 프로젝트
- 개인의 일상적인 지식 관리와 메모 운영에 장기적으로 사용할 시스템

## 주요 기술 스택

- Kotlin, Spring Boot, JPA
- PostgreSQL, Redis
- Thymeleaf (UI)
- JUnit (테스트)

## 현재 구현 상태

Clean Architecture 계층, PostgreSQL 저장 기반, Thymeleaf UI가 준비되었습니다:

### REST API
- `POST /api/v1/notes` - 노트 생성
- `GET /api/v1/notes/{id}` - 노트 조회
- `GET /api/v1/notes/search?keyword=...` - 노트 검색
- `PUT /api/v1/notes/{id}` - 노트 수정
- `DELETE /api/v1/notes/{id}` - 노트 삭제

### Thymeleaf UI
- `GET /login` - 로그인 페이지 (세션 기반 폼 로그인)
- `GET /notes` - 노트 목록 (검색, 북마크 필터 포함)
- `GET /notes/new` - 노트 작성 폼
- `GET /notes/{id}` - 노트 상세
- `GET /notes/{id}/edit` - 노트 수정 폼
- `POST /notes/{id}/delete` - 노트 삭제
- `POST /notes/{id}/bookmark` / `unbookmark` - 북마크 토글

### 인증
- 로그인 방식: Spring Security 세션 로그인
- 사용자 소스: DB(users 테이블) 기반
- 초기 계정(seed): 앱 시작 시 없으면 자동 생성
  - username: `pos-admin`
  - password: `pos-admin1234`
- 보호 경로: `/notes/**`
- 공개 경로: `/login`, `/api/**`, 정적 리소스

### 인프라
- 계층화된 패키지 구조 (`domain`, `application`, `adapter`, `infrastructure`)
- `JpaNotePersistenceAdapter`를 통한 PostgreSQL 저장/조회
- `note_tags` 분리 테이블 기반 태그 저장
- TDD 기반 단위 테스트 + 웹 통합 테스트

## 기획 문서

- `docs/mvp-v1.md` - MVP 범위 및 완료 기준
- `docs/architecture-draft.md` - 아키텍처 계층 설명
- `docs/sprint-1-plan.md` - 1주차 작업 계획
- `docs/LEARNING_GUIDE.md` - **← 개발 방식/설계 패턴 상세 설명** (필독!)
- `docs/postgresql-로컬-실행.md` - **← PostgreSQL로 실제 저장해보는 방법**

## 테스트 실행

```powershell
.\gradlew.bat test
```

## 로그인 계정 변경

환경변수로 초기 계정(seed) 정보를 바꿀 수 있습니다.

> 여기서 `seed`는 CSS 프레임워크 Bootstrap이 아니라,
> "앱 시작 시 초기 데이터를 넣는 시드 작업"을 의미합니다.

```powershell
$env:POS_SECURITY_USERNAME = "my-admin"
$env:POS_SECURITY_PASSWORD = "my-secret-password"
$env:POS_SECURITY_ROLE = "ROLE_USER"
.\gradlew.bat bootRun
```

## 환경변수 설정 방법 (PowerShell)

`$env:KEY="value"` 와 `setx KEY "value"`는 용도가 다릅니다.

- `$env:...` : 현재 터미널 세션에서만 유효 (창 닫으면 사라짐)
- `setx ...` : 사용자 환경변수로 영구 저장 (재부팅/새 터미널에도 유지)

즉, 아래처럼 이해하면 됩니다.

- 빠르게 1회 테스트: `$env:...`
- 매번 입력하기 귀찮음: `setx ...`

### 1) 세션용(임시) 설정

현재 PowerShell 창에서만 적용됩니다.

```powershell
$env:GEMINI_API_KEY = "AIza...실제키..."
$env:POS_AI_PROVIDER = "gemini"
.\gradlew.bat bootRun
```

요약 화면(`/summary`)에서는 모델을 라디오 버튼으로 선택할 수 있습니다.

- `Flash` (기본 선택)
- `Pro`

이 선택은 요청마다 적용되므로, 모델 변경을 위해 서버 재기동할 필요가 없습니다.

> 이 방식은 PC 재부팅 또는 터미널 종료 후 다시 설정해야 합니다.

### 2) 영구 설정 (`setx`)

한 번 저장해두면 새 터미널부터 자동으로 적용됩니다.

```powershell
setx GEMINI_API_KEY "AIza...실제키..."
setx POS_AI_PROVIDER "gemini"
```

적용 확인/실행:

```powershell
# 새 PowerShell 창을 연 뒤 실행
echo $env:GEMINI_API_KEY
echo $env:POS_AI_PROVIDER
.\gradlew.bat bootRun
```

### 주의사항

- `setx`는 "현재 창"에는 즉시 반영되지 않습니다. 새 터미널을 열어야 합니다.
- API 키는 절대 코드나 `application.yaml`에 직접 쓰지 말고 환경변수로만 관리하세요.
- 실수로 키가 유출되면 즉시 해당 키를 폐기(revoke)하고 새 키를 발급하세요.

### Gemini 모델명 커스터마이즈 (선택)

기본 매핑:

- `Flash` -> `gemini-3-flash-preview`
- `Pro` -> `gemini-3.1-pro-preview`

원하면 환경변수로 바꿀 수 있습니다.

```powershell
$env:GEMINI_FLASH_MODEL = "gemini-3-flash-preview"
$env:GEMINI_PRO_MODEL = "gemini-3.1-pro-preview"
```

## PostgreSQL로 실제 저장해보기

기본 실행 설정은 PostgreSQL 기준으로 작성되어 있습니다.

1. 환경파일 준비 (`.env`는 Git에 올라가지 않음)

```powershell
Copy-Item .env.example .env
```

2. PostgreSQL 준비
   - 직접 설치하거나
   - Docker Desktop이 있다면 루트의 `compose.yaml` 사용
3. 애플리케이션 실행

```powershell
.\bootRun.ps1
```

4. 자세한 실행 방법은 `docs/postgresql-로컬-실행.md` 문서 참고

## 시작하기

이 프로젝트를 이해하려면:

1. `docs/LEARNING_GUIDE.md`를 읽어 전체 구조와 설계 원칙 파악
2. 각 레이어의 코드를 위에서 아래로(컨트롤러 → 서비스 → 도메인) 읽기
3. 테스트 코드를 보며 각 계층의 책임 확인
4. `docs/postgresql-로컬-실행.md`를 보며 인메모리 저장소와 PostgreSQL 저장소 차이 이해

