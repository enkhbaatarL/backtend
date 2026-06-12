# Scenario Case Execution ERD / DDL Draft

- domain: gas
- status: template / not rollout-ready
- owner: platform BE
- updated at: 2026-06-07 (output result persistence canonical document reference added)
- canonical design input: [scenario-case-execution-workflow-design-draft.md](scenario-case-execution-workflow-design-draft.md)
- related canonical output result persistence: [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)

## Purpose

This document is the canonical home for future DB-backed rollout SQL for Scenario Case execution.

Current status: no phase-1 mock API implementation depends on this DDL. Do not apply this document to any database until all unresolved decisions are closed and DBA review is complete.

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
- 꼬리 컬럼 `case_id` 는 "시나리오별 최근 케이스(실행상태 조건 포함) 조회" 를 인덱스만으로 처리하기 위함이다. `case_id` 는 auto-increment 이므로 `WHERE scnr_id = ? AND case_stat_cd = ? ORDER BY case_id DESC LIMIT 1` 가 정렬 비용 없이 최신 1건을 반환한다.
- `scnr_id` 는 외부 scenario aggregate(`tmm_mi_scenario_m`) 에 대한 논리 참조이며 물리 FK 는 걸지 않는다(`last_run_id` 와 동일한 application-validated 참조 방침). 참조 대상 실물은 `tmm_mi_scenario_m.scenario_id INT UNSIGNED` 로 확인되었고(2026-06-10, 구 명칭 `tmm_scenariom` 은 superseded), 타입이 `scnr_id INT UNSIGNED` 와 일치한다. 참조 대상은 full-word 컬럼 컨벤션(MI/agent 팀)을 쓰지만 플랫폼 `tmd_` 테이블 컬럼명은 용어사전 약어(시나리오=SCNR)를 따른다.
- 명명 rename (2026-06-10): 용어사전(시나리오=SCNR, 케이스=CASE) 정합화를 위해 `scncase` → `scnrcase`, `scn_id` → `scnr_id` 로 모든 테이블/인덱스/제약 이름을 일괄 변경한다. 공통코드 `cd_id` 값도 동일한 용어사전 정합 기준으로 `SCNCASE` → `SCNRCASE`, `SCNCASEH` → `SCNRCASEH` 로 변경한다(기존 "공통코드 값은 변경하지 않는다" 결정은 superseded). 기존 환경 반영은 조건부 rename/ALTER 분기 대신 drop & recreate 전략을 따른다(아래 마이그레이션 섹션).

2026-06-11 Notion evidence sync:

- Reviewed Notion pages `MI Fundamental Model`, `MI Fundamental Model 실행`, and `Fundamental Model Run Callback API Spec`.
- The Notion `MI Fundamental Model 실행` page still contains a historical DDL block using `scn_id`, `run_seq BIGINT UNSIGNED`, and `tmd_mdl_scnrcaseh_ix01 (exec_stat_cd, exec_bgn_dtm)`.
- Those Notion DDL details are reference-only and are superseded by this DDL draft:
  - use `scnr_id`, not `scn_id`;
  - use `run_seq INT UNSIGNED AUTO_INCREMENT UNIQUE`, not `BIGINT UNSIGNED`;
  - do not create `tmd_mdl_scnrcaseh_ix01` until the timeout scheduler query is implemented and verified;
  - keep `tmd_mdl_scnrcasem_ix03 (last_run_id, del_yn)` for callback latest-case status sync.
- No additional owned table DDL is missing for the current case/run scope. `tcm_filel` is an existing common table and is referenced through compatibility/pre-check SQL instead of being redefined here.

## Scope

### In Scope

- Future physical table/column DDL for `tmd_mdl_scnrcasem` and `tmd_mdl_scnrcaseh`
- Future common-code rollout for `SCNRCASE` and `SCNRCASEH`
- Future index and verification SQL for DB-backed duplicate-run guard and timeout scheduler
- Future DB-backed query shape for case detail lookup
- Future DB-backed update/delete predicate shape for active case metadata update and soft delete
- Raw output file linkage via `SCNRCASEH + run_seq`

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
- no common-code change
- no DBA rollout action

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

## Closed Decisions

- `run_id` format and PK (2026-05-21): backend-generated `run_id` format is `yyyymmdd_hhmmss_XXXX`. `tmd_mdl_scnrcaseh.run_id` is `VARCHAR(20) PRIMARY KEY`. `tmd_mdl_scnrcasem.last_run_id` is `VARCHAR(20)`.
- `run_seq` surrogate key (2026-05-22, type fixed 2026-06-11): `tmd_mdl_scnrcaseh`에 `run_seq INT UNSIGNED AUTO_INCREMENT UNIQUE`를 추가한다. `file_rel_obj_dstg_no`가 `INT UNSIGNED` 계열이어서 `run_id`(`VARCHAR(20)`)를 직접 저장할 수 없는 제약을 해결하기 위한 surrogate key다. output 파일은 `SCNRCASEH` / `run_seq`로 `tcm_filel`에 연결한다. `run_id`는 model pod 연동 키로 그대로 유지한다. Java 타입은 unsigned 범위를 보존하기 위해 `Long`을 사용한다. 이전 "no separate surrogate key" 결정과 `BIGINT UNSIGNED` 후보는 이 결정으로 superseded된다.
- `last_run_id` column (2026-05-19, 2026-05-21 length updated): added to `tmd_mdl_scnrcasem` as `VARCHAR(20) NULL`, an application-validated latest execution pointer. No physical FK to `tmd_mdl_scnrcaseh.run_id`. Update order and the `case_stat_cd` / `last_run_id` invariant are enforced by the application within a single transaction. See workflow design draft §2 and §5.
- input file linkage strategy (2026-06-05): DB-backed `POST /cases`는 `TEMP/0` 임시 relation을 거치지 않고 `case_id` 확보 후 `SCNRCASE + case_id` relation으로 직접 업로드한다. 파일 3개 중 하나라도 실패하면 API 전체를 실패로 처리하며, 이미 성공한 업로드 분은 `createCase()` service가 file delete primitive를 호출해 보상 삭제한다.
- `tcm_filel` compatibility / index coverage (2026-06-05): 운영 DB에서 `tcm_filel.file_rel_obj_dstg_no = INT UNSIGNED`, `tcm_filel_ix01 (file_rel_obj_spt_cd, file_rel_obj_dstg_no)` 존재를 확인했다. `WHERE del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = :case_id` EXPLAIN 결과 `key = tcm_filel_ix01`, `type = ref`, `rows` small, no full scan으로 확인되었다.
- Result payload / pointer columns (2026-05-19): `req_param_json` and `rslt_data_json` are not in scope. Request information is tracked via case metadata and `tcm_filel` linkage; result payload/files are owned by the external model. Revisit only when a result-query / download feature spec is defined.
- `scenario_id` column-name removal (2026-05-20, superseded by 2026-06-10 `scnr_id` re-introduction): the old `scenario_id` column name and physical FK assumption were removed. The current proposed model uses nullable `scnr_id` as an application-validated logical scenario reference for MI supply-model rows.
- `tmd_mdl_` prefix rename (2026-06-10): 모든 모델 실행 관련 테이블 이름에 표준단어 `모델(MDL)` 을 붙여 `tmd_` 다음에 `mdl_` 을 삽입한다(당시 이름 기준 `tmd_scncasem → tmd_mdl_scncasem` 등 6테이블). 같은 날 아래 `scnrcase` rename 결정이 더해져 최종 이름은 `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, `tmd_mdl_scnrcasevoll`, `tmd_mdl_scnrcasetrdehdrl`, `tmd_mdl_scnrcasetrdeflwl`, `tmd_mdl_scnrcaseprcl` 이다. 인덱스/제약 이름도 최종 테이블명 기준으로 정합화한다. 표준단어 출처: `docs/conventions/db-term-dictionary-sheet2.csv` (모델=MDL).
- `scnrcase` 표준단어 정합 rename (2026-06-10): 용어사전에 시나리오=SCNR, 케이스=CASE 로 등록되어 있어 사전에 없는 `scn` 약어를 쓰던 `scncase` 를 `scnrcase` 로, `scn_id` 를 `scnr_id` 로 일괄 변경한다(ERD 리뷰 P2 반영). 공통코드 `cd_id` 값도 같은 기준으로 `SCNCASE` → `SCNRCASE`, `SCNCASEH` → `SCNRCASEH` 로 변경한다(기존 "공통코드 값은 변경하지 않는다" 결정은 superseded). 기존 환경 반영은 drop & recreate 전략을 따른다.
- `scnr_id` re-introduction (2026-06-10, supersedes the 2026-05-20 removal): nullable `scnr_id INT UNSIGNED NULL` is re-added to `tmd_mdl_scnrcasem` so that `MI 수급모델 실행` reuses `tmd_mdl_scnrcasem` + `tmd_mdl_scnrcaseh` instead of a separate `tmd_scnrmdlexech` table. Fundamental rows keep `scnr_id = NULL`; supply-model rows carry the scenario identifier. Mapping is `scenario : case = 1:N` (a new `case_id` per execution, shared `scnr_id`). `scnr_id` is an application-validated logical reference to the external scenario aggregate (`tmm_mi_scenario_m.scenario_id INT UNSIGNED`, 2026-06-10 실물 스키마 확인, 구 명칭 `tmm_scenariom` superseded) with no physical FK. `tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` 는 active 존재 확인/최신 케이스 조회용 인덱스다. See [mi-scenario-model-execution-workflow-design.md](mi-scenario-model-execution-workflow-design.md).
- per-scenario active-run guard DB 보강 (2026-06-10): `tmd_mdl_scnrcasem` 에 `actv_scnr_id INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED` generated column 과 `tmd_mdl_scnrcasem_uq01 UNIQUE (actv_scnr_id)` 을 추가한다. helper 는 `case_stat_cd`/`scnr_id` 에서 DB 가 자동 파생하므로 invariant(`RUNNING` 이면 `scnr_id`, 그 외 `NULL`)가 코드 동기화 없이 보장된다. 수급모델 실행 접수는 새 case INSERT 가 동일 scenario active row 와 `actv_scnr_id` 중복으로 unique 충돌하면 active 실행 존재로 보고 conflict(409)로 매핑한다. fundamental row 는 `scnr_id = NULL` → `actv_scnr_id = NULL` 이라 unique 가 바인딩되지 않아 case 단위 conditional update 가드와 충돌하지 않는다. `scnr_id` 기준 RUNNING 존재 확인(ix02)은 1차 가드, unique 는 DB 최후 방어선이다. 표준단어: 활성=ACTV. (2026-06-10 helper 를 수동 `actv_yn CHAR(1)` 에서 generated column 으로 변경 — drift 제거)
- Volumes/Trade Flows/Price 결과 데이터 저장 (2026-06-07): 1차 mock은 json seed 기반 in-memory 응답으로 처리. DB 테이블 미생성. DB-backed 전환 시 parsed result data는 `tmd_mdl_scnrcaseh.run_seq` 기준으로 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)의 테이블/키/인덱스/파티션 전략을 따른다.

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
  AND cd_id IN ('SCNRCASE', 'SCNRCASEH');

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
- Confirm no active duplicate rows already exist for `SCNRCASE` / `SCNRCASEH` in `file_rel_obj_spt_cd`.
- Confirm `tmd_mdl_scnrcasem_ix01 (del_yn, crt_dtm)` is the retained list index and `tmd_mdl_scnrcasem_ix02 (scnr_id, case_stat_cd, case_id)` backs the per-scenario active-run existence lookup for `MI 수급모델 실행` (2026-06-10).
- Confirm `tmd_mdl_scnrcasem_ix03 (last_run_id, del_yn)` backs callback latest-case status sync (`WHERE last_run_id = :run_id AND del_yn = 'N'`) without a full scan.
- Confirm `actv_scnr_id` 가 STORED generated column(`IF(case_stat_cd='RUNNING', scnr_id, NULL)`)으로 생성되었고 `tmd_mdl_scnrcasem_uq01 (actv_scnr_id)` unique 가 걸렸는지, fundamental row(`scnr_id = NULL` → `actv_scnr_id = NULL`) 다건이 unique 에 걸리지 않는지 확인한다 (MySQL unique 는 NULL row 를 중복 검사에서 제외).
- Confirm `tmd_mdl_scnrcasem_uq01`, `tmd_mdl_scnrcaseh_uq01` and `tmd_mdl_scnrcaseh_fk01` names do not collide with existing objects.
- Confirm `ASSIGNEE_NAME` JOIN path remains `tmd_mdl_scnrcasem.usr_no = tmd_usrm.usr_no`, display column `tmd_usrm.usr_nm`.
- Confirm `last_run_id VARCHAR(20) NULL` remains an application-validated pointer, not a physical FK or unique key.

## Apply

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
    case_titl_nm     VARCHAR(200)                          NOT NULL COMMENT '케이스제목명',
    case_expn        TEXT                                  NULL     COMMENT '케이스설명',
    case_stat_cd     VARCHAR(20) DEFAULT 'PENDING'         NOT NULL COMMENT '케이스상태코드',
    last_run_id      VARCHAR(20)                           NULL     COMMENT '최신실행ID (tmd_mdl_scnrcaseh.run_id 논리참조, 물리 FK 없음)',
    actv_scnr_id     INT UNSIGNED AS (IF(case_stat_cd = 'RUNNING', scnr_id, NULL)) STORED COMMENT '활성시나리오ID (case_stat_cd=RUNNING 이면 scnr_id, 그 외 NULL. scenario 당 active 1건 unique helper. case_stat_cd/scnr_id 에서 자동 파생되는 STORED generated column)',
    CONSTRAINT tmd_mdl_scnrcasem_uq01
        UNIQUE (actv_scnr_id)
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
    exec_bgn_dtm    DATETIME                              NULL COMMENT '실행시작일시',
    exec_end_dtm    DATETIME                              NULL COMMENT '실행종료일시',
    CONSTRAINT tmd_mdl_scnrcaseh_uq01
        UNIQUE (run_seq),
    CONSTRAINT tmd_mdl_scnrcaseh_fk01
        FOREIGN KEY (case_id) REFERENCES tmd_mdl_scnrcasem (case_id)
)
    COMMENT 'TMD_시나리오케이스이력';

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
```

Apply ordering:

1. Create `tmd_mdl_scnrcasem`.
2. Create `tmd_mdl_scnrcasem_ix01`.
3. Create `tmd_mdl_scnrcasem_ix02`.
4. Create `tmd_mdl_scnrcasem_ix03`.
5. Create `tmd_mdl_scnrcaseh`.
6. Register `SCNRCASE`.
7. Register `SCNRCASEH`.

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

## Backfill

No existing data backfill is expected for phase-1 mock.

If mock or external data must be migrated later, define a separate one-off import plan.

## Rollback

Rollback must be executed only after checking that no live references remain in `tcm_filel`.

```sql
-- 1) reference safety check before common-code rollback
SELECT file_rel_obj_spt_cd, COUNT(*) AS ref_cnt
FROM tcm_filel
WHERE del_yn = 'N'
  AND file_rel_obj_spt_cd IN ('SCNRCASE', 'SCNRCASEH')
GROUP BY file_rel_obj_spt_cd;

-- 2) common-code rollback: execute only when reference count is zero
DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASE';

DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEH';

-- 3) child/history rollback first
-- DROP TABLE 이 종속 제약(tmd_mdl_scnrcaseh_uq01, fk01)을 함께 제거하므로 DROP INDEX 불필요
DROP TABLE tmd_mdl_scnrcaseh;

-- 4) table rollback: execute only when master table removal is explicitly approved
-- DROP TABLE 이 종속 인덱스(tmd_mdl_scnrcasem_ix01, ix02, ix03)를 함께 제거하므로 DROP INDEX 불필요
DROP TABLE tmd_mdl_scnrcasem;

-- 4b) column-only rollback when keeping the table (revert scnr_id / actv_scnr_id addition)
-- DROP INDEX tmd_mdl_scnrcasem_ix02 ON tmd_mdl_scnrcasem;
-- ALTER TABLE tmd_mdl_scnrcasem DROP INDEX tmd_mdl_scnrcasem_uq01;
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN actv_scnr_id;  -- generated column, unique 제거 후 drop
-- ALTER TABLE tmd_mdl_scnrcasem DROP COLUMN scnr_id;
```

Rollback rules:

- Do not delete `SCNRCASE` or `SCNRCASEH` common-code rows if any `tcm_filel` rows still reference them.
- Roll back child/history objects (`tmd_mdl_scnrcaseh`) before the master table (`tmd_mdl_scnrcasem`).
- Do not delete S3 objects automatically without explicit storage/DBA approval.
- If any application release already depends on `tmd_mdl_scnrcasem`, prefer forward-fix over table drop.

## Verify / EXPLAIN

Pre-rollout evidence already confirmed:

- `SCNRCASE + case_id` file lookup on production DB used `key = tcm_filel_ix01`, `type = ref`, `rows` small, and no full scan under `del_yn = 'N'`.

Add remaining verification SQL here after DB-backed queries are implemented.

Expected future checks:

- Verify duplicate-run guard under concurrent transaction simulation. The conditional `tmd_mdl_scnrcasem` update must report `affected rows = 1` for exactly one of two concurrent requests on the same case.
- Verify per-scenario active-run guard under concurrent insert simulation. 같은 `scnr_id` 로 `case_stat_cd = 'RUNNING'` row 2건을 동시에 INSERT 하면 generated `actv_scnr_id` 가 동일 값이 되어 정확히 1건만 성공하고 나머지는 `tmd_mdl_scnrcasem_uq01` unique 충돌이어야 한다. `scnr_id = NULL` (fundamental) row 는 RUNNING 이어도 `actv_scnr_id = NULL` 이라 다건이 허용되어야 한다. 추가로 RUNNING → terminal 전이 시 `actv_scnr_id` 가 자동으로 NULL 로 풀려(별도 UPDATE 없이) 같은 scenario 의 다음 실행 INSERT 가 허용되는지 확인한다.
- Verify `run_id` uniqueness on `tmd_mdl_scnrcaseh`.
- Verify `run_seq` UNIQUE constraint on `tmd_mdl_scnrcaseh`.
- Verify `SCNRCASE + case_id` file lookup (upload input files).
- Verify `SCNRCASEH + run_seq` file lookup (model execution output files).
- Verify `last_run_id` invariant: for every `tmd_mdl_scnrcasem` row with `last_run_id IS NOT NULL`, the referenced `tmd_mdl_scnrcaseh` row exists and its `exec_stat_cd` matches `case_stat_cd` in the terminal/RUNNING semantics defined by the workflow draft.
- Run `EXPLAIN` for callback latest-case sync SQL (`WHERE last_run_id = :run_id AND del_yn = 'N'`) and confirm `key = tmd_mdl_scnrcasem_ix03`.
- `tmd_mdl_scnrcaseh_ix01 (exec_stat_cd, exec_bgn_dtm)` was removed (2026-06-10): no current query uses it and the table is small. When the timeout scheduler (scan `exec_stat_cd = 'RUNNING'` rows older than a threshold by `exec_bgn_dtm`) is implemented, re-add this index and run `EXPLAIN` against the actual query to confirm usage.
- Run `EXPLAIN` for canonical list SQL (`WHERE del_yn = 'N' ORDER BY crt_dtm DESC`) and confirm `key = tmd_mdl_scnrcasem_ix01` usage.
- Verify DB-backed update/delete affect only active owned case rows via `case_id + usr_no + del_yn = 'N'` predicate.
- After `ASSIGNEE_NAME` search SQL is implemented, run `EXPLAIN` against the JOIN query resolving `usr_no` to uploader name and confirm index usage on both sides of the JOIN.

## DBA Deployment Notes

- This document must be reviewed with DBA/common-code owner before rollout.
- Phase-1 mock API can proceed without this DDL.
- 과거 `scenario_id` 컬럼명과 물리 FK 전제는 제거되었다. 현재 proposed model은 `tmd_mdl_scnrcasem.scnr_id` nullable 논리 참조를 사용하며, MI Fundamental Model row는 `NULL`, MI 수급모델 row는 scenario 식별자를 저장한다. rollout 전에는 `scnr_id` 기반 active-run guard(존재 확인 query + `tmd_mdl_scnrcasem_uq01` unique), list index 선택, JOIN query 실행계획을 DBA review로 확정해야 한다.
