-- Document Control
-- status: Draft / MANUAL REVIEW AND APPLY ONLY / NOT EXECUTED BY CODEX
-- canonical for: Testdb template7 upload-only publication precheck/apply/verify/rollback SQL
-- owner: BE / DBA; date / last synced: 2026-10-20
-- depends on: model-input-upload-validation-testdata-draft.sql (92 full definitions verified and committed)
--             catalog ERD §6.10 / domain template immutability and upload fixed-label rules
-- execution record: work-notes/2026/2026-10-20-model-input-upload-testdb-preparation.md
-- supersedes: none. Do not run the Case-publication SQL for this upload path.
-- open questions: actual publication/commit, runtime configuration and FE smoke result.
-- Prerequisite: reviewed patch PRECHECK_OK/VERIFIED and committed role totals
-- DEMAND40/194, OUTPUT5/19, SUPPLY15/89, TRADE32/157 confirmed with no subsequent edits.
-- Count checks below are drift guards, not a substitute for that full-definition validation.
-- Scope: root7 DRAFT -> ACTIVE and audit/application timestamp only. No Case binding or child/other template writes.
-- Keep upload disabled and all affected writers/workers stopped; dedicated connection, no pending transaction.
-- Existing partial92/99 and incomplete write/finalize policies remain unchanged; upload/create/read smoke only.
-- On ANY error or STOP result: immediately ROLLBACK and stop. Do not override guard variables.
-- Default final ROLLBACK. For actual publication run sections1~4, then execute COMMIT only when published_must_be_1=1;
-- otherwise execute ROLLBACK. Do not execute the default ROLLBACK before the chosen COMMIT.
USE midb;
SET @v_actor='SYSTEM';
SET @v_ok=0,@v_published=0;

-- 1. Lock template/model and its catalog/references (transaction-local isolation).
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id,mdl_spt_cd,tmplt_stat_cd FROM tmd_mdl_inpttmpltm
WHERE tmplt_id=7 OR mdl_spt_cd='DRAFT_C_STRUCTURE' ORDER BY tmplt_id FOR UPDATE;
SELECT inpt_file_id FROM tmd_mdl_inptfiled WHERE tmplt_id=7 FOR UPDATE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id=7 FOR UPDATE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7 FOR UPDATE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=7 FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id=7 FOR UPDATE;

-- 2. Require the exact unused DRAFT, no same-model ACTIVE, and the committed rule coverage.
SET @v_root=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=7
 AND CAST(mdl_spt_cd AS BINARY)=CAST('DRAFT_C_STRUCTURE' AS BINARY) AND tmplt_ver_no=1
 AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N'
 AND CAST(tmplt_expn AS BINARY)=CAST('structure-c-20261019; baselineId=2026-10-07-delivery-261006-v8-v6; partial=92/99; NOT_PUBLISHED' AS BINARY));
SET @v_active=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_stat_cd='ACTIVE' AND del_yn='N');
SET @v_links=(SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=7);
SET @v_drafts=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id=7);
SET @v_files=(SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=7);
SET @v_sheets=(SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7);
SET @v_tables=(SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7);
SET @v_bad=(SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7
 AND (f.del_yn<>'N' OR s.del_yn<>'N' OR b.del_yn<>'N' OR s.sht_stat_cd<>'VISIBLE'
 OR COALESCE(JSON_EXTRACT(b.qry_json,'$.uploadValidation.schemaVersion')=1,0)=0
 OR COALESCE(JSON_TYPE(JSON_EXTRACT(b.qry_json,'$.uploadValidation.headerCells'))='ARRAY',0)=0
 OR COALESCE(JSON_LENGTH(JSON_EXTRACT(b.qry_json,'$.uploadValidation.headerCells')),0)=0));
SET @v_roles=(SELECT COUNT(*) FROM (
 SELECT f.mdl_file_cd,COUNT(*) AS table_count,
 SUM(JSON_LENGTH(JSON_EXTRACT(b.qry_json,'$.uploadValidation.headerCells'))) AS checks
 FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7
 GROUP BY f.mdl_file_cd HAVING
 (mdl_file_cd='DEMAND' AND table_count=40 AND checks=194)
 OR (mdl_file_cd='OUTPUT' AND table_count=5 AND checks=19)
 OR (mdl_file_cd='SUPPLY' AND table_count=15 AND checks=89)
 OR (mdl_file_cd='TRADE' AND table_count=32 AND checks=157)
) expected_roles);
SET @v_period=(SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=7 AND f.mdl_file_cd='SUPPLY'
 AND ((s.sht_nm='LiqUtilAdjustment' AND b.inpt_tbl_nm='Monthly LNG Train Utilization')
 OR (s.sht_nm='LiqProdAdjustment' AND b.inpt_tbl_nm='Monthly LNG Train Production'))
 AND JSON_EXTRACT(b.qry_json,'$.headerRows')=CAST('[7,8,9,10]' AS JSON));
SET @v_ok=IF(CAST(DATABASE() AS BINARY)=CAST('midb' AS BINARY) AND @@SESSION.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@v_actor)) BETWEEN 1 AND 50 AND @v_root=1 AND @v_active=0
 AND @v_links=0 AND @v_drafts=0 AND @v_files=4 AND @v_sheets=70 AND @v_tables=92
 AND @v_bad=0 AND @v_roles=4 AND @v_period=2,1,0);
SELECT @v_root AS draft_must_be_1,@v_active AS active_must_be_0,@v_links AS links_must_be_0,
 @v_drafts AS drafts_must_be_0,@v_roles AS role_matches_must_be_4,@v_period AS period_matches_must_be_2,
 @v_bad AS invalid_rules_must_be_0,@v_ok AS precheck_must_be_1,
 IF(@v_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;

-- 3. The only permanent write: publish root7. Marker describes original registration provenance and is retained.
SET @v_time=CURRENT_TIMESTAMP(6);
UPDATE tmd_mdl_inpttmpltm
SET tmplt_stat_cd='ACTIVE',aply_bgn_dtm=@v_time,mod_usr_logn_id=@v_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @v_ok=1 AND tmplt_id=7 AND CAST(mdl_spt_cd AS BINARY)=CAST('DRAFT_C_STRUCTURE' AS BINARY)
 AND tmplt_ver_no=1 AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N';

-- 4. Check stored target/time/actor, not driver ROW_COUNT. Then choose COMMIT or ROLLBACK below.
SET @v_published=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE @v_ok=1 AND tmplt_id=7
 AND CAST(mdl_spt_cd AS BINARY)=CAST('DRAFT_C_STRUCTURE' AS BINARY) AND tmplt_ver_no=1
 AND tmplt_stat_cd='ACTIVE' AND aply_bgn_dtm=@v_time AND del_yn='N'
 AND CAST(mod_usr_logn_id AS BINARY)=CAST(@v_actor AS BINARY));
SELECT @v_published AS published_must_be_1;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,del_yn
FROM tmd_mdl_inpttmpltm WHERE tmplt_id=7;

-- 5. Manual decision in the SAME connection: published_must_be_1=1 -> COMMIT; otherwise ROLLBACK.
ROLLBACK;
-- After committed publication: SELECT the root again and configure upload.model-code=DRAFT_C_STRUCTURE.
-- Rollback before commit is transactional. After commit keep upload disabled if verification fails;
-- preserve published definitions and investigate. Do not switch back to DRAFT after use or edit ACTIVE JSON.
