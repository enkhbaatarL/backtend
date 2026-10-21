-- Document Control
-- status: Draft / MANUAL REVIEW AND APPLY ONLY; NOT EXECUTED
-- canonical for: Schema rollout — shared baseline tables/columns/keys/indexes/nullability,
--                incremental precheck/apply/backfill/verify/preserving rollback SQL
-- owner: BE / DBA; date: 2026-10-21; last synced: 2026-10-21
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md section 6.12
--             docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md section 5.7
--             docs/model-input-draft-read-ddl-draft.sql
--             docs/model-input-incremental-upload-ddl-draft.sql (and multi-upload extension)
--             docs/model-input-audit-precision-ddl-draft.sql; existing request/finalization/column-v2 DDL
-- supersedes: none; additive extension only. Existing CREATE/reset/backfill packages MUST NOT be replayed.
-- Open Questions: actual master MySQL definitions/constraints/names, capacity/metadata locks,
--                 manual application results and application/worker compatibility evidence are unverified.
-- Codex has not connected to or run SQL against a real DB. No automatic migration is configured here.
-- Business lifecycle/state/ownership rules remain in the linked domain design; API contracts remain in specs.
-- All objects belong to master dataSource, not a tenant schema. No file content hash/deduplication is added.
-- MDL/INPT/BASE/FILE/ROW/ORG/RVSN/OBJ/REF/CNT use existing dictionary tokens; no dictionary additions.

-- Manual deployment order / go-no-go:
-- 1) Keep mi.model-input.shared-baseline.enabled=false (default); this controls NEW Case creation only.
--    Stop affected web/runner/worker writers and drain work normally.
--    Capture backup/restore evidence, exact SHOW CREATE, PK-keyed existing data and counts. Confirm
--    execution/lease termination through existing operational evidence; zero counts alone do not prove it.
-- 2) Run PRECHECK against the approved master connection; reconcile every difference before APPLY.
-- 3) Apply only absent objects/steps, in listed order, with FK/CHECK enabled. MySQL DDL implicitly commits.
--    Stop on any error; inspect each completed statement before retry. Do not blindly rerun this file.
-- 4) No BACKFILL. Verify existing rows unchanged and base_id NULL; deploy compatibility code with creation OFF.
-- 5) All web/runner/separate worker consumers must support both formats for reads/CHANGE/FINALIZE/recovery.
--    Complete BE/DBA checks below before separately approved limited activation. DDL alone is not activation.
-- 6) Stop new shared creation on ownership/source/row/history/partial-commit/file-output mismatch or limits.
--    Preserve successful data; use the compatible-app ROLLBACK section rather than clearing base_id.

-- 1. PRECHECK — read only; save output without private object paths or JSON payloads.
SELECT DATABASE() AS target_master_schema, VERSION() AS mysql_version,
       @@session.sql_mode, @@transaction_isolation, @@max_allowed_packet,
       @@session.time_zone, @@system_time_zone;
-- DBA confirms approved master schema, InnoDB and enforced CHECK support (MySQL 8.0.16+),
-- SQL privileges, parent-index byte limits, metadata-lock window and disk/transaction/packet headroom.
-- Do not assume online/instant ALTER, set a different global mode, or disable FOREIGN_KEY_CHECKS.
SHOW CREATE TABLE tmd_custcpnym;
SHOW CREATE TABLE tmd_mdl_scnrcasem;
SHOW CREATE TABLE tcm_filel;
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_scnrcasetmpltd;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftfiled;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SHOW CREATE TABLE tmd_mdl_inptdrftrowh;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
SHOW CREATE TABLE tmd_mdl_inptupldactvm;

SELECT table_name,engine,table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_custcpnym','tmd_mdl_scnrcasem','tcm_filel','tmd_mdl_inpttmpltm','tmd_mdl_inptfiled',
  'tmd_mdl_inptshtd','tmd_mdl_inpttbld','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
  'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh',
  'tmd_mdl_inptbasem','tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd');
SELECT table_name,column_name,column_type,is_nullable,column_default,extra,collation_name,datetime_precision
FROM information_schema.columns WHERE table_schema=DATABASE()
AND table_name IN ('tmd_custcpnym','tmd_mdl_scnrcasem','tcm_filel','tmd_mdl_inpttmpltm',
 'tmd_mdl_inpttbld','tmd_mdl_inptdrftm','tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptbasem','tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd')
ORDER BY table_name,ordinal_position;
-- Expected parent/child types: company MEDIUMINT UNSIGNED; Case/template/table INT UNSIGNED;
-- file BIGINT UNSIGNED; UUID CHAR(36) ascii_bin; org_rvsn_val VARCHAR(68) ascii_bin.
-- Existing Draft src_spt_cd is VARCHAR(20) ascii_bin, CASE fields required by existing ck06,
-- UPLOAD Case/source fields nullable; rowm.file_rvsn_no exists. Existing audit DATETIME precision=0.
-- Initially no new tables/base_id/reserved names. If present, compare exact definition and skip
-- matching steps; incompatible or partially applied definitions are STOP, never CREATE IF NOT EXISTS.
SELECT table_name,constraint_name,constraint_type,enforced FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND (table_name IN
 ('tmd_mdl_inptbasem','tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd')
 OR constraint_name IN ('tmd_mdl_inptdrftm_fk10','tmd_mdl_inptdrftm_ck10'));
SELECT table_name,index_name,non_unique,seq_in_index,column_name,sub_part
FROM information_schema.statistics WHERE table_schema=DATABASE()
AND table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptbasem','tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd')
ORDER BY table_name,index_name,seq_in_index;
-- Reserved ix10/fk10/ck10 collision: stop and correct canonical design before changing names.
-- Preserve all existing tmm_mdlinpt... and tmd_mdl_inpt... constraints, including generated rowh UKs.
SELECT trigger_name,event_object_table,action_timing,event_manipulation
FROM information_schema.triggers WHERE trigger_schema=DATABASE()
AND event_object_table IN ('tmd_mdl_inptdrftm','tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd');
SELECT table_name,constraint_name,referenced_table_name,column_name,referenced_column_name
FROM information_schema.key_column_usage WHERE constraint_schema=DATABASE()
AND referenced_table_name IN ('tmd_mdl_inptdrftm','tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled','tmd_mdl_inptbaserowd') ORDER BY table_name,constraint_name,ordinal_position;
SELECT COUNT(*) AS pending_operations FROM tmd_mdl_inptdrftprcsl
WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
SELECT COUNT(*) AS active_pointers FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL;
-- Expected both0 AFTER normal drain, plus operational termination evidence. Never force-fail/delete work.
-- Export BEFORE/AFTER PK-keyed snapshots of existing Case/file/catalog and all Draft tables.
-- Compare identities/revisions/operations/files/row history/payloads, not just counts. Do not log object paths.

-- 2. APPLY — manual additive DDL only. New tables start EMPTY; no header/manifest/row seed.
CREATE TABLE tmd_mdl_inptbasem
(
    base_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    org_case_id INT UNSIGNED NOT NULL,
    tmplt_id INT UNSIGNED NOT NULL,
    org_rvsn_val VARCHAR(68) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    base_ver_no SMALLINT UNSIGNED NOT NULL,
    row_cnt BIGINT UNSIGNED NOT NULL,
    CONSTRAINT tmd_mdl_inptbasem_ux01 UNIQUE(cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val,base_ver_no),
    CONSTRAINT tmd_mdl_inptbasem_ux02 UNIQUE(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val),
    INDEX tmd_mdl_inptbasem_ix01(org_case_id),
    INDEX tmd_mdl_inptbasem_ix02(tmplt_id),
    CONSTRAINT tmd_mdl_inptbasem_fk01 FOREIGN KEY(cust_cpny_no)
        REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbasem_fk02 FOREIGN KEY(org_case_id)
        REFERENCES tmd_mdl_scnrcasem(case_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbasem_fk03 FOREIGN KEY(tmplt_id)
        REFERENCES tmd_mdl_inpttmpltm(tmplt_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbasem_ck01 CHECK(base_ver_no>=1),
    CONSTRAINT tmd_mdl_inptbasem_ck02 CHECK(REGEXP_LIKE(org_rvsn_val,'^sv1_[0-9a-f]{64}$','c')),
    CONSTRAINT tmd_mdl_inptbasem_ck03 CHECK(row_cnt>=0),
    CONSTRAINT tmd_mdl_inptbasem_ck04 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tmd_mdl_inptbasefiled
(
    base_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    mdl_file_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    file_no BIGINT UNSIGNED NOT NULL,
    obj_ref_val LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY(base_id,mdl_file_cd),
    CONSTRAINT tmd_mdl_inptbasefiled_ux01 UNIQUE(base_id,file_no),
    INDEX tmd_mdl_inptbasefiled_ix01(file_no),
    CONSTRAINT tmd_mdl_inptbasefiled_fk01 FOREIGN KEY(base_id)
        REFERENCES tmd_mdl_inptbasem(base_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbasefiled_fk02 FOREIGN KEY(file_no)
        REFERENCES tcm_filel(file_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbasefiled_ck01 CHECK(mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT')),
    CONSTRAINT tmd_mdl_inptbasefiled_ck02 CHECK(
        REGEXP_LIKE(obj_ref_val,'[^[:space:]]','c') AND OCTET_LENGTH(obj_ref_val)<=16384),
    CONSTRAINT tmd_mdl_inptbasefiled_ck03 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tmd_mdl_inptbaserowd
(
    base_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    inpt_tbl_id INT UNSIGNED NOT NULL,
    org_row_no INT UNSIGNED NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY(base_id,inpt_tbl_id,org_row_no),
    INDEX tmd_mdl_inptbaserowd_ix01(inpt_tbl_id),
    CONSTRAINT tmd_mdl_inptbaserowd_fk01 FOREIGN KEY(base_id)
        REFERENCES tmd_mdl_inptbasem(base_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbaserowd_fk02 FOREIGN KEY(inpt_tbl_id)
        REFERENCES tmd_mdl_inpttbld(inpt_tbl_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptbaserowd_ck01 CHECK(org_row_no BETWEEN 1 AND 1048576),
    CONSTRAINT tmd_mdl_inptbaserowd_ck02 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE tmd_mdl_inptdrftm
    ADD COLUMN base_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
    ADD INDEX tmd_mdl_inptdrftm_ix10(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val),
    ADD CONSTRAINT tmd_mdl_inptdrftm_ck10 CHECK(base_id IS NULL OR src_spt_cd='CASE'),
    ADD CONSTRAINT tmd_mdl_inptdrftm_fk10
        FOREIGN KEY(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)
        REFERENCES tmd_mdl_inptbasem(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
-- Nullable composite FK deliberately skips legacy/UPLOAD rows. Existing source-required ck06 remains.
-- Application enforces immutable base reference and one-transaction completed baseline creation;
-- no new user/status/current-revision/cache/row-baseline columns, triggers or reset procedure.

-- 3. BACKFILL — NOT NEEDED / no SQL.
-- Existing Drafts retain NULL base_id, stored rowIds and all rowm/rowh/file/revision/operation data.
-- Never infer shared membership from an old edited Draft or recalculate its original rows/IDs.
-- First new shared baseline is created by normal application scan of fixed immutable existing files.

-- 4. VERIFY — read only, on a quiescent/snapshot-consistent view. Expected anomaly queries0 rows/counts.
-- Immediately after DDL: all three new tables EMPTY, every existing Draft base_id NULL.
-- After separately approved activation: baseline count may grow; verify all baselines and both formats.
SHOW CREATE TABLE tmd_mdl_inptbasem;
SHOW CREATE TABLE tmd_mdl_inptbasefiled;
SHOW CREATE TABLE tmd_mdl_inptbaserowd;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
-- Rerun PRECHECK metadata queries: verify exact types/column order/audit defaults/index order,
-- all CHECKs ENFORCED and every FK RESTRICT; existing keys/generated UKs and data unchanged.
SELECT COUNT(*) AS baselines FROM tmd_mdl_inptbasem;
SELECT COUNT(*) AS baseline_files FROM tmd_mdl_inptbasefiled;
SELECT COUNT(*) AS baseline_rows FROM tmd_mdl_inptbaserowd;
SELECT COUNT(*) AS shared_drafts FROM tmd_mdl_inptdrftm WHERE base_id IS NOT NULL;
SELECT cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val,base_ver_no,COUNT(*) AS duplicates
FROM tmd_mdl_inptbasem GROUP BY cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val,base_ver_no
HAVING COUNT(*)<>1;
SELECT b.base_id FROM tmd_mdl_inptbasem b LEFT JOIN tmd_mdl_inptbasefiled f ON f.base_id=b.base_id
GROUP BY b.base_id HAVING COUNT(f.mdl_file_cd)<>4 OR COUNT(DISTINCT f.mdl_file_cd)<>4
 OR COUNT(DISTINCT f.file_no)<>4;
SELECT b.base_id,b.row_cnt,COUNT(r.org_row_no) AS actual_rows
FROM tmd_mdl_inptbasem b LEFT JOIN tmd_mdl_inptbaserowd r ON r.base_id=b.base_id
GROUP BY b.base_id,b.row_cnt HAVING b.row_cnt<>COUNT(r.org_row_no);
SELECT COUNT(*) AS invalid_base_header FROM tmd_mdl_inptbasem
WHERE del_yn<>'N' OR base_ver_no<>1 OR NOT REGEXP_LIKE(org_rvsn_val,'^sv1_[0-9a-f]{64}$','c');
-- Future versions require their matching reader; unsupported version is a stop, never guessed rows.
SELECT COUNT(*) AS invalid_base_manifest FROM tmd_mdl_inptbasefiled f
LEFT JOIN tmd_mdl_inptbasem b ON b.base_id=f.base_id
LEFT JOIN tcm_filel s ON s.file_no=f.file_no
WHERE b.base_id IS NULL OR f.del_yn<>'N' OR s.file_no IS NULL
 OR f.mdl_file_cd NOT IN ('DEMAND','SUPPLY','TRADE','OUTPUT')
 OR NOT REGEXP_LIKE(f.obj_ref_val,'[^[:space:]]','c') OR OCTET_LENGTH(f.obj_ref_val)>16384
 OR NOT(CAST(f.obj_ref_val AS BINARY)<=>CAST(s.strg_path_nm AS BINARY))
 OR NOT(s.file_rel_obj_spt_cd<=>'SCNRCASE') OR NOT(s.file_rel_obj_dstg_no<=>b.org_case_id);
-- Exact location is FileRef.location from existing tcm_filel.strg_path_nm, with no URI normalization.
-- Availability changes (soft delete/expiry/current publication or Case-file replacement) are checked
-- by source() in BE as access/source conflicts; do not label every historical baseline as corrupt.
SELECT COUNT(*) AS invalid_base_row_table FROM tmd_mdl_inptbaserowd r
LEFT JOIN tmd_mdl_inptbasem b ON b.base_id=r.base_id
LEFT JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id=r.inpt_tbl_id
LEFT JOIN tmd_mdl_inptshtd s ON s.inpt_sht_id=t.inpt_sht_id
LEFT JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id
WHERE b.base_id IS NULL OR t.inpt_tbl_id IS NULL OR s.inpt_sht_id IS NULL OR f.inpt_file_id IS NULL
 OR f.tmplt_id<>b.tmplt_id OR r.del_yn<>'N' OR r.org_row_no NOT BETWEEN 1 AND 1048576;
-- Mandatory BE verification (SQL totals alone are insufficient): parse the same immutable qry_json
-- through InputReadDefinition, require every row inside bodyRanges and compare the COMPLETE scanned
-- logical row set including identityColumns/Regas exceptions; check deleted catalog children and
-- current source/catalog access separately. A missing/invalid definition is a failure, not a skip.
SELECT COUNT(*) AS invalid_shared_draft_parent FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptbasem b ON b.base_id=d.base_id
WHERE d.base_id IS NOT NULL AND (d.src_spt_cd<>'CASE' OR b.base_id IS NULL
 OR NOT(d.cust_cpny_no<=>b.cust_cpny_no) OR NOT(d.org_case_id<=>b.org_case_id)
 OR NOT(d.tmplt_id<=>b.tmplt_id) OR NOT(d.org_rvsn_val<=>b.org_rvsn_val));
SELECT COUNT(*) AS invalid_shared_file FROM tmd_mdl_inptdrftm d
JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=d.drft_id
LEFT JOIN tmd_mdl_inptbasefiled b ON b.base_id=d.base_id
 AND CAST(b.mdl_file_cd AS BINARY)=CAST(f.mdl_file_cd AS BINARY)
WHERE d.base_id IS NOT NULL AND (b.base_id IS NULL OR NOT(f.file_no<=>b.file_no)
 OR f.upld_file_id IS NOT NULL OR f.del_yn<>'N');
SELECT d.drft_id FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=d.drft_id AND f.del_yn='N'
WHERE d.base_id IS NOT NULL GROUP BY d.drft_id HAVING COUNT(f.mdl_file_cd)<>4;
SELECT COUNT(*) AS invalid_shared_physical_row FROM tmd_mdl_inptdrftrowm m
JOIN tmd_mdl_inptdrftm d ON d.drft_id=m.drft_id
LEFT JOIN tmd_mdl_inptbaserowd b ON b.base_id=d.base_id
 AND b.inpt_tbl_id=m.inpt_tbl_id AND b.org_row_no=m.org_row_no
WHERE d.base_id IS NOT NULL AND (m.del_yn<>'N' OR m.file_rvsn_no<>0
 OR (m.org_row_no IS NOT NULL AND (b.base_id IS NULL OR m.prcs_id IS NOT NULL OR m.clnt_row_id IS NOT NULL)));

-- Check each materialized original's ID independently using the specified UUID v5 bytes/name.
-- SHA1 here derives row identity only; it is NOT a new file hash or an authorization token.
WITH original_hash AS (
 SELECT m.row_id,LOWER(SHA1(CONCAT(UNHEX(REPLACE(d.drft_id,'-','')),
  CAST(CONCAT('mi-input-row/v1/',d.base_id,'/',m.inpt_tbl_id,'/',m.org_row_no) AS BINARY)))) AS digest
 FROM tmd_mdl_inptdrftm d JOIN tmd_mdl_inptdrftrowm m ON m.drft_id=d.drft_id
 WHERE d.base_id IS NOT NULL AND m.org_row_no IS NOT NULL
), expected_id AS (
 SELECT row_id,CONCAT(SUBSTRING(digest,1,8),'-',SUBSTRING(digest,9,4),'-5',SUBSTRING(digest,14,3),'-',
  ELT(1+MOD(CONV(SUBSTRING(digest,17,1),16,10),4),'8','9','a','b'),SUBSTRING(digest,18,3),'-',
  SUBSTRING(digest,21,12)) AS expected_row_id FROM original_hash
)
SELECT COUNT(*) AS invalid_original_uuid FROM expected_id
WHERE CAST(row_id AS BINARY)<>CAST(expected_row_id AS BINARY);

-- A materialized original has a continuous history from0, including [0,first-change).
-- All shared physical rows have exactly one current interval; no soft-deleted/orphan interval.
SELECT m.row_id FROM tmd_mdl_inptdrftm d JOIN tmd_mdl_inptdrftrowm m ON m.drft_id=d.drft_id
LEFT JOIN tmd_mdl_inptdrftrowh h ON h.row_id=m.row_id
WHERE d.base_id IS NOT NULL GROUP BY m.row_id,m.org_row_no
HAVING COUNT(h.row_id)=0 OR SUM(h.end_rvsn_no IS NULL)<>1
 OR (m.org_row_no IS NOT NULL AND MIN(h.bgn_rvsn_no)<>0);
WITH intervals AS (
 SELECT h.*,d.rvsn_no,
  LEAD(h.bgn_rvsn_no) OVER(PARTITION BY h.row_id ORDER BY h.bgn_rvsn_no) AS next_begin
 FROM tmd_mdl_inptdrftrowh h JOIN tmd_mdl_inptdrftm d ON d.drft_id=h.drft_id
 WHERE d.base_id IS NOT NULL
)
SELECT COUNT(*) AS invalid_shared_intervals FROM intervals h
LEFT JOIN tmd_mdl_inptdrftrowm m ON m.row_id=h.row_id
WHERE m.row_id IS NULL OR h.del_yn<>'N' OR m.drft_id<>h.drft_id OR m.inpt_tbl_id<>h.inpt_tbl_id
 OR h.bgn_rvsn_no>h.rvsn_no OR h.end_rvsn_no>h.rvsn_no
 OR (h.end_rvsn_no IS NOT NULL AND h.end_rvsn_no<=h.bgn_rvsn_no)
 OR NOT(h.end_rvsn_no<=>h.next_begin);

-- Stored rowh UKs cannot see virtual originals: include both to detect logical current collisions.
WITH logical_current AS (
 SELECT d.drft_id,b.inpt_tbl_id,b.org_row_no AS row_no
 FROM tmd_mdl_inptdrftm d JOIN tmd_mdl_inptbaserowd b ON b.base_id=d.base_id
 WHERE NOT EXISTS (SELECT 1 FROM tmd_mdl_inptdrftrowm m WHERE m.drft_id=d.drft_id
  AND m.inpt_tbl_id=b.inpt_tbl_id AND m.file_rvsn_no=0 AND m.org_row_no=b.org_row_no)
 UNION ALL
 SELECT h.drft_id,h.inpt_tbl_id,h.row_no FROM tmd_mdl_inptdrftrowh h
 JOIN tmd_mdl_inptdrftm d ON d.drft_id=h.drft_id
 WHERE d.base_id IS NOT NULL AND h.end_rvsn_no IS NULL AND h.row_no IS NOT NULL
)
SELECT drft_id,inpt_tbl_id,row_no,COUNT(*) AS logical_duplicates FROM logical_current
GROUP BY drft_id,inpt_tbl_id,row_no HAVING COUNT(*)>1;

SELECT COUNT(*) AS missing_shared_revision0 FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptdrftrvsnl r ON r.drft_id=d.drft_id AND r.rvsn_no=0
WHERE d.base_id IS NOT NULL AND (r.drft_id IS NULL OR r.del_yn<>'N' OR r.json_rvsn_no<>2
 OR r.base_rvsn_no IS NOT NULL OR r.prcs_id IS NOT NULL OR r.dlta_json IS NOT NULL);
SELECT d.drft_id FROM tmd_mdl_inptdrftm d WHERE d.base_id IS NOT NULL AND d.rvsn_no=0
 AND (EXISTS(SELECT 1 FROM tmd_mdl_inptdrftrowm m WHERE m.drft_id=d.drft_id)
   OR EXISTS(SELECT 1 FROM tmd_mdl_inptdrftrowh h WHERE h.drft_id=d.drft_id));
-- Mandatory BE verification: replay successful revision0..n/operation records with InputDraftHistory,
-- compare ALL logical identities/intervals both ways at current and historical revisions, prove each
-- added row belongs to its successful ADD operation/clientRowKey and no UUID collision between the
-- virtual and added sets. Validate first materialization/late-token rollback, moves/deletes/swaps,
-- repeated same-position writes, UPDATE/FINALIZE/worker restart and byte-preserving output behavior.
-- Run legacy/UPLOAD checks below independently; base_id NULL still uses stored full rows.
SELECT COUNT(*) AS invalid_upload_base FROM tmd_mdl_inptdrftm
WHERE src_spt_cd='UPLOAD' AND base_id IS NOT NULL;
SELECT d.drft_id FROM tmd_mdl_inptdrftm d
LEFT JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=d.drft_id AND f.del_yn='N'
WHERE d.base_id IS NULL GROUP BY d.drft_id,d.src_spt_cd,d.drft_stat_cd
HAVING (d.src_spt_cd='CASE' AND COUNT(f.mdl_file_cd)<>4)
 OR (d.src_spt_cd='UPLOAD' AND COUNT(f.mdl_file_cd) NOT BETWEEN 1 AND 4)
 OR (d.drft_stat_cd='FINALIZED' AND COUNT(f.mdl_file_cd)<>4);
-- Rerun incremental-upload VERIFY (multi-upload-aware candidate check), request/finalization and v2
-- history checks. Existing all-original-row checks apply ONLY to base_id IS NULL; do not demand
-- N rowm + N rowh for shared Drafts. Preserve legacy/UPLOAD row/file baseline verification.
-- Recompare captured PK snapshots after DDL and prove no legacy Draft was automatically converted.
-- For a controlled, unedited second shared Draft: exactly one reused complete baseline, revision0,
-- four fixed file refs, rowm=0 and rowh=0; first/reuse timings and scan/INSERT counts are measured.
-- Measure actual two-session UK/FK/lock/batch rollback, FINALIZE/resume concurrency, memory and lock
-- wait. A transaction deadlock must roll back fully and preserve prior successful history. Unit/H2
-- checks and the illustrative226,102 rows are not actual MySQL/performance evidence.

-- EXPLAIN templates — DBA fills approved fixture IDs; these SELECTs intentionally contain placeholders
-- and are COMMENTS, not statements to run blindly. Retain plans with actual baseline/page/override data.
-- EXPLAIN SELECT base_id FROM tmd_mdl_inptbasem WHERE cust_cpny_no=<company> AND org_case_id=<case>
--  AND tmplt_id=<template> AND org_rvsn_val='<sourceVersion>' AND base_ver_no=1;
-- Expected reuse key: tmd_mdl_inptbasem_ux01.
-- EXPLAIN SELECT org_row_no FROM tmd_mdl_inptbaserowd WHERE base_id='<baseUUID>'
--  AND inpt_tbl_id=<table> AND org_row_no BETWEEN <first> AND <last> ORDER BY org_row_no;
-- Expected page key: PRIMARY(base_id,inpt_tbl_id,org_row_no) range, not all baseline rows.
-- EXPLAIN SELECT row_id FROM tmd_mdl_inptdrftrowm WHERE drft_id='<draftUUID>'
--  AND inpt_tbl_id=<table> AND file_rvsn_no=0 AND org_row_no BETWEEN <first> AND <last>;
-- Expected existing original-row UK(drft_id,inpt_tbl_id,file_rvsn_no,org_row_no).
-- EXPLAIN SELECT * FROM tmd_mdl_inptdrftrowh WHERE drft_id='<draftUUID>' AND inpt_tbl_id=<table>
--  AND bgn_rvsn_no<=<revision> AND (end_rvsn_no IS NULL OR end_rvsn_no><revision>);
-- Expected existing row-history index; inspect actual plan/selectivity. Integrity whole-count index
-- scans are intentional costs, not automatic failures; full scans for selective pages need correction.

-- 5. ROLLBACK / PARTIAL APPLY — preservation and compatible application, no destructive reverse DDL.
-- Disable NEW shared creation; stop affected work and confirm existing operational token/termination
-- conditions. Do not erase operations, successful revisions, original files or baseline/override rows.
-- If no shared Draft exists: after verification a previous compatible legacy/UPLOAD app can resume.
-- If ANY shared Draft exists: restore the LAST GOOD app supporting BOTH storage formats, including
-- every web/runner/worker reader/writer/FINALIZE path. Keep base_id and all new tables/keys intact.
-- Clearing base_id would hide virtual original rows. Old apps cannot read those Drafts correctly.
-- No DROP/DELETE/TRUNCATE, automatic TTL, reverse migration, base_id=NULL or v2 reset is authorized.
-- Partial DDL: keep writers stopped, inspect SHOW CREATE against each statement, complete only
-- missing approved steps. Do not treat a transaction ROLLBACK as undo for committed MySQL DDL.
-- Fully reverting to an old reader would require a separately approved stopped-write expansion of
-- every virtual identity/history using SAME rowIds, successful-revision/output comparison, then
-- detaching base_id only at the end. That data-conversion operation is outside this package.
-- DBA owns manual execution/captured results; BE owns consumer compatibility and logical replay;
-- operations owns stop/activation/compatible-app recovery. Any missing required evidence is NO-GO.
-- Implementation readiness: ready for the approved implementation slice; actual DB rollout and
-- shared-creation activation remain pending the Open Questions and separate operational approval.
