# 파일 다운로드 암호화 정책 공통화 구현 프롬프트

## Document Control

- status: Approved
- canonical for: Reference only
- owner: BE
- source documents:
  - `AGENTS.md`
  - `docs/templates/codex-standard-task-prompt.md`
  - `docs/design-docs/gas/file-encryption-by-customer-design.md`
  - `docs/work-notes/2026/2026-07-15-file-download-encryption-condition-investigation.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - 외부 파일 암복호화 서비스 API 계약과 dedicated profile 환경변수 주입
- supersedes:
  - 없음
- last synced: 2026-07-15
- open questions: 없음

## 복붙용 구현 프롬프트

```text
AGENTS.md를 따르라.

이번 요청은 API/schema 계약을 바꾸지 않는 파일 다운로드 암호화 정책 공통화 refactor다.
`spring-api-implementation` skill을 사용하되 한 공통 File API slice만 최소 diff로 구현하라.
구현 완료 후 같은 세션의 self-review를 최종 merge gate로 간주하지 말고, 별도 새 세션에서 `code-review`를 수행할 수 있도록 changed files와 검증 명령을 남겨라.

목표:
- 단건 `POST /api/v1/file-management/download/file`과 ZIP `POST /api/v1/file-management/download`의 다운로드 암호화 여부를 `FileEncryptionPolicy.evaluateDownload(...)` 한 곳에서 결정한다.
- 공통 decision은 feature flag, 원본 파일 확장자, `Role.USER`, 유효 고객사 순서로 평가한다.
- 단건의 응답 전체 암호화와 ZIP의 entry별 암호화 실행 방식은 유지한다.
- 업로드 복호화 정책과 내부 평문 소비 경로는 변경하지 않는다.

Source of truth:
- Business rule / component boundary: `docs/design-docs/gas/file-encryption-by-customer-design.md` 1절, 3.2절, 3.5절, 3.6절, 6절, 8절
- API request/response: 기존 `FileDedicatedController` Swagger annotation과 `FileDownloadReq`. 이번 작업에서 변경하지 않는다.
- Schema / DDL: 변경 없음. 기존 `tcm_filel` metadata와 Object Storage 평문 저장 구조를 유지한다.
- Investigation / current-state evidence: `docs/work-notes/2026/2026-07-15-file-download-encryption-condition-investigation.md`
- Implementation work note: create/update `docs/work-notes/2026/2026-07-15-file-download-encryption-policy-unification-implementation.md`
- work note와 이 구현 프롬프트는 execution/reference artifact이며 business rule의 source of truth가 아니다.

범위:
- 포함:
  - 구현 전에 implementation work note의 `## Execution Plan`을 먼저 작성한다.
  - `DownloadEncryptionDecision`을 추가한다.
  - `FileEncryptionPolicy`에 단건/ZIP 공통 `evaluateDownload(Role role, String companyCd, String fileName)`를 추가한다.
  - `FileDownloadEncryptor`가 공통 decision으로 단건 다운로드 암호화 여부와 skip reason을 결정하도록 변경한다.
  - `FileDedicatedController#downloadFiles`가 현재 역할과 USER일 때의 유효 고객사 코드를 `FileService`에 전달하도록 service signature를 확장한다.
  - `FileService#downloadFiles`가 각 ZIP entry에 공통 decision을 적용한다.
  - 모든 production/test caller와 focused test를 새 signature/decision 계약에 맞춘다.
  - canonical design과 구현이 충돌하면 Java 구현을 중단하고 design conflict를 보고한다.
- 제외:
  - endpoint path, HTTP method, query/body, response header/body 변경
  - `@RequiredPermission` 역할 목록 변경
  - `FileDownloadReq`, `FileDownloadRes` shape 변경
  - `FileCryptoApiClient`의 upload/status/encrypt/decrypt/download 계약과 retry 변경
  - `file.encryption.*`, `external.file-crypto.*` 설정값 변경
  - 업로드 복호화 조건 또는 `FileEncryptionPolicy.isEnabled(companyCd, fileName)` 의미 변경
  - Object Storage 저장 형식, `tcm_filel`, entity/repository/schema/DDL 변경
  - `directDownloadFiles()` 및 별도 직접 생성 Excel 다운로드 범위 확대
  - unrelated refactor, 사용자 요청 없는 staging/commit/push

현재 상태:
- implementation target branch: `feature/gas2`. 시작 시 실제 branch를 확인하고 다르면 작업 전 보고한다.
- `FileDownloadEncryptor`는 feature flag → 확장자 → 역할 → 고객사 조건을 자체 `if`로 평가한다.
- ZIP controller는 `Role.USER`일 때만 company code를 전달하고, `FileService`는 `isEnabled(companyCd, fileName)`만 평가한다.
- 두 API의 실효 조건은 현재 같지만 역할 조건의 owner가 달라 향후 drift 위험이 있다.
- `FileService#downloadFiles(List<FileDownloadReq>)` 내부 overload는 null company context로 평문 ZIP을 만든다.
- worktree에는 이 기능의 interim controller/design/test/work-note 변경과 unrelated untracked work note가 함께 존재한다. 기존 staged/unstaged/untracked 상태를 모두 사용자 소유로 취급하고 덮어쓰거나 되돌리지 않는다.
- repository `.gitignore`의 `test/` 규칙 때문에 신규 test 파일은 일반 untracked 상태에 나타나지 않을 수 있다. 추적 필요 시 해당 파일만 확인하고 사용자의 기존 staging 범위를 변경하지 않는다.

먼저 확인할 파일:
- `src/main/java/com/sk/domain/shared/controller/FileDedicatedController.java`
- `src/main/java/com/sk/domain/shared/service/FileDownloadEncryptor.java`
- `src/main/java/com/sk/domain/shared/service/FileEncryptionPolicy.java`
- `src/main/java/com/sk/domain/shared/service/FileService.java`
- `src/main/java/com/sk/domain/shared/client/FileCryptoApiClient.java`
- `src/main/java/com/sk/domain/shared/service/permissions/AuthorizationService.java`
- `src/main/java/com/sk/domain/shared/enums/Role.java`
- `src/test/java/com/sk/domain/shared/controller/FileDedicatedControllerTest.java`
- `src/test/java/com/sk/domain/shared/service/FileDownloadEncryptorTest.java`
- `src/test/java/com/sk/domain/shared/service/FileServiceTest.java`
- `docs/design-docs/gas/file-encryption-by-customer-design.md`

구현 규칙:
1. 시작 시 `git branch --show-current`, `git status --short`, 관련 staged/unstaged diff를 확인한다.
2. `DownloadEncryptionDecision`은 다음 값을 가진다.
   - `ENCRYPT`
   - `FEATURE_DISABLED` (`feature_disabled`)
   - `EXTENSION_NOT_TARGET` (`extension_not_target`)
   - `ROLE_NOT_TARGET` (`role_not_target`)
   - `COMPANY_NOT_TARGET` (`company_not_target`)
   - enum은 `shouldEncrypt()`와 non-encrypt decision의 log용 skip reason을 제공한다.
   - 위치는 기존 shared enum package를 우선하되, local package convention과 충돌하면 가장 가까운 공통 package를 선택하고 work note에 이유를 기록한다.
3. `FileEncryptionPolicy#evaluateDownload(...)`를 추가한다.
   - 평가 순서는 반드시 feature flag → 확장자 → 역할 → 고객사다.
   - feature disabled면 `FEATURE_DISABLED`.
   - 원본 파일명이 target extension이 아니면 `EXTENSION_NOT_TARGET`.
   - `role != Role.USER` 또는 role null이면 `ROLE_NOT_TARGET`.
   - enabled company가 아니거나 companyCd null이면 `COMPANY_NOT_TARGET`.
   - 모두 만족하면 `ENCRYPT`.
   - 고객사/확장자 비교의 기존 대소문자 무시 동작을 유지한다.
   - 업로드가 사용하는 `isEnabled(companyCd, fileName)`과 `isEnabledCompany`, `isTargetExtension`, global `isEnabled` 의미를 바꾸지 않는다.
4. `FileDownloadEncryptor`를 공통 decision 소비자로 변경한다.
   - feature disabled/비대상 확장자일 때 인증·고객사 조회를 생략하는 기존 short-circuit 최적화는 유지할 수 있다.
   - 단, 실제 `FileCryptoApiClient.encrypt(...)` 호출 전 최종 조건은 반드시 `evaluateDownload(...) == ENCRYPT` 하나다.
   - `Role.USER` 여부로 최종 암호화 여부를 직접 결정하지 않는다. USER 여부는 company context 조회 필요성을 판단하는 데만 사용할 수 있다.
   - skip log reason은 decision의 canonical reason을 사용한다.
   - 암호화 실패를 평문으로 fallback하지 않고 기존 예외를 전파한다.
   - `ScenarioGroupDedicatedController`, `MiDataDetailDedicatedController` 등 기존 `FileDownloadEncryptor` caller 동작을 유지한다.
5. ZIP controller/service 경계를 변경한다.
   - controller는 현재 role을 조회한다.
   - USER일 때만 `getCompanyCdWithPriority()`로 companyCd를 조회하고, 그 외 역할은 null context를 사용한다.
   - `FileService#downloadFiles`에 role과 companyCd를 함께 전달한다.
   - controller는 `evaluateDownload`를 호출하거나 최종 암호화 boolean을 만들지 않는다.
6. `FileService#downloadFiles`는 entry별로 같은 decision을 사용한다.
   - 권장 signature: `downloadFiles(List<FileDownloadReq> list, Role role, String companyCd)`.
   - 내부 overload `downloadFiles(list)`는 `role=null`, `companyCd=null`로 위임해 인증 컨텍스트 없는 내부 호출이 기존처럼 평문 ZIP을 생성하도록 한다.
   - `decision.shouldEncrypt()`일 때만 entry 전체를 byte[]로 읽고 외부 암호화 API를 호출한다.
   - 비대상 entry는 기존 4KB streaming copy를 유지한다. 모든 entry를 무조건 메모리에 올리지 않는다.
   - ZIP container 전체를 암호화하지 않는다.
   - 대상 entry 하나의 암호화 실패도 ZIP 전체 실패로 전파하고 평문 fallback을 추가하지 않는다.
7. 기존 metadata 검증을 유지한다.
   - `fileNo`, `fileNm`, `fileSize`는 DB 평문 metadata 기준으로 검증한다.
   - 내부 staged file 차단, 누락/중복 요청 검증, 원본 entry filename을 변경하지 않는다.
8. API 권한 목록을 변경하지 않는다.
   - 단건은 기존 `ADMIN`, `OPERATOR`, `USER`, `API_PLUS_OPERATOR`, `API_SUB_ADMIN` 허용을 유지한다.
   - ZIP은 기존 `ADMIN`, `OPERATOR`, `USER` 허용을 유지한다.
   - 허용된 non-USER 요청은 다운로드는 가능하지만 암호화하지 않는다.

Acceptance criteria:
- `FileEncryptionPolicy.evaluateDownload`가 단건과 ZIP의 최종 암호화 decision을 모두 소유한다.
- `file.encryption.enabled=false` → 두 경로 모두 암호화하지 않음.
- 비대상 확장자 → 두 경로 모두 암호화하지 않음.
- `Role.ADMIN`, `Role.OPERATOR`, API role 또는 null role → 두 경로 모두 암호화하지 않음.
- `Role.USER` + 비대상 고객사 → 두 경로 모두 암호화하지 않음.
- `Role.USER` + enabled 고객사 + target Office 확장자 → 단건 전체 또는 해당 ZIP entry를 암호화.
- 고객사·확장자 비교는 대소문자 무시.
- ZIP 혼합 파일은 대상 Office entry만 암호화하고 나머지는 평문 entry로 유지.
- 내부 `downloadFiles(list)` overload는 외부 암호화 API를 호출하지 않음.
- 단건과 ZIP 모두 외부 암호화 오류 시 평문 fallback 없이 요청 실패.
- 업로드 복호화, Object Storage 평문 저장, `FileCryptoApiClient`, API request/response/permission, DB/schema에 회귀나 diff가 없음.
- `FileDedicatedController`에 최종 암호화 조건 조합이나 `FileCryptoApiClient` 호출이 없음.

필수 테스트:
- `FileEncryptionPolicyTest`
  - global disabled → `FEATURE_DISABLED`
  - 비대상 확장자 → `EXTENSION_NOT_TARGET`
  - `ADMIN`, `OPERATOR`, `API_PLUS_OPERATOR`, `API_SUB_ADMIN`, null role → `ROLE_NOT_TARGET`
  - USER + null/비대상 company → `COMPANY_NOT_TARGET`
  - USER + enabled company + target extension → `ENCRYPT`
  - company/extension 대소문자 무시
- `FileDownloadEncryptorTest`
  - global disabled/비대상 확장자는 인증·고객사·crypto client 미호출
  - non-USER는 company 조회와 crypto client 미호출
  - USER + 비대상 company는 crypto client 미호출
  - USER + 대상 company/extension은 crypto client 호출 및 암호문 반환
  - crypto client 예외는 평문 fallback 없이 전파
- `FileDedicatedControllerTest`
  - USER는 `downloadFiles(body, Role.USER, companyCd)` 호출
  - ADMIN 등 non-USER는 company 조회 없이 `downloadFiles(body, role, null)` 호출
- `FileServiceTest`
  - USER + 대상 company인 혼합 ZIP은 대상 entry만 암호화
  - non-USER + 대상 company라도 모든 entry 암호화 skip
  - 내부 null-context overload는 crypto client 미호출
  - 기존 missing/mismatched metadata 검증 회귀 없음

검증 순서:
1. 현재 분산 조건과 caller 확인
   - `git branch --show-current`
   - `git status --short`
   - `rg -n 'encryptForDownloadIfRequired|downloadFiles\(|isEnabled\(companyCd|Role.USER' src/main/java/com/sk/domain/shared src/test/java/com/sk/domain/shared`
2. policy focused test
   - `./gradlew test -PforceTest --tests com.sk.domain.shared.service.FileEncryptionPolicyTest`
3. 단건/ZIP 회귀 test
   - `./gradlew test -PforceTest --tests com.sk.domain.shared.service.FileEncryptionPolicyTest --tests com.sk.domain.shared.service.FileDownloadEncryptorTest --tests com.sk.domain.shared.service.FileServiceTest --tests com.sk.domain.shared.controller.FileDedicatedControllerTest`
4. compile gate
   - `./gradlew compileJava -x test`
   - `./gradlew compileTestJava`
5. 정적 범위 확인
   - `rg -n 'DownloadEncryptionDecision|evaluateDownload' src/main/java src/test/java`
   - `rg -n 'file-management/download|RequiredPermission' src/main/java/com/sk/domain/shared/controller/FileDedicatedController.java`
   - `git diff --check`
   - `git diff --cached --check`
   - `git status --short`
- 신규 test가 `.gitignore`에 가려지면 `git check-ignore -v <test-path>`로 원인을 확인한다. 자동 staging하지 말고 누락 위험과 정확한 파일 경로를 보고한다. 구현 세션에서 사용자가 명시적으로 staging을 요청한 경우에만 해당 파일을 한정해 처리한다.
- 실패 시 가장 작은 focused test부터 수정·재실행한다. unrelated baseline 실패를 해결하려고 범위를 확장하지 않는다.

완료 조건:
- 공통 decision과 단건/ZIP 소비 경로가 Acceptance criteria를 충족한다.
- focused behavioral tests, `compileJava`, `compileTestJava`, staged/unstaged `git diff --check`가 통과한다.
- 실패/생략 gate는 원인과 영향 범위를 implementation work note에 기록한다.
- work note에 changed files, 조건 평가 순서, short-circuit 보존 여부, ZIP streaming 보존, API/schema 비변경, verification, residual risk를 기록한다.
- work note 끝에 `## Actual Result`를 추가해 계획 대비 결과와 deviation 이유를 남긴다.
- staging/commit/push를 수행하지 않는다.
- 최종 응답에는 별도 `code-review` 세션에 전달할 changed files와 정확한 test command를 명시한다.

기대 산출물:
- `DownloadEncryptionDecision`
- 공통 `FileEncryptionPolicy.evaluateDownload(...)`
- 공통 decision을 소비하는 단건 `FileDownloadEncryptor`
- role/company context를 전달하고 공통 decision을 소비하는 ZIP controller/service 경로
- focused policy/단건/ZIP regression tests
- implementation work note

최종 응답 형식:
1. Implementation summary
2. Changed components
3. Policy / layering decision
4. Contract alignment
5. Security / tenant considerations
6. SQL / DBA implications
7. Tests / verification status
8. Work note update points
9. Risks / separate code-review handoff
```
