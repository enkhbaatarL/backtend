# 가격전망해석 도메인 설계

## Document Control

- status: Review
- canonical for: 가격전망해석의 비즈니스 규칙, 공유 소유권, 생성·수정·초기화 의미와 수급모델 재실행 시 무효화 정책
- owner: BE
- source documents:
  - `AGENTS.md`
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `src/main/java/com/sk/domain/mi/controller/nextgen/PriceInterpretationController.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/PriceInterpretationServiceImpl.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/priceinterp/PriceInterpretationStore.java`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenarioPriceInterp.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioModelExecutionServiceImpl.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/execution/ScenarioExecutionRegistrar.java`
- depends on:
  - endpoint 및 DTO 계약: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - 수급모델 실행 lifecycle: `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md` F4
- supersedes:
  - 본 문서의 2026-06-30 header/child aggregate 초안과 `OUTDATED` 보존 후 교체 정책
- last synced: 2026-07-14
- open questions:
  - 가격전망해석 Agent callback에 generation/source correlation 값이 없어, 삭제된 이전 생성 요청의 지연 callback이 이후 새 `GENERATING` row에 반영될 수 있는 ABA 위험을 어떻게 차단할지 후속 계약이 필요하다.

## 1. Feature summary

시나리오 그룹의 수급모델 실행 결과를 바탕으로 시나리오별 가격전망해석을 Agent가 생성한다. 생성된 해석은 사용자별 사본이 아니라 권한 범위 내 공유 최신본이며, 권한 있는 사용자는 작성자와 무관하게 조회·수정·Agent 원본으로 초기화할 수 있다.

수급모델을 다시 실행하면 해당 시나리오의 기존 가격 결과가 더 이상 최신 기준이 아니므로, 그 결과를 근거로 한 가격전망해석 원본과 현재 수정본도 함께 무효화한다. 재실행 접수 전에 Backend가 기존 해석 존재 여부와 현재 수정 여부를 반환하고, Frontend는 초기화가 필요하면 아래 안내를 표시한다.

> 기존 수정 내역을 초기화하고 새로 생성하시겠습니까?

사용자 확인을 받은 수급모델 재실행이 접수되면 해당 시나리오의 기존 가격전망해석 row를 제거한다. 수급모델 재실행 자체가 가격전망해석 Agent 생성을 자동 호출하지는 않는다. 새 수급모델 실행이 완료된 뒤 기존 가격전망해석 생성 API를 별도로 호출해야 한다.

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

### 2.2 수정 이력의 한계

현재 모델에는 수정 이벤트를 누적하는 history table이 없다. `revision`도 각 수정의 작성자·내용·시각을 복원하는 이력이 아니며, 초기화 때도 증가한다.

본 요구사항에서 사용하는 용어는 아래처럼 제한한다.

- `hasPriceInterpretation`: 해당 시나리오에 비삭제 가격전망해석 row가 존재한다. `GENERATING`, `GENERATED`, `FAILED`를 모두 포함한다.
- `hasPriceInterpretationModification`: 현재 `generatedContent`와 `currentContent`가 null-safe 비교로 다르다. 현재 화면에서 손실될 사용자 수정본이 있다는 뜻이다.
- “수정 내역 조회”: 수정 이벤트 목록 조회가 아니라 위 현재 수정본 존재 여부 조회를 뜻한다.

과거에 수정했다가 Agent 원본으로 초기화한 경우 `revision > 0`이어도 현재 두 content가 같으므로 `hasPriceInterpretationModification=false`다. 이 경우에도 active row가 있으므로 `priceInterpretationResetRequired=true`이며 안내 팝업은 표시한다.

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
| 수급모델 재실행 접수 | row 제거 | row 제거 | row와 함께 제거 |

`revision`은 현재 코드에서 낙관적 잠금 토큰으로 검증되지 않는다. 이번 요구사항은 별도 수정 이력이나 동시 편집 제어를 추가하지 않는다.

## 4. Fixed business rules for model re-execution

1. 수급모델 실행 팝업 진입 시 기존 `GET .../model-execution-options`가 가격전망해석 영향을 함께 반환한다.
2. 조회는 actor/작성자 조건 없이 path `scenarioId`의 비삭제 가격전망해석 row를 기준으로 한다.
3. active row가 있으면 `priceInterpretationResetRequired=true`다. 현재 수정본 존재 여부와 무관하게 안내 팝업을 표시한다.
4. `hasPriceInterpretationModification=true`는 현재 Agent 원본과 공유 최신본이 다름을 뜻하며, 별도 history row 존재를 뜻하지 않는다.
5. Frontend가 팝업에서 확인한 뒤 두 실행 접수 API에 `priceInterpretationResetConfirmed=true`를 전달한다.
6. 실행 POST는 조회 결과를 신뢰하지 않고 최종 실행 등록 transaction 안에서 active row 존재 여부를 다시 확인한다.
7. 최종 확인 시 active row가 있는데 확인 값이 없거나 `false`면 실행을 접수하지 않고 `409 Conflict`로 반환한다.
8. 확인 값이 `true`면 해당 시나리오의 가격전망해석 row를 제거하고 같은 transaction에서 새 Scenario Case/Run을 생성한다.
9. 삭제 또는 Case/Run 생성 중 하나라도 실패하면 transaction 전체를 rollback한다.
10. active row가 없으면 확인 값이 없거나 `false`여도 최초 실행/재실행을 정상 접수한다.
11. 영향 범위는 해당 `scenarioId` 한 건이며 같은 그룹의 다른 시나리오 해석은 유지한다.
12. 수급모델 실행 접수 뒤 Job dispatch가 after-commit 단계에서 실패하더라도, 이미 접수된 새 run은 `FAILED`로 남고 기존 가격전망해석 삭제를 복원하지 않는다.
13. 수급모델 재실행 완료 후 가격전망해석은 미생성 상태이며, 사용자가 가격전망해석 생성 API를 호출할 때 새 `sourceRunId` 기준으로 생성한다.

## 5. Query and response semantics

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-execution-options` 응답에 다음 필드를 추가한다. 직렬화 필드의 정본은 API feature spec이다.

| Logical field | Rule |
|---|---|
| `hasPriceInterpretation` | active `MiScenarioPriceInterp` row 존재 여부 |
| `hasPriceInterpretationModification` | `!Objects.equals(generatedContent, currentContent)` |
| `priceInterpretationResetRequired` | `hasPriceInterpretation`과 동일한 v1 파생값 |

이 조회는 팝업 표시를 위한 advisory read다. 실제 삭제 허용 여부는 실행 POST의 transaction-time recheck가 최종 source of truth다.

## 6. Deletion strategy

### 6.1 Recommended operation

수급모델 재실행 무효화는 active `MiScenarioPriceInterp` row의 물리 삭제를 기본안으로 한다.

근거:

- 요구사항은 기존 Agent 원본과 사용자 수정본의 복구 불가능한 초기화를 요구한다.
- 현재 모델에는 보존해야 할 수정 history table이 없다.
- 현재 엔티티의 unique 제약은 `(scenario_id, del_yn)`이다. 같은 시나리오에서 재실행이 반복될 때 active row를 계속 soft delete하면 두 번째 `del_yn='Y'` row부터 unique 충돌할 수 있다.
- content를 null로 비우는 방식은 정상 상태를 표현할 `NOT_GENERATED` enum이 없고 잔존 row의 의미를 불명확하게 만든다.

Scenario Group 삭제의 기존 soft-delete 정책은 별도 lifecycle이며 변경하지 않는다. 재실행 삭제만 물리 삭제로 분리한다.

### 6.2 Transaction boundary

파일 upload/staging은 기존처럼 transaction 밖에서 수행한다. 최종 짧은 transaction은 아래 순서로 처리한다.

1. Scenario row lock 및 상태/mode/active execution 재검증
2. 가격전망해석 active row 재조회
3. 필요 시 `priceInterpretationResetConfirmed` 검증
4. 대상 가격전망해석 row 삭제
5. Scenario Case 생성
6. staged file relation 연결
7. Run history 생성 및 Case latest run 연결
8. parameter execution이면 preparation과 Case 연결
9. commit 후 Job dispatch 예약

두 실행 접수 경로가 모두 `ScenarioExecutionRegistrar#registerStaged`를 사용하므로, 삭제 orchestration은 이 공통 최종 등록 경계 또는 그 직전 공통 coordinator에 한 번만 둔다. multipart/JSON service branch에 서로 다른 삭제 로직을 중복 구현하지 않는다.

## 7. Concurrency and callback considerations

### 7.1 Preflight-to-submit race

사전 조회 후 다른 사용자가 가격전망해석을 생성·수정할 수 있다. 실행 POST는 active row를 재조회하고 확인 값이 없으면 409로 차단하므로, 조회 당시 row가 없었다가 새로 생긴 경우에도 무확인 삭제하지 않는다.

사용자가 `priceInterpretationResetConfirmed=true`로 제출한 뒤 commit 전 다른 사용자가 수정하는 경합은 DB row lock/transaction 경계로 직렬화해야 한다. 구체적인 repository lock 방식은 구현 단계에서 가장 가까운 Scenario lock 패턴을 재사용한다.

### 7.2 Late Agent callback

재실행 삭제 후 이전 가격전망해석 callback이 도착하면 active row가 없는 동안에는 현재 `PriceInterpretationStore`가 해당 시나리오 결과를 skip한다. 그러나 현재 callback에는 `generationId`나 `sourceRunId`가 없으므로, 새 수급모델 완료 후 새 해석 생성을 이미 시작한 시점에 이전 callback이 늦게 도착하면 새 `GENERATING` row를 잘못 완료시킬 수 있다.

이 ABA 위험은 이번 사전조회/삭제 API만으로 완전히 해결되지 않는다. 구현 전 아래 중 하나를 별도 결정해야 한다.

- 권장: Agent request/callback에 stable `generationId` 또는 `sourceRunId`를 전달하고 row의 현재 값과 일치할 때만 callback을 적용한다.
- 대안: 이전 generation이 terminal임을 보장하기 전 수급모델 재실행 또는 새 가격전망해석 생성을 차단한다.

## 8. API and implementation handoff

### 8.1 API feature spec responsibility

- `model-execution-options` 추가 응답 필드
- `from-parameters` JSON request의 `priceInterpretationResetConfirmed`
- multipart 실행 request part의 `priceInterpretationResetConfirmed`
- `409 Conflict` 확인 필요 오류
- 정확한 팝업 조건과 문구

### 8.2 Expected implementation touch points

- `ModelExecutionOptionsRes`
- `ModelParamProposalServiceImpl#getModelExecutionOptions`
- `ScenarioModelExecutionDedicatedController`
- `ScenarioModelExecutionService` / `ScenarioModelExecutionServiceImpl`
- `ModelParamProposalExecutionReq`
- `PriceInterpretationStore`
- `MiScenarioPriceInterpRepository`
- `ScenarioExecutionRegistrar` 또는 공통 실행 등록 coordinator
- options/service/controller/두 실행 경로 transaction 테스트

### 8.3 Data access recommendation

- 조회: JPA 단건 조회 또는 projection. 시나리오당 active row 최대 1건이므로 QueryDSL/MyBatis를 새로 도입하지 않는다.
- 삭제: JPA repository physical delete. 삭제와 Case/Run 생성은 동일 transaction에 참여한다.
- schema: 신규 table/column은 필요하지 않다. 실제 DB unique 제약과 물리 delete 권한은 구현 전 확인한다.

## 9. Acceptance scenarios

| Given | When | Then |
|---|---|---|
| 가격전망해석 없음 | options 조회 | 세 flag 모두 `false` |
| Agent 원본과 현재본 동일 | options 조회 | `hasPriceInterpretation=true`, modification=`false`, resetRequired=`true` |
| 타인이 현재본 수정 | options 조회 | actor와 무관하게 세 flag 모두 `true` |
| active row 있음, 확인=false | 실행 POST | `409`, 해석/Case/Run 변경 없음 |
| active row 있음, 확인=true | 실행 POST 성공 | 대상 row 삭제 + 새 Case/Run commit |
| active row 없음, 확인 생략 | 실행 POST 성공 | 기존 실행 접수와 동일 |
| 같은 그룹의 다른 시나리오에 해석 존재 | 한 시나리오 재실행 | 대상 시나리오 row만 삭제 |
| 해석 삭제 후 Case/Run 저장 실패 | 실행 POST | 전체 rollback, 기존 해석 유지 |
| 실행 commit 후 Job dispatch 실패 | 상태 조회 | 새 run은 `FAILED`, 기존 해석은 복원하지 않음 |

## 10. Implementation readiness

- options/read contract: ready
- model execution confirmation/delete API contract: ready
- schema change: 없음
- production implementation: `blocked by open questions`
- blocker: 가격전망해석 callback의 stale-generation correlation. 삭제된 이전 요청의 callback이 이후 새 row에 반영되지 않도록 `generationId`/`sourceRunId` 검증 또는 동등한 lifecycle 차단 정책을 먼저 확정해야 한다.

## 11. Change log

- 2026-06-30: 가격전망해석 신규 도메인 초안 작성.
- 2026-07-14: 실제 2026-07-06 구현을 기준으로 문서를 재동기화했다. 생성 전용이 아니라 시나리오별 수정/원본 초기화가 구현되어 있음을 반영하고, 미구현 header aggregate·그룹 version·그룹 전체 reset·`OUTDATED` 보존 정책을 현재 per-scenario row 모델로 supersede했다.
- 2026-07-14: 수급모델 재실행 사전 영향 조회, 확인 팝업, 두 실행 접수 경로의 확인 값, 해당 시나리오 가격전망해석 row 삭제 및 transaction 원자성 규칙을 추가했다.
