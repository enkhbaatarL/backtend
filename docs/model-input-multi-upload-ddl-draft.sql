-- Input upload initial batch: one operation -> 1..4 files.
-- Document Control
-- status: Draft
-- canonical for: Schema rollout (initial multi-file upload index migration)
-- owner: BE / DBA
-- date: 2026-10-20
-- source documents: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rule 67
-- depends on: docs/model-input-incremental-upload-ddl-draft.sql
-- supersedes: UNIQUE(prcs_id) on tmd_mdl_inptupldfiled only
-- This file is a manual DBA draft; no database was changed by writing it.

-- 1. PRE-CHECK. Stop new upload admission and drain/stop API workers first.
-- Apply the incremental-upload baseline before this migration, including on new installations.
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
SELECT index_name,non_unique,seq_in_index,column_name
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptupldfiled'
ORDER BY index_name,seq_in_index;
-- Expected before first apply: ux01 is UNIQUE(prcs_id); ix02 does not exist.
-- Verify actual names and FK indexes. If ix02 already exists, inspect it before any retry.
SELECT COUNT(*) AS pending_uploads FROM tmd_mdl_inptdrftprcsl
WHERE prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE') AND prcs_stat_cd IN ('QUEUED','RUNNING');
-- Expected 0 after draining. Preserve all successful drafts/files/history.

-- 2. APPLY. Keep prcs_id indexed for operation lookup/cleanup; retain every PK/FK/other UK.
ALTER TABLE tmd_mdl_inptupldfiled
    ADD INDEX tmd_mdl_inptupldfiled_ix02(prcs_id),
    DROP INDEX tmd_mdl_inptupldfiled_ux01;

-- 3. BACKFILL: none. Existing one-file operations remain valid.

-- 4. VERIFY. ix02 must be non_unique=1 on prcs_id; ux01 must be absent.
SELECT index_name,non_unique,seq_in_index,column_name
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptupldfiled'
  AND index_name IN ('tmd_mdl_inptupldfiled_ux01','tmd_mdl_inptupldfiled_ix02');
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
-- Substitute an existing operation UUID and confirm indexed lookup on representative data.
EXPLAIN SELECT * FROM tmd_mdl_inptupldfiled
WHERE prcs_id='00000000-0000-4000-8000-000000000000' ORDER BY upld_file_id;
SELECT p.prcs_id FROM tmd_mdl_inptdrftprcsl p
LEFT JOIN tmd_mdl_inptupldfiled u ON u.prcs_id=p.prcs_id
WHERE p.prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')
GROUP BY p.prcs_id,p.prcs_type_cd
HAVING COUNT(u.upld_file_id)<1 OR COUNT(u.upld_file_id)>4
    OR (p.prcs_type_cd='UPLOAD_FILE' AND COUNT(u.upld_file_id)<>1);
-- Expected 0 rows. Failed role-detection/duplicate-role requests may retain invalid kind metadata.
SELECT p.prcs_id FROM tmd_mdl_inptdrftprcsl p
JOIN tmd_mdl_inptupldfiled u ON u.prcs_id=p.prcs_id
WHERE p.prcs_type_cd='UPLOAD_CREATE' AND p.prcs_stat_cd='SUCCEEDED'
GROUP BY p.prcs_id,p.drft_id
HAVING COUNT(*)<>COUNT(DISTINCT u.mdl_file_cd)
    OR COUNT(DISTINCT u.tmplt_id)<>1
    OR SUM(CASE WHEN u.prcs_stat_cd='COMMITTED' AND u.drft_id=p.drft_id
        AND u.file_rvsn_no=0 THEN 0 ELSE 1 END)<>0;
-- Expected 0 rows. Also run the baseline DDL's owner/current-pointer/revision verification queries.

-- 5. DEPLOYMENT / ROLLBACK.
-- Apply and verify this migration before enabling multi-file admission.
-- Deploy compatible API and workers together. Old operationFile readers handle only one file;
-- do not mix old/new workers once multi-file operations can exist.
-- On failure: stop admission, drain/check actual commits, preserve data and private objects,
-- and deploy a compatible reader/worker. Do not restore UNIQUE(prcs_id) after batch records exist.
-- No DELETE/TRUNCATE/DROP TABLE or history reset is part of this migration or rollback.
-- Implementation input: ready. DBA apply readiness requires actual MySQL pre-check/verification.
