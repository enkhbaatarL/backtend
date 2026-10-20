# MI 모델 Input 화면 편집 Delivery Plan

## Document Control

- status: Draft
- canonical for: 배포 일정, 로컬 delivery 순서, 티켓 경계, 의존성, 상태와 agent coordination
- owner: BE
- source documents:
  - 2026-10-16 사용자 결정: 공통 요청 처리 → Draft 생성·재개·조회를 첫 구현 묶음으로 채택하고 이후 구현을 승인했다. 현재 범위는 §7A, 실행·검증 결과는 기존 작업 노트를 따른다.
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md`
  - `docs/references/mi-model-input-catalog-inventory.md`
  - `docs/references/mi-model-input-calculation-engine-spike.md`
  - `docs/references/mi-model-input-sheet-calculation-compatibility.md`
  - `docs/references/mi-model-input-catalog-inventory-2026-09-28.md`
  - `docs/references/mi-model-input-baseline-2026-10-07.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md`
- supersedes: 없음
- last synced: 2026-10-19 (F-O-01 실환경 기본 성공 흐름·C DRAFT 등록과 후속 검증 구분)
- open questions:
  - F-S-06 제외 INPUT-02A는 §6A의 `DONE (CONDITIONAL)`이다. 계산/격리 비동기·운영 기준은 domain Q7·규칙63/64를 따른다. 공통 저장 구조/최종저장·복구 계약은 domain §3.4·§5.1~5.3/DDL §6.3·편집 API §9로 문서 범위 완료다. 표별 정책·DBA 적용 준비/제품 구현·INPUT-10의 API 전체 저장 응답 검증은 후속이다. 지정 사례 밖 계산 allowlist를 자동 승인하지 않는다. GDP/PIPELINE 오탐·UNIQUE 5셀 보존 판정은 유지한다.

## 1. Purpose and source-of-truth boundary

이 문서는 독립수급모델 화면 편집 기능을 로컬 Epic과 티켓으로 관리하기 위한 delivery plan이다.
무엇을 어떤 순서로 끝낼지는 이 문서가 관리하지만, 아래 사실의 canonical source는 별도 문서다.

| 사실 | Canonical source |
| --- | --- |
| business rule, Draft lifecycle, 최종저장·Case·Run 경계 | `mi-model-input-template-catalog-domain-model-design.md` |
| endpoint, DTO, validation, error | `docs/feature-specs/*`와 OpenAPI |
| table, column, key, index, DBA rollout | `mi-model-input-template-catalog-erd-ddl-draft.md` |
| 조사·구현·검증 결과 | 각 티켓의 stable work note |

이 문서에 계약이나 스키마 세부를 복사해 별도 source of truth로 만들지 않는다.

### 배포 일정 — 2026-10-07 사용자 지정

| 구분 | 일정 | 확정 수준 |
| --- | --- | --- |
| 개발 환경 배포 | 2026-10-19 전후 | 잠정 일정 |
| 운영 배포 | 2026-10-22 | 필수 배포 기한 |

위 일정은 독립수급모델 입력 편집 기능의 배포 계획이다. 티켓 완료·검증·실제 배포 상태는 별도로 관리한다.

### 기준본 고정 상태 — 2026-10-07

- 검증용 기준본 고정 완료: 사용자 지정 10월 6일 전달본 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6의 원본 snapshot과 정규화본을 보관했다. 개발 전용 8개 시트를 제거하고 업무 70개 시트와 유지 package bytes 보존을 검증했다. 정확한 파일·hash·재현 근거는 `mi-model-input-baseline-2026-10-07.md` 및 manifest를 따른다.
- INPUT-02A의 계산/보존 비교 source는 이 정규화본이다. 판별대장의 DEMAND·OUTPUT·TRADE v3 좌표·색·수식 관측은 v8 재대조가 필요하다. 기준본 고정 완료를 계산 엔진 GO나 운영 template DB 등록으로 해석하지 않는다.

## 2. Fixed delivery boundary

- 이번 release는 기존 값 `SET/CLEAR`와 요청된 구조 변경을 함께 포함한다. 구조 변경을 후속 release로 일괄 미루지 않는다.
- 주된 추가 방식과 개별 중간 삽입·삭제 예외는 domain design §6 규칙 13·26·45·47을 따른다. 표별 허용 범위·참조 갱신 계약과 실파일 검증을 완료한 동작을 배포한다.
- 화면 action은 `임시저장`과 `최종저장`이며 별도 미리보기는 없다.
- 임시저장은 Draft와 revision을 저장하고 계산 허용 수식으로 영향받는 workbook 값을 재계산한다.
- 외부 workbook 참조 수식은 domain design §6 규칙 54의 표별 계산 예외를 제외하고 source snapshot의 formula·cached value·externalLink/package를 보존한다. 예외 셀은 고정 외부 데이터 캐시를 사용해 재계산하며 외부 링크는 갱신하지 않는다. 이전 3파일 실측 42,394개는 역사적 수치이며, OUTPUT 외부 링크를 포함한 최신 대상은 기준본 정규화 source로 다시 집계한다.
- SUPPLY·TRADE 외부 Excel 원본은 현재와 향후 운영 모두 제공받지 않는다. 업무담당자 요청, 파일 반입, snapshot/version 확보를 티켓 선행조건이나 blocker로 만들지 않는다.
- 최종저장은 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` XLSM 4개를 생성하고 새 `INDEPENDENT` Case를 만든다. Run은 별도 요청이다.
- 파라미터 제안의 파일 생성 로직은 재사용하지 않는다. 생성 결과 bytes를 `MultipartFile`로 바꾸는 adapter 패턴만 재사용한다.
- 기존 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT` 보완이나 template mapping 없이 cutover 때 case master만
  soft delete해 화면과 active API에서 제외한다. 파일·run history·결과는 hard delete하지 않는다.
- 첨부본의 `SUMMARY`, `플랫폼 입력 안내`는 개발 전용이다. runtime catalog, Draft projection,
  최종 XLSM과 Job input에는 넣지 않는다. 별도 운영 파일은 수신하지 않으며 현재 개발본에서 개발 전용 시트를
  제거한 정규화본으로 template hash와 source snapshot을 만든다.
- 수식·직접 참조가 없는 visible sheet 6개는 사용자 수정, Draft 변경, dependency와 Backend workbook 재계산에서
  제외하고 최종 XLSM에는 원본 상태로 보존한다. read-only 표시는 SUPPLY `LNG Summary` 1개만 source 값을 제공한다.
  TRADE `backupLNG_TradeRestrictionInput`과 값·수식이 없는 구분 탭 4개는 화면에 표시하지 않는다(domain 규칙 43·57).
- TRADE `4.5 PriceModelInput`은 domain 규칙 44에 따라 T-183/T-184의 입력 편집·아래 행 추가를 반영한다.
  미표시 T-177~T-182와 U열의 화면 제외 및 승인 변경 밖 원본 보존은 규칙 44·57을 따른다.

위 규칙의 상세와 변경은 domain design에서만 수행한다.

## 3. Ticket status and coordination rules

### Status

| 상태 | 의미 |
| --- | --- |
| `READY` | 선행조건과 입력 문서가 준비되어 agent에게 바로 배정 가능 |
| `IN_PROGRESS` | 담당 agent와 stable work note가 있고 실행 중 |
| `BLOCKED` | 선행 티켓 결과나 material decision이 필요 |
| `BACKLOG` | 후속 Epic 범위이며 현재 release에 포함하지 않음 |
| `DONE` | 티켓 Done Criteria와 관련 검증을 충족 |

### Coordination

- coordinating agent만 이 delivery plan의 상태와 공용 canonical 문서를 갱신한다.
- ticket agent는 자기 stable work note와 티켓 전용 산출물만 갱신하고, 공용 delivery plan을 수정하지 않는다.
- 병렬 agent는 채팅 결론 대신 repository artifact와 work note로 결과를 반환한다.
- 같은 Java/DDL/canonical 문서를 건드리는 구현 티켓은 동시에 시작하지 않는다.
- `BLOCKED` 티켓은 prompt가 있더라도 선행 티켓 완료 전 실행하지 않는다.

## 4. Epic 1 — 값·구조 편집, XLSM 4개 생성, 새 Case

### Epic Goal

사용자가 기존 값을 수정하거나 허용된 행·열 구조를 변경하고 임시저장으로 관련 sheet/table 갱신값을 확인한 뒤,
최종저장에서 새로운 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` XLSM과 `INDEPENDENT` Case를 생성할 수 있다.

### Tickets

| ID | 티켓 | 산출물/완료 경계 | 선행 | 상태 |
| --- | --- | --- | --- | --- |
| `INPUT-01` | 실제 XLSM 3개 카탈로그 전수 조사 | 전체 sheet 분류, 화면 table/field 후보, 안정적인 row identity와 미결정 항목 | 없음 | `DONE` |
| `INPUT-01A` | 개발본 XLSM 4개 재조사 | 기존 3역할 변경과 신규 OUTPUT sheet/table/formula/package 구조, 개발 전용 시트, 화면 후보, 4파일 영향 | 01 | `DONE` |
| `INPUT-02` | Backend Excel 호환 계산 엔진 PoC | 실제 XLSM의 수식/cached value/매크로/서식/외부 link 보존 결과와 엔진 선택 근거 | 없음 | `DONE` (`NO-GO`) |
| `INPUT-02B` | 시트·테이블별 계산 호환성 목록 | 이전 65개 시트·60개 표·335 family 조사 완료; 현재 검증 계획은 계산 호환성 reference의 2026-10-07 판별대장 반영 절 | 01, 02 | `DONE` |
| `INPUT-02A` | 계산 엔진·격리 실행 후속 PoC | F-S-06 제외 선정19사례·승인 POI 부분 계산/보존·제한환경/격리/정리 근거에 한정, §6A 조건과 후속 검증 전달 | 02, 02B, 후보 승인 | `DONE` (`CONDITIONAL`) |
| `INPUT-03` | 카탈로그·Draft domain/ERD 확정 | field/option/dependency/구조 정책, Draft/change/revision/finalized Case의 logical·physical model과 DBA 검토 입력 | 01A, 02A, 02B | `READY` (설계) |
| `INPUT-04` | 조회·임시저장·최종저장 API 계약 | endpoint, DTO, 오류, 권한, 4파일 snapshot/revision/idempotency, payload/DRM 및 구조 변경 요청·검증 계약 | 01A, 02A, 02B | `READY` (계약) |
| `INPUT-05` | 카탈로그 seed와 화면 조회 API | Case의 XLSM 4개를 `file → sheet → table → field/row` projection으로 조회 | 03, 04 | `BLOCKED` |
| `INPUT-06` | Draft 생성·조회 persistence | 사용자 Draft, source snapshot, template version, revision 생성·조회 | 03, 04 | `IN_PROGRESS` (승인된 첫 범위는 §7A, 전체 잔여는 후속) |
| `INPUT-07` | 값 적용·재계산·XLSM materialization | `SET/CLEAR`와 승인된 구조 변경을 반영하고 계산 허용 영향 수식만 재계산해 role별 XLSM bytes 4개 생성 | 02A, 03, 04 | `BLOCKED` |
| `INPUT-08` | 값·구조 변경 임시저장 | catalog validation, optimistic revision, 변경 저장, 영향 table 값 반환/재조회 | 05, 06, 07, STRUCT-02~04 | `BLOCKED` |
| `INPUT-09` | 최종저장·Case 생성 | XLSM bytes를 `MultipartFile`로 변환, 이미 구현된 독립실행 4파일 Case 생성 서비스에 전달, 중복 Case 방지, `caseId` 반환 | 06, 07, 08 | `BLOCKED` |
| `INPUT-10` | 실파일 E2E·DBA rollout 검증 | 조회→수정→임시저장→최종저장→Case→별도 Run, API 전체 저장 응답/운영 시간 기준과 DBA 적용 검증 | 05~09, STRUCT-05 | `BLOCKED` |
| `INPUT-11` | legacy 3파일 Case cutover 정리 | 대상 snapshot(`INDEPENDENT/MI_BASE/SCENARIO`), `RUNNING` 0건·MI Base 영향 확인, case master soft delete(`SCENARIO`는 연결 row 포함), 목록/API 미노출 검증, rollback 증거 | 10, 운영 승인 | `BLOCKED` |

### Dependency waves

```text
Wave 1: INPUT-01 (DONE) → INPUT-01A (DONE: latest 4-file set)
        INPUT-02 (DONE: NO-GO) → INPUT-02B (DONE)
                    │
Gate 1A:        INPUT-02A (DONE: CONDITIONAL, F-S-06 제외)
                    │
Wave 2:        INPUT-03 + INPUT-04 + STRUCT-01

Wave 3: INPUT-05 + INPUT-06 + INPUT-07 + STRUCT-02~04
                         │
Wave 4:               INPUT-08
                         │
Wave 5:               INPUT-09
                         │
Wave 6:               STRUCT-05 → INPUT-10
                         │
Wave 7:               INPUT-11 (destructive rollout approval required)
```

Wave 1과 Gate 1A는 완료됐다. 다음은 INPUT-03/04의 설계·계약 착수다. 두 티켓의 선행01A/02A/02B는 DONE이며, READY는 제품 구현 승인이 아니다. 공용 canonical/DDL의 변경 순서는 coordinating agent가 조정하며 이번 종료 정리로 병렬 실행을 시작하지 않는다. STRUCT-01은 표별 일반 계약 정리 전 BLOCKED를 유지한다. INPUT-07은 INPUT-03/04 완료 전 배정하지 않는다.

## 5. Completed Ticket — INPUT-01

### Ticket Goal

현재 실제 `DEMAND`, `SUPPLY`, `TRADE` XLSM 3개를 전수 조사해 1차 값 수정 화면에 필요한
sheet/table/field 카탈로그 근거와 아직 사람이 결정해야 할 예외를 만든다.

### Done Criteria

- 세 파일의 모든 sheet가 `화면 대상 / 참조·계산 전용 / 빈 시트 / 제외 후보` 중 하나로 분류된다.
- 화면 대상 sheet의 논리 table 경계, header/data 범위, field key 후보, 값 타입, editable/formula/read-only가 기록된다.
- 각 table의 안정적인 row identity 후보와 identity가 성립하지 않는 예외가 기록된다.
- cross-sheet 영향 관계와 외부 reference는 계산식 복제가 아니라 evidence metadata로 기록된다.
- 행·열 추가는 구현하지 않지만 후속 조사를 위한 구조 정책 후보와 blocker가 table별로 구분된다.
- 업무 민감 원본 cell value를 보고서나 로그에 출력하지 않는다.

### Stable outputs

- Work note: `docs/work-notes/2026/2026-09-16-model-input-catalog-inventory.md`
- Evidence artifact: `docs/references/mi-model-input-catalog-inventory.md`
- Action: update

### Agent prompt

```text
AGENTS.md를 따르라.

목표:
- 실제 독립수급모델 input XLSM 3개를 전수 조사해 1차 값 수정 화면의 카탈로그 근거와 예외를 완성한다.

관련 코드·정본:
- docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md
- docs/work-notes/2026/2026-09-03-model-input-transfer-format.md
- docs/design-docs/gas/mi-model-input-editing-delivery-plan.md → INPUT-01
- scripts/measure-model-input-excel.py
- scripts/demand.xlsm, scripts/supply.xlsm, scripts/trade.xlsm (존재하는 로컬 표본만 읽고 수정·커밋하지 않는다)

범위·필수 조건:
- 세 파일의 모든 sheet를 화면 대상, 참조·계산 전용, 빈 시트, 제외 후보로 분류한다.
- 화면 대상은 논리 table 경계, header/data 범위, field key 후보, 값 타입, editable/formula/read-only, row identity 후보를 조사한다.
- cross-sheet formula와 external reference는 영향 evidence로 기록하되 table 관계를 계산식으로 사용하지 않는다.
- 행·열 추가는 구현하지 않는다. table별 구조 변경 blocker와 후속 정책 후보만 기록한다.
- cell 원문 값이나 민감 업무 데이터를 출력·기록하지 않는다. 좌표, 타입, 개수, formula/reference 패턴만 기록한다.
- 제품 코드, API contract, DDL, 공용 delivery plan은 수정하지 않는다.

완료 조건·검증:
- 모든 sheet가 누락 없이 분류되고 합계가 workbook 실측 sheet 수와 일치한다.
- 기존 측정 스크립트 또는 별도 read-only probe로 결과를 재현할 수 있다.
- evidence artifact에는 확인 사실과 추정·미결정 사항이 구분된다.
- 변경 문서에 git diff --check를 실행한다.

Work note:
- Stable path: docs/work-notes/2026/2026-09-16-model-input-catalog-inventory.md
- Action: update

Coordination:
- docs/references/mi-model-input-catalog-inventory.md와 위 work note만 직접 갱신한다.
- 완료 후 coordinating agent에게 요약, 변경 파일, 검증 결과, 남은 blocker를 반환한다.
```

## 5A. Completed Ticket — INPUT-01A

### Ticket Goal

2026-09-28 수신한 최신 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` XLSM 4개를 재조사해
기존 3파일 설계의 변경점과 신규 `OUTPUT`의 catalog·계산·실행 영향을 확정한다.

### Result

- 첨부 개발본은 76개 시트이고, 실제 인풋에 없는 `SUMMARY`·`플랫폼 입력 안내` 6개를 제외한 runtime 기준은 70개다.
- 개발 전용 시트에는 수식이 없어 수식 1,863,766셀, formula family 341개는 동일하다.
- 기존 3역할의 수식/외부참조/family 수는 이전 조사와 같고 안내 시트가 추가됐다.
- 첨부 `OUTPUT`은 7개 시트지만 실제 운영 구성은 개발 전용 2개를 제외한 5개 business sheet다. 수식 889셀, 신규 family 6개이며 5개 화면 후보 sheet에
  6개 직접 편집 table 후보와 12개 계산·참조 table 후보가 있다.
- 사용자는 `OUTPUT`이 다른 세 역할과 같은 필수 input임을 확인했다.
- Stable work note: `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` update
- Evidence: `docs/references/mi-model-input-catalog-inventory-2026-09-28.md`

## 6. Completed Ticket — INPUT-02

### Ticket Goal

실제 XLSM 3개로 Backend 계산 엔진 후보를 검증해 1차 값 수정에 필요한 수식 재계산과 workbook fidelity를
충족할 수 있는지 판정한다. 이 티켓은 production 구현이 아니라 기술 선택을 위한 spike다.

### Done Criteria

- 기존 Apache POI 경계와 후보 계산 방식의 지원·제약이 코드와 공식 자료 근거로 정리된다.
- 실제 표본의 허용된 test cell 변경 후 cross-sheet formula 결과와 cached value 갱신 여부가 확인된다.
- XLSM macro, style, merged cell, validation, named range, external link의 보존 또는 손실이 항목별로 기록된다.
- 결과 파일을 다시 열 수 있고 원본 파일은 변경되지 않는다.
- 처리 시간·peak memory를 최소 한 번 측정하고 동기 임시저장에 사용할 수 있는지 판정한다.
- `GO / CONDITIONAL / NO-GO`와 다음 최소 조치가 제시된다.

### Stable outputs

- Work note: `docs/work-notes/2026/2026-09-16-model-input-calculation-engine-spike.md`
- Evidence artifact: `docs/references/mi-model-input-calculation-engine-spike.md`
- Action: update

### Agent prompt

```text
AGENTS.md를 따르라.

목표:
- 실제 독립수급모델 XLSM으로 Backend Excel 호환 계산 방식을 검증하고 1차 값 수정 구현에 사용할 엔진 경계를 판정한다.

관련 코드·정본:
- docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md
- docs/work-notes/2026/2026-09-03-model-input-transfer-format.md
- docs/design-docs/gas/mi-model-input-editing-delivery-plan.md → INPUT-02
- src/main/java/com/sk/domain/mi/service/nextgen/paramproposal/excel/ModelParamExcelMergeService.java
- scripts/demand.xlsm, scripts/supply.xlsm, scripts/trade.xlsm (존재하는 로컬 표본만 읽고 수정·커밋하지 않는다)

범위·필수 조건:
- 현재 Apache POI의 formula evaluation, recalculation flag, XLSM 보존 한계를 먼저 확인한다.
- 실제 표본의 복사본에서 허용된 test input을 변경하고 cross-sheet formula와 cached value 갱신을 검증한다.
- macro, style, merged cell, validation, named range, external link 보존을 항목별로 확인한다.
- 파라미터 제안의 ModelParamExcelMergeService 생성 로직을 새 기능의 해법으로 가정하지 않는다.
- model container에 재계산 책임을 넘기지 않는다.
- production service/API/DDL은 구현하지 않는다. 필요한 probe와 test만 최소 범위로 작성한다.
- cell 원문 값이나 민감 업무 데이터를 로그·문서에 남기지 않는다.

완료 조건·검증:
- 후보별 지원/실패 evidence와 실제 파일 검증 결과를 남긴다.
- 생성 결과 파일을 다시 열고 XLSM package 무결성과 주요 보존 항목을 확인한다.
- 처리 시간과 peak memory를 측정하고 임시저장 동기 처리 가능 여부를 판정한다.
- 최종 결과를 GO, CONDITIONAL, NO-GO 중 하나로 판정하고 API/ERD에 영향을 주는 조건을 적는다.
- 작성한 probe/test와 문서에 가장 작은 관련 검증 및 git diff --check를 실행한다.

Work note:
- Stable path: docs/work-notes/2026/2026-09-16-model-input-calculation-engine-spike.md
- Action: update

Coordination:
- docs/references/mi-model-input-calculation-engine-spike.md와 위 work note, 티켓 전용 probe/test만 직접 갱신한다.
- 공용 domain design, feature spec, DDL, delivery plan은 수정하지 않는다.
- 완료 후 coordinating agent에게 판정, 변경 파일, 검증 결과, 라이선스·배포·운영 blocker를 반환한다.
```

### Result

- 판정: `NO-GO`
- Apache POI 5.4.0은 1 GiB heap에서 `supply.xlsm` 전체 로딩에 실패했다.
- headless LibreOffice는 cached value를 갱신하지 못했고, 처리 시간이 길며 XLSM package fidelity를 훼손했다.
- 상세 실행 증거는 `docs/references/mi-model-input-calculation-engine-spike.md`와 stable work note가 소유한다.

## 6A. Completed Ticket — INPUT-02A (`CONDITIONAL`, F-S-06 제외)

### Ticket Goal

승인된 계산 엔진 후보로 `INPUT-02`와 같은 실파일 probe를 반복해 production 계산 경계와
동기/비동기 실행 방식을 선택한다.

### Start condition

- 2026-10-13 후보 승인: POI 부분 계산(domain design §1A.4 Q7). 이미 쓰는 의존성이라 라이선스·반입 승인이 필요 없고, 상용 엔진은 평가하지 않는다. 후보 승인은 아래 Done Criteria 충족이나 `GO`가 아니다.
- 2026-10-15 운영 기준 사용자 승인: domain 규칙64의 격리 worker/동시성/비동기·자원·저장 목표/전체 상한을 따른다. 우선 실제 자원 제한에서 대표 승인 경로를 재측정하며 승인 자체를 제한환경 성능 검증이나 최종 판정으로 사용하지 않는다.
- 후보가 상용 엔진이면 평가 라이선스와 의존성 반입 승인을 확보한다. 상용 엔진을 전제로 고정하지 않는다.
- 외부 workbook 원본은 계산 선행조건이 아니며 업무담당자에게 요청하지 않는다. 외부 참조의 계산/보존 구분은 domain design §6 규칙 33·54를 따른다.
- 2026-10-15 사용자 후속 결정: 로컬 DRM 복호화 시험은 불가하고 이미 적용된 암복호화 기능을 재사용하므로 이번 INPUT-02A의 실제 DRM 재검증은 제외한다(domain 규칙63). DRM 표본/시험 설정·원격 재검증 증거를 착수 선행조건으로 요구하지 않는다. 로컬 worker 평문 임시 파일의 수명·삭제 검증은 아래 완료 기준으로 유지한다.
- `mi-model-input-sheet-calculation-compatibility.md`의 현재 검증 기준으로 표별 계산·보존·구조 변경 fixture를 선정한다. 이전 category 기반 oracle/254 family 목록을 그대로 실행하지 않는다.

### Done Criteria

- 승인된 source 값·구조 변경 후 계산 allowlist의 same-sheet/cross-sheet 결과가 golden과 일치하고, 계산 제외 수식·cached value는 보존된다.
- 승인 정규화 source의 계산 제외 formula·cached value와 externalLink, macro, style, merged cell, validation, named range를 보존한다. domain 규칙 54의 계산 결과 및 구조 변경 승인 영역만 계약대로 갱신한다. 이전 42,394셀을 최신 4파일 전체의 확정 합계로 사용하지 않는다.
- 실제 4개 파일의 wall time과 peak memory를 측정하고 승인 계산 경로와 OUTPUT의 값/구조 변경·미표시 A 보존 검증을 포함한다. OUTPUT의 최신 정책·검증 경로는 domain 규칙 50/§6.1과 compatibility의 V-OUTPUT 항목을 따른다. 과거 Helper 포함 817셀을 계산 allowlist로 사용하지 않으며 최신 family·cell 수는 source hash·표별 계산 범위 확정 후 재집계한다.
- domain 규칙64의 승인된 격리 worker/비동기 실행 경계에서 자원 제한 강제·대표 경로 실측 근거를 기록하고, 제품/API·전체 저장 응답 미검증 범위를 최종 판정/후속 티켓에 연결한다.
- 라이선스, 로컬 worker 평문 임시 파일 수명·삭제(domain 규칙63), timeout/kill, 외부 link 비갱신·보존 정책을 기록한다. 실제 DRM 암복호화 시험은 위 사용자 제외 결정으로 해당 없음이며 Pass와 구분한다.
- `GO / CONDITIONAL / NO-GO`와 `INPUT-03`, `INPUT-04`, `INPUT-07`에 넘길 제약을 확정한다.

### Stable outputs

- Work note: [기존 stable work note](../../work-notes/2026/2026-09-03-model-input-transfer-format.md) **update**. 별도 새 note를 만들지 않는다. 초기 stage1 note는 당시 이력으로 유지한다.
- Evidence artifacts: [engine spike](../../references/mi-model-input-calculation-engine-spike.md)·[compatibility](../../references/mi-model-input-sheet-calculation-compatibility.md) update. 과거 XLSM/golden/도구/실행 기록은 수정하지 않는다.

### Final verdict — 2026-10-15 사용자 승인

**`DONE (CONDITIONAL)` — F-S-06 제외.** 조건부 완료는 고정 source와 아래 선정 경로에서 POI 부분 계산/원본 보존 및 규칙63·64의 실행 기준을 채택한다는 뜻이다. 모든 표/입력 조합·모델 결과·제품/API·운영 배포의 검증 완료를 뜻하지 않는다. 이전 전체 workbook POI/LibreOffice `NO-GO`는 유지한다.

- 기준본: [2026-10-07 manifest](../../references/mi-model-input-baseline-2026-10-07-manifest.json)의 v8/v6 정규화4파일. 입력/보존19 Pass, 승인 계산/동일변경 Excel9 Pass, 명시적 추가 계산 제외10은 해당 없음(PRESERVE), 기록한 범위의 읽기 전용 native19 Pass다. 새 계산 범위나 기존 오류의 허용을 추론하지 않는다.
- 운영 채택: domain **규칙64**의 worker/자원·순차/비동기·저장 목표/전체 상한, **규칙63**의 평문 임시 파일 수명·정리를 따른다. 기준값은 해당 정본에서만 변경한다. 기존 암복호화 기능을 재사용하며 실제 DRM 재검증은 **해당 없음(사용자 제외)**이다.
- 실증: 승인7경로×3 자원21회, 동시/중단 정리6시험·14요청, 평문 수명/실제 프로세스 복구10시험, 실제2vCPU/2GiB 대표4경로×3 인정12회가 완료됐다. 제한환경 최대 파일 처리14.564초·worker kernel peak666.5MiB·OOM0, 승인 결과 hash/보존 동일이다. 준비 manifest mount 누락1회는 수정/정리한 환경 오류로 별도 보존했다(총13시도/12인정). [종료 기록](../../../temp/input-02a-engine/2026-10-15-input-02a-conditional-closeout-c2mlcayi/final-summary.json)의 기존 증거 링크와 stable note를 따른다.
- 한계: 각 경로3회·통제하지 않은 warm cache, Linux/tmpfs/JDK 환경의 로컬 파일 처리 구간이다. 각 역할 중앙값 합은 같은4파일 저장 요청의 실측이 아니다. API/대기열·기동/DB/storage upload·저장 확정/fsync를 포함한 전체 응답, 제품의 상태/부분 실패, OS reboot/정전·접근 불가 사용자 핸들/물리 매체 삭제는 검증하지 않았다. native Pass는 기록 범위의 읽기/무저장 확인이며 전수 수식 계산·native 저장/매크로 인증이 아니다.
- 제외/후속: F-S-06은 종료 대상에서 제외한다. CLEAR3(F-O-03/F-D-04/F-T-03)는 빈 값 금지 결정이므로 허용 파일 시험이 아니며 거부/기본값의 제품 검증은 INPUT-07/08→10으로 전달한다. 지정 밖 계산·구조는 해당 계약과 근거를 확보한 범위만 연다. 선정19파일의 정책/Excel/native를 다시 요청하거나 전체 조사·DRM 원격 증거를 새 INPUT-02A 선행조건으로 추가하지 않는다.

### INPUT-03/04/07 handoff constraints

| 전달 티켓 | 반드시 이어받을 제약 | 후속 완료 경계 |
| --- | --- | --- |
| INPUT-03 | 고정 source/template와 안정적인 row identity, 승인된 입력·계산·PRESERVE·구조 경계를 구분한다. 연결 설비의 행 변경/원본 cache 동반 이동과 규칙15·60·61의 성공 Draft/실패 요청 경계를 유지한다. worksheet 행 번호·설비명만으로 영속 identity를 정하지 않는다 | domain/ERD·저장 소유권·revision 및 실패 경계를 확정. 규칙63/64를 임의 변경하지 않고 API/DDL 정본과 연결 |
| INPUT-04 | 규칙64의 비동기 접수와 실제 저장 완료를 구분한다. Q8의 revision/idempotency·요청 취소/직전 성공 Draft 보존·부분 업로드/Case 연결 실패를 계약으로 구체화한다. PoC 시간을 전체 저장 응답 보장으로 표현하지 않는다 | feature spec/OpenAPI에서 상태·완료/실패·timeout 경계 확정. 실제 응답/저장 시간 검증은 아래 INPUT-10 후속 항목으로 전달 |
| INPUT-07 | source 전체 복제 후 승인 셀/구조만 변경. POI는 승인된 source-only 부분 계산만 실행하고 golden은 비교용이다. 명시적 계산 제외10, A/UNIQUE·원본 오류/외부 cache·hidden/package를 보존하며 링크 갱신/매크로 실행을 하지 않는다. 구조의 참조/shared/chain·서식/validation/names 변경은 승인 기대값만 반영 | 규칙63/64의 자원 격리·동시1·작업별 정리/복구를 실제 제품에 적용하고 실패 파일을 성공으로 게시하지 않음. 기존 공용 @Async pool을 workbook 동시성 근거로 쓰지 않음. INPUT-03/04 완료 후 구현·제품 보존/계산 검증 |

**후속 티켓: INPUT-10 — API 전체 저장 응답 검증 (`PENDING`, 현재 티켓 상태 `BLOCKED`).** INPUT-04 계약과 INPUT-07~09 구현 후, 실제 동일4파일 저장 요청의 접수→대기→처리→DB/storage 저장 확정→완료 응답을 측정한다. 규칙64의30초 목표/전체120초 상한 충족 여부, 접수/미완성 파일을 성공으로 오인하지 않는지, timeout/부분 실패 시 취소·직전 성공 Draft·중복 Case 방지·규칙63 정리를 확인한다. 구체 API 필드/오류는 INPUT-04 정본을 따른다. 이 제품 검증을 INPUT-02A의 미완료 시험으로 남기지 않으며 운영 배포 전 INPUT-10에서 판정한다.

## 7. Remaining Epic 1 ticket boundaries

후속 티켓은 선행 결과 통합 후 표준 task prompt를 작성한다. 지금 단계에서는 아래 완료 경계만 고정한다.

### INPUT-03 — 카탈로그·Draft domain/ERD 확정

- field/options/dependency/구조 정책과 Draft/change/finalized Case의 책임·lifecycle을 canonical domain design에 확정한다.
- physical table/JSON 경계, key/index, revision uniqueness, active Draft cardinality와 DBA rollout을 DDL draft에 확정한다.
- §6A의 INPUT-03 전달 제약을 반영한다. READY는 설계 착수 가능이며 물리 모델/업무 정책 확정이나 제품 구현 완료가 아니다.
- 공통 저장 구조는 domain §3.4·§5.1~5.3/DDL §6.3의 정의로 문서 범위 완료. 남은 표별 field/구조 정책은 해당 표/동작 쓰기를 열 때 처리하며 공통 설계의 선행조건이 아니다. 실제 DBA 적용 준비·구현/검증은 별도다.
- `new-domain-model-design`과 `erd-change-review`를 필요한 미결에 적용한다.

### INPUT-04 — 조회·임시저장·최종저장 API 계약

- 조회 projection, Draft 생성/조회, 값·구조 변경 임시저장, 최종저장 endpoint와 DTO/error/OpenAPI 계약을 확정한다.
- domain 규칙64로 승인된 격리 비동기 실행의 접수/완료·timeout, 임시저장 계산 실패, DRM/payload/snapshot/idempotency를 포함한다. §6A의 INPUT-04 제약과 INPUT-10 API 전체 저장 응답 후속 검증을 계약에 연결한다.
- EDT-01~07·최종저장/실패 복구의 공통 계약은 [편집 API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md) §3~6·§9로 문서 범위 완료. 표별 정책·실제 generated OpenAPI/제품 검증은 후속이다.
- `backend-feature-spec`을 필요한 계약 범위에 적용한다.

### INPUT-05 — 카탈로그 seed와 화면 조회 API

- 승인된 template version과 source snapshot으로 화면 대상 table projection만 반환한다.
- 원본 binary·storage 식별자를 노출하지 않는다.
- 조회 API slice만 구현하고 write API는 포함하지 않는다.

### INPUT-06 — Draft 생성·조회 persistence

- 2026-10-16 채택한 첫 구현 묶음은 아래 §7A다. 공통 요청 처리와 EDT-01~03에 필요한 조회 연결을 포함하며 현재 실행 결과는 기존 작업 노트를 따른다. 전체 INPUT-05/07~09의 실행 범위로 확대하지 않는다.

- source case/files/template version을 고정한 사용자 Draft와 optimistic revision을 생성·조회한다.
- 값 변경 적용, XLSM 생성, Case 생성은 포함하지 않는다.

### INPUT-07 — 값 적용·재계산·XLSM materialization

- 고정 source snapshot의 원본 workbook 파일 전체를 먼저 복제하고 catalog가 허용한 sheet/cell만 변경한다.
- catalog가 허용한 기존 field의 `SET/CLEAR`와 `STRUCT-02~04`에서 확정한 구조 변경을 반영한다.
- Backend 계산 경계에서 계산 허용 수식/cached value만 갱신한다.
- §6A의 INPUT-07 전달 제약과 domain 규칙63/64를 구현·검증한다. 선정 PoC 통과를 미검증 표/조건의 계산 허용으로 확대하지 않는다.
- 외부 참조의 계산/보존 구분은 domain design §6 규칙 33·54를 따른다. 계산 예외의 결과 외 formula·외부 데이터 캐시·externalLink/package는 보존하고 외부 파일을 열거나 갱신하지 않는다.
- TRADE `4.5 PriceModelInput`의 T-183/T-184는 domain 규칙 44의 승인 값/구조 변경을 반영하고 그 밖 원본 영역은 보존한다. 이 편집 허용만으로 계산 엔진 적용이나 외부 참조 갱신을 허용하지 않는다.
- 파라미터 제안 파일 생성 로직과 model container를 사용하지 않는다.

### INPUT-08 — 값·구조 변경 임시저장

- Draft revision 충돌과 catalog write permission을 검증하고 값·구조 변경분을 저장한다.
- 성공한 계산 결과의 영향 table을 반환하거나 재조회할 수 있게 한다.
- Case와 최종 XLSM을 생성하지 않는다.

### INPUT-09 — 최종저장·Case 생성

- 고정 source snapshot과 Draft 전체 변경으로 XLSM 4개를 처음부터 materialize한다.
- source snapshot의 원본 workbook 파일 전체를 먼저 복제하고 catalog가 허용한 sheet/cell만 변경한다.
- hidden sheet는 계산 엔진에 전달하지 않고 worksheet/package 내용을 수정하지 않는다.
- TRADE `4.5 PriceModelInput`의 T-183/T-184는 domain 규칙 44에 따라 입력값 편집과 각 표의 마지막 데이터 행 아래 행 추가를 최종 XLSM에 반영하고, 승인 변경 밖 원본 영역은 보존한다. 이 편집 허용만으로 계산 엔진 적용이나 외부 참조 갱신을 허용하지 않는다.
- 후속 최종저장 연결은 domain §5.2/§5.4를 따른다. 기존4파일 검증/저장 책임을 재사용하되, 준비 참조→비공개 업로드→Case/파일/양식/Draft 동시 확정을 지킨다. 기존 createCase/staged API 그대로 호출하는 것으로 원자성/복구가 충족됐다고 보지 않는다.
- 같은 Draft 재시도에 Case를 중복 생성하지 않고 `caseId`를 반환한다. Run은 호출하지 않는다.

### INPUT-10 — 실파일 E2E·DBA rollout 검증

- 실제 파일 조회부터 별도 Run까지 전체 흐름과 원본 불변성을 검증한다.
- **후속 API 전체 저장 응답 검증: PENDING.** §6A의 동일4파일 요청/비동기 접수→저장 확정·완료 응답, 규칙64 시간 기준과 timeout/부분 실패·정리를 실제 API/DB/storage로 검증한다. INPUT-04 계약 및 INPUT-07~09 구현 이후 수행하며 INPUT-02A 조건부 완료를 이 제품 검증의 Pass로 사용하지 않는다.
- DB pre-check/apply/rollback/verify와 고객사 환경 적용 증거를 기록한다.
- 계산 허용 cached value 갱신, 계산 제외 외부 참조 formula/cached/link 및 계산 예외의 원본 수식·외부 데이터 캐시·link 불변, macro/style/validation, Case 파일 연결, 모델 결과 반영을 확인한다.
- source와 결과의 hidden sheet 집합·순서·상태와 hidden worksheet part 불변을 확인하고, hidden sheet가
  조회 projection·Draft 변경·dependency·계산 대상에서 제외되는지 검증한다.
- 운영 기준 정규화본과 최종 XLSM/Case/Job input에 `SUMMARY`, `플랫폼 입력 안내`가 없음을 확인한다.
- 화면에서 수정 가능한 칸을 바꾼 최종 XLSM으로 Run을 실행해 변경이 모델 결과에 반영되는지 확인한다. 모델은 수식 칸의 cached value를 기본으로 읽고 일부 영역만 실행 시점에 Python으로 재산출한다(2026-10-07 모델팀 답변). 반영되지 않으면 원인을 나눈다.
  - 변경 칸에서 모델이 읽는 칸까지의 경로에 백엔드가 재계산하지 않고 보존한 수식(예: domain 규칙 45의 `DemandOutputOverwrite` K·S열)이 있으면, 백엔드 재계산 범위 결정을 다시 검토한다.
  - 백엔드가 전달한 파일에 값과 계산 결과가 올바르게 있는데 모델이 무시하면, input 파일이 기준이므로 모델 측 수정 사항으로 모델팀에 전달한다(2026-10-07 사용자 결정).
  - 시험 파일은 `scripts/model-input-cached-value-probe.py`로 만들 수 있다.

### INPUT-11 — legacy 3파일 Case cutover 정리

- active `INDEPENDENT | MI_BASE | SCENARIO` 중 `DEMAND/SUPPLY/TRADE`만 있고 `OUTPUT`이 없는 대상을 확정해 ID snapshot을 보관한다. `SCENARIO`는 2026-10-13 사용자 결정으로 추가했다(domain 규칙 36).
- `RUNNING` 0건과 current `MI_BASE` 포함 여부를 확인하고, 운영 승인 후 `INDEPENDENT | MI_BASE`는 case master의 `del_yn`만 `Y`로 변경한다. `SCENARIO`는 case master와 연결 run history·execution-preparation·해당 시나리오의 active 가격전망해석을 함께 변경한다.
- 파일 metadata/object, run history, 결과 row는 물리 삭제하지 않는다.
- 목록 미노출과 detail/status/result/input/run의 `404 RSC_NOT_FOUND`를 검증한다. `SCENARIO`는 정리 후 파라미터 팝업·최근 실행 파라미터·시나리오 결과가 남은 non-deleted row로 판정되고 데이터 불일치 오류가 없는지 검증한다.
- Scenario 4파일 전환 배포가 이 티켓보다 먼저 나가면, 정리 전까지 3파일 Scenario 건은 팝업 입력 파일에서 `OUTPUT` 누락(`complete=false`)으로 보인다. 두 작업의 실행 순서는 배포 계획에서 맞춘다.
- 실제 데이터 변경을 시작할 때 별도 stable work note와 DBA 승인 절차를 사용한다.

## 7A. 공통 BE 1·2 첫 구현 계획 — 2026-10-16 사용자 채택

- **현재 delivery 상태 — 2026-10-19:** QRY-01/EDT-01~07·F-O-01 처리·공통 worker/최종저장 연결의 구현에 이어, 사용자 Compose/testdb에서 Case105→106의 기본 성공 흐름을 확인했다. 상세 실행 결과와 시간대 설정 조치·미검증 범위는 [공용 작업 노트](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#case105-106-smoke-20261019)가 소유한다. FE의 제한 범위 API 연결은 진행할 수 있고, 일반 표/구조·계산 확장과 실환경 경합/복구·Kubernetes 운영 검증은 별도 후속이다. 전체 INPUT-10/11 완료로 승격하지 않는다.
- C의 92개 표는 [새 DRAFT 양식 등록](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#c-structure-registration-20261019)만 완료했다. 발행·새 Case 연결·보류7개는 후속이며 기존 Case103·105 양식을 교체하지 않는다. application model-code 설정은 사용하지 않으며 각 Case 연결 양식을 따른다.
- 2026-10-18 EDT-04/05 공통 기반·후속 실제 처리/최종저장 지원 경계는 [편집 API §1.2](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md), 증분 SQL은 [DDL §6.7](mi-model-input-template-catalog-erd-ddl-draft.md), 검증·독립 검토/수정 결과와 B 인수 결과는 [기존 작업 노트](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)가 소유한다. 아래 첫 구현 순서는 기존 EDT-01~03 계획 이력이다.

- 2026-10-16 table prefix 변경: 현재 물리명과 수동 적용 절차는 [DDL §6.6](mi-model-input-template-catalog-erd-ddl-draft.md) 및 [rename SQL](../../model-input-draft-table-prefix-rename-draft.sql)이 소유한다. 새 이름 적용·API 확인의 실행 사실은 [기존 작업 노트의 최신 결과](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다.

**첫 범위의 구현·검증 결과는 작업 노트 참조.** 사용자가 처음 승인한 ① 공통 요청 처리와 ② Draft 생성·재개·조회는 최소 QRY-01과 EDT-01~03/revision0 범위였다. 구현·Case103 기본 DB 연동 시험과 이후 ③ 임시저장 → ④ 최종저장의 F-O-01 기본 성공 결과는 [기존 작업 노트](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 구현 범위는 편집 API §1.2, 수동 적용 SQL은 DDL이 소유한다. 작업/파일 복구·경합의 추가 실환경 검증과 전체 기능/운영 배포는 구분한다. 표별 정책은 후속이며 PriceModel/Regas 추가 추천은 미승인이다.

### 범위·정본 연결

- FE 구간 로딩 방식은 [QRY 상세 §5.1](../../feature-specs/mi-fundamental-model-input-query-api-proposal.md)의 사용자 확정 계약을 따른다. Draft EDT-03도 편집 API §2의 참조로 연결한다. FE 구현/화면 연동 검증은 후속이며 BE API의 시험 결과와 구분한다.

- 첫 endpoint 범위는 [편집 API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md) §3의 **EDT-01~03**(생성/재개·구조 조회·선택 표 조회)이다. 공개 DTO/validation/오류·ID/revision/페이지·editing 표현은 같은 spec §2~3·§6, source 식별/원본 조회 표현은 [QRY 상세](../../feature-specs/mi-fundamental-model-input-query-api-proposal.md)의 확정 계약을 참조한다. 이 계획에서 계약을 다시 정의하지 않는다.
- 저장/행 identity·lifecycle과 재사용 경계는 [domain](mi-model-input-template-catalog-domain-model-design.md) §3.2~3.4·§5.1·§5.4, 물리 정의/코드 호환은 [DDL](mi-model-input-template-catalog-erd-ddl-draft.md) §6.3~6.4를 따른다. 현재 구현 범위는 편집 API §1.2, 구현·검증 근거는 기존 작업 노트로 연결한다.
- INPUT-06을 중심으로, EDT에 필요한 **원본4파일·발행 참조/구조·선택 표를 읽는 최소 조회 연결**만 INPUT-05 책임에서 포함한다. QRY 공개 API 전체·운영 catalog 발행/목록·seed 구축을 이 첫 묶음에 추가하지 않는다. 이미 확정한 읽기 정의/실제 등록이 필요한 사실은 연결 입력으로 확인하고, 원천이 없으면 정본 오류로 차단한다. 임의 catalog/다른 양식·원본/fallback/mock 성공으로 대체하지 않는다.

### 내부 진행 순서·완료 기준

| 순서 | 첫 구현 범위 | 확인할 결과 |
| --- | --- | --- |
| 1. 공통 요청·응답/안전 처리 | EDT-01~03의 인증된 회사/사용자·source 접근/개인 소유, ID/body/query·수치/크기 검사, 공통 안전 오류·본문/MDC 비로깅. 기존 resolver/응답 패턴을 필요한 범위에서 재사용 | 타회사/타인 Draft·잘못된 입력을 정본대로 거부. 숫자/정수·응답 값 손실 없음. DTO·오류/OpenAPI와 계약 일치. EDT-04 이후 전용 명령 처리·범용 validator/공통 handler 전체 개편을 선제 구현하지 않음 |
| 2. 고정 원본과 Draft 생성·재개 | 접근 가능한 원본4파일/발행 기준 고정, 개인 진행 Draft1건, revision0·원본 논리 행ID/위치의 최소 저장, 기존 Draft 재개. 이번 API에 필요한 master 저장/조회만 준비 | 생성 중 실패는 부분 Draft 없음. 재개 시 ID·기준·성공 상태/진행 참조 보존. 원본 기준 불일치와 중복 생성을 계약대로 처리. 동일 진행 Draft를 중복 생성하지 않고, 생성/재개만으로 Case/Run을 만들지 않음 |
| 3. Draft 구조·선택 표 조회 | EDT-02/03에 필요한 구조/표·안정 행 대응/현재 성공 revision·editing metadata를 연결. 생성 직후 revision0은 고정 원본 기준으로 읽음 | 구조 조회 최초 표 미선택, 선택 표만 조회, 페이지/revision 불일치 거부. 저장된 성공 이력이 있으면 그 기준으로 읽고 임의 revision0/원본으로 되돌리지 않음. 조회로 계산/성공 이력을 생성하지 않음 |
| 4. 첫 범위 검증·정본 동기화 | DB 없이 가능한 핵심 로직/요청·응답 테스트와 OpenAPI 대조, 변경 diff/권한·로그·수치/제한 검증. 실제 DB 의존 검증은 아래 분리 | 통과한 코드/계약 검증과 미실행 DB/실파일 검증을 구분해 같은 stable work note에 기록. compile 성공이나 mock 응답만으로 저장 동시성/복원·실제 연동 Pass를 보고하지 않음 |

미결 표별 정책은 해당 동작만 미완비로 표현하고 읽기/공통 작업의 선행조건으로 확대하지 않는다. editing metadata의 READY/INCOMPLETE/NOT_ALLOWED/UNSUPPORTED는 승인/준비된 실제 근거로 판정하며, **PriceModel/Regas 추가 추천은 미승인·후속**으로 유지한다. 기존에 승인한 다른 표의 정책은 변경하지 않는다.

### 검증 결과와 남은 DBA 확인

- **구현·시험 근거:** 코드/로컬 검증·사용자 실행의 적용/기본 연동 시험과 한계는 [기존 작업 노트의 최신 결과](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 새 테이블 전체/미사용 처리 계층을 먼저 만들 필요는 없다.
- **DBA 후속 유지:** DDL §13 DBA-01~05(실제 schema/ID, 제약·경합 환경, packet/크기 설정, 감사 표준, SQL/rollout)의 확인된 범위와 잔여는 기존 작업 노트를 참조한다. 기본 연동 시험을 DBA 운영 준비 전체 승인으로 대체하거나 확인 범위를 표별 목록/seed 설계로 확대하지 않는다.
- **DB 연동/배포 완료 기준:** 실제 연결에서 생성·재개/조회·원자성·동시 생성1건·수치 왕복/row 대응을 검증하고 DBA 결과와 최종 매핑을 일치시킨 뒤 판단한다. 앱은 ddl-auto:validate/Flyway disabled이므로 필요한 테이블/매핑을 확인·적용하기 전에 새 entity를 포함한 앱을 배포하지 않는다. DB 독립 검증 완료와 실제 저장 API 사용 가능을 별도 상태로 보고한다.

### 후속 경계·실행 상태

EDT-04~07 접수/처리, 변경 적용·계산/구조 명령·XLSM 생성, 격리 worker/재시작·timeout 처리, 업로드/최종 Case·파일 복구/Run은 다음 묶음이다. 첫 범위에서는 진행/완료 참조를 읽을 수 있도록 계약을 보존할 뿐 이 작업들을 시작하거나 가짜 성공으로 응답하지 않는다. 표별 정책·추가 목록/seed 설계와 후속 구현/운영 적용·배포는 별도 작업이다.

- 최초 계획 반영 완료 기준(이력): 첫 범위·진행 순서/검증/DBA 경계와 미승인 유지가 정본/기존 note에 일치하는 문서 범위였다. 이후 구현·시험 상태는 다음 항목을 따른다.
- 현재 첫 범위의 구현·기본 시험 결과와 잔여는 [기존 작업 노트](../../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 이 결과를 전체 INPUT-03/04·INPUT-05/06 DONE 또는 INPUT-07 배정으로 확대하지 않는다. 후속도 같은 stable work note를 **update**한다.

## 8. 이번 release의 구조 변경 작업 — STRUCT 티켓

기존 Epic 2의 STRUCT 티켓은 이번 release에 포함한다. `INPUT-03/04`와 구조 정책·write 계약을 맞추고, 구조 변경 구현을 `INPUT-07/08/09`에 연결해 `INPUT-10` 전에 검증한다. 실제 catalog 조사와 실파일 검증을 통과한 table별 동작만 연다. 다음 티켓은 선행 계약·처리 규칙 미확정으로 `BLOCKED`이며 구현 완료를 뜻하지 않는다.

| ID | 티켓 | 완료 경계 | 상태 |
| --- | --- | --- | --- |
| `STRUCT-01` | 구조 변경 가능 table 선정 | table별 추가 경계·선행 master 관계와 승인된 중간 삽입·삭제 예외 확정 | `BLOCKED` |
| `STRUCT-02` | 행 변경 처리 | 마지막 행 추가와 승인된 중간 삽입·삭제, 안정적인 row identity·필수값·연결 표 변경의 원자적 처리 | `BLOCKED` |
| `STRUCT-03` | 열 변경 처리 | 마지막 열 추가와 승인된 삭제, header 후보·기간축·허용 범위·관련 formula range 갱신 | `BLOCKED` |
| `STRUCT-04` | 수식·서식·validation 처리 | 신규 셀의 복제·생성 규칙과 구조 변경 시 formula/style/merged cell/validation/named range 갱신·보존 | `BLOCKED` |
| `STRUCT-05` | table별 실파일 E2E | 추가·삽입·삭제 후 허용된 참조/계산 결과 갱신, 보존 영역 불변 및 모델 실행 결과 검증 | `BLOCKED` |

## 9. Current dispatch state

| Ticket | Dispatch | Stable work note |
| --- | --- | --- |
| `INPUT-01` | Done | `docs/work-notes/2026/2026-09-16-model-input-catalog-inventory.md` |
| `INPUT-01A` | Done — 개발본 76개, runtime 70개 시트, OUTPUT 5개 business sheet/6개 편집 table 후보 | `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` |
| `INPUT-02` | Done — `NO-GO` | `docs/work-notes/2026/2026-09-16-model-input-calculation-engine-spike.md` |
| `INPUT-02B` | Done — 당시 3파일 기준 외부 참조 42,394셀 계산 제외·보존. 현재 외부 참조 정책은 domain 규칙 33·54 | `docs/work-notes/2026/2026-09-16-model-input-sheet-calculation-compatibility.md` |
| `INPUT-02A` | Done — `CONDITIONAL`, F-S-06 제외. 2026-10-15 사용자 승인·조건/한계/전달은 §6A | `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` (초기 stage1 note는 당시 이력) |
| `INPUT-03` | 공통 저장 구조/코드 연결 대조 — domain §5.4·DDL §6.3~6.4. DBA-01~05의 확인 범위/잔여는 기존 작업 노트 참조, 표별 정책·첫 범위 밖 구현/검증 후속 | `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` update |
| `INPUT-04` | 공통 EDT-01~07/최종저장·실패 복구 계약 문서 — [편집 API Draft](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md) §3~6·§9. F-O-01 한정 구현·실환경 성공은 §7A·기존 작업 노트 참조. 일반 표 계약·전체 제품/운영 검증 후속 | `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` update |
| `INPUT-06` / 공통 BE | IN_PROGRESS — QRY-01/EDT-01~07 및 F-O-01 기본 성공 흐름 확인. 경합/복구·전체 persistence/운영 검증은 §7A·기존 작업 노트의 후속 범위 | `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` update |
| `INPUT-07` | 일반 표/전체 범위는 INPUT-03/04 준비 후 배정. 별도 승인 F-O-01 한정 구현·실환경 결과는 §7A | 선행 완료 후 지정 |
| `INPUT-10` | 전체 E2E는 제품 구현/STRUCT-05 후 배정. F-O-01 기본 성공 흐름만 확인했으며 API 전체 저장 응답·실환경 경합/복구 검증은 PENDING | 선행 완료 후 지정 |
| `INPUT-11` | Do not dispatch — 4파일 E2E 완료와 운영 승인 필요 | 착수 시 별도 생성 |
| 나머지 | Do not dispatch | 선행 티켓 완료 후 지정 |

2026-10-16 공통 BE 추천 묶음과 Sector H/J·I 음수 금지, 연월 범위·신규/기존 수정 중복 금지는 사용자 채택한 기준이다. 국가/하위부문은 같은 날 최종적으로 필수 자유입력으로 변경했으며 목록 선택·option snapshot/seed 연결안은 철회했다. 이전 inventory §6.3은 조사 이력이며 허용 목록/발행 근거가 아니다. 이 입력 방식의 후속은 모델의 명칭 인식 제약 확인만 남긴다. 다른 표 정책/명칭 인식은 표별 후속이다. 공통 저장 물리 정의/최종저장·복구 계약은 이번 후속에서 마쳤으며 DBA 적용 준비·INPUT-10의 API 전체 저장 응답 검증은 남았다. 구현/배포와 전체 INPUT-03/04 완료로 승격하지 않는다.

2026-10-16 후속으로 수요 입력·해제와 BCPrices 기존 수정의 두 묶음을 사용자 채택했다. 업무 기준은 domain 규칙45·§6.1, 공개 검증은 편집 API §4.1~4.3이다. 해당 업무 미결은 제거했고 다른 표 정책·전체 구조/계산·물리/제품 검증 및 기존 발행 불변/원본 보존은 유지한다. 정책 문서 반영 완료이며 INPUT-03/04 전체 완료·구현/DB 적용은 아니다.

2026-10-16 공통 계약 정리 당시의 작업 전환: **표별 정책은 후속**, PriceModel 비계약 물량/Regas 신규 설비 추천은 **미승인**으로 남긴다. 공통 저장7개 책임·성공 이력/행 위치·요청 중복/진행 제한·JSON 정밀도/복원과 최종저장 접수/조회·4파일/Case/양식/Draft 동시 확정·실패/응답 유실/재시작 복구의 문서 범위는 완료했다. 공통 설계 완료를 위해 표마다 모든 정책을 먼저 정할 필요는 없다. 미완비 동작만 차단하는 원칙을 유지한다. 이 계약 정리 자체를 추가 구현/DB·seed 적용이나 전체 INPUT-03/04·제품/DBA 완료 또는 INPUT-07 배정으로 해석하지 않는다. 이후 첫 구현 상태는 §7A를 따른다.

2026-10-16 첫 구현 전 BE·DBA 확인 이력: 당시 코드/문서 기준의 재사용·보완 경계를 domain §5.4·DDL §6.4에 기록하고 실제 DB 확인 항목을 DDL §13 DBA-01~05로 분리했다. 기존 staged 파일 기능/모델 Run 복구를 Draft 계약 충족으로 보지 않는 경계는 유지한다. 이후 첫 구현·시험 결과와 남은 DBA 확인은 §7A·기존 작업 노트를 참조하며 표별 정책 후속·PriceModel/Regas 추가 추천 미승인을 유지한다.

첫 구현 범위는 **§7A의 공통 BE 1·2**이며 최신 실행·검증 결과는 기존 작업 노트를 참조한다. 위 INPUT-03/04의 전체 후속/이전 Do not dispatch를 표별 모든 정책을 먼저 정해야 한다는 의미로 확대하지 않는다. 후속 구현 배정·남은 DBA 확인은 별도로 진행하며 표별 정책 후속·PriceModel/Regas 추가 추천 미승인을 유지한다.

## 10. Change Log

- 2026-10-17: 첫 구현/DB 접속·적용의 이전 상태 문구를 정리하고 문서 header·§7A·현재 dispatch를 기존 작업 노트의 최신 실행 결과 참조로 연결했다. INPUT-06은 승인된 첫 범위의 IN_PROGRESS로 표시하고 전체 ticket DONE과 구분했다. endpoint/업무 정책·티켓 선행조건 변경이나 코드·DB 실행은 없다.

- 2026-10-16 사용자 채택으로 공통 요청 처리와 Draft 생성·재개·조회(EDT-01~03)를 첫 구현 계획 §7A/INPUT-06 dispatch에 반영했다. 필요한 최소 원본/발행 조회와 코드·DB독립 검증/실제 DB 연동·배포의 완료 경계를 분리했다. 후속 INPUT-09의 파일 서비스 연결 설명은 domain §5.2/§5.4의 기존 원자성/복구 경계 참조로 정렬했다. 계약/정책 변경·구현/DB 적용은 없다.

- 2026-10-16 기존 코드·문서 기준의 공통 BE 필수 확인을 완료하고 실제 DB 실물 확인5개를 DBA 후속으로 분리했다. 현재 제안 병합 결과 타입은4파일임을 정정했고, staged API/UNKNOWN 정리·JSON 정밀도/본문 로깅의 보완 경계를 기록했다. 코드/DB 변경은 없다.

- 2026-10-16 공통 저장 구조와 EDT-06/07 최종저장·실패 복구 계약을 기존 정본에 마무리했다. 표별 정책은 후속으로 분리하고 PriceModel/Regas 추가 추천은 미승인 유지. 구현/DB 적용/제품 검증은 별도다.

- 2026-10-16: Sector/Monthly 수요 입력·해제 및 BCPrices 기존 연도/가격 수정 두 묶음 채택을 정본/API 검증·예시·현재 미결에 반영했다. 전체 티켓/제품/DBA 준비도는 승격하지 않았다.

- 2026-10-16 최종: Sector 국가/하위부문을 필수 자유입력으로 전환하고 앞선 목록 선택·발행/seed 연결안을 철회했다. 모델의 명칭 인식 제약만 이 입력 방식의 후속으로 남기며 연월·중복·I/H/J 정책은 유지했다. 아래 목록 선택 채택은 이 결정 전 이력이다.

- 2026-10-16: Sector 목록/연월/신규·기존 수정 중복 금지의 사용자 채택을 domain 규칙45·편집 API §4.1·DDL 후속에 연결했다. 업무 미결과 실제 목록/정의·seed/저장 검증을 구분하며 전체 티켓 완료로 승격하지 않았다.

- 2026-10-16: 공통 BE 추천 묶음과 Sector 정책 채택을 §9의 정본 연결에 반영했다. 물리 DDL/최종저장 상세·제품 검증은 후속이며 전체 티켓 완료로 승격하지 않았다.

- 2026-10-16: INPUT-03/04의 부분 공통 초안과 기존 stable note를 §9에 연결했다. 전체 티켓 DONE·STRUCT/제품 구현 승인으로 승격하지 않으며 §6A 조건부 완료·INPUT-10 PENDING은 유지한다.

- 2026-10-15: 사용자 승인으로 F-S-06 제외 INPUT-02A를 `DONE (CONDITIONAL)`로 완료했다. 규칙63/64·선정19사례/실제 제한 실측·검증 한계·INPUT-03/04/07 전달 제약을 §6A에 기록했다. INPUT-03/04는 설계/계약 READY, INPUT-07과 제품 검증은 BLOCKED를 유지하며 API 전체 저장 응답은 INPUT-10 후속 PENDING으로 등록했다. 전체 기능/조사·구현/배포 완료 판정은 하지 않았다.
- 2026-10-13: 사용자 결정으로 `INPUT-11` 정리 대상에 3파일 `SCENARIO` case를 추가했다. 연결 run history·execution-preparation·active 가격전망해석을 함께 soft delete하고 Scenario 화면 판정 검증과 배포 순서 주의를 적었다. 티켓 상태·선행조건은 유지한다.
- 2026-10-13: 미커밋 문서 정합성 검토에 따라 open question과 §9의 `GDP`/`PIPELINE`/`_XLFN.UNIQUE` 검증 문구를 최신 함수 조사(오탐 정정)와 domain 규칙 57(UNIQUE 5셀 원본 보존)에 맞췄다. §9 `INPUT-02B`의 42,394셀은 당시 3파일 수치로 표시했다. 티켓 상태·선행조건은 유지한다.
- 2026-10-13: 사용자 결정으로 INPUT-02A 계산 후보를 POI 부분 계산으로 승인하고(domain Q7) 상태를 `IN_PROGRESS`로 바꿨다. INPUT-03/04 배정 조건(동기/비동기 판정 후)과 다른 티켓 상태는 유지한다.
- 2026-10-08: 사용자 확인대로 read-only 표시를 `LNG Summary` 1개로 정렬하고 `backupLNG_TradeRestrictionInput`은 미표시·원본 보존으로 구분했다. 티켓 상태·선행조건은 유지한다.
- 2026-10-08: OUTPUT v8 사람 확인과 domain 규칙 50/§6.1에 맞춰 INPUT-02A의 과거 817셀 재계산 예시를 입력/구조 변경·미표시 A 보존 검증으로 정렬했다. 상세 계산 범위·엔진 검증과 티켓 선행조건은 유지한다.
- 2026-10-08: 사용자 지시에 따라 INPUT-09의 PriceModelInput 시트 전체 무수정 문구를 domain 규칙 44의 T-183/T-184 값 편집·마지막 데이터 행 아래 추가 반영과 승인 변경 밖 원본 보존으로 정렬했다. hidden sheet 보존과 티켓 상태·선행조건은 유지한다.
- 2026-10-07: 설계 검토 결과에 따라 §2·INPUT-07의 외부 참조 수식 고정 숫자(42,394)를 역사적 수치로 돌리고, INPUT-09가 이미 구현된 독립실행 4파일 Case 생성 서비스를 재사용하도록 문구를 정정했다.
- 2026-10-07: 사용자 지정 최신 v8/v6 4파일의 원본/정규화 hash와 보존 검증으로 검증용 기준본 고정을 완료했다. v3 판별대장 좌표의 v8 재대조는 후속 검증으로 구분했다.
- 2026-10-07: 모델팀 답변(수식 칸은 기본 cached value를 읽고 일부 영역만 Python 재산출)에 따라, Python 재산출 영역 목록은 요청하지 않고 사용자 수정 반영 여부를 `INPUT-10` E2E에서 확인하도록 추가했다. 근거는 `docs/work-notes/2026/2026-10-06-model-formula-cell-read-behavior-check.md`.
- 2026-10-07: 판별대장 기반 최신 계산 검증 목록을 INPUT-02A 입력으로 연결했다. 이전 3파일 family 합계는 역사적 실측으로 유지하고, OUTPUT 계산·DEMAND 수식 보존·Regas 연쇄 갱신 등 표별 정책을 적용한 뒤 fixture를 선정한다.
- 2026-10-07: 사용자 확인으로 구조 변경을 이번 release에 포함했다. 마지막 행·열 추가를 중심으로 기존 중간 삽입·삭제 예외를 유지하며, STRUCT 티켓을 후속 BACKLOG에서 이번 release의 BLOCKED 작업으로 옮겼다.
- 2026-10-07: 사용자 지정 배포 일정을 기록했다. 개발 환경 배포는 2026-10-19 전후의 잠정 일정이며, 운영 배포는 2026-10-22 필수 기한이다.
- 2026-09-16: 로컬 Epic 2개와 티켓 backlog를 생성하고 `INPUT-01`, `INPUT-02`를 최초 READY 상태로 등록했다.
- 2026-09-16: `INPUT-01` 완료 결과를 통합해 65개 시트, 23개 화면 후보와 row identity/빈 슬롯 예외를 후속 입력으로 반영했다.
- 2026-09-16: `INPUT-02`를 `NO-GO`로 완료 처리하고 상용 계산 엔진·격리 실행 검증을 `INPUT-02A`로 분리했다.
- 2026-09-17: `INPUT-02B` 결과를 통합하고 외부 workbook 참조 수식 42,394개를 Backend 계산에서 제외해 원본 formula·cached value·link/package를 보존하는 정책으로 범위를 축소했다.
- 2026-09-28: 첨부 개발본 4파일을 재조사해 `INPUT-01A`를 완료했다. `OUTPUT`을 같은 input 역할로 추가하고, template/source snapshot/Draft/materialization/Case/Run의 필수 set을 4개로 변경했다.
- 2026-09-28: 사용자 결정에 따라 legacy 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT` 보완 없이 soft delete하는 `INPUT-11` cutover 티켓으로 분리했다.
- 2026-09-28: 첨부본의 `SUMMARY`, `플랫폼 입력 안내`는 개발 전용이고 실제 인풋에는 없음을 반영했다. 첨부본 76개와 runtime 70개 sheet를 구분했다.
- 2026-09-28: 별도 운영 파일은 수신하지 않고 현재 개발본에서 개발 전용 시트만 제거한 정규화본을 운영 template/source snapshot으로 사용한다고 확정했다.
- 2026-09-28: 사용자 확인에 따라 운영 hidden sheet는 source snapshot에서 최종 XLSM으로 그대로 복사하고 계산 로직에서 제외하는 원본 보존 영역으로 확정했다.
- 2026-09-28: 수식·직접 참조가 없는 visible sheet 6개는 read-only로 조회하되 write·Draft·dependency·Backend 재계산에서 제외하고 최종 XLSM에 보존하는 것으로 확정했다.
- 2026-10-01: 사용자 확인에 따라 위 6개 중 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`, SUPPLY `Input Upstream ->`, `Input Midstream ->`)는 화면에 표시하지 않고 원본 그대로 복제하도록 바꿨다. read-only 표시는 `LNG Summary`, `backupLNG_TradeRestrictionInput` 2개다.
- 2026-09-28: 업무 담당 매니저 답변에 따라 TRADE `4.5 PriceModelInput` U열을 화면에서 제외하고, 후속 사용자 결정에 따라 U열을 제외한 table 값은 read-only로 표시하며 sheet 전체를 수정·계산 대상에서 제외했다. 최종 XLSM은 원본 workbook 전체를 먼저 복제하고 다른 allowlist sheet만 변경한다.
