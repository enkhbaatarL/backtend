-- Newsletter Template V2 Canonical Data Source Spec Alter
-- Target: existing test DB where newsletter v2 tables and sample data already exist
-- Date: 2026-03-17
--
-- Apply order
-- 1. Run this file to add the new canonical spec column
-- 2. Re-run docs/newsletter-template-v2-insert-example-oil-price-trends.sql

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT VERSION() AS mysql_version;

SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'tmd_nwsltrtmpltblk'
  AND column_name IN ('data_src_type_cd', 'data_src_ref', 'data_src_spec_json')
ORDER BY ordinal_position;

SELECT COUNT(*) AS total_block_count
FROM tmd_nwsltrtmpltblk;

SELECT data_src_type_cd, COUNT(*) AS row_count
FROM tmd_nwsltrtmpltblk
GROUP BY data_src_type_cd
ORDER BY data_src_type_cd;

/* =========================================================
 * 1) APPLY
 * ========================================================= */

ALTER TABLE tmd_nwsltrtmpltblk
    ADD COLUMN data_src_spec_json JSON NULL COMMENT '데이터소스명세JSON'
    AFTER data_src_ref;

/* =========================================================
 * 2) VERIFY
 * ========================================================= */

SHOW CREATE TABLE tmd_nwsltrtmpltblk;

SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'tmd_nwsltrtmpltblk'
  AND column_name = 'data_src_spec_json';

/* =========================================================
 * 3) ROLLBACK
 * ========================================================= */

-- Rollback is safe only before the application starts depending on this column.
-- ALTER TABLE tmd_nwsltrtmpltblk
--     DROP COLUMN data_src_spec_json;
