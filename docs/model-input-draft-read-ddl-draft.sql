-- Document Control
-- status: Draft / USER REVIEW AND MANUAL APPLY ONLY
-- canonical for: EDT-01~03 first-slice apply/verify SQL (domain/physical rationale: linked DDL draft §6.5)
-- owner: BE / DBA (user applies); date: 2026-10-16
-- last synced: 2026-10-16 (검토용 분류·사전 확인·적용 순서 주석 보완; SQL 실행문 변경 없음)
-- physical rationale: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.5
-- This file was NOT applied by Codex. No business policy or production seed is defined here.
-- Run section 1 first. If catalog tables already exist, compare SHOW CREATE TABLE and skip section 2.
-- Never run CREATE over existing incompatible tables. Do not disable FK checks.
-- 2026-10-16 사용자 table prefix 변경: tmd_mdl_inpt...가 현재 table명이다.
-- 이미 구 이름으로 적용한 DB는 이 CREATE/ALTER를 다시 실행하지 않고
-- docs/model-input-draft-table-prefix-rename-draft.sql로10개 이름만 변경한다. 실행은 사용자 담당이다.
-- 기존 명시 constraint/index 이름(tmm_mdlinpt..._fk/ck/ux/ix)은 호환을 위해 유지한다.
-- 테이블명 변경은 컬럼/키/인덱스 구조나 데이터 변경이 아니다.

-- 검토 범위와 실행 상태
-- 사용자가 testdb에 DDL을 직접 적용하고 확인 쿼리도 실행한다. Codex는 쿼리 제공·결과 해석을 맡는다.
-- 사용자 적용 보고·1차 존재 확인 결과와 후속 상태는 docs/work-notes/2026/2026-09-03-model-input-transfer-format.md에 기록한다.
-- Codex는 DB 직접 접속·SELECT/DDL/DML을 실행하지 않는다. 실제 전체 정의/제약 호환 확인은 후속이며 파일 전체를 그대로 재실행하지 않는다.
-- A. 신규 Draft 저장 5개 (§4): 현재 생성·재개·조회 API의 최초 상태 revision0에 사용한다.
--    tmd_mdl_inptdrftm       : 개인 Draft 소유·원본 Case/양식·상태·현재 저장 번호
--    tmd_mdl_inptdrftfiled   : 생성 시 고정한 DEMAND/SUPPLY/TRADE/OUTPUT 원본 파일 연결
--    tmd_mdl_inptdrftrvsnl   : 성공한 저장 이력(현재 API는 revision0만 기록)
--    tmd_mdl_inptdrftrowm    : 표별 행의 고정 rowId와 원본 행 대응
--    tmd_mdl_inptdrftrowh    : 저장 시점별 행 위치 이력
--    파일은 기존 저장소를 참조하며 파일 내용 전체를 이 5개 table에 복제하지 않는다.
-- B. 없을 때만 생성할 기존 설계 6개 (§2): 이미 있으면 실제 정의를 비교하고 해당 CREATE/INDEX를 건너뛴다.
--    tmd_mdl_inpttmpltm      : 양식 버전
--    tmd_mdl_inptfiled       : 양식의 파일 역할
--    tmd_mdl_inptshtd        : 시트
--    tmd_mdl_inpttbld        : 시트 안의 표
--    tmd_mdl_inptmodscopd    : 수정 가능 범위
--    tmd_mdl_scnrcasetmpltd : Case와 양식 버전 연결
-- C. 기존 table 변경 (§3): tmd_mdl_inpttbld의 qry_json/JSON 검사 추가와 행 범위 ck01 교체.
--    B에서 이 table을 새로 만들었을 때도 §3이 필요하다. 이미 같은 변경이 있으면 중복 적용하지 않는다.
-- 표별 정책·목록/seed 설계는 후속이며 PriceModel/Regas 추가 추천은 미승인으로 유지한다.
-- 변경값 저장·최종저장 요청/준비파일 table과 해당 prcs_id 연결 제약은 이번 적용 대상이 아니다.

-- 적용 전 확인사항 (보류 해제 후 사용자/DBA가 확인)
-- 1) 대상이 master schema인지, MySQL 버전·engine·문자셋/문자 비교 방식이 초안과 호환되는지 확인한다.
-- 2) Case/파일/사용자/회사와 위 11개 table의 실제 정의를 SHOW CREATE TABLE로 비교한다.
--    연결 ID의 타입·UNSIGNED·길이·NULL·현재 최대값/auto_increment가 참조/Java 표현 범위에 맞아야 한다.
--    신규5개 이름이 이미 존재하면 해당 CREATE를 재실행하지 않고 정의 차이부터 확인한다.
-- 3) qry_json과 ck01/ck06의 현재 존재·내용을 확인해 이미 적용된 ADD/DROP을 중복 실행하지 않는다.
-- 4) 자동 계산 컬럼·NULL을 포함한 중복 방지·연결 제약·CHECK/JSON_VALID, transaction/lock 설정을 확인한다.
--    max_allowed_packet과 연결 설정도 JSON 저장 한도에 맞는지 확인한다.
-- 5) 사용자 채택 crt_*/mod_*·감사 DATETIME 정렬은 별도 증분 SQL을 따른다. 실제 길이/환경 시간대·외부 표준 확인은 DBA-04다.
-- 6) 기준 Case103의 4역할 파일/유효 metadata·저장소 접근, Case-양식 연결·조회 정의와
--    mi.model-input.model-code의 실제 등록값을 확인한다. 이 파일에 production 등록 데이터는 없다.
--    qry_json은 조회할 열/헤더/본문 등을 정하는 정보다. NULL 저장은 허용하지만 미완비면 API 조회는 거절된다.
--    새 DRAFT 발행에서 준비하며 기존 ACTIVE/DEPRECATED 정의나 Case 연결을 덮어쓰지 않는다.
-- 7) 위 결과를 반영한 적용 대상 구간·실행 권한·수동 적용/검증·복구 순서를 사용자/DBA가 검토한다.

-- 적용 순서 (실행 승인·DB 보류 해제 후)
-- 1) §1 읽기 전용 사전 확인과 SHOW CREATE TABLE 비교로 적용할 구간을 정한다.
-- 2) §2 없는 기존 설계 table만 기재 순서(양식→파일→시트→표→수정범위→Case 연결)로 생성한다.
-- 3) §3 qry_json/ck06 추가와 ck01 교체를 현재 정의에 맞춰 적용한다.
-- 4) §4 신규5개를 Draft→고정 파일→저장 이력→행 ID→위치 이력 순서로 생성한다.
-- 5) 실제 생성된 컬럼·중복 방지/연결/검사 제약을 다시 확인한다.
-- 6) 기준 Case/조회 정의·설정 준비를 확인한 뒤 생성→동일 Draft 재개→구조/표 구간 조회를 검증한다.
-- 7) §5의 이상 조회 결과가 모두 0행인지 확인한다. 빈 DB의 0행만으로 API 검증 완료로 보지 않는다.
-- 복구 시 신규 Draft 쓰기를 중단하고 호환되는 이전 앱을 사용하되 저장 행·원본 파일·양식 연결은 보존한다.

-- 1. PRE-CHECK (read-only). Verify master schema, existing types/engines/audit standard and ID headroom.
-- 적용 전 읽기 전용 확인 구간. 이 결과만으로 실제 전체 정의/제약 호환 확인을 대신하지 않는다.
SELECT DATABASE(), VERSION(), @@max_allowed_packet, @@transaction_isolation;
SELECT table_name, engine, table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
('tmd_mdl_scnrcasem','tcm_filel','tmd_usrm','tmd_custcpnym','tmd_mdl_inpttmpltm',
 'tmd_mdl_inptfiled','tmd_mdl_inptshtd','tmd_mdl_inpttbld','tmd_mdl_inptmodscopd',
 'tmd_mdl_scnrcasetmpltd','tmd_mdl_inptdrftm','tmd_mdl_inptdrftfiled',
 'tmd_mdl_inptdrftrvsnl','tmd_mdl_inptdrftrowm','tmd_mdl_inptdrftrowh');
-- Also review SHOW CREATE TABLE for existing rows above. Existing IDs must fit Java positive Long/Integer usages.
-- crt_*/mod_* names and DATETIME audit precision follow the user decision (2026-10-19).
-- Existing tables: use model-input-audit-precision-ddl-draft.sql; do not rerun CREATE.

-- 2. EXISTING CATALOG DESIGN: only create missing catalog set after pre-check.
-- B. 기존 설계6개: 실제 DB에 없는 table만 아래 순서로 생성. 이미 있으면 해당 CREATE와 INDEX를 건너뛴다.
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
) COMMENT 'TMM_모델입력템플릿마스터';

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
) COMMENT 'TMM_모델입력파일상세';

CREATE INDEX tmm_mdlinptfiled_ix01
    ON tmd_mdl_inptfiled (file_strt_hash_val, mdl_file_cd, del_yn);

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
) COMMENT 'TMM_모델입력시트상세';

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
    CONSTRAINT tmm_mdlinpttbld_ux01 UNIQUE (inpt_sht_id, inpt_tbl_nm),
    CONSTRAINT tmm_mdlinpttbld_ux02 UNIQUE (inpt_sht_id, tbl_ord),
    CONSTRAINT tmm_mdlinpttbld_fk01 FOREIGN KEY (inpt_sht_id)
        REFERENCES tmd_mdl_inptshtd (inpt_sht_id),
    CONSTRAINT tmm_mdlinpttbld_ck01 CHECK
        (tbl_bgn_row_no >= 1 AND tbl_bgn_row_no <= hdr_bgn_row_no
         AND hdr_bgn_row_no <= hdr_end_row_no AND hdr_end_row_no < data_bgn_row_no
         AND data_bgn_row_no <= tbl_end_row_no AND tbl_end_row_no <= 1048576),
    CONSTRAINT tmm_mdlinpttbld_ck02 CHECK
        (tbl_bgn_col_no >= 1 AND tbl_bgn_col_no <= tbl_end_col_no AND tbl_end_col_no <= 16384),
    CONSTRAINT tmm_mdlinpttbld_ck03 CHECK (row_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck04 CHECK (col_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck05 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력테이블상세';

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
) COMMENT 'TMM_모델입력수정범위상세';

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
) COMMENT 'TMD_시나리오케이스입력템플릿상세';

CREATE INDEX tmd_mdl_scnrcasetmpltd_ix01
    ON tmd_mdl_scnrcasetmpltd (tmplt_id, del_yn);

-- 3. READ DEFINITION STORAGE: apply only after confirming qry_json does not already exist.
-- Only populate on a new DRAFT publication; never mutate existing ACTIVE/DEPRECATED definitions.
-- C. 기존 table 변경 1/2: qry_json LONGTEXT NULL과 유효 JSON/최대1MiB 검사 추가.
-- 컬럼/ck06이 이미 있으면 현재 정의를 비교한다. 기존 조회 정의나 사용자 입력값을 채우는 DML은 없다.
ALTER TABLE tmd_mdl_inpttbld
    ADD COLUMN qry_json LONGTEXT NULL COMMENT '버전1 검증된 표 조회 정의 JSON',
    ADD CONSTRAINT tmm_mdlinpttbld_ck06 CHECK (qry_json IS NULL OR (JSON_VALID(qry_json) AND OCTET_LENGTH(qry_json)<=1048576));

-- Existing rows remain valid. Allow an approved control-only rectangle with no grid header/body.
-- Public grid rows/columns are explicitly defined by qry_json; do not fabricate placeholder rows.
-- C. 기존 table 변경 2/2: ck01을 교체해 헤더/본문 없는 선택 항목 전용 영역도 표현한다.
-- 기존 유효 행을 수용하는 제약 변경이며, 실제 표의 조회 행/열은 qry_json에서 검증한다.
ALTER TABLE tmd_mdl_inpttbld DROP CHECK tmm_mdlinpttbld_ck01;
ALTER TABLE tmd_mdl_inpttbld ADD CONSTRAINT tmm_mdlinpttbld_ck01 CHECK
    (tbl_bgn_row_no BETWEEN 1 AND 1048576 AND tbl_end_row_no BETWEEN tbl_bgn_row_no AND 1048576
     AND ((hdr_bgn_row_no=0 AND hdr_end_row_no=0)
          OR (hdr_bgn_row_no>=tbl_bgn_row_no AND hdr_end_row_no>=hdr_bgn_row_no AND hdr_end_row_no<=tbl_end_row_no))
     AND (data_bgn_row_no=0 OR data_bgn_row_no BETWEEN tbl_bgn_row_no AND tbl_end_row_no));

-- 4. FIRST-SLICE DRAFT STORAGE: no change/finalization request or upload-preparation tables yet.
-- A. 신규5개. Case/파일/사용자/회사·양식/표 부모 table 확인 후 아래 순서로 생성한다.
-- 신규 1/5: 개인 Draft master. 같은 회사·사용자·원본 Case의 진행 Draft는1개로 제한한다.
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
    org_case_id INT UNSIGNED NOT NULL,
    tmplt_id INT UNSIGNED NOT NULL,
    org_rvsn_val VARCHAR(68) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    drft_stat_cd VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    rvsn_no BIGINT UNSIGNED NOT NULL DEFAULT 0,
    actv_prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    fnsv_case_id INT UNSIGNED NULL,
    actv_org_case_id INT UNSIGNED GENERATED ALWAYS AS
        (IF(del_yn='N' AND drft_stat_cd='DRAFT',org_case_id,NULL)) STORED,
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
    CONSTRAINT tmm_mdlinptdrftm_ck05 CHECK(del_yn IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 신규 2/5: 원본4파일 연결. 역할별1개/같은 파일 중복 연결 방지; 정확히4개인지는 API와 §5에서 확인한다.
CREATE TABLE tmd_mdl_inptdrftfiled
(
    drft_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    mdl_file_cd VARCHAR(20) NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    file_no BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY(drft_id,mdl_file_cd),
    CONSTRAINT tmm_mdlinptdrftfiled_ux01 UNIQUE(drft_id,file_no),
    CONSTRAINT tmm_mdlinptdrftfiled_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmm_mdlinptdrftfiled_fk02 FOREIGN KEY(file_no) REFERENCES tcm_filel(file_no),
    CONSTRAINT tmm_mdlinptdrftfiled_ck01 CHECK(mdl_file_cd IN ('DEMAND','SUPPLY','TRADE','OUTPUT')),
    CONSTRAINT tmm_mdlinptdrftfiled_ck02 CHECK(del_yn IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 신규 3/5: 성공 저장 이력. 현재 API는 변경분 없는 revision0만 기록하며 이후 저장은 후속이다.
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
    CONSTRAINT tmm_mdlinptdrftrvsnl_ck04 CHECK(del_yn IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 신규 4/5: 고정 rowId. 같은 Draft/표/원본 행의 중복 초기화를 막는다.
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
    CONSTRAINT tmm_mdlinptdrftrowm_ux01 UNIQUE(drft_id,inpt_tbl_id,org_row_no),
    CONSTRAINT tmm_mdlinptdrftrowm_ux02 UNIQUE(prcs_id,clnt_row_id),
    CONSTRAINT tmm_mdlinptdrftrowm_ux03 UNIQUE(drft_id,inpt_tbl_id,row_id),
    CONSTRAINT tmm_mdlinptdrftrowm_fk01 FOREIGN KEY(drft_id) REFERENCES tmd_mdl_inptdrftm(drft_id),
    CONSTRAINT tmm_mdlinptdrftrowm_fk02 FOREIGN KEY(inpt_tbl_id) REFERENCES tmd_mdl_inpttbld(inpt_tbl_id),
    CONSTRAINT tmm_mdlinptdrftrowm_ck01 CHECK((org_row_no IS NOT NULL AND org_row_no BETWEEN 1 AND 1048576 AND prcs_id IS NULL AND clnt_row_id IS NULL)
        OR (org_row_no IS NULL AND prcs_id IS NOT NULL AND clnt_row_id IS NOT NULL)),
    CONSTRAINT tmm_mdlinptdrftrowm_ck02 CHECK(del_yn IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 신규 5/5: rowId의 위치 이력. 같은 표의 현재 행 위치와 각 행의 현재 이력 중복을 막는다.
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

-- Request-table FKs on prcs_id are deliberately deferred until the CHANGE slice creates its table.
-- EDT-01 writes revision0/source rows only. Do not insert later revisions/ADDED rows manually.

-- 5. VERIFY (after user apply). Each counter should be zero.
-- 이상 조회 3개: 고정 파일4개 누락/초과, 현재 저장 이력 누락, 진행 Draft 중복. 정상 결과는 각각0행이다.
-- API로 Draft를 생성/재개한 뒤 확인한다. 데이터가 없어0행인 상태는 동작 검증의 증거가 아니다.
SELECT d.drft_id FROM tmd_mdl_inptdrftm d LEFT JOIN tmd_mdl_inptdrftfiled f ON f.drft_id=d.drft_id
GROUP BY d.drft_id HAVING COUNT(f.file_no)<>4;
SELECT d.drft_id FROM tmd_mdl_inptdrftm d LEFT JOIN tmd_mdl_inptdrftrvsnl v
 ON v.drft_id=d.drft_id AND v.rvsn_no=d.rvsn_no WHERE v.drft_id IS NULL;
SELECT cust_cpny_no,usr_no,org_case_id,COUNT(*) FROM tmd_mdl_inptdrftm
WHERE del_yn='N' AND drft_stat_cd='DRAFT' GROUP BY cust_cpny_no,usr_no,org_case_id HAVING COUNT(*)>1;
-- Verify a selected source case has one matching ACTIVE/DEPRECATED publication, four roles,
-- validated qry_json for each visible table, and valid file metadata; no data is fabricated here.
-- ROLLBACK: stop new Draft writes; retain rows/files and use compatible previous app.
-- Do NOT DROP populated tables, delete source/Case files, or rewrite publication bindings.
