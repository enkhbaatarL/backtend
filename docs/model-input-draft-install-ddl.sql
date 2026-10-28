-- Model Input Draft API — 개발계·운영계 공통 최초 설치 (현재 코드 최종 구조)
-- Document Control
-- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED
-- canonical for: Schema rollout — 개발계·운영계 최초 설치 CREATE/초기화/pre-check/verify/보존 복구 SQL
-- owner: BE / DBA; date / last synced: 2026-10-22
-- source documents: model-input-draft-read-ddl-draft.sql, model-input-draft-change-request-ddl-draft.sql,
--   model-input-draft-finalization-ddl-draft.sql, model-input-draft-column-v2-ddl-draft.sql,
--   model-input-audit-precision-ddl-draft.sql, model-input-incremental-upload-ddl-draft.sql,
--   model-input-multi-upload-ddl-draft.sql, model-input-upload-deadline-ddl-draft.sql,
--   model-input-shared-baseline-ddl-draft.sql, model-input-web-runner-ddl-draft.sql
-- depends on: design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md sections 6.3–6.12;
--   design-docs/gas/mi-model-input-template-catalog-domain-model-design.md (업무/수명 규칙);
--   feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md (API 계약)
-- supersedes: 위 증분 파일의 신규 환경 설치 순서 및 이전 파일명 model-input-draft-dev-install-ddl.sql.
--   기존 설치 DB의 증분 migration은 대체하지 않음.
-- code evidence: feature/gas2 HEAD b3557576340a126e9cf46990903409d0b49b5c02 + 작성 당시 working tree.
--   ModelInputDraftRepository / InputUploadRepository / InputDraftFinalizationRepository / InputDraftRunnerRepository.
-- Open Questions: 개발계·운영계 각각의 실제 master 서버/schema/부모 타입/권한/용량/버전/배포 SHA,
--   utf8mb4 기본 collation 일치와 MySQL 실행 결과 미확인.
--
-- 사용법: 1 PRE-CHECK → 수동 검토 → 2 CREATE 블록 내용 선택 실행 → 3 INITIALIZE → 4 VERIFY.
-- 전체 파일 일괄 실행 금지. 변경 SQL은 MANUAL 블록 안에만 있다. 첫 오류에 즉시 중단한다.
-- 새 전용 master 연결에서 실행하고, 이전 transaction이 없는지 확인한다. tenant DB에 적용하지 않는다.
-- 개발계에서 먼저 검증한 동일 파일을 운영계에 사용한다. 파일 SHA-256과 적용 결과는 환경별로 보관한다.
-- 환경별 서버/접속 endpoint·schema·운영자·백업·적용 결과는 독립 확인한다. 개발계 입력/결과를 복사하지 않는다.
-- 아래 신규 19개가 전부 없는 환경 전용이다. 하나라도 있으면 STOP 후 SHOW CREATE 대조/별도 차이 SQL 준비.
-- IF NOT EXISTS/기존 DROP·RENAME/테스트 Draft reset/데이터 backfill/FK 검사 해제는 사용하지 않는다.
-- MySQL DDL은 암묵 commit된다. CREATE 중 일부 실패를 ROLLBACK으로 되돌릴 수 없다.
-- 작업 전 부모 테이블 정의·데이터의 복구 가능한 백업 및 앱 계정 권한을 DBA가 확인한다.
-- 최초 설치에도 관련 web/runner/worker/외부 시험 writer를 중지하고 유지보수 중 새 요청을 차단한다.
-- 이 SQL은 catalog 6 + 공유 기준 3 + Draft 5 + 요청 1 + 업로드 2 + 최종저장 준비 1 + 실행기 1을 생성한다.
-- 현재 형식: 요청 json_rvsn_no=2, 감사 DATETIME(0), 실행/기한 DATETIME(6), 업로드 다중 파일,
--   UPLOAD 600초 허용(기존 120초도 허용), CHANGE/FINALIZE 120초, NULL base_id 호환.
-- 기존 tmm_mdlinpt... 제약/인덱스 이름은 정본과의 호환을 위해 그대로 보존한다.
--
-- 1. PRE-CHECK — 조회/세션 변수만 사용. 환경별 새 연결에서 입력값을 따로 지정한다.
-- 입력 순서:
-- ① DB 클라이언트에서 적용할 환경(개발계 또는 운영계)의 쓰기 DB에 연결하고 대상 DB를 선택한다.
-- ② 다음 SELECT만 먼저 실행해 server_uuid(서버 고유 ID)와 schema_name(DB 이름)을 확인한다.
SELECT @@GLOBAL.server_uuid AS server_uuid, DATABASE() AS schema_name;
--    schema_name이 NULL이면 DB 클라이언트에서 대상 DB를 선택한 뒤 다시 조회한다.
-- ③ 연결 정보가 적용할 환경의 쓰기 DB인지 확인한 뒤 아래 네 SET의 NULL을 실제 값으로 바꾸어 실행한다.
--    조회 결과만으로 개발계/운영계를 알 수 없으므로 DB 클라이언트의 접속 대상도 함께 확인한다.
--    operator는 작업 이력에 남길 실제 적용자의 로그인 ID다. 자동 생성하지 않으며 비밀번호를 넣지 않는다.
-- 작성 예시(아래는 주석이며 실제 운영값이 아님):
-- SET @mi_install_environment = 'DEVELOPMENT';
-- SET @mi_install_expected_server_uuid = '<위에서 조회하고 대상 환경과 대조한 server_uuid>';
-- SET @mi_install_expected_schema = 'midb'; -- schema_name 조회 결과가 midb인 경우에만 사용.
-- SET @mi_install_operator = 'release.operator'; -- 실제 적용자의 로그인 ID로 교체.
-- 운영계는 environment를 'PRODUCTION'으로 지정하고 UUID/schema도 운영계 조회 결과를 사용한다.
SET @mi_install_environment = NULL; -- 'DEVELOPMENT' 또는 'PRODUCTION': 작업 기록용 표식이며 실제 환경 증거는 아님.
SET @mi_install_expected_server_uuid = NULL; -- DBA가 대상 환경의 쓰기 서버로 확인한 server_uuid.
SET @mi_install_expected_schema = NULL; -- 위 SELECT의 schema_name, 즉 이번에 테이블을 생성할 DB 이름.
SET @mi_install_operator = NULL; -- 작업 이력에 기록할 실제 적용자 로그인 ID. 예: 'release.operator'.
-- 같은 schema 이름이 양 환경에 있을 수 있으므로 서버도 확인한다. UUID는 조회값을 확인 없이 복사하지 않는다.
-- @mi_install_operator: 승인된 운영자 로그인 ID (자격증명 아님), 영문/숫자/._@- 1~50자.
SELECT @mi_install_environment AS deployment_environment,
       @@GLOBAL.server_uuid AS actual_server_uuid, @mi_install_expected_server_uuid AS expected_server_uuid,
       DATABASE() AS actual_master_schema, @mi_install_expected_schema AS expected_master_schema,
       VERSION() AS mysql_version, @@version_comment AS distribution,
       @@GLOBAL.read_only AS server_read_only, @@GLOBAL.super_read_only AS server_super_read_only,
       @@foreign_key_checks AS fk_checks, @@sql_mode AS sql_mode,
       @@max_allowed_packet AS max_allowed_packet, @@transaction_isolation AS session_isolation,
       @@session.time_zone AS session_time_zone, @@system_time_zone AS system_time_zone,
       @@character_set_connection AS connection_charset, @@collation_database AS database_collation,
       @@session.default_collation_for_utf8mb4 AS default_utf8mb4_collation;
-- MySQL 8.0.16+ / InnoDB / CHECK ENFORCED / FK=1, strict sql_mode와 utf8mb4 연결을 확인한다.
-- MySQL 8.4+ 등 다른 버전은 해당 버전에서 별도 호환 검증한다. MariaDB는 이 검증의 대상이 아니다.
-- 부모 ID: case_id/usr_no INT UNSIGNED, cust_cpny_no MEDIUMINT UNSIGNED, file_no BIGINT UNSIGNED.
-- 부모 PK/unique와 실제 engine은 SHOW CREATE로 확인한다. packet은 성공 delta 최대16MiB + protocol 여유 필요.
-- 이 연결의 isolation/시간대는 앱 연결의 증거가 아니다. 앱 master READ_COMMITTED/시간대를 별도 확인한다.
-- 최초 적용 전에 개발계/운영계 버전·부모 정의·sql_mode·utf8mb4 기본 collation을 대조한다.
-- CREATE는 정본대로 DEFAULT CHARSET=utf8mb4를 사용한다. 기본 collation 차이는 문자 비교/UK 의미를 바꿀 수 있다.
-- 차이가 있으면 그대로 적용하거나 서버 설정을 바꾸지 말고 호환성/정렬 규칙을 먼저 확정한다.
-- 읽기 replica/접속 대상 불일치/대상 서버 변경(예: failover)은 STOP 후 쓰기 서버 확인부터 다시 진행한다.
SELECT table_name,engine,table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_mdl_scnrcasem','tcm_filel','tmd_usrm','tmd_custcpnym') ORDER BY table_name;
SHOW CREATE TABLE tmd_mdl_scnrcasem;
SHOW CREATE TABLE tcm_filel;
SHOW CREATE TABLE tmd_usrm;
SHOW CREATE TABLE tmd_custcpnym;

-- 기대 0행: 신규 이름 또는 이전 tmm_ 이름이 있으면 최초 설치를 진행하지 않는다.
SELECT table_name,engine FROM information_schema.tables
WHERE table_schema=DATABASE() AND (table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm')
 OR LEFT(table_name,11)='tmm_mdlinpt') ORDER BY table_name;
SELECT IF(COALESCE(BINARY @mi_install_environment IN ('DEVELOPMENT','PRODUCTION'),0)=1
 AND COALESCE(BINARY @@GLOBAL.server_uuid=BINARY @mi_install_expected_server_uuid,0)=1
 AND COALESCE(BINARY DATABASE()=BINARY @mi_install_expected_schema,0)=1
 AND @@GLOBAL.read_only=0 AND @@GLOBAL.super_read_only=0
 AND @@foreign_key_checks=1
 AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name IN ('tmd_mdl_scnrcasem','tcm_filel','tmd_usrm','tmd_custcpnym')
      AND engine='InnoDB')=4
 AND NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND (table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm') OR LEFT(table_name,11)='tmm_mdlinpt')),
 'REVIEW_PARENT_TYPES_VERSION_PRIVILEGES_THEN_APPLY','STOP') AS precheck_decision;
-- 위 decision은 타입/버전/CHECK/권한/backup/환경 간 collation 차이/외부 실행 종료를 자동 검증하지 않는다.
-- 서버/schema 불일치/테이블 존재/부모 타입·index 불일치/권한 부족이면 STOP. 무시하고 변수를 덮어쓰지 않는다.

-- 2. APPLY — 위 사전 검토 완료 후 아래 MANUAL_CREATE 내부 19개 CREATE만 순서대로 선택 실행.
/* MANUAL_CREATE_BEGIN
-- 01/19 tmd_mdl_inpttmpltm
CREATE TABLE tmd_mdl_inpttmpltm
(
    tmplt_id            INT UNSIGNED AUTO_INCREMENT COMMENT '입력템플릿ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    mdl_spt_cd          VARCHAR(20) NOT NULL COMMENT '모델구분코드',
    tmplt_ver_no        SMALLINT UNSIGNED NOT NULL COMMENT '템플릿버전번호',
    tmplt_ver_nm        VARCHAR(100) NOT NULL COMMENT '템플릿버전명',
    tmplt_stat_cd       VARCHAR(20) DEFAULT 'DRAFT' NOT NULL
                            COMMENT '템플릿상태코드 (DRAFT/ACTIVE/DEPRECATED)',
    aply_bgn_dtm        DATETIME(6) NULL COMMENT '적용시작일시',
    tmplt_expn          TEXT NULL COMMENT '템플릿설명',
    actv_mdl_spt_cd     VARCHAR(20) AS
                            (IF(del_yn = 'N' AND tmplt_stat_cd = 'ACTIVE', mdl_spt_cd, NULL)) STORED
                            COMMENT '활성모델구분코드',
    CONSTRAINT tmm_mdlinpttmpltm_ux01 UNIQUE (mdl_spt_cd, tmplt_ver_no),
    CONSTRAINT tmm_mdlinpttmpltm_ux02 UNIQUE (actv_mdl_spt_cd),
    CONSTRAINT tmm_mdlinpttmpltm_ck01 CHECK (tmplt_stat_cd IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    CONSTRAINT tmm_mdlinpttmpltm_ck02 CHECK (del_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMM_모델입력템플릿마스터';

-- 02/19 tmd_mdl_inptfiled
CREATE TABLE tmd_mdl_inptfiled
(
    inpt_file_id        INT UNSIGNED AUTO_INCREMENT COMMENT '입력파일ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    tmplt_id            INT UNSIGNED NOT NULL COMMENT '입력템플릿ID',
    mdl_file_cd         VARCHAR(20) NOT NULL COMMENT '모델파일코드 (DEMAND/SUPPLY/TRADE/OUTPUT)',
    file_nm             VARCHAR(255) NOT NULL COMMENT '기준파일명',
    file_ord            SMALLINT UNSIGNED NOT NULL COMMENT '파일순서',
    file_type_cd        VARCHAR(20) NOT NULL COMMENT '파일유형코드 (XLSX/XLSM)',
    file_strt_hash_val  CHAR(64) NULL COMMENT '파일구조해시값',
    CONSTRAINT tmm_mdlinptfiled_ux01 UNIQUE (tmplt_id, mdl_file_cd),
    CONSTRAINT tmm_mdlinptfiled_ux02 UNIQUE (tmplt_id, file_ord),
    CONSTRAINT tmm_mdlinptfiled_fk01 FOREIGN KEY (tmplt_id)
        REFERENCES tmd_mdl_inpttmpltm (tmplt_id),
    CONSTRAINT tmm_mdlinptfiled_ck01 CHECK (mdl_file_cd IN ('DEMAND', 'SUPPLY', 'TRADE', 'OUTPUT')),
    CONSTRAINT tmm_mdlinptfiled_ck02 CHECK (file_type_cd IN ('XLSX', 'XLSM')),
    CONSTRAINT tmm_mdlinptfiled_ck03 CHECK (del_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMM_모델입력파일상세';
CREATE INDEX tmm_mdlinptfiled_ix01 ON tmd_mdl_inptfiled (file_strt_hash_val, mdl_file_cd, del_yn);

-- 03/19 tmd_mdl_inptshtd
CREATE TABLE tmd_mdl_inptshtd
(
    inpt_sht_id         INT UNSIGNED AUTO_INCREMENT COMMENT '입력시트ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_file_id        INT UNSIGNED NOT NULL COMMENT '입력파일ID',
    sht_nm              VARCHAR(150) NOT NULL COMMENT '시트명',
    sht_ord             SMALLINT UNSIGNED NOT NULL COMMENT '시트순서',
    sht_stat_cd         VARCHAR(20) DEFAULT 'VISIBLE' NOT NULL
                            COMMENT '시트상태코드 (VISIBLE/HIDDEN/VERY_HIDDEN)',
    CONSTRAINT tmm_mdlinptshtd_ux01 UNIQUE (inpt_file_id, sht_nm),
    CONSTRAINT tmm_mdlinptshtd_ux02 UNIQUE (inpt_file_id, sht_ord),
    CONSTRAINT tmm_mdlinptshtd_fk01 FOREIGN KEY (inpt_file_id)
        REFERENCES tmd_mdl_inptfiled (inpt_file_id),
    CONSTRAINT tmm_mdlinptshtd_ck01 CHECK (sht_stat_cd IN ('VISIBLE', 'HIDDEN', 'VERY_HIDDEN')),
    CONSTRAINT tmm_mdlinptshtd_ck02 CHECK (del_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMM_모델입력시트상세';

-- 04/19 tmd_mdl_inpttbld
CREATE TABLE tmd_mdl_inpttbld
(
    inpt_tbl_id         INT UNSIGNED AUTO_INCREMENT COMMENT '입력테이블ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_sht_id         INT UNSIGNED NOT NULL COMMENT '입력시트ID',
    inpt_tbl_nm         VARCHAR(100) NOT NULL COMMENT '입력테이블명',
    tbl_ord             SMALLINT UNSIGNED NOT NULL COMMENT '테이블순서',
    tbl_bgn_row_no      INT UNSIGNED NOT NULL COMMENT '테이블시작행번호',
    tbl_end_row_no      INT UNSIGNED NOT NULL COMMENT '테이블종료행번호',
    tbl_bgn_col_no      SMALLINT UNSIGNED NOT NULL COMMENT '테이블시작열번호',
    tbl_end_col_no      SMALLINT UNSIGNED NOT NULL COMMENT '테이블종료열번호',
    hdr_bgn_row_no      INT UNSIGNED NOT NULL COMMENT '헤더시작행번호',
    hdr_end_row_no      INT UNSIGNED NOT NULL COMMENT '헤더종료행번호',
    data_bgn_row_no     INT UNSIGNED NOT NULL COMMENT '데이터시작행번호',
    row_add_psbl_yn     CHAR(1) DEFAULT 'N' NOT NULL COMMENT '행추가가능여부',
    col_add_psbl_yn     CHAR(1) DEFAULT 'N' NOT NULL COMMENT '열추가가능여부',
    inpt_tbl_expn       VARCHAR(500) NULL COMMENT '입력테이블설명',
    qry_json LONGTEXT NULL COMMENT '버전1 검증된 표 조회 정의 JSON',
    CONSTRAINT tmm_mdlinpttbld_ux01 UNIQUE (inpt_sht_id, inpt_tbl_nm),
    CONSTRAINT tmm_mdlinpttbld_ux02 UNIQUE (inpt_sht_id, tbl_ord),
    CONSTRAINT tmm_mdlinpttbld_fk01 FOREIGN KEY (inpt_sht_id)
        REFERENCES tmd_mdl_inptshtd (inpt_sht_id),
    CONSTRAINT tmm_mdlinpttbld_ck01 CHECK
    (tbl_bgn_row_no BETWEEN 1 AND 1048576 AND tbl_end_row_no BETWEEN tbl_bgn_row_no AND 1048576
     AND ((hdr_bgn_row_no=0 AND hdr_end_row_no=0)
          OR (hdr_bgn_row_no>=tbl_bgn_row_no AND hdr_end_row_no>=hdr_bgn_row_no AND hdr_end_row_no<=tbl_end_row_no))
     AND (data_bgn_row_no=0 OR data_bgn_row_no BETWEEN tbl_bgn_row_no AND tbl_end_row_no)),
    CONSTRAINT tmm_mdlinpttbld_ck02 CHECK
        (tbl_bgn_col_no >= 1 AND tbl_bgn_col_no <= tbl_end_col_no AND tbl_end_col_no <= 16384),
    CONSTRAINT tmm_mdlinpttbld_ck03 CHECK (row_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck04 CHECK (col_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck05 CHECK (del_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck06 CHECK (qry_json IS NULL OR (JSON_VALID(qry_json) AND OCTET_LENGTH(qry_json)<=1048576))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMM_모델입력테이블상세';

-- 05/19 tmd_mdl_inptmodscopd
CREATE TABLE tmd_mdl_inptmodscopd
(
    mod_scop_id         INT UNSIGNED AUTO_INCREMENT COMMENT '수정범위ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_tbl_id         INT UNSIGNED NOT NULL COMMENT '입력테이블ID',
    mod_scop_ord        SMALLINT UNSIGNED NOT NULL COMMENT '수정범위순서',
    bgn_row_no          INT UNSIGNED NOT NULL COMMENT '시작행번호',
    end_row_no          INT UNSIGNED NOT NULL COMMENT '종료행번호',
    bgn_col_no          SMALLINT UNSIGNED NOT NULL COMMENT '시작열번호',
    end_col_no          SMALLINT UNSIGNED NOT NULL COMMENT '종료열번호',
    CONSTRAINT tmm_mdlinptmodscopd_ux01 UNIQUE (inpt_tbl_id, mod_scop_ord),
    CONSTRAINT tmm_mdlinptmodscopd_ux02 UNIQUE
        (inpt_tbl_id, bgn_row_no, end_row_no, bgn_col_no, end_col_no),
    CONSTRAINT tmm_mdlinptmodscopd_fk01 FOREIGN KEY (inpt_tbl_id)
        REFERENCES tmd_mdl_inpttbld (inpt_tbl_id),
    CONSTRAINT tmm_mdlinptmodscopd_ck01 CHECK
        (bgn_row_no >= 1 AND bgn_row_no <= end_row_no AND end_row_no <= 1048576),
    CONSTRAINT tmm_mdlinptmodscopd_ck02 CHECK
        (bgn_col_no >= 1 AND bgn_col_no <= end_col_no AND end_col_no <= 16384),
    CONSTRAINT tmm_mdlinptmodscopd_ck03 CHECK (del_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMM_모델입력수정범위상세';

-- 06/19 tmd_mdl_scnrcasetmpltd
CREATE TABLE tmd_mdl_scnrcasetmpltd
(
    case_id             INT UNSIGNED COMMENT '시나리오케이스ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    tmplt_id            INT UNSIGNED NOT NULL COMMENT '입력템플릿ID',
    CONSTRAINT tmd_mdl_scnrcasetmpltd_fk01 FOREIGN KEY (case_id)
        REFERENCES tmd_mdl_scnrcasem (case_id),
    CONSTRAINT tmd_mdl_scnrcasetmpltd_fk02 FOREIGN KEY (tmplt_id)
        REFERENCES tmd_mdl_inpttmpltm (tmplt_id),
    CONSTRAINT tmd_mdl_scnrcasetmpltd_ck01 CHECK (del_yn IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'TMD_시나리오케이스입력템플릿상세';
CREATE INDEX tmd_mdl_scnrcasetmpltd_ix01 ON tmd_mdl_scnrcasetmpltd (tmplt_id, del_yn);

-- 07/19 tmd_mdl_inptbasem
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

-- 08/19 tmd_mdl_inptbasefiled
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

-- 09/19 tmd_mdl_inptbaserowd
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

-- 10/19 tmd_mdl_inptdrftm
CREATE TABLE tmd_mdl_inptdrftm
(
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    usr_no INT UNSIGNED NOT NULL,
    src_spt_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'CASE',
    org_case_id INT UNSIGNED NULL,
    tmplt_id INT UNSIGNED NOT NULL,
    org_rvsn_val VARCHAR(68) CHARACTER SET ascii COLLATE ascii_bin NULL,
    drft_stat_cd VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    rvsn_no BIGINT UNSIGNED NOT NULL DEFAULT 0,
    actv_prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    fnsv_case_id INT UNSIGNED NULL,
    actv_org_case_id INT UNSIGNED GENERATED ALWAYS AS
        (IF(del_yn='N' AND drft_stat_cd='DRAFT',org_case_id,NULL)) STORED,
    actv_upld_usr_no INT UNSIGNED GENERATED ALWAYS AS
        (IF(src_spt_cd='UPLOAD' AND drft_stat_cd='DRAFT' AND del_yn='N',usr_no,NULL)) STORED,
    base_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
    CONSTRAINT tmm_mdlinptdrftm_ux01 UNIQUE(cust_cpny_no,usr_no,actv_org_case_id),
    CONSTRAINT tmm_mdlinptdrftm_ux02 UNIQUE(fnsv_case_id),
    CONSTRAINT tmm_mdlinptdrftm_fk01 FOREIGN KEY(org_case_id) REFERENCES tmd_mdl_scnrcasem(case_id),
    CONSTRAINT tmm_mdlinptdrftm_fk02 FOREIGN KEY(tmplt_id) REFERENCES tmd_mdl_inpttmpltm(tmplt_id),
    CONSTRAINT tmm_mdlinptdrftm_fk03 FOREIGN KEY(cust_cpny_no) REFERENCES tmd_custcpnym(cust_cpny_no),
    CONSTRAINT tmm_mdlinptdrftm_fk04 FOREIGN KEY(usr_no) REFERENCES tmd_usrm(usr_no),
    CONSTRAINT tmm_mdlinptdrftm_fk05 FOREIGN KEY(fnsv_case_id) REFERENCES tmd_mdl_scnrcasem(case_id),
    CONSTRAINT tmm_mdlinptdrftm_ck01 CHECK(drft_stat_cd IN ('DRAFT','FINALIZED','DISCARDED')),
    CONSTRAINT tmm_mdlinptdrftm_ck02 CHECK(rvsn_no<=9007199254740991),
    CONSTRAINT tmm_mdlinptdrftm_ck03 CHECK((drft_stat_cd='FINALIZED')=(fnsv_case_id IS NOT NULL)),
    CONSTRAINT tmm_mdlinptdrftm_ck04 CHECK(drft_stat_cd='DRAFT' OR actv_prcs_id IS NULL),
    CONSTRAINT tmm_mdlinptdrftm_ck05 CHECK(del_yn IN ('Y','N')),
    CONSTRAINT tmd_mdl_inptdrftm_ux03 UNIQUE(cust_cpny_no,usr_no,drft_id),
    CONSTRAINT tmd_mdl_inptdrftm_ux04 UNIQUE(cust_cpny_no,actv_upld_usr_no),
    INDEX tmd_mdl_inptdrftm_ix10(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val),
    CONSTRAINT tmd_mdl_inptdrftm_ck06 CHECK(
        (src_spt_cd='CASE' AND org_case_id IS NOT NULL AND org_rvsn_val IS NOT NULL)
        OR (src_spt_cd='UPLOAD' AND org_case_id IS NULL AND org_rvsn_val IS NULL)),
    CONSTRAINT tmd_mdl_inptdrftm_ck10 CHECK(base_id IS NULL OR src_spt_cd='CASE'),
    CONSTRAINT tmd_mdl_inptdrftm_fk10
        FOREIGN KEY(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)
        REFERENCES tmd_mdl_inptbasem(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11/19 tmd_mdl_inptdrftprcsl
CREATE TABLE tmd_mdl_inptdrftprcsl
(
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    cust_cpny_no MEDIUMINT UNSIGNED NOT NULL,
    usr_no INT UNSIGNED NOT NULL,
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    req_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    prcs_type_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    usr_role_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    base_rvsn_no BIGINT UNSIGNED NULL,
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
    crt_req_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
        (IF(prcs_type_cd='UPLOAD_CREATE',req_id,NULL)) STORED,
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux01 UNIQUE(drft_id,req_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux02 UNIQUE(actv_drft_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux03 UNIQUE(drft_id,prcs_id),
    INDEX tmd_mdl_inptdrftprcsl_ix01(prcs_stat_cd,expr_dtm),
    CONSTRAINT tmd_mdl_inptdrftprcsl_fk01 FOREIGN KEY(cust_cpny_no,usr_no,drft_id)
        REFERENCES tmd_mdl_inptdrftm(cust_cpny_no,usr_no,drft_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck01 CHECK(prcs_type_cd IN ('CHANGE','FINALIZE','UPLOAD_CREATE','UPLOAD_FILE')),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck02 CHECK(prcs_stat_cd IN ('QUEUED','RUNNING','SUCCEEDED','FAILED')),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck03 CHECK(
        json_rvsn_no=2 AND (base_rvsn_no IS NULL OR base_rvsn_no<=9007199254740991)),
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
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck09 CHECK(
        expr_dtm=TIMESTAMPADD(SECOND,120,crt_dtm)
        OR (prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')
            AND expr_dtm=TIMESTAMPADD(SECOND,600,crt_dtm))),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux04 UNIQUE(cust_cpny_no,usr_no,crt_req_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_ux05 UNIQUE(cust_cpny_no,usr_no,prcs_id),
    CONSTRAINT tmd_mdl_inptdrftprcsl_fk02 FOREIGN KEY(cust_cpny_no)
        REFERENCES tmd_custcpnym(cust_cpny_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftprcsl_fk03 FOREIGN KEY(usr_no)
        REFERENCES tmd_usrm(usr_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftprcsl_ck10 CHECK(
        (prcs_type_cd='UPLOAD_CREATE' AND base_rvsn_no IS NULL
            AND ((prcs_stat_cd='SUCCEEDED' AND drft_id IS NOT NULL)
                OR (prcs_stat_cd<>'SUCCEEDED' AND drft_id IS NULL)))
        OR (prcs_type_cd<>'UPLOAD_CREATE' AND drft_id IS NOT NULL AND base_rvsn_no IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12/19 tmd_mdl_inptdrftrvsnl
CREATE TABLE tmd_mdl_inptdrftrvsnl
(
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rvsn_no BIGINT UNSIGNED NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    json_rvsn_no SMALLINT UNSIGNED NOT NULL,
    base_rvsn_no BIGINT UNSIGNED NULL,
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    dlta_json LONGTEXT NULL,
    PRIMARY KEY(drft_id,rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrvsnl_ux01 UNIQUE(prcs_id),
    CONSTRAINT tmm_mdlinptdrftrvsnl_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmm_mdlinptdrftrvsnl_fk02 FOREIGN KEY(drft_id,base_rvsn_no) REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrvsnl_ck01 CHECK(rvsn_no<=9007199254740991),
    CONSTRAINT tmm_mdlinptdrftrvsnl_ck02 CHECK((rvsn_no=0 AND base_rvsn_no IS NULL AND prcs_id IS NULL AND dlta_json IS NULL)
        OR (rvsn_no>0 AND base_rvsn_no IS NOT NULL AND base_rvsn_no=rvsn_no-1 AND prcs_id IS NOT NULL AND dlta_json IS NOT NULL)),
    CONSTRAINT tmm_mdlinptdrftrvsnl_ck03 CHECK(dlta_json IS NULL OR (JSON_VALID(dlta_json) AND OCTET_LENGTH(dlta_json)<=16777216)),
    CONSTRAINT tmm_mdlinptdrftrvsnl_ck04 CHECK(del_yn IN ('Y','N')),
    CONSTRAINT tmd_mdl_inptdrftrvsnl_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13/19 tmd_mdl_inptupldfiled
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
    INDEX tmd_mdl_inptupldfiled_ix02(prcs_id),
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

-- 14/19 tmd_mdl_inptupldactvm
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

-- 15/19 tmd_mdl_inptdrftfiled
CREATE TABLE tmd_mdl_inptdrftfiled
(
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    mdl_file_cd VARCHAR(20) NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    file_no BIGINT UNSIGNED NULL,
    upld_file_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    PRIMARY KEY(drft_id,mdl_file_cd),
    CONSTRAINT tmm_mdlinptdrftfiled_ux01 UNIQUE(drft_id,file_no),
    CONSTRAINT tmm_mdlinptdrftfiled_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmm_mdlinptdrftfiled_fk02 FOREIGN KEY(file_no) REFERENCES tcm_filel(file_no),
    CONSTRAINT tmm_mdlinptdrftfiled_ck01 CHECK(mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT')),
    CONSTRAINT tmm_mdlinptdrftfiled_ck02 CHECK(del_yn IN ('Y','N')),
    CONSTRAINT tmd_mdl_inptdrftfiled_fk03 FOREIGN KEY(drft_id,upld_file_id)
        REFERENCES tmd_mdl_inptupldfiled(drft_id,upld_file_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftfiled_ck03 CHECK(
        (file_no IS NOT NULL AND upld_file_id IS NULL) OR (file_no IS NULL AND upld_file_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16/19 tmd_mdl_inptdrftrowm
CREATE TABLE tmd_mdl_inptdrftrowm
(
    row_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    inpt_tbl_id INT UNSIGNED NOT NULL,
    org_row_no INT UNSIGNED NULL,
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    clnt_row_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    file_rvsn_no BIGINT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT tmm_mdlinptdrftrowm_ux01 UNIQUE(drft_id,inpt_tbl_id,file_rvsn_no,org_row_no),
    CONSTRAINT tmm_mdlinptdrftrowm_ux02 UNIQUE(prcs_id,clnt_row_id),
    CONSTRAINT tmm_mdlinptdrftrowm_ux03 UNIQUE(drft_id,inpt_tbl_id,row_id),
    CONSTRAINT tmm_mdlinptdrftrowm_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmm_mdlinptdrftrowm_fk02 FOREIGN KEY(inpt_tbl_id) REFERENCES tmd_mdl_inpttbld(inpt_tbl_id),
    CONSTRAINT tmm_mdlinptdrftrowm_ck01 CHECK((org_row_no IS NOT NULL AND org_row_no BETWEEN 1 AND 1048576 AND prcs_id IS NULL AND clnt_row_id IS NULL)
        OR (org_row_no IS NULL AND prcs_id IS NOT NULL AND clnt_row_id IS NOT NULL)),
    CONSTRAINT tmm_mdlinptdrftrowm_ck02 CHECK(del_yn IN ('Y','N')),
    CONSTRAINT tmd_mdl_inptdrftrowm_fk03 FOREIGN KEY(drft_id,prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(drft_id,prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftrowm_fk04 FOREIGN KEY(drft_id,file_rvsn_no)
        REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptdrftrowm_ck03 CHECK(file_rvsn_no<=9007199254740991)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17/19 tmd_mdl_inptdrftrowh
CREATE TABLE tmd_mdl_inptdrftrowh
(
    row_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    bgn_rvsn_no BIGINT UNSIGNED NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    inpt_tbl_id INT UNSIGNED NOT NULL,
    end_rvsn_no BIGINT UNSIGNED NULL,
    row_no INT UNSIGNED NULL,
    actv_row_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
        (IF(end_rvsn_no IS NULL,row_id,NULL)) STORED,
    actv_row_no INT UNSIGNED GENERATED ALWAYS AS (IF(end_rvsn_no IS NULL,row_no,NULL)) STORED,
    PRIMARY KEY(row_id,bgn_rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrowh_ux01 UNIQUE(actv_row_id),
    CONSTRAINT tmm_mdlinptdrftrowh_ux02 UNIQUE(drft_id,inpt_tbl_id,actv_row_no),
    INDEX tmm_mdlinptdrftrowh_ix01(drft_id,inpt_tbl_id,bgn_rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrowh_fk01 FOREIGN KEY(drft_id,inpt_tbl_id,row_id) REFERENCES tmd_mdl_inptdrftrowm(drft_id,inpt_tbl_id,row_id),
    CONSTRAINT tmm_mdlinptdrftrowh_fk02 FOREIGN KEY(drft_id,bgn_rvsn_no) REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrowh_fk03 FOREIGN KEY(drft_id,end_rvsn_no) REFERENCES tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrowh_ck01 CHECK(row_no IS NULL OR row_no BETWEEN 1 AND 1048576),
    CONSTRAINT tmm_mdlinptdrftrowh_ck02 CHECK(end_rvsn_no IS NULL OR end_rvsn_no>bgn_rvsn_no),
    CONSTRAINT tmm_mdlinptdrftrowh_ck03 CHECK(bgn_rvsn_no<=9007199254740991 AND (end_rvsn_no IS NULL OR end_rvsn_no<=9007199254740991)),
    CONSTRAINT tmm_mdlinptdrftrowh_ck04 CHECK(del_yn IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 18/19 tmd_mdl_inptfnsvfiled
CREATE TABLE tmd_mdl_inptfnsvfiled
(
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    mdl_file_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    strg_ref_json LONGTEXT NOT NULL,
    prcs_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    file_size BIGINT UNSIGNED NULL,
    file_no BIGINT UNSIGNED NULL,
    PRIMARY KEY(prcs_id,try_tokn,mdl_file_cd),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ux01 UNIQUE(file_no),
    INDEX tmd_mdl_inptfnsvfiled_ix01(prcs_stat_cd,mod_dtm),
    CONSTRAINT tmd_mdl_inptfnsvfiled_fk01 FOREIGN KEY(prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptfnsvfiled_fk02 FOREIGN KEY(file_no)
        REFERENCES tcm_filel(file_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck01 CHECK(mdl_file_cd IN ('SUPPLY','DEMAND','TRADE','OUTPUT')),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck02 CHECK(prcs_stat_cd IN ('PREPARED','UPLOADED','COMMITTED','CLEANUP_PENDING','CLEANED')),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck03 CHECK(
        (prcs_stat_cd='COMMITTED' AND file_no IS NOT NULL AND file_size IS NOT NULL)
        OR (prcs_stat_cd<>'COMMITTED' AND file_no IS NULL)),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck04 CHECK(JSON_VALID(strg_ref_json) AND OCTET_LENGTH(strg_ref_json)<=16384),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck05 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 19/19 tmd_mdl_inptexecm
CREATE TABLE tmd_mdl_inptexecm
(
    exec_no TINYINT UNSIGNED NOT NULL PRIMARY KEY,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    exec_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    exec_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    bgn_dtm DATETIME(6) NULL,
    end_dtm DATETIME(6) NULL,
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    aprv_exec_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    aprv_usr_logn_id VARCHAR(50) NULL,
    aprv_dtm DATETIME(6) NULL,
    INDEX tmd_mdl_inptexecm_ix01(prcs_id),
    CONSTRAINT tmd_mdl_inptexecm_fk01 FOREIGN KEY(prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptexecm_ck01 CHECK(exec_no=1 AND del_yn='N'),
    CONSTRAINT tmd_mdl_inptexecm_ck02 CHECK(
        (exec_stat_cd='STOPPED' AND prcs_id IS NULL AND try_tokn IS NULL
            AND ((exec_id IS NULL AND bgn_dtm IS NULL AND end_dtm IS NULL)
                OR (exec_id IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NOT NULL)))
        OR (exec_stat_cd IN ('ACTIVE','RECOVERING') AND exec_id IS NOT NULL AND bgn_dtm IS NOT NULL AND end_dtm IS NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck03 CHECK(
        (prcs_id IS NULL AND try_tokn IS NULL) OR (prcs_id IS NOT NULL AND try_tokn IS NOT NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck04 CHECK(
        (aprv_exec_id IS NULL AND aprv_usr_logn_id IS NULL AND aprv_dtm IS NULL)
        OR (aprv_exec_id IS NOT NULL AND aprv_usr_logn_id IS NOT NULL AND aprv_dtm IS NOT NULL AND exec_id IS NOT NULL)),
    CONSTRAINT tmd_mdl_inptexecm_ck05 CHECK(exec_stat_cd<>'RECOVERING' OR aprv_exec_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

MANUAL_CREATE_END */
-- 실패/연결 단절 시 쓰기를 계속 막고 실제 SHOW CREATE를 확인해 완료된 문장을 기록한다.
-- 이미 생성한 테이블을 삭제하거나 파일 전체를 다시 실행하지 않는다.

-- 3. INITIALIZE / BACKFILL — 이력 backfill 없음. 실행기 최초 STOPPED 1행만 수동 초기화.
-- 19개 CREATE 성공/정의 검증 뒤 실행한다. §1의 환경/서버/schema/운영자 입력을 같은 연결에서 사용한다.
-- INSERT는 신규 테이블에 1회만 한다. 기존 행은 UPSERT/UPDATE/DELETE로 초기화하지 않는다.
-- MANUAL 블록 기본값 ROLLBACK. 검증 성공 시 마지막 ROLLBACK 대신 COMMIT을 운영자가 선택한다.
/* MANUAL_INITIALIZE_BEGIN
SELECT COUNT(*) AS existing_runner_rows_must_be_0 FROM tmd_mdl_inptexecm;
-- 0이 아니면 여기서 중단한다. 기존 행은 보존한다. 중복 INSERT는 PK 오류로 실패한다.
START TRANSACTION;
INSERT INTO tmd_mdl_inptexecm(exec_no,crt_usr_logn_id,mod_usr_logn_id,exec_stat_cd)
SELECT 1,@mi_install_operator,@mi_install_operator,'STOPPED'
WHERE COALESCE(BINARY @mi_install_environment IN ('DEVELOPMENT','PRODUCTION'),0)=1
  AND COALESCE(BINARY @@GLOBAL.server_uuid=BINARY @mi_install_expected_server_uuid,0)=1
  AND COALESCE(BINARY DATABASE()=BINARY @mi_install_expected_schema,0)=1
  AND @@GLOBAL.read_only=0 AND @@GLOBAL.super_read_only=0
  AND @mi_install_operator IS NOT NULL
  AND REGEXP_LIKE(CONVERT(@mi_install_operator USING utf8mb4) COLLATE utf8mb4_bin,_utf8mb4'^[a-zA-Z0-9._@-]{1,50}$','c');
SELECT COUNT(*) AS runner_rows_must_be_1 FROM tmd_mdl_inptexecm;
SELECT exec_no,exec_id,exec_stat_cd,bgn_dtm,end_dtm,prcs_id,try_tokn,
       aprv_exec_id,aprv_usr_logn_id,aprv_dtm FROM tmd_mdl_inptexecm;
-- 1행, exec_no=1 / STOPPED, 실행·작업·승인 필드는 모두 NULL이어야 한다.
-- 0행/불일치/오류면 ROLLBACK 후 중단. 올바른 신규 1행일 때만 아래 ROLLBACK 대신 COMMIT 선택.
ROLLBACK;
MANUAL_INITIALIZE_END */

-- 4. VERIFY — CREATE 후/초기화 COMMIT 후 실행한다. 조회 결과에는 JSON/비공개 저장 경로를 포함하지 않는다.
SELECT COUNT(*) AS installed_tables_must_be_19,
       SUM(engine='InnoDB') AS innodb_tables_must_be_19
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm');
SELECT table_name,column_name,column_type,is_nullable,column_default,extra,collation_name,datetime_precision
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm')
ORDER BY table_name,ordinal_position;
SELECT tc.table_name,tc.constraint_name,tc.enforced,cc.check_clause
FROM information_schema.table_constraints tc JOIN information_schema.check_constraints cc
 ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name
WHERE tc.constraint_schema=DATABASE() AND tc.table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm')
ORDER BY tc.table_name,tc.constraint_name;
-- 모든 CHECK ENFORCED=YES. request ck03=2 / ck09=120초 또는 UPLOAD600초, table ck01은 header0/0 허용.
SELECT table_name,index_name,non_unique,seq_in_index,column_name,sub_part
FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm')
ORDER BY table_name,index_name,seq_in_index;
SELECT table_name,constraint_name,column_name,referenced_table_name,referenced_column_name
FROM information_schema.key_column_usage WHERE constraint_schema=DATABASE()
 AND table_name IN (
 'tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled',
 'tmd_mdl_inptshtd',
 'tmd_mdl_inpttbld',
 'tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd',
 'tmd_mdl_inptbasem',
 'tmd_mdl_inptbasefiled',
 'tmd_mdl_inptbaserowd',
 'tmd_mdl_inptdrftm',
 'tmd_mdl_inptdrftprcsl',
 'tmd_mdl_inptdrftrvsnl',
 'tmd_mdl_inptupldfiled',
 'tmd_mdl_inptupldactvm',
 'tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrowm',
 'tmd_mdl_inptdrftrowh',
 'tmd_mdl_inptfnsvfiled',
 'tmd_mdl_inptexecm') AND referenced_table_name IS NOT NULL
ORDER BY table_name,constraint_name,ordinal_position;
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_inptmodscopd;
SHOW CREATE TABLE tmd_mdl_scnrcasetmpltd;
SHOW CREATE TABLE tmd_mdl_inptbasem;
SHOW CREATE TABLE tmd_mdl_inptbasefiled;
SHOW CREATE TABLE tmd_mdl_inptbaserowd;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptupldfiled;
SHOW CREATE TABLE tmd_mdl_inptupldactvm;
SHOW CREATE TABLE tmd_mdl_inptdrftfiled;
SHOW CREATE TABLE tmd_mdl_inptdrftrowm;
SHOW CREATE TABLE tmd_mdl_inptdrftrowh;
SHOW CREATE TABLE tmd_mdl_inptfnsvfiled;
SHOW CREATE TABLE tmd_mdl_inptexecm;

-- 최초 설치/seed COMMIT 직후: 실행기만 1행, 나머지 18개 0행. catalog 등록 전에 확인한다.
SELECT 'tmd_mdl_inpttmpltm' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inpttmpltm
UNION ALL SELECT 'tmd_mdl_inptfiled' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptfiled
UNION ALL SELECT 'tmd_mdl_inptshtd' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptshtd
UNION ALL SELECT 'tmd_mdl_inpttbld' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inpttbld
UNION ALL SELECT 'tmd_mdl_inptmodscopd' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptmodscopd
UNION ALL SELECT 'tmd_mdl_scnrcasetmpltd' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_scnrcasetmpltd
UNION ALL SELECT 'tmd_mdl_inptbasem' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptbasem
UNION ALL SELECT 'tmd_mdl_inptbasefiled' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptbasefiled
UNION ALL SELECT 'tmd_mdl_inptbaserowd' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptbaserowd
UNION ALL SELECT 'tmd_mdl_inptdrftm' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftm
UNION ALL SELECT 'tmd_mdl_inptdrftprcsl' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftprcsl
UNION ALL SELECT 'tmd_mdl_inptdrftrvsnl' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftrvsnl
UNION ALL SELECT 'tmd_mdl_inptupldfiled' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptupldfiled
UNION ALL SELECT 'tmd_mdl_inptupldactvm' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptupldactvm
UNION ALL SELECT 'tmd_mdl_inptdrftfiled' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftfiled
UNION ALL SELECT 'tmd_mdl_inptdrftrowm' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftrowm
UNION ALL SELECT 'tmd_mdl_inptdrftrowh' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptdrftrowh
UNION ALL SELECT 'tmd_mdl_inptfnsvfiled' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptfnsvfiled
UNION ALL SELECT 'tmd_mdl_inptexecm' AS table_name,COUNT(*) AS row_count FROM tmd_mdl_inptexecm;

SELECT exec_no,exec_id,exec_stat_cd,bgn_dtm,end_dtm,prcs_id,try_tokn,
       aprv_exec_id,aprv_usr_logn_id,aprv_dtm FROM tmd_mdl_inptexecm;
-- 기대 STOPPED 1행 / 나머지 NULL. ACTIVE가 보이면 자동 reset하지 말고 이미 가동된 원인을 확인한다.
-- 다중 업로드: ix02(prcs_id) NON_UNIQUE=1, ux01(prcs_id)는 없어야 한다.
SELECT index_name,non_unique,seq_in_index,column_name FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptupldfiled'
 AND index_name IN ('tmd_mdl_inptupldfiled_ix02','tmd_mdl_inptupldfiled_ux01');

-- 5. DBA DEPLOYMENT / GO-NO-GO
-- 운영계 적용 전: 개발계의 이 파일 적용/제약 강제/격리 API·파일 결과·복구 검증과 배포 코드 SHA를 확인한다.
-- 같은 SQL 파일이어도 운영계의 §1 pre-check·백업·앱 권한 확인을 생략하지 않는다. 운영계는 별도 연결/결과로 진행한다.
-- BE/DBA: pre-check 결과 보관 → CREATE/정의·제약·빈 데이터 verify → 초기 STOPPED seed COMMIT/verify
--   → 동일 schema를 쓰는 호환 API/소비자 코드 배포 → catalog 등록/발행 → 격리 fixture API 검증 → 접수 활성화.
-- DDL 검증 전 mi.model-input.web-runner.enabled=false, mi.model-input.shared-baseline.enabled=false,
--   mi.model-input.upload.enabled=false를 배포 설정에 명시한다. 로컬/testdb profile의 true를 복사하지 않는다.
-- 공유 기준 생성 OFF여도 코드가 base_id를 읽으므로 공유 기준 컬럼/테이블을 이 설치에서 생략하지 않는다.
-- CHANGE/FINALIZE/UPLOAD는 요청 접수만으로 완료되지 않는다. 유효 소비자(웹 실행기 또는 승인된 worker)가 필요하다.
-- 웹 실행기를 쓸 경우 STOPPED seed/GET_LOCK 권한과 같은 master 연결을 확인 후 활성화한다.
-- 양 환경 모두 최초 설치를 전제로 한다. 대상 환경에 이전 worker/실행 흔적이 있으면 최초 설치 전제를 재확인한다.
-- 기존 worker와 혼합 소비하지 않는다. 개발계에서의 종료 확인은 운영계 종료 증거가 아니다.
-- 실제 CHECK/FK 강제, 두 DB 연결의 named lock/행 잠금 경쟁, 앱 계정 SELECT/INSERT/UPDATE/DELETE 권한,
--   MySQL/storage/인증·회사 소유 범위/재시작 복구/파일 결과를 격리 fixture로 검증한다.
-- 요청 data.status=SUCCEEDED 및 resultRevision 재조회까지 확인하고 최종 Case/파일 metadata·객체를 대조한다.
--
-- catalog 데이터는 이 파일에 없다. 등록/발행/Case 연결은 환경별 별도 DML 승인 범위다.
-- 공통 최초 등록·발행: model-input-catalog-install-registration.sql (DRAFT) 다음
--   model-input-catalog-install-publication.sql (호환 reader 확인 후 ACTIVE)을 사용한다. 각각 기본 ROLLBACK이다.
-- testdb/개발계의 tmplt_id=7/8/10 등 숫자나 Case103/108/109를 다른 환경 ID로 가정하지 않는다.
-- 기존 model-input-catalog-structure-data-draft.sql 및 editor-write-template-version / v3 / v4 / v5 파일은
--   원본 정의 참고 자료다. testdb ID와 단계별 전제를 갖고 있어 개발계/운영계에 그대로 재실행하면 안 된다.
-- 환경별 실제 template/model-code/source-key와 정규화 XLSM에 맞춘 등록본을 준비/발행해야 API를 사용할 수 있다.
-- 운영계에 개발계 Draft/요청/실행기 상태/파일 metadata·객체를 복사하지 않는다. 운영계 STOPPED는 독립 초기화한다.
-- 양 환경 SHOW CREATE/컬럼·제약·index·table collation 결과를 대조한다(환경별 ID/감사값/시각은 복사 대상 아님).
-- 빈 테이블/정의 성공만으로 정책 READY·업로드 가능·편집/FINALIZE 배포 완료로 판정하지 않는다.
--
-- 6. ROLLBACK / FALLBACK
-- 초기 seed COMMIT 전: ROLLBACK은 그 INSERT만 취소한다. 앞서 성공한 CREATE는 취소하지 않는다.
-- CREATE 일부 성공/배포 실패: 접수·소비를 중지하고 테이블/행/요청·파일 이력을 보존한다.
-- 실제 정의를 대조해 누락 문장만 진행하거나 같은 v2/다중 업로드/공유 기준을 읽는 호환 코드로 수정 배포한다.
-- 구 prefix/v1/단일파일 reader로 되돌리거나 CHECK를 축소하지 않는다. base_id/기한/이력을 고쳐 맞추지 않는다.
-- 이미 실행한 소비자의 종료/실제 commit이 불명확하면 종료 증거와 기존 복구 절차를 먼저 확보한다.
-- DROP/TRUNCATE/대량 DELETE/기존 Case·tcm_filel·스토리지 삭제는 이 설치/복구 범위에 없다.
--
-- References (MySQL 8.0):
-- https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html
-- https://dev.mysql.com/doc/refman/8.0/en/create-table-foreign-keys.html
-- https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html
-- https://dev.mysql.com/doc/refman/8.0/en/insert-select.html
-- https://dev.mysql.com/doc/refman/8.0/en/replication-options.html#sysvar_server_uuid
-- https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_default_collation_for_utf8mb4
-- Implementation readiness: ready (최초 설치 SQL 검토 입력).
-- DBA apply readiness: 위 Open Questions와 PRE-CHECK/실제 MySQL verify를 담당자가 확인한 뒤 결정.
