# 가격전망해석 수급모델 실행 직전 Soft-delete 구현 프롬프트

## Document Control

- status: Approved
- canonical for: Reference only
- owner: BE
- source documents:
  - `docs/templates/codex-standard-task-prompt.md`
  - `docs/design-docs/gas/price-forecast-interpretation-domain-design.md`
  - `docs/design-docs/gas/price-forecast-interpretation-erd-ddl-draft.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 본 문서의 options/사용자 확인/사전검증 포함 버전
- last synced: 2026-07-14
- open questions: 없음

## 복붙용 구현 프롬프트

```text
AGENTS.md를 따르라.

현재 작업은 `feature/gas2` 브랜치의 implementation-ready feature slice다.
먼저 `feature-delivery-orchestrator`로 현재 정본과 구현 readiness만 확인한 뒤 `spring-api-implementation` skill로 구현하라.
구현 완료 후 최종 code review는 별도 새 세션의 `code-review` skill로 수행할 수 있도록 changed files와 검증 명령을 남겨라.

목표:
- multipart/JSON 수급모델 실행 경로가 합류하는 최종 실행 등록 transaction에서 대상 Scenario의 active 가격전망해석 row를 조회한다.
- active row가 있으면 Case/Run 생성 직전에 `MiScenarioPriceInterp#softDelete()`를 호출하고, 없으면 no-op으로 기존 실행 등록을 계속한다.
- 가격전망해석 soft delete와 Case/file relation/Run/preparation 등록을 같은 transaction에서 commit/rollback한다.

Source of truth:
- Business rule / lifecycle: `docs/design-docs/gas/price-forecast-interpretation-domain-design.md` 4절, 6절, 8.2절, 9절
- API contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` 5.5절, 5.8절
- Scenario execution lifecycle: `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md` F4
- Schema / DDL: `docs/design-docs/gas/price-forecast-interpretation-erd-ddl-draft.md` (functional unique DB 적용 완료)
- Work note: create/update `docs/work-notes/2026/2026-07-14-price-interpretation-model-rerun-soft-delete-implementation.md`
- work note는 execution record이며 API/business/schema source of truth가 아니다.

범위:
- 포함:
  - 구현 전에 work note의 `## Execution Plan`을 먼저 작성한다.
  - `MiScenarioPriceInterpRepository`에 path `scenarioId` 기준 active row 단건 write-lock 조회를 추가한다.
  - `PriceInterpretationStore`에 수급모델 실행 전용 soft-delete mutation을 추가한다.
  - `ScenarioExecutionRegistrar#registerStaged`에서 Case 생성 직전에 위 mutation을 한 번 호출한다.
  - active row 있음/없음, 대상 Scenario 한정 삭제, Case 생성 전 호출 순서, transaction 경계를 검증하는 focused test를 추가·수정한다.
- 제외:
  - `model-execution-options` 응답 변경
  - 가격전망해석 존재 여부 조회 API 또는 팝업 판단 field 추가
  - multipart/JSON request에 확인값 추가
  - 사용자 확인, 사전검증, file staging 전 검증
  - 확인 전용 409 error code 또는 `ResultCode` 추가
  - Controller, request/response DTO, `ScenarioModelExecutionService` signature 변경
  - 가격전망해석 생성 횟수 제한, Agent request/callback, stale callback lifecycle 변경
  - `softDeleteByGroup` 동작 변경 또는 그룹 전체 삭제
  - JPA legacy unique metadata 정리, 새 table/column/index/constraint, DDL 실행
  - Kubernetes Job submit/callback/timeout 변경
  - unrelated refactor, 사용자 요청 없는 staging/commit/push

현재 상태:
- branch: 반드시 `feature/gas2`인지 확인한다. 다르면 구현하지 말고 보고한다.
- DB rollout: active row만 제한하고 deleted row 다건을 허용하는 functional unique 적용 완료.
- production Java: 가격전망해석 자동 soft-delete와 registrar 연결은 미구현.
- 두 실행 경로는 이미 final `TransactionOperations` callback에서 `ScenarioExecutionRegistrar#registerStaged`로 합류한다.
- worktree에는 본 기능 문서와 unrelated Kubernetes Java 변경이 함께 존재한다. 모든 기존 변경을 사용자 소유로 취급하고 덮어쓰거나 되돌리지 않는다.
- 기존 staged/unstaged 상태를 임의로 변경하지 않는다.

먼저 확인할 파일:
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImpl.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/execution/ScenarioExecutionRegistrar.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/priceinterp/PriceInterpretationStore.java`
- `src/main/java/com/sk/domain/shared/repository/master/tmm/MiScenarioPriceInterpRepository.java`
- `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenarioPriceInterp.java`
- `src/test/java/com/sk/domain/mi/service/nextgen/execution/ScenarioExecutionRegistrarTest.java`
- 가격전망해석 store의 가장 가까운 기존 테스트. 없으면 `PriceInterpretationStoreTest`를 추가한다.

구현 규칙:
1. 작업 시작 시 `git branch --show-current`, `git status --short`, 관련 diff를 확인하고 기존 사용자 변경을 보존한다.
2. repository에 scenario 단위 active row 조회를 추가한다.
   - 조건은 path `scenarioId`와 `del_yn='N'`에 해당하는 entity 상태다.
   - 반환형은 `Optional<MiScenarioPriceInterp>`다.
   - `@Lock(LockModeType.PESSIMISTIC_WRITE)` 또는 프로젝트의 동등한 JPA write-lock pattern을 사용한다.
   - functional unique로 active row는 최대 1건이다.
3. `PriceInterpretationStore`에 의미가 명확한 모델 실행 전용 메서드를 추가한다. 예: `softDeleteForModelExecution(Long scenarioId)`.
   - `@Transactional(propagation = Propagation.MANDATORY)`로 외부 final transaction 참여를 강제한다.
   - repository의 active row write-lock 조회 결과가 있으면 `MiScenarioPriceInterp#softDelete()`를 호출한다.
   - row가 없으면 예외 없이 no-op이다.
   - 별도 `save()`가 불필요한 managed entity dirty checking 패턴이면 기존 패턴을 따른다.
   - group id나 actor 조건을 추가하지 않는다.
4. `ScenarioExecutionRegistrar`에 `PriceInterpretationStore`를 주입한다.
   - `registerStaged`가 이미 `MANDATORY` final transaction에 참여하는 구조를 유지한다.
   - `scenario.getScenarioId()`로 모델 실행 전용 soft-delete 메서드를 호출한다.
   - 호출 위치는 actor 정보 조회가 성공한 뒤, `createRunningScenarioCase(...)` 호출 직전이다.
   - multipart/JSON service branch에는 삭제 코드를 중복 추가하지 않는다.
5. 같은 transaction의 의미를 보존한다.
   - soft delete 실패 시 Case/Run을 만들지 않는다.
   - Case/file relation/Run/preparation 등록 실패 시 soft delete도 rollback된다.
   - commit 이후 Job dispatch 실패 시 기존 가격전망해석을 복원하는 코드는 추가하지 않는다.
6. 기존 `PriceInterpretationStore#softDeleteByGroup`은 그룹 삭제 cascade이므로 재사용하지 않는다.
7. 같은 Scenario Group의 다른 Scenario 가격전망해석을 조회하거나 삭제하지 않는다.
8. API request/response와 file staging 순서는 변경하지 않는다.

Acceptance criteria:
- active 가격전망해석 row 없음 → 삭제 동작 없이 기존 Case/Run 생성 흐름 성공.
- active 가격전망해석 row 있음 → Case 생성 전에 대상 row의 `del_yn='Y'` 처리 후 기존 Case/Run 생성 흐름 성공.
- `GENERATING`, `GENERATED`, `FAILED` 상태와 content/수정자에 관계없이 active row면 삭제 대상.
- 같은 그룹의 다른 Scenario row는 유지.
- 가격전망해석 조회/soft-delete에서 예외 발생 → Case 생성 repository가 호출되지 않음.
- soft delete 이후 Case/Run 등록이 실패 → 같은 transaction rollback으로 기존 row가 active 상태를 유지함. 실제 DB integration 검증이 어렵다면 `MANDATORY` 경계와 호출 순서를 unit test로 검증하고 residual risk를 work note에 기록.
- multipart/JSON 두 경로는 기존 공통 registrar 호출만으로 동일 동작하며 branch별 삭제 코드가 없음.
- options/DTO/controller/service signature/ResultCode/Kubernetes/JPA unique metadata/schema DDL에 diff가 없음.

권장 테스트:
- `PriceInterpretationStoreTest`
  - active row 있음: `softDelete()` 호출
  - active row 없음: no-op
  - scenarioId가 repository 호출에 그대로 전달됨
  - 모델 실행 전용 mutation의 `Propagation.MANDATORY` 경계
- `ScenarioExecutionRegistrarTest`
  - 가격전망해석 soft-delete 호출이 Case repository 저장보다 먼저 수행됨
  - soft-delete 예외 시 Case repository 미호출
  - 기존 성공 응답/Case/Run/file/preparation 동작 회귀 없음

검증 순서:
1. 시작 상태와 공통 실행 경계 확인
   - `git branch --show-current`
   - `git status --short`
   - `rg -n 'registerStaged\(|softDeleteByGroup|class PriceInterpretationStore|interface MiScenarioPriceInterpRepository' src/main/java src/test/java`
2. focused test
   - 실제 생성된 테스트 class명에 맞춰 아래 filter를 조정한다.
   - `./gradlew test -PforceTest --tests com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStoreTest --tests com.sk.domain.mi.service.nextgen.execution.ScenarioExecutionRegistrarTest`
3. compile gate
   - `./gradlew compileJava -x test`
   - `./gradlew compileTestJava`
4. 정적 범위 확인
   - `rg -n 'priceInterpretationResetRequired|priceInterpretationResetConfirmed|BIZ_PRICE_INTERP_RESET_CONFIRMATION_REQUIRED' src/main/java src/test/java`
   - 위 검색은 신규 구현이 없어야 한다. 기존 baseline 결과가 있다면 새 diff인지 구분해 기록한다.
   - `git diff --check`
   - `git status --short`
- 검증 실패 시 가장 작은 실패 명령부터 수정·재실행한다.
- unrelated baseline/user-owned 변경의 실패를 고치기 위해 범위를 확장하지 않는다.

완료 조건:
- production code와 focused tests가 Acceptance criteria를 충족한다.
- focused tests, `compileJava`, `compileTestJava`, `git diff --check` 결과를 기록한다.
- 실패하거나 생략한 gate는 원인과 영향 범위를 정확히 work note에 기록한다.
- work note 끝에 `## Actual Result`를 추가하고 changed files, transaction boundary, data access/lock 선택, 검증 결과, residual risk를 남긴다.
- staging/commit/push를 수행하지 않는다.
- 최종 응답에는 별도 `code-review` 세션에 전달할 changed files와 test command를 명시한다.

기대 산출물:
- scenario 단위 active 가격전망해석 write-lock repository query
- 모델 실행 전용 자동 soft-delete store mutation
- 공통 registrar의 Case 생성 전 mutation 호출
- focused regression tests
- implementation work note

최종 응답 형식:
1. Implementation summary
2. Changed components
3. Transaction / lock decision
4. Tests / verification status
5. Preserved user changes
6. Risks / follow-up
7. Separate code-review handoff
```
