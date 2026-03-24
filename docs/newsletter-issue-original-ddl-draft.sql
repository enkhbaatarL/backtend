-- Newsletter Original Issue DDL Draft (DBA)
-- Date: 2026-03-12
-- Logical name: newsletter_issue_original
-- Physical table: tmd_nwsltrorgissm
-- Rollout: DDL -> app
-- Note: Backfill snapshot quality enhancement is deferred by request.

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nws_ltr_mstr', 'tmd_nws_ltr_pub_hist', 'tmd_nwsltrorgissm');

SELECT h.nws_ltr_mstr_id, h.issno, COUNT(*) AS dup_cnt
FROM tmd_nws_ltr_pub_hist h
WHERE h.del_yn = 'N'
GROUP BY h.nws_ltr_mstr_id, h.issno
HAVING COUNT(*) > 1;

/* =========================================================
 * 1) APPLY - CREATE TABLE
 * ========================================================= */

CREATE TABLE tmd_nwsltrorgissm
(
    nws_ltr_org_iss_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈ID'
        PRIMARY KEY,
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

    CONSTRAINT tmd_nwsltrorgissm_fk01
        FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrorgissm_fk02
        FOREIGN KEY (src_nws_ltr_pub_id) REFERENCES tmd_nws_ltr_pub_hist (nws_ltr_pub_id),

    CONSTRAINT tmd_nwsltrorgissm_ck01
        CHECK (pub_stat_cd IN ('PUBLISHING', 'PUBLISHED', 'SUSPENDED')),
    CONSTRAINT tmd_nwsltrorgissm_ck02
        CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgissm_ck03
        CHECK (pub_dtm IS NOT NULL OR pub_stat_cd <> 'PUBLISHED')
)
    COMMENT 'TMD_뉴스레터발행원본기본';

CREATE INDEX tmd_nwsltrorgissm_ix01
    ON tmd_nwsltrorgissm (nws_ltr_mstr_id, pub_stat_cd, del_yn, pub_dtm);

/* =========================================================
 * 2) BACKFILL - FORWARD (minimal placeholder snapshot)
 * ========================================================= */

INSERT INTO tmd_nwsltrorgissm
(
    nws_ltr_mstr_id,
    issno,
    src_nws_ltr_pub_id,
    pub_stat_cd,
    pub_dtm,
    org_snpsht_json,
    crt_usr_logn_id,
    mod_usr_logn_id
)
SELECT h.nws_ltr_mstr_id,
       h.issno,
       h.nws_ltr_pub_id,
       CASE h.pub_stat_cd
           WHEN 0 THEN 'PUBLISHING'
           WHEN 1 THEN 'PUBLISHED'
           WHEN 2 THEN 'SUSPENDED'
           ELSE 'SUSPENDED'
           END AS pub_stat_cd,
       h.pub_dtm,
       JSON_OBJECT(
               'source', 'tmd_nws_ltr_pub_hist',
               'newsletterPublishId', h.nws_ltr_pub_id,
               'newsletterTitle', h.nws_ltr_titl,
               'emailSubject', h.eml_titl
       ) AS org_snpsht_json,
       'MIGRATION_20260312',
       'MIGRATION_20260312'
FROM tmd_nws_ltr_pub_hist h
WHERE h.del_yn = 'N'
ON DUPLICATE KEY UPDATE
    pub_stat_cd = VALUES(pub_stat_cd),
    pub_dtm = VALUES(pub_dtm),
    org_snpsht_json = VALUES(org_snpsht_json),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

/* =========================================================
 * 4) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrorgissm;

SELECT COUNT(*) AS src_cnt
FROM tmd_nws_ltr_pub_hist h
WHERE h.del_yn = 'N';

SELECT COUNT(*) AS dst_cnt
FROM tmd_nwsltrorgissm o
WHERE o.del_yn = 'N';

SELECT COUNT(*) AS null_snapshot_cnt
FROM tmd_nwsltrorgissm o
WHERE o.org_snpsht_json IS NULL;

SELECT o.nws_ltr_mstr_id, o.issno, COUNT(*) AS dup_cnt
FROM tmd_nwsltrorgissm o
GROUP BY o.nws_ltr_mstr_id, o.issno
HAVING COUNT(*) > 1;

/* =========================================================
 * 5) ROLLBACK
 * ========================================================= */

-- 롤백 1: 애플리케이션이 신규 테이블 읽기/쓰기 미사용 상태임을 확인 후 수행
-- 롤백 2: 데이터만 되돌릴 경우
-- DELETE FROM tmd_nwsltrorgissm WHERE crt_usr_logn_id = 'MIGRATION_20260312';

-- 롤백 3: 전체 철회
-- DROP TABLE IF EXISTS tmd_nwsltrorgissm;
