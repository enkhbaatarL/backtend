-- Document Control
-- status: Draft / TESTDB MANUAL RESET+REBIND ONLY / NOT EXECUTED ON ANY SERVER DB
--         (rehearsed only on a local disposable MySQL 8.0.40 container built from the repo DDL drafts)
-- canonical for: Case108 draft-data reset + binding to the published editor template version apply/verify SQL
-- owner: BE / DBA; date / last synced: 2026-10-20
-- source pattern: model-input-case-103-107-draft-reset-draft.sql (reviewed APPROVE); only the Case set,
--                 counts and the target-template pin differ.
-- depends on: model-input-editor-write-template-version-draft.sql PART A + PART B COMMITTED first
--             (single ACTIVE of DRAFT_C_STRUCTURE = editor version, template7 DEPRECATED).
--             model-input-draft-read-/-change-request-/-finalization-/-incremental-upload-/-web-runner-/
--             -draft-column-v2-ddl-draft.sql (draft tables and keys).
-- execution record: docs/work-notes/2026/2026-10-20-model-input-case-108-editor-template-rebind.md
-- User decision 2026-10-20: rebind Case108 (currently template7) to the new editor template; delete its drafts first.
-- DELETE target: ALL drafts with org_case_id=108 (all owners/statuses/deleted rows) and their Draft file links/
-- revisions/row identities/positions/requests/finalization preparation metadata. Upload drafts (org_case_id NULL,
-- even if they finalized into Case108), other Cases, Case/tcm_filel rows, storage objects and both catalogs stay.
-- Why every status: FINALIZED/DISCARDED drafts pinned to template7 would fail SOURCE_CHANGED after the rebind.
-- No DDL on persistent objects, FOREIGN_KEY_CHECKS stays ON. Newest revisions are deleted first.
-- Before executing, stop all affected API/file/catalog writers and draft consumers, finish their work, and retain
-- a restorable backup of the Case108 drafts and every child row. No pending transaction on this connection.
-- QUEUED/RUNNING requests, unfinished file cleanup, upload references or runner references -> STOP.
-- Do not clear runner pointers/mark work failed by SQL to bypass a guard; finish existing recovery first.
-- ANY error/STOP: ROLLBACK immediately, stop. Never --force or override guard variables.
-- Rehearsal: execute whole file (default ROLLBACK). Apply: execute sections1~4,
-- then choose COMMIT INSTEAD OF section5 ROLLBACK only after READY_TO_COMMIT and seven zeros.

-- 1. Read-only inventory and session-local target containers.
USE midb;
SET @rb_actor='SYSTEM';
SET @rb_model='DRAFT_C_STRUCTURE';
SET @rb_source=7;
SET @rb_editor_marker='editor-write-20261020; source=tmplt7/DRAFT_C_STRUCTURE/v1; patched=52/92; scopes=126; NOT_PUBLISHED';
SET @rb_ok=0,@rb_verified=0,@rb_template=NULL,@rb_editor=0,@rb_source_deprecated=0,@rb_old_binding=0,@rb_ready_tables=0;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,del_yn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model ORDER BY tmplt_ver_no DESC;
SELECT c.case_id,c.exec_type_cd,c.del_yn,b.tmplt_id,b.del_yn AS binding_del_yn
FROM tmd_mdl_scnrcasem c LEFT JOIN tmd_mdl_scnrcasetmpltd b ON b.case_id=c.case_id
WHERE c.case_id IN (108) ORDER BY c.case_id;
SELECT drft_id,org_case_id,cust_cpny_no,usr_no,tmplt_id,drft_stat_cd,rvsn_no,actv_prcs_id,del_yn
FROM tmd_mdl_inptdrftm WHERE org_case_id IN (108) ORDER BY org_case_id,drft_id;
-- Save a recoverable backup of the target Drafts and all their child rows before actual COMMIT.
-- After COMMIT SQL cannot recover erased history. Export data privately; don't print request/storage JSON.
DROP TEMPORARY TABLE IF EXISTS tmp_case_reset_drafts;
CREATE TEMPORARY TABLE tmp_case_reset_drafts (
 drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
DROP TEMPORARY TABLE IF EXISTS tmp_case_reset_requests;
CREATE TEMPORARY TABLE tmp_case_reset_requests (
 prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

-- 2. Lock / precheck. All following changes are in this one transaction.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT case_id FROM tmd_mdl_scnrcasem WHERE case_id IN (108) ORDER BY case_id FOR UPDATE;
SELECT case_id,tmplt_id,del_yn FROM tmd_mdl_scnrcasetmpltd WHERE case_id IN (108) ORDER BY case_id FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE org_case_id IN (108) ORDER BY drft_id FOR UPDATE;
SELECT p.prcs_id FROM tmd_mdl_inptdrftprcsl p JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
WHERE d.org_case_id IN (108) ORDER BY p.prcs_id FOR UPDATE;
SELECT file_no FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no IN (108)
ORDER BY file_no FOR UPDATE;
SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model ORDER BY tmplt_id FOR UPDATE;
-- Multiple ACTIVE rows deliberately cause a scalar-subquery error, rather than picking one arbitrarily.
SET @rb_template=(SELECT tmplt_id FROM tmd_mdl_inpttmpltm
 WHERE mdl_spt_cd=@rb_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N' AND aply_bgn_dtm IS NOT NULL);
-- The single ACTIVE must be the published editor version (PART B done) and Case108 must still point to template7.
SET @rb_editor=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@rb_template
 AND BINARY tmplt_expn=BINARY @rb_editor_marker AND tmplt_ver_no>1);
SET @rb_source_deprecated=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@rb_source
 AND BINARY mdl_spt_cd=BINARY @rb_model AND tmplt_stat_cd='DEPRECATED' AND del_yn='N');
SET @rb_old_binding=(SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE case_id=108 AND tmplt_id=@rb_source AND del_yn='N');
SET @rb_ready_tables=(SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@rb_template
 AND JSON_UNQUOTE(JSON_EXTRACT(b.qry_json,'$.capabilities.SET'))='READY');
INSERT INTO tmp_case_reset_drafts SELECT drft_id FROM tmd_mdl_inptdrftm WHERE org_case_id IN (108);
INSERT INTO tmp_case_reset_requests SELECT prcs_id FROM tmd_mdl_inptdrftprcsl
WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts);
-- Optional newer web-runner table must not still reference a target request. Never erase lease/termination evidence.
SET @reset_runner_refs=0;
SET @reset_runner_sql=IF(EXISTS(SELECT 1 FROM information_schema.tables
 WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptexecm'),
 'SELECT COUNT(*) INTO @reset_runner_refs FROM tmd_mdl_inptexecm WHERE prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests) FOR UPDATE',
 'SET @reset_runner_refs=0');
PREPARE case_reset_runner_stmt FROM @reset_runner_sql;
EXECUTE case_reset_runner_stmt;
DEALLOCATE PREPARE case_reset_runner_stmt;
SELECT f.prcs_id,f.try_tokn,f.mdl_file_cd FROM tmd_mdl_inptfnsvfiled f
WHERE f.prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests) FOR UPDATE;
SELECT upld_file_id FROM tmd_mdl_inptupldfiled
WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts)
 OR prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests) FOR UPDATE;
SELECT cust_cpny_no,usr_no FROM tmd_mdl_inptupldactvm
WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts)
 OR prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests) FOR UPDATE;
SET @rb_today=DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+09:00'),'%Y%m%d');
SET @rb_drafts=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm
 WHERE org_case_id IN (108) AND drft_stat_cd='DRAFT' AND del_yn='N');
SET @rb_busy=(SELECT COUNT(*) FROM tmd_mdl_inptdrftprcsl p JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
 WHERE d.org_case_id IN (108) AND p.prcs_stat_cd IN ('QUEUED','RUNNING'));
-- Same role selection as ModelInputDraftService.source(): filename contains exactly one of the four roles.
SET @rb_file_cases=(SELECT COUNT(*) FROM (
 SELECT file_rel_obj_dstg_no FROM tcm_filel
 WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no IN (108) AND del_yn='N'
 AND (file_vald_end_dt IS NULL OR file_vald_end_dt>=@rb_today)
 GROUP BY file_rel_obj_dstg_no HAVING COUNT(*)=4
 AND SUM(LOCATE('DEMAND',UPPER(org_file_nm))>0)=1
 AND SUM(LOCATE('SUPPLY',UPPER(org_file_nm))>0)=1
 AND SUM(LOCATE('TRADE',UPPER(org_file_nm))>0)=1
 AND SUM(LOCATE('OUTPUT',UPPER(org_file_nm))>0)=1
 AND SUM(((LOCATE('DEMAND',UPPER(org_file_nm))>0)+(LOCATE('SUPPLY',UPPER(org_file_nm))>0)
         +(LOCATE('TRADE',UPPER(org_file_nm))>0)+(LOCATE('OUTPUT',UPPER(org_file_nm))>0))=1)=4
 AND SUM(strg_path_nm IS NOT NULL AND TRIM(strg_path_nm)<>'')=4
) usable_cases);
-- CASE drafts should not have upload-owned objects/owner slots. Unexpected associations stop, not erase uploads.
SET @reset_uploaded_refs=(SELECT COUNT(*) FROM tmd_mdl_inptupldfiled
 WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts)
 OR prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests));
SET @reset_slot_refs=(SELECT COUNT(*) FROM tmd_mdl_inptupldactvm
 WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts)
 OR prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests));
SET @reset_upload_refs=@reset_uploaded_refs+@reset_slot_refs;
SET @reset_pending_files=(SELECT COUNT(*) FROM tmd_mdl_inptfnsvfiled
 WHERE prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests) AND prcs_stat_cd NOT IN ('COMMITTED','CLEANED'));
-- Unknown inbound FK / cascading delete must never broaden this reset to another table.
SET @reset_unknown_fk=(SELECT COUNT(*) FROM information_schema.referential_constraints r
 WHERE r.unique_constraint_schema=DATABASE()
 AND r.referenced_table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftprcsl',
     'tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrvsnl','tmd_mdl_inptfnsvfiled')
 AND (r.constraint_schema<>DATABASE() OR r.delete_rule NOT IN ('RESTRICT','NO ACTION')
 OR r.table_name NOT IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftprcsl',
     'tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrvsnl','tmd_mdl_inptfnsvfiled',
     'tmd_mdl_inptupldfiled','tmd_mdl_inptupldactvm','tmd_mdl_inptexecm')));
SET @reset_engines=(SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
 AND table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftprcsl',
     'tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrvsnl','tmd_mdl_inptfnsvfiled',
     'tmd_mdl_scnrcasetmpltd') AND engine='InnoDB');
SET @rb_ok=IF(CAST(DATABASE() AS BINARY)=CAST('midb' AS BINARY) AND @@session.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@rb_actor)) BETWEEN 1 AND 50 AND @rb_template IS NOT NULL
 AND @rb_editor=1 AND @rb_source_deprecated=1 AND @rb_old_binding=1 AND @rb_ready_tables=52
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasem WHERE case_id IN (108)
      AND del_yn='N' AND exec_type_cd IN ('INDEPENDENT','MI_BASE'))=1
 AND @rb_busy=0 AND @rb_file_cases=1 AND @reset_runner_refs=0 AND @reset_upload_refs=0
 AND @reset_pending_files=0 AND @reset_unknown_fk=0 AND @reset_engines=8
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@rb_template AND del_yn='N')=4
 AND (SELECT COUNT(DISTINCT mdl_file_cd) FROM tmd_mdl_inptfiled WHERE tmplt_id=@rb_template
      AND del_yn='N' AND mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT'))=4,1,0);
SELECT @rb_template AS editor_active_template_id,@rb_editor AS editor_version_expect_1,
 @rb_source_deprecated AS template7_deprecated_expect_1,@rb_old_binding AS case108_on_template7_expect_1,
 @rb_ready_tables AS ready_tables_expect_52,@rb_drafts AS active_drafts_to_delete,
 (SELECT COUNT(*) FROM tmp_case_reset_drafts) AS total_drafts_to_delete,
 @rb_busy AS active_requests_expect_0,@rb_file_cases AS usable_file_cases_expect_1,
 @reset_pending_files AS unfinished_file_cleanup_expect_0,@reset_runner_refs AS runner_refs_expect_0,
 @reset_upload_refs AS upload_refs_expect_0,@reset_unknown_fk AS unknown_fk_expect_0,
 IF(@rb_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;


-- Target counts BEFORE deletion; review these exact counts and the section1 full Draft list.
SET @cnt_draft=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE org_case_id IN (108));
SET @cnt_draft_file=(SELECT COUNT(*) FROM tmd_mdl_inptdrftfiled WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_revision=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrvsnl WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_row_identity=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrowm WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_row_position=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrowh WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_request=(SELECT COUNT(*) FROM tmd_mdl_inptdrftprcsl WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_finalization_preparation=(SELECT COUNT(*) FROM tmd_mdl_inptfnsvfiled WHERE prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests));
SELECT 'draft' AS item,@cnt_draft AS row_count
UNION ALL SELECT 'draft_file' AS item,@cnt_draft_file AS row_count
UNION ALL SELECT 'revision' AS item,@cnt_revision AS row_count
UNION ALL SELECT 'row_identity' AS item,@cnt_row_identity AS row_count
UNION ALL SELECT 'row_position' AS item,@cnt_row_position AS row_count
UNION ALL SELECT 'request' AS item,@cnt_request AS row_count
UNION ALL SELECT 'finalization_preparation' AS item,@cnt_finalization_preparation AS row_count;

-- 3. Delete children before parents. Every DELETE is gated and scoped to the frozen Case108 IDs.
DELETE FROM tmd_mdl_inptfnsvfiled WHERE @rb_ok=1 AND prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests);
DELETE FROM tmd_mdl_inptdrftrowh WHERE @rb_ok=1 AND drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts);
DELETE FROM tmd_mdl_inptdrftrowm WHERE @rb_ok=1 AND drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts);
-- Single-table DELETE permits ORDER BY; revision self-FK is child revision -> preceding base revision.
DELETE FROM tmd_mdl_inptdrftrvsnl WHERE @rb_ok=1 AND drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts)
ORDER BY rvsn_no DESC,drft_id;
DELETE FROM tmd_mdl_inptdrftfiled WHERE @rb_ok=1 AND drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts);
DELETE FROM tmd_mdl_inptdrftprcsl WHERE @rb_ok=1 AND prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests);
DELETE FROM tmd_mdl_inptdrftm WHERE @rb_ok=1 AND drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts);
UPDATE tmd_mdl_scnrcasetmpltd SET tmplt_id=@rb_template,del_yn='N',mod_usr_logn_id=@rb_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @rb_ok=1 AND case_id IN (108) AND (tmplt_id<>@rb_template OR del_yn<>'N');
INSERT INTO tmd_mdl_scnrcasetmpltd (case_id,tmplt_id,crt_usr_logn_id,mod_usr_logn_id,del_yn)
SELECT c.case_id,@rb_template,@rb_actor,@rb_actor,'N'
FROM tmd_mdl_scnrcasem c LEFT JOIN tmd_mdl_scnrcasetmpltd b ON b.case_id=c.case_id
WHERE @rb_ok=1 AND c.case_id IN (108) AND b.case_id IS NULL;

-- 4. Seven remaining counters must be zero, the one binding must point to the editor ACTIVE catalog.
SET @cnt_draft=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE org_case_id IN (108));
SET @cnt_draft_file=(SELECT COUNT(*) FROM tmd_mdl_inptdrftfiled WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_revision=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrvsnl WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_row_identity=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrowm WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_row_position=(SELECT COUNT(*) FROM tmd_mdl_inptdrftrowh WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_request=(SELECT COUNT(*) FROM tmd_mdl_inptdrftprcsl WHERE drft_id IN (SELECT drft_id FROM tmp_case_reset_drafts));
SET @cnt_finalization_preparation=(SELECT COUNT(*) FROM tmd_mdl_inptfnsvfiled WHERE prcs_id IN (SELECT prcs_id FROM tmp_case_reset_requests));
SET @reset_remaining=@cnt_draft+@cnt_draft_file+@cnt_revision+@cnt_row_identity+@cnt_row_position+@cnt_request+@cnt_finalization_preparation;
SET @rb_verified=IF(@rb_ok=1 AND @reset_remaining=0 AND
 (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE case_id IN (108) AND tmplt_id=@rb_template AND del_yn='N')=1,1,0);
SELECT IF(@rb_verified=1,'READY_TO_COMMIT','STOP_AND_ROLLBACK') AS decision,@rb_template AS new_template_id;
SELECT 'draft' AS item,@cnt_draft AS remaining_expect_0
UNION ALL SELECT 'draft_file' AS item,@cnt_draft_file AS remaining_expect_0
UNION ALL SELECT 'revision' AS item,@cnt_revision AS remaining_expect_0
UNION ALL SELECT 'row_identity' AS item,@cnt_row_identity AS remaining_expect_0
UNION ALL SELECT 'row_position' AS item,@cnt_row_position AS remaining_expect_0
UNION ALL SELECT 'request' AS item,@cnt_request AS remaining_expect_0
UNION ALL SELECT 'finalization_preparation' AS item,@cnt_finalization_preparation AS remaining_expect_0;
SELECT b.case_id,b.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,b.del_yn
FROM tmd_mdl_scnrcasetmpltd b JOIN tmd_mdl_inpttmpltm t USING(tmplt_id)
WHERE b.case_id IN (108) ORDER BY b.case_id;

-- 5. Rehearsal default. For actual apply select COMMIT instead after successful section4.
ROLLBACK;
-- COMMIT;
-- Before COMMIT, ROLLBACK restores everything. After COMMIT erased Drafts need backup restore;
-- don't blindly restore on top of newly created Drafts/requests. Existing Case/file/catalog are retained.
-- No schema/dictionary change. Implementation readiness: ready for manual precheck/rehearsal.

-- 6. After committed apply, Case108 needs its own fresh sourceVersion:
-- GET /api/v1/mi/fundamental-model/cases/{caseId}/inputs/structure
-- POST /api/v1/mi/fundamental-model/cases/{caseId}/input-drafts
-- body: {"sourceVersion":"<fresh data.sourceVersion from that Case's GET>"}
-- Expect created=true, revision=0; never fabricate Draft/history rows directly.
-- SQL checks file metadata only; actual remote XLSM structure/readability and API permissions remain API checks.
