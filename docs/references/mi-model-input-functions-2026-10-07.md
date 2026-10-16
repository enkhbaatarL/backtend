# 최신 모델 입력 4파일 함수·POI 5.4.0 지원 조사

## Document Control

- status: Complete
- canonical for: Reference only — 고정 v8/v6의 함수 관측 및 POI 기본 등록 확인 근거
- owner: BE
- source documents:
  - `mi-model-input-baseline-2026-10-07.md` 및 manifest
  - `mi-model-input-functions-2026-10-07.json`
  - `../../scripts/scan-model-input-functions.py`
  - `../../build.gradle` — POI/poi-ooxml 5.4.0
  - `mi-model-input-calculation-engine-spike.md` 부록 A — 별도 교차 조사·실제 함수 호출 시험·배열 범위/표 대응
- depends on:
  - `../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` — 업무 정책 소유
  - `mi-model-input-sheet-calculation-compatibility.md` — 표별 검증 계획 소유
- last synced: 2026-10-08
- supersedes: 최신 함수 관측으로 이전 스캔의 본문 수식만 센 함수 횟수와 GDP/PIPELINE UDF 후보 해석을 대체. 과거 실행 기록은 보존한다.
- open questions: 표별 계산 허용/보존 매핑, 실제 함수 조합·변경 입력·구조 변경 정답, 전체 XLSM 로딩·보존·운영 경계는 후속 검증 대상이다.

## 결과와 해석

사용자가 고정한 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6 정규화본의 **4파일·70개 시트·1,863,766개 셀 수식**을 읽기 전용으로 조사했다. 전체 셀 수식에 있는 함수는 25종이며 조건부서식까지 포함하면 29종이다. POI 5.4.0의 실제 기본 등록 목록에 27종이 있고 `_XLFN.UNIQUE`, `_XLUDF.ISFORMULA` 2종은 없다. 파일 전체를 POI로 로딩하거나 수식을 실행·재저장하지 않았다.

지원 확인은 `Version.getVersion()` 및 `WorkbookEvaluator.getSupportedFunctionNames()/getNotSupportedFunctionNames()`를 실제 로컬 5.4.0 jar에서 호출한 결과다. 등록 목록은 지원 251개/미지원 161개이며 사용 함수 29개를 대조했다. jar별 SHA-256·등록 목록과 함수별 역할/시트/수식 위치/대표 좌표는 [JSON 근거](mi-model-input-functions-2026-10-07.json)에 있다. [공식 개발 가이드](https://poi.apache.org/components/spreadsheet/eval-devguide.html)의 목록 조회 방법을 사용하며, 웹에 실린 5.2.0 함수 개수를 5.4.0의 근거로 사용하지 않는다.

여기서 **지원 구현 등록**은 기본 evaluator가 해당 함수 구현을 보유한다는 뜻이다. 실제 인수·배열 형태·함수 조합·Excel 정답 일치·XLSM 보존·전체 메모리 적합성의 통과 판정은 아니다. prefix 함수는 원문 이름과 prefix 제거 후 이름을 모두 대조했다. 위 두 함수는 원문과 `UNIQUE`/`ISFORMULA` 모두 지원 목록에 없다. 별도 Java 구현/UDF 등록은 하지 않았다.

이 문서와 JSON은 전체 함수 집계·기본 등록·입력 hash의 주 근거다. 별도 [spike 부록 A](mi-model-input-calculation-engine-spike.md)는 실제 함수 호출 시험과 배열 범위/판별대장 대응을 보유한다. 그 시험에서 `UNIQUE`·`ISFORMULA`는 원문/대문자/prefix 제거 표기 각각 31개 인수 형태 모두 `NotImplementedFunctionException`이었다. 기본 등록 27종 중 22종은 값을, 5종은 오류값만 반환했으므로 실제 업무 계산 정확도 검증과 구분한다. 이 JSON의 `formulaExecutionVerified=false`는 원래 스캔에서 함수를 실행하지 않았다는 기록이며 별도 시험 결과는 spike를 참조한다.

## 역할별 조사 범위

| 역할 | 업무 시트 | 셀 수식 | shared follower 포함 수 | 셀 수식의 함수 종류 |
| --- | ---: | ---: | ---: | ---: |
| DEMAND | 25 | 699,298 | 558,116 | 11 |
| OUTPUT | 5 | 889 | 858 | 0 |
| SUPPLY | 23 | 460,803 | 270,180 | 13 |
| TRADE | 17 | 702,776 | 590,471 | 19 |

shared follower는 XML에 수식 본문이 생략된 공통 수식 셀이다. master 수식의 함수명을 따라 사용 셀·호출 수에 포함했다. 함수명 집계에는 좌표 이동이 영향을 주지 않으므로 수식 전체를 재작성하지 않았다. master가 뒤에 있거나 없을 때를 구분하며 master를 찾지 못하면 스캔을 실패시킨다.

셀 수식 외에는 조건부서식 37개, 데이터 유효성 14개, 이름 정의 94개, OOXML Table 계산열 수식 4개를 조사했다. 이름 정의의 업무 문자열은 기록하지 않고 식별 hash만 사용한다. workbook 밖 외부 Excel 원본이나 VBA 프로시저 코드를 실행·분석한 결과는 아니다. 셀 함수 호출로 드러나는 UDF와 확장 함수는 함수 목록에 포함한다.

OUTPUT의 889개 수식은 직접 참조·산술/연결 등 함수 호출이 없는 식이다. 함수가 없다는 사실은 재계산 불필요나 모델 영향 없음의 근거가 아니다. 전체 수식 중 함수 없는 식은 486,691셀이다.

모든 셀 수식에 XML `v` 요소는 있지만 459,967셀은 본문이 비어 있다. `v` 요소 존재를 유효한 숫자 계산값이나 실제 계산 성공으로 해석하지 않는다. 오류·빈 값의 기대 동작과 기존 오류 보존은 표별 정책/검증에서 구분한다.

## 함수 전체 목록

사용 셀 수는 같은 함수가 한 셀에 여러 번 등장해도 1로 세고, 호출 수는 등장 횟수를 센다. 여러 함수가 같은 셀에 함께 있으므로 아래 사용 셀 수를 합산해 전체 수식 셀 수로 사용하지 않는다. 그 밖 수식의 횟수는 규칙/정의에 있는 호출 수이며 적용 범위의 모든 셀에 곱하지 않는다.

| 함수 | 셀 수식 사용 셀 | 셀 내 호출 수 | 그 밖 수식 위치·호출 수 | POI 5.4.0 기본 등록 |
| --- | ---: | ---: | --- | --- |
| `AND` | 189,698 | 242,338 | 조건부서식 16 | 지원 구현 등록 |
| `AVERAGEIFS` | 304 | 336 | 없음 | 지원 구현 등록 |
| `CHOOSE` | 4 | 4 | 없음 | 지원 구현 등록 |
| `COUNTA` | 1 | 1 | 없음 | 지원 구현 등록 |
| `COUNTBLANK` | 745 | 745 | 없음 | 지원 구현 등록 |
| `COUNTIF` | 8 | 12 | 없음 | 지원 구현 등록 |
| `COUNTIFS` | 1,555 | 1,556 | 없음 | 지원 구현 등록 |
| `IF` | 641,737 | 1,026,052 | 없음 | 지원 구현 등록 |
| `IFERROR` | 330,329 | 366,577 | Table 계산열 2 | 지원 구현 등록 |
| `INDEX` | 570,412 | 901,904 | Table 계산열 6 | 지원 구현 등록 |
| `ISBLANK` | 118,471 | 118,511 | 없음 | 지원 구현 등록 |
| `ISERROR` | 0 | 0 | 조건부서식 2 | 지원 구현 등록 |
| `ISNUMBER` | 141,581 | 141,581 | 조건부서식 18 | 지원 구현 등록 |
| `LEFT` | 24,772 | 24,772 | 없음 | 지원 구현 등록 |
| `LEN` | 24,772 | 24,772 | 없음 | 지원 구현 등록 |
| `MATCH` | 570,929 | 1,040,414 | Table 계산열 6 | 지원 구현 등록 |
| `MAX` | 1,756 | 1,756 | 없음 | 지원 구현 등록 |
| `MIN` | 5,728 | 6,000 | 없음 | 지원 구현 등록 |
| `NOT` | 0 | 0 | 조건부서식 18 | 지원 구현 등록 |
| `OR` | 7,190 | 7,190 | 조건부서식 7 | 지원 구현 등록 |
| `ROW` | 0 | 0 | 조건부서식 5 | 지원 구현 등록 |
| `SEARCH` | 77,192 | 77,192 | 조건부서식 2 | 지원 구현 등록 |
| `SUM` | 37,982 | 37,982 | 없음 | 지원 구현 등록 |
| `SUMIF` | 163 | 171 | Table 계산열 3 | 지원 구현 등록 |
| `SUMIFS` | 347,355 | 356,939 | 없음 | 지원 구현 등록 |
| `TEXT` | 3,988 | 3,988 | 없음 | 지원 구현 등록 |
| `VLOOKUP` | 766 | 774 | 없음 | 지원 구현 등록 |
| `_XLFN.UNIQUE` | 5 | 5 | 없음 | **기본 등록 없음** |
| `_XLUDF.ISFORMULA` | 0 | 0 | 조건부서식 16 | **기본 등록 없음** |

## 미지원 위치와 설계에 넘길 판단

| 함수 | 실제 사용 위치 | 다음 판정 |
| --- | --- | --- |
| `_XLFN.UNIQUE` | TRADE `MidstreamContractInput!CK4/CQ4`, `backup_MidstreamContractInput!CK4/CQ4`, hidden `Industry research!A3` — 셀 수식 5곳 | 2026-10-08 사용자 확인 및 대장 T-137/T-138/T-187/T-188의 기존 A 정책에 따라 모두 원본 보존. 이 5셀 자체의 직접 재계산에 UNIQUE 구현을 요구하지 않는다. 하류 계산 경계는 별도이며 상세 범위는 아래 보존 정책을 따른다. |
| `_XLUDF.ISFORMULA` | SUPPLY 6개 시트와 TRADE 2개 시트의 조건부서식 총 16규칙. 상세 범위는 JSON | 일반 셀 값 계산 함수와 구분한다. 현재 원본 규칙 보존이 필요한지, 엔진에서 색상 조건까지 평가할 요구가 있는지 확인한다. 원본 보존만 하는 규칙을 계산 allowlist로 승격하지 않는다. |

`UNIQUE`는 [Excel 내장 함수](https://support.microsoft.com/en-us/excel/functions/unique-function)이며 `_xlfn.UNIQUE`는 [공식 저장 문법](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-xlsx/3d025add-118d-4413-9856-ab65712ec1b0)이다. POI 기본 미지원을 VBA 제공 주체 미확인으로 해석하거나 이 함수 출처 확인을 위해 VBA 도구 설치를 요구하지 않는다. VBA의 다른 동작은 별도 미검증이다.

이름 정의식 94개 및 이름 간 참조에는 함수 호출이 없지만 이름이 가리키는 일반 셀의 수식 의존성은 추적하지 않았다. 이름 경유 계산 영향은 미검증이다. Table 계산열 4개도 셀 밖 metadata에서 검출된 위치 정보이며, 실제 셀의 계산/보존 정책과 구조 변경 수식 복제 규칙에 연결해야 한다.

이전의 `GDP`·`PIPELINE`은 이번 최신 수식의 실제 함수 호출이 아니다. 따옴표 안 문자열·시트명·구조 참조 label을 제거하지 않은 정규식의 오탐을 제거했다. 최신 source에서 이 두 이름의 UDF 실행 환경을 확보하는 작업을 만들지 않는다. 실제 함수 호출이 새로 확인되면 그 근거로 재검토한다.

### UNIQUE 사용 위치의 현재 보존 정책 — 2026-10-08

사용자는 `MidstreamContractInput!CK4/CQ4`가 미표시 표의 헤더 용도라고 확인했다. [통합 판별대장](model-input/README.md)의 사람 확인은 T-137/T-138을 이미 **A 원본 복제·표기 안 함**으로 확정했으며 domain 규칙 57을 적용한다. 아래 나머지 3셀도 T-187의 기존 A와 T-188의 A/hidden 보존 결정에 대응한다. 미표시라는 이유만으로 다른 표를 계산 제외하는 일반 규칙을 새로 만든 것이 아니다.

양방향 조사 후 사용자 최종 결정(2026-10-08)으로 **5셀 모두 미표시·모델 미사용·A 원본 복제로 분류 확정**했다. 업무 정책은 domain 규칙 57의 「UNIQUE 적용 최종 확인」을 따른다. 모델 미사용은 사용자 확인이며, 아래 수식 연결 관측을 삭제하거나 모델 실행 검증 완료로 바꾸지 않는다.

원본 복제의 직접 근거는 고정 TRADE v8 원본 `SUMMARY` 구현 기준의 다음 문구다.

> 입력 화면에 표시하지 않음. 원본 시트/값 유지.

| 대장 항목 | UNIQUE 대상 셀 | 원본 SUMMARY 구현 기준 위치 |
| --- | --- | --- |
| T-137 LNG Contracts Pivot Table | MidstreamContractInput!CK4 | J17 |
| T-138 PNG Contracts Pivot Table | MidstreamContractInput!CQ4 | J18 |
| T-187 backup MidstreamContractInput Reference | backup_MidstreamContractInput!CK4/CQ4 | J67 |
| T-188 Industry research Reference | Industry research!A3 | J68 |

네 항목의 C열은 모두 `미표시`이며 J열은 위 문구와 동일하다. 정규화본에서는 개발 전용 SUMMARY를 제거했으므로 이 근거는 baseline manifest의 정규화 전 원본 snapshot에서 읽기 전용으로 확인했다. 이 문구는 화면 제외와 원본 유지의 구현 지시다. 모델 미사용은 별도의 사용자 확인이며, 수식 참조 없음에서 원본 복제를 추론한 것이 아니다.

| 시트 / 표 | UNIQUE 시작 셀·배열 범위 | 현재 처리 |
| --- | --- | --- |
| `MidstreamContractInput` / T-137 `LNG Contracts Pivot Table` | `CK4`, 배열 `CK4:CM10`; 표 관측 `CK3:CN10` | 원본 수식·배열 결과·cached value·관련 설정 보존, 재계산/새로고침 제외 |
| `MidstreamContractInput` / T-138 `PNG Contracts Pivot Table` | `CQ4`, 배열 `CQ4:CS29`; 표 관측 `CQ3:CT29` | 원본 수식·배열 결과·cached value·관련 설정 보존, 재계산/새로고침 제외 |
| `backup_MidstreamContractInput` / T-187 | `CK4` → `CK4:CM14`, `CQ4` → `CQ4:CS29` | 시트 전체 미표시·A 원본 복제(2026-10-08 사용자 재확인, domain 규칙 57 보완). 두 UNIQUE와 배열 결과를 포함해 시트 전체 편집·재계산 제외 |
| hidden `Industry research` / T-188 | `A3` | 시트 전체 미표시·A 원본 복제(2026-10-08 사용자 재확인, domain 규칙 57 보완). 숨김 상태·원래 `#REF!`와 오류 저장값도 보존, 시트 전체 편집·재계산 제외 |

시작 셀만 복사하거나 수식을 값으로 바꾸는 것이 아니라, 최종 workbook을 원본에서 복제해 해당 표·배열 결과를 그대로 유지한다. **현재 UNIQUE 5곳 자체는 모두 보존 경로이며, 이 5셀의 직접 재계산을 위해 UNIQUE 구현을 요구하지 않는다.** 같은 시트의 표시·편집 표까지 계산 제외로 확대하지 않는다. 최신 양방향 연결 근거는 다음 절을 따른다. 하류 수식의 계산 허용·보존 기준값 사용 조건은 domain §1A.4 Q5의 업무 결정과 구분하며, 이 원본 복제 결정만으로 전체 계산 엔진의 blocker 해소나 모델 영향 없음을 판정하지 않는다. 정책 변경으로 이 영역을 갱신하게 될 때 UNIQUE/배열 shape 지원을 재검토한다.

### UNIQUE 5셀의 상류·하류와 사용자 입력 연결 — 2026-10-08

상류는 계산에 쓰이는 입력을 거슬러 올라가는 방향, 하류는 결과를 사용하는 수식을 따라가는 방향이다. 고정 TRADE v8의 17시트·702,776개 수식에서 두 방향을 확인하고 통합 대장 snapshot `f0011ffc1161b5e12f858b884cdff71a1bb2dd4612c7c7b738de4c183d2ed99c`의 직접 입력 기록과 대조했다. T-133/T-139의 입력·표시·추가·C 분류는 최신 v8 사람 확인 기록을 따르며, 대장의 `정책 상태=미확정`은 상세 write/계산 정책이 남았다는 뜻이다. 아래 연결을 실제 계산 허용 목록으로 사용하지 않는다.

| UNIQUE 시작 셀 | 직접 읽는 범위 — 같은 시트 | 상류의 사용자 입력 연결 | 현재 배열 결과의 직접 / 전체 후행 수식 셀 | 후행의 정책 경계 |
| --- | --- | --- | --- | --- |
| MidstreamContractInput!CK4 | Q4:Q2000, W4:W2000, V4:V2000 | Q5:Q2000 직접 연결; V/W 수식을 거쳐 R5:R2000·T5:T2000 연결. T-139 입력 Q5:U2918의 일부 | 24 / 23,438 | T-137·T-135 A를 거쳐 T-139의 AG/AI까지 연결 |
| MidstreamContractInput!CQ4 | A4:A2000, F4:F2000, E4:E2000 | A5:A1104 직접 연결; E/F 수식을 거쳐 B5:B1104·C5:C1104 연결. T-133 입력 A5:D1104의 일부 | 100 / 18,264 | T-138·T-136 A 안에 머묾 |
| backup_MidstreamContractInput!CK4 | Q4:Q2000, W4:W2000, V4:V2000 | 자기 시트의 R/T에도 연결되지만, 전체 시트 T-187 A여서 사용자 편집 영역 연결 없음 | 40 / 17,038 | 전부 같은 backup 시트 T-187 A |
| backup_MidstreamContractInput!CQ4 | A4:A2000, F4:F2000, E4:E2000 | 자기 시트의 B/C에도 연결되지만, 전체 시트 T-187 A여서 사용자 편집 영역 연결 없음 | 100 / 18,264 | 전부 같은 backup 시트 T-187 A |
| Industry research!A3 | 원래 인수 #REF! | 유효한 선행 셀을 해석할 수 없음. 전체 시트 T-188 A이며 복원 가능한 사용자 입력 연결은 관측되지 않음 | 0 / 0 | 숨김 시트 전체 원본 보존 |

개수는 참조 토큰 수가 아닌 수식 셀 수다. 현재 배열은 CK4:CM10 / CQ4:CS29 / backup CK4:CM14 / backup CQ4:CS29 / Industry A3다. 시작 셀 5곳 자체를 직접 읽는 수식은 없고, 위 직접 후행은 펼쳐진 결과 셀을 읽는다. 모든 후행은 같은 원본 시트 안에 있으며 현재 TRADE의 다른 시트로 이어지는 경로는 없다.

**LNG 경로는 편집 표로 돌아온다.** 대표 경로는 `CK5 → AT5 → BI5 → AG5 → AI5`다. AG5는 T-135의 BA5:BA2000·BI5:BI2000을 조회하면서 새 입력 Q5/R5/T5도 읽는다. AG/AI를 별도로 거슬러 올라가 A에서 멈추면 입력 Q5:U2918 전체와 연결되며, A에서 읽는 조회 범위는 BA5:BA2000·BI5:BI2000이다. 현재 배열에서 연결되는 T-139의 수식은 AG5:AG4707·AI5:AI4707 합계 9,406셀이고 직접 입력 칸은 아니다. 전체가 실제 값 변경/재계산 대상이라는 뜻은 아니다.

**보존 경계와 수식 연결은 별개다.** 현재 배열의 첫 후행 24/100/40/100셀은 모두 A 원본 복제 영역에 있다. 사용자 최종 확인에 따라 A에서 갱신 전파를 멈추며, 5셀 자체의 원본 복제 분류 질문은 종료했다. AG/AI는 자기 입력 때문에 계산 후보가 될 수 있으며, 이때 보존된 BA/BI를 기준값으로 읽는 처리의 승인·새 계약 키의 조회 실패/기존 오류 처리·모델 영향은 5셀 밖의 domain Q5 검토에 연결한다. 입력 연결이 확인됐다는 이유로 UNIQUE나 T-135/T-136의 기존 보존 결정을 변경하지 않는다.

현재 배열 아래에 미리 채운 수식까지 포함하면 LNG/PNG별 후행은 본 시트 41,338/41,916셀, backup 34,898/41,916셀이다. 이를 합친 본 시트 83,254·backup 76,814는 [기존 후행 조사](../work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md)와 일치한다. 이 넓은 집합은 현재 배열의 직접 참조 수나 재계산 승인 수가 아니다.

추가 정책·검증에 넘길 사실:

- 본 시트 두 UNIQUE는 사용자 입력과 연결돼 있으며 사용자 최종 결정에 따라 원본 결과를 고정 기준값으로 보존한다. 현재 분류는 미결정이 아니다. 향후 최신 입력으로 이 기준표도 갱신하는 요구가 새로 생길 때만 A 변경 후보로 명시하고 UNIQUE·배열 shape·후행 계산을 함께 검토한다.
- LNG 직접 입력은 2918행까지 기록돼 있지만 UNIQUE의 세 원본 범위는 2000행에서 끝난다. 2001~2918행은 현재 UNIQUE 입력에 포함되지 않는다. CN/CT의 전체 열 SUMIFS가 읽는 범위와는 별개이며, 구조 변경이나 갱신 요구를 확정할 때 이 차이를 검증한다. PNG의 입력 끝 1104행과 UNIQUE의 원본 끝 2000행도 분리한다.
- 본 시트 V5:W2000·E5:F2000의 선행 수식 각각 3,992셀에 원래 #REF!가 있다. 유효한 Q/R/T·A/B/C 연결은 확인했지만 삭제된 참조의 원래 원천은 복원하지 않았다. 저장값·수동 계산 모드는 실제 계산 성공 근거가 아니다.
- 정의된 이름에는 본 시트와 backup의 BL4:CF4를 가리키는 `_xlnm._FilterDatabase`가 존재한다. 해당 범위는 UNIQUE 배열과 겹치지 않으며, 시트 전체에 이름 참조가 없다고 일반화하지 않는다. 이번 5셀 경로에서 미해석 이름/구조 참조 토큰은 발견되지 않았다.
- 4파일 외부 링크 metadata에는 과거 Trade 파일의 동명 시트가 있다. 셀 수식 1,863,766개에서 확인한 명시적 외부 참조 중 이번 배열·현재 후행 수식 셀과 겹치는 것은 0건이다. 같은 이름만으로 현재 기준본과 동일한 파일이라고 보지 않으며 VBA·외부 모델의 소비는 별도다.

검증은 최신 XML에서 복원한 세 대상 시트 참조 색인 1,070,405개와 기존 색인의 6개 좌표/소유자 배열 전체 일치, shared 상대 좌표의 Translator 대조, 시트별 수식 수·입력 hash 전후 일치·parse failure 0으로 수행했다. 정적 경로의 관측이며 분기 실행·실제 값 변경·재계산·배열 확장·파일 생성·모델 실행 검증은 수행하지 않았다. 재현 코드·상세 좌표·외부 참조 대조와 이번 변경 검증은 [stable work note](../work-notes/2026/2026-09-03-model-input-transfer-format.md)의 「UNIQUE 5셀 상류·하류와 사용자 편집 연결 조사 결과」에 기록한다.

지원 함수가 있는 외부 참조 수식도 외부 파일 없이 재계산할 수 있다는 뜻은 아니다. 업무 정책은 domain 규칙 33~34·54를 따르며 외부 원본 요청이나 link 갱신을 추가하지 않는다. 조건부서식 원본 보존과 native 표시 재현도 함수 등록 여부와 별도 검증이다. [공식 평가 안내](https://poi.apache.org/components/spreadsheet/eval.html)는 evaluator 실행과 cached value 갱신, Excel 재개방을 요청하는 플래그를 구분한다.

이 결과는 기존 POI 전체 SUPPLY 로딩의 1 GiB OOM이나 LibreOffice의 보존 실패를 해소하지 않는다. 다음 단계는 표별 allowlist를 이 사용 위치에 연결하고, 계산 허용 함수/조합·변경 입력·구조 변경의 Excel 비교와 원본 보존/메모리 검증을 진행하는 것이다. 계산 엔진은 아직 선정하지 않는다.

## 재현·검증 경계

스크립트는 `scripts/scan-model-input-functions.py`다. 입력은 고정 baseline manifest와 프로젝트 POI 5.4.0 및 의존 jar의 classpath다. 새 출력 경로를 지정해야 하며 기존 결과를 덮어쓰지 않는다.

```bash
python3 -B scripts/scan-model-input-functions.py \
  --manifest docs/references/mi-model-input-baseline-2026-10-07-manifest.json \
  --poi-classpath '<POI 5.4.0 및 의존 jar의 로컬 classpath>' \
  --output '<새 함수 조사 JSON 경로>'
```

스크립트는 원본을 열어 읽기만 하고 입력 hash를 전후 대조한다. 수식·셀 값 원문을 출력하지 않으며 사용자 허용 정책을 추론하지 않는다. 전체 실행과 독립 파서 대조·누락 검사·diff 결과는 `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`의 이번 함수 조사 단계가 소유한다. 실제 workbook 계산·재저장·모델 E2E는 후속 검증이다.
