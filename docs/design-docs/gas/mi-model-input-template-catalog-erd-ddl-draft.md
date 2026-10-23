# MI Model Input Template Catalog ERD / DDL Draft

## Document Control

- status: Draft
- canonical for: Schema rollout — model input template catalog tables, columns, keys, indexes, nullability, migration, rollback, and verify SQL; §6.1/§10A의 기존 불변 파일 참조와 §6.2/§10B의 발행 양식/catalog·Case 연결, §6.3의 Draft 공통 저장 및 §6.10의 직접 업로드 확장 물리 정의, §6.11의 A 실행기 상태 저장, §6.12의 Case 공유 초기 기준과 Draft 호환/적용 설계. 기존 공통 파일 table 전체 DDL은 재정의하지 않는다.
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
- last synced: 2026-10-23 — Demand Parameters/Bands 기존 값 수정 전용 결정 반영(업무 정본 §6.9, v6). 이전 동기화: 2026-10-21 (§6.12 Case 공유 초기 기준 설계; §6.11 A 실행기 상태 table 사용자 승인; §6.10 직접 업로드 저장 확장/증분 DDL 초안; §6.9 미배포 v2 단일 저장/복원 형식·Draft 초기화 SQL 확정; §6.8 감사 초 정밀도 정렬·증분 적용 준비; §6.3.4.1 성공값 좌표 이동·저장 이력 오류 처리 보완; 사용자 제공 midb 정의에 따른 §6.4 파일 metadata 매핑 정렬 유지; 나머지 DBA 후속은 §13)
- open questions:
  - §6.11의 상태 table1개 추가는 2026-10-20 사용자 후속 승인됐다. 실제 DB 조회·DDL 적용/경합 검증은 별도이며 아직 수행하지 않았다.
  - 직접 업로드의 물리 설계/증분 SQL은 §6.10에 정리했다. 실제 환경 제약명·타입·수동 적용, MySQL 제약/동시성 및 새 저장 형식 reader/writer 검증은 미실행이며 이 확인 전에는 적용 준비 완료로 판정하지 않는다.
  - Q2 §6.2/§10B의 기존 catalog table/승인 GET 정의·seed·발행과 신규/기존 Case-template 관계 등록 결과를 확인해야 한다. field/control/options/화면 규칙의 실제 저장 구조/정의는 기존 후속이며 root ID만으로 완비를 가정하지 않는다.
  - 3파일 `SCENARIO` legacy cleanup의 apply/verify/rollback DML(case master + run history + 연결 execution-preparation + active 가격전망해석)은 미작성이다. `INPUT-11` 착수 시 작성해 DBA 검토를 받는다.
  - domain 규칙47의 삭제 정책은 현재 추가 플래그만으로 표현하지 않는다. 열 capability/field 상태·생성 정의는 기존 qry_json 확장으로 표현하며 §6.9를 따른다. 실제 표별 정책/처리기 연결과 catalog DML은 후속이다.
  - domain 규칙 46에서 확정한 별도 화면 갱신 규칙의 DB 저장 단위, 양식 version/표/필드 연결, 물리 테이블·컬럼·키는 후속 설계 대상이다. 아래 기존 6개 table DDL은 아직 이 저장 구조를 포함하지 않는다.
  - 이번 release의 구조 변경 포함과 마지막 행·열 추가 중심 원칙은 domain 규칙 13·26에서 확정했다. 표별 추가 경계와 중간 삽입·삭제 예외를 저장할 물리 표현, 신규 셀의 수식·서식 복제 규칙은 미정이다.
  - 감사 이름/초 정밀도는 §6.8의 사용자 결정으로 확정했다. 실제 적용·백업/손실 확인·환경별 시간대와 외부 DBA 표준 확인은 남아 있다.
  - 운영 DB의 `tmm_mi_model_param_m` 현재 값은 확인되지 않았다. 이 문서의 대조는 저장소 DDL/seed 기준이다.
  - 실제 운영 승인본 4개의 업무 구조에 대한 seed 수와 구조 hash는 승인본 검증 후 확정해야 한다. 개발 시트 포함 파일 수용은 domain 규칙39~40을 따르며 시트 제거를 계측의 전제조건으로 두지 않는다.

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

2026-09-28 첨부 개발본의 output은 7개 시트이고 `SUMMARY`, `플랫폼 입력 안내` 2개는
runtime catalog에서 제외하는 개발 시트다. 다른 3역할도 같은 개발 전용 시트를 합계 4개 포함한다.
2026-10-19 이후 이 시트들이 남은 실제 파일의 수용·보존은 domain 규칙39~40을 따른다. 이 결정으로 DDL이나 기존 catalog seed를 바꾸지 않는다.

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
- 기존 table alter: Q2용 내용 해시/등록 시각 컬럼 추가안은 철회(§6.1). Draft 열 저장 v2의 요청 이력 CHECK 변경·7table 초기화는 §6.9/전용 SQL이 소유한다.
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
`tmd_mdl_scnrcasem` 패턴을 따라 `crt_*`를 사용한다. 감사 이름·초 정밀도의 사용자 채택과 실제 적용 잔여는 §6.8/DBA-04를 따른다.

```sql
CREATE TABLE tmd_mdl_inpttmpltm
(
    tmplt_id            INT UNSIGNED AUTO_INCREMENT COMMENT '입력템플릿ID' PRIMARY KEY,
    crt_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
    crt_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id     VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm             DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
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
- 표시한 PK/required는 NOT NULL, `?`만 NULL 허용이며 임의 DEFAULT0을 두지 않는다. 감사 컬럼은 기존6개/`BaseEntity`와 같은 `crt_usr_logn_id/mod_usr_logn_id VARCHAR(50)`, `crt_dtm/mod_dtm DATETIME`, `del_yn CHAR(1) DEFAULT 'N'`을 사용한다. 감사 이름·초 정밀도는 §6.8의 사용자 결정을 따르며 외부 DBA 승인으로 간주하지 않는다. 성공 이력/요청/참조에 CASCADE DELETE·독립 soft delete/자동 TTL을 두지 않는다.
- `sourceVersion`은 원본 등록/양식 참조로 계산한 `VARCHAR(68) ascii_bin`을 고정하고 새 원본 내용 SHA/revision은 만들지 않는다. sourceKey는 기존 식별 규칙으로 파생한다. 원본 fileNo는 `tcm_filel.file_no BIGINT UNSIGNED`, Case ID는 `tmd_mdl_scnrcasem.case_id INT UNSIGNED`, template/table ID는 기존 `INT UNSIGNED`와 타입을 맞춘다.

#### 6.3.2 테이블·컬럼·참조

| 물리 테이블 / PK | 필수 컬럼 | NULL 허용 컬럼·책임 |
| --- | --- | --- |
| `tmd_mdl_inptdrftm` / `drft_id UUID` | `cust_cpny_no`, `usr_no`, `org_case_id INT UNSIGNED`, `tmplt_id INT UNSIGNED`, `org_rvsn_val VARCHAR(68)`, `drft_stat_cd VARCHAR(20)`, `rvsn_no BIGINT UNSIGNED` | `actv_prcs_id UUID?`(진행 포인터), `fnsv_case_id INT UNSIGNED?`(최종 Case). DRAFT/FINALIZED/DISCARDED CHECK. FINALIZED와 최종 Case 존재는 동치, 종료 Draft는 진행 포인터 NULL |
| `tmd_mdl_inptdrftfiled` / `(drft_id, mdl_file_cd)` | `drft_id UUID`, `mdl_file_cd VARCHAR(20) ascii_bin`, `file_no BIGINT UNSIGNED` | 없음. SUPPLY/DEMAND/TRADE/OUTPUT CHECK, Draft의 고정 원본4개. 같은 Draft/file_no UK로 한 파일의 역할 중복 금지 |
| `tmd_mdl_inptdrftprcsl` / `prcs_id UUID` | `drft_id UUID`, `req_id UUID`, `prcs_type_cd VARCHAR(20)`, `usr_role_cd VARCHAR(20)`, `base_rvsn_no BIGINT UNSIGNED`, `prcs_stat_cd VARCHAR(20)`, `json_rvsn_no SMALLINT UNSIGNED`(현재1), `req_json LONGTEXT`, `expr_dtm DATETIME(6)` | `try_tokn UUID?`, `bgn_dtm/end_dtm DATETIME(6)?`, `rslt_json/fail_json LONGTEXT?`. 종류 CHANGE/FINALIZE, 상태 QUEUED/RUNNING/SUCCEEDED/FAILED CHECK. 초 단위 접수시각 crt_dtm+120초가 expr_dtm이며 기한 비교는 마이크로초 DB 시계다(§6.8). RUNNING은 token/시작시각 필수, 종료는 종료시각 필수. 성공은 result만, 실패는 failure만 존재 |
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

- 기존 full-row Draft 생성은 원본4역할·revision0·실제 원본 행ID/위치0을 함께 commit한다. §6.12 공유 Case 저장은 검증된 baseline 참조·가상 행 대응으로 대체한다. 업무값 변경/계산은 하지 않는다. 동시 생성 UK 충돌은 승자 Draft를 재조회하고 sourceVersion을 대조한다.
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

#### 6.3.4.1 성공 delta schemaVersion1 구현 형식 — 2026-10-18

기존 LONGTEXT/관계형 컬럼 안의 내부 형식이며 새 DDL은 없다. 공개 요청을 저장 결과로 간주하지 않고, 후속 표별 처리기가 검증한 **변경된 결과만** 내부 성공 확정에 전달한다. top-level은 정확히 `policyTemplateId`(고정 tmplt_id), `commands`(저장 요청의 순서 있는 operations와 같은 의미), `tables`, `rows`, `cells`다. schemaVersion은 기존 json_rvsn_no=1이며 payload 안에 중복 저장하지 않는다. 각 배열은 빈 경우에도 필수다.

- `tables`: 바뀐 표만 `{tableId,startRow,endRow,dataStart,definition,scopes}`. definition은 §6.5의 조회 형식이며 승인 fields/capabilities/열·identity/화면 규칙과 control 의미는 유지하고 header/body·control binding 좌표만 처리 결과로 이동할 수 있다. scopes는 현재 표 안의 `{startRow,endRow,startColumn,endColumn}` 배열이다. 기존 column bounds/행·열 추가 정책을 바꾸거나 발행 catalog를 UPDATE하지 않는다.
- `rows`: 이번 성공에서 추가/이동/삭제된 행의 최종 대응만 `{tableId,rowId,clientRowKey,rowNumber}`. 원본/이전 성공 행은 clientRowKey:null, 신규는 같은 요청 ADD_ROW 임시키이며 rowId는 서버 UUID다. rowNumber:null은 삭제 tombstone이다. ADD 후 DELETE도 신규 identity와 tombstone을 남긴다. 기존 삭제 identity를 되살리거나 새 ID로 재사용하지 않는다. 위치 변경 전체의 이전 구간을 먼저 닫고 새 구간을 추가한다.
- `cells`: 바뀐 사용자/승인 파생·계산 저장값만 `{tableId,rowId,rowNumber,columnNumber,raw,display,formula}`. 논리 행은 rowId만, header/control/미활성 슬롯은 rowNumber만 사용한다(다른 쪽은 null). raw/display는 정확한 JSON number·string·boolean 또는 null, formula는 boolean이다. CLEAR는 raw/display:null·formula:false다. raw와 표시를 서로 변환하지 않고, 계산 셀은 검증된 cached raw/display와 formula:true만 복원한다. 수식 계산/수식 원문 생성은 조회에서 하지 않는다. 같은 좌표의 배열 순서는 적용 순서이며 최종 사용자 값은 commands의 마지막 SET/CLEAR/ADD 값과 일치해야 한다.

허용된 tables 좌표 이동에서는 기존 성공 fixed 값도 header 순서/control ID의 대응에 따라 이동한다. 공유 control은 같은 owner 좌표 값을 한 번의 이전 상태로 읽고 출발 좌표를 비운 뒤 목적지에 배치하므로 좌표 교환에서도 값을 잃지 않는다. 이번 revision의 cells(명시 null 포함)가 이동된 이전 값보다 나중에 적용된다. 서로 다른 이전 고정값이 같은 새 좌표로 모이는 경우 이번 cells의 명시 값이 없으면 저장 실패로 차단한다. 저장 delta 안의 definition 형식/버전 오류도 저장 이력 해석 실패이며, 원본 발행 정의의 정책409와 구분해 기존 안전한500/data:null을 반환한다.

복원은 revision0의 원본 행 대응에서 시작해 각 성공 payload를 순서대로 적용한다. 각 revision의 최종 행 위치와 관계형 row history가 같아야 하고, chain은0부터 현재까지 연속·parent=n-1·동일 Draft의 SUCCEEDED CHANGE/base/result revision과 대응해야 한다. 알 수 없는 key/schema·누락·소속/행 구간 불일치는 저장 실패로 차단한다. 읽기의 안전한 HTTP500/data:null 계약은 유지하고 접수된 작업의 성공 확정 실패는 PERSISTENCE_FAILED로 기록한다. 삭제/이동으로 빈 원본 위치에 이전 논리 행 값을 다시 노출하지 않으며 신규 행은 해당 성공 결과에 없는 값을 원본 빈 슬롯에서 추정하지 않는다. 전체 workbook/cell snapshot은 저장하지 않는다.

#### 6.3.5 적용 준비·검증 경계

- **Pre-check:** 실제 master의 catalog6개/Case·파일·회사·사용자 타입·engine/collation/FK와 감사 예외, packet 여유를 확인한다. 현재 데이터에 새 Draft는 없다는 전제는 운영 확인 전 사실로 단정하지 않는다. 신규7개 이름 충돌/동일 정의 존재 여부와 nullable generated UK·경합을 staging MySQL에서 검증한다.
- **Apply 초안 순서:** Draft(외부 FK) → 원본 참조/요청 → 성공revision → 행 → 행위치 → 준비파일. 복합 UK와 parent revision FK를 함께 작성한다. 전체 CREATE/ALTER/verify SQL 패키지 및 DBA go/no-go는 실제 환경 확인 후 별도 적용 준비 범위다. 이번에는 Markdown 물리 정의만 갱신했고 SQL/DB 접속·적용은 하지 않았다.
- **Backfill:** 기존 Case/원본파일 내용·이력 변환/전체 재등록은 요구하지 않는다. 사용자가 편집을 시작할 때 기존 고정 원본에서 새 Draft revision0을 생성한다.
- **Rollback:** 새 쓰기/접수 중지 → 진행 요청 성공 여부 확인/실패 정리 → 이력/최종 Case/파일 보존. DROP이나 원본/성공 Case 삭제를 rollback으로 사용하지 않는다. 읽기 호환을 확보한 앱 롤백 순서는 적용 패키지에서 확정한다.
- **Verify 후속:** 동일 owner/source 동시 생성1건, 서로 다른 종류 동시 접수1건, 동일 requestId 중복/상이내용, revision 연속/성공 요청당1건, row origin/current위치 유일성·소속, 정확한 숫자/크기·재시작 replay, 최종 Case/4file/고정양식/FINALIZED의 all-or-nothing, commit 응답 유실·부분 업로드/늦은PUT/정리·timeout/늦은worker를 실제 DB/storage로 검증한다. 문서 검증은 이 행동의 Pass가 아니다.

명명은 기존 MDL/INPT/DRFT/PRCS/REQ/RVSN/ROW/FNSV/FILE/REF/JSON/TRY/TOKN/EXPR/CLNT/APLY·기존 회사/사용자 용어를 재사용한다. 신규 사전 등록은 없다.

### 6.4 코드 기준의 공통 저장 호환성 확인 — Codex 직접 DB 접속 없음

[domain §5.4](mi-model-input-template-catalog-domain-model-design.md)의 기존 코드 연결 검토에 대응하는 물리/직렬화 경계다. **annotation/초안 확인은 실제 DB 스키마 확인이 아니다.** 새 테이블·업무 정책/공개 endpoint·상한을 추가/변경하지 않는다.

2026-10-18 파일 metadata 대조는 사용자가 제공한 시험 midb의 `SHOW CREATE TABLE tcm_filel` 결과를 근거로 한다. 사용자 승인으로 아래 파일명·경로의 Entity 매핑을 해당 규격에 맞췄으며 DB 변경은 없다. dev·prod 컬럼의 동일 규격은 사용자 지시에 따른 적용 전제이고 별도 실측 결과가 아니다. 환경별 시간대 확인과 나머지 DBA 후속은 별도로 유지한다.

| 확인 영역 | 소스/문서에서 확인한 사실 | 공통 저장에 적용할 기준·남은 확인 |
| --- | --- | --- |
| master 연결/적용 방식 | [JpaConfig](../../../src/main/java/com/sk/common/config/database/JpaConfig.java)는 Case/UploadedFile/User/CustomerCompany가 있는 master entity/repository와 masterTransactionManager를 같은 고정 datasource에 묶는다. application.yml은 ddl-auto:validate/Flyway disabled다. | 신규7개도 같은 master JPA/transaction 사용. tenant MyBatis manager와 분산 transaction을 구성하지 않음. schema는 앱 자동 생성 대상이 아니며 새 entity 배포 전 DBA 적용 순서 필요(DBA-01/05) |
| 참조 ID/Java 범위 | ScenarioCase case_id와 User usr_no는 INT UNSIGNED, UploadedFile file_no는 BIGINT UNSIGNED, CustomerCompany cust_cpny_no는 MEDIUMINT UNSIGNED 선언이다. Java Case/file ID는 Long, user/company·file relation 번호는 Integer. Case→파일 조회/등록에서 Math.toIntExact(caseId)를 사용한다. | §6.3 FK 타입은 annotation상 일치한다. SQL unsigned 최대값과 Java 양수 범위가 같다고 가정하지 않음. 기존 relation API의 caseId는 Integer 양수로 표현 가능해야 한다. 파일번호는 공개 양의 int64 범위. 실제 최대 ID/auto_increment·컬럼 signedness는 DBA-01에서 확인하며 이번에 타입 확대/기존 데이터 변환을 요구하지 않음 |
| 파일 metadata/감사 | 사용자 제공 DB 정의와 [UploadedFile](../../../src/main/java/com/sk/domain/shared/entity/master/tcm/UploadedFile.java)의 정렬된 매핑은 원본·저장 파일명 VARCHAR(300) NOT NULL, 저장 경로 VARCHAR(255) NULL이다. MIME100·확장자20, file_size INT UNSIGNED 선언은 유지한다. [BaseEntity](../../../src/main/java/com/sk/domain/shared/entity/base/BaseEntity.java)/TimeBaseEntity는 crt_*/mod_*/del_yn을 사용하나 명명 문서 시스템 컬럼은 reg_*다. | 경로의 DB/Entity NULL 허용과 Draft 원본의 경로 필수 규칙을 구별한다. ModelInputDraftService의 경로 NULL·빈값 차단은 유지한다. 서버 생성 경로/파일명·bytes가 기존 metadata 범위에 맞도록 새 경로에서 검증하며 조용히 절삭해 넣지 않음. 감사 이름·정밀도 채택 및 실제 확인 잔여는 DBA-04, actor의 SYSTEM fallback 문제는 domain §5.4의 BE 책임 |
| 정확한 JSON 숫자 | [ObjectMapperBuilderCustomizer](../../../src/main/java/com/sk/common/config/ObjectMapperBuilderCustomizer.java)는 날짜/timezone 설정이며 공통 BigDecimal/BigInteger·plain-number 강제 설정이 없다. 기존 제안 validator는 Object/Number를 나중에 BigDecimal(number.toString())로 바꾸고 XlsmStreamMerger는 Double 숫자를 처리한다. | 이 경로들을 Draft 숫자 보존 구현으로 그대로 재사용하지 않음. 새 EDT 입력/영속 payload/재조회는 처음 JSON 숫자를 읽는 시점부터 정확한10진수/큰 정수로 다루고 지수 없는 숫자로 쓰며 비교도 동일 값으로 수행. 이미 Double로 바뀐 뒤 BigDecimal 변환으로 원래 자릿수를 복구할 수 없다. 전역 ObjectMapper 변경은 요구하지 않고 해당 경로에서 해결. JSON/DB 왕복과 최종 XLSM 수치 보존은 구현 후 별도 검증 |
| JSON 제한·body/로그 경계 | application.yml의300MB는 multipart 제한이며 EDT JSON1 MiB·깊이/명령 수 제한의 근거가 아니다. [HttpLoggingFilter](../../../src/main/java/com/sk/common/filter/HttpLoggingFilter.java)는 EDT 경로를 제외하지 않아 요청/응답 body와 MDC의 REQUEST_BODY를 남길 수 있고 wrapper는 body를 먼저 읽는다. 제안 응답 일부 reader의 중복 key 검사도 EDT 요청에 자동 적용되지 않는다. | 공통 API의1 MiB/깊이8·명령500/ADD100·TEXT4096, 영속 delta16 MiB 등 기존 계약을 EDT 읽기/저장 경계에서 강제. HTTP/MDC body 수집/출력을 해당 경로에서 제외해야 하며 handler 뒤의 검사만으로 앞선 전체 body 캐싱 제한/비노출이 보장되지 않음. 공통 mapper/filter 전체 개편을 선결하지 않고 영향 경로에 필요한 최소 변경. 실제 DB packet/접속 설정은 DBA-03 |
| 업로드 metadata 원자성 | FileService의 staged 업로드는 PUT 뒤 TEMP/0 metadata를 별도 저장, attach는 row 잠금 후 관계만 바꾼다. 최종준비fileNo와 Case 관계는 구분된다. | §6.3 신규 준비 기록의 전용 key는 PUT 전에 commit, tcm_filel/fileNo는 최종 성공 transaction에서만 생성한다. 기존 TEMP/0 row를 이 저장소로 사용하거나 기존 Scenario staged TTL 대상에 편입하지 않음. commit UNKNOWN/재시작·정리 연결은 domain §5.4를 따른다 |

소스 검색 범위에서 catalog6개/Case-template·신규7개에 대응하는 entity/repository 및 EDT endpoint는 발견하지 못했다. 따라서 기존 파일4개를 읽는 기능이 있어도 고정 발행/개인 Draft·재전송/복구 persistence가 이미 구현됐다고 할 수 없다. 실제 DB에 해당 table이 없는지도 소스만으로 결론내리지 않고 DBA-01로 확인한다.

### 6.5 EDT-01~03 첫 구현용 수동 DDL — 2026-10-16

2026-10-21 신규 Case 공유 저장 설계는 §6.12를 따른다. 아래 전체 원본 행/위치 저장은 기존 `base_id IS NULL` 경로의 기준이며 신규 공유 저장은 별도 증분 적용한다.

검토/수동 적용 SQL 정본은 [model-input-draft-read-ddl-draft.sql](../../model-input-draft-read-ddl-draft.sql)이다. §6.3 전체7개 중 첫 생성/조회에 필요한 Draft·고정4파일·revision0·행ID·위치이력 **5개만** 생성한다. 요청/최종준비파일 테이블과 그 prcs_id FK는 이후 변경/최종저장 slice에서 추가하며 지금 기능이 쓰지 않는다. catalog6개는 기존 설계의 신규 설치용 구간으로 제공하고 이미 존재하면 pre-check 뒤 해당 CREATE를 건너뛴다. 실행/DB 적용은 사용자 담당이다.

`tmd_mdl_inpttbld.qry_json LONGTEXT NULL`을 추가한다. 이는 발행 묶음에 고정되는 버전1 **조회 정의**이며 전체 셀/사용자 입력/새 업무 정책을 저장하지 않는다. NULL/미지원 버전/미완비 정의는 POLICY_INCOMPLETE/POLICY_UNSUPPORTED이고 현재 ACTIVE/MAX로 보정하지 않는다. 새로운 DRAFT 발행에서만 정의를 작성하고 발행 후 불변/기존 Case 연결을 유지한다. JSON_VALID와 최대1 MiB/표를 검증한다.

조회 정의 version1의 필수 key는 schemaVersion(1), columns(1기반 표시 열번호 배열), headerRows(반복 헤더 행번호 배열), bodyRanges(startRow/endRow), identityColumns(활성 원본 행을 식별할 비수식 입력 열번호 배열), fields(편집 API field metadata와 columnNumber), capabilities(SET/CLEAR/ADD_ROW/DELETE_ROW의 승인 준비도), inputGuideRules/controls/displayUpdateRules(확인된 조회 규칙 배열)다. 빈 규칙 배열은 실제로 없음이 확인된 경우만 등록한다. identityColumns는 업무 중복 키/행ID가 아니며 해당 행의 식별 입력이 모두 빈 경우 EMPTY_SLOT로 분류하는 읽기 근거다. 미확인 행/표·필드를 임의 추정하지 않는다. 기존 승인 정책 밖 동작은 발행 READY로 만들지 않는다.

2026-10-22 확장: `navigationVisible`은 선택적 boolean이며 생략하면 `true`다. `false`인 표도 내부 catalog·control 소유 범위·scopes에 남는다. API 탐색 제외 동작은 입력 제공 명세 §3이 소유한다. 기존 LONGTEXT/JSON_VALID 범위 안의 발행 데이터 확장이며 DDL 변경은 없다. 새 key를 읽는 백엔드의 호환 반영을 먼저 확인한 뒤 이 key를 포함한 새 양식을 발행한다.

**Regas 원본 연결 행 예외(2026-10-20):** domain 규칙51의 `SUPPLY/RegasAdjustment` 월별 표 중 발행 `identityColumns=[2,3,4,5,6]`인 정의에만 B:F master 연결 수식 예외를 적용한다. 같은 고정 파일의 `BCRegasification` 활성 원본 행을 먼저 확인하고, 각 행의 +3 위치에 있는 Regas B:F 다섯 수식이 같은 master 행/열을 직접 참조하는지 전부 대조한 후 별도 원본 rowId를 배정한다. 두 표의 경계·행 집합·수식 대응이 맞지 않으면 `POLICY_INCOMPLETE`로 생성하지 않는다. 설비명/저장된 계산값으로 행을 연결하거나 다른 표의 formula identity를 허용하는 예외가 아니다. 기존 구조 조회 카탈로그처럼 H:AE 비수식 입력 열을 identityColumns로 발행한 정의는 일반 활성 행 판정을 유지한다. 시트명만으로 B:F 식별 방식으로 교체하거나 발행 카탈로그를 수정하지 않는다. §6.9.5의 안정 rowId/내부 연결 계약을 유지하며 JSON key·물리 schema를 추가하지 않는다.

입력 양식은 Case에 연결된 ACTIVE/DEPRECATED 발행으로 결정한다. application model-code 설정과의 일치는 요구하지 않는다. 양식 미연결/삭제/미발행은 POLICY_INCOMPLETE이며 현재 ACTIVE/MAX로 대체하지 않는다. DB의 mdl_spt_cd와 모델별 버전/ACTIVE 유일성 제약은 유지한다. worker의 모델 코드 간 처리 범위와 고정 기준 검증은 domain §5.5를 따른다.

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

### 6.7 EDT-04/05 공통 요청 증분 DDL — 2026-10-18

수동 증분 정본은 [model-input-draft-change-request-ddl-draft.sql](../../model-input-draft-change-request-ddl-draft.sql)이다. 기존 적용 catalog/Draft 5개·Case103을 유지하고 §6.3의 요청 이력1개 및 성공revision/신규행의 동일 Draft 요청 복합 FK2개만 추가한다. 최종 준비파일은 제외한다. 사전 확인→CREATE→누락 FK→verify 순서이며 실제 적용은 사용자 담당이다. backfill은 없다. 롤백은 접수 중단·실제 commit 확인 후 이력을 보존하는 앱 롤백이며 DROP/데이터 삭제를 사용하지 않는다.

ERD 검토: 기존 승인 물리 정의를 재사용한다. 요청의 원자적 명령 배열/결과는 의도된 JSON payload이며 관계형 소유·유일성과 분리한다. PK/UK는 §6.3을 유지하고 상태 단독 index는 추가하지 않는다. Draft→요청 순서의 row 잠금과 같은 master transaction을 사용한다. 신규 사전 용어는 없으며 감사 컬럼 예외와 MySQL 실물 CHECK/nullable generated UK·경합·packet/EXPLAIN은 DBA 후속이다. 기존 tmm 접두사 제약은 재명명하지 않는다. 적용 준비도는 사용자 pre-check 후 ready이며 실제 MySQL 통과는 미확인이다.

### 6.8 감사 시간 정밀도 정렬 — 2026-10-19 사용자 채택

사용자는 실제 DB/코드의 `crt_usr_logn_id/crt_dtm/mod_usr_logn_id/mod_dtm/del_yn` 이름을 유지하고 기존 Case·파일·회사·사용자와 같은 **생성/수정 DATETIME(0)**을 채택했다. 이 방향은 catalog6개와 §6.3 Draft7개에 적용하며 아직 없는 최종준비파일은 후속 CREATE 때 적용한다. 기존 테이블을 재생성하지 않는다. 신규 단어·이름·관계·PK/FK/UK/정규화/인덱스·소유권 변경은 없다. audit DEFAULT/ON UPDATE는 CURRENT_TIMESTAMP(0), 적용/처리 시간 `aply_bgn_dtm/expr_dtm/bgn_dtm/end_dtm`은 DATETIME(6)을 유지한다.

새 접수는 DB 현재 시각의 초 아래를 버린 같은 값을 crt_dtm/mod_dtm에 넣고 그 값+120초를 expr_dtm에 저장한다. 실제 접수 후 허용 시간은120초를 넘지 않으며 최대1초 미만 짧아질 수 있다. 만료/claim/성공 조건은 기존 마이크로초 DB 시계로 비교하고 Draft→요청 잠금·시도 토큰·원자 성공 경계를 유지한다. 감사 시각으로 revision 순서/worker 재시도 여부를 결정하지 않는다. 공개 endpoint·조회/숫자 계약·작업 정책 차단은 변경하지 않는다.

증분 적용·사전/사후 조회의 정본은 [감사 시간 증분 SQL](../../model-input-audit-precision-ddl-draft.sql)이다. 모든 writer 중단·진행 작업0·PK별 백업과 손실 확인 뒤, 호환 코드 배포→기존 감사 소수점 명시 절삭(요청은 생성/만료를 같은 UPDATE로 정렬)→단계별 ALTER→정의/건수/기준 Case103·Draft/행ID 보존 확인 순서다. request crt_dtm의 ck09는 같은 ALTER에서 drop/re-add하여 MySQL의 CHECK 참조 컬럼 변경 규칙을 지킨다. 처리 시작/종료 시간은 보존하며 과거 종료 요청의 만료만 생성 소수점만큼 앞당겨 ck09를 유지한다. 손실은 타입 재확대로 복원되지 않으며 정확 복원은 백업이 필요하다. DDL은 자동 commit되므로 일괄 transaction rollback으로 간주하지 않는다. 생성/변경 SQL 파일은 빈 환경/누락 table용 최신 정의이며 기존 적용 DB에는 재실행하지 않는다.

ERD/code 영향은 감사 정밀도와 접수 기준의 연계 수정에 한정한다. 직접 SQL Repository 경로이며 JPA BaseEntity/기존 엔티티·tenant 조회/DTO를 변경하지 않는다. 이름 CRT/MOD/DTM은 기존 사전 용어이고 새 등록은 없다. 사용자 채택은 외부 DBA 공식 승인이나 실제 MySQL 적용 성공을 뜻하지 않는다. 구현 입력은 `ready`, 수동 적용은 SQL pre-check/백업·손실 확인/유지보수 조건을 충족한 뒤 `ready`다. UTC/한국 시간 설정·외부 표준 확인은 DBA-04 잔여이며 이번 정밀도 수정에서 바꾸지 않는다. [MySQL 소수점 처리](https://dev.mysql.com/doc/refman/8.0/en/fractional-seconds.html)와 [CHECK 참조 컬럼 ALTER 제약](https://dev.mysql.com/doc/refman/8.0/en/alter-table.html)을 적용 근거로 한다.

<a id="column-storage-impact-20261019"></a>

### 6.9 열 저장·복원 v2 — 미배포 단일 형식 / Draft 초기화

**2026-10-19 사용자 정정·확정:** 이 기능은 미배포 구현 중이다. v1 이력을 유지하는 이중 reader/변환/backfill은 만들지 않는다. 초기화 범위는 **Draft·변경 요청·저장 이력**이며 원본/최종 Case·입력 파일·공통 파일 정보·카탈로그·Case-template 연결은 보존한다. v2는 Draft 저장 JSON 형식의 번호이며 이 보존 대상 전체의 버전이 아니다. 앞선 v1/v2 공존·`IN (1,2)` 권고는 이 결정으로 대체한다. 실제 DB 적용은 이번 작업에 포함하지 않는다.

#### 6.9.1 물리 저장·버전·코드 경계

- 기존 `tmd_mdl_inptdrftprcsl`의 req_json/rslt_json/fail_json(각1 MiB), `tmd_mdl_inptdrftrvsnl`의 dlta_json(16 MiB)을 재사용한다. 새 열 table/관계형 column/index/FK는 없다. 저장 JSON은 전체 workbook이 아닌 검증된 변경 결과와 필요한 구조 metadata다.
- 새 Draft의 revision0과 모든 후속 성공 revision, CHANGE/FINALIZE 요청의 `json_rvsn_no`는 **2**다. revision0은 기존처럼 base/prcs/delta=NULL이다. 이 숫자는 사용자 Draft revision과 독립이다. 저장 버전1/알 수 없는 버전은 자동 변환하지 않고 안전한 저장 이력 오류로 차단한다.
- 요청 이력 ck03은 `base_rvsn_no<=9007199254740991 AND json_rvsn_no=2`로 교체한다. revision table에는 버전1 전용 CHECK가 없어 ALTER하지 않고 reader/writer 검증과 배포 후 조회로2를 확인한다. 기존 키/크기/JSON_VALID/감사/상태 제약은 유지한다.
- `qry_json.schemaVersion`은 **기존 조회 정의의1을 유지**한다. 아직 배포 전이므로 그 정의 parser에 편집 명세 §3.3의 열 metadata를 확장한다. 기존 catalog를 저장 이력 v2로 일괄 치환하지 않는다. 기존 정의에 없는 두 열 capability/필드 DELETE_COLUMN은 INCOMPLETE로 취급하며 READY로 추정하지 않는다. 신규 열 지원 정의에는6capabilities·4field policyStatus와 READY일 때 columnCreation이 필요하다. 기존 타입·서식/입력/갱신 규칙을 재사용한다.
- 따라서 이 저장 전환의 catalog DML/seed는 **없다**. 각 표의 실제 열 생성/계산·보존 정책을 연결하는 catalog 작성은 해당 처리기 구현과 함께 수행하며, 이 SQL로 미지원 표를 READY로 열지 않는다.
- 구현 지점: InputDraftJson/Controller 요청 해석, InputReadDefinition/ModelInputDraftResponses, ModelInputDraftService의 접수·결과·현재 값 조회, InputDraftHistory의 구조/값 복원, ModelInputDraftRepository의 요청/revision0/성공 version 및 claim/완료 검사, worker·최종저장의 동일 version 처리. 공통 코드에서 저장 번호2와 열 identity 복원을 구현하며 catalog schemaVersion1은 유지한다. 실제 표별 열 처리와 catalog READY 확대는 별도다. FINALIZE 공개 결과 형태는 기존 명세를 유지하고 저장 번호만2로 일치시킨다.

#### 6.9.2 요청·성공 결과·delta의 정확한 형식

명시한 모든 key는 필수이고 다른 key/중복 key는 거부한다. nullable 필드도 생략하지 않는다. ID/정확 수치·순서/원자성·접근 경계는 [편집 API §4~6](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md)이 소유한다. 아래는 BE 내부 성공 저장 형식이며 FE가 delta를 작성하지 않는다.

| 저장 위치 | v2 형식 |
| --- | --- |
| CHANGE req_json | 공개 `{requestId,baseRevision,operations}` 그대로. 기존6명령의 의미/순서·정확 숫자 보존 |
| CHANGE rslt_json | `{resultRevision,rowMappings,columnMappings,affectedTableIds}`. 신규 열마다 공개 mapping5필드; 진행/실패는 성공 result를 저장하지 않음 |
| fail_json | 기존 Problem. 실패 요청의 새 열/row mapping을 성공 이력에 넣지 않음 |
| dlta_json top-level | `{policyTemplateId,commands,tables,rows,columns,cells}`. policyTemplateId는 고정 양식 ID, commands는 저장 요청 operations와 의미/순서 동일. 다섯 배열은 비어도 필수 |
| tables[] | `{tableId,startRow,endRow,startColumn,endColumn,dataStart,definition,scopes}`. 구조가 바뀐 표의 **변경 후 전체 구조 정의**. definition은 확장된 조회 정의, scopes는 기존4좌표 object 배열. 기존행/열 구간·header/body/fields/controls/갱신 규칙의 이동과 파생표 변경을 함께 기술 |
| rows[] | 직접 행은 기존 `{tableId,rowId,clientRowKey,rowNumber}` 유지. 자동 연결 행 생성은 §6.9.5의 `derivedFromRowId`/예약 내부 key 예외를 따른다. 기존 행은 clientRowKey:null, 삭제는 rowNumber:null. relational row identity/history와 대조 |
| columns[] | `{tableId,fieldKey,sourceColumnNumber,columnNumber,clientColumnKey}`. 추가/이동/삭제된 열만 한 번씩. 아래 identity 규칙 적용 |
| cells[] | 기존 `{tableId,rowId,rowNumber,columnNumber,raw,display,formula}` 유지. 최종 위치의 값 덮어쓰기. 활성 논리 행은 rowId만, header/control/비활성 슬롯은 rowNumber만 지정하고 다른 행 참조는null. raw/display는 기존 scalar 또는null, formula boolean. 빈칸은 raw/display:null·formula:false |

**열 identity와 초기 상태**

1. Draft revision0에서 표 경계의 원본 열 대응을 고정 catalog로부터 구성한다. 기존 편집 field는 catalog fieldKey를 그대로 쓰고 sourceColumnNumber는 원본 Excel1-based 열 번호다. 편집 field가 없는 읽기 전용 원본 열은 fieldKey:null이며 sourceColumnNumber로 내부에서 식별한다. 이는 새 공개 columnId를 추가한 것이 아니다. 전체 표시/고정 열을 포함하되 각 표의 경계 밖 값까지 소유하지 않는다.
2. 신규 열은 서버의 새 fieldKey(공개 key 형식)를 가지며 sourceColumnNumber:null, clientColumnKey는 해당 ADD_COLUMN의 임시키다. 기존 열의 변경은 clientColumnKey:null이며 sourceColumnNumber/fieldKey는 불변이다. sourceColumnNumber와 fieldKey가 둘 다null인 열은 금지한다.
3. columnNumber는 최종 Excel1-based 위치(1~16384) 또는 삭제표시 null이다. sourceColumnNumber가 있는 열은 그 원본 열 identity로, 신규 열은 fieldKey로 이전 상태를 찾는다. fieldKey가 있는 기존 열도 기존 source 대응을 함께 검증한다. 동일 표의 source 번호/nonnull fieldKey는 유일하며 삭제 상태까지 충돌 검사를 유지한다. 활성 columnNumber는 서로 달라야 한다.
4. 삭제한 키는 다시 활성화/재사용하지 않는다. 같은 연월 재추가는 sourceColumnNumber:null인 새 fieldKey다. ADD→DELETE는 columns에 새 키/clientColumnKey/columnNumber:null을 남기고 rslt_json의 columnMappings에 DELETED를 남긴다. fieldKey:null인 원본 읽기 전용 열은 공개 SET/DELETE_COLUMN 대상이 아니다.
5. 생존 열의 이동도 columns에 기록한다. 전체 적용 후 definition.columns/fields.columnNumber·scopes·bounds·control binding이 최종 활성 위치와 맞아야 한다. 표시에서 제외된 빈 꼬리 슬롯을 원본 열 identity로 다시 활성화하지 않는다. 물리 파일의 남는 서식/빈 슬롯 보존은 각 표의 승인 STRUCT 처리기가 담당한다.
6. 원본 catalog는 불변이다. 새 fieldKey/현재 위치·삭제 표시는 해당 Draft의 delta chain에만 남긴다. readonly MAX 등 파생 열의 이동은 원본 source identity를 유지한다. FE의 명령만 보고 고정 열/다른 표의 정책을 새로 허용하지 않는다.

**값 복원 순서 — 좌표 이동 전에 이전 값을 읽는다**

1. 소유 Draft/원본·고정 양식 검증 뒤 revision0부터 요청·성공 revision의 연속성(parent=n-1), SUCCEEDED CHANGE, base/result/version2를 확인한다.
2. commands를 순서대로 해석해 임시 행/열 참조와 삭제 후 참조를 검증한다. 직접 추가한 rows/columns의 신규 identity가 각 공개 ADD와 정확히1대1인지 확인한다. 기존 키의 출처 바꾸기·이동 충돌·삭제 부활은 저장 오류다. 자동 연결 행의 별도 생성·삭제 대조는 §6.9.5의 내부 저장 보완을 따른다.
3. 이번 revision의 최종 rows/columns/구조를 먼저 검증하되, **이전 성공 셀 overlay 전체를 이전 상태에서 수집**한다. rowId는 유지하고 열 값은 이전 열 identity→새 columnNumber로 이동한다. 삭제 열의 값은 버리고 신규 열에는 이전 물리 위치의 값을 복사하지 않는다. 고정 셀은 기존 header 순번/control ID의 행 이동과 열 이동을 함께 적용한다. 공유 control은 owner 기준 한 번만 이동하고 상충하는 목적지 값은 실패시킨다.
4. 이동을 마친 뒤 이번 cells를 최종 좌표에 덮어쓴다. 명시 blank도 값이므로 이동값/원본 값을 가린다. 현재 commands의 마지막 SET/CLEAR, 생성 기본값·자동 header/계산 결과가 이 최종 cells/상태와 맞는지 검증한다. 이미 삭제된 행/열에는 최종 cells를 남기지 않는다.
5. 조회 시 원본 셀을 읽는 주소는 **sourceRow와 sourceColumnNumber의 교차점**이다. 현재 columnNumber로 원본 파일을 읽지 않는다. 둘 중 하나라도 신규라 원본 대응이 없으면 시작은 빈칸이다. 그 위에 누적 성공 overlay를 적용한다. 새 열 빈칸/명시0·이동된 값·삭제 후 같은 위치 재사용을 구분한다.
6. 새 열의 미입력 칸은 승인된 빈칸 정책일 때 생략 가능하다. BCRegas0 등 확정 기본값·필수값·자동 header/계산값은 처리기가 materialize한 cells로 명시한다. 복원 시 현재 catalog의 기본값/Period를 다시 계산하지 않는다. 숫자를 display 문자열에서 역변환하지 않는다.
7. 동일 성공 transaction으로 delta·row history·result의 두 mapping·Draft revision·요청 SUCCEEDED를 확정한다. 결과는 ADD 순서의 rowMappings/columnMappings, commands 순서로 처음 나타난 표 뒤에 추가 영향 표를 tableId 순서로 붙인 affectedTableIds로 결정한다. 복원한 결과와 rslt_json을 대조한다. 동일 requestId 재전송은 이 저장 결과를 사용하고 새 키를 발급하지 않는다.
8. 최종 파일 생성도 원본+같은 v2 성공 chain으로 재현한다. 수식 재배치/계산은 승인된 처리기가 소유하며 조회에서 계산하지 않는다. 실패/timeout은 전체 변경을 공개하지 않고 직전 성공을 유지한다. 알 수 없는 형식/불일치의 읽기는 기존 안전한500, 접수 후 저장 검증 실패는 PERSISTENCE_FAILED다.

#### 6.9.3 작은 형식 예시 — 실제 workbook/seed 아님

가상 `FIXTURE/Monthly/input`의 원본 B열 식별값·C열 월 입력 뒤 D열을 추가한 revision1 예시다. 아래 정책은 형식 검증용 fixture이며 실제 표 허용 범위를 넓히지 않는다. 양식 ID7001·rowId·좌표도 가상이다. sourceColumnNumber:null이므로 D열의 미입력 본문은 원본 D열을 읽지 않는다.

```json
{
  "policyTemplateId": 7001,
  "commands": [
    {
      "commandId": "addMonth",
      "type": "ADD_COLUMN",
      "tableId": "FIXTURE/Monthly/input",
      "clientColumnKey": "newMonth",
      "headerValues": {
        "year": 2032,
        "month": 3
      }
    },
    {
      "commandId": "setZero",
      "type": "SET",
      "tableId": "FIXTURE/Monthly/input",
      "rowRef": {
        "rowId": "a0000000-0000-4000-8000-000000000011"
      },
      "clientColumnKey": "newMonth",
      "value": 0
    }
  ],
  "tables": [
    {
      "tableId": "FIXTURE/Monthly/input",
      "startRow": 1,
      "endRow": 4,
      "startColumn": 2,
      "endColumn": 4,
      "dataStart": 4,
      "definition": {
        "schemaVersion": 1,
        "columns": [
          2,
          3,
          4
        ],
        "headerRows": [
          1,
          2,
          3
        ],
        "bodyRanges": [
          {
            "startRow": 4,
            "endRow": 4
          }
        ],
        "identityColumns": [
          2
        ],
        "fields": [
          {
            "fieldKey": "name",
            "columnNumber": 2,
            "dataType": "TEXT",
            "policyStatus": {
              "CREATE": "INCOMPLETE",
              "SET": "READY",
              "CLEAR": "INCOMPLETE",
              "DELETE_COLUMN": "NOT_ALLOWED"
            },
            "setConstraints": {}
          },
          {
            "fieldKey": "month_old",
            "columnNumber": 3,
            "dataType": "DECIMAL",
            "policyStatus": {
              "CREATE": "INCOMPLETE",
              "SET": "READY",
              "CLEAR": "INCOMPLETE",
              "DELETE_COLUMN": "READY"
            },
            "setConstraints": {}
          },
          {
            "fieldKey": "month_new",
            "columnNumber": 4,
            "dataType": "DECIMAL",
            "policyStatus": {
              "CREATE": "INCOMPLETE",
              "SET": "READY",
              "CLEAR": "INCOMPLETE",
              "DELETE_COLUMN": "READY"
            },
            "setConstraints": {}
          }
        ],
        "capabilities": {
          "SET": "READY",
          "CLEAR": "INCOMPLETE",
          "ADD_ROW": "INCOMPLETE",
          "DELETE_ROW": "INCOMPLETE",
          "ADD_COLUMN": "READY",
          "DELETE_COLUMN": "READY"
        },
        "inputGuideRules": [],
        "controls": [],
        "displayUpdateRules": [],
        "columnCreation": {
          "headerValues": {
            "year": {
              "dataType": "INTEGER",
              "create": {
                "required": true,
                "initial": {
                  "kind": "EMPTY"
                },
                "constraints": {}
              }
            },
            "month": {
              "dataType": "INTEGER",
              "create": {
                "required": true,
                "initial": {
                  "kind": "EMPTY"
                },
                "constraints": {
                  "minimum": 1,
                  "maximum": 12
                }
              }
            }
          },
          "body": {
            "dataType": "DECIMAL",
            "create": {
              "required": false,
              "initial": {
                "kind": "EMPTY"
              },
              "constraints": {}
            }
          }
        }
      },
      "scopes": [
        {
          "startRow": 4,
          "endRow": 4,
          "startColumn": 2,
          "endColumn": 4
        }
      ]
    }
  ],
  "rows": [],
  "columns": [
    {
      "tableId": "FIXTURE/Monthly/input",
      "fieldKey": "month_new",
      "sourceColumnNumber": null,
      "columnNumber": 4,
      "clientColumnKey": "newMonth"
    }
  ],
  "cells": [
    {
      "tableId": "FIXTURE/Monthly/input",
      "rowId": null,
      "rowNumber": 1,
      "columnNumber": 4,
      "raw": 2032,
      "display": "2032",
      "formula": false
    },
    {
      "tableId": "FIXTURE/Monthly/input",
      "rowId": null,
      "rowNumber": 2,
      "columnNumber": 4,
      "raw": 3,
      "display": "3",
      "formula": false
    },
    {
      "tableId": "FIXTURE/Monthly/input",
      "rowId": null,
      "rowNumber": 3,
      "columnNumber": 4,
      "raw": 171,
      "display": "171",
      "formula": false
    },
    {
      "tableId": "FIXTURE/Monthly/input",
      "rowId": "a0000000-0000-4000-8000-000000000011",
      "rowNumber": null,
      "columnNumber": 4,
      "raw": 0,
      "display": "0",
      "formula": false
    }
  ]
}
```

성공 rslt_json은 다음과 같다. QUEUED/RUNNING/FAILED에는 이 result가 없으며 공개 OperationView의 columnMappings는[]다.

```json
{
  "resultRevision": 1,
  "rowMappings": [],
  "columnMappings": [
    {
      "commandId": "addMonth",
      "clientColumnKey": "newMonth",
      "tableId": "FIXTURE/Monthly/input",
      "fieldKey": "month_new",
      "columnState": "ACTIVE"
    }
  ],
  "affectedTableIds": [
    "FIXTURE/Monthly/input"
  ]
}
```

#### 6.9.4 개발계·운영계 수동 적용과 복구

실행 SQL 정본은 [Draft v2 reset/apply/verify/rollback SQL](../../model-input-draft-column-v2-ddl-draft.sql)이다. 변경 구문은 명명된 MANUAL 블록으로 분리했으며 전체 파일 일괄 실행 대상이 아니다. 삭제 대상7개는 Draft master/file/revision/row identity/row position/request/finalization preparation이다. 마지막 항목도 Draft 소유 처리 metadata이며 COMMITTED 파일 자체/Case는 보존한다.

| 단계 | 개발계 | 운영계 | 통과 기준/담당 |
| --- | --- | --- | --- |
| 준비 | 실제 master schema·기존 정의·미적용 table 확인 | 별도 운영 연결에서 같은 확인. 개발계 이름/ID/백업을 복사하지 않음 | BE/DBA: 환경 식별, MySQL8 CHECK 강제·InnoDB, schema diff 확인 |
| 중지·보관 | 신규 접수 중지→진행 작업/준비파일 정리→모든 writer/worker 중지→7table backup | 같은 순서. 아직 미배포라도 외부 시험 writer 없음 확인 | active pointer/QUEUED/RUNNING/미정리 준비파일0. 보존 대상과 FK/trigger 검토 |
| 초기화 | SQL §2 transaction·§3 검증 후 명시 COMMIT | 개발계 검증 후 같은 SQL을 운영계에서 별도 실행 | 7table0, 원본 Case/파일/catalog/연결 불변. 예외면 ROLLBACK |
| schema | 비어 있는 요청 table의 ck03을 =2로 교체 | 동일한 승인 SQL·결과 대조 | upper bound/나머지 제약 불변. DDL 결과 불명은 정의 조회 후 판단 |
| 배포·확인 | v2 전용 writer/reader/worker/FINALIZE 전체 교체 후 새 Draft 시험 | 개발계 동작 검증 결과를 확인한 같은 구현을 배포 | 기존1 하드코딩 제거·전체 소비자 동일. 구 코드와 혼합 가동 금지 |
| 열 활성화 | 실제 표별 정책/처리·파일 검증 뒤 READY | 검증된 범위만 READY | 저장 형식/SQL 준비만으로 전체 열 기능 완료로 판정하지 않음 |

신규 환경은 기존 read→change-request→finalization DDL의 **없는 table/FK만** 먼저 생성하고 이 v2 SQL을 적용한다. 기존 table은 재생성하지 않는다. 기존 audit 정밀도 등 별도 승인 변경은 그 정본을 따르며 이번 SQL에서 임의로 합치지 않는다. 신규 열 정책을 배포하는 catalog DML은 이번 reset SQL과 분리한다.

DDL은 암묵 commit되므로 reset의 DELETE transaction을 검증·COMMIT/ROLLBACK으로 끝낸 뒤 ALTER한다. 최신 revision부터 지우는 single-table ORDER BY DELETE로 self-FK를 지키며 FK 검사를 끄거나 TRUNCATE하지 않는다. [MySQL DELETE](https://dev.mysql.com/doc/refman/8.0/en/delete.html), [ALTER TABLE](https://dev.mysql.com/doc/refman/8.0/en/alter-table.html), [암묵 commit](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html)을 따른다.

롤백 경계는 다음과 같다. reset COMMIT 전에는 transaction ROLLBACK으로 기존 Draft를 복구한다. COMMIT 후에는 초기화가 확정되며 DDL 원복만으로 데이터가 돌아오지 않는다. 새 v2 데이터가 하나도 없는 경우에만 ck03을1로 되돌려 **빈 구 코드 환경**으로 복구할 수 있다. 새 v2 데이터가 생기면 쓰기를 멈추고 v2 수정 배포로 복구한다. 이번 초기화 승인이 이후 새 데이터의 자동 삭제/재초기화를 허용하지 않는다. 오대상 삭제의 backup 복구는 모든 writer 중지·빈 대상 확인 후 부모부터 수행하는 DBA 수동 절차이며, v1을 v2로 변환하거나 버전 번호만 바꾸는 복구는 없다.

#### 6.9.5 검증·준비도

- 2026-10-23 Midstream 기존행 편집: 업무 정본 규칙59의 일반 보존 결정을 신규 v6 DRAFT의 PNG/LNG 두 표 SET에 연결한다. 원본 열/본문/identity/uploadValidation은 유지하고 입력 scopes는 A5:D1104·Q5:U2918만 둔다. 계산 결과/외부 셀 delta를 생성하지 않으며 기존 셀 delta와 성공 이력 재생으로 입력만 복원한다. CLEAR/빈 문자열·생성/삭제 미결은 개방하지 않는다(편집 API §3.3). schema/기존 v5 설치 생성물·ACTIVE·Case 연결 변경은 없다. v6 전체 준비도는 별도 BCRegas2건 미해결 상태이며 SQL 생성/로컬 검사로 발행 준비 완료를 주장하지 않는다.

##### 실제 편집기 파생 열 위치 소유 — 2026-10-19

- 2026-10-23 신규 편집 양식 초안은 `docs/model-input-editor-remaining-v6-policy.json`과 이를 읽어 생성하는 `docs/model-input-editor-remaining-v6-registration-draft.sql`을 따른다. 기존 v5 정의·ACTIVE·Case/Draft 연결은 수정하지 않는다. 표 내부 경계에 필요한 계산 열을 포함해도 공개 `columns`·`identityColumns`·업로드 고정 헤더는 기존 정의를 유지하며, 계산 열을 사용자 입력으로 열지 않는다. BCRegasification의 기존 G:Q 및 연도 추가 연결은 독립 검토에서 요청 검사 불일치2건이 확인돼 수정 합의 대기이며 적용할 수 없다. master B:F/행 추가·삭제는 일반 READY로 확대하지 않는다. PMI 두 표는 기존 D/I 국가 SET(필수 자유입력·CLEAR 금지)과 E/J 강도 SET/CLEAR를 연결한다. 대륙·지역과 Selected는 국가 변경으로 자동 갱신하지 않으며 행 추가는 미개방이다(domain §6.8). 기존 Weather User-Adjusted 두 표는 검증된 국가/강도 SET/CLEAR만 연결하고 행 추가는 유지 보류한다. 현재 준비도/검증은 안정 작업 노트에 기록하며 등록 SQL 생성 자체가 발행이나 배포를 뜻하지 않는다.

- DemandAdjLogicInput의 기존 Parameters 표는 C1:F3 경계를 유지하고 D2:D3/F2:F3만 입력한다. Premium(2행) 변경은 원본 L3:L7, Non-premium(3행) 변경은 M3:M7 수식으로 계산하며 결과는 숨김 계산영역에 유지한다. Bands는 H1:J7 경계의 기존 H/J 숫자와 I 숫자/문자 혼합 입력을 제공한다. H/I 변경은 해당 K 연결식, J 변경은 해당 L/M만 갱신하며 중앙5행 L/M은 원본 D2/D3 직접 참조라 J5 변경의 계산 대상이 아니다. 계산에 필요한 L5/M5는 원본 수식으로 평가하되 D2/D3가 바뀌지 않았으면 저장 셀은 보존한다. C/E 라벨·나머지 원본은 보존한다. I의 숫자/문자 및 실제 빈칸을 구별하며 API 타입은 편집 API §3.3을 따른다. 이전 L/M cache를 계산 근거로 쓰지 않는다. 2026-10-23 업무 정본 §6.9에 따라 두 표는 기존 값 수정 전용으로 고정한다. 신규 v6 DRAFT는 rowAdd/columnAdd=N 및 ADD_ROW/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN=NOT_ALLOWED, 필드 CREATE/DELETE_COLUMN=NOT_ALLOWED를 명시한다. 신규 유형/행을 잔여 구현으로 남기지 않으며 기존 v5/발행 양식은 유지한다.

- ShippingCostInput 운임5표·운하중단2표는 원본 B:F 또는 B:D의 고정 헤더/기존 본문을 유지한다. 연속 배치된 다른 표를 이동하지 않고, 신규 논리 행은 고정 원본 B:F의 마지막 내용 행과 현재7표의 예약 행 뒤에 요청 순서로 배정한다. 기존 `bodyRanges`/scope를 비연속 범위로 확장하여 다른 표의 중간 행을 포함하지 않는다. 운임은 기존 C3 모드/현재 입력을 기준으로 E만 계산하고, Pacific/Atlantic의 새 C는 직전 논리 행 C×1.02로 생성한다. E의 직전 E/D 참조도 물리 인접 행이 아닌 같은 표의 직전 논리 행이다. 운하중단의 신규 D는 고정 D61/D72 참조로 생성한다. C3 공유 control SET은 운임5표에 반영하며 A 연간 운송비·기존 외부/다른 표는 보존한다. 표별 실제 준비도는 구현/검증을 마친 후 발행하며 이 선언만으로 READY가 되지 않는다.

- MidstreamCapacityInput 배분표는 내부 DX1:EC끝행에 EC 결과를 포함하고 표시/입력은 DX:EB만 유지한다. DX/DZ/EA/EB 변경행 및 신규행의 EC를 원본 INDEX/MATCH식으로 계산하며 A 보존인 FK:FM의 저장값만 읽는다. DY만 바꾼 행은 EC를 보존한다. Nord Stream FB:FD는 입력/추가만 제공하며 SUMMARY43/44의 FF:FI·FK:FM을 포함한 별도 A표를 갱신하지 않는다.

- ShippingCostInput 노선표는 내부 AC2:AM끝행을 유지하고 입력/표시는 AC:AE·AH:AI·AL:AM이다. 기간/속도/운하 변경행과 신규 행은 원본 AF/AG 및 AJ/AK 수식으로 계산하고 연료만 바꾸면 AJ/AK만 계산한다. AC/AD/AM만 수정한 행의 파생값은 유지한다. 고정 추가 거리 D61/D72와 A 연간 운송비표는 보존한다. 신규 네 계산 셀은 원본 마지막 행 서식/상대 수식을 사용하며 기존 rowId에 결과를 보관한다.

- DEMAND MacroParametersLogicInput은 표시/입력 I:P를 유지하고 내부 경계 I7:Q끝행에 Q 연결식 결과를 같은 rowId로 보관한다. I/L 입력 변경행과 신규 행의 Q만 원본 `I행&L행`으로 계산하며 B:F의 A 선택표·다른 행 Q는 보존한다. 원본 I8:P끝행 자동 필터와 대응 숨김 이름만 입력 마지막 행까지 확장한다. BCInput 규칙58의 별도 Helper 보존 예외를 이 표로 확대하지 않는다.

- ShippingCostInput Technical Parameters는 원본 B81:E83 내부 경계를 유지하고 B83/E83만 입력한다. B83 변경 시 원본 C83을 읽어 D83 수식/결과만 갱신하며 C83·A 운송비 계산표는 보존한다. Panama Spot Capacity는 B88:E90 고정 입력이며 ADD를 제공하지 않는다. Importer Classification AP:AQ와 Exporter List AT는 SUMMARY의 아래 목록 추가 범위를 유지하고 관련 A 운송비 원본을 재계산하지 않는다.

- TRADE UpstreamAdjLogicInput의 고정 공급조정표는 내부 I1:R674에 Q/R 계산 결과를 보관하고 표시/입력은 I/J/P만 유지한다. P 변경은 원본 AZ 기준값을 읽는 Q식, I/J 변경은 R 연결키만 갱신하며 외부 K/M과 O·General/AZ는 보존한다. 국가 목록은 내부 T1:U끝행·본문4행부터로 U2 COUNTA를 같은 표에 저장한다. 입력은 T열뿐이며 기본 T3:T100 참조는 유지하고100행을 넘는 추가부터 현재 끝행까지 늘린다. Premium 국가 목록 A열은 별도 파생식을 생성하지 않는다.

- TRADE CostInput의 SRMC표는 AK:AN만 표시/입력하고 AO:AR 순위·연결키까지 내부 경계에 포함한다. 기존 연도/비용 변경과 신규 행의 이전·현재 연도 그룹만 원본 COUNTIFS/IF 수식으로 다시 계산한다. 동률은 원본의 위쪽 행 누적 순서를 유지한다. 기본 참조 끝1000행 안에서는 기존 수식을 유지하고 이를 넘어 추가하면 승인 표의 현재 끝행까지 순위 참조를 확장한다. 기존 AS의 외부 MATCH 수식/저장값·관련 externalLink는 보존하며 새 AS 수식은 생성하지 않는다.

- TRADE CostInput의 앞선5표는 입력 A:D/H:K/O:R/V:Y/AC:AG와 그 오른쪽 Selected Input·Helper 두 열을 내부 경계에 포함한다. 표시 columns/수정 scopes는 SUMMARY51..55의 입력 범위만 유지한다. 기준값·사용자값 변경은 Selected Input, 연도·국가 변경은 Helper만 같은 rowId에 계산하며 새 행은 원본 정상 수식을 상대 복사한다. 원본의 빈값 또는 숫자0이면 기준값을 선택하는 수식은 그대로 유지한다. 기존 예약 수식이 있으면 일치 여부를 확인한 후 사용하며 옆 표/외부참조/순위표를 이 처리에 포함하지 않는다.

- WeatherLogicInput의 네 입력표는 국가(D/I/N/S)가 변경된 행의 지리 정보 두 열(B:C/G:H/L:M/Q:R)만 원본 General F:H3..86 lookup 수식으로 계산하여 같은 rowId에 저장한다. 강도(E/J/O/T)만 변경하면 지리 정보 cache는 유지한다. 원본에서 아래 행 추가가 확정된 Base 두 표(L:O/Q:T)의 신규 행에만 정상 lookup 수식을 상대 복사한다. User-Adjusted 두 표의 미확정 선택표 동시 확장은 제공하지 않고, A 원본 보존인 V:AD의 값·수식/행은 변경하지 않는다. 기존 입력 범위/새 lookup 오류의 원자 실패를 유지하며 새 국가 목록 제한을 따로 만들지 않는다.

- HydrogenInput의 입력 F:H는 원본 본문9..736행을 유지한다. 그 아래 같은 열의 보존 Helper 블록은 이동하지 않고, 고정 원본 F:H의 마지막 내용 셀 뒤부터 신규 입력을 배치한다(현재 원본10078행). `bodyRanges`와 수정 scopes를 기존/신규 구간으로 분리하며 중간 Helper 행을 조회·입력 대상으로 포함하지 않는다. 기존 다중 본문 구간과 rowId 위치 이력으로 표현하므로 schema/public 필드를 추가하지 않는다. 판별대장 T062의 I Helper·F737 이후 다른 표 Helper 및 A영역은 원본 보존이며 신규 Helper를 만들지 않는다. LNGBunkeringInput은 H:K 입력만 확장하고 T058의 L 환산 계산열·M 보조열은 보존한다.

- {USA}Hurricanes는 내부 경계를 B3:I끝행으로 잡고 표시 columns/입력 fields는 B:D, 헤더7행·본문13행부터를 유지한다. 고정 source의 B:E8..12 오류 수식은 보존하며 입력 변경의 정상 E CHECK와 I3 집계만 같은 표의 v2 결과에 저장한다. 새 행 E는 E13 정상 원본 수식의 상대 참조를 적용한다. I3의 E8:끝행과 기준 셀 수를 함께 늘리고 다른 E행 cache는 원본/직전 성공값을 사용한다.
- BCProduction의 내부 표 경계는 A7:K끝행으로 잡고 화면/입력은 C:F·H:K를 유지한다. 기존 C 입력 변경은 B(연간 개월수), E/F 입력 변경은 G(연결키)만 부분 계산한다. 새 행의 A/B/G는 원본 정상 수식을 상대 행 이동하여 생성하며 A는 직전 행 순번을 잇는다. L의 저장값과 N:T 참조표/피벗은 복사·재계산하지 않는다. 자동 필터 A7:L끝행과 동일 숨김 필터 이름만 새 마지막 행까지 늘리며 원본 필터 조건·정렬 설정은 유지한다.
- ProductionAdjustment의 월간 입력 정의는 내부 경계를 H3:O끝행으로 잡고 화면 columns와 사용자 fields/scopes는 I:K·M, 헤더7행·본문8행부터를 유지한다. 기존 H 연결 Helper와 N/O의 영향받는 수식, M3 집계 결과를 같은 표의 v2 이력에 보관하기 위한 내부 경계이며 사용자 입력 범위를 늘리지 않는다. 신규 H/N/O는 정상 원본 수식의 상대 행 참조를 적용하고 기존 예약 N201/O201은 동일 수식인지 확인하여 유지한다. 규칙31의 기존 G/L 오류 셀은 재계산/복제하지 않고 신규 G/L은 비워 둔다. M3의 두 집계 범위와 기준 숫자 확장은 규칙31을 따른다.
- BCInput의 새 편집 발행 정의는 GDP/인구 표의 X열과 발전 이용률 표의 AO열까지 내부 table 경계를 포함하여 기존 수식의 변경 결과를 같은 rowId에 저장한다. 화면 `columns`/사용자 `fields`/수정 scopes는 각각 Q:V와 AK:AN을 유지한다. 발전량 표의 AG는 기존 Z:AI 내부 경계에 포함하되 사용자 필드에서 제외한다. domain 규칙58의 기존 AE:AF·저장값 Helper 보존과 신규 Helper 미생성은 유지한다. 기존 X/AG/AO4 수식의 선행 입력을 변경한 경우만 원본 수식을 부분 계산하며, Helper 값만 있는 셀을 수식으로 바꾸거나 새 행 Helper를 생성하지 않는다.
- BCRegasification의 새 편집 발행 정의는 원본 readonly `Maximum capacity` R열까지 내부 table 경계에 포함한다. 화면 `columns`와 사용자 `fields`/수정 scopes는 기존 B:Q 입력 계약을 유지하며 MAX를 사용자 입력으로 열지 않는다. 연도 추가 시 MAX는 원본 `sourceColumnNumber:18`, `fieldKey:null`을 유지한 채 현재 위치만 이동한다(§6.9.1의 파생 열 이동 계약). 새로운 연도 열은 별도 fieldKey를 갖는다.
- 연결 행 추가/삭제를 제공하는 새 정의는 A Helper까지 내부 경계 A:R에 포함한다. A는 표시/입력 필드가 아니며 같은 설비 행의 이동·신규 계산 결과를 rowId에 보관한다. 활성 설비 밖의 예약 Helper는 domain 규칙51대로 보존한다. Regas 국가 합계 블록은 연결 설비 증감 수만큼 header/body 경계를 함께 이동하며 기존 국가 rowId를 유지한다. 표시 행과 고정 원본 행의 대응은 §6.9.5의 위치 이력으로 복원한다.
- BCRegasification 기존 B:Q 입력 변경은 같은 rowId의 A 연결키(E/F 변경 시)와 Maximum(기존 용량 변경 시), 연결 월 표의 해당 B:F/DM을 먼저 갱신한다. 현재 설비명에 대한 첫 MATCH 대상이 달라지거나 그 대상의 기존 연간 용량이 변경된 생존 행만 G·AF:DK·DN:DV를 계산하고 국가 합계에 반영한다. 같은 요청의 직접 월 변경/구조 이동 결과를 사용하며, 새 연도 추가만으로 기존 연결 월 계산을 갱신하지 않는 domain 규칙51의 보존 예외는 유지한다.
- RegasAdjustment의 설비별 월 표는 같은 설비 행의 readonly 연간 합계 DM:DV까지 내부 경계를 포함하여 rowId별 계산 delta를 보관한다. 표시 `columns`는 기존 B:DK, 직접 수정 scopes는 H:AE를 유지한다. DL 간격은 변경 대상으로 삼지 않는다. 국가별 월 합계는 기존 별도 table/rowId를 유지한다. 이 경계 확장은 내부 계산 저장을 위한 것이며 숨긴 파생 열을 사용자 필드로 추가하지 않는다.
- 이는 미발행 catalog 정의/정책과 v2 delta를 연결하는 표현 보완이며 물리 schema 변경은 없다. 기존 발행 catalog를 수정하지 않고 새 발행 묶음에 등록하며, 실제 DB/DML 적용과 지원 READY 판정은 실파일 검증 뒤 별도 수행한다. RegasAdjustment의 연도 추가 보존 예외는 domain 규칙51을 따른다.

##### 실제 편집기 연결 행 저장 보완 — 2026-10-19 구현 검토안

- BCRegasification의 공개 ADD_ROW 하나가 만드는 RegasAdjustment 자동 행은 별도 UUID rowId를 갖는다. `rows[]`의 자동 행에만 `derivedFromRowId`를 추가하여 같은 Draft의 master rowId를 명시한다. 직접 행의 기존4키는 유지한다. 자동 행의 `clientRowKey`는 내부 예약값 `_regas_` + master UUID이며 공개 명령의 임시키로 사용할 수 없다(공개 키는 영문자로 시작). 이 값은 기존 `clnt_row_id VARCHAR(64)`·요청별 UK에 저장하므로 새 table/column/DDL은 필요 없다.
- 신규 자동 행의 생성 revision을 복원할 때 승인된 두 table 관계와 master의 공개 ADD_ROW/같은 성공 revision을 정확히1대1로 대조한다. 이후 이동·삭제는 이미 고정된 연결 관계를 대조한다. 원본 연결은 고정 baseline의 master 원본8행↔연결11행의 같은 설비 대응을 검증한 뒤 고정 rowId로 묶는다. 추가 행 관계는 이 내부 연결 값으로 유지하며 설비명이나 현재 행번호로 다시 찾지 않는다.
- 자동 행을 공개 rowMappings에 추가하지 않는다. rowMappings는 공개 ADD_ROW만 포함하고 affectedTableIds에는 연결 표/집계표도 포함한다. 자동 행의 생성·이동·삭제·파생 셀과 master 변경은 같은 성공 revision/transaction에 함께 저장한다. master 삭제 시 연결 행도 삭제하고 직접 연결 표 ADD/DELETE는 허용하지 않는다.
- 원본/기존 자동 행 이동·삭제에서는 `clientRowKey:null`을 유지하며 기존 연결 identity를 재작성하지 않는다. ADD→DELETE에서도 신규 두 identity와 삭제 상태를 함께 남긴다. 임의 파생 표·없는 master·중복 연결·cross-Draft 연결·연결만 살아남는 delta는 저장 오류다. 검토·제품 구현/검증 완료는 실제 편집기 stable work note에서 판정한다.

- 기존 직접 행 규칙과의 차이: BCRegasification ADD_ROW가 자동 생성하는 RegasAdjustment 연결 행(domain 규칙51)은 공개 ADD_ROW와1대1이 아니다. 위 derivedFromRowId/예약 key 보완이 이 경우의 내부 저장 계약이다. 실제 자동 행 생성·파일 처리/저장·재생의 구현 및 검증 상태는 실제 편집기 stable work note에서 추적한다. 이번 열 형식 확정을 전체 행 처리 지원 완료로 보지 않으며, 해당 행×열 조합은 그 보완 뒤 검증한다.
- 필수 구현 검증: 첫 월 추가·헤더/빈칸/명시0, 중간/끝 월 삭제 후 source→현재 열 매핑, 같은 연월 두 키, 삭제 후 같은 연월 새 키, ADD→DELETE의 tombstone/mapping, 신규 행×신규 열, 기존/신규 row의 열 이동·고정 header/control/파생 MAX 이동, 페이지/재시작 동일 결과, 동일 요청 재전송·실패 원자성·최종4파일.
- SQL 검증: reset transaction rollback·child-first/self-FK 순서·commit 뒤7table0 및 보존 대상 비교, CHECK2/상한/기존 제약, 신규 request/revision0/성공 version2·mapping/columns 배열, 빈 환경에서의 CHECK 원복. SQL의 JSON 배열 확인만으로 논리 복원 검증을 대신하지 않는다.
- ERD 영향은 CHECK1개 교체와 승인된7table 데이터 초기화뿐이다. 기존 JSON은 Draft별 순서 있는 변경 기록으로 쓰며 새 JSON 검색/index나 관계형 열 저장을 추가하지 않는다. 기존 오브젝트/단어를 재사용하고 이름/사전 추가는 없다. JPA/MyBatis/QueryDSL 모델 변경 없이 기존 master JDBC 경로를 수정하는 구현 입력이다.
- **형식·SQL 준비도: ready. 공통 v2 코드/격리 fixture·F-O-01 최종4파일 검증과 실제 표별 열 처리·DB 적용은 분리한다. 실제 표별 열 처리/실제 적용: 후속.** 환경별 schema/실제 데이터·백업/미정리 파일 상태는 SQL pre-check 결과로 확인한다. 실제 개발/운영 접속·적용·열 동작 성공은 이 문서 작성으로 확인한 것이 아니다.

- 부속 control SET은 공개 명령의 선택 표/control ID를 고정 발행의 sourceTableId/binding으로 검증한 뒤 값 소유 표의 `rowId:null,rowNumber,columnNumber` 셀 delta로 저장한다. 기존 표/행/열 schema 및 version2를 유지하고 control용 행 identity나 컬럼을 추가하지 않는다. 복원 시 마지막 명령값과 저장된 비수식 raw값을 대조하며 같은 값을 공유하는 표는 같은 소유 셀을 읽는다. LNG C1은 연간 표 본문/헤더에 중복하지 않고, 모드 SET만으로 합계나 다른 보존 영역을 재계산하지 않는다.

- LNG 기존/추가 연도 제목은 qry_json fields.policyStatus.SET_HEADER로 입력 준비도를 제공한다(누락은 INCOMPLETE). 공개 headerKey year는 현재 열 key로 찾은3행 고정 셀 delta를 재사용한다. 신규 필드는 기존 연도와 동일한 SET_HEADER 준비도를 가지며 물리 schema/version2 변경은 없다. B/C 지역·국가명은 원본 SUMMARY30 입력 범위를 따른다. B4/B109의 TOTAL, C5/10/15/25/27/35/43의 Sub-Total은 보존하고 다른 지역명 수정만으로 합계를 재계산하지 않는다.

- TRADE MidstreamCapacityInput의 PNG Infrastructure(SUMMARY33)는 B/D/G와 기존 J:Q·새 연도 입력만 제공하며 본문은11행부터다. 4..10행 및 외부 E:F/H:I와 A R:S는 domain33/34의 원본 수식/저장값 보존 대상이다. C의 R&S 연결식은 보존값에 의존하므로 기존/예약 C식·cache는 유지하고 예약 밖 새 행에는 상대 C식과 서식을 생성해 보존 R/S 또는 확인된 빈 셀로 계산한다. 외부 신규 조회값을 추정하거나 링크를 갱신하지 않는다. 새 연도는 다른 표/Helper를 이동하지 않는 비연속 열에 만들고 header3/fields/scopes로 제공한다.

<a id="incremental-upload-storage-20261019"></a>

### 6.10 직접 업로드 초안 저장 확장 — 2026-10-19

§6.12 도입 후에도 UPLOAD는 `base_id IS NULL`이며 이 절의 역할별 파일 baseline/전체 행 저장을 유지한다. 공유 baseline에 자동 편입하지 않는다.

직접 업로드의 행·열 편집은 기존 초안과 같은 정책/공통 API를 사용한다(사용자 후속 확인; domain §5.6·편집 API §3). 아래 UPLOAD 분기는 원본 Case 없는 부분 초안의 생성·파일 기준점/소유 저장을 위한 내부 구분이며 별도 편집 저장 체계를 뜻하지 않는다. xlsm/xlsx·원본 형식 유지·파일당50MiB는 입력 제공 명세 §3.3의 기존 확정을 유지하며 이번 확인으로 SQL/스키마를 추가 변경하지 않는다.

이 절은 §6.3의 **기존 Case 기반 경로를 유지**하면서 [domain 규칙67·§5.6](mi-model-input-template-catalog-domain-model-design.md#incremental-upload-lifecycle-storage-20261019)을 구현할 물리 확장이다. API는 [입력 제공 명세 §3.3](../../feature-specs/mi-fundamental-model-input-provision-feature-spec.md#incremental-input-upload-api-20261019)이 소유한다. 수동 증분 SQL은 [model-input-incremental-upload-ddl-draft.sql](../../model-input-incremental-upload-ddl-draft.sql)이다. 이 절/SQL은 설계 초안이며 실제 DB 적용·실행 검증이나 기존 코드의 지원 완료를 뜻하지 않는다. §6.3~6.9에서 원본 Case·고정4파일·CHANGE 성공만 전제한 문구는 직접 업로드에 한해 아래 확장을 따른다.

#### 6.10.1 변경 분류·테이블 책임

**신규2개 + 기존4개 물리 변경 + 성공 revision payload 확장**이다. 별도 편집 이력 체계를 만들지 않고 기존 요청·revision·행/열 복원을 재사용한다. 가짜 Case, TEMP/0 공통 파일 등록, Case ID 대신 UUID를 기존 정수 관계 번호에 넣는 방식을 사용하지 않는다.

| 테이블 | 변경·소유 데이터 |
| --- | --- |
| `tmd_mdl_inptdrftm` | `src_spt_cd VARCHAR(20) ascii_bin NOT NULL DEFAULT 'CASE'` 추가. CASE는 기존 `org_case_id/org_rvsn_val` 필수, UPLOAD는 둘 다 NULL CHECK. template/owner/revision/state는 공통 사용. `org_case_id`와 `org_rvsn_val`만 NULL 허용으로 확장하며 최종 Case는 기존 fnsv_case_id 사용 |
| `tmd_mdl_inptdrftprcsl` | CHANGE/FINALIZE에 UPLOAD_CREATE/UPLOAD_FILE 추가. 소유 회사/사용자 NOT NULL 추가(기존 요청은 Draft에서 backfill). 최초 요청은 base_rvsn_no=NULL, 성공 전 drft_id=NULL; 최초 성공 때만 생성된 drft_id 기록. 나머지 종류는 둘 다 필수. 상태·토큰·종료 결과·payload 한도는 기존 공통 구조 사용. 신규 업로드의 접수 후 기한은600초, 기존 이력과 CHANGE/FINALIZE는120초다. `ck09`의 호환 변경은 [업로드 기한 증분 DDL](../../model-input-upload-deadline-ddl-draft.sql)이 소유한다 |
| 신규 `tmd_mdl_inptupldactvm` | PK `(cust_cpny_no,usr_no)`. 현재 최초 접수 prcs_id 또는 성공한 drft_id 중 하나만 보유하고 둘 다 NULL이면 비어 있음. 사용자별 잠금 기준이며 과거 초안 목록이 아님. 최초 접수 이전에도 행을 안전하게 생성/잠금해 경쟁을 직렬화 |
| 신규 `tmd_mdl_inptupldfiled` | PK `upld_file_id UUID`. private 원본 객체의 서버 참조(최대16KiB JSON), 소유 회사/사용자, 원본 파일명300·확장자20·MIME100, 실제 수신 크기/내용 SHA-256, 요청·검증 양식·역할·성공 Draft/revision. 상태 PREPARED/RECEIVED/COMMITTED/CLEANUP_PENDING/CLEANED. 파일 바이트는 DB에 넣지 않음 |
| `tmd_mdl_inptdrftfiled` | 기존 `(drft_id,mdl_file_cd)` PK 유지. file_no를 NULL 허용하고 upld_file_id UUID? 추가. 정확히 하나만 존재하는 XOR CHECK. CASE는 기존 tcm_filel, UPLOAD는 새 private 원본 참조 사용. 업로드 중/실패 후보는 여기에 넣지 않음 |
| `tmd_mdl_inptdrftrowm` | `file_rvsn_no BIGINT UNSIGNED NOT NULL DEFAULT 0` 추가. 해당 역할 파일의 기준 revision이며 행 위치 이력의 시작 번호와 구별. 원본 행 UK를 `(drft_id,inpt_tbl_id,file_rvsn_no,org_row_no)`로 확장해 교체 전후 같은 Excel 행번호에 서로 다른 rowId를 보장 |
| `tmd_mdl_inptdrftrvsnl` | 물리 컬럼/키 변경 없음. n>0의 성공 요청은 CHANGE 또는 UPLOAD_FILE. 업로드 성공 delta의 v2 형식은 아래에서 정의. 최초 UPLOAD_CREATE는 기존 revision0 규칙대로 prcs_id/delta=NULL |

필수/NULL 타입·감사 DATETIME(0)/처리 DATETIME(6)·master 경계·RESTRICT FK는 §6.3/§6.8을 재사용한다. src_spt_cd는 BE 내부 분류이며 FE의 sourceKey를 새 고정 문자열로 바꾸지 않는다. org_rvsn_val=NULL을 공개 API의 임의 sourceVersion 값으로 채우지 않는다. 업로드의 조회/완료 DTO는 API 정본에서 별도 완결한다.

#### 6.10.2 키·제약·조회와 정규화

- Draft: 기존 Case별 활성 UK 유지. 추가 generated `actv_upld_usr_no=IF(src_spt_cd='UPLOAD' AND drft_stat_cd='DRAFT' AND del_yn='N',usr_no,NULL)`와 `(cust_cpny_no,actv_upld_usr_no)` UK로 직접 업로드 활성 Draft도1건을 보장한다. 최초 검증 전에는 Draft가 없으므로 이 UK만으로 충분하지 않으며 위 단일 작성 행을 함께 사용한다.
- 요청: 기존 `(drft_id,req_id)`/활성 drft_id UK는 후속 업로드·편집·최종저장을 함께 묶는다. 최초 전용 generated `crt_req_id`와 `(cust_cpny_no,usr_no,crt_req_id)` UK를 추가한다. 최초 requestId 범위는 인증된 회사/사용자이며 sourceKey/파일/metadata는 동일 내용 비교 대상이다. 후속은 기존 Draft/requestId 범위다. 최초 성공 뒤에도 같은 초기 키/작업을 유지한다.
- 요청/단일 작성의 owner 복합 FK는 같은 회사·사용자의 Draft/요청만 참조한다. private 파일의 성공 `(drft_id,prcs_id)` 및 `(drft_id,file_rvsn_no)` FK, 현재 파일의 `(drft_id,upld_file_id)` FK를 사용한다. 역할·고정 양식·성공 상태·현재 파일과 해당 baseline 소속 등은 DB FK만으로 표현되지 않으므로 짧은 확정 transaction과 검증 SQL로 확인한다.
- 2026-10-20 최초 다중 업로드 사용자 승인: 최초 요청당 파일1~4개, 후속 요청당 하나다. 기존 널 허용 `prcs_id` UK는 일반 `prcs_id` index로 대체한다. 성공 역할/revision당 UK와 교체 전 참조는 유지한다. 증분 적용/검증/롤백은 [model-input-multi-upload-ddl-draft.sql](../../model-input-multi-upload-ddl-draft.sql)이 소유하며 기존 설치와 신규 설치 모두 기본 업로드 DDL 다음에 적용한다. 정리 index `(prcs_stat_cd,mod_dtm)`는 유지한다.
- 1NF/2NF/3NF 검토: 역할은 배열 컬럼 대신 현재 역할 row와 파일별 baseline으로 분리했다. 단일 작성은 owner당 한 row다. JSON은 기존 원자 요청/결과 및 provider 저장 참조에만 사용한다. 요청/파일의 owner와 고정 양식은 독립 접수·권한 확인/파일 검증 시점의 증거이며 복합 FK/확정 검증으로 일치시킨다. 전체 workbook·현재 readiness를 중복 저장하지 않는다.

#### 6.10.3 접수·성공·교체/복원 경계

1. **수신 준비:** 서버 UUID로 생성한 private 객체 참조를 PREPARED로 먼저 기록한 뒤 저장한다. 아직 operation/초안/Case가 아니며 조회·다운로드에 공개하지 않는다. 수신 완료 바이트의 크기·SHA-256을 기록하고 RECEIVED로 바꾼다. 요청/파일 기본 검증 실패·중복 재전송의 새 사본은 현재 파일에 연결하지 않고 별도 정리한다. 실제 수신 파일은 `0<size<=52428800`. 새 digest는 업로드 동일성 판별용이며 기존 tcm_filel.file_hash_val의 의미를 바꾸지 않는다.
2. **접수:** 현재 요청의 재전송 여부를 먼저 대조하고, 단일 작성 행 잠금 뒤 같은 키를 다시 조회한 후에만 busy/revision을 판정한다. UK 경쟁이 발생하면 해당 transaction을 rollback한 뒤 같은 잠금/원 요청 재조회로 동일 내용의 원래 결과를 반환한다. 새 최초 요청은 단일 작성 행이 비어 있는 경우만 UPLOAD_CREATE/파일 요청 연결/최초 진행 포인터를 한 transaction에 기록한다. 후속은 단일 작성→Draft→요청 순으로 잠가 현재 revision/소유/교체 확인/진행 없음 확인 후 UPLOAD_FILE과 공통 진행 포인터를 확정한다. **직접 업로드의 CHANGE/FINALIZE·claim/완료·timeout/재시작 복구·취소도 같은 잠금 순서를 사용**하며 Draft/요청을 먼저 잠근 뒤 단일 작성 행을 얻지 않는다. 원본 Case 경로의 기존 순서는 유지한다. 파일 수신 중 DB 잠금을 유지하지 않는다. crt_dtm/expr_dtm은 파일 수신 후 이 접수의 DB 시각으로 정하고 업로드 유형에600초를 적용한다. 기존120초 이력과 다른 작업 유형은 [업로드 기한 증분 DDL](../../model-input-upload-deadline-ddl-draft.sql)의 `ck09`에서 함께 허용한다.
3. **검증:** 첫 요청의 worker가 실제 검증을 시작할 때 선택한 ACTIVE tmplt_id를 파일 검증 기록에 한 번 고정한다. 이후 ACTIVE 변경으로 다시 선택하지 않는다. 후속은 Draft의 tmplt_id를 그대로 쓴다. 파일명 역할과 실제 형식/MIME·구조를 대조하고 필요 행/표를 파싱한다. private 파일이 RECEIVED라는 이유로 유효 역할에 포함하지 않는다.
4. **첫 성공:** 단일 작성→요청 잠금 아래 토큰/기한/현재 최초 포인터·소유/정책를 대조하고 새 UPLOAD Draft·revision0·해당 파일의 행ID/위치·현재 역할 연결, 파일 COMMITTED/baseline0, 작업 SUCCEEDED/결과, 단일 작성의 최초 포인터→Draft 전환을 한 master transaction에 commit한다. 최초 검증 실패는 Draft/Case가 없고 실패 기록/자기 후보 정리만 남기며 최초 슬롯을 해제한다. 기존 성공 replay는 이후 상태를 초기화하지 않는다.
5. **추가/교체 성공:** 같은 잠금 순서 아래 현재 baseRevision/진행 포인터/토큰/기한을 다시 대조한다. revision=n+1·baseline 파일 COMMITTED·현재 역할 참조·해당 역할의 행/구조 초기화·성공 결과와 포인터 해제를 함께 확정한다. 교체 전 파일 객체/성공 이력은 보존하며 이전 역할의 활성 행 위치를 새 revision에서 tombstone으로 닫고 새 파일 기준 rowId를 만든다. 다른 역할의 행/열/값/계산은 바꾸지 않는다. 실패는 기존 참조/revision/편집을 그대로 둔다.
6. **조회/편집/최종 복원:** 역할별로 목표 revision 이하의 마지막 성공 파일 baseline을 선택하고, 그 역할에 속한 **baseline 이후** 성공 CHANGE의 승인 결과만 복원한다. 예전 파일의 셀·행·열·구조 delta를 새 파일에 다시 적용하지 않는다. 다른 역할의 기존 delta는 유지한다. snapshot/cache도 같은 기준점으로 판별하며 실패·진행 파일을 사용하지 않는다. 기존 Case 경로는 baseline0/기존 고정4파일을 유지한다.
7. **취소·최종저장:** 진행 없음/확인된 취소는 같은 단일 작성→Draft 잠금에서 DISCARDED와 단일 작성 포인터 해제를 함께 확정한다. 최종저장은 기존4파일·Case/file metadata/고정 template/FINALIZED/요청 성공 확정 transaction에 슬롯 해제를 포함한다. 이때만 실제 Case 및 tcm_filel4건을 만든다. 취소 A는 새 최초 B의 실패/timeout으로 복구하지 않는다. 종료/실패 후 동일 요청 결과는 불변이며 새 작성을 가리키는 포인터를 과거 실패 처리로 해제하지 않는다.

UPLOAD_FILE의 v2 dlta_json은 닫힌 형식 `{policyTemplateId,uploadFileId,kind}`이며 세 key가 필수다. file baseline·행 identity/위치는 관계형 성공 기록과 대조한다. CHANGE의 v2 형식은 §6.9를 그대로 사용한다. 읽기/최종저장 reader는 연결 요청 종류로 분기하며 업로드 delta를 CHANGE로 해석하거나 무시하지 않는다. UPLOAD_CREATE/UPLOAD_FILE의 req_json은 파일 바이트/경로 대신 정규화 metadata와 실제 파일 크기·내용 digest/의미 있는 파일명을 포함하고 기존1MiB 한도를 유지한다. 2026-10-20 다중 최초 요청은 `files` 배열을 파일명/MIME/크기/hash 순으로 정렬해 저장한다. 단일 요청은 기존 `file` object를 유지해 과거 재전송과 호환한다. 최초 배치의 모든 성공 파일과 원본 행은 같은 revision0에 연결되며 UPLOAD_FILE delta 형식은 변경하지 않는다. 전체 후보의 연결/성공 확정은 같은 transaction, 실패 정리는 요청의 모든 후보를 대상으로 한다. 랜덤 수신 UUID·object key·multipart boundary를 동일성 비교에 넣지 않는다. 파일 형식/크기/개인 경로를 성공 응답으로 무조건 노출하지 않으며 API 필드 추가는 해당 정본에서 결정한다.

#### 6.10.4 코드 영향·보관·배포/검증

- **발행된 검증 정의:** 기존 `tmd_mdl_inpttbld.qry_json`에 `uploadValidation:{schemaVersion:1,headerCells:[{rowNumber,columnNumber,value}]}`를 등록한다. 좌표는1기반이고 value는 승인된 고정 제목/헤더 문자열 또는 숫자다. 표마다 headerRows가 있으면 최소 하나의 고정 헤더 검사가 필요하다. 기간·사용자 입력값을 정적 헤더로 등록하지 않는다. 누락은 POLICY_INCOMPLETE로 차단한다. 새 물리 컬럼은 없으며 이미 발행된 양식을 제자리 수정하지 않고 기존 DRAFT 등록/새 버전 발행 절차를 따른다. 실제 라벨·대상 template을 확인하지 않은 범용 DML/자동 활성화는 제공하지 않는다.
- **수신 종료 증거:** PREPARED 객체 기록 전에 값 없는 `.upload-{UUID}.marker`를 소유 임시 디렉터리에0600으로 fsync하고 디렉터리도 동기화한다. 살아 있는 수신 JVM은 `.lock` FileLock을 유지한다. 다음 시작은 같은 root에서 이전 lock을 획득한 경우만 종료 증거를 새 소유 디렉터리에 복사·동기화한 뒤 이전 디렉터리를 지운다. 증거별 DB transaction에서 `prcs_id IS NULL`인 PREPARED/RECEIVED만 CLEANUP_PENDING으로 바꾸고 DB 결과 확인 뒤 marker를 지운다. 접수된 작업/성공 원본은 이 정리 대상이 아니다. 정리 도중 다시 종료돼도 marker를 보존한다. 시각 경과만으로 writer 종료를 추정하지 않는다. 디렉터리 증거가 사라진 호스트/Pod 유실은 자동 정리 완료로 판단하지 않는다.
- **활성화 순서:** 업로드 기본값은 `mi.model-input.upload.enabled=false`다. SQL 적용과 검증 규칙을 포함한 새 양식 발행, 인증된 회사의 private storage 및 수신/worker 종료 증거 디렉터리를 확인한 뒤 `mi.model-input.upload.source-key`와 `mi.model-input.upload.model-code`를 설정한다. 값/행열 편집 및 최종저장은 기존 `mi.model-input.change-worker.enabled`·`mi.model-input.finalization.enabled` 설정/실제 지원 범위를 따른다. 개발·시험의 기존 durable worker에 업로드 claim/복구를 연결했고, 고객 배포의 웹 Pod 내 실행 방식은 domain §5.5.3의 별도 운영 결정이다. 기존 전역 multipart 상한은 유지하며 API 수신 스트림에서 파일당50MiB를 따로 확인한다. 업로드 package는 압축 해제 총량512MiB·기존 entry/CRC/XML 제한도 적용한다.
- 기존 직접 SQL Repository/record의 primitive caseId, tcm_filel 전용 FileRef, 모든 요청의 Draft 필수, 서비스 source(caseId)/고정4파일 및 worker의 Draft→actor 복원을 분기해야 한다. 최초 worker는 요청의 검증된 owner/접수 role을 복원한다. JPA/QueryDSL 생성 모델은 이번 문서 작업에서 변경하지 않으며 기존 Case entity/FK/tenant router를 개편하지 않는다.
- 미접수/실패 후보는 이전 writer/전송 종료·DB 실제 결과와 현재/성공 baseline 미참조를 확인한 뒤 자기 private 객체만 정리한다. PREPARED 기록으로 재시작 뒤 미접수 사본도 추적한다. COMMITTED 및 교체/취소한 성공 원본·요청/revision은 기존 자동 TTL 없음/이력 보존을 따른다. 목록/선택 재개·장기 자동 만료/사용자 복구 기능을 추가하지 않는다. 로컬 평문 정리는 domain 규칙63을 유지한다.
- **Pre-check / apply / backfill:** 기존 감사/요청/최종준비/열v2 적용 상태와 실제 제약 이름을 확인하고 writer/worker를 중지한다. 기존 CASE 데이터와 owner를 보존하는 nullable 확장·owner backfill 후 제약 추가→신규2개→현재파일/행 baseline 확장 순서다. 기존 데이터를 초기화하지 않는다. 이전 §6.9의 별도 초기화 SQL을 이 적용에 다시 실행하지 않는다.
- **호환/rollback:** 기존 앱은 UPLOAD의 NULL Case/새 요청을 처리하지 못한다. 호환 reader/worker와 SQL을 준비한 뒤 새 진입을 열고, 구버전 혼재 writer/worker를 두지 않는다. 롤백은 신규 접수 중지·진행/commit 결과 확인·데이터/객체 보존 후 호환 버전으로 수행한다. NULL을 가짜 Case로 backfill하거나 새 table DROP/성공 파일 삭제로 되돌리지 않는다. DDL 암묵 commit으로 구간별 실제 정의를 확인한다.
- **Verify:** SQL의 정의/소속/현재 포인터·baseline/revision 검증 결과0과 기존 Case·파일·행ID 보존을 확인한다. 별도 fixture로 최초 서로 다른 requestId/탭의 경쟁, 성공/실패 replay, 동시 파일/편집/최종저장 차단, 실패 교체 보존, 교체 전 행ID/열 delta 재사용 차단·다른 역할 불변, 취소 후 B 실패, deadline/서버 재시작/늦은 확정과 형식별 최종저장을 검증한다. EXPLAIN/nullable UK·CHECK·FK의 실제 강제는 MySQL 실물 검증 대상이며 정적 검사로 통과 처리하지 않는다.
- **명명:** 기존 MDL/INPT/UPLD/ACTV/FILE/FILEX/DRFT/PRCS/REQ/SRC/SPT/CD/CNTT/HASH/RVSN/REF/JSON/CRT/MIME/SIZE 토큰을 재사용한다. 파일 metadata 이름은 기존 filex_nm/mime_type_nm과 맞춘다. 기존 tmm 접두사 제약명은 재명명하지 않고 신규 제약만 tmd 표준명을 사용한다. 사전 추가 없음. DB 세부 근거: [CHECK의 NULL 판정](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html), [복합 FK/타입 조건](https://dev.mysql.com/doc/refman/8.0/en/create-table-foreign-keys.html), [DDL 암묵 commit](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html).

#### 6.10.5 Open Questions / implementation readiness

- 사용자 업무 선택: 현재 저장 설계에서 추가로 필요한 미결 없음. 확정된 단일 작성·부분 편집·교체/취소·형식 및 작업 유형별 기한을 재질문하지 않는다.
- 기술 후속: 실제 SHOW CREATE와 SQL 제약명/타입 대조, MySQL 실행/경합 검증, 위 공통 reader/writer/worker의 새 분기와 API 조회/취소/완료 DTO 구체화, 환경별 private 저장소/정리 실행 연결이 남는다. 실제 DB를 확인하지 않고 DBA 적용 ready로 승인하지 않는다.
- **Implementation readiness: ready** — 저장 구조의 구현 입력. **DBA apply readiness: blocked by open questions** — 위 환경/실행 검증을 충족한 뒤 수동 적용한다. 전체 업로드 제품 기능/관련 API가 완료됐다는 판정이 아니다.

<a id="in-process-runner-state-proposal-20261020"></a>

### 6.11 A 웹 실행기 상태 저장 — 2026-10-20 사용자 승인

**상태: 사용자 방향 승인 / 실제 SQL 적용 없음.** 수명/인계 순서는 [domain §5.5.3](mi-model-input-template-catalog-domain-model-design.md#worker-execution-direction-20261019)이 소유한다. 이 절은 기존 구조 적합성 검토와 승인된 최소 물리 변경을 정의한다.

#### 기존 구조로 표현할 수 없는 이유

- `tmd_mdl_inptdrftprcsl.try_tokn/bgn_dtm/end_dtm`은 개별 요청의 처리 시도/결과다. 요청 claim 전·idle 중에는 소비자 시도를 표현하지 못하고, 요청 성공은 Java 처리 본문·finally·전송의 종료 증거가 아니다.
- `tmd_mdl_inptfnsvfiled`는 operation/token별 준비파일, `tmd_mdl_inptupldfiled`는 수신 파일, `tmd_mdl_inptupldactvm`은 회사/사용자별 작성 상태다. master queue 전체 소비자나 복구 승인의 소유 테이블이 아니다.
- 공통 작업 종류/CHECK를 변경해 가짜 요청을 만들거나 req/rslt/fail/strg_ref JSON에 소비자 상태를 넣지 않는다. 세션 잠금과 로컬 operation marker도 영속 소비자 상태의 대체가 아니다.

#### 최소 변경안 — 신규 table1개, 기존 table ALTER 없음

`tmd_mdl_inptexecm`을 같은 master schema에 두고 queue당 고정1행을 유지한다. 사용자 입력으로 queue/tenant를 선택하지 않는다. 물리 정의/수동 적용 SQL은 [model-input-web-runner-ddl-draft.sql](../../model-input-web-runner-ddl-draft.sql)이다.

| 컬럼 | 제안 타입·제약 | 책임 |
| --- | --- | --- |
| `exec_no` | `TINYINT UNSIGNED NOT NULL PRIMARY KEY`, CHECK `=1` | master 모델 입력 queue의 단일 제어 행 |
| `crt_usr_logn_id/crt_dtm/mod_usr_logn_id/mod_dtm/del_yn` | 기존 §6.8 이름·타입·감사 시간, CHECK `del_yn='N'` | 기존 감사 규칙 유지 |
| `exec_id` | `CHAR(36) ascii_bin NULL` | 현재 소비자/복구 명령의 시도 UUID. 최초 초기화 때만 NULL |
| `exec_stat_cd` | `VARCHAR(20) ascii_bin NOT NULL` | `ACTIVE`, `RECOVERING`, `STOPPED` |
| `bgn_dtm/end_dtm` | `DATETIME(6) NULL` | 해당 시도 시작/종료 확인 시각. 시간 경과로 종료를 추정하지 않음 |
| `prcs_id/try_tokn` | 각 `CHAR(36) ascii_bin NULL`, 둘 다 NULL 또는 둘 다 값 | 처리/정리 중인 요청과 예상 처리 token. 처리 본문·정리 완료 전 지우지 않음 |
| `aprv_exec_id` | `CHAR(36) ascii_bin NULL` | 마지막 수동 복구가 대상으로 삼은 이전 소비자 시도 |
| `aprv_usr_logn_id/aprv_dtm` | `VARCHAR(50) NULL` / `DATETIME(6) NULL` | 같은 승인 대상의 운영자 식별 기록·승인 시각 |

- 상태 CHECK: 최초 `STOPPED`는 exec_id/bgn_dtm/end_dtm과 작업 쌍이 NULL이다. 실행/복구 시도는 exec_id/bgn_dtm 필수·end_dtm NULL이고, 종료 확인된 `STOPPED`는 exec_id/bgn_dtm/end_dtm 필수·작업 쌍 NULL이다. 승인3필드는 모두 NULL 또는 모두 값이며 최초 초기화에서는 NULL이다.
- `prcs_id`에 기존 요청 PK로 RESTRICT FK와 지원 index를 둔다. token 일치는 같은 master transaction에서 대조한다. claim 전 예상 token 기록도 허용하므로 기존 요청의 nullable try_tokn에 새 UK/FK를 추가하지 않는다. 이전 소비자 UUID는 보존된 별도 행을 참조하는 값이 아니므로 자기참조 FK를 만들지 않는다.
- 단일 현재 상태/마지막 승인 기록이며 실행 이력 테이블로 확대하지 않는다. 배열·JSON·반복 그룹은 없고 각 값은 고정 queue 행의 현재 상태에 속한다. 별도 상태 index, TTL, 자동 초기화/reset, 기존 요청의 backfill은 없다.
- 명명은 기존 `tmd_mdl_inpt*` 계열과 사전의 MDL/INPT/EXEC/NO/ID/PRCS/TRY/TOKN/STAT/CD/BGN/END/APRV/USR/LOGN/DTM을 재사용한다. 사전 등록 추가는 없다.

#### 코드·DBA 영향과 승인 범위

- 새 직접 SQL Repository와 웹 실행기/복구 명령에서만 읽고 쓴다. 공개 API/DTO·기존 entity·QueryDSL projection·기존 worker Main/Supervisor/Compose 변경은 없다. 작업 연결은 기존 claim 전에 소비자 행을 잠그고 기록하며, 기존 회사/사용자→Draft→operation 잠금 순서 앞에 소비자 행을 둔다. 외부 IO 동안 row transaction을 유지하지 않는다.
- 조건부 갱신은 현재 exec_id/상태와 operation/token을 함께 대조한다. 수동 승인 수락 시 새 복구 exec_id로 바꾸는 commit 자체가 승인 소비다. 별도 재사용 가능한 승인 token이나 자동 복구 허가 플래그를 만들지 않는다. 이전 대상의 명령은 현재 exec_id와 달라 재사용할 수 없다.
- **Pre-check/apply/backfill:** A를 끈 상태에서 실제 MySQL 정의와 기존 요청/미정리 상태를 확인한다. 신규 table을 수동 생성하고 최초1행은 운영자가 초기화한다. 기존 이력으로 종료 사실을 추정해 자동 seed하지 않으며, 테이블/행 누락 시 앱은 소비를 보류한다. 이전 실행/전송의 종료를 확인할 수 없는 잔여는 최초 활성화를 보류한다. 기존 worker 종료는 사용자 담당이라는 domain 전제를 유지한다.
- **Verify/rollback:** 고정1행·CHECK/FK·기존 table 정의와 데이터 불변·120초 ck09 유지, 실제 두 연결의 세션 잠금/행 잠금 경쟁을 확인한다. rollback은 A 비활성화와 새 행/기존 이력 보존이며 DROP/reset을 사용하지 않는다. A가 시작된 뒤 예전 worker로 돌아가는 절차는 이 승인에 포함하지 않는다.
- 승인 후 증분 SQL에 pre-check/apply/초기화/verify/보존 rollback/DBA 순서를 작성한다. 실제 MySQL 실행계획/제약/잠금 검증은 아직 없다. 고정 PK 조회 외 새 대량 조회 index는 이번 상태 table에 필요하지 않다.

**Open Questions:** 실제 MySQL의 정의/제약/잠금 경쟁 수동 검증. `implementation readiness: ready` — table1개와 위 초기화/인계 방식은 사용자 승인됐으며 제품 구현/대역 검증을 진행한다. 실제 DB 적용/배포 승인을 뜻하지 않는다.

### 6.12 Case 초안 공유 기준 행 — 2026-10-21 설계

실행 DDL 초안은 [model-input-shared-baseline-ddl-draft.sql](../../model-input-shared-baseline-ddl-draft.sql)이며 수동 사전 확인/적용/검증/복구 구간을 제공한다. 실제 DB 적용·MySQL 검증은 미실행이다.

**정본 범위:** 신규 3개 table과 Draft의 nullable 참조, 가상 원본 identity/위치 해석, 수동 증분 적용·검증·복구의 구현 입력이다. 실행 SQL 초안은 위 파일을 따르며 실제 DB 적용은 별도다. 업무 소유·원본 동일성·생성 순서는 [domain §5.7](mi-model-input-template-catalog-domain-model-design.md#57-case-초안의-불변-초기-기준-행-공유--2026-10-21-설계), API는 편집 명세 §3을 따른다. §6.5 기존 생성 DDL과 §6.10 업로드 DDL을 다시 실행하거나 덮어쓰지 않는다.

#### 6.12.1 저장 형식·컬럼·타입

공통 UUID/Case/template/table/회사/file 타입·감사 필드는 §6.3.1·§6.8을 재사용한다. 신규 table은 master InnoDB, PK → 감사 시스템 컬럼 → 일반 컬럼 순서다. UUID/코드/sourceVersion의 비교는 ascii_bin, 객체 참조는 utf8mb4_bin으로 정확 비교한다. `?`만 NULL이며 그 외 NOT NULL이다. 성공 baseline의 `del_yn`은 `N`만 허용하고 child 독립 삭제나 CASCADE를 두지 않는다.

| table / PK | 일반 컬럼 | 책임·CHECK |
| --- | --- | --- |
| `tmd_mdl_inptbasem` / `base_id UUID` | `cust_cpny_no MEDIUMINT UNSIGNED`, `org_case_id INT UNSIGNED`, `tmplt_id INT UNSIGNED`, `org_rvsn_val VARCHAR(68) ascii_bin`, `base_ver_no SMALLINT UNSIGNED`, `row_cnt BIGINT UNSIGNED` | committed 완성 기준만 존재. `base_ver_no>=1`, sourceVersion 정규식은 기존 sv1 형식, row_cnt>=0. 사용자 소유·상태/현재 revision 컬럼 없음 |
| `tmd_mdl_inptbasefiled` / `(base_id,mdl_file_cd)` | `file_no BIGINT UNSIGNED`, `obj_ref_val LONGTEXT utf8mb4_bin` | 역할은 기존4종 CHECK, 객체 참조는 공백 불가·최대16KiB UTF-8. 서버가 기존 FileRef.location을 정확 보관/대조. URI를 해석/정규화하거나 사용자가 제공한 경로로 치환하지 않음 |
| `tmd_mdl_inptbaserowd` / `(base_id,inpt_tbl_id,org_row_no)` | PK 외 감사 필드만 | 원본 논리 행 한 건. `org_row_no BETWEEN 1 AND 1048576`, `inpt_tbl_id INT UNSIGNED`. baseline rowId/위치 이력/셀값 없음 |
| 기존 `tmd_mdl_inptdrftm` 추가 | `base_id UUID NULL DEFAULT NULL` | NULL은 기존 full-row 저장. non-NULL은 공유 Case 저장. CHECK `base_id IS NULL OR src_spt_cd='CASE'`. 생성 후 참조 불변. 직접 업로드는 항상 NULL |

`base_ver_no=1`은 이번 공유 저장/행 판정/ID 파생 규칙 묶음이다. 현재 reader의 일반 identityColumns 판정과 Regas 연결 원본 예외를 그대로 동결한다. 판정/ID 규칙 변경 시 새 버전을 쓰고 기존 버전 reader를 유지한다. 성공 delta의 `json_rvsn_no=2`와는 다른 축이며 요청/결과/CHANGE payload를 v3로 바꾸지 않는다. baseline 버전이 지원되지 않으면 `POLICY_UNSUPPORTED`로 중단하고 행을 추측하지 않는다.

base header의 templateVersion/catalogRevision은 기존 tmplt_id 및 불변 발행 참조·sourceVersion에 결합되므로 독립 버전/해석 JSON 사본을 추가하지 않는다. 현재 조회 가능한 catalog를 source()와 같은 기준으로 검증해야 하며 동일 발행 내용을 수정하는 운영을 허용하지 않는다. row_cnt는 발행 완전성 검증을 위한 의도적 집계 중복이다.

#### 6.12.2 키·FK·인덱스와 DB 밖 불변식

새 오브젝트에는 아래 제약명을 사용한다. 기존 Draft 제약명은 §6.6의 rename 전 이름이 남을 수 있으므로 `SHOW CREATE TABLE`로 확인하고 삭제/재번호하지 않는다. 아래 새 번호 10은 이 slice용 예약 번호이며 실제 충돌이면 실행 DDL 작성 전에 이 정본부터 조정한다.

| 대상 | 물리 제약/인덱스 |
| --- | --- |
| base master | `tmd_mdl_inptbasem_ux01(cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val,base_ver_no)` — 재사용 키. 모든 열 non-null. `..._ux02(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)` — Draft 복합 참조 대상 |
| base master FK | `..._fk01(cust_cpny_no) → tmd_custcpnym(cust_cpny_no)`, `..._fk02(org_case_id) → tmd_mdl_scnrcasem(case_id)`, `..._fk03(tmplt_id) → tmd_mdl_inpttmpltm(tmplt_id)`. `_ux01`의 회사 선두 index 재사용, `_ix01(org_case_id)`·`_ix02(tmplt_id)`로 나머지 FK 지원 |
| base files | `tmd_mdl_inptbasefiled_ux01(base_id,file_no)`로 역할 간 같은 파일 중복 방지. `_fk01(base_id) → base master`, `_fk02(file_no) → tcm_filel(file_no)`, `_ix01(file_no)` |
| base rows | `tmd_mdl_inptbaserowd_fk01(base_id) → base master`, `_fk02(inpt_tbl_id) → tmd_mdl_inpttbld(inpt_tbl_id)`, `_ix01(inpt_tbl_id)`. PK가 선택 baseline/표/원본 행 범위 조회를 덮는다 |
| Draft 참조 | `tmd_mdl_inptdrftm_ix10(base_id,cust_cpny_no,org_case_id,tmplt_id,org_rvsn_val)`와 `..._fk10` 동일 열 → base master `_ux02`. `..._ck10`은 CASE 제한. NULL인 기존/UPLOAD row는 FK를 건너뛰며 기존 출처 필수성 CHECK는 유지 |

CHECK 이름은 각 신규 table의 `_ck01`부터 표의 조건 순서로 배정한다. 모든 FK는 RESTRICT/NO ACTION이며 기존 Case/파일/catalog 참조 삭제를 허용하지 않는다. soft delete는 FK가 막지 못하므로 source 접근 검사를 생략하지 않는다.

DB FK만으로 확인할 수 없는 것은 같은 master transaction의 생성/조회 검증과 후속 DBA 검증 질의에 명시한다.

- baseline의 파일 역할은 정확히4개, 현재 확인한 Case의 file_no/객체 참조와 일치한다. Draft의 고정 파일4건도 baseline manifest와 일치한다.
- 모든 기준 행의 table은 header의 tmplt_id에 속하며 현재 정의의 bodyRanges 안이고 최초 스캔의 실제 논리 행 집합과 같다. `row_cnt=COUNT(base rows)`이며 중복/누락 없이 한 번에 commit한다. 행0건은 실제로 빈 경우 허용하되 부분 생성의 빈 header로 해석하지 않는다.
- base에 연결된 Draft의 물리 원본 행은 baseline에 반드시 존재하고 ID 파생 결과와 같으며 `file_rvsn_no=0`이다. 추가 행은 기존 성공 operation/clientRowKey에 속한다. 두 집합이 겹치지 않는다.
- 논리 활성 위치에는 가상 원본과 실제 저장 행을 **모두** 포함한다. 기존 `rowh` generated UK는 저장된 행 사이만 검사하므로 전체 논리 상태의 중복 방지는 합성 후 검증 + Draft 잠금이 담당한다. 물리 UK만으로 충분하다고 간주하지 않는다.

정규화: files/rows는 원자 항목이며 복합 PK 전체에 종속한다. header에는 독립적인 사용자/현재 상태를 중복하지 않는다. role manifest는 파일 원본 metadata를 변경하는 사본이 아니라 당시 고정 참조의 증거다. row_cnt 집계와 복합 FK용 parent unique는 완전성/회사·원본 경계 강제 목적이다. 별도 상태 index/현재값 캐시·파티션은 추가하지 않는다.

#### 6.12.3 rowId와 필요한 원본 행만 저장하는 규칙

공유 Draft 원본 rowId는 **UUID v5**로 생성한다. namespace는 해당 `drft_id`의 UUID 16바이트(network byte order), name은 UTF-8 ASCII 문자열 `mi-input-row/v1/{base_id}/{inpt_tbl_id}/{org_row_no}`다. UUID는 소문자 표준36자, 정수는 선행0 없는 양의10진수다. namespace+name의 SHA-1에서 앞16바이트를 취하고 version5/variant RFC 비트를 설정하는 UUID v5 알고리즘을 고정한다. Java `UUID.nameUUIDFromBytes`는 v3이므로 그대로 호출해서 대체하지 않는다. 구현 시 독립 기준값과 고정 벡터로 검증한다. 이 해시는 파일 내용 증명/권한 토큰 용도가 아니다.

- 기존 full-row Draft의 저장된 rowId는 절대 재계산하지 않는다. 공유 Draft의 원본은 v5, 신규 추가 행은 기존 서버 v4 방식이다. 이동·삭제·다시 조회해도 원본 이름 입력은 최초 좌표로 고정한다. 삭제된 원본 슬롯을 새 행이 채우면 새 v4 ID를 사용한다.
- rowId만으로 기준 행/권한을 조회하지 않는다. 서버가 소유 확인한 Draft/표의 baseline에서 ID를 파생·대조한다. 클라이언트가 맞는 모양의 UUID를 보내도 멤버십이 없으면 기존 검증 오류다. 충돌/중복 ID가 검출되면 전체 요청을 실패시키며 임의 재발급·덮어쓰기를 하지 않는다.
- **미변경 원본:** 논리 identity `(table,id,original,null,null,baseline=0)`와 위치 `(table,id,begin=0,end=NULL,row=original)`만 합성하고 물리 rowm/rowh에는 넣지 않는다.
- **원본의 첫 row delta가 revision r(같은 위치 재기록 포함):** 같은 성공 transaction에서 기존 rowm 형식으로 원본 identity를 넣고 rowh `[0,r)`의 원본 위치와 `[r,∞)`의 새 위치 또는 NULL tombstone을 저장한다. 원본인데 물리 row가 없다는 이유로 ADD로 처리하지 않는다. 값만 바뀐 이전 revision은 이 원본 위치 구간 안에 있으므로 과거 값은 delta로 복원한다.
- **후속 위치 변경:** 기존 current 구간을 r에서 닫고 새 구간을 연다. 되돌아온 원본 위치도 기존 identity를 유지하고 이미 기록한 history를 삭제해 다시 가상 상태로 만들지 않는다.
- **추가 행:** 기존 rowm의 prcs_id/clientRowKey와 성공 revision 시작 위치를 사용한다. 같은 요청 추가 후 삭제도 성공 ID와 NULL 위치를 보관한다. 원본 자리 이동·교환은 영향받는 모든 이전 current를 먼저 닫고 새 위치를 넣어 generated UK를 만족시킨다. 처음 저장할 원본의 과거 `[0,r)`는 닫힌 구간으로 직접 넣는다.
- 기존 rowm/rowh PK/UK/FK는 유지한다. 새 baseline과 rowm을 잇는 nullable 컬럼은 row마다 추가하지 않는다. Draft 참조를 따라 table/original 멤버십을 서버에서 확인한다.

#### 6.12.4 합성 조회와 성공 확정의 정확한 경계

구현은 물리 행 읽기와 논리 행 읽기를 명시적으로 구분한다. `rowIdentities/rowPositions/rows/initialPageRows`의 소비자를 확인하여 공유 경로에서는 합성 결과를 사용하게 한다. 단일 책임의 repository 내부 함수/기존 record 확장으로 충분하며 전용 cache/새 ORM 계층은 요구하지 않는다.

1. Draft의 base_id가 NULL이면 기존 SQL/검증을 그대로 사용한다. non-NULL이면 먼저 소유/source/manifest/base 버전을 검증한다. 그 다음 baseline B, 이 Draft의 물리 identities P 및 위치 H를 같은 DB snapshot/잠금 아래 읽는다.
2. 논리 identities는 `derive(B)`와 P의 추가 행을 합친다. P의 원본 행은 B에 존재하는지·ID가 같은지 검증하되 두 번 합치지 않는다. baseline 원본마다 P가 있으면 그 H 전체를, 없으면 가상 `[0,∞)`를 사용한다. P 존재에 대응하는 H 누락·불연속·겹침·잘못된 row/table/revision은 오류다. 물리 원본이 미래 revision에서 처음 기록돼도 `[0,r)`가 있어 과거 복원이 가능하다.
3. 성공 revision0..n 및 operation 결과를 기존 `InputDraftHistory.restore`로 재생한다. 재생 identity/위치 전체와 합성 입력을 양방향 대조한다. 가상 위치를 성공 delta에서 새로 만들어낸 결과와만 비교하여 저장 오류를 은폐하지 않는다. baseline count/범위 및 물리 원본 대응을 먼저 확인한다.
4. revision0의 구조/페이지 빠른 경로는 baseline 전체 count·범위·4파일과 이 Draft의 revision0/물리 행 없음 확인을 DB에서 수행하고 필요한 페이지 범위만 JVM으로 가져온다. count 검사는 O(N)일 수 있다. B 전체를 매번 JVM 로드하거나 생성 시 2N INSERT하는 방식으로 돌아가지 않는다. NULL base_id 경로의 `validateInitialRows`는 유지한다.
5. n>0의 첫 구현은 전체 B/P/H와 성공 delta를 합성해 현재와 같은 페이지 렌더 입력을 만든다. 작은 페이지 SQL만으로 이동/삭제·표 확장/열 변화가 있는 현재 위치를 추정하지 않는다. 내부 과거 r은 `InputDraftEditingBasis.history(r)`의 prefix/구간 절단을 유지한다.
6. `ModelInputDraftService.validateChange`의 bindings, `prepareWorkerChange`, `restore`, `InputDraftFinalizationRepository.prepareClaimed → verify` 및 `InputDraftEditingBasis`가 같은 논리 합성을 사용한다. at-revision Draft를 복제하는 코드에서도 base 참조/버전을 잃지 않는다. UPLOAD 교체의 직접 물리 읽기는 base_id NULL 불변식 아래 기존 의미를 유지한다.
7. `completeChange`는 row delta에 처음 기록하는 원본/기존 물리 원본/신규 추가를 구별하고 §6.12.3을 수행한다. `added=false`이면 반드시 기존 current UPDATE1건이라는 현재 전제에 첫 원본 기록 분기를 추가한다. late token/기한 실패 또는 정합성 오류면 최초 rowm·과거 구간까지 rollback한다. 성공 revision/작업 결과/포인터 갱신은 기존과 동일하다.

#### 6.12.5 DBA 적용·검증·중단/복구 순서

이 절은 **후속 실행 DDL의 필수 작성 항목과 순서**이며 지금 SQL을 실행하라는 지시가 아니다. DBA가 실제 환경과 승인된 실행 패키지를 확인한 뒤 수동 적용한다. 기존 v2 초기화/reset SQL을 이 변경의 migration으로 사용하지 않는다.

| 단계 / 담당 | 구체 작업·기대 결과·중단 조건 |
| --- | --- |
| 1. pre-check / BE·DBA | master DB/MySQL 버전·InnoDB·CHECK 지원, 기존 catalog/Draft/UPLOAD 스키마와 `SHOW CREATE TABLE`, 타입/unsigned/collation, `_ix10/_fk10/_ck10` 충돌 여부, FK parent index·길이를 대조. 신규 이름 없음 또는 동일 정의임을 확인. 백업/복원 가능성과 ALTER metadata lock·transaction 자원 한도를 확인. 불일치는 적용 중단 |
| 2. apply / DBA | base master→files→rows 생성, Draft nullable base_id 추가 후 index/CHECK/FK 추가. 기존 PK/UK/row history/파일 값 변경 없음. 새 구조는 아직 앱에서 사용하지 않음. DDL implicit commit을 고려해 문장별 완료 상태를 확인하고 부분 적용 뒤 전체 스크립트를 맹목 재실행하지 않음 |
| 3. backfill / 없음 | 기존 Draft는 모두 base_id NULL로 유지. 기존 편집 이력을 원본으로 승격하거나 최초 원본/rowId를 다시 만들지 않음. 기준이 같은 옛 Draft가 있어도 첫 공유 baseline은 고정 파일을 정상 판독해 새로 등록 |
| 4. 호환 앱 배포 / BE | 신규 공유 생성은 기본 OFF. 웹·단일 runner·별도 worker가 있으면 그 worker까지 모두 두 저장 형식의 읽기/변경/최종저장을 지원해야 함. 혼재 중에는 NULL 경로만 생성. API/소유/직접 업로드 회귀·새 저장 테스트 통과 뒤 다음 단계 |
| 5. 제한 활성화 / BE·운영 | 승인 환경에서 Case 신규 생성만 ON. 같은 기준 사용자A/B 생성, A 수정/B revision0 불변, 재개/응답유실, 동시 첫 생성, source 변경 충돌, 삭제/행 이동/열 변경/과거 복원/최종저장, 직접 업로드 생성·교체를 검증. 실제4파일 결과 보존/형식과 권한 확인 전 확대 금지 |
| 6. verify / BE·DBA | 아래 이상 검출 항목 각각0, 성공 baseline1건, 신규 두 번째 Draft의 초기 물리 rowm/rowh 각각0, 성공 revision0/고정파일4건 확인. 첫·재사용 시간/스캔 횟수·INSERT 건수·잠금 대기/rollback·메모리 실측. 설계 예시226,102를 실측값으로 사용하지 않음 |
| 7. 중단 / 운영 | 소유·원본 불일치, 잘못된 rowId/위치·이력, 부분 commit, 파일 재생성 불일치 또는 운영 한도 초과면 신규 공유 생성 OFF. 문제 Draft의 새 변경/최종저장을 중단하고 성공 이력/원본을 보존. 실행 중 작업은 기존 종료 확인/token 규칙으로 정리하며 행을 임의 삭제하지 않음 |
| 8. rollback / BE·DBA | 공유 Draft가 생기기 전에는 신규 생성 OFF 후 이전 앱 복귀 가능. 공유 Draft가 한 건이라도 있으면 **두 형식을 읽는 마지막 정상 앱으로 복귀**하며 공유 reader/writer는 유지. base_id를 NULL로 지우거나 옛 앱으로 바로 복귀하면 원본 행이 사라진 것으로 해석되므로 금지. 새 table/column은 보존; 즉시 DROP 불필요 |

후속 DBA 검증 질의는 다음 조건을 독립 검출한다. 저장소 SQL 파일의 기존 전체 행 존재 전제를 가진 검증 쿼리는 공유 Draft를 별도 분기하도록 증분 패키지에 작성하고, 기존/UPLOAD 검증은 보존한다.

- baseline 재사용 키 중복, 정확히4개가 아닌 manifest/역할 중복/잘못된 file_no·객체 참조, row_cnt와 실제 row 수 불일치.
- baseline 행 table의 template 불일치, bodyRanges 밖/중복 원본 좌표, soft delete된 기준 child. SQL로 JSON 범위 검사를 구현하기 어려우면 같은 source 정의를 읽는 BE 검증 도구로 검출하며 검사 생략으로 바꾸지 않는다.
- 공유 Draft인데 CASE가 아님, parent와 회사/Case/template/sourceVersion 불일치, 고정 파일과 manifest 불일치, baseline의 물리 원본/ID 불일치 또는 `file_rvsn_no<>0`.
- 논리 전체 current 위치 중복/누락, original 첫 변경의 `[0,r)` 누락, 구간 겹침/비정상 종료, 성공 delta/작업 결과와 양방향 복원 불일치. 파생 UUID/전체 재생 검증은 BE 도구가 담당한다.
- legacy/UPLOAD의 NULL base 유지와 row/file baseline 검증 통과. 변경된 source 때문에 기존 Draft가 조회 거절되는 정상 충돌을 데이터 손상으로 오판하지 않는다.

새 index의 실제 사용은 baseline 적중 `_ux01`, 페이지 PK의 `(base_id,inpt_tbl_id,org_row_no)` 범위, 물리 override의 기존 `(drft_id,inpt_tbl_id,file_rvsn_no,org_row_no)` 및 row history index를 `EXPLAIN`으로 확인한다. 무결성 전체 count의 index scan은 의도된 비용이므로 무조건 ALL/index scan을 실패로 보지 않는다. 선택 페이지에 전체 행 scan이 생기거나 잘못된 선두 key를 쓰면 활성화 전에 수정한다.

구버전 앱으로 완전 복귀해야 할 별도 요구가 생기면 쓰기를 멈추고 각 공유 Draft의 원본 가상 identity/전체 위치를 **동일 rowId**로 기존 rowm/rowh에 완전히 전개한 뒤 합성 전후 모든 성공 revision·파일 결과를 대조하고 마지막에 base_id를 해제해야 한다. 대규모 데이터 변환·부분 중단 복구가 필요하므로 이 설계의 자동 backfill/rollback에 포함하지 않는다. 기본 복구 경로는 호환 앱 유지다.

#### 6.12.6 구현 순서·준비도·사전 검증

1. 내부 slice: 실제 환경에 맞춘 additive DDL 초안·단일 합성 경로·결정적 UUID·최초 위치 변경 저장·과거 복원 테스트. 생성 OFF이며 기존/UPLOAD 저장 회귀 확인. 이 단계만 배포해 공유 생성 ON을 허용하지 않는다.
2. 연결 slice: EDT-01 baseline 생성/재사용 및 위 모든 소비자 연결, 두 저장 형식 CHANGE/FINALIZE·worker 재시작 테스트. mock/단위 검증과 실제 MySQL FK/UK/row lock·batch rollback 증거를 구분한다. 현재 `create` 재개는 Case→Draft인 반면 `prepareClaimed → verify`는 Draft/operation→Case여서 기존 deadlock 가능성이 있다. 생성 경로의 순서를 전체 경로의 공통 보장으로 해석하지 않는다. 제한 활성화 전 FINALIZE 동시 재개를 검증하고, deadlock이면 해당 transaction 전체 rollback·성공 이력 보존·EDT-01 재호출 대조를 확인한다. 잠금 순서 통일은 기존 위험에 대한 별도 최소 보완 후보이며 이번 문서만으로 Java 수정 범위를 추가 승인하지 않는다.
3. DBA 수동 적용·호환 앱 전환·제한 활성화 및 성능/파일 결과 검증. 이미 적용한 DDL을 재실행하지 않고 pre-check 결과를 따른다.

필수 동작 검증에는 두 Draft의 ID 분리/안정성, A의 이동·삭제·추가 후 B 불변, 첫 변경 이전 과거 revision, 이동 목적지가 가상 원본인 중복 거절, 같은 요청 재전송, batch 중간 실패/commit 불명, 회사·사용자 혼입 거절, source/template 변경 충돌, 열 변화/Regas 연결 행, 과거 full-row Draft와 UPLOAD_FILE 교체, FINALIZE 고정 양식/4파일 보존을 포함한다. 신규 생성 ON 전에 N 규모의 첫/재사용 비용도 측정한다.

- Dictionary: 기존 `BASE/VER/ROW/ORG/FILE/OBJ/REF/VAL/CNT`와 현행 공통 토큰을 재사용한다. 추가 용어 없음. 신규 object는 `tmd_mdl_inpt{base...}{m/d}` 및 `_ux/_ix/_fkNN` 규칙을 따른다. 감사명 예외는 §6.8의 기존 사용자 결정이다.
- Open Questions: 이 slice의 미해결 업무/스키마 선택 없음. 환경별 실제 제약/권한/운영 한도는 pre-check 증거 항목이며 이미 확인한 값으로 주장하지 않는다.
- implementation readiness: `ready` — 내부 persistence/복원 slice의 구현 입력. 실행 DDL 작성/DBA 적용 승인·운영 활성화는 별도다.

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

- 감사 컬럼 이름·정밀도의 사용자 결정은 §6.8을 따른다. 실제 적용/외부 표준·환경 시간 확인은 DBA-04로 남긴다.
- generated column/unique 지원과 이름 충돌을 확인한다.
- seed SQL과 승인된 업무 구조가 호환되는 4개 workbook 검증 결과를 함께 승인한다. 개발 시트의 파일 수용과 runtime catalog 제외는 domain 규칙39~40을 따른다.
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

공통 저장 정의는 §6.3, **기존 코드/문서 대조 완료 범위**는 §6.4 및 domain §5.4다. 아래는 실제 DB 확인이 필요한 후속 목록이며, 파일 metadata의 사용자 제공 정의와 매핑 정렬은 §6.4에 반영했다. 해당 정렬만으로 DBA-01~05 전체 확인·운영 적용을 완료로 판단하지 않는다. Codex는 직접 DB 접속·SQL 실행/적용을 하지 않았으며 소스의 annotation이나 과거 DDL 초안을 운영 적용 증거로 쓰지 않는다.

| ID / 담당 | 실제 확인할 것 | 필요한 결과/완료 경계 |
| --- | --- | --- |
| DBA-01 / DBA, BE 대조 | master의 Case/파일/User/회사·catalog6개/Case-template의 존재·실제 CREATE 정의와 신규7개 이름 충돌. PK/FK signedness/길이/NULL·engine/charset/collation·파일 metadata(§6.4)/file_size·참조 가능한 active 데이터/최대 ID·auto_increment | 실제 정의와 §6.3/§6.4의 차이 목록. Case→Integer relation/파일번호→양의 int64 등 코드 표현 범위 확인. 기존 Case4파일·발행 연결 대상 여부는 BE와 확인하되 목록/seed 구축·내용 재등록으로 확대하지 않음 |
| DBA-02 / DBA, BE 경합 검증 | 실제 MySQL 버전에서 nullable generated UK·복합 FK/parent revision·CHECK/JSON_VALID가 동작하는지, 같은 master transaction/엔진·격리/lock 설정이 맞는지 | 적용 가능한 제약/생성 순서와 경합 검증 환경 확인. 실제 동시 생성/접수/최종 commit·늦은worker 시험은 구현 후 BE+DBA 검증이며 지금 Pass로 기록하지 않음 |
| DBA-03 / DBA, BE 저장/접속 대조 | max_allowed_packet·connector/배포 환경 한도와 utf8mb4 LONGTEXT/JSON_VALID. 최대16 MiB delta 및 같은 SQL/프로토콜 여유 수용 여부 | 실제값과 최대 영속 요청의 여유 확인. 부족하면 적용 전에 설정/계약 대응 확정; 숫자 반올림·문자열 절삭/부분 저장으로 회피하지 않음. multipart300MB를 JSON/DB 한도 증거로 사용하지 않음 |
| DBA-04 / 사용자·BE, 외부 DBA·표준 담당은 별도 | 사용자 채택: 기존 crt_*/mod_*/del_yn 이름과 감사 DATETIME(0) 유지(§6.8). 실제 컬럼/백업·소수점 손실·증분 적용/verify와 환경별 DB/JVM 시간대 확인 | 사용자 방향 결정은 완료, 실제 적용·검증과 외부 DBA 표준 확인은 미완료. 사용자 채택을 외부 DBA 공식 승인으로 표시하지 않으며 시간대 설정은 이번에 변경하지 않음 |
| DBA-05 / DBA·BE 배포 담당 | 위 결과를 반영한 CREATE/ALTER/검증 SQL 패키지·권한·적용 순서, ddl-auto:validate 앱과의 호환/롤백·운영 owner/go-no-go | 실제 환경에 맞는 패키지와 수동 적용 승인 후 별도 실행. 이번에는 SQL 패키지/실행을 만들지 않음. 참조 데이터/성공 이력 보존·기존 Case backfill 불필요 기준 유지, legacy cutover/표별 정책·seed 설계는 별도 |

BE 구현 후의 제품 검증은 별도다: 입력→JSON text→DB→재조회/XLSM 수치 보존, body/로그·소유/회사 접근, durable 접수/격리 worker·재시작/timeout/UNKNOWN·부분 업로드/늦은PUT/정리·같은 최종 Case를 검증한다. 기존 Scenario staged/Run 시험 결과를 새 Draft의 Pass로 사용하지 않는다. 실제 저장소 ACL/암호화·편집 worker 운영 강제는 BE/운영 확인이며 DBA만의 DB 실물 확인 목록으로 넘기지 않는다.

표별 field/구조·파생/화면 정책은 후속이다. Sector 국가/하위부문 목록·option snapshot/seed 연결안을 되살리지 않으며 이미 승인한 Sector/Monthly/BCPrices 정책은 유지한다. **PriceModel/Regas 추가 입력 추천은 미승인·후속**이다. 장기 archive/폐기는 별도이며 이번에는 자동 만료를 도입하지 않는다.

기존 위험/후속 항목:

- 구조 변경 허용 범위는 domain 규칙 13·26·45·47을 따른다. 마지막 행·열 추가와 개별 중간 삽입·삭제 예외를 구분할 정책 저장 구조·검증 계약은 미정이며, 현재 추가 가능 플래그만으로 전체 정책을 표현하지 않는다.
- 새 행/열의 수식·서식·validation 복제 규칙이 미정이다.
- 구조 hash의 정규화/판별 방식이 미정이다.
- 감사 컬럼 이름/정밀도는 사용자 채택(§6.8)으로 정리했다. 실제 적용·외부 표준 확인/환경 시간대는 DBA-04 잔여다.
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

- 2026-10-21 공유 기준 slice(§6.12): 내부 persistence/복원 구현 입력은 `ready`. 실행 DDL 작성·실제 MySQL/DBA 적용·성능/파일 보존 검증은 후속이며 아래 전체 제품 준비도와 구분한다.

- 2026-10-19 직접 업로드: §6.10 저장 구조/증분 SQL 작성과 독립 정적 검토 완료. 저장 구조 구현 입력은 `ready`, 실제 MySQL/환경 대조·동시성 검증 전 DBA 적용은 `blocked by open questions`다. 신규 API/worker 및 일반 행·열 처리의 제품 구현/검증 완료 판정은 별도다.

- 2026-10-16: §6.3의 관계형/JSON 방향·공통 유일성/성공 경계는 사용자 채택한 설계 기준이다. 공통 물리 정의/저장 계약은 §6.3으로 문서 범위 완료다. 전체 CREATE/ALTER/verify SQL·DBA rollout 및 실제 제품 동작은 미완료다. 기존6개 SQL의 적용 완료/확장 완료로 읽지 않는다. 공통 설계 입력은 `ready after minor cleanup`(실제 schema·감사 예외/SQL 적용 준비 확인), 표별 쓰기/최종 파일 제품 활성화는 해당 정책/STRUCT·구현 검증 전 `blocked by open questions`다.

- 코드/문서 확인 결과: §6.4 대조 Complete. 추가 사용자 공통 업무 결정 없음. 실제 DB 준비도는 DBA-01~05 미확인으로 별도 유지하며 공통7개/DDL 적용·제품 동작을 승인하지 않는다.

- ERD/DDL review verdict: `REVISE` for rollout — 공통 저장 구조 선택은 완료, 실제 schema/감사·SQL 패키지/DBA 확인이 남았다.
- hierarchy/keys/types/index 방향은 구현 입력으로 충분하다.
- DBA apply와 행·열 추가 실행 API는 §6.8의 감사 변경 적용 검증, 추가 방식, 복제 규칙을 해결한 뒤 `ready`로 전환한다.
- current readiness: `blocked by open questions` for write/execute implementation;
  read-only catalog schema/entity drafting is `ready after minor cleanup`.
- Q2 기존 원본 등록과 §6.2/§10B의 양식/catalog 연결안은 추가 스키마 없이 기존 master/관계를 사용한다. table/GET 정의·seed/발행·Case 연결 등록·제품 검증은 미완료다. 기존 catalog의 `ready after minor cleanup` 및 write/execute의 `blocked by open questions` 판정을 변경하지 않는다.
