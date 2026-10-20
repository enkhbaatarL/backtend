-- Document Control
-- status: Draft / MANUAL REVIEW AND APPLY ONLY; NOT EXECUTED
-- canonical for: Schema rollout — incremental upload precheck/apply/backfill/verify/preserving rollback SQL
-- owner: BE / DBA; date / last synced: 2026-10-19
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.10
--             domain-model-design.md §5.6 / rule67; existing audit / request / finalization / column-v2 DDL
-- supersedes: none; this is an ordered extension, not a replacement CREATE/reset script.
-- open questions: actual MySQL definitions/constraints, execution/concurrency and compatible application validation.
-- No real DB connection or SQL execution by Codex. No Case/file/catalog deletion or Draft reset.
-- SQL assumes the exact prior constraint names below; reconcile SHOW CREATE before applying.
-- MySQL DDL implicitly commits. Stop all affected writers/workers; retain PK backups and each step's result.
-- Keep mi.model-input.upload.enabled=false through application/DDL verification.
-- Before enabling: publish a reviewed template version with qry_json.uploadValidation fixed-label rules;
-- configure mi.model-input.upload.source-key / model-code and the proven worker/intake recovery paths.
-- Existing ACTIVE/DEPRECATED definitions are immutable. This script does not publish or mutate their rules.

-- 1. PRE-CHECK — read only. All previous Draft tables and v2/audit changes must already exist.
SELECT DATABASE(), VERSION(), @@transaction_isolation, @@max_allowed_packet;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftfiled;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SHOW CREATE TABLE tmd_mdl_inptdrftrowh;
SHOW CREATE TABLE tmd_mdl_inptfnsvfiled;
SELECT table_name,engine,table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN ('tmd_mdl_inptupldactvm','tmd_mdl_inptupldfiled');
-- Expected: new table names absent. If already applied, VERIFY only; do not rerun ALTER/backfill.
SELECT table_name,column_name,column_type,is_nullable,collation_name,datetime_precision
FROM information_schema.columns WHERE table_schema=DATABASE()
AND table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftprcsl','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftrowm')
ORDER BY table_name,ordinal_position;
SELECT tc.table_name,tc.constraint_name,tc.constraint_type,tc.enforced
FROM information_schema.table_constraints tc WHERE tc.constraint_schema=DATABASE()
AND tc.table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftprcsl','tmd_mdl_inptdrftrowm');
-- Expected CHECK enforcement; existing ck03 requires json_rvsn_no=2, audit DATETIME(0), processing DATETIME(6).
-- UUID CHAR(36) ascii_bin; company MEDIUMINT UNSIGNED; user/template INT UNSIGNED; file BIGINT UNSIGNED.
SELECT COUNT(*) AS pending_operations FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
SELECT COUNT(*) AS active_pointers FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL;
SELECT COUNT(*) AS invalid_legacy_request_owner FROM tmd_mdl_inptdrftprcsl p
LEFT JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id WHERE d.drft_id IS NULL;
SELECT COUNT(*) AS non_v2_revisions FROM tmd_mdl_inptdrftrvsnl WHERE json_rvsn_no<>2;
SELECT COUNT(*) AS non_v2_operations FROM tmd_mdl_inptdrftprcsl WHERE json_rvsn_no<>2;
-- All above counters must be zero. A mismatch is STOP, not permission to reset/delete data.
-- Capture counts and PK lists for all existing tables, especially Case/file/catalog and Draft row identities.
-- Confirm no unknown inbound FK/trigger, maintained backups, InnoDB, actual app READ_COMMITTED and privileges.

-- 2. APPLY A — preserve CASE data, extend nullability and owner references.
ALTER TABLE tmd_mdl_inptdrftm
    DROP FOREIGN KEY tmm_mdlinptdrftm_fk01,
    MODIFY org_case_id INT UNSIGNED NULL,
    MODIFY org_rvsn_val VARCHAR(68) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD src_spt_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'CASE',
    ADD actv_upld_usr_no INT UNSIGNED GENERATED ALWAYS AS
        (IF(src_spt_cd='UPLOAD' AND drft_stat_cd='DRAFT' AND del_yn='N',usr_no,NULL)) STORED,
    ADD CONSTRAINT tmd_mdl_inptdrftm_ux03 UNIQUE(cust_cpny_no,usr_no,drft_id),
    ADD CONSTRAINT tmd_mdl_inptdrftm_ux04 UNIQUE(cust_cpny_no,actv_upld_usr_no),
    ADD CONSTRAINT tmm_mdlinptdrftm_fk01 FOREIGN KEY(org_case_id)
        REFERENCES tmd_mdl_scnrcasem(case_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftm_ck06 CHECK(
        (src_spt_cd='CASE' AND org_case_id IS NOT NULL AND org_rvsn_val IS NOT NULL)
        OR (src_spt_cd='UPLOAD' AND org_case_id IS NULL AND org_rvsn_val IS NULL));

ALTER TABLE tmd_mdl_inptdrftprcsl
    ADD cust_cpny_no MEDIUMINT UNSIGNED NULL,
    ADD usr_no INT UNSIGNED NULL;
-- 3. BACKFILL — one bounded maintenance transaction; preserve all request/state/result/revision values.
START TRANSACTION;
UPDATE tmd_mdl_inptdrftprcsl p JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
SET p.cust_cpny_no=d.cust_cpny_no,p.usr_no=d.usr_no,p.mod_dtm=p.mod_dtm
WHERE p.cust_cpny_no IS NULL AND p.usr_no IS NULL;
COMMIT;
SELECT COUNT(*) AS unfilled_owner FROM tmd_mdl_inptdrftprcsl
WHERE cust_cpny_no IS NULL OR usr_no IS NULL;
-- Expected zero. Retain original mod_dtm; source classification defaults existing rows to CASE.

-- Drop/re-add the known inbound composite FKs while changing their referenced column's nullability.
ALTER TABLE tmd_mdl_inptdrftrvsnl DROP FOREIGN KEY tmd_mdl_inptdrftrvsnl_fk03;
ALTER TABLE tmd_mdl_inptdrftrowm DROP FOREIGN KEY tmd_mdl_inptdrftrowm_fk03;
ALTER TABLE tmd_mdl_inptdrftprcsl
    DROP FOREIGN KEY tmd_mdl_inptdrftprcsl_fk01,
    DROP CHECK tmd_mdl_inptdrftprcsl_ck01,
    DROP CHECK tmd_mdl_inptdrftprcsl_ck03,
    MODIFY drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    MODIFY base_rvsn_no BIGINT UNSIGNED NULL,
    MODIFY cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    MODIFY usr_no INT UNSIGNED NOT NULL,
    ADD crt_req_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
        (IF(prcs_type_cd='UPLOAD_CREATE',req_id,NULL)) STORED,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ux04 UNIQUE(cust_cpny_no,usr_no,crt_req_id),
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ux05 UNIQUE(cust_cpny_no,usr_no,prcs_id),
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_fk01 FOREIGN KEY(cust_cpny_no,usr_no,drft_id)
        REFERENCES tmd_mdl_inptdrftm(cust_cpny_no,usr_no,drft_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_fk02 FOREIGN KEY(cust_cpny_no)
        REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_fk03 FOREIGN KEY(usr_no)
        REFERENCES tmd_usrm(usr_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck01 CHECK(prcs_type_cd IN ('CHANGE','FINALIZE','UPLOAD_CREATE','UPLOAD_FILE')),
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck03 CHECK(
        json_rvsn_no=2 AND (base_rvsn_no IS NULL OR base_rvsn_no<=9007199254740991)),
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck10 CHECK(
        (prcs_type_cd='UPLOAD_CREATE' AND base_rvsn_no IS NULL
            AND ((prcs_stat_cd='SUCCEEDED' AND drft_id IS NOT NULL)
                OR (prcs_stat_cd<>'SUCCEEDED' AND drft_id IS NULL)))
        OR (prcs_type_cd<>'UPLOAD_CREATE' AND drft_id IS NOT NULL AND base_rvsn_no IS NOT NULL));
ALTER TABLE tmd_mdl_inptdrftrvsnl
    ADD CONSTRAINT tmd_mdl_inptdrftrvsnl_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE tmd_mdl_inptdrftrowm
    ADD CONSTRAINT tmd_mdl_inptdrftrowm_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- 2. APPLY B — independent private intake record. PREPARED is registered BEFORE object PUT.
CREATE TABLE tmd_mdl_inptupldfiled
(
    upld_file_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    usr_no INT UNSIGNED NOT NULL,
    strg_ref_json LONGTEXT NOT NULL,
    org_file_nm VARCHAR(300) NOT NULL,
    filex_nm VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NULL,
    mime_type_nm VARCHAR(100) NULL,
    file_size BIGINT UNSIGNED NULL,
    cntt_hash_val CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    prcs_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    mdl_file_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NULL,
    tmplt_id INT UNSIGNED NULL,
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    file_rvsn_no BIGINT UNSIGNED NULL,
    CONSTRAINT tmd_mdl_inptupldfiled_ux01 UNIQUE(prcs_id),
    CONSTRAINT tmd_mdl_inptupldfiled_ux02 UNIQUE(drft_id,upld_file_id),
    CONSTRAINT tmd_mdl_inptupldfiled_ux03 UNIQUE(drft_id,mdl_file_cd,file_rvsn_no),
    INDEX tmd_mdl_inptupldfiled_ix01(prcs_stat_cd,mod_dtm),
    CONSTRAINT tmd_mdl_inptupldfiled_fk01 FOREIGN KEY(cust_cpny_no,usr_no,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(cust_cpny_no,usr_no,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_fk02 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_fk03 FOREIGN KEY(drft_id,file_rvsn_no)
        REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_fk04 FOREIGN KEY(tmplt_id)
        REFERENCES tmd_mdl_inpttmpltm(tmplt_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_fk05 FOREIGN KEY(cust_cpny_no)
        REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_fk06 FOREIGN KEY(usr_no)
        REFERENCES tmd_usrm(usr_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldfiled_ck01 CHECK(prcs_stat_cd IN ('PREPARED','RECEIVED','COMMITTED','CLEANUP_PENDING','CLEANED')),
    CONSTRAINT tmd_mdl_inptupldfiled_ck02 CHECK(JSON_VALID(strg_ref_json) AND OCTET_LENGTH(strg_ref_json)<=16384),
    CONSTRAINT tmd_mdl_inptupldfiled_ck03 CHECK(file_size IS NULL OR file_size BETWEEN 1 AND 52428800),
    CONSTRAINT tmd_mdl_inptupldfiled_ck04 CHECK(mdl_file_cd IS NULL OR mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT')),
    CONSTRAINT tmd_mdl_inptupldfiled_ck05 CHECK(
        (prcs_stat_cd='COMMITTED' AND prcs_id IS NOT NULL AND drft_id IS NOT NULL
            AND file_rvsn_no IS NOT NULL AND file_rvsn_no<=9007199254740991
            AND mdl_file_cd IS NOT NULL AND tmplt_id IS NOT NULL
            AND filex_nm IS NOT NULL AND filex_nm IN ('xlsx','xlsm') AND mime_type_nm IS NOT NULL)
        OR (prcs_stat_cd<>'COMMITTED' AND drft_id IS NULL AND file_rvsn_no IS NULL)),
    CONSTRAINT tmd_mdl_inptupldfiled_ck06 CHECK(
        prcs_stat_cd NOT IN ('RECEIVED','COMMITTED')
        OR (file_size IS NOT NULL AND cntt_hash_val IS NOT NULL AND CHAR_LENGTH(cntt_hash_val)=64)),
    CONSTRAINT tmd_mdl_inptupldfiled_ck07 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tmd_mdl_inptupldactvm
(
    cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    usr_no INT UNSIGNED NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    PRIMARY KEY(cust_cpny_no,usr_no),
    CONSTRAINT tmd_mdl_inptupldactvm_fk01 FOREIGN KEY(cust_cpny_no,usr_no,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(cust_cpny_no,usr_no,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldactvm_fk02 FOREIGN KEY(cust_cpny_no,usr_no,drft_id)
        REFERENCES tmd_mdl_inptdrftm(cust_cpny_no,usr_no,drft_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldactvm_fk03 FOREIGN KEY(cust_cpny_no)
        REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldactvm_fk04 FOREIGN KEY(usr_no)
        REFERENCES tmd_usrm(usr_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptupldactvm_ck01 CHECK(prcs_id IS NULL OR drft_id IS NULL),
    CONSTRAINT tmd_mdl_inptupldactvm_ck02 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. APPLY C — current file pointer and original-row generations.
ALTER TABLE tmd_mdl_inptdrftfiled
    DROP FOREIGN KEY tmm_mdlinptdrftfiled_fk02,
    MODIFY file_no BIGINT UNSIGNED NULL,
    ADD upld_file_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD CONSTRAINT tmm_mdlinptdrftfiled_fk02 FOREIGN KEY(file_no)
        REFERENCES tcm_filel(file_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftfiled_fk03 FOREIGN KEY(drft_id,upld_file_id)
        REFERENCES tmd_mdl_inptupldfiled(drft_id,upld_file_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftfiled_ck03 CHECK(
        (file_no IS NOT NULL AND upld_file_id IS NULL) OR (file_no IS NULL AND upld_file_id IS NOT NULL));
ALTER TABLE tmd_mdl_inptdrftrowm
    ADD file_rvsn_no BIGINT UNSIGNED NOT NULL DEFAULT 0,
    DROP INDEX tmm_mdlinptdrftrowm_ux01,
    ADD CONSTRAINT tmm_mdlinptdrftrowm_ux01 UNIQUE(drft_id,inpt_tbl_id,file_rvsn_no,org_row_no),
    ADD CONSTRAINT tmd_mdl_inptdrftrowm_fk04 FOREIGN KEY(drft_id,file_rvsn_no)
        REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT tmd_mdl_inptdrftrowm_ck03 CHECK(file_rvsn_no<=9007199254740991);
-- Existing rows keep IDs and baseline0. New tables start empty; no catalog/Case/file registration backfill.

-- 4. VERIFY — expected counters0; keep schema/index output, not private strg_ref_json/req_json contents.
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
SHOW CREATE TABLE tmd_mdl_inptupldactvm;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptdrftfiled;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SELECT table_name,index_name,non_unique,seq_in_index,column_name FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftprcsl','tmd_mdl_inptupldfiled','tmd_mdl_inptupldactvm','tmd_mdl_inptdrftrowm')
ORDER BY table_name,index_name,seq_in_index;

SELECT COUNT(*) AS invalid_slot FROM tmd_mdl_inptupldactvm s
LEFT JOIN tmd_mdl_inptdrftm d ON d.drft_id=s.drft_id
LEFT JOIN tmd_mdl_inptdrftprcsl p ON p.prcs_id=s.prcs_id
WHERE (s.drft_id IS NOT NULL AND (d.drft_id IS NULL OR d.src_spt_cd<>'UPLOAD' OR d.drft_stat_cd<>'DRAFT' OR d.del_yn<>'N'))
   OR (s.prcs_id IS NOT NULL AND (p.prcs_id IS NULL OR p.prcs_type_cd<>'UPLOAD_CREATE' OR p.prcs_stat_cd NOT IN ('QUEUED','RUNNING')));
SELECT COUNT(*) AS missing_draft_slot FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptupldactvm s ON s.cust_cpny_no=d.cust_cpny_no AND s.usr_no=d.usr_no AND s.drft_id=d.drft_id
WHERE d.src_spt_cd='UPLOAD' AND d.drft_stat_cd='DRAFT' AND d.del_yn='N' AND s.drft_id IS NULL;
SELECT COUNT(*) AS missing_initial_slot FROM tmd_mdl_inptdrftprcsl p
LEFT JOIN tmd_mdl_inptupldactvm s ON s.cust_cpny_no=p.cust_cpny_no AND s.usr_no=p.usr_no AND s.prcs_id=p.prcs_id
WHERE p.prcs_type_cd='UPLOAD_CREATE' AND p.prcs_stat_cd IN ('QUEUED','RUNNING') AND s.prcs_id IS NULL;
SELECT COUNT(*) AS invalid_current_file FROM tmd_mdl_inptdrftfiled f
JOIN tmd_mdl_inptdrftm d ON d.drft_id=f.drft_id
LEFT JOIN tmd_mdl_inptupldfiled u ON u.upld_file_id=f.upld_file_id
WHERE (d.src_spt_cd='CASE' AND f.file_no IS NULL)
   OR (d.src_spt_cd='UPLOAD' AND (f.upld_file_id IS NULL OR u.upld_file_id IS NULL
       OR u.prcs_stat_cd<>'COMMITTED' OR NOT(u.drft_id<=>d.drft_id)
       OR NOT(u.tmplt_id<=>d.tmplt_id) OR NOT(u.mdl_file_cd<=>f.mdl_file_cd) OR u.file_rvsn_no>d.rvsn_no
       OR EXISTS (SELECT 1 FROM tmd_mdl_inptupldfiled newer
           WHERE newer.drft_id=d.drft_id AND newer.mdl_file_cd=f.mdl_file_cd AND newer.prcs_stat_cd='COMMITTED'
             AND newer.file_rvsn_no<=d.rvsn_no AND newer.file_rvsn_no>u.file_rvsn_no)));
SELECT COUNT(*) AS invalid_committed_upload FROM tmd_mdl_inptupldfiled u
LEFT JOIN tmd_mdl_inptdrftprcsl p ON p.prcs_id=u.prcs_id
LEFT JOIN tmd_mdl_inptdrftm d ON d.drft_id=u.drft_id
LEFT JOIN tmd_mdl_inptdrftrvsnl r ON r.drft_id=u.drft_id AND r.rvsn_no=u.file_rvsn_no
WHERE u.prcs_stat_cd='COMMITTED' AND (p.prcs_id IS NULL OR p.prcs_stat_cd<>'SUCCEEDED'
 OR p.prcs_type_cd NOT IN ('UPLOAD_CREATE','UPLOAD_FILE') OR d.src_spt_cd<>'UPLOAD'
 OR NOT(u.tmplt_id<=>d.tmplt_id) OR r.drft_id IS NULL
 OR (p.prcs_type_cd='UPLOAD_CREATE' AND u.file_rvsn_no<>0)
 OR (p.prcs_type_cd='UPLOAD_FILE' AND (u.file_rvsn_no<>p.base_rvsn_no+1 OR NOT(r.prcs_id<=>p.prcs_id))));
SELECT COUNT(*) AS invalid_revision_request FROM tmd_mdl_inptdrftrvsnl r
JOIN tmd_mdl_inptdrftprcsl p ON p.prcs_id=r.prcs_id AND p.drft_id=r.drft_id
WHERE r.rvsn_no>0 AND (p.prcs_stat_cd<>'SUCCEEDED' OR p.prcs_type_cd NOT IN ('CHANGE','UPLOAD_FILE')
 OR NOT(p.base_rvsn_no<=>r.base_rvsn_no));
SELECT COUNT(*) AS invalid_upload_candidate FROM tmd_mdl_inptdrftprcsl p
LEFT JOIN tmd_mdl_inptupldfiled u ON u.prcs_id=p.prcs_id
WHERE p.prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE') AND (u.upld_file_id IS NULL
 OR (p.prcs_stat_cd='SUCCEEDED' AND u.prcs_stat_cd<>'COMMITTED')
 OR (p.prcs_stat_cd='FAILED' AND u.prcs_stat_cd='COMMITTED'));
SELECT COUNT(*) AS invalid_active_row_baseline FROM tmd_mdl_inptdrftrowm m
JOIN tmd_mdl_inptdrftrowh h ON h.row_id=m.row_id AND h.end_rvsn_no IS NULL AND h.row_no IS NOT NULL
JOIN tmd_mdl_inptdrftm d ON d.drft_id=m.drft_id
JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id=m.inpt_tbl_id
JOIN tmd_mdl_inptshtd sh ON sh.inpt_sht_id=t.inpt_sht_id
JOIN tmd_mdl_inptfiled tf ON tf.inpt_file_id=sh.inpt_file_id
LEFT JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=m.drft_id AND f.mdl_file_cd=tf.mdl_file_cd AND f.del_yn='N'
LEFT JOIN tmd_mdl_inptupldfiled u ON u.upld_file_id=f.upld_file_id
WHERE tf.tmplt_id<>d.tmplt_id OR (d.src_spt_cd='CASE' AND m.file_rvsn_no<>0)
 OR (d.src_spt_cd='UPLOAD' AND (u.upld_file_id IS NULL OR NOT(m.file_rvsn_no<=>u.file_rvsn_no)));
SELECT COUNT(*) AS invalid_active_pointer FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptdrftprcsl p ON p.drft_id=d.drft_id AND p.prcs_id=d.actv_prcs_id
WHERE d.actv_prcs_id IS NOT NULL AND (p.prcs_id IS NULL OR p.prcs_stat_cd NOT IN ('QUEUED','RUNNING'));
SELECT COUNT(*) AS missing_active_pointer FROM tmd_mdl_inptdrftprcsl p
JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id
WHERE p.prcs_stat_cd IN ('QUEUED','RUNNING') AND NOT(d.actv_prcs_id<=>p.prcs_id);
SELECT d.drft_id FROM tmd_mdl_inptdrftm d LEFT JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=d.drft_id AND f.del_yn='N'
GROUP BY d.drft_id,d.src_spt_cd,d.drft_stat_cd
HAVING (d.src_spt_cd='CASE' AND COUNT(f.mdl_file_cd)<>4)
 OR (d.src_spt_cd='UPLOAD' AND (COUNT(f.mdl_file_cd)<1 OR COUNT(f.mdl_file_cd)>4))
 OR (d.drft_stat_cd='FINALIZED' AND COUNT(f.mdl_file_cd)<>4);
-- Expected zero rows. Also compare original existing table counts/PKs and all CASE state/results.
-- Run EXPLAIN for owner-slot PK, current role PK, request replay UK and baseline lookup in fixture DB.
-- Actual two-session races/replay/replace rollback/timeout/late worker and role-masked replay are required tests.
-- All UPLOAD-origin mutations (including CHANGE/FINALIZE/claim/complete/recovery/cancel) lock:
-- owner-slot -> existing Draft (if any) -> operation. Never acquire the slot after locking Draft/operation.
-- Recheck requestId under that lock BEFORE busy/revision rejection; resolve duplicate UK races to the original result.

-- 5. ROLLBACK / DEPLOYMENT
-- Disable new upload admissions; stop/drain the common workers and confirm actual write-DB commits.
-- Preserve every successful file/baseline/request/revision/Case and all new tables/columns.
-- No reverse NOT NULL ALTER, fake Case assignment, DROP TABLE, DELETE or TRUNCATE rollback.
-- Deploy only a compatible reader/worker; old code interprets NULL caseId as0 and cannot consume upload requests.
-- If migration stops midway, keep writes stopped, compare each SHOW CREATE and complete only missing steps.
-- Existing column-v2 reset is NOT part of this migration and must not be replayed afterward.
-- Retention is the existing no-automatic-TTL policy. Cleanup removes only proven uncommitted private candidates.
-- Before apply: actual MySQL/FK/CHECK/names/packet/backups verified; after apply: constraints/counters/PKs preserved.
-- Feature enablement waits for API/worker/file-format and fixture concurrency checks; this draft is not approval to deploy.
