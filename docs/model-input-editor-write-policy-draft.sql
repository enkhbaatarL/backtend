-- Document Control
-- status: Draft / MANUAL REVIEW AND APPLY ONLY / NOT EXECUTED BY CODEX
-- canonical for: Parameters existing-input write-policy preparation/verify SQL (no publication)
-- owner: BE / DBA; date / last synced: 2026-10-20
-- source: model-input-catalog-structure-data-draft.sql @c_q077
-- depends on: design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rules3/6/21
--             design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.9.5
--             feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md §3~6
-- execution record: work-notes/2026/2026-10-19-model-input-excel-editor.md
-- supersedes: none. Do not rerun or overwrite the original structure-registration SQL.
-- Scope: one unreferenced DRAFT table; D2:D3/F2:F3 SET/CLEAR and two scopes only.
-- Existing keys/geometry/identity/uploadValidation stay unchanged. No new field or row/column command.
-- This is NOT an ACTIVE transition, Case binding, schema migration, or whole-editor READY claim.
-- Dedicated MySQL8.0.16+ connection; InnoDB/CHECK/FK enabled; stop on any error or STOP.
-- Stop affected catalog writers during manual application. Never use --force.
-- If the definition is ACTIVE/DEPRECATED or referenced, prepare a new DRAFT version separately.
-- Never switch a published template back to DRAFT to pass these guards.
-- Default final ROLLBACK. Review VERIFIED first; only an authorized operator may choose COMMIT.

-- 1. Operator-supplied exact target and reviewed current qry_json digest; never infer MAX/version7.
SET @p_schema=NULL, @p_template=NULL, @p_model=NULL, @p_version=NULL, @p_actor=NULL;
SET @p_expected_qry_sha256=NULL;
SET @p_table=NULL, @p_ok=0, @p_verified=0, @p_before=NULL;
SELECT DATABASE() AS target_schema, VERSION() AS mysql_version, @@SESSION.foreign_key_checks AS fk_enabled;
-- Inspect the chosen target first and fill @p_expected_qry_sha256 from its reviewed complete definition.
SELECT t.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,b.inpt_tbl_id,
       b.qry_json,SHA2(b.qry_json,256) AS current_qry_sha256
FROM tmd_mdl_inpttmpltm t JOIN tmd_mdl_inptfiled f USING(tmplt_id)
JOIN tmd_mdl_inptshtd s USING(inpt_file_id) JOIN tmd_mdl_inpttbld b USING(inpt_sht_id)
WHERE t.tmplt_id=@p_template AND f.mdl_file_cd='TRADE' AND s.sht_nm='DemandAdjLogicInput'
  AND b.inpt_tbl_nm='Demand Adjustment Parameters';

-- Shared payload: focused Java checks consume these exact JSON literals, not a parallel READY fixture.
SET @p_baseline='{"schemaVersion":1,"columns":[3,4,5,6],"headerRows":[1],"bodyRanges":[{"startRow":2,"endRow":3}],"identityColumns":[3,4,5,6],"fields":[{"fieldKey":"demandAdjustment","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';
SET @p_definition='{"schemaVersion":1,"columns":[3,4,5,6],"headerRows":[1],"bodyRanges":[{"startRow":2,"endRow":3}],"identityColumns":[3,4,5,6],"fields":[{"fieldKey":"demandAdjustment","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"READY"},"setConstraints":{}},{"fieldKey":"columnE","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"READY"},"setConstraints":{}}],"capabilities":{"SET":"READY","CLEAR":"READY","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';
SET @p_scopes='[{"startRow":2,"endRow":3,"startColumn":4,"endColumn":4},{"startRow":2,"endRow":3,"startColumn":6,"endColumn":6}]';

-- 2. Lock the selected hierarchy and all existing references in this transaction.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@p_template FOR UPDATE;
SELECT inpt_file_id FROM tmd_mdl_inptfiled WHERE tmplt_id=@p_template FOR UPDATE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id=@p_template FOR UPDATE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_template FOR UPDATE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id=@p_template FOR UPDATE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@p_template FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id=@p_template FOR UPDATE;
SELECT tmplt_id FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@p_template FOR UPDATE;

-- Scalar subquery deliberately fails on duplicate matching tables.
SET @p_table=(SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id=@p_template AND BINARY f.mdl_file_cd=BINARY 'TRADE' AND f.del_yn='N'
 AND BINARY s.sht_nm=BINARY 'DemandAdjLogicInput' AND s.sht_stat_cd='VISIBLE' AND s.del_yn='N'
 AND BINARY b.inpt_tbl_nm=BINARY 'Demand Adjustment Parameters' AND b.del_yn='N'
 AND b.tbl_bgn_row_no=1 AND b.tbl_end_row_no=3 AND b.tbl_bgn_col_no=3 AND b.tbl_end_col_no=6
 AND b.hdr_bgn_row_no=1 AND b.hdr_end_row_no=1 AND b.data_bgn_row_no=2
 AND b.row_add_psbl_yn='Y' AND b.col_add_psbl_yn='N');
SET @p_before=(SELECT qry_json FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@p_table);
SET @p_ok=IF(BINARY DATABASE()=BINARY @p_schema AND @@SESSION.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@p_actor)) BETWEEN 1 AND 50
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@p_template
      AND BINARY mdl_spt_cd=BINARY @p_model AND tmplt_ver_no=@p_version
      AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N')=1
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@p_template)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id=@p_template)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@p_template)
 AND @p_table IS NOT NULL AND SHA2(@p_before,256)=@p_expected_qry_sha256
 AND BINARY CAST(JSON_REMOVE(@p_before,'$.uploadValidation') AS CHAR CHARACTER SET utf8mb4)
     =BINARY CAST(CAST(@p_baseline AS JSON) AS CHAR CHARACTER SET utf8mb4)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@p_table),1,0);
SELECT @p_table AS exact_table_id,IF(@p_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;

-- 3. Merge the reviewed definition without removing unrelated root metadata (including uploadValidation).
-- Only two top-level properties differ from baseline: fields and capabilities.
UPDATE tmd_mdl_inpttbld SET qry_json=JSON_MERGE_PATCH(qry_json,CAST(@p_definition AS JSON)),
 mod_usr_logn_id=@p_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @p_ok=1 AND inpt_tbl_id=@p_table AND SHA2(qry_json,256)=@p_expected_qry_sha256;
INSERT INTO tmd_mdl_inptmodscopd
 (inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,end_col_no,
  crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @p_table,x.ord,x.first_row,x.last_row,x.first_col,x.last_col,
 @p_actor,CURRENT_TIMESTAMP,@p_actor,CURRENT_TIMESTAMP,'N'
FROM JSON_TABLE(@p_scopes,'$[*]' COLUMNS(ord FOR ORDINALITY,
 first_row INT PATH '$.startRow',last_row INT PATH '$.endRow',
 first_col INT PATH '$.startColumn',last_col INT PATH '$.endColumn')) x
WHERE @p_ok=1;

-- 4. Exact final payload and two non-overlapping scopes. All other tables are outside both writes.
SET @p_verified=IF(@p_ok=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@p_table
      AND BINARY CAST(CAST(qry_json AS JSON) AS CHAR CHARACTER SET utf8mb4)
          =BINARY CAST(JSON_MERGE_PATCH(@p_before,CAST(@p_definition AS JSON)) AS CHAR CHARACTER SET utf8mb4))=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@p_table)=2
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@p_table AND del_yn='N'
      AND bgn_row_no=2 AND end_row_no=3 AND bgn_col_no=end_col_no
      AND ((mod_scop_ord=1 AND bgn_col_no=4) OR (mod_scop_ord=2 AND bgn_col_no=6)))=2,1,0);
SELECT IF(@p_verified=1,'VERIFIED_UNPUBLISHED','STOP_AND_ROLLBACK') AS decision;
SELECT inpt_tbl_id,qry_json FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@p_table;
SELECT mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,end_col_no
FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@p_table ORDER BY mod_scop_ord;
ROLLBACK;
-- Choose COMMIT instead of the final ROLLBACK only after authorized manual review and VERIFIED_UNPUBLISHED.
-- After a committed change this is intentionally not rerunnable: old digest/scope0 guards fail.
-- No automatic post-COMMIT rollback/deletion; preserve evidence and review references before a separate reversal.

-- BEGIN BANDS — standalone block; select ONLY this block for Bands manual preparation.
-- Parameters above is independent and must not be rerun as a prerequisite. Do not batch both blocks.
-- I3:I7 remains display-only; CREATE and row/column commands remain incomplete.
-- Document Control
-- status: Draft / MANUAL REVIEW AND APPLY ONLY / NOT EXECUTED BY CODEX
-- canonical for: Bands existing-input write-policy preparation/verify SQL (no publication)
-- owner: BE / DBA; date / last synced: 2026-10-20
-- source: model-input-catalog-structure-data-draft.sql @c_q078
-- depends on: design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rules3/6/21
--             design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.9.5
--             feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md §3~6
-- execution record: work-notes/2026/2026-10-19-model-input-excel-editor.md
-- supersedes: none. Do not rerun or overwrite the original structure-registration SQL.
-- Scope: one unreferenced DRAFT table; H3:H7/J3:J7 SET/CLEAR and two scopes only.
-- Existing keys/geometry/identity/uploadValidation stay unchanged. No new field or row/column command.
-- This is NOT an ACTIVE transition, Case binding, schema migration, or whole-editor READY claim.
-- Dedicated MySQL8.0.16+ connection; InnoDB/CHECK/FK enabled; stop on any error or STOP.
-- Stop affected catalog writers during manual application. Never use --force.
-- If the definition is ACTIVE/DEPRECATED or referenced, prepare a new DRAFT version separately.
-- Never switch a published template back to DRAFT to pass these guards.
-- Default final ROLLBACK. Review VERIFIED first; only an authorized operator may choose COMMIT.

-- 1. Operator-supplied exact target and reviewed current qry_json digest; never infer MAX/version7.
SET @b_schema=NULL, @b_template=NULL, @b_model=NULL, @b_version=NULL, @b_actor=NULL;
SET @b_expected_qry_sha256=NULL;
SET @b_table=NULL, @b_ok=0, @b_verified=0, @b_before=NULL;
SELECT DATABASE() AS target_schema, VERSION() AS mysql_version, @@SESSION.foreign_key_checks AS fk_enabled;
-- Inspect the chosen target first and fill @b_expected_qry_sha256 from its reviewed complete definition.
SELECT t.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,b.inpt_tbl_id,
       b.qry_json,SHA2(b.qry_json,256) AS current_qry_sha256
FROM tmd_mdl_inpttmpltm t JOIN tmd_mdl_inptfiled f USING(tmplt_id)
JOIN tmd_mdl_inptshtd s USING(inpt_file_id) JOIN tmd_mdl_inpttbld b USING(inpt_sht_id)
WHERE t.tmplt_id=@b_template AND f.mdl_file_cd='TRADE' AND s.sht_nm='DemandAdjLogicInput'
  AND b.inpt_tbl_nm='Demand Adjustment Bands';

-- Shared payload: focused Java checks consume these exact JSON literals, not a parallel READY fixture.
SET @b_baseline='{"schemaVersion":1,"columns":[8,9,10],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":7}],"identityColumns":[8,9,10],"fields":[{"fieldKey":"lowerBound","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"deviations","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';
SET @b_definition='{"schemaVersion":1,"columns":[8,9,10],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":7}],"identityColumns":[8,9,10],"fields":[{"fieldKey":"lowerBound","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"READY"},"setConstraints":{}},{"fieldKey":"deviations","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"READY","CLEAR":"READY"},"setConstraints":{}}],"capabilities":{"SET":"READY","CLEAR":"READY","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';
SET @b_scopes='[{"startRow":3,"endRow":7,"startColumn":8,"endColumn":8},{"startRow":3,"endRow":7,"startColumn":10,"endColumn":10}]';

-- 2. Lock the selected hierarchy and all existing references in this transaction.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@b_template FOR UPDATE;
SELECT inpt_file_id FROM tmd_mdl_inptfiled WHERE tmplt_id=@b_template FOR UPDATE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id=@b_template FOR UPDATE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@b_template FOR UPDATE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id=@b_template FOR UPDATE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@b_template FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id=@b_template FOR UPDATE;
SELECT tmplt_id FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@b_template FOR UPDATE;

-- Scalar subquery deliberately fails on duplicate matching tables.
SET @b_table=(SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id=@b_template AND BINARY f.mdl_file_cd=BINARY 'TRADE' AND f.del_yn='N'
 AND BINARY s.sht_nm=BINARY 'DemandAdjLogicInput' AND s.sht_stat_cd='VISIBLE' AND s.del_yn='N'
 AND BINARY b.inpt_tbl_nm=BINARY 'Demand Adjustment Bands' AND b.del_yn='N'
 AND b.tbl_bgn_row_no=1 AND b.tbl_end_row_no=7 AND b.tbl_bgn_col_no=8 AND b.tbl_end_col_no=10
 AND b.hdr_bgn_row_no=1 AND b.hdr_end_row_no=2 AND b.data_bgn_row_no=3
 AND b.row_add_psbl_yn='Y' AND b.col_add_psbl_yn='N');
SET @b_before=(SELECT qry_json FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@b_table);
SET @b_ok=IF(BINARY DATABASE()=BINARY @b_schema AND @@SESSION.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@b_actor)) BETWEEN 1 AND 50
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@b_template
      AND BINARY mdl_spt_cd=BINARY @b_model AND tmplt_ver_no=@b_version
      AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N')=1
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@b_template)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id=@b_template)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@b_template)
 AND @b_table IS NOT NULL AND SHA2(@b_before,256)=@b_expected_qry_sha256
 AND BINARY CAST(JSON_REMOVE(@b_before,'$.uploadValidation') AS CHAR CHARACTER SET utf8mb4)
     =BINARY CAST(CAST(@b_baseline AS JSON) AS CHAR CHARACTER SET utf8mb4)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@b_table),1,0);
SELECT @b_table AS exact_table_id,IF(@b_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;

-- 3. Merge the reviewed definition without removing unrelated root metadata (including uploadValidation).
-- Only two top-level properties differ from baseline: fields and capabilities.
UPDATE tmd_mdl_inpttbld SET qry_json=JSON_MERGE_PATCH(qry_json,CAST(@b_definition AS JSON)),
 mod_usr_logn_id=@b_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @b_ok=1 AND inpt_tbl_id=@b_table AND SHA2(qry_json,256)=@b_expected_qry_sha256;
INSERT INTO tmd_mdl_inptmodscopd
 (inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,end_col_no,
  crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @b_table,x.ord,x.first_row,x.last_row,x.first_col,x.last_col,
 @b_actor,CURRENT_TIMESTAMP,@b_actor,CURRENT_TIMESTAMP,'N'
FROM JSON_TABLE(@b_scopes,'$[*]' COLUMNS(ord FOR ORDINALITY,
 first_row INT PATH '$.startRow',last_row INT PATH '$.endRow',
 first_col INT PATH '$.startColumn',last_col INT PATH '$.endColumn')) x
WHERE @b_ok=1;

-- 4. Exact final payload and two non-overlapping scopes. All other tables are outside both writes.
SET @b_verified=IF(@b_ok=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@b_table
      AND BINARY CAST(CAST(qry_json AS JSON) AS CHAR CHARACTER SET utf8mb4)
          =BINARY CAST(JSON_MERGE_PATCH(@b_before,CAST(@b_definition AS JSON)) AS CHAR CHARACTER SET utf8mb4))=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@b_table)=2
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@b_table AND del_yn='N'
      AND bgn_row_no=3 AND end_row_no=7 AND bgn_col_no=end_col_no
      AND ((mod_scop_ord=1 AND bgn_col_no=8) OR (mod_scop_ord=2 AND bgn_col_no=10)))=2,1,0);
SELECT IF(@b_verified=1,'VERIFIED_UNPUBLISHED','STOP_AND_ROLLBACK') AS decision;
SELECT inpt_tbl_id,qry_json FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@b_table;
SELECT mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,end_col_no
FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id=@b_table ORDER BY mod_scop_ord;
ROLLBACK;
-- Choose COMMIT instead of the final ROLLBACK only after authorized manual review and VERIFIED_UNPUBLISHED.
-- After a committed change this is intentionally not rerunnable: old digest/scope0 guards fail.
-- No automatic post-COMMIT rollback/deletion; preserve evidence and review references before a separate reversal.
-- END BANDS
