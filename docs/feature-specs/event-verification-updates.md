# Event 웹 검증 기반 업데이트 API

## UI → Backend

`POST /api/v1/mi/next-gen/events/{miEventId}/verification-updates`

로그인 토큰(`Authorization: Bearer ...`)과 `Content-Type: application/json`을 사용한다.
`dedicated` 프로파일에서는 노출하지 않는다. 기존 시나리오 변경 API와 같은 회사·역할 권한
(SKGAS USER, 타사 접근 권한 ADMIN 및 기존 운영자 정책)을 적용한다.
Event는 공유 데이터이며 별도 소유자 ACL은 없다. 게시·미삭제 여부를 확인한 뒤 Agent를 호출한다.

```json
{
  "verificationId": "12345678-1234-4234-8234-123456789abc",
  "expectedUpdatedAt": "2026-11-06T09:00:00.123456"
}
```

- `miEventId`: 양의 Long 정수, 한 요청에 Event 하나.
- `verificationId`: **필수 UUID 문자열**. 앞선 검증 API에서 해당 Event에 반환된, 화면에 표시한 결과의 ID.
- `expectedUpdatedAt`: 선택적 ISO 날짜·시간 문자열 또는 null. 이전 검증의 `sourceUpdatedAt` 원문을 전달한다.
  시간대 없는 값도 허용하며 소수 초 정밀도·시간대를 변환하지 않는다.
- 본문은 필수이며 수정 시각을 생략해도 `{"verificationId":"12345678-1234-4234-8234-123456789abc"}`를 보낸다. `{}`는 422다.
- **`actorUsrLognId`는 UI에서 받지 않는다.** 검증된 로그인 JWT의 subject(로그인 ID)를 Backend가 설정한다.
  사용자 ID, 검증 결과, 변경 내용 등 추가 필드는 422로 거절한다.
  로그인 사용자 claim(`userNo`)이 없는 client_credentials 토큰은 403으로 거절한다.

## Backend → Agent

같은 시나리오 Agent의 `POST /events/{miEventId}/verification-updates`를 한 번 호출한다.
**앞선 `/events/verifications` 결과가 필요하다.** 유효한 ID가 없거나 만료·변경 오류가 발생해도 Backend가 자동으로 새 검증을 호출하지 않는다.

```json
{
  "verificationId": "12345678-1234-4234-8234-123456789abc",
  "actorUsrLognId": "10603",
  "expectedUpdatedAt": "2026-11-06T09:00:00.123456"
}
```

Agent가 검증 ID로 저장 결과·근거를 조회 → Event 연결·적용 여부·만료·변경 검사 → 수정안 생성 → 같은 근거로 재검증
→ 저장 조건·동시 변경·만료 재확인 → 변경 전 Snapshot 저장 → Event 수정 → 검증 행에 적용 Snapshot ID 기록을 처리한다.
마지막 세 저장은 동일 트랜잭션이다. Backend는 별도 검증 테이블이나 Snapshot을 생성하지 않으며 긴 HTTP 호출 동안 DB 트랜잭션을 유지하지 않는다.
업데이트 단계에서는 **새 웹 검색을 하지 않는다.** 저장 이후 다시 검색·검증하지도 않는다.
`checkedAt`은 원본 검증 기준 시각이며 `postVerification`은 저장 직전 수정안의 검증 결과다.
`expectedUpdatedAt`을 생략해도 저장된 Event 상태 해시로 변경을 검사한다.

원본 검증은 기본 `checkedAt + 30분`까지 재사용할 수 있다. 실제 반환된 `expiresAt`을 사용하며 응답 수신 시점부터 30분을 계산하지 않는다.
Agent는 요청 시작과 저장 직전에 만료를 검사하므로 처리 중 만료도 409가 된다. 만료와 Event 변경은 독립적인 조건이다.

## UI 응답

Agent의 성공 응답은 래퍼 없는 JSON이다. Backend는 기존 API와 동일하게 **BaseResponse.data**에 담는다.
HTTP 200 또는 최상위 `status: true`는 요청 처리 완료이며 저장 성공을 뜻하지 않는다.

| data.updateStatus | 의미 | UI 처리 |
| --- | --- | --- |
| updated | Agent가 저장 조건을 충족하여 Snapshot 및 Event 저장 완료 | Event 상세·Snapshot 재조회 |
| unchanged | 변경할 내용 없음 | data.message 표시 |
| skipped | 근거·수정안 생성·검증·저장 조건 등으로 수정 생략 | data.message와 검증 상세 표시 |

**공개 검증 등급(`verification.status`, `postVerification.status`)으로 저장 여부를 추론하지 않는다.**
`pass/no_contradiction`과 `skipped`가 함께 반환될 수 있으며, "검증 통과 · 수정 생략"으로 표시한다.
`no_contradiction`은 "명확한 불일치 없음"이며 모든 사실 확인 완료로 표시하지 않는다.
Backend는 `updateStatus`를 그대로 전달하고 Snapshot·버전·실제 변경 내역의 구조를 검증한다.

Agent의 **내부 판단**에서 최초 pass는 unchanged, warning은 skipped이며, 근거가 있는 fail만 수정안을 생성한다.
최초 warning을 수정안 생성 대상으로 확대하는 기능은 아직 없다. 수정안의 모든 항목이 일치/검증 대상 없음이거나,
최신성의 확인 시점 한계만 남고 근거·구체적인 기준 날짜·미확인 범위 명시 등의 조건을 만족하면 저장할 수 있다.
출처 상충·핵심 근거 부족·불일치가 남으면 저장을 보류한다. 이 조건은 Agent가 판단하며 Backend가 재구현하지 않는다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "miEventId": 5,
    "verificationId": "12345678-1234-4234-8234-123456789abc",
    "checkedAt": "2026-11-06T01:00:00Z",
    "updateStatus": "skipped",
    "message": "검증 근거가 충분하지 않아 업데이트하지 않았습니다.",
    "verification": {
      "miEventId": 5,
      "verificationId": "12345678-1234-4234-8234-123456789abc",
      "expiresAt": "2026-11-06T01:30:00Z",
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

`verification`은 변경 전 DB Event의 결과이고 `postVerification`은 **저장 전 수정안** 검증 결과다.
최상위 `verificationId`는 요청한 원본 검증 ID다. `verification`에도 같은 ID와 원본 `expiresAt`이 포함된다.
`postVerification`은 별도 저장된 원본 검증이 아니므로 그 내부의 `verificationId`, `expiresAt`은 null일 수 있다.
Backend는 요청과 다른 최상위·원본 검증 ID를 반환하는 응답을 502로 거절한다.
조건부 저장 시 `postVerification`에 `pass/no_contradiction`과 최신성 주의가 함께 남을 수 있다.
`checks[].cautionReason`, `qualificationQuote`, `finding`, `verdict`를 보존하고 화면에도 표시한다.
`skipped`의 `postVerification`은 미저장 수정안의 결과이므로 현재 DB Event의 검증 결과로 덮어쓰지 않는다.
`message`는 표시용이며 문자열 비교로 업무 분기하지 않는다. 공개 등급과 저장 상태를 각각 표시한다.
두 객체의 구조는 [기존 검증 API](event-verifications.md)와 같다.
`miEventSnapshotId`는 **변경 직전 상태**를 저장한 Snapshot이다. `snapshotVersionNo`는 신규 버전,
`previousMiEventSnapshotId`는 직전 Snapshot이며 최초이면 null이다.
`updatedAt`과 검증의 `sourceUpdatedAt`은 문자열 원문을 보존한다.
`updated` 이후 Event·Snapshot을 재조회하고 사용한 검증 ID를 적용 완료로 처리한다.
추가 수정은 최신 Event를 다시 검증해 새 ID와 그 검증의 `sourceUpdatedAt` 원문으로 요청한다.
Agent 로그의 `save_verification_status`, `save_post_verification_status`는 내부 등급이며 API 필드가 아니다.
Backend 로그의 `verificationStatus`, `postVerificationStatus`는 공개 등급이다.

`changedFields`는 실제 변경된 DB 컬럼 배열이다. `changes`는 `{field, before, after, reason, sourceUrls}` 배열이며
`before`/`after`의 JSON 타입(문자열·배열·객체·null)을 유지한다. 변경 가능한 컬럼은 아래 여섯 개다.

- `one_line_summary`, `search_text`, `key_bullets_json`
- `detail_analysis`, `numeric_facts_md`, `institution_view_synthesis`

최초 Snapshot의 `changed_fields_json=[]` 규칙은 Agent가 적용한다. 최초 변경도 응답 `changedFields`에는 포함된다.
Snapshot의 `update_trigger_cd`는 `LLM_VALIDATION_UPDATE`다.

## 오류와 재시도

Agent 오류는 Backend 공통 오류 응답(`status: false`, `code`, `message`)으로 변환한다.
알려진 409 충돌은 복구에 필요한 `data`를 제공한다. 다른 오류는 `data: null`이며 422 `detail` 등 내부 원문은 노출하지 않는다.

| HTTP / Backend code | 의미 및 처리 |
| --- | --- |
| 401 / 403 | 로그인 또는 수정 권한 확인 |
| 404 / RSC-001 | Event 없음·삭제·미게시 또는 검증 ID 없음·삭제·다른 Event의 ID. Event와 ID 연결 확인 |
| 409 / BIZ-007 | EVENT_UPDATE_STALE. 최신 Event 재조회 후 재검증하여 새 ID 사용 |
| 409 / BIZ-008 | EVENT_VERIFICATION_EXPIRED. 재검증하여 새 ID 사용 |
| 409 / BIZ-009 | EVENT_VERIFICATION_ALREADY_APPLIED. data의 기존 Snapshot ID 확인 및 Event 재조회 |
| 422 / REQ-003 | Event ID·본문 형식 확인 |
| 500 / API-005 | EVENT_UPDATE_SCHEMA_ERROR. Event/Snapshot 스키마 확인 |
| 500 / API-006 | EVENT_VERIFICATION_INVALID. 저장된 검증 형식 오류 확인 및 재검증 |
| 500 / API-004 | LLM 응답·수정안 등 Agent 처리 실패 |
| 502 / API-003 또는 API-004 | 연결·응답 수신 실패 또는 잘못된 Agent 응답 |
| 503 / API-003 | Agent 서비스 사용 불가 |
| 504 / API-002 | LLM_RESPONSE_TIMEOUT 또는 Backend 응답 대기시간 초과 |

알려진 409의 `data` 키는 Agent 오류 계약과 동일한 **snake_case**다. 정상 응답의 camelCase와 구분한다.
아래는 이미 적용된 검증을 다시 요청한 경우다. 만료·변경 오류도 같은 구조이며 Snapshot ID는 null이다.

```json
{
  "status": false,
  "code": "BIZ-009",
  "message": "이미 적용한 검증 결과입니다. Event와 기존 Snapshot을 확인해 주세요.",
  "data": {
    "error_code": -206,
    "error_name": "EVENT_VERIFICATION_ALREADY_APPLIED",
    "verification_id": "12345678-1234-4234-8234-123456789abc",
    "mi_event_snapshot_id": 123
  }
}
```

Backend는 요청과 일치하는 검증 ID와 양의 Snapshot ID만 전달하며 Agent의 임의 추가 정보는 노출하지 않는다.
알 수 없는 409 또는 잘못된 복구 식별자는 502로 처리한다.

성공 응답 전체를 재전송하는 멱등 방식은 아니지만, **같은 verificationId의 중복 적용은 Agent가 차단**한다.
이미 적용된 ID를 다시 요청하면 409와 기존 Snapshot ID가 반환된다. unchanged/skipped는 적용 완료로 기록하지 않는다.
HTTP 자동 재시도와 redirect 처리는 비활성화했다.
**연결 종료·시간 초과·응답 유실·잘못된 응답이 발생해도 Agent가 이미 저장했을 수 있다.**
자동 재호출하지 말고 Event 상세와 Snapshot 목록을 먼저 확인한다. 재시도할 경우 **같은 verificationId**를 사용한다.
재시도를 위해 자동으로 새 검증을 실행해 새 ID를 발급하지 않는다. UI도 처리 중 중복 요청을 막는다.
unchanged/skipped는 만료 전 같은 ID로 재요청할 수 있으나 같은 근거이므로 결과 변경을 보장하지 않는다.

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
Agent의 검증 저장 테이블과 필수 actor/verificationId 계약이 준비된 버전을 사용해야 한다.
Agent·Backend·Frontend를 함께 배포한다. 기존 verificationId 없는 업데이트 요청은 더 이상 지원하지 않는다.

## 검증

```powershell
.\gradlew.bat "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NONE" test -PforceTest --tests '*EventVerification*Test'
```

테스트는 MVC·기존 인증 체인·권한 검사와 실제 HTTP 클라이언트/모의 Agent를 사용한다.
로그인 사용자 전달, 입력 위조 거절, 시각 원문, 세 처리 결과, Snapshot/diff, 동시 변경 오류,
schema 오류, 잘못된 응답, 타임아웃과 자동 재시도 방지를 확인한다.
추가로 pass/no_contradiction + skipped, 미저장 수정안의 pass, 확인 시점 한계를 포함한 조건부 저장,
주의 사유·발췌 원문 보존과 공개 등급/저장 결과 분리를 검증한다.
검증 요청의 인증 사용자 주입, Event별 검증 ID/만료 시각 전달, 필수 UUID 입력 및 응답 ID 일치,
만료·변경·이미 적용 오류 구분과 기존 Snapshot ID 보존, 새 검색 없는 동일 ID 재요청도 검증한다.
실제 웹 검색·LLM·Agent DB 트랜잭션 검증은 별도 통합 환경에서 수행한다.
