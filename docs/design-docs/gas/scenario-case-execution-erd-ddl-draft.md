# Scenario Case Execution ERD / DDL Draft

- domain: gas
- status: Implemented (base schema and MI Base extension rolled out); Review (post-rollout concurrency verification pending)
- owner: platform BE
- updated at: 2026-10-08 (MI Base 현재 4파일 검증 설명 정렬; 기존 DB 적용 이력·SQL 유지)
- canonical for: Scenario Case/run physical schema, constraints, migration, rollback, verify SQL
- canonical design input: [scenario-case-execution-workflow-design-draft.md](scenario-case-execution-workflow-design-draft.md)
- related canonical output result persistence: [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)
- open questions:
  - 적용된 DB에서 current 유/무 동시 지정과 지정-vs-mutation 경쟁의 실제 MySQL 동작을 검증해야 한다.
  - `SCNRCASEHI` 공통코드의 `cd_rnk = '7'` 충돌 여부와 등록 시점은 DBA/공통코드 담당이 확인해야 한다.

## Purpose

This document is the canonical home for the DB-backed rollout SQL for Scenario Case execution.

Current status: the base DDL through the existing case/run and result persistence has been applied to production, and MI Fundamental Model case/run persistence, Volumes/Trade Flows/Price real-DB result storage, and MI 수급모델 실행 depend on those tables (see `docs/work-notes/2026/2026-06-05-fundamental-model-create-case-real-db-implementation.md` and later 2026-06 real-DB work notes for rollout evidence). 사용자 확인(2026-09-15)에 따라 generated `actv_base_yn` column과 `tmd_mdl_scnrcasem_uq02` unique extension도 testdb, 고객사 개발 DB, 운영 DB에 적용됐다. 사용자 확인(2026-09-17)에 따라 `SCNRCASEHTF` 공통코드도 모든 대상 DB에 적용됐고 `cd_rnk = 8` 중복이 없다. 이 세션에서는 DB metadata를 직접 조회하지 않았으며 적용된 MySQL의 동시 지정·지정-vs-mutation 경쟁 검증은 남아 있다. 아래 pre-check/apply/rollback/verify SQL은 적용 이력과 재현·복구 기준으로 유지한다.

MI Fundamental Model 1차 mock API의 endpoint/path/request/response canonical source는 [mi-fundamental-model-mock-api-feature-spec.md](../../feature-specs/mi-fundamental-model-mock-api-feature-spec.md) 이다. 이 ERD/DDL draft는 DB rollout draft이며, API 계약을 재정의하지 않는다.

Volumes / Trade Flows / Price parsed output result table, key, index, partition-ready strategy, and mock-to-real transition boundary are canonical in [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md). This case execution DDL draft owns only case/run tables and raw file linkage codes.

2026-05-20 정합성 메모 (historical, 2026-06-10 `scnr_id` 재도입으로 부분 superseded):

- MI Fundamental Model은 MI Scenario Workbench와 동급 메뉴이며, 1차 mock canonical API path는 `/api/v1/mi/fundamental-model/**` 이다.
- `Scenario Case` 명칭은 현업 용어로 유지하지만 API parent가 `Scenario`라는 뜻은 아니다.
- 1차 mock canonical contract에는 `scenarioId`가 없다.
- `scenario_id`라는 과거 컬럼명과 물리 FK 전제는 사용하지 않는다. 현재 proposed model은 nullable `tmd_mdl_scnrcasem.scnr_id`로 MI 수급모델의 scenario 식별자를 논리 참조한다.
- `run_id` 는 backend가 `yyyymmdd_hhmmss_XXXX` 포맷으로 생성하며, `tmd_mdl_scnrcaseh.run_id` 와 `tmd_mdl_scnrcasem.last_run_id` 는 `VARCHAR(20)` 기준으로 검토한다.
- change note: 과거 `scenario_id` 물리 FK/컬럼명 전제는 superseded로 분류한다. 현재 DB/DDL 적용 SQL은 `scnr_id` nullable column과 `tmd_mdl_scnrcasem_ix02` 기준으로 검토한다.

2026-06-10 정합성 메모 (위 2026-05-20 의 `scenario_id` 제거 결정을 부분 supersede):

- `MI 수급모델 실행`(시나리오 기준)을 별도 execution 테이블(`tmd_scnrmdlexech`) 없이 `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` 로 통합 수용하기로 한다. 자세한 근거는 [mi-scenario-model-execution-workflow-design.md](mi-scenario-model-execution-workflow-design.md) 참고.
- 이를 위해 `tmd_mdl_scnrcasem` 에 nullable `scnr_id`(`INT UNSIGNED NULL`) 컬럼을 다시 추가한다. MI Fundamental Model row 는 `scnr_id = NULL`, MI 수급모델 row 는 scenario 식별자를 채운다.
- mapping 은 `scenario : case = 1:N` 이다. 수급모델은 실행마다 새 `case_id` 가 발행되고 `scnr_id` 는 동일하게 유지된다. 입력 파일은 fundamental 과 동일하게 `tcm_filel` 의 `SCNRCASE + case_id` relation 으로 실행별로 자연 분리된다.
- "scenario 당 active 실행 1건" 가드는 2단으로 둔다. 1차는 `scnr_id` 기준 `case_stat_cd = 'RUNNING'` row 존재 여부 확인(친절한 409 응답용)이고, 최후 방어선은 `actv_scnr_id` generated column unique 다(아래 bullet). `tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` 는 1차 존재 확인/최신 케이스 조회용 인덱스다. (line 177 의 "ix02 불필요" 가정은 본 결정으로 supersede)
- `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` generated column 과 `tmd_mdl_scnrcasem_uq01 UNIQUE (actv_scnr_id)` 을 추가한다 (2026-06-10, 존재 확인만으로는 동시 insert 경합을 막을 수 없다는 리뷰 반영. 2026-06-10 helper 를 수동 `actv_yn CHAR(1)` 에서 generated column 으로 변경 — `case_stat_cd` 와 helper 가 어긋날 수 없도록 DB 가 파생하게 함). helper 값은 `case_stat_cd`/`scnr_id` 에서 DB 가 자동 계산하므로 애플리케이션이 동기화할 필요가 없다(RUNNING 이면 `scnr_id`, 그 외 `NULL`). 수급모델 실행 접수는 새 case row INSERT 이므로 동일 scenario 의 active row 가 이미 있으면 `actv_scnr_id` 가 중복되어 unique 충돌이 나고, 이를 conflict(409)로 매핑한다. fundamental row 는 `scnr_id = NULL` 이라 RUNNING 이어도 `actv_scnr_id = NULL` 이 되어 MySQL unique 가 바인딩되지 않으므로(NULL 다건 허용) 기존 case 단위 conditional update 가드와 충돌하지 않는다. 표준단어: 활성=ACTV (사전 등록 완료).
- current MI Base 최대 1건은 `exec_type_cd`와 `del_yn`에서 파생되는 generated `actv_base_yn` + `tmd_mdl_scnrcasem_uq02 UNIQUE (actv_base_yn)`로 강제한다. 새 지정 시 이전 base를 `INDEPENDENT`로 복귀시키고 대상만 `MI_BASE`로 변경한다.
- 꼬리 컬럼 `case_id` 는 "시나리오별 최근 케이스(실행상태 조건 포함) 조회" 를 인덱스만으로 처리하기 위함이다. `case_id` 는 auto-increment 이므로 `WHERE scnr_id = ? AND case_stat_cd = ? ORDER BY case_id DESC LIMIT 1` 가 정렬 비용 없이 최신 1건을 반환한다.
- `scnr_id` 는 외부 scenario aggregate(`tmm_mi_scenario_m`) 에 대한 논리 참조이며 물리 FK 는 걸지 않는다(`last_run_id` 와 동일한 application-validated 참조 방침). 참조 대상 실물은 `tmm_mi_scenario_m.scenario_id INT UNSIGNED` 로 확인되었고(2026-06-10, 구 명칭 `tmm_scenariom` 은 superseded), 타입이 `scnr_id INT UNSIGNED` 와 일치한다. 참조 대상은 full-word 컬럼 컨벤션(MI/agent 팀)을 쓰지만 플랫폼 `tmd_` 테이블 컬럼명은 용어사전 약어(시나리오=SCNR)를 따른다.
- 명명 rename (2026-06-10): 용어사전(시나리오=SCNR, 케이스=CASE) 정합화를 위해 `scncase` → `scnrcase`, `scn_id` → `scnr_id` 로 모든 테이블/인덱스/제약 이름을 일괄 변경한다. 공통코드 `cd_id` 값도 동일한 용어사전 정합 기준으로 `SCNCASE` → `SCNRCASE`, `SCNCASEH` → `SCNRCASEH` 로 변경한다(기존 "공통코드 값은 변경하지 않는다" 결정은 superseded). 기존 환경 반영은 조건부 rename/ALTER 분기 대신 drop & recreate 전략을 따른다(아래 마이그레이션 섹션).

2026-06-11 Notion evidence sync:

- Reviewed Notion pages `MI Fundamental Model`, `MI Fundamental Model 실행`, and `Fundamental Model Run Callback API Spec`.
- The Notion `MI Fundamental Model 실행` page still contains a historical DDL block using `scn_id`, `run_seq BIGINT UNSIGNED`, and `tmd_mdl_scnrcaseh_ix01 (exec_stat_cd, exec_bgn_dtm)`.
- Those Notion DDL details are reference-only and are superseded by this DDL draft:
  - use `scnr_id`, not `scn_id`;
  - use `run_seq INT UNSIGNED AUTO_INCREMENT UNIQUE`, not `BIGINT UNSIGNED`;
  - use `tmd_mdl_scnrcaseh_ix01 (exec_stat_cd, exec_bgn_dtm, case_id)` for the approved timeout scheduler, but apply it only with the scheduler implementation and verify the actual query with `EXPLAIN`;
  - keep `tmd_mdl_scnrcasem_ix03 (last_run_id, del_yn)` for callback latest-case status sync.
- No additional owned table DDL is missing for the current case/run scope. `tcm_filel` is an existing common table and is referenced through compatibility/pre-check SQL instead of being redefined here.

## Scope

### In Scope

- Future physical table/column DDL for `tmd_mdl_scnrcasem` and `tmd_mdl_scnrcaseh`
- Common-code rollout/reference SQL for `SCNRCASE`, `SCNRCASEH`, `SCNRCASEHI`, and `SCNRCASEHTF`
- Future index and verification SQL for DB-backed duplicate-run guard and timeout scheduler
- Future DB-backed query shape for case detail lookup
- Future DB-backed update/delete predicate shape for active case metadata update and soft delete
- Current MI Base 지정을 위한 generated current marker·unique와 legacy migration
- Raw output file linkage via `SCNRCASEH + run_seq`
- Model execution intermediate file linkage via `SCNRCASEHI + run_seq`
- Trade Flow output file linkage via `SCNRCASEHTF + run_seq`

### Out of Scope

- Phase-1 in-memory mock API implementation
- Java implementation details
- 결과 파일 다운로드 contract 및 결과 파일 물리 저장
- Volumes / Trade Flows / Price parsed result data tables. 해당 DB-backed 테이블 DDL과 인덱스/파티션 전략은 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md) 에서 관리한다.

## Phase-1 Case Detail / Update / Delete API Schema Impact

`GET /api/v1/mi/fundamental-model/cases/{caseId}`, `PATCH /api/v1/mi/fundamental-model/cases/{caseId}`, `DELETE /api/v1/mi/fundamental-model/cases/{caseId}` 추가는 phase-1 mock API 범위에서 DB schema 변경을 요구하지 않는다.

- phase-1 implementation source: in-memory mock store seeded from `src/main/resources/mock/fundamental-model/fundamental-model-cases.json`
- no new table
- no new column
- no index change
- common-code detail: `SCNRCASEHI` and `SCNRCASEHTF` in `tcm_commcdd`
- `SCNRCASEHTF`는 사용자 확인 기준 모든 대상 DB 적용 완료, `cd_rnk = 8` 무충돌

상세 응답은 case metadata, uploaded file metadata, latest run projection만 반환한다. full run history, result file reference, result download path/URL은 이 API와 schema impact 범위에서 제외한다.

`GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes`, `GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes/trade`, `GET /api/v1/mi/fundamental-model/cases/{caseId}/price` 추가도 phase-1 mock에서 DB schema 변경을 요구하지 않는다. 1차 mock은 json seed 기반 고정 데이터를 반환하며, no new table / no new column / no index change / no DBA rollout action이다. DB-backed 전환 시 결과 수치 데이터 저장 테이블 설계는 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)를 따른다.

수정은 `tmd_mdl_scnrcasem.case_titl_nm`, `tmd_mdl_scnrcasem.case_expn`에 해당하는 metadata만 변경한다. 삭제는 physical delete가 아니라 `tmd_mdl_scnrcasem.del_yn = 'Y'`에 해당하는 soft-delete semantics만 반영한다. phase-1 mock에서는 record의 `delYn` 필드로 이 동작을 모사한다.

Future DB-backed 전환 시 expected query shape:

1. `tmd_mdl_scnrcasem`에서 `case_id`, `usr_no` ownership scope, `del_yn = 'N'` 기준으로 case master를 조회한다.
2. `tcm_filel`에서 `file_rel_obj_spt_cd = 'SCNRCASE'` 및 `file_rel_obj_dstg_no = :case_id` 기준으로 업로드 파일 metadata를 조회한다.
3. `tmd_mdl_scnrcasem.last_run_id`가 null이 아니면 `tmd_mdl_scnrcaseh.run_id = last_run_id`로 latest execution row 1건을 조회한다.
   - API `latestRun.startedAt` maps to `tmd_mdl_scnrcaseh.exec_bgn_dtm`.
   - API `latestRun.finishedAt` maps to `tmd_mdl_scnrcaseh.exec_end_dtm`.
   - FE calculates elapsed time from these two fields; no separate duration column or response field is required for phase 1.
4. result payload/download 관련 테이블 또는 컬럼은 조회하지 않는다.

DB-backed 구현 전에는 위 query shape에 대해 ownership 조건, `tcm_filel` 연결 인덱스, `run_id` unique/index 사용 여부를 EXPLAIN으로 검증해야 한다.

Future DB-backed update shape:

```sql
UPDATE tmd_mdl_scnrcasem
SET case_titl_nm = :title,
    case_expn = :description,
    mod_dtm = NOW(),
    mod_usr_logn_id = :actor
WHERE case_id = :case_id
  AND usr_no = :usr_no
  AND del_yn = 'N';
```

- predicate는 `case_id + usr_no ownership + del_yn = 'N'`을 반드시 포함한다.
- update 대상은 `case_titl_nm`, `case_expn`, `mod_*` columns로 제한한다.
- files, `last_run_id`, `case_stat_cd`, `tmd_mdl_scnrcaseh`, result 관련 데이터는 갱신하지 않는다.

Future DB-backed delete shape:

```sql
UPDATE tmd_mdl_scnrcasem
SET del_yn = 'Y',
    mod_dtm = NOW(),
    mod_usr_logn_id = :actor
WHERE case_id = :case_id
  AND usr_no = :usr_no
  AND del_yn = 'N';
```

- predicate는 `case_id + usr_no ownership + del_yn = 'N'`을 반드시 포함한다.
- delete는 `del_yn = 'Y'`, `mod_*` columns만 갱신한다.
- `tcm_filel`, `tmd_mdl_scnrcaseh`, latestRun/result payload는 물리 삭제하지 않는다.
- affected row count가 `0`이면 active resource 관점의 `404 RSC_NOT_FOUND`로 처리한다.

## Ownership / Common-Code Decisions

- ownership scope는 `usr_no` only로 확정한다.
- `ASSIGNEE_NAME` 검색 JOIN 경계는 `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no`, 표시 컬럼은 `tmd_usrm.usr_nm`으로 확정한다.
- `SCNRCASE` target duplicate status was confirmed clear on 2026-05-19. Draft metadata is `cd_nm = '시나리오 케이스 입력 파일'`, `cd_expn = '시나리오 케이스 입력 파일 연결 코드'`. Final registration still requires DBA/common-code owner confirmation.
- `SCNRCASEH` common-code (output file linkage via `run_seq`) must also be registered. Target duplicate status confirmed clear on 2026-05-26. Draft metadata is `cd_nm = '시나리오 케이스 실행 결과 파일'`, `cd_expn = '시나리오 케이스 모델 실행 결과 파일 연결 코드'`. Final registration still requires DBA/common-code owner confirmation.
- `SCNRCASEHI` common-code (model execution intermediate file linkage via `run_seq`) must be registered. Metadata is `cd_nm = '시나리오 케이스 실행 중간 파일'`, `cd_expn = '시나리오 케이스 모델 실행 중간 파일 연결 코드'`, `cd_rnk = '7'`. Target duplicate status and `cd_rnk` collision must be checked with Pre-Check 2 before registration. Final registration requires DBA/common-code owner confirmation.
- `SCNRCASEHTF` common-code (Trade Flow result file linkage via `run_seq`) is registered in every target DB per user confirmation on 2026-09-17. Metadata is `cd_nm = '시나리오 케이스 실행 결과 Trade Flow'`, `cd_expn = '시나리오 케이스 모델 실행 결과 Trade Flow 파일 연결 코드'`, `cd_rnk = '8'`, `use_bgn_dt = '20260907'`; rank duplication was confirmed absent.

## Closed Decisions

- `run_id` format and PK (2026-05-21): backend-generated `run_id` format is `yyyymmdd_hhmmss_XXXX`. `tmd_mdl_scnrcaseh.run_id` is `VARCHAR(20) PRIMARY KEY`. `tmd_mdl_scnrcasem.last_run_id` is `VARCHAR(20)`.
- `run_seq` surrogate key (2026-05-22, type fixed 2026-06-11): `tmd_mdl_scnrcaseh`에 `run_seq INT UNSIGNED AUTO_INCREMENT UNIQUE`를 추가한다. `file_rel_obj_dstg_no`가 `INT UNSIGNED` 계열이어서 `run_id`(`VARCHAR(20)`)를 직접 저장할 수 없는 제약을 해결하기 위한 surrogate key다. output 파일은 `SCNRCASEH` / `run_seq`로 `tcm_filel`에 연결한다. `run_id`는 model pod 연동 키로 그대로 유지한다. Java 타입은 unsigned 범위를 보존하기 위해 `Long`을 사용한다. 이전 "no separate surrogate key" 결정과 `BIGINT UNSIGNED` 후보는 이 결정으로 superseded된다.
- 모델 실행 중간파일 연결 (2026-09-07): 모델이 실행 중 생성해 업로드하는 중간파일 n개는 output 파일과 동일하게 `file_rel_obj_dstg_no = tmd_mdl_scnrcaseh.run_seq` 에 귀속되지만, `file_rel_obj_spt_cd = 'SCNRCASEHI'` 로 output(`SCNRCASEH`)과 구분한다. 테이블/컬럼 변경은 없고 `tcm_commcdd` detail 1건만 추가한다. 매핑 테이블 신설, `tcm_filel` 컬럼 추가, 파일명/경로 규칙 구분 대안은 검토 후 채택하지 않았다. 적재 주체는 output 과 동일하게 model pod 직접 INSERT 이며 backend 는 쓰기에 관여하지 않는다. 설계 근거와 pod INSERT 컬럼 계약은 [mi-fundamental-model-intermediate-file-design.md](mi-fundamental-model-intermediate-file-design.md) 가 canonical 이다.
- Trade Flow 결과파일 연결 (2026-09-17): 모델이 저장하는 Trade Flow 결과파일은 output/intermediate와 동일한 `file_rel_obj_dstg_no = tmd_mdl_scnrcaseh.run_seq`에 귀속되고 `file_rel_obj_spt_cd = 'SCNRCASEHTF'`로 구분한다. 추가 table/column/index는 없으며, 상세 응답에서는 `files.tradeOutput` 별도 배열로 제공한다. 적재 주체는 model pod다.
- `last_run_id` column (2026-05-19, 2026-05-21 length updated): added to `tmd_mdl_scnrcasem` as `VARCHAR(20) NULL`, an application-validated latest execution pointer. No physical FK to `tmd_mdl_scnrcaseh.run_id`. Update order and the `case_stat_cd` / `last_run_id` invariant are enforced by the application within a single transaction. See workflow design draft §2 and §5.
- Fundamental Model input file linkage strategy (2026-06-05): DB-backed `POST /api/v1/mi/fundamental-model/cases`는 `TEMP/0` 임시 relation을 거치지 않고 `case_id` 확보 후 `SCNRCASE + case_id` relation으로 직접 업로드한다. 파일 3개 중 하나라도 실패하면 API 전체를 실패로 처리하며, 이미 성공한 업로드 분은 `createCase()` service가 file delete primitive를 호출해 보상 삭제한다. 아래 Next Gen Scenario model-execution staging 결정과 endpoint 범위가 다르다.
- model-execution staging persistence (2026-07-10): 장기 transaction 개선을 위한 staged upload는 별도 table/column/status를 만들지 않고 기존 `tcm_filel`의 `file_rel_obj_spt_cd = 'TEMP'`, `file_rel_obj_dstg_no = 0` relation을 durable marker로 재사용한다. object key는 처음부터 최종 `SCNRCASE/{uuid}` prefix를 사용한다. 최종 Case transaction은 marker row를 `SCNRCASE + case_id`로 attach하는 metadata update만 수행하고 Object Storage copy/move/delete를 포함하지 않는다. 따라서 기존 `FileService.updateFile()`의 TEMP move 동작을 staging finalization에 재사용하지 않고 internal metadata-only attach primitive를 사용한다. crash orphan 식별자는 `TEMP/0 + SCNRCASE storage path`, TTL은 24시간이며 cleanup은 bounded batch와 실제 `EXPLAIN`으로 기존 `tcm_filel_ix01` 사용성을 검증한다. 이 결정은 no new table, no new column, no new common-code, no immediate index/DBA rollout action으로 분류한다.
- `tcm_filel` compatibility / index coverage (2026-06-05): 운영 DB에서 `tcm_filel.file_rel_obj_dstg_no = INT UNSIGNED`, `tcm_filel_ix01 (file_rel_obj_spt_cd, file_rel_obj_dstg_no)` 존재를 확인했다. `WHERE del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = :case_id` EXPLAIN 결과 `key = tcm_filel_ix01`, `type = ref`, `rows` small, no full scan으로 확인되었다.
- Result payload / pointer columns (2026-05-19): `req_param_json` and `rslt_data_json` are not in scope. Request information is tracked via case metadata and `tcm_filel` linkage; result payload/files are owned by the external model. Revisit only when a result-query / download feature spec is defined.
- `scenario_id` column-name removal (2026-05-20, superseded by 2026-06-10 `scnr_id` re-introduction): the old `scenario_id` column name and physical FK assumption were removed. The current proposed model uses nullable `scnr_id` as an application-validated logical scenario reference for MI supply-model rows.
- `tmd_mdl_` prefix rename (2026-06-10): 모든 모델 실행 관련 테이블 이름에 표준단어 `모델(MDL)` 을 붙여 `tmd_` 다음에 `mdl_` 을 삽입한다(당시 이름 기준 `tmd_scncasem → tmd_mdl_scncasem` 등 6테이블). 같은 날 아래 `scnrcase` rename 결정이 더해져 최종 이름은 `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, `tmd_mdl_scnrcasevolrgnl`, `tmd_mdl_scnrcasetrdehdrl`, `tmd_mdl_scnrcasetrdeflwl`, `tmd_mdl_scnrcaseprcl` 이다. `tmd_mdl_scnrcasevoll` atomic Volumes table 방향은 2026-06-17에 superseded 되었다. 인덱스/제약 이름도 최종 테이블명 기준으로 정합화한다. 표준단어 출처: `docs/conventions/db-term-dictionary-sheet2.csv` (모델=MDL).
- `scnrcase` 표준단어 정합 rename (2026-06-10): 용어사전에 시나리오=SCNR, 케이스=CASE 로 등록되어 있어 사전에 없는 `scn` 약어를 쓰던 `scncase` 를 `scnrcase` 로, `scn_id` 를 `scnr_id` 로 일괄 변경한다(ERD 리뷰 P2 반영). 공통코드 `cd_id` 값도 같은 기준으로 `SCNCASE` → `SCNRCASE`, `SCNCASEH` → `SCNRCASEH` 로 변경한다(기존 "공통코드 값은 변경하지 않는다" 결정은 superseded). 기존 환경 반영은 drop & recreate 전략을 따른다.
- `scnr_id` re-introduction (2026-06-10, supersedes the 2026-05-20 removal): nullable `scnr_id INT UNSIGNED NULL` is re-added to `tmd_mdl_scnrcasem` so that `MI 수급모델 실행` reuses `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` instead of a separate `tmd_scnrmdlexech` table. Fundamental rows keep `scnr_id = NULL`; supply-model rows carry the scenario identifier. Mapping is `scenario : case = 1:N` (a new `case_id` per execution, shared `scnr_id`). `scnr_id` is an application-validated logical reference to the external scenario aggregate (`tmm_mi_scenario_m.scenario_id INT UNSIGNED`, 2026-06-10 실물 스키마 확인, 구 명칭 `tmm_scenariom` superseded) with no physical FK. `tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` 는 active 존재 확인/최신 케이스 조회용 인덱스다. See [mi-scenario-model-execution-workflow-design.md](mi-scenario-model-execution-workflow-design.md).
- per-scenario active-run guard DB 보강 (2026-06-10): `tmd_mdl_scnrcasem` 에 `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` generated column 과 `tmd_mdl_scnrcasem_uq01 UNIQUE (actv_scnr_id)` 을 추가한다. helper 는 `case_stat_cd`/`scnr_id` 에서 DB 가 자동 파생하므로 invariant(`RUNNING` 이면 `scnr_id`, 그 외 `NULL`)가 코드 동기화 없이 보장된다. 수급모델 실행 접수는 새 case INSERT 가 동일 scenario active row 와 `actv_scnr_id` 중복으로 unique 충돌하면 active 실행 존재로 보고 conflict(409)로 매핑한다. fundamental row 는 `scnr_id = NULL` → `actv_scnr_id = NULL` 이라 unique 가 바인딩되지 않아 case 단위 conditional update 가드와 충돌하지 않는다. `scnr_id` 기준 RUNNING 존재 확인(ix02)은 1차 가드, unique 는 DB 최후 방어선이다. 표준단어: 활성=ACTV. (2026-06-10 helper 를 수동 `actv_yn CHAR(1)` 에서 generated column 으로 변경 — drift 제거)
- Volumes/Trade Flows/Price 결과 데이터 저장 (2026-06-07): 1차 mock은 json seed 기반 in-memory 응답으로 처리. DB 테이블 미생성. DB-backed 전환 시 parsed result data는 `tmd_mdl_scnrcaseh.run_seq` 기준으로 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)의 테이블/키/인덱스/파티션 전략을 따른다.
- 실행유형 명시 컬럼 `exec_type_cd` 도입 (2026-06-16): 기존에 `scnr_id` NULL 여부로 암묵 구분하던 실행유형을 명시 컬럼으로 분리한다. 사유: `MI Base` 적재 데이터를 실행 테이블에 두기로 하면서 `INDEPENDENT`(독립실행, `scnr_id = NULL`)와 `MI_BASE`(MI Base 적재, `scnr_id = NULL`)가 둘 다 `scnr_id = NULL` 이 되어 `scnr_id` 만으로는 구분이 불가능해졌다. `exec_type_cd VARCHAR(20) DEFAULT 'INDEPENDENT' NOT NULL`, 코드값 `INDEPENDENT`/`SCENARIO`/`MI_BASE`. `actv_scnr_id` 처럼 generated column 으로는 파생할 수 없어(INDEPENDENT/MI_BASE 가 동일 `scnr_id = NULL`) writer 가 명시적으로 채우는 stored 컬럼이며, `scnr_id` 와의 정합은 `tmd_mdl_scnrcasem_ck01 CHECK ((exec_type_cd = 'SCENARIO') = (scnr_id IS NOT NULL))` 로 DB 가 강제한다. 코드값은 형제 코드(`case_stat_cd`=`PENDING`/`RUNNING`...)와 동일한 풀영어 단어 컨벤션을 따른다(용어사전 약어 `SCNR` 대신 `SCENARIO`). `MI_BASE` 는 사용자 실행이 아닌 적재 데이터로 admin `usr_no` 를 채우며, "1건 유지" 는 가드로 강제하지 않고 조회 시 `exec_type_cd = 'MI_BASE'` 의 최신 1건(`ORDER BY case_id DESC LIMIT 1`)을 선택한다. 기존 목록/상세/ASSIGNEE 조회는 `exec_type_cd` 조건으로 `MI_BASE` 를 제외한다.
- MI Base 운영 의미 정정 (2026-09-07 확정, 위 bullet의 별도 적재/admin 소유 설명 supersede): `MI_BASE`는 기존 완료 `INDEPENDENT` case를 지정한 것이며 `case_id`, `usr_no`, `last_run_id`, run history, result row, `SCNRCASE + case_id` input 파일을 유지한다. 새 지정 시 이전 current는 `INDEPENDENT`로 복귀하고 generated `actv_base_yn` + `uq02`가 active `MI_BASE`를 최대 1건으로 제한한다.

## DDL Source-of-Truth

이 문서는 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh` 의 table, column, key, index, rollout SQL, verify SQL canonical source다.

[scenario-case-execution-workflow-design-draft.md](scenario-case-execution-workflow-design-draft.md)는 workflow/domain semantics만 관리하며 case/run `CREATE TABLE` 후보 DDL을 보유하지 않는다. workflow 문서에 있던 중복 Candidate DDL은 2026-06-10에 제거되었다.

DBA review가 완료되고 open decision이 모두 닫히면 아래 `## Apply` 섹션의 SQL을 기준으로 final rollout SQL을 확정한다.

## Pre-Check

Run these checks in the target DB before `Apply`.

```sql
-- 1) file linkage target column type / existing index coverage
SHOW CREATE TABLE tcm_filel;
SHOW INDEX FROM tcm_filel;

EXPLAIN
SELECT *
FROM tcm_filel
WHERE del_yn = 'N'
  AND file_rel_obj_spt_cd = 'SCNRCASE'
  AND file_rel_obj_dstg_no = :case_id;

-- 2) common-code group and target code duplication check
SELECT cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id IN ('SCNRCASE', 'SCNRCASEH', 'SCNRCASEHI', 'SCNRCASEHTF');

-- 2a) cd_rnk collision check for the new intermediate-file code
SELECT cd_id, cd_nm, cd_rnk, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
ORDER BY cd_rnk;

-- 2b) exec_type_cd code group (master) duplication check — 신규 그룹이므로 detail 전에 확인
SELECT cd_grp_id, cd_grp_nm, use_yn
FROM tcm_commcdm
WHERE cd_grp_id = 'mdl_exec_type_cd';

-- 2c) exec_type_cd detail code duplication check
SELECT cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'mdl_exec_type_cd'
  AND cd_id IN ('INDEPENDENT', 'SCENARIO', 'MI_BASE');

-- 3) existing table/object duplication check
SHOW TABLES LIKE 'tmd_mdl_scnrcasem';
SHOW INDEX FROM tmd_mdl_scnrcasem;
SHOW TABLES LIKE 'tmd_mdl_scnrcaseh';
SHOW INDEX FROM tmd_mdl_scnrcaseh;

-- 4) confirm user master lookup path for assignee name search/display
SHOW CREATE TABLE tmd_usrm;
SHOW INDEX FROM tmd_usrm;

-- 5) confirm `tmd_mdl_scnrcaseh` run_seq / FK shape if rolled out in the same release
SHOW CREATE TABLE tmd_mdl_scnrcaseh;
```

Pre-check review points:

- Confirm `tcm_filel.file_rel_obj_dstg_no` is compatible with `tmd_mdl_scnrcasem.case_id INT UNSIGNED`.
- Confirm `SCNRCASE + case_id` file lookup uses `key = tcm_filel_ix01`, `type = ref`, and no full scan.
- Confirm no active duplicate rows already exist for `SCNRCASE` / `SCNRCASEH` / `SCNRCASEHI` / `SCNRCASEHTF` in `file_rel_obj_spt_cd`. `SCNRCASEHTF`와 `cd_rnk = 8`은 2026-09-17 사용자 확인 기준 모든 대상 DB에서 적용·무충돌 상태다.
- Confirm `cd_rnk = '7'` does not collide within the `file_rel_obj_spt_cd` group. If it does, assign the next free rank and record the applied value here.
- Confirm `tmd_mdl_scnrcasem_ix01 (del_yn, crt_dtm)` is the retained list index and `tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` backs the per-scenario active-run existence lookup for `MI 수급모델 실행` (2026-06-10).
- Confirm `tmd_mdl_scnrcasem_ix03 (last_run_id, del_yn)` backs callback latest-case status sync (`WHERE last_run_id = :run_id AND del_yn = 'N'`) without a full scan.
- Confirm `actv_scnr_id` 가 STORED generated column(`IF(case_stat_cd='RUNNING', scnr_id, NULL)`)으로 생성되었고 `tmd_mdl_scnrcasem_uq01 (actv_scnr_id)` unique 가 걸렸는지, fundamental row(`scnr_id = NULL` → `actv_scnr_id = NULL`) 다건이 unique 에 걸리지 않는지 확인한다 (MySQL unique 는 NULL row 를 중복 검사에서 제외).
- MI Base extension 적용 전 active `MI_BASE` 건수와 대상 `case_id`를 확인한다. legacy 보존 후보는 기존 규칙의 최대 `case_id`이되 case/latest run/input 3종/Price 적격성을 모두 통과해야 한다. 나머지 active `MI_BASE`는 `INDEPENDENT`로 복귀시킨다. 보존 후보가 부적격이면 자동으로 다른 row를 고르지 않고 no-go로 중단한다.
- Confirm `tmd_mdl_scnrcasem_uq01`, `tmd_mdl_scnrcasem_uq02`, `tmd_mdl_scnrcaseh_uq01` and `tmd_mdl_scnrcaseh_fk01` names do not collide with existing objects.
- Confirm `ASSIGNEE_NAME` JOIN path remains `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no`, display column `tmd_usrm.usr_nm`.
- Confirm `last_run_id VARCHAR(20) NULL` remains an application-validated pointer, not a physical FK or unique key.

## Apply

아래 전체 CREATE TABLE의 `actv_base_yn` generated 정의가 확정 schema다. base schema와 MI Base column/constraint는 적용 완료됐으므로 전체 DDL이나 incremental extension을 재실행하지 않는다. 아래 SQL은 적용 이력과 신규 환경 rollout/rollback 기준으로 유지한다.

Current draft finalizes `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, and common-code registration wording.

```sql
CREATE TABLE tmd_mdl_scnrcasem
(
    case_id          INT UNSIGNED AUTO_INCREMENT COMMENT '시나리오케이스ID'
        PRIMARY KEY,
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    usr_no           INT UNSIGNED                          NOT NULL COMMENT '사용자번호',
    scnr_id           INT UNSIGNED                          NULL     COMMENT '시나리오ID (MI 수급모델 실행 연계, MI Fundamental Model은 null, tmm_mi_scenario_m.scenario_id 논리참조, 물리 FK 없음)',
    exec_type_cd     VARCHAR(20) DEFAULT 'INDEPENDENT'     NOT NULL COMMENT '수급모델실행유형코드 (INDEPENDENT/SCENARIO/MI_BASE)',
    case_titl_nm     VARCHAR(200)                          NOT NULL COMMENT '케이스제목명',
    case_expn        TEXT                                  NULL     COMMENT '케이스설명',
    case_stat_cd     VARCHAR(20) DEFAULT 'PENDING'         NOT NULL COMMENT '케이스상태코드',
    last_run_id      VARCHAR(20)                           NULL     COMMENT '최신실행ID (tmd_mdl_scnrcaseh.run_id 논리참조, 물리 FK 없음)',
    actv_scnr_id     INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED COMMENT '활성시나리오ID (case_stat_cd=RUNNING 이면 scnr_id, 그 외 NULL. scenario 당 active 1건 unique helper. case_stat_cd/scnr_id 에서 자동 파생되는 STORED generated column)',
    actv_base_yn     CHAR(1) AS (IF(del_yn = 'N' AND exec_type_cd = 'MI_BASE', 'Y', NULL)) STORED COMMENT '활성베이스여부 (active MI Base이면 Y, 그 외 NULL. 전역 current MI Base 1건 unique helper)',
    CONSTRAINT tmd_mdl_scnrcasem_uq01
        UNIQUE (actv_scnr_id),
    CONSTRAINT tmd_mdl_scnrcasem_uq02
        UNIQUE (actv_base_yn),
    CONSTRAINT tmd_mdl_scnrcasem_ck01
        CHECK ((exec_type_cd = 'SCENARIO') = (scnr_id IS NOT NULL))
)
    COMMENT 'TMD_시나리오케이스마스터';

CREATE INDEX tmd_mdl_scnrcasem_ix01
    ON tmd_mdl_scnrcasem (del_yn, crt_dtm);

CREATE INDEX tmd_mdl_scnrcasem_ix02
    ON tmd_mdl_scnrcasem (scnr_id, case_stat_cd, case_id);

CREATE INDEX tmd_mdl_scnrcasem_ix03
    ON tmd_mdl_scnrcasem (last_run_id, del_yn);

CREATE TABLE tmd_mdl_scnrcaseh
(
    run_id          VARCHAR(20)                           NOT NULL COMMENT '실행ID (yyyymmdd_hhmmss_XXXX)'
        PRIMARY KEY,
    run_seq         INT UNSIGNED AUTO_INCREMENT COMMENT '실행순번 (tcm_filel output 파일 연결용 surrogate key)',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR        DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    case_id         INT UNSIGNED                          NOT NULL COMMENT '시나리오케이스ID',
    exec_stat_cd    VARCHAR(20) DEFAULT 'RUNNING'         NOT NULL COMMENT '실행상태코드',
    fail_rsn_cd     VARCHAR(20)                           NULL COMMENT '실패사유코드',
    err_msg_cntt    VARCHAR(2000)                         NULL COMMENT '오류메시지내용',
    exec_bgn_dtm    DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '실행시작일시 (DB 시각으로 생성)',
    exec_end_dtm    DATETIME                              NULL COMMENT '실행종료일시',
    CONSTRAINT tmd_mdl_scnrcaseh_uq01
        UNIQUE (run_seq),
    CONSTRAINT tmd_mdl_scnrcaseh_fk01
        FOREIGN KEY (case_id) REFERENCES tmd_mdl_scnrcasem (case_id)
)
    COMMENT 'TMD_시나리오케이스이력';

CREATE INDEX tmd_mdl_scnrcaseh_ix01
    ON tmd_mdl_scnrcaseh (exec_stat_cd, exec_bgn_dtm, case_id);

INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNRCASE', '시나리오 케이스 입력 파일', '5', '시나리오 케이스 입력 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);

INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNRCASEH', '시나리오 케이스 실행 결과 파일', '6', '시나리오 케이스 모델 실행 결과 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);

INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNRCASEHI', '시나리오 케이스 실행 중간 파일', '7', '시나리오 케이스 모델 실행 중간 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);

-- Trade Flow 결과파일 코드. 2026-09-17 사용자 확인 기준 모든 대상 DB 적용 완료.
INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNRCASEHTF', '시나리오 케이스 실행 결과 Trade Flow', '8',
    '시나리오 케이스 모델 실행 결과 Trade Flow 파일 연결 코드',
    '20260907', '99991231', 'Y', NULL, NULL
);

-- 실행유형코드 공통코드 그룹 (신규 그룹이므로 detail 보다 먼저 등록)
INSERT INTO tcm_commcdm (
    cd_grp_id, cd_grp_nm, cd_grp_expn,
    use_bgn_dt, use_end_dt, use_yn
)
VALUES (
    'mdl_exec_type_cd', '수급모델실행유형코드', '수급모델 실행유형 (INDEPENDENT:독립실행 / SCENARIO:시나리오실행 / MI_BASE:현재 MI Base 지정)',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y'
);

-- 실행유형코드 공통코드 상세 (tmd_mdl_scnrcasem.exec_type_cd)
INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES
    ('mdl_exec_type_cd', 'INDEPENDENT', '독립실행',     '1', 'MI Fundamental Model 독립 실행', '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL),
    ('mdl_exec_type_cd', 'SCENARIO',    '시나리오실행', '2', 'MI 수급모델 시나리오 실행',     '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL),
    ('mdl_exec_type_cd', 'MI_BASE',     'MI Base',      '3', '현재 MI Base로 지정된 독립실행', '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL);
```

Apply ordering:

1. Create `tmd_mdl_scnrcasem`.
2. Create `tmd_mdl_scnrcasem_ix01`.
3. Create `tmd_mdl_scnrcasem_ix02`.
4. Create `tmd_mdl_scnrcasem_ix03`.
5. Create `tmd_mdl_scnrcaseh`.
6. Create `tmd_mdl_scnrcaseh_ix01`.
7. Register `SCNRCASE` (existing `file_rel_obj_spt_cd` group — `tcm_commcdd` only).
8. Register `SCNRCASEH` (existing `file_rel_obj_spt_cd` group — `tcm_commcdd` only).
9. Register `SCNRCASEHI` (existing `file_rel_obj_spt_cd` group — `tcm_commcdd` only). Independent of the table DDL; can be applied on its own in a later release.
10. Register `SCNRCASEHTF` (existing `file_rel_obj_spt_cd` group — `tcm_commcdd` only). 2026-09-17 사용자 확인 기준 모든 대상 DB 적용 완료.
11. Register `mdl_exec_type_cd` group in `tcm_commcdm` (new group — must precede its detail codes).
12. Register `mdl_exec_type_cd` codes in `tcm_commcdd` (`INDEPENDENT` / `SCENARIO` / `MI_BASE`).

### 기존 배포 환경 마이그레이션 (옛 이름 테이블이 이미 있는 경우)

기존 환경(예: dev)에는 과거 이름 세대의 테이블이 있을 수 있다: 1세대 `tmd_scncasem`/`tmd_scncaseh`, 2세대 `tmd_mdl_scncasem`/`tmd_mdl_scncaseh`. 보존할 데이터가 없음이 확인되었으므로(2026-06-10) 조건부 rename/ALTER 분기 대신 **drop & recreate** 로 적용한다. `DROP TABLE IF EXISTS` 는 idempotent 하므로 어떤 중간 적용 상태에서도 재실행할 수 있다.

```sql
-- 0) 안전 확인: drop 대상에 보존 데이터가 없는지 확인한다. 1건이라도 보존 대상이 있으면 이 전략을 중단하고 별도 migration 계획을 세운다.
SELECT 'tmd_scncasem' AS t, COUNT(*) AS cnt FROM tmd_scncasem
UNION ALL SELECT 'tmd_mdl_scncasem', COUNT(*) FROM tmd_mdl_scncasem;
-- (존재하지 않는 테이블은 해당 행을 빼고 실행)

-- 1) 옛 이름 테이블 제거 (자식/이력 먼저: FK 종속)
DROP TABLE IF EXISTS tmd_scncaseh;
DROP TABLE IF EXISTS tmd_mdl_scncaseh;
DROP TABLE IF EXISTS tmd_mdl_scnrcaseh;
DROP TABLE IF EXISTS tmd_scncasem;
DROP TABLE IF EXISTS tmd_mdl_scncasem;
DROP TABLE IF EXISTS tmd_mdl_scnrcasem;

-- 2) 새 이름 기준 재생성: 위 `## Apply` 의 CREATE TABLE / CREATE INDEX 블록을 그대로 실행한다.
```

- 기존 fundamental case row 는 backfill 불필요하다. `scnr_id` 는 `NULL` 이 정상값이다.
- 이 마이그레이션은 application 배포보다 먼저(또는 동시) 적용되어야 `ddl-auto: validate` 가 통과한다.
- 공통코드 `SCNRCASE`/`SCNRCASEH` 는 테이블 drop 과 무관하게 유지한다. 이미 등록된 환경에서는 `Apply` 의 INSERT 를 건너뛴다(pre-check 2 로 확인).

Apply guardrails:

- Execute common-code `INSERT` only after pre-check confirms target duplicates do not exist.
- If the DBA standard prefers idempotent `MERGE`/`INSERT ... SELECT WHERE NOT EXISTS` style, keep the same metadata values and adapt only the insertion pattern.
- `mod_dtm` auto-update remains application-managed (`UPDATE ... SET mod_dtm = NOW()`), not DB-trigger-managed in this draft.
- `run_seq` is retained as the file-linkage surrogate key because `tcm_filel.file_rel_obj_dstg_no` is `INT UNSIGNED` and cannot store `run_id VARCHAR(20)`. `run_seq` is also `INT UNSIGNED` so the file linkage is type-compatible; Java uses `Long` to cover the unsigned range.
- Java/JPA read-back rule: `run_seq` is a DB-owned non-PK `AUTO_INCREMENT` value. The entity maps it as read-only (`insertable = false`, `updatable = false`). After inserting `tmd_mdl_scnrcaseh`, the service must `flush` and then query `run_seq` by `run_id` in the same transaction before creating the Kubernetes Job. Do not assume `runHistory.getRunSeq()` is populated immediately after `save`.

### Fixed MI Base lifecycle schema

- `actv_base_yn`은 `del_yn = 'N' AND exec_type_cd = 'MI_BASE'`일 때 `Y`, 그 외에는 `NULL`인 STORED generated column이다.
- 새 지정 transaction은 current/target row를 안정된 순서로 write lock하고 기존 current를 `INDEPENDENT`로 복귀시킨 UPDATE를 flush한 뒤 대상을 `MI_BASE`로 변경한다. 승격 UPDATE가 먼저 실행되어 generated unique가 일시 충돌하지 않도록 순서를 명시적으로 보장한다.
- application은 `actv_base_yn`을 직접 쓰지 않는다.
- `tmd_mdl_scnrcasem_uq02 UNIQUE (actv_base_yn)`가 active `MI_BASE`를 최대 1건으로 제한한다.
- 과거 MI Base 지정 여부는 case type에 보존하지 않는다. 별도 이력 table도 이번 schema 범위에 추가하지 않는다.

### Incremental MI Base extension — current-only (2026-09-07 확정, 2026-09-15 적용 확인)

기본 schema가 이미 배포된 환경은 아래 순서로 적용한다. `actv_base_yn`은 `exec_type_cd`와 `del_yn`에서만 파생되므로 application이 직접 값을 쓰지 않는다. 2026-09-07 기준 기존 application 코드와 운영 적재 SQL·배치에는 새 `MI_BASE` row를 계속 생성하는 writer가 없음을 확인했다. 다만 pre-check 시작부터 unique 생성 완료까지 임시 운영 SQL을 포함한 `exec_type_cd = 'MI_BASE'` 변경은 금지한다.

```sql
-- 0) pre-check: column/index 중복과 현재 active MI Base를 확인한다.
SELECT column_name, data_type, generation_expression, extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'tmd_mdl_scnrcasem'
  AND column_name = 'actv_base_yn';

SHOW INDEX FROM tmd_mdl_scnrcasem
WHERE Key_name = 'tmd_mdl_scnrcasem_uq02';

SELECT case_id, exec_type_cd, case_stat_cd, last_run_id, del_yn
FROM tmd_mdl_scnrcasem
WHERE del_yn = 'N'
  AND exec_type_cd = 'MI_BASE'
ORDER BY case_id DESC;

SELECT MAX(case_id) AS keep_case_id
FROM tmd_mdl_scnrcasem
WHERE del_yn = 'N'
  AND exec_type_cd = 'MI_BASE';

-- legacy 보존 후보 적격성 확인.
-- 결과의 case_ok/run_ok/input_ok/price_ok가 모두 1이어야 한다.
SELECT c.case_id,
       (c.scnr_id IS NULL AND c.case_stat_cd = 'COMPLETED') AS case_ok,
       (h.run_id IS NOT NULL
        AND h.case_id = c.case_id
        AND h.exec_stat_cd = 'COMPLETED'
        AND h.del_yn = 'N') AS run_ok,
       (COALESCE(f.active_file_count, 0) = 3
        AND COALESCE(f.demand_count, 0) = 1
        AND COALESCE(f.supply_count, 0) = 1
        AND COALESCE(f.trade_count, 0) = 1
        AND COALESCE(f.single_role_file_count, 0) = 3) AS input_ok,
       EXISTS (
           SELECT 1
           FROM tmd_mdl_scnrcaseprcl p
           WHERE p.run_seq = h.run_seq
             AND p.del_yn = 'N'
       ) AS price_ok
FROM tmd_mdl_scnrcasem c
LEFT JOIN tmd_mdl_scnrcaseh h
  ON h.run_id = c.last_run_id
LEFT JOIN (
    SELECT file_rel_obj_dstg_no AS case_id,
           COUNT(*) AS active_file_count,
           SUM(UPPER(org_file_nm) LIKE '%DEMAND%') AS demand_count,
           SUM(UPPER(org_file_nm) LIKE '%SUPPLY%') AS supply_count,
           SUM(UPPER(org_file_nm) LIKE '%TRADE%') AS trade_count,
           SUM(((UPPER(org_file_nm) LIKE '%DEMAND%')
              + (UPPER(org_file_nm) LIKE '%SUPPLY%')
              + (UPPER(org_file_nm) LIKE '%TRADE%')) = 1) AS single_role_file_count
    FROM tcm_filel
    WHERE del_yn = 'N'
      AND file_rel_obj_spt_cd = 'SCNRCASE'
    GROUP BY file_rel_obj_dstg_no
) f
  ON f.case_id = c.case_id
WHERE c.case_id = :keep_case_id
  AND c.del_yn = 'N'
  AND c.exec_type_cd = 'MI_BASE';

-- 1) helper column apply: 위 pre-check에서 column이 없을 때만 실행한다.
ALTER TABLE tmd_mdl_scnrcasem
    ADD COLUMN actv_base_yn CHAR(1)
        AS (IF(del_yn = 'N' AND exec_type_cd = 'MI_BASE', 'Y', NULL)) STORED
        COMMENT '활성베이스여부 (active MI Base이면 Y, 그 외 NULL. 전역 current MI Base 1건 unique helper)';

-- 2) legacy backfill: 적격성 검증을 통과한 기존 규칙(case_id 최대)을 current로 보존한다.
-- active MI Base가 2건 이상이고 위 적격성 결과가 모두 1일 때만 실행한다.
-- :keep_case_id는 위 MAX(case_id), :migration_actor는 DBA 승인 식별자로 치환한다.
UPDATE tmd_mdl_scnrcasem
SET exec_type_cd = 'INDEPENDENT',
    mod_usr_logn_id = :migration_actor,
    mod_dtm = NOW()
WHERE del_yn = 'N'
  AND exec_type_cd = 'MI_BASE'
  AND case_id <> :keep_case_id;

-- 3) guard apply: active MI Base가 0~1건임을 확인한 뒤 index가 없을 때만 실행한다.
SELECT COUNT(*) AS active_mi_base_count
FROM tmd_mdl_scnrcasem
WHERE actv_base_yn = 'Y';

ALTER TABLE tmd_mdl_scnrcasem
    ADD CONSTRAINT tmd_mdl_scnrcasem_uq02 UNIQUE (actv_base_yn);

-- 4) verify
SELECT case_id, exec_type_cd, actv_base_yn, del_yn
FROM tmd_mdl_scnrcasem
WHERE actv_base_yn = 'Y';

SHOW CREATE TABLE tmd_mdl_scnrcasem;
SHOW INDEX FROM tmd_mdl_scnrcasem
WHERE Key_name = 'tmd_mdl_scnrcasem_uq02';
```

Expected result:

- 보존할 legacy row가 있으면 `case_ok`, `run_ok`, `input_ok`, `price_ok`가 모두 `1`이다. 하나라도 `0`이면 unique 적용을 중단하고 운영자가 그 row를 보완하거나 모든 legacy MI Base를 `INDEPENDENT`로 바꿔 current 없음으로 시작하는 별도 승인 SQL을 정한다.
- `active_mi_base_count`는 `0` 또는 `1`이다.
- active `MI_BASE`가 있으면 그 row만 `actv_base_yn = 'Y'`이고 모든 다른 row는 `NULL`이다.
- `uq02` 생성 후 기존 current를 먼저 해제하지 않은 채 서로 다른 두 active row를 직접 `MI_BASE`로 만드는 write는 unique 충돌로 실패한다. current가 있는 정상 교체 command는 공통 current row lock으로 직렬화되며, 두 요청이 순차 성공하면 마지막 commit 대상만 current로 남는다. current가 0건인 최초 지정 경합은 공통 current row가 없으므로 한 요청이 unique 충돌로 실패할 수 있다.
- legacy 중복 정리에서 `case_id`, `usr_no`, `case_stat_cd`, `last_run_id`, run/file/result row는 바뀌지 않는다.

Deployment order and compatibility:

1. 쓰기 API 배포 전 운영 구간을 확보하고, pre-check 시작부터 helper/unique 생성 검증 완료까지 임시 운영 SQL을 포함한 MI Base 변경을 금지한다.
2. old application과 기존 운영 적재 SQL·배치에는 MI Base writer가 없고 추가 generated column도 쓰지 않으므로 DDL 선행 배포와 호환된다. old application의 `MAX(case_id)` read는 active MI Base가 0~1건이면 같은 row를 반환한다.
3. DDL 검증 후 `ScenarioCase.actvBaseYn` read-only mapping과 MI Base 지정 API/application을 배포한다.
4. application 배포 후 서로 다른 두 case의 교체 및 concurrency focused test를 수행한다.

Incremental rollback/fallback:

- API에 문제가 있으면 지정 endpoint를 먼저 비활성화하거나 이전 application으로 rollback하고 `uq02`는 정합성 guard로 유지하는 forward-fix를 우선한다.
- 물리 rollback이 승인되면 generated column을 참조하지 않는 application으로 먼저 되돌린 뒤 `ALTER TABLE tmd_mdl_scnrcasem DROP INDEX tmd_mdl_scnrcasem_uq02`, `ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN actv_base_yn` 순서로 실행한다.
- rollback은 legacy backfill로 `INDEPENDENT` 복귀한 과거 row를 자동으로 `MI_BASE`로 되돌리지 않는다. 이전 분류 복원이 필요하면 pre-check 결과를 근거로 별도 DBA 승인 SQL을 작성한다.

## Backfill

No existing data backfill was required for the phase-1 mock-to-real-DB cutover.

If mock or external data must be migrated later, define a separate one-off import plan.

`exec_type_cd` backfill: column default is `INDEPENDENT`, so existing MI Fundamental Model rows (`scnr_id IS NULL`) need no action. Only re-classify rows that already carry a scenario identifier:

```sql
UPDATE tmd_mdl_scnrcasem
SET exec_type_cd = 'SCENARIO',
    mod_dtm = NOW()
WHERE scnr_id IS NOT NULL
  AND exec_type_cd <> 'SCENARIO';
```

Historical note: 별도 ingestion path가 MI Base row를 INSERT한다는 전제는 2026-09-03에 supersede되었다. 현재 운영은 기존 `INDEPENDENT` case의 `exec_type_cd`를 운영자가 `MI_BASE`로 변경하며, 이 작업은 초기 schema backfill에 포함되지 않는다.

## Rollback

Rollback must be executed only after checking that no live references remain in `tcm_filel`.

```sql
-- 1) reference safety check before common-code rollback
SELECT file_rel_obj_spt_cd, COUNT(*) AS ref_cnt
FROM tcm_filel
WHERE file_rel_obj_spt_cd IN ('SCNRCASE', 'SCNRCASEH', 'SCNRCASEHI', 'SCNRCASEHTF')
GROUP BY file_rel_obj_spt_cd;

-- 2) common-code rollback: execute only when reference count is zero
DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASE';

DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEH';

DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEHI';

DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEHTF';

-- 3) child/history rollback first
-- DROP TABLE 이 종속 제약(tmd_mdl_scnrcaseh_uq01, fk01)을 함께 제거하므로 DROP INDEX 불필요
DROP TABLE tmd_mdl_scnrcaseh;

-- 4) table rollback: execute only when master table removal is explicitly approved
-- DROP TABLE 이 종속 인덱스(tmd_mdl_scnrcasem_ix01, ix02, ix03)를 함께 제거하므로 DROP INDEX 불필요
DROP TABLE tmd_mdl_scnrcasem;

-- 4b) column-only rollback when keeping the table (revert scnr_id / active helpers / exec_type_cd addition)
-- ALTER TABLE tmd_mdl_scnrcasem DROP INDEX tmd_mdl_scnrcasem_uq02;
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN actv_base_yn;
-- ALTER TABLE tmd_mdl_scnrcasem DROP CHECK tmd_mdl_scnrcasem_ck01;
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN exec_type_cd;
-- DELETE FROM tcm_commcdd WHERE cd_grp_id = 'mdl_exec_type_cd' AND cd_id IN ('INDEPENDENT','SCENARIO','MI_BASE');
-- DELETE FROM tcm_commcdm WHERE cd_grp_id = 'mdl_exec_type_cd';  -- detail 전부 제거 후 그룹 제거
-- DROP INDEX tmd_mdl_scnrcasem_ix02 ON tmd_mdl_scnrcasem;
-- ALTER TABLE tmd_mdl_scnrcasem DROP INDEX tmd_mdl_scnrcasem_uq01;
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN actv_scnr_id;  -- generated column, unique 제거 후 drop
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN scnr_id;  -- exec_type_cd / ck01 제거 후 drop
```

Rollback rules:

- Do not delete `SCNRCASE`, `SCNRCASEH`, `SCNRCASEHI`, or `SCNRCASEHTF` common-code rows if any `tcm_filel` rows still reference them.
- Roll back child/history objects (`tmd_mdl_scnrcaseh`) before the master table (`tmd_mdl_scnrcasem`).
- Do not delete S3 objects automatically without explicit storage/DBA approval.
- If any application release already depends on `tmd_mdl_scnrcasem`, prefer forward-fix over table drop.

## Verify / EXPLAIN

Pre-rollout evidence already confirmed:

- `SCNRCASE + case_id` file lookup on production DB used `key = tcm_filel_ix01`, `type = ref`, `rows` small, and no full scan under `del_yn = 'N'`.
- 사용자 확인(2026-09-17): 모든 대상 DB에 `SCNRCASEHTF`가 등록됐고 `cd_rnk = 8` 중복이 없다. 이 세션에서 DB metadata를 직접 조회한 결과는 아니다.

```sql
-- 공통코드 적용값 확인
SELECT cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn, use_bgn_dt, use_end_dt, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEHTF';

-- 특정 run의 Trade Flow 결과파일 연결 확인
SELECT file_no, org_file_nm, file_size, del_yn, file_vald_end_dt
FROM tcm_filel
WHERE del_yn = 'N'
  AND file_rel_obj_spt_cd = 'SCNRCASEHTF'
  AND file_rel_obj_dstg_no = :run_seq
ORDER BY file_no;
```

### Timeout scheduler rollout pre-check and EXPLAIN

Do not apply these statements from application deployment. The DBA executes the pre-check, applies `tmd_mdl_scnrcaseh_ix01` only when absent, and retains the evidence with the scheduler rollout. Apply the `exec_bgn_dtm` default before deploying application code that omits this column on insert; otherwise newly created runs can retain a null start time on an older nullable schema.

```sql
-- FK가 생성한 case_id 인덱스와 scheduler index를 함께 확인한다.
-- case_id 인덱스가 존재하면 별도 단일-column index를 추가하지 않는다.
SHOW INDEX FROM tmd_mdl_scnrcaseh;

-- exec_bgn_dtm은 timeout 판정과 같은 DB 시각 기준으로 생성되어야 한다.
SHOW COLUMNS FROM tmd_mdl_scnrcaseh LIKE 'exec_bgn_dtm';
SELECT COUNT(*) AS null_exec_bgn_dtm_count
FROM tmd_mdl_scnrcaseh
WHERE exec_bgn_dtm IS NULL;

-- null row가 없음을 확인한 뒤에만 기존 table에 적용한다.
ALTER TABLE tmd_mdl_scnrcaseh
    MODIFY exec_bgn_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실행시작일시 (DB 시각으로 생성)';

-- tmd_mdl_scnrcaseh_ix01 이 없을 때만 실행한다.
CREATE INDEX tmd_mdl_scnrcaseh_ix01
    ON tmd_mdl_scnrcaseh (exec_stat_cd, exec_bgn_dtm, case_id);

-- Application의 실제 timeout 후보 조회와 동일한 EXPLAIN이다.
EXPLAIN
SELECT h.run_id
FROM tmd_mdl_scnrcaseh h
JOIN tmd_mdl_scnrcasem c
  ON c.case_id = h.case_id
WHERE h.exec_stat_cd = 'RUNNING'
  AND h.exec_bgn_dtm < NOW() - INTERVAL 10 MINUTE
  AND c.exec_type_cd IN ('SCENARIO', 'INDEPENDENT')
ORDER BY h.exec_bgn_dtm ASC, h.case_id ASC
LIMIT 100;

-- Rollback: scheduler release가 더 이상 이 index에 의존하지 않는 경우에만 DBA 승인 후 실행한다.
DROP INDEX tmd_mdl_scnrcaseh_ix01 ON tmd_mdl_scnrcaseh;
```

Expected result: timeout 후보 조회의 `h` access에 `key = tmd_mdl_scnrcaseh_ix01` 이 선택되어야 한다. `SHOW INDEX` 결과에서 `case_id`를 포함하는 FK implicit index가 있으면 그것을 유지하고, 중복 단일-column index는 만들지 않는다.

Add remaining verification SQL here after DB-backed queries are implemented.

Expected future checks:

- Verify duplicate-run guard under concurrent transaction simulation. The conditional `tmd_mdl_scnrcasem` update must report `affected rows = 1` for exactly one of two concurrent requests on the same case.
- Verify per-scenario active-run guard under concurrent insert simulation. 같은 `scnr_id` 로 `case_stat_cd = 'RUNNING'` row 2건을 동시에 INSERT 하면 generated `actv_scnr_id` 가 동일 값이 되어 정확히 1건만 성공하고 나머지는 `tmd_mdl_scnrcasem_uq01` unique 충돌이어야 한다. `scnr_id = NULL` (fundamental) row 는 RUNNING 이어도 `actv_scnr_id = NULL` 이라 다건이 허용되어야 한다. 추가로 RUNNING → terminal 전이 시 `actv_scnr_id` 가 자동으로 NULL 로 풀려(별도 UPDATE 없이) 같은 scenario 의 다음 실행 INSERT 가 허용되는지 확인한다.
- Verify `tmd_mdl_scnrcasem_ck01` invariant: an INSERT/UPDATE with `exec_type_cd = 'SCENARIO'` and `scnr_id IS NULL`, or with `exec_type_cd IN ('INDEPENDENT','MI_BASE')` and `scnr_id IS NOT NULL`, must be rejected by the CHECK constraint.
- Historical verification (2026-09-15 migration): legacy 보존 후보의 case/latest run/당시 input 3종/Price 적격성 검사는 적용 이력으로 유지한다. 현재 MI Base 지정의 적격 기준으로 사용하지 않는다.
- Verify current MI Base designation: case/latest completed run, 정확히 4개 input의 `DEMAND`/`SUPPLY`/`TRADE`/`OUTPUT` 역할별 1개와 Price 적격성을 검증하며 부적격이면 기존 current를 변경하지 않는다. 정확한 API 적격 계약은 `mi-fundamental-model-mi-base-designation-feature-spec.md`를 따른다.
- Verify `actv_base_yn` expression and `tmd_mdl_scnrcasem_uq02`: active `MI_BASE` row만 `Y`, 나머지는 `NULL`이며 서로 다른 두 active case를 동시에 지정해도 최종 active는 최대 1건이다. current가 있는 교체 경합에서 직렬화된 두 요청이 모두 성공하면 마지막 commit 대상이 current다. current 0건 최초 지정 경합에서는 한 요청이 unique 충돌로 실패할 수 있다.
- Verify designation-vs-run race: `claimRun`의 atomic predicate에 `exec_type_cd = 'INDEPENDENT'`가 포함되어 지정된 MI Base를 `RUNNING`으로 변경하지 않는다. update/delete도 같은 row-lock 또는 atomic exec-type predicate로 MI Base 변경을 차단한다.
- Verify one input-generation source snapshot pins one current `case_id` and its exact `DEMAND`/`SUPPLY`/`TRADE`/`OUTPUT` file metadata; designation during generation must not mix files from two bases.
- Verify changed `GET /cases` returns the current row in `miBase`, active `INDEPENDENT` rows in `cases`, excludes `SCENARIO`, and returns non-null `execType` for every item. 이전 MI Base는 새 지정 시 `INDEPENDENT`로 복귀해 `cases`에 포함된다. `totalCount`는 `miBase` 0|1 + filtered `cases.size()`이며 검색은 pinned `miBase`를 제외한 `cases`에만 적용한다.
- Verify `run_id` uniqueness on `tmd_mdl_scnrcaseh`.
- Verify `run_seq` UNIQUE constraint on `tmd_mdl_scnrcaseh`.
- Verify `SCNRCASE + case_id` file lookup (upload input files).
- Verify `SCNRCASEH + run_seq` file lookup (model execution output files).
- Verify `SCNRCASEHI + run_seq` file lookup (model execution intermediate files). 같은 `run_seq` 로 조회했을 때 output(`SCNRCASEH`)과 중간파일(`SCNRCASEHI`)이 서로 섞이지 않아야 한다.
- Verify `SCNRCASEHTF + run_seq` file lookup (Trade Flow result files). 같은 `run_seq`로 조회했을 때 output(`SCNRCASEH`), intermediate(`SCNRCASEHI`), tradeOutput(`SCNRCASEHTF`)이 서로 섞이지 않아야 한다.
- Verify `last_run_id` invariant: for every `tmd_mdl_scnrcasem` row with `last_run_id IS NOT NULL`, the referenced `tmd_mdl_scnrcaseh` row exists and its `exec_stat_cd` matches `case_stat_cd` in the terminal/RUNNING semantics defined by the workflow draft.
- Run `EXPLAIN` for callback latest-case sync SQL (`WHERE last_run_id = :run_id AND del_yn = 'N'`) and confirm `key = tmd_mdl_scnrcasem_ix03`.
- `tmd_mdl_scnrcaseh_ix01 (exec_stat_cd, exec_bgn_dtm, case_id)` supports the timeout scheduler. The scheduler queries `exec_stat_cd = 'RUNNING' AND exec_bgn_dtm < NOW() - INTERVAL 10 MINUTE`, joins the case row to limit `exec_type_cd IN ('SCENARIO', 'INDEPENDENT')`, and uses `case_id` as the join/covering tail; `MI_BASE` is excluded. Before production rollout, run `EXPLAIN` against the actual limited query and confirm `key = tmd_mdl_scnrcaseh_ix01`.
- Run `EXPLAIN` for canonical list SQL (`WHERE del_yn = 'N' ORDER BY crt_dtm DESC`) and confirm `key = tmd_mdl_scnrcasem_ix01` usage.
- Verify DB-backed update/delete affect only active owned case rows via `case_id + usr_no + del_yn = 'N'` predicate.
- After `ASSIGNEE_NAME` search SQL is implemented, run `EXPLAIN` against the JOIN query resolving `usr_no` to uploader name and confirm index usage on both sides of the JOIN.

## DBA Deployment Notes

- The base schema in this document has been reviewed with DBA/common-code owner and applied to production. 사용자 확인(2026-09-15)에 따라 generated `actv_base_yn`/`uq02` current-only extension은 testdb, 고객사 개발 DB, 운영 DB에 적용됐다. 사용자 확인(2026-09-17)에 따라 `SCNRCASEHTF`/`cd_rnk = 8`도 모든 대상 DB에 적용됐고 rank 충돌이 없다. 직접 DB metadata 조회와 경쟁 시나리오 검증은 별도 evidence로 남긴다.
- 기존 환경의 `mdl_exec_type_cd`/`MI_BASE` 설명이 `MI Base 적재` 의미로 남아 있으면 공통코드 owner 승인 후 `현재 MI Base로 지정된 독립실행` 의미로 동기화한다. 코드값은 변경하지 않는다.
- Phase-1 API is now DB-backed against this DDL, not the in-memory mock store.
- 과거 `scenario_id` 컬럼명과 물리 FK 전제는 제거되었다. 현재 proposed model은 `tmd_mdl_scnrcasem.scnr_id` nullable 논리 참조를 사용하며, MI Fundamental Model row는 `NULL`, MI 수급모델 row는 scenario 식별자를 저장한다. rollout 전에는 `scnr_id` 기반 active-run guard(존재 확인 query + `tmd_mdl_scnrcasem_uq01` unique), list index 선택, JOIN query 실행계획을 DBA review로 확정해야 한다.

## MI Base Extension ERD Review

### Change classification

- existing table constraint/index extension
- 신규 `actv_base_yn` STORED generated column 1개
- 신규 nullable unique `tmd_mdl_scnrcasem_uq02` 1개
- 신규 table/FK/common code/backfill history table 없음

### Normalization assessment

| Normal form | Result |
|---|---|
| 1NF | Pass. `actv_base_yn`은 반복 그룹이나 배열이 아닌 단일 파생값이다. |
| 2NF | Pass. PK가 단일 `case_id`이므로 부분 함수 종속이 없다. |
| 3NF | Pass. `actv_base_yn`은 `exec_type_cd`와 `del_yn`에서 결정되는 derived helper다. |
| BCNF | Pass for business columns. `actv_base_yn`은 current 단건 invariant helper다. |

### Index strategy

- `actv_base_yn`은 선택도가 낮지만 조회 성능용 index가 아니라 전역 current MI Base 최대 1건을 강제하는 integrity constraint다.
- non-null 값은 최대 1건이고 나머지는 `NULL`이므로 write amplification과 저장공간 증가는 제한적이다.
- 기존 목록의 current MI Base 조회는 `uq02`의 `actv_base_yn = 'Y'` 조건을 사용할 수 있다. 실제 구현 후 `EXPLAIN`에서 `key = tmd_mdl_scnrcasem_uq02`, `type = const|ref`인지 확인한다.
- 기존 `ix01`~`ix03`, `uq01`, FK index와 역할이 겹치지 않으므로 다른 index를 제거하지 않는다.

### Dictionary updates

- 없음. 컬럼명은 기존 등록 용어 `ACTV`(활성), `BASE`(베이스), `YN`(여부)을 조합한다.

### Implementation readiness

- schema status: `blocked by DBA review/rollout`
- application compatibility: generated column은 old application이 쓰지 않고 기존 application·운영 적재 SQL·배치에도 MI Base writer가 없으므로 지정 API보다 먼저 배포할 수 있다. DDL 실행 구간의 임시 MI Base 변경 금지, current seed, 조회 전환 순서는 DBA/application rollout에서 확정한다.
- go condition: legacy current seed 적격성 통과 또는 current 없음 승인, DDL 실행 구간의 MI Base 변경 금지 확인, current marker 0~1건, generated column/unique 적용과 검증 완료
- no-go condition: 보존 후보의 case/run/input/Price 적격성 실패, current를 결정할 수 없거나 migration 대상이 예상과 다름, 예상하지 못한 MI Base writer 발견, DDL lock/실행시간이 운영 허용 범위를 초과함
