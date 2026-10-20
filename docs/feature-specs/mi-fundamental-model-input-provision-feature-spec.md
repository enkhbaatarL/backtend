# MI Fundamental Model Input 제공 Feature Spec

## Document Control

- status: Draft
- canonical for: MI Fundamental Model input 조회·업로드 파싱 API의 요청 식별 경계, 응답 범위와 검토 중인 endpoint/field/error 계약. 저장 Case 숫자 표시 정책은 §3.2, QRY-01/02의 추출 표시 JSON에 대한 DRM 비적용은 §5가 소유한다.
- owner: Shared (BE / FE / Security·DRM)
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/references/mi-model-input-catalog-inventory-2026-09-28.md`
  - frontend `docs/ko/fundamental-model-sheet-api.md` (`feature/parkjaehyun`, `014733c372bf75665b7b2c541657cf32711e032d`) — FE 요구 근거, 이 문서와 동일한 계약으로 확정된 것은 아님
  - frontend `docs/ko/fundamental-model-sheet-api.openapi.yaml` version `0.4.0` — 필드 이름/셀 표현 재사용 근거
- depends on:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `SECURITY.md`
- supersedes:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`의 기존 `API Slice C — Input 내용 제공` 초안
- last synced: 2026-10-19 (개발 시트 포함 파일 수용과 응답 제외를 domain 규칙39~40에 정렬)
- open questions:
  - sheet/table projection의 원본 수식은 셀 표시 데이터로 전달하지 않는다. 별도 화면 갱신 규칙의 JSON 필드 이름·타입, 참조/대상 표현, 지원 계산 방식과 적용 순서를 확정해야 한다. 구체 범위는 §3.1C를 따른다.
  - 전체 구조와 선택 표 값을 같은 응답에 동봉할지, 초기 구조 조회와 후속 표 값 조회를 별도 operation으로 나눌지 확정해야 한다.
  - endpoint path/method, 표 선택 request와 최초 선택 표 정책, operation별 필드 필수/nullable 및 version 표현을 확정해야 한다. §3의 응답 필드 이름과 셀 표현은 이번 사용자 결정에 따른다.
  - 단일 표의 응답 크기 상한/paging, 4개 role의 일관된 snapshot과 구조/값 조회 실패 처리를 확정해야 한다.
  - 현재 조사 후보의 최종 표시 table 목록과 표 경계/header/row identity를 `INPUT-03`에서 확정해야 한다. 조회 필드와 수정 허용 범위는 별도로 정한다.
  - 연결 편집표가 다른 표의 값을 필요로 할 때의 추가 조회 흐름을 확정해야 한다. 단일 표 값 응답에 다른 표의 값을 암묵적으로 포함하지 않는다.
  - domain 규칙 47의 LiqUtilAdjustment 행·열 삭제 허용을 전달하는 구조 정책 표현과 삭제 대상 범위·위치 제한·삭제 후 Period/수식 갱신의 write 계약을 확정해야 한다.
  - LNG Demand 모드의 값 셀은 C1이고 선택값 변경은 domain 규칙 50이 허용한다. 판별대장 T-020은 이 control을 T-021 표와 함께 표시하도록 확인했다. 원본 선택지의 `Dynamaic` 오타를 고칠지와 화면 선택지·저장값 표기는 아직 결정되지 않았으며, 결정 전에는 options 값을 확정하지 않는다.
  - 업로드 원본/파싱 결과의 보존 방식·소유권·수명, `inputId`를 사용하는 후속 표 값 조회 request 및 여러 kind 업로드의 동일 template version 보장 방식을 확정해야 한다.
  - POST /input-uploads의 full prefix, 최초 표 선택/표 선택 request와 tableData null 여부, 지원 workbook 형식/용량·압축 해제 상한 및 구체 오류 HTTP/ResultCode를 확정해야 한다. FE의 .xls/.xlsx/.xlsm·50 MiB는 아직 승인된 BE 수치가 아니다.

## 1. Feature Goal

계약 탐색 경계: 저장 Case의 사용자 확정 QRY-01/02는 [조회 상세 §1.1·§3~5](mi-fundamental-model-input-query-api-proposal.md)가 소유한다. 이 문서의 그 범위에 대한 과거 미결 문구는 재질문 대상이 아닌 통합 대기 이력이다. 신규 Draft 생성/재개·조회·임시저장 접수/결과의 BE 추천 계약은 [공통 편집 API 초안](mi-fundamental-model-input-draft-editing-api-proposal.md)이 소유한다. 공통 BE 묶음은 2026-10-16 사용자 채택했으며 해당 문서 전체는 Draft다. Sector의 채택 정책은 domain 규칙45, 공개 검증은 편집 API §4.1이 소유한다. 최종저장 전체 계약은 그 초안 §9의 다음 slice다. 아래 조회/업로드 계약을 Draft 쓰기 계약으로 확대하지 않는다.

선택한 Fundamental Model `caseId`의 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` input에 대해 화면의 `파일 → 시트 → 테이블` 구조를 그리는 정보는 네 파일 전체를 제공하고, 헤더와 실제 입력값은 선택한 논리 입력 영역 하나 및 명시적으로 연결된 부속 control만 제공한다. 원본 파일은 FE 응답으로 전달하지 않는다.

이 문서는 MI Base 목록·상세·지정 API를 정의하지 않는다. 해당 계약은 [MI Fundamental Model MI Base 지정 Feature Spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 소유한다.

사용자 업로드 파일도 요청 `kind`에 대응하는 저장된 양식 정의로 서버에서 검증·파싱하고, §3의 조회 응답과 같은 구조/셀/control 표현으로 화면에 제공한다. 업로드 operation의 범위는 §3.3을 따른다.

## 2. Fixed Decisions

- 저장된 case input 원본 4개의 소유 단위와 요청 대상 식별자는 `caseId`다. 아직 case가 없는 단일 파일 업로드는 §3.3의 별도 요청 경계를 따른다.
- `runId`, `execType`, `fileNo`, bucket, object key는 대상 실행 건을 선택하는 공개 요청 식별자로 사용하지 않는다.
- 서버가 `caseId + SCNRCASE relation`으로 연결된 원본을 선택한다.
- 원본 Excel bytes를 Base64로 JSON에 넣는 방식은 비파일 전달로 보지 않으며 후보에서 제외한다.
- object storage 경로, bucket, object key, 원본 binary를 응답에 포함하지 않으며 ZIP 또는 역할별 원본 파일 다운로드 API를 제공하지 않는다.
- 화면 탐색용 구조 정보는 `file role → sheet → table` 계층으로 네 역할 전체를 제공한다. 이 전체 구조에는 각 표의 실제 셀 값을 넣지 않는다.
- 값 조회는 선택한 table 하나의 헤더·행·열·값과 §3.1B의 부속 control을 반환한다. 다른 table의 grid나 sheet 전체의 값은 함께 반환하지 않는다. 선택한 표의 읽기 전용 값도 표시 데이터에 포함할 수 있으며 조회 범위를 수정 가능 셀로 제한하지 않는다.
- 여기서 `table`은 시트 안의 선택 가능한 논리 입력 영역이다. 일반 표, 표와 부속 UI control(드롭다운 등)의 조합, control만 있는 영역을 포함한다. 선택 영역의 부속 control은 §3.1B의 명시적 연결에 따라 함께 반환할 수 있다.
- 표 단위 조회와 API 호출 수는 별개다. 전체 구조와 단일 표 값을 한 응답에 담을지 별도 operation으로 나눌지는 Open Questions에서 결정한다. raw workbook 또는 범용 전체 sheet cell grid를 화면 계약으로 사용하지 않는다.
- 백엔드 양식 카탈로그가 표시 대상 테이블, 필드 정의, 수정 가능 범위, 행·열 추가 정책, 사용자가 선택할 header/값 후보, 테이블 간 영향 관계를 소유한다.
- 이번 release의 구조 변경 포함과 마지막 행·열 추가 중심 원칙, 중간 삽입·삭제 예외는 domain design §6 규칙 13·26·45·47을 따른다. 조회 응답의 구조 정책과 화면 갱신 규칙은 이 범위를 표현해야 하며, 구체 구조 변경 요청·검증·저장은 별도 write 계약에서 확정한다.
- 2026-10-06 사용자 전달 FE 합의: 수식 셀도 화면에는 값으로 전달하고, 입력/열 추가 시 필요한 화면 갱신은 별도 규칙 정보로 제공한다. FE가 적용할 규칙의 전달 범위와 미결정 표현은 §3.1C를 따른다.
- 전수 조사에서 분류된 화면 대상은 후보일 뿐이며 최종 표시 table 목록과 표 경계는 별도로 확정한다. 표 단위 데이터 제공이라는 결정만으로 표 내부 일부 field만 선별하거나 계산값을 모두 조회에서 제외한다고 해석하지 않는다. 조회 대상과 수정 허용 범위는 구분한다.
- 개발 전용 시트의 설명에서 도출한 후보도 같은 개발본의 운영 대상 business sheet 구조와 대조·승인한 뒤에만 catalog seed로 등록한다.
- 이 문서는 조회와 화면용 업로드 파싱 계약을 소유한다. `임시저장`과 `최종저장`은 별도 write 계약에서 정의하며 `미리보기` action은 두지 않는다.
- 응답 범위와 공통 필드 이름/셀 표현은 §3과 같이 고정한다. endpoint와 operation별 필수/nullable·초기 선택은 아직 확정하지 않는다.
- `SUMMARY`, `플랫폼 입력 안내`가 포함된 파일의 수용·원본 보존과 기준 정규화본의 역할은 [domain 규칙39~40](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)을 따른다. 두 개발 시트는 runtime catalog와 조회 응답에 포함하지 않으며, 파일 안에 있다는 이유만으로 호환 파일의 연결을 거부하지 않는다. 이 결정은 §3.3의 미구현 업로드 파싱 API가 구현됐다는 뜻이 아니다.
- 운영 기준 정규화본의 hidden sheet는 조회 projection, Draft 변경, dependency 분석과 Backend 계산에서 제외한다.
  최종 XLSM 생성 시에는 source snapshot에서 순서·이름·숨김 상태와 worksheet/package 내용을 그대로 복사한다.
  hidden sheet를 삭제하거나 빈 sheet로 다시 만들지 않는다.
- 수식과 직접 참조가 없는 것으로 확인된 visible sheet 6개는 Draft 변경, dependency graph와 Backend workbook 재계산에서 제외하고 최종 XLSM에는 원본 상태로 보존한다. 화면 표시는 둘로 나눈다.
  - read-only 표시 sheet인 SUPPLY `LNG Summary`는 source snapshot 값을 조회 응답에 표시하되 수정 가능 field를 제공하지 않는다. TRADE `backupLNG_TradeRestrictionInput`은 domain 규칙 57의 미표시 대상이다.
  - 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`, SUPPLY `Input Upstream ->`, `Input Midstream ->`)는 조회 응답에 포함하지 않는다.
- domain design §6 규칙 57에 따라 최신 SUMMARY에서 표 전체가 미표시로 확정된 표는 조회 응답에 포함하지 않는다. 표 일부 열의 미표시와는 구분한다.
- TRADE `4.5 PriceModelInput`은 domain 규칙 44에 따라 T-183/T-184를 수정 가능한 표로 제공한다. 선택 표의 `inputCells`는 승인 입력을 포함하며 고정 제목·헤더는 조회만 제공한다. 입력 범위·아래 행 추가·열/기간 고정 및 최종 파일 반영/보존 경계는 규칙 44가 소유한다. 규칙 57의 미표시 T-177~T-182와 U열은 화면에서 제외하며 다른 표의 수정 가능 field로 노출하지 않는다.
- 2026-10-06 사용자 결정: SUPPLY `ProductionAdjustment`는 **C — 구조변경**이며 행 추가가 가능하다. `L7:L200`은 헤더/본문 모두 화면에서 제외한다. 해당 값과 수정 위치는 `tableData.columns/rows/inputCells`에 넣지 않고 control로 우회 노출하지 않는다. 이 범위의 `L7`은 헤더, `L8:L200`은 기존 오류 수식이다. 기존 L8:L200 수식은 원본 그대로 보존하고 신규 행의 L셀은 빈칸으로 둔다. 기존/신규 L셀의 화면·재계산 제외 및 오류 판정은 domain 규칙 31/32를 따른다. 그 밖의 행 추가 write 상세는 별도 계약으로 확정한다.
- 기존 3파일 `INDEPENDENT | MI_BASE` case는 4파일 cutover 전에 soft delete한다. `OUTPUT`을 보완하거나
  신규 template에 mapping하지 않으며, 삭제 후 모든 active-resource API에서 `404 RSC_NOT_FOUND`로 취급한다.
- `INDEPENDENT | MI_BASE`의 legacy cleanup은 case master의 `del_yn`만 변경한다. 연결 파일·run history·결과 데이터의 physical delete는 이 계약 범위가 아니다. 이 조회 API가 다루지 않는 3파일 `SCENARIO` case의 정리 범위는 domain 규칙 36을 따른다.

## 3. Fixed Response Scope — 전체 구조 + 선택한 표 하나의 값

2026-10-01 사용자 결정에 따라 전체 snapshot/role/sheet 단위의 실제 값을 일괄 반환하는 후보를 선택지에서 제외한다. 반환 단위는 아래 두 가지다.

| 정보 | 반환 범위 | 포함 내용 | 포함하지 않는 내용 |
| --- | --- | --- | --- |
| 화면 구조 | DEMAND/SUPPLY/TRADE/OUTPUT 네 파일의 전체 표시 구조 | 파일 역할·표시명/순서, 하위 시트명/순서, 표 ID·이름·설명/순서, 위치·좌표 등 표를 선택하고 렌더링할 구조 정보 | 모든 표의 실제 값, 전체 sheet grid |
| 선택 영역 데이터 | 요청으로 선택한 table 하나와 명시적으로 연결된 부속 control | 해당 표의 표시 헤더·열·행·값, 다중 헤더/기간 정보, 원본 위치와 수정 가능 정보, 부속 control의 현재 값·선택지 | 다른 표의 grid 값, 연결되지 않은 control 값, 전체 sheet 값 |

여기서 전체 구조는 **화면 표시 대상의 전체 구조**다. §2에서 확정한 hidden/development sheet, 빈 구분 탭, TRADE U열 등 제외 정책을 해제하는 결정은 아니다.

### 3.1 공통 응답 구조 — FE 필드 이름 재사용

2026-10-01 사용자 선택에 따라 FE의 `sourceKey`, `sections → sheets → tables`와 `columns/rows/inputCells/inputGuideRules` 이름 및 셀 표현을 유지한다. 기존 `sheets[].data`는 제거하고 선택 표 데이터를 `tableData`에 둔다. 별도 `structure.files`, `headers`, `editingInfo` 응답 필드는 만들지 않는다.

```text
BaseResponse.data
  sourceKey
  sections[DEMAND, SUPPLY, TRADE, OUTPUT]
    kind
    inputId
    sheets[]
      sheetName
      tables[]    ← ID·이름·순서·좌표 등 전체 구조 정보; 실제 셀 값 없음
  tableData
    tableId       ← sections[].sheets[].tables[].id에 해당하는 선택 표
    columns
    rows          ← 선택 표의 헤더 행과 본문 값만
    inputCells
    inputGuideRules
    controls[]    ← 선택 영역에 연결된 드롭다운 등의 정의·현재 값
```

전체 구조와 선택 표 값을 동봉할 때의 공통 응답 구조다. 구조 조회/값 조회를 별도 operation으로 나눌지, 최초 표 선택 전 `tableData`를 어떻게 표현할지는 Open Questions에서 확정한다. 아래 타입은 이번 공통 표현의 기준이며 operation별 required/nullable 계약을 대신하지 않는다.

| 필드 | 타입/내용 | 의미 |
| --- | --- | --- |
| `sourceKey` | string | BE가 결정하는 출처 식별자. source 선택과 응답 대조에 사용하며 저장된 출처의 원본 선택/권한 경계는 기존 `caseId + SCNRCASE`를 유지한다. |
| `sections` | array | 저장 case의 전체 구조 조회에서는 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 순서로 각 역할 정확히 하나. 업로드 응답은 §3.3의 해당 kind 하나다. 배열 순서가 화면 표시 순서다. |
| `sections[].kind` | `DEMAND \| SUPPLY \| TRADE \| OUTPUT` | 파일 역할. |
| `sections[].inputId` | string | 해당 역할의 원본/metadata 기준을 구별하는 서버 식별자. 파일명이나 저장소 경로를 쓰지 않는다. 발급·유효기간·전체 set version 표현은 미정이다. |
| `sections[].sheets` | array | 표시할 시트 전체를 순서대로 반환. hidden/development sheet 등 기존 제외 정책을 따른다. |
| `sheets[].sheetName` | string | 원본 시트명. 역할과 함께 시트를 식별한다. |
| `sheets[].tables` | array | 해당 시트의 선택 가능한 입력 영역 metadata 전체. `id`, `kind`, `sheetName`, `name`, `description`, `location`, `rowPolicy`, `columnPolicy`, `coordinates`를 FE와 같은 이름으로 유지하고 §3.1B의 `controlIds`를 추가한다. 셀 값은 포함하지 않는다. |
| `tableData.tableId` | string | 선택한 입력 영역의 `tables[].id`와 일치한다. 값 응답은 이 영역의 grid 및 명시적으로 연결된 부속 control만 담는다. |
| `tableData.columns` | array of `{field, headerName}` | 선택 표의 표시·헤더·빈 입력 셀에 필요한 원본 열. `field`는 원본 0-based 열 번호의 `columnN`, `headerName`은 Excel 열 문자다. 다중 헤더 문자열은 rows에서 읽는다. |
| `tableData.rows` | array of `{id, rowNumber, columnN: string \| number}` | 선택 표의 필요한 헤더 행과 본문 값. 원본 1-based `rowNumber` 오름차순, `id = rowNumber - 1`을 사용하고 범위별로 재번호화하지 않는다. 이 `id`는 source/표 범위 안의 화면 행 식별자이며 구조 변경 후 영속 row key를 대신하지 않는다. |
| `tableData.inputCells` | integer array | 선택 표의 수정 가능 셀을 `rowNumber * 16384 + zeroBasedColumnIndex`로 표현. 빈 입력 셀 포함. 없으면 `[]`. 수정 가능 판정은 BE의 확정 정책을 따른다. |
| `tableData.inputGuideRules` | array | 선택 표에 적용되는 안내 metadata. 없으면 `[]`. FE의 `sheetName`, `range`, `role`, `item`, `emptyHandling`, `startRow`, `endRow`, `startColumn`, `endColumn` 이름과 `scenario/reference/conditional` 역할 표현을 유지한다. 범위의 행은 1-based, 열은 0-based, 양 끝 포함이다. |
| `tableData.controls` | array | 선택 영역의 `controlIds`에 명시된 UI control을 표시 순서대로 반환한다. 일반 표에 control이 없으면 `[]`. 타입과 값/binding은 §3.1B를 따른다. |

- 표 `id`는 전체 sections 안에서 유일하고 동일 논리 표는 출처가 달라도 같은 ID를 유지한다. 우선 기존 FE의 `kind/sheetName/name` ID를 재사용하며 표명 변경 시 ID/전용 컴포넌트 영향은 함께 검토한다.
- 표 metadata의 `kind`와 `sheetName`은 상위 section/sheet와 일치한다. `location`과 `coordinates`는 같은 표 범위를 설명하며 header/기간 행도 해당 표의 표시 기준이다.
- `inputId`가 구별하는 source 기준이 다르면 이전 데이터·편집 초안을 다른 원본에 적용하지 않는다. 단일 표 조회의 실제 request 연결과 stale 응답 처리 계약은 operation 확정 때 명시한다.
- `rowPolicy/columnPolicy`는 기존 표시 문구를 유지한다. 실제 행·열 변경은 표별 확정 정책을 따라야 하며 현재 FE의 문자열 분기나 전용 컴포넌트 예외를 BE write 허용 근거로 삼지 않는다.
- `inputGuideRules`는 개발 전용 안내 sheet 자체를 응답에 넣는다는 뜻이 아니다. 필요한 안내를 catalog metadata로 제공할 수 있으며 색이나 안내 sheet 존재를 실행 때의 편집 권한 원천으로 삼지 않는다. 안내 범위가 표시된다는 사실만으로 모든 셀이 수정 가능해지는 것도 아니다.

### 3.1A 선택 표 데이터 예시

아래는 형식 검토용 가상 값이다. 전체 응답 중 `tableData`만 발췌했고 행도 축약했다. 실제 조회에서는 선택 표의 계약 범위 전체를 반환한다.

```json
{
  "tableData": {
    "tableId": "DEMAND/BCInput/Base Case Hydrogen Demand",
    "columns": [
      { "field": "column53", "headerName": "BB" },
      { "field": "column54", "headerName": "BC" },
      { "field": "column55", "headerName": "BD" },
      { "field": "column56", "headerName": "BE" }
    ],
    "rows": [
      { "id": 2, "rowNumber": 3, "column53": "Country", "column54": "2025", "column55": "2026", "column56": "2027" },
      { "id": 3, "rowNumber": 4, "column53": "Example Country", "column54": "1.00", "column55": "1.10" }
    ],
    "inputCells": [],
    "inputGuideRules": [],
    "controls": []
  }
}
```

### 3.1B 표와 부속 UI control

`tables`/`tableData` 이름을 유지하되 모든 입력 영역에 행·열 형태를 강제하지 않는다. 일반 표는 기존 grid 필드와 `controls: []`를 사용한다. 표 위에 드롭다운이 있으면 grid와 controls를 함께 반환한다. 모드 선택만 있는 영역은 `columns/rows/inputCells/inputGuideRules: []`와 controls를 반환하며 가짜 헤더/본문 행을 만들지 않는다.

- `tables[].controlIds`: 해당 입력 영역에 함께 표시할 control ID 배열. 없으면 `[]`. 현재 값·선택지는 전체 구조에 넣지 않고 선택 데이터의 controls에만 담는다. 동일 control을 여러 영역에서 사용하면 동일 ID를 참조한다.
- 각 control의 `id`는 template 안에서 안정적이고 유일하다. `sourceTableId`는 그 값의 소유 영역을 가리키며 전체 구조의 기존 `tables[].id`와 일치한다. `tableData.tableId`는 사용자가 선택한 영역이고 `sourceTableId`는 control의 소유 영역이므로 둘은 다를 수 있다. 공유 control도 같은 source/template 및 같은 sheet 안에서 catalog가 명시적으로 연결한 경우만 동봉한다.
- grid와 control은 표시 형태만 다르다. 동일 원본 셀의 현재 값을 두 표현에 중복하지 않는다. control로 표현한 값 셀은 rows의 해당 셀 필드에서 제외하고 control의 value가 소유한다. 선택 영역 밖 다른 표의 grid 전체를 control에 넣어 반환하지 않는다.

| control 필드 | 타입 | 의미 |
| --- | --- | --- |
| `id` | string | `controlIds`에서 참조한 control 식별자. |
| `type` | `select` | 현재 구체 사례는 단일 선택 드롭다운이다. 다른 UI 유형은 실제 조사 사례에 따라 별도 타입 계약을 추가한다. 임의 JSON을 받는 확장 필드는 두지 않는다. |
| `label` | string | 화면에 표시할 제목. |
| `position` | `beforeGrid` | 표 위에 표시. control만 있는 영역에서도 이 순서로 렌더링한다. controls 배열이 control 사이의 표시 순서다. |
| `sourceTableId` | string | 실제 값/편집 위치를 소유한 입력 영역 ID. |
| `binding` | `{rowNumber, columnIndex}` | 소유 영역의 원본 sheet에 있는 값 셀. 행은 1-based, 열은 0-based. 렌더링 control ID와 원본 셀 주소를 구분한다. |
| `value` | string \| number \| null | 현재 저장값. null은 미선택/빈 값이며 grid 셀의 null 금지와 별도 계약이다. options에 없는 기존 값도 숨기거나 첫 선택지로 자동 대체하지 않는다. |
| `options` | array of `{value, label}` | 선택지의 저장값(string 또는 number)과 표시명. 순서 유지, 저장값 중복 금지. 원본/확정 catalog를 기준으로 제공하고 FE가 값을 하드코딩해 보충하지 않는다. |
| `editable` | boolean | catalog의 확정 편집 정책을 따른다. false여도 현재 값과 선택지를 조회할 수 있다. 드롭다운을 렌더링한다는 사실만으로 수정 권한이 생기지 않는다. |

`inputCells`는 grid로 표시한 수정 가능 셀에 사용하고 control 수정 여부는 `editable`로 표현한다. 두 표현 모두 같은 BE 정책에서 도출한다. control 변경을 저장할 때의 request와 검증은 별도 write 계약에서 control ID를 소유 영역/binding에 연결해 정의한다. 조회 응답만으로 원본 셀 쓰기나 재계산을 수행하지 않는다.

**LNG Demand 적용:** 2026-10-07 사용자 확인으로 T-020 `LNG Demand Mode`는 판별대장의 **E 부속 control**이며 T-021 `Annual LNG Demand Reference`의 grid와 함께 반환한다. 현재 FE의 `DEMAND/LNG Demand/LNG Demand Mode`는 `B1:C1`, 설명 `Fixed / Dynamic 선택`, `headerRow: 0`인 항목이며 이 기존 ID를 모드 control의 소유 영역으로 유지한다. 실제 값 셀은 C1(`binding: {rowNumber: 1, columnIndex: 2}`)이고 domain 규칙 50의 선택값 편집 허용을 따른다. catalog는 연간표의 `controlIds`로 연결하며 C1을 별도 grid 셀로 중복 전달하지 않는다. 원본 선택지 오타의 수정 여부와 화면 선택지·저장값 표기는 Open Questions로 유지하며 FE의 현재 열 추가 예외를 BE 변경 허용으로 해석하지 않는다.

아래는 확정된 C1 binding·연간표와의 연결·편집 허용을 반영한 **형식 검토용 예시**다. control ID, 현재 `value`, `Fixed/Dynamic` options 표기는 설명용이며 실제 저장값·선택지를 확정하는 근거가 아니다. grid는 설명을 위해 축약했다. 이 예시의 선택 영역 metadata에는 `controlIds: ["DEMAND/LNG Demand/mode"]`가 있어야 한다.

```json
{
  "tableData": {
    "tableId": "DEMAND/LNG Demand/Annual LNG Demand Reference",
    "columns": [{ "field": "column2", "headerName": "C" }],
    "rows": [{ "id": 3, "rowNumber": 4, "column2": "Example Country" }],
    "inputCells": [],
    "inputGuideRules": [],
    "controls": [{
      "id": "DEMAND/LNG Demand/mode",
      "type": "select",
      "label": "LNG Demand Mode",
      "position": "beforeGrid",
      "sourceTableId": "DEMAND/LNG Demand/LNG Demand Mode",
      "binding": { "rowNumber": 1, "columnIndex": 2 },
      "value": "Fixed",
      "options": [{ "value": "Fixed", "label": "Fixed" }, { "value": "Dynamic", "label": "Dynamic" }],
      "editable": true
    }]
  }
}
```

### 3.1C 셀 표시값과 별도 화면 갱신 규칙

2026-10-06 사용자 전달 FE 합의다. `rows`의 수식 셀은 계산 결과값으로 표시하며 원본 수식 문자열을 셀 값으로 보내지 않는다. 입력 변경·열 추가에 따라 다른 표시값이 바뀌어야 하면 별도 갱신 규칙을 제공하고 FE가 화면에서 적용한다. FE는 수식 여부를 알아내거나 workbook 수식을 직접 해석하지 않는다.

- 규칙은 발동 동작, 참조할 입력, 갱신 대상과 계산/초기값 생성 방법을 식별할 수 있어야 한다. 구체 JSON 필드 이름·타입·연산 표현·적용 순서는 Open Questions이며 지금 임의의 응답 필드를 확정하지 않는다.
- 규칙과 값은 같은 source/template 및 선택 표 기준으로 제공한다. 별도 규칙 전달이 다른 표의 grid 값을 암묵적으로 동봉하는 근거가 되지는 않는다.
- BE는 domain 규칙 46에 따라 DB에 저장한 양식 version별 카탈로그 갱신 규칙을 읽어 제공한다. 물리 저장 구조는 기존 catalog ERD/DDL 설계가 소유하며 응답 JSON 형태와 DB 저장 형태가 같아야 한다고 가정하지 않는다.
- FE는 BE가 전달한 규칙을 적용하고 계산값의 직접 수정 가능 여부는 기존 `inputCells`/control `editable`을 따른다. 규칙 적용 대상이라는 이유만으로 직접 수정 권한이 생기지 않는다.
- 확정 사례: `LiqUtilAdjustment`의 마지막 월 열 뒤에 열을 추가하면 새 열의 `Period` 표시값은 직전 마지막 Period + 1이다. 일반 헤더 `B10:F10`과 월별 `Year/Month/Period`의 표 형태는 기존 카탈로그 설계 대상이다.
- `LiqUtilAdjustment`의 행·열 삭제 허용은 domain 규칙 47을 따른다. 삭제 시 FE에 전달할 갱신 규칙은 삭제 범위·위치와 Period/수식 처리의 write 정책을 확정한 뒤 구체화한다.
- `H7:CL7`은 같은 시트 내부값 참조 수식이다(2026-10-07 사용자 확인으로 범위 정정). `G7`은 시작 연도 값으로 수식 범위에서 제외하며 고정 헤더 정책은 유지한다. 조회에는 결과값을 제공하며, 참조 입력 변경 시 FE에 전달할 갱신 규칙의 구체 계산식은 실제 원본 수식 확인 후 확정한다. 모든 수식 셀에 적용 가능한 일반 Excel 계산기를 FE에 요구하는 결정은 아니다.
- BE write 검증·계산 허용 수식 처리·최종 파일 생성 책임은 domain 규칙 15–17/20–21/46을 따른다. FE의 화면 갱신 결과만으로 저장/최종 XLSM 계산 검증을 완료한 것으로 보지 않는다. 제출값과 파생값의 write 표현은 별도 write 계약에서 확정한다.

### 3.2 단일 표 값의 경계

- 표 본문만 보내 헤더를 잃지 않는다. 해당 표 표시를 위해 필요한 다중 헤더·기간 라벨은 기존 FE와 같이 `rows`에 원본 행 번호로 포함하며 별도 `headers` 배열을 만들지 않는다. 빈 입력 셀도 포함한다.
- 원본 행/열 위치를 유지해 표 좌표·편집 위치와 대응한다. 선택 표의 값은 수정 가능 값과 읽기 전용 값을 구분해 제공하며 수정 불가라는 이유만으로 표시에서 제외하지 않는다.
- `tableData.tableId`는 전체 구조의 `tables[].id`와 일치해야 한다. tableData는 같은 source/template 기준으로 읽은 값이어야 한다.
- 행의 셀 필드는 `columns[].field`와 대응하고 선택 표에 필요한 열만 담는다. 문자열 또는 숫자로 표시하며 형식화된 퍼센트·소수 자리·쉼표를 유지한다. 날짜·boolean은 표시 문자열로 제공하고 셀 객체/셀 값 null을 반환하지 않는다. 빈 셀 필드는 생략할 수 있으나 편집 위치와 필요한 빈 열은 보존한다. 원본 수식 노출은 Open Questions의 별도 결정으로 남긴다.
- 다른 표의 grid 값은 tableData에 암묵적으로 섞지 않는다. §3.1B의 부속 control은 `controlIds/sourceTableId`로 연결과 소유권을 명시한 예외이며, 임의 참조 표 데이터를 동봉하는 통로로 쓰지 않는다.
- 전체 구조 반환과 table 값 반환은 값을 재계산하거나 Draft/Case/Run을 생성하는 동작이 아니다. 저장값 표시와 변경 후 재계산은 구분한다.

공개 식별자는 source case와 서버가 발급한 snapshot 및 안정적인 table 식별자를 기준으로 정하며 저장소 식별자를 노출하지 않는다. snapshot의 발급/수명과 구체 request 표현은 Open Questions에 남긴다.

#### 저장 Case 숫자 표시 정책 — 2026-10-15 사용자 확정

이 절은 저장 Case 입력 조회의 숫자 표시 정책 정본이다. [조회 상세 §5.3](mi-fundamental-model-input-query-api-proposal.md#53-수식-저장값-판독-기준--사용자-확정)의 확정된 존재·타입 판독 이후 적용하며 그 판독/실패 계약을 바꾸지 않는다. 지원 범위는 승인된 양식 version별 표시 catalog에 필요한 숫자·서식으로 한정한다. 정책 채택은 실제 catalog 대응, 제품 구현 또는 HTTP/FE 검증 완료를 뜻하지 않는다.

- **회계 폭 독립 표시:** 관측한 회계 서식의 배치 공백만 제외한다. 숫자·부호·쉼표·소수 2자리와 0의 `-` 표시는 유지하고 정렬은 FE의 화면 배치로 처리한다. 적용 대상은 `_-* #,##0.00_-;-* #,##0.00_-;_-* "-"??_-;_-@_-` 서식 계열이며, 같은 코드의 custom numFmtId 및 `\-`/`-` 동등 표현을 포함한다. 내용 서식 `#,##0.00;-#,##0.00;"-";@`에 해당하는 표시를 제공하며 P22 등 셀 좌표를 판정 조건으로 쓰지 않는다. 일반 문자열·다른 서식에 trim이나 공백 제거를 확대하지 않는다.
- **General 기준:** US 표기를 고정한다. 소수점은 `.`, 지수는 대문자 `E`와 부호·최소 2자리 지수로 표현하며 요청/브라우저의 지역 설정으로 재서식하지 않는다. 열 폭·폰트에 따른 축약은 제외하고 같은 저장 숫자·서식에는 같은 표시 문자열을 제공한다. 저장 숫자를 변경하지 않고 표시용으로만 반올림한다.
- **General 소수/지수·정밀도:** 검증 후보의 Excel 호환 십진수 변환(`NumberToTextConverter.toText`와 동등한 변환)을 기준으로 0은 `0`으로 표시한다. 0이 아닌 수는 절댓값이 `100000000000` 미만이고, `0.0001` 이상이거나 불필요한 끝의 0을 제외한 소수 자릿수가 9 이하이면 소수 표기 후보로 처리한다. 정수부 자릿수를 최소 1로 세어 소수 자릿수를 `max(0, 10 - 정수부 자릿수)`로 반올림하고 끝의 불필요한 0을 제거한다. 11자리 정수는 유지하며, 반올림 결과의 절댓값이 위 상한에 도달하면 지수 표기로 전환한다. 나머지는 유효숫자 6자리 지수 표기와 불필요한 끝의 0 제거를 적용한다. 반올림은 HALF_UP(정확히 중간이면 절댓값이 커지는 방향)이다. 모든 숫자에 소수 9자리를 일괄 적용하지 않는다.
- **응답/FE 경계:** 형식화한 grid 값은 기존 `columnN: string | number` 중 string으로 보존하고 FE는 표시·복사 시 다시 숫자로 변환하거나 재서식/trim하지 않는다. `controls.value/options`의 저장값 타입·의미, 원본 좌표·편집 위치·grid/control 소유권은 §3.1~3.2를 유지한다. 문자열·날짜·다른 명시 서식은 기존 지원 동작을 유지하며 새 셀 객체/응답 필드·DB 구조를 추가하지 않는다.

[Q3 독립 검증 기록](../work-notes/2026/2026-10-15-model-input-query-q3-stored-value-verification.md)과 [표시 후보·지원 한계](../../temp/model-input-query-q3/2026-10-15-stored-values-g01xpf_b/display-refinement/README.md)는 이 정책의 근거이며 제품 실행 의존이 아니다. 모든 숫자·지역 설정·Excel 버전이나 원래 화면의 공백/픽셀 일치를 보장하지 않는다. 승인 표시 범위와 후보의 타입/서식/숫자 범위를 대조하고 부족한 검증은 실제 필요한 항목만 추가한다. 미확인 범위를 지원 완료로 간주하거나 표시 문자열을 임의로 잘라 맞추지 않는다.

### 3.3 POST /input-uploads — kind별 저장 양식으로 파싱

2026-10-01 사용자 결정: 업로드 파일의 `kind`에 대응하는 미리 저장된 포맷으로 파싱하고 입력값 조회와 같은 응답 표현을 사용한다. 포맷은 §2의 백엔드 양식 카탈로그를 뜻하며 시트·논리 입력 영역·헤더·셀 좌표·control binding·수정/행열 정책을 template version별로 정의한다. 파일마다 화면 포맷을 새로 추론하지 않는다.

**Operation 초안:** `POST /input-uploads`, `multipart/form-data`. 전체 prefix는 Open Questions에서 확정한다. 한 요청에 workbook 하나를 받으며 네 파일이 모두 모일 때까지 기다리지 않는다. 기존 4파일 Case 생성 endpoint와 별개이고 이 호출로 Draft/Case/Run을 생성하거나 모델/수식 재계산을 수행하지 않는다.

| multipart 필드 | 초안 필수 여부 | 의미 |
| --- | --- | --- |
| `sourceKey` | 필수 | FE 출처 목록의 EXCEL_UPLOAD 키. 표시명/원본 파일명으로 대신하지 않는다. 실제 키 값과 업로드 묶음 식별/수명은 별도 계약이다. |
| `kind` | 필수 | `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 중 하나. 해당 파일을 적용할 역할이며 OUTPUT도 실행 입력 파일이다. |
| `file` | 필수 | 선택한 workbook 하나. 원본 저장 경로나 이미 저장된 fileNo를 받는 필드가 아니다. |

표 선택 request와 최초 선택 정책은 Open Questions에서 정한다. `sourceKey/kind/file` 및 업로드 kind 하나만 반환하는 아래 범위는 FE 제안을 재사용한 Draft 기준이고, FE의 whole-sheet 데이터 반환 제안은 채택하지 않는다.

**포맷 선택과 검증:**

1. 인증/허용 권한, 출처 유형, kind와 파일 기본 조건을 검증한다. 저장 case 조회의 `caseId + SCNRCASE`·4파일 완전성 조건을 단일 업로드에 적용하지 않는다.
2. 서버가 해당 모델에 적용할 등록 양식 버전을 결정하고 처리 시작 시 고정한다. kind는 그 버전 안의 파일 역할 정의를 선택한다. kind만으로 모든 양식 버전이 구분된다고 가정하거나 처리 중 바뀐 ACTIVE 버전을 다시 읽지 않는다. 적용 버전이 없거나 선택할 수 없으면 실패한다.
3. 실제 workbook의 필수 sheet/헤더/표 경계와 역할 구조를 선택한 양식 정의에 맞춰 검증한다. kind 또는 파일명의 주장만 믿지 않는다. 값 차이와 catalog가 허용한 행·열 확장은 허용하며 파일 전체가 기준본과 byte/hash까지 같을 것을 요구하지 않는다. 구체적 확장 판정은 표별 확정 정책을 따른다.
4. 구조가 맞지 않거나 알려지지 않은 양식이면 실패하고 다른 kind/버전/색 기반 포맷으로 자동 fallback하지 않는다. 업로드 시 발견한 새 시트·표·편집 범위가 catalog 정의를 자동 갱신하지 않는다.
5. 해당 kind의 전체 표시 구조를 정의에서 구성하고 선택 영역의 헤더·grid·부속 control 값을 업로드 원본에서 읽는다. 표 좌표/원본 번호/표시값·빈 편집 위치와 제외 정책은 §3.1~§3.2를 재사용한다. 저장값은 사용자 파일에서 읽고 기준본 값으로 조용히 대체하지 않는다.
6. 파싱/검증 완료 후 200 성공 응답을 반환한다. 잘못된 workbook이나 파싱 실패를 빈 성공으로 반환하지 않으며 내부 파일 경로는 노출하지 않는다. 자원 제한/stream 종료는 §8 및 SECURITY.md를 따른다.

**조회와 공통 응답, operation별 반환 범위:**

| 항목 | 저장 case 입력값 조회 | 단일 파일 업로드 파싱 |
| --- | --- | --- |
| envelope/필드 표현 | BaseResponse.data 및 §3.1 | 동일 |
| `sourceKey` | 조회한 저장 출처 | 요청한 업로드 출처 |
| `sections` | 네 kind의 전체 표시 구조 | 업로드한 kind 하나의 전체 표시 구조; 다른 kind를 비우거나 덮어쓰는 응답 아님 |
| `sections[].inputId` | 해당 역할의 원본/metadata 기준 | 파싱한 사용자 파일과 고정 양식 버전을 구별하는 서버 식별자; 파일명으로 대체하지 않음 |
| `tableData` | 선택 영역 하나의 grid 및 부속 controls | 동일한 표현, 업로드 kind의 선택 영역 하나만; 전체 sheet/모든 표 값은 반환하지 않음 |

업로드 시 최초 선택 영역을 어떻게 정할지는 미정이다. 표 선택 전 null을 사용하기로 확정하면 이를 해당 operation schema에 명시하며 임의 첫 표 값이나 mock을 넣지 않는다. 표가 선택된 응답은 `tableData.tableId`와 control의 `sourceTableId`가 응답 구조 안의 해당 kind에 속하고 같은 업로드 inputId/양식 기준이어야 한다.

FE는 성공한 업로드의 해당 kind 구조/데이터만 교체한다. 다른 kind 상태를 초기화하거나 업로드 응답이 저장 case의 4파일 완성을 뜻한다고 해석하지 않는다. 후속 표 선택 시 같은 업로드 원본을 조회할 수 있어야 하며 매번 재업로드하거나 기준본을 조회하는 방식으로 대신하지 않는다. 이를 가능하게 하는 보존/접근/만료 계약은 Open Questions에서 확정한다. 이 파싱 응답은 영구 파일 등록이나 최종저장을 완료했다는 의미가 아니다.

## 4. Server-side Resolution and Validation

아래는 저장 case 조회의 검증이다. 단일 파일 업로드는 §3.3을 따른다.

1. `caseId`로 `del_yn = 'N'`인 active case를 조회한다.
2. 허용 `execType`은 `INDEPENDENT | MI_BASE`다. `SCENARIO`는 active resource hiding 규칙으로 `404 RSC_NOT_FOUND`를 반환한다.
3. `tcm_filel`에서 `del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = :caseId`인 파일을 조회한다.
4. 파일명의 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 키워드를 대소문자 무관으로 검사해 각 역할을 정확히 1개씩 선택한다.
5. active 파일이 4개가 아니거나 역할이 누락·중복되면 불완전한 input으로 처리한다.
6. DB metadata와 실제 object storage 객체가 모두 존재하는지 확인한다. 전체 화면 구조는 같은 source의 완전한 4파일 set을 가리켜야 한다.
7. 선택한 표가 해당 파일·시트·표 구조에 속하는지 검증하고 해당 표의 표시 데이터를 추출한다. 정상적으로 값이 없는 표와 원본을 읽지 못한 실패를 같은 빈 성공으로 반환하지 않는다.

## 5. Response and Access Boundary

- 응답은 `BaseResponse<T>`와 JSON을 사용한다.
- 구체적인 DTO는 §3의 전체 구조/단일 표 값 경계를 따르며 field fidelity와 operation 구성을 확정한 뒤 정의한다.
- 호출 권한 후보는 기존 목록과 같은 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`다.
- 현재 공유 조회 정책에서는 case 소유자 `usr_no`가 현재 사용자와 달라도 input 접근을 허용한다.
- 저장 출처에서는 서버가 case와 연결된 정확한 파일을 선택한다. 업로드 출처에서는 사용자가 전송한 파일을 처리하며 클라이언트가 서버 file/storage 식별자를 지정하지 않는다. 후속 업로드 inputId는 권한 검사 없이 다른 사용자의 원본을 선택하는 수단으로 사용하지 않는다.
- input request/response body는 대용량·업무 민감 데이터일 수 있으므로 HTTP 본문 로깅 대상에서 제외한다.
- **2026-10-15 사용자 확정 — JSON DRM 비적용:** QRY-01/02가 엑셀에서 추출해 웹 화면에 제공하는 구조·저장값 JSON에는 회사 DRM 정책을 적용하지 않는다. 이 적용 여부는 미결정/추가 승인 항목에서 제외한다.
- HTTPS·인증·role/condition 및 tenant/Case/file 접근 검사와 위 본문 로깅 제외는 유지한다. 원본 파일의 기존 암복호화 정책도 유지하며 JSON 비적용을 파일 DRM의 전역 해제로 확대하지 않는다.

## 6. Representative Errors — Draft

| 조건 | HTTP | `ResultCode` | 의미 |
|---|---:|---|---|
| `caseId < 1` 또는 형식 오류 | 400 | `REQ_INVALID_PARAMETER` | 잘못된 case 식별자 |
| case 없음, soft delete, `SCENARIO` case | 404 | `RSC_NOT_FOUND` | input 제공 대상이 아님 |
| input 4개 미만/초과, 역할 누락·중복 | 409 | `BIZ_RULE_VIOLATION` | input 정합성 오류 |
| DB metadata는 있지만 저장 객체를 읽을 수 없음 | 500 | `SYS_INTERNAL_ERROR` | 저장소와 metadata 불일치 |
| 인증되지 않음 | 401 | 공통 인증 오류 | 로그인 필요 |
| 허용되지 않은 role/condition | 403 | 공통 권한 오류 | 접근 불가 |

오류 계약은 endpoint와 전달 방식을 정할 때 함께 확정한다. 내부 저장 경로나 provider 오류 원문은 응답에 노출하지 않는다.

## 7. OpenAPI Documentation Plan

- 계약 확정 후 operation을 정의한다.
- description에는 `caseId` 기준, `DEMAND/SUPPLY/TRADE/OUTPUT` 4개, incomplete input 의미를 명시한다.
- JSON schema에는 네 파일의 구조 metadata와 선택 표 데이터의 구분, table 식별, 헤더/값 타입·nullable·빈 목록 의미와 version을 기록한다.
- 전체 구조의 각 table에는 실제 값이 없고 값 payload는 선택 영역의 grid 및 명시적으로 연결된 부속 control만 담는다는 제약을 example/description과 검증 기준에 명시한다.
- FE와 동일한 `sourceKey/sections/kind/inputId/sheetName/tables` 및 `tableData.columns/rows/inputCells/inputGuideRules`를 유지하고 새 `controlIds/controls` 계약을 기술한다. `sheets[].data`는 제공하지 않고 grid 헤더는 rows에 포함한다. 일반 표·표와 control·control 전용 영역의 예시를 구분하고 공통 `BaseResponse`까지 포함한 최종 schema로 FE 인터페이스를 생성한다.
- operation을 분리할 경우 구조 조회와 표 값 조회의 책임, paging/default/snapshot 의미를 구분한다.
- 대표 200/400/401/403/404/409/500 응답을 `@ApiResponses`로 구조화한다.
- 업로드는 multipart sourceKey/kind/file 및 해당 kind 하나의 sections·선택 영역 데이터 예시를 별도로 기술한다. 지원 형식/용량·구조 불일치/양식 부재/만료 오류와 전체 prefix는 Open Questions 확정 후 HTTP/ResultCode까지 맞춘다. FE의 임의 문자열 오류코드를 기존 ResultCode에 검토 없이 추가하지 않는다.

## 8. Data and Implementation Impact

- 기존 `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` 재사용을 우선한다.
- 서버가 선택한 원본은 streaming OOXML/SAX처럼 전체 workbook object model을 만들지 않는 방식으로 필요한 값만 읽는다.
  `INPUT-02`에서 Apache POI `XSSFWorkbook` 전체 로딩은 1 GiB heap으로 실패했으므로 조회 기본 경로로 사용하지 않는다.
- 구조 metadata와 실제 값 추출을 구분한다. 선택 영역의 헤더·본문 및 명시적으로 연결된 control 값/선택지를 읽기 위해 원본을 탐색할 수는 있지만 그 밖의 미선택 표 값은 DTO/응답에 포함하지 않는다.
- 이 응답 단위 결정 자체는 새 DB table/column이나 cache 추가를 요구하지 않는다. catalog persistence는 기존 domain/DDL 설계가 소유한다.
- workbook 크기, sheet/row/cell 수, 응답 크기에 상한을 두고 stream/workbook을 항상 닫는다.
- 구체적인 상한은 실제 input 표본을 측정한 뒤 확정한다.
- 업로드도 저장된 immutable 양식 정의와 같은 표시 추출 규칙을 사용한다. 이 공통 표현 결정만으로 별도 범용 파서 framework/cache/새 DB table을 요구하지 않는다. 사용자 업로드 처리 중 stream/temp 자원은 정상·실패 경로 모두 정리하며 반환 후 원본 보존 경계는 §3.3의 후속 조회 계약에 맞춰 확정한다.

## 9. Acceptance Criteria — Common Draft

- Given 수식 셀이 화면 표시 범위에 있을 때, then 셀 표시 데이터는 수식 문자열이 아닌 결과값이며 입력/구조 변경에 필요한 화면 갱신 규칙은 §3.1C의 별도 정보로 제공한다.
- Given LiqUtilAdjustment에 마지막 월 열 추가 규칙이 전달됐을 때, when FE가 마지막 열 뒤에 새 열을 추가하면, then 새 Period는 직전 마지막 Period + 1로 표시된다. 규칙의 구체 JSON 표현은 확정 후 검증한다.
- Given SUPPLY ProductionAdjustment의 해당 표를 조회/업로드 파싱할 때, then `L7:L200`의 헤더/값/편집 위치를 화면 payload에서 제외하고 다른 열의 원본 번호를 유지한다. 원본 파일에서 이 범위의 수식/값을 삭제하거나 대체하지 않는다.
- Given ProductionAdjustment 행 추가로 신규 L셀이 생성된 source를 조회할 때, then 그 셀도 화면에 노출하지 않는다. 신규 L셀은 domain 규칙 31에 따라 빈칸이며 기존 오류 수식의 복사/확장 예외를 적용하지 않는다.
- Given 허용된 업로드 출처와 kind/단일 파일일 때, when POST /input-uploads를 처리하면, then 고정한 양식 버전의 kind 정의로 검증·파싱하고 해당 kind 전체 구조와 선택 영역 데이터만 공통 응답 표현으로 반환한다. Case/Run/최종저장을 생성하지 않는다.
- Given workbook이 요청 kind/등록 양식과 맞지 않을 때, then 성공/빈 데이터/다른 포맷 fallback으로 처리하지 않는다. 허용된 값 차이와 행·열 확장을 무조건 구조 불일치로 처리하지 않는다.
- Given 업로드한 파일을 다른 표로 전환할 때, then 동일 업로드 inputId/양식 기준의 값과 control을 조회하며 기준본 데이터로 대체하지 않는다. 구체 후속 request/수명은 구현 전에 확정한다.
- Given control 없는 일반 표를 선택할 때, then 기존 grid 표현과 `controls: []`를 반환한다.
- Given 표 위에 연결된 select control이 있을 때, then 해당 grid와 control 현재 값/선택지/binding을 반환하며 구조의 controlIds 및 sourceTableId와 일치한다. 연결되지 않은 control과 다른 표의 grid 값은 포함하지 않는다.
- Given control 전용 영역을 선택할 때, then grid 배열들은 `[]`이고 controls는 제공하며 가짜 표 행을 만들지 않는다.
- Given 여러 입력 영역이 동일 모드를 공유할 때, then 동일 control ID/소유 영역/binding을 사용한다. 동일 source에서 저장된 값은 일치하며 원본 셀 값을 rows와 controls에 중복하지 않는다.
- Given control 값이 비었거나 선택지에 없는 기존 값일 때, then null 또는 기존 값을 그대로 반환하며 첫 선택지로 자동 대체하지 않는다. editable은 catalog 정책을 따른다.
- Given active `INDEPENDENT | MI_BASE` case와 완전한 input 4개가 있을 때, when 허용된 사용자가 구조를 조회하면, then 같은 source의 네 파일 전체 표시 구조를 반환하며 각 표의 실제 값은 포함하지 않는다.
- Given 표시 구조 안의 입력 영역 하나를 선택했을 때, when 값을 조회하면, then 해당 grid와 명시적으로 연결된 부속 control만 반환하며 다른 표의 grid나 다른 시트/파일의 값은 포함하지 않는다.
- Given 선택 표에 읽기 전용 값과 수정 가능 값이 함께 있을 때, then 둘 다 표 표시 범위에 포함할 수 있으며 수정 가능 정보를 별도로 구분한다.
- Given 선택 표가 다중 헤더 또는 빈 편집 셀을 가질 때, then 표 본문 밖의 필요한 헤더·라벨과 빈 편집 위치를 유지한다.
- Given 전체 구조와 표 값이 함께 반환될 때, then `sourceKey/sections`는 기존 FE 필드 이름을 유지하고 `sheets[].data`는 없으며 `tableData.tableId`가 구조 안의 유일한 표 ID와 일치한다.
- Given 선택 표의 열이 BB부터 시작하고 헤더가 3행일 때, then `column53` 등 원본 열 번호와 `rowNumber=3/id=2`를 유지하며 A/1부터 재번호화하지 않는다. 헤더는 rows에 포함한다.
- Given 수정/안내 항목이 없을 때, then `inputCells/inputGuideRules`는 `[]`이며 셀 색/개발 안내 sheet만으로 입력 권한을 추정하지 않는다.
- Given 역할이 누락·중복된 case일 때, then 불완전한 세트를 정상 결과로 반환하지 않는다.
- Given `SCENARIO` 또는 soft-deleted case일 때, then `404 RSC_NOT_FOUND`를 반환한다.
- Given 기존 3파일 case가 cutover cleanup으로 soft delete됐을 때, then 목록에 노출되지 않고 input 조회도 `404 RSC_NOT_FOUND`를 반환한다.
- Given 운영 template을 등록할 때, then `SUMMARY`와 `플랫폼 입력 안내`는 runtime sheet 목록과 구조 hash에 포함되지 않는다.
- Given 네 역할과 승인 업무 구조가 호환되는 파일에 `SUMMARY` 또는 `플랫폼 입력 안내`가 남아 있을 때, then 두 시트의 존재나 기준 파일과 다른 내용 hash만으로 Case 연결을 거부하지 않고 조회 응답에서는 두 시트를 제외한다. 다른 접근·파일 유효성·판독 조건은 유지한다(domain 규칙39~40).
- Given 운영 source snapshot에 hidden sheet가 있을 때, when 조회 projection을 만들면, then hidden sheet의 table/cell은 응답과 계산 대상에 포함하지 않는다.
- Given Draft를 최종저장할 때, then source와 결과의 hidden sheet 집합·순서·이름·숨김 상태가 같고 hidden worksheet part는 원본 그대로 보존된다.
- Given read-only 표시 sheet인 SUPPLY `LNG Summary` 1개를 조회할 때, then source snapshot 값은 반환하지만 수정 가능 field는 없고 Draft·Backend 재계산 대상에도 포함되지 않는다. TRADE `backupLNG_TradeRestrictionInput`은 화면 구조·선택 표 값 응답에 표시하지 않고 최종 XLSM에는 원본 보존한다(domain 규칙 43·57).
- Given 구분 탭 4개가 source snapshot에 있을 때, when 조회 projection을 만들면, then 응답에 포함하지 않고 최종저장 결과에는 원본 그대로 보존한다.
- Given TRADE `4.5 PriceModelInput`의 T-183 또는 T-184를 조회할 때, then domain 규칙 44의 입력 셀은 수정 가능 대상으로 제공하고 고정 제목·헤더는 조회만 제공한다. 규칙 57의 미표시 T-177~T-182와 U열은 응답에서 제외한다.
- Given 해당 Trade Draft에 승인 입력 편집·아래 행 추가가 있을 때, when 최종저장하면, then domain 규칙 44에 따라 해당 표의 변경을 반영하고 열/기간은 고정한다. 다른 표의 범위·값을 함께 변경하지 않으며 T-177~T-182 및 승인 변경 밖 원본 영역은 보존한다. 상세 write 계약·실파일 검증 완료를 이 수용 기준 정의로 대신하지 않는다.
- 공개 요청이 `fileNo`나 object key를 받지 않으므로 다른 case의 파일을 선택할 수 없다.
- 구조화 데이터 응답에는 원본 Excel binary·Base64·object storage 경로를 포함하지 않는다.
- 전달 방식별 status, 부분 실패와 재시도 acceptance criteria는 계약 확정 후 추가한다. QRY-01/02의 JSON DRM 비적용은 §5의 확정 사항을 따른다.

## 10. Scope Boundary

### Included

- `caseId` 요청 식별 경계
- `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할 검증
- 네 파일 전체 화면 구조와 선택 표 하나의 값이라는 응답 범위
- kind별 등록 양식으로 단일 업로드 파일 검증·파싱 및 조회와 공통 응답 표현
- 응답 크기·snapshot·부분 실패 결정과 §5의 JSON DRM 비적용 경계
- 확정 후 endpoint와 request/response/error/OpenAPI 계약

### Excluded

- MI Base 목록·상세·지정 API
- 원본 input 파일의 ZIP·역할별 다운로드 API
- `임시저장` Draft와 `최종저장`으로 XLSM 4개 및 새 독립실행 case를 만드는 write API
- output/결과 파일 다운로드
- 범용 Excel-to-JSON 변환 API

## 11. Implementation Readiness

- status: `blocked by open questions`
- implementation: endpoint/표 선택·최초 선택, response fidelity, 단일 표 크기/paging·snapshot 및 업로드 보존/접근/후속 조회 계약 확정 전에는 착수하지 않는다. QRY-01/02의 JSON DRM 비적용은 §5에서 확정됐으며 기존 접근/로그 보안의 제품 적용·검증은 남는다.
- next decision: `INPUT-03`의 표시 catalog/표 경계/row identity와 field fidelity를 확정하고 §3의 응답 범위에 맞춰 endpoint/DTO/error/OpenAPI 계약을 갱신한다. 이미 사용자 확정된 조회 상세 계약은 재결정하지 않고 정본 통합 대상으로 구분한다.

## 12. Change Log

- 2026-10-16: 기존 QRY 확정 범위의 소유 문서와 새 공통 편집 API Draft를 연결했다. 기존 조회/업로드 endpoint·필드·오류는 변경하지 않았으며 최종저장 상세/제품 구현·INPUT-03/04 전체 완료로 승격하지 않았다.

- 2026-10-15: 사용자 확인으로 QRY-01/02의 엑셀 추출 표시 JSON에 회사 DRM을 적용하지 않음을 §5에 확정했다. 해당 미결정/승인 선행조건을 제거했으며 HTTPS·인증/권한·본문 로깅 제외와 기존 파일 암복호화는 유지한다. 제품 보안 검증 완료나 티켓 상태 변경을 뜻하지 않는다.
- 2026-10-15: 사용자 승인으로 저장 Case 숫자 표시 정책을 §3.2에 채택했다. 회계 배치 공백만 제외하고 General은 US 기준·열 폭 축약 제외·승인 표시 범위 한정으로 제공한다. 표시 규칙은 §3.2가 소유하며 기존 판독/실패 계약·응답 구조·제품 코드·INPUT 상태는 변경하지 않는다.
- 2026-10-13: domain 규칙 36에 3파일 `SCENARIO` 정리가 추가돼, §2의 "case master만 변경" 문구가 `INDEPENDENT | MI_BASE` 정리에만 해당함을 명시했다. 이 문서의 조회 계약은 바꾸지 않았다.
- 2026-10-13: 미커밋 문서 정합성 검토에서 아래 세 변경의 Change Log 누락을 보완했다. 본문 계약은 바꾸지 않았다.
- 2026-10-08: domain 규칙 44에 맞춰 TRADE `4.5 PriceModelInput`의 시트 전체 read-only·최종 XLSM 미변경 계약을 T-183/T-184 입력 편집·아래 행 추가 제공으로 바꿨다(§2, §9). 미표시 T-177~T-182와 U열의 화면 제외는 유지한다.
- 2026-10-08: 사용자 확인대로 read-only 표시 수용 기준을 SUPPLY `LNG Summary` 1개로 정렬했다. TRADE `backupLNG_TradeRestrictionInput`은 미표시·원본 보존하며 기존 조회 계약의 필드/오류는 변경하지 않는다.
- 2026-10-07: domain 규칙 57에 따라 최신 SUMMARY에서 표 전체가 미표시로 확정된 표를 조회 응답에서 제외했다(§2). 표 일부 열의 미표시와는 구분한다.
- 2026-10-07: 사용자 확인으로 LiqUtilAdjustment 내부 참조 수식 범위를 `G7:CL7`에서 `H7:CL7`로 정정했다. `G7`은 시작 연도 값이며 고정 헤더 정책을 유지한다(§3.1C).
- 2026-10-07: 판별대장 T-020과 domain 규칙 50에 맞춰 LNG Demand 모드 open question을 정리했다. 값 셀 C1·선택값 변경 허용·T-021과 함께 표시는 확인된 사실로 두고, 원본 선택지 `Dynamaic` 오타 처리와 options 표기만 미결정으로 남겼다.
- 2026-10-07: 이번 release에 구조 변경을 포함하는 domain 결정에 조회 계약을 맞췄다. 마지막 행·열 추가 중심 원칙과 기존 중간 삽입·삭제 예외를 참조하며, 구체 write 계약은 미확정 상태로 유지한다.
- 2026-10-06: 사용자 전달 FE 합의로 수식 결과값과 별도 화면 갱신 규칙 전달을 분리했다. 입력/열 추가 시 FE가 규칙으로 표시값을 갱신하고, 기존 BE write/파일 생성 책임은 유지한다. 규칙 JSON 표현과 G7:CL7의 실제 계산식은 미결정으로 남겼다.
- 2026-10-06 최종: 사용자 승인으로 기존 ProductionAdjustment L8:L200 수식 보존·신규 행 L셀 빈칸·화면/재계산 제외 유지로 확정했다. 앞선 신규 L수식 복사/행 참조 변경 및 오류 확장 예외를 대체했다.
- 2026-10-06 중간 결정(위 최종 결정으로 대체): ProductionAdjustment를 C 구조변경으로 명시하고 필요한 신규 L수식의 행 참조 변경 복사와 오류 보존 예외를 허용했었다.
- 2026-10-06: 사용자 확인에 따라 ProductionAdjustment 행 추가 가능과 L7:L200 화면 제외를 명시했다. 헤더 L7과 기존 오류 수식 L8:L200을 구분하고 원본 보존/재계산 제외 규칙은 domain을 참조한다.
- 2026-10-01: 사용자 결정으로 POST /input-uploads의 kind별 저장 포맷 파싱과 조회 공통 응답 표현을 추가했다. FE의 단일 파일 multipart/해당 kind section 제안은 Draft 기준으로 재사용하고 whole-sheet 값 반환은 제외했다. 양식 버전 고정·구조 검증과 업로드/저장 case 경계를 구분했으며 보존/수명·후속 조회/최초 선택·오류 상세는 미정이다.
- 2026-10-01: 사용자 지적의 LNG Demand 모드 선택 예외를 반영했다. table을 선택 가능한 논리 입력 영역으로 정의하고 `tableData.controls`와 구조의 `controlIds`를 추가했다. 기존 ID/grid 표현을 유지하며 공유 control의 명시적 연결·원본 binding·값 중복 방지와 control 전용 영역을 규정했다. 실제 LNG 값 셀/선택지/공유 대상은 미정이다.
- 2026-10-01: 사용자 승인에 따라 기존 FE의 `sourceKey`, `sections → sheets → tables`, `columns/rows/inputCells/inputGuideRules` 필드 이름과 원본 행/열 기반 표현을 재사용한다. `sheets[].data`를 제거하고 선택 표의 값을 `tableData`에 배치했다. 별도 `headers/editingInfo`를 추가하지 않고 표 ID·source 기준의 대응 관계와 예시/수용 기준을 정리했다.
- 2026-10-01: 사용자 결정에 따라 화면 `파일 → 시트 → 테이블` 구조 정보는 네 파일 전체, 실제 헤더·값은 선택 표 하나만 제공하도록 응답 범위를 확정했다. 전체 snapshot/role/sheet 값 일괄 반환 후보를 제거하고 API 호출 구성은 미정으로 유지했다. 표 단위 제공과 일부 조회 field 선별/수정 허용 범위를 혼동하지 않도록 문구를 정리했다.
- 2026-09-10: MI Base 지정 계약과 분리해 독립 Draft를 생성했다. 기존 문서의 고정 request identity, 전달 후보, 보안 경계, 미결정 사항을 이동했다.
- 2026-09-14: 2026-09-08 회의의 table projection 방향에 맞춰 원본 파일 응답 제외를 고정하고 ZIP·역할별 다운로드 fallback 후보를 제거했다. BE가 제출 변경분을 원본 XLSM 복제본에 반영하는 내부 생성 흐름은 유지한다.
- 2026-09-16: 화면 계약을 sheet/table projection으로 고정하고, 카탈로그의 편집·구조 변경·선택 후보·영향 관계 소유권을 명시했다. 조회 계약과 `임시저장`/`최종저장` write 계약을 분리했다.
- 2026-09-16: `INPUT-01`의 화면 후보/승인 경계를 반영하고, `INPUT-02`의 POI 전체 로딩 `NO-GO`에 따라 조회 구현 후보를 streaming 방식으로 제한했다.
- 2026-09-28: 사용자 확인에 따라 `OUTPUT`을 기존 세 역할과 같은 필수 input으로 추가하고 4파일 snapshot·완전성 계약으로 갱신했다. 이 시점에는 기존 3파일 case 호환 정책을 open question으로 남겼다.
- 2026-09-28: 후속 사용자 결정에 따라 기존 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT` 보완 없이 cutover 전에 soft delete하고 화면·active API에서 제외한다. 연결 파일·실행이력·결과는 물리 삭제하지 않는다.
- 2026-09-28: 당시 개발 시트를 runtime catalog·응답·Draft·최종 XLSM·Job input에서 제외하고 정규화본을 운영 기준으로 사용했다. 파일 수용·원본 보존 조건은 아래 2026-10-19 결정으로 대체됐다.
- 2026-10-19: 사용자 승인에 따라 domain 규칙39~40의 개발 시트 포함 파일 수용을 반영했다. 조회 응답 제외는 유지하며 파일 수용·보존의 정본은 해당 domain 규칙이다.
- 2026-09-28: 사용자 확인에 따라 운영 hidden sheet는 다른 시트 계산과 무관한 원본 보존 영역으로 확정했다. 조회·Draft·dependency·Backend 계산에서는 제외하고 최종 XLSM에는 source snapshot 그대로 복사한다.
- 2026-09-28: `직접 입력 대상 아님.`인 visible sheet 중 수식·직접 참조가 없는 6개를 read-only 표시 대상으로 확정했다. source 값은 조회하되 write·Draft·dependency·Backend 재계산에서 제외하고 최종 XLSM에는 보존한다.
- 2026-10-01: 사용자 확인에 따라 위 6개 중 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`, SUPPLY `Input Upstream ->`, `Input Midstream ->`)를 조회 응답에서 제외했다. read-only 표시는 SUPPLY `LNG Summary`, TRADE `backupLNG_TradeRestrictionInput` 2개로 줄었고, 6개 모두 계산 제외·최종 XLSM 원본 보존은 그대로다.
