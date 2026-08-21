# MI Fundamental Model - Trade Flows API 설계

- domain: gas
- status: implemented (real DB rollout complete 2026-06-15; separation rationale/data shape below remain canonical)
- owner: platform BE
- updated at: 2026-07-03
- canonical spec: [mi-fundamental-model-mock-api-feature-spec.md](../../feature-specs/mi-fundamental-model-mock-api-feature-spec.md)
- canonical persistence: [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)
- role: Trade Flows API를 Volumes API에서 분리한 근거와 데이터 구조 설계 결정을 기록한다

---

## 1. 분리 배경

MI Fundamental Model Volumes 탭은 세 가지 하위 뷰를 가진다.

| 탭 | 데이터 성격 | UI 구조 |
|----|------------|---------|
| Global | 시계열 × Region/Country 계층 | 행: 국가/지역/합계, 열: 시간 축 (Monthly/Yearly) |
| LNG | 시계열 × Region/Country 계층 | 행: 국가/지역/합계, 열: 시간 축 (Monthly/Yearly) |
| Trade | 단일 연도 × From/To Country 매트릭스 | 행: 수입국, 열: 수출국, 셀: 물량 |

초기 설계에서는 세 탭을 `GET /{caseId}/volumes?segment=GLOBAL|LNG|TRADE` 단일 엔드포인트로 통합했다.

### 분리 결정 이유

| 항목 | Global / LNG | Trade |
|------|-------------|-------|
| 필수 입력 | `segment` | `selector`(LNG/PNG), `year` |
| 선택 입력 | `granularity`(MONTHLY/YEARLY), `types`, `regions` | `unit`(MTPA/BCM), `types`(TO_COUNTRY/FROM_COUNTRY), `regions` |
| granularity 개념 | 있음 (월별 ↔ 연별 집계) | 없음 (단일 연도 매트릭스) |
| 응답 shape | `{ columns, rows }` — 시계열 구조 | `{ fromCountries, toCountries, flows }` — 매트릭스 구조 |
| `types` 의미 | 데이터 타입 (Gas Demand, LNG Imports 등) | 필터 방향 (TO_COUNTRY, FROM_COUNTRY) |

응답 shape과 입력 파라미터가 근본적으로 달라, 단일 엔드포인트로 유지하면:
- 응답 DTO 타입 안정성이 깨진다 (`segment`에 따라 shape이 달라지는 union type)
- `granularity` 파라미터가 Trade에서 무의미하게 남는다
- `types` enum이 두 가지 의미를 동시에 가진다

**결론:** `GET /{caseId}/volumes/trade` 별도 엔드포인트로 분리.

---

## 2. 데이터 구조 설계

### 2.1 Sparse Matrix + 헤더 배열

Trade Flow는 본질적으로 From country(열) × To country(행) 매트릭스다. 이를 표현하는 방식 세 가지를 검토했다.

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. 완전 매트릭스 | 모든 (From, To) 조합을 0 포함해서 반환 | 프론트 처리 단순 | LNG 2026 예시 기준 From 100 × To 100 = 10,000개 항목(응답 Total 행 포함 시 10,100개). JSON 크기 과다 |
| B. Sparse flows만 | 0이 아닌 항목만 반환 | 응답 경량 | 프론트가 전체 국가 목록을 모름. 매트릭스 헤더 렌더링 불가 |
| **C. 헤더 배열 + Sparse flows** | `fromCountries`, `toCountries` 배열 + 0이 아닌 flows만 반환 | 헤더 정보로 전체 매트릭스 구성 가능. flows는 경량 유지 | 프론트가 lookup 로직 필요 |

**C 방식 채택.** 엑셀 원본 데이터의 비영(non-zero) 항목이 15개(Total 포함 24개)인 반면 LNG 2026 예시 기준 전체 조합은 From 100 × To 100 = 10,000개(응답 Total 행 포함 시 10,100개)에 달해 A 방식은 비효율적이다.

### 2.2 fromCountries / toCountries 구조

```json
{ "key": "REGION:AFRICA", "label": "Africa", "kind": "REGION_SUBTOTAL", "region": "AFRICA", "country": null }
{ "key": "COUNTRY:AFRICA:Angola", "label": "Angola", "kind": "COUNTRY", "region": "AFRICA", "country": "Angola" }
{ "key": "TOTAL", "label": "Total", "kind": "GRAND_TOTAL", "region": null, "country": "Total" }
```

- `key`: 행/열 식별자. `flows[].fromKey`, `flows[].toKey`가 참조한다
- `label`: 화면 표시명. 지역 집계는 region label, 개별 국가는 country, 합계는 `Total`
- `kind == REGION_SUBTOTAL`: 지역 집계. 화면에서 **볼드** 처리
- `kind == COUNTRY`: 개별 국가
- `kind == GRAND_TOTAL`: 합계 행. `toCountries` 마지막에만 위치
- 기존 `region`, `country`는 필터링과 후속 DB 전환 시 region/country 조건 조회를 위해 유지한다

### 2.3 Region 고유값 vs Country 목록 동적 변동

담당자 확인 사항:
> "region의 고유값은 고정. base 버전이나 기준 정보에 따라 country의 고유값 / region 내 country 목록 변경 가능"

이 특성으로 인해:
- `region`은 `FundamentalModelRegion` enum으로 고정 관리
- `country`는 String으로 처리 (enum 불가)
- `fromCountries`와 `toCountries`는 서로 다를 수 있으므로 별도 배열로 분리

DB 전환 시 country 목록 변동을 수용하는 방안:
1. 별도 메타데이터 테이블: `trade_flow_country_meta(version_id, region, country, axis)`
2. JSON 컬럼: `from_countries_json`, `to_countries_json` (컬럼 목록을 JSON으로 저장)

### 2.4 Total 행

엑셀의 "Total" 행은 각 수출국별 전체 수입량 합계다.

- `toCountries` 마지막에 `{ "region": null, "country": "Total" }` 추가
- flows에 `toRegion=null, toCountry="Total"` 항목으로 포함
- region 필터 적용 시에도 Total 행은 항상 포함 (집계 행 특성)

---

## 3. 필터 동작 설계

`types` (TO_COUNTRY/FROM_COUNTRY)와 `regions`의 조합:

| types | regions | 동작 |
|-------|---------|------|
| 전체 (기본) | 전체 (기본) | 전체 반환 |
| TO_COUNTRY | ASIA | toCountries를 ASIA로 축소, fromCountries 전체 |
| FROM_COUNTRY | AFRICA | fromCountries를 AFRICA로 축소, toCountries 전체 |
| TO_COUNTRY, FROM_COUNTRY | ASIA | fromCountries, toCountries 모두 ASIA로 축소 |

flows는 축소된 fromCountries × toCountries 범위에서 non-zero 값만 포함한다.
Total 행(`toRegion=null`)은 필터와 무관하게 항상 포함된다.

---

## 4. Mock 데이터 출처 (1차 mock 단계 참고용, 2026-06-15 real DB로 전환 완료)

엑셀 파일: `Output_Model_Dashboard_Templates_20260406_001_suje.xlsx`의 "Trade Flow Dashboard" 시트

- 시트 구조: row 9 = From country 헤더, col 4 = To country 레이블
- selector = LNG (PNG/LNG selector = "LNG"), year = 2026, unit = mtpa
- From country: 100개 (8개 region, region 집계 포함). 원본 시트 `F9:DA9` 기준
- To country: 100개 (8개 region, region 집계 포함). 원본 시트 `D13:D112` 기준이며, API 응답에는 `Total` 행을 마지막에 추가한다
- 비영 flows: 15개 (Total 포함 24개)

```
fromCountries region별 구성:
  AFRICA(13), NORTH_AMERICA(5), SOUTH_AMERICA(11), ASIA(22),
  AUSTRALIA(5), EUROPE(29), MIDDLE_EAST(13), RUSSIA(2)

toCountries LNG 2026 예시 구성:
  AFRICA(13), NORTH_AMERICA(5), SOUTH_AMERICA(11), ASIA(22),
  AUSTRALIA(5), EUROPE(29), MIDDLE_EAST(13), RUSSIA(2)
```

---

## 5. DB 전환 결정 (2026-06 ERD review에서 확정, real DB 적용 완료)

- country 목록 가변 구조는 header row table(`tmd_mdl_scnrcasetrdehdrl`) + sparse flow table(`tmd_mdl_scnrcasetrdeflwl`)로 확정했다. JSON 컬럼 후보(`from_countries_json`/`to_countries_json`)는 superseded됐다.
- Sparse flows 저장 방식은 `tmd_mdl_scnrcasetrdeflwl`의 복합 키 구조로 확정했다. 물리 스키마 정본은 [mi-fundamental-model-output-result-persistence-design.md](mi-fundamental-model-output-result-persistence-design.md)를 따른다.
- 집계 행(region aggregate, Total)의 저장 방식: 파생값으로 DB에 미저장하고 조회 시 계산할지, 원본 엑셀 데이터 그대로 저장할지 결정 필요
