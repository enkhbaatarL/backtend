# Event 추천 API 연동

## UI → Backend

`POST /api/v1/mi/next-gen/events/recommendations`

- 기존 Event 목록과 동일한 로그인 인증 및 권한을 사용한다 (SK Gas USER / 타사 접근 ADMIN 등 기존 권한 정책).
- `Content-Type: application/json`, `Authorization: Bearer <UI access token>`.
- `dedicated` 프로파일에는 노출하지 않는다.

```json
{"miEventIds": [5, 12]}
```

ID 전체를 전달한다. 양의 JSON 정수 1~5개만 허용하며 중복, null, 문자열, 소수는 거절한다.
Backend 입력 검증 오류는 기존 공통 규칙에 따라 HTTP 400이다.

성공 응답은 Backend 공통 BaseResponse이며 추천 정보는 `data`에 있다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "items": [
      {"miEventId": 47, "title": "LNG 공급 차질 관련 Event", "reason": "함께 수급 영향을 분석할 수 있습니다."}
    ],
    "selectionReason": "선택한 조합에 추가 정보를 제공하는 후보를 선정했습니다."
  }
}
```

`code`, `message`는 기존 공통 성공 코드 규칙을 따른다. `items`는 전체 요청에서 0~2개다.
`items: []`는 정상적인 추천 없음이다. 상세 본문은 기존 Event 상세 API에서 조회한다.
이 API는 자동 선택, 시나리오 생성, DB 저장, callback 또는 SSE를 사용하지 않는다.

## Backend → Agent 설정

기존 프로파일의 `api.scenario-design-agent.url`을 기본 주소로 사용하며
`POST /events/recommendations`에 동일한 JSON을 보낸다. Agent 인증 헤더는 보내지 않는다.
추천 요청 전용 클라이언트를 사용하므로 시나리오 설계의 timeout은 바뀌지 않는다.

| 설정 | 환경변수 / 로컬 .env 키 | 기본값 |
| --- | --- | --- |
| `api.event-recommendation-agent.url` | `EVENT_RECOMMENDATION_AGENT_URL` | 해당 프로파일의 scenario-design-agent 주소 |
| `api.event-recommendation-agent.connection-timeout` | `EVENT_RECOMMENDATION_AGENT_CONNECTION_TIMEOUT` | `1s` |
| `api.event-recommendation-agent.read-timeout` | `EVENT_RECOMMENDATION_AGENT_READ_TIMEOUT` | `120s` |

로컬 기본 주소는 `http://localhost:8000`이다. Docker Desktop의 Backend 컨테이너에서 호스트의
Agent를 호출한다면 `EVENT_RECOMMENDATION_AGENT_URL=http://host.docker.internal:8000`을 사용하고
Agent가 컨테이너에서 접근 가능한 인터페이스에 바인딩되어 있어야 한다. 설정 변경 후 Backend를 재시작한다.
UI HTTP 클라이언트 및 프록시 timeout도 120초 응답을 기다릴 수 있도록 여유 있게 설정한다.

## 오류

오류 응답은 `status: false`, `data: null`이다. 실패를 빈 추천으로 변환하지 않는다.
Agent 내부 오류 본문은 UI에 그대로 노출하지 않고 안내 메시지로 변환한다.

| HTTP | 의미 | UI 처리 |
| --- | --- | --- |
| 400 | Backend 입력 검증 실패 / Agent 처리 조건 초과 | 입력값·처리 조건 확인 |
| 404 | Agent에서 선택 Event 없음/삭제/미게시 | 목록 새로고침 |
| 422 | Agent 입력 검증 실패 | 입력값 확인 |
| 500 | Agent 검색·LLM 처리 실패 | 추천 실패 안내 |
| 502 | Agent 연결 실패(`API-003`) 또는 잘못된 응답(`API-004`) | 연결 확인 후 재시도 |
| 504 | Agent 또는 Backend 대기시간 초과(`API-002`) | 재시도 안내 |

인증/권한 오류는 기존 Backend의 401/403 정책을 따른다.
Agent가 성공으로 응답해도 필수 필드 누락, 추천 2개 초과, 중복 ID, 선택된 ID 재추천은 502로 처리한다.

## UI에서 선택이 바뀌는 경우

요청마다 선택 ID 전체를 전달하고, 오래된 응답은 성공·실패 모두 무시한다.
선택 해제 시에도 요청 번호를 갱신해야 이전 응답이 다시 표시되지 않는다.
아래는 기존 인증된 API 클라이언트를 사용하는 화면에 적용할 예시이며, 실제 UI 수정은 별도다.

```javascript
let recommendationRequest = 0;

async function onSelectionChanged(miEventIds) {
  const request = ++recommendationRequest;
  setRecommendations(null);
  setRecommendationError(null);
  if (miEventIds.length < 1 || miEventIds.length > 5) {
    setRecommendationLoading(false);
    return;
  }
  setRecommendationLoading(true);
  try {
    const response = await api.post(
      '/api/v1/mi/next-gen/events/recommendations',
      { miEventIds: [...miEventIds] },
      { timeout: 130000 }
    );
    if (request !== recommendationRequest) return;
    // Axios 기준: response.data는 Backend의 BaseResponse.
    setRecommendations(response.data.data);
  } catch (error) {
    if (request !== recommendationRequest) return;
    setRecommendationError(error);
  } finally {
    if (request === recommendationRequest) setRecommendationLoading(false);
  }
}
// 화면 해제 시에도 ++recommendationRequest 로 대기 중 응답을 무효화한다.
```

현재 Agent는 공유 데이터 테스트용으로 사용자별 Event 후보 접근 권한을 확인하지 않는다.
Backend의 API 접근 권한 검사는 적용되지만 사용자별 후보 격리를 제공하지는 않는다.
그 요구가 생기면 Agent 검색 조건과 양쪽 API 계약에 권한 정보를 추가해야 한다.

## 검증

```powershell
.\gradlew.bat "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NONE" test -PforceTest --tests '*EventRecommendationTest'
.\gradlew.bat "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NONE" build
```

프로젝트는 기본 build에서 테스트를 건너뛰므로 테스트 실행 시 `-PforceTest`가 필요하다.
계약 테스트는 실제 HTTP 클라이언트와 로컬 모의 Agent를 사용하며 DB/Redis/LLM은 필요하지 않다.
실제 UI 연동 확인에는 로그인한 UI, Backend, 검색 인덱스와 LLM이 준비된 Agent가 필요하다.
