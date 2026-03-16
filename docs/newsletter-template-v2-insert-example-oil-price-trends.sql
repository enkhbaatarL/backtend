-- Newsletter Template V2 Insert Example
-- Scenario: Figma-based "Oil Price Trends" sample layout
-- Date: 2026-03-16
--
-- Assumptions
-- 1. newsletter master id = 7
-- 2. main chart code = 'BRENT'
-- 3. reference collection chart type = 'WTI_SRF5000'
-- 4. reference preference table config id = 101
-- 5. target company is not specialized
--
-- Purpose
-- - Provide a concrete example row set for the 4-table v2 template model
-- - Make section/block/field composition reviewable from the UI layout

START TRANSACTION;

/* =========================================================
 * 0) INPUT VARIABLES
 * ========================================================= */

SET @newsletter_master_id := 1;
SET @main_chart_code := 'BRENT';
SET @reference_chart_type := 'WTI_SRF5000';
SET @reference_pref_table_id := 101;

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
    'Oil Price Trends 기본 템플릿',
    '피그마 시안 기준 예시 템플릿. 헤더/요약/메인차트/뉴스리스트/참조차트+표 포함',
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

SELECT nws_ltr_tmplt_v2_id
INTO @tmplt_id
FROM tmd_nwsltrtmpltm
WHERE nws_ltr_mstr_id = @newsletter_master_id
  AND del_yn = 'N';

/* =========================================================
 * 2) SECTION INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrtmpltsctn
(
    nws_ltr_tmplt_v2_id,
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
(@tmplt_id, 'HEADER',      'FIXED',   '뉴스레터 헤더',                    0, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'AI_SUMMARY',  'FIXED',   '핵심 요약',                        1, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'MAIN_CHART',  'FIXED',   '주요 뉴스 Flow 및 Brent 유가 추이', 2, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'NEWS_EVENTS', 'FIXED',   '관련 뉴스',                        3, 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@tmplt_id, 'REF_01',      'GENERAL', 'WTI 및 SRF5000 5분 차트',          4, 'Y', NULL, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    sctn_nm = VALUES(sctn_nm),
    sctn_type_cd = VALUES(sctn_type_cd),
    sctn_ord = VALUES(sctn_ord),
    vsblty_yn = VALUES(vsblty_yn),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_tmplt_sctn_id INTO @sec_header
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_v2_id = @tmplt_id
  AND sctn_cd = 'HEADER'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_ai_summary
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_v2_id = @tmplt_id
  AND sctn_cd = 'AI_SUMMARY'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_main_chart
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_v2_id = @tmplt_id
  AND sctn_cd = 'MAIN_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_news_events
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_v2_id = @tmplt_id
  AND sctn_cd = 'NEWS_EVENTS'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_sctn_id INTO @sec_ref_01
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_v2_id = @tmplt_id
  AND sctn_cd = 'REF_01'
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
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@sec_header,      'NEWSLETTER_TITLE', 'TITLE',     'GENERAL', 'Oil Price Trends',              0, 'Y', 'N', NULL, 'STATIC',        NULL,                                    'SYSTEM', 'SYSTEM'),
(@sec_ai_summary,  'AI_SUMMARY_TEXT',  'TEXT',      'GENERAL', '핵심 요약',                      0, 'Y', 'N', NULL, 'STATIC',        NULL,                                    'SYSTEM', 'SYSTEM'),
(@sec_main_chart,  'MAIN_FLOW_CHART',  'CHART',     'GENERAL', '주요 뉴스 Flow 및 Brent 유가 추이', 0, 'Y', 'Y', NULL, 'CHART_SERIES',  @main_chart_code,                         'SYSTEM', 'SYSTEM'),
(@sec_news_events, 'NEWS_EVENT_LIST',  'NEWS_CARD', 'GENERAL', '관련 뉴스',                      0, 'Y', 'Y', NULL, 'NEWS_EVENT',    NULL,                                    'SYSTEM', 'SYSTEM'),
(@sec_ref_01,      'REF_CLTN_CHART_1', 'CHART',     'GENERAL', 'WTI 및 SRF5000 5분 차트',        0, 'Y', 'N', NULL, 'CHART_SERIES',  CONCAT('cltn:', @reference_chart_type),  'SYSTEM', 'SYSTEM'),
(@sec_ref_01,      'REF_TABLE_1',      'TABLE',     'GENERAL', '시장 지표 요약 표',              1, 'Y', 'N', NULL, 'DYNAMIC_QUERY', CONCAT('pref_table:', @reference_pref_table_id), 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    blk_nm = VALUES(blk_nm),
    blk_type_cd = VALUES(blk_type_cd),
    blk_fmt_cd = VALUES(blk_fmt_cd),
    blk_ord = VALUES(blk_ord),
    vsblty_yn = VALUES(vsblty_yn),
    plot_on_chrt_yn = VALUES(plot_on_chrt_yn),
    data_src_type_cd = VALUES(data_src_type_cd),
    data_src_ref = VALUES(data_src_ref),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_tmplt_blk_id INTO @blk_title
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_header
  AND blk_cd = 'NEWSLETTER_TITLE'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_ai_summary
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_ai_summary
  AND blk_cd = 'AI_SUMMARY_TEXT'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_main_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_main_chart
  AND blk_cd = 'MAIN_FLOW_CHART'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_news_events
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_news_events
  AND blk_cd = 'NEWS_EVENT_LIST'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_ref_chart
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_ref_01
  AND blk_cd = 'REF_CLTN_CHART_1'
  AND del_yn = 'N';

SELECT nws_ltr_tmplt_blk_id INTO @blk_ref_table
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_ref_01
  AND blk_cd = 'REF_TABLE_1'
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
-- HEADER / TITLE
(@blk_title, 'text',        '뉴스레터명', 0, 'STATIC:Oil Price Trends', 'Y', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_title, 'publishDate', '발행일',     1, 'STATIC:${publishDate}',   'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_title, 'issueLabel',  '발행호수',   2, 'STATIC:${issueNo}',       'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- AI SUMMARY / TEXT
(@blk_ai_summary, 'text', '요약본문', 0, 'STATIC:${aiSummary}', 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),

-- MAIN CHART / CHART_SERIES
(@blk_main_chart, 'timestamps',    '차트시간축',   0, 'tif_nws_ltr_chrt.timestamp', 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_main_chart, 'series.high',   '고가시리즈',   1, 'tif_nws_ltr_chrt.high_1',    'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_main_chart, 'series.low',    '저가시리즈',   2, 'tif_nws_ltr_chrt.low_1',     'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_main_chart, 'series.open',   '시가시리즈',   3, 'tif_nws_ltr_chrt.open_prc',  'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_main_chart, 'series.close',  '종가시리즈',   4, 'tif_nws_ltr_chrt.trdprc_1',  'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_main_chart, 'series.volume', '거래량시리즈', 5, 'tif_nws_ltr_chrt.num_moves', 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- NEWS EVENTS / NEWS_CARD
(@blk_news_events, 'items',            '뉴스목록',  0, 'tmd_nws_ltr_evnt.*',         'N', 'Y', '{"maxItems": 6, "orderBy": "evnt_crt_tm DESC"}', 'SYSTEM', 'SYSTEM'),
(@blk_news_events, 'items.*.title',    '뉴스제목',  1, 'tmd_nws_ltr_evnt.evnt_titl', 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_events, 'items.*.content',  '뉴스본문',  2, 'tmd_nws_ltr_evnt.evnt_cntt', 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE CHART / COLLECTION CHART
(@blk_ref_chart, 'timestamps',   '참조차트시간축', 0, 'DYNAMIC_QUERY:timestamp', 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_ref_chart, 'series.close', '참조차트값',     1, 'DYNAMIC_QUERY:value',     'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),

-- REFERENCE TABLE / DYNAMIC QUERY
(@blk_ref_table, 'description', '표설명',   0, 'STATIC:',              'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_ref_table, 'columns',     '테이블컬럼', 1, 'DYNAMIC_QUERY:header', 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_ref_table, 'rows',        '테이블행',   2, 'DYNAMIC_QUERY:detail', 'Y', 'Y', NULL, 'SYSTEM', 'SYSTEM')
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

SELECT t.nws_ltr_tmplt_v2_id, t.nws_ltr_mstr_id, t.tmplt_nm, t.schm_ver
FROM tmd_nwsltrtmpltm t
WHERE t.nws_ltr_tmplt_v2_id = @tmplt_id;

SELECT s.sctn_cd, s.sctn_nm, s.sctn_ord
FROM tmd_nwsltrtmpltsctn s
WHERE s.nws_ltr_tmplt_v2_id = @tmplt_id
  AND s.del_yn = 'N'
ORDER BY s.sctn_ord;

SELECT b.blk_cd, b.blk_type_cd, b.data_src_type_cd, b.data_src_ref, b.blk_ord
FROM tmd_nwsltrtmpltblk b
WHERE b.nws_ltr_tmplt_sctn_id IN (@sec_header, @sec_ai_summary, @sec_main_chart, @sec_news_events, @sec_ref_01)
  AND b.del_yn = 'N'
ORDER BY b.nws_ltr_tmplt_sctn_id, b.blk_ord;

SELECT f.fld_key, f.fld_nm, f.fld_ord, f.is_editable_yn
FROM tmd_nwsltrtmpltblkfld f
WHERE f.nws_ltr_tmplt_blk_id IN (@blk_title, @blk_ai_summary, @blk_main_chart, @blk_news_events, @blk_ref_chart, @blk_ref_table)
  AND f.del_yn = 'N'
ORDER BY f.nws_ltr_tmplt_blk_id, f.fld_ord;

COMMIT;

/* =========================================================
 * 6) ROLLBACK
 * ========================================================= */

-- DELETE FROM tmd_nwsltrtmpltblkfld WHERE nws_ltr_tmplt_blk_id IN (@blk_title, @blk_ai_summary, @blk_main_chart, @blk_news_events, @blk_ref_chart, @blk_ref_table);
-- DELETE FROM tmd_nwsltrtmpltblk WHERE nws_ltr_tmplt_blk_id IN (@blk_title, @blk_ai_summary, @blk_main_chart, @blk_news_events, @blk_ref_chart, @blk_ref_table);
-- DELETE FROM tmd_nwsltrtmpltsctn WHERE nws_ltr_tmplt_sctn_id IN (@sec_header, @sec_ai_summary, @sec_main_chart, @sec_news_events, @sec_ref_01);
-- DELETE FROM tmd_nwsltrtmpltm WHERE nws_ltr_tmplt_v2_id = @tmplt_id;
