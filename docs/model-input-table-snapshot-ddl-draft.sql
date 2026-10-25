-- Document Control
-- status: Manual rollout template / MANUAL DBA REVIEW AND APPLY ONLY
-- canonical for: Schema rollout - MySQL Table Snapshot objects and manual rollout
-- owner: BE / DBA; date: 2026-10-25
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md section3.2
-- supersedes: none; additive masterDataSource extension
-- environment execution evidence: docs/work-notes/2026/2026-10-25-excel-table-mysql-snapshot-implementation.md
-- Open questions: actual master MySQL definitions, capacity/packet/connection budget and manual apply evidence.
-- New dictionary token: SNAP (snapshot). Other tokens reuse the existing dictionary.
-- Intentional JSON denormalization: immutable original Cell payload, not per-cell searchable business state.

-- 1. PRECHECK (read only). Keep mysql.enabled=false until schema verification completes.
SELECT DATABASE(),VERSION(),@@max_allowed_packet,@@sql_mode;
SHOW CREATE TABLE tmd_mdl_scnrcasem;
SHOW CREATE TABLE tmd_custcpnym;
SHOW CREATE TABLE tmd_usrm;
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()
 AND table_name IN ('tmd_mdl_inptsnapm','tmd_mdl_inptsnapd','tmd_mdl_inptsnapjsond');
-- Expected none. Existing/partial definitions: STOP, compare SHOW CREATE and reconcile manually.
-- MySQL8.0.16+ enforced CHECK, InnoDB, matching parent types, DDL privilege and disk/backup headroom required.
-- max_allowed_packet must exceed a bounded part (8MiB+2) plus statement/protocol overhead.

-- 2. APPLY. DDL implicitly commits. Do not blindly rerun or use CREATE IF NOT EXISTS.
CREATE TABLE tmd_mdl_inptsnapm (
 snap_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
 crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 del_yn CHAR(1) NOT NULL DEFAULT 'N',
 cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
 usr_no INT UNSIGNED NOT NULL,
 cust_cpny_cd VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 org_case_id INT UNSIGNED NOT NULL,
 base_ver_no SMALLINT UNSIGNED NOT NULL,
 prcs_stat_cd VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 expr_dtm DATETIME(6) NULL,
 org_rvsn_val VARCHAR(68) CHARACTER SET ascii COLLATE ascii_bin NULL,
 CONSTRAINT tmd_mdl_inptsnapm_ux01 UNIQUE(cust_cpny_no,org_case_id,base_ver_no),
 INDEX tmd_mdl_inptsnapm_ix01(prcs_stat_cd,crt_dtm,snap_id),
 INDEX tmd_mdl_inptsnapm_ix02(org_case_id),
 INDEX tmd_mdl_inptsnapm_ix03(usr_no),
 CONSTRAINT tmd_mdl_inptsnapm_fk01 FOREIGN KEY(cust_cpny_no) REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapm_fk02 FOREIGN KEY(org_case_id) REFERENCES tmd_mdl_scnrcasem(case_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapm_fk03 FOREIGN KEY(usr_no) REFERENCES tmd_usrm(usr_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapm_ck01 CHECK(base_ver_no=1 AND del_yn='N'),
 CONSTRAINT tmd_mdl_inptsnapm_ck02 CHECK(prcs_stat_cd IN ('QUEUED','BUILDING','READY','FAILED')),
 CONSTRAINT tmd_mdl_inptsnapm_ck03 CHECK((prcs_stat_cd='BUILDING' AND try_tokn IS NOT NULL AND expr_dtm IS NOT NULL)
  OR (prcs_stat_cd<>'BUILDING' AND try_tokn IS NULL AND expr_dtm IS NULL)),
 CONSTRAINT tmd_mdl_inptsnapm_ck04 CHECK(prcs_stat_cd<>'READY' OR org_rvsn_val IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tmd_mdl_inptsnapd (
 snap_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 inpt_tbl_id INT UNSIGNED NOT NULL,
 crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 del_yn CHAR(1) NOT NULL DEFAULT 'N',
 obj_ref_json LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 cntt_hash_val CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 PRIMARY KEY(snap_id,inpt_tbl_id),
 INDEX tmd_mdl_inptsnapd_ix01(inpt_tbl_id),
 CONSTRAINT tmd_mdl_inptsnapd_fk01 FOREIGN KEY(snap_id) REFERENCES tmd_mdl_inptsnapm(snap_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapd_fk02 FOREIGN KEY(inpt_tbl_id) REFERENCES tmd_mdl_inpttbld(inpt_tbl_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapd_ck01 CHECK(OCTET_LENGTH(obj_ref_json)<=1048576 AND del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tmd_mdl_inptsnapjsond (
 snap_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 inpt_tbl_id INT UNSIGNED NOT NULL,
 json_ord SMALLINT UNSIGNED NOT NULL,
 crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
 mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 del_yn CHAR(1) NOT NULL DEFAULT 'N',
 cntt_json LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 PRIMARY KEY(snap_id,inpt_tbl_id,json_ord),
 CONSTRAINT tmd_mdl_inptsnapjsond_fk01 FOREIGN KEY(snap_id,inpt_tbl_id) REFERENCES tmd_mdl_inptsnapd(snap_id,inpt_tbl_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT tmd_mdl_inptsnapjsond_ck01 CHECK(json_ord<1024 AND OCTET_LENGTH(cntt_json)<=8388610 AND del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. BACKFILL: none automatically. New committed Case registration enqueues with trusted owner context.
-- Existing Case population is a separately approved bounded operation using the application enqueue path.
-- Failures keep FAILED headers; no automatic retry or global delete. Do not alter Case/files/Draft rows.

-- 4. VERIFY. Check full SHOW CREATE, FK/CHECK enforcement and compare existing business rows unchanged.
SHOW CREATE TABLE tmd_mdl_inptsnapm;
SHOW CREATE TABLE tmd_mdl_inptsnapd;
SHOW CREATE TABLE tmd_mdl_inptsnapjsond;
SELECT prcs_stat_cd,COUNT(*) FROM tmd_mdl_inptsnapm GROUP BY prcs_stat_cd;
-- Expected empty before activation. Enable only after application/worker/schema and source guards verified.
-- Read index: ux01(company,Case,builder), d PK(snapshot,table), jsond PK(snapshot,table,ordinal).
-- EXPLAIN the bound application SELECTs with representative values; no full JSON payload logging.

-- 5. ROLLBACK: set table-snapshot.enabled=false and mysql.enabled=false, then deploy compatible code.
-- Preserve all successful snapshots and original Case/files/Drafts. Do not DROP tables or erase READY data.
-- Running builders are fenced by state/token/deadline and cannot publish after expiry.
-- Re-enable only after verifying state and the supported schema/extractor version.
