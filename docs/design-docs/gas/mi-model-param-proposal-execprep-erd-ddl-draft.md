# MI 파라미터 제안 기반 실행 준비 ERD/DDL Draft

## Document Control

- status: Draft
- canonical for: Schema rollout
- owner: BE / DBA
- source documents:
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `docs/design-docs/gas/mi-agent-param-proposal.md`
  - `docs/design-docs/gas/mi-scenario-generation-request-erd-ddl-draft.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - 없음
- last synced: 2026-07-08
- open questions:
  - (resolved) table/subject 물리명은 DBA 승인으로 `tmd_mi_model_execpreph` / subject `md`로 확정 (이전 draft명 `tmd_execpreph`에서 변경). 인덱스명도 `tmd_mi_model_execpreph_{ix|ux}nn`로 정합.
  - (resolved → Option A) 값 조정 재실행은 같은 `model_param_proposal_id`에 확정 API를 재호출해 `confirmation_parameters`를 갱신한다(최신 스냅샷만 유지, mutable). 변경 없음 재실행은 재확정 없이 기존 confirmed 값을 재사용한다. Agent 확정 API가 이미 `CONFIRMED`인 proposal의 재확정을 허용하도록 Agent가 수정 확정했다(2026-07-09 회신) → Option A 전제 충족. 과거 실행별 확정값 복원이 요구되면 그때만 `tmd_mi_model_execpreph`에 실행별 confirmed JSON snapshot을 fallback으로 추가한다.

- domain: gas / MI model execution
- created at: 2026-07-08
- scope: Backend 파라미터 제안 기반 수급모델 실행 준비 단일 테이블 `tmd_mi_model_execpreph` physical ERD/DDL draft (Agent proposal 테이블은 read-only 참조, 본 DDL 대상 아님)

## Source Documents

- [mi-model-parameter-proposal-design.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-model-parameter-proposal-design.md) — canonical business/lifecycle/ownership
- [mi-agent-param-proposal.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-agent-param-proposal.md) — Agent 소유 schema 초안 (`tmm_mi_model_param_proposal_m`, PK `model_param_proposal_id INT UNSIGNED`)
- [mi-scenario-generation-request-erd-ddl-draft.md](/Users/hoduhomil/source/mip/backend/docs/design-docs/gas/mi-scenario-generation-request-erd-ddl-draft.md) — 동일 도메인 백엔드 orchestration 요청 테이블 포맷 선례
- 참조 명명/사전: [db-object-naming-rules.md](/Users/hoduhomil/source/mip/backend/docs/conventions/db-object-naming-rules.md), [db-term-dictionary-sheet2.csv](/Users/hoduhomil/source/mip/backend/docs/conventions/db-term-dictionary-sheet2.csv)

## Decision Summary

- canonical write aggregate는 파라미터 제안 기반 수급모델 실행 준비 이력 단일 테이블 `tmd_mi_model_execpreph` 하나다. child detail 테이블은 만들지 않는다.
- `tmd_mi_model_execpreph`는 수급모델 실행 이력 테이블이 아니다. 실제 수급모델 실행 master/history는 기존 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`가 source of truth다.
- `tmd_mi_model_execpreph` row 1건은 수급모델 input Excel 3개 세트를 생성하고 실행 접수하는 **실행 준비 1회**를 대표한다. 같은 `scnr_id` + `agnt_prpsl_id` 조합으로 여러 row가 생길 수 있다.
- 수급모델 실행은 기존 `mi-scenario-model-execution-workflow-design.md` 기준을 따른다. 실행 1회마다 새 `case_id`를 만들고, 각 Case 아래에 Run history를 쌓는다. `tmd_mi_model_execpreph.case_id`는 해당 실행 준비 row가 만든 Case를 0..1로 가리키는 포인터다.
- 최근 수급모델 실행에 사용된 confirmed parameter 조회는 최신 `ScenarioCase` → `tmd_mi_model_execpreph.case_id` → `agnt_prpsl_id` → Agent `confirmation_parameters` 경로를 사용한다. 최초 Agent 제안값 조회는 같은 `agnt_prpsl_id`의 Agent `proposal_parameters`를 사용한다.
- 재실행 파라미터 규칙은 두 경로다. (1) **값 조정 재실행 = 같은 `model_param_proposal_id` 재확정(Option A)**: 확정 API를 같은 id에 재호출해 `confirmation_parameters`를 갱신하고, 같은 `agnt_prpsl_id`로 새 `tmd_mi_model_execpreph` row + 새 `case_id`를 만든다. Agent `proposal_parameters`(최초 제안)는 재확정으로 바뀌지 않는다. (2) **파라미터 생성부터 다시 = 새 `model_param_proposal_id`**: 새 proposal 생성 요청으로 새 `agnt_prpsl_id`를 발급한다(RUNNING pre-check 적용). 두 경우 모두 Excel 3개는 새 `case_id` 기준으로 새로 생성한다. "최신 실행 confirmed 값" 조회는 항상 최신 `case_id` 기준이라 정합하며, 값 조정 in-flight 중 재확정~새 case 생성 사이의 짧은 window에서만 직전 case가 갱신값으로 보일 수 있다(과거 실행별 정확 복원이 필요하면 execprep snapshot fallback).
- 최근 수급모델 실행 input Excel 3개 다운로드는 최신 `ScenarioCase.case_id` 기준 기존 `tcm_filel(SCNRCASE + case_id)` 파일 조회를 사용한다. `tmd_mi_model_execpreph`에는 파일 3개 목록을 복제하지 않는다.
- proposal 계산 데이터/payload/hash/snapshot은 저장하지 않는다. Agent `tmm_mi_model_param_proposal_m`을 read-only로 조회한다.
- request-time snapshot(`scenario_group_id`, `mi_event_id`, `event_type_l2_id`)도 Backend에 복제하지 않는다. Agent proposal 테이블이 이를 저장하며(write-then-callback 보장으로 완료 callback 시점엔 Agent row가 이미 존재), Backend는 재조회로 cross-validate한다(business rules 8/19/20). `agnt_prpsl_id` 확정 전 window는 아직 proposal 자체가 생성 중이라 검증 대상이 없다.
- `Scenario 1:N execution-preparation`이며, 진행 중(in-flight) preparation은 시나리오당 최대 1건이다.
- 실행 전 제안 폐기/취소 전이는 두지 않는다. in-flight preparation은 confirm→실행으로 진행하거나 timeout/reconcile로 `REQUEST_FAILED`가 될 때까지 유지된다. 따라서 생성 완료·미실행 상태에서는 in-flight 가드(ux02)가 새 파라미터 생성을 막고, 사용자는 기존 제안을 검토·수정해 실행하거나 timeout을 기다린다.
- active(in-flight) 유일성은 `ScenarioCase.actv_scnr_id`와 동일한 STORED generated column + UNIQUE로 강제한다. 식에 `del_yn = 'N'`을 포함해 soft delete row의 유령 충돌을 막는다.
- `EXECUTING`은 in-flight 집합에서 제외한다. 이는 `tmd_mi_model_execpreph`가 실행 이력을 소유한다는 뜻이 아니라, 실제 실행 중복/상태/terminal 이력은 기존 `tmd_mdl_scnrcasem.actv_scnr_id`(RUNNING) UNIQUE와 `tmd_mdl_scnrcaseh`가 통제한다는 뜻이다.
- `agnt_prpsl_id`는 Agent PK와 동일한 `INT UNSIGNED`, nullable, row 단위 write-once. 같은 Agent proposal을 반복 실행할 수 있으므로 **unique가 아니다**.
- `case_id`는 실행 접수 성공 후 연결, nullable + UNIQUE. 하나의 Case는 하나의 execution-preparation row에만 연결된다.
- callback path `scenarioId`는 proposal identity가 아니라 현재 proposal-request in-flight row를 찾는 scope key다. `agnt_prpsl_id`가 확인돼도 단독 unique lookup을 하지 않고, `REQUESTING`/`PROPOSAL_PENDING` row에만 수렴한다.
- 물리 FK는 `scnr_id`, `agnt_prpsl_id`, `case_id` 모두 걸지 않는다. 전부 논리 참조 + application validation이다.
- Backend orchestration 상태 전이/멱등성은 DB CHECK가 아니라 application compare-and-set update로 보장한다.
- 실패 단계는 상태 코드(`REQUEST_FAILED` vs `EXECUTION_FAILED`)가 이미 구분하므로 별도 `fail_stg_cd` 컬럼을 두지 않는다.
- `retry_cnt`는 재시도 정책이 P1 open이므로 이 DDL에 포함하지 않고 정책 확정 시 append한다.

## Naming / Dictionary Review

적용한 명명 규칙:

- table pattern: `{resource(1)}{subject(2)}_{business_name}{entity_type(1)}`
- subject: 기준정보 `md` — 백엔드 실행/시나리오 orchestration 계열(`tmd_scnrcrth`, `tmd_mdl_scnrcasem`)과 정합. (Agent 소유 config/proposal은 `mm`)
- entity type: `h` (이력) — request lifecycle 누적 성격. 선례 `tmd_scnrcrth`와 동일
- business_name: `mi_model_execprep` (MI 수급모델 `MI_MODEL` + 실행 `EXEC` + 준비 `PREP`) — DBA 승인명. `tmm_mi_model_param_proposal_m` 등 `mi_model` 계열과 정합하도록 이전 draft `execprep`에서 확장했다. 업무 의미는 "수급모델 실행 이력"이 아니라 "파라미터 제안 기반 수급모델 실행 준비와 input Excel 3개 세트 생성을 추적하는 Backend orchestration"이다. Agent는 upstream 제안 source/correlation 대상이므로 table 물리명에는 넣지 않고 `agnt_prpsl_id` 컬럼으로 표현한다.
- column prefix: `exec_prep_*`
- index/UK pattern: `{table_name}_{ix|ux}{nn}`, PK는 MySQL `PRIMARY`
- common system columns: `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn` — `BaseEntity`/`TimeBaseEntity` 실제 물리명 기준 (명명규칙 요약본의 `reg_user_id`가 아니라 코드/`ddl-auto=validate` 기준)

사용 사전 용어:

| 한글 | 약어 | 비고 |
|---|---|---|
| 실행 | `EXEC` | 기존 |
| 준비 | `PREP` | 이번 설계 신규 등록 (No 382) |
| 제안 | `PRPSL` | 이번 설계 신규 등록 (No 383) |
| 에이전트 | `AGNT` | 이번 설계 신규 등록 (No 384) |
| 활성 | `ACTV` | 기존. generated helper 컬럼 `actv_scnr_id`에 사용 |
| 시나리오 | `SCNR` | 기존 |
| 케이스 | `CASE` | 기존 |
| 상태 | `STAT` | 기존 |
| 요청 | `REQ` | 기존 |
| 사용자 | `USR` | 기존 |
| 번호 | `NO` | 기존 |
| 오류 | `ERR` | 기존 |
| 메세지 | `MSG` | 기존 |
| 내용 | `CNTT` | 기존 |
| 코드 | `CD` | 기존 |

naming review 결과(erd-change-review/DBA 승인 완료):

- table 물리명은 DBA 승인으로 `tmd_mi_model_execpreph`를 채택한다(이전 draft `tmd_execpreph`에서 변경, `mi_model` 계열 정합). 대안 `tmd_agnt_execpreph`는 Agent 소유 테이블로 오해될 수 있어 채택하지 않았다. proposal 문맥을 이름에 넣는 `tmd_prpslexecpreph`도 후보였지만, 역할 기준 `execprep`을 유지하고 proposal linkage는 `agnt_prpsl_id` 컬럼 및 table comment로 표현한다.
- subject는 `md`로 DBA 확정.

## Logical ERD

```text
tmm_mi_scenario_m (existing, Agent/Scenario 소유)
  scenario_id
  1
  |            논리참조 (물리 FK 없음)
  N
tmd_mi_model_execpreph (new, Backend 소유)
  PK exec_prep_id
  IX agnt_prpsl_id      -> tmm_mi_model_param_proposal_m.model_param_proposal_id (논리참조, repeated execution 허용)
  UK case_id            -> tmd_mdl_scnrcasem.case_id (논리참조, 실행 접수 후 연결)
  UK actv_scnr_id       (STORED generated, active/in-flight 시 scnr_id / 아니면 NULL)
```

FK 적용 판단:

| 관계 | FK 적용 | 판단 |
|---|---:|---|
| `tmd_mi_model_execpreph.scnr_id -> tmm_mi_scenario_m.scenario_id` | N | 기존 `ScenarioCase.scnr_id`도 물리 FK 없이 논리 참조. Scenario/Agent 소유 테이블과 배포/삭제 결합 회피. |
| `tmd_mi_model_execpreph.agnt_prpsl_id -> tmm_mi_model_param_proposal_m.model_param_proposal_id` | N | Agent 소유 테이블에 물리 FK를 걸면 Agent/Backend 배포 순서·삭제 정책이 강결합된다. 반복 실행으로 같은 Agent proposal ID가 여러 execution-preparation row에 들어갈 수 있다. |
| `tmd_mi_model_execpreph.case_id -> tmd_mdl_scnrcasem.case_id` | N | 기존 Case 테이블과 배포 결합을 피한다. nullable unique + application validation으로 하나의 실행 준비 row가 만든 Case를 추적한다. |

## Table: `tmd_mi_model_execpreph`

의미: MI 파라미터 제안 기반 수급모델 실행 준비와 input Excel 3개 세트 생성을 추적하는 Backend orchestration 이력. 실제 수급모델 실행 이력은 `tmd_mdl_scnrcasem` / `tmd_mdl_scnrcaseh`에 쌓인다.

| 컬럼 | 타입 | Null | Key | 설명 |
|---|---|---:|---|---|
| `exec_prep_id` | `BIGINT UNSIGNED` | N | PK | Backend surrogate id (AUTO_INCREMENT) |
| `crt_usr_logn_id` | `VARCHAR(50)` | N |  | 생성사용자로그인ID |
| `crt_dtm` | `DATETIME` | N |  | 생성일시 |
| `mod_usr_logn_id` | `VARCHAR(50)` | N |  | 수정사용자로그인ID |
| `mod_dtm` | `DATETIME` | N |  | 수정일시 |
| `del_yn` | `CHAR(1)` | N |  | 삭제여부 |
| `scnr_id` | `INT UNSIGNED` | N | IX | 대상 시나리오ID (`tmm_mi_scenario_m.scenario_id` 논리참조) |
| `req_usr_no` | `INT UNSIGNED` | N |  | 요청사용자번호(소유자) |
| `exec_prep_stat_cd` | `VARCHAR(30)` | N |  | 파라미터제안기반실행준비상태코드 (Backend orchestration status). 최장값 `PREPARING_EXECUTION`(19자) + 헤드룸 |
| `agnt_prpsl_id` | `INT UNSIGNED` | Y | IX | 에이전트제안ID. 선행 생성 시 NULL, 응답/callback에서 row 단위 write-once 저장. 반복 실행으로 중복 허용 |
| `case_id` | `INT UNSIGNED` | Y | UK | 시나리오케이스ID. 실행 접수 성공 후 기존 `tmd_mdl_scnrcasem` Case를 가리키는 포인터 |
| `actv_scnr_id` | `INT UNSIGNED` | Y | UK | 활성시나리오ID. STORED generated column, active(in-flight) 유일성 helper |
| `err_msg_cntt` | `VARCHAR(1000)` | Y |  | 오류메시지내용 (실패/timeout 요약) |

컬럼 순서는 규칙(`PK -> 시스템 컬럼 -> 일반 컬럼`)을 따른다. `actv_scnr_id`는 참조 컬럼(`scnr_id`, `exec_prep_stat_cd`, `del_yn`) 이후에 정의한다.

Nullable 판단:

- `scnr_id`: preparation은 항상 시나리오를 대상으로 하므로 NOT NULL. (`ScenarioCase.scnr_id`는 INDEPENDENT 케이스 때문에 nullable이지만 여기서는 해당 없음)
- `agnt_prpsl_id`: 선행 `REQUESTING` insert 시점엔 미확정이므로 nullable. Agent 응답/callback에서 row 단위 write-once로 채운다. 같은 proposal로 반복 실행할 수 있으므로 중복을 허용한다.
- `case_id`: 실행 접수 전까지 nullable. 실행 접수 성공 후 새 `case_id`를 저장한다.
- `actv_scnr_id`: active(in-flight) 상태 + 미삭제일 때만 값을 가진다. 그 외 NULL. 표준단어 `ACTV`(활성) + `SCNR`(시나리오)를 사용한다.
- `err_msg_cntt`: 실패/운영 추적 정보가 있을 때만 저장.

Unique/index 판단:

- `tmd_mi_model_execpreph_ux01(case_id)`: 하나의 기존 Case가 둘 이상의 execution-preparation row에 연결되지 않도록 보강. nullable unique.
- `tmd_mi_model_execpreph_ux02(actv_scnr_id)`: 시나리오당 active(in-flight) preparation 최대 1건 invariant를 DB에서 강제. nullable unique.
- `tmd_mi_model_execpreph_ix01(scnr_id, del_yn, exec_prep_id DESC)`: 시나리오별 최신 preparation 조회 및 목록. PK가 AUTO_INCREMENT이므로 `exec_prep_id DESC`가 최신 정렬을 대체해 별도 timestamp index를 두지 않는다.
- `tmd_mi_model_execpreph_ix02(agnt_prpsl_id, exec_prep_stat_cd, exec_prep_id DESC)`: 같은 Agent proposal의 반복 실행 준비 이력 조회와 proposal callback 후보(`REQUESTING`/`PROPOSAL_PENDING`) 검색을 지원한다. `agnt_prpsl_id`는 high-selectivity이며 unique가 아니다.
- 저카디널리티 `del_yn`, `exec_prep_stat_cd` 단독 index는 두지 않는다.

### In-flight 유일성 (generated column)

```sql
actv_scnr_id INT UNSIGNED
  GENERATED ALWAYS AS (
    IF(exec_prep_stat_cd IN ('REQUESTING','PROPOSAL_PENDING','CONFIRMING','PREPARING_EXECUTION')
       AND del_yn = 'N', scnr_id, NULL)
  ) STORED
```

- in-flight 상태: `REQUESTING`, `PROPOSAL_PENDING`, `CONFIRMING`, `PREPARING_EXECUTION`.
- `EXECUTING`/terminal(`EXECUTION_FAILED`, `REQUEST_FAILED`) 또는 `del_yn='Y'`이면 `NULL` → in-flight 집합에서 제외.
- `del_yn='N'` 조건이 없으면 재실행 흐름에서 soft delete된 진행-상태 row가 유령 충돌을 일으킬 수 있으므로 반드시 포함한다.
- 기존 `tmd_mdl_scnrcasem.actv_scnr_id`(`if(case_stat_cd='RUNNING', scnr_id, null) stored`) 패턴과 동일 계열.

## Lifecycle / Status Codes

`exec_prep_stat_cd` 허용값(Backend orchestration, monotonic):

| 코드 | 의미 | in-flight |
|---|---|:---:|
| `REQUESTING` | 생성·commit 완료, Agent 응답/callback 대기 | O |
| `PROPOSAL_PENDING` | Agent 제안 생성/사용자 검토 대기 (Agent `PROCESSING`/`PROPOSED` 포괄) | O |
| `CONFIRMING` | 사용자 실행 요청 검증 + Agent confirmation | O |
| `PREPARING_EXECUTION` | confirmation 성공, Excel 3개 생성(confirmed params 파생, 아직 case 미생성) | O |
| `EXECUTING` | 기존 model execution 접수 완료(case INSERT → `tcm_filel(SCNRCASE+case_id)` 기록 → dispatch → `execprep.case_id` 저장). 이후 실행 상태/terminal 이력은 Case/Run이 관리 | X |
| `EXECUTION_FAILED` | confirmation 이후 실행 접수 실패 | X |
| `REQUEST_FAILED` | Agent 제안 요청 실패가 확정됐거나 timeout/reconcile로 proposal 부재가 확정됨 | X |

Agent 상태(`PROCESSING/PROPOSED/CONFIRMED/FAILED`)는 `tmm_mi_model_param_proposal_m`이 source of truth이며 이 테이블에 복제하지 않는다. 상태 코드 범위와 전이 invariant는 DB CHECK가 아니라 application validation으로 강제한다(선례 `tmd_scnrcrth`와 동일).

## Application Transaction Rules

선행 생성(동시 요청 직렬화 + in-flight guard + RUNNING pre-check):

```sql
-- MiScenario pessimistic lock으로 직렬화한 뒤 insert.
-- (A) RUNNING pre-check: 같은 시나리오에 진행 중인 수급모델 실행이 있으면 새 파라미터 생성을 진입에서 막는다.
--     Agent 재계산·사용자 재확정 사이클 낭비를 방지하는 friendly fast-fail이다(최종 접수 unique는 backstop으로 유지).
SELECT 1
FROM tmd_mdl_scnrcasem
WHERE scnr_id = :scnrId
  AND case_stat_cd = 'RUNNING'
  AND del_yn = 'N'
LIMIT 1;
-- 존재하면 409 "실행 중이라 재시작 불가"로 fast-fail하고 아래 insert를 수행하지 않는다.
-- tmd_mdl_scnrcasem_ix02(scnr_id, case_stat_cd, case_id) 사용.

-- (B) in-flight guard: 같은 시나리오에 in-flight preparation이 있으면 ux02(actv_scnr_id) 위반으로 차단된다.
INSERT INTO tmd_mi_model_execpreph
    (crt_usr_logn_id, mod_usr_logn_id, scnr_id, req_usr_no, exec_prep_stat_cd)
VALUES
    (:actor, :actor, :scnrId, :reqUsrNo, 'REQUESTING');
```

- RUNNING pre-check와 `REQUESTING` insert는 반드시 동일 MiScenario lock 스코프에서 수행해 in-flight 가드(ux02)와 일관성을 맞춘다.
- pre-check와 최종 실행 접수 사이 TOCTOU(그 틈에 새 run 시작)는 기존 `tmd_mdl_scnrcasem.actv_scnr_id`(RUNNING) UNIQUE가 최후 방어선으로 막는다. pre-check는 UX/비용용 조기 차단, unique는 정합성 보장용 backstop이다.
- `EXECUTING`은 execprep in-flight 집합에서 제외되므로(재실행 유리), 이전 준비 row가 EXECUTING이어도 execprep ux02는 새 준비를 막지 않는다. 이전 실행이 아직 RUNNING인 경우의 차단 책임은 이 RUNNING pre-check + `tmd_mdl_scnrcasem` unique가 진다.

Agent proposal ID write-once 저장(응답/callback 공통):

```sql
UPDATE tmd_mi_model_execpreph
SET agnt_prpsl_id = :agentProposalId,
    mod_usr_logn_id = :actor,
    mod_dtm = CURRENT_TIMESTAMP
WHERE exec_prep_id = :execPrepId
  AND agnt_prpsl_id IS NULL;
-- 0건이면 재조회: 같은 ID면 no-op 수렴, 다른 ID면 correlation conflict(덮어쓰지 않음).
```

Callback correlation:

- callback `data.modelParamProposalId` 또는 Agent 테이블 재조회로 확인한 proposal ID가 있더라도 `agnt_prpsl_id` 단독 lookup은 하지 않는다. 같은 proposal ID로 반복 실행 row가 생길 수 있으므로 proposal 생성 callback은 `exec_prep_stat_cd IN ('REQUESTING','PROPOSAL_PENDING')`인 row만 후보로 삼는다.
- `agnt_prpsl_id`가 아직 저장되지 않은 row라면 `exec_prep_id` + `agnt_prpsl_id IS NULL` 조건으로 write-once 저장한다.
- proposal ID가 없거나 실패 callback의 `data`가 비어 있으면 path `scenarioId`의 현재 in-flight row(`actv_scnr_id = :scnrId`)가 정확히 1건일 때만 처리한다.
- 현재 in-flight row가 없으면 late/no-op으로 처리하고, terminal/history row나 새 unrelated row에 붙이지 않는다.
- 동일 row에 이미 다른 `agnt_prpsl_id`가 있으면 correlation conflict로 보고 덮어쓰지 않는다.

상태 전이(compare-and-set, monotonic guard 예시):

```sql
-- REQUESTING/PROPOSAL_PENDING 실패
UPDATE tmd_mi_model_execpreph
SET exec_prep_stat_cd = 'REQUEST_FAILED',
    err_msg_cntt = :errMsg,
    mod_usr_logn_id = :actor, mod_dtm = CURRENT_TIMESTAMP
WHERE exec_prep_id = :execPrepId
  AND exec_prep_stat_cd IN ('REQUESTING', 'PROPOSAL_PENDING');
```

`REQUEST_FAILED` 전이는 Agent가 callback을 보낼 수 없는 실패가 확정됐거나 timeout/reconcile로 Agent proposal 부재가 확정된 경우에만 수행한다. Agent 호출 결과가 "Agent 접수 실패"인지 "Agent는 접수했지만 응답만 유실"인지 불명확하면 row를 `REQUESTING` in-flight 상태로 유지해 `tmd_mi_model_execpreph_ux02(actv_scnr_id)`가 새 preparation 생성을 막게 한다. 이후 callback, Agent table read-reconcile, timeout 복구가 최종 outcome을 결정한다.

JSON 실행 접수 성공(해당 실행 준비 row에 새 Case 연결):

```sql
UPDATE tmd_mi_model_execpreph
SET case_id = :caseId,
    exec_prep_stat_cd = 'EXECUTING',
    mod_usr_logn_id = :actor, mod_dtm = CURRENT_TIMESTAMP
WHERE exec_prep_id = :execPrepId
  AND exec_prep_stat_cd = 'PREPARING_EXECUTION';
```

- Excel 3개 세트 생성은 `PREPARING_EXECUTION`에서 선행하고(confirmed params 파생, case_id 불필요), 파일 3개는 `EXECUTING` 전이 트랜잭션에서 발급된 `case_id`로 `tcm_filel(SCNRCASE+case_id)`에 기록한다. 즉 `EXECUTING` 전이 = `case INSERT → tcm_filel 기록 → dispatch → execprep.case_id 저장` 순서이며, 기존 `mi-scenario-model-execution-workflow-design.md` B.실행 접수 흐름과 동형이다(업로드 수신 바이트 대신 생성 바이트를 사용).
- 실행 1회마다 `tmd_mdl_scnrcasem`에 새 `case_id`를 만들고, 그 아래 `tmd_mdl_scnrcaseh`에 run history를 만든다.
- `tmd_mi_model_execpreph` row 1건은 실행 준비 1회를 대표하므로 정상 실행 접수 성공 시 하나의 `case_id`만 연결된다. 같은 `agnt_prpsl_id`로 재실행하면 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만든다.
- terminal 상태를 되돌리는 late callback은 위 guard( `IN (...)` 선행상태 조건 )로 no-op 수렴한다.
- SSE는 write commit 이후 발행한다.

## Normalization Assessment

| 정규형 | 판단 |
|---|---|
| 1NF | 모든 컬럼이 atomic. proposal parameter array/JSON을 저장하지 않는다(Agent 테이블 read). |
| 2NF | 단일 PK `exec_prep_id`에 non-key 컬럼이 완전 종속. |
| 3NF | Agent 상태·proposal 데이터를 복제하지 않아 이행 종속이 없다. `actv_scnr_id`는 파생값이지만 STORED generated column으로 명시적 유일성 helper 역할만 한다. |
| BCNF | `exec_prep_id`, `case_id`, `actv_scnr_id`는 각각 candidate key 성격(뒤 2개는 nullable unique, 값이 있을 때 determinant). `agnt_prpsl_id`는 반복 실행 때문에 determinant가 아니며 일반 참조 index다. |

의도적 비정규화:

- `err_msg_cntt`는 오류를 child table로 분리하지 않고 header 요약 메시지로 둔다(선례 `tmd_scnrcrth.err_msg_cntt`와 동일).

## Physical DDL

```sql
/* =========================================================
   MI 파라미터 제안 Execution-Preparation DDL draft
   Date: 2026-07-08
   Scope: Backend orchestration 단일 테이블
   Canonical table: tmd_mi_model_execpreph
   ========================================================= */

/* =========================================================
   1. pre-check
   ========================================================= */

-- 1-1. target table collision
SHOW TABLES LIKE 'tmd_mi_model_execpreph';

-- 1-1b. legacy draft table name drift check
-- 이전 draft명 tmd_execpreph가 이미 적용된 환경이면 신규 CREATE 대신 2c RENAME 보정을 수행한다.
SHOW TABLES LIKE 'tmd_execpreph';

-- 1-2. object name collision
SELECT TABLE_NAME, INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmd_mi_model_execpreph'
ORDER BY INDEX_NAME;

-- 1-2b. legacy/generated helper column name drift check
-- 기대: 신규 적용 전이면 0건, 이미 적용된 환경이면 actv_scnr_id만 존재해야 한다.
-- inflight_scnr_id가 있으면 표준단어 미준수 이름이므로 2a 보정 DDL을 먼저 적용한다.
SELECT COLUMN_NAME, GENERATION_EXPRESSION, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmd_mi_model_execpreph'
  AND COLUMN_NAME IN ('actv_scnr_id', 'inflight_scnr_id');

-- 1-3. 논리참조 대상 존재/키 타입 확인 (물리 FK는 걸지 않음)
SHOW TABLES LIKE 'tmm_mi_scenario_m';
SHOW TABLES LIKE 'tmd_mdl_scnrcasem';
SHOW TABLES LIKE 'tmm_mi_model_param_proposal_m';

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
       (TABLE_NAME = 'tmm_mi_scenario_m'             AND COLUMN_NAME = 'scenario_id')
    OR (TABLE_NAME = 'tmd_mdl_scnrcasem'             AND COLUMN_NAME = 'case_id')
    OR (TABLE_NAME = 'tmm_mi_model_param_proposal_m' AND COLUMN_NAME = 'model_param_proposal_id')
  );
-- 기대: 셋 다 int unsigned. scnr_id/case_id/agnt_prpsl_id 타입 정합 확인.
-- 주: tmm_mi_model_param_proposal_m DDL/read 권한은 feature spec 기준 확정이다.
--     본 테이블은 물리 FK가 없으므로 Agent proposal 테이블 배치와 독립적으로 생성 가능하다.
--     DBA 적용 전에는 동일 schema 배치와 key type 정합만 pre-check로 확인한다.

/* =========================================================
   2. apply
   ========================================================= */

CREATE TABLE `tmd_mi_model_execpreph` (
  `exec_prep_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '파라미터제안기반실행준비ID',
  `crt_usr_logn_id` varchar(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성사용자로그인ID',
  `crt_dtm` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `mod_usr_logn_id` varchar(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정사용자로그인ID',
  `mod_dtm` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  `del_yn` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부',
  `scnr_id` int unsigned NOT NULL COMMENT '시나리오ID (tmm_mi_scenario_m.scenario_id 논리참조, 물리 FK 없음)',
  `req_usr_no` int unsigned NOT NULL COMMENT '요청사용자번호',
  `exec_prep_stat_cd` varchar(30) NOT NULL COMMENT '파라미터제안기반실행준비상태코드',
  `agnt_prpsl_id` int unsigned DEFAULT NULL COMMENT '에이전트제안ID (tmm_mi_model_param_proposal_m.model_param_proposal_id 논리참조, row 단위 write-once, 반복 실행 중복 허용)',
  `case_id` int unsigned DEFAULT NULL COMMENT '시나리오케이스ID (tmd_mdl_scnrcasem.case_id 논리참조, 실행 접수 후 포인터)',
  `actv_scnr_id` int unsigned
    GENERATED ALWAYS AS (
      if(`exec_prep_stat_cd` in ('REQUESTING','PROPOSAL_PENDING','CONFIRMING','PREPARING_EXECUTION')
         and `del_yn` = 'N', `scnr_id`, NULL)
    ) STORED
    COMMENT '활성시나리오ID (in-flight 이고 미삭제이면 scnr_id, 그 외 NULL. scenario 당 in-flight 1건 unique helper)',
  `err_msg_cntt` varchar(1000) DEFAULT NULL COMMENT '오류메시지내용',
  PRIMARY KEY (`exec_prep_id`)
) COMMENT='TMD_파라미터제안기반수급모델실행준비이력';

CREATE UNIQUE INDEX `tmd_mi_model_execpreph_ux01`
    ON `tmd_mi_model_execpreph` (`case_id`);

CREATE UNIQUE INDEX `tmd_mi_model_execpreph_ux02`
    ON `tmd_mi_model_execpreph` (`actv_scnr_id`);

CREATE INDEX `tmd_mi_model_execpreph_ix01`
    ON `tmd_mi_model_execpreph` (`scnr_id`, `del_yn`, `exec_prep_id` DESC);

CREATE INDEX `tmd_mi_model_execpreph_ix02`
    ON `tmd_mi_model_execpreph` (`agnt_prpsl_id`, `exec_prep_stat_cd`, `exec_prep_id` DESC);

/* =========================================================
   2a. correction for already-applied legacy draft only
   ========================================================= */

-- 이미 inflight_scnr_id로 적용된 환경에서만 수행한다.
-- generated helper 컬럼은 파생값이므로 데이터 backfill은 필요 없다.
-- MySQL 버전에 따라 DROP INDEX 구문이 ALTER TABLE DROP INDEX와 호환되는지 DBA가 확인한다.
-- 기존 draft가 case_id용 ux02를 함께 갖고 있으면 2b correction으로 index 이름을 먼저 정리한다.
-- ALTER TABLE `tmd_mi_model_execpreph`
--   DROP INDEX `tmd_mi_model_execpreph_ux03`,
--   DROP COLUMN `inflight_scnr_id`,
--   ADD COLUMN `actv_scnr_id` int unsigned
--     GENERATED ALWAYS AS (
--       if(`exec_prep_stat_cd` in ('REQUESTING','PROPOSAL_PENDING','CONFIRMING','PREPARING_EXECUTION')
--          and `del_yn` = 'N', `scnr_id`, NULL)
--     ) STORED
--     COMMENT '활성시나리오ID (in-flight 이고 미삭제이면 scnr_id, 그 외 NULL. scenario 당 in-flight 1건 unique helper)'
--     AFTER `agnt_prpsl_id`;
--
-- CREATE UNIQUE INDEX `tmd_mi_model_execpreph_ux02`
--     ON `tmd_mi_model_execpreph` (`actv_scnr_id`);

/* =========================================================
   2b. correction for already-applied legacy unique/index draft only
   ========================================================= */

-- 이미 agnt_prpsl_id unique 또는 case_id unique 이름이 현재 canonical과 다른 환경에서만 수행한다.
-- agnt_prpsl_id는 반복 실행 때문에 unique가 아니며, case_id는 row당 실행 Case 1건 포인터라 unique다.
-- 실제 적용 전 SHOW INDEX 결과에 맞춰 중복 index 이름을 DBA가 확인한다.
-- ALTER TABLE `tmd_mi_model_execpreph`
--   DROP INDEX `tmd_mi_model_execpreph_ux01`;
-- CREATE INDEX `tmd_mi_model_execpreph_ix02`
--   ON `tmd_mi_model_execpreph` (`agnt_prpsl_id`, `exec_prep_stat_cd`, `exec_prep_id` DESC);
--
-- 기존 `tmd_mi_model_execpreph_ux02(case_id)`를 `tmd_mi_model_execpreph_ux01(case_id)`로 맞춰야 하는 경우:
-- ALTER TABLE `tmd_mi_model_execpreph`
--   DROP INDEX `tmd_mi_model_execpreph_ux02`;
-- CREATE UNIQUE INDEX `tmd_mi_model_execpreph_ux01`
--   ON `tmd_mi_model_execpreph` (`case_id`);

/* =========================================================
   2c. correction for already-applied legacy table name only
   ========================================================= */

-- 이전 draft명 tmd_execpreph가 이미 적용된 환경에서만 수행한다(1-1b가 tmd_execpreph를 찾은 경우).
-- 신규 CREATE 대신 테이블과 인덱스명을 DBA 승인명 tmd_mi_model_execpreph로 rename한다.
-- generated column/데이터는 그대로 보존된다.
-- ALTER TABLE `tmd_execpreph` RENAME TO `tmd_mi_model_execpreph`;
-- MySQL은 RENAME TABLE 시 인덱스가 자동 이동하지만 인덱스명은 바뀌지 않으므로,
-- 표준 정합을 위해 인덱스명도 함께 맞춘다(버전별 RENAME INDEX 지원 여부는 DBA 확인).
-- ALTER TABLE `tmd_mi_model_execpreph`
--   RENAME INDEX `tmd_execpreph_ux01` TO `tmd_mi_model_execpreph_ux01`,
--   RENAME INDEX `tmd_execpreph_ux02` TO `tmd_mi_model_execpreph_ux02`,
--   RENAME INDEX `tmd_execpreph_ix01` TO `tmd_mi_model_execpreph_ix01`,
--   RENAME INDEX `tmd_execpreph_ix02` TO `tmd_mi_model_execpreph_ix02`;

/* =========================================================
   3. backfill
   ========================================================= */

-- 신규 테이블. backfill 없음. 기존 mock/API-only 상태는 이관하지 않는다.

/* =========================================================
   4. rollback
   ========================================================= */

DROP TABLE IF EXISTS `tmd_mi_model_execpreph`;

/* =========================================================
   5. verify
   ========================================================= */

SHOW CREATE TABLE `tmd_mi_model_execpreph`;

SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmd_mi_model_execpreph'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- generated column 정의 확인
SELECT COLUMN_NAME, GENERATION_EXPRESSION, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmd_mi_model_execpreph'
  AND COLUMN_NAME = 'actv_scnr_id';

-- legacy column name should not remain
SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmd_mi_model_execpreph'
  AND COLUMN_NAME = 'inflight_scnr_id';

-- in-flight unique 동작 smoke test (동일 시나리오 2건 진행 시 2번째가 ux02 위반이어야 함)
-- INSERT INTO tmd_mi_model_execpreph (crt_usr_logn_id, mod_usr_logn_id, scnr_id, req_usr_no, exec_prep_stat_cd)
--   VALUES ('SYSTEM','SYSTEM', 1, 1, 'REQUESTING');
-- INSERT INTO tmd_mi_model_execpreph (crt_usr_logn_id, mod_usr_logn_id, scnr_id, req_usr_no, exec_prep_stat_cd)
--   VALUES ('SYSTEM','SYSTEM', 1, 1, 'REQUESTING');  -- expect: Duplicate entry for key 'tmd_mi_model_execpreph_ux02'

-- 시나리오별 최신 preparation 조회 실행계획
EXPLAIN
SELECT exec_prep_id, exec_prep_stat_cd, agnt_prpsl_id, case_id
FROM tmd_mi_model_execpreph
WHERE scnr_id = 1
  AND del_yn = 'N'
ORDER BY exec_prep_id DESC
LIMIT 1;

-- 최근 수급모델 실행에 사용된 execution-preparation 조회
EXPLAIN
SELECT ep.exec_prep_id, ep.agnt_prpsl_id, ep.case_id
FROM tmd_mdl_scnrcasem c
JOIN tmd_mi_model_execpreph ep
  ON ep.case_id = c.case_id
WHERE c.scnr_id = 1
  AND c.exec_type_cd = 'SCENARIO'
  AND c.del_yn = 'N'
  AND ep.del_yn = 'N'
ORDER BY c.case_id DESC
LIMIT 1;

/* =========================================================
   6. DBA action / deployment note
   ========================================================= */

-- 1. ddl-auto=validate 매핑 전에 이 DDL을 수동 배포한다.
-- 2. 물리 FK는 걸지 않는다 (scnr_id/agnt_prpsl_id/case_id 모두 논리 참조).
-- 3. 상태 코드 범위/전이 invariant는 application validation으로 강제한다 (DB CHECK 미적용).
-- 4. agnt_prpsl_id는 Agent PK와 동일 int unsigned. Agent proposal 테이블 DDL은 확정이고 read 권한도 확보됐다.
--    DBA 적용 전 동일 schema 배치와 key type 정합만 pre-check로 확인한다(본 테이블 생성과는 독립).
-- 5. 이미 legacy draft의 inflight_scnr_id로 반영된 환경은 2a 보정 DDL로 actv_scnr_id에 맞춘 뒤 application mapping을 배포한다.
-- 6. 이미 legacy draft의 agnt_prpsl_id UNIQUE로 반영된 환경은 2b 보정 DDL로 non-unique index에 맞춘다.
-- 7. 이미 legacy draft명 tmd_execpreph로 반영된 환경은 신규 CREATE 대신 2c RENAME 보정으로 tmd_mi_model_execpreph에 맞춘다.
-- 8. retry_cnt/추가 lifecycle timestamp는 재시도 정책(P1) 확정 시 append-only로 추가한다.
```

## Rollout Plan

1. Pre-check
   - `tmd_mi_model_execpreph` 이름/인덱스 충돌 확인.
   - 이전 draft명 `tmd_execpreph`가 이미 적용된 환경이면 신규 CREATE 대신 2c RENAME 보정을 적용.
   - generated helper 컬럼이 표준명 `actv_scnr_id`인지 확인. `inflight_scnr_id`가 있으면 legacy drift로 보고 2a 보정 DDL을 적용.
   - 이미 `agnt_prpsl_id` unique로 적용된 환경이면 반복 실행을 막는 legacy drift로 보고 2b 보정 DDL을 적용.
   - 이미 `case_id` unique가 `tmd_mi_model_execpreph_ux02` 이름으로 적용된 환경이면 2b 보정으로 canonical index 이름(`ux01`)을 맞춘다.
   - 논리참조 대상 3개 테이블의 키 컬럼이 `int unsigned`인지 확인.
   - Agent proposal 테이블 DDL/read 권한은 확정. 물리 FK가 없으므로 `tmd_mi_model_execpreph` 생성은 독립 가능하며, 동일 schema 배치와 key type 정합만 확인.

2. Apply
   - `tmd_mi_model_execpreph` 생성 → `ux01~ux02`, `ix01~ix02` 생성.
   - generated column `actv_scnr_id` 정의가 STORED로 반영됐는지 확인.
   - 이미 `inflight_scnr_id`로 적용된 환경이면 테이블 재생성이 아니라 2a 보정 DDL로 unique helper 컬럼명만 정정.
   - 이미 `agnt_prpsl_id` unique로 적용된 환경이면 2b 보정 DDL로 non-unique index로 전환한다.

3. Backfill
   - 없음.

4. Application rollout
   - 선행 `REQUESTING` insert를 MiScenario pessimistic lock 뒤에 수행, in-flight guard(ux02)로 동시 요청 차단.
   - 같은 lock 스코프에서 `tmd_mdl_scnrcasem` RUNNING pre-check를 먼저 수행해, 진행 중 실행이 있으면 새 파라미터 생성을 진입에서 409로 fast-fail한다. 최종 접수의 `tmd_mdl_scnrcasem.actv_scnr_id` UNIQUE는 race backstop으로 유지.
   - Agent 응답/callback에서 row 단위 `agnt_prpsl_id` write-once 저장, monotonic compare-and-set 상태 전이.
   - Agent 접수 여부가 불명확한 실패는 `REQUEST_FAILED`로 닫지 않고 `REQUESTING` in-flight 상태를 유지해 callback/read-reconcile/timeout 복구로 수렴.
   - JSON execution 접수 성공 시 해당 `tmd_mi_model_execpreph` row에 새 `case_id`를 연결한다. 같은 Agent proposal 재실행은 새 `tmd_mi_model_execpreph` row와 새 `case_id`를 만든다.

5. Verify
   - `SHOW CREATE TABLE`로 nullable/unique/index/generated 정의 확인.
   - in-flight unique smoke test로 시나리오당 진행 1건 강제 확인.
   - 최신 preparation 조회 `EXPLAIN`에서 `tmd_mi_model_execpreph_ix01` 사용 확인.

6. Rollback
   - application rollback으로 신규 write 중지.
   - 신규 데이터 보존 필요 여부 확인 후 `DROP TABLE` 또는 rename 격리.

## Open Risks / Handoff

- table/subject 물리명(`tmd_mi_model_execpreph` / `md`)은 DBA·erd-change-review 승인 완료(이전 draft `tmd_execpreph`에서 변경). 이미 `tmd_execpreph`로 적용된 환경은 2c RENAME 보정을 적용한다.
- Agent proposal 테이블(`tmm_mi_model_param_proposal_m`) DDL은 feature spec 기준 확정이고 read 권한도 확보됐다.
- DBA 적용 전에는 동일 schema 배치·키 타입만 pre-check로 확인한다(본 테이블 생성과는 독립).
- `agnt_prpsl_id`는 반복 실행 때문에 unique가 아니어야 한다. 이미 unique로 적용된 환경은 보정 전 반복 실행이 막힌다.
- `case_id`는 row당 실행 Case 1건 포인터로 사용한다. 이미 `case_id` 컬럼이 적용된 환경은 컬럼을 유지하고 index 이름/정의만 canonical에 맞춘다.
- 값 조정 재실행은 Option A(같은 `model_param_proposal_id` 재확정, `confirmation_parameters` 갱신)로 확정했다. Agent 확정 API가 이미 `CONFIRMED`인 proposal의 재확정을 허용하도록 Agent가 수정 확정해(2026-07-09 회신) Option A 전제가 충족됐다. 과거 실행별 확정값을 정확히 복원해야 하는 요구가 확정되면 Agent 단일 snapshot으로 부족하므로 `tmd_mi_model_execpreph`에 실행별 confirmed parameter JSON snapshot 추가를 별도 ERD로 검토한다(fallback).
- `retry_cnt`, 확정/실행 timestamp 컬럼은 재시도 정책(P1) 확정 후 append 검토.
- 상태 invariant가 DB CHECK가 아니라 application validation/integration test에 의존한다.
- erd-change-review 대상: 논리 참조의 FK 미적용 타당성, nullable unique 2종, non-unique `agnt_prpsl_id` 반복 실행 index, generated column `del_yn` 포함 식.
- legacy draft가 이미 `inflight_scnr_id`로 적용된 환경은 application 구현 전에 `actv_scnr_id`로 정정해야 한다.

## Implementation Readiness

- ready for Agent create/callback/read API implementation by slice.
- ready for JSON execution linkage after DBA index correction: `case_id` 포인터로 최신 실행 preparation/파일 다운로드 조회 경로가 확정됐다.
- table/subject 물리명(`tmd_mi_model_execpreph` / `md`)은 DBA 승인 완료로 canonical DDL 적용 가능. 이미 legacy `tmd_execpreph` 테이블명, `agnt_prpsl_id` unique 또는 `inflight_scnr_id`가 적용된 환경은 2a/2b/2c 보정 방침을 함께 확인한다.
