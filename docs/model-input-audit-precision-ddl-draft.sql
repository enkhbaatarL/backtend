-- Document Control
-- status: Draft / USER REVIEW AND MANUAL APPLY ONLY
-- canonical for: catalog/Draft audit precision migration, rollback and verification SQL
-- owner: BE / user applies; external DBA approval is not asserted
-- date: 2026-10-19
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md section 6.8
-- supersedes: DATETIME(6) audit precision in earlier catalog/Draft CREATE drafts only
-- open questions: actual pre/post results, backup/maintenance approval and environment timezones
-- Codex does not connect to or execute SQL against the real database.
-- This file contains READ-ONLY queries that PRODUCE statements for separate manual review/execution.
-- Never execute generated statements before all go/no-go conditions below are met.

-- Scope: six catalog + seven Draft-design tables; only PRESENT tables are generated.
-- Missing final-file table is expected before its separate rollout. Do not create it here.
-- Keep names, actors, IDs, statuses, revisions, files, payloads, PK/FK/UK and row bindings.
-- crt_dtm/mod_dtm become DATETIME NOT NULL, defaults CURRENT_TIMESTAMP;
-- mod_dtm retains ON UPDATE CURRENT_TIMESTAMP and both comments are preserved.
-- aply_bgn_dtm and processing expr_dtm/bgn_dtm/end_dtm remain DATETIME(6).
-- New requests use whole-second crt_dtm + 120 seconds; precise DB deadline comparisons remain.
-- Existing TERMINAL request expr_dtm is shortened by its creation fraction (<1 second),
-- atomically with crt_dtm, to retain ck09. bgn_dtm/end_dtm are not rewritten.

-- 1. PRE-CHECK (read-only; retain/export all outputs).
SELECT DATABASE() AS target_schema, VERSION() AS mysql_version, @@session.sql_mode,
       @@session.time_zone, @@global.time_zone, @@system_time_zone;
SELECT table_name,column_name,column_type,datetime_precision,is_nullable,column_default,extra,column_comment
FROM information_schema.columns WHERE table_schema=DATABASE()
AND table_name IN ('tmd_mdl_inpttmpltm','tmd_mdl_inptfiled','tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld','tmd_mdl_inptmodscopd','tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled','tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
AND column_name IN ('crt_dtm','mod_dtm','aply_bgn_dtm','expr_dtm','bgn_dtm','end_dtm')
ORDER BY table_name,ordinal_position;
-- Compare SHOW CREATE TABLE for EVERY present target. Expected audit pair: DATETIME(6), NOT NULL,
-- CURRENT_TIMESTAMP(6); mod only has ON UPDATE CURRENT_TIMESTAMP(6). Already DATETIME(0) is a skip.
-- Any other type/default/trigger/check referencing audit columns: STOP and revise this draft.
SELECT trigger_name,event_object_table,action_statement FROM information_schema.triggers
WHERE trigger_schema=DATABASE() AND event_object_table IN
 ('tmd_mdl_inpttmpltm','tmd_mdl_inptfiled','tmd_mdl_inptshtd','tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd','tmd_mdl_scnrcasetmpltd','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled');
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SELECT constraint_name,enforced FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='tmd_mdl_inptdrftprcsl' AND constraint_type='CHECK';
SELECT COUNT(*) AS active_requests FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
SELECT COUNT(*) AS active_pointers FROM tmd_mdl_inptdrftm WHERE actv_prcs_id IS NOT NULL;
SELECT COUNT(*) AS invalid_deadlines FROM tmd_mdl_inptdrftprcsl WHERE expr_dtm<>TIMESTAMPADD(SECOND,120,crt_dtm);
-- All three counts MUST be zero, ck09 must match and be ENFORCED. Do not force-fail/delete active work.

-- Generate READ-ONLY fractional-value/count checks. Copy and execute the returned SELECTs first.
SELECT CONCAT('SELECT ',CHAR(39),table_name,CHAR(39),' AS table_name, COUNT(*) AS rows_total, ',
 'COALESCE(SUM(MICROSECOND(crt_dtm)<>0),0) AS fractional_created, ',
 'COALESCE(SUM(MICROSECOND(mod_dtm)<>0),0) AS fractional_modified FROM `',table_name,'`;') AS pre_post_check_sql
FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='crt_dtm'
AND table_name IN ('tmd_mdl_inpttmpltm','tmd_mdl_inptfiled','tmd_mdl_inptshtd','tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd','tmd_mdl_scnrcasetmpltd','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
ORDER BY table_name;

-- Export these identity/state results before AND after. Compare exactly; do not reset Case103.
SELECT drft_id,org_case_id,org_rvsn_val,drft_stat_cd,rvsn_no,actv_prcs_id,fnsv_case_id
FROM tmd_mdl_inptdrftm WHERE drft_id='08a7b46a-6f38-49dc-8065-30334102631a';
SELECT mdl_file_cd,file_no FROM tmd_mdl_inptdrftfiled
WHERE drft_id='08a7b46a-6f38-49dc-8065-30334102631a' ORDER BY mdl_file_cd;
SELECT rvsn_no,json_rvsn_no,base_rvsn_no,prcs_id,dlta_json FROM tmd_mdl_inptdrftrvsnl
WHERE drft_id='08a7b46a-6f38-49dc-8065-30334102631a' ORDER BY rvsn_no;
SELECT row_id,inpt_tbl_id,org_row_no,prcs_id,clnt_row_id FROM tmd_mdl_inptdrftrowm
WHERE drft_id='08a7b46a-6f38-49dc-8065-30334102631a' ORDER BY row_id;
SELECT row_id,bgn_rvsn_no,end_rvsn_no,inpt_tbl_id,row_no FROM tmd_mdl_inptdrftrowh
WHERE drft_id='08a7b46a-6f38-49dc-8065-30334102631a' ORDER BY row_id,bgn_rvsn_no;

-- 2. GO/NO-GO / DEPLOYMENT (manual)
-- Pause ALL catalog/Draft writers/dispatchers across instances and drain active work normally.
-- Repeat section 1 AFTER quiescence. Backup/export all target rows keyed by PK (audit times +
-- request expr_dtm included), retain SHOW CREATE, row counts and Case103 source/file metadata.
-- Confirm loss of subsecond audit values and old terminal deadline fractions with the user/operator.
-- DATETIME(6)->DATETIME cannot recover those fractions by changing the type back.
-- Deploy precision-compatible code FIRST while writers remain stopped. It also works with old (6) tables.
-- Old enqueue code is incompatible with new crt_dtm(0)/ck09: DO NOT resume/rollback old code on new DDL.
-- Existing business data stays in place. Do not run the initial CREATE scripts or disable FK/CHECK.

-- 3. BACKFILL generator (read-only). Review/copy statements, run in one manual transaction,
-- verify fractional counts/ck09/identity snapshots, then COMMIT; on any error ROLLBACK before DDL.
-- Explicit mod_dtm avoids ON UPDATE replacing historical modification times with migration time.
-- expr comes first because MySQL UPDATE assignments evaluate left-to-right.
SELECT CONCAT('UPDATE `',table_name,'` SET ',
 IF(table_name='tmd_mdl_inptdrftprcsl',
 'expr_dtm=TIMESTAMPADD(SECOND,120,TIMESTAMPADD(MICROSECOND,-MICROSECOND(crt_dtm),crt_dtm)), ',''),
 'crt_dtm=TIMESTAMPADD(MICROSECOND,-MICROSECOND(crt_dtm),crt_dtm), ',
 'mod_dtm=TIMESTAMPADD(MICROSECOND,-MICROSECOND(mod_dtm),mod_dtm) ',
 'WHERE MICROSECOND(crt_dtm)<>0 OR MICROSECOND(mod_dtm)<>0;') AS backfill_sql
FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='crt_dtm'
AND table_name IN ('tmd_mdl_inpttmpltm','tmd_mdl_inptfiled','tmd_mdl_inptshtd','tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd','tmd_mdl_scnrcasetmpltd','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
ORDER BY table_name;

-- 4. APPLY generator (read-only). Only execute AFTER committed backfill verifies all fractions=0.
-- Review each generated ALTER against retained SHOW CREATE; skip absent/already aligned tables.
-- MySQL prohibits modifying a CHECK-referenced column unless that CHECK is dropped in the same
-- statement. Request ALTER drops and re-adds the SAME ck09 in one atomic ALTER; no unenforced window.
-- Retain all other keys/checks/defaults/comments. DDL implicitly commits and may rebuild/lock a table;
-- schedule downtime/disk headroom with the operator, do not assume online/instant ALTER.
SELECT CONCAT('ALTER TABLE `',table_name,'` ',
 IF(table_name='tmd_mdl_inptdrftprcsl','DROP CHECK tmd_mdl_inptdrftprcsl_ck09, ',''),
 GROUP_CONCAT(CONCAT('MODIFY COLUMN `',column_name,'` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
 IF(column_name='mod_dtm',' ON UPDATE CURRENT_TIMESTAMP',''),
 ' COMMENT ',CHAR(39),REPLACE(column_comment,CHAR(39),CONCAT(CHAR(39),CHAR(39))),CHAR(39))
 ORDER BY ordinal_position SEPARATOR ', '),
 IF(table_name='tmd_mdl_inptdrftprcsl',
 ', ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck09 CHECK (expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm))',''),';') AS apply_sql
FROM information_schema.columns WHERE table_schema=DATABASE()
AND column_name IN ('crt_dtm','mod_dtm') AND data_type='datetime' AND is_nullable='NO'
AND table_name IN ('tmd_mdl_inpttmpltm','tmd_mdl_inptfiled','tmd_mdl_inptshtd','tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd','tmd_mdl_scnrcasetmpltd','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh','tmd_mdl_inptdrftprcsl','tmd_mdl_inptfnsvfiled')
GROUP BY table_name HAVING COUNT(*)=2 AND MAX(datetime_precision)>0 ORDER BY table_name;

-- 5. VERIFY (read-only): rerun section 1 and every generated fractional check.
-- Audit precision=0, defaults/ON UPDATE precision=0; processing/apply time precision remains 6.
-- Fraction counts=0, unchanged row totals/identities/business payloads, invalid_deadlines=0,
-- active requests/pointers=0 and enforced ck09 required before resuming writes.
-- Verify EDT-01 resume/EDT-02/03 keep the existing Case103 Draft/revision/row IDs.
-- Test new enqueue/deadline behavior on a separate approved fixture, never mutate/reset Case103.
-- Actual MySQL ALTER/constraint validation is a USER-RUN check; H2 tests cannot replace it.

-- 6. ROLLBACK / PARTIAL FAILURE (manual; no DROP/DELETE/TRUNCATE)
-- Keep writers stopped and the compatible new code. Inspect each table after partial DDL failure;
-- fix/retry only missing steps, or restore the retained exact SHOW CREATE audit definitions via ALTER.
-- For request crt_dtm rollback to (6), also drop/re-add ck09 in that SAME ALTER as in section 4.
-- Widening DATETIME back to DATETIME(6) restores storage capacity only, never lost fractions.
-- If exact time restoration is required, with writers still stopped restore PK-keyed audit values
-- and request expr_dtm together from the verified backup in one transaction under enforced ck09.
-- Restore only these time columns; do not overwrite Case/revision/rows/payloads/files or replay work.
-- Old app may resume only after its original compatible definitions and checks are verified.
-- Timezone configuration is recorded for follow-up; no timezone/global SQL-mode change is included.
-- References: https://dev.mysql.com/doc/refman/8.0/en/fractional-seconds.html
--             https://dev.mysql.com/doc/refman/8.0/en/alter-table.html
