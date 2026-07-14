# 가격전망해석 도메인 설계

## Document Control

- status: Review
- canonical for: 가격전망해석의 비즈니스 규칙, 공유 소유권, 생성·수정·초기화 의미와 수급모델 재실행 시 무효화 정책
- owner: BE
- source documents:
  - `AGENTS.md`
  - `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `docs/design-docs/gas/price-forecast-interpretation-erd-ddl-draft.md`
  - `src/main/java/com/sk/domain/mi/controller/nextgen/PriceInterpretationController.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/PriceInterpretationServiceImpl.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/priceinterp/PriceInterpretationStore.java`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenarioPriceInterp.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImpl.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/execution/ScenarioExecutionRegistrar.java`
- depends on:
  - endpoint 및 DTO 계약: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - 수급모델 실행 lifecycle: `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md` F4
  - physical unique 및 DBA rollout: `docs/design-docs/gas/price-forecast-interpretation-erd-ddl-draft.md`
- supersedes:
  - 본 문서의 2026-06-30 header/child aggregate 초안과 `OUTDATED` 보존 후 교체 정책
- last synced: 2026-07-14
- open questions:
  - 수급모델 실행 연계 범위에는 없음. 가격전망해석 생성·callback lifecycle은 가격전망해석 도메인 담당자가 별도로 소유한다.

## 1. Feature summary

시나리오 그룹의 수급모델 실행 결과를 바탕으로 시나리오별 가격전망해석을 Agent가 생성한다. 생성된 해석은 사용자별 사본이 아니라 권한 범위 내 공유 최신본이며, 권한 있는 사용자는 작성자와 무관하게 조회·수정·Agent 원본으로 초기화할 수 있다.

수급모델을 다시 실행하면 해당 시나리오의 기존 가격 결과가 더 이상 최신 기준이 아니므로, 그 결과를 근거로 한 가격전망해석 원본과 현재 수정본도 함께 무효화한다. Backend는 수급모델 실행을 최종 등록하는 transaction에서 대상 시나리오의 active 가격전망해석 row를 조회하고, 있으면 사용자 확인 절차 없이 soft delete한 뒤 새 Case/Run을 생성한다.

수급모델 재실행 자체가 가격전망해석 Agent 생성을 자동 호출하지는 않는다. 새 수급모델 실행이 완료된 뒤 기존 가격전망해석 생성 API를 별도로 호출해야 한다.

가격전망해석은 수급모델 완료 run당 한 번만 생성할 수 있다. 이 생성 횟수 제한과 Agent request/callback 정합성은 가격전망해석 도메인의 upstream invariant이며, 이번 수급모델 실행 연계 기능은 이를 구현하거나 검증하지 않는다.

## 2. Existing implementation reviewed

### 2.1 가격전망해석은 생성 전용이 아니다

현재 코드는 아래 기능을 모두 제공한다.

| 기능 | 현재 코드 |
|---|---|
| 조회 | `GET /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/price-interpretations` |
| Agent 생성 접수 | `POST /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/price-interpretations` |
| 사용자 수정 | `PATCH /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/scenarios/{scenarioId}/price-interpretation` |
| Agent 원본 초기화 | `PATCH /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/scenarios/{scenarioId}/price-interpretation/reset` |

`MiScenarioPriceInterp`는 다음 값을 한 row에 보관한다.

- `generatedContent`: 마지막 Agent 생성 원본
- `currentContent`: 화면에 표시되는 공유 최신본. 사용자 수정 시 이 값만 변경한다.
- `sourceRunId`: 해석의 근거가 된 시나리오 수급모델 완료 실행 ID
- `status`: `GENERATING | GENERATED | FAILED`
- `revision`: 사용자 수정 또는 원본 초기화 시 증가하는 참고값
- `BaseEntity.modifyUserId`, `modifyDt`: 마지막 변경의 기술 감사 정보

따라서 “타인 수정 포함”은 별도 사용자 소유권 조회가 아니라 같은 시나리오의 공유 active row를 사용자 조건 없이 조회하는 것으로 충족한다.

### 2.2 재실행 영향 판단

현재 모델에는 수정 이벤트를 누적하는 history table이 없다. `revision`도 각 수정의 작성자·내용·시각을 복원하는 이력이 아니며, 초기화 때도 증가한다.

`generatedContent`(`org_cntt`)와 `currentContent`(`cur_cntt`)는 동일한 `tmm_mi_scenario_prc_interp_m` row에 저장된다. 수급모델 재실행 시 해당 row 전체가 `del_yn='Y'`로 비활성화되므로, 별도 수정 여부나 수정자·작성자를 조회할 필요가 없다.

수급모델 실행 연계에서는 path `scenarioId`의 비삭제 가격전망해석 row 존재 여부 하나만 사용한다. `GENERATING`, `GENERATED`, `FAILED`를 모두 포함하며, active row가 있으면 해당 row 전체를 soft delete한다.

## 3. Current domain model

### 3.1 Aggregate boundary

- 생성 요청 단위: `ScenarioGroup`
- 저장 및 재실행 무효화 단위: `Scenario`
- cardinality: `Scenario 1 : 0..1 active MiScenarioPriceInterp`
- 소유권: 개인 사용자가 아니라 Scenario/ScenarioGroup 권한 범위의 공유 데이터

그룹 생성 API가 여러 시나리오를 한 번에 Agent에 요청하더라도, 수급모델 재실행 endpoint는 `scenarioId` 단위다. 따라서 재실행 시 그룹 전체 해석을 제거하지 않고 재실행 대상 시나리오의 active row만 제거한다.

### 3.2 Content and revision semantics

| Operation | `generatedContent` | `currentContent` | `revision` |
|---|---|---|---:|
| 최초 생성 성공 | Agent 결과 | Agent 결과 | 유지(초기값 0) |
| 사용자 수정 | 유지 | 요청 content | `+1` |
| Agent 원본 초기화 | 유지 | `generatedContent` | `+1` |
| 기존 재생성 성공 | 새 Agent 결과로 교체 | 새 Agent 결과로 교체 | 현재 구현은 유지 |
| 수급모델 재실행 접수 | 삭제 row에 보존 | 삭제 row에 보존 | row와 함께 보존, `del_yn='Y'` |

`revision`은 현재 코드에서 낙관적 잠금 토큰으로 검증되지 않는다. 이번 요구사항은 별도 수정 이력이나 동시 편집 제어를 추가하지 않는다.

## 4. Fixed business rules for model re-execution

1. multipart/JSON 두 수급모델 실행 접수 API의 request/response 계약은 변경하지 않는다.
2. 실행 전 options 조회, 안내 팝업, 사용자 확인값, 확인 전용 오류 코드를 추가하지 않는다.
3. 두 실행 경로가 합류하는 최종 실행 등록 transaction에서 actor/작성자 조건 없이 path `scenarioId`의 active 가격전망해석 row를 조회한다.
4. active row가 있으면 상태, `org_cntt`와 `cur_cntt`의 동일 여부, 마지막 수정자와 무관하게 `del_yn='Y'`로 soft delete한다.
5. active row가 없으면 no-op으로 처리하고 기존 실행 등록을 계속한다.
6. 가격전망해석 soft delete를 Case 생성보다 먼저 수행하고, 같은 transaction에서 새 Scenario Case/Run을 생성한다.
7. 삭제 또는 Case/Run 생성 중 하나라도 실패하면 transaction 전체를 rollback한다.
8. 영향 범위는 해당 `scenarioId` 한 건이며 같은 그룹의 다른 시나리오 해석은 유지한다.
9. 수급모델 실행 접수 뒤 Job dispatch가 after-commit 단계에서 실패하더라도, 이미 접수된 새 run은 `FAILED`로 남고 기존 가격전망해석의 soft-delete 상태를 복원하지 않는다.
10. 수급모델 재실행 완료 후 가격전망해석은 미생성 상태이며, 사용자가 가격전망해석 생성 API를 호출할 때 새 `sourceRunId` 기준으로 생성한다.

## 5. API semantics

가격전망해석 무효화는 수급모델 실행의 내부 처리다. `model-execution-options` 응답, multipart request part, JSON request DTO, 실행 응답 및 오류 계약을 변경하지 않는다.

## 6. Deletion strategy

### 6.1 Fixed operation

수급모델 재실행 무효화는 active `MiScenarioPriceInterp` row의 soft delete로 처리한다.

- `MiScenarioPriceInterp#softDelete()`로 `del_yn='Y'`를 설정하고 감사 컬럼을 갱신한다.
- `org_cntt`, `cur_cntt`, `src_run_id`, `rvsn_no`는 삭제 row에 보존한다.
- 일반 조회와 생성 대상 판정은 `del_yn='N'` row만 사용하므로 삭제 직후 화면에서는 가격전망해석 미생성 상태가 된다.
- 같은 시나리오의 soft-deleted row 다건과 active row 최대 1건을 함께 허용해야 한다.

테이블 담당자가 `scenario_id`와 `CASE WHEN del_yn='N' THEN 'N' END`를 조합한 functional unique를 확정하고 DB 적용을 완료했다. active row는 `(scenario_id, 'N')`으로 시나리오당 최대 1건만 허용하고, deleted row는 두 번째 key part가 `NULL`이므로 여러 건을 보존한다. 정확한 DDL과 DBA rollout 기록은 `price-forecast-interpretation-erd-ddl-draft.md`가 source of truth다.

### 6.2 Transaction boundary

파일 upload/staging은 기존처럼 transaction 밖에서 수행한다. 최종 짧은 transaction은 아래 순서로 처리한다.

1. Scenario row lock 및 상태/mode/active execution 재검증
2. 가격전망해석 active row 재조회
3. row가 있으면 대상 가격전망해석 row `softDelete()`, 없으면 no-op
4. Scenario Case 생성
5. staged file relation 연결
6. Run history 생성 및 Case latest run 연결
7. parameter execution이면 preparation과 Case 연결
8. commit 후 Job dispatch 예약

두 실행 접수 경로가 모두 `ScenarioExecutionRegistrar#registerStaged`를 사용하므로, 삭제 orchestration은 이 공통 최종 등록 경계의 Case 생성 직전에 한 번만 둔다. multipart/JSON service branch에 서로 다른 삭제 로직을 중복 구현하지 않는다.

## 7. Concurrency and ownership boundary

### 7.1 Final mutation race

최종 삭제 조회는 active 가격전망해석 row에 write lock을 거는 방식으로 같은 row의 수정/삭제 경합을 직렬화한다. lock으로 조회한 active row가 있으면 즉시 soft delete하고 같은 transaction에서 Case/Run을 생성한다.

다만 현재 수급모델 실행 경로의 `MiScenarioRepository#findByScenarioIdForUpdate` lock은 수급모델 실행 요청끼리는 직렬화하지만, `PriceInterpretationStore#beginGeneration` 등 가격전망해석 생성 경로는 같은 Scenario lock을 사용하지 않는다. functional unique는 active row 중복을 막지만, active row가 아직 없는 상태에서 가격전망해석 생성과 수급모델 실행의 선후 관계까지 결정하지는 않는다. 아래 중 한 가지 동시성 규약을 가격전망해석 도메인 담당자와 함께 확정한다.

- 가격전망해석 생성과 수급모델 실행이 동일한 Scenario row lock을 먼저 획득한다.
- 확정된 active-row unique index와 DB isolation에서 `SELECT ... FOR UPDATE`의 부재 row/gap lock을 사용하고 이를 통합 테스트로 검증한다.
- 동등한 scenario 단위 직렬화 수단을 별도로 둔다.

### 7.2 가격전망해석 lifecycle 책임 분리

가격전망해석의 완료 run당 1회 생성 제한, Agent request, callback correlation과 stale callback 처리는 가격전망해석 도메인 담당자의 책임이다.

수급모델 실행 연계 기능의 책임은 아래로 한정한다.

- 실행 등록 transaction에서 대상 Scenario의 active row 조회
- active row가 있으면 soft delete, 없으면 no-op
- 삭제와 새 Case/Run 생성의 원자성 보장

따라서 가격전망해석 callback 계약은 이번 수급모델 실행 기능 구현의 선행조건이나 완료 기준에 포함하지 않는다.

## 8. API and implementation handoff

### 8.1 API feature spec responsibility

- 기존 multipart/JSON 실행 request와 response 계약 유지
- 가격전망해석 soft delete가 별도 API field나 오류를 추가하지 않는 내부 실행 규칙임을 명시

### 8.2 Expected implementation touch points

#### 8.2.1 final transaction 조회와 soft delete

- 두 실행 경로는 final `TransactionOperations` callback에서 Scenario row를 다시 lock하고 공통 `ScenarioExecutionRegistrar#registerStaged`로 합류한다.
- `ScenarioExecutionRegistrar#registerStaged` 메서드 시작부에서 Case 생성보다 먼저 가격전망해석 무효화 컴포넌트를 한 번 호출한다. multipart/JSON branch에 삭제 로직을 각각 구현하지 않는다.
- `PriceInterpretationStore`의 모델 실행 전용 mutation은 `MANDATORY` transaction으로 두어 registrar의 final transaction 참여를 강제한다. 이 메서드는 active row를 write lock으로 조회하고, row가 있으면 `MiScenarioPriceInterp#softDelete()`를 호출하며 없으면 no-op이다.
- 위 호출 다음에 기존 Case, file relation, Run history, preparation 연결을 수행한다. 따라서 soft delete 또는 Case/Run 저장 중 하나라도 실패하면 모두 rollback되고, after-commit Job dispatch 시점에는 삭제하지 않는다.

#### 8.2.2 Test impact

- `ScenarioExecutionRegistrarTest`: active row 무효화 호출이 Case 생성보다 먼저 수행되는지, row 조회/삭제 실패 시 Case를 만들지 않는지.
- `PriceInterpretationStoreTest`: active row 없음 no-op, active row soft delete, target scenario 단위 조회, `MANDATORY` transaction 경계.
- 가능한 integration test: target scenario만 soft delete, 같은 그룹의 다른 scenario 유지, Case/Run 저장 실패 시 rollback, 반복 재실행.

### 8.3 Data access recommendation

- 조회: path `scenarioId` 기준 active JPA entity 단건 write-lock 조회. 시나리오당 active row 최대 1건이므로 QueryDSL/MyBatis를 새로 도입하지 않는다.
- 삭제: 조회한 active JPA entity의 `softDelete()` 호출. soft delete와 Case/Run 생성은 동일 transaction에 참여한다.
- schema: functional unique의 DB 적용이 완료됐다. 정확한 DDL과 pre-check/apply/rollback/verify 절차는 `price-forecast-interpretation-erd-ddl-draft.md`를 따른다.
- JPA metadata: 현재 `@UniqueConstraint(scenario_id, del_yn)`는 functional unique를 표현하지 못한다. 정리는 별도 schema-mapping cleanup으로 추적하고, 이번 자동 soft-delete 실행 연계 slice에는 포함하지 않는다. DDL이 schema source of truth다.

## 9. Acceptance scenarios

| Given | When | Then |
|---|---|---|
| active row 있음 | multipart/JSON 실행 POST 성공 | 대상 row `del_yn='Y'` + 새 Case/Run commit |
| active row 없음 | multipart/JSON 실행 POST 성공 | 삭제 없이 기존 실행 접수와 동일 |
| 같은 그룹의 다른 시나리오에 해석 존재 | 한 시나리오 재실행 | 대상 시나리오 row만 삭제 |
| 해석 삭제 후 Case/Run 저장 실패 | 실행 POST | 전체 rollback, 기존 해석 유지 |
| 실행 commit 후 Job dispatch 실패 | 상태 조회 | 새 run은 `FAILED`, 기존 해석은 복원하지 않음 |
| 같은 시나리오에서 해석 생성·재실행을 두 차례 이상 반복 | 각 재실행 POST | 삭제 row 다건 보존, active row 최대 1건, unique 충돌 없음 |

## 10. Implementation readiness

- API contract change: 없음
- model execution automatic soft-delete mutation: ready
- schema change: functional unique DB 적용 완료
- model execution integration implementation: `ready`
- deployment gate: satisfied
- scope boundary: 완료 run당 1회 생성과 Agent callback 정합성은 가격전망해석 도메인 담당자의 별도 책임이며 이번 구현의 blocker가 아니다.

## 11. Change log

- 2026-06-30: 가격전망해석 신규 도메인 초안 작성.
- 2026-07-14: 실제 2026-07-06 구현을 기준으로 문서를 재동기화했다. 생성 전용이 아니라 시나리오별 수정/원본 초기화가 구현되어 있음을 반영하고, 미구현 header aggregate·그룹 version·그룹 전체 reset·`OUTDATED` 보존 정책을 현재 per-scenario row 모델로 supersede했다.
- 2026-07-14: 수급모델 재실행 시 해당 시나리오 가격전망해석 row 삭제 및 transaction 원자성 규칙을 추가했다.
- 2026-07-14: `org_cntt`와 `cur_cntt`가 동일 row에 저장되는 구조를 반영해 재실행 영향 판단을 active row 존재 여부 하나로 단순화하고 별도 수정 여부 응답을 제거했다.
- 2026-07-14: 완료 run당 1회 생성과 callback lifecycle을 가격전망해석 도메인 책임으로 분리하고, 수급모델 실행 연계 구현 범위를 active row 조회·삭제로 한정해 callback 관련 blocker를 제거했다.
- 2026-07-14: 재실행 무효화 방식을 physical delete에서 soft delete로 변경했다. 현재 `(scenario_id, del_yn)` unique와 반복 삭제가 충돌하므로 구체적인 constraint/index 변경은 테이블 담당자 검토 중인 open question으로 남겼다.
- 2026-07-14: 실제 multipart/JSON 실행 흐름을 추적해 soft delete를 `ScenarioExecutionRegistrar#registerStaged`의 Case 생성 직전에 두고, final write-lock 조회·테스트 위치를 파일과 메서드 단위로 확정했다.
- 2026-07-14: 테이블 담당자가 active row만 제한하는 functional unique를 확정하고 DB 적용을 완료해 반복 soft-delete schema blocker를 해제했다. 물리 DDL과 rollout 기록은 별도 canonical ERD/DDL draft로 분리했다.
- 2026-07-14: 구현 범위를 단순화해 options 영향 조회, 사용자 확인값, 사전검증, 확인 전용 오류 계약을 제거했다. 수급모델 실행 final transaction에서 active row가 있으면 자동 soft delete한 뒤 Case/Run을 생성한다.
