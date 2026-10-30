-- Document Control
-- status: Draft / user-run validation observed; COMMIT not confirmed
-- canonical for: Manual independent Case -> existing scenario linkage apply/verify SQL
-- owner: BE / DBA; date: 2026-10-30
-- source documents:
--   src/main/java/com/sk/domain/mi/service/nextgen/query/LatestModelParamProposalExecutionContextResolver.java
--   src/main/java/com/sk/domain/mi/service/nextgen/ModelParamProposalServiceImpl.java
--   src/main/java/com/sk/domain/shared/repository/master/tmd/custom/CustomScenarioCaseRepositoryImpl.java
--   docs/design-docs/gas/mi-model-param-proposal-execprep-erd-ddl-draft.md
--
-- MySQL 8.x / same connection / select the intended customer DB before running.
-- 대상: 기존 COMPLETED 독립 Case와 최신 Run, 같은 시나리오의 PROPOSED 또는 CONFIRMED Agent 제안.
-- PROPOSED이고 확정값/확정시각이 SQL NULL이면 제안 JSON을 확정값으로 복사한다.
-- 이미 CONFIRMED인 제안의 확정값·시각·Agent audit은 보존한다.
-- 기존 preparation은 case_id가 NULL 또는 대상 Case여야 한다. 다른 Case 연결은 덮어쓰지 않는다.
-- 변경: Case, preparation 및 필요한 경우 Agent의 확정 JSON/상태/확정시각/audit.
-- Run/결과/파일/제안 원본/callback/가격해석은 변경하지 않고 모델을 다시 실행하지 않는다.
-- Agent 테이블 UPDATE 권한이 필요하며, 변경 전 세 행의 원본을 별도로 보관한다.
-- 연결된 제안 JSON과 독립 실행의 실제 입력 Excel이 같다는 것을 이 SQL이 보장하지는 않는다.
-- Case 전체 유형을 변경하므로 해당 Case의 모든 과거 Run이 함께 시나리오 소속이 된다.
--
-- 실행 방법:
--   A. 아래 3개 ID와 actor를 입력하고 1번의 사전 조회 결과를 확인한다.
--   B. 대상 시나리오/Case의 실행·편집·제안 요청 및 Agent 작업이 끝난 상태에서 2~5번을
--      같은 connection으로 실행한다. 실행 중에는 해당 데이터에 대한 다른 쓰기를 중지한다.
--      기존 transaction이 없는 전용 connection을 사용하고 대상 세 행의 원본을 별도로 보관한다.
--   C. 파일 전체 실행은 마지막 ROLLBACK으로 변경을 취소한다.
--      실제 반영은 5번까지 실행해 READY_TO_COMMIT과 검증 항목을 확인한 후,
--      마지막 ROLLBACK 대신 같은 connection에서 COMMIT을 별도로 실행한다.
--   D. ANY SQL error / STOP / 예상과 다른 조회 결과: 즉시 ROLLBACK 후 중단.
--      오류 무시(--force) 또는 @rl_ok 등 guard 변수 수동 변경 금지.
--      UPDATE를 단독 선택 실행하지 말 것. 기본 ROLLBACK을 삭제한 채 전체 실행하지 말 것.
--   E. COMMIT 이후 이 SQL 재실행은 INDEPENDENT 사전조건에서 차단된다.
--      COMMIT 후 복구는 원본 세 행과 현재 상태를 다시 검토하는 별도 작업이다.

-- 1. 입력 및 읽기 전용 사전 조회 (아직 transaction/UPDATE 없음).
SET @rl_case_id = NULL;       -- 기존 독립 실행 Case ID
SET @rl_scnr_id = NULL;       -- 연결할 기존 시나리오 ID
SET @rl_exec_prep_id = NULL;  -- 재사용할 기존 실행준비 이력 ID
SET @rl_actor = 'SYSTEM';    -- 승인된 작업자 login ID (최대 50자)
SET @rl_ok = 0, @rl_verified = 0, @rl_run_id = NULL, @rl_run_seq = NULL;

SELECT DATABASE() AS target_schema, @@hostname AS server_hostname, @@port AS server_port,
       @@session.time_zone AS session_time_zone,
       @rl_case_id AS input_case_id, @rl_scnr_id AS input_scnr_id,
       @rl_exec_prep_id AS input_exec_prep_id;

-- Case와 모든 실행이력: last_run_id가 연결할 결과의 Run인지 확인.
SELECT c.case_id, c.usr_no, c.exec_type_cd, c.scnr_id, c.case_stat_cd, c.last_run_id,
       c.del_yn, c.mod_usr_logn_id, c.mod_dtm,
       h.run_id, h.run_seq, h.exec_stat_cd, h.exec_bgn_dtm, h.exec_end_dtm,
       h.del_yn AS run_del_yn
FROM tmd_mdl_scnrcasem c
LEFT JOIN tmd_mdl_scnrcaseh h ON h.case_id = c.case_id
WHERE c.case_id = @rl_case_id
ORDER BY h.run_seq DESC;

SELECT s.scenario_id, s.scenario_group_id, s.title, s.status, s.del_yn,
       g.del_yn AS group_del_yn
FROM tmm_mi_scenario_m s
JOIN tmm_mi_scenario_group_m g ON g.scenario_group_id = s.scenario_group_id
WHERE s.scenario_id = @rl_scnr_id;

-- 같은 시나리오/대상 Case의 preparation 전체: 지정 행과 최신 행, 기존 연결 Case를 확인.
-- JSON 본문/스토리지 경로는 출력하지 않는다.
SELECT p.exec_prep_id, p.scnr_id, p.req_usr_no, p.case_id, p.exec_prep_stat_cd,
       p.agnt_prpsl_id, p.del_yn, p.mod_usr_logn_id, p.mod_dtm,
       p.err_msg_cntt IS NOT NULL AS has_preparation_error,
       a.scenario_id AS proposal_scenario_id, a.scenario_group_id AS proposal_group_id,
       a.status AS proposal_status, a.del_yn AS proposal_del_yn,
       JSON_TYPE(a.proposal_parameters) AS proposed_json_type,
       JSON_LENGTH(a.proposal_parameters) AS proposed_item_count,
       JSON_TYPE(a.confirmation_parameters) AS confirmed_json_type,
       JSON_LENGTH(a.confirmation_parameters) AS confirmed_item_count
FROM tmd_mi_model_execpreph p
LEFT JOIN tmm_mi_model_param_proposal_m a ON a.model_param_proposal_id = p.agnt_prpsl_id
WHERE p.exec_prep_id = @rl_exec_prep_id OR p.scnr_id = @rl_scnr_id OR p.case_id = @rl_case_id
ORDER BY p.exec_prep_id DESC;

SELECT c.case_id, c.exec_type_cd, c.case_stat_cd, c.last_run_id, c.del_yn,
       h.run_seq, h.exec_stat_cd, h.del_yn AS run_del_yn
FROM tmd_mdl_scnrcasem c
LEFT JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id
WHERE c.scnr_id = @rl_scnr_id
ORDER BY c.case_id DESC;

SELECT prc_interp_id, status, src_run_id, del_yn
FROM tmm_mi_scenario_prc_interp_m
WHERE scenario_id = @rl_scnr_id AND del_yn = 'N';

-- 2. 잠금 및 사전조건 재확인. 사전 조회 후 데이터 변경 가능성이 있어 transaction 안에서 다시 판정.
-- SERIALIZABLE은 transaction 안의 읽기 범위도 보호한다. transaction은 짧게 유지한다.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

SELECT scenario_id FROM tmm_mi_scenario_m WHERE scenario_id = @rl_scnr_id FOR UPDATE;
SELECT g.scenario_group_id
FROM tmm_mi_scenario_group_m g
JOIN tmm_mi_scenario_m s ON s.scenario_group_id = g.scenario_group_id
WHERE s.scenario_id = @rl_scnr_id FOR SHARE;
SELECT case_id FROM tmd_mdl_scnrcasem
WHERE case_id = @rl_case_id OR scnr_id = @rl_scnr_id ORDER BY case_id FOR UPDATE;
SELECT h.run_id
FROM tmd_mdl_scnrcaseh h JOIN tmd_mdl_scnrcasem c ON c.case_id = h.case_id
WHERE c.case_id = @rl_case_id OR c.scnr_id = @rl_scnr_id
ORDER BY h.run_seq FOR UPDATE;
SELECT exec_prep_id FROM tmd_mi_model_execpreph
WHERE exec_prep_id = @rl_exec_prep_id OR scnr_id = @rl_scnr_id OR case_id = @rl_case_id
ORDER BY exec_prep_id FOR UPDATE;
SELECT a.model_param_proposal_id
FROM tmm_mi_model_param_proposal_m a
JOIN tmd_mi_model_execpreph p ON p.agnt_prpsl_id = a.model_param_proposal_id
WHERE p.exec_prep_id = @rl_exec_prep_id FOR UPDATE;
SELECT prc_interp_id FROM tmm_mi_scenario_prc_interp_m
WHERE scenario_id = @rl_scnr_id AND del_yn = 'N' FOR SHARE;

SET @rl_run_id = (SELECT last_run_id FROM tmd_mdl_scnrcasem WHERE case_id = @rl_case_id);
SET @rl_run_seq = (SELECT run_seq FROM tmd_mdl_scnrcaseh
                   WHERE run_id = @rl_run_id AND case_id = @rl_case_id);

SET @rl_proposal_id = (SELECT agnt_prpsl_id FROM tmd_mi_model_execpreph WHERE exec_prep_id = @rl_exec_prep_id);
SET @rl_confirm_required = (SELECT IF(status = 'PROPOSED', 1, 0) FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);
SET @rl_proposed_hash_before = (SELECT SHA2(CAST(proposal_parameters AS CHAR), 256) FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);
SET @rl_confirmed_hash_before = (SELECT SHA2(CAST(confirmation_parameters AS CHAR), 256) FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);
SET @rl_confirmed_at_before = (SELECT confirmed_dtm FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);
SET @rl_agent_actor_before = (SELECT mod_usr_logn_id FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);
SET @rl_agent_modified_before = (SELECT mod_dtm FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id);

-- UPDATE 전 원본 행. audit와 오류값까지 포함해 복구가 필요한 경우 별도 보관한다.
-- err_msg_cntt에는 내부 정보가 있을 수 있으므로 이 결과는 승인된 DB 도구 안에서만 보관한다.
SELECT case_id, exec_type_cd, scnr_id, case_stat_cd, last_run_id,
       mod_usr_logn_id, mod_dtm, del_yn
FROM tmd_mdl_scnrcasem WHERE case_id = @rl_case_id;
SELECT exec_prep_id, scnr_id, case_id, exec_prep_stat_cd, agnt_prpsl_id,
       err_msg_cntt, mod_usr_logn_id, mod_dtm, del_yn
FROM tmd_mi_model_execpreph WHERE exec_prep_id = @rl_exec_prep_id;
-- JSON 원본도 복구용으로 승인된 DB 도구 안에서만 안전하게 보관한다.
SELECT model_param_proposal_id, status, proposal_parameters, confirmation_parameters,
       confirmed_dtm, mod_usr_logn_id, mod_dtm, del_yn
FROM tmm_mi_model_param_proposal_m WHERE model_param_proposal_id = @rl_proposal_id;

SET @rl_ids_ok = IF(@rl_case_id > 0 AND @rl_scnr_id > 0 AND @rl_exec_prep_id > 0
    AND DATABASE() IS NOT NULL AND CHAR_LENGTH(TRIM(@rl_actor)) BETWEEN 1 AND 50, 1, 0);
SET @rl_storage_ok = IF(
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE() AND table_name IN ('tmd_mdl_scnrcasem','tmd_mi_model_execpreph','tmm_mi_model_param_proposal_m')
       AND engine = 'InnoDB') = 3
    AND NOT EXISTS (SELECT 1 FROM information_schema.triggers
                    WHERE trigger_schema = DATABASE()
                      AND event_object_table IN ('tmd_mdl_scnrcasem','tmd_mi_model_execpreph','tmm_mi_model_param_proposal_m')), 1, 0);
SET @rl_case_ok = (SELECT COUNT(*) FROM tmd_mdl_scnrcasem c
    JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id AND h.case_id = c.case_id
    WHERE c.case_id = @rl_case_id AND c.exec_type_cd = 'INDEPENDENT'
      AND (c.scnr_id IS NULL OR c.scnr_id = @rl_scnr_id)
      AND c.case_stat_cd = 'COMPLETED' AND c.del_yn = 'N'
      AND h.exec_stat_cd = 'COMPLETED' AND h.del_yn = 'N' AND h.run_seq = @rl_run_seq);
SET @rl_link_ok = (SELECT COUNT(*)
    FROM tmd_mi_model_execpreph p
    JOIN tmm_mi_scenario_m s ON s.scenario_id = p.scnr_id
    JOIN tmm_mi_scenario_group_m g ON g.scenario_group_id = s.scenario_group_id
    JOIN tmm_mi_model_param_proposal_m a ON a.model_param_proposal_id = p.agnt_prpsl_id
    WHERE p.exec_prep_id = @rl_exec_prep_id AND p.scnr_id = @rl_scnr_id AND p.del_yn = 'N'
      AND (p.case_id IS NULL OR p.case_id = @rl_case_id)
      AND p.exec_prep_stat_cd IN ('PROPOSAL_PENDING','EXECUTION_FAILED','EXECUTING')
      AND s.del_yn = 'N' AND s.status <> 'discarded' AND g.del_yn = 'N'
      AND a.scenario_id = s.scenario_id AND a.scenario_group_id = s.scenario_group_id
      AND a.model_param_proposal_id = @rl_proposal_id AND a.del_yn = 'N'
      AND JSON_TYPE(a.proposal_parameters) = 'ARRAY'
      AND ((a.status = 'PROPOSED' AND a.confirmation_parameters IS NULL AND a.confirmed_dtm IS NULL)
        OR (a.status = 'CONFIRMED' AND JSON_TYPE(a.confirmation_parameters) = 'ARRAY')));
SET @rl_shared_proposal_cases = (SELECT COUNT(*) FROM tmd_mi_model_execpreph
    WHERE @rl_confirm_required = 1 AND agnt_prpsl_id = @rl_proposal_id
      AND case_id IS NOT NULL AND exec_prep_id <> @rl_exec_prep_id AND del_yn = 'N');
SET @rl_conflicts =
    (SELECT COUNT(*) FROM tmd_mi_model_execpreph
      WHERE case_id = @rl_case_id AND exec_prep_id <> @rl_exec_prep_id)
  + (SELECT COUNT(*) FROM tmd_mdl_scnrcasem
      WHERE scnr_id = @rl_scnr_id AND case_stat_cd = 'RUNNING' AND del_yn = 'N')
  + (SELECT COUNT(*) FROM tmd_mdl_scnrcaseh
      WHERE case_id = @rl_case_id AND exec_stat_cd = 'RUNNING' AND del_yn = 'N')
  + (SELECT COUNT(*) FROM tmd_mi_model_execpreph
      WHERE scnr_id = @rl_scnr_id AND exec_prep_id <> @rl_exec_prep_id AND del_yn = 'N'
        AND exec_prep_stat_cd IN ('REQUESTING','PROPOSAL_PENDING','CONFIRMING','PREPARING_EXECUTION'));

-- 화면별 최신 선택 규칙이 다르다. 대상을 최신으로 보이게 하려고 ID/시각을 바꾸지 않는다.
SET @rl_latest_case_ok = IF(NOT EXISTS (
    SELECT 1 FROM tmd_mdl_scnrcasem
    WHERE scnr_id = @rl_scnr_id AND exec_type_cd = 'SCENARIO' AND del_yn = 'N'
      AND case_id > @rl_case_id), 1, 0);
SET @rl_latest_prep_ok = IF(@rl_exec_prep_id = (
    SELECT MAX(exec_prep_id) FROM tmd_mi_model_execpreph WHERE scnr_id = @rl_scnr_id AND del_yn = 'N'), 1, 0);
SET @rl_latest_run_ok = IF(NOT EXISTS (
    SELECT 1 FROM tmd_mdl_scnrcasem c JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id
    WHERE c.scnr_id = @rl_scnr_id AND c.del_yn = 'N' AND c.case_stat_cd = 'COMPLETED'
      AND h.del_yn = 'N' AND h.exec_stat_cd = 'COMPLETED' AND h.run_seq > @rl_run_seq), 1, 0);
-- 기존 해석을 자동 무효화하는 것은 이 초안의 변경 범위 밖. 다른 Run의 해석/생성 중이면 중단.
SET @rl_interp_conflicts = (SELECT COUNT(*) FROM tmm_mi_scenario_prc_interp_m
    WHERE scenario_id = @rl_scnr_id AND del_yn = 'N'
      AND (NOT (src_run_id <=> @rl_run_id) OR status = 'generating'));

SET @rl_today = DATE_FORMAT(CURRENT_DATE(), '%Y%m%d');
SET @rl_input_files_ok = (SELECT IF(COUNT(*) = 4
    AND SUM(LOCATE('DEMAND', UPPER(org_file_nm)) > 0) = 1
    AND SUM(LOCATE('SUPPLY', UPPER(org_file_nm)) > 0) = 1
    AND SUM(LOCATE('TRADE', UPPER(org_file_nm)) > 0) = 1
    AND SUM(LOCATE('OUTPUT', UPPER(org_file_nm)) > 0) = 1
    AND SUM((LOCATE('DEMAND', UPPER(org_file_nm)) > 0)
          + (LOCATE('SUPPLY', UPPER(org_file_nm)) > 0)
          + (LOCATE('TRADE', UPPER(org_file_nm)) > 0)
          + (LOCATE('OUTPUT', UPPER(org_file_nm)) > 0) = 1) = 4
    AND SUM(strg_path_nm IS NOT NULL AND TRIM(strg_path_nm) <> '') = 4, 1, 0)
    FROM tcm_filel WHERE file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = @rl_case_id
      AND del_yn = 'N' AND (file_vald_end_dt IS NULL OR file_vald_end_dt >= @rl_today));

SET @rl_price_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcaseprcl WHERE run_seq = @rl_run_seq AND del_yn = 'N');
SET @rl_volume_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasevolrgnl WHERE run_seq = @rl_run_seq AND del_yn = 'N');
SET @rl_header_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdehdrl WHERE run_seq = @rl_run_seq AND del_yn = 'N');
SET @rl_flow_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdeflwl WHERE run_seq = @rl_run_seq AND del_yn = 'N');
SET @rl_runs_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcaseh WHERE case_id = @rl_case_id);
SET @rl_files_before = (SELECT COUNT(*) FROM tcm_filel
    WHERE (file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = @rl_case_id)
       OR (file_rel_obj_spt_cd IN ('SCNRCASEH','SCNRCASEHI','SCNRCASEHTF') AND file_rel_obj_dstg_no = @rl_run_seq));

SET @rl_ok = IF(@rl_ids_ok = 1 AND @rl_storage_ok = 1 AND @rl_case_ok = 1 AND @rl_link_ok = 1
    AND @rl_conflicts = 0 AND @rl_latest_case_ok = 1 AND @rl_latest_prep_ok = 1
    AND @rl_latest_run_ok = 1 AND @rl_interp_conflicts = 0 AND @rl_input_files_ok = 1
    AND @rl_price_before > 0 AND @rl_shared_proposal_cases = 0, 1, 0);
SELECT @rl_ids_ok AS inputs_expect_1, @rl_storage_ok AS innodb_no_triggers_expect_1,
       @rl_case_ok AS completed_independent_case_expect_1, @rl_link_ok AS existing_proposal_link_expect_1,
       @rl_proposal_id AS proposal_id, @rl_confirm_required AS will_copy_proposal,
       @rl_shared_proposal_cases AS other_cases_affected_expect_0,
       @rl_conflicts AS conflicting_links_or_work_expect_0,
       @rl_latest_case_ok AS latest_case_expect_1, @rl_latest_prep_ok AS latest_prep_expect_1,
       @rl_latest_run_ok AS latest_completed_run_expect_1,
       @rl_interp_conflicts AS incompatible_price_interpretations_expect_0,
       @rl_input_files_ok AS four_usable_input_files_expect_1,
       @rl_run_id AS reused_run_id, @rl_run_seq AS reused_run_seq,
       @rl_price_before AS active_price_rows_expect_positive,
       @rl_volume_before AS volume_rows_info, @rl_header_before AS trade_header_rows_info,
       @rl_flow_before AS trade_flow_rows_info,
       IF(@rl_ok = 1, 'PRECHECK_OK', 'STOP_AND_ROLLBACK') AS decision;

-- 3. UPDATE: Agent 확정과 Case/preparation 연결을 한 statement로 변경한다.
-- 사전조건 불충족이면 변경하지 않는다. InnoDB에서는 이 statement 실패 시 어느 행도 변경되지 않는다.
-- Case/Run 상태와 last_run_id는 유지하고 기존 완료 결과를 사용한다.
SET @rl_now = CURRENT_TIMESTAMP;
UPDATE tmd_mdl_scnrcasem c
JOIN tmd_mi_model_execpreph p ON p.exec_prep_id = @rl_exec_prep_id AND p.scnr_id = @rl_scnr_id
JOIN tmm_mi_model_param_proposal_m a ON a.model_param_proposal_id = p.agnt_prpsl_id
    AND a.model_param_proposal_id = @rl_proposal_id AND a.scenario_id = p.scnr_id
SET c.exec_type_cd = 'SCENARIO', c.scnr_id = @rl_scnr_id,
    c.mod_usr_logn_id = @rl_actor, c.mod_dtm = @rl_now,
    p.case_id = @rl_case_id, p.exec_prep_stat_cd = 'EXECUTING', p.err_msg_cntt = NULL,
    p.mod_usr_logn_id = @rl_actor, p.mod_dtm = @rl_now,
    a.confirmation_parameters = IF(@rl_confirm_required = 1, a.proposal_parameters, a.confirmation_parameters),
    a.status = 'CONFIRMED',
    a.confirmed_dtm = IF(@rl_confirm_required = 1, @rl_now, a.confirmed_dtm),
    a.mod_usr_logn_id = IF(@rl_confirm_required = 1, @rl_actor, a.mod_usr_logn_id),
    a.mod_dtm = IF(@rl_confirm_required = 1, @rl_now, a.mod_dtm)
WHERE @rl_ok = 1 AND c.case_id = @rl_case_id
  AND c.exec_type_cd = 'INDEPENDENT' AND c.del_yn = 'N' AND c.case_stat_cd = 'COMPLETED'
  AND (c.scnr_id IS NULL OR c.scnr_id = @rl_scnr_id) AND c.last_run_id = @rl_run_id
  AND p.del_yn = 'N' AND (p.case_id IS NULL OR p.case_id = @rl_case_id)
  AND p.exec_prep_stat_cd IN ('PROPOSAL_PENDING','EXECUTION_FAILED','EXECUTING')
  AND a.del_yn = 'N' AND JSON_TYPE(a.proposal_parameters) = 'ARRAY'
  AND ((@rl_confirm_required = 1 AND a.status = 'PROPOSED'
        AND a.confirmation_parameters IS NULL AND a.confirmed_dtm IS NULL)
    OR (@rl_confirm_required = 0 AND a.status = 'CONFIRMED'
        AND JSON_TYPE(a.confirmation_parameters) = 'ARRAY'));
-- 이후 조회 결과와 5번 최종 판정으로 실제 데이터 상태를 확인한다.

-- 4. 사후 검증: 연결·상태·각 화면에서 실제 선택되는 ID를 다시 조회.
SELECT c.case_id, c.exec_type_cd, c.scnr_id, c.case_stat_cd, c.last_run_id,
       h.run_id, h.run_seq, h.case_id AS run_case_id, h.exec_stat_cd,
       p.exec_prep_id, p.case_id AS prep_case_id, p.exec_prep_stat_cd,
       p.err_msg_cntt IS NULL AS preparation_error_cleared,
       a.model_param_proposal_id, a.scenario_id AS proposal_scenario_id, a.status AS proposal_status,
       a.confirmed_dtm, JSON_TYPE(a.confirmation_parameters) AS confirmed_json_type,
       SHA2(CAST(a.proposal_parameters AS CHAR), 256) = @rl_proposed_hash_before AS proposal_unchanged_expect_1,
       IF(@rl_confirm_required = 1, SHA2(CAST(a.confirmation_parameters AS CHAR), 256) = @rl_proposed_hash_before,
          SHA2(CAST(a.confirmation_parameters AS CHAR), 256) <=> @rl_confirmed_hash_before) AS confirmation_copied_or_preserved_expect_1,
       CASE h.exec_stat_cd WHEN 'COMPLETED' THEN 'EXECUTED_CONFIRMED' END AS expected_current_ui_state
FROM tmd_mdl_scnrcasem c
JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id AND h.case_id = c.case_id
JOIN tmd_mi_model_execpreph p ON p.case_id = c.case_id AND p.scnr_id = c.scnr_id AND p.del_yn = 'N'
JOIN tmm_mi_model_param_proposal_m a ON a.model_param_proposal_id = p.agnt_prpsl_id AND a.scenario_id = c.scnr_id
WHERE c.case_id = @rl_case_id AND p.exec_prep_id = @rl_exec_prep_id;

SET @rl_selected_case = (SELECT MAX(case_id) FROM tmd_mdl_scnrcasem
    WHERE scnr_id = @rl_scnr_id AND exec_type_cd = 'SCENARIO' AND del_yn = 'N');
SET @rl_selected_prep = (SELECT MAX(exec_prep_id) FROM tmd_mi_model_execpreph
    WHERE scnr_id = @rl_scnr_id AND del_yn = 'N');
SET @rl_selected_run_seq = (SELECT MAX(h.run_seq)
    FROM tmd_mdl_scnrcasem c JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id
    WHERE c.scnr_id = @rl_scnr_id AND c.del_yn = 'N' AND c.case_stat_cd = 'COMPLETED'
      AND h.del_yn = 'N' AND h.exec_stat_cd = 'COMPLETED');

SELECT 'price' AS item, @rl_price_before AS before_count, COUNT(*) AS after_count
FROM tmd_mdl_scnrcaseprcl WHERE run_seq = @rl_run_seq AND del_yn = 'N'
UNION ALL SELECT 'volume', @rl_volume_before, COUNT(*) FROM tmd_mdl_scnrcasevolrgnl WHERE run_seq = @rl_run_seq AND del_yn = 'N'
UNION ALL SELECT 'trade_header', @rl_header_before, COUNT(*) FROM tmd_mdl_scnrcasetrdehdrl WHERE run_seq = @rl_run_seq AND del_yn = 'N'
UNION ALL SELECT 'trade_flow', @rl_flow_before, COUNT(*) FROM tmd_mdl_scnrcasetrdeflwl WHERE run_seq = @rl_run_seq AND del_yn = 'N';

-- 다운로드 연결 확인. 파일 정보는 참조 키/이름/유효 여부만 출력.
SELECT file_no, file_rel_obj_spt_cd, file_rel_obj_dstg_no, org_file_nm, del_yn, file_vald_end_dt
FROM tcm_filel
WHERE (file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = @rl_case_id)
   OR (file_rel_obj_spt_cd IN ('SCNRCASEH','SCNRCASEHI','SCNRCASEHTF') AND file_rel_obj_dstg_no = @rl_run_seq)
ORDER BY file_rel_obj_spt_cd, file_no;

-- 5. 최종 판정. READY는 DB 내 연결 검증이며 실제 HTTP/화면/Excel 값 일치 검증이 아니다.
SET @rl_verified = IF(@rl_ok = 1
    AND @rl_selected_case = @rl_case_id AND @rl_selected_prep = @rl_exec_prep_id
    AND @rl_selected_run_seq = @rl_run_seq
    AND (SELECT COUNT(*)
         FROM tmd_mdl_scnrcasem c
         JOIN tmd_mdl_scnrcaseh h ON h.run_id = c.last_run_id AND h.case_id = c.case_id
         JOIN tmd_mi_model_execpreph p ON p.case_id = c.case_id AND p.scnr_id = c.scnr_id
         JOIN tmm_mi_scenario_m s ON s.scenario_id = c.scnr_id
         JOIN tmm_mi_scenario_group_m g ON g.scenario_group_id = s.scenario_group_id
         JOIN tmm_mi_model_param_proposal_m a ON a.model_param_proposal_id = p.agnt_prpsl_id
         WHERE c.case_id = @rl_case_id AND c.scnr_id = @rl_scnr_id AND c.exec_type_cd = 'SCENARIO'
           AND c.case_stat_cd = 'COMPLETED' AND c.del_yn = 'N'
           AND h.run_id = @rl_run_id AND h.run_seq = @rl_run_seq AND h.exec_stat_cd = 'COMPLETED' AND h.del_yn = 'N'
           AND p.exec_prep_id = @rl_exec_prep_id AND p.exec_prep_stat_cd = 'EXECUTING'
           AND p.del_yn = 'N' AND p.err_msg_cntt IS NULL
           AND a.scenario_id = s.scenario_id AND a.scenario_group_id = s.scenario_group_id
           AND a.status = 'CONFIRMED' AND a.del_yn = 'N'
           AND JSON_TYPE(a.proposal_parameters) = 'ARRAY' AND JSON_TYPE(a.confirmation_parameters) = 'ARRAY'
           AND a.model_param_proposal_id = @rl_proposal_id
           AND SHA2(CAST(a.proposal_parameters AS CHAR), 256) = @rl_proposed_hash_before
           AND ((@rl_confirm_required = 1
                 AND SHA2(CAST(a.confirmation_parameters AS CHAR), 256) = @rl_proposed_hash_before
                 AND a.confirmed_dtm = @rl_now AND a.mod_usr_logn_id = @rl_actor AND a.mod_dtm = @rl_now)
             OR (@rl_confirm_required = 0
                 AND (SHA2(CAST(a.confirmation_parameters AS CHAR), 256) <=> @rl_confirmed_hash_before)
                 AND (a.confirmed_dtm <=> @rl_confirmed_at_before)
                 AND (a.mod_usr_logn_id <=> @rl_agent_actor_before)
                 AND (a.mod_dtm <=> @rl_agent_modified_before)))
           AND s.del_yn = 'N' AND s.status <> 'discarded' AND g.del_yn = 'N'
           AND c.mod_usr_logn_id = @rl_actor AND p.mod_usr_logn_id = @rl_actor
           AND c.mod_dtm = @rl_now AND p.mod_dtm = @rl_now) = 1
    AND @rl_price_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcaseprcl WHERE run_seq = @rl_run_seq AND del_yn = 'N')
    AND @rl_volume_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasevolrgnl WHERE run_seq = @rl_run_seq AND del_yn = 'N')
    AND @rl_header_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdehdrl WHERE run_seq = @rl_run_seq AND del_yn = 'N')
    AND @rl_flow_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdeflwl WHERE run_seq = @rl_run_seq AND del_yn = 'N')
    AND @rl_runs_before = (SELECT COUNT(*) FROM tmd_mdl_scnrcaseh WHERE case_id = @rl_case_id)
    AND @rl_files_before = (SELECT COUNT(*) FROM tcm_filel
        WHERE (file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = @rl_case_id)
           OR (file_rel_obj_spt_cd IN ('SCNRCASEH','SCNRCASEHI','SCNRCASEHTF') AND file_rel_obj_dstg_no = @rl_run_seq)), 1, 0);
SELECT @rl_selected_case AS latest_parameter_case_id,
       @rl_selected_prep AS current_proposal_exec_prep_id,
       @rl_selected_run_seq AS graph_latest_completed_run_seq,
       @rl_verified AS verified_expect_1,
       IF(@rl_verified = 1, 'READY_TO_COMMIT', 'STOP_AND_ROLLBACK') AS decision;

-- 6. 기본 종료는 취소. 실제 반영 시에는 위 결과 검토 후 이 줄 대신 COMMIT을 별도 실행.
ROLLBACK;
SELECT 'ROLLED_BACK_NO_CHANGES_COMMITTED' AS final_action;
