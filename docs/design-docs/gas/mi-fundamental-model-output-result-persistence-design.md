# MI Fundamental Model - Output Result Persistence Design

## Document Control

- status: Approved
- canonical for: Business rules and schema rollout draft for MI Fundamental Model output result data persistence
- owner: Shared BE/DBA
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
- depends on:
  - `tmd_scncasem.case_id`
  - `tmd_scncaseh.run_id`
  - `tmd_scncaseh.run_seq`
  - `tcm_filel.file_rel_obj_spt_cd = 'SCNCASEH'`
  - API response DTO contract in `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
- supersedes:
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md` as canonical Trade Flow persistence design
  - "DB-backed 전환 시 별도 ERD review 필요" notes for Volumes/Trade Flows/Price result data in older drafts
- last synced: 2026-06-07
- open questions:
  - DBA physical rollout window and target schema name
  - final numeric precision if model output confirms a narrower or wider requirement than `DECIMAL(30, 18)`
  - DBA confirmation of the initial no-partition recommendation or a documented row-count-based partition override

## Purpose

이 문서는 MI Fundamental Model 실행 output 파일에서 추출되는 결과 수치 데이터의 DB persistence source-of-truth다.

API path, request/response field, validation, error mapping은 feature spec이 canonical source다. 이 문서는 해당 API 응답을 DB-backed로 전환할 때 필요한 table, column, key, index, partition, rollout, mock-to-real boundary만 확정한다.

## Source-of-Truth Boundaries

| Fact type | Canonical document |
|---|---|
| Endpoint/request/response/error contract | `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md` |
| Case/run lifecycle, callback, timeout, duplicate-run guard | `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md` |
| Case/run table rollout (`tmd_scncasem`, `tmd_scncaseh`) | `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md` |
| Output result data tables, keys, indexes, partition/readiness | this document |
| Raw output file metadata linkage | `tcm_filel` via `file_rel_obj_spt_cd = 'SCNCASEH'`, documented in the case execution DDL draft |

## Fixed Decisions

1. 결과 데이터는 `case_id`가 아니라 `tmd_scncaseh.run_seq`에 귀속한다.
   - `case_id`는 사용자 화면 resource identity다.
   - `run_seq`는 model execution 1회와 output file/result snapshot 1회를 식별한다.
   - 재실행 시 이전 run의 output file과 parsed result row는 보존한다.

2. raw output file과 parsed result data를 분리한다.
   - raw output file metadata: `tcm_filel(file_rel_obj_spt_cd = 'SCNCASEH', file_rel_obj_dstg_no = run_seq)`
   - parsed Volumes rows: `tmd_scncaseh_vol_l`
   - parsed Trade Flow header axis rows: `tmd_scncaseh_trd_hdr_l`
   - parsed Trade Flow sparse flow rows: `tmd_scncaseh_trd_flw_l`
   - parsed Price rows: `tmd_scncaseh_prc_l`

3. `COMPLETED` 전이는 `tmd_scncaseh.exec_stat_cd`가 canonical 상태다.
   - Python/model side가 result table insert를 commit한 뒤 backend callback을 호출한다.
   - Java/backend는 callback 성공 처리에서 `tmd_scncaseh.exec_stat_cd`와 `tmd_scncasem.case_stat_cd`를 `COMPLETED`로 전이한다.
   - Python/model side가 실패 callback을 호출하면 Java/backend는 `exec_stat_cd`와 case 최신 상태를 `FAILED`로 전이한다.
   - 별도 result load master/status table은 두지 않는다.
   - backend는 `COMPLETED` callback 처리 전 `run_seq` 기준 result row 존재 여부를 검증해야 한다. 검증 실패 시 `COMPLETED`로 바꾸지 않는다.

4. 결과 row는 immutable snapshot으로 본다.
   - 같은 `run_seq`에 대해 전체 result를 재적재하려면 먼저 기존 result rows를 `del_yn = 'Y'` 처리하거나 DBA 승인 하에 delete 후 재insert한다.
   - 일반 API 조회는 항상 `del_yn = 'N'` 결과만 읽는다.

5. Trade Flow header는 JSON 컬럼이 아니라 row table로 저장한다.
   - 기존 trade flow draft의 JSON header 후보는 superseded다.
   - DBA JSON 정책 미확정 리스크를 제거하고, header key/label/kind/order/filter를 인덱스와 검증 SQL로 확인 가능하게 한다.

6. Price annual/monthly 값은 모두 저장한다.
   - annual 값은 monthly 값에서 자동 집계한다고 가정하지 않는다.
   - source output이 annual table과 monthly table을 별도로 제공하므로 `granularity_cd`로 구분해 저장한다.

## Concept Model

```text
tmd_scncasem (case)
  └── tmd_scncaseh (run, run_seq)
        ├── tcm_filel (SCNCASEH + run_seq, raw output files)
        ├── tmd_scncaseh_vol_l (Volumes monthly atomic rows)
        ├── tmd_scncaseh_trd_hdr_l (Trade Flow FROM/TO axis headers)
        ├── tmd_scncaseh_trd_flw_l (Trade Flow sparse non-zero values)
        └── tmd_scncaseh_prc_l (Price annual/monthly rows)
```

## Case / Run Lookup Rules

`GET /cases/{caseId}/volumes`, `GET /cases/{caseId}/volumes/trade`, `GET /cases/{caseId}/price`는 feature spec대로 `caseId`를 받지만 DB는 최신 run의 result를 읽는다.

Recommended lookup flow:

1. `tmd_scncasem`에서 `case_id = :case_id`, `usr_no = :usr_no`, `del_yn = 'N'`을 조회한다.
2. `case_stat_cd = 'COMPLETED'`이고 `last_run_id IS NOT NULL`인지 확인한다.
3. `tmd_scncaseh.run_id = tmd_scncasem.last_run_id`로 latest run을 조회한다.
4. latest run의 `exec_stat_cd = 'COMPLETED'`인지 확인한다.
5. latest run의 `run_seq`로 각 result table을 조회한다.
6. result row가 없으면 DB-backed 전환 시점의 API는 data integrity mismatch로 보고 `409 BIZ_RULE_VIOLATION` 또는 운영 표준 오류로 처리한다. 정상 운영에서는 Python insert commit 후 callback이 호출되므로 이 상태가 노출되지 않아야 한다.

Historical run result 조회가 필요하면 `runId` 기반 API를 별도 feature spec으로 추가한다. 이 문서는 `caseId -> latest run -> result`만 canonical 범위로 둔다.

## Canonical Table Set

### `tmd_scncaseh_vol_l`

Volumes Global/LNG monthly atomic rows. YEARLY API 응답은 `MONTHLY` rows를 조회 시 aggregate한다.

```sql
CREATE TABLE tmd_scncaseh_vol_l
(
    vol_seq         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '물량순번'
        PRIMARY KEY,
    run_seq         BIGINT UNSIGNED                       NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    sgmt_cd         VARCHAR(20)                           NOT NULL COMMENT '세그먼트코드 (GLOBAL/LNG)',
    data_type_cd    VARCHAR(30)                           NOT NULL COMMENT '데이터타입코드',
    row_kind_cd     VARCHAR(30)                           NOT NULL COMMENT '행구분코드',
    row_key         VARCHAR(220)                          NOT NULL COMMENT '행키값',
    rgn_cd          VARCHAR(50)                           NULL COMMENT '지역코드',
    nat_nm          VARCHAR(120)                          NULL COMMENT '국가명',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    base_mnth       TINYINT UNSIGNED                      NOT NULL COMMENT '기준월',
    vol_val         DECIMAL(30, 18)                       NOT NULL COMMENT '물량값',
    CONSTRAINT tmd_scncaseh_vol_l_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_scncaseh (run_seq),
    CONSTRAINT tmd_scncaseh_vol_l_ux01
        UNIQUE (run_seq, sgmt_cd, data_type_cd, row_key, base_year, base_mnth)
)
    COMMENT 'TMD_시나리오케이스실행물량내역';

CREATE INDEX tmd_scncaseh_vol_l_ix01
    ON tmd_scncaseh_vol_l (run_seq, sgmt_cd, data_type_cd, rgn_cd, base_year, base_mnth);
```

Rules:

- `sgmt_cd = 'TRADE'` is not allowed in this table. Trade Flows use dedicated tables.
- `row_key` is non-null to avoid MySQL unique-key ambiguity with nullable `rgn_cd`/`nat_nm`.
- Recommended `row_key` examples:
  - `COUNTRY:ASIA:China`
  - `REGION_SUBTOTAL:ASIA`
  - `TYPE_TOTAL`
- `TYPE_TOTAL` may be stored if the source output provides it. If not stored, service may derive it from `REGION_SUBTOTAL` rows. The storage decision must be consistent per `run_seq`.

### `tmd_scncaseh_trd_hdr_l`

Trade Flow matrix axis headers. FROM/TO axes are both row records.

```sql
CREATE TABLE tmd_scncaseh_trd_hdr_l
(
    trd_hdr_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름헤더순번'
        PRIMARY KEY,
    run_seq         BIGINT UNSIGNED                       NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    selector_cd     VARCHAR(10)                           NOT NULL COMMENT '선택코드 (LNG/PNG)',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    axis_cd         VARCHAR(10)                           NOT NULL COMMENT '축코드 (FROM/TO)',
    hdr_key         VARCHAR(220)                          NOT NULL COMMENT '헤더키값',
    hdr_lbl_nm      VARCHAR(200)                          NOT NULL COMMENT '헤더라벨명',
    hdr_kind_cd     VARCHAR(30)                           NOT NULL COMMENT '헤더구분코드',
    rgn_cd          VARCHAR(50)                           NULL COMMENT '지역코드',
    nat_nm          VARCHAR(120)                          NULL COMMENT '국가명',
    hdr_ord         INT UNSIGNED                          NOT NULL COMMENT '헤더순서',
    CONSTRAINT tmd_scncaseh_trd_hdr_l_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_scncaseh (run_seq),
    CONSTRAINT tmd_scncaseh_trd_hdr_l_ux01
        UNIQUE (run_seq, selector_cd, base_year, axis_cd, hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름헤더내역';

CREATE INDEX tmd_scncaseh_trd_hdr_l_ix01
    ON tmd_scncaseh_trd_hdr_l (run_seq, selector_cd, base_year, axis_cd, hdr_ord);
```

Rules:

- `axis_cd`: `FROM`, `TO`
- `hdr_kind_cd`: `REGION_TOTAL`, `COUNTRY`, `GRAND_TOTAL`
- `hdr_key` maps directly to API `fromCountries[].key`, `toCountries[].key`, `flows[].fromKey`, `flows[].toKey`.
- `hdr_ord` preserves FE display order from the output file.
- `GRAND_TOTAL` is valid only for the TO axis unless a future API contract adds FROM total.

### `tmd_scncaseh_trd_flw_l`

Trade Flow sparse non-zero matrix values.

```sql
CREATE TABLE tmd_scncaseh_trd_flw_l
(
    trd_flw_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름순번'
        PRIMARY KEY,
    run_seq         BIGINT UNSIGNED                       NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    selector_cd     VARCHAR(10)                           NOT NULL COMMENT '선택코드 (LNG/PNG)',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    from_hdr_key    VARCHAR(220)                          NOT NULL COMMENT 'From 헤더키값',
    to_hdr_key      VARCHAR(220)                          NOT NULL COMMENT 'To 헤더키값',
    from_rgn_cd     VARCHAR(50)                           NULL COMMENT 'From 지역코드',
    from_nat_nm     VARCHAR(120)                          NULL COMMENT 'From 국가명',
    to_rgn_cd       VARCHAR(50)                           NULL COMMENT 'To 지역코드',
    to_nat_nm       VARCHAR(120)                          NULL COMMENT 'To 국가명',
    flow_val        DECIMAL(30, 18)                       NOT NULL COMMENT '흐름값 (MTPA 기준)',
    CONSTRAINT tmd_scncaseh_trd_flw_l_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_scncaseh (run_seq),
    CONSTRAINT tmd_scncaseh_trd_flw_l_ux01
        UNIQUE (run_seq, selector_cd, base_year, from_hdr_key, to_hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름내역';

CREATE INDEX tmd_scncaseh_trd_flw_l_ix01
    ON tmd_scncaseh_trd_flw_l (run_seq, selector_cd, base_year);

CREATE INDEX tmd_scncaseh_trd_flw_l_ix02
    ON tmd_scncaseh_trd_flw_l (run_seq, selector_cd, base_year, to_rgn_cd);

CREATE INDEX tmd_scncaseh_trd_flw_l_ix03
    ON tmd_scncaseh_trd_flw_l (run_seq, selector_cd, base_year, from_rgn_cd);
```

Rules:

- Store MTPA only. API `unit=BCM` conversion is query/service logic.
- Store only non-zero values. Missing cells are interpreted as `0` by the API/FE contract.
- `from_hdr_key` and `to_hdr_key` must exist in `tmd_scncaseh_trd_hdr_l` for the same `run_seq`, `selector_cd`, and `base_year`. Enforce by application validation or by DBA-approved composite FK if rollout complexity is acceptable.
- Region columns are duplicated for filter performance and operational inspection. Header rows remain canonical for display label/order.

### `tmd_scncaseh_prc_l`

Price annual/monthly table rows. Chart response is derived from monthly rows.

```sql
CREATE TABLE tmd_scncaseh_prc_l
(
    prc_seq         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '가격순번'
        PRIMARY KEY,
    run_seq         BIGINT UNSIGNED                       NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    granularity_cd  VARCHAR(10)                           NOT NULL COMMENT '단위코드 (MONTHLY/YEARLY)',
    period_key      VARCHAR(6)                            NOT NULL COMMENT '기간키값 (YYYY 또는 YYYYMM)',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    base_mnth       TINYINT UNSIGNED                      NULL COMMENT '기준월',
    prc_rgn_nm      VARCHAR(200)                          NOT NULL COMMENT '가격지역명',
    base_case_prc   DECIMAL(30, 18)                       NOT NULL COMMENT '베이스케이스가격',
    user_adj_prc    DECIMAL(30, 18)                       NOT NULL COMMENT '사용자조정가격',
    CONSTRAINT tmd_scncaseh_prc_l_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_scncaseh (run_seq),
    CONSTRAINT tmd_scncaseh_prc_l_ux01
        UNIQUE (run_seq, granularity_cd, prc_rgn_nm, period_key)
)
    COMMENT 'TMD_시나리오케이스실행가격내역';

CREATE INDEX tmd_scncaseh_prc_l_ix01
    ON tmd_scncaseh_prc_l (run_seq, granularity_cd, prc_rgn_nm, base_year, base_mnth);
```

Rules:

- `granularity_cd = 'MONTHLY'`: `period_key = YYYYMM`, `base_mnth` is 1-12.
- `granularity_cd = 'YEARLY'`: `period_key = YYYY`, `base_mnth IS NULL`.
- `prc_rgn_nm` is dynamic output data, not a fixed enum.
- `BASE_CASE` and `LAST_ADJUSTED` API series labels are derived from `base_case_prc` and `user_adj_prc`.

## Index and Partition Strategy

### Initial Index Strategy

| Table | Main query | Required index |
|---|---|---|
| `tmd_scncaseh_vol_l` | run + segment + type/region + period order | `ix01 (run_seq, sgmt_cd, data_type_cd, rgn_cd, base_year, base_mnth)` |
| `tmd_scncaseh_trd_hdr_l` | run + selector + year + axis display order | `ix01 (run_seq, selector_cd, base_year, axis_cd, hdr_ord)` |
| `tmd_scncaseh_trd_flw_l` | run + selector + year sparse matrix | `ix01 (run_seq, selector_cd, base_year)` |
| `tmd_scncaseh_trd_flw_l` | TO region filter | `ix02 (run_seq, selector_cd, base_year, to_rgn_cd)` |
| `tmd_scncaseh_trd_flw_l` | FROM region filter | `ix03 (run_seq, selector_cd, base_year, from_rgn_cd)` |
| `tmd_scncaseh_prc_l` | run + granularity + price region + period order | `ix01 (run_seq, granularity_cd, prc_rgn_nm, base_year, base_mnth)` |

Low-cardinality columns such as `del_yn`, status, selector, segment, and region are not indexed alone. They are only used after `run_seq` or similarly selective columns.

### Partition Decision

Initial rollout recommendation: do not physically partition on the first DB-backed release unless DBA row-count estimation shows the result tables will exceed normal operational thresholds.

Reason:

- Current API always resolves to one latest `run_seq` before reading result rows.
- Current Trade Flow year range is 2023-2030.
- `run_seq` is the most selective access key.
- Physical partitioning can complicate FK constraints and rollback operations depending on target MySQL version and DBA policy.

Partition-ready rule:

- If DBA chooses partitioning, use RANGE partitioning by `base_year` on large row tables only:
  - `tmd_scncaseh_vol_l`
  - `tmd_scncaseh_trd_hdr_l`
  - `tmd_scncaseh_trd_flw_l`
  - `tmd_scncaseh_prc_l`
- Include `base_year` in every unique key for partitioned tables.
- Re-check target MySQL support for FK constraints on partitioned InnoDB tables. If unsupported or operationally risky, keep logical references in application validation and omit physical FK constraints on partitioned child row tables.
- Use partition names like `ptr_2023`, `ptr_2024`, ..., `ptr_2030`, `ptr_max`.

Candidate partition clause for DBA review:

```sql
PARTITION BY RANGE COLUMNS (base_year)
(
    PARTITION ptr_2023 VALUES LESS THAN (2024),
    PARTITION ptr_2024 VALUES LESS THAN (2025),
    PARTITION ptr_2025 VALUES LESS THAN (2026),
    PARTITION ptr_2026 VALUES LESS THAN (2027),
    PARTITION ptr_2027 VALUES LESS THAN (2028),
    PARTITION ptr_2028 VALUES LESS THAN (2029),
    PARTITION ptr_2029 VALUES LESS THAN (2030),
    PARTITION ptr_2030 VALUES LESS THAN (2031),
    PARTITION ptr_max VALUES LESS THAN (MAXVALUE)
);
```

## Query Shapes

### Volumes

```sql
SELECT data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm,
       base_year, base_mnth, vol_val
FROM tmd_scncaseh_vol_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = :segment
  AND data_type_cd IN (:types)
  AND (rgn_cd IN (:regions) OR row_kind_cd = 'TYPE_TOTAL')
ORDER BY data_type_cd, row_kind_cd, rgn_cd, nat_nm, base_year, base_mnth;
```

### Trade Flows

```sql
SELECT axis_cd, hdr_key, hdr_lbl_nm, hdr_kind_cd, rgn_cd, nat_nm, hdr_ord
FROM tmd_scncaseh_trd_hdr_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = :selector
  AND base_year = :year
ORDER BY axis_cd, hdr_ord;

SELECT from_hdr_key, to_hdr_key,
       from_rgn_cd, from_nat_nm, to_rgn_cd, to_nat_nm, flow_val
FROM tmd_scncaseh_trd_flw_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = :selector
  AND base_year = :year;
```

Region filter:

- `types=TO_COUNTRY`: add `AND (to_rgn_cd IN (:regions) OR to_hdr_key = 'TOTAL')`
- `types=FROM_COUNTRY`: add `AND from_rgn_cd IN (:regions)`
- both directions: apply both conditions with API-defined semantics.

### Price

```sql
SELECT granularity_cd, period_key, base_year, base_mnth,
       prc_rgn_nm, base_case_prc, user_adj_prc
FROM tmd_scncaseh_prc_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
ORDER BY granularity_cd, prc_rgn_nm, base_year, base_mnth;
```

## Normalization Assessment

| Table | Assessment |
|---|---|
| `tmd_scncaseh_vol_l` | Atomic monthly value per run/segment/type/row/period. `row_key` prevents nullable unique-key ambiguity. |
| `tmd_scncaseh_trd_hdr_l` | Header metadata normalized as rows instead of JSON arrays. Header order/label/kind are axis attributes. |
| `tmd_scncaseh_trd_flw_l` | Sparse flow values separated from header metadata. Region/name columns are intentional denormalization for filter speed and DBA inspection. |
| `tmd_scncaseh_prc_l` | Price values are atomic by run/granularity/region/period. Annual and monthly rows are not mixed by nullable month because `period_key` is non-null. |

Intentional denormalization:

- Trade Flow `from_*`/`to_*` region/name columns duplicate header metadata to avoid repeated joins for region filtering and operational checks.
- Price stores base and adjusted values in one row because the API always returns both series together for the same period/region.

## Rollout Draft Structure

### Pre-Check

```sql
SHOW CREATE TABLE tmd_scncaseh;
SHOW INDEX FROM tmd_scncaseh;

SHOW TABLES LIKE 'tmd_scncaseh_vol_l';
SHOW TABLES LIKE 'tmd_scncaseh_trd_hdr_l';
SHOW TABLES LIKE 'tmd_scncaseh_trd_flw_l';
SHOW TABLES LIKE 'tmd_scncaseh_prc_l';

SHOW INDEX FROM tcm_filel;

SELECT cd_grp_id, cd_id, cd_nm, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNCASEH';
```

Expected:

- `tmd_scncaseh.run_seq` exists and is unique.
- `SCNCASEH` is registered or ready to register as defined by the case execution DDL draft.
- None of the new result table names already exist.

### Apply

Apply order:

1. `tmd_scncaseh_vol_l`
2. `tmd_scncaseh_trd_hdr_l`
3. `tmd_scncaseh_trd_flw_l`
4. `tmd_scncaseh_prc_l`
5. Secondary indexes
6. Optional partitions only if DBA approves physical partition rollout

### Backfill

No automatic backfill is included in the canonical rollout.

If mock JSON or historical output files must be imported, create a separate one-off import work note and script with:

- source file list
- target `run_seq`
- row counts by table
- checksum or aggregate validation
- rollback SQL

### Rollback

Rollback order:

```sql
DROP TABLE tmd_scncaseh_prc_l;
DROP TABLE tmd_scncaseh_trd_flw_l;
DROP TABLE tmd_scncaseh_trd_hdr_l;
DROP TABLE tmd_scncaseh_vol_l;
```

Rollback rules:

- Do not delete raw output files or `tcm_filel` rows as part of result table rollback unless storage owner explicitly approves.
- If application has already shipped DB-backed result APIs, prefer forward-fix over dropping tables.
- If partitioned tables are used, DBA must verify partition metadata and FK behavior before rollback.

### Verify / EXPLAIN

Required verification after DDL and before enabling real result APIs:

```sql
EXPLAIN
SELECT *
FROM tmd_scncaseh_vol_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = 'GLOBAL'
  AND data_type_cd IN ('DEMAND')
  AND rgn_cd IN ('ASIA')
ORDER BY data_type_cd, row_kind_cd, rgn_cd, nat_nm, base_year, base_mnth;

EXPLAIN
SELECT *
FROM tmd_scncaseh_trd_hdr_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = 'LNG'
  AND base_year = 2026
ORDER BY axis_cd, hdr_ord;

EXPLAIN
SELECT *
FROM tmd_scncaseh_trd_flw_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = 'LNG'
  AND base_year = 2026
  AND to_rgn_cd = 'ASIA';

EXPLAIN
SELECT *
FROM tmd_scncaseh_prc_l
WHERE del_yn = 'N'
  AND run_seq = :run_seq
ORDER BY granularity_cd, prc_rgn_nm, base_year, base_mnth;
```

Expected:

- `type` is not `ALL`.
- `key` uses the intended table-specific index.
- Estimated rows are bounded by one run's result size.

Data verification:

```sql
SELECT COUNT(*) FROM tmd_scncaseh_vol_l WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_scncaseh_trd_hdr_l WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_scncaseh_trd_flw_l WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_scncaseh_prc_l WHERE run_seq = :run_seq AND del_yn = 'N';
```

## Mock to Real Transition Boundary

Phase 1 mock:

- `src/main/resources/mock/fundamental-model/fundamental-model-volumes-data.json`
- `src/main/resources/mock/fundamental-model/fundamental-model-trade-flows-data.json`
- `src/main/resources/mock/fundamental-model/fundamental-model-price-data.json`
- no DB writes
- no DBA rollout

DB-backed real:

- result API flags in `FundamentalModelMockProperties` switch from mock service to real service per API.
- real service must not read mock JSON.
- real service reads only latest completed run result tables through `caseId -> last_run_id -> run_seq`.
- Python/model side must commit result table inserts before invoking success callback.
- Java/backend must not set `exec_stat_cd = 'COMPLETED'` unless callback validation and result row existence checks pass.
- `tcm_filel(SCNCASEH + run_seq)` remains the source for raw output file metadata, not for chart/table API rows.

Transition checklist:

1. Apply case/run DDL from `scenario-case-execution-erd-ddl-draft.md`.
2. Apply result tables from this document.
3. Implement result ingestion from model output files.
4. Verify row counts and EXPLAIN for one completed run.
5. Switch result API flags from mock to real in a controlled profile.
6. Keep mock seed files only for local/demo mode.

## DBA Action Items

- Confirm whether initial rollout uses no partition or RANGE partition by `base_year`.
- Confirm `DECIMAL(30, 18)` is acceptable for Volumes/Trade/Price values.
- Confirm physical FK policy for result row tables, especially if partitioning is enabled.
- Confirm secondary index count against insert volume and model output size.

## Implementation Readiness

ready after minor cleanup

- The data model, run linkage, key strategy, mock-to-real boundary, and DBA verification shape are fixed enough for implementation planning.
- Physical rollout remains blocked until DBA confirms partition/FK policy and numeric precision, but those decisions do not change the API contract or case/run ownership model.
