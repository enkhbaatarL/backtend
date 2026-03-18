-- Newsletter Issue State DDL Draft (DBA)
-- Date: 2026-03-17
-- Logical name: newsletter_issue_section_state / newsletter_issue_block_state
-- Physical table: tmd_nwsltrorgisssctnstat / tmd_nwsltrorgissblkstat
-- Dependency: tmd_nwsltrorgissm, tmd_nwsltrtmpltsctn, tmd_nwsltrtmpltblk must exist first
-- Rollout: DDL -> app(preview/write path) -> publish path -> verify
-- Policy:
--   - original published detail remains snapshot-first (`tmd_nwsltrorgissm.org_snpsht_json`)
--   - issue state is a pre-publication input and does not replace published snapshot reads
--   - each issue block state row is treated as one rendered block instance row
--   - row identity is `nws_ltr_org_iss_blk_stat_id`; `blk_ord` is an ordering key, not an update identity
--   - null-safe rollout required; no mandatory backfill for historical published issues

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tmd_nwsltrorgissm',
      'tmd_nwsltrtmpltsctn',
      'tmd_nwsltrtmpltblk',
      'tmd_nwsltrorgisssctnstat',
      'tmd_nwsltrorgissblkstat'
  );

-- orphan template block check
SELECT b.nws_ltr_tmplt_blk_id
FROM tmd_nwsltrtmpltblk b
LEFT JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_sctn_id IS NULL;

-- duplicate block order risk check for rerun / existing table case
SELECT nws_ltr_org_iss_sctn_stat_id, blk_ord, COUNT(*) AS cnt
FROM tmd_nwsltrorgissblkstat
GROUP BY nws_ltr_org_iss_sctn_stat_id, blk_ord
HAVING COUNT(*) > 1;

/* =========================================================
 * 1) APPLY - CREATE TABLE
 * ========================================================= */

CREATE TABLE tmd_nwsltrorgisssctnstat
(
    nws_ltr_org_iss_sctn_stat_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈섹션상태ID'
        PRIMARY KEY,
    crt_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                      CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_org_iss_id          BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터원본이슈ID',
    nws_ltr_tmplt_sctn_id       BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터템플릿섹션ID',
    sctn_ord                    INT UNSIGNED                           NOT NULL COMMENT '섹션순서',
    vsblty_yn                   CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    sctn_titl                   VARCHAR(200)                           NULL COMMENT '섹션제목오버라이드',
    tgt_cmpny_no                INT UNSIGNED                           NULL COMMENT '대상고객사번호',
    sctn_stat_json              JSON                                   NULL COMMENT '섹션상태JSON',

    CONSTRAINT tmd_nwsltrorgisssctnstat_ux01 UNIQUE (nws_ltr_org_iss_id, nws_ltr_tmplt_sctn_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ux02 UNIQUE (nws_ltr_org_iss_id, sctn_ord),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ux03 UNIQUE (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id),

    CONSTRAINT tmd_nwsltrorgisssctnstat_fk01
        FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_fk02
        FOREIGN KEY (nws_ltr_tmplt_sctn_id) REFERENCES tmd_nwsltrtmpltsctn (nws_ltr_tmplt_sctn_id),

    CONSTRAINT tmd_nwsltrorgisssctnstat_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ck02 CHECK (vsblty_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터원본이슈섹션상태';

CREATE INDEX tmd_nwsltrorgisssctnstat_ix01
    ON tmd_nwsltrorgisssctnstat (nws_ltr_org_iss_id, del_yn, sctn_ord);

CREATE TABLE tmd_nwsltrorgissblkstat
(
    nws_ltr_org_iss_blk_stat_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈블록상태ID'
        PRIMARY KEY,
    crt_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                      CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_org_iss_id          BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터원본이슈ID',
    nws_ltr_org_iss_sctn_stat_id BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터원본이슈섹션상태ID',
    nws_ltr_tmplt_blk_id        BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터템플릿블록ID',
    blk_ord                     INT UNSIGNED                           NOT NULL COMMENT '블록순서',
    vsblty_yn                   CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    blk_titl                    VARCHAR(200)                           NULL COMMENT '블록제목오버라이드',
    tgt_cmpny_no                INT UNSIGNED                           NULL COMMENT '대상고객사번호',
    blk_stat_json               JSON                                   NULL COMMENT '블록상태JSON',

    -- repeated rows for the same template block are allowed.
    -- section-local rendering order must still be unique.
    CONSTRAINT tmd_nwsltrorgissblkstat_ux01 UNIQUE (nws_ltr_org_iss_sctn_stat_id, blk_ord),

    CONSTRAINT tmd_nwsltrorgissblkstat_fk01
        FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgissblkstat_fk02
        FOREIGN KEY (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id)
            REFERENCES tmd_nwsltrorgisssctnstat (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgissblkstat_fk03
        FOREIGN KEY (nws_ltr_tmplt_blk_id) REFERENCES tmd_nwsltrtmpltblk (nws_ltr_tmplt_blk_id),

    CONSTRAINT tmd_nwsltrorgissblkstat_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgissblkstat_ck02 CHECK (vsblty_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터원본이슈블록상태';

CREATE INDEX tmd_nwsltrorgissblkstat_ix01
    ON tmd_nwsltrorgissblkstat (nws_ltr_org_iss_sctn_stat_id, del_yn, blk_ord);

CREATE INDEX tmd_nwsltrorgissblkstat_ix02
    ON tmd_nwsltrorgissblkstat (nws_ltr_org_iss_id, del_yn);

CREATE INDEX tmd_nwsltrorgissblkstat_ix03
    ON tmd_nwsltrorgissblkstat (nws_ltr_org_iss_sctn_stat_id, nws_ltr_tmplt_blk_id, del_yn, blk_ord);

/* =========================================================
 * 2) BACKFILL - OPTIONAL / SAFE DEFAULTS ONLY
 * ========================================================= */

-- Historical published issues do not require mandatory state-row backfill.
-- Published original detail must keep reading org_snpsht_json only.
-- Repeated blocks are distinguished by row PK (`nws_ltr_org_iss_blk_stat_id`)
-- and section-local order (`blk_ord`). No separate instance number is used.
-- Write path must update/delete repeated block rows by row PK after creation.
-- `blk_ord` is only a section-local render order and may change during reorder.
--
-- Optional draft-only seed policy:
-- - if preview/write path is enabled for existing non-published draft issues,
--   the application may seed section/block state rows from template defaults
--   on first edit/load.
-- - this is an application concern, not a mandatory DBA backfill step.

/* =========================================================
 * 3) VERIFY
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrorgisssctnstat;
SHOW CREATE TABLE tmd_nwsltrorgissblkstat;

SELECT COUNT(*) AS dup_section_template_cnt
FROM (
    SELECT nws_ltr_org_iss_id, nws_ltr_tmplt_sctn_id, COUNT(*) AS cnt
    FROM tmd_nwsltrorgisssctnstat
    GROUP BY nws_ltr_org_iss_id, nws_ltr_tmplt_sctn_id
    HAVING COUNT(*) > 1
) t;

SELECT COUNT(*) AS dup_block_template_cnt
FROM (
    SELECT nws_ltr_org_iss_sctn_stat_id, blk_ord, COUNT(*) AS cnt
    FROM tmd_nwsltrorgissblkstat
    GROUP BY nws_ltr_org_iss_sctn_stat_id, blk_ord
    HAVING COUNT(*) > 1
) t;

EXPLAIN
SELECT s.nws_ltr_org_iss_sctn_stat_id, s.sctn_ord, s.vsblty_yn
FROM tmd_nwsltrorgisssctnstat s
WHERE s.nws_ltr_org_iss_id = 1001
  AND s.del_yn = 'N'
ORDER BY s.sctn_ord;

EXPLAIN
SELECT b.nws_ltr_org_iss_blk_stat_id, b.blk_ord, b.vsblty_yn
FROM tmd_nwsltrorgissblkstat b
WHERE b.nws_ltr_org_iss_sctn_stat_id = 2001
  AND b.del_yn = 'N'
ORDER BY b.blk_ord;

EXPLAIN
SELECT b.nws_ltr_org_iss_blk_stat_id, b.nws_ltr_tmplt_blk_id, b.blk_ord
FROM tmd_nwsltrorgissblkstat b
WHERE b.nws_ltr_org_iss_sctn_stat_id = 2001
  AND b.nws_ltr_tmplt_blk_id = 3001
  AND b.del_yn = 'N'
ORDER BY b.blk_ord;

-- application-owned integrity check:
-- block must belong to the template section referenced by the section-state row.
SELECT b.nws_ltr_org_iss_blk_stat_id,
       b.nws_ltr_org_iss_sctn_stat_id,
       b.nws_ltr_tmplt_blk_id,
       ss.nws_ltr_tmplt_sctn_id AS section_template_id,
       tb.nws_ltr_tmplt_sctn_id AS block_template_section_id
FROM tmd_nwsltrorgissblkstat b
JOIN tmd_nwsltrorgisssctnstat ss
  ON ss.nws_ltr_org_iss_sctn_stat_id = b.nws_ltr_org_iss_sctn_stat_id
 AND ss.nws_ltr_org_iss_id = b.nws_ltr_org_iss_id
JOIN tmd_nwsltrtmpltblk tb
  ON tb.nws_ltr_tmplt_blk_id = b.nws_ltr_tmplt_blk_id
WHERE ss.nws_ltr_tmplt_sctn_id <> tb.nws_ltr_tmplt_sctn_id;

/* =========================================================
 * 4) ROLLBACK
 * ========================================================= */

-- rollback 1: only before app switches preview/write path to these tables
-- DROP TABLE IF EXISTS tmd_nwsltrorgissblkstat;
-- DROP TABLE IF EXISTS tmd_nwsltrorgisssctnstat;

/* =========================================================
 * 5) DBA ACTION / DEPLOYMENT NOTE
 * ========================================================= */

-- 1. Apply after original issue and template tables exist.
-- 2. Deploy application only after DDL is visible to ddl-auto=validate.
-- 3. Update JPA/entity metadata first so no removed unique key remains in application mapping.
-- 4. Switch preview/write path first; do not switch original detail read path.
-- 5. Preview/write path must treat each block-state row as one block instance row.
-- 6. Repeated rows with the same `nws_ltr_tmplt_blk_id` are ordered by `blk_ord`
--    but must be updated/deleted by row PK.
-- 7. Loader / resolver / assembler must use row-based block-state read models before rollout.
-- 8. Template block ownership under section-state is validated in application service layer.
