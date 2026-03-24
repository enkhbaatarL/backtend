-- Newsletter Template V2 DDL Draft (DBA)
-- Date: 2026-03-23
-- Logical name: newsletter_template_v2
-- Physical table: tmd_nwsltrtmpltm / tmd_nwsltrtmpltsctn / tmd_nwsltrtmpltsctnfld / tmd_nwsltrtmpltblk / tmd_nwsltrtmpltblkfld
-- Rollout: DDL -> app read-path switch -> seed/backfill -> legacy block-field cleanup
-- Boundary note:
--   - This DDL defines storage truth only.
--   - This revision introduces a physical section-level field definition store for
--     `section.fields` / `section.fieldDefs`.
--   - `tmd_nwsltrtmpltsctnfld` is the source of truth for section-owned field definition.
--   - `tmd_nwsltrtmpltblkfld` must keep renderer-owned field definition only.
-- Policy:
--   - NewsletterTemplateV2 is versioned per master
--   - source of truth for current active template version is tmd_nws_ltr_mstr.nws_ltr_tmplt_id
--   - publish-time fixed template version is tmd_nws_ltr_pub_hist.nws_ltr_tmplt_id
--   - current version must not be derived indirectly; use `tmd_nws_ltr_mstr.nws_ltr_tmplt_id`
--   - issue-state physical tables are not part of the current active V2 model
--   - section field value/state table is not introduced in this change
--   - state-only metadata keys such as `stateScoped` / `stateMutable` are not used

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tmd_nws_ltr_mstr',
      'tmd_nws_ltr_pub_hist',
      'tmd_nwsltrtmpltm',
      'tmd_nwsltrtmpltsctn',
      'tmd_nwsltrtmpltsctnfld',
      'tmd_nwsltrtmpltblk',
      'tmd_nwsltrtmpltblkfld'
  );

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
      (table_name = 'tmd_nws_ltr_mstr' AND column_name IN ('nws_ltr_tmplt_id'))
      OR (table_name = 'tmd_nws_ltr_pub_hist' AND column_name IN ('nws_ltr_tmplt_id'))
      OR (table_name = 'tmd_nwsltrtmpltm' AND column_name IN ('nws_ltr_tmplt_id', 'nws_ltr_mstr_id'))
  );

SELECT s.sctn_cd,
       b.blk_cd,
       f.fld_key,
       f.del_yn
FROM tmd_nwsltrtmpltblkfld f
         JOIN tmd_nwsltrtmpltblk b
              ON b.nws_ltr_tmplt_blk_id = f.nws_ltr_tmplt_blk_id
         JOIN tmd_nwsltrtmpltsctn s
              ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE f.del_yn = 'N'
  AND (
    (s.sctn_cd = 'NEWS_FLOW' AND b.blk_cd = 'NEWS_FLOW_CHART' AND f.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND b.blk_cd = 'REFERENCE_HEADER' AND f.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND b.blk_cd = 'FOOTER_NOTICE' AND f.fld_key IN ('footerContent'))
    )
ORDER BY s.sctn_cd, b.blk_cd, f.fld_key;

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
 * 1-A) APPLY - EXISTING TABLE ALIGNMENT
 * ========================================================= */

ALTER TABLE tmd_nws_ltr_mstr
    MODIFY COLUMN nws_ltr_tmplt_id BIGINT UNSIGNED NOT NULL COMMENT '현재활성뉴스레터템플릿버전ID';

ALTER TABLE tmd_nws_ltr_pub_hist
    MODIFY COLUMN nws_ltr_tmplt_id BIGINT UNSIGNED NOT NULL COMMENT '발행시점뉴스레터템플릿버전ID';

-- Existing rollout case: if tmd_nwsltrtmpltm already exists from an older draft,
-- align it to versioned-template semantics before app rollout.
ALTER TABLE tmd_nwsltrtmpltm

ALTER TABLE tmd_nwsltrtmpltm
    DROP INDEX tmd_nwsltrtmpltm_ux01;

-- === 컬럼 삭제: schm_ver, tmplt_ver_no ===

-- tmd_nwsltrtmpltm
ALTER TABLE tmd_nwsltrtmpltm DROP INDEX tmd_nwsltrtmpltm_ux01;
ALTER TABLE tmd_nwsltrtmpltm DROP COLUMN schm_ver;
ALTER TABLE tmd_nwsltrtmpltm DROP COLUMN tmplt_ver_no;

-- tmd_nwsltrorgissm
ALTER TABLE tmd_nwsltrorgissm DROP COLUMN schm_ver;

-- tmd_nwsltrusrrvsnsnpshth
ALTER TABLE tmd_nwsltrusrrvsnsnpshth DROP COLUMN schm_ver;

-- `tmd_nws_ltr_pub_hist.nws_ltr_tmplt_id` legacy FK cleanup/rebind is environment-specific.
-- Remove any legacy FK to V1 template before switching this column to V2 template-version meaning.

/* =========================================================
 * 2) BACKFILL / CLEANUP
 * ========================================================= */

-- Run this section only after the application reads `tmd_nwsltrtmpltsctnfld`.
-- This backfill is idempotent if the unique key is respected and existing rows are cleaned first.

INSERT INTO tmd_nwsltrtmpltsctnfld
(
    nws_ltr_tmplt_sctn_id,
    fld_key,
    fld_nm,
    fld_ord,
    is_editable_yn,
    is_required_yn,
    fld_meta,
    crt_usr_logn_id,
    mod_usr_logn_id
)
SELECT b.nws_ltr_tmplt_sctn_id,
       f.fld_key,
       f.fld_nm,
       f.fld_ord,
       f.is_editable_yn,
       f.is_required_yn,
       f.fld_meta,
       'SYSTEM',
       'SYSTEM'
FROM tmd_nwsltrtmpltblkfld f
         JOIN tmd_nwsltrtmpltblk b
              ON b.nws_ltr_tmplt_blk_id = f.nws_ltr_tmplt_blk_id
         JOIN tmd_nwsltrtmpltsctn s
              ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE f.del_yn = 'N'
  AND (
    (s.sctn_cd = 'NEWS_FLOW' AND b.blk_cd = 'NEWS_FLOW_CHART' AND f.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND b.blk_cd = 'REFERENCE_HEADER' AND f.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND b.blk_cd = 'FOOTER_NOTICE' AND f.fld_key IN ('footerContent'))
    );

-- Legacy section-owned block-field rows must be soft-deleted after section-field rows are verified.
UPDATE tmd_nwsltrtmpltblkfld f
    JOIN tmd_nwsltrtmpltblk b
        ON b.nws_ltr_tmplt_blk_id = f.nws_ltr_tmplt_blk_id
    JOIN tmd_nwsltrtmpltsctn s
        ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
SET f.del_yn = 'Y',
    f.mod_usr_logn_id = 'SYSTEM',
    f.mod_dtm = CURRENT_TIMESTAMP
WHERE f.del_yn = 'N'
  AND (
    (s.sctn_cd = 'NEWS_FLOW' AND b.blk_cd = 'NEWS_FLOW_CHART' AND f.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND b.blk_cd = 'REFERENCE_HEADER' AND f.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND b.blk_cd = 'FOOTER_NOTICE' AND f.fld_key IN ('footerContent'))
    );

/* =========================================================
 * 3) VERIFY
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrtmpltm;
SHOW CREATE TABLE tmd_nwsltrtmpltsctn;
SHOW CREATE TABLE tmd_nwsltrtmpltsctnfld;
SHOW CREATE TABLE tmd_nwsltrtmpltblk;
SHOW CREATE TABLE tmd_nwsltrtmpltblkfld;
SHOW CREATE TABLE tmd_nws_ltr_mstr;
SHOW CREATE TABLE tmd_nws_ltr_pub_hist;

SELECT s.sctn_cd,
       sf.fld_key,
       sf.fld_ord,
       sf.is_editable_yn,
       sf.is_required_yn,
       sf.del_yn
FROM tmd_nwsltrtmpltsctnfld sf
         JOIN tmd_nwsltrtmpltsctn s
              ON s.nws_ltr_tmplt_sctn_id = sf.nws_ltr_tmplt_sctn_id
WHERE sf.del_yn = 'N'
ORDER BY s.sctn_cd, sf.fld_ord, sf.fld_key;

SELECT s.sctn_cd,
       b.blk_cd,
       f.fld_key,
       f.del_yn
FROM tmd_nwsltrtmpltblkfld f
         JOIN tmd_nwsltrtmpltblk b
              ON b.nws_ltr_tmplt_blk_id = f.nws_ltr_tmplt_blk_id
         JOIN tmd_nwsltrtmpltsctn s
              ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE (
    (s.sctn_cd = 'NEWS_FLOW' AND b.blk_cd = 'NEWS_FLOW_CHART' AND f.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND b.blk_cd = 'REFERENCE_HEADER' AND f.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND b.blk_cd = 'FOOTER_NOTICE' AND f.fld_key IN ('footerContent'))
    )
ORDER BY s.sctn_cd, b.blk_cd, f.fld_key;

/* =========================================================
 * 4) ROLLBACK
 * ========================================================= */

-- Roll back cleanup first if legacy block-field rows were soft-deleted.
UPDATE tmd_nwsltrtmpltblkfld f
    JOIN tmd_nwsltrtmpltblk b
        ON b.nws_ltr_tmplt_blk_id = f.nws_ltr_tmplt_blk_id
    JOIN tmd_nwsltrtmpltsctn s
        ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
SET f.del_yn = 'N',
    f.mod_usr_logn_id = 'SYSTEM',
    f.mod_dtm = CURRENT_TIMESTAMP
WHERE f.del_yn = 'Y'
  AND (
    (s.sctn_cd = 'NEWS_FLOW' AND b.blk_cd = 'NEWS_FLOW_CHART' AND f.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND b.blk_cd = 'REFERENCE_HEADER' AND f.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND b.blk_cd = 'FOOTER_NOTICE' AND f.fld_key IN ('footerContent'))
    );

DELETE sf
FROM tmd_nwsltrtmpltsctnfld sf
         JOIN tmd_nwsltrtmpltsctn s
              ON s.nws_ltr_tmplt_sctn_id = sf.nws_ltr_tmplt_sctn_id
WHERE (
    (s.sctn_cd = 'NEWS_FLOW' AND sf.fld_key IN ('title', 'aggregationTime'))
        OR (s.sctn_cd IN ('REFERENCE1', 'REFERENCE2', 'REFERENCE3', 'REFERENCE4') AND sf.fld_key IN ('title', 'description'))
        OR (s.sctn_cd = 'FOOTER' AND sf.fld_key IN ('footerContent'))
    );

DROP TABLE IF EXISTS tmd_nwsltrtmpltsctnfld;

DROP TABLE IF EXISTS tmd_nwsltrtmpltblkfld;
DROP TABLE IF EXISTS tmd_nwsltrtmpltblk;
DROP TABLE IF EXISTS tmd_nwsltrtmpltsctn;
DROP TABLE IF EXISTS tmd_nwsltrtmpltm;

-- Existing table rollback for column meaning changes must be handled separately.
-- Do not blindly revert tmd_nws_ltr_mstr / tmd_nws_ltr_pub_hist after app rollout.

/* =========================================================
 * 5) DBA ACTION / DEPLOYMENT NOTE
 * ========================================================= */

-- 1. Apply `CREATE TABLE tmd_nwsltrtmpltsctnfld` first.
-- 2. Deploy the application that reads section field definitions from the new table.
-- 3. Run backfill INSERT and VERIFY queries.
-- 4. Soft-delete legacy section-owned rows in `tmd_nwsltrtmpltblkfld`.
-- 5. Keep block rows (`NEWS_FLOW_CHART`, `FOOTER_NOTICE`) unless separately reviewed.
-- 6. `REFERENCE_HEADER` block-row cleanup should be handled in a separate SQL slice after runtime verification.
