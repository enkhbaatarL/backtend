# MI Base 데이터 적재 가이드 (외부 적재 주체용)

- domain: gas
- status: draft (적재 주체 협의용)
- owner: platform BE
- updated at: 2026-06-16
- role: MI Base baseline 데이터를 플랫폼 실행 테이블에 직접 INSERT 하는 외부 적재 주체에게 대상 테이블/컬럼/규칙/순서를 안내하는 핸드오프 문서

> 이 문서는 "적재하는 쪽"이 보는 가이드다. 플랫폼 백엔드는 MI Base 데이터를 **조회 전용**으로만 사용하고, 적재(INSERT)는 하지 않는다.

## 1. 목적

플랫폼 가격전망 화면은 차트에 `MI Base`(가장 최신 기준) 곡선을 오버레이로 그린다. 이를 위해 MI Base baseline 결과를 플랫폼이 이미 쓰는 **실행 테이블 구조**(시나리오 케이스/실행이력/가격결과)에 한 세트로 적재해야 한다.

- 사용자가 실행하는 데이터(독립실행/시나리오실행)와 **같은 테이블**을 공유하되, `exec_type_cd = 'MI_BASE'` 로 구분한다.
- MI Base는 사용자 실행 흐름(대기→실행중→완료)을 타지 않는다. 적재 시점에 이미 완료된 데이터이므로 `COMPLETED` 상태로 바로 INSERT 한다.

## 2. 적재 대상 테이블

| 순서 | 테이블 | 역할 | 적재 row 수 |
|---|---|---|---|
| 1 | `tmd_mdl_scnrcasem` | 시나리오 케이스 마스터 (1세트의 머리) | 1건 |
| 2 | `tmd_mdl_scnrcaseh` | 실행 이력 (결과 연결 키 `run_seq` 발급) | 1건 |
| 3 | `tmd_mdl_scnrcaseprcl` | 가격 결과 (차트에 그려지는 실제 데이터) | N건 (지역 × 연도별·월별 기간 모두) |
| 4 | `tcm_filel` (입력 파일) | 입력 파일 메타데이터 (`SCNRCASE` + `case_id`) | 입력 파일 수만큼 |
| 5 | `tcm_filel` (출력 파일) | 출력 파일 메타데이터 (`SCNRCASEH` + `run_seq`) | 출력 파일 수만큼 |

- 결과 데이터는 전부 `tmd_mdl_scnrcaseh.run_seq` 를 외래 키처럼 사용해 `run_seq` 단위로 연결된다.
- MI Base는 **가격전망 차트 오버레이**가 목적이므로 가격(`tmd_mdl_scnrcaseprcl`)만 적재하면 된다. Volumes/Trade Flows 결과 테이블은 MI Base 범위가 아니다(필요해지면 동일하게 `run_seq` 로 연결, 별도 협의).
- 파일은 **실제 바이너리는 S3(object storage)에 업로드**하고, `tcm_filel` 에는 그 메타데이터(원본/저장 파일명, 저장 경로, 크기, 관계 키)만 INSERT 한다. `tcm_filel` 자체에 파일 바이너리를 넣지 않는다.
- 파일과 케이스/실행의 연결 기준:

| 구분 | `file_rel_obj_spt_cd` | `file_rel_obj_dstg_no` |
|---|---|---|
| 입력 파일 | `SCNRCASE` | `tmd_mdl_scnrcasem.case_id` |
| 출력 파일 | `SCNRCASEH` | `tmd_mdl_scnrcaseh.run_seq` |

## 3. 핵심 규칙 (반드시 준수)

1. **`exec_type_cd = 'MI_BASE'`, `scnr_id = NULL`** — 이 둘은 같이 가야 한다. 마스터에는 `CHECK ((exec_type_cd = 'SCENARIO') = (scnr_id IS NOT NULL))` 제약(`tmd_mdl_scnrcasem_ck01`)이 걸려 있어, `MI_BASE` 인데 `scnr_id` 를 채우면 INSERT 가 거부된다.
2. **상태는 `COMPLETED`** — 마스터 `case_stat_cd = 'COMPLETED'`, 이력 `exec_stat_cd = 'COMPLETED'`. `PENDING`/`RUNNING` 단계를 거치지 않는다.
3. **`usr_no` 는 admin 사용자 번호** — MI Base는 특정 업로더가 없으므로 운영에서 합의한 admin `usr_no` 고정값을 넣는다. (실제 값은 §7 참고)
4. **`run_id` 형식** — `yyyymmdd_hhmmss_XXXX` (예: `20260616_021500_b0a1`), `VARCHAR(20)`, 전역 unique. 적재 주체가 생성한다.
5. **`run_seq` 는 자동 증가** — `tmd_mdl_scnrcaseh.run_seq` 는 `AUTO_INCREMENT` 다. INSERT 시 값을 넣지 말고, 넣은 뒤 `run_id` 로 다시 조회해 `run_seq` 를 얻어 가격 결과에 사용한다.
6. **"최신 1건" 은 강제 제약이 아니라 조회 규칙** — 플랫폼은 `exec_type_cd = 'MI_BASE'` 중 `case_id` 가 가장 큰(가장 최근) 1건을 읽는다. 따라서 새 baseline 이 나오면 **새 케이스 세트를 새로 INSERT** 하면 되고, 과거 건은 그대로 둬도 된다(이력 누적). 굳이 정리하려면 과거 마스터를 `del_yn = 'Y'` 로 soft delete 한다(물리 삭제 금지).

## 4. 적재 순서 (한 트랜잭션 권장)

```text
1) tmd_mdl_scnrcasem INSERT (MI_BASE, COMPLETED)  → case_id 확보 (AUTO_INCREMENT)
2) run_id 생성 (yyyymmdd_hhmmss_XXXX)
3) tmd_mdl_scnrcaseh INSERT (case_id, COMPLETED)  → run_seq 확보 (run_id 로 재조회)
4) tmd_mdl_scnrcasem UPDATE last_run_id = run_id    (최신 실행 포인터 연결)
5) tmd_mdl_scnrcaseprcl INSERT × N (run_seq 로 연결)
6) 입력 파일 → S3 업로드 후 tcm_filel INSERT (SCNRCASE + case_id)
7) 출력 파일 → S3 업로드 후 tcm_filel INSERT (SCNRCASEH + run_seq)
```

> 파일이 없으면 6/7 단계는 생략. 입력 파일은 baseline 생성에 쓴 원본이 있을 때만 적재하면 된다(필수 아님). 출력 파일(결과 산출물)이 차트 외 다운로드로 제공돼야 하면 7단계로 적재한다.

## 5. INSERT 예시

> 컬럼은 명시적으로 나열한다. `actv_scnr_id`(생성 컬럼), `run_seq`(자동 증가)는 값으로 넣지 않는다.
> `:admin_usr_no`, `:admin_login_id` 는 운영 합의값으로 치환한다.

### 5-1. 마스터 (`tmd_mdl_scnrcasem`)

```sql
INSERT INTO tmd_mdl_scnrcasem
    (usr_no, scnr_id, exec_type_cd, case_titl_nm, case_expn, case_stat_cd, last_run_id,
     crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
VALUES
    (:admin_usr_no, NULL, 'MI_BASE', 'MI Base 2026-06-16', 'MI Base baseline 적재', 'COMPLETED', NULL,
     :admin_login_id, NOW(), :admin_login_id, NOW(), 'N');
-- 발급된 case_id 확보 (LAST_INSERT_ID() 등)
```

### 5-2. 실행 이력 (`tmd_mdl_scnrcaseh`)

```sql
INSERT INTO tmd_mdl_scnrcaseh
    (run_id, case_id, exec_stat_cd, exec_bgn_dtm, exec_end_dtm,
     crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
VALUES
    ('20260616_021500_b0a1', :case_id, 'COMPLETED', NOW(), NOW(),
     :admin_login_id, NOW(), :admin_login_id, NOW(), 'N');
-- run_seq 재조회
SELECT run_seq FROM tmd_mdl_scnrcaseh WHERE run_id = '20260616_021500_b0a1';

-- 마스터 최신 실행 포인터 연결
UPDATE tmd_mdl_scnrcasem
SET last_run_id = '20260616_021500_b0a1', mod_dtm = NOW()
WHERE case_id = :case_id;
```

### 5-3. 가격 결과 (`tmd_mdl_scnrcaseprcl`)

지역(JKM/TTF/HH) × 기간 조합마다 1행. `UNIQUE (run_seq, prc_rgn_nm, perd_key)` 가 있어 같은 run_seq 안에서 (지역, 기간) 중복은 거부된다.

**연도별(`Y`)과 월별(`M`) 데이터를 모두 적재해야 한다.** 같은 run_seq 안에 두 단위가 함께 들어간다.

- 연도별: `perd_unit_spt_cd = 'Y'`, `perd_key = 'YYYY'` (예: `2026`)
- 월별: `perd_unit_spt_cd = 'M'`, `perd_key = 'YYYYMM'` (예: `202607`)
- `perd_unit_spt_cd` 가 unique 키에는 없지만 `perd_key` 형식(4자리 vs 6자리)이 달라 연/월 행은 서로 충돌하지 않는다.

```sql
INSERT INTO tmd_mdl_scnrcaseprcl
    (run_seq, perd_unit_spt_cd, perd_key, prc_rgn_nm, base_case_prc, user_adj_prc,
     crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
VALUES
    -- 연도별 (Y)
    (:run_seq, 'Y', '2026', 'JKM', 11.60, 11.60, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N'),
    (:run_seq, 'Y', '2026', 'TTF', 10.20, 10.20, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N'),
    (:run_seq, 'Y', '2026', 'HH',   3.50,  3.50, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N'),
    -- 월별 (M)
    (:run_seq, 'M', '202607', 'JKM', 11.85, 11.85, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N'),
    (:run_seq, 'M', '202607', 'TTF', 10.40, 10.40, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N'),
    (:run_seq, 'M', '202607', 'HH',   3.55,  3.55, :admin_login_id, NOW(), :admin_login_id, NOW(), 'N');
    -- ... 연/월 각각 기간·지역 전체 반복
```

가격 컬럼 의미:

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `perd_unit_spt_cd` | VARCHAR(2) | 기간 단위. `M`(월) / `Y`(년) |
| `perd_key` | VARCHAR(6) | 기간 키. 월이면 `YYYYMM`, 년이면 `YYYY` |
| `prc_rgn_nm` | VARCHAR(200) | 가격 지역명. `JKM` / `TTF` / `HH` |
| `base_case_prc` | DOUBLE | 베이스케이스 가격 (단위: USD/MMBtu) |
| `user_adj_prc` | DOUBLE | 사용자 조정 가격. MI Base는 조정 개념이 없으므로 `base_case_prc` 와 동일 값으로 채운다(협의 필요) |

### 5-4. 입력 파일 (`tcm_filel`, `SCNRCASE` + `case_id`)

먼저 파일 바이너리를 **S3에 업로드**한 뒤, 그 메타데이터를 `tcm_filel` 에 INSERT 한다. `file_rel_obj_spt_cd = 'SCNRCASE'`, `file_rel_obj_dstg_no = case_id` 로 케이스에 연결한다. (입력 원본이 있을 때만, 파일 1개당 1행)

```sql
-- 1) S3 업로드 (예시: 적재 주체의 storage client 사용)
--    s3://{bucket}/mi-base/{case_id}/demand_20260616.xlsx  등으로 PUT

-- 2) 업로드 결과 메타데이터를 tcm_filel 에 INSERT
INSERT INTO tcm_filel
    (org_file_nm, strg_file_nm, strg_path_nm, file_size, filex_nm, mime_type_nm,
     file_rel_obj_spt_cd, file_rel_obj_dstg_no, img_conv_cmpt_yn,
     crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
VALUES
    ('demand_2026.xlsx', 'demand_20260616_021500.xlsx', 'mi-base/12345',
     20480, 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
     'SCNRCASE', :case_id, 'N',
     :admin_login_id, NOW(), :admin_login_id, NOW(), 'N');
    -- 입력 파일이 여러 개면 행 반복 (file_rel_obj_dstg_no 는 동일 case_id)
```

### 5-5. 출력 파일 (`tcm_filel`, `SCNRCASEH` + `run_seq`)

출력 산출물도 **S3 업로드 후** `tcm_filel` 에 INSERT 한다. 입력과 달리 `file_rel_obj_spt_cd = 'SCNRCASEH'`, `file_rel_obj_dstg_no = run_seq` (실행 단위) 로 연결한다.

```sql
-- 1) S3 업로드
--    s3://{bucket}/mi-base/{case_id}/output/{run_id}/result.xlsx  등으로 PUT

-- 2) tcm_filel INSERT
INSERT INTO tcm_filel
    (org_file_nm, strg_file_nm, strg_path_nm, file_size, filex_nm, mime_type_nm,
     file_rel_obj_spt_cd, file_rel_obj_dstg_no, img_conv_cmpt_yn,
     crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn)
VALUES
    ('mi_base_result.xlsx', 'result_20260616_021500.xlsx', 'mi-base/12345/output',
     51200, 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
     'SCNRCASEH', :run_seq, 'N',
     :admin_login_id, NOW(), :admin_login_id, NOW(), 'N');
    -- 출력 파일이 여러 개면 행 반복 (file_rel_obj_dstg_no 는 동일 run_seq)
```

`tcm_filel` 주요 컬럼:

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `file_no` | BIGINT UNSIGNED | PK, AUTO_INCREMENT (직접 넣지 않음) |
| `org_file_nm` | VARCHAR(100) | 원본 파일명 (NOT NULL) |
| `strg_file_nm` | VARCHAR(100) | S3 저장 파일명 (NOT NULL) |
| `strg_path_nm` | VARCHAR(100) | S3 저장 경로(prefix/bucket 정책에 맞춤, NOT NULL) |
| `file_size` | INT UNSIGNED | 파일 크기(byte) |
| `filex_nm` | VARCHAR(20) | 파일 확장자명 |
| `mime_type_nm` | VARCHAR(100) | MIME 유형명 |
| `file_rel_obj_spt_cd` | VARCHAR(20) | 관계객체 구분코드. 입력 `SCNRCASE` / 출력 `SCNRCASEH` |
| `file_rel_obj_dstg_no` | INT UNSIGNED | 관계객체 식별번호. 입력 `case_id` / 출력 `run_seq` |
| `img_conv_cmpt_yn` | CHAR(1) | 이미지변환완료여부 (NOT NULL). 비이미지 파일은 `'N'` |

## 6. 제약 / 주의

- `tmd_mdl_scnrcasem_ck01` CHECK: `MI_BASE` 는 `scnr_id` 가 반드시 NULL. (위반 시 INSERT 실패)
- `tmd_mdl_scnrcaseh.run_id` PK + `run_seq` UNIQUE: 같은 `run_id` 재사용 금지.
- `tmd_mdl_scnrcaseprcl` UNIQUE `(run_seq, prc_rgn_nm, perd_key)`: 동일 실행 안에서 (지역, 기간) 중복 금지.
- `actv_scnr_id` 는 DB 생성 컬럼이라 직접 INSERT/UPDATE 불가. `MI_BASE` 는 `scnr_id = NULL` 이라 항상 NULL 로 파생되어 active 시나리오 unique 가드와 무관하다.
- 모든 테이블에 공통 audit 컬럼(`crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn`)이 NOT NULL 로 존재한다. `del_yn` 기본 `'N'`.
- 물리 삭제(`DELETE`) 금지. 폐기는 `del_yn = 'Y'` soft delete.
- 파일은 **S3 업로드가 먼저, `tcm_filel` INSERT 가 나중**이다. 메타데이터만 적재하며 바이너리는 넣지 않는다.
- `tcm_filel.file_rel_obj_dstg_no` 는 `INT UNSIGNED` 다. 입력은 `case_id`, 출력은 `run_seq` 를 정확히 넣어야 플랫폼 조회가 연결된다(혼동 시 파일이 화면에 안 붙는다).
- `tcm_filel.img_conv_cmpt_yn` 은 NOT NULL 이므로 비이미지(xlsx 등)는 `'N'` 으로 채운다.
- 파일 연결 코드 `SCNRCASE` / `SCNRCASEH` 는 공통코드(`file_rel_obj_spt_cd` 그룹)에 등록돼 있어야 한다(플랫폼 측에서 관리, [erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md) 참고).

## 7. 운영 협의 필요 항목

- **admin `usr_no` / 로그인 ID 실제값** — 환경별(dev/prod) MI Base 적재용 admin 계정 식별자 확정.
- **케이스 제목/설명 규칙** — `case_titl_nm` 에 baseline 기준일자 등을 어떻게 표기할지(예: `MI Base YYYY-MM-DD`).
- **`user_adj_prc` 채움 규칙** — MI Base에 조정 개념이 없으면 `base_case_prc` 동일값으로 둘지 확정.
- **갱신 주기/방식** — 새 baseline 시 신규 세트 INSERT(누적) vs 과거 soft delete 정리 정책.
- **결과 범위** — 가격 외 Volumes/Trade Flows도 MI Base가 채워야 하는지(현재 가이드는 가격 only 전제).

## 8. 참고 (플랫폼 측 canonical 문서)

- 실행유형 컬럼/CHECK/공통코드 등 물리 스키마: [scenario-case-execution-erd-ddl-draft.md](scenario-case-execution-erd-ddl-draft.md)
- 실행 workflow/도메인 의미: [scenario-case-execution-workflow-design-draft.md](scenario-case-execution-workflow-design-draft.md)
- 결과 데이터(Volumes/Price/Trade) 영속 설계: [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)
