-- Newsletter Template V2 Insert Example
-- Scenario: Figma-based "Oil Price Trends" editor structure
-- Date: 2026-03-26
-- Aligned to actual test DB data (tmd_nwsltrtmpltsctn, tmd_nwsltrtmpltsctnfld,
-- tmd_nwsltrtmpltblk, tmd_nwsltrtmpltblkfld)
--
-- Assumptions
-- 1. newsletter master name = 'Oil Price Trends'
-- 2. newsletter master id fallback = 1
-- 3. main chart code = 'LCOc1'
-- 4. reference specialized company no = 2020
-- 5. reference1 dedicated chart type = 'WTI_DI'
-- 6. reference2 collection chart type = 'WTI_SNP500'
-- 7. reference3 preference table code = 'OIL_PRC_TRND', pref_table refId = 1
-- 8. reference4 collection chart type = 'GOV10_2SPRD'
-- 9. this script assumes tmd_nwsltrtmpltsctnfld table exists
--
-- Structure (section → block):
--   COVER         → COVER_IMAGE_CARD, COVER_INFO(publishCycle only)
--   DATE_ISSUE    → (no active blocks — fields on section level)
--   TITLE_SPECIALIZED → (no active blocks — fields on section level)
--   SUMMARY       → (no active blocks — fields on section level)
--   NEWS_FLOW     → NEWS_FLOW_CHART(timestamps,series), NEWS_ITEM
--   REFERENCE(5)  → REFERENCE_CHART(dedi:WTI_DI)
--   REFERENCE(6)  → REFERENCE_CHART(cltn:WTI_SNP500)
--   REFERENCE(7)  → REFERENCE_TABLE(pref_table:1)
--   REFERENCE(8)  → REFERENCE_CHART(cltn:GOV10_2SPRD)
--   FOOTER        → FOOTER_NOTICE(showLogo only)
--
-- Section-level fields (tmd_nwsltrtmpltsctnfld):
--   COVER: newsletterName, newsletterDescription
--   DATE_ISSUE: publishDate, issueNumber
--   TITLE_SPECIALIZED: title, subtitle, priceChangeFrom, priceChangeTo
--   SUMMARY: summaryText
--   NEWS_FLOW: title, aggregationTime
--   REFERENCE(5): title, description
--   REFERENCE(6): title, description
--   REFERENCE(7): title, description
--   REFERENCE(8): title, description
--   FOOTER: footerContent

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
SET @reference3_pref_table_ref_id := 1;
SET @reference4_chart_type := 'GOV10_2SPRD';

SELECT nws_ltr_mstr_id
INTO @newsletter_master_id
FROM tmd_nws_ltr_mstr
WHERE mstr_nm = @newsletter_master_name
  AND del_yn = 'N'
LIMIT 1;

SET @newsletter_master_id := COALESCE(@newsletter_master_id, 1);

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
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(
    @newsletter_master_id,
    'Test Template',
    'v2 Energy 용 최신 피그마 편집 화면 기준 템플릿',
    'SYSTEM',
    'SYSTEM'
)
ON DUPLICATE KEY UPDATE
    tmplt_nm = VALUES(tmplt_nm),
    tmplt_desc = VALUES(tmplt_desc),
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
(@tmplt_id, 'COVER',              'FIXED',       'Cover',         0, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'DATE_ISSUE',         'FIXED',       'Date & Issue',  1, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'TITLE_SPECIALIZED',  'SPECIALIZED', 'Title',         2, 'Y', @title_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'SUMMARY',            'FIXED',       'Summary',       3, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'NEWS_FLOW',          'SPECIALIZED', 'News Flow',     4, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE',          'SPECIALIZED', 'Reference',     5, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE',          'SPECIALIZED', 'Reference',     6, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE',          'SPECIALIZED', 'Reference',     7, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REFERENCE',          'SPECIALIZED', 'Reference',     8, 'Y', @reference_specialized_tgt_cmpny_no, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'FOOTER',             'FIXED',       'Footer',        9, 'Y', NULL, 'SYSTEM', 'SYSTEM')
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

-- REFERENCE sections share sctn_cd='REFERENCE', distinguished by sctn_ord
SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference1
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE'
  AND sctn_ord = 5
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference2
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE'
  AND sctn_ord = 6
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference3
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE'
  AND sctn_ord = 7
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_reference4
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'REFERENCE'
  AND sctn_ord = 8
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_footer
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'FOOTER'
  AND del_yn = 'N';

/* =========================================================
 * 2-1) SECTION FIELD INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltsctnfld
(
    nws_ltr_tmplt_sctn_id,
    fld_key,
    fld_nm,
    fld_ord,
    is_editable_yn,
    is_required_yn,
    fld_meta,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
-- COVER
(@sec_cover, 'newsletterName',        '뉴스레터명',      1, 'N', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_MASTER_NAME","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),
(@sec_cover, 'newsletterDescription', '뉴스레터 소개',   2, 'N', 'N', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_MASTER_EXPLANATION","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),

-- DATE_ISSUE
(@sec_date_issue, 'publishDate', '날짜',       0, 'N', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_PUBLISH_HISTORY_PUBLISH_DTM","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),
(@sec_date_issue, 'issueNumber', '이슈 번호',   1, 'N', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_PUBLISH_HISTORY_ISSUE_NO","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),

-- TITLE_SPECIALIZED
(@sec_title_specialized, 'title',           '뉴스레터 제목',     0, 'Y', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_PUBLISH_HISTORY_TITLE","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),
(@sec_title_specialized, 'subtitle',        '뉴스레터 부제',     1, 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@sec_title_specialized, 'priceChangeFrom', '유가 변동 시작값',  2, 'Y', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"PRICE_CHANGE_FROM","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),
(@sec_title_specialized, 'priceChangeTo',   '유가 변동 종료값',  3, 'Y', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"PRICE_CHANGE_TO","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),

-- SUMMARY
(@sec_summary, 'summaryText', '핵심 요약', 0, 'Y', 'Y', '{"valueSource":{"srcRef":null,"srcTypeCd":"NEWSLETTER_PUBLISH_HISTORY_AI_SUMMARY","srcSpecJson":null}}', 'SYSTEM', 'SYSTEM'),

-- NEWS_FLOW
(@sec_news_flow, 'title',           '뉴스 Flow 제목', 0, 'Y', 'Y', '{"defaultValue":"주요 뉴스 Flow 및 Brent 유가 추이"}', 'SYSTEM', 'SYSTEM'),
(@sec_news_flow, 'aggregationTime', '집계 시간 설명', 1, 'Y', 'Y', '{"defaultValue":"뉴욕시간 전일 20:00부터 당일 17:00까지"}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE1 (WTI_DI dedicated chart, sctn_ord=5)
(@sec_reference1, 'title',       '참고 제목', 0, 'Y', 'Y', '{"defaultValue":"WTI 및 달러 인덱스 5분 차트"}', 'SYSTEM', 'SYSTEM'),
(@sec_reference1, 'description', '참고 설명', 1, 'Y', 'N', '{"defaultValue":"WTI: 청색, 좌측\\n달러인덱스: 회색, 우측"}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE2 (WTI_SNP500 collection chart, sctn_ord=6)
(@sec_reference2, 'title',       '참고 제목', 0, 'Y', 'Y', '{"defaultValue":"WTI 및 S&P 500"}', 'SYSTEM', 'SYSTEM'),
(@sec_reference2, 'description', '참고 설명', 1, 'Y', 'N', '{"defaultValue":"WTI: 청색, 좌측\\nS&P 500: 회색, 우측"}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE3 (유가 추이 table, sctn_ord=7)
(@sec_reference3, 'title',       '참고 제목', 0, 'Y', 'Y', '{"defaultValue":"유가 추이"}', 'SYSTEM', 'SYSTEM'),
(@sec_reference3, 'description', '참고 설명', 1, 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE4 (GOV10_2SPRD collection chart, sctn_ord=8)
(@sec_reference4, 'title',       '참고 제목', 0, 'Y', 'Y', '{"defaultValue":"미국 10년물 국채금리 및 미 10년-2년물 국채 금리 스프레드 추이"}', 'SYSTEM', 'SYSTEM'),
(@sec_reference4, 'description', '참고 설명', 1, 'Y', 'N', '{"defaultValue":"미국 10년물 국채금리: 청색,좌측\\n미국 10년-2년물 국채금리 스프레드: 회색,우측"}', 'SYSTEM', 'SYSTEM'),

-- FOOTER
(@sec_footer, 'footerContent', '푸터 내용', 1, 'Y', 'Y', '{"defaultValue":"본 메일은 발신전용입니다. 수신거부 또는 문의는 담당자에게 연락해 주세요."}', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    fld_nm = VALUES(fld_nm),
    fld_ord = VALUES(fld_ord),
    is_editable_yn = VALUES(is_editable_yn),
    is_required_yn = VALUES(is_required_yn),
    fld_meta = VALUES(fld_meta),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

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
-- COVER: COVER_IMAGE_CARD
(@sec_cover, 'COVER_IMAGE_CARD', 'IMAGE', 'GENERAL', '뉴스레터 커버 이미지', 0, 'Y', 'N', NULL, 'STATIC', NULL,
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
-- COVER: COVER_INFO (publishCycle only)
(@sec_cover, 'COVER_INFO', 'TEXT', 'GENERAL', '뉴스레터 기본 정보', 1, 'Y', 'N', NULL, 'STATIC', NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'publishCycle', 'sourcePath', 'publishCycle', 'required', TRUE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- NEWS_FLOW: NEWS_FLOW_CHART (timestamps, series only)
(@sec_news_flow, 'NEWS_FLOW_CHART', 'CHART', 'GENERAL', '주요 뉴스 Flow 및 Brent 유가 추이', 0, 'Y', 'Y', NULL, 'CHART_SERIES', @main_chart_code,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'MAIN_CHART', 'refCode', @main_chart_code),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
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
-- NEWS_FLOW: NEWS_ITEM
(@sec_news_flow, 'NEWS_ITEM', 'NEWS_CARD', 'GENERAL', '개별 뉴스 카드 (repeatable 1..N)', 1, 'Y', 'N', NULL, 'NEWS_EVENT', NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'NEWS_EVENT'),
     'query', JSON_OBJECT(),
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
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- REFERENCE1 (WTI_DI dedicated chart): REFERENCE_CHART
(@sec_reference1, 'REFERENCE_CHART', 'CHART', 'GENERAL', '참고 차트 블록', 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES', CONCAT('dedi:', @reference1_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'DEDICATED_CHART', 'refCode', @reference1_chart_type),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- REFERENCE2 (WTI_SNP500 collection chart): REFERENCE_CHART
(@sec_reference2, 'REFERENCE_CHART', 'CHART', 'GENERAL', '참고 차트 블록', 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES', CONCAT('cltn:', @reference2_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference2_chart_type),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY('fromDate', 'toDate'),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- REFERENCE3 (유가 추이 table): REFERENCE_TABLE
(@sec_reference3, 'REFERENCE_TABLE', 'TABLE', 'GENERAL', '참고 테이블 블록', 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'DYNAMIC_QUERY', CONCAT('pref_table:', @reference3_pref_table_ref_id),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'PREFERENCE_TABLE', 'refId', @reference3_pref_table_ref_id),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'tableData', 'sourcePath', 'tableData', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY(),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- REFERENCE4 (GOV10_2SPRD collection chart): REFERENCE_CHART
(@sec_reference4, 'REFERENCE_CHART', 'CHART', 'GENERAL', '참고 차트 블록', 1, 'Y', 'N', @reference_specialized_tgt_cmpny_no, 'CHART_SERIES', CONCAT('cltn:', @reference4_chart_type),
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference4_chart_type),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE),
         JSON_OBJECT('fieldKey', 'content', 'sourcePath', 'content', 'required', FALSE)
     )),
     'transform', JSON_ARRAY(),
     'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
         'query', JSON_ARRAY('fromDate', 'toDate'),
         'state', JSON_ARRAY(),
         'contentInput', JSON_ARRAY()
     ))
 ), 'SYSTEM', 'SYSTEM'),
-- FOOTER: FOOTER_NOTICE (showLogo only)
(@sec_footer, 'FOOTER_NOTICE', 'TEXT', 'GENERAL', '푸터 안내 문구', 0, 'Y', 'N', NULL, 'STATIC', NULL,
 JSON_OBJECT(
     'locator', JSON_OBJECT('kind', 'INLINE'),
     'query', JSON_OBJECT(),
     'assign', JSON_OBJECT('fields', JSON_ARRAY(
         JSON_OBJECT('fieldKey', 'showLogo', 'sourcePath', 'showLogo', 'required', FALSE)
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

/* -- Soft-delete legacy blocks (DATE_ISSUE_INFO, TITLE_MAIN_SPECIALIZED, SUMMARY_BODY,
   -- REFERENCE_CONTENT) that have been promoted to section fields or removed
UPDATE tmd_nwsltrtmpltblk
SET del_yn = 'Y',
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_sctn_id IN (@sec_date_issue, @sec_title_specialized, @sec_summary)
  AND blk_cd IN ('DATE_ISSUE_INFO', 'TITLE_MAIN_SPECIALIZED', 'SUMMARY_BODY')
  AND del_yn = 'N';

UPDATE tmd_nwsltrtmpltblk
SET del_yn = 'Y',
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_sctn_id IN (@sec_reference1, @sec_reference2, @sec_reference3, @sec_reference4)
  AND blk_cd = 'REFERENCE_CONTENT'
  AND del_yn = 'N';
*/

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

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference1_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference1
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference2_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference2
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference3_table
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference3
  AND blk_cd = 'REFERENCE_TABLE'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_reference4_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_reference4
  AND blk_cd = 'REFERENCE_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_footer_notice
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_footer
  AND blk_cd = 'FOOTER_NOTICE'
  AND del_yn = 'N';

/* =========================================================
 * 4) BLOCK FIELD INSERT
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
(@blk_cover_image_card, 'imageBase64',  '커버 이미지 Base64', 0, NULL, 'N', 'Y', '{"format":"base64","transform":"ENCODE_IMAGE_BASE64","valueSource":"NEWSLETTER_MASTER_IMAGE_URL","stateMutable":false}', 'SYSTEM', 'SYSTEM'),
(@blk_cover_image_card, 'showImage',    '커버 이미지 노출',   1, NULL, 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),

-- COVER_INFO (publishCycle only)
(@blk_cover_info, 'publishCycle', '발행 주기', 0, NULL, 'N', 'Y', '{"options":["DAILY","WEEKLY","MONTHLY","QUARTERLY","ON-DEMAND"],"transform":"ENUM_CODE","valueSource":"NEWSLETTER_MASTER_PERIOD_TYPE","stateMutable":false}', 'SYSTEM', 'SYSTEM'),

-- NEWS_FLOW_CHART (timestamps, series only)
(@blk_news_flow_chart, 'timestamps', '차트 시간축', 2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_flow_chart, 'series',     '차트 시리즈', 3, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- NEWS_ITEM
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

-- REFERENCE1_CHART (WTI_DI)
(@blk_reference1_chart, 'timestamps', '참조 차트 시간축', 0, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference1_chart, 'series',     '차트 시리즈',    1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference1_chart, 'content',    '내용',          2, NULL, 'Y', 'N', '{"maxLength":500,"stateScoped":false,"defaultValue":null,"stateMutable":false,"editorMutable":true,"hasDefaultValue":false,"persistWhenEmpty":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE2_CHART (WTI_SNP500)
(@blk_reference2_chart, 'timestamps', '참조 차트 시간축', 0, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference2_chart, 'series',     '차트 시리즈',    1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference2_chart, 'content',    '내용',          2, NULL, 'Y', 'N', '{"maxLength":500,"stateScoped":false,"defaultValue":null,"stateMutable":false,"editorMutable":true,"hasDefaultValue":false,"persistWhenEmpty":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE3_TABLE (유가 추이)
(@blk_reference3_table, 'tableData', '테이블 데이터', 0, NULL, 'N', 'Y', '{"valueType":"TableRes","sourceDatabase":"midb","sourceTable":"tmd_nws_ltr_pref_tbl","sourceColumn":"json_value","baseCondition":"pref_tbl_cd=''OIL_PRC_TRND''","runtimeCondition":"nws_ltr_pub_id"}', 'SYSTEM', 'SYSTEM'),
(@blk_reference3_table, 'content',   '내용',         1, NULL, 'Y', 'N', '{"maxLength":500,"stateScoped":false,"defaultValue":null,"stateMutable":false,"editorMutable":true,"hasDefaultValue":false,"persistWhenEmpty":false}', 'SYSTEM', 'SYSTEM'),

-- REFERENCE4_CHART (GOV10_2SPRD)
(@blk_reference4_chart, 'timestamps', '참조 차트 시간축', 0, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference4_chart, 'series',     '차트 시리즈',    1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_reference4_chart, 'content',    '내용',          2, NULL, 'Y', 'N', '{"maxLength":500,"stateScoped":false,"defaultValue":null,"stateMutable":false,"editorMutable":true,"hasDefaultValue":false,"persistWhenEmpty":false}', 'SYSTEM', 'SYSTEM'),

-- FOOTER_NOTICE (showLogo only)
(@blk_footer_notice, 'showLogo', '로고 노출 여부', 0, NULL, 'Y', 'N', '{"defaultValue":true}', 'SYSTEM', 'SYSTEM')
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
 * 5) POST-CHECK
 * ========================================================= */

SELECT t.nws_ltr_tmplt_id, t.nws_ltr_mstr_id, t.tmplt_nm
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

SELECT sf.nws_ltr_tmplt_sctn_id, sf.fld_key, sf.fld_ord, sf.is_editable_yn, sf.is_required_yn, sf.fld_meta
FROM tmd_nwsltrtmpltsctnfld sf
JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = sf.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.del_yn = 'N'
  AND sf.del_yn = 'N'
ORDER BY s.sctn_ord, sf.fld_ord;

SELECT f.nws_ltr_tmplt_blk_id, f.fld_key, f.fld_ord, f.col_mapping, f.fld_meta
FROM tmd_nwsltrtmpltblkfld f
WHERE f.nws_ltr_tmplt_blk_id IN (
    @blk_cover_image_card,
    @blk_cover_info,
    @blk_news_flow_chart,
    @blk_news_item,
    @blk_reference1_chart,
    @blk_reference2_chart,
    @blk_reference3_table,
    @blk_reference4_chart,
    @blk_footer_notice
)
  AND f.del_yn = 'N'
ORDER BY f.nws_ltr_tmplt_blk_id, f.fld_ord;

COMMIT;
