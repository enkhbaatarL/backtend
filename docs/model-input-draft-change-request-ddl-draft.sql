-- Document Control
-- status: Draft / USER REVIEW AND MANUAL APPLY ONLY
-- canonical for: EDT-04/05 incremental apply/verify SQL
-- owner: BE / DBA (user applies); date: 2026-10-18
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.3/6.7
-- Codex does not connect to or execute SQL against the real database.
-- Existing catalog/Draft tables and Case103 data MUST NOT be recreated or reset.
-- Audit DATETIME alignment (2026-10-19): existing tables use model-input-audit-precision-ddl-draft.sql.
-- New requests store whole-second crt_dtm and derive expr_dtm from that same value; precise deadline comparisons remain.
-- Execute each section only after reviewing its preconditions. MySQL DDL implicitly commits.

-- 1. PRE-CHECK: use the same master schema as the application; retain output.
SELECT DATABASE(), VERSION(), @@max_allowed_packet, @@transaction_isolation;
SELECT table_name, engine, table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftprcsl','tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm');
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
-- Expected: three existing InnoDB tables; request table absent. Compare CHAR(36) ascii_bin IDs.
-- Expected: no active pointer and no unbound prcs_id before the first request-table rollout.
SELECT COUNT(*) AS active_pointer_count FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL;
SELECT COUNT(*) AS revision_request_count FROM tmd_mdl_inptdrftrvsnl WHERE prcs_id IS NOT NULL;
SELECT COUNT(*) AS added_row_request_count FROM tmd_mdl_inptdrftrowm WHERE prcs_id IS NOT NULL;
-- All three counts must be zero for this initial rollout. Otherwise STOP and investigate.
-- Confirm MySQL CHECK enforcement, DATETIME audit / DATETIME(6) processing times, nullable generated UK and packet headroom
-- for 1 MiB request/result/error payloads. Session isolation above is only the DBA connection.
-- App transactions explicitly use READ_COMMITTED; verify app connection separately.
-- If the request table or the below FK names already exist, compare SHOW CREATE TABLE;
-- skip matching statements individually. Do not use IF NOT EXISTS to hide incompatible definitions.

-- 2. APPLY: stop Draft writes, then create only the missing request table.
CREATE TABLE tmd_mdl_inptdrftprcsl
(
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    req_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    prcs_type_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    usr_role_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    base_rvsn_no BIGINT UNSIGNED NOT NULL,
    prcs_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    json_rvsn_no SMALLINT UNSIGNED NOT NULL,
    req_json LONGTEXT NOT NULL,
    expr_dtm DATETIME(6) NOT NULL,
    try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    bgn_dtm DATETIME(6) NULL,
    end_dtm DATETIME(6) NULL,
    rslt_json LONGTEXT NULL,
    fail_json LONGTEXT NULL,
    actv_drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
        (IF(prcs_stat_cd IN ('QUEUED','RUNNING'),drft_id,NULL)) STORED,
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux01 UNIQUE(drft_id,req_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux02 UNIQUE(actv_drft_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux03 UNIQUE(drft_id,prcs_id),
    INDEX tmd_mdl_inptdrftprcsl_ix01(prcs_stat_cd,expr_dtm),
    CONSTRAINT tmd_mdl_inptdrftprcsl_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck01 CHECK(prcs_type_cd IN ('CHANGE','FINALIZE')),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck02 CHECK(prcs_stat_cd IN ('QUEUED','RUNNING','SUCCEEDED','FAILED')),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck03 CHECK(base_rvsn_no<=9007199254740991 AND json_rvsn_no=1),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck04 CHECK(JSON_VALID(req_json) AND OCTET_LENGTH(req_json)<=1048576),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck05 CHECK(rslt_json IS NULL OR (JSON_VALID(rslt_json) AND OCTET_LENGTH(rslt_json)<=1048576)),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck06 CHECK(fail_json IS NULL OR (JSON_VALID(fail_json) AND OCTET_LENGTH(fail_json)<=1048576)),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck07 CHECK(
        (prcs_stat_cd='QUEUED' AND try_tokn IS NULL AND bgn_dtm IS NULL AND end_dtm IS NULL AND rslt_json IS NULL AND fail_json IS NULL)
        OR (prcs_stat_cd='RUNNING' AND try_tokn IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NULL AND rslt_json IS NULL AND fail_json IS NULL)
        OR (prcs_stat_cd='SUCCEEDED' AND try_tokn IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NOT NULL AND rslt_json IS NOT NULL AND fail_json IS NULL)
        OR (prcs_stat_cd='FAILED' AND end_dtm IS NOT NULL AND rslt_json IS NULL AND fail_json IS NOT NULL
            AND ((try_tokn IS NULL AND bgn_dtm IS NULL) OR (try_tokn IS NOT NULL AND bgn_dtm IS NOT NULL)))),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck08 CHECK(del_yn='N'),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck09 CHECK(expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add only the previously deferred request references. Existing revision0/source rows remain unchanged.
ALTER TABLE tmd_mdl_inptdrftrvsnl
    ADD CONSTRAINT tmd_mdl_inptdrftrvsnl_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id);
ALTER TABLE tmd_mdl_inptdrftrowm
    ADD CONSTRAINT tmd_mdl_inptdrftrowm_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id);

-- 3. BACKFILL: none. Preserve existing Case103 Draft/files/revision0/row IDs.
-- No policy seed, final-file table or data migration is included.

-- 4. VERIFY: compare SHOW CREATE/INDEX and retain the pre/post existing row counts/IDs.
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SHOW INDEX FROM tmd_mdl_inptdrftprcsl;
SELECT COUNT(*) AS invalid_active_pointer_count FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptdrftprcsl p ON p.drft_id=d.drft_id AND p.prcs_id=d.actv_prcs_id
WHERE d.actv_prcs_id IS NOT NULL AND (p.prcs_id IS NULL OR p.prcs_stat_cd NOT IN ('QUEUED','RUNNING'));
SELECT COUNT(*) AS missing_active_pointer_count FROM tmd_mdl_inptdrftprcsl p
JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
WHERE p.prcs_stat_cd IN ('QUEUED','RUNNING') AND (d.actv_prcs_id IS NULL OR d.actv_prcs_id<>p.prcs_id);
-- Both counters must be zero. New public CHANGE requests currently reject unsupported processing;
-- they must not create operations/revisions or modify existing cell/row values.

-- 5. ROLLBACK / DEPLOYMENT
-- Apply DDL before new application code. Old read code remains usable with no active operations.
-- Stop new admissions before app rollback. Confirm pending/unknown commits on the write DB first.
-- Keep request history, new table and FK constraints; no DROP/DELETE/TRUNCATE rollback.
-- If DDL partially succeeds, keep existing data and compare definitions before applying missing steps.
-- DBA owner decides go/no-go after actual MySQL constraints/privileges/packet and application checks.
