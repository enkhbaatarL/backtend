# Newsletter v2 — 템플릿 정의 → 발행 스냅샷 생성 파이프라인 설계

- version: `template-pipeline-design-2026-03-16-r1`
- owner: BE
- status: DRAFT (리뷰 대기)
- 참조:
  - `docs/newsletter-v2-design.md` (Living Doc)
  - `docs/newsletter-v2-ddl-canonical-index.md` (확정 DDL)
  - `docs/newsletter-v2-field-mapping.md` (필드 매핑)

---

## 0. 미결사항 결정안

### 0-1. section_type 코드 관리

**결정: VARCHAR 자유입력 + CHECK 제약**

| 대안 | 장점 | 단점 |
|---|---|---|
| CommonCodeDetail FK | 코드 중앙관리, UI 자동연동 | 조인 비용, 변경 시 공통코드 배포 필요 |
| **VARCHAR + CHECK** | 단순, 빠른 확장, 템플릿 내 자체 관리 | 코드 중복 위험(열거형으로 보완) |

- DB: `VARCHAR(30)` + `CHECK (sctn_type_cd IN ('FIXED','GENERAL','SPECIALIZED'))`
- Java: `SectionTypeCode` enum 으로 매핑
- 근거: section_type은 3개 고정값이며 공통코드까지 갈 규모가 아님. 향후 추가 시 DDL ALTER + enum 추가로 충분.

### 0-2. GENERAL 섹션 type 코드 부여 규칙

**결정: `GENERAL` 고정 코드 사용**

- 관리자가 섹션을 추가할 때 기본 type = `GENERAL`
- 특화형은 `SPECIALIZED` + `tgt_cmpny_no` 조합
- `FIXED`는 시스템 predefined 섹션(헤더/푸터)에만 사용

### 0-3. tgt_cmpny_no FK 설정 여부

**결정: FK 미설정, 논리적 참조만 유지**

- `tgt_cmpny_no`는 `tmd_nws_ltr_tmplt_sctn` 및 `tmd_nws_ltr_tmplt_blk` 테이블에 `INT UNSIGNED NULL` 컬럼으로 존재
- 실제 회사 테이블(`tcm_company` 등)에 대한 FK 제약은 걸지 않음
- 근거: 멀티 테넌트 환경에서 회사 테이블이 다른 데이터소스에 있을 수 있고, 템플릿 관리의 유연성이 더 중요함
- 유효성 검증은 애플리케이션 레벨에서 수행

### 0-4. NEWS_CARD 다건 조회 시 건수 제한 방식

**결정: BlockField의 `field_meta` JSON 내 `maxItems` 속성으로 제어**

```json
{
  "field_key": "news_items",
  "col_mapping": "tmd_nws_ltr_evnt.event_title,event_content,...",
  "field_meta": {
    "maxItems": 20,
    "orderBy": "evnt_crt_dtm DESC"
  }
}
```

- 기본값: 20건 (v1 하드코딩과 동일)
- 템플릿 관리자가 블록필드 단위로 조정 가능
- 스냅샷 생성 시 BlockDataResolver가 `maxItems`를 적용

---

## 1. 템플릿 엔티티 DDL 확정

### 1-1. 테이블 구조 개요

```
tmd_nwsltrtmpltm (Template)
 └── tmd_nwsltrtmpltsctn (Section)  [1:N]
      └── tmd_nwsltrtmpltblk (Block)  [1:N]
           └── tmd_nwsltrtmpltblkfld (BlockField)  [1:N]
```

### 1-2. DDL

#### Table 1: `tmd_nwsltrtmpltm` (뉴스레터템플릿기본)

```sql
CREATE TABLE tmd_nwsltrtmpltm
(
    nws_ltr_tmplt_v2_id  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿V2ID'
        PRIMARY KEY,
    crt_usr_logn_id      VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm              DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id      VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm              DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn               CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_mstr_id      INT UNSIGNED                          NOT NULL COMMENT '뉴스레터마스터ID',
    tmplt_nm             VARCHAR(200)                          NOT NULL COMMENT '템플릿명',
    tmplt_desc           VARCHAR(500)                          NULL     COMMENT '템플릿설명',
    schm_ver             SMALLINT UNSIGNED DEFAULT 1           NOT NULL COMMENT '스키마버전',

    CONSTRAINT tmd_nwsltrtmpltm_ux01 UNIQUE (nws_ltr_mstr_id),

    CONSTRAINT tmd_nwsltrtmpltm_fk01
        FOREIGN KEY (nws_ltr_mstr_id) REFERENCES tmd_nws_ltr_mstr (nws_ltr_mstr_id),

    CONSTRAINT tmd_nwsltrtmpltm_ck01
        CHECK (del_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터템플릿V2기본';
```

**설계 의도:**
- `nws_ltr_mstr_id` UNIQUE: 뉴스레터 마스터 1건당 활성 템플릿 1건 (버전 관리 없음, 확정사항 ⑥)
- 구조 변경 시 새 마스터+템플릿 세트를 생성하거나, 기존 템플릿을 직접 수정 (기발행분은 org_snpsht_json에 동결)

#### Table 2: `tmd_nwsltrtmpltsctn` (뉴스레터템플릿섹션)

```sql
CREATE TABLE tmd_nwsltrtmpltsctn
(
    nws_ltr_tmplt_sctn_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿섹션ID'
        PRIMARY KEY,
    crt_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_tmplt_v2_id   BIGINT UNSIGNED                      NOT NULL COMMENT '뉴스레터템플릿V2ID',
    sctn_cd               VARCHAR(50)                           NOT NULL COMMENT '섹션코드(고유식별자, e.g. HEADER, MAIN_CHART, REF_01)',
    sctn_type_cd          VARCHAR(30)                           NOT NULL COMMENT '섹션유형코드(FIXED,GENERAL,SPECIALIZED)',
    sctn_nm               VARCHAR(200)                          NOT NULL COMMENT '섹션명',
    sctn_ord              INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '섹션순서',
    vsblty_yn             CHAR(1)     DEFAULT 'Y'              NOT NULL COMMENT '노출여부',
    tgt_cmpny_no          INT UNSIGNED                          NULL     COMMENT '대상고객사번호(SPECIALIZED일 때)',

    CONSTRAINT tmd_nwsltrtmpltsctn_ux01 UNIQUE (nws_ltr_tmplt_v2_id, sctn_cd),

    CONSTRAINT tmd_nwsltrtmpltsctn_fk01
        FOREIGN KEY (nws_ltr_tmplt_v2_id) REFERENCES tmd_nwsltrtmpltm (nws_ltr_tmplt_v2_id),

    CONSTRAINT tmd_nwsltrtmpltsctn_ck01
        CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltsctn_ck02
        CHECK (sctn_type_cd IN ('FIXED', 'GENERAL', 'SPECIALIZED')),
    CONSTRAINT tmd_nwsltrtmpltsctn_ck03
        CHECK (vsblty_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltsctn_ck04
        CHECK (sctn_type_cd <> 'SPECIALIZED' OR tgt_cmpny_no IS NOT NULL)
)
    COMMENT 'TMD_뉴스레터템플릿섹션';

CREATE INDEX tmd_nwsltrtmpltsctn_ix01
    ON tmd_nwsltrtmpltsctn (nws_ltr_tmplt_v2_id, sctn_ord);
```

#### Table 3: `tmd_nwsltrtmpltblk` (뉴스레터템플릿블록)

```sql
CREATE TABLE tmd_nwsltrtmpltblk
(
    nws_ltr_tmplt_blk_id  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿블록ID'
        PRIMARY KEY,
    crt_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id       VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm               DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_tmplt_sctn_id BIGINT UNSIGNED                      NOT NULL COMMENT '뉴스레터템플릿섹션ID',
    blk_cd                VARCHAR(50)                           NOT NULL COMMENT '블록코드(고유식별자)',
    blk_type_cd           VARCHAR(30)                           NOT NULL COMMENT '블록타입코드(TEXT,CHART,TABLE,NEWS_CARD,IMAGE,TITLE)',
    blk_fmt_cd            VARCHAR(30) DEFAULT 'GENERAL'        NOT NULL COMMENT '블록형식코드(GENERAL,SPECIALIZED)',
    blk_nm                VARCHAR(200)                          NOT NULL COMMENT '블록명',
    blk_ord               INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '블록순서',
    vsblty_yn             CHAR(1)     DEFAULT 'Y'              NOT NULL COMMENT '노출여부',
    plot_on_chrt_yn       CHAR(1)     DEFAULT 'N'              NOT NULL COMMENT '차트변곡점표시여부',
    tgt_cmpny_no          INT UNSIGNED                          NULL     COMMENT '대상고객사번호(SPECIALIZED일 때)',
    data_src_type_cd      VARCHAR(30)                           NULL     COMMENT '데이터소스타입(STATIC,DYNAMIC_QUERY,CHART_SERIES,NEWS_EVENT)',
    data_src_ref           VARCHAR(500)                          NULL     COMMENT '데이터소스참조(테이블명, 차트코드 등)',

    CONSTRAINT tmd_nwsltrtmpltblk_ux01 UNIQUE (nws_ltr_tmplt_sctn_id, blk_cd),

    CONSTRAINT tmd_nwsltrtmpltblk_fk01
        FOREIGN KEY (nws_ltr_tmplt_sctn_id) REFERENCES tmd_nwsltrtmpltsctn (nws_ltr_tmplt_sctn_id),

    CONSTRAINT tmd_nwsltrtmpltblk_ck01
        CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltblk_ck02
        CHECK (blk_type_cd IN ('TEXT', 'CHART', 'TABLE', 'NEWS_CARD', 'IMAGE', 'TITLE')),
    CONSTRAINT tmd_nwsltrtmpltblk_ck03
        CHECK (blk_fmt_cd IN ('GENERAL', 'SPECIALIZED')),
    CONSTRAINT tmd_nwsltrtmpltblk_ck04
        CHECK (vsblty_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltblk_ck05
        CHECK (plot_on_chrt_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltblk_ck06
        CHECK (blk_fmt_cd <> 'SPECIALIZED' OR tgt_cmpny_no IS NOT NULL),
    CONSTRAINT tmd_nwsltrtmpltblk_ck07
        CHECK (blk_type_cd <> 'CHART' OR plot_on_chrt_yn IS NOT NULL)
)
    COMMENT 'TMD_뉴스레터템플릿블록';

CREATE INDEX tmd_nwsltrtmpltblk_ix01
    ON tmd_nwsltrtmpltblk (nws_ltr_tmplt_sctn_id, blk_ord);
```

#### Table 4: `tmd_nwsltrtmpltblkfld` (뉴스레터템플릿블록필드)

```sql
CREATE TABLE tmd_nwsltrtmpltblkfld
(
    nws_ltr_tmplt_blk_fld_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터템플릿블록필드ID'
        PRIMARY KEY,
    crt_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id          VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                   CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_tmplt_blk_id     BIGINT UNSIGNED                      NOT NULL COMMENT '뉴스레터템플릿블록ID',
    fld_key                  VARCHAR(100)                          NOT NULL COMMENT '필드키(스냅샷 content 내 JSON key)',
    fld_nm                   VARCHAR(200)                          NOT NULL COMMENT '필드명(관리용)',
    fld_ord                  INT UNSIGNED DEFAULT 0                NOT NULL COMMENT '필드순서',
    col_mapping              VARCHAR(500)                          NULL     COMMENT '컬럼매핑(DB컬럼 or 정적값 참조)',
    is_editable_yn           CHAR(1)     DEFAULT 'N'              NOT NULL COMMENT '사용자수정가능여부',
    is_required_yn           CHAR(1)     DEFAULT 'N'              NOT NULL COMMENT '필수여부',
    fld_meta                 JSON                                  NULL     COMMENT '필드메타정보(maxItems, orderBy, format 등)',

    CONSTRAINT tmd_nwsltrtmpltblkfld_ux01 UNIQUE (nws_ltr_tmplt_blk_id, fld_key),

    CONSTRAINT tmd_nwsltrtmpltblkfld_fk01
        FOREIGN KEY (nws_ltr_tmplt_blk_id) REFERENCES tmd_nwsltrtmpltblk (nws_ltr_tmplt_blk_id),

    CONSTRAINT tmd_nwsltrtmpltblkfld_ck01
        CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltblkfld_ck02
        CHECK (is_editable_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrtmpltblkfld_ck03
        CHECK (is_required_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터템플릿블록필드';

CREATE INDEX tmd_nwsltrtmpltblkfld_ix01
    ON tmd_nwsltrtmpltblkfld (nws_ltr_tmplt_blk_id, fld_ord);
```

### 1-3. 네이밍 규칙 정리

| 논리명 | 물리 테이블 | 물리 컬럼 prefix | 비고 |
|---|---|---|---|
| 뉴스레터템플릿V2기본 | `tmd_nwsltrtmpltm` | `nws_ltr_tmplt_v2_` | v1 `tmd_nws_ltr_tmplt`과 구분 |
| 뉴스레터템플릿섹션 | `tmd_nwsltrtmpltsctn` | `nws_ltr_tmplt_sctn_` | |
| 뉴스레터템플릿블록 | `tmd_nwsltrtmpltblk` | `nws_ltr_tmplt_blk_` | |
| 뉴스레터템플릿블록필드 | `tmd_nwsltrtmpltblkfld` | `nws_ltr_tmplt_blk_fld_` | |

---

## 2. 스냅샷 JSON 스키마 확정 (`org_snpsht_json`)

### 2-1. 최상위 구조

```jsonc
{
  "schemaVersion": 2,
  "templateId": 1,                    // tmd_nwsltrtmpltm.nws_ltr_tmplt_v2_id
  "newsletterId": 1,                  // nws_ltr_mstr_id
  "issueNo": 22,
  "publishedAt": "2026-03-16T09:00:00+09:00",
  "meta": {
    "title": "Oil Market Weekly #22",
    "emailSubject": "[MIP] Oil Market Weekly #22",
    "subInfoText": "보충 설명 텍스트",     // nullable
    "aiSummary": "AI가 생성한 요약",       // nullable
    "coverImageUrl": "static/images/oil_trend_cover_img.png"
  },
  "sections": [/* Section[] */]
}
```

### 2-2. Section 구조

```jsonc
{
  "id": "sec-HEADER",                   // sctn_cd에서 파생
  "type": "FIXED",                       // FIXED | GENERAL | SPECIALIZED
  "title": "뉴스레터 헤더",
  "order": 0,
  "visibility": true,
  "target": null,                        // SPECIALIZED일 때 고객사 코드 (e.g. "SKE")
  "isCollapsed": false,
  "blocks": [/* Block[] */]
}
```

### 2-3. Block 구조 (공통 필드)

```jsonc
{
  "id": "blk-MAIN_CHART",               // blk_cd에서 파생
  "type": "CHART",                       // TEXT | CHART | TABLE | NEWS_CARD | IMAGE | TITLE
  "format": "GENERAL",                   // GENERAL | SPECIALIZED
  "order": 0,
  "visibility": true,
  "isCollapsed": false,
  "title": "Brent 유가 추이",
  "plotOnChart": true,
  "target": null,
  "editableFields": ["title"],           // is_editable=Y인 field_key 목록
  "content": {/* type별 상이 */}
}
```

### 2-4. Block.content — 타입별 구조

#### TEXT

```jsonc
{
  "text": "본문 텍스트 내용..."
}
```

- `editableFields`: `["text"]` → 사용자가 텍스트 수정 가능

#### TITLE

```jsonc
{
  "text": "Oil Market Weekly",
  "publishDate": "2026년 03월 16일",
  "issueLabel": "#22"
}
```

#### CHART (OHLCV 시계열)

```jsonc
{
  "chartType": "candlestick",           // line | bar | candlestick
  "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지",
  "timestamps": ["1710547200000", "..."],
  "series": [
    {
      "key": "high",
      "label": "High",
      "renderAs": "HIGH",
      "data": [92.5, 93.1, "..."],
      "visible": true
    },
    {
      "key": "close",
      "label": "Close",
      "renderAs": "CLOSE",
      "data": [91.2, 92.0, "..."],
      "visible": true,
      "chartOptions": {
        "dtick": "1",
        "range": "[89.0, 95.0]",
        "isBidirectional": false
      }
    }
    // ... open, low, volume
  ]
}
```

- Full Resolve: 5시리즈 × ~253포인트 모두 포함 (확정사항 ①)
- `editableFields`: 일반적으로 빈 배열 (차트 데이터 수정 불가)

#### TABLE (동적 쿼리 결과)

```jsonc
{
  "description": "",
  "columns": [
    { "field": "indicator", "name": "지표명" },
    { "field": "value", "name": "값" },
    { "field": "change", "name": "변동" }
  ],
  "rows": [
    { "cells": ["WTI", "78.50", "+1.2"] },
    { "cells": ["Brent", "82.30", "+0.8"] }
  ]
}
```

- `editableFields`: `["rows"]` → 사용자가 행 데이터 수정 가능 (정책에 따라)

#### NEWS_CARD (뉴스 이벤트 목록)

```jsonc
{
  "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지",
  "items": [
    {
      "newsId": "12345",
      "title": "OPEC+ 감산 합의",
      "content": ["감산 규모 200만 배럴...", "2분기 적용 예정"],
      "timestamp": 1710547200000,
      "createdDate": "2026-03-15",
      "markTime": "14:30",
      "source": "Reuters",
      "isTurningPoint": true,
      "directionalityType": "BULLISH"
    }
  ]
}
```

- `editableFields`: `["items.*.title", "items.*.content"]` → 뉴스 제목/본문 수정 가능

#### IMAGE

```jsonc
{
  "url": "https://storage.example.com/newsletter/chart-capture-22.png",
  "alt": "차트 캡처 이미지",
  "width": 800,
  "height": 600
}
```

### 2-5. editableFields 설계 원칙 (확정사항 ③)

- 각 Block의 `editableFields` 배열에 `is_editable=Y`인 BlockField의 `fld_key`를 나열
- 발행 시점의 수정 정책이 스냅샷에 동결됨
- 프론트엔드는 `editableFields`만 보고 수정 가능 범위를 판단 (별도 API 불필요)
- 경로 표기:
  - 단순 필드: `"text"`, `"title"`
  - 중첩 필드: `"items.*.title"` (배열 내 모든 항목의 title)

### 2-6. 템플릿 BlockField → 스냅샷 content 변환 규칙

| BlockField.fld_key | BlockField.col_mapping | 변환 규칙 |
|---|---|---|
| `text` | `tmd_nws_ltr_txt.nws_ltr_txt_val WHERE nws_ltr_txt_type='SUB_INFO_TXT'` | 단일 값 조회 → `content.text` |
| `title` | `STATIC:${block.blk_nm}` | 블록명 그대로 사용 |
| `timestamps` | `tif_nws_ltr_chrt.tmstmp WHERE code='${data_src_ref}'` | 차트 시계열 timestamp 배열 |
| `series.high` | `tif_nws_ltr_chrt.high_1` | 시리즈별 데이터 배열 |
| `columns` | `DYNAMIC_QUERY:header` | 동적 쿼리 결과의 header 파싱 |
| `rows` | `DYNAMIC_QUERY:detail` | 동적 쿼리 결과의 detail 파싱 |
| `items` | `tmd_nws_ltr_evnt.*` | 뉴스 이벤트 목록 조회 |

**`col_mapping` prefix 규칙:**
- `STATIC:` — 정적 값 (템플릿 정의에서 직접 가져옴)
- `DYNAMIC_QUERY:` — MybatisDynamicQueryService를 통한 동적 쿼리
- 그 외 — 직접 테이블.컬럼 참조 (Repository 호출)

---

## 3. 발행 파이프라인 설계

### 3-1. 전체 흐름

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Trigger      │────▶│ TemplateLoader   │────▶│ BlockDataResolver│────▶│SnapshotAssembler│
│ (Airflow/API) │     │                  │     │                  │     │                 │
└──────────────┘     └──────────────────┘     └──────────────────┘     └────────┬────────┘
                                                                                 │
                                                                                 ▼
                                                                      ┌─────────────────┐
                                                                      │SnapshotPersister │
                                                                      │(tmd_nwsltrorgissm│
                                                                      │ org_snpsht_json) │
                                                                      └─────────────────┘
```

### 3-2. 컴포넌트 상세

#### A. NewsletterPublishPipelineService (오케스트레이터)

```java
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterPublishPipelineService {

    private final TemplateLoader templateLoader;
    private final BlockDataResolver blockDataResolver;
    private final SnapshotAssembler snapshotAssembler;
    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterPublishHistoryRepository publishHistoryRepository;

    /**
     * 발행 파이프라인 실행.
     * Airflow 또는 Admin API에서 호출.
     *
     * @param newsletterMasterId 뉴스레터 마스터 ID
     * @param issueNo            발행 차수
     * @param publishHistoryId   원본 발행이력 ID (tmd_nws_ltr_pub_hist.nws_ltr_pub_id)
     */
    public void executePublish(Integer newsletterMasterId, Integer issueNo, Integer publishHistoryId) {

        // 1. 발행이력 조회
        NewsletterPublishHistory pubHist = publishHistoryRepository
            .findById(publishHistoryId)
            .orElseThrow(() -> new AppErrorException(ResultCode.Error.NEWSLETTER_NOT_FOUND));

        // 2. 템플릿 로드 (4테이블 트리)
        TemplateTree template = templateLoader.load(newsletterMasterId);

        // 3. 블록별 데이터 resolve
        Map<String, ResolvedBlockData> resolvedData =
            blockDataResolver.resolveAll(template, pubHist);

        // 4. 스냅샷 JSON 조합
        String snapshotJson = snapshotAssembler.assemble(template, resolvedData, pubHist);

        // 5. 원본 이슈 저장
        NewsletterIssueOriginal issueOriginal = NewsletterIssueOriginal.builder()
            .newsletterMasterId(newsletterMasterId)
            .issueNo(issueNo)
            .sourcePublishHistoryId(publishHistoryId)
            .publishStatusCode("PUBLISHED")
            .publishDateTime(pubHist.getPublishDateTime())
            .originalSnapshotJson(snapshotJson)
            .schemaVersion(2)
            .build();

        issueOriginalRepository.save(issueOriginal);

        log.info("뉴스레터 발행 스냅샷 생성 완료 - masterId: {}, issueNo: {}", newsletterMasterId, issueNo);
    }
}
```

#### B. TemplateLoader

```java
@Service
@RequiredArgsConstructor
public class TemplateLoader {

    private final NewsletterTemplateV2Repository templateRepository;
    private final NewsletterTemplateSectionRepository sectionRepository;
    private final NewsletterTemplateBlockRepository blockRepository;
    private final NewsletterTemplateBlockFieldRepository blockFieldRepository;

    /**
     * 뉴스레터 마스터 ID로 4테이블 트리를 한번에 로드.
     */
    public TemplateTree load(Integer newsletterMasterId) {
        // 1. Template
        NewsletterTemplateV2 template = templateRepository
            .findByNewsletterMasterIdAndDelYn(newsletterMasterId, "N")
            .orElseThrow(() -> new AppErrorException(ResultCode.Error.NEWSLETTER_NOT_FOUND));

        // 2. Sections (order by sctn_ord)
        List<NewsletterTemplateSection> sections = sectionRepository
            .findByTemplateIdAndDelYnOrderBySctnOrd(template.getId(), "N");

        // 3. Blocks per section
        List<Long> sectionIds = sections.stream()
            .map(NewsletterTemplateSection::getId).toList();
        List<NewsletterTemplateBlock> blocks = blockRepository
            .findBySectionIdInAndDelYnOrderByBlkOrd(sectionIds, "N");

        // 4. Fields per block
        List<Long> blockIds = blocks.stream()
            .map(NewsletterTemplateBlock::getId).toList();
        List<NewsletterTemplateBlockField> fields = blockFieldRepository
            .findByBlockIdInAndDelYnOrderByFldOrd(blockIds, "N");

        return TemplateTree.of(template, sections, blocks, fields);
    }
}
```

**`TemplateTree`**: Section → Block → BlockField 트리를 메모리에 보관하는 VO.

#### C. BlockDataResolver

블록의 `data_src_type_cd`에 따라 데이터를 조회하는 Strategy 패턴.

```java
@Service
@RequiredArgsConstructor
public class BlockDataResolver {

    private final Map<String, BlockDataStrategy> strategies;

    public Map<String, ResolvedBlockData> resolveAll(
            TemplateTree template, NewsletterPublishHistory pubHist) {

        Map<String, ResolvedBlockData> result = new LinkedHashMap<>();

        for (TemplateTree.SectionNode section : template.getSections()) {
            for (TemplateTree.BlockNode block : section.getBlocks()) {
                String strategyKey = block.getDataSourceType(); // STATIC, DYNAMIC_QUERY, CHART_SERIES, NEWS_EVENT
                BlockDataStrategy strategy = strategies.get(strategyKey);
                if (strategy == null) {
                    strategy = strategies.get("STATIC"); // fallback
                }
                ResolvedBlockData data = strategy.resolve(block, pubHist);
                result.put(block.getBlockCode(), data);
            }
        }

        return result;
    }
}
```

**Strategy 구현체:**

| data_src_type_cd | Strategy | v1 대응 로직 |
|---|---|---|
| `STATIC` | `StaticBlockDataStrategy` | 템플릿 정의값 그대로 사용 (TITLE, 고정 TEXT) |
| `DYNAMIC_QUERY` | `DynamicQueryBlockDataStrategy` | `MybatisDynamicQueryService` 호출 → TABLE content 생성. v1의 `createTableReference()` 대체 |
| `CHART_SERIES` | `ChartSeriesBlockDataStrategy` | `NewsletterChartRepository` / `NewsletterTemplateDedicatedChartRepository` / `NewsletterCollectionChartRepository` 조회 → CHART content 생성. v1의 `convertNewsletterChartsToChartData()`, `getDediChartData()`, `getCollectionChartData()` 대체 |
| `NEWS_EVENT` | `NewsEventBlockDataStrategy` | `NewsletterEventRepository` 조회 → NEWS_CARD content 생성. v1의 뉴스 이벤트 매핑 로직 대체 |

#### D. SnapshotAssembler

```java
@Service
@RequiredArgsConstructor
public class SnapshotAssembler {

    private final ObjectMapper objectMapper;

    /**
     * 템플릿 트리 + resolve된 데이터 → org_snpsht_json 문자열 생성.
     */
    public String assemble(
            TemplateTree template,
            Map<String, ResolvedBlockData> resolvedData,
            NewsletterPublishHistory pubHist) {

        SnapshotRoot root = new SnapshotRoot();
        root.setSchemaVersion(2);
        root.setTemplateId(template.getTemplate().getId());
        root.setNewsletterId(pubHist.getNewsletterMaster().getNewsletterMasterId());
        root.setIssueNo(pubHist.getIssueNo());
        root.setPublishedAt(pubHist.getPublishDateTime());

        // meta
        SnapshotMeta meta = new SnapshotMeta();
        meta.setTitle(pubHist.getNewsletterTitle());
        meta.setEmailSubject(pubHist.getEmailSubject());
        meta.setAiSummary(pubHist.getAiSummary());
        root.setMeta(meta);

        // sections
        List<SnapshotSection> sections = new ArrayList<>();
        for (TemplateTree.SectionNode sectionNode : template.getSections()) {
            SnapshotSection section = new SnapshotSection();
            section.setId("sec-" + sectionNode.getSectionCode());
            section.setType(sectionNode.getSectionTypeCode());
            section.setTitle(sectionNode.getSectionName());
            section.setOrder(sectionNode.getSectionOrder());
            section.setVisibility("Y".equals(sectionNode.getVisibilityYn()));
            section.setTarget(resolveTarget(sectionNode.getTargetCompanyNo()));
            section.setIsCollapsed(false);

            List<SnapshotBlock> blocks = new ArrayList<>();
            for (TemplateTree.BlockNode blockNode : sectionNode.getBlocks()) {
                SnapshotBlock block = new SnapshotBlock();
                block.setId("blk-" + blockNode.getBlockCode());
                block.setType(blockNode.getBlockTypeCode());
                block.setFormat(blockNode.getBlockFormatCode());
                block.setOrder(blockNode.getBlockOrder());
                block.setVisibility("Y".equals(blockNode.getVisibilityYn()));
                block.setIsCollapsed(false);
                block.setTitle(blockNode.getBlockName());
                block.setPlotOnChart("Y".equals(blockNode.getPlotOnChartYn()));
                block.setTarget(resolveTarget(blockNode.getTargetCompanyNo()));

                // editableFields: is_editable_yn='Y'인 필드키 목록
                List<String> editableFields = blockNode.getFields().stream()
                    .filter(f -> "Y".equals(f.getIsEditableYn()))
                    .map(TemplateTree.FieldNode::getFieldKey)
                    .toList();
                block.setEditableFields(editableFields);

                // content: resolved data
                ResolvedBlockData data = resolvedData.get(blockNode.getBlockCode());
                block.setContent(data != null ? data.toContentMap() : Map.of());

                blocks.add(block);
            }
            section.setBlocks(blocks);
            sections.add(section);
        }
        root.setSections(sections);

        return objectMapper.writeValueAsString(root);
    }
}
```

### 3-3. v1 `getNewsletterDetail()` 하드코딩 대체 매핑

| v1 코드 영역 (MiNewsletterService) | v2 대체 | 비고 |
|---|---|---|
| Lines 328-339: templatesByOrder 맵 구축 | `TemplateLoader.load()` | 4테이블 트리로 대체 |
| Lines 341-346: mainChartTemplate → MAIN_CHRT_NM/CODE 추출 | `CHART_SERIES` strategy | Block의 data_src_ref에 차트 코드 저장 |
| Lines 347-364: flowChart 조회 + ChartData 변환 | `ChartSeriesBlockDataStrategy.resolve()` | |
| Lines 366-386: referenceList → Reference 생성 분기 | `BlockDataResolver.resolveAll()` | 블록별 strategy로 자동 분기 |
| Lines 389-397: SUB_INFO_TXT 조회 | `StaticBlockDataStrategy` 또는 `TemplateLoader` 시 포함 | meta.subInfoText로 이동 |
| Lines 400-417: NewsEvent → NewsEventItem 매핑 | `NewsEventBlockDataStrategy.resolve()` | |
| Lines 419-440: IssueDetail DTO 조합 | `SnapshotAssembler.assemble()` | JSON으로 직접 저장 |
| Lines 678-719: createTableReference (동적쿼리) | `DynamicQueryBlockDataStrategy.resolve()` | |
| Lines 721-759: createDediChartReference / createCltnChartReference | `ChartSeriesBlockDataStrategy.resolve()` | data_src_ref로 차트 종류 구분 |

---

## 4. v1 → v2 마이그레이션 매핑

### 4-1. elementType → Section/Block 매핑표

#### Section 매핑

| v2 Section | sctn_cd | sctn_type_cd | 구성 |
|---|---|---|---|
| 헤더/타이틀 | `HEADER` | `FIXED` | 제목, 일자, 이슈번호 |
| 메인 차트 | `MAIN_CHART` | `FIXED` | 뉴스 플로우 차트 |
| 관련 뉴스 | `NEWS_EVENTS` | `FIXED` | 뉴스 카드 목록 |
| 참조 영역 #N | `REF_{order}` | `GENERAL` | 테이블/차트 참조 (elementOrder 2+에서 1:1) |
| AI 요약 | `AI_SUMMARY` | `FIXED` | AI 요약 텍스트 |
| 보충 정보 | `SUB_INFO` | `GENERAL` | SUB_INFO_TXT |

#### Block 매핑 (elementType → Block)

| v1 elementType | v2 Block type | v2 Block blk_cd | data_src_type_cd | data_src_ref |
|---|---|---|---|---|
| `MAIN_CHRT_NM` + `MAIN_CHRT_CODE` | `CHART` | `MAIN_FLOW_CHART` | `CHART_SERIES` | `{MAIN_CHRT_CODE value}` |
| (뉴스 이벤트 - 하드코딩) | `NEWS_CARD` | `NEWS_EVENT_LIST` | `NEWS_EVENT` | — |
| `RERNC_TABLE_NM` + `RERNC_TABLE_CD` | `TABLE` | `REF_TABLE_{order}` | `DYNAMIC_QUERY` | `pref_table:{RERNC_TABLE_CD value}` |
| `DEDI_CHRT_NM` + `DEDI_CHRT_CD` | `CHART` | `REF_DEDI_CHART_{order}` | `CHART_SERIES` | `dedi:{DEDI_CHRT_CD value}` |
| `CLTN_CHRT_NM` + `CLTN_CHRT_CD` | `CHART` | `REF_CLTN_CHART_{order}` | `CHART_SERIES` | `cltn:{CLTN_CHRT_CD value}` |
| `SUB_INFO_TXT` | `TEXT` | `SUB_INFO_TEXT` | `STATIC` | — |
| (타이틀 - 하드코딩) | `TITLE` | `NEWSLETTER_TITLE` | `STATIC` | — |
| (AI 요약 - 하드코딩) | `TEXT` | `AI_SUMMARY_TEXT` | `STATIC` | — |

### 4-2. `tmd_nws_ltr_tmplt` → v2 4테이블 변환 로직

마이그레이션 SQL/스크립트 의사코드:

```
FOR EACH DISTINCT nws_ltr_tmplt_id IN tmd_nws_ltr_tmplt:

  1. nws_ltr_mstr_id 확정
     - tmd_nws_ltr_pub_hist에서 해당 tmplt_id를 사용하는 마스터 조회

  2. tmd_nwsltrtmpltm INSERT
     - tmplt_nm = "{마스터명} 기본 템플릿"

  3. FIXED 섹션 생성 (HEADER, MAIN_CHART, NEWS_EVENTS, AI_SUMMARY)
     - 이 섹션들은 v1에서 하드코딩된 영역

  4. FOR EACH elementOrder IN tmd_nws_ltr_tmplt WHERE order >= 2:
     - 해당 order의 elementType 그룹 확인 (RERNC_TABLE / DEDI_CHRT / CLTN_CHRT)
     - tmd_nwsltrtmpltsctn INSERT (GENERAL 섹션, REF_{order})
     - tmd_nwsltrtmpltblk INSERT (TABLE 또는 CHART)
     - tmd_nwsltrtmpltblkfld INSERT (필드 목록)
       - TABLE: columns, rows, description 필드
       - CHART: timestamps, series.* 필드

  5. SUB_INFO_TXT 존재 시:
     - SUB_INFO 섹션 + TEXT 블록 생성

  6. 블록필드 is_editable_yn 기본값:
     - TEXT 블록의 text 필드 → 'Y'
     - TITLE 블록의 title 필드 → 'Y'
     - NEWS_CARD 블록의 items.*.title, items.*.content → 'Y'
     - CHART, TABLE 데이터 필드 → 'N'
```

### 4-3. 기존 발행분 org_snpsht_json 백필 전략

현재 `tmd_nwsltrorgissm` 백필은 **최소 플레이스홀더 스냅샷** (schm_ver=1)으로 완료됨:

```json
{
  "source": "tmd_nws_ltr_pub_hist",
  "newsletterPublishId": 123,
  "newsletterTitle": "...",
  "emailSubject": "..."
}
```

**Full Resolve 백필 전략:**

| 단계 | 작업 | 기준 |
|---|---|---|
| Phase A (현행) | 최소 플레이스홀더 (완료) | schm_ver=1 |
| Phase B | v2 파이프라인 가동 후 신규 발행 | schm_ver=2 |
| Phase C (선택) | 과거 발행분 Full Resolve 백필 | 배치 스크립트로 publishHistoryId 기반 재실행 |

**Phase C 세부:**
- 별도 배치 Job으로 `schm_ver=1`인 레코드를 순회
- `NewsletterPublishPipelineService.executePublish()`와 동일한 로직을 배치에서 재실행
- 단, v1 `tmd_nws_ltr_tmplt` → v2 4테이블 변환이 선행되어야 함
- 백필 대상 범위: 최근 N개월(운영 판단) 또는 전체

**Phase C 리스크:**
- 과거 차트 데이터(`tif_nws_ltr_chrt`)가 삭제/만료되었을 수 있음 → 차트 데이터 누락 허용 or SKIP
- 동적 쿼리 대상 테이블 스키마가 변경되었을 수 있음 → 오류 핸들링 필요
- 권장: Phase C는 운영 안정화 이후 선택적 실행

---

## 5. 발행 시 `nws_ltr_tmplt_id` 매핑

`tmd_nwsltrorgissm`에 `nws_ltr_tmplt_v2_id` 컬럼 추가는 불필요.

- **근거 (확정사항 ⑥)**: 별도 이력 관리 없음. `org_snpsht_json` 내 `templateId`로 발행 시점 템플릿을 역추적 가능.
- 발행 시점의 전체 구조가 JSON에 동결되므로, 템플릿 PK 참조만으로 충분.

---

## 6. 구현 단계 제안 (템플릿/파이프라인 한정)

| 단계 | 작업 | 산출물 |
|---|---|---|
| Step 1 | DDL 4테이블 생성 + JPA Entity | 엔티티 4개, Repository 4개 |
| Step 2 | TemplateLoader 구현 + TemplateTree VO | 단위 테스트 |
| Step 3 | BlockDataStrategy 4개 구현 | Strategy별 단위 테스트 |
| Step 4 | SnapshotAssembler 구현 | 스냅샷 JSON 생성 통합 테스트 |
| Step 5 | NewsletterPublishPipelineService 오케스트레이터 | E2E 테스트 |
| Step 6 | v1 템플릿 데이터 → v2 4테이블 마이그레이션 스크립트 | SQL 스크립트 |
| Step 7 | v2 상세조회 API (org_snpsht_json 기반) | 기존 getNewsletterDetail deprecate |
| Step 8 | Phase C 백필 배치 (선택) | 배치 Job |

---

## 변경 이력

- 2026-03-16: 초기 버전 작성
