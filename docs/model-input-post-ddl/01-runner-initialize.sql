-- Document Control
-- status: Draft / MANUAL APPLY ONLY / DEFAULT ROLLBACK; owner: BE / DBA; last synced: 2026-10-22
-- canonical for: Schema rollout — DDL 후 실행기 최초 STOPPED 초기화/verify
-- source: ../model-input-draft-install-ddl.sql MANUAL_INITIALIZE; README.md
-- 00-session.sql SESSION_READY 후 같은 연결에서 실행. 관련 접수/runner/worker는 중지한 상태.
-- 마지막 ROLLBACK까지 실행하면 예행. 실제 저장은 그 직전에서 멈춰 VERIFIED 결과 후 같은 연결에서 COMMIT 선택.
SET @ci_runner_ok=0,@ci_runner_verified=0;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT exec_no,exec_stat_cd FROM tmd_mdl_inptexecm FOR UPDATE;
SET @ci_runner_existing=(SELECT COUNT(*) FROM tmd_mdl_inptexecm);
SET @ci_runner_ok=IF(COALESCE(BINARY @ci_environment IN ('DEVELOPMENT','PRODUCTION'),0)=1
 AND COALESCE(BINARY @@GLOBAL.server_uuid=BINARY @ci_expected_server_uuid,0)=1
 AND COALESCE(BINARY DATABASE()=BINARY @ci_expected_schema,0)=1
 AND @@GLOBAL.read_only=0 AND @@GLOBAL.super_read_only=0 AND @@SESSION.foreign_key_checks=1
 AND @ci_actor IS NOT NULL AND REGEXP_LIKE(@ci_actor,'^[a-zA-Z0-9._@-]{1,50}$','c') AND @ci_runner_existing=0,1,0);
SELECT @ci_runner_existing AS existing_runner_rows_must_be_0,
 IF(@ci_runner_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;
INSERT INTO tmd_mdl_inptexecm(exec_no,crt_usr_logn_id,mod_usr_logn_id,exec_stat_cd)
SELECT 1,@ci_actor,@ci_actor,'STOPPED' FROM DUAL WHERE @ci_runner_ok=1;
SET @ci_runner_verified=IF(@ci_runner_ok=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptexecm)=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptexecm WHERE exec_no=1 AND exec_stat_cd='STOPPED' AND del_yn='N'
      AND exec_id IS NULL AND bgn_dtm IS NULL AND end_dtm IS NULL AND prcs_id IS NULL AND try_tokn IS NULL
      AND aprv_exec_id IS NULL AND aprv_usr_logn_id IS NULL AND aprv_dtm IS NULL
      AND BINARY crt_usr_logn_id=BINARY @ci_actor AND BINARY mod_usr_logn_id=BINARY @ci_actor)=1,1,0);
SELECT @ci_runner_verified AS runner_verified_must_be_1,
 IF(@ci_runner_verified=1,'VERIFIED_RUNNER_INITIALIZED','STOP_AND_ROLLBACK') AS decision;
SELECT exec_no,exec_stat_cd,exec_id,bgn_dtm,end_dtm,prcs_id,try_tokn,
 aprv_exec_id,aprv_usr_logn_id,aprv_dtm FROM tmd_mdl_inptexecm;
-- runner_verified_must_be_1=1 / VERIFIED_RUNNER_INITIALIZED일 때만 마지막 ROLLBACK 대신 COMMIT 선택.
ROLLBACK;
-- 기존 행이 있으면 STOP. UPDATE/UPSERT/DELETE로 초기화하지 않는다. 오류 시 즉시 ROLLBACK하고 중단한다.
