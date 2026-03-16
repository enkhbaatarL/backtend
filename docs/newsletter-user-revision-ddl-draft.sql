-- Newsletter User Revision DDL Draft (DBA)
-- Date: 2026-03-12
-- Logical name: newsletter_user_revision
-- Physical table: tmd_nwsltrusrrvsnm
-- Dependency: tmd_nwsltrorgissm must exist first
-- Rollout: DDL -> app -> verify
-- Policy:
--   - original snapshot is mandatory in tmd_nwsltrorgissm.org_snpsht_json
--   - replay delta target window = 8 (tunable range 6~10)

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nwsltrorgissm', 'tmd_usrm', 'tmd_nwsltrusrrvsnm');

-- duplicate check (when table already exists)
SELECT r.nws_ltr_org_iss_id, r.ownr_usr_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsnm r
GROUP BY r.nws_ltr_org_iss_id, r.ownr_usr_no
HAVING COUNT(*) > 1;

-- orphan check (when table already exists)
SELECT r.nws_ltr_usr_rvsn_id
FROM tmd_nwsltrusrrvsnm r
LEFT JOIN tmd_nwsltrorgissm o ON o.nws_ltr_org_iss_id = r.nws_ltr_org_iss_id
WHERE o.nws_ltr_org_iss_id IS NULL;

SELECT r.nws_ltr_usr_rvsn_id
FROM tmd_nwsltrusrrvsnm r
LEFT JOIN tmd_usrm u ON u.usr_no = r.ownr_usr_no
WHERE u.usr_no IS NULL;

/* =========================================================
 * 1) APPLY - CREATE TABLE
 * ========================================================= */

CREATE TABLE tmd_nwsltrusrrvsnm
(
    nws_ltr_usr_rvsn_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터사용자리비전ID'
        PRIMARY KEY,
    crt_usr_logn_id    VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id    VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn             CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_org_iss_id BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터원본이슈ID',
    ownr_usr_no      INT UNSIGNED                           NOT NULL COMMENT '소유자사용자번호',
    rvsn_stat_cd        VARCHAR(20)                            NOT NULL COMMENT '편집상태(EDIT_DRAFT,EDIT_SAVED)',
    rvsn_no        BIGINT UNSIGNED                        NOT NULL COMMENT '리비전번호(1 이상)',
    snpsht_base_type VARCHAR(30)                            NOT NULL COMMENT '스냅샷기준유형(ORIGINAL,REVISION_SNAPSHOT)',

    snpsht_no BIGINT UNSIGNED                        NULL COMMENT '최신스냅샷번호',
    dlta_no      BIGINT UNSIGNED                        NOT NULL COMMENT '마지막델타번호',
    fnsv_dtm          DATETIME                               NULL COMMENT '최종저장일시',
    drft_strg_dtm DATETIME                              NULL COMMENT '최근임시저장일시',

    CONSTRAINT tmd_nwsltrusrrvsnm_ux01 UNIQUE (nws_ltr_org_iss_id, ownr_usr_no),

    CONSTRAINT tmd_nwsltrusrrvsnm_fk01
        FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrusrrvsnm_fk02
        FOREIGN KEY (ownr_usr_no) REFERENCES tmd_usrm (usr_no),

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
)
    COMMENT 'TMD_뉴스레터사용자리비전마스터';

CREATE INDEX tmd_nwsltrusrrvsnm_ix01
    ON tmd_nwsltrusrrvsnm (nws_ltr_org_iss_id, del_yn);

CREATE INDEX tmd_nwsltrusrrvsnm_ix02
    ON tmd_nwsltrusrrvsnm (ownr_usr_no, rvsn_stat_cd, del_yn);

/* =========================================================
 * 2) OPTIMISTIC LOCK UPDATE PATTERN (reference)
 * ========================================================= */

-- expected behavior:
-- 1 row updated: success
-- 0 row updated: REVISION_VERSION_CONFLICT (409)
-- UPDATE tmd_nwsltrusrrvsnm
--    SET rvsn_stat_cd = :editStatus,
--        snpsht_base_type = :snapshotBaseType, -- ORIGINAL | REVISION_SNAPSHOT
--        snpsht_no = :latestSnapshotNo, -- ORIGINAL이면 null
--        dlta_no = :nextDeltaNo,           -- NOT NULL
--        rvsn_no = rvsn_no + 1,
--        mod_usr_logn_id = :actorLoginId,
--        mod_dtm = CURRENT_TIMESTAMP,
--        drft_strg_dtm = CASE WHEN :isDraft = 1 THEN CURRENT_TIMESTAMP ELSE drft_strg_dtm END,
--        fnsv_dtm = CASE WHEN :isFinal = 1 THEN CURRENT_TIMESTAMP ELSE fnsv_dtm END
--  WHERE nws_ltr_usr_rvsn_id = :revisionId
--    AND ownr_usr_no = :actorUserNo
--    AND rvsn_no = :expectedRevisionNo
--    AND del_yn = 'N';

/* =========================================================
 * 3) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrusrrvsnm;

SELECT COUNT(*) AS active_cnt
FROM tmd_nwsltrusrrvsnm
WHERE del_yn = 'N';

SELECT nws_ltr_org_iss_id, ownr_usr_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrusrrvsnm
GROUP BY nws_ltr_org_iss_id, ownr_usr_no
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS invalid_snapshot_base_cnt
FROM tmd_nwsltrusrrvsnm r
WHERE (r.snpsht_base_type = 'ORIGINAL' AND r.snpsht_no IS NOT NULL)
   OR (r.snpsht_base_type = 'REVISION_SNAPSHOT' AND r.snpsht_no IS NULL);

SELECT COUNT(*) AS delta_pointer_orphan_cnt
FROM tmd_nwsltrusrrvsnm r
LEFT JOIN tmd_nwsltrusrrvsndltah d
  ON d.nws_ltr_usr_rvsn_id = r.nws_ltr_usr_rvsn_id
 AND d.dlta_no = r.dlta_no
WHERE d.dlta_no IS NULL;

EXPLAIN
SELECT r.ownr_usr_no, r.rvsn_stat_cd
  FROM tmd_nwsltrusrrvsnm r
  JOIN tmd_nwsltrorgissm o ON o.nws_ltr_org_iss_id = r.nws_ltr_org_iss_id
 WHERE o.nws_ltr_mstr_id = 1
   AND o.issno = 1
   AND r.del_yn = 'N';

/* =========================================================
 * 4) ROLLBACK
 * ========================================================= */

-- rollback 1: only before app switches to this table
-- DROP TABLE IF EXISTS tmd_nwsltrusrrvsnm;

-- rollback 2: keep table but remove migration test data only (if any policy-based inserts exist)
-- DELETE FROM tmd_nwsltrusrrvsnm WHERE crt_usr_logn_id = 'MIGRATION_20260312';
