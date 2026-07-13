# A4a executionInputFiles Implementation Prompt

## Document Control

- status: Approved
- canonical for: Reference only — implementation task prompt
- owner: BE
- source documents:
  - `AGENTS.md`
  - `docs/templates/codex-standard-task-prompt.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-api-inventory.md`
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
  - `docs/work-notes/2026/2026-07-13-a10-current-state-contract-merge.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 없음
- last synced: 2026-07-13
- open questions:
  - 없음

## Orchestrator Decision

1. Request classification: `implementation-ready task`
2. Recommended skill path:
   - `spring-api-implementation`
   - 구현 완료 후 별도 새 세션에서 `code-review`
3. Immediate next artifact: A4a `executionInputFiles` production implementation과 검증 결과
4. Why this order fits:
   - API response, 상태별 노출 규칙, 파일 조회 anchor, 불완전 파일 처리 정책이 canonical feature spec에 확정되어 있다.
   - 신규 domain/schema/DDL 변경이 없어 model-design 또는 ERD 단계가 필요하지 않다.
5. Blocking questions / assumptions:
   - blocking question 없음. 다만 독립 A10 endpoint의 실제 외부 사용 증거가 발견되면 제거 전에 중단하고 사용자에게 알린다.
6. Completion boundary:
   - A4a 응답이 계약대로 `executionInputFiles`를 반환하고, 독립 A10 외부 mapping과 stale 구현을 정리하며, 관련 테스트와 compile이 통과하면 완료다.
7. Source-of-truth assignment:
   - API contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
   - Business rules / lifecycle: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
   - Schema / DBA rollout: 변경 없음
   - External evidence: 없음
   - Execution record: 구현용 work note

> 🔀 Cross-validation recommended: 구현 계획을 작성한 뒤 동일 요청을 새 Codex CLI 세션에서 독립 검토하고, scope·contract·risk 차이가 있으면 구현 전에 work note에 결론을 남긴다.

## Copy-Paste Prompt

```text
AGENTS.md와 docs/templates/codex-standard-task-prompt.md를 따르라.
이번 작업은 `spring-api-implementation` 스킬을 사용한다.

목표:
- `GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals` A4a current-state 응답에 canonical 계약의 nullable `executionInputFiles`를 구현한다.
- `currentState=EXECUTED_CONFIRMED`일 때만 해당 execution-preparation의 `caseId`에 연결된 실행 input file metadata를 반환한다.
- 최신 계약에서 미생성으로 확정된 독립 `GET .../model-executions/latest/input-files` 외부 endpoint와 stale 구현을 정리한다.

Source of truth:
- API contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - §5.2.1 current-state 조회/상태 매핑
  - §5.7 A4a 성공 실행 input Excel 파일 projection
  - §10 DTO pseudo-code의 `CurrentModelParamProposalRes` / `ModelExecutionInputFilesRes`
  - §11 A4a 불완전 input file error/response 정책
  - §14 2026-07-13 A10→A4a 병합 change log
- API inventory: `docs/feature-specs/mi-model-parameter-proposal-backend-api-inventory.md`
  - A4a, A10, A11, A12 구분
- Business rule / lifecycle: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- Contract merge execution record: `docs/work-notes/2026/2026-07-13-a10-current-state-contract-merge.md`
- Schema / DDL: 없음. 기존 `tcm_filel`의 `SCNRCASE + caseId` 관계를 재사용하며 schema를 변경하지 않는다.
- External evidence: 없음
- Work note: create/update `docs/work-notes/2026/2026-07-13-model-param-proposal-execution-input-files-implementation.md`

현재 상태:
- 구현 상태: 일부 구현
  - A4a Controller mapping, current-state 조회, Case/Run 상태 매핑, `CurrentModelParamProposalRes`, 상태별 테스트는 존재한다.
  - `CurrentModelParamProposalRes`에는 아직 `executionInputFiles`가 없다.
  - `ModelParamProposalServiceImpl`은 파일 projection을 조립하지 않는다.
  - 구 계약의 독립 `GET .../model-executions/latest/input-files`, `LatestModelExecutionInputFilesRes`, service/delegator/test 구현이 남아 있다.
- 테스트 상태: 정상. 기존 관련 테스트는 통과하지만 구형 response shape와 독립 A10 endpoint를 검증한다.
- DB rollout 상태: 없음
- Controller profile: 기존 A4a와 동일하게 `@Profile("!dedicated")` 경계를 유지한다.

범위:
- 포함:
  - `CurrentModelParamProposalRes`에 nullable `executionInputFiles` 필드 추가
  - canonical shape의 `ModelExecutionInputFilesRes` 구현
    - `boolean complete`
    - non-null `List<String> missingFileRoles`
    - non-null `List<ModelExecutionInputFileRes> files`
  - 기존 `ModelExecutionInputFileRes(fileRole, fileNo, fileNm, fileSize)` 재사용
  - A4a가 이미 선택한 최신 non-deleted execution-preparation의 `caseId`를 파일 조회 anchor로 사용
  - `currentState=EXECUTED_CONFIRMED`에서만 `tcm_filel(SCNRCASE + caseId)` input file 조회
  - `EXECUTED_CONFIRMED` 외 모든 상태에서 `executionInputFiles=null` 반환 및 파일 repository 미호출
  - `DEMAND`, `SUPPLY`, `TRADE` 고정 role 순서 유지
  - 정상/부분 누락/전체 누락 projection 및 데이터 불완전 warning log
  - 독립 `/model-executions/latest/input-files` Controller mapping 제거
  - 독립 endpoint 전용 service interface/implementation/delegator method와 stale DTO/test 정리
  - 기존 독립 A10 파일 분류 로직을 A4a 내부 projection으로 이동 또는 작은 전용 reader/projector로 추출해 중복 없이 재사용
  - Swagger schema/description과 직렬화 테스트 갱신
  - 상태별 service test와 최소 controller/DTO test 갱신
- 제외:
  - A4a 이외 current-state/business lifecycle 변경
  - A8/A9 latest parameter resolver 변경
  - A6 JSON/multipart 실행 접수 변경
  - Agent request/callback/SSE 변경
  - 공통 `POST /api/v1/file-management/download/file` 변경
  - file binary streaming 또는 presigned URL 추가
  - schema/DDL/entity column/index 변경
  - historical work note bulk edit

구현 규칙:
1. 구현 전에 work note의 `## Execution Plan`을 먼저 작성한다. planned files, scope, 예상 위험을 명시한다.
2. 현재 작업 트리의 사용자 변경을 보존하고 관련 없는 파일을 수정하지 않는다.
3. 가장 가까운 local pattern부터 확인한다:
   - `ModelParamProposalController#getCurrentModelParamProposal`
   - `ModelParamProposalServiceImpl#getCurrentProposal`
   - `ModelParamProposalServiceImpl#toCaseLinkedCurrentProposalResponse`
   - `CurrentModelParamProposalRes`
   - 현재 `ScenarioModelExecutionServiceImpl#getLatestInputFiles` 파일 role projection
   - current-state service/serialization tests
4. canonical response 순서를 따른다. `executionInputFiles`는 `modelFiles` 다음, `failureReason` 전에 둔다.
5. nested `ModelExecutionInputFilesRes`에는 `scenarioId`와 `caseId`를 넣지 않는다. 두 값은 A4a top-level에 이미 존재한다.
6. 노출 gate의 canonical 기준은 계산된 `currentState=EXECUTED_CONFIRMED`다. 현재 매핑상 `modelExecutionStatus=COMPLETED`와 일치하지만 raw status만으로 별도 latest Case를 다시 resolve하지 않는다.
7. A4a가 선택한 execution-preparation의 `caseId`를 그대로 사용한다. `LatestModelParamProposalExecutionContextResolver` 또는 scenario 전체 latest Case resolver를 호출하지 않는다.
8. `EXECUTED_CONFIRMED`가 아니면 파일 repository를 호출하지 않는다. 파일 존재 여부로 노출 상태를 넓히지 않는다.
9. `complete=true`는 `DEMAND`/`SUPPLY`/`TRADE`가 각각 정확히 1개일 때만 가능하다.
10. `missingFileRoles`와 `files`는 non-null array이며 role 순서는 `DEMAND`, `SUPPLY`, `TRADE`다.
11. 일부/전체 누락은 HTTP 200을 유지한다:
    - 일부 누락: `complete=false`, 누락 role 목록, 존재하는 파일만 반환
    - 전체 누락: `complete=false`, `missingFileRoles=[DEMAND,SUPPLY,TRADE]`, `files=[]`
    - 정상 write 결과로 간주하지 말고 scenarioId/caseId/누락 정보를 warning log로 남긴다.
12. 기존 A10 구현의 duplicate/invalid filename 처리 방식은 canonical 계약과 충돌하지 않는 범위에서 보존한다. 새로운 오류 코드나 409/500 변환을 만들지 않는다.
13. 독립 A10 endpoint 제거 전 repo callsite와 active 문서를 검색한다. canonical 문서/FE 가이드는 호출 금지 상태다. 실제 외부 사용 증거 또는 source-of-truth 충돌이 발견되면 제거하지 말고 중단 후 사용자에게 알린다.
14. API contract를 재정의하지 않는다. 구현 중 계약 변경이 필요해지면 production code를 계속 수정하지 말고 canonical feature spec 문제로 escalation한다.
15. schema 영향은 없다. DDL/ERD 파일을 수정하지 않는다.

예상 변경 지점:
- `src/main/java/com/sk/domain/mi/dto/response/nextgen/CurrentModelParamProposalRes.java`
- 신규 또는 rename된 `src/main/java/com/sk/domain/mi/dto/response/nextgen/ModelExecutionInputFilesRes.java`
- 기존 `src/main/java/com/sk/domain/mi/dto/response/nextgen/ModelExecutionInputFileRes.java` 재사용
- `src/main/java/com/sk/domain/mi/service/nextgen/ModelParamProposalServiceImpl.java`
- 필요 시 작은 input-file read/projector component 및 focused unit test
- `src/main/java/com/sk/domain/mi/controller/nextgen/ScenarioModelExecutionController.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionService.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImpl.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/mock/ScenarioModelExecutionServiceDelegator.java`
- stale `LatestModelExecutionInputFilesRes`와 독립 A10 전용 테스트
- `src/test/java/com/sk/domain/mi/service/nextgen/ModelParamProposalServiceImplTest.java`
- `src/test/java/com/sk/domain/mi/service/nextgen/CurrentModelParamProposalSerializationTest.java`
- 관련 Controller/DTO focused tests

Acceptance criteria:
- Given A4a current-state가 `EXECUTING`/`modelExecutionStatus=RUNNING`일 때,
  when current-state API를 조회하면,
  then `executionInputFiles=null`이고 file repository는 호출되지 않는다.
- Given A4a current-state가 `EXECUTED_CONFIRMED`/`modelExecutionStatus=COMPLETED`이고 파일 3개가 완전할 때,
  when 조회하면,
  then `executionInputFiles.complete=true`, `missingFileRoles=[]`, `files`가 DEMAND/SUPPLY/TRADE 순서로 반환된다.
- Given `EXECUTED_CONFIRMED`인데 일부 파일이 누락됐을 때,
  when 조회하면,
  then HTTP 200, `complete=false`, 고정 순서의 `missingFileRoles`, partial `files`를 반환하고 warning log를 남긴다.
- Given `EXECUTED_CONFIRMED`인데 모든 파일이 없을 때,
  when 조회하면,
  then HTTP 200, `complete=false`, `missingFileRoles=[DEMAND,SUPPLY,TRADE]`, `files=[]`를 반환한다.
- Given A4a current-state가 `EXECUTION_FAILED`/`modelExecutionStatus=FAILED`이거나 proposal/execution 전 단계 상태일 때,
  when 조회하면,
  then `executionInputFiles=null`이고 file repository는 호출되지 않는다.
- Given A4a JSON을 직렬화할 때,
  when 완료 상태와 비완료 상태를 각각 직렬화하면,
  then `executionInputFiles`가 canonical nested shape 또는 explicit null로 나타나고 기존 top-level 필드는 유지된다.
- Given application mapping을 검사할 때,
  when Controller/API surface를 확인하면,
  then 독립 `GET .../model-executions/latest/input-files`는 더 이상 노출되지 않는다.
- 기존 A4a 상태 매핑, groupedRationales, A8/A9, A6 실행 접수 동작은 회귀하지 않는다.

검증 순서:
1. 변경 직후 가장 작은 DTO/서비스 테스트부터 실행한다.
2. 최소 권장 명령:
   - `./gradlew compileJava -x test`
   - `./gradlew test -PforceTest --tests com.sk.domain.mi.service.nextgen.ModelParamProposalServiceImplTest --tests com.sk.domain.mi.service.nextgen.CurrentModelParamProposalSerializationTest --tests com.sk.domain.mi.controller.nextgen.ScenarioModelExecutionControllerTest --tests com.sk.domain.mi.service.nextgen.ScenarioModelExecutionServiceImplTest --tests com.sk.domain.mi.dto.response.nextgen.LatestModelExecutionResponseDtoTest`
   - stale test/class를 rename 또는 제거했다면 위 filter를 실제 최종 class명에 맞춰 조정하고 결과를 work note에 정확히 기록한다.
   - `git diff --check`
3. focused verification이 통과한 뒤 영향 범위가 넓어졌다면 관련 nextgen test를 추가 실행한다.
4. 실제 DB/Object Storage smoke는 환경이 없으면 skipped reason과 residual risk를 work note에 남긴다.

완료 조건:
- canonical A4a response에 `executionInputFiles`가 구현되어 있다.
- 상태별 null/non-null 및 no-query 정책이 테스트로 고정되어 있다.
- 완전/부분/전체 누락 파일 projection이 테스트로 고정되어 있다.
- 독립 A10 외부 endpoint와 stale public contract가 정리되어 있다.
- compile과 focused tests가 통과한다.
- work note에 changed files, verification command/result, skipped checks, residual risks, plan 대비 실제 결과를 기록한다.
- 구현 완료 후 같은 세션 self-review를 최종 gate로 삼지 말고, 별도 새 세션에서 `code-review`를 실행한다.

기대 산출물:
- implementation
```
