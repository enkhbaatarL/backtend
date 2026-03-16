-- Newsletter User Revision Delta DDL Draft (DBA)
-- Date: 2026-03-12
-- Logical name: newsletter_user_revision_delta
-- Physical table: tmd_nwsltrusrrvsndltah
-- Dependency: tmd_nwsltrusrrvsnm must exist first
-- Rollout: DDL -> app -> verify
-- Policy: append-only delta history, revision save transaction must atomically insert delta and update revision pointer

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nwsltrusrrvsnm', 'tmd_nwsltrusrrvsndltah');

-- duplicate dlta_no check (when table already exists)
SELECT d.nws_ltr_usr_rvsn_id, d.dlta_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsndltah d
GROUP BY d.nws_ltr_usr_rvsn_id, d.dlta_no
HAVING COUNT(*) > 1;

-- orphan revision check (when table already exists)
SELECT d.nws_ltr_usr_rvsn_dlta_id
FROM tmd_nwsltrusrrvsndltah d
LEFT JOIN tmd_nwsltrusrrvsnm r ON r.nws_ltr_usr_rvsn_id = d.nws_ltr_usr_rvsn_id
WHERE r.nws_ltr_usr_rvsn_id IS NULL;

/* =========================================================
 * 1) APPLY - CREATE TABLE
 * ========================================================= */

CREATE TABLE tmd_nwsltrusrrvsndltah
(
    nws_ltr_usr_rvsn_dlta_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전델타ID'
        PRIMARY KEY,
    crt_usr_logn_id         VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                 DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id         VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                 DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                  CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_usr_rvsn_id      BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터사용자리비전ID',
    dlta_no                BIGINT UNSIGNED                        NOT NULL COMMENT '델타번호(리비전 내 단조증가)',
    base_rvsn_no        BIGINT UNSIGNED                        NULL COMMENT '요청기준리비전번호(revisionNo, optional)',
    req_type            VARCHAR(10)                            NOT NULL COMMENT '요청유형(DRAFT,SAVE)',
    dlta_json             JSON                                   NOT NULL COMMENT '델타JSON(JSON Patch 배열)',

    CONSTRAINT tmd_nwsltrusrrvsndltah_ux01 UNIQUE (nws_ltr_usr_rvsn_id, dlta_no),

    CONSTRAINT tmd_nwsltrusrrvsndltah_fk01
        FOREIGN KEY (nws_ltr_usr_rvsn_id) REFERENCES tmd_nwsltrusrrvsnm (nws_ltr_usr_rvsn_id),

    CONSTRAINT tmd_nwsltrusrrvsndltah_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck02 CHECK (dlta_no > 0),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck03 CHECK (base_rvsn_no IS NULL OR base_rvsn_no >= 0),
    CONSTRAINT tmd_nwsltrusrrvsndltah_ck04 CHECK (req_type IN ('DRAFT', 'SAVE'))
)
    COMMENT 'TMD_뉴스레터사용자리비전델타이력';

-- replay query by revision + delta range
CREATE INDEX tmd_nwsltrusrrvsndltah_ix01
    ON tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, dlta_no, del_yn);

-- request type analysis / troubleshooting
CREATE INDEX tmd_nwsltrusrrvsndltah_ix02
    ON tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, req_type, dlta_no, del_yn);

/* =========================================================
 * 2) BACKFILL - N/A (new table)
 * ========================================================= */

-- 신규 테이블이므로 mandatory backfill 없음.
-- 과거 리비전은 필요 시 운영 정책에 따라 baseline delta 재생성 배치로 이관.

/* =========================================================
 * 3) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrusrrvsndltah;

SELECT COUNT(*) AS active_cnt
FROM tmd_nwsltrusrrvsndltah
WHERE del_yn = 'N';

SELECT nws_ltr_usr_rvsn_id, dlta_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsndltah
GROUP BY nws_ltr_usr_rvsn_id, dlta_no
HAVING COUNT(*) > 1;

EXPLAIN
SELECT d.dlta_no, d.req_type, d.dlta_json
FROM tmd_nwsltrusrrvsndltah d
WHERE d.nws_ltr_usr_rvsn_id = 1001
  AND d.dlta_no > 50
  AND d.del_yn = 'N'
ORDER BY d.dlta_no ASC
LIMIT 200;

/* =========================================================
 * 4) ROLLBACK
 * ========================================================= */

-- rollback 1: only before app switches to this table
-- DROP TABLE IF EXISTS tmd_nwsltrusrrvsndltah;

-- rollback 2: keep table but remove migration test data only (if any policy-based inserts exist)
-- DELETE FROM tmd_nwsltrusrrvsndltah WHERE crt_usr_logn_id = 'MIGRATION_20260312';
