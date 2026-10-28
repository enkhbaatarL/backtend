# 행 추가 가능 표의 행 삭제 v8 등록·발행

## Document Control
- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED
- canonical for: Schema rollout — 행 삭제 정책 v8 등록·발행
- owner: BE / DBA
- date: 2026-10-26
- source documents: `../scripts/build-model-input-row-delete-v8.py`
- depends on: `model-input-editor-v6-deployment-guide.md`, `design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §6.8A

## 변경 범위

v7의 96표/171수정 범위를 복사하고 행 추가가 READY인 표 중 삭제 INCOMPLETE인 51표의 DELETE_ROW만 READY로 바꾼다. 행 추가 가능 표는 총 55개다. 명시적 NOT_ALLOWED 및 행 추가 금지 표는 승격하지 않는다. PNG Infrastructure 원본 행 삭제 금지와 Regas 연결 삭제 제한은 코드에서 유지한다. 물리 schema 변경은 없다.

기존 v5/v6/v7 발행 정의·Case/Draft 참조는 보존한다. v8 발행만으로 기존 Case/Draft는 바뀌지 않는다. 기존 Draft의 INCOMPLETE를 새 코드에서 READY로 덮어쓰지 않는다.

## 적용 절차

1. `python3 scripts/build-model-input-row-delete-v8.py --check`로 SQL이 현재 원본과 일치하는지 확인한다.
2. [기존 배포 절차](model-input-editor-v6-deployment-guide.md)의 환경 식별·writer 정지·백업·전용 연결·예행·COMMIT 및 새 연결 확인 절차를 따른다. 파일/목표 버전은 이 문서의 v8을 사용한다.
3. `model-input-row-delete-v8-registration-draft.sql` 입력부에 실제 환경과 정확한 기존 ACTIVE ID/버전/marker를 지정한다. UPGRADE의 이전 버전은 8 미만이다. v8을 임의 덮어쓰거나 버전을 수정하지 않는다. 기본 ROLLBACK으로 예행하고 VERIFIED_UNPUBLISHED를 확인한다.
4. 승인된 등록 COMMIT 후 새 연결에서 확인한 v8 DRAFT ID를 `model-input-row-delete-v8-publication-draft.sql`에 지정한다. v8 코드 배포 확인 후 reader_ready를 설정하고 기본 ROLLBACK 예행으로 VERIFIED_PUBLISHED를 확인한다. 실제 발행 COMMIT은 별도 승인 후 같은 연결에서 수행한다.
5. v8을 참조하는 격리 Case/Draft에서 `editing.capabilities.DELETE_ROW=READY`를 확인한다. 원본/추가 행 삭제, 같은 요청 ADD→DELETE, 저장 후 재조회·재시작·최종 파일을 확인한다. PMI의 세 표 동시 삭제 및 PNG 원본 행 거부도 확인한다.

이 작업은 실DB 등록·발행·기존 참조 재연결·애플리케이션 배포를 실행하지 않는다. SQL 실행 결과와 테스트/코드 완료 상태를 구분해 기록한다.
