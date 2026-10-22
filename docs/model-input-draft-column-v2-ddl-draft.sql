-- Document Control
-- status: Review / MANUAL SECTIONS ONLY; not applied to any real database
-- canonical for: Draft v2 pre-deployment reset DML, CHECK apply/verify/rollback SQL
-- owner: BE / DBA; date: 2026-10-19; last synced: 2026-10-19
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.9
-- supersedes: earlier §6.9 v1/v2 coexistence recommendation (not the historical v1 SQL)
-- scope: RESET ONLY the seven Draft-owned tables listed below.
-- Preserve original/finalized Case, tcm_filel, storage objects, catalog and Case-template links.
-- No v1 reader, history conversion, policy seed, TRUNCATE, DROP TABLE or FK disabling.
-- Development first, then production: same reviewed file, separate connection/precheck/results.
-- Resolve the actual master schema with the operator; no host/schema/credential is assumed.
-- MySQL 8.0.16+ with enforced CHECK, InnoDB. Compare actual SHOW CREATE before any mutation.
-- All mutating sections are inside /* MANUAL ... */ blocks. Execute selected block contents only.
-- Never execute this entire file as a migration. Do not use a client "continue on error" option.
-- On ANY reset error: ROLLBACK immediately; do not continue to COMMIT or ALTER.

-- 0. SESSION / TARGET (read-only diagnostics; fill the expected schema separately).
SET @mi_column_v2_expected_schema = NULL;
SET @mi_column_v2_authorize_reset = 0;
SELECT DATABASE() AS actual_schema, VERSION() AS mysql_version,
       @@foreign_key_checks AS foreign_key_checks, @@transaction_isolation AS isolation_level;
-- Set @mi_column_v2_expected_schema to the reviewed master schema in THIS connection.
-- Stop Draft intake/web writes/workers/finalization/recovery before backup and keep stopped through verify.
-- Retain a consistent restorable backup of all seven tables and their definitions, plus row counts below.
-- Confirm backup restore method with generated-column handling; do not copy secrets/payloads into this file.

-- 1. PRE-CHECK. All seven tables must exist as InnoDB before running the remaining queries.
SELECT table_name, engine FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftrvsnl',
  'tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
ORDER BY table_name;
-- Missing tables: apply only their missing canonical CREATE/FK sections from read/change-request/finalization
-- DDL drafts first, compare existing definitions, then return here. Do not recreate catalog or Case tables.
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftfiled;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SHOW CREATE TABLE tmd_mdl_inptdrftrowh;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptfnsvfiled;
SELECT tc.table_name, tc.constraint_name, tc.enforced, cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
 ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name
WHERE tc.constraint_schema=DATABASE() AND tc.table_name='tmd_mdl_inptdrftprcsl'
 AND tc.constraint_name='tmd_mdl_inptdrftprcsl_ck03';
-- Before apply: ENFORCED=YES and base_rvsn_no<=9007199254740991 AND json_rvsn_no=1.
-- Already =2: do not reset again; only verify/reconcile the previous execution record.
-- Missing/different constraint: STOP; do not overwrite an unknown definition.

SELECT k.table_schema,k.table_name,k.constraint_name,k.column_name,
       k.referenced_table_name,k.referenced_column_name
FROM information_schema.key_column_usage k
WHERE k.referenced_table_schema=DATABASE() AND k.referenced_table_name IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftrvsnl',
  'tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
ORDER BY k.table_schema,k.table_name,k.constraint_name,k.ordinal_position;
-- Expect only the seven-table internal FK graph in §6.9.
-- Any outside-schema/table inbound FK or trigger side effect: STOP and scope separately.
SELECT trigger_schema,trigger_name,event_object_table,action_timing,event_manipulation
FROM information_schema.triggers
WHERE event_object_schema=DATABASE() AND event_object_table IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftrvsnl',
  'tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled');
-- Expected: zero triggers, unless separately inspected and included in the approved reset scope.

SELECT 'draft' AS item,COUNT(*) AS row_count FROM tmd_mdl_inptdrftm
UNION ALL SELECT 'draft_file',COUNT(*) FROM tmd_mdl_inptdrftfiled
UNION ALL SELECT 'revision',COUNT(*) FROM tmd_mdl_inptdrftrvsnl
UNION ALL SELECT 'row_identity',COUNT(*) FROM tmd_mdl_inptdrftrowm
UNION ALL SELECT 'row_position',COUNT(*) FROM tmd_mdl_inptdrftrowh
UNION ALL SELECT 'request',COUNT(*) FROM tmd_mdl_inptdrftprcsl
UNION ALL SELECT 'finalization_preparation',COUNT(*) FROM tmd_mdl_inptfnsvfiled;
SELECT prcs_type_cd,prcs_stat_cd,json_rvsn_no,COUNT(*) AS row_count
FROM tmd_mdl_inptdrftprcsl GROUP BY prcs_type_cd,prcs_stat_cd,json_rvsn_no;
SELECT COUNT(*) AS active_drafts FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL;
SELECT COUNT(*) AS active_requests FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
SELECT COUNT(*) AS unfinished_file_cleanup FROM tmd_mdl_inptfnsvfiled
WHERE prcs_stat_cd NOT IN ('COMMITTED','CLEANED');
-- All three must be zero. Resolve in-flight work/storage cleanup through the existing owner first.
-- COMMITTED file rows may be deleted here ONLY as Draft-owned preparation metadata;
-- their tcm_filel records, finalized Case and objects remain intact. Never delete a storage prefix.

-- Preserve these counts and a backup/diff of the retained records for post-check (counts alone are insufficient).
SELECT 'case' AS item,COUNT(*) AS row_count FROM tmd_mdl_scnrcasem
UNION ALL SELECT 'file',COUNT(*) FROM tcm_filel
UNION ALL SELECT 'template',COUNT(*) FROM tmd_mdl_inpttmpltm
UNION ALL SELECT 'catalog_file',COUNT(*) FROM tmd_mdl_inptfiled
UNION ALL SELECT 'catalog_sheet',COUNT(*) FROM tmd_mdl_inptshtd
UNION ALL SELECT 'catalog_table',COUNT(*) FROM tmd_mdl_inpttbld
UNION ALL SELECT 'catalog_scope',COUNT(*) FROM tmd_mdl_inptmodscopd
UNION ALL SELECT 'case_template',COUNT(*) FROM tmd_mdl_scnrcasetmpltd;

-- 2. RESET DML (operator selects this block, same connection; all prechecks/backup completed).
/* MANUAL_RESET_BEGIN
SET @mi_column_v2_authorize_reset = 1;
SET @mi_column_v2_reset_ok = IF(
 COALESCE(@mi_column_v2_expected_schema=DATABASE(),0)=1
 AND @mi_column_v2_authorize_reset=1 AND @@foreign_key_checks=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL)=0
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING'))=0
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfnsvfiled WHERE prcs_stat_cd NOT IN ('COMMITTED','CLEANED'))=0,
 1,0);
SELECT @mi_column_v2_reset_ok AS must_be_one;
-- If zero: STOP. No DELETE below will affect rows, but that is NOT a successful reset.
START TRANSACTION;
DELETE FROM tmd_mdl_inptfnsvfiled WHERE @mi_column_v2_reset_ok=1;
DELETE FROM tmd_mdl_inptdrftrowh WHERE @mi_column_v2_reset_ok=1;
DELETE FROM tmd_mdl_inptdrftrowm WHERE @mi_column_v2_reset_ok=1;
-- Self-FK points to rvsn_no-1: delete newest children first, across every Draft.
DELETE FROM tmd_mdl_inptdrftrvsnl WHERE @mi_column_v2_reset_ok=1 ORDER BY rvsn_no DESC,drft_id;
DELETE FROM tmd_mdl_inptdrftfiled WHERE @mi_column_v2_reset_ok=1;
DELETE FROM tmd_mdl_inptdrftprcsl WHERE @mi_column_v2_reset_ok=1;
DELETE FROM tmd_mdl_inptdrftm WHERE @mi_column_v2_reset_ok=1;
-- Repeat section 3 counts in this transaction. All seven must be zero, preserved data unchanged.
-- Choose exactly one command manually: COMMIT on verified success, otherwise ROLLBACK.
MANUAL_RESET_END */
-- COMMIT;
-- ROLLBACK;
-- After explicit COMMIT, continue. Never issue ALTER with this transaction still open.

-- 3. EMPTY VERIFY (also run before COMMIT inside reset transaction, then again after COMMIT).
SELECT 'draft' AS item,COUNT(*) AS must_be_zero FROM tmd_mdl_inptdrftm
UNION ALL SELECT 'draft_file',COUNT(*) FROM tmd_mdl_inptdrftfiled
UNION ALL SELECT 'revision',COUNT(*) FROM tmd_mdl_inptdrftrvsnl
UNION ALL SELECT 'row_identity',COUNT(*) FROM tmd_mdl_inptdrftrowm
UNION ALL SELECT 'row_position',COUNT(*) FROM tmd_mdl_inptdrftrowh
UNION ALL SELECT 'request',COUNT(*) FROM tmd_mdl_inptdrftprcsl
UNION ALL SELECT 'finalization_preparation',COUNT(*) FROM tmd_mdl_inptfnsvfiled;
-- Re-run preserved-record comparison from section 1; stop on unexplained difference.

-- 4. APPLY CHECK (all seven tables empty, reset COMMITTED, writers still stopped).
-- No column/table/index changes, no backfill; shared CHANGE/FINALIZE request storage version is 2.
/* MANUAL_APPLY_BEGIN
ALTER TABLE tmd_mdl_inptdrftprcsl
 DROP CHECK tmd_mdl_inptdrftprcsl_ck03,
 ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck03
 CHECK(base_rvsn_no<=9007199254740991 AND json_rvsn_no=2) ENFORCED;
MANUAL_APPLY_END */
-- Compare SHOW CREATE after uncertain/error outcome before any retry; do not rerun the reset.

-- 5. VERIFY SCHEMA; deploy the v2-only web/worker/finalization/read code before reopening intake.
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SELECT tc.constraint_name,tc.enforced,cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
 ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name
WHERE tc.constraint_schema=DATABASE() AND tc.table_name='tmd_mdl_inptdrftprcsl'
 AND tc.constraint_name='tmd_mdl_inptdrftprcsl_ck03';
-- Expected =2 and original baseRevision upper bound; all other definitions unchanged.
-- Any existing v1 application writes/reads 1: it MUST NOT be restarted on this target as-is.

-- 6. POST-SMOKE CHECKS (new test Draft only; no synthetic production rows inserted by this file).
SELECT COUNT(*) AS invalid_request_versions FROM tmd_mdl_inptdrftprcsl WHERE json_rvsn_no<>2;
SELECT COUNT(*) AS invalid_revision_versions FROM tmd_mdl_inptdrftrvsnl WHERE json_rvsn_no<>2;
SELECT COUNT(*) AS invalid_success_payloads FROM tmd_mdl_inptdrftprcsl
WHERE prcs_type_cd='CHANGE' AND prcs_stat_cd='SUCCEEDED' AND
 (COALESCE(JSON_TYPE(JSON_EXTRACT(rslt_json,'$.columnMappings')),'MISSING')<>'ARRAY'
  OR COALESCE(JSON_TYPE(JSON_EXTRACT(rslt_json,'$.rowMappings')),'MISSING')<>'ARRAY');
SELECT COUNT(*) AS invalid_delta_columns FROM tmd_mdl_inptdrftrvsnl
WHERE rvsn_no>0 AND COALESCE(JSON_TYPE(JSON_EXTRACT(dlta_json,'$.columns')),'MISSING')<>'ARRAY';
-- All zero. This checks version/container shape only, NOT correct logical restore or file output.
-- Application acceptance: §6.9 append/delete/re-add/replay/blank/0/row×column/failure/restart/finalization cases.

-- 7. ROLLBACK / FALLBACK (separate manual action; never run as part of the apply sequence).
-- Before reset COMMIT: ROLLBACK restores deleted rows. No ALTER may have been run in that transaction.
-- After reset COMMIT: rows do not return through ROLLBACK; no data restoration is intended by this reset.
-- Before any new v2 data exists, stop all writers and verify all seven empty with section 3.
-- Then the previous CHECK can be restored for an EMPTY old-code environment:
/* MANUAL_ROLLBACK_BEGIN
ALTER TABLE tmd_mdl_inptdrftprcsl
 DROP CHECK tmd_mdl_inptdrftprcsl_ck03,
 ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck03
 CHECK(base_rvsn_no<=9007199254740991 AND json_rvsn_no=1) ENFORCED;
MANUAL_ROLLBACK_END */
-- Once ANY new v2 data exists, do not narrow the CHECK, relabel data as v1, or automatically reset again.
-- Stop writes and fix forward with v2-readable code. A later reset/backup restoration is a separately scoped action.
-- If an incorrect target was reset, stop here and use the DBA-controlled seven-table backup restore plan.
-- Restore parent-first (Draft, requests, revisions ascending, row identities, row positions, Draft files,
-- finalization metadata), preserving generated-column behavior and referenced Case/file/catalog records.
-- Do not restore other features or switch foreign_key_checks off to force a restore.

-- References: https://dev.mysql.com/doc/refman/8.0/en/delete.html (single-table ordered DELETE)
-- https://dev.mysql.com/doc/refman/8.0/en/alter-table.html (DROP/ADD CHECK)
-- https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html (DDL implicit commit)
