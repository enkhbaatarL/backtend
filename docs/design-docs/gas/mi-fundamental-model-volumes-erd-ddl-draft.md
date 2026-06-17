# MI Fundamental Model - Volumes ERD / DDL Draft

## Document Control

- domain: gas
- status: Superseded
- canonical for: Reference only - historical Volumes persistence candidate based on `tmd_fm_vol_row` / `tmd_fm_vol_val`
- owner: platform BE
- updated at: 2026-06-10
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md`
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
- depends on:
  - Active Volumes API response contract in `mi-fundamental-model-mock-api-feature-spec.md`
  - Active output result persistence design in `mi-fundamental-model-output-result-persistence-design.md`
  - Scenario Case / Run tables `tmd_mdl_scnrcasem` and `tmd_mdl_scnrcaseh`
- superseded by:
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
- fixed decisions:
  - 없음. Active Volumes persistence decisions moved to `mi-fundamental-model-output-result-persistence-design.md`.
- open questions:
  - 없음. Active open questions moved to `mi-fundamental-model-output-result-persistence-design.md`.

---

## Supersession Notice

이 문서는 2026-06-04 시점의 Volumes 단독 ERD 후보를 보존하는 reference-only 문서다.

2026-06-10 기준 Volumes / Trade Flows / Price output result persistence의 canonical source는 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md) 이다.

Active Volumes persistence는 `tmd_mdl_scnrcaseh.run_seq`에 귀속되는 region-bucket JSON table `tmd_mdl_scnrcasevolrgnl`을 사용한다. `tmd_mdl_scnrcasevoll` atomic row 방향과 이 문서의 `tmd_fm_vol_row` / `tmd_fm_vol_val` 분리 구조, 인덱스, rollout SQL, verify SQL은 superseded 되었으며 DBA rollout 또는 구현 기준으로 사용하지 않는다.

이 문서의 아래 본문과 canonical 문서가 충돌하면 항상 `mi-fundamental-model-output-result-persistence-design.md`를 따른다.

## 1. Historical Scope (Superseded)

This draft covers DB-backed persistence for:

```text
GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes
```

Historical candidate supported API dimensions:

- `segment`: `GLOBAL`, `LNG`
- `types`: `DEMAND`, `PRODUCTION`, `LNG_IMPORTS`, `LNG_EXPORTS`, `PIPELINE_IMPORTS`, `PIPELINE_EXPORTS`
- `regions`: `ASIA`, `EUROPE`, `NORTH_AMERICA`, `MIDDLE_EAST`, `AUSTRALIA`, `RUSSIA`, `AFRICA`, `SOUTH_AMERICA`
- `granularity`: `MONTHLY`, `YEARLY`

Trade Flow data was outside this historical draft. Active Volumes/Trade Flows/Price persistence is now unified in `mi-fundamental-model-output-result-persistence-design.md`.

---

## 2. Historical Storage Model (Superseded)

Superseded candidate structure:

```text
tmd_mdl_scnrcaseh
  └──< tmd_fm_vol_val        -- run x leaf row x month value
          >── tmd_fm_vol_row -- reusable country-level row dimension
```

Stored rows:

- `COUNTRY` leaf rows only
- monthly values only (`base_month` 1-12)

Generated rows:

- Current API projection:
  - `COUNTRY`: sum leaf rows by `segment + data_type + region + country + period`, ignoring `contract_spt_cd`
  - `REGION_SUBTOTAL`: sum current-projection country rows by `segment + data_type + region + period`
  - `TYPE_TOTAL`: sum selected regions' generated `REGION_SUBTOTAL` rows by `segment + data_type + period`
- Future Contracted/Uncontracted projection:
  - include `contract_spt_cd` in the grouping key and expose or map it through a future API/display contract

This historical candidate kept subtotal data from becoming a second source of truth. Active rollout decisions are maintained in `mi-fundamental-model-output-result-persistence-design.md`.

---

## 3. Historical Candidate DDL (Not Rollout SQL)

### 3.1 `tmd_fm_vol_row`

Reusable country-level row dimension. It stores only leaf rows that can receive imported model values.

```sql
CREATE TABLE tmd_fm_vol_row (
    vol_row_seq       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'Volumes row surrogate key',
    segment_cd        VARCHAR(10)      NOT NULL                  COMMENT 'GLOBAL / LNG',
    data_type_cd      VARCHAR(30)      NOT NULL                  COMMENT 'DEMAND / PRODUCTION / LNG_IMPORTS / LNG_EXPORTS / PIPELINE_IMPORTS / PIPELINE_EXPORTS',
    region_cd         VARCHAR(30)      NOT NULL                  COMMENT 'Region code',
    country_nm        VARCHAR(100)     NOT NULL                  COMMENT 'Country display name',
    contract_spt_cd   VARCHAR(20)      NOT NULL DEFAULT 'NONE'   COMMENT 'NONE / CONTRACTED / UNCONTRACTED',
    disp_ord          SMALLINT         NOT NULL                  COMMENT 'Display order within segment/type/region/contract group',
    use_yn            CHAR(1)          NOT NULL DEFAULT 'Y'      COMMENT 'Use Y/N',
    reg_user_id       VARCHAR(100)     NULL                      COMMENT 'Registration user ID',
    reg_dtm           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Registration datetime',
    mod_user_id       VARCHAR(100)     NULL                      COMMENT 'Modification user ID',
    mod_dtm           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modification datetime',
    del_yn            CHAR(1)          NOT NULL DEFAULT 'N'      COMMENT 'Delete Y/N',

    UNIQUE KEY tmd_fm_vol_row_ux01 (
        segment_cd,
        data_type_cd,
        region_cd,
        country_nm,
        contract_spt_cd
    ),
    KEY tmd_fm_vol_row_ix01 (
        segment_cd,
        data_type_cd,
        region_cd,
        contract_spt_cd,
        disp_ord,
        vol_row_seq
    )
) COMMENT 'MI Fundamental Model Volumes country row dimension';
```

Notes:

- `row_kind` is intentionally absent. Stored rows are always country leaf rows.
- `region_cd` and `country_nm` are `NOT NULL` because subtotal/total rows are not stored.
- `contract_spt_cd = 'NONE'` keeps Global rows and LNG rows without contract split unique-safe without nullable composite-key behavior.
- When future LNG import starts generating Contracted/Uncontracted values, use `CONTRACTED` and `UNCONTRACTED` rows under the same country. Current API assembly must aggregate those rows into one country response row until the UI contract exposes the split.
- If an authoritative country code is available, add `country_cd` and use it in `ux01` instead of `country_nm`.

### 3.2 `tmd_fm_vol_val`

Run-level monthly country value fact table.

```sql
CREATE TABLE tmd_fm_vol_val (
    vol_val_seq       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'Volumes value surrogate key',
    run_seq           INT UNSIGNED     NOT NULL                  COMMENT 'tmd_mdl_scnrcaseh.run_seq FK',
    vol_row_seq       BIGINT UNSIGNED  NOT NULL                  COMMENT 'tmd_fm_vol_row.vol_row_seq FK',
    base_year         SMALLINT UNSIGNED NOT NULL                 COMMENT 'Base year',
    base_month        TINYINT UNSIGNED NOT NULL                  COMMENT 'Base month 1-12',
    vol_val           DECIMAL(15,4)    NOT NULL                  COMMENT 'Volume value, unit mtpa',
    reg_user_id       VARCHAR(100)     NULL                      COMMENT 'Registration user ID',
    reg_dtm           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Registration datetime',
    mod_user_id       VARCHAR(100)     NULL                      COMMENT 'Modification user ID',
    mod_dtm           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modification datetime',
    del_yn            CHAR(1)          NOT NULL DEFAULT 'N'      COMMENT 'Delete Y/N',

    CONSTRAINT tmd_fm_vol_val_fk01 FOREIGN KEY (run_seq)
        REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_fm_vol_val_fk02 FOREIGN KEY (vol_row_seq)
        REFERENCES tmd_fm_vol_row (vol_row_seq),

    UNIQUE KEY tmd_fm_vol_val_ux01 (
        run_seq,
        vol_row_seq,
        base_year,
        base_month
    ),
    KEY tmd_fm_vol_val_ix01 (
        run_seq,
        base_year,
        base_month,
        vol_row_seq
    ),
    KEY tmd_fm_vol_val_ix02 (
        vol_row_seq,
        run_seq,
        base_year,
        base_month
    ),

    CONSTRAINT tmd_fm_vol_val_ck01 CHECK (base_month BETWEEN 1 AND 12)
) COMMENT 'MI Fundamental Model Volumes monthly country value';
```

Notes:

- `run_seq` is used instead of `case_id` because Volumes are execution results. The API `caseId` must resolve the latest completed run or the otherwise selected run before querying this table.
- `base_month = 0` is intentionally not used in this draft. `YEARLY` response values are generated by summing monthly rows.
- `vol_val` is `NOT NULL`; missing model output should be represented by no fact row. Response assembly can left join/gap-fill to `null` if the API must expose missing cells.

---

## 4. Historical Query Assembly Rules (Superseded)

### 4.1 Base country rows

```sql
SELECT
    r.segment_cd,
    r.data_type_cd,
    r.region_cd,
    r.country_nm,
    r.contract_spt_cd,
    v.base_year,
    v.base_month,
    v.vol_val
FROM tmd_fm_vol_val v
JOIN tmd_fm_vol_row r
  ON r.vol_row_seq = v.vol_row_seq
 AND r.use_yn = 'Y'
 AND r.del_yn = 'N'
WHERE v.run_seq = :run_seq
  AND v.del_yn = 'N'
  AND r.segment_cd = :segment
  AND r.data_type_cd IN (:types)
  AND r.region_cd IN (:regions)
ORDER BY
    r.data_type_cd,
    r.region_cd,
    r.contract_spt_cd,
    r.disp_ord,
    r.country_nm,
    v.base_year,
    v.base_month;
```

### 4.2 Generated rows

Application or SQL aggregation generates:

- country response rows from base country values
- current UI/API `COUNTRY` rows by `data_type_cd + region_cd + country_nm`, aggregating all `contract_spt_cd` values
- current UI/API `REGION_SUBTOTAL` rows by `data_type_cd + region_cd`
- current UI/API `TYPE_TOTAL` rows by `data_type_cd` over the selected regions

Future UI/API support for Contracted/Uncontracted can include `contract_spt_cd` in these grouping keys without changing the base table structure.

---

## 5. Historical ERD Review (Superseded)

### 5.1 Normalization

| Normal form | Assessment |
|---|---|
| 1NF | Satisfied. Each row stores one leaf dimension and one monthly value. |
| 2NF | Satisfied. Fact attributes depend on the full business key `run_seq + vol_row_seq + base_year + base_month`. |
| 3NF | Mostly satisfied. `region_cd`, `country_nm`, and `contract_spt_cd` belong to row dimension, not fact. |
| BCNF | Candidate determinant is row business key in `tmd_fm_vol_row`; fact uniqueness is run-row-period. |

### 5.2 Unique Keys

- `tmd_fm_vol_row_ux01` identifies one reusable country-level leaf row.
- `tmd_fm_vol_val_ux01` prevents duplicate values for the same run, country row, year, and month.
- No unique key includes nullable `region` or `country` fields because subtotal and total rows are not persisted.

### 5.3 Index Strategy

- `tmd_fm_vol_row_ix01` supports segment/type/region filtering and display ordering.
- `tmd_fm_vol_val_ix01` supports run-period scans and period column generation.
- `tmd_fm_vol_val_ix02` supports row-first joins when optimizer starts from filtered row dimension.
- Standalone low-cardinality indexes on `segment_cd`, `data_type_cd`, `region_cd`, `contract_spt_cd`, or `del_yn` are intentionally avoided.

### 5.4 Extensibility

- New years require no schema change.
- New data types require row master rows and enum/API validation updates; no fact schema change.
- New regions/countries require row master rows; no fact schema change.
- New segments require row master rows and API validation updates; no fact schema change.
- If row structure becomes model-version-specific, add a template/version key to `tmd_fm_vol_row` or snapshot row dimension per run.

---

## 6. Historical Code Impact Review (Superseded)

- Controller/DTO contract still returns `COUNTRY`, `REGION_SUBTOTAL`, and `TYPE_TOTAL`; only DB persistence scope changes.
- Service must resolve `caseId` to active completed case and target `run_seq`.
- Repository query returns country leaf rows only.
- Response assembler must generate subtotal/total rows in the same order as the current mock behavior.
- Current response assembler must aggregate across `contract_spt_cd` because current UI does not display Contracted/Uncontracted.
- Future LNG Contracted/Uncontracted display requires an API response field addition or a deterministic display mapping, but no base table change.
- Use `BigDecimal` internally for aggregation and convert to response numeric type at the boundary.

---

## 7. Historical Rollout / Compatibility Review (Not Rollout SQL)

Do not use this section for DBA rollout. The active rollout SQL, verify SQL, rollback order, and partition/index strategy are defined in `mi-fundamental-model-output-result-persistence-design.md`.

### Pre-check

```sql
SHOW TABLES LIKE 'tmd_fm_vol_row';
SHOW TABLES LIKE 'tmd_fm_vol_val';
SHOW TABLES LIKE 'tmd_mdl_scnrcaseh';
```

Expected:

- Volumes tables do not already exist, unless this draft is being revised.
- `tmd_mdl_scnrcaseh` exists with compatible `run_seq` type.

### Apply

Use the `CREATE TABLE` statements in section 3 after DBA naming and type review.

### Backfill

Not needed for first rollout if DB-backed Volumes import starts after table creation.

### Rollback

```sql
DROP TABLE IF EXISTS tmd_fm_vol_val;
DROP TABLE IF EXISTS tmd_fm_vol_row;
```

Rollback is safe only before production data import. After import, use DBA-approved backup/export before dropping.

### Verify

```sql
SHOW INDEX FROM tmd_fm_vol_row;
SHOW INDEX FROM tmd_fm_vol_val;

SELECT COUNT(*) AS row_cnt
FROM tmd_fm_vol_row
WHERE del_yn = 'N';

SELECT run_seq, COUNT(*) AS val_cnt
FROM tmd_fm_vol_val
WHERE del_yn = 'N'
GROUP BY run_seq;
```

### DBA Action / Deployment Note

- Confirm physical naming against MI Platform DB object naming rules.
- Confirm dictionary terms for `segment`, `fundamental`, `contract`, and any final abbreviations.
- Confirm FK creation policy for result tables.
- Run `EXPLAIN` on the base country-row query before production rollout.

---

## 8. Implementation Readiness

superseded - not implementation input

Do not implement from this document. Active Volumes persistence uses `tmd_mdl_scnrcasevolrgnl` as defined in `mi-fundamental-model-output-result-persistence-design.md`.
