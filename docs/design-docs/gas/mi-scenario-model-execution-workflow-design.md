# MI Scenario Model Execution Workflow Design

- domain: gas
- status: implemented (real DB rollout complete; scenario lifecycle cancellation/reconciliation pending)
- owner: platform BE
- updated at: 2026-10-07
- role: 시나리오 그룹 상세 화면의 `MI 수급모델 실행` workflow에 대해 backend aggregate, persistence 책임, latest status/source-of-truth 규칙을 정리하는 설계 문서
- canonical for: MI 수급모델 실행의 scenario-linked lifecycle, Kubernetes Job orchestration, latest execution 규칙
- upstream sources: `mi-scenario-workbench-backend-feature-spec.md`, `scenario-case-execution-workflow-design-draft.md`
- dependencies: scheduler-enabled dedicated deployment, Kubernetes jobs get/list/delete RBAC
- last sync date: 2026-10-07

## 실행 입력 4파일 전환 — 2026-10-07 사용자 확정

- Scenario의 `PARAMETER_PROPOSAL`과 `FILE_UPLOAD` 모두 4파일 실행으로 전환한다. 파일 set과 OUTPUT 제안 제외·원본 보존 business rule은 `mi-model-parameter-proposal-design.md` §1A를 따른다.
- 같은 실행의 네 파일을 기존 `SCNRCASE + case_id`로 연결하고 `ScenarioExecutionRegistrar`가 네 input path를 검증·Job에 전달해야 한다. Case/Run lifecycle과 기존 transaction/보상 삭제 책임은 유지한다.
- 2026-10-13 사용자 결정: 전환 전에 3파일로 실행한 `SCENARIO` Case는 4파일 cutover 정리에서 연결 run history·execution-preparation·해당 시나리오의 active 가격전망해석과 함께 soft delete한다. 정리 후 해당 시나리오는 남은 non-deleted 실행 기준으로 보이며, 남은 실행이 없으면 미실행 상태로 다시 실행할 수 있다. 대상 판별·pre-check·보존은 `mi-model-input-template-catalog-domain-model-design.md` 규칙 36~38을 따른다.
- 현재 코드는 3파일이며 추가 구현 대기다. 아래 기존 3파일 서술은 종전 흐름의 이력이고 4파일 목표의 근거로 사용하지 않는다. multipart API 계약은 `mi-scenario-workbench-backend-feature-spec.md` F4, JSON 계약은 parameter proposal feature spec을 따른다.

> ## 변경 메모 (2026-06-09) — SUPERSEDED BY 2026-06-10 통합 메모
>
> `MI 수급모델 실행` 의 입력 모델이 변경되었다.
>
> - 기존: 팝업에서 baseline parameter table을 조회한 뒤 사용자가 값을 수정해 그 `tables[]` snapshot을 실행 접수 body로 제출.
> - 변경: 팝업 parameter 조회는 read-only 참조로 유지하고, 실행 접수는 parameter 수정이 아니라 **입력 파일 3개 업로드**로 한다.
> - 실행 접수 시 파일을 storage(S3)에 올리고 execution을 `PENDING`(실행 대기) 상태로 생성한다.
> - 이에 따라 execution row가 보관하던 `tables[] JSON snapshot` 은 업로드 파일 참조 집합으로 바뀌고, 진입 상태 `QUEUED` 는 `PENDING` 으로 대체된다.
>
> 이 메모의 "파일 업로드 입력 모델" 결정은 유지하지만, `PENDING` 진입 상태와 별도 execution 저장소 전제는 아래 2026-06-10 통합 메모로 superseded 되었다. 현재 active 흐름은 통합 case/run row를 접수 즉시 `RUNNING` 으로 생성한다.

> ## 변경 메모 (2026-06-10) — 팝업 parameter 조회 제거 (위 2026-06-09 메모의 "baseline 참조 표시" 부분을 supersede)
>
> 기획 확정으로 `MI 수급모델 실행` 팝업의 입력 모델이 한 번 더 단순화되었다.
>
> - 팝업은 시나리오 그룹 상세 화면에서 열리며, **시나리오 선택 드롭다운**과 **입력 파일 업로드 UI**만 제공한다. 드롭다운에는 해당 시나리오 그룹에 속한 시나리오만 노출한다(2026-06-10 확정).
> - 사용자는 시나리오 1개를 선택하고 `Demand`/`Supply`/`Trade` 가 각각 포함된 파일 3개를 모두 업로드해야 실행을 접수할 수 있다(Excel, 파일당 최대 5MB).
> - baseline parameter 참조 표시는 현재 단계에서 두지 않는다. parameter 조회 API(`GET .../model-execution-parameters`)는 현재 단계 scope에서 제외한다(설계만 존재·미구현 상태로 동결).
> - 본문 중 baseline parameter 관련 절("A. `Scenario` 쪽 baseline parameter", "Source-Of-Truth Rule §1", "Query Responsibility §A")은 본 메모로 supersede 되며, 의사결정 히스토리로만 보존한다.

> ## 변경 메모 (2026-06-10) — execution 저장소 통합 (이 문서의 별도 테이블 권고를 supersede)
>
> `MI 수급모델 실행` 의 execution persistence 를 별도 aggregate(`tmd_scnrmdlexech`)로 두지 않고, MI Fundamental Model 이 이미 쓰는 `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` 를 재사용한다. 두 실행 흐름은 "시나리오 식별자" 외에는 동일하기 때문이다.
>
> - `tmd_mdl_scnrcasem` 에 nullable `scnr_id` 한 컬럼만 추가한다. Fundamental row 는 `scnr_id = NULL`, 수급모델 row 는 scenario 식별자를 채운다.
> - mapping 은 `scenario : case = 1:N`. 수급모델은 실행마다 새 `case_id` 가 발행되고 `scnr_id` 는 동일하게 유지된다. → 입력 파일이 `tcm_filel` 의 `SCNRCASE + case_id` relation 으로 실행별 자연 분리되고, 결과는 fundamental 과 동일하게 `run_seq` 기준으로 적재된다.
> - 아래 "B. `Scenario Model Execution`" 의 후보 테이블 `tmd_scnrmdlexech` 와 "Persistence Draft" 의 컬럼 세트(`ext_run_id`, `req_dtm`, `actv_exec_token`, 입력 파일 직컬럼 등)는 **본 메모로 supersede 된다**. 사유:
>   - `ext_run_id` (외부 추적키): fundamental 과 동일하게 backend 발급 `run_id` path + terminal payload idempotency 로 callback 정합성을 보장하므로 불필요.
>   - `req_dtm` (요청 시각): `PENDING` 단계를 두지 않고 접수 즉시 `RUNNING` 으로 가므로 `crt_dtm` = 실행 시작이 되어 별도 컬럼 불필요. 상태 모델에서 `PENDING` / `DISPATCH_TIMEOUT` 구분은 제거하고 `RUNNING` + 단일 `TIMEOUT` 으로 단순화한다.
>   - `actv_exec_token` (active 1건 helper): 별도 token 컬럼 대신 통합 테이블의 `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` generated column + `tmd_mdl_scnrcasem_uq01 UNIQUE (actv_scnr_id)` 으로 동일한 효과를 얻는다(2026-06-10 ERD 리뷰 P1 반영, 2026-06-10 수동 `actv_yn CHAR(1)` 에서 generated column 으로 변경 — helper 를 DB 가 `case_stat_cd`/`scnr_id` 에서 자동 파생하게 해 drift 제거). helper 값(RUNNING 이면 `scnr_id`, 그 외 `NULL`)은 애플리케이션 동기화 없이 보장된다. 실행 접수는 새 case row INSERT 이므로 동일 scenario active row 가 이미 있으면 `actv_scnr_id` 중복으로 unique 충돌이 나고 이를 409 conflict 로 매핑한다. `scnr_id` 기준 `RUNNING` 존재 여부 확인(`tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` 사용)은 친절한 응답을 위한 1차 가드이고, unique 가 DB 최후 방어선이다.
>   - 입력 파일 3개: 별도 직컬럼 없이 `tcm_filel` (`SCNRCASE + case_id`) relation 을 그대로 재사용한다.
> - 상태 값은 통합 테이블 기준 `RUNNING`/`COMPLETED`/`FAILED` 를 사용한다. 본문 하단의 `PENDING`/`SUCCEEDED`/`DISPATCH_TIMEOUT` 표기는 별도 테이블 안의 히스토리다.
> - active-run 가드 로직은 nextgen submit 흐름(현재 `ScenarioModelExecutionServiceImpl` 전부 stub)이 구현될 때 함께 wiring 한다. 스키마(컬럼·인덱스·unique)와 가드 규칙은 본 문서/ERD-DDL 초안에 확정해 둔다.
> - 물리 스키마 단일 소스는 [scenario-case-execution-erd-ddl-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md) 다(2026-06-10 `scnr_id` re-introduction / `actv_scnr_id` generated column helper unique / `scnrcase` rename 결정 로그 참고). `scnr_id` 의 참조 대상 실물은 `tmm_mi_scenario_m.scenario_id INT UNSIGNED` 다(구 명칭 `tmm_scenariom` superseded).
> - 본 메모 기준으로 아래 본문 중 별도 테이블 전제 섹션 — "상태 모델", "중복 실행 / 재실행 규칙"의 `PENDING` 전제, "Timeout Scheduler Rule"의 `PENDING`/`DISPATCH_TIMEOUT` 정리, "Persistence Draft", "권장 인덱스 / 제약 방향", "Historical active helper token candidate" 의 token helper, "Query Responsibility"의 `scnr_mdl_exec_id`/`ext_run_id`/`actv_exec_token` 서술 — 은 의사결정 히스토리로만 보존한다.

> 빠른 진입점:
> - API contract는 [mi-scenario-workbench-backend-feature-spec.md](/Users/hoduhomil/source/mip/backend/docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md) 의 `F4` 를 따른다.
> - event 기반 비동기 request aggregate 패턴은 [mi-scenario-generation-request-workflow-design.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-scenario-generation-request-workflow-design.md) 를 참고한다.
> - 실행 history, timeout scheduler 패턴은 [scenario-case-execution-workflow-design-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md) 를 참고한다.
> - platform 저장 책임의 상위 초안은 [mi-scenario-workbench-platform-db-ownership-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-scenario-workbench-platform-db-ownership-draft.md) 를 참고한다.

## 문서 목적

- `Scenario MI 수급모델 실행` slice의 canonical aggregate 구조를 정한다.
- `execution 입력 파일`(실행 접수)의 read/write path source-of-truth를 정한다. baseline parameter 팝업 조회는 현재 단계 scope에서 제외되었다(2026-06-10 메모).
- callback, timeout scheduler, scenario 상세조회의 책임 경계를 분리한다.
- 이 문서는 OpenAPI field contract를 대체하지 않으며, final physical DDL rollout은 별도 artifact에서 다룬다.

## 문서 경계

- 이 문서는 aggregate, 상태 전이, query responsibility, persistence draft를 정의한다.
- 이 문서는 외부 모델 API payload 상세와 상세 결과 데이터의 물리 스키마를 확정하지 않는다.
- 이 문서는 scenario 상세조회 결과 데이터 자체를 어떤 테이블에 저장할지 최종 결정하지 않는다.
- 이 문서는 `Scenario` aggregate 자체의 전체 컬럼 구조를 재정의하지 않는다.

## 현재 요구 재정리

현재 `F4` 요구를 persistence 관점에서 다시 쓰면 다음과 같다.

1. 사용자는 시나리오 그룹 상세 화면에서 `MI 수급모델 실행` 팝업을 연다.
2. 팝업은 시나리오 선택 드롭다운과 파일 업로드 UI를 보여준다. 드롭다운에는 해당 시나리오 그룹에 속한 시나리오만 노출하며, parameter 조회/표시는 없다.
3. 사용자는 시나리오 1개를 선택하고 입력 파일 3개를 업로드해 실행을 접수한다.
4. 실행 접수 시 파일을 storage(S3)에 올리고 execution(case)을 생성한다. 접수 즉시 `RUNNING` 으로 간다(2026-06-10 저장소 통합 메모: `PENDING` 단계 제거).
5. 같은 scenario에 active execution이 있으면 새 실행을 막아야 한다.
6. callback은 실행 상태만 terminal로 전이하고, scenario 상세조회용 결과 데이터 생성 책임은 가지지 않는다.
7. timeout은 별도 scheduler 또는 background 정리 작업이 `FAILED/TIMEOUT` 으로 정리해야 한다.

이 요구는 별도 `tmd_scnrmdlexech` aggregate를 만들지 않고, 통합 실행 테이블인 `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` 로 처리한다. `MI 수급모델 실행` row는 `tmd_mdl_scnrcasem.scnr_id` 에 선택한 scenario id를 저장하고, MI Fundamental Model row는 `scnr_id = NULL` 로 둔다.

실행유형은 `exec_type_cd`(`INDEPENDENT`/`SCENARIO`/`MI_BASE`) 명시 컬럼으로 구분한다(2026-06-16). `MI 수급모델 실행` row 는 `exec_type_cd = 'SCENARIO'` 로 INSERT 하며 `scnr_id` 가 항상 NOT NULL 이어야 한다 — `tmd_mdl_scnrcasem_ck01 CHECK ((exec_type_cd = 'SCENARIO') = (scnr_id IS NOT NULL))` 가 DB 레벨에서 이 정합을 강제한다. `INDEPENDENT`(독립실행)·`MI_BASE`(MI Base 적재) row 는 `scnr_id = NULL` 이며 본 수급모델 실행 흐름과 무관하다. 컬럼/제약/공통코드 canonical source 는 [scenario-case-execution-erd-ddl-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md) 다.

## Active Design Summary (2026-06-10)

이 섹션이 현재 구현 기준이다. 아래 historical/superseded 섹션과 충돌하면 이 섹션을 따른다.

| 항목 | Active 설계 |
|---|---|
| 실행 저장소 | `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` 재사용 |
| 별도 실행 테이블 | 없음. `tmd_scnrmdlexech` 는 만들지 않는다 |
| 수급모델 식별 | `tmd_mdl_scnrcasem.scnr_id = selected scenario id` |
| Fundamental 식별 | `tmd_mdl_scnrcasem.scnr_id = NULL` |
| 실행 생성 | 수급모델 submit 시 새 case row를 `RUNNING` 으로 INSERT 후 run history row INSERT |
| 입력 파일 | `tcm_filel(file_rel_obj_spt_cd = 'SCNRCASE', file_rel_obj_dstg_no = case_id)` |
| 상태 | `RUNNING` / `COMPLETED` / `FAILED` |
| active 1건 제한 | `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` + `UNIQUE (actv_scnr_id)` |
| latest projection | `WHERE scnr_id = :scnr_id ORDER BY case_id DESC LIMIT 1` |
| 물리 DDL source | [scenario-case-execution-erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md) |

`actv_yn` / `actv_exec_token` / `UNIQUE (scnr_id, actv_yn)` / `UNIQUE (scenario_id, actv_exec_token)` 은 채택안이 아니다. 최종 채택안은 generated column `actv_scnr_id` + `UNIQUE (actv_scnr_id)` 이다.

## Recommended Structure

### 구조 요약

- 팝업 입력은 시나리오 선택 + 입력 파일 3개 업로드다. baseline parameter 조회는 현재 단계 scope에 없다(2026-06-10 메모).
- `MI 수급모델 실행` 은 별도 execution aggregate가 아니라 통합 case/run aggregate로 저장한다.
- master row는 `tmd_mdl_scnrcasem`, execution history row는 `tmd_mdl_scnrcaseh` 다.
- latest execution 상태는 `scnr_id` 기준 최신 case projection으로 계산한다.
- execution input은 parameter snapshot이 아니라 업로드 입력 파일 3개의 `tcm_filel` 참조 집합으로 보관한다.
- callback은 case/run 상태 전이만 담당하고, 상세조회용 결과 데이터는 output result table read source에 둔다.

### 왜 별도 `tmd_scnrmdlexech` 를 만들지 않는가

현재 `F4`는 MI Fundamental Model 실행과 동일하게 "입력 파일 3개 + 비동기 실행 + callback + 결과 table 적재" 구조다. 차이는 scenario 식별자(`scnr_id`) 존재 여부뿐이다. 따라서 별도 실행 테이블을 만들면 동일한 파일/상태/callback/result linkage 규칙을 중복 구현하게 된다.

- `case_id` 로 입력 파일 3개를 자연 분리할 수 있다.
- `run_id` / `run_seq` 로 callback, output file, parsed result table linkage를 fundamental과 공유할 수 있다.
- `scnr_id` 가 `NULL` 이면 fundamental row, 값이 있으면 수급모델 row로 구분된다.
- active 1건 제한은 `actv_scnr_id` generated column unique로 DB가 강제한다.
- 물리 DDL과 JPA/entity mapping의 중복을 피한다.

## Aggregate Structure

### A. `Scenario` 쪽 baseline parameter (superseded)

> **SUPERSEDED (2026-06-10)** — 팝업 parameter 조회가 현재 단계에서 제거되어(상단 "팝업 parameter 조회 제거" 메모) 본 절은 의사결정 히스토리로만 보존한다.

- 의미: 팝업에서 read-only 참조로 보여주는 canonical baseline parameter
- 책임:
  - 최초 제안 parameter 저장
  - scenario 생성 시 baseline 초기값 보관
  - `GET .../model-execution-parameters` 의 source-of-truth 역할

설계 메모:

- baseline parameter는 `Scenario` aggregate에 속한다.
- 물리적으로 `tmm_mi_scenario_m` 직컬럼인지, 별도 scenario child table인지, JSON 컬럼인지 여부는 아직 미정이다.
- 입력 모델이 파일 업로드로 바뀌면서 parameter 조회는 execution과 분리된다. parameter 조회는 항상 baseline 참조를 반환하고, execution input(파일)과 lifecycle을 공유하지 않는다.
- scenario 수정 시 baseline parameter를 함께 갱신할지는 현재 문서 범위에서 확정하지 않는다.

### B. `MI 수급모델 실행` 통합 case/run row

- 의미: 선택한 scenario 기준 한 번의 `MI 수급모델 실행` 접수와 상태 전이를 대표하는 통합 case/run row
- 저장소:
  - `tmd_mdl_scnrcasem`: 실행 접수 단위 master. `scnr_id` 에 선택한 scenario id 저장
  - `tmd_mdl_scnrcaseh`: 실제 모델 실행 history. `run_id` / `run_seq` 보관
  - `tcm_filel`: 입력 파일 3개. `SCNRCASE + case_id` 로 연결
- 책임:
  - 실행 식별자(`case_id`, `run_id`, `run_seq`) 관리
  - 상태 관리(`case_stat_cd`, `exec_stat_cd`)
  - 입력 파일 3개 참조 보관
  - timeout/failure reason 보관
  - latest execution projection의 source row 역할

정리하면:

- `scenario : case = 1:N`
- latest execution은 `scnr_id = :scnr_id ORDER BY case_id DESC LIMIT 1` 기준 1건으로 계산한다.
- active execution은 scenario당 최대 1건만 허용하며, `actv_scnr_id` generated column unique로 강제한다.
- `tmd_scnrmdlexech` 는 만들지 않는다.

## Source-Of-Truth Rule

### 1. 팝업 parameter 조회 (superseded)

> **SUPERSEDED (2026-06-10)** — 팝업 parameter 조회는 현재 단계 scope에서 제외되었다(상단 "팝업 parameter 조회 제거" 메모). `GET .../model-execution-parameters` 는 설계만 존재·미구현 상태로 동결한다. 아래 내용은 의사결정 히스토리로만 보존한다.

`GET /scenarios/{scenarioId}/model-execution-parameters` 의 source-of-truth는 scenario baseline parameter다.

- 입력 모델이 파일 업로드로 바뀌면서 execution은 더 이상 parameter snapshot을 보관하지 않는다.
- 따라서 parameter 조회는 execution 이력과 무관하게 항상 baseline 참조를 반환한다.
- parameter 조회는 계산형 API가 아니라 팝업에서 참고용으로 보여주는 read-only reference API다.
- "마지막으로 제출한 parameter 복원" 규칙은 더 이상 적용되지 않는다. 사용자가 다시 제출하는 입력은 parameter 값이 아니라 새로 업로드하는 파일 3개다.

### 2. 상태 조회

상태 조회의 source-of-truth는 통합 case/run row다.

- scenario 기준 latest 상태: `tmd_mdl_scnrcasem.scnr_id = :scnr_id` 기준 최신 case row
- 실행 단건 상태: `tmd_mdl_scnrcaseh.run_id = :run_id`
- `case_stat_cd` 와 latest `exec_stat_cd` 는 같은 transaction에서 함께 terminal/RUNNING 상태로 맞춘다.

### 3. scenario 상세조회

- execution table은 상세조회 결과 데이터의 source-of-truth가 아니다.
- scenario 상세조회는 결과 테이블 4개에서 `run_seq` 기준으로 데이터를 읽는다(2026-06-10 확정).
  - `tmd_mdl_scnrcasevolrgnl`: Volumes (Global, LNG) region-bucket JSON 결과
  - `tmd_mdl_scnrcasetrdehdrl`: Volumes Trade Flow FROM/TO 헤더 목록
  - `tmd_mdl_scnrcasetrdeflwl`: Volumes Trade Flow 실제 흐름 값
  - `tmd_mdl_scnrcaseprcl`: Price 결과
  - 테이블/키/인덱스/적재 경계는 [mi-fundamental-model-output-result-persistence-design.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md) 를 따른다.
- callback 전 모델 측이 결과 row insert를 먼저 commit하고, callback은 execution 상태만 갱신한다. backend는 `COMPLETED` callback 처리 전 `run_seq` 기준 결과 row 존재를 검증한다.

## Historical / Superseded Notes Below

아래 섹션 중 `HISTORICAL ONLY` 또는 `SUPERSEDED` 로 표시된 내용은 구현 기준이 아니다. 현재 구현 기준은 위 `Active Design Summary` 와 `Query Responsibility` 의 통합 case/run 흐름이다.

## Historical Latest Execution Projection Rule (Superseded)

> **HISTORICAL ONLY (2026-06-10)** — 아래 projection 규칙은 별도 테이블 후보의 히스토리다. 통합 테이블 기준 latest projection 은 `WHERE scnr_id = ? ORDER BY case_id DESC LIMIT 1` (`tmd_mdl_scnrcasem_ix02` 사용)이다.

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

## Historical Status Model (Superseded)

> **HISTORICAL ONLY (2026-06-10)** — 통합 테이블 채택으로 `PENDING` 단계와 `SUCCEEDED` 표기는 제거되었다. 현재 상태 모델은 `RUNNING`/`COMPLETED`/`FAILED` 이고 실패 사유에서 `DISPATCH_TIMEOUT` 은 단일 `TIMEOUT` 으로 통합되었다. 아래 표는 별도 테이블 후보 히스토리로만 보존한다.

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

> **ACTIVE (2026-06-10)** — scenario 당 active 1건, terminal 후 재접수 허용, 재접수 = 새 case/run row insert 규칙을 유지한다. `PENDING` 상태는 사용하지 않는다.

- scenario 기준 active execution은 최대 1건이다.
- `scnr_id` 기준 `RUNNING` row가 있으면 새 접수를 거절한다.
- latest case가 `COMPLETED` 또는 `FAILED` 이면 새 접수를 허용한다.
- 재접수는 기존 row update가 아니라 새 입력 파일 3개 업로드 + 새 `tmd_mdl_scnrcasem` row + 새 `tmd_mdl_scnrcaseh` row insert다.
- 동시성 최후 방어선은 `actv_scnr_id` generated column + `UNIQUE (actv_scnr_id)` 이다.

## Callback And Detail Read Boundary

- 외부 모델 API는 callback 전에 scenario 상세조회용 결과 데이터를 먼저 생성한다.
- callback API는 `tmd_mdl_scnrcaseh.exec_stat_cd` 와 `tmd_mdl_scnrcasem.case_stat_cd` 를 terminal 상태로 전이한다.
- callback 처리 안에서 결과 데이터 materialization을 수행하지 않는다.
- `runId` 는 Platform/backend가 발급한 canonical execution identifier다.
- callback lookup은 path의 `runId` 를 기준으로 한다.
- 별도 `externalRunId` / `ext_run_id` 저장 컬럼은 두지 않는다.
- callback 인증/서명과 `runId` + terminal payload idempotency로 정합성을 보장한다.
- 이미 terminal 상태인 동일 terminal payload 재전송은 `200 OK` no-op으로 처리한다.
- scheduler timeout 이후 도착한 late callback도 식별자가 일치하면 `200 OK` no-op으로 처리하되, execution 상태는 되돌리지 않는다.
- 기존 terminal 상태와 상이한 terminal payload가 들어오면 상태를 바꾸지 않고 conflict/error로 처리한다. 단, `TIMEOUT`/`SCENARIO_CANCELLED` backend-forced terminal은 상이 payload도 `200 OK` no-op 예외로 처리한다.

## Timeout Scheduler Rule

> **ACTIVE (2026-06-10)** — `PENDING`/`DISPATCH_TIMEOUT` 정리는 제거되었다. `RUNNING` timeout 만 남으며, timeout 전이 시 `case_stat_cd` 를 terminal 로 바꾸면 `actv_scnr_id` generated column 이 자동으로 `NULL` 로 풀려 active 점유가 해제된다(별도 helper UPDATE 불필요. 상세 절차는 [scenario-case-execution-workflow-design-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md) Stage 5).

- scheduler 또는 background 정리 작업이 주기적으로 stale execution을 찾는다.
- `RUNNING` timeout 기본안:
  - 대상 상태: `RUNNING`
  - 조건: `started_at < now - timeout_threshold`
  - 전이: `FAILED`
  - 실패 사유: `TIMEOUT`
- 별도 `PENDING` stale 정리는 없다. Job dispatch 실패는 접수/dispatch 흐름 안에서 즉시 `FAILED` 로 전이한다.

## Scenario Lifecycle Cancellation And Kubernetes Job Reconciliation

### DB transaction rule

- 직접 `DISCARDED`된 scenario, `REALIZED` 전환으로 자동 `DISCARDED`된 sibling scenario, 삭제되는 group의 모든 scenario가 cancellation 대상이다. realized 대상 본인의 실행은 유지한다.
- 대상 case/history 중 `RUNNING` row만 같은 시각으로 `FAILED` terminal 전이한다. history에는 `fail_rsn_cd = SCENARIO_CANCELLED`, `exec_end_dtm`, audit 값을 기록한다.
- `SCENARIO_CANCELLED`는 18자로 현재 `VARCHAR(20)`에 저장 가능하다. `CANCELLED_BY_SCENARIO_DELETE`는 28자이므로 사용하지 않는다.
- 이미 terminal인 case/history의 status와 failure reason은 덮어쓰지 않고 soft delete만 수행한다.
- history와 latest case를 함께 terminal로 바꿔 `last_run_id` 상태 불변식을 유지하고, case status가 `FAILED`가 되면서 generated `actv_scnr_id`가 자동으로 `NULL`이 되게 한 뒤 case/history를 soft delete한다.
- scenario/group/signpost entity 변경은 bulk DML보다 먼저 수행한다. bulk DML의 `flushAutomatically`로 entity 변경을 반영한 뒤, 모든 managed entity 변경이 끝난 시점에만 persistence context를 clear한다. `deleteScenarioGroup()`에서 bulk DML 이후 detached entity를 변경하는 순서는 금지한다.
- submit과 cancellation이 경합해 cancellation 대상 조회 뒤 새 RUNNING case가 생성되지 않도록, 두 command 경로가 동일한 scenario row lock 또는 동등한 DB serialization 규칙에 참여해야 한다.

### Late callback rule

- 일반 terminal callback 재전송은 shared workflow의 canonical 비교 키(`status`, `failureReason`, `errorMessage`; `finishedAt` 제외)가 같을 때 기존 idempotent no-op 규칙을 적용한다. 현재 구현이 `finishedAt`까지 비교하는 drift는 본 구현에서 함께 정리한다.
- `FAILED + SCENARIO_CANCELLED` 또는 `FAILED + TIMEOUT`은 backend-forced terminal 상태다. 이후 callback은 incoming terminal payload가 달라도 저장 상태를 덮어쓰지 않고 `200 OK`로 ack하며 audit log만 남긴다.
- soft-deleted cancellation history도 callback correlation을 위해 `runId`로 내부 조회할 수 있어야 한다. active-resource 조회 API에는 계속 노출하지 않는다.

### Kubernetes reconciliation rule

- scenario 상태 변경/group 삭제 요청 transaction에서는 Kubernetes API를 호출하지 않는다.
- 기존 `ModelExecutionTimeoutScheduler` tick에서 timeout DB 처리와 Job reconciliation을 분리된 단계로 실행한다. 한 단계 실패가 다른 단계를 건너뛰지 않도록 단계별 예외 경계를 둔다.
- Job 생성 시 original `runId`를 label 또는 annotation으로 저장하고, `KubernetesJobService`의 단일 `toJobName(runId)` helper가 생성/조회/삭제 이름 변환을 담당한다.
- reconciler는 namespace의 managed active Job을 조회한 뒤 runId를 수집하고, DB에서 해당 run의 상태/사유를 batch 조회한다. 삭제 대상은 `FAILED + TIMEOUT` 또는 `FAILED + SCENARIO_CANCELLED`인 active Job으로 제한한다.
- 정상 `COMPLETED`, 외부 callback `FAILED`, 이미 Kubernetes terminal인 Job은 즉시 삭제하지 않는다. 기존 `ttlSecondsAfterFinished = 3600` 로그 보존 정책을 따른다.
- Job 삭제는 idempotent하게 처리하고 not-found는 성공으로 간주한다. owned Pod가 함께 정리되도록 cascade propagation을 명시한다.
- cleanup 주기 30초는 scheduler flag가 활성인 환경의 목표 SLA다. flag 비활성 환경에는 SLA가 없으므로 production rollout 전에 `features.scheduling.model-execution-timeout.enabled=true`, dedicated single replica, jobs get/list/delete RBAC를 gate로 검증한다.
- 기존 label 없는 Job은 deterministic name prefix/legacy runId parsing을 한시적 fallback으로 지원하고, rollout 이후 label 기반 조회로 수렴한다.

## Persistence Draft

> **SUPERSEDED (2026-06-10)** — 아래 별도 테이블 `tmd_scnrmdlexech` 및 컬럼 세트는 상단 "변경 메모 (2026-06-10)" 로 대체되었다. 실제 채택안은 `tmd_mdl_scnrcasem`(+ nullable `scnr_id`) + `tmd_mdl_scnrcaseh` 재사용이며, 물리 DDL 은 [scenario-case-execution-erd-ddl-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md) 를 따른다. 아래 내용은 의사결정 히스토리로만 보존한다.

### candidate table: `tmd_scnrmdlexech` (superseded)

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

## Historical Index / Constraint Candidate (Superseded)

> **SUPERSEDED (2026-06-10)** — 별도 테이블(`tmd_scnrmdlexech`) 기준 인덱스/제약이다. 통합 테이블의 인덱스/unique 는 ERD-DDL 초안의 `ix01`/`ix02`/`uq01` 을 따른다. 아래 "active helper token" 의 nullable helper unique 아이디어는 최종적으로 수동 `actv_yn` 컬럼이 아니라 `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` generated column + `UNIQUE (actv_scnr_id)` 형태로 통합 테이블에 채택되었다.

- PK: `scnr_mdl_exec_id`
- Conditional UK: `ext_run_id`
  - 전제: 외부 모델 API가 execution당 안정적으로 1:1 `externalRunId` 를 발급하고 callback 재전송 시 같은 값을 유지할 때만 적용한다.
  - `ext_run_id` 는 nullable이며 primary lookup key가 아니라 correlation / audit 검증용 보조키다.
- Index: `(scenario_id, req_dtm DESC, scnr_mdl_exec_id DESC)` for latest execution projection
- Index: `(exec_stat_cd, exec_bgn_dtm)` for timeout scheduler
- Unique helper: `(scenario_id, actv_exec_token)`

### Historical active helper token candidate (superseded)

> **SUPERSEDED (2026-06-10)** — 아래 nullable token 방식은 별도 테이블 후보의 historical idea다. 구현하지 않는다. 최종 active-run guard는 통합 테이블의 `actv_scnr_id` generated column + `UNIQUE (actv_scnr_id)` 이다.

MySQL/manual DBA 환경에서는 partial unique index가 없어 "active row만 scenario당 1건" 제약을 단순하게 걸기 어렵다. 따라서 아래 같은 nullable helper column 방식이 현실적이다.

- active row: `actv_exec_token = 'Y'`
- terminal row: `actv_exec_token = NULL`
- unique `(scenario_id, actv_exec_token)`

이렇게 하면 terminal row는 여러 건 허용되고, active row만 1건으로 제한할 수 있다.

DB unique는 동시성의 최후 방어선이라는 아이디어만 유지한다. active helper의 실제 구현은 `actv_exec_token` 이 아니라 `actv_scnr_id` generated column 이다.

## Query Responsibility

### A. 팝업 parameter 조회 (superseded)

> **SUPERSEDED (2026-06-10)** — parameter 조회 API 는 현재 단계 scope에서 제외되었다. 팝업은 시나리오 선택 드롭다운 + 파일 업로드만 제공하며 backend 조회는 발생하지 않는다(드롭다운 목록 조회는 별도 시나리오 목록 API 책임).

1. scenario baseline parameter 조회
2. baseline 참조 반환 (execution 이력과 무관)

### B. 실행 접수

1. scenario 선택값과 입력 파일 3개를 검증한다.
2. `scnr_id` 기준 `RUNNING` row 존재 여부를 조회해 active 실행이 있으면 409로 거절한다.
3. 새 `tmd_mdl_scnrcasem` row를 `case_stat_cd = 'RUNNING'`, `scnr_id = :selectedScenarioId` 로 INSERT한다.
   - `actv_scnr_id` generated column이 `scnr_id` 로 자동 산출된다.
   - 동일 scenario active row가 이미 있으면 `UNIQUE (actv_scnr_id)` 충돌이 발생하며, 이를 409 conflict로 매핑한다.
4. 발급된 `case_id` 기준으로 입력 파일 3개를 storage(S3)와 `tcm_filel(SCNRCASE + case_id)` 에 저장한다.
5. `run_id` 를 발급하고 `tmd_mdl_scnrcaseh` row를 `exec_stat_cd = 'RUNNING'` 으로 INSERT한다.
6. Kubernetes Job에 `case_id`, `run_id`, `run_seq`, callback URL, 입력 파일 참조를 전달한다.
7. Job dispatch 실패 시 같은 transaction/보상 흐름에서 case/run을 `FAILED` 로 전이한다. 이때 `case_stat_cd` 가 terminal 이 되면서 `actv_scnr_id` 는 자동으로 `NULL` 이 되어 active 점유가 해제된다.

### C. 상태 조회

1. scenario 기준 latest 상태는 `tmd_mdl_scnrcasem.scnr_id = :scnr_id` 최신 case row를 조회한다.
2. 실행 단건 상태는 `tmd_mdl_scnrcaseh.run_id = :run_id` 를 조회한다.
3. 상태/시각/실패 사유를 반환한다.

### D. callback 처리

1. path의 `runId` 로 `tmd_mdl_scnrcaseh` 를 조회한다.
2. callback 인증/서명과 terminal payload idempotency를 검증한다.
3. non-terminal이면 `COMPLETED` 또는 `FAILED` 로 전이한다.
4. 대상 run이 latest run이면 같은 transaction에서 `tmd_mdl_scnrcasem.case_stat_cd` 도 같은 terminal 상태로 전이한다.
5. 이미 terminal이고 동일 terminal payload이면 `200 OK` no-op 처리한다.
6. 이미 terminal이고 상이 terminal payload이면 상태를 바꾸지 않고 conflict/error 처리한다. 단, `TIMEOUT`/`SCENARIO_CANCELLED` backend-forced terminal은 상이 payload도 `200 OK` no-op 예외로 처리한다.
7. callback 처리 안에서는 결과 데이터 materialization을 수행하지 않는다.

### E. timeout 정리

1. `RUNNING` + threshold 초과 row 조회
2. `FAILED/TIMEOUT` 전이
3. 대상 run이 latest run이면 case도 `FAILED` 로 전이한다. `case_stat_cd` 가 terminal 이 되면 `actv_scnr_id` 는 자동으로 `NULL` 이 된다.

### F. dispatch stale 정리

수급모델 통합 흐름에는 별도 `PENDING` dispatch stale 단계가 없다. Job dispatch 실패는 접수/dispatch 흐름에서 즉시 `FAILED` 로 전이한다.

## Historical Design Archive

아래 내용은 2026-06-10 통합 결정 이전의 별도 single-history-table 후보를 설명하던 근거다. 현재 구현 기준이 아니다.

- 별도 upload master aggregate가 없다.
- latest execution cache를 위한 별도 master row가 없어도 query 비용이 작다.
- 입력 파일은 execution당 3개로 고정된 종속 자원이라 별도 master aggregate가 필요 없다.

위 전제는 `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` 통합 결정으로 superseded 되었다. 수급모델 실행도 case master + run history 구조를 사용한다.

별도 `tmd_scnrmdlexech` 또는 single-history-table 설계는 구현하지 않는다.

## Open Questions

- ~~timeout 임계 시간과 scheduler 주기~~ → 2026-06-22 구현 완료: 10분 RUNNING 임계값, `@Scheduled(fixedDelay = 30000)` 30초 polling, `features.scheduling.model-execution-timeout.enabled` feature flag(기본 `false`)로 SKGAS dedicated 단일 인스턴스에서만 활성화. DBA가 `tmd_mdl_scnrcaseh_ix01` `SHOW INDEX`/`EXPLAIN` pre-check를 확인하기 전까지는 운영 활성화를 보류한다(`docs/work-notes/2026/2026-06-22-model-execution-timeout-scheduler-implementation.md` 참고).
- ~~scenario 상세조회 결과 데이터의 physical owner와 저장 시점~~ → 2026-06-10 확정, Volumes table은 2026-06-17 `tmd_mdl_scnrcasevolrgnl`로 변경 확정. 결과 테이블 4개(`tmd_mdl_scnrcasevolrgnl`/`tmd_mdl_scnrcasetrdehdrl`/`tmd_mdl_scnrcasetrdeflwl`/`tmd_mdl_scnrcaseprcl`)에 모델 측이 `run_seq` 기준으로 적재한다("Source-Of-Truth Rule §3" 참고). execution 쪽 DDL에는 결과 readiness/materialization 컬럼을 추가하지 않는다.

## Recommended Next Step

1. 이 문서 기준으로 `erd-change-review` 를 수행한다.
2. execution persistence는 별도 테이블을 만들지 않고 `tmd_mdl_scnrcasem`(+ nullable `scnr_id`) + `tmd_mdl_scnrcaseh` 통합안을 따른다(상단 2026-06-10 통합 메모). 물리 DDL의 canonical source는 [scenario-case-execution-erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md) 다.
3. feature spec의 `Data / Model Impact` 와 `ERD Review Handoff` 는 이 구조를 기준으로 정리한다.
