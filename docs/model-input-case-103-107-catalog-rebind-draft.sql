-- Document Control
-- status: Draft / TESTDB MANUAL APPLY ONLY / NOT EXECUTED BY CODEX
-- canonical for: Case103/107 latest ACTIVE catalog binding apply/verify/rollback SQL
-- owner: BE / DBA; date / last synced: 2026-10-20
-- depends on: docs/model-input-draft-read-ddl-draft.sql (Case binding / Draft keys)
--             docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md
-- execution record: docs/work-notes/2026/2026-10-20-model-input-case-103-107-catalog-rebind.md
-- Scope: only tmd_mdl_scnrcasetmpltd rows for case_id IN (103,107).
-- Use the one published ACTIVE of model DRAFT_C_STRUCTURE, not global MAX(tmplt_id).
-- No catalog publication/definition/file/Case/Draft/history/request changes or deletions.
-- Existing four-role Case files are reused. SQL cannot verify remote workbook bytes/structure.
-- If an active draft exists for either Case, STOP: changing the binding does not migrate its pinned catalog.
-- Existing FINALIZED/DISCARDED drafts remain stored, but re-reading them after a changed binding
-- may fail SOURCE_CHANGED because the API recalculates the Case sourceVersion with the current catalog.
-- Pause the two Cases' draft creation/edit/finalization, file replacement and catalog publication during apply.
-- Dedicated connection, no pending transaction, master midb, InnoDB/FK checks enabled.
-- On any error/STOP: ROLLBACK and stop; don't override guards or use --force.
-- Rehearsal: run whole file (ends ROLLBACK). Apply: run sections1~3,
-- inspect READY_TO_COMMIT, then execute COMMIT instead of section4 ROLLBACK.

-- 1. Read-only inventory. Review existing bindings/drafts/files first.
USE midb;
SET @rb_actor='SYSTEM';
SET @rb_model='DRAFT_C_STRUCTURE';
SET @rb_ok=0,@rb_verified=0,@rb_template=NULL;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,del_yn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model ORDER BY tmplt_ver_no DESC;
SELECT c.case_id,c.exec_type_cd,c.del_yn,b.tmplt_id,b.del_yn AS binding_del_yn
FROM tmd_mdl_scnrcasem c LEFT JOIN tmd_mdl_scnrcasetmpltd b ON b.case_id=c.case_id
WHERE c.case_id IN (103,107) ORDER BY c.case_id;
SELECT drft_id,org_case_id,cust_cpny_no,usr_no,tmplt_id,drft_stat_cd,rvsn_no,actv_prcs_id,del_yn
FROM tmd_mdl_inptdrftm WHERE org_case_id IN (103,107) ORDER BY org_case_id,drft_id;
-- Save this before inventory if a post-COMMIT reversal may be needed.

-- 2. Lock / precheck. All following changes are in this one transaction.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT case_id FROM tmd_mdl_scnrcasem WHERE case_id IN (103,107) ORDER BY case_id FOR UPDATE;
SELECT case_id,tmplt_id,del_yn FROM tmd_mdl_scnrcasetmpltd WHERE case_id IN (103,107) ORDER BY case_id FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE org_case_id IN (103,107) ORDER BY drft_id FOR UPDATE;
SELECT p.prcs_id FROM tmd_mdl_inptdrftprcsl p JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
WHERE d.org_case_id IN (103,107) ORDER BY p.prcs_id FOR UPDATE;
SELECT file_no FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no IN (103,107)
ORDER BY file_no FOR UPDATE;
SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model ORDER BY tmplt_id FOR UPDATE;
-- Multiple ACTIVE rows deliberately cause a scalar-subquery error, rather than picking one arbitrarily.
SET @rb_template=(SELECT tmplt_id FROM tmd_mdl_inpttmpltm
 WHERE mdl_spt_cd=@rb_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N' AND aply_bgn_dtm IS NOT NULL);
SET @rb_today=DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+09:00'),'%Y%m%d');
SET @rb_drafts=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm
 WHERE org_case_id IN (103,107) AND drft_stat_cd='DRAFT' AND del_yn='N');
SET @rb_busy=(SELECT COUNT(*) FROM tmd_mdl_inptdrftprcsl p JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
 WHERE d.org_case_id IN (103,107) AND p.prcs_stat_cd IN ('QUEUED','RUNNING'));
-- Same role selection as ModelInputDraftService.source(): filename contains exactly one of the four roles.
SET @rb_file_cases=(SELECT COUNT(*) FROM (
 SELECT file_rel_obj_dstg_no FROM tcm_filel
 WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no IN (103,107) AND del_yn='N'
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
SET @rb_ok=IF(CAST(DATABASE() AS BINARY)=CAST('midb' AS BINARY) AND @@session.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@rb_actor)) BETWEEN 1 AND 50 AND @rb_template IS NOT NULL
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@rb_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasem WHERE case_id IN (103,107)
      AND del_yn='N' AND exec_type_cd IN ('INDEPENDENT','MI_BASE'))=2
 AND @rb_drafts=0 AND @rb_busy=0 AND @rb_file_cases=2
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@rb_template AND del_yn='N')=4
 AND (SELECT COUNT(DISTINCT mdl_file_cd) FROM tmd_mdl_inptfiled WHERE tmplt_id=@rb_template
      AND del_yn='N' AND mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT'))=4,1,0);
SELECT @rb_template AS latest_active_template_id,@rb_drafts AS active_drafts_expect_0,
 @rb_busy AS active_requests_expect_0,@rb_file_cases AS usable_file_cases_expect_2,
 IF(@rb_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;

-- 3. Change only existing bindings; register the binding if a Case has none.
UPDATE tmd_mdl_scnrcasetmpltd SET tmplt_id=@rb_template,del_yn='N',
 mod_usr_logn_id=@rb_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @rb_ok=1 AND case_id IN (103,107) AND (tmplt_id<>@rb_template OR del_yn<>'N');
INSERT INTO tmd_mdl_scnrcasetmpltd
 (case_id,tmplt_id,crt_usr_logn_id,mod_usr_logn_id,del_yn)
SELECT c.case_id,@rb_template,@rb_actor,@rb_actor,'N'
FROM tmd_mdl_scnrcasem c LEFT JOIN tmd_mdl_scnrcasetmpltd b ON b.case_id=c.case_id
WHERE @rb_ok=1 AND c.case_id IN (103,107) AND b.case_id IS NULL;
SET @rb_verified=IF(@rb_ok=1 AND
 (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE case_id IN (103,107) AND tmplt_id=@rb_template AND del_yn='N')=2,1,0);
SELECT IF(@rb_verified=1,'READY_TO_COMMIT','STOP_AND_ROLLBACK') AS decision;
SELECT b.case_id,b.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,b.del_yn
FROM tmd_mdl_scnrcasetmpltd b JOIN tmd_mdl_inpttmpltm t USING(tmplt_id)
WHERE b.case_id IN (103,107) ORDER BY b.case_id;

-- 4. Default rehearsal; choose COMMIT instead of this ROLLBACK for reviewed actual apply.
ROLLBACK;
-- COMMIT;
-- Before COMMIT: ROLLBACK restores all writes. After COMMIT, do not blindly restore old bindings:
-- inspect any newly created drafts/requests and use the saved before inventory for a separate reversal.
-- No DDL/dictionary term additions needed. Implementation readiness: ready for manual precheck/rehearsal.

-- 5. After committed apply, use authenticated API for each Case separately:
-- GET /api/v1/mi/fundamental-model/cases/{caseId}/inputs/structure
-- POST /api/v1/mi/fundamental-model/cases/{caseId}/input-drafts
-- body: {"sourceVersion":"<fresh data.sourceVersion from that Case's GET>"}
-- Verify created=true / revision=0 and GET the returned draftEndpoint.
-- Do not directly INSERT drafts/history/row IDs or reuse an old/other Case's sourceVersion.
