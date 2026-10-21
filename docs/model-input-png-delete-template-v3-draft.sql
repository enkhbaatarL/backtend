-- Document Control
-- status: Draft for other environments / applied to testdb midb on 2026-10-21 (template 10/v3)
-- canonical for: DRAFT_C_STRUCTURE v3 registration and publication SQL; not the policy/schema source of truth
-- owner: BE / DBA; date / last synced: 2026-10-21
-- source: DRAFT_C_STRUCTURE v2, template 8, registered by the 2026-10-20 revision
--         of model-input-editor-write-template-version-draft.sql
-- depends on: design-docs/gas/mi-model-input-template-catalog-domain-model-design.md,
--             feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md §3.3
-- execution record: work-notes/2026/2026-10-21-png-delete-template-commit-and-db-publication.md
--
-- Clone all 4 files, 70 sheets, 92 tables and 126 modification scopes from ACTIVE template 8.
-- Change only the PNG Infrastructure table's DELETE_ROW/DELETE_COLUMN capability and original-field
-- DELETE_COLUMN policy. Existing Case and Draft links remain pinned to their current template.
-- Run PART A and PART B separately in one dedicated midb connection each, with utf8mb4 and FK checks enabled.
-- Stop catalog writers, uploads and Draft workers during each transaction. Stop on any SQL error.
-- Each PART ends in ROLLBACK by default. Only after reviewing its final VERIFIED/PUBLISHED result may an
-- authorized operator replace that final ROLLBACK with COMMIT in the same connection. Never override guards.
-- A completed PART A cannot be rerun; do not automatically delete a committed DRAFT.

-- PART A: register v3 as DRAFT.
SET @a_schema = 'midb', @a_actor = 'SYSTEM', @a_model = 'DRAFT_C_STRUCTURE';
SET @a_source = 8, @a_source_version = 2, @a_version = 3;
SET @a_source_marker = 'editor-write-20261020; source=tmplt7/DRAFT_C_STRUCTURE/v1; patched=52/92; scopes=126; NOT_PUBLISHED';
SET @a_marker = 'png-delete-20261021; source=tmplt8/DRAFT_C_STRUCTURE/v2; changed=PNG Infrastructure only; NOT_PUBLISHED';
SET @a_name = '2026-10-21 PNG Infrastructure delete DRAFT';
SET @a_ok = 0, @a_new = NULL, @a_verified = 0, @a_png = NULL;
SET SESSION group_concat_max_len = 16777216;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, aply_bgn_dtm, tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @a_model FOR UPDATE;
SELECT f.inpt_file_id FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @a_source FOR SHARE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id = @a_source FOR SHARE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_source FOR SHARE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
WHERE f.tmplt_id = @a_source FOR SHARE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @a_source FOR SHARE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id = @a_source FOR SHARE;
SET @a_links = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @a_source);
SET @a_drafts = (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id = @a_source);
SET @a_png = (SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_source
 AND BINARY f.mdl_file_cd = BINARY 'TRADE' AND BINARY s.sht_nm = BINARY 'MidstreamCapacityInput'
 AND BINARY b.inpt_tbl_nm = BINARY 'PNG Infrastructure' AND f.del_yn = 'N' AND s.del_yn = 'N' AND b.del_yn = 'N');
-- EXPECTED_PNG_FIELDS_AND_CAPS (generated from the committed v2 registration payload).
SET @a_expected_fields = '[{"fieldKey":"route","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"fromCountry","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"toCountry","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"READY","SET":"READY","CLEAR":"READY"},"create":{"required":false,"initial":{"kind":"EMPTY"},"constraints":{}},"setConstraints":{}}]';
SET @a_expected_caps = '{"SET":"READY","CLEAR":"READY","ADD_ROW":"READY","DELETE_ROW":"INCOMPLETE","ADD_COLUMN":"READY"}';
SET @a_ok = IF(BINARY DATABASE() = BINARY @a_schema AND @@SESSION.foreign_key_checks = 1
 AND CHAR_LENGTH(TRIM(@a_actor)) BETWEEN 1 AND 50 AND @a_png IS NOT NULL
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @a_source
  AND BINARY mdl_spt_cd = BINARY @a_model AND tmplt_ver_no = @a_source_version
  AND tmplt_stat_cd = 'ACTIVE' AND aply_bgn_dtm IS NOT NULL AND del_yn = 'N'
  AND BINARY tmplt_expn = BINARY @a_source_marker) = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @a_model
  AND tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N') = 1
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inpttmpltm WHERE BINARY mdl_spt_cd = BINARY @a_model
  AND (tmplt_ver_no = @a_version OR BINARY tmplt_expn = BINARY @a_marker))
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id = @a_source AND del_yn = 'N') = 4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id = @a_source) = 4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @a_source AND s.del_yn = 'N') = 70
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @a_source) = 70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
  JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_source AND b.del_yn = 'N') = 92
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
  JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_source) = 92
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
  JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @a_source AND m.del_yn = 'N') = 126
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
  JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @a_source) = 126
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld WHERE inpt_tbl_id = @a_png AND tbl_ord = 1
  AND tbl_bgn_row_no = 1 AND tbl_end_row_no = 207 AND tbl_bgn_col_no = 2 AND tbl_end_col_no = 17
  AND hdr_bgn_row_no = 1 AND hdr_end_row_no = 3 AND data_bgn_row_no = 11
  AND row_add_psbl_yn = 'Y' AND col_add_psbl_yn = 'Y' AND JSON_VALID(qry_json) = 1
  AND JSON_EXTRACT(qry_json, '$.fields') = CAST(@a_expected_fields AS JSON)
  AND JSON_EXTRACT(qry_json, '$.capabilities') = CAST(@a_expected_caps AS JSON)) = 1, 1, 0);
SELECT @a_png AS source_png_table, @a_links AS source_case_links, @a_drafts AS source_drafts,
 IF(@a_ok = 1, 'PRECHECK_OK', 'STOP_AND_ROLLBACK') AS decision;
-- Stop and ROLLBACK when precheck is not PRECHECK_OK. Every write below is gated by @a_ok.

INSERT INTO tmd_mdl_inpttmpltm (mdl_spt_cd, tmplt_ver_no, tmplt_ver_nm, tmplt_stat_cd, aply_bgn_dtm,
 tmplt_expn, crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
SELECT @a_model, @a_version, @a_name, 'DRAFT', NULL, @a_marker, @a_actor, CURRENT_TIMESTAMP,
 @a_actor, CURRENT_TIMESTAMP, 'N' FROM DUAL WHERE @a_ok = 1;
SET @a_new = (SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE @a_ok = 1
 AND BINARY mdl_spt_cd = BINARY @a_model AND tmplt_ver_no = @a_version
 AND BINARY tmplt_expn = BINARY @a_marker AND tmplt_stat_cd = 'DRAFT'
 AND aply_bgn_dtm IS NULL AND del_yn = 'N' AND crt_usr_logn_id = @a_actor);
INSERT INTO tmd_mdl_inptfiled (tmplt_id, mdl_file_cd, file_nm, file_ord, file_type_cd, file_strt_hash_val,
 crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
SELECT @a_new, f.mdl_file_cd, f.file_nm, f.file_ord, f.file_type_cd, f.file_strt_hash_val,
 @a_actor, CURRENT_TIMESTAMP, @a_actor, CURRENT_TIMESTAMP, 'N'
FROM tmd_mdl_inptfiled f WHERE @a_ok = 1 AND @a_new IS NOT NULL AND f.tmplt_id = @a_source AND f.del_yn = 'N';
INSERT INTO tmd_mdl_inptshtd (inpt_file_id, sht_nm, sht_ord, sht_stat_cd,
 crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
SELECT nf.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd,
 @a_actor, CURRENT_TIMESTAMP, @a_actor, CURRENT_TIMESTAMP, 'N'
FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id = s.inpt_file_id AND sf.tmplt_id = @a_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
WHERE @a_ok = 1 AND s.del_yn = 'N';
INSERT INTO tmd_mdl_inpttbld (inpt_sht_id, inpt_tbl_nm, tbl_ord, tbl_bgn_row_no, tbl_end_row_no,
 tbl_bgn_col_no, tbl_end_col_no, hdr_bgn_row_no, hdr_end_row_no, data_bgn_row_no, row_add_psbl_yn,
 col_add_psbl_yn, inpt_tbl_expn, qry_json, crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
SELECT ns.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord, b.tbl_bgn_row_no, b.tbl_end_row_no,
 b.tbl_bgn_col_no, b.tbl_end_col_no, b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no,
 b.row_add_psbl_yn, b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json,
 @a_actor, CURRENT_TIMESTAMP, @a_actor, CURRENT_TIMESTAMP, 'N'
FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id = b.inpt_sht_id
JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id = ss.inpt_file_id AND sf.tmplt_id = @a_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id = nf.inpt_file_id AND BINARY ns.sht_nm = BINARY ss.sht_nm
WHERE @a_ok = 1 AND b.del_yn = 'N';
INSERT INTO tmd_mdl_inptmodscopd (inpt_tbl_id, mod_scop_ord, bgn_row_no, end_row_no, bgn_col_no,
 end_col_no, crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
SELECT nb.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no, m.end_row_no, m.bgn_col_no, m.end_col_no,
 @a_actor, CURRENT_TIMESTAMP, @a_actor, CURRENT_TIMESTAMP, 'N'
FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id = m.inpt_tbl_id
JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id = sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id = ss.inpt_file_id
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id = nf.inpt_file_id AND BINARY ns.sht_nm = BINARY ss.sht_nm
JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id = ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm = BINARY sb.inpt_tbl_nm
WHERE @a_ok = 1 AND sf.tmplt_id = @a_source AND m.del_yn = 'N';
SET @a_expected_png_qry = (SELECT JSON_SET(CAST(qry_json AS JSON),
 '$.capabilities.DELETE_ROW', 'READY', '$.capabilities.DELETE_COLUMN', 'READY',
 '$.fields[0].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[1].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[2].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[3].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[4].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[5].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[6].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[7].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[8].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[9].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED',
 '$.fields[10].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED')
 FROM tmd_mdl_inpttbld WHERE @a_ok = 1 AND inpt_tbl_id = @a_png);
UPDATE tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
SET b.qry_json = @a_expected_png_qry, b.mod_usr_logn_id = @a_actor, b.mod_dtm = CURRENT_TIMESTAMP
WHERE @a_ok = 1 AND f.tmplt_id = @a_new AND BINARY f.mdl_file_cd = BINARY 'TRADE'
AND BINARY s.sht_nm = BINARY 'MidstreamCapacityInput'
AND BINARY b.inpt_tbl_nm = BINARY 'PNG Infrastructure';

-- Verification: natural-key pairing must account for every copied row, with one intended JSON change.
SET @a_files_equal = (SELECT COUNT(*) FROM tmd_mdl_inptfiled sf JOIN tmd_mdl_inptfiled nf
 ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
 WHERE sf.tmplt_id = @a_source AND BINARY nf.file_nm = BINARY sf.file_nm AND nf.file_ord = sf.file_ord
 AND nf.file_type_cd = sf.file_type_cd AND nf.file_strt_hash_val <=> sf.file_strt_hash_val
 AND nf.del_yn = sf.del_yn);
SET @a_sheets_equal = (SELECT COUNT(*) FROM tmd_mdl_inptshtd ss JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id = nf.inpt_file_id AND BINARY ns.sht_nm = BINARY ss.sht_nm
 WHERE sf.tmplt_id = @a_source AND ss.sht_ord = ns.sht_ord AND ss.sht_stat_cd = ns.sht_stat_cd
 AND ns.del_yn = ss.del_yn);
SET @a_tables_equal = (SELECT COUNT(*) FROM tmd_mdl_inpttbld sb JOIN tmd_mdl_inptshtd ss USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id = nf.inpt_file_id AND BINARY ns.sht_nm = BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id = ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm = BINARY sb.inpt_tbl_nm
 WHERE sf.tmplt_id = @a_source AND sb.tbl_ord = nb.tbl_ord
 AND sb.tbl_bgn_row_no = nb.tbl_bgn_row_no AND sb.tbl_end_row_no = nb.tbl_end_row_no
 AND sb.tbl_bgn_col_no = nb.tbl_bgn_col_no AND sb.tbl_end_col_no = nb.tbl_end_col_no
 AND sb.hdr_bgn_row_no = nb.hdr_bgn_row_no AND sb.hdr_end_row_no = nb.hdr_end_row_no
 AND sb.data_bgn_row_no = nb.data_bgn_row_no AND sb.row_add_psbl_yn = nb.row_add_psbl_yn
 AND sb.col_add_psbl_yn = nb.col_add_psbl_yn AND sb.inpt_tbl_expn <=> nb.inpt_tbl_expn
 AND sb.del_yn = nb.del_yn
 AND (CASE WHEN sb.inpt_tbl_id = @a_png THEN CAST(nb.qry_json AS JSON) = CAST(@a_expected_png_qry AS JSON)
      ELSE CAST(nb.qry_json AS JSON) = CAST(sb.qry_json AS JSON) END));
SET @a_scopes_equal = (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd sm
 JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id = sm.inpt_tbl_id
 JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id = sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id = ss.inpt_file_id
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id = @a_new AND BINARY nf.mdl_file_cd = BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id = nf.inpt_file_id AND BINARY ns.sht_nm = BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id = ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm = BINARY sb.inpt_tbl_nm
 JOIN tmd_mdl_inptmodscopd nm ON nm.inpt_tbl_id = nb.inpt_tbl_id AND nm.mod_scop_ord = sm.mod_scop_ord
 WHERE sf.tmplt_id = @a_source AND nm.bgn_row_no = sm.bgn_row_no AND nm.end_row_no = sm.end_row_no
 AND nm.bgn_col_no = sm.bgn_col_no AND nm.end_col_no = sm.end_col_no AND nm.del_yn = sm.del_yn);
-- HIERARCHY_DIGEST_A
SET @a_catalog_digest = (
  SELECT SHA2(GROUP_CONCAT(SHA2(payload, 256) ORDER BY record_kind, record_id SEPARATOR ','), 256)
  FROM (
    SELECT 'F' AS record_kind, f.inpt_file_id AS record_id,
      CAST(JSON_ARRAY('F', f.inpt_file_id, f.tmplt_id, f.mdl_file_cd, f.file_nm, f.file_ord,
        f.file_type_cd, f.file_strt_hash_val, f.del_yn) AS CHAR) AS payload
    FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @a_new
    UNION ALL
    SELECT 'S', s.inpt_sht_id,
      CAST(JSON_ARRAY('S', s.inpt_sht_id, s.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd, s.del_yn) AS CHAR)
    FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_new
    UNION ALL
    SELECT 'T', b.inpt_tbl_id,
      CAST(JSON_ARRAY('T', b.inpt_tbl_id, b.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord,
        b.tbl_bgn_row_no, b.tbl_end_row_no, b.tbl_bgn_col_no, b.tbl_end_col_no,
        b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no, b.row_add_psbl_yn,
        b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json, b.del_yn) AS CHAR)
    FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_new
    UNION ALL
    SELECT 'M', m.mod_scop_id,
      CAST(JSON_ARRAY('M', m.mod_scop_id, m.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no,
        m.end_row_no, m.bgn_col_no, m.end_col_no, m.del_yn) AS CHAR)
    FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
    WHERE f.tmplt_id = @a_new
  ) hierarchy
);
UPDATE tmd_mdl_inpttmpltm SET tmplt_expn = CONCAT(@a_marker, '; digest=', @a_catalog_digest)
WHERE @a_ok = 1 AND @a_new IS NOT NULL AND @a_catalog_digest IS NOT NULL
AND tmplt_id = @a_new AND BINARY tmplt_expn = BINARY @a_marker AND tmplt_stat_cd = 'DRAFT';
SET @a_verified = IF(@a_ok = 1 AND @a_new IS NOT NULL AND @a_catalog_digest IS NOT NULL
 AND @a_files_equal = 4 AND @a_sheets_equal = 70 AND @a_tables_equal = 92 AND @a_scopes_equal = 126
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id = @a_new) = 4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_new) = 70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
  JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @a_new) = 92
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
  JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @a_new) = 126
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @a_new AND tmplt_stat_cd = 'DRAFT'
  AND BINARY tmplt_expn = BINARY CONCAT(@a_marker, '; digest=', @a_catalog_digest)) = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @a_source AND tmplt_stat_cd = 'ACTIVE'
  AND BINARY tmplt_expn = BINARY @a_source_marker) = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @a_source) = @a_links
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id = @a_source) = @a_drafts
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @a_new)
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id = @a_new), 1, 0);
SELECT @a_new AS new_tmplt_id, @a_files_equal AS files_must_be_4, @a_sheets_equal AS sheets_must_be_70,
 @a_tables_equal AS tables_must_be_92, @a_scopes_equal AS scopes_must_be_126,
 IF(@a_verified = 1, 'VERIFIED_UNPUBLISHED', 'STOP_AND_ROLLBACK') AS decision;
ROLLBACK;

-- PART B: run only after PART A was reviewed and COMMITted, with compatible server and worker ready.
SET @b_schema = 'midb', @b_actor = 'SYSTEM', @b_model = 'DRAFT_C_STRUCTURE';
SET @b_source = 8, @b_source_version = 2, @b_version = 3;
SET @b_source_marker = 'editor-write-20261020; source=tmplt7/DRAFT_C_STRUCTURE/v1; patched=52/92; scopes=126; NOT_PUBLISHED';
SET @b_marker = 'png-delete-20261021; source=tmplt8/DRAFT_C_STRUCTURE/v2; changed=PNG Infrastructure only; NOT_PUBLISHED';
SET @b_ok = 0, @b_published = 0, @b_new = NULL, @b_time = NULL, @b_source_time = NULL;
SET SESSION group_concat_max_len = 16777216;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, aply_bgn_dtm, tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @b_model FOR UPDATE;
SET @b_new = (SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE BINARY mdl_spt_cd = BINARY @b_model
 AND tmplt_ver_no = @b_version AND tmplt_stat_cd = 'DRAFT' AND aply_bgn_dtm IS NULL AND del_yn = 'N'
 AND BINARY LEFT(tmplt_expn, CHAR_LENGTH(@b_marker) + 9) = BINARY CONCAT(@b_marker, '; digest='));
SELECT f.inpt_file_id FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @b_new FOR SHARE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @b_new FOR SHARE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @b_new FOR SHARE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @b_new FOR SHARE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id IN (@b_source, @b_new) FOR SHARE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id = @b_new FOR SHARE;
SET @b_source_time = (SELECT aply_bgn_dtm FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @b_source);
SET @b_links = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @b_source);
-- HIERARCHY_DIGEST_B
SET @b_catalog_digest = (
  SELECT SHA2(GROUP_CONCAT(SHA2(payload, 256) ORDER BY record_kind, record_id SEPARATOR ','), 256)
  FROM (
    SELECT 'F' AS record_kind, f.inpt_file_id AS record_id,
      CAST(JSON_ARRAY('F', f.inpt_file_id, f.tmplt_id, f.mdl_file_cd, f.file_nm, f.file_ord,
        f.file_type_cd, f.file_strt_hash_val, f.del_yn) AS CHAR) AS payload
    FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @b_new
    UNION ALL
    SELECT 'S', s.inpt_sht_id,
      CAST(JSON_ARRAY('S', s.inpt_sht_id, s.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd, s.del_yn) AS CHAR)
    FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @b_new
    UNION ALL
    SELECT 'T', b.inpt_tbl_id,
      CAST(JSON_ARRAY('T', b.inpt_tbl_id, b.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord,
        b.tbl_bgn_row_no, b.tbl_end_row_no, b.tbl_bgn_col_no, b.tbl_end_col_no,
        b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no, b.row_add_psbl_yn,
        b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json, b.del_yn) AS CHAR)
    FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @b_new
    UNION ALL
    SELECT 'M', m.mod_scop_id,
      CAST(JSON_ARRAY('M', m.mod_scop_id, m.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no,
        m.end_row_no, m.bgn_col_no, m.end_col_no, m.del_yn) AS CHAR)
    FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
    WHERE f.tmplt_id = @b_new
  ) hierarchy
);
SET @b_ok = IF(BINARY DATABASE() = BINARY @b_schema AND @@SESSION.foreign_key_checks = 1
 AND CHAR_LENGTH(TRIM(@b_actor)) BETWEEN 1 AND 50 AND @b_new IS NOT NULL AND @b_catalog_digest IS NOT NULL
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @b_source
  AND BINARY mdl_spt_cd = BINARY @b_model AND tmplt_ver_no = @b_source_version
  AND tmplt_stat_cd = 'ACTIVE' AND aply_bgn_dtm IS NOT NULL AND del_yn = 'N'
  AND BINARY tmplt_expn = BINARY @b_source_marker) = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @b_model AND tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N') = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @b_new
  AND BINARY tmplt_expn = BINARY CONCAT(@b_marker, '; digest=', @b_catalog_digest)) = 1
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @b_new)
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id = @b_new)
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id = @b_new AND del_yn = 'N') = 4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @b_new AND s.del_yn = 'N') = 70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
  JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @b_new AND b.del_yn = 'N') = 92
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
  JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id = @b_new AND m.del_yn = 'N') = 126, 1, 0);
SELECT @b_new AS new_tmplt_id, @b_links AS source_case_links,
 IF(@b_ok = 1, 'PRECHECK_OK', 'STOP_AND_ROLLBACK') AS decision;
SET @b_time = CURRENT_TIMESTAMP(6);
UPDATE tmd_mdl_inpttmpltm SET tmplt_stat_cd = 'DEPRECATED', mod_usr_logn_id = @b_actor,
 mod_dtm = CURRENT_TIMESTAMP WHERE @b_ok = 1 AND tmplt_id = @b_source AND tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N';
UPDATE tmd_mdl_inpttmpltm SET tmplt_stat_cd = 'ACTIVE', aply_bgn_dtm = @b_time,
 mod_usr_logn_id = @b_actor, mod_dtm = CURRENT_TIMESTAMP
 WHERE @b_ok = 1 AND tmplt_id = @b_new AND tmplt_stat_cd = 'DRAFT' AND aply_bgn_dtm IS NULL AND del_yn = 'N';
SET @b_published = IF(@b_ok = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @b_new AND tmplt_stat_cd = 'ACTIVE'
  AND aply_bgn_dtm = @b_time AND del_yn = 'N') = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id = @b_source AND tmplt_stat_cd = 'DEPRECATED'
  AND aply_bgn_dtm <=> @b_source_time AND del_yn = 'N') = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @b_model AND tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N') = 1
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @b_source) = @b_links
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id = @b_new), 1, 0);
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, aply_bgn_dtm, del_yn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd = @b_model ORDER BY tmplt_ver_no;
SELECT IF(@b_published = 1, 'PUBLISHED_PENDING_COMMIT', 'STOP_AND_ROLLBACK') AS decision;
ROLLBACK;
