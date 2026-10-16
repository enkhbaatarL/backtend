# MI 모델 Input 화면 편집 Delivery Plan

## Document Control

- status: Draft
- canonical for: 배포 일정, 로컬 delivery 순서, 티켓 경계, 의존성, 상태와 agent coordination
- owner: BE
- source documents:
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
- last synced: 2026-10-13
- open questions:
  - 계산 방식은 2026-10-13 사용자 결정으로 POI 부분 계산이다(domain design §1A.4 Q7). 판별대장의 표별 정책을 적용한 계산 allowlist와 실행 격리·동기/비동기 경계가 미확정이다. 이전 `GDP`·`PIPELINE` 후보는 최신 함수 조사의 오탐이고, `_XLFN.UNIQUE` 5셀은 domain 규칙 57에 따라 원본 보존이다. UNIQUE는 정책이 바뀌어 계산 경로에 들어올 때만 다시 검증한다.

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
| `INPUT-02A` | 계산 엔진·격리 실행 후속 PoC | 표별 계산 허용 수식(규칙 54의 외부 캐시 계산 예외 포함)으로 정확도·fidelity·성능·라이선스와 동기/비동기 경계를 판정 | 02, 02B, 후보 승인 | `IN_PROGRESS` |
| `INPUT-03` | 카탈로그·Draft domain/ERD 확정 | field/option/dependency/구조 정책, Draft/change/revision/finalized Case의 logical·physical model과 DBA 검토 입력 | 01A, 02A, 02B | `BLOCKED` |
| `INPUT-04` | 조회·임시저장·최종저장 API 계약 | endpoint, DTO, 오류, 권한, 4파일 snapshot/revision/idempotency, payload/DRM 및 구조 변경 요청·검증 계약 | 01A, 02A, 02B | `BLOCKED` |
| `INPUT-05` | 카탈로그 seed와 화면 조회 API | Case의 XLSM 4개를 `file → sheet → table → field/row` projection으로 조회 | 03, 04 | `BLOCKED` |
| `INPUT-06` | Draft 생성·조회 persistence | 사용자 Draft, source snapshot, template version, revision 생성·조회 | 03, 04 | `BLOCKED` |
| `INPUT-07` | 값 적용·재계산·XLSM materialization | `SET/CLEAR`와 승인된 구조 변경을 반영하고 계산 허용 영향 수식만 재계산해 role별 XLSM bytes 4개 생성 | 02A, 03, 04 | `BLOCKED` |
| `INPUT-08` | 값·구조 변경 임시저장 | catalog validation, optimistic revision, 변경 저장, 영향 table 값 반환/재조회 | 05, 06, 07, STRUCT-02~04 | `BLOCKED` |
| `INPUT-09` | 최종저장·Case 생성 | XLSM bytes를 `MultipartFile`로 변환, 이미 구현된 독립실행 4파일 Case 생성 서비스에 전달, 중복 Case 방지, `caseId` 반환 | 06, 07, 08 | `BLOCKED` |
| `INPUT-10` | 실파일 E2E·DBA rollout 검증 | 조회→수정→임시저장→최종저장→Case→별도 Run과 DBA 적용 검증 | 05~09, STRUCT-05 | `BLOCKED` |
| `INPUT-11` | legacy 3파일 Case cutover 정리 | 대상 snapshot(`INDEPENDENT/MI_BASE/SCENARIO`), `RUNNING` 0건·MI Base 영향 확인, case master soft delete(`SCENARIO`는 연결 row 포함), 목록/API 미노출 검증, rollback 증거 | 10, 운영 승인 | `BLOCKED` |

### Dependency waves

```text
Wave 1: INPUT-01 (DONE) → INPUT-01A (DONE: latest 4-file set)
        INPUT-02 (DONE: NO-GO) → INPUT-02B (DONE)
                    │
Gate 1A:        INPUT-02A (IN_PROGRESS: POI 부분 계산 승인, 실행 경계 판정 중)
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

Wave 1은 완료됐다. 현재는 Gate 1A만 다음 실행 후보이며, 후속 wave의 병렬 가능 여부는
`INPUT-02A`의 실제 계산·운영 경계 판정을 보고 coordinating agent가 다시 판단한다.

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

## 6A. In-progress Ticket — INPUT-02A

### Ticket Goal

승인된 계산 엔진 후보로 `INPUT-02`와 같은 실파일 probe를 반복해 production 계산 경계와
동기/비동기 실행 방식을 선택한다.

### Start condition

- 2026-10-13 후보 승인: POI 부분 계산(domain design §1A.4 Q7). 이미 쓰는 의존성이라 라이선스·반입 승인이 필요 없고, 상용 엔진은 평가하지 않는다. 후보 승인은 아래 Done Criteria 충족이나 `GO`가 아니다.
- 후보가 상용 엔진이면 평가 라이선스와 의존성 반입 승인을 확보한다. 상용 엔진을 전제로 고정하지 않는다.
- 외부 workbook 원본은 계산 선행조건이 아니며 업무담당자에게 요청하지 않는다. 외부 참조의 계산/보존 구분은 domain design §6 규칙 33·54를 따른다.
- DRM 복호화 표본 또는 검증 가능한 대체 절차를 준비한다.
- `mi-model-input-sheet-calculation-compatibility.md`의 현재 검증 기준으로 표별 계산·보존·구조 변경 fixture를 선정한다. 이전 category 기반 oracle/254 family 목록을 그대로 실행하지 않는다.

### Done Criteria

- 승인된 source 값·구조 변경 후 계산 allowlist의 same-sheet/cross-sheet 결과가 golden과 일치하고, 계산 제외 수식·cached value는 보존된다.
- 승인 정규화 source의 계산 제외 formula·cached value와 externalLink, macro, style, merged cell, validation, named range를 보존한다. domain 규칙 54의 계산 결과 및 구조 변경 승인 영역만 계약대로 갱신한다. 이전 42,394셀을 최신 4파일 전체의 확정 합계로 사용하지 않는다.
- 실제 4개 파일의 wall time과 peak memory를 측정하고 승인 계산 경로와 OUTPUT의 값/구조 변경·미표시 A 보존 검증을 포함한다. OUTPUT의 최신 정책·검증 경로는 domain 규칙 50/§6.1과 compatibility의 V-OUTPUT 항목을 따른다. 과거 Helper 포함 817셀을 계산 allowlist로 사용하지 않으며 최신 family·cell 수는 source hash·표별 계산 범위 확정 후 재집계한다.
- web request JVM 내 동기 처리와 격리 worker/비동기 처리 중 안전한 경계를 판정한다.
- 라이선스, DRM 임시 파일 수명·삭제, timeout/kill, 외부 link 비갱신·보존 정책을 기록한다.
- `GO / CONDITIONAL / NO-GO`와 `INPUT-03`, `INPUT-04`, `INPUT-07`에 넘길 제약을 확정한다.

### Stable outputs

- Work note: 티켓 착수일에 `docs/work-notes/YYYY/YYYY-MM-DD-model-input-calculation-engine-followup.md` 생성
- Evidence artifact: 기존 `docs/references/mi-model-input-calculation-engine-spike.md` update

## 7. Remaining Epic 1 ticket boundaries

후속 티켓은 선행 결과 통합 후 표준 task prompt를 작성한다. 지금 단계에서는 아래 완료 경계만 고정한다.

### INPUT-03 — 카탈로그·Draft domain/ERD 확정

- field/options/dependency/구조 정책과 Draft/change/finalized Case의 책임·lifecycle을 canonical domain design에 확정한다.
- physical table/JSON 경계, key/index, revision uniqueness, active Draft cardinality와 DBA rollout을 DDL draft에 확정한다.
- `new-domain-model-design`과 `erd-change-review`를 적용한다.

### INPUT-04 — 조회·임시저장·최종저장 API 계약

- 조회 projection, Draft 생성/조회, 값·구조 변경 임시저장, 최종저장 endpoint와 DTO/error/OpenAPI 계약을 확정한다.
- 계산 시간에 따른 동기/비동기, 임시저장 계산 실패, DRM/payload/snapshot/idempotency를 포함한다.
- `backend-feature-spec`을 적용한다.

### INPUT-05 — 카탈로그 seed와 화면 조회 API

- 승인된 template version과 source snapshot으로 화면 대상 table projection만 반환한다.
- 원본 binary·storage 식별자를 노출하지 않는다.
- 조회 API slice만 구현하고 write API는 포함하지 않는다.

### INPUT-06 — Draft 생성·조회 persistence

- source case/files/template version을 고정한 사용자 Draft와 optimistic revision을 생성·조회한다.
- 값 변경 적용, XLSM 생성, Case 생성은 포함하지 않는다.

### INPUT-07 — 값 적용·재계산·XLSM materialization

- 고정 source snapshot의 원본 workbook 파일 전체를 먼저 복제하고 catalog가 허용한 sheet/cell만 변경한다.
- catalog가 허용한 기존 field의 `SET/CLEAR`와 `STRUCT-02~04`에서 확정한 구조 변경을 반영한다.
- Backend 계산 경계에서 계산 허용 수식/cached value만 갱신한다.
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
- role별 bytes를 `MultipartFile`로 변환해 이미 4파일을 받는 Fundamental Model Case application service에 전달한다. Case 생성 서비스의 4파일 계약을 다시 구현하지 않는다.
- 같은 Draft 재시도에 Case를 중복 생성하지 않고 `caseId`를 반환한다. Run은 호출하지 않는다.

### INPUT-10 — 실파일 E2E·DBA rollout 검증

- 실제 파일 조회부터 별도 Run까지 전체 흐름과 원본 불변성을 검증한다.
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
| `INPUT-02A` | In progress — POI 부분 계산 승인(2026-10-13). 실행 경계·남은 fixture 비교 진행 | `docs/work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md` (10-08 표본 준비·10-12 4사례 결과는 `2026-09-03-model-input-transfer-format.md`) |
| `INPUT-11` | Do not dispatch — 4파일 E2E 완료와 운영 승인 필요 | 착수 시 별도 생성 |
| 나머지 | Do not dispatch | 선행 티켓 완료 후 지정 |

다음 coordinator action은 승인된 POI 부분 계산으로 동기/비동기 판정 근거(4파일 시간·메모리·동시 요청)와
남은 fixture의 Excel 비교를 확보하는 것이다. 검증 범위는 계산 호환성 reference의 최신 표별 계산·보존·구조 변경 목록을 따른다. `GDP`·`PIPELINE`은 오탐이라 검증 대상이 아니며, `_XLFN.UNIQUE` 5셀은 원본 보존 검증만 한다(domain 규칙 57). `INPUT-03`, `INPUT-04`는 계산이 web request 안에서 끝나는지,
격리 worker와 계산 상태가 필요한지 확정된 뒤 배정한다.

## 10. Change Log

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
