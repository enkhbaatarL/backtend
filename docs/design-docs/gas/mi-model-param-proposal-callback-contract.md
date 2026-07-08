# MI 수급모델 파라미터 제안 — Agent 콜백 계약 (Agent 전달용)

- date: 2026-07-07
- direction: Agent → Backend
- status: Confirmed (write-then-callback 순서·wrapper `data` 셰이프 Agent 확정, 2026-07-07)
- canonical source: `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` 5.4
- 이 문서는 위 Feature Spec 5.4에서 Agent가 구현에 필요한 부분만 발췌한 전달용 요약이다. 상충 시 Feature Spec이 우선한다.

## 0. 컨벤션 근거 (중요)

- 본 기능 Agent 담당자의 API 규약 1.2.1(JSON key camelCase), 1.2.2(response wrapper `{status, message, data}`)를 따른다.
- Backend의 기존 시나리오 설계 콜백(`ScenarioSignpostsCallbackReq` 등)은 flat(`{result, reason, ...}`) 형태지만, 이는 담당 개발자별 컨벤션 차이다. **이 파라미터 제안 콜백 API는 위 Agent 담당자 규약(wrapper + camelCase)에 맞춘다.** (다른 콜백 API에는 영향 없음)

---

## 1. 개요

Agent가 파라미터 제안 계산을 마치면(`PROPOSED`) 또는 실패하면(`FAILED`) Backend 콜백 엔드포인트를 호출한다.

**중요 — 콜백은 "완료 신호"다.** Backend는 콜백 body의 제안 내용을 저장·신뢰하지 않는다. 신호를 받으면 Backend가 **Agent proposal 테이블을 직접 재조회**해 결과를 확정한다. 따라서:

- Agent는 **proposal 테이블에 결과를 commit한 뒤** 콜백을 발행해야 한다(write-then-callback). 이 순서만 지키면 HTTP 응답/콜백 도착 순서 race는 문제되지 않는다.
- 콜백 payload에 correlation ID / event ID / hash 같은 추가 식별자는 필요 없다.
- 중복·지연 콜백은 Backend가 재조회로 수렴시키므로 멱등이다. Agent는 특별한 ack 해석 없이 `200`만 확인하면 된다.

## 2. 엔드포인트

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals/callback` |
| Content-Type | `application/json` |

- `{scenarioId}` = 대상 리소스 식별자(proposal 요청 시 이미 Agent에 전달됨). 파라미터 제안은 **시나리오당 1회**뿐이라 `scenarioId`가 proposal을 유일하게 특정한다(별도 body correlation 불필요).
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
| `data.eventId` | △ | 성공 시 채운다. 교차검증용. |
| `data.status` | △ | 성공 시 `PROPOSED`. 실패 시 `FAILED`(생략되면 wrapper `status=error`로 판정). |
| `data.proposalParameters` | X | 실어 보내면 Feature Spec의 flat parameter array 형식이다. Backend는 **무시**하고 Agent proposal 테이블을 재조회하므로 넣지 않아도 된다. |

`O`=필수, `△`=성공 시 권장·실패 시 생략 가능, `X`=선택. correlation은 path `scenarioId`가 담당하므로 `data` 식별자는 correlation 필수 조건이 아니다.

- **correlation은 path의 `scenarioId`로 한다.** 파라미터 제안은 **시나리오당 1회**만 가능하므로(사용자별 반복 아님) `scenarioId`가 proposal을 유일하게 특정한다. `data`의 식별자들은 교차검증용이며 correlation 필수 조건이 아니다.
- 따라서 규약 1.2.2대로 **실패(`error`) 콜백의 `data`가 `null`/빈 object여도 무방**하다. 이 경우 wrapper `status=error`(+ 있으면 `message`)만으로 실패를 처리하고, 결과는 path `scenarioId`로 Agent proposal 테이블을 재조회해 확정한다.
- `data`가 채워진 경우, Backend는 식별자 일치와 `status`만 확인한다. 제안값 상세(`modelTarget`, `values` 등)는 검증하지 않는다(재조회로 대체).
- `wrapper.status`와 `data.status`가 모두 존재하면서 어긋나면(예: `success` + `FAILED`) `400`으로 거절한다.

## 5. Response

| 상황 | HTTP | Body |
|---|---|---|
| 정상 수신(신규/중복/지연 모두) | `200` | `BaseResponse<Void>` (`SUC-000`) |
| 인증 실패 | `401` | 공통 auth error |
| 권한 부족 | `403` | 공통 permission error |
| `modelParamProposalId`에 해당하는 Backend request 없음 | `404` | `RSC_NOT_FOUND` |
| 식별자 snapshot 불일치 | `409` | `BIZ_RULE_VIOLATION` |
| wrapper/status/식별자 field invalid | `400` | `REQ_INVALID_PARAMETER` |

- 동일 콜백 재전송·timeout 후 지연 콜백은 모두 `200`(Backend가 재조회 후 monotonic no-op).
- Agent는 `2xx`면 성공으로 간주한다. 별도 disposition 해석 불필요.

## 6. Agent 측 확정 사항

1. **proposal 테이블 commit 이후 콜백 발행(write-then-callback)** 순서 준수 — 이 계약의 정합성 전제. **(2026-07-07 Agent 확정)**
2. 위 `data` 필드 셰이프가 실제 발행 형태와 일치함 — Agent 규약 1.2.2 wrapper와 일치 **확정(2026-07-07)**. 성공 시 식별자 4종+`data.status` 채움, 실패 시 `data`(null/빈 object) 생략은 규약대로 허용된다.
3. (선택) retry 정책 — network error/`5xx` 시 동일 body로 재전송해도 무방하다(Backend 멱등). 최대 횟수/backoff는 Agent 운영 기준을 따른다.

## 7. 참고 (Backend 내부 — Agent 구현 무관)

- Backend는 콜백 수신 → 식별자 교차검증 → Agent proposal 테이블 재조회 → orchestration 행 monotonic update → commit 이후 SSE 발행 순으로 처리한다.
- FE는 SSE 신호(또는 polling)를 받은 뒤 `GET .../model-param-proposals/{id}`로 결과를 재조회한다.
