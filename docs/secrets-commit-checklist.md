# Secrets Commit Checklist

민감정보(API 키, DB 비밀번호, JWT secret)가 Git에 올라가지 않도록 커밋 전에 확인하는 체크리스트입니다.

## 60초 빠른 점검

- [ ] `.env`에만 실제 시크릿이 있고, `.env.example`에는 빈값/placeholder만 있다.
- [ ] 코드/스크립트(`.kt`, `.ps1`, `.yml`)에 시크릿 하드코딩이 없다.
- [ ] `git diff --cached`에 `KEY`, `SECRET`, `TOKEN`, `PASSWORD` 값이 노출되지 않는다.
- [ ] 노출이 의심되면 커밋을 멈추고 키를 교체(rotate)한다.

## 이 저장소 기준 규칙

- 실제 시크릿은 `.env` 또는 OS 환경변수에만 둡니다.
- `.env.example`은 템플릿 파일이므로 실제 값을 넣지 않습니다.
- `POS_JWT_SECRET`, `GEMINI_API_KEY`, `POS_DB_PASSWORD`, `POS_REDIS_PASSWORD`는 실제값 커밋 금지입니다.
- `bootRun.ps1` 같은 실행 스크립트에 기본 시크릿을 하드코딩하지 않습니다.

## 커밋 직전 명령어

```powershell
Set-Location "D:\dev\personal-operating-system"
git status --short
```

```powershell
Set-Location "D:\dev\personal-operating-system"
git --no-pager diff --cached
```

```powershell
Set-Location "D:\dev\personal-operating-system"
git --no-pager grep -nE "(API[_-]?KEY|SECRET|TOKEN|PASSWORD|POS_JWT_SECRET|GEMINI_API_KEY)=" -- ":(exclude).env" ":(exclude).env.example"
```

## pre-commit 자동 차단 설정

```powershell
Set-Location "D:\dev\personal-operating-system"
.\scripts\install-git-hooks.ps1
```

설치 후 확인:

```powershell
Set-Location "D:\dev\personal-operating-system"
git config --get core.hooksPath
```

- 출력이 `.githooks`면 활성화 완료입니다.
- 훅 스크립트 위치: `.githooks/pre-commit`, `scripts/pre-commit-secrets.ps1`

## 유출 발생 시 즉시 대응

1. 노출된 키/API 토큰/JWT secret을 즉시 재발급(rotate)합니다.
2. Git 추적 파일에서 값을 제거하고 커밋을 정정합니다.
3. 원격 저장소에 이미 올라갔으면 히스토리 정리(BFG/git filter-repo) 여부를 팀과 결정합니다.
4. 관련 시스템(백엔드, BFF, 배치)에 새 키를 반영하고 정상 동작을 확인합니다.

## 참고

- 앱 설정: `src/main/resources/application.yaml`
- 실행 스크립트: `bootRun.ps1`
- 환경변수 템플릿: `.env.example`
- 실제 로컬 환경변수: `.env` (Git ignore 대상)

