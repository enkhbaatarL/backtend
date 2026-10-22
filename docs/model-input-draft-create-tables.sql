-- Document Control
-- status: Review; owner: BE / DBA; last synced: 2026-10-22
-- canonical for: Reference only — 최초 설치 생성문 전용 배포본
-- source: model-input-draft-install-ddl.sql / MANUAL_CREATE (동일 정의)

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
