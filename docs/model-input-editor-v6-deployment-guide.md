# Model Input v6 개발계·운영계 등록과 발행

## Document Control

- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED
- canonical for: Schema rollout — v6 SQL 적용 순서와 환경별 확인
- owner: BE / DBA / 배포 담당자
- date / last synced: 2026-10-23
- source documents:
  - `model-input-editor-remaining-v6-registration-draft.sql`
  - `model-input-editor-remaining-v6-publication-draft.sql`
  - `../scripts/build-model-input-catalog-install.py` (`--editor-v6`)
- depends on: 업무 정본 §3.3·규칙45/67, 기존 Model Input schema와 runner 설치
- execution record: `work-notes/2026/2026-10-19-model-input-excel-editor.md`
- supersedes: 기존 v5 최초 설치 안내를 대체하지 않음
- open questions: 실제 양 환경 schema·현재 ACTIVE/v6 등록 여부·배포 SHA·권한·MySQL 예행/COMMIT/API 결과 미확인

## 1. 적용할 파일과 모드

개발계에서 확인한 뒤 운영계에서 같은 SQL 원본과 호환 코드를 사용한다. 환경별 서버 UUID·schema·템플릿 ID·기존 marker·실행 결과는 각각 확인한다. 개발계 ID를 운영계에 복사하지 않는다.

| 순서 | 파일 | 결과 |
| --- | --- | --- |
| 1 | [v6 등록 SQL](model-input-editor-remaining-v6-registration-draft.sql) | 새 v6 DRAFT와 정의 생성, `VERIFIED_UNPUBLISHED` |
| 2 | 해당 환경의 최신 v6 호환 코드 확인 | 발행 SQL의 `@ci_reader_ready=1` 설정 근거 |
| 3 | [v6 발행 SQL](model-input-editor-remaining-v6-publication-draft.sql) | 검증한 v6가 유일한 ACTIVE, `VERIFIED_PUBLISHED` |
| 4 | 새 연결 확인 후 기능 활성화·격리된 시험 | 환경별 실제 API/파일 결과 확인 |

각 SQL은 전용 연결의 별도 transaction(한 묶음)이다. 기본 마지막 `ROLLBACK`은 그 묶음을 취소한다. 등록 예행에서 얻은 ID는 실제 저장한 ID가 아니므로 발행에 사용하지 않는다. SQL 전체 내용·marker·내장 데이터는 바꾸지 않고 입력부만 채운다.

| `@ci_mode` | 사용할 환경 | 이전 ACTIVE 입력 |
| --- | --- | --- |
| `FIRST_INSTALL` | schema/runner는 준비됐고 `DRAFT_C_STRUCTURE` 등록 이력이 0개 | 이전 ID·버전·marker 모두 NULL |
| `UPGRADE` | 같은 모델의 ACTIVE가 정확히 1개이고 버전이 6 미만 | 해당 환경의 정확한 ID·버전·`tmplt_expn` 전체 |

모드는 등록과 발행에서 동일하게 지정한다. FIRST_INSTALL 발행 시점에는 등록 COMMIT을 마친 v6 DRAFT 한 개만 있어야 한다. UPGRADE는 발행 순간에도 지정한 이전 양식만 ACTIVE여야 한다. 삭제 표시 이력도 버전 재사용의 근거로 삼지 않는다.

- 필요한 schema가 없으면 [최초 배포 안내](model-input-first-deployment-runbook.md) §2~5의 schema/runner 준비만 별도 승인으로 먼저 수행한다. v6를 바로 사용할 환경에서는 그 문서의 v5 등록·발행 단계로 이어가지 않는다.
- 기존 schema/runner가 있으면 CREATE·runner 초기화를 반복하지 않는다. 기존 모델이 있는데 ACTIVE가 0개/복수인 상태는 FIRST_INSTALL로 우회하지 않고 원인을 확인한다.
- 기존 v6가 있으면 먼저 실제 ID/상태/marker/전체 정의를 비교한다. 같은 최신 DRAFT이면 등록을 반복하지 않고 발행 입력을 검토한다. 예전 95표 v6나 다른 marker의 DRAFT이면 덮어쓰지 않고 별도 정리/버전 계획을 승인받는다. 이미 발행/참조된 v6의 정의를 수정하거나 삭제하지 않는다.
- 기존 `model-input-catalog-install-*.sql` 및 `model-input-post-ddl/02~03`은 **v5 최초 설치용**이다. v6 교체에 버전 숫자만 바꾸어 재사용하지 않는다.

## 2. 해당 환경에서 먼저 확인할 내용

배포 담당자가 관련 catalog writer·API 접수·runner/worker가 쓰지 않음을 확인한다. 기능 플래그 OFF나 runner의 한 시점 상태만으로 이전 작업 종료를 추정하지 않는다. 진행 중 요청/잠금이 있으면 기존 진단·종료 확인 절차를 따르며 여기서 강제 종료나 상태 초기화를 하지 않는다. 백업과 환경별 쓰기 권한, MySQL/InnoDB/FK·SQL 모드를 기존 schema 설치 기록과 대조한다.

아래는 **대상 환경 DB 콘솔의 읽기 전용 사전 확인**이다. 이 문서 작성 과정에서는 실행하지 않았다.

```sql
SELECT @@GLOBAL.server_uuid AS server_uuid, DATABASE() AS schema_name,
       VERSION() AS mysql_version, @@GLOBAL.read_only AS read_only,
       @@GLOBAL.super_read_only AS super_read_only,
       @@SESSION.foreign_key_checks AS fk_checks, @@SESSION.sql_mode AS sql_mode;
SELECT tmplt_id, mdl_spt_cd, tmplt_ver_no, tmplt_ver_nm,
       tmplt_stat_cd, aply_bgn_dtm, tmplt_expn, del_yn
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE'
ORDER BY tmplt_ver_no;
```

확인한 UUID/schema가 의도한 환경과 일치하는지 대조한 뒤 실행 복사본의 입력부를 채운다. 비밀번호·접속 토큰을 파일에 넣지 않는다.

| 공통 입력(두 SQL 각각) | 값 |
| --- | --- |
| `@ci_environment` | 개발계 `DEVELOPMENT`, 운영계 `PRODUCTION` |
| `@ci_expected_server_uuid`, `@ci_expected_schema` | 위 조회로 확인하고 승인한 해당 환경의 값 |
| `@ci_actor` | 실제 적용자 로그인 ID, 영문/숫자/`._@-` 1~50자 |
| `@ci_mode` | 위 두 모드 중 하나 |
| `@ci_previous_template_id`, `@ci_previous_version`, `@ci_previous_marker` | UPGRADE의 실제 이전 ACTIVE, FIRST_INSTALL은 NULL |

UPGRADE 입력은 v5라고 가정하거나 testdb ID를 쓰지 않는다. 실제 이전 양식과 배포 계획이 일치하는지 확인한다. marker에 작은따옴표가 있다면 SQL 문자열 규칙에 따라 이스케이프한다.

## 3. 등록 예행과 실제 저장

1. 등록 SQL의 기본 `ROLLBACK;`까지 실행해 예행한다. SQL 오류/`STOP_AND_ROLLBACK`/검증 불일치가 나오면 같은 연결에서 즉시 ROLLBACK하고 중단한다. 오류를 무시하거나 `--force`로 계속하지 않는다.
2. `PRECHECK_OK`, 전체 계층/정의의 `verified_must_be_1=1`, 마지막 `VERIFIED_UNPUBLISHED`를 확인한다. 건수만 같아도 성공으로 보지 않으며 SQL은 전체 metadata·qry bytes·수정 범위까지 비교한다.
3. 실제 저장이 승인되면 같은 입력으로 처음부터 재실행하고 마지막 ROLLBACK 직전에서 멈춘다. 위 성공값을 다시 확인한 경우에만 마지막 ROLLBACK 대신 **같은 연결에서 COMMIT**한다. 이미 ROLLBACK한 뒤 COMMIT만 추가하지 않는다.
4. 새 연결에서 §2의 모델 조회로 실제 v6 ID, 버전6, `DRAFT`, `aply_bgn_dtm=NULL`, 등록 SQL의 정확한 marker/이름을 확인한다. 이 실제 ID를 발행 입력으로 기록한다. COMMIT 응답이 유실되면 저장 상태부터 확인하며 등록 SQL을 반복하지 않는다.

SQL의 기대 건수·marker는 생성 파일이 정본이다. 현재 정의는 4파일/70시트/96표/171개 수정 범위이며, 최신 PMI Selected 숨김 표와 연결 행 정책·BCRegas 필수0/A:R·Demand 구조 고정·Pipeline 읽기 표를 포함한다. 예전 95표 v6의 ID나 hash를 현재 결과로 취급하지 않는다.

## 4. 호환 코드와 발행

배포 담당자는 **해당 환경**의 이미지/배포 SHA가 최신 v6 정의를 지원하는지 확인한다. BCRegas 용량 연결 계산·공유 MAX 수식 보존, PMI User-Adjusted 추가 시 Base/Selected 동시 생성·파생 행 이력 복원, Pipeline 본문13행/숨김 표, Demand 고정 구조와 기존 편집 정책을 지원해야 한다. 저장소 커밋이나 로컬 검사 통과는 서버 배포 완료 증거가 아니다.

발행 SQL의 공통 입력을 새 연결에서 다시 지정하고 다음 두 입력을 추가한다.

| 발행 입력 | 값 |
| --- | --- |
| `@ci_expected_template_id` | §3의 실제 등록 COMMIT 후 확인한 이 환경의 v6 DRAFT ID |
| `@ci_reader_ready` | 호환 코드 배포 확인 뒤 1, 미확인이면 기본0 유지 |

1. 발행 SQL을 기본 ROLLBACK까지 예행한다. 대상 DRAFT는 아직 Case/Draft/업로드/공유 기준에서 참조되지 않아야 한다. marker/전체 manifest/기존 ACTIVE가 다르면 멈춘다.
2. `PRECHECK_OK`, `verified_must_be_1=1`, `published_must_be_1=1`, `VERIFIED_PUBLISHED`를 모두 확인한다. `activated_rows=1`, `retired_rows`는 UPGRADE에서1/FIRST_INSTALL에서0이어야 한다. 두 rows 값은 클라이언트의 affected rows가 아니라 같은 트랜잭션에서 ID·모델·버전·상태·시각·marker·적용자가 일치하는 행을 조회한 검증 결과다. 중간 조회에 영향을 받는 `ROW_COUNT()`를 사용하지 않는다. 이 값과 `@ci_ok`, `@ci_verified`, `@ci_new`는 계산값이므로 수동으로 바꾸지 않는다.

   하나라도 불일치하면 같은 연결에서 `ROLLBACK;` 후 중단한다. `1 row affected` 로그만으로 성공 처리하거나 검증값을 1로 바꾸지 않는다. 수정된 v6 SQL은 `CAST(... AS BINARY)`로 바이트 비교를 유지한다. 이전 SQL 실행이 실패했다면 해당 트랜잭션의 ROLLBACK을 확인한 뒤 환경/현재 ACTIVE/v6 DRAFT 상태부터 다시 확인한다.
3. UPGRADE는 같은 transaction에서 지정한 이전 ACTIVE만 DEPRECATED로 전환한 다음 v6를 ACTIVE로 만든다. 이전 양식의 적용 시각과 Case/Draft/업로드/공유 기준 연결 수는 보존한다. FIRST_INSTALL은 이전 양식을 변경하지 않는다. 예행 ROLLBACK 뒤에는 v6 DRAFT/이전 ACTIVE 상태로 돌아가야 한다.
4. 실제 발행이 승인되면 처음부터 재실행하고 동일 성공값 확인 뒤 마지막 ROLLBACK 대신 같은 연결에서 COMMIT한다. 새 연결에서 아래를 확인한다.

```sql
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, aply_bgn_dtm, tmplt_expn
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE'
ORDER BY tmplt_ver_no;
SELECT COUNT(*) AS active_templates_must_be_1
FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_stat_cd='ACTIVE' AND del_yn='N';
```

v6의 실제 ID/marker가 발행 입력과 같고 ACTIVE가1개여야 한다. UPGRADE의 이전 ID는 DEPRECATED, 적용 시각은 기존 값이어야 한다. 위 조회는 SQL의 전체 정의 검증을 대신하지 않는다. 등록과 발행의 ID/marker, 이전 참조 수, SQL 파일 SHA-256, 성공 출력·COMMIT/새 연결 확인 결과를 환경별로 남긴다.

## 5. 기능 활성화·보존·실패 처리

최근 dev/prod 설정은 `upload`, `shared-baseline`, `web-runner`, `change-worker`, `finalization`이 모두 `${MODEL_TIMEOUT_DRAFT_ENABLED:false}`를 사용한다. 값이 없으면 기능은 OFF다. SQL 발행과 별개로 담당자가 해당 환경의 실효 설정을 확인하고, DB/코드 준비와 작업 종료 확인 후 **`MODEL_TIMEOUT_DRAFT_ENABLED=true`**로 활성화한다. 다른 설정 경로의 override도 확인한다. source-key `DRAFT_C_STRUCTURE_V1`은 template version 숫자와 별개이므로 v6라는 이유로 바꾸지 않는다.

격리된 시험 데이터로 새 Case/새 업로드의 v6 선택·구조 조회·승인된 변경·최종저장을 확인한다. CHANGE/FINALIZE는 202 수신만으로 성공으로 판단하지 않고 terminal `SUCCEEDED`/결과 revision 및 파일 재조회를 확인한다. PMI 연결 행, BCRegas 기존 용량 수정과 연도 추가의 서로 다른 계산/보존 범위를 확인한다. 보류/INCOMPLETE 정책과 native Excel/외부 모델 실행의 별도 검증 한계는 유지한다.

기존 Case/Draft/진행 중 업로드는 고정된 이전 양식을 계속 사용한다. v6 발행은 기존 연결을 수정하지 않으며 기존 Case 재연결은 별도 승인·SQL 범위다. 실패 시 COMMIT 전에는 같은 연결에서 ROLLBACK하고 중단한다. COMMIT 이후 문제는 접수/소비를 비활성화하고 양식·이력·Case·파일을 보존해 조사한다. 이전 양식 재활성화, v6 삭제/덮어쓰기, runner 강제 초기화를 복구 명령으로 제공하지 않는다.

운영계는 개발계 결과를 복사하지 않고 §2부터 사전 확인·모드 선택·예행·실제 저장·배포/실효 설정·API 결과를 독립 확인한다. 현재 산출물은 수동 예행용이며 실제 양 환경 적용/발행/배포는 수행하지 않았다.
