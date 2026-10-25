# BCRegasification 행 편집 v7 등록·발행

## Document Control
- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED
- canonical for: Schema rollout — Regas 행 편집 개방
- owner: BE / DBA
- date: 2026-10-25
- source documents: `model-input-regas-rows-v7-policy.json`, `../scripts/build-model-input-regas-v7.py`
- depends on: `model-input-editor-v6-deployment-guide.md`

## 변경 범위
v6의 96표/171수정 범위를 그대로 복사하고 BCRegasification의 rowAdd, ADD_ROW/DELETE_ROW, 신규 행 CREATE 정책만 변경한다. 기존 식별값 SET/CLEAR와 RegasAdjustment 직접 행 편집 정책은 보존한다. 물리 schema 변경은 없다.

이미 발행된 v6 또는 기존 Case/Draft를 덮어쓰지 않는다. v7 발행만으로 기존 Case/Draft 정책은 바뀌지 않는다. 기존 대상의 재연결은 별도 범위이며 이 SQL에 포함하지 않는다.

## 적용
1. `python3 scripts/build-model-input-regas-v7.py --check`로 생성 결과를 확인한다.
2. `model-input-editor-v6-deployment-guide.md`의 환경 식별·writer 정지 확인·백업·전용 연결·예행·COMMIT 후 새 연결 확인 절차를 따른다. 버전과 파일은 아래 v7 전용 파일을 사용한다.
3. `model-input-regas-rows-v7-registration-draft.sql` 입력부에 실제 환경을 지정한다. 기존 ACTIVE가 v6이면 UPGRADE와 정확한 v6 ID/버전/marker를 사용한다. 새 버전은 7이다. 기본 ROLLBACK으로 예행하고 VERIFIED_UNPUBLISHED 확인 후 승인된 실제 실행에서만 COMMIT한다.
4. 등록 COMMIT 후 새 연결로 확인한 v7 DRAFT ID를 `model-input-regas-rows-v7-publication-draft.sql`에 지정한다. 호환 코드 배포 확인 후 reader_ready를 설정하고 VERIFIED_PUBLISHED를 확인한다. 기본 ROLLBACK이며 실제 COMMIT은 별도 승인 후 수행한다.
5. 새 정책에 연결된 격리 Draft에서 식별값 5개를 사용해 추가하고, 연결 monthly 행/국가별 합계/삭제/최종 파일을 확인한다. 기존 참조 수와 기존 정의 불변도 확인한다.

실제 MySQL 등록·발행 및 배포 API 검증은 이 변경에서 실행하지 않았다.

## 개발계 실행 커맨드 (사용자 직접 실행)

### 1. 실행 사본과 DB 접속

저장소 루트에서 실행한다. 아래 HOST/PORT/USER는 개발계 접속값으로 바꾸며 비밀번호는 프롬프트로 입력한다.

```bash
python3 scripts/build-model-input-regas-v7.py --check
mkdir -p temp/catalog-regas-v7-development
cp docs/model-input-regas-rows-v7-registration-draft.sql temp/catalog-regas-v7-development/register.sql
cp docs/model-input-regas-rows-v7-publication-draft.sql temp/catalog-regas-v7-development/publish.sql
mysql --host=HOST --port=PORT --user=USER --password --default-character-set=utf8mb4 midb
```

개발계 권한으로 기존 카탈로그 5개 테이블을 백업한다. 별도 터미널에서 실행한다.

```bash
mysqldump --host=HOST --port=PORT --user=USER --password --single-transaction --no-tablespaces --skip-lock-tables midb tmd_mdl_inpttmpltm tmd_mdl_inptfiled tmd_mdl_inptshtd tmd_mdl_inpttbld tmd_mdl_inptmodscopd > temp/catalog-regas-v7-development/catalog-before-v7.sql
```

접속한 DB 콘솔에서 사전 조회한다.

```sql
SELECT @@GLOBAL.server_uuid, DATABASE(), CURRENT_USER(), VERSION(), @@GLOBAL.read_only;
SELECT tmplt_id,tmplt_ver_no,tmplt_stat_cd,tmplt_expn,aply_bgn_dtm
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' ORDER BY tmplt_ver_no;
SELECT COUNT(*) AS unfinished FROM tmd_mdl_inptdrftprcsl WHERE prcs_stat_cd IN ('QUEUED','RUNNING');
ROLLBACK;
```

서버/schema가 개발계인지 확인한다. 진행 작업이 있으면 먼저 정상 종료를 기다린다. ACTIVE가 정확히 하나이고 버전이 7 미만이어야 한다. v7이 이미 있으면 반복 실행하지 말고 상태/marker를 확인한다.

### 2. 입력부 설정과 등록 예행

`register.sql`, `publish.sql`의 입력부를 실제 조회값으로 바꾼다. 두 파일의 아래 값은 동일하게 입력한다.

```sql
SET @ci_environment='DEVELOPMENT';
SET @ci_expected_server_uuid='사전 조회의 server_uuid';
SET @ci_expected_schema='midb';
SET @ci_actor='실제 적용자 로그인 ID';
SET @ci_mode='UPGRADE';
SET @ci_previous_template_id=실제_ACTIVE_ID;
SET @ci_previous_version=실제_ACTIVE_버전;
SET @ci_previous_marker='실제_ACTIVE_tmplt_expn_전체';
```

marker에 작은따옴표가 있으면 SQL 이스케이프를 적용한다. `@ci_version=7`, marker, hex 데이터, guard와 건수는 변경하지 않는다. schema가 없는 신규 환경은 이 UPGRADE 예제를 사용하지 말고 설치 절차부터 확인한다.

DB 콘솔에서 실행한다(저장소 루트에서 시작한 mysql 기준).

```sql
ROLLBACK;
SOURCE temp/catalog-regas-v7-development/register.sql;
```

`PRECHECK_OK`, `verified_must_be_1=1`, `VERIFIED_UNPUBLISHED`를 모두 확인한다. 기본 마지막 ROLLBACK 때문에 예행의 template ID는 저장되지 않는다. 오류가 있으면 즉시 `ROLLBACK;`하고 중단한다. `--force`는 쓰지 않는다.

### 3. 실제 등록

등록 사본의 **실행문 `ROLLBACK;` 한 줄만 주석 처리**한다. 같은 DB 콘솔에서 처음부터 다시 실행한다.

```sql
ROLLBACK;
SOURCE temp/catalog-regas-v7-development/register.sql;
SELECT @ci_ok AS precheck,@ci_verified AS verified,@ci_new AS registered_template_id;
```

같은 연결에서 precheck=1, verified=1 및 VERIFIED_UNPUBLISHED를 확인한 경우에만 실행한다.

```sql
COMMIT;
```

새 연결에서 v7 DRAFT의 실제 ID와 marker를 확인한다. 예행 ID를 재사용하지 않는다.

```sql
SELECT tmplt_id,tmplt_ver_no,tmplt_stat_cd,tmplt_expn,aply_bgn_dtm
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_ver_no=7;
ROLLBACK;
```

### 4. 발행 예행과 실제 발행

`publish.sql`의 추가 입력부에 실제 등록 ID와 호환 코드 확인값을 넣는다.

```sql
SET @ci_expected_template_id=실제_v7_DRAFT_ID;
SET @ci_reader_ready=1;
```

reader_ready=1은 기존 Regas master 행 추가·삭제와 monthly 자동 연동을 지원하는 코드가 개발계에 배포됐음을 확인한 경우에만 설정한다. 새 endpoint나 schema는 필요하지 않지만 오래된 코드에서는 정책만 열지 않는다.

```sql
ROLLBACK;
SOURCE temp/catalog-regas-v7-development/publish.sql;
```

`PRECHECK_OK`, verified=1, retired_rows=1, activated_rows=1, published=1, `VERIFIED_PUBLISHED`를 확인한다. 예행 후에는 v7 DRAFT와 이전 ACTIVE가 그대로여야 한다.

발행 사본의 실행문 `ROLLBACK;` 한 줄을 주석 처리한 뒤 처음부터 다시 실행한다.

```sql
ROLLBACK;
SOURCE temp/catalog-regas-v7-development/publish.sql;
SELECT @ci_ok AS precheck,@ci_verified AS verified,@ci_published AS published,@ci_new AS template_id;
```

위 값이 모두 1이고 VERIFIED_PUBLISHED이면 같은 연결에서 `COMMIT;`한다. 하나라도 다르면 `ROLLBACK;`한다.

### 5. 새 연결에서 최종 확인

```sql
SELECT tmplt_id,tmplt_ver_no,tmplt_stat_cd,tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' ORDER BY tmplt_ver_no;
SELECT COUNT(*) AS active_must_be_1 FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_stat_cd='ACTIVE' AND del_yn='N';
```

v7 ACTIVE 1개와 이전 버전 DEPRECATED를 확인한다. 새 v7 정책에 연결한 격리 Draft의 BCRegasification `editing.capabilities`에서 ADD_ROW/DELETE_ROW=READY인지 확인하고 추가·삭제 및 RegasAdjustment 연동을 검증한다. 기존 Case/Draft는 자동 재연결되지 않으므로 기존 화면이 즉시 바뀌지 않을 수 있다.
