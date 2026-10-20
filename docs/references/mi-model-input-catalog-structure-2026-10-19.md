# 최신 모델 입력 카탈로그 구조 SQL 조사 — 2026-10-19

## Document Control

- status: Complete — 읽기 구조 조사·SQL 초안 준비 완료, DB 적용/발행 아님
- canonical for: Reference only — 최신 기준본의 시트·표·조회 좌표 확인 근거와 보류 목록
- not canonical for: 업무 정책, API, schema, 계산·쓰기 승인
- owner: BE (C)
- source documents:
  - [기준본 설명](mi-model-input-baseline-2026-10-07.md), [manifest](mi-model-input-baseline-2026-10-07-manifest.json)
  - manifest의 normalizedPath 4파일 및 동일 해시 원본 snapshot의 SUMMARY (조사 근거로만 사용)
  - [기존 전수 조사](mi-model-input-catalog-inventory-2026-09-28.md), [이전 3파일 조사](mi-model-input-catalog-inventory.md)
  - [SUMMARY·보존 분류 대조](mi-model-input-summary-preservation-review-2026-10-08.md), [판별 기록](model-input/README.md)
- depends on:
  - [domain](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md) §3.3, 규칙3·39~44·45·49~58, §6.1
  - [DDL](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md) §6.2·§10B 및 [reader DDL](../model-input-draft-read-ddl-draft.sql) §2~3
  - [source-of-truth governance](../conventions/source-of-truth-governance.md)
- source work note: [독립 SQL 작업 기록](../work-notes/2026/2026-10-19-model-input-catalog-structure-sql.md)
- supersedes: 없음. v3 기록·기존 smoke SQL·발행 catalog를 수정/대체하지 않음
- last synced: 2026-10-19
- open questions: 아래 7항목의 조회 표현, 199열의 필드 타입, 표별 쓰기·계산/구조 변경 정책, 구조 hash 방식, 실제 DB 적용 검증

## 결과와 범위

[구조 데이터 SQL](../model-input-catalog-structure-data-draft.sql)은 **새 DRAFT 양식1개 → 파일 역할4개 → 유지 시트70개 → 확인된 조회 표92개**를 등록하는 초안이다. 개발 전용 시트와 미표시 표는 조회 표로 만들지 않는다. 기준본 SUMMARY의 표시 항목99개 중7개는 아래 사유로 SQL의 표 INSERT에서 제외했다. 70개 시트 metadata 등록은70개 입력 표 생성이 아니다.

구조가 읽힌다는 사실과 사용자가 수정해도 된다는 결정은 별개다. 일반 표의 `capabilities`와 포함 필드의 `policyStatus`는 모두 `INCOMPLETE`, 확정 읽기 전용인 SUPPLY `LNG Summary`는 `NOT_ALLOWED`다. 수정 가능 범위인 `tmd_mdl_inptmodscopd`는 등록하지 않는다. `READY`·후보 목록·기본 허용·새 reader 속성을 만들지 않았다. 이 부분 DRAFT는 발행 후보 완성본이 아니다.

## 기준 파일과 전체 시트 확인

`baselineId=2026-10-07-delivery-261006-v8-v6`. 정규화본과 source snapshot의 SHA-256을 manifest와 직접 대조했고, 유지 worksheet70개 각각의 part 해시·이름·순서·상태·셀/수식 수도 대조했다. 조사 전후 네 정규화본과 네 source snapshot 모두 해시가 같다. SUMMARY는 해시가 맞는 source snapshot에서만 읽었다.

| 역할 | 정규화 SHA-256 | 유지 시트 | 수식 |
| --- | --- | ---: | ---: |
| DEMAND | `ef516b6de2a829c891dbd991ba9f9d626f104dcae2e5ecd8128b1f0ae31c23bd` | 25 | 699,298 |
| SUPPLY | `eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36` | 23 | 460,803 |
| TRADE | `b1211662aa00d49d9f03c1d732acd495cf99446ede69abde34565007b3819a31` | 17 | 702,776 |
| OUTPUT | `69742cc9c6fba170324c09c21986574a390c741ba26d371acdce570aeaac28ce` | 5 | 889 |

전체70개 중 visible56개·hidden14개다. 빈 구분 탭4개와 hidden14개는 metadata/원본 보존 대상이며 표를 등록하지 않는다. 미표시/백업/참조 시트의 값·수식·package는 이번에 변경하지 않았다. 원본 SUMMARY의 전체 미표시93개 항목(판별대장94행)은 규칙57의 기존 제외를 유지하며, 워크북 밖 서버 LUT/외부 결과 설명3개도 새 시트로 만들지 않는다.

| 역할 / 순서 | 유지 시트 | 상태 | 이번 SQL 표 수 / 보류 수 | 분류 근거 |
| --- | --- | --- | ---: | --- |
| DEMAND / 1 | `Input Adjustment ->` | visible | 0 / 0 | 빈 구분 탭, 화면 제외 |
| DEMAND / 2 | `GDPPopulationAdjustment` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 3 | `ElectrificationRateAdjustment` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 4 | `PowerMixAdjustment` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 5 | `CoalGasAdjustment` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 6 | `WeatherAdjustment` | visible | 4 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 7 | `PMIAdjustment` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 8 | `HydrogenAdjustment` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 9 | `LNGBunkeringAdjustment` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 10 | `Scenario Tracker` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| DEMAND / 11 | `DemandOutputOverwrite` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 12 | `기준정보 ->` | visible | 0 / 0 | 빈 구분 탭, 화면 제외 |
| DEMAND / 13 | `BCInput` | visible | 8 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 14 | `MacroParametersLogicInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 15 | `PMIInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 16 | `PMILogicInput` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 17 | `WeatherLogicInput` | visible | 4 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 18 | `WeatherInput` | visible | 3 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 19 | `LNGBunkeringInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 20 | `LNGBunkeringLogicInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 21 | `HydrogenInput` | visible | 1 / 1 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 22 | `LNG Demand` | visible | 0 / 2 | 표시 표와 미표시 참조/계산 영역을 분리 |
| DEMAND / 23 | `General` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| DEMAND / 24 | `Sheet2_DISCARDAFTUSE` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| DEMAND / 25 | `CubeDump_DISCARDAFTUSE` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 1 | `Input Upstream ->` | visible | 0 / 0 | 빈 구분 탭, 화면 제외 |
| SUPPLY / 2 | `ProductionAdjustment` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 3 | `Input Midstream ->` | visible | 0 / 0 | 빈 구분 탭, 화면 제외 |
| SUPPLY / 4 | `{USA}Hurricanes` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 5 | `LiqUtilAdjustment` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 6 | `LiqProdAdjustment` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 7 | `RegasAdjustment` | visible | 2 / 1 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 8 | `PipelineAdjustment` | visible | 0 / 1 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 9 | `BCProduction` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 10 | `BCHurricanes` | visible | 4 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 11 | `BCLiquefaction` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 12 | `BCRegasification` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 13 | `BCPipeline` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 14 | `LNG Summary` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| SUPPLY / 15 | `LiqUtilAdjustment_backup` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| SUPPLY / 16 | `LiqProdAdjustment_backup` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| SUPPLY / 17 | `BCLiquefaction_backup` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| SUPPLY / 18 | `{USA}RigsLogic` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 19 | `{China}PolicyLogic` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 20 | `{USA}HurricanesLogic` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 21 | `RegasLogic` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 22 | `PipelineLogic` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| SUPPLY / 23 | `MonthlyAdjProduction` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| TRADE / 1 | `LNG_TradeRestrictionInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 2 | `PNG_TradeRestrictionInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 3 | `Sheet1` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| TRADE / 4 | `MidstreamContractInput` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 5 | `ShippingCostInput` | visible | 11 / 1 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 6 | `MidstreamCapacityInput` | visible | 3 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 7 | `UpstreamAdjLogicInput` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 8 | `DemandAdjLogicInput` | visible | 3 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 9 | `CostInput` | visible | 6 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 10 | `4.5 PriceModelInput` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 11 | `LNGExportRestictionInput` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| TRADE / 12 | `backupLNG_TradeRestrictionInput` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| TRADE / 13 | `backup_MidstreamContractInput` | visible | 0 / 0 | SUMMARY 미표시·참조/계산/백업 원본 보존 |
| TRADE / 14 | `Industry research` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| TRADE / 15 | `net exporter` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| TRADE / 16 | `pipeline cap` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| TRADE / 17 | `pipeline contract` | hidden | 0 / 0 | hidden 원본 보존, 화면/계산 입력 제외 |
| OUTPUT / 1 | `USCostofSupply` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| OUTPUT / 2 | `BCPrices` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| OUTPUT / 3 | `PremiumCalc` | visible | 2 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |
| OUTPUT / 4 | `AnnualPriceCalc` | visible | 0 / 1 | 표시 표와 미표시 참조/계산 영역을 분리 |
| OUTPUT / 5 | `PriceSeasonality` | visible | 1 / 0 | 표시 표와 미표시 참조/계산 영역을 분리 |

## 등록한 표와 조회 범위

좌표는 Excel의1-based 숫자를 SQL에 저장한다. 범위는 최신 source SUMMARY의 명시 영역·실제 정규화본 헤더/본문·정본의 제외 열을 대조했다. used range·색·필터로 끝행을 추정하지 않았다. 본문에는 명시된 빈 입력 슬롯도 포함되며, 현재 값이 있는 행 수와 다를 수 있다. 마지막 행/열이 실제 데이터 끝이라는 뜻이 아니다.

표 안의 `identityColumns`는 **원천 행이 존재하는지 검사하는 비수식 열**이다. 업무 중복 금지 키가 아니다. 실제 안정 행ID는 현행 `ModelInputDraftService.create`가 원본 행마다 UUID로 발급한다. 이름/국가/연도가 중복된다고 행을 합치거나 삭제하지 않는다. 이 조사에서 모든 선택 identity 열의 본문에 수식이 없음을 확인했고 실제 reader의 활성 행 판별과226,102행이 일치했다. 원천에서 모두 빈 슬롯은 rowId를 미리 만들지 않는다.

`tableId = role/sht_nm/inpt_tbl_nm`이다. `inpt_tbl_nm`에는 아래 최신 영문 표 이름을 그대로 사용한다. `fieldKey`는 헤더를 정규화한 표 안의 유일한 영문 키이며, 숫자 기간/헤더 없음/중복 헤더에는 `column<Excel열>` 또는 열번호 접미사를 사용한다. SQL의 `@c_qNNN`이 정확한692개 key→columnNumber/dataType 대응을 소유한다. 현행 계약의 키 정규식과 타입3종만 쓴다. 예: `OUTPUT/BCPrices/Base Case Prices`의 `year→B(INTEGER)`, `hh→C`, `eastAsiaSpot→D`, `ttf→E(DECIMAL)`.

| 역할 | 표 이름 / 시트 | baseline 범위 | headerRows / 본문 | 조회 열 / identity 열 | SQL 정의 | 미정 필드 열 |
| --- | --- | --- | --- | --- | --- | --- |
| DEMAND | GDP Adjustment / `GDPPopulationAdjustment` | `B6:F2000` | 6 / 12:2000 | B:C, E:F / B:C, E:F | `@c_q001` | E |
| DEMAND | Population Adjustment / `GDPPopulationAdjustment` | `J6:M2000` | 6 / 12:2000 | J:K, M / J:K, M | `@c_q002` | J:K, M |
| DEMAND | Electrification Rate Adjustment / `ElectrificationRateAdjustment` | `B6:F2000` | 6 / 12:2000 | B:D, F / B:D, F | `@c_q003` | B:D, F |
| DEMAND | Annual Power Mix Adjustment / `PowerMixAdjustment` | `B6:F7989` | 6 / 12:7989 | B:D, F / B:D, F | `@c_q004` | 없음 |
| DEMAND | Monthly Power Mix Adjustment / `PowerMixAdjustment` | `J6:O7989` | 6 / 12:7989 | J:M, O / J:M, O | `@c_q005` | J:M, O |
| DEMAND | Coal Futures Price Adjustment / `CoalGasAdjustment` | `B6:E2000` | 6 / 12:2000 | B:C, E / B:C, E | `@c_q006` | B:C, E |
| DEMAND | Gas Futures Price Adjustment / `CoalGasAdjustment` | `H6:K2000` | 6 / 12:2000 | H:I, K / H:I, K | `@c_q007` | H:I, K |
| DEMAND | Country HDD and CDD Adjustment / `WeatherAdjustment` | `B6:G7997` | 6 / 12:7997 | B:E, G / B:E, G | `@c_q008` | B:E, G |
| DEMAND | Country Temperature Adjustment / `WeatherAdjustment` | `K6:O7997` | 6 / 12:7997 | K:M, O / K:M, O | `@c_q009` | 없음 |
| DEMAND | Regional HDD and CDD Adjustment / `WeatherAdjustment` | `T6:X7997` | 6 / 12:7997 | T:X / T:X | `@c_q010` | T:X |
| DEMAND | Regional Temperature Change Adjustment / `WeatherAdjustment` | `AB6:AE7997` | 6 / 12:7997 | AB:AE / AB:AE | `@c_q011` | AB:AE |
| DEMAND | Monthly PMI Adjustment / `PMIAdjustment` | `B6:F8000` | 6 / 13:8000 | B:D, F / B:D, F | `@c_q012` | 없음 |
| DEMAND | PMI Reference Country Adjustment / `PMIAdjustment` | `J6:L8000` | 6 / 12:8000 | J, L / J, L | `@c_q013` | J, L |
| DEMAND | Hydrogen Production Adjustment / `HydrogenAdjustment` | `B6:E8000` | 6 / 12:8000 | B:C, E / B:C, E | `@c_q014` | B:C, E |
| DEMAND | LNG Bunkering Vessel Count Adjustment / `LNGBunkeringAdjustment` | `B6:E2000` | 6 / 12:2000 | B:C, E / B:C, E | `@c_q015` | B:C, E |
| DEMAND | LNG Bunkering Fuel Consumption Adjustment / `LNGBunkeringAdjustment` | `H6:K2000` | 6 / 12:2000 | H:I, K / H:I, K | `@c_q016` | H:I, K |
| DEMAND | Demand Model Manual Overwrite Sector Input / `DemandOutputOverwrite` | `D6:K507` | 6,7 / 8:507 | D:G, I / D:G, I | `@c_q017` | I |
| DEMAND | Demand Model Manual Overwrite Monthly Input / `DemandOutputOverwrite` | `M6:S507` | 6,7 / 8:507 | M:O, Q / M:O, Q | `@c_q018` | M:O, Q |
| DEMAND | RE Base Case Gas Demand Data by Sector / `BCInput` | `C2:N14493` | 2,3 / 4:14493 | C:N / C:N | `@c_q019` | 없음 |
| DEMAND | RE Base Case GDP and Population Data / `BCInput` | `Q2:V2368` | 2,3 / 4:2368 | Q:V / Q:V | `@c_q020` | 없음 |
| DEMAND | Power Generation Data / `BCInput` | `Z2:AI100803` | 2,3 / 4:100803 | Z:AD, AH:AI / Z:AD, AH:AI | `@c_q021` | 없음 |
| DEMAND | Power Generation Utilisation Rate / `BCInput` | `AK2:AN1125` | 2,3 / 4:1125 | AK:AN / AK:AN | `@c_q022` | 없음 |
| DEMAND | Car Sales for Electrification Input / `BCInput` | `AQ2:AU13548` | 2,3 / 4:13548 | AQ:AU / AQ:AU | `@c_q023` | 없음 |
| DEMAND | Base Case LNG Bunkering Demand / `BCInput` | `AW2:AY252` | 2,3 / 4:252 | AW:AY / AW:AY | `@c_q024` | 없음 |
| DEMAND | Base Case Hydrogen Demand / `BCInput` | `BB2:BD290` | 2,3 / 4:290 | BB:BD / BB:BD | `@c_q025` | 없음 |
| DEMAND | Coal and Gas Futures Pricing / `BCInput` | `BG2:BK28` | 2,3 / 4:28 | BG:BK / BG:BK | `@c_q026` | 없음 |
| DEMAND | Selected Macro Parameter Intensities / `MacroParametersLogicInput` | `I7:P176` | 7,8 / 9:176 | I:P / I:P | `@c_q027` | 없음 |
| DEMAND | Base Case PMI Input / `PMIInput` | `V7:AK1016` | 7,8 / 9:1016 | V:AK / V:AK | `@c_q028` | AH:AK |
| DEMAND | User-Adjusted PMI Intensity Parameter / `PMILogicInput` | `B7:E92` | 7,8 / 9:92 | B:E / B:E | `@c_q029` | E |
| DEMAND | Base Case PMI Intensity Parameter / `PMILogicInput` | `G7:J92` | 7,8 / 9:92 | G:J / G:J | `@c_q030` | 없음 |
| DEMAND | User-Adjusted HDD-Gas Intensity Input / `WeatherLogicInput` | `B7:E92` | 7,8 / 9:92 | B:E / D:E | `@c_q031` | E |
| DEMAND | User-Adjusted CDD-Gas Intensity Input / `WeatherLogicInput` | `G7:J92` | 7,8 / 9:92 | G:J / I:J | `@c_q032` | J |
| DEMAND | Base Case HDD-Gas Intensity Input / `WeatherLogicInput` | `L7:O92` | 7,8 / 9:92 | L:O / N:O | `@c_q033` | 없음 |
| DEMAND | Base Case CDD-Gas Intensity Input / `WeatherLogicInput` | `Q7:T92` | 7,8 / 9:92 | Q:T / S:T | `@c_q034` | 없음 |
| DEMAND | Base Case HDD Input / `WeatherInput` | `BV7:CK1016` | 7,8 / 9:1016 | BV:CK / BV:CK | `@c_q035` | CH:CK |
| DEMAND | Base Case CDD Input / `WeatherInput` | `CN7:DC1016` | 7,8 / 9:1016 | CN:DC / CN:DC | `@c_q036` | CZ:DC |
| DEMAND | Base Case Temperature / `WeatherInput` | `EI7:EX1016` | 7,8 / 9:1016 | EI:EX / EI:EX | `@c_q037` | EU:EX |
| DEMAND | Base Case LNG Bunkering Input / `LNGBunkeringInput` | `H7:K56` | 7,8 / 9:56 | H:K / H:K | `@c_q038` | 없음 |
| DEMAND | LNG Bunkering Share by Country / `LNGBunkeringLogicInput` | `B6:Q98` | 6,7 / 8:98 | B:Q / B:Q | `@c_q039` | M:Q |
| DEMAND | Base Case Hydrogen Production Input / `HydrogenInput` | `F7:H736` | 7,8 / 9:736 | F:H / F:H | `@c_q040` | 없음 |
| SUPPLY | PNG Production Adjustment / `ProductionAdjustment` | `I7:M200` | 7 / 8:200 | I:K, M / I:K, M | `@c_q041` | I:K, M |
| SUPPLY | Monthly Hurricane Adjustment / `{USA}Hurricanes` | `B7:D200` | 7 / 13:200 | B:D / B:D | `@c_q042` | B:D |
| SUPPLY | Monthly LNG Train Utilization / `LiqUtilAdjustment` | `B7:CL407` | 7,8,10 / 11:407 | B:CL / B:CL | `@c_q043` | 없음 |
| SUPPLY | Monthly LNG Train Production / `LiqProdAdjustment` | `B7:CL407` | 7,8,10 / 11:407 | B:CL / B:CL | `@c_q044` | G:CL |
| SUPPLY | Monthly Regas Capacity Adjustment / `RegasAdjustment` | `B8:DK382` | 8,9,10 / 11:382 | B:DK / H:AE | `@c_q045` | F |
| SUPPLY | Monthly Regas Capacity by Country / `RegasAdjustment` | `G385:DK444` | 385,386 / 387:444 | G:DK / G | `@c_q046` | 없음 |
| SUPPLY | Annual Gas Production Baseline / `BCProduction` | `C7:K24779` | 7 / 8:24779 | C:F, H:K / C:F, H:K | `@c_q047` | 없음 |
| SUPPLY | Hurricane Impact on Upstream Assets / `BCHurricanes` | `B7:G11` | 7 / 8:11 | B:G / B:F | `@c_q048` | 없음 |
| SUPPLY | Hurricane Affected Provinces / `BCHurricanes` | `I7:K13` | 7 / 8:13 | I:K / I, K | `@c_q049` | J |
| SUPPLY | Hurricane Impact on Liquefaction Plants / `BCHurricanes` | `B14:G19` | 14,15 / 16:19 | B:G / B:F | `@c_q050` | 없음 |
| SUPPLY | Hurricane Affected LNG Plants / `BCHurricanes` | `I15:K41` | 15 / 16:41 | I:K / I, K | `@c_q051` | J |
| SUPPLY | Monthly LNG Train Baseline / `BCLiquefaction` | `B7:N52411` | 7 / 8:52411 | B:L, N / B:L, N | `@c_q052` | 없음 |
| SUPPLY | Annual Regas Capacity Baseline / `BCRegasification` | `B7:Q379` | 7 / 8:379 | B:Q / B:Q | `@c_q053` | 없음 |
| SUPPLY | Annual Pipeline Capacity Baseline / `BCPipeline` | `B7:F70` | 7 / 8:70 | B:F / B:F | `@c_q054` | 없음 |
| SUPPLY | LNG Production Reconciliation / `LNG Summary` | `C7:Y531` | 7,8 / 9:531 | C:Y / C:Y | `@c_q055` | 없음 |
| TRADE | LNG Restriction / `LNG_TradeRestrictionInput` | `B5:K509` | 5 / 6:509 | B:K / B:K | `@c_q056` | C, K |
| TRADE | PNG Restriction / `PNG_TradeRestrictionInput` | `B5:J509` | 5 / 6:509 | B:J / B:J | `@c_q057` | B:J |
| TRADE | Forecasted PNG Contract Volume (Imports) / `MidstreamContractInput` | `A2:D1104` | 2,3,4 / 5:1104 | A:D / A:D | `@c_q058` | 없음 |
| TRADE | LNG Contract Volumes / `MidstreamContractInput` | `Q2:U2918` | 2,3,4 / 5:2918 | Q:U / Q:U | `@c_q059` | 없음 |
| TRADE | Shipping Route Parameters / `ShippingCostInput` | `AC2:AM375` | 2,3 / 4:375 | AC:AE, AH:AI, AL:AM / AC:AE, AH:AI, AL:AM | `@c_q060` | 없음 |
| TRADE | Importer Classification / `ShippingCostInput` | `AP2:AQ92` | 2,3 / 4:92 | AP:AQ / AP:AQ | `@c_q061` | 없음 |
| TRADE | Exporter List / `ShippingCostInput` | `AT2:AT29` | 2,3 / 4:29 | AT / AT | `@c_q062` | 없음 |
| TRADE | Atlantic Charter Cost / `ShippingCostInput` | `B15:F24` | 15,16 / 17:24 | B, D, F / B, D, F | `@c_q063` | D, F |
| TRADE | Fuel Cost - IFO380 / `ShippingCostInput` | `B26:F35` | 26,27 / 28:35 | B:F / B:D, F | `@c_q064` | D, F |
| TRADE | Suez Canal Cost / `ShippingCostInput` | `B37:F46` | 37,38 / 39:46 | B:F / B:D, F | `@c_q065` | D, F |
| TRADE | Panama Canal Cost / `ShippingCostInput` | `B48:F57` | 48,49 / 50:57 | B:F / B:D, F | `@c_q066` | D, F |
| TRADE | Suez Canal Disruption / `ShippingCostInput` | `B59:D68` | 59,60 / 61:68 | B:D / B:C | `@c_q067` | 없음 |
| TRADE | Panama Canal Disruption / `ShippingCostInput` | `B70:D79` | 70,71 / 72:79 | B:D / B:C | `@c_q068` | 없음 |
| TRADE | Technical Parameters / `ShippingCostInput` | `B81:E83` | 81,82 / 83:83 | B:E / B, E | `@c_q069` | 없음 |
| TRADE | Panama Spot Capacity / `ShippingCostInput` | `B86:E90` | 86,87 / 88:90 | B:E / B:E | `@c_q070` | 없음 |
| TRADE | PNG Infrastructure / `MidstreamCapacityInput` | `B1:Q207` | 1,2,3 / 11:207 | B, D, G, J:Q / B, D, G, J:Q | `@c_q071` | 없음 |
| TRADE | Pipeline Import Allocation Shares / `MidstreamCapacityInput` | `DX1:EB186` | 1,2,3 / 4:186 | DX:EB / DX:EB | `@c_q072` | 없음 |
| TRADE | Nord Stream Importer Allocation / `MidstreamCapacityInput` | `FB1:FD8` | 1,2,3 / 4:8 | FB:FD / FB:FD | `@c_q073` | 없음 |
| TRADE | Country Supply Adjustment / `UpstreamAdjLogicInput` | `I1:P674` | 1,2 / 3:674 | I:J, P / I:J, P | `@c_q074` | P |
| TRADE | Excess LNG to PNG Countries / `UpstreamAdjLogicInput` | `T1:T40` | 1,2,3 / 4:40 | T / T | `@c_q075` | 없음 |
| TRADE | Premium Market Countries / `DemandAdjLogicInput` | `A1:A16` | 1,2 / 3:16 | A / A | `@c_q076` | 없음 |
| TRADE | Demand Adjustment Parameters / `DemandAdjLogicInput` | `C1:F3` | 1 / 2:3 | C:F / C:F | `@c_q077` | 없음 |
| TRADE | Demand Adjustment Bands / `DemandAdjLogicInput` | `H1:J7` | 1,2 / 3:7 | H:J / H:J | `@c_q078` | I |
| TRADE | Regasification Tariff / `CostInput` | `A1:D379` | 1,2 / 3:379 | A:D / A:D | `@c_q079` | D |
| TRADE | Liquefaction Cost / `CostInput` | `H1:K233` | 1,2 / 3:233 | H:K / H:K | `@c_q080` | K |
| TRADE | LNG Upstream Cost / `CostInput` | `O1:R227` | 1,2 / 3:227 | O:R / O:R | `@c_q081` | R |
| TRADE | Non-LNG Upstream Cost / `CostInput` | `V1:Y615` | 1,2 / 3:615 | V:Y / V:Y | `@c_q082` | Y |
| TRADE | Pipeline Cost / `CostInput` | `AC1:AG1554` | 1,2 / 3:1554 | AC:AG / AC:AG | `@c_q083` | AF:AG |
| TRADE | LNG Project SRMC / `CostInput` | `AK1:AN519` | 1,2 / 3:519 | AK:AN / AK:AN | `@c_q084` | 없음 |
| TRADE | JKM - Base Case / `4.5 PriceModelInput` | `AI2:AK35` | 2,3 / 4:35 | AI:AK / AI:AK | `@c_q085` | 없음 |
| TRADE | TTF - Base Case / `4.5 PriceModelInput` | `AM2:AO59` | 2,3 / 4:59 | AM:AO / AM:AO | `@c_q086` | 없음 |
| TRADE | LNG Export Restriction / `LNGExportRestictionInput` | `B2:E200` | 2,3 / 4:200 | B:E / B:E | `@c_q087` | B:E |
| OUTPUT | US Cost of Supply / `USCostofSupply` | `B6:L816` | 6,7 / 8:816 | B:L / B:L | `@c_q088` | L |
| OUTPUT | Base Case Prices / `BCPrices` | `B6:E21` | 6,7 / 8:21 | B:E / B:E | `@c_q089` | 없음 |
| OUTPUT | PremiumCalc TTF / `PremiumCalc` | `B35:H47` | 35,36,37,38 / 39:47 | B:C / B:C | `@c_q090` | 없음 |
| OUTPUT | PremiumCalc East Asia Spot / `PremiumCalc` | `B53:H61` | 53,54,55,56 / 57:61 | B:C / B:C | `@c_q091` | 없음 |
| OUTPUT | Price Seasonality / `PriceSeasonality` | `B1:O4` | 1 / 2:4 | B:O / B:O | `@c_q092` | 없음 |

조회891열 중692열만 실제 저장값의 타입과 헤더 대응을 확인해 `fields`에 넣었다. 나머지199열은 값이 없거나 숫자·문자·오류가 섞여 **필드 타입을 확정하지 않았고** 조회 열로만 유지한다. 이는 표 범위 불확정과 다르며 미정열 목록은 위 마지막 열에 있다. 헤더·셀 원시값은 그대로 reader가 반환한다. 모든 필드의 CREATE/SET/CLEAR·제약·초기값과 계산/보존 승인은 별도 후속이다. 화면의 기존 수식 열에 필드가 존재해도 쓰기 권한은 생기지 않는다.

특히 다음 최신 차이를 반영했다.

- OUTPUT `US Cost of Supply`: `B6:L816`, 입력 선언은 `B8:L816`. v3 `B7:K818`을 복사하지 않았다. L열 입력/수식 대응 A-07은 미결정이며 필드 타입·쓰기를 확정하지 않는다.
- OUTPUT Premium: TTF는 본문39:47, East Asia Spot은57:61, 둘 다 조회는B:C만. D:H와62:65는 화면에 넣지 않았다.
- DEMAND 일반 조정: header6 / body12부터, Monthly PMI만13부터다. 깨진 선행 수식 행과 Base/Helper 열을 빼고, Overwrite는H/P의 BCM 입력·J/R 계산열을 projection에서 제외했다. Sector 국가·하위부문은 자유입력 결정 그대로이며 옵션seed는 없다.
- DEMAND BCInput의 AE:AG·X·AO·AZ·BE 등 미표시 Helper는 해당 표의 정본·SUMMARY에 맞춰 제외했다. Power Generation은 Z:AD,AH:AI만 조회한다.
- TRADE Country Supply Adjustment는 I/J/P만 조회한다. Shipping Route는 AC:AE,AH:AI,AL:AM, 계약표는 A:D / Q:U만 조회한다.
- 행/열 추가 flag는 최신 SUMMARY·판별 기록의 승인된 구조 방향을 반영하고 Regas 직접 표의 행/열은 규칙51대로 N이다. 구조 추가 flag의 Y는 완성된 write 정책/계산 구현 승인이 아니다. 기간 열 추가는 현행 schemaVersion1 capability에 없는 별도 연계사항이다.

## SQL에서 제외한 표시 항목7개

구조 조사 자체를 멈추지 않고 다음 항목만 표 INSERT에서 보류했다. source의 영문 이름·좌표를 기준으로 재조사한다. 새 정책이나 JSON 필드를 임의로 추가하지 않았다.

| 역할 / SUMMARY 행 | 표 / 시트 | source 범위 | 보류 사유 / 필요한 확인 |
| --- | --- | --- | --- |
| DEMAND / 29 | LNG Demand Mode / `LNG Demand` | `B1:C1` | 선택 control과 연간 표 연결/옵션 정의를 발명하지 않음 |
| DEMAND / 30 | Annual LNG Demand Reference / `LNG Demand` | `B3:N109` | 국가행·소계·TOTAL의 분리 및 기간 헤더 편집/부속 control 연결 확인 필요 |
| DEMAND / 73 | Hydrogen Gas Conversion Factor / `HydrogenInput` | `Q7:S8` | 가로 고정 라벨·단일 입력 셀의 field binding/조회 표현 확인 필요; 지정 헤더 영역에 실제 저장값 확인 불가 |
| OUTPUT / 19 | AnnualPriceCalc Spread / `AnnualPriceCalc` | `B49:G49` | 같은 행의 고정 라벨/하한·상한 값에 대한 field binding 확인 필요 |
| SUPPLY / 18 | Annual Regas Capacity by Country / `RegasAdjustment` | `DM10:DV382` | 본문 존재 판별에 쓸 비수식 원천 열을 확인하지 못함; 비수식 식별 열이 빈 수식/저장값 행이 있어 원천 행 판별을 별도로 확정해야 함 |
| SUPPLY / 20 | Monthly Pipeline Capacity Adjustment / `PipelineAdjustment` | `B7:H200` | 비수식 식별 열이 빈 수식/저장값 행이 있어 원천 행 판별을 별도로 확정해야 함 |
| TRADE / 20 | Pacific Charter Cost / `ShippingCostInput` | `B2:F13` | C3 단일 상승률과 B/D/F 본문 혼합 구조: 별도 control 또는 grid 분리 확인 필요 |

연간 Regas 집계는 DM:DV 본문 전체가 수식이라 현행 Draft 생성의 `identityColumns`에 넣을 수 없다. 국가별 월간 집계는 실제 비수식 국가 열 G를 확인해 등록했다. Pipeline은 B:D의 수식 기반 식별과 H만 비어 있는 행을 원천 행으로 어떻게 다룰지 남아 있다. 임의 row key나 수식 식별 허용으로 우회하지 않았다. 고정 셀형 Spread·Hydrogen 환산계수는 조회 표현/필드 결합을 먼저 확인해야 한다. C1 선택 control은 원본 `Dynamaic` 오타/화면·저장 선택지 결정까지 포함해 규칙50을 따른다.

## 현행 reader 오프라인 검증

현재 소스의 `InputReadDefinition`, `InputDraftJson`, `InputWorkbookReader`, `InputDraftException`과 `ResultCode`를 Java17로 별도 임시 디렉터리에 컴파일했다. DB/네트워크와 연결되는 타입3개(`FileRef` DTO, storage, temp-file entry)만 차단용 stub을 두고 reader의 `readLocal`을 호출했다. 제품 parser/reader 소스는 수정하지 않았고 검증 전후 소스 해시도 확인했다. Spring context·서버·전체 앱·Gradle build는 실행하지 않았다.

| 검사 | 실제 결과 |
| --- | --- |
| normalized/source snapshot8개 SHA-256 / 유지 worksheet70개 part hash | 모두 manifest와 일치, 전후 불변 |
| 현재 parser의 schemaVersion1 / 허용 속성·순서·fieldKey·정책 상태 | 92정의·692필드 통과 |
| 존재하지 않는 reader 필드 주입 | 92정의 모두 거부 |
| 제품reader의 선택 header/body | 316,506 worksheet행, 1,980,188 저장값 셀, 본문 수식40,464셀 판독 |
| 제품reader의 원천 행 존재 판별 | 226,102행, Python 조사와 일치; identity에 수식0 |
| tableId/fieldKey/표순서 중복·부모 시트 존재/visible·DDL 좌표/본문 포함·1MiB 제한 | 통과 |
| SQL literal/문장 경계·실제 JSON92개 재추출·허용 INSERT대상4개·DML 범위 | 통과; 끝은ROLLBACK, 고정PK/upsert/UPDATE/DELETE/DDL/Case 연결 없음 |
| 실제 MySQL 구문·제약·transaction·DML·DB 조회·제품 HTTP/FE | 미실행. 로컬MySQL parser도 없어 정적검사로 실행 성공을 주장하지 않음 |
| Excel 재계산·native 표시·매크로·외부링크·storage/API | 미실행; 이번 읽기 구조 범위 밖 |

재현 도구·전체 출력은 로컬 `temp/catalog-structure-c-20261019/`에 보관한다. 명령은 stable work note에 기록했다. Java 실행에서 logging provider 부재 메시지1건이 있었지만 reader 결과/검증은 성공했다. 분석용 도구·자료는 새 전용 디렉터리만 사용했다.

## 사용자의 수동 적용·검증 순서

1. SQL §1에서 대상 master·MySQL8+·실제 `SHOW CREATE TABLE`·기존 audit DDL 정합성을 확인한다. audit은 `crt_*/mod_*`, `DATETIME` 초 단위이며 ID는 auto_increment다. 등록할 양식의 모델 코드(`mdl_spt_cd`), 미사용 version, 감사 login을 NULL 자리3곳에 지정한다. 파일은 manifest 해시로 다시 확인한다.
2. 동일 version 또는 동일 marker가 있으면 재등록하지 않는다. SQL §2는 이번에 새 root1건을 만들었을 때만 실제 DB의 부모행을 조회해 하위 INSERT를 진행한다. 같은 연결에서 stop-on-error(오류 즉시 중단)로 한 등록자가 실행한다. 임의 고정 ID·MAX(version)+1·오래된 LAST_INSERT_ID·IGNORE/upsert를 사용하지 않는다.
3. 같은 transaction 안에서 §3을 실행한다. root1/DRAFT·역할4·시트70(25/23/17/5)·표92(40/15/32/5)·수정범위0, 잘못된 시트/중복/JSON/seed 차이0건을 확인한다. JSON92개와 파일/시트의 실제 부모·순서·이름/상태를 대조하는 SQL이 포함돼 있다. 한 건이라도 다르거나 SQL 오류가 나면 ROLLBACK한다.
4. 파일 끝의 기본 명령은 `ROLLBACK`이다. 첫 실행 결과를 확인하고 부분 DRAFT 등록을 보존하려는 경우에만 마지막 결정을 `COMMIT`으로 교체한다. SQL은 자동 발행하지 않으며 기존 Case/Draft를 새 양식으로 전환하지 않는다.

`file_strt_hash_val`은 NULL이다. 내용 SHA-256과 미정인 구조 hash를 같은 값으로 취급하지 않았다. 이 DDL의 기존 nullable(빈 값 허용) 정의를 따르며, 구조 hash 방식 합의 전에도 파일 역할/시트/표 등록은 검토할 수 있다.

## A 연계사항과 정본 반영 제안

- A chat: `01a14dd2-1658-7502-950e-2d87c72d0ee4` — `A · 공통 Draft 임시저장 요청·상태·복구 기반 구현`. 이 결과는 파일로 전달하며 A 코드·공용 정본·기존 작업 노트는 수정하지 않았다.
- 이 SQL의 tableId는 최신 영문 표명을 사용한다. 과거 Case103 smoke의 `OUTPUT/BCPrices/base_case_prices`와 자동 치환하지 않고 A가 조회·worker의 실제 테이블/필드 대응을 대조해야 한다. 기존 Draft `08a7b46a-6f38-49dc-8065-30334102631a`는 기존 고정 양식을 유지한다.
- 7개 보류 조회와199열 타입·필드 검증, 계산/보존·의존/화면 갱신 규칙을 정본/새 DRAFT에서 완성해야 한다. 현행 reader가 받지 않는 추가 속성·기간 열 추가·수식 행 식별을 SQL에 발명하지 않았다.
- Sector 국가/하위부문 옵션 seed는 없다. PriceModel 비계약 물량/Regas 신규 설비 추천은 미승인 상태를 유지한다. 원본 모델의 명칭 인식·표별 정책 확인과 읽기 구조 검증을 구분한다.
- 정본 수정이 필요하면 A가 domain/DDL의 완료 상태만 정합화한다. 예를 들어 DDL의 “실제 seed 없음/운영 승인본 아직 계측 안함”과 현재 소비 코드 부재 같은 과거 문구는 이번 조사/현행 소스 근거와 대조할 수 있다. 내용 hash를 구조 hash로 정의하거나 부분 조사를 전체 발행 완료로 바꾸지 않는다.

Readiness: 이 범위의 구조 SQL 검토는 가능하다. **실제 DB 적용·ACTIVE 발행·Case 연결·전체 표 편집 가능 상태는 미완료이며 이 작업의 완료 조건이 아니다.**
