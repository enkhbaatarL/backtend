# MI 수급모델 파라미터 제안 Domain Design

## Document Control

- domain: gas / MI model execution
- status: draft
- owner: platform BE
- created at: 2026-07-03
- updated at: 2026-07-03
- canonical for: 파라미터 제안의 business concept, ownership, lifecycle, 상태 전이, Excel 생성 및 기존 수급모델 실행 연결 경계
- not canonical for:
  - endpoint, request/response, HTTP error contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - physical DDL 및 DBA rollout: 후속 ERD/DDL draft
  - Agent API: Notion `api 정의 (frontend-backend-agent)`는 외부 evidence이며 repository canonical source가 아님
- implementation readiness: ready after minor cleanup for mock; real integration requires Open Questions의 P0 항목 확정

## 1. Feature Summary

이미 생성된 Scenario 중 **파라미터 제안 대상 Event를 기반으로 생성된 Scenario**에서 사용자가 `MI 수급모델 실행` 팝업을 열어 Agent에게 모델 파라미터 제안을 요청하고, 생성된 제안값을 검토·수정한 뒤 수급모델 실행을 접수하는 기능이다.

실행 입력 방식은 Scenario의 기반 Event에 따라 분기한다.

- `PARAMETER_PROPOSAL`: 사용자는 Excel 파일을 업로드하지 않는다. Backend가 협의된 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개를 준비하고, 사용자가 확정한 값을 Agent가 지정한 파일·시트·테이블·slot에 병합한다.
- `FILE_UPLOAD`: 기존 방식 그대로 사용자가 `DEMAND`/`SUPPLY`/`TRADE` Excel 파일 3개를 직접 업로드한다.

두 방식 모두 파일 3개가 준비된 이후에는 기존 Scenario Model Execution pipeline을 사용한다. 기존 파일 업로드 기능은 유지되며 제거·deprecated 대상이 아니다.

이 기능은 Scenario 생성 기능이 아니다. 기존 `Scenario`와 기존 `Scenario Case/Run` 실행 aggregate 사이에 위치하는 실행 준비 단계다.

## 2. Existing Structure Reviewed

- frontend-facing model execution base path: `/api/v1/mi/next-gen/scenarios/{scenarioId}`
- 기존 실행 접수:
  - `POST /model-executions`
  - `multipart/form-data`의 파일 3개를 받음
  - 파일명으로 `DEMAND`/`SUPPLY`/`TRADE` 역할을 구분
  - 파라미터 제안 비대상 Event 기반 Scenario에서 계속 사용하는 active contract
- 기존 실행 상태 조회:
  - `GET /model-executions/{runId}`
- 기존 실행 persistence:
  - `tmd_mdl_scnrcasem`: Scenario Case master
  - `tmd_mdl_scnrcaseh`: 실행 history
  - `tcm_filel`: case 입력 파일 3개
- 기존 실행 dispatch:
  - 파일 3개를 storage에 올린 뒤 Kubernetes Job에 경로 전달
- 기존 async UI pattern:
  - Agent callback 수신
  - SSE는 완료 신호만 전달
  - FE는 상태 API를 재조회해 canonical state를 가져옴
- 기존 Agent proposal evidence:
  - 요청: `POST /model-parameters/scenarios/{scenario_id}/proposals`
  - 완료 callback: `PROCESSING -> PROPOSED | FAILED`
  - 확정: `POST /model-parameters/proposals/{modelParamProposalId}/confirmation`

## 3. Proposed Domain Model

### 3.1 `ModelParameterProposal`

수급모델 실행 전 한 번의 파라미터 제안 생성과 사용자 확정을 대표하는 transaction/history 성격의 resource다.

- owner: 요청 사용자
- target: 기존 `Scenario`
- upstream owner: Agent가 proposal 계산 및 Agent-side proposal row를 소유
- Backend owner:
  - FE에 노출할 proposal lifecycle projection
  - callback 수신 결과 snapshot
  - 사용자 확정 snapshot
  - callback idempotency 및 실행 중복 방지
  - Excel 생성과 Scenario Case/Run 연결
- relationship:
  - `Scenario 1:N ModelParameterProposal`
  - `ModelParameterProposal 0..1 ScenarioCase`

### 3.2 Lifecycle

| Status | 의미 | 다음 상태 |
|---|---|---|
| `PROCESSING` | Agent가 제안 생성 중 | `PROPOSED`, `FAILED` |
| `PROPOSED` | callback 결과 저장 완료, 사용자 검토 가능 | `CONFIRMING` |
| `CONFIRMING` | 사용자 실행 요청을 검증하고 Agent confirmation 수행 중 | `CONFIRMED`, `FAILED` 또는 재시도 가능 상태 |
| `CONFIRMED` | Agent confirmation 성공, 확정 snapshot 고정 | `EXECUTING`, `EXECUTION_FAILED` |
| `EXECUTING` | Excel 생성 후 기존 model execution 접수 완료 | terminal model execution은 기존 Case/Run이 관리 |
| `EXECUTION_FAILED` | confirmation 이후 Excel 생성·업로드·실행 접수 실패 | 재시도 정책 필요 |
| `FAILED` | Agent proposal 생성 또는 callback 처리 실패 | terminal; 재제안 정책 필요 |

Agent 외부 계약은 `PROCESSING/PROPOSED/CONFIRMED/FAILED`만 사용한다. `CONFIRMING/EXECUTING/EXECUTION_FAILED`는 Backend orchestration 상태 후보이며 최종 DDL에서는 Agent status와 Backend status를 분리할 수 있다.

### 3.3 `ModelExecutionInputMode`

Scenario의 기반 Event가 어떤 수급모델 실행 입력 workflow를 사용해야 하는지 나타내는 derived policy다.

| Mode | 대상 | 입력 준비 방식 |
|---|---|---|
| `PARAMETER_PROPOSAL` | 파라미터 제안 대상 Event 기반 Scenario | Agent 제안 + 사용자 수정 + Backend Excel 3개 생성 |
| `FILE_UPLOAD` | 파라미터 제안 비대상 Event 기반 Scenario | 사용자 Excel 3개 직접 업로드 |

- mode는 사용자가 선택하는 값이 아니다.
- Backend가 `Scenario -> ScenarioGroup -> Base Event` 관계로 판정한다.
- FE는 Backend가 제공한 mode로 팝업 UI를 분기한다.
- 제안 대상 여부의 실제 DB source는 신규 Event flag 또는 Event Type 설정 중 하나로 확정해야 한다.
- 기존 `templateAvailable`은 Scenario Design template 존재 여부이므로 이 정책에 재사용하지 않는다.

## 4. Table Design Draft

### 4.1 권고안: 별도 proposal history table

후속 ERD review 대상 후보는 `tmm_mdlparamrecmh`(MI 모델 파라미터 추천 이력)다. API에서는 Agent 용어와 정렬해 `ModelParameterProposal`을 사용하되, 물리명은 현재 데이터 사전의 `모델(MDL)`, `파라미터(PARAM)`, `추천(RECM)` 용어를 우선 사용한다.

후보 컬럼:

| Logical field | Physical candidate | 목적 |
|---|---|---|
| backend proposal key | `mdl_param_recm_seq` | Backend surrogate PK |
| Agent proposal ID | `agnt_prpsl_id` 후보 | callback/confirmation correlation, UNIQUE 필요 |
| Scenario ID | `scnr_id` | 대상 Scenario logical reference |
| Scenario Case ID | `case_id` | 실행 접수 후 연결, nullable, UNIQUE 후보 |
| request user | `req_usr_no` | 요청자/소유자 |
| proposal status | `recm_stat_cd` | Backend lifecycle status |
| proposed snapshot | `prpsl_param_json` 후보 | Agent callback 전체 proposal snapshot |
| confirmed snapshot | `cfrm_param_json` 후보 | 사용자 확정 전체 snapshot |
| failure reason | `fail_rsn_ctnt` 후보 | Agent/Backend 실패 사유 |
| requested/completed/confirmed timestamps | naming review 필요 | lifecycle timestamp |
| common audit | 공통 5종 | `reg_user_id`, `reg_dtm`, `mod_user_id`, `mod_dtm`, `del_yn` |

물리명 후보 중 `에이전트`, `제안`, `확정`, timestamp 조합은 데이터 사전 검토가 필요하므로 DDL로 확정하지 않는다.

### 4.2 저장 대안 비교

| 대안 | 장점 | 문제 | 판단 |
|---|---|---|---|
| Backend DB snapshot | 재조회, 재시작, audit, idempotency, 실행 연결 가능 | DDL/DBA 필요 | real integration 권고 |
| Redis only | mock 구현이 빠르고 기존 validation cache와 유사 | TTL/재시작 유실, 실행 감사 불가 | mock adapter에 한정 |
| FE round-trip only | Backend 저장 최소화 | callback 이후 FE 전달, reload, 위변조 검증, 다중 인스턴스 문제 | 채택하지 않음 |
| Agent 조회 API 의존 | 중복 저장 감소 | 현재 Agent GET 계약 없음, 장애 결합 증가 | 현재 불가 |

## 5. Relationship Design

```text
MiScenario (existing)
  1
  |
  N
ModelParameterProposal (new candidate)
  0..1
  |
  1
ScenarioCase / ScenarioCaseRunHistory (existing)
  |
  N
tcm_filel: DEMAND / SUPPLY / TRADE generated files
```

- proposal은 Scenario에 종속되지만 Scenario Case보다 먼저 생성된다.
- 최종 실행 접수 성공 시 proposal에 `case_id`를 연결한다.
- 모델 실행 terminal 상태는 proposal status에 복제하지 않고 기존 Case/Run을 source of truth로 유지한다.

## 6. Business Rules

1. 제안 요청 대상 Scenario는 존재하고 삭제되지 않아야 한다.
2. 요청 사용자는 기존 Next Gen Scenario/Model Execution permission을 만족해야 한다.
3. `PARAMETER_PROPOSAL` mode Scenario만 proposal 생성과 JSON execution을 사용할 수 있다.
4. `FILE_UPLOAD` mode Scenario는 proposal 생성을 거절하고 기존 multipart execution만 사용할 수 있다.
5. mode와 다른 실행 입력 방식을 요청하면 Backend가 거절하며 client가 mode를 override할 수 없다.
6. 한 실행 준비 시도에서 proposal 생성 요청은 1회만 허용한다.
7. 같은 Scenario에 `PROCESSING` proposal 또는 `RUNNING` model execution이 있으면 새 요청을 차단한다.
8. callback path ID, body `modelParamProposalId`, `scenarioId`는 저장된 request와 일치해야 한다.
9. 동일 terminal callback 재전송은 같은 payload면 성공으로 idempotent 처리한다. 다른 payload면 conflict로 기록하고 덮어쓰지 않는다.
10. 사용자는 `proposedValue`만 수정할 수 있다.
11. `parameterName`, `modelTarget`, slot-axis 값, `basecaseValue`는 Agent callback snapshot 기준 immutable이다.
12. 최종 실행 요청은 전체 confirmation parameter set을 보내며 Backend는 저장된 proposal 구조와 일치하는지 검증한다.
13. Agent confirmation 성공 후에만 Excel 3개 생성과 기존 model execution 접수를 진행한다.
14. Excel 병합은 Agent `modelTarget`의 `modelFile`, `modelSheet`, `tableName`, `valueFieldNames`, `slotAxesFieldNames`를 사용한다.
15. 생성된 파일명은 기존 `DEMAND`/`SUPPLY`/`TRADE` 역할 검증을 통과해야 한다.
16. 두 mode 모두 준비된 파일 3개를 기존 `tcm_filel(SCNRCASE + case_id)`와 Kubernetes Job dispatch 흐름에 전달한다.
17. 최종 실행 버튼 중복 클릭은 동일 proposal 기준 하나의 Case/Run만 생성해야 한다.

## 7. Extension Considerations

- proposal history UI가 필요해질 수 있으므로 Scenario당 여러 proposal row를 허용하는 1:N 형태를 유지한다.
- Excel mapping schema가 변경될 수 있으므로 callback 원본 JSON은 lossless하게 보존한다.
- 사용자 수정 필드가 `proposedValue` 외로 확장되면 mutable field allow-list를 versioning해야 한다.
- Agent contract version 또는 proposal schema version 필드를 후속 검토한다.

## 8. Data Access Recommendation by Responsibility

- proposal header/status/snapshot write: JPA
  - callback CAS 상태 전이와 Case 연결 transaction에 적합
- proposal JSON retrieval: JPA entity 또는 projection
- Excel mapping/merge: Apache POI 기반 별도 domain service
- generated file persistence: 기존 `FileService` 재사용
- mock: 동일 service contract 아래 in-memory 또는 JSON seed adapter

## 9. API/DTO Impact

후속 feature spec에서 다음을 정의한다.

- model execution input mode 조회 API
- proposal 생성 요청 API
- proposal 상태/결과 조회 API
- Agent callback API
- SSE completion signal
- 사용자 확정 + model execution 접수 API
- 동적 `values[]` key-value shape와 immutable/mutable field validation

## 10. SQL / DBA Implications

- real integration 권고안은 신규 proposal history table 1개가 필요하다.
- 최소 index 후보:
  - Agent proposal ID UNIQUE
  - `(scnr_id, reg_dtm desc)` latest proposal 조회
  - `case_id` UNIQUE nullable
- active proposal 중복 방지는 generated column unique 또는 transaction/application guard 중 ERD review에서 확정한다.
- JSON column 크기와 최대 proposal parameter/value 수를 측정해야 한다.
- schema는 수동 DBA 배포하며 `ddl-auto=validate` mapping 적용 전에 DDL이 선행되어야 한다.

## 11. Risks / Open Questions

### P0 — real integration 전 필수

1. 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일의 저장 위치, version 선택 규칙, owner.
2. Event의 파라미터 제안 대상 여부를 저장하는 canonical source: Event 직속 flag인지 Event Type 설정인지.
3. 기존 Scenario에 대해 mode를 결정할 수 있도록 대상 여부 데이터가 backfill되어 있는지.
4. Agent `tableName` 필드의 최종 required 계약. 목표 확인일은 2026-07-06~07-07.
5. Agent confirmation이 중복 호출에 idempotent한지, confirmation 성공 후 Backend Excel 생성이 실패했을 때 재호출 가능한지.
6. `values[].proposedValue`와 동적 slot-axis 값의 허용 JSON type 및 null 정책.
7. callback 인증을 Bearer token으로 구현할지 기존 role/JWT callback 정책과 통합할지.
8. Backend DB 영속 권고안 승인 및 DDL rollout 가능일.

### P1 — 운영 전 확정

1. `FAILED` proposal 이후 같은 실행 준비에서 재제안을 허용할지.
2. `CONFIRMED` 이후 Excel 생성 실패 시 사용자가 같은 proposal로 재시도할지.
3. proposal 보존 기간과 개인정보/민감정보 분류.
4. SSE 재연결 시 missed event를 상태 조회로 수렴시키는 polling interval/timeout.

## 12. Exit Criteria / Handoff

- mock API contract drafting: ready
- real DB implementation: DB snapshot 권고안 승인 후 ready
- `backend-feature-spec` handoff:
  - endpoint family, DTO, callback, SSE, final execution request를 고정한다.
- `erd-change-review` handoff:
  - 신규 history table 필요성
  - Agent proposal ID unique/idempotency
  - active proposal uniqueness
  - JSON snapshot 크기/index
  - nullable `case_id` 0..1 연결
- implementation readiness: ready after minor cleanup for mock; P0 미결정 사항은 real integration blocker

## 13. Dictionary Updates

- 이번 설계에서는 데이터 사전을 변경하지 않았다.
- 사용 가능한 기존 용어:
  - 모델 `MDL`
  - 파라미터 `PARAM`
  - 추천 `RECM`
  - 시나리오 `SCNR`
  - 상태 `STAT`
  - 요청 `REQ`
  - 실패 `FAIL`
- 후속 DDL 작성 전에 `에이전트`, `제안`, `확정` 물리명 사용 여부를 검토하고 필요한 경우 append-only로 등록한다.

## 14. Document Control / Source-of-Truth Notes

- 이 문서는 business lifecycle, ownership, 상태 전이, Excel merge와 기존 실행 aggregate 연결의 canonical source다.
- frontend/backend endpoint 계약은 feature spec을 따른다.
- Agent Notion 문서는 external evidence로만 사용하며, 확정 내용을 feature spec과 DDL에 normalize한 뒤 구현한다.
- P0 질문은 mock 구현을 막지 않지만 real DB/Agent/Excel integration을 막는다.
