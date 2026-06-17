# MI Fundamental Model - Volumes Country Series JSON Superseded Candidate

## Document Control

- status: Superseded
- canonical for: Reference only - rejected/superseded Volumes country-series JSON candidate
- owner: Shared BE/DBA
- source documents:
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
  - `docs/design-docs/gas/mi-fundamental-model-volumes-region-bucket-json-plan-b.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- depends on:
  - `tmd_mdl_scnrcaseh.run_seq`
  - Active Volumes API read-only query behavior
- supersedes:
  - 없음
- superseded by:
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
- last synced: 2026-06-17
- open questions:
  - 없음. This candidate is not implementation input.

## Supersession Notice

2026-06-17 결정으로 Volumes persistence는 `tmd_mdl_scnrcasevolrgnl` region-bucket JSON table을 사용한다. 이 문서의 `tmd_mdl_scnrcasevolnatl` country-series JSON 후보는 superseded 되었으며 DBA rollout 또는 구현 기준으로 사용하지 않는다.

Active DDL, query, rollout, verify SQL은 `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`를 따른다.

## Purpose

This historical draft refined the earlier region-bucket JSON Plan B for:

```text
GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes
```

The earlier Plan B grouped all countries under one `region` bucket. This document proposed a more intuitive storage key:

```text
run_seq + segment + data_type_cd + region + country + perd_unit_spt_cd
```

The JSON payload then stores the period/value series for that country or region subtotal row. This keeps the row meaning easy to inspect while still reducing insert volume compared with one row per period value.

## Applicability Conditions

This superseded design was considered only under these constraints:

- `GET /cases/{caseId}/volumes` remains read-only.
- Result rows are fully loaded before the run is marked `COMPLETED`.
- Query filters remain `segment`, `types`, `regions`, and `granularity`.
- DB-side country search, value threshold filtering, and cell-level aggregation are not required.
- `perd_key`, `attr_type_cd`, and `vol_val` are not direct SQL `WHERE` conditions.

If future requirements need SQL filtering below country row level, perform a new ERD review instead of reviving this candidate directly.

## Proposed Storage Model

Store one row per country or region subtotal series:

```text
run_seq + sgmt_cd + data_type_cd + rgn_cd + nat_nm + perd_unit_spt_cd
```

`nat_nm` is always `NOT NULL`.

Rules:

- Country row: `nat_nm` stores the actual country name.
- Region subtotal row: `nat_nm = '__REGION_SUBTOTAL__'`.
- Do not store `row_kind_cd`. If the service needs row kind semantics, derive `REGION_SUBTOTAL` from `nat_nm = '__REGION_SUBTOTAL__'`; otherwise derive `COUNTRY`.
- `TYPE_TOTAL` is not stored. The service computes it by summing selected regions' `REGION_SUBTOTAL` rows per period.

This removes `row_key` while avoiding MySQL nullable unique-key ambiguity.

## Candidate Table

Superseded candidate table name:

- `tmd_mdl_scnrcasevolnatl`
- Meaning: `TMD_시나리오케이스실행물량국가내역`
- Entity type: `l` because rows are immutable run result detail/history records.

```sql
CREATE TABLE tmd_mdl_scnrcasevolnatl
(
    vol_nat_seq      BIGINT UNSIGNED AUTO_INCREMENT COMMENT '물량국가순번'
        PRIMARY KEY,
    run_seq          INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    sgmt_cd          VARCHAR(20)                           NOT NULL COMMENT '세그먼트코드 (GLOBAL/LNG)',
    data_type_cd     VARCHAR(30)                           NOT NULL COMMENT '데이터타입코드',
    rgn_cd           VARCHAR(50)                           NOT NULL COMMENT '지역코드',
    nat_nm           VARCHAR(120)                          NOT NULL COMMENT '국가명 또는 지역합계 sentinel',
    perd_unit_spt_cd VARCHAR(2)                            NOT NULL COMMENT '기간단위구분코드 (Y:년/M:월, PeriodUnitSeparateCd)',
    json_schm_ver    SMALLINT UNSIGNED DEFAULT 1           NOT NULL COMMENT 'JSON스키마버전',
    vol_data_json    JSON                                  NOT NULL COMMENT '물량데이터JSON',
    CONSTRAINT tmd_mdl_scnrcasevolnatl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasevolnatl_ux01
        UNIQUE (run_seq, sgmt_cd, data_type_cd, rgn_cd, nat_nm, perd_unit_spt_cd),
    CONSTRAINT tmd_mdl_scnrcasevolnatl_ck01
        CHECK (perd_unit_spt_cd IN ('Y', 'M')),
    CONSTRAINT tmd_mdl_scnrcasevolnatl_ck02
        CHECK (JSON_VALID(vol_data_json))
)
    COMMENT 'TMD_시나리오케이스실행물량국가내역';
```

Index note:

- `tmd_mdl_scnrcasevolnatl_ux01` is expected to support the main read path.
- Do not add a duplicate secondary index with the same columns unless DBA `EXPLAIN` shows a need.
- Avoid standalone indexes on low-cardinality columns such as `sgmt_cd`, `perd_unit_spt_cd`, or `del_yn`.

## JSON Array Payload

Candidate `vol_data_json` stores a JSON array of period/attribute values:

```json
[
  {
    "perdKey": "202601",
    "attrType": "TOTAL",
    "volVal": 15.1
  },
  {
    "perdKey": "202602",
    "attrType": "TOTAL",
    "volVal": 15.4
  }
]
```

For LNG contracted/uncontracted values, the same country row can contain multiple `attrType` entries per period:

```json
[
  {
    "perdKey": "202601",
    "attrType": "TOTAL",
    "volVal": 15.1
  },
  {
    "perdKey": "202601",
    "attrType": "CONTRACTED",
    "volVal": 10.0
  },
  {
    "perdKey": "202601",
    "attrType": "UNCONTRACTED",
    "volVal": 5.1
  }
]
```

Validation rules:

- `vol_data_json` must be a non-empty JSON array.
- Every item must include `perdKey`, `attrType`, and `volVal`.
- `perd_unit_spt_cd = 'M'`: every `perdKey` must match `YYYYMM` and month `01` through `12`.
- `perd_unit_spt_cd = 'Y'`: every `perdKey` must match `YYYY`.
- `attrType` must be `TOTAL`, `CONTRACTED`, or `UNCONTRACTED`.
- `CONTRACTED` and `UNCONTRACTED` are valid only for `sgmt_cd = 'LNG'` and `data_type_cd IN ('LNG_IMPORTS', 'LNG_EXPORTS')`.
- `(perdKey, attrType)` must be unique inside the JSON array.
- `json_schm_ver` stays as a lightweight compatibility marker for future JSON shape changes.

## Query Shape

```sql
SELECT data_type_cd,
       rgn_cd,
       nat_nm,
       perd_unit_spt_cd,
       json_schm_ver,
       vol_data_json
FROM tmd_mdl_scnrcasevolnatl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = :segment
  AND data_type_cd IN (:types)
  AND rgn_cd IN (:regions)
  AND perd_unit_spt_cd = :period_unit
ORDER BY data_type_cd,
         rgn_cd,
         CASE nat_nm WHEN '__REGION_SUBTOTAL__' THEN 0 ELSE 1 END,
         nat_nm;
```

Service assembly:

1. Resolve `caseId -> latest completed run_seq`.
2. Query selected country/subtotal series rows.
3. Build `columns` from the union of selected `perdKey` values.
4. Render each selected type/region with `REGION_SUBTOTAL` first, then `COUNTRY` rows.
5. For `nat_nm = '__REGION_SUBTOTAL__'`, derive API row kind as `REGION_SUBTOTAL`, set `country = null`, and use the region label as `rowLabel`.
6. For all other `nat_nm` values, derive API row kind as `COUNTRY`, use `nat_nm` as API `country` and `rowLabel`.
7. Compute `TYPE_TOTAL` from selected regions' `REGION_SUBTOTAL` series.

## ERD Review

### Change Classification

Redesign recommended.

This is not a minor column/index change from the atomic row model. It changes the Volumes result table grain from one row per cell to one row per country/subtotal series.

### Normalization

This design intentionally violates 1NF because `vol_data_json` stores repeated period/attribute values in one row.

The denormalization is justified only under the read-only API constraints because it reduces insert volume while keeping rows inspectable at country/subtotal level.

Compared with region-bucket JSON, this country-series design is easier to inspect and safer for payload size, but it inserts more rows.

### Key Safety

- `nat_nm` is `NOT NULL`.
- `__REGION_SUBTOTAL__` is a reserved sentinel value.
- `row_kind_cd` is not stored; service-side row kind is derived from the sentinel.
- Loader validation must prevent a real country row from using the sentinel.
- `TYPE_TOTAL` is not stored to avoid ambiguity with partial region filters.

### Code Impact

- JPA/MyBatis projection changes from atomic value rows to series rows with JSON parsing.
- DTO response shape does not need to change.
- Service logic must parse `vol_data_json`, pivot by `perdKey`, and compute `TYPE_TOTAL`.
- Loader must validate sentinel usage, JSON array uniqueness, and period format.

## Recommendation

Do not use this country-series JSON design for implementation.

It was more intuitive than region-bucket JSON because each row mapped to a single displayed country or region subtotal row, but the accepted 2026-06-17 direction is `tmd_mdl_scnrcasevolrgnl` region-bucket JSON.

Do not implement this candidate unless a future ERD review explicitly supersedes `tmd_mdl_scnrcasevolrgnl`.

## Dictionary Updates

No dictionary update was made.

Terms already present and used:

- `물량` / `VOL`
- `국가` / `NAT`
- `지역` / `RGN`
- `JSON` / `JSON`
- `스키마버전` / `SCHMVER`
- `기간` / `PERD`
- `수` / `CNT`
- `데이터` / `DATA`
- `값` / `VAL`

## Implementation Readiness

`superseded - not implementation input`

Use `mi-fundamental-model-output-result-persistence-design.md` for active Volumes persistence.
