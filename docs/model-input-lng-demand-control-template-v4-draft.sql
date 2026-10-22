-- Document Control
-- status: Draft / DBA review only / NOT EXECUTED
-- canonical for: DRAFT_C_STRUCTURE v4 registration SQL for LNG Demand control + annual table.
-- owner: BE / DBA; date / last synced: 2026-10-22
-- source: DEMAND-normalized.xlsm SUMMARY rows 29-30 and LNG Demand B1:C1/B3:N109;
--         docs/model-input-png-delete-template-v3-draft.sql (v3 clone pattern).
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rule 50;
--             docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md section 3.1B;
--             docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md section 6.5.
-- execution record: docs/work-notes/2026/2026-10-22-lng-demand-control-new-template-registration.md
-- supersedes: none. No existing published template, Case link, Draft or file is updated.
--
-- Scope: clone an ACTIVE DRAFT_C_STRUCTURE v3 catalog (4 files, 70 sheets, 92 tables, 126 scopes)
-- into a new v4 DRAFT, then add LNG Demand Mode as table order 1 and Annual LNG Demand Reference
-- as table order 2. Only the annual table declares lngMode in controls; sourceTableId points to Mode.
-- The first area is the value owner, while FE selects the second table for grid + control.
-- C1 currently contains Dynamic, outside its Excel validation values Dynamaic/Fixed. Preserve that
-- existing value as-is; new SET uses the literal saved option Dynamaic (label Dynamic) or Fixed.
-- No schema alteration, publication or Case rebinding is included. Default end is ROLLBACK.
-- Before DBA use: confirm target DB and current v3 marker, row geometry check, deployed reader/editor,
-- workbook structure, source backup and an isolated rehearsal. Stop on any error or failed guard.

SET NAMES utf8mb4;
SET @lc_actor = NULL; -- fill with a real audit login in the DBA review copy; NULL makes precheck fail.
SET @lc_schema = 'midb', @lc_model = 'DRAFT_C_STRUCTURE', @lc_version = 4;
SET @lc_source_prefix = 'png-delete-20261021; source=tmplt8/DRAFT_C_STRUCTURE/v2; changed=PNG Infrastructure only; NOT_PUBLISHED; digest=';
SET @lc_marker = 'lng-demand-control-20261022; source=v3; added=2; scopes=20; NOT_PUBLISHED';
SET @lc_name = '2026-10-22 LNG Demand control DRAFT';
SET @lc_ok = 0, @lc_new = NULL, @lc_verified = 0;
SET @lc_mode_qry = '{"schemaVersion":1,"columns":[],"headerRows":[],"bodyRanges":[],"identityColumns":[],"fields":[],"capabilities":{"SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED","ADD_ROW":"NOT_ALLOWED","DELETE_ROW":"NOT_ALLOWED","ADD_COLUMN":"NOT_ALLOWED","DELETE_COLUMN":"NOT_ALLOWED"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[],"uploadValidation":{"schemaVersion":1,"headerCells":[{"rowNumber":1,"columnNumber":2,"value":"LNG Demand Mode"}]}}';
SET @lc_annual_qry = '{"schemaVersion":1,"columns":[2,3,5,6,7,8,9,10,11,12,13,14],"headerRows":[3],"bodyRanges":[{"startRow":4,"endRow":109}],"identityColumns":[2,3],"fields":[{"fieldKey":"region","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY"},"setConstraints":{}},{"fieldKey":"country","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY"},"setConstraints":{}},{"fieldKey":"year5","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year6","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year7","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year8","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year9","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year10","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year11","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year12","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year13","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}},{"fieldKey":"year14","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"READY","CLEAR":"READY","SET_HEADER":"READY"},"setConstraints":{}}],"capabilities":{"SET":"READY","CLEAR":"READY","ADD_ROW":"NOT_ALLOWED","DELETE_ROW":"NOT_ALLOWED","ADD_COLUMN":"READY","DELETE_COLUMN":"NOT_ALLOWED"},"inputGuideRules":[],"controls":[{"id":"lngMode","type":"select","label":"LNG Demand Mode","position":"beforeGrid","sourceTableId":"DEMAND/LNG Demand/LNG Demand Mode","binding":{"rowNumber":1,"columnIndex":2},"editable":true,"options":[{"value":"Dynamaic","label":"Dynamic"},{"value":"Fixed","label":"Fixed"}]}],"displayUpdateRules":[],"columnCreation":{"headerValues":{"year":{"dataType":"INTEGER","create":{"required":true,"initial":{"kind":"EMPTY"},"constraints":{}}}},"body":{"dataType":"DECIMAL","create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}}}},"uploadValidation":{"schemaVersion":1,"headerCells":[{"rowNumber":3,"columnNumber":4,"value":"연도"}]}}';
SET @lc_scopes = '[[1,1,1,1,3,3],[2,1,5,108,2,2],[2,2,4,4,3,3],[2,3,6,9,3,3],[2,4,11,14,3,3],[2,5,16,24,3,3],[2,6,26,26,3,3],[2,7,28,34,3,3],[2,8,36,42,3,3],[2,9,44,109,3,3],[2,10,6,9,5,14],[2,11,11,14,5,14],[2,12,16,33,5,14],[2,13,36,42,5,14],[2,14,44,63,5,14],[2,15,65,75,5,14],[2,16,77,88,5,14],[2,17,90,99,5,14],[2,18,101,103,5,14],[2,19,105,108,5,14]]';
SET SESSION group_concat_max_len = 16777216;
SELECT DATABASE() AS selected_schema, VERSION() AS mysql_version,
       @@SESSION.foreign_key_checks AS fk_enabled, @@SESSION.autocommit AS autocommit,
       JSON_VALID(@lc_mode_qry) AS mode_json_valid, JSON_VALID(@lc_annual_qry) AS annual_json_valid,
       JSON_LENGTH(@lc_scopes) AS new_scopes_must_be_20;
SHOW CREATE TABLE tmd_mdl_inpttbld; -- ck01 must permit header rows 0/0 for Mode.

SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, aply_bgn_dtm, tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @lc_model FOR UPDATE;
SET @lc_source = (SELECT tmplt_id FROM tmd_mdl_inpttmpltm
 WHERE BINARY mdl_spt_cd = BINARY @lc_model AND tmplt_ver_no = 3
 AND tmplt_stat_cd = 'ACTIVE' AND aply_bgn_dtm IS NOT NULL AND del_yn = 'N');
SELECT f.inpt_file_id FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @lc_source FOR SHARE;
SET @lc_links = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @lc_source);
SET @lc_drafts = (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id = @lc_source);
SET @lc_ok = IF(BINARY DATABASE() = BINARY @lc_schema AND @@SESSION.foreign_key_checks = 1
 AND CHAR_LENGTH(TRIM(@lc_actor)) BETWEEN 1 AND 50 AND @lc_source IS NOT NULL
 AND JSON_VALID(@lc_mode_qry) = 1 AND JSON_VALID(@lc_annual_qry) = 1
 AND JSON_LENGTH(@lc_scopes) = 20
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @lc_source
      AND BINARY LEFT(tmplt_expn,CHAR_LENGTH(@lc_source_prefix)) = BINARY @lc_source_prefix
      AND CHAR_LENGTH(tmplt_expn) = CHAR_LENGTH(@lc_source_prefix) + 64) = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE BINARY mdl_spt_cd = BINARY @lc_model
      AND tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N') = 1
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inpttmpltm WHERE BINARY mdl_spt_cd = BINARY @lc_model
      AND (tmplt_ver_no = @lc_version OR BINARY tmplt_expn = BINARY @lc_marker))
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id = @lc_source AND del_yn = 'N') = 4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
      WHERE f.tmplt_id = @lc_source AND s.del_yn = 'N') = 70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source AND b.del_yn = 'N') = 92
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
      WHERE f.tmplt_id = @lc_source AND m.del_yn = 'N') = 126
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
      WHERE f.tmplt_id = @lc_source AND BINARY f.mdl_file_cd = BINARY 'DEMAND'
        AND BINARY s.sht_nm = BINARY 'LNG Demand' AND s.sht_stat_cd = 'VISIBLE' AND s.del_yn = 'N') = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source
        AND BINARY f.mdl_file_cd = BINARY 'DEMAND' AND BINARY s.sht_nm = BINARY 'LNG Demand') = 0,
 1, 0);
SELECT @lc_source AS source_tmplt_id, @lc_links AS source_case_links,
       @lc_drafts AS source_drafts, IF(@lc_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;
-- STOP_AND_ROLLBACK: issue ROLLBACK and stop. Every write below is gated by @lc_ok=1.

INSERT INTO tmd_mdl_inpttmpltm (mdl_spt_cd,tmplt_ver_no,tmplt_ver_nm,tmplt_stat_cd,aply_bgn_dtm,
 tmplt_expn,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_model,@lc_version,@lc_name,'DRAFT',NULL,@lc_marker,@lc_actor,CURRENT_TIMESTAMP,
 @lc_actor,CURRENT_TIMESTAMP,'N' FROM DUAL WHERE @lc_ok=1;
SET @lc_new = (SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE @lc_ok=1
 AND BINARY mdl_spt_cd=BINARY @lc_model AND tmplt_ver_no=@lc_version
 AND tmplt_stat_cd='DRAFT' AND BINARY tmplt_expn=BINARY @lc_marker AND del_yn='N');
INSERT INTO tmd_mdl_inptfiled (tmplt_id,mdl_file_cd,file_nm,file_ord,file_type_cd,file_strt_hash_val,
 crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_new,f.mdl_file_cd,f.file_nm,f.file_ord,f.file_type_cd,f.file_strt_hash_val,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptfiled f WHERE @lc_ok=1 AND @lc_new IS NOT NULL AND f.tmplt_id=@lc_source AND f.del_yn='N';
INSERT INTO tmd_mdl_inptshtd (inpt_file_id,sht_nm,sht_ord,sht_stat_cd,
 crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT nf.inpt_file_id,s.sht_nm,s.sht_ord,s.sht_stat_cd,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=s.inpt_file_id AND sf.tmplt_id=@lc_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
WHERE @lc_ok=1 AND s.del_yn='N';
INSERT INTO tmd_mdl_inpttbld (inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,
 tbl_bgn_col_no,tbl_end_col_no,hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,
 col_add_psbl_yn,inpt_tbl_expn,qry_json,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT ns.inpt_sht_id,b.inpt_tbl_nm,b.tbl_ord,b.tbl_bgn_row_no,b.tbl_end_row_no,b.tbl_bgn_col_no,
 b.tbl_end_col_no,b.hdr_bgn_row_no,b.hdr_end_row_no,b.data_bgn_row_no,b.row_add_psbl_yn,
 b.col_add_psbl_yn,b.inpt_tbl_expn,b.qry_json,@lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=b.inpt_sht_id
JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=ss.inpt_file_id AND sf.tmplt_id=@lc_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
WHERE @lc_ok=1 AND b.del_yn='N';
INSERT INTO tmd_mdl_inptmodscopd (inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,
 end_col_no,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT nb.inpt_tbl_id,m.mod_scop_ord,m.bgn_row_no,m.end_row_no,m.bgn_col_no,m.end_col_no,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id=m.inpt_tbl_id
JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=ss.inpt_file_id
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
WHERE @lc_ok=1 AND sf.tmplt_id=@lc_source AND m.del_yn='N';
SET @lc_sheet = (SELECT ns.inpt_sht_id FROM tmd_mdl_inptshtd ns
 JOIN tmd_mdl_inptfiled nf USING(inpt_file_id) WHERE @lc_ok=1 AND nf.tmplt_id=@lc_new
 AND BINARY nf.mdl_file_cd=BINARY 'DEMAND' AND BINARY ns.sht_nm=BINARY 'LNG Demand');
INSERT INTO tmd_mdl_inpttbld (inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,
 tbl_bgn_col_no,tbl_end_col_no,hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,
 col_add_psbl_yn,inpt_tbl_expn,qry_json,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_sheet,'LNG Demand Mode',1,1,1,2,3,0,0,1,'N','N',NULL,
 @lc_mode_qry,@lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM DUAL WHERE @lc_ok=1 AND @lc_sheet IS NOT NULL;
INSERT INTO tmd_mdl_inpttbld (inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,
 tbl_bgn_col_no,tbl_end_col_no,hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,
 col_add_psbl_yn,inpt_tbl_expn,qry_json,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_sheet,'Annual LNG Demand Reference',2,3,109,2,14,3,3,4,'N','Y',NULL,
 @lc_annual_qry,@lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM DUAL WHERE @lc_ok=1 AND @lc_sheet IS NOT NULL;
SET @lc_mode = (SELECT inpt_tbl_id FROM tmd_mdl_inpttbld WHERE inpt_sht_id=@lc_sheet
 AND BINARY inpt_tbl_nm=BINARY 'LNG Demand Mode');
SET @lc_annual = (SELECT inpt_tbl_id FROM tmd_mdl_inpttbld WHERE inpt_sht_id=@lc_sheet
 AND BINARY inpt_tbl_nm=BINARY 'Annual LNG Demand Reference');
INSERT INTO tmd_mdl_inptmodscopd (inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,
 end_col_no,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT IF(x.tbl_ord=1,@lc_mode,@lc_annual),x.scope_ord,x.r1,x.r2,x.c1,x.c2,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM JSON_TABLE(@lc_scopes,'$[*]' COLUMNS(
 tbl_ord INT PATH '$[0]',scope_ord INT PATH '$[1]',r1 INT PATH '$[2]',r2 INT PATH '$[3]',
 c1 INT PATH '$[4]',c2 INT PATH '$[5]')) x
WHERE @lc_ok=1 AND @lc_mode IS NOT NULL AND @lc_annual IS NOT NULL;

-- Verify copied rows and exact LNG payload. Compare all source tables by natural key; only two new
-- rows and twenty scopes may differ. JSON equality ignores key order, but never ignores values.
SET @lc_files_copied = (SELECT COUNT(*) FROM tmd_mdl_inptfiled sf JOIN tmd_mdl_inptfiled nf
 ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 WHERE sf.tmplt_id=@lc_source AND BINARY nf.file_nm=BINARY sf.file_nm AND nf.file_ord=sf.file_ord
 AND nf.file_type_cd=sf.file_type_cd AND nf.file_strt_hash_val <=> sf.file_strt_hash_val);
SET @lc_sheets_copied = (SELECT COUNT(*) FROM tmd_mdl_inptshtd ss JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 WHERE sf.tmplt_id=@lc_source AND ns.sht_ord=ss.sht_ord AND ns.sht_stat_cd=ss.sht_stat_cd);
SET @lc_tables_copied = (SELECT COUNT(*) FROM tmd_mdl_inpttbld sb JOIN tmd_mdl_inptshtd ss USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
 WHERE sf.tmplt_id=@lc_source AND sb.tbl_ord=nb.tbl_ord
 AND sb.tbl_bgn_row_no=nb.tbl_bgn_row_no AND sb.tbl_end_row_no=nb.tbl_end_row_no
 AND sb.tbl_bgn_col_no=nb.tbl_bgn_col_no AND sb.tbl_end_col_no=nb.tbl_end_col_no
 AND sb.hdr_bgn_row_no=nb.hdr_bgn_row_no AND sb.hdr_end_row_no=nb.hdr_end_row_no
 AND sb.data_bgn_row_no=nb.data_bgn_row_no AND sb.row_add_psbl_yn=nb.row_add_psbl_yn
 AND sb.col_add_psbl_yn=nb.col_add_psbl_yn AND sb.inpt_tbl_expn <=> nb.inpt_tbl_expn
 AND CAST(nb.qry_json AS JSON)=CAST(sb.qry_json AS JSON));
SET @lc_scopes_copied = (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd sm
 JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id=sm.inpt_tbl_id
 JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
 JOIN tmd_mdl_inptmodscopd nm ON nm.inpt_tbl_id=nb.inpt_tbl_id AND nm.mod_scop_ord=sm.mod_scop_ord
 WHERE sf.tmplt_id=@lc_source AND nm.bgn_row_no=sm.bgn_row_no AND nm.end_row_no=sm.end_row_no
 AND nm.bgn_col_no=sm.bgn_col_no AND nm.end_col_no=sm.end_col_no);
-- HIERARCHY_DIGEST_A
SET @lc_catalog_digest = (
  SELECT SHA2(GROUP_CONCAT(SHA2(payload, 256) ORDER BY record_kind, record_id SEPARATOR ','), 256)
  FROM (
    SELECT 'F' AS record_kind, f.inpt_file_id AS record_id,
      CAST(JSON_ARRAY('F', f.inpt_file_id, f.tmplt_id, f.mdl_file_cd, f.file_nm, f.file_ord,
        f.file_type_cd, f.file_strt_hash_val, f.del_yn) AS CHAR) AS payload
    FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'S', s.inpt_sht_id,
      CAST(JSON_ARRAY('S', s.inpt_sht_id, s.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd, s.del_yn) AS CHAR)
    FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'T', b.inpt_tbl_id,
      CAST(JSON_ARRAY('T', b.inpt_tbl_id, b.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord,
        b.tbl_bgn_row_no, b.tbl_end_row_no, b.tbl_bgn_col_no, b.tbl_end_col_no,
        b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no, b.row_add_psbl_yn,
        b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json, b.del_yn) AS CHAR)
    FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'M', m.mod_scop_id,
      CAST(JSON_ARRAY('M', m.mod_scop_id, m.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no,
        m.end_row_no, m.bgn_col_no, m.end_col_no, m.del_yn) AS CHAR)
    FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
    WHERE f.tmplt_id = @lc_new
  ) hierarchy
);
UPDATE tmd_mdl_inpttmpltm SET tmplt_expn = CONCAT(@lc_marker, '; digest=', @lc_catalog_digest)
WHERE @lc_ok = 1 AND @lc_new IS NOT NULL AND @lc_catalog_digest IS NOT NULL
AND tmplt_id = @lc_new AND BINARY tmplt_expn = BINARY @lc_marker AND tmplt_stat_cd = 'DRAFT';
SET @lc_verified = IF(@lc_ok=1 AND @lc_new IS NOT NULL AND @lc_mode IS NOT NULL AND @lc_annual IS NOT NULL
 AND @lc_catalog_digest IS NOT NULL
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@lc_new AND tmplt_stat_cd='DRAFT'
      AND BINARY tmplt_expn=BINARY CONCAT(@lc_marker,'; digest=',@lc_catalog_digest))=1
 AND @lc_files_copied=4 AND @lc_sheets_copied=70 AND @lc_tables_copied=92 AND @lc_scopes_copied=126
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_sht_id=@lc_sheet)=2
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@lc_mode AND tbl_ord=1
      AND CAST(qry_json AS JSON)=CAST(@lc_mode_qry AS JSON))=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_tbl_id=@lc_annual AND tbl_ord=2
      AND CAST(qry_json AS JSON)=CAST(@lc_annual_qry AS JSON)
      AND JSON_UNQUOTE(JSON_EXTRACT(qry_json,'$.controls[0].sourceTableId'))='DEMAND/LNG Demand/LNG Demand Mode')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd WHERE inpt_tbl_id IN (@lc_mode,@lc_annual))=20
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@lc_new)=4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
      WHERE f.tmplt_id=@lc_new)=70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_new)=94
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
      WHERE f.tmplt_id=@lc_new)=146
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@lc_source)=@lc_links
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id=@lc_source)=@lc_drafts
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@lc_new)
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id=@lc_new),1,0);
SELECT @lc_new AS new_tmplt_id,@lc_files_copied AS files_must_be_4,
 @lc_sheets_copied AS sheets_must_be_70,@lc_tables_copied AS copied_tables_must_be_92,
 @lc_scopes_copied AS copied_scopes_must_be_126,
 IF(@lc_verified=1,'VERIFIED_UNPUBLISHED','STOP_AND_ROLLBACK') AS decision;
SELECT b.inpt_tbl_nm,b.tbl_ord,b.tbl_bgn_row_no,b.tbl_end_row_no,b.tbl_bgn_col_no,b.tbl_end_col_no,
 b.hdr_bgn_row_no,b.data_bgn_row_no,b.row_add_psbl_yn,b.col_add_psbl_yn,
 JSON_EXTRACT(b.qry_json,'$.controls') AS controls
FROM tmd_mdl_inpttbld b WHERE b.inpt_sht_id=@lc_sheet ORDER BY b.tbl_ord;
ROLLBACK;
-- No COMMIT or publication step in this draft. After review, a separate DBA action must confirm
-- target version, compatible files and full transaction result before any new version is persisted.
