# Event 웹 검증 기반 업데이트 API

## UI → Backend

`POST /api/v1/mi/next-gen/events/{miEventId}/verification-updates`

로그인 토큰(`Authorization: Bearer ...`)과 `Content-Type: application/json`을 사용한다.
`dedicated` 프로파일에서는 노출하지 않는다. 기존 시나리오 변경 API와 같은 회사·역할 권한
(SKGAS USER, 타사 접근 권한 ADMIN 및 기존 운영자 정책)을 적용한다.
Event는 공유 데이터이며 별도 소유자 ACL은 없다. 게시·미삭제 여부를 확인한 뒤 Agent를 호출한다.

```json
{
  "expectedUpdatedAt": "2026-11-06T09:00:00.123456"
}
```

- `miEventId`: 양의 Long 정수, 한 요청에 Event 하나.
- `expectedUpdatedAt`: 선택적 ISO 날짜·시간 문자열 또는 null. 이전 검증의 `sourceUpdatedAt` 원문을 전달한다.
  시간대 없는 값도 허용하며 소수 초 정밀도·시간대를 변환하지 않는다.
- 본문은 필수이며 수정 시각을 생략하려면 `{}`를 보낸다.
- **`actorUsrLognId`는 UI에서 받지 않는다.** 검증된 로그인 JWT의 subject(로그인 ID)를 Backend가 설정한다.
  사용자 ID, 검증 결과, 변경 내용 등 추가 필드는 422로 거절한다.
  로그인 사용자 claim(`userNo`)이 없는 client_credentials 토큰은 403으로 거절한다.

## Backend → Agent

같은 시나리오 Agent의 `POST /events/{miEventId}/verification-updates`를 한 번 호출한다.
별도의 `/events/verifications` 선행 호출은 하지 않는다.

```json
{
  "actorUsrLognId": "10603",
  "expectedUpdatedAt": "2026-11-06T09:00:00.123456"
}
```

Agent가 새 웹 검증 → 수정안 생성 → 재검증 → 동시 변경 검사 → 변경 전 Snapshot 저장 → Event 수정을 처리한다.
Snapshot과 Event 저장은 Agent의 동일 트랜잭션에서 수행한다. Backend에서 Snapshot을 중복 생성하지 않으며,
긴 Agent 호출 동안 DB 트랜잭션을 유지하지 않는다. 시각을 생략해도 Agent의 동시 변경 검사는 수행된다.

## UI 응답

Agent의 성공 응답은 래퍼 없는 JSON이다. Backend는 기존 API와 동일하게 **BaseResponse.data**에 담는다.
HTTP 200 또는 최상위 `status: true`는 요청 처리 완료이며 저장 성공을 뜻하지 않는다.

| data.updateStatus | 의미 | UI 처리 |
| --- | --- | --- |
| updated | 최초 FAIL, 수정안 재검증 PASS, Snapshot 및 Event 저장 완료 | Event 상세 재조회 |
| unchanged | 최초 PASS 등 변경 없음 | data.message 표시 |
| skipped | WARNING·근거 부족·수정안 생성 불가·재검증 미통과 | data.message와 검증 결과 표시 |

**WARNING도 API를 호출할 수 있지만 현재 Agent 계약에서는 저장하지 않는다.**
저장하려면 최초 검증 FAIL과 수정안 재검증 PASS가 필요하다. Backend에서 판정을 변경하거나 강제로 저장하지 않는다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "miEventId": 5,
    "checkedAt": "2026-11-06T01:00:00Z",
    "updateStatus": "skipped",
    "message": "검증 근거가 충분하지 않아 업데이트하지 않았습니다.",
    "verification": {
      "miEventId": 5,
      "title": "Event 제목",
      "sourceUpdatedAt": "2026-11-06T09:00:00.123456",
      "status": "warning",
      "reasonCode": "no_evidence",
      "review": "비교 가능한 웹 근거가 부족합니다.",
      "links": [],
      "checks": []
    },
    "postVerification": null,
    "miEventSnapshotId": null,
    "snapshotVersionNo": null,
    "previousMiEventSnapshotId": null,
    "updatedAt": "2026-11-06T09:00:00.123456",
    "changedFields": [],
    "changes": []
  }
}
```

`verification`은 변경 전 결과이고 `postVerification`은 수정안 결과다. `updated` 응답에서 각각 `fail`, `pass`가 정상이다.
두 객체의 구조는 [기존 검증 API](event-verifications.md)와 같다.
`miEventSnapshotId`는 **변경 직전 상태**를 저장한 Snapshot이다. `snapshotVersionNo`는 신규 버전,
`previousMiEventSnapshotId`는 직전 Snapshot이며 최초이면 null이다.
`updatedAt`과 검증의 `sourceUpdatedAt`은 문자열 원문을 보존한다.

`changedFields`는 실제 변경된 DB 컬럼 배열이다. `changes`는 `{field, before, after, reason, sourceUrls}` 배열이며
`before`/`after`의 JSON 타입(문자열·배열·객체·null)을 유지한다. 변경 가능한 컬럼은 아래 여섯 개다.

- `one_line_summary`, `search_text`, `key_bullets_json`
- `detail_analysis`, `numeric_facts_md`, `institution_view_synthesis`

최초 Snapshot의 `changed_fields_json=[]` 규칙은 Agent가 적용한다. 최초 변경도 응답 `changedFields`에는 포함된다.
Snapshot의 `update_trigger_cd`는 `LLM_VALIDATION_UPDATE`다.

## 오류와 재시도

Agent 오류는 Backend 공통 오류 응답(`status: false`, `code`, `message`, `data: null`)으로 변환한다.
Agent의 `data.error_name`과 422 `detail` 배열은 그대로 노출하지 않는다. UI는 아래 HTTP/code로 분기한다.

| HTTP / Backend code | 의미 및 처리 |
| --- | --- |
| 401 / 403 | 로그인 또는 수정 권한 확인 |
| 404 / RSC-001 | Event 없음·삭제·미게시 상태 재확인 |
| 409 / BIZ-007 | EVENT_UPDATE_STALE. 최신 Event 재조회 후 새 수정 시각으로 요청 |
| 422 / REQ-003 | Event ID·본문 형식 확인 |
| 500 / API-005 | EVENT_UPDATE_SCHEMA_ERROR. Event/Snapshot 스키마 확인 |
| 500 / API-004 | LLM 응답·수정안 등 Agent 처리 실패 |
| 502 / API-003 또는 API-004 | 연결·응답 수신 실패 또는 잘못된 Agent 응답 |
| 503 / API-003 | Agent 서비스 사용 불가 |
| 504 / API-002 | LLM_RESPONSE_TIMEOUT 또는 Backend 응답 대기시간 초과 |

요청 ID 기반 멱등 처리는 없다. HTTP 자동 재시도와 redirect 처리는 비활성화했다.
**연결 종료·시간 초과·응답 유실·잘못된 응답이 발생해도 Agent가 이미 저장했을 수 있다.**
자동 재호출하지 말고 Event 상세와 Snapshot 목록을 먼저 확인한다. UI도 처리 중 중복 요청을 막는다.

## 설정 및 운영

```properties
EVENT_VERIFICATION_AGENT_URL=http://localhost:8000
EVENT_VERIFICATION_AGENT_CONNECTION_TIMEOUT=3s
EVENT_VERIFICATION_AGENT_READ_TIMEOUT=210s
EVENT_VERIFICATION_AGENT_UPDATE_READ_TIMEOUT=420s
```

주소는 기존 검증 API와 공유하며 미설정 시 `api.scenario-design-agent.url`을 사용한다.
Gateway prefix 및 trailing slash를 지원한다. UI 토큰은 Agent에 전달하지 않는다.
업데이트의 응답 대기시간은 별도 설정으로 기본 420초이며 기존 검증의 210초에는 영향을 주지 않는다.
Agent의 외부 분석 기본 제한 360초에 DB 처리 시간을 더해 조정한다. UI·gateway는 **480초 이상**을 시작점으로
실제 지연에 맞춰 설정한다. 이 저장소에서 외부 gateway의 timeout은 변경하지 않았다.
동기 JSON API이며 callback/SSE를 사용하지 않는다.

Qdrant 추천 인덱스는 이 API로 갱신하지 않는다. 검색 반영은 기존 export/index 갱신 절차를 사용한다.

## 검증

```powershell
.\gradlew.bat "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NONE" test -PforceTest --tests '*EventVerification*Test'
```

테스트는 MVC·기존 인증 체인·권한 검사와 실제 HTTP 클라이언트/모의 Agent를 사용한다.
로그인 사용자 전달, 입력 위조 거절, 시각 원문, 세 처리 결과, Snapshot/diff, 동시 변경 오류,
schema 오류, 잘못된 응답, 타임아웃과 자동 재시도 방지를 확인한다.
실제 웹 검색·LLM·Agent DB 트랜잭션 검증은 별도 통합 환경에서 수행한다.
