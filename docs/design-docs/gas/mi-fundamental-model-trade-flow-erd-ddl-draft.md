# MI Fundamental Model - Trade Flow ERD / DDL Draft

## Document Control

- domain: gas
- status: Superseded
- canonical for: Reference only - historical Trade Flow header/flow persistence candidate
- owner: platform BE
- updated at: 2026-06-11 (Open Decisions 표에 현재 해소 상태 주석 추가; DDL 본문은 historical reference로 보존)
- source documents:
  - mi-fundamental-model-mock-api-feature-spec.md
  - mi-fundamental-model-trade-flows-api-design.md
  - scenario-case-execution-erd-ddl-draft.md
- depends on:
  - Trade Flow API response contract (`key`, `label`, `kind`, `fromKey`, `toKey`) in mi-fundamental-model-mock-api-feature-spec.md
- superseded by:
  - mi-fundamental-model-output-result-persistence-design.md
- open questions:
  - 없음. Active open questions moved to mi-fundamental-model-output-result-persistence-design.md.

---

## Supersession Notice

이 문서는 2026-06-03 시점의 Trade Flow 단독 ERD 후보를 보존하는 reference-only 문서다.

2026-06-07 이후 Volumes / Trade Flows / Price output result persistence의 canonical source는 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md) 이다.

특히 이 문서의 JSON header 후보(`from_countries_json`, `to_countries_json`)는 superseded되었다. Canonical 설계는 `tmd_mdl_scnrcasetrdehdrl` row table과 `tmd_mdl_scnrcasetrdeflwl` sparse flow table을 사용한다.

## 1. 구조 개요

```
tmd_mdl_scnrcasem (case)
  └── tmd_mdl_scnrcaseh (run)
        ├── tmd_mdl_scnrcaseh_trd_hdr  -- run × selector × year 단위 헤더 (country 목록)
        └── tmd_mdl_scnrcaseh_trd      -- 실제 non-zero flow 값
```

- run 1건에 selector(LNG/PNG) × year 조합 수만큼 `tmd_mdl_scnrcaseh_trd_hdr` 행이 생긴다.
- `tmd_mdl_scnrcaseh_trd_hdr` 1건에 해당하는 non-zero flow 값들이 `tmd_mdl_scnrcaseh_trd`에 저장된다.
- country 목록은 base 버전에 따라 달라질 수 있으나 가끔 변경되는 수준이므로 JSON 컬럼으로 관리한다.
- API 응답의 `key`, `label`, `kind` 계약을 DB-backed 전환 후에도 유지하기 위해 header JSON에 동일한 식별/표시 메타데이터를 포함한다.
- flow 행은 nullable `region/country` 조합 대신 non-null `from_country_key`, `to_country_key` 후보 컬럼으로 중복 방지한다.
- flow 값은 MTPA 기준으로 저장하는 후보안이다. API `unit=BCM` 지원이 필요하면 저장값을 조회 시 변환한다.

---

## 2. Candidate DDL (Design Review Only — Not Rollout SQL)

### 2.1 tmd_mdl_scnrcaseh_trd_hdr

run × selector × year 조합별 헤더 테이블.
from/to country 목록을 JSON으로 저장한다.

```sql
CREATE TABLE tmd_mdl_scnrcaseh_trd_hdr (
    trd_hdr_seq       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'Trade Flow 헤더 surrogate key',
    run_seq           INT UNSIGNED        NOT NULL                COMMENT 'tmd_mdl_scnrcaseh.run_seq FK',
    selector_cd       VARCHAR(10)         NOT NULL                COMMENT 'LNG / PNG',
    base_year         SMALLINT UNSIGNED   NOT NULL                COMMENT '기준 연도 (예: 2026)',
    from_countries_json JSON              NOT NULL                COMMENT 'From country 목록 [{key,label,kind,region,country}]',
    to_countries_json   JSON              NOT NULL                COMMENT 'To country 목록 [{key,label,kind,region,country}] — Total 행 포함',
    crt_dtm           DATETIME            NOT NULL DEFAULT NOW()  COMMENT '생성일시',
    mod_dtm           DATETIME            NOT NULL DEFAULT NOW()  COMMENT '수정일시',

    CONSTRAINT fk_trd_hdr_run FOREIGN KEY (run_seq)
        REFERENCES tmd_mdl_scnrcaseh (run_seq),

    UNIQUE KEY uq_trd_hdr_run_selector_year (run_seq, selector_cd, base_year)
) COMMENT 'MI Fundamental Model Trade Flow 헤더 (run × selector × year)';
```

**JSON 컬럼 예시 (from_countries_json / to_countries_json):**

```json
[
  { "key": "REGION:AFRICA", "label": "Africa", "kind": "REGION_SUBTOTAL", "region": "AFRICA", "country": null },
  { "key": "COUNTRY:AFRICA:Angola", "label": "Angola", "kind": "COUNTRY", "region": "AFRICA", "country": "Angola" },
  { "key": "COUNTRY:AFRICA:Algeria", "label": "Algeria", "kind": "COUNTRY", "region": "AFRICA", "country": "Algeria" },
  { "key": "REGION:NORTH_AMERICA", "label": "America N", "kind": "REGION_SUBTOTAL", "region": "NORTH_AMERICA", "country": null },
  { "key": "TOTAL", "label": "Total", "kind": "GRAND_TOTAL", "region": null, "country": "Total" }
]
```

- `key`: API `fromCountries[].key`, `toCountries[].key`, `flows[].fromKey`, `flows[].toKey`와 동일한 응답 식별자
- `label`: 화면 표시명. 예: `Africa`, `America N`, `Angola`, `Total`
- `kind = REGION_SUBTOTAL`: 지역 집계 행/열 (화면 볼드 처리)
- `kind = COUNTRY`: 개별 국가
- `kind = GRAND_TOTAL`: 합계 행 (`to_countries_json` 마지막에만 위치)
- `region` / `country`: region filter와 후속 조회 조건을 위한 원천 메타데이터

---

### 2.2 tmd_mdl_scnrcaseh_trd

non-zero flow 값 저장 테이블 (Sparse Matrix).

```sql
CREATE TABLE tmd_mdl_scnrcaseh_trd (
    trd_seq           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'Trade Flow surrogate key',
    trd_hdr_seq       BIGINT UNSIGNED     NOT NULL                COMMENT 'tmd_mdl_scnrcaseh_trd_hdr.trd_hdr_seq FK',
    from_country_key  VARCHAR(180)        NOT NULL                COMMENT 'From header key (from_countries_json.key)',
    to_country_key    VARCHAR(180)        NOT NULL                COMMENT 'To header key (to_countries_json.key)',
    from_region       VARCHAR(50)         NULL                    COMMENT 'From 지역 (region 집계 행이면 country=null)',
    from_country      VARCHAR(100)        NULL                    COMMENT 'From 국가 (지역 집계 행이면 null)',
    to_region         VARCHAR(50)         NULL                    COMMENT 'To 지역 (Total 행이면 null)',
    to_country        VARCHAR(100)        NULL                    COMMENT 'To 국가 (Total이면 "Total", 지역집계면 null)',
    flow_value        DECIMAL(30, 18)     NOT NULL                COMMENT 'Flow 수치 (non-zero만 저장)',
    crt_dtm           DATETIME            NOT NULL DEFAULT NOW()  COMMENT '생성일시',

    CONSTRAINT fk_trd_hdr FOREIGN KEY (trd_hdr_seq)
        REFERENCES tmd_mdl_scnrcaseh_trd_hdr (trd_hdr_seq),

    UNIQUE KEY uq_trd_flow (trd_hdr_seq, from_country_key, to_country_key)
) COMMENT 'MI Fundamental Model Trade Flow 수치 (Sparse Matrix, non-zero only)';
```

- `from_country_key`, `to_country_key`는 nullable region/country 조합의 MySQL unique 한계를 피하기 위한 후보 컬럼이다.
- 애플리케이션은 `from_country_key`가 `from_countries_json[*].key`, `to_country_key`가 `to_countries_json[*].key`에 존재하는지 검증한다.
- `from_region/from_country/to_region/to_country`는 필터 조회와 디버깅/운영 확인을 위해 중복 저장하는 후보안이다.

---

## 3. 인덱스 설계 (후보)

```sql
-- run 기준 조회 (가장 빈번)
CREATE INDEX tmd_mdl_scnrcaseh_trd_hdr_ix01 ON tmd_mdl_scnrcaseh_trd_hdr (run_seq);

-- flow 조회 (hdr_seq 기준 — FK 인덱스로 커버 가능)
-- tmd_mdl_scnrcaseh_trd의 fk_trd_hdr가 인덱스 역할 수행
-- region 필터 조회 빈도가 높을 경우 아래 추가 검토
-- CREATE INDEX tmd_mdl_scnrcaseh_trd_ix01 ON tmd_mdl_scnrcaseh_trd (trd_hdr_seq, to_region);
-- CREATE INDEX tmd_mdl_scnrcaseh_trd_ix02 ON tmd_mdl_scnrcaseh_trd (trd_hdr_seq, from_region);
```

---

## 4. Open Decisions (해소 완료 — canonical 문서로 이관됨)

> 이 표의 항목들은 작성 당시(2026-06-03)에는 미결이었으나, 현재는 대부분 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)(status: Approved)에서 확정/superseded되었다. **이 표를 live action list로 사용하지 말 것.** 남아 있는 미결(precision, MTPA→BCM 변환 계수)은 canonical 문서의 open questions에서만 추적한다.

| # | 항목 | 작성 당시 방향 | 현재 상태 (2026-06-11 기준) |
|---|------|-----------|-----------|
| 1 | `from_countries_json` / `to_countries_json` JSON 컬럼 | JSON 컬럼 채택 (country 변경 빈도 낮음) | **Superseded** — header는 JSON이 아니라 row table `tmd_mdl_scnrcasetrdehdrl`로 저장 (canonical Fixed Decision 5) |
| 2 | `flow_value` 타입 | `DECIMAL(30,18)` 후보 | **Open (이관)** — canonical open questions의 numeric precision 항목으로 추적. 모델 output 자릿수 확인 후 확정 |
| 3 | 집계 행(region aggregate, Total) 저장 방식 | 엑셀 원본 그대로 저장 (파생값 미계산) | **Resolved** — non-zero sparse 저장 + header `hdr_kind_cd`(REGION_SUBTOTAL/GRAND_TOTAL)로 canonical 정의. 엑셀 원천 저장 범위만 모델팀 확인 |
| 4 | region 필터 조회 빈도 | 미확인 | **Resolved** — canonical에 `tmd_mdl_scnrcasetrdeflwl_ix02 (…, to_rgn_cd)`, `ix03 (…, from_rgn_cd)` 정의됨 |
| 5 | `tmd_mdl_scnrcaseh.run_seq` FK 타입 호환 | `INT UNSIGNED` 기준 | **Resolved** — `run_seq INT UNSIGNED` 확정, `tcm_filel.file_rel_obj_dstg_no INT UNSIGNED` 호환 확인 (case-erd-ddl draft) |
| 6 | 저장 단위 / 응답 단위 | DB 저장은 MTPA 단일 기준 후보. API `unit=BCM`은 조회 시 변환 | **Partially resolved** — MTPA 저장 + 조회 시 변환 확정. 변환 계수·반올림·표시 정밀도는 service logic 미결 |
| 7 | header key / label / kind 저장 범위 | header JSON에 API 응답 메타데이터 포함. flow는 `from_country_key` / `to_country_key` non-null 저장 | **Superseded** — row table 컬럼 `hdr_key` / `hdr_lbl_nm` / `hdr_kind_cd`로 확정 |

---

## 5. 조회 패턴 (예상)

### 5.1 기본 조회 (필터 없음)

```sql
-- 1. 헤더 조회 (country 목록 + 메타)
SELECT trd_hdr_seq, selector_cd, base_year,
       from_countries_json, to_countries_json
FROM tmd_mdl_scnrcaseh_trd_hdr
WHERE run_seq = :run_seq
  AND selector_cd = :selector
  AND base_year = :year;

-- 2. Flow 조회
SELECT from_country_key, to_country_key,
       from_region, from_country, to_region, to_country, flow_value
FROM tmd_mdl_scnrcaseh_trd
WHERE trd_hdr_seq = :trd_hdr_seq;
```

### 5.2 region 필터 적용 시

- `types = TO_COUNTRY`, `regions = [ASIA]` → `WHERE to_region = 'ASIA' OR to_country_key = 'TOTAL'`
- `types = FROM_COUNTRY`, `regions = [AFRICA]` → `WHERE from_region = 'AFRICA'`
- Total 행(`to_region IS NULL`)은 필터와 무관하게 항상 포함

---

## 6. 관계도

```
tmd_mdl_scnrcasem
  │  case_id (PK)
  │
  └──< tmd_mdl_scnrcaseh
         │  run_seq (PK, INT UNSIGNED AUTO_INCREMENT)
         │  run_id  (VARCHAR(20), UNIQUE)
         │
         └──< tmd_mdl_scnrcaseh_trd_hdr
                │  trd_hdr_seq (PK)
                │  run_seq (FK)
                │  selector_cd + base_year (UNIQUE with run_seq)
                │  from_countries_json [{key,label,kind,region,country}]
                │  to_countries_json   [{key,label,kind,region,country}]
                │
                └──< tmd_mdl_scnrcaseh_trd
                       trd_seq (PK)
                       trd_hdr_seq (FK)
                       from_country_key / to_country_key (UNIQUE with trd_hdr_seq)
                       from_region / from_country
                       to_region / to_country
                       flow_value
```

---

## 7. Implementation Readiness

superseded

- 이 문서는 더 이상 rollout SQL 또는 implementation input으로 사용하지 않는다.
- 새 implementation/DBA review는 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)를 기준으로 진행한다.
