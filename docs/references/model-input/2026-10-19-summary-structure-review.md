# SUMMARY 기준 행·기간 추가 검토표

## Document Control

- status: Review
- canonical for: Reference only — 원본 SUMMARY 추출과 사람 검토용 후보
- owner: Shared (BE / FE)
- last synced: 2026-10-19
- source documents: `../mi-model-input-baseline-2026-10-07-manifest.json`의 sourceSnapshotPath 4개; 2026-10-19 사용자 LiqProd 신규 월 입력 방식·Period 산식·월 건너뛰기/중복 허용·입력 제한 근거 원칙·새 열 fieldKey·임시 키/성공 대응·기존 저장 API 열 명령 연결·headerValues 형식·LiqProd 월 열 삭제·같은 저장의 추가 후 삭제·성공 mapping 다섯 필드/상태·항목 없는 경우 빈 배열·삭제 키 미재사용/동일 요청 키 유지·열 동작 조회 metadata·LiqProd/LiqUtil 새 월 본문 빈칸/0 구분·editing.columnCreation 기존 표현 재사용·LNG Demand 새 연도 국가 입력·PMIInput·Base Case HDD/CDD/Temperature 새 연도 본문 빈칸/0 구분·U1 신규 입력 공통 시작값/U2 새 연도 직접 입력 결정
- depends on: `../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` 규칙13·26·45~51·58·65~66
- supersedes: 없음. 업무 승인·API 계약·운영 catalog를 대체하지 않는다.
- open questions: U1/U2는2026-10-19 사용자 채택으로 해소됐으며 이번 요구 범위의 사용자 필수 미결은0건이다. 표별 원본 대조/구현·검증·요청 밖 선택 기능 제안은 구분해 유지한다.

## 읽는 방법

- 사용자 요청에 따라 SUMMARY의 H `행 추가`, I `기간 추가`를 1차 판단 근거로 삼는다. 열 제목은 실제로 `기간 추가`이며, OUTPUT의 `세로 연도 행`을 열 추가로 바꾸지 않는다.
- 원문은 그대로 두고 `사람 수정`에 `행=..., 열=..., 사유=...`를 적는다. 이미 채택된 업무 결정은 참고 열에 표시했다. 사람 수정이 확정되면 업무 정본과 API/capability에 반영하며 이 표를 runtime 권한으로 직접 사용하지 않는다.
- 아래 목록은 SUMMARY의 화면 표시99항목이다. API 조회용92표와 동일 집합이 아니며 보류7항목도 빠뜨리지 않고 포함한다. 미표시93항목은 추가 버튼 후보에서 제외하고 파일별 건수를 보존한다.
- `아래 행 추가`는 추가 후보, `표 안에 추가`는 빈 슬롯/범위 확인, `선택표 동시 확장`은 연계 범위 확인, `국가 행 제한`은 허용 국가/합계 제외 범위 확인이다. 고정/조회/연결/기존 개수 문구는 자동 추가 허용으로 해석하지 않는다.
- 행 추가에서 행 삭제를, 기간 추가에서 기간 삭제를 추론하지 않는다. 삭제는 기존 domain의 표별 채택 결정과 별도 확인을 따른다.
- 읽기 전용 OOXML 추출이다. source4개의 SHA-256은 manifest와 일치했고 파일/매크로/수식/저장값을 수정하거나 재계산하지 않았다.

<a id="user-open-decisions-inventory-20261019"></a>

## 사용자 결정 미결 통합 집계 — 2026-10-19

- 집계 기준: 표마다 같은 질문을 반복하지 않고 **사용자가 선택할 공통 업무 정책2건(U1/U2)**으로 묶었다. 2026-10-19 사용자가 두 추천을 모두 채택했다. 현재 사용자 필수 미결은 **0건**이며 업무/API 정본에 반영했다. 기존 허용 범위·기본값·필수값·자동 계산/보존 규칙은 유지한다. 검토 범위는 업무 정본 §1A.4/§6/§11, 조회 API §9, 편집 API §10/§10.1, 제공 API의 현행 Open Questions와 고정 source4개다. 원본/정본에서 확인 가능한 사실·구현/검증 항목·추가 기능 제안은 사용자 결정으로 세지 않는다.

| ID | 현재 상태 | 채택된 범위 | 정본 |
| --- | --- | --- | --- |
| U1 | 확정(2026-10-19) | 명시된 기본값·필수값·자동 생성/수식 규칙이 없는 신규 사용자 입력칸의 빈칸 시작·미입력 빈칸 유지·숫자0 명시적 입력 구분. 기존 규칙 우선 | [업무 §6.6](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#common-new-input-default-decision-20261019)·[API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#common-new-input-default-api-decision-20261019) |
| U2 | 확정(2026-10-19) | 생성 주체가 미정이던7표의 새 연도는 사용자 직접 입력. LNG Demand 및 기존 월 헤더 입력/자동 생성 계약 유지 | [업무 §6.6](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#annual-header-manual-decision-20261019)·[API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#common-new-input-default-api-decision-20261019) |

### 사용자 질문에서 제외한 항목과 이유

| 항목 | 제외 근거/처리 |
| --- | --- |
| C1 표 목록/기간축·추가 위치 | 원본 SUMMARY·기존 표별 채택과 규칙13/26을 대조할 사실/기술 작업. 화면99항목이나 미검토 행 수를 질문 개수로 세지 않음. PNG Infrastructure 원본 J3:Q3은2023~2030 연도이며, SUMMARY J33의 월별 문구만으로 월 축 추가 질문을 만들지 않음 |
| C2/C3/C6/C9의 JSON·키·상태/오류·버튼·재조회 | [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-creation-metadata-decision-20261019)·§2~6/§10.1의 채택 범위를 적용. 잔여 필드 이름/oneOf·HTTP/issue 위치·내부 발급/저장 방식은 기존 계약을 따르는 BE 작업이며 사용자에게 다시 승인받지 않음 |
| C7 삭제 범위·모든 월 삭제 등 | 이미 채택한 표별 삭제 허용/fieldKey·값 보존 원칙을 유지하고 구체 수식/좌표·파일 결과는 구현/검증. 근거 없는 삭제 횟수/위치 제한을 새 승인 항목으로 만들지 않음. 연도 열 삭제·BCPrices 행 삭제 등 요청하지 않은 추가 동작도 필수 미결로 승격하지 않음 |
| 계산·연결/오류·모델 인식·원본 보존 — domain Q5/H1/H4, C8 등 | 원본 수식/참조·모델 사용 경로·기존 계산/보존 채택을 먼저 대조해야 할 기술·사실 확인. 원본에 근거한 필수 계산을 다시 승인받거나 미확인 범위를 사용자 정책 질문으로 만들지 않음. 원본·명시 보존 예외·기존 실패 원자성을 적용하며 조사 공백을 포괄 재계산 허가 요청으로 바꾸지 않음. 새 업무 예외가 실제 확인되면 그 근거를 별도로 제시하며 현재 질문으로 가산하지 않음 |
| Midstream H2/H3의 슬롯·후보/키·필수/CLEAR | 기존704행 슬롯·Q:U 필수 등 개별 채택은 유지. 범위/후보·형식은 원본과 기존 계약 대조, 미확정 추가 금지/필수값은 규칙66으로 만들지 않음. 이미 정해진 오류/0·빈칸 의미를 재질문하지 않음 |
| PriceModel/Regas 추가 추천, BCPrices 연도 CLEAR/행 삭제 등 | 미승인 추천이라는 표기 자체가 사용자 결정 필요의 근거는 아님. 원본·기존 업무 계약으로 해결할 부분과 요청 밖 추가 기능을 구분하며, 선택 제안을 현행 범위의 필수 승인 항목으로 만들지 않음 |
| 조회 Q2~Q6·DDL/DBA/자원·보안·OpenAPI/worker | 조회 정책은 이미 채택. seed/실물 대응·판독/응답 크기·접근/로그·원본 보존·계산·경합/복구·DBA 적용·자원 상한은 구현/설계/검증 담당 작업으로 제외 |
| 업로드 교체의 파일 간 영향 | 집계 중 최신 정본을 다시 대조했다. 사용자가 다른 파일 표에 영향 없다고 확정해 [제공 API §3.3](../../feature-specs/mi-fundamental-model-input-provision-feature-spec.md#incremental-input-upload-api-20261019)·domain 규칙67에 반영돼 있다. 파일 간 계산/이를 이유로 한 교체 거절은 미결에서 제외. 초안 생성/양식 고정/직접 업로드 단일 작성/부분 편집/재전송·revision/형식·50MiB/최초 표 선택도 확정. 과거 복수 초안 채택은 현재 단일 작성으로 대체돼 재질문하지 않음 |
| 업로드 초안 목록/선택 재개 | 사용자가 목록/선택 재개를 이번 범위에서 제외했다. 기존 초안 ID의 소유 조회·현재 작성 취소와 구분한다. 업로드 endpoint/성공·실패 DTO/취소·저장은 제공 API §3.3·domain §5.6·DDL §6.10으로 완결했으며 구현/검증 결과는 업로드 stable note가 소유 |
| 오래된 미결 표기 | Dynamaic 표시/저장값은 QRY §5의 사용자 채택으로 이미 확정, CLEAR 별도 허용 질문은 철회. 문서의 당시 미결 문구나 historical work note를 새 사용자 질문으로 세지 않음 |

### 원본 대조 근거와 사용자 결정 완료

- source4개 SHA-256이 baseline manifest와 일치했다. `SUMMARY`의 표시/입력·행/기간 추가·설명과 관련 업무 시트의 헤더/안내/값을 읽기 전용 대조했다. sharedStrings와 worksheet inline 문자열에서 초기값/미입력/default/initial/blank/자동 생성 문구도 확인했다. 기존 숫자·빈칸 관측과 신규 입력 생성 정책을 구분한다.
- SUPPLY 안내에는 빈칸/0의 Base·override 의미와 Regas의 BC/12 연결 기본값이 있고 TRADE 안내에는 빈칸/0이면 Base를 고르는 비용 표 등이 있다. 이는 U1의 저장 초기값 결정으로 바꾸지 않는 기존 업무 의미다. 이미 채택한 BCRegasification 신규 수치0/필수와 다른 표의 확정 기본값도 U1로 덮어쓰지 않는다.
- **사용자 결정 완료:** U1/U2를 모두 추천안으로 채택했다. 기존 기본값·필수값·자동 계산 규칙 유지도 명시적으로 재확인했다. 현행 사용자 필수 결정 미결은0건이다. 두 결정의 업무/API 계약은 위 정본을 따르며 실제 구현·검증 완료나 runtime READY로 자동 승격하지 않는다. 표별 같은 초기값·연도 입력 주체를 다시 질문하지 않는다.

## 요약

| 역할 | 전체 항목 | 화면 표시 | 미표시 | 원문 기간 추가 후보 |
| --- | ---: | ---: | ---: | ---: |
| DEMAND | 79 | 43 | 36 | 6 |
| OUTPUT | 18 | 6 | 12 | 0 |
| SUPPLY | 31 | 17 | 14 | 4 |
| TRADE | 64 | 33 | 31 | 1 |

원문 기간 추가 후보는11항목이다. SUPPLY RegasAdjustment 직접 추가는 이후 채택된 규칙51과 다르므로 직접 추가 제외 방향으로 표시했다. 나머지10항목(연도8·월2)은 사람 확인 후보이며 제품 지원/정책 완비 선언이 아니다.

## 먼저 확인할 차이

| 위치 | SUMMARY 원문 | 기존 채택 기준·검토 방향 |
| --- | --- | --- |
| SUPPLY SUMMARY!H17:I17 | 아래 행 추가 / 연월 추가 | 규칙51: RegasAdjustment 직접 행·열 추가 제외. 행은 BCRegasification에서 연계하고 BCRegasification 연도 추가 때는 Regas 보존 |
| SUPPLY SUMMARY!I15 | Year/Month 입력; Period 자동 생성 | QRY §5.4: LiqUtilAdjustment의 새 Month/Year/Period는 이전 마지막 월 기준 자동 생성. 원문보다 후속 채택을 우선 |
| SUPPLY SUMMARY!I16 | Year/Month 입력; Period 자동 생성 | 2026-10-19 사용자 확정: Year/Month 직접 입력·입력 연월 기준 Period 서버 자동 계산·월 건너뛰기/중복 허용(domain 규칙65). 근거 없는 과거 월·업무 연도 범위 제한 없음(규칙66). 식별·임시 키/성공 대응·열 명령 연결·headerValues·월 열 삭제 허용 확정; 잔여 형식·연계/삭제 영향 검증 미결 |
| OUTPUT SUMMARY!I12 | 세로 연도 행 | BCPrices 연도 추가는 ADD_ROW. ADD_COLUMN 대상 아님 |
| DEMAND SUMMARY!H30 | 국가 행 제한 | 전체 행 추가로 일반화하지 않고 국가/합계 행 구분과 허용 범위 확인 |
| TRADE SUMMARY!I33 | 연도 추가 | 원본 J3:Q3은2023~2030 연도 헤더로 확인. SUMMARY의 월별 문구로 새 월 추가 질문을 만들지 않으며 실제 참조/연계는 BE가 대조 |

## 원본 파일

- DEMAND: `/Users/hoduhomil/source/mip/backend/temp/model-input-baselines/2026-10-07-delivery-261006-v8-v6/source/DEMAND-source.xlsm`, `SUMMARY!A9:J88`, SHA-256 `73fa29ff6fa19652243479a43754a65076364488120a69657178c8311b6ce0bc`.
- OUTPUT: `/Users/hoduhomil/source/mip/backend/temp/model-input-baselines/2026-10-07-delivery-261006-v8-v6/source/OUTPUT-source.xlsm`, `SUMMARY!A9:J27`, SHA-256 `fa8bbda8ffa3e3aac1ce0767d403c9ab51f94806b286535e8de7878d416e9080`.
- SUPPLY: `/Users/hoduhomil/source/mip/backend/temp/model-input-baselines/2026-10-07-delivery-261006-v8-v6/source/SUPPLY-source.xlsm`, `SUMMARY!A9:J40`, SHA-256 `c5ab906edf807dbf2707a9e13d7146a92c7490f054fed1b7d3d42afd4312824e`.
- TRADE: `/Users/hoduhomil/source/mip/backend/temp/model-input-baselines/2026-10-07-delivery-261006-v8-v6/source/TRADE-source.xlsm`, `SUMMARY!A9:J73`, SHA-256 `8274fc25be1a89dac8e57eda64d84593576d7a71524d4b46de035da300cf6f8e`.

## DEMAND

| SUMMARY행 | 업무 시트 / 표 | 화면 | 행 추가 원문(H) | 기간 추가 원문(I) | 기존 확정·확인할 점 | 사람 수정 |
| ---: | --- | --- | --- | --- | --- | --- |
| 11 | GDPPopulationAdjustment / GDP Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 12 | GDPPopulationAdjustment / Population Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 13 | ElectrificationRateAdjustment / Electrification Rate Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 14 | PowerMixAdjustment / Annual Power Mix Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 15 | PowerMixAdjustment / Monthly Power Mix Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 16 | CoalGasAdjustment / Coal Futures Price Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 17 | CoalGasAdjustment / Gas Futures Price Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 18 | WeatherAdjustment / Country HDD and CDD Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 19 | WeatherAdjustment / Country Temperature Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 20 | WeatherAdjustment / Regional HDD and CDD Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 21 | WeatherAdjustment / Regional Temperature Change Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 22 | PMIAdjustment / Monthly PMI Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 23 | PMIAdjustment / PMI Reference Country Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 24 | HydrogenAdjustment / Hydrogen Production Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 25 | LNGBunkeringAdjustment / LNG Bunkering Vessel Count Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 26 | LNGBunkeringAdjustment / LNG Bunkering Fuel Consumption Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 27 | DemandOutputOverwrite / Demand Model Manual Overwrite Sector Input | 입력+조회 | 아래 행 추가 | 고정 | 규칙45: 끝 행 추가·행 삭제; 열 추가 불가 | 미검토 |
| 28 | DemandOutputOverwrite / Demand Model Manual Overwrite Monthly Input | 입력+조회 | 아래 행 추가 | 고정 | 규칙45: 끝 행 추가·행 삭제; 열 추가 불가 | 미검토 |
| 29 | LNG Demand / LNG Demand Mode | 입력+조회 | 기존 1개 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 30 | LNG Demand / Annual LNG Demand Reference | 입력+조회 | 국가 행 제한 | 연도 추가 | 규칙50: 연도 입력·국가90값/합계16 구분; [새 국가 입력 빈칸 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#lng-demand-empty-body-decision-20261019); 조회 보류와 업무 허용 구분 | 부분 확정(2026-10-19): 새 연도 국가별90입력 빈칸 생성·미입력 빈칸 유지·명시적 숫자0 구분; 합계16셀 자동 계산·기존 E:N/E6 정책 유지. 사유=사용자 채택 |
| 32 | BCInput / RE Base Case Gas Demand Data by Sector | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 33 | BCInput / RE Base Case GDP and Population Data | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 34 | BCInput / Power Generation Data | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 35 | BCInput / Power Generation Utilisation Rate | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 36 | BCInput / Car Sales for Electrification Input | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 37 | BCInput / Base Case LNG Bunkering Demand | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 38 | BCInput / Base Case Hydrogen Demand | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 39 | BCInput / Coal and Gas Futures Pricing | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 41 | MacroParametersLogicInput / Selected Macro Parameter Intensities | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 44 | PMIInput / Base Case PMI Input | 입력+조회 | 아래 행 추가 | 연도 추가 | [새 연도 본문 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#pmi-empty-body-decision-20261019)·[API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#pmi-empty-body-api-decision-20261019) | 부분 확정(2026-10-19): 새 연도 사용자 입력 본문 빈칸 생성·미입력 빈칸 유지·명시적 숫자0 구분; 기존 PMI 값/빈칸 보존. 사유=사용자 채택. 행/기타 구조 정책은 이번 결정 대상 아님; U2 새 연도 직접 입력 확정 |
| 46 | PMILogicInput / User-Adjusted PMI Intensity Parameter | 입력+조회 | 선택표 동시 확장 | 고정 | 연계 표와 같은 요청 성공/실패 범위 확인 | 미검토 |
| 47 | PMILogicInput / Base Case PMI Intensity Parameter | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 49 | WeatherLogicInput / User-Adjusted HDD-Gas Intensity Input | 입력+조회 | 선택표 동시 확장 | 고정 | 연계 표와 같은 요청 성공/실패 범위 확인 | 미검토 |
| 50 | WeatherLogicInput / User-Adjusted CDD-Gas Intensity Input | 입력+조회 | 선택표 동시 확장 | 고정 | 연계 표와 같은 요청 성공/실패 범위 확인 | 미검토 |
| 51 | WeatherLogicInput / Base Case HDD-Gas Intensity Input | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 52 | WeatherLogicInput / Base Case CDD-Gas Intensity Input | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 61 | WeatherInput / Base Case HDD Input | 입력+조회 | 아래 행 추가 | 연도 추가 | [새 연도 본문 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#hdd-empty-body-decision-20261019)·[API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#hdd-empty-body-api-decision-20261019) | 부분 확정(2026-10-19): 새 연도 사용자 입력 본문 빈칸 생성·미입력 빈칸 유지·명시적 숫자0 구분; 기존 HDD 값/빈칸·Helper 보존. 사유=사용자 채택. 행/기타 구조 정책은 이번 결정 대상 아님; U2 새 연도 직접 입력 확정 |
| 62 | WeatherInput / Base Case CDD Input | 입력+조회 | 아래 행 추가 | 연도 추가 | [새 연도 본문 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#cdd-empty-body-decision-20261019)·[API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#cdd-empty-body-api-decision-20261019) | 부분 확정(2026-10-19): 새 연도 사용자 입력 본문 빈칸 생성·미입력 빈칸 유지·명시적 숫자0 구분; 기존 CDD 값/빈칸·Helper 보존. 사유=사용자 채택. 행/기타 구조 정책은 이번 결정 대상 아님; U2 새 연도 직접 입력 확정 |
| 65 | WeatherInput / Base Case Temperature | 입력+조회 | 아래 행 추가 | 연도 추가 | [새 연도 본문 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#temperature-empty-body-decision-20261019)·[API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#temperature-empty-body-api-decision-20261019) | 부분 확정(2026-10-19): 새 연도 사용자 입력 본문 빈칸 생성·미입력 빈칸 유지·명시적 숫자0 구분; 기존 Temperature 값/빈칸·Helper 보존. 사유=사용자 채택. 행/기타 구조 정책은 이번 결정 대상 아님; U2 새 연도 직접 입력 확정 |
| 67 | LNGBunkeringInput / Base Case LNG Bunkering Input | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 69 | LNGBunkeringLogicInput / LNG Bunkering Share by Country | 입력+조회 | 아래 행 추가 | 연도 추가 | SUMMARY 기준 후보 | 부분 확정(2026-10-19): U1에 따라 새 연도 사용자 입력 본문 빈칸 생성·미입력 유지·명시적 숫자0 구분; U2에 따라 새 연도 직접 입력. 기존 기본값·필수값·자동 계산 우선. 사유=사용자 공통 정책 채택 |
| 71 | HydrogenInput / Base Case Hydrogen Production Input | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 73 | HydrogenInput / Hydrogen Gas Conversion Factor | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |

## OUTPUT

| SUMMARY행 | 업무 시트 / 표 | 화면 | 행 추가 원문(H) | 기간 추가 원문(I) | 기존 확정·확인할 점 | 사람 수정 |
| ---: | --- | --- | --- | --- | --- | --- |
| 10 | USCostofSupply / US Cost of Supply | 입력+조회 | 현재 범위 내 | 현재 코드 고정 | SUMMARY 기준 후보 | 미검토 |
| 12 | BCPrices / Base Case Prices | 입력+조회 | 아래 행 추가 | 세로 연도 행 | 규칙50/6.1: 연도는ADD_ROW; 행삭제 미확정 | 미검토 |
| 13 | PremiumCalc / PremiumCalc TTF | 입력+조회 | 현재 범위 내 | 현재 코드 고정 | SUMMARY 기준 후보 | 미검토 |
| 14 | PremiumCalc / PremiumCalc East Asia Spot | 입력+조회 | 현재 범위 내 | 현재 코드 고정 | SUMMARY 기준 후보 | 미검토 |
| 19 | AnnualPriceCalc / AnnualPriceCalc Spread | 입력+조회 | 기존 1개 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 27 | PriceSeasonality / Price Seasonality | 입력+조회 | 기존 3개 | 월 1~12 고정 | SUMMARY 기준 후보 | 미검토 |

## SUPPLY

| SUMMARY행 | 업무 시트 / 표 | 화면 | 행 추가 원문(H) | 기간 추가 원문(I) | 기존 확정·확인할 점 | 사람 수정 |
| ---: | --- | --- | --- | --- | --- | --- |
| 11 | ProductionAdjustment / PNG Production Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 14 | {USA}Hurricanes / Monthly Hurricane Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 15 | LiqUtilAdjustment / Monthly LNG Train Utilization | 입력+조회 | 아래 행 추가 | 오른쪽 월 추가: Year/Month 입력; Period 자동 생성 | 규칙46/47·QRY5.4/5.5: 월3헤더 자동·삭제값 보존; [본문 정책](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqutil-empty-body-decision-20261019)·[API](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#liqutil-empty-body-api-decision-20261019) | 부분 확정(2026-10-19): 새 월 본문 빈칸 기본·미입력 유지·명시적 숫자0 구분; 사유=사용자 채택. 기존 숫자값 보존. 행 정책은 이번 결정 대상 아님 |
| 16 | LiqProdAdjustment / Monthly LNG Train Production | 입력+조회 | 아래 행 추가 | 오른쪽 월 추가: Year/Month 입력; Period 자동 생성 | [규칙65](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-header-decision-20261019)·[API 채택 범위](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#liqprod-month-input-20261019); 식별·임시 키/성공 대응·열 명령 연결·headerValues·월 열 삭제 허용 확정; 잔여 형식·연계/삭제 영향 검증 미결 | 부분 확정(2026-10-19): 열 헤더=Year/Month 직접 입력·Period는 입력 연월 기준 서버 자동 계산; 월 건너뛰기/중복 허용·근거 없는 과거 월/연도 범위 제한 없음; 새 열도 기존 fieldKey 방식으로 구별; clientColumnKey로 같은 저장의 열/값 연결·성공 columnMappings에서 실제 키 확인; 기존 operations의 ADD_COLUMN/DELETE_COLUMN 채택; 선택 월의 헤더/본문 삭제 허용·B:F 유지(fieldKey별 구별); headerValues에 year/month 전달·Period 제외; 같은 저장의 임시 키 추가→삭제 허용·최종 열 제외·성공 삭제 상태 표시; 성공 mapping 다섯 필드/ACTIVE·DELETED 채택·삭제된 새 열도 fieldKey 유지; 작업 진행/실패·신규 열 추가 없는 성공의 mapping=[]; 삭제 키 미재사용·동일 요청 재전송 기존 키 유지; 표 capability/열 policyStatus의 열 동작 추가·기존 상태/READY로 버튼 제어; 새 월 본문 빈칸 기본·미입력 빈칸 유지·명시적 숫자0 구분; 사유=사용자 입력 허용·제한 근거 원칙(규칙66) 및 fieldKey/임시 키 연결 채택. 행 정책은 이번 결정 대상 아님 |
| 17 | RegasAdjustment / Monthly Regas Capacity Adjustment | 입력+조회 | 아래 행 추가 | 연월 추가 | 규칙51: 직접 행·열 추가 제외; BCRegasification 연계 | 미검토 |
| 18 | RegasAdjustment / Annual Regas Capacity by Country | 조회 | 연결값 | 고정 | 조회 전용; 직접 추가 대상 제외 | 미검토 |
| 19 | RegasAdjustment / Monthly Regas Capacity by Country | 조회 | 연결값 | 고정 | 조회 전용; 직접 추가 대상 제외 | 미검토 |
| 20 | PipelineAdjustment / Monthly Pipeline Capacity Adjustment | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 21 | BCProduction / Annual Gas Production Baseline | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 23 | BCHurricanes / Hurricane Impact on Upstream Assets | 입력+조회 | 현재 고정 범위 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 24 | BCHurricanes / Hurricane Impact on Liquefaction Plants | 입력+조회 | 기존 강도 4단계 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 25 | BCHurricanes / Hurricane Affected Provinces | 입력+조회 | 현재 고정 범위 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 26 | BCHurricanes / Hurricane Affected LNG Plants | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 27 | BCLiquefaction / Monthly LNG Train Baseline | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 28 | BCRegasification / Annual Regas Capacity Baseline | 입력+조회 | 아래 행 추가 | 연도 추가 | 규칙51: 행은Regas연계; 연도는MAX이동·Regas보존 | 부분 확정(2026-10-19): U2 새 연도 직접 입력; 규칙51의 신규 수치0/필수·MAX 갱신·Regas 보존 유지. 사유=사용자 공통 정책 채택 |
| 29 | BCPipeline / Annual Pipeline Capacity Baseline | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 30 | LNG Summary / LNG Production Reconciliation | 조회 | 조회 전용 | 기존 연도 헤더 | 조회 전용; 직접 추가 대상 제외 | 미검토 |

## TRADE

| SUMMARY행 | 업무 시트 / 표 | 화면 | 행 추가 원문(H) | 기간 추가 원문(I) | 기존 확정·확인할 점 | 사람 수정 |
| ---: | --- | --- | --- | --- | --- | --- |
| 10 | LNG_TradeRestrictionInput / LNG Restriction | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 11 | PNG_TradeRestrictionInput / PNG Restriction | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 13 | MidstreamContractInput / Forecasted PNG Contract Volume (Imports) | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 19 | MidstreamContractInput / LNG Contract Volumes | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 20 | ShippingCostInput / Pacific Charter Cost | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 21 | ShippingCostInput / Atlantic Charter Cost | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 22 | ShippingCostInput / Fuel Cost - IFO380 | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 23 | ShippingCostInput / Suez Canal Cost | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 24 | ShippingCostInput / Panama Canal Cost | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 25 | ShippingCostInput / Suez Canal Disruption | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 26 | ShippingCostInput / Panama Canal Disruption | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 27 | ShippingCostInput / Shipping Route Parameters | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 28 | ShippingCostInput / Importer Classification | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 29 | ShippingCostInput / Technical Parameters | 입력+조회 | 현재 표 범위 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 30 | ShippingCostInput / Panama Spot Capacity | 입력+조회 | 현재 표 범위 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 32 | ShippingCostInput / Exporter List | 입력+조회 | 표 안에 추가 | 고정 | 빈 슬롯/표 끝 범위 확인; 중간 삽입 승인 아님 | 미검토 |
| 33 | MidstreamCapacityInput / PNG Infrastructure | 입력+조회 | 아래 행 추가 | 연도 추가 | 연도 추가와 월 입력/집계 축·연계 범위 확인 | 부분 확정(2026-10-19): U1에 따라 새 연도 사용자 입력 본문 빈칸 생성·미입력 유지·명시적 숫자0 구분; U2에 따라 새 연도 직접 입력. 기존 기본값·필수값·자동 계산 우선. 사유=사용자 공통 정책 채택 |
| 34 | MidstreamCapacityInput / Pipeline Import Allocation Shares | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 35 | MidstreamCapacityInput / Nord Stream Importer Allocation | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 45 | UpstreamAdjLogicInput / Country Supply Adjustment | 입력+조회 | 현재 고정 범위 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 46 | UpstreamAdjLogicInput / Excess LNG to PNG Countries | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 48 | DemandAdjLogicInput / Premium Market Countries | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 49 | DemandAdjLogicInput / Demand Adjustment Parameters | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 50 | DemandAdjLogicInput / Demand Adjustment Bands | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 51 | CostInput / Regasification Tariff | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 52 | CostInput / Liquefaction Cost | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 53 | CostInput / LNG Upstream Cost | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 54 | CostInput / Non-LNG Upstream Cost | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 55 | CostInput / Pipeline Cost | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 56 | CostInput / LNG Project SRMC | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 63 | 4.5 PriceModelInput / JKM - Base Case | 입력+조회 | 아래로 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 64 | 4.5 PriceModelInput / TTF - Base Case | 입력+조회 | 아래로 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |
| 65 | LNGExportRestictionInput / LNG Export Restriction | 입력+조회 | 아래 행 추가 | 고정 | SUMMARY 기준 후보 | 미검토 |

## 같은 저장의 열 추가 후 삭제 — 사용자 결정 참조

- 열 삭제가 허용된 표에 적용하는 사용자 후속 결정은 [업무 정본 §5.1](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#column-add-delete-decision-20261019), 공개 요청/성공 응답은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-add-delete-api-decision-20261019)을 따른다. 원본 SUMMARY에서 삭제 정책을 추론한 것이 아니며 다른 표의 삭제 허용을 넓히지 않는다.

## Period 관측 근거 — LiqProdAdjustment

<a id="liqprod-period-evidence-20261019"></a>

- 2026-10-19 읽기 전용 관측: 위 고정 SUPPLY 원본의 `LiqProdAdjustment!G7:CL9`에는2025년1월/85부터2031년12월/168까지84개월 헤더가 있다. 모든 저장값이 `85 + (year - 2025) × 12 + (month - 1)`과 일치한다. Period 행 자체는 수식이 없는 숫자다. 이 관측은 원본 수식 추출이 아닌 저장값 대조다.
- 별도 사용자 결정: 같은 날 사용자가 입력 연월 기준 계산1안을 채택했다. 산식과 적용 경계는 [업무 정본 규칙65](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-header-decision-20261019)를 따른다. 관측과 사용자 결정은 실제 신규 열 생성/계산 검증 완료를 뜻하지 않는다.

## 입력 제한 근거 관측 — 2026-10-19

- 고정 SUPPLY source hash가 manifest와 일치했다. SUMMARY!H16:J16과 LiqProdAdjustment의 안내/문자열, 셀 입력 제한 설정(dataValidation)을 읽기 전용 확인했다. SUMMARY는 Year/Month 입력·Period 자동 생성을 명시하며 중복 금지·과거 월 금지·업무 연도 범위를 명시하지 않는다. 해당 시트의 셀 입력 제한 설정은0개이며 안내/문자열에도 위 제한이나 삭제 정책을 확인하지 못했다. 이 관측 자체가 삭제 허용이나 실제 모델 동작 검증의 근거는 아니다. 삭제는 별도 후속 사용자 결정으로 규칙65에 반영했다.

## LiqProd 새 월 본문 초기값 — 원본 관측

- 2026-10-19 읽기 전용 관측: 고정 SUPPLY source hash가 manifest와 일치했다. LiqProdAdjustment!G11:CL407의33348개 본문 셀은 서식만 있는 빈 셀이며 값·수식·inline 문자열 내용이 없다. 이 관측은 원본 사실이며, 같은 날 후속 사용자 결정으로 새 월 본문 빈칸 기본·미입력 유지·숫자0 구분을 [업무 정본 규칙65](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-empty-body-decision-20261019)에 반영했다. 원본 관측과 사용자 채택·제품 구현/검증을 구분한다.

## LiqUtil 새 월 본문 초기값 — 원본 관측

- 2026-10-19 읽기 전용 관측: 같은 고정 SUPPLY 원본의 SUMMARY!H15:J15는 오른쪽 월 추가와 헤더 입력/생성을 설명하지만 본문 초기값은 명시하지 않는다. LiqUtilAdjustment!G11:CL407의33348개 본문 셀은 기존 숫자값으로 채워져 있고0과 다른 숫자가 함께 있다. 원본의 기존 숫자는 신규 월을 빈칸/0/1/직전 월 복사 중 어느 방식으로 생성할지 확정하는 근거가 아니며, 기존 Util 헤더 자동3규칙에서도 본문 초기값을 추론하지 않는다. 같은 날 후속 사용자 결정으로 새 월 본문 빈칸 기본·미입력 유지·숫자0 구분을 규칙46에 반영했으며 기존 숫자값은 보존한다.

## CLEAR 질문 근거 재확인 — 파일 요구사항 아님

- 2026-10-19 고정 SUPPLY source hash 일치 후 SUMMARY!H15:J16과 LiqUtilAdjustment/LiqProdAdjustment의 안내·문자열을 읽기 전용 확인했다. 입력 내용/사용자 수정 안내와 행·월 추가 설명은 있으나 CLEAR의 별도 허용/금지·빈 값 필수 검증 문구는 확인하지 못했다. 두 시트의 dataValidation 설정도 각각0개다. 이는 확인한 원문/설정에 대한 관측이며 모델 실행 영향 검증은 아니다.
- 별도 허용/금지 승인 질문은 원본 요구가 아니라 assistant가 추가한 설계 질문이었다. 기존 규칙66에 따라 필수 미결/추가 승인 조건으로 두지 않는다. 기존 CLEAR 계약·실제 필드/계산/보존 준비도는 구분하며 사용자 문의를 새 정책 승인으로 기록하지 않는다.

## 새 열 생성 안내 — 사용자 결정 참조

- 2026-10-19 사용자가 `editing.columnCreation`에 기존 타입·초기값 표현을 재사용하는 방식을 채택했다. 입력 헤더 정의/본문 초기값 안내의 API 계약은 [편집 API §1.1/§3.3](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-creation-metadata-decision-20261019)이 소유한다. 두 월 표의 확정 정책을 전달하며 새 입력 제한·필수값/추가 승인 조건을 만들지 않는다. 원본과 기존 계약으로 확인되는 내용은 재질문하지 않는다.

<a id="lng-demand-empty-body-evidence-20261019"></a>

## LNG Demand 새 연도 국가별 입력 — 원본 관측과 사용자 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H30:J30은 연도 추가·국가행 연간 물량 입력·합계 수식 보호를 설명하지만 새 국가 입력의 초기값/미입력 처리는 명시하지 않는다. LNG Demand의 마지막 N열 국가90칸은 기존 숫자값이며 다음 O열의 해당90칸은 원본에 존재하지 않는다. 이 관측만으로 새 열의 빈칸/0/직전 값 복사를 결정하지 않는다.
- 위 원본 관측과 별도로 2026-10-19 사용자가 새 국가90입력을 빈칸으로 생성·미입력 빈칸 유지·숫자0은 명시적 입력으로 구분하도록 확정했다. 업무 정책은 [규칙50의 후속 결정](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#lng-demand-empty-body-decision-20261019), 제출/초기값 표현은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#lng-demand-empty-body-api-decision-20261019)이 소유한다. 이 초기값은 더 이상 미결이 아니다. 기존 합계16셀 자동 계산·E:N 보존·E6의 별도 정책은 유지한다.

<a id="pmi-empty-body-evidence-20261019"></a>

## PMIInput 새 연도 본문 — 원본 관측과 사용자 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H44:J44는 행 추가·연도 추가와 국가·월·연도별 PMI를 명시하지만 새 연도 본문 초기값은 정하지 않는다. PMIInput 안내 D1은 `PMI by country and year-month`, D2는 `Rystad Energy`다. 기존 Z8:AK8은2024~2035이며 본문에는50 등 숫자와 빈칸이 함께 있다(예: Z9:AG9=50, AH9:AK9=빈칸). 시트의 dataValidation 설정은0개다. 기존50을 새 연도 생성 기본값으로 지정하는 안내는 확인하지 못했다.
- 위 관측과 별도로 2026-10-19 사용자가 PMIInput 새 연도 본문도 빈칸으로 생성·미입력 빈칸 유지·숫자0은 명시적 입력으로 구분하도록 채택했다. 업무 정책은 [domain §6.2](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#pmi-empty-body-decision-20261019), 공개 제출/초기값 표현은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#pmi-empty-body-api-decision-20261019)이 소유한다. 이 초기값은 더 이상 미결이 아니다. 기존50이나 다른 값을 새 열에 복사하지 않으며 기존 값/빈칸은 보존한다.

<a id="hdd-empty-body-evidence-20261019"></a>

## Base Case HDD 새 연도 본문 — 원본 관측과 사용자 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H61:J61은 행·연도 추가 및 Helper 제외/연도 열만 확장을 명시하지만 신규 연도 본문 초기값은 정하지 않는다. WeatherInput D1은 `[Weather] HDD and CDD by country and year-month`, D2는 `Rystad Energy`다. Base Case HDD의 기존 BZ8:CK8은2024~2035이며 BZ9:CG9는 같은 숫자513.01629881154508, CH9:CK9는 빈칸이다. 안내/문자열·셀 입력 제한 설정에서 신규 기본값 지정은 확인하지 못했다(dataValidation0개). 기존 연도값의 반복/빈칸은 신규 생성 규칙과 구분한다.
- 위 관측과 별도로 2026-10-19 사용자가 Base Case HDD 새 연도 본문을 빈칸 생성·미입력 빈칸 유지·숫자0은 명시적 입력으로 구분하도록 채택했다. 업무 정책은 [domain §6.3](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#hdd-empty-body-decision-20261019), 공개 제출/초기값 표현은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#hdd-empty-body-api-decision-20261019)이 소유한다. 이 초기값은 더 이상 미결이 아니며 기존 값/빈칸·Helper는 보존한다.

<a id="cdd-empty-body-evidence-20261019"></a>

## Base Case CDD 새 연도 본문 — 원본 관측과 사용자 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H62:J62는 행·연도 추가 및 Helper 제외/연도 열만 확장을 명시하지만 새 연도 본문 초기값은 정하지 않는다. WeatherInput 안내/문자열에도 신규 기본값 지정은 확인하지 못했다. CDD 표의 CR8:DC8은2024~2035이며 CR9:CY9는 숫자0, CZ9:DC9는 빈칸이다. 시트 dataValidation은0개다. 기존0/빈칸 관측은 새 연도 생성 규칙과 구분한다.
- 위 관측과 별도로 2026-10-19 사용자가 Base Case CDD 새 연도 본문을 빈칸 생성·미입력 빈칸 유지·숫자0은 명시적 입력으로 구분하도록 채택했다. 업무 정책은 [domain §6.4](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#cdd-empty-body-decision-20261019), 공개 제출/초기값 표현은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#cdd-empty-body-api-decision-20261019)이 소유한다. 이 초기값은 더 이상 미결이 아니며 기존 값/빈칸·Helper는 보존한다.

<a id="temperature-empty-body-evidence-20261019"></a>

## Base Case Temperature 새 연도 본문 — 원본 관측과 사용자 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H65:J65는 행·연도 추가 및 Helper 제외/연도 열만 확장을 명시하지만 새 연도 본문 초기값은 정하지 않는다. WeatherInput 안내/문자열에도 신규 기본값 지정은 확인하지 못했다. Temperature 표의 EM8:EX8은2024~2035이며 EM9:ET9는 같은 숫자0.89945670628183327, EU9:EX9는 빈칸이다. 시트 dataValidation은0개다. 기존 숫자/빈칸 관측은 새 연도 생성 규칙과 구분한다.
- 위 관측과 별도로 2026-10-19 사용자가 Base Case Temperature 새 연도 본문을 빈칸 생성·미입력 빈칸 유지·숫자0은 명시적 입력으로 구분하도록 채택했다. 업무 정책은 [domain §6.5](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#temperature-empty-body-decision-20261019), 공개 제출/초기값 표현은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#temperature-empty-body-api-decision-20261019)이 소유한다. 이 초기값은 더 이상 미결이 아니며 기존 값/빈칸·Helper는 보존한다.

## LNG Bunkering Share by Country 새 연도 본문 — 원본 관측과 U1 결정

- 2026-10-19 고정 DEMAND source hash가 manifest와 일치했다. SUMMARY!H69:J69는 행·연도 추가 및 국가·연도별 배분율을 명시하지만 새 연도 본문 초기값은 정하지 않는다. LNGBunkeringLogicInput D1은 `Input on Regional LNG Bunkering Demand - Country Share`, D2는 `Rystad Energy`다. E7:Q7은2023~2035이며 Canada 행에는 E8=0, F8~L8에 다른 숫자, M8:Q8에 빈칸이 있다. 안내/문자열에 신규 기본값 지정은 확인하지 못했고 dataValidation은0개다. 기존 값/빈칸 관측은 새 연도 생성 규칙과 구분한다.
- 원본 관측 당시 초기값은 미정이었으나 사용자가 U1을 채택해 이 표의 신규 사용자 입력 본문도 공통 빈칸 생성·미입력 유지·명시적 숫자0 구분으로 확정됐다. 새 연도 직접 입력도 U2로 확정했다. 기존 명시 기본값·필수값·자동 계산 규칙은 우선하며 업무/API 정본 §6.6/§1.1을 따른다. 이 초기값/입력 주체는 더 이상 미결이 아니다.

## Open Questions — LiqProdAdjustment 후속 결정

- 신규 월의 입력 주체·Period 산식·월 건너뛰기/중복 허용은 규칙65로 확정했다. 근거 없는 과거 월·업무 연도 범위 제한을 추가할지 다시 묻지 않고 규칙66을 적용한다. 새 열의 fieldKey·clientColumnKey 연결·성공 columnMappings 대응은 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-fieldkey-decision-20261019)로 확정했다. 기존 operations의 ADD_COLUMN/DELETE_COLUMN 채택은 [편집 API §1.1 열 명령](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-operations-decision-20261019)을 따른다. 남은 항목은 상세 요청/응답·연결·계산/보존·JSON/타입 표현이다. headerValues 객체와 두 월 표의 전달 형식은 [편집 API §1.1 헤더값](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-header-values-decision-20261019)으로 확정했다. LiqProd 월 열 삭제의 허용/범위는 [업무 정본 규칙65](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-delete-decision-20261019)로 확정했다. 삭제 구현의 수식/연결·계산/보존 검증은 잔여이며 허용 여부를 미결로 되돌리지 않는다. 같은 저장의 새 열 추가 후 삭제 허용과 성공 응답의 삭제 상태 표시 의미는 [편집 API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-add-delete-api-decision-20261019)로 확정했다. 성공 mapping의 다섯 필드/ACTIVE·DELETED 및 삭제된 새 열의 fieldKey 유지는 [편집 API §1.1 항목 형식](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-mapping-item-decision-20261019)으로 확정했다. 진행/실패/신규 열 추가 없는 성공의 빈 배열 통일과 추가 후 삭제 DELETED 항목 유지는 [편집 API §1.1 빈 배열](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-empty-mappings-decision-20261019)로 확정했다. 삭제 키 미재사용·같은 연월 재추가 새 키·동일 requestId 재전송 기존 키 유지는 [편집 API §1.1 키 수명](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-key-lifecycle-api-decision-20261019)과 domain §5.1로 확정했다. 성공 후 구조/표 재조회도 기존 API §2의 확정 사항으로 재질문하지 않는다. 열 동작의 표 capability/열 policyStatus·상태값/버튼 제어는 [편집 API §1.1 metadata](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-editing-metadata-decision-20261019)로 확정했다. 새 열 생성 안내는 [편집 API의 editing.columnCreation 채택](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#column-creation-metadata-decision-20261019)에 따라 기존 표현을 재사용하도록 확정했다. LiqProd의 새 월 본문 빈칸/미입력 유지·숫자0 구분은 규칙65와 [편집 API §1.1 본문 정책](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#liqprod-empty-body-api-decision-20261019)으로 확정했다. Util도 별도 후속 사용자 결정으로 같은 새 월 본문 정책을 [업무 정본 규칙46](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqutil-empty-body-decision-20261019)과 [API §1.1](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#liqutil-empty-body-api-decision-20261019)에 반영했다. 두 월 표의 초기값은 더 이상 미결이 아니다. 원본 근거가 없는 CLEAR의 별도 허용/금지 질문은 철회하고 필수 미결에서 제거했다. 기존 API §4와 domain 규칙66을 따르며 사용자 문의를 새 CLEAR 정책 승인으로 기록하지 않는다. 새 열 생성 안내 형식(C6)과 신규 입력 시작값/미정 새 연도 입력 주체(U1/U2)는 확정됐다. 남은 자료 대조·JSON·계산/보존·구현 검증은 사용자 승인 대기와 구분한다. API 잔여는 [편집 API C4](../../feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md#fe-column-decisions-20261019)를 따른다.
