# 모델 입력 판별대장

## Document Control

- status: Complete
- canonical for: Reference only — 입력 시트·표 판별 검토 근거
- owner: BE
- source documents: 사용자 첨부 2026-10-06 판별대장, LiqUtilAdjustment/BCLiquefaction 별도 수정본, ProductionAdjustment 최종 승인 기록
- depends on: [모델 입력 카탈로그 설계](../../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md)
- last synced: 2026-10-07
- open questions: v8 입력파일에 따른 정책 변경 후보는 후속 검토 대상이다.

## 보관 파일

[2026-10-07 통합 판별대장](모델인풋_판별대장_2026-10-06_별도수정본통합_2026-10-07.xlsx)은 10월 6일 첨부 대장을 기준으로 LiqUtilAdjustment와 BCLiquefaction의 별도 수정 24개 셀을 통합했다. 첨부 대장의 Regas 확정사항을 유지했다.

2026-10-07에는 ProductionAdjustment의 최종 M3 집계 범위·기준 숫자 동반 증가 승인 내용을 4개 셀에 추가 반영했다. 기존 C 분류·행 추가·L/N/O 정책과 다른 표의 기록을 보존했다.

같은 날 BCPipeline의 C 구조변경·표 마지막 행 아래 행 추가 허용 결정을 표 대장과 시트 대장의 사람 확인 8개 셀에 반영했다. 그 밖의 세부 정책은 미확정으로 유지했다.

같은 날 BCHurricanes의 `Hurricane Impact on Upstream Assets`와 `Hurricane Impact on Liquefaction Plants` 두 표(T-112·T-113)를 B 값만 교체·행 추가 불가로 반영했다. G열 8개 수식은 외부 LUT의 파일 내부 캐시를 이용한 재계산 대상으로 기록했다.

후속 사용자 확인으로 `Hurricane Affected Provinces`(T-114)는 B 값만 교체, `Hurricane Affected LNG Plants`(T-115)는 C 구조변경으로 반영했다. 두 표의 수식은 각각 0개다. LNG Plants의 행 추가는 표 마지막 데이터 행 아래이며 상세 write 정책은 별도다. 시트 단위 C는 B 표 3개와 C 표 1개의 요약이며 표별 허용 범위를 따른다.

`BCProduction`의 `Production Year Count Reference`(T-111)는 사용자 확인에 따라 A 원본 복제·미표시로 확정했다. 같은 시트의 기준표에 행을 추가해도 이 참조표의 값·수식·저장된 계산값과 관련 피벗은 갱신하지 않는다.

최신 4파일 원본 `SUMMARY`의 미표시 93개 항목에 대응하는 대장 94행(DEMAND 37·OUTPUT 12·SUPPLY 14·TRADE 31)을 사용자 확인에 따라 A 원본 복제·표기 안 함으로 맞췄다. DEMAND 백업 1개 항목은 대장 2행에 대응한다. 기존 일치 21행은 유지하고 73행을 갱신했다. 워크북 밖 참조 3행은 분류 기록이며 새 XLSM 시트·셀 복제를 뜻하지 않는다. 업무 정책은 domain 규칙 57, 조회 제외는 input-provision feature spec을 따른다.

BCInput T-023~T-030 8표와 S-14 시트는 최종 v8에 대한 사용자 결정으로 C 구조변경·입력값 편집·표 마지막 행 아래 추가·열 고정으로 반영했다. 신규 AE:AG·X 수식은 생성하지 않고, 기존 AE:AF는 재계산하지 않으며, 저장값 Helper는 생성·갱신하지 않는다. 현재 입력 범위·화면 표기와 제외 열 정책은 사람 확인 항목에 기록했고, 이전 A 분류 메모는 과거 기록으로 구분해 보존했다. 업무 정본은 domain 규칙 58을 따른다.

Trade v8 `SUMMARY`의 64개 항목을 현재 대장에 반영했다. 표시 33표는 입력 범위·고정 헤더·행/기간 추가·화면 표기를 갱신하고 C 구조 변경 30표, B 값만 교체 3표로 기록했다. `현재 표 범위`는 사용자 결정대로 행 추가 불가이며 Technical Parameters·Panama Spot Capacity에 적용했다. Country Supply Adjustment도 고정 범위로 기록했다. Demand Adjustment Parameters는 행 추가를 반영하되 신규 행 종류·계산 연결을 미확정으로 남겼다. 미표시 31표는 기존 A 원본 복제·표기 안 함을 유지한다. 이번 변경은 판별 기록이며 업무 설계·모델 계산 구현의 변경 완료를 뜻하지 않는다.

2026-10-08 UNIQUE 보존 경계를 보완했다. T-137/T-138의 UNIQUE 수식·배열 결과와 T-187/T-188의 시트 전체 A 보존을 명시하고 기존 T-135/T-136의 A 결정을 유지했다. T-139 `AG/AI`와 표 아래 `CN/CT`의 원본 보존 또는 보존 기준값을 읽는 부분 재계산은 별도 미확정 질문으로 기록했다. 자동 `서버 계산 가능` 수치와 본 시트 연결 83,254셀 관측은 계산 승인·모델 영향·성공 검증이 아니다. 수식·배열의 값 치환도 승인하지 않았다. 정책은 domain §1A.4 Q5/§6 규칙 57, 관측 근거는 [함수 reference의 UNIQUE 보존 정책](../mi-model-input-functions-2026-10-07.md)과 [INPUT-02A Phase 2](../../work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md)를 따른다.

Output v8 `SUMMARY`의 18개 항목을 기준으로 갱신했다. 표시 6표는 SUMMARY의 노랑 입력 범위·고정 헤더·조회/미표시·행/기간 정책을 반영하며, 과거 파랑 셀 수정 가능 기준을 대체한다. Base Case Prices만 C 구조 변경·아래 연도 행 추가이고 나머지 5표는 B 값만 교체·행 추가 불가이다. East Asia Spot 입력은 SUMMARY대로 C57:C61이다. 미표시 12표는 A 원본 복제·표기 안 함을 유지하며 Helper의 과거 재계산 설명을 최신 보존 기록과 맞췄다. 상세 계산 정책은 SUMMARY가 정의한 것으로 확대하지 않는다.

Demand v8 `SUMMARY`의 79항목(대장 80행)은 사용자 요청에 따라 즉시 반영 가능한 내용만 갱신했다. 일반 조정 16표·BCInput 8표·C1 선택의 기존 승인과 미표시 37행 A를 유지한다. 미기입 15표 중 명확한 11표는 입력·조회·아래 행 추가·기간 정책과 C 분류를 기록했고, 4표는 입력/조회만 반영했다. 선택표 동시 확장·단일 Hydrogen 환산계수 행 추가 및 최종 분류는 미확정이다. Overwrite 2표의 BCM 입력 제외·비우기·추가 위치와 Annual LNG Demand의 지역·국가명 편집 확대는 기존 사람 승인을 유지하며 새 선언과의 차이만 기록했다. BCInput의 수식/저장값 Helper 보존 결정도 유지한다. 이번 변경은 판별 기록이며 업무 설계 정본·실제 입력폼·모델 계산 구현의 변경 완료를 뜻하지 않는다.

대장의 자동 스캔 근거는 DEMAND·OUTPUT·TRADE v3 / SUPPLY v6이며, 최신 v8/v6 기준본에 대한 재스캔 결과로 갱신된 파일은 아니다. Trade·Output의 C/G/J:O와 Demand의 C/G/J:M/O 참고 선언 및 사람 확인은 v8 내용으로 갱신했지만 H:I/P:AZ와 시트 자동 집계·파일 정보는 이전 스캔을 유지한다. OUTPUT의 과거 파랑 셀 수·입력 영향도도 v3 관측이며 현재 입력 허용 근거가 아니다. 최신 허용 범위는 사람 확인을 따르며, 새 SUMMARY 참고 원문과 기존 확정 정책이 충돌하는 항목은 미확정 변경 후보로 구분한다. 상세 write/계산 등 미확정 항목은 그대로 남아 있다. 업무 규칙·API·스키마의 정본은 각각 설계·feature spec·DDL 문서가 소유한다.

`LNG Demand Mode`(T-020)는 사용자 확인으로 새 분류 `E 부속 control`이다. 값 셀 C1의 선택 control을 `Annual LNG Demand Reference`(T-021) 표 데이터와 함께 전달하며 기존 ID·선택값 편집 허용을 유지한다. 표/시트 분류 드롭다운과 안내에도 E를 추가했다. T-021의 C 구조 변경·계산 정책과 원본 `Dynamaic` 오타 처리·선택지 저장값 미결정은 유지한다. 업무 정본은 domain 규칙 50, control 전달 표현은 input-provision spec §3.1B를 따른다.

2026-10-08 `시트 대장`의 시트 단위 분류 미기입 9행을 채웠다. 사용자 기준에 따라 최신 SUMMARY에 행 추가 또는 기간(열) 추가가 선언된 표가 있는 8시트(DEMAND PMILogicInput·WeatherLogicInput·HydrogenInput, SUPPLY {USA}Hurricanes·LiqProdAdjustment·PipelineAdjustment·BCProduction·BCRegasification)는 C 구조 변경이다. RegasAdjustment는 SUMMARY의 행·연월 추가 선언과 달리 2026-10-06 T-106 B 결정을 따른다는 사용자 확인으로 B 값만 교체이다. 시트 단위 값은 표별 분류의 요약이며 표 대장 BS의 미기입·미확정 표에 구조 변경 허용을 확대하지 않는다. 같은 날 SUPPLY v6 SUMMARY C열(화면)을 근거로 화면 표기(AR)가 비어 있던 SUPPLY 10시트를 `표기 (수정 가능)`으로 채웠다. 10시트 모두 `입력+조회` 표를 하나 이상 가진다. 시트 값은 요약이며 같은 시트의 `조회` 전용 표(RegasAdjustment T-107·T-108)나 표별 입력 범위를 바꾸지 않는다. 이어서 사용자 확인으로 표 단위 분류가 비어 있던 SUPPLY T-103·T-105·T-109·T-110·T-117을 C 구조 변경으로 기록했다. 정책 상태는 미확정이며, 행·열 추가 위치와 상세 write 정책은 정하지 않았다. T-117의 행·열 추가 구체 허용 범위는 domain 규칙 51(STRUCT-01) 확정 대상으로 남는다.

- [통합 범위 및 검증 기록](../../work-notes/2026/2026-10-07-model-input-ledger-update-integration.md)
- [ProductionAdjustment 최종 정책 동기화](../../work-notes/2026/2026-10-07-production-adjustment-repository-ledger-sync.md)
- [BCPipeline 분류 반영](../../work-notes/2026/2026-10-07-bc-pipeline-classification-update.md)
- [Production Year Count Reference 미표시·원본 복제 확정](../../work-notes/2026/2026-10-07-production-year-count-reference-preservation.md)
- [BCHurricanes 두 표 분류 반영](../../work-notes/2026/2026-10-07-bchurricanes-table-classification-update.md)
- [BCHurricanes Province·LNG Plant 표 분류 반영](../../work-notes/2026/2026-10-07-bchurricanes-affected-assets-classification-update.md)
- [v6/v8 비교 검토](../../work-notes/2026/2026-10-07-model-input-v6-v8-ledger-comparison.md)
- [SUMMARY 미표시 표 원본 복제 분류 반영](../../work-notes/2026/2026-10-07-summary-hidden-table-copy-ledger-update.md)
- [최신 입력파일 기준본](../mi-model-input-baseline-2026-10-07.md)
- [BCInput v8 수식·참조 및 정책 동기화](../../work-notes/2026/2026-10-07-demand-bcinput-v8-formula-dependency-review.md)
- [Trade v8 SUMMARY 판별대장 반영](../../work-notes/2026/2026-10-07-trade-summary-ledger-update.md)
- [Output v8 SUMMARY 판별대장 반영](../../work-notes/2026/2026-10-07-output-summary-ledger-update.md)
- [Demand v8 SUMMARY 즉시 반영 가능 내용 갱신](../../work-notes/2026/2026-10-07-demand-summary-ledger-update.md)
- [LNG Demand Mode 부속 control 분류 추가](../../work-notes/2026/2026-10-07-lng-demand-control-classification.md)
- [시트 단위 분류 미기입 시트 채우기](../../work-notes/2026/2026-10-08-blank-sheet-classification-fill.md)

이 XLSX는 판별 기록으로 Git 관리 대상이다. 업무 입력 XLSM의 원본·정규화본은 위 기준본 문서에 따른 로컬 보관 위치를 사용한다.
