# MI Fundamental Model - 1st Mock API Feature Spec

- domain: gas
- status: draft for 1st mock implementation
- owner: platform BE
- updated at: 2026-06-08 (run/callback DB-backed readiness sync)
- canonical design input: [scenario-case-execution-workflow-design-draft.md](../design-docs/gas/scenario-case-execution-workflow-design-draft.md)
- reference only:
  - Notion `MI Fundamental Model 실행` (`350263ff004c8035b20ef02683447f4b`)
  - Notion `MI Fundamental Model` (`34c263ff004c81ad817bc0a5a1c2992f`)

## Document Roles / Source of Truth

이 문서는 MI Fundamental Model 1차 mock API의 endpoint, request/response DTO, error mapping, mock behavior handoff의 canonical source다.

관련 문서 역할은 다음처럼 분리한다.

| 문서 | 역할 | canonical 범위 |
|---|---|---|
| 이 feature spec | API 구현 계약 | endpoint path, method, DTO field, response shape, error/status mapping, phase 1 mock behavior |
| [scenario-case-execution-workflow-design-draft.md](../design-docs/gas/scenario-case-execution-workflow-design-draft.md) | workflow/domain design | Scenario Case aggregate, state transition, callback/timeout semantics, future persistence rationale |
| [scenario-case-execution-erd-ddl-draft.md](../design-docs/gas/scenario-case-execution-erd-ddl-draft.md) | DBA rollout 계약 | physical table/column DDL, index, common-code registration, pre-check/apply/rollback/verify SQL (현재 template 상태) |
| [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md) | output result persistence 설계 | Volumes / Trade Flows / Price parsed result table, key, index, partition-ready strategy, mock→real 전환 경계 |
| `docs/work-notes/**` | 실행 이력 | changed files, verification, accepted decisions, superseded decisions, residual risks |
| Notion / Slack / Figma | external evidence/share artifact | repo 문서로 정규화되기 전까지 canonical contract가 아니다 |

Work note와 외부 문서가 이 feature spec 또는 canonical design draft와 충돌하면, 구현자는 feature spec/design draft를 우선하고 충돌 내용을 새 work note에 기록한다.

## 1. Feature Goal

MI Fundamental Model의 1차 mock API는 MI Fundamental Model 화면에서 사용자가 Scenario Case를 업로드하고, 특정 case 실행을 요청한 뒤, 실행 상태를 확인하고, 외부 모델 완료 callback 형태로 mock 실행을 terminal 상태로 전이할 수 있게 하는 backend 계약이다.

`Scenario Case`는 현업에서 사용하는 업무 용어이므로 유지한다. 다만 API hierarchy에서 MI Next Gen의 `Scenario` 하위 resource라는 의미는 아니다. MI Fundamental Model은 MI Next Gen와 동급 메뉴이며, 이 문서의 canonical API는 Next Gen path family를 사용하지 않는다.

이번 문서는 구현자가 바로 controller/service/DTO/mock store를 만들 수 있는 API feature spec을 제공한다. 단, 이 문서는 Java 구현을 포함하지 않으며 DB rollout 가능한 DDL도 확정하지 않는다.

### Phase 1 Scope

- Scenario Case 업로드
  - 파일 3개
  - 제목
  - 설명
  - 생성 직후 상태 `PENDING`
- Scenario Case 실행 요청
  - 특정 case 실행
  - 같은 case의 `RUNNING` 중복 실행 차단
  - `run_id` 생성
  - 실행 상태 `RUNNING`
- Scenario Case 상태 조회
  - 1차 권장안: case 기준 상태 조회
  - 상태: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- Scenario Case 상세 조회
  - case metadata
  - uploaded file metadata
  - latestRun projection
- Scenario Case metadata 수정
  - `title`, `description`만 허용
  - title은 trim 후 1-200자 필수
  - 수정 응답은 최신 상세 응답 반환
- Scenario Case soft delete
  - active resource에서 제외
  - upload files, run history, latestRun/result payload는 물리 삭제하지 않음
- 개발/시연용 mock 동작
  - in-memory/mock store 기준
  - 반복 시연을 위한 reset API 포함
- 외부 모델 callback API mock 구현
  - endpoint path 추천
  - callback DTO 초안
  - terminal 상태 전이/idempotency 규칙
- Volumes 데이터 조회 (Global / LNG)
  - `COMPLETED` case의 실행 결과 중 Volumes 뷰 반환
  - segment(`GLOBAL` | `LNG`) 필수. `TRADE`는 이 엔드포인트에서 허용하지 않는다
  - Type / Region 다중 필터 지원 (미전달 시 전체)
  - granularity(`MONTHLY` | `YEARLY`) 지원, 기본 `MONTHLY`
  - 1차 mock은 json seed 기반 고정 응답
- Trade Flows 데이터 조회 (Trade)
  - `COMPLETED` case의 실행 결과 중 Trade Flow 매트릭스 반환
  - 별도 엔드포인트 `GET /{caseId}/volumes/trade` 사용
  - selector(`LNG` | `PNG`), year(2023~2030) 필수
  - unit(`MTPA` | `BCM`) 선택, 기본 `MTPA`
  - types(`TO_COUNTRY` | `FROM_COUNTRY`), Region 다중 필터 지원 (미전달 시 전체)
  - granularity 없음 (단일 연도 매트릭스 구조)
  - fromCountries / toCountries 헤더 배열 + sparse flows 배열로 응답
  - 1차 mock은 json seed 기반 고정 응답
- Price 데이터 조회
  - `COMPLETED` case의 실행 결과 중 Price 뷰 반환
  - 필터 없음, segment 없음, granularity 없음
  - 차트 데이터(Base case price / Last adjusted price) + Annual 테이블 + Monthly 테이블 동시 반환
  - 1차 mock은 json seed 기반 고정 응답

### Explicitly Not Implemented

- 결과 파일 다운로드
- 운영 수준 callback 인증/서명/재시도 정책
- 실제 Kubernetes Job dispatch 연동
- timeout scheduler 구현
- DB persistence 구현

### Boundary Against Existing ScenarioModelExecution API

기존 `ScenarioModelExecution` API는 Next Gen의 parameter 기반 MI 수급모델 실행 리소스를 다룬다. 이 spec의 `FundamentalModelCase`는 파일 3개 업로드를 기준으로 생성되는 Scenario Case aggregate이며, 기존 `model-executions` resource와 별개 기능으로 구현한다.

구현 시 controller/DTO/service 명명도 다음처럼 분리한다.

| 구분 | 기존 API | 이번 API |
|---|---|---|
| 주요 resource | `model-executions` | `fundamental-model/cases` |
| 실행 식별자 | `executionId`, `externalRunId` | `caseId`, `runId` |
| 입력 방식 | JSON parameter | multipart file upload + metadata |
| 목적 | Scenario MI 수급모델 실행 | Scenario Case 기반 MI Fundamental Model mock 실행 |

## 2. Endpoint

### Recommended Path Family

권장 base path:

```text
/api/v1/mi/fundamental-model
```

계약 원칙:

- MI Fundamental Model은 MI Next Gen와 동급 메뉴다.
- `Scenario Case` 명칭은 현업 용어로 유지하지만, API parent가 `Scenario`라는 뜻은 아니다.
- `scenarioId`는 MI Fundamental Model 1차 mock API의 path variable, query parameter, multipart part, response DTO에서 사용하지 않는다.
- canonical endpoint는 `/api/v1/mi/fundamental-model/**` path family만 사용한다.
- 기존 `/api/v1/mi/next-gen/**/fundamental-*` 및 `/api/v1/mi/next-gen/scenarios/{scenarioId}/fundamental-model-cases/**` 형태는 superseded contract다. 구현자는 이 경로를 신규 controller 계약으로 사용하지 않는다.

### Phase 1 Implemented Endpoint Summary

아래 endpoint는 1차 mock API 구현 대상이다. 단, callback endpoint는 mock store 상태 전이를 위한 구현 범위이며, 운영 수준 Kubernetes Job dispatch, callback 인증/서명/재시도 정책은 future integration scope로 남긴다.

| 기능 | Method | Path | HTTP status | Response data |
|---|---|---|---|---|
| Scenario Case 목록 조회 | `GET` | `/api/v1/mi/fundamental-model/cases` | `200 OK` | `FundamentalModelCaseListRes` |
| Scenario Case 업로드 | `POST` | `/api/v1/mi/fundamental-model/cases` | `201 Created` | `FundamentalModelCaseCreateRes` |
| Scenario Case 상세 조회 | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}` | `200 OK` | `FundamentalModelCaseDetailRes` |
| Scenario Case metadata 수정 | `PATCH` | `/api/v1/mi/fundamental-model/cases/{caseId}` | `200 OK` | `FundamentalModelCaseDetailRes` |
| Scenario Case 삭제 | `DELETE` | `/api/v1/mi/fundamental-model/cases/{caseId}` | `200 OK` | `Void` |
| Scenario Case 실행 요청 | `POST` | `/api/v1/mi/fundamental-model/cases/{caseId}/runs` | `202 Accepted` | `FundamentalModelRunAcceptRes` |
| Scenario Case 상태 조회 | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/status` | `200 OK` | `FundamentalModelCaseStatusRes` |
| Volumes 필터 옵션 조회 | `GET` | `/api/v1/mi/fundamental-model/cases/volumes/filter-options` | `200 OK` | `FundamentalModelVolumesFilterOptionsRes` |
| Volumes 데이터 조회 (Global/LNG) | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/volumes` | `200 OK` | `FundamentalModelVolumesRes` |
| Trade Flows 데이터 조회 | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/volumes/trade` | `200 OK` | `FundamentalModelTradeFlowsRes` |
| Price 데이터 조회 | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/price` | `200 OK` | `FundamentalModelPriceRes` |
| 외부 모델 완료 callback | `POST` | `/api/v1/mi/fundamental-model/runs/{runId}/callback` | `200 OK` | `FundamentalModelRunCallbackRes` |
| Mock 데이터 초기화 | `POST` | `/api/v1/mi/fundamental-model/cases/reset` | `200 OK` | `Void` |

### Future / Deferred Contracts

아래 항목은 이 문서에서 경계만 기록하며, 구현하려면 별도 feature spec 또는 DDL/ERD review가 필요하다.

| 항목 | 현재 상태 | 필요한 후속 source of truth |
|---|---|---|
| 결과 파일 다운로드 | phase 1 제외 | result/download API feature spec + result/file ERD review |
| 운영 Kubernetes Job dispatch | phase 1 제외 | external model interface spec + workflow design |
| callback 인증/서명/재시도 | mock 구현 밖의 운영 정책 | security/integration design + feature spec update |
| DB persistence / DDL (volumes/trade flows/price 포함) | phase 1 제외. mock은 json seed | case/run DDL은 `scenario-case-execution-erd-ddl-draft.md`, output result data persistence는 `mi-fundamental-model-output-result-persistence-design.md` |
| Ownership scope | DB-backed 확정 사항 | `usr_no` only로 확정. 조직/부서/tenant 정책 키는 본 모델에 추가하지 않는다 |
| `ASSIGNEE_NAME` DB 검색 경계 | DB-backed 확정 사항 + 검증 필요 | `tmd_scncasem.usr_no = tmd_usrm.usr_no` JOIN, 표시값 `tmd_usrm.usr_nm` 사용. rollout 전 JOIN 양측 인덱스 EXPLAIN 검증 필요 |
| timeout scheduler | phase 1 제외 | DB-backed scheduler design + query/EXPLAIN verification |
| Volumes/Price 필터 저장 (Save 토글) | phase 1 제외. FE 로컬 상태로 관리 | user preference API feature spec + DB 테이블 설계 |

### Future Contract Only

아래 내용은 phase 1 구현 계약이 아니라 후속 실연동 또는 DB-backed 전환 시 계약 초안으로만 사용한다.

- 운영 Kubernetes Job dispatch request/response
- 운영 callback 인증/서명/재시도/idempotency 세부 정책
- result 조회/download endpoint와 result pointer/reference persistence
- DB-backed timeout scheduler query와 batch update strategy
- delete retention/audit policy beyond `tmd_scncasem.del_yn = 'Y'`

### Status Query Recommendation

1차 구현은 case 기준 상태 조회를 권장한다.

| 방식 | 장점 | 단점 | 판단 |
|---|---|---|---|
| case 기준 | `PENDING`처럼 아직 `run_id`가 없는 상태도 조회 가능하다. FE가 업로드 응답의 `caseId`만 보관하면 된다. | 과거 실행 이력 중 특정 run만 조회하려면 추가 endpoint가 필요하다. 후속 DB 전환 시 case ownership/permission 검증을 별도 설계해야 한다. | 1차 mock API 권장 |
| `run_id` 기준 | 외부 모델 callback 추적 키가 동일하다. 실행 이력 중심 조회에 적합하다. | 업로드 직후 `PENDING` case 조회가 불가능하다. | 후속 실연동/이력 조회에서 검토 |

후속으로 이력 조회가 필요해지면 `GET /api/v1/mi/fundamental-model/runs/{runId}` 또는 case 하위 `GET /api/v1/mi/fundamental-model/cases/{caseId}/runs/{runId}`를 별도 계약으로 추가한다.

### Detail Query Recommendation

상세 조회는 화면 상세 진입 시 필요한 case aggregate projection을 반환한다.

- canonical endpoint: `GET /api/v1/mi/fundamental-model/cases/{caseId}`
- response DTO: `FundamentalModelCaseDetailRes`
- 포함 범위:
  - case metadata (`caseId`, `title`, `description`, `status`, `assigneeName`, `createdAt`, `updatedAt`)
  - uploaded file metadata (`files`)
  - 최신 실행 projection (`latestRun`)
- 제외 범위:
  - full run history
  - result payload
  - result file download URL/path
  - result 조회/download contract

`GET /cases/{caseId}/status`는 FE polling을 위한 경량 상태 조회로 유지한다. 상태 조회는 파일 metadata와 설명 등 화면 상세 metadata를 반환하지 않는다. 반대로 상세 조회는 화면 상세 rendering에 필요한 projection이며, polling interval에서 반복 호출하는 API로 설계하지 않는다.

### Callback Path Recommendation

외부 모델 완료 callback은 `runId` 단독 path를 권장한다.

```text
POST /api/v1/mi/fundamental-model/runs/{runId}/callback
```

검토한 대안:

| 방식 | 예시 | 장점 | 단점 | 판단 |
|---|---|---|---|---|
| runId 단독 | `/api/v1/mi/fundamental-model/runs/{runId}/callback` | `run_id`가 backend-generated `yyyymmdd_hhmmss_XXXX` unique key이므로 callback lookup이 단순하다. model pod는 backend가 Job args/env로 전달한 callback URL을 그대로 호출하면 된다. Design callback의 `generationId` 중심 구조와 유사하다. | case scope는 service가 내부 조회로 검증해야 한다. | 권장 |
| case/run 중첩 | `/api/v1/mi/fundamental-model/cases/{caseId}/runs/{runId}/callback` | callback URL만 봐도 case scope가 드러난다. | 외부 모델 URL이 FE/API resource hierarchy에 과하게 결합된다. path parameter mismatch 검증이 늘어난다. | 비권장 |

callback URL은 실행 요청 dispatch 시 backend가 생성해 Kubernetes Job args/env로 전달한다. pod 또는 pod 내부 실행 프로세스는 해당 URL을 사용해 실행 완료/실패 시 호출한다.

## 3. Request DTO Draft

### 3.1 Scenario Case 목록 조회

- Method: `GET`
- Path variables: 없음

- Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `searchType` | `FundamentalModelCaseSearchType` | 조건부 | 검색 대상 필드. `keyword`가 non-blank일 때 필수. `TITLE` \| `ASSIGNEE_NAME` |
| `keyword` | `String` | N | 검색 키워드. null 또는 trim 후 공백이면 검색 조건 없음으로 처리한다 |

규칙:

- canonical contract는 `non-blank keyword requires searchType`이다.
- `keyword`가 null 또는 blank(trim 후 빈 문자열)이면 검색 조건 없음으로 처리하고 전체 case를 반환한다. 이때 `searchType`이 함께 전달되어도 검색 조건 없음으로 본다.
- `keyword`가 non-blank인데 `searchType`이 없으면 `400 REQ_INVALID_PARAMETER`로 거절한다.
- 검색 조건이 없으면 전체 case를 반환한다. 1차 mock 계약은 `scenarioId` 필터를 제공하지 않는다.
- `TITLE` 검색은 case 제목에서 대소문자 구분 없이 부분 일치한다.
- `ASSIGNEE_NAME` 검색은 case 업로더 이름에서 대소문자 구분 없이 부분 일치한다.
- 페이지네이션 없이 전체 반환한다. 정렬 기준은 `createdAt` 내림차순(최신순)이다.

Acceptance criteria:

- Given `GET /cases`, when 검색 파라미터가 없으면, then 전체 case 목록을 반환한다.
- Given `GET /cases?keyword=`, when `keyword`가 빈 문자열이면, then 전체 case 목록을 반환한다.
- Given `GET /cases?searchType=TITLE&keyword=` 또는 `keyword`가 blank이면, then 전체 case 목록을 반환한다.
- Given `GET /cases?keyword=JKM`, when `keyword`가 non-blank이고 `searchType`이 없으면, then `400 REQ_INVALID_PARAMETER`로 거절한다.
- Given `GET /cases?searchType=TITLE&keyword=JKM`, when 둘 다 유효하면, then `TITLE` 기준 검색을 적용한다.

### 3.2 Scenario Case 업로드

- Method: `POST`
- Content-Type: `multipart/form-data`
- Path variables: 없음

- Multipart parts:

| Part name | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `title` | `String` | Y | trim 후 1-200자 | Scenario Case 제목 |
| `description` | `String` | N | 최대 2000자 권장 | Scenario Case 설명 |
| `files` | `MultipartFile[]` | Y | 정확히 3개, 각 파일 non-empty | 모델 실행에 필요한 입력 파일 3개 |

권장 DTO/handler 형태:

```text
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
createCase(
  @RequestParam("title") String title,
  @RequestParam(value = "description", required = false) String description,
  @RequestPart("files") List<MultipartFile> files
)
```

검증 규칙:

- `files.size() == 3` 이 아니면 거절한다.
- 파일 중 하나라도 비어 있으면 거절한다.
- 1차 mock에서는 확장자/MIME allowlist를 확정하지 않는다. 단, 구현 시 기존 `FileService` 정책을 재사용할 수 있으면 `FILE_FORMAT_ERROR`, `FILE_SIZE_ERROR`로 매핑한다.
- 같은 파일명 중복은 1차 mock에서는 허용한다. 운영 정책상 막아야 하면 후속 open question으로 남긴다.

### 3.3 Scenario Case 실행 요청

- Method: `POST`
- Content-Type: `application/json`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Request body: 없음

1차 기본 동작은 실행 요청 직후 상태를 `RUNNING`으로 두고 자동 완료시키지 않는다. 데모에서 완료/실패 전환이 필요하면 phase 1에 포함된 callback endpoint를 호출해 terminal 상태로 전이한다. 자동 완료 모드 (`AUTO_COMPLETE`, `AUTO_FAIL`) 같은 데모 편의 옵션은 1차 scope에서 제외한다.

실제 연동에서는 backend가 Kubernetes Job을 생성할 때 `runId`, 입력 파일 storage path 3개, output storage prefix, callback URL/auth 정보를 함께 전달한다. Kubernetes API의 Job 생성 성공은 장시간 실행 결과가 아니라 dispatch 접수 성공 여부만 표현한다. 접수 성공이면 backend는 case/run 상태를 `RUNNING`으로 유지하고 클라이언트에 `202 Accepted`를 반환한다. pod 내부 실행 완료 또는 실패는 pod 또는 실행 프로세스가 backend callback API를 호출해 terminal 상태로 전이한다. backend가 pod 내부 Python 완료 여부를 주기적으로 polling 하는 방식은 채택하지 않는다.

### 3.4 Scenario Case 상세 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters: 없음

규칙:

- 존재하는 case이면 case metadata, uploaded file metadata, latest run projection을 반환한다.
- `latestRunId`가 없거나 아직 실행되지 않은 `PENDING` case이면 `latestRun = null`이다.
- full run history는 반환하지 않는다.
- result payload, result reference, download URL은 반환하지 않는다.
- case가 없으면 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given seed case ID, when 상세 조회하면, then case metadata, files, latestRun을 반환한다.
- Given 업로드 직후 case, when 상세 조회하면, then `status = PENDING`, files 3개, `latestRun = null`을 반환한다.
- Given 실행 요청 후 case, when 상세 조회하면, then `status = RUNNING`, latestRun 정보를 반환한다.
- Given 존재하지 않는 caseId, when 상세 조회하면, then `404 RSC_NOT_FOUND`로 응답한다.
- Given soft-deleted caseId, when 상세 조회하면, then `404 RSC_NOT_FOUND`로 응답한다.

### 3.5 Scenario Case metadata 수정

- Method: `PATCH`
- Content-Type: `application/json`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Request DTO: `FundamentalModelCaseUpdateReq`

```json
{
  "title": "Updated JKM price upside case",
  "description": "Updated memo"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `title` | `String` | Y | trim 후 1-200자 | Scenario Case 제목. `tmd_scncasem.case_titl_nm`에 매핑 |
| `description` | `String` | N | nullable/optional | Scenario Case 설명. `tmd_scncasem.case_expn`에 매핑 |

규칙:

- request body는 `title`, `description`만 허용한다.
- `title`은 null, 누락, blank를 허용하지 않는다. 저장 전 trim한다.
- `description`은 null 또는 누락을 허용하며, 전달된 값을 `case_expn` equivalent에 반영한다.
- 수정 대상은 case metadata인 `case_titl_nm`, `case_expn`뿐이다.
- files, status, latestRun, run history, result payload/download 정보는 수정하지 않는다.
- 수정 응답은 최신 `FundamentalModelCaseDetailRes`를 반환한다.
- case가 없거나 soft-deleted 상태이면 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given active case, when `title`, `description`을 수정하면, then 최신 상세 응답에서 두 필드만 변경된다.
- Given active case with files/latestRun/status, when 수정하면, then files/latestRun/status는 유지된다.
- Given blank title, when 수정하면, then `400 REQ_INVALID_PARAMETER`로 거절한다.
- Given soft-deleted caseId, when 수정하면, then `404 RSC_NOT_FOUND`로 응답한다.

### 3.6 Scenario Case 삭제

- Method: `DELETE`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Request body: 없음
- Response: `BaseResponse<Void>` (`200 OK`)

규칙:

- 삭제는 physical delete가 아니라 `tmd_scncasem.del_yn = 'Y'` soft delete semantics로 처리한다.
- phase-1 mock 구현은 record에 `delYn`을 두고 `Y`로 갱신해 DB soft-delete 동작을 반영한다.
- 삭제 시 upload files, run history, latestRun/result payload는 물리 삭제하지 않는다.
- 삭제된 case는 list/detail/status/run/update/delete에서 active resource가 아니므로 `404 RSC_NOT_FOUND`로 취급한다.
- 반복 delete는 이미 active resource가 아니므로 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given active case, when 삭제하면, then `200 OK`와 `BaseResponse<Void>`를 반환한다.
- Given deleted case, when 목록 조회하면, then 목록에서 제외된다.
- Given deleted case, when detail/status/run/update/delete를 호출하면, then `404 RSC_NOT_FOUND`로 응답한다.

### 3.7 Scenario Case 상태 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters: 없음

상태 조회는 polling용 경량 projection이다. 상세 조회와 달리 `description`, `assigneeName`, `files`를 반환하지 않는다.

### 3.8 Mock 데이터 초기화

- Method: `POST`
- Path variables: 없음

- Request body: 없음

Reset API 포함 판단:

- 1차 개발/시연에서는 같은 브라우저 세션에서 list/upload/run/status를 반복 검증해야 하므로 포함을 권장한다.
- 운영 배포에서는 [`FundamentalModelMockProperties`](src/main/java/com/sk/domain/mi/config/FundamentalModelMockProperties.java) 의 reset 전용 flag (`mi.fundamental-model.mock.fundamental-model-cases.api.reset`) 를 **controller** 에서 직접 확인해 `false` 일 때 `404 Not Found` 로 차단한다. `@Profile("mock")` 같은 전체 프로필 분기는 사용하지 않는다.
- path와 summary에 `[mockapi 전용]` 을 명시한다.

### 3.9 외부 모델 완료 Callback

- Method: `POST`
- Content-Type: `application/json`
- Path variable:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `runId` | `String` | Y | backend가 `yyyymmdd_hhmmss_XXXX` 포맷으로 발급해 외부 모델에 전달한 실행 추적 ID |

- Request body:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | `String` | Y | `COMPLETED | FAILED` 만 허용 |
| `finishedAt` | `LocalDateTime` | Y | 외부 모델 기준 실행 완료/실패 시각 |
| `failureReason` | `String` | N | `FAILED`일 때 권장. `EXTERNAL_ERROR | UNKNOWN` 후보. `TIMEOUT`은 scheduler가 설정하는 전용 값으로 callback payload에서는 허용하지 않는다 |
| `errorMessage` | `String` | N | 실패 상세 메시지. `FAILED`이면 권장 |

규칙:

- backend는 결과 데이터와 결과 파일을 저장하지 않는다.
- callback은 실행 상태를 terminal 상태로 전이하는 용도이며, 결과 payload 또는 결과 pointer/reference를 저장하지 않는다.
- 후속으로 결과 조회/download가 필요해지면 별도 API 계약과 persistence 검토에서 result pointer/reference를 다시 설계한다.

### 3.10 Volumes 필터 옵션 조회

- Method: `GET`
- Path: `/api/v1/mi/fundamental-model/cases/volumes/filter-options`
- Path variables: 없음
- Query parameters: 없음

규칙:

- case 독립 정적 endpoint다. caseId 없이 호출한다.
- segment별로 허용되는 type 목록, region 목록, granularity 목록을 `{ value, label }` 쌍으로 반환한다.
- TRADE segment의 `types`는 `TradeFlowDirection` 값(`TO_COUNTRY`, `FROM_COUNTRY`)이며, `granularities`는 빈 배열이다.
- GLOBAL/LNG segment의 `types`는 `FundamentalModelDataType` 값이며, `granularities`는 `MONTHLY`, `YEARLY`를 반환한다.
- 항상 GLOBAL → LNG → TRADE 순서로 반환한다.
- mock/real 구분 없이 enum 기반 정적 응답이다.

### 3.11 Volumes 데이터 조회 (Global / LNG)

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `segment` | `FundamentalModelSegment` | Y | — | `GLOBAL` \| `LNG`. `TRADE`는 허용하지 않는다 |
| `types` | `FundamentalModelDataType[]` | N | segment별 전체 | 쉼표 구분 다중값. 미전달 시 segment별 허용 Type 전체 포함. `GLOBAL`: `DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`; `LNG`: `LNG_IMPORTS`, `LNG_EXPORTS` |
| `regions` | `FundamentalModelRegion[]` | N | 전체 | 쉼표 구분 다중값. 미전달 시 전체 포함. `ASIA`, `EUROPE`, `NORTH_AMERICA`, `MIDDLE_EAST`, `AUSTRALIA`, `RUSSIA`, `AFRICA`, `SOUTH_AMERICA` |
| `granularity` | `FundamentalModelGranularity` | N | `MONTHLY` | `MONTHLY` \| `YEARLY` |

규칙:

- `caseId`에 해당하는 case가 `COMPLETED` 상태여야 한다. `PENDING` 또는 `RUNNING`이면 `409 BIZ_RULE_VIOLATION`, `FAILED` 또는 soft-deleted이면 `404 RSC_NOT_FOUND`로 응답한다.
- `segment=TRADE`가 전달되면 `400 REQ_INVALID_PARAMETER`로 응답한다. Trade Flows 조회는 `GET /{caseId}/volumes/trade`를 사용한다.
- `types`, `regions` 모두 미전달 시 segment별 허용 Type과 전체 Region 데이터를 반환한다.
- `segment=LNG`에서 `DEMAND`, `PRODUCTION`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`가 `types`로 전달되면 `400 REQ_INVALID_PARAMETER`로 응답한다.
- `REGION_SUBTOTAL` 행은 선택된 Region별 subtotal을 나타낸다.
- `TYPE_TOTAL` 행은 선택된 Region들의 `REGION_SUBTOTAL` 값을 합산한 Type별 total을 나타낸다. 모든 Region이 선택되면 전체 Region 기준 Type total과 같다.
- FE는 `rows`를 응답 순서 그대로 렌더링할 수 있다. 서버는 각 Type 안에서 Region별로 `REGION_SUBTOTAL` 행을 먼저 반환하고, 이어서 해당 Region의 `COUNTRY` 행들을 반환한다. Type의 마지막 행은 `TYPE_TOTAL`이다.
- `granularity=YEARLY`이면 월별 데이터를 연간으로 집계해 반환한다. columns의 `month` 필드는 `null`이 된다.
- `granularity=MONTHLY`이면 columns의 `month` 필드는 1~12 정수다.
- 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환한다. 필터는 mock 데이터 내에서 적용한다.

### 3.12 Price 데이터 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters: 없음 (필터 없음, segment 없음, granularity 없음)

규칙:

- `caseId`에 해당하는 case가 `COMPLETED` 상태여야 한다. 상태 조건은 §3.10과 동일하다.
- 응답은 차트 데이터, Annual 테이블, Monthly 테이블을 단일 응답으로 동시에 반환한다.
- 차트는 `BASE_CASE`와 `LAST_ADJUSTED` 두 series를 포함한다.
- Annual 테이블과 Monthly 테이블의 행 `label`은 데이터 기반 동적 값이다. 서버가 고정 enum으로 강제하지 않는다.
- 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환한다.


### 3.13 Trade Flows 데이터 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `selector` | `TradeFlowSelector` | Y | — | `LNG` \| `PNG` |
| `year` | `Integer` | Y | — | 조회 연도. 2023~2030 범위 |
| `unit` | `TradeFlowUnit` | N | `MTPA` | `MTPA` \| `BCM` |
| `types` | `TradeFlowDirection[]` | N | 전체 | `TO_COUNTRY` \| `FROM_COUNTRY`. 미전달 시 전체. 지역 필터 적용 방향을 결정한다 |
| `regions` | `FundamentalModelRegion[]` | N | 전체 | 쉼표 구분 다중값. 미전달 시 전체 포함 |

규칙:

- `caseId`에 해당하는 case가 `COMPLETED` 상태여야 한다. 상태 조건은 §3.10과 동일하다.
- `selector`, `year`는 필수다. 미전달 시 `400 REQ_INVALID_PARAMETER`.
- `year`가 2023~2030 범위 밖이면 `400 REQ_INVALID_PARAMETER`.
- `types`와 `regions`를 함께 전달하면 direction에 따라 fromCountries/toCountries 헤더와 flows를 필터링한다.
  - `types=TO_COUNTRY` + `regions=ASIA`: toCountries를 ASIA로 축소, fromCountries는 전체 유지
  - `types=FROM_COUNTRY` + `regions=ASIA`: fromCountries를 ASIA로 축소, toCountries는 전체 유지
  - `types=TO_COUNTRY,FROM_COUNTRY` + `regions=ASIA`: 양방향 모두 ASIA로 축소
- `toRegion=null`인 Total 행 flows는 region 필터와 무관하게 항상 포함된다.
- flows는 0이 아닌 값만 포함한다 (sparse). fromCountries × toCountries 매트릭스에서 값이 없는 셀은 프론트가 0(또는 `-`)으로 처리한다.
- 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환한다. `selector`/`year` 값은 응답에 echo하며 필터는 seed 데이터 내에서 적용한다.

## 4. Response DTO Draft

모든 응답은 `BaseResponse<T>`를 사용한다. 성공 시 `status=true`, `code=SUC-000`, `message=Success.`, `data=<DTO>` 형태를 따른다.

### 4.1 `FundamentalModelCaseListRes`

```json
{
  "totalCount": 5,
  "cases": [
    {
      "caseId": 105,
      "title": "시뮬레이션 케이스 5",
      "status": "RUNNING",
      "assigneeName": "홍길동",
      "createdAt": "2026-05-19T15:00:00",
      "updatedAt": "2026-05-19T15:05:00"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `totalCount` | `Integer` | 필터 적용 후 전체 case 수. 페이지네이션 없이 전체 반환이므로 `cases.size()`와 동일하다 |
| `cases` | `List<FundamentalModelCaseListItemRes>` | case 목록. `createdAt` 내림차순(최신순) 정렬 |

### 4.2 `FundamentalModelCaseListItemRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `title` | `String` | case 제목 |
| `status` | `FundamentalModelCaseStatus` | case 최신 상태. `PENDING`, `RUNNING`, `COMPLETED`, `FAILED` |
| `assigneeName` | `String` | case를 업로드한 사용자 이름. mock에서는 고정값 사용 |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `updatedAt` | `LocalDateTime` | case 최종 변경 시각 |

`latestRun` 정보는 목록 항목에 포함하지 않는다. 실행 상세(runId, startedAt 등)가 필요하면 단건 상태 조회 API(`GET /{caseId}/status`)를 사용한다.

### 4.3 `FundamentalModelCaseDetailRes`

```json
{
  "caseId": 101,
  "title": "JKM price upside case",
  "description": "May demand shock scenario case",
  "status": "RUNNING",
  "assigneeName": "홍길동",
  "assigneeUserNo": 1,
  "files": [
    {
      "fileNo": 1011,
      "fileIndex": 1,
      "originalFileName": "input_1.xlsx",
      "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "size": 12345
    }
  ],
  "latestRun": {
    "runId": "20260521_143500_A1B2",
    "status": "RUNNING",
    "requestedAt": "2026-05-19T14:35:00",
    "startedAt": "2026-05-19T14:35:00",
    "finishedAt": null,
    "failureReason": null,
    "errorMessage": null
  },
  "createdAt": "2026-05-19T14:30:00",
  "updatedAt": "2026-05-19T14:35:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `title` | `String` | case 제목 |
| `description` | `String` | case 설명 |
| `status` | `FundamentalModelCaseStatus` | case 최신 상태 |
| `assigneeName` | `String` | case를 업로드한 사용자 이름. mock에서는 고정값 사용 |
| `assigneeUserNo` | `Integer` | case를 업로드한 사용자 번호. DB-backed 구현에서는 `tmd_scncasem.usr_no` |
| `files` | `List<FundamentalModelCaseFileRes>` | 업로드 파일 metadata |
| `latestRun` | `FundamentalModelRunStatusRes` | 최신 실행 정보. `PENDING`이면 `null` |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `updatedAt` | `LocalDateTime` | case 최종 변경 시각 |

상세 응답은 최신 실행 1건만 포함한다. 전체 실행 이력은 별도 runs API가 정의되기 전까지 이 응답에 포함하지 않는다. result payload와 result download 정보도 포함하지 않는다.

### 4.4 `FundamentalModelCaseCreateRes`

```json
{
  "caseId": 101,
  "title": "JKM price upside case",
  "description": "May demand shock scenario case",
  "status": "PENDING",
  "files": [
    {
      "fileNo": 1011,
      "fileIndex": 1,
      "originalFileName": "input_1.xlsx",
      "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "size": 12345
    }
  ],
  "createdAt": "2026-05-19T14:30:00",
  "statusEndpoint": "/api/v1/mi/fundamental-model/cases/101/status"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | mock store가 생성한 case ID. future DB는 `tmd_scncasem.case_id` 후보 |
| `title` | `String` | 제목 |
| `description` | `String` | 설명. 입력 없으면 `null`. 기존 NextGen mock의 optional 문자열 필드 패턴과 동일. 향후 DB 전환 시 `tmd_scncasem.case_expn TEXT NULL` 과 자연스럽게 매핑됨 |
| `status` | `FundamentalModelCaseStatus` | 생성 직후 `PENDING` |
| `files` | `List<FundamentalModelCaseFileRes>` | 업로드 파일 3개 metadata |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `statusEndpoint` | `String` | case 기준 상태 조회 endpoint |

### 4.5 `FundamentalModelCaseFileRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `fileNo` | `Long` | 파일 식별자. canonical contract는 DB-backed 기준 `tcm_filel.file_no`를 사용한다. mock 구현도 같은 필드명으로 응답한다 |
| `fileIndex` | `Integer` | 1부터 시작하는 업로드 순서 |
| `originalFileName` | `String` | 원본 파일명 |
| `contentType` | `String` | multipart content type |
| `size` | `Long` | 파일 크기 byte |

### 4.6 `FundamentalModelRunAcceptRes`

```json
{
  "caseId": 101,
  "runId": "20260521_143500_A1B2",
  "status": "RUNNING",
  "requestedAt": "2026-05-19T14:35:00",
  "startedAt": "2026-05-19T14:35:00",
  "statusEndpoint": "/api/v1/mi/fundamental-model/cases/101/status"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | 실행 대상 case ID |
| `runId` | `String` | backend/mock store가 생성한 `yyyymmdd_hhmmss_XXXX` 포맷 실행 ID. future DB는 `tmd_scncaseh.run_id` |
| `status` | `FundamentalModelRunStatus` | 실행 접수 직후 `RUNNING` |
| `requestedAt` | `LocalDateTime` | 실행 요청 시각 |
| `startedAt` | `LocalDateTime` | mock 실행 시작 시각. 1차에서는 `requestedAt`과 동일 |
| `statusEndpoint` | `String` | case 기준 상태 조회 endpoint |

### 4.7 `FundamentalModelCaseStatusRes`

```json
{
  "caseId": 101,
  "title": "JKM price upside case",
  "status": "RUNNING",
  "latestRun": {
    "runId": "20260521_143500_A1B2",
    "status": "RUNNING",
    "requestedAt": "2026-05-19T14:35:00",
    "startedAt": "2026-05-19T14:35:00",
    "finishedAt": null,
    "failureReason": null,
    "errorMessage": null
  },
  "createdAt": "2026-05-19T14:30:00",
  "updatedAt": "2026-05-19T14:35:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `title` | `String` | case 제목 |
| `status` | `FundamentalModelCaseStatus` | case 최신 상태 |
| `latestRun` | `FundamentalModelRunStatusRes` | 최신 실행 정보. `PENDING`이면 `null` |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `updatedAt` | `LocalDateTime` | case 최종 변경 시각 |

### 4.8 `FundamentalModelRunStatusRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `runId` | `String` | `yyyymmdd_hhmmss_XXXX` 포맷의 실행 추적 ID |
| `status` | `FundamentalModelRunStatus` | 실행 상태. `RUNNING`, `COMPLETED`, `FAILED` 만 가질 수 있다 |
| `requestedAt` | `LocalDateTime` | 실행 요청 시각 |
| `startedAt` | `LocalDateTime` | 실행 시작 시각. future DB `tmd_scncaseh.exec_bgn_dtm` 매핑 |
| `finishedAt` | `LocalDateTime` | 완료/실패 시각. future DB `tmd_scncaseh.exec_end_dtm` 매핑. 실행 중이면 `null` |
| `failureReason` | `String` | `TIMEOUT`, `EXTERNAL_ERROR`, `UNKNOWN` 후보. 실패 전에는 `null` |
| `errorMessage` | `String` | 실패 상세 메시지. 실패 전에는 `null` |

`FundamentalModelRunStatus` 는 `FundamentalModelCaseStatus` 와 분리한다. case status는 `PENDING/RUNNING/COMPLETED/FAILED` 를 가지지만 run status는 `PENDING` 을 가지지 않는다. 두 도메인을 같은 enum으로 묶으면 컴파일 타임에 `PENDING` 값이 run status로 흘러드는 실수를 막을 수 없으므로 별도 enum으로 분리한다.

FE는 `latestRun.startedAt`과 `latestRun.finishedAt`으로 소요시간을 계산한다. API는 표시용 duration 문자열 또는 elapsed seconds를 별도 필드로 제공하지 않는다.

### 4.9 `FundamentalModelRunCallbackRes`

```json
{
  "runId": "20260521_143500_A1B2",
  "caseId": 101,
  "status": "COMPLETED",
  "processedAt": "2026-05-19T14:40:00",
  "idempotentReplay": false
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `runId` | `String` | callback 대상 run ID. `yyyymmdd_hhmmss_XXXX` 포맷 |
| `caseId` | `Long` | 연결된 case ID |
| `status` | `FundamentalModelRunStatus` | callback 처리 후 run terminal 상태 (`COMPLETED` 또는 `FAILED`). callback 대상이 case latest run인 경우 case 최신 상태도 같은 값으로 동기화된다 |
| `processedAt` | `LocalDateTime` | backend callback 처리 시각 |
| `idempotentReplay` | `Boolean` | 동일 terminal payload 재전송을 no-op으로 처리했으면 `true` |

### 4.10 `FundamentalModelCaseUpdateReq`

```json
{
  "title": "Updated JKM price upside case",
  "description": "Updated memo"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `title` | `String` | 수정할 case 제목. trim 후 1-200자 필수 |
| `description` | `String` | 수정할 case 설명. nullable/optional |

수정 응답은 별도 update response DTO를 만들지 않고 `FundamentalModelCaseDetailRes`를 그대로 반환한다.

### 4.11 `FundamentalModelVolumesRes`

```json
{
  "caseId": 101,
  "segment": "GLOBAL",
  "granularity": "MONTHLY",
  "unit": "MTPA",
  "appliedFilters": {
    "types": ["DEMAND", "PRODUCTION"],
    "regions": ["ASIA", "EUROPE"]
  },
  "columns": [
    { "key": "2026-01", "label": "26-01", "year": 2026, "month": 1 },
    { "key": "2026-02", "label": "26-02", "year": 2026, "month": 2 }
  ],
  "rows": [
    {
      "type": "DEMAND",
      "typeLabel": "Demand",
      "region": "ASIA",
      "country": null,
      "rowKind": "REGION_SUBTOTAL",
      "rowLabel": "Asia",
      "values": { "2026-01": 0.3, "2026-02": 0.3 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Demand",
      "region": "ASIA",
      "country": "China",
      "rowKind": "COUNTRY",
      "rowLabel": "China",
      "values": { "2026-01": 15.1, "2026-02": 0.3 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Demand",
      "region": "ASIA",
      "country": "Japan",
      "rowKind": "COUNTRY",
      "rowLabel": "Japan",
      "values": { "2026-01": 0.2, "2026-02": 0.2 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Demand",
      "region": null,
      "country": null,
      "rowKind": "TYPE_TOTAL",
      "rowLabel": "Total",
      "values": { "2026-01": 344.8, "2026-02": 344.8 }
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `segment` | `FundamentalModelSegment` | 조회한 segment |
| `granularity` | `FundamentalModelGranularity` | 조회한 시간 단위 |
| `unit` | `FundamentalModelVolumeUnit` | 수치 단위. phase 1에서는 `MTPA` 고정 |
| `appliedFilters` | `VolumesAppliedFilters` | 실제 적용된 필터. 미전달 시 전체 enum 목록 |
| `columns` | `List<VolumesColumn>` | 시간 축 메타데이터 목록 |
| `rows` | `List<VolumesRow>` | 데이터 행 목록 |

**`VolumesColumn`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `key` | `String` | 안정적인 기간 식별자. `MONTHLY`이면 `"2026-01"` 형식, `YEARLY`이면 `"2026"` 형식 |
| `label` | `String` | 표시 라벨. `MONTHLY`이면 `"26-01"` 형식, `YEARLY`이면 `"2026"` 형식 |
| `year` | `Integer` | 연도 |
| `month` | `Integer` | 월(1~12). `YEARLY`이면 `null` |

**`VolumesRow`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `type` | `FundamentalModelDataType` | 데이터 타입. `COUNTRY`, `REGION_SUBTOTAL`, `TYPE_TOTAL` 모두 해당 Type 값을 가진다 |
| `typeLabel` | `String` | Type section 화면 표시명. 예: `Demand`, `Production`, `LNG Imports` |
| `region` | `FundamentalModelRegion` | 지역. `rowKind=TYPE_TOTAL`이면 `null` |
| `country` | `String` | 국가명. `rowKind=REGION_SUBTOTAL` 또는 `TYPE_TOTAL`이면 `null` |
| `rowKind` | `VolumesRowKind` | `COUNTRY` \| `REGION_SUBTOTAL` \| `TYPE_TOTAL` |
| `rowLabel` | `String` | 행 화면 표시명. `REGION_SUBTOTAL`이면 지역 label, `COUNTRY`이면 국가명, `TYPE_TOTAL`이면 `Total` |
| `values` | `Map<String, Double>` | column key별 수치 map. key는 `columns[].key`와 동일하며 FE는 `row.values[column.key]`로 조회한다 |

표시용 text는 `typeLabel`, `rowLabel`을 사용한다. `type`, `region`, `country`, `rowKind`는 필터/집계/스타일 분기용 canonical code 필드로 유지한다. Rowspan은 응답 순서에서 연속된 동일 `type` 구간을 기준으로 FE가 계산한다.

테이블 상단 헤더는 `columns[].label`을 순서대로 표시한다. `MONTHLY`이면 `"26-01"` 형식, `YEARLY`이면 `"2026"` 형식이다. 각 row의 `values` object key는 반환된 `columns[].key` 집합과 동일해야 한다.

`rows` 배열은 FE가 응답 순서 그대로 렌더링할 수 있도록 정렬된다. 정렬 순서는 선택된 `types` 순서대로 Type section을 만들고, 각 Type section 안에서는 선택된 `regions` 순서대로 `REGION_SUBTOTAL` 행을 먼저 반환한 뒤 해당 Region의 `COUNTRY` 행들을 반환한다. 각 Type section의 마지막 행은 `TYPE_TOTAL`이다.

**`VolumesRowKind` enum:**

| 값 | 의미 |
|---|---|
| `COUNTRY` | 국가 단위 행 |
| `REGION_SUBTOTAL` | 지역 소계 행 (화면의 bold 지역 합계 행) |
| `TYPE_TOTAL` | Type 합계 행. 선택된 Region들의 `REGION_SUBTOTAL` 합산값 (화면의 "Total" 행) |

**`FundamentalModelDataType` enum:**
`DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`

**`FundamentalModelRegion` enum:**
`ASIA`, `EUROPE`, `NORTH_AMERICA`, `MIDDLE_EAST`, `AUSTRALIA`, `RUSSIA`, `AFRICA`, `SOUTH_AMERICA`

**`FundamentalModelSegment` enum:**
`GLOBAL`, `LNG`, `TRADE` — Volumes API(`GET /volumes`)에서 `TRADE`는 허용하지 않는다. `TRADE` segment 조회는 `GET /volumes/trade` 사용.

**`FundamentalModelGranularity` enum:**
`MONTHLY`, `YEARLY`

**`FundamentalModelVolumeUnit` enum:**
`MTPA` — phase 1에서는 Volumes 응답의 `unit`이 항상 `MTPA`다. 후속 unit 변환 기능은 별도 request contract로 정의한다.

### 4.12 `FundamentalModelVolumesFilterOptionsRes`

```json
{
  "segments": [
    {
      "segment": "GLOBAL",
      "types": [
        { "value": "DEMAND",           "label": "Demand" },
        { "value": "PRODUCTION",       "label": "Production" },
        { "value": "LNG_IMPORTS",      "label": "LNG Imports" },
        { "value": "LNG_EXPORTS",      "label": "LNG Exports" },
        { "value": "PIPELINE_IMPORTS", "label": "Pipeline Imports" },
        { "value": "PIPELINE_EXPORTS", "label": "Pipeline Exports" }
      ],
      "regions": [
        { "value": "ASIA",          "label": "Asia" },
        { "value": "EUROPE",        "label": "Europe" },
        { "value": "NORTH_AMERICA", "label": "North America" },
        { "value": "MIDDLE_EAST",   "label": "Middle East" },
        { "value": "AUSTRALIA",     "label": "Australia" },
        { "value": "RUSSIA",        "label": "Russia" },
        { "value": "AFRICA",        "label": "Africa" },
        { "value": "SOUTH_AMERICA", "label": "South America" }
      ],
      "granularities": [
        { "value": "MONTHLY", "label": "Monthly" },
        { "value": "YEARLY",  "label": "Yearly" }
      ]
    },
    {
      "segment": "LNG",
      "types": [
        { "value": "LNG_IMPORTS", "label": "LNG Imports" },
        { "value": "LNG_EXPORTS", "label": "LNG Exports" }
      ],
      "regions": [ "/* 동일 */" ],
      "granularities": [
        { "value": "MONTHLY", "label": "Monthly" },
        { "value": "YEARLY",  "label": "Yearly" }
      ]
    },
    {
      "segment": "TRADE",
      "types": [
        { "value": "TO_COUNTRY",   "label": "To country" },
        { "value": "FROM_COUNTRY", "label": "From country" }
      ],
      "regions": [ "/* 동일 */" ],
      "granularities": []
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `segments` | `List<VolumesSegmentFilterOptions>` | segment별 필터 옵션 목록. 항상 GLOBAL → LNG → TRADE 순서 |

**`VolumesSegmentFilterOptions`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `segment` | `FundamentalModelSegment` | 탭 구분자 |
| `types` | `List<FilterOption>` | 해당 segment에서 선택 가능한 type 목록. TRADE는 `TradeFlowDirection` 값 |
| `regions` | `List<FilterOption>` | 선택 가능한 region 목록. 모든 segment 동일 |
| `granularities` | `List<FilterOption>` | 선택 가능한 granularity 목록. TRADE는 빈 배열 |

**`FilterOption`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `value` | `String` | API 파라미터 전송값 (예: `LNG_IMPORTS`) |
| `label` | `String` | 화면 표시명 (예: `LNG Imports`) |

### 4.13 `FundamentalModelPriceRes`

```json
{
  "caseId": 101,
  "chart": {
    "series": [
      {
        "seriesType": "BASE_CASE",
        "label": "Base case price",
        "points": [
          { "year": 2023, "month": 1, "value": 8.51 },
          { "year": 2023, "month": 2, "value": 8.65 }
        ]
      },
      {
        "seriesType": "LAST_ADJUSTED",
        "label": "Last adjusted price",
        "points": [
          { "year": 2023, "month": 1, "value": 9.02 },
          { "year": 2023, "month": 2, "value": 9.15 }
        ]
      }
    ]
  },
  "annualTable": {
    "columns": [
      { "year": 2023 },
      { "year": 2024 }
    ],
    "rows": [
      { "label": "Oak Area Spot",  "values": [75.71, 8.56] },
      { "label": "Took Area Spot", "values": [12.50, 9.03] }
    ]
  },
  "monthlyTable": {
    "columns": [
      { "year": 2023, "month": 1, "periodLabel": "2026-1" },
      { "year": 2023, "month": 2, "periodLabel": "2026-2" }
    ],
    "rows": [
      { "label": "Oak Area Spot",  "values": [8.51, 8.63] },
      { "label": "Took Area Spot", "values": [9.02, 9.11] }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `chart` | `PriceChart` | Monthly Futures Shoulder Chart용 라인 그래프 데이터 |
| `annualTable` | `PriceTable` | 연간 가격 테이블 |
| `monthlyTable` | `PriceTable` | 월별 가격 테이블 |

**`PriceChart`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `series` | `List<PriceChartSeries>` | 차트 시리즈 목록. 항상 `BASE_CASE`, `LAST_ADJUSTED` 2개 |

**`PriceChartSeries`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `seriesType` | `PriceSeriesType` | `BASE_CASE` \| `LAST_ADJUSTED` |
| `label` | `String` | 범례 표시 이름 |
| `points` | `List<PriceChartPoint>` | 시간 순서 데이터 포인트 |

**`PriceChartPoint`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `year` | `Integer` | 연도 |
| `month` | `Integer` | 월(1~12) |
| `value` | `Double` | 가격 값 |

**`PriceTable`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `columns` | `List<PriceColumn>` | 시간 축 메타데이터 |
| `rows` | `List<PriceRow>` | 데이터 행 목록 |

**`PriceColumn`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `year` | `Integer` | 연도 |
| `month` | `Integer` | 월(1~12). `annualTable`에서는 `null` |
| `periodLabel` | `String` | 표시 라벨. `monthlyTable`에서는 `"2026-1"` 형식, `annualTable`에서는 `null` |

**`PriceRow`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `label` | `String` | 행 이름. 데이터 기반 동적 값 (예: `"Oak Area Spot"`). 서버가 고정 enum으로 강제하지 않는다 |
| `values` | `List<Double>` | `columns` 순서와 1:1 대응하는 수치 목록 |


### 4.14 `FundamentalModelTradeFlowsRes`

```json
{
  "caseId": 106,
  "selector": "LNG",
  "year": 2026,
  "unit": "MTPA",
  "appliedFilters": {
    "types": ["TO_COUNTRY", "FROM_COUNTRY"],
    "regions": ["ASIA", "AFRICA"]
  },
  "fromCountries": [
    { "key": "REGION:AFRICA", "label": "Africa", "kind": "REGION_TOTAL", "region": "AFRICA", "country": null },
    { "key": "COUNTRY:AFRICA:Angola", "label": "Angola", "kind": "COUNTRY", "region": "AFRICA", "country": "Angola" },
    { "key": "REGION:NORTH_AMERICA", "label": "America N", "kind": "REGION_TOTAL", "region": "NORTH_AMERICA", "country": null },
    { "key": "COUNTRY:NORTH_AMERICA:United States", "label": "United States", "kind": "COUNTRY", "region": "NORTH_AMERICA", "country": "United States" }
  ],
  "toCountries": [
    { "key": "REGION:AFRICA", "label": "Africa", "kind": "REGION_TOTAL", "region": "AFRICA", "country": null },
    { "key": "COUNTRY:AFRICA:Senegal", "label": "Senegal", "kind": "COUNTRY", "region": "AFRICA", "country": "Senegal" },
    { "key": "REGION:ASIA", "label": "Asia", "kind": "REGION_TOTAL", "region": "ASIA", "country": null },
    { "key": "COUNTRY:ASIA:Bangladesh", "label": "Bangladesh", "kind": "COUNTRY", "region": "ASIA", "country": "Bangladesh" },
    { "key": "TOTAL", "label": "Total", "kind": "GRAND_TOTAL", "region": null, "country": "Total" }
  ],
  "flows": [
    { "fromKey": "COUNTRY:AFRICA:Angola", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.2894 },
    { "fromKey": "COUNTRY:NORTH_AMERICA:United States", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "NORTH_AMERICA", "fromCountry": "United States", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.4780 },
    { "fromKey": "COUNTRY:AFRICA:Angola", "toKey": "TOTAL", "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": null, "toCountry": "Total", "value": 0.2894 }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `caseId` | `Long` | case ID |
| `selector` | `TradeFlowSelector` | 조회한 Trade 유형 |
| `year` | `Integer` | 조회 연도 |
| `unit` | `TradeFlowUnit` | 조회한 수치 단위 |
| `appliedFilters` | `TradeFlowsAppliedFilters` | 실제 적용된 필터. 미전달 시 전체 enum 목록 |
| `fromCountries` | `List<TradeCountryHeader>` | From country 축 헤더 목록 (컬럼). 필터 적용 후 순서 보장 |
| `toCountries` | `List<TradeCountryHeader>` | To country 축 헤더 목록 (행). 마지막 항목은 Total(`region=null`) |
| `flows` | `List<TradeFlowItem>` | 0이 아닌 trade flow 값 목록 (sparse). 매트릭스에서 없는 셀은 0으로 처리 |

**`TradeCountryHeader`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `key` | `String` | 행/열 식별자. FE lookup key로 사용한다. 예: `REGION:AFRICA`, `COUNTRY:AFRICA:Angola`, `TOTAL` |
| `label` | `String` | 화면 표시명. 지역 집계는 region label, 개별 국가는 country, 합계는 `Total` |
| `kind` | `TradeCountryHeaderKind` | 헤더 유형. `REGION_TOTAL`, `COUNTRY`, `GRAND_TOTAL` |
| `region` | `FundamentalModelRegion` | 지역. `null`이면 Total 행을 나타낸다 |
| `country` | `String` | 국가명. `null`이면 해당 지역의 집계 행/열이다 |

프론트 렌더링 규칙:
- `kind == "REGION_TOTAL"` → **볼드** 지역 집계 행/열 (예: "Africa")
- `kind == "COUNTRY"` → 개별 국가 행/열 (예: "Angola")
- `kind == "GRAND_TOTAL"` → Total 합계 행 (항상 toCountries 마지막)
- 셀 lookup은 `flows`를 `(fromKey, toKey)` 기준으로 map 구성하고, 누락 셀은 0으로 처리한다

**`TradeFlowItem`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `fromKey` | `String` | From country 헤더 식별자. `fromCountries[].key`를 참조한다 |
| `toKey` | `String` | To country 헤더 식별자. `toCountries[].key`를 참조한다 |
| `fromRegion` | `FundamentalModelRegion` | 수출 지역 |
| `fromCountry` | `String` | 수출 국가. `null`이면 지역 집계 |
| `toRegion` | `FundamentalModelRegion` | 수입 지역. `null`이면 Total 행 |
| `toCountry` | `String` | 수입 국가. `null`이면 지역 집계. `"Total"`이면 Total 합계 행 |
| `value` | `Double` | 물량 |

**신규 enum:**

**`TradeFlowSelector`:** `LNG`, `PNG`

**`TradeFlowUnit`:** `MTPA`, `BCM`

**`TradeFlowDirection`:** `TO_COUNTRY`, `FROM_COUNTRY`

**`TradeCountryHeaderKind`:** `REGION_TOTAL`, `COUNTRY`, `GRAND_TOTAL`

## 5. Representative Error Scenarios

현재 `ResultCode`에는 전용 state conflict enum이 활성화되어 있지 않다. 따라서 1차 구현은 기존 enum을 우선 사용하고, 필요 시 후속으로 더 구체적인 enum 추가를 검토한다.

| 시나리오 | HTTP status | ResultCode 후보 | 처리 |
|---|---|---|---|
| `keyword`가 non-blank인데 `searchType` 없음 | `400` | `REQ_INVALID_PARAMETER` | canonical contract는 `non-blank keyword requires searchType` |
| `searchType`이 있고 `keyword`가 null 또는 blank | `200` | `SUC-000` | 검색 조건 없음으로 정규화해 전체 목록 반환 |
| `searchType`이 허용값 외 문자열 | `400` | `REQ_INVALID_PARAMETER` | enum 변환 실패 |
| `caseId`가 없거나 존재하지 않음 | `404` | `RSC_NOT_FOUND` | 존재하지 않는 case 리소스로 처리 |
| 삭제된 `caseId`로 list 외 active-resource API 호출 | `404` | `RSC_NOT_FOUND` | detail/status/run/update/delete 모두 active resource가 아니므로 not found |
| title 누락 또는 blank | `400` | `REQ_INVALID_PARAMETER` | Bean Validation 또는 수동 검증 |
| title 200자 초과 | `400` | `REQ_INVALID_PARAMETER` | future DB `case_titl_nm VARCHAR(200)` 정합성 |
| 파일 part 누락 | `400` | `REQ_MISSING_PARAMETER` 또는 `REQ_INVALID_PARAMETER` | handler가 missing part를 잡으면 공통 handler는 `REQ_INVALID_PARAMETER` 가능 |
| 파일 개수가 3개가 아님 | `400` | `REQ_INVALID_PARAMETER` | 메시지에 expected/actual count 포함 |
| 빈 파일 포함 | `400` | `FILE_INVALID` | 각 file `isEmpty()` 검증 |
| 파일 크기 제한 초과 | `400` | `FILE_SIZE_ERROR` | 제한값은 구현 설정 확인 필요 |
| 허용하지 않는 파일 형식 | `400` | `FILE_FORMAT_ERROR` | allowlist 확정 후 적용 |
| 이미 `RUNNING`인 case 실행 요청 | `409` 권장 | `BIZ_RULE_VIOLATION` 1차 후보, 후속 `BIZ_STATE_CONFLICT` 재활성화 검토 | 중복 실행 차단 |
| Volumes 조회 시 `segment` 누락 | `400` | `REQ_INVALID_PARAMETER` | Volumes는 segment 필수 |
| Volumes 조회 시 `segment=TRADE` 전달 | `400` | `REQ_INVALID_PARAMETER` | Trade Flows는 `/volumes/trade` 엔드포인트를 사용 |
| Volumes 조회 시 `types`/`regions`/`segment`가 허용값 외 문자열 | `400` | `REQ_INVALID_PARAMETER` | enum 변환 실패 |
| Volumes/Price 조회 시 case가 `PENDING` 또는 `RUNNING` 상태 | `409` 권장 | `BIZ_RULE_VIOLATION` | 결과 데이터가 아직 준비되지 않은 상태 |
| Volumes/Price 조회 시 case가 `FAILED` 상태 | `404` | `RSC_NOT_FOUND` | 실행 실패로 결과 없음 |
| Trade Flows 조회 시 `selector` 또는 `year` 누락 | `400` | `REQ_INVALID_PARAMETER` | 두 파라미터 모두 필수 |
| Trade Flows 조회 시 `year`가 2023~2030 범위 밖 | `400` | `REQ_INVALID_PARAMETER` | 허용 연도 초과 |
| Trade Flows 조회 시 case가 `PENDING` 또는 `RUNNING` 상태 | `409` 권장 | `BIZ_RULE_VIOLATION` | Volumes와 동일 |
| Trade Flows 조회 시 case가 `FAILED` 상태 | `404` | `RSC_NOT_FOUND` | Volumes와 동일 |
| 인증 없음 | `401` | `AUTH_REQUIRED` 또는 `AUTH_TOKEN_INVALID` | 기존 security handler 따름 |
| 권한 없음 | `403` | `AUTH_INSUFFICIENT_PERMISSIONS` 또는 `AUTH_ACCESS_DENIED` | `@RequiredPermissionRules` 결과 따름 |
| mock store 내부 오류 | `500` | `SYS_INTERNAL_ERROR` | 예외 로그 포함 |
| callback 대상 `runId` 없음 | `404` | `RSC_NOT_FOUND` | 존재하지 않는 실행 |
| callback `status`가 `COMPLETED | FAILED` 외 값 | `400` | `REQ_INVALID_PARAMETER` | terminal 상태만 허용 |
| 이미 terminal 상태인데 동일 payload 재전송 | `200` | `SUC-000` | idempotent no-op |
| 이미 terminal 상태인데 상이 payload 재전송 | `409` 권장 | `BIZ_RULE_VIOLATION` 1차 후보 | 상태 overwrite 금지 |
| timeout으로 `FAILED` 처리된 뒤 늦은 `COMPLETED` callback | `200` 권장 | `SUC-000` | 상태는 바꾸지 않고 late callback 로그만 남김 |

`BaseResponse` error 예시:

```json
{
  "status": false,
  "code": "BIZ-001",
  "message": "The operation violates a business rule.(case is already RUNNING)",
  "data": null
}
```

## 6. Swagger Annotation Recommendation

Controller tag 권장:

`FundamentalModelCaseController`:

```text
@Tag(
  name = "6. MI Fundamental Model Case API",
  description = "MI Fundamental Model Scenario Case mock API입니다."
)
```

`FundamentalModelRunController`:

```text
@Tag(
  name = "6-1. MI Fundamental Model Run API",
  description = "MI Fundamental Model 실행 Callback API입니다."
)
```

태그 번호 판단 근거:

- SK Gas API Swagger 그룹(`/api/v1/mi/next-gen/**`) 내 NextGen 컨트롤러는 `1` ~ `5` 단독 숫자 체계를 사용한다. (`1` Overview, `2` Event, `2-1` Model Execution, `3` Scenario, `5` Design)
- MI Fundamental Model은 NextGen와 동급의 독립 메뉴 단위이므로 NextGen 번호 범위(`1`~`5`) 안에 넣지 않는다.
- `6.` 부터 새 번호 범위를 시작해 NextGen 블록과 명확히 분리한다.
- callback 전용 `FundamentalModelRunController`는 `6-1.`을 사용한다. 이후 Fundamental Model 하위 컨트롤러가 추가될 경우 `6-2.`, `6-3.` 형태로 확장한다.

Endpoint summary 권장:

| Controller | Endpoint | `@Operation.summary` |
|---|---|---|
| `FundamentalModelCaseController` | list | `MI Fundamental Model Scenario Case 목록 조회 API` |
| `FundamentalModelCaseController` | upload | `MI Fundamental Model Scenario Case 업로드 API` |
| `FundamentalModelCaseController` | detail | `MI Fundamental Model Scenario Case 상세 조회 API` |
| `FundamentalModelCaseController` | update | `MI Fundamental Model Scenario Case 수정 API` |
| `FundamentalModelCaseController` | delete | `MI Fundamental Model Scenario Case 삭제 API` |
| `FundamentalModelCaseController` | run | `MI Fundamental Model Scenario Case 실행 요청 API` |
| `FundamentalModelCaseController` | status | `MI Fundamental Model Scenario Case 상태 조회 API` |
| `FundamentalModelCaseController` | volumes-filter-options | `MI Fundamental Model Volumes 필터 옵션 조회 API` |
| `FundamentalModelCaseController` | volumes | `MI Fundamental Model Volumes 데이터 조회 API` |
| `FundamentalModelCaseController` | trade-flows | `MI Fundamental Model Trade Flows 데이터 조회 API` |
| `FundamentalModelCaseController` | price | `MI Fundamental Model Price 데이터 조회 API` |
| `FundamentalModelCaseController` | reset | `[mockapi 전용] MI Fundamental Model Scenario Case 데이터 초기화 API` |
| `FundamentalModelRunController` | callback | `MI Fundamental Model Scenario Case 실행 완료 Callback API` |

Swagger 문서에는 다음을 명시한다.

- 목록 조회 API의 canonical contract는 `non-blank keyword requires searchType`이다. `keyword`가 null 또는 blank이면 `searchType`이 있어도 검색 조건 없음으로 처리한다.
- 업로드 API는 `multipart/form-data` 전용이다.
- `files` part는 정확히 3개가 필수다.
- 상세 조회는 case metadata, uploaded file metadata, latestRun만 반환한다. full run history/result 조회/download는 포함하지 않는다.
- 수정 API는 `title`, `description`만 받으며 응답은 최신 `FundamentalModelCaseDetailRes`다.
- 삭제 API는 physical delete가 아니라 soft delete이며, 삭제된 case는 이후 active-resource API에서 `404 RSC_NOT_FOUND`로 취급한다.
- 실행 요청은 `RUNNING` 상태에서 중복 접수를 차단한다.
- 상태 조회는 case 기준 polling용 경량 projection이며 `PENDING` case는 `latestRun = null`이다. 상세 조회와 달리 file metadata를 반환하지 않는다.
- Volumes 조회는 `COMPLETED` case에만 가능하다. `segment`(`GLOBAL`|`LNG`) 필수, `TRADE` 전달 시 400. `types`/`regions` 미전달 시 전체 반환, `granularity` 기본값 `MONTHLY`.
- Trade Flows 조회는 `COMPLETED` case에만 가능하다. `selector`/`year` 필수, `unit` 기본값 `MTPA`. fromCountries/toCountries 헤더 배열과 sparse flows 배열로 응답한다. `toRegion=null`인 Total 행은 항상 포함된다.
- Price 조회는 `COMPLETED` case에만 가능하다. 쿼리 파라미터 없음. 차트 + Annual 테이블 + Monthly 테이블을 단일 응답으로 반환한다.
- reset API는 mock 설정 flag (`mi.fundamental-model.mock.fundamental-model-cases.api.reset`) 로 활성화한다. 운영 배포에서는 flag 를 `false` 로 두어 사용을 차단한다.
- callback API는 외부 모델 전용이며 FE가 직접 호출하지 않는다.

## 7. Data / Model Impact

### 7.1 1st Mock Implementation

1차 구현은 DB schema 변경 없이 `src/main/resources/mock/fundamental-model/fundamental-model-cases.json` seed 파일을 읽어 in-memory store로 동작한다. `Next Gen` mock API와 동일하게 기능/메뉴별 mock resource 폴더를 두되, `Fundamental Model`은 `fundamental-model` 폴더를 canonical 위치로 사용한다.

seed 파일은 다음 최상위 구조를 가진다.

```json
{
  "cases": [],
  "runs": []
}
```

- `cases`는 `FundamentalModelCaseMockRecord`에 대응하며 `files`를 inline 배열로 포함한다.
- `runs`는 `FundamentalModelRunMockRecord`에 대응한다.
- `case.latestRunId`가 non-null이면 동일한 `runId`를 가진 run이 `runs`에 있어야 한다.
- `case.delYn`은 phase-1 mock soft-delete 상태를 나타내며, seed에 없으면 `N`으로 해석한다.
- `reset` API는 store를 빈 상태로 만드는 것이 아니라 seed json을 다시 읽어 초기 mock 상태로 되돌린다.

권장 mock aggregate:

| Store 객체 | 역할 |
|---|---|
| `FundamentalModelCaseMockRecord` | `caseId`, title, description, status, `assigneeName`, `assigneeUserNo`, `files: List<FundamentalModelFileMockRecord>`, latestRunId, delYn, timestamps |
| `FundamentalModelRunMockRecord` | `runId`, `caseId`, status, requestedAt, startedAt, finishedAt, failureReason, errorMessage |
| `FundamentalModelFileMockRecord` | fileNo, fileIndex, originalFileName, contentType, size |

`FundamentalModelCaseMockRecord.assigneeName`은 case 업로드 시점의 사용자 이름을 표현한다. mock에서는 업로드 요청에 업로더 이름을 받지 않으므로 고정값(예: `"홍길동"`)으로 설정한다. 향후 DB 전환 시 `tmd_scncasem.usr_no = tmd_usrm.usr_no`로 JOIN하고, 작성자명 표시는 `tmd_usrm.usr_nm`을 사용한다.

`FundamentalModelFileMockRecord` 는 별도 store map 없이 `FundamentalModelCaseMockRecord.files` 에 `List` 로 직접 포함된다. case 조회 시 파일 목록을 항상 함께 반환하는 접근 패턴이 유일하고, file만 독립 조회하는 use case가 없기 때문이다. 향후 DB 전환 시에는 이 리스트가 `tcm_filel` 별도 테이블로 분리된다 (§7.2 참고). `FundamentalModelFileMockRecord` 를 `FundamentalModelCaseFileRes` (응답 DTO) 와 별개 타입으로 두는 이유는 mock 내부 표현이 presentation layer에 직접 결합되지 않도록 하기 위함이다.

동시성:

- `ConcurrentHashMap<Long, FundamentalModelCaseMockRecord>`와 `ConcurrentHashMap<String, FundamentalModelRunMockRecord>` 또는 synchronized service method를 사용한다.
- run 요청의 `RUNNING` 중복 차단은 check-and-set이 한 critical section 안에서 수행되어야 한다.

#### Volumes/Trade Flows/Price 결과 데이터 mock seed

Volumes/Trade Flows/Price 결과 데이터는 별도 json seed 파일로 관리한다. figma 화면 데이터를 기반으로 직접 작성한다.

**파일 경로:**

```
src/main/resources/mock/fundamental-model/fundamental-model-volumes-data.json
src/main/resources/mock/fundamental-model/fundamental-model-trade-flows-data.json
src/main/resources/mock/fundamental-model/fundamental-model-price-data.json
```

**`fundamental-model-volumes-data.json` 최상위 구조:**

```json
{
  "GLOBAL": {
    "columns": [
      { "key": "2026-01", "label": "26-01", "year": 2026, "month": 1 }
    ],
    "rows": [
      { "type": "DEMAND", "region": "ASIA", "country": null,    "rowKind": "REGION_SUBTOTAL", "values": { "2026-01": 0.3 } },
      { "type": "DEMAND", "region": "ASIA", "country": "China", "rowKind": "COUNTRY",         "values": { "2026-01": 0.3 } },
      { "type": "DEMAND", "region": null,   "country": null,    "rowKind": "TYPE_TOTAL",      "values": { "2026-01": 344.8 } }
    ]
  },
  "LNG": {
    "monthly": {
      "columns": [{ "key": "2026-01", "label": "26-01", "year": 2026, "month": 1 }],
      "rows": [
        { "type": "LNG_IMPORTS", "region": "ASIA", "country": null, "rowKind": "REGION_SUBTOTAL", "values": { "2026-01": 6.3 } }
      ]
    },
    "yearly": {
      "columns": [{ "key": "2026", "label": "2026", "year": 2026, "month": null }],
      "rows": [
        { "type": "LNG_IMPORTS", "region": "ASIA", "country": null, "rowKind": "REGION_SUBTOTAL", "values": { "2026": 77.1 } }
      ]
    }
  }
}
```

- segment(`GLOBAL`, `LNG`)를 최상위 key로 가진다. `TRADE` key는 포함하지 않는다.
- 기본 seed 형식은 `columns` + `rows`이며, 이는 Monthly 원천 데이터로 해석된다.
- 필요 시 segment별로 `monthly` / `yearly` 하위 객체를 함께 둘 수 있다. `yearly`가 존재하면 `granularity=YEARLY` 요청 시 mock 서비스가 direct yearly seed를 우선 사용한다.
- `yearly`가 없으면 `granularity=YEARLY` 요청 시 mock 서비스가 `monthly`의 `month`별 값을 연간 합산해 반환한다.
- `rows`의 `type`, `region`, `rowKind`는 §4.11 enum 값과 동일해야 한다.
- `REGION_SUBTOTAL`은 Region별 subtotal 원천값이다.
- `TYPE_TOTAL`은 전체 Region 선택 시 사용할 Type total 원천값이다. Region 필터가 일부만 선택되면 mock service가 선택된 Region의 `REGION_SUBTOTAL` 값을 합산해 재계산한다.
- 각 table(`columns` + `rows`)의 `values` object key 집합은 해당 table의 `columns[].key` 집합과 동일해야 한다.

**`fundamental-model-trade-flows-data.json` 최상위 구조:**

```json
{
  "year": 2026,
  "type": "LNG",
  "unit": "MTPA",
  "fromCountries": [
    { "region": "AFRICA",        "country": null      },
    { "region": "AFRICA",        "country": "Angola"  },
    { "region": "NORTH_AMERICA", "country": null      }
  ],
  "toCountries": [
    { "region": "AFRICA", "country": null         },
    { "region": "ASIA",   "country": "Bangladesh" },
    { "region": null,     "country": "Total"      }
  ],
  "flows": [
    { "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.2894 },
    { "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": null,   "toCountry": "Total",      "value": 0.2894 }
  ]
}
```

- `fromCountries`와 `toCountries`는 각각 컬럼 헤더와 행 헤더다. `region=null, country="Total"`은 toCountries 마지막에 위치한다.
- `country=null`은 해당 지역의 집계 행/열을 나타낸다.
- `flows`는 0이 아닌 값만 포함한다. `toRegion=null`인 flows는 Total 행에 대응한다.
- `fromCountries`와 `toCountries`는 base 버전/기준 정보에 따라 country 목록이 달라질 수 있다. region 고유값은 `FundamentalModelRegion` enum으로 고정된다.
- DB 전환 시 country 목록의 동적 변동 특성은 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)의 `tmd_scncaseh_trd_hdr_l` header row table 설계를 따른다.

**`fundamental-model-price-data.json` 최상위 구조:**

```json
{
  "chart": {
    "series": [
      {
        "seriesType": "BASE_CASE",
        "label": "Base case price",
        "points": [{ "year": 2023, "month": 1, "value": 8.51 }]
      },
      {
        "seriesType": "LAST_ADJUSTED",
        "label": "Last adjusted price",
        "points": [{ "year": 2023, "month": 1, "value": 9.02 }]
      }
    ]
  },
  "annualTable": {
    "columns": [{ "year": 2023, "month": null, "periodLabel": null }],
    "rows": [
      { "label": "Oak Area Spot", "values": [75.71] }
    ]
  },
  "monthlyTable": {
    "columns": [{ "year": 2023, "month": 1, "periodLabel": "2026-1" }],
    "rows": [
      { "label": "Oak Area Spot", "values": [8.51] }
    ]
  }
}
```

- Price seed는 segment 구분 없이 단일 객체다.
- `seriesType`은 `BASE_CASE` 또는 `LAST_ADJUSTED` 고정이다.
- `annualTable.columns`의 `month`는 `null`, `periodLabel`은 `null`이다.
- mock 서비스는 이 파일을 classpath resource로 읽어 `FundamentalModelPriceRes`로 변환해 반환한다.

**mock 서비스 로딩 규칙:**

- 세 seed 파일은 모두 애플리케이션 시작 시(`@PostConstruct`) 로딩한다.
- `fundamental-model-volumes-data.json`이 없으면 Volumes API는 빈 응답(`columns: [], rows: []`)을 반환한다.
- `fundamental-model-trade-flows-data.json`이 없으면 Trade Flows API는 빈 응답(`fromCountries: [], toCountries: [], flows: []`)을 반환한다.
- `fundamental-model-price-data.json`이 없으면 Price API는 빈 응답(`chart.series: [], annualTable.rows: [], monthlyTable.rows: []`)을 반환한다.
- reset API는 Volumes/Trade Flows/Price seed를 재로딩하지 않는다. case/run seed(`fundamental-model-cases.json`)만 재로딩한다.

### 7.2 Future DB Mapping Notes

| API 필드 | 향후 DB 후보 | 메모 |
|---|---|---|
| `caseId` | `tmd_scncasem.case_id` | `INT UNSIGNED` 초안. Java DTO는 `Long` 사용 가능하나 mapper 변환 주의 |
| `scenarioId` | superseded | MI Fundamental Model 1차 mock API 계약에서는 제거한다. workflow/ERD draft 기준으로 `scenario_id` 전제는 제거 완료되었고 superseded 이력으로만 유지한다 |
| `title` | `tmd_scncasem.case_titl_nm` | 최대 200자 |
| `description` | `tmd_scncasem.case_expn` | nullable |
| `delYn` | `tmd_scncasem.del_yn` | soft delete flag. active resource 조회 조건은 `del_yn = 'N'` |
| `status` | `tmd_scncasem.case_stat_cd` | 최신 상태 cache 역할 |
| `assigneeUserNo` | `tmd_scncasem.usr_no` | 상세 응답에서 업로더 사용자 번호로 반환 |
| `assigneeName` | `tmd_usrm.usr_nm` | `tmd_scncasem.usr_no = tmd_usrm.usr_no` JOIN 표시값 |
| `latestRunId` | `tmd_scncasem.last_run_id` | `VARCHAR(20) NULL`. `tmd_scncaseh.run_id` 에 대한 application-validated 논리 참조이며 물리 FK 없음. `case_stat_cd` 와 invariant 동기화 |
| `runId` | `tmd_scncaseh.run_id` | `VARCHAR(20)` + unique. backend-generated `yyyymmdd_hhmmss_XXXX` |
| run status | `tmd_scncaseh.exec_stat_cd` | execution 관점 상태 |
| `latestRun.startedAt` | `tmd_scncaseh.exec_bgn_dtm` | FE 소요시간 계산 시작 시각 |
| `latestRun.finishedAt` | `tmd_scncaseh.exec_end_dtm` | FE 소요시간 계산 종료 시각. `RUNNING`이면 null |
| `failureReason` | `tmd_scncaseh.fail_rsn_cd` | `TIMEOUT`, `EXTERNAL_ERROR`, `UNKNOWN` |
| `errorMessage` | `tmd_scncaseh.err_msg_cntt` | 최대 길이 후속 확정 |
| upload files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNCASE'`, `file_rel_obj_dstg_no = case_id` |
| output files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNCASEH'`, `file_rel_obj_dstg_no = run_seq` |

`SCNCASE`/`SCNCASEH` 공통코드는 등록 완료된 것으로 implementation input을 둔다. `tcm_filel.file_rel_obj_dstg_no`와 `tmd_scncasem.case_id`/`tmd_scncaseh.run_seq`의 타입 호환성은 `erd-change-review`에서 재확인한다.

Trade Flows의 fromCountries/toCountries country 목록은 base 버전/기준 정보에 따라 변동될 수 있다. region 고유값은 고정이지만 region 내 country 목록이 달라지는 구조이므로, DB-backed 전환 시 header metadata는 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)의 `tmd_scncaseh_trd_hdr_l` row table에 저장한다.

결과 payload/pointer 컬럼은 phase 1 scope에서 제외하기로 결정되었다 (workflow draft Closed Decisions 참고). callback 처리 안에서 결과 데이터를 저장하지 않으며, 결과 조회/download 가 별도 spec으로 확정되면 그 시점에 컬럼 또는 별도 테이블을 재검토한다.

## 8. Recommended Data Access Approach

1차 mock:

- DB 접근 없음.
- `src/main/resources/mock/fundamental-model/fundamental-model-cases.json` 를 classpath resource로 읽어 Service 내부 in-memory mock store를 초기화한다.
- 업로드/실행/callback/update/delete로 발생하는 변경은 런타임 메모리 store에만 반영한다. json 파일 자체를 API 호출 중 수정하지 않는다.
- 기존 Next Gen mock service/delegator 구조를 참고하되, 기존 `ScenarioModelExecutionMockService` 와 store 를 공유하지 않는다.
- mock 활성화 방식은 신규 [`FundamentalModelMockProperties`](src/main/java/com/sk/domain/mi/config/FundamentalModelMockProperties.java) 의 per-API boolean flag 와 `@Primary` Delegator 패턴을 따른다. `NextGenMockProperties` 와 별개 클래스이며 prefix 는 `mi.fundamental-model.mock` 이다. `@Profile("mock")` 같은 전체 프로필 분기는 사용하지 않는다.
- 업로드/상세/수정/삭제/실행/상태/callback API 의 flag 는 `FundamentalModelCaseServiceDelegator` 내부에서 확인한다 (`mi.fundamental-model.mock.fundamental-model-cases.api.*`). Delegator 가 각 호출마다 real 또는 mock 구현체를 선택한다. 1차에서는 real 구현이 없으므로 flag 기본값은 모두 `true` 로 두되, real 구현이 추가되는 시점부터 개별 flag 를 `false` 로 전환해 점진적으로 real 구현으로 옮길 수 있다.
- `reset` flag 는 Delegator 가 아닌 **controller** 에서 직접 확인한다. 기존 `NextGen` 의 Delegator 패턴과 의도적으로 분리한다. flag 가 `false` 이면 controller 가 `404 Not Found` 를 반환하고 mock service 는 호출되지 않는다.

DB 전환 시 권장:

- command성 upload/run/status latest 조회는 MyBatis 또는 JPA 모두 가능하다.
- 파일 테이블 `tcm_filel` 및 기존 공통 파일 저장 흐름과 조인/연계가 필요하면 MyBatis가 운영 SQL 확인과 DBA 리뷰에 유리하다.
- 단순 aggregate 저장을 JPA로 시작할 경우에도 `tcm_filel` 연계와 transaction boundary는 명확히 분리해야 한다.

이 spec의 1차 구현 handoff는 mock store 기준이므로 data access 구현은 후속 DB 전환 시 다시 결정한다.

## 9. Implementation Touch Points

권장 신규 package/file 후보:

| 구분 | 후보 |
|---|---|
| Case Controller | `FundamentalModelCaseController` — `@RequestMapping("/api/v1/mi/fundamental-model/cases")`. list / upload / detail / update / delete / run / status / reset 을 담당한다. |
| Run Controller | `FundamentalModelRunController` — `@RequestMapping("/api/v1/mi/fundamental-model/runs")`. callback endpoint 전용. `FundamentalModelCaseService`(Delegator) 를 주입받아 `handleCallback` 을 호출한다. |
| Service interface | `FundamentalModelCaseService` |
| Real service (placeholder) | `FundamentalModelCaseServiceImpl` (1차에는 미구현, Delegator 가 mock 으로 라우팅) |
| Mock service | `FundamentalModelCaseMockService` |
| Delegator | `FundamentalModelCaseServiceDelegator` (`@Service @Primary`, real/mock 구현체 주입 + `FundamentalModelMockProperties` 로 메서드별 분기) |
| Mock 설정 properties | 신규 `com.sk.domain.mi.config.FundamentalModelMockProperties` 생성 (`mi.fundamental-model.mock` prefix, `NextGenMockProperties` 와 별개) |
| Request DTO | `FundamentalModelCaseListReq`, `FundamentalModelCaseUpdateReq`, `FundamentalModelVolumesReq`, `FundamentalModelTradeFlowsReq` |
| Callback DTO | `FundamentalModelRunCallbackReq` |
| Response DTO | `FundamentalModelCaseListRes`, `FundamentalModelCaseListItemRes`, `FundamentalModelCaseDetailRes`, `FundamentalModelCaseCreateRes`, `FundamentalModelCaseFileRes`, `FundamentalModelRunAcceptRes`, `FundamentalModelCaseStatusRes`, `FundamentalModelRunStatusRes`, `FundamentalModelRunCallbackRes`, `FundamentalModelVolumesRes`, `FundamentalModelTradeFlowsRes`, `FundamentalModelPriceRes` |
| Enum | `FundamentalModelCaseStatus`, `FundamentalModelRunStatus`, `FundamentalModelCaseSearchType`, `FundamentalModelSegment`, `FundamentalModelDataType`, `FundamentalModelRegion`, `FundamentalModelGranularity`, `VolumesRowKind`, `PriceSeriesType`, `TradeFlowSelector`, `TradeFlowUnit`, `TradeFlowDirection`, `TradeCountryHeaderKind` |

package는 이번 feature spec 수정에서 확정하지 않는다. MI Fundamental Model이 Next Gen와 동급 메뉴로 정리되었으므로 implementation follow-up에서 기존 `nextgen` package 후보를 `fundamentalmodel` package로 옮길지 검토해야 한다.

service method signature는 canonical contract에 맞게 `scenarioId`를 받지 않는다. 예: `listCases(searchType, keyword)`, `createCase(title, description, files)`, `getCaseDetail(caseId)`, `updateCase(caseId, request)`, `deleteCase(caseId)`, `runCase(caseId)`, `getStatus(caseId)`, `getVolumes(caseId, request)`, `getPrice(caseId)`, `reset()`, `handleCallback(runId, request)` 형태를 기준으로 구현한다.

`application.yml` 설정 예시 (1차 mock 기준, 모든 API mock 활성화):

```yaml
mi:
  fundamental-model:
    mock:
      fundamental-model-cases:
        api:
          list-cases: true
          create-case: true
          get-detail: true
          update: true
          delete: true
          run: true
          get-status: true
          get-volumes: true
          get-trade-flows: true
          get-price: true
          reset: true
      fundamental-model-runs:
        api:
          callback: true
```

운영 배포에서는 real 구현이 준비된 API 부터 해당 flag 를 `false` 로 전환한다. `reset` flag 가 `false` 이면 controller 는 `404 Not Found` 로 차단한다.

권장 permission:

```text
@RequiredPermissionRules(rules = {
  @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
  @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
})
```

Reset API도 1차 개발/시연 편의상 같은 권한을 적용하되, mock 설정 flag 로만 활성화한다 (§3.6 참고). 외부 모델 callback API는 1차 mock 구현 범위에 포함한다. 다만 운영 수준 callback 인증/서명/재시도 정책은 실제 Kubernetes Job 연동 전에 별도 보안 설계에서 확정한다.

callback API 권한 권장:

```text
@RequiredPermissionRules(rules = {
  @PermissionRule(role = Role.SYSTEM),
  @PermissionRule(role = Role.API_SUB_ADMIN),
  @PermissionRule(role = Role.API_PLUS_OPERATOR)
})
```

사용자 화면에서 호출하는 API가 아니므로 `Role.USER` 와 일반 `Role.ADMIN` user-scope 조건은 넣지 않는 쪽을 권장한다. 실제 model pod가 JWT 기반으로 호출하지 못한다면 별도 callback 인증 filter, HMAC signature, mTLS, private network allowlist 중 하나를 보안 설계에서 확정한다.

phase 1 mock 시연 동선:

- callback API의 권한 annotation 은 mock/운영 환경에 관계없이 동일하게 유지한다. 본 spec 의 mocking 방식은 controller/권한 swap 이 아니라 service 구현체 swap (Delegator 패턴, §8/§9 참고) 이므로 권한을 환경별로 다르게 적용하지 않는다.
- 시연 시에는 SYSTEM-scope 권한을 가진 내부 계정 (예: `API_SUB_ADMIN`, `API_PLUS_OPERATOR`) 으로 Swagger 또는 별도 도구에서 callback endpoint 를 호출해 terminal 상태로 전이한다.
- 일반 USER 가 직접 완료 전이를 트리거해야 하는 시연 요구가 있으면, callback path 와 권한을 그대로 두고 시연 전용 trigger endpoint 를 별도 path/권한으로 새로 정의한다 (후속 feature spec 사항).

## 10. Mock Behavior Rules

### List Cases

1. `delYn != Y` 인 active mock case를 대상으로 `createdAt` 내림차순 정렬한다.
2. `keyword`가 null 또는 blank(trim 후 빈 문자열)이면 검색 조건 없음으로 처리한다. 이 경우 `searchType`이 함께 전달되어도 필터를 적용하지 않는다.
3. `keyword`가 non-blank인 경우에만 `searchType`을 필수로 요구하고, 둘 다 유효하면 필터를 적용한다.
   - `TITLE`: case 제목에서 대소문자 구분 없이 부분 일치 필터
   - `ASSIGNEE_NAME`: `assigneeName`에서 대소문자 구분 없이 부분 일치 필터
4. `keyword`가 non-blank인데 `searchType`이 없으면 `RSC_NOT_FOUND` 대신 `REQ_INVALID_PARAMETER`로 거절한다.
5. 결과 목록과 `totalCount`를 `FundamentalModelCaseListRes`로 반환한다.
6. 조건에 맞는 case가 없어도 빈 리스트(`totalCount=0`, `cases=[]`)로 정상 응답한다.

### Upload

1. `title`, `description`, `files`를 받는다.
2. `files`가 정확히 3개인지 검증한다.
3. 각 파일이 비어 있지 않은지 검증한다.
4. mock `caseId`를 증가값으로 생성한다.
5. 파일 binary는 저장하지 않고 metadata만 mock store에 보관한다.
6. case 상태는 `PENDING`으로 저장한다.
7. `FundamentalModelCaseCreateRes`를 반환한다.

### Detail Query

1. `caseId`로 active case를 찾는다.
2. case가 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
3. case metadata와 uploaded file metadata를 반환한다.
4. `latestRunId`가 null이면 `latestRun = null`로 반환한다.
5. `latestRunId`가 있으면 run store에서 최신 run 1건만 찾아 `latestRun`으로 반환한다.
6. full run history, result payload, result file/download metadata는 반환하지 않는다.

### Update

1. `caseId`로 active case를 찾는다.
2. case가 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
3. `title`을 trim 후 1-200자 필수로 검증한다.
4. `description`은 nullable/optional로 받아 record에 반영한다.
5. `status`, `files`, `latestRunId`, run store는 변경하지 않는다.
6. `updatedAt`을 갱신한다.
7. 최신 `FundamentalModelCaseDetailRes`를 반환한다.

### Delete

1. `caseId`로 active case를 찾는다.
2. case가 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
3. record의 `delYn`을 `Y`로 바꾸고 `updatedAt`을 갱신한다.
4. upload files, run history, latestRun/result payload는 물리 삭제하지 않는다.
5. 이후 list/detail/status/run/update/delete에서 해당 case는 active resource가 아니므로 `RSC_NOT_FOUND`로 취급한다.
6. 응답은 `BaseResponse<Void>` 200 OK다.

### Run Request

1. `caseId`로 active case를 찾는다.
2. case가 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
3. case status가 `RUNNING`이면 `409 + BIZ_RULE_VIOLATION` 후보로 거절한다.
4. 실행 가능 상태는 `PENDING`, `COMPLETED`, `FAILED`다.
5. `runId`는 `yyyymmdd_hhmmss_XXXX` 포맷으로 생성한다. `yyyymmdd_hhmmss`는 실행 요청 시각 기준이고, `XXXX`는 같은 초 안의 충돌을 방지하는 4자리 suffix다.
6. run record를 `RUNNING`으로 생성한다.
7. case 최신 상태를 `RUNNING`으로 변경하고 `latestRunId`를 갱신한다.
8. `FundamentalModelRunAcceptRes`를 `202 Accepted`로 반환한다.

### Status Query

1. `caseId`로 active case를 찾는다.
2. case status와 latest run 정보를 반환한다.
3. `PENDING` case는 아직 실행 이력이 없으므로 `latestRun = null`이다.
4. status query는 polling용 경량 projection이므로 `description`, `assigneeName`, `files`를 반환하지 않는다.
5. 1차 기본 mock은 자동으로 `COMPLETED` 또는 `FAILED`로 전이하지 않는다.

### Reset

1. 현재 메모리 store의 cases와 runs를 삭제한다.
2. `src/main/resources/mock/fundamental-model/fundamental-model-cases.json` seed 파일을 다시 읽어 초기 cases/runs를 복원한다.
3. mock store의 case ID sequence는 seed case의 최대 `caseId` 이상으로 재설정한다. reset 이후 신규 업로드는 seed case와 충돌하지 않는 다음 ID를 사용한다.
4. 응답은 `BaseResponse<Void>` 또는 `BaseResponse<ResetRes>` 중 하나를 선택한다. 기존 reset API와 맞추려면 `BaseResponse<Void>`를 권장한다.

### External Callback Handling

1. path `runId`로 run record를 찾는다.
2. run이 없으면 `RSC_NOT_FOUND`.
3. run이 `RUNNING`이면 terminal 상태로 전이한다.
4. `status = COMPLETED`이면 run 상태와 case 최신 상태를 `COMPLETED`로 변경하고 `finishedAt`을 반영한다.
5. `status = FAILED`이면 run 상태와 case 최신 상태를 `FAILED`로 변경하고 `failureReason`, `errorMessage`, `finishedAt`을 반영한다.
6. 이미 terminal 상태인 run에 동일 terminal payload가 재전송되면 상태를 바꾸지 않고 `200 OK` no-op으로 응답한다. 이때 `FundamentalModelRunCallbackRes.idempotentReplay = true` 로 설정한다.
7. 이미 terminal 상태인 run에 상이 payload가 들어오면 상태를 바꾸지 않는다. 동일 retry로 볼 수 없으면 `409 + BIZ_RULE_VIOLATION` 후보로 처리한다.
   - 비교 키는 `status`, `failureReason`, `errorMessage` 세 필드만 사용한다. 세 필드가 모두 같으면 "동일 payload"로 간주한다.
   - `finishedAt` 은 외부 모델 retry마다 다르게 들어올 수 있으므로 비교 대상에서 제외한다.
   - `runId` 는 path 식별자이므로 payload 비교 키에 포함하지 않는다.
8. timeout 등으로 이미 `FAILED` 처리된 run에 늦은 `COMPLETED` callback이 오면 상태를 되돌리지 않는다. 외부 모델 재시도 폭주를 막기 위해 `200 OK` no-op과 late callback 로그를 권장한다.
9. callback 처리 안에서는 결과 payload materialization이나 파일 다운로드 처리를 수행하지 않는다.

### Volumes Data Query

1. `caseId`로 active case를 찾는다. 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
2. case status가 `COMPLETED`가 아니면 상태에 따라 아래와 같이 처리한다.
   - `PENDING` 또는 `RUNNING`: `409 BIZ_RULE_VIOLATION`.
   - `FAILED`: `404 RSC_NOT_FOUND`.
3. `segment` 파라미터가 없으면 `400 REQ_INVALID_PARAMETER`.
4. `types`가 미전달이면 segment별 허용 `FundamentalModelDataType` 목록을 필터로 사용한다.
   - `GLOBAL`: `DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`
   - `LNG`: `LNG_IMPORTS`, `LNG_EXPORTS`
   - `TRADE`: 이 API에서 허용하지 않는다. Trade Flows API를 사용한다.
5. `types`에 segment에서 허용하지 않는 값이 포함되면 `400 REQ_INVALID_PARAMETER`로 응답한다.
6. `regions`가 미전달이면 전체 `FundamentalModelRegion` 목록을 필터로 사용한다.
7. `granularity`가 미전달이면 `MONTHLY`를 기본값으로 사용한다.
8. `granularity=MONTHLY`이면 `columns`의 `label`은 `"26-01"` 형식이고, `year`/`month`는 각각 4자리 연도와 1~12 정수이다.
9. `granularity=YEARLY`이면 `columns`의 `label`은 연도 문자열이고, `year`는 4자리 연도, `month`는 `null`이다.
10. 응답의 `unit`은 phase 1에서 `MTPA` 고정이다. 후속 unit 변환 기능은 별도 request contract로 정의한다.
11. `rows`는 필터를 적용한 뒤 FE가 응답 순서 그대로 렌더링할 수 있게 정렬한다. 선택된 `types` 순서대로 Type section을 만들고, 각 Type section 안에서는 선택된 `regions` 순서대로 `REGION_SUBTOTAL` 행을 먼저 반환한 뒤 해당 Region의 `COUNTRY` 행들을 반환한다. 각 Type section의 마지막 행은 `TYPE_TOTAL`이다.
12. `typeLabel`은 `type` enum label을 반환한다. `rowLabel`은 `REGION_SUBTOTAL`이면 region enum label, `COUNTRY`이면 country, `TYPE_TOTAL`이면 `Total`을 반환한다.
13. `TYPE_TOTAL`은 선택된 Region들의 `REGION_SUBTOTAL` 값을 합산해 반환한다. 예: `regions=ASIA,EUROPE`이면 해당 Type의 Asia subtotal + Europe subtotal이 Type total이다.
14. 응답의 `appliedFilters`에는 실제 적용된 `types`와 `regions` 목록을 반환한다.
15. 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환하며, 필터는 해당 데이터 범위 내에서 적용한다. seed의 표시 label은 서버가 응답 조립 시 파생한다.

### Trade Flows Data Query

1. `caseId`로 active case를 찾는다. 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
2. case status 조건은 §Volumes Data Query와 동일하게 처리한다.
3. `selector`, `year` 파라미터가 없으면 `400 REQ_INVALID_PARAMETER`.
4. `year`가 2023~2030 범위 밖이면 `400 REQ_INVALID_PARAMETER`.
5. `unit` 미전달 시 `MTPA`를 기본값으로 사용한다.
6. `types` 미전달 시 전체 `TradeFlowDirection` 목록(`TO_COUNTRY`, `FROM_COUNTRY`)을 필터로 사용한다.
7. `regions` 미전달 시 전체 `FundamentalModelRegion` 목록을 필터로 사용한다.
8. `types`와 `regions`가 함께 전달되면 direction에 따라 헤더를 필터링한다.
   - `FROM_COUNTRY`가 types에 포함되면 fromCountries를 선택된 regions로 축소한다. `TO_COUNTRY`만이면 fromCountries는 전체 유지.
   - `TO_COUNTRY`가 types에 포함되면 toCountries를 선택된 regions로 축소한다. `FROM_COUNTRY`만이면 toCountries는 전체 유지.
   - `region=null`(Total)인 toCountries 항목은 필터와 무관하게 항상 포함된다.
9. flows는 fromCountries/toCountries 범위 내의 non-zero 값만 포함한다. `toRegion=null`인 Total flows는 항상 포함된다.
10. 응답의 `appliedFilters`에는 실제 적용된 `types`와 `regions` 목록을 반환한다.
11. 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환한다. `selector`/`year` 값은 응답에 echo하며 필터는 seed 데이터 범위 내에서 적용한다.

### Price Data Query

1. `caseId`로 active case를 찾는다. 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
2. case status 조건은 §Volumes Data Query와 동일하게 처리한다.
3. 쿼리 파라미터 없음. 요청 즉시 차트 + Annual 테이블 + Monthly 테이블을 구성해 단일 응답으로 반환한다.
4. `chart.series`는 항상 `BASE_CASE`, `LAST_ADJUSTED` 두 시리즈를 포함한다. 데이터가 없으면 `points: []`로 반환한다.
5. `annualTable`과 `monthlyTable`의 행 `label`은 데이터 기반 동적 값으로, 서버가 고정 enum으로 강제하지 않는다.
6. `annualTable.columns`의 `month`는 `null`이다.
7. `monthlyTable.columns`의 `month`는 1~12 정수다.
8. 1차 mock에서는 json seed 파일 기반 고정 데이터를 반환한다.

## 11. Risks / Open Questions

- 외부 모델 완료 동기화는 callback 방식으로 확정한다. phase 1 mock은 callback endpoint를 구현한다. 남은 항목은 실제 Kubernetes Job 연동용 인증 방식, header/signature 규격, retry 정책이다.
- `run_id` 생성 주체는 backend로 확정한다. 생성 포맷은 `yyyymmdd_hhmmss_XXXX` 이다. Kubernetes Job 연동 계약에서는 backend가 Job/pod에 전달한 `run_id` 를 완료/실패 callback path에 그대로 사용하는지만 확인한다.
- 결과 payload와 결과 파일은 model pod가 S3/object storage에 저장/관리하며 backend 소관이 아니다. 1차 범위에서는 backend가 result pointer/reference도 저장하지 않는다.
- Volumes/Trade Flows/Price 결과 데이터는 1차 mock에서 json seed 기반 고정 응답으로 처리한다. DB-backed 전환 시 parsed result table, key, partition/index, mock-to-real 경계는 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)를 따른다.
- 결과 파일 다운로드는 이번 1차 scope에서 제외한다. 단, status response의 `latestRun.runId`는 후속 결과 조회의 연결 키가 될 수 있다.
- Volumes 필터의 Save 토글(사용자별 필터 저장)은 1차 scope에서 제외한다. 후속 구현 시 user preference 테이블 설계가 필요하다.
- 삭제는 phase-1에서 case active-resource 제외를 위한 soft delete만 구현한다. 보존 기간, audit 조회, 복구 정책은 후속 정책 범위다.
- timeout은 설계 문서에서 5분 가정이 있으나 1차 mock에서는 scheduler를 구현하지 않는다.
- 운영 입력 파일명에는 `DEMAND`/`SUPPLY`/`TRADE` 키워드가 포함되는 것으로 확정한다. DB-backed run dispatch 전환 시 backend는 `SCNCASE + caseId` 파일 목록에서 파일명 키워드로 입력 파일 역할을 매핑한다.
- mock store의 런타임 변경분은 서버 재시작 또는 reset 시 유실되고 seed json 기준으로 복원된다. 개발/시연 전용으로만 사용한다.
- `caseId`를 path에 노출하므로 후속 DB 전환 시 case ownership과 권한 검증이 필수다.
- DB-backed update/delete 전환 시 predicate는 `case_id + usr_no ownership + del_yn = 'N'`을 포함해야 한다.
- workflow/ERD draft/ERD draft의 `scenario_id` 상태 해석은 §7.2 Future DB Mapping Notes의 superseded note를 따른다.

## 12. Exit Criteria / Handoff

### Contract Stability

이 계약은 1차 mock 구현에는 충분히 안정적이다.

구현 가능 항목:

- 목록 조회 controller (`non-blank keyword requires searchType` 검증, `createdAt` 내림차순 정렬)
- multipart 업로드 controller
- case 상세 조회 controller
- case metadata 수정 controller
- case soft delete controller
- 파일 3개 필수 검증
- in-memory case/run/file metadata store
- `PENDING -> RUNNING` 상태 전이
- `RUNNING` 중복 실행 차단
- case 기준 status polling
- callback endpoint를 통한 `RUNNING -> COMPLETED | FAILED` terminal 전이
- 삭제된 case의 list 제외 및 detail/status/run/update/delete `404 RSC_NOT_FOUND` 처리
- mock reset
- `BaseResponse<T>` 래핑
- Volumes 데이터 조회 (`COMPLETED` case 한정, segment `GLOBAL`|`LNG` 필수, `TRADE` 전달 시 400, types/regions 다중 필터, granularity MONTHLY/YEARLY)
- Trade Flows 데이터 조회 (`COMPLETED` case 한정, selector/year 필수, unit/types/regions 선택 필터, fromCountries/toCountries 헤더 + sparse flows 응답, Total 행 항상 포함)
- Price 데이터 조회 (`COMPLETED` case 한정, 차트 + Annual 테이블 + Monthly 테이블 단일 응답)

아직 안정적이지 않은 항목:

- 실제 Kubernetes Job 연동 (run/callback DB 상태 전이 slice에서는 제외)
- callback 운영 인증/서명/재시도 세부 정책
- 결과 파일 다운로드
- Volumes/Trade Flows/Price DB persistence implementation (canonical table/key/partition/index design은 `mi-fundamental-model-output-result-persistence-design.md`에 확정됨)
- timeout scheduler
- Volumes 필터 Save 기능 (user preference 저장)

### Input for `erd-change-review`

- `tmd_scncasem`, `tmd_scncaseh`, `tcm_filel` 매핑 초안의 타입/인덱스/제약 검증
- `tcm_filel.file_rel_obj_dstg_no`와 `case_id` 타입 호환성 검증
- `SCNCASE`/`SCNCASEH` 공통코드는 등록 완료된 것으로 implementation input을 둔다. DDL review에서는 환경별 코드 반영 여부와 `tcm_filel.file_rel_obj_dstg_no` 타입 호환성만 확인한다
- `case_stat_cd`와 `exec_stat_cd` 이중 상태 저장 정합성 검증
- `RUNNING` 중복 실행 방지를 위한 transaction/lock/index 전략 검토 (workflow draft §5 conditional master update 기준)
- `run_id` unique 제약과 조회 인덱스 검토
- backend-generated `yyyymmdd_hhmmss_XXXX` 저장 길이(`VARCHAR(20)`)와 외부 모델 왕복 payload의 `run_id` 보존 규칙 검토
- 목록 조회 query shape는 1차 mock canonical contract 기준으로 scenario scope를 전제하지 않는다. `scenario_id` 상태는 §7.2 Future DB Mapping Notes의 superseded note를 기준으로 해석한다.
- `ASSIGNEE_NAME` 검색 JOIN 경계는 `tmd_scncasem.usr_no = tmd_usrm.usr_no`, 표시 컬럼은 `tmd_usrm.usr_nm`으로 확정한다. DDL review에서는 JOIN 양측 인덱스 커버리지를 확인한다
- callback compare-and-set update 조건과 terminal overwrite 방지 전략 검토
- `tmd_scncasem.last_run_id` 컬럼의 invariant 와 application-validated 운용 방식 검토. case 최신 상태 동기화 시 `last_run_id = :run_id` 조건이 race condition 과 late callback 에 대해 충분한지 확인
- `req_param_json`, `rslt_ref_json` 제거는 workflow draft Closed Decisions 에서 확정되었다. 결과 조회/download spec 이 별도로 확정되면 그 시점의 컬럼/테이블 도입을 별도 review 로 다룬다.
- DB-backed update predicate는 `case_id + usr_no ownership + del_yn = 'N'` 조건으로 active 소유 case만 갱신하고, `case_titl_nm`, `case_expn`, `mod_*` 컬럼만 변경한다.
- DB-backed delete predicate는 `case_id + usr_no ownership + del_yn = 'N'` 조건으로 active 소유 case만 soft delete하고, `del_yn = 'Y'`, `mod_*` 컬럼만 변경한다.

### Input for `design-consistency-review`

- 이 feature spec과 `scenario-case-execution-workflow-design-draft.md`의 용어/path/model 정합성 확인. 특히 `scenario_id` 상태 진술이 §7.2 Future DB Mapping Notes와 일치하는지 확인
- Notion 공유 문서가 canonical 계약처럼 읽히지 않는지 확인
- 기존 `ScenarioModelExecution` API와 naming/path/scope가 혼동되지 않는지 확인. MI Fundamental Model은 Next Gen 하위 API가 아니다
- 결과 조회/download 제외 범위와 edit/delete phase-1 soft-delete 범위가 feature spec과 design doc에서 일관되는지 확인
- callback path가 Design callback의 canonical-id 중심 구조와 일관되는지 확인

### Input for `spring-api-implementation`

- Controller path family: `FundamentalModelCaseController` → `/api/v1/mi/fundamental-model/cases` (list / upload / detail / update / delete / run / status / volumes / volumes/trade / price / reset). `FundamentalModelRunController` → `/api/v1/mi/fundamental-model/runs` (callback 전용).
- List: `GET`, query params `searchType` (enum `TITLE`|`ASSIGNEE_NAME`) + `keyword` (`keyword` null/blank이면 전체 반환, non-blank이면 `searchType` 필수), 결과 `FundamentalModelCaseListRes`, `createdAt` 내림차순
- Upload: `POST`, multipart, `title`, `description`, `files` exactly 3
- Detail: `GET /{caseId}`, returns `FundamentalModelCaseDetailRes`
- Update: `PATCH /{caseId}`, request `FundamentalModelCaseUpdateReq(title, description)`, returns latest `FundamentalModelCaseDetailRes`
- Delete: `DELETE /{caseId}`, soft delete, returns `BaseResponse<Void>` 200 OK
- Run: `POST /{caseId}/runs`, returns `202 Accepted`
- Status: `GET /{caseId}/status`, case 기준 조회
- Volumes: `GET /{caseId}/volumes`, query params `segment`(`GLOBAL`|`LNG` 필수, `TRADE` 전달 시 400) + `types`(선택) + `regions`(선택) + `granularity`(선택, 기본 `MONTHLY`). `COMPLETED` case 한정. returns `FundamentalModelVolumesRes`
- Trade Flows: `GET /{caseId}/volumes/trade`, query params `selector`(필수) + `year`(필수, 2023~2030) + `unit`(선택, 기본 `MTPA`) + `types`(선택) + `regions`(선택). `COMPLETED` case 한정. returns `FundamentalModelTradeFlowsRes`
- Price: `GET /{caseId}/price`, 쿼리 파라미터 없음. `COMPLETED` case 한정. returns `FundamentalModelPriceRes` (차트 + Annual 테이블 + Monthly 테이블 동시 반환)
- Callback: `POST /api/v1/mi/fundamental-model/runs/{runId}/callback`, 외부 모델 전용
- Reset: `POST /reset`, `FundamentalModelMockProperties` 의 per-API flag (`mi.fundamental-model.mock.fundamental-model-cases.api.reset`) 로 활성화. controller 에서 직접 확인. `@Profile` 분기는 사용하지 않음
- Service signatures: `scenarioId` 없이 `caseId` 또는 `runId` 단독 식별자로 처리한다. `getVolumes(caseId, request)`, `getPrice(caseId)` 추가
- Mock 활성화: 신규 `FundamentalModelCaseServiceDelegator` (`@Service @Primary`) + `FundamentalModelMockProperties` 의 per-API flag (`get-volumes`, `get-trade-flows`, `get-price` flag 추가). 1차에는 모든 flag `true` 기본값, real 구현 추가 시 개별 flag `false` 로 전환. `reset` flag 는 Delegator 가 아닌 controller 에서 확인
- Active-resource rule: `delYn = Y` case는 list에서 제외하고 detail/status/run/update/delete/volumes/price에서 `RSC_NOT_FOUND` 처리
- Data state rule: volumes/price는 `COMPLETED` case만 허용. `PENDING`/`RUNNING`이면 `409 BIZ_RULE_VIOLATION`, `FAILED`이면 `404 RSC_NOT_FOUND`
- 신규 enum: `FundamentalModelSegment` (`GLOBAL`, `LNG`, `TRADE` — `TRADE`는 `/volumes`에서 불허), `FundamentalModelDataType` (`DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`), `FundamentalModelRegion` (`ASIA`, `EUROPE`, `NORTH_AMERICA`, `MIDDLE_EAST`, `AUSTRALIA`, `RUSSIA`, `AFRICA`, `SOUTH_AMERICA`), `FundamentalModelGranularity` (`MONTHLY`, `YEARLY`), `FundamentalModelVolumeUnit` (`MTPA`), `VolumesRowKind` (`COUNTRY`, `REGION_SUBTOTAL`, `TYPE_TOTAL`), `PriceSeriesType` (`BASE_CASE`, `LAST_ADJUSTED`), `TradeFlowSelector` (`LNG`, `PNG`), `TradeFlowUnit` (`MTPA`, `BCM`), `TradeFlowDirection` (`TO_COUNTRY`, `FROM_COUNTRY`), `TradeCountryHeaderKind` (`REGION_TOTAL`, `COUNTRY`, `GRAND_TOTAL`)
- Status enum: `FundamentalModelCaseStatus` = `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`. `FundamentalModelRunStatus` = `RUNNING`, `COMPLETED`, `FAILED` (분리 enum). `FundamentalModelCaseSearchType` = `TITLE`, `ASSIGNEE_NAME`
- Failure reason candidates: `TIMEOUT`, `EXTERNAL_ERROR`, `UNKNOWN`
- Base response: `BaseResponse<T>` via `BaseResponseFactory`
- Permissions: existing Next Gen `USER/SKGAS_COMPANY_USER_ONLY` and `ADMIN/THIRD_PARTY_ADMIN_ONLY` pattern
