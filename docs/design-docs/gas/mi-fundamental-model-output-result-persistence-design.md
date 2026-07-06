# MI Fundamental Model - Output Result Persistence Design

## Document Control

- status: Approved
- canonical for: Business rules and schema rollout draft for MI Fundamental Model output result data persistence
- owner: Shared BE/DBA
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `docs/design-docs/gas/mi-fundamental-model-volumes-region-bucket-json-plan-b.md`
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
- depends on:
  - `tmd_mdl_scnrcasem.case_id`
  - `tmd_mdl_scnrcaseh.run_id`
  - `tmd_mdl_scnrcaseh.run_seq`
  - `tcm_filel.file_rel_obj_spt_cd = 'SCNRCASEH'`
  - API response DTO contract in `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
- supersedes:
  - `docs/design-docs/gas/mi-fundamental-model-volumes-erd-ddl-draft.md` as canonical Volumes persistence design
  - `docs/design-docs/gas/mi-fundamental-model-volumes-region-bucket-json-plan-b.md` as a separate Plan B candidate; the approved `tmd_mdl_scnrcasevolrgnl` decision is folded into this canonical document
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md` as canonical Trade Flow persistence design
  - "DB-backed 전환 시 별도 ERD review 필요" notes for Volumes/Trade Flows/Price result data in older drafts
- last synced: 2026-07-03
- implementation status: physical rollout complete (real DB implementation shipped 2026-06-15 for Volumes/Trade Flows/Price; see `docs/work-notes/2026/2026-06-15-*-real-db-implementation.md`). Remaining open questions below are follow-up DBA policy items, not rollout blockers.
- open questions:
  - result retention policy: keep latest N runs per case, keep runs for M months, or archive by explicit DBA operation
  - DBA confirmation of future partition objective: query pruning only vs retention/archive management (initial no-partition rollout is live)
  - Price 물리명 사전 정합성 (2026-06-11 ERD review, 해소됨): `granularity_cd` → `perd_unit_spt_cd`(기간단위구분코드, enum `PeriodUnitSeparateCd`) 재사용, `period_key` → `perd_key`(표준 약어 `PERD` + 기존 `_key` 패밀리), `user_adj_prc`의 `조정/ADJ`는 용어사전(`db-term-dictionary-sheet2.csv` id 380)에 등록 완료. Price 결과테이블 물리명은 모두 사전 정합.
  - Price series 라벨 단일화 (2026-06-11 ERD review, 해소됨): API series 코드는 `BASE_CASE` / `USER_ADJUSTED`로 통일했다. 코드 enum `PriceSeriesType`·mock seed에 맞춰 feature spec의 `LAST_ADJUSTED` 표기(토큰 8곳 + 라벨 문자열 3곳)를 `USER_ADJUSTED` / `User adjusted price`로 정렬 완료.
  - Price `unit` 상수 필드 (2026-06-15 해소): Price 응답은 `unit: "USD/mmbtu"` 고정 상수 필드를 반환한다. ERD는 단위 컬럼을 두지 않고 API 조립 시 상수로 채운다.

## Purpose

이 문서는 MI Fundamental Model 실행 output 파일에서 추출되는 결과 수치 데이터의 DB persistence source-of-truth다.

API path, request/response field, validation, error mapping은 feature spec이 canonical source다. 이 문서는 해당 API 응답을 DB-backed로 전환할 때 필요한 table, column, key, index, partition, rollout, mock-to-real boundary만 확정한다.

## Source-of-Truth Boundaries

| Fact type | Canonical document |
|---|---|
| Endpoint/request/response/error contract | `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md` |
| Case/run lifecycle, callback, timeout, duplicate-run guard | `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md` |
| Case/run table rollout (`tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`) | `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md` |
| Output result data tables, keys, indexes, partition/readiness | this document |
| Raw output file metadata linkage | `tcm_filel` via `file_rel_obj_spt_cd = 'SCNRCASEH'`, documented in the case execution DDL draft |

## Fixed Decisions

1. 결과 데이터는 `case_id`가 아니라 `tmd_mdl_scnrcaseh.run_seq`에 귀속한다.
   - `case_id`는 사용자 화면 resource identity다.
   - `run_seq`는 model execution 1회와 output file/result snapshot 1회를 식별한다.
   - 재실행 시 이전 run의 output file과 parsed result row는 보존한다.

2. raw output file과 parsed result data를 분리한다.
   - raw output file metadata: `tcm_filel(file_rel_obj_spt_cd = 'SCNRCASEH', file_rel_obj_dstg_no = run_seq)`
   - parsed Volumes region-bucket rows: `tmd_mdl_scnrcasevolrgnl`
   - parsed Trade Flow header axis rows: `tmd_mdl_scnrcasetrdehdrl`
   - parsed Trade Flow sparse flow rows: `tmd_mdl_scnrcasetrdeflwl`
   - parsed Price rows: `tmd_mdl_scnrcaseprcl`

3. `COMPLETED` 전이는 `tmd_mdl_scnrcaseh.exec_stat_cd`가 canonical 상태다.
   - Python/model side가 result table insert를 commit한 뒤 backend callback을 호출한다.
   - Java/backend는 callback 성공 처리에서 `tmd_mdl_scnrcaseh.exec_stat_cd`와 `tmd_mdl_scnrcasem.case_stat_cd`를 `COMPLETED`로 전이한다.
   - Python/model side가 실패 callback을 호출하면 Java/backend는 `exec_stat_cd`와 case 최신 상태를 `FAILED`로 전이한다.
   - 별도 result load master/status table은 두지 않는다.
   - backend는 `COMPLETED` callback 처리 전 `run_seq` 기준 result row 존재 여부를 검증해야 한다. 검증 실패 시 `COMPLETED`로 바꾸지 않는다.

4. 결과 row는 immutable snapshot으로 본다.
   - 같은 `run_seq`에 대해 전체 result를 재적재하려면 DBA 승인 하에 기존 result rows를 hard-delete 후 재insert한다.
   - Soft-delete는 조회 제외/운영 격리 용도이며, 현재 unique key 구조에서는 same-key 재insert 수단으로 사용하지 않는다.
   - 일반 API 조회는 항상 `del_yn = 'N'` 결과만 읽는다.

5. Trade Flow header는 JSON 컬럼이 아니라 row table로 저장한다.
   - 기존 trade flow draft의 JSON header 후보는 superseded다.
   - DBA JSON 정책 미확정 리스크를 제거하고, header key/label/kind/order/filter를 인덱스와 검증 SQL로 확인 가능하게 한다.

6. Trade Flow는 연도별 matrix만 저장한다.
   - `tmd_mdl_scnrcasetrdehdrl`, `tmd_mdl_scnrcasetrdeflwl`은 `base_year`를 기간 컬럼으로 유지한다.
   - Trade Flow에는 `perd_unit_spt_cd`, `perd_key`를 추가하지 않는다.

7. Volumes와 Price annual/monthly 값은 모두 저장할 수 있다.
   - annual 값은 monthly 값에서 자동 집계한다고 가정하지 않는다.
   - source output이 annual table과 monthly table을 별도로 제공할 수 있으므로 `perd_unit_spt_cd`와 `perd_key`로 기간을 표현한다.
   - Volumes는 relational column `perd_unit_spt_cd`로 bucket granularity를 구분하고, 실제 기간 목록은 `vol_data_json.periodKeys`에 저장한다.
   - Price는 relational columns `perd_unit_spt_cd` + `perd_key`로 기간을 표현한다.
   - Price의 `perd_unit_spt_cd + perd_key`는 엄밀히는 의도적 중복이다. `perd_key = '2026'`이면 `Y`, `perd_key = '202601'`이면 `M`을 추론할 수 있지만, `base_mnth = 0` 같은 sentinel 방식보다 도메인 의미가 명확하므로 명시 컬럼을 둔다.
   - Price의 `perd_unit_spt_cd`는 조회 분류용이며, 기간 식별의 유일성은 `perd_key`가 담당한다. 따라서 Price UK에는 가능한 한 `perd_unit_spt_cd`를 넣지 않는다.

8. Volumes는 atomic row table `tmd_mdl_scnrcasevoll`을 사용하지 않고 region-bucket JSON table `tmd_mdl_scnrcasevolrgnl`에 저장한다.
   - 승인 결정일: 2026-06-17.
   - Python/model loader target table도 `tmd_mdl_scnrcasevolrgnl`로 협의 완료(2026-06-17).
   - 저장 단위는 `run_seq + sgmt_cd + perd_unit_spt_cd + data_type_cd + rgn_cd` 1 row다.
   - `vol_data_json`은 해당 region/type/granularity의 `periodKeys`와 row/value 배열을 담는다.
   - API filter floor가 region이고 country/value 단위 DB 검색이 필요 없다는 조건에 근거한 의도적 denormalization이다.
   - `TYPE_TOTAL`은 bucket마다 저장하지 않고 선택된 region subtotal을 서비스에서 합산한다.
   - `attrType`은 JSON row 내부 필드다. 현재 API는 `TOTAL`만 노출하므로 Java/backend는 `TOTAL` row만 응답에 사용하고, `CONTRACTED` / `UNCONTRACTED`는 향후 LNG breakdown 확장 시 JSON 내부에서 처리한다.

9. Price 단위는 `USD/mmbtu` 고정이며 `tmd_mdl_scnrcaseprcl`에 단위 컬럼을 저장하지 않는다.
   - 모델 output이 항상 USD/mmbtu 단위로 출력하는 불변 계약이다.
   - API response는 `unit: "USD/mmbtu"` 상수 필드로 반환한다.
   - 모델 output 스펙이 변경되어 단위가 run마다 달라지는 경우 `prc_unit_cd VARCHAR(20)` 컬럼 추가와 마이그레이션이 필요하다.

10. external reference sync:
   - 2026-06-10: Notion reference pages `결과 저장 테이블 초안`, `상세 컬럼표와 적재 규칙` 에 남아 있던 구 physical name `tmd_mdl_scncase*` / `scn_id` 는 superseded다.
   - canonical physical names are `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, `tmd_mdl_scnrcasevolrgnl`, `tmd_mdl_scnrcasetrdehdrl`, `tmd_mdl_scnrcasetrdeflwl`, `tmd_mdl_scnrcaseprcl`, and `scnr_id`.
   - naming authority는 `ScenarioCase` / `ScenarioCaseRunHistory` entity와 `scenario-case-execution-erd-ddl-draft.md` 의 rollout naming이다.
   - 2026-06-11: Notion pages `MI Fundamental Model`, `결과 저장 테이블 초안`, and `상세 컬럼표와 적재 규칙` were rechecked as external evidence.
   - Confirmed evidence: Python/model side stores parsed output rows under `run_seq`, commits result rows before callback, and does not require a separate result status table.
   - Superseded or narrowed evidence: Notion's remaining `base_year` partitioning question does not define the retention strategy. This document is canonical: initial rollout is no partition, and future retention/archive partitioning should use `run_seq` RANGE if approved.
   - Volumes atomic numeric precision is no longer a physical column decision because Volumes values are stored inside `tmd_mdl_scnrcasevolrgnl.vol_data_json`. Trade Flow 컬럼(`flow_val`)과 Price 컬럼(`base_case_prc` / `user_adj_prc`)은 2026-06-15 기준 모델 부동소수 산출물 보존을 위해 `DOUBLE`로 확정했다.

11. Result row accumulation is governed by `run_seq`.
   - Every completed model execution creates a new result snapshot under one `run_seq`.
   - Trade Flow `base_year`, Volumes `vol_data_json.periodKeys`, and Price `perd_key` are data period dimensions, not lifecycle or retention dimensions.
   - Retention/archive policy must be expressed in terms of runs, for example latest N runs per case or runs older than M months.
   - Until a retention policy is approved, result rows are preserved and no automated purge is assumed.

## Concept Model

```text
tmd_mdl_scnrcasem (case)
  └── tmd_mdl_scnrcaseh (run, run_seq)
        ├── tcm_filel (SCNRCASEH + run_seq, raw output files)
        ├── tmd_mdl_scnrcasevolrgnl (Volumes annual/monthly region-bucket JSON rows)
        ├── tmd_mdl_scnrcasetrdehdrl (Trade Flow FROM/TO axis headers)
        ├── tmd_mdl_scnrcasetrdeflwl (Trade Flow sparse non-zero values)
        └── tmd_mdl_scnrcaseprcl (Price annual/monthly rows)
```

## Case / Run Lookup Rules

`GET /cases/{caseId}/volumes`, `GET /cases/{caseId}/volumes/trade`, `GET /cases/{caseId}/price`는 feature spec대로 `caseId`를 받지만 DB는 최신 run의 result를 읽는다.

Recommended lookup flow:

1. `tmd_mdl_scnrcasem`에서 `case_id = :case_id`, `del_yn = 'N'`을 조회한다.
2. `case_stat_cd = 'COMPLETED'`이고 `last_run_id IS NOT NULL`인지 확인한다.
3. `tmd_mdl_scnrcaseh.run_id = tmd_mdl_scnrcasem.last_run_id`로 latest run을 조회한다.
4. latest run의 `exec_stat_cd = 'COMPLETED'`인지 확인한다.
5. latest run의 `run_seq`로 각 result table을 조회한다.
6. result row가 없으면 DB-backed 전환 시점의 API는 data integrity mismatch로 보고 `409 BIZ_RULE_VIOLATION` 또는 운영 표준 오류로 처리한다. 정상 운영에서는 Python insert commit 후 callback이 호출되므로 이 상태가 노출되지 않아야 한다.

Result 조회 API는 다른 사용자가 실행한 case 결과도 볼 수 있는 read model이다. 따라서 `caseId -> latest run -> result` 해석을 위한 active case 검증은 `usr_no` 소유자 검증과 분리한다. endpoint 접근 가능 여부는 controller permission rule로 판단하고, update/delete 같은 소유자 변경 작업에만 `usr_no` ownership predicate를 적용한다.

Historical run result 조회가 필요하면 `runId` 기반 API를 별도 feature spec으로 추가한다. 이 문서는 `caseId -> latest run -> result`만 canonical 범위로 둔다.

## Canonical Table Set

### `tmd_mdl_scnrcasevolrgnl`

Volumes Global/LNG annual/monthly region-bucket JSON rows. The old atomic-row table `tmd_mdl_scnrcasevoll` is no longer an active rollout target.

YEARLY API 응답은 source yearly buckets를 우선 사용한다. source가 yearly buckets를 제공하지 않는 경우에만 별도 서비스 집계 정책을 feature spec/implementation에서 확정한다.

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

Rules:

- `sgmt_cd = 'TRADE'` is not allowed in this table. Trade Flows use dedicated tables.
- One row represents one bucket: `run_seq + sgmt_cd + perd_unit_spt_cd + data_type_cd + rgn_cd`.
- `rgn_cd` is `NOT NULL` because every bucket belongs to exactly one region.
- `ux01` is the required access path and duplicate guard for the only expected read path. Do not add a secondary index until DBA verifies an `EXPLAIN` gap.
- `perd_unit_spt_cd`는 enum `PeriodUnitSeparateCd` 코드값을 쓰며 Volumes는 `Y`(연) / `M`(월) subset만 사용한다.
- `vol_data_json.periodKeys` carries period identity:
  - `periodUnit = 'Y'`: every period key is `YYYY` (4자리).
  - `periodUnit = 'M'`: every period key is `YYYYMM` (6자리 zero-padded).
- Top-level JSON `segment`, `periodUnit`, `dataType`, and `region` must match the relational key columns.
- `periodKeys` must be non-empty and sorted ascending.
- Every JSON row `values` array must have exactly the same length as `periodKeys`; `values[i]` maps to `periodKeys[i]`.
- `rows[0]` must be the region subtotal row (`country == null`) for deterministic response assembly.
- Country rows must have non-null, non-blank `country`.
- Current API exposes only `TOTAL` attribute rows. Java/backend must ignore or filter out non-`TOTAL` rows until the API contract exposes breakdown filtering.
- Planned LNG extension: `CONTRACTED` / `UNCONTRACTED` can appear inside JSON for `sgmt_cd = 'LNG'` and `data_type_cd IN ('LNG_IMPORTS', 'LNG_EXPORTS')`. A country's `TOTAL` should equal `CONTRACTED + UNCONTRACTED` per period when both breakdown rows exist.
- `TYPE_TOTAL` is not stored in each bucket. Service computes it by summing selected region subtotal rows per period.
- DBA policy상 `CHECK` 제약을 운영 DB에 적용하지 않는다면 동일 조건을 Python/model loader와 Java/backend validation에서 강제해야 한다.

### `tmd_mdl_scnrcasetrdehdrl`

Trade Flow matrix axis headers. FROM/TO axes are both row records.

```sql
CREATE TABLE tmd_mdl_scnrcasetrdehdrl
(
    trd_hdr_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름헤더순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
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
    CONSTRAINT tmd_mdl_scnrcasetrdehdrl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasetrdehdrl_ux01
        UNIQUE (run_seq, selector_cd, base_year, axis_cd, hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름헤더내역';

CREATE INDEX tmd_mdl_scnrcasetrdehdrl_ix01
    ON tmd_mdl_scnrcasetrdehdrl (run_seq, selector_cd, base_year, axis_cd, hdr_ord);
```

Rules:

- `axis_cd`: `FROM`, `TO`
- `hdr_kind_cd`: `REGION_SUBTOTAL`, `COUNTRY`, `GRAND_TOTAL`
- `hdr_key` maps directly to API `fromCountries[].key`, `toCountries[].key`, `flows[].fromKey`, `flows[].toKey`.
- `hdr_ord` preserves FE display order from the output file.
- `GRAND_TOTAL` is valid only for the TO axis unless a future API contract adds FROM total.

### `tmd_mdl_scnrcasetrdeflwl`

Trade Flow sparse non-zero matrix values.

```sql
CREATE TABLE tmd_mdl_scnrcasetrdeflwl
(
    trd_flw_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
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
    flow_val        DOUBLE                                NOT NULL COMMENT '흐름값 (MTPA 기준)',
    CONSTRAINT tmd_mdl_scnrcasetrdeflwl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasetrdeflwl_ux01
        UNIQUE (run_seq, selector_cd, base_year, from_hdr_key, to_hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름내역';

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix01
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year);

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix02
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year, to_rgn_cd);

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix03
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year, from_rgn_cd);
```

Rules:

- Store MTPA only. API `unit=BCM` conversion is query/service logic.
- Store only non-zero val[04_incremental_apply_v2.sql](../../../../../../Downloads/04_incremental_apply_v2.sql)ues. Missing cells are interpreted as `0` by the API/FE contract.
- `from_hdr_key` and `to_hdr_key` must exist in `tmd_mdl_scnrcasetrdehdrl` for the same `run_seq`, `selector_cd`, and `base_year`. Enforce by application validation or by DBA-approved composite FK if rollout complexity is acceptable.
- Region columns are duplicated for filter performance and operational inspection. Header rows remain canonical for display label/order.

### `tmd_mdl_scnrcaseprcl`

Price annual/monthly table rows. Chart response is derived from monthly rows.

```sql
CREATE TABLE tmd_mdl_scnrcaseprcl
(
    prc_seq         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '가격순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    perd_unit_spt_cd VARCHAR(2)                           NOT NULL COMMENT '기간단위구분코드 (M:월/Y:년, PeriodUnitSeparateCd)',
    perd_key        VARCHAR(6)                            NOT NULL COMMENT '기간키값 (YYYY 또는 YYYYMM)',
    prc_rgn_nm      VARCHAR(200)                          NOT NULL COMMENT '가격지역명',
    base_case_prc   DOUBLE                                NOT NULL COMMENT '베이스케이스가격 (단위: USD/mmbtu 고정)',
    user_adj_prc    DOUBLE                                NOT NULL COMMENT '사용자조정가격 (단위: USD/mmbtu 고정)',
    CONSTRAINT tmd_mdl_scnrcaseprcl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcaseprcl_ux01
        UNIQUE (run_seq, prc_rgn_nm, perd_key),
    CONSTRAINT tmd_mdl_scnrcaseprcl_ck01
        CHECK (
            (perd_unit_spt_cd = 'Y'
             AND perd_key REGEXP '^[0-9]{4}$')
         OR (perd_unit_spt_cd = 'M'
             AND perd_key REGEXP '^[0-9]{6}$'
             AND CAST(SUBSTRING(perd_key, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12)
        )
)
    COMMENT 'TMD_시나리오케이스실행가격내역';
```

`tmd_mdl_scnrcaseprcl`은 별도 secondary index를 두지 않는다. 유일 조회 경로는 `run_seq` 기준 latest-run 전량 읽기이며 `tmd_mdl_scnrcaseprcl_ux01 (run_seq, prc_rgn_nm, perd_key)`가 기간/지역 유일성과 기본 접근 경로를 담당한다. `perd_unit_spt_cd`는 낮은 cardinality의 조회 분류용 컬럼이므로 UK에는 넣지 않는다. 정렬 보조가 필요할 정도로 run 단위 Price row 수가 커지면 DBA EXPLAIN 확인 후 non-unique 보조 인덱스를 별도 승인한다.

Rules:

- `perd_unit_spt_cd`는 enum `PeriodUnitSeparateCd` 코드값을 쓰며 Price는 `M`(월) / `Y`(년) subset만 사용한다. API granularity `MONTHLY` / `YEARLY`는 서비스 레이어에서 `M` / `Y`로 매핑한다. (Price API는 granularity를 request로 받지 않고 응답에 annual/monthly 테이블을 모두 반환하므로 이 컬럼은 내부 row 구분자다.)
- `perd_unit_spt_cd = 'M'`: `perd_key = YYYYMM` (6자리 zero-padded).
- `perd_unit_spt_cd = 'Y'`: `perd_key = YYYY` (4자리).
- 연/월 row의 기간 정보는 `perd_key` 하나로 식별하고, 연/월 구분은 `perd_unit_spt_cd`로 명시한다. API DTO의 `year` / `month` int 값은 서비스가 `perd_key`를 파싱해 채운다(Y → `year`만, M → 앞 4자리 `year` + 뒤 2자리 `month`).
- `perd_unit_spt_cd + perd_key`는 의도적 중복이다. `perd_key` 길이로 `Y`/`M`을 추론할 수 있지만, sentinel 월 값보다 도메인 의미가 명확하고 응답 분류도 단순해지므로 Price에는 명시 컬럼을 둔다. Volumes는 같은 기간 정책을 JSON `periodKeys` validation으로 적용한다.
- `perd_key` 무결성은 `tmd_mdl_scnrcaseprcl_ck01` CHECK로 숫자 포맷과 월 범위까지 검증한다. DBA 정책상 CHECK 제약을 운영 DB에 적용하지 않는다면 동일 조건을 Python/model loader와 Java/backend validation에서 강제해야 한다. `YYYYMM` zero-padding 덕분에 문자열 정렬이 곧 시간순 정렬이다.
- `prc_rgn_nm` is dynamic output data, not a fixed enum.
- `BASE_CASE` and `USER_ADJUSTED` API series labels are derived from `base_case_prc` and `user_adj_prc`.
- 2026-06-11 ERD review open items (DB-backed 전환 전 정리 필요):
  - API series 코드는 `BASE_CASE` / `USER_ADJUSTED`로 통일되었다(코드 enum `PriceSeriesType`·mock seed 기준, feature spec도 정렬 완료). 물리 컬럼 `base_case_prc` / `user_adj_prc`와 의미가 일치한다.
  - Price 결과테이블 물리명 사전 정합 완료: `granularity_cd` → `perd_unit_spt_cd`, `period_key` → `perd_key`, `user_adj_prc`의 `조정/ADJ` 용어사전 등록(`db-term-dictionary-sheet2.csv` id 380). 추가 rename/등록 불필요.
  - (2026-06-11 후속) Price 테이블은 `perd_unit_spt_cd` + `perd_key`로 기간을 표현한다. 기간 유일성은 `perd_key`가 담당하므로 `ux01`은 `(run_seq, prc_rgn_nm, perd_key)`로 확정했다. `perd_key` 포맷은 `ck01` CHECK로 숫자 포맷과 월 범위까지 보강한다.

## Index and Partition Strategy

### Initial Index Strategy

| Table | Main query | Required index |
|---|---|---|
| `tmd_mdl_scnrcasevolrgnl` | run + segment + period unit + type/region bucket lookup | `ux01 (run_seq, sgmt_cd, data_type_cd, rgn_cd, perd_unit_spt_cd)` covers duplicate prevention and expected lookup. No secondary index initially. |
| `tmd_mdl_scnrcasetrdehdrl` | run + selector + year + axis display order | `ix01 (run_seq, selector_cd, base_year, axis_cd, hdr_ord)` |
| `tmd_mdl_scnrcasetrdeflwl` | run + selector + year sparse matrix | `ix01 (run_seq, selector_cd, base_year)` |
| `tmd_mdl_scnrcasetrdeflwl` | TO region filter | `ix02 (run_seq, selector_cd, base_year, to_rgn_cd)` |
| `tmd_mdl_scnrcasetrdeflwl` | FROM region filter | `ix03 (run_seq, selector_cd, base_year, from_rgn_cd)` |
| `tmd_mdl_scnrcaseprcl` | run + price region + period order | `ux01 (run_seq, prc_rgn_nm, perd_key)` 가 유일성과 기본 접근 경로를 커버. 별도 secondary index 없음 |

Low-cardinality columns such as `del_yn`, status, selector, segment, and region are not indexed alone. They are only used after `run_seq` or similarly selective columns.

### Partition Decision

Initial rollout recommendation: do not physically partition on the first DB-backed release unless DBA row-count estimation shows the result tables will exceed normal operational thresholds.

Reason:

- Current API always resolves to one latest `run_seq` before reading result rows.
- `run_seq` is the most selective access key.
- Physical partitioning can complicate FK constraints and rollback operations depending on target MySQL version and DBA policy.
- Retention/archive requirements are not yet fixed.

Period-column partitioning is not the recommended retention strategy.

- model period has a small bounded range such as 2023-2030.
- Each run writes rows across the same model year range, so every year partition grows as run count grows.
- Dropping a period partition would delete one model period across all runs, which is not the desired archive unit.
- Volumes and Price queries may read all periods for a selected run, so period partitioning can add operational complexity without solving accumulation.

Future partition-ready rule:

- If partitioning is needed for retention/archive, prefer RANGE partitioning by `run_seq` on large result row tables:
  - `tmd_mdl_scnrcasevolrgnl`
  - `tmd_mdl_scnrcasetrdehdrl`
  - `tmd_mdl_scnrcasetrdeflwl`
  - `tmd_mdl_scnrcaseprcl`
- `run_seq` is already included in every unique key of the result tables, which is compatible with MySQL partition-key requirements.
- Partition ranges must align with an approved run retention policy and DBA purge procedure.
- Before enabling partitioning, re-check target MySQL support for FK constraints on partitioned InnoDB tables. If unsupported or operationally risky, keep logical references in application validation and omit physical FK constraints on partitioned child row tables.
- `crt_dtm` based partitioning is not the first-choice design unless unique keys and purge procedures are redesigned around time-based retention.

## Query Shapes

### Volumes

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

Current API compatibility rule:

- Until the feature spec exposes an attribute filter, parse only JSON rows with `attrType = 'TOTAL'`.
- If a future API exposes `TOTAL` / `CONTRACTED` / `UNCONTRACTED`, keep the SQL bucket lookup unchanged and filter/assemble the requested attribute rows from `vol_data_json`.
- `TYPE_TOTAL` is computed in service from selected region subtotal rows, not read from SQL.

### Trade Flows

```sql
SELECT axis_cd, hdr_key, hdr_lbl_nm, hdr_kind_cd, rgn_cd, nat_nm, hdr_ord
FROM tmd_mdl_scnrcasetrdehdrl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = :selector
  AND base_year = :year
ORDER BY axis_cd, hdr_ord;

SELECT from_hdr_key, to_hdr_key,
       from_rgn_cd, from_nat_nm, to_rgn_cd, to_nat_nm, flow_val
FROM tmd_mdl_scnrcasetrdeflwl
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
SELECT perd_unit_spt_cd, perd_key,
       prc_rgn_nm, base_case_prc, user_adj_prc
FROM tmd_mdl_scnrcaseprcl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
ORDER BY
  perd_unit_spt_cd,
  perd_key,
  prc_rgn_nm;
```

## Normalization Assessment

| Table | Assessment |
|---|---|
| `tmd_mdl_scnrcasevolrgnl` | Intentionally denormalized region-bucket JSON table. Relational key columns identify one `run_seq + segment + period unit + type + region` bucket, while `vol_data_json` holds period keys and row values. This violates 1NF by design to avoid excessive atomic inserts and matches the read-only API filter floor. |
| `tmd_mdl_scnrcasetrdehdrl` | Header metadata normalized as rows instead of JSON arrays. Header order/label/kind are axis attributes. |
| `tmd_mdl_scnrcasetrdeflwl` | Sparse flow values separated from header metadata. Region/name columns are intentional denormalization for filter speed and DBA inspection. |
| `tmd_mdl_scnrcaseprcl` | Price values are atomic by run/granularity/region/period. 기간 식별은 non-null `perd_key`가 담당하고, `perd_unit_spt_cd`는 분류/validation 명확성을 위한 의도적 중복이다. |

Intentional denormalization:

- Trade Flow `from_*`/`to_*` region/name columns duplicate header metadata to avoid repeated joins for region filtering and operational checks.
- Volumes `vol_data_json` stores repeated row/value groups inside one bucket. This is accepted only while the API remains read-only and does not require DB-side country search, country filtering, or value threshold filtering.
- Volumes/Price `perd_unit_spt_cd` duplicates information inferable from period-key length, but it is retained to make domain granularity explicit and avoid sentinel month values.
- Volumes LNG `TOTAL` JSON rows may duplicate values mathematically comparable to `CONTRACTED + UNCONTRACTED`, but `TOTAL` is treated as source output to preserve rounding/unclassified differences and avoid query-time reconstruction.
- Price stores base and adjusted values in one row because the API always returns both series together for the same period/region.

## Rollout Draft Structure

### Pre-Check

```sql
SHOW CREATE TABLE tmd_mdl_scnrcaseh;
SHOW INDEX FROM tmd_mdl_scnrcaseh;

SHOW TABLES LIKE 'tmd_mdl_scnrcasevolrgnl';
SHOW TABLES LIKE 'tmd_mdl_scnrcasetrdehdrl';
SHOW TABLES LIKE 'tmd_mdl_scnrcasetrdeflwl';
SHOW TABLES LIKE 'tmd_mdl_scnrcaseprcl';

SHOW INDEX FROM tcm_filel;

SELECT cd_grp_id, cd_id, cd_nm, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
  AND cd_id = 'SCNRCASEH';
```

Expected:

- `tmd_mdl_scnrcaseh.run_seq` exists and is unique.
- `SCNRCASEH` is registered or ready to register as defined by the case execution DDL draft.
- None of the new result table names already exist.

### Apply

Apply order:

1. `tmd_mdl_scnrcasevolrgnl`
2. `tmd_mdl_scnrcasetrdehdrl`
3. `tmd_mdl_scnrcasetrdeflwl`
4. `tmd_mdl_scnrcaseprcl`
5. Secondary indexes
6. Optional partitions only if DBA approves physical partition rollout

If `tmd_mdl_scnrcasetrdeflwl` was already created with `flow_val DECIMAL(30, 18)`, apply the following DBA-approved compatibility DDL instead of recreating the table:

```sql
ALTER TABLE tmd_mdl_scnrcasetrdeflwl
    MODIFY flow_val DOUBLE NOT NULL COMMENT '흐름값 (MTPA 기준)';
```

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
DROP TABLE tmd_mdl_scnrcaseprcl;
DROP TABLE tmd_mdl_scnrcasetrdeflwl;
DROP TABLE tmd_mdl_scnrcasetrdehdrl;
DROP TABLE tmd_mdl_scnrcasevolrgnl;
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
FROM tmd_mdl_scnrcasevolrgnl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = 'GLOBAL'
  AND perd_unit_spt_cd = 'M'
  AND data_type_cd IN ('DEMAND')
  AND rgn_cd IN ('ASIA')
ORDER BY data_type_cd, rgn_cd;

EXPLAIN
SELECT *
FROM tmd_mdl_scnrcasetrdehdrl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = 'LNG'
  AND base_year = 2026
ORDER BY axis_cd, hdr_ord;

EXPLAIN
SELECT *
FROM tmd_mdl_scnrcasetrdeflwl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND selector_cd = 'LNG'
  AND base_year = 2026
  AND to_rgn_cd = 'ASIA';

EXPLAIN
SELECT *
FROM tmd_mdl_scnrcaseprcl
WHERE del_yn = 'N'
  AND run_seq = :run_seq
ORDER BY
  perd_unit_spt_cd,
  perd_key,
  prc_rgn_nm;
```

Expected:

- `type` is not `ALL`.
- `key` uses the intended table-specific index.
- Estimated rows are bounded by one run's result size.

Data verification:

```sql
SELECT COUNT(*) FROM tmd_mdl_scnrcasevolrgnl WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdehdrl WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdeflwl WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcaseprcl WHERE run_seq = :run_seq AND del_yn = 'N';

SELECT COUNT(*) AS invalid_volume_bucket_rows
FROM tmd_mdl_scnrcasevolrgnl
WHERE run_seq = :run_seq
  AND del_yn = 'N'
  AND (
      perd_unit_spt_cd NOT IN ('Y', 'M')
   OR JSON_VALID(vol_data_json) = 0
   OR JSON_UNQUOTE(JSON_EXTRACT(vol_data_json, '$.schemaVersion')) <> CAST(json_schm_ver AS CHAR)
   OR JSON_UNQUOTE(JSON_EXTRACT(vol_data_json, '$.segment')) <> sgmt_cd
   OR JSON_UNQUOTE(JSON_EXTRACT(vol_data_json, '$.periodUnit')) <> perd_unit_spt_cd
   OR JSON_UNQUOTE(JSON_EXTRACT(vol_data_json, '$.dataType')) <> data_type_cd
   OR JSON_UNQUOTE(JSON_EXTRACT(vol_data_json, '$.region')) <> rgn_cd
  );

SELECT COUNT(*) AS invalid_volume_bucket_shape_rows
FROM tmd_mdl_scnrcasevolrgnl
WHERE run_seq = :run_seq
  AND del_yn = 'N'
  AND (
      JSON_TYPE(JSON_EXTRACT(vol_data_json, '$.periodKeys')) <> 'ARRAY'
   OR JSON_LENGTH(JSON_EXTRACT(vol_data_json, '$.periodKeys')) = 0
   OR JSON_TYPE(JSON_EXTRACT(vol_data_json, '$.rows')) <> 'ARRAY'
   OR JSON_LENGTH(JSON_EXTRACT(vol_data_json, '$.rows')) = 0
  );

SELECT COUNT(*) AS invalid_period_rows
FROM tmd_mdl_scnrcaseprcl
WHERE run_seq = :run_seq
  AND del_yn = 'N'
  AND NOT (
      (perd_unit_spt_cd = 'Y'
       AND perd_key REGEXP '^[0-9]{4}$')
   OR (perd_unit_spt_cd = 'M'
       AND perd_key REGEXP '^[0-9]{6}$'
       AND CAST(SUBSTRING(perd_key, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12)
  );
```

Expected invalid count values are `0`. If DBA policy does not apply CHECK constraints in production, this same validation condition is mandatory in Python/model loader and Java/backend validation before a run can be marked `COMPLETED`. Deep JSON validation such as sorted `periodKeys`, per-row `values` length, first-row subtotal, and `TOTAL` attr handling remains application/loader responsibility because MySQL JSON SQL is awkward for nested array validation.

## Mock to Real Transition Boundary (completed 2026-06-15)

Phase 1 mock (historical, superseded):

- `src/main/resources/mock/fundamental-model/fundamental-model-volumes-data.json`
- `src/main/resources/mock/fundamental-model/fundamental-model-trade-flows-data.json`
- `src/main/resources/mock/fundamental-model/fundamental-model-price-data.json`
- no DB writes
- no DBA rollout

DB-backed real (current state):

- result API flags in `FundamentalModelMockProperties` switched from mock service to real service per API.
- real service does not read mock JSON in the DB-backed profile.
- real service reads only latest completed run result tables through `caseId -> last_run_id -> run_seq`.
- Python/model side inserts Volumes into `tmd_mdl_scnrcasevolrgnl` and commits result table inserts before invoking success callback.
- Java/backend does not set `exec_stat_cd = 'COMPLETED'` unless callback validation and result row existence checks pass.
- `tcm_filel(SCNRCASEH + run_seq)` remains the source for raw output file metadata, not for chart/table API rows.
- mock seed files are kept only for local/demo mode.

Transition checklist (all complete):

1. Apply case/run DDL from `scenario-case-execution-erd-ddl-draft.md`. — done
2. Apply result tables from this document. — done
3. Implement result ingestion from model output files. — done
4. Verify row counts and EXPLAIN for one completed run. — done
5. Switch result API flags from mock to real in a controlled profile. — done
6. Keep mock seed files only for local/demo mode. — done

## DBA Action Items

- ~~Confirm initial no-partition rollout.~~ confirmed and live.
- Confirm whether CHECK constraints are applied in production. If not, require the same period-key validation in Python/model loader and Java/backend validation.
- Define result retention policy: latest N runs per case, M-month retention, or manual archive only. (still open — no retention job implemented yet)
- If partitioning is required later, confirm `run_seq` RANGE partition design and FK policy. (still open, future work)
- ~~Confirm JSON column production policy and maximum expected `vol_data_json` payload size for `tmd_mdl_scnrcasevolrgnl`.~~ Trade Flow `flow_val` and Price values are `DOUBLE` as of 2026-06-15; `vol_data_json` policy confirmed as part of the 2026-06-15 rollout.
- Confirm secondary index count against insert volume and model output size.

## Implementation Readiness

implemented and rolled out (2026-06-15)

- The data model, run linkage, key strategy, mock-to-real boundary, and DBA verification shape were fixed and carried through to production rollout.
- Physical rollout is complete. Remaining DBA items (retention policy, future partition/FK policy) are follow-up operational decisions and do not block the current implementation or API contract.
