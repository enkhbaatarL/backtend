# Model Input Draft 최초 배포 실행 안내서

## Document Control

- status: Review / 사용자·DBA 수동 적용용
- canonical for: Reference only — 정본 SQL의 실행 순서와 결과 판정 안내
- owner: BE / DBA
- date / last synced: 2026-10-22
- source documents: 아래 DDL·Catalog 등록·발행 SQL 3개
- depends on: 각 SQL의 Document Control, 전제 조건 및 검증 구간
- supersedes: 없음
- open questions: 실제 개발계·운영계 서버/권한/DDL 적용·COMMIT·API 검증 결과는 미확인

**개발계에서 먼저 완료한 뒤, 같은 원본 SQL로 운영계에서 이 절차를 처음부터 반복한다.** 이 문서는 DB에 적용한 결과가 아니라 적용 방법이다. 테이블 정의·데이터·검증 SQL은 아래 3개 파일이 정본이다. 문서와 SQL이 다르면 실행을 멈추고 배포 담당자에게 확인한다.

## 1. 사용할 파일과 전체 순서

| 순서 | 사용할 파일/작업 | 완료 시 상태 |
| --- | --- | --- |
| 1 | [DDL](model-input-draft-install-ddl.sql) §1 사전 확인 | 신규 19개 테이블이 없는 올바른 쓰기 DB |
| 2 | DDL §2 생성 및 §4 정의 확인 | 19개 테이블 생성, 아직 모두 빈 상태 |
| 3 | DDL §3 실행기 초기화 예행 → 실제 저장 → §4 검증 | 실행기 STOPPED 1행, 나머지 18개 0행 |
| 4 | [Catalog 등록](model-input-catalog-install-registration.sql) 예행 → 실제 저장 | v5 DRAFT, 역할 4·시트 70·표 94·수정 범위 146 |
| 5 | 대상 환경의 호환 코드 배포 확인 | 배포 담당자가 v5 reader 지원 확인 |
| 6 | [Catalog 발행](model-input-catalog-install-publication.sql) 예행 → 실제 저장 | 해당 v5가 ACTIVE, 같은 모델 ACTIVE는 1개 |
| 7 | 별도 연결에서 최종 결과 확인·인계 | DB 준비 결과와 남은 API 검증을 구분해 기록 |

이번 파일은 **최초 설치 전용**이다. 대상 19개 테이블 또는 모델 `DRAFT_C_STRUCTURE`의 이전 Catalog가 이미 있으면 기존 데이터를 지우거나 덮어쓰지 말고 중단한다. 기존 버전 교체와 기존 Case의 양식 재연결은 이 절차에 포함하지 않는다.

작성 시점의 원본 파일 SHA-256은 다음과 같다. SHA-256은 파일 내용이 같은지 비교하는 지문이다. `shasum`을 쓸 수 있는 환경에서는 `shasum -a 256 <파일 경로>`로 확인한다. 다른 도구를 쓰는 경우에도 SHA-256 결과를 비교한다. 값이 다르면 배포 담당자에게 최신 검토본인지 확인한다. 환경 입력을 채운 실행 복사본의 해시는 원본과 달라질 수 있다.

| 원본 파일 | SHA-256 |
| --- | --- |
| DDL | `60a2b43f77f0203776eee998f33325c3f818f6ff1cec65377be3bb1197fc255c` |
| Catalog 등록 | `c7b4353629bf7d39804c07949c60120915707223f7ba24f9bd2730f588c65072` |
| Catalog 발행 | `c054e3189f486223ef51ef6bff8d8702ccade623fc9104bc3070f6abe188999b` |

### 적용 전 배포 담당자와 확인할 것

- 원본 SQL 3개가 개발계에서 검토한 동일 파일인지 파일 SHA-256으로 확인하고, 환경별 실행 결과와 함께 보관한다. 환경 입력을 바꾼 **실행 복사본**은 별도로 보관한다. 복사본에서 수정할 것은 지정된 입력값과 최종 COMMIT 선택뿐이다.
- 대상 DB의 복구 가능한 백업과 권한을 확인한다. 접속 정보·백업 위치는 해당 조직 절차로 관리하고 비밀번호·토큰은 이 문서나 결과 파일에 넣지 않는다.
- 관련 Catalog writer, API 접수, web runner/worker가 쓰지 않도록 중지한다. DDL의 기능 비활성화 설정과 별도로 실제 쓰기 중지를 확인한다. 진행 중 요청을 강제로 삭제해 맞추지 않는다.
- DB 클라이언트의 **오류 후 계속 실행** 옵션을 끈다. 오류가 나면 뒤 문장을 계속 실행하지 않는다. 다른 미완료 transaction이 없는 새 전용 연결을 사용한다.

DDL은 테이블 구조를 만드는 SQL이다. DDL 생성은 성공한 문장마다 확정될 수 있어 `ROLLBACK`으로 취소되지 않는다. 실행기 초기화와 Catalog 등록·발행은 transaction, 즉 한 묶음의 변경으로 처리하며 `COMMIT`은 저장, `ROLLBACK`은 그 묶음 취소다.

## 2. 환경별 입력값 준비

DB 클라이언트에서 **적용할 환경의 쓰기 DB**에 연결하고 대상 DB를 선택한다. 다음 조회만 먼저 실행한다.

```sql
SELECT @@GLOBAL.server_uuid AS server_uuid,
       DATABASE() AS schema_name;
```

| 기록할 값 | 어디서 확인하는가 | 입력 예시 |
| --- | --- | --- |
| 환경 | 이번 적용 대상이 개발계인지 운영계인지 확인 | 개발계 `DEVELOPMENT`, 운영계 `PRODUCTION` |
| 서버 UUID | 위 `server_uuid` 결과와 DB 클라이언트의 접속 대상을 함께 대조 | 조회한 UUID 문자열 |
| DB 이름 | 위 `schema_name` 결과 | 결과가 midb일 때만 `midb` |
| 적용자 | 작업 이력에 남길 실제 적용자 로그인 ID | 실제 ID로 바꾼 `release.operator` |

`schema_name=NULL`이면 DB를 선택하고 다시 조회한다. UUID는 서버 고유 ID이며 조회 결과만으로 개발계/운영계인지 알 수 없다. DBA가 확인한 접속 대상과 대조한다. 적용자는 영문·숫자·`.`·`_`·`@`·`-` 조합 1~50자이며 비밀번호를 넣는 칸이 아니다.

DDL **실행 복사본의 기존 NULL 줄**을 다음 형태로 수정한다. 아래 `<...>`는 실제 값으로 교체하며 그대로 실행하지 않는다.

```sql
SET @mi_install_environment = 'DEVELOPMENT';
SET @mi_install_expected_server_uuid = '<확인한 UUID>';
SET @mi_install_expected_schema = '<확인한 DB 이름>';
SET @mi_install_operator = '<실제 적용자 로그인 ID>';
```

Catalog 등록·발행 **각 실행 복사본**에서는 같은 값을 다음 변수명으로 입력한다.

```sql
SET @ci_environment = 'DEVELOPMENT';
SET @ci_expected_server_uuid = '<확인한 UUID>';
SET @ci_expected_schema = '<확인한 DB 이름>';
SET @ci_actor = '<실제 적용자 로그인 ID>';
```

운영계는 `PRODUCTION`과 운영계에서 확인한 UUID·DB 이름을 사용한다. 예시 입력을 세션에 먼저 실행한 뒤 NULL이 남은 원본 파일을 실행하면 값이 다시 NULL로 바뀐다. **기존 NULL 입력 줄을 수정한 복사본을 실행한다.** 세션 변수는 그 연결에만 있으므로 새 연결에서는 다시 입력한다.

## 3. DDL 사전 확인

DDL의 `-- 1. PRE-CHECK`부터 `-- 2. APPLY` **직전까지** 실행한다. 여기에는 구조를 바꾸는 문장이 없다. 다음 결과를 모두 확인하고 보관한다.

| 확인 항목 | 통과 결과 |
| --- | --- |
| 서버 UUID / schema | 준비한 환경별 값과 일치 |
| `server_read_only`, `server_super_read_only` | 둘 다 `0` |
| `fk_checks` | `1` |
| 기존 부모 테이블 목록 | `tmd_mdl_scnrcasem`, `tcm_filel`, `tmd_usrm`, `tmd_custcpnym` 4개, 모두 InnoDB |
| 신규 이름/이전 tmm_ 이름 존재 조회 | **0행** |
| `precheck_decision` | `REVIEW_PARENT_TYPES_VERSION_PRIVILEGES_THEN_APPLY` |

긴 decision 문자열은 자동 확인 항목을 통과했다는 뜻이다. 이어서 DBA가 DDL §1의 `SHOW CREATE`와 주석에 따라 부모 키의 unsigned 타입·index, MySQL 버전, CHECK 강제 지원, 문자 정렬 규칙(collation), 권한·용량·백업을 확인한다. MySQL 8.4 등 별도 버전은 호환성을 따로 확인한다. 개발계 결과로 운영계 확인을 대신하지 않는다.

**STOP 또는 하나라도 불일치하면 다음 단계로 가지 않는다.** 값만 바꾸어 통과시키거나 기존 테이블을 삭제하지 않는다.

## 4. 테이블 생성과 정의 확인

1. DDL에서 `MANUAL_CREATE_BEGIN`과 `MANUAL_CREATE_END` **사이의 내용만** 선택한다. 바깥의 `/* ... */` 표식은 제외한다.
2. 19개 `CREATE TABLE`과 중간의 2개 `CREATE INDEX`를 파일 순서대로 실행한다. 첫 오류에 즉시 멈춘다.
3. DDL §4의 정의 조회를 실행한다. 이 시점의 행 수 조회에서는 **실행기도 0행**이다. 실행기 1행 기대값은 다음 단계에서 초기화를 저장한 뒤 적용한다.

| 확인 항목 | 이 단계의 통과 결과 |
| --- | --- |
| `installed_tables_must_be_19` | `19` |
| `innodb_tables_must_be_19` | `19` |
| 컬럼/CHECK/FK/index/`SHOW CREATE TABLE` | DDL 정의와 일치, CHECK의 ENFORCED는 모두 YES |
| 19개 테이블 행 수 | 모두 `0` |
| 업로드 index 조회 | `tmd_mdl_inptupldfiled_ix02` / `non_unique=1` / `prcs_id`; `..._ux01`은 없음 |

여기서 실패해도 앞서 생성된 테이블은 `ROLLBACK`으로 없어지지 않는다. 성공한 문장을 기록하고 실제 `SHOW CREATE`를 배포 담당자에게 전달한다. 전체 CREATE를 다시 실행하거나 이미 생성된 테이블을 삭제하지 않는다.

## 5. 실행기 STOPPED 초기화

실행기는 요청을 처리할 소비자의 현재 상태를 기록한다. 최초 1행만 만든다. 관련 접수/소비는 계속 중지한 상태로 둔다.

### 5-1. ROLLBACK 예행

1. 같은 연결에서 DDL §1의 환경 입력값이 유지되어 있는지 확인한다. 새 연결이면 네 입력 SET만 다시 실행한다. 테이블이 생겼으므로 최초 부재를 검사하는 §1 전체를 다시 실행하지 않는다.
2. `MANUAL_INITIALIZE_BEGIN`과 `MANUAL_INITIALIZE_END` 사이 내용을 바깥 주석 표식 없이 실행한다. 이번에는 안쪽 마지막 `ROLLBACK;`까지 실행한다.
3. 아래 결과를 확인한다.

| 출력 | 예행 중 기대 결과 |
| --- | --- |
| `existing_runner_rows_must_be_0` | `0` — 0이 아니면 INSERT 전 중단 |
| `runner_rows_must_be_1` | `1` |
| 상태 조회 | `exec_no=1`, `exec_stat_cd=STOPPED`; 실행·작업·승인 필드는 모두 NULL |

예행이 끝난 뒤 아래 조회 결과는 `0`이어야 한다. 예행 중 보인 1행은 저장되지 않았다.

```sql
SELECT COUNT(*) AS runner_rows_after_rehearsal
FROM tmd_mdl_inptexecm;
```

### 5-2. 실제 저장

같은 초기화 구간을 다시 실행하되 **마지막 `ROLLBACK;` 직전에서 멈춘다.** 위 결과가 모두 맞고 오류가 없으면 같은 연결에서 다음 한 문장만 실행한다.

```sql
COMMIT;
```

불일치·오류면 `COMMIT` 대신 `ROLLBACK;`을 실행하고 중단한다. 이미 `ROLLBACK`까지 실행했다면 뒤에 `COMMIT`을 추가해도 저장되지 않는다. 초기화 구간을 처음부터 다시 실행해야 한다.

새 연결에서 확인한다.

```sql
SELECT exec_no,exec_stat_cd,exec_id,bgn_dtm,end_dtm,prcs_id,try_tokn,
       aprv_exec_id,aprv_usr_logn_id,aprv_dtm
FROM tmd_mdl_inptexecm;
```

**정확히 1행**, `exec_no=1`, `STOPPED`, 나머지 조회 필드는 NULL이어야 한다. 이후 DDL §4 전체를 실행한다. 테이블 19개, 실행기 1행, 나머지 18개 0행 및 정의/index 일치를 확인하면 DDL 단계가 끝난다. **Catalog 등록 후에는 ‘나머지 18개 0행’을 다시 요구하지 않는다.**

## 6. Catalog DRAFT 등록

Catalog는 파일·시트·표·편집 범위를 설명하는 데이터다. 이번 등록은 `DRAFT_C_STRUCTURE`, 버전 `5`를 새 DRAFT로 만든다. 중간 v1~v4 SQL은 실행하지 않는다.

### 6-1. ROLLBACK 예행

1. 새 전용 연결에서 [등록 SQL](model-input-catalog-install-registration.sql) 실행 복사본의 환경 입력 4개를 채운다. 나머지 내장 데이터·모델/버전·marker·검증 변수는 바꾸지 않는다.
2. 파일을 처음부터 마지막 `ROLLBACK;`까지 실행한다. 첫 오류나 `STOP_AND_ROLLBACK`이면 즉시 ROLLBACK하고 중단한다. 오류 후 다음 문장을 계속 실행하지 않는다.
3. 다음 결과를 모두 확인한다.

| 출력 | 통과 결과 |
| --- | --- |
| 사전 `decision` | `PRECHECK_OK` |
| `existing_model_rows_must_be_0` | `0` |
| `files_must_be_4` / `sheets_must_be_70` | `4` / `70` |
| `tables_must_be_94` / `scopes_must_be_146` | `94` / `146` |
| `verified_must_be_1` | `1` |
| 마지막 `decision` | `VERIFIED_UNPUBLISHED` |

이 검증은 건수뿐 아니라 좌표·이름·상태·표 정의 전체·수정 범위를 비교한다. 마지막 `registered_template_id`는 **예행의 임시 ID**이므로 발행 입력에 사용하지 않는다.

예행 종료 뒤 다음 결과는 `0`이어야 한다. 실행기 STOPPED 1행은 그대로 보존된다.

```sql
SELECT COUNT(*) AS model_rows_after_rehearsal
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE';
```

### 6-2. 실제 등록 COMMIT

1. 같은 입력의 등록 SQL을 처음부터 다시 실행하되 **마지막 `ROLLBACK;` 직전에서 멈춘다.**
2. 위 통과 결과가 모두 같고 오류가 없으면 같은 연결에서 `COMMIT;`을 실행한다. 불일치면 `ROLLBACK;` 후 중단한다.
3. **새 연결**에서 아래 조회를 실행한다. 조회된 실제 `tmplt_id`를 이번 환경의 등록 ID로 기록한다. 예행 ID와 달라도 정상이다.

```sql
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn,del_yn
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE';
```

기대 결과는 정확히 1행, 모델 `DRAFT_C_STRUCTURE`, 버전 `5`, 상태 `DRAFT`, `aply_bgn_dtm=NULL`, `del_yn=N`이다. `tmplt_expn`은 등록/발행 SQL의 `@ci_marker` 문자열과 일치해야 한다. **SQL 파일이 발행 입력의 정본이며 marker를 임의로 고쳐 맞추지 않는다.**

이미 DRAFT가 저장됐다면 등록 파일을 다시 실행하지 않는다. 중복 등록은 차단된다. 다음 발행 단계로 간다.

## 7. 호환 코드 확인과 발행 입력

배포 담당자가 **이번 대상 환경**에 v5 지원 코드가 배포됐는지 확인한다. 확인 항목은 `navigationVisible` 지원, 숨긴 내부 표의 `lngMode` control 연결, 기존 편집 정책 지원이다. 배포 코드 SHA와 확인자를 기록한다. 저장소에 코드가 있거나 로컬 테스트가 통과했다는 사실만으로 대상 서버 배포 완료로 판단하지 않는다.

발행 SQL 실행 복사본의 환경 입력 4개를 채운 뒤, 추가로 다음 두 값을 지정한다.

```sql
SET @ci_expected_template_id = <6-2에서 새 연결로 확인한 실제 tmplt_id 숫자>;
SET @ci_reader_ready = 1;
```

`reader_ready=1`은 배포 담당자가 위 지원을 확인한 뒤에만 지정한다. 아직 확인하지 못했으면 `0`으로 두고 **발행 단계는 보류**한다. 운영계에 개발계 ID를 입력하지 않는다.

## 8. Catalog ACTIVE 발행

### 8-1. ROLLBACK 예행

새 전용 연결에서 입력을 채운 [발행 SQL](model-input-catalog-install-publication.sql)을 처음부터 마지막 `ROLLBACK;`까지 실행한다.

| 출력 | 통과 결과 |
| --- | --- |
| 사전 `decision` / `exact_draft_id` | `PRECHECK_OK` / 이 환경의 실제 등록 ID |
| Case/Draft/upload file/shared baseline 참조 잠금 조회 | 각각 **0행** |
| `files_must_be_4` / `sheets_must_be_70` | `4` / `70` |
| `tables_must_be_94` / `scopes_must_be_146` | `94` / `146` |
| `verified_must_be_1` | `1` |
| `published_must_be_1` / 마지막 `decision` | `1` / `VERIFIED_PUBLISHED` |
| 마지막 root 조회 | 등록 ID 그대로, 버전 5, ACTIVE, 적용 시각 값 있음 |

예행 중 ACTIVE로 보였더라도 마지막 ROLLBACK 이후에는 **다시 DRAFT**여야 한다. 새 연결에서 §6-2의 root 조회로 `DRAFT`, 적용 시각 NULL, ID/marker 불변을 확인한다.

### 8-2. 실제 발행 COMMIT

1. 같은 입력의 발행 SQL을 처음부터 다시 실행하되 **마지막 `ROLLBACK;` 직전에서 멈춘다.**
2. 위 결과가 전부 맞고 오류가 없으면 같은 연결에서 `COMMIT;`을 실행한다. 불일치면 `ROLLBACK;` 후 중단한다.
3. 새 연결에서 다음 최종 확인을 진행한다. 등록 SQL을 다시 실행하지 않는다.

## 9. 새 연결에서 최종 확인

서버 UUID/DB 이름을 §2의 조회로 다시 확인한 다음, §6-2의 root 조회를 실행한다. **실제 등록 ID 그대로 1행, v5 ACTIVE, 적용 시각 값 있음, marker 불변**이어야 한다. 이어서 다음 조회를 실행한다.

```sql
SELECT COUNT(*) AS active_templates_must_be_1
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_stat_cd='ACTIVE' AND del_yn='N';

SELECT t.tmplt_id,
 (SELECT COUNT(*) FROM tmd_mdl_inptfiled f WHERE f.tmplt_id=t.tmplt_id) AS files_must_be_4,
 (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id=t.tmplt_id) AS sheets_must_be_70,
 (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
  JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=t.tmplt_id) AS tables_must_be_94,
 (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
  JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
  WHERE f.tmplt_id=t.tmplt_id) AS scopes_must_be_146
FROM tmd_mdl_inpttmpltm t
WHERE t.mdl_spt_cd='DRAFT_C_STRUCTURE' AND t.tmplt_ver_no=5;

SELECT b.inpt_tbl_nm,
 COALESCE(JSON_UNQUOTE(JSON_EXTRACT(b.qry_json,'$.navigationVisible')),'true') AS navigation_visible,
 JSON_UNQUOTE(JSON_EXTRACT(b.qry_json,'$.controls[0].id')) AS first_control_id,
 JSON_UNQUOTE(JSON_EXTRACT(b.qry_json,'$.controls[0].sourceTableId')) AS control_source_table
FROM tmd_mdl_inpttbld b
JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id)
JOIN tmd_mdl_inpttmpltm t USING(tmplt_id)
WHERE t.mdl_spt_cd='DRAFT_C_STRUCTURE' AND t.tmplt_ver_no=5
 AND f.mdl_file_cd='DEMAND' AND s.sht_nm='LNG Demand'
ORDER BY b.tbl_ord;
```

| 확인 결과 | 기대값 |
| --- | --- |
| `active_templates_must_be_1` | `1` |
| 계층 건수 조회 | 실제 등록 ID 1행, `4 / 70 / 94 / 146` |
| LNG Demand 조회 1행째 | `LNG Demand Mode`, `navigation_visible=false`, control 2개 필드는 NULL |
| LNG Demand 조회 2행째 | `Annual LNG Demand Reference`, `navigation_visible=true`, `first_control_id=lngMode`, `control_source_table=DEMAND/LNG Demand/LNG Demand Mode` |

이 마지막 조회는 독립 연결에서 저장 상태를 확인하는 용도다. 전체 내용 비교를 수행한 등록·발행 SQL의 VERIFIED 결과를 대신하지 않는다.

## 10. 실패 시 중단 위치와 복구

| 발생 시점/현상 | 할 일 |
| --- | --- |
| 사전 확인 STOP, 부모/서버/schema 불일치, 테이블/기존 모델 존재 | 뒤 구간을 실행하지 않고 실제 결과를 배포 담당자에게 전달 |
| CREATE 중 오류/연결 끊김 | 쓰기 중지 유지, 성공한 문장과 실제 정의 확인. DDL 취소/전체 재실행/삭제 금지 |
| 초기화·등록·발행의 SQL 오류/검증 불일치 | 같은 연결에서 즉시 ROLLBACK 후 중단. 이어서 COMMIT하지 않음 |
| ROLLBACK까지 실행한 뒤 저장하려는 경우 | COMMIT만 추가하지 않음. 해당 transaction 구간을 처음부터 다시 실행 |
| COMMIT 응답 유실/연결 끊김으로 저장 여부가 불명확 | 새 연결에서 서버/schema와 실제 행·상태를 확인해 담당자 판단 전까지 재실행하지 않음 |
| Catalog COMMIT 후 문제 발견 | 접수·소비 비활성화, Catalog/Case/Draft/파일 보존. 담당자와 호환 코드 수정·새 버전 검토 |

기존 ACTIVE를 DRAFT로 되돌리거나 성공 이력·Case·파일을 삭제하지 않는다. 실행기 상태가 예상과 달라도 ACTIVE를 강제로 STOPPED로 바꾸지 않는다. 접수 재개와 소비자 활성화는 실제 상태와 배포 담당자 확인 뒤 별도로 진행한다.

## 11. 완료 기록과 운영계 반복

환경마다 아래 기록을 남긴다. 비밀값과 비공개 파일 경로·JSON 요청 본문은 기록하지 않는다.

| 항목 | 이번 환경의 기록 |
| --- | --- |
| 환경 / 작업 일시 / 적용자 | 입력 |
| 확인한 서버 UUID / DB 이름 | 입력 |
| 원본 SQL 3개 SHA-256 / 실행 복사본 변경 범위 | 입력 |
| 백업 확인 / DDL 정의 검증 결과 | 입력 |
| 실행기 초기화 예행·COMMIT / 새 연결 확인 | 입력 |
| 등록 예행·COMMIT / 실제 tmplt_id / marker | 입력 |
| 대상 환경 호환 코드 SHA / 확인자 | 입력 |
| 발행 예행·COMMIT / 새 연결 ACTIVE 1개·계층 결과 | 입력 |
| 이상 결과 / 중단·미확인 사항 | 입력 |

여기까지 통과하면 **DDL·Catalog 등록·발행의 DB 준비 완료**다. 실제 API/storage/권한·잠금/복구 검증은 별도다. 적용된 양식의 모든 표가 편집 가능한 것은 아니며 보류/INCOMPLETE/NOT_ALLOWED 정책은 유지된다.

BE는 해당 환경에서 격리된 시험 데이터로 구조/Annual+lngMode/업로드/승인된 편집/FINALIZE와 결과 파일을 확인하고 접수 재개를 결정한다. 비동기 요청은 최초 202나 HTTP 200만으로 완료를 판단하지 않고 `data.status=SUCCEEDED`와 결과 재조회까지 확인한다.

운영계는 개발계 결과와 동일 원본 SQL·배포 코드를 확인한 뒤 **§2부터 모든 환경별 사전 확인·생성·초기화·등록·발행·검증을 독립 수행**한다. 개발계 데이터·등록 ID·실행기 상태·COMMIT 결과를 운영계에 복사하지 않는다.
