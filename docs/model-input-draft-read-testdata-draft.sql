-- Document Control
-- status: Draft / TESTDB USER MANUAL EXECUTION ONLY
-- canonical for: Case103 existing smoke template completion/verify SQL; not production catalog seed
-- owner: BE / user executes; date: 2026-10-16
-- depends on: docs/model-input-draft-read-ddl-draft.sql;
--   docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.5;
--   docs/feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md §1.2/4.3
-- supersedes: this file's earlier ROW_COUNT/LAST_INSERT_ID-based initial registration SQL
-- execution record: docs/work-notes/2026/2026-09-03-model-input-transfer-format.md
-- Codex has NOT connected to the target DB or executed this file there.
-- 현재 table명 tmd_mdl_inpt... 기준. DDL/rename을 다시 실행하는 SQL이 아니다.

-- 사용자 확인 상태: tmplt_id=1/version1/DRAFT/N/MANUAL_TEST_SETUP,
-- 파일 정의0, 전체 Case 연결0, Case103 연결NULL, 관련 Draft0.
-- 기존 시험 양식1을 유지하고 누락8행만 추가한다:
-- 파일역할4 + OUTPUT/BCPrices 시트1 + 표1 + 수정범위1 + Case103 연결1.
-- 기존 시험 양식1의 DRAFT→ACTIVE 발행만 UPDATE하며 새 양식/다른 양식은 만들거나 바꾸지 않는다.
-- 원본 Case/파일 내용·metadata, Draft/행/revision은 INSERT/UPDATE/DELETE하지 않는다.
-- 시험 모델코드 DRAFT_API_SMOKE와 tableId는 testdb 전용이며 전체 catalog 발행이 아니다.
-- application model-code 설정은 필요 없다. Case103에 연결된 시험 양식을 사용한다.
-- 표별 정책은 후속, PriceModel/Regas 추가 추천은 미승인. 목록/seed 설계는 포함하지 않는다.
-- 동작 capabilities는 INCOMPLETE이며 기존 승인 BCPrices SET 필드 metadata만 유지한다.
-- BCPrices B6:E7 헤더/B8:E21 본문은 로컬 baseline/parser/reader로 확인했다.
-- 로컬 파일 확인은 원격 객체의 동일성/접근 확인이 아니다. 첫 표14행으로 대용량 다음 구간은 검증하지 않는다.

-- 실행 순서 (같은 연결, 다른 API/등록 작업과 동시에 실행하지 않는다):
-- 1. 아래 START TRANSACTION부터 마지막 SELECT까지 실행한다. 자동 COMMIT은 없다.
-- 2. PRECHECK_OK와 마지막 READY_TO_COMMIT/역할4/시트1/표1/범위1/연결1을 확인한다.
-- 3. 모두 맞으면 같은 연결에서 주석 없는 COMMIT; 을 별도로 직접 실행한다.
--    파일 끝의 -- COMMIT; 줄은 안내 주석이므로 파일 전체 실행만으로 저장 확정되지 않는다.
--    확정 후 새 DB 연결에서 마지막 VERIFY SELECT만 재실행해 다른 연결에도 보이는지 확인한다.
-- 4. 오류/STOP/NOT_READY이면 남은 문장을 중단하고 같은 연결에서 ROLLBACK; 을 실행한 뒤 결과를 공유한다.
-- 트랜잭션을 열린 채로 두지 않는다. 마지막 SELECT의 READY_TO_COMMIT은 COMMIT 전에는 미확정 값이다.
-- 등록 완료 후에는 이 파일 전체를 다시 실행하지 않는다. 기존 ACTIVE 정의는 수정하지 않는다.
-- 미확정 등록 연결이 남아 있으면 그 연결의 VERIFY/COMMIT 또는 ROLLBACK을 먼저 마친다.
-- 이전 작업 종료 후 새 조회에서 양식1/DRAFT/N/파일0/연결0/관련Draft0을 다시 확인한 경우에만
-- 선행조건을 재검사하는 이 보완 SQL을1회 재실행할 수 있다. 다른 상태면 재등록하지 않는다.
-- ROW_COUNT()/LAST_INSERT_ID() 없이 실제 DB 행으로 부모 ID/정의를 조회한다.
USE midb;
START TRANSACTION;

SET @smoke_template_id = NULL;
SET @smoke_sheet_id = NULL;
SET @smoke_table_id = NULL;
SET @smoke_definition_ready = 0;

-- API와 같은 Case 잠금 순서. 원본과 확인된 시험 양식만 잠근다.
SELECT case_id, exec_type_cd, del_yn
FROM midb.tmd_mdl_scnrcasem WHERE case_id = 103 FOR UPDATE;
SELECT tmplt_id, mdl_spt_cd, tmplt_ver_no, tmplt_stat_cd, del_yn
FROM midb.tmd_mdl_inpttmpltm WHERE tmplt_id = 1 FOR UPDATE;
SELECT file_no
FROM midb.tcm_filel
WHERE file_rel_obj_dstg_no = 103 AND file_rel_obj_spt_cd = 'SCNRCASE'
ORDER BY file_no FOR UPDATE;

-- 캡처로 확인한 빈 시험 양식만 허용한다. 조건 불충족이면 NULL로 후속 DML은0행이다.
SET @smoke_template_id = (
    SELECT t.tmplt_id
    FROM midb.tmd_mdl_inpttmpltm t
    WHERE t.tmplt_id = 1 AND t.mdl_spt_cd = 'DRAFT_API_SMOKE' AND t.tmplt_ver_no = 1
      AND t.tmplt_stat_cd = 'DRAFT' AND t.del_yn = 'N' AND t.crt_usr_logn_id = 'MANUAL_TEST_SETUP'
      AND t.tmplt_ver_nm = 'Case103 Draft API smoke / BCPrices only'
      AND (SELECT COUNT(*) FROM midb.tmd_mdl_inpttmpltm x WHERE x.mdl_spt_cd = 'DRAFT_API_SMOKE') = 1
      AND EXISTS (SELECT 1 FROM midb.tmd_mdl_scnrcasem c
                  WHERE c.case_id = 103 AND c.exec_type_cd = 'MI_BASE' AND c.del_yn = 'N')
      AND NOT EXISTS (SELECT 1 FROM midb.tmd_mdl_inptfiled f WHERE f.tmplt_id = t.tmplt_id)
      AND NOT EXISTS (SELECT 1 FROM midb.tmd_mdl_scnrcasetmpltd b WHERE b.case_id = 103 OR b.tmplt_id = t.tmplt_id)
      AND NOT EXISTS (SELECT 1 FROM midb.tmd_mdl_inptdrftm d WHERE d.org_case_id = 103 OR d.tmplt_id = t.tmplt_id)
      AND (SELECT COUNT(*) FROM midb.tcm_filel f
           WHERE f.file_rel_obj_dstg_no = 103 AND f.file_rel_obj_spt_cd = 'SCNRCASE'
             AND f.del_yn = 'N'
             AND (f.file_vald_end_dt IS NULL OR f.file_vald_end_dt >= DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'), '%Y%m%d'))) = 4
      AND (SELECT COUNT(*) FROM midb.tcm_filel f
           WHERE f.file_rel_obj_dstg_no = 103 AND f.file_rel_obj_spt_cd = 'SCNRCASE' AND f.del_yn = 'N'
             AND (f.file_vald_end_dt IS NULL OR f.file_vald_end_dt >= DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00'), '%Y%m%d'))
             AND f.strg_path_nm IS NOT NULL AND TRIM(f.strg_path_nm) <> '' AND LOWER(f.org_file_nm) LIKE '%.xlsm'
             AND ((f.file_no = 388581 AND UPPER(f.org_file_nm) LIKE '%SUPPLY%')
               OR (f.file_no = 388582 AND UPPER(f.org_file_nm) LIKE '%OUTPUT%')
               OR (f.file_no = 388583 AND UPPER(f.org_file_nm) LIKE '%DEMAND%')
               OR (f.file_no = 388584 AND UPPER(f.org_file_nm) LIKE '%TRADE%'))) = 4
);
SELECT CASE WHEN @smoke_template_id = 1 THEN 'PRECHECK_OK' ELSE 'STOP_AND_ROLLBACK' END AS precheck_result;

INSERT INTO midb.tmd_mdl_inptfiled
    (crt_usr_logn_id, mod_usr_logn_id, tmplt_id, mdl_file_cd, file_nm, file_ord, file_type_cd)
SELECT 'MANUAL_TEST_SETUP', 'MANUAL_TEST_SETUP', @smoke_template_id,
       CASE f.file_no WHEN 388583 THEN 'DEMAND' WHEN 388581 THEN 'SUPPLY'
                      WHEN 388584 THEN 'TRADE' WHEN 388582 THEN 'OUTPUT' END,
       f.org_file_nm,
       CASE f.file_no WHEN 388583 THEN 1 WHEN 388581 THEN 2 WHEN 388584 THEN 3 WHEN 388582 THEN 4 END,
       'XLSM'
FROM midb.tcm_filel f
WHERE @smoke_template_id = 1
  AND f.file_no IN (388581, 388582, 388583, 388584)
  AND f.file_rel_obj_dstg_no = 103 AND f.file_rel_obj_spt_cd = 'SCNRCASE' AND f.del_yn = 'N';

INSERT INTO midb.tmd_mdl_inptshtd
    (crt_usr_logn_id, mod_usr_logn_id, inpt_file_id, sht_nm, sht_ord, sht_stat_cd)
SELECT 'MANUAL_TEST_SETUP', 'MANUAL_TEST_SETUP', f.inpt_file_id, 'BCPrices', 1, 'VISIBLE'
FROM midb.tmd_mdl_inptfiled f
WHERE f.tmplt_id = @smoke_template_id AND f.mdl_file_cd = 'OUTPUT' AND f.del_yn = 'N'
  AND (SELECT COUNT(*) FROM midb.tmd_mdl_inptfiled x WHERE x.tmplt_id = @smoke_template_id) = 4;
SET @smoke_sheet_id = (
    SELECT s.inpt_sht_id FROM midb.tmd_mdl_inptshtd s
    JOIN midb.tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
    WHERE f.tmplt_id = @smoke_template_id AND f.mdl_file_cd = 'OUTPUT' AND f.del_yn = 'N'
      AND s.sht_nm = 'BCPrices' AND s.del_yn = 'N'
);

SET @smoke_query_json = '{
  "schemaVersion":1,
  "columns":[2,3,4,5],
  "headerRows":[6,7],
  "bodyRanges":[{"startRow":8,"endRow":21}],
  "identityColumns":[2],
  "fields":[
    {"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"INCOMPLETE"},"setConstraints":{"minimum":1000,"maximum":9999}},
    {"fieldKey":"hh","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"NOT_ALLOWED"},"setConstraints":{"minimum":0}},
    {"fieldKey":"eastAsiaSpot","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"NOT_ALLOWED"},"setConstraints":{"minimum":0}},
    {"fieldKey":"ttf","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"NOT_ALLOWED"},"setConstraints":{"minimum":0}}
  ],
  "capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},
  "inputGuideRules":[],"controls":[],"displayUpdateRules":[]
}';

INSERT INTO midb.tmd_mdl_inpttbld
    (crt_usr_logn_id, mod_usr_logn_id, inpt_sht_id, inpt_tbl_nm, tbl_ord,
     tbl_bgn_row_no, tbl_end_row_no, tbl_bgn_col_no, tbl_end_col_no,
     hdr_bgn_row_no, hdr_end_row_no, data_bgn_row_no, row_add_psbl_yn, col_add_psbl_yn,
     inpt_tbl_expn, qry_json)
SELECT 'MANUAL_TEST_SETUP', 'MANUAL_TEST_SETUP', @smoke_sheet_id, 'base_case_prices', 1,
       6, 21, 2, 5, 6, 7, 8, 'Y', 'N',
       'testdb first-read fixture / OUTPUT BCPrices / no write API', @smoke_query_json
WHERE @smoke_sheet_id IS NOT NULL;
SET @smoke_table_id = (
    SELECT b.inpt_tbl_id FROM midb.tmd_mdl_inpttbld b
    WHERE b.inpt_sht_id = @smoke_sheet_id AND b.inpt_tbl_nm = 'base_case_prices' AND b.del_yn = 'N'
);

INSERT INTO midb.tmd_mdl_inptmodscopd
    (crt_usr_logn_id, mod_usr_logn_id, inpt_tbl_id, mod_scop_ord,
     bgn_row_no, end_row_no, bgn_col_no, end_col_no)
SELECT 'MANUAL_TEST_SETUP', 'MANUAL_TEST_SETUP', @smoke_table_id, 1, 8, 21, 2, 5
WHERE @smoke_table_id IS NOT NULL;

-- 발행 전에 저장된 전체 건수·좌표·정의를 확인한다.
SET @smoke_definition_ready = (
    SELECT COUNT(*) FROM midb.tmd_mdl_inpttbld b
    JOIN midb.tmd_mdl_inptshtd s ON s.inpt_sht_id = b.inpt_sht_id
    JOIN midb.tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
    JOIN midb.tmd_mdl_inptmodscopd m ON m.inpt_tbl_id = b.inpt_tbl_id
    WHERE f.tmplt_id = @smoke_template_id AND f.mdl_file_cd = 'OUTPUT' AND f.del_yn = 'N'
      AND s.sht_nm = 'BCPrices' AND s.sht_ord = 1 AND s.sht_stat_cd = 'VISIBLE' AND s.del_yn = 'N'
      AND b.inpt_tbl_id = @smoke_table_id AND b.inpt_tbl_nm = 'base_case_prices' AND b.tbl_ord = 1 AND b.del_yn = 'N'
      AND b.tbl_bgn_row_no = 6 AND b.tbl_end_row_no = 21 AND b.tbl_bgn_col_no = 2 AND b.tbl_end_col_no = 5
      AND b.hdr_bgn_row_no = 6 AND b.hdr_end_row_no = 7 AND b.data_bgn_row_no = 8
      AND b.row_add_psbl_yn = 'Y' AND b.col_add_psbl_yn = 'N' AND BINARY b.qry_json = BINARY @smoke_query_json
      AND m.mod_scop_ord = 1 AND m.bgn_row_no = 8 AND m.end_row_no = 21
      AND m.bgn_col_no = 2 AND m.end_col_no = 5 AND m.del_yn = 'N'
      AND (SELECT COUNT(*) FROM midb.tmd_mdl_inptfiled x WHERE x.tmplt_id = @smoke_template_id) = 4
      AND (SELECT COUNT(*) FROM midb.tmd_mdl_inptshtd x JOIN midb.tmd_mdl_inptfiled y ON y.inpt_file_id = x.inpt_file_id
           WHERE y.tmplt_id = @smoke_template_id) = 1
      AND (SELECT COUNT(*) FROM midb.tmd_mdl_inpttbld x JOIN midb.tmd_mdl_inptshtd y ON y.inpt_sht_id = x.inpt_sht_id
           JOIN midb.tmd_mdl_inptfiled z ON z.inpt_file_id = y.inpt_file_id WHERE z.tmplt_id = @smoke_template_id) = 1
      AND (SELECT COUNT(*) FROM midb.tmd_mdl_inptmodscopd x WHERE x.inpt_tbl_id = @smoke_table_id) = 1
);

UPDATE midb.tmd_mdl_inpttmpltm
SET tmplt_stat_cd = 'ACTIVE', aply_bgn_dtm = CURRENT_TIMESTAMP(6), mod_usr_logn_id = 'MANUAL_TEST_SETUP'
WHERE tmplt_id = @smoke_template_id AND mdl_spt_cd = 'DRAFT_API_SMOKE' AND tmplt_ver_no = 1
  AND tmplt_stat_cd = 'DRAFT' AND del_yn = 'N' AND crt_usr_logn_id = 'MANUAL_TEST_SETUP'
  AND @smoke_definition_ready = 1;

-- UPDATE 처리 행 수 대신 저장된 시험 ACTIVE 상태를 확인해 연결한다.
INSERT INTO midb.tmd_mdl_scnrcasetmpltd
    (case_id, crt_usr_logn_id, mod_usr_logn_id, tmplt_id)
SELECT 103, 'MANUAL_TEST_SETUP', 'MANUAL_TEST_SETUP', t.tmplt_id
FROM midb.tmd_mdl_inpttmpltm t
WHERE t.tmplt_id = @smoke_template_id AND t.mdl_spt_cd = 'DRAFT_API_SMOKE' AND t.tmplt_ver_no = 1
  AND t.tmplt_stat_cd = 'ACTIVE' AND t.del_yn = 'N' AND @smoke_definition_ready = 1
  AND NOT EXISTS (SELECT 1 FROM midb.tmd_mdl_scnrcasetmpltd b WHERE b.case_id = 103);

-- VERIFY: 세션 변수 없음. 재접속/COMMIT 후에도 이 SELECT만으로 실제 데이터를 확인한다.
SELECT CASE WHEN q.tmplt_stat_cd = 'ACTIVE' AND q.del_yn = 'N'
                  AND q.mdl_spt_cd = 'DRAFT_API_SMOKE' AND q.tmplt_ver_no = 1
                  AND q.crt_usr_logn_id = 'MANUAL_TEST_SETUP'
                  AND q.registered_file_roles = 4 AND q.registered_sheets = 1
                  AND q.registered_tables = 1 AND q.registered_scopes = 1
                  AND q.registered_case_binding = 1 AND q.case103_template_id = 1 AND q.binding_del_yn = 'N'
            THEN 'READY_TO_COMMIT' ELSE 'NOT_READY' END AS bootstrap_result,
       103 AS case_id, 'DRAFT_API_SMOKE' AS template_model_code,
       q.tmplt_id, q.tmplt_stat_cd, q.registered_file_roles, q.registered_sheets,
       q.registered_tables, q.registered_scopes, q.registered_case_binding,
       'OUTPUT/BCPrices/base_case_prices' AS first_table_id
FROM (
    SELECT t.tmplt_id, t.mdl_spt_cd, t.tmplt_ver_no, t.tmplt_stat_cd, t.del_yn, t.crt_usr_logn_id,
           c.tmplt_id AS case103_template_id, c.del_yn AS binding_del_yn,
           (SELECT COUNT(*) FROM midb.tmd_mdl_inptfiled f WHERE f.tmplt_id = 1) AS registered_file_roles,
           (SELECT COUNT(*) FROM midb.tmd_mdl_inptshtd s
            JOIN midb.tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
            WHERE f.tmplt_id = 1) AS registered_sheets,
           (SELECT COUNT(*) FROM midb.tmd_mdl_inpttbld b
            JOIN midb.tmd_mdl_inptshtd s ON s.inpt_sht_id = b.inpt_sht_id
            JOIN midb.tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
            WHERE f.tmplt_id = 1) AS registered_tables,
           (SELECT COUNT(*) FROM midb.tmd_mdl_inptmodscopd m
            JOIN midb.tmd_mdl_inpttbld b ON b.inpt_tbl_id = m.inpt_tbl_id
            JOIN midb.tmd_mdl_inptshtd s ON s.inpt_sht_id = b.inpt_sht_id
            JOIN midb.tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
            WHERE f.tmplt_id = 1) AS registered_scopes,
           (SELECT COUNT(*) FROM midb.tmd_mdl_scnrcasetmpltd x WHERE x.tmplt_id = 1) AS registered_case_binding
    FROM (SELECT 1 AS target_template_id) expected
    LEFT JOIN midb.tmd_mdl_inpttmpltm t ON t.tmplt_id = expected.target_template_id
    LEFT JOIN midb.tmd_mdl_scnrcasetmpltd c ON c.case_id = 103
) q;

-- READY_TO_COMMIT이면 같은 연결에서 다음 문장 하나를 직접 실행한다:
-- COMMIT;
-- 오류/STOP_AND_ROLLBACK/NOT_READY이면 같은 연결에서 다음 문장 하나를 직접 실행한다:
-- ROLLBACK;
-- COMMIT 후 새 DB 연결에서 위 VERIFY SELECT만 다시 실행하고 결과를 전달한다.
-- 재조회 READY_TO_COMMIT은 데이터 준비 상태 표시이며 재COMMIT 요청이 아니다.
-- 실제 저장소 읽기·API 생성/재개/조회 성공은 이후 API 시험으로 확인한다.
