# MI 수급모델 파라미터 제안 Backend Feature Spec

## 1. Document Control

- domain: gas / MI model execution
- status: Review
- owner: BE (platform)
- created at: 2026-07-03
- updated at: 2026-07-14
- canonical for: frontend-facing API contract, Backend callback endpoint/minimum compatibility contract, proposed Agent integration questions
- canonical design reference: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- price interpretation rerun reference: `docs/design-docs/gas/price-forecast-interpretation-domain-design.md`
- physical schema references:
  - execution preparation: `docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md`
  - Case/Run/file relation: `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
- Agent schema evidence: `docs/design-docs/gas/mi-agent-param-proposal.md` (Draft, reference only)
- external evidence:
  - Notion `api 정의 (frontend-backend-agent)`
  - `https://app.notion.com/p/38f263ff004c80719d14de5702d946b9`
  - contract snapshot: 2026-07-03
  - re-reviewed at 2026-07-06
  - response-only `groupedRationales` implementation request: 2026-07-13
- depends on:
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
  - `docs/conventions/source-of-truth-governance.md`
- supersedes: 없음
- last synced: 2026-07-14
- open questions: 13절 및 duplicate-slot Agent payload의 A6/Excel 실행 좌표 호환성 참조
- integration model: callback = 완료 신호. Backend가 Agent proposal 테이블을 read-only 직접 조회해 결과 서빙(CAS/hash/snapshot 없음). 공유 DB read 및 read 권한은 확정(조회 가능). callback path `scenarioId`는 proposal identity가 아니라 현재 in-flight execution-preparation을 찾는 scope key다.
- callback 인증: 확정. 기존 시나리오 설계 콜백과 동일한 Resource Server JWT 체인(`CustomJwtDecoder` HS256 self-issued + `@RequiredPermissionRules(Role.SYSTEM ...)`) 재사용. Agent는 Backend 토큰 발급 API와 발급 인풋 2종을 전달받아 JWT를 자체 발급·갱신한다.
- implementation readiness: frontend mock ready; real Agent integration은 proposal read 계약 확정(DB DDL 수령)으로 착수 가능. A4a/A4/A8/A9 response-only `groupedRationales` projection 계약은 구현 가능하며 schema 변경은 없다. SLOT-only grouping은 기존 `tmm_mi_model_param_m` read-only master 일괄 조회를 사용한다. Agent proposal table은 기존 Backend DB/schema에 추가되는 직접 read 대상이며 별도 view/synonym 경로는 필요 없다. 잔여는 runtime table visibility/권한/type 검증과 dimension_map ERD 정정으로 read 엔티티 착수를 막지 않음(인증·공유 read·콜백 계약·write-then-callback 순서·wrapper 셰이프·재시도 정책·proposal DDL 확정)

## 2. Feature Goal

Frontend가 기존 Scenario의 기반 Event에 따라 두 가지 수급모델 실행 흐름을 사용할 수 있게 한다.

### A. 파라미터 제안 대상 Event 기반 Scenario

1. 파라미터 제안 생성 접수
2. 팝업 진입 또는 SSE 완료 신호 수신 후 Scenario 기준 current-state 조회
3. `values[].proposedValue`만 표에 표시하고 필요 시 수정
4. 사용자 확정값으로 Agent confirmation
5. Backend 원본 Excel 3개 생성·병합
6. 기존 Scenario Model Execution 비동기 실행 접수

이 흐름에서 사용자는 Excel 파일을 업로드하지 않는다.

### B. 파라미터 제안 비대상 Event 기반 Scenario

1. 사용자가 기존 방식대로 `DEMAND`/`SUPPLY`/`TRADE` Excel 파일 3개를 업로드한다.
2. 기존 Scenario Model Execution 비동기 실행을 접수한다.

기존 multipart 파일 업로드 실행 계약은 계속 지원하며 제거·deprecated하지 않는다.

두 실행 흐름 모두 최종 실행 등록 transaction에서 해당 시나리오의 active 가격전망해석 row를 조회한다. row가 있으면 soft delete한 뒤 같은 transaction에서 Case/Run을 생성하며, 없으면 기존 실행 등록을 그대로 진행한다. 이를 위한 별도 request/response field나 사전 확인 API는 추가하지 않는다.

## 3. Canonical References

- lifecycle/business rules: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- 기존 model execution contract: `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md` F4
- 기존 execution/file persistence: `docs/design-docs/gas/mi-scenario-model-execution-workflow-design.md`
- Agent contract: 외부 evidence를 본 문서로 normalize하며 Notion 자체는 canonical source가 아니다.
- Agent DB 설계는 table ownership/field mapping의 evidence다. Agent DDL을 Backend schema contract로 사용하지 않는다.

### Source-of-truth Responsibilities

- 가격전망해석 재실행 무효화 범위/lifecycle: `docs/design-docs/gas/price-forecast-interpretation-domain-design.md`
- A6/A7의 가격전망해석 자동 soft-delete 실행 계약: 본 feature spec 5.5/5.8
- A8/A9 조회 대상, 공통 404, request/response 계약: 본 feature spec 5.5.1/5.6
- A4a 성공 실행 input file projection과 다운로드 계약: 본 feature spec 5.2.1/5.7
- physical table/column, join, index, EXPLAIN, rollout: `docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md`
- 일반 Scenario Case latest/lifecycle 규칙: `docs/design-docs/gas/mi-scenario-model-execution-workflow-design.md`
- 구현 파일, 검증 명령/결과, 잔여 위험: task별 `docs/work-notes/YYYY/*`
- 비canonical 문서는 위 책임 문서의 계약을 반복 정의하지 않고 필요한 canonical section을 참조한다.

### 3.1 API ↔ Agent DB mapping

| API field | Agent DB evidence | Rule |
|---|---|---|
| `modelParamProposalId` | proposal table PK 후보 | Agent identity. Backend local PK와 동일시하지 않음 |
| `scenarioId` | `scenario_id` | path와 callback/body가 저장된 request snapshot과 일치해야 함 |
| `scenarioGroupId` | `scenario_group_id` | `scenarioId`에서 도출한 요청 시점 snapshot |
| `eventId` | `mi_event_id` | API 명칭은 `eventId`, DB mapping은 `mi_event_id`로 고정 |
| `modelTarget.modelFile` | `model_file` | A4/A6 입력 검증은 `DEMAND|SUPPLY|TRADE`를 case-insensitive하게 해석한다. A4a tree leaf의 `parameter`는 Agent 저장 payload 값을 보존하고 rendering master와 case-sensitive exact match한다. |
| `modelTarget.modelSheet` | `model_sheet` | 문자열 exact match |
| `modelTarget.tableName` | `model_table` | phase 1 required contract. 실제 테이블명/표시명을 그대로 사용하며 Agent payload 반영 배포가 선행되어야 함 |
| `proposalParameters` | `proposal_parameters` JSON array | Agent 원본 flat parameter array |
| `confirmationParameters` | `confirmation_parameters` JSON array | 동일 shape의 사용자 확정 parameter array |

Agent `status`는 proposal 계산 상태이고 `callback_status`는 Agent→Backend 전달 상태다. Backend orchestration 상태와 혼합하지 않는다.

### 3.2 2026-07-03 Agent Notion 계약 동기화 결과

#### Confirmed Facts

- proposal 생성은 body 없는 `POST /model-parameters/scenarios/{scenarioId}/proposals`이며 즉시 wrapper 응답의 `data`에 `modelParamProposalId`와 `PROCESSING`을 반환한다.
- Backend가 callback endpoint와 URL을 결정해 Agent 팀에 전달하고, Agent는 전달받은 URL을 배포 운영 설정값으로 등록·관리한다.
- callback은 `{status,message,data}` wrapper와 Bearer token을 사용하며 `data.status` terminal 값은 `PROPOSED | FAILED`다.
- callback 식별자는 `modelParamProposalId`, `scenarioId`, `scenarioGroupId`, `eventId`다.
- confirmation은 `POST /model-parameters/proposals/{modelParamProposalId}/confirmation`이며 request body는 `{data:{confirmationParameters:[...]}}`다.
- `proposalParameters`와 `confirmationParameters`는 동일한 flat parameter array shape다. parameter/model target/slot identity와 `basecaseValue`는 유지하고 `values[].proposedValue`는 달라질 수 있다. Agent가 confirmation 저장 과정에서 parameter/value `rationale`을 보정할 수 있으므로 Backend의 구조·동일값 재확인 비교에서는 rationale 문자열 일치를 요구하지 않는다.
- Agent `modelFile` casing은 `Demand | Supply | Trade`다.

#### Interpretations

- Notion의 “callback URL은 운영 설정으로 관리”는 URL 결정권이 Agent에 있다는 뜻이 아니다. Backend가 정한 callback URL을 Agent 설정에 등록한다는 의미로 사용자 확인했다.
- Bearer token 검증 방식은 확정되었다. 기존 시나리오 설계 콜백과 동일한 플랫폼 self-issued JWT(`CustomJwtDecoder`, HS256)를 재사용하며, Agent는 Backend 토큰 발급 API와 발급 인풋 2종을 전달받아 자체 발급·갱신한다. (OAuth2 외부 IdP, static token, HMAC은 채택하지 않는다)
- Agent가 ack body를 해석하거나 특정 retry schedule을 따른다는 근거는 없다.
- `callbackCorrelationId`, `callbackEventId`, `schemaVersion`, `completedAt`, `errorCode`, `errorMessage`는 Backend가 제안한 확장이지 2026-07-03 Agent 계약의 confirmed field가 아니다.

#### Classification

| 차이 | 분류 | 문서 처리 |
|---|---|---|
| proposal request의 `callbackUrl` | 현재 Backend 설계를 Notion 계약에 맞게 되돌릴 사항 | body에서 제거; Backend가 정한 URL을 Agent 운영 설정에 등록 |
| flat callback DTO | 현재 Backend 설계를 Notion 계약에 맞게 되돌릴 사항 | wrapper 수신 후 내부 normalize |
| callback 인증 구현 방식 | 확정 | 기존 콜백과 동일 self-issued JWT 재사용; Agent가 Backend 토큰 발급 API + 인풋 2종으로 자체 발급 |
| `callbackCorrelationId` | Agent 계약을 확장해야 할 사항 | race 방지 확장안; 대안은 response-before-callback 순서 보장 |
| `callbackEventId` | Agent 계약을 확장해야 할 사항 | delivery 멱등키 확장안; 대안은 Backend payload hash |
| `schemaVersion`, `completedAt`, `errorCode` | Agent 계약을 확장해야 할 사항 | 필요성 합의 전 proposed; `errorMessage`는 wrapper `message`로 대체 가능 |
| ack disposition | Agent 팀 협의가 필요한 계약 변경 | 최소안은 `200 BaseResponse<Void>` |
| Agent retry schedule | Agent 팀 협의가 필요한 계약 변경 | fixed contract에서 제거 |
| `modelFile` casing | Backend 내부에서 결정 가능한 사항 | inbound 대문자 normalize, confirmation outbound title-case 변환 |
| confirmation wrapper | 현재 Backend 설계를 Notion 계약에 맞게 되돌릴 사항 | Agent client request에 `data` wrapper 명시 |
| callback을 신호로 격하 + Agent 테이블 재조회 / orchestration monotonic / after-commit SSE | Backend 내부에서 결정 가능한 사항 | CAS·hash·snapshot 없이 유지 (Domain Design 3.5) |

## 4. Confirmed API Contract

### Frontend-facing

| Method | Path | 목적 |
|---|---|---|
| `GET` | `/api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/graph` | 그래프 데이터와 파라미터 제안 대상 여부 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-execution-options` | 기반 Event에 따른 실행 입력 mode 조회 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals` | Agent proposal 생성 접수 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals` | 팝업 진입용 Scenario 기준 현재 파라미터 제안/확정/실행 상태 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/{modelParamProposalId}` | 상태 및 proposal 결과 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/stream` | proposal 완료/실패 SSE 신호 구독 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/from-parameters` (`application/json`) | 제안 대상: 파라미터 확정 후 Excel 생성·실행 접수 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions` (`multipart/form-data`) | 비대상: 사용자 Excel 3개 업로드 후 기존 실행 접수 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters` | 가장 최근 수급모델 실행과 연결된 proposal의 현재 확정 파라미터 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters` | 가장 최근 수급모델 실행의 원 Agent 제안 파라미터 조회 |

### Agent-facing callback

| Method | Path | 목적 |
|---|---|---|
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/callback` | Agent proposal 완료/실패 결과 수신 |

두 실행 접수 계약은 모두 active contract이며 path를 분리한다. Backend는 Scenario의 기반 Event에서 도출한 `inputMode`와 endpoint의 일치를 검증한다. JSON 파라미터 확정 실행은 `/model-executions/from-parameters`, multipart 파일 업로드 실행은 `/model-executions`만 사용한다. 두 request의 기존 shape는 유지하며, active 가격전망해석 soft delete는 공통 최종 실행 등록 transaction의 내부 처리다. 기존 JSON path 사용자가 없음을 확인했으므로 구 JSON mapping은 deprecated alias 없이 즉시 제거한다.

Agent-facing callback의 endpoint/path와 외부 URL은 Backend가 결정한다. Backend는 확정 URL을 Agent 팀에 전달하고, Agent는 `callbackBaseUrl + callbackPath` 등의 배포 설정값으로 등록한다. proposal request body로 callback 정보를 전달하는 방식은 현재 confirmed contract가 아니다.

callback path는 `{scenarioId}`를 경로에 둔다. `scenarioId`는 proposal identity가 아니라 **현재 in-flight execution-preparation을 찾는 scope key**다. callback `data.modelParamProposalId`가 있으면 Backend는 이를 in-flight 후보 검색 조건으로 쓰고 path `scenarioId`와 교차검증한다. `modelParamProposalId`는 반복 실행 때문에 `tmd_mi_model_execpreph`에서 unique가 아니므로 terminal/history row를 선택하는 단독 key로 쓰지 않는다. `modelParamProposalId`가 없거나 실패 callback의 `data`가 `null`이면 Backend는 path `scenarioId`의 현재 in-flight preparation이 정확히 1건일 때만 처리한다. in-flight preparation이 없으면 late/no-op으로 수렴하며 terminal row나 새 unrelated row에 붙이지 않는다.

## 5. Endpoint

### 5.0 Model execution options 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-execution-options`

팝업 진입 시 FE가 호출해 실행 입력 UI를 결정한다.

```json
{
  "scenarioId": 309,
  "inputMode": "PARAMETER_PROPOSAL",
  "parameterProposalAvailable": true,
  "fileUploadAvailable": false
}
```

`inputMode`:

- `PARAMETER_PROPOSAL`: 제안 생성/수정 UI 표시, JSON execution 사용
- `FILE_UPLOAD`: 기존 파일 3개 업로드 UI 표시, multipart execution 사용

- mode는 Backend가 `Scenario -> ScenarioGroup -> eventTypeL2Id` 관계로 도출한다.
- FE request로 mode를 변경할 수 없다.
- `eventTypeL2Id`가 `3`, `12`, `13`, `14`, `15`, `16`, `20` 중 하나면 `FILE_UPLOAD`를 반환한다.
- 그 외 `eventTypeL2Id`는 `PARAMETER_PROPOSAL`을 반환한다.
- `PARAMETER_PROPOSAL`이면 `parameterProposalAvailable=true`, `fileUploadAvailable=false`다.
- `FILE_UPLOAD`이면 `parameterProposalAvailable=false`, `fileUploadAvailable=true`다.
- dimension mapping 존재 여부는 `inputMode` 응답을 변경하지 않는다. 상세 business rule은 canonical design의 3.4절과 6절을 따른다.
- 가격전망해석 존재 여부는 이 options 응답에 포함하지 않는다. 수급모델 실행 final transaction이 active row를 직접 조회하고 자동 soft delete한다.
- 가격전망해석의 완료 run당 1회 생성 제한과 Agent callback 정합성은 가격전망해석 도메인이 소유한다. 이 options/실행 API는 해당 규칙을 검증하거나 가격전망해석 생성을 호출하지 않는다.

### 5.0.1 Scenario Group 그래프 대상 여부 조회

`GET /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/graph`

기존 그래프 응답의 최상위 필드에 필수 boolean `parameterProposalTarget`을 포함한다.

```json
{
  "scenarioGroupId": 1,
  "parameterProposalTarget": false,
  "indexName": "JKM",
  "periodUnit": "monthly"
}
```

- `parameterProposalTarget=true`: `inputMode=PARAMETER_PROPOSAL`인 파라미터 제안 대상
- `parameterProposalTarget=false`: `inputMode=FILE_UPLOAD`인 파라미터 제안 미대상
- 판정은 Model execution options와 동일하게 Scenario Group의 `eventTypeL2Id`를 사용한다.
- `eventTypeL2Id IN (3, 12, 13, 14, 15, 16, 20)`이면 `false`, 그 외에는 `true`다.
- FE request나 dimension mapping 존재 여부로 값을 변경할 수 없다.

### 5.1 Proposal 생성 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals`

- request body: 없음
- Backend는 Scenario/권한/중복 상태와 `inputMode=PARAMETER_PROPOSAL`을 검증한다.
- Backend는 Agent 호출 전에 `scenarioId` 기준 execution-preparation을 `REQUESTING` 상태와 `modelParamProposalId=null`로 생성·commit한다. 외부 Agent 호출 동안 이 DB transaction을 열어두지 않는다.
- Backend Agent client는 body 없이 Agent `POST /model-parameters/scenarios/{scenarioId}/proposals`를 호출한다.
- Backend가 callback URL을 결정해 Agent 팀에 전달하고, Agent는 이를 배포 운영 설정값으로 등록한다.
- Agent HTTP 200의 `{status,message,data}` wrapper에서 `data.modelParamProposalId`와 `data.status=PROCESSING`을 읽어 Backend `BaseResponse`로 변환한다.
- Agent 응답 경로는 응답의 `modelParamProposalId`를, callback 경로는 Agent 테이블 재조회로 확인한 ID를 execution-preparation row의 external correlation key로 조건부 저장한다. 두 경로 중 먼저 도착한 값이 write-once로 설정되며, 같은 ID는 no-op, 다른 ID는 기존 값을 덮어쓰지 않고 correlation conflict로 처리한다.
- Agent 응답 처리로 이미 저장된 terminal 상태를 `PROCESSING`으로 되돌리지 않는다. 응답 직전 현재 persisted status를 다시 읽어 FE에 반환한다.
- Agent 요청 timeout 시 late callback 가능성이 있으므로 execution-preparation을 즉시 삭제하지 않고 callback/Agent 테이블 재조회로 수렴시킨다.
- Agent 요청 결과가 “Agent 접수 실패”인지 “Agent는 접수했지만 응답만 유실”인지 불명확하면 `REQUEST_FAILED`로 닫지 않는다. 해당 row를 `REQUESTING` in-flight 상태로 유지해 `ux02(actv_scnr_id)`가 새 preparation 생성을 막게 하고, callback/read-reconcile/timeout 복구가 최종 outcome을 결정한다.
- Agent proposal 생성이 명시적으로 실패해 최신 execution-preparation이 `REQUEST_FAILED`이거나 Agent status가 `FAILED`로 수렴한 경우, FE는 다시 `POST /model-param-proposals`를 호출할 수 있다. Backend는 실패 row를 재사용하지 않고 새 `tmd_mi_model_execpreph` row를 `REQUESTING`으로 생성한 뒤 새 Agent proposal 요청을 보낸다. 실패 row는 audit/history로 보존하며 in-flight guard 대상이 아니다.
- Backend→FE HTTP status: `200 OK`

Response data:

```json
{
  "modelParamProposalId": 10001,
  "scenarioId": 309,
  "status": "PROCESSING",
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "statusEndpoint": "/api/v1/mi/next-gen/scenarios/309/model-param-proposals/10001"
}
```

- 일반 응답 status는 `PROCESSING`이다.
- callback은 Agent가 proposal 테이블에 결과를 commit한 뒤 발행하는 완료 신호이고 Backend는 신호 수신 후 테이블을 재조회하므로, HTTP 응답과 callback 도착 순서 race는 결과 정합성에 영향을 주지 않는다(재조회로 수렴). 기존 시나리오 설계 콜백과 동일한 write-then-callback 전제이며 `callbackCorrelationId` 확장은 불필요하다.

### 5.2 Proposal 상태/결과 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/{modelParamProposalId}`

```json
{
  "modelParamProposalId": 10001,
  "scenarioId": 309,
  "scenarioGroupId": 61,
  "eventId": 1378,
  "status": "PROPOSED",
  "modelFiles": [
    {
      "modelFile": "Demand",
      "modelSheets": [
        {
          "modelSheet": "WeatherAdjustment",
          "modelTables": [
            {
              "tableName": "table1",
              "parameters": [
                {
                  "parameter": {
                    "parameterName": "Temperature",
                    "modelTarget": {
                      "modelFile": "Demand",
                      "modelSheet": "WeatherAdjustment",
                      "tableName": "table1",
                      "valueFieldNames": ["Temperature"],
                      "slotAxesFieldNames": ["Country", "Year", "Month"]
                    },
                    "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
                    "values": [
                      {
                        "Country": "Germany",
                        "Year": 2027,
                        "Month": 1,
                        "basecaseValue": 1.2,
                        "proposedValue": -3.8,
                        "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
                      }
                    ]
                  },
                  "rendering": {
                    "readMode": "FIXED_COLUMNS",
                    "columns": [
                      {"column": "K", "role": "Year", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "L", "role": "Month", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "M", "role": "Country", "kind": "SLOT", "align": "LEFT"},
                      {"column": "O", "role": "Temperature (°C)", "kind": "VALUE", "align": "RIGHT"}
                    ],
                    "semanticAxes": "Country, Year, Month 기준 기온 조정값"
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "failureReason": null,
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "completedAt": "2026-07-03T15:00:20+09:00"
}
```

- `PROCESSING`: `parameters=[]`, `completedAt=null`
- `PROPOSED`: Agent row의 `proposal_parameters` flat array 반환
- `FAILED`: `parameters=[]`, `failureReason` 반환
- `CONFIRMED/EXECUTING`: Agent row의 `confirmation_parameters` flat array 반환

Flat parameter array validation/rendering:

- FE는 array의 최초 등장 순서를 유지하면서 `modelFile -> modelSheet -> tableName`으로 그룹핑해 tab과 서브메뉴 2단을 구성한다.
- `tableName`은 phase 1부터 required이며 실제 테이블명/표시명을 그대로 사용한다.
- `valueFieldNames`는 phase 1에서 정확히 1개여야 한다.
- 표의 축 header는 `slotAxesFieldNames` 순서를 그대로 사용한다.
- 값 header는 value field의 camel-case를 공백으로 분리한 label을 사용해 `% {label}` 하나만 생성한다.
- 예: `slotAxesFieldNames=[Country,Year,Month]`, `valueFieldNames=[Temperature]`이면 `Country | Year | Month | % Temperature`다.
- 각 `values[]` item은 `slotAxesFieldNames`에 선언된 key를 빠짐없이 포함하고 추가 slot-axis key를 포함하지 않는다.
- A4a/A4/A8/A9 read response에서는 같은 parameter 안의 duplicate slot-axis 조합을 허용하고 원본 `values[]` row를 모두 보존한다. 이 read 규칙은 아래 5.2.2의 `groupedRationales` projection에만 적용된다.
- A6 execution request는 기존 계약대로 slot-axis 조합의 유일성을 요구하며 `ModelParamConfirmationValidator`가 duplicate slot identity를 거절한다. duplicate Agent payload의 실행 좌표 의미가 확정되기 전까지 이 execution 규칙을 완화하지 않는다.
- `basecaseValue`는 Agent 원본 비교·검증용으로 payload에 유지하지만 화면에는 표시하지 않는다.
- FE value cell은 `proposedValue`를 표시하며 사용자 수정도 동일 필드에 반영한다.
- 사용자가 값을 수정하지 않아도 조회한 원본 `proposedValue` 그대로 실행 요청할 수 있다.
- phase 1 표는 long-format이다. matrix wide-format pivot은 본 계약 범위에 포함하지 않는다.

**데이터 소스:** `status`/`parameters`/`failureReason`는 Backend가 **Agent proposal 테이블(`tmm_mi_model_param_proposal_m`)을 read-only로 직접 조회**해 서빙한다(callback payload를 저장·재생하지 않음). 컬럼 매핑(2026-07-07 DB DDL 확정): `status`←`status`(`PROCESSING|PROPOSED|CONFIRMED|FAILED`), `parameters`←`proposal_parameters`(또는 `CONFIRMED/EXECUTING`에서 `confirmation_parameters`), `failureReason`←`error_message`(+구조화 `error_code` 사용 가능), `completedAt`←`proposal_completed_dtm`, confirmation 시각←`confirmed_dtm`. `callback_status`/`callback_sent_dtm`은 Agent 전송 상태로 무시한다. `CONFIRMING/EXECUTING` 등 Backend 전용 상태는 Backend orchestration 행에서 덮어 얹는다. 상세는 Domain Design 3.1/3.5 참조.

A4의 `parameters[]` 각 원소에는 5.2.2의 response-only `groupedRationales[]`를 추가한다. Agent 저장 JSON과 기존 `parameters[]`/`values[]` 내용·순서는 변경하지 않는다.

### 5.2.1 Scenario 기준 현재 파라미터 상태 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals`

파라미터 제안 팝업 진입 시 FE가 `scenarioId`만으로 현재 표시해야 할 상태와 파라미터 데이터를 결정하기 위한 read-only 조회다. 이 API는 생성 요청을 만들거나 Agent를 호출하지 않는다.

조회 우선순위:

1. Scenario의 최신 non-deleted `tmd_mi_model_execpreph` row를 찾는다. 정렬 기준은 `exec_prep_id DESC` 또는 동등한 생성시각 최신순이다.
2. 최신 execution-preparation row가 없으면 `NOT_REQUESTED`, `parameterSource=NONE`, `modelFiles=[]`를 반환한다.
3. 최신 row가 `REQUESTING`이거나 Agent row가 아직 없거나 Agent status가 `PROCESSING`이면 `GENERATING`을 반환한다.
4. 최신 row가 `PROPOSAL_PENDING`이고 Agent status가 `PROPOSED`이면 `PROPOSAL_READY`로 정규화하고 Agent `proposal_parameters`로 `modelFiles` tree를 만든다.
5. 최신 row가 `CONFIRMING` 또는 `PREPARING_EXECUTION`이거나 Agent status가 `CONFIRMED`인데 아직 `case_id`가 없으면 `CONFIRMED_PENDING_EXECUTION`으로 정규화하고 Agent `confirmation_parameters`로 tree를 만든다.
6. 최신 row에 `case_id`가 있고 연결된 Scenario Case/Run이 `RUNNING`이면 `EXECUTING`과 Agent `confirmation_parameters` tree를 반환한다.
7. 최신 row에 `case_id`가 있고 연결된 Scenario Case/Run이 `COMPLETED`이면 `EXECUTED_CONFIRMED`, Agent `confirmation_parameters` tree, 해당 `case_id`의 `executionInputFiles`를 반환한다. 이것이 FE가 "수급모델 실행까지 성공한 상태"에서 보여줄 확정버전 파라미터와 다운로드 가능한 실행 input이다.
8. 최신 row 또는 Agent row가 실패 상태면 `PROPOSAL_FAILED` 또는 `EXECUTION_FAILED`를 반환하고 `failureReason`을 채운다. `EXECUTION_FAILED`에서 실제 confirmation payload가 있으면 `CONFIRMATION_PARAMETERS` tree를 반환한다. confirmation payload가 없으면 `parameterSource=NONE`, `modelFiles=[]`로 정규화한다. 연결 Run이 `FAILED`이면 dispatch 이후 모델 실행 실패로 보고 `EXECUTION_FAILED`와 `modelExecutionStatus=FAILED`를 반환한다. 연결 Run 실패의 `failureReason`은 raw `errorMessage`보다 stable `failureReasonCode`를 우선하며, 내부 exception/root-cause 문자열은 API로 반환하지 않는다.

상태 매핑:

| 기준 | 조건 | `currentState` | `parameterSource` | 비고 |
|---|---|---|---|---|
| execution-preparation | 최신 non-deleted row 없음 | `NOT_REQUESTED` | `NONE` | `modelFiles=[]` |
| Backend status / Agent row | `REQUESTING` 또는 Agent row 없음 또는 Agent `PROCESSING` | `GENERATING` | `NONE` | `modelFiles=[]` |
| Agent status | Agent `PROPOSED` | `PROPOSAL_READY` | `PROPOSAL_PARAMETERS` | Agent `proposal_parameters` tree |
| Backend status / Agent status | Backend `CONFIRMING`/`PREPARING_EXECUTION` 또는 Agent `CONFIRMED`, `case_id` 없음 | `CONFIRMED_PENDING_EXECUTION` | `CONFIRMATION_PARAMETERS` | Agent `confirmation_parameters` tree |
| Case/Run status | latest run `RUNNING` | `EXECUTING` | `CONFIRMATION_PARAMETERS` | confirmation tree, `modelExecutionStatus=RUNNING` |
| Case/Run status | latest run `COMPLETED` | `EXECUTED_CONFIRMED` | `CONFIRMATION_PARAMETERS` | confirmation tree, `modelExecutionStatus=COMPLETED` |
| Case/Run status | latest run `FAILED` | `EXECUTION_FAILED` | confirmation payload 존재 시 `CONFIRMATION_PARAMETERS`, 없으면 `NONE` | `modelExecutionStatus=FAILED` |
| Backend status | `REQUEST_FAILED` | `PROPOSAL_FAILED` | `NONE` | Agent proposal 생성/수렴 실패, `modelFiles=[]` |
| Agent status | Agent `FAILED` | `PROPOSAL_FAILED` | `NONE` | Agent proposal 실패, `modelFiles=[]` |
| Backend status | `EXECUTION_FAILED` + `case_id` 없음 | `EXECUTION_FAILED` | confirmation payload 존재 시 `CONFIRMATION_PARAMETERS`, 없으면 `NONE` | confirmation/Excel/file/Case·Run commit 이전 단계 실패. `modelExecutionStatus=null` |

`modelExecutionStatus`는 기존 A11 model execution status vocabulary(`RUNNING|COMPLETED|FAILED`)를 재사용한다. `case_id`가 연결됐지만 Case/Run 정합이 깨진 경우에는 8절의 데이터 불일치 오류로 처리한다. `EXECUTING` 전이는 Case INSERT와 `execprep.case_id` 저장을 한 트랜잭션에서 commit한 뒤 dispatch하므로(Domain Design 규칙 29), "Case는 존재하는데 `case_id=null`"인 상태는 영속화되지 않는다. 따라서 A4a는 이 window를 판정 대상으로 두지 않는다.

`executionInputFiles`는 다운로드 노출 정책이다. `currentState=EXECUTED_CONFIRMED`일 때만 최신 execution-preparation row의 `caseId`에 연결된 파일 projection을 반환하고, 그 외 모든 상태에서는 `null`을 반환한다. 파일 존재 여부만으로 노출 상태를 확대하지 않으며, 향후 다운로드 가능 상태가 추가되면 이 상태 allow-list를 확장한다. 세부 파일 계약은 5.7을 따른다.

동시성/신선도 규칙:

- 최신 proposal/execution-preparation row가 과거 실행보다 우선한다. 즉 사용자가 새 파라미터 제안을 시작하면 이전 실행의 확정 파라미터보다 새 row의 `GENERATING`/`PROPOSAL_READY`/`CONFIRMED_PENDING_EXECUTION` 상태가 먼저 반환된다.
- SSE는 완료 신호일 뿐이다. FE는 SSE 수신 직후 이 API를 재조회해 화면을 갱신할 수 있다.
- 동일 Agent proposal로 값 조정 재실행(Option A)을 수행하면 Agent `confirmation_parameters`가 현재 확정값으로 갱신된다. 이 API는 Agent의 현재 `confirmation_parameters`를 반환하며 실행별 과거 확정값의 exact 복원은 현행 계약에 포함하지 않는다.
- `FILE_UPLOAD` mode Scenario에서 호출하면 `409 BIZ_RULE_VIOLATION`으로 거절한다. FE는 5.0 options API로 입력 mode를 먼저 확인할 수 있다.

Response tree contract:

```text
modelFiles[]
└── modelSheets[]
    └── modelTables[]
        └── parameters[]
            ├── parameter
            └── rendering
```

| JSON path | Type | Required/null/empty rule |
|---|---|---|
| `modelFiles` | array | required, never `null`; `parameterSource=NONE`이면 `[]` |
| `modelFiles[].modelFile` | string | required, non-blank |
| `modelFiles[].modelSheets` | array | required, non-empty |
| `modelSheets[].modelSheet` | string | required, non-blank |
| `modelSheets[].modelTables` | array | required, non-empty |
| `modelTables[].tableName` | string | required, non-blank |
| `modelTables[].parameters` | array | required, non-empty |
| `parameters[].parameter` | object | required, Agent flat parameter field에 response-only `groupedRationales[]`를 추가한 shape |
| `parameters[].rendering` | object | required, rendering master projection |

`parameter` fields:

- `parameterName`
- `modelTarget.modelFile`
- `modelTarget.modelSheet`
- `modelTarget.tableName`
- `modelTarget.valueFieldNames[]`
- `modelTarget.slotAxesFieldNames[]`
- parameter-level `rationale`
- response-only `groupedRationales[]`
- `values[]`

`values[]`는 dynamic slot-axis key와 `basecaseValue`, `proposedValue`, `rationale`를 포함하는 Agent payload 의미를 그대로 보존한다. `groupedRationales`는 기존 데이터를 대체하지 않는 별도 read projection이며 세부 계약은 5.2.2를 따른다. Public response DTO는 MI response package가 소유하며 Agent client request DTO Java class를 직접 공유하지 않는다.

`rendering` fields:

- `readMode`
- `columns[]`
- `semanticAxes`

`rendering`에는 `parameterName`, `modelFile`, `modelSheet`, `modelTable`, `tableName`을 반복하지 않는다. 좌표와 identity는 tree path와 `parameter.modelTarget`을 사용한다.

Assembly and integrity rules:

- 선택된 `parameterSource`의 Agent parameter 배열을 입력으로 사용한다.
- file/sheet/table/parameter는 각각 입력 배열에서 최초 등장한 순서를 유지한다.
- 빈 file/sheet/table node는 반환하지 않는다.
- 모든 입력 parameter는 tree에 정확히 한 번 포함되어야 한다.
- parameter composite identity는 `parameterName`, `modelFile`, `modelSheet`, `tableName`, `valueFieldNames`, `slotAxesFieldNames` 전체이며 배열 순서도 identity에 포함한다. 동일 identity가 중복되면 `409 BIZ_RULE_VIOLATION`이다.
- rendering master는 parameter name 집합으로 한 번 일괄 조회하며 N+1 조회를 만들지 않는다.
- parameter마다 rendering master가 정확히 하나 있어야 한다. 누락 또는 중복이면 `409 BIZ_RULE_VIOLATION`이다.
- tree `modelFile`/`modelSheet`/`tableName`, `parameter.modelTarget` 좌표, rendering master `modelFile`/`modelSheet`/`modelTable`은 case-sensitive exact match여야 한다.
- A4a에서는 Agent 저장 payload 좌표를 case-normalize하지 않는다. `parameter`와 tree path에는 payload 값을 보존한다.
- `parameterSource=PROPOSAL_PARAMETERS` 또는 `CONFIRMATION_PARAMETERS`인데 payload가 null, blank, non-array, empty이거나 파싱할 수 없으면 정상 empty tree로 숨기지 않고 `409 BIZ_RULE_VIOLATION`으로 처리한다.
- 정합성 오류 응답 메시지에는 raw payload, 내부 exception, master 상세값을 노출하지 않는다.

Top-level fields:

- 유지: `scenarioId`, `currentState`, `parameterSource`, `execPrepId`, `modelParamProposalId`, `scenarioGroupId`, `eventId`, `caseId`, `runId`, `modelExecutionStatus`, `modelFiles`, `failureReason`, `requestedAt`, `completedAt`, `confirmedAt`
- 추가: `executionInputFiles` — nullable; `EXECUTED_CONFIRMED`에서만 non-null
- 제거: top-level `parameters`, top-level `parameterShapes`
- timestamp Java type/serialization과 `failureReason` string 계약은 변경하지 않는다.

Compatibility and A6 mapping:

- FE는 기존 top-level `parameters`, navigation-only `modelFiles`, `parameterShapes`를 production contract로 소비하지 않는다는 합의에 따라 동일 endpoint에서 새 tree로 in-place 교체한다. 실제 소비자가 발견되면 구현을 중단하고 version/additive migration 전략을 다시 합의한다.
- A6 `POST .../model-executions/from-parameters` request는 `{ "modelParamProposalId": ..., "confirmationParameters": [...] }`를 유지한다.
- FE는 모든 `data.modelFiles[].modelSheets[].modelTables[].parameters[].parameter`를 최초 tree 순서대로 flatten하되 response-only `groupedRationales`를 제거한 뒤 `confirmationParameters[]`로 보낸다.
- tree wrapper와 `rendering`은 A6로 전송하지 않는다.
- `groupedRationales`는 A6 request, Agent X2 confirmation, B1 callback, Agent 저장 JSON에 포함하지 않는다.
- A4a tree response slice implementation readiness: `ready`. 이 문서의 다른 endpoint 관련 P0 open question은 A4a response projection 구현을 막지 않는다.

Response data examples:

생성중:

```json
{
  "scenarioId": 309,
  "currentState": "GENERATING",
  "parameterSource": "NONE",
  "execPrepId": 51001,
  "modelParamProposalId": 10001,
  "scenarioGroupId": 61,
  "eventId": 1378,
  "caseId": null,
  "runId": null,
  "modelExecutionStatus": null,
  "executionInputFiles": null,
  "modelFiles": [],
  "failureReason": null,
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "completedAt": null,
  "confirmedAt": null
}
```

생성완료 + 제안 파라미터:

```json
{
  "scenarioId": 309,
  "currentState": "PROPOSAL_READY",
  "parameterSource": "PROPOSAL_PARAMETERS",
  "execPrepId": 51001,
  "modelParamProposalId": 10001,
  "scenarioGroupId": 61,
  "eventId": 1378,
  "caseId": null,
  "runId": null,
  "modelExecutionStatus": null,
  "executionInputFiles": null,
  "modelFiles": [
    {
      "modelFile": "Demand",
      "modelSheets": [
        {
          "modelSheet": "WeatherAdjustment",
          "modelTables": [
            {
              "tableName": "table1",
              "parameters": [
                {
                  "parameter": {
                    "parameterName": "Temperature",
                    "modelTarget": {
                      "modelFile": "Demand",
                      "modelSheet": "WeatherAdjustment",
                      "tableName": "table1",
                      "valueFieldNames": ["Temperature"],
                      "slotAxesFieldNames": ["Country", "Year", "Month"]
                    },
                    "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
                    "values": [
                      {
                        "Country": "Germany",
                        "Year": 2027,
                        "Month": 1,
                        "basecaseValue": 1.2,
                        "proposedValue": -3.8,
                        "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
                      }
                    ]
                  },
                  "rendering": {
                    "readMode": "FIXED_COLUMNS",
                    "columns": [
                      {"column": "K", "role": "Year", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "L", "role": "Month", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "M", "role": "Country", "kind": "SLOT", "align": "LEFT"},
                      {"column": "O", "role": "Temperature (°C)", "kind": "VALUE", "align": "RIGHT"}
                    ],
                    "semanticAxes": "Country, Year, Month 기준 기온 조정값"
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "failureReason": null,
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "completedAt": "2026-07-03T15:00:20+09:00",
  "confirmedAt": null
}
```

수급모델 실행까지 접수/완료된 확정 파라미터:

```json
{
  "scenarioId": 309,
  "currentState": "EXECUTED_CONFIRMED",
  "parameterSource": "CONFIRMATION_PARAMETERS",
  "execPrepId": 51002,
  "modelParamProposalId": 10001,
  "scenarioGroupId": 61,
  "eventId": 1378,
  "caseId": 88001,
  "runId": "20260709_153000_abcd",
  "modelExecutionStatus": "COMPLETED",
  "executionInputFiles": {
    "complete": true,
    "missingFileRoles": [],
    "files": [
      {"fileRole": "DEMAND", "fileNo": 991001, "fileNm": "DEMAND_88001.xlsx", "fileSize": 1048576},
      {"fileRole": "SUPPLY", "fileNo": 991002, "fileNm": "SUPPLY_88001.xlsx", "fileSize": 1048576},
      {"fileRole": "TRADE", "fileNo": 991003, "fileNm": "TRADE_88001.xlsx", "fileSize": 1048576}
    ]
  },
  "modelFiles": [
    {
      "modelFile": "Demand",
      "modelSheets": [
        {
          "modelSheet": "WeatherAdjustment",
          "modelTables": [
            {
              "tableName": "table1",
              "parameters": [
                {
                  "parameter": {
                    "parameterName": "Temperature",
                    "modelTarget": {
                      "modelFile": "Demand",
                      "modelSheet": "WeatherAdjustment",
                      "tableName": "table1",
                      "valueFieldNames": ["Temperature"],
                      "slotAxesFieldNames": ["Country", "Year", "Month"]
                    },
                    "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
                    "values": [
                      {
                        "Country": "Germany",
                        "Year": 2027,
                        "Month": 1,
                        "basecaseValue": 1.2,
                        "proposedValue": -3.5,
                        "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
                      }
                    ]
                  },
                  "rendering": {
                    "readMode": "FIXED_COLUMNS",
                    "columns": [
                      {"column": "K", "role": "Year", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "L", "role": "Month", "kind": "SLOT", "align": "RIGHT"},
                      {"column": "M", "role": "Country", "kind": "SLOT", "align": "LEFT"},
                      {"column": "O", "role": "Temperature (°C)", "kind": "VALUE", "align": "RIGHT"}
                    ],
                    "semanticAxes": "Country, Year, Month 기준 기온 조정값"
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "failureReason": null,
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "completedAt": "2026-07-03T15:00:20+09:00",
  "confirmedAt": "2026-07-03T15:01:00+09:00"
}
```

`currentState`:

- `NOT_REQUESTED`: 파라미터 제안 이력이 없음. `modelFiles=[]`
- `GENERATING`: Agent proposal 생성 중. `modelFiles=[]`
- `PROPOSAL_READY`: Agent proposal 생성 완료. `proposal_parameters` tree
- `CONFIRMED_PENDING_EXECUTION`: 사용자 확정 후 실행 Case 생성 전 또는 Excel 생성 중. `confirmation_parameters` tree
- `EXECUTING`: 수급모델 실행 접수 후 Case/Run이 `RUNNING`. `confirmation_parameters` tree
- `EXECUTED_CONFIRMED`: 최신 연결 Case/Run이 `COMPLETED`. 연결 proposal의 현재 `confirmation_parameters` tree
- `PROPOSAL_FAILED`: proposal 생성 실패. `modelFiles=[]`
- `EXECUTION_FAILED`: confirmation 이후 Excel/파일/Case·Run commit 이전 실패 또는 commit 이후 dispatch/모델 실행 실패. confirmation payload가 있으면 tree, 없으면 `parameterSource=NONE`, `modelFiles=[]`. 연결 Case/Run이 있고 `modelExecutionStatus=FAILED`이면 dispatch 또는 모델 실행 실패이고, `case_id`가 없고 `modelExecutionStatus=null`이면 Backend confirmation/Excel/file/Case·Run commit 이전 단계 실패다.

`parameterSource`:

- `NONE`: 반환할 파라미터 없음, `modelFiles=[]`
- `PROPOSAL_PARAMETERS`: Agent `proposal_parameters`를 tree로 반환
- `CONFIRMATION_PARAMETERS`: Agent `confirmation_parameters`를 tree로 반환

### 5.2.2 A4a/A4/A8/A9 공통 response-only grouped rationale projection

다음 네 public GET은 Agent parameter를 응답할 때 각 parameter object에 `groupedRationales[]`를 추가한다. 추가 public GET endpoint는 만들지 않는다.

| API | response path |
|---|---|
| A4a current-state GET | `modelFiles[].modelSheets[].modelTables[].parameters[].parameter.groupedRationales` |
| A4 proposal-detail GET | `parameters[].groupedRationales` |
| A8 latest confirmed parameters GET | `parameters[].groupedRationales` |
| A9 latest proposed parameters GET | `parameters[].groupedRationales` |

목적과 shape:

- `groupedRationales`는 같은 고정 행(`rendering.columns.kind=SLOT`)에 속한 시간별 value-level rationale을 화면에서 함께 표시하기 위한 Backend read projection이다.
- grouping 축은 `modelTarget.slotAxesFieldNames` 중 parameter master의 `basecase_column_roles`와 일치하여 `rendering.columns.kind=SLOT`으로 해석되는 field만 사용한다.
- `MATRIX_TIME_COLUMNS`의 `TIME_AXIS` field(예: `Year`, `Month`, `Period`)는 grouped item과 그룹 key에서 제외한다.
- 각 grouped item은 SLOT field를 `modelTarget.slotAxesFieldNames`의 선언 순서대로 포함하고 마지막에 `rationale`을 포함한다.
- parameter-level `rationale`, 원본 `values[]`, 모든 value row와 `values[].rationale`, `basecaseValue`, `proposedValue`, dynamic slot-axis key/value 및 배열 순서는 그대로 보존한다.
- 동일 slot 조합의 `values[]` row를 병합·삭제·재정렬하지 않는다. `groupedRationales`는 기존 데이터를 대체하지 않는다.

그룹 key와 순서:

- 그룹핑은 각 parameter 내부에서 독립적으로 수행한다. 서로 다른 parameter의 row는 절대 같은 그룹이 되지 않는다.
- 그룹 key는 위 규칙으로 해석된 `SLOT` field의 값만으로 구성한다.
- `TIME_AXIS` field 값이 달라도 `SLOT` field 값이 모두 같으면 같은 그룹이다.
- `basecaseValue`, `proposedValue`, parameter/value `rationale`은 그룹 key에 포함하지 않는다.
- 문자열과 boolean slot은 JSON 타입과 값을 그대로 비교한다.
- 숫자 slot은 A6 의미값 canonicalization과 일관되게 숫자값으로 비교한다. 따라서 JSON number `1`과 `1.0`은 같은 그룹이다.
- 숫자값 기준으로 같은 그룹이더라도 grouped item의 slot 값은 그 그룹이 최초 등장한 `values[]` element의 원래 JSON number 표현과 타입을 보존한다. 예를 들어 최초 값이 `1.0`이면 출력도 `1.0`이다.
- 그룹 배열은 해당 slot 조합이 `values[]`에 최초 등장한 순서를 유지한다.
- grouped item의 dynamic property 순서는 `slotAxesFieldNames` 중 SLOT field의 원래 순서이며 `rationale`은 마지막 property다.

Rationale 결합:

- 같은 그룹의 `values[].rationale`을 value row의 원래 등장 순서대로 `\n` 하나로 연결한다.
- rationale 문자열은 trim하거나 중복 제거하지 않는다.
- 그룹에 value row가 하나뿐이면 그 row의 rationale 문자열을 그대로 사용한다.

예시:

```json
{
  "parameterName": "LiqUtilAdjustment",
  "modelTarget": {
    "modelFile": "Supply",
    "modelSheet": "LiqUtilAdjustment",
    "tableName": "table1",
    "valueFieldNames": ["LiqUtilAdjustment"],
    "slotAxesFieldNames": ["Country", "LNG Plant", "Year", "Month", "Period"]
  },
  "rationale": "기존 parameter rationale",
  "groupedRationales": [
    {
      "Country": "Algeria",
      "LNG Plant": "Arzew GL1Z",
      "rationale": "A\nB"
    }
  ],
  "values": [
    {
      "Country": "Algeria",
      "LNG Plant": "Arzew GL1Z",
      "Year": 2027,
      "Month": 1,
      "Period": 109,
      "basecaseValue": 0.2,
      "proposedValue": 0.21,
      "rationale": "A"
    },
    {
      "Country": "Algeria",
      "LNG Plant": "Arzew GL1Z",
      "Year": 2027,
      "Month": 2,
      "Period": 110,
      "basecaseValue": 0.2,
      "proposedValue": 0.22,
      "rationale": "B"
    }
  ]
}
```

Validation/error boundary:

- A4a/A8/A9 공통 tree projector는 endpoint가 선택한 raw Agent parameter JSON의 파싱, master 검증, grouped rationale/rendering 생성, tree 조립을 담당한다. endpoint별 조회 anchor와 public error mapping은 각 서비스 경계가 유지한다. A4는 기존 flat response projection을 유지한다.
- 공통 projection은 parameter name으로 `tmm_mi_model_param_m` master를 일괄 조회해 SLOT 축을 해석한다. master 누락·중복 또는 SLOT 축을 하나도 해석할 수 없는 metadata 불일치는 endpoint의 기존 데이터 불일치/error 정책으로 실패한다.
- required slot-axis field가 value row에 없거나 projection에 필요한 `modelTarget.slotAxesFieldNames`, `values[]`, `values[].rationale`가 유효하지 않으면 해당 row를 조용히 제외하거나 빈 `groupedRationales`로 정상화하지 않는다. endpoint의 기존 데이터 불일치/error 정책으로 실패한다.
- 특히 A8/A9의 parameter JSON이 `null`, blank, non-array, malformed JSON이면 기존 `API_RESPONSE_ERROR`와 `Agent proposal parameter JSON is invalid.` message를 유지하며 빈 배열로 반환하지 않는다.
- A4a의 non-`NONE` payload 정합성 오류는 기존 `409 BIZ_RULE_VIOLATION` 정책을 유지한다. A4의 status별 empty array와 기존 parse/error 정책도 유지한다.

Wire/storage boundary:

- `groupedRationales`는 Agent `proposal_parameters`/`confirmation_parameters` 저장 JSON에 쓰지 않는다.
- A6 `confirmationParameters[]`, Agent X2 confirmation request의 `ModelParamPayloadItem[]`, B1 callback의 optional `proposalParameters`에 포함하지 않는다.
- `ModelParamPayloadItem`과 `AgentModelParamProposal` entity는 변경하지 않는다.
- A4a typed response와 A4/A8/A9 JSON response는 동일한 공통 projection component를 사용하며 입력 Agent JSON tree를 직접 mutate하지 않는다.

### 5.3 SSE

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/stream`

성공:

```text
event: model-parameter-proposal
data: {"scenarioId":309,"modelParamProposalId":10001,"status":"PROPOSED"}
```

실패:

```text
event: model-parameter-proposal
data: {"scenarioId":309,"modelParamProposalId":10001,"status":"FAILED"}
```

- SSE는 신호만 제공한다.
- FE는 이벤트 수신 직후 5.2 상태 API를 재조회한다.
- 연결 유실 시 재연결 후 상태 API를 조회한다.
- polling fallback을 허용한다.

### 5.4 Agent callback

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/callback`

- direction: Agent → Backend
- Content-Type: `application/json`
- path `{scenarioId}`는 현재 in-flight execution-preparation을 찾는 scope key다. `data.modelParamProposalId`가 있으면 in-flight 후보 검색 조건으로 사용하고, 없으면 path `scenarioId`의 현재 in-flight row에만 수렴한다.
- authentication header: `Authorization: Bearer {callback-token}`
- authentication 검증: 기존 시나리오 설계 콜백과 동일하게 표준 Resource Server JWT 체인(`CustomJwtDecoder`, HS256 self-issued) + `@RequiredPermissionRules(Role.SYSTEM ...)`을 재사용한다. Agent는 Backend 토큰 발급 API와 발급 인풋 2종으로 JWT를 자체 발급·갱신한다. (별도 static token/HMAC/외부 OAuth2 도입 없음)
- callback URL은 Backend가 결정하며, Agent는 전달받은 값을 `callbackBaseUrl + callbackPath` 등의 배포 운영 설정으로 관리한다.
- Backend는 `{status, message, data}` wrapper를 그대로 수신한 뒤 boundary에서 내부 DTO로 normalize한다. **이 방식(wrapper 수용 + boundary normalize)을 1순위 계약으로 채택한다.** Agent에 flat 페이로드로의 변경을 요청하지 않는다.
- 채택 근거: **본 기능 Agent 담당자의 API 규약(1.2.1 camelCase key, 1.2.2 response wrapper `{status, message, data}`)이 이 콜백에 적용된다.** 담당 개발자별 컨벤션 차이가 있어(기존 Backend 시나리오 설계 콜백 `ScenarioSignpostsCallbackReq`는 flat `{result, reason, ...}`), **이 파라미터 제안 콜백 API만 Agent 담당자 규약인 wrapper + camelCase에 맞춘다.** boundary normalize로 도메인 레벨 일관성(공용 `AgentCallbackResult` enum 공유)을 확보한다.
- callback endpoint는 `permitAll` 또는 네트워크 격리만으로 보호하지 않는다.

#### 5.4.1 채택 callback 계약 (wrapper 수용 + boundary normalize, 1순위)

2026-07-03 Notion wrapper 계약을 변경하지 않고 그대로 수신하며, Backend boundary에서 내부 DTO로 normalize하는 채택 계약이다. Agent에 flat 페이로드 변경을 요청하는 대안은 채택하지 않는다.

**callback은 완료 신호(signal)다.** canonical proposal 결과·상태는 callback payload가 아니라 **Backend가 Agent proposal 테이블을 재조회**해 얻는다(Domain Design 3.5). 따라서 Backend는 callback의 `data.proposalParameters` 내용을 저장·신뢰하지 않으며, payload hash / CAS / terminal overwrite 보호를 두지 않는다. Agent가 proposal 테이블 commit 이후 신호를 발행(write-then-callback)한다는 전제다.

Request:

```json
{
  "status": "success",
  "message": "model parameter proposal completed",
  "data": {
    "modelParamProposalId": 10001,
    "scenarioId": 309,
    "scenarioGroupId": 61,
    "eventId": 1378,
    "status": "PROPOSED"
  }
}
```

Validation:

- wrapper `status`: `success | error`; `data.status=PROPOSED`이면 `success`, `FAILED`이면 `error`여야 한다.
- normalize: wrapper `status`를 boundary에서 공용 `AgentCallbackResult`로 매핑한다 — `success -> SUCCESS`, `error -> FAILURE`. (`AgentCallbackResult`는 `success`/`failure`/`error`를 이미 수용하므로 Agent가 `failure`를 보내도 `FAILURE`로 흡수된다.) 이로써 시나리오 설계 콜백과 동일한 성공/실패 enum을 공유한다.
- `data.status`(`PROPOSED | FAILED`)는 도메인 terminal 상태이며 wrapper `status`(전송 성공/실패)와 별개다. 두 값이 어긋나면(`success + FAILED` 등) `REQ_INVALID_PARAMETER`로 거절한다.
- `message`: 필수. 실패 시 Backend failure reason의 입력으로 사용할 수 있다.
- correlation은 `data.modelParamProposalId`가 있으면 path `scenarioId`의 in-flight execution-preparation 후보 중 같은 Agent proposal ID를 가진 row를 우선한다. 없으면 path `scenarioId`의 현재 in-flight execution-preparation scope로만 처리한다. `modelParamProposalId`와 `scenarioId` 어느 쪽도 terminal/history row를 다시 선택하는 단독 key로 쓰지 않는다.
- **실패 콜백**: 규약 1.2.2대로 `error` 콜백의 `data`가 `null`/빈 object여도 무방하다. wrapper `status=error`(+`message`)로 실패 처리하고 결과는 path `scenarioId` 재조회로 확정한다.
- `data.status`: `PROPOSED | FAILED`만 허용한다. (canonical 상태는 재조회로 확정하며, 이 값은 신호 힌트다)
- `data.proposalParameters`: Notion 계약상 존재하나 **Backend는 내용을 저장·신뢰하지 않고 무시**한다(재조회로 대체).
- `modelTarget.*`, `modelFile` casing 등 payload 세부는 재조회 대상이므로 callback 단계에서 검증하지 않는다.

Processing (signal):

1. 합의된 Bearer token 검증 방식으로 호출 주체를 인증한다.
2. wrapper를 역직렬화하고 식별자·`status`(성공/실패 힌트)만 내부 신호 DTO로 normalize한다.
3. `data.modelParamProposalId`가 있으면 path `scenarioId`의 in-flight execution-preparation 후보 중 같은 Agent proposal ID를 가진 row를 조회하고 교차검증한다.
4. `modelParamProposalId`가 없으면 path `scenarioId`의 현재 in-flight execution-preparation이 정확히 1건일 때만 조회한다. 없으면 late/no-op, 여러 건이면 invariant violation으로 처리한다.
5. **Agent proposal 테이블을 재조회**해 Agent proposal ID와 canonical 상태·결과를 얻는다.
6. preparation의 `modelParamProposalId`가 비어 있으면 재조회 ID를 조건부 저장한다. 이미 같은 ID면 no-op, 다른 ID면 409로 거절하고 덮어쓰지 않는다.
7. Backend orchestration 행 상태를 재조회 결과로 갱신한다 — monotonic. `PROCESSING` 이후 단계면 no-op.
8. orchestration write commit 이후 SSE를 발행한다.

Backend 내부 idempotency:

- 중복/지연/순서역전 callback은 재조회로 수렴하는 **멱등 신호**다. 별도 payload hash / CAS는 없다.
- 이미 `PROCESSING` 이후 단계(`CONFIRMING`/`CONFIRMED`/`EXECUTING`)인 proposal에 도착한 신호는 상태를 되돌리지 않고 no-op이다(monotonic guard). in-flight row가 없는 late callback도 terminal/history row에 붙이지 않고 no-op으로 수렴한다.
- Backend는 callback payload를 저장하지 않으므로 "충돌 payload" 판정 자체가 없다. 결과 정합성은 Agent 테이블이 단일 진실원이다.

Backend response 최소안:

- Agent가 처리 결과의 세부 disposition을 해석한다는 요구가 확인되지 않았으므로 성공은 `200 BaseResponse<Void>`를 우선한다.
- callback이 신호이므로 disposition 구분(APPLIED/DUPLICATE 등)은 두지 않는다. 모든 정상/no-op 처리는 단순 `200`으로 수렴한다.

#### 5.4.2 Proposed Contract — Agent 협의 필요

> callback을 완료 신호로 격하하고 결과를 Agent 테이블 재조회로 얻으므로, callback race/멱등 목적의 `callbackCorrelationId`·`callbackEventId`·payload hash 확장은 **moot**(불필요)이다. 아래 표는 협의 이력으로 남긴다.

| 제안 | 목적 | 최소 계약 대안 |
|---|---|---|
| proposal request의 runtime `callbackUrl` | 요청별 callback routing이 필요한 미래 요구 | Backend가 정한 URL을 Agent 배포 설정에 등록하는 confirmed 방식 유지 |
| `callbackCorrelationId` request/callback echo | Agent HTTP 응답 저장 전 callback race 제거 | Agent가 HTTP response 완료 후 callback 시작을 보장 |
| `callbackEventId` stable UUID | retry 단위의 명시적 멱등키 | `modelParamProposalId + terminal status + payload hash` |
| `schemaVersion` | callback schema evolution | 배포 호환성으로 관리 |
| `completedAt` | Agent 작업 완료 시각 보존 | Backend `receivedAt` 저장 |
| `errorCode` | 구조화된 실패 분류 | wrapper `message` 저장 |
| `errorMessage` | 실패 상세 | wrapper `message`와 중복이므로 최소안에서는 불필요 |
| ack `disposition` | Agent가 duplicate/late 결과를 구분 | 모든 정상/no-op 처리를 단순 2xx로 수렴 |

flat callback DTO는 Notion wrapper와 직접 충돌하므로 Agent 동의 전에는 제안하지 않는다. 확장이 필요하면 wrapper의 `data` 내부에 필드를 추가하는 방향을 우선한다.

#### 5.4.3 Resolved / Open

**Resolved (확정)**

- 인증: 기존 콜백과 동일한 self-issued JWT + `@RequiredPermissionRules(Role.SYSTEM ...)` 재사용. Agent가 Backend 토큰 발급 API + 인풋 2종으로 자체 발급·갱신한다.
- 공유 DB read: Agent proposal 테이블 read 가능(권한 확정). callback = 신호, 결과는 재조회.
- 순서: write-then-callback 전제(Agent가 proposal 테이블 commit 후 신호 발행). **2026-07-07 Agent 답변으로 명시 확정.** 재조회로 수렴하므로 `callbackCorrelationId`/`callbackEventId`/payload hash는 moot(불필요).
- wrapper 셰이프: callback `{status, message, data}` wrapper 필드 셰이프가 Agent 규약 1.2.2와 일치함을 **최종 확인(2026-07-07)**. 식별자 4종(`modelParamProposalId`, `scenarioId`, `scenarioGroupId`, `eventId`)+`data.status`(`PROPOSED|FAILED`)는 `data` 내부이며, 성공 시 채우고 실패(`error`) 시 `data`(null/빈 object) 생략은 규약대로 허용(correlation은 path `scenarioId`).
- retry/ack: callback이 멱등 신호이므로 중복·지연은 Backend monotonic guard로 no-op 수렴. Agent가 ack disposition을 해석할 필요 없음(단순 `200`).

**Open (남은 확인 — 콜백 계약을 막지는 않음)**

1. `schemaVersion`, `completedAt`, `errorCode`가 Agent 운영/감사에 필요한가(선택적 확장; 최소안 미포함).

### 5.5 사용자 확정 및 Model Execution 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/from-parameters`

- `Content-Type: application/json`
- Backend→FE HTTP status: `202 Accepted`
- `PARAMETER_PROPOSAL` mode에서만 허용
- 이 mode에서는 사용자 파일 upload 없음

Request:

```json
{
  "modelParamProposalId": 10001,
  "confirmationParameters": [
    {
      "parameterName": "Temperature",
      "modelTarget": {
        "modelFile": "DEMAND",
        "modelSheet": "WeatherAdjustment",
        "tableName": "Table2",
        "valueFieldNames": ["Temperature"],
        "slotAxesFieldNames": ["Country", "Year", "Month"]
      },
      "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
      "values": [
        {
          "Country": "Germany",
          "Year": 2027,
          "Month": 1,
          "basecaseValue": 1.2,
          "proposedValue": -3.5,
          "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
        }
      ]
    }
  ]
}
```

처리 순서:

1. Scenario, permission, proposal ownership/status 검증.
2. Agent proposal 테이블의 `proposal_parameters` array와 request `confirmationParameters`의 parameter/model target/slot identity 및 `basecaseValue` 구조 비교
3. `values[].proposedValue`가 numeric인지 검증한다. parameter/value `rationale`은 required string이지만 Agent confirmation에서 보정될 수 있고 Excel 실행 입력에 쓰이지 않으므로 문자열 동일성 비교에서는 제외한다.
4. 같은 Scenario에 `RUNNING` Case가 없는지 기존 active-run guard로 확인
5. Agent proposal 상태를 재조회한다. `PROPOSED`면 Agent confirmation API를 호출한다. `CONFIRMED`이고 request `confirmationParameters`가 Agent `confirmation_parameters`와 동일하면 호출을 생략한다. `CONFIRMED`이지만 값이 다르면 같은 proposal 재확정(Option A)을 위해 Agent confirmation API를 재호출해 Agent `confirmation_parameters`를 최신 확정값으로 갱신한다.
6. Agent `CONFIRMED` 응답 확인
7. 협의된 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개 로드
8. 확정값을 지정 file/sheet/table/slot에 merge
9. 기존 Scenario Case/Run 생성과 Kubernetes Job dispatch를 재사용한다. 파일 3개는 Scenario lock/Case transaction 전에 직렬로 Object Storage의 최종 `SCNRCASE/{uuid}` key에 업로드하고, `tcm_filel`에는 Case 미연결 `TEMP/0` staged row로 기록한다. 최종 짧은 transaction에서 Scenario lock/active guard 뒤 해당 시나리오의 active 가격전망해석 row를 write lock으로 조회한다. row가 있으면 `del_yn='Y'`로 soft delete하고 없으면 no-op으로 처리한 뒤, Case INSERT·staged row 3건의 `SCNRCASE + case_id` metadata-only attachment·Run INSERT·Case `lastRunId`·`execprep.case_id + EXECUTING`을 함께 commit한다. 이 transaction에는 Object Storage copy/move/delete를 넣지 않으며 K8s dispatch는 commit 이후 수행한다(Domain Design 규칙 29~30). 최종 lock/unique conflict는 기존 정책대로 처리하고 staged file 즉시 삭제 비용을 수용한다. commit 이후 dispatch 실패는 Case/Run을 `FAILED`, 연결 preparation을 `EXECUTION_FAILED`로 처리하며 soft delete한 기존 가격전망해석은 복원하지 않는다. Run에는 `failureReasonCode=JOB_SUBMIT_FAILED`, `errorMessage=Model execution dispatch failed.`만 저장하고 실제 exception stack/root cause는 실행 식별자와 함께 server log에만 기록한다.
10. 기존 `ModelExecutionAcceptRes` 반환

Staging lifecycle:

- JSON과 multipart 실행 모두 같은 staging/registrar 경계를 사용한다.
- 3개 업로드는 이번 단계에서 직렬 처리하며, bounded parallelism은 transaction 분리 효과 측정 후 별도 변경으로 검토한다.
- final transaction 전에 실패하면 성공한 staged file을 즉시 보상 삭제한다.
- process crash로 남은 `TEMP/0 + SCNRCASE storage path` row/object는 생성 후 24시간 TTL이 지나면 bounded cleanup 대상이다.
- staged `file_no`는 final Case attachment가 성공하기 전 API 응답에 포함하지 않는다.

**재시도·멱등 (P0 resolved):** callback과 동일한 monotonic read-back 패턴으로 confirmation을 취급한다.

- 재시도 단위는 **실행 POST 전체 재요청**이다(부분 resume 없음).
- **동일값 재시도에 대해서는 Agent confirmation 멱등성을 계약 전제로 요구하지 않는다.** Backend가 호출 직전 proposal 상태를 재조회(5번)하고 이미 `CONFIRMED`이며 request 값이 저장값과 같으면 confirmation을 생략하므로, 재시도·응답 유실(Agent는 CONFIRMED, Backend는 응답 미수신) 상황에서도 이중 confirm이 발생하지 않는다. Agent가 추가로 confirmation을 멱등 처리하면 방어적 보너스이며 계약 전제는 아니다.
- confirmation 성공 이후 단계(원본 로드·merge·dispatch) 실패는 Agent `CONFIRMED`를 롤백하지 않는다. 사용자가 실행 POST를 재요청하면 Backend는 `CONFIRMED`를 확인하고 confirmation을 건너뛴 뒤 Excel merge·Case/Run·dispatch를 다시 수행한다.
- 재요청의 `confirmationParameters`가 이미 저장된 `confirmation_parameters`와 동일하면 기존 confirmed 값을 재사용한다. 다르면 같은 `modelParamProposalId` 재확정(Option A)으로 Agent `confirmation_parameters`를 갱신한 뒤 새 `tmd_mi_model_execpreph` row와 새 `case_id`로 실행한다.
- Case/Run 중복 가드는 **Scenario 기준 RUNNING 최대 1개**만 유지한다. 동일 proposal request에서 여러 번 실행 접수할 수 있으며, 각 접수는 새 `case_id`를 만든다. 이미 RUNNING Case가 있으면 409로 차단한다.
- 확정 전제: Agent가 `CONFIRMED` 시점에 `confirmation_parameters`를 저장하고, 이미 `CONFIRMED`인 proposal의 재확정을 허용한다(Option A). 재확정 후 Agent `confirmation_parameters`는 최신 확정값으로 갱신된다.

FE는 GET으로 받은 proposal array를 local form state로 유지한다. 실행 요청 시 각 row의 현재 `proposedValue`를 `confirmationParameters`에 담는다. 사용자가 수정한 row는 수정값을, 수정하지 않은 row는 Agent 생성값을 그대로 보낸다. Backend는 parameter/model target/slot identity와 `basecaseValue`를 검증한 뒤 confirmation array를 Agent confirmation API에 전달한다. `rationale`은 required transport field지만 사용자 편집 대상이나 Excel 실행 입력이 아니며 Agent 저장 과정에서 보정될 수 있으므로 실행 동일성 판단에 사용하지 않는다.

Response는 기존 실행 접수 응답을 그대로 사용한다.

Backend Agent client는 5번에서 Agent Notion 계약의 wrapper를 정확히 전송한다. `modelFile`은 Backend 대문자 enum을 Agent casing인 `Demand | Supply | Trade`로 역변환한다.

Agent로 전송하는 body는 `{data:{confirmationParameters:[...]}}`이며 5.5 request의 flat parameter array를 그대로 사용한다.

### 5.5.1 A8/A9 공통 Latest Model Parameter Proposal Execution Anchor

A8/A9는 동일한 최신 A6 실행을 기준으로 조회해야 한다. 이 규칙은 신규 HTTP endpoint나 API response DTO가 아니라 Backend 내부 공통 조회 context다. A4a의 `executionInputFiles`는 이 resolver를 사용하지 않고 A4a가 선택한 최신 execution-preparation row의 `caseId`를 그대로 사용한다.

`PARAMETER_PROPOSAL` 시나리오는 A6 JSON execution만 Case를 생성하고 A7 multipart execution은 `inputMode` guard로 차단한다. 따라서 이 전제에서 시나리오의 최신 Scenario Case는 최신 A6 execution Case와 동일하다.

공통 logical lookup:

1. `tmd_mdl_scnrcasem`에서 `scnr_id = :scenarioId`, `exec_type_cd = 'SCENARIO'`, `del_yn = 'N'`인 최신 Case를 `case_id DESC` 기준으로 1건 선택한다.
2. `tmd_mi_model_execpreph`에서 `case_id = latestCase.case_id`, `del_yn = 'N'`인 A6 execution-preparation row를 찾는다.
3. 아래 내부 context를 구성한다.

```text
LatestModelParamProposalExecutionContext
  Long scenarioId
  Long caseId
  Long execPrepId
  Long modelParamProposalId          tmd_mi_model_execpreph.agnt_prpsl_id
```

공통 실패 규칙:

- 최신 Scenario Case가 없으면 `404 RSC_NOT_FOUND`.
- 최신 Case에 연결된 non-deleted execution-preparation row가 없거나 `agnt_prpsl_id`가 null이면 data invariant 위반으로 로깅하고 `404 RSC_NOT_FOUND`.
- 같은 `scenarioId + modelParamProposalId`로 여러 번 실행할 수 있으므로 proposal ID가 아니라 최신 `case_id`가 anchor다.

physical join/index/EXPLAIN은 canonical DDL draft `docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md`를 따른다.

### 5.6 최근 실행 기준 확정/제안 파라미터 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters`

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters`

두 endpoint 모두 5.5.1의 `LatestModelParamProposalExecutionContext`를 사용한다.

endpoint별 추가 조회:

1. context의 `modelParamProposalId`로 Agent proposal 테이블을 read-only 조회한다.
2. confirmed endpoint는 Agent `confirmation_parameters`, proposed endpoint는 Agent `proposal_parameters`를 선택한다.
3. 선택한 payload를 A4a와 동일한 공통 projector로 변환해 `modelFiles > modelSheets > modelTables > parameters > {parameter, rendering}` tree를 반환한다. `parameter.groupedRationales`와 `rendering.readMode`, `rendering.columns`, `rendering.semanticAxes`의 세부 계약은 5.2.1/5.2.2를 따른다. Agent 저장 JSON은 변경하지 않는다.
4. confirmed endpoint의 `parameterSource`는 `CONFIRMATION_PARAMETERS`, proposed endpoint는 `PROPOSAL_PARAMETERS`다.

confirmed endpoint(A8) 의미:

- A8은 최신 A6-linked Case와 연결된 proposal의 **조회 시점 현재값**인 Agent `confirmation_parameters`를 반환한다.
- 같은 proposal을 재확정하면 Agent row가 갱신되므로, A8은 해당 `caseId` 실행 시점에 실제 사용된 immutable parameter snapshot을 보장하지 않는다. 이 동작은 직전 실행 화면에서 연결 proposal의 현재 confirmation 값을 보여주는 현행 요구사항으로 수용한다.
- 해당 Case가 실제 사용한 input은 `SCNRCASE + caseId` 파일 3개가 source of truth다. A4a는 자신의 최신 execution-preparation이 `EXECUTED_CONFIRMED`일 때 그 row의 `caseId` 파일을 `executionInputFiles`로 노출한다. A8의 현재 confirmation 값과 Case input 파일의 값이 항상 동일하다고 가정하지 않는다.
- 실행별 exact confirmed parameter 복원은 현행 범위가 아니며, `tmd_mi_model_execpreph`에 confirmed JSON snapshot을 추가하지 않는다.

Response data:

```json
{
  "scenarioId": 309,
  "caseId": 88001,
  "execPrepId": 51001,
  "modelParamProposalId": 10001,
  "parameterSource": "CONFIRMATION_PARAMETERS",
  "modelFiles": [
    {
      "modelFile": "Demand",
      "modelSheets": [
        {
          "modelSheet": "WeatherAdjustment",
          "modelTables": [
            {
              "tableName": "table1",
              "parameters": [
                {
                  "parameter": {
                    "parameterName": "Temperature",
                    "modelTarget": {
                      "modelFile": "Demand",
                      "modelSheet": "WeatherAdjustment",
                      "tableName": "table1",
                      "valueFieldNames": ["Temperature"],
                      "slotAxesFieldNames": ["Country", "Year", "Month"]
                    },
                    "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
                    "groupedRationales": [
                      {
                        "Country": "Germany",
                        "Year": 2027,
                        "Month": 1,
                        "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
                      }
                    ],
                    "values": [
                      {
                        "Country": "Germany",
                        "Year": 2027,
                        "Month": 1,
                        "basecaseValue": 1.2,
                        "proposedValue": -3.5,
                        "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
                      }
                    ]
                  },
                  "rendering": {
                    "readMode": "FIXED_COLUMNS",
                    "columns": [],
                    "semanticAxes": null
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

- latest Case/execprep 404는 5.5.1 공통 실패 규칙을 따른다.
- 같은 Agent proposal의 값 변경 재실행은 Agent `confirmation_parameters`를 갱신하는 Option A를 따른다. A8의 current-value 의미와 과거 실행별 exact snapshot 비지원은 위 계약으로 확정됐다.
- A8/A9 parameter JSON이 `null`, blank, non-array, malformed JSON이면 `API_RESPONSE_ERROR`와 `Agent proposal parameter JSON is invalid.`을 유지한다. response projection이 이를 빈 배열로 정상화하지 않는다.
- 기존 A8/A9 empty-array 동작은 유지한다. Agent parameter JSON이 유효한 `[]`이면 `parameterSource`는 endpoint별 값으로 유지하고 `modelFiles=[]`를 반환한다. A4a non-`NONE` source의 empty payload 409 계약과는 구분한다.
- 공통 projector의 parameter/master/좌표/grouped-rationale/rendering 검증이 실패해도 A8/A9 경계에서는 동일한 `API_RESPONSE_ERROR`와 public message로 매핑한다. A4a의 `409 BIZ_RULE_VIOLATION` data mismatch 계약을 A8/A9에 노출하지 않는다.
- A8/A9 응답에는 `parameters`를 포함하지 않는다. A4a envelope의 `currentState`, `modelExecutionStatus`, `executionInputFiles`, `failureReason`, timestamp도 추가하지 않는다.

### 5.7 A4a 성공 실행 input Excel 파일 projection

A4a `GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals` 응답의 nullable `executionInputFiles` 계약이다. 독립 `GET .../model-executions/latest/input-files` endpoint(A10)는 만들지 않는다. 실제 파일 다운로드는 기존 File Management API를 아래 계약으로 재사용한다.

```http
POST /api/v1/file-management/download/file
Content-Type: application/json
```

```json
{
  "fileNo": 991001,
  "fileNm": "DEMAND_88001.xlsx",
  "fileSize": 1048576
}
```

- download request의 `fileNo`, `fileNm`, `fileSize`는 모두 required이며 A4a `executionInputFiles.files[]`의 동명 필드를 그대로 전달한다.
- 이 공통 다운로드 API는 `GET` 또는 `fileNo` 단독 query parameter 계약이 아니다.
- 성공 응답은 `BaseResponse<T>`가 아닌 파일 binary `ResponseEntity<byte[]>`다.

조회 기준:

1. A4a가 선택한 최신 non-deleted execution-preparation row와 연결 Case/Run으로 `currentState`를 판정한다.
2. `currentState=EXECUTED_CONFIRMED`이면 같은 row의 `caseId` 기준으로 기존 실행 파일 저장 규칙(`tcm_filel`, `SCNRCASE + case_id`)에서 input Excel 3개 파일을 조회한다.
3. 파일 role은 기존 저장 metadata 또는 filename convention으로 `DEMAND`/`SUPPLY`/`TRADE`를 식별한다.
4. 그 외 상태에서는 파일을 조회하지 않고 `executionInputFiles=null`을 반환한다.

Response data:

```json
{
  "executionInputFiles": {
    "complete": true,
    "missingFileRoles": [],
    "files": [
      {
        "fileRole": "DEMAND",
        "fileNo": 991001,
        "fileNm": "DEMAND_88001.xlsx",
        "fileSize": 1048576
      },
      {
        "fileRole": "SUPPLY",
        "fileNo": 991002,
        "fileNm": "SUPPLY_88001.xlsx",
        "fileSize": 1048576
      },
      {
        "fileRole": "TRADE",
        "fileNo": 991003,
        "fileNm": "TRADE_88001.xlsx",
        "fileSize": 1048576
      }
    ]
  }
}
```

- `complete`는 `DEMAND`/`SUPPLY`/`TRADE` 파일이 각각 정확히 1개씩 존재할 때만 `true`다.
- `missingFileRoles`와 `files`는 항상 array이며 `null`을 반환하지 않는다. 두 array의 role 순서는 `DEMAND`, `SUPPLY`, `TRADE`로 고정한다.
- 정상 3개 세트는 `complete=true`, `missingFileRoles=[]`, `files` 3건으로 반환한다.
- 3개 파일 중 일부가 없으면 HTTP `200`을 유지하고 `complete=false`, 누락 role을 `missingFileRoles`에 담으며, `files`에는 실제 존재하는 파일만 반환한다. 누락 상태는 데이터 불완전으로 로깅한다. `409 BIZ_RULE_VIOLATION` 또는 `500`으로 변환하지 않는다.
- 파일이 모두 없으면 `complete=false`, `missingFileRoles=["DEMAND","SUPPLY","TRADE"]`, `files=[]`를 반환한다.
- A6 정상 실행은 3개 role을 원자적으로 연결하는 것이 invariant다. 불완전 응답은 legacy/운영 데이터 이상을 FE가 안전하게 처리하기 위한 read projection이며 정상 write 결과로 간주하지 않는다.
- 파일 binary를 A4a에서 직접 stream하지 않는다. 공통 파일 다운로드 정책/권한/감사를 재사용한다.

### 5.8 기존 파일 업로드 Model Execution 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions`

- `Content-Type: multipart/form-data`
- `FILE_UPLOAD` mode에서만 허용
- 기존 `files` request part에 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개를 전달한다.
- 최종 실행 등록 transaction에서 해당 시나리오의 active 가격전망해석 row를 조회한다. row가 있으면 soft delete하고 없으면 no-op으로 처리한 뒤 Case/Run 생성을 같은 transaction으로 commit한다.
- 기존 validation, Case/Run 생성, Job dispatch, `ModelExecutionAcceptRes` 계약을 유지한다. 내부 storage 경계는 5.5와 동일하게 최종 `SCNRCASE/{uuid}` 선업로드 → `TEMP/0` staged metadata → 짧은 transaction의 metadata-only Case attachment를 사용한다.
- 파라미터 제안 기능 도입으로 제거하거나 deprecated하지 않는다.

## 6. Request DTO Draft

```text
AgentModelParamProposalReq
  없음                                confirmed: request body 없음

AgentCallbackEnvelopeReq                (Agent 규약 1.2.2 wrapper 그대로 수신)
  AgentCallbackResult status          required, success|error → SUCCESS|FAILURE 로 매핑(공용 enum 재사용)
  String message                      required
  ModelParamProposalCallbackData data nullable — 실패(error) 시 null/빈 object 허용(규약 1.2.2). modelParamProposalId가 있으면 primary correlation, 없으면 path scenarioId의 in-flight scope로 수렴

ModelParamProposalCallbackData          (성공 시 채워짐; 실패 시 null 가능)
  Long modelParamProposalId          optional, 있으면 in-flight 후보 검색 조건
  Long scenarioId                    optional, 있으면 path와 일치 검증
  Long scenarioGroupId               optional, 교차검증
  Long eventId                       optional, 교차검증
  AgentProposalTerminalStatus status optional, PROPOSED|FAILED (도메인 terminal; 없으면 wrapper status로 판정)
  List<ModelParamPayloadItem> proposalParameters optional, 수신하나 Backend 무시(재조회로 대체)

AgentModelParamConfirmationReq
  AgentModelParamConfirmationData data required

AgentModelParamConfirmationData
  List<ModelParamPayloadItem> confirmationParameters required, non-empty

ModelParamProposalExecutionReq
  Long modelParamProposalId          required
  List<ModelParamPayloadItem> confirmationParameters required, non-empty

ModelParamPayloadItem
  String parameterName               required, immutable
  ModelTarget modelTarget            required, immutable
  String rationale                   required, execution-equivalence comparison excluded
  List<Map<String, Object>> values   required, non-empty; dynamic slot-axis keys + basecaseValue(required key, nullable numeric)/proposedValue/rationale

ModelTarget
  String modelFile                   required, immutable, DEMAND|SUPPLY|TRADE
  String modelSheet                  required, immutable
  String tableName                   required, immutable
  List<String> valueFieldNames       required, immutable, size=1 in phase 1
  List<String> slotAxesFieldNames    required, non-empty, immutable
```

`groupedRationales`는 위 request DTO 어디에도 존재하지 않는다. FE는 A4a/A4/A8/A9 응답에서 이 response-only field를 제거한 뒤 A6 `confirmationParameters[]`를 구성하며, Backend는 X2 Agent confirmation에도 기존 `ModelParamPayloadItem[]`만 전송한다. B1 callback input contract도 변경하지 않는다.

`proposal_parameters`와 `confirmation_parameters`는 모두 flat parameter array다. Backend는 parameter identity(`parameterName + modelTarget`), slot-axis 값 조합, `basecaseValue`를 기준으로 구조를 비교한다. `basecaseValue` key는 항상 존재해야 하지만 값은 `null` 또는 numeric을 허용하며, proposal과 confirmation의 `null ↔ null`은 동일 구조다. `null ↔ numeric` 또는 서로 다른 numeric 값은 immutable basecase 변경으로 거절한다. `values[].proposedValue`는 사용자 확정값이므로 달라질 수 있다. parameter/value `rationale`은 required string이지만 Agent confirmation 저장 과정에서 달라질 수 있고 Excel 병합에 사용되지 않으므로 문자열 동일성 및 동일값 재시도 판정에서 제외한다. JSON object property/array 순서와 numeric lexical form(`-3.5`, `-3.50`)도 동일성 판정에서 제외하고 DTO 역직렬화 후 의미값으로 비교한다.

`values[]` validation:

- dynamic slot-axis key 집합은 `modelTarget.slotAxesFieldNames`와 정확히 일치한다.
- `basecaseValue` key는 required이고 값은 nullable numeric이다. `proposedValue`는 required numeric이며 `rationale`은 required string이다.
- phase 1에서는 `valueFieldNames.size() == 1`이므로 하나의 `basecaseValue`/`proposedValue` pair가 해당 value field에 대응한다.
- `basecaseValue`는 UI 비표시·immutable이고 `proposedValue`가 UI 표시·편집·confirmation 전달 값이다.
- confirmation에서는 `proposedValue`만 변경 가능하며 미수정 값도 원본 그대로 포함한다.

`AgentCallbackEnvelopeReq.status`는 wrapper 수신 시 boundary에서 공용 `AgentCallbackResult`(`com.sk.domain.shared.enums.nextgen.AgentCallbackResult`)로 역직렬화한다. 이 enum은 `@JsonCreator`로 `success`/`failure`/`error`를 이미 수용하며, 기존 시나리오 설계 콜백 DTO(`ScenarioGenerationCallbackReq.result` 등)가 쓰는 것과 동일하다. 두 Agent 연동이 같은 성공/실패 enum을 공유해 도메인 레벨에서 일관된다. `data.status`(`AgentProposalTerminalStatus`, `PROPOSED|FAILED`)는 도메인 terminal 상태로 별도 유지한다.

## 7. Response DTO Draft

```text
ModelExecutionOptionsRes
  Long scenarioId
  ModelInputMode inputMode
  boolean parameterProposalAvailable
  boolean fileUploadAvailable

ModelParamProposalAcceptRes
  Long modelParamProposalId
  Long scenarioId
  ModelParamProposalStatus status
  OffsetDateTime requestedAt
  String statusEndpoint

ModelParamProposalRes
  Long modelParamProposalId
  Long scenarioId
  Long scenarioGroupId
  Long eventId
  ModelParamProposalStatus status
  List<ResponseModelParamPayloadItem> parameters response-only groupedRationales 포함
  String failureReason
  OffsetDateTime requestedAt
  OffsetDateTime completedAt

CurrentModelParamProposalRes
  Long scenarioId
  CurrentModelParamProposalState currentState
  ModelParamParameterSource parameterSource
  Long execPrepId                       nullable
  Long modelParamProposalId             nullable
  Long scenarioGroupId                  nullable
  Long eventId                          nullable
  Long caseId                           nullable
  String runId                          nullable, 연결 Case의 latest run id
  String modelExecutionStatus           nullable, RUNNING|COMPLETED|FAILED
  List<CurrentModelParamFileRes> modelFiles required, non-null
  ModelExecutionInputFilesRes executionInputFiles nullable; EXECUTED_CONFIRMED에서만 non-null
  String failureReason                  nullable
  LocalDateTime requestedAt             nullable, 기존 serialization 유지
  LocalDateTime completedAt             nullable, 기존 serialization 유지
  LocalDateTime confirmedAt             nullable, 기존 serialization 유지

CurrentModelParamFileRes
  String modelFile                      required
  List<CurrentModelParamSheetRes> modelSheets required, non-empty

CurrentModelParamSheetRes
  String modelSheet                     required
  List<CurrentModelParamTableRes> modelTables required, non-empty

CurrentModelParamTableRes
  String tableName                      required
  List<CurrentModelParamLeafRes> parameters required, non-empty

CurrentModelParamLeafRes
  CurrentModelParameterRes parameter     required
  CurrentModelParamRenderingRes rendering required

CurrentModelParameterRes
  String parameterName                  required
  CurrentModelParamTargetRes modelTarget required
  String rationale                      required
  List<Map<String, Object>> groupedRationales required, non-null, 5.2.2 dynamic slot-axis projection
  List<Map<String, Object>> values      required, non-empty, dynamic slot-axis keys 보존

CurrentModelParamTargetRes
  String modelFile                      required
  String modelSheet                     required
  String tableName                      required
  List<String> valueFieldNames          required
  List<String> slotAxesFieldNames       required

CurrentModelParamRenderingRes
  BasecaseReadMode readMode             required
  List<ModelParamColumn> columns        required, non-null
  String semanticAxes                   nullable

LatestModelParamProposalParametersRes
  Long scenarioId
  Long caseId
  Long execPrepId
  Long modelParamProposalId
  ModelParamParameterSource parameterSource required; A8=CONFIRMATION_PARAMETERS, A9=PROPOSAL_PARAMETERS
  List<CurrentModelParamFileRes> modelFiles required, non-null; 5.2.1과 동일한 tree

ModelExecutionInputFilesRes
  boolean complete                    required; DEMAND/SUPPLY/TRADE 각각 1개면 true
  List<String> missingFileRoles       required, non-null; DEMAND|SUPPLY|TRADE 고정 순서
  List<ModelExecutionInputFileRes> files required, non-null; 존재하는 role만 DEMAND|SUPPLY|TRADE 고정 순서

ModelExecutionInputFileRes
  String fileRole                    DEMAND|SUPPLY|TRADE
  Long fileNo
  String fileNm
  Long fileSize

```

`ResponseModelParamPayloadItem`은 설명용 response shape 이름이며 Agent request DTO `ModelParamPayloadItem`을 변경하거나 재사용한다는 뜻이 아니다. Java 구현에서 A4는 response-owned `JsonNode` copy를 사용할 수 있고, A4a/A8/A9 tree는 `CurrentModelParameterRes`가 response field를 소유한다. 네 GET의 세부 grouped field 계약은 5.2.2가 canonical이다.

`ModelParamProposalCallbackAckRes`와 `CallbackDisposition`은 Agent-facing confirmed DTO가 아니다. 협의 전 callback 성공 응답은 `BaseResponse<Void>`를 사용한다.

Status enum 후보:

- `PROCESSING`
- `PROPOSED`
- `CONFIRMING`
- `CONFIRMED`
- `EXECUTING`
- `EXECUTION_FAILED`
- `FAILED`

FE가 반드시 처리해야 하는 최소 상태는 `PROCESSING`, `PROPOSED`, `FAILED`, `EXECUTING`이다.

`ModelParamProposalStatus`는 known `modelParamProposalId` 기준 A4 상태/결과 조회용 proposal-scope enum이고, `CurrentModelParamProposalState`는 `scenarioId` 기준 A4a 팝업 표시상태 projection enum이다. 두 enum은 일부 이름을 공유하지만 같은 lifecycle 축이 아니다.

`CurrentModelParamProposalState`:

- `NOT_REQUESTED`
- `GENERATING`
- `PROPOSAL_READY`
- `CONFIRMED_PENDING_EXECUTION`
- `EXECUTING`
- `EXECUTED_CONFIRMED`
- `PROPOSAL_FAILED`
- `EXECUTION_FAILED`

`ModelParamParameterSource`:

- `NONE`
- `PROPOSAL_PARAMETERS`
- `CONFIRMATION_PARAMETERS`

## 8. Representative Error Scenarios

| 상황 | HTTP | ResultCode 후보 | 처리 |
|---|---:|---|---|
| Scenario 없음/삭제 | 404 | `RSC_NOT_FOUND` | proposal/실행 거절 |
| 권한 없음 | 403 | 공통 permission error | 거절 |
| `FILE_UPLOAD` mode에서 proposal 요청 | 409 | `BIZ_RULE_VIOLATION` | workflow 불일치 거절 |
| `FILE_UPLOAD` mode에서 JSON execution | 409 | `BIZ_RULE_VIOLATION` | workflow 불일치 거절 |
| `PARAMETER_PROPOSAL` mode에서 multipart execution | 409 | `BIZ_RULE_VIOLATION` | workflow 우회 거절 |
| `FILE_UPLOAD` mode에서 current-state proposal 조회 | 409 | `BIZ_RULE_VIOLATION` | workflow 불일치 거절 |
| `PROCESSING` proposal 존재 | 409 | `RSC_ALREADY_EXISTS` | 중복 생성 차단 |
| Scenario model execution `RUNNING` | 409 | `RSC_ALREADY_EXISTS` | proposal/실행 차단 |
| Agent proposal 요청 timeout | 504 | `API_TIMEOUT` | local request 생성 시 보상 필요 |
| Agent proposal 요청 실패 | 502/503 | `API_RESPONSE_ERROR`/`API_SERVICE_UNAVAILABLE` | 실패 응답 |
| callback Bearer token 없음/유효하지 않음 | 401 | 공통 authentication error | 저장하지 않음 |
| callback token은 유효하나 호출 권한 없음 | 403 | 공통 permission error | 저장하지 않음 |
| path `scenarioId`에 해당하는 proposal 없음 | 404 | `RSC_NOT_FOUND` | 반영하지 않음; Agent retry 처리는 협의 필요 |
| callback `data` 식별자 존재하나 snapshot 불일치 | 409 | `BIZ_RULE_VIOLATION` | 재조회/반영하지 않음 |
| callback wrapper `status` invalid | 400 | `REQ_INVALID_PARAMETER` | 재조회/반영하지 않음 |
| 동일 callback 재전송 | 200 | `SUC-000` | 재조회 후 monotonic no-op |
| timeout 후 late callback | 200 | `SUC-000` | monotonic guard no-op(terminal/이후 단계 유지) |
| proposal이 `PROPOSED` 또는 허용된 재실행 상태 `CONFIRMED`가 아님 | 409 | `BIZ_RULE_VIOLATION` | 실행 거절. `CONFIRMED`는 동일값 confirmation 생략 또는 Option A 재확정 허용 |
| parameter/target/axis/basecase 구조 변경 또는 numeric이 아닌 `proposedValue` | 400 | `REQ_INVALID_PARAMETER` | 실행 거절. `basecaseValue=null`은 허용하지만 null/numeric 간 변경은 거절. required rationale 누락/타입 오류는 거절하되 문자열 차이 자체는 허용 |
| Agent confirmation 실패 | 502/503 | Agent mapping code | Excel 생성 시작하지 않음 |
| 원본 Excel 없음/version 불일치 | 422/500 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| file/sheet/table/slot mapping 실패 | 422 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| commit 이후 Kubernetes Job dispatch 실패 | 202 접수 후 비동기 `FAILED` 수렴 | `JOB_SUBMIT_FAILED` | Case/Run/preparation 실패 전이. API/DB에는 고정 public message만 사용하고 root cause는 server log에만 기록 |
| 동일 proposal로 실행 재요청 | 200/202 기존 실행 접수 응답 | 기존 정책 | RUNNING이 없으면 새 `case_id` 실행 접수 |
| 같은 Scenario에 RUNNING 실행 존재 | 409 | `RSC_ALREADY_EXISTS` | 기존 `ScenarioCase.actv_scnr_id` guard |
| A4a `EXECUTED_CONFIRMED` Case의 input file role 일부/전체 누락 | 200 | `SUC-000` | `executionInputFiles.complete=false`, `missingFileRoles`, partial/empty `files` 반환 후 데이터 불완전 로그 |
| A4a `parameterSource`가 non-`NONE`인데 payload가 null/blank/non-array/empty/invalid JSON | 409 | `BIZ_RULE_VIOLATION` | 정상 empty tree로 숨기지 않고 고정 public message로 실패 |
| A4a grouped projection에 필요한 slot-axis/rationale field 누락 또는 타입 불일치 | 409 | `BIZ_RULE_VIOLATION` | 해당 value row를 제외하거나 빈 grouped array로 숨기지 않고 기존 current-state 데이터 불일치 정책으로 실패 |
| A4a parameter composite identity 중복 | 409 | `BIZ_RULE_VIOLATION` | 임의 중복 제거 금지 |
| A4a rendering master 누락 또는 중복 | 409 | `BIZ_RULE_VIOLATION` | parameter leaf 누락 또는 임의 선택 금지 |
| A4a payload/master file·sheet·table 좌표 불일치 | 409 | `BIZ_RULE_VIOLATION` | case-sensitive exact match 실패, raw 좌표 비노출 |
| current-state 조회 시 연결 `case_id`는 있으나 Case/Run 정합이 깨짐 | 409 | `BIZ_RULE_VIOLATION` | 데이터 불일치 상태로 로깅하고 FE에는 복구 불가 상태 반환 |
| A8/A9 Agent parameter JSON이 null/blank/non-array/malformed | 5xx | `API_RESPONSE_ERROR` | `Agent proposal parameter JSON is invalid.` 유지, 빈 배열 정상화 금지 |
| A8/A9 tree projection의 master/좌표/grouped-rationale/rendering 정합성 실패 | 5xx | `API_RESPONSE_ERROR` | A4a의 409를 노출하지 않고 기존 A8/A9 public error로 매핑 |

## 9. Swagger Annotation Recommendation

- proposal request summary: `MI 수급모델 파라미터 제안 생성 접수 API`
- proposal current-state query summary: `MI 수급모델 파라미터 현재 상태 조회 API`
- proposal query summary: `MI 수급모델 파라미터 제안 상태/결과 조회 API`
- callback summary: `[Agent 콜백] MI 수급모델 파라미터 제안 완료 API`
- JSON execution summary: `확정 파라미터 기반 MI 수급모델 실행 접수 API`
- model execution options description: 기반 Event에 따른 실행 입력 mode를 조회한다고 명시한다.
- JSON/multipart execution description: 접수 transaction에서 해당 시나리오의 active 가격전망해석이 있으면 자동 soft delete한 뒤 Case/Run을 생성한다고 명시한다.
- callback controller는 Agent-facing tag로 분리한다.
- callback operation은 Bearer security scheme(`Role.SYSTEM` 등 `@RequiredPermissionRules`)을 명시한다. 기존 시나리오 설계 콜백과 동일한 self-issued JWT를 재사용한다.
- flat parameter array, dynamic slot-axis key, `valueFieldNames.size=1`, `values[].proposedValue` 단일 mutable field 규칙을 description과 example에 명확히 기록한다.
- A4a/A4/A8/A9 response `parameters` 설명에는 Agent raw array가 아니라 5.2.2의 response-only `groupedRationales` projection이 추가된 shape임을 명시한다.
- A4a `CurrentModelParamLeafRes.parameter` 설명에는 `groupedRationales`가 화면 표시용이고 A6 전송 전에 제거해야 함을 명시한다. A6 request DTO와 Agent client DTO에는 이 field를 추가하지 않는다.

## 10. Data/Model Impact

- mock slice:
  - schema change 없음
  - JSON seed + in-memory/Redis state 가능
  - Scenario별 `eventTypeL2Id` mock seed로 `inputMode`를 계산
- real slice 권고:
  - Agent proposal 테이블 read-only 직접 조회로 결과 서빙 (기존 Backend DB/schema에 Agent 테이블 추가, 별도 view/synonym 불필요, runtime table visibility/권한/type 확인 필요)
  - thin Backend execution-preparation persistence(`tmd_mi_model_execpreph`: 제안 기반 실행 준비 상태, Agent proposal ID, 실행 Case 연결; proposal 데이터·hash 없음)
  - 실제 수급모델 실행 master/history는 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 계속 source of truth다.
  - `tmd_mi_model_execpreph` row 1건은 input Excel 3개 세트 생성과 수급모델 실행 접수 1회를 나타낸다. 같은 `scnr_id + agnt_prpsl_id`로 여러 번 실행할 수 있으므로 매 실행마다 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만든다.
  - `tmd_mi_model_execpreph.case_id`는 nullable + UNIQUE 포인터다. 실행 접수 전에는 null이고, 실행 접수 후에는 해당 row가 만든 단일 Case를 가리킨다. 한 Case가 여러 execution-preparation row에 붙으면 안 된다.
  - `agnt_prpsl_id`는 unique가 아니다. 반복 실행 이력과 callback 후보 검색을 위해 `(agnt_prpsl_id, exec_prep_stat_cd, exec_prep_id DESC)` 일반 index를 둔다.
  - A8/A9의 최근 실행 선택은 5.5.1 `LatestModelParamProposalExecutionContext`를 단일 계약으로 사용한다. physical join/index/EXPLAIN은 canonical execprep DDL draft를 따른다.
  - A4a `EXECUTED_CONFIRMED` input Excel 파일은 A4a가 선택한 execution-preparation의 `caseId`로 기존 `tcm_filel(SCNRCASE + case_id)` 저장 규칙에서 조회한다. 파일 binary download는 공통 File Management API를 재사용한다.
- Agent-owned schema:
  - parameter master와 dimension mapping은 Backend에서 복제하지 않는다.
  - proposal table은 Agent lifecycle의 source이며 Backend Case/Run orchestration source가 아니다.
  - `proposal_parameters`, `confirmation_parameters`는 JSON root array다. 기존 `JSON_TYPE = 'ARRAY'` CHECK를 유지한다.
- response-only `groupedRationales` projection:
  - schema, DDL, entity, repository, query, Agent JSON 저장 변경 없음
  - 조회 시 response copy에만 계산하며 `proposal_parameters`/`confirmation_parameters`를 다시 저장하지 않음
- 가격전망해석 재실행 연계:
  - 신규 table/column 없음
  - A6/A7 최종 실행 등록 transaction은 해당 시나리오 active 가격전망해석 row soft delete와 Case/Run 생성을 원자적으로 수행한다.
  - 재실행 무효화는 `del_yn='Y'` soft delete를 사용한다. active row만 제한하고 deleted row 다건을 허용하는 functional unique의 DB 적용이 완료됐으며, 정확한 물리 제약과 rollout은 `docs/design-docs/gas/price-forecast-interpretation-erd-ddl-draft.md`를 따른다.
- 정확한 DDL은 `erd-change-review` 이후 별도 canonical draft에서 관리한다.

## 11. Recommended Data Access Approach

- proposal state/snapshot: JPA
- current-state 조회: 최신 execution-preparation + Agent proposal read + Case/Run 상태 조합. JPA repository projection으로 시작하고 조인이 복잡해지면 QueryDSL로 승격
- callback 처리: Agent 테이블 재조회 → Backend orchestration 행 monotonic update + transaction (CAS 없음)
- callback SSE publish: transaction after-commit event
- A8/A9 latest anchor: JPA repository projection 또는 공통 resolver/helper 한 곳에서 `LatestModelParamProposalExecutionContext`를 구성한다. 두 endpoint가 Case/execprep 선택 predicate와 404를 각각 구현하지 않는다.
- A4a input files: current-state 조회에서 이미 선택한 execution-preparation의 `caseId`를 사용하고 latest Case resolver를 별도로 호출하지 않는다.
- grouped rationale projection: data access 변경 없이 service 계층의 공통 response projector를 사용한다. A4a/A4/A8/A9가 같은 grouping 구현을 공유하고 endpoint별 parser/error policy는 기존 service에 남긴다.
- 가격전망해석 삭제: A6/A7 공통 final transaction에서 path `scenarioId` 기준 active JPA row를 write lock으로 조회하고 entity `softDelete()`를 호출한다. row가 없으면 no-op이다.
- latest/active 조회가 복잡해지면 repository projection 또는 QueryDSL
- Excel merge: Apache POI service
- file upload/dispatch: 기존 `ScenarioModelExecutionServiceImpl` 내부 책임을 reusable collaborator로 추출

## 12. Implementation Touch Points

- controller
  - proposal frontend controller
  - proposal current-state GET mapping 추가
  - Agent callback controller
- security
  - callback endpoint는 기존 시나리오 설계 콜백과 동일하게 표준 Resource Server JWT chain(`CustomJwtDecoder`, HS256 self-issued) + `@RequiredPermissionRules(Role.SYSTEM ...)`을 재사용한다.
  - Agent는 Backend 토큰 발급 API와 발급 인풋 2종으로 JWT를 자체 발급·갱신한다. dedicated static token/HMAC/외부 OAuth2 도입은 없다.
  - `ResourceServerConfig.tokenByPassSecurityFilterChain` matcher에 callback path를 추가하지 않는다.
- service
  - `ModelParamProposalService`
  - `PriceInterpretationStore` active row 실행 직전 조회/자동 soft delete
  - current-state 조회 service method
  - `ModelParamProposalCallbackService`
  - `ModelInputWorkbookProvider`
  - `ModelParamWorkbookMergeService`
  - A8/A9 공통 `LatestModelParamProposalExecutionContext` resolver/helper
  - A4a/A4/A8/A9 공통 response-only `groupedRationales` projector (`service/nextgen/paramproposal`)
  - 기존 execution pipeline reusable method 추출
- client
  - proposal request/confirmation method를 Agent HTTP interface에 추가
- DTO/enums
  - proposal/confirmation/callback/SSE/status
  - `CurrentModelParamProposalRes`, `CurrentModelParameterRes`, `CurrentModelParamProposalState`, `ModelParamParameterSource`
  - A4/A8/A9 response `JsonNode` Swagger description은 response projection을 명시하되 `ModelParamPayloadItem` request DTO는 변경하지 않음
- persistence
  - proposal entity/repository 또는 mock store
  - `MiScenarioPriceInterpRepository` active scenario row 조회 + `MiScenarioPriceInterp#softDelete()`
  - latest Scenario Case + linked execution-preparation 공통 projection/query
- resources
  - mock proposal JSON seed
- configuration
  - Agent URL/timeout
  - Backend가 결정하고 Agent 배포 설정에 등록할 callback base URL/path
  - callback credential 발급·저장·rotation 책임은 인증 방식 합의 후 확정
  - base workbook location/version
  - Event Type L2 제외 목록 policy
  - 단계별 mock switch

## 13. Open Questions

> Resolved: (a) callback 인증 방식 — 기존 콜백과 동일 self-issued JWT 재사용, Agent가 Backend 토큰 발급 API + 인풋 2종으로 자체 발급. (b) 공유 DB read — Agent proposal 테이블 read 가능(권한 확정). (c) write-then-callback 순서 — Agent가 proposal 테이블 commit 이후 콜백을 발행함을 Agent 답변으로 확정(2026-07-07). correlation/event ID·payload-hash 불필요 확정. (d) callback `{status, message, data}` wrapper 필드 셰이프 최종 확인 — Agent 규약 1.2.2와 일치, 실패 시 `data`(null/빈 object) 생략 허용, 식별자 4종+`data.status`는 `data` 내부(2026-07-07). (e) confirmation 재시도 정책 — 재시도 단위는 실행 POST 전체, Backend가 confirmation 호출 직전 proposal 상태 재조회로 이중 confirm을 막으므로 Agent confirmation 멱등성은 계약 전제로 요구하지 않음(2026-07-07, 상세 5.5). 위 다섯 항목은 P0에서 제외한다.

### P0

1. proposal table read 계약은 확정(2026-07-07 DB DDL 수령, 컬럼 매핑 5.2 반영). Agent proposal table은 기존 Backend DB/schema에 추가되는 직접 read 대상이며 별도 view/synonym 경로는 사용하지 않는다. **잔여**: (a) runtime table visibility/SELECT 권한/key type 정합 확인, (b) dimension_map ERD blocker #2(nullable UNIQUE)/#3(조건부 CHECK) 정정 — Backend 런타임 미의존, `erd-change-review`로 분리 처리.
2. Agent callback/confirmation payload에 master의 `model_table`을 `tableName`으로 반영하는 배포 시점(required shape는 확정).
3. 원본 Excel 3개 위치와 version 선택 규칙.
4. (resolved) Backend execution-preparation 저장안은 채택됐고 `tmd_mi_model_execpreph` DBA DDL은 적용 완료됐다. application 구현은 착수 가능하며, real runtime 배포 전에는 entity mapping 기준 `ddl-auto=validate` 및 핵심 index/constraint 확인만 수행한다.
5. (null 정책 resolved) `basecaseValue`는 required key + nullable numeric, `proposedValue`는 required numeric으로 확정. 숫자 precision/최대·최소 범위는 잔여.
6. parameter/value row 최대 건수와 JSON column size 한도(컬럼 타입은 native `JSON`+`JSON_TYPE=ARRAY` CHECK로 확정).
7. callback 신호 유실 시 폴백 폴링 interval/timeout.
8. `schemaVersion`, `completedAt`, `errorCode` 확장 필요 여부 — `completedAt`(`proposal_completed_dtm`)·`error_code`는 Agent 테이블에 **이미 컬럼 존재**, `schemaVersion`만 미존재. `errorMessage`는 wrapper `message`와 중복됨.
9. duplicate-slot Agent payload의 A6/Excel 실행 좌표 의미. A4a/A4/A8/A9 read response는 duplicate value row를 보존하고 rationale만 별도 그룹핑한다. A6 validator는 현재 duplicate slot identity를 거절하며, 서로 다른 `basecaseValue`/`proposedValue`가 같은 Excel 좌표를 가리키는 경우 first/last 선택·row 병합·writer overwrite를 허용하지 않는다. 대표/실제 payload 감사 후 실행 계약을 별도 확정한다.

### P1

- 없음. 구현 전 확정이 필요한 P1 항목은 모두 resolved 또는 P0/P1 외 runtime verification으로 이동했다.

### Resolved Decisions

1. Option A 재확정과 A8 조회 의미: Agent confirmation API는 이미 `CONFIRMED`인 proposal의 재확정을 허용한다. 같은 Agent proposal로 값 변경 재실행을 지원하고 Agent `confirmation_parameters` 현재값을 갱신한다. A8은 최신 A6-linked Case가 가리키는 proposal의 조회 시점 현재 `confirmation_parameters`를 반환하며, 해당 Case의 실행 시점 immutable snapshot을 보장하지 않는다. 실행별 exact 복원은 현행 요구사항이 아니므로 `tmd_mi_model_execpreph`에 confirmed JSON snapshot을 추가하지 않는다.
2. SSE scope: endpoint는 scenario scope 신규 채널로 둔다. `GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/stream`을 사용하며 기존 scenario-group SSE channel에 event type을 추가하지 않는다.
3. Model execution staging: 기존 `tcm_filel TEMP/0` marker, 최종 `SCNRCASE/{uuid}` object key, 최종 lock conflict 409 후 staged file 삭제, crash orphan TTL 24시간, JSON/multipart 공통 적용, 업로드 3개 직렬 처리를 사용한다. API field와 DDL은 변경하지 않는다.
3. Agent proposal read path: Agent proposal table은 기존 Backend DB/schema에 추가된 테이블을 직접 read한다. 별도 view/synonym 또는 우회 경로는 만들지 않는다.
4. Proposal generation retry: proposal 생성이 `REQUEST_FAILED` 또는 Agent `FAILED`로 terminal 수렴한 뒤에는 새 proposal 생성 요청을 허용한다. 실패 row는 재사용하지 않고 audit/history로 보존하며, 새 요청은 새 execution-preparation row와 새 Agent proposal request를 만든다.

## 14. Change Log / Sync Note

- 2026-07-14: **A8/A9 응답 tree 계약 및 projector 공통화.** flat `parameters`를 제거하고 endpoint별 `parameterSource`와 A4a 동일 `modelFiles > modelSheets > modelTables > parameters > {parameter, rendering}` tree를 반환한다. A4a/A8/A9는 raw Agent JSON 파싱, parameter-name batch master 조회, 좌표 검증, grouped rationale/rendering, tree 조립을 공통 projector로 수행한다. latest Case/current-value 의미와 endpoint별 409/API_RESPONSE_ERROR 경계는 유지하며 schema/DDL은 변경하지 않는다.
- 2026-07-14: **수급모델 재실행 시 가격전망해석 자동 soft delete 계약 확정.** A2 options 응답과 A6/A7 request는 변경하지 않는다. 두 실행 경로가 합류하는 공통 final transaction에서 대상 시나리오의 active 해석 row가 있으면 soft delete하고, Case/Run 생성을 원자 commit한다. 수급모델 재실행은 가격전망해석 Agent 생성을 자동 호출하지 않는다.
- 2026-07-14: **가격전망해석 functional unique 적용 완료.** `scenario_id`와 active일 때만 `'N'`을 반환하는 functional key를 조합해 active row 최대 1건과 deleted row 다건 보존을 함께 강제한다. schema blocker를 해제하며 정확한 DDL/DBA rollout은 `price-forecast-interpretation-erd-ddl-draft.md`가 소유한다.

- 2026-07-13: **A6 JSON execution path 분리.** 동일한 `POST /model-executions`에 JSON/multipart 계약을 함께 노출할 때 SpringDoc/OpenAPI operation이 혼합되는 문제를 제거하기 위해 A6 JSON path를 `POST /scenarios/{scenarioId}/model-executions/from-parameters`로 이동했다. A7 multipart path는 기존 `/model-executions`를 유지한다. 기존 JSON path 사용자가 없음을 확인해 deprecated alias 없이 즉시 이동하며 request/response/permission/service/schema는 변경하지 않는다.
- 2026-07-13: **`groupedRationales` grouping key를 rendering `SLOT` 축으로 한정.** 동일한 고정 설비 행의 시간별 rationale을 결합하도록 parameter master `basecase_column_roles`와 일치하는 slot axis만 key/output에 포함하고 `MATRIX_TIME_COLUMNS`의 `TIME_AXIS`(`Year`/`Month`/`Period` 등)는 제외한다. A4a/A4/A8/A9에 공통 적용하며 Agent 저장 JSON, A6/X2/B1 contract, schema는 변경하지 않는다.
- 2026-07-13: **A4a/A4/A8/A9 response-only `groupedRationales` contract 추가.** 최초 계약은 parameter별 `slotAxesFieldNames` 전체 값으로 그룹핑하는 규칙이었으나 같은 날 SLOT-only grouping 결정으로 대체되었다. 최초 그룹/axis 순서와 원본 `values[]` 보존, rationale `\n` 결합, trim/dedup 금지, 숫자 canonicalization 규칙은 유지한다.
- 2026-07-13: **A10을 A4a 성공 실행 파일 projection으로 병합.** 독립 `GET .../model-executions/latest/input-files` endpoint는 만들지 않는다. A4a는 자신이 선택한 최신 execution-preparation이 `EXECUTED_CONFIRMED`일 때만 같은 row의 `caseId`로 `executionInputFiles`를 반환하고, 그 외 상태에서는 `null`을 반환한다. 기존 `complete`/`missingFileRoles`/partial files 및 공통 File Management 다운로드 계약은 유지한다. A8/A9의 latest Case anchor는 변경하지 않는다.
- 2026-07-13: A4a current-state 응답을 top-level `parameters`/`parameterShapes`와 navigation-only `modelFiles`에서 `modelFiles > modelSheets > modelTables > parameters > {parameter, rendering}` tree로 in-place 교체. A4a 전용 MI response DTO 경계, 순서/composite identity, rendering master 일괄 조회 및 409 정합성 규칙, A6 flatten mapping을 고정했다. 특정 proposal 상세 GET, Agent/A6 wire contract, schema/DDL은 변경하지 않는다.
- 2026-07-03: 사용자 설명과 Agent Notion 문서를 기준으로 최초 작성.
- 2026-07-03: 대상 Event 기반 Scenario는 Backend 원본 파일 생성/merge, 비대상 Event 기반 Scenario는 기존 사용자 파일 3개 업로드 방식으로 분기하도록 정정.
- 2026-07-03: 기존 multipart 실행 계약은 active contract로 유지하고 `inputMode` 판정 API를 추가.
- 2026-07-03: Agent confirmation 성공 후 Excel 생성 및 기존 execution pipeline 재사용 순서 반영.
- 2026-07-03: proposal 요청은 한 실행 준비 시도당 1회로 반영.
- 2026-07-06: Agent DB 설계의 parameter master, dimension mapping, proposal table 책임을 반영하고 API/DB field mapping을 명시.
- 2026-07-06: Agent proposal lifecycle과 Backend execution-preparation lifecycle을 분리하고 real integration DDL blocker를 갱신.
- 2026-07-06: `eventTypeL2Id IN (12,13,14,15,16,20)`은 `FILE_UPLOAD`, 나머지는 `PARAMETER_PROPOSAL`로 eligibility 계약 확정.
- 2026-07-07: eligibility 제외 목록에 `eventTypeL2Id=3`을 추가해 `FILE_UPLOAD` 대상을 `IN (3,12,13,14,15,16,20)`으로 갱신. 동일 집합을 그룹 그래프 API(`GET /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}/graph`)의 `parameterProposalTarget`(대상=true) 필드도 공유한다.
- 2026-07-06: Agent-generated proposal ID의 생성 순서를 고려해 callback을 고정 collection path로 변경하고 body correlation으로 확정.
- 2026-07-06: Backend-generated `callbackCorrelationId`를 dispatch 전에 저장하고 Agent가 echo하도록 해 응답/callback race 제거.
- 2026-07-06: callback JWT `Role.SYSTEM`, DTO validation, CAS/idempotency, late callback, ack disposition, Agent retry 계약 확정.
- 2026-07-06: 2026-07-03 Agent Notion 계약을 재검토해 위 세 항목을 confirmed contract에서 철회하고 `Proposed Contract`/P0 Open Questions로 이동.
- 2026-07-06: callback wrapper 수신, body 없는 proposal request, Backend 결정 URL의 Agent 배포 설정 등록, confirmation `data` wrapper, modelFile casing normalization을 최소 호환 계약으로 복원.
- 2026-07-06: callback URL 소유권을 `Backend 결정·전달, Agent 배포 설정 등록·관리`로 명확히 하고 proposal request body 없음 계약을 유지.
- 2026-07-06: callback path를 `/scenarios/{scenarioId}/model-param-proposals/callback`로 정정(위 고정 collection path 항목 supersede). 대상 리소스 ID(`scenarioId`)를 경로에, `modelParamProposalId`는 body correlation으로 두어 기존 시나리오 설계 콜백 패턴과 정합화. `scenarioId`는 proposal 요청 시 이미 Agent에 전달되므로 proposal ID 생성 순서와 무관하다.
- 2026-07-06: **결과 조회를 Backend가 Agent proposal 테이블 read-only 직접 조회로 전환**(시나리오 설계와 동일). callback을 완료 신호로 격하하고 CAS/payload-hash/terminal overwrite/snapshot 저장을 제거. Backend는 thin orchestration 행만 write(monotonic). CAS/idempotency/hash를 fixed contract로 둔 이전 항목들을 supersede. 당시 선행조건으로 둔 Agent proposal DDL 확정 + 동일 schema/Backend read 권한 승인 + write-then-callback 순서는 2026-07-07 확정 항목으로 supersede됨.
- 2026-07-06: **callback correlation을 path `scenarioId` 단독으로 확정.** 파라미터 제안은 시나리오당 1회뿐(사용자별 반복 아님)이라 `scenarioId`가 proposal을 유일 특정. body `modelParamProposalId`는 교차검증용으로 격하하고, 실패(`error`) 콜백의 `data`가 규약 1.2.2대로 null이어도 무방하도록 DTO를 nullable로 완화. Design relationship을 `Scenario 1:1 ModelParamProposal`로 정정. 이전 "body correlation 필수" 판단 supersede. 이 결정은 2026-07-08 correlation scope-key 결정으로 다시 supersede됨.
- 2026-07-06: **callback wrapper 유지 근거를 Agent 담당자 API 규약(1.2.1 camelCase, 1.2.2 `{status,message,data}` wrapper)으로 확정.** 담당 개발자별 컨벤션 차이(기존 Backend 콜백은 flat)를 명시하고, 이 파라미터 제안 콜백만 Agent 규약에 맞춤. (당시 추가한 "실패 콜백에도 `data` 식별자 유지 필수" 항목은 correlation을 path `scenarioId` 단독으로 확정한 아래 항목으로 supersede됨 — 시나리오당 1회라 불필요.)
- 2026-07-06: **callback 인증·공유 DB read를 확정으로 반영**하고 P0에서 제외. 인증은 기존 콜백과 동일 self-issued JWT 재사용(Agent가 Backend 토큰 발급 API + 인풋 2종으로 자체 발급), 공유 read는 조회 가능. 5.4.3을 Resolved/Open으로 재구성하고 correlation/event ID·payload-hash를 moot으로 확정. 8절 error table의 "상이 payload → 409" 잔재(신호 격하와 모순)를 제거. Agent 전달용 콜백 계약서(`docs/design-docs/gas/mi-model-param-proposal-callback-contract.md`) 추가.
- 2026-07-07: Agent confirmed payload를 기준으로 `proposalParameters`/`confirmationParameters`를 동일한 flat parameter array로 최종 확정. `tableName` required, `valueFieldNames.size=1`, dynamic slot-axis key, `basecaseValue` read-only, `proposedValue` mutable 계약을 채택하며 같은 날 검토한 nested-tree object 안을 supersede.
- 2026-07-07: FE 표에서 `basecaseValue` 컬럼을 제거하고 `% {formatted valueFieldName}` 한 개에 `proposedValue`만 표시하도록 확정. 미수정 시 Agent 생성값, 수정 시 사용자 변경값을 전체 confirmation array의 `proposedValue`로 Agent confirmation API에 전달하도록 명시.
- 2026-07-07: **P0 open question 1(write-then-callback 순서)·2(callback wrapper 셰이프)를 Agent 답변으로 확정하고 Resolved로 이동.** 순서는 Agent가 proposal 테이블 commit 이후 콜백 발행함을 명시 확정, wrapper는 `{status, message, data}`로 Agent 규약 1.2.2와 일치(실패 시 `data` null/빈 object 생략 허용, 식별자 4종+`data.status`는 `data` 내부). 5.4.3 Resolved 갱신, 남은 P0에서 두 항목 제외·재번호(잔여 1~9). implementation readiness/exit criteria의 write-then-callback 선행조건 제거.
- 2026-07-07: **P0 confirmation 재시도 정책 확정·Resolved 이동.** 재시도 단위는 실행 POST 전체 재요청. Backend가 confirmation 호출 직전 proposal 상태를 재조회해 `PROPOSED`일 때만 호출하고 `CONFIRMED`면 생략하는 monotonic read-back으로 이중 confirm을 막으므로 **Agent confirmation 멱등성을 계약 전제로 요구하지 않는다**(있으면 방어적 보너스). 후속 단계 실패는 CONFIRMED 롤백 없이 재요청으로 merge·dispatch 재수행, 재요청 `confirmationParameters`는 저장값과 동일 필수(다르면 409). 당시 Case/Run 중복 가드는 live 실행 최대 1개로 정리했으나, 2026-07-08 multi-execution decision으로 “Scenario 기준 RUNNING 최대 1개, 실행 1회마다 새 case_id”로 정정됨. 5.5 처리 순서 5번을 read-back guard로 정정. 남은 P0 재번호(잔여 1~8).
- 2026-07-07: **Agent proposal table DB DDL 수령·proposal read 계약 확정**(`docs/design-docs/gas/mi-agent-param-proposal.md` 3절, ERD blocker #1 해소). 컬럼 매핑을 5.2에 확정 — `completedAt`←`proposal_completed_dtm`, `failureReason`←`error_message`(+`error_code`), `parameters`←`proposal_parameters`/`confirmation_parameters`(native `JSON`+`JSON_TYPE=ARRAY`), `status` CHECK로 4종 고정. `completedAt`/`error_code`가 Agent 스키마에 이미 존재함을 P0 8에 반영. Agent proposal table은 기존 Backend DB/schema에 추가되는 직접 read 대상이며 별도 view/synonym 경로는 사용하지 않는다. **잔여**: `scenario_id` 유일성 부재(DB 미강제 → app 가드+`modelParamProposalId` 교차검증으로 correlation, race 방지 partial-unique/lock 협의)·runtime table visibility/권한/type 확인·dimension_map blocker #2/#3(erd-change-review). P0 1을 이 잔여로 좁힘.
- 2026-07-07: **Backend execution-preparation 선행 생성 순서 확정.** Agent 호출 전에 `scenarioId` 기준 `REQUESTING` 행을 `modelParamProposalId=null`로 생성·commit하고 외부 호출한다. Agent 응답의 ID 또는 callback이 Agent 테이블 재조회로 확인한 ID 중 먼저 도착한 값을 write-once로 채우며, 늦은 응답은 callback 상태를 `PROCESSING`으로 되돌리지 않는다. timeout 시 late callback을 위해 preparation을 즉시 삭제하지 않는다.
- 2026-07-08: **callback correlation을 scope-key 방식으로 재정리.** path `scenarioId`는 proposal identity가 아니라 현재 in-flight execution-preparation을 찾는 scope key다. `data.modelParamProposalId`가 있으면 in-flight 후보 검색 조건으로 사용하고, 없으면 path `scenarioId`의 현재 in-flight row에만 수렴한다. `modelParamProposalId`는 반복 실행 때문에 `tmd_mi_model_execpreph`에서 unique가 아니며 terminal/history row에 붙이지 않는다. in-flight row가 없으면 late/no-op이며 terminal/history row에 붙이지 않는다. 모호한 Agent 호출 실패는 `REQUEST_FAILED`로 닫지 않고 `REQUESTING` in-flight 상태를 유지해 callback/read-reconcile/timeout 복구로 수렴한다.
- 2026-07-08: Backend proposal request orchestration ERD/DDL blocker를 재분류. `tmd_mi_model_execpreph` 물리명 유지, table comment `TMD_파라미터제안생성요청이력` 채택, Agent read permission/DDL/callback auth/write-then-callback은 resolved로 유지. `tmd_mi_model_execpreph`는 실행 이력이 아니며 실제 수급모델 실행 master/history는 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 계속 소유한다. 구현 착수 가능하되 real runtime 배포는 `tmd_mi_model_execpreph` mapping/index/constraint validate와 Agent proposal table runtime visibility/권한/type 확인이 필요하다.
- 2026-07-08: **proposal request 1건에서 여러 수급모델 실행을 허용하도록 Case linkage 전제를 정정.** 기존 `tmd_mi_model_execpreph 1:N tmd_mdl_scnrcasem` linkage 및 `tmd_mdl_scnrcasem.exec_prep_id` 추가안은 supersede. 실행 1회마다 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만들고, `tmd_mi_model_execpreph.case_id` nullable + UNIQUE 포인터로 해당 실행 Case를 연결한다. 같은 `scnr_id + agnt_prpsl_id` 반복 실행을 허용하므로 `agnt_prpsl_id` unique는 금지하고 일반 index로 둔다. 최근 실행 기준 확정/제안 파라미터 조회와 input Excel 파일 조회는 최신 Case → `tmd_mi_model_execpreph.case_id` 경로를 사용한다.
- 2026-07-09: **Scenario 기준 current-state 조회 API 추가.** `GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals`가 팝업 진입 시 `scenarioId`만으로 생성중/제안완료/확정 후 실행 전·실행중/최신 실행 확정 파라미터 표시 상태를 결정한다. 최신 execution-preparation row가 과거 실행보다 우선하며, 반환 파라미터 source는 `parameterSource`로 명시한다.
- 2026-07-09: **SSE scope와 Agent proposal read path 확정.** proposal SSE는 scenario-scope 신규 endpoint(`/scenarios/{scenarioId}/model-param-proposals/stream`)로 유지하고 기존 scenario-group SSE channel에 합치지 않는다. Agent proposal table은 기존 Backend DB/schema에 추가된 테이블을 직접 read하며 별도 view/synonym 경로는 필요 없다.
- 2026-07-09: **`tmd_mi_model_execpreph` DBA DDL 적용 완료 확인.** execution-preparation DDL 적용은 더 이상 implementation blocker가 아니며, 남은 항목은 entity mapping validate와 `agnt_prpsl_id` non-unique index / `case_id` nullable unique / `actv_scnr_id` generated rule 확인이다.
- 2026-07-09: **Proposal 생성 실패 후 재제안 허용 정책 확정.** `REQUEST_FAILED` 또는 Agent `FAILED` terminal 후 FE는 다시 `POST /model-param-proposals`를 호출할 수 있다. Backend는 실패 row를 재사용하지 않고 새 `tmd_mi_model_execpreph` row와 새 Agent proposal request를 만든다.
- 2026-07-10: **A6 JSON execution 구현 입력 계약 동기화.** 실제 Agent confirmation sample에서 rationale 보정이 확인되어 parameter/value rationale은 required string으로 유지하되 구조·동일값 재시도 비교에서 제외했다. `PROPOSED` 최초 확정과 `CONFIRMED` 동일값 생략/Option A 재확정을 모두 허용하고, Case/file/Run/`execprep.case_id` commit 이후 dispatch 실패는 연결 `modelExecutionStatus=FAILED`로 구분했다.
- 2026-07-10: **`basecaseValue=null` 계약 확정.** 실제 Agent payload의 nullable basecase를 정상값으로 수용한다. `basecaseValue` key는 required이고 값은 null 또는 numeric이며, proposal/confirmation 비교에서 `null ↔ null`만 동일 구조로 인정하고 `null ↔ numeric` 및 서로 다른 numeric 값은 immutable basecase 변경으로 거절한다. `proposedValue`는 required numeric을 유지한다.
- 2026-07-10: **dispatch 실패 공개 오류 정책 확정.** Kubernetes Job 생성 실패의 실제 exception/root cause는 server log에만 기록한다. Run에는 `JOB_SUBMIT_FAILED`와 `Model execution dispatch failed.` 고정 public message만 저장하고, current-state `failureReason`은 raw Run error message보다 stable failure reason code를 우선한다. API field와 DDL은 변경하지 않는다.
- 2026-07-10: **모델 실행 staging/transaction 경계 확정.** JSON·multipart 모두 파일 3개를 최종 `SCNRCASE/{uuid}` key에 직렬 선업로드하고 `tcm_filel TEMP/0`로 stage한다. 최종 짧은 transaction은 Case/Run/lastRun/preparation과 metadata-only file attachment만 commit하며, conflict는 409 후 보상 삭제하고 crash orphan은 24시간 TTL로 정리한다. API field와 DDL은 변경하지 않는다.
- 2026-07-10: **A10 불완전 input file response 계약 확정.** `LatestModelExecutionInputFilesRes`에 required `complete`와 non-null `missingFileRoles`를 추가했다. 최신 Case가 존재하면 파일 일부/전체 누락도 HTTP 200으로 반환하며, `complete=false`, 고정 role 순서의 누락 목록, partial/empty `files`로 FE가 다운로드 불가 상태를 판별한다. schema 변경은 없다.
- 2026-07-10: **A8 current confirmation 계약 확정.** 최신 A6-linked Case가 연결한 proposal의 조회 시점 현재 Agent `confirmation_parameters`를 반환한다. 해당 Case 실행 시점의 immutable parameter snapshot은 보장하지 않으며 실행별 exact 복원은 현행 범위가 아니므로 execprep confirmed JSON snapshot/ERD 변경은 추가하지 않는다. 실제 실행 input의 source of truth는 A10의 `SCNRCASE + caseId` 파일 3개다.
- 2026-07-10: **A10 공통 파일 다운로드 계약 동기화.** 기존 File Management 다운로드는 `POST /api/v1/file-management/download/file`이며 request body에 A10이 제공하는 `fileNo`, `fileNm`, `fileSize`를 모두 전달한다. 이전 `GET + fileNo query parameter` 표현은 폐기한다.
- 2026-07-10: **A8/A9/A10 공통 Latest Model Parameter Proposal Execution Anchor 확정.** 세 endpoint의 최신 Case 선택, execprep 연결, soft-delete 조건, `case_id DESC`, 공통 404를 5.5.1 `LatestModelParamProposalExecutionContext`에 단일 정의했다. A8/A9는 context의 proposal ID, A10은 case ID만 사용한다. 신규 API/schema 변경은 없다.

## 15. Exit Criteria / Handoff

- 가격전망해석 자동 soft-delete mutation: `ready`. API shape 변경 없이 공통 final transaction에서 active row 삭제와 Case/Run 생성을 함께 처리할 수 있고, 반복 soft delete와 active row 1건을 함께 만족할 canonical functional unique의 DB 적용이 완료됐다.
- A4a/A4/A8/A9 `groupedRationales` read contract: `ready`. A4a/A8/A9는 공통 tree projector를, A4는 공통 grouped-rationale primitive를 사용한다. schema/ERD/repository query 계약 변경이 없어 이번 slice의 `new-domain-model-design`/`erd-change-review`는 생략한다. A6 duplicate-slot 실행 의미는 projection 구현과 분리된 open question이다.
- frontend mock 구현: ready
- callback 계약(엔드포인트/인증/wrapper/순서/응답): Agent 전달 ready. 전달용 계약서는 `docs/design-docs/gas/mi-model-param-proposal-callback-contract.md`
- real Agent/DB/Excel 구현: proposal read 계약 확정(DB DDL 수령)으로 read 엔티티/조회 착수 가능. `tmd_mi_model_execpreph`와 Agent proposal table이 DB 반영됐으므로 Agent create/callback/read, 최신 실행 기준 파라미터 조회, A4a 성공 실행 파일 projection은 mock 없이 실DB 연동 slice로 구현 가능하다. Excel merge 서비스 자체는 별도 개발 slice로 둔다.
- Backend proposal request/execution-preparation 구현: API/service/repository 구현은 착수 가능. `tmd_mi_model_execpreph` DBA DDL은 적용 완료됐으며, real runtime 배포 전에는 entity mapping validate와 index/constraint(`agnt_prpsl_id` non-unique, `case_id` nullable unique, `actv_scnr_id` generated) 확인을 수행한다. 실행 이력 source of truth는 기존 Case/Run이며, `tmd_mi_model_execpreph.case_id`로 제안 기반 실행 준비 row와 단일 실행 Case를 연결한다.
- `erd-change-review` 대상:
  - Agent proposal 테이블 read-only 조회 + thin Backend execution-preparation(orchestration) 책임 분리
  - Agent proposal table runtime visibility / SELECT 권한 / key type 정합
  - `tmd_mi_model_execpreph.case_id` nullable unique linkage와 `agnt_prpsl_id` non-unique index runtime validate
  - dimension_map ERD blocker #2(nullable UNIQUE)/#3(조건부 CHECK) 정정
  - A8 current Agent `confirmation_parameters` 조회 계약 검증(실행별 confirmed snapshot은 현행 범위 아님)
  - Agent JSON root array CHECK와 payload 최대 크기
  - active/unique rule (`actv_scnr_id` unique 유지, `agnt_prpsl_id` unique 금지)
- `spring-api-implementation`은 16절의 real integration slice 중 Agent create/callback/read와 최신 실행 기준 조회부터 구현할 수 있다.
- current-state 조회 API는 별도 schema 없이 `spring-api-implementation`에서 `ModelParamProposalController`/`ModelParamProposalService`에 추가 구현할 수 있다.

## 16. API Slice Boundary

### Slice 1 — 2026-07-06~07-07 frontend mock

Included:

- `GET /scenarios/{scenarioId}/model-execution-options`
- `POST /scenarios/{scenarioId}/model-param-proposals`
- `GET /scenarios/{scenarioId}/model-param-proposals`
- `GET /scenarios/{scenarioId}/model-param-proposals/{id}`
- proposal SSE signal 또는 polling fallback
- `POST /scenarios/{scenarioId}/model-executions/from-parameters` JSON mock
- mock DTO/enum/seed/state transition
- `eventTypeL2Id` 제외 목록 기반 `PARAMETER_PROPOSAL`/`FILE_UPLOAD` 분기

Excluded:

- real Agent HTTP call
- real callback authentication
- DB proposal table
- real Excel merge
- real generated file upload/Job dispatch 변경

Mock strategy:

- JSON seed로 Agent callback 결과 shape 제공
- Scenario seed의 `eventTypeL2Id`로 `inputMode`를 계산하고 잘못된 workflow 호출을 거절
- `PROCESSING -> PROPOSED` 시간 지연 전이
- 사용자 confirmation request validation
- final execution은 기존 mock execution response shape 반환

Verification:

```bash
./gradlew test --tests '*ModelParamProposal*'
./gradlew compileJava -x test
```

필수 eligibility test case:

- `eventTypeL2Id=3`, `12`, `20` → `FILE_UPLOAD`
- `eventTypeL2Id=11`, `17`, `21` → `PARAMETER_PROPOSAL`
- 제외 목록에 없는 신규 ID → `PARAMETER_PROPOSAL`

Agent 계약 합의 후 real integration 필수 callback test case:

- 인증 없음/유효하지 않음 → `401`; 권한 불충분 → `403`
- Notion wrapper 최초 callback 반영, 동일 재전송 no-op
- Agent 응답 timeout 후 `REQUESTING` in-flight 유지 상태에서 callback 수신 → 같은 row에 수렴
- in-flight row 없는 late callback → terminal/history row 미변경 no-op
- 기존 terminal 결과와 상이 payload → terminal 상태 변경 없음
- Agent HTTP 응답과 callback 순서 역전 → `modelParamProposalId` write-once로 같은 row에 수렴
- callback transaction rollback → SSE 미발행

### Slice 2 — 2026-07-10 real integration

- Agent proposal request/confirmation
- 기존 ScenarioGroup의 `eventTypeL2Id` 기반 eligibility 계산
- `POST /scenarios/{scenarioId}/model-param-proposals/callback`
- `GET /scenarios/{scenarioId}/model-param-proposals`
- A8/A9는 5.5.1 공통 `LatestModelParamProposalExecutionContext` resolver를 사용한다.
- `GET /scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters`
  - A8은 연결 proposal의 조회 시점 현재 `confirmation_parameters`를 반환하며 실행 시점 immutable snapshot을 보장하지 않는다.
- `GET /scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters`
- A4a `EXECUTED_CONFIRMED` 응답의 `executionInputFiles`
  - A4a가 선택한 execution-preparation의 `caseId`를 사용하며 `complete`, `missingFileRoles`, `files`로 정상/불완전 3-file set을 구분한다.
  - 다른 current state에서는 `executionInputFiles=null`이다.
- 합의된 Bearer token 인증
- callback 신호 수신 + Agent proposal 테이블 재조회(read-only)
- Backend orchestration 행 monotonic update, commit 이후 SSE publish
- thin execution-preparation 행(`tmd_mi_model_execpreph.case_id` nullable unique, `agnt_prpsl_id` non-unique)
- 원본 workbook provider
- Apache POI merge
- generated file 3개 upload
- 기존 Case/Run/Job dispatch 연결

### Slice 3 — 2026-07-13 response-only grouped rationale projection

Included:

- A4a `parameter.groupedRationales`
- A4/A8/A9 `parameters[].groupedRationales`
- A4a typed DTO와 A4/A8/A9 `JsonNode`가 공유하는 response projector
- FE A4a→A6 mapping에서 `groupedRationales` 제거
- 공통 grouping, 네 GET service/serialization, A6 non-propagation 회귀 테스트

Excluded:

- 신규 endpoint
- A6 request DTO, X2 `ModelParamPayloadItem`, B1 callback, A5 SSE 변경
- Agent entity/저장 JSON, schema/DDL, repository/query 변경
- duplicate slot의 A6 validator 또는 Excel writer 동작 완화

Verification:

```bash
./gradlew test --tests '*ModelParameterResponseProjectorTest'
./gradlew test --tests 'com.sk.domain.mi.service.nextgen.ModelParamProposalServiceImplTest'
./gradlew test --tests 'com.sk.domain.mi.service.nextgen.ScenarioModelExecutionServiceImplTest'
./gradlew test --tests 'com.sk.domain.mi.controller.nextgen.ScenarioModelExecutionControllerTest'
./gradlew test --tests 'com.sk.domain.mi.service.nextgen.CurrentModelParamProposalSerializationTest'
./gradlew test --tests 'com.sk.domain.mi.service.nextgen.paramproposal.ModelParamConfirmationValidatorTest'
./gradlew test --tests 'com.sk.domain.mi.service.nextgen.execution.ModelParamExecutionCoordinatorTest'
./gradlew compileTestJava
```
