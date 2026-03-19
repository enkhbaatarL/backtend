-- Newsletter V2 Patch Example
-- Purpose:
-- - Apply only post-seed changes after the original insert scripts were already executed
-- - Refresh `NEWS_ITEM` template block spec/fields to the latest single-card contract
-- - Remove deprecated `collapsed` from issue-state JSON rows
--
-- Safe scope:
-- - Template patch targets only the `NEWS_ITEM` block in `NEWS_FLOW`
-- - Issue-state patch targets only the selected original issue
-- - Legacy `NEWS_ITEM` issue-state rows are removed because canonical ownership moved to snapshot/revision

START TRANSACTION;

/* =========================================================
 * 0) INPUT VARIABLES
 * ========================================================= */

SET @newsletter_master_name := 'Oil Price Trends';
SET @newsletter_master_id := NULL;
SET @issue_no := 55;
SET @news_from_date := '2026-03-10';
SET @news_to_date := '2026-03-16';
SET @collection_from_date := '2026-03-10';
SET @collection_to_date := '2026-03-16';
SET @reference1_chart_type := 'WTI_DI';
SET @reference2_chart_type := 'WTI_SNP500';
SET @reference4_chart_type := 'GOV10_2SPRD';

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
  AND del_yn = 'N'
LIMIT 1;

SELECT nws_ltr_org_iss_id
INTO @issue_original_id
FROM tmd_nwsltrorgissm
WHERE nws_ltr_mstr_id = @newsletter_master_id
  AND issno = @issue_no
  AND del_yn = 'N'
LIMIT 1;

SELECT b.nws_ltr_tmplt_blk_id
INTO @blk_news_item
FROM tmd_nwsltrtmpltblk b
JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.sctn_cd = 'NEWS_FLOW'
  AND b.blk_cd = 'NEWS_ITEM'
  AND s.del_yn = 'N'
  AND b.del_yn = 'N'
LIMIT 1;

SELECT b.nws_ltr_tmplt_blk_id
INTO @blk_reference1_chart
FROM tmd_nwsltrtmpltblk b
JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.sctn_cd = 'REFERENCE1'
  AND b.blk_cd = 'REFERENCE_CHART'
  AND s.del_yn = 'N'
  AND b.del_yn = 'N'
LIMIT 1;

SELECT b.nws_ltr_tmplt_blk_id
INTO @blk_reference2_chart
FROM tmd_nwsltrtmpltblk b
JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.sctn_cd = 'REFERENCE2'
  AND b.blk_cd = 'REFERENCE_CHART'
  AND s.del_yn = 'N'
  AND b.del_yn = 'N'
LIMIT 1;

SELECT b.nws_ltr_tmplt_blk_id
INTO @blk_reference4_chart
FROM tmd_nwsltrtmpltblk b
JOIN tmd_nwsltrtmpltsctn s
  ON s.nws_ltr_tmplt_sctn_id = b.nws_ltr_tmplt_sctn_id
WHERE s.nws_ltr_tmplt_id = @tmplt_id
  AND s.sctn_cd = 'REFERENCE4'
  AND b.blk_cd = 'REFERENCE_CHART'
  AND s.del_yn = 'N'
  AND b.del_yn = 'N'
LIMIT 1;

/* =========================================================
 * 2) TEMPLATE BLOCK PATCH - NEWS_ITEM
 * ========================================================= */

UPDATE tmd_nwsltrtmpltblk
SET data_src_type_cd = 'NEWS_EVENT',
    data_src_ref = NULL,
    data_src_spec_json = JSON_OBJECT(
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
    ),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id = @blk_news_item
  AND del_yn = 'N';

DELETE FROM tmd_nwsltrtmpltblkfld
WHERE nws_ltr_tmplt_blk_id = @blk_news_item;

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
(@blk_news_item, 'newsId',      '뉴스 ID',   0, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'title',       '제목',      1, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'content',     '내용',      2, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'timestamp',   '발생시각',  3, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'createdDate', '날짜',      4, NULL, 'N', 'Y', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'createTime',  '시간',      5, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'source',      '출처',      6, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'originalUrl', '원문 링크', 7, NULL, 'Y', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'showInChart', '차트 표시', 8, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM'),
(@blk_news_item, 'trend',       '추세',      9, NULL, 'N', 'N', NULL, 'SYSTEM', 'SYSTEM');

/* =========================================================
 * 3) TEMPLATE BLOCK PATCH - REFERENCE_CHART
 * ========================================================= */

UPDATE tmd_nwsltrtmpltblk
SET data_src_type_cd = 'CHART_SERIES',
    data_src_ref = CONCAT('dedi:', @reference1_chart_type),
    data_src_spec_json = JSON_OBJECT(
        'locator', JSON_OBJECT('kind', 'DEDICATED_CHART', 'refCode', @reference1_chart_type),
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
    ),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id = @blk_reference1_chart
  AND del_yn = 'N';

UPDATE tmd_nwsltrtmpltblk
SET data_src_type_cd = 'CHART_SERIES',
    data_src_ref = CONCAT('cltn:', @reference2_chart_type),
    data_src_spec_json = JSON_OBJECT(
        'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference2_chart_type),
        'query', JSON_OBJECT(),
        'assign', JSON_OBJECT('fields', JSON_ARRAY(
            JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
            JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
        )),
        'transform', JSON_ARRAY(),
        'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
            'query', JSON_ARRAY('fromDate', 'toDate'),
            'state', JSON_ARRAY(),
            'contentInput', JSON_ARRAY()
        ))
    ),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id = @blk_reference2_chart
  AND del_yn = 'N';

UPDATE tmd_nwsltrtmpltblk
SET data_src_type_cd = 'CHART_SERIES',
    data_src_ref = CONCAT('cltn:', @reference4_chart_type),
    data_src_spec_json = JSON_OBJECT(
        'locator', JSON_OBJECT('kind', 'COLLECTION_CHART', 'refCode', @reference4_chart_type),
        'query', JSON_OBJECT(),
        'assign', JSON_OBJECT('fields', JSON_ARRAY(
            JSON_OBJECT('fieldKey', 'timestamps', 'sourcePath', 'timestamps', 'required', TRUE),
            JSON_OBJECT('fieldKey', 'series', 'sourcePath', 'series', 'required', TRUE)
        )),
        'transform', JSON_ARRAY(),
        'overridePolicy', JSON_OBJECT('issueState', JSON_OBJECT(
            'query', JSON_ARRAY('fromDate', 'toDate'),
            'state', JSON_ARRAY(),
            'contentInput', JSON_ARRAY()
        ))
    ),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id = @blk_reference4_chart
  AND del_yn = 'N';

DELETE FROM tmd_nwsltrtmpltblkfld
WHERE nws_ltr_tmplt_blk_id IN (@blk_reference1_chart, @blk_reference2_chart, @blk_reference4_chart)
  AND fld_key = 'aggregationTime';

UPDATE tmd_nwsltrtmpltblkfld
SET fld_ord = 0,
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id IN (@blk_reference1_chart, @blk_reference2_chart, @blk_reference4_chart)
  AND fld_key = 'timestamps'
  AND del_yn = 'N';

UPDATE tmd_nwsltrtmpltblkfld
SET fld_ord = 1,
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_tmplt_blk_id IN (@blk_reference1_chart, @blk_reference2_chart, @blk_reference4_chart)
  AND fld_key = 'series'
  AND del_yn = 'N';

/* =========================================================
 * 4) ISSUE STATE PATCH - REMOVE deprecated UI keys
 * ========================================================= */

UPDATE tmd_nwsltrorgisssctnstat
SET sctn_stat_json = JSON_REMOVE(sctn_stat_json, '$.collapsed', '$.removed', '$.showDivider'),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND del_yn = 'N'
  AND sctn_stat_json IS NOT NULL
  AND JSON_CONTAINS_PATH(sctn_stat_json, 'one', '$.collapsed', '$.removed', '$.showDivider');

UPDATE tmd_nwsltrorgissblkstat
SET blk_stat_json = JSON_REMOVE(blk_stat_json, '$.collapsed', '$.removed'),
    mod_usr_logn_id = 'SYSTEM',
    mod_dtm = CURRENT_TIMESTAMP
WHERE nws_ltr_org_iss_id = @issue_original_id
  AND del_yn = 'N'
  AND blk_stat_json IS NOT NULL
  AND JSON_CONTAINS_PATH(blk_stat_json, 'one', '$.collapsed', '$.removed');

UPDATE tmd_nwsltrorgissblkstat b
JOIN tmd_nwsltrorgisssctnstat s
  ON s.nws_ltr_org_iss_sctn_stat_id = b.nws_ltr_org_iss_sctn_stat_id
JOIN tmd_nwsltrtmpltsctn ts
  ON ts.nws_ltr_tmplt_sctn_id = s.nws_ltr_tmplt_sctn_id
JOIN tmd_nwsltrtmpltblk tb
  ON tb.nws_ltr_tmplt_blk_id = b.nws_ltr_tmplt_blk_id
SET b.blk_stat_json = JSON_REMOVE(b.blk_stat_json, '$.fromDate', '$.toDate'),
    b.mod_usr_logn_id = 'SYSTEM',
    b.mod_dtm = CURRENT_TIMESTAMP
WHERE b.nws_ltr_org_iss_id = @issue_original_id
  AND ts.sctn_cd = 'REFERENCE1'
  AND tb.blk_cd = 'REFERENCE_CHART'
  AND b.del_yn = 'N'
  AND b.blk_stat_json IS NOT NULL
  AND JSON_CONTAINS_PATH(b.blk_stat_json, 'one', '$.fromDate', '$.toDate');

UPDATE tmd_nwsltrorgissblkstat b
JOIN tmd_nwsltrorgisssctnstat s
  ON s.nws_ltr_org_iss_sctn_stat_id = b.nws_ltr_org_iss_sctn_stat_id
JOIN tmd_nwsltrtmpltsctn ts
  ON ts.nws_ltr_tmplt_sctn_id = s.nws_ltr_tmplt_sctn_id
JOIN tmd_nwsltrtmpltblk tb
  ON tb.nws_ltr_tmplt_blk_id = b.nws_ltr_tmplt_blk_id
SET b.del_yn = 'Y',
    b.mod_usr_logn_id = 'SYSTEM',
    b.mod_dtm = CURRENT_TIMESTAMP
WHERE b.nws_ltr_org_iss_id = @issue_original_id
  AND ts.sctn_cd = 'NEWS_FLOW'
  AND tb.blk_cd = 'NEWS_ITEM'
  AND b.del_yn = 'N';

/* =========================================================
 * 5) POST-CHECK
 * ========================================================= */

SELECT b.nws_ltr_tmplt_blk_id,
       b.blk_cd,
       b.data_src_type_cd,
       b.data_src_ref,
       b.data_src_spec_json
FROM tmd_nwsltrtmpltblk b
WHERE b.nws_ltr_tmplt_blk_id IN (@blk_news_item, @blk_reference1_chart, @blk_reference2_chart, @blk_reference4_chart);

SELECT f.nws_ltr_tmplt_blk_id,
       f.fld_key,
       f.fld_ord,
       f.is_editable_yn,
       f.is_required_yn
FROM tmd_nwsltrtmpltblkfld f
WHERE f.nws_ltr_tmplt_blk_id = @blk_news_item
  AND f.del_yn = 'N'
ORDER BY f.fld_ord;

SELECT s.nws_ltr_org_iss_sctn_stat_id,
       s.sctn_stat_json
FROM tmd_nwsltrorgisssctnstat s
WHERE s.nws_ltr_org_iss_id = @issue_original_id
  AND s.del_yn = 'N'
ORDER BY s.sctn_ord;

SELECT b.nws_ltr_org_iss_blk_stat_id,
       b.blk_ord,
       b.blk_stat_json
FROM tmd_nwsltrorgissblkstat b
WHERE b.nws_ltr_org_iss_id = @issue_original_id
  AND b.del_yn = 'N'
ORDER BY b.nws_ltr_org_iss_sctn_stat_id, b.blk_ord;

COMMIT;
