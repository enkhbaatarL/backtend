# MI 수급모델 파라미터 제안 Domain Design

## Document Control

- domain: gas / MI model execution
- status: Review
- owner: BE (platform)
- created at: 2026-07-03
- updated at: 2026-07-08
- canonical for: 파라미터 제안의 business concept, ownership, lifecycle, 상태 전이, Excel 생성 및 기존 수급모델 실행 연결 경계
- not canonical for:
  - endpoint, request/response, HTTP error contract: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - physical DDL 및 DBA rollout: 후속 ERD/DDL draft
  - Agent API: Notion `api 정의 (frontend-backend-agent)`는 외부 evidence이며 repository canonical source가 아님
  - Agent schema/DDL: `docs/design-docs/gas/mi-agent-param-proposal.md`는 Agent 소유 schema 초안이며 Backend canonical DDL이 아님
- source documents:
  - `docs/design-docs/gas/mi-agent-param-proposal.md`
  - Notion `api 정의 (frontend-backend-agent)`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes: 없음
- last synced: 2026-07-08
- open questions: 11절 참조
- integration model: **callback = 완료 신호. Backend가 Agent proposal 테이블을 read-only 직접 조회해 결과를 서빙**(시나리오 설계와 동일). CAS/hash/snapshot 저장 없음
- implementation readiness: ready for API implementation by slice. frontend mock/API contract is ready. real Agent read/callback implementation can start because Agent proposal DDL, Backend read permission, callback authentication, wrapper shape, and write-then-callback order are resolved. Remaining items are rollout/operation details: Agent/Backend physical topology pre-check, `tmd_mi_model_execpreph` DBA DDL approval/apply, workbook location/version, and payload sizing.

## 1. Feature Summary

이미 생성된 Scenario 중 **파라미터 제안 대상 Event를 기반으로 생성된 Scenario**에서 사용자가 `MI 수급모델 실행` 팝업을 열어 Agent에게 모델 파라미터 제안을 요청하고, 생성된 제안값을 검토·수정한 뒤 수급모델 실행을 접수하는 기능이다.

실행 입력 방식은 Scenario의 기반 Event에 따라 분기한다.

- `PARAMETER_PROPOSAL`: 사용자는 Excel 파일을 업로드하지 않는다. Backend가 협의된 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개를 준비하고, 사용자가 확정한 값을 Agent가 지정한 파일·시트·테이블·slot에 병합한다.
- `FILE_UPLOAD`: 기존 방식 그대로 사용자가 `DEMAND`/`SUPPLY`/`TRADE` Excel 파일 3개를 직접 업로드한다.

두 방식 모두 파일 3개가 준비된 이후에는 기존 Scenario Model Execution pipeline을 사용한다. 기존 파일 업로드 기능은 유지되며 제거·deprecated 대상이 아니다.

이 기능은 Scenario 생성 기능이 아니다. 기존 `Scenario`와 기존 `Scenario Case/Run` 실행 aggregate 사이에 위치하는 파라미터 제안 생성 요청/검토 단계다. 실제 수급모델 실행 master/history는 계속 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 소유한다.

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
- 2026-07-06 Agent DB 설계 evidence:
  - `tmm_mi_model_param_m`: Basecase Excel 위치/읽기 규칙과 사용 가능한 모델 파라미터를 관리하는 Agent configuration master
  - `tmm_mi_model_param_dimension_map`: `event_type_l2_id + dimension_key`를 직접/간접/routing/미대상 모델 파라미터에 연결하는 Agent mapping
  - `tmm_mi_model_param_proposal_m`: Agent proposal 생성/확정 payload 및 callback 전송 상태를 저장하려는 Agent transaction table
  - 앞의 두 테이블은 DDL/seed 초안이 있으나 proposal table은 DDL이 없어 physical contract가 완결되지 않음
- Backend existing identity path:
  - `MiScenario.scenario_id -> MiScenarioGroup.scenario_group_id -> MiEvent.mi_event_id/event_type_l2_id`
  - `scenarioGroupId`, `miEventId`, `eventTypeL2Id`는 Scenario에서 도출되므로 요청 시점 snapshot과 현재 관계를 구분해야 함

## 3. Proposed Domain Model

### 3.1 `ModelParamProposal`

수급모델 실행 전 한 번의 파라미터 제안 생성과 사용자 확정을 대표하는 transaction/history 성격의 resource다.

- owner: 요청 사용자
- target: 기존 `Scenario`
- upstream owner: Agent가 proposal 계산 및 Agent-side proposal row를 소유
- Backend owner:
  - FE에 노출할 proposal lifecycle projection (proposal 결과 데이터는 Agent proposal 테이블을 read-only로 직접 조회해 서빙)
  - Backend proposal request orchestration 상태(`CONFIRMING`/`PREPARING_EXECUTION`/`EXECUTING` 등 Agent가 모르는 상태)와 실행 접수 전 중복 방지
  - Excel 생성과 Scenario Case/Run 연결

  사용자 확정값(confirmation payload)은 Agent confirmation API가 Agent proposal 테이블에 저장하므로 Backend는 confirmed snapshot을 복제하지 않는다.
- shared contract:
  - `modelParamProposalId`는 Agent proposal identity이며 Backend PK로 재해석하지 않는다.
  - Backend는 Agent table을 직접 **수정하지 않고(read-only)** Agent API로만 생성/확정한다.
  - **결과 조회는 Backend가 Agent proposal 테이블을 직접 read한다.** 이는 시나리오 설계가 Agent-write 테이블을 Backend가 `scenarioGroupRepository`로 읽는 방식과 동일하다. Agent proposal DDL과 Backend read 권한은 확정됐다. 배포 전에는 Agent/Backend 물리 토폴로지(동일 schema/instance vs view/synonym)와 key type 정합만 pre-check로 확인한다.
  - 따라서 callback은 proposal 데이터를 나르지 않는 **완료 신호(signal)** 이며, Backend는 신호 수신 후 Agent 테이블을 재조회한다. 신호 유실 시 상태 재조회·폴링으로 수렴한다.
- relationship:
  - `Scenario 1:N ModelParamProposal` — proposal history를 위해 시나리오당 여러 row를 허용하되, **진행 중(in-flight) preparation은 시나리오당 최대 1개**로 제한한다(§3.2 in-flight 후보 + §10 generated column UNIQUE). callback correlation은 path `scenarioId` + 활성 row로 수렴한다. (`FAILED` 후 재제안 허용 여부는 Open; 허용 시에도 in-flight는 시나리오당 1개)
  - `ModelParamProposal 1:N ExecutionPreparation` — 같은 Agent proposal(`agnt_prpsl_id`)로 확정값 기반 수급모델 실행을 여러 번 접수할 수 있다. 실행 1회마다 새 `tmd_mi_model_execpreph` row를 만들고 input Excel 3개 세트를 새로 생성한다. 재실행 시 값 조정은 같은 `model_param_proposal_id` 재확정(Option A, §6 규칙 18)으로, 파라미터를 아예 생성부터 다시 하면 새 `model_param_proposal_id`가 발급된다.
  - `ExecutionPreparation 0..1 ScenarioCase` — 실행 접수 성공 후 해당 row에 새 `case_id`를 연결한다. 실행 상태/terminal 이력은 기존 Case/Run이 관리한다.

### 3.2 Lifecycle

Backend orchestration 상태와 Agent 상태를 **분리**한다. Agent 상태(`PROCESSING/PROPOSED/CONFIRMED/FAILED`)는 Agent proposal 테이블이 source of truth이며 Backend는 복제하지 않고 read-only로 조회한다. Backend는 Agent가 모르는 orchestration 상태만 저장하며, 이 상태는 monotonic이다.

Backend orchestration 상태:

| Backend status | 의미 | 다음 상태 |
|---|---|---|
| `REQUESTING` | execution-preparation 생성·commit 완료, Agent proposal 요청 응답 또는 callback 대기 중 | `PROPOSAL_PENDING`, `REQUEST_FAILED` |
| `PROPOSAL_PENDING` | Agent proposal 요청 접수됨, 제안 생성·사용자 검토 대기 (Agent `PROCESSING`/`PROPOSED`를 포괄) | `CONFIRMING`, `REQUEST_FAILED` |
| `CONFIRMING` | 사용자 실행 요청을 검증하고 Agent confirmation 수행 중 | `PREPARING_EXECUTION`, `EXECUTION_FAILED` |
| `PREPARING_EXECUTION` | Agent confirmation 성공, Excel 3개 생성 중(confirmed params 파생, case 미생성) | `EXECUTING`, `EXECUTION_FAILED` |
| `EXECUTING` | 기존 model execution 접수 완료(case INSERT → `tcm_filel(SCNRCASE+case_id)` 기록 → dispatch) | terminal model execution은 기존 Case/Run이 관리 |
| `EXECUTION_FAILED` | confirmation 이후 Excel 생성·업로드·실행 접수 실패 | 재시도 정책 필요 |
| `REQUEST_FAILED` | Agent proposal 생성 요청 또는 callback 처리 실패 | terminal; 재제안 정책 필요 |

전이 예시:

```
정상: REQUESTING → PROPOSAL_PENDING → CONFIRMING → PREPARING_EXECUTION → EXECUTING
실패: REQUESTING → REQUEST_FAILED
      CONFIRMING / PREPARING_EXECUTION → EXECUTION_FAILED
```

Backend ↔ Agent 상태 매핑:

| Backend orchestration status | Agent status (read-only, source of truth) | FE 표시 근거 |
|---|---|---|
| `REQUESTING` | (아직 없음) | Backend 상태 |
| `PROPOSAL_PENDING` | `PROCESSING` / `PROPOSED` | 제안 생성 중 vs 검토 가능 세부 구분은 Agent 테이블 재조회로 판정 |
| `CONFIRMING` | `PROPOSED` → `CONFIRMED` 진행 중 | Backend 상태 |
| `PREPARING_EXECUTION` | `CONFIRMED` | Backend 상태 |
| `EXECUTING` | `CONFIRMED` | 이후 terminal은 기존 Case/Run |
| `EXECUTION_FAILED` | `CONFIRMED` (Backend 후단계 실패) | Backend 상태 |
| `REQUEST_FAILED` | `FAILED` 또는 Agent 요청 실패 | Backend 상태 |

핵심: FE의 "검토 가능(`PROPOSED`)" 신호는 Backend 상태가 아니라 **Agent proposal 테이블 read**로 얻는다. Backend `PROPOSAL_PENDING`은 Agent `PROCESSING`과 `PROPOSED`를 모두 포괄하며, 두 세부 상태 구분은 Agent 테이블 재조회로 한다. Agent 외부 계약은 `PROCESSING/PROPOSED/CONFIRMED/FAILED`만 사용한다.

in-flight 상태 후보(활성 preparation 중복 방지 대상): `REQUESTING`, `PROPOSAL_PENDING`, `CONFIRMING`, `PREPARING_EXECUTION`. `EXECUTING`은 기존 `ScenarioCase.actv_scnr_id`(RUNNING) UNIQUE가 실행 중복을 통제하므로 preparation in-flight 제약에서 제외한다(재실행 유리).

### 3.3 Agent-owned configuration and proposal model

| Agent table | Domain responsibility | Backend usage boundary |
|---|---|---|
| `tmm_mi_model_param_m` | Excel/Basecase 읽기 위치, 파라미터 활성 여부, slot/value 해석 규칙 | callback `modelTarget` 검증 근거. Backend 복제 master를 만들지 않음 |
| `tmm_mi_model_param_dimension_map` | Event Type L2의 scenario dimension을 모델 파라미터로 routing | proposal 내부 parameter routing 근거. `inputMode` eligibility 판정에는 사용하지 않음 |
| `tmm_mi_model_param_proposal_m` | Agent 계산 상태, proposal/confirmation payload, Backend callback delivery 상태 | Agent lifecycle source. **Backend가 read-only로 직접 조회해 FE에 서빙(write 금지)**. Backend orchestration 상태는 별도 Backend 행에 보관 |

`callback_status`는 Agent가 Backend에 callback을 전달했는지 나타낸다. Backend의 callback 처리 완료나 model execution 접수 상태를 의미하지 않는다.

### 3.4 `ModelExecutionInputMode`

Scenario의 기반 Event가 어떤 수급모델 실행 입력 workflow를 사용해야 하는지 나타내는 derived policy다.

| Mode | 대상 | 입력 준비 방식 |
|---|---|---|
| `PARAMETER_PROPOSAL` | 파라미터 제안 대상 Event 기반 Scenario | Agent 제안 + 사용자 수정 + Backend Excel 3개 생성 |
| `FILE_UPLOAD` | 파라미터 제안 비대상 Event 기반 Scenario | 사용자 Excel 3개 직접 업로드 |

- mode는 사용자가 선택하는 값이 아니다.
- Backend가 `Scenario -> ScenarioGroup -> event_type_l2_id` 관계로 판정한다.
- FE는 Backend가 제공한 mode로 팝업 UI를 분기한다.
- `event_type_l2_id IN (3, 12, 13, 14, 15, 16, 20)`이면 `FILE_UPLOAD`다.
- 그 외 모든 `event_type_l2_id`는 `PARAMETER_PROPOSAL`이다.
- 신규 Event Type L2도 별도 제외 목록에 추가되지 않는 한 `PARAMETER_PROPOSAL`로 판정한다.
- 이 규칙은 기존 `MiScenarioGroup.eventTypeL2`로 계산하므로 eligibility 전용 컬럼이나 기존 데이터 backfill이 필요하지 않다.
- 기존 `templateAvailable`은 Scenario Design template 존재 여부이므로 이 정책에 재사용하지 않는다.

### 3.5 Callback delivery (완료 신호)

- callback은 proposal 데이터를 나르지 않는 **완료 신호**다. Backend는 신호 수신 후 Agent proposal 테이블을 재조회해 canonical 결과·terminal 상태를 얻는다. (시나리오 설계의 "신호 + 재조회"와 동일)
- Agent Notion confirmed contract는 proposal request body를 사용하지 않는다. Backend가 callback endpoint/URL을 결정해 Agent 팀에 전달하고, Agent는 전달받은 URL을 배포 운영 설정값으로 등록·관리한다.
- Backend는 Notion `{status,message,data}` wrapper를 수신해 내부 신호 DTO로 normalize한다(성공/실패는 공용 `AgentCallbackResult`로).
- callback body의 `modelParamProposalId`, `scenarioId`, `scenarioGroupId`, `eventId`는 신호가 겨냥한 대상을 request snapshot과 교차 검증하는 용도다.
- **canonical 상태 판정은 callback payload가 아니라 Agent 테이블 재조회로 한다.** 따라서 중복/지연/순서역전 callback은 재조회로 수렴하는 **멱등 신호**이며, payload hash / CAS / terminal overwrite 보호가 필요하지 않다.
- Backend orchestration 상태는 monotonic이다. callback이 Agent HTTP 응답보다 먼저 도착할 수 있으며, 이때 `REQUESTING` 행을 `scenarioId`로 찾고 Agent proposal 테이블을 재조회해 Agent proposal ID와 canonical 상태를 채운다. 이후 늦게 도착한 Agent 응답은 상태를 `PROCESSING`으로 되돌리지 않는다.
- Agent 응답 경로와 callback 경로가 서로 다른 Agent proposal ID를 관찰하면 기존 값을 덮어쓰지 않고 correlation conflict로 처리한다. 같은 ID의 중복 갱신은 no-op이다.
- Backend가 write하는 것은 자기 orchestration 행(상태/`case_id`)뿐이며, 재조회 결과를 기준으로 갱신한다. SSE는 그 write commit 이후 발행한다.
- 신호 유실 대비: 재연결 시 상태 재조회 + 폴백 폴링으로 수렴한다.
- 전제: Agent가 proposal 테이블 commit 이후 신호를 발행한다(write-then-callback). Backend가 선행 `REQUESTING` 행과 조건부 갱신으로 응답/callback 순서 역전을 수렴시키므로 별도 correlation/event ID 확장은 불필요하다.
- endpoint, request/response field, 인증 및 HTTP error 계약은 Feature Spec 5.4절을 따른다.

### 3.6 Execution-preparation 생성 및 Agent proposal ID 수렴

Backend execution-preparation은 Agent proposal ID를 받은 뒤 생성하지 않는다. 동일 Scenario의 동시 제안 요청을 먼저 차단하고 callback이 Agent HTTP 응답보다 먼저 도착하는 경우에도 대상을 찾을 수 있도록 다음 순서를 사용한다.

1. Scenario, permission, `inputMode`, active preparation/model execution 중복을 검증한다.
2. `scenarioId`, 요청자, 요청 시점 snapshot과 Backend 상태 `REQUESTING`을 가진 execution-preparation을 생성하고 commit한다. 이 시점의 Agent proposal ID는 `null`이다.
3. 선행 transaction이 끝난 뒤 Agent proposal 요청 API를 호출한다. 외부 호출 동안 DB transaction을 열어두지 않는다.
4. Agent HTTP 응답 경로는 응답의 Agent proposal ID를, callback 경로는 Agent proposal 테이블 재조회로 확인한 ID를 조건부 저장한다. 두 경로 중 먼저 도착한 값이 write-once로 설정된다.
5. 나중에 도착한 경로는 같은 ID면 no-op으로 수렴하고, 다른 ID면 기존 값을 덮어쓰지 않고 실패 처리한다.
6. Agent HTTP 응답 직전 persisted 상태를 다시 읽어 callback이 먼저 반영한 terminal 상태를 `PROCESSING`으로 되돌리지 않고 FE에 반환한다.

Agent 요청이 명시적으로 실패하면 preparation에 실패 상태와 사유를 기록한다. timeout은 Agent commit 여부가 불명확하고 late callback이 올 수 있으므로 preparation을 즉시 삭제하지 않으며, callback 또는 Agent proposal 테이블 재조회로 최종 상태를 수렴시킨다.

## 4. Table Design Draft

### 4.1 권고안: Agent proposal table과 분리된 Backend proposal request orchestration table

Agent의 `tmm_mi_model_param_proposal_m`이 proposal 계산 이력·payload를 소유하고 **Backend가 이를 read-only로 직접 조회**하므로, Backend는 proposal 데이터를 복제 저장하지 않는다. Backend persistence가 필요한 최소 이유는 proposal 자체 보관이나 수급모델 실행 이력 저장이 아니라 Agent가 모르는 **Backend execution-preparation 상태, 사용자 ownership, Case 연결**이다. 사용자 확정 파라미터(confirmation payload)는 Agent confirmation API가 Agent proposal 테이블에 저장하므로 Backend는 별도 confirmed snapshot을 복제하지 않는다. 실제 수급모델 실행 master/history는 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 계속 source of truth다.

후속 ERD review 대상 후보는 Agent proposal ID를 external reference로 갖는 **thin Backend execution-preparation 행**이다(proposal 데이터 snapshot·hash 없이 실행 준비 상태와 Case 포인터만 저장). 물리명 `tmd_mi_model_execpreph`는 유지하되, 업무 의미는 `수급모델 실행 이력`이 아니라 `파라미터 제안 기반 수급모델 실행 준비 이력`이다. 기존 `tmm_mdlparamrecmh` 후보명은 보류한다.

후보 컬럼 (proposal 데이터는 Agent 테이블 read이므로 제외 — proposed snapshot / payload hash / correlation·event ID 없음):

| Logical field | Physical candidate | 목적 |
|---|---|---|
| backend preparation key | naming review 필요 | Backend surrogate PK |
| Agent proposal ID | `agnt_prpsl_id` 후보 | Agent proposal reference/query key. 타입은 Agent PK `tmm_mi_model_param_proposal_m.model_param_proposal_id`(`INT UNSIGNED`)와 동일한 **`int unsigned`**로 고정. 선행 생성 시 `null`, 응답/callback에서 row 단위 조건부 저장(write-once). 같은 proposal 반복 실행을 위해 UNIQUE 금지 |
| Scenario ID | `scnr_id` | 대상 Scenario logical reference |
| active(in-flight) Scenario ID | `actv_scnr_id` | STORED generated column. in-flight 상태 **AND** `del_yn='N'`이면 `scnr_id`, 아니면 NULL. UNIQUE로 시나리오당 in-flight 1건 보장. 표준단어 `ACTV`(활성) + `SCNR`(시나리오)를 사용하며 기존 `ScenarioCase.actv_scnr_id` 패턴과 정합 |
| Scenario Case ID | `case_id` | 실행 접수 후 기존 `tmd_mdl_scnrcasem` Case 포인터, nullable, UNIQUE. 실행 1회 준비 row 하나가 Case 하나에 연결된다 |
| request user | `req_usr_no` | 요청자/소유자 |
| orchestration status | naming review 필요 | Agent status와 분리된 Backend lifecycle status (monotonic, §3.2 7종) |
| failure reason | `fail_rsn_ctnt` 후보 | Backend orchestration 실패 사유 |
| requested/confirmed timestamps | naming review 필요 | lifecycle timestamp |
| common audit | 공통 5종 | `reg_user_id`, `reg_dtm`, `mod_user_id`, `mod_dtm`, `del_yn` |

공통 audit의 실제 JPA 근접 패턴은 `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn`이다. 명명 규칙 요약의 `reg_user_id`와 코드가 충돌하므로 canonical DDL 작성 전 DBA 기준을 확인한다. `에이전트`, `제안`, `확정`, `실행 준비` 및 timestamp 조합은 데이터 사전 검토가 필요하므로 물리명으로 확정하지 않는다.

### 4.2 저장 대안 비교

| 대안 | 장점 | 문제 | 판단 |
|---|---|---|---|
| **Agent proposal 테이블 read-only 공유 read** | **중복 저장 제거, callback을 신호로 격하(CAS/hash 불필요), 기존 시나리오 설계와 동일** | **Agent 물리 스키마 결합. DDL/read 권한은 확정, 배포 전 물리 토폴로지 pre-check 필요** | **채택** |
| Backend DB proposal snapshot | 재조회, 재시작, audit, idempotency, 실행 연결 가능 | proposal 데이터 이중 저장, callback CAS/hash 필요 | thin orchestration 행만 채택(proposal 데이터는 복제 안 함) |
| Redis only | mock 구현이 빠르고 기존 validation cache와 유사 | TTL/재시작 유실, 실행 감사 불가 | mock adapter에 한정 |
| FE round-trip only | Backend 저장 최소화 | callback 이후 FE 전달, reload, 위변조 검증, 다중 인스턴스 문제 | 채택하지 않음 |
| Agent 조회 GET API 의존 | Agent DB 결합 회피 | Agent GET 신설 필요, 기존 시나리오 설계도 GET 없이 DB read | 채택하지 않음(직접 read로 대체) |

## 5. Relationship Design

```text
MiScenario (existing)
  1
  |
  N
ModelParamProposal (new candidate)
  1
  |
  N
ExecutionPreparation (`tmd_mi_model_execpreph`)
  0..1
  |
  1
ScenarioCase / ScenarioCaseRunHistory (existing)
  |
  N
tcm_filel: DEMAND / SUPPLY / TRADE generated files
```

- execution-preparation은 Scenario와 Agent proposal에 종속되지만 Scenario Case보다 먼저 생성된다.
- 같은 Agent proposal로 여러 번 실행 접수할 수 있으며, 실행 1회마다 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만든다.
- 모델 실행 상태와 terminal 이력은 `tmd_mi_model_execpreph`에 복제하지 않고 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`를 source of truth로 유지한다.

## 6. Business Rules

1. 제안 요청 대상 Scenario는 존재하고 삭제되지 않아야 한다.
2. 요청 사용자는 기존 Next Gen Scenario/Model Execution permission을 만족해야 한다.
3. `PARAMETER_PROPOSAL` mode Scenario만 proposal 생성과 JSON execution을 사용할 수 있다.
4. `FILE_UPLOAD` mode Scenario는 proposal 생성을 거절하고 기존 multipart execution만 사용할 수 있다.
5. mode와 다른 실행 입력 방식을 요청하면 Backend가 거절하며 client가 mode를 override할 수 없다.
6. 한 실행 준비 시도에서 proposal 생성 요청은 1회만 허용한다.
7. 같은 Scenario에 `PROCESSING` proposal(=in-flight preparation) 또는 `RUNNING` model execution이 있으면 새 파라미터 생성 요청을 진입에서 차단한다(RUNNING pre-check fast-fail). `RUNNING` 판정은 `MiScenario` lock 스코프에서 `tmd_mdl_scnrcasem`을 조회하며, pre-check와 실제 실행 접수 사이 race는 기존 `ScenarioCase.actv_scnr_id` UNIQUE가 backstop으로 막는다. in-flight preparation 중복은 별도로 preparation in-flight UNIQUE가 막는다.
8. callback(신호)의 `modelParamProposalId`, `scenarioId`, `scenarioGroupId`, `eventId`를 저장된 request snapshot과 교차 검증한다.
9. callback은 신호이므로 중복/지연/순서역전 재전송은 Agent 테이블 재조회로 수렴하는 멱등 처리다. Backend orchestration 상태는 monotonic이며 `PROCESSING` 이후 단계에서 도착한 신호는 no-op이다. payload hash / CAS는 사용하지 않는다.
10. FE 표에는 `values[].proposedValue`만 값 cell로 표시하며 `basecaseValue`는 표시하지 않는다. 사용자는 `proposedValue`만 수정할 수 있다.
11. `parameterName`, `modelTarget`, parameter/value rationale, slot-axis 값과 `basecaseValue`는 Agent proposal 조회 결과 기준 immutable이다.
12. 최종 실행 요청은 전체 confirmation parameter array를 보낸다. 사용자가 수정한 row는 수정된 `proposedValue`, 수정하지 않은 row는 Agent가 생성한 원본 `proposedValue`를 그대로 포함한다.
13. Backend는 확정 시점의 Agent proposal array와 비교해 `values[].proposedValue` 변경만 허용하고, 검증된 전체 array를 Agent confirmation API로 전달한다.
14. Agent confirmation 성공 후에만 Excel 3개 생성과 기존 model execution 접수를 진행한다.
15. Excel 병합은 Agent `modelTarget`의 required `modelFile`, `modelSheet`, `tableName`, `valueFieldNames`, `slotAxesFieldNames`를 사용한다.
16. 생성된 파일명은 기존 `DEMAND`/`SUPPLY`/`TRADE` 역할 검증을 통과해야 한다.
17. 두 mode 모두 준비된 파일 3개를 기존 `tcm_filel(SCNRCASE + case_id)`와 Kubernetes Job dispatch 흐름에 전달한다.
18. 최종 실행 버튼을 누를 때마다 별도 수급모델 실행으로 접수하며, 실행 1회마다 새 `case_id`와 run history를 만들고 input Excel 3개를 새 `case_id` 기준으로 새로 생성한다(값 변경 없이 재실행해도 Excel은 case_id에 묶이므로 재생성). 재실행 파라미터 경로는 두 가지다. (a) **값 조정 재실행 = 같은 `model_param_proposal_id` 재확정(Option A)**: 확정 API를 같은 id에 재호출해 `confirmation_parameters`를 갱신하며, Agent `proposal_parameters`(최초 제안)는 불변이다. (b) **파라미터 생성부터 다시 = 새 `model_param_proposal_id`**: 새 proposal 생성 요청으로 새 `agnt_prpsl_id`를 발급한다(규칙 7 RUNNING pre-check 적용). 두 경우 모두 같은 Scenario에 `RUNNING` model execution이 있으면 기존 `ScenarioCase.actv_scnr_id` unique guard로 새 실행을 차단한다.
19. Agent request 생성 시 `scenarioId`로부터 도출한 `scenarioGroupId`, `miEventId`, `eventTypeL2Id`를 함께 검증하고 request snapshot으로 고정한다.
20. callback의 식별자들은 생성 시점 snapshot과 모두 일치해야 하며, 현재 Scenario 관계가 이후 변경된 경우 자동 덮어쓰지 않는다.
21. `event_type_l2_id IN (3, 12, 13, 14, 15, 16, 20)`이면 `FILE_UPLOAD`, 그 외에는 `PARAMETER_PROPOSAL`로 판정한다.
22. dimension mapping의 존재 여부나 활성 model parameter 수는 `inputMode`를 변경하지 않는다. proposal 생성 이후 parameter routing에만 사용한다.
23. 지연 callback(신호)은 Agent 재조회로 수렴하며 Backend orchestration 상태를 되돌리지 않는다(monotonic no-op).
24. Backend orchestration 행 write commit 이후에 SSE를 발행한다.
25. Backend는 Agent 호출 전에 `scenarioId` 기준 `REQUESTING` execution-preparation을 생성·commit하고, 외부 호출 동안 해당 DB transaction을 유지하지 않는다.
26. Agent 응답과 callback은 Agent proposal ID를 write-once 방식으로 채운다. 같은 ID는 no-op, 다른 ID는 correlation conflict이며 기존 값을 덮어쓰지 않는다.
27. Agent 요청 timeout 시 late callback 가능성을 보존하기 위해 preparation을 즉시 삭제하지 않는다.

## 7. Extension Considerations

- proposal history UI가 필요해질 수 있으므로 Scenario당 여러 proposal row를 허용하는 1:N 형태를 유지한다. (원본 proposal 이력과 사용자 확정값은 모두 Agent 테이블이 보관하므로 Backend는 별도 confirmed snapshot을 저장하지 않는다.)
- Excel mapping schema가 변경될 수 있으므로 Agent proposal 테이블의 원본 JSON은 lossless하게(Agent 소유) 유지된다.
- 사용자 수정 필드가 `values[].proposedValue` 외로 확장되면 mutable field allow-list를 versioning해야 한다.
- Agent contract version 또는 proposal schema version 필드를 후속 검토한다.
- callback의 `completedAt`, structured `errorCode`는 Agent 확장 후보지만 Backend 수신 시각과 wrapper `message`로 시작할 수 있다.

## 8. Data Access Recommendation by Responsibility

- Backend orchestration 행 write: JPA
  - 신호 수신 후 Agent 재조회 결과로 상태 갱신 transaction에 적합 (CAS 불필요, monotonic guard)
- proposal 데이터 retrieval: Agent proposal 테이블 read-only JPA entity 또는 projection
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
- 가장 최근 수급모델 실행에 사용된 확정 파라미터 조회 API
- 가장 최근 수급모델 실행의 원 Agent 제안 파라미터 조회 API
- 가장 최근 수급모델 실행 input Excel 3개 파일 조회 API
- flat parameter array와 `values[].proposedValue` 단일 mutable field validation

## 10. SQL / DBA Implications

- proposal 데이터는 Agent 테이블 read이나, Backend proposal request orchestration 상태는 Agent DB에 맡길 수 없으므로 thin Backend persistence는 별도 검토가 필요하다. 단, 실제 실행 이력은 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`에 남긴다.
- Agent proposal 테이블 DDL과 Backend read 권한은 확정됐다. Backend가 read하려면 배포 전 동일 schema/instance인지 view/synonym 경유인지와 key type 정합을 확인해야 한다.
- 최소 index 후보:
  - Agent proposal ID non-unique index (반복 실행 이력 조회/callback 후보 조회)
  - `(scnr_id, del_yn, exec_prep_id desc)` 시나리오별 최신 preparation 조회
  - `case_id` UNIQUE nullable
- 최근 실행 기준 파라미터 조회는 최신 `tmd_mdl_scnrcasem.case_id` → `tmd_mi_model_execpreph.case_id` → Agent proposal table 순서로 수행한다.
- 최근 실행 input Excel 파일 조회는 최신 `case_id` 기준 기존 `tcm_filel(SCNRCASE + case_id)` 저장 규칙을 재사용한다.
- active(in-flight) preparation 중복 방지는 `ScenarioCase.actv_scnr_id`와 동일하게 STORED generated column + UNIQUE로 확정한다. 재실행 흐름이 이전 preparation을 soft delete하므로, 식에 **`del_yn='N'`을 반드시 포함**해 soft delete된 행이 유령 충돌을 일으키지 않게 한다.

  ```
  actv_scnr_id =
    if(preparation_status in ('REQUESTING','PROPOSAL_PENDING','CONFIRMING','PREPARING_EXECUTION')
       and del_yn = 'N', scnr_id, null)   -- STORED generated column
  UNIQUE (actv_scnr_id)
  ```

  `EXECUTING`은 기존 `actv_scnr_id`(RUNNING) UNIQUE가 실행 중복을 통제하므로 제외한다. 두 UNIQUE guard(preparation in-flight vs case active)는 서로 다른 계층에서 작동한다. 추가로 기존처럼 `MiScenario` pessimistic lock으로 동시 요청을 직렬화한다.
- JSON column 크기와 최대 proposal parameter/value 수를 측정해야 한다.
- Agent proposal/confirmation JSON root는 array다. `proposal_parameters`, `confirmation_parameters`는 `JSON_TYPE(...) = 'ARRAY'` CHECK를 유지한다.
- schema는 수동 DBA 배포하며 `ddl-auto=validate` mapping 적용 전에 DDL이 선행되어야 한다.

### 10.1 Agent ERD review

- change classification: Agent 신규 master 1개, mapping 1개, proposal transaction 1개. Backend schema 변경 여부는 execution-preparation table 승인 전 미확정이다.
- normalization:
  - parameter master의 `basecase_column_roles`, `basecase_row_filter`, `basecase_stop_rule`은 자체 DSL 문자열이므로 parser/version 계약이 필요하다.
  - dimension mapping은 row 단위로 분리되어 1NF~BCNF 방향은 적절하다.
  - proposal의 `scenario_id`, `scenario_group_id`, `mi_event_id` 동시 저장은 request snapshot 목적의 의도적 비정규화다. 생성 시 일치 검증과 immutable 규칙이 필요하다.
- constraints:
  - nullable `model_param_id`를 포함한 UNIQUE는 MySQL에서 `NULL` row 중복을 막지 못한다. `indirect`/`na` natural key를 별도로 보장해야 한다.
  - `direct`/`routing -> model_param_id NOT NULL`, `na -> model_param_id IS NULL` 조건부 무결성이 필요하다.
- DDL blocker:
  - proposal table 전체 DDL 부재는 해소됐다(2026-07-07 DB 반영본 수령). 잔여는 dimension mapping nullable UNIQUE/조건부 무결성 정정이며 Backend proposal read/callback API의 런타임 critical path는 아니다.
- index:
  - `(event_type_l2_id, dimension_key)`는 routing 조회에 적합하고 `model_param_id` FK index도 필요하다.
  - low-cardinality `use_yn`, `mapping_method`, status 단독 index는 두지 않는다.
  - proposal callback retry index는 실제 조회 조건과 건수를 기준으로 `EXPLAIN` 후 결정한다.
- naming:
  - `uk_*`, `idx_*`, `fk_*`는 `{table_name}_{ux|ix|fk}{nn}` 규칙과 불일치한다.
  - Agent audit user column `VARCHAR(100)`은 Backend `BaseEntity`의 `VARCHAR(50)`과 다르므로 같은 schema라면 정합화가 필요하다.
- rollout: Agent proposal table read 계약은 확정됐고, dimension mapping 정정 DDL은 Agent/DBA rollout 항목으로 분리한다.

## 11. Risks / Open Questions

### P0 — real integration rollout 전 필수

Resolved and no longer P0: callback authentication, Backend read permission, Agent proposal table DDL, write-then-callback order, callback wrapper shape, and confirmation retry/idempotency policy. Canonical API details are in the feature spec.

1. Agent/Backend 물리 토폴로지(동일 schema/instance vs view/synonym)와 key type 정합 pre-check. read 권한 자체는 확정.
2. Backend execution-preparation DDL rollout: `tmd_mi_model_execpreph` table/subject DBA 최종 승인, DDL 적용, `ddl-auto=validate` 전 배포 순서 확인.
3. required로 확정한 Agent `tableName` 필드의 Agent payload 반영 배포 시점.
4. 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일의 저장 위치, version 선택 규칙, owner.
5. `values[].basecaseValue`/`proposedValue`와 dynamic slot-axis 값의 허용 JSON type, null, precision 정책.
6. parameter/value row 최대 건수와 JSON column size 한도.
7. callback 신호 유실 시 폴백 폴링 interval/timeout.
8. `schemaVersion` 등 Agent 스키마 진화 시 Backend read 결합 versioning 방법. `completedAt`/`error_code`는 Agent schema에 이미 존재한다.

### P1 — 운영 전 확정

1. `FAILED` proposal 이후 같은 실행 준비에서 재제안을 허용할지.
2. `CONFIRMED` 이후 Excel 생성 실패 시 사용자가 같은 proposal로 재시도할지.
3. proposal 보존 기간과 개인정보/민감정보 분류.
4. SSE 재연결 시 missed event를 상태 조회로 수렴시키는 polling interval/timeout.
5. (resolved → Option A) 값 조정 재실행은 같은 `model_param_proposal_id` 재확정으로 `confirmation_parameters`를 갱신한다(최신 스냅샷만 유지). 변경 없음 재실행은 재확정 없이 기존 confirmed 값을 재사용한다. Agent 확정 API가 이미 `CONFIRMED`인 proposal의 재확정을 허용하도록 Agent가 수정 확정했다(2026-07-09 회신) → Option A 전제 충족. 과거 실행별 확정값 복원이 요구되면 `tmd_mi_model_execpreph`에 실행별 confirmed snapshot JSON 추가를 fallback으로 검토한다.

## 12. Exit Criteria / Handoff

- mock API contract drafting: ready
- real DB implementation: ready by slice. Agent read/callback/orchestration 구현은 착수 가능하고, runtime 배포는 Agent/Backend 물리 토폴로지 pre-check와 `tmd_mi_model_execpreph` DBA DDL 적용 이후 가능하다. Excel merge/dispatch 구현은 workbook location/version 및 payload sizing 확정이 필요하다.
- `backend-feature-spec` handoff:
  - endpoint family, DTO, callback, SSE, final execution request를 고정한다.
- `erd-change-review` handoff:
  - Agent proposal 테이블 read-only 조회 + thin Backend execution-preparation(orchestration) 책임 분리
  - Agent proposal ID non-unique index (반복 실행 허용)
  - active(in-flight) preparation uniqueness — `del_yn` 포함 STORED generated column + UNIQUE
  - confirmed snapshot은 Backend에 저장하지 않음(Agent proposal 테이블이 확정값 보관)
  - `tmd_mi_model_execpreph.case_id` nullable UNIQUE 포인터로 execution-preparation row 1건과 Case 1건을 연결
- implementation readiness: ready for API implementation by slice; full real execution rollout depends on the P0 rollout items above.

## 13. Dictionary Updates

- 사용 가능한 기존 용어:
  - 모델 `MDL`
  - 파라미터 `PARAM`
  - 추천 `RECM`
  - 시나리오 `SCNR`
  - 상태 `STAT`
  - 요청 `REQ`
  - 실패 `FAIL`
- 이번 설계에서 `db-term-dictionary-sheet2.csv`에 append-only로 신규 등록한 용어 (No 382~384):
  - 준비 `PREP` (PREPARATION) — execution-preparation 테이블/컬럼 명명용
  - 제안 `PRPSL` (PROPOSAL) — Agent 파라미터 제안 `agnt_prpsl_id` 명명용
  - 에이전트 `AGNT` (AGENT) — Agent 참조 컬럼 명명용
- `확정`은 기존 어휘(`초안 DRFT`, `최종저장 FNSV`)로 충분하고 Backend가 confirmed snapshot을 저장하지 않으므로 별도 등록하지 않는다.
- Agent 초안(`tmm_mi_model_param_proposal_m` 등)의 물리명은 Agent 소유이므로 Backend DDL로 복제하지 않는다. Backend는 위 3개 용어로 자체 execution-preparation 물리명을 구성한다.

## 14. Document Control / Source-of-Truth Notes

- 이 문서는 business lifecycle, ownership, 상태 전이, Excel merge와 기존 실행 aggregate 연결의 canonical source다.
- frontend/backend endpoint 계약은 feature spec을 따른다.
- Agent Notion과 Agent DB 설계 문서는 source evidence다. Agent physical schema는 정정된 canonical DDL로, Backend physical schema는 별도 Backend DDL draft로 승격해야 한다.
- P0 질문은 Agent 연동 없는 frontend mock 구현을 막지 않지만 real DB/Agent/Excel integration을 막는다.
