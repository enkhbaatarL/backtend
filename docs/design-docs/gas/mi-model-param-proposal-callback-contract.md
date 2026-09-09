# MI 수급모델 파라미터 제안 — Agent 콜백 계약 (Agent 전달용)

## Document Control

- status: Review
- canonical for: Reference only
- owner: BE / Agent
- created at: 2026-07-07
- last synced: 2026-09-03
- direction: Agent → Backend
- source documents: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` §5.4/§13
- depends on: `docs/design-docs/gas/mi-model-parameter-proposal-design.md` §3.2/§3.5/§11
- supersedes: 이 문서의 과거 “시나리오당 1회, scenarioId가 요청을 유일 식별” 설명
- open questions: 즉시 `-202` 거절에는 callback이 없다. 다른 요청의 지연 callback 경계는 canonical spec §13 참조. 후속 생성 `status=error` 일반 실패 저장 결정은 callback 입력/식별 계약을 변경하지 않는다.
- confirmed evidence: write-then-callback 순서·wrapper `data` 셰이프는 2026-07-07 Agent 합의다. 2026-08-27 신규 거절 분기에 대한 검증 완료를 뜻하지 않는다.
- 이 문서는 Feature Spec의 전달용 요약이며 별도 API 계약 원본이 아니다. 이번 수정은 설명/참조 정합화이며 callback path/인증/request/response를 새로 변경하지 않는다.

## 0. 컨벤션 근거 (중요)

- 본 기능 Agent 담당자의 API 규약 1.2.1(JSON key camelCase), 1.2.2(response wrapper `{status, message, data}`)를 따른다.
- Backend의 기존 시나리오 설계 콜백(`ScenarioSignpostsCallbackReq` 등)은 flat(`{result, reason, ...}`) 형태지만, 이는 담당 개발자별 컨벤션 차이다. **이 파라미터 제안 콜백 API는 위 Agent 담당자 규약(wrapper + camelCase)에 맞춘다.** (다른 콜백 API에는 영향 없음)

---

## 1. 개요

Agent가 파라미터 제안 계산을 마치면(`PROPOSED`) 또는 실패하면(`FAILED`) Backend 콜백 엔드포인트를 호출한다.

**중요 — 콜백은 "완료 신호"다.** Backend는 콜백 body의 제안 내용을 저장·신뢰하지 않는다. proposal ID를 확인할 수 있으면 **Agent proposal 테이블을 직접 재조회**한다. ID 없는 실패 신호는 기존 요청 실패 처리로 수렴한다.

- Agent는 **proposal 테이블에 결과를 commit한 뒤** 콜백을 발행해야 한다(write-then-callback). 같은 요청의 HTTP 응답/콜백 순서 역전은 Backend 재조회와 조건부 상태 갱신으로 처리한다.
- 현행 payload에 correlation ID / event ID / hash를 추가하지 않는다. 단, 요청이 종료된 뒤 새 요청이 생기는 경우 과거 callback을 식별할 수 있는지는 별도 확인이 필요하다.
- 같은 요청의 중복·지연 신호는 재조회/조건부 갱신으로 처리한다. 정상/no-op은 `200`이며, 이 응답 정책이 서로 다른 요청 사이의 상관관계를 보장하지는 않는다.

## 2. 엔드포인트

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/callback` |
| Content-Type | `application/json` |

- `{scenarioId}`는 현재 제안 요청 후보를 찾는 **scope key(조회 범위)**다. 실패 후 같은 시나리오에 새 요청을 만들 수 있으므로 개별 요청을 유일하게 식별하지 않는다. 후보는 Backend `REQUESTING`/`PROPOSAL_PENDING`이며 종료 이력을 선택하지 않는다.
- callback base URL/path는 Backend가 결정해 전달한다. Agent는 전달받은 값을 `callbackBaseUrl + callbackPath` 형태의 배포 운영 설정으로 등록·관리한다.

## 3. 인증

- Header: `Authorization: Bearer {JWT}`
- 검증: Backend 표준 Resource Server JWT 체인(HS256 self-issued) + `Role.SYSTEM` 권한. 기존 시나리오 설계 콜백과 동일한 방식이다.
- Agent는 이미 전달받은 **Backend 토큰 발급 API + 발급 인풋 2종**으로 JWT를 자체 발급·갱신한다.
- 토큰 없음/유효하지 않음 → `401`, 토큰은 유효하나 권한 부족 → `403`.

## 4. Request Body

Notion `{status, message, data}` wrapper를 그대로 사용한다.

### 성공 (`PROPOSED`)

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

### 실패 (`FAILED`)

```json
{
  "status": "error",
  "message": "insufficient dimension mapping for event 1378",
  "data": {
    "modelParamProposalId": 10001,
    "scenarioId": 309,
    "scenarioGroupId": 61,
    "eventId": 1378,
    "status": "FAILED"
  }
}
```

### 필드 규칙

| 필드 | 필수 | 규칙 |
|---|---|---|
| `status` (wrapper) | O | `success` \| `error`. `data.status=PROPOSED`면 `success`, `FAILED`면 `error`. |
| `message` | O | 실패 시 사유 문자열. Backend가 failure reason 입력으로 사용할 수 있다. |
| `data.modelParamProposalId` | △ | 성공 시 채운다. 실패 시 규약 1.2.2대로 생략 가능(path `scenarioId`로 대체). 채우면 교차검증. |
| `data.scenarioId` | △ | 성공 시 채운다. path의 `{scenarioId}`와 일치해야 함. |
| `data.scenarioGroupId` | △ | 성공 시 채운다. 교차검증용. |
| `data.eventId` | △ | 기존 Agent wrapper 호환을 위해 수신하지만 Backend 교차검증에는 사용하지 않는다. |
| `data.status` | △ | 성공 시 `PROPOSED`. 실패 시 `FAILED`(생략되면 wrapper `status=error`로 판정). |
| `data.proposalParameters` | X | 실어 보내면 Feature Spec의 flat parameter array 형식이다. Backend는 **무시**하고 Agent proposal 테이블을 재조회하므로 넣지 않아도 된다. |

`O`=필수, `△`=성공 시 채움·실패 시 생략 가능, `X`=선택. path는 scope를 지정하며, `data` 또는 기존 preparation에서 proposal ID를 확인해 재조회한다. 둘 다 ID가 없는 성공 신호는 처리할 수 없다.

- **correlation은 path `scenarioId`의 현재 후보와 확인 가능한 proposal ID로 검증한다.** 활성 후보가 없으면 late/no-op이며, 이미 저장된 ID와 다르면 덮어쓰지 않는다. 새 후보의 ID가 null인 구간이나 ID 없는 신호에서 과거 요청을 구별하는 한계는 canonical spec §13을 따른다.
- 규약 1.2.2대로 **실패(`error`) 콜백의 `data`는 `null`/빈 object일 수 있다**. `message`는 필수이며 숫자 오류 코드는 필요하지 않다. Agent ID도 없으면 기존 `REQUEST_FAILED` 처리이며, 문구만 보고 전용 상태를 추정하지 않는다. NO_CHANGES는 과거 읽기 호환만 유지한다. 이 입력 허용이 재요청 후 과거 신호의 안전성 보장은 아니다.
- `data`가 채워진 경우, Backend는 식별자 일치와 `status`만 확인한다. 제안값 상세(`modelTarget`, `values` 등)는 검증하지 않는다(재조회로 대체).
- `wrapper.status`와 `data.status`가 모두 존재하면서 어긋나면(예: `success` + `FAILED`) `400`으로 거절한다.

## 5. Response

| 상황 | HTTP | Body |
|---|---|---|
| 정상 수신(신규/중복/지연 모두) | `200` | `BaseResponse<Void>` (`SUC-000`) |
| 인증 실패 | `401` | 공통 auth error |
| 권한 부족 | `403` | 공통 permission error |
| 활성 Backend 후보 없음 | `200` | late/no-op (`SUC-000`) |
| 확인된 `modelParamProposalId`의 Agent row 없음 | `404` | `RSC_NOT_FOUND` |
| 식별자 snapshot 불일치 | `409` | `BIZ_RULE_VIOLATION` |
| wrapper/status/식별자 field invalid | `400` | `REQ_INVALID_PARAMETER` |

- 같은 요청의 정상 재전송·지연 신호는 `200`으로 처리한다. 후보가 있으면 재조회/상태 반영이 가능하고, 이미 종료됐거나 후보가 없으면 no-op이다. 식별자 불일치 등은 위 오류 규칙을 따른다.
- Agent는 `2xx`면 성공으로 간주한다. 별도 disposition 해석 불필요.

## 6. Agent 측 확정 사항

1. **proposal 테이블 commit 이후 콜백 발행(write-then-callback)** 순서 준수 — 이 계약의 정합성 전제. **(2026-07-07 Agent 확정)**
2. 위 `data` 필드 셰이프가 실제 발행 형태와 일치함 — Agent 규약 1.2.2 wrapper와 일치 **확정(2026-07-07)**. 성공 시 식별자 4종+`data.status` 채움, 실패 시 `data`(null/빈 object) 생략은 규약대로 허용된다.
3. retry 최대 횟수/backoff는 Agent 운영 기준을 따른다. 서로 다른 요청 사이의 지연 재전송까지 안전하다고 단정하지 않으며, 신규 거절 분기는 아래 확인 사항을 따른다.

**2026-08-27 사용자 확인:** `-202`로 거절한 요청에는 callback이 없다. 상세 오류 코드 정의는 spec §5.2.1, 확인 근거와 잔여 경계는 §13이 canonical이다. 실제 Agent 소스/운영 이력을 직접 검증한 것은 아니다. 이번에 callback 필드를 추가하지 않으며, 향후 이 거절에도 callback을 발행하도록 바뀌면 요청별 식별 계약을 먼저 보완한다.

## 7. 참고 (Backend 내부 — Agent 구현 무관)

- Backend는 콜백 수신 → 식별자 교차검증 → Agent proposal 테이블 재조회 → orchestration 행 monotonic update → commit 이후 SSE 발행 순으로 처리한다.
- FE 팝업은 SSE 신호/생성 POST 오류 후 scenario 기준 `GET .../model-param-proposals`(A4a)로 재조회한다. proposal ID 없는 실패도 조회할 수 있으며, 신규 실패 의미/화면 처리는 spec §5.2.1의 FE 전달문을 따른다.
