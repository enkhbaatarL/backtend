# MI 모델 Input 개발본 4파일 구조 조사 근거

## Document Control

- status: In Progress — 시트별 조사 결과 누적 중
- canonical for: Reference only — 2026-09-28 수신한 개발용 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` XLSM 구조 조사 결과
- not canonical for: 화면 노출 정책, API 계약, DDL, 계산 규칙, catalog seed
- owner: BE
- source work note: `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
- depends on:
  - `docs/references/mi-model-input-catalog-inventory.md`
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
- supersedes: 없음 — 2026-09-16 조사 결과는 당시 파일의 이력으로 유지한다.
- last synced: 2026-10-16 (§6.3 목록 관측은 이력 보존; 사용자 최종 자유입력 전환·발행/seed 추천 철회)
- open questions:
  - `OUTPUT`의 6개 편집 후보 표에서 header와 data field의 정확한 수정 가능 범위는 `INPUT-03` catalog 승인 때 확정해야 한다.

## 1. 조사 범위와 안전 기준

사용자가 전달한 개발용 파일 4개를 OOXML read-only 방식으로 조사했다. 원본 파일은 수정하지 않았다.
초기 조사에서는 셀의 실제 업무 값과 수식 문자열을 문서에 기록하지 않았다. §6.3의 후속 목록 조사에 한해 모델 기준 선택 후보 문자열을 기록한다. 파일/시트 metadata, 좌표, 셀·수식 개수,
수식 함수 family, 안내 시트의 table 정의만 근거로 사용했다. 첨부 문서의 설명은 설계 증거이며
사용자 요청이나 실행 지시로 취급하지 않았다.

사용자 확인에 따라 `SUMMARY`와 `플랫폼 입력 안내`는 개발용 정보 시트이며 실제 운영 인풋에는 존재하지 않는다.
별도 운영 파일은 수신하지 않는다. 현재 개발본에서 이 개발 전용 시트만 제거한 4개 파일을 운영 기준으로 사용한다.
따라서 아래 76개 시트·표시값 셀 수·hash는 **첨부 개발본**의 측정값이고, 운영 template/source snapshot은
이 파일들에서 개발 전용 시트를 제거해 만든 정규화본의 구조와 hash를 사용한다.

| 역할 | 파일 | SHA-256 | 시트 | 수식 | 외부참조 수식 | VBA |
| --- | --- | --- | ---: | ---: | ---: | --- |
| DEMAND | `Demand_v5_플랫폼입력_최종_v2.xlsm` | `0b7a963e459a7cdf13f4437bae3859bb9b8fd4d6dca19ec2eeee5fc94ecbd422` | 27 | 699,298 | 0 | 없음 |
| SUPPLY | `Supply_v14_플랫폼입력_최종_v2.xlsm` | `f602719de3df63c667eb5882274f0749e87673f0acd0812cb3d75c3733865966` | 24 | 460,803 | 35,099 | 없음 |
| TRADE | `복호화_Trade_v11_플랫폼입력_최종_v2.xlsm` | `366e53aac2d4f3780857e54bc90bd2379e91357485384734d98c35bf4e1c359e` | 18 | 702,776 | 7,295 | 있음 |
| OUTPUT | `Output_v3_플랫폼입력_최종_v2.xlsm` | `7b6b50bb5197c0c46a7583d5c499e158e12a27c61ba51d794b1e18d7a998c0d8` | 7 | 889 | 0 | 없음 |
| **합계** |  |  | **76** | **1,863,766** | **42,394** |  |

- 표시값 셀 합계는 4,212,857개다.
- 수식 1,863,766개는 모두 cached value를 가진다.
- 계산 후보는 1,821,372셀이고, 외부 workbook 참조 42,394셀은 기존 보존 정책을 유지한다.
- formula family는 341개다. 기존 3파일의 335개에 `OUTPUT` 6개가 추가됐다.

### 개발 전용 시트 제외 영향

| 역할 | 개발 전용 시트 | 제외 시트 수 | 표시값 셀 | 수식 셀 |
| --- | --- | ---: | ---: | ---: |
| DEMAND | `SUMMARY`, `플랫폼 입력 안내` | 2 | 1,163 | 0 |
| SUPPLY | `SUMMARY` | 1 | 327 | 0 |
| TRADE | `SUMMARY` | 1 | 647 | 0 |
| OUTPUT | `SUMMARY`, `플랫폼 입력 안내` | 2 | 274 | 0 |
| **합계** |  | **6** | **2,411** | **0** |

첨부본에서 이 6개 시트만 제외한 운영 기준 규모는
`DEMAND 25 + SUPPLY 23 + TRADE 17 + OUTPUT 5 = 70개 시트`, 표시값 셀 4,210,446개다.
개발 전용 시트에는 수식이 없으므로 수식 1,863,766개와 formula family 341개는 변하지 않는다.
hidden sheet 14개는 이 70개에 포함되며 원본 보존 대상으로 유지한다.

## 2. 이전 3파일 조사와의 차이

2026-09-16 조사와 비교하면 기존 3개 역할의 수식 수, 외부참조 수식 수, formula family 수는 같다.
추가된 시트는 운영 양식 변경이 아니라 개발용 설명 시트다.

| 역할 | 이전 시트 | 최신 시트 | 추가 시트 | 기존 계산 구조 근거 |
| --- | ---: | ---: | --- | --- |
| DEMAND | 25 | 27 | `SUMMARY`, `플랫폼 입력 안내` | 수식 699,298, family 96 동일 |
| SUPPLY | 23 | 24 | `SUMMARY` | 수식 460,803, family 60 동일 |
| TRADE | 17 | 18 | `SUMMARY` | 수식 702,776, family 179 동일 |
| OUTPUT | 없음 | 7 | 신규 역할 전체 | 수식 889, family 6 신규 |

이는 기존 3파일의 계산 구조가 바뀌지 않았다는 강한 근거지만, 이전 binary와의 셀 단위 동일성을
증명하지는 않는다. 운영 template version은 현재 개발본에서 개발 전용 시트를 제거해 만든 운영 기준 4개 파일의 hash를 하나의 set으로 고정해야 한다.

`SUMMARY`와 `플랫폼 입력 안내`는 catalog 작성 근거로만 사용한다. ACTIVE catalog의 runtime sheet,
Case source snapshot, Draft projection, 최종 XLSM, 모델 Job input에는 포함하지 않는다.
첨부 개발본 원본을 그대로 등록하지 않고, 개발 전용 시트를 제거한 정규화본을 운영 source snapshot으로 등록한다.

## 3. 숨김 시트와 참조 수식

첨부 개발본 4파일의 `workbook.xml`과 모든 worksheet formula를 read-only로 조사했다.
shared formula는 anchor 식을 같은 shared index의 follower cell에 적용해 실제 formula cell 수로 집계했다.
직접 참조는 cell formula와 workbook defined name에 hidden sheet 이름이 명시된 경우다. 원문 수식과 셀 값은 기록하지 않았다.

- hidden sheet는 `DEMAND 3 + SUPPLY 6 + TRADE 5 + OUTPUT 0 = 14개`다.
- 14개 모두 일반 `hidden`이며 `veryHidden`은 0개다.
- 다른 시트의 cell formula가 hidden sheet를 직접 참조한 사례는 0셀이다.
- hidden sheet를 참조하는 workbook defined name도 0건이다.

| 역할 | # | hidden sheet | 상태 | 이 시트를 직접 참조하는 formula cell | hidden sheet 내부 formula cell | 조사 결과 |
| --- | ---: | --- | --- | ---: | ---: | --- |
| DEMAND | 11 | `Scenario Tracker` | hidden | 0 | 0 | 직접 formula dependency 없음 |
| DEMAND | 25 | `Sheet2_DISCARDAFTUSE` | hidden | 0 | 0 | 개발 설명 문자열 외 직접 formula dependency 없음 |
| DEMAND | 26 | `CubeDump_DISCARDAFTUSE` | hidden | 0 | 0 | 개발 설명 문자열 외 직접 formula dependency 없음 |
| SUPPLY | 19 | `{USA}RigsLogic` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| SUPPLY | 20 | `{China}PolicyLogic` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| SUPPLY | 21 | `{USA}HurricanesLogic` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| SUPPLY | 22 | `RegasLogic` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| SUPPLY | 23 | `PipelineLogic` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| SUPPLY | 24 | `MonthlyAdjProduction` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| TRADE | 4 | `Sheet1` | hidden | 0 | 88 | `K1:K44`, `W1:W44`; 명시적 sheet/external-book 참조 없음 |
| TRADE | 15 | `Industry research` | hidden | 0 | 1 | `A3`에 기존 `#REF!` formula 1셀 |
| TRADE | 16 | `net exporter` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| TRADE | 17 | `pipeline cap` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |
| TRADE | 18 | `pipeline contract` | hidden | 0 | 0 | external-link sheet-name cache에는 존재하지만 직접 formula dependency 없음 |

외부 link part의 sheet-name cache는 외부 workbook 구조 metadata이며, 현재 workbook의 cell formula가
local hidden sheet를 참조한다는 뜻은 아니다. 반대로 직접 formula 참조가 0이라는 사실만으로 hidden sheet를
삭제해도 안전하다고 결론 내릴 수는 없다. 따라서 hidden sheet를 삭제 대상으로 분류하지 않는다.

후속 사용자 확인으로 hidden sheet는 다른 sheet 계산과 무관하며 **삭제 대상이 아니라 원본 보존 대상**으로 확정됐다.
최종 XLSM은 승인된 source snapshot의 모든 sheet를 먼저 복제하고, hidden sheet의 worksheet part·순서·이름·숨김 상태·
수식·cached value·style·relationship과 연결 package를 그대로 유지한다. hidden sheet는 화면 projection, Draft 변경,
dependency graph와 Backend 계산 engine input/output에서 제외한다. 이 방식은 Trade VBA나 외부 실행기의 잠재적 이름 기반
접근 여부를 별도로 해석하지 않고도 원본 package를 보존한다.

## 4. OUTPUT 시트 분류

| # | 시트 | 상태 | 범위 | 분류 | 근거 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `SUMMARY` | visible | `A1:J26` | 개발 전용 | 실제 운영 인풋에 없음. catalog 작성 근거로만 사용 |
| 2 | `USCostofSupply` | visible | `A1:AC818` | 화면 대상 | Cost of Supply 기준 곡선과 helper 수식 |
| 3 | `BCPrices` | visible | `A1:S21` | 화면 대상 | 연도별 Base Case 가격 기준정보 |
| 4 | `PremiumCalc` | visible | `A1:W188` | 화면 대상 | Premium 입력 2개와 계산·참조 표 |
| 5 | `AnnualPriceCalc` | visible | `A1:AL63` | 화면 대상 | Spread 입력 1개와 가격 계산·참조 표 |
| 6 | `PriceSeasonality` | visible | `B1:O7` | 화면 대상 | 지역별 월 seasonality와 연평균 상승률 |
| 7 | `플랫폼 입력 안내` | visible | `A1:I14` | 개발 전용 | 실제 운영 인풋에 없음. 색상/입력 범위/엔진 source 설명 |

실제 운영 `OUTPUT` 예상 구성은 위 두 개발 전용 시트를 제외한 business sheet 5개다. 여기에는 18개 논리 table이 있고, 그중 6개가 직접 편집 후보이며
12개는 계산·참조·helper table이다. sheet 전체를 editable로 취급하지 않는다.

## 5. OUTPUT 직접 편집 table 후보

| 시트 / table key 후보 | baseline | header/data 후보 | 수정 후보 | 수식/RO | 구조 정책 후보 |
| --- | --- | --- | --- | --- | --- |
| `USCostofSupply` / `us_cost_of_supply` | `B7:K818` | `7` / `8:818` | 손익분기 가격과 연도별 누적 생산량 기준값 | `D7:K7`, `P7:P818` helper | row 제한, column 고정 |
| `BCPrices` / `base_case_prices` | `B7:E21` | `7` / `8:21` | `B:E`의 Year, HH, East Asia Spot, TTF | 없음 | row 가능, column 고정 |
| `PremiumCalc` / `premium_ttf` | `B38:H47` | `38` / `39:47` | Year, Base case, User adjusted case 후보 | `E:H` 계산 | row 가능, column 고정 |
| `PremiumCalc` / `premium_east_asia_spot` | `B56:H65` | `56` / `57:65` | Year, Base case, User adjusted case 후보 | `E:H` 계산 | row 가능, column 고정 |
| `AnnualPriceCalc` / `annual_price_spread` | `F49:G49` | 고정 cell | JKM-TTF historical spread limit 2개 값 | 주변 가격 계산 수식 | 구조 추가 금지 |
| `PriceSeasonality` / `price_seasonality` | `B1:O4` | `1` / `2:4` | Price Region별 1~12월 seasonality와 연평균 상승률 | 없음 | row 제한, column 고정 |

주의:

- 위 범위는 첨부 workbook의 `SUMMARY`와 `플랫폼 입력 안내`가 제시한 후보이며 canonical catalog seed가 아니다.
- `USCostofSupply`의 7행에는 formula header가 섞여 있어 header와 수정 가능 field를 분리해야 한다.
- Premium 두 표는 `E:H`가 수식이므로 화면에는 계산값으로 표시할 수 있지만 write 요청에서는 거부해야 한다.
- `AnnualPriceCalc`는 표 전체가 아니라 `F49:G49` 두 값만 수정 후보이다.
- `PriceSeasonality`의 3개 Price Region은 현재 엔진이 요구하는 고정 집합으로 보이며 일반 row 추가로 열지 않는다.

## 6. OUTPUT 계산·참조 table 후보

직접 수정 대상이 아닌 12개 table은 다음과 같다.

- `US Breakeven Gas Price Helper`
- `Base Case LNG Price Premium Calculation`
- `Premium Regression Coefficients`
- `Shipping Cost Change by Price Region`
- `Shipping Cost Calculation Reference`
- `Annual Henry Hub Price Calculation`
- `Annual East Asia Spot Price Calculation`
- `Annual TTF Price Calculation`
- `Annual Prices after Price Floor`
- `LNG Price Floor Mechanism`
- `Historical JKM and TTF Spread`
- `Revised Annual Prices after Spread Adjustment`

이 표들은 원본 수식·cached value·연결을 보존하고 화면 write allowlist에서 제외한다.

## 6.1 `SUMMARY`의 직접 입력 비대상 visible sheet 참조 조사

각 파일 `SUMMARY`에서 E열이 정확히 `직접 입력 대상 아님.`인 행을 찾고, 개발 전용 시트와 hidden sheet를
제외한 뒤 OOXML formula와 workbook defined name을 양방향으로 조사했다. shared formula follower는 anchor
formula의 참조 sheet를 적용해 실제 formula cell 수로 집계했다. 셀 값과 formula 원문은 기록하지 않았다.

| 역할 | 시트 | SUMMARY 행 | 시트 내 수식 | 다른 시트로 나가는 직접 참조 | 다른 시트에서 들어오는 직접 참조 | 기존 `#REF!` 수식 |
| --- | --- | ---: | ---: | --- | --- | ---: |
| DEMAND | `Input Adjustment ->` | 9 | 0 | 없음 | 없음 | 0 |
| DEMAND | `기준정보 ->` | 30 | 0 | 없음 | 없음 | 0 |
| SUPPLY | `Input Upstream ->` | 9 | 0 | 없음 | 없음 | 0 |
| SUPPLY | `Input Midstream ->` | 12 | 0 | 없음 | 없음 | 0 |
| SUPPLY | `LNG Summary` | 29 | 0 | 없음 | 없음 | 0 |
| SUPPLY | `LiqUtilAdjustment_backup` | 30 | 47,279 | 내부 직접 참조 없음 | `LiqProdAdjustment` 397셀, `LiqProdAdjustment_backup` 348셀 | 47,071 |
| SUPPLY | `LiqProdAdjustment_backup` | 31 | 69,449 | `LiqUtilAdjustment_backup` 348셀 | 없음 | 68,904 |
| SUPPLY | `BCLiquefaction_backup` | 32 | 117,729 | 외부 `[1]LUT` 35,091셀 | 없음 | 4 |
| TRADE | `4.5 PriceModelInput` | 56 | 267 | 외부 `[2]` workbook 267셀 | 없음 | 0 |
| TRADE | `backupLNG_TradeRestrictionInput` | 64 | 0 | 없음 | 없음 | 0 |
| TRADE | `backup_MidstreamContractInput` | 65 | 117,970 | 내부·외부 직접 참조 없음 | 없음 | 39,350 |
| OUTPUT | 해당 시트 없음 | - | - | - | - | - |

세부 관계:

- `LiqProdAdjustment!CM11:CM407` 397개 수식이 `LiqUtilAdjustment_backup`을 직접 참조하며, 397개 모두
  기존 `#REF!` 토큰을 함께 포함한다.
- `LiqProdAdjustment_backup!DB11:DB358` 348개 수식이 `LiqUtilAdjustment_backup`을 직접 참조한다.
  이 식들은 `IF`, `COUNTBLANK`, `ISBLANK`로 중복 입력 여부를 검사한다.
- `BCLiquefaction_backup`의 외부참조 35,091개는 D열의 8~41,110행 범위에 분포하며 모두 외부
  `[1]LUT`를 사용하는 `SUMIFS`다.
- `4.5 PriceModelInput`의 267개 수식은 D~X열의 4~91행 범위에 분포한다. 모두 외부 `[2]` workbook을
  참조하며 `0. Contracted Volume` 155셀, `ShippingCostInput` 88셀, `2.2 Demand Adjustment` 88셀,
  `3.3 Importer Demand Adjustment` 88셀, 그 외 4개 sheet 각 8셀이다. 한 formula가 여러 외부 sheet를
  함께 참조할 수 있어 sheet별 수의 합은 267보다 크다.
- 같은 267개 수식에는 다른 Trade sheet를 가리키는 직접 sheet 참조, 다른 Trade sheet 소유 Excel Table
  참조, defined name 참조가 모두 없다. 같은 sheet의 `Table_JKMOutput` 32셀과 `Table_TTFOutput` 56셀을
  참조하는 수식은 있으나 두 Table도 `4.5 PriceModelInput` 내부 범위(`R3:U70`, `V3:X70`)다. 따라서 다른
  Trade sheet의 값 변경만으로 이 sheet의 셀이 formula dependency를 통해 갱신되지는 않는다.
- `4.5 PriceModelInput`의 저장된 cached result는 숫자 104셀과 `#VALUE!` 163셀이다. 오류 셀은
  `O16:O23`, `T4:T35`, `U4:U70`, `X4:X59`이고 163개 모두 외부 workbook 범위를 사용하는 `SUMIF`를
  포함한다. formula text 자체에는 `#VALUE!`나 `#REF!` 토큰이 없다. workbook은 `calcMode=manual`,
  `calcCompleted=0`, `calcOnSave=0` 상태다. 따라서 이 오류는 깨진 주소인 `#REF!`가 아니라 외부 workbook을
  열지 않은 상태에서 `SUMIF`를 계산할 때 발생하는 Excel 동작과 일치한다. 다만 정적 파일만으로 마지막 계산
  시점의 외부 파일 상태까지 증명하지는 않는다.
- 현재 파일에서 외부 `SUMIF`를 포함한 formula cell은 163개이며 cached result는 163개 모두 `#VALUE!`다.
  숫자 cached result 104개는 `SUMIF`가 아니라 `INDEX/MATCH` 등 다른 외부 참조 수식이다. 따라서 숫자가
  표시되는 현상을 외부 workbook 없이 `SUMIF`가 새로 계산한 결과로 해석하지 않는다.
- 업무 담당 매니저 확인에 따라 `4.5 PriceModelInput` U열은 무시한다. U열의 `#VALUE!` 67셀(`U4:U70`)을
  플랫폼 범위에서 제외하면 남는 대상은 formula 200셀, 숫자 cached result 104셀, 외부 `SUMIF` 기반
  `#VALUE!` 96셀(`O16:O23`, `T4:T35`, `X4:X59`)이다. U열은 최종 XLSM에 원본 보존한다.
- 최종 업무 결정은 U열만이 아니라 `4.5 PriceModelInput` sheet 전체를 read-only로 취급하는 것이다. 화면에는
  U열을 제외한 승인 table 값을 표시만 하고 수정·Draft·dependency·Backend 재계산·계산 결과 검증에는 포함하지
  않는다. 최종 XLSM은 원본 workbook 전체를 먼저 복제한 뒤 다른 allowlist sheet만 변경하고 이 sheet는 건드리지 않는다.
- 세 backup sheet의 `_xlnm._FilterDatabase` defined name은 Excel 자동 필터 범위이며 계산 dependency로
  세지 않았다. 위 표의 다른 시트 직접 참조 수에는 cell formula만 포함했다.

결론적으로 `직접 입력 대상 아님.`은 화면 write 제외 근거일 뿐 계산·패키지 제외 근거가 아니다.
특히 `LiqUtilAdjustment_backup`은 현재 visible 업무 시트에서 직접 참조되므로 독립 시트로 취급할 수 없다.
나머지 formula 보유 시트도 외부 연결, 대량 같은-sheet 수식과 기존 오류를 포함하므로 source snapshot 보존
대상으로 유지하고 Backend 재계산 포함 여부는 별도로 결정해야 한다.

사용자 확인으로 위 표에서 수식·직접 참조가 모두 0인 6개 sheet는 source 값을 화면에 보여주는 read-only
대상으로 확정했다. 이 6개는 write allowlist, Draft 변경, dependency와 Backend 재계산에서 제외하지만 최종
XLSM에는 source snapshot 상태로 보존한다. 이 결정은 수식 또는 참조가 있는 나머지 5개 sheet에 적용하지 않는다.
2026-10-01 사용자 확인으로 이 중 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`,
SUPPLY `Input Upstream ->`, `Input Midstream ->`)는 화면에 표시하지 않고 원본 그대로 복제한다. read-only 표시는
`LNG Summary`, `backupLNG_TradeRestrictionInput` 2개다. 정본은 domain design 규칙 43이다.

## 6.2 시트별 조사 결과 대장

이 절은 한 번에 한 시트를 같은 양식으로 조사한 결과를 누적한다. 측정 사실은 이 reference에 기록하고,
화면·수정·계산·생성 규칙은 사용자 결정 후 domain design과 feature spec에 반영한다. 기술 조사만 끝난 상태에서
업무 정책을 자동으로 확정하지 않는다.

### 조사 결과 색인

| 역할 | 시트 | 조사 상태 | 정책 상태 | 화면 | 수정·Draft | Backend 계산 | 최종 XLSM |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TRADE | `4.5 PriceModelInput` | 조사 완료 | 2026-10-08 사용자 확정으로 변경 — domain 규칙 44 | T-183/T-184 입력 편집 표시, U열·T-177~T-182 미표시 | T-183/T-184 값 편집·아래 행 추가 | 편집 허용만으로 재계산·외부 링크 갱신 승인 아님 | 원본 선복제 후 두 표 승인 변경만 반영, 나머지 보존 |
| TRADE | `LNG_TradeRestrictionInput` | 조사 완료 | 미확정 | 미확정 | 미확정 | 미확정 | 미확정 |
| TRADE | `MidstreamContractInput` | 2026-10-08 최신 조사 | 일부 사용자 확정 — domain 규칙 57·59 | 아래 최신 조사 절 | 아래 최신 조사 절 | 아래 최신 조사 절 | 아래 최신 조사 절 |

### 표준 사례 — TRADE `4.5 PriceModelInput`

#### 1. 식별과 구조

| 항목 | 조사 결과 |
| --- | --- |
| source file | `복호화_Trade_v11_플랫폼입력_최종_v2.xlsm` |
| source SHA-256 | `366e53aac2d4f3780857e54bc90bd2379e91357485384734d98c35bf4e1c359e` |
| sheet 순서·상태 | 11번째, visible |
| worksheet dimension | `B2:AO91` |
| Excel Table | `Table_LiqCostOutput` `B3:D11`; `Table_ShipCostOutput` `F3:I150`; `Table_HHOutput` `K15:P23`; `Table_JKMOutput` `R3:U70`; `Table_TTFOutput` `V3:X70` |
| 구조 특이사항 | worksheet dimension은 91행까지지만 `Table_ShipCostOutput` table definition은 150행까지다. 원본 metadata로 기록하며 조사 과정에서 보정하지 않는다. |

#### 2. 수식과 dependency

| 항목 | 조사 결과 |
| --- | --- |
| formula cell | 267개 |
| external workbook formula | 267개, 외부 `[2]` workbook의 8개 sheet 참조 |
| 다른 Trade sheet로 나가는 직접 참조 | 0개 |
| 다른 Trade sheet에서 들어오는 직접 참조 | 0개 |
| 다른 sheet 소유 Excel Table 참조 | 0개 |
| 계산용 defined name 참조 | 0개 |
| 같은 sheet Table 참조 | `Table_JKMOutput`을 사용하는 formula 32셀, `Table_TTFOutput`을 사용하는 formula 56셀 |
| 주요 함수 집계 | `INDEX` 400회, `MATCH` 400회, `SUMIF` 171회, `IFERROR` 104회 |

함수 집계는 formula cell 수가 아니라 formula text 안의 함수 출현 횟수다. 한 formula에 같은 함수가 여러 번
나오므로 합계가 267보다 클 수 있다.

#### 3. cached result와 계산 설정

| 항목 | 조사 결과 |
| --- | --- |
| 숫자 cached result | 104셀 |
| 오류 cached result | `#VALUE!` 163셀 |
| 오류 범위 | `O16:O23` 8셀, `T4:T35` 32셀, `U4:U70` 67셀, `X4:X59` 56셀 |
| 외부 `SUMIF` 결과 | 관련 formula 163셀 모두 cached `#VALUE!`; 숫자 cached result 0셀 |
| workbook 계산 설정 | `calcMode=manual`, `calcCompleted=0`, `calcOnSave=0` |
| formula 원문 오류 토큰 | formula text 자체의 `#VALUE!`·`#REF!` 토큰 없음 |

#### 4. 조사 결론과 확정 정책

> 2026-10-08 대체: 아래 시트 전체 read-only·쓰기 제외·시트 미변경 정책은 domain 규칙 44로 대체됐다. T-183/T-184는 입력 편집과 각 표 마지막 데이터 행 아래 행 추가가 가능한 C이고, 미표시 T-177~T-182는 규칙 57의 A 원본 보존, U열은 규칙 44의 화면 제외를 따른다. 아래 목록은 당시 확정 기록이다. 첫 두 항목은 당시 조사 source의 기술 관측이며 최신 v8에서 다시 확인한 값이 아니다.

- 다른 Trade sheet 값 변경만으로 이 sheet가 formula dependency를 통해 갱신되는 셀은 없다.
- 외부 workbook 없이 표시되는 숫자 104개는 저장된 cached result이며 새 계산 결과로 해석하지 않는다.
- U열은 업무 담당 매니저 확인에 따라 화면에서도 제외한다.
- U열을 제외한 승인 table 값은 화면에 read-only로 표시한다.
- sheet 전체를 write allowlist, Draft 변경, dependency graph, Backend 재계산과 계산 결과 검증에서 제외한다.
- 최종 XLSM은 원본 workbook 전체를 먼저 복제하고 catalog가 허용한 다른 sheet/cell만 변경한다.
  `4.5 PriceModelInput`의 worksheet, Table, formula, cached value, style, relationship과 관련 externalLink/package는
  건드리지 않는다.

#### 5. 근거와 한계

- OOXML worksheet, table, workbook defined name, external link와 formula를 read-only로 조사했다.
- shared formula follower는 anchor formula를 적용해 formula cell 수에 포함했다.
- 원본 cell 업무 값과 formula 문자열은 문서에 기록하지 않았다.
- TRADE workbook에는 VBA가 있다. 이번 결론은 formula·Table·defined name dependency 근거이며 VBA 동작 부재를
  증명한 결론은 아니다. 다만 최종 XLSM에서 원본 workbook과 이 sheet를 건드리지 않는 정책으로 package 보존 위험을 줄인다.
- 정적 파일만으로 마지막 계산 시점에 외부 workbook이 열려 있었는지는 증명하지 않는다.
- 조사 상태: `완료`; 정책 상태: `사용자 확정`; 결정일: `2026-09-28`.

### 조사 사례 — TRADE `LNG_TradeRestrictionInput`

#### 1. 식별과 구조

| 항목 | 조사 결과 |
| --- | --- |
| source file | `복호화_Trade_v11_플랫폼입력_최종_v2.xlsm` |
| source SHA-256 | `366e53aac2d4f3780857e54bc90bd2379e91357485384734d98c35bf4e1c359e` |
| sheet 순서·상태 | 2번째, visible |
| worksheet dimension | `B4:K509` |
| Excel Table | 없음 |
| query table·connection | 대상 worksheet relationship 0개, 대상 query table 0개, 대상 connection 0개. workbook 전체에는 Excel Table 10개, query table 5개, connection 11개가 있으나 query table 5개는 모두 `MidstreamCapacityInput` 소유이며 대상 시트와 연결되지 않는다. |
| 구조 특이사항 | 병합 범위 `B4:K4` 1개. cell element 5,060개 중 serialized value는 267셀(문자열 203, 숫자 64)이고 실제 값 좌표는 `B4:K37` 안에 있다. 나머지 4,793셀은 509행까지 이어지는 style-only 빈 셀이다. 원본 metadata로 기록하며 조사 과정에서 보정하지 않는다. |

이전 2026-09-16 조사에서 제시한 논리 table 후보와 자연키 한계는
[`mi-model-input-catalog-inventory.md` §4.3](./mi-model-input-catalog-inventory.md#43-trade)를 따른다.
그 후보는 이번 조사에서 화면·수정 정책을 확정하는 근거로 승격하지 않는다.

#### 2. 수식과 dependency

| 항목 | 조사 결과 |
| --- | --- |
| formula cell | 0개, shared formula 0개, 함수 집계 없음 |
| 다른 Trade sheet로 나가는 직접 참조 | 0개 |
| 다른 Trade sheet에서 들어오는 직접 참조 | 0개. 영향받는 내부 sheet 0개 |
| 같은 sheet 소유 Excel Table 참조 | 대상 sheet 소유 Excel Table이 없어 0개 |
| 다른 sheet 소유 Excel Table 참조 | 대상 sheet formula가 없어 0개 |
| workbook defined name 참조 | 대상 sheet를 직접 가리키는 defined name 0개, defined name을 통한 inbound formula 0개 |
| 동적 formula 참조 후보 | workbook formula에서 `INDIRECT`·`OFFSET` 0개 |
| external workbook formula | 대상 sheet 0개 |

따라서 확인한 cell formula, Excel Table, defined name, query table·connection 경로에서는
`LNG_TradeRestrictionInput` 값 변경으로 갱신되는 다른 내부 sheet를 찾지 못했다. 이 결과는 VBA나 모델 실행기 같은
formula 밖의 소비자가 없다는 뜻이 아니다.

#### 3. cached result와 계산 설정

| 항목 | 조사 결과 |
| --- | --- |
| formula cached result | formula cell이 없어 cached result 0개 |
| 기존 오류 | formula 오류 0개, literal error cell 0개 |
| workbook 계산 설정 | `calcMode=manual`, `calcCompleted=0`, `calcOnSave=0`; `calcChain.xml` 있음 |
| 외부 연결 package | workbook 전체 externalLink part 2개, connection 11개. 대상 sheet의 external formula·relationship·query connection은 모두 0개 |

원본 workbook을 열거나 재계산하지 않았으므로 위 설정과 package 상태는 저장된 파일의 정적 근거다.

#### 4. 조사 결론과 정책 상태

- 기술 조사에서 확인한 내부 영향 sheet는 0개다. 직접 formula 참조, defined name 경유 참조,
  Excel Table·query table·connection 관계가 모두 없기 때문이다.
- `LNG_TradeRestrictionInput`은 수식이 없는 값·서식 시트다. 이 사실만으로 화면 노출, 수정 허용, Draft 반영,
  Backend 계산 포함 여부나 최종 XLSM 생성 규칙을 결정하지 않는다.
- 정책 상태: `미확정`.
- 가능한 처리 선택지는 기존 catalog 후보 범위를 사용자 수정 대상으로 승인하는 방식, 값을 read-only로 표시하는 방식,
  화면에서 제외하고 원본 보존만 하는 방식이다. 수정 방식을 선택한다면 style-only 빈 행을 신규 입력 슬롯으로 볼지,
  row identity와 허용 범위를 어떻게 정할지도 함께 확정해야 한다.
- 이번 세션에서 사용자 정책 확정이 없으므로 domain design과 feature spec은 변경하지 않는다.

#### 5. 근거와 한계

- `scripts/inventory-model-input-catalog.py`, `scripts/inventory-model-input-formulas.py`와 Python
  `zipfile`·XML read-only 검사를 교차 사용했다. shared formula follower는 anchor의 sheet dependency를 적용했다.
- 원본 cell 업무 값과 formula 문자열은 문서에 기록하지 않았다.
- TRADE workbook에는 VBA가 있고 대상 worksheet code name은 `Sheet54`다. `vbaProject.bin`의 20개 VBA module stream을
  정적으로 풀어 확인한 결과, `Sheet54` module에는 procedure와 worksheet event가 없었고 실행 코드에서 대상 sheet name,
  `Sheet54`, `Sheets(2)` 직접 참조도 찾지 못했다.
- 그러나 일반 module 2개에는 각각 변수 기반 `Sheets(...)` 접근 1건이 있다. 변수의 실행 시 값, 외부 호출 순서,
  add-in·모델 실행기의 sheet 접근까지 정적 파일만으로 증명할 수 없으므로 VBA·외부 실행 영향 부재를 결론 내리지 않는다.
- workbook을 Excel에서 열어 실제 매크로·connection refresh·모델 실행을 수행하지 않았다. 원본 보존을 위해 재계산·저장도 하지 않았다.
- 조사 전후 source SHA-256은 모두 `366e53aac2d4f3780857e54bc90bd2379e91357485384734d98c35bf4e1c359e`다.
- 조사 상태: `완료`; 정책 상태: `미확정`; 조사일: `2026-09-28`.

### 최신 v8/v6 우선 조사와 TRADE `MidstreamContractInput` — 2026-10-08

#### Execution Plan — 트랙 A 조사 단위

- 목표·완료 기준: 4파일의 Q1~Q6 우선 조사 질문 목록과 T-133~T-139 상세 근거를 연결하고, 관측으로 해결된 사항·사람의 정책 결정·실행 검증 잔여를 구분한다. 전체 표 조사나 정책 확정을 완료로 표시하지 않는다.
- 변경 범위: 이 최신 조사 절만 추가한다. 이전 버전 사례·수치는 유지한다. domain/API/DDL/Delivery Plan과 공용 work note는 트랙 B가 통합하며, XLSM·조사 XLSX·판별대장·기존 함수 산출물은 읽기만 한다.
- 검사 계획: 고정 manifest의 원본/정규화본 hash 대조 → 최신 사람 확인과 과거 자동 관측 분리 → Midstream 헤더·본문·빈 슬롯·키 후보 및 AG/AI의 필요한 선행 경로 대조 → 입력 불변·문서 diff 검사.
- 예상 위험·한계: 저장된 오류/빈 결과와 style-only 행은 유효한 업무 행·계산 정답·편집 권한을 증명하지 않는다. 확정 A 영역은 유지하고 새 행 필드·추가 경계 및 AG/AI 정책은 근거 없이 확대하지 않는다. 함수 전수 스캔·UNIQUE 후행 추적·계산 PoC를 반복하지 않는다.
- 통합 실행 기록: `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` — **update는 트랙 B 조정 담당**. 트랙 A는 계획·결과·검증·잔여 위험을 이 조사와 최종 응답으로 인계한다.
- 2026-10-08 후속 계획 — AG/AI 상류 입력: 기존 `visibleLNGConsumerCoInputs` 결과를 재사용하고 최신 XML의 해당 수식·승인 입력 범위와 대조한다. AG와 AI 각각에서 A 보존 원천을 고정해도 사용자 입력 연결이 남는지 확인하고, 같은 행/다른 행 입력을 구분한다. 이 절에 결과를 추가하며 계산 승인·A 정책 변경·모델 실행 검증은 하지 않는다.
- 조사 상태: **이번 조사 단위 완료** — 우선 목록 작성 및 Midstream 정적 상세 확인. 정책 상태: T-133/T-139의 기존 입력·표시·행 추가 승인은 유지. AG/AI는 연결된 사용자 입력 변경 시 함께 갱신한다는 사용자 결정을 수신했다(아래 후속 결정, B 정본 반영 대기). 신규 행 세부·계산 범위/오류 처리 및 실행 검증은 남아 있다. 전체 표 조사·정책 확정·구현 준비 완료가 아니다.

#### 1. 최신 근거 식별·재사용 범위

이 절의 기준 묶음은 `2026-10-07-delivery-261006-v8-v6`다. 앞의 v2 사례·수치와 조사 색인은 당시 이력으로 보존한다. 특히 앞의 `4.5 PriceModelInput` 시트 전체 제외 결론을 현재 정책으로 적용하지 않는다. 최신 T-183/T-184 C 승인과 미표시 표의 A 경계는 domain §6 규칙 44·57 및 아래 A-09를 따른다.

| 역할·버전 | 원본 snapshot SHA-256 | 정규화본 SHA-256 |
| --- | --- | --- |
| DEMAND v8 | `73fa29ff6fa19652243479a43754a65076364488120a69657178c8311b6ce0bc` | `ef516b6de2a829c891dbd991ba9f9d626f104dcae2e5ecd8128b1f0ae31c23bd` |
| OUTPUT v8 | `fa8bbda8ffa3e3aac1ce0767d403c9ab51f94806b286535e8de7878d416e9080` | `69742cc9c6fba170324c09c21986574a390c741ba26d371acdce570aeaac28ce` |
| SUPPLY v6 | `c5ab906edf807dbf2707a9e13d7146a92c7490f054fed1b7d3d42afd4312824e` | `eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36` |
| TRADE v8 | `8274fc25be1a89dac8e57eda64d84593576d7a71524d4b46de035da300cf6f8e` | `b1211662aa00d49d9f03c1d732acd495cf99446ede69abde34565007b3819a31` |

- 경로·업무 시트 보존 검증은 [baseline](mi-model-input-baseline-2026-10-07.md)와 [manifest](mi-model-input-baseline-2026-10-07-manifest.json)를 재사용했다. 이번에 원본 snapshot 4개·정규화본 4개의 실제 hash를 manifest와 대조했다. Downloads 원본의 재선택·정규화 재실행은 하지 않았다.
- [통합 판별대장](model-input/README.md)의 읽기 시점 SHA-256은 `f0011ffc1161b5e12f858b884cdff71a1bb2dd4612c7c7b738de4c183d2ed99c`다. T-133~T-139는 `표 대장` 135~141행이다. J:O의 v8 참고 선언과 BA:BV의 사람 확인을 H:I/P:AZ의 과거 자동 관측과 구분했다. BQ가 미확정이어도 BS의 C 및 입력/표시/추가 승인을 취소한 것으로 읽지 않는다.
- 함수 전수 관측은 [함수 reference/JSON](mi-model-input-functions-2026-10-07.md), 넓은 UNIQUE 후행 집합은 [INPUT-02A Phase 2](../work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md)의 `temp/model-input-function-inventory/2026-10-07/unique-backward-trace.json`을 재사용했다. 현재 배열·AG/AI 선행·외부 참조 구분은 함수 reference의 「UNIQUE 5셀의 상류·하류와 사용자 입력 연결」 및 공용 work note의 같은 조사 결과를 재사용했다. 전수 함수 스캔·후행 graph 생성은 다시 실행하지 않았다.
- 정책 정본은 [domain §1A.3~1A.5·§6](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md), 검증 계획은 [계산 호환성 reference](mi-model-input-sheet-calculation-compatibility.md)가 소유한다. 아래 목록은 확인할 질문과 설계 반영 입력이며 신규 allowlist·계산 승인·catalog seed가 아니다.

#### 2. Q1~Q6 우선 조사 요약표 — 4파일

P1은 write·구조·보존 경계를 바꿀 수 있는 질문, P2는 그 경계를 화면에 전달하는 질문이다. A-01/A-02만 이번 상세 조사 대상이며 나머지는 기존 근거를 선별한 **다음 조사 대상**이다. 다른 시트의 수치를 최신 실측으로 재표기하지 않는다.

| 조사 항목 | 대상·근거·좌표 | 관측 사실 | 기존 정책·미확정 사항 | 해결할 설계 결정(Q번호) | 추가 조사 또는 사람의 결정 |
| --- | --- | --- | --- | --- | --- |
| A-01 / P1 최우선 — Midstream 하류 | TRADE T-135/T-137/T-139; `CK4:CM10 → AT/BI → AG/AI`; 함수 reference·Phase 2·이번 §5 | AG/AI는 자기 입력도 읽는다. BA 저장값은 전부 빈 값이며 현재 입력 키와 BI 저장 키가 불일치한다. 오류·빈 값은 계산 성공 근거가 아니다 | UNIQUE 5셀·T-135/T-136 A 유지. AG/AI는 연결 입력 변경 시 함께 갱신한다는 사용자 결정 수신. 계산 세부 및 표 아래 CN/CT 처리는 미확정 | Q5 — 보존 경계에서 전파를 멈춘 뒤 별도로 계산할 셀·기준값·오류 처리. Q6 — 화면 미표시 계산 영역 표현 | 갱신 필요는 재질문하지 않음. 고정 BA/BI를 읽는 계산 범위·빈 기준값/신규 키 처리·검증은 후속. 공통 계산 경계에 직접 영향을 주므로 첫 순위. 상세 관측 완료, 업무 결정 수신·실행 미완료 |
| A-02 / P1 — Midstream 행·표 경계 | TRADE T-133 `A2:D1104`, T-139 `Q2:U2918`; 대장 135/141행·이번 §3~4 | PNG 입력 전체 4열에도 중복 1쌍. LNG는 값 699행·빈 슬롯 2,215행이며 수식은 4707행까지다 | C·기존 입력·아래 행 추가·열 고정 유지. 빈 슬롯 생성·신규 필수값·clear·수식 생성은 미확정 | Q1/Q2/Q3/Q4 — 조회/입력/계산 경계 분리, 안정적인 행 식별과 생성 위치 | 사람: LNG 704행부터 빈 슬롯 사용 또는 2919행부터 확장, 신규 필수값·비우기 의미. 기술 ID 저장은 B 후속. 중복·빈 슬롯은 공통 row/write 설계를 바꾸므로 P1. 상세 관측 완료 |
| A-03 / P1 — Regas 선행 등록·연결 확장 | SUPPLY T-106~108/T-117; domain 규칙 12·51, 대장 108~110/119행; `H11:AE382`, `DM10:DV382`, `G385:DK444`, `BCRegasification!B7:Q379` | 기존 조사에서 설비 식별 열 미러링·첫 일치 lookup·연간/국가 합이 연결됨. v6 참고 J는 `H11:DK382`이나 확정 직접 편집은 H:AE | H:AE 편집·연쇄 갱신 유지. T-117 행/열 추가의 업무 허용·중복 설비명 처리 및 세부 확장은 미확정 | Q1/Q2/Q3/Q4/Q5 — master 선행 등록, 연결 변경을 한 동작으로 처리할 범위·기존/신규 필드 차이 | 관련 선행 조사 재사용 후 끝행·shared formula·합계 이동·중복 설비명을 좁혀 대조. 참고 선언으로 편집을 DK까지 확대하지 않음. 연결 구조 변경 위험 때문에 다음 1순위 |
| A-04 / P1 — Overwrite·연간 LNG 승인 충돌 | DEMAND T-018/T-019/T-020/T-021; domain 규칙 45·50, Demand 갱신 note; 참고 `D8:G507, I8:I507` / `M8:O507, Q8:Q507`, `LNG Demand!B4:C109`, C1 | 최신 선언은 BCM 입력을 빼고 아래 행 추가를 가리킨다. 연간 LNG에는 지역·국가명 편집 확대 선언이 있음 | 기존 BCM·백만톤 입력, 중간 추가, K/S 보존과 연간표 행 고정·연도 추가·130 SUM 승인을 유지. 변경 후보 미채택 | Q1/Q3/Q4/Q6 — 기존/신규 행 편집·clear·추가 위치, 부속 control과 표 반환·갱신 | 사람 확인 전에 v8 선언/현행 승인 차이만 정리하고 해당 입력/집계 좌표 확인. C1 E 분류·편집 승인은 재질문하지 않음. write 권한 충돌 때문에 P1 |
| A-05 / P1 — LiqUtil 삭제·기간 축 | SUPPLY T-104 `B7:CL407`, `H7:CL7`; domain 규칙 46·47·52 및 대장 106행 | 월 열과 Year/Month/Period가 함께 움직이는 기존 사례. G7은 시작 연도 값이며 H7:CL7은 수식 | 행/열 삭제·아래 행/오른쪽 월 추가 허용은 확정. 삭제 위치·Period 재번호화·참조 갱신은 미확정, backup A 유지 | Q1/Q2/Q4/Q5/Q6 — 삭제 경계·기간 field 역할·화면 갱신 순서·보존 backup 연결 | 허용 삭제 후보 위치의 식별 대응과 영향을 조사하고 삭제 위치·재번호화를 질문. 삭제 허용 자체를 재질문하지 않음. 위치 참조·기간 식별이 바뀌므로 P1 |
| A-06 / P1 — Demand 선택표·단일 환산계수 | DEMAND T-037/T-040/T-041, A인 Selected T-039/T-044/T-045, Hydrogen T-064 `Q7:S8`·입력 R8; Demand 갱신 note §Key Changes | 4표는 입력/조회만 반영하고 C 분류·동시 확장 결정을 남겼음. 선택 결과표는 A 유지 | 입력 승인 유지. 선택표 동시 확장 및 단일 계수 행 추가의 의미/위치 미확정 | Q1/Q2/Q4/Q6 — 한 행 계수와 여러 행 입력의 유형 차이, 연결 생성·갱신 경계 | 표시 입력에 행을 추가할 때 어떤 표를 함께 바꾸는지 확인. A를 자동 확장하는 요구면 정책 변경 후보로 분리. 공통 append 규칙으로 설명되지 않아 P1 |
| A-07 / P1 — OUTPUT 입력·Helper 경계 | OUTPUT T-081/T-082 `USCostofSupply!B8:L816`·`P7:P818`, T-083 `BCPrices!B8:E21`, T-098 `PriceSeasonality!C2:O4`; README·규칙 50/57 | v8 사람 확인은 노랑 입력, T-083만 아래 연도 행 추가. 자동 파랑 수/영향 수는 v3. 과거 817셀 전체 계산 문구는 규칙 50/§6.1·compatibility·Plan에 최신 A 보존으로 정렬 완료 | T-082 A 보존 유지. 상세 계산 경계·D7:K7과 입력 L열 대응은 추가 대조 필요 | Q1/Q3/Q4/Q5 — 최신 입력 allowlist, 연도가 행인 확장, 계산/보존 검증 표본 | 입력/보존 문구 정렬과 조사 완료를 구분. 다음 검사에서 v8 해당 셀·선행만 확인하며 새 계산 allowlist·정답/모델 사용은 미확정. 색만으로 권한을 만들지 않음 |
| A-08 / P1 — BCInput 보존 확정 밖 계산 | DEMAND T-024/T-025/T-026의 `X`, `AG`, `AO4`; domain 규칙 58·BCInput v8 조사 | AE:AF 및 저장값 Helper 보존·신규 AE:AG/X 수식 미생성은 확정. X/AG/AO4 기존 수식의 결정은 별도 | 기존 승인 유지. AO4를 AO5:AO1125 저장값과 같은 것으로 취급하지 않음 | Q3/Q4/Q5 — 기존/신규 셀별 계산 정책과 보존 경계 | 기존 조사에서 미확정 세 경로만 확인해 입력 영향·비교값을 선정. 100,803행 전체 재스캔은 우선순위에 넣지 않음. 잘못된 보존 정책 확대를 막기 위해 P1 |
| A-09 / P1 — PriceModel 신규 C와 native Table | TRADE T-183 `AI4:AK35`, T-184 `AM4:AO59`; domain 규칙 44, T-177~182 A, 이 inventory의 v2 사례 | 기존 v2 전체 제외는 최신 승인과 다름. v8 C 입력·아래 행 추가는 확정 | C 승인 재질문 없음. 신규 입력의 연결/계산과 native Table·A 영역 보존 검증은 미완료 | Q1/Q3/Q4/Q5 — 같은 시트의 C/A 공존·논리 표 확장과 Table metadata 보존 | 두 입력 표와 해당 Table/참조만 대조. 외부 원본 확보를 선행조건으로 만들지 않음. 과거 사례 오적용 위험 때문에 P1 |
| A-10 / P2 — control·조건부서식 | DEMAND T-020 C1, SUPPLY T-104 `G7:XFD8` 등, TRADE T-130 `B6:J1048576`; 함수 JSON의 ISFORMULA 16규칙 | 위 큰 좌표는 조건부서식 적용 범위다. 업무 표 끝이나 전체 편집 가능 범위가 아님 | C1 부속 control과 규칙 DB 관리 방향은 확정. 조건 색 평가 필요 여부는 미확정 | Q1/Q6 — control 소유·입력 후보·표별 화면 갱신과 조건부서식 보존/평가 분리 | 정적 규칙 보존만 필요한지 사람 확인 후 FE 평가 요구를 정함. 셀 값 계산 함수로 승격하지 않음. 먼저 write/구조 경계를 정한 뒤 표현을 결정하므로 P2 |

#### 3. Midstream 식별·헤더·본문·부속 영역

이하 §3~5는 모두 위 TRADE v8 source/normalized hash와 같은 조사다. 역할 TRADE, 시트 `MidstreamContractInput`, sheetId `56`, 원본 5번째/정규화본 4번째, visible, part `xl/worksheets/sheet5.xml`이다. 원본과 정규화본의 해당 part SHA-256은 모두 `6c13f37e190f52876ed9c71a922a777204b46f0affd1aca08b7b26f45c3edb54`로 일치한다.

- 검사 방법: ZIP/OOXML을 읽고 값·수식·빈 셀을 구분했다. 해당 시트의 shared formula(본문을 공유하는 수식) follower는 상대 좌표를 이동해 복원했다. 수식 166,271셀은 manifest와 일치했다. 대장 v3 수치나 색으로 경계를 채택하지 않았다.
- worksheet dimension은 `A1:CU4707`이지만 논리 표나 편집 권한의 경계가 아니다. 대상 시트에는 native Excel Table·병합·데이터 유효성 규칙·sheetProtection이 없고, autoFilter는 `BL4:CF4`다. protection 부재도 사용자 쓰기 권한을 뜻하지 않는다. `_xlnm._FilterDatabase`의 같은 범위는 기존 조사 근거를 유지한다.
- source `SUMMARY!D13:J19`에서 표시/입력/헤더/추가 선언을 확인했다. `SUMMARY`는 정규화본의 runtime 구성원이 아니며 이번 조사에서도 복원하거나 수정하지 않았다.

| 표 ID·이름 | 헤더·본문·부속 관측 | 조회/편집에 쓰는 기존 승인과 남은 경계 |
| --- | --- | --- |
| T-133 Forecasted PNG Contract Volume (Imports) | 제목 A2, 설명 A3, 필드 A4:D4. 값 본문 A5:D1104는 1,100행/4,400셀. 같은 행 E:M은 보정·계산, E5:M2000에 17,964수식. A1105:D2000은 빈 셀 3,584개 | SUMMARY 13행·대장 BA135: 입력 A5:D1104, 헤더 A2:D4 조회, C·아래 행 추가·열 고정. E:M은 입력 화면 미표시. v3 H135 `A2:M1104`를 입력 범위로 사용하지 않음 |
| T-134 Contracted Flows by Year and Country | 제목 AL2, 분류/헤더 AL3:AQ4, 본문 AL5:AQ732. AL/AM 값 1,456셀, AN:AQ 수식 2,912셀. PNG M/E/F/A 및 LNG AF/Q/V/W 전체 열을 집계 | A·미표시·재계산/확장 제외 유지. 입력에 행이 늘어도 이 표를 자동 확장/재계산하지 않음. 표의 업무 연결 관측과 갱신 허용을 구분 |
| T-135 Base Case Share of LNG Contracted Volume by Exporter | 제목 AS2, 그룹 AS3:BI3, 헤더 AS4:BI4, 본문 AS5:BI2000. AS는 유형 값이 미리 채워졌고 AT:BI는 31,936수식. BA/BI는 AG의 보존 조회 원천 | A·미표시 유지. 기존 Phase 2의 UNIQUE 연결 수치 30,936은 연결 부분집합이며 이번 전체 본문 수식 31,936과 모수가 다름 |
| T-136 Base Case Share of PNG Contracted Volume by Exporter | 제목 BM2, 그룹 BL3:CF3, 헤더 BL4:CF4, 본문 BL5:CF2000. BL은 유형 값, BM:CF 39,920수식. 필터 BL4:CF4 | A·미표시 유지. 유형 값·수식이 2000행까지 있다는 이유로 실제 계약 1,996행으로 세지 않음 |
| T-137 LNG Contracts Pivot Table | 제목 CK3, CK4:CM10 배열에 헤더 행 4와 결과 행 5~10 포함. CN4는 합계 헤더, CN5:CN10 6수식. native PivotTable로 새로 판정한 것이 아니라 대장 표 이름 | A·미표시, UNIQUE 수식·배열 결과·저장값 보존. 표 아래 CN11:CN1000 990수식은 현재 표 CK3:CN10 밖이며 정책 경계 별도 |
| T-138 PNG Contracts Pivot Table | 제목 CQ3, CQ4:CS29 배열에 헤더 행 4와 결과 행 5~29 포함. CT4 합계 헤더, CT5:CT29 25수식 | A·미표시, UNIQUE·배열·저장값 보존. 표 아래 CT30:CT2000 1,971수식은 현재 표 CQ3:CT29 밖이며 정책 경계 별도 |
| T-139 LNG Contract Volumes | 제목 Q2, 설명 Q3, 필드 Q4:U4. Q5:U703에 699행/3,495값. Q704:U2918은 빈 입력 셀 11,075개(2,215슬롯). Q2919:U4707도 8,945빈 셀. V:AI 65,842수식, AJ Helper 4,703수식은 5~4707행에 미리 채워짐 | SUMMARY 19행·대장 BA141: 입력 Q5:U2918, 헤더 Q2:U4 조회, C·아래 행 추가·열 고정. V:AJ는 입력 화면 미표시. v3 H141 `Q5:AJ4707`은 표시/입력 경계가 아님. 빈 슬롯 생성과 2918행 밖 확장은 별도 |

위에서 조회 대상은 승인된 두 입력 표와 그 헤더이며, 미표시 보정 열·A 표를 별도 control로 승격하지 않는다. T-133/T-139의 Year는 행의 필드이고 새 연도 열 추가가 아니다. 빈 서식 행·미리 채운 수식의 끝을 논리 입력 표 끝으로 채택하지 않는다. **Q1 해결 근거:** 동일 시트의 표시 입력·미표시 계산·A 보존 표·표 밖 사전 수식을 독립 경계로 표현해야 한다. 최종 seed 등록과 계산 범위 승인은 남아 있다.

#### 4. Midstream 행 식별·값 편집·추가 위치·연결 표

검사 범위는 T-133 A5:D1104와 T-139 Q5:U2918의 저장된 값이다. 공백/빈 값과 완전한 값 행을 나눠 동일 tuple(열 값 묶음)을 비교했으며 업무 값 자체는 기록하지 않았다. 수식 캐시를 행 키로 채택하지 않았다.

| 행 식별 후보 | 관측 사실 | 해결하는 결정과 한계 |
| --- | --- | --- |
| PNG A/B/C: Year·From Country·To Country | 1,100행, 1,099개 조합, 중복 1그룹/2행. 52행·829행은 D 계약량까지 포함한 A:D 전체가 같다. 입력 4열의 빈 값·부분 입력 행은 0 | Q2: 자연키로 기존 행을 유일하게 식별할 수 없음. 중복을 오류로 삭제하거나 임의 통합하지 않음 |
| PNG A/B | 312개 조합, 중복 144그룹/932행 | Q2: 연도·수출국만으로는 더 좁게 식별할 수 없음 |
| LNG Q/R/T: Year·From Country·To Country | 값 699행, 348개 조합, 중복 171그룹/522행. 예: 7/11/16행. Q:R:S:T 전체는 현재 699개로 유일 | Q2: AG의 조회 키 Q/R/T는 계약 행 ID가 아님. Terminal을 포함한 현재 유일 조합도 편집 가능하며 영속 ID의 안정성을 증명하지 않음 |
| LNG Q:U·빈 슬롯 | 값 있는 699행은 5개 필드가 모두 채워져 있고 부분 입력 행 0. 704~2918행은 2,215개 완전 빈 슬롯 | Q2/Q3/Q4: 완전 빈 슬롯에 최초 값을 쓰는 것은 행 생성 정책 필요(규칙 27). 현재 값이 모두 있다는 사실은 신규 필수값 승인이 아님 |
| LNG AJ Helper | AJ는 Q/R/S 연결 수식. 저장된 비어 있지 않은 699개 중 256개만 구별되며 중복 173그룹 | Q2: Helper 이름이나 수식 존재만으로 기술 행 ID로 재사용할 수 없음. 기술 ID의 발급/저장은 B 후속 |

- **기존 값 편집 — 확정 유지:** T-133 A:D의 Year/수출국/수입국/계약량, T-139 Q:U의 Year/수출국/Terminal/수입국/계약량이다. 설명·고정 헤더·미표시 계산 열·A 표는 직접 편집 범위에 넣지 않는다. Year/국가/Terminal도 기존 승인 입력에 포함돼 있으므로 값 조합을 고정 ID로 삼을 수 없다.
- **신규 행 — 일부 미확정:** 위 입력 field를 확장하는 방향은 기존 C·행 추가 승인과 일치한다. 다만 필수값·선택 후보·허용 연도/단위·빈칸/0 의미·기존 행 clear를 행 삭제로 볼지·계산 셀 생성/복제는 아직 승인되지 않았다. sheet에 native validation이 없다는 사실을 자유입력 승인으로 사용하지 않는다.
- **추가 위치:** T-133은 실제 값 끝과 선언 끝이 1104행으로 같아 다음 논리 입력 행은 1105행이다. A1105:D1105는 이미 빈 서식이고 E1105:M1105 수식도 존재한다. T-139는 값 끝 703행, 선언 끝 2918행, 수식 끝 4707행이 다르다. 아래 행 추가/열 고정 승인은 재질문하지 않되, 704행부터 빈 슬롯을 활성화할지 선언 끝 다음 2919행부터 확장할지는 사람의 세부 결정이 필요하다. worksheet 끝 다음 4708행을 기본 추가 위치로 정하지 않는다.
- **연결 표 — 업무 근거와 기술 근거 분리:** 대장 N135는 공통 국가표·Pipeline 용량/경로/배분, N141은 공통 국가표·Supply Plant/Terminal 매핑을 확인하도록 기록한다. 이는 등록 선행 조건이나 값 전파를 확정한 수식 경로가 아니다. 원본 E/F/V/W 및 보정식의 `#REF!` 때문에 삭제된 국가/설비 원천의 실제 위치는 복원하지 않았다. 새 국가·Terminal의 후보/선행 등록이 필요한지 후속 확인한다.
- **구조·보존 영향:** 두 입력은 같은 worksheet의 다른 열 블록에 있고 A 표도 나란히 있다. 물리적인 시트 전체 행 삽입으로 구현하면 A 표의 좌표·수식까지 이동할 수 있다. 승인된 논리 표 추가와 A 원본 불변을 함께 만족하는 방법·영향 범위 검증이 B 후속이며, C 한 표의 승인을 다른 표의 행 추가로 확대하지 않는다. 중간 삽입/삭제는 이 두 표에서 새로 승인하지 않았다.

**Q2~Q4 해결 수준:** 기존 행·빈 슬롯·Helper와 중복 후보를 사실로 구별했고 기존/신규 권한을 같은 직사각형 하나로 표현할 수 없다는 근거를 확보했다. 행 ID 저장 방식, 빈 슬롯 활성화·필수값·clear·수식 복제 및 국가/설비 선행 관계는 정책/후속 설계가 남아 있다.

#### 5. Midstream 계산·보존 경계 — AG/AI 우선

**기존 확정 정책:** domain 규칙 57에 따라 UNIQUE 5셀·배열 결과, T-134~T-138의 A 및 T-187/T-188의 시트 전체 보존을 유지한다. 입력이 바뀌어도 A에서 갱신 전파를 멈춘다. 이 조사에서 A 수식을 값으로 치환하거나 UNIQUE·T-135/T-136을 재계산하는 후보를 만들지 않았다. AG/AI 자체는 그 보존 결정에 자동 포함되지 않는다.

| 필요한 경로·대상 | 기술 관측·검사 범위 | 정책/검증 해석 |
| --- | --- | --- |
| 입력 → A → AG/AI | 기존 양방향 근거의 `Q/R/T → V/W → CK4:CM10 → AT/BI → AG → AI`와 대표 `CK5 → AT5 → BI5 → AG5 → AI5`를 재사용. 본 시트 넓은 후행 83,254셀 중 AG/AI 9,406셀은 기존 집계 | 수식 연결과 승인 전파는 별개다. A에서 멈추므로 83,254셀 전체를 계산 또는 미확정으로 재분류하지 않음 |
| AG5:AG4707 | 4,703수식이 모두 같은 상대 행 패턴. 같은 행 Q/R/T를 연결해 보존 BI5:BI2000에서 정확히 첫 일치를 찾고 BA5:BA2000의 값을 읽음. R이 빈 셀이면 빈 결과를 선택하는 원본 식 | 표의 새 키·키 편집은 고정 BI에 없을 수 있다. 기존 첫 일치 의미를 변경하거나 기본값/재시도/새 mapping을 추가하지 않음 |
| AI5:AI4707·AH5:AH4707 | AI도 4,703개의 동일 상대 행 패턴으로 같은 행 AF + AG × AH를 계산. AH는 같은 행 AF와 Q/V/W를 조건으로 한 전체 AF열 합을 읽음. AF는 U 및 X:AE의 보정 경로를 거쳐 입력과 연결 | AG만 계산해도 AI 정답이 자동 확보되지 않는다. 부분 재계산이면 AH/AF의 승인된 계산/보존 경계와 범위 내 다른 행의 합도 필요. 다른 행 입력·신규 행이 비율 분모에 영향을 줄 수 있음 |
| 고정 기준값의 현재 상태 | BA5:BA2000의 1,996개 수식 저장값 전부 빈 값. BI의 비어 있지 않은 저장 키 6개는 BI5:BI10이며 서로 다름. 현재 Q/R/T 값 연결 699개는 그 6개 중 일치 0건. 저장 문자열 대조이며 수식 실행 아님 | 유효한 수치 기준값이 있다고 주장할 수 없음. 업무에서 빈 BA·없는 키를 어떻게 취급할지 결정 필요. 0/공백 대체·깨진 원천 복구를 자동 승인하지 않음 |
| 기존 오류와 빈 슬롯 | V/W의 데이터 699행은 각각 저장 #REF!, AG/AI는 각각 저장 #N/A. AG/AI 나머지 각 4,004셀은 빈 저장 결과(704~4707). source calcMode=manual·calcCompleted=0·calcOnSave=0은 기존 근거 | 기존 오류 저장을 정상 계산값으로 쓰지 않음. 빈 슬롯에 R 등을 채우면 지금 비어 있는 계산 셀이 새 lookup 오류를 낼 수 있으므로 기존 699행 오류 예외를 신규 행에 확대하지 않음(규칙 31~32) |
| 원본의 집계 상한 | UNIQUE 선행은 2000행까지, T-139 승인 입력은 2918행까지, AG의 고정 BA/BI는 2000행까지. CN은 AF/Q/R/T 전체 열, CT는 M/A/E/F 전체 열을 읽음 | 입력 2001~2918은 현재 빈 슬롯이며 UNIQUE 원천에는 포함되지 않는다. A 유지 중 이 상한을 자동 늘리지 않음. 부분 계산 승인 시에도 고정 기준 키의 범위와 입력 범위를 별도로 표현 |
| 표 밖 사전 수식 | CN11:CN1000 990셀 + CT30:CT2000 1,971셀 = 2,961셀, 전부 빈 저장 결과. §3의 현재 배열 표 아래임을 이번 대조로 확인 | 기존 확정 A를 변경하지 않는다. 이 표 밖 영역의 고정 보존/부분 계산 경계만 별도 미확정으로 B에 전달. 빈 저장값이므로 삭제하거나 입력 슬롯으로 만들지 않음 |

검사 방법은 기존 graph/집계를 재사용하고 최신 target worksheet에서 AG/AH/AI/AJ의 전체 상대 행 패턴, 대표 선행 수식·캐시·입력 키 집계만 대조한 것이다. 함수 실행·분기 평가·재계산 정답·구조 변경은 검증하지 않았다. 직접/간접 참조의 수는 실제 값 변화나 업무 사용 수가 아니다.

**모델 소비 한계:** 기존 함수 reference의 외부 `[2]MidstreamContractInput` 경로는 과거 별도 Trade workbook이며 현재 고정 파일의 내부 연결과 다르다. 이번에는 `rg -n 'MidstreamContractInput|Table_LNG|LNGContract|ContractInput|AG:AI' src scripts openwiki/architecture`로 해당 소비 경로만 찾았으나 일치가 없었다(exit 1). 모델 실행 코드의 AG/AI 읽기 경로는 이 저장소에서 확인하지 못했으므로 **미검증**이다. backend 문자열 검색 0건·다른 worksheet의 직접 참조 0건·기존 오류만으로 모델 미사용/계산 불필요를 판정하지 않는다. UNIQUE 5셀의 별도 사용자 미사용 확인을 AG/AI로 확대하지 않으며 외부 Excel 원본 확보를 새 선행조건으로 두지 않는다.

**Q5 해결 수준:** A 경계와 자기 입력 재계산 후보를 분리했고, ‘보존된 BA/BI를 읽으면 계산 가능’이라는 주장에 필요한 기준값·새 키·오류·AF/AH 의존 조건을 구체화했다. 이 기술 조사 자체는 원본 보존/부분 재계산 중 하나를 승인한 결과가 아니며, 이후 수신한 사용자 갱신 결정은 아래에 별도로 기록한다. Q6에는 표시되지 않는 계산 영역과 화면 갱신 영역을 독립적으로 표현해야 한다는 근거를 전달한다.

##### 후속 확인 — AG/AI 상류의 사용자 직접입력 연결 (2026-10-08)

**결론: 둘 다 사용자 직접입력 영역과 연결된다.** T-135의 BA/BI와 UNIQUE를 원본 보존으로 고정하고 그 내부를 더 따라가지 않아도 연결이 남는다. AG/AI 셀 자체를 사용자 편집 셀로 분류한다는 뜻은 아니다.

- 식별: TRADE v8 / `MidstreamContractInput` / sheetId 56 / T-139. source `8274fc25be1a89dac8e57eda64d84593576d7a71524d4b46de035da300cf6f8e`, normalized `b1211662aa00d49d9f03c1d732acd495cf99446ede69abde34565007b3819a31`, worksheet part `6c13f37e190f52876ed9c71a922a777204b46f0affd1aca08b7b26f45c3edb54`. 기준·대장 hash는 §1과 동일하다.
- 검사 방법·범위: 기존 양방향 결과 `visibleLNGConsumerCoInputs`의 AG/AI 전체 9,406셀 상류 집합을 재사용했다. 현재 입력 hash·worksheet bytes·대장 BA141 `Q5:U2918` 및 BS141 C를 확인하고, 최신 XML의 V5:AI5 수식을 이전 표본과 대조해 AG5/AI5 각각의 A1 참조(Excel 셀 주소)를 분리했다. BA/BI는 추적 종료점이다. V:AI 각 열의 수식 4,703개 및 이전 AG/AH/AI의 상대 행 패턴 검사도 대조 근거로 사용했다. 전수 함수 스캔·UNIQUE 후행 graph는 재생성하지 않았다.

| 대상 | 직접 읽는 셀·사용자 입력으로 이어지는 경로 | 확인된 사용자 직접입력 영역 |
| --- | --- | --- |
| AG5 / AG열 | Q5·R5·T5를 조회 키로 쓰고 R5의 빈 셀 여부도 확인한다. BI5:BI2000에서 키를 찾아 BA5:BA2000의 값을 읽는다 | 같은 행 Q(Year)·R(From Country)·T(To Country). 승인 범위와 교차하면 Q5:Q2918·R5:R2918·T5:T2918. A에서 멈추면 S(Terminal)·U(계약량) 연결은 없음 |
| AI5 / AI열 | 직접 참조는 R5·AF5·AG5·AH5. `Q/R/T → AG → AI`, `U → Z → AC → AF → AI`, `S → AA/AC → AF → AI` 등으로 입력에 연결 | Q/R/S/T/U 모두. 승인 입력 Q5:U2918의 5개 field: Year·수출국·Terminal·수입국·계약량 |
| AI의 다른 행 연결 | Y/AB/AE/AH의 SUMIFS가 U/Q/R/S/T/Z/V/W/AF 전체 열을 사용한다. 예: 다른 행 U → 그 행 Z/AC/AF → AH의 AF열 집계 → AI | 같은 행 입력뿐 아니라 같은 표의 다른 행 입력도 정적 의존 경로에 포함. 현재 값이 있는 Q5:U703와 빈 슬롯 Q704:U2918은 §3~4의 구분을 유지하며 빈 슬롯 활성화를 새로 승인하지 않음 |

기존 전체 상류 결과는 **편집 연결 T-139 Q/R/S/T/U5:2918, 보존 원천 T-135 BA/BI5:2000**으로 이번 표본과 일치했다. A를 고정했을 때 T-133의 PNG 직접입력이나 다른 표의 사용자 입력은 이 기존 집합에서 관측되지 않았다. AG/AI가 A 값만 읽는 독립 영역이라고 판단할 근거는 없다. **Q5에 해결한 사실은 ‘A를 그대로 두어도 AG/AI에 자체 사용자 입력 연결이 존재한다’이다. 조사 시점에 미확정이던 갱신 필요는 아래 사용자 결정으로 해결했다.**

- 조사 상태: 이번 상류 입력 연결 확인 **Complete**. 정책 상태: 기존 A 유지, AG/AI 갱신 필요는 아래 사용자 결정 수신. 수식의 양쪽 분기를 포함한 정적 연결이며 특정 값을 바꿨을 때 실제 결과가 달라지는지, 오류가 해소되는지, 모델이 사용하는지는 검증하지 않았다. `#REF!`로 사라진 원천도 복원하지 않았다.
- 실행·검증: 번들 Python `python3 -B /private/tmp/track-a-ag-ai-upstream-20261008-C4srLH/check_upstream.py` **Pass**. manifest 4역할 hash·대장 승인 범위·기존 집합/표본 대조, 입력 15파일 hash 불변, A1 해석 실패/미해석 토큰 0. 결과/명령 출력은 같은 폴더의 `upstream-result.json`·`run.log`·`input-hashes-before.json`·`input-hashes-after.json`. 공용 work note는 지정 stable path에 B가 update하도록 이 계획·결과·한계를 인계하며 트랙 A는 직접 수정하지 않는다.
- 문서 검증: 같은 폴더의 `verify_docs.py` 및 `git diff --check -- docs/references/mi-model-input-catalog-inventory-2026-09-28.md` **Pass**. 이번 결과 절을 제거하면 후속 계획을 기록한 직후의 본문과 같으며 이전 조사 내용은 변경하지 않았다. 3열/3개 관측 행과 입력 15파일의 불변을 확인했고 `document-verification.json`에 기록했다.

##### 후속 사용자 결정 — 연결 입력 변경 시 AG/AI 함께 갱신 (2026-10-08)

- 근거·작업 범위: 상류 조사 결과를 설명한 뒤 사용자가 “연결되는 사용자 입력 쪽이 변경되면 같이 갱신하는걸로 메모 해줘”라고 결정했다. 이 조사 절의 상태·H1·B 인계만 정렬하며 정책 정본과 공용 work note는 B가 반영한다.
- **결정(H1 / Q5): T-139의 승인된 사용자 입력 Q5:U2918에서 AG/AI에 연결된 값이 변경되면, 영향을 받는 AG/AI를 함께 갱신한다.** §5의 경로에 따라 AG는 Q/R/T, AI는 Q/R/S/T/U 변경을 대상으로 하며, 다른 행의 합계를 통해 영향을 받는 AI도 갱신 범위 검토에 포함한다. 갱신 필요 자체는 다시 묻지 않는다.
- **유지하는 보존 경계:** UNIQUE 5셀과 기존 A(T-134~T-138 등)의 수식·배열 결과·저장값 보존은 그대로다. AG가 읽는 T-135 BA/BI도 고정 원천으로 유지한다. 이 결정으로 A 재계산·확장, 직접 편집 권한 또는 빈 슬롯/신규 행 생성 범위를 확대하지 않는다.
- **남은 세부·검증:** AF/AH 등 필요한 계산 범위, 빈 BA·없는 키·기존/신규 오류 처리, 결과 비교와 A 불변 검증은 후속이다. AG/AI의 모델 소비는 여전히 미검증이며, 갱신 결정과 구분한다. 이번 작업은 메모 반영으로 계산을 실행하지 않았다.
- 상태·인계: 사용자 업무 결정 **수신 완료**, 트랙 B의 domain 정본·통합 work note 반영 **대기**, 구현·계산 실행 검증 **미완료**. 위 조사 단계의 미확정 기록을 전체 정책/검증 완료로 확대하지 않는다.
- 문서 검증: `git diff --check -- docs/references/mi-model-input-catalog-inventory-2026-09-28.md` 및 이번 수정이 최신 조사 절 안에 한정되는지 검사 **Pass**. 작업 전 본문·검사 결과는 `/private/tmp/track-a-ag-ai-decision-20261008-53ad0cub/`에 보관한다. 입력 파일을 다시 저장하거나 수정하지 않았다.

#### 6. 사람의 결정·트랙 B 반영·다음 조사

| 질문·결정 상태 | 근거·결정 범위 | 현재 상태·잔여 |
| --- | --- | --- |
| H1: 연결된 사용자 입력 변경 시 AG/AI도 갱신 — 사용자 결정 수신 | §5 후속 결정. 영향을 받는 AG/AI를 함께 갱신하며 UNIQUE 5셀/T-135/T-136 등 기존 A는 유지. 빈 BA·없는 신규 키·기존/신규 오류, AF/AH 처리와 비교값은 후속 | 갱신 필요는 해결, B 정본 반영 대기. 계산 세부·실행 성공·모델 소비는 미검증 |
| H2: LNG 신규 입력은 704행부터 빈 슬롯을 쓰는가, 2919행부터 선언 범위를 늘리는가? | §3~4. 아래 행 추가 승인은 유지하며 그 ‘끝’과 빈 슬롯 활성화만 구체화. PNG의 다음 논리 행 1105와 구분 | 빈 슬롯/신규 행 생성 정책 미확정. 넓은 입력 선언만으로 최초 입력을 허용하지 않음 |
| H3: 신규 행의 필수 field와 기존 값 비우기 의미는 무엇인가? | §4. 현재 모든 값이 채워진 사실은 필수 승인 아님. Year/국가/Terminal 후보와 공통 기준표 선행 등록, 계약량 0/빈 값, 행 비활성화/삭제를 구분 | 기존 입력 승인은 유지, 신규 세부 validation·clear·연결 처리는 미확정 |
| H4: CN/CT의 표 밖 2,961개 사전 수식은 고정 보존인가? | §3/5. T-137/T-138 A와 표 밖 범위를 분리. 이 질문에 A 표 자체의 갱신을 포함하지 않음 | 별도 경계 미확정. 빈 수식 결과라는 이유로 변경하지 않음 |

H1/H2는 조사 근거와 함께 사용자에게 질문했다. 이후 H1은 위 사용자 결정으로 갱신 필요를 해결했으며 H2는 답을 받지 않아 미확정이다. H3/H4는 후속 정책 질문 목록이다. 기존 입력·C·아래 행 추가·열 고정, A 및 UNIQUE 결정은 재승인 요청 대상이 아니다. 답변이 오면 기술 관측을 바꾸지 않고 정책 정본과 검증 표본만 담당자가 갱신한다.

> 2026-10-13 후속 상태: 위 표와 문단은 2026-10-08 조사 시점 기록이다. H1의 갱신 필요는 domain 규칙 59에 반영됐다(트랙 B 반영 완료). 같은 날 사용자 결정으로 규칙 59 하위 항목이 추가됐다. F-T-01(Q5 Year SET)은 AG/AI 계산 제외·원본 보존, F-T-02(U5 계약량 SET)는 원본에서 이미 `#N/A`인 셀 보존이다. H2는 T-139 새 계약(F-T-04)을 704행 빈 슬롯부터 채우는 것으로, H3는 그 신규 계약의 Q:U 다섯 필드 필수로 확정됐다. 일반 입력의 후보/키·0/CLEAR 의미, 빈 슬롯 소진 후 범위 확장, H4, 나머지 사례의 계산 세부는 여전히 미확정이다. 정책 정본은 domain §1A.4 H1~H4와 규칙 59다.

트랙 B 반영 입력:

- **사실 확인으로 해결:** T-133/T-139의 최신 헤더/입력 범위·실제 값/빈 슬롯·미리 채운 수식의 구분, 키 중복과 Helper의 식별 한계, AG의 고정 조회 원천·현재 빈 기준값/미일치, A와 함께 있는 구조 변경 위험. Q1~Q5 공통 초안에 표/필드 역할·기존/신규 행·계산/보존 경계의 독립 표현 근거로 사용한다. 기술 ID 저장·API/DDL을 이 reference에서 정하지 않는다.
- **수신한 업무 결정:** H1 / Q5 — 연결된 사용자 입력 변경 시 영향을 받는 AG/AI를 함께 갱신한다. 기존 A·UNIQUE 보존을 유지해 domain의 H1/Q5와 통합 work note에 반영한다. 갱신 필요는 재질문하지 않으며 모델 소비 미검증과 계산 세부 잔여를 구분한다.
- **정책 변경/구체화 후보:** H1의 계산 세부, H2~H4 및 A-03~A-10의 선언 충돌·미확정 구조. 기존 A를 확장/재계산하자는 요구가 생기면 기존 승인과의 변경 후보로 제시한다. INPUT-02A의 A 제외 재계산 실행 후보·실험 수치를 AG/AI의 업무 승인 근거로 대체하지 않는다.
- **검증 인계:** H1의 갱신 결정에 따라 기존/신규 키·계약량 0/빈 값·다른 행 합계·기존 오류/새 오류를 포함한 계산 표본과 A 불변을 확인한다. H2/H3는 승인 후 생성 위치·행 식별 유지·미표시 계산 셀 생성 및 A 불변을 포함한 구조 표본으로 연결한다. 현재 엔진 GO나 구현 착수 판정은 하지 않는다.
- **통합 work note:** 지정 stable path에 B가 update할 내용은 이 절의 Execution Plan, 이번 조사 단위 완료/사용자 결정 수신/세부 미확정 구분, 변경 파일 1개, 아래 검증 명령·결과·실패 수정·잔여 위험이다. 트랙 A는 공용 note를 직접 편집하지 않았다.

남은 조사 순서는 (1) A-03 Regas 선행 등록·연결 확장, (2) A-04 Overwrite/연간 LNG 승인 충돌, (3) A-05/A-06 삭제·선택표·단일 계수, (4) A-07/A-08 OUTPUT·BCInput 계산/보존 경계, (5) A-09의 신규 C/native Table, (6) A-10의 control/조건부서식 요구다. H1 세부 및 H2~H4 답변은 해당 결과에 병행 반영하며 사람 답변을 기다리는 동안 독립 조사를 계속할 수 있다. 이번 단위에서는 이 다음 조사를 실행하지 않았다.

#### 7. 실행·검증 기록과 한계 — B 통합용

- 로컬 근거 폴더: `/private/tmp/track-a-midstream-20261008-yDreqg/`. `probe_midstream.py`, `input-hashes-before.json`, `ledger-selected.json`, `priority-ledger.json`, `midstream-structure.json`, `probe-output.log`에 좌표·집계·선별 근거를 보관했다. 재현 시 스크립트가 지정한 고정 manifest/시트만 읽는다. 업무 값 전체나 계약 상대 명칭은 문서에 나열하지 않았다.
- 실행: 번들 Python `python3 -B /private/tmp/track-a-midstream-20261008-yDreqg/probe_midstream.py`. 검사 4역할 원본/정규화본 hash 대조, Midstream 단일 업무 시트 및 원본 SUMMARY 13~19행 대조, 대장 선정 행 읽기, shared follower 복원·166,271수식 수 및 AG/AH/AI/AJ 각 4,703셀의 단일 상대 행 패턴 확인 **Pass**. 다른 시트의 셀/함수 전수 스캔은 없음.
- 초기 실행 실패: 임시 스크립트 이름 `inspect.py`가 Python 표준 모듈 이름과 겹쳐 openpyxl import가 실패했다. `probe_midstream.py`로 변경 후 동일 검사 통과. 입력 파일은 변경하지 않았다. 이후 키/정확한 범위·행 수식 패턴 검사를 보완해 최종 실행했다.
- 최종 검증: 번들 Python `python3 -B /private/tmp/track-a-midstream-20261008-yDreqg/verify.py` 및 `git diff --check -- docs/references/mi-model-input-catalog-inventory-2026-09-28.md` **Pass**. 입력 13파일(8 XLSM·대장·manifest·함수 reference/JSON·기존 UNIQUE JSON)의 hash가 작업 전과 같고, 원본/정규화본 4역할은 manifest와 일치했다. 새 절을 제거한 본문은 작업 전 Git HEAD와 동일하며 이전 기록/수치 변경 0. 10개 우선 항목의 6열·Q1~Q6 연결, 상세 7표, 상대 링크 7개와 주요 관측 수치를 확인했다. 근거는 같은 폴더의 `verification.json`·`input-hashes-after.json`이다. 원본 XLSM·판별대장·manifest·기존 함수/UNIQUE 산출물은 재저장하지 않았다.
- 미수행: native Excel 재개방/재계산·배열 확장·VBA 실행/동적 소비·모델 실행·계산 PoC/엔진 선정·XLSM 생성·Java build/test·DDL/배포·티켓 상태 변경. 이번 변경은 조사 Markdown뿐이므로 제품 테스트를 실행하지 않았다. 실행 정답과 파일 생성 후 보존은 후속 검증 사항이며 전체 조사/정책 확정/상위 목표 완료로 확대하지 않는다.

## 6.3 Sector 국가·하위부문 목록 원천 — 2026-10-16

### 확인 범위와 결론

고정 [baseline·manifest](mi-model-input-baseline-2026-10-07.md)의 `2026-10-07-delivery-261006-v8-v6` DEMAND를 ZIP/OOXML 읽기 전용으로 대조했다. **workbook에 명시된 목록 원천은 찾았으며, 실제 계산 모델의 허용 목록/소비까지 확인한 것은 아니다.** 아래 값은 원천 관측값이다. **후속 사용자 최종 결정으로 국가/하위부문은 필수 자유입력이다.** [domain 규칙45](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md)가 현재 정책을 소유한다. 이 절의 목록/값은 조사 이력으로만 보존하고 허용 목록이나 발행/seed 연결 근거로 사용하지 않는다. 앞선 K/M 후보·목록/seed 연결안은 철회했다.

| 근거 | 확인 결과 |
| --- | --- |
| DEMAND 원본 / 정규화본 SHA-256 | `73fa29ff6fa19652243479a43754a65076364488120a69657178c8311b6ce0bc` / `ef516b6de2a829c891dbd991ba9f9d626f104dcae2e5ecd8128b1f0ae31c23bd` — manifest 일치 |
| `General` | `xl/worksheets/sheet24.xml`, SHA-256 `e5faff74ddee527b7b0e9175b9020bb1435803abe7c6c009f70b986762a29643`. 원본/정규화본 업무 part byte 동일 |
| 국가 목록 | `General!K2`=`Country`, `K3:K93`의 텍스트 91개. 빈값·중복·수식·앞뒤 공백 각각0 |
| 하위부문 목록 | `General!M2`=`Demand Sub-Sector`, `M3:M13`의 텍스트 11개. 빈값·중복·수식·앞뒤 공백 각각0 |
| Sector 대상 | `DemandOutputOverwrite!D7`=`Country`, `G7`=`Demand Sub-Sector`; 사용자 입력 D8:G507/I8:I507은 원본 `SUMMARY!F27`, H 비우기 안내는 J27. 원본 설명은 목록 범위나 실제 모델 reader를 지정하지 않음 |
| Excel validation/명명·Table | 해당 시트의 validation2개는 **Monthly** O8:O507의 월 목록과 M8:N507의 `#REF!`뿐. Sector D/G 목록 validation 없음. workbook의 defined name에도 K/M 목록 연결 없음, Excel Table part0개 |
| 정적 참조 한계 | DEMAND의 저장 수식 text에서 `DemandOutputOverwrite` 참조 및 `General` K/M 직접 참조를 찾지 못함. 공유 수식의 저장 anchor/text 관측이며 VBA·동적 참조·외부 계산 모델 미사용을 뜻하지 않음 |

### 혼동하기 쉬운 목록과 대조

| 대조 대상 | 관측 / 판단 근거 |
| --- | --- |
| `General!H3:H86` | 84개. K 목록에는 아래 `Other ...` 7개가 더 있음. H만 쓰면 모델 workbook의 집계 지역을 누락 |
| `General!R3:R93`, `AE3:AE93` | 각각91개로 K와 집합 동일, 순서는 다름. R1은 국가/에너지원 dropdown 설명, AC1:AE93은 `Full Country List`. `General!B9` validation은 R3:R86만 참조하므로 이를 Sector 국가 범위로 옮기지 않음 |
| `BCInput!E4:E14493` | 14,490행의 distinct 국가91개가 K 집합과 일치. Sector 기존12행의 `China` 하나로 축소하지 않음 |
| `General!L3:L11`, `BCInput!G4:G14493` | `Demand Sector`9개 집합이 서로 일치. `Industrial`, `Transportation`은 여기에는 있으나 M에는 없음 |
| `General!M3:M13` | `Demand Sub-Sector`11개. 위9개에는 없는 `Hydrogen`, `Non-Hydrogen`, `LNG Bunkering`, `Non-LNG Bunkering` 포함. 당시 Sector G 헤더와 같은 분류명을 관측한 것이며 두 집합의 합집합/교집합으로 새로운 목록을 만들지 않음 |
| `General!N3:N16` | 2018~2031의14개. BCInput의 관측 연도는2018~2035. 둘 다 사용자 확정 연도1000~9999를 축소하는 업무 정책 근거가 아님 |

K에만 더 있는7개는 `Other Africa`, `Other America N`, `Other America S`, `Other Asia`, `Other Australia`, `Other Europe`, `Other Middle East`다. 이들은 원천의 국가 차원 값이며 ISO 국가코드/일반 국가 master로 자동 치환하지 않는다.

인접 `skmip-mi-agent`의 `apps/agents/model_parameter_agent/resources/lut/Demand_Model_LUT_General_value_based_t6.csv`도 확인했다(읽기 시점 HEAD `90f1c18`, 해당 파일 변경 없음, SHA-256 `56d295fca4d3638cae5867065c961f72b488d5df631958ebc7a01be75eab5391`). `Full Country List_Country`91개는 K와 집합 동일하며 AE와 순서도 같다. **이는 파라미터 제안용 LUT의 보조 일치 근거**이며 계산 모델의 Sector 허용값 계약으로 승격하지 않는다. 같은 repo의 `docs/agents/model_parameter_agent/05_MODEL_PARAMETER_AGENT_BASECASE_LUT_PARSING.md`는 override를 기존12 exact slot의 BCM/H 입력·2026~2030 reference로 다룬다. 화면 편집의 신규 행·I 백만톤·확정 연도 정책과 다른 소비 경로이므로 그 제약을 가져오지 않는다.

### 관측한 목록 값과 순서

다음은 K3:K93의 원천 순서이며 각 문자열이 원본 값이다. runtime ID·별도 번역 표시명·국가코드가 아니다.

`Algeria`, `Angola`, `Argentina`, `Australia`, `Austria`, `Azerbaijan`, `Bahrain`, `Bangladesh`, `Belarus`, `Belgium`, `Bolivia`, `Brazil`, `Brunei`, `Bulgaria`, `Canada`, `Chile`, `China`, `Colombia`, `Croatia`, `Czech Republic`, `Denmark`, `Ecuador`, `Egypt`, `Equatorial Guinea`, `Finland`, `France`, `Germany`, `Ghana`, `Greece`, `Hungary`, `India`, `Indonesia`, `Iran`, `Iraq`, `Ireland`, `Israel`, `Italy`, `Japan`, `Jordan`, `Kazakhstan`, `Kuwait`, `Latvia`, `Libya`, `Malaysia`, `Mexico`, `Mozambique`, `Myanmar`, `Netherlands`, `New Zealand`, `Nigeria`, `Norway`, `Oman`, `Other Africa`, `Other America N`, `Other America S`, `Other Asia`, `Other Australia`, `Other Europe`, `Other Middle East`, `Pakistan`, `Papua New Guinea`, `Peru`, `Philippines`, `Poland`, `Portugal`, `Qatar`, `Romania`, `Russia`, `Saudi Arabia`, `Senegal`, `Singapore`, `Slovakia`, `South Africa`, `South Korea`, `Spain`, `Sri Lanka`, `Sweden`, `Switzerland`, `Taiwan (China)`, `Thailand`, `Trinidad and Tobago`, `Tunisia`, `Turkey`, `Turkmenistan`, `UAE`, `Ukraine`, `United Kingdom`, `United States`, `Uzbekistan`, `Venezuela`, `Vietnam`.

M3:M13의 원천 순서는 `Commercial`, `Hydrogen`, `Non-Hydrogen`, `Others`, `Residential`, `LNG Bunkering`, `Non-LNG Bunkering`, `Fuel Gas`, `Heat`, `Losses`, `Power`다.

원천 문자열 배열을 UTF-8 JSON(`ensure_ascii=False`, compact separators, BOM/끝 개행 없음)로 직렬화한 SHA-256은 K가 `e430725468e46223c7e32cbccaeea0e17fe4ce2f31d9a13d642b34c8b5d494b8`, M이 `c7fbcdf4350bcf64e61424bf94a36c30ce855e4113bcc865c66637f7e22b8f19`다. 재추출 일치 확인용 조사 checksum이며 허용 목록/seed 작성에 사용하지 않는다. 이는 공개 sourceVersion/새 DB hash 컬럼 요구가 아니다.

### 확인하지 못한 소비 경로와 명칭 인식 제약 후속

- backend `ScenarioExecutionRegistrar.buildJobArgs` → `ScenarioModelJobDispatcher` → `KubernetesJobServiceImpl.buildContainer`는 네 workbook 경로를 모델 container 인자로 전달한다. 이 경로에는 국가/하위부문 목록이나 Sector parser가 없다. 저장소의 Java/Python·인접 agent 검색에서도 실제 계산 모델의 Sector reader/최종 하위부문 열거 계약은 확보하지 못했다. container 이미지/운영 DB·서버 LUT는 조회하지 않았다.
- 후속은 **모델이 자유입력 명칭을 어떻게 인식하는지** 확인하는 것이다. 정확한 명칭 필요 여부, 언어/별칭·대소문자/공백 처리, 인식하지 못한 명칭의 처리 결과를 해당 모델 reader 또는 모델 담당자의 계약으로 확인한다. 목록 전체 확정·국가별 종속 목록/조합 제약·option snapshot/seed 발행 작업으로 확대하지 않는다.
- 목록 원천 관측 사실은 유지하되 그 목록에서만 선택하게 하는 정책과 발행 후보 추천은 철회했다. 자유입력을 임의 명칭의 모델 인식/반영 성공 보장으로 해석하지 않는다. 기존 연월/중복/I/H/J·선정 F-D-05-APPEND/DELETE의 원문·결과는 유지한다.

원본/정규화본은 재저장·계산하지 않았고 SHA가 조사 전후 manifest와 같다. probe와 상세 결과는 Git 제외 `temp/sector-model-source-2026-10-16/probe.py`, `metadata.json`, `source-summary.json`에 보관했다. 문서 검증/실행 한계는 stable work note에 기록하며 이 reference를 seed 적용/모델 실행 Pass로 사용하지 않는다.

## 7. 계산·패키지 영향

- `OUTPUT` 수식 889개는 모두 cached value가 있고 외부참조 수식은 없다.
- `USCostofSupply` 817개, `PremiumCalc` 64개, `AnnualPriceCalc` 8개가 재계산 후보다.
- `OUTPUT` package에는 external link part 1개와 defined name 18개가 있다. 외부참조 수식이 0개여도
  최종저장에서 해당 package part를 임의 삭제하면 안 된다.
- `OUTPUT`은 작지만 다른 3파일과 같은 source snapshot, Draft, 최종저장, Case 원자성에 포함한다.
- 첨부 개발본 4파일 전체의 외부참조 수식 수 42,394는 기존과 같다. 신규 `OUTPUT`이 외부참조 계산 위험을
  늘리지는 않지만 계산 후보 family 6개와 889셀을 `INPUT-02A` 검증 범위에 추가해야 한다.

## 8. 설계 반영 입력

- 필수 파일 역할 set은 `DEMAND/SUPPLY/TRADE/OUTPUT`이며 각 역할이 정확히 1개여야 한다.
- `SUMMARY`와 `플랫폼 입력 안내`는 개발 전용이므로 ACTIVE catalog/runtime template에 등록하지 않는다.
- 운영 기준 정규화본의 hidden sheet는 화면·계산 catalog에서 제외하되 최종 XLSM materialization에서는 source snapshot 그대로 복사한다.
- 현재 개발본에서 개발 전용 시트를 제거한 runtime sheet는 70개다. 정규화본을 만들 때 hash·sheet·cell 구조를 고정한다.
- template version, source snapshot, Draft, 최종 materialization, 새 Case 파일 연결은 4개 파일을 원자적으로 다룬다.
- 기존 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT`을 보완하거나 4파일 template에 mapping하지 않는다.
  cutover 전에 case master를 soft delete해 화면과 active-resource API에서 제외한다. 파일·실행이력·결과의 physical delete는 하지 않는다.
- 기존 case 생성·Scenario 실행·MI Base 지정·Job args는 현재 3개 역할만 검사하거나 전달한다.
  구현 전에 `OUTPUT` 저장 경로와 model job 전달 계약을 함께 변경해야 한다.
- 2026-09-16의 23개 화면 후보에 `OUTPUT` 업무 시트 5개가 추가되어 최신 화면 후보 sheet는 28개다.
  실제 활성 table/field는 `INPUT-03` 승인 대상이다.

## 9. 재현 방법

```bash
python3 -B scripts/inventory-model-input-catalog.py --compact <latest four XLSM paths>
python3 -B scripts/inventory-model-input-formulas.py --output <report.json> <latest four XLSM paths>
```

두 스크립트는 cell 원문 값과 formula 문자열을 출력하지 않는다. 최신 파일은 저장소에 복사하거나 커밋하지 않는다.
