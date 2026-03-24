-- Newsletter User Revision Snapshot DDL Draft (DBA)
-- Date: 2026-03-12
-- Logical name: newsletter_user_revision_snapshot
-- Physical table: tmd_nwsltrusrrvsnsnpshth
-- Dependency: tmd_nwsltrusrrvsnm, tmd_nwsltrusrrvsndltah must exist first
-- Rollout: DDL -> app -> verify
-- Policy: snapshot insert only when snapshotPayload is provided (optional snapshot policy preserved)

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nwsltrusrrvsnm', 'tmd_nwsltrusrrvsndltah', 'tmd_nwsltrusrrvsnsnpshth');

-- duplicate snpsht_no check (when table already exists)
SELECT s.nws_ltr_usr_rvsn_id, s.snpsht_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsnsnpshth s
GROUP BY s.nws_ltr_usr_rvsn_id, s.snpsht_no
HAVING COUNT(*) > 1;

-- orphan revision check (when table already exists)
SELECT s.nws_ltr_usr_rvsn_snpsht_id
FROM tmd_nwsltrusrrvsnsnpshth s
LEFT JOIN tmd_nwsltrusrrvsnm r ON r.nws_ltr_usr_rvsn_id = s.nws_ltr_usr_rvsn_id
WHERE r.nws_ltr_usr_rvsn_id IS NULL;

-- orphan base delta check (when table already exists)
SELECT s.nws_ltr_usr_rvsn_snpsht_id
FROM tmd_nwsltrusrrvsnsnpshth s
LEFT JOIN tmd_nwsltrusrrvsndltah d
  ON d.nws_ltr_usr_rvsn_id = s.nws_ltr_usr_rvsn_id
 AND d.dlta_no = s.base_dlta_no
WHERE d.dlta_no IS NULL;

/* =========================================================
 * 1) APPLY - CREATE TABLE
 * ========================================================= */

CREATE TABLE tmd_nwsltrusrrvsnsnpshth
(
    nws_ltr_usr_rvsn_snpsht_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전스냅샷ID'
        PRIMARY KEY,
    crt_usr_logn_id        VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id        VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                 CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_usr_rvsn_id     BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터사용자리비전ID',
    snpsht_no            BIGINT UNSIGNED                        NOT NULL COMMENT '스냅샷번호(리비전 내 단조증가)',
    base_dlta_no          BIGINT UNSIGNED                        NOT NULL COMMENT '기준델타번호(복원 시작점)',
    snpsht_json            JSON                                   NOT NULL COMMENT '스냅샷JSON',

    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ux01 UNIQUE (nws_ltr_usr_rvsn_id, snpsht_no),

    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_fk01
        FOREIGN KEY (nws_ltr_usr_rvsn_id) REFERENCES tmd_nwsltrusrrvsnm (nws_ltr_usr_rvsn_id),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_fk02
        FOREIGN KEY (nws_ltr_usr_rvsn_id, base_dlta_no) REFERENCES tmd_nwsltrusrrvsndltah (nws_ltr_usr_rvsn_id, dlta_no),

    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck02 CHECK (snpsht_no > 0),
    CONSTRAINT tmd_nwsltrusrrvsnsnpshth_ck03 CHECK (base_dlta_no > 0)
)
    COMMENT 'TMD_뉴스레터사용자리비전스냅샷';

-- latest snapshot lookup
CREATE INDEX tmd_nwsltrusrrvsnsnpshth_ix01
    ON tmd_nwsltrusrrvsnsnpshth (nws_ltr_usr_rvsn_id, snpsht_no DESC, del_yn);

-- replay startpoint lookup
CREATE INDEX tmd_nwsltrusrrvsnsnpshth_ix02
    ON tmd_nwsltrusrrvsnsnpshth (nws_ltr_usr_rvsn_id, base_dlta_no, del_yn);

/* =========================================================
 * 2) BACKFILL - N/A (new table)
 * ========================================================= */

-- Snapshot policy is optional.
-- No mandatory backfill for existing revisions.
-- Snapshots are inserted only when snapshotPayload is provided by client save request.

/* =========================================================
 * 3) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrusrrvsnsnpshth;

SELECT COUNT(*) AS active_cnt
FROM tmd_nwsltrusrrvsnsnpshth
WHERE del_yn = 'N';

SELECT nws_ltr_usr_rvsn_id, snpsht_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsnsnpshth
GROUP BY nws_ltr_usr_rvsn_id, snpsht_no
HAVING COUNT(*) > 1;

EXPLAIN
SELECT s.nws_ltr_usr_rvsn_snpsht_id, s.snpsht_no, s.base_dlta_no, s.snpsht_json
FROM tmd_nwsltrusrrvsnsnpshth s
WHERE s.nws_ltr_usr_rvsn_id = 1001
  AND s.del_yn = 'N'
ORDER BY s.snpsht_no DESC
LIMIT 1;

/* =========================================================
 * 4) ROLLBACK
 * ========================================================= */

-- rollback 1: only before app switches to this table
-- DROP TABLE IF EXISTS tmd_nwsltrusrrvsnsnpshth;

-- rollback 2: keep table but remove migration test data only (if any policy-based inserts exist)
-- DELETE FROM tmd_nwsltrusrrvsnsnpshth WHERE crt_usr_logn_id = 'MIGRATION_20260312';
