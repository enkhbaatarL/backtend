-- Model Input upload deadline: preserve historical 120-second rows and admit new 600-second rows.
-- Document Control
-- status: Draft
-- canonical for: tmd_mdl_inptdrftprcsl_ck09 constraint migration, verification, and rollback
-- owner: BE / DBA
-- date: 2026-10-21
-- source documents: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md rule 67;
--                   docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md section 3.3
-- depends on: docs/model-input-incremental-upload-ddl-draft.sql
-- supersedes: ck09 requiring exactly 120 seconds for UPLOAD_CREATE/UPLOAD_FILE
-- Manual DBA draft. Writing this file does not change testdb.

-- 1. PRE-CHECK. Stop all writers to this master DB and confirm no active operation.
SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='tmd_mdl_inptdrftprcsl'
  AND tc.CONSTRAINT_NAME='tmd_mdl_inptdrftprcsl_ck09';
-- Expected before first apply: expr_dtm = crt_dtm + INTERVAL 120 SECOND.
SELECT prcs_type_cd, TIMESTAMPDIFF(SECOND,crt_dtm,expr_dtm) AS deadline_seconds,
       COUNT(*) AS operations
FROM tmd_mdl_inptdrftprcsl
GROUP BY prcs_type_cd, TIMESTAMPDIFF(SECOND,crt_dtm,expr_dtm)
ORDER BY prcs_type_cd, deadline_seconds;
-- Expected before first apply: every existing request has deadline_seconds=120.
SELECT COUNT(*) AS active_operations FROM tmd_mdl_inptdrftprcsl
WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
-- Expected: 0. If not 0, do not apply or restart yet.

-- 2. APPLY. One ALTER replaces the CHECK without changing columns or old rows.
ALTER TABLE tmd_mdl_inptdrftprcsl
    DROP CHECK tmd_mdl_inptdrftprcsl_ck09,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck09 CHECK(
        expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm)
        OR (prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')
            AND expr_dtm=TIMESTAMPADD(SECOND,600,crt_dtm))
    );

-- 3. BACKFILL: none. Existing 120-second records remain valid and unchanged.

-- 4. VERIFY. Both queries must show the new expression and zero invalid rows.
SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='tmd_mdl_inptdrftprcsl'
  AND tc.CONSTRAINT_NAME='tmd_mdl_inptdrftprcsl_ck09';
SELECT COUNT(*) AS invalid_deadlines FROM tmd_mdl_inptdrftprcsl
WHERE NOT (expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm)
    OR (prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')
        AND expr_dtm=TIMESTAMPADD(SECOND,600,crt_dtm)));
-- Expected: invalid_deadlines=0. After new-code deploy, new UPLOAD rows use 600;
-- CHANGE/FINALIZE and pre-migration history continue to use 120.

-- 5. ROLLBACK / DEPLOYMENT.
-- Apply and verify this DDL before starting code that creates 600-second UPLOAD requests.
-- An older backend writing 120-second requests remains compatible with the widened CHECK.
-- If the new image must be rolled back after any 600-second row exists, restore the old
-- image but retain this CHECK. Do not rewrite historical deadlines or restore the old
-- 120-only CHECK while 600-second rows remain. If no 600-second row was ever written,
-- a separate reviewed ALTER may restore the previous CHECK after stopping writers.
-- Preserve all operations, Drafts, uploaded files, and private objects.
-- Implementation input: ready. DBA apply readiness requires actual pre-check and approval.
