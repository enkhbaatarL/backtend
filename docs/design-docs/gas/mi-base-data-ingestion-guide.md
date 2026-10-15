# MI Base 운영 지정 가이드

## Document Control

- status: Review
- canonical for: 기존 독립수급모델 실행 건을 현재 MI Base로 지정할 때의 운영 사전·사후 확인 절차
- owner: BE / Operator
- source documents:
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
- depends on:
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`
- supersedes:
  - 2026-06-16 작성된 별도 MI Base case/run/result/file INSERT 가이드 내용
- last synced: 2026-10-08
- open questions: 없음

## 1. 목적

MI Base는 별도로 생성·적재하는 실행 세트가 아니다. 기존 독립수급모델 실행 case 중 선택한 1건을 현재 MI Base로 지정한다. 새 지정 시 이전 MI Base는 `INDEPENDENT`로 복귀하고 대상만 `MI_BASE`가 된다. current MI Base는 상세와 결과만 조회하는 읽기 전용 case이며, 별도 해제 없이 새 지정으로만 교체한다.

이 문서는 권한 있는 사용자가 지정하기 전후에 확인할 절차를 정리한다. 상태 전이와 새 기준 적용 시점은 workflow design, API path·응답·권한 계약은 enhancement feature spec, 물리 제약과 rollout SQL은 ERD/DDL draft가 canonical source다.

## 2. 현재 데이터 구조

MI Base로 지정되기 전에 해당 건은 일반 독립실행으로 생성·실행된다.

| 데이터 | 연결 기준 | MI Base 지정 시 처리 |
|---|---|---|
| case master | `tmd_mdl_scnrcasem.case_id` | 대상 row 유지·`exec_type_cd = MI_BASE`. 이전 current는 `INDEPENDENT`로 복귀 |
| run history | `tmd_mdl_scnrcaseh.case_id` | 기존 row·`run_id`·`run_seq` 유지 |
| input Excel | `tcm_filel(SCNRCASE + case_id)` | 기존 `DEMAND/SUPPLY/TRADE/OUTPUT` 4개 유지 |
| output files | `tcm_filel(SCNRCASEH + run_seq)` | 기존 row 유지 |
| parsed result | latest run의 `run_seq` | 기존 Volumes/Trade Flows/Price 결과 유지 |
| owner/audit | `usr_no`, `crt_*`, `mod_*` | 기존 실행 소유자와 생성 이력 유지. 교체되는 case들의 `mod_*`만 요청 actor/변경 시각으로 갱신 |

## 3. 핵심 규칙

1. 별도 `tmd_mdl_scnrcasem`, `tmd_mdl_scnrcaseh`, 결과, 파일 row를 INSERT하지 않는다.
2. 지정 대상은 `scnr_id IS NULL`, `case_stat_cd = 'COMPLETED'`인 active 독립실행 case다. `SCENARIO`, 미완료 case, soft-deleted case는 지정하지 않는다.
3. MI Base 지정 시 `case_id`, `usr_no`, `case_stat_cd`, `last_run_id`, case/run 상태와 결과, input/output 파일을 변경하지 않는다.
4. `tmd_mdl_scnrcasem_ck01` 제약에 따라 `MI_BASE`는 `scnr_id = NULL`을 유지한다.
5. current MI Base는 전역에서 최대 1건이다. 새 지정 시 이전 current는 `INDEPENDENT`로 복귀하고 해당 UPDATE가 DB에 반영된 뒤 대상만 `MI_BASE`가 된다. 두 변경은 한 transaction에서 처리한다.
6. 이전 MI Base row를 물리 삭제하거나 soft delete하지 않는다. 원래 case/run/input/result와 소유자는 보존하며, 복귀 후 일반 `INDEPENDENT` case로 목록에 노출한다.
7. `exec_type_cd`와 `del_yn`에서 파생되는 generated `actv_base_yn` unique constraint가 동시 지정에서도 active `MI_BASE` 2건 생성을 막는다. application은 helper 값을 직접 쓰지 않는다.
8. `case_id` 크기는 현재 MI Base 판정 기준이 아니다. 더 오래된 case도 지정 요청이 성공하면 현재 MI Base가 된다.
9. 지정 commit 이후 시작되는 수급모델 input 생성만 새 MI Base의 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 4개를 사용한다. 이미 생성·접수·완료된 실행의 input, 결과, 기준값은 바꾸지 않는다.
10. MI Base가 아직 없으면 기존 플랫폼 기본 template을 사용한다. 지정된 MI Base가 있으면 해당 파일을 읽지 못할 때 이전/default template으로 조용히 fallback하지 않고 실행 준비를 실패시킨다.
11. 기존 Scenario Case 목록 API는 current `miBase`와 active `INDEPENDENT` `cases`를 분리하도록 변경한다. 이전 MI Base는 복귀 후 `cases`에 포함한다. 구체적인 필드·검색·정렬 계약은 enhancement feature spec을 따른다.
12. 한 input 생성은 시작 시 현재 MI Base `caseId`와 네 역할 파일 metadata를 한 번만 source snapshot으로 고정한다. 생성 중 지정이 바뀌어도 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT`을 서로 다른 case에서 섞지 않는다.
13. 지정과 기존 수정·삭제 command는 같은 row-lock 또는 atomic `exec_type_cd = 'INDEPENDENT'` 조건으로 직렬화한다. MI Base로 바뀐 row를 뒤늦은 수정·삭제가 변경하지 못해야 한다. 실행은 `PENDING | FAILED`, 지정은 `COMPLETED`만 허용하므로 두 command의 적격 상태가 겹치지 않는다.
14. current MI Base는 상세·상태·Volumes filter-options·Volumes·Trade Flows·Price 조회를 허용한다. FE는 `execType = MI_BASE`에서 결과 탭은 노출하되 수정·삭제·재실행 action을 노출하거나 호출하지 않으며, backend도 mutation을 active `INDEPENDENT` 전용으로 유지한다.
15. MI Base 지정 취소만 수행하는 해제 API는 제공하지 않는다. current 변경은 새 완료 `INDEPENDENT` case를 지정해 기존 current를 자동 복귀시키는 흐름만 사용한다.

## 4. 지정 전 확인

운영 지정 수단은 최소한 다음을 확인해야 한다.

- active case이고 `case_stat_cd = 'COMPLETED'`, `exec_type_cd = 'INDEPENDENT'`인지 확인한다. 단, 현재 MI Base와 같은 `caseId` 반복 요청은 `MI_BASE` type을 멱등 예외로 허용하고 나머지 적격성을 다시 확인한 뒤 성공으로 처리한다.
- `scnr_id IS NULL`인지
- `SCNRCASE + case_id`에 active input 파일이 정확히 4개인지
- 파일명으로 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할이 각각 1개씩 식별되는지
- `last_run_id`가 가리키는 run history와 case 상태가 정합한지
- MI Base 그래프에 필요한 Price 결과가 해당 latest completed run에 존재하는지

사전 확인이 하나라도 실패하면 기존 MI Base를 변경하지 않고 지정 전체를 실패시킨다.

## 5. 지정 후 검증

- 대상 case의 `exec_type_cd = 'MI_BASE'`인지 확인한다.
- 이전 current가 있었다면 `INDEPENDENT`로 복귀했는지 확인한다. active `MI_BASE`와 generated current marker는 정확히 1건이어야 한다.
- case/run/file/result의 식별자와 row 건수가 지정 전과 같은지 확인한다.
- `GET /api/v1/mi/fundamental-model/cases` 응답의 `miBase`에 지정 대상이 노출되고 `execType = MI_BASE`인지 확인한다.
- current `miBase.caseId`로 상세·상태·Volumes filter-options·Volumes·Trade Flows·Price를 조회할 수 있고, 기존 latest completed run 결과가 반환되는지 확인한다.
- current `MI_BASE`에 수정·삭제·재실행을 직접 요청해도 backend가 active `INDEPENDENT` 제한으로 거부하는지 확인한다. FE에서도 해당 action이 노출되거나 호출되지 않아야 한다.
- 지정 이후 생성한 수급모델 input은 대상 case의 4개 input을 기준으로 하고, 지정 전 실행 결과는 변하지 않았는지 확인한다.
- input 생성 도중 MI Base를 교체해도 진행 중 생성의 네 파일은 처음 선택한 단일 source snapshot에 고정되고 다음 생성부터 새 base가 적용되는지 확인한다.
- 지정-vs-update, 지정-vs-delete 경쟁에서 MI Base row가 수정·삭제되지 않는지 확인한다. 실행 API는 `PENDING | FAILED`만 허용하므로 `COMPLETED` 지정 대상과 적격 상태가 겹치지 않는다.
- 허용 actor가 다른 사용자가 생성한 적격 case도 소유자·소유 회사 조건 없이 지정할 수 있는지 확인한다.

## 6. 권한 경계

- MI Base 지정은 기존 MI 수급모델 실행 접수와 동일한 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`를 허용한다. 별도 operator entitlement는 추가하지 않는다.
- MI Base 지정 대상에는 `usr_no = currentUser` 또는 소유 회사 predicate를 적용하지 않는다. 서버는 대상 소유자나 회사를 지정 조건으로 조회하지 않는다.
- 대상 `caseId`에는 active/type/status와 run/input/Price 적격성만 검증한다.

지정 API의 정확한 role/condition, endpoint, 오류 응답은 [MI Fundamental Model MI Base 지정 Feature Spec](../../feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md)을 따른다. Input 내용 제공은 별도 [MI Fundamental Model Input 제공 Feature Spec](../../feature-specs/mi-fundamental-model-input-provision-feature-spec.md)에서 관리한다.

## 7. 비범위

- MI Base 지정 취소만 수행하는 해제 API
- 별도 지정 승인 workflow 및 지정 이력 table/API
- MI Base용 별도 case/run/result/file 적재
- Input 내용 조회·전달·편집 API

## 8. Canonical References

- lifecycle/business semantics: `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
- physical schema/constraints: `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
- list/detail/status/result/지정 API contract: `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`
- input 제공 API draft: `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md`

## 9. Implementation Readiness

- status: `contract ready; activation blocked by DBA review/rollout`
- domain rule: 새 지정 시 이전 MI Base의 `INDEPENDENT` 복귀로 확정
- API contract: endpoint, lifecycle, actor role/condition, 대상 공통 적격성 검증, MI Base 상세 결과 조회와 읽기 전용 mutation 차단은 ready. 대상 소유자·회사 제한은 적용하지 않고 별도 지정 해제 API도 제공하지 않는다.
- schema dependency: generated `actv_base_yn`과 unique DDL의 DBA 검토·선행 적용이 필요하다.
