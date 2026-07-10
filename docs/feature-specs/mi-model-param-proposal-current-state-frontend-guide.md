# 수급모델 파라미터 제안 — 프론트 통합 가이드

> Base path: `/api/v1/mi/next-gen/scenarios/{scenarioId}`
> 대상 기능: 수급모델 실행 전, Agent가 파라미터를 제안하고 사용자가 검토·확정해 실행으로 넘기는 흐름.

이 문서는 **프론트가 이 기능을 붙일 때 궁금해할 것**을 한곳에 정리한 것입니다.
전체 흐름 → 엔드포인트 목록 → 상태(currentState) → SSE → 에러 → FAQ 순서입니다.

---

## 0. 등장 인물 & 큰 그림

- **FE(프론트)**: 팝업 UI. 상태 조회 + 제안 요청 + 확정.
- **BE(백엔드)**: 오케스트레이션(preparation row 관리) + Agent 중계 + 상태 정규화.
- **Agent**: 실제 파라미터 제안 생성. 비동기로 처리 후 **BE에 콜백**.

```
FE ──① 제안 요청(POST)──▶ BE ──▶ Agent (비동기 생성 시작)
FE ──② 상태 조회(GET)──▶ BE   (지금 어느 단계인지)
                          ▲
Agent ──콜백(완료/실패)──▶ BE ──③ SSE push──▶ FE (완료/실패 알림)
FE ──④ SSE 받으면 다시 ② GET──▶ 최신 상태로 화면 갱신
```

**핵심 원칙**: FE는 **`currentState` 하나로 화면을 분기**합니다. Agent 상태·내부 상태를 직접 조합할 필요 없습니다. BE가 이미 정규화해서 내려줍니다.

---

## 1. 엔드포인트 목록

| # | Method | Path (base 이하) | 용도 | FE 사용 시점 |
|---|---|---|---|---|
| A | GET | `/model-execution-options` | 입력 방식(제안 대상 여부) 조회 | 팝업/버튼 노출 판단 |
| B | POST | `/model-param-proposals` | 제안 생성 **요청 접수** | "제안 요청" 클릭 |
| C | GET | `/model-param-proposals` | **현재 상태 조회 (핵심)** | 팝업 진입 / SSE 수신 후 |
| D | GET | `/model-param-proposals/{modelParamProposalId}` | 특정 제안 상세 | 특정 제안 결과 상세 필요 시 |
| E | GET | `/model-param-proposals/stream` | SSE 구독 (text/event-stream) | 진행 중 상태일 때 |
| F | POST | `/model-executions` | 확정 파라미터로 **수급모델 실행 트리거** ⚠️ **구현 전** | 확정 후 실행 |
| — | POST | `/model-param-proposals/callback` | **Agent→BE 콜백** (FE 사용 X) | — |

> ⚠️ **F. POST `/model-executions`는 아직 구현 전입니다.** 경로만 우선 안내하며, 요청/응답 스펙은 확정 후 갱신 예정.
> 이 API가 확정→실행을 트리거하면, 이 API 묶음이 그 결과를 `currentState`(`CONFIRMED_PENDING_EXECUTION`/`EXECUTING`/`EXECUTED_CONFIRMED`)로 반영해 보여줍니다.

모든 응답은 공통 `BaseResponse<T>` 래핑입니다(`{ code, message, data }` 형태). 아래 필드 설명은 `data` 기준.

---

## 2. 엔드포인트별 상세

### A. GET `/model-execution-options` — 입력 방식 조회
이 시나리오가 **파라미터 제안 대상인지 / 파일 업로드 대상인지** 판단.

응답 (`ModelExecutionOptionsRes`):
| 필드 | 예시 | 설명 |
|---|---|---|
| `scenarioId` | 309 | |
| `inputMode` | `PARAMETER_PROPOSAL` \| `FILE_UPLOAD` | 입력 방식 |
| `parameterProposalAvailable` | true | 제안 기능 사용 가능 여부 |
| `fileUploadAvailable` | false | 파일 업로드 사용 가능 여부 |

> `inputMode`는 시나리오의 EventTypeL2로 BE가 판정합니다. `FILE_UPLOAD`면 이하 제안 API는 전부 `409`.

---

### B. POST `/model-param-proposals` — 제안 생성 요청 접수
요청 바디 없음(scenarioId만 path). BE가 Agent에 생성을 요청하고 접수 응답을 즉시 반환(비동기 시작).

응답 (`ModelParamProposalAcceptRes`):
| 필드 | 예시 | 설명 |
|---|---|---|
| `modelParamProposalId` | 10001 | Agent 제안 ID (이후 상세/SSE 대조용) |
| `scenarioId` | 309 | |
| `status` | `PROCESSING` | 접수 시점 Agent 상태 (항상 처리 시작 상태) |
| `requestedAt` | 2026-07-09T15:30:00 | 접수 시각 |
| `statusEndpoint` | `.../model-param-proposals/10001` | 상세 조회 endpoint(그대로 사용 가능) |

**접수 성공 = 생성 완료 아님.** 이후 완료 신호는 SSE(E)로 오고, 최신 상태는 GET(C)으로 확인합니다.

거부 케이스:
- 이미 진행 중인 제안/실행 존재 → `409 Conflict`
- `FILE_UPLOAD` 모드 → `409 Conflict`
- 폐기(DISCARDED) 시나리오 → `400`

---

### C. GET `/model-param-proposals` — 현재 상태 조회 (핵심)
팝업 진입 시 첫 호출. **`currentState` 하나로 화면 분기.** (자세한 상태 정의는 §3)

응답 (`CurrentModelParamProposalRes`) 주요 필드:
| 필드 | 설명 |
|---|---|
| `currentState` | **화면용 정규화 상태 (8종)** — 이것만 보고 분기 |
| `parameterSource` | `parameters`의 출처: `NONE` / `PROPOSAL_PARAMETERS`(제안값) / `CONFIRMATION_PARAMETERS`(확정값) |
| `parameters` | flat 파라미터 배열 (출처는 `parameterSource`) |
| `modelFiles` | `parameters`를 `modelFile > modelSheet > tableName` 계층으로 머지한 트리 (탐색 UI용) |
| `failureReason` | 실패 상태에서만 채워짐 |
| `execPrepId` / `modelParamProposalId` | BE preparation ID / Agent 제안 ID |
| `scenarioGroupId` / `eventId` | 참조용 |
| `caseId` / `runId` / `modelExecutionStatus` | 실행 단계 진입 후 채워짐 (`RUNNING`/`COMPLETED`/`FAILED`) |
| `requestedAt` / `completedAt` / `confirmedAt` | 요청 / 제안완료 / 확정 시각 |

> **주의**: 필드는 상태에 따라 채워지는 것만 유효합니다. §3의 "필드 신뢰 규칙" 표를 반드시 따르세요.

---

### D. GET `/model-param-proposals/{modelParamProposalId}` — 특정 제안 상세
특정 제안 하나의 상태/결과. (C가 "시나리오 기준 현재"라면, D는 "이 제안 ID 기준")

응답 (`ModelParamProposalRes`):
| 필드 | 설명 |
|---|---|
| `status` | FE 표시 상태 문자열 (Agent status 또는 BE overlay status) |
| `parameters` / `modelFiles` | 제안/확정 파라미터와 계층 트리 |
| `failureReason` | 실패 사유 |
| `modelParamProposalId` / `scenarioId` / `scenarioGroupId` / `eventId` | 식별자 |
| `requestedAt` / `completedAt` | 요청 / 완료(또는 확정) 시각 |

대부분의 팝업 흐름은 **C(현재 상태)만으로 충분**합니다. D는 특정 제안 이력을 콕 집어 볼 때만.

---

### E. GET `/model-param-proposals/stream` — SSE 구독
진행 중(`GENERATING` 등) 상태일 때 완료/실패를 **push로** 받기 위한 구독. §4 참조.

---

## 3. currentState — 상태 정의와 전이

### 3.1 값 (8종)
| currentState | 의미 | 종류 |
|---|---|---|
| `NOT_REQUESTED` | 아직 제안을 요청한 적 없음 | 초기 |
| `GENERATING` | Agent가 제안 생성 중 | 진행 중 |
| `PROPOSAL_READY` | 제안 완료, 사용자 검토/확정 대기 | **사용자 액션 필요** |
| `CONFIRMED_PENDING_EXECUTION` | 확정됨, 실행 준비/시작 대기 | 진행 중 |
| `EXECUTING` | 확정 파라미터로 수급모델 실행 중 | 진행 중 |
| `EXECUTED_CONFIRMED` | 실행 완료 (최종 성공) | 종료(성공) |
| `PROPOSAL_FAILED` | 제안 생성 단계 실패 | 종료(실패) |
| `EXECUTION_FAILED` | 확정 이후 실행 단계 실패 | 종료(실패) |

### 3.2 전이 다이어그램
```
NOT_REQUESTED
   │ POST (제안 요청)
   ▼
GENERATING ───────────────► PROPOSAL_FAILED  (제안 생성 실패)
   │ Agent 완료(SSE)
   ▼
PROPOSAL_READY
   │ 사용자 확정
   ▼
CONFIRMED_PENDING_EXECUTION ─► EXECUTION_FAILED  (실행 준비 실패)
   │ 실행 시작
   ▼
EXECUTING ────────────────────► EXECUTION_FAILED  (실행 실패)
   │ 실행 완료
   ▼
EXECUTED_CONFIRMED  (종료)
```

- 실패 종료 상태(`PROPOSAL_FAILED`, `EXECUTION_FAILED`)에서는 **POST 재요청**으로 새 사이클 시작 가능.
- 진행 중 상태(`GENERATING`, `CONFIRMED_PENDING_EXECUTION`, `EXECUTING`)에서 POST하면 `409`(이미 진행 중).

### 3.3 상태별 필드 신뢰 규칙
| currentState | parameterSource | parameters | case/run 필드 | failureReason | 화면 권장 |
|---|---|---|---|---|---|
| `NOT_REQUESTED` | `NONE` | `[]` | null | null | "제안 요청" 버튼 |
| `GENERATING` | `NONE` | `[]` | null | null | 로딩/진행 표시 + SSE 구독 |
| `PROPOSAL_READY` | `PROPOSAL_PARAMETERS` | 제안값 | null | null | 파라미터 검토 + 확정 버튼 |
| `CONFIRMED_PENDING_EXECUTION` | `CONFIRMATION_PARAMETERS` | 확정값 | null | null | 실행 대기 표시 |
| `EXECUTING` | `CONFIRMATION_PARAMETERS` | 확정값 | `RUNNING` | null | 실행 진행 표시 |
| `EXECUTED_CONFIRMED` | `CONFIRMATION_PARAMETERS` | 확정값 | `COMPLETED` | null | 완료 표시 |
| `PROPOSAL_FAILED` | `NONE` | `[]` | null | 채워짐 | 실패 메시지 + 재요청 |
| `EXECUTION_FAILED` | `CONFIRMATION_PARAMETERS` | 확정값 | `FAILED`(상황에 따라) | 채워짐 | 실패 메시지 + 재요청 |

> `parameterSource == NONE`이면 `parameters`는 항상 빈 배열(`[]`).
> `PROPOSAL_PARAMETERS`(검토 대상 제안값)와 `CONFIRMATION_PARAMETERS`(확정된 값)를 UI에서 구분하세요.

---

## 4. SSE 계약

- 구독: `GET /model-param-proposals/stream` (`Accept: text/event-stream`), scenario 단위.
- **이벤트명**: `model-parameter-proposal`
- **페이로드**:
  ```json
  { "scenarioId": 309, "modelParamProposalId": 10001, "status": "PROPOSED" }
  ```
  - `status`: `PROPOSED`(제안 완료) 또는 `FAILED`(실패). 실패 중 요청 자체 실패면 `modelParamProposalId`가 `null`일 수 있음.
- **push되는 신호**: 완료(`PROPOSED`)와 실패(`FAILED`) **종단 신호만** 옵니다.
  진행 중(`PROCESSING`)이나 확정 이후(`CONFIRMED`)는 SSE로 오지 않습니다.

권장 패턴:
1. 팝업 진입 → GET(C)로 `currentState` 확인 후 화면 그림.
2. `GENERATING`이면 SSE(E) 구독.
3. SSE 이벤트 수신 → **다시 GET(C)** 로 최신 `currentState`·`parameters` 재조회해 갱신.
   (SSE 페이로드는 "갱신하라"는 트리거로만 쓰고, 실제 표시 데이터는 GET 응답을 신뢰)

---

## 5. 에러 / 상태 코드

| 상황 | HTTP | 비고 |
|---|---|---|
| 진행 중 제안/실행 존재 상태에서 재요청 | `409` | `RSC_ALREADY_EXISTS` |
| `FILE_UPLOAD` 모드에서 제안 API 호출 | `409` | `BIZ_RULE_VIOLATION` |
| 폐기(DISCARDED) 시나리오 요청 | `400` | `BIZ_RULE_VIOLATION` |
| 시나리오/제안 리소스 없음 | `404` | `RSC_NOT_FOUND` |
| Agent 통신 타임아웃/실패 | `5xx` | `API_TIMEOUT` 등. 타임아웃 시 요청은 접수 상태 유지될 수 있음 |
| 데이터 정합성 충돌 | `409` | `BIZ_RULE_VIOLATION` (내부 상태 불일치) |

> 타임아웃(`API_TIMEOUT`)으로 POST가 실패해도 BE가 요청을 `REQUESTING`으로 유지하는 경우가 있어,
> **재시도 전 GET(C)로 현재 상태를 먼저 확인**하는 것이 안전합니다.

---

## 6. FAQ / 자주 헷갈리는 점

- **Q. POST 응답 오면 제안 완료?** — 아니요. 접수(생성 시작)일 뿐. 완료는 SSE + GET으로 확인.
- **Q. 폴링 vs SSE?** — 기본은 SSE. SSE 연결이 어려운 환경이면 GET(C) 폴링으로 대체 가능(상태는 GET이 항상 정답).
- **Q. `parameters`를 어디서 그리나?** — 상태별 `parameterSource`로 판단. `NONE`이면 표시할 파라미터 없음.
- **Q. 재요청은 언제 되나?** — 종료(성공/실패) 상태에서만. 진행 중이면 `409`.
- **Q. 제안값 vs 확정값?** — `PROPOSAL_READY`까지는 제안값(`PROPOSAL_PARAMETERS`), 확정 이후는 확정값(`CONFIRMATION_PARAMETERS`).
- **Q. `caseId`/`runId`는 언제 생기나?** — 확정→실행 단계 진입 후. 그 전에는 `null`.

---

## 7. (참고) 내부 상태 → currentState 도출 우선순위

FE가 알 필요는 없지만 문의 대응/디버깅용.

1. preparation row 없음 → `NOT_REQUESTED`
2. preparation `REQUEST_FAILED` → `PROPOSAL_FAILED`
3. Agent proposal `FAILED` → `PROPOSAL_FAILED`
4. **caseId 연결됨** → 최신 Run 상태로: `RUNNING`→`EXECUTING`, `COMPLETED`→`EXECUTED_CONFIRMED`, `FAILED`→`EXECUTION_FAILED`
5. preparation `EXECUTION_FAILED`(caseId 없음) → `EXECUTION_FAILED`
6. preparation `CONFIRMING`/`PREPARING_EXECUTION` 또는 Agent `CONFIRMED` → `CONFIRMED_PENDING_EXECUTION`
7. preparation `REQUESTING` 또는 Agent proposal 없음/`PROCESSING` → `GENERATING`
8. Agent `PROPOSED` → `PROPOSAL_READY`

내부 enum:
- `ModelExecutionPreparationStatus`: `REQUESTING, PROPOSAL_PENDING, REQUEST_FAILED, CONFIRMING, PREPARING_EXECUTION, EXECUTING, EXECUTION_FAILED`
- `AgentModelParamProposalStatus`: `PROCESSING, PROPOSED, CONFIRMED, FAILED`
- `ModelExecutionStatus`(Run): `RUNNING, COMPLETED, FAILED`
- `CurrentModelParamProposalState`(화면용): 위 §3.1의 8종
