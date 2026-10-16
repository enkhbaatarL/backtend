# MI 모델 Input 카탈로그 전수 조사 근거

## Document Control

- status: Complete
- canonical for: Reference only — 실제 `demand.xlsm`, `supply.xlsm`, `trade.xlsm` 구조의 2026-09-16 시점 조사 결과
- not canonical for: 화면 노출 정책, API 계약, DDL, 계산 규칙, catalog seed
- owner: BE
- ticket: `INPUT-01`
- source work note: `docs/work-notes/2026/2026-09-16-model-input-catalog-inventory.md`
- depends on:
  - `docs/design-docs/gas/mi-model-input-editing-delivery-plan.md`
- supersedes: 없음
- last synced: 2026-09-17
- open questions:
  - 화면 후보 table/field의 활성화와 영속 row identity는 `INPUT-03`에서 확정해야 한다.

## 1. 조사 범위와 안전 기준

조사 대상은 저장소의 아래 파일 3개다. 해시는 같은 파일로 결과를 재현하기 위한 식별값이다.

| 모델 | 파일 | SHA-256 | 시트 | VBA |
| --- | --- | --- | ---: | --- |
| DEMAND | `scripts/demand.xlsm` | `3f2628680f3b383902cf0e25270b5cf0eb95595934aeed008404679f7cb0d8ec` | 25 | 없음 |
| SUPPLY | `scripts/supply.xlsm` | `b7ad13d49726cd9e881016cf1cc3d511ab47be65cc87cba038a5da7de857fa7f` | 23 | 없음 |
| TRADE | `scripts/trade.xlsm` | `e3ef772df507f05193fe6f4e517e438b5f0ed9daa810a14a127e6f8a3d5e4e54` | 17 | 있음 |

- OOXML을 읽기 전용으로 조사했다. 원본 파일은 수정하지 않았다.
- 문서에는 셀의 실제 업무 값, 수식 문자열, 고객사 식별값을 기록하지 않았다.
- 헤더명, 시트명, 좌표, 타입, 개수, 참조 시트명만 구조 근거로 기록했다.
- `화면 대상`은 제품 정책 확정이 아니라 **값 수정 화면 후보**다. `INPUT-03`이 이 근거를 검토해 canonical catalog를 확정해야 한다.
- 논리 범위 끝은 실제 데이터 끝이 아니라 현재 양식이 미리 서식·수식을 준비한 슬롯 끝을 포함할 수 있다.

## 2. 전체 계측 결과

| 모델 | `<v>` 값 셀 | inline 문자열 셀 | 수식 셀 | 외부 통합문서 참조 수식 셀 | OOXML Table | 분류 합계 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| DEMAND | 1,587,136 | 0 | 699,298 | 0 | 0 | 25 |
| SUPPLY | 1,993,828 | 0 | 460,803 | 35,099 | 0 | 23 |
| TRADE | 620,178 | 10 | 702,776 | 7,295 | 10 | 17 |
| **합계** | **4,201,142** | **10** | **1,862,877** | **42,394** | **10** | **65** |

기존 측정 기준인 non-empty `<v>` 셀은 4,201,142개이고, Trade에 inline 문자열 셀 10개가 별도로 있어
실제 표시값 셀은 4,201,152개다. 수식 셀 1,862,877개에는 cached result가 모두 존재한다. 이것은 현재 파일을 읽어 표시할 수 있다는 뜻이지,
화면 수정 후 새 값을 계산할 수 있다는 뜻은 아니다. DEMAND/SUPPLY에는 OOXML Table 정의가 하나도 없어
표 경계를 파일 메타데이터만으로 자동 복원할 수 없다.

분류 합계는 다음과 같다.

| 모델 | 화면 대상 | 참조·계산 전용 | 빈 시트 | 제외 후보 | 합계 |
| --- | ---: | ---: | ---: | ---: | ---: |
| DEMAND | 9 | 11 | 2 | 3 | 25 |
| SUPPLY | 6 | 6 | 8 | 3 | 23 |
| TRADE | 8 | 2 | 0 | 7 | 17 |
| **합계** | **23** | **19** | **10** | **13** | **65** |

## 3. 시트 전수 분류

### 3.1 DEMAND — 25/25

| # | 시트 | 상태 | 범위 | 분류 | 근거 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `Input Adjustment ->` | visible | `A1` | 빈 시트 | 셀 값·수식 없음 |
| 2 | `GDPPopulationAdjustment` | visible | `B1:O2000` | 화면 대상 | 직접 입력 열과 계산·검사 열이 분리됨 |
| 3 | `ElectrificationRateAdjustment` | visible | `B1:N2000` | 화면 대상 | 직접 입력 열 존재 |
| 4 | `PowerMixAdjustment` | visible | `B1:P7989` | 화면 대상 | 연간/월간 조정표 후보 |
| 5 | `CoalGasAdjustment` | visible | `B1:N2000` | 화면 대상 | 석탄/가스 조정표 2개 |
| 6 | `WeatherAdjustment` | visible | `B1:AG7997` | 화면 대상 | 국가/지역·기온/도일 조정표 4개 |
| 7 | `PMIAdjustment` | visible | `B1:N8000` | 화면 대상 | PMI 및 참조국 입력 후보 |
| 8 | `HydrogenAdjustment` | visible | `B1:N8000` | 화면 대상 | 생산량 조정 슬롯 존재 |
| 9 | `LNGBunkeringAdjustment` | visible | `A1:N2000` | 화면 대상 | 선박 수/연료소비 조정표 2개 |
| 10 | `Scenario Tracker` | hidden | `B3:H21` | 제외 후보 | 숨김 운영 추적표, 계산 참조 없음 |
| 11 | `DemandOutputOverwrite` | visible | `B1:AD507` | 화면 대상 | 부문/월 수요 overwrite 입력표 2개 |
| 12 | `기준정보 ->` | visible | `A1` | 빈 시트 | 셀 값·수식 없음 |
| 13 | `BCInput` | visible | `B1:BL100803` | 참조·계산 전용 | 대형 base input 및 계산 원천 |
| 14 | `MacroParametersLogicInput` | visible | `A1:Q176` | 참조·계산 전용 | 로직 파라미터·검증용, 직접 입력 계약 미확정 |
| 15 | `PMIInput` | visible | `B1:AZ7651` | 참조·계산 전용 | `PMIAdjustment` 영향 대상 |
| 16 | `PMILogicInput` | visible | `A1:BI92` | 참조·계산 전용 | PMI 계산 보조 |
| 17 | `WeatherLogicInput` | visible | `A1:BG92` | 참조·계산 전용 | `General` 참조 계산 |
| 18 | `WeatherInput` | visible | `A1:EY10000` | 참조·계산 전용 | `General`, `WeatherAdjustment` 영향 대상 |
| 19 | `LNGBunkeringInput` | visible | `B1:S10000` | 참조·계산 전용 | `General` 참조 계산 |
| 20 | `LNGBunkeringLogicInput` | visible | `B1:S98` | 참조·계산 전용 | 계산 보조 |
| 21 | `HydrogenInput` | visible | `B1:Y10077` | 참조·계산 전용 | `HydrogenAdjustment` 영향 대상 |
| 22 | `LNG Demand` | visible | `B1:N109` | 참조·계산 전용 | 결과/조회 성격 |
| 23 | `General` | visible | `B1:AM93` | 참조·계산 전용 | 공통 lookup·선택 목록 |
| 24 | `Sheet2_DISCARDAFTUSE` | hidden | `A1:J4505` | 제외 후보 | 이름과 숨김 상태가 폐기용임을 표시 |
| 25 | `CubeDump_DISCARDAFTUSE` | hidden | `A1:G172` | 제외 후보 | 이름과 숨김 상태가 폐기용임을 표시 |

### 3.2 SUPPLY — 23/23

| # | 시트 | 상태 | 범위 | 분류 | 근거 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `Input Upstream ->` | visible | `A1` | 빈 시트 | 셀 값·수식 없음 |
| 2 | `ProductionAdjustment` | visible | `A1:P201` | 화면 대상 | 연간/월간 생산 조정표 |
| 3 | `Input Midstream ->` | visible | `A1` | 빈 시트 | 셀 값·수식 없음 |
| 4 | `{USA}Hurricanes` | visible | `A1:L201` | 화면 대상 | 허리케인 강도 입력 슬롯 |
| 5 | `LiqUtilAdjustment` | visible | `A1:LP407` | 화면 대상 | 설비×월 가동률 matrix |
| 6 | `LiqProdAdjustment` | visible | `A1:HE407` | 화면 대상 | 설비×월 생산량 matrix |
| 7 | `RegasAdjustment` | visible | `B1:DX766` | 화면 대상 | 설비×월 조정표와 파생표 |
| 8 | `PipelineAdjustment` | visible | `A1:AB202` | 화면 대상 | 파이프라인 월별 조정 레코드 |
| 9 | `BCProduction` | visible | `A1:LJ25000` | 참조·계산 전용 | base production 원천 |
| 10 | `BCHurricanes` | visible | `B1:K41` | 참조·계산 전용 | base hurricane 원천, 외부 참조 8셀 |
| 11 | `BCLiquefaction` | visible | `A1:AH52411` | 참조·계산 전용 | liquefaction master/base |
| 12 | `BCRegasification` | visible | `A1:V5402` | 참조·계산 전용 | regasification master/base |
| 13 | `BCPipeline` | visible | `B1:J5402` | 참조·계산 전용 | pipeline master/base |
| 14 | `LNG Summary` | visible | `A1:Y531` | 참조·계산 전용 | 표시·요약 결과 |
| 15 | `LiqUtilAdjustment_backup` | visible | `A1:LP358` | 제외 후보 | backup 복제본 |
| 16 | `LiqProdAdjustment_backup` | visible | `A1:HE358` | 제외 후보 | backup 복제본 |
| 17 | `BCLiquefaction_backup` | visible | `A1:AH41110` | 제외 후보 | backup 복제본, 외부 참조 35,091셀 |
| 18 | `{USA}RigsLogic` | hidden | `C1:H4` | 빈 시트 | 값·수식 셀 없음 |
| 19 | `{China}PolicyLogic` | hidden | `C1:H4` | 빈 시트 | 값·수식 셀 없음 |
| 20 | `{USA}HurricanesLogic` | hidden | `C1:H4` | 빈 시트 | 값·수식 셀 없음 |
| 21 | `RegasLogic` | hidden | `C1:H4` | 빈 시트 | 값·수식 셀 없음 |
| 22 | `PipelineLogic` | hidden | `C1:H4` | 빈 시트 | 값·수식 셀 없음 |
| 23 | `MonthlyAdjProduction` | hidden | `A1` | 빈 시트 | 셀 값·수식 없음 |

### 3.3 TRADE — 17/17

| # | 시트 | 상태 | 범위 | 분류 | 근거 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `LNG_TradeRestrictionInput` | visible | `B4:K509` | 화면 대상 | LNG 제한 레코드 입력 |
| 2 | `PNG_TradeRestrictionInput` | visible | `B4:J509` | 화면 대상 | PNG 제한 레코드 입력 슬롯 |
| 3 | `Sheet1` | hidden | `A1:W90` | 제외 후보 | 숨김 임시 시트, 제품 의미 미확정 |
| 4 | `MidstreamContractInput` | visible | `A1:CU4707` | 참조·계산 전용 | 계약 원천·다단계 계산 혼합, 수식 166,271셀 |
| 5 | `ShippingCostInput` | visible | `A1:AV18929` | 화면 대상 | 상단에 명시적 사용자 입력표 9개 존재 |
| 6 | `MidstreamCapacityInput` | visible | `B1:FN8750` | 화면 대상 | 5개 OOXML Table 및 사용자 조정 열 존재 |
| 7 | `UpstreamAdjLogicInput` | visible | `A1:AI674` | 화면 대상 | 일반 파라미터와 명시적 User-Input 열 존재 |
| 8 | `DemandAdjLogicInput` | visible | `A1:R40` | 화면 대상 | 시장 목록·선택/threshold 입력 후보; 계약 신뢰도 낮음 |
| 9 | `CostInput` | visible | `A1:AS2001` | 화면 대상 | 5개 비용표의 명시적 User-Input 열 존재 |
| 10 | `4.5 PriceModelInput` | visible | `B2:AO91` | 참조·계산 전용 | OOXML Table 5개가 전부 다른 입력/외부 workbook 계산 결과 |
| 11 | `LNGExportRestictionInput` | visible | `B2:E13` | 화면 대상 | 헤더와 빈 슬롯만 존재; 행 추가 없이는 1차 사용 불가 |
| 12 | `backupLNG_TradeRestrictionInput` | visible | `B4:J509` | 제외 후보 | backup 복제본 |
| 13 | `backup_MidstreamContractInput` | visible | `A1:CU2918` | 제외 후보 | backup 복제본 |
| 14 | `Industry research` | hidden | `A2:F3` | 제외 후보 | 숨김 조사 보조 |
| 15 | `net exporter` | hidden | `A1:E255` | 제외 후보 | 숨김 lookup 원천 |
| 16 | `pipeline cap` | hidden | `A1:M266` | 제외 후보 | 숨김 lookup 원천 |
| 17 | `pipeline contract` | hidden | `A1:G208` | 제외 후보 | 숨김 lookup 원천 |

## 4. 화면 대상 논리 테이블 카탈로그 후보

표기:

- 타입: `I` 정수, `N` 소수, `T` 문자열, `E` 선택값, `B` Yes/No, `Y/M` 연·월.
- `편집`은 사용자가 보낼 수 있는 필드 후보, `수식/RO`는 workbook 수식 또는 기준값·helper라 직접 수정 금지다.
- `확정`은 헤더와 현재 수식/값 분포가 경계를 뒷받침한다. `후보`는 서식 꼬리나 빈 슬롯 때문에 `INPUT-03` 확인이 필요하다.
- 1차 값 수정에서는 아래 모든 표의 `rowAdd=false`, `columnAdd=false`를 기본값으로 제안한다.

### 4.1 DEMAND

| 시트 / 표 | 경계 (header / data) | key 후보 | 편집 필드 후보 | 수식/RO | 상태 |
| --- | --- | --- | --- | --- | --- |
| `GDPPopulationAdjustment` / GDP | `B6:H2000` (`6` / `7:2000`) | Year(Y)+Country(E) | `B,C,F:% change(N)` | `D,E,G,H` | 확정 |
| 같은 시트 / Population | `J6:O2000` (`6` / `7:2000`) | Year(Y)+Country(E) | `J,K,M:Population(N)` | `L,N,O` | 확정, 현재 literal 입력 0행 |
| `ElectrificationRateAdjustment` | `B6:H2000` (`6` / `7:2000`) | Year+Country+Drive Train(E) | `B,C,D,F:% sales(N)` | `E,G,H` | 확정, 현재 literal 입력 0행 |
| `PowerMixAdjustment` / annual | `B6:H7989` (`6` / `7:7989`) | Year+Country+Energy Source(E) | `B,C,D,F:% mix(N)` | `E,G,H` | 확정 |
| 같은 시트 / monthly | `J6:P7989` (`6` / `7:7989`) | Year+Month+Country+Energy Source | `J,K,L,M,O:% mix(N)` | `N,P` | 후보, 현재 0행 |
| `CoalGasAdjustment` / coal | `B6:G2000` (`6` / `7:2000`) | Month-Year+Region(E) | `B,C,E:price(N)` | `D,F,G` | 확정, 현재 literal 입력 0행 |
| 같은 시트 / gas | `H6:M2000` (`6` / `7:2000`) | Month-Year+Region(E) | `H,I,K:price(N)` | `J,L,M` | 확정, 현재 literal 입력 0행 |
| `WeatherAdjustment` / country degree-day | `B6:I7996` (`6` / `7:7996`) | Year+Month+Country+HDD/CDD(E) | `B,C,D,E,G(N)` | `F,H,I` | 확정, 현재 literal 입력 0행 |
| 같은 시트 / country temperature | `K6:Q7997` (`6` / `7:7997`) | Year+Month+Country | `K,L,M,O:temperature(N)` | `N,P,Q` | 확정 |
| 같은 시트 / region degree-day | `T6:Z7996` (`6` / `7:7996`) | Year+Month+Region+HDD/CDD | `T,U,V,W,X(N)` | `Y,Z` | 후보, 현재 0행 |
| 같은 시트 / region temperature | `AB6:AG7996` (`6` / `7:7996`) | Year+Month+Region | `AB,AC,AD,AE:change(N)` | `AF,AG` | 후보, 현재 0행 |
| `PMIAdjustment` / PMI | `B6:H8000` (`6` / `7:8000`) | Year+Month+Country | `B,C,D,F:PMI(N)` | `E,G,H` | 확정 |
| 같은 시트 / reference mapping | `J6:N100` (`6` / `7:100`) | Country(E) | `J,L:Reference Country(E)` | `K,M,N` | 후보, 현재 0행 |
| `HydrogenAdjustment` | `B6:G8000` (`6` / `7:8000`) | Year+Country | `B,C,E:production(N)` | `D,F,G` | 확정, 현재 literal 입력 0행 |
| `LNGBunkeringAdjustment` / vessel | `A6:G2000` (`6` / `7:2000`) | Year+Vessel Type(E) | `B,C,E:count(I)` | `A,D,F,G` | 확정, 현재 literal 입력 0행 |
| 같은 시트 / fuel | `H6:M2000` (`6` / `7:2000`) | Year+Vessel Type(E) | `H,I,K:consumption(N)` | `J,L,M` | 확정, 현재 literal 입력 0행 |
| `DemandOutputOverwrite` / sector | `D7:K507` (`7` / `8:507`) | Country+Year+Month+Sub-Sector | `D,E,F,G,H:demand(N)` | `I,J,K` | 확정 |
| 같은 시트 / monthly | `M7:S507` (`7` / `8:507`) | Country+Year+Month | `M,N,O,P:demand(N)` | `Q,R,S` | 확정, 현재 literal 입력 0행 |

현재 literal 입력이 있는 행만 보면 GDP 93, PowerMix annual 57, Weather country temperature 150,
PMI 48, Demand sector 12행의 key 후보는 각각 중복 0건이었다. 빈 슬롯만 있는 표는 자연키를 검증할
표본이 없으므로 `templateVersion + tableKey + rowNo`를 1차 임시 row token으로 쓰고, 행 추가를 열기 전에
영속 surrogate ID 정책을 확정해야 한다.

### 4.2 SUPPLY

| 시트 / 표 | 경계 (header / data) | key 후보 | 편집 필드 후보 | 수식/RO | 상태 |
| --- | --- | --- | --- | --- | --- |
| `ProductionAdjustment` / annual | `A7:H200` (`7` / `8:200`) | Year+Province/Country | `B,C,E:volume(N),F:%(N)` | `A,D,G,H` | 확정, 현재 literal 입력 0행 |
| 같은 시트 / monthly | `I7:O201` (`7` / `8:200`) | Year+Month+Country | `I,J,K,M:volume(N)` | `L,N,O` | 후보, 현재 literal 입력 0행 |
| `{USA}Hurricanes` | `B7:E200` (`7` / `8:200`) | Year+Month | `B,C,D:intensity(N)` | `E` | 확정, 현재 literal 입력 0행 |
| `LiqUtilAdjustment` | `B10:CN407` (`7:10` / `11:407`) | Country+Plant+Train | `G:CL` 월별 utilisation(N) | `B:F` 속성, `CN` 검사 | 확정 |
| `LiqProdAdjustment` | `B10:CM407` (`7:10` / `11:407`) | Country+Plant+Train | `G:CL` 월별 production(N) | `B:F` 속성, `CM` 검사 | 확정, 현재 literal 입력 0행 |
| `RegasAdjustment` / main | `B8:DK382` (`9:10` / `11:382`) | Continent+Region+Country+Plant | `H:AE` 현재 literal 월 값 | `B:G`, `AF:DK` 수식 | 확정 |
| 같은 시트 / annual rollup | `DM10:DV382` (`10` / `11:382`) | main row 연결 | 없음 | 전부 RO, 파생값 | 확정 |
| 같은 시트 / country monthly rollup | `G385:DK444` (`385:386` / `387:444`) | Country | 없음 | 전부 RO, stale 가능 | 확정 |
| `PipelineAdjustment` | `A7:I200` (`7` / `8:200`) | Year+Month+Pipeline | `B,C,D,H:capacity(N)` | `A,E:G,I` | 확정, 5행 key 불완전 |

- `ProductionAdjustment` 월간 표의 `L8:L200`은 193셀 전부 수식과 cached result가 `#REF!`이고,
  같은 시트의 다른 수식과 다른 시트 수식에서 L열 참조가 확인되지 않았다. 이 열은 화면 편집·계산 대상이
  아닌 원본 보존 영역으로 확정했으며, 정책은
  `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §6 규칙 30–32가 소유한다.
- `LiqUtilAdjustment`와 `LiqProdAdjustment`의 `(Country, Plant, Train)`은 397/397행에서 중복 0건이다.
- `RegasAdjustment`의 4개 자연키 조합은 372/372행에서 중복 0건이지만, 설비명 단독은 중복 사례가
  확인돼 영속 식별자로 사용할 수 없다. `BCRegasification`과 함께 발급하는 surrogate ID가 필요하다.
- `PipelineAdjustment`는 실제 값이 있는 60행 중 55행만 key가 완전하다. 나머지 5행은 깨진 참조의
  영향으로 자연키 기반 수정 요청을 안전하게 만들 수 없다.
- matrix의 기간 열 추가는 단순 열 추가가 아니다. 다중 행 헤더, 기본값 수식, 검사/rollup을 함께
  이동·복제해야 하므로 1차에는 `row_add=false`, `column_add=false`가 안전하다.

### 4.3 TRADE

| 시트 / 표 | 경계 (header / data) | key 후보 | 편집 필드 후보 | 수식/RO | 상태 |
| --- | --- | --- | --- | --- | --- |
| `LNG_TradeRestrictionInput` | `B4:K509` (`5` / `6:509`) | Year+Month+From+To+Terminal+Train+Scope | `B:K` 직접 값 | 수식 없음 | 확정, 자연키 불완전 |
| `PNG_TradeRestrictionInput` | `B4:J509` (`5` / `6:509`) | Year+Month+From+To+Pipeline+Scope | `B:J` 직접 값 | 수식 없음 | 확정, 현재 0행 |
| `ShippingCostInput` / Pacific charter | `B4:G14` (`5` / `6:14`) | Year | `D:growth(N),F:absolute(N)` | `A,C,E,G` | 확정 |
| 같은 시트 / Atlantic charter | `B15:G25` (`16` / `17:25`) | Year | `D,F` | `A,C,E,G` | 확정 |
| 같은 시트 / fuel | `B26:G36` (`27` / `28:36`) | Year | `D,F` | `A,C,E,G` | 확정 |
| 같은 시트 / Suez crossing | `B37:G47` (`38` / `39:47`) | Year | `D,F` | `A,C,E,G` | 확정 |
| 같은 시트 / Panama crossing | `B48:G58` (`49` / `50:58`) | Year | `D,F` | `A,C,E,G` | 확정 |
| 같은 시트 / Suez disruption | `B59:D68` (`60` / `61:68`) | Year | `C:Yes/No(B),D:distance(N)` | 없음 | 확정 |
| 같은 시트 / Panama disruption | `B70:D79` (`71` / `72:79`) | Year | `C,D` | 없음 | 확정 |
| 같은 시트 / technical parameters | `B81:E83` (`82` / `83`) | 단일 행 | `B,C,E(N)` | `D` 파생 후보 | 후보 |
| 같은 시트 / Panama spot | `B86:E93` (`87` / `88:93`) | Year | `C:capacity(N),D:Use(B),E:Memo(T)` | 없음 | 확정 |
| 같은 시트 / shipping calculation | `I2:Y18929` | 계산 레코드 | 없음 | 전부 RO | 확정 |
| 같은 시트 / lookup blocks | `AB2:AT375` | lookup key | 없음 | 전부 RO | 후보(복수 블록 집합) |
| `MidstreamCapacityInput` / liquefaction base | `U3:AE27` (`3` / `4:27`) | Country | 없음 | OOXML Table, RO | 확정 |
| 같은 시트 / regas base | `AI3:AS61` (`3` / `4:61`) | Country | 없음 | OOXML Table, RO | 확정 |
| 같은 시트 / LNG production base | `BM3:BV92` (`3` / `4:92`) | Country+Plant | 없음 | OOXML Table, RO | 확정 |
| 같은 시트 / pipeline disruption | `DD3:DH651` (`3` / `4:651`) | Year+Month+Pipeline | `DH:capacity(N)` | `DG` base | OOXML Table, 확정 |
| 같은 시트 / pipeline share | `DX3:EC186` (`3` / `4:186`) | Year+From+To+Pipeline | `EB:share(N)` | `EC` selected | 확정 |
| 같은 시트 / regas disruption | `EE3:EI6267` (`3` / `4:6267`) | Year+Month+Country | `EI:capacity(N)` | `EH` base | OOXML Table, key 중복 |
| 같은 시트 / importer share | `FB3:FD8` (`3` / `4:8`) | From+To | `FD:share(N)` | 없음 | 확정 |
| `UpstreamAdjLogicInput` / general | `A2:C5` (`2` / `3:5`) | Parameter(T) | `B:value(N)` | `C` unit | 후보 |
| 같은 시트 / country-year adjustment | `I2:R674` (`2` / `3:674`) | Year+Country | `N:%(N),P:volume(N)` | `K:M,O,Q,R` | 확정, 현재 0행 |
| 같은 시트 / support | `T2:AZ674` | 내부 key | 없음 | 외부참조 포함 RO | 후보(복수 블록 집합) |
| `DemandAdjLogicInput` / markets | `A2:A40` (`2` / `3:40`) | row ordinal | `A:market(E)` | 없음 | 후보 |
| 같은 시트 / selector | `C2:C3` (`2` / `3`) | 단일 값 | `C3(E)` | 없음 | 후보 |
| 같은 시트 / thresholds | `E2:M7` (`2` / `3:7`) | row ordinal | `D/F` 계열 수치 후보 | `H:M` 수식/RO | 후보, 정책 확인 필수 |
| `CostInput` / regas tariff | `A2:F379` (`2` / `3:379`) | Year+Country | `D:user input(N)` | `C,E,F` | 확정 |
| 같은 시트 / liquefaction cost | `H2:M233` (`2` / `3:233`) | Year+Country | `K(N)` | `J,L,M` | 확정 |
| 같은 시트 / LNG upstream | `O2:T227` (`2` / `3:227`) | Year+Country | `R(N)` | `Q,S,T` | 확정 |
| 같은 시트 / non-LNG upstream | `V2:AA615` (`2` / `3:615`) | Year+Country | `Y(N)` | `X,Z,AA` | 확정 |
| 같은 시트 / pipeline cost | `AC2:AI1554` (`2` / `3:1554`) | Year+From+To | `AG(N)` | `AF,AH,AI` | 확정 |
| 같은 시트 / project SRMC | `AK2:AS519` (`2` / `3:519`) | Project+Country | 없음 | 전부 RO | 확정 |
| `LNGExportRestictionInput` | `B3:E13` (`3` / `4:13`) | Year+Country+Plant+Train | `B:E` | 수식 없음 | 확정, 현재 0행·행 추가 blocker |

확인된 row identity 예외:

- LNG 제한의 현재 32개 입력 행은 Year/Month가 비어 있어 제안한 자연키가 완전한 행이 0개다.
  화면 변경 식별에는 workbook row 번호를 임시 token으로 쓰고, 저장용에는 surrogate ID가 필요하다.
- `MidstreamCapacityInput`의 pipeline disruption 648행과 pipeline share 183행은 제안 key 중복 0건이다.
- regas disruption 6,264행은 `(Year, Month, Country)` 중복 행이 108개다. 시설 또는 원천 row ID가
  key에 더 필요하다.
- `DemandAdjLogicInput` 같은 목록형 표는 행 순서 자체가 의미를 갖는다. 행 추가/정렬을 허용하기 전
  명시적 order와 ID가 필요하다.
- Cost 사용자 입력 열은 현재 literal 입력이 없어 자연키 중복을 실제 변경 데이터로 검증하지 못했다.

## 5. 의존성 메타데이터

아래 정보는 "어떤 표가 영향을 받을 수 있는가"를 알리는 용도다. 계산 규칙이나 재계산 순서를 의미하지 않는다.

| 출발 | 직접 관찰된 참조/영향 대상 | 주의 |
| --- | --- | --- |
| DEMAND adjustment 계열 | `PMIInput`, `WeatherInput`, `HydrogenInput` 등 | 수식 문자열 해석만으로 모든 간접 의존성을 보장하지 못함 |
| `WeatherInput` | `General`, `WeatherAdjustment` | 화면 저장 후 파생 시트 재계산 필요 |
| `RegasAdjustment` | `BCRegasification` | 행 위치 기반 미러링; 정렬/삽입에 취약 |
| `LiqProdAdjustment` | `LiqUtilAdjustment_backup` | 현행 파일이 backup 시트를 직접 참조하므로 제외 시트도 생성 파일에서 보존해야 함 |
| `BCHurricanes` | 외부 workbook/LUT | 외부 참조 8셀 |
| `BCLiquefaction_backup` | 외부 workbook | 외부 참조 35,091셀 |
| `MidstreamCapacityInput` | `MidstreamContractInput` 및 외부 workbook | 외부 참조 5,167셀 |
| `UpstreamAdjLogicInput` | 외부 workbook | 외부 참조 1,344셀 |
| `CostInput` | `MidstreamCapacityInput` 및 외부 workbook | 외부 참조 517셀 |
| `4.5 PriceModelInput` | `CostInput`, `ShippingCostInput`, 외부 workbook | 수식 267셀이 모두 외부 참조 포함 |

따라서 `INPUT-04`의 영향 메타데이터는 최소 `source table -> affected sheet/table`을 표현한다.
같은 workbook 내부의 계산 허용 수식은 `INPUT-07` 계산 대상이지만, 외부 workbook 참조 수식 42,394개는
canonical domain rule에 따라 계산하지 않고 원본 formula·cached value·externalLink/package를 보존한다.
SUPPLY·TRADE는 외부 Excel 원본 없이 계속 운영하는 범위이므로 외부 파일 확보 요청이나 반입은 후속 작업이 아니다.

## 6. 구조 변경 blocker와 정책 후보

표 유형별 후보 정책은 다음과 같다. 개별 table seed에는 이 유형을 펼쳐 저장해야 한다.

| 표 유형 / 해당 표 | 1차 정책 | 이후 열 수 있는 조건 |
| --- | --- | --- |
| fixed-column record — DEMAND/SUPPLY의 연·월 레코드 표, Trade 제한·비용 표 | rowAdd/columnAdd 모두 금지 | surrogate ID, 필수 key, 선택 목록, append 위치, 수식/helper 복제 규칙 확정 후 row append만 후보 |
| matrix-time — `LiqUtil`, `LiqProd`, `Regas`, Midstream 연도 matrix | rowAdd/columnAdd 모두 금지 | master 동시 등록과 기간축 header/formula/check 복제 transaction 확정 후 append-only 후보 |
| single/config — Shipping technical parameter, Demand selector/threshold | rowAdd/columnAdd 모두 금지 | 고정 field contract로만 수정; 일반 행·열 추가는 계속 금지 후보 |
| list — Demand markets와 참조국 목록 | rowAdd/columnAdd 모두 금지 | stable ID+order와 중복 정책 확정 후 row append/reorder 후보 |
| read-only/derived — rollup, lookup, base, project SRMC | 영구 rowAdd/columnAdd 금지 후보 | 원천 table 변경 후 계산 엔진이 재생성 |
| 빈 template — LNG export restriction 및 현재 0행 표 | rowAdd/columnAdd 모두 금지 | 최초 행 생성 API를 구조 변경으로 명시하고 별도 승인한 뒤 row append 후보 |

| 범위 | blocker | 정책 후보 / 후속 영향 |
| --- | --- | --- |
| 전체 | 자연키가 없거나 현재 빈 표가 많음 | 1차는 `templateVersion + tableKey + rowNo` 임시 token, 구조 변경 전 surrogate ID 도입 |
| 빈 preformatted slot | 값을 처음 넣는 행은 값 수정인지 행 추가인지 모호함 | 1차 API에서 "기존 활성 행"과 "빈 슬롯 활성화"를 구분하고 후자는 기본 차단 |
| DEMAND/SUPPLY | OOXML Table 정의 0개 | 자동 탐지 금지, version별 catalog 좌표를 승인·seed해야 함 |
| matrix 표 | 연/월 다중 헤더와 수식 복제가 결합됨 | 중간 insert 금지, append-only 후보; 1차 `row/column add=false` |
| `RegasAdjustment` | master와 adjustment가 행 위치로 결합, 파생 rollup stale | 설비 등록을 `BCRegasification`+adjustment 동시 트랜잭션으로 설계 |
| `PipelineAdjustment` | 5개 불완전 key, 깨진 선택 목록/참조 흔적 | 서버 선택 목록·필수값 검증 확정 전 행 추가 금지 |
| TRADE 제한표 | 현재 연/월 없는 중복 가능 레코드 | surrogate ID와 중복 허용 규칙 필요 |
| TRADE regas disruption | 자연키 108개 중복 | 시설/원천 식별자를 추가 조사해야 함 |
| `LNGExportRestictionInput` | 데이터 행 0개 | 행 추가 정책 없이는 1차 값 수정 대상에서 비활성화 |
| 외부 참조/매크로 | Trade는 VBA와 외부 참조를 포함 | 외부 참조 formula/cached/link와 VBA/package를 보존하고 외부 참조 셀은 재계산하지 않음 |
| backup/숨김/제외 시트 | 화면에서는 제외해도 다른 시트가 참조할 수 있음 | 화면 미노출과 XLSM 삭제를 동일시하지 말고 최종 파일에는 보존 |

## 7. 후속 티켓에 넘기는 결정 입력

- `INPUT-03`: 위 23개 화면 후보를 그대로 활성화하지 말고 `확정/후보`와 row identity 예외를 반영해
  version별 catalog seed, field/option/dependency, `editable/readOnly/formula`, `rowAdd/columnAdd`
  정책과 영속 surrogate ID의 logical/physical model을 확정한다.
- `INPUT-04`: 조회 응답은 전체 workbook이 아니라 승인된 table slice를 제공하고, 빈 슬롯/파생표/제외 후보를
  명시적으로 구분한다. dependency는 영향 표시 metadata로 제한하고 계산 규칙으로 사용하지 않는다.
- `INPUT-05`: 승인된 template version의 화면 대상 table만 조회하고 빈 슬롯·파생표·제외 후보의 노출 정책을 적용한다.
- `INPUT-06`: draft delta는 좌표만 저장하지 말고 `templateVersion + tableKey + row token + fieldKey`를
  저장한다. `rowNo` 기반 token은 기존 행의 임시 식별자일 뿐 구조 변경용 영속 ID로 사용하지 않는다.
- `INPUT-07`: 원본 XLSM package, VBA, 숨김/backup 시트를 보존한 복제본에 delta를 적용하고 계산 허용 수식만 재계산한다. 외부 참조 셀은 원본 상태로 보존한다.
- `INPUT-08`: 임시저장 전 formula/RO 필드 변경 거부, key 완전성, 중복, 빈 문자열을 검증한다.
- `INPUT-10`: E2E에는 대표적인 row-record, matrix-time, 빈 template, 외부 참조 formula/cached/link 불변, VBA 보존 사례가 모두 필요하다.

## 8. 재현 방법

민감 원문을 출력하지 않는 티켓 전용 도구:

```bash
python3 -B scripts/inventory-model-input-catalog.py \
  scripts/demand.xlsm scripts/supply.xlsm scripts/trade.xlsm --compact
```

출력에는 workbook/sheet metadata, 좌표, 셀·수식·외부 참조 개수, validation 범위, OOXML Table 범위,
열별 타입 개수(비-compact 모드)만 포함된다. 기존 `scripts/measure-model-input-excel.py`의 총계와
교차검증했으며 시트 수, 값 셀, 수식 셀 총계가 일치했다.
