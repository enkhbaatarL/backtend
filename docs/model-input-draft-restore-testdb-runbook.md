# Draft 원본 복원 — testdb 웹 서버 배포·검증 절차

## Document Control

- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED
- canonical for: Reference only — testdb 웹 배포·검증 순서
- owner: BE / DBA / FE
- date: 2026-10-28
- depends on:
  - [API 계약 §11](feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#draft-baseline-comparison-restore-proposal-20261027)
  - [증분 DDL](model-input-draft-restore-v3-ddl-draft.sql) — 변경/검증/rollback SQL 정본
- source documents: `InputDraftWebConfiguration`, `InputDraftWebTasks`, `ModelInputDraftService`, `application.yml`, `application-dev.yml`, `application-prod.yml`
- supersedes: 없음
- open questions: 실제 testdb schema·활성 profile·배포 image/commit·고정 catalog 정책·서버 자원은 적용 직전 확인

이 문서는 실행 절차이며 API/SQL 계약을 새로 정의하지 않는다. 실제 DB 적용·배포·서버 검증을 수행한 기록도 아니다. 구현의 로컬 검증은 [구현 작업 노트](work-notes/2026/2026-10-27-draft-comparison-restore-api-design.md), 이 문서의 작성 검증은 [문서 작업 노트](work-notes/2026/2026-10-28-draft-restore-testdb-runbook.md)를 참조한다.

## 1. 적용 범위와 순서

**배포본 확정 → 복원 OFF로 모든 웹 서버 선배포 → DDL pre-check → ALTER → verify → 복원 ON으로 웹 서버 설정 반영 → API 테스트** 순서다.

- 기존 Draft·웹 러너·최종저장용 schema가 설치되어 있고 v2 저장을 사용 중인 testdb의 증분 배포에 적용한다. 이전 schema라면 이 SQL로 건너뛰지 않는다.
- 별도 worker 프로세스/Pod/supervisor는 시작하지 않는다. 웹 서버의 `InputDraftWebRunner`가 CHANGE·업로드·FINALIZE를 처리한다. worker 코드 삭제는 이번 범위가 아니다.
- `mi.model-input.change-worker.enabled`는 웹의 CHANGE 접수/실행에도 필요하다. 별도 worker 미사용을 이유로 끄지 않는다.
- 새 테이블·열·인덱스·데이터 backfill·catalog 발행·기존 Case/Draft 참조 재연결은 이번 증분 DDL에 포함하지 않는다. 기존 install SQL이나 다른 Snapshot/catalog SQL을 함께 실행하지 않는다.

## 2. 환경변수

설정 의미와 기본값의 정본은 [API §11.8](feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#118-설정과-수동-활성화)이다. 아래는 이번 배포 단계별 값이다.

| 환경변수 | 호환 코드 선배포 / DDL 적용 중 | DDL 검증 후 테스트 | 확인점 |
| --- | --- | --- | --- |
| `MODEL_INPUT_DRAFT_ENABLED` | `true` 유지 | `true` | 현재 dev/prod profile에서 upload/shared-baseline/web-runner/change-worker/finalization에 공통 적용 |
| `MODEL_INPUT_RESTORE_ENABLED` | `false` | `true` | 새 RESTORE 명령 접수와 READY 판정 |
| `MODEL_INPUT_RESTORE_PREVIEW_ENABLED` | `false` | `true` | preview 및 RESTORE 접수의 공통 전제 |
| `MODEL_INPUT_CHANGE_CURSOR_SECRET` | Secret에서 주입 | 같은 값 유지 | UTF-8 32바이트 이상, 모든 웹 인스턴스에 동일한 값 |
| `MODEL_INPUT_RESTORE_READ_MAX_CONCURRENT` | `1` | `1`로 시작 | 변경 조회/preview가 공유하는 JVM별 동시 실행 수 |
| `MODEL_INPUT_RESTORE_READ_TIMEOUT_MS` | `30000` | `30000`으로 시작 | 조회/preview 처리 기한 |
| `MODEL_INPUT_RESTORE_READ_SCRATCH_BYTES` | `268435456` | 같은 값으로 시작 | 요청별 임시파일 합계 256 MiB |

`MODEL_INPUT_DRAFT_ENABLED=false`는 복원만 끄는 설정이 아니다. 이번 점검에서는 복원 두 변수만 OFF/ON으로 바꾼다. 표의 공통 변수 매핑은 실제 활성 profile의 effective property(최종 적용값)로 확인한다. 별도 override가 있다면 파일 기본값보다 그것을 우선 확인한다. testdb라는 이름의 Spring profile이 있다고 가정하지 않는다.

서명키 실제 값은 저장소·문서·API 응답·로그에 기록하지 않는다. 준비한 Secret의 참조/version과 모든 웹의 일치 여부만 확인한다. 키를 교체하면 기존 cursor는 무효이므로 첫 페이지부터 다시 조회한다.

설정 변경은 웹 서버 재시작/재배포로 반영한다. 혼합 설정으로 요청 결과가 달라지지 않도록 전체 rollout 완료 후 테스트한다. 웹 실행에 `mi.model-input.worker-process=true`나 standalone worker 시작 명령을 추가하지 않는다.

## 3. 배포 전 준비

1. 복원 구현의 신규 파일·설정·DDL이 포함된 배포 commit/image를 확정한다. 공유 checkout의 다른 미커밋 작업을 무조건 포함하지 않는다. 모든 웹 서버에서 같은 배포본인지 확인할 식별자를 남긴다.
2. 그 배포본으로 정상 빌드와 관련 `-PforceTest` 테스트를 실행한다. bare `build`/`test` 성공은 이 저장소에서 테스트 실행 증거가 아니다. 이전 격리 검증의 제외 파일/기존 실패를 현재 배포본에서 확인하며, 당시 우회 설정을 그대로 배포 검증 완료로 취급하지 않는다.
3. DBA가 실제 testdb 연결과 schema, 적용 전 CHECK 정의를 확인할 준비를 한다. DDL은 implicit commit(자동 확정)과 metadata lock(구조 변경 잠금)이 있으므로 작업 시간·락 영향을 확인한다.
4. 인증 가능한 테스트 계정과 원본을 보존한 테스트용 Case/Draft를 준비한다. 원본 파일·현재 성공 값·행 ID/fieldKey·revision을 기록한다. 실제 중요 Case를 시험용으로 수정하지 않는다.
5. 일반 행, 삭제 가능한 원본 기간 열, PMI·Regas 연결 행의 표를 준비한다. `editing.capabilities.DELETE_ROW` 또는 표/필드의 `DELETE_COLUMN`이 READY인지 확인한다. catalog를 새로 발행해도 기존 Draft의 고정 정책은 자동 변경되지 않는다. 정책이 부족하면 해당 테스트를 중단하고 별도 catalog 절차로 해결한다.
6. 실제 JVM 임시 디렉터리의 쓰기 권한·여유 공간을 확인한다. 256 MiB는 요청당 한도이고 JVM 기본 메모리/다른 작업/웹 복제본의 디스크 사용량을 포함한 총 용량이 아니다. gateway/client timeout도 새 조회의 30초 처리 기한과 실제 응답을 전달할 수 있는지 확인한다.

## 4. 호환 코드 선배포 — 복원 OFF

1. §2의 OFF 단계 설정을 적용하고 **모든 웹 서버**를 v2/v3 reader 지원 배포본으로 교체한다. 별도 worker 배포는 없다.
2. 기존 Draft 구조·표 조회 및 일반 SET 저장이 동작하는지 확인한다. 저장은 202 접수 뒤 응답의 `statusEndpoint`로 종료 결과를 확인한다.
3. 웹 로그의 `Model input consumer started`와 `operations=UPLOAD,CHANGE,FINALIZE`를 확인한다. 같은 DB 큐의 활성 소비자는 하나이고 다른 웹 인스턴스의 `QUEUE_LOCK_HELD` 대기는 가능하다. 로그의 database·host·instance와 배포 대상을 대조한다.
4. `PREVIOUS_CONSUMER_NOT_STOPPED` 또는 `runner held`가 있으면 DDL/새 기능 활성화를 이어가지 않는다. 로그만 보고 DB 상태를 강제로 바꾸지 말고 기존 recovery 절차에서 실제 종료·잠금을 확인한다. [복구 정책 §5.5.3](design-docs/gas/mi-model-input-template-catalog-domain-model-design.md).
5. 준비한 테스트 Draft에서 RESTORE와 preview가 OFF로 거절되는지 확인한다. 서명키가 유효하면 변경 조회는 가능하지만 원본 구조 복원 상태는 UNSUPPORTED다. 기존 v2 클라이언트 저장은 유지되어야 한다.

## 5. testdb 증분 DDL — DBA 수동 실행

SQL은 [model-input-draft-restore-v3-ddl-draft.sql](model-input-draft-restore-v3-ddl-draft.sql)만 사용한다. 아래는 해당 파일의 실행 구간 순서이며 SQL을 중복 정의하지 않는다. 파일 전체를 한 번에 실행하지 않는다.

| 구간 | 수행 | 다음 단계로 넘어갈 조건 |
| --- | --- | --- |
| `1. Pre-check` | SELECT/SHOW만 실행 | `DATABASE()`가 승인한 testdb schema이고 MySQL의 CHECK 강제 지원, 현 `ck03` 정의·기존 v2 데이터와 rowh/revision 구조가 예상과 일치 |
| `2. Apply` | 승인된 ALTER 한 건 실행 | `tmd_mdl_inptdrftprcsl_ck03` 변경 성공. 실패하면 중단하고 현 SHOW 결과부터 재확인 |
| `3. Verify` | 새 연결에서 SHOW/SELECT 실행 | 확대된 CHECK가 ENFORCED이고 `invalid_rows=0` |

이미 확대된 제약이 있거나 v3 이력이 존재하면 최초 설치처럼 재실행하지 않는다. 현재 상태를 먼저 확인한다. 정의·제약명·지원 버전이 예상과 다르면 임의로 제약을 제거하거나 과거 데이터를 보정하지 않는다. `4. Rollback pre-check`와 주석 처리된 `5. Rollback SQL`은 정상 적용 절차가 아니다.

적용 시각·대상 schema·SQL 파일의 배포 버전·적용 전후 SHOW 결과·verify 결과를 남긴다. 이 절의 완료는 복원 기능 활성화 완료와 별개다.

## 6. 복원 ON — 웹 설정 반영

1. DDL verify 후 `MODEL_INPUT_RESTORE_ENABLED=true`, `MODEL_INPUT_RESTORE_PREVIEW_ENABLED=true`로 변경한다. `MODEL_INPUT_DRAFT_ENABLED=true`와 공통 서명키·기본 조회 한계는 유지한다.
2. 모든 웹 서버의 설정 반영과 rollout 종료를 확인한다. 새 기능을 켜면서 구버전 이미지를 섞지 않는다.
3. 새 또는 기존 테스트 Draft에서 변경 조회가 200인지 확인한다. 삭제된 지원 원본의 항목에서 `restoreStatus=READY`가 보이는지 확인한다. INCOMPLETE/NOT_ALLOWED/UNSUPPORTED를 수동 우회하지 않는다.

## 7. 배포 후 API 테스트

API 정본은 §11 및 기존 EDT-03/04/FINALIZE 계약이다. 아래 경로의 공통 prefix는 `/api/v1/mi/fundamental-model`이고, 승인한 testdb 웹 주소와 기존 인증 방식을 사용한다. `tableId`, `cursor`는 URL 인코딩한다. Swagger/Postman 또는 curl의 `--get --data-urlencode`를 사용하면 `/`, 공백, `+` 등이 손상되지 않는다.

`GET /cases/{caseId}/inputs/structure`의 `sourceVersion`으로 `POST /cases/{caseId}/input-drafts`를 호출하거나 테스트용 직접 업로드 Draft를 사용한다. Case 생성 API는 기존 Draft를 재개할 수 있으므로 반환된 실제 `draftId/revision`을 사용하며 0이라고 가정하지 않는다.

### 7.1 공통 저장·대기 규칙

- 매 단계 `GET /input-drafts/{draftId}`의 현재 성공 revision을 확인하고 `GET /input-drafts/{draftId}/table-data?revision=…&tableId=…`를 읽는다. 최초 원본 입력/표시와 SOURCE rowId·fieldKey를 저장한다.
- `POST /input-drafts/{draftId}/change-requests`의 202는 접수다. 응답의 `statusEndpoint`를 약 2초 간격으로 조회하고 SUCCEEDED/FAILED에서 종료한다. FAILED이면 다음 변경으로 넘어가지 않고 failure를 기록한다.
- 성공한 `resultRevision`으로 구조·표·변경 목록을 다시 읽는다. 다음 저장은 새 requestId와 그 revision을 사용한다. 응답 유실 재전송만 동일 requestId/동일 body를 사용한다.
- 아래 JSON의 `baseRevision:7`은 예시다. 실제 성공 revision, tableId, SOURCE rowId/fieldKey로 치환하고 새 요청마다 새 소문자 UUID requestId를 만든다. 좌표·과거 값·schemaVersion을 복원 명령에 추가하지 않는다.

### 7.2 기준값과 셀 되돌리기

1. `GET /input-drafts/{draftId}/changes?revision=…&tableId=…`를 읽는다. 새 미수정 Draft면 items=[]이고, 기존 Draft는 현재 차이가 나온다. `nextCursor`가 있으면 같은 revision으로 끝까지 읽고 중복·누락을 확인한다.
2. 기존 SET으로 허용 입력을 바꾸고 저장한다. 변경 목록의 CELL.baseline이 저장 전 값으로 바뀌지 않고 Draft 생성 원본을 계속 가리키는지 확인한다.
3. baseline VALUE는 기존 SET, EMPTY는 CLEAR로 되돌리고 저장한다. 해당 차이가 사라지는지 확인한다. 숫자 0·실제 빈칸·문자열 `"0"`은 각 필드가 허용하는 범위에서 구분한다. 타입 정책을 억지로 우회하지 않는다.
4. 편집 가능한 header/control도 변경 후 baseline으로 되돌려 확인한다. 화면 되돌리기 자체는 로컬 상태 변경이며 실제 저장은 기존 autosave다. FE 미연결이면 API 테스트 결과와 화면 검증을 구분한다.

### 7.3 삭제한 원본 행 복원

1. SOURCE rowId를 사용해 기존 DELETE_ROW를 저장한다. SUCCEEDED 후 해당 ROW/DELETED와 `restoreStatus=READY`를 확인한다.
2. `GET /input-drafts/{draftId}/restore-preview?revision=…&tableId=…&kind=ROW&targetId=…`를 호출한다. `preview:true`, 현재 성공 revision, 복원 예상 표·typed 입력을 확인한다. `page.nextStartRow`가 있으면 같은 대상/revision으로 전체 페이지를 확인한다.
3. preview 전후 Draft의 성공 revision·activeOperationId와 실제 표가 바뀌지 않아야 한다. preview는 DB 작업을 만들거나 저장하지 않는다.
4. 아래 RESTORE_ROW를 기존 change-requests에 저장한다.

```json
{
  "requestId": "a0000000-0000-4000-8000-000000000001",
  "baseRevision": 7,
  "operations": [
    {
      "type": "RESTORE_ROW",
      "commandId": "restoreRow",
      "tableId": "<tableId>",
      "rowId": "a0000000-0000-4000-8000-000000000008"
    }
  ]
}
```

5. SUCCEEDED 후 원래 rowId가 SOURCE로 활성화되고 원본 순서에 맞게 배치되는지 확인한다. 복원만 한 작업의 새 rowMappings/columnMappings는 빈 배열이다. 다른 이미 활성 셀의 변경은 유지된다.
6. 삭제 전 수정했던 원본×원본 교차는 삭제 직전 수정값이 아닌 기준 원본값으로 돌아와야 한다. preview의 해당 값/구조와 실제 저장 결과를 비교한다. 변경 목록에서 대상 ROW/DELETED가 사라져야 한다.

### 7.4 삭제한 원본 열 복원

1. 삭제 허용 원본 fieldKey를 기록하고 DELETE_COLUMN을 저장한다. COLUMN/DELETED의 READY를 확인한다.
2. preview의 `kind=COLUMN&targetId=<원래 fieldKey>`로 예상 표 전체를 확인한다. URL 인코딩·페이지·비영속 검사는 행과 같다.
3. 아래 RESTORE_COLUMN을 저장한다.

```json
{
  "requestId": "a0000000-0000-4000-8000-000000000002",
  "baseRevision": 7,
  "operations": [
    {
      "type": "RESTORE_COLUMN",
      "commandId": "restoreColumn",
      "tableId": "<tableId>",
      "fieldKey": "<originalFieldKey>"
    }
  ]
}
```

4. 원래 fieldKey·원본 헤더/입력·수식 표시와 기간 순서가 복구되고, 이웃 열과 기존 수정값이 유지되는지 확인한다. 현재 columnField/좌표는 이동할 수 있으므로 안정 ID인 fieldKey로 대조한다.

### 7.5 구조·이력·연결 회귀

각 시나리오는 독립 테스트 Draft 또는 기록한 성공 상태에서 진행한다. 기존 미저장 FE 변경과 섞지 않는다.

| 시나리오 | 확인할 결과 |
| --- | --- |
| 원본 행과 열을 모두 삭제한 뒤 행→열, 열→행 순서로 각각 복원 | 마지막 원본 교차값 동일, 동일 rowId/fieldKey, 기존 활성 순서/다른 변경 유지 |
| 추가 행/열과의 교차에 0·빈값·허용 typed 값을 저장한 뒤 원본 축 삭제/복원 | 삭제 직전 성공 이력의 교차값 보존. 삭제 후 만들어진 추가 축은 승인 CREATE 정책 적용, 필수 근거 없으면 전체 실패 |
| 같은 원본 ID 삭제→복원→삭제→복원 | 동일 ID 유지, 최근 삭제 직전 교차값 사용, 중복/유령 행·열 없음 |
| 복원 후 일반 SET/CLEAR 저장 | v3 이후 일반 v2 변경도 성공하고 재조회 일치 |
| PMI 공개 User-Adjusted 원본 행 복원 | 공개 행과 Base/Selected 대응, Selected 계산 표시·국가 연결 일치. 내부 표를 직접 요청하지 않음 |
| Regas 공개 master 원본 행 복원 | 월별 대응 행·국가 합계/연도·Helper/계산 표시 일치 |
| 파일 교체 기능을 사용하는 테스트 Draft에서 한 역할만 성공 교체 | 그 역할만 새 원본 기준·새 ID 사용. 과거 파일의 삭제 ID 복원 거부, 다른 역할 기준 유지 |
| 기존 v2 Draft 조회/일반 저장 및 v2→v3→v2 혼합 Draft 재진입 | 기존 클라이언트 계약 유지, baseline과 변경 표시 복원 |

### 7.6 오류·경합·용량

- 다른 소유자/내부 표는 404, 오래된 revision은 409 `REVISION_CONFLICT`, cursor 변조/대상 불일치는 400인지 확인한다. 같은 requestId/동일 body 재전송은 기존 작업을 재사용하고, body 변경은 IDEMPOTENCY_CONFLICT다.
- 이미 활성/ADDED/다른 파일 기준의 대상 복원, 복원과 같은 ID 삭제를 한 요청에 넣는 조합을 거절하고 revision/기존 값이 유지되는지 확인한다.
- 진행 작업 중 별도 변경 접수는 DRAFT_BUSY이고, 진행 중 조회는 직전 성공 상태다. FE는 진행 요청과 이후 로컬 변경을 구분해 보존한다.
- 동일 웹 JVM에서 두 느린 변경 조회/preview를 겹쳐 슬롯을 점유했을 때 429와 `Retry-After: 2`를 확인한다. 서로 다른 복제본에 분산된 요청은 JVM별 한도라 둘 다 처리될 수 있다. 대기 요청이 없으면 반드시 429가 나온다고 가정하지 않는다.
- 실제 큰 테스트 표로 지연·최고 임시 사용량·정리 결과를 측정한다. 처리 한계를 넘으면 422 `CHANGE_QUERY_UNAVAILABLE`/`RESTORE_PREVIEW_UNAVAILABLE`여야 하며 timeout/정리 후 다음 요청이 다시 처리되어야 한다. 테스트 편의를 위해 실제 원본을 손상시키거나 DB 상태를 직접 조작하지 않는다.

### 7.7 웹 재시작과 최종저장

1. 혼합 이력을 만든 후 진행 작업이 종료된 시점에 **동일한 v3 호환 배포본**으로 웹을 재시작한다. 재진입한 같은 Draft의 ID·입력·변경 목록과 수식 표시를 비교한다.
2. 독립 테스트 Draft에서 기존 `POST /input-drafts/{draftId}/finalizations` 계약으로 최종저장을 접수한다. 응답의 statusEndpoint에서 SUCCEEDED를 확인한다. 최종저장이 자동 실행을 연계하는 환경에서는 테스트 Case만 사용하고 실행 결과도 별도로 확인한다.
3. 생성된 네 역할 파일의 입력·수식/표시·연결 결과와 대상 밖 시트/형식/매크로 보존, 원본 Case 파일 미변경을 확인한다. FINALIZED Draft에 추가 변경이 거절되는지도 확인한다.

## 8. 판정·기록과 중단/되돌림

배포 commit/image, 웹 rollout 상태·profile/DB 식별, 변수의 비밀값 제외 적용 여부, DDL 전후 SHOW/verify, 테스트 draftId/tableId/requestId/operationId·성공 revision, 각 시나리오 실제 결과를 기록한다. preview 성공이나 202만으로 저장 성공을 판정하지 않는다. FE 미연결·미지원 정책·미수행 테스트를 통과로 표시하지 않는다.

새 접수를 중단하려면 복원 두 변수를 false로 반영하되 v3 reader와 기존 Draft/web-runner 설정은 유지한다. OFF는 이미 접수한 작업을 취소하거나 이력을 지우지 않는다. 롤아웃/작업 정리는 기존 웹 러너 종료 규칙을 따른다.

CHECK와 앱의 rollback은 [증분 DDL의 rollback 구간](model-input-draft-restore-v3-ddl-draft.sql)이 정본이다. 실패한 v3 접수도 operation에 남을 수 있다. v3 operation/revision 중 하나라도 남으면 v2 전용 앱·옛 CHECK로 되돌리지 않는다. 호환 코드를 유지한 채 기능 OFF로 대응하며, 이력을 삭제하거나 version을 바꾸어 rollback 조건을 맞추지 않는다.
