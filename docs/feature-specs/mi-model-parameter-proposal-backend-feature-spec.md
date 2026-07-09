# MI 수급모델 파라미터 제안 Backend Feature Spec

## 1. Document Control

- domain: gas / MI model execution
- status: Review
- owner: BE (platform)
- created at: 2026-07-03
- updated at: 2026-07-08
- canonical for: frontend-facing API contract, Backend callback endpoint/minimum compatibility contract, proposed Agent integration questions
- canonical design reference: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- physical schema reference: `docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md`
- Agent schema evidence: `docs/design-docs/gas/mi-agent-param-proposal.md` (Draft, reference only)
- external evidence:
  - Notion `api 정의 (frontend-backend-agent)`
  - `https://app.notion.com/p/38f263ff004c80719d14de5702d946b9`
  - contract snapshot: 2026-07-03
  - re-reviewed at 2026-07-06
- depends on:
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
  - `docs/conventions/source-of-truth-governance.md`
- supersedes: 없음
- last synced: 2026-07-08
- open questions: 13절 참조
- integration model: callback = 완료 신호. Backend가 Agent proposal 테이블을 read-only 직접 조회해 결과 서빙(CAS/hash/snapshot 없음). 공유 DB read 및 read 권한은 확정(조회 가능). callback path `scenarioId`는 proposal identity가 아니라 현재 in-flight execution-preparation을 찾는 scope key다.
- callback 인증: 확정. 기존 시나리오 설계 콜백과 동일한 Resource Server JWT 체인(`CustomJwtDecoder` HS256 self-issued + `@RequiredPermissionRules(Role.SYSTEM ...)`) 재사용. Agent는 Backend 토큰 발급 API와 발급 인풋 2종을 전달받아 JWT를 자체 발급·갱신한다.
- implementation readiness: frontend mock ready; real Agent integration은 proposal read 계약 확정(DB DDL 수령)으로 착수 가능. 잔여는 스키마 세부(물리 토폴로지)·dimension_map ERD 정정으로 read 엔티티 착수를 막지 않음(인증·공유 read·콜백 계약·write-then-callback 순서·wrapper 셰이프·재시도 정책·proposal DDL 확정)

## 2. Feature Goal

Frontend가 기존 Scenario의 기반 Event에 따라 두 가지 수급모델 실행 흐름을 사용할 수 있게 한다.

### A. 파라미터 제안 대상 Event 기반 Scenario

1. 파라미터 제안 생성 접수
2. SSE 완료 신호 수신 후 상태/결과 재조회
3. `values[].proposedValue`만 표에 표시하고 필요 시 수정
4. 사용자 확정값으로 Agent confirmation
5. Backend 원본 Excel 3개 생성·병합
6. 기존 Scenario Model Execution 비동기 실행 접수

이 흐름에서 사용자는 Excel 파일을 업로드하지 않는다.

### B. 파라미터 제안 비대상 Event 기반 Scenario

1. 사용자가 기존 방식대로 `DEMAND`/`SUPPLY`/`TRADE` Excel 파일 3개를 업로드한다.
2. 기존 Scenario Model Execution 비동기 실행을 접수한다.

기존 multipart 파일 업로드 실행 계약은 계속 지원하며 제거·deprecated하지 않는다.

## 3. Canonical References

- lifecycle/business rules: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- 기존 model execution contract: `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md` F4
- 기존 execution/file persistence: `docs/design-docs/gas/mi-scenario-model-execution-workflow-design.md`
- Agent contract: 외부 evidence를 본 문서로 normalize하며 Notion 자체는 canonical source가 아니다.
- Agent DB 설계는 table ownership/field mapping의 evidence다. Agent DDL을 Backend schema contract로 사용하지 않는다.

### 3.1 API ↔ Agent DB mapping

| API field | Agent DB evidence | Rule |
|---|---|---|
| `modelParamProposalId` | proposal table PK 후보 | Agent identity. Backend local PK와 동일시하지 않음 |
| `scenarioId` | `scenario_id` | path와 callback/body가 저장된 request snapshot과 일치해야 함 |
| `scenarioGroupId` | `scenario_group_id` | `scenarioId`에서 도출한 요청 시점 snapshot |
| `eventId` | `mi_event_id` | API 명칭은 `eventId`, DB mapping은 `mi_event_id`로 고정 |
| `modelTarget.modelFile` | `model_file` | API는 `DEMAND|SUPPLY|TRADE`; Agent seed의 `Demand|Supply|Trade`는 boundary에서 normalize |
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
- `proposalParameters`와 `confirmationParameters`는 동일한 flat parameter array shape다. 두 snapshot 사이에서 `values[].proposedValue`만 달라질 수 있다.
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
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/{modelParamProposalId}` | 상태 및 proposal 결과 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/stream` | proposal 완료/실패 SSE 신호 구독 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions` (`application/json`) | 제안 대상: 확정값으로 Excel 생성 후 실행 접수 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions` (`multipart/form-data`) | 비대상: 사용자 Excel 3개 업로드 후 기존 실행 접수 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters` | 가장 최근 수급모델 실행에 사용된 확정 파라미터 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters` | 가장 최근 수급모델 실행의 원 Agent 제안 파라미터 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/input-files` | 가장 최근 수급모델 실행에 사용된 input Excel 3개 파일 메타데이터 조회 |

### Agent-facing callback

| Method | Path | 목적 |
|---|---|---|
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/callback` | Agent proposal 완료/실패 결과 수신 |

두 `POST /model-executions` 계약은 모두 active contract다. Backend는 Scenario의 기반 Event에서 도출한 `inputMode`와 `Content-Type`의 일치를 검증한다. Spring mapping은 `consumes`로 구분한다.

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
  "parameters": [
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
          "proposedValue": -3.8,
          "rationale": "Germany 2027년 1월 한파 영향을 반영해 제안값을 산정했습니다."
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
- 같은 parameter 안에서 slot-axis 값의 조합은 유일해야 한다.
- `basecaseValue`는 Agent 원본 비교·검증용으로 payload에 유지하지만 화면에는 표시하지 않는다.
- FE value cell은 `proposedValue`를 표시하며 사용자 수정도 동일 필드에 반영한다.
- 사용자가 값을 수정하지 않아도 조회한 원본 `proposedValue` 그대로 실행 요청할 수 있다.
- phase 1 표는 long-format이다. matrix wide-format pivot은 본 계약 범위에 포함하지 않는다.

**데이터 소스:** `status`/`parameters`/`failureReason`는 Backend가 **Agent proposal 테이블(`tmm_mi_model_param_proposal_m`)을 read-only로 직접 조회**해 서빙한다(callback payload를 저장·재생하지 않음). 컬럼 매핑(2026-07-07 DB DDL 확정): `status`←`status`(`PROCESSING|PROPOSED|CONFIRMED|FAILED`), `parameters`←`proposal_parameters`(또는 `CONFIRMED/EXECUTING`에서 `confirmation_parameters`), `failureReason`←`error_message`(+구조화 `error_code` 사용 가능), `completedAt`←`proposal_completed_dtm`, confirmation 시각←`confirmed_dtm`. `callback_status`/`callback_sent_dtm`은 Agent 전송 상태로 무시한다. `CONFIRMING/EXECUTING` 등 Backend 전용 상태는 Backend orchestration 행에서 덮어 얹는다. 상세는 Domain Design 3.1/3.5 참조.

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

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions`

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

1. Scenario, permission, proposal ownership/status 검증
2. Agent proposal 테이블의 `proposal_parameters` array와 request `confirmationParameters` 구조 비교
3. 변경 허용 필드가 `values[].proposedValue`뿐인지 검증
4. 같은 Scenario에 `RUNNING` Case가 없는지 기존 active-run guard로 확인
5. Agent proposal 상태를 재조회한다. `PROPOSED`면 Agent confirmation API를 호출한다. `CONFIRMED`이고 request `confirmationParameters`가 Agent `confirmation_parameters`와 동일하면 호출을 생략한다. `CONFIRMED`이지만 값이 다르면 같은 proposal 재확정(Option A)을 위해 Agent confirmation API 재호출이 필요하며, Agent가 CONFIRMED 상태 재확정을 허용하지 않는 환경에서는 `409 BIZ_RULE_VIOLATION`으로 거절하고 새 proposal 생성을 요구한다.
6. Agent `CONFIRMED` 응답 확인
7. 협의된 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개 로드
8. 확정값을 지정 file/sheet/table/slot에 merge
9. 기존 Scenario Case/Run 생성, storage upload, Kubernetes Job dispatch 재사용. 실행 1회마다 새 `case_id`를 생성한다
10. 기존 `ModelExecutionAcceptRes` 반환

**재시도·멱등 (P0 resolved):** callback과 동일한 monotonic read-back 패턴으로 confirmation을 취급한다.

- 재시도 단위는 **실행 POST 전체 재요청**이다(부분 resume 없음).
- **동일값 재시도에 대해서는 Agent confirmation 멱등성을 계약 전제로 요구하지 않는다.** Backend가 호출 직전 proposal 상태를 재조회(5번)하고 이미 `CONFIRMED`이며 request 값이 저장값과 같으면 confirmation을 생략하므로, 재시도·응답 유실(Agent는 CONFIRMED, Backend는 응답 미수신) 상황에서도 이중 confirm이 발생하지 않는다. Agent가 추가로 confirmation을 멱등 처리하면 방어적 보너스이며 계약 전제는 아니다.
- confirmation 성공 이후 단계(원본 로드·merge·dispatch) 실패는 Agent `CONFIRMED`를 롤백하지 않는다. 사용자가 실행 POST를 재요청하면 Backend는 `CONFIRMED`를 확인하고 confirmation을 건너뛴 뒤 Excel merge·Case/Run·dispatch를 다시 수행한다.
- 재요청의 `confirmationParameters`가 이미 저장된 `confirmation_parameters`와 동일하면 기존 confirmed 값을 재사용한다. 다르면 같은 `modelParamProposalId` 재확정(Option A)으로 Agent `confirmation_parameters`를 갱신한 뒤 새 `tmd_mi_model_execpreph` row와 새 `case_id`로 실행한다. 단, Agent가 CONFIRMED 상태 재확정을 허용하지 않는 배포에서는 값 변경 재실행을 거절하고 새 proposal 생성을 요구한다.
- Case/Run 중복 가드는 **Scenario 기준 RUNNING 최대 1개**만 유지한다. 동일 proposal request에서 여러 번 실행 접수할 수 있으며, 각 접수는 새 `case_id`를 만든다. 이미 RUNNING Case가 있으면 409로 차단한다.
- 잔여 가정: Agent가 `CONFIRMED` 시점에 `confirmation_parameters`를 저장한다(Agent DB 설계 문서에 명시됨 — 재요청/재확정 시 재검증 근거). CONFIRMED 상태 재확정 허용 여부는 Agent 계약 확인이 필요하다.

FE는 GET으로 받은 proposal array를 local form state로 유지한다. 실행 요청 시 각 row의 현재 `proposedValue`를 `confirmationParameters`에 담는다. 사용자가 수정한 row는 수정값을, 수정하지 않은 row는 Agent 생성값을 그대로 보낸다. Backend는 immutable field를 검증한 뒤 동일 confirmation array를 Agent confirmation API에 전달한다.

Response는 기존 실행 접수 응답을 그대로 사용한다.

Backend Agent client는 5번에서 Agent Notion 계약의 wrapper를 정확히 전송한다. `modelFile`은 Backend 대문자 enum을 Agent casing인 `Demand | Supply | Trade`로 역변환한다.

Agent로 전송하는 body는 `{data:{confirmationParameters:[...]}}`이며 5.5 request의 flat parameter array를 그대로 사용한다.

### 5.6 최근 실행 기준 확정/제안 파라미터 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters`

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters`

가장 최근 수급모델 실행 Case를 기준으로 그 실행에 연결된 `tmd_mi_model_execpreph` row와 Agent proposal snapshot을 조회한다.

조회 기준:

1. `tmd_mdl_scnrcasem`에서 해당 `scenarioId`의 최신 실행 Case를 찾는다.
2. `tmd_mi_model_execpreph.case_id = latestCase.case_id`인 execution-preparation row를 찾는다.
3. `tmd_mi_model_execpreph.agnt_prpsl_id`로 Agent proposal 테이블을 read-only 조회한다.
4. confirmed endpoint는 Agent `confirmation_parameters`, proposed endpoint는 Agent `proposal_parameters`를 반환한다.

Response data:

```json
{
  "scenarioId": 309,
  "caseId": 88001,
  "execPrepId": 51001,
  "modelParamProposalId": 10001,
  "parameters": [
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

- 가장 최근 실행 Case가 없으면 `404 RSC_NOT_FOUND`.
- 최신 Case에 연결된 `tmd_mi_model_execpreph` row가 없으면 `404 RSC_NOT_FOUND` 또는 legacy 데이터로 판단해 FE에 비노출한다.
- same `scenarioId + modelParamProposalId`로 여러 번 실행할 수 있으므로 `modelParamProposalId`만으로 최신 실행을 판단하지 않는다.
- 현재 계약은 최신 실행 기준 조회만 보장한다. 같은 Agent proposal의 값 변경 재실행은 Agent `confirmation_parameters`를 갱신하는 Option A를 따른다. 과거 실행별 confirmed parameter 복원이 요구되면 Agent 단일 최신 snapshot만으로는 부족하므로 `tmd_mi_model_execpreph`에 실행별 confirmed parameter snapshot JSON을 추가하는 별도 ERD 변경이 필요하다.

### 5.7 최근 실행 input Excel 파일 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions/latest/input-files`

가장 최근 수급모델 실행에 사용된 input Excel 3개(`DEMAND`/`SUPPLY`/`TRADE`)의 파일 메타데이터를 조회한다. 실제 파일 다운로드는 기존 File Management download API(`/api/v1/file-management/download/file`)에 `fileNo`를 넘겨 수행한다.

조회 기준:

1. `tmd_mdl_scnrcasem`에서 해당 `scenarioId`의 최신 실행 Case를 찾는다.
2. 최신 `case_id` 기준으로 기존 실행 파일 저장 규칙(`tcm_filel`, `SCNRCASE + case_id`)에서 input Excel 3개 파일을 조회한다.
3. 파일 role은 기존 저장 metadata 또는 filename convention으로 `DEMAND`/`SUPPLY`/`TRADE`를 식별한다.

Response data:

```json
{
  "scenarioId": 309,
  "caseId": 88001,
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
```

- 최신 실행 Case가 없으면 `404 RSC_NOT_FOUND`.
- 3개 파일 중 일부가 없으면 `409 BIZ_RULE_VIOLATION` 또는 `500`이 아니라 데이터 불완전 상태로 로깅하고 FE에는 다운로드 불가 상태를 명확히 반환한다.
- 파일 binary를 이 endpoint에서 직접 stream하지 않는다. 공통 파일 다운로드 정책/권한/감사를 재사용한다.

### 5.8 기존 파일 업로드 Model Execution 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions`

- `Content-Type: multipart/form-data`
- `FILE_UPLOAD` mode에서만 허용
- 기존 `files` request part에 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개를 전달한다.
- 기존 validation, Case/Run 생성, storage upload, Job dispatch, `ModelExecutionAcceptRes` 계약을 유지한다.
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
  String rationale                   required, immutable
  List<Map<String, Object>> values   required, non-empty; dynamic slot-axis keys + basecaseValue/proposedValue/rationale

ModelTarget
  String modelFile                   required, immutable, DEMAND|SUPPLY|TRADE
  String modelSheet                  required, immutable
  String tableName                   required, immutable
  List<String> valueFieldNames       required, immutable, size=1 in phase 1
  List<String> slotAxesFieldNames    required, non-empty, immutable
```

`proposal_parameters`와 `confirmation_parameters`는 모두 flat parameter array다. Backend는 parameter identity(`parameterName + modelTarget`)와 slot-axis 값 조합을 기준으로 두 array를 비교하며 `values[].proposedValue` 이외의 차이를 거절한다. JSON object property/array 순서와 numeric lexical form(`-3.5`, `-3.50`)은 동일성 판정에서 제외하고 DTO 역직렬화 후 의미값으로 비교한다.

`values[]` validation:

- dynamic slot-axis key 집합은 `modelTarget.slotAxesFieldNames`와 정확히 일치한다.
- `basecaseValue`, `proposedValue`는 required numeric이며 `rationale`은 required string이다.
- phase 1에서는 `valueFieldNames.size() == 1`이므로 하나의 `basecaseValue`/`proposedValue` pair가 해당 value field에 대응한다.
- `basecaseValue`는 UI 비표시·immutable이고 `proposedValue`가 UI 표시·편집·confirmation 전달 값이다.
- confirmation에서는 `proposedValue`만 변경 가능하며 미수정 값도 원본 그대로 포함한다.

`AgentCallbackEnvelopeReq.status`는 wrapper 수신 시 boundary에서 공용 `AgentCallbackResult`(`com.sk.domain.shared.enums.nextgen.AgentCallbackResult`)로 역직렬화한다. 이 enum은 `@JsonCreator`로 `success`/`failure`/`error`를 이미 수용하며, 기존 시나리오 설계 콜백 DTO(`ScenarioGenerationCallbackReq.result` 등)가 쓰는 것과 동일하다. 두 Agent 연동이 같은 성공/실패 enum을 공유해 도메인 레벨에서 일관된다. `data.status`(`AgentProposalTerminalStatus`, `PROPOSED|FAILED`)는 도메인 terminal 상태로 별도 유지한다.

## 7. Response DTO Draft

```text
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
  List<ModelParamPayloadItem> parameters
  String failureReason
  OffsetDateTime requestedAt
  OffsetDateTime completedAt

LatestModelParamProposalParametersRes
  Long scenarioId
  Long caseId
  Long execPrepId
  Long modelParamProposalId
  List<ModelParamPayloadItem> parameters

LatestModelExecutionInputFilesRes
  Long scenarioId
  Long caseId
  List<ModelExecutionInputFileRes> files

ModelExecutionInputFileRes
  String fileRole                    DEMAND|SUPPLY|TRADE
  Long fileNo
  String fileNm
  Long fileSize

```

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

## 8. Representative Error Scenarios

| 상황 | HTTP | ResultCode 후보 | 처리 |
|---|---:|---|---|
| Scenario 없음/삭제 | 404 | `RSC_NOT_FOUND` | proposal/실행 거절 |
| 권한 없음 | 403 | 공통 permission error | 거절 |
| `FILE_UPLOAD` mode에서 proposal 요청 | 409 | `BIZ_RULE_VIOLATION` | workflow 불일치 거절 |
| `FILE_UPLOAD` mode에서 JSON execution | 409 | `BIZ_RULE_VIOLATION` | workflow 불일치 거절 |
| `PARAMETER_PROPOSAL` mode에서 multipart execution | 409 | `BIZ_RULE_VIOLATION` | workflow 우회 거절 |
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
| proposal이 `PROPOSED` 아님 | 409 | `BIZ_RULE_VIOLATION` | 실행 거절 |
| `values[].proposedValue` 외 parameter/target/axis/basecase/rationale 변경 | 400 | `REQ_INVALID_PARAMETER` | 실행 거절 |
| Agent confirmation 실패 | 502/503 | Agent mapping code | Excel 생성 시작하지 않음 |
| 원본 Excel 없음/version 불일치 | 422/500 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| file/sheet/table/slot mapping 실패 | 422 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| 동일 proposal로 실행 재요청 | 200/202 기존 실행 접수 응답 | 기존 정책 | RUNNING이 없으면 새 `case_id` 실행 접수 |
| 같은 Scenario에 RUNNING 실행 존재 | 409 | `RSC_ALREADY_EXISTS` | 기존 `ScenarioCase.actv_scnr_id` guard |

## 9. Swagger Annotation Recommendation

- proposal request summary: `MI 수급모델 파라미터 제안 생성 접수 API`
- proposal query summary: `MI 수급모델 파라미터 제안 상태/결과 조회 API`
- callback summary: `[Agent 콜백] MI 수급모델 파라미터 제안 완료 API`
- JSON execution summary: `확정 파라미터 기반 MI 수급모델 실행 접수 API`
- callback controller는 Agent-facing tag로 분리한다.
- callback operation은 Bearer security scheme(`Role.SYSTEM` 등 `@RequiredPermissionRules`)을 명시한다. 기존 시나리오 설계 콜백과 동일한 self-issued JWT를 재사용한다.
- flat parameter array, dynamic slot-axis key, `valueFieldNames.size=1`, `values[].proposedValue` 단일 mutable field 규칙을 description과 example에 명확히 기록한다.

## 10. Data/Model Impact

- mock slice:
  - schema change 없음
  - JSON seed + in-memory/Redis state 가능
  - Scenario별 `eventTypeL2Id` mock seed로 `inputMode`를 계산
- real slice 권고:
  - Agent proposal 테이블 read-only 직접 조회로 결과 서빙 (DDL/read 권한 확정, 동일 schema 배치 확인 필요)
  - thin Backend execution-preparation persistence(`tmd_mi_model_execpreph`: 제안 기반 실행 준비 상태, Agent proposal ID, 실행 Case 연결; proposal 데이터·hash 없음)
  - 실제 수급모델 실행 master/history는 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 계속 source of truth다.
  - `tmd_mi_model_execpreph` row 1건은 input Excel 3개 세트 생성과 수급모델 실행 접수 1회를 나타낸다. 같은 `scnr_id + agnt_prpsl_id`로 여러 번 실행할 수 있으므로 매 실행마다 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만든다.
  - `tmd_mi_model_execpreph.case_id`는 nullable + UNIQUE 포인터다. 실행 접수 전에는 null이고, 실행 접수 후에는 해당 row가 만든 단일 Case를 가리킨다. 한 Case가 여러 execution-preparation row에 붙으면 안 된다.
  - `agnt_prpsl_id`는 unique가 아니다. 반복 실행 이력과 callback 후보 검색을 위해 `(agnt_prpsl_id, exec_prep_stat_cd, exec_prep_id DESC)` 일반 index를 둔다.
  - 최근 실행 기준 조회는 `tmd_mdl_scnrcasem` 최신 Case → `tmd_mi_model_execpreph.case_id` → Agent proposal table 순서로 수행한다.
  - 최근 실행 input Excel 파일은 기존 `tcm_filel(SCNRCASE + case_id)` 저장 규칙에서 조회한다. 파일 binary download는 공통 File Management API를 재사용한다.
- Agent-owned schema:
  - parameter master와 dimension mapping은 Backend에서 복제하지 않는다.
  - proposal table은 Agent lifecycle의 source이며 Backend Case/Run orchestration source가 아니다.
  - `proposal_parameters`, `confirmation_parameters`는 JSON root array다. 기존 `JSON_TYPE = 'ARRAY'` CHECK를 유지한다.
- 정확한 DDL은 `erd-change-review` 이후 별도 canonical draft에서 관리한다.

## 11. Recommended Data Access Approach

- proposal state/snapshot: JPA
- callback 처리: Agent 테이블 재조회 → Backend orchestration 행 monotonic update + transaction (CAS 없음)
- callback SSE publish: transaction after-commit event
- latest/active 조회가 복잡해지면 repository projection 또는 QueryDSL
- Excel merge: Apache POI service
- file upload/dispatch: 기존 `ScenarioModelExecutionServiceImpl` 내부 책임을 reusable collaborator로 추출

## 12. Implementation Touch Points

- controller
  - proposal frontend controller
  - Agent callback controller
  - JSON model execution mapping 추가
- security
  - callback endpoint는 기존 시나리오 설계 콜백과 동일하게 표준 Resource Server JWT chain(`CustomJwtDecoder`, HS256 self-issued) + `@RequiredPermissionRules(Role.SYSTEM ...)`을 재사용한다.
  - Agent는 Backend 토큰 발급 API와 발급 인풋 2종으로 JWT를 자체 발급·갱신한다. dedicated static token/HMAC/외부 OAuth2 도입은 없다.
  - `ResourceServerConfig.tokenByPassSecurityFilterChain` matcher에 callback path를 추가하지 않는다.
- service
  - `ModelParamProposalService`
  - `ModelParamProposalCallbackService`
  - `ModelInputWorkbookProvider`
  - `ModelParamWorkbookMergeService`
  - 기존 execution pipeline reusable method 추출
- client
  - proposal request/confirmation method를 Agent HTTP interface에 추가
- DTO/enums
  - proposal/confirmation/callback/SSE/status
- persistence
  - proposal entity/repository 또는 mock store
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

1. proposal table read 계약은 확정(2026-07-07 DB DDL 수령, 컬럼 매핑 5.2 반영). **잔여**: (a) Agent/Backend 물리 토폴로지(동일 schema/instance vs view/synonym), (b) dimension_map ERD blocker #2(nullable UNIQUE)/#3(조건부 CHECK) 정정 — Backend 런타임 미의존, `erd-change-review`로 분리 처리.
2. Agent callback/confirmation payload에 master의 `model_table`을 `tableName`으로 반영하는 배포 시점(required shape는 확정).
3. 원본 Excel 3개 위치와 version 선택 규칙.
4. Backend execution-preparation 저장안은 채택됐고 ERD/DDL draft는 작성·검토됨. 잔여: `tmd_mi_model_execpreph` table/subject DBA 최종 승인 및 DDL 적용. application 구현은 착수 가능하지만, `ddl-auto=validate`가 적용되는 real runtime 배포 전에는 DBA DDL 적용이 선행돼야 한다.
5. `basecaseValue`/`proposedValue`의 null/precision/최대·최소 범위.
6. parameter/value row 최대 건수와 JSON column size 한도(컬럼 타입은 native `JSON`+`JSON_TYPE=ARRAY` CHECK로 확정).
7. callback 신호 유실 시 폴백 폴링 interval/timeout.
8. `schemaVersion`, `completedAt`, `errorCode` 확장 필요 여부 — `completedAt`(`proposal_completed_dtm`)·`error_code`는 Agent 테이블에 **이미 컬럼 존재**, `schemaVersion`만 미존재. `errorMessage`는 wrapper `message`와 중복됨.

### P1

1. proposal 생성 실패 후 재제안 허용 여부.
2. Agent confirmation API가 이미 `CONFIRMED`인 proposal의 재확정을 허용하는지 여부. 허용하면 같은 Agent proposal로 값 변경 재실행을 지원하고 Agent `confirmation_parameters` 최신 snapshot을 갱신한다. 허용하지 않으면 값 변경 재실행은 새 proposal 생성으로만 지원한다. 과거 실행별 confirmed parameter 복원이 필요하면 `tmd_mi_model_execpreph`에 실행별 confirmed parameter JSON snapshot을 추가해야 한다.
3. SSE endpoint를 scenario scope로 둘지 기존 scenario-group SSE channel에 event type을 추가할지.

## 14. Change Log / Sync Note

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
- 2026-07-07: **Agent proposal table DB DDL 수령·proposal read 계약 확정**(`docs/design-docs/gas/mi-agent-param-proposal.md` 3절, ERD blocker #1 해소). 컬럼 매핑을 5.2에 확정 — `completedAt`←`proposal_completed_dtm`, `failureReason`←`error_message`(+`error_code`), `parameters`←`proposal_parameters`/`confirmation_parameters`(native `JSON`+`JSON_TYPE=ARRAY`), `status` CHECK로 4종 고정. `completedAt`/`error_code`가 Agent 스키마에 이미 존재함을 P0 8에 반영. **잔여**: `scenario_id` 유일성 부재(DB 미강제 → app 가드+`modelParamProposalId` 교차검증으로 correlation, race 방지 partial-unique/lock 협의)·물리 토폴로지·dimension_map blocker #2/#3(erd-change-review). P0 1을 이 잔여로 좁힘.
- 2026-07-07: **Backend execution-preparation 선행 생성 순서 확정.** Agent 호출 전에 `scenarioId` 기준 `REQUESTING` 행을 `modelParamProposalId=null`로 생성·commit하고 외부 호출한다. Agent 응답의 ID 또는 callback이 Agent 테이블 재조회로 확인한 ID 중 먼저 도착한 값을 write-once로 채우며, 늦은 응답은 callback 상태를 `PROCESSING`으로 되돌리지 않는다. timeout 시 late callback을 위해 preparation을 즉시 삭제하지 않는다.
- 2026-07-08: **callback correlation을 scope-key 방식으로 재정리.** path `scenarioId`는 proposal identity가 아니라 현재 in-flight execution-preparation을 찾는 scope key다. `data.modelParamProposalId`가 있으면 in-flight 후보 검색 조건으로 사용하고, 없으면 path `scenarioId`의 현재 in-flight row에만 수렴한다. `modelParamProposalId`는 반복 실행 때문에 `tmd_mi_model_execpreph`에서 unique가 아니며 terminal/history row에 붙이지 않는다. in-flight row가 없으면 late/no-op이며 terminal/history row에 붙이지 않는다. 모호한 Agent 호출 실패는 `REQUEST_FAILED`로 닫지 않고 `REQUESTING` in-flight 상태를 유지해 callback/read-reconcile/timeout 복구로 수렴한다.
- 2026-07-08: Backend proposal request orchestration ERD/DDL blocker를 재분류. `tmd_mi_model_execpreph` 물리명 유지, table comment `TMD_파라미터제안생성요청이력` 채택, Agent read permission/DDL/callback auth/write-then-callback은 resolved로 유지. `tmd_mi_model_execpreph`는 실행 이력이 아니며 실제 수급모델 실행 master/history는 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 계속 소유한다. 구현 착수 가능하되 real runtime 배포는 `tmd_mi_model_execpreph` DBA DDL 적용과 Agent/Backend 물리 토폴로지 pre-check가 필요하다.
- 2026-07-08: **proposal request 1건에서 여러 수급모델 실행을 허용하도록 Case linkage 전제를 정정.** 기존 `tmd_mi_model_execpreph 1:N tmd_mdl_scnrcasem` linkage 및 `tmd_mdl_scnrcasem.exec_prep_id` 추가안은 supersede. 실행 1회마다 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만들고, `tmd_mi_model_execpreph.case_id` nullable + UNIQUE 포인터로 해당 실행 Case를 연결한다. 같은 `scnr_id + agnt_prpsl_id` 반복 실행을 허용하므로 `agnt_prpsl_id` unique는 금지하고 일반 index로 둔다. 최근 실행 기준 확정/제안 파라미터 조회와 input Excel 파일 조회는 최신 Case → `tmd_mi_model_execpreph.case_id` 경로를 사용한다.

## 15. Exit Criteria / Handoff

- frontend mock 구현: ready
- callback 계약(엔드포인트/인증/wrapper/순서/응답): Agent 전달 ready. 전달용 계약서는 `docs/design-docs/gas/mi-model-param-proposal-callback-contract.md`
- real Agent/DB/Excel 구현: proposal read 계약 확정(DB DDL 수령)으로 read 엔티티/조회 착수 가능. `tmd_mi_model_execpreph`와 Agent proposal table이 DB 반영됐으므로 Agent create/callback/read 및 최신 실행 기준 파라미터/파일 조회는 mock 없이 실DB 연동 slice로 구현 가능하다. Excel merge 서비스 자체는 별도 개발 slice로 둔다.
- Backend proposal request/execution-preparation 구현: API/service/repository 구현은 착수 가능. real runtime 배포는 `tmd_mi_model_execpreph` DBA DDL의 index 보정(`agnt_prpsl_id` non-unique, `case_id` nullable unique) 확인 후 가능하다. 실행 이력 source of truth는 기존 Case/Run이며, `tmd_mi_model_execpreph.case_id`로 제안 기반 실행 준비 row와 단일 실행 Case를 연결한다.
- `erd-change-review` 대상:
  - Agent proposal 테이블 read-only 조회 + thin Backend execution-preparation(orchestration) 책임 분리
  - Agent/Backend 물리 토폴로지
  - `tmd_mi_model_execpreph.case_id` nullable unique linkage와 `agnt_prpsl_id` non-unique index DBA 반영 확인
  - dimension_map ERD blocker #2(nullable UNIQUE)/#3(조건부 CHECK) 정정
  - (선택) 실행별 confirmed snapshot
  - Agent JSON root array CHECK와 payload 최대 크기
  - active/unique rule (`actv_scnr_id` unique 유지, `agnt_prpsl_id` unique 금지)
- `spring-api-implementation`은 16절의 real integration slice 중 Agent create/callback/read와 최신 실행 기준 조회부터 구현할 수 있다.

## 16. API Slice Boundary

### Slice 1 — 2026-07-06~07-07 frontend mock

Included:

- `GET /scenarios/{scenarioId}/model-execution-options`
- `POST /scenarios/{scenarioId}/model-param-proposals`
- `GET /scenarios/{scenarioId}/model-param-proposals/{id}`
- proposal SSE signal 또는 polling fallback
- `POST /scenarios/{scenarioId}/model-executions` JSON mock
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
- `GET /scenarios/{scenarioId}/model-executions/latest/model-param-proposal/confirmed-parameters`
- `GET /scenarios/{scenarioId}/model-executions/latest/model-param-proposal/proposed-parameters`
- `GET /scenarios/{scenarioId}/model-executions/latest/input-files`
- 합의된 Bearer token 인증
- callback 신호 수신 + Agent proposal 테이블 재조회(read-only)
- Backend orchestration 행 monotonic update, commit 이후 SSE publish
- thin execution-preparation 행(`tmd_mi_model_execpreph.case_id` nullable unique, `agnt_prpsl_id` non-unique)
- 원본 workbook provider
- Apache POI merge
- generated file 3개 upload
- 기존 Case/Run/Job dispatch 연결
