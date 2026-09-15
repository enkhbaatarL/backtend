# MI Model Input Template Catalog ERD / DDL Draft

## Document Control

- status: Draft
- canonical for: Schema rollout — model input template catalog tables, columns, keys, indexes, nullability, migration, rollback, and verify SQL
- owner: BE / DBA
- source documents:
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - 이 문서의 2026-09-04 `양식 → 테이블` 2단계 초안
- last synced: 2026-09-07
- open questions:
  - 행·열 추가의 append/insert 방식과 수식·서식 복제 규칙이 미정이다.
  - `BaseEntity`의 `crt_*` 감사 컬럼과 DB 명명 문서의 `reg_*` 표준 충돌을 DBA/데이터 표준 담당자가 확인해야 한다.
  - 운영 DB의 `tmm_mi_model_param_m` 현재 값은 확인되지 않았다. 이 문서의 대조는 저장소 DDL/seed 기준이다.

## Purpose

독립수급모델 고도화 화면에서 input Excel 3개를 시트/테이블 단위로 표시·편집하려면
"어느 파일에 무슨 시트가 있고, 그 안에 어떤 표가 어떤 형태로 있는지"를 서버가 알아야 한다.
이 문서는 그 카탈로그와 **양식 버전**의 물리 스키마를 소유한다.

API 계약은 이 문서가 정의하지 않는다. 실행/케이스 스키마는
[scenario-case-execution-erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md)가 소유한다.

## 배경 — 왜 버전이 필요한가

양식은 이미 바뀌었고 앞으로도 바뀐다. 2026-09-04 실측 결과다.

| 파일 | basecase 템플릿 시트 | 실제 case 파일 시트 |
| --- | ---: | ---: |
| demand | 13 | 25 |
| supply | 14 | 23 |
| trade | 8 | 17 |

그리고 기존 파라미터 마스터(`tmm_mi_model_param_m`)의 시드 값이 실제 파일과 이미 어긋나 있다.

| 마스터 `model_sheet` | 실제 파일 | 상태 |
| --- | --- | --- |
| `VolumeAdjustment` (Supply) | `ProductionAdjustment` | 시트명 변경 |
| `TradeRestrictionInput` (Trade) | `LNG_TradeRestrictionInput` / `PNG_TradeRestrictionInput` | 시트 분리 |

trade.xlsm에 `backupLNG_TradeRestrictionInput` 시트가 남아 있는 것이 개명의 흔적이다.
supply.xlsm의 `LiqUtilAdjustment_backup`(60,216셀)은 basecase의 `LiqUtilAdjustment`(60,216셀)와
셀 수가 정확히 일치한다 — 양식 진화 이력을 DB가 아니라 파일 안에 남겨온 상태다.

버전이 없으면 마스터를 새 양식에 맞추는 순간 과거 case 해석이 깨지고,
맞추지 않으면 새 양식을 읽지 못한다.

## 배경 — 왜 편집 대상 지정이 필요한가

실제 파일 3개의 전체 규모다.

| 항목 | 값 |
| --- | ---: |
| 시트 | 65 |
| 값 보유 셀 | 4,201,142 |
| 수식 셀 | 1,862,877 (전부 계산 완료) |
| celldata JSON 환산 | 341MB (gzip 20.6MB) |

이 중 편집 대상으로 추정되는 `*Adjustment` 계열은 **123,890셀(2.9%)** 이고,
`_backup` / `_DISCARDAFTUSE` 시트가 **886,709셀(21.1%)** 를 차지한다.
카탈로그로 편집 대상을 지정하면 전송량·브라우저 메모리·서버 파싱이 함께 줄어든다.

## 1. Document Control

문서 제어 정보는 문서 상단 `Document Control`을 따른다.

## 2. Current structure reviewed

- 실제 input 파일은 `tcm_filel`의 `SCNRCASE + case_id` 관계가 저장한다.
- case와 실행 lifecycle은 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 저장한다.
- `tmm_mi_model_param_m`은 Agent 파라미터 제안/병합용 일부 좌표만 저장하므로 전체 파일
  구조 및 사용자 수정 범위의 canonical table로 재사용하지 않는다.
- 기존 초안의 `SMALLINT UNSIGNED` 행 번호는 실제 100,803행을 저장하지 못한다.

## 3. Change classification

- 분류: `new table`
- 범위: 신규 master/detail/relation 6개
- 기존 table alter: 없음
- 초기 seed: 필요
- 기존 case backfill: 구조 검증된 대상만 필요

## 4. Canonical references

- business ownership/lifecycle/range rules:
  `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
- API contract:
  `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md`
- actual file relationship:
  `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`

## 5. Fixed schema decisions

- 한 양식 version은 서로 호환되는 `DEMAND/SUPPLY/TRADE` 파일 set이다.
- 파일·시트·테이블·수정범위를 각각 별도 detail table로 정규화한다.
- 좌표는 1-based 숫자이며 행은 `INT UNSIGNED`, 열은 `SMALLINT UNSIGNED`다.
- 사용자 수정 가능 범위는 여러 직사각형 row로 저장한다. 임의 JSON 좌표는 사용하지 않는다.
- 행/열 추가 가능 여부는 table 정책 컬럼으로 저장한다.
- case는 사용한 template set 한 건을 relation table로 고정한다.

## 6. Proposed schema change

명명은 [db-object-naming-rules.md](../../conventions/db-object-naming-rules.md)와
[db-term-dictionary-sheet2.csv](../../conventions/db-term-dictionary-sheet2.csv)를 따른다.
`시트/SHT`는 이번 설계에서 사전에 추가했다. 감사 컬럼은 JPA `BaseEntity`와 가장 가까운
`tmd_mdl_scnrcasem` 패턴을 따라 `crt_*`를 사용한 초안이며, 표준 충돌은 적용 전에 해결한다.

```sql
CREATE TABLE tmm_mdlinpttmpltm
(
    tmplt_id            INT UNSIGNED AUTO_INCREMENT COMMENT '입력템플릿ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    mdl_spt_cd          VARCHAR(20) NOT NULL COMMENT '모델구분코드',
    tmplt_ver_no        SMALLINT UNSIGNED NOT NULL COMMENT '템플릿버전번호',
    tmplt_ver_nm        VARCHAR(100) NOT NULL COMMENT '템플릿버전명',
    tmplt_stat_cd       VARCHAR(20) DEFAULT 'DRAFT' NOT NULL
                            COMMENT '템플릿상태코드 (DRAFT/ACTIVE/DEPRECATED)',
    aply_bgn_dtm        DATETIME(6) NULL COMMENT '적용시작일시',
    tmplt_expn          TEXT NULL COMMENT '템플릿설명',
    actv_mdl_spt_cd     VARCHAR(20) AS
                            (IF(del_yn = 'N' AND tmplt_stat_cd = 'ACTIVE', mdl_spt_cd, NULL)) STORED
                            COMMENT '활성모델구분코드',
    CONSTRAINT tmm_mdlinpttmpltm_ux01 UNIQUE (mdl_spt_cd, tmplt_ver_no),
    CONSTRAINT tmm_mdlinpttmpltm_ux02 UNIQUE (actv_mdl_spt_cd),
    CONSTRAINT tmm_mdlinpttmpltm_ck01 CHECK (tmplt_stat_cd IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    CONSTRAINT tmm_mdlinpttmpltm_ck02 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력템플릿마스터';

CREATE TABLE tmm_mdlinptfiled
(
    inpt_file_id        INT UNSIGNED AUTO_INCREMENT COMMENT '입력파일ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    tmplt_id            INT UNSIGNED NOT NULL COMMENT '입력템플릿ID',
    mdl_file_cd         VARCHAR(20) NOT NULL COMMENT '모델파일코드 (DEMAND/SUPPLY/TRADE)',
    file_nm             VARCHAR(255) NOT NULL COMMENT '기준파일명',
    file_ord            SMALLINT UNSIGNED NOT NULL COMMENT '파일순서',
    file_type_cd        VARCHAR(20) NOT NULL COMMENT '파일유형코드 (XLSX/XLSM)',
    file_strt_hash_val  CHAR(64) NULL COMMENT '파일구조해시값',
    CONSTRAINT tmm_mdlinptfiled_ux01 UNIQUE (tmplt_id, mdl_file_cd),
    CONSTRAINT tmm_mdlinptfiled_ux02 UNIQUE (tmplt_id, file_ord),
    CONSTRAINT tmm_mdlinptfiled_fk01 FOREIGN KEY (tmplt_id)
        REFERENCES tmm_mdlinpttmpltm (tmplt_id),
    CONSTRAINT tmm_mdlinptfiled_ck01 CHECK (mdl_file_cd IN ('DEMAND', 'SUPPLY', 'TRADE')),
    CONSTRAINT tmm_mdlinptfiled_ck02 CHECK (file_type_cd IN ('XLSX', 'XLSM')),
    CONSTRAINT tmm_mdlinptfiled_ck03 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력파일상세';

CREATE INDEX tmm_mdlinptfiled_ix01
    ON tmm_mdlinptfiled (file_strt_hash_val, mdl_file_cd, del_yn);

CREATE TABLE tmm_mdlinptshtd
(
    inpt_sht_id         INT UNSIGNED AUTO_INCREMENT COMMENT '입력시트ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_file_id        INT UNSIGNED NOT NULL COMMENT '입력파일ID',
    sht_nm              VARCHAR(150) NOT NULL COMMENT '시트명',
    sht_ord             SMALLINT UNSIGNED NOT NULL COMMENT '시트순서',
    sht_stat_cd         VARCHAR(20) DEFAULT 'VISIBLE' NOT NULL
                            COMMENT '시트상태코드 (VISIBLE/HIDDEN/VERY_HIDDEN)',
    CONSTRAINT tmm_mdlinptshtd_ux01 UNIQUE (inpt_file_id, sht_nm),
    CONSTRAINT tmm_mdlinptshtd_ux02 UNIQUE (inpt_file_id, sht_ord),
    CONSTRAINT tmm_mdlinptshtd_fk01 FOREIGN KEY (inpt_file_id)
        REFERENCES tmm_mdlinptfiled (inpt_file_id),
    CONSTRAINT tmm_mdlinptshtd_ck01 CHECK (sht_stat_cd IN ('VISIBLE', 'HIDDEN', 'VERY_HIDDEN')),
    CONSTRAINT tmm_mdlinptshtd_ck02 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력시트상세';

CREATE TABLE tmm_mdlinpttbld
(
    inpt_tbl_id         INT UNSIGNED AUTO_INCREMENT COMMENT '입력테이블ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_sht_id         INT UNSIGNED NOT NULL COMMENT '입력시트ID',
    inpt_tbl_nm         VARCHAR(100) NOT NULL COMMENT '입력테이블명',
    tbl_ord             SMALLINT UNSIGNED NOT NULL COMMENT '테이블순서',
    tbl_bgn_row_no      INT UNSIGNED NOT NULL COMMENT '테이블시작행번호',
    tbl_end_row_no      INT UNSIGNED NOT NULL COMMENT '테이블종료행번호',
    tbl_bgn_col_no      SMALLINT UNSIGNED NOT NULL COMMENT '테이블시작열번호',
    tbl_end_col_no      SMALLINT UNSIGNED NOT NULL COMMENT '테이블종료열번호',
    hdr_bgn_row_no      INT UNSIGNED NOT NULL COMMENT '헤더시작행번호',
    hdr_end_row_no      INT UNSIGNED NOT NULL COMMENT '헤더종료행번호',
    data_bgn_row_no     INT UNSIGNED NOT NULL COMMENT '데이터시작행번호',
    row_add_psbl_yn     CHAR(1) DEFAULT 'N' NOT NULL COMMENT '행추가가능여부',
    col_add_psbl_yn     CHAR(1) DEFAULT 'N' NOT NULL COMMENT '열추가가능여부',
    inpt_tbl_expn       VARCHAR(500) NULL COMMENT '입력테이블설명',
    CONSTRAINT tmm_mdlinpttbld_ux01 UNIQUE (inpt_sht_id, inpt_tbl_nm),
    CONSTRAINT tmm_mdlinpttbld_ux02 UNIQUE (inpt_sht_id, tbl_ord),
    CONSTRAINT tmm_mdlinpttbld_fk01 FOREIGN KEY (inpt_sht_id)
        REFERENCES tmm_mdlinptshtd (inpt_sht_id),
    CONSTRAINT tmm_mdlinpttbld_ck01 CHECK
        (tbl_bgn_row_no >= 1 AND tbl_bgn_row_no <= hdr_bgn_row_no
         AND hdr_bgn_row_no <= hdr_end_row_no AND hdr_end_row_no < data_bgn_row_no
         AND data_bgn_row_no <= tbl_end_row_no AND tbl_end_row_no <= 1048576),
    CONSTRAINT tmm_mdlinpttbld_ck02 CHECK
        (tbl_bgn_col_no >= 1 AND tbl_bgn_col_no <= tbl_end_col_no AND tbl_end_col_no <= 16384),
    CONSTRAINT tmm_mdlinpttbld_ck03 CHECK (row_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck04 CHECK (col_add_psbl_yn IN ('Y', 'N')),
    CONSTRAINT tmm_mdlinpttbld_ck05 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력테이블상세';

CREATE TABLE tmm_mdlinptmodscopd
(
    mod_scop_id         INT UNSIGNED AUTO_INCREMENT COMMENT '수정범위ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    inpt_tbl_id         INT UNSIGNED NOT NULL COMMENT '입력테이블ID',
    mod_scop_ord        SMALLINT UNSIGNED NOT NULL COMMENT '수정범위순서',
    bgn_row_no          INT UNSIGNED NOT NULL COMMENT '시작행번호',
    end_row_no          INT UNSIGNED NOT NULL COMMENT '종료행번호',
    bgn_col_no          SMALLINT UNSIGNED NOT NULL COMMENT '시작열번호',
    end_col_no          SMALLINT UNSIGNED NOT NULL COMMENT '종료열번호',
    CONSTRAINT tmm_mdlinptmodscopd_ux01 UNIQUE (inpt_tbl_id, mod_scop_ord),
    CONSTRAINT tmm_mdlinptmodscopd_ux02 UNIQUE
        (inpt_tbl_id, bgn_row_no, end_row_no, bgn_col_no, end_col_no),
    CONSTRAINT tmm_mdlinptmodscopd_fk01 FOREIGN KEY (inpt_tbl_id)
        REFERENCES tmm_mdlinpttbld (inpt_tbl_id),
    CONSTRAINT tmm_mdlinptmodscopd_ck01 CHECK
        (bgn_row_no >= 1 AND bgn_row_no <= end_row_no AND end_row_no <= 1048576),
    CONSTRAINT tmm_mdlinptmodscopd_ck02 CHECK
        (bgn_col_no >= 1 AND bgn_col_no <= end_col_no AND end_col_no <= 16384),
    CONSTRAINT tmm_mdlinptmodscopd_ck03 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력수정범위상세';

CREATE TABLE tmd_mdl_scnrcasetmpltd
(
    case_id             INT UNSIGNED COMMENT '시나리오케이스ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    tmplt_id            INT UNSIGNED NOT NULL COMMENT '입력템플릿ID',
    CONSTRAINT tmd_mdl_scnrcasetmpltd_fk01 FOREIGN KEY (case_id)
        REFERENCES tmd_mdl_scnrcasem (case_id),
    CONSTRAINT tmd_mdl_scnrcasetmpltd_fk02 FOREIGN KEY (tmplt_id)
        REFERENCES tmm_mdlinpttmpltm (tmplt_id),
    CONSTRAINT tmd_mdl_scnrcasetmpltd_ck01 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMD_시나리오케이스입력템플릿상세';

CREATE INDEX tmd_mdl_scnrcasetmpltd_ix01
    ON tmd_mdl_scnrcasetmpltd (tmplt_id, del_yn);
```

## 7. ERD review

| 검토 항목 | 결과 |
| --- | --- |
| PK | 모든 table에 단일 PK가 있다. case-template는 `case_id` 자체가 PK다. |
| FK | hierarchy와 case/template 참조에 physical FK를 둔다. 모든 FK 선두 index는 PK/UK/IX로 확보된다. |
| UK | version, 역할, sibling name/order, 수정범위 중복을 방지한다. |
| nullability | 설명·적용시작·구조해시만 nullable이다. 해시는 판별 방식 확정 전 optional이다. |
| status/delete | template lifecycle + 전 table `del_yn`을 사용한다. ACTIVE 하위 row의 변경/삭제는 service에서 금지한다. |
| tenant | 플랫폼 공용 configuration이므로 tenant FK를 두지 않는다. case 접근은 기존 case 권한을 따른다. |

수정범위가 부모 table 경계 안인지와 범위끼리 겹치는지는 다른 row와의 비교가 필요해 CHECK로
완전히 강제할 수 없다. 등록 service와 seed 검증 SQL이 보장한다.

## 8. Code impact review

- JPA: `BaseEntity`를 상속하는 read/write master/detail entity와 repository가 필요하다.
- QueryDSL: template hierarchy projection 한 번으로 조회하거나 단계별 batch 조회해 N+1을 피한다.
- MyBatis: 필수 아님. 초기 seed가 크면 DBA SQL/batch에만 사용한다.
- DTO/API: file/sheet/table/scope hierarchy와 1-based 좌표, add flags가 추가된다.
- service: ACTIVE 전환, 3개 역할 완전성, 범위 포함/비중첩, case mapping 검증이 필요하다.
- `tmm_mi_model_param_m`: 이번 change set에서는 수정하지 않는다.

## 9. Rollout / compatibility review

1. 6개 신규 table을 생성한다. 기존 application은 참조하지 않아 DDL 선행 배포와 호환된다.
2. `DRAFT` template과 file/sheet/table/scope seed를 적재한다.
3. hierarchy와 실제 3개 workbook 구조를 검증한다.
4. 검증된 template만 `ACTIVE`로 전환한다.
5. legacy case는 자동 추측 backfill하지 않고 구조 검증된 case만 mapping한다.
6. 새 application은 mapping 없는 legacy case의 동작을 feature spec에 정의한 뒤 배포한다.

기존 데이터 column 변경이 없어 일반 backfill은 없다. 초기 catalog seed와 선택적 case mapping은
신규 데이터 등록 작업이다.

## 10. DDL draft structure recommendation

### Pre-check

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tmm_mdlinpttmpltm', 'tmm_mdlinptfiled', 'tmm_mdlinptshtd',
      'tmm_mdlinpttbld', 'tmm_mdlinptmodscopd', 'tmd_mdl_scnrcasetmpltd'
  );

SHOW CREATE TABLE tmd_mdl_scnrcasem;
```

Expected: 신규 table은 0건이고, `tmd_mdl_scnrcasem.case_id`는 `INT UNSIGNED` PK다.

### Apply

위 `## 6. Proposed schema change`의 CREATE TABLE/INDEX를 순서대로 실행한다.

### Backfill

- 기존 table column backfill: 필요 없음.
- initial catalog seed: 별도 승인된 seed SQL 필요.
- legacy case mapping: 실제 파일 구조 hash/수동 검증을 통과한 case만 insert한다.

### Rollback

application/seed가 사용하기 전, DBA가 명시적으로 승인한 경우에만 역순 drop한다.

```sql
DROP TABLE IF EXISTS tmd_mdl_scnrcasetmpltd;
DROP TABLE IF EXISTS tmm_mdlinptmodscopd;
DROP TABLE IF EXISTS tmm_mdlinpttbld;
DROP TABLE IF EXISTS tmm_mdlinptshtd;
DROP TABLE IF EXISTS tmm_mdlinptfiled;
DROP TABLE IF EXISTS tmm_mdlinpttmpltm;
```

application이 이미 의존하면 drop 대신 forward-fix를 우선한다.

### Verify

```sql
SHOW CREATE TABLE tmm_mdlinpttmpltm;
SHOW CREATE TABLE tmm_mdlinptfiled;
SHOW CREATE TABLE tmm_mdlinptshtd;
SHOW CREATE TABLE tmm_mdlinpttbld;
SHOW CREATE TABLE tmm_mdlinptmodscopd;
SHOW CREATE TABLE tmd_mdl_scnrcasetmpltd;

-- 모델별 ACTIVE 최대 1건: 결과 0건
SELECT mdl_spt_cd, COUNT(*)
FROM tmm_mdlinpttmpltm
WHERE tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N'
GROUP BY mdl_spt_cd
HAVING COUNT(*) > 1;

-- ACTIVE template의 파일 역할 완전성: 각 row가 role_cnt=3
SELECT t.tmplt_id, COUNT(DISTINCT f.mdl_file_cd) AS role_cnt
FROM tmm_mdlinpttmpltm t
LEFT JOIN tmm_mdlinptfiled f ON f.tmplt_id = t.tmplt_id AND f.del_yn = 'N'
WHERE t.tmplt_stat_cd = 'ACTIVE' AND t.del_yn = 'N'
GROUP BY t.tmplt_id
HAVING role_cnt <> 3;

-- 수정범위가 부모 table 밖인 경우: 결과 0건
SELECT s.mod_scop_id
FROM tmm_mdlinptmodscopd s
JOIN tmm_mdlinpttbld t ON t.inpt_tbl_id = s.inpt_tbl_id
WHERE s.del_yn = 'N'
  AND (s.bgn_row_no < t.tbl_bgn_row_no OR s.end_row_no > t.tbl_end_row_no
       OR s.bgn_col_no < t.tbl_bgn_col_no OR s.end_col_no > t.tbl_end_col_no);

-- 같은 table의 수정범위가 겹치는 경우: 결과 0건
SELECT a.mod_scop_id, b.mod_scop_id
FROM tmm_mdlinptmodscopd a
JOIN tmm_mdlinptmodscopd b
  ON b.inpt_tbl_id = a.inpt_tbl_id AND b.mod_scop_id > a.mod_scop_id
WHERE a.del_yn = 'N' AND b.del_yn = 'N'
  AND a.bgn_row_no <= b.end_row_no AND b.bgn_row_no <= a.end_row_no
  AND a.bgn_col_no <= b.end_col_no AND b.bgn_col_no <= a.end_col_no;
```

### DBA action / deployment note

- `crt_*` 대 `reg_*` 감사 컬럼 표준을 확정한다.
- generated column/unique 지원과 이름 충돌을 확인한다.
- seed SQL과 3개 실제 workbook 검증 결과를 함께 승인한다.
- catalog read application 배포 전 ACTIVE seed와 legacy mapping 정책을 확인한다.

## 11. Recommendation

기존 2단계 `template → table` 초안 대신 5단계 `template → file → sheet → table → scope`를
채택한다. 실제 파일 저장 table과 catalog를 분리하고, 한 case가 사용한 template set을 고정하면
현재 실행과 과거 실행을 같은 규칙으로 재현할 수 있다.

## 12. Required SQL / DBA action

- 위 DDL은 draft이며 DBA 승인 전 적용하지 않는다.
- initial seed SQL은 실제 편집 대상 table 경계를 확정한 뒤 별도로 작성한다.
- case mapping backfill은 자동 파일명 매칭만으로 실행하지 않는다.

## 13. Risks / open questions

- 행/열 append와 중간 insert 중 어느 동작을 허용할지 미정이다.
- 새 행/열의 수식·서식·validation 복제 규칙이 미정이다.
- 구조 hash의 정규화/판별 방식이 미정이다.
- `BaseEntity`와 DB 명명 문서의 감사 컬럼 표준이 충돌한다.
- ACTIVE child soft delete 및 version activation을 수행할 운영 주체/API가 미정이다.

## 14. Normalization assessment

| 정규형 | 결과 |
| --- | --- |
| 1NF | Pass. 배열/JSON 반복 그룹 없이 파일·시트·테이블·범위를 row로 분리했다. |
| 2NF | Pass. 모든 business table이 단일 PK이며 non-key가 해당 PK 전체에 종속된다. |
| 3NF | Pass. file name은 file, sheet name은 sheet, 좌표는 table/scope에만 저장한다. |
| BCNF | Pass. version/role/sibling order의 determinant는 PK 또는 candidate key다. |

## 15. Index strategy recommendation

- low-cardinality `del_yn`, status, add flag 단독 index는 만들지 않는다.
- hierarchy FK는 sibling UK의 첫 column이므로 별도 중복 index를 만들지 않는다.
- 구조 판별은 selectivity가 높은 hash를 선두로 둔 `(file_strt_hash_val, mdl_file_cd, del_yn)`을 사용한다.
- case template 역조회는 `(tmplt_id, del_yn)`을 사용한다.
- 실제 데이터가 65 sheets 수준이므로 covering index를 선제 추가하지 않는다.
- nullable hash도 MySQL index에 포함되지만 조회는 non-null hash equality로 제한한다.

## 16. Query optimization notes

- hierarchy 전체 조회는 root `tmplt_id` equality에서 시작하므로 full scan이 필요 없다.
- application 구현 후 `EXPLAIN`에서 각 child join의 `key`가 `_ux01` 또는 `_ux02`인지 확인한다.
- 목록 크기가 작아 pagination/partitioning/aggregate 최적화는 필요 없다.
- JPA collection 순회로 N+1이 생기지 않도록 projection 또는 batch 조회를 사용한다.

## 17. Dictionary updates

- added: `시트 / SHT / 단일단어`
- reason: Excel file-sheet-table 계층의 필수 business term이다.
- path: `docs/conventions/db-term-dictionary-sheet2.csv`
- append: 385번 row, 기존 row 변경 없음.
- 미등록 `편집`, `규칙`은 사용하지 않고 등록 용어 `수정(MOD)`, `범위(SCOP)`로 대체했다.

## 18. Implementation readiness

- ERD/DDL review verdict: `REVISE`.
- hierarchy/keys/types/index 방향은 구현 입력으로 충분하다.
- DBA apply와 행·열 추가 실행 API는 감사 컬럼 표준, 추가 방식, 복제 규칙을 해결한 뒤 `ready`로 전환한다.
- current readiness: `blocked by open questions` for write/execute implementation;
  read-only catalog schema/entity drafting is `ready after minor cleanup`.
