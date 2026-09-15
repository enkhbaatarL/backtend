# MI 모델 입력 양식 카탈로그 도메인 모델 설계

## Document Control

- status: Draft
- canonical for: Business rules, ownership, lifecycle, and relationships of the MI model input template catalog
- owner: BE
- source documents:
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/design-docs/gas/mi-agent-param-proposal.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - 없음
- last synced: 2026-09-09
- open questions:
  - 행·열 추가가 끝에 붙이는 append만 허용되는지, 중간 insert도 허용되는지 FE/모델 검증이 필요하다.
  - 새 행·열에 원본 수식·서식·데이터 유효성 규칙을 어떤 방식으로 복제할지 정해야 한다.
  - 변경 파일을 읽는 모델 실행 컨테이너가 Excel 수식을 실제로 재계산하는지 확인해야 한다. workbook의 recalculation flag만으로 cached value가 즉시 갱신되지는 않는다.
  - 다른 workbook을 참조하는 수식이 실행 환경에서 해석 가능한지, 필요한 외부 파일과 링크 갱신 정책을 모델 담당자와 확정해야 한다.
  - `BaseEntity`의 `crt_*` 감사 컬럼과 DB 명명 문서의 `reg_*` 표준 충돌을 DBA/데이터 표준 담당자가 확인해야 한다.
  - 기간 축, 셀 원천(BASE/USER), 파생 테이블 표시를 스키마에 넣을지 조회 응답에서만 계산할지 정해야 한다. §14.4 참조.

## 1. Feature summary

독립수급모델 실행 input은 한 번의 실행에서 함께 사용하는 `DEMAND`, `SUPPLY`, `TRADE`
Excel 파일 3개로 구성된다. 화면이 전체 workbook을 추측해서 편집하지 않도록 서버가 아래 구조를
버전별 카탈로그로 관리한다.

`양식 버전 → 파일 → 시트 → 시트 내 테이블 → 사용자 수정 가능 범위`

이 카탈로그는 실제 업로드 파일 bytes나 사용자 수정값을 저장하지 않는다. 실제 파일은 기존
`tcm_filel`의 `SCNRCASE + case_id` 관계가 계속 소유하며, 카탈로그는 파일을 해석하고 검증하는
구조 정보만 소유한다.

## 2. Existing structure reviewed

- `tmd_mdl_scnrcasem`: 독립실행 case와 현재 상태를 소유한다.
- `tcm_filel`: case input 파일 3개의 실제 파일 metadata와 object storage 연결을 소유한다.
- `tmm_mi_model_param_m`: Agent 파라미터 제안·병합을 위한 일부 파일/시트/셀 위치를 소유한다.
  - 실제 65개 시트 중 일부만 포함하고, `model_table` 값이 실질적으로 테이블을 구분하지 못한다.
  - `use_yn`은 Agent 제안 대상 여부이므로 사용자 편집 가능 여부로 재사용할 수 없다.
- `BasecaseReadMode`, `ColumnRoleMap`: 표 읽기 방식과 열 역할 파싱 개념은 재사용 가능하다.
- 실제 파일에는 demand/supply workbook table part가 없으므로 OOXML `ListObject`만으로 테이블
  경계를 자동 생성할 수 없다.

## 3. Proposed domain model

| 엔터티 | 분류 | 책임 |
| --- | --- | --- |
| InputTemplate | configuration master | 함께 호환되는 3개 input 파일 구조의 버전과 상태 |
| InputFileDefinition | detail | 양식 버전에 속한 파일 역할, 기준 파일명, 순서, 구조 해시 |
| InputSheetDefinition | detail | 파일 안의 시트명, 순서, 표시 상태 |
| InputTableDefinition | detail | 시트 안의 논리 테이블 경계와 행·열 추가 가능 정책 |
| InputModificationScope | detail | 테이블 안에서 사용자가 값을 바꿀 수 있는 하나 이상의 직사각형 범위 |
| ScenarioCaseTemplate | relation | case 파일을 해석할 때 사용한 양식 버전의 고정 참조 |

카탈로그의 owner는 플랫폼 운영/BE다. 사용자 또는 tenant별 설정이 아니므로 회사·사용자 FK를
두지 않는다. 사용자 수정 결과는 새 독립실행 case의 파일로 저장하고 카탈로그 master를 변경하지 않는다.

## 4. Table design draft

물리 컬럼·타입·키·인덱스는
`docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md`가 canonical source다.

| 물리 테이블 | 핵심 속성 | null 의도 | 삭제/상태 |
| --- | --- | --- | --- |
| `tmm_mdlinpttmpltm` | model type, version, name, status, apply start | 설명·적용시작은 선택 | `DRAFT/ACTIVE/DEPRECATED` + soft delete |
| `tmm_mdlinptfiled` | template FK, file role, file name, order, type, structure hash | hash는 등록 전까지 선택 | soft delete |
| `tmm_mdlinptshtd` | file FK, sheet name, order, visibility status | 없음 | soft delete |
| `tmm_mdlinpttbld` | sheet FK, table name/order, baseline rectangle, header/data start, row/column-add flags | 설명만 선택 | soft delete |
| `tmm_mdlinptmodscopd` | table FK, scope order, start/end row and column | 없음 | soft delete |
| `tmd_mdl_scnrcasetmpltd` | case logical reference, template FK | 없음 | soft delete |

행 번호는 Excel 최대 1,048,576행과 실측 최대 100,803행을 수용해야 하므로
`INT UNSIGNED`를 사용한다. `SMALLINT UNSIGNED`는 최대 65,535여서 사용할 수 없다.
열 번호는 Excel 최대 16,384열을 수용하는 `SMALLINT UNSIGNED`를 사용한다.

## 5. Relationship design

```text
InputTemplate 1 ── N InputFileDefinition
InputFileDefinition 1 ── N InputSheetDefinition
InputSheetDefinition 1 ── N InputTableDefinition
InputTableDefinition 1 ── N InputModificationScope
InputTemplate 1 ── N ScenarioCaseTemplate ── 1 ScenarioCase
```

- 한 양식 버전은 파일 역할별 정의를 한 건만 가진다.
- 한 파일은 같은 시트명과 시트순서를 중복해서 가질 수 없다.
- 한 시트는 같은 테이블명과 테이블순서를 중복해서 가질 수 없다.
- 한 테이블은 여러 수정 가능 범위를 가질 수 있다. 범위가 없으면 읽기 전용이다.
- case는 양식 버전 하나에 고정된다. 양식이 나중에 바뀌어도 과거 case 해석은 바뀌지 않는다.

## 6. Business rules

1. 모델 유형별 `ACTIVE` 양식은 최대 한 건이다.
2. `ACTIVE` 전환 시 `DEMAND`, `SUPPLY`, `TRADE` 파일 역할이 각각 정확히 한 건 있어야 한다.
3. `ACTIVE` 양식과 하위 정의는 직접 수정하지 않는다. 변경은 새 `DRAFT` 버전을 복제해 수정한 뒤 활성화한다.
4. 좌표는 모두 Excel 기준 1-based 숫자다. 열 문자(`A`, `DA`)는 API 표시 시 계산하며 DB 기준키로 쓰지 않는다.
5. 테이블 baseline 경계는 `시작 <= 종료`, header/data 시작은 테이블 경계 안이어야 한다.
6. 수정 가능 범위는 부모 테이블 baseline 경계 안에 있어야 하고 같은 테이블의 다른 활성 범위와 겹치지 않아야 한다.
7. `row_add_psbl_yn`과 `col_add_psbl_yn`은 사용자 파일 인스턴스의 구조 확장 권한이다. 카탈로그 baseline 좌표를 변경하지 않는다.
8. 추가 가능 여부가 `Y`인 테이블도 셀 값 수정 권한이 자동으로 전체 테이블에 생기지 않는다. 기존 셀은 수정 가능 범위로 별도 허용한다.
9. 실제 input 파일명이 달라도 역할 식별은 `DEMAND/SUPPLY/TRADE`로 한다. `file_nm`은 기준 양식 표시·검증 정보다.
10. 알려진 양식과 구조가 맞지 않는 파일을 자동 fallback해서 다른 버전으로 읽지 않는다.
11. 삭제된 양식/하위 정의는 신규 case에 사용할 수 없고, 기존 case가 참조하는 양식은 물리 삭제하지 않는다.
12. 식별 열이 다른 표를 미러링하는 표는 단독으로 행을 추가할 수 없다. master 표에 대상 행이 먼저 있어야 한다.
    `row_add_psbl_yn = Y` 는 그 표가 행을 가질 수 있다는 뜻일 뿐, 선행 조건 없이 추가해도 된다는 뜻이 아니다.
    선행 관계를 표현하는 수단이 정해지기 전에는 이런 표의 행 추가를 API로 열지 않는다. 표본은 §14.5.
13. 행 추가는 append-only로 다룬다. 중간 insert는 위치 기반 참조, 고정 집계 범위, 고정 열 인덱스를 함께 깨뜨린다.

## 7. Extension considerations

- 다음 단계에서 셀 타입·validation·표시 라벨이 필요하면 테이블별 column definition을 별도 detail로
  추가할 수 있다. 현재 요청에는 필수 정보가 아니므로 이번 모델에 넣지 않는다.
- 기간이 열에 있는 표(§14.3 유형 A)는 연도·월 헤더 행 위치를 모르면 열을 해석하거나 추가할 수 없다.
  기간 축 표현은 column definition보다 먼저 필요할 수 있으므로 다음 slice의 첫 검토 대상으로 둔다.
- 대상 식별에 업무 이름을 쓸 수 없는 표본이 확인됐다(§11). 행 identity는 surrogate ID를 전제로 설계한다.
- 표 사이의 행 추가 선행 관계(§6 규칙 12)를 표현할 수단이 필요하다. 표 정의에 master 표 참조를 두는 방법과,
  행 추가를 표 단위가 아니라 도메인 동작(예: 설비 등록)으로 계약하는 방법이 있다. 후자는 카탈로그를 넓히지
  않지만 동작별 계약이 늘어난다. 실제 행 추가 요구가 확정될 때 함께 정한다.
- 기존 행에서는 읽기 전용이고 신규 행에서는 필수인 열이 있다(§14.5). 수정 가능 범위와 별개 개념이므로
  같은 직사각형 범위로 표현하지 않는다.
- 수정 범위가 직사각형으로 표현되지 않는 예외가 확인되면 여러 범위 row로 분할한다. 임의 JSON 좌표는
  검색·검증과 정합성 보장이 어려워 기본 저장 형식으로 사용하지 않는다.
- 독립 파일 버전 조합이 실제 운영 요구로 확인되면 template set과 file version 사이에 membership
  관계를 추가할 수 있다. 현재는 한 실행이 3개 파일을 한 snapshot으로 사용하므로 set 버전을 우선한다.
- 다른 시트의 값을 참조하는 formula cell은 원본 수식 자체가 dependency의 source of truth다. source input을
  변경할 때 target formula cell을 값으로 덮어쓰지 않고 수식을 보존한 뒤 호환되는 계산 엔진으로 재계산한다.
- table-level dependency는 영향받는 화면 table 표시, 재조회 범위 결정, template 검증을 위한 파생 metadata로는
  사용할 수 있지만 새 target 값을 계산하는 규칙으로 사용할 수 없다. 합계·lookup·연쇄 수식은 source/target
  table 관계만으로 결과를 계산할 수 없기 때문이다.
- dependency 조회 요구가 확정되면 immutable template version에서 formula를 분석해
  `sourceInputTable → targetInputTable` edge를 생성하는 방식을 우선한다. 사람이 중복 등록한 관계를 수식과
  별도 source of truth로 관리하지 않는다. 정확한 셀/range 영향 조회가 필요할 때만 edge 범위를 세분화한다.
- formula가 아니라 같은 값을 여러 위치에 하드코딩해야 하는 예외만 명시적 value-propagation rule 후보로 둔다.
  이 경우에도 template version, source/target range, 변환 규칙과 중복 적용 방지를 함께 정의해야 한다.

## 8. Data access recommendation by responsibility

- 카탈로그 CRUD와 단건 hierarchy 조회: JPA.
- 한 양식 전체를 `file → sheet → table → scope`로 읽는 화면 projection: QueryDSL 또는 전용 projection query.
- 초기 65개 시트/테이블 seed의 대량 등록: DBA 실행 SQL 또는 관리용 batch. 운영 API로 일반 사용자에게 열지 않는다.
- 실제 workbook 내용 파싱/생성: 기존 파일 서비스와 ZIP/SAX 처리 경계. 카탈로그 repository에 넣지 않는다.

## 9. API/DTO impact

API 경로·요청·응답 계약은 feature spec/OpenAPI가 소유한다. 후속 계약에는 최소한 아래를 정의해야 한다.

- template version 식별자와 파일 역할
- sheet name/order/status
- table id/name과 baseline 좌표
- `rowAddPossible`, `columnAddPossible`
- `modifiableScopes[]`의 1-based start/end row/column
- legacy case에 template mapping이 없을 때의 오류 또는 등록 절차
- 구조 불일치 파일의 오류 코드와 사용자 메시지

## 10. SQL / DBA implications

- 신규 master/detail/relation 테이블 6개 생성이며 기존 업무 테이블의 column alter는 없다.
- 초기 backfill은 기존 case 전체를 추측 매핑하지 않는다. 구조 검증된 case만 명시적으로 연결한다.
- DDL을 application보다 먼저 배포할 수 있지만, 새 application이 mapping을 필수로 읽기 전에
  active template seed와 대상 case mapping을 완료해야 한다.
- 실제 DDL은 pre-check/apply/backfill/rollback/verify/DBA action 순서로 제공한다.

## 11. Risks / open questions

- 행·열 중간 삽입은 수식 참조를 이동시킬 수 있다. append-only 여부가 확정되지 않아 실행 API 구현을 막는다.
- 새 행·열의 서식·수식 복제 규칙이 없다. 추가 가능 flag만으로 파일 생성 동작을 확정할 수 없다.
- 현재 Backend의 formula recalculation flag는 Excel-compatible engine이 파일을 열 때 재계산을 요청할 뿐,
  파일 안의 cached formula result를 Backend에서 갱신하지 않는다. 모델 container가 cached value를 읽는다면
  변경 전 값으로 실행될 수 있다.
- 실제 세 input 표본에는 다른 sheet를 참조하는 복합 수식과 외부 workbook 참조가 존재한다. table dependency
  catalog만으로 계산 결과를 갱신하는 구현은 안전하지 않다.
- `tmm_mi_model_param_m`도 같은 파일/시트 위치를 저장한다. 양식 버전 FK를 공유하지 않으면 이중 관리가 남는다.
- `BaseEntity` 감사 컬럼과 DB 명명 문서의 시스템 컬럼명이 다르다. physical DDL 적용 전 표준 예외 확인이 필요하다.
- 양식 판별 해시의 입력값 정규화 방식이 미정이다. hash column은 nullable로 두되 자동 판별을 아직 계약하지 않는다.
- 표본에서 설비명이 유일키가 아니었다. `BCRegasification`의 `Malta LNG`가 국가만 다른 2행으로 존재하고
  helper `설비명&LifeCycle`도 중복이라, 업무 이름 기반 행 identity는 성립하지 않는다.
- `RegasAdjustment`의 식별 열은 이름이 아니라 `BCRegasification`의 행 번호로 연결돼 있다. 원본 정렬이나
  행 삽입만으로 조정값이 다른 설비에 붙는다. 카탈로그 좌표로는 이 결합을 막을 수 없다.
- 같은 시트 안의 rollup 표는 수식이 대부분 값으로 굳어 있어 표시·검증에 그대로 쓸 수 없다.
  읽기 전용 표시만으로는 부족하고 재계산 책임을 정해야 한다.
- SUMMARY가 명세한 표 중 `SeasonalityLogic`은 workbook이 아니라 서버 LUT에 있다. 카탈로그를 시트 하위로만
  두면 이 표는 양식 버전 관리 밖에 남는다.
- 유형 A의 행 추가는 두 시트·세 표에 걸친 142셀 생성과 파생 집계 재계산을 요구한다(§14.5).
  현재 스키마는 표별 행 추가 가능 여부만 표현하므로, 이 상태로 행 추가 API를 열면 미러 수식만 있고
  원본이 없는 행이 만들어질 수 있다.
- 미러 수식은 원본의 빈 칸을 `0`으로 표시한다. 원본 필수값 검증 없이는 화면에 잘못된 코드값이 노출된다.
- `BCRegasification` 열 추가는 `가능`으로 명세돼 있으나 조회 수식의 열 인덱스와 연도 범위가 고정이라
  단순 append가 아니다. 열 추가 계약은 대상 수식 갱신 규칙까지 포함해야 한다.

## 12. Exit criteria / handoff

- 모델 안정성: `ready after minor cleanup` — 계층, 소유권, version, 좌표, 범위, 추가 가능 정책의 저장 형태는 확정 가능하다.
- API contract drafting: 조회-only hierarchy 계약은 작성 가능하다. 값 변경 후 실행 계약은 모델 container의
  수식 재계산 경계를 확인한 뒤 가능하며, 행·열 추가 계약은 추가 방식과 복제 규칙도 확정해야 한다.
- ERD review required areas:
  - 6개 신규 테이블의 PK/FK/UK/index와 soft-delete uniqueness
  - row/column data type 범위
  - active template 단건 보장 generated column
  - legacy case mapping과 DBA rollout 순서
  - 감사 컬럼 명명 충돌

## 13. Dictionary updates

- added: `시트 / SHT / 단일단어`
- reason: file-sheet-table 계층에서 Excel sheet를 다른 개념으로 대체할 수 없다.
- file: `docs/conventions/db-term-dictionary-sheet2.csv`
- append note: 기존 행을 수정하지 않고 마지막에 385번 row로 추가했다.

## 14. 실측 baseline 좌표 (SUPPLY 표본)

측정 파일은 양식 표본 `Supply_v14_플랫폼입력_최종.xlsm`(시트 24개)이며, 실측 근거와 절차는
`docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`의 2026-09-09 절이 소유한다.
아래 좌표는 카탈로그 seed의 출발점이자 표 경계 규칙(§6 규칙 5·6)의 검증 표본이다.
시트 24개 중 SUMMARY와 대표 유형 2종, 그 참조 시트만 실측했으므로 SUPPLY 전체 seed는 아니다.

좌표는 §6 규칙 4에 따라 Excel 1-based 숫자이며, 열 문자는 확인용 병기다.

### 14.1 표 경계 (`tmm_mdlinpttbld`)

| 시트 | 입력테이블명 | 행 | 열 | 헤더 행 | 데이터 시작 | 행추가 | 열추가 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RegasAdjustment | Monthly Regas Capacity Adjustment | 9–382 | 2–115 (`B`–`DK`) | 9–10 | 11 | Y | Y |
| RegasAdjustment | Regas Adjustment Annual Rollup | 10–382 | 117–126 (`DM`–`DV`) | 10–10 | 11 | N | N |
| RegasAdjustment | Regas Adjustment Country Monthly Rollup | 385–444 | 7–115 (`G`–`DK`) | 385–386 | 387 | N | N |

`RegasAdjustment`는 1–3행 범례·설명 블록과 8행 표 제목을 제외하면 위 3개 표가 시트를 남김없이 덮는다.
어느 표에도 속하지 않는 셀은 0개다. 이 중 사용자 편집 대상은 첫 번째 표 하나뿐이다.
| PipelineAdjustment | Monthly Pipeline Capacity Adjustment | 7–200 | 1–9 (`A`–`I`) | 7–7 | 8 | Y | N |
| BCRegasification | Annual Regas Capacity Baseline | 7–379 | 1–18 (`A`–`R`) | 7–7 | 8 | Y | Y |
| BCPipeline | Annual Pipeline Capacity Baseline | 7–70 | 2–6 (`B`–`F`) | 7–7 | 8 | Y | N |

행·열 추가 정책은 SUMMARY 시트의 G/H 열(`가능` / `고정`)을 그대로 옮긴 값이다.

### 14.2 수정 가능 범위 (`tmm_mdlinptmodscopd`)

| 입력테이블 | 순서 | 행 | 열 | 근거 |
| --- | --- | --- | --- | --- |
| Monthly Regas Capacity Adjustment | 1 | 11–382 | 8–115 (`H`–`DK`) | 월별 Capacity 값. 식별 열 `B`–`G`(2–7)는 기존 행에서 조회 수식이라 제외. 신규 행에서는 필수이며 §14.5 참조 |
| Monthly Pipeline Capacity Adjustment | 1 | 13–200 | 2–4 (`B`–`D`) | 연도·월·Pipeline 키. 8–12행은 `#REF!` 잔해라 제외 |
| Monthly Pipeline Capacity Adjustment | 2 | 8–200 | 8–8 (`H`) | 사용자 조정 Capacity |
| Annual Regas Capacity Baseline | 1 | 8–379 | 2–17 (`B`–`Q`) | 식별 5열 + 연도 2025–2035. `A` Helper와 `R` Max는 수식이라 제외 |
| Annual Pipeline Capacity Baseline | 1 | 8–70 | 2–6 (`B`–`F`) | 표 전체 |

파생 rollup 테이블 2개의 값 영역은 수정 가능 범위를 갖지 않는다(§6 규칙에 따라 읽기 전용).
예외는 아래 색 코드 항목의 마지막 줄에 적었다.

#### 색 코드 근거

원본 워크북은 `RegasAdjustment` `Q2`/`Q3`에 범례를 두고 셀 성격을 fill로 구분한다.
플랫폼입력 버전이 추가한 밝은 노랑과 함께 4종이 쓰이며, 수정 가능 범위는 이 신호와 일치한다.

| fill | 범례 | 적용 영역 | 셀 수 |
| --- | --- | --- | ---: |
| `FFFFE600` | SUMMARY "직접 수정할 값" | 표1 `H11:AE382` (2023-01~2024-12) | 8,928 |
| `theme6 / tint 0.8` | `Q2` User input | 표1 `AF11:DK382` (2025-01~2031-12) | 31,248 |
| `theme6 / tint 0.6` | `Q3` Formula cells | 표2 `DN11:DV382` 3,348 + 표3 `H387:DK444` 6,264 | 9,612 |
| `none` / `theme2` | 범례 없음 | 표1 식별 열 `B11:G382`, 표2 `DM` 국가 열 | 2,604 |

- 표2·표3의 **값 영역은 전량이 `Formula cells`** 로 표시돼 있다. 조회 전용이라는 판단이 색으로도 확인된다.
- 표1의 2025년 이후 월 셀은 수식이지만 `User input` 색이다. 색만으로 수정 가능 범위를 도출하면 이 84개 열이
  누락되고, 반대로 수식 여부만으로 도출하면 이 열이 잘못 제외된다. 두 신호를 함께 봐야 한다.
- **예외 1건**: 표3의 국가 라벨 `G387:G444` 58셀은 `User input` 색이고 실제로도 리터럴이다.
  집계 대상 국가 목록을 사람이 유지하는 구조이며, 본표에 없는 국가 8개가 이미 들어 있다.
  플랫폼에서는 이 목록을 본표의 국가 열에서 파생해야 하므로 수정 가능 범위로 옮기지 않는다.

### 14.3 대표 표 유형 2종

| | 유형 A — 행=대상, 열=기간 | 유형 B — 행=레코드 |
| --- | --- | --- |
| 표본 | RegasAdjustment | PipelineAdjustment |
| 기간 위치 | 열. 9행 연도 + 10행 월 2단 헤더, 108열(2023-01~2031-12) | 행. `B` 연도 / `C` 월 |
| 대상 식별 | 좌측 `B`–`G` 조회 열, 372행 | `D` Pipeline name |
| 값 단위 | 월 물량 `Mt LNG` | 연율 `Mt/y` |
| 기본값 출처 | 2025년 이후는 `BCRegasification` 연간 용량 ÷ 12 수식 | 없음. `G` Base capacity가 하드코딩 |
| 확장 방향 | 행(설비) + 열(기간) | 행만 |

두 유형 모두 `(대상키, 연, 월) → 값`으로 환원되므로 저장 모델을 나누지 않는다.
유형 차이는 화면 표현과 새 파일 생성 시 좌표 전개 규칙에만 반영한다.

### 14.4 이 스키마로 아직 표현되지 않는 것

1. **기간 축(period axis)** — 유형 A는 "열 = (연도 9행, 월 10행)"이라는 정보 없이는 열 해석도 열 추가도 할 수 없고,
   유형 B는 같은 기간이 열 2–3에 있다. 테이블 정의에 축 표현이 없으면 두 유형을 같은 규칙으로 다룰 수 없다.
2. **셀 원천(BASE / USER)** — 유형 A는 "수식이면 기본값, 리터럴이면 사용자 값"이다. 되돌리기와
   새 버전 생성 시 수식 보존 판단에 필요하지만 카탈로그에도 case 데이터에도 저장 위치가 없다.
3. **파생 테이블 표시** — 14.1의 rollup 2개는 읽기 전용을 넘어 "값이 stale이므로 서버 재계산" 이라는 성질을 갖는다.
   수정 가능 범위가 없다는 사실만으로는 이 차이를 표현하지 못한다.
4. **case별 변경분 저장 위치** — 카탈로그는 사용자 수정값을 소유하지 않는다(§3). 화면 편집 → 새 버전 생성에는
   `(case, 테이블, 대상키, 연, 월) → 값` 형태의 변경분이 필요하며, 소유 위치가 아직 정해지지 않았다.
5. **신규 행 필수 입력과 표 사이의 행 추가 선행 관계** — 유형 A의 식별 열은 기존 행에서 읽기 전용이지만
   신규 행에서는 필수이고, 그 값은 master 표에 먼저 등록돼야 한다(§14.5). 수정 가능 범위 하나로는
   이 두 성격을 구분할 수 없고, 표별 행 추가 플래그로는 선행 관계를 표현할 수 없다.

이 4건은 이번 모델의 결함이 아니라 다음 slice의 결정 대상이다. 확정 전에는 스키마를 넓히지 않는다.

### 14.5 행 추가는 표 단위 연산이 아니다 (유형 A)

`RegasAdjustment`의 식별 열 `B`–`F`는 사용자가 값을 넣는 열이 아니라
`=BCRegasification!B8` 형태로 **원본 행을 미러링하는 수식**이다. 오프셋 −3이 372행 전부 일치하고
양쪽 데이터 행 수도 372행으로 같아, 두 표는 1:1로 강제 대응된다.
따라서 `RegasAdjustment`에만 설비 행을 추가할 수 없다. 원본에 행이 없으면 미러 수식은 값을 만들지 못한다.
SUMMARY I15가 요구하는 순서(① BCRegasification 등록 → ② 조정 시트 연결 행 → ③ 월 기본값·조정값)와 같은 제약이다.

설비 1개를 추가할 때 실제로 채워야 하는 셀은 아래와 같다. 노랑으로 표시된 셀만으로는 행이 완성되지 않는다.

| 대상 | 셀 | 내용 |
| --- | ---: | --- |
| BCRegasification 신규 행 | 1 | `A` Helper (`=E&F`) |
| BCRegasification 신규 행 | 5 | `B`–`F` 대륙·지역·국가·설비·Life Cycle Category |
| BCRegasification 신규 행 | 11 | `G`–`Q` 연간 Capacity 2025–2035 |
| BCRegasification 신규 행 | 1 | `R` Maximum capacity |
| RegasAdjustment 신규 행 | 5 | `B`–`F` 미러 수식 (오프셋 −3 유지) |
| RegasAdjustment 신규 행 | 1 | `G` Nameplate 조회 수식 |
| RegasAdjustment 신규 행 | 24 | `H`–`AE` 2023-01~2024-12 리터럴 |
| RegasAdjustment 신규 행 | 84 | `AF`–`DK` 2025-01~2031-12 미러 수식 |
| RegasAdjustment 우측 rollup | 10 | `DM` 국가 + `DN`–`DV` 연간 합계 |
| **합계** | **142** | |

여기에 더해 하단 국가 rollup(§14.1)의 `SUMIFS` 범위가 `H$11:H$382`로 고정돼 있어 신규 행이 집계에서 빠진다.
범위를 넓히려면 58행 × 108열 = 6,264셀을 갱신해야 하는데 그중 수식이 남은 셀은 66개뿐이라 수식 수정으로는
해결되지 않는다. 신규 설비의 국가가 rollup의 58개 국가 목록에 없으면 국가 행(109셀)도 추가해야 한다.
현재 본표는 50개국이므로 8개국은 이미 자리가 있고, 그 밖의 국가는 행 추가 대상이다.

부수 확인:

- 원본의 빈 칸은 미러 수식에서 `0`이 된다. `BCRegasification` 365행 `GNL Escobar`는 Life Cycle Category가
  비어 있고, `RegasAdjustment` 368행 `F`는 문자열이 아니라 `0`을 표시한다. 원본 필수값 검증이 없으면 그대로 노출된다.
- `A` Helper와 `R` Maximum capacity는 372행 중 각각 365행·366행이 값으로 굳어 있다. 신규 행을 수식으로 채울지
  값으로 채울지 규칙이 필요하다.
- 조회 수식의 열 인덱스가 고정이다. `G`의 `VLOOKUP(...,$E:$R,14,FALSE)`는 `R`이 14번째라는 전제이고,
  월 수식의 `MATCH(연도, $E$7:$Q$7, 0)`은 연도 범위가 `Q`에서 끝난다는 전제다.
  SUMMARY가 `BCRegasification` 열 추가를 `가능`으로 두었지만, `R` 앞에 2036년 열을 넣으면 두 전제가 모두 깨진다.

결론:

- 유형 A의 행 추가는 **master 표(BCRegasification) 등록을 선행 조건으로 갖는 시트 간 트랜잭션**이다.
  표별 `row_add_psbl_yn` 플래그만으로 허용 여부를 판단할 수 없다.
- 중간 insert는 오프셋 −3, rollup 고정 범위, 고정 열 인덱스를 동시에 깨뜨린다. **append-only가 전제**다.
- 화면에서도 두 표에 각각 추가하게 두면 안 되고, "설비 등록" 하나의 동작으로 묶어야 한다.

## 15. Document control / source-of-truth notes

- 이 문서는 카탈로그의 비즈니스 소유권, version lifecycle, 관계, 수정 범위 규칙의 canonical source다.
- §14의 실측 좌표는 seed 작성과 규칙 검증을 위한 참조값이다. 실측 절차와 원본 근거는
  `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`가 소유한다.
- 물리 table/column/index/key/nullability/rollout SQL은
  `docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md`를 참조한다.
- endpoint/request/response/error는
  `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md`에서 정의한다.
- 행·열 추가 방식과 감사 컬럼 표준 충돌은 구현 전 해결해야 하지만, hierarchy 조회용 schema 초안 검토는 진행할 수 있다.
- implementation readiness: `ready after minor cleanup`.
