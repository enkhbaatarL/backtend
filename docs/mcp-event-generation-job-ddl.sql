-- [MCP Event Generate] 이벤트 생성 비동기 작업 상태 테이블 (feature spec 4.8, design D-12)
--
-- 배경: 이벤트 생성 서버(POST /events/generations)는 동기 호출이고 최대 15분 걸린다. MCP 채널은 접수(jobId)만 즉시 응답하고
--       B.E 가 백그라운드에서 생성 서버를 호출한 뒤 결과를 이 테이블에 남긴다. MCP client 는 jobId 로 상태·결과를 조회한다.
-- 기존 테이블 재사용 검토: tmm_mi_scenario_group_operation_m 은 Agent 소유이고 scenario_group FK·operation_type CHECK 가 있어
--       쓰려면 제약을 바꿔야 한다. tcm_job_runs 는 Airflow 배치용이다. 그래서 새 테이블로 둔다 (컬럼 관례는 operation_m 을 따름).
--
-- 배포 순서 (ddl-auto: validate 라 엔티티가 테이블을 요구함):
--   1) 이 DDL 을 backend 가 접속하는 모든 DB 에 적용 (전용 backend 와 같은 DB 를 쓰는 일반 backend 포함)
--   2) backend 배포
--   - 테이블만 먼저 생기는 것은 현재 backend 에 영향 없음 (아무도 읽지 않음)
--   - 테이블 없이 새 backend 를 띄우면 기동 실패 (schema validation)
--
-- 롤백: backend 를 이전 버전으로 되돌린 뒤 DROP (아래). 진행 중 작업 기록은 사라진다 — 생성된 이벤트(tmm_mi_mievent_m)에는 영향 없음.

CREATE TABLE `tmd_mcp_job_mi_event_generation_m` (
  `event_generation_job_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '이벤트 생성 작업 ID',
  `usr_no` int unsigned NOT NULL COMMENT '요청 사용자번호 (작업 조회 권한 기준)',
  `user_query` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '사용자 입력 문장 (원문)',
  `date_from` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '근거 조회 시작일(YYYY-MM-DD)',
  `date_to` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '근거 조회 종료일(YYYY-MM-DD)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '작업 상태 (RUNNING/SUCCEEDED/FAILED/INTERRUPTED)',
  `result_payload` json DEFAULT NULL COMMENT '생성 서버 응답 data 원문 (SUCCEEDED 일 때)',
  `error_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '실패 코드 (ResultCode)',
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '실패 메시지',
  `started_at` datetime(6) NOT NULL COMMENT '작업 시작 일시',
  `completed_at` datetime(6) DEFAULT NULL COMMENT '작업 종료 일시',
  `heartbeat_at` datetime(6) NOT NULL COMMENT '실행 중 인스턴스의 마지막 heartbeat 일시',
  `crt_usr_logn_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'SYSTEM' COMMENT '생성사용자로그인ID',
  `crt_dtm` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
  `mod_usr_logn_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'SYSTEM' COMMENT '수정사용자로그인ID',
  `mod_dtm` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
  `del_yn` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'N' COMMENT '삭제여부',
  PRIMARY KEY (`event_generation_job_id`),
  KEY `tmd_mcp_job_mi_event_generation_m_idx01` (`usr_no`, `event_generation_job_id`),
  KEY `tmd_mcp_job_mi_event_generation_m_idx02` (`status`, `heartbeat_at`),
  CONSTRAINT `tmd_mcp_job_mi_event_generation_m_fk01` FOREIGN KEY (`usr_no`) REFERENCES `tmd_usrm` (`usr_no`),
  CONSTRAINT `tmd_mcp_job_mi_event_generation_m_ck01` CHECK ((`status` in (_utf8mb4'RUNNING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'INTERRUPTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='MCP 이벤트 생성 비동기 작업 상태';

-- 롤백
-- DROP TABLE `tmd_mcp_job_mi_event_generation_m`;
