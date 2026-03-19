# Newsletter Template V2 Field Ownership Matrix

- date: `2026-03-17`
- status: `working draft`
- purpose: Newsletter v2 section/block/field별 기본값 출처, state 소유 여부, editor 수정 가능 여부, snapshot 조립 우선순위를 한눈에 관리하기 위한 기준 문서

## 1. 공통 해석 기준

### 1-1. 기본 용어
- `기본값 출처`: editor/delta/state 값이 없을 때 snapshot 조립 시 참조하는 기본 소스
- `state 소유`: `issue section state` 또는 `issue block state`가 직접 값을 가진다는 뜻
- `editor 수정`: 사용자가 발행본 편집 화면에서 수정 가능한지 여부
- `template 기본값`: template 자체에 고정값 또는 기본 설정이 정의된 경우
- `source-derived 기본값`: master/publish history/resolver 결과에서 파생되는 기본값

### 1-2. snapshot 조립 우선순위
필드별 최종값 조립 우선순위는 아래를 기본으로 한다.

1. editor delta/snapshot 값
2. issue state 값
3. template 기본값 또는 source-derived 기본값

단, `state 소유 = 아니오`인 필드는 2번을 건너뛴다.

### 1-3. 상태 키 공통 규칙

| 레벨 | 항목 | 기본값 | 사용자 수정 | 의미 |
|---|---|---:|---|---|
| Section | `vsblty_yn` | `Y` | 가능 | 보이기/숨기기 |
| Section | `removed` | `false` | 가능 | 항상 존재하는 issue section state 기본 키, 발행본 편집에서 제외 |
| Section | `del_yn` | `N` | 사용자 수정 아님 | DB row soft delete |
| Block | `vsblty_yn` | `Y` | 가능 | 보이기/숨기기 |
| Block | `removed` | 없음 | 가능 | 선택적으로만 존재하는 issue block state 키, 발행본 편집에서 제외 |
| Block | `del_yn` | `N` | 사용자 수정 아님 | DB row soft delete |

### 1-4. Ownership Metadata Boundary

- `fld_meta.valueSource`는 execution truth source가 아니라 ownership/documentation metadata다.
- source-derived 기본값 계산 책임은 application ownership resolver에 있다.
- `data_src_spec_json`은 canonical assignment/result shape를 정의하고, field 기본값의 구체 source path를 DB contract에 노출하지 않는다.
- `overridePolicy.issueState.contentInput`은 editable field subset이면서 issue block state가 직접 소유하는 값만 포함한다.
- non-editable system-derived field는 `contentInput`에 넣지 않는다.

## 2. Section / Block / Field Matrix

| Section | Block | Field | 기본값 출처 | state 소유 | editor 수정 | snapshot 우선순위 | 비고 |
|---|---|---|---|---|---|---|---|
| `COVER` | `COVER_IMAGE_CARD` | `imageBase64` | `tmd_nws_ltr_mstr.img_url -> encodeImageToBase64(...)` | 아니오 | 아니오 | `source-derived` | master-derived, `stateMutable=false` |
| `COVER` | `COVER_IMAGE_CARD` | `showImage` | template default `true` | 예 | 예 | `delta/snapshot -> state -> template default` | `contentInput.showImage` |
| `COVER` | `COVER_INFO` | `publishCycle` | `tmd_nws_ltr_mstr.perd_type -> getPeriodType().getCode()` | 아니오 | 아니오 | `source-derived` | master-derived |
| `COVER` | `COVER_INFO` | `newsletterName` | `tmd_nws_ltr_mstr.mstr_nm` | 아니오 | 아니오 | `source-derived` | master-derived |
| `COVER` | `COVER_INFO` | `newsletterDescription` | `tmd_nws_ltr_mstr.expn` | 아니오 | 아니오 | `source-derived` | master-derived |
| `DATE_ISSUE` | `DATE_ISSUE_INFO` | `publishDate` | `tmd_nws_ltr_pub_hist.pub_dtm` | 아니오 | 아니오 | `source-derived` | `PUBLISH_CONTEXT` 기본값 |
| `DATE_ISSUE` | `DATE_ISSUE_INFO` | `issueNumber` | `tmd_nws_ltr_pub_hist.issno` | 아니오 | 아니오 | `source-derived` | `PUBLISH_CONTEXT` 기본값 |
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `title` | `tmd_nws_ltr_pub_hist.nws_ltr_titl` | 아니오 | 예 | `delta/snapshot -> source-derived` | publish history 기본값 + editor 수정 |
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `subtitle` | 기본값 없음 | 아니오 | 예 | `delta/snapshot only` | 입력 시에만 존재 |
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `priceChangeFrom` | 미정 | 아니오 | 예 | `delta/snapshot -> default/source TBD` | source 확정 필요 |
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `priceChangeTo` | 미정 | 아니오 | 예 | `delta/snapshot -> default/source TBD` | source 확정 필요 |
| `SUMMARY` | `SUMMARY_BODY` | `summaryText` | `tmd_nws_ltr_pub_hist.ai_smry` | 아니오 | 예 | `delta/snapshot -> source-derived` | AI summary 기본값 + editor 수정 |
| `NEWS_FLOW` | `NEWS_FLOW_CHART` | `title` | template default `"주요 뉴스 Flow 및 Brent 유가 추이"` | 예 | 예 | `delta/snapshot -> state -> template default` | 필수입력 |
| `NEWS_FLOW` | `NEWS_FLOW_CHART` | `aggregationTime` | template default `"뉴욕시간 전일 20:00부터 당일 17:00까지"` | 예 | 예 | `delta/snapshot -> state -> template default` | 필수입력 |
| `NEWS_FLOW` | `NEWS_FLOW_CHART` | `timestamps` | `midb.tif_nws_ltr_chrt` (`code='LCOc1'` + `src_nws_ltr_pub_id`) | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_FLOW_CHART` | `series` | `midb.tif_nws_ltr_chrt` (`code='LCOc1'` + `src_nws_ltr_pub_id`) | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `newsId` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve, item 식별자 |
| `NEWS_FLOW` | `NEWS_ITEM` | `title` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `content` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `timestamp` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `createdDate` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `createTime` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `source` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `NEWS_FLOW` | `NEWS_ITEM` | `originalUrl` | 기본값 없음 | 아니오 | 예 | `snapshot only -> resolved content` | runtime-expanded block, 백엔드 issue state 미사용 |
| `NEWS_FLOW` | `NEWS_ITEM` | `showInChart` | news resolver fallback | 아니오 | 예 | `snapshot only -> resolved content` | runtime-expanded block, snapshot top-level `plotOnChart`와 함께 노출 |
| `NEWS_FLOW` | `NEWS_ITEM` | `trend` | news resolver | 아니오 | 아니오 | `resolved content` | runtime resolve |
| `REFERENCE1` | `REFERENCE_HEADER` | `title` | template default `"WTI 및 달러 인덱스 5분 차트"` | 아니오 | 예 | `delta/snapshot -> template default` | 필수입력, state 저장 없음, `maxLength=100` |
| `REFERENCE1` | `REFERENCE_HEADER` | `description` | template default `"WTI: 청색, 좌측\n달러인덱스: 회색, 우측"` | 아니오 | 예 | `delta/snapshot -> template default` | 선택입력, state 저장 없음, `maxLength=500` |
| `REFERENCE1` | `REFERENCE_CHART` | `timestamps` | chart resolver | 아니오 | 아니오 | `resolved content` | `REFERENCE1` 기준, runtime resolve |
| `REFERENCE1` | `REFERENCE_CHART` | `series` | chart resolver | 아니오 | 아니오 | `resolved content` | `REFERENCE1` 기준, runtime resolve |
| `REFERENCE1` | `REFERENCE_CONTENT` | `content` | template field 정의만 존재, 기본값 `null` 또는 `''` | 아니오 | 예 | `delta/snapshot only` | state 저장 없음, 사용자 입력 시 delta/snapshot만 저장, `maxLength=500` |
| `REFERENCE2` | `REFERENCE_HEADER` | `title` | 기본값 없음 | 아니오 | 예 | `delta/snapshot only` | 필수입력, `maxLength=100` |
| `REFERENCE2` | `REFERENCE_HEADER` | `description` | 기본값 없음 | 아니오 | 예 | `delta/snapshot only` | 선택입력, `maxLength=500` |
| `REFERENCE2` | `REFERENCE_CHART` | `timestamps` | collection chart resolver (`WTI_SNP500`) | 아니오 | 아니오 | `resolved content` | `midb-ske.tif_ske_chrt.timestamp` + `midb.tif_nws_ltr_chrt.timestamp` |
| `REFERENCE2` | `REFERENCE_CHART` | `series` | collection chart resolver (`WTI_SNP500`) | 아니오 | 아니오 | `resolved content` | `.SPX`/`CLc1`, colors `grey`/`blue`, `ChartData.series` |
| `REFERENCE2` | `REFERENCE_CONTENT` | `content` | template field 정의만 존재, 기본값 `null` 또는 `''` | 아니오 | 예 | `delta/snapshot only` | state 저장 없음, 사용자 입력 시 delta/snapshot만 저장, `maxLength=500` |
| `REFERENCE3` | `REFERENCE_HEADER` | `title` | template default `"유가 추이"` | 아니오 | 예 | `delta/snapshot -> template default` | 필수입력, state 저장 없음, `maxLength=100` |
| `REFERENCE3` | `REFERENCE_HEADER` | `description` | 기본값 없음 | 아니오 | 예 | `delta/snapshot only` | 선택입력, state 저장 없음, `maxLength=500` |
| `REFERENCE3` | `REFERENCE_TABLE` | `tableData` | `midb.tmd_nws_ltr_pref_tbl.json_value` (`pref_tbl_cd='OIL_PRC_TRND'` + `nws_ltr_pub_id`) | 아니오 | 아니오 | `resolved content` | runtime resolve, `TableRes{headers, rows}` |
| `REFERENCE3` | `REFERENCE_CONTENT` | `content` | template field 정의만 존재, 기본값 `null` 또는 `''` | 아니오 | 예 | `delta/snapshot only` | state 저장 없음, 사용자 입력 시 delta/snapshot만 저장, `maxLength=500` |
| `REFERENCE4` | `REFERENCE_HEADER` | `title` | template default `"미국 10년물 국채금리 및 미 10년-2년물 국채 금리 스프레드 추이"` | 아니오 | 예 | `delta/snapshot -> template default` | `REFERENCE1` 동일 구조, 필수입력, state 저장 없음, `maxLength=100` |
| `REFERENCE4` | `REFERENCE_HEADER` | `description` | template default `"미국 10년물 국채금리: 청색,좌측\n미국 10년-2년물 국채금리 스프레드: 회색,우측"` | 아니오 | 예 | `delta/snapshot -> template default` | `REFERENCE1` 동일 구조, 선택입력, state 저장 없음, `maxLength=500` |
| `REFERENCE4` | `REFERENCE_CHART` | `timestamps` | collection chart resolver (`GOV10_2SPRD`) | 아니오 | 아니오 | `resolved content` | `midb.tif_interestratesr.date` + `tmd_nws_ltr_cltn_chrt.start_date/end_date` |
| `REFERENCE4` | `REFERENCE_CHART` | `series` | collection chart resolver (`GOV10_2SPRD`) | 아니오 | 아니오 | `resolved content` | `TRUS10T`/`TRUS10T-TRUS2YT`, colors `blue`/`grey`, `ChartData.series` |
| `REFERENCE4` | `REFERENCE_CONTENT` | `content` | template field 정의만 존재, 기본값 `null` 또는 `''` | 아니오 | 예 | `delta/snapshot only` | state 저장 없음, 사용자 입력 시 delta/snapshot만 저장, `maxLength=500` |
| `FOOTER` | `FOOTER_NOTICE` | `showLogo` | template default `true` | 예 | 예 | `delta/snapshot -> state -> template default` | `contentInput.showLogo` |
| `FOOTER` | `FOOTER_NOTICE` | `footerContent` | template 기본값 | 아니오 | 예 | `delta/snapshot -> template default` | template fixed default + editor 수정 |

## 3. 현재 state 소유 필드

| Section | Block | state에 직접 저장되는 값 |
|---|---|---|
| `*` | section-level | `removed` |
| `COVER` | `COVER_IMAGE_CARD` | `showImage` |
| `NEWS_FLOW` | `NEWS_FLOW_CHART` | `removed`, `title`, `aggregationTime` |
| `REFERENCE1` | `REFERENCE_CHART` | `removed` |
| `REFERENCE2` | `REFERENCE_CHART` | `removed`, `fromDate`, `toDate` |
| `REFERENCE4` | `REFERENCE_CHART` | `removed`, `fromDate`, `toDate` |
| `FOOTER` | `FOOTER_NOTICE` | `showLogo` |

## 3-1. Repeatable Block Rule

| Block | 정책 | 표현 방식 | 비고 |
|---|---|---|---|
| `NEWS_ITEM` | repeatable `1..N` | template block 1건 + `NEWS_EVENT` 조회 결과 개수만큼 runtime-expanded snapshot block 생성 | snapshot block id는 `blk-NEWS_ITEM-{newsId}`, 0건이면 `blk-NEWS_ITEM-empty` |

## 4. editor 수정 가능하지만 state 비소유인 필드

| Section | Block | Field |
|---|---|---|
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `title`, `subtitle`, `priceChangeFrom`, `priceChangeTo` |
| `SUMMARY` | `SUMMARY_BODY` | `summaryText` |
| `REFERENCE1` | `REFERENCE_HEADER` | `title`, `description` |
| `REFERENCE1` | `REFERENCE_CONTENT` | `content` |
| `REFERENCE2` | `REFERENCE_HEADER` | `title`, `description` |
| `REFERENCE2` | `REFERENCE_CONTENT` | `content` |
| `REFERENCE3` | `REFERENCE_HEADER` | `title`, `description` |
| `REFERENCE3` | `REFERENCE_CONTENT` | `content` |
| `REFERENCE4` | `REFERENCE_HEADER` | `title`, `description` |
| `REFERENCE4` | `REFERENCE_CONTENT` | `content` |
| `FOOTER` | `FOOTER_NOTICE` | `footerContent` |

## 5. 추가 확정 필요 항목

| Section | Block | Field | 확인 필요 |
|---|---|---|---|
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `priceChangeFrom` | 기본값 source가 publish history인지 별도 계산값인지 |
| `TITLE_SPECIALIZED` | `TITLE_MAIN_SPECIALIZED` | `priceChangeTo` | 기본값 source가 publish history인지 별도 계산값인지 |
| `REFERENCE1` | `REFERENCE_HEADER` | `title`, `description` | template 기본값 확정, issue state 미저장, 사용자 수정 시 delta/snapshot만 저장 |
| `REFERENCE1` | `REFERENCE_CONTENT` | `content` | 기본값은 `null`/`''`, issue state 미저장, delta/snapshot-only 저장 정책 확정 |
| `REFERENCE2` | `REFERENCE_HEADER` | `title`, `description` | title required / description optional 확정, 기본값 부여 여부 미정 |
| `REFERENCE2` | `REFERENCE_CHART` | `timestamps`, `series` | `WTI_SNP500` collection chart 기준. `tmd_nws_ltr_cltn_chrt`에서 `nws_ltr_pub_id + cltn_chrt_type`로 기간을 읽고, `midb-ske.tif_ske_chrt`에서 `.SPX`, `midb.tif_nws_ltr_chrt`에서 `CLc1`를 각각 조회 후 `ChartData`로 resolve |
| `REFERENCE2` | `REFERENCE_CONTENT` | `content` | 기본값은 `null`/`''`, issue state 미저장, delta/snapshot-only 저장 정책 확정 |
| `REFERENCE3` | `REFERENCE_HEADER` | `title`, `description` | title만 template 기본값 사용, description은 기본값 없음 |
| `REFERENCE3` | `REFERENCE_TABLE` | `tableData` | `midb.tmd_nws_ltr_pref_tbl.json_value`를 `pref_tbl_cd='OIL_PRC_TRND'` + `nws_ltr_pub_id` 조건으로 조회 후 `TableRes`로 resolve |
| `REFERENCE3` | `REFERENCE_CONTENT` | `content` | 기본값은 `null`/`''`, issue state 미저장, delta/snapshot-only 저장 정책 확정 |
| `REFERENCE4` | `REFERENCE_HEADER` | `title`, `description` | `REFERENCE1` 동일 구조, header 기본값 확정 |
| `REFERENCE4` | `REFERENCE_CHART` | `timestamps`, `series` | `GOV10_2SPRD` collection chart 기준. `tmd_nws_ltr_cltn_chrt`에서 `nws_ltr_pub_id + cltn_chrt_type`로 기간을 읽고, `midb.tif_interestratesr`에서 `TRUS10T`/`TRUS10T-TRUS2YT`를 각각 조회 후 `ChartData`로 resolve |
| `REFERENCE4` | `REFERENCE_CONTENT` | `content` | 기본값은 `null`/`''`, issue state 미저장, delta/snapshot-only 저장 정책 확정 |

## 6. 유지 원칙

- `is_editable_yn`은 editor 수정 가능 여부만 의미한다.
- `overridePolicy.issueState.contentInput`은 issue block state가 직접 소유하는 값만 포함한다.
- `fld_meta.valueSource`는 ownership metadata이며 execution truth source가 아니다.
- 사용자의 발행본 수정값은 state를 직접 바꾸지 않고 delta/snapshot 경로로 저장한다.
- `REFERENCE1~4.REFERENCE_CONTENT.content`는 template에 field만 정의하고 issue state에는 저장하지 않는다. 기본값은 `null` 또는 `''`로 해석하며, 사용자 입력이 있을 때만 delta/snapshot에 기록한다.
- published original detail은 항상 snapshot 우선으로 재현한다.
- `removed`는 editor 편집 제외 상태이며 `del_yn`과 의미가 다르다.
