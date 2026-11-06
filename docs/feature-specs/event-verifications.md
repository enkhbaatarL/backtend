# Event 웹 검증 API

## UI → Backend

`POST /api/v1/mi/next-gen/events/verifications`

기존 로그인 토큰(`Authorization: Bearer ...`)과 `Content-Type: application/json`을 사용한다.
`dedicated` 프로파일에서는 노출하지 않는다.

```json
{"miEventIds": [5, 12]}
```

양의 JSON 정수 1~5개만 허용한다. 중복, 문자열 ID, 소수(5.0 포함), boolean, null, 추가 필드는
HTTP **422 / REQ-003**으로 거절한다. 이 정책은 검증 API에만 적용하며 기존 추천 API의 400 정책은 유지한다.

Backend → Agent 요청에는 인증한 사용자의 `actorUsrLognId`를 필수로 추가한다.
UI에서 actor를 입력할 수 없으며, 로그인 사용자 정보가 없는 서비스 토큰은 403으로 거절한다.

```json
{"miEventIds": [5, 12], "actorUsrLognId": "SKGAS001@SKGAS.com"}
```

Agent가 `tmm_mi_mievent_verification_m`에 검증 결과·검색 근거·Event 상태 해시를 저장한다.
이 단계에서는 Event 본문과 Snapshot을 변경하지 않는다. Backend에 별도 검증 저장 테이블을 만들지 않는다.

Event API와 동일한 회사/역할 규칙(SKGAS USER, 타사 접근 권한 ADMIN 및 기존 운영자 접근 정책)을 적용한다.
현재 Event는 해당 권한 사용자에게 공유되는 데이터이며, 사용자별 소유권 ACL은 없다.
Backend는 모든 ID의 PUBLISHED·미삭제 상태를 짧은 읽기 트랜잭션으로 확인한다. 하나라도 부적합하면
404로 전체 요청을 종료하고 Agent를 호출하지 않는다. Agent도 검색 직전에 DB 상태를 다시 확인한다.
향후 사용자별 데이터 격리가 생기면 접근 검사와 Agent 계약을 함께 확장해야 한다.

## 응답

Agent의 응답에는 data 래퍼가 없지만, **UI 응답에는 Backend 공통 BaseResponse 래퍼**를 적용한다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "checkedAt": "2026-11-04T01:00:00Z",
    "items": [
      {
        "miEventId": 5,
        "verificationId": "12345678-1234-4234-8234-123456789abc",
        "expiresAt": "2026-11-04T01:30:00Z",
        "title": "Event 제목",
        "sourceUpdatedAt": "2026-11-03T09:00:00",
        "status": "warning",
        "reasonCode": "no_evidence",
        "review": "비교 가능한 웹 근거를 찾지 못해 사실과 최신성을 확인할 수 없습니다.",
        "links": [],
        "checks": []
      }
    ]
  }
}
```

위 응답은 ID 5 하나를 요청한 경우다. 여러 ID를 요청하면 요청한 순서대로 각 ID의 결과가 반환된다.
HTTP 200 및 최상위 `status: true`는 처리 결과 반환 성공이며, 검증 통과 여부는 `items[].status`로 판단한다.
일부 Event만 검색·비교에 실패하더라도 나머지 결과와 함께 HTTP 200으로 반환한다.

각 항목의 `verificationId`는 Agent가 저장한 서로 다른 검증 결과의 UUID다.
화면은 `miEventId`, `verificationId`, `expiresAt`, `sourceUpdatedAt`과 표시한 검증 결과를 함께 관리한다.
화면에 표시한 결과의 ID를 [업데이트 API](event-verification-updates.md)에 전달한다. Event ID나 Snapshot ID로 대체할 수 없다.
Backend는 ID·만료 시각 누락, 중복 검증 ID, 잘못된 형식의 응답을 502로 처리한다.

기본 만료 시각은 **checkedAt + 30분**이며 응답 수신 시점부터 새로 계산하지 않는다.
`expiresAt` 문자열은 변환하지 않고 전달한다. UI의 만료 안내와 별개로 최종 만료 판정은 Agent가 한다.
현재 검증 이력 조회 API는 없다. 화면 메모리의 결과를 잃으면 재검증하거나 별도 화면 상태를 복원해야 한다.

| status | reasonCode | UI 처리 |
| --- | --- | --- |
| pass | verified | 최소 1개 일치, 나머지도 일치 또는 검증 대상 없음 |
| pass | no_contradiction | 명확한 불일치 없음. 주의·미확인 항목을 함께 표시 |
| warning | review_required | 일치 확인 항목 없음. 주의·미확인 항목 확인 |
| warning | no_evidence | 검색 근거 부족 안내 |
| warning | search_error | 검색 오류·시간 초과·미완료, 재검증 가능 |
| warning | verification_error | 비교 처리 실패, 재검증 가능 |
| fail | contradicted | 근거와 명확한 불일치 표시 |
| fail | outdated | 후속 근거로 대체된 정보 표시 |

예를 들어 **3개 일치 + 주의 2개 + 확인 불가 1개는 pass/no_contradiction**이다.
이는 모든 사실 확인 완료를 뜻하지 않는다. `checks`의 `caution`, `unverified`, `finding`을 숨기지 않는다.
이 공개 등급은 업데이트 API에도 동일하게 적용되며 저장 여부(`updateStatus`)와 별개다.

`review`는 최대 300자의 한국어 후기이며 프로그램 분기에 사용하지 않는다.
`checkedAt`은 UTC 검증 **기준** 시각으로 완료 시각이 아니다.
`sourceUpdatedAt`은 DB 수정 시각이며 null 또는 timezone 없는 값일 수 있다.
Backend는 두 시각의 문자열을 보존한다. UI도 timezone 없는 값에 임의로 Z를 붙이거나 UTC로 해석하지 않는다.

비교 완료 시 `checks`는 아래 6개 aspect를 각각 하나씩 포함한다.

| aspect | 대상 |
| --- | --- |
| entity | 사건·운영사·설비 동일성 |
| location | 발생 장소·영향 지역 |
| time | 발생일·시행일·전망 대상 기간 |
| quantity | 수치·단위·측정 범위 |
| freshness | 최신 변경·복구·취소·정정 |
| attribution | 기관·발표 주체·전망 귀속 |

각 check는 `aspect`, `verdict`, `eventClaim`, `finding`, `sourceUrls`를 갖는다.
선택적 `cautionReason`, `qualificationQuote`도 원문 그대로 반환한다. 생략되거나 null인 Agent 응답도 허용한다.
`cautionReason`은 `as_of_limitation`(확인 시점 한계), `source_conflict`(출처 상충),
`insufficient_evidence`(근거 부족), `other`이며 사유가 없으면 null이다.
`qualificationQuote`는 기준 날짜와 이후 미확인 범위를 명시한 본문 발췌다. 확인 시점 한계일 때 함께 표시한다.
verdict는 `matched`, `caution`, `contradicted`, `outdated`, `unverified`, `not_applicable` 중 하나다.
검증 미완료(`no_evidence`, `search_error`, `verification_error`)는 `checks: []`다.
기관별 전망 차이·기사 나이만으로 UI에서 fail로 바꾸지 않는다. Agent 판정을 그대로 표시한다.
`links`와 `sourceUrls`는 근거 링크이며, 없으면 빈 배열이다. 제목·후기는 일반 텍스트로 표시한다.

## 전체 요청 오류

전체 오류는 HTTP 오류 상태와 `status: false`, `data: null`로 전달한다.
Agent의 도메인 오류 본문과 FastAPI의 detail 배열은 내부 형식이므로 UI에 그대로 노출하지 않는다.

| HTTP | 의미 |
| --- | --- |
| 401 / 403 | 로그인 또는 Event 접근 권한 오류 |
| 404 | 선택 ID 중 하나라도 없음·삭제·미게시. 목록 새로고침 |
| 422 | 입력 형식·개수·중복·추가 필드 오류 |
| 500 | Backend DB 또는 Agent의 요청 전체 처리 실패 |
| 502 | Agent 연결 실패(API-003) 또는 잘못된 응답/API 계약 불일치(API-004) |
| 503 | Agent 서비스 사용 불가 |
| 504 | Agent/Backend 대기시간 초과(API-002) |

응답 누락, ID 순서·목록 불일치, 미지의 판정 코드, 잘못된 비교 항목 구조 등은 502이며,
검증 완료나 항목별 warning으로 위장하지 않는다.

## Agent 주소와 timeout

`application.yml`의 `api.event-verification-agent`에서 설정한다. 로컬 .env 또는 배포 환경변수로 변경할 수 있다.

```properties
EVENT_VERIFICATION_AGENT_URL=http://localhost:8000
EVENT_VERIFICATION_AGENT_CONNECTION_TIMEOUT=3s
EVENT_VERIFICATION_AGENT_READ_TIMEOUT=210s
```

주소를 생략하면 해당 프로파일의 `api.scenario-design-agent.url`을 사용한다.
gateway prefix가 있다면 주소에 포함한다. 예: `https://agent.example/gateway/scenario`를 지정하면
`https://agent.example/gateway/scenario/events/verifications`에 POST한다. UI 인증 헤더를 Agent에 전달하지 않는다.
Docker Desktop의 Backend 컨테이너에서 호스트 Agent를 호출하면 `http://host.docker.internal:8000` 등의 접근 가능한 주소가 필요하다.

Agent의 기본 제한은 항목별 대기·검색·비교를 포함해 180초이며 DB 조회 시간은 별도다.
Backend는 기본 210초, UI 및 gateway는 예를 들어 **240초 이상**을 시작점으로 잡고 실제 DB·처리 지연에 맞춰 조정한다.
이 저장소에서 배포 gateway 설정은 변경하지 않았다. 인프라가 이 시간을 허용하지 않으면 작업 접수·상태 조회 방식의 별도 구현이 필요하다.
현재 API는 동기 JSON 응답이며 callback/SSE를 사용하지 않는다. Agent를 기다리는 동안 DB 트랜잭션을 유지하지 않는다.

## 재검증·이력·시나리오 정책

- HTTP 클라이언트의 자동 재시도는 비활성화했다. 재호출은 새 검색·비교이며 캐시를 재사용하지 않는다.
- UI는 검증 중 버튼을 비활성화하고, 같은 선택의 중복 요청을 막는다. 선택 변경·화면 이동 후 이전 응답은 무시한다.
- `search_error`와 `verification_error`는 사용자 재검증 대상으로 표시할 수 있다. 화면에서 HTTP 200을 무조건 pass로 표시하지 않는다.
- 검증 결과·근거 저장은 Agent가 담당한다. Backend는 verificationId와 expiresAt을 전달하며 별도 저장하지 않는다.
- Event 변경 또는 만료 이후에는 새 검증을 실행하고 새 ID를 사용한다. 업데이트 API가 자동으로 새 검증을 수행하지 않는다.
- 시나리오 생성 허용·경고·차단 정책은 추가하지 않았다. 검증 API가 생성을 자동 차단하지 않는다.

## 검증 및 실행

```powershell
.\gradlew.bat "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NONE" test -PforceTest --tests '*EventVerification*Test' --tests '*EventRecommendationTest'
```

테스트는 실제 HTTP 클라이언트와 모의 Agent를 연결하고, 권한·입력·부분 실패·오류·시간 제한·gateway prefix를 검증한다.
실제 웹 검색/LLM·DB를 연결한 통합 검증은 별도다. 새 실행 JAR가 필요하면 **기존 Backend를 종료한 뒤 build하고 재실행**한다.
