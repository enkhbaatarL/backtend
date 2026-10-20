-- Document Control
-- status: Draft / USER-APPROVED SCOPE; MANUAL APPLY ONLY; NOT EXECUTED
-- canonical for: Schema rollout — A web runner state, precheck/create/initialize/verify/preserving rollback
-- owner: BE / DBA; date / last synced: 2026-10-20
-- depends on: design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.11
--             design-docs/gas/mi-model-input-template-catalog-domain-model-design.md §5.5.3
-- supersedes: none
-- open questions: real MySQL constraints, two-connection locking and manual deployment verification.
-- Codex does not connect to or apply SQL against the real DB. Existing tables/data/ck09 are unchanged.

-- 1. PRE-CHECK. Keep mi.model-input.web-runner.enabled=false until verified.
SELECT DATABASE(), VERSION();
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptfnsvfiled;
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptexecm';
-- If present, compare SHOW CREATE and existing state; never reset/reseed it.
SELECT COUNT(*) AS running_count FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd='RUNNING';
SELECT COUNT(*) AS pending_final_files FROM tmd_mdl_inptfnsvfiled
WHERE prcs_stat_cd IN ('PREPARED','UPLOADED','CLEANUP_PENDING');
SELECT COUNT(*) AS unresolved_upload_files FROM tmd_mdl_inptupldfiled
WHERE prcs_stat_cd IN ('PREPARED','CLEANUP_PENDING');
-- Initial activation requires the operator to confirm all previous execution/transfers stopped,
-- resolve unknown commits and recover outstanding work. Counts0 alone are NOT termination proof.
-- Existing dedicated worker shutdown is the user's responsibility; no mixed operation is supported.

-- 2. APPLY. New table only; no ALTER of existing request/file tables.
CREATE TABLE tmd_mdl_inptexecm
(
    exec_no TINYINT UNSIGNED NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    exec_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    exec_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    bgn_dtm DATETIME(6) NULL,
    end_dtm DATETIME(6) NULL,
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    aprv_exec_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    aprv_usr_logn_id VARCHAR(50) NULL,
    aprv_dtm DATETIME(6) NULL,
    INDEX tmd_mdl_inptexecm_ix01(prcs_id),
    CONSTRAINT tmd_mdl_inptexecm_fk01 FOREIGN KEY(prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptexecm_ck01 CHECK(exec_no=1 AND del_yn='N'),
    CONSTRAINT tmd_mdl_inptexecm_ck02 CHECK(
        (exec_stat_cd='STOPPED' AND prcs_id IS NULL AND try_tokn IS NULL
            AND ((exec_id IS NULL AND bgn_dtm IS NULL AND end_dtm IS NULL)
                OR (exec_id IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NOT NULL)))
        OR (exec_stat_cd IN ('ACTIVE','RECOVERING') AND exec_id IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck03 CHECK(
        (prcs_id IS NULL AND try_tokn IS NULL) OR (prcs_id IS NOT NULL AND try_tokn IS NOT NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck04 CHECK(
        (aprv_exec_id IS NULL AND aprv_usr_logn_id IS NULL AND aprv_dtm IS NULL)
        OR (aprv_exec_id IS NOT NULL AND aprv_usr_logn_id IS NOT NULL AND aprv_dtm IS NOT NULL AND exec_id IS NOT NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck05 CHECK(exec_stat_cd<>'RECOVERING' OR aprv_exec_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. INITIALIZE / BACKFILL. No existing history backfill. Run ONCE after the above operator checks.
-- Replace the audit actor with the approved operator identity before running; this is not a credential.
-- Missing row is a blocked runner, NOT permission for the app to bootstrap automatically.
INSERT INTO tmd_mdl_inptexecm(exec_no,crt_usr_logn_id,mod_usr_logn_id,exec_stat_cd)
VALUES(1,'REPLACE_WITH_OPERATOR','REPLACE_WITH_OPERATOR','STOPPED');

-- 4. VERIFY. Expected exactly1 row; initial exec_id/prcs_id/try_tokn/approval NULL, status STOPPED.
SHOW CREATE TABLE tmd_mdl_inptexecm;
SHOW INDEX FROM tmd_mdl_inptexecm;
SELECT exec_no,exec_id,exec_stat_cd,prcs_id,try_tokn,aprv_exec_id,aprv_usr_logn_id,aprv_dtm
FROM tmd_mdl_inptexecm;
-- Verify actual CHECK enforcement and unchanged request ck09; test competing GET_LOCK connections
-- and session death separately from H2. Compare existing table definitions/counts/PKs before/after.

-- 5. ROLLBACK / DBA DEPLOYMENT.
-- Keep A disabled -> precheck -> create/initialize/verify -> compatible app -> isolated fixture checks -> enable A.
-- Rollback: stop intake/runner gracefully; otherwise confirm actual termination and use the recovery command.
-- Keep this row and all request/file history. Never DELETE/TRUNCATE/DROP/reset to bypass ACTIVE/RECOVERING.
-- Do not revert to the dedicated worker without a separate verified handover.
-- Running this draft or a successful H2 test is not approval for operational deployment.
