# A4a `executionInputFiles` 구현 프롬프트

## Document Control

- status: Approved
- canonical for: Reference only
- owner: BE
- source documents:
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

## 실행 프롬프트

```text
AGENTS.md를 따르라.

이번 요청은 feature-delivery-orchestrator 기준 `implementation-ready task`다.
`spring-api-implementation` skill을 사용해 한 API slice만 구현하라.
구현 완료 후 같은 세션에서 최종 code review를 대신하지 말고, 별도 새 세션에서 `code-review`를 실행할 수 있도록 changed files와 검증 결과를 남겨라.

목표:
- A4a `GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals` 응답에 canonical nullable `executionInputFiles` projection을 구현한다.
- A4a가 선택한 최신 execution-preparation의 `caseId`와 `EXECUTED_CONFIRMED` 상태를 anchor로 사용한다.
- 현재 독립 A10 `GET .../model-executions/latest/input-files`로 노출된 파일 projection을 A4a 내부 read projection으로 이동하고, 더 이상 유효하지 않은 외부 mapping과 전용 계약을 정리한다.

Source of truth:
- API contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`의 5.2.1, 5.7, 7절
- API inventory/reference: `docs/feature-specs/mi-model-parameter-proposal-backend-api-inventory.md`
- Schema / DDL: 변경 없음. 기존 `tcm_filel`의 `SCNRCASE + case_id` 관계를 재사용한다.
- Business rule / lifecycle: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- Superseding decision: `docs/work-notes/2026/2026-07-13-a10-current-state-contract-merge.md`
- External evidence: 없음
- Work note: create/update `docs/work-notes/2026/2026-07-13-model-param-proposal-execution-input-files-implementation.md`
- 위 work note는 execution record일 뿐 API/business/schema source of truth가 아니다.

범위:
- 포함:
  - 구현 전에 work note의 `## Execution Plan`을 먼저 작성한다.
  - `CurrentModelParamProposalRes`에 nullable `executionInputFiles`를 추가한다.
  - 중첩 response는 `complete`, non-null `missingFileRoles`, non-null `files`만 포함한다. A4a top-level에 이미 있는 `scenarioId`와 `caseId`를 중첩 object에 반복하지 않는다.
  - 현재 `LatestModelExecutionInputFilesRes`/`getLatestInputFiles`에 있는 DEMAND/SUPPLY/TRADE projection 규칙을 A4a에서 재사용 가능한 내부 service/helper 경계로 이동한다.
  - `ModelParamProposalServiceImpl`이 판정한 `currentState=EXECUTED_CONFIRMED`에서만 최신 execution-preparation row의 `caseId`로 파일을 조회한다.
  - `EXECUTED_CONFIRMED` 외 모든 상태는 `executionInputFiles=null`이며 `UploadedFileRepository` 파일 조회를 수행하지 않는다.
  - 독립 A10 controller mapping, service interface method, implementation entrypoint, mock delegator method와 stale DTO/import/test를 제거하거나 새 내부 projection 계약으로 rename/migrate한다.
  - A4a service/serialization 테스트로 상태 gate, case anchor, complete/partial/empty/duplicate file set과 non-null array 계약을 검증한다.
- 제외:
  - schema/DDL/entity/repository query signature 변경
  - A6 JSON model execution write flow
  - A8/A9 endpoint, parameter response, latest Case resolver 계약 변경
  - Agent request/confirmation/callback, SSE 변경
  - 공통 `POST /api/v1/file-management/download/file` 구현 또는 계약 변경
  - 파일 binary streaming, storage object 변경, live DB/Object Storage 데이터 변경
  - canonical feature spec/design/API inventory 재작성
  - unrelated refactor와 historical work note 일괄 수정

현재 상태:
- 구현 상태: 일부 구현. A4a current-state와 tree response는 구현되어 있으나 `CurrentModelParamProposalRes.executionInputFiles`는 없다.
- 파일 projection 상태: 독립 A10 형태의 `LatestModelExecutionInputFilesRes`와 `ScenarioModelExecutionServiceImpl.getLatestInputFiles()`에 complete/partial/empty/duplicate 처리 로직이 존재한다.
- stale 외부 mapping: `ScenarioModelExecutionController`의 `/model-executions/latest/input-files`, `ScenarioModelExecutionService`, `ScenarioModelExecutionServiceDelegator`와 관련 테스트가 남아 있다.
- 테스트 상태: 현재 구형 A10 및 A4a response shape 기준 테스트가 통과하므로 최신 canonical 계약에 맞게 이동/갱신해야 한다.
- DB rollout 상태: 없음. 이번 slice는 schema/DDL 비변경이다.

먼저 확인할 파일:
- `src/main/java/com/sk/domain/mi/dto/response/nextgen/CurrentModelParamProposalRes.java`
- `src/main/java/com/sk/domain/mi/dto/response/nextgen/LatestModelExecutionInputFilesRes.java`
- `src/main/java/com/sk/domain/mi/dto/response/nextgen/ModelExecutionInputFileRes.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ModelParamProposalServiceImpl.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionService.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImpl.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/mock/ScenarioModelExecutionServiceDelegator.java`
- `src/main/java/com/sk/domain/mi/controller/nextgen/ScenarioModelExecutionController.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/query/LatestModelParamProposalExecutionContextResolver.java`
- `src/test/java/com/sk/domain/mi/service/nextgen/ModelParamProposalServiceImplTest.java`
- `src/test/java/com/sk/domain/mi/service/nextgen/CurrentModelParamProposalSerializationTest.java`
- `src/test/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImplTest.java`
- `src/test/java/com/sk/domain/mi/controller/nextgen/ScenarioModelExecutionControllerTest.java`
- `src/test/java/com/sk/domain/mi/dto/response/nextgen/LatestModelExecutionResponseDtoTest.java`

구현 규칙:
- 먼저 위 source of truth와 관련 코드의 현재 diff를 읽고 사용자 소유 변경을 보존한다.
- 계약이나 실제 외부 소비 증거가 충돌하면 구현을 중단하고 보고한다. 특히 독립 A10 endpoint의 실제 외부 사용 증거가 발견되면 compatibility 결정을 임의로 내리지 않는다.
- A4a 파일 anchor는 `ModelParamProposalServiceImpl`이 이미 선택한 최신 execution-preparation의 `caseId`다. A8/A9용 `LatestModelParamProposalExecutionContextResolver`를 A4a 파일 조회에 호출하거나 최신 Case를 다시 선택하지 않는다.
- `currentState`를 먼저 판정한 뒤 정확히 `EXECUTED_CONFIRMED`일 때만 파일 projection을 만든다. 파일 존재 여부로 상태 allow-list를 확대하지 않는다.
- 파일 조회는 기존 `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(Math.toIntExact(caseId), "SCNRCASE")` local pattern을 재사용한다.
- role 순서는 `DEMAND`, `SUPPLY`, `TRADE`로 고정한다. 세 role이 각각 정확히 1개이고 미식별 파일이 없을 때만 `complete=true`다.
- 일부 또는 전체 파일 누락은 HTTP 200 response data로 표현한다. 누락은 `complete=false`, 고정 순서 `missingFileRoles`, partial/empty `files`로 반환하고 데이터 불완전 로그를 남긴다. 이를 409/500으로 바꾸지 않는다.
- 중복 role 또는 role 미식별 파일도 정상 complete로 오판하지 않는다. 기존 projection의 warning/logging과 고정 role ordering 의미를 보존하되 raw 내부 데이터나 storage 정보를 API error로 노출하지 않는다.
- `missingFileRoles`와 `files`는 projection이 non-null일 때 항상 non-null array다.
- 새 중첩 DTO는 MI response package가 소유한다. 독립 endpoint용 `scenarioId`/`caseId` wrapper DTO를 그대로 중첩해 canonical JSON shape를 어기지 않는다.
- A8/A9의 `LatestModelParamProposalExecutionContextResolver`, `getLatestConfirmedParameters`, `getLatestProposedParameters`와 관련 테스트는 유지한다.
- A4a controller path, permission annotation, `@Profile("!dedicated")`, `BaseResponse<T>` wrapper는 변경하지 않는다. 새 controller endpoint를 추가하지 않는다.
- 현재 독립 A10 API가 제거되면 Swagger mapping, service/delegator surface와 테스트도 함께 정리해 dead code와 API 이중 노출을 남기지 않는다.
- test source가 `.gitignore` 규칙에 가려지는지 확인하고, 존재 여부와 실행 결과를 work note에 기록한다. 사용자 승인 없이 staging 범위를 확대하지 않는다.
- 구현 중 새로운 API/business/schema 사실이 필요하면 work note나 코드 주석으로 임의 확정하지 말고 canonical 문서 충돌로 보고한다.

Acceptance criteria:
- A4a JSON top-level에 `executionInputFiles`가 존재한다.
- `EXECUTED_CONFIRMED` 응답의 `executionInputFiles`는 non-null이며 `{complete, missingFileRoles, files}` shape다. 중첩 object에는 `scenarioId`, `caseId`가 없다.
- A4a가 반환하는 top-level `caseId`와 파일 repository 조회에 사용한 case ID가 동일하다.
- `EXECUTED_CONFIRMED` 외 `NOT_REQUESTED`, `GENERATING`, `PROPOSAL_READY`, `CONFIRMED_PENDING_EXECUTION`, `EXECUTING`, `PROPOSAL_FAILED`, `EXECUTION_FAILED`에서는 `executionInputFiles=null`이다.
- 적어도 RUNNING/COMPLETED/FAILED case-linked 상태 테스트가 있으며, RUNNING과 FAILED에서는 file repository가 호출되지 않고 COMPLETED에서만 호출된다.
- 정상 3-file set은 `complete=true`, `missingFileRoles=[]`, `files`가 DEMAND/SUPPLY/TRADE 순서다.
- partial set은 `complete=false`, 누락 role 고정 순서, 존재 파일만 포함한다. empty set은 세 role 누락과 `files=[]`다.
- duplicate 또는 role 미식별 데이터는 `complete=false`이며 warning 대상이다.
- 독립 `/model-executions/latest/input-files` controller mapping이 production code에서 사라진다.
- `ScenarioModelExecutionService`와 delegator에 `getLatestInputFiles` public surface가 남지 않는다.
- `LatestModelExecutionInputFilesRes`가 더 이상 독립 endpoint wrapper로 남지 않는다. 필요 시 canonical 중첩 DTO로 rename/migrate하고 stale reference를 제거한다.
- A8/A9 endpoint와 resolver 동작, A4a tree/`groupedRationales`, A6 request shape에는 회귀가 없다.
- schema/DDL/entity와 공통 file download API에는 diff가 없다.

검증 순서:
1. 변경 전 상태 확인
   - `git status --short`
   - `rg -n 'latest/input-files|LatestModelExecutionInputFilesRes|getLatestInputFiles|executionInputFiles' src/main/java src/test/java`
2. 가장 작은 focused test부터 실행
   - `./gradlew test -PforceTest --tests com.sk.domain.mi.service.nextgen.ModelParamProposalServiceImplTest --tests com.sk.domain.mi.service.nextgen.CurrentModelParamProposalSerializationTest`
3. stale A10 cleanup과 인접 회귀 검증
   - `./gradlew test -PforceTest --tests com.sk.domain.mi.service.nextgen.ScenarioModelExecutionServiceImplTest --tests com.sk.domain.mi.controller.nextgen.ScenarioModelExecutionControllerTest --tests com.sk.domain.mi.service.nextgen.ScenarioModelExecutionSerializationTest`
4. compile gate
   - `./gradlew compileJava -x test`
   - `./gradlew compileTestJava`
5. 정적 계약 확인
   - `rg -n 'latest/input-files|LatestModelExecutionInputFilesRes|getLatestInputFiles' src/main/java src/test/java`
   - `rg -n 'executionInputFiles|EXECUTED_CONFIRMED|SCNRCASE' src/main/java/com/sk/domain/mi src/test/java/com/sk/domain/mi`
   - `git diff --check`
- 제거 또는 rename한 test class가 filter에 포함되면 최종 class명에 맞춰 명령을 조정하고, 실제 실행 명령과 이유를 work note에 기록한다.
- 검증 실패 시 원인을 수정하고 같은 최소 명령부터 재실행한다. 반복 실패나 unrelated baseline blocker는 숨기지 말고 중단·보고한다.

완료 조건:
- 위 acceptance criteria를 production code와 focused behavioral test로 충족한다.
- compileJava와 compileTestJava가 통과한다.
- stale 독립 A10 production mapping/public service surface가 남지 않는다.
- work note에 changed files, data access choice, 실제 검증 명령, pass/fail, skipped checks, schema/DDL 비변경, residual risks, next actions를 기록한다.
- work note 끝에 `## Actual Result`를 추가해 계획 대비 결과와 deviation 이유를 남긴다.
- 구현 완료 후 독립 검증 세션과 별도 `code-review` 세션에 전달할 changed files/test command를 최종 응답에 명시한다.

기대 산출물:
- A4a `executionInputFiles` production implementation
- 독립 A10 external surface/stale code cleanup
- focused unit/serialization/controller regression tests
- implementation work note

최종 응답 형식:
1. Implementation summary
2. Changed components
3. Data access decision
4. Contract alignment
5. Security / tenant considerations
6. SQL / DBA implications
7. Tests / verification status
8. Work note update points
9. Risks / follow-up
```
