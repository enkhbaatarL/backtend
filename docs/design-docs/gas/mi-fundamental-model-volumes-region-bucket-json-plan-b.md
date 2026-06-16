# MI Fundamental Model - Volumes Region Bucket JSON Plan B

## Document Control

- status: Draft
- canonical for: Reference only - Plan B schema candidate for Volumes region-bucket JSON persistence
- owner: Shared BE/DBA
- source documents:
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- depends on:
  - `tmd_mdl_scnrcaseh.run_seq`
  - Volumes API read-only query contract in `mi-fundamental-model-mock-api-feature-spec.md`
  - Current approved atomic-row design in `mi-fundamental-model-output-result-persistence-design.md`
- supersedes:
  - 없음
- last synced: 2026-06-15
- open questions:
  - DBA approval for JSON column usage as canonical parsed result storage
  - Final JSON schema and loader-side validation responsibility
  - Whether to keep a temporary atomic staging table during model output load verification
  - Maximum expected JSON payload size per `run_seq + sgmt_cd + perd_unit_spt_cd + data_type_cd + rgn_cd`

## Purpose

This draft evaluates a Plan B persistence model for:

```text
GET /api/v1/mi/fundamental-model/cases/{caseId}/volumes
```

The current approved design stores one atomic row per Volumes row/period/value in `tmd_mdl_scnrcasevoll`. After checking generated `GLOBAL` monthly data, one model execution can require more than 20,000 Volumes inserts for only the Global monthly slice. Because the API is read-only and filters only by `segment`, `type`, `region`, and `granularity`, this draft proposes a region-bucket JSON table as an intentional denormalization candidate.

This document is not canonical rollout SQL until approved by BE/DBA. If accepted, `mi-fundamental-model-output-result-persistence-design.md` must be updated to supersede the atomic Volumes table decision.

## Source-of-Truth Boundaries

| Fact type | Canonical document |
|---|---|
| Endpoint/request/response/error contract | `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md` |
| Case/run lifecycle and `run_seq` result ownership | `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md` |
| Current approved output result table set | `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md` |
| This Plan B candidate | This document, reference-only until approved |

## Applicability Conditions

The region-bucket JSON design is appropriate only if all conditions hold:

- `GET /cases/{caseId}/volumes` remains read-only.
- Result materialization is completed before callback transitions the run to `COMPLETED`.
- Query filters remain limited to `segment`, `types`, `regions`, and `granularity`.
- No DB-side country search, country sort, cell-level filter, or aggregate query is required.
- `perd_key`, `attr_type_cd`, and `vol_val` are not used as direct SQL `WHERE` conditions.
- API response ordering is deterministic from JSON payload order plus enum/request filter order.

If any condition changes, prefer the approved atomic-row design or introduce a separate normalized detail table.

## Proposed Storage Model

Store one row per:

```text
run_seq + sgmt_cd + perd_unit_spt_cd + data_type_cd + rgn_cd
```

The row stores all country rows, period values, attribute values, and the region subtotal for that region/type/granularity in a JSON payload.

`TYPE_TOTAL` is not stored in each region bucket. The service computes API `TYPE_TOTAL` by summing selected region buckets' `REGION_SUBTOTAL` rows. This preserves the current API rule that partial region filters return a total over only selected regions.

Expected row count for `GLOBAL + MONTHLY`:

```text
6 data types * 8 regions = 48 rows per run
```

This replaces more than 20,000 cell-level inserts for the same slice with bucket-level inserts.

## Candidate DDL

Candidate table name:

- `tmd_mdl_scnrcasevolrgnl`
- Meaning: `TMD_시나리오케이스실행물량지역내역`
- Entity type: `l` because rows are immutable run result history/detail records.

```sql
CREATE TABLE tmd_mdl_scnrcasevolrgnl
(
    vol_rgn_seq      BIGINT UNSIGNED AUTO_INCREMENT COMMENT '물량지역순번'
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
    perd_unit_spt_cd VARCHAR(2)                            NOT NULL COMMENT '기간단위구분코드 (Y:년/M:월, PeriodUnitSeparateCd)',
    json_schm_ver    SMALLINT UNSIGNED DEFAULT 1           NOT NULL COMMENT 'JSON스키마버전',
    vol_data_json    JSON                                  NOT NULL COMMENT '물량데이터JSON',
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ux01
        UNIQUE (run_seq, sgmt_cd, data_type_cd, rgn_cd, perd_unit_spt_cd),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ck01
        CHECK (perd_unit_spt_cd IN ('Y', 'M')),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ck02
        CHECK (JSON_VALID(vol_data_json))
)
    COMMENT 'TMD_시나리오케이스실행물량지역내역';
```

Notes:

- `ux01` already supports the only expected read path and the exact key lookup, so no separate secondary index is created. Add one only after `EXPLAIN` and DBA review if `IN` predicates do not use `ux01` well.
- `rgn_cd` is `NOT NULL` because every bucket belongs to exactly one region.
- The earlier draft proposed extra inspection columns `row_cnt`, `perd_cnt`, and `data_hash`. These were dropped from the applied table to keep the row minimal; the same counts are derivable from the JSON (`rows.length`, `periodKeys.length`) and integrity is enforced in the loader instead. Only `json_schm_ver` is retained for schema evolution.
- If DBA policy does not allow `CHECK` constraints, enforce the same validation in the Python/model loader and Java/backend verification.

## JSON Payload Shape

Applied `vol_data_json` structure (slim form). Each row carries only `country`, `attrType`, and `values`; row identity (`rowKind`, `rowKey`) and explicit `order` are dropped and instead derived at parse time. `values` is a **positional array aligned to `periodKeys`** (not a keyed object), which avoids repeating the period-key string on every row — a large saving for monthly buckets:

```json
{
  "schemaVersion": 1,
  "segment": "GLOBAL",
  "periodUnit": "M",
  "dataType": "DEMAND",
  "region": "ASIA",
  "periodKeys": ["202601", "202602"],
  "rows": [
    {
      "country": null,
      "attrType": "TOTAL",
      "values": [123.45, 124.12]
    },
    {
      "country": "China",
      "attrType": "TOTAL",
      "values": [15.1, 15.4]
    }
  ]
}
```

`values[i]` corresponds to `periodKeys[i]`. `periodKeys` is therefore load-bearing (the only source of period identity/order) and must be present and complete.

Derived fields (computed by the parser, not stored):

- Row kind: `country == null` → region subtotal; otherwise a country row. (Maps to `VolumesRowKind` `REGION_SUBTOTAL` / `COUNTRY` in the API DTO.)
- Row order: JSON `rows` array order is authoritative. `rows[0]` is the region subtotal; country rows follow in source order. (Planned LNG extension: each country would emit `TOTAL` then `CONTRACTED` then `UNCONTRACTED` consecutively.)
- Stable row key (if needed for assembly/dedup): reconstruct from `region` + `country` + `attrType`.

Validation rules:

- Top-level `segment`, `periodUnit`, `dataType`, and `region` must match the relational key columns.
- `periodKeys` must be non-empty and sorted ascending.
- `periodUnit = M`: every period key must match `YYYYMM` and month `01` through `12`.
- `periodUnit = Y`: every period key must match `YYYY`.
- Every `values` array must have exactly the same length as `periodKeys`; `values[i]` maps to `periodKeys[i]`.
- `rows[0]` must be the region subtotal row (`country == null`) for deterministic response assembly.
- Country rows must have non-null `country`.
- `attrType` is currently `TOTAL` only for all loaded data (GLOBAL and LNG alike).
- `CONTRACTED` / `UNCONTRACTED` are a **planned extension** for `sgmt_cd = LNG` and `data_type_cd IN (LNG_IMPORTS, LNG_EXPORTS)`. When added, each country emits `TOTAL` then `CONTRACTED` then `UNCONTRACTED` consecutively, and a country's `TOTAL` should equal `CONTRACTED + UNCONTRACTED` per period (when both breakdown rows exist).
- The region subtotal row uses `attrType = TOTAL` only (no contracted/uncontracted split at region level).

## Query Shape

```sql
SELECT data_type_cd, rgn_cd, json_schm_ver, vol_data_json
FROM tmd_mdl_scnrcasevolrgnl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = :segment
  AND perd_unit_spt_cd = :period_unit
  AND data_type_cd IN (:types)
  AND rgn_cd IN (:regions)
ORDER BY data_type_cd, rgn_cd;
```

Service assembly:

1. Resolve `caseId -> latest completed run_seq` using the existing case/run rules.
2. Query selected type/region buckets.
3. Build columns from JSON `periodKeys`.
4. For each selected type and region, emit rows in JSON `rows` array order: the region subtotal (`country == null`) first, then country rows. Derive `rowKind` from `country` and map `attrType` straight through.
5. Compute `TYPE_TOTAL` by summing selected regions' region-subtotal values per period.
6. Return rows in feature-spec order.

## ERD Review

### PK / UK / FK

- PK: `vol_rgn_seq` surrogate key.
- FK: `run_seq -> tmd_mdl_scnrcaseh.run_seq`.
- UK: `(run_seq, sgmt_cd, perd_unit_spt_cd, data_type_cd, rgn_cd)` prevents duplicate buckets.

### Index

- The UK is enough for exact key lookup and range over selected `data_type_cd` / `rgn_cd`.
- Avoid standalone indexes on `sgmt_cd`, `perd_unit_spt_cd`, `data_type_cd`, `rgn_cd`, or `del_yn`.
- If the table becomes large and MySQL does not use `ux01` well for `IN` predicates, add only one DBA-approved secondary index after `EXPLAIN`.

### Normalization

This design intentionally violates 1NF because `vol_data_json` contains repeated row and period groups. The denormalization is justified only by:

- read-only API behavior,
- no DB-side filtering below region,
- high insert volume in the atomic-row design,
- region-level retrieval matching the smallest API filter unit.

2NF/3NF/BCNF are not fully applicable inside the JSON payload. Relational determinant columns still identify one bucket.

### Compatibility

- API response shape does not change.
- Feature spec validation for `segment`, `types`, `regions`, and `granularity` does not change.
- Existing Java DTOs can remain unchanged.
- Repository/mapper code changes from reading many atomic rows to reading bucket JSON rows and parsing payloads.

## Rollout / Compatibility Review

This is a redesign candidate, not a null-safe ALTER of the current approved table.

Recommended rollout if approved:

1. Create `tmd_mdl_scnrcasevolrgnl`.
2. Update Python/model loader to insert region buckets instead of atomic Volumes rows.
3. Add loader validation for JSON key alignment, row count, period count, and hash.
4. Update Java DB-backed Volumes query to parse region buckets.
5. During first rollout, optionally dual-load atomic rows and region buckets for one validation run only, then compare aggregates.
6. Switch canonical design document from atomic `tmd_mdl_scnrcasevoll` to region-bucket JSON only after validation.

No backfill is required unless historical completed runs must be served from DB. If backfill is required, create a separate import script and work note.

## DDL Draft Structure Recommendation

When this candidate becomes approved, produce DBA handoff SQL with:

- pre-check:
  - `SHOW TABLES LIKE 'tmd_mdl_scnrcasevolrgnl';`
  - `SHOW CREATE TABLE tmd_mdl_scnrcaseh;`
- apply:
  - create table
  - create only non-redundant indexes
- backfill:
  - explicit "not needed" or one-off import plan
- rollback:
  - drop candidate table before application switch, or stop writes and retain table for investigation after application switch
- verify:
  - bucket count by `run_seq`, `sgmt_cd`, `perd_unit_spt_cd`, `data_type_cd`
  - JSON validity and schema version count
  - `values` array length equals `periodKeys` length for every row; `periodKeys` length matches expected period count
  - subtotal aggregate comparison against source output (LNG `TOTAL = CONTRACTED + UNCONTRACTED` check applies once the breakdown extension is loaded)
- DBA action / deployment note:
  - confirm JSON column policy and maximum row size expectations

## Risks / Open Questions

- JSON payload makes cell-level DBA SQL inspection harder.
- A malformed JSON bucket can invalidate a whole region/type/granularity response.
- MySQL JSON binary storage still occupies significant row space; large regions need payload-size checks.
- Java parsing moves more validation responsibility into application code.
- If future APIs add country search, country-level filtering, or value threshold filtering, this design becomes a poor fit.
- If future API exposes `attrType` as a filter, service must filter inside JSON after bucket retrieval.

## Recommendation

Proceed with this Plan B as a team-review candidate.

The design is reasonable under the confirmed constraints:

- API is read-only.
- Filter floor is region.
- Country-level DB filtering/search is not required.
- `perd_key`, `attr_type`, and `vol_val` are not SQL filter dimensions.
- Observed atomic-row insert volume exceeds 20,000 rows for one `GLOBAL` monthly run slice.

Do not implement until BE/DBA agree whether JSON bucket storage can replace `tmd_mdl_scnrcasevoll` as the canonical Volumes result table.

## Dictionary Updates

No dictionary update was made.

Terms already present and used:

- `물량` / `VOL`
- `지역` / `RGN`
- `JSON` / `JSON`
- `스키마버전` / `SCHMVER`
- `행` / `ROW`
- `기간` / `PERD`
- `수` / `CNT`
- `해시` / `HASH`
- `데이터` / `DATA`

## Test Data

Region-bucket test data was generated from `06120948_result_output_dashboard 2.xlsx` for `run_seq = 2`, using the slim row form (`country`, `attrType`, `values` only):

- `docs/tmd_mdl_scnrcasevolrgnl_global_yearly_testdata.sql` — `GLOBAL` / `Y`, 6 types × 8 regions = 48 buckets, periods `2026`–`2030`.
- `docs/tmd_mdl_scnrcasevolrgnl_global_monthly_testdata.sql` — `GLOBAL` / `M`, 48 buckets, periods `202601`–`203012` (60 months). Per-year `*-NA` annual columns in the sheet are excluded.
- `docs/tmd_mdl_scnrcasevolrgnl_lng_yearly_testdata.sql` — `LNG` / `Y`, 2 types (`LNG_IMPORTS`, `LNG_EXPORTS`) × 8 regions = 16 buckets, per-country `TOTAL` only. (`CONTRACTED` / `UNCONTRACTED` breakdown is a planned extension, not yet loaded.)

Each file is idempotent: it first deletes its own `run_seq + sgmt_cd + perd_unit_spt_cd` slice before inserting, so the three files coexist in the table without unique-key conflict.

## Implementation Readiness

`blocked by open questions`

Open questions are limited to team/DBA approval of JSON canonical storage, final JSON schema, and loader validation ownership. API behavior is not blocked.
