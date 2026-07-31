# MI Dashboard Summary Download Backend Feature Spec

## 1. Document Control

- status: Review
- canonical for: Dashboard summary file-download endpoint path, HTTP method, request fields, `predictDate` validation, response shape, and representative API errors
- owner: BE
- source documents:
  - `docs/work-notes/2026/2026-07-28-null-return-dereference-remediation-plan.md`
  - `docs/work-notes/2026/2026-07-28-null-return-dereference-security-review.md`
- depends on:
  - `AGENTS.md`
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 없음
- last synced: 2026-07-29
- open questions:
  - 없음

## 2. Feature goal

- 인증·인가된 사용자가 Dashboard summary 관련 파일을 ZIP으로 내려받는 기존 기능을 유지한다.
- optional `predictDate`가 전달되면 정확한 `yyyyMM` 형식으로 검증해 해당 월의 시작일부터 종료일까지 조회한다.
- 잘못된 `predictDate`를 요청 단위 NPE가 아닌 정의된 HTTP 400으로 처리한다.
- 예상하지 못한 예외의 내부 Java 메시지를 uncaught HTTP 500 응답에 노출하지 않는다.
- 이 API는 조회 및 파일 다운로드 기능이며 영속 상태를 변경하지 않는다.

## 3. Canonical references

- endpoint와 validation 및 대표 오류 응답의 canonical source는 이 문서다.
- 변경 파일, 테스트 결과, Sparrow 재분석 결과와 잔여 위험은 다음 execution record에 기록한다.
  - `docs/work-notes/2026/2026-07-28-null-return-dereference-remediation-plan.md`
- 물리 schema와 DDL 변경은 이 API slice에 없다.
- 시장 데이터 invalid row 처리 정책은 이 문서의 범위가 아니며 PR 3의 별도 living design에서 정한다.

## 4. Fixed API contract

- `predictDate`는 optional이다.
- `predictDate`가 전달되면 ASCII 숫자 6자리이며 월 부분이 `01`부터 `12`여야 한다.
- canonical validation pattern은 `^[0-9]{4}(0[1-9]|1[0-2])$`이다.
- 별도의 연도 범위 제한은 이번 API slice에서 추가하지 않는다.
- `predictDate`가 없으면 기존처럼 예측 기간 조건 없이 조회한다.
- `predictDate`가 유효하면 Service가 한 번 strict parse한 `YearMonth`를 기준으로 조회 기간을 계산한다.
  - `202506`의 시작일: `2025-06-01`
  - `202506`의 종료일: `2025-06-30`
- Controller validation을 우회해 Service를 직접 호출해도 invalid 값은 `REQ_INVALID_PARAMETER`로 종료한다.
- 예상하지 못한 예외는 내부 log에 원본 메시지와 stack trace를 남기되, uncaught HTTP 500 응답에는 원본 예외 detail을 포함하지 않는다.
- 성공 시 기존 ZIP 파일명, byte body, download header 계약을 유지한다.

## 5. Endpoint

### Dashboard summary 파일 다운로드

- Method: `GET`
- Path: `/api/v1/mi/dashboard/companies/{companyNo}/summary/file-download`
- 성격: read/file-download
- persistent state change: 없음
- 권한: 기존 `RequiredPermission` 계약을 유지한다.
  - `ADMIN`
  - `USER`
  - `API_PLUS_OPERATOR`
  - `API_SUB_ADMIN`
  - condition: `THIRD_PARTY_ADMIN_ONLY`
- HTTP status:
  - `200 OK`: ZIP byte body 반환
  - `400 Bad Request`: request parameter validation 실패
  - 기존 권한·리소스 오류 status 처리: 현재 공통 예외 처리 계약 유지
  - `500 Internal Server Error`: 예상하지 못한 uncaught exception

## 6. Request DTO draft

- request body와 별도 request DTO는 없다.

| 위치 | 필드 | 타입 | 필수 | 계약 |
| --- | --- | --- | --- | --- |
| path | `companyNo` | `Integer` | Y | 조회 대상 고객회사 번호. 이번 slice에서 기존 계약을 변경하지 않는다. |
| query | `date` | `String` | Y | 기존 기준일 파라미터. 현재 문서에서는 형식이나 validation을 변경하지 않는다. |
| query | `predictDate` | `String` | N | 전달 시 `^[0-9]{4}(0[1-9]\|1[0-2])$`를 만족하는 `yyyyMM`. |

### `predictDate` 경계 규칙

| 입력 | 결과 |
| --- | --- |
| 파라미터 미전달 | 허용 |
| `202506` | 허용 |
| 빈 값 `predictDate=` | HTTP 400, `REQ_INVALID_PARAMETER` |
| `x` | HTTP 400, `REQ_INVALID_PARAMETER` |
| `202613` | HTTP 400, `REQ_INVALID_PARAMETER` |
| `2026-06` | HTTP 400, `REQ_INVALID_PARAMETER` |
| `20250601` | HTTP 400, `REQ_INVALID_PARAMETER` |
| `202506suffix` | HTTP 400, `REQ_INVALID_PARAMETER` |
| 앞뒤 공백이 포함된 값 | HTTP 400, `REQ_INVALID_PARAMETER` |

### Idempotency and concurrency

- 동일한 저장 상태에서 같은 요청을 반복해도 서버의 영속 상태는 바뀌지 않는다.
- 별도 idempotency key나 concurrency control을 추가하지 않는다.

## 7. Response DTO draft

### Success

- 별도 response DTO와 `BaseResponse<T>` envelope을 사용하지 않는다.
- status: `200 OK`
- `Content-Type`: `application/octet-stream`
- `Content-Disposition`: attachment; 기존 encoded ZIP filename 사용
- `Content-Length`: 반환 ZIP byte length
- body: ZIP file bytes

기존 filename 형식:

```text
dashboard_summary_{date}_{currentYYYYMMDDHHMMSS}.zip
```

### Error

- 오류 응답은 공통 `BaseResponse<Object>` shape를 사용한다.
- serialize 설정에 따른 null field 표시는 공통 응답 정책을 따른다.

```json
{
  "status": false,
  "code": "REQ-003",
  "message": "One or more parameters are invalid.(...)"
}
```

```json
{
  "status": false,
  "code": "SYS-001",
  "message": "An internal server error has occurred."
}
```

- validation detail의 구체적인 문구는 framework message에 따라 달라질 수 있다.
- HTTP 400의 안정된 계약은 `REQ-003` code와 invalid parameter 의미다.
- uncaught HTTP 500의 안정된 계약은 `SYS-001` code와 `SYS_INTERNAL_ERROR` 기본 message다.
- uncaught HTTP 500 body에는 원본 `Exception#getMessage()`, Java class/method 이름 또는 stack trace를 추가하지 않는다.

## 8. Representative error scenarios

| 상황 | HTTP status | ResultCode | 응답 detail 규칙 |
| --- | ---: | --- | --- |
| invalid `predictDate`가 Controller validation에서 거절됨 | 400 | `REQ_INVALID_PARAMETER` (`REQ-003`) | invalid parameter 식별 가능. 내부 stack trace 미포함 |
| Service 직접 호출에 invalid `predictDate`가 전달됨 | 400 변환 대상 | `REQ_INVALID_PARAMETER` (`REQ-003`) | `AppErrorException`으로 변환. NPE 금지 |
| 예상하지 못한 NPE 등 `handleUncaughtException` 도달 | 500 | `SYS_INTERNAL_ERROR` (`SYS-001`) | 공개 detail 없음 |
| 고객회사 또는 Dashboard summary 데이터 없음 | 기존 공통 처리 유지 | `RSC_NOT_FOUND` (`RSC-001`) | 이번 slice에서 변경하지 않음 |
| 다운로드 권한 없음 | 기존 공통 처리 유지 | `AUTH_ACCESS_DENIED` (`AUTH-007`) | 이번 slice에서 변경하지 않음 |

### Uncaught 500 logging boundary

- server log에는 exception type, 원본 message, stack trace와 기존 correlation 정보를 유지한다.
- client response와 response MDC error message에는 원본 exception detail을 포함하지 않는다.
- 이번 sanitization 범위는 `GlobalExceptionHandler#handleUncaughtException`이다.
- framework internal exception, `IOException`, Object Storage 등 다른 5xx handler의 공개 detail 통합은 제외한다.

## 9. Swagger annotation recommendation

- `@Operation`
  - summary: `대시보드 Summary 파일 다운로드`
  - description: `대시보드 Summary 관련 파일을 ZIP으로 다운로드합니다. predictDate는 선택값이며 전달 시 yyyyMM 형식이어야 합니다.`
- `predictDate`의 `@Parameter`
  - description: `예측 월(선택, yyyyMM)`
  - example: `202506`
  - required: `false`
  - pattern: `^[0-9]{4}(0[1-9]|1[0-2])$`
- 대표 `@ApiResponse`
  - `200`: ZIP file download
  - `400`: `REQ_INVALID_PARAMETER`
  - `500`: sanitized `SYS_INTERNAL_ERROR`

## 10. Data/model impact

- DB schema/DDL 변경: 없음
- domain entity/column 변경: 없음
- request/response DTO 추가: 없음
- ERD review: 불필요
- 조회 대상과 기존 query 구조는 유지한다.

## 11. Recommended data access approach

- 신규 data access 구현은 필요하지 않다.
- 기존 repository/JPA와 Dashboard mapper/MyBatis 조회 흐름을 유지한다.
- 이번 slice는 API boundary validation, Service 입력 정규화와 공통 오류 응답 변경에 한정한다.

## 12. Implementation touch points

- Controller
  - `src/main/java/com/sk/domain/mi/controller/MiDashboardController.java`
  - `predictDate` parameter constraint
  - class-level `@Validated`는 추가하지 않고 Spring MVC built-in method validation을 사용
- Service
  - `src/main/java/com/sk/domain/mi/service/MiDashboardService.java`
  - `uuuuMM` + `ResolverStyle.STRICT` 기반 단일 `YearMonth` parse
  - invalid 입력을 `AppErrorException(REQ_INVALID_PARAMETER)`으로 변환
- Global error handler
  - `src/main/java/com/sk/common/handler/GlobalExceptionHandler.java`
  - `handleUncaughtException`의 response detail 제거
  - 내부 exception logging 유지
- Tests
  - `src/test/java/com/sk/domain/mi/controller/MiDashboardControllerTest.java`
  - `src/test/java/com/sk/domain/mi/service/MiDashboardServiceTest.java`
  - `src/test/java/com/sk/common/handler/GlobalExceptionHandlerTest.java`

## 13. Open questions

- 없음
- Dashboard `companyNo` tenant binding의 별도 authorization finding은 이 null-return API slice에 포함하지 않는다.

## 14. Change log / sync note

- 2026-07-29
  - 최초 작성
  - optional `predictDate`의 `yyyyMM` validation과 HTTP 400 계약 고정
  - Service strict parsing fallback 계약 고정
  - uncaught HTTP 500 response sanitization 범위 고정
  - schema/DDL 및 다른 5xx handler를 범위 밖으로 명시

## 15. Exit criteria / handoff

- implementation readiness: `ready`
- endpoint shape, validation, HTTP 400과 uncaught HTTP 500 응답 계약에 open question이 없다.
- schema 변경이 없으므로 `erd-change-review` handoff는 필요하지 않다.
- `spring-api-implementation`은 다음을 구현할 수 있다.
  - Controller `predictDate` validation
  - Service strict `YearMonth` parsing과 controlled invalid-input error
  - generic uncaught 500 response detail 제거
  - Controller/Service/handler regression tests

## 16. API slice boundary

### Included

- `GET /api/v1/mi/dashboard/companies/{companyNo}/summary/file-download`
- optional `predictDate` validation
- Service direct-call validation fallback
- `GlobalExceptionHandler#handleUncaughtException` response sanitization
- finding `102148`, `102149`

### Excluded

- 다른 Dashboard endpoint의 date parameter validation
- `companyNo` tenant binding authorization finding
- 다른 5xx handler의 공개 detail 정책 통합
- Object Storage/PDF 안정성 수정
- 시장 데이터 invalid row 정규화
- schema/DDL 변경

### DTOs needed now

- 신규 DTO 없음

### Query/mock strategy

- production query는 기존 repository와 mapper를 그대로 사용한다.
- Controller 테스트는 Service를 mock 처리해 parameter validation과 오류 응답을 검증한다.
- Service 테스트는 invalid value가 repository/mapper 접근 전에 controlled error로 종료되는지 검증한다.
- handler 테스트는 임의 NPE의 내부 detail이 HTTP 500 body에 없는지 검증한다.

### Verification commands

```bash
./gradlew test -PforceTest --tests 'com.sk.domain.mi.controller.MiDashboardControllerTest'
./gradlew test -PforceTest --tests 'com.sk.domain.mi.service.MiDashboardServiceTest'
./gradlew test -PforceTest --tests 'com.sk.common.handler.GlobalExceptionHandlerTest'
./gradlew compileJava -x test
git diff --check
```
