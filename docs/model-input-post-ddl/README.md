# DDL 적용 후 실행 순서

아래 번호순 묶음은 **v5 최초 설치용**이다. v6 직접 설치/기존 ACTIVE 교체는 [v6 개발계·운영계 적용 안내](../model-input-editor-v6-deployment-guide.md)의 별도 등록·발행 SQL을 사용한다. v6라는 이유로 기존 runner 초기화나 이 묶음의 v5 등록/발행을 반복하지 않는다.

## Document Control

- status: Review / 수동 실행용
- canonical for: Reference only — 공통 세션을 사용하는 번호순 실행본 안내
- owner: BE / DBA
- date / last synced: 2026-10-22
- source documents: 이 폴더의 SQL 4개, [전체 실행 안내서](../model-input-first-deployment-runbook.md)
- open questions: 실제 DB 적용·권한·잠금·COMMIT/API 결과는 미확인

**DDL 19개 테이블 생성이 끝났고 관련 접수·runner·worker가 중지된 최초 환경**에서 사용한다. 기존 실행기 상태를 변경하거나 기존 Catalog를 교체하는 SQL은 아니다. 실제 DB에는 아직 실행하지 않았다.

| 순서 | 파일 | 수정할 입력 | 성공 결과 |
| --- | --- | --- | --- |
| 00 | [공통 설정](00-session.sql) | 환경·서버 UUID·DB 이름·적용자 4개를 한 번 입력 | `SESSION_READY` |
| 01 | [실행기 초기화](01-runner-initialize.sql) | 없음 | `VERIFIED_RUNNER_INITIALIZED`, STOPPED 1행 |
| 02 | [Catalog 등록](02-catalog-register.sql) | 없음 | `VERIFIED_UNPUBLISHED`, 4/70/94/146 |
| 03 | [Catalog 발행](03-catalog-publish.sql) | 실제 등록 ID, 호환 코드 확인 | `VERIFIED_PUBLISHED`, ACTIVE 1개 |

**00부터 같은 DB 콘솔/연결에서 진행한다.** 01~03은 공통 입력값을 재사용한다. 새 연결·재접속·환경 변경 시 00을 다시 입력/실행하되, 이미 COMMIT한 01/02를 다시 실행하지 않는다. 오류나 STOP이면 같은 연결에서 `ROLLBACK;` 후 중단하고 뒤 파일을 실행하지 않는다.

## 00. 공통 입력 한 번

`00-session.sql`의 첫 SELECT만 먼저 실행한다.

```sql
SELECT @@GLOBAL.server_uuid AS server_uuid, DATABASE() AS schema_name;
```

접속 대상이 적용할 환경의 쓰기 DB인지 확인하고, 파일의 NULL 4개를 실제 값으로 바꾼다.

```sql
SET @ci_environment='DEVELOPMENT'; -- 운영계는 'PRODUCTION'.
SET @ci_expected_server_uuid='<확인한 server_uuid>';
SET @ci_expected_schema='<확인한 schema_name>';
SET @ci_actor='<실제 적용자 로그인 ID>';
```

예시의 `<...>`는 실제 값으로 교체한다. `schema_name=NULL`이면 DB를 선택한 뒤 다시 조회한다. 적용자는 영문/숫자/`._@-` 1~50자다. 수정한 00 전체를 실행하고 `session_decision=SESSION_READY`를 확인한다. 이후 01/02에는 입력값을 다시 넣지 않는다.

## 01~03 공통 실행 방법

세 파일은 모두 기본 마지막 SQL이 `ROLLBACK;`이다. **끝까지 실행하면 예행이고 저장되지 않는다.**

1. 예행: 해당 파일 전체를 실행하여 성공 결과를 확인한다. ROLLBACK 후 아래 단계별 확인 조회로 기존 상태가 유지되는지 확인한다.
2. 실제 저장: 파일을 다시 실행하되 **마지막 `ROLLBACK;` 문장 직전에서 멈춘다.**
3. 오류가 없고 해당 파일의 성공 결과가 맞으면, **같은 연결**에서 `COMMIT;`을 실행한다. 불일치면 `ROLLBACK;` 후 중단한다.
4. 단계별 확인 조회를 통과한 뒤 다음 파일로 간다.

이미 ROLLBACK까지 실행한 뒤 COMMIT만 추가해도 저장되지 않는다. 실제 저장하려면 해당 파일을 다시 실행해야 한다. DB 클라이언트의 ‘오류 후 계속 실행’ 옵션은 끈다.

## 01. 실행기 초기화

`01-runner-initialize.sql`을 위 공통 방법으로 실행한다.

- `existing_runner_rows_must_be_0=0`, 사전 `decision=PRECHECK_OK`
- `runner_verified_must_be_1=1`, 마지막 `decision=VERIFIED_RUNNER_INITIALIZED`
- `exec_no=1`, 상태 `STOPPED`, 실행/작업/승인 필드는 모두 NULL

예행 ROLLBACK 후 다음 조회는 0행, 실제 COMMIT 후에는 위 조건의 1행이어야 한다.

```sql
SELECT exec_no,exec_stat_cd,exec_id,bgn_dtm,end_dtm,prcs_id,try_tokn,
       aprv_exec_id,aprv_usr_logn_id,aprv_dtm
FROM tmd_mdl_inptexecm;
```

기존 행이 보이면 초기화 SQL로 덮어쓰지 않는다. 특히 ACTIVE/RECOVERING을 강제로 STOPPED로 변경하지 않는다.

## 02. Catalog 등록

`02-catalog-register.sql`에는 환경 입력 수정이 필요 없다. 위 공통 방법으로 실행한다.

- 사전 `decision=PRECHECK_OK`, `existing_model_rows_must_be_0=0`
- `files_must_be_4=4`, `sheets_must_be_70=70`, `tables_must_be_94=94`, `scopes_must_be_146=146`
- `verified_must_be_1=1`, 마지막 `decision=VERIFIED_UNPUBLISHED`

예행 ROLLBACK 후 아래 조회는 0행이다. 실제 COMMIT 후에는 **DRAFT 1행, 버전 5, 적용 시각 NULL, 삭제 N**이어야 한다. 실제 저장된 `tmplt_id`를 기록한다. 예행에서 출력된 임시 ID는 사용하지 않는다. marker(`tmplt_expn`)는 실행본의 `@ci_marker`와 일치해야 한다.

```sql
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn,del_yn
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE';
```

## 03. Catalog 발행

배포 담당자가 대상 환경에 v5 지원 코드(navigationVisible, 내부 lngMode control, 기존 편집 정책)가 배포됐는지 확인한 뒤 진행한다. 아직 확인하지 못했으면 이 단계는 보류한다.

`03-catalog-publish.sql`의 입력 2개만 수정한다.

```sql
SET @ci_expected_template_id=<02 COMMIT 후 조회한 실제 tmplt_id 숫자>;
SET @ci_reader_ready=1; -- v5 지원 코드 배포 확인 완료 시에만 1.
```

위 공통 방법으로 실행하고 다음을 확인한다.

- 사전 `decision=PRECHECK_OK`, `exact_draft_id`가 실제 등록 ID와 일치
- 건수 `4/70/94/146`, `verified_must_be_1=1`
- `published_must_be_1=1`, 마지막 `decision=VERIFIED_PUBLISHED`

예행 ROLLBACK 후 §02의 조회 결과는 다시 DRAFT/적용 시각 NULL이어야 한다. 실제 COMMIT 후에는 **같은 ID의 ACTIVE 1행, 적용 시각 있음, marker 불변**이어야 한다.

최종적으로 별도 확인 연결에서 다음 값이 1인지 확인한다. 그 뒤 [전체 안내서 §9](../model-input-first-deployment-runbook.md#9-새-연결에서-최종-확인)의 계층 건수·LNG control 조회까지 확인한다.

```sql
SELECT COUNT(*) AS active_templates_must_be_1
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_stat_cd='ACTIVE' AND del_yn='N';
```

새 확인 연결의 값은 원래 작업 연결의 세션 변수와 공유되지 않는다. COMMIT 응답이 유실되면 재실행 전에 실제 저장 상태부터 확인한다. 이후 접수 재개·소비자 활성화·API/storage 검증은 배포 담당자와 별도로 진행한다.
