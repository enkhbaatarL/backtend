# MI Scenario Model Execution Workflow Design

- domain: gas
- status: draft
- owner: platform BE
- updated at: 2026-06-09
- role: scenario 상세 화면의 `MI 수급모델 실행` workflow에 대해 backend aggregate, persistence 책임, latest status/source-of-truth 규칙을 정리하는 설계 문서

> ## 변경 메모 (2026-06-09)
>
> `MI 수급모델 실행` 의 입력 모델이 변경되었다.
>
> - 기존: 팝업에서 baseline parameter table을 조회한 뒤 사용자가 값을 수정해 그 `tables[]` snapshot을 실행 접수 body로 제출.
> - 변경: 팝업 parameter 조회는 read-only 참조로 유지하고, 실행 접수는 parameter 수정이 아니라 **입력 파일 3개 업로드**로 한다.
> - 실행 접수 시 파일을 storage(S3)에 올리고 execution을 `PENDING`(실행 대기) 상태로 생성한다.
> - 이에 따라 execution row가 보관하던 `tables[] JSON snapshot` 은 업로드 파일 참조 집합으로 바뀌고, 진입 상태 `QUEUED` 는 `PENDING` 으로 대체된다.
>
> 아래 본문은 이 변경을 반영한다. parameter 복원(latest execution snapshot을 다시 보여주는) 규칙은 더 이상 적용되지 않으며, parameter 조회는 baseline 참조 표시로 단순화된다.

> 빠른 진입점:
> - API contract는 [mi-scenario-workbench-backend-feature-spec.md](/Users/hoduhomil/source/mip/backend/docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md) 의 `F4` 를 따른다.
> - event 기반 비동기 request aggregate 패턴은 [mi-scenario-generation-request-workflow-design.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-scenario-generation-request-workflow-design.md) 를 참고한다.
> - 실행 history, timeout scheduler 패턴은 [scenario-case-execution-workflow-design-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md) 를 참고한다.
> - platform 저장 책임의 상위 초안은 [mi-scenario-workbench-platform-db-ownership-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-scenario-workbench-platform-db-ownership-draft.md) 를 참고한다.

## 문서 목적

- `Scenario MI 수급모델 실행` slice의 canonical aggregate 구조를 정한다.
- `scenario baseline parameter`(팝업 참조 조회)와 `execution 입력 파일`(실행 접수)이 각각 어떤 read/write path의 source-of-truth인지 정한다.
- callback, timeout scheduler, scenario 상세조회의 책임 경계를 분리한다.
- 이 문서는 OpenAPI field contract를 대체하지 않으며, final physical DDL rollout은 별도 artifact에서 다룬다.

## 문서 경계

- 이 문서는 aggregate, 상태 전이, query responsibility, persistence draft를 정의한다.
- 이 문서는 외부 모델 API payload 상세와 상세 결과 데이터의 물리 스키마를 확정하지 않는다.
- 이 문서는 scenario 상세조회 결과 데이터 자체를 어떤 테이블에 저장할지 최종 결정하지 않는다.
- 이 문서는 `Scenario` aggregate 자체의 전체 컬럼 구조를 재정의하지 않는다.

## 현재 요구 재정리

현재 `F4` 요구를 persistence 관점에서 다시 쓰면 다음과 같다.

1. 사용자는 scenario 상세 화면에서 `MI 수급모델 실행` 팝업을 연다.
2. 팝업은 scenario에 저장된 baseline parameter를 read-only 참조로 보여준다.
3. 사용자는 parameter를 수정하는 대신 입력 파일 3개를 업로드해 실행을 접수한다.
4. 실행 접수 시 파일을 storage(S3)에 올리고 execution을 `PENDING`(실행 대기) 상태로 생성한다.
5. 같은 scenario에 active execution이 있으면 새 실행을 막아야 한다.
6. callback은 실행 상태만 terminal로 전이하고, scenario 상세조회용 결과 데이터 생성 책임은 가지지 않는다.
7. timeout은 별도 scheduler 또는 background 정리 작업이 `FAILED/TIMEOUT` 으로 정리해야 한다.

이 요구를 만족하려면 `Scenario` 와 별도로 `Scenario Model Execution` aggregate가 필요하다.

## Recommended Structure

### 구조 요약

- `Scenario baseline parameter` 는 scenario 쪽 read/write 경계에 둔다.
- `Scenario Model Execution` 은 별도 execution history aggregate로 둔다.
- execution persistence는 1차 기준으로 `single history table` 을 기본안으로 둔다.
- latest execution 상태는 별도 master/cache table 없이 execution history latest projection으로 계산한다.
- execution input은 parameter snapshot이 아니라 업로드 입력 파일 3개의 참조 집합으로 보관한다. 파일 자체는 storage(S3)에 두고 execution은 파일 참조만 갖는다.
- callback은 execution 상태 전이만 담당하고, 상세조회용 결과 데이터는 execution table 바깥 read source에 둔다.

### 왜 single history table인가

현재 `F4`는 event 기반 generation request처럼 source event mapping child를 필요로 하지 않는다. 입력 파일은 3개로 고정이라 execution row에 종속된 단순 파일 참조 집합이면 충분하다. 따라서 1차 rollout에서는 아래 이유로 execution history table 1개(+입력 파일 참조)가 가장 단순하다.

- scenario 자체가 이미 parent aggregate다.
- 입력 파일은 execution당 3개로 고정된 종속 자원이다.
- latest 상태는 `scenario_id` 기준 latest execution projection으로 계산 가능하다.
- callback과 timeout이 update해야 할 row는 항상 execution 1건이다.
- 별도 master/cache row를 두면 write fan-out과 compare-and-set 부담이 늘어난다.

## Aggregate Structure

### A. `Scenario` 쪽 baseline parameter

- 의미: 팝업에서 read-only 참조로 보여주는 canonical baseline parameter
- 책임:
  - 최초 제안 parameter 저장
  - scenario 생성 시 baseline 초기값 보관
  - `GET .../model-execution-parameters` 의 source-of-truth 역할

설계 메모:

- baseline parameter는 `Scenario` aggregate에 속한다.
- 물리적으로 `tmm_scenariom` 직컬럼인지, 별도 scenario child table인지, JSON 컬럼인지 여부는 아직 미정이다.
- 입력 모델이 파일 업로드로 바뀌면서 parameter 조회는 execution과 분리된다. parameter 조회는 항상 baseline 참조를 반환하고, execution input(파일)과 lifecycle을 공유하지 않는다.
- scenario 수정 시 baseline parameter를 함께 갱신할지는 현재 문서 범위에서 확정하지 않는다.

### B. `Scenario Model Execution`

- 의미: scenario 기준 한 번의 `MI 수급모델 실행` 접수와 상태 전이를 대표하는 execution history row
- 책임:
  - 실행 식별자 관리
  - 상태 관리
  - 외부 추적키 관리
  - 입력 파일 3개 참조 보관
  - timeout/failure reason 보관
  - latest execution projection의 source row 역할

권장 candidate physical name:

- `tmd_scnrmdlexech`

정리하면:

- `scenario_id : execution = 1:N`
- latest execution은 `requested_at desc, scnr_mdl_exec_id desc` 기준 1건으로 계산한다.
- active execution은 scenario당 최대 1건만 허용한다.

## Source-Of-Truth Rule

### 1. 팝업 parameter 조회

`GET /scenarios/{scenarioId}/model-execution-parameters` 의 source-of-truth는 scenario baseline parameter다.

- 입력 모델이 파일 업로드로 바뀌면서 execution은 더 이상 parameter snapshot을 보관하지 않는다.
- 따라서 parameter 조회는 execution 이력과 무관하게 항상 baseline 참조를 반환한다.
- parameter 조회는 계산형 API가 아니라 팝업에서 참고용으로 보여주는 read-only reference API다.
- "마지막으로 제출한 parameter 복원" 규칙은 더 이상 적용되지 않는다. 사용자가 다시 제출하는 입력은 parameter 값이 아니라 새로 업로드하는 파일 3개다.

### 2. 상태 조회

`GET /scenarios/{scenarioId}/model-executions/{executionId}` 의 source-of-truth는 execution history row 자체다.

### 3. scenario 상세조회

- execution table은 상세조회 결과 데이터의 source-of-truth가 아니다.
- scenario 상세조회는 별도 결과 저장 경로에서 데이터를 읽는다.
- callback 전 외부 모델 API가 결과 데이터를 먼저 준비하고, callback은 execution 상태만 갱신한다.

## Latest Execution Projection Rule

latest execution projection은 최소 아래 규칙으로 계산한다.

- 기준: `requested_at desc`, tie-breaker `scnr_mdl_exec_id desc`
- 목적:
  - 버튼 활성화 판단
  - 상태 badge 표시

projection에서 사용하는 값:

- `latest status`
- `latest failure reason`
- `latest executionId`
- `latest input files`

### 화면 해석 규칙

| latest 내부 상태 | 화면 상태 | 버튼 활성화 |
|---|---|---|
| 없음 | `READY` | 가능 |
| `PENDING` | `EXECUTING` | 불가 |
| `RUNNING` | `EXECUTING` | 불가 |
| `SUCCEEDED` | `SUCCEEDED` | 가능 |
| `FAILED` | `FAILED` | 가능 |

## 상태 모델

execution row는 최소 아래 상태를 권장한다.

| 상태 | 의미 | terminal 여부 |
|---|---|---|
| `PENDING` | 입력 파일 업로드 완료, 실행 대기(외부 실행 시작 전) | N |
| `RUNNING` | 외부 모델 API 처리 중 | N |
| `SUCCEEDED` | 실행 성공 | Y |
| `FAILED` | 실행 실패 또는 timeout | Y |

### 실패 사유

| 코드 | 의미 |
|---|---|
| `TIMEOUT` | `RUNNING` 상태에서 callback 미도착으로 scheduler timeout 처리 |
| `DISPATCH_TIMEOUT` | `PENDING` 상태에서 외부 dispatch 시작/전환이 장시간 지연되어 내부 정리 처리 |
| `EXTERNAL_ERROR` | 외부 API 실패 응답 또는 callback 실패 |
| `UNKNOWN` | 분류되지 않은 실패 |

## 중복 실행 / 재실행 규칙

- scenario 기준 active execution은 최대 1건이다.
- latest execution이 `PENDING` 또는 `RUNNING` 이면 새 접수를 거절한다.
- latest execution이 `SUCCEEDED` 또는 `FAILED` 이면 새 접수를 허용한다.
- 재접수는 기존 execution row update가 아니라 새 입력 파일 3개 업로드 + 새 execution row insert다.
- parameter 조회는 execution 상태와 무관하게 항상 baseline 참조를 반환한다.

## Callback And Detail Read Boundary

- 외부 모델 API는 callback 전에 scenario 상세조회용 결과 데이터를 먼저 생성한다.
- callback API는 `Scenario Model Execution` 상태만 terminal 상태로 전이한다.
- callback 처리 안에서 결과 데이터 materialization을 수행하지 않는다.
- `executionId` 는 Platform/backend가 발급한 canonical execution identifier다.
- `externalRunId` 는 외부 모델 API가 별도 run key를 제공하는 경우에만 dispatch accept response에서 받아 저장하는 외부 추적키다.
- callback lookup은 path의 `executionId` 를 기준으로 한다.
- `externalRunId` 가 제공된 경우 body의 `externalRunId` 는 저장된 `ext_run_id` 와 correlation / audit / mismatch 검증 용도로 사용한다.
- 외부 모델 API가 별도 run key를 제공하지 않으면 `ext_run_id` 는 null로 두고, `executionId` + callback 인증/서명 + terminal payload idempotency로 정합성을 보장한다.
- 이미 terminal 상태인 동일 terminal payload 재전송은 `200 OK` no-op으로 처리한다.
- scheduler timeout 이후 도착한 late callback도 식별자가 일치하면 `200 OK` no-op으로 처리하되, execution 상태는 되돌리지 않는다.
- `externalRunId` 제공 모드에서 mismatch가 발생하거나 기존 terminal 상태와 상이한 terminal payload가 들어오면 상태를 바꾸지 않고 conflict/error로 처리한다.

## Timeout Scheduler Rule

- scheduler 또는 background 정리 작업이 주기적으로 stale execution을 찾는다.
- `RUNNING` timeout 기본안:
  - 대상 상태: `RUNNING`
  - 조건: `started_at < now - timeout_threshold`
  - 전이: `FAILED`
  - 실패 사유: `TIMEOUT`
- `PENDING` stale 정리는 같은 `TIMEOUT` 으로 묶지 않는다.
  - 대상 상태: `PENDING`
  - 조건: `req_dtm < now - dispatch_timeout_threshold`
  - 전이: `FAILED`
  - 실패 사유: `DISPATCH_TIMEOUT`
- 외부 dispatch 전 내부 지연과 외부 실행 중 timeout은 운영 의미가 다르므로 failure reason을 분리한다.

## Persistence Draft

### candidate table: `tmd_scnrmdlexech`

권장 컬럼 범주:

| 범주 | 예시 컬럼 | 설명 |
|---|---|---|
| PK | `scnr_mdl_exec_id` | execution 고유 ID |
| FK | `scenario_id` | parent scenario |
| 상태 | `exec_stat_cd` | `PENDING/RUNNING/SUCCEEDED/FAILED` |
| 실패 | `fail_rsn_cd`, `err_msg_cntt` | 실패 사유/메시지 |
| 외부 추적키 | `ext_run_id` | 외부 모델 API가 별도 run key를 제공할 때 저장하는 nullable dispatch/callback correlation key |
| 시각 | `req_dtm`, `exec_bgn_dtm`, `exec_end_dtm` | 요청/시작/종료 시각 |
| 입력 파일 | 입력 파일 3개 참조 | execution에 종속된 업로드 파일 참조. 파일 자체는 storage(S3)에, execution은 file 참조(file relationship)만 보관 |
| active helper | `actv_exec_token` | active execution unique 보조용 nullable 컬럼 |
| audit | `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn` | 공통 audit |

설계 메모:

- 입력 파일 참조는 운영 추적과 재실행 입력 식별 목적이다. parameter snapshot JSON 컬럼은 더 이상 두지 않는다.
- 입력 파일 참조를 execution row 직컬럼(예: `inp_file1_no`/`inp_file2_no`/`inp_file3_no`)으로 둘지, 별도 file relationship/child table로 둘지는 물리 DDL 단계에서 확정한다. 기존 fundamental model case 업로드와 같은 file relationship 방식을 우선 후보로 둔다.
- `actv_exec_token` 은 active execution일 때만 상수값을 두고, terminal 상태 전이 시 `NULL` 로 해제한다.
- `fail_rsn_cd` 는 DB 컬럼과 API `failureReason` 양쪽에 노출한다. `err_msg_cntt`/`errorMessage` 는 사람이 읽는 상세 메시지이고, `fail_rsn_cd`/`failureReason` 은 시스템 분기용 코드다.

## 권장 인덱스 / 제약 방향

- PK: `scnr_mdl_exec_id`
- Conditional UK: `ext_run_id`
  - 전제: 외부 모델 API가 execution당 안정적으로 1:1 `externalRunId` 를 발급하고 callback 재전송 시 같은 값을 유지할 때만 적용한다.
  - `ext_run_id` 는 nullable이며 primary lookup key가 아니라 correlation / audit 검증용 보조키다.
- Index: `(scenario_id, req_dtm DESC, scnr_mdl_exec_id DESC)` for latest execution projection
- Index: `(exec_stat_cd, exec_bgn_dtm)` for timeout scheduler
- Unique helper: `(scenario_id, actv_exec_token)`

### active helper token 확정

MySQL/manual DBA 환경에서는 partial unique index가 없어 "active row만 scenario당 1건" 제약을 단순하게 걸기 어렵다. 따라서 아래 같은 nullable helper column 방식이 현실적이다.

- active row: `actv_exec_token = 'Y'`
- terminal row: `actv_exec_token = NULL`
- unique `(scenario_id, actv_exec_token)`

이렇게 하면 terminal row는 여러 건 허용되고, active row만 1건으로 제한할 수 있다.

DB unique는 동시성의 최후 방어선이다. 서비스는 insert 전 latest execution을 조회해 `PENDING/RUNNING` 이면 `REQ_CONFLICT` 로 거절하고, unique 충돌도 같은 의미의 conflict로 매핑한다.

## Query Responsibility

### A. 팝업 parameter 조회

1. scenario baseline parameter 조회
2. baseline 참조 반환 (execution 이력과 무관)

### B. 실행 접수

1. latest execution 조회
2. `PENDING/RUNNING` 이면 거절
3. 입력 파일 3개를 검증하고 storage(S3)에 업로드
4. Platform이 `scnr_mdl_exec_id(executionId)` 를 발급하고 새 execution row를 `PENDING` 으로 insert
5. 업로드 파일 참조, `actv_exec_token='Y'` 저장
6. 업로드 실패 시 이미 올라간 파일을 보상(삭제)하고 접수를 실패 처리
7. 외부 모델 API에 `executionId`, callback URL, 입력 파일 참조를 dispatch
8. 외부 dispatch accept response에 `externalRunId` 가 있으면 `ext_run_id` 에 저장하고 `RUNNING` 으로 전이
   - 외부 모델 API가 별도 run key를 제공하지 않으면 `ext_run_id = NULL` 로 두고 `RUNNING` 으로 전이
9. 외부 dispatch 실패 시 `FAILED/EXTERNAL_ERROR` 로 전이하고 `actv_exec_token = NULL`

### C. 상태 조회

1. `scnr_mdl_exec_id` 단건 조회
2. 상태/시각/실패 사유 반환

### D. callback 처리

1. `scnr_mdl_exec_id` 단건 조회
2. `ext_run_id` 가 있으면 body의 `externalRunId` 와 저장된 `ext_run_id` 정합성 검증
   - `ext_run_id` 가 없으면 `externalRunId` 검증은 skip하고 callback 인증/서명과 `executionId` 로 정합성 검증
3. non-terminal이면 `SUCCEEDED` 또는 `FAILED` 로 전이
4. 이미 terminal이고 동일 terminal payload이면 `200 OK` no-op 처리
5. 이미 terminal이고 상이 terminal payload이거나 `externalRunId` 제공 모드에서 mismatch이면 conflict/error 처리
6. callback 처리 안에서는 결과 데이터 materialization 또는 readiness 검증을 수행하지 않음
7. terminal 전이 시 `actv_exec_token = NULL`

### E. timeout 정리

1. `RUNNING` + threshold 초과 row 조회
2. `FAILED/TIMEOUT` 전이
3. `actv_exec_token = NULL`

### F. dispatch stale 정리

1. `PENDING` + dispatch threshold 초과 row 조회
2. `FAILED/DISPATCH_TIMEOUT` 전이
3. `actv_exec_token = NULL`

## 왜 master/history 2-table을 지금은 쓰지 않는가

과거 `scenario-case-execution` draft는 upload case master + execution history 구조가 적합했다. 하지만 `F4`는 다음 점에서 다르다.

- 별도 upload master aggregate가 없다.
- latest execution cache를 위한 별도 master row가 없어도 query 비용이 작다.
- 입력 파일은 execution당 3개로 고정된 종속 자원이라 별도 master aggregate가 필요 없다.

따라서 1차 rollout에서는 single history table이 더 단순하고 reviewable하다.  
다만 향후 아래 요구가 생기면 master/cache table을 다시 검토할 수 있다.

- scenario 목록/상세에서 latest execution status를 대량 projection해야 함
- active execution lock을 query만으로 안정적으로 처리하기 어려움
- execution 결과 요약 캐시를 execution row와 분리해야 함

## Open Questions

- scenario baseline parameter의 physical owner는 백엔드 임의 결정 대상이 아니며, 타 팀 공유 자료를 받은 뒤 확인한다.
- scenario 수정이 baseline parameter 갱신을 의미하는지 여부
- timeout 임계 시간과 scheduler 주기
- 입력 파일 참조를 execution row 직컬럼으로 둘지 file relationship/child table로 둘지, 그리고 파일 형식/크기/개수 검증 규칙(현재 3개 고정 전제)
- scenario 상세조회 결과 데이터의 physical owner와 저장 시점은 외부 API 책임으로 두며, `ScenarioModelExecution` DDL에는 결과 readiness/materialization 컬럼을 추가하지 않는다.

## Recommended Next Step

1. 이 문서 기준으로 `erd-change-review` 를 수행한다.
2. execution table DDL draft를 별도 canonical DDL artifact로 분리한다.
3. feature spec의 `Data / Model Impact` 와 `ERD Review Handoff` 는 이 구조를 기준으로 정리한다.
