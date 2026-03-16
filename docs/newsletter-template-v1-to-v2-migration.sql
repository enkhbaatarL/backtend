-- Newsletter Template V1 to V2 Migration Draft
-- Date: 2026-03-16

/* =========================================================
 * 0) PRE-CHECK
 * ========================================================= */

SELECT COUNT(DISTINCT nws_ltr_tmplt_id) AS v1_template_count
FROM tmd_nws_ltr_tmplt
WHERE del_yn = 'N';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('tmd_nwsltrtmpltm', 'tmd_nwsltrtmpltsctn', 'tmd_nwsltrtmpltblk', 'tmd_nwsltrtmpltblkfld');

/* =========================================================
 * 1) APPLY
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltm (nws_ltr_mstr_id, tmplt_nm, tmplt_desc, schm_ver, crt_usr_logn_id, mod_usr_logn_id)
SELECT DISTINCT h.nws_ltr_mstr_id,
       CONCAT(m.mstr_nm, ' 기본 템플릿'),
       'Migrated from v1 template',
       2,
       'MIGRATION_20260316',
       'MIGRATION_20260316'
FROM tmd_nws_ltr_tmplt t
JOIN tmd_nws_ltr_pub_hist h ON h.nws_ltr_tmplt_id = t.nws_ltr_tmplt_id
JOIN tmd_nws_ltr_mstr m ON m.nws_ltr_mstr_id = h.nws_ltr_mstr_id
WHERE t.del_yn = 'N'
ON DUPLICATE KEY UPDATE
    tmplt_nm = VALUES(tmplt_nm),
    schm_ver = VALUES(schm_ver),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

-- Step C/D/E/F는 운영 데이터 검증 후 order별 INSERT를 확정한다.
-- 메인 차트 / 뉴스 이벤트 / AI 요약 / SUB_INFO 및 order>=2 참조 영역을 섹션-블록-필드로 적재한다.

/* =========================================================
 * 2) POST-CHECK
 * ========================================================= */

SELECT COUNT(*) AS v2_template_count
FROM tmd_nwsltrtmpltm
WHERE del_yn = 'N';

SELECT COUNT(*) AS general_section_count
FROM tmd_nwsltrtmpltsctn
WHERE del_yn = 'N'
  AND sctn_type_cd = 'GENERAL';

SELECT COUNT(*) AS block_count
FROM tmd_nwsltrtmpltblk
WHERE del_yn = 'N';

/* =========================================================
 * 3) ROLLBACK
 * ========================================================= */

DELETE FROM tmd_nwsltrtmpltblkfld WHERE crt_usr_logn_id = 'MIGRATION_20260316';
DELETE FROM tmd_nwsltrtmpltblk WHERE crt_usr_logn_id = 'MIGRATION_20260316';
DELETE FROM tmd_nwsltrtmpltsctn WHERE crt_usr_logn_id = 'MIGRATION_20260316';
DELETE FROM tmd_nwsltrtmpltm WHERE crt_usr_logn_id = 'MIGRATION_20260316';
