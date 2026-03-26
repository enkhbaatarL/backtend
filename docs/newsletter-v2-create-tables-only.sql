-- Newsletter V2 Create Tables Only
-- Date: 2026-03-23
-- Purpose:
--   - DBA 전달용 신규 테이블 생성 구문 모음
--   - 개발기 DB 초기 구축용
-- Scope:
--   - CREATE TABLE / CREATE INDEX only
--   - except prerequisite legacy index drop and parent table ALTER
--   - BACKFILL / DROP excluded
-- Prerequisites:
--   - Existing parent tables must already exist:
--     tmd_nws_ltr_mstr, tmd_nws_ltr_pub_hist, tmd_usrm

/* =========================================================
 * 0) PREREQUISITE
 * ========================================================= */

-- `tmd_nws_ltr_mstr.nws_ltr_tmplt_id` is repurposed to point to V2 template ID.
-- Drop the legacy index first if it still exists in the target environment.
DROP INDEX nws_ltr_mstr_fk01 ON tmd_nws_ltr_mstr;

-- Parent table column type alignment for V2 FK compatibility.
ALTER TABLE tmd_nws_ltr_mstr
    MODIFY COLUMN nws_ltr_tmplt_id BIGINT UNSIGNED NOT NULL COMMENT '현재활성뉴스레터템플릿버전ID';

ALTER TABLE tmd_nws_ltr_pub_hist
    MODIFY COLUMN nws_ltr_tmplt_id BIGINT UNSIGNED NOT NULL COMMENT '발행시점뉴스레터템플릿버전ID';

/* =========================================================
 * 1) TEMPLATE TABLES
 * ========================================================= */

CREATE TABLE tmd_nwsltrtmpltm
(
    nws_ltr_tmplt_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿ID' PRIMARY KEY,
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_mstr_id  INT UNSIGNED                          NOT NULL COMMENT '뉴스레터마스터ID',
    tmplt_nm         VARCHAR(200)                          NOT NULL COMMENT '템플릿명',
    tmplt_desc       VARCHAR(500)                          NULL COMMENT '템플릿설명',
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
    nws_ltr_tmplt_id      BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터템플릿V2ID',
    sctn_cd               VARCHAR(50)                          NOT NULL COMMENT '섹션코드',
    sctn_type_cd          VARCHAR(30)                          NOT NULL COMMENT '섹션유형코드',
    sctn_nm               VARCHAR(200)                         NOT NULL COMMENT '섹션명',
    sctn_ord              INT UNSIGNED DEFAULT 0               NOT NULL COMMENT '섹션순서',
    vsblty_yn             CHAR(1)     DEFAULT 'Y'              NOT NULL COMMENT '노출여부',
    tgt_cmpny_no          INT UNSIGNED                         NULL COMMENT '대상고객사번호',
    CONSTRAINT tmd_nwsltrtmpltsctn_ux01 UNIQUE (nws_ltr_tmplt_id, sctn_cd),
    CONSTRAINT tmd_nwsltrtmpltsctn_fk01 FOREIGN KEY (nws_ltr_tmplt_id) REFERENCES tmd_nwsltrtmpltm (nws_ltr_tmplt_id)
) COMMENT 'TMD_뉴스레터템플릿섹션';

CREATE INDEX tmd_nwsltrtmpltsctn_ix01 ON tmd_nwsltrtmpltsctn (nws_ltr_tmplt_id, sctn_ord);

CREATE TABLE tmd_nwsltrtmpltsctnfld
(
    nws_ltr_tmplt_sctn_fld_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿섹션필드ID' PRIMARY KEY,
    crt_usr_logn_id           VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id           VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                    CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_tmplt_sctn_id     BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터템플릿섹션ID',
    fld_key                   VARCHAR(100)                          NOT NULL COMMENT '필드키',
    fld_nm                    VARCHAR(200)                          NOT NULL COMMENT '필드명',
    fld_ord                   INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '필드순서',
    is_editable_yn            CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '사용자수정가능여부',
    is_required_yn            CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '필수여부',
    fld_meta                  JSON                                  NULL COMMENT '필드메타정보',
    CONSTRAINT tmd_nwsltrtmpltsctnfld_ux01 UNIQUE (nws_ltr_tmplt_sctn_id, fld_key),
    CONSTRAINT tmd_nwsltrtmpltsctnfld_fk01 FOREIGN KEY (nws_ltr_tmplt_sctn_id) REFERENCES tmd_nwsltrtmpltsctn (nws_ltr_tmplt_sctn_id),
    CONSTRAINT tmd_nwsltrtmpltsctnfld_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltsctnfld_ck02 CHECK (is_editable_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltsctnfld_ck03 CHECK (is_required_yn IN ('N', 'Y'))
) COMMENT 'TMD_뉴스레터템플릿섹션필드';

CREATE INDEX tmd_nwsltrtmpltsctnfld_ix01 ON tmd_nwsltrtmpltsctnfld (nws_ltr_tmplt_sctn_id, fld_ord);

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
    data_src_spec_json    JSON                                  NULL COMMENT '데이터소스명세JSON',
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
 * 2) ISSUE / SUBSCRIPTION TABLES
 * ========================================================= */

CREATE TABLE tmd_nwsltrorgissm
(
    nws_ltr_org_iss_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈ID' PRIMARY KEY,
    crt_usr_logn_id    VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id    VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn             CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_mstr_id    INT UNSIGNED                          NOT NULL COMMENT '뉴스레터마스터ID',
    issno              INT UNSIGNED                          NOT NULL COMMENT '발행차수',
    src_nws_ltr_pub_id INT UNSIGNED                          NOT NULL COMMENT '원본발행이력ID',
    pub_stat_cd        VARCHAR(20)                           NOT NULL COMMENT '발행상태코드(PUBLISHING,PUBLISHED,SUSPENDED)',
    pub_dtm            DATETIME                              NULL COMMENT '발행일시',
    org_snpsht_json    JSON                                  NOT NULL COMMENT '원본스냅샷JSON',
    CONSTRAINT tmd_nwsltrorgissm_ux01 UNIQUE (nws_ltr_mstr_id, issno),
    CONSTRAINT tmd_nwsltrorgissm_ux02 UNIQUE (src_nws_ltr_pub_id),
    CONSTRAINT tmd_nwsltrorgissm_fk01 FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrorgissm_fk02 FOREIGN KEY (src_nws_ltr_pub_id) REFERENCES tmd_nws_ltr_pub_hist (nws_ltr_pub_id),
    CONSTRAINT tmd_nwsltrorgissm_ck01 CHECK (pub_stat_cd IN ('PUBLISHING', 'PUBLISHED', 'SUSPENDED')),
    CONSTRAINT tmd_nwsltrorgissm_ck02 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgissm_ck03 CHECK (pub_dtm IS NOT NULL OR pub_stat_cd <> 'PUBLISHED')
) COMMENT 'TMD_뉴스레터발행원본기본';

CREATE INDEX tmd_nwsltrorgissm_ix01 ON tmd_nwsltrorgissm (nws_ltr_mstr_id, pub_stat_cd, del_yn, pub_dtm);

CREATE TABLE tmd_nwsltrsubm
(
    nws_ltr_sub_id   BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터구독ID' PRIMARY KEY,
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_mstr_id  INT UNSIGNED                          NOT NULL COMMENT '뉴스레터마스터ID',
    usr_no           INT UNSIGNED                          NOT NULL COMMENT '사용자번호',
    sub_yn           TINYINT(1)  DEFAULT 1                NOT NULL COMMENT '구독여부',
    sub_dtm          DATETIME                              NULL COMMENT '구독일시',
    unsub_dtm        DATETIME                              NULL COMMENT '구독해지일시',
    CONSTRAINT tmd_nwsltrsubm_ux01 UNIQUE (nws_ltr_mstr_id, usr_no),
    CONSTRAINT tmd_nwsltrsubm_fk01 FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrsubm_fk02 FOREIGN KEY (usr_no) REFERENCES tmd_usrm (usr_no),
    CONSTRAINT tmd_nwsltrsubm_ck01 CHECK (del_yn IN ('N', 'Y'))
) COMMENT 'TMD_뉴스레터구독마스터';

CREATE INDEX tmd_nwsltrsubm_ix01 ON tmd_nwsltrsubm (nws_ltr_mstr_id, sub_yn, del_yn);
CREATE INDEX tmd_nwsltrsubm_ix02 ON tmd_nwsltrsubm (usr_no, del_yn);

/* =========================================================
 * 3) REVISION TABLES
 * ========================================================= */

CREATE TABLE tmd_nwsltrusrrvsnm
(
    nws_ltr_usr_rvsn_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn              CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_org_iss_id  BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터원본이슈ID',
    ownr_usr_no         INT UNSIGNED                          NOT NULL COMMENT '소유자사용자번호',
    rvsn_stat_cd        VARCHAR(20)                           NOT NULL COMMENT '편집상태(EDIT_DRAFT,EDIT_SAVED)',
    rvsn_no             BIGINT UNSIGNED                       NOT NULL COMMENT '리비전번호(1 이상)',
    snpsht_base_type    VARCHAR(30)                           NOT NULL COMMENT '스냅샷기준유형(ORIGINAL,REVISION_SNAPSHOT)',
    snpsht_no           BIGINT UNSIGNED                       NULL COMMENT '최신스냅샷번호',
    dlta_no             BIGINT UNSIGNED                       NOT NULL COMMENT '마지막델타번호',
    fnsv_dtm            DATETIME                              NULL COMMENT '최종저장일시',
    drft_strg_dtm       DATETIME                              NULL COMMENT '최근임시저장일시',
    CONSTRAINT tmd_nwsltrusrrvsnm_ux01 UNIQUE (nws_ltr_org_iss_id, ownr_usr_no),
    CONSTRAINT tmd_nwsltrusrrvsnm_fk01 FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrusrrvsnm_fk02 FOREIGN KEY (ownr_usr_no) REFERENCES tmd_usrm (usr_no),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck02 CHECK (rvsn_stat_cd IN ('EDIT_DRAFT', 'EDIT_SAVED')),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck03 CHECK (rvsn_no > 0),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck04 CHECK (snpsht_base_type IN ('ORIGINAL', 'REVISION_SNAPSHOT')),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck05 CHECK (
        (snpsht_base_type = 'ORIGINAL' AND snpsht_no IS NULL)
            OR (snpsht_base_type = 'REVISION_SNAPSHOT' AND snpsht_no IS NOT NULL)
    ),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck06 CHECK (snpsht_no IS NULL OR snpsht_no <= rvsn_no),
    CONSTRAINT tmd_nwsltrusrrvsnm_ck07 CHECK (dlta_no > 0 AND dlta_no <= rvsn_no)
) COMMENT 'TMD_뉴스레터사용자리비전마스터';

CREATE INDEX tmd_nwsltrusrrvsnm_ix01 ON tmd_nwsltrusrrvsnm (nws_ltr_org_iss_id, del_yn);
CREATE INDEX tmd_nwsltrusrrvsnm_ix02 ON tmd_nwsltrusrrvsnm (ownr_usr_no, rvsn_stat_cd, del_yn);

CREATE TABLE tmd_nwsltrusrrvsndltah
(
    nws_ltr_usr_rvsn_dlta_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전델타ID' PRIMARY KEY,
    crt_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                   CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_usr_rvsn_id      BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터사용자리비전ID',
    dlta_no                  BIGINT UNSIGNED                       NOT NULL COMMENT '델타번호(리비전 내 단조증가)',
    base_rvsn_no             BIGINT UNSIGNED                       NULL COMMENT '요청기준리비전번호(revisionNo, optional)',
    req_type                 VARCHAR(10)                           NOT NULL COMMENT '요청유형(DRAFT,SAVE)',
    dlta_json                JSON                                  NOT NULL COMMENT '델타JSON(JSON Patch 배열)',
    CONSTRAINT tmd_nwsltrusrrvsndltah_ux01 UNIQUE (nws_ltr_usr_rvsn_id, dlta_no),
    CONSTRAINT tmd_nwsltrusrrvsndltah_fk01 FOREIGN KEY (nws_ltr_usr_rvsn_id) REFERENCES tmd_nwsltrusrrvsnm (nws_ltr_usr_rvsn_id),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck02 CHECK (dlta_no > 0),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck03 CHECK (base_rvsn_no IS NULL OR base_rvsn_no >= 0),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck04 CHECK (req_type IN ('DRAFT', 'SAVE'))
) COMMENT 'TMD_뉴스레터사용자리비전델타이력';

CREATE INDEX tmd_nwsltrusrrvsndltah_ix01 ON tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, dlta_no, del_yn);
CREATE INDEX tmd_nwsltrusrrvsndltah_ix02 ON tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, req_type, dlta_no, del_yn);

CREATE TABLE tmd_nwsltrusrrvsnsnpshth
(
    nws_ltr_usr_rvsn_snpsht_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전스냅샷ID' PRIMARY KEY,
    crt_usr_logn_id            VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id            VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                     CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    nws_ltr_usr_rvsn_id        BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터사용자리비전ID',
    snpsht_no                  BIGINT UNSIGNED                       NOT NULL COMMENT '스냅샷번호(리비전 내 단조증가)',
    base_dlta_no               BIGINT UNSIGNED                       NOT NULL COMMENT '기준델타번호(복원 시작점)',
    snpsht_json                JSON                                  NOT NULL COMMENT '스냅샷JSON',
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ux01 UNIQUE (nws_ltr_usr_rvsn_id, snpsht_no),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_fk01 FOREIGN KEY (nws_ltr_usr_rvsn_id) REFERENCES tmd_nwsltrusrrvsnm (nws_ltr_usr_rvsn_id),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_fk02 FOREIGN KEY (nws_ltr_usr_rvsn_id, base_dlta_no) REFERENCES tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, dlta_no),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck02 CHECK (snpsht_no > 0),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck03 CHECK (base_dlta_no > 0)
) COMMENT 'TMD_뉴스레터사용자리비전스냅샷';

CREATE INDEX tmd_nwsltrusrrvsnsnpshth_ix01 ON tmd_nwsltrusrrvsnsnpshth (nws_ltr_usr_rvsn_id, snpsht_no DESC, del_yn);
CREATE INDEX tmd_nwsltrusrrvsnsnpshth_ix02 ON tmd_nwsltrusrrvsnsnpshth (nws_ltr_usr_rvsn_id, base_dlta_no, del_yn);
