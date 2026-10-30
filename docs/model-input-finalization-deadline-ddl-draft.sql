-- Model Input finalization deadline compatibility migration
-- Document Control
-- status: Draft
-- canonical for: tmd_mdl_inptdrftprcsl_ck09 migration, verification and rollback
-- owner: BE / DBA
-- date: 2026-10-30
-- source documents: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rule 64
-- depends on: docs/model-input-upload-deadline-ddl-draft.sql
-- supersedes: ck09 allowing 600 seconds only for UPLOAD_CREATE/UPLOAD_FILE
-- Manual DBA draft. No DB changes are made by creating this file.

-- 1. PRE-CHECK: stop intake and all writers; wait for QUEUED/RUNNING=0.
SELECT DATABASE(), VERSION();
SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE, tc.ENFORCED
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='tmd_mdl_inptdrftprcsl'
  AND tc.CONSTRAINT_NAME='tmd_mdl_inptdrftprcsl_ck09';
-- Confirm current definition and ENFORCED=YES; do not apply against an unexpected definition.
SELECT prcs_type_cd, TIMESTAMPDIFF(SECOND,crt_dtm,expr_dtm) AS deadline_seconds, COUNT(*) AS operations
FROM tmd_mdl_inptdrftprcsl GROUP BY prcs_type_cd, TIMESTAMPDIFF(SECOND,crt_dtm,expr_dtm);
SELECT COUNT(*) AS active_operations FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
-- Expected active_operations=0. Check all existing rows against the new CHECK before ALTER.
SELECT COUNT(*) AS invalid_deadlines FROM tmd_mdl_inptdrftprcsl
WHERE NOT (expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm)
    OR (prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE','FINALIZE') AND expr_dtm=TIMESTAMPADD(SECOND,600,crt_dtm)));
-- Expected invalid_deadlines=0.

-- 2. APPLY: widening only. Stop if pre-check differs. MySQL DDL implicitly commits.
ALTER TABLE tmd_mdl_inptdrftprcsl
    DROP CHECK tmd_mdl_inptdrftprcsl_ck09,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck09 CHECK(
        expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm)
        OR (prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE','FINALIZE')
            AND expr_dtm=TIMESTAMPADD(SECOND,600,crt_dtm))
    );

-- 3. BACKFILL: none; historical deadlines and active state are never rewritten.
-- 4. VERIFY: repeat the constraint definition and invalid_deadlines pre-checks.
-- Expected: ENFORCED=YES, FINALIZE included for 600, invalid_deadlines=0.
-- 5. ROLLBACK: old code can run with the widened CHECK; keep it if any 600-second
-- FINALIZE row exists. Do not rewrite/delete history to restore the old constraint.
-- Only if no such row exists, and writers are stopped, DBA may restore the previous
-- upload-only CHECK from docs/model-input-upload-deadline-ddl-draft.sql.
-- Deployment: pre-check -> apply -> verify -> compatible backend deployment ->
-- confirm new FINALIZE uses 600 seconds and completes. No DDL was applied here.
