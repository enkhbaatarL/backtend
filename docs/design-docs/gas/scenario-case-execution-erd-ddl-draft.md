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

2026-05-20 정합성 메모:

- MI Fundamental Model은 MI Scenario Workbench와 동급 메뉴이며, 1차 mock canonical API path는 `/api/v1/mi/fundamental-model/**` 이다.
- `Scenario Case` 명칭은 현업 용어로 유지하지만 API parent가 `Scenario`라는 뜻은 아니다.
- 1차 mock canonical contract에는 `scenarioId`가 없다.
- 이 문서의 proposed model은 `tmd_scncasem.scenario_id` 컬럼 및 `scenario_id` 기반 FK/논리 참조/query/index를 포함하지 않는다.
- `run_id` 는 backend가 `yyyymmdd_hhmmss_XXXX` 포맷으로 생성하며, `tmd_scncaseh.run_id` 와 `tmd_scncasem.last_run_id` 는 `VARCHAR(20)` 기준으로 검토한다.
- change note: 과거 `scenario_id` 전제는 superseded로 분류하며, 실제 DB/DDL 적용 SQL은 지금 확정하지 않는다. DB-backed 전환 전 후속 ERD/DBA review에서 case ownership, permission scope, list query/index shape를 확정해야 한다.

## Scope

### In Scope

- Future physical table/column DDL for `tmd_scncasem` and `tmd_scncaseh`
- Future common-code rollout for `SCNCASE` and `SCNCASEH`
- Future index and verification SQL for DB-backed duplicate-run guard and timeout scheduler
- Future DB-backed query shape for case detail lookup
- Future DB-backed update/delete predicate shape for active case metadata update and soft delete
- Raw output file linkage via `SCNCASEH + run_seq`

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

수정은 `tmd_scncasem.case_titl_nm`, `tmd_scncasem.case_expn`에 해당하는 metadata만 변경한다. 삭제는 physical delete가 아니라 `tmd_scncasem.del_yn = 'Y'`에 해당하는 soft-delete semantics만 반영한다. phase-1 mock에서는 record의 `delYn` 필드로 이 동작을 모사한다.

Future DB-backed 전환 시 expected query shape:

1. `tmd_scncasem`에서 `case_id`, `usr_no` ownership scope, `del_yn = 'N'` 기준으로 case master를 조회한다.
2. `tcm_filel`에서 `file_rel_obj_spt_cd = 'SCNCASE'` 및 `file_rel_obj_dstg_no = :case_id` 기준으로 업로드 파일 metadata를 조회한다.
3. `tmd_scncasem.last_run_id`가 null이 아니면 `tmd_scncaseh.run_id = last_run_id`로 latest execution row 1건을 조회한다.
   - API `latestRun.startedAt` maps to `tmd_scncaseh.exec_bgn_dtm`.
   - API `latestRun.finishedAt` maps to `tmd_scncaseh.exec_end_dtm`.
   - FE calculates elapsed time from these two fields; no separate duration column or response field is required for phase 1.
4. result payload/download 관련 테이블 또는 컬럼은 조회하지 않는다.

DB-backed 구현 전에는 위 query shape에 대해 ownership 조건, `tcm_filel` 연결 인덱스, `run_id` unique/index 사용 여부를 EXPLAIN으로 검증해야 한다.

Future DB-backed update shape:

```sql
UPDATE tmd_scncasem
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
- files, `last_run_id`, `case_stat_cd`, `tmd_scncaseh`, result 관련 데이터는 갱신하지 않는다.

Future DB-backed delete shape:

```sql
UPDATE tmd_scncasem
SET del_yn = 'Y',
    mod_dtm = NOW(),
    mod_usr_logn_id = :actor
WHERE case_id = :case_id
  AND usr_no = :usr_no
  AND del_yn = 'N';
```

- predicate는 `case_id + usr_no ownership + del_yn = 'N'`을 반드시 포함한다.
- delete는 `del_yn = 'Y'`, `mod_*` columns만 갱신한다.
- `tcm_filel`, `tmd_scncaseh`, latestRun/result payload는 물리 삭제하지 않는다.
- affected row count가 `0`이면 active resource 관점의 `404 RSC_NOT_FOUND`로 처리한다.

## Ownership / Common-Code Decisions

- ownership scope는 `usr_no` only로 확정한다.
- `ASSIGNEE_NAME` 검색 JOIN 경계는 `tmd_scncasem.usr_no = tmd_usrm.usr_no`, 표시 컬럼은 `tmd_usrm.usr_nm`으로 확정한다.
- `SCNCASE` target duplicate status was confirmed clear on 2026-05-19. Draft metadata is `cd_nm = '시나리오 케이스 입력 파일'`, `cd_expn = '시나리오 케이스 입력 파일 연결 코드'`. Final registration still requires DBA/common-code owner confirmation.
- `SCNCASEH` common-code (output file linkage via `run_seq`) must also be registered. Target duplicate status confirmed clear on 2026-05-26. Draft metadata is `cd_nm = '시나리오 케이스 실행 결과 파일'`, `cd_expn = '시나리오 케이스 모델 실행 결과 파일 연결 코드'`. Final registration still requires DBA/common-code owner confirmation.

## Closed Decisions

- `run_id` format and PK (2026-05-21): backend-generated `run_id` format is `yyyymmdd_hhmmss_XXXX`. `tmd_scncaseh.run_id` is `VARCHAR(20) PRIMARY KEY`. `tmd_scncasem.last_run_id` is `VARCHAR(20)`.
- `run_seq` surrogate key (2026-05-22): `tmd_scncaseh`에 `run_seq BIGINT UNSIGNED AUTO_INCREMENT UNIQUE`를 추가한다. `file_rel_obj_dstg_no`가 `INT UNSIGNED` 계열이어서 `run_id`(`VARCHAR(20)`)를 직접 저장할 수 없는 제약을 해결하기 위한 surrogate key다. output 파일은 `SCNCASEH` / `run_seq`로 `tcm_filel`에 연결한다. `run_id`는 model pod 연동 키로 그대로 유지한다. 이전 "no separate surrogate key" 결정은 이 결정으로 superseded된다.
- `last_run_id` column (2026-05-19, 2026-05-21 length updated): added to `tmd_scncasem` as `VARCHAR(20) NULL`, an application-validated latest execution pointer. No physical FK to `tmd_scncaseh.run_id`. Update order and the `case_stat_cd` / `last_run_id` invariant are enforced by the application within a single transaction. See workflow design draft §2 and §5.
- input file linkage strategy (2026-06-05): DB-backed `POST /cases`는 `TEMP/0` 임시 relation을 거치지 않고 `case_id` 확보 후 `SCNCASE + case_id` relation으로 직접 업로드한다. 파일 3개 중 하나라도 실패하면 API 전체를 실패로 처리하며, 이미 성공한 업로드 분은 `createCase()` service가 file delete primitive를 호출해 보상 삭제한다.
- `tcm_filel` compatibility / index coverage (2026-06-05): 운영 DB에서 `tcm_filel.file_rel_obj_dstg_no = INT UNSIGNED`, `tcm_filel_ix01 (file_rel_obj_spt_cd, file_rel_obj_dstg_no)` 존재를 확인했다. `WHERE del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNCASE' AND file_rel_obj_dstg_no = :case_id` EXPLAIN 결과 `key = tcm_filel_ix01`, `type = ref`, `rows` small, no full scan으로 확인되었다.
- Result payload / pointer columns (2026-05-19): `req_param_json` and `rslt_data_json` are not in scope. Request information is tracked via case metadata and `tcm_filel` linkage; result payload/files are owned by the external model. Revisit only when a result-query / download feature spec is defined.
- `scenario_id` removal (2026-05-20): `tmd_scncasem` is treated as an independent Scenario Case aggregate keyed by `case_id`; `scenario_id` column and related index/query assumptions were removed from the proposed model.
- Volumes/Trade Flows/Price 결과 데이터 저장 (2026-06-07): 1차 mock은 json seed 기반 in-memory 응답으로 처리. DB 테이블 미생성. DB-backed 전환 시 parsed result data는 `tmd_scncaseh.run_seq` 기준으로 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)의 테이블/키/인덱스/파티션 전략을 따른다.

## Candidate DDL (Design Review Only — Not Rollout SQL)

설계 검토용 candidate DDL은 [scenario-case-execution-workflow-design-draft.md](scenario-case-execution-workflow-design-draft.md) 의 `## Candidate DDL - Not Rollout SQL / Persistence 영향 초안` 섹션에서 관리한다.

- 해당 섹션의 SQL은 workflow 설계 근거(conditional update, timeout 인덱스 등)와 함께 읽혀야 의미가 완전하므로 workflow draft에 유지한다.
- 이 섹션에는 candidate SQL을 복사하지 않는다.
- DBA review가 완료되고 open decision이 모두 닫히면 아래 `## Apply` 섹션에 final rollout SQL을 작성한다.

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
  AND file_rel_obj_spt_cd = 'SCNCASE'
  AND file_rel_obj_dstg_no = :case_id;

-- 2) common-code group and target code duplication check
SELECT cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id IN ('SCNCASE', 'SCNCASEH');

-- 3) existing table/object duplication check
SHOW TABLES LIKE 'tmd_scncasem';
SHOW INDEX FROM tmd_scncasem;
SHOW TABLES LIKE 'tmd_scncaseh';
SHOW INDEX FROM tmd_scncaseh;

-- 4) confirm user master lookup path for assignee name search/display
SHOW CREATE TABLE tmd_usrm;
SHOW INDEX FROM tmd_usrm;

-- 5) confirm `tmd_scncaseh` run_seq / FK shape if rolled out in the same release
SHOW CREATE TABLE tmd_scncaseh;
```

Pre-check review points:

- Confirm `tcm_filel.file_rel_obj_dstg_no` is compatible with `tmd_scncasem.case_id INT UNSIGNED`.
- Confirm `SCNCASE + case_id` file lookup uses `key = tcm_filel_ix01`, `type = ref`, and no full scan.
- Confirm no active duplicate rows already exist for `SCNCASE` / `SCNCASEH` in `file_rel_obj_spt_cd`.
- Confirm `tmd_scncasem_ix01 (del_yn, crt_dtm)` is the retained list index and `tmd_scncasem_ix02` is not required for current query shape.
- Confirm `tmd_scncaseh_uq01`, `tmd_scncaseh_fk01`, and `tmd_scncaseh_ix01` names do not collide with existing objects.
- Confirm `ASSIGNEE_NAME` JOIN path remains `tmd_scncasem.usr_no = tmd_usrm.usr_no`, display column `tmd_usrm.usr_nm`.
- Confirm `last_run_id VARCHAR(20) NULL` does not require a separate index under current detail/latest-run access patterns.

## Apply

Current draft finalizes `tmd_scncasem`, `tmd_scncaseh`, and common-code registration wording.

```sql
CREATE TABLE tmd_scncasem
(
    case_id          INT UNSIGNED AUTO_INCREMENT COMMENT '시나리오케이스ID'
        PRIMARY KEY,
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    usr_no           INT UNSIGNED                          NOT NULL COMMENT '사용자번호',
    case_titl_nm     VARCHAR(200)                          NOT NULL COMMENT '케이스제목명',
    case_expn        TEXT                                  NULL     COMMENT '케이스설명',
    case_stat_cd     VARCHAR(20) DEFAULT 'PENDING'         NOT NULL COMMENT '케이스상태코드',
    last_run_id      VARCHAR(20)                           NULL     COMMENT '최신실행ID (tmd_scncaseh.run_id 논리참조, 물리 FK 없음)'
)
    COMMENT 'TMD_시나리오케이스마스터';

CREATE INDEX tmd_scncasem_ix01
    ON tmd_scncasem (del_yn, crt_dtm);

CREATE TABLE tmd_scncaseh
(
    run_id          VARCHAR(20)                           NOT NULL COMMENT '실행ID (yyyymmdd_hhmmss_XXXX)'
        PRIMARY KEY,
    run_seq         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '실행순번 (tcm_filel output 파일 연결용 surrogate key)',
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
    CONSTRAINT tmd_scncaseh_uq01
        UNIQUE (run_seq),
    CONSTRAINT tmd_scncaseh_fk01
        FOREIGN KEY (case_id) REFERENCES tmd_scncasem (case_id)
)
    COMMENT 'TMD_시나리오케이스이력';

CREATE INDEX tmd_scncaseh_ix01
    ON tmd_scncaseh (exec_stat_cd, exec_bgn_dtm);

INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNCASE', '시나리오 케이스 입력 파일', '5', '시나리오 케이스 입력 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);

INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNCASEH', '시나리오 케이스 실행 결과 파일', '6', '시나리오 케이스 모델 실행 결과 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);
```

Apply ordering:

1. Create `tmd_scncasem`.
2. Create `tmd_scncasem_ix01`.
3. Create `tmd_scncaseh`.
4. Create `tmd_scncaseh_ix01`.
5. Register `SCNCASE`.
6. Register `SCNCASEH`.

Apply guardrails:

- Execute common-code `INSERT` only after pre-check confirms target duplicates do not exist.
- If the DBA standard prefers idempotent `MERGE`/`INSERT ... SELECT WHERE NOT EXISTS` style, keep the same metadata values and adapt only the insertion pattern.
- `mod_dtm` auto-update remains application-managed (`UPDATE ... SET mod_dtm = NOW()`), not DB-trigger-managed in this draft.
- `run_seq` is retained as the file-linkage surrogate key because `tcm_filel.file_rel_obj_dstg_no` is `INT UNSIGNED` and cannot store `run_id VARCHAR(20)`.

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
  AND file_rel_obj_spt_cd IN ('SCNCASE', 'SCNCASEH')
GROUP BY file_rel_obj_spt_cd;

-- 2) common-code rollback: execute only when reference count is zero
DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNCASE';

DELETE FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNCASEH';

-- 3) child/history rollback first
DROP INDEX tmd_scncaseh_ix01 ON tmd_scncaseh;
DROP TABLE tmd_scncaseh;

-- 4) table/index rollback: execute only when master table removal is explicitly approved
DROP INDEX tmd_scncasem_ix01 ON tmd_scncasem;
DROP TABLE tmd_scncasem;
```

Rollback rules:

- Do not delete `SCNCASE` or `SCNCASEH` common-code rows if any `tcm_filel` rows still reference them.
- Roll back child/history objects (`tmd_scncaseh`) before the master table (`tmd_scncasem`).
- Do not delete S3 objects automatically without explicit storage/DBA approval.
- If any application release already depends on `tmd_scncasem`, prefer forward-fix over table drop.

## Verify / EXPLAIN

Pre-rollout evidence already confirmed:

- `SCNCASE + case_id` file lookup on production DB used `key = tcm_filel_ix01`, `type = ref`, `rows` small, and no full scan under `del_yn = 'N'`.

Add remaining verification SQL here after DB-backed queries are implemented.

Expected future checks:

- Verify duplicate-run guard under concurrent transaction simulation. The conditional `tmd_scncasem` update must report `affected rows = 1` for exactly one of two concurrent requests on the same case.
- Verify `run_id` uniqueness on `tmd_scncaseh`.
- Verify `run_seq` UNIQUE constraint on `tmd_scncaseh`.
- Verify `SCNCASE + case_id` file lookup (upload input files).
- Verify `SCNCASEH + run_seq` file lookup (model execution output files).
- Verify `last_run_id` invariant: for every `tmd_scncasem` row with `last_run_id IS NOT NULL`, the referenced `tmd_scncaseh` row exists and its `exec_stat_cd` matches `case_stat_cd` in the terminal/RUNNING semantics defined by the workflow draft.
- After timeout scheduler SQL is implemented, run `EXPLAIN` against the actual query and confirm intended index usage on `tmd_scncaseh_ix01 (exec_stat_cd, exec_bgn_dtm)`. Until actual SQL exists, expected query-shape examples are not implementation verification.
- Run `EXPLAIN` for canonical list SQL (`WHERE del_yn = 'N' ORDER BY crt_dtm DESC`) and confirm `key = tmd_scncasem_ix01` usage.
- Verify DB-backed update/delete affect only active owned case rows via `case_id + usr_no + del_yn = 'N'` predicate.
- After `ASSIGNEE_NAME` search SQL is implemented, run `EXPLAIN` against the JOIN query resolving `usr_no` to uploader name and confirm index usage on both sides of the JOIN.

## DBA Deployment Notes

- This document must be reviewed with DBA/common-code owner before rollout.
- Phase-1 mock API can proceed without this DDL.
- `scenario_id`는 proposed model에서 제거되었고, ownership scope는 `usr_no` only로 확정했다. rollout 전에는 list index 선택과 JOIN query 실행계획을 DBA review로 확정해야 한다.
