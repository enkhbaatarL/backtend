-- Document Control
-- status: Draft / default ROLLBACK
-- canonical for: testdb v5 catalog registration/publication; no schema change
-- owner: BE; date: 2026-10-22
-- source: model-input-lng-demand-control-template-v4-draft.sql and v4-publication-draft.sql
-- execution record: work-notes/2026/2026-10-22-lng-demand-mode-hidden-v5-publication.md
-- Existing Case/Draft and v4 catalog stay fixed. Stop on SQL error or STOP_AND_ROLLBACK.
-- Run one part in one dedicated midb session. Default ROLLBACK rehearsal first.
-- Apply copy excludes terminal ROLLBACK; inspect final success then COMMIT in the SAME session.
SET NAMES utf8mb4;
SET @lc_actor = 'SYSTEM', @lc_schema='midb', @lc_model='DRAFT_C_STRUCTURE', @lc_version=5;
SET @lc_marker = 'lng-demand-hidden-20261022; source=tmplt12/v4; mode=navigation-hidden; NOT_PUBLISHED';
SET @lc_source_marker = 'lng-demand-control-20261022; source=v3; added=2; scopes=20; NOT_PUBLISHED';
SET @lc_name = '2026-10-22 LNG Demand hidden Mode v5';
SET @lc_source=12, @lc_new=NULL, @lc_ok=0, @lc_verified=0;
SET SESSION innodb_lock_wait_timeout=5;
SET SESSION group_concat_max_len=16777216;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@lc_model FOR UPDATE;
SELECT f.inpt_file_id FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source FOR SHARE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
 WHERE f.tmplt_id = @lc_source FOR SHARE;
SET @lc_links=(SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@lc_source);
SET @lc_drafts=(SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id=@lc_source);
SET @lc_source_digest = (
  SELECT SHA2(GROUP_CONCAT(SHA2(payload, 256) ORDER BY record_kind, record_id SEPARATOR ','), 256)
  FROM (
    SELECT 'F' AS record_kind, f.inpt_file_id AS record_id,
      CAST(JSON_ARRAY('F', f.inpt_file_id, f.tmplt_id, f.mdl_file_cd, f.file_nm, f.file_ord,
        f.file_type_cd, f.file_strt_hash_val, f.del_yn) AS CHAR) AS payload
    FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @lc_source
    UNION ALL
    SELECT 'S', s.inpt_sht_id,
      CAST(JSON_ARRAY('S', s.inpt_sht_id, s.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd, s.del_yn) AS CHAR)
    FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source
    UNION ALL
    SELECT 'T', b.inpt_tbl_id,
      CAST(JSON_ARRAY('T', b.inpt_tbl_id, b.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord,
        b.tbl_bgn_row_no, b.tbl_end_row_no, b.tbl_bgn_col_no, b.tbl_end_col_no,
        b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no, b.row_add_psbl_yn,
        b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json, b.del_yn) AS CHAR)
    FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_source
    UNION ALL
    SELECT 'M', m.mod_scop_id,
      CAST(JSON_ARRAY('M', m.mod_scop_id, m.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no,
        m.end_row_no, m.bgn_col_no, m.end_col_no, m.del_yn) AS CHAR)
    FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
    WHERE f.tmplt_id = @lc_source
  ) hierarchy
);
SET @lc_ok=IF(BINARY DATABASE()=BINARY @lc_schema AND @@SESSION.foreign_key_checks=1
 AND CHAR_LENGTH(TRIM(@lc_actor)) BETWEEN 1 AND 50
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@lc_source AND BINARY mdl_spt_cd=BINARY @lc_model AND tmplt_ver_no=4 AND tmplt_stat_cd='ACTIVE' AND aply_bgn_dtm IS NOT NULL AND del_yn='N'
 AND BINARY tmplt_expn=BINARY CONCAT(@lc_source_marker,'; digest=',@lc_source_digest))=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@lc_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@lc_source AND del_yn='N')=4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_source AND s.del_yn='N')=70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_source AND b.del_yn='N')=94
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id) JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_source AND m.del_yn='N')=146
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@lc_model AND tmplt_ver_no=@lc_version)
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_source AND BINARY f.mdl_file_cd=BINARY 'DEMAND' AND BINARY s.sht_nm=BINARY 'LNG Demand' AND BINARY b.inpt_tbl_nm=BINARY 'LNG Demand Mode' AND b.tbl_ord=1 AND JSON_CONTAINS_PATH(b.qry_json,'one','$.navigationVisible')=0)=1
,1,0);
SELECT @lc_source AS source_template,IF(@lc_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;
INSERT INTO tmd_mdl_inpttmpltm (mdl_spt_cd,tmplt_ver_no,tmplt_ver_nm,tmplt_stat_cd,aply_bgn_dtm,
 tmplt_expn,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_model,@lc_version,@lc_name,'DRAFT',NULL,@lc_marker,@lc_actor,CURRENT_TIMESTAMP,
 @lc_actor,CURRENT_TIMESTAMP,'N' FROM DUAL WHERE @lc_ok=1;
SET @lc_new = (SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE @lc_ok=1
 AND BINARY mdl_spt_cd=BINARY @lc_model AND tmplt_ver_no=@lc_version
 AND tmplt_stat_cd='DRAFT' AND BINARY tmplt_expn=BINARY @lc_marker AND del_yn='N');
INSERT INTO tmd_mdl_inptfiled (tmplt_id,mdl_file_cd,file_nm,file_ord,file_type_cd,file_strt_hash_val,
 crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @lc_new,f.mdl_file_cd,f.file_nm,f.file_ord,f.file_type_cd,f.file_strt_hash_val,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptfiled f WHERE @lc_ok=1 AND @lc_new IS NOT NULL AND f.tmplt_id=@lc_source AND f.del_yn='N';
INSERT INTO tmd_mdl_inptshtd (inpt_file_id,sht_nm,sht_ord,sht_stat_cd,
 crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT nf.inpt_file_id,s.sht_nm,s.sht_ord,s.sht_stat_cd,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=s.inpt_file_id AND sf.tmplt_id=@lc_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
WHERE @lc_ok=1 AND s.del_yn='N';
INSERT INTO tmd_mdl_inpttbld (inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,
 tbl_bgn_col_no,tbl_end_col_no,hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,
 col_add_psbl_yn,inpt_tbl_expn,qry_json,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT ns.inpt_sht_id,b.inpt_tbl_nm,b.tbl_ord,b.tbl_bgn_row_no,b.tbl_end_row_no,b.tbl_bgn_col_no,
 b.tbl_end_col_no,b.hdr_bgn_row_no,b.hdr_end_row_no,b.data_bgn_row_no,b.row_add_psbl_yn,
 b.col_add_psbl_yn,b.inpt_tbl_expn,b.qry_json,@lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=b.inpt_sht_id
JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=ss.inpt_file_id AND sf.tmplt_id=@lc_source
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
WHERE @lc_ok=1 AND b.del_yn='N';
INSERT INTO tmd_mdl_inptmodscopd (inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,
 end_col_no,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT nb.inpt_tbl_id,m.mod_scop_ord,m.bgn_row_no,m.end_row_no,m.bgn_col_no,m.end_col_no,
 @lc_actor,CURRENT_TIMESTAMP,@lc_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id=m.inpt_tbl_id
JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf ON sf.inpt_file_id=ss.inpt_file_id
JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
WHERE @lc_ok=1 AND sf.tmplt_id=@lc_source AND m.del_yn='N';
-- Change only the unpublished clone's navigation flag; keep owner geometry/scopes/control data.
UPDATE tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
SET b.qry_json=JSON_SET(b.qry_json,'$.navigationVisible',CAST('false' AS JSON)),b.mod_usr_logn_id=@lc_actor,b.mod_dtm=CURRENT_TIMESTAMP
WHERE @lc_ok=1 AND f.tmplt_id=@lc_new AND BINARY f.mdl_file_cd=BINARY 'DEMAND' AND BINARY s.sht_nm=BINARY 'LNG Demand' AND BINARY b.inpt_tbl_nm=BINARY 'LNG Demand Mode';
SET @lc_files_copied = (SELECT COUNT(*) FROM tmd_mdl_inptfiled sf JOIN tmd_mdl_inptfiled nf
 ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 WHERE sf.tmplt_id=@lc_source AND BINARY nf.file_nm=BINARY sf.file_nm AND nf.file_ord=sf.file_ord
 AND nf.file_type_cd=sf.file_type_cd AND nf.file_strt_hash_val <=> sf.file_strt_hash_val);
SET @lc_sheets_copied = (SELECT COUNT(*) FROM tmd_mdl_inptshtd ss JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 WHERE sf.tmplt_id=@lc_source AND ns.sht_ord=ss.sht_ord AND ns.sht_stat_cd=ss.sht_stat_cd);
SET @lc_tables_copied = (SELECT COUNT(*) FROM tmd_mdl_inpttbld sb JOIN tmd_mdl_inptshtd ss USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
 WHERE sf.tmplt_id=@lc_source AND sb.tbl_ord=nb.tbl_ord
 AND sb.tbl_bgn_row_no=nb.tbl_bgn_row_no AND sb.tbl_end_row_no=nb.tbl_end_row_no
 AND sb.tbl_bgn_col_no=nb.tbl_bgn_col_no AND sb.tbl_end_col_no=nb.tbl_end_col_no
 AND sb.hdr_bgn_row_no=nb.hdr_bgn_row_no AND sb.hdr_end_row_no=nb.hdr_end_row_no
 AND sb.data_bgn_row_no=nb.data_bgn_row_no AND sb.row_add_psbl_yn=nb.row_add_psbl_yn
 AND sb.col_add_psbl_yn=nb.col_add_psbl_yn AND sb.inpt_tbl_expn <=> nb.inpt_tbl_expn
 AND CAST(nb.qry_json AS JSON)=CAST(IF(BINARY sf.mdl_file_cd=BINARY 'DEMAND' AND BINARY ss.sht_nm=BINARY 'LNG Demand' AND BINARY sb.inpt_tbl_nm=BINARY 'LNG Demand Mode',JSON_SET(sb.qry_json,'$.navigationVisible',CAST('false' AS JSON)),sb.qry_json) AS JSON));
SET @lc_scopes_copied = (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd sm
 JOIN tmd_mdl_inpttbld sb ON sb.inpt_tbl_id=sm.inpt_tbl_id
 JOIN tmd_mdl_inptshtd ss ON ss.inpt_sht_id=sb.inpt_sht_id JOIN tmd_mdl_inptfiled sf USING(inpt_file_id)
 JOIN tmd_mdl_inptfiled nf ON nf.tmplt_id=@lc_new AND BINARY nf.mdl_file_cd=BINARY sf.mdl_file_cd
 JOIN tmd_mdl_inptshtd ns ON ns.inpt_file_id=nf.inpt_file_id AND BINARY ns.sht_nm=BINARY ss.sht_nm
 JOIN tmd_mdl_inpttbld nb ON nb.inpt_sht_id=ns.inpt_sht_id AND BINARY nb.inpt_tbl_nm=BINARY sb.inpt_tbl_nm
 JOIN tmd_mdl_inptmodscopd nm ON nm.inpt_tbl_id=nb.inpt_tbl_id AND nm.mod_scop_ord=sm.mod_scop_ord
 WHERE sf.tmplt_id=@lc_source AND nm.bgn_row_no=sm.bgn_row_no AND nm.end_row_no=sm.end_row_no
 AND nm.bgn_col_no=sm.bgn_col_no AND nm.end_col_no=sm.end_col_no);
SET @lc_catalog_digest = (
  SELECT SHA2(GROUP_CONCAT(SHA2(payload, 256) ORDER BY record_kind, record_id SEPARATOR ','), 256)
  FROM (
    SELECT 'F' AS record_kind, f.inpt_file_id AS record_id,
      CAST(JSON_ARRAY('F', f.inpt_file_id, f.tmplt_id, f.mdl_file_cd, f.file_nm, f.file_ord,
        f.file_type_cd, f.file_strt_hash_val, f.del_yn) AS CHAR) AS payload
    FROM tmd_mdl_inptfiled f WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'S', s.inpt_sht_id,
      CAST(JSON_ARRAY('S', s.inpt_sht_id, s.inpt_file_id, s.sht_nm, s.sht_ord, s.sht_stat_cd, s.del_yn) AS CHAR)
    FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'T', b.inpt_tbl_id,
      CAST(JSON_ARRAY('T', b.inpt_tbl_id, b.inpt_sht_id, b.inpt_tbl_nm, b.tbl_ord,
        b.tbl_bgn_row_no, b.tbl_end_row_no, b.tbl_bgn_col_no, b.tbl_end_col_no,
        b.hdr_bgn_row_no, b.hdr_end_row_no, b.data_bgn_row_no, b.row_add_psbl_yn,
        b.col_add_psbl_yn, b.inpt_tbl_expn, b.qry_json, b.del_yn) AS CHAR)
    FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
      JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id = @lc_new
    UNION ALL
    SELECT 'M', m.mod_scop_id,
      CAST(JSON_ARRAY('M', m.mod_scop_id, m.inpt_tbl_id, m.mod_scop_ord, m.bgn_row_no,
        m.end_row_no, m.bgn_col_no, m.end_col_no, m.del_yn) AS CHAR)
    FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
      JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
    WHERE f.tmplt_id = @lc_new
  ) hierarchy
);
UPDATE tmd_mdl_inpttmpltm SET tmplt_expn=CONCAT(@lc_marker,'; digest=',@lc_catalog_digest)
WHERE @lc_ok=1 AND tmplt_id=@lc_new AND tmplt_stat_cd='DRAFT' AND BINARY tmplt_expn=BINARY @lc_marker;
SET @lc_verified=IF(@lc_ok=1 AND @lc_new IS NOT NULL
 AND @lc_files_copied=4 AND @lc_sheets_copied=70 AND @lc_tables_copied=94 AND @lc_scopes_copied=146
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@lc_new AND tmplt_stat_cd='DRAFT' AND BINARY tmplt_expn=BINARY CONCAT(@lc_marker,'; digest=',@lc_catalog_digest))=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@lc_new AND del_yn='N')=4
 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_new AND s.del_yn='N')=70
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_new AND b.del_yn='N')=94
 AND (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id) JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@lc_new AND m.del_yn='N')=146
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@lc_source)=@lc_links
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id=@lc_source)=@lc_drafts
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@lc_new)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id=@lc_new),1,0);
SELECT @lc_new AS new_template,@lc_files_copied AS files,@lc_sheets_copied AS sheets,@lc_tables_copied AS tables,@lc_scopes_copied AS scopes,
IF(@lc_verified=1,'VERIFIED_UNPUBLISHED','STOP_AND_ROLLBACK') AS decision;
ROLLBACK;
