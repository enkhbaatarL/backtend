# MI Model Input Template Catalog ERD / DDL Draft

## Document Control

- status: Draft
- canonical for: Schema rollout — model input template catalog tables, columns, keys, indexes, nullability, migration, rollback, and verify SQL; §6.1/§10A의 기존 불변 파일 참조와 §6.2/§10B의 발행 양식/catalog·Case 연결 및 §6.3의 Draft 공통 저장 물리 정의. 기존 공통 파일 table 전체 DDL은 재정의하지 않는다.
- owner: BE / DBA
- source documents:
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/references/mi-model-input-catalog-inventory-2026-09-28.md`
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - 이 문서의 2026-09-04 `양식 → 테이블` 2단계 초안
- last synced: 2026-10-16 (기존 코드/문서 기준 타입·저장/연동 대조 §6.4; 실제 DB 확인은 §13 DBA-01~05로 분리)
- open questions:
  - Q2 §6.2/§10B의 기존 catalog table/승인 GET 정의·seed·발행과 신규/기존 Case-template 관계 등록 결과를 확인해야 한다. field/control/options/화면 규칙의 실제 저장 구조/정의는 기존 후속이며 root ID만으로 완비를 가정하지 않는다.
  - 3파일 `SCENARIO` legacy cleanup의 apply/verify/rollback DML(case master + run history + 연결 execution-preparation + active 가격전망해석)은 미작성이다. `INPUT-11` 착수 시 작성해 DBA 검토를 받는다.
  - domain 규칙 47의 LiqUtilAdjustment 행·열 삭제 허용 정책을 카탈로그에 저장할 물리 표현을 확정해야 한다. 현재 SQL의 행·열 추가 플래그만으로 삭제 허용을 표현하지 않는다.
  - domain 규칙 46에서 확정한 별도 화면 갱신 규칙의 DB 저장 단위, 양식 version/표/필드 연결, 물리 테이블·컬럼·키는 후속 설계 대상이다. 아래 기존 6개 table DDL은 아직 이 저장 구조를 포함하지 않는다.
  - 이번 release의 구조 변경 포함과 마지막 행·열 추가 중심 원칙은 domain 규칙 13·26에서 확정했다. 표별 추가 경계와 중간 삽입·삭제 예외를 저장할 물리 표현, 신규 셀의 수식·서식 복제 규칙은 미정이다.
  - `BaseEntity`의 `crt_*` 감사 컬럼과 DB 명명 문서의 `reg_*` 표준 충돌을 DBA/데이터 표준 담당자가 확인해야 한다.
  - 운영 DB의 `tmm_mi_model_param_m` 현재 값은 확인되지 않았다. 이 문서의 대조는 저장소 DDL/seed 기준이다.
  - 개발 전용 시트가 없는 실제 운영 승인본 4개를 아직 계측하지 않았다. seed 수와 구조 hash는 승인본 검증 후 확정해야 한다.

## Purpose

독립수급모델 고도화 화면에서 input Excel 4개를 시트/테이블 단위로 표시·편집하려면
"어느 파일에 무슨 시트가 있고, 그 안에 어떤 표가 어떤 형태로 있는지"를 서버가 알아야 한다.
이 문서는 그 카탈로그와 **양식 버전**의 물리 스키마를 소유한다.
Q2의 기존 불변 파일 등록 참조와 추가 스키마 불필요 판단은 §6.1·§10A에서 소유한다.

API 계약은 이 문서가 정의하지 않는다. 실행/케이스 스키마는
[scenario-case-execution-erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md)가 소유한다.

## 배경 — 왜 버전이 필요한가

양식은 이미 바뀌었고 앞으로도 바뀐다. 2026-09-04 실측 결과다.

| 파일 | basecase 템플릿 시트 | 실제 case 파일 시트 |
| --- | ---: | ---: |
| demand | 13 | 25 |
| supply | 14 | 23 |
| trade | 8 | 17 |
| output | 신규 | 5 (운영 예상) |

2026-09-28 첨부 개발본의 output은 7개 시트지만 `SUMMARY`, `플랫폼 입력 안내` 2개는
실제 운영 인풋에 없다. 다른 3역할도 같은 개발 전용 시트를 합계 4개 포함한다.

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

2026-09-28 첨부 개발본 4파일의 규모와 개발 전용 시트를 제외한 운영 예상 규모는 아래와 같다.
역할별 상세와 기존 3파일 대비 변화는
`docs/references/mi-model-input-catalog-inventory-2026-09-28.md`가 소유한다.

| 항목 | 첨부 개발본 | 운영 예상 |
| --- | ---: | ---: |
| 시트 | 76 | 70 |
| 값 보유 셀 | 4,212,857 | 4,210,446 |
| 수식 셀 | 1,863,766 | 1,863,766 |
| 외부 workbook 참조 수식 셀 | 42,394 | 42,394 |

운영 예상값은 첨부본에서 개발 전용 6개 시트만 제외하고 나머지는 같다는 가정이다.
실제 운영 승인본을 아직 직접 계측하지 않았으므로 template seed/hash 확정 전에 재검증한다.

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
- Q2: `UploadedFile.fileHashVal`은 Java mapping상 길이20이며 외부 입력 hash의 저장/중복 조회에 사용한다. Case 직접 업로드와 내부 staged 업로드는 이 값에 null을 전달한다. `tmd_mdl_inptfiled.file_strt_hash_val`은 양식 구조 해시라 파일 내용 기준의 저장 위치가 아니다.
- 공통 파일의 전체 CREATE DDL은 repo 및 확인한 로컬 0014/0047 SQL에 없다. 기존 파일/Case 관계는 현재 entity/repository와 Case DDL의 참조를 따른다. Q2를 위해 그 공통 table을 재정의하거나 ALTER하지 않는다.

## 3. Change classification

- 분류: catalog는 `new table`, Q2 파일 내용 식별은 `no schema change`
- 범위: 기존 catalog 신규 master/detail/relation 6개. Q2는 기존 파일 등록/객체 참조 사용
- 기존 table alter: 없음. Q2용 내용 해시/등록 시각 컬럼 추가안은 철회(§6.1)
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

- 한 양식 version은 서로 호환되는 `DEMAND/SUPPLY/TRADE/OUTPUT` 파일 set이다.
- `SUMMARY`와 `플랫폼 입력 안내`는 개발 전용이므로 runtime sheet definition과 구조 hash 대상에 포함하지 않는다.
- 파일·시트·테이블·수정범위를 각각 별도 detail table로 정규화한다.
- 좌표는 1-based 숫자이며 행은 `INT UNSIGNED`, 열은 `SMALLINT UNSIGNED`다.
- 사용자 수정 가능 범위는 여러 직사각형 row로 저장한다. 임의 JSON 좌표는 사용하지 않는다.
- 행/열 추가 가능 여부는 table 정책 컬럼으로 저장한다.
- case는 사용한 template set 한 건을 relation table로 고정한다.
- Q2는 domain §3.2의 불변 파일 등록을 사용하고 추가 table/column/index·내용 기준 backfill이 없다. 기존 외부 입력 hash와 catalog 구조 hash도 변경하지 않는다.

## 6. Proposed schema change

별도 화면 갱신 규칙의 DB 저장 요구와 소유권은 domain 규칙 46을 따른다. 아래 SQL은 기존 6개 catalog table 초안이며, 갱신 규칙의 저장 단위·테이블·컬럼·키를 확정하기 전에는 해당 요구를 지원하는 완성된 DDL로 취급하지 않는다.

명명은 [db-object-naming-rules.md](../../conventions/db-object-naming-rules.md)와
[db-term-dictionary-sheet2.csv](../../conventions/db-term-dictionary-sheet2.csv)를 따른다.
`시트/SHT`는 이번 설계에서 사전에 추가했다. 감사 컬럼은 JPA `BaseEntity`와 가장 가까운
`tmd_mdl_scnrcasem` 패턴을 따라 `crt_*`를 사용한 초안이며, 표준 충돌은 적용 전에 해결한다.

```sql
CREATE TABLE tmd_mdl_inpttmpltm
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

CREATE TABLE tmd_mdl_inptfiled
(
    inpt_file_id        INT UNSIGNED AUTO_INCREMENT COMMENT '입력파일ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    del_yn              CHAR(1) DEFAULT 'N' NOT NULL COMMENT '삭제여부',
    tmplt_id            INT UNSIGNED NOT NULL COMMENT '입력템플릿ID',
    mdl_file_cd         VARCHAR(20) NOT NULL COMMENT '모델파일코드 (DEMAND/SUPPLY/TRADE/OUTPUT)',
    file_nm             VARCHAR(255) NOT NULL COMMENT '기준파일명',
    file_ord            SMALLINT UNSIGNED NOT NULL COMMENT '파일순서',
    file_type_cd        VARCHAR(20) NOT NULL COMMENT '파일유형코드 (XLSX/XLSM)',
    file_strt_hash_val  CHAR(64) NULL COMMENT '파일구조해시값',
    CONSTRAINT tmm_mdlinptfiled_ux01 UNIQUE (tmplt_id, mdl_file_cd),
    CONSTRAINT tmm_mdlinptfiled_ux02 UNIQUE (tmplt_id, file_ord),
    CONSTRAINT tmm_mdlinptfiled_fk01 FOREIGN KEY (tmplt_id)
        REFERENCES tmd_mdl_inpttmpltm (tmplt_id),
    CONSTRAINT tmm_mdlinptfiled_ck01 CHECK (mdl_file_cd IN ('DEMAND', 'SUPPLY', 'TRADE', 'OUTPUT')),
    CONSTRAINT tmm_mdlinptfiled_ck02 CHECK (file_type_cd IN ('XLSX', 'XLSM')),
    CONSTRAINT tmm_mdlinptfiled_ck03 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력파일상세';

CREATE INDEX tmm_mdlinptfiled_ix01
    ON tmd_mdl_inptfiled (file_strt_hash_val, mdl_file_cd, del_yn);

CREATE TABLE tmd_mdl_inptshtd
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
        REFERENCES tmd_mdl_inptfiled (inpt_file_id),
    CONSTRAINT tmm_mdlinptshtd_ck01 CHECK (sht_stat_cd IN ('VISIBLE', 'HIDDEN', 'VERY_HIDDEN')),
    CONSTRAINT tmm_mdlinptshtd_ck02 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMM_모델입력시트상세';

CREATE TABLE tmd_mdl_inpttbld
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
        REFERENCES tmd_mdl_inptshtd (inpt_sht_id),
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

CREATE TABLE tmd_mdl_inptmodscopd
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
        REFERENCES tmd_mdl_inpttbld (inpt_tbl_id),
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
        REFERENCES tmd_mdl_inpttmpltm (tmplt_id),
    CONSTRAINT tmd_mdl_scnrcasetmpltd_ck01 CHECK (del_yn IN ('Y', 'N'))
) COMMENT 'TMD_시나리오케이스입력템플릿상세';

CREATE INDEX tmd_mdl_scnrcasetmpltd_ix01
    ON tmd_mdl_scnrcasetmpltd (tmplt_id, del_yn);
```

### 6.1 Q2 내용 식별값의 보관 위치

원본 불변식과 등록 성공 경계는 [domain §3.2](mi-model-input-template-catalog-domain-model-design.md#32-저장-case-조회의-원본-기준-등록--2026-10-15-구체화)를 따른다. 사용자 확인에 따라 업로드마다 새 파일 row/UUID 객체를 만들고 등록된 원본의 경로/내용은 바뀌지 않는다. 따라서 기존 파일 등록 자체를 불변 내용 버전으로 사용하며 **원본 내용 식별용 추가 스키마는 없다**. 양식/catalog 연결은 기존 catalog/관계 초안의 적용·등록이 필요한 §6.2/§10B와 구별한다.

| 기존 보관 위치 | Q2에서의 사용 |
| --- | --- |
| `tcm_filel.file_no` | 서버가 확인한 불변 파일 등록 식별값. 등록과 객체 참조의 일대일 대응을 확인해 사용 |
| `tcm_filel.strg_path_nm`·`strg_file_nm` | 실제 저장 객체 참조를 서버가 확인하는 기존 metadata. 임시 URL을 버전으로 사용하지 않음 |
| `tcm_filel.file_rel_obj_spt_cd/file_rel_obj_dstg_no` | 해당 Case의 현재 유효 파일 연결. 정확한 네 역할과 객체를 조회 기준에 결합 |
| `tcm_filel.file_hash_val` | 기존 외부 입력 hash/중복 조회 용도를 유지. 서버 내용 기준값으로 변경하지 않음 |
| `tmd_mdl_inptfiled.file_strt_hash_val` | 기존 양식 구조 hash. 실제 파일 내용 hash와 구별 |

앞서 제안한 `file_cntt_hash_val`·`file_cntt_hash_reg_dtm` 추가와 등록 pair/전용 UPDATE 설계는 철회했다. 해당 ALTER·backfill·복구 DROP SQL은 실행하지 않았으며 이 문서의 활성 SQL에서도 제거했다. 새 table/column/index/FK·내용 해시 알고리즘/등록 시각 저장·JPA 추가 mapping은 요구하지 않는다.

`contentRevision`의 내부 구성은 API 상세 §4.4가 소유한다. 기존 파일 등록의 불변성이 확인된 전제에서 사용하는 버전 참조이며, metadata만 같다는 사실로 임의의 mutable 객체를 불변이라고 판단하는 규칙이 아니다. Case-template/catalogRevision 연결안은 §6.2·§10B에 구체화했으며 실제 적용/등록·제품 연결은 후속이다.

### 6.2 Q2 templateVersion·catalogRevision의 물리 연결

발행/불변·Case 연결 시점/기존 Case 정책은 [domain §3.3](mi-model-input-template-catalog-domain-model-design.md#33-q2-양식catalog-발행-묶음과-case-연결--2026-10-15-구체화)가 소유한다. 내부 문자열 구성은 조회 상세 §4.6을 따른다. 현재 최소안은 이 문서에 이미 있는 table/column/key를 사용하며 새 revision column/table을 추가하지 않는다.

| 역할 | 기존 DDL 보관 위치 | 제약/사용 |
| --- | --- | --- |
| 양식 발행 식별값 | `tmd_mdl_inpttmpltm.tmplt_id` | 양의 INT UNSIGNED PK. 한 번 발행한 내용/버전과 대응 유지 |
| 양식 버전 번호 | `tmd_mdl_inpttmpltm.tmplt_ver_no` | SMALLINT UNSIGNED NOT NULL. `(mdl_spt_cd, tmplt_ver_no)` unique로 모델별 중복 방지 |
| 발행 catalog 묶음 | 위와 같은 `tmplt_id` | file→sheet→table→scope FK 경로로 소속. 후속 field/control/options/화면 규칙도 같은 버전에 소속돼야 함 |
| Case의 고정 양식 | `tmd_mdl_scnrcasetmpltd(case_id, tmplt_id)` | case_id PK로 Case당 한 연결. Case/양식 physical FK와 기존 tmplt_id index 유지 |

catalogRevision은 별도 숫자 카운터가 아니라 불변 발행 묶음의 참조다. `mod_dtm`·현재 ACTIVE flag·최신/MAX 버전·파일 구조 hash를 대체값으로 사용하지 않는다. 단순 FK/PK가 catalog 불변을 강제하는 것은 아니며 발행/seed writer가 domain 규칙3·§3.3을 지켜야 한다. 발행 뒤 수정은 새 tmplt_id/버전의 INSERT로 처리하고 기존 하위 row를 UPDATE/soft delete해 해석을 바꾸지 않는다. ACTIVE→DEPRECATED 상태 변경은 허용된 lifecycle이며 기존 참조를 유지한다.

기존 JPA의 Case·UploadedFile은 master persistence unit에 있다. 이 초안의 catalog/관계도 같은 master 저장 경계와 기존 FK 전제를 사용한다. 새 tenant router/교차 DB join·분산 transaction을 도입하지 않는다. 실제 적용 DB/seed·FK/감사 컬럼 확인은 기존 DBA 절차를 유지한다. 이번 조사에서는 table/seed 적용을 확인하거나 수행하지 않았다.

현재 6개 CREATE는 field/control/options/화면 규칙의 모든 물리 표현을 포함하지 않는다. 따라서 root ID만 채워 QRY를 완료로 선언하지 않는다. GET에 필요한 승인 정의를 같은 발행 묶음에 등록하는 후속 DDL/seed는 기존 Q5/INPUT-03 범위이며 이 절에서 그 table 구조를 임의로 추가하지 않는다.

### 6.3 공통 Draft 혼합 저장 물리 정의 — 2026-10-16 Draft

이번 공통 설계의 저장 분리는 아래 **7개 신규 테이블**을 기준으로 한다. 기존6개 catalog DDL·§6.1~6.2/§10A~10B는 유지한다. 이는 물리 설계 정본의 정의이며 실행 가능한 전체 CREATE/ALTER 패키지·DBA 적용 승인은 아니다. 실제 DB 존재를 가정하지 않는다. lifecycle/복구는 [domain §3.4·§5.1~5.3](mi-model-input-template-catalog-domain-model-design.md), endpoint/DTO는 [편집 API §9](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md)가 소유한다. 표별 field/options/구조·파생 정책 저장은 별도 후속이며 아래 공통 구조 확정의 선행조건이 아니다.

#### 6.3.1 공통 타입·경계

- 신규 테이블은 기존 Case/파일과 같은 **master persistence** 경계를 사용한다. tenant DB에 분산하거나 DB 간 transaction으로 만들지 않는다. tenant는 권한 확인된 회사번호 `cust_cpny_no MEDIUMINT UNSIGNED`, 소유자는 `usr_no INT UNSIGNED`로 보관한다. 요청 header만으로 회사/DB를 선택하지 않으며 worker는 확인된 actor·role·tenant 문맥을 복원/정리한다. 접수한 actor login은 요청 감사 컬럼, owner/회사번호는 Draft, 접수 역할은 요청 usr_role_cd로 보관해 QUEUED 재시작에도 JWT/요청 header를 재사용하지 않고 복원한다. 현재 허용 권한/원본 접근을 서버에서 다시 대조하며 role 문자열만으로 권한을 새로 부여하지 않는다.
- 아래 `UUID`는 `CHAR(36) CHARACTER SET ascii COLLATE ascii_bin`, 소문자 UUID만 허용한다. revision은 `BIGINT UNSIGNED` + `0..9007199254740991` CHECK, 1기반 행번호는 `INT UNSIGNED` + `1..1048576` CHECK다. ID/코드의 equality는 대소문자 무시 collation에 의존하지 않는다.
- 표시한 PK/required는 NOT NULL, `?`만 NULL 허용이며 임의 DEFAULT0을 두지 않는다. 감사 컬럼은 기존6개/`BaseEntity`와 같은 `crt_usr_logn_id/mod_usr_logn_id VARCHAR(50)`, `crt_dtm/mod_dtm DATETIME(6)`, `del_yn CHAR(1) DEFAULT 'N'`을 사용한다. 운영 감사 `reg_*` 표준 예외 승인은 §13의 DBA 적용 전 확인으로 남긴다. 성공 이력/요청/참조에 CASCADE DELETE·독립 soft delete/자동 TTL을 두지 않는다.
- `sourceVersion`은 원본 등록/양식 참조로 계산한 `VARCHAR(68) ascii_bin`을 고정하고 새 원본 내용 SHA/revision은 만들지 않는다. sourceKey는 기존 식별 규칙으로 파생한다. 원본 fileNo는 `tcm_filel.file_no BIGINT UNSIGNED`, Case ID는 `tmd_mdl_scnrcasem.case_id INT UNSIGNED`, template/table ID는 기존 `INT UNSIGNED`와 타입을 맞춘다.

#### 6.3.2 테이블·컬럼·참조

| 물리 테이블 / PK | 필수 컬럼 | NULL 허용 컬럼·책임 |
| --- | --- | --- |
| `tmd_mdl_inptdrftm` / `drft_id UUID` | `cust_cpny_no`, `usr_no`, `org_case_id INT UNSIGNED`, `tmplt_id INT UNSIGNED`, `org_rvsn_val VARCHAR(68)`, `drft_stat_cd VARCHAR(20)`, `rvsn_no BIGINT UNSIGNED` | `actv_prcs_id UUID?`(진행 포인터), `fnsv_case_id INT UNSIGNED?`(최종 Case). DRAFT/FINALIZED/DISCARDED CHECK. FINALIZED와 최종 Case 존재는 동치, 종료 Draft는 진행 포인터 NULL |
| `tmd_mdl_inptdrftfiled` / `(drft_id, mdl_file_cd)` | `drft_id UUID`, `mdl_file_cd VARCHAR(20) ascii_bin`, `file_no BIGINT UNSIGNED` | 없음. SUPPLY/DEMAND/TRADE/OUTPUT CHECK, Draft의 고정 원본4개. 같은 Draft/file_no UK로 한 파일의 역할 중복 금지 |
| `tmd_mdl_inptdrftprcsl` / `prcs_id UUID` | `drft_id UUID`, `req_id UUID`, `prcs_type_cd VARCHAR(20)`, `usr_role_cd VARCHAR(20)`, `base_rvsn_no BIGINT UNSIGNED`, `prcs_stat_cd VARCHAR(20)`, `json_rvsn_no SMALLINT UNSIGNED`(현재1), `req_json LONGTEXT`, `expr_dtm DATETIME(6)` | `try_tokn UUID?`, `bgn_dtm/end_dtm DATETIME(6)?`, `rslt_json/fail_json LONGTEXT?`. 종류 CHANGE/FINALIZE, 상태 QUEUED/RUNNING/SUCCEEDED/FAILED CHECK. 접수시각 crt_dtm+120초가 expr_dtm. RUNNING은 token/시작시각 필수, 종료는 종료시각 필수. 성공은 result만, 실패는 failure만 존재 |
| `tmd_mdl_inptdrftrvsnl` / `(drft_id, rvsn_no)` | `drft_id UUID`, `rvsn_no BIGINT UNSIGNED`, `json_rvsn_no SMALLINT UNSIGNED` | `base_rvsn_no BIGINT UNSIGNED?`, `prcs_id UUID?`, `dlta_json LONGTEXT?`. revision0은 세 nullable 값 모두 NULL; n>0은 parent=n-1/성공 CHANGE 작업/delta 모두 필수. 사용자 명령·그 성공의 승인 파생/구조 결과를 한 payload로 보관 |
| `tmd_mdl_inptdrftrowm` / `row_id UUID` | `drft_id UUID`, `inpt_tbl_id INT UNSIGNED` | `org_row_no INT UNSIGNED?`, `prcs_id UUID?`, `clnt_row_id VARCHAR(64) ascii_bin?`. 원본은 org_row_no만, 신규는 성공 ADD의 prcs_id/clientRowKey만 존재(CHECK). clientRowKey는 API의 영문 식별 문자열이며 UUID가 아님. 업무값을 ID/UK로 사용하지 않음 |
| `tmd_mdl_inptdrftrowh` / `(row_id, bgn_rvsn_no)` | `row_id UUID`, `drft_id UUID`, `inpt_tbl_id INT UNSIGNED`, `bgn_rvsn_no BIGINT UNSIGNED` | `end_rvsn_no BIGINT UNSIGNED?`, `row_no INT UNSIGNED?`. [시작 revision, 종료 revision) 동안의 위치. 종료 NULL은 현재 대응. row_no NULL은 삭제 tombstone이며 행ID 재사용 없음. 비NULL 종료는 시작보다 커야 함 |
| `tmd_mdl_inptfnsvfiled` / `(prcs_id, try_tokn, mdl_file_cd)` | `prcs_id UUID`, `try_tokn UUID`, `mdl_file_cd VARCHAR(20) ascii_bin`, `strg_ref_json LONGTEXT`, `prcs_stat_cd VARCHAR(20)` | `file_size BIGINT UNSIGNED?`, `file_no BIGINT UNSIGNED?`. FINALIZE 시도별4역할. 상태 PREPARED/UPLOADED/COMMITTED/CLEANUP_PENDING/CLEANED CHECK. COMMITTED만 성공 file_no 필수, 다른 상태는 NULL. 서버 생성 private 저장 참조·필요한 metadata, 업로드/정리 추적 책임 |

물리 FK(모두 RESTRICT, 같은 master): Draft→기존 Case/양식·회사(`tmd_custcpnym`)/사용자(`tmd_usrm`), 고정파일→Draft/`tcm_filel`, 요청→Draft, 성공revision→Draft/부모revision/성공 요청, 행→Draft/catalog 표/추가 요청, 행위치→행/시작·종료revision, 최종준비파일→요청/성공 `tcm_filel`이다. Draft의 최종 Case도 기존 Case FK다. 원본/최종 Case FK는 서로 다른 컬럼이며 원본 Case를 수정하지 않는다. 최종준비파일의 fileNo는 역할/시도 간 중복될 수 없도록 NULL 허용 `_ux01(file_no)`도 둔다. 서버 시도 토큰은 요청마다 한 번 배정하며 RUNNING 자동 재실행이 없으므로 같은 요청의 두 시도를 동시에 진행시키지 않는다.

동일 Draft 소속을 보장하려고 요청에 `(drft_id, prcs_id)`, 행에 `(drft_id, inpt_tbl_id, row_id)` UK를 두고 성공revision/신규행/행위치의 복합 FK에서 재사용한다. 행위치의 시작/종료는 `(drft_id, revision)` FK다. Draft의 진행 포인터/현재 성공 번호는 순환 삽입 제약을 피하려고 논리 참조로 두고 동일 transaction의 조건 갱신과 검증 쿼리로 정합성을 보장한다. 표가 Draft의 고정 양식 소속인지, 참조 작업이 성공 CHANGE/FINALIZE인지 같은 상위 의미 제약도 FK만으로 보장됐다고 보지 않고 저장 시 대조한다.

#### 6.3.3 유일성·조회 index·성공 확정

| 테이블 | 추가 UK/index·강제할 경계 |
| --- | --- |
| Draft | nullable generated `actv_org_case_id = IF(del_yn='N' AND drft_stat_cd='DRAFT',org_case_id,NULL)`. `_ux01(cust_cpny_no,usr_no,actv_org_case_id)`로 진행 Draft1건. `_ux02(fnsv_case_id)`로 최종 Case 역연결1건. 원본 변경 sourceVersion을 UK에 넣어 두 번째 진행 Draft를 만들지 않음 |
| 요청 | `_ux01(drft_id,req_id)`는 CHANGE/FINALIZE 공통 중복 키. nullable generated `actv_drft_id = IF(prcs_stat_cd IN ('QUEUED','RUNNING'),drft_id,NULL)`, `_ux02(actv_drft_id)`로 진행 요청1건. `_ux03(drft_id,prcs_id)`는 복합 FK용. `_ix01(prcs_stat_cd,expr_dtm)`는 timeout/재시작 복구 조회용이며 상태 단독 index를 추가하지 않음 |
| 성공revision | PK가 revision 중복/순서 조회 보장. `_ux01(prcs_id)`로 한 성공 CHANGE당 revision1건. parent 참조는 같은 Draft에서만 허용 |
| 행 | `_ux01(drft_id,inpt_tbl_id,org_row_no)`로 원본 행ID1개, `_ux02(prcs_id,clnt_row_id)`로 성공 추가ID1개, `_ux03(drft_id,inpt_tbl_id,row_id)`는 복합 FK용 |
| 행위치 | generated `actv_row_id = IF(end_rvsn_no IS NULL,row_id,NULL)`의 `_ux01(actv_row_id)`로 현재 대응1건. generated `actv_row_no = IF(end_rvsn_no IS NULL,row_no,NULL)`의 `_ux02(drft_id,inpt_tbl_id,actv_row_no)`로 현재 활성 위치 중복 금지. `_ix01(drft_id,inpt_tbl_id,bgn_rvsn_no)`로 revision별 위치 조회 |
| 최종준비파일 | PK가 같은 시도/역할1건, `_ix01(prcs_stat_cd,mod_dtm)`는 정리 재처리용. 전용 저장 참조는 서버가 operation/시도/역할로 생성, 원본이나 다른 시도의 key와 재사용 금지 |

기존 등록 명명 규칙대로 각 제약/index에 테이블명+`_uxNN/_ixNN/_fkNN/_ckNN`을 붙인다. generated helper 타입은 참조 Case INT/Drft·행 UUID와 동일하다. MySQL nullable UK는 과거 종료행의 NULL 중복을 허용하는 용도로만 사용하고 활성 제약 값은 NULL이 될 수 없도록 상태/필수 CHECK와 함께 검증한다.

- Draft 생성은 원본4역할·revision0·실제 원본 행ID/위치0을 함께 commit한다. 업무값 변경/계산은 하지 않는다. 동시 생성 UK 충돌은 승자 Draft를 재조회하고 sourceVersion을 대조한다.
- 접수는 Draft row 잠금 아래 requestId 기존 기록을 먼저 확인하고, QUEUED 요청/진행 포인터를 함께 commit한다. 업로드나 계산 전에 접수를 durable하게 만든다. 메모리 queue만을 접수 증거로 사용하지 않는다.
- CHANGE 성공은 동일 Draft/현재revision/진행요청/RUNNING·시도토큰/기한 조건을 확인하고 성공revision/delta·신규행·위치이력·현재revision/작업결과·슬롯 해제를 한 transaction에서 commit한다. 위치 변경은 이전 current 구간을 먼저 닫고 새 구간을 넣어 UK 일시 충돌을 피한다. 같은 transaction에서 중복 업무키도 최종 활성 행 상태 기준으로 검사하므로 Draft별 쓰기 직렬화가 경합 수단이다. 표별 업무키를 범용 행UK로 강제하거나 기존 중복을 전수 청소하지 않는다.
- FINALIZE 성공은 domain §5.2의 **새 Case·파일4건·Case-template·FINALIZED·작업성공·준비파일 COMMITTED**를 같은 transaction에서 확정한다. 성공revision은 불변이다. source 접근/기준·현재revision과 시도/기한이 바뀌면 rollback한다. Case/파일 등록을 먼저 commit하고 후속 transaction에서 Draft를 연결하지 않는다.
- 실패/timeout/복구는 동일 row 잠금/조건 갱신으로 직렬화한다. terminal 요청을 재실행하지 않으며 실패 이후 token은 성공 확정에 사용할 수 없다. 네트워크 오류로 commit 여부가 불명하면 쓰기 DB의 같은 row 잠금 아래 재조회로 확정하기 전 파일 삭제/실패 전환을 금지한다. 캐시/읽기 복제본의 미발견을 rollback 증거로 쓰지 않는다. RUNNING worker 종료는 실패 확정 후 새 요청 재시도이고 자동 이어계산은 하지 않는다.

#### 6.3.4 검증된 JSON text·복원·보존

수치 보존을 위해 `req_json/dlta_json/rslt_json/fail_json/strg_ref_json`은 **utf8mb4 LONGTEXT**로 저장한다. MySQL JSON 숫자 변환이나 DOUBLE/고정 scale DECIMAL로 사용자 수치를 바꾸지 않는다. LONGTEXT는 임의 문자열 허용이라는 뜻이 아니며 DB에는 JSON_VALID CHECK, 앱에는 버전별 허용 key/enum/타입·중복 key 거부·깊이/bytes 검증이 필요하다. DB JSON 연산/path index를 사용하지 않는다. JSON 숫자는 Java BigDecimal 등 정확한 10진 표현으로 읽고 쓰며 원본값 변환/반올림은 하지 않는다.

| payload | schemaVersion1 / UTF-8 영속 상한·검증 |
| --- | --- |
| 요청 | 1 MiB. 공개 body 한도와 같으며 내부 envelope는 별도 json_rvsn_no/관계형 컬럼 사용. CHANGE 명령 의미/배열 순서 또는 FINALIZE의 정규화된 title/description과 baseRevision을 보관. 숫자3.20/3.2는 같은 값, 문자열은 계약상의 정규화 외 그대로. 종류도 멱등 비교에 포함 |
| 성공 delta | 16 MiB/성공revision. 순서 있는 정규 명령, 승인 정책 참조, 바뀐 파생값/구조 효과·검증 결과만 보관. 내부 깊이16·단일 문자열4096자(승인 수식 등 내부 생성 문자열 포함), 사용자 제한은 API의 더 좁은 기준을 유지. 전체 workbook/모든 cached cell 복사 금지 |
| 작업 결과/안전 오류 | 각각1 MiB. 공개 결과를 재구성할 resultRevision/rowMappings/affectedTableIds 또는 최종 Case/4역할 fileNo, 안전한 Problem. 원문 예외/거부값 금지 |
| 준비 파일 저장 참조 | 역할별16 KiB·깊이4. 서버 생성 bucket/key·회사/암호화 문맥 참조·파일명/MIME 등 기존 metadata. 클라이언트 입력 경로·평문 bytes·키 비밀은 저장하지 않음 |

이 값은 공통 저장의 초기 기술 상한이며 업무 수치 소수 자릿수 제한이 아니다. 상한을 넘는 내부 성공 결과는 PERSISTENCE_FAILED로 요청 전체 실패 처리하고 직전 성공을 보존한다. 조용한 절삭/반올림/부분 저장은 금지한다. DBA는 최대 payload+SQL/프로토콜 여유가 가능한 `max_allowed_packet`/드라이버 설정을 pre-check해야 한다. 실제 처리량 검증 없이 이 상한에서 성능 Pass를 주장하지 않는다.

revision0은 참조만 보유한다. n>0의 delta를 순서대로 합성하고 행위치 구간을 대조해 현재 Draft 값을 복원하며 조회를 위해 매번 계산하지 않는다. 최종저장은 이 성공 명령 chain으로 원본4개를 다시 materialize/검증한다. 실패 요청은 replay에서 제외한다. 내부 schemaVersion을 읽을 수 없으면 PERSISTENCE_FAILED이며 최신 schema로 추정 해석하지 않는다. snapshot/checkpoint 추가·전체셀 EAV·새 범용 JSON 질의 계층은 이번 구조에 포함하지 않는다.

Draft 존속 중 기준 참조/성공 chain/행이력/종료 요청·중복 키와 결과를 함께 보존하며 자동 purge를 만들지 않는다. 완료/실패 기록의 독립 soft delete로 UK를 비우지 않는다. 장기 archive/폐기 도입 시 별도 보존 계약이 필요하다. 준비 파일은 업로드 전에 참조 기록, 실패 뒤 CLEANUP_PENDING, 실제 전송 종료와 object 삭제 확인 뒤 CLEANED다. 정리 실패/늦은 업로드는 기록을 보존해 재처리하며 성공 Case의 COMMITTED object는 지우지 않는다. 평문 작업사본은 이 저장의 대안이 아니며 domain 규칙63의 삭제 기준을 따른다.

#### 6.3.5 적용 준비·검증 경계

- **Pre-check:** 실제 master의 catalog6개/Case·파일·회사·사용자 타입·engine/collation/FK와 감사 예외, packet 여유를 확인한다. 현재 데이터에 새 Draft는 없다는 전제는 운영 확인 전 사실로 단정하지 않는다. 신규7개 이름 충돌/동일 정의 존재 여부와 nullable generated UK·경합을 staging MySQL에서 검증한다.
- **Apply 초안 순서:** Draft(외부 FK) → 원본 참조/요청 → 성공revision → 행 → 행위치 → 준비파일. 복합 UK와 parent revision FK를 함께 작성한다. 전체 CREATE/ALTER/verify SQL 패키지 및 DBA go/no-go는 실제 환경 확인 후 별도 적용 준비 범위다. 이번에는 Markdown 물리 정의만 갱신했고 SQL/DB 접속·적용은 하지 않았다.
- **Backfill:** 기존 Case/원본파일 내용·이력 변환/전체 재등록은 요구하지 않는다. 사용자가 편집을 시작할 때 기존 고정 원본에서 새 Draft revision0을 생성한다.
- **Rollback:** 새 쓰기/접수 중지 → 진행 요청 성공 여부 확인/실패 정리 → 이력/최종 Case/파일 보존. DROP이나 원본/성공 Case 삭제를 rollback으로 사용하지 않는다. 읽기 호환을 확보한 앱 롤백 순서는 적용 패키지에서 확정한다.
- **Verify 후속:** 동일 owner/source 동시 생성1건, 서로 다른 종류 동시 접수1건, 동일 requestId 중복/상이내용, revision 연속/성공 요청당1건, row origin/current위치 유일성·소속, 정확한 숫자/크기·재시작 replay, 최종 Case/4file/고정양식/FINALIZED의 all-or-nothing, commit 응답 유실·부분 업로드/늦은PUT/정리·timeout/늦은worker를 실제 DB/storage로 검증한다. 문서 검증은 이 행동의 Pass가 아니다.

명명은 기존 MDL/INPT/DRFT/PRCS/REQ/RVSN/ROW/FNSV/FILE/REF/JSON/TRY/TOKN/EXPR/CLNT/APLY·기존 회사/사용자 용어를 재사용한다. 신규 사전 등록은 없다.

### 6.4 코드 기준의 공통 저장 호환성 확인 — 실제 DB 미접속

[domain §5.4](mi-model-input-template-catalog-domain-model-design.md)의 기존 코드 연결 검토에 대응하는 물리/직렬화 경계다. **annotation/초안 확인은 실제 DB 스키마 확인이 아니다.** 새 테이블·업무 정책/공개 endpoint·상한을 추가/변경하지 않는다.

| 확인 영역 | 소스/문서에서 확인한 사실 | 공통 저장에 적용할 기준·남은 확인 |
| --- | --- | --- |
| master 연결/적용 방식 | [JpaConfig](../../../src/main/java/com/sk/common/config/database/JpaConfig.java)는 Case/UploadedFile/User/CustomerCompany가 있는 master entity/repository와 masterTransactionManager를 같은 고정 datasource에 묶는다. application.yml은 ddl-auto:validate/Flyway disabled다. | 신규7개도 같은 master JPA/transaction 사용. tenant MyBatis manager와 분산 transaction을 구성하지 않음. schema는 앱 자동 생성 대상이 아니며 새 entity 배포 전 DBA 적용 순서 필요(DBA-01/05) |
| 참조 ID/Java 범위 | ScenarioCase case_id와 User usr_no는 INT UNSIGNED, UploadedFile file_no는 BIGINT UNSIGNED, CustomerCompany cust_cpny_no는 MEDIUMINT UNSIGNED 선언이다. Java Case/file ID는 Long, user/company·file relation 번호는 Integer. Case→파일 조회/등록에서 Math.toIntExact(caseId)를 사용한다. | §6.3 FK 타입은 annotation상 일치한다. SQL unsigned 최대값과 Java 양수 범위가 같다고 가정하지 않음. 기존 relation API의 caseId는 Integer 양수로 표현 가능해야 한다. 파일번호는 공개 양의 int64 범위. 실제 최대 ID/auto_increment·컬럼 signedness는 DBA-01에서 확인하며 이번에 타입 확대/기존 데이터 변환을 요구하지 않음 |
| 파일 metadata/감사 | [UploadedFile](../../../src/main/java/com/sk/domain/shared/entity/master/tcm/UploadedFile.java)은 원본/저장 파일명·저장 전체 경로 길이100, MIME100·확장자20, file_size INT UNSIGNED 선언이다. [BaseEntity](../../../src/main/java/com/sk/domain/shared/entity/base/BaseEntity.java)/TimeBaseEntity는 crt_*/mod_*/del_yn을 사용하나 명명 문서 시스템 컬럼은 reg_*다. | 서버 생성 경로/파일명·bytes가 기존 metadata 범위에 맞도록 새 경로에서 검증한다. 준비 기록 LONGTEXT가 길어도 최종 metadata에 조용히 절삭해 넣지 않음. 감사 예외 승인·실제 정밀도/길이는 DBA-01/04. actor의 SYSTEM fallback 문제는 domain §5.4의 BE 책임 |
| 정확한 JSON 숫자 | [ObjectMapperBuilderCustomizer](../../../src/main/java/com/sk/common/config/ObjectMapperBuilderCustomizer.java)는 날짜/timezone 설정이며 공통 BigDecimal/BigInteger·plain-number 강제 설정이 없다. 기존 제안 validator는 Object/Number를 나중에 BigDecimal(number.toString())로 바꾸고 XlsmStreamMerger는 Double 숫자를 처리한다. | 이 경로들을 Draft 숫자 보존 구현으로 그대로 재사용하지 않음. 새 EDT 입력/영속 payload/재조회는 처음 JSON 숫자를 읽는 시점부터 정확한10진수/큰 정수로 다루고 지수 없는 숫자로 쓰며 비교도 동일 값으로 수행. 이미 Double로 바뀐 뒤 BigDecimal 변환으로 원래 자릿수를 복구할 수 없다. 전역 ObjectMapper 변경은 요구하지 않고 해당 경로에서 해결. JSON/DB 왕복과 최종 XLSM 수치 보존은 구현 후 별도 검증 |
| JSON 제한·body/로그 경계 | application.yml의300MB는 multipart 제한이며 EDT JSON1 MiB·깊이/명령 수 제한의 근거가 아니다. [HttpLoggingFilter](../../../src/main/java/com/sk/common/filter/HttpLoggingFilter.java)는 EDT 경로를 제외하지 않아 요청/응답 body와 MDC의 REQUEST_BODY를 남길 수 있고 wrapper는 body를 먼저 읽는다. 제안 응답 일부 reader의 중복 key 검사도 EDT 요청에 자동 적용되지 않는다. | 공통 API의1 MiB/깊이8·명령500/ADD100·TEXT4096, 영속 delta16 MiB 등 기존 계약을 EDT 읽기/저장 경계에서 강제. HTTP/MDC body 수집/출력을 해당 경로에서 제외해야 하며 handler 뒤의 검사만으로 앞선 전체 body 캐싱 제한/비노출이 보장되지 않음. 공통 mapper/filter 전체 개편을 선결하지 않고 영향 경로에 필요한 최소 변경. 실제 DB packet/접속 설정은 DBA-03 |
| 업로드 metadata 원자성 | FileService의 staged 업로드는 PUT 뒤 TEMP/0 metadata를 별도 저장, attach는 row 잠금 후 관계만 바꾼다. 최종준비fileNo와 Case 관계는 구분된다. | §6.3 신규 준비 기록의 전용 key는 PUT 전에 commit, tcm_filel/fileNo는 최종 성공 transaction에서만 생성한다. 기존 TEMP/0 row를 이 저장소로 사용하거나 기존 Scenario staged TTL 대상에 편입하지 않음. commit UNKNOWN/재시작·정리 연결은 domain §5.4를 따른다 |

소스 검색 범위에서 catalog6개/Case-template·신규7개에 대응하는 entity/repository 및 EDT endpoint는 발견하지 못했다. 따라서 기존 파일4개를 읽는 기능이 있어도 고정 발행/개인 Draft·재전송/복구 persistence가 이미 구현됐다고 할 수 없다. 실제 DB에 해당 table이 없는지도 소스만으로 결론내리지 않고 DBA-01로 확인한다.

### 6.5 EDT-01~03 첫 구현용 수동 DDL — 2026-10-16

검토/수동 적용 SQL 정본은 [model-input-draft-read-ddl-draft.sql](../../model-input-draft-read-ddl-draft.sql)이다. §6.3 전체7개 중 첫 생성/조회에 필요한 Draft·고정4파일·revision0·행ID·위치이력 **5개만** 생성한다. 요청/최종준비파일 테이블과 그 prcs_id FK는 이후 변경/최종저장 slice에서 추가하며 지금 기능이 쓰지 않는다. catalog6개는 기존 설계의 신규 설치용 구간으로 제공하고 이미 존재하면 pre-check 뒤 해당 CREATE를 건너뛴다. 실행/DB 적용은 사용자 담당이다.

`tmd_mdl_inpttbld.qry_json LONGTEXT NULL`을 추가한다. 이는 발행 묶음에 고정되는 버전1 **조회 정의**이며 전체 셀/사용자 입력/새 업무 정책을 저장하지 않는다. NULL/미지원 버전/미완비 정의는 POLICY_INCOMPLETE/POLICY_UNSUPPORTED이고 현재 ACTIVE/MAX로 보정하지 않는다. 새로운 DRAFT 발행에서만 정의를 작성하고 발행 후 불변/기존 Case 연결을 유지한다. JSON_VALID와 최대1 MiB/표를 검증한다.

조회 정의 version1의 필수 key는 schemaVersion(1), columns(1기반 표시 열번호 배열), headerRows(반복 헤더 행번호 배열), bodyRanges(startRow/endRow), identityColumns(활성 원본 행을 식별할 비수식 입력 열번호 배열), fields(편집 API field metadata와 columnNumber), capabilities(SET/CLEAR/ADD_ROW/DELETE_ROW의 승인 준비도), inputGuideRules/controls/displayUpdateRules(확인된 조회 규칙 배열)다. 빈 규칙 배열은 실제로 없음이 확인된 경우만 등록한다. identityColumns는 업무 중복 키/행ID가 아니며 해당 행의 식별 입력이 모두 빈 경우 EMPTY_SLOT로 분류하는 읽기 근거다. 미확인 행/표·필드를 임의 추정하지 않는다. 기존 승인 정책 밖 동작은 발행 READY로 만들지 않는다.

발행 모델 구분은 서버 설정 `mi.model-input.model-code`와 일치해야 한다. 미설정/불일치/양식 미연결은 POLICY_INCOMPLETE이며 임의 코드/현재 ACTIVE로 대체하지 않는다. 실제 값은 DBA-01의 기존 등록값에서 확인한다. 이 설정은 사용자 업무 정책이 아니다.

기존 table의 ck01은 일반 grid의 헤더/본문 순서를 전제했다. 수동 SQL §3은 기존 유효 행을 유지하면서 헤더0/본문0 또는1인 control 전용 단일행 사각형도 저장하도록 그 제약만 완화한다. 실제 grid는 qry_json의 columns/headerRows/bodyRanges로 명시하며 control 값은 grid에 중복하지 않는다. 같은 발행/역할/sheet의 sourceTableId 소유 사각형과 binding·공유 정의 일치를 검사한다. 신규 schema 변경을 자동 적용하거나 기존 발행을 다시 쓰지 않는다.

첫 slice는 masterDataSource에 고정한 JdbcTemplate과 기존 primary JpaTransactionManager를 결합한다. catalog projection과 원본 행 초기값 일괄 저장이 현재 필요이므로 미사용 JPA entity mapping을 추가하지 않는다. 따라서 사용자 DDL 적용 전이라도 새 entity의 ddl-auto validate 때문에 기존 앱 기동이 실패하는 구조는 추가하지 않는다. H2에서 같은 JPA manager의 JDBC rollback을 검증하며 MySQL 실물 DDL/키·CHECK 검증을 대체하지 않는다.

파일 판독은 저장 수식 cache만 읽는 순차 XML 방식이다. 평문 임시 workspace0700/파일0600, 정상/실패 finally 정리와 시작 시 중단 workspace lock 확인·정리를 사용한다. 다른 살아 있는 JVM의 lock/임의 디렉터리를 지우지 않는다. 초기 자원 방어 상한은 원본300MiB, package10000개 part, 압축 해제 공유문자열64MiB/스타일8MiB/개별 part512MiB다. 실제 Case103 적합성·메모리/소요시간은 미측정이며 저장 worker/SLA 검증으로 확대하지 않는다.

실제 읽기 정의/Case-template·4역할 등록은 검증할 기준 Case의 실제 데이터로 준비해야 한다. 이번 SQL은 production seed/목록·표별 정책을 만들지 않는다. 실제 적용/조회 정의 완비·원본 연결 전에는 endpoint 코드/독립 테스트 완료와 실제 사용 가능을 구분한다.

### 6.6 사용자 table prefix 변경 — 2026-10-16

입력 catalog/Draft의 table명은 `tmd_mdl_inpt...`로 변경한다. 아래 매핑이 현재 물리명이며 이전 실행 기록의 `tmm_mdlinpt...`는 변경 전 이름이다. 업무 소유/공용 master 저장·권한·API 계약과 컬럼/키/인덱스 구조는 바꾸지 않는다.

| 변경 전 | 현재 이름 |
| --- | --- |
| `tmm_mdlinpttmpltm` | `tmd_mdl_inpttmpltm` |
| `tmm_mdlinptfiled` | `tmd_mdl_inptfiled` |
| `tmm_mdlinptshtd` | `tmd_mdl_inptshtd` |
| `tmm_mdlinpttbld` | `tmd_mdl_inpttbld` |
| `tmm_mdlinptmodscopd` | `tmd_mdl_inptmodscopd` |
| `tmm_mdlinptdrftm` | `tmd_mdl_inptdrftm` |
| `tmm_mdlinptdrftfiled` | `tmd_mdl_inptdrftfiled` |
| `tmm_mdlinptdrftrvsnl` | `tmd_mdl_inptdrftrvsnl` |
| `tmm_mdlinptdrftrowm` | `tmd_mdl_inptdrftrowm` |
| `tmm_mdlinptdrftrowh` | `tmd_mdl_inptdrftrowh` |

후속 설계의 요청 이력/최종 준비파일도 각각 `tmd_mdl_inptdrftprcsl`, `tmd_mdl_inptfnsvfiled`로 정렬한다. 이2개는 이번 신규 생성/rename 대상이 아니다. 이미 `tmd_mdl_`인 Case 연결 `tmd_mdl_scnrcasetmpltd`와 다른 기능 table은 유지한다. MDL/INPT 등 기존 사전 용어만 재사용해 사전 추가는 없다.

기존 적용 DB용 사전 확인·rename·verify·역 rename 정본은 [table-prefix-rename SQL](../../model-input-draft-table-prefix-rename-draft.sql)이다. 새 설치는 §6.5의 수동 DDL을 사용하고 기존 DB에는 CREATE/ALTER를 다시 적용하지 않는다. 기존 명시 constraint/index 식별자(`tmm_mdlinpt..._fk/ck/ux/ix`)는 재생성하지 않고 새 설치 DDL에도 같은 이름을 유지한다. 이번 요청은 table명 변경이며, 종속 이름까지 일괄 정리하는 DDL은 포함하지 않는다.

사용자는 해당 API/등록 작업을 중단한 상태에서 구 이름10개·새 이름0개와 InnoDB/연결 제약·별도 table 권한/뷰·routine 의존을 확인하고, 같은 schema의 단일 `RENAME TABLE`10쌍을 적용한다. 이후 새 이름10개·구 이름0개, 행 수/기존 데이터·FK 참조와 앱 접근을 검증하고 새 이름 코드·등록 SQL을 사용한다. 데이터 backfill/복사·FK 검사 해제는 필요하지 않다. 구/신 이름 코드의 동시 호환 창은 제공하지 않는다.

기존 명시 제약 이름 보존과 metadata lock·table 단위 권한 별도 처리 근거는 [MySQL8.0 RENAME TABLE](https://dev.mysql.com/doc/refman/8.0/en/rename-table.html), 일반 transaction ROLLBACK 대신 별도 역 rename이 필요한 경계는 [implicit commit](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html)을 참조한다. 실제 MySQL 적용/권한·의존 검증은 사용자 실행 결과 전 미확인이다. 제품 코드의 새 table명 조회는 SQL 적용 뒤에 사용한다.

## 7. ERD review

아래 기존 검토표는 catalog6개 범위다. 공통 Draft7개는 §6.3에서 복합 PK/개인 tenant 소유·성공/요청/행 대응 제약을 별도로 정의했으며 해당 표에 이 기존 판정을 그대로 적용하지 않는다.

| 검토 항목 | 결과 |
| --- | --- |
| PK | 모든 table에 단일 PK가 있다. case-template는 `case_id` 자체가 PK다. |
| FK | hierarchy와 case/template 참조에 physical FK를 둔다. 모든 FK 선두 index는 PK/UK/IX로 확보된다. |
| UK | version, 역할, sibling name/order, 수정범위 중복을 방지한다. |
| nullability | 설명·적용시작·구조해시만 nullable이다. 구조 해시는 판별 방식 확정 전 optional이며 Q2용 새 컬럼은 없다. |
| status/delete | template lifecycle + 전 table `del_yn`을 사용한다. ACTIVE 하위 row의 변경/삭제는 service에서 금지한다. |
| tenant | 플랫폼 공용 configuration이므로 tenant FK를 두지 않는다. case 접근은 기존 case 권한을 따른다. |

수정범위가 부모 table 경계 안인지와 범위끼리 겹치는지는 다른 row와의 비교가 필요해 CHECK로
완전히 강제할 수 없다. 등록 service와 seed 검증 SQL이 보장한다.

Q2는 기존 파일 PK·객체 참조·Case 연결을 사용한다. 새 관계/UK/FK나 내용 hash 검색/공유·전역 deduplication을 추가하지 않으며 기존 tenant/Case 접근 검사를 유지한다.

## 8. Code impact review

- JPA: `BaseEntity`를 상속하는 read/write master/detail entity와 repository가 필요하다.
- QueryDSL: template hierarchy projection 한 번으로 조회하거나 단계별 batch 조회해 N+1을 피한다.
- MyBatis: 필수 아님. 초기 seed가 크면 DBA SQL/batch에만 사용한다.
- DTO/API: file/sheet/table/scope hierarchy와 1-based 좌표, add flags가 추가된다.
- service: ACTIVE 전환, 4개 역할 완전성, 범위 포함/비중첩, case mapping 검증이 필요하다.
- legacy cleanup: 3파일 `INDEPENDENT | MI_BASE` case는 신규 template에 mapping하지 않고 cutover DML로
  `tmd_mdl_scnrcasem.del_yn = 'Y'` 처리한다. file/run/result row는 물리 삭제하지 않는다.
  2026-10-13 사용자 결정으로 3파일 `SCENARIO` case도 대상이며, case master와 함께 `tmd_mdl_scnrcaseh`,
  연결 `tmd_mi_model_execpreph`, 해당 시나리오의 active `tmm_mi_scenario_prc_interp_m`의 `del_yn`도 `Y`로 바꾼다(domain 규칙 36).
- `tmm_mi_model_param_m`: 이번 change set에서는 수정하지 않는다.
- Q2 후속 구현은 기존 UploadedFile/file_no·객체 참조와 SCNRCASE 조회를 식별자 생성에 연결한다. 새 UploadedFile 컬럼/builder/of()·전용 등록 UPDATE·hash 파라미터/공개 DTO 변경은 필요 없다.
- Q2 repository/QueryDSL/MyBatis는 현재 file_no·SCNRCASE relation 조회를 재사용한다. 새 hash 기반 조회/index나 외부 writer 변경을 선행조건으로 추가하지 않는다. 실제 조회의 일관성/접근/실행 계획 검증은 후속 구현에서 수행한다.

## 9. Rollout / compatibility review

1. 6개 신규 table을 생성한다. 기존 application은 참조하지 않아 DDL 선행 배포와 호환된다.
2. `DRAFT` template과 file/sheet/table/scope seed를 적재한다.
3. hierarchy와 실제 4개 workbook 구조를 검증한다.
4. 검증된 template만 `ACTIVE`로 전환한다.
5. 아래 pre-check로 active legacy 3파일 `INDEPENDENT | MI_BASE | SCENARIO` case와 `RUNNING`/current `MI_BASE` 포함 여부를 확정한다.
6. 대상 `case_id` snapshot을 보관한 뒤 승인된 cutover DML로 soft delete한다. `INDEPENDENT | MI_BASE`는 case master만, `SCENARIO`는 case master와 위 연결 row를 바꾼다. file/result row와 object는 보존한다.
7. legacy case가 active resource 조회에서 제외된 것을 검증한 뒤 4파일 application을 배포한다.

기존 catalog change set은 기존 데이터 column 변경이 없어 일반 backfill은 없다. 초기 catalog seed는 신규 데이터 등록 작업이고,
legacy 3파일 case는 mapping하지 않고 별도 cutover DML로 soft delete한다.
Q2의 원본 내용 식별에는 추가 컬럼/재등록이 없다(§10A). 양식/catalog는 기존 6개 table 초안을 재사용하고 §10B의 승인 seed/발행·Case-template 관계 등록이 필요하다. 파일 내용 재등록과 양식 관계 등록을 구별하고 legacy 범위는 유지한다.

## 10. DDL draft structure recommendation

### Pre-check

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'tmd_mdl_inpttmpltm', 'tmd_mdl_inptfiled', 'tmd_mdl_inptshtd',
      'tmd_mdl_inpttbld', 'tmd_mdl_inptmodscopd', 'tmd_mdl_scnrcasetmpltd'
  );

SHOW CREATE TABLE tmd_mdl_scnrcasem;

-- legacy 3파일 INDEPENDENT/MI_BASE/SCENARIO 후보: 실행 전 case_id 목록과 건수를 저장한다.
WITH legacy_case AS (
    SELECT c.case_id,
           c.exec_type_cd,
           c.case_stat_cd,
           COUNT(f.file_no) AS active_file_cnt,
           SUM(UPPER(f.org_file_nm) LIKE '%DEMAND%') AS demand_cnt,
           SUM(UPPER(f.org_file_nm) LIKE '%SUPPLY%') AS supply_cnt,
           SUM(UPPER(f.org_file_nm) LIKE '%TRADE%') AS trade_cnt,
           SUM(UPPER(f.org_file_nm) LIKE '%OUTPUT%') AS output_cnt
    FROM tmd_mdl_scnrcasem c
    JOIN tcm_filel f
      ON f.file_rel_obj_spt_cd = 'SCNRCASE'
     AND f.file_rel_obj_dstg_no = c.case_id
     AND f.del_yn = 'N'
    WHERE c.del_yn = 'N'
      AND c.exec_type_cd IN ('INDEPENDENT', 'MI_BASE', 'SCENARIO')
    GROUP BY c.case_id, c.exec_type_cd, c.case_stat_cd
)
SELECT *
FROM legacy_case
WHERE active_file_cnt = 3
  AND demand_cnt = 1
  AND supply_cnt = 1
  AND trade_cnt = 1
  AND output_cnt = 0
ORDER BY case_id;
```

Expected: 신규 table은 0건이고, `tmd_mdl_scnrcasem.case_id`는 `INT UNSIGNED` PK다.
legacy 후보의 `RUNNING`은 0건이어야 하며 `MI_BASE` 포함 여부를 운영자가 확인해야 한다.
`SCENARIO` 후보는 연결 run history·execution-preparation·active 가격전망해석 건수도 함께 기록한다.
파일명 판별 결과가 모호한 case는 자동 대상에 넣지 않고 수동 검토한다.

### Apply

위 `## 6. Proposed schema change`의 CREATE TABLE/INDEX를 순서대로 실행한다.

### Backfill

- 기존 table column backfill: 필요 없음.
- initial catalog seed: 별도 승인된 seed SQL 필요.
- legacy case mapping/`OUTPUT` 보완: 수행하지 않는다.
- legacy case cleanup: 별도 승인된 DML로 `INDEPENDENT | MI_BASE`는 `tmd_mdl_scnrcasem.del_yn = 'Y'`만 갱신한다.
  대상 ID snapshot, `RUNNING` 0건, current `MI_BASE` 포함 여부 확인이 선행돼야 한다.
- `SCENARIO` legacy cleanup DML(case master + `tmd_mdl_scnrcaseh` + 연결 `tmd_mi_model_execpreph` + 해당 시나리오 active
  `tmm_mi_scenario_prc_interp_m`)은 아직 작성하지 않았다. `INPUT-11` 착수 시 대상 snapshot·건수 검증·rollback DML과 함께
  작성해 DBA 검토를 받는다.

### Rollback

application/seed가 사용하기 전, DBA가 명시적으로 승인한 경우에만 역순 drop한다.

```sql
DROP TABLE IF EXISTS tmd_mdl_scnrcasetmpltd;
DROP TABLE IF EXISTS tmd_mdl_inptmodscopd;
DROP TABLE IF EXISTS tmd_mdl_inpttbld;
DROP TABLE IF EXISTS tmd_mdl_inptshtd;
DROP TABLE IF EXISTS tmd_mdl_inptfiled;
DROP TABLE IF EXISTS tmd_mdl_inpttmpltm;
```

application이 이미 의존하면 drop 대신 forward-fix를 우선한다.

### Verify

```sql
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_inptmodscopd;
SHOW CREATE TABLE tmd_mdl_scnrcasetmpltd;

-- 모델별 ACTIVE 최대 1건: 결과 0건
SELECT mdl_spt_cd, COUNT(*)
FROM tmd_mdl_inpttmpltm
WHERE tmplt_stat_cd = 'ACTIVE' AND del_yn = 'N'
GROUP BY mdl_spt_cd
HAVING COUNT(*) > 1;

-- ACTIVE template의 파일 역할 완전성: 각 row가 role_cnt=4
SELECT t.tmplt_id, COUNT(DISTINCT f.mdl_file_cd) AS role_cnt
FROM tmd_mdl_inpttmpltm t
LEFT JOIN tmd_mdl_inptfiled f ON f.tmplt_id = t.tmplt_id AND f.del_yn = 'N'
WHERE t.tmplt_stat_cd = 'ACTIVE' AND t.del_yn = 'N'
GROUP BY t.tmplt_id
HAVING role_cnt <> 4;

-- 수정범위가 부모 table 밖인 경우: 결과 0건
SELECT s.mod_scop_id
FROM tmd_mdl_inptmodscopd s
JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id = s.inpt_tbl_id
WHERE s.del_yn = 'N'
  AND (s.bgn_row_no < t.tbl_bgn_row_no OR s.end_row_no > t.tbl_end_row_no
       OR s.bgn_col_no < t.tbl_bgn_col_no OR s.end_col_no > t.tbl_end_col_no);

-- 같은 table의 수정범위가 겹치는 경우: 결과 0건
SELECT a.mod_scop_id, b.mod_scop_id
FROM tmd_mdl_inptmodscopd a
JOIN tmd_mdl_inptmodscopd b
  ON b.inpt_tbl_id = a.inpt_tbl_id AND b.mod_scop_id > a.mod_scop_id
WHERE a.del_yn = 'N' AND b.del_yn = 'N'
  AND a.bgn_row_no <= b.end_row_no AND b.bgn_row_no <= a.end_row_no
  AND a.bgn_col_no <= b.end_col_no AND b.bgn_col_no <= a.end_col_no;

-- 개발 전용 시트가 ACTIVE runtime catalog에 등록된 경우: 결과 0건
SELECT s.inpt_sht_id, s.sht_nm
FROM tmd_mdl_inptshtd s
JOIN tmd_mdl_inptfiled f ON f.inpt_file_id = s.inpt_file_id
JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id = f.tmplt_id
WHERE t.tmplt_stat_cd = 'ACTIVE'
  AND t.del_yn = 'N' AND f.del_yn = 'N' AND s.del_yn = 'N'
  AND s.sht_nm IN ('SUMMARY', '플랫폼 입력 안내');
```

### DBA action / deployment note

- `crt_*` 대 `reg_*` 감사 컬럼 표준을 확정한다.
- generated column/unique 지원과 이름 충돌을 확인한다.
- seed SQL과 개발 전용 시트가 없는 운영 승인본 4개 workbook 검증 결과를 함께 승인한다.
- catalog read application 배포 전 ACTIVE seed와 legacy soft-delete 대상 snapshot·DML·rollback 절차를 확인한다.

## 10A. Q2 원본 파일 등록 참조 — 추가 rollout 없음

- **Pre-check/verify:** 기존 파일 PK·실제 객체 참조·SCNRCASE 관계 조회를 제품 경로에 연결할 때 실제 DB metadata와 쿼리를 대조한다. 현재 source/schema 참조만으로 새 쿼리의 행동/실행 계획을 검증한 것으로 보고하지 않는다. 이는 기존 연결의 확인이며 새 hash 컬럼이나 과거 원본 내용의 재등록 승인이 아니다.
- **Apply:** Q2 내용 식별을 위한 추가 DDL 없음. §10의 기존 catalog CREATE/seed/Case-template·legacy 순서는 별도로 유지한다.
- **Backfill:** 내용 해시 계산·기존 hash 복사·새 내용 기준 등록 없음. 유효한 기존 4파일 등록을 그대로 사용한다. 기존 3파일 Case에 OUTPUT을 보충하거나 새 template에 자동 mapping하지 않는다.
- **Rollback:** 이번 Q2 정정에서 추가 스키마/등록값이 없으므로 전용 rollback SQL 없음. 기존 공통 파일/객체·hash·Case 관계를 변경하지 않는다.
- **구현 검증:** 반복 업로드마다 새 file_no·UUID 객체가 생기고 이전 참조가 유지되는지, 직접 Case 커밋과 staged attach의 성공 경계, 네 역할/객체 연결·catalog 변경 시 버전 대조와 기존 실패 처리를 검증한다. 읽기 전체 재해시나 ETag/버킷 특수 기능 시험을 필수 검증으로 추가하지 않는다.
- **양식/catalog 경계:** 원본 내용 식별과 별개인 기존 catalog DDL/seed·Case-template 관계 등록은 §6.2·§10B를 따른다. 이번 연결안 구체화로 INPUT-03/04·INPUT-05 전체 상태나 제품 구현 완료를 변경하지 않는다.

## 10B. Q2 양식/catalog·Case 연결 등록과 검증

### Pre-check / publish

- 기존 §6의 6개 table이 적용됐는지와 Case/template PK·FK·index를 확인한다. 현재 repo에는 entity/repository·등록/조회 연결 코드가 없고 운영 table/seed 적용도 이번에 확인하지 않았다.
- 발행할 해당 모델의 DRAFT root와 정확한 4역할·조회에 필요한 정의/선택지/규칙을 검증한다. 기존 ACTIVE 정의를 직접 고치지 않고 새 버전 묶음을 발행한다. 이전 ACTIVE→DEPRECATED와 새 ACTIVE 전환은 기존 단건 unique와 일치하게 처리한다.
- 새 Case용 후보는 검증된 ACTIVE, 기존 미연결 Case의 과거 기준 등록은 실제 파일과 대응한다고 확인한 ACTIVE/DEPRECATED를 사용한다. 단순 ACTIVE/MAX 선택이나 파일명/구조 hash 하나로 연결하지 않는다. 명확한 대응 근거가 없으면 등록하지 않는다.

### Apply / registration DML draft

새 ALTER/CREATE는 없다. **기존 §6 table 적용/승인 seed·발행은 여전히 필요**하며 기존 master schema를 재사용한다는 뜻이다. Case/네 파일 저장 커밋 뒤 조회 제공 전에 별도 짧은 등록 transaction에서 실제 파일/객체/양식 대응과 연결 부재를 확인하고 다음 관계를 INSERT한다. 아래는 검증을 마친 서버/관리 등록용 바인딩 예시이며 이번 작업에서 실행하지 않았다.

```sql
INSERT INTO tmd_mdl_scnrcasetmpltd (
    case_id, crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn, tmplt_id
) VALUES (
    :verified_case_id, :actor_login_id, CURRENT_TIMESTAMP(6),
    :actor_login_id, CURRENT_TIMESTAMP(6), 'N', :verified_tmplt_id
);
```

Expected affected rows: 신규 연결은 1. 같은 Case/같은 양식의 재등록은 기존 row를 검증하고 그대로 유지한다. PK 충돌/다른 연결은 INSERT IGNORE나 upsert UPDATE로 숨기지 않고 등록을 중단해 현재 연결을 확인한다. 반쯤 등록된 관계나 다른 tmplt_id로 조용히 교체하지 않는다. 별도 등록 실패는 이미 커밋한 Case/원본 파일의 기존 업로드 성공을 소급 취소하지 않는다. GET은 이 DML을 수행하지 않는다.

### Backfill

- 연결이 없는 유효한 기존 4파일 Case만 실제 역할/객체/발행 양식 대응 검증 후 위 관계 INSERT 대상으로 삼는다. 원본 파일·file_no/UUID·기존 hash는 그대로다. 알려진 과거 파일에는 대응되는 DEPRECATED 묶음을 사용하고 현재 ACTIVE를 일괄 대입하지 않는다.
- 미확정/여러 후보/정의 미완비는 미연결로 유지하고 기존 조회 해석 정책 오류를 따른다. 기존 3파일 Case mapping/OUTPUT 보충과 legacy cleanup 범위를 변경하지 않는다.
- 초기 catalog seed/기존 Case 연결 대상·근거와 건수는 DBA/BE 등록 기록으로 남긴다. 파일 bytes 재해시/내용 기준 재등록이나 새 history table은 요구하지 않는다.

### Rollback / compatibility

- 기존 Case/파일 업로드 API와 공개 요청/응답은 유지한다. 새 조회/관계 사용을 중단해도 기존 Case/파일·발행 catalog를 보존한다. binding 등록 실패는 해당 관계 transaction만 취소한다.
- 이미 참조 중인 발행 묶음을 DRAFT로 되돌려 수정하거나 삭제하지 않는다. 신규 발행이 잘못됐으면 새 Case 연결을 중단하고 수정한 새 발행 묶음으로 처리하며 기존 Case를 자동 재연결하지 않는다.
- 잘못 등록한 Case 관계의 정정/제거는 실제 참조 영향과 대상/복구 근거를 확인하는 별도 수동 변경이다. 이 초안에 무조건 실행할 DELETE/UPDATE rollback을 추가하지 않는다. 기존 §10의 신규 catalog table rollback도 참조가 생기면 그대로 적용하지 않는다.

### Verify / read projection

```sql
-- 권한/유효 Case를 먼저 확인한 뒤 조회: 검증된 연결 Case에서 예상 1건
SELECT r.case_id, t.tmplt_id, t.mdl_spt_cd, t.tmplt_ver_no, t.tmplt_stat_cd
FROM tmd_mdl_scnrcasetmpltd r
JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id = r.tmplt_id
WHERE r.case_id = :authorized_case_id
  AND r.del_yn = 'N' AND t.del_yn = 'N'
  AND t.mdl_spt_cd = :server_model_cd
  AND t.tmplt_stat_cd IN ('ACTIVE', 'DEPRECATED');

-- DRAFT/누락/삭제 양식을 가리키는 활성 관계: 조회 제공 대상으로 선정한 Case에서 예상 0건
SELECT r.case_id
FROM tmd_mdl_scnrcasetmpltd r
LEFT JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id = r.tmplt_id
WHERE r.del_yn = 'N' AND r.case_id IN (:query_enabled_case_ids)
  AND (t.tmplt_id IS NULL OR t.del_yn <> 'N'
       OR t.tmplt_stat_cd NOT IN ('ACTIVE', 'DEPRECATED'));
```

선정 Case마다 첫 쿼리가 정확히 1행인지 목록과 대조한다. 관계 없는 Case는 두 번째 이상 조회에 나오지 않으므로 0건만 보고 검증 통과로 판단하지 않는다. Case 자체의 404/권한 검사를 먼저 수행하고 관계/해석 기준 없음은 기존 409로 구분한다. 단일 inner join의 결과 없음만 보고 Case 404로 처리하지 않는다. 관계는 case_id PK, 양식은 tmplt_id PK로 읽으며 새 index를 추가하지 않는다. 실제 구현/DBA 단계에서 EXPLAIN과 정확한 4역할/구조 대응·하위 정의/규칙의 버전 소속을 확인한다.

QRY-01/02는 이 관계에서 얻은 두 내부 버전을 현재 네 불변 파일/객체 기준에 결합한다. workbook 읽기 동안 긴 DB transaction/row lock을 유지하는 것을 기본안으로 삼지 않는다. 응답 확정 전 기준 대조가 새 DB 상태를 읽는지, 같은 repeatable-read snapshot/1차 cache 재조회만으로 변경 검출을 주장하지 않는지 제품 경합 시험으로 확인한다.

### Deployment order / completion boundary

기존 catalog/관계 DDL 확인 → GET용 승인 정의/seed 검증·발행 → 신규/기존 Case의 검증된 관계 등록 → 관계/발행 버전 read projection과 기존 API 버전 대조 구현·시험 순서다. 새 ACTIVE 전환에도 기존 Case의 두 버전 유지, 새 버전 묶음의 다른 조합 식별자, 미연결/모호/미지원·catalog 정의 누락, 등록 동시성/반복 등록과 페이지 사이 기준 변경을 검증한다. 실제 DDL/seed/등록·EXPLAIN/제품 시험은 이번 문서 작업에서 실행하지 않았다. 전체 INPUT 티켓 상태나 기존 read-only schema drafting 판정은 변경하지 않는다.

## 11. Recommendation

기존 2단계 `template → table` 초안 대신 5단계 `template → file → sheet → table → scope`를
채택한다. 실제 파일 저장 table과 catalog를 분리하고, 한 case가 사용한 template set을 고정하면
현재 실행과 과거 실행을 같은 규칙으로 재현할 수 있다.

## 12. Required SQL / DBA action

- 위 DDL은 draft이며 DBA 승인 전 적용하지 않는다.
- initial seed SQL은 실제 편집 대상 table 경계를 확정한 뒤 별도로 작성한다.
- 기존 3파일 case에는 template mapping을 만들지 않는다.
- legacy cleanup은 hard delete SQL이 아니라 case master soft-delete DML로 작성하고 DBA 승인 전 실행하지 않는다.
- Q2의 기존 등록 참조 사용에는 별도 공통 파일 ALTER/backfill/복구 SQL이 없다(§10A). 기존 catalog/Case-template 및 catalogRevision 관련 후속은 별도로 유지한다.

## 13. Risks / open questions

### 2026-10-16 실제 DB 확인이 필요한 DBA 후속

공통 저장 정의는 §6.3, **기존 코드/문서 대조 완료 범위**는 §6.4 및 domain §5.4다. 아래 항목은 모두 **실제 DB 미확인/PENDING**이다. 이번에는 접속/SQL 실행/적용하지 않았다. 소스의 annotation이나 과거 DDL 초안을 운영 적용 증거로 쓰지 않는다.

| ID / 담당 | 실제 확인할 것 | 필요한 결과/완료 경계 |
| --- | --- | --- |
| DBA-01 / DBA, BE 대조 | master의 Case/파일/User/회사·catalog6개/Case-template의 존재·실제 CREATE 정의와 신규7개 이름 충돌. PK/FK signedness/길이/NULL·engine/charset/collation·metadata 경로100/file_size·참조 가능한 active 데이터/최대 ID·auto_increment | 실제 정의와 §6.3/§6.4의 차이 목록. Case→Integer relation/파일번호→양의 int64 등 코드 표현 범위 확인. 기존 Case4파일·발행 연결 대상 여부는 BE와 확인하되 목록/seed 구축·내용 재등록으로 확대하지 않음 |
| DBA-02 / DBA, BE 경합 검증 | 실제 MySQL 버전에서 nullable generated UK·복합 FK/parent revision·CHECK/JSON_VALID가 동작하는지, 같은 master transaction/엔진·격리/lock 설정이 맞는지 | 적용 가능한 제약/생성 순서와 경합 검증 환경 확인. 실제 동시 생성/접수/최종 commit·늦은worker 시험은 구현 후 BE+DBA 검증이며 지금 Pass로 기록하지 않음 |
| DBA-03 / DBA, BE 저장/접속 대조 | max_allowed_packet·connector/배포 환경 한도와 utf8mb4 LONGTEXT/JSON_VALID. 최대16 MiB delta 및 같은 SQL/프로토콜 여유 수용 여부 | 실제값과 최대 영속 요청의 여유 확인. 부족하면 적용 전에 설정/계약 대응 확정; 숫자 반올림·문자열 절삭/부분 저장으로 회피하지 않음. multipart300MB를 JSON/DB 한도 증거로 사용하지 않음 |
| DBA-04 / DBA·데이터 표준 담당 | crt_*/mod_*와 reg_* 표준 차이의 예외 승인, 실제 감사 컬럼 길이/시간 정밀도·DB 시간 설정 | 기존 BaseEntity 호환 감사 컬럼의 확정/예외 근거. 공통 설계의 사용자 업무 결정으로 되돌리지 않음 |
| DBA-05 / DBA·BE 배포 담당 | 위 결과를 반영한 CREATE/ALTER/검증 SQL 패키지·권한·적용 순서, ddl-auto:validate 앱과의 호환/롤백·운영 owner/go-no-go | 실제 환경에 맞는 패키지와 수동 적용 승인 후 별도 실행. 이번에는 SQL 패키지/실행을 만들지 않음. 참조 데이터/성공 이력 보존·기존 Case backfill 불필요 기준 유지, legacy cutover/표별 정책·seed 설계는 별도 |

BE 구현 후의 제품 검증은 별도다: 입력→JSON text→DB→재조회/XLSM 수치 보존, body/로그·소유/회사 접근, durable 접수/격리 worker·재시작/timeout/UNKNOWN·부분 업로드/늦은PUT/정리·같은 최종 Case를 검증한다. 기존 Scenario staged/Run 시험 결과를 새 Draft의 Pass로 사용하지 않는다. 실제 저장소 ACL/암호화·편집 worker 운영 강제는 BE/운영 확인이며 DBA만의 DB 실물 확인 목록으로 넘기지 않는다.

표별 field/구조·파생/화면 정책은 후속이다. Sector 국가/하위부문 목록·option snapshot/seed 연결안을 되살리지 않으며 이미 승인한 Sector/Monthly/BCPrices 정책은 유지한다. **PriceModel/Regas 추가 입력 추천은 미승인·후속**이다. 장기 archive/폐기는 별도이며 이번에는 자동 만료를 도입하지 않는다.

기존 위험/후속 항목:

- 구조 변경 허용 범위는 domain 규칙 13·26·45·47을 따른다. 마지막 행·열 추가와 개별 중간 삽입·삭제 예외를 구분할 정책 저장 구조·검증 계약은 미정이며, 현재 추가 가능 플래그만으로 전체 정책을 표현하지 않는다.
- 새 행/열의 수식·서식·validation 복제 규칙이 미정이다.
- 구조 hash의 정규화/판별 방식이 미정이다.
- `BaseEntity`와 DB 명명 문서의 감사 컬럼 표준이 충돌한다.
- ACTIVE child soft delete 및 version activation을 수행할 운영 주체/API가 미정이다.
- legacy 3파일 case soft-delete의 대상 ID/건수, `RUNNING` 0건, current `MI_BASE` 영향은 운영 DB pre-check가 필요하다.
- Q2의 양식/catalog·Case 연결안은 §6.2·§10B에 있다. 실제 table/승인 GET 정의·seed/발행/기존 Case 연결 대상과 결과, 제품 projection/버전 대조·경합/실행 계획 검증은 남았다. 내용 hash/독립 revision 컬럼을 새 선행조건으로 두지 않는다.

## 14. Normalization assessment

아래 판정은 기존6개 catalog만 대상으로 한다. §6.3의 순서 있는 JSON text/delta는 원자적 변경 순서/복원을 위한 의도적 비정규 저장이다. 행위치의 Draft/표 ID는 같은 소속 복합 FK 검증을 위한 중복이며 새로운 업무 정본이 아니다. 따라서 전체 신규7개까지 일괄 1NF~BCNF Pass로 확장하지 않는다.

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
- 운영 데이터가 약 70 sheets 수준이므로 covering index를 선제 추가하지 않는다.
- nullable hash도 MySQL index에 포함되지만 조회는 non-null hash equality로 제한한다.

## 16. Query optimization notes

- hierarchy 전체 조회는 root `tmplt_id` equality에서 시작하므로 full scan이 필요 없다.
- application 구현 후 `EXPLAIN`에서 각 child join의 `key`가 `_ux01` 또는 `_ux02`인지 확인한다.
- 목록 크기가 작아 pagination/partitioning/aggregate 최적화는 필요 없다.
- JPA collection 순회로 N+1이 생기지 않도록 projection 또는 batch 조회를 사용한다.

## 17. Dictionary updates

- 기존 설계에서 `시트 / SHT / 단일단어`를 385번 row에 등록했다.
- 이번 `OUTPUT` 역할 추가에는 신규 용어가 필요하지 않다. 기존 등록어 `출력(OUTP)`,
  `파일(FILE)`, `역할(ROLE)`로 표현 가능하므로 사전 변경은 없다.
- 미등록 `편집`, `규칙`은 사용하지 않고 등록 용어 `수정(MOD)`, `범위(SCOP)`로 대체했다.
- Q2는 기존 파일 식별값을 사용하므로 신규 DB 용어/컬럼 등록은 없다.

## 18. Implementation readiness

- 2026-10-16: §6.3의 관계형/JSON 방향·공통 유일성/성공 경계는 사용자 채택한 설계 기준이다. 공통 물리 정의/저장 계약은 §6.3으로 문서 범위 완료다. 전체 CREATE/ALTER/verify SQL·DBA rollout 및 실제 제품 동작은 미완료다. 기존6개 SQL의 적용 완료/확장 완료로 읽지 않는다. 공통 설계 입력은 `ready after minor cleanup`(실제 schema·감사 예외/SQL 적용 준비 확인), 표별 쓰기/최종 파일 제품 활성화는 해당 정책/STRUCT·구현 검증 전 `blocked by open questions`다.

- 코드/문서 확인 결과: §6.4 대조 Complete. 추가 사용자 공통 업무 결정 없음. 실제 DB 준비도는 DBA-01~05 미확인으로 별도 유지하며 공통7개/DDL 적용·제품 동작을 승인하지 않는다.

- ERD/DDL review verdict: `REVISE` for rollout — 공통 저장 구조 선택은 완료, 실제 schema/감사·SQL 패키지/DBA 확인이 남았다.
- hierarchy/keys/types/index 방향은 구현 입력으로 충분하다.
- DBA apply와 행·열 추가 실행 API는 감사 컬럼 표준, 추가 방식, 복제 규칙을 해결한 뒤 `ready`로 전환한다.
- current readiness: `blocked by open questions` for write/execute implementation;
  read-only catalog schema/entity drafting is `ready after minor cleanup`.
- Q2 기존 원본 등록과 §6.2/§10B의 양식/catalog 연결안은 추가 스키마 없이 기존 master/관계를 사용한다. table/GET 정의·seed/발행·Case 연결 등록·제품 검증은 미완료다. 기존 catalog의 `ready after minor cleanup` 및 write/execute의 `blocked by open questions` 판정을 변경하지 않는다.
