# MI 파라미터 제안 기반 수급모델 실행 — Backend API 인벤토리

## Document Control

- domain: gas / MI model execution
- status: Draft (구현 착수 전 API inventory)
- owner: BE (platform)
- created at: 2026-07-08
- updated at: 2026-07-09
- purpose: "MI 파라미터 제안 기반 수급모델 실행 준비" 기능 구현을 위해 개발/변경해야 할 Backend API를 빠짐없이 도출하고, 기존 재사용 대상과 신규 대상을 구분한다.
- canonical references:
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` (API 계약)
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md` (lifecycle/규칙)
  - `docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md` (`tmd_mi_model_execpreph`)
  - `docs/design-docs/gas/mi-agent-param-proposal.md` (Agent 테이블 `tmm_mi_model_param_proposal_m`, read-only)
  - `docs/design-docs/gas/mi-scenario-model-execution-workflow-design.md` (기존 case/run 실행 흐름)
- note: 이 문서는 canonical 계약을 대체하지 않는다. endpoint/DTO 계약은 feature-spec, lifecycle는 design doc이 canonical이다.
- change log:
  - 2026-07-08: 최초 작성.
  - 2026-07-08: model 실행 terminal callback(B2) 추가, `근거 문서` 컬럼·상태전이 매트릭스(1.1) 추가. **O1(Agent CONFIRMED 재확정 허용 여부)를 Backend 설계 결정 Resolved로 갱신** — 이미 CONFIRMED인 proposal의 재확정 허용을 전제로 값 조정 재실행 = Option A(같은 `model_param_proposal_id` 재확정) 채택, 실제 연동 전 Agent 배포/검증 gate 유지.
  - 2026-07-08: 로컬 연동 테스트용 Agent VM base URL(`http://40.82.137.178:8002`) 및 X1/X2 테스트 endpoint를 0.1절에 기록. 환경별 runtime 설정값이며 canonical API path를 대체하지 않는다.
  - 2026-07-09: A4a current-state collection GET 추가. FE 팝업 진입 시 `scenarioId`만으로 생성중/제안완료/확정 후 실행 전·실행중/최신 실행 확정 파라미터 표시 상태를 결정한다.
  - 2026-07-09: A4a terminal 실패 정책 정리. latest Run `FAILED`는 `EXECUTION_FAILED` + `modelExecutionStatus=FAILED`로 표시하며, Backend confirmation/Excel/file/Case/dispatch 실패는 `EXECUTION_FAILED` + `modelExecutionStatus=null`로 구분한다.
  - 2026-07-09: SSE scope와 Agent proposal read path 확정. A5는 scenario-scope 신규 SSE endpoint로 유지하고, Agent proposal table은 기존 Backend DB/schema에서 직접 read한다.
  - 2026-07-09: `tmd_mi_model_execpreph` DBA DDL 적용 완료 확인. O3는 적용 gate가 아니라 runtime validate/index/constraint 확인 항목으로 전환.
  - 2026-07-09: proposal 생성 실패 후 재제안 정책 확정. `REQUEST_FAILED`/Agent `FAILED` 후 새 proposal 생성 요청을 허용하되 실패 row는 재사용하지 않는다.

---

## 0. 코드 대조로 확인한 현재 상태 (신규/변경 판정 근거)

| 확인 항목 | 코드 근거 | 판정 영향 |
|---|---|---|
| `parameterProposalTarget` 필드 | `ScenarioGroupGraphRes.java:33`, `ScenarioGroupServiceImpl.resolveParameterProposalTarget()` | graph API는 **이미 반영 완료** |
| `ModelInputMode` enum + 제외목록 `{3,12,13,14,15,16,20}` | `ModelInputMode.java:23` | 판정 로직 존재. options API에서 재사용만 하면 됨 |
| multipart `POST /model-executions` | `ScenarioModelExecutionDedicatedController.java:44` (`dedicated` profile) | **기존재사용** (inputMode 가드만 추가) |
| `GET /model-executions/{runId}` 상태 조회 | `ScenarioModelExecutionController.java:44` (`!dedicated` profile) | **기존재사용** |
| model 실행 terminal callback | `FundamentalModelRunDedicatedController.java:41` → `/api/v1/mi/fundamental-model/runs/{runId}/callback` | **기존재사용** (K8s Job 완료 → Case/Run terminal 전이) |
| `AgentCallbackResult` 공용 enum | `AgentCallbackResult.java` (success/failure/error 수용) | 콜백 boundary normalize에 재사용 |
| 기존 Agent 콜백 패턴 | `ScenarioDesignCallbackController` (`@RequiredPermissionRules(Role.SYSTEM ...)` + `CustomJwtDecoder`) | 콜백 인증 **패턴 재사용** |

> 주의: multipart submit(A7)은 `dedicated`, runId 상태조회(A11)는 `!dedicated` 컨트롤러에 있고, B2는 dedicated 명칭 controller이나 profile 제한이 없다. JSON execution과 신규 proposal API의 프로파일 배치 정책을 구현 시 결정해야 한다(열린 이슈 O5). 기존 `ScenarioModelExecutionServiceImpl`의 submit/status 흐름은 구현되어 있으나 multipart 파일 수신 중심이므로, generated Excel bytes 경로를 태우는 내부 추출/오버로드가 필요하다.

---

## 0.1 Local Agent VM Test Endpoint (non-canonical)

Agent 팀이 로컬 연동 테스트용으로 제공한 VM 배포 endpoint다. 아래 URL은 **환경별 runtime 설정값**이며, feature-spec의 canonical Agent API path를 대체하지 않는다. Backend 구현 시 파라미터 제안 Agent client의 base URL 설정으로 분리한다.

| 항목 | 값 |
|---|---|
| Agent base URL | `http://40.82.137.178:8002` |
| X1 proposal 생성 테스트 URL | `POST http://40.82.137.178:8002/model-parameters/scenarios/{scenario_id}/proposals` |
| X2 confirmation 테스트 URL | `POST http://40.82.137.178:8002/model-parameters/proposals/{modelParamProposalId}/confirmation` |

---

## 1. API 인벤토리 (계층별)

### 계층 A — Backend가 FE에 노출하는 API

| # | Method + Path | 목적 | 신규/기존/변경 | 상태전이(`tmd_mi_model_execpreph`) | 주요 req/res | 의존 | 근거 문서 | 미확정/이슈 |
|---|---|---|---|---|---|---|---|---|
| A1 | `GET /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/graph` | 그래프 + `parameterProposalTarget` | **변경(완료)** — 필드 이미 존재 | 없음 | +`parameterProposalTarget:boolean` | `MiScenarioGroup.eventTypeL2` | spec 4, 5.0.1 | — |
| A2 | `GET .../scenarios/{scenarioId}/model-execution-options` | 실행 입력 mode 조회 | **신규** | 없음 | res: `inputMode`, `parameterProposalAvailable`, `fileUploadAvailable` | `ModelInputMode.from(eventTypeL2Id)` (기존 enum) | spec 5.0; design 3.4 | — |
| A3 | `POST .../scenarios/{scenarioId}/model-param-proposals` | 제안 생성 접수 | **신규** | (신규 row) `REQUESTING` → `PROPOSAL_PENDING` / `REQUEST_FAILED` | req: **body 없음** / res: `ModelParamProposalAcceptRes`(id, status=PROCESSING, statusEndpoint) | Agent client(X1), `tmd_mi_model_execpreph` insert(선행 commit), RUNNING pre-check, `ux02(actv_scnr_id)` | spec 5.1; design 3.6 | 모호한 Agent 실패는 `REQUEST_FAILED`로 닫지 않고 `REQUESTING` 유지 |
| A4a | `GET .../scenarios/{scenarioId}/model-param-proposals` | 팝업 current-state 조회 | **신규** | 없음(read) | res: `CurrentModelParamProposalRes`(`currentState`, `parameterSource`, ids, `parameters[]`, failureReason) | 최신 `tmd_mi_model_execpreph` → Agent proposal read(R1) → 연결 Case/Run 상태 | spec 5.2.1; design 3.2/6 | 최신 preparation row가 과거 실행보다 우선. 실행별 historical confirmed snapshot은 미보장 |
| A4 | `GET .../scenarios/{scenarioId}/model-param-proposals/{modelParamProposalId}` | 상태/결과 조회 | **신규** | 없음(read) | res: `ModelParamProposalRes`(status, `parameters[]`=flat array, failureReason) | **Agent proposal 테이블 read-only**(R1) + orchestration 상태 overlay | spec 5.2; Agent doc 3 | 기존 DB/schema 직접 read. runtime table visibility/권한/type 확인 |
| A5 | `GET .../scenarios/{scenarioId}/model-param-proposals/stream` | 완료/실패 SSE 신호 | **신규** | 없음 | SSE event `model-parameter-proposal` {scenarioId, id, status} | after-commit SSE publish | spec 5.3; design 6 rule 24 | scenario-scope 신규 SSE로 확정. 기존 group SSE 채널에 합치지 않음 |
| A6 | `POST .../scenarios/{scenarioId}/model-executions` **(application/json)** | 확정값→Excel 생성→실행 접수 | **변경(기존 path 재사용) + 핸들러·확정·merge 신규** | `CONFIRMING`→`PREPARING_EXECUTION`→`EXECUTING` / `EXECUTION_FAILED` | req: `ModelParamProposalExecutionReq`(id, `confirmationParameters[]`) / res: 기존 `ModelExecutionAcceptRes`, 202 | Agent confirmation(X2), Excel merge(D-POI), 기존 Case/Run/Job dispatch, `tcm_filel`, B2 콜백 | spec 5.5; design 6 | 재확정 = Option A **설계 결정 확정**(O1 Resolved), Agent 배포/검증 gate 필요. 프로파일 배치(O5) |
| A7 | `POST .../scenarios/{scenarioId}/model-executions` **(multipart/form-data)** | 파일 3개 업로드 실행 | **기존재사용 + 변경** | — (기존 case/run) | 기존 `files` part | `inputMode=FILE_UPLOAD` 가드 추가 | spec 5.8; workflow design | `consumes` 분기, inputMode↔Content-Type 일치 검증 |
| A8 | `GET .../model-executions/latest/model-param-proposal/confirmed-parameters` | 최근 실행 확정 파라미터 | **신규** | 없음 | res: `LatestModelParamProposalParametersRes` (Agent `confirmation_parameters`) | 최신 Case→`tmd_mi_model_execpreph.case_id`→Agent read(R1) | spec 5.6; DDL draft | 과거 실행별 복원은 미보장(최신만) |
| A9 | `GET .../model-executions/latest/model-param-proposal/proposed-parameters` | 최근 실행 원 제안 파라미터 | **신규** | 없음 | 위와 동일 shape (Agent `proposal_parameters`) | 위 동일 | spec 5.6; DDL draft | — |
| A10 | `GET .../model-executions/latest/input-files` | 최근 실행 input Excel 3개 메타 | **신규** | 없음 | res: `LatestModelExecutionInputFilesRes`(fileRole/fileNo/fileNm/fileSize) | 최신 `case_id`→`tcm_filel(SCNRCASE+case_id)` | spec 5.7; DDL draft | 파일 role 식별(metadata vs filename), 3개 미만 시 불완전 상태 반환 |
| A11 | `GET .../scenarios/{scenarioId}/model-executions/{runId}` | 실행 상태 조회(폴링) | **기존재사용** | — | 기존 `ModelExecutionStatusRes` | 기존 Case/Run | workflow design; F4 | — |
| A12 | `GET /api/v1/file-management/download/file?fileNo=` | 파일 바이너리 다운로드 | **기존재사용** | — | 기존 File Management | — | spec 5.7 | A10이 fileNo 제공, 실제 download는 이 API |

### 계층 B — 외부(Agent·model/K8s)가 Backend를 호출 (inbound callback)

| # | Method + Path | 목적 | 신규/기존 | 상태전이 | 주요 req/res | 의존 | 근거 문서 | 이슈 |
|---|---|---|---|---|---|---|---|---|
| B1 | `POST .../scenarios/{scenarioId}/model-param-proposals/callback` | 제안 완료/실패 신호 수신 | **신규** | 신호 → Agent 재조회 → orchestration monotonic 갱신(`PROPOSAL_PENDING` 등), no-op 수렴 | req: `AgentCallbackEnvelopeReq`(`{status,message,data}` wrapper) / res: `200 BaseResponse<Void>` | `CustomJwtDecoder`+`@RequiredPermissionRules(Role.SYSTEM)` **재사용**, `AgentCallbackResult` normalize, Agent read(R1) | spec 5.4; design 3.5 | path `scenarioId`=**scope key**(identity 아님). in-flight row 없으면 late/no-op. `tokenByPass` matcher 추가 금지 |
| B2 | `POST /api/v1/mi/fundamental-model/runs/{runId}/callback` | model/K8s 실행 terminal 신호 수신 | **기존재사용** | (execprep 밖) 기존 Case/Run `RUNNING`→`COMPLETED/FAILED` | 기존 run callback payload | 기존 Fundamental run callback pipeline, `tmd_mdl_scnrcaseh`(runId lookup) | workflow design; F4 | B1(proposal 콜백)과 **별개**. JSON execution이 기존 Case/Run 생성·dispatch 경로를 재사용하면 동일 runId callback으로 수렴 |

### 계층 C — Backend가 Agent를 호출 (outbound client) + 공유 DB read

| # | 대상 | 목적 | 신규/기존 | 트리거 API | 주요 payload | 근거 문서 | 이슈 |
|---|---|---|---|---|---|---|---|
| X1 | Agent `POST /model-parameters/scenarios/{scenarioId}/proposals` | 제안 생성 요청 | **신규 client 메서드** | A3 | req: **body 없음** / res wrapper `data.{modelParamProposalId, status=PROCESSING}` | spec 3.2, 5.1 | callback URL은 Backend 결정→Agent 배포설정 등록. 로컬 테스트 base URL은 0.1절 참조 |
| X2 | Agent `POST /model-parameters/proposals/{modelParamProposalId}/confirmation` | 사용자 확정값 전송 | **신규 client 메서드** | A6 | req: `{data:{confirmationParameters:[...]}}`, `modelFile`을 `Demand\|Supply\|Trade`로 역변환 | spec 5.5, 6; Agent doc 3 | **Option A 설계 결정 확정(O1 Resolved)** → 같은 id 재호출로 `confirmation_parameters` 갱신. 실제 연동 전 Agent 배포/검증 gate 필요. 로컬 테스트 base URL은 0.1절 참조 |
| R1 | Agent 테이블 `tmm_mi_model_param_proposal_m` **read-only 조회** | canonical 상태·payload 서빙 | **신규 read entity/repository** | A4·A8·A9·B1 | 컬럼매핑: `status`, `proposal_parameters`/`confirmation_parameters`(JSON array), `error_message`/`error_code`, `proposal_completed_dtm`, `confirmed_dtm` | spec 5.2; Agent doc 3 | 기존 Backend DB/schema 직접 read. table visibility·SELECT 권한·key type 정합 확인 |
| X3 | Backend **토큰 발급 API** (+ 발급 인풋 2종) | Agent가 콜백 JWT 자체 발급 | **확인 필요(기존 추정)** | B1 사전 | Agent에 발급 API + 인풋 2종 전달 | spec 5.4, 13 | 기존 self-issued JWT 발급 경로 재사용 가능 여부·계약 확인(문서 미확정) |

### 계층 D — 신규 내부 서비스/컴포넌트 (HTTP API 아님, 구현 필요)

- **`ModelParamProposalService`** / **`ModelParamProposalCallbackService`** — orchestration write(JPA, monotonic compare-and-set).
- **`ModelInputWorkbookProvider`** — 협의된 원본 `DEMAND/SUPPLY/TRADE` workbook 로드 (위치/version 규칙 미정, O4).
- **`ModelParamWorkbookMergeService`** — Apache POI 기반 file/sheet/table/slot merge.
- **`tmd_mi_model_execpreph` entity/repository** — thin orchestration 행 (`ddl-auto=validate` 전 DBA DDL 선행).
- 기존 execution pipeline(Case/Run INSERT, storage upload, K8s Job dispatch)을 **reusable collaborator로 추출** — 현재 `ScenarioModelExecutionServiceImpl`의 submit/status 흐름은 구현되어 있으나 multipart 수신 바이트 중심이라 **generated bytes 경로**를 태우는 오버로드/추출이 필요.

---

## 1.1 상태전이 매트릭스 (`tmd_mi_model_execpreph.exec_prep_stat_cd`)

| Backend status | 진입 API / 이벤트 | in-flight |
|---|---|:---:|
| `REQUESTING` | A3가 RUNNING pre-check 통과 후 `tmd_mi_model_execpreph` 선행 insert (`agnt_prpsl_id=null`) | O |
| `PROPOSAL_PENDING` | X1 응답 또는 B1 callback 후 Agent read 결과가 `PROCESSING`/`PROPOSED` | O |
| `REQUEST_FAILED` | X1 요청 실패 확정, 또는 실패 callback/timeout-reconcile로 proposal 부재 확정 | X |
| `CONFIRMING` | A6(JSON) 실행 요청 검증 시작 | O |
| `PREPARING_EXECUTION` | X2 confirmation 성공, Excel 3개 merge 시작(case 미생성) | O |
| `EXECUTING` | 새 Case INSERT → `tcm_filel(SCNRCASE+case_id)` 기록 → `execprep.case_id` 저장(한 트랜잭션 commit) → commit 이후 K8s dispatch | X |
| `EXECUTION_FAILED` | confirmation 이후 Excel/파일 기록/Case·Run/dispatch 실패 | X |

`EXECUTING` 이후 terminal(실행 성공/실패)은 기존 `tmd_mdl_scnrcasem`/`tmd_mdl_scnrcaseh`가 소유하며 B2 콜백/타임아웃 스케줄러가 전이한다.

---

## 2. 전체 시퀀스 → API 매핑

```
[팝업 진입]        A1 graph(parameterProposalTarget) / A2 options(inputMode)
      │
      ├─ FILE_UPLOAD ──────────────► A7 multipart submit ──► (기존 Case/Run) ──► B2 콜백 ──► A11 status
      │
      └─ PARAMETER_PROPOSAL
          │
[제안요청]  A3 POST model-param-proposals ──► X1 Agent proposals (REQUESTING→PROPOSAL_PENDING)
          │
[Agent 계산·완료]  Agent가 proposal 테이블 commit(write-then-callback)
          │
[callback]  B1 callback 수신(신호) ──► R1 Agent 테이블 재조회 ──► orchestration 갱신 ──► A5 SSE 발행
          │
[검토·수정]  팝업 진입/A5 SSE 신호 → A4a current-state GET 또는 A4 id GET 재조회(proposedValue 표시·편집)
          │
[확정·실행접수]  A6 POST model-executions(JSON) ──► X2 Agent confirmation
          │        (CONFIRMING → PREPARING_EXECUTION: Excel 3개 merge → EXECUTING: case INSERT→tcm_filel→dispatch→case_id 저장)
          │
[모델 실행 완료]  B2 /fundamental-model/runs/{runId}/callback ──► Case/Run terminal 전이
          │
[상태/결과조회]  A4a current-state / A11 status(runId) / A8·A9 latest confirmed·proposed params / A10 latest input-files(→A12 download)
          │
[재실행]  값조정 = A6 재요청(Option A: 같은 id 재확정, 새 tmd_mi_model_execpreph row + 새 case_id)   ← O1 Resolved로 정식 채택
          파라미터부터 = A3 새 요청(새 modelParamProposalId, RUNNING pre-check)
```

---

## 3. feature-spec ↔ design 대조: 누락/불일치

| 항목 | 내용 | 판정 |
|---|---|---|
| **정합** | A2~A10 + B1 = design 9절 "API/DTO Impact" 항목과 1:1 대응 | ✅ |
| **① 상태조회(A11) 명시 격차** | design/spec 신규 API 목록엔 없지만, JSON 실행(A6)이 **기존 `ModelExecutionAcceptRes`/상태조회 재사용**을 전제 → A11이 암묵적 의존 | 명시 필요(신규 아님, 재사용) |
| **② model terminal callback(B2)·다운로드(A12) 격차** | proposal feature-spec "confirmed API contract" 표엔 없지만, 실행 후 상태 수렴에 기존 F4 콜백(B2)·File Management(A12)가 반드시 재사용됨 | 인벤토리에 명시(재사용) |
| **③ 토큰 발급 API(X3) 격차** | 인증 절엔 "Agent가 Backend 토큰 발급 API + 인풋 2종으로 자체 발급"이라 되어 있으나, **그 발급 API 자체의 계약/존재**는 어느 문서도 확정 안 함 | **확인 필요** |
| **④ SSE scope** | spec 5.3의 scenario-scope path(`/model-param-proposals/stream`)를 fixed contract로 유지한다. 기존 scenario-group SSE 채널에 event type을 추가하지 않는다. | resolved |
| **⑤ 재실행 값비교 저장소** | spec 5.5는 재요청 시 "저장된 `confirmation_parameters`와 동일 검증"을 요구하나, Backend는 confirmed snapshot 미저장 → 비교 기준이 **Agent 테이블 재조회**임을 구현자가 인지해야(오해 소지) | 명확화 필요 |
| **⑥ error table 잔재** | spec 8절 "callback data 식별자 snapshot 불일치 → 409"는 신호 격하(재조회 수렴) 방침과 부분 상충 소지 | 경미, 확인 |
| **⑦ generated bytes 내부 경로** | design/DDL이 암시하는 내부 변경은 feature-spec 표보다 넓다. 특히 generated Excel bytes를 multipart 중심 `ScenarioModelExecutionServiceImpl`에 태우는 내부 추출/오버로드가 필요 | 내부 구현 범위(계층 D) |
| **⑧ 팝업 current-state 조회 추가** | 기존 A4는 `modelParamProposalId`가 필요해 FE가 scenario 선택 직후 사용할 수 없음. A4a가 최신 execution-preparation 기준 상태와 파라미터 source를 결정 | 신규 반영 |
| **⑨ proposal 실패 후 재제안** | `REQUEST_FAILED`/Agent `FAILED` terminal 후 A3 새 요청 허용. 실패 row는 audit/history로 보존하고 새 request가 새 `tmd_mi_model_execpreph` row와 새 Agent proposal ID를 만든다 | resolved |

---

## 4. 알려진 열린 이슈 (구현 착수 전 확정 대상)

- **O1 — Agent 재확정 허용 여부 → 설계 결정 RESOLVED (2026-07-08).** Agent 팀 회신으로 이미 `CONFIRMED`인 proposal의 재확정을 허용하는 Option A가 확정됐다. Backend 설계는 값 조정 재실행을 **Option A**(같은 `model_param_proposal_id`에 X2 confirmation 재호출 → Agent `confirmation_parameters` 갱신 → 새 `tmd_mi_model_execpreph` row + 새 `case_id`)로 정식 채택하되, 실제 연동 전 Agent 배포/검증을 gate로 둔다.
  - A6 처리순서 5번: proposal이 `CONFIRMED`이고 request 값이 저장값과 다르면 **X2 재호출로 재확정**한다(이전의 "재확정 불가 시 409 거절 / 새 proposal 강제" 분기는 폐기).
  - `tmd_mi_model_execpreph`에 실행별 confirmed snapshot JSON을 추가하는 fallback ERD 변경은 "과거 실행별 확정값 정확 복원" 요구가 확정될 때만 검토(현행 계약은 최신 실행 기준만 보장).
- **O2 — Agent table read runtime pre-check** (P0): Agent proposal table은 기존 Backend DB/schema에 추가되는 직접 read 대상이다. 별도 view/synonym 경로는 사용하지 않는다. R1 read 착수는 가능하나 runtime 배포 전 table visibility, `SELECT` 권한, key type 정합을 확인한다.
- **O3 — `tmd_mi_model_execpreph` runtime validate** (resolved DDL apply): DBA DDL 적용 완료. runtime 배포 전 entity mapping validate와 `agnt_prpsl_id` non-unique index / `case_id` nullable unique / `actv_scnr_id` generated rule 확인만 수행한다.
- **O4 — 원본 workbook 위치·version 규칙** (P0): `ModelInputWorkbookProvider` 구현 blocker.
- **O5 — 프로파일/컨트롤러 배치**: 기존 multipart submit(A7)은 `dedicated`, runId 조회(A11)는 `!dedicated`, B2는 dedicated 명칭 controller이나 profile 제한이 없다. JSON A6·callback B1의 프로파일 배치 정책 결정 필요.
- **O6 — 부수**: JSON precision/최대 row·column size(P0), 신호 유실 폴백 폴링 interval, X3 토큰 발급 API 계약(3절 ③). SSE scope는 scenario-scope 신규 SSE로 resolved.

---

## 5. 요약

- **신규 개발**: FE용 9개(A2·A3·A4a·A4·A5·A6·A8·A9·A10) + inbound callback 1개(B1) + outbound Agent client 2개(X1·X2) + Agent 테이블 read repository(R1) + 내부 서비스 4종(계층 D).
- **기존재사용**: A1(완료)·A7(가드만 추가·변경)·A11·A12·**B2(model terminal callback)** 및 Case/Run/Job dispatch/`tcm_filel`/콜백 JWT 인프라.
- **리스크 상태**: 최대 리스크였던 **O1(재확정 허용 여부)는 Backend 설계 결정 Resolved** — Option A 경로 확정. SSE scope, proposal 실패 후 재제안 정책, `tmd_mi_model_execpreph` DBA DDL 적용도 resolved. 실제 연동 전 Agent 배포/검증 gate는 남아 있으며, 남은 blocker는 O2/O4(Agent table runtime visibility/권한/type 확인·workbook 위치)로 좁혀짐. O3는 runtime validate 확인 항목이다.
- **착수 가능**: Agent create/callback/read(A3·B1·A4a·A4·A8·A9)와 최신 실행 기준 조회는 mock 없이 실DB slice로 구현 가능(feature-spec 15·16절). Excel merge(D)는 별도 slice.
