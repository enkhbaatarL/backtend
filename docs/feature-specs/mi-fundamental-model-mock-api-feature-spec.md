# MI Fundamental Model Case API Feature Spec

## Document Control

- status: Approved
- canonical for: MI Fundamental Model Case real-DB endpoint, request/response DTO, validation, error/status mapping
- owner: BE
- source documents:
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
- depends on:
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `docs/conventions/openapi-documentation-conventions.md`
- supersedes:
  - 이 문서의 1차 mock runtime 구성·reset API·in-memory store 구현 handoff
- last synced: 2026-09-28
- open questions:
  - 현재 구현된 API와 Region/Country 필터 확장 계약에는 없음
  - callback 인증/서명, 필터 저장은 별도 후속 계약에서 확정

Notion `MI Fundamental Model 실행`(`350263ff004c8035b20ef02683447f4b`)과
`MI Fundamental Model`(`34c263ff004c81ad817bc0a5a1c2992f`)은 reference only다.

## Document Roles / Source of Truth

이 문서는 MI Fundamental Model Case API의 현재 real-DB endpoint, request/response DTO, validation,
error/status mapping의 canonical source다. 1차 mock 설명은 real-DB 전환 전 의사결정의 historical
reference일 뿐 현재 runtime 또는 구현 handoff가 아니다.

관련 문서 역할은 다음처럼 분리한다.

| 문서 | 역할 | canonical 범위 |
|---|---|---|
| 이 feature spec | API 구현 계약 | endpoint path, method, DTO field, response shape, validation, error/status mapping |
| [scenario-case-execution-workflow-design-draft.md](../design-docs/gas/scenario-case-execution-workflow-design-draft.md) | workflow/domain design | Scenario Case aggregate, state transition, callback/timeout semantics, future persistence rationale |
| [scenario-case-execution-erd-ddl-draft.md](../design-docs/gas/scenario-case-execution-erd-ddl-draft.md) | DBA rollout 계약 | physical table/column DDL, index, common-code registration, pre-check/apply/rollback/verify SQL (현재 template 상태) |
| [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md) | output result persistence 설계 | Volumes / Trade Flows / Price parsed result table, key, index, partition-ready strategy, mock→real 전환 경계 |
| `docs/work-notes/**` | 실행 이력 | changed files, verification, accepted decisions, superseded decisions, residual risks |
| Notion / Slack / Figma | external evidence/share artifact | repo 문서로 정규화되기 전까지 canonical contract가 아니다 |

Work note와 외부 문서가 이 feature spec 또는 canonical design draft와 충돌하면, 구현자는 feature spec/design draft를 우선하고 충돌 내용을 새 work note에 기록한다.

### Related Enhancement Contract

다음 MI Base 변경 계약은 [MI Fundamental Model MI Base 지정 Feature Spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 canonical source다. 본 문서의 기존 Scenario Case 목록 request/response/behavior 절은 해당 변경에 한해 superseded된다.

- 기존 목록 변경: `GET /api/v1/mi/fundamental-model/cases`가 current `miBase`와 current 제외 `cases`를 분리 반환
- 목록 DTO 변경: `FundamentalModelCaseListRes.miBase`, `FundamentalModelCaseListItemRes.execType` 추가. `totalCount`는 두 영역의 표시 건수 합계
- 상세/결과 read 변경: active `MI_BASE`를 상세·상태·Volumes filter-options·Volumes·Trade Flows·Price에서 조회할 수 있다. 기존 query parameter, response DTO, completed-case 검증, 빈 결과와 오류 계약은 이 문서를 유지하고 `MI_BASE` 허용 범위는 enhancement spec이 소유한다.
- MI Base mutation 정책: FE는 `execType=MI_BASE`에서 수정·삭제·재실행 action을 노출하거나 호출하지 않고, backend도 해당 mutation을 active `INDEPENDENT` 전용으로 유지한다. 별도 MI Base 지정 해제 API는 제공하지 않는다.
- MI Base 지정: 완료 독립실행 case를 현재 MI Base로 원자적으로 지정하고, 이전 current는 `INDEPENDENT`로 복귀시키며, 이후 시작되는 수급모델 input 생성에 적용하는 신규 idempotent API. 기존 MI 수급모델 실행과 같은 `USER/SKGAS_COMPANY_USER_ONLY`, `ADMIN/THIRD_PARTY_ADMIN_ONLY`를 사용하고 지정 대상의 소유자·회사 조건은 적용하지 않는다.
- input 내용 제공: 별도 [MI Fundamental Model Input 제공 Feature Spec](mi-fundamental-model-input-provision-feature-spec.md)에서 `caseId` 식별 경계와 미확정 전달 계약을 관리한다.

이 문서의 upload 및 status/result 세부 계약은 그대로 유지한다. detail/status/result의 `MI_BASE` 허용과 update/delete/run 차단 경계는 enhancement spec이 supersede한다.

## 1. Feature Goal

MI Fundamental Model Case API는 사용자가 Scenario Case와 input Excel 4개를 등록하고, 특정 case의
모델 실행을 비동기로 요청하며, 실행 상태와 real-DB에 적재된 Volumes/Trade Flows/Price 결과를
조회할 수 있게 한다. 모델 callback은 run/case 상태를 terminal 상태로 전이한다.

`Scenario Case`는 현업에서 사용하는 업무 용어이므로 유지한다. 다만 API hierarchy에서 MI Next Gen의 `Scenario` 하위 resource라는 의미는 아니다. MI Fundamental Model은 MI Next Gen와 동급 메뉴이며, 이 문서의 canonical API는 Next Gen path family를 사용하지 않는다.

이 문서는 현재 controller/service/DTO와 후속 API 변경의 계약 입력이다. Java 구현과 물리 schema/rollout
SQL은 각각 source code와 canonical DDL/design 문서가 소유한다.

### Current Scope

- Scenario Case 업로드
  - `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 입력 파일 4개
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
  - 실행 유형 `execType` (`INDEPENDENT | MI_BASE`)
  - input/output/intermediate/tradeOutput 다운로드 metadata
  - latestRun projection
- Scenario Case metadata 수정
  - `title`, `description`만 허용
  - title은 trim 후 1-200자 필수
  - 수정 응답은 최신 상세 응답 반환
- Scenario Case soft delete
  - active resource에서 제외
  - upload files, run history, latestRun/result payload는 물리 삭제하지 않음
- 외부 모델 callback
  - `runId` 기준 real-DB run/case terminal 상태 전이
  - callback DTO와 idempotency 규칙
- Volumes 데이터 조회 (Global / LNG)
  - `COMPLETED` case의 실행 결과 중 Volumes 뷰 반환
  - segment(`GLOBAL` | `LNG`) 필수. `TRADE`는 이 엔드포인트에서 허용하지 않는다
  - Type / Region / Country 다중 필터 지원 (미전달 시 전체)
  - granularity(`MONTHLY` | `YEARLY`) 지원, 기본 `MONTHLY`
  - periods 선택 필터로 원하는 연도 또는 월 컬럼만 조회
  - latest completed run의 real-DB 결과 조회
- Trade Flows 데이터 조회 (Trade)
  - `COMPLETED` case의 실행 결과 중 Trade Flow 매트릭스 반환
  - 별도 엔드포인트 `GET /{caseId}/volumes/trade` 사용
  - selector(`LNG` | `PNG`), year(2023~2030) 필수
  - unit(`MTPA` | `BCM`) 선택, 기본 `MTPA`
  - types(`TO_COUNTRY` | `FROM_COUNTRY`), Region / Country 다중 필터 지원 (미전달 시 전체)
  - granularity 없음 (단일 연도 매트릭스 구조)
  - fromCountries / toCountries 헤더 배열 + sparse flows 배열로 응답
  - Country 필터 적용 축도 실제 매칭 Country가 속한 저장 `REGION_SUBTOTAL` 헤더를 원래 순서로 유지하고, subtotal/total은 선택 Country leaf만으로 재계산
  - latest completed run의 real-DB 결과 조회
- Price 데이터 조회
  - `COMPLETED` case의 실행 결과 중 Price 뷰 반환
  - 필터 없음, segment 없음, granularity 없음
  - 차트 데이터(Base case price / User adjusted price) + Annual 테이블 + Monthly 테이블 동시 반환
  - latest completed run의 real-DB 결과 조회

### Current Exclusions

- 운영 수준 callback 인증/서명/재시도 정책
- Volumes 필터 저장
- input workbook 내용 조회·편집·재생성
- MI Base 지정 및 current MI Base 목록/상세/결과 read 확장과 mutation 차단. 해당 계약은 enhancement feature spec이 소유한다.

### Boundary Against Existing ScenarioModelExecution API

기존 `ScenarioModelExecution` API는 Next Gen의 parameter 기반 MI 수급모델 실행 리소스를 다룬다. 이 spec의 `FundamentalModelCase`는 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 파일 4개 업로드를 기준으로 생성되는 Scenario Case aggregate이며, 기존 `model-executions` resource와 별개 기능으로 구현한다.

구현 시 controller/DTO/service 명명도 다음처럼 분리한다.

| 구분 | 기존 API | 이번 API |
|---|---|---|
| 주요 resource | `model-executions` | `fundamental-model/cases` |
| 실행 식별자 | `executionId`, `externalRunId` | `caseId`, `runId` |
| 입력 방식 | JSON parameter | multipart file upload + metadata |
| 목적 | Scenario MI 수급모델 실행 | Scenario Case 기반 MI Fundamental Model 독립 실행 |

## 2. Endpoint

### Canonical Path Family

권장 base path:

```text
/api/v1/mi/fundamental-model
```

계약 원칙:

- MI Fundamental Model은 MI Next Gen와 동급 메뉴다.
- `Scenario Case` 명칭은 현업 용어로 유지하지만, API parent가 `Scenario`라는 뜻은 아니다.
- `scenarioId`는 MI Fundamental Model Case API의 path variable, query parameter, multipart part, response DTO에서 사용하지 않는다.
- canonical endpoint는 `/api/v1/mi/fundamental-model/**` path family만 사용한다.
- 기존 `/api/v1/mi/next-gen/**/fundamental-*` 및 `/api/v1/mi/next-gen/scenarios/{scenarioId}/fundamental-model-cases/**` 형태는 superseded contract다. 구현자는 이 경로를 신규 controller 계약으로 사용하지 않는다.

### Current Implemented Endpoint Summary

아래 endpoint는 현재 real-DB 구현 기준이다. case upload/run은 dedicated controller, 나머지 case 조회·변경과
결과 조회는 non-dedicated controller, callback은 dedicated run controller가 담당한다.

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

input 내용 제공 endpoint와 응답 형식은 아직 이 표에 추가하지 않는다. `caseId` 대상 식별과 서버 측 원본 선택 규칙은 [MI Fundamental Model Input 제공 Feature Spec](mi-fundamental-model-input-provision-feature-spec.md)을 따르며, 구조화 JSON의 DRM 비적용 가능 여부와 FE 데이터 요구를 확인한 후 해당 문서에서 최종 계약을 확정한다.

### Current and Deferred Integration Status

아래 항목은 이 문서에서 경계만 기록하며, 구현하려면 별도 feature spec 또는 DDL/ERD review가 필요하다.

| 항목 | 현재 상태 | 필요한 후속 source of truth |
|---|---|---|
| input/output/intermediate/tradeOutput 파일 다운로드 metadata | 상세 응답 계약 확정 | 이 feature spec의 `FundamentalModelCaseFilesRes`; 다운로드는 기존 공통 File API 사용 |
| 운영 Kubernetes Job dispatch | 구현 완료 (2026-06-12, 재시도 정책 no-retry 확정 2026-06-21) | external model interface spec + workflow design |
| callback 인증/서명 | 재시도 정책은 확정, 인증/서명 세부는 별도 운영 정책 | security/integration design + feature spec update |
| DB persistence / DDL (volumes/trade flows/price 포함) | 구현 완료 (2026-06-15 real DB rollout) | case/run DDL은 `scenario-case-execution-erd-ddl-draft.md`, output result data persistence는 `mi-fundamental-model-output-result-persistence-design.md` |
| Ownership scope | DB-backed 확정 사항 | owner-scoped mutation(run/update/delete 등)은 `usr_no` only로 확정. 조직/부서/tenant 정책 키는 본 모델에 추가하지 않는다. Volumes/Trade Flows/Price result read는 다른 사용자가 실행한 결과도 조회 가능한 shared read model이므로 `usr_no` owner predicate를 적용하지 않는다 |
| `ASSIGNEE_NAME` DB 검색 경계 | DB-backed 확정 사항 + 검증 필요 | `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no` JOIN, 표시값 `tmd_usrm.usr_nm` 사용. rollout 전 JOIN 양측 인덱스 EXPLAIN 검증 필요 |
| timeout scheduler | 코드 구현 완료 (2026-06-22), DBA index 검증 전까지 feature flag 기본 비활성 | 상세는 `mi-scenario-model-execution-workflow-design.md` Open Questions 참고 |
| Volumes/Price 필터 저장 (Save 토글) | 현재 제외. FE 로컬 상태로 관리 | user preference API feature spec + DB 테이블 설계 |

### Deferred Contracts

아래 내용은 현재 구현 계약이 아니며 별도 feature spec/design에서 확정한 뒤 구현한다.

- 운영 callback 인증/서명 세부 정책
- result 조회/download endpoint와 result pointer/reference persistence
- Volumes/Price 필터 저장 API와 user preference persistence
- delete retention/audit policy beyond `tmd_mdl_scnrcasem.del_yn = 'Y'`

### Status Query Contract

현재 계약은 case 기준 상태 조회를 사용한다.

| 방식 | 장점 | 단점 | 판단 |
|---|---|---|---|
| case 기준 | `PENDING`처럼 아직 `run_id`가 없는 상태도 조회 가능하다. FE가 업로드 응답의 `caseId`만 보관하면 된다. | 과거 실행 이력 중 특정 run만 조회하려면 추가 endpoint가 필요하다. | 현재 계약 |
| `run_id` 기준 | 외부 모델 callback 추적 키가 동일하다. 실행 이력 중심 조회에 적합하다. | 업로드 직후 `PENDING` case 조회가 불가능하다. | 후속 실연동/이력 조회에서 검토 |

후속으로 이력 조회가 필요해지면 `GET /api/v1/mi/fundamental-model/runs/{runId}` 또는 case 하위 `GET /api/v1/mi/fundamental-model/cases/{caseId}/runs/{runId}`를 별도 계약으로 추가한다.

### Detail Query Contract

상세 조회는 화면 상세 진입 시 필요한 case aggregate projection을 반환한다.

- canonical endpoint: `GET /api/v1/mi/fundamental-model/cases/{caseId}`
- response DTO: `FundamentalModelCaseDetailRes`
- 포함 범위:
  - case metadata (`caseId`, `title`, `description`, `status`, `assigneeName`, `createdAt`, `updatedAt`)
  - 파일 metadata (`files.input`, `files.output`, `files.intermediate`, `files.tradeOutput`)
  - 최신 실행 projection (`latestRun`)
- 제외 범위:
  - full run history
  - result payload
  - result 조회 contract

`files.input`은 `tcm_filel.file_rel_obj_spt_cd = 'SCNRCASE'` 및 `file_rel_obj_dstg_no = case_id` 기준 파일 목록이다.
`files.output`은 최신 실행의 `run_seq`를 기준으로 `tcm_filel.file_rel_obj_spt_cd = 'SCNRCASEH'` 및 `file_rel_obj_dstg_no = run_seq`에 연결된 파일 목록이다.
`files.intermediate`는 같은 `run_seq`를 기준으로 `file_rel_obj_spt_cd = 'SCNRCASEHI'`에 연결된 모델 실행 중간파일 목록이다 (2026-09-07 추가). 개수는 고정하지 않는다(n개).
`files.tradeOutput`은 같은 `run_seq`를 기준으로 `file_rel_obj_spt_cd = 'SCNRCASEHTF'`에 연결된 Trade Flow 결과파일 목록이다 (2026-09-17 추가). 기존 output/intermediate와 별도 목록이며 개수는 고정하지 않는다.
네 목록 모두 `del_yn = 'N'`과 파일 유효종료일자 조건을 만족하는 row를 반환한다.
각 목록은 서로 독립적으로 `fileNm`을 대소문자 구분 없이 일반 문자열 오름차순으로 정렬하며, 숫자 자연정렬은 적용하지 않는다.
파일명 비교 결과가 같으면 원본 row의 `file_no ASC`를 보조 기준으로 사용한다. 최신 실행이나 해당 파일이 없으면 빈 배열이다.

`files.input`, `files.output`, `files.intermediate`, `files.tradeOutput` 항목은 기존 공통 File API 다운로드 요청에 전달할
`fileNo`, `fileNm`, `fileSize`를 반환한다. 이 계약은 상세 조회와 같은
projection을 반환하는 수정 응답에도 동일하게 적용한다. 업로드 응답 `FundamentalModelCaseFileRes`는 기존 계약을 유지한다.
중간파일/Trade Flow 결과파일 식별 방식과 pod 적재 계약은
[mi-fundamental-model-intermediate-file-design.md](../design-docs/gas/mi-fundamental-model-intermediate-file-design.md)가 canonical이다.

`GET /cases/{caseId}/status`는 FE polling을 위한 경량 상태 조회로 유지한다. 상태 조회는 파일 metadata와 설명 등 화면 상세 metadata를 반환하지 않는다. 반대로 상세 조회는 화면 상세 rendering에 필요한 projection이며, polling interval에서 반복 호출하는 API로 설계하지 않는다.

### Callback Path Contract

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

## 3. Request Contract

### 3.1 Scenario Case 목록 조회

> 이 절의 기존 list request/behavior는 enhancement 계약으로 superseded됐다. current MI Base 포함, 검색 적용 범위, 응답 DTO는 [MI Fundamental Model MI Base 지정 Feature Spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 canonical source다.

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
- 검색 조건이 없으면 전체 case를 반환한다. 현재 계약은 `scenarioId` 필터를 제공하지 않는다.
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
| `files` | `MultipartFile[]` | Y | 정확히 4개, 각 파일 non-empty, 파일명 역할 키워드 검증 | 모델 실행에 필요한 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 입력 파일 |

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

- `files.size() == 4` 가 아니면 거절한다.
- 파일 중 하나라도 비어 있으면 거절한다.
- 파일명에는 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 키워드가 각각 정확히 1개씩 포함되어야 한다. 비교는 대소문자 무관이며, 하나의 파일명이 여러 역할 키워드를 동시에 포함하거나 특정 역할 키워드가 누락/중복되면 `400 REQ_INVALID_PARAMETER`로 거절한다. 여기서 `OUTPUT`은 실행 결과 파일이 아니라 모델 실행에 먼저 전달하는 네 번째 입력 파일 역할이다.
- 확장자/MIME/크기 검증은 기존 `FileService` 정책을 재사용하고 해당 오류를 `FILE_FORMAT_ERROR`, `FILE_SIZE_ERROR`로 매핑한다.
- 네 파일은 하나의 Case aggregate로 업로드한다. 하나라도 업로드 또는 file metadata 저장에 실패하면 API 전체를 실패시키고, 그 전에 성공한 파일은 기존 보상 삭제 흐름으로 정리한다.
- 같은 파일명 자체는 허용하지만 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할 키워드는 각각 정확히 1개여야 한다.

### 3.3 Scenario Case 실행 요청

- Method: `POST`
- Content-Type: `application/json`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Request body: 없음

실행 요청은 Kubernetes Job dispatch가 성공하면 상태를 `RUNNING`으로 유지하고 `202 Accepted`를 반환한다.
완료/실패는 callback 또는 timeout/reconciliation 경로가 terminal 상태로 전이하며 자동 완료 데모 옵션은 사용하지 않는다.

실행 가능한 case 상태는 최초 실행의 `PENDING`과 모델 호출 실패 후 재시도의 `FAILED`다. `COMPLETED`는 성공 종료 상태이므로 같은 case를 다시 실행할 수 없다. 새로운 정상 실행은 파일 업로드 API로 별도 `INDEPENDENT` case를 생성한다. `FAILED` 재시도는 기존 case를 유지하면서 새 `runId`와 `tmd_mdl_scnrcaseh` 이력만 생성한다.

backend는 Kubernetes Job을 생성할 때 `runId`, `runSeq`, `DEMAND`/`SUPPLY`/`TRADE`/`OUTPUT` storage path 4개와 callback URL을 전달한다. 역할별 CLI 인자는 각각 `--demand-excel-path`, `--supply-excel-path`, `--trade-excel-path`, `--output-excel-path`다. `--output-excel-path`는 모델 실행 전에 소비하는 `OUTPUT` 역할 input workbook 경로이며, 모델 실행 결과 저장 위치를 뜻하지 않는다. 결과 파일 적재는 기존 `runSeq`와 `SCNRCASEH` 관계 계약을 유지한다. Kubernetes API의 Job 생성 성공은 장시간 실행 결과가 아니라 dispatch 접수 성공 여부만 표현한다. 접수 성공이면 backend는 case/run 상태를 `RUNNING`으로 유지하고 클라이언트에 `202 Accepted`를 반환한다. pod 내부 실행 완료 또는 실패는 pod 또는 실행 프로세스가 backend callback API를 호출해 terminal 상태로 전이한다. backend가 pod 내부 Python 완료 여부를 주기적으로 polling 하는 방식은 채택하지 않는다.

실행 요청은 case 상태를 변경하고 새 run을 생성하는 owner-scoped mutation이다. DB-backed 구현은 `case_id + usr_no ownership + del_yn = 'N' + exec_type_cd = 'INDEPENDENT'` 기준으로 active owned 독립실행 case인지 확인한 뒤 실행 선점/상태 전이를 수행한다. 소유자가 아니거나 `MI_BASE | SCENARIO`인 case의 실행 요청은 active resource hiding 규칙에 따라 `RSC_NOT_FOUND`로 처리한다.

### 3.4 Scenario Case 상세 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters: 없음

규칙:

- active `execType = INDEPENDENT | MI_BASE` case이면 case metadata, `execType`, input/output/intermediate/tradeOutput 다운로드 metadata, latest run projection을 반환한다. `SCENARIO`는 이 API의 active resource가 아니므로 `404 RSC_NOT_FOUND`로 처리한다.
- `latestRunId`가 없거나 아직 실행되지 않은 `PENDING` case이면 `latestRun = null`이다.
- full run history는 반환하지 않는다.
- result payload, result reference, presigned download URL은 반환하지 않는다. `files.input[]`, `files.output[]`, `files.intermediate[]`,
  `files.tradeOutput[]`은 공통 File API 다운로드 요청용 `fileNo`, `fileNm`, `fileSize`를 반환한다.
- case가 없으면 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given active case ID, when 상세 조회하면, then case metadata, files, latestRun을 반환한다.
- Given 업로드 직후 case, when 상세 조회하면, then `status = PENDING`, input files 4개, `latestRun = null`을 반환한다.
- Given 실행 요청 후 case, when 상세 조회하면, then `status = RUNNING`, latestRun 정보를 반환한다.
- Given 목록의 current MI Base case ID, when 상세 조회하면, then 기존 상세 정보와 `execType = MI_BASE`를 반환한다.
- Given active INDEPENDENT case ID, when 상세 조회하면, then `execType = INDEPENDENT`를 반환한다.
- Given SCENARIO case ID, when 상세 조회하면, then `404 RSC_NOT_FOUND`로 응답한다.
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
| `title` | `String` | Y | trim 후 1-200자 | Scenario Case 제목. `tmd_mdl_scnrcasem.case_titl_nm`에 매핑 |
| `description` | `String` | N | nullable/optional | Scenario Case 설명. `tmd_mdl_scnrcasem.case_expn`에 매핑 |

규칙:

- request body는 `title`, `description`만 허용한다.
- `title`은 null, 누락, blank를 허용하지 않는다. 저장 전 trim한다.
- `description`은 null 또는 누락을 허용하며, 전달된 값을 `case_expn` equivalent에 반영한다.
- 수정 대상은 case metadata인 `case_titl_nm`, `case_expn`뿐이다.
- files, status, latestRun, run history, result payload/download 정보는 수정하지 않는다.
- 수정 응답은 최신 `FundamentalModelCaseDetailRes`를 반환한다.
- 수정은 active `INDEPENDENT`만 허용한다. case가 없거나 soft-deleted 또는 `MI_BASE | SCENARIO`이면 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given active case, when `title`, `description`을 수정하면, then 최신 상세 응답에서 두 필드만 변경된다.
- Given active case with files/latestRun/status, when 수정하면, then files/latestRun/status는 유지된다.
- Given blank title, when 수정하면, then `400 REQ_INVALID_PARAMETER`로 거절한다.
- Given soft-deleted caseId, when 수정하면, then `404 RSC_NOT_FOUND`로 응답한다.
- Given current MI Base caseId, when 수정하면, then `404 RSC_NOT_FOUND`로 응답한다.

### 3.6 Scenario Case 삭제

- Method: `DELETE`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Request body: 없음
- Response: `BaseResponse<Void>` (`200 OK`)

규칙:

- 삭제는 physical delete가 아니라 `tmd_mdl_scnrcasem.del_yn = 'Y'` soft delete semantics로 처리한다.
- real-DB 구현은 `tmd_mdl_scnrcasem.del_yn`을 `Y`로 갱신한다.
- 삭제 시 upload files, run history, latestRun/result payload는 물리 삭제하지 않는다.
- 삭제된 case는 list/detail/status/run/update/delete에서 active resource가 아니므로 `404 RSC_NOT_FOUND`로 취급한다.
- 반복 delete는 이미 active resource가 아니므로 `404 RSC_NOT_FOUND`로 응답한다.
- 삭제는 active `INDEPENDENT`만 허용한다. `MI_BASE | SCENARIO` case는 `404 RSC_NOT_FOUND`로 응답한다.

Acceptance criteria:

- Given active case, when 삭제하면, then `200 OK`와 `BaseResponse<Void>`를 반환한다.
- Given deleted case, when 목록 조회하면, then 목록에서 제외된다.
- Given deleted case, when detail/status/run/update/delete를 호출하면, then `404 RSC_NOT_FOUND`로 응답한다.
- Given current MI Base caseId, when 삭제하면, then `404 RSC_NOT_FOUND`로 응답한다.

### 3.7 Scenario Case 상태 조회

- Method: `GET`
- Path variables:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `caseId` | `Long` | Y | Scenario Case ID |

- Query parameters: 없음

상태 조회는 polling용 경량 projection이다. 상세 조회와 달리 `description`, `assigneeName`, `files`를 반환하지 않는다.

### 3.8 Removed: Mock 데이터 초기화 (historical)

1차 mock의 `POST /api/v1/mi/fundamental-model/cases/reset` 계약은 real-DB 전환 때 제거되었다.
현재 controller/service에는 reset endpoint와 method가 없으며, 이 경로를 다시 구현하거나 OpenAPI에 노출하지 않는다.

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
| `finishedAt` | `LocalDateTime` | Y | 외부 모델의 UTC 실행 완료/실패 시각. offset 없는 ISO-8601 값(`yyyy-MM-ddTHH:mm:ss`)으로 전달하며 Backend가 KST로 변환해 저장 |
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
- Query parameters:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `caseId` | `Long` | N | — | 실제 Region/Country/기간 옵션을 조회할 Scenario Case ID. 미전달 시 정적 type/region/granularity 옵션만 반환 |
| `selector` | `TradeFlowSelector` | N | — | TRADE Region/Country 옵션의 데이터 범위. `year`와 함께 전달해야 하며 `LNG` \| `PNG` |
| `year` | `Integer` | N | — | TRADE Region/Country 옵션의 데이터 범위. `selector`와 함께 전달해야 하며 2023~2030 |

규칙:

- segment별로 허용되는 type 목록, Region 아래 Country가 중첩된 region 목록, granularity 목록을 반환한다.
- `caseId`가 전달되면 해당 case의 최신 완료 run 결과를 기준으로 실제 데이터 row/header가 존재하는 Region/Country만 반환한다. 값이 0인 row/header도 존재하면 옵션에 포함한다.
- GLOBAL/LNG의 Region/Country는 각각 해당 segment의 MONTHLY/YEARLY 결과 합집합이며, 기간 옵션은 bucket의 `periodKeys`를 기준으로 반환한다.
- TRADE의 Region/Country는 `caseId + selector + year`에 해당하는 From/To header의 합집합이다. 같은 Country가 양쪽에 있으면 한 번만 반환한다.
- TRADE 옵션을 실제 데이터로 조회하려면 `caseId`, `selector`, `year`를 함께 전달한다. `selector`와 `year` 중 하나만 전달하거나, `caseId` 없이 두 값을 전달하면 `400 REQ_INVALID_PARAMETER`로 응답한다.
- `caseId`가 전달되지 않으면 모든 Region을 `countries=[]`로 반환하고 `periods`는 빈 배열을 반환한다.
- `caseId`만 전달되어 TRADE 조회 범위가 없는 경우 TRADE는 모든 Region을 `countries=[]`로 반환한다. GLOBAL/LNG는 실제 데이터 기준 옵션을 반환한다.
- TRADE segment의 `types`는 `TradeFlowDirection` 값(`TO_COUNTRY`, `FROM_COUNTRY`)이며, `granularities`는 빈 배열이다.
- TRADE segment는 단일 `year` matrix API를 사용하므로 segment `periods`는 빈 배열이다.
- GLOBAL/LNG segment의 `types`는 `FundamentalModelDataType` 값이며, `granularities`는 `MONTHLY`, `YEARLY`를 반환한다.
- Region은 `FundamentalModelRegion` enum 순서, 각 Region의 Country는 `label` 오름차순으로 반환한다.
- 항상 GLOBAL → LNG → TRADE 순서로 반환한다.
- `caseId`가 `COMPLETED` 상태가 아니면 결과 조회 API와 동일하게 `409 BIZ_RULE_VIOLATION`으로 응답한다.

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
| `countryKeys` | `String[]` | N | 전체 | Country 식별자 다중값. `COUNTRY:{REGION}:{country}` 형식이며 filter-options의 `regions[].countries[].value`를 전달한다 |
| `granularity` | `FundamentalModelGranularity` | N | `MONTHLY` | `MONTHLY` \| `YEARLY` |
| `periods` | `String[]` | N | 전체 | 선택 기간. `granularity=YEARLY`이면 `yyyy`, `granularity=MONTHLY`이면 `yyyy-MM` 형식의 쉼표 구분 다중값 |

규칙:

- `caseId`에 해당하는 case가 `COMPLETED` 상태여야 한다. `PENDING` 또는 `RUNNING`이면 `409 BIZ_RULE_VIOLATION`, `FAILED` 또는 soft-deleted이면 `404 RSC_NOT_FOUND`로 응답한다.
- `segment=TRADE`가 전달되면 `400 REQ_INVALID_PARAMETER`로 응답한다. Trade Flows 조회는 `GET /{caseId}/volumes/trade`를 사용한다.
- `types`, `regions`, `countryKeys` 모두 미전달 시 segment별 허용 Type과 전체 Region/Country 데이터를 반환한다.
- `regions`와 `countryKeys`를 함께 전달하면 AND 조건으로 적용한다. 즉 선택 Region 안에서 선택 Country만 조회한다.
- `countryKeys`만 전달하면 key에서 Region을 추출해 `regions`를 자동 적용한다. 응답 `appliedFilters.regions`에는 추출된 Region 목록을 반환한다.
- `countryKeys`는 대소문자를 포함한 exact match이며 형식이 잘못되거나 Region enum이 유효하지 않으면 `400 REQ_INVALID_PARAMETER`로 응답한다. 형식은 유효하지만 최신 완료 run 결과에 없는 key이면 오류가 아니라 빈 결과를 반환한다.
- `countryKeys`의 null/빈 목록은 미전달과 같고, 중복 key는 첫 등장 순서를 유지해 제거한다. 응답 `appliedFilters.countryKeys`에는 이 정규화된 요청값을 반환한다.
- `segment=LNG`에서 `DEMAND`, `PRODUCTION`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`가 `types`로 전달되면 `400 REQ_INVALID_PARAMETER`로 응답한다.
- `countryKeys`가 전달되면 선택된 Country의 `COUNTRY` 행만 반환한다. 각 `REGION_SUBTOTAL`은 해당 Region에서 선택된 Country 값의 합, `TYPE_TOTAL`은 선택된 모든 Country 값의 합으로 다시 계산한다.
- Country 필터 subtotal/total은 원본 `COUNTRY` 숫자를 `BigDecimal`로 합산한 뒤 응답 경계에서 한 번만 소수 다섯째 자리까지 0 방향 절삭한다. leaf 값을 먼저 절삭한 뒤 합산하지 않는다.
- `countryKeys`가 전달되지 않으면 `REGION_SUBTOTAL` 행은 선택된 Region별 원본 subtotal을, `TYPE_TOTAL` 행은 선택된 Region들의 `REGION_SUBTOTAL` 합을 나타낸다.
- FE는 `rows`를 응답 순서 그대로 렌더링할 수 있다. 서버는 각 Type 안에서 Region별로 `REGION_SUBTOTAL` 행을 먼저 반환하고, 이어서 해당 Region의 `COUNTRY` 행들을 반환한다. Type의 마지막 행은 `TYPE_TOTAL`이다.
- `granularity=YEARLY`이면 월별 데이터를 연간으로 집계해 반환한다. columns의 `month` 필드는 `null`이 된다.
- `granularity=MONTHLY`이면 columns의 `month` 필드는 1~12 정수다.
- `periods`가 전달되면 `columns`와 각 row의 `values`는 선택 기간과 일치하는 항목만 포함한다.
- 선택 기간이 실제 result data에 없으면 에러가 아니라 `200 OK`로 응답하고 `columns=[]`, `rows=[]`를 반환한다.
- `periods` 값이 granularity별 형식과 맞지 않으면 `400 REQ_INVALID_PARAMETER`로 응답한다.
- 조회 조건에 매칭되는 실제 result row가 없으면 에러가 아니라 `200 OK`로 응답하고 `columns=[]`, `rows=[]`를 반환한다.
- latest completed run의 Volumes result row를 조회하고 application layer에서 필터·subtotal·total을 계산한다.

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
- 차트는 `BASE_CASE`와 `USER_ADJUSTED` 두 series를 포함한다.
- Annual 테이블과 Monthly 테이블의 행 `label`은 데이터 기반 동적 값이다. 서버가 고정 enum으로 강제하지 않는다.
- Price 응답의 수치 필드(`chart.*.points[].value`, `annualTable.rows[].baseCasePrice`, `annualTable.rows[].userAdjustedPrice`, `monthlyTable.rows[].baseCasePrice`, `monthlyTable.rows[].userAdjustedPrice`)는 원본 값을 변경하지 않고 응답 직렬화 시 소수점 셋째 자리에서 `HALF_UP`으로 반올림한 JSON number로 반환한다.
- 실제 price result row가 없으면 에러가 아니라 `200 OK`로 응답하고 chart series와 table rows는 빈 배열로 반환한다. table columns는 유지한다.
- latest completed run의 Price result row를 조회해 응답을 조립한다.


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
| `types` | `TradeFlowDirection[]` | N | 전체 | `TO_COUNTRY` \| `FROM_COUNTRY`. 미전달 시 전체. Region/Country 필터 적용 방향을 결정한다 |
| `regions` | `FundamentalModelRegion[]` | N | 전체 | 쉼표 구분 다중값. 미전달 시 전체 포함 |
| `countryKeys` | `String[]` | N | 전체 | Country 식별자 다중값. `COUNTRY:{REGION}:{country}` 형식이며 filter-options의 `regions[].countries[].value`를 전달한다. `regions` 미전달 시 key의 Region을 적용한다 |

규칙:

- `caseId`에 해당하는 case가 `COMPLETED` 상태여야 한다. 상태 조건은 §3.10과 동일하다.
- `selector`, `year`는 필수다. 미전달 시 `400 REQ_INVALID_PARAMETER`.
- `year`가 2023~2030 범위 밖이면 `400 REQ_INVALID_PARAMETER`.
- `regions`와 `countryKeys`의 조합 및 validation은 §3.11과 동일하다. 두 값이 함께 전달되면 AND 조건이며, `countryKeys`만 전달하면 key에서 Region을 추출해 필터와 응답 `appliedFilters.regions`에 적용한다.
- `types`는 Region/Country 필터를 적용할 방향을 결정한다.
  - `types=TO_COUNTRY`이면 To 축만 선택 Region/Country로 축소하고 From 축은 전체 유지한다.
  - `types=FROM_COUNTRY`이면 From 축만 선택 Region/Country로 축소하고 To 축은 전체 유지한다.
  - `types=TO_COUNTRY,FROM_COUNTRY`이거나 미전달이면 동일한 Region/Country 선택 목록을 양쪽 축에 각각 적용한다.
- 방향별 Country 목록은 따로 받지 않는다. 따라서 `FROM_COUNTRY`, `TO_COUNTRY`를 함께 선택한 요청에서 “From은 Angola, To는 Japan”처럼 서로 다른 Country 조건을 지정할 수 없다.
- Country 필터가 적용된 방향은 실제 매칭된 `COUNTRY` header와 그 Country가 속한 저장 `REGION_SUBTOTAL` header를 반환한다. 이때 저장 header의 원래 순서를 유지하므로 각 Region subtotal은 해당 Region의 선택 Country보다 앞에 위치한다.
- `TOTAL` header/flow는 응답 축의 마지막에 유지한다. Country 필터가 한 방향이라도 적용되면 저장된 집계 flow를 재사용하지 않고 필터 후 남은 Country-to-Country leaf flow만으로 응답의 Region subtotal과 Total을 다시 계산한다.
- Country 필터 subtotal/total은 원본 Country-to-Country leaf 숫자를 `BigDecimal`로 합산한 뒤 응답 경계에서 한 번만 소수 다섯째 자리까지 0 방향 절삭한다. 응답용으로 절삭된 leaf 값을 재합산하지 않는다.
- flows는 0이 아닌 값만 포함한다 (sparse). fromCountries × toCountries 매트릭스에서 값이 없는 셀은 프론트가 0(또는 `-`)으로 처리한다.
- 유효한 형식의 `countryKeys`가 필터 적용 축의 실제 header에 하나도 매칭되지 않으면 에러가 아니라 `200 OK`로 응답하고 `fromCountries=[]`, `toCountries=[]`, `flows=[]`를 반환한다.
- 조회 조건에 매칭되는 실제 trade result row가 없으면 에러가 아니라 `200 OK`로 응답하고 `fromCountries=[]`, `toCountries=[]`, `flows=[]`를 반환한다. header는 있으나 flow row만 없으면 header는 반환하고 `flows=[]`로 응답한다.
- latest completed run의 Trade header/flow row를 조회하고 `selector`/`year` 및 선택 필터를 적용한다.

## 4. Response Contract

모든 응답은 `BaseResponse<T>`를 사용한다. 성공 시 `status=true`, `code=SUC-000`, `message=Success.`, `data=<DTO>` 형태를 따른다.

### 4.1 `FundamentalModelCaseListRes`

> 아래 1차 mock 예시와 필드 표는 historical baseline이다. `miBase`, `execType`, 변경된 `totalCount` 의미는 [MI Base 지정 feature spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 supersede한다.

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
| `assigneeName` | `String` | `tmd_mdl_scnrcasem.usr_no`로 연결된 사용자 이름 |
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
  "execType": "INDEPENDENT",
  "assigneeName": "홍길동",
  "assigneeUserNo": 1,
  "files": {
    "input": [
      {
        "fileNo": 1001,
        "fileNm": "input_1.xlsx",
        "fileSize": 12345
      }
    ],
    "output": [
      {
        "fileNo": 2011,
        "fileNm": "output.xlsx",
        "fileSize": 54321
      }
    ],
    "intermediate": [
      {
        "fileNo": 3011,
        "fileNm": "step1_demand_curve.csv",
        "fileSize": 1234
      },
      {
        "fileNo": 3012,
        "fileNm": "step2_supply_stack.csv",
        "fileSize": 2345
      }
    ],
    "tradeOutput": [
      {
        "fileNo": 4011,
        "fileNm": "trade_flow_result.xlsx",
        "fileSize": 67890
      }
    ]
  },
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
| `execType` | `FundamentalModelExecType` | 실행 유형. 상세 조회에서는 `INDEPENDENT \| MI_BASE`, non-null |
| `assigneeName` | `String` | `tmd_mdl_scnrcasem.usr_no`로 연결된 사용자 이름 |
| `assigneeUserNo` | `Integer` | case를 업로드한 사용자 번호. `tmd_mdl_scnrcasem.usr_no` |
| `files` | `FundamentalModelCaseFilesRes` | input/output/intermediate/tradeOutput 다운로드 metadata |
| `latestRun` | `FundamentalModelRunStatusRes` | 최신 실행 정보. `PENDING`이면 `null` |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `updatedAt` | `LocalDateTime` | case 최종 변경 시각 |

상세 응답은 active `INDEPENDENT | MI_BASE` case를 지원하고 최신 실행 1건만 포함한다. 전체 실행 이력은 별도 runs API가 정의되기 전까지 이 응답에 포함하지 않는다.
상세 `GET` 허용 범위는 수정·삭제·실행 범위를 넓히지 않으며, 이 mutation들은 active `INDEPENDENT`만 허용한다. MI Base 지정 actor/time은 별도 계약 전까지 응답하지 않는다.
result payload는 포함하지 않는다. input/output/intermediate/tradeOutput 파일은 기존 공통 File API에 전달할
`fileNo`, `fileNm`, `fileSize`를 제공한다.

#### `FundamentalModelCaseFilesRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `input` | `List<FundamentalModelDownloadFileRes>` | `SCNRCASE + case_id`로 연결된 입력 파일 다운로드 metadata 목록. 업로드 기준 4개 |
| `output` | `List<FundamentalModelDownloadFileRes>` | 최신 실행 `SCNRCASEH + run_seq`로 연결된 output 파일 목록. 최신 실행 또는 output 파일이 없으면 빈 배열 |
| `intermediate` | `List<FundamentalModelDownloadFileRes>` | 최신 실행 `SCNRCASEHI + run_seq`로 연결된 모델 실행 중간파일 목록(n개). 최신 실행 또는 중간파일이 없으면 빈 배열 |
| `tradeOutput` | `List<FundamentalModelDownloadFileRes>` | 최신 실행 `SCNRCASEHTF + run_seq`로 연결된 Trade Flow 결과파일 목록. 최신 실행 또는 Trade Flow 결과파일이 없으면 빈 배열 |

#### `FundamentalModelDownloadFileRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `fileNo` | `Long` | 공통 File API 다운로드 요청의 `fileNo` |
| `fileNm` | `String` | 공통 File API 다운로드 요청의 `fileNm` (`tcm_filel.org_file_nm`) |
| `fileSize` | `Long` | 공통 File API 다운로드 요청의 `fileSize` (byte) |

`input`, `output`, `intermediate`, `tradeOutput`은 모두 `fileNo`, `fileNm`, `fileSize`의 동일한 다운로드 metadata shape다.

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
| `caseId` | `Long` | `tmd_mdl_scnrcasem.case_id` |
| `title` | `String` | 제목 |
| `description` | `String` | 설명. 입력 없으면 `null`. `tmd_mdl_scnrcasem.case_expn`에 매핑 |
| `status` | `FundamentalModelCaseStatus` | 생성 직후 `PENDING` |
| `files` | `List<FundamentalModelCaseFileRes>` | 업로드한 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 파일 4개 metadata |
| `createdAt` | `LocalDateTime` | case 생성 시각 |
| `statusEndpoint` | `String` | case 기준 상태 조회 endpoint |

### 4.5 `FundamentalModelCaseFileRes`

| 필드 | 타입 | 설명 |
|---|---|---|
| `fileNo` | `Long` | 파일 식별자. `tcm_filel.file_no` |
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
| `runId` | `String` | backend가 생성해 `tmd_mdl_scnrcaseh.run_id`에 저장하는 `yyyymmdd_hhmmss_XXXX` 포맷 실행 ID |
| `status` | `FundamentalModelRunStatus` | 실행 접수 직후 `RUNNING` |
| `requestedAt` | `LocalDateTime` | 실행 요청 시각 |
| `startedAt` | `LocalDateTime` | 실행 시작 시각. 현재 접수 시각과 동일하게 저장 |
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
| `startedAt` | `LocalDateTime` | 실행 시작 시각. `tmd_mdl_scnrcaseh.exec_bgn_dtm` 매핑 |
| `finishedAt` | `LocalDateTime` | 완료/실패 시각. `tmd_mdl_scnrcaseh.exec_end_dtm` 매핑. 실행 중이면 `null` |
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
    "regions": ["ASIA"],
    "countryKeys": ["COUNTRY:ASIA:China", "COUNTRY:ASIA:Japan"],
    "periods": []
  },
  "columns": [
    { "key": "2026-01", "label": "26-01", "year": 2026, "month": 1 },
    { "key": "2026-02", "label": "26-02", "year": 2026, "month": 2 }
  ],
  "rows": [
    {
      "type": "DEMAND",
      "typeLabel": "Gas Demand",
      "region": "ASIA",
      "country": null,
      "rowKind": "REGION_SUBTOTAL",
      "rowLabel": "Asia",
      "values": { "2026-01": 0.3, "2026-02": 0.3 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Gas Demand",
      "region": "ASIA",
      "country": "China",
      "rowKind": "COUNTRY",
      "rowLabel": "China",
      "values": { "2026-01": 15.1, "2026-02": 0.3 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Gas Demand",
      "region": "ASIA",
      "country": "Japan",
      "rowKind": "COUNTRY",
      "rowLabel": "Japan",
      "values": { "2026-01": 0.2, "2026-02": 0.2 }
    },
    {
      "type": "DEMAND",
      "typeLabel": "Gas Demand",
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
| `unit` | `FundamentalModelVolumeUnit` | 수치 단위. 현재 `MTPA` 고정 |
| `appliedFilters` | `VolumesAppliedFilters` | 실제 적용된 필터. 미전달 시 전체 enum 목록 |
| `columns` | `List<VolumesColumn>` | 시간 축 메타데이터 목록 |
| `rows` | `List<VolumesRow>` | 데이터 행 목록 |

**`VolumesAppliedFilters`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `types` | `List<FundamentalModelDataType>` | 실제 적용된 type 목록 |
| `regions` | `List<FundamentalModelRegion>` | 실제 적용된 region 목록 |
| `countryKeys` | `List<String>` | 실제 적용된 Country key 목록. 미전달 시 빈 배열이며 선택 Region의 전체 Country가 적용됨 |
| `periods` | `List<String>` | 요청으로 선택한 기간. 미전달 시 빈 배열이며 전체 기간이 적용됨 |

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
| `typeLabel` | `String` | Type section 화면 표시명. 예: `Gas Demand`, `Gas Production`, `LNG Imports` |
| `region` | `FundamentalModelRegion` | 지역. `rowKind=TYPE_TOTAL`이면 `null` |
| `country` | `String` | 국가명. `rowKind=REGION_SUBTOTAL` 또는 `TYPE_TOTAL`이면 `null` |
| `rowKind` | `VolumesRowKind` | `COUNTRY` \| `REGION_SUBTOTAL` \| `TYPE_TOTAL` |
| `rowLabel` | `String` | 행 화면 표시명. `REGION_SUBTOTAL`이면 지역 label, `COUNTRY`이면 국가명, `TYPE_TOTAL`이면 `Total` |
| `values` | `Map<String, Double>` | column key별 수치 map. key는 `columns[].key`와 동일하며 FE는 `row.values[column.key]`로 조회한다. JSON number로 반환하며 각 원천값과 합계 결과를 소수 다섯째 자리까지 0 방향 절삭한다. Country 필터 합계는 원본 leaf 합산 후 한 번만 절삭한다. |

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
`MTPA` — 현재 Volumes 응답의 `unit`은 항상 `MTPA`다. 후속 unit 변환 기능은 별도 request contract로 정의한다.

### 4.12 `FundamentalModelVolumesFilterOptionsRes`

아래는 `?caseId=106&selector=LNG&year=2026` 호출 예시다. TRADE의 Region/Country는 이 selector/year의 From/To 합집합이다.

```json
{
  "segments": [
    {
      "segment": "GLOBAL",
      "types": [
        { "value": "DEMAND",           "label": "Gas Demand" },
        { "value": "PRODUCTION",       "label": "Gas Production" },
        { "value": "LNG_IMPORTS",      "label": "LNG Imports" },
        { "value": "LNG_EXPORTS",      "label": "LNG Exports" },
        { "value": "PIPELINE_IMPORTS", "label": "Pipeline Imports" },
        { "value": "PIPELINE_EXPORTS", "label": "Pipeline Exports" }
      ],
      "regions": [
        {
          "value": "ASIA",
          "label": "Asia",
          "countries": [
            { "value": "COUNTRY:ASIA:China", "label": "China" },
            { "value": "COUNTRY:ASIA:Japan", "label": "Japan" }
          ]
        }
      ],
      "granularities": [
        { "value": "MONTHLY", "label": "Monthly" },
        { "value": "YEARLY",  "label": "Yearly" }
      ],
      "periods": {
        "yearly": [
          { "value": "2023", "label": "2023" }
        ],
        "monthly": [
          { "value": "2023-01", "label": "Jan" }
        ]
      }
    },
    {
      "segment": "LNG",
      "types": [
        { "value": "LNG_IMPORTS", "label": "LNG Imports" },
        { "value": "LNG_EXPORTS", "label": "LNG Exports" }
      ],
      "regions": [
        {
          "value": "ASIA",
          "label": "Asia",
          "countries": [
            { "value": "COUNTRY:ASIA:China", "label": "China" },
            { "value": "COUNTRY:ASIA:Japan", "label": "Japan" }
          ]
        }
      ],
      "granularities": [
        { "value": "MONTHLY", "label": "Monthly" },
        { "value": "YEARLY",  "label": "Yearly" }
      ],
      "periods": {
        "yearly": [{ "value": "2026", "label": "2026" }],
        "monthly": [{ "value": "2026-01", "label": "Jan" }]
      }
    },
    {
      "segment": "TRADE",
      "types": [
        { "value": "TO_COUNTRY",   "label": "To country" },
        { "value": "FROM_COUNTRY", "label": "From country" }
      ],
      "regions": [
        {
          "value": "ASIA",
          "label": "Asia",
          "countries": [
            { "value": "COUNTRY:ASIA:China", "label": "China" },
            { "value": "COUNTRY:ASIA:Japan", "label": "Japan" }
          ]
        }
      ],
      "granularities": [],
      "periods": {
        "yearly": [],
        "monthly": []
      }
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
| `regions` | `List<RegionFilterOption>` | 선택 가능한 Region과 하위 Country 목록. `caseId`가 있으면 segment별 실제 데이터 기준이므로 서로 다를 수 있음 |
| `granularities` | `List<FilterOption>` | 선택 가능한 granularity 목록. TRADE는 빈 배열 |
| `periods` | `FundamentalModelPeriodFilterOptions` | 해당 segment의 Volumes 결과에 실제 존재하는 연/월 기간 옵션. TRADE 또는 `caseId` 미전달 시 빈 배열 |

**`RegionFilterOption`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `value` | `String` | Region API 값. `FundamentalModelRegion` 이름과 동일 (예: `ASIA`) |
| `label` | `String` | Region 화면 표시명 (예: `Asia`) |
| `countries` | `List<FilterOption>` | 해당 Region 아래 선택 가능한 Country. Country의 `value`는 `COUNTRY:{REGION}:{country}` 형식. 실제 데이터 범위가 없으면 빈 배열 |

**`FundamentalModelPeriodFilterOptions`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `yearly` | `List<FilterOption>` | 연도 옵션. `value`는 `yyyy` 형식 |
| `monthly` | `List<FilterOption>` | 월 옵션. `value`는 `yyyy-MM` 형식이고 `label`은 `Jan`, `Feb` 같은 영문 월 축약 표기 |

**`FilterOption`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `value` | `String` | API 파라미터 전송값 (예: `LNG_IMPORTS`) |
| `label` | `String` | 화면 표시명 (예: `LNG Imports`) |

### 4.13 `FundamentalModelPriceRes`

```json
{
  "caseId": 101,
  "unit": "USD/mmbtu",
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
        "seriesType": "USER_ADJUSTED",
        "label": "User adjusted price",
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
| `unit` | `String` | 가격 단위. `"USD/mmbtu"` 고정 상수. 모델 output 불변 계약이므로 DB에 저장하지 않고 서버가 상수로 반환한다 |
| `chart` | `PriceChart` | Monthly Futures Shoulder Chart용 라인 그래프 데이터 |
| `annualTable` | `PriceTable` | 연간 가격 테이블 |
| `monthlyTable` | `PriceTable` | 월별 가격 테이블 |

**`PriceChart`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `series` | `List<PriceChartSeries>` | 차트 시리즈 목록. 항상 `BASE_CASE`, `USER_ADJUSTED` 2개 |

**`PriceChartSeries`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `seriesType` | `PriceSeriesType` | `BASE_CASE` \| `USER_ADJUSTED` |
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
    "types": ["TO_COUNTRY"],
    "regions": ["ASIA"],
    "countryKeys": ["COUNTRY:ASIA:Bangladesh"]
  },
  "fromCountries": [
    { "key": "REGION:AFRICA", "label": "Africa", "kind": "REGION_SUBTOTAL", "region": "AFRICA", "country": null },
    { "key": "COUNTRY:AFRICA:Angola", "label": "Angola", "kind": "COUNTRY", "region": "AFRICA", "country": "Angola" },
    { "key": "REGION:NORTH_AMERICA", "label": "America N", "kind": "REGION_SUBTOTAL", "region": "NORTH_AMERICA", "country": null },
    { "key": "COUNTRY:NORTH_AMERICA:United States", "label": "United States", "kind": "COUNTRY", "region": "NORTH_AMERICA", "country": "United States" },
    { "key": "TOTAL", "label": "Total", "kind": "GRAND_TOTAL", "region": null, "country": "Total" }
  ],
  "toCountries": [
    { "key": "REGION:ASIA", "label": "Asia", "kind": "REGION_SUBTOTAL", "region": "ASIA", "country": null },
    { "key": "COUNTRY:ASIA:Bangladesh", "label": "Bangladesh", "kind": "COUNTRY", "region": "ASIA", "country": "Bangladesh" },
    { "key": "TOTAL", "label": "Total", "kind": "GRAND_TOTAL", "region": null, "country": "Total" }
  ],
  "flows": [
    { "fromKey": "REGION:AFRICA", "toKey": "REGION:ASIA", "fromRegion": "AFRICA", "fromCountry": null, "toRegion": "ASIA", "toCountry": null, "value": 0.2894 },
    { "fromKey": "REGION:AFRICA", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "AFRICA", "fromCountry": null, "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.2894 },
    { "fromKey": "REGION:AFRICA", "toKey": "TOTAL", "fromRegion": "AFRICA", "fromCountry": null, "toRegion": null, "toCountry": "Total", "value": 0.2894 },
    { "fromKey": "COUNTRY:AFRICA:Angola", "toKey": "REGION:ASIA", "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": "ASIA", "toCountry": null, "value": 0.2894 },
    { "fromKey": "COUNTRY:AFRICA:Angola", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.2894 },
    { "fromKey": "COUNTRY:AFRICA:Angola", "toKey": "TOTAL", "fromRegion": "AFRICA", "fromCountry": "Angola", "toRegion": null, "toCountry": "Total", "value": 0.2894 },
    { "fromKey": "REGION:NORTH_AMERICA", "toKey": "REGION:ASIA", "fromRegion": "NORTH_AMERICA", "fromCountry": null, "toRegion": "ASIA", "toCountry": null, "value": 0.4780 },
    { "fromKey": "REGION:NORTH_AMERICA", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "NORTH_AMERICA", "fromCountry": null, "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.4780 },
    { "fromKey": "REGION:NORTH_AMERICA", "toKey": "TOTAL", "fromRegion": "NORTH_AMERICA", "fromCountry": null, "toRegion": null, "toCountry": "Total", "value": 0.4780 },
    { "fromKey": "COUNTRY:NORTH_AMERICA:United States", "toKey": "REGION:ASIA", "fromRegion": "NORTH_AMERICA", "fromCountry": "United States", "toRegion": "ASIA", "toCountry": null, "value": 0.4780 },
    { "fromKey": "COUNTRY:NORTH_AMERICA:United States", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": "NORTH_AMERICA", "fromCountry": "United States", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.4780 },
    { "fromKey": "COUNTRY:NORTH_AMERICA:United States", "toKey": "TOTAL", "fromRegion": "NORTH_AMERICA", "fromCountry": "United States", "toRegion": null, "toCountry": "Total", "value": 0.4780 },
    { "fromKey": "TOTAL", "toKey": "REGION:ASIA", "fromRegion": null, "fromCountry": "Total", "toRegion": "ASIA", "toCountry": null, "value": 0.7674 },
    { "fromKey": "TOTAL", "toKey": "COUNTRY:ASIA:Bangladesh", "fromRegion": null, "fromCountry": "Total", "toRegion": "ASIA", "toCountry": "Bangladesh", "value": 0.7674 },
    { "fromKey": "TOTAL", "toKey": "TOTAL", "fromRegion": null, "fromCountry": "Total", "toRegion": null, "toCountry": "Total", "value": 0.7674 }
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
| `fromCountries` | `List<TradeCountryHeader>` | From country 축 헤더 목록 (컬럼). Country 필터 적용 시 매칭 Country와 그 Country가 속한 저장 Region subtotal을 원래 순서로 유지하며, 마지막 항목은 Total |
| `toCountries` | `List<TradeCountryHeader>` | To country 축 헤더 목록 (행). Country 필터 적용 시 매칭 Country와 그 Country가 속한 저장 Region subtotal을 원래 순서로 유지하며, 마지막 항목은 Total |
| `flows` | `List<TradeFlowItem>` | 0이 아닌 trade flow 값 목록 (sparse). 매트릭스에서 없는 셀은 0으로 처리 |

**`TradeFlowsAppliedFilters`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `types` | `List<TradeFlowDirection>` | Region/Country 필터를 실제 적용한 방향 목록 |
| `regions` | `List<FundamentalModelRegion>` | 실제 적용된 Region 목록. `regions` 없이 `countryKeys`만 전달하면 key에서 추출해 적용한 Region 목록 |
| `countryKeys` | `List<String>` | 실제 적용된 Country key 목록. 미전달 시 빈 배열이며 선택 Region의 전체 Country가 적용됨 |

**`TradeCountryHeader`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `key` | `String` | 행/열 식별자. FE lookup key로 사용한다. 예: `REGION:AFRICA`, `COUNTRY:AFRICA:Angola`, `TOTAL` |
| `label` | `String` | 화면 표시명. 지역 집계는 region label, 개별 국가는 country, 합계는 `Total` |
| `kind` | `TradeCountryHeaderKind` | 헤더 유형. `REGION_SUBTOTAL`, `COUNTRY`, `GRAND_TOTAL` |
| `region` | `FundamentalModelRegion` | 지역. `null`이면 Total 행을 나타낸다 |
| `country` | `String` | 국가명. `null`이면 해당 지역의 집계 행/열이다 |

프론트 렌더링 규칙:
- `kind == "REGION_SUBTOTAL"` → **볼드** 지역 집계 행/열 (예: "Africa")
- `kind == "COUNTRY"` → 개별 국가 행/열 (예: "Angola")
- `kind == "GRAND_TOTAL"` → Total 합계 행/열 (항상 각 축 마지막)
- Country 필터 적용 축에서도 실제 매칭 Country의 `REGION_SUBTOTAL`을 원래 header 순서대로 렌더링한다. 이 subtotal은 해당 Region에서 선택된 Country leaf만 합산한 값이다
- 셀 lookup은 `flows`를 `(fromKey, toKey)` 기준으로 map 구성하고, 누락 셀은 0으로 처리한다
- `GRAND_TOTAL`은 `COUNTRY` flow만 합산한다. `REGION_SUBTOTAL` flow를 다시 더해 중복 집계하지 않는다

**`TradeFlowItem`:**

| 필드 | 타입 | 설명 |
|---|---|---|
| `fromKey` | `String` | From country 헤더 식별자. `fromCountries[].key`를 참조한다 |
| `toKey` | `String` | To country 헤더 식별자. `toCountries[].key`를 참조한다 |
| `fromRegion` | `FundamentalModelRegion` | 수출 지역 |
| `fromCountry` | `String` | 수출 국가. `null`이면 지역 집계 |
| `toRegion` | `FundamentalModelRegion` | 수입 지역. `null`이면 Total 행 |
| `toCountry` | `String` | 수입 국가. `null`이면 지역 집계. `"Total"`이면 Total 합계 행 |
| `value` | `Double` | 물량. JSON number로 반환하며 소수점 다섯째 자리까지 0 방향 절삭하고, 여섯째 자리에서 반올림하지 않는다. Country 필터 합계는 원본 leaf 합산 후 한 번만 절삭한다 |

**신규 enum:**

**`TradeFlowSelector`:** `LNG`, `PNG`

**`TradeFlowUnit`:** `MTPA`, `BCM`

**`TradeFlowDirection`:** `TO_COUNTRY`, `FROM_COUNTRY`

**`TradeCountryHeaderKind`:** `REGION_SUBTOTAL`, `COUNTRY`, `GRAND_TOTAL`

## 5. Representative Error Scenarios

현재 `ResultCode`에는 전용 state conflict enum이 활성화되어 있지 않다. 현재 계약은 기존 enum을 사용하고,
별도 요구가 생기기 전에는 새 오류 코드를 추가하지 않는다.

| 시나리오 | HTTP status | ResultCode 후보 | 처리 |
|---|---|---|---|
| `keyword`가 non-blank인데 `searchType` 없음 | `400` | `REQ_INVALID_PARAMETER` | canonical contract는 `non-blank keyword requires searchType` |
| `searchType`이 있고 `keyword`가 null 또는 blank | `200` | `SUC-000` | 검색 조건 없음으로 정규화해 전체 목록 반환 |
| `searchType`이 허용값 외 문자열 | `400` | `REQ_INVALID_PARAMETER` | enum 변환 실패 |
| `caseId`가 없거나 존재하지 않음 | `404` | `RSC_NOT_FOUND` | 존재하지 않는 case 리소스로 처리 |
| 삭제된 `caseId`로 list 외 active-resource API 호출 | `404` | `RSC_NOT_FOUND` | detail/status/run/update/delete 모두 active resource가 아니므로 not found |
| title 누락 또는 blank | `400` | `REQ_INVALID_PARAMETER` | Bean Validation 또는 수동 검증 |
| title 200자 초과 | `400` | `REQ_INVALID_PARAMETER` | `case_titl_nm VARCHAR(200)` 정합성 |
| 파일 part 누락 | `400` | `REQ_MISSING_PARAMETER` 또는 `REQ_INVALID_PARAMETER` | handler가 missing part를 잡으면 공통 handler는 `REQ_INVALID_PARAMETER` 가능 |
| 파일 개수가 4개가 아님 | `400` | `REQ_INVALID_PARAMETER` | 정확히 4개가 필요함을 안내 |
| 빈 파일 포함 | `400` | `FILE_INVALID` | 각 file `isEmpty()` 검증 |
| 파일 크기 제한 초과 | `400` | `FILE_SIZE_ERROR` | 제한값은 구현 설정 확인 필요 |
| 허용하지 않는 파일 형식 | `400` | `FILE_FORMAT_ERROR` | allowlist 확정 후 적용 |
| Run 요청 대상 Case의 `DEMAND`/`SUPPLY`/`TRADE`/`OUTPUT` input storage path 중 하나 이상 누락 | `400` | `BIZ_RULE_VIOLATION` | Job을 생성하지 않고 요청 실패 |
| 이미 `RUNNING`인 case 실행 요청 | `409` 권장 | `BIZ_RULE_VIOLATION` 1차 후보, 후속 `BIZ_STATE_CONFLICT` 재활성화 검토 | 중복 실행 차단 |
| 이미 `COMPLETED`인 case 실행 요청 | `409` 권장 | `BIZ_RULE_VIOLATION` | 완료 case 재사용 금지. 새 실행은 새 case로 생성 |
| Volumes 조회 시 `segment` 누락 | `400` | `REQ_INVALID_PARAMETER` | Volumes는 segment 필수 |
| Volumes 조회 시 `segment=TRADE` 전달 | `400` | `REQ_INVALID_PARAMETER` | Trade Flows는 `/volumes/trade` 엔드포인트를 사용 |
| Volumes 조회 시 `types`/`regions`/`segment`가 허용값 외 문자열 | `400` | `REQ_INVALID_PARAMETER` | enum 변환 실패 |
| filter-options에서 `selector`/`year` 중 하나만 전달하거나 `caseId` 없이 전달 | `400` | `REQ_INVALID_PARAMETER` | TRADE 데이터 범위는 세 값을 함께 전달해야 함 |
| filter-options 또는 Trade Flows 조회 시 `year`가 2023~2030 범위 밖 | `400` | `REQ_INVALID_PARAMETER` | 허용 연도 초과 |
| Volumes/Trade Flows 조회 시 `countryKeys` 형식 또는 포함 Region이 유효하지 않음 | `400` | `REQ_INVALID_PARAMETER` | `COUNTRY:{REGION}:{country}` 형식 검증 실패 |
| Volumes/Trade Flows 조회 시 형식은 유효하지만 결과에 없는 `countryKeys` 전달 | `200` | `SUC-000` | 정상 빈 결과 반환 |
| Volumes/Price 조회 시 case가 `PENDING` 또는 `RUNNING` 상태 | `409` 권장 | `BIZ_RULE_VIOLATION` | 결과 데이터가 아직 준비되지 않은 상태 |
| Volumes/Price 조회 시 case가 `FAILED` 상태 | `404` | `RSC_NOT_FOUND` | 실행 실패로 결과 없음 |
| Trade Flows 조회 시 `selector` 또는 `year` 누락 | `400` | `REQ_INVALID_PARAMETER` | 두 파라미터 모두 필수 |
| Trade Flows 조회 시 case가 `PENDING` 또는 `RUNNING` 상태 | `409` 권장 | `BIZ_RULE_VIOLATION` | Volumes와 동일 |
| Trade Flows 조회 시 case가 `FAILED` 상태 | `404` | `RSC_NOT_FOUND` | Volumes와 동일 |
| 인증 없음 | `401` | `AUTH_REQUIRED` 또는 `AUTH_TOKEN_INVALID` | 기존 security handler 따름 |
| 권한 없음 | `403` | `AUTH_INSUFFICIENT_PERMISSIONS` 또는 `AUTH_ACCESS_DENIED` | `@RequiredPermissionRules` 결과 따름 |
| persistence 또는 응답 조립 중 내부 오류 | `500` | `SYS_INTERNAL_ERROR` | client에는 내부 상세를 노출하지 않음 |
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

## 6. OpenAPI Contract

Controller tag 권장:

`FundamentalModelCaseController`:

```text
@Tag(
  name = "6. MI Fundamental Model Case API",
  description = "MI Fundamental Model Scenario Case API입니다."
)
```

`FundamentalModelRunDedicatedController`:

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
| `FundamentalModelCaseDedicatedController` | upload | `MI Fundamental Model Scenario Case 업로드 API` |
| `FundamentalModelCaseController` | detail | `MI Fundamental Model Scenario Case 상세 조회 API` |
| `FundamentalModelCaseController` | update | `MI Fundamental Model Scenario Case 수정 API` |
| `FundamentalModelCaseController` | delete | `MI Fundamental Model Scenario Case 삭제 API` |
| `FundamentalModelCaseDedicatedController` | run | `MI Fundamental Model Scenario Case 실행 요청 API` |
| `FundamentalModelCaseController` | status | `MI Fundamental Model Scenario Case 상태 조회 API` |
| `FundamentalModelCaseController` | volumes-filter-options | `MI Fundamental Model Volumes 필터 옵션 조회 API` |
| `FundamentalModelCaseController` | volumes | `MI Fundamental Model Volumes 데이터 조회 API` |
| `FundamentalModelCaseController` | trade-flows | `MI Fundamental Model Trade Flows 데이터 조회 API` |
| `FundamentalModelCaseController` | price | `MI Fundamental Model Price 데이터 조회 API` |
| `FundamentalModelRunDedicatedController` | callback | `MI Fundamental Model Scenario Case 실행 완료 Callback API` |

Swagger 문서에는 다음을 명시한다.

- 목록 조회 API의 canonical contract는 `non-blank keyword requires searchType`이다. `keyword`가 null 또는 blank이면 `searchType`이 있어도 검색 조건 없음으로 처리한다.
- 업로드 API는 `multipart/form-data` 전용이다.
- `files` part는 정확히 4개가 필수이며 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할이 각각 1개여야 한다.
- 상세 조회는 case metadata, input/output/intermediate/tradeOutput 다운로드 metadata, latestRun을 반환한다. 네 파일 목록 모두
  `fileNo`, `fileNm`, `fileSize`를 제공하며 full run history/result payload는 포함하지 않는다.
- 수정 API는 `title`, `description`만 받으며 응답은 최신 `FundamentalModelCaseDetailRes`다.
- 삭제 API는 physical delete가 아니라 soft delete이며, 삭제된 case는 이후 active-resource API에서 `404 RSC_NOT_FOUND`로 취급한다.
- 실행 요청은 `PENDING` 최초 실행과 `FAILED` 모델 호출 재시도만 허용한다. `RUNNING` 중복 접수와 `COMPLETED` case 재실행은 차단한다.
- 상태 조회는 case 기준 polling용 경량 projection이며 `PENDING` case는 `latestRun = null`이다. 상세 조회와 달리 file metadata를 반환하지 않는다.
- Volumes filter-options는 `caseId`가 있으면 최신 완료 run의 실제 Region/Country/기간만 반환한다. TRADE는 `selector`와 `year`를 함께 전달한 현재 데이터 범위에서 From/To Region/Country 합집합을 반환하며, `regions[].countries[]` 중첩 구조를 사용한다.
- Volumes 조회는 `COMPLETED` case에만 가능하다. `segment`(`GLOBAL`|`LNG`) 필수, `TRADE` 전달 시 400. `types`/`regions`/`countryKeys` 미전달 시 전체 반환, `granularity` 기본값 `MONTHLY`. Country 선택 시 Region/Type subtotal을 선택 결과로 재계산한다.
- Trade Flows 조회는 `COMPLETED` case에만 가능하다. `selector`/`year` 필수, `unit` 기본값 `MTPA`. `types`가 지정한 방향에 동일한 `regions`/`countryKeys`를 적용한다. Country 선택 시 실제 매칭 Country가 속한 저장 Region subtotal header를 원래 순서로 유지하고 subtotal/total은 선택 Country leaf만으로 재계산한다.
- filter-options의 `selector`/`year`에는 함께 전달해야 하는 조건과 연도 범위를 `@Parameter`로 명시한다. Volumes/Trade의 `countryKeys`에는 `COUNTRY:ASIA:Japan` 예시, exact-match 의미, `regions`와의 AND 조건, `regions` 미전달 시 key에서 Region을 추출하는 동작을 명시한다.
- `RegionFilterOption.countries`와 두 결과 DTO의 `appliedFilters.countryKeys`에는 빈 배열의 의미를 `@Schema`로 명시한다.
- Price 조회는 `COMPLETED` case에만 가능하다. 쿼리 파라미터 없음. 차트 + Annual 테이블 + Monthly 테이블을 단일 응답으로 반환한다.
- callback API는 외부 모델 전용이며 FE가 직접 호출하지 않는다.

## 7. Data / Model Impact

Region/Country 필터 확장은 기존 Volumes result row와 Trade header/flow를 조회해 응답을 조립하므로 schema/DDL 변경이 없다. Country의 별도 master code를 추가하지 않고 기존 결과의 `region`과 `country`로 `COUNTRY:{REGION}:{country}` key를 만든다. 기존 repository 조회와 application-level 필터/합계 계산을 확장하는 범위이며 별도 ERD review는 필요하지 않다.

### 7.1 1st Mock Implementation (superseded by real DB implementation, 2026-06-05 ~ 2026-06-19)

아래 내용은 real-DB 전환 전 1차 mock의 historical reference다. 현재 source tree에는 case mock
service/properties/delegator/reset endpoint와 아래 seed resource가 없으며, 클래스명·경로·로딩 규칙을
현재 구현 입력으로 사용하지 않는다.

1차 mock 구현은 DB schema 변경 없이 `src/main/resources/mock/fundamental-model/fundamental-model-cases.json`
seed 파일을 읽어 in-memory store로 동작했다.

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

`FundamentalModelCaseMockRecord.assigneeName`은 case 업로드 시점의 사용자 이름을 표현한다. mock에서는 업로드 요청에 업로더 이름을 받지 않으므로 고정값(예: `"홍길동"`)으로 설정한다. 향후 DB 전환 시 `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no`로 JOIN하고, 작성자명 표시는 `tmd_usrm.usr_nm`을 사용한다.

`FundamentalModelFileMockRecord` 는 별도 store map 없이 `FundamentalModelCaseMockRecord.files` 에 `List` 로 직접 포함된다. mock seed의 `files`는 입력 파일을 의미하며, 상세 응답에서는 `files.input`으로 변환한다. output 파일은 DB-backed 구현에서 `SCNRCASEH + run_seq`로, 중간파일은 `SCNRCASEHI + run_seq`로, Trade Flow 결과파일은 `SCNRCASEHTF + run_seq`로 조회한다. 현재 mock seed는 별도 output/intermediate/tradeOutput file store를 갖지 않으므로 세 배열은 빈 배열로 반환한다. 향후 DB 전환 시에는 이 리스트가 `tcm_filel` 별도 테이블로 분리된다 (§7.2 참고). `FundamentalModelFileMockRecord` 를 응답 DTO와 별개 타입으로 두는 이유는 mock 내부 표현이 presentation layer에 직접 결합되지 않도록 하기 위함이다.

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
- DB 전환 시 country 목록의 동적 변동 특성은 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)의 `tmd_mdl_scnrcasetrdehdrl` header row table 설계를 따른다.

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
        "seriesType": "USER_ADJUSTED",
        "label": "User adjusted price",
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
- `seriesType`은 `BASE_CASE` 또는 `USER_ADJUSTED` 고정이다.
- `annualTable.columns`의 `month`는 `null`, `periodLabel`은 `null`이다.
- mock 서비스는 이 파일을 classpath resource로 읽어 `FundamentalModelPriceRes`로 변환해 반환한다.

**mock 서비스 로딩 규칙:**

- 세 seed 파일은 모두 애플리케이션 시작 시(`@PostConstruct`) 로딩한다.
- `fundamental-model-volumes-data.json`이 없으면 Volumes API는 빈 응답(`columns: [], rows: []`)을 반환한다.
- `fundamental-model-trade-flows-data.json`이 없으면 Trade Flows API는 빈 응답(`fromCountries: [], toCountries: [], flows: []`)을 반환한다.
- `fundamental-model-price-data.json`이 없으면 Price API는 빈 응답(`chart.series: [], annualTable.rows: [], monthlyTable.rows: []`)을 반환한다.
- reset API는 Volumes/Trade Flows/Price seed를 재로딩하지 않는다. case/run seed(`fundamental-model-cases.json`)만 재로딩한다.

### 7.2 Current real-DB Mapping

| API 필드 | 현재 DB mapping | 메모 |
|---|---|---|
| `caseId` | `tmd_mdl_scnrcasem.case_id` | `INT UNSIGNED` 초안. Java DTO는 `Long` 사용 가능하나 mapper 변환 주의 |
| `scenarioId` | API 미노출 | Fundamental Model 독립실행 case는 `scnr_id = NULL`; Scenario 실행과 같은 table을 공유하지만 API에 `scenarioId`를 노출하지 않는다 |
| `title` | `tmd_mdl_scnrcasem.case_titl_nm` | 최대 200자 |
| `description` | `tmd_mdl_scnrcasem.case_expn` | nullable |
| `delYn` | `tmd_mdl_scnrcasem.del_yn` | soft delete flag. active resource 조회 조건은 `del_yn = 'N'` |
| `status` | `tmd_mdl_scnrcasem.case_stat_cd` | 최신 상태 cache 역할 |
| `assigneeUserNo` | `tmd_mdl_scnrcasem.usr_no` | 상세 응답에서 업로더 사용자 번호로 반환 |
| `assigneeName` | `tmd_usrm.usr_nm` | `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no` JOIN 표시값 |
| `latestRunId` | `tmd_mdl_scnrcasem.last_run_id` | `VARCHAR(20) NULL`. `tmd_mdl_scnrcaseh.run_id` 에 대한 application-validated 논리 참조이며 물리 FK 없음. `case_stat_cd` 와 invariant 동기화 |
| `runId` | `tmd_mdl_scnrcaseh.run_id` | `VARCHAR(20)` + unique. backend-generated `yyyymmdd_hhmmss_XXXX` |
| run status | `tmd_mdl_scnrcaseh.exec_stat_cd` | execution 관점 상태 |
| `latestRun.startedAt` | `tmd_mdl_scnrcaseh.exec_bgn_dtm` | FE 소요시간 계산 시작 시각 |
| `latestRun.finishedAt` | `tmd_mdl_scnrcaseh.exec_end_dtm` | FE 소요시간 계산 종료 시각. `RUNNING`이면 null |
| `failureReason` | `tmd_mdl_scnrcaseh.fail_rsn_cd` | `TIMEOUT`, `EXTERNAL_ERROR`, `UNKNOWN` |
| `errorMessage` | `tmd_mdl_scnrcaseh.err_msg_cntt` | 최대 길이 후속 확정 |
| upload files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNRCASE'`, `file_rel_obj_dstg_no = case_id` |
| output files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNRCASEH'`, `file_rel_obj_dstg_no = run_seq` |
| intermediate files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNRCASEHI'`, `file_rel_obj_dstg_no = run_seq` |
| Trade Flow output files | `tcm_filel` | `file_rel_obj_spt_cd = 'SCNRCASEHTF'`, `file_rel_obj_dstg_no = run_seq` |

`SCNRCASE`/`SCNRCASEH`는 기존 등록 코드다. `SCNRCASEHI`는 2026-09-07에 확정됐고, `SCNRCASEHTF`는 2026-09-17 사용자 확인 기준 모든 대상 DB에 등록됐으며 `cd_rnk = 8` 충돌이 없다. `tcm_filel.file_rel_obj_dstg_no`와 `tmd_mdl_scnrcasem.case_id`/`tmd_mdl_scnrcaseh.run_seq`의 타입 호환성은 기존 DDL review 결과를 따른다.

Trade Flows의 fromCountries/toCountries country 목록은 base 버전/기준 정보에 따라 변동될 수 있다. region 고유값은 고정이지만 region 내 country 목록이 달라지는 구조이므로, header metadata는 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)의 `tmd_mdl_scnrcasetrdehdrl` row table에 저장한다.

callback payload에는 결과 payload/pointer를 포함하지 않는다. parsed result persistence는 output result design의
별도 table을 사용하고, output/intermediate/tradeOutput file reference는 상세 응답의 다운로드 metadata 계약을 따른다.

## 8. Current Data Access Approach

현재 구현은 `FundamentalModelCaseServiceImpl` 단일 real service가 담당하며 mock delegator를 사용하지 않는다.

- case/run command와 상태 전이는 `ScenarioCaseRepository`, `ScenarioCaseRunHistoryRepository`를 사용한다.
- 현재 구현된 input/output/intermediate/tradeOutput 파일 metadata는 `UploadedFileRepository`와 기존 `FileService`를 사용하며, 역할별 관계 코드로 구분한다.
- Volumes/Trade Flows/Price는 각 result repository projection으로 latest completed run의 row를 읽고 application layer에서 응답을 조립한다.
- upload/run/callback은 transaction boundary 안에서 상태 불변식을 지키며, callback은 running-to-terminal compare-and-set과 latest run 조건으로 late overwrite를 막는다.
- 목록·상세·결과 조회는 active resource와 endpoint별 ownership/shared-read 규칙을 적용한다.

## 9. Implementation Touch Points

현재 구현 touch point:

| 구분 | 현재 구현 |
|---|---|
| Case Controller | `FundamentalModelCaseController` — list/detail/update/delete/status/Volumes/Trade Flows/Price |
| Dedicated Case Controller | `FundamentalModelCaseDedicatedController` — upload/run과 운영용 Kubernetes Job 권한 검증 endpoint |
| Dedicated Run Controller | `FundamentalModelRunDedicatedController` — callback endpoint 전용 |
| Service interface | `FundamentalModelCaseService` |
| Real service | `FundamentalModelCaseServiceImpl` |
| Persistence | `ScenarioCaseRepository`, `ScenarioCaseRunHistoryRepository`, `UploadedFileRepository`, Volumes/Trade/Price result repositories |
| Request DTO | `FundamentalModelCaseListReq`, `FundamentalModelCaseUpdateReq`, `FundamentalModelVolumesReq`, `FundamentalModelTradeFlowsReq` |
| Callback DTO | `FundamentalModelRunCallbackReq` |
| Response DTO | `FundamentalModelCaseListRes`, `FundamentalModelCaseListItemRes`, `FundamentalModelCaseDetailRes`, `FundamentalModelCaseFilesRes`, `FundamentalModelDownloadFileRes`, `FundamentalModelCaseCreateRes`, `FundamentalModelCaseFileRes`, `FundamentalModelRunAcceptRes`, `FundamentalModelCaseStatusRes`, `FundamentalModelRunStatusRes`, `FundamentalModelRunCallbackRes`, `FundamentalModelVolumesRes`, `FundamentalModelTradeFlowsRes`, `FundamentalModelPriceRes` |
| Enum | `FundamentalModelCaseStatus`, `FundamentalModelRunStatus`, `FundamentalModelCaseSearchType`, `FundamentalModelSegment`, `FundamentalModelDataType`, `FundamentalModelRegion`, `FundamentalModelGranularity`, `VolumesRowKind`, `PriceSeriesType`, `TradeFlowSelector`, `TradeFlowUnit`, `TradeFlowDirection`, `TradeCountryHeaderKind` |

package는 현재 `com.sk.domain.mi.*.fundamentalmodel` 경계를 사용한다.

service method signature는 `scenarioId`를 받지 않고 `caseId` 또는 `runId`로 식별한다.
`listCases(request)`, `createCase(title, description, files)`, `getCaseDetail(caseId)`,
`updateCase(caseId, request)`, `deleteCase(caseId)`, `runCase(caseId)`, `getCaseStatus(caseId)`,
`getVolumesFilterOptions(caseId)`, `getVolumes(caseId, request)`, `getTradeFlows(caseId, request)`,
`getPrice(caseId)`, `handleCallback(runId, request)`가 현재 interface다.

권장 permission:

```text
@RequiredPermissionRules(rules = {
  @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
  @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
})
```

사용자 화면용 case API는 위 permission rule을 적용한다. 외부 모델 callback은 사용자 화면에서 호출하지 않으며,
운영 인증/서명 경계는 별도 보안·integration 계약에서 확정한다.

callback API 권한 권장:

```text
@RequiredPermissionRules(rules = {
  @PermissionRule(role = Role.SYSTEM),
  @PermissionRule(role = Role.API_SUB_ADMIN),
  @PermissionRule(role = Role.API_PLUS_OPERATOR)
})
```

사용자 화면에서 호출하는 API가 아니므로 `Role.USER` 와 일반 `Role.ADMIN` user-scope 조건은 넣지 않는 쪽을 권장한다. 실제 model pod가 JWT 기반으로 호출하지 못한다면 별도 callback 인증 filter, HMAC signature, mTLS, private network allowlist 중 하나를 보안 설계에서 확정한다.

callback endpoint는 현재 별도 permission annotation 없이 dedicated deployment에 노출된다. 인증/신뢰 경계의
추가 변경은 이 문서에서 가정하지 않고 별도 security-sensitive 계약과 검증을 거친다.

## 10. Observable Behavior Rules

### List Cases

> current MI Base를 포함하는 최신 목록 동작은 enhancement feature spec §3이 canonical source다.

1. `del_yn = 'N'`인 active Fundamental Model case를 대상으로 `createdAt` 내림차순 정렬한다.
2. `keyword`가 null 또는 blank(trim 후 빈 문자열)이면 검색 조건 없음으로 처리한다. 이 경우 `searchType`이 함께 전달되어도 필터를 적용하지 않는다.
3. `keyword`가 non-blank인 경우에만 `searchType`을 필수로 요구하고, 둘 다 유효하면 필터를 적용한다.
   - `TITLE`: case 제목에서 대소문자 구분 없이 부분 일치 필터
   - `ASSIGNEE_NAME`: `assigneeName`에서 대소문자 구분 없이 부분 일치 필터
4. `keyword`가 non-blank인데 `searchType`이 없으면 `RSC_NOT_FOUND` 대신 `REQ_INVALID_PARAMETER`로 거절한다.
5. 결과 목록과 `totalCount`를 `FundamentalModelCaseListRes`로 반환한다.
6. 조건에 맞는 case가 없어도 빈 리스트(`totalCount=0`, `cases=[]`)로 정상 응답한다.

### Upload

1. `title`, `description`, `files`를 받는다.
2. `files`가 정확히 4개이고 파일명에 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할이 각각 정확히 1개인지 검증한다.
3. 각 파일이 비어 있지 않은지 검증한다.
4. case를 `tmd_mdl_scnrcasem`에 저장하고 DB가 `caseId`를 발급한다.
5. 파일 binary는 기존 storage 흐름으로 저장하고 metadata는 `tcm_filel`에 `SCNRCASE + case_id`로 연결한다.
6. 파일 하나라도 업로드에 실패하면 먼저 성공한 파일을 보상 삭제하고 요청 전체를 실패시킨다.
7. case 상태는 `PENDING`으로 저장한다.
8. `FundamentalModelCaseCreateRes`를 반환한다.

### Detail Query

1. `caseId`로 active case를 찾는다.
2. case가 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
3. case metadata와 `SCNRCASE + case_id` 기준 input 파일의 `fileNo`, `fileNm`, `fileSize` metadata를 반환한다.
4. `latestRunId`가 null이면 `latestRun = null`, `files.output = []`, `files.intermediate = []`, `files.tradeOutput = []`로 반환한다.
5. `latestRunId`가 있으면 `tmd_mdl_scnrcaseh`에서 최신 run 1건을 찾아 `latestRun`으로 반환하고, 해당 run의 같은 `run_seq` 기준 `SCNRCASEH` output 파일, `SCNRCASEHI` 중간파일, `SCNRCASEHTF` Trade Flow 결과파일 metadata를 각각 반환한다.
6. full run history와 result payload는 반환하지 않는다.

### Update

1. `caseId`로 active `INDEPENDENT` case를 찾는다.
2. case가 없거나 `delYn = Y` 또는 `MI_BASE | SCENARIO`이면 `RSC_NOT_FOUND`.
3. `title`을 trim 후 1-200자 필수로 검증한다.
4. `description`은 nullable/optional로 받아 case row에 반영한다.
5. `status`, `files`, `latestRunId`, run history는 변경하지 않는다.
6. `updatedAt`을 갱신한다.
7. 최신 `FundamentalModelCaseDetailRes`를 반환한다.

### Delete

1. `caseId`로 active `INDEPENDENT` case를 찾는다.
2. case가 없거나 `delYn = Y` 또는 `MI_BASE | SCENARIO`이면 `RSC_NOT_FOUND`.
3. case row의 `del_yn`을 `Y`로 바꾸고 수정 audit 정보를 갱신한다.
4. upload files, run history, latestRun/result payload는 물리 삭제하지 않는다.
5. 이후 list/detail/status/run/update/delete에서 해당 case는 active resource가 아니므로 `RSC_NOT_FOUND`로 취급한다.
6. 응답은 `BaseResponse<Void>` 200 OK다.

### Run Request

1. `caseId`로 active `INDEPENDENT` case를 찾는다.
2. case가 없거나 `delYn = Y` 또는 `MI_BASE | SCENARIO`이면 `RSC_NOT_FOUND`.
3. case owner가 현재 사용자와 다르면 `RSC_NOT_FOUND`.
4. case status가 `RUNNING`이면 `409 + BIZ_RULE_VIOLATION` 후보로 거절한다.
5. 실행 가능 상태는 최초 실행의 `PENDING`과 모델 호출 실패 재시도의 `FAILED`다. `COMPLETED`는 재실행할 수 없으며 새로운 정상 실행은 새 case를 생성해야 한다.
6. `runId`는 `yyyymmdd_hhmmss_XXXX` 포맷으로 생성한다. `yyyymmdd_hhmmss`는 실행 요청 시각 기준이고, `XXXX`는 같은 초 안의 충돌을 방지하는 4자리 suffix다.
7. `tmd_mdl_scnrcaseh` run row를 `RUNNING`으로 생성한다.
8. case 최신 상태를 `RUNNING`으로 변경하고 `latestRunId`를 갱신한다.
9. `FundamentalModelRunAcceptRes`를 `202 Accepted`로 반환한다.

### Status Query

1. `caseId`로 active `INDEPENDENT | MI_BASE` case를 찾는다. `SCENARIO` 또는 soft-deleted case는 `RSC_NOT_FOUND`로 처리한다.
2. case status와 latest run 정보를 반환한다.
3. `PENDING` case는 아직 실행 이력이 없으므로 `latestRun = null`이다.
4. status query는 polling용 경량 projection이므로 `description`, `assigneeName`, `files`를 반환하지 않는다.
5. terminal 전이는 callback 또는 timeout/reconciliation 경로에서 처리한다.

### Removed Mock Reset

- real-DB 전환 후 reset endpoint와 동작은 존재하지 않는다.
- 테스트 데이터 초기화는 API 계약이 아니라 테스트 fixture 또는 승인된 운영 절차로 처리한다.

### External Callback Handling

1. path `runId`로 `tmd_mdl_scnrcaseh` run row를 찾는다.
2. run이 없으면 `RSC_NOT_FOUND`.
3. run이 `RUNNING`이면 terminal 상태로 전이한다.
4. callback의 `finishedAt`은 offset 없는 UTC `LocalDateTime`으로 해석하고 KST(`Asia/Seoul`) `LocalDateTime`으로 변환한다.
5. `status = COMPLETED`이면 run 상태와 case 최신 상태를 `COMPLETED`로 변경하고 KST로 변환한 `finishedAt`을 `exec_end_dtm`에 반영한다.
6. `status = FAILED`이면 run 상태와 case 최신 상태를 `FAILED`로 변경하고 `failureReason`, `errorMessage`, KST로 변환한 `finishedAt`을 반영한다.
7. 이미 terminal 상태인 run에 동일 terminal payload가 재전송되면 상태를 바꾸지 않고 `200 OK` no-op으로 응답한다. 이때 `FundamentalModelRunCallbackRes.idempotentReplay = true` 로 설정한다.
8. 이미 terminal 상태인 run에 상이 payload가 들어오면 상태를 바꾸지 않는다. 동일 retry로 볼 수 없으면 `409 + BIZ_RULE_VIOLATION` 후보로 처리한다.
   - 비교 키는 `status`, `failureReason`, `errorMessage` 세 필드만 사용한다. 세 필드가 모두 같으면 "동일 payload"로 간주한다.
   - `finishedAt` 은 외부 모델 retry마다 다르게 들어올 수 있으므로 비교 대상에서 제외한다.
   - `runId` 는 path 식별자이므로 payload 비교 키에 포함하지 않는다.
9. timeout 등으로 이미 `FAILED` 처리된 run에 늦은 `COMPLETED` callback이 오면 상태를 되돌리지 않는다. 외부 모델 재시도 폭주를 막기 위해 `200 OK` no-op과 late callback 로그를 권장한다.
10. callback 처리 안에서는 결과 payload materialization이나 파일 다운로드 처리를 수행하지 않는다.

### Volumes Filter Options Query

1. `caseId`가 없으면 정적 type/Region/granularity를 반환한다. 각 Region의 `countries`와 모든 `periods`는 빈 배열이다.
2. `caseId`가 있으면 active `COMPLETED` case의 최신 완료 run을 조회한다.
3. GLOBAL/LNG는 해당 segment의 MONTHLY/YEARLY result row 합집합으로 Region/Country를 만들고, 각 granularity의 `periodKeys`로 기간 옵션을 만든다.
4. TRADE는 `selector`와 `year`가 함께 있을 때 해당 header의 From/To Region/Country 합집합을 만든다. 두 값이 없으면 정적 Region과 빈 Country 목록을 반환한다.
5. `selector`/`year`의 일부만 있거나 `caseId` 없이 둘을 전달하면 `400 REQ_INVALID_PARAMETER`로 응답한다.
6. Region은 enum 순서, Country는 label 오름차순으로 정렬하며 중복 key는 제거한다.

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
6. `regions`가 미전달이고 `countryKeys`도 없으면 전체 `FundamentalModelRegion` 목록을 필터로 사용한다. `countryKeys`만 있으면 key에서 Region을 추출한다.
7. `regions`와 `countryKeys`가 함께 있으면 Region AND Country 조건을 적용한다. Country key 형식은 `COUNTRY:{REGION}:{country}`이며 exact match한다.
8. `granularity`가 미전달이면 `MONTHLY`를 기본값으로 사용한다.
9. `periods`가 전달되면 선택 기간만 `columns`와 각 row `values`에 포함한다. `MONTHLY`는 `yyyy-MM`, `YEARLY`는 `yyyy` 형식이다.
10. `granularity=MONTHLY`이면 `columns`의 `label`은 `"26-01"` 형식이고, `year`/`month`는 각각 4자리 연도와 1~12 정수이다.
11. `granularity=YEARLY`이면 `columns`의 `label`은 연도 문자열이고, `year`는 4자리 연도, `month`는 `null`이다.
12. 응답의 `unit`은 현재 `MTPA` 고정이다. 후속 unit 변환 기능은 별도 request contract로 정의한다.
13. `rows`는 필터를 적용한 뒤 FE가 응답 순서 그대로 렌더링할 수 있게 정렬한다. 선택된 `types` 순서대로 Type section을 만들고, 각 Type section 안에서는 선택된 `regions` 순서대로 `REGION_SUBTOTAL` 행을 먼저 반환한 뒤 해당 Region의 선택된 `COUNTRY` 행들을 반환한다. 각 Type section의 마지막 행은 `TYPE_TOTAL`이다.
14. `typeLabel`은 `type` enum label을 반환한다. `rowLabel`은 `REGION_SUBTOTAL`이면 region enum label, `COUNTRY`이면 country, `TYPE_TOTAL`이면 `Total`을 반환한다.
15. `countryKeys`가 있으면 `REGION_SUBTOTAL`과 `TYPE_TOTAL`을 선택된 Country 값만으로 다시 계산한다. 없으면 기존 Region subtotal 기반 계산을 유지한다.
16. 응답의 `appliedFilters`에는 실제 적용된 `types`, `regions`, `countryKeys`, 요청으로 선택한 `periods` 목록을 반환한다.
17. latest completed run의 Volumes result row를 조회하고, 표시 label과 필터 결과는 서버가 응답 조립 시 파생한다.

### Trade Flows Data Query

1. `caseId`로 active case를 찾는다. 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
2. case status 조건은 §Volumes Data Query와 동일하게 처리한다.
3. `selector`, `year` 파라미터가 없으면 `400 REQ_INVALID_PARAMETER`.
4. `year`가 2023~2030 범위 밖이면 `400 REQ_INVALID_PARAMETER`.
5. `unit` 미전달 시 `MTPA`를 기본값으로 사용한다.
6. `types` 미전달 시 전체 `TradeFlowDirection` 목록(`TO_COUNTRY`, `FROM_COUNTRY`)을 필터로 사용한다.
7. `regions` 미전달이고 `countryKeys`도 없으면 전체 `FundamentalModelRegion` 목록을 필터로 사용한다. `countryKeys`만 있으면 key에서 Region을 추출해 필터와 응답 `appliedFilters.regions`에 적용한다.
8. `regions`와 `countryKeys`가 함께 있으면 Region AND Country 조건을 적용한다. Country key 형식과 validation은 Volumes Data Query와 같다.
9. `types`에 `FROM_COUNTRY`가 포함되면 From 축, `TO_COUNTRY`가 포함되면 To 축에 동일한 Region/Country 목록을 적용한다. 포함되지 않은 방향은 전체를 유지한다.
10. Country 필터가 적용된 방향은 실제 매칭 Country와 그 Country가 속한 저장 `REGION_SUBTOTAL` header를 원래 순서로 유지한다. Region만 필터링한 방향은 기존 Region subtotal과 Country header를 함께 유지한다.
11. 각 축 마지막의 `TOTAL` header와 관련 flow는 유지한다. Country 필터가 한 방향이라도 적용되면 필터 후 남은 Country-to-Country leaf flow만으로 응답의 Region subtotal과 Total을 다시 계산하며 저장된 `REGION_SUBTOTAL`/`GRAND_TOTAL` flow를 재사용하거나 중복 포함하지 않는다.
12. 유효한 `countryKeys`가 필터 적용 축의 실제 header에 하나도 매칭되지 않으면 `200 OK` 빈 결과를 반환한다.
13. flows는 fromCountries/toCountries 범위 내의 non-zero 값만 포함한다.
14. 응답의 `appliedFilters`에는 실제 적용된 `types`, `regions`, `countryKeys` 목록을 반환한다.
15. latest completed run의 Trade header/flow row를 조회하고 `selector`/`year` 및 선택 필터를 적용한다.

### Price Data Query

1. `caseId`로 active case를 찾는다. 없거나 `delYn = Y`이면 `RSC_NOT_FOUND`.
2. case status 조건은 §Volumes Data Query와 동일하게 처리한다.
3. 쿼리 파라미터 없음. 요청 즉시 차트 + Annual 테이블 + Monthly 테이블을 구성해 단일 응답으로 반환한다.
4. chart는 monthly/yearly granularity별로 `BASE_CASE`, `USER_ADJUSTED` 시리즈를 구성한다. 데이터가 없으면 빈 series set 또는 `points: []`로 반환한다.
5. `annualTable`과 `monthlyTable`의 행 `label`은 데이터 기반 동적 값으로, 서버가 고정 enum으로 강제하지 않는다.
6. `annualTable.columns`의 `month`는 `null`이다.
7. `monthlyTable.columns`의 `month`는 1~12 정수다.
8. Price 조회는 `caseId -> active case -> last_run_id -> run_seq`로 latest result를 해석한다. Price result read는 다른 사용자가 실행한 결과도 로그인 사용자가 조회할 수 있는 shared read model이므로 `usr_no` owner predicate를 적용하지 않는다. endpoint 권한은 controller permission rule로 처리하고, `usr_no` ownership predicate는 update/delete처럼 소유자 변경 작업에 적용한다.
9. result row를 DB에서 조회한 뒤 응답을 조립한다. chart point는 조회 결과 중 monthly는 서버 현재월 - 6개월, yearly는 서버 현재연도 - 1년부터 포함한다. 이 기간 필터는 chart에만 적용하고 `annualTable`/`monthlyTable` row는 조회된 기간 전체를 유지한다.

## 11. Risks / Open Questions

- Region/Country 필터 확장 계약의 open question은 없다. `countryKeys` 식별자, AND 조합, `regions` 미전달 시 Region 추출, Trade 방향 적용, 매칭 Region subtotal header 유지, 현재 `selector/year` 합집합, 합계 재계산 규칙이 확정되었다.
- 외부 모델 완료 동기화는 callback 방식으로 구현되어 있다. 남은 항목은 운영 인증 방식과 header/signature 규격이다. Job dispatch 재시도는 no-retry로 확정됐다.
- `run_id` 생성 주체는 backend로 확정한다. 생성 포맷은 `yyyymmdd_hhmmss_XXXX` 이다. Kubernetes Job 연동 계약에서는 backend가 Job/pod에 전달한 `run_id` 를 완료/실패 callback path에 그대로 사용하는지만 확인한다.
- 결과 payload는 model pod가 object storage와 parsed result table에 적재하고 backend가 조회한다. output/intermediate/tradeOutput 파일은 상세 응답의 metadata를 기존 공통 File API에 전달해 다운로드한다.
- Volumes/Trade Flows/Price 결과 데이터는 [mi-fundamental-model-output-result-persistence-design.md](../design-docs/gas/mi-fundamental-model-output-result-persistence-design.md)의 table/key/index 계약을 따른다.
- Volumes 필터 Save 토글은 현재 제외한다. 후속 구현 시 user preference API/schema 설계가 필요하다.
- 삭제는 case active-resource 제외를 위한 soft delete다. 보존 기간, audit 조회, 복구 정책은 후속 정책 범위다.
- timeout scheduler는 구현됐고 10분 임계값/30초 polling을 사용한다. 운영 활성화 조건은 workflow design의 feature flag와 DBA index 검증 상태를 따른다.
- 운영 입력 파일명에는 `DEMAND`/`SUPPLY`/`TRADE`/`OUTPUT` 키워드가 각각 포함되어야 하며, backend는 `SCNRCASE + caseId` 파일 목록에서 역할을 매핑한다. 모델 이미지는 네 번째 입력을 `--output-excel-path`로 받아야 한다.
- `caseId` 접근 정책은 mutation의 `usr_no` ownership과 shared result read를 구분한다.
- update/delete predicate는 `case_id + usr_no ownership + del_yn = 'N'`을 포함한다.
- `scenarioId` 비노출과 `scnr_id` 저장 의미는 §7.2 Current real-DB Mapping을 따른다.

## 12. Exit Criteria / Handoff

### Contract Stability

기존 API 계약과 Region/Country 필터 확장 계약은 구현에 충분히 안정적이다.

Region/Country 필터 확장 implementation readiness: `ready`.

- 포함 endpoint: `GET /cases/volumes/filter-options`, `GET /cases/{caseId}/volumes`, `GET /cases/{caseId}/volumes/trade`
- 제외 endpoint: Price, Case CRUD/run/callback, 필터 Save
- 신규/변경 DTO: `RegionFilterOption`, `VolumesSegmentFilterOptions.regions`, Volumes/Trade request의 `countryKeys`, 두 응답의 `appliedFilters.countryKeys`. Trade response shape은 유지하되 Country 필터 적용 축의 `REGION_SUBTOTAL` 포함 규칙을 명시한다.
- query strategy: 기존 최신 완료 run의 Volumes bucket 및 Trade header/flow 조회를 재사용하고 application layer에서 옵션 합집합·필터·합계를 계산한다.
- schema/ERD: 변경 없음, 추가 review 불필요
- OpenAPI readiness: §3, §4, §5, §6의 parameter/schema/error/example 계획이 구현 입력으로 충분하다. 구현 후 generated `/v3/api-docs` focused test로 동기화를 검증한다.
- 권장 검증: 관련 service focused test → generated OpenAPI focused test → `./gradlew compileJava -x test`

현재 real-DB 구현 baseline:

- case CRUD/run/callback의 real DB 구현 (`tmd_mdl_scnrcasem`/`tmd_mdl_scnrcaseh`, 2026-06-05~06-08)
- Kubernetes Job 실제 dispatch 연동 및 재시도 정책 확정(no-retry, 2026-06-12/06-21)
- Volumes/Trade Flows/Price DB persistence implementation (canonical table/key/partition/index design은 `mi-fundamental-model-output-result-persistence-design.md`에 확정, 2026-06-15 rollout 완료)
- 목록/상세/status와 owner-scoped update/delete/run, active-resource hiding
- `SCNRCASE` input과 `SCNRCASEH` output/`SCNRCASEHI` intermediate/`SCNRCASEHTF` Trade Flow 결과파일 다운로드 metadata 조회
- callback과 timeout/reconciliation의 terminal 상태 전이
- `BaseResponse<T>` 응답 wrapper

다음 구현 slice:

- Trade Country 필터 적용 축에서 실제 매칭 Country가 속한 저장 `REGION_SUBTOTAL` header를 원래 순서로 유지하고, 해당 subtotal/total을 선택 Country leaf만으로 재계산하도록 기존 Region-Country 필터 구현을 동기화한다.

아직 안정적이지 않은 항목:

- callback 운영 인증/서명 세부 정책 (재시도 정책은 no-retry로 확정됨, 2026-06-21)
- timeout scheduler 운영 활성화 (구현은 2026-06-22 완료 — 10분 임계값/30초 polling, `SCENARIO`/`INDEPENDENT` 공통 적용. DBA index 검증 전까지 feature flag 기본 비활성. 상세는 `mi-scenario-model-execution-workflow-design.md` Open Questions 참고)
- Volumes 필터 Save 기능 (user preference 저장)

### Input for `erd-change-review`

- `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, `tcm_filel` 매핑 초안의 타입/인덱스/제약 검증
- `tcm_filel.file_rel_obj_dstg_no`와 `case_id` 타입 호환성 검증
- `SCNRCASE`/`SCNRCASEH`는 기존 등록 코드다. `SCNRCASEHI` 등록 상태는 canonical DDL draft를 따르고, `SCNRCASEHTF`는 모든 대상 DB에 적용됐으며 `cd_rnk = 8` 중복이 없다는 사용자 확인을 implementation input으로 둔다.
- `case_stat_cd`와 `exec_stat_cd` 이중 상태 저장 정합성 검증
- `RUNNING` 중복 실행 방지를 위한 transaction/lock/index 전략 검토 (workflow draft §5 conditional master update 기준)
- `run_id` unique 제약과 조회 인덱스 검토
- backend-generated `yyyymmdd_hhmmss_XXXX` 저장 길이(`VARCHAR(20)`)와 외부 모델 왕복 payload의 `run_id` 보존 규칙 검토
- 목록 조회 query shape는 scenario scope를 전제하지 않는다. `scenarioId` 비노출과 `scnr_id` 저장 의미는 §7.2 Current real-DB Mapping을 기준으로 해석한다.
- `ASSIGNEE_NAME` 검색 JOIN 경계는 `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no`, 표시 컬럼은 `tmd_usrm.usr_nm`으로 확정한다. DDL review에서는 JOIN 양측 인덱스 커버리지를 확인한다
- callback compare-and-set update 조건과 terminal overwrite 방지 전략 검토
- `tmd_mdl_scnrcasem.last_run_id` 컬럼의 invariant 와 application-validated 운용 방식 검토. case 최신 상태 동기화 시 `last_run_id = :run_id` 조건이 race condition 과 late callback 에 대해 충분한지 확인
- `req_param_json`, `rslt_ref_json` 미도입은 workflow draft Closed Decisions에서 확정되었다. output/intermediate/tradeOutput 다운로드 metadata는 기존 `tcm_filel` 관계를 사용하므로 추가 컬럼/테이블이 필요 없다. 향후 별도 result reference 저장 구조를 도입할 때만 schema review를 수행한다.
- DB-backed update predicate는 `case_id + usr_no ownership + del_yn = 'N' + exec_type_cd = 'INDEPENDENT'` 조건으로 active 소유 독립실행 case만 갱신하고, `case_titl_nm`, `case_expn`, `mod_*` 컬럼만 변경한다.
- DB-backed delete predicate는 `case_id + usr_no ownership + del_yn = 'N' + exec_type_cd = 'INDEPENDENT'` 조건으로 active 소유 독립실행 case만 soft delete하고, `del_yn = 'Y'`, `mod_*` 컬럼만 변경한다.

### Input for `design-consistency-review`

- 이 feature spec과 `scenario-case-execution-workflow-design-draft.md`의 용어/path/model 정합성 확인. 특히 `scenarioId` 비노출과 `scnr_id` 저장 의미가 §7.2 Current real-DB Mapping과 일치하는지 확인
- Notion 공유 문서가 canonical 계약처럼 읽히지 않는지 확인
- 기존 `ScenarioModelExecution` API와 naming/path/scope가 혼동되지 않는지 확인. MI Fundamental Model은 Next Gen 하위 API가 아니다
- 결과 조회/download 제외 범위와 edit/delete soft-delete 범위가 feature spec과 design doc에서 일관되는지 확인
- callback path가 Design callback의 canonical-id 중심 구조와 일관되는지 확인

### Input for `spring-api-implementation`

- Controller path family: `FundamentalModelCaseController` → `/api/v1/mi/fundamental-model/cases` (list/detail/update/delete/status/volumes/volumes-trade/price), `FundamentalModelCaseDedicatedController` → 같은 base path의 upload/run, `FundamentalModelRunDedicatedController` → `/api/v1/mi/fundamental-model/runs` callback.
- List: `GET`, query params `searchType` (enum `TITLE`|`ASSIGNEE_NAME`) + `keyword` (`keyword` null/blank이면 검색 없음, non-blank이면 `searchType` 필수). 변경된 `miBase + cases`, `execType`, `totalCount`, 검색 범위는 enhancement feature spec §3을 따른다.
- Upload: `POST`, multipart, `title`, `description`, `files` exactly 4. 파일명 역할은 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT`이 각각 정확히 1개이며, 일부 업로드 실패 시 기존 보상 삭제를 수행한다.
- Detail: `GET /{caseId}`, active `INDEPENDENT | MI_BASE`, returns non-null `execType`을 포함한 `FundamentalModelCaseDetailRes`
- Update: `PATCH /{caseId}`, active `INDEPENDENT`만 허용. request `FundamentalModelCaseUpdateReq(title, description)`, returns latest `FundamentalModelCaseDetailRes`
- Delete: `DELETE /{caseId}`, active `INDEPENDENT`만 soft delete, returns `BaseResponse<Void>` 200 OK
- Run: `POST /{caseId}/runs`, active `INDEPENDENT`의 `PENDING` 최초 실행 또는 `FAILED` 모델 호출 재시도만 허용하고 `COMPLETED`는 `409 BIZ_RULE_VIOLATION`, returns `202 Accepted` on success. `MI_BASE | SCENARIO`는 `404 RSC_NOT_FOUND`
- Status: `GET /{caseId}/status`, active `INDEPENDENT | MI_BASE` case 기준 조회
- Filter options: `GET /volumes/filter-options`, `caseId`를 전달하면 active `INDEPENDENT | MI_BASE`의 결과 범위를 조회한다. query params `caseId`(선택) + TRADE 조회 범위용 `selector`/`year`(선택 pair). `regions`는 `RegionFilterOption`으로 변경하고 Country child는 `FilterOption`을 재사용한다. TRADE는 현재 `selector/year`의 From/To 합집합이다.
- Volumes: `GET /{caseId}/volumes`, active `INDEPENDENT | MI_BASE` 중 `COMPLETED` case 한정. query params `segment`(`GLOBAL`|`LNG` 필수, `TRADE` 전달 시 400) + `types`(선택) + `regions`(선택) + `countryKeys`(선택) + `granularity`(선택, 기본 `MONTHLY`) + `periods`(선택, `MONTHLY`는 `yyyy-MM`, `YEARLY`는 `yyyy`). Country 선택 시 subtotal/total을 재계산한다. returns `FundamentalModelVolumesRes`
- Trade Flows: `GET /{caseId}/volumes/trade`, active `INDEPENDENT | MI_BASE` 중 `COMPLETED` case 한정. query params `selector`(필수) + `year`(필수, 2023~2030) + `unit`(선택, 기본 `MTPA`) + `types`(선택) + `regions`(선택) + `countryKeys`(선택). `regions` 없이 `countryKeys`만 전달하면 key에서 Region을 추출한다. `types`에 포함된 방향에 동일한 Region/Country 목록을 적용하고, 실제 매칭 Country가 속한 저장 `REGION_SUBTOTAL` header를 원래 순서로 유지하며 subtotal/total은 선택 Country leaf만으로 재계산한다. 유효하지만 실제 header에 매칭되지 않는 Country key만 전달한 경우 빈 결과를 반환한다. returns `FundamentalModelTradeFlowsRes`
- Price: `GET /{caseId}/price`, active `INDEPENDENT | MI_BASE` 중 `COMPLETED` case 한정. 쿼리 파라미터 없음. returns `FundamentalModelPriceRes` (차트 + Annual 테이블 + Monthly 테이블 동시 반환)
- Callback: `POST /api/v1/mi/fundamental-model/runs/{runId}/callback`, 외부 모델 전용
- Service: `FundamentalModelCaseService` + `FundamentalModelCaseServiceImpl` 단일 real 구현. `scenarioId` 없이 `caseId` 또는 `runId`로 식별하며 mock delegator/reset method는 없다.
- Active-resource rule: `delYn = Y` case는 list에서 제외하고 detail/status/run/update/delete/volumes/price에서 `RSC_NOT_FOUND` 처리
- Data state rule: volumes/price는 `COMPLETED` case만 허용. `PENDING`/`RUNNING`이면 `409 BIZ_RULE_VIOLATION`, `FAILED`이면 `404 RSC_NOT_FOUND`
- 신규 enum: `FundamentalModelSegment` (`GLOBAL`, `LNG`, `TRADE` — `TRADE`는 `/volumes`에서 불허), `FundamentalModelDataType` (`DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`), `FundamentalModelRegion` (`ASIA`, `EUROPE`, `NORTH_AMERICA`, `MIDDLE_EAST`, `AUSTRALIA`, `RUSSIA`, `AFRICA`, `SOUTH_AMERICA`), `FundamentalModelGranularity` (`MONTHLY`, `YEARLY`), `FundamentalModelVolumeUnit` (`MTPA`), `VolumesRowKind` (`COUNTRY`, `REGION_SUBTOTAL`, `TYPE_TOTAL`), `PriceSeriesType` (`BASE_CASE`, `USER_ADJUSTED`), `TradeFlowSelector` (`LNG`, `PNG`), `TradeFlowUnit` (`MTPA`, `BCM`), `TradeFlowDirection` (`TO_COUNTRY`, `FROM_COUNTRY`), `TradeCountryHeaderKind` (`REGION_SUBTOTAL`, `COUNTRY`, `GRAND_TOTAL`)
- Status enum: `FundamentalModelCaseStatus` = `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`. `FundamentalModelRunStatus` = `RUNNING`, `COMPLETED`, `FAILED` (분리 enum). `FundamentalModelCaseSearchType` = `TITLE`, `ASSIGNEE_NAME`
- Failure reason candidates: `TIMEOUT`, `EXTERNAL_ERROR`, `UNKNOWN`
- Base response: `BaseResponse<T>` via `BaseResponseFactory`
- Permissions: existing Next Gen `USER/SKGAS_COMPANY_USER_ONLY` and `ADMIN/THIRD_PARTY_ADMIN_ONLY` pattern
