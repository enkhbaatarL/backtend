-- Newsletter Issue State Insert Example
-- Scenario: Figma-based "Oil Price Trends" editor instance
-- Date: 2026-03-17
--
-- Assumptions
-- 1. template insert example has already been applied
-- 2. repeated block rows are allowed in `tmd_nwsltrorgissblkstat`
-- 3. original issue row exists in `tmd_nwsltrorgissm`
--
-- Purpose
-- - Provide example issue section/block state rows for the editor-aligned section structure
-- - Treat each `tmd_nwsltrorgissblkstat` row as one rendered block instance
-- - Show repeatable block usage with `NEWS_ITEM`
-- - Reflect specialized reference layouts:
--   `REFERENCE1 = HEADER + CHART + CONTENT`
--   `REFERENCE2 = HEADER + CHART + CONTENT`
--   `REFERENCE3 = HEADER + TABLE + CONTENT`
--   `REFERENCE4 = HEADER + CHART + CONTENT`

START TRANSACTION;

/* =========================================================
 * 0) INPUT VARIABLES
 * ========================================================= */

SET @newsletter_master_name := 'Oil Price Trends';
SET @newsletter_master_id := NULL;
SET @issue_no := 55;

SELECT nws_ltr_mstr_id
INTO @newsletter_master_id
FROM tmd_nws_ltr_mstr
WHERE mstr_nm = @newsletter_master_name
  AND del_yn = 'N'
LIMIT 1;

SET @newsletter_master_id := COALESCE(@newsletter_master_id, 1);

/* =========================================================
 * 1) LOOKUP TEMPLATE / ISSUE IDS
 * ========================================================= */

SELECT nws_ltr_tmplt_id
INTO @tmplt_id
FROM tmd_nwsltrtmpltm
WHERE nws_ltr_mstr_id = @newsletter_master_id
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_id
INTO @issue_original_id
FROM tmd_nwsltrorgissm
WHERE nws_ltr_mstr_id = @newsletter_master_id
  AND issno = @issue_no
  AND del_yn = 'N';

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

SELECT nws_ltr_tmplt_sctn_id INTO @sec_title
FROM tmd_nwsltrtmpltsctn
WHERE nws_ltr_tmplt_id = @tmplt_id
  AND sctn_cd = 'TITLE'
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

SELECT nws_ltr_tmplt_blk_id INTO @blk_title_main
FROM tmd_nwsltrtmpltblk
WHERE nws_ltr_tmplt_sctn_id = @sec_title
  AND blk_cd = 'TITLE_MAIN'
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
 * 2) SECTION STATE INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrorgisssctnstat
(
    nws_ltr_org_iss_id,
    nws_ltr_tmplt_sctn_id,
    sctn_ord,
    vsblty_yn,
    sctn_titl,
    tgt_cmpny_no,
    sctn_stat_json,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@issue_original_id, @sec_cover,      0, 'Y', 'Cover',       NULL, JSON_OBJECT('collapsed', FALSE, 'showDivider', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_date_issue, 1, 'Y', 'Date & Issue', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_title,      2, 'Y', 'Title',       NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_summary,    3, 'Y', 'Summary',     NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_news_flow,  4, 'Y', 'News Flow',   NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_reference1, 5, 'Y', 'Reference 1', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_reference2, 6, 'Y', 'Reference 2', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_reference3, 7, 'Y', 'Reference 3', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_reference4, 8, 'Y', 'Reference 4', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE),                       'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_footer,     9, 'Y', 'Footer',      NULL, JSON_OBJECT('collapsed', FALSE, 'showDivider', TRUE, 'removed', FALSE),  'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    sctn_ord = VALUES(sctn_ord),
    vsblty_yn = VALUES(vsblty_yn),
    sctn_titl = VALUES(sctn_titl),
    tgt_cmpny_no = VALUES(tgt_cmpny_no),
    sctn_stat_json = VALUES(sctn_stat_json),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_cover
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_cover
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_date_issue
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_date_issue
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_title
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_title
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_summary
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_summary
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_news_flow
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_news_flow
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_reference
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_reference1
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_reference2
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_reference2
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_reference3
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_reference3
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_reference4
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_reference4
  AND del_yn = 'N';

SELECT nws_ltr_org_iss_sctn_stat_id INTO @sec_state_footer
FROM tmd_nwsltrorgisssctnstat
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND nws_ltr_tmplt_sctn_id = @sec_footer
  AND del_yn = 'N';

/* =========================================================
 * 3) BLOCK STATE INSERT
 * ========================================================= */

INSERT INTO tmd_nwsltrorgissblkstat
(
    nws_ltr_org_iss_id,
    nws_ltr_org_iss_sctn_stat_id,
    nws_ltr_tmplt_blk_id,
    blk_ord,
    vsblty_yn,
    blk_titl,
    tgt_cmpny_no,
    blk_stat_json,
    crt_usr_logn_id,
    mod_usr_logn_id
)
VALUES
(@issue_original_id, @sec_state_cover,      @blk_cover_image_card, 0, 'Y', '뉴스레터 커버 이미지',                 NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'contentInput', JSON_OBJECT('showImage', TRUE)), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_cover,      @blk_cover_info,       1, 'Y', '뉴스레터 기본 정보',                   NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_date_issue, @blk_date_issue_info,  0, 'Y', '일자 및 이슈 넘버',                  NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'contentInput', JSON_OBJECT('publishDate', '2025-11-27', 'issueNumber', 55)), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_title,      @blk_title_main,       0, 'Y', '뉴스레터 제목 및 부제',               NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_summary,    @blk_summary_body,     0, 'Y', '뉴스레터 핵심요약',                  NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_news_flow,  @blk_news_flow_chart,  0, 'Y', '주요 뉴스 Flow 및 Brent 유가 추이',  NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'contentInput', JSON_OBJECT('title', '주요 뉴스 Flow 및 Brent 유가 추이', 'aggregationTime', '뉴욕시간 전일 20:00부터 당일 17:00까지')), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_news_flow,  @blk_news_item,        1, 'Y', 'Valero-Phillips 66, 정제사 합병 이슈', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'newsId', 1001, 'plotOnChart', TRUE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_news_flow,  @blk_news_item,        2, 'Y', 'EU, 러시아산 에너지 수입 감축 발표',   NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'newsId', 1002, 'plotOnChart', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_news_flow,  @blk_news_item,        3, 'Y', '테슬라, 새 배터리 비중 공개',         NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'newsId', 1003, 'plotOnChart', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference,  @blk_reference1_header,  0, 'Y', '미국 10년물 국채금리 및 미 10년-2년물 국채 금리 스프레드 추이', NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference,  @blk_reference1_chart,   1, 'Y', '미국 10년물 국채금리 및 스프레드 차트',                       NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'fromDate', '2026-01-29', 'toDate', '2026-01-29', 'market', 'OIL'), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference,  @blk_reference1_content, 2, 'Y', '내용',                                                        NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference2, @blk_reference2_header,  0, 'Y', '시장 지표 요약 표',                                            NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference2, @blk_reference2_chart,   1, 'Y', 'Reference 2 Chart',                                           NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'fromDate', '2026-01-29', 'toDate', '2026-01-29', 'market', 'OIL'), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference2, @blk_reference2_content, 2, 'Y', '내용',                                                        NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference3, @blk_reference3_header,  0, 'Y', 'Reference 3 Header',                                          NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference3, @blk_reference3_table,   1, 'Y', '시장 지표 요약 표',                                            NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference3, @blk_reference3_content, 2, 'Y', '내용',                                                        NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference4, @blk_reference4_header,  0, 'Y', 'Reference 4 Header',                                          NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference4, @blk_reference4_chart,   1, 'Y', 'Reference 4 Chart',                                           NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'fromDate', '2026-01-29', 'toDate', '2026-01-29', 'market', 'OIL'), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_reference4, @blk_reference4_content, 2, 'Y', '내용',                                                        NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE), 'SYSTEM', 'SYSTEM'),
(@issue_original_id, @sec_state_footer,     @blk_footer_notice,    0, 'Y', '뉴스레터 푸터',                      NULL, JSON_OBJECT('collapsed', FALSE, 'removed', FALSE, 'contentInput', JSON_OBJECT('showLogo', TRUE)), 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    blk_ord = VALUES(blk_ord),
    vsblty_yn = VALUES(vsblty_yn),
    blk_titl = VALUES(blk_titl),
    tgt_cmpny_no = VALUES(tgt_cmpny_no),
    blk_stat_json = VALUES(blk_stat_json),
    mod_usr_logn_id = VALUES(mod_usr_logn_id),
    mod_dtm = CURRENT_TIMESTAMP;

/* =========================================================
 * 4) POST-CHECK
 * ========================================================= */

SELECT s.nws_ltr_org_iss_sctn_stat_id,
       ts.sctn_cd,
       s.sctn_ord,
       s.vsblty_yn,
       s.sctn_titl
FROM tmd_nwsltrorgisssctnstat s
JOIN tmd_nwsltrtmpltsctn ts
  ON ts.nws_ltr_tmplt_sctn_id = s.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_org_iss_id = @issue_original_id
  AND s.del_yn = 'N'
ORDER BY s.sctn_ord;

SELECT b.nws_ltr_org_iss_blk_stat_id,
       tb.blk_cd,
       b.blk_ord,
       b.vsblty_yn,
       b.blk_titl,
       b.blk_stat_json
FROM tmd_nwsltrorgissblkstat b
JOIN tmd_nwsltrtmpltblk tb
  ON tb.nws_ltr_tmplt_blk_id = b.nws_ltr_tmplt_blk_id
WHERE b.nws_ltr_org_iss_id = @issue_original_id
  AND b.del_yn = 'N'
ORDER BY b.nws_ltr_org_iss_sctn_stat_id, b.blk_ord, b.nws_ltr_org_iss_blk_stat_id;

COMMIT;
