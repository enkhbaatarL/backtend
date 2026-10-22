-- Document Control
-- status: Draft / MANUAL APPLY ONLY; owner: BE / DBA; last synced: 2026-10-22
-- canonical for: Reference only — DDL 적용 후 번호순 실행본의 공통 세션 입력
-- source: ../model-input-catalog-install-registration.sql 환경 guard; README.md
-- 같은 DB 연결에서 00 → 01 → 02 → 03 순서로 실행한다. 진행 중 transaction이 없는 상태에서만 00을 실행한다.
SET NAMES utf8mb4;
SELECT @@GLOBAL.server_uuid AS server_uuid,DATABASE() AS schema_name;
-- 위 조회와 접속 대상을 대조한 뒤 아래 네 NULL 줄을 실행 복사본에서 수정한다.
SET @ci_environment=NULL; -- 개발계 'DEVELOPMENT', 운영계 'PRODUCTION'.
SET @ci_expected_server_uuid=NULL; -- 확인한 server_uuid 문자열.
SET @ci_expected_schema=NULL; -- 확인한 schema_name 문자열. 예: 'midb'.
SET @ci_actor=NULL; -- 실제 적용자 로그인 ID. 예: 'release.operator'. 비밀번호 아님.
SET @ci_expected_template_id=NULL,@ci_reader_ready=0;
SELECT IF(COALESCE(BINARY @ci_environment IN ('DEVELOPMENT','PRODUCTION'),0)=1
 AND COALESCE(BINARY @@GLOBAL.server_uuid=BINARY @ci_expected_server_uuid,0)=1
 AND COALESCE(BINARY DATABASE()=BINARY @ci_expected_schema,0)=1
 AND @@GLOBAL.read_only=0 AND @@GLOBAL.super_read_only=0 AND @@SESSION.foreign_key_checks=1
 AND @ci_actor IS NOT NULL AND REGEXP_LIKE(@ci_actor,'^[a-zA-Z0-9._@-]{1,50}$','c'),'SESSION_READY','STOP') AS session_decision,
 @ci_environment AS environment,DATABASE() AS selected_schema,@@GLOBAL.server_uuid AS server_uuid;
-- SESSION_READY일 때만 01 진행. 새 연결/재접속/환경 변경 시 이 파일을 다시 입력·실행한다.
