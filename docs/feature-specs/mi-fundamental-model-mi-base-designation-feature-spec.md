# MI Fundamental Model MI Base 지정 Feature Spec

## Document Control

- status: Review
- canonical for: 기존 Scenario Case 목록의 current MI Base 포함 계약·current MI Base 상세/결과 조회 계약·MI Base 지정 API 계약
- owner: BE
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
- depends on:
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `SECURITY.md`
- supersedes: 없음
- last synced: 2026-09-14
- open questions: 없음. 상세 화면의 지정 actor/time 표시와 input 내용 제공은 별도 후속 계약에서 다룬다.

## 1. Feature Goal

기존 Scenario Case 목록 API가 current MI Base와 그 외 실행 case를 분리 반환하도록 변경하고, 목록에서 선택한 active `MI_BASE`도 기존 case 상세와 결과 API로 조회할 수 있게 한다. MI Base는 읽기 전용으로 제공하며 수정·삭제·재실행은 허용하지 않는다. 완료된 실행 case를 현재 MI Base로 원자적으로 지정하고 이전 MI Base는 `INDEPENDENT`로 복귀시키며, 별도 지정 해제 API는 제공하지 않는다.

## 2. Referenced Domain Constraints

MI Base의 lifecycle·보존·source snapshot 의미는 `scenario-case-execution-workflow-design-draft.md`, 물리 제약·migration은 `scenario-case-execution-erd-ddl-draft.md`가 canonical source다. 아래 항목은 API 요청·응답과 검증에 직접 필요한 참조 요약이며, 충돌 시 각 canonical design을 따른다.

- `caseId`는 input 파일 3개가 귀속된 실행 case의 식별자다.
- `runId`는 case 아래 실행 이력 식별자이며 input 파일 제공 대상 식별자로 사용하지 않는다.
- `execType` 코드는 `INDEPENDENT | SCENARIO | MI_BASE`를 유지한다.
- MI Base는 별도 case/run/file 세트를 신규 적재한 건이 아니라, 기존 독립실행 case 1건을 권한 있는 사용자가 `execType = MI_BASE`로 지정한 건이다.
- MI Base 지정은 case의 `caseId`, 소유자, 실행 이력, 결과, `SCNRCASE + caseId`로 연결된 input Excel 3개를 변경하지 않는다.
- 현재 MI Base는 전역에서 정확히 0~1건이다. active `MI_BASE` row 자체를 최대 1건으로 제한하며, 새 대상을 지정하면 이전 MI Base는 `INDEPENDENT`로 복귀한다.
- 지정 이후 시작하는 수급모델 input 생성은 새 MI Base case의 `DEMAND`, `SUPPLY`, `TRADE` input을 기준으로 한다. 이미 생성·접수·완료된 실행의 input, 결과, 기준값은 소급 변경하지 않는다.
- 한 번의 수급모델 input 생성은 시작 시 현재 MI Base `caseId`와 정확한 3개 파일 metadata를 한 번만 선택해 source snapshot으로 고정한다. 생성 중 MI Base가 바뀌어도 세 역할을 다른 case에서 섞지 않는다.
- 물리 스키마 및 DBA rollout은 `scenario-case-execution-erd-ddl-draft.md`를 따른다. current MI Base 단건 불변식은 `exec_type_cd`와 `del_yn`에서 파생되는 generated `actv_base_yn` unique로 보장한다.

### Fixed MI Base Lifecycle

- 새 지정 transaction은 기존 current의 `exec_type_cd`를 `INDEPENDENT`로 복귀시킨 뒤 대상의 `exec_type_cd`를 `MI_BASE`로 변경한다.
- 과거 MI Base 지정 여부는 `exec_type_cd`에 이력으로 남기지 않는다. 별도 지정 이력 table/API도 이번 범위에 포함하지 않는다.
- current는 유일한 active `exec_type_cd = MI_BASE` row다. `MAX(case_id)`나 일반 `mod_dtm`으로 계산하지 않는다.
- `actv_base_yn`은 `del_yn = 'N' AND exec_type_cd = 'MI_BASE'`일 때만 `Y`, 그 외에는 `NULL`인 STORED generated column이다. `UNIQUE (actv_base_yn)`이 current 최대 1건을 강제한다.
- current MI Base는 상세·상태·Volumes filter-options·Volumes·Trade Flows·Price 조회만 허용하는 읽기 전용 case다. metadata 수정, soft delete, 동일 case 재실행은 허용하지 않는다.
- FE는 상세 응답의 `execType = MI_BASE`를 기준으로 수정·삭제·재실행 action을 노출하거나 관련 API를 호출하지 않는다. backend도 직접 호출을 막기 위해 mutation 대상의 `INDEPENDENT` 제한을 유지한다.
- current가 필요 없어진다는 의미의 별도 지정 해제 command는 제공하지 않는다. 기준 교체는 새 완료 `INDEPENDENT` case를 지정하는 API만 사용한다.
- 이전 MI Base는 `INDEPENDENT`로 복귀하므로 기존 `cases` 목록과 기존 독립실행 read/mutation 규칙을 다시 따른다.

## 3. API Slice A — 기존 Scenario Case 목록에 current MI Base 포함

### Endpoint

```http
GET /api/v1/mi/fundamental-model/cases
```

별도 `/execution-options` 조회 API는 추가하지 않는다. 기존 목록 응답을 화면의 `기준값 (MI Base)`와 `그 외 실행 케이스` 두 영역에 직접 대응하도록 확장한다.

### Request

기존 query contract를 유지한다.

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `searchType` | `FundamentalModelCaseSearchType` | 조건부 | `keyword`가 non-blank일 때 필수. `TITLE \| ASSIGNEE_NAME` |
| `keyword` | `String` | N | null 또는 trim 후 blank이면 검색하지 않음 |

current MI Base는 고정 기준값이므로 검색 결과와 무관하게 `miBase`에 항상 반환한다. 검색 조건은 `cases`에만 적용한다. 이 규칙은 검색 불일치 때문에 `miBase=null`이 되어 "현재 기준 없음"으로 오해되는 것을 막는다.

### Selection and Sort

1. `del_yn = 'N'`인 active case만 대상으로 한다.
2. 단일 active `exec_type_cd = 'MI_BASE'` case를 `miBase`로 반환한다. runtime 조회가 `MAX(case_id)`로 current를 고르지 않는다.
3. `cases`는 current를 제외한 active `INDEPENDENT` case다. 이전 MI Base도 새 지정 시 `INDEPENDENT`로 복귀하므로 여기에 포함된다.
4. `SCENARIO`는 `miBase`와 `cases`에서 모두 제외한다.
5. `cases`는 기존과 같이 `createdAt DESC, caseId DESC`로 정렬하고 paging/limit 없이 반환한다.
6. `totalCount`는 화면에 표시되는 전체 건수로 `(miBase == null ? 0 : 1) + cases.size()`다. 검색 중에는 pinned `miBase` 1건과 필터를 통과한 `cases` 건수의 합이다.

### Response

응답 wrapper는 기존 `BaseResponse<FundamentalModelCaseListRes>`를 유지한다. `FundamentalModelCaseListRes`에 nullable `miBase`를 추가하고, `FundamentalModelCaseListItemRes`에 non-null `execType`을 추가한다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "totalCount": 21,
    "miBase": {
      "caseId": 105,
      "title": "미국-이란 긴장 고조 케이스",
      "status": "COMPLETED",
      "execType": "MI_BASE",
      "assigneeName": "홍길동",
      "createdAt": "2026-06-30T16:25:00",
      "updatedAt": "2026-06-30T16:25:00"
    },
    "cases": [
      {
        "caseId": 110,
        "title": "수급모델 독립 실행 테스트 0001",
        "status": "COMPLETED",
        "execType": "INDEPENDENT",
        "assigneeName": "홍길동",
        "createdAt": "2026-06-30T17:30:00",
        "updatedAt": "2026-06-30T17:34:00"
      }
    ]
  }
}
```

`FundamentalModelCaseListRes`:

| 필드 | 타입 | nullable | 설명 |
|---|---|---|---|
| `totalCount` | `Integer` | N | 현재 화면에 표시되는 `miBase`와 필터된 `cases`의 합 |
| `miBase` | `FundamentalModelCaseListItemRes` | Y | current MI Base. 없으면 `null` |
| `cases` | `List<FundamentalModelCaseListItemRes>` | N | current를 제외한 실행 case. 없으면 빈 목록 |

`FundamentalModelCaseListItemRes`는 기존 필드를 유지하고 다음 필드를 추가한다.

| 필드 | 타입 | nullable | 설명 |
|---|---|---|---|
| `execType` | `FundamentalModelExecType` | N | `miBase`는 `MI_BASE`; `cases`는 `INDEPENDENT` |

`기준값 (MI Base)`, `그 외 실행 케이스` section label과 `BC` badge는 응답의 `miBase` 위치를 기준으로 FE가 표시한다. 목록에서는 지정자·지정시각 문구를 사용하지 않으므로 해당 audit 필드는 이번 목록 DTO에 추가하지 않는다.

### Compatibility

- path와 query parameter는 유지하지만 response schema와 `totalCount` 의미가 바뀌는 consumer-visible contract change다.
- 기존 consumer가 unknown JSON field를 무시하더라도 `totalCount == cases.size()`를 전제하면 오동작하므로 FE와 backend를 조율해 배포한다.
- 기존 `cases`의 `INDEPENDENT` 범위는 유지된다. 이전 MI Base도 복귀 후 `INDEPENDENT`로 반환된다.
- 기존 필드를 즉시 제거하거나 이름을 바꾸지 않으며 API version은 `/v1`을 유지한다.

### Permissions

- 기존 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`를 유지한다.
- 목록은 소유자 `usr_no`로 제한하지 않는 shared read model이다.
- 클라이언트가 company/tenant 식별자를 전달해 조회 범위를 넓히는 계약은 추가하지 않는다.

### Representative Errors

| 조건 | HTTP | `ResultCode` |
|---|---:|---|
| non-blank `keyword`인데 `searchType` 없음 | 400 | `REQ_INVALID_PARAMETER` |
| 인증되지 않음 | 401 | 공통 인증 오류 |
| 허용되지 않은 role/condition | 403 | 공통 권한 오류 |

### Current MI Base 상세 및 결과 조회

목록에서 `miBase` 또는 `cases` item을 선택하면 기존 상세 endpoint를 그대로 사용한다.

```http
GET /api/v1/mi/fundamental-model/cases/{caseId}
```

- active `exec_type_cd IN ('INDEPENDENT', 'MI_BASE')` case를 조회할 수 있다. `SCENARIO`와 soft-deleted case는 기존 active resource hiding 규칙에 따라 `404 RSC_NOT_FOUND`를 반환한다.
- 기존 상세 응답의 case metadata, input/output/intermediate 파일명 metadata, `latestRun` 범위는 유지한다.
- `FundamentalModelCaseDetailRes`에 non-null `execType: FundamentalModelExecType`을 추가한다. current MI Base 상세는 `MI_BASE`, 일반 독립실행 상세는 `INDEPENDENT`를 반환하며 FE는 이 값으로 상세 화면의 `BC` 표시 여부를 판단한다.
- 목록과 같은 shared read 권한인 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`를 유지하며 소유자 `usr_no`로 제한하지 않는다.
- `PATCH /cases/{caseId}`, `DELETE /cases/{caseId}`, `POST /cases/{caseId}/runs`는 active `INDEPENDENT`만 허용하고 `MI_BASE`에는 계속 `404 RSC_NOT_FOUND`를 반환한다. 실행 API의 허용 상태는 기존 Case API Feature Spec에 따라 `PENDING | FAILED`이며 `COMPLETED`는 재실행할 수 없다.
- FE는 `execType = MI_BASE`인 상세 화면에서 결과 탭은 노출하되 수정·삭제·재실행 action은 노출하거나 호출하지 않는다. 이 FE 정책은 backend의 mutation 차단을 대체하지 않는다.
- 지정 actor/time은 현재 상세 응답에 추가하지 않는다. 필요해지면 저장 의미와 API 계약을 별도 확정한다.

MI Base 상세의 결과 탭은 기존 응답 형식과 필터 계약을 그대로 사용한다. 다음 read operation은 active `exec_type_cd IN ('INDEPENDENT', 'MI_BASE')`를 허용하고 `SCENARIO`와 soft-deleted case는 기존 resource hiding 규칙에 따라 `404 RSC_NOT_FOUND`를 반환한다.

| 화면 용도 | Method | Endpoint |
|---|---|---|
| 상태 | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/status` |
| Volumes/Trade 필터 옵션 | `GET` | `/api/v1/mi/fundamental-model/cases/volumes/filter-options?caseId={caseId}` |
| Volumes | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/volumes` |
| Trade Flows | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/volumes/trade` |
| Price | `GET` | `/api/v1/mi/fundamental-model/cases/{caseId}/price` |

- 각 operation의 query parameter, response DTO, 빈 결과와 오류 계약은 [MI Fundamental Model Case API Feature Spec](mi-fundamental-model-mock-api-feature-spec.md)을 그대로 따른다. 이 문서는 그 계약을 복제하지 않고 `MI_BASE` case의 read 허용 범위만 확장한다.
- MI Base는 지정 적격성상 `COMPLETED`이므로 결과 API의 기존 completed-case 조건을 만족해야 한다. latest completed run/result 처리도 일반 완료 독립실행과 같다.
- `caseId` 없이 호출하는 filter-options의 정적 옵션 응답은 기존 동작을 유지한다.

## 4. API Slice B — 독립실행 case를 MI Base로 지정

### Endpoint

```http
PUT /api/v1/mi/fundamental-model/cases/{caseId}/mi-base
```

`caseId`가 가리키는 완료 실행을 전역 현재 MI Base로 지정하는 idempotent command다. 같은 현재 MI Base를 다시 지정해도 authorization과 latest run/input 3종/Price 적격성을 먼저 다시 검증하며, 모두 통과하면 row를 변경하지 않고 `200`으로 현재값을 반환한다. MI Base 해제만 수행하는 endpoint는 이번 슬라이스에 포함하지 않는다.

### Request

- path variable: `caseId` (`Long`, 1 이상, 필수)
- query/body: 없음
- 클라이언트는 `execType`, 이전 MI Base ID, 파일 식별자 또는 company/tenant 식별자를 전달하지 않는다.

### Eligibility and Designation

1. 공통 대상은 `del_yn = 'N'`, `scnr_id IS NULL`, `case_stat_cd = 'COMPLETED'`이고 `exec_type_cd = 'INDEPENDENT'`여야 한다. 단, 요청 `caseId`가 현재 MI Base와 같으면 `exec_type_cd = 'MI_BASE'`를 멱등 예외로 허용하며, 2~4의 적격성을 다시 검증한 뒤에만 변경 없는 `200`을 반환한다. 그 밖의 `MI_BASE`나 `SCENARIO`는 대상이 될 수 없다.
2. `last_run_id`가 대상 case의 active `COMPLETED` run history를 가리켜야 한다.
3. `SCNRCASE + caseId`에 active input 파일이 정확히 3개이고 파일명의 `DEMAND`, `SUPPLY`, `TRADE` 역할이 각각 1개여야 한다.
4. 최신 완료 run에는 MI Base 그래프에 필요한 Price 결과가 존재해야 한다.
5. 한 transaction에서 기존 current `MI_BASE`를 `INDEPENDENT`로 되돌리고 그 UPDATE를 DB에 flush한 뒤 대상을 `MI_BASE`로 변경한다. JPA 변경 감지의 자동 UPDATE 순서에 맡기지 않는다. `actv_base_yn`은 application이 직접 쓰지 않고 DB가 두 row의 `exec_type_cd`와 `del_yn`에서 자동 계산한다. 변경 row의 `mod_usr_logn_id`, `mod_dtm`은 요청 actor와 변경 시각으로 갱신한다.
6. case/run/file/result row를 생성·삭제하지 않고 `case_id`, `usr_no`, `case_stat_cd`, `last_run_id`, 실행 이력과 결과를 보존한다.
7. `exec_type_cd`와 `del_yn`에서 파생한 generated `actv_base_yn` unique constraint가 current MI Base 2건 생성을 차단한다. constraint 충돌은 `409 BIZ_RULE_VIOLATION`으로 매핑한다.
8. transaction commit 이후 시작되는 수급모델 input 생성만 새 MI Base input 3개를 사용한다. input 생성 시작 시 현재 MI Base `caseId`와 `DEMAND`, `SUPPLY`, `TRADE` 파일 metadata를 한 번에 source snapshot으로 고정하고, 세 workbook open은 그 snapshot만 사용한다. 기존에 생성·staging·접수·완료된 실행은 변경하지 않는다.
9. MI Base 지정은 `COMPLETED` case만 허용하고 실행 API는 `PENDING | FAILED`만 허용하므로 지정과 실행의 적격 상태는 겹치지 않는다. metadata 수정·soft delete는 지정과 교차할 수 있으므로 write lock 또는 atomic exec-type predicate로 직렬화하며, 지정이 먼저 commit되면 해당 mutation은 MI Base를 변경하지 않고 실패한다.
10. current가 있는 상태의 서로 다른 대상 동시 지정은 current/target row lock으로 직렬화한다. 두 요청이 순차 성공하면 최종 current는 마지막 commit 대상이다. current가 0건인 최초 지정 두 건은 공통 current row가 없어 완전 직렬화되지 않을 수 있으므로 unique guard가 한 요청을 거부할 수 있다. unique 충돌·deadlock·lock timeout처럼 실제 DB 경쟁 오류가 발생한 요청은 `409`로 종료하고, 성공·충돌 후 클라이언트는 `/cases`를 재조회한다.

기존 MI Base는 `INDEPENDENT`로 되돌아와 `cases`에 포함된다.

### Response

응답 wrapper는 변경된 목록과 같은 `BaseResponse<FundamentalModelCaseListItemRes>`를 사용한다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "caseId": 110,
    "title": "수급모델 독립 실행 테스트 0001",
    "status": "COMPLETED",
    "execType": "MI_BASE",
    "assigneeName": "홍길동",
    "createdAt": "2026-06-30T17:30:00",
    "updatedAt": "2026-07-23T16:50:00"
  }
}
```

### Permissions

- 기존 MI 수급모델 실행 접수와 동일하게 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`를 허용한다. 별도 operator entitlement는 추가하지 않는다.
- MI Base 지정은 사용자 개인 소유 자원 변경이 아니라 권한 있는 사용자가 전체 독립실행 case 중 현재 기준을 선택하는 command다.
- 허용 actor는 다른 사용자가 생성한 완료 실행도 지정할 수 있으므로 `usr_no = currentUser` 또는 대상 소유 회사 조건을 적용하지 않는다.
- 서버는 `caseId`에 해당하는 case의 active/type/status와 run/input/Price 적격성만 검증한다. 대상 소유자·회사 조회는 지정 조건이 아니다.

### Representative Errors

| 조건 | HTTP | `ResultCode` | 클라이언트 의미 |
|---|---:|---|---|
| `caseId < 1` 또는 형식 오류 | 400 | `REQ_INVALID_PARAMETER` | 잘못된 실행 case 식별자 |
| case 없음, soft delete, 또는 `SCENARIO` case | 404 | `RSC_NOT_FOUND` | 지정 가능한 독립실행 자원이 아님 |
| 대상 상태가 `PENDING`, `RUNNING`, `FAILED` | 409 | `BIZ_RULE_VIOLATION` | 완료된 실행만 지정 가능 |
| latest run 불일치·미완료, input 역할 누락/중복, Price 결과 없음 | 409 | `BIZ_RULE_VIOLATION` | MI Base로 사용할 데이터가 완전하지 않음 |
| unique/deadlock/lock-timeout 등 실제 지정 경쟁 오류 | 409 | `BIZ_RULE_VIOLATION` | 현재 옵션 재조회 필요 |
| 인증되지 않음 | 401 | 공통 인증 오류 | 로그인 필요 |
| 허용되지 않은 role/condition | 403 | 공통 권한 오류 | 지정 권한 없음 |

## 5. OpenAPI Documentation Plan

### Case List Operation

- summary: `MI Fundamental Model 실행 Case 및 MI Base 목록 조회 API`
- description에 화면의 current MI Base와 그 외 실행 case를 각각 `miBase`, `cases`로 분리 반환함을 명시한다.
- `miBase=null`, `cases=[]`, `totalCount=(miBase 0|1)+cases.size()`의 의미와 `SCENARIO` 제외를 명시한다.
- 검색은 pinned `miBase`가 아니라 `cases`에만 적용하고, `cases` 정렬은 `createdAt DESC, caseId DESC`임을 설명한다.
- item의 `execType` schema는 non-null enum으로 표현하고, operation 예시는 current `MI_BASE`와 `INDEPENDENT`를 모두 보여준다.

### Case Detail Operation

- description에 active `INDEPENDENT`와 current `MI_BASE`를 조회할 수 있고 `SCENARIO`와 soft-deleted case는 조회되지 않음을 명시한다.
- `FundamentalModelCaseDetailRes.execType`을 non-null enum으로 문서화하고 `MI_BASE` 상세 예시를 추가한다.
- 상세 `GET` 허용 범위가 수정·삭제·실행 권한을 넓히지 않는다는 점을 명시한다.
- FE가 `execType=MI_BASE`에서 결과 탭은 노출하고 수정·삭제·재실행 action은 호출하지 않는다는 소비자 분기를 설명한다.

### Status and Result Read Operations

- status/filter-options/Volumes/Trade Flows/Price description에 active `INDEPENDENT | MI_BASE`를 조회할 수 있고 `SCENARIO`와 soft-deleted case는 조회되지 않음을 명시한다.
- 기존 query parameter, response schema, completed-case 검증, 빈 결과와 대표 오류는 Case API Feature Spec 계약을 유지한다.
- generated OpenAPI에서 다섯 operation의 MI Base 허용 설명과 기존 structured success/error response가 함께 노출되는지 확인한다.

### MI Base Designation Operation

- summary: `독립실행 case MI Base 지정 API`
- description에 완료 독립실행을 현재 MI Base로 지정하고, 기존 MI Base는 `INDEPENDENT`로 복귀하며, 이후 시작되는 실행에만 새 기준이 적용됨을 명시한다.
- path `caseId` schema에는 1 이상 제약과 완료 독립실행 case 식별자라는 의미를 기록한다.
- 동일 case 반복 지정은 `200`인 멱등 동작, 기존 실행 결과에는 소급 적용하지 않음, 성공 후 `/cases` 재조회 흐름을 설명한다.
- operation 예시는 `execType=MI_BASE`인 `FundamentalModelCaseListItemRes`를 사용한다.
- 200/400/401/403/404/409 대표 응답을 `@ApiResponses`로 구조화하고, 상태 부적합·데이터 불완전·동시 지정 충돌이 모두 409 business-rule 오류임을 설명한다.
- 기존 `AppErrorException`의 공통 400 mapping에 기대지 않고, 404/409는 `AppHttpStatusException` 등 HTTP status를 보존하는 공통 예외 경로로 구현한다.

## 6. Data and Implementation Impact

### Schema / DBA

- schema change: `tmd_mdl_scnrcasem.actv_base_yn`을 `exec_type_cd`와 `del_yn`에서 파생하는 STORED generated helper로 두고 `tmd_mdl_scnrcasem_uq02 UNIQUE (actv_base_yn)`를 추가한다.
- DDL/ERD review: legacy current seed는 이전 조회 호환을 위한 1회 migration일 뿐, 배포 후 current 판정에 `MAX(case_id)`를 사용하지 않는다. pre-check/backfill/apply/rollback/verify는 canonical DDL draft를 따른다.
- 신규 table, FK, common code는 없다. `ACTV`, `BASE`, `YN`은 기존 DB 표준 용어를 재사용한다.
- MI Base 지정 작업은 기존 case row의 `exec_type_cd`, `mod_*`만 변경하며 generated `actv_base_yn`은 DB가 계산한다. 실행·파일·결과 row를 신규 생성하지 않는다.

### Recommended Data Access

- 목록: 기존 QueryDSL `findListRows`를 current MI Base 단건과 current 제외 case 목록을 함께 조회하는 repository contract로 변경한다. current 조회에는 `actv_base_yn` unique index를 사용하고, 나머지 목록에는 기존 검색·정렬을 유지한다.
- 상세: `findDetailRow`의 active type 범위를 `INDEPENDENT | MI_BASE`로 넓히고 projection/`FundamentalModelCaseDetailRes`에 non-null `execType`을 추가한다. mutation 경로의 `INDEPENDENT` 제한은 유지한다.
- 상태/결과 read: 상태 및 completed result 조회용 active-case 경계는 `INDEPENDENT | MI_BASE`를 허용한다. mutation에 사용하는 active-case/conditional update 경계와 분리해 `MI_BASE` 허용이 수정·삭제·재실행으로 전파되지 않게 한다.
- MI Base 지정: JPA transaction에서 대상과 current MI Base를 write lock으로 확인한다. 기존 current type을 `INDEPENDENT`로 복귀시키고 flush한 뒤 대상 type을 `MI_BASE`로 변경한다. DB unique는 current 2건 방어선이다. current가 있으면 교체 command가 해당 row lock으로 직렬화되고, current 0건 최초 지정 경합에서는 한 요청이 unique 충돌로 `409`가 될 수 있다.
- 기존 실행 정책 정합: `POST /cases/{caseId}/runs`의 conditional update는 `PENDING | FAILED`만 허용한다. `COMPLETED` 요청은 `409 BIZ_RULE_VIOLATION`으로 거부한다.
- 기존 mutation: update/delete는 지정과 같은 lock 또는 atomic `execType = INDEPENDENT` predicate를 적용한다.
- 이후 input 생성: merge 시작 시 현재 MI Base case와 그 case의 `SCNRCASE + caseId` input 3개 metadata를 한 번만 조회해 immutable source snapshot으로 고정한다. `BasecaseWorkbookProvider`는 세 역할을 snapshot에서만 열어야 한다. MI Base가 없을 때만 기존 플랫폼 기본 template 3개 전체를 snapshot으로 선택하며, 지정된 MI Base 파일을 읽지 못한 경우 이전/default 기준으로 조용히 fallback하지 않고 실행 준비를 실패시킨다.
- input 파일 metadata: 기존 `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` 재사용.

### Expected Touch Points

- `FundamentalModelCaseController`
- `FundamentalModelCaseService` / `FundamentalModelCaseServiceImpl`
- 변경 `FundamentalModelCaseListRes` / `FundamentalModelCaseListItemRes`
- 변경 `FundamentalModelCaseDetailRes` / `ScenarioCaseDetailRow`
- 신규 전용 projection 또는 기존 `ScenarioCaseListRow`의 내부 재사용
- `CustomScenarioCaseRepository` / `CustomScenarioCaseRepositoryImpl`
- `ScenarioCase`의 generated current marker read-only mapping과 MI Base 전이 method
- `BasecaseWorkbookProvider`의 현재 MI Base input 조회 경계
- 목록·상세·상태/결과 조회·지정·새 MI Base input 적용 흐름의 service/controller/repository focused tests

## 7. Acceptance Criteria

### List

- Given current MI Base 1건과 active INDEPENDENT가 있을 때, when `GET /cases`를 호출하면, then current는 `miBase`, 나머지는 `cases`에 `createdAt DESC, caseId DESC`로 나온다.
- Given 이전 MI Base가 새 지정으로 `INDEPENDENT`에 복귀했을 때, when 목록을 조회하면, then 해당 case는 `cases`에 포함되고 current만 `miBase`에 나온다.
- Given SCENARIO case가 있을 때, when 목록을 조회하면, then 어느 응답 필드에도 포함되지 않는다.
- Given MI Base가 없을 때, then `miBase=null`이고 `cases`는 정상 반환된다.
- Given current 이외 case가 없을 때, then `cases=[]`, `totalCount=1`이고 현재 MI Base는 정상 반환된다.
- Given 검색어가 current MI Base와 일치하지 않을 때, then `miBase`는 그대로 반환하고 `cases`에만 검색을 적용한다.
- Given 목록 응답일 때, then `totalCount=(miBase == null ? 0 : 1)+cases.size()`이고 모든 item의 `caseId`, `title`, `status`, `execType`, `assigneeName`, `createdAt`, `updatedAt`은 non-null이다.

### Detail

- Given 목록의 current `miBase.caseId`일 때, when `GET /cases/{caseId}`를 호출하면, then 기존 상세 metadata/files/latestRun과 `execType=MI_BASE`를 반환한다.
- Given active `INDEPENDENT` case일 때, when 상세 조회하면, then 기존 상세 내용과 `execType=INDEPENDENT`를 반환한다.
- Given `SCENARIO` 또는 soft-deleted case일 때, when 상세 조회하면, then `404 RSC_NOT_FOUND`를 반환한다.
- Given current `MI_BASE`일 때, when 수정·삭제·실행을 요청하면, then 기존 resource hiding 규칙에 따라 `404 RSC_NOT_FOUND`를 반환한다.
- Given current `MI_BASE` 상세 화면일 때, then FE는 결과 탭을 노출하고 수정·삭제·재실행 action은 노출하거나 관련 API를 호출하지 않는다.

### Status and Results

- Given current `MI_BASE`일 때, when status 또는 Volumes filter-options를 조회하면, then 일반 완료 독립실행과 같은 응답 계약으로 현재 상태와 실제 결과 범위의 옵션을 반환한다.
- Given current `MI_BASE`일 때, when Volumes, Trade Flows 또는 Price를 조회하면, then latest completed run의 기존 결과를 일반 완료 독립실행과 같은 필터·응답 계약으로 반환한다.
- Given `SCENARIO` 또는 soft-deleted case일 때, when 위 status/result operation을 호출하면, then 기존 resource hiding 규칙에 따라 `404 RSC_NOT_FOUND`를 반환한다.

### MI Base 지정

- Given 완료된 active INDEPENDENT case와 current MI Base가 있을 때, when 대상 case를 지정하면, then 같은 transaction에서 대상이 current MI Base가 되고 응답은 대상 option을 반환한다. 이전 case는 `INDEPENDENT`로 복귀한다.
- Given 현재 MI Base와 같은 `caseId`를 다시 지정할 때, when authorization과 latest run/input 3개/Price 적격성이 모두 유효하면, then 추가 row 변경 없이 동일한 성공 응답을 반환한다. 적격성이 깨졌으면 기존 row를 바꾸지 않고 409를 반환한다.
- Given 더 작은 `caseId`의 과거 독립실행을 지정할 때, then `caseId` 크기와 무관하게 그 대상이 현재 MI Base가 된다.
- Given current가 있는 상태에서 두 사용자가 서로 다른 case를 동시에 지정할 때, then 요청은 공통 current/target lock으로 직렬화되고 DB에는 active `MI_BASE`가 최대 1건만 남는다. 둘 다 순차 성공하면 마지막 commit 대상이 current다. Given current가 0건인 최초 지정 경합일 때, then 한 요청은 성공하고 unique 충돌이 난 요청은 409가 될 수 있다.
- Given 대상이 미완료이거나 latest run/input 3개/Price 결과가 불완전할 때, then 409를 반환하고 기존 MI Base는 변경하지 않는다.
- Given 허용 actor가 다른 사용자가 생성한 완료 독립실행을 지정할 때, then 소유자 또는 소유 회사 조건 없이 현재 MI Base로 교체된다.
- Given `COMPLETED` INDEPENDENT case일 때, when 실행을 다시 요청하면, then 409로 거부한다. 새 정상 실행은 새 case를 생성해야 하며 `FAILED`일 때만 같은 case로 모델 호출을 재시도한다.
- Given metadata 수정 또는 soft delete가 지정과 경쟁할 때, then 같은 row-lock/atomic predicate 경계로 직렬화되어 MI Base row를 변경하지 않는다.
- Given 지정 transaction이 commit된 뒤 새 수급모델 input을 생성할 때, then 대상 case의 `DEMAND`, `SUPPLY`, `TRADE`를 기준으로 사용한다.
- Given input 생성이 source snapshot을 선택한 뒤 MI Base가 교체될 때, then 진행 중 생성은 선택한 한 case의 세 파일만 사용하고 다음 생성부터 새 MI Base를 사용한다.
- Given 지정 전에 이미 생성·접수·완료된 실행이 있을 때, then 해당 input·결과·기준값은 변경되지 않는다.

## 8. API Slice Boundary

### Included

- 변경 `GET /api/v1/mi/fundamental-model/cases`
- 변경 `GET /api/v1/mi/fundamental-model/cases/{caseId}`의 active `MI_BASE` 조회와 상세 `execType` 응답
- 변경 `GET /api/v1/mi/fundamental-model/cases/{caseId}/status`의 active `MI_BASE` 조회
- 변경 `GET /api/v1/mi/fundamental-model/cases/volumes/filter-options`의 `caseId=MI_BASE` 조회
- 변경 `GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes`
- 변경 `GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes/trade`
- 변경 `GET /api/v1/mi/fundamental-model/cases/{caseId}/price`
- 신규 `PUT /api/v1/mi/fundamental-model/cases/{caseId}/mi-base`
- `FundamentalModelCaseListRes.miBase`와 `FundamentalModelCaseListItemRes.execType` 추가, `totalCount` 의미 변경
- 현재 MI Base 원자적 지정, 이전 current의 `INDEPENDENT` 복귀, 완료 case 재실행 금지 정합화, update/delete와의 경쟁 방지, 이후 수급모델 input 생성의 단일 source snapshot 전환
- current MI Base generated marker 단건 unique schema guard와 current-only legacy migration

### Excluded

- MI Base 지정 취소만 수행하는 해제 API
- MI Base 지정 이력 조회·승인 workflow와 별도 history table
- 드롭다운 `BC` badge, section label, 선택 상태, `파일 업로드` action
- 상세 화면의 MI Base 지정자·지정시각 audit 필드
- input 내용 조회·전달·편집 API. 해당 계약은 [MI Fundamental Model Input 제공 Feature Spec](mi-fundamental-model-input-provision-feature-spec.md)에서 관리한다.
- output/결과 파일 다운로드
- Excel 수식·참조 기반 영향 셀 계산 API
- cache와 범용 Excel-to-JSON 변환 API

### Verification Plan

```bash
./gradlew test -PforceTest --tests 'com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseServiceImplTest'
./gradlew test -PforceTest --tests 'com.sk.domain.mi.controller.fundamentalmodel.FundamentalModelCaseControllerTest'
./gradlew compileJava -x test
```

추가 focused test class를 분리하면 실제 class name으로 대체한다. 목록 operation은 generated OpenAPI에서 `miBase` nullable, `cases` non-null, `execType` non-null, `totalCount` 의미와 검색 범위를 확인한다. 상세 operation은 active `MI_BASE` 조회, 상세 `execType`, `SCENARIO` hiding, `MI_BASE` mutation 거부를 확인한다. status/filter-options/Volumes/Trade Flows/Price는 current `MI_BASE`의 정상 조회와 `SCENARIO` hiding을 검증한다. 지정 operation은 path, `caseId` 제약, side effect/idempotency 설명과 structured errors를 확인한다. 지정 concurrency test는 lifecycle 안과 무관하게 current marker가 최대 1건인지 검증한다.

## 9. Implementation Readiness

- MI Base 지정+상세/결과 조회: `ready`; 지정 API 운영 활성화는 DBA rollout 선행 필요
- existing list/detail/status/result API slice: `contract ready`
- MI Base 지정 API slice: `contract ready; activation blocked by DBA rollout`
- authorization: 기존 MI 수급모델 실행과 같은 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`로 확정했다. 지정은 사용자·회사 소유 범위로 제한하지 않는다.
- schema/ERD blocker: generated `actv_base_yn`, legacy current seed, unique DDL의 DBA 검토 및 application보다 선행 배포 필요. 기존 application·운영 적재 SQL·배치에 MI Base writer는 없으며 DDL 실행 구간의 임시 MI Base 변경만 금지한다.
- OpenAPI readiness: 변경 `/cases`의 `miBase`/`cases`/`totalCount`/검색 의미, 상세와 status/result의 active `MI_BASE` 조회, 상세 `execType`, 지정 endpoint의 path 제약, side effect, 멱등성, 대표 오류 설명은 확정했다. generated OpenAPI 확인은 구현 후 필요하다.
- implementation handoff: 목록 DTO/query, 상세와 status/result의 `INDEPENDENT | MI_BASE` read 범위, 상세 `execType`, mutation의 `INDEPENDENT` 제한, 지정 API의 role/condition과 공통 적격성 검증은 구현 가능하다. 지정 API 활성화 전에 canonical DDL의 generated current 단건 guard rollout을 완료해야 한다.

## 10. Change Log

- 2026-09-03: 최신 MI Base 1건 우선 목록, `execType` 응답, `caseId` 기준 input 대상 식별, shared permission을 확정했다.
- 2026-09-03: ZIP 전달 확정을 철회했다. DRM 검토와 FE 협의 전까지 input 제공 endpoint와 응답 형식을 open question으로 둔다.
- 2026-09-03: 원본 파일을 전달하지 않는 구조화 JSON A~C안을 우선 검토한다. DRM 비적용 여부와 JSON fidelity는 미확정이다.
- 2026-09-04: 기존 `/cases` 변경안을 철회했다. 전용 실행 Case 목록 API와 신규 DTO를 분리하고 `/execution-cases`를 endpoint 우선안으로 기록했다.
- 2026-09-04: UI가 MI Base 선택 드롭다운임을 확인했다. endpoint를 `/execution-options`로 확정하고 검색·상세 목록 필드를 제거한 `miBase + recentExecutions` 최소 응답으로 변경했다.
- 2026-09-04: 완료 독립실행을 현재 MI Base로 교체하는 `PUT /cases/{caseId}/mi-base` 계약을 추가했다. `case_id` 최신값 선택 규칙을 단일 active MI Base 규칙으로 바꾸고, 동시성 보장을 위한 `actv_base_yn` unique guard와 이후 실행 input source 전환을 설계에 포함했다.
- 2026-09-04: 과거 MI Base 보존 요구 가능성을 반영해 A(이전 type 복귀)와 B(과거 MI_BASE 유지 + explicit current marker)를 decision gate로 되돌렸다. 과거 base를 구분해야 한다면 B를 권고하며, current를 `MAX(case_id)`/일반 `mod_dtm`으로 계산하는 방식은 제외했다.
- 2026-09-04: 최신 기획 화면을 반영해 별도 `/execution-options` 제안을 철회하고 기존 `GET /cases`에 `miBase`를 추가했다. `cases`는 current 제외 실행 건, `totalCount`는 두 영역의 표시 합계이며 검색은 pinned `miBase`를 제외한 `cases`에만 적용한다.
- 2026-09-07: MI Base lifecycle을 current-only 방식으로 확정했다. 새 지정 시 이전 MI Base는 `INDEPENDENT`로 복귀하고, `actv_base_yn`은 `exec_type_cd`와 `del_yn`에서 파생되는 STORED generated column으로 고정한다. 과거 `MI_BASE` 보존과 explicit marker B안은 채택하지 않는다.
- 2026-09-07: MI Base 지정 권한을 기존 MI 수급모델 실행과 동일한 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`로 확정하고 대상 회사 검증을 추가했다. 이 중 대상 회사 검증은 2026-09-10 업무 검토로 superseded됐다.
- 2026-09-07: 목록의 current `MI_BASE`를 기존 `GET /cases/{caseId}`로 상세 조회할 수 있도록 확정했다. 상세 DTO에 non-null `execType`을 추가하고, 수정·삭제·실행 및 상태·결과 API의 범위는 확대하지 않았다.
- 2026-09-07: 구현을 ① MI Base 지정+상세 조회, ② 중간파일 다운로드, ③ 그 외 후속 고도화로 분리했다. Task 1은 input 제공 open question과 분리해 구현 가능하며 Task 2는 별도 다운로드 계약, Task 3은 API 단위 재분할 후 진행한다.
- 2026-09-10: 사용자 검토에서 대상 case의 active SKGAS 회사 소유 검증이 실제 업무 요구가 아닌 과잉 해석임을 확인했다. 지정은 소유자·소유 회사로 제한하지 않고 호출자 role/condition과 case 공통 적격성만 검증하도록 정정했다.
- 2026-09-10: 미확정 Input 제공 계약을 `mi-fundamental-model-input-provision-feature-spec.md`로 분리하고 이 문서를 MI Base 목록·상세·지정 계약으로 축소했다.
- 2026-09-10: `COMPLETED` INDEPENDENT는 재사용하지 않고 새 정상 실행마다 새 case를 생성하는 정책을 반영했다. 같은 case의 `/runs` 재호출은 모델 호출이 `FAILED`일 때만 허용하므로 지정-vs-run 경쟁 요구를 제거했다.
- 2026-09-14: MI Base 상세 화면의 결과 탭 제공을 확정해 status/Volumes filter-options/Volumes/Trade Flows/Price read를 active `MI_BASE`에 열었다. MI Base는 읽기 전용으로 유지해 FE가 수정·삭제·재실행 action을 노출·호출하지 않고 backend도 mutation을 계속 차단한다. current 교체는 지정 API로만 수행하며 별도 지정 해제 API는 만들지 않는다.
