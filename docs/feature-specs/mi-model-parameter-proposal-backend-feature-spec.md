# MI 수급모델 파라미터 제안 Backend Feature Spec

## 1. Document Control

- domain: gas / MI model execution
- status: draft
- owner: platform BE
- created at: 2026-07-03
- updated at: 2026-07-03
- canonical for: frontend-facing endpoint path, HTTP method, request/response fields, validation, representative errors, callback contract
- canonical design reference: `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
- physical schema reference: 후속 ERD/DDL draft
- external evidence:
  - Notion `api 정의 (frontend-backend-agent)`
  - `https://app.notion.com/p/38f263ff004c80719d14de5702d946b9`
  - reviewed at 2026-07-03
- implementation readiness: mock ready after minor cleanup; real integration blocked by P0 Open Questions

## 2. Feature Goal

Frontend가 기존 Scenario의 기반 Event에 따라 두 가지 수급모델 실행 흐름을 사용할 수 있게 한다.

### A. 파라미터 제안 대상 Event 기반 Scenario

1. 파라미터 제안 생성 접수
2. SSE 완료 신호 수신 후 상태/결과 재조회
3. 제안값 표시 및 `proposedValue` 수정
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

## 4. Fixed API Contract

### Frontend-facing

| Method | Path | 목적 |
|---|---|---|
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-execution-options` | 기반 Event에 따른 실행 입력 mode 조회 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals` | Agent proposal 생성 접수 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals/{modelParamProposalId}` | 상태 및 proposal 결과 조회 |
| `GET` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals/stream` | proposal 완료/실패 SSE 신호 구독 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions` (`application/json`) | 제안 대상: 확정값으로 Excel 생성 후 실행 접수 |
| `POST` | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions` (`multipart/form-data`) | 비대상: 사용자 Excel 3개 업로드 후 기존 실행 접수 |

### Agent-facing callback

| Method | Path | 목적 |
|---|---|---|
| `POST` | `/api/v1/mi/next-gen/model-parameter-proposals/{modelParamProposalId}/callback` | Agent proposal 완료/실패 결과 수신 |

두 `POST /model-executions` 계약은 모두 active contract다. Backend는 Scenario의 기반 Event에서 도출한 `inputMode`와 `Content-Type`의 일치를 검증한다. Spring mapping은 `consumes`로 구분한다.

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

- mode는 Backend가 `Scenario -> ScenarioGroup -> Base Event` 관계로 도출한다.
- FE request로 mode를 변경할 수 없다.
- proposal eligibility의 canonical DB source는 P0 Open Question으로 관리한다.

### 5.1 Proposal 생성 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals`

- request body: 없음
- Backend는 Scenario/권한/중복 상태와 `inputMode=PARAMETER_PROPOSAL`을 검증한다.
- Backend는 Agent `POST /model-parameters/scenarios/{scenarioId}/proposals`를 호출한다.
- Agent HTTP 200 `PROCESSING` 응답을 Backend `BaseResponse`로 변환한다.
- Backend→FE HTTP status: `200 OK`

Response data:

```json
{
  "modelParamProposalId": 10001,
  "scenarioId": 309,
  "status": "PROCESSING",
  "requestedAt": "2026-07-03T15:00:00+09:00",
  "statusEndpoint": "/api/v1/mi/next-gen/scenarios/309/model-parameter-proposals/10001"
}
```

### 5.2 Proposal 상태/결과 조회

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals/{modelParamProposalId}`

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
          "rationale": "Germany 2027년 1월 한파 영향을 반영했습니다."
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
- `PROPOSED`: 전체 `parameters` 반환
- `FAILED`: `parameters=[]`, `failureReason` 반환
- `CONFIRMED/EXECUTING`: 사용자가 실행을 접수한 상태

### 5.3 SSE

`GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-parameter-proposals/stream`

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

`POST /api/v1/mi/next-gen/model-parameter-proposals/{modelParamProposalId}/callback`

- direction: Agent → Backend
- authentication: Bearer callback token 후보; 최종 security review 필요
- path ID와 body `data.modelParamProposalId`는 같아야 한다.
- callback payload는 Agent 외부 계약 wrapper를 그대로 수신한다.
- 성공 callback은 `PROPOSED`, 실패 callback은 `FAILED`만 허용한다.
- 같은 terminal payload 재전송은 HTTP 200으로 idempotent 처리한다.
- 저장 완료 후 SSE를 발행한다.

Backend response:

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": null
}
```

Callback 응답은 현재 프로젝트의 `BaseResponse<Void>` serialization shape를 따른다.

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
          "rationale": "사용자 검토 후 -3.5로 수정했습니다."
        }
      ]
    }
  ]
}
```

처리 순서:

1. Scenario, permission, proposal ownership/status 검증
2. 저장된 callback snapshot과 request 구조 비교
3. 변경 허용 필드가 `proposedValue`뿐인지 검증
4. 동일 proposal의 기존 Case/Run 존재 여부 확인
5. Agent confirmation API 호출
6. Agent `CONFIRMED` 응답 확인
7. 협의된 원본 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개 로드
8. 확정값을 지정 file/sheet/table/slot에 merge
9. 기존 Scenario Case/Run 생성, storage upload, Kubernetes Job dispatch 재사용
10. 기존 `ModelExecutionAcceptRes` 반환

Response는 기존 실행 접수 응답을 그대로 사용한다.

### 5.6 기존 파일 업로드 Model Execution 접수

`POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions`

- `Content-Type: multipart/form-data`
- `FILE_UPLOAD` mode에서만 허용
- 기존 `files` request part에 `DEMAND`/`SUPPLY`/`TRADE` 파일 3개를 전달한다.
- 기존 validation, Case/Run 생성, storage upload, Job dispatch, `ModelExecutionAcceptRes` 계약을 유지한다.
- 파라미터 제안 기능 도입으로 제거하거나 deprecated하지 않는다.

## 6. Request DTO Draft

```text
ModelParameterProposalExecutionReq
  Long modelParamProposalId          required
  List<ModelParameterConfirmation> confirmationParameters required, non-empty

ModelParameterConfirmation
  String parameterName               required, immutable
  ModelTarget modelTarget            required, immutable
  String rationale                   required, immutable for phase 1
  List<Map<String, Object>> values   required, non-empty

ModelTarget
  String modelFile                   required, immutable, DEMAND|SUPPLY|TRADE
  String modelSheet                  required, immutable
  String tableName                   required after Agent contract confirmation
  List<String> valueFieldNames       required, immutable
  List<String> slotAxesFieldNames    required, immutable
```

`values[]`의 동적 key 때문에 phase 1 DTO는 `Map<String, Object>`가 현실적이다. Backend는 저장된 proposal의 axis/value metadata를 기준으로 strict validation한다.

## 7. Response DTO Draft

```text
ModelParameterProposalAcceptRes
  Long modelParamProposalId
  Long scenarioId
  ModelParameterProposalStatus status
  OffsetDateTime requestedAt
  String statusEndpoint

ModelParameterProposalRes
  Long modelParamProposalId
  Long scenarioId
  Long scenarioGroupId
  Long eventId
  ModelParameterProposalStatus status
  List<ModelParameterProposalItemRes> parameters
  String failureReason
  OffsetDateTime requestedAt
  OffsetDateTime completedAt
```

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
| callback ID/Scenario 불일치 | 409 | `BIZ_RULE_VIOLATION` | 저장하지 않음 |
| callback status invalid | 400 | `REQ_INVALID_PARAMETER` | 저장하지 않음 |
| proposal이 `PROPOSED` 아님 | 409 | `BIZ_RULE_VIOLATION` | 실행 거절 |
| immutable target/axis/basecase 변경 | 400 | `REQ_INVALID_PARAMETER` | 실행 거절 |
| Agent confirmation 실패 | 502/503 | Agent mapping code | Excel 생성 시작하지 않음 |
| 원본 Excel 없음/version 불일치 | 422/500 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| file/sheet/table/slot mapping 실패 | 422 | `BIZ_RULE_VIOLATION` | 실행 생성하지 않음 |
| 동일 proposal 실행 중복 클릭 | 409 또는 기존 결과 반환 | 정책 확정 필요 | 중복 Case 방지 |

## 9. Swagger Annotation Recommendation

- proposal request summary: `MI 수급모델 파라미터 제안 생성 접수 API`
- proposal query summary: `MI 수급모델 파라미터 제안 상태/결과 조회 API`
- callback summary: `[Agent 콜백] MI 수급모델 파라미터 제안 완료 API`
- JSON execution summary: `확정 파라미터 기반 MI 수급모델 실행 접수 API`
- callback controller는 Agent-facing tag로 분리한다.
- 동적 `values[]`는 example과 field rule을 description에 명확히 기록한다.

## 10. Data/Model Impact

- mock slice:
  - schema change 없음
  - JSON seed + in-memory/Redis state 가능
  - Scenario별 `inputMode` mock seed 필요
- real slice 권고:
  - 신규 proposal history table
  - proposal/confirmation JSON snapshot
  - Agent proposal ID unique
  - Scenario/Case logical linkage
- 정확한 DDL은 `erd-change-review` 이후 별도 canonical draft에서 관리한다.

## 11. Recommended Data Access Approach

- proposal state/snapshot: JPA
- callback state transition: conditional update/CAS + transaction
- latest/active 조회가 복잡해지면 repository projection 또는 QueryDSL
- Excel merge: Apache POI service
- file upload/dispatch: 기존 `ScenarioModelExecutionServiceImpl` 내부 책임을 reusable collaborator로 추출

## 12. Implementation Touch Points

- controller
  - proposal frontend controller
  - Agent callback controller
  - JSON model execution mapping 추가
- service
  - `ModelParameterProposalService`
  - `ModelParameterProposalCallbackService`
  - `ModelInputWorkbookProvider`
  - `ModelParameterWorkbookMergeService`
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
  - callback base URL/token
  - base workbook location/version
  - proposal eligibility source
  - 단계별 mock switch

## 13. Open Questions

### P0

1. 원본 Excel 3개 위치와 version 선택 규칙.
2. Event의 proposal eligibility canonical source와 field/config 이름.
3. 기존 Event/Scenario에 대한 eligibility backfill 상태.
4. Agent `tableName` 최종 계약. 2026-07-07 이전 확인 필요.
5. Agent confirmation idempotency와 Backend 후속 실패 시 재시도 가능 여부.
6. callback Bearer token 발급/rotation/검증 방식.
7. Backend DB snapshot 저장안 승인 및 DBA 일정.
8. 동적 value type/null/precision 제한.

### P1

1. proposal 생성 실패 후 재제안 허용 여부.
2. 동일 proposal 실행 중복 요청을 409로 반환할지 기존 run response로 idempotent 반환할지.
3. SSE endpoint를 scenario scope로 둘지 기존 scenario-group SSE channel에 event type을 추가할지.

## 14. Change Log / Sync Note

- 2026-07-03: 사용자 설명과 Agent Notion 문서를 기준으로 최초 작성.
- 2026-07-03: 대상 Event 기반 Scenario는 Backend 원본 파일 생성/merge, 비대상 Event 기반 Scenario는 기존 사용자 파일 3개 업로드 방식으로 분기하도록 정정.
- 2026-07-03: 기존 multipart 실행 계약은 active contract로 유지하고 `inputMode` 판정 API를 추가.
- 2026-07-03: Agent confirmation 성공 후 Excel 생성 및 기존 execution pipeline 재사용 순서 반영.
- 2026-07-03: proposal 요청은 한 실행 준비 시도당 1회로 반영.

## 15. Exit Criteria / Handoff

- frontend mock 구현: ready after minor cleanup
- real Agent/DB/Excel 구현: P0 질문 해결 후 ready
- `erd-change-review` 대상:
  - proposal history table
  - JSON snapshot
  - active/unique rule
  - Case linkage와 callback idempotency
- `spring-api-implementation`은 16절의 mock slice부터 구현할 수 있다.

## 16. API Slice Boundary

### Slice 1 — 2026-07-06~07-07 frontend mock

Included:

- `GET /scenarios/{scenarioId}/model-execution-options`
- `POST /scenarios/{scenarioId}/model-parameter-proposals`
- `GET /scenarios/{scenarioId}/model-parameter-proposals/{id}`
- proposal SSE signal 또는 polling fallback
- `POST /scenarios/{scenarioId}/model-executions` JSON mock
- mock DTO/enum/seed/state transition
- `PARAMETER_PROPOSAL`/`FILE_UPLOAD` mock eligibility 분기

Excluded:

- real Agent HTTP call
- real callback authentication
- DB proposal table
- real Excel merge
- real generated file upload/Job dispatch 변경

Mock strategy:

- JSON seed로 Agent callback 결과 shape 제공
- Scenario seed별 `inputMode`를 반환하고 잘못된 workflow 호출을 거절
- `PROCESSING -> PROPOSED` 시간 지연 전이
- 사용자 confirmation request validation
- final execution은 기존 mock execution response shape 반환

Verification:

```bash
./gradlew test --tests '*ModelParameterProposal*'
./gradlew compileJava -x test
```

### Slice 2 — 2026-07-10 real integration

- Agent proposal request/confirmation
- real Event eligibility 조회
- callback + SSE publish
- DB snapshot
- 원본 workbook provider
- Apache POI merge
- generated file 3개 upload
- 기존 Case/Run/Job dispatch 연결
