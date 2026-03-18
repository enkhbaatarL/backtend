# Newsletter v2 Issue State / Snapshot 설계 정리

## 문서 목적
- 본 문서는 Newsletter v2에서 `template`, `issue state`, `resolved content`, `original snapshot`의 역할을 분리한 설계 기준을 정리한다.
- 대상 독자: 백엔드, 프론트엔드, 기획.
- 본 문서는 구현 전 설계 정리 문서이며, API/DDL 계약 원본을 직접 대체하지 않는다.

## Contract Sync Checklist
- Living Doc: `docs/newsletter-v2-design.md`
- Template 구조 참조: `docs/newsletter-v2-template-pipeline-design.md`
- Canonical DDL: `docs/newsletter-v2-ddl-canonical-index.md`
- Last synced at (KST): `2026-03-16 19:10`
- Sync owner: `BE`

## 0. 용어 정리
- `template`: 스키마/정책
- `issue section state / issue block state`: 발행 이슈별 상태값
- `resolved content`: template의 query policy와 issue state를 반영해 조회/가공한 실제 데이터
- `original snapshot`: 발행 완료 시점에 동결한 원본 완성본
- `rendered block instance row`: `tmd_nwsltrorgissblkstat`의 개별 row. preview/publish 조립 시 실제 렌더링 block 1건으로 해석한다.

## 0-1. Source of Truth / 책임 경계
- 본 문서는 역할 분리와 책임 경계를 정의하는 설계 기준 문서다.
- 물리 스키마의 최종 원본은 canonical DDL 문서군이다.
- `original snapshot`은 published original의 유일한 조회 원본이다.
- `issue state`는 preview 및 publish 직전 snapshot 생성 입력값이며, published 이후 원본 조회의 원천이 아니다.
- `template`은 구조와 지원 가능 옵션을 정의하고, instance-level 최종 상태값은 issue state가 가진다.

## 0-2. 고정 결정 사항
- original detail은 항상 `org_snpsht_json` 우선으로 조회한다.
- preview는 `template + issue state + resolved content`를 동적으로 조합한다.
- `plotOnChart`는 template에서 "이 block이 해당 옵션 구조를 지원하는가"를 뜻하고, issue block state에서 최종 true/false 값을 가진다.
- `plotOnChart`를 차트 연계 표시용 canonical state key로 사용한다.
  - 과거 문서/예시의 `showInChart`는 `plotOnChart`로 통일한다.
- field-level state 테이블은 도입하지 않는다.
  - 이유: 현재 요구사항의 가변값은 section/block 단위이며, field는 주로 content schema 또는 editable field 정의에 가깝다.
  - 예외: 향후 field별 order/visibility/override 요구가 독립적으로 생길 때만 별도 검토한다.
- 반복 block는 별도 `blk_inst_no` 없이 지원한다.
  - 같은 template block을 참조하는 복수 `issue block state` row를 허용한다.
  - 각 row는 `nws_ltr_org_iss_blk_stat_id`와 section 내 `blk_ord`로 식별/정렬한다.

## 0-3. DB 약어 판단
- `sctn`: 기존 `tmd_nwsltrtmpltsctn` 등에서 이미 사용 중이므로 유지 가능
- `blk`: 기존 `tmd_nwsltrtmpltblk` 등에서 이미 사용 중이므로 유지 가능
- `iss`: 기존 `tmd_nwsltrorgissm`, `issno` 등에서 이미 사용 중이므로 유지 가능
- `stt`: 승인하지 않음
  - 근거: 상태 약어는 용어사전에 `STAT`로 등록되어 있고, 기존 상태 컬럼도 `pub_stat_cd`, `rvsn_stat_cd`를 사용한다.
  - 따라서 신규 state 테이블/컬럼은 `stt` 대신 `stat`를 사용한다.

## 1. 발행 파이프라인에서 `state + content + template -> original snapshot` 생성 규칙

### 1-1. 기본 원칙
- `template`은 구조와 정책을 정의한다.
- `issue state`는 매 발행본마다 달라질 수 있는 값을 정의한다.
- `resolved content`는 실제 렌더링 데이터다.
- `original snapshot`은 위 3개를 합성한 발행 완료 시점의 동결본이다.

### 1-2. 생성 순서
1. `template` 로드
2. 해당 `issue`의 `section state`, `block state` 로드
3. 각 block의 `query policy`와 `issue state`를 이용해 `resolved content` 생성
4. `template + issue state + resolved content`를 합성해 `original snapshot` 생성
5. 발행 완료 시 `original snapshot` 저장

### 1-3. 합성 규칙
- Section
  - `id`, `type`, 기본 라벨, 기본 순서: template 기준
  - `order`, `visibility`, title override, 기타 상태: issue section state 기준
- Block
  - `id`, `type`, `format`, field schema, data source 정의: template 기준
  - `order`, `visibility`, title override, `plotOnChart` 등 옵션 값: issue block state 기준
  - template은 block option의 지원 가능 여부만 정의하며, published snapshot에는 issue block state에서 계산된 최종값을 기록한다.
  - `content`: resolved content 기준
  - 반복 block는 template block 1건에 대해 issue block state row가 여러 건 생성될 수 있으며, snapshot block도 row 수만큼 생성한다.
- 값 우선순위
  - structure/policy는 template 기준, instance state 값은 issue state 기준

### 1-4. 발행 완료 규칙
- `original snapshot`은 실제 발행 완료본과 동일해야 한다.
- 발행 이후 template, state 원천, source data가 바뀌어도 과거 발행본은 snapshot 기준으로 재현한다.

## 2. 원본 조회 정책

### 2-1. 권장 정책
- 원본 상세 조회: `snapshot 우선`
- preview: `동적 생성`

### 2-2. 이유
- 원본 상세 조회는 "실제 발행 완료본"을 보여줘야 한다.
- preview는 발행 전 상태를 검토하기 위한 용도이므로 `template + issue state + resolved content`를 동적으로 조합해도 된다.

### 2-3. 정책 분리
- `Original Published View`
  - 데이터 원천: `original snapshot`
  - 목적: 발행 이력 재현, 감사, 비교, 메일 재현
- `Issue Preview View`
  - 데이터 원천: `template + issue state + resolved content`
  - 목적: 발행 전 확인, 운영 미리보기, 검수

### 2-4. 구현 가드
- original detail API는 state 테이블을 fallback source로 사용하지 않는다.
- preview/build path만 state 테이블을 읽는다.
- 과거 published issue의 state row 유무와 무관하게 original detail은 snapshot만으로 재현 가능해야 한다.

## 3. Issue State ERD 초안

### 3-1. 설계 원칙
- section/block 정체성과 관계는 정규화한다.
- 자주 조회/정렬/검증되는 핵심 상태는 컬럼으로 둔다.
- 확장성이 필요한 나머지 상태는 `*_stat_json`에 둔다.
- field는 기본적으로 별도 state 테이블을 두지 않는다.
  - field는 대부분 content schema 정의에 가깝기 때문이다.
- block state는 반드시 동일 `issue`의 section state에만 연결되어야 한다.
- issue block state row 자체를 rendered block instance row로 해석한다.
- 같은 template block을 참조하는 복수 row를 허용한다.
- 신규 state 테이블은 null-safe rollout을 위해 기존 read path를 깨지 않는 독립 추가 테이블로 도입한다.

### 3-2. 관계 개요
```text
tmd_nwsltrorgissm (Original Issue)
 └── tmd_nwsltrorgisssctnstat (Issue Section State)
      └── tmd_nwsltrorgissblkstat (Issue Block State)
```

### 3-3. 테이블 초안: `tmd_nwsltrorgisssctnstat`
```sql
CREATE TABLE tmd_nwsltrorgisssctnstat
(
    nws_ltr_org_iss_sctn_stat_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈섹션상태ID'
        PRIMARY KEY,
    crt_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                      CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_org_iss_id          BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터원본이슈ID',
    nws_ltr_tmplt_sctn_id       BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터템플릿섹션ID',
    sctn_ord                    INT UNSIGNED                           NOT NULL COMMENT '섹션순서',
    vsblty_yn                   CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    sctn_titl                   VARCHAR(200)                           NULL COMMENT '섹션제목오버라이드',
    tgt_cmpny_no                INT UNSIGNED                           NULL COMMENT '대상고객사번호',
    sctn_stat_json              JSON                                   NULL COMMENT '섹션상태JSON',

    CONSTRAINT tmd_nwsltrorgisssctnstat_ux01 UNIQUE (nws_ltr_org_iss_id, nws_ltr_tmplt_sctn_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ux02 UNIQUE (nws_ltr_org_iss_id, sctn_ord),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ux03 UNIQUE (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_fk01 FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_fk02 FOREIGN KEY (nws_ltr_tmplt_sctn_id) REFERENCES tmd_nwsltrtmpltsctn (nws_ltr_tmplt_sctn_id),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgisssctnstat_ck02 CHECK (vsblty_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터원본이슈섹션상태';

CREATE INDEX tmd_nwsltrorgisssctnstat_ix01
    ON tmd_nwsltrorgisssctnstat (nws_ltr_org_iss_id, sctn_ord, del_yn);
```

### 3-4. 테이블 초안: `tmd_nwsltrorgissblkstat`
```sql
CREATE TABLE tmd_nwsltrorgissblkstat
(
    nws_ltr_org_iss_blk_stat_id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '뉴스레터원본이슈블록상태ID'
        PRIMARY KEY,
    crt_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id             VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm                     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn                      CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',

    nws_ltr_org_iss_id          BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터원본이슈ID',
    nws_ltr_org_iss_sctn_stat_id BIGINT UNSIGNED                       NOT NULL COMMENT '뉴스레터원본이슈섹션상태ID',
    nws_ltr_tmplt_blk_id        BIGINT UNSIGNED                        NOT NULL COMMENT '뉴스레터템플릿블록ID',
    blk_ord                     INT UNSIGNED                           NOT NULL COMMENT '블록순서',
    vsblty_yn                   CHAR(1)     DEFAULT 'Y'               NOT NULL COMMENT '노출여부',
    blk_titl                    VARCHAR(200)                           NULL COMMENT '블록제목오버라이드',
    tgt_cmpny_no                INT UNSIGNED                           NULL COMMENT '대상고객사번호',
    blk_stat_json               JSON                                   NULL COMMENT '블록상태JSON',

    CONSTRAINT tmd_nwsltrorgissblkstat_ux01 UNIQUE (nws_ltr_org_iss_sctn_stat_id, blk_ord),
    CONSTRAINT tmd_nwsltrorgissblkstat_fk01 FOREIGN KEY (nws_ltr_org_iss_id) REFERENCES tmd_nwsltrorgissm (nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgissblkstat_fk02 FOREIGN KEY (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id)
        REFERENCES tmd_nwsltrorgisssctnstat (nws_ltr_org_iss_sctn_stat_id, nws_ltr_org_iss_id),
    CONSTRAINT tmd_nwsltrorgissblkstat_fk03 FOREIGN KEY (nws_ltr_tmplt_blk_id) REFERENCES tmd_nwsltrtmpltblk (nws_ltr_tmplt_blk_id),
    CONSTRAINT tmd_nwsltrorgissblkstat_ck01 CHECK (del_yn IN ('N', 'Y')),
    CONSTRAINT tmd_nwsltrorgissblkstat_ck02 CHECK (vsblty_yn IN ('N', 'Y'))
)
    COMMENT 'TMD_뉴스레터원본이슈블록상태';

CREATE INDEX tmd_nwsltrorgissblkstat_ix01
    ON tmd_nwsltrorgissblkstat (nws_ltr_org_iss_sctn_stat_id, blk_ord, del_yn);
```

### 3-5. `*_stat_json` 사용 기준
- `sctn_stat_json`
  - `collapsed`
  - `showDivider`
  - `removed`
  - 기타 섹션 전용 UI/표시 상태
- `blk_stat_json`
  - `highlightMode`
  - `collapsed`
  - `removed`
  - 기간/시장/기준시점 등 block query에 필요한 state 파라미터
  - `contentInput`: STATIC 또는 사용자 입력 기반 block의 편집 입력값 묶음

### 3-6. 컬럼 승격 기준
- 다음 조건에 해당하는 값은 `*_stat_json`에만 두지 않고 컬럼 승격을 검토한다.
  - 반복 조회 조건으로 사용됨
  - 정렬 기준으로 사용됨
  - DB 레벨 유효성 검증이 필요함
  - 운영/감사 기준으로 직접 확인해야 함
- 현재 컬럼 유지:
  - `sctn_ord`, `blk_ord`, `vsblty_yn`, `sctn_titl`, `blk_titl`, `tgt_cmpny_no`
- 현재 JSON 유지:
  - `collapsed`, `showDivider`, `highlightMode`, `removed`
  - `plotOnChart`, `fromDate`, `toDate`, `market`, `maxItems`
  - `contentInput`
- 확정 사항:
  - `removed`, `plotOnChart`, `fromDate`, `toDate`, `market`, `maxItems`, `contentInput`은 현재 설계 범위에서 JSON 유지
  - 이유: preview/publish 조립 입력값으로 사용되며 현재 DB 정렬/검색/감사 인덱스 요구가 확정되지 않았기 때문

### 3-7. 무결성 보장 범위
- DB 레벨에서 보장하는 것
  - issue -> section state
  - issue + section state -> block state
  - section 내 `blk_ord` 단일성
- 애플리케이션 레벨에서 추가 검증할 것
  - `nws_ltr_tmplt_blk_id`가 참조하는 block이 실제로 해당 `nws_ltr_org_iss_sctn_stat_id`의 template section 하위 block인지 검증
  - 같은 template block을 참조하는 복수 row가 있어도 각 row가 독립 rendered block instance로 취급되는지 검증
  - `plotOnChart` 같은 state key는 template이 해당 옵션 구조를 지원하는 block에서만 허용되도록 검증
  - `contentInput`은 block field 정의와 호환되는 입력 key만 허용되도록 검증
  - template default와 issue override 조합 결과가 block type 제약을 위반하지 않는지 검증

## 4. Template Query Policy / Issue State / Resolved Content 예시 스키마

### 4-1. Template Query Policy
- 목적: "어떤 데이터를 어떤 파라미터로 조회할지"를 정의
- 주의: raw DB/table/column/SQL을 그대로 저장하지 않는다.
- 저장 위치 예시
  - `tmd_nwsltrtmpltblk.data_src_type_cd`
  - `tmd_nwsltrtmpltblk.data_src_ref`
  - 필요 시 `blk_stat_json`과 별개로 template query policy JSON 확장

예시:
```json
{
  "dataSourceType": "NEWS_EVENT",
  "dataSourceRef": "NEWS_EVENT_BY_PERIOD",
  "requiredQueryParams": ["fromDate", "toDate", "market"],
  "contentMapping": {
    "items": "collection"
  }
}
```

### 4-2. Issue State
- 목적: 이번 issue에 적용되는 가변 값
- 저장 위치
  - 섹션 관련 값: `sctn_stat_json` 또는 section state 컬럼
  - 블록 관련 값: `blk_stat_json` 또는 block state 컬럼
- 주의
  - published original의 원본 조회는 issue state를 직접 읽지 않는다.
  - issue state는 publish 전 조립 입력값이며, publish 완료 후에는 snapshot으로 책임이 이동한다.
  - 렌더링 최종 content 그 자체를 직접 저장하는 것이 아니라, resolver가 content를 만들기 위한 입력값을 저장한다.
  - STATIC/사용자편집 block의 입력값은 `blk_stat_json.contentInput` 아래에 둔다.

예시:
```json
{
  "fromDate": "2026-03-01",
  "toDate": "2026-03-16",
  "market": "OIL",
  "plotOnChart": true,
  "contentInput": {
    "title": "Oil Price Trends",
    "subtitle": "국제 유가 흐름 요약"
  }
}
```

### 4-3. Resolved Content
- 목적: 실제 렌더링할 최종 데이터
- 생성 방식: `query policy + issue state`를 바탕으로 resolver가 생성
- 구분 원칙
  - `issue state`: 조회 파라미터, UI 상태, 사용자 입력값(`contentInput`)
  - `resolved content`: 위 입력값을 해석/조회해 renderer가 바로 사용할 수 있게 만든 결과

예시:
```json
{
  "items": [
    {
      "title": "WTI 급락",
      "content": "중동 이슈 영향",
      "createdDate": "2026-03-16"
    }
  ]
}
```

## 5. `NEWS_EVENT`, `CHART_SERIES` 기준 구체 예시

### 5-1. `NEWS_EVENT`

Template Query Policy:
```json
{
  "dataSourceType": "NEWS_EVENT",
  "dataSourceRef": "NEWS_EVENT_BY_PERIOD",
  "requiredQueryParams": ["fromDate", "toDate", "market", "maxItems"],
  "contentKeys": ["items"]
}
```

Issue Block State:
```json
{
  "fromDate": "2026-03-10",
  "toDate": "2026-03-16",
  "market": "OIL",
  "maxItems": 6,
  "plotOnChart": true
}
```

Resolved Content:
```json
{
  "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지",
  "items": [
    {
      "newsId": "101",
      "title": "공급 차질 우려",
      "content": ["중동 정세 불안 지속"],
      "createdDate": "2026-03-16",
      "isTurningPoint": true
    }
  ]
}
```

### 5-2. `CHART_SERIES`

Template Query Policy:
```json
{
  "dataSourceType": "CHART_SERIES",
  "dataSourceRef": "BRENT_PRICE_BY_PERIOD",
  "requiredQueryParams": ["fromDate", "toDate", "interval", "market"],
  "contentKeys": ["timestamps", "series.close"]
}
```

## 6. 롤아웃 / 마이그레이션 원칙

### 6-1. 배포 순서
1. DBA가 state 테이블을 먼저 생성한다.
2. 애플리케이션은 state row가 없어도 기존 published read 및 기본 조립 경로가 깨지지 않는 호환 모드로 배포한다.
3. preview/write path부터 state 테이블을 사용한다.
4. publish 시점에만 `template + issue state + resolved content`를 합성해 `org_snpsht_json`을 저장한다.
5. original detail read는 계속 snapshot만 읽는다.

### 6-2. backfill 정책
- 기존 published issue에 대해 state row 강제 backfill은 필수가 아니다.
- 과거 발행본은 `org_snpsht_json`만으로 재현 가능해야 한다.
- 필요한 경우에만 preview/운영 편집용 default state row를 생성한다.

### 6-3. null-safe rollout 규칙
- 신규 컬럼/테이블 도입만으로 기존 read가 실패하면 안 된다.
- `ddl-auto=validate` 환경이므로 엔티티/매퍼 반영은 DBA DDL 적용 이후에만 진행한다.
- 자동 마이그레이션 도구를 전제하지 않는다.

Issue Block State:
```json
{
  "fromDate": "2026-03-01",
  "toDate": "2026-03-16",
  "interval": "DAY",
  "market": "BRENT",
  "plotOnChart": true
}
```

Resolved Content:
```json
{
  "timestamps": [1741996800000, 1742083200000],
  "series": {
    "close": [72.3, 73.1]
  }
}
```

## 6. `queryParams` 표준 키 네이밍 규칙

### 6-1. 원칙
- `queryParams` key는 실제 DB 컬럼명이 아니라 의미 기반 이름이어야 한다.
- 블록마다 실제 컬럼명이 달라도 같은 의미라면 동일 key를 사용한다.
- key 이름은 `camelCase`를 사용한다.

### 6-2. 대표 표준 키
- `fromDate`
- `toDate`
- `baseDate`
- `baseDateTime`
- `market`
- `companyNo`
- `issueNo`
- `interval`
- `maxItems`
- `orderByKey`
- `keyword`
- `chartType`

### 6-3. 금지 예시
- `evnt_crt_dtm_from`
- `timestamp_to`
- `brent_price_market_cd`

위와 같은 이름은 실제 컬럼 의존도가 높아 template이 물리 스키마를 과도하게 알게 된다.

## 7. `dataSourceRef -> 실제 컬럼 매핑` 예시표

| dataSourceRef | 의미 파라미터 | 실제 컬럼/조건 예시 |
|---|---|---|
| `NEWS_EVENT_BY_PERIOD` | `fromDate` | `tmd_nws_ltr_evnt.evnt_crt_tm >= :fromDate` |
| `NEWS_EVENT_BY_PERIOD` | `toDate` | `tmd_nws_ltr_evnt.evnt_crt_tm < :toDate` |
| `NEWS_EVENT_BY_PERIOD` | `market` | `tmd_nws_ltr_evnt.market_cd = :market` |
| `NEWS_EVENT_BY_PERIOD` | `maxItems` | `LIMIT :maxItems` |
| `BRENT_PRICE_BY_PERIOD` | `fromDate` | `tif_nws_ltr_chrt.timestamp >= :fromDate` |
| `BRENT_PRICE_BY_PERIOD` | `toDate` | `tif_nws_ltr_chrt.timestamp < :toDate` |
| `BRENT_PRICE_BY_PERIOD` | `interval` | `tif_nws_ltr_chrt.interval_cd = :interval` |
| `BRENT_PRICE_BY_PERIOD` | `market` | `tif_nws_ltr_chrt.chart_cd = :market` 또는 사전 정의 ref 해석 |

### 7-1. 해석 규칙
- `queryParams`는 템플릿이 정의하는 의미 파라미터다.
- 실제 DB 컬럼과 SQL 조립은 백엔드 resolver가 `dataSourceRef` 기준으로 수행한다.
- 프론트/기획은 `dataSourceRef`와 `requiredQueryParams`까지만 인지하면 된다.

## 8. 설계 결론
- 템플릿은 정책이다.
- issue state는 값이다.
- resolved content는 데이터다.
- original snapshot은 발행 완료본 동결본이다.
- 원본 조회는 snapshot 우선, preview는 동적 생성으로 분리한다.
- issue state 저장은 `section state + block state` 하이브리드 모델이 적절하다.
- `queryParams`는 의미 기반 이름으로 표준화하고, 실제 컬럼 매핑은 서버가 책임진다.
