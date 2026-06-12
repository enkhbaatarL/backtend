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
  - `tmd_mdl_scnrcasem.case_id`
  - `tmd_mdl_scnrcaseh.run_id`
  - `tmd_mdl_scnrcaseh.run_seq`
  - `tcm_filel.file_rel_obj_spt_cd = 'SCNRCASEH'`
  - API response DTO contract in `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
- supersedes:
  - `docs/design-docs/gas/mi-fundamental-model-volumes-erd-ddl-draft.md` as canonical Volumes persistence design
  - `docs/design-docs/gas/mi-fundamental-model-trade-flow-erd-ddl-draft.md` as canonical Trade Flow persistence design
  - "DB-backed 전환 시 별도 ERD review 필요" notes for Volumes/Trade Flows/Price result data in older drafts
- last synced: 2026-06-11
- open questions:
  - DBA physical rollout window and target schema name
  - final numeric precision if model output confirms a narrower or wider requirement than `DECIMAL(30, 18)`
  - result retention policy: keep latest N runs per case, keep runs for M months, or archive by explicit DBA operation
  - DBA confirmation of the initial no-partition recommendation
  - DBA confirmation of future partition objective: query pruning only vs retention/archive management
  - Price 물리명 사전 정합성 (2026-06-11 ERD review, 해소됨): `granularity_cd` → `perd_unit_spt_cd`(기간단위구분코드, enum `PeriodUnitSeparateCd`) 재사용, `period_key` → `perd_key`(표준 약어 `PERD` + 기존 `_key` 패밀리), `user_adj_prc`의 `조정/ADJ`는 용어사전(`db-term-dictionary-sheet2.csv` id 380)에 등록 완료. Price 결과테이블 물리명은 모두 사전 정합.
  - Price series 라벨 단일화 (2026-06-11 ERD review, 해소됨): API series 코드는 `BASE_CASE` / `USER_ADJUSTED`로 통일했다. 코드 enum `PriceSeriesType`·mock seed에 맞춰 feature spec의 `LAST_ADJUSTED` 표기(토큰 8곳 + 라벨 문자열 3곳)를 `USER_ADJUSTED` / `User adjusted price`로 정렬 완료.
  - Price `unit` 상수 필드 (2026-06-11 ERD review): feature spec은 Price 응답에 `unit: "USD/mmbtu"` 상수 필드를 정의하지만 현재 `FundamentalModelPriceRes`에는 해당 필드가 없다. ERD가 단위 컬럼을 두지 않는 결정은 유효하나 DB-backed 전환 전 DTO/spec 정합성을 확정해야 한다.

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
   - parsed Volumes rows: `tmd_mdl_scnrcasevoll`
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
   - `perd_unit_spt_cd + perd_key`는 엄밀히는 의도적 중복이다. `perd_key = '2026'`이면 `Y`, `perd_key = '202601'`이면 `M`을 추론할 수 있지만, `base_mnth = 0` 같은 sentinel 방식보다 도메인 의미가 명확하므로 Volumes/Price에는 명시 컬럼을 둔다.
   - `perd_unit_spt_cd`는 조회 분류용이며, 기간 식별의 유일성은 `perd_key`가 담당한다. 따라서 UK에는 가능한 한 `perd_unit_spt_cd`를 넣지 않는다.

8. Price 단위는 `USD/mmbtu` 고정이며 `tmd_mdl_scnrcaseprcl`에 단위 컬럼을 저장하지 않는다.
   - 모델 output이 항상 USD/mmbtu 단위로 출력하는 불변 계약이다.
   - API response는 `unit: "USD/mmbtu"` 상수 필드로 반환한다.
   - 모델 output 스펙이 변경되어 단위가 run마다 달라지는 경우 `prc_unit_cd VARCHAR(20)` 컬럼 추가와 마이그레이션이 필요하다.

9. external reference sync:
   - 2026-06-10: Notion reference pages `결과 저장 테이블 초안`, `상세 컬럼표와 적재 규칙` 에 남아 있던 구 physical name `tmd_mdl_scncase*` / `scn_id` 는 superseded다.
   - canonical physical names are `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, `tmd_mdl_scnrcasevoll`, `tmd_mdl_scnrcasetrdehdrl`, `tmd_mdl_scnrcasetrdeflwl`, `tmd_mdl_scnrcaseprcl`, and `scnr_id`.
   - naming authority는 `ScenarioCase` / `ScenarioCaseRunHistory` entity와 `scenario-case-execution-erd-ddl-draft.md` 의 rollout naming이다.
   - 2026-06-11: Notion pages `MI Fundamental Model`, `결과 저장 테이블 초안`, and `상세 컬럼표와 적재 규칙` were rechecked as external evidence.
   - Confirmed evidence: Python/model side stores parsed output rows under `run_seq`, commits result rows before callback, and does not require a separate result status table.
   - Superseded or narrowed evidence: Notion's remaining `base_year` partitioning question does not define the retention strategy. This document is canonical: initial rollout is no partition, and future retention/archive partitioning should use `run_seq` RANGE if approved.
   - Numeric precision remains open as `DECIMAL(30, 18)` until model output confirms a narrower requirement; do not switch to floating-point storage from Notion discussion alone.

10. Result row accumulation is governed by `run_seq`.
   - Every completed model execution creates a new result snapshot under one `run_seq`.
   - Trade Flow `base_year` and Volumes/Price `perd_key` are data period dimensions, not lifecycle or retention dimensions.
   - Retention/archive policy must be expressed in terms of runs, for example latest N runs per case or runs older than M months.
   - Until a retention policy is approved, result rows are preserved and no automated purge is assumed.

## Concept Model

```text
tmd_mdl_scnrcasem (case)
  └── tmd_mdl_scnrcaseh (run, run_seq)
        ├── tcm_filel (SCNRCASEH + run_seq, raw output files)
        ├── tmd_mdl_scnrcasevoll (Volumes annual/monthly atomic rows)
        ├── tmd_mdl_scnrcasetrdehdrl (Trade Flow FROM/TO axis headers)
        ├── tmd_mdl_scnrcasetrdeflwl (Trade Flow sparse non-zero values)
        └── tmd_mdl_scnrcaseprcl (Price annual/monthly rows)
```

## Case / Run Lookup Rules

`GET /cases/{caseId}/volumes`, `GET /cases/{caseId}/volumes/trade`, `GET /cases/{caseId}/price`는 feature spec대로 `caseId`를 받지만 DB는 최신 run의 result를 읽는다.

Recommended lookup flow:

1. `tmd_mdl_scnrcasem`에서 `case_id = :case_id`, `usr_no = :usr_no`, `del_yn = 'N'`을 조회한다.
2. `case_stat_cd = 'COMPLETED'`이고 `last_run_id IS NOT NULL`인지 확인한다.
3. `tmd_mdl_scnrcaseh.run_id = tmd_mdl_scnrcasem.last_run_id`로 latest run을 조회한다.
4. latest run의 `exec_stat_cd = 'COMPLETED'`인지 확인한다.
5. latest run의 `run_seq`로 각 result table을 조회한다.
6. result row가 없으면 DB-backed 전환 시점의 API는 data integrity mismatch로 보고 `409 BIZ_RULE_VIOLATION` 또는 운영 표준 오류로 처리한다. 정상 운영에서는 Python insert commit 후 callback이 호출되므로 이 상태가 노출되지 않아야 한다.

Historical run result 조회가 필요하면 `runId` 기반 API를 별도 feature spec으로 추가한다. 이 문서는 `caseId -> latest run -> result`만 canonical 범위로 둔다.

## Canonical Table Set

### `tmd_mdl_scnrcasevoll`

Volumes Global/LNG annual/monthly atomic rows. YEARLY API 응답은 source annual rows를 우선 사용하고, source가 annual rows를 제공하지 않는 경우에만 별도 서비스 집계 정책을 feature spec/implementation에서 확정한다.

```sql
CREATE TABLE tmd_mdl_scnrcasevoll
(
    vol_seq         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '물량순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
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
    perd_unit_spt_cd VARCHAR(2)                           NOT NULL COMMENT '기간단위구분코드 (Y:년/M:월, PeriodUnitSeparateCd)',
    perd_key        VARCHAR(6)                            NOT NULL COMMENT '기간키값 (YYYY 또는 YYYYMM)',
    vol_val         DECIMAL(30, 18)                       NOT NULL COMMENT '물량값',
    CONSTRAINT tmd_mdl_scnrcasevoll_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasevoll_ux01
        UNIQUE (run_seq, sgmt_cd, data_type_cd, row_key, perd_key),
    CONSTRAINT tmd_mdl_scnrcasevoll_ck01
        CHECK (
            (perd_unit_spt_cd = 'Y'
             AND perd_key REGEXP '^[0-9]{4}$')
         OR (perd_unit_spt_cd = 'M'
             AND perd_key REGEXP '^[0-9]{6}$'
             AND CAST(SUBSTRING(perd_key, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12)
        )
)
    COMMENT 'TMD_시나리오케이스실행물량내역';

CREATE INDEX tmd_mdl_scnrcasevoll_ix01
    ON tmd_mdl_scnrcasevoll (run_seq, sgmt_cd, data_type_cd, rgn_cd, perd_unit_spt_cd, perd_key);
```

Rules:

- `sgmt_cd = 'TRADE'` is not allowed in this table. Trade Flows use dedicated tables.
- `row_key` is non-null to avoid MySQL unique-key ambiguity with nullable `rgn_cd`/`nat_nm`.
- `perd_unit_spt_cd`는 enum `PeriodUnitSeparateCd` 코드값을 쓰며 Volumes는 `Y`(연) / `M`(월) subset만 사용한다.
- `perd_unit_spt_cd = 'Y'`: `perd_key = YYYY` (4자리).
- `perd_unit_spt_cd = 'M'`: `perd_key = YYYYMM` (6자리 zero-padded).
- `perd_key`가 기간 식별을 담당하므로 `tmd_mdl_scnrcasevoll_ux01`에는 `perd_unit_spt_cd`를 넣지 않는다. `perd_unit_spt_cd`는 조회/응답 분류와 validation 명확성을 위한 컬럼이다.
- `perd_key` 무결성은 `tmd_mdl_scnrcasevoll_ck01` CHECK로 숫자 포맷과 월 범위까지 검증한다. DBA 정책상 CHECK 제약을 운영 DB에 적용하지 않는다면 동일 조건을 Python/model loader와 Java/backend validation에서 강제해야 한다.
- Recommended `row_key` examples:
  - `COUNTRY:ASIA:China`
  - `REGION_SUBTOTAL:ASIA`
  - `TYPE_TOTAL`
- `TYPE_TOTAL` may be stored if the source output provides it. If not stored, service may derive it from `REGION_SUBTOTAL` rows. The storage decision must be consistent per `run_seq`.

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
- `hdr_kind_cd`: `REGION_TOTAL`, `COUNTRY`, `GRAND_TOTAL`
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
    flow_val        DECIMAL(30, 18)                       NOT NULL COMMENT '흐름값 (MTPA 기준)',
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
- Store only non-zero values. Missing cells are interpreted as `0` by the API/FE contract.
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
    base_case_prc   DECIMAL(30, 18)                       NOT NULL COMMENT '베이스케이스가격 (단위: USD/mmbtu 고정)',
    user_adj_prc    DECIMAL(30, 18)                       NOT NULL COMMENT '사용자조정가격 (단위: USD/mmbtu 고정)',
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
- `perd_unit_spt_cd + perd_key`는 의도적 중복이다. `perd_key` 길이로 `Y`/`M`을 추론할 수 있지만, sentinel 월 값보다 도메인 의미가 명확하고 응답 분류도 단순해지므로 Volumes/Price에 같은 정책을 적용한다.
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
| `tmd_mdl_scnrcasevoll` | run + segment + type/region + period unit/order | `ix01 (run_seq, sgmt_cd, data_type_cd, rgn_cd, perd_unit_spt_cd, perd_key)` |
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
  - `tmd_mdl_scnrcasevoll`
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
SELECT data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm,
       perd_unit_spt_cd, perd_key, vol_val
FROM tmd_mdl_scnrcasevoll
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = :segment
  AND data_type_cd IN (:types)
  AND perd_unit_spt_cd = :period_unit
  AND (rgn_cd IN (:regions) OR row_kind_cd = 'TYPE_TOTAL')
ORDER BY data_type_cd, row_kind_cd, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key;
```

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
| `tmd_mdl_scnrcasevoll` | Atomic annual/monthly value per run/segment/type/row/period. `row_key` prevents nullable unique-key ambiguity. 기간 식별은 non-null `perd_key`가 담당하고, `perd_unit_spt_cd`는 분류/validation 명확성을 위한 의도적 중복이다. |
| `tmd_mdl_scnrcasetrdehdrl` | Header metadata normalized as rows instead of JSON arrays. Header order/label/kind are axis attributes. |
| `tmd_mdl_scnrcasetrdeflwl` | Sparse flow values separated from header metadata. Region/name columns are intentional denormalization for filter speed and DBA inspection. |
| `tmd_mdl_scnrcaseprcl` | Price values are atomic by run/granularity/region/period. 기간 식별은 non-null `perd_key`가 담당하고, `perd_unit_spt_cd`는 분류/validation 명확성을 위한 의도적 중복이다. |

Intentional denormalization:

- Trade Flow `from_*`/`to_*` region/name columns duplicate header metadata to avoid repeated joins for region filtering and operational checks.
- Volumes/Price `perd_unit_spt_cd` duplicates information inferable from `perd_key` length, but it is retained to make domain granularity explicit and avoid sentinel month values.
- Price stores base and adjusted values in one row because the API always returns both series together for the same period/region.

## Rollout Draft Structure

### Pre-Check

```sql
SHOW CREATE TABLE tmd_mdl_scnrcaseh;
SHOW INDEX FROM tmd_mdl_scnrcaseh;

SHOW TABLES LIKE 'tmd_mdl_scnrcasevoll';
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

1. `tmd_mdl_scnrcasevoll`
2. `tmd_mdl_scnrcasetrdehdrl`
3. `tmd_mdl_scnrcasetrdeflwl`
4. `tmd_mdl_scnrcaseprcl`
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
DROP TABLE tmd_mdl_scnrcaseprcl;
DROP TABLE tmd_mdl_scnrcasetrdeflwl;
DROP TABLE tmd_mdl_scnrcasetrdehdrl;
DROP TABLE tmd_mdl_scnrcasevoll;
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
FROM tmd_mdl_scnrcasevoll
WHERE del_yn = 'N'
  AND run_seq = :run_seq
  AND sgmt_cd = 'GLOBAL'
  AND data_type_cd IN ('DEMAND')
  AND perd_unit_spt_cd = 'M'
  AND rgn_cd IN ('ASIA')
ORDER BY data_type_cd, row_kind_cd, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key;

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
SELECT COUNT(*) FROM tmd_mdl_scnrcasevoll WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdehdrl WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcasetrdeflwl WHERE run_seq = :run_seq AND del_yn = 'N';
SELECT COUNT(*) FROM tmd_mdl_scnrcaseprcl WHERE run_seq = :run_seq AND del_yn = 'N';

SELECT COUNT(*) AS invalid_period_rows
FROM tmd_mdl_scnrcasevoll
WHERE run_seq = :run_seq
  AND del_yn = 'N'
  AND NOT (
      (perd_unit_spt_cd = 'Y'
       AND perd_key REGEXP '^[0-9]{4}$')
   OR (perd_unit_spt_cd = 'M'
       AND perd_key REGEXP '^[0-9]{6}$'
       AND CAST(SUBSTRING(perd_key, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12)
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

Expected `invalid_period_rows = 0`. If DBA policy does not apply CHECK constraints in production, this same validation condition is mandatory in Python/model loader and Java/backend validation before a run can be marked `COMPLETED`.

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
- `tcm_filel(SCNRCASEH + run_seq)` remains the source for raw output file metadata, not for chart/table API rows.

Transition checklist:

1. Apply case/run DDL from `scenario-case-execution-erd-ddl-draft.md`.
2. Apply result tables from this document.
3. Implement result ingestion from model output files.
4. Verify row counts and EXPLAIN for one completed run.
5. Switch result API flags from mock to real in a controlled profile.
6. Keep mock seed files only for local/demo mode.

## DBA Action Items

- Confirm initial no-partition rollout.
- Confirm whether CHECK constraints are applied in production. If not, require the same period-key validation in Python/model loader and Java/backend validation.
- Define result retention policy: latest N runs per case, M-month retention, or manual archive only.
- If partitioning is required later, confirm `run_seq` RANGE partition design and FK policy.
- Confirm `DECIMAL(30, 18)` is acceptable for Volumes/Trade/Price values.
- Confirm secondary index count against insert volume and model output size.

## Implementation Readiness

ready after minor cleanup

- The data model, run linkage, key strategy, mock-to-real boundary, and DBA verification shape are fixed enough for implementation planning.
- Physical rollout remains blocked until DBA confirms retention policy, partition/FK policy, and numeric precision, but those decisions do not change the API contract or case/run ownership model.
