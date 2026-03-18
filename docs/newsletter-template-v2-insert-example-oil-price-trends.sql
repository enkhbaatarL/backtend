-- Newsletter Template V2 Insert Example
-- Scenario: Figma-based "Oil Price Trends" editor structure
-- Date: 2026-03-17
-- Cover scope note:
-- - `COVER` section fields are aligned to the Figma selection `Cover` only.
-- - Selection-confirmed constraints currently reflected in SQL:
--   `imageBase64` derived from `tmd_nws_ltr_mstr.img_url` via
--   `encodeImageToBase64(imageUrl, MIME_TYPE)`, `publishCycle` derived from
--   `tmd_nws_ltr_mstr.perd_type` via `getPeriodType().getCode()`,
--   `newsletterName` derived from `tmd_nws_ltr_mstr.mstr_nm`,
--   `newsletterDescription` derived from `tmd_nws_ltr_mstr.expn`,
--   `newsletterName.maxLength = 100`, `newsletterDescription.maxLength = 500`
--
-- Assumptions
-- 1. newsletter master name = 'Oil Price Trends'
-- 2. newsletter master id fallback = 1
-- 3. main chart code = 'LCOc1'
-- 4. reference specialized company no = 2020
-- 5. reference1 dedicated chart type = 'WTI_DI'
-- 6. reference2 collection chart type = 'WTI_SNP500'
-- 7. reference3 preference table code fallback = 'OIL_PRC_TRND'
-- 8. reference3 preference table config id fallback = 101
-- 9. reference4 collection chart type = 'GOV10_2SPRD'
-- 10. this script assumes data_src_spec_json column already exists
--
-- Purpose
-- - Align template section/block structure with the latest editor screen
-- - Separate Cover / Date&Issue / Title / Summary / News Flow / Reference / Footer
-- - Define specialized reference sections:
--   `REFERENCE1 = HEADER + CHART + CONTENT`
--   `REFERENCE2 = HEADER + CHART + CONTENT`
--   `REFERENCE3 = HEADER + TABLE + CONTENT`
-- - `NEWS_ITEM` is a repeatable template block. One template block definition may render
--   1..N issue block state rows in the same section.
-- - Editor-modified values are written through delta/snapshot paths; `overridePolicy.issueState`
--   should only keep values that are directly owned by issue block state

START TRANSACTION;

/* =========================================================
 * 0) INPUT VARIABLES
 * ========================================================= */

SET @newsletter_master_name := 'Oil Price Trends';
SET @newsletter_master_id := 1;

SET @title_specialized_tgt_cmpny_no := 2020;
SET @main_chart_code := 'LCOc1';
SET @reference_specialized_tgt_cmpny_no := 2020;
SET @reference1_chart_type := 'WTI_DI';
SET @reference2_chart_type := 'WTI_SNP500';
SET @reference3_pref_table_code := 'OIL_PRC_TRND';
SET @reference4_chart_type := 'GOV10_2SPRD';
SET @collection_from_date := '2026-03-10';
SET @collection_to_date := '2026-03-16';
SET @news_from_date := '2026-03-10';
SET @news_to_date := '2026-03-16';

SELECT nws_ltr_mstr_id
INTO @newsletter_master_id
FROM tmd_nws_ltr_mstr
WHERE mstr_nm = @newsletter_master_name
  AND del_yn = 'N'
LIMIT 1;

SET @newsletter_master_id := COALESCE(@newsletter_master_id, 1);

SELECT data_reg_sn
INTO @reference3_pref_table_id
FROM tmd_nws_ltr_tmplt_pref_tbl
WHERE pref_tbl_cd = @reference3_pref_table_code
  AND del_yn = 'N'
LIMIT 1;

SET @reference3_pref_table_id := COALESCE(@reference3_pref_table_id, 101);

/* =========================================================
 * 0-1) COLLECTION CHART CONFIG
 * ========================================================= */

/*UPDATE tmd_nws_ltr_tmplt_cltn_chrt
SET del_yn = 'Y',
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE cltn_chrt_type IN (@reference2_chart_type, @reference4_chart_type)
  AND del_yn = 'N';

INSERT INTO tmd_nws_ltr_tmplt_cltn_chrt
(
    cltn_chrt_type,
    tgt_db_nm,
    tgt_tbl_nm,
    qry_cdtn_cntt,
    tbl_colm_nm,
    color,
    dtick,
    range,
    bidir_yn,
    tm_axis_intvl_val,
    tm_axis_intvl_unit,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@reference2_chart_type, 'midb-ske', 'tif_ske_chrt', 'code=''.SPX''', 'timestamp,trdprc_1', 'grey', NULL, NULL, 'N', NULL, NULL, 'SYSTEM', 'SYSTEM'),
(@reference2_chart_type, 'midb', 'tif_nws_ltr_chrt', 'code=''CLc1''', 'timestamp,trdprc_1', 'blue', NULL, NULL, 'N', NULL, NULL, 'SYSTEM', 'SYSTEM'),
(@reference4_chart_type, 'midb', 'tif_interestratesr', 'code=''TRUS10T''', 'date,ry', 'blue', NULL, NULL, 'N', NULL, NULL, 'SYSTEM', 'SYSTEM'),
(@reference4_chart_type, 'midb', 'tif_interestratesr', 'code=''TRUS10T-TRUS2YT''', 'date,ry', 'grey', NULL, NULL, 'Y', NULL, NULL, 'SYSTEM', 'SYSTEM');
*/
/* =========================================================
 * 1) TEMPLATE ROOT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltm
(
    nws_ltr_mstr_id,
    tmplt_nm,
    tmplt_desc,
    schm_ver,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(
    @newsletter_master_id,
    'Test Template',
    'v2 Energy 용최신 피그마 편집 화면 기준 템플릿',
    2,
    'SYSTEM',
    'SYSTEM'
)
ON DUPLICATE KEY UPDATE
    tmplt_nm = VALUES(tmplt_nm),
    tmplt_desc = VALUES(tmplt_desc),
    schm_ver = VALUES(schm_ver),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_tmplt_id
INTO @tmplt_id
FROM tmd_nwsltrtmpltm
WHERE nws_ltr_mstr_id = @newsletter_master_id
  AND del_yn = 'N';

/* =========================================================
 * 2) SECTION INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltsctn
(
    nws_ltr_tmplt_id,
    sctn_cd,
    sctn_type_cd,
    sctn_nm,
    sctn_ord,
    vsblty_yn,
    tgt_cmpny_no,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@tmplt_id, 'COVER',      'FIXED',   'Cover',         0, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'DATE_ISSUE', 'FIXED',   'Date & Issue',  1, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'TITLE_SPECIALIZED', 'SPECIALIZED', 'Title', 2, 'Y', @title_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'SUMMARY',    'FIXED',   'Summary',       3, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'NEWS_FLOW',  'SPECIALIZED', 'News Flow',       4, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE1',  'SPECIALIZED', 'Reference 1',       5, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE2',  'SPECIALIZED', 'Reference 2',       6, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE3',  'SPECIALIZED', 'Reference 3',       7, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE4',  'SPECIALIZED', 'Reference 4',       8, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'FOOTER',     'FIXED',   'Footer',        9, 'Y', NULL, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    sctn_nm = VALUES(sctn_nm),
    sctn_type_cd = VALUES(sctn_type_cd),
    sctn_ord = VALUES(sctn_ord),
    vsblty_yn = VALUES(vsblty_yn),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_tmplt_sctn_id INTO @sec_cover
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'COVER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_date_issue
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'DATE_ISSUE'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_title_specialized
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'TITLE_SPECIALIZED'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_summary
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'SUMMARY'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_news_flow
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'NEWS_FLOW'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference1
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE1'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference2
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE2'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference3
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE3'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference4
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE4'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_footer
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'FOOTER'
  AND del_yn = 'N';

/* =========================================================
 * 3) BLOCK INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltblk
(
    nws_ltr_tmplt_sctn_id,
    blk_cd,
    blk_type_cd,
    blk_fmt_cd,
    blk_nm,
    blk_ord,
    vsblty_yn,
    plot_on_chrt_yn,
    tgt_cmpny_no,
    data_src_type_cd,
    data_src_ref,
    data_src_spec_json,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@sec_cover,      'COVER_IMAGE_CARD', 'IMAGE',     'GENERAL', '뉴스레터 커버 이미지',            0, 'Y', 'N', NULL, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'imageBase64', 'sourcePath', 'imageBase64', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'showImage', 'sourcePath', 'showImage', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY('showImage')
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_cover,      'COVER_INFO',       'TEXT',      'GENERAL', '뉴스레터 기본 정보',              1, 'Y', 'N', NULL, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'publishCycle', 'sourcePath', 'publishCycle', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'newsletterName', 'sourcePath', 'newsletterName', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'newsletterDescription', 'sourcePath', 'newsletterDescription', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_date_issue, 'DATE_ISSUE_INFO',  'TEXT',      'GENERAL', '발행일 및 이슈 번호',             0, 'Y', 'N', NULL, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'PUBLISH_CONTEXT'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'publishDate', 'sourcePath', 'publishDate', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'issueNumber', 'sourcePath', 'issueLabel', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(
         JSON_OBJECT('type', 'FORMAT_DATE', 'sourcePath', 'publishDate', 'format', 'YYYY-MM-DD')
     ),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_title_specialized, 'TITLE_MAIN_SPECIALIZED', 'TITLE', 'SPECIALIZED', '뉴스레터 제목 및 부제 특화형', 0, 'Y', 'N', @title_specialized_tgt_cmpny_no, 'STATIC', NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'subtitle', 'sourcePath', 'subtitle', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'priceChangeFrom', 'sourcePath', 'priceChangeFrom', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'priceChangeTo', 'sourcePath', 'priceChangeTo', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_summary,    'SUMMARY_BODY',     'TEXT',      'GENERAL', '뉴스레터 핵심 요약',             0, 'Y', 'N', NULL, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'summaryText', 'sourcePath', 'summaryText', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_news_flow,  'NEWS_FLOW_CHART',  'CHART',     'GENERAL', '주요 뉴스 Flow 및 Brent 유가 추이', 0, 'Y', 'Y', NULL, 'CHART_SERIES',  @main_chart_code,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'MAIN_CHART', 'refCode', @main_chart_code),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'aggregationTime', 'sourcePath', 'aggregationTime', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY('title', 'aggregationTime')
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_news_flow,  'NEWS_ITEM',        'NEWS_CARD', 'GENERAL', '개별 뉴스 카드 (repeatable 1..N)', 1, 'Y', 'N', NULL, 'NEWS_EVENT',    NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'NEWS_EVENT'),
     'query', JSON_OBJECT(
         'fromDate', @news_from_date,
         'toDate', @news_to_date
     ),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'newsId', 'sourcePath', 'newsId', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'timestamp', 'sourcePath', 'timestamp', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'createdDate', 'sourcePath', 'createdDate', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'createTime', 'sourcePath', 'createTime', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'source', 'sourcePath', 'source', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'originalUrl', 'sourcePath', 'originalUrl', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'showInChart', 'sourcePath', 'showInChart', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'trend', 'sourcePath', 'trend', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY('fromDate', 'toDate'),
         'state', JSON_ARRAY('plotOnChart'),
         'contentInput', JSON_ARRAY('originalUrl')
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference1, 'REFERENCE_HEADER', 'TEXT',      'GENERAL', '참고 데이터 제목/설명',           0, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'description', 'sourcePath', 'description', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference1, 'REFERENCE_CHART',  'CHART',     'GENERAL', '참고 차트 블록',                 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES',  CONCAT('dedi:', @reference1_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'DEDICATED_CHART', 'refCode', @reference1_chart_type),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'aggregationTime', 'sourcePath', 'aggregationTime', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference1, 'REFERENCE_CONTENT', 'TEXT',     'GENERAL', '참고 내용 블록',                 2, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference2, 'REFERENCE_HEADER', 'TEXT',      'GENERAL', '참고 데이터 제목/설명',           0, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'description', 'sourcePath', 'description', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference2, 'REFERENCE_CHART',  'CHART',     'GENERAL', '참고 차트 블록',                 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES',  CONCAT('cltn:', @reference2_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference2_chart_type),
     'query', JSON_OBJECT(
         'fromDate', @collection_from_date,
         'toDate', @collection_to_date
     ),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'aggregationTime', 'sourcePath', 'aggregationTime', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY('fromDate', 'toDate'),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference2, 'REFERENCE_CONTENT', 'TEXT',     'GENERAL', '참고 내용 블록',                 2, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference3, 'REFERENCE_HEADER', 'TEXT',      'GENERAL', '참고 데이터 제목/설명',           0, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'description', 'sourcePath', 'description', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference3, 'REFERENCE_TABLE',  'TABLE',     'GENERAL', '참고 테이블 블록',               1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'DYNAMIC_QUERY', CONCAT('pref_table:', @reference3_pref_table_id),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'PREFERENCE_TABLE', 'refId', @reference3_pref_table_id),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'tableData', 'sourcePath', 'tableData', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference3, 'REFERENCE_CONTENT', 'TEXT',     'GENERAL', '참고 내용 블록',                 2, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference4, 'REFERENCE_HEADER', 'TEXT',      'GENERAL', '참고 데이터 제목/설명',           0, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'title', 'sourcePath', 'title', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'description', 'sourcePath', 'description', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference4, 'REFERENCE_CHART',  'CHART',     'GENERAL', '참고 차트 블록',                 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES',  CONCAT('cltn:', @reference4_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference4_chart_type),
     'query', JSON_OBJECT(
         'fromDate', @collection_from_date,
         'toDate', @collection_to_date
     ),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'aggregationTime', 'sourcePath', 'aggregationTime', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY('fromDate', 'toDate'),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_reference4, 'REFERENCE_CONTENT', 'TEXT',     'GENERAL', '참고 내용 블록',                 2, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
(@sec_footer,     'FOOTER_NOTICE',    'TEXT',      'GENERAL', '푸터 안내 문구',                 0, 'Y', 'N', NULL, 'STATIC',        NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'showLogo', 'sourcePath', 'showLogo', 'required', FALSE),
         JSON_OBJECT('fieldKey', 'footerContent', 'sourcePath', 'footerContent', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY('showLogo')
     ))
 ), 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    blk_nm = VALUES(blk_nm),
    blk_type_cd = VALUES(blk_type_cd),
    blk_fmt_cd = VALUES(blk_fmt_cd),
    blk_ord = VALUES(blk_ord),
    vsblty_yn = VALUES(vsblty_yn),
    plot_on_chrt_yn = VALUES(plot_on_chrt_yn),
    data_src_type_cd = VALUES(data_src_type_cd),
    data_src_ref = VALUES(data_src_ref),
    data_src_spec_json = VALUES(data_src_spec_json),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_tmplt_blk_id INTO @blk_cover_image_card
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_cover
  AND blk_cd = 'COVER_IMAGE_CARD'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_cover_info
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_cover
  AND blk_cd = 'COVER_INFO'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_date_issue_info
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_date_issue
  AND blk_cd = 'DATE_ISSUE_INFO'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_title_main_specialized
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_title_specialized
  AND blk_cd = 'TITLE_MAIN_SPECIALIZED'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_summary_body
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_summary
  AND blk_cd = 'SUMMARY_BODY'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_news_flow_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_news_flow
  AND blk_cd = 'NEWS_FLOW_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_news_item
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_news_flow
  AND blk_cd = 'NEWS_ITEM'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference1_header
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference1
  AND blk_cd = 'REFERENCE_HEADER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference1_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference1
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference1_content
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference1
  AND blk_cd = 'REFERENCE_CONTENT'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference2_header
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference2
  AND blk_cd = 'REFERENCE_HEADER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference2_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference2
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference2_content
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference2
  AND blk_cd = 'REFERENCE_CONTENT'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference3_header
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference3
  AND blk_cd = 'REFERENCE_HEADER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference3_table
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference3
  AND blk_cd = 'REFERENCE_TABLE'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference3_content
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference3
  AND blk_cd = 'REFERENCE_CONTENT'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference4_header
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference4
  AND blk_cd = 'REFERENCE_HEADER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference4_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference4
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference4_content
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference4
  AND blk_cd = 'REFERENCE_CONTENT'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_footer_notice
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_footer
  AND blk_cd = 'FOOTER_NOTICE'
  AND del_yn = 'N';

/* =========================================================
 * 4) BLOCK FIELD CLEANUP
 * ========================================================= */

UPDATE tmd_nwsltrtmpltblk
SET del_yn = 'Y',
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_sctn_id IN (@sec_reference1, @sec_reference2, @sec_reference3, @sec_reference4)
  AND blk_cd = 'REFERENCE_TEXT'
  AND del_yn = 'N';

DELETE FROM tmd_nwsltrtmpltblkfld
WHERE nws_ltr_tmplt_blk_id IN (@blk_news_flow_chart, @blk_news_item, @blk_reference1_chart, @blk_reference2_chart, @blk_reference3_table)
  AND fld_key IN (
      'chartTitle',
      'chartSubTitle',
      'date',
      'time',
      'originalLink',
      'series.high',
      'series.low',
      'series.open',
      'series.close',
      'series.volume',
      'series.left',
      'series.right',
      'chartData',
      'createdDate',
      'source',
      'title',
      'content',
      'plotOnChart'
  );

DELETE FROM tmd_nwsltrtmpltblkfld
WHERE nws_ltr_tmplt_blk_id = @blk_news_item
  AND fld_key = 'aggregationTime';

/* =========================================================
 * 5) BLOCK FIELD INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltblkfld
(
    nws_ltr_tmplt_blk_id,
    fld_key,
    fld_nm,
    fld_ord,
    col_mapping,
    is_editable_yn,
    is_required_yn,
    fld_meta,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
-- COVER_IMAGE_CARD
(@blk_cover_image_card, 'imageBase64',  '커버 이미지 Base64', 0, NULL, 'N', 'Y', '{"valueSource":"NEWSLETTER_MASTER_IMAGE_URL","transform":"ENCODE_IMAGE_BASE64","format":"base64","stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_cover_image_card, 'showImage',    '커버 이미지 노출',   1, NULL, 'Y', 'N', NULL,                                            'SYSTEM', 'SYSTEM'),

-- COVER_INFO
(@blk_cover_info, 'publishCycle',          '발행 주기',       0, NULL, 'N', 'Y', '{"valueSource":"NEWSLETTER_MASTER_PERIOD_TYPE","transform":"ENUM_CODE","options":["DAILY","WEEKLY","MONTHLY","QUARTERLY","ON-DEMAND"],"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_cover_info, 'newsletterName',        '뉴스레터명',      1, NULL, 'N', 'Y', '{"valueSource":"NEWSLETTER_MASTER_NAME","maxLength":100,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_cover_info, 'newsletterDescription', '뉴스레터 소개',   2, NULL, 'N', 'N', '{"valueSource":"NEWSLETTER_MASTER_EXPLANATION","maxLength":500,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- DATE_ISSUE_INFO
(@blk_date_issue_info, 'publishDate', '날짜',         0, NULL, 'N', 'Y', '{"valueSource":"NEWSLETTER_PUBLISH_HISTORY_PUBLISH_DTM","transform":"FORMAT_DATE","format":"YYYY-MM-DD","stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_date_issue_info, 'issueNumber', '이슈 번호',     1, NULL, 'N', 'Y', '{"valueSource":"NEWSLETTER_PUBLISH_HISTORY_ISSUE_NO","stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- TITLE_MAIN_SPECIALIZED
(@blk_title_main_specialized, 'title',           '뉴스레터 제목',          0, NULL, 'Y', 'Y', '{"valueSource":"NEWSLETTER_PUBLISH_HISTORY_TITLE","maxLength":100,"editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_title_main_specialized, 'subtitle',        '뉴스레터 부제',          1, NULL, 'Y', 'N', '{"maxLength":100,"editorMutable":true,"hasDefaultValue":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_title_main_specialized, 'priceChangeFrom', '유가 변동 시작값',      2, NULL, 'Y', 'Y', '{"inputType":"number","unit":"USD/bbl","editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_title_main_specialized, 'priceChangeTo',   '유가 변동 종료값',      3, NULL, 'Y', 'Y', '{"inputType":"number","unit":"USD/bbl","editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- SUMMARY_BODY
(@blk_summary_body, 'summaryText', '핵심 요약', 0, NULL, 'Y', 'Y', '{"valueSource":"NEWSLETTER_PUBLISH_HISTORY_AI_SUMMARY","maxLength":1000,"editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- NEWS_FLOW_CHART
(@blk_news_flow_chart, 'title',           '뉴스 Flow 제목', 0, NULL, 'Y', 'Y', '{"defaultValue":"주요 뉴스 Flow 및 Brent 유가 추이","editorMutable":true,"stateScoped":true,"stateMutable":true}', 'SYSTEM', 'SYSTEM'),
(@blk_news_flow_chart, 'aggregationTime', '집계 시간 설명', 1, NULL, 'Y', 'Y', '{"defaultValue":"뉴욕시간 전일 20:00부터 당일 17:00까지","editorMutable":true,"stateScoped":true,"stateMutable":true}', 'SYSTEM', 'SYSTEM'),
(@blk_news_flow_chart, 'timestamps',      '차트 시간축',    2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_flow_chart, 'series',          '차트 시리즈',    3, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- NEWS_ITEM (repeatable template block)
(@blk_news_item, 'newsId',       '뉴스 ID',    0, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'title',        '제목',       1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'content',      '내용',       2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'timestamp',    '발생시각',   3, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'createdDate',  '날짜',       4, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'createTime',   '시간',       5, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'source',       '출처',       6, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'originalUrl',  '원문 링크',   7, NULL, 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'showInChart',  '차트 표시',   8, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'trend',        '추세',       9, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE1_HEADER
(@blk_reference1_header, 'title',       '참고 제목',   0, NULL, 'Y', 'Y', '{"defaultValue":"WTI 및 달러 인덱스 5분 차트","maxLength":100,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_reference1_header, 'description', '참고 설명',   1, NULL, 'Y', 'N', '{"defaultValue":"WTI: 청색, 좌측\\n달러인덱스: 회색, 우측","maxLength":500,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE1_CHART
(@blk_reference1_chart, 'aggregationTime', '집계 시간 설명', 0, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference1_chart, 'timestamps',      '참조 차트 시간축', 1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference1_chart, 'series',          '차트 시리즈',    2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE1_CONTENT
(@blk_reference1_content, 'content', '내용', 0, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"hasDefaultValue":false,"defaultValue":null,"persistWhenEmpty":false,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE2_HEADER
(@blk_reference2_header, 'title',       '참고 제목',   0, NULL, 'Y', 'Y', '{"maxLength":100,"editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_reference2_header, 'description', '참고 설명',   1, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"stateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE2_CHART
(@blk_reference2_chart, 'aggregationTime', '집계 시간 설명', 0, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference2_chart, 'timestamps',      '참조 차트 시간축', 1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference2_chart, 'series',          '차트 시리즈',    2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE2_CONTENT
(@blk_reference2_content, 'content', '내용', 0, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"hasDefaultValue":false,"defaultValue":null,"persistWhenEmpty":false,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE3_HEADER
(@blk_reference3_header, 'title',       '참고 제목',   0, NULL, 'Y', 'Y', '{"defaultValue":"유가 추이","maxLength":100,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_reference3_header, 'description', '참고 설명',   1, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE3_TABLE
(@blk_reference3_table, 'tableData', '테이블 데이터', 0, NULL, 'N', 'Y', '{"valueType":"TableRes","sourceDatabase":"midb","sourceTable":"tmd_nws_ltr_pref_tbl","sourceColumn":"json_value","baseCondition":"pref_tbl_cd=''OIL_PRC_TRND''","runtimeCondition":"nws_ltr_pub_id"}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE3_CONTENT
(@blk_reference3_content, 'content', '내용', 0, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"hasDefaultValue":false,"defaultValue":null,"persistWhenEmpty":false,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE4_HEADER
(@blk_reference4_header, 'title',       '참고 제목',   0, NULL, 'Y', 'Y', '{"defaultValue":"미국 10년물 국채금리 및 미 10년-2년물 국채 금리 스프레드 추이","maxLength":100,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_reference4_header, 'description', '참고 설명',   1, NULL, 'Y', 'N', '{"defaultValue":"미국 10년물 국채금리: 청색,좌측\\n미국 10년-2년물 국채금리 스프레드: 회색,우측","maxLength":500,"editorMutable":true,"templateScoped":true,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE4_CHART
(@blk_reference4_chart, 'aggregationTime', '집계 시간 설명', 0, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference4_chart, 'timestamps',      '참조 차트 시간축', 1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference4_chart, 'series',          '차트 시리즈',    2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE4_CONTENT
(@blk_reference4_content, 'content', '내용', 0, NULL, 'Y', 'N', '{"maxLength":500,"editorMutable":true,"hasDefaultValue":false,"defaultValue":null,"persistWhenEmpty":false,"stateScoped":false,"stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- FOOTER_NOTICE
(@blk_footer_notice, 'showLogo',      '로고 노출 여부', 0, NULL, 'Y', 'N', '{"defaultValue":true,"editorMutable":true,"stateScoped":true,"stateMutable":true}', 'SYSTEM', 'SYSTEM'),
(@blk_footer_notice, 'footerContent', '푸터 내용',     1, NULL, 'Y', 'Y', '{"defaultValue":"본 메일은 발신전용입니다. 수신거부 또는 문의는 담당자에게 연락해 주세요.","maxLength":1000,"editorMutable":true,"templateScoped":true,"stateMutable":false}', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    fld_nm = VALUES(fld_nm),
    fld_ord = VALUES(fld_ord),
    col_mapping = VALUES(col_mapping),
    is_editable_yn = VALUES(is_editable_yn),
    is_required_yn = VALUES(is_required_yn),
    fld_meta = VALUES(fld_meta),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

/* =========================================================
 * 6) POST-CHECK
 * ========================================================= */

SELECT t.nws_ltr_tmplt_id, t.nws_ltr_mstr_id, t.tmplt_nm, t.schm_ver
FROM tmd_nwsltrtmpltm t
WHERE t.nws_ltr_tmplt_id = @tmplt_id;

SELECT s.sctn_cd, s.sctn_nm, s.sctn_ord, b.blk_cd, b.blk_nm, b.blk_ord, b.data_src_type_cd, b.data_src_ref, b.data_src_spec_json
FROM tmd_nwsltrtmpltsctn s
JOIN tmd_nwsltrtmpltblk b
  ON b.nws_ltr_tmplt_sctn_id = s.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.del_yn = 'N'
  AND b.del_yn = 'N'
ORDER BY s.sctn_ord, b.blk_ord;

SELECT f.nws_ltr_tmplt_blk_id, f.fld_key, f.fld_ord, f.col_mapping, f.fld_meta
FROM tmd_nwsltrtmpltblkfld f
WHERE f.nws_ltr_tmplt_blk_id IN (
    @blk_cover_image_card,
    @blk_cover_info,
    @blk_date_issue_info,
    @blk_title_main_specialized,
    @blk_summary_body,
    @blk_news_flow_chart,
    @blk_news_item,
    @blk_reference1_header,
    @blk_reference1_chart,
    @blk_reference1_content,
    @blk_reference2_header,
    @blk_reference2_chart,
    @blk_reference2_content,
    @blk_reference3_header,
    @blk_reference3_table,
    @blk_reference3_content,
    @blk_reference4_header,
    @blk_reference4_chart,
    @blk_reference4_content,
    @blk_footer_notice
)
  AND f.del_yn = 'N'
ORDER BY f.nws_ltr_tmplt_blk_id, f.fld_ord;

COMMIT;
