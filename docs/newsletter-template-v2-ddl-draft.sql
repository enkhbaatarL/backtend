-- Newsletter Template V2 DDL Draft (DBA)
-- Date: 2026-03-16
-- Logical name: newsletter_template_v2
-- Physical table: tmd_nwsltrtmpltm / tmd_nwsltrtmpltsctn / tmd_nwsltrtmpltblk / tmd_nwsltrtmpltblkfld
-- Rollout: DDL -> migration -> app

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tmd_nws_ltr_mstr',
      'tmd_nwsltrtmpltm',
      'tmd_nwsltrtmpltsctn',
      'tmd_nwsltrtmpltblk',
      'tmd_nwsltrtmpltblkfld'
  );

/* =========================================================
 * 1) APPLY
 * ========================================================= */

CREATE TABLE tmd_nwsltrtmpltm
(
    nws_ltr_tmplt_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn              CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_mstr_id     INT UNSIGNED                          NOT NULL COMMENT '뉴스레터마스터ID',
    tmplt_nm            VARCHAR(200)                          NOT NULL COMMENT '템플릿명',
    tmplt_desc          VARCHAR(500)                          NULL COMMENT '템플릿설명',
    schm_ver            SMALLINT UNSIGNED DEFAULT 1           NOT NULL COMMENT '스키마버전',
    CONSTRAINT tmd_nwsltrtmpltm_ux01 UNIQUE (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrtmpltm_fk01 FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrtmpltm_ck01 CHECK (del_yn IN ('N', 'Y'))
) COMMENT 'TMD_뉴스레터템플릿V2기본';

CREATE TABLE tmd_nwsltrtmpltsctn
(
    nws_ltr_tmplt_sctn_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿섹션ID' PRIMARY KEY,
    crt_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_tmplt_id   BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터템플릿V2ID',
    sctn_cd               VARCHAR(50)                           NOT NULL COMMENT '섹션코드',
    sctn_type_cd          VARCHAR(30)                           NOT NULL COMMENT '섹션유형코드',
    sctn_nm               VARCHAR(200)                          NOT NULL COMMENT '섹션명',
    sctn_ord              INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '섹션순서',
    vsblty_yn             CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    tgt_cmpny_no          INT UNSIGNED                          NULL COMMENT '대상고객사번호',
    CONSTRAINT tmd_nwsltrtmpltsctn_ux01 UNIQUE (nws_ltr_tmplt_id, sctn_cd),
    CONSTRAINT tmd_nwsltrtmpltsctn_fk01 FOREIGN KEY (nws_ltr_tmplt_id) REFERENCES tmd_nwsltrtmpltm (nws_ltr_tmplt_id)
) COMMENT 'TMD_뉴스레터템플릿섹션';

CREATE INDEX tmd_nwsltrtmpltsctn_ix01 ON tmd_nwsltrtmpltsctn (nws_ltr_tmplt_id, sctn_ord);

CREATE TABLE tmd_nwsltrtmpltblk
(
    nws_ltr_tmplt_blk_id  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿블록ID' PRIMARY KEY,
    crt_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_tmplt_sctn_id BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터템플릿섹션ID',
    blk_cd                VARCHAR(50)                           NOT NULL COMMENT '블록코드',
    blk_type_cd           VARCHAR(30)                           NOT NULL COMMENT '블록타입코드',
    blk_fmt_cd            VARCHAR(30) DEFAULT 'GENERAL'         NOT NULL COMMENT '블록형식코드',
    blk_nm                VARCHAR(200)                          NOT NULL COMMENT '블록명',
    blk_ord               INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '블록순서',
    vsblty_yn             CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    plot_on_chrt_yn       CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '차트변곡점표시여부',
    tgt_cmpny_no          INT UNSIGNED                          NULL COMMENT '대상고객사번호',
    data_src_type_cd      VARCHAR(30)                           NULL COMMENT '데이터소스타입',
    data_src_ref          VARCHAR(500)                          NULL COMMENT '데이터소스참조',
    CONSTRAINT tmd_nwsltrtmpltblk_ux01 UNIQUE (nws_ltr_tmplt_sctn_id, blk_cd),
    CONSTRAINT tmd_nwsltrtmpltblk_fk01 FOREIGN KEY (nws_ltr_tmplt_sctn_id) REFERENCES tmd_nwsltrtmpltsctn (nws_ltr_tmplt_sctn_id)
) COMMENT 'TMD_뉴스레터템플릿블록';

CREATE INDEX tmd_nwsltrtmpltblk_ix01 ON tmd_nwsltrtmpltblk (nws_ltr_tmplt_sctn_id, blk_ord);

CREATE TABLE tmd_nwsltrtmpltblkfld
(
    nws_ltr_tmplt_blk_fld_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿블록필드ID' PRIMARY KEY,
    crt_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                   CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_tmplt_blk_id     BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터템플릿블록ID',
    fld_key                  VARCHAR(100)                          NOT NULL COMMENT '필드키',
    fld_nm                   VARCHAR(200)                          NOT NULL COMMENT '필드명',
    fld_ord                  INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '필드순서',
    col_mapping              VARCHAR(500)                          NULL COMMENT '컬럼매핑',
    is_editable_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '사용자수정가능여부',
    is_required_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '필수여부',
    fld_meta                 JSON                                  NULL COMMENT '필드메타정보',
    CONSTRAINT tmd_nwsltrtmpltblkfld_ux01 UNIQUE (nws_ltr_tmplt_blk_id, fld_key),
    CONSTRAINT tmd_nwsltrtmpltblkfld_fk01 FOREIGN KEY (nws_ltr_tmplt_blk_id) REFERENCES tmd_nwsltrtmpltblk (nws_ltr_tmplt_blk_id)
) COMMENT 'TMD_뉴스레터템플릿블록필드';

CREATE INDEX tmd_nwsltrtmpltblkfld_ix01 ON tmd_nwsltrtmpltblkfld (nws_ltr_tmplt_blk_id, fld_ord);

/* =========================================================
 * 2) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrtmpltm;
SHOW CREATE TABLE tmd_nwsltrtmpltsctn;
SHOW CREATE TABLE tmd_nwsltrtmpltblk;
SHOW CREATE TABLE tmd_nwsltrtmpltblkfld;

/* =========================================================
 * 3) ROLLBACK
 * ========================================================= */

DROP TABLE IF EXISTS tmd_nwsltrtmpltblkfld;
DROP TABLE IF EXISTS tmd_nwsltrtmpltblk;
DROP TABLE IF EXISTS tmd_nwsltrtmpltsctn;
DROP TABLE IF EXISTS tmd_nwsltrtmpltm;
