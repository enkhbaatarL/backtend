-- Newsletter Subscription DDL Draft (DBA)
-- Date: 2026-03-12
-- Logical name: newsletter_subscription
-- Physical table: tmd_nwsltrsubm
-- Dependency: tmd_nws_ltr_mstr, tmd_usrm must exist first
-- Rollout: DDL -> app -> verify
-- Note:
--   - This draft is for DBA review/execution.
--   - Run in each environment schema (dev/stg/prod) with deployment order control.
--   - Application runs with ddl-auto=validate.

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

-- dependency / target table existence check
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nws_ltr_mstr', 'tmd_usrm', 'tmd_nwsltrsubm');

-- duplicate check before unique key apply
SELECT s.nws_ltr_mstr_id, s.usr_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrsubm s
GROUP BY s.nws_ltr_mstr_id, s.usr_no
HAVING COUNT(*) > 1;

-- FK orphan check (when table already exists)
SELECT s.nws_ltr_sub_id, s.nws_ltr_mstr_id
FROM tmd_nwsltrsubm s
LEFT JOIN tmd_nws_ltr_mstr m ON m.nws_ltr_mstr_id = s.nws_ltr_mstr_id
WHERE m.nws_ltr_mstr_id IS NULL;

SELECT s.nws_ltr_sub_id, s.usr_no
FROM tmd_nwsltrsubm s
LEFT JOIN tmd_usrm u ON u.usr_no = s.usr_no
WHERE u.usr_no IS NULL;

/* =========================================================
 * 1) APPLY - CREATE TABLE (new environments)
 * ========================================================= */

CREATE TABLE tmd_nwsltrsubm
(
    nws_ltr_sub_id   BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터구독ID'
        PRIMARY KEY,
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_mstr_id  INT UNSIGNED                           NOT NULL COMMENT '뉴스레터마스터ID',
    usr_no           INT UNSIGNED                           NOT NULL COMMENT '사용자번호',
    sub_yn           TINYINT(1)  DEFAULT 1                 NOT NULL COMMENT '구독여부',
    sub_dtm          DATETIME                               NULL COMMENT '구독일시',
    unsub_dtm        DATETIME                               NULL COMMENT '구독해지일시',

    CONSTRAINT tmd_nwsltrsubm_ux01 UNIQUE (nws_ltr_mstr_id, usr_no),
    CONSTRAINT tmd_nwsltrsubm_fk01
        FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    CONSTRAINT tmd_nwsltrsubm_fk02
        FOREIGN KEY (usr_no) REFERENCES tmd_usrm (usr_no),
    CONSTRAINT tmd_nwsltrsubm_ck01 CHECK (del_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터구독마스터';

CREATE INDEX tmd_nwsltrsubm_ix01
    ON tmd_nwsltrsubm (nws_ltr_mstr_id, sub_yn, del_yn);

CREATE INDEX tmd_nwsltrsubm_ix02
    ON tmd_nwsltrsubm (usr_no, del_yn);

/* =========================================================
 * 1-1) APPLY - ALTER TABLE (existing environments)
 * ========================================================= */

-- execute selectively by environment state; remove already-existing column/constraint operations.
ALTER TABLE tmd_nwsltrsubm
    ADD COLUMN nws_ltr_sub_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '뉴스레터구독ID',
    ADD COLUMN crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    ADD COLUMN crt_dtm DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    ADD COLUMN mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    ADD COLUMN mod_dtm DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    ADD COLUMN del_yn CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    ADD COLUMN nws_ltr_mstr_id INT UNSIGNED NOT NULL COMMENT '뉴스레터마스터ID',
    ADD COLUMN usr_no INT UNSIGNED NOT NULL COMMENT '사용자번호',
    ADD COLUMN sub_yn TINYINT(1) DEFAULT 1 NOT NULL COMMENT '구독여부',
    ADD COLUMN sub_dtm DATETIME NULL COMMENT '구독일시',
    ADD COLUMN unsub_dtm DATETIME NULL COMMENT '구독해지일시';

ALTER TABLE tmd_nwsltrsubm
    ADD CONSTRAINT tmd_nwsltrsubm_ux01 UNIQUE (nws_ltr_mstr_id, usr_no);

ALTER TABLE tmd_nwsltrsubm
    ADD CONSTRAINT tmd_nwsltrsubm_fk01
        FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),
    ADD CONSTRAINT tmd_nwsltrsubm_fk02
        FOREIGN KEY (usr_no) REFERENCES tmd_usrm (usr_no),
    ADD CONSTRAINT tmd_nwsltrsubm_ck01 CHECK (del_yn IN ('N', 'Y'));

CREATE INDEX tmd_nwsltrsubm_ix01
    ON tmd_nwsltrsubm (nws_ltr_mstr_id, sub_yn, del_yn);

CREATE INDEX tmd_nwsltrsubm_ix02
    ON tmd_nwsltrsubm (usr_no, del_yn);

/* =========================================================
 * 2) BACKFILL - FORWARD (optional)
 * ========================================================= */

-- optional policy: set sub_dtm for active subscriptions when null.
UPDATE tmd_nwsltrsubm
SET sub_dtm = COALESCE(sub_dtm, crt_dtm)
WHERE sub_yn = 1
  AND sub_dtm IS NULL;

/* =========================================================
 * 3) POST-CHECK
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrsubm;

SELECT COUNT(*) AS active_cnt
FROM tmd_nwsltrsubm
WHERE del_yn = 'N';

SELECT nws_ltr_mstr_id, usr_no, COUNT(*) AS dup_cnt
FROM tmd_nwsltrsubm
GROUP BY nws_ltr_mstr_id, usr_no
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS orphan_master_cnt
FROM tmd_nwsltrsubm s
LEFT JOIN tmd_nws_ltr_mstr m ON m.nws_ltr_mstr_id = s.nws_ltr_mstr_id
WHERE m.nws_ltr_mstr_id IS NULL;

SELECT COUNT(*) AS orphan_user_cnt
FROM tmd_nwsltrsubm s
LEFT JOIN tmd_usrm u ON u.usr_no = s.usr_no
WHERE u.usr_no IS NULL;

/* =========================================================
 * 4) VERIFY (query plan / behavior)
 * ========================================================= */

-- recipient query explain
EXPLAIN
SELECT DISTINCT u.usr_logn_id AS email
FROM tmd_nwsltrsubm ns
JOIN tmd_usrm u ON u.usr_no = ns.usr_no
WHERE ns.nws_ltr_mstr_id = 1
  AND ns.sub_yn = 1
  AND ns.del_yn = 'N'
  AND u.del_yn = 'N'
  AND u.usr_logn_id IS NOT NULL;

/* =========================================================
 * 5) ROLLBACK
 * ========================================================= */

-- rollback 1: only before app switches to this table
-- DROP TABLE IF EXISTS tmd_nwsltrsubm;

-- rollback 2: keep table, rollback migration-added data only (if policy-based updates were applied)
-- UPDATE tmd_nwsltrsubm
--    SET sub_dtm = NULL
--  WHERE mod_usr_logn_id = 'MIGRATION_20260312'
--    AND sub_yn = 1;

/* =========================================================
 * 6) RISKS
 * ========================================================= */

-- RISK-1: existing duplicated (nws_ltr_mstr_id, usr_no) rows will block unique key creation.
-- RISK-2: orphan rows will block FK creation.
-- RISK-3: ALTER statement must be executed selectively by environment state.

/* =========================================================
 * 7) OPEN QUESTIONS
 * ========================================================= */

-- OQ-1: sub_dtm/unsub_dtm historical backfill scope (all rows vs active rows only).
-- OQ-2: whether to enforce additional CHECK for sub_yn IN (0,1) depending on target MySQL mode/version.
