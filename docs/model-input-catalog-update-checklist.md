# 배포 시 Model Input Catalog 업데이트 실행 안내

## Document Control
- status: Ready for manual preparation / DB NOT EXECUTED
- canonical for: 운영 절차 진입점과 실행 기록 양식. SQL·정책 정의는 아래 원본을 따른다.
- owner: BE / DBA / 배포 담당자
- date: 2026-10-24
- source: [v6 상세 배포 가이드](model-input-editor-v6-deployment-guide.md)
- execution record: [절차 검토 기록](work-notes/2026/2026-10-24-model-input-catalog-update-procedure.md)

Catalog는 파일·시트·표·편집 규칙을 담은 DB 데이터다. 코드 배포만으로 갱신되지 않는다. 아래는 현재 저장소의 **v6** 적용용 진입점이다. 다음 버전에는 해당 버전 SQL을 준비해야 하며 버전 숫자만 바꾸어 실행하지 않는다.

## 1. 이번 배포에 필요한 작업 선택

| 상황 | 선택 |
| --- | --- |
| 코드만 변경되고 Catalog 정의는 동일 | Catalog 재등록·재발행 불필요. 서버 코드와 현재 정의 호환성 확인 |
| schema가 있고 모델 등록 이력 없음 | v6 `FIRST_INSTALL` |
| 기존 ACTIVE가 정확히 1개이고 버전 < 6 | v6 `UPGRADE` |
| 이미 최신 v6 DRAFT 존재 | 전체 정의 일치 확인 후 등록 생략, 발행 검토 |
| 이미 최신 v6 ACTIVE | 전체 정의·호환 코드 확인. 같은 SQL 재실행 불필요 |
| 예전 95표 v6, 다른 marker, ACTIVE 0개/복수 등 | 중단하고 별도 버전·정리 계획 결정 |

schema가 없으면 [최초 설치 안내](model-input-first-deployment-runbook.md) §2~5를 먼저 따른다. 기존 환경에서 DDL·runner 초기화를 반복하지 않는다. `model-input-catalog-install-*.sql`, `model-input-post-ddl/02~03`은 v5 최초 설치용이므로 v6 업데이트에 사용하지 않는다.

## 2. 실행 준비

저장소 루트에서 DB 연결 없이 생성물 일치를 확인한다.

```bash
python3 scripts/build-model-input-catalog-install.py --editor-v6 --check
shasum -a 256 docs/model-input-editor-remaining-v6-registration-draft.sql docs/model-input-editor-remaining-v6-publication-draft.sql
```

검사 실패 시 배포 산출물을 다시 확인한다. 운영 준비 과정에서 생성 파일을 임의 수정하지 않는다.

DB 작업자는 [상세 가이드 §2](model-input-editor-v6-deployment-guide.md#2-해당-환경에서-먼저-확인할-내용)의 읽기 전용 조회로 환경을 확인한다. 관련 API 접수·catalog writer·runner/worker 작업 종료와 백업을 확인한다. 기능 OFF만으로 진행 중 작업이 종료됐다고 판단하지 않는다.

실행 복사본은 다음 두 파일만 준비한다. 비밀번호·토큰은 저장하지 않는다.

- [등록 SQL](model-input-editor-remaining-v6-registration-draft.sql)
- [발행 SQL](model-input-editor-remaining-v6-publication-draft.sql)

두 파일의 입력부에 환경, 서버 UUID, schema, 실제 적용자, 모드, 이전 ACTIVE의 실제 ID·버전·marker를 각각 채운다. FIRST_INSTALL의 이전 값은 NULL이다. 발행 파일에는 **등록 COMMIT 후 새 연결에서 조회한 ID**와 호환 코드 배포 확인 후 `reader_ready=1`을 추가한다. 개발계 ID를 운영계에 복사하지 않는다.

## 3. 실행 순서 — 개발계 완료 후 운영계에서 독립 반복

| 순서 | 작업 | 통과 기준 |
| --- | --- | --- |
| 1 | 등록 SQL 전체를 기본 ROLLBACK까지 예행 | `PRECHECK_OK`, `verified_must_be_1=1`, `VERIFIED_UNPUBLISHED` |
| 2 | 등록을 처음부터 다시 실행, 마지막 ROLLBACK 직전에 결과 확인 후 같은 연결에서 COMMIT | 새 연결에서 실제 v6 ID, DRAFT, 적용 시각 NULL, 정확한 marker |
| 3 | 해당 서버의 호환 코드 배포 확인 | 배포 SHA·확인자 기록, 상세 가이드 §4 지원 항목 충족 |
| 4 | 발행 SQL 전체를 기본 ROLLBACK까지 예행 | `PRECHECK_OK`, `verified_must_be_1=1`, `published_must_be_1=1`, `VERIFIED_PUBLISHED` |
| 5 | 발행을 처음부터 다시 실행, 마지막 ROLLBACK 직전에 결과 확인 후 같은 연결에서 COMMIT | `activated_rows=1`; `retired_rows`는 UPGRADE 1 / FIRST_INSTALL 0 |
| 6 | 새 연결에서 최종 조회 | 지정한 v6 ACTIVE, 모델 ACTIVE 정확히 1개, UPGRADE의 이전 ID DEPRECATED |
| 7 | 실효 설정 확인·기능 활성화 후 격리된 새 Case로 시험 | 새 v6 선택, 조회·승인된 변경·최종저장·결과 파일 확인 |

각 SQL은 새 전용 연결에서 실행한다. 오류·STOP·불일치가 하나라도 있으면 같은 연결에서 ROLLBACK하고 멈춘다. 오류 후 계속 실행하거나 `--force`를 쓰지 않는다. 이미 ROLLBACK했다면 뒤에 COMMIT을 붙여도 저장되지 않는다. COMMIT 응답이 유실되면 새 연결로 저장 상태부터 확인한다.

건수만으로 성공 처리하지 않는다. 전체 정의 비교는 SQL에 맡기며 기대 건수·marker의 정본도 SQL이다. 현재 가이드는 4파일/70시트/96표/171범위다. 자동 일괄 COMMIT보다 **예행과 결과 확인 후 명시적 COMMIT** 방식으로 실행한다.

## 4. 완료 판단과 실패 처리

상세 가이드 §5에 따라 실효 설정을 확인한다. source-key `DRAFT_C_STRUCTURE_V1`은 버전 번호와 별개다. CHANGE/FINALIZE는 요청 접수 응답만 보지 말고 최종 `SUCCEEDED`, revision과 결과 파일까지 확인한다. 로컬 검사 통과는 실제 환경 성공 증거가 아니다.

새 ACTIVE는 새 Case/업로드에 적용된다. 기존 Case/Draft의 고정된 양식 연결은 자동 변경되지 않는다. 기존 Case에도 새 규칙을 적용해야 한다면 별도 재연결 검토가 필요하다.

COMMIT 전 실패는 ROLLBACK한다. COMMIT 후 문제는 접수·소비를 중지하고 데이터와 이력을 보존해 조사한다. 이전 양식 재활성화·v6 삭제·runner 강제 초기화를 즉시 복구 수단으로 사용하지 않는다.

## 5. 환경별 기록 양식

```text
환경 / 서버 UUID / schema:
배포 코드 SHA / 확인자:
SQL SHA-256 (등록 / 발행):
모드:
이전 ACTIVE ID / 버전 / marker / 적용 시각 / 참조 수:
등록 예행 결과 / 실제 COMMIT / 새 연결 확인:
실제 신규 ID / marker:
발행 예행 결과 / 실제 COMMIT / 새 연결 확인:
기존 참조 보존 확인:
실효 기능 설정:
시험 Case / Draft / operation ID / 최종 상태 / revision / 파일 확인:
적용자 / 시각 / 남은 문제:
```

실제 출력과 검증 증거는 접근이 제한된 환경별 배포 기록에 보관한다. 이 안내 자체는 DB 적용 완료 증거가 아니다.

## 6. 환경 JSON으로 실행 SQL 준비

준비 스크립트는 DB에 연결하지 않는다. 아래 JSON을 `/tmp/catalog-env.json`으로 저장하고 실제 환경값으로 바꾼다. UUID는 §2 조회 결과를 사용한다. 비밀번호는 입력하지 않는다.

```json
{
  "environment": "DEVELOPMENT",
  "expected_server_uuid": "11111111-1111-1111-1111-111111111111",
  "expected_schema": "testdb",
  "actor": "your.login",
  "mode": "UPGRADE",
  "previous_template_id": 10,
  "previous_version": 5,
  "previous_marker": "현재 ACTIVE의 tmplt_expn 전체",
  "expected_template_id": null,
  "reader_ready": false
}
```

최초 설치는 `mode=FIRST_INSTALL`, `previous_template_id`, `previous_version`, `previous_marker`를 모두 JSON `null`로 지정한다.

```bash
python3 scripts/prepare-model-input-catalog-sql.py --config /tmp/catalog-env.json --stage register --output-dir /tmp/catalog-register
```

새 폴더에 `register-rehearsal.sql`, `register-manual.sql`이 생성된다. 기존 폴더는 덮어쓰지 않는다. 생성 전 원본 SQL의 `--editor-v6 --check` 검사도 자동 수행한다.

1. 전용 대화형 DB 연결에서 `register-rehearsal.sql`을 실행하고 §3 성공값을 확인한다. 마지막 ROLLBACK으로 취소된다.
2. 실제 등록 시 새 전용 대화형 연결에서 `register-manual.sql`을 실행한다. 이 파일은 마지막 ROLLBACK을 주석으로 바꿔 **transaction을 열린 상태로 둔다**. 성공값을 직접 확인한 뒤 즉시 **같은 연결**에서 `COMMIT;`, 실패면 `ROLLBACK;`을 입력한다. SQL 오류 시 뒤 문장을 계속 실행하지 않는다. 연결을 닫는 일회성 배치 명령으로 실행하지 않는다.
3. 새 연결에서 실제 저장된 신규 ID를 확인하고 JSON의 `expected_template_id`에 입력한다. 호환 코드 배포 확인 후에만 `reader_ready`를 JSON `true`로 바꾼다. 예행 ID는 사용하지 않는다.
4. 아래 명령으로 발행 파일을 준비한다.

```bash
python3 scripts/prepare-model-input-catalog-sql.py --config /tmp/catalog-env.json --stage publish --output-dir /tmp/catalog-publish
```

`publish-rehearsal.sql`로 예행 후, 새 연결에서 `publish-manual.sql`을 실행한다. §3 발행 성공값을 확인한 뒤 같은 연결에서 직접 `COMMIT;` 또는 `ROLLBACK;`을 결정한다. 결정할 때까지 잠금이 유지되므로 연결을 방치하지 않는다. COMMIT 후 새 연결 검증과 API 시험도 수행한다.

두 단계는 별도 transaction이다. 두 manual 파일을 한 번에 이어 실행하지 않는다. 스크립트는 실제 DB 상태·서버 코드 배포 여부를 확인하지 않으므로 원본 SQL의 검증과 담당자의 결과 확인을 생략하지 않는다.
