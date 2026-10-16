# MI 모델 입력 시트 계산 호환성 목록

## Document Control

- status: Draft
- canonical for: Reference only
- owner: BE
- source documents:
  - `docs/references/mi-model-input-catalog-inventory.md`
  - `docs/references/mi-model-input-calculation-engine-spike.md`
  - `docs/design-docs/gas/mi-model-input-editing-delivery-plan.md`
  - 사용자 제공 `모델인풋_판별대장_자동채움_2026-10-06.xlsx` — 2026-10-07 수신
  - `docs/references/mi-model-input-baseline-2026-10-07.md` 및 manifest — 사용자 지정 10월 6일 전달본 v8/v6의 고정 bytes/hash
  - `docs/references/mi-model-input-functions-2026-10-07.md` 및 JSON — 최신 4파일의 전체 함수 관측과 POI 5.4.0 기본 등록 확인
  - `docs/references/model-input/README.md` 및 `mi-model-input-summary-preservation-review-2026-10-08.md` — OUTPUT v8 사람 확인·미표시 A 대응
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §6 규칙 13·26·29~34·39~59 및 §6.1·§1A
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md` §1A — Scenario OUTPUT 원본 보존
- supersedes: 없음
- last synced: 2026-10-15
- open questions:
  - 미확정 표의 계산 allowlist와 구조 변경·수식/서식 복제 계약이 필요하다. 자동 계산 가능 판정은 엔진 함수 지원 검증이 아니다.
  - v8/v6 정규화 source bytes/hash는 고정됐다. v3 대장의 DEMAND·OUTPUT·TRADE 범위·색·수식 관측을 v8과 대조하고 cell별 정책·family·대표 fixture를 재집계해야 한다.
  - 최신 실제 함수 `_XLFN.UNIQUE` 5곳은 2026-10-08 대장 정책 대응에서 모두 원본 보존으로 확인했다. 정책 변경으로 재계산 경로에 포함할 때만 함수/배열 호환성을 재검토한다. GDP/PIPELINE은 최신 스캔의 오탐이며 조건부서식 ISFORMULA는 보존/평가 요구가 별도다.
  - **F-S-06 제외 INPUT-02A는 2026-10-15 사용자 승인으로 `DONE (CONDITIONAL)`**이다. 운영 기준은 domain 규칙63/64, 상태·전달 제약은 Delivery Plan §6A를 따른다. 선정 밖 계산/구조·모델/제품 worker·API 전체 저장 응답은 후속이며 이번 티켓의 대기 항목과 구분한다.

## INPUT-02A 최종 조건부 완료 — 2026-10-15

**범위 한정 채택:** 고정 v8/v6 정규화 source의 선정19사례만 아래 상태로 완료했다. 사용자 최종 승인과 운영 기준·INPUT-03/04/07 전달 제약 및 API 전체 저장 응답 후속 티켓은 [Delivery Plan §6A](../design-docs/gas/mi-model-input-editing-delivery-plan.md#6a-completed-ticket--input-02a-conditional-f-s-06-제외)가 소유한다. [종료 기록](../../temp/input-02a-engine/2026-10-15-input-02a-conditional-closeout-c2mlcayi/final-summary.json)·[문서/자료 불변 검증](../../temp/input-02a-engine/2026-10-15-input-02a-conditional-closeout-c2mlcayi/verification.json)·[stable work note](../work-notes/2026/2026-09-03-model-input-transfer-format.md)를 연결한다.

| 사례 | 입력·구조/보존 | 실제 계산 | 동일 변경 Excel 정답 | 읽기 전용 native 재개방 |
| --- | --- | --- | --- | --- |
| F-D-02, F-S-01/02/03, F-D-03, F-S-04-COLUMN, F-S-05-COLUMN, F-S-04-ROW, F-S-04-DELETE (9) | Pass (각 승인 범위) | Pass | Pass | Pass (각 기록 범위) |
| F-O-01/02, F-T-01/02, F-D-01, F-T-04, F-O-04, F-S-05-ROW, F-D-05-APPEND/DELETE (10) | Pass (각 승인 범위) | 해당 없음(PRESERVE) | 해당 없음 | Pass (각 기록 범위) |
| F-O-03, F-D-04, F-T-03 | 허용 CLEAR 사례 해당 없음: 빈 값 금지 | 해당 없음 | 해당 없음 | 해당 없음(허용 파일 미생성) |
| F-S-06 | 이번 종료 범위 제외 | 제외 | 제외 | 제외 |

- 선정19의 정책·승인 계산/Excel·native 대기0, 지정 허용 사례의 미해결 Fail0이다. CLEAR3의 제품 거부/기본값 적용은 INPUT-07/08→10에서 검증한다. C2는0 입력 허용이며 자동 기본값0으로 새로 결정하지 않는다. E6/U5는 사용자 결정 기본값0을 따른다. 기존 F-O-03의 빈 셀 실행/기대값 기록은 승인 취소 전 이력이며 현재 성공 사례로 합산하지 않는다.
- 승인 운영 기준은 domain 규칙64의 worker1개/2vCPU·2GiB·동시1·역할 순차/비동기·저장 목표30초/요청 전체120초 상한, 평문 수명·삭제는 규칙63이다. 아래 자원21회/격리·중단6시험/평문10시험·제한환경 인정12회 근거를 채택했다. 실제 DRM 암복호화 재검증은 **해당 없음(사용자 제외)**이며 Pass가 아니다.
- 검증 한계: 제한환경 대표4경로 각3회는 warm cache·Linux/tmpfs의 로컬 파일 처리 구간이다. 대표 최대14.564초/worker kernel peak666.5MiB·OOM0을 같은4파일 저장 요청의 처리시간이나 p95/SLA로 확대하지 않는다. API/대기열·DB/storage 저장 확정과 상태/부분 실패는 **INPUT-10 후속 PENDING**(계약 INPUT-04, 구현 INPUT-07~09)이다. OS reboot/정전·접근 불가 사용자 핸들/물리 매체 삭제·native 전수 재계산/저장·매크로/모델 결과는 미검증이다.
- **이 절이 현재 종료 상태다.** 아래 단계별 `In Progress`·미승인 추천·운영/DRM/최종 판정 대기 문구와 기존 JSON은 당시 이력으로 보존한다. 일반 미검증 표의 계산/구조 정책을 자동 승인하지 않으며 전체 입력 기능·조사·배포 완료를 선언하지 않는다.

## 현재 검증 기준 — 2026-10-07 판별대장 반영

**기준본 고정 완료:** 사용자가 10월 6일 전달본 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6을 선택했다. 실제 원본 hash 확인, 개발 전용 시트 제거, 70개 업무 worksheet 및 유지 package bytes 보존 검사를 완료했다. 실행 검증에는 [고정 결과](mi-model-input-baseline-2026-10-07.md)의 정규화본·manifest hash를 사용한다. 아래 v3 대장은 정책 근거이며 DEMAND·OUTPUT·TRADE의 좌표·색·수식 관측과 fixture는 v8 재대조 전 실행 확정값으로 사용하지 않는다. SUPPLY는 대장 v6과 원본 hash가 같다.

이 절이 현재 검증 계획이다. 아래 「이전 3파일 실측·검증 계획」의 숫자·좌표·family ID·TC-SRC 목록은 역사적 evidence이며 현재 실행 parameter로 사용하지 않는다. 검증 목록 갱신은 계산 성공이나 구현 완료를 뜻하지 않는다. 업무 허용/보존 규칙은 domain design §6이 소유하고 이 문서는 그 규칙을 확인할 검증 범위·기대값을 소유한다.

### 최신 전체 함수·POI 지원 조사 — 2026-10-07

[함수 조사 reference](mi-model-input-functions-2026-10-07.md)와 [JSON](mi-model-input-functions-2026-10-07.json)은 고정 v8/v6 4파일·70개 시트·1,863,766개 셀 수식과 별도 수식 위치를 조사한 최신 기술 근거다. shared follower를 포함해 함수 29종을 확인했고 POI 5.4.0의 기본 구현 등록은 27종이다. `_XLFN.UNIQUE`는 셀 수식 5곳, `_XLUDF.ISFORMULA`는 조건부서식 16규칙에 있으며 두 함수의 기본 등록은 없다. GDP/PIPELINE은 문자열 등에서 나온 오탐으로 이번 실제 함수 목록에 없다.

전체 함수 목록은 승인 계산 allowlist가 아니다. 조건부서식·Table 계산열·보존 표의 함수까지 조사했으므로 표별 정책과 매핑한 뒤 `CALCULATE/PRESERVE/PENDING` 검증 경로를 정한다. POI 기본 등록은 실제 함수 조합의 정답·전체 workbook 로딩/보존 검증 통과가 아니다. 기존 `INPUT-02` 전체 workbook 경로의 `NO-GO`는 유지하며 최신 source 네 사례의 부분 실행은 아래 별도 결과를 따른다. 다른 계산 경로는 미검증이다. 대장 사람 확인 정책과 자동 스캔 칸은 이번 조사에서 수정하지 않았다.

2026-10-08 UNIQUE 정책 대응: 사용자 확인과 최신 통합 대장의 A/표기 안 함 결정에 따라 `MidstreamContractInput` T-137/T-138의 `CK4/CQ4` 및 배열 결과, T-187 backup의 2셀, T-188 hidden의 1셀은 모두 PRESERVE다. 범위·근거는 [함수 reference의 보존 정책](mi-model-input-functions-2026-10-07.md)을 따른다. 함수 지원이 아니라 값·수식·cached value·관련 원본 구성의 보존을 검증한다. 현재 5셀 자체에 UNIQUE 재계산/배열 재생성을 추가하지 않는다. 연결 입력 변경 시 영향을 받는 T-139 AG/AI의 갱신 필요는 domain 규칙 59로 확정됐다. 필요한 선행 계산 범위·빈 기준값/없는 키·오류 처리·Excel 비교값과 모델 영향 검증은 §1A Q5/H1의 잔여다.

### OUTPUT 최신 정책 대응 — 2026-10-08

현재 OUTPUT 검증은 domain 규칙 50/§6.1의 승인 입력·표별 구조와 규칙 57의 A 보존을 따른다. 표시 6표/미표시 A 12표의 대응은 아래 목록에 반영했으며 입력·화면 정책 반영은 상세 계산 승인이나 실행 성공과 다르다. 과거 파랑 입력·T-082 Helper 포함 817셀 재계산은 현재 계획에서 제외하고 나머지 계산 범위/정답은 PENDING으로 분리한다.

### 대장 식별과 해석

- 수신 경로: `/Users/hoduhomil/Downloads/모델인풋_판별대장_자동채움_2026-10-06.xlsx` (실제 파일명은 Unicode NFD).
- 대장 SHA-256: `b2d9a598029a118aede599940fa59bb196002773000907b435fdd2b92a60827a`.
- 「표 대장」 A3:BV195는 193행이다. 최종 분류 BS 기준 A 32 / B 4 / C 21 / 미기재 136행이다. 정책 상태 BQ는 확정 41 / 미확정 1 / 미기재 151행으로, 최종 분류와 별개다. T-100은 C와 일부 처리 규칙이 확정됐지만 전체 정책 상태는 미확정이다.
- 「시트 대장」 A3:AR80는 78행이며 X 제거 8개를 제외하면 70개다. 대장의 개발본 시트 수와 기존 9/28 개발본 76개를 혼용하지 않는다. 실제 선택된 v8/v6 정규화 source의 hash 검증 결과는 위 고정 reference가 소유한다.
- 우선순위: 최신 사용자 결정·canonical domain 정책 → 대장 사람 확인 BA:BV 및 명시된 사용자 확인 메모 → SUMMARY/색/자동 분석. 최종 분류나 상태가 빈칸이어도 명시된 확인 범위만 반영한다. 자동 분석 0건은 의존성 부재나 계산 불필요의 증명이 아니다.
- 자동 분석 AQ:AX / 시트 AF:AM은 영향·오류 위험 탐지에 사용한다. 「서버 계산 가능」은 외부 참조·#REF!·동적 참조에 대한 정적 분류이며 함수 지원이나 Excel 계산 결과와의 일치를 증명하지 않는다. 열별 수치는 중복될 수 있다.
- 입력 색은 역할·버전별로 다르다. SUPPLY v6 노랑 FFFFDE85는 입력 후보, 파랑 FFAACED2는 고정 헤더다. OUTPUT v8은 domain 규칙 50/§6.1의 노랑 입력·파랑 고정 제목/헤더·청록 자동/조회 구분을 따른다. OUTPUT 파랑 FF39B8F2 입력 설명은 v3 관측으로 현재 허용 근거가 아니다. 명시된 사용자 수정 범위를 색보다 우선하고 조건부서식·INDIRECT/OFFSET·파일 밖 엔진 경로는 정적 분석의 한계로 남긴다.

「파일 정보」 A3:K6의 대장 작성 당시 원본 식별자는 다음과 같다. 이 표는 대장 기록을 보존한 것이며 현재 선택된 v8/v6 source hash는 위 고정 reference를 따른다.

| 역할 | 대장 원본 버전 | SHA-256 | 시트 / 수식(시트 대장 합) |
| --- | --- | --- | ---: |
| DEMAND | Demand_v5_플랫폼입력_최종_v3.xlsm | `0c381e2bffae27f53aaea3eddc787e84112dba466a7af1d08f3df74a81735aec` | 27 / 699,298 |
| OUTPUT | Output_v3_플랫폼입력_최종_v3.xlsm | `a890f4c5b25eeef5e6679a2b1b162c83f6d3c815291f13f0abf1c60aa0c46e74` | 7 / 889 |
| SUPPLY | Supply_v14_플랫폼입력_최종_v6_플랫폼전달.xlsm | `c5ab906edf807dbf2707a9e13d7146a92c7490f054fed1b7d3d42afd4312824e` | 25 / 460,803 |
| TRADE | 복호화_Trade_v11_플랫폼입력_최종_v3.xlsm | `3a7579c9d76de22d841c043a63c17a5f1f2294ea2568ccbde1e1ef5319003e44` | 19 / 702,776 |

총 수식 1,863,766개는 대장 시트 합계다. 이전 계산 후보 254 family / 1,503,061셀은 현재 allowlist 합계가 아니다. 최신 family별 계산/보존 수는 대장만으로 확정할 수 없다. OUTPUT 외부 연결 파일 1개도 「파일 정보」 G4에 있으므로 이전 v2의 외부 수식 0개를 최신의 링크 부재 근거로 사용하지 않는다.

### 검증 분류

| 분류 | 확인할 내용 |
| --- | --- |
| CALCULATE | 승인된 수식 셀만 동일 변경을 적용한 Excel golden(비교 기준 파일)과 타입·오류·값을 비교한다. |
| PRESERVE | 제외된 기존 값·수식·cached result(저장된 계산값)·서식·관계를 source snapshot과 비교한다. |
| STRUCTURE | 승인된 추가/삽입/삭제 후 좌표·수식 참조·집계 범위·신규 셀·연결 표를 확인한다. CALCULATE/PRESERVE를 함께 적용할 수 있다. |
| PENDING | 계산·구조 허용 범위 또는 정답 fixture가 미확정이다. 임의 재계산이나 전체 family GO 판정을 하지 않는다. |

A/B/C는 편집 방식이며 계산 허용 여부와 같지 않다. C라도 기존 수식은 보존할 수 있고 B라도 같은 표의 합계는 재계산할 수 있다. 구조 변경은 이번 release에 포함하며 주된 방식은 표의 마지막 행·열 추가다. 중간 삽입·삭제는 domain의 표별 예외를 따른다.

### 표별 검증 연결 목록

대장 193개 항목을 모두 아래 표에 연결했다. ID T-n의 원본 행은 n+2다. 근거 열은 BS(분류), BQ(정책 상태), BA:BU(범위·함께 수정할 곳·메모)이며, 행 묶음은 각 행을 생략 없이 포함한다. 대장의 미기재는 표 전체 미승인과 같지 않으므로 현재 설계나 명시된 사용자 메모가 있는 부분은 구분해 적용한다.

| 역할 / 시트 | 대장 ID | BS 분류 | 현재 검증 경로 |
| --- | --- | --- | --- |
| DEMAND / `Input Adjustment ->` | T-001 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| DEMAND / `GDPPopulationAdjustment` | T-002~T-003 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `ElectrificationRateAdjustment` | T-004 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `PowerMixAdjustment` | T-005~T-006 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `CoalGasAdjustment` | T-007~T-008 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `WeatherAdjustment` | T-009~T-012 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `PMIAdjustment` | T-013~T-014 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `HydrogenAdjustment` | T-015 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `LNGBunkeringAdjustment` | T-016~T-017 | C 구조 변경 | STRUCTURE + PRESERVE — 입력 행 확장, 기존 비입력 수식 보존; V-DEMAND-INPUT |
| DEMAND / `DemandOutputOverwrite` | T-018~T-019 | C 구조 변경 | STRUCTURE + PRESERVE — K/S 재계산·확장 제외; V-DEMAND-OVERWRITE |
| DEMAND / `LNG Demand` | T-020 | E 부속 control — domain 규칙 50 | C1 기존 선택값 변경·validation 보존; 연결 T-021의 합계/열 추가와 분리; V-DEMAND-LNG |
| DEMAND / `LNG Demand` | T-021 | C 구조 변경 | CALCULATE + STRUCTURE — 합계 130셀·열 추가; C1은 값 변경만; V-DEMAND-LNG |
| DEMAND / `기준정보 ->` | T-022 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| DEMAND / `BCInput` | T-023~T-030 | C 구조 변경 | STRUCTURE + PRESERVE — V-DEMAND-BCINPUT, v8 입력값 편집·표 마지막 행 추가; 수식·Helper 제외 범위는 domain 규칙 58 |
| DEMAND / `MacroParametersLogicInput` | T-031~T-032 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `PMIInput` | T-033~T-036 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `PMILogicInput` | T-037~T-039 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `WeatherLogicInput` | T-040~T-045 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `WeatherInput` | T-046~T-056 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `LNGBunkeringInput` | T-057~T-059 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `LNGBunkeringLogicInput` | T-060 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `HydrogenInput` | T-061~T-064 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| DEMAND / `General` | T-065~T-078 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| DEMAND / `Sheet2_DISCARDAFTUSE` | T-079 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| DEMAND / `CubeDump_DISCARDAFTUSE` | T-080 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| OUTPUT / `USCostofSupply` | T-081 | B — domain 규칙 50/§6.1 | 값 변경 + PRESERVE, 계산 범위 PENDING — V-OUTPUT-COST; T-082 Helper 제외 |
| OUTPUT / `USCostofSupply` | T-082 | A — domain 규칙 50·57 | PRESERVE — 미표시 Helper의 값·수식·저장 결과 유지; V-OUTPUT-PRESERVE |
| OUTPUT / `BCPrices` | T-083 | C — domain 규칙 50/§6.1 | STRUCTURE + PRESERVE — 아래 연도 행 추가·열 고정, 상세 write/파일 밖 연결 PENDING; V-OUTPUT-PRICES |
| OUTPUT / `PremiumCalc` | T-084~T-085 | B — domain 규칙 50/§6.1 | 값 변경 + PRESERVE, 비A 계산 범위 PENDING — V-OUTPUT-PREMIUM |
| OUTPUT / `PremiumCalc` | T-086~T-089 | A — domain 규칙 50·57 | PRESERVE — 미표시 4표 원본 유지; V-OUTPUT-PRESERVE |
| OUTPUT / `AnnualPriceCalc` | T-090 | B — domain 규칙 50/§6.1 | 값 변경 + PRESERVE, 비A 계산 범위 PENDING — V-OUTPUT-SPREAD |
| OUTPUT / `AnnualPriceCalc` | T-091~T-097 | A — domain 규칙 50·57 | PRESERVE — 미표시 7표 원본 유지; V-OUTPUT-PRESERVE |
| OUTPUT / `PriceSeasonality` | T-098 | B 값만 교체 | 값 변경 + PRESERVE — 수식 0, 구조 고정; V-OUTPUT-SEASON |
| SUPPLY / `Input Upstream ->` | T-099 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `ProductionAdjustment` | T-100 | C 구조 변경 | CALCULATE + STRUCTURE + PRESERVE — L/N/O/M3 구분; V-PRODUCTION |
| SUPPLY / `ProductionAdjustment` | T-101 | 미기재 | CALCULATE + STRUCTURE + PRESERVE — L/N/O/M3 구분; V-PRODUCTION |
| SUPPLY / `Input Midstream ->` | T-102 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `{USA}Hurricanes` | T-103 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| SUPPLY / `LiqUtilAdjustment` | T-104 | 미기재 | STRUCTURE 부분 확정, 계산 범위·삭제 세부 PENDING; V-LIQUTIL |
| SUPPLY / `LiqProdAdjustment` | T-105 | 미기재 | PRESERVE 기존 backup 오류 경로, 나머지 CALCULATE 범위 PENDING; V-BACKUP |
| SUPPLY / `RegasAdjustment` | T-106 | B 값만 교체 | CALCULATE + STRUCTURE — T-106 직접 값과 T-107/108 파생 합계; V-REGAS |
| SUPPLY / `RegasAdjustment` | T-107~T-108 | 미기재 | CALCULATE + STRUCTURE — T-106 직접 값과 T-107/108 파생 합계; V-REGAS |
| SUPPLY / `PipelineAdjustment` | T-109 | F-S-06 H67 CLEAR 및 I67/I3 계산·나머지 보존 확정(규칙 62); 일반 정책 미확정 | 입력/POI 2셀 계산·Excel 비교/보존 Pass. native 별도 상태. 표 전체 허용으로 확대하지 않음 |
| SUPPLY / `BCProduction` | T-110 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| SUPPLY / `BCProduction` | T-111 | A 원본 복제 | PRESERVE — N7:T24 미표시·원본 복제. T-110 행 추가에도 재집계·재계산·피벗 새로고침 없음; V-PRODUCTION-YEAR-REFERENCE |
| SUPPLY / `BCHurricanes` | T-112~T-113 | B 값만 교체 | CALCULATE — 행 추가 불가, G8:G11·G16:G19의 8개 수식은 외부 LUT 내부 캐시 사용; V-BCHURRICANES |
| SUPPLY / `BCHurricanes` | T-114 | B 값만 교체 | 값 변경 + PRESERVE — 기존 행·열 고정, 표 수식 0개; V-BCHURRICANES-ASSETS |
| SUPPLY / `BCHurricanes` | T-115 | C 구조 변경 | STRUCTURE — 마지막 데이터 행 아래 추가, 표 수식 0개. 열 추가·삭제·신규 행 상세 write 정책은 PENDING; V-BCHURRICANES-ASSETS |
| SUPPLY / `BCLiquefaction` | T-116 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| SUPPLY / `BCRegasification` | T-117 | 미기재 | STRUCTURE/CALCULATE 연결 요구 확인, 신규 셀 상세 PENDING; V-REGAS |
| SUPPLY / `BCPipeline` | T-118 | C 구조 변경 | STRUCTURE — 마지막 행 아래 추가 허용, 수식 0개·내부/외부 셀 수식 참조 없음. 세부 write 정책·구조 검증 fixture는 PENDING; V-BCPIPELINE |
| SUPPLY / `LNG Summary` | T-119 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `LiqUtilAdjustment_backup` | T-120 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `LiqProdAdjustment_backup` | T-121 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `BCLiquefaction_backup` | T-122 | A 원본 복제 | PRESERVE — V-COPY / V-BACKUP / V-HIDDEN, 표별 원본 유지 |
| SUPPLY / `{USA}RigsLogic` | T-123 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `{China}PolicyLogic` | T-124 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `{USA}HurricanesLogic` | T-125 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `RegasLogic` | T-126 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `PipelineLogic` | T-127 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `MonthlyAdjProduction` | T-128 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| SUPPLY / `SeasonalityLogic (서버 LUT)` | T-129 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `LNG_TradeRestrictionInput` | T-130 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `PNG_TradeRestrictionInput` | T-131 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `Sheet1` | T-132 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| TRADE / `MidstreamContractInput` | T-133~T-139 | T-133/T-139 C 입력·아래 행 추가, T-134~T-138 A — domain §1A·규칙 57·59 | 승인 입력 + PRESERVE — V-MIDSTREAM-VALUE / V-COPY / V-OPTIONAL-FUNCTION. 연결 입력에 따른 AG/AI 갱신 필요 확정, 계산 세부·신규 행·표 밖 CN/CT는 PENDING |
| TRADE / `ShippingCostInput` | T-140~T-152 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `MidstreamCapacityInput` | T-153~T-164 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `UpstreamAdjLogicInput` | T-165~T-167 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `DemandAdjLogicInput` | T-168~T-170 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `CostInput` | T-171~T-176 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `4.5 PriceModelInput` | T-177~T-182 | A — domain 규칙 57의 현재 보존 결정 | PRESERVE — 미표시 계산표 원본 보존; V-PRICE-PRESERVE |
| TRADE / `4.5 PriceModelInput` | T-183/T-184 | C — domain 규칙 44의 현재 편집 결정 | STRUCTURE + PRESERVE — 입력 편집·아래 행 추가, 열/기간 고정. 재계산 자동 승인 아님; V-PRICE-INPUT |
| TRADE / `LNGExportRestictionInput` | T-185 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `backupLNG_TradeRestrictionInput` | T-186 | 미기재 | PRESERVE — 미표시·A 원본 복제(domain 규칙 57); V-COPY |
| TRADE / `backup_MidstreamContractInput` | T-187 | 시트 전체 미표시·A 원본 복제(2026-10-08 사용자 재확인) | PRESERVE — domain 규칙 57 보완에 따라 시트 전체 보존; UNIQUE 2셀·배열 결과를 포함해 편집·구조 변경·재계산/새로고침 제외; V-COPY |
| TRADE / `Industry research` | T-188 | 시트 전체 미표시·A 원본 복제(2026-10-08 사용자 재확인) | PRESERVE — domain 규칙 57 보완에 따라 숨김 상태와 시트 전체 보존; A3 UNIQUE를 포함해 편집·구조 변경·재계산/새로고침 제외; V-COPY·V-HIDDEN |
| TRADE / `net exporter` | T-189 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| TRADE / `pipeline cap` | T-190 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| TRADE / `pipeline contract` | T-191 | 미기재 | PRESERVE — hidden 정책 우선; V-HIDDEN |
| TRADE / `Demand/Supply 결과 (외부 연결)` | T-192 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |
| TRADE / `국가·Terminal 공통 매핑 (서버 LUT)` | T-193 | 미기재 | PENDING — 표별 편집/계산 범위 확정 후 fixture 선정 |

### 최신 정책 검증 시나리오

아래는 제품·후보 엔진의 검증 계획이다. F-D-02·F-S-01·02·03은 뒤의 「4사례 POI 선택 계산 실행」에 실제 부분 실행 증거를 연결했다. 다른 경로는 표본 준비 또는 미검증 상태를 유지한다. CALCULATE는 승인 source의 기존 값 보존 검사와 다른 기대값을 갖는다. 동일 입력·구조 변경을 적용한 Excel 비교 파일이 없으면 정답 검증은 PENDING이다.

| ID | 변경·검증 대상 | 기대값 / 제한 |
| --- | --- | --- |
| V-DEMAND-INPUT | T-002~017 승인 입력 SET/CLEAR 및 마지막 행 추가 | 입력/신규 행 값 반영, 열 추가 거부. 비입력 기존 수식·저장값 보존, 신규 비입력 수식 확장 없음. 대장 BA의 PMI 13행 예외 등 표별 실제 범위 적용. |
| V-DEMAND-OVERWRITE | T-018/019 마지막 행 추가·행 삭제 및 값 편집(domain 규칙13/45) | D:I / M:Q 입력 적용, K/S 기존 수식·저장값 원본 보존·신규 수식 확장 없음(규칙45 지정 DELETE의 삭제/참조·행 동반 이동 예외 포함). J/K/R/S 화면 미노출. 중간 삽입은 현재 범위 제외. 선택 표/정확한 행·값·이동/서식/metadata 기대값을 확정한 뒤 독립 실행. |
| V-DEMAND-LNG | T-020 C1과 T-021 연간 값 편집·마지막 열 추가 | 기존 E:N의 13개 합계 행만 재계산(130셀); E4:N109 전체를 계산 범위로 쓰지 않음. 새 열 합계 참조 이동·연도 헤더·24행 값 확인. C1을 바꿔 수식 0개인 control의 재계산을 요구하지 않음. |
| V-OUTPUT-COST | T-081 최신 노랑 입력 선언과 고정 헤더 | domain 규칙 50/§6.1의 입력 범위·행/열 고정 및 수식/헤더 직접 쓰기 제외를 대조. T-082는 V-OUTPUT-PRESERVE. 과거 817셀 전체 재계산 금지; D7:K7·L열 대응과 비A 계산 범위/정답은 PENDING이며 임의 8셀 allowlist도 확정하지 않음. |
| V-OUTPUT-PRICES | T-083 입력 편집·마지막 데이터 행 아래 연도 행 추가 | domain 규칙 50/§6.1의 고정 헤더·열과 신규 행 대응, 승인 변경 밖 원본 보존. 필드/서식·validation·범위/참조 처리 기대값은 write 계약 후 검증. 수식/내부 참조 0은 과거 대장 관측이며 최신 source 검증·파일 밖 모델 연결 확인을 대신하지 않음. |
| V-OUTPUT-PREMIUM | T-084/T-085 입력·조회·미표시 분리 | domain 규칙 50/§6.1의 승인 C열만 값 변경, B열 연도 조회·D:H 미표시, 행/기간 고정 및 East Asia Spot 62~65행 원본 유지. 일부 미표시 D:H의 비A 계산/보존 경계는 PENDING. |
| V-OUTPUT-SPREAD | T-090 하한/상한 값 변경 | domain 규칙 50/§6.1의 승인 두 입력과 설명 영역 조회를 분리하고 구조 고정 확인. 미표시 A 계산표 갱신 금지; 그 밖 영향·정답 fixture는 PENDING. |
| V-OUTPUT-PRESERVE | T-082·T-086~T-089·T-091~T-097의 미표시 A 12표 | 표시 입력 편집·T-083 행 추가 후에도 Helper P8:P816 포함 기존 값·수식·배열 결과/저장값·관련 구성 보존. 같은 시트 전체 bytes 동일이나 일부 미표시 열 전체 A를 요구하지 않음. |
| V-OUTPUT-SEASON | T-098 C2:O4 값 편집 및 C2 CLEAR 거부 | domain §6.1의 C2 사용자 확인 적용: C2 SET은 숫자 0을 포함하고 CLEAR는 거부 기대값이다. 나머지 셀의 빈 값 허용은 미확정. 허용된 입력 변경 밖 셀·수식 0·행·열 유지. 거부 동작의 제품 검증은 PENDING. |
| V-PIPELINE-CLEAR | T-109 PipelineAdjustment H67 CLEAR / F-S-06 | domain 규칙 62의 허용된 비우기 사례. 실제 blank·기존 style/셀/행 유지. I67 원본 수식 유지 및 계산 결과 `Error - enter value` 문자열이 정상 기대값. 영향 계산 범위/동일 변경 Excel 정답·실제 평가 전에는 입력-only 파일의 다른 수식/저장값/metadata 보존을 계산 결과로 사용하지 않음. |
| V-PRODUCTION | T-100 기존 값 편집 / 마지막 행 추가 | 기존 L8:L200 수식·저장값 보존, 신규 L 빈칸. L7 헤더 포함 화면 제외. 신규 N/O 상대 행 참조 이동 복사 및 결과 검증. M3의 G/O 끝행 확장. 기준 상수 386 처리·서식/validation·행 식별이 미확정인 상태로 전체 구조 GO 판정 금지. |
| V-REGAS-VALUE | T-106 H11:AE382 편집 | AF:DK 입력 거부, B:G·AF:DK·DM:DV 사용자 쓰기 금지. 연간 수치 DM:DV 및 국가 월 합의 영향 수식을 golden 비교. 연간 블록 3,720셀 안에는 국가 표기 372셀과 연간 수치 3,348셀이 있어 합계 전체를 수치 수식으로 간주하지 않음. T-108 6,264셀의 계산을 과거처럼 PRESERVATION_ONLY로 제외하지 않음. |
| V-REGAS-ROW | BCRegasification / RegasAdjustment 연결 끝행 추가 | 연결 수식 생성/참조 이동·SUMIFS 고정 끝행 확장, 국가 합계 블록을 추가 행 수만큼 아래로 이동. 국가 목록은 늘리지 않음. 새 설비가 기존 국가 합계에 포함되는지 확인. |
| V-REGAS-COL | 기준표 연도 추가·Regas 원본 보존 | domain 규칙51 최종 사용자 결정: BCRegasification의 연도 입력과 최대 용량 이동/계산만 대상, Regas는 전체 원본 보존. 과거 Regas 연간 블록/월 집계 연결 확장 후보는 연도 추가에 적용하지 않음. 행 추가/삭제 연결은 독립 사례로 유지. |
| V-REGAS-LOOKUP | 같은 설비명 두 행 / 목록 밖 국가 | 원본 첫 일치 lookup 의미 유지. 현재 두 행의 값 동일 여부 확인. 값이 달라지거나 국가 목록 밖인 fixture는 기존 가정 밖의 미결정 사례로 표시; 새 lookup 보정 로직을 임의 도입하지 않음. |
| V-LIQUTIL | 마지막 월 열 추가·행/열 삭제 | 새 Period = 직전 Period + 1, FE 전달 규칙과 BE 저장·파일 결과 일치. G7:CL7 내부 참조 정책을 대장 초기 '확정 아님' 메모보다 우선. 삭제 범위·재번호화·수식/참조 정책 미확정 fixture는 PENDING. |
| V-BACKUP | SUPPLY backup 3개 및 활성 CM11:CM407 기존 오류 | backup 수식·저장값 보존. 기존 오류는 보존 expected error로 비교하고 0/빈칸으로 고치지 않음. CM 경로를 이유로 LiqProd 전체 golden을 요구하지 않음; 나머지 승인 계산은 별도 검증. |
| V-COPY | General / 구분 탭 / read-only `LNG Summary` 1개 / 미표시 backup | 원본 수식·값·서식 보존. SUPPLY `LNG Summary`만 source 값 조회·쓰기/Draft/재계산 제외, TRADE `backupLNG_TradeRestrictionInput`과 구분 탭 4개는 화면 미표시·원본 보존(domain 규칙 43·57). General outbound나 원본의 기존 오류 존재만으로 계산 대상으로 승격하지 않음. |
| V-DEMAND-BCINPUT | T-023~T-030의 v8 입력값 편집·표 마지막 행 추가 | domain 규칙 58의 신규 수식 생성·기존 AE:AF 재계산·저장값 Helper 생성/갱신 제외 및 원본 보존을 검증한다. 미확정인 기존 AG·X·AO4 재계산을 자동 허용하지 않는다. 실제 행 추가·보존 동작은 아직 미검증. |
| V-PRICE-PRESERVE | TRADE 4.5 PriceModelInput T-177~T-182 및 승인 변경 밖 원본 영역 | domain 규칙 44·57 적용. 미표시 계산표 값·수식·저장값·외부 link 관계 보존, U열 화면 제외. T-183/T-184 승인 구조 변경이 있는 worksheet 전체 bytes 동일을 요구하지 않고 보존 범위와 변경 metadata를 구분. |
| V-PRICE-INPUT | TRADE T-183/T-184 승인 입력 SET/CLEAR 및 마지막 데이터 행 아래 추가 | domain 규칙 44 적용. 기존 입력·신규 행과 고정 헤더/열·기간을 대조하고 다른 표 범위/값 불변 확인. 기존 외부 수식·저장값·externalLink 보존. 서식/validation·구조 metadata·참조 갱신 기대값은 상세 write 계약 확정 후 검증. 실제 생성/계산 성공 증거는 아직 없음. |
| V-HIDDEN | 최신 정규화 source hidden/veryHidden sheet | engine input/output 제외, 집합·순서·상태·worksheet bytes·관련 package 동일. 이전 hidden 14개 수치는 source 비교 증거 없이 현재 개수로 고정하지 않음. |
| V-DEV-SHEETS | 최신 대장의 SUMMARY / 플랫폼 입력 안내 8개 | 정규화 source·runtime catalog·최종 XLSM·Case·Job에는 없음. 제거 전 개발본과 최종파일을 package 동일 비교하지 않고 정규화 source를 보존 기준으로 사용. |
| V-EXTERNAL | 역할 4개 전체의 외부 수식·링크 | 외부 파일 요청·refresh·자동 내부 시트 연결 금지. domain 규칙 54의 계산 예외는 V-BCHURRICANES로 검증하고 나머지 외부 수식·저장값·externalLink는 승인 source대로 보존; 대장 자동 0건을 링크 부재로 간주하지 않음. |
| V-BCHURRICANES | SUPPLY T-112·T-113 값 변경 | domain 규칙 54 적용. 행 추가 거부, G8:G11·G16:G19의 8개 계산 결과를 변경 입력 및 고정 LUT 캐시에 따른 기대값과 비교. 원본 수식·LUT 캐시·externalLink 관계 불변. 기존 G열 결과를 읽는 것만으로 재계산 성공 판정 금지. 필요한 캐시 누락 시 성공 차단. 2026-10-12 F-S-02/03 독립 선택 계산·golden 비교·출력 보존 Pass; 2026-10-13 원본 산출물 native 재개방 복구 경고 미관측·동일 hash 읽기 전용 복사본 각 8셀 값 유지 Pass. 후속 읽기를 새 재계산 성공으로 사용하지 않음. |
| V-BCHURRICANES-ASSETS | SUPPLY T-114·T-115 값 변경 및 T-115 행 추가 | domain 규칙 55 적용. T-114는 기존 행·열 유지, T-115는 논리 표 마지막 데이터 행 아래 추가. 두 표의 수식 0개를 확인하고 수식 재계산·복제 검사 없음. 다른 표 및 규칙 54의 계산 범위 보존. 신규 행 필드·서식/validation 및 구조변경 상세 기대값은 write 계약 확정 후 검증. |
| V-PACKAGE | 독립 입력 편집 결과 XLSM 4개 | 재개방·형식/관계 무결성·VBA/connection 보존. 구조 변경 승인 영역의 범위·names/merge/validation은 계약대로 갱신; 그 밖 영역만 원본 동일 비교. 전 파일의 수식/표/관계 개수가 무조건 같다는 이전 검사를 사용하지 않음. |
| V-BCPIPELINE | SUPPLY BCPipeline T-118의 마지막 행 추가 | C 분류·행 추가 허용을 적용한다. 기존 셀 수식이 없어 수식 재계산·복제 검증 대상은 없다. 신규 행 값·서식·필터/이름 범위 등의 세부 기대값은 write 계약 확정 후 검증한다. |
| V-PRODUCTION-YEAR-REFERENCE | SUPPLY BCProduction T-111 Production Year Count Reference | N7:T24의 화면 미표시 및 원본 값·수식·cached result·관련 피벗 설정/캐시·외부 원본 관계 보존. T-110의 승인된 변경/행 추가에도 참조표 갱신·피벗 새로고침 없음. 같은 시트의 다른 표까지 보존 정책을 확대하지 않는다. |
| V-SCENARIO-OUTPUT | 두 Scenario mode의 OUTPUT 준비·Case·Job 전달 | OUTPUT 제안·병합·재계산·재직렬화 없음, 기준 원본 bytes hash 동일. 독립 편집 OUTPUT의 값/구조 변경 및 미확정 계산 범위와 혼동 금지. |
| V-FAIL | 승인 계산의 새 오류·중단 / 보존하기로 한 기존 오류 | 승인 계산이 실패한 부분 결과로 최종 Case/실행 준비 성공 처리 금지. 보존 영역의 expected error는 실패로 오인하지 않음. 임시저장 실패 시 변경 저장/rollback 계약은 아직 미확정. |
| V-FAMILY | 승인 정규화 source + 표별 정책 확정 후 목록 생성 | 역할/시트/table/cell·source hash별 계산/보존/구조 정책을 먼저 적용한 뒤 family별 셀·대표 fixture 집계. 같은 family 내 서로 다른 정책 영역을 합쳐 재계산하지 않음. |
| V-OPTIONAL-FUNCTION | UNIQUE 5곳의 현재 PRESERVE 경로와 향후 계산 정책 변경 | 현재는 수식·배열 결과·cached value·관련 원본 구성 보존을 검증하고 UNIQUE 계산을 요구하지 않는다. 계산 정책 변경 시에만 함수 지원·shape·값을 검증한다. GDP/PIPELINE 오탐은 제외하며 조건부서식 `_XLUDF.ISFORMULA`는 규칙 보존/평가 요구를 별도 판정. |
| V-MIDSTREAM-VALUE | T-139 기존 승인 입력 변경과 AG/AI 영향 | domain 규칙 59의 갱신 필요와 규칙 57의 A 보존을 함께 검증한다. Q5/H1의 선행 범위·기준값/오류 처리가 확정되기 전에는 입력/보존 표본만 준비한다. 같은 행 AG/AI만으로 다른 행 합계 영향 검증을 대체하지 않는다. |

### INPUT-02A 선정 표본 — 2026-10-08

**현재 부분 검증 — 2026-10-15 F-D-05-DELETE 완료 반영:** F-S-06 제외 생성된 허용17사례의 입력·source 보존·선정 native는 Pass다. F-D-02·F-S-01/02/03·F-S-04-COLUMN·F-D-03·F-S-05-COLUMN은 승인 범위의 POI 계산·Excel 정답 비교 Pass, F-T-01/02·F-D-01은 이번 사용자 결정으로 지정 입력 외 전체 보존이며 계산/Excel 계산 정답 해당 없음이다. F-O-01/02/04·F-T-04·F-S-05-ROW·F-D-05-APPEND/DELETE도 계산 해당 없음이다. F-O-03은 C2 빈 값 금지에 따른 업무 입력 Fail/기술적 보존 Pass 이력으로 성공 입력에서 제외하고 제품 거부 검증은 별도 PENDING이다. 기존3건 추가 계산 정책 대기는 해소했으며 현재 미실행 허용 후보는2구조/CLEAR0다. 아래 이전 실행의 계산/native 대기는 날짜별 이력으로 구분한다. 이 부분 검증을 INPUT-02A 완료/GO나 전체 조사 완료로 처리하지 않는다. 업무 정책은 [domain §6·§6.1 및 §1A](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md)를 참조한다.

**후속 CLEAR 표본:** F-O-03을 허용 입력 사례에서 제외하고 사용자 지정 SUPPLY `PipelineAdjustment!H67`을 **F-S-06**으로 새 실행했다. 규칙 62의 I67/I3 계산·나머지 보존 사용자 확인 및 수정된 Excel 정답 확보 후 POI 실제 평가/저장·2셀 값/타입 비교/원본 보존 Pass다. native는 별도 상태이며 아래 POI 실행 후속을 따른다. 기존 사례 ID·파일은 재사용하거나 덮어쓰지 않는다.

**후속 F-T-04 현재 상태:** 지정 Q704:U704 슬롯 입력/보존 및 같은 hash native 재개방 Pass. 사용자 전체 계산 제외 결정(domain 규칙 59)에 따라 계산/Excel 계산 정답 해당 없음(PRESERVE)으로 대기를 해소했다. 아래 「F-T-04 전체 계산 제외·원본 보존 후속」의 재검증 결과를 우선하며 초기 표본/실행 기록의 계산 PENDING과 구분한다.

#### 잔여 6사례 확정 입력·보존 실제 실행 — 2026-10-13

각 사례를 같은 변경 전 정규화 source에서 독립 생성했다. 기존 복사본 존재나 hash 일치만으로 실행 판정하지 않고, 실제 SET/CLEAR로 새 XLSM을 저장하고 ZIP/XML 및 셀별 값을 재읽었다. 원본 비교 기준은 source이며 계산 정답을 입력/보존 검사에 사용하지 않았다. 당시 F-O-03 CLEAR 입력 Pass는 빈 셀 생성 성공이라는 기술 판정이다. 아래 표는 후속 사용자 결정에 맞춰 해당 산출물의 업무 입력 판정을 정정했다.

| 사례 / source 역할 | 독립 변경 조건 | 입력 / 보존 | 계산 / Excel 계산 정답 | native 재개방 |
| --- | --- | --- | --- | --- |
| F-O-01 / OUTPUT | `USCostofSupply!B8` SET, number → number, source + 0.01 | Pass / Pass. P8:P816 809수식·저장값 불변 | 해당 없음 / 해당 없음 — 2026-10-13 §6.1 사용자 확인: B8 외 추가 갱신 없음 | Pass — 읽기 전용 복사본의 입력/Helper·헤더 선정11셀 유지(아래 후속) |
| F-O-02 / OUTPUT | `PriceSeasonality!C2` SET, number → number, source + 0.125 | Pass / Pass. C2 밖 입력·헤더·행/열/서식 유지 | 해당 없음 / 해당 없음 — 표 수식 0 | Pass — 읽기 전용 복사본의 C2·선정4셀 유지(아래 후속) |
| F-O-03 / OUTPUT | 기존 C2 CLEAR 산출물, number → 실제 blank. 현재는 거부 기대값 | Fail(업무 입력) / Pass(기술 보존). 빈 셀 생성은 확인했으나 C2 빈 값 금지에 위배. 제품의 CLEAR 거부 검증 PENDING | 해당 없음 / 해당 없음 | 기술 관측 Pass — 읽기 전용 UI 개방·복구 경고 미관측·빈 입력줄. 업무 적합성/제품 거부 검증 아님 |
| F-T-01 / TRADE | `MidstreamContractInput!Q5` Year SET, number → number, source +1 | Pass / Pass. Q5 외 전체 원본 보존 | 해당 없음 / 해당 없음 — 2026-10-14 규칙59 최종 예외로 AF/AH·AG/AI·CN/CT·다른 행까지 PRESERVE. 지정 검증 Complete | Pass — 기존 읽기 전용 복사본의 선정9셀 값/수식·기존 오류 표시 유지 |
| F-T-02 / TRADE | 같은 source의 U5 계약량 SET, number → number, source +0.125 | Pass / Pass. U5 외 정상/오류 수식·저장값 전체 보존 | 해당 없음 / 해당 없음 — 규칙59 최종 예외로 비오류·다른 행·표 밖 추가 계산 대기 해소. 지정 검증 Complete | Pass — 기존 읽기 전용 복사본의 선정9셀 값/수식·기존 오류 표시 유지 |
| F-D-01 / DEMAND | `BCInput!N4` SET, number → number, source +0.125 | Pass / Pass. N4 외 Helper·AE:AF·X/AG/AO4 등 원본 보존 | 해당 없음 / 해당 없음 — 2026-10-14 규칙58 지정 입력 외 전체 계산 제외. 지정 검증 Complete | Pass — 기존 읽기 전용 복사본의 N4/O4·AE4/AF4 선정4셀 유지 |

- 실행/비교 범위: 선택 시트의 모든 기존 셀을 좌표별로 대조했다(USCostofSupply 12,221셀, PriceSeasonality 각 89셀, Midstream 각 222,163셀, BCInput 1,429,777셀). 사례별 대상 입력 한 셀만 값이 달라졌다. F-O-03의 대상 변경은 현재 업무 정책상 허용되지 않는다. 원본 수식/shared·array 속성·저장값·타입·style, 행/열·validation·merge·조건부서식·표 범위를 구분해 검사했고 모든 다른 part의 개별 hash가 일치한다. 기존 저장 오류는 BCInput 252셀, Midstream 각 23,065셀 그대로이며 새 저장 오류 0이다. **미계산 수식의 정확성·#N/A 업무 허용을 뜻하지 않는다.**
- 실제 package 보존: TRADE VBA·externalLink/캐시·connection/queryTable·native Table·dynamic metadata, OUTPUT externalLink/캐시, 역할별 names·styles·calcChain·hidden 상태/worksheet를 확인했다. 대상 source에 없는 pivot/cache 등은 해당 없음으로 기록했다. 승인 입력과 같은 시트의 보존 영역을 분리했으며 worksheet bytes/전체 개수 하나로 판정하지 않았다.
- 계산 진입 조건: 이번 새 계산 가능 사례/구조 실행 가능 사례는 0건이다. TRADE의 BA5:BA2000 1,996셀은 저장값이 모두 비었고, 비어 있지 않은 BI 저장 키 6개와 현재 입력 699키의 일치는 0건이다. Q5 변경 키도 불일치다. AG는 Q/R/T 키로 BA를 조회하고 AI는 AF + AG × AH이며, AH의 AF열 전체 SUMIFS와 Y/AB/AE 등 다른 행 영향을 기존 인계 및 현재 표본 수식으로 연결했다. AG5/AI5 두 셀만으로 전체 영향 검증을 선언하지 않았으며 AF/AH 선행 범위·누락/기존/새 오류 처리·H4 표 밖 2,961수식 경계 및 동일 변경 Excel 정답은 대기다.
- OUTPUT 관측: D7:K7은 앞 연도 + 1인 연도 헤더 수식이고 L7은 literal 연도, 검사한 L8/L816은 빈 셀이다. B8을 직접 읽는 P8은 보존된 수식/저장값을 유지했다. 관측을 새 비A allowlist로 승격하거나 과거 817셀을 계산하지 않았다. F-D-01의 계산 세부도 정책·정답 대기를 유지한다.
- native 범위/한계: F-O-03만 동일 hash·0444 복사본을 Excel UI에서 수동 계산·기존 매크로 비활성 상태로 열고 무저장 닫았다. 빈 C2 표시와 복구 경고 미관측은 확인했으나 전체 셀의 native 값/타입·재계산/저장 cycle은 검증하지 않았다. 다음 개방 전에 도구가 사용자 Excel 조작을 감지해 나머지는 PENDING으로 남겼다. 검사에서 바꾼 계산 설정은 자동으로 복원하고 검사 전용 빈 helper는 무저장으로 닫았다. 외부 workbook 개방·링크/connection/pivot 새로고침·매크로 실행은 하지 않았다.
- 도구 이력: 초기 PriceSeasonality 범위 요약에 승인 C2를 포함해 부분 범위가 Fail로 표시됐다. 셀별 보존/입력 검사는 Pass였고, 두 범위 보고서만 C2를 제외한 보존 55셀로 재검사하여 Pass를 확인했다. 최초 보고서와 로그는 보존했고 XLSM은 바꾸지 않았다. 대기 관측 스크립트의 반복 검색 중단 및 exclusive 파일 생성 거부 이력도 보존했으며 한 번의 순회로 바꾼 최종 검사는 Pass다. 제품/계산 결과 실패로 섞지 않는다.
- 자료: Git 제외 새 [실행 안내·재현](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/README.md), [최종 상태·각 새 XLSM/source hash·검사 경로](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/final-summary.json), [독립 대기 명세/질문·정본 반영 위치](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/pending-cases-latest.json), [사례별 Excel 제출 양식](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/excel-submission-remaining.json)을 연결한다. 실제 전후 값/타입·수식·보존 part hash·범위는 각 `result.json`/`preservation.json`(F-O-02/03 최종은 `seasonality-audit/`)에 있다. 원본/대장/기존 자료 242경로와 새 파일·native 복사본 hash 불변, 생성 6파일 ZIP CRC·모든 XML/rels parse 및 저장 후 재읽기 Pass. Java/JUnit 테스트·새 POI 계산은 사용하지 않았다.
- 사용자 확인: 사례가 실제로 어느 시트/셀을 바꾸는지, CLEAR가 왜 필요한지 설명을 요청받아 Q5/U5 필드·AF/AG/AH/AI 수식 연결, B8/P8 보존, T-139 생성 위치 및 기존 CLEAR 명세의 의미를 설명했다. 이는 새 정책 답변이 아니며 domain 정본을 변경하지 않았다. 아래 대기 표와 제출 절차를 사용해 사례별 결정을 받은 후 승인 계산/구조만 재개한다.

#### F-T-01/02·F-D-01 지정 입력 외 전체 보존 확정 — 2026-10-14

- 사용자 최종 결정은 domain 규칙58/59·Q5/H1/H4에 반영했다. F-T-01은 Q5 Year2026→2027, F-T-02는 U5 계약량 source +0.125, F-D-01은 N4 source +0.125의 지정 입력만 반영한다. 나머지 수식·저장값 전체 보존이며 기존 빈 cache/오류를 새 계산값/정답으로 사용하거나0/공백으로 바꾸지 않는다. 다른 입력 변경·일반 업무 정책에는 확대하지 않는다.
- [정책 결과/기존 실행 근거](../../temp/input-02a-engine/2026-10-14-three-input-only-policy-9pl1af3y/policy-result.json)·[현재3사례 명세](../../temp/input-02a-engine/2026-10-14-three-input-only-policy-9pl1af3y/cases-current.json): 세 파일의 입력/전체 source 보존·저장 후 재읽기 Pass와 동일 hash의 기존 native Pass(각9/9/4선정셀)를 연결했다. **계산·Excel 계산 정답은 해당 없음(PRESERVE)**, 지정3사례의 남은 확인 없음·Complete다. 앞선 전체 셀/part 보존 검증을 같은 artifact hash로 재사용하며 이번 XLSM 생성·POI 계산·Excel/native 재실행은0이다. native 선정 읽기를 전체 workbook 값/서식 인증으로 확대하지 않는다.
- [최신 사례 상태](../../temp/input-02a-engine/2026-10-14-three-input-only-policy-9pl1af3y/status-audit-current.json)·[검증/재현](../../temp/input-02a-engine/2026-10-14-three-input-only-policy-9pl1af3y/verification.json): F-S-06 제외 생성된 허용12사례 모두 지정 검증 Complete·입력/보존/native Pass, 추가 계산 정책3→0·승인 계산 정답 대기0·미실행7구조/CLEAR0다. 기존428자료/source/산출물·기존 보존/native 기록 hash 불변·변경3문서 `git diff --check` Pass. 전체 INPUT-02A는 In Progress며 앞선3건 계산 대기는 이번 결정 전 이력이다.

#### 잔여 6파일 native 재개방 후속 — 2026-10-13

- 대상/결과: F-D-01·F-T-01/02/04·F-O-01/02 모두 **native 재개방 Pass**. 원산출물과 같은 bytes/hash의 새 0444 복사본을 Excel 16.113.4에서 실제 readOnly=true/manual/forceDisable/alerts=true로 열고 저장 없이 닫았다. 선정57셀의 값/표시·35수식은 저장본과 일치하며 기존 오류4셀은 native 표시로 비교했다. 전체 workbook의 native 값/서식·오류 타입 API 인증이나 재계산/저장 cycle을 수행한 것은 아니다.
- 제어/보존: calculate/evaluate·링크/connection/pivot refresh·macro·save/save-as 명령을 사용하지 않았다. 원본/기존 자료446경로 및 새6복사본 hash 불변·ZIP Pass, 기존 통합 문서8/saved 상태 유지, 검사 후 원래 automatic/low security와 alerts=true를 확인했다. 복구/갱신/enable-content를 수락한 동작은 없으며 alerts가 켜진 native 개방이 정상 반환됐다.
- 근거/재현: [최종 native 결과·사례별 제어/읽기 경로](../../temp/input-02a-engine/2026-10-13-native-six-g2wtwxb5/native-summary.json), [실행 안내](../../temp/input-02a-engine/2026-10-13-native-six-g2wtwxb5/README.md), 같은 경로의 `read-config.json`·각 `attempt-N-command/stdout/result.json`·도구/실패 이력·전후 hash·`restoration.json`을 따른다. 최초 F-O-02의 검증 도구 NSNull 오류는 수정 후 해소됐고 첫 기록은 보존했다.
- 상태 경계: 아래 초기 UI 관측/정책 후속의 native PENDING은 이번 검사 전 이력이다. 이번 결과는 6파일 native 대기만 해소하며 F-D-01·F-T-02 비오류·F-T-04 신규 행 및 F-T-01 AG/AI 밖 범위의 계산/정답·미실행9명세·전체 INPUT-02A는 기존 상태를 유지한다. F-S-06은 이번 검사에서 제외했다. 다른 에이전트의 일반 정합성/업무 정책 문구는 변경하지 않는다.

#### F-T-01 AG/AI 계산 제외 후속 — 2026-10-13

- 사용자 확인: **F-T-01은 AG·AI를 계산하지 않는다.** 업무 정본은 domain §6 규칙 59의 사례 한정 예외 및 §1A.4 Q5/H1에 반영했다. Q5의 Year SET 2026→2027 입력은 유지하고 AG/AI의 원본 수식·저장값·타입·서식을 보존한다. F-T-02나 다른 Year 변경·AF/AH·CN/CT·신규 행까지 결정한 것으로 확대하지 않는다.
- 상태: F-T-01 입력/보존 Pass 유지, **AG/AI 계산·Excel 계산 정답은 해당 없음(PRESERVE)**. 다른 계산 범위·native 재개방의 미확인은 별도다. 이전 실행 요약/제출 양식의 PENDING은 이 결정 전 기록으로 보존하고, 현재 AG/AI 상태는 이 후속을 따른다. 기존 `#N/A`는 원본 보존값이며 계산 정답으로 승인한 것이 아니다.
- 재확인 근거: [새 정책 후속 결과·보호 hash](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/2026-10-13-f-t-01-policy-hcxa32ng/policy-result.json). 기존 F-T-01 산출물을 읽기 전용으로 검사하여 AG5:AG4707·AI5:AI4707 9,406셀의 수식·저장값·타입·서식 XML이 변경 전 source와 같음을 확인했다. Q5 typed SET, 산출물/source hash·ZIP 및 기존 자료 315경로 불변 Pass. 파일 재생성·POI 계산·native 재개방은 수행하지 않았다.

#### F-T-02 원본 #N/A 보존 후속 — 2026-10-13

- 사용자 확인·정본: **F-T-02의 원본에서 이미 #N/A인 셀은 변경하지 않고 그대로 둔다.** domain §6 규칙 59의 사례 한정 보존 예외를 적용한다. 오류 셀의 수식·저장값·타입·서식을 보존하며 0/빈 값/새 계산 결과로 치환하지 않는다.
- 상태/범위: U5 SET 입력·보존 Pass 유지. 원본 #N/A 셀의 계산/Excel 계산 정답은 해당 없음(PRESERVE), 비오류 셀의 계산 범위·정답과 native는 PENDING이다. 새 #N/A나 다른 오류·다른 사례의 허용을 확정한 것은 아니다. 이전 기록은 결정 전 상태로 유지한다.
- 근거: [새 정책 후속 검증](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/2026-10-13-f-t-02-policy-x0nkv7sj/policy-result.json)에 선택 시트의 기존 #N/A 셀별 원본/산출물 XML·타입/수식/저장값·서식 일치, 다른 worksheet/part 보존, U5 typed SET·hash/ZIP 및 보호 자료 불변을 기록한다. 기존 산출물을 재읽기만 했으며 새 계산·XLSM 생성·native 개방은 하지 않았다.

#### F-O-01 추가 갱신 없음 후속 — 2026-10-13

- 사용자 확인·정본: **F-O-01은 B8 외 갱신할 셀이 없다.** domain §6.1에 반영했다. B8 SET 0.34→0.35만 적용하고 P Helper·연도 헤더 등 다른 값/수식/저장값·서식은 원본 보존한다. 다른 OUTPUT 입력/시트·구조 변경으로 확대하지 않는다.
- 현재 상태: 입력/보존 Pass, **수식 계산·Excel 계산 정답 해당 없음**, native 재개방 PENDING. 기존 PENDING 기록은 이 결정 전 이력으로 유지하고 현재 상태는 이 후속을 따른다.
- 근거: [새 정책 후속 검증](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/2026-10-13-f-o-01-policy-_fila3gt/policy-result.json)에서 기존 산출물/source hash·B8 typed 값·Helper 809수식/저장값·나머지 셀/metadata/part 보존·ZIP·기존 자료 불변을 확인한다. 새 XLSM 생성이나 계산은 필요하지 않아 수행하지 않았다.

#### F-O-03 C2 빈 값 금지 후속 — 2026-10-13

- 업무 정본: domain §6.1의 **C2 사용자 확인**을 따른다. 기존 F-O-03 CLEAR 양성 명세(허용 입력을 성공 처리하는 사례)는 폐기하고 CLEAR 거부 기대값으로 변경한다. C2를 0으로 SET하는 것은 별도 변경이며 기존 빈 산출물을 치환하거나 재사용해 성공 판정하지 않는다.
- 현재 상태: 기존 CLEAR 파일의 업무 입력 **Fail**, 빈 셀 생성/보존 및 기존 native UI 관측은 기술 기록으로 유지한다. 제품에서 CLEAR를 거부하고 직전 값을 유지하는 검증은 **PENDING**(제품 구현 범위 밖). 수식 계산/Excel 계산 정답은 해당 없음이다. F-O-02의 숫자 SET 결과는 영향이 없다.
- 근거/이력: [새 정책 후속 결과](../../temp/input-02a-engine/2026-10-13-f-o-03-policy-j7_q3og3/policy-result.json)에 사용자 결정·정본 위치·source/기존 산출물 hash·C2 재읽기와 현재 판정을 연결한다. 이전 fixture/cases.json·검증 결과·XLSM은 수정하지 않고 당시 기록으로 보관한다. 다른 셀/표의 CLEAR 허용을 추론하지 않는다.

#### F-S-06 PipelineAdjustment H67 CLEAR 대체 표본 — 2026-10-13

- 사용자 지정: SUPPLY `PipelineAdjustment!H67`을 F-O-03 대신 CLEAR 표본으로 선정했다. 후속 답변으로 **허용된 비우기 사례**임을 확인했으며 업무 정본은 domain §6 규칙 62다. 그 뒤 H67 실제 blank에서 I67 `Error - enter value`가 정상 CHECK 결과임을 사용자 확인했다(아래 기대값 확정 후속). F-O-03의 기존 업무 Fail/기술 보존 이력과 C2 빈 값 금지는 유지한다.
- 독립 변경/비교: 고정 정규화 SUPPLY source에서 새 파일을 생성해 H67 number→실제 blank만 적용했다. 0·빈 문자열·행 삭제 없이 기존 셀/style을 유지했다. 모든 선택 시트 1,809셀을 좌표별로 대조해 다른 1,808셀·788수식/저장값을 보존했고 B7:I200·H8:H200·I8:I200 범위에서 허용 H67과 보존 대상을 분리했다. 모든 다른 part 및 별도 행/열·validation·metadata 불변, hidden 6시트·externalLink/캐시·pivot/캐시·names 12개·styles/calcChain/dynamic metadata 보존 Pass. source에 없는 VBA·connection/native Table 등은 해당 없음이다.
- 초기 입력-only 실행 상태: **입력 Pass / 원본 보존 Pass / 구조 해당 없음**, 당시 계산·Excel 정답·native PENDING. I67 shared 수식/원본 캐시 공백을 보존했고 재계산하지 않았다. 이 초기 파일의 캐시는 계산 정답이 아니다. 이후 I67/I3 정책·수정 Excel 정답을 확보한 새 POI 실행/비교는 아래 후속을 따른다.
- 근거/재현: [새 실행 안내](../../temp/input-02a-engine/2026-10-13-f-s-06-clear-rms9uajc/README.md), [실행 결과/source·산출물 hash](../../temp/input-02a-engine/2026-10-13-f-s-06-clear-rms9uajc/execution/result.json), [새 XLSM](../../temp/input-02a-engine/2026-10-13-f-s-06-clear-rms9uajc/execution/F-S-06-SUPPLY-H67-CLEAR.xlsm), `case.json`·`source-observations.json`·`preservation.json`·환경/명령·복사 도구 provenance·로그·보호 hash에 실제 값/타입·범위·근거를 기록한다. 원본/대장/기존 자료 459경로 hash 불변·ZIP/XML·저장 후 재읽기 및 새 replay 경로의 같은 XLSM hash 확인 Pass. Java/POI 계산·native·외부 workbook/링크/매크로/새로고침은 미실행이다.
- 후속 경과: I67 기대값 이후 계산 범위도 I67/I3·나머지 원본 보존으로 확정됐다. 최신 Excel 값 확보와 실제 POI 비교는 아래 후속을 따른다. 초기 [제출 명세](../../temp/input-02a-engine/2026-10-13-f-s-06-clear-rms9uajc/excel-submission.json)와 I67만 확정한 제출 기록은 당시 대기 이력이다. 파일 밖 모델 영향을 이번 부분 실행만으로 검증했다고 하지 않는다.

#### F-S-06 I67 정상 CHECK 기대값 확정 후속 — 2026-10-13

- 당시 사용자 확인/상태: H67이 실제 blank일 때 I67 `Error - enter value`가 정상 동작이다(domain 규칙 62). **I67 CHECK 결과 정책 대기 해소**, 입력/기술 보존 Pass 유지. 이 문구는 문자열 경고이며 Excel 수식 오류 코드가 아니다. 당시 실제 평가·저장/동일 변경 Excel 정답·native는 PENDING이었으며 현재 상태는 아래 POI 후속을 따른다.
- 좁은 연결 확인: 기존 source의 I67은 H67과 B67/C67/D67을 읽는다. 같은 시트 I3의 `193-COUNTIFS($I$8:$I$200," ")`는 I67을 포함해 CHECK 결과를 집계한다. 관측은 집계 계산 승인/Excel 정답이 아니며 I67만 확인해 전체 영향을 검증했다고 하지 않는다. 초기 CLEAR 파일의 I67 공백·I3 저장값은 원본 캐시로 유지했고 수정하지 않았다.
- 근거/다음: [새 정책/검증 결과](../../temp/input-02a-engine/2026-10-13-f-s-06-check-0spahxsg/policy-result.json), [최신 제출 명세](../../temp/input-02a-engine/2026-10-13-f-s-06-check-0spahxsg/excel-submission-latest.json)에 기대 타입/문구·정본 위치·source/산출물 hash·보호 자료·남은 범위/정답/native를 연결한다. 사용자 기대 문구를 golden이나 엔진 출력 생성에 쓰지 않고, 정책·같은 변경 Excel 정답이 모두 확보된 범위만 계산한다.

#### F-S-06 I67/I3 POI 선택 계산·Excel 비교 후속 — 2026-10-13

- 정책/정답: 사용자 확인한 I67 CHECK·I3 집계만 계산하고 나머지 수식/저장값은 원본 보존한다(domain 규칙 62). 최초 사용자 저장본은 H67 실제 blank인데 I67 공백/I3 원본 캐시가 남아 있었다. 사용자가 Excel 안에서 H67 값을 변경 후 실제 blank로 되돌려 저장한 수정본에서 두 결과가 갱신됐다. 최초 관측 hash와 수정본 hash/값·타입 및 수정본 snapshot은 새 실행 근거에 기록한다. 최초 상태 bytes는 사용자 수정 전에 별도 보관하지 못했고, 검사 중 과거 공백을 기대한 assert가 수정된 값에서 실패한 이력은 기존 경로에 유지했다. 이를 Excel/POI 계산 실패나 원본 변경으로 오인하지 않는다.
- 실제 실행: Java 17.0.14·POI 5.4.0 기존 기본 평가기로 승인 2셀을 평가했다(2 requested / 2 evaluated / 0 failures). 매 실행마다 정규화 SUPPLY source에서 새 H67 CLEAR 입력본을 독립 생성했다. I67은 source B67/C67/D67와 실제 blank H67을, I3은 승인 I67 새 결과와 나머지 I8:I200 source 캐시를 읽는다. golden은 unit 생성/엔진 입력·출력 생성에 쓰지 않고 평가·저장 뒤 비교에만 읽었다. 새 XLSM의 I67 문자열과 I3 숫자 값·타입이 동일 변경의 수정 Excel 정답 2셀 전체와 일치했다.
- 보존/재현: H67 내용과 I67/I3 저장값만 변경했다. 선택 시트 1,809셀 중 나머지 1,806셀과 788수식 원문/shared 속성·서식/타입·행/열/validation/non-cell XML 및 다른 part 불변, hidden 6시트·externalLink/cache·pivot/cache·names 12개·styles/calcChain/dynamic metadata 보존 Pass. 원본/대장/기존 자료/제출본 523경로 전후 hash·ZIP/저장 후 재읽기 Pass. 독립 replay도 평가/비교/보존 Pass이며 XLSM hash가 동일하다. 정상 CHECK 경고를 수식 오류로 분류하거나 공백/0으로 대체하지 않았다.
- 근거: [새 실행 안내](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/README.md), [실제 결과·2셀 상태](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/execution/summary.json), [평가/저장값·Excel 비교](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/execution/F-S-06/calculation-comparison.json), [POI 계산 XLSM](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/execution/F-S-06/F-S-06-SUPPLY-POI-RESULT.xlsm), source/입력/unit provenance·Java/jar/도구 hash·명령/log·보존·독립 replay/최초 관측은 같은 Git 제외 경로에 있다. runtime Java 버전은 실제 `java-version.log`에 있고 stdout-only 환경 필드가 비어 있던 점은 `final-environment.json`에 별도로 정정해 원기록을 유지한다.
- native/한계: [native 관측](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/native-observations.json)의 최종 상태는 **PENDING**이다. 같은 hash/0444 결과 복사본을 준비하고 사용자 승인 후 수동 계산으로 전환했으나, 두 번 모두 목표 대신 이전 F-O-03 읽기 전용 복사본이 열려 무저장 닫고 추가 시도를 중단했다. F-S-06은 열지 못해 native 값·복구 경고 미확인이다. Excel 기본 설정의 자동=1/수동=0으로 복원을 확인하고 기존 통합 문서8을 유지했다. 설치된 Excel 16.113.4/build 26100421은 현 host 정보이며 golden 작성 당시 버전의 직접 사용자 확인과 구분한다. [최종 상태](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/final-summary.json)·[최종 hash/ZIP/문서 검사](../../temp/input-02a-engine/2026-10-13-f-s-06-poi-m51vi4cc/final-verification.json)를 연결한다. JUnit/Gradle 테스트·상용 엔진·매크로/외부 workbook/링크·connection/pivot 새로고침은 미실행이다. F-S-06 부분 검증을 전체 INPUT-02A 완료/GO나 일반 Pipeline 정책 확정으로 확대하지 않는다.

#### Source 식별·로컬 자료

사용자 지정 `docs/references/model-input/{ROLE}-normalized.xlsm` 네 파일을 기존 manifest와 대조했다. 작업 전후 hash가 아래와 같고 ZIP CRC(압축 파일 내용 손상 검사)도 통과했다. 같은 폴더의 조사 대장 XLSX 1개도 작업 전후 hash 불변이다. 전체 함수/영향 조사를 다시 하지 않고 선정 셀·표 경계 및 보존 metadata의 존재만 확인했다.

| 역할 | 정규화 source SHA-256 |
| --- | --- |
| DEMAND | `ef516b6de2a829c891dbd991ba9f9d626f104dcae2e5ecd8128b1f0ae31c23bd` |
| OUTPUT | `69742cc9c6fba170324c09c21986574a390c741ba26d371acdce570aeaac28ce` |
| SUPPLY | `eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36` |
| TRADE | `b1211662aa00d49d9f03c1d732acd495cf99446ede69abde34565007b3819a31` |

로컬 실행 디렉터리는 `temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/`다. Git 제외 여부를 확인했으며 기존 파일을 덮어쓰지 않았다.

- [사례별 실제 값·타입·source/복사본 hash·보존 범위](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/prepared/cases.json): 각 사례의 변경 전후 실제 값, XML 타입, 계산 비교 셀, 정책, 상태와 담당 역할을 소유하는 로컬 실행 자료다. 업무 값은 이 Markdown에 복제하지 않는다.
- [미결정 사례 명세](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/pending-cases.json), [Excel 결과 제출 양식](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/excel-submission-template.json).
- [원본 보존 기준](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/preservation-baseline.json), [표본 검사 결과](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/prepared/verification.json), [자료별 hash](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/handoff-hashes.json).
- `source-inspection.json`·`selection-details.json`은 선정 범위의 원본 셀·metadata·외부 LUT 캐시 관측이다. `input-hashes-before.json`은 XLSM 4개·XLSX 1개의 작업 전 hash다. 이 자료와 복사본은 로컬 보관이며 Git checkout만으로 전달되지 않는다. 다른 환경으로 인계할 때 승인된 파일 전달 경로로 묶음을 함께 전달하고 hash를 확인한다.

#### 준비 완료 사례와 비교 연결

각 사례는 같은 역할의 정규화 source에서 **독립적으로** 시작한다. `source + 증분`은 재현용 합성 변경이며 실제 변경 전후 값은 `cases.json`에 정확히 기록했다. SET은 타입을 유지해 값을 지정하고 CLEAR는 값 요소를 제거한 실제 빈 셀이다. CLEAR는 0, 빈 문자열 수식, 행 삭제가 아니다. 아래는 당시 준비한 10사례의 이력이며, **F-O-03 CLEAR는 후속 사용자 결정으로 허용 입력 명세에서 제외**했다. 구조/metadata 변화는 **없음**이며 입력 셀 내용과 문자열 저장 표현 외 변경이 없다. 계산 캐시는 원래 값으로 남아 있으므로 복사본은 계산 결과 파일로 사용할 수 없다.

| 사례 ID / V-ID | 역할·시트·표 / 정책 근거 | 변경·타입 | 계산 비교 셀·기대값 출처 | 보존 범위·준비 한계 |
| --- | --- | --- | --- | --- |
| F-D-01 / V-DEMAND-BCINPUT | DEMAND `BCInput` T-023 / 규칙58 | N4 SET, number → number, source +0.125 | 지정 입력 외 모든 수식/저장값 원본 보존. 계산/Excel 정답 해당 없음 | Helper·AE:AF·X/AG/AO4 등 N4 외 전체 보존. 입력/보존·기존 native Pass, 지정 검증 Complete |
| F-D-02 / V-DEMAND-LNG | DEMAND `LNG Demand` T-021 / 규칙 50 | E6 SET, number → number, source + 1 | 재저장본의 E5·E4·E109 갱신과 전체 승인 130셀 Excel 저장값 확보·산술 일치 | C1 validation 조건·C:D 이름/헤더·24행 값·합계 수식 유지. POI 계산/보존 Pass; native 원산출물 재개방·동일 hash 복사본 130셀 값 유지 Pass(아래 한계 참조) |
| F-S-01 / V-REGAS-VALUE | SUPPLY `RegasAdjustment` T-106~T-108 / 규칙 51 | H11 SET, number → number, source + 0.125 | DN11·H435 갱신, DO11·I435 유지; DN11:DV382·H387:DK444의 Excel 저장값 9,612셀 확보·산술 일치 | B:G·AF:DK 등 계산 입력·수식 유지. 국가 연결 D11/G435 확인. Excel 버전 16.113.4 / build 26100421. POI 계산/보존 Pass; native 원산출물 재개방·동일 hash 복사본 9,612셀 값 유지 Pass(아래 한계 참조) |
| F-S-02 / V-BCHURRICANES | SUPPLY `BCHurricanes` T-112 / 규칙 54 | E8 SET string `1 week`, F8 SET number `0.25` | G8:G11·G16:G19의 Excel 저장값 확보(16.113.4 / build 26100421), 8셀 산술 대조 일치 | G 수식·고정 LUT 캐시 값 유지. Excel 비교 파일과 source 보존 기준을 구분. POI 계산/보존 Pass; native 원산출물 재개방·동일 hash 복사본 8셀 값 유지 Pass(아래 한계 참조) |
| F-S-03 / V-BCHURRICANES | SUPPLY `BCHurricanes` T-113 / 규칙 54 | E16 SET string `1 week`, F16 SET number `0.25` | G8:G11·G16:G19의 Excel 저장값 8셀 확보·산술 일치 | T-112와 독립 복사본. G 수식·고정 LUT AH14:AI17 값/타입 유지. 같은 버전 Excel. POI 계산/보존 Pass; native 원산출물 재개방·동일 hash 복사본 8셀 값 유지 Pass(아래 한계 참조) |
| F-T-01 / V-MIDSTREAM-VALUE·V-COPY·V-OPTIONAL-FUNCTION | TRADE `MidstreamContractInput` T-139 / 규칙59·57 | Q5 SET, number(Year) → number, source +1 | Q5 외 전체 계산 제외·원본 수식/저장값/타입/서식 보존, 계산/Excel 정답 해당 없음 | AF/AH·AG/AI·CN/CT·다른 행·A/UNIQUE·backup/Industry 보존 Pass. 기존 native Pass, 지정 검증 Complete. 다른 사례로 확대하지 않음 |
| F-T-02 / 같은 V-ID | TRADE 같은 시트 T-139 / 규칙59·57 | U5 SET, number → number, source +0.125 | U5 외 비오류/기존 오류 수식·저장값 전체 보존. 계산/Excel 정답 해당 없음 | F-T-01과 독립. 다른 행/표 밖·A·UNIQUE 등 보존 및 기존 native Pass, 지정 검증 Complete. 기존 오류를 계산 정답으로 사용하지 않음 |
| F-O-01 / V-OUTPUT-COST·V-OUTPUT-PRESERVE | OUTPUT `USCostofSupply` T-081/T-082 / 규칙 50·§6.1·57 | B8 SET, number → number, source + 0.01 | 사용자 확인: B8 외 추가 갱신 없음. 계산/Excel 계산 정답 해당 없음 | 입력/보존 Pass. P8은 B8을 읽는 수식이지만 원본 수식·저장값 유지. 연도 헤더/다른 셀·metadata도 원본 보존 |
| F-O-02 / V-OUTPUT-SEASON | OUTPUT `PriceSeasonality` T-098 / 규칙 50·§6.1 | C2 SET, number → number, source + 0.125 | 입력 typed 값, 다른 셀 source. 이 표의 수식 계산은 해당 없음 | C2 이외 값·지역/월 헤더·행/열 유지. 원본 C2에서 시작한 F-O-03과 독립 |
| F-O-03 / V-OUTPUT-SEASON | OUTPUT 같은 시트 T-098 / §6.1 C2 사용자 확인 | 당시 C2 CLEAR, number → blank. 현재 허용 입력 명세에서 제외 | 업무 입력 Fail. 제품 CLEAR 거부 검증 PENDING; 기존 빈 셀 생성/보존·UI 관측은 기술 이력 | 기존 셀·행·서식 보존 기록 유지. 0으로 치환하거나 성공 입력으로 재사용하지 않음 |

현재 source에서 과거 `CoalGasAdjustment!E7` 후보는 literal 입력이 아닌 오류 수식이다. 이를 과거 TC-SRC 좌표만으로 덮어쓰지 않고 F-D-01로 대체했다. 이는 일반 조정표의 편집 승인을 취소하거나 전체 경계를 재조사한 결과가 아니다.

복사본은 위 로컬 디렉터리의 `prepared/{ID}-{ROLE}-NOT-CALCULATED.xlsm`이다.

| 사례 | 복사본 SHA-256 |
| --- | --- |
| F-D-01 | `b577893b5976cb95d49b98d2fd465c3d6a5183375064a3b741eded8f8af85d3d` |
| F-D-02 | `e42a1167d1c17618ca216826bf71c5bf5fc30680a814c6cf21143d6c848d82ba` |
| F-S-01 | `decdc24850e34c9792b92824420950ccca86135813ff2f8b23551a78b72c931f` |
| F-S-02 | `97f00e1a8d668474f7a6e87890ba4670d74ebf8639d70c5bb2dcee27fc45b7a0` |
| F-S-03 | `086e93b2fbb26c47748c416853cc5e2fa57453c7e3496c2974aeee9eb5e71dbf` |
| F-T-01 | `a39625c440254b24719f333f4f5670348499dae0c8ff30696f79304c54664867` |
| F-T-02 | `afb9037a345827e5340ec50200c73dfb3eb0eb017ed093feda7d3c9f2440099f` |
| F-O-01 | `6eb70d23e48d7514a9fe6a06aad9af1fd4be1b5913e201769efb0fc652ea514f` |
| F-O-02 | `e375f7c6647cdbf83a7be66e54ddd25ef9d5ba43f99720bccba4643c35c555c6` |
| F-O-03 | `2d243759a52f6b2e20a7bcfbb3423614da9549c40e88253b26b309fe09850d92` |

#### 4사례 golden 제출값 확인 — 2026-10-12

사용자가 제출한 `prepared/{ID}-{ROLE}-CALCULATED-golden.xlsm` 네 파일을 같은 source/독립 변경 조건과 대조했다. F-S-01·F-S-03도 기존 F-S-02와 같은 **Excel for Mac 16.113.4 (26100421)**에서 계산·저장했다는 사용자 확인을 받았다. F-D-02는 같은 Excel 작업의 후속 시트 계산·재저장 보고와 native Mac Excel 저장본을 확인했다. 아래는 최종 저장본의 상태이며 파일 이름을 근거로 정하지 않았다.

| 사례 | 전체 승인 비교 범위 / 저장값 확인 | 현재 상태 | 제출 파일 SHA-256 |
| --- | --- | --- | --- |
| F-D-02 | LNG 승인 합계 130셀 모두 number·오류 없음·산술 일치. E5·E4·E109 갱신 | Excel 비교값 AVAILABLE — 사용자 후속 시트 계산·재저장본 | `b03025d35bbb3ec6fa0db90042a60715ee90e0523f8e8f353ba30a3fc165a1d3` |
| F-S-01 | 연간 3,348 + 국가 월합 6,264 = 9,612셀 모두 number·오류 없음·산술 일치. DN11/H435 갱신, DO11/I435 유지 | Excel 비교값 AVAILABLE | `d89fd6bb2fd0bf90317cf020e4352c8b0d1ded857480125d5fcf2d4ce8406978` |
| F-S-02 | G8:G11·G16:G19의 8셀 모두 number·오류 없음·고정 LUT 산술 일치. 영향 G8 갱신 | Excel 비교값 AVAILABLE | `7cb73812dd9863f54a13c65f328d01a008b6c54cca73677b613c6c0e8306fdd4` |
| F-S-03 | 같은 8셀 모두 number·오류 없음·고정 LUT 산술 일치. 영향 G16 갱신, G8 유지 | Excel 비교값 AVAILABLE | `660f9a867ffa63dda0e8ab828edb4cf2b3400255f91406895f7d0539295295d5` |

- [최종 4사례 입력·전체 저장값/타입/오류·수식·허용 오차·hash 검사](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-four-golden-2026-10-12-9i34qjby/verification-recheck-lng.json), [확보한 Excel 결과 9,758셀](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-four-golden-2026-10-12-9i34qjby/excel-results-four-confirmed.json), [사례별 다음 작업·자료 hash](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-four-golden-2026-10-12-9i34qjby/handoff-summary-confirmed.json)를 연결한다. 실제 업무 값은 로컬 JSON에만 기록한다. 최초 F-D-02 미갱신/3사례 확보 이력은 같은 디렉터리의 `verification-final.json`·`excel-results-four.json`·`handoff-summary.json`으로 보존한다.
- 네 사례의 입력/타입과 전체 승인 수식이 명세/source와 일치했다. 선정 시트의 승인 계산 밖 값·타입·수식·숫자 서식, validation 조건·merge와 workbook의 시트 순서/숨김 상태·defined names 관측은 유지됐다. Hurricanes의 고정 LUT 8캐시 셀도 값/타입을 유지했다. LNG C1 validation에는 Excel 저장 후 revision `uid` 속성만 추가됐고 대상/목록/조건은 그대로다([차이 확인](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-four-golden-2026-10-12-9i34qjby/verification-lng-validation.json)). 이는 선정 범위 검사이며 전체 workbook 보존 인증은 아니다.
- SUPPLY golden의 외부 링크 대상 경로는 source와 달라졌다. 고정 LUT와 계산에 읽힌 선정 시트의 입력값은 유지됐다. 이 Excel 저장본의 링크 경로를 제품 출력의 기대 변경으로 승인하지 않는다. **계산값은 Excel golden, 원본 보존은 변경 전 정규화 source**로 비교한다. part 재번호화나 XML bytes 차이를 무조건 보존 실패로 처리하지 않는다.
- 세 SUPPLY 파일에 저장된 `calcPr`는 manual·calcOnSave=0이며 F-S-01은 calcCompleted=0도 남아 있다. 이 설정만으로 값 미갱신을 선언하지 않고 실제 저장값을 대조했다. 실행 전 Application 설정·정확한 계산 호출 순서/시각·새로고침 동작은 미보고로 남긴다. 비교값은 사용자 Excel 저장본에서 확보했고 제한된 SUM/SUMIFS·고정 LUT의 독립 산술은 검산이며 후보 엔진 PoC가 아니다.
- 원본 4파일·조사 대장 1개가 초기 표본 준비 때의 hash를 유지하며 manifest와 일치한다. 제출 4파일 및 비교한 source/미계산 복사본의 ZIP 무결성·읽기 전후 hash 불변도 확인했다. 모든 XLSM은 읽기 전용으로 검사했다.
- F-D-02 후속: 최초 제출은 E6만 바뀌고 영향 3셀의 결과가 남아 있어 계산·저장을 요청했다. 사용자 시트 계산 후 새 hash와 130셀 일치·E5/E4/E109만 변경된 것을 확인했다. 당시 인계 검사 중 F-D-02 hash 변경을 감지한 것은 이 사용자 재저장과 일치하며 새 기준으로 재검사했다. 미갱신 이력을 덮어쓰지 않았다.
- 사람 확인/다음 작업: 이 제출 확인 단계에서 **F-D-02·F-S-01·02·03**을 엔진 실행에 인계했다. 후속 POI 부분 실행과 그 산출물의 2026-10-13 native 재개방/복사본 값 유지 확인은 아래 결과를 따른다. 다른 준비 6사례의 Excel 확인·계산 정책과 구조/CLEAR 8미결정 명세는 남는다. 실명 담당자 지정은 새 선행조건이 아니며 현재는 사용자가 Excel 확인과 실행을 수행한다.

재현 검사: 같은 디렉터리의 `verify_four.py`를 번들 Python `-B`로 실행하고 `--output`에 새 JSON 경로를 지정한다. 기존 검사 파일을 덮어쓰지 않는다. 전체 검사 범위와 산술 조건은 코드/JSON에 기록했으며 공유 수식은 각 셀의 실제 참조로 풀어 source와 대조한다.

#### 4사례 POI 선택 계산 실행 — 2026-10-12

**부분 실행 완료:** Java 17.0.14 / Apache POI 5.4.0 기본 `FormulaEvaluator`로 각 독립 미계산 복사본의 승인 수식만 평가했다. 작은 메모리 workbook에 승인 수식과 읽기 값만 적재하고, POI 반환 결과를 원본 package 복사본에 저장한 뒤 다시 읽었다. golden은 비교용으로만 사용했다. 과거 선택 프로토타입의 계산 정책·산술/자기 일관성 결과나 전체 workbook 로딩 NO-GO를 이번 결과로 대체하지 않는다.

| 사례 | 실제 평가 / 저장 후 Excel golden 비교 | source 보존 | 영향 확인 |
| --- | --- | --- | --- |
| F-D-02 | E:N의 승인 13합계 행 130/130셀 Pass | Pass | E5 → E4 → E109 갱신, E6 외 입력 및 C1 validation/24행 값 유지 |
| F-S-01 | DN11:DV382·H387:DK444 9,612/9,612셀 Pass | Pass | DN11/H435 갱신, DO11/I435 유지. 연결 계산 밖 캐시 31,620셀은 읽기 상수로 사용하고 보존 |
| F-S-02 | G8:G11·G16:G19 8/8셀 Pass | Pass | source의 고정 LUT 8셀로 POI VLOOKUP 평가; G8 갱신·G16 유지 |
| F-S-03 | 같은 8/8셀 Pass, F-S-02와 독립 | Pass | 같은 고정 LUT로 G16 갱신·G8 유지 |

- 값·타입·오류: 최종 9,758셀 모두 numeric, 수식 평가 실패/오류 결과 0. 숫자는 확보 기록의 `abs(a-b) <= max(1e-9, 1e-9*max(abs(a),abs(b)))`를 사용했다. 실제 평가 셀·보존 입력 캐시·수식·POI 반환값·저장값·Excel 값과 차이는 로컬 사례별 JSON으로 구분한다.
- Hurricanes 최초 실행: 두 사례의 8셀씩 모두 `WorkbookNotFoundException`으로 평가 실패했다. 메모리 LUT만 evaluator map에 등록하고 주 평가기를 빠뜨린 도구 설정이 원인이다. 주 평가기와 LUT 평가기를 함께 등록한 후 두 사례만 재실행해 해소했다. 최초 실패 로그/진단 XLSM은 유지하며 그 G 캐시를 계산 성공으로 사용하지 않았다. 수식의 `[1]LUT` 참조는 평가 입력과 생성 XLSM 모두 유지했다. source 캐시만 사용했고 외부 workbook 개방·링크 갱신·매크로 실행은 없었다.
- 보존: 변경 전 정규화 source에 셀별 값·타입·수식/shared 속성·저장값·style 및 셀 밖 XML/validation·merge·조건부서식을 대조했다. 같은 시트의 승인 변경과 보존 셀을 분리했고, 모든 다른 part는 개별 bytes/hash가 같다. 실제 DEMAND/SUPPLY의 hidden 상태/시트·names·styles·calcChain, SUPPLY externalLink/캐시/관계·pivot/캐시·dynamic metadata가 유지됐다. 해당 source에 VBA/connection/native Table은 없음을 기록했으며 부재를 보존 시험 성공으로 확대하지 않는다. golden의 링크 경로 변경과 validation uid 추가는 출력에 반영하지 않았다.
- 산출물/재현: Git 제외 새 [실행 폴더 안내·재현 명령](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/README.md), [최종 사례별 결과·XLSM 경로/hash](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/final-summary.json), [엔진/버전/jar hash·설정](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/environment.json), [최초 명령](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/executed-commands.json) 및 [Hurricanes 재실행 명령](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/hurricanes-retry/executed-commands.json)을 연결한다. 사례별 `unit-provenance.json`, `calculation-comparison.json`, `preservation.json`이 상세 근거다. Java/JUnit 테스트는 사용하지 않고 standalone 실제 평가를 실행했다. 힙 768m은 실행 설정이며 성능/동시성 결론이 아니다.
- 검증/한계: 원본 전달 4파일·snapshot/정규화본·조사 대장·기존 표본/golden/기록을 포함한 90개 경로의 작업 전후 hash 불변, 생성 4파일 ZIP CRC·저장 후 재읽기 Pass. [최종 저장값/Excel 기록·전체 XML 검사](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/final-saved-audit.json)도 9,758셀 Pass. 2026-10-12 실행 당시 native 재개방은 **NOT_RUN**이었고, 후속 2026-10-13 직접 재개방 경고 확인/동일 hash 복사본의 값 유지 결과는 다음 절을 따른다. ZIP/XML 검사만으로 native 성공을 선언하지 않는다. 나머지 준비 6사례·8미결정 명세·구조 변경·4역할 전체/모델 E2E·성능/동시성·DRM/운영 경계·엔진 선정/배포는 남아 있다. INPUT-02A 티켓/전체 조사 상태는 변경하지 않는다.

#### 4사례 native Excel 재개방과 값 유지 확인 — 2026-10-13

**부분 확인 완료:** 최종 POI 산출물 네 파일을 Mac용 Microsoft Excel 16.113.4 (26100421)에서 직접 열고 저장 없이 닫았으며 복구 경고는 미관측이다. SUPPLY 세 파일의 일반 외부 링크 안내는 `업데이트하지 않음`으로 거부했다. 이후 사용자 승인 AppleScript로 동일 bytes/hash의 읽기 전용 복사본에서 전체 승인 계산값을 읽어 POI 저장값 유지 **Pass**를 확인했다. 복구 경고 관측과 native 값 읽기의 대상/조건을 구분한다.

| 사례 | native 값 읽기 범위 / 유지 판정 | 십진 표현의 최대 차이 |
| --- | --- | --- |
| F-D-02 | `LNG Demand` E:N의 승인 13합계 행 130/130셀 Pass | 0 |
| F-S-01 | `RegasAdjustment` DN11:DV382·H387:DK444 9,612/9,612셀 Pass | 1e-15 |
| F-S-02 | `BCHurricanes` G8:G11·G16:G19 8/8셀 Pass | 2e-17 |
| F-S-03 | 같은 8/8셀 Pass, 독립 산출물 | 2e-17 |

- 값·타입·오류: Excel의 실제 `value2` 반환값 9,758셀 모두 numeric·오류/누락/중복 0. 기존 `abs(a-b) <= max(1e-9, 1e-9*max(abs(a),abs(b)))` 내 일치하며, 보고 값을 IEEE754 float64로 재읽으면 전부 동일하다. JSON/XML 십진 표현만 473셀에서 마지막 자리가 다르다. 이는 native 재개방 후 값 유지 검사이며 새 수식 평가·변경 입력 재계산이나 Excel golden 확보를 대신하지 않는다.
- 복사본 경계: writable 원산출물에서 `read only true`가 실제 적용되지 않아 최초 F-D-02 두 시도는 값 읽기 전에 중단하고 무저장 닫았다. 원산출물과 동일 hash인 새 복사본의 파일 권한을 0444로 고정한 뒤 Excel의 실제 readOnly=true를 확인했다. **원산출물 직접 재개방 경고 확인은 수행했지만, AppleScript의 전체 값 읽기는 이 읽기 전용 복사본에서 수행했다.** 원산출물의 권한/내용·기존 표본/golden/기록은 변경하지 않았다. Hurricanes 대상은 초기 실패 진단 파일이 아닌 `hurricanes-retry/` 최종 산출물이다.
- 실행 조건: 값/수식 없는 임시 helper로 개방 전 수동 계산 모드를 고정하고 `msoAutomationSecurityForceDisable`·`update links do not update links`를 적용했다. 개방 후 manual·forceDisable·readOnly·alerts=true를 확인하고 값/수식만 읽었다. calculate/evaluate·셀 쓰기·링크/외부 workbook/connection/pivot 새로고침·macro·save/save-as를 요청하지 않았다. 네 대상 모두 닫기 전 saved=true이며 `close ... saving no`로 닫았다. 대상 닫기 후 helper만 남은 상태에서 기존 앱 설정을 복원하고 helper도 무저장 닫았다. 최종 열린 workbook 0개를 확인했다.
- 수식/보존: LNG/Regas 9,742수식은 leading `=` 외 텍스트가 같다. Hurricanes 16수식은 `[1]LUT`가 source의 기존 externalLinkPath rId2 경로로 펼쳐졌으며, 그 표기만 읽기 비교에서 정규화하면 나머지도 같다. 디스크 수식·externalLink/캐시는 hash 불변이며 이 표시 차이를 승인 파일 변경으로 가져오지 않았다. 기존 입력 90경로와 산출물 4개 총 94경로, 읽기 전용 복사본 4개의 전후 hash 불변·산출물 ZIP CRC/전체 XML·관계 part parse Pass. 네 대상에 VBA/XLM macro part가 없음을 재확인했고 그 부재를 macro 동작 검증으로 확대하지 않는다.
- 근거: [최종 native 결과·설정/범위](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/native-summary.json), [동일 hash 읽기 전용 복사본 manifest](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/read-only-copies-manifest.json), [실행·재현/실패 이력](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/README.md) 및 [stable 실행 기록](../work-notes/2026/2026-09-03-model-input-transfer-format.md#2026-10-13-생성-4사례-native-excel-무저장-재개방-결과)을 연결한다. 실제 값·셀별 비교·로그는 Git 제외 temp에만 있다.
- 남은 범위: 최초 직접 UI 재개방은 개방 전 앱 계산 모드가 자동이었으며 내부 재계산 여부는 확인하지 않았다. 이후 AppleScript 값 읽기는 별도로 개방 전 수동 계산을 고정했다. Excel 내부 이벤트/네트워크 계측·전체 visual 비교·native 재계산/저장 cycle·모델 실행은 수행하지 않았다. 나머지 6준비 사례·8미결정 명세·구조/4역할 전체·운영/DRM·엔진 선정은 남으며 INPUT-02A의 `BLOCKED` 및 전체 조사 In Progress를 유지한다. 전체 workbook 경로 `NO-GO`와 이번 POI 선택 계산/후속 값 유지 Pass도 구분한다.

#### F-S-02 제출 계산 파일 확인 이력 — 2026-10-12

아래는 Excel golden 제출본의 당시 확인 이력이다. 이후 POI 최종 산출물의 계산/보존과 2026-10-13 native 재개방·동일 hash 복사본 값 유지 확인은 위 별도 결과를 따른다.

- 1차 사용자 제출 [F-S-02-SUPPLY-CALCULATED-golden.xlsm](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/prepared/F-S-02-SUPPLY-CALCULATED-golden.xlsm)을 읽기 전용으로 확인했다. 이름은 `CALCULATED`이지만 당시 SHA-256과 모든 package part bytes가 기존 F-S-02 미계산 복사본과 같았다.
- 1차 제출본의 E8/F8 변경 입력·타입은 사례 명세와 일치했다. SUPPLY source hash는 기존 manifest와 일치하며, 당시 source 대비 두 입력 셀 외 worksheet 내용과 나머지 part, G 수식·고정 LUT 캐시·외부 링크·서식/validation/hidden 등은 보존됐다. 제출본·미계산 복사본·source ZIP 검사와 읽기 전후 hash 불변을 확인했다.
- G8:G11·G16:G19 저장값을 고정 캐시의 독립 산술값과 대조한 결과, 변경 입력의 영향 셀 G8은 불일치다. 나머지 7셀의 일치는 입력 변경 영향 밖의 기존 저장값 일치이며 새 재계산 증거가 아니다. 실제 값·타입·차이는 [로컬 검증 결과](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-f-s-02-2026-10-12-41oa67gp/verification.json)에 기록했다. 계산 결과가 이 제출 파일에 저장됐다고 확인할 수 없어 F-S-02 Excel 정답은 PENDING을 유지한다. 실제 계산·저장 파일과 계산 도구/버전·설정·범위 확인이 다음 작업이며 엔진 검증 NOT_RUN·INPUT-02A 상태는 변경하지 않는다.
- 같은 날 2차 재확인: 같은 경로의 파일 hash는 `8ac80acf4eb4e168d47e634ed1e423650a5848438b422f540907671876952fc7`로 바뀌었고 producer는 `Microsoft Macintosh Excel`로 표시됐다. 입력/8수식·고정 LUT 캐시 값은 유지됐으나 G8 저장값 불일치는 그대로이며 `calcMode=manual`, `calcOnSave=0`다. ZIP·읽기 전후 hash 불변 Pass. 저장 과정의 part 좌표 재배치·XML 표현 변경은 원본 훼손으로 단정하지 않았고 전체 semantic 보존 검사는 아직 하지 않았다. [2차 실제 값/검증 결과](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-f-s-02-2026-10-12-41oa67gp/verification-recheck-2.json)를 보존하며 Excel 정답 PENDING을 유지한다.
- 3차 표시 서식 확인: 새 hash `d6071affea7fe40104423f2bb6a21990294072047cdf42a7c1b524c0cecb570e`의 G8은 numeric 타입·표시 서식 `0%`(numFmtId 9)이고 실제 저장값 불일치가 계속된다. 조건부 서식도 G8을 덮어쓰지 않아 단순 표시 반올림 문제가 아니다. [3차 저장값 검사](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-f-s-02-2026-10-12-41oa67gp/verification-format-check-3.json)에서 입력·8수식·LUT 캐시와 ZIP/hash 검사를 확인했다. [Microsoft 계산 설명](https://learn.microsoft.com/en-us/office/client-developer/excel/excel-recalculation)에 따라 Shift+F9는 계산 대상으로 표시된 셀만 처리한다. Excel 밖에서 입력을 바꾼 복사본의 변경 인식 누락은 현재 가설이며, Excel 안에서 승인 입력 E8을 다른 유효 값으로 잠시 변경 후 사례 값으로 복원하고 해당 시트를 계산·저장해 확인한다.
- Excel 결과 확보: 사용자가 Excel에서 E8 변경/복원 후 해당 시트 계산으로 표시값 갱신을 확인했고, 버전 `16.113.4 (26100421)`을 제공했다. 최종 제출본 hash는 `7cb73812dd9863f54a13c65f328d01a008b6c54cca73677b613c6c0e8306fdd4`다. [확보한 Excel 값·실행 근거](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-f-s-02-2026-10-12-41oa67gp/excel-result-f-s-02-final.json)와 [최종 저장값/보존 대조](../../temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/review-f-s-02-2026-10-12-41oa67gp/verification-calculated-final.json)를 연결한다. 8셀 모두 numeric·오류 없음·산술값 일치이며, BCHurricanes 전체 셀의 값/수식은 초기 표본 대비 G8 저장 결과만 달라졌다. 고정 LUT 값·타입과 8수식, source hash·ZIP·읽기 전후 hash 불변도 확인했다. 비교값은 제출 Excel 파일에서 읽었고 독립 산술값은 교차 검산으로만 사용했다. 최초 3회 미계산 관측은 이력이며 F-S-02 비교값 상태를 현재 AVAILABLE로 갱신한다. 최종 native Excel 재개방·전체 workbook 보존 인증·후보 엔진 실행은 미수행이고 보고되지 않은 실행 세부는 JSON의 null/미확인으로 유지한다.

#### PENDING 사례·사람 확인 연결

아래는 초기 미완성 검증 명세와 후속 상태다. F-S-04 행/열 추가와 F-S-05 행/열 삭제를 각각 독립 명세로 나눴다(8묶음 → 10사례). 이후 F-T-04는 지정 슬롯 입력/보존·native Pass와 전체 계산 제외로 대기를 해소했고, F-D-04/F-T-03은 빈 값 금지·기본값0 사용자 확인으로 CLEAR 거부 기대값이 됐다. F-D-03의 새2036 O열 입력·구조/원본 보존·Excel16정답·POI16·최종 native는2026-10-14 Pass로 완료했다. F-D-05는 중간 삽입을 이번 범위에서 제외하고 마지막 행 추가/행 삭제2독립 명세로 분리했다. F-S-04는 master 마지막 행 추가/삭제·Regas 자동 추가/직접 구조 금지 사용자 확인 및 독립 삭제 명세를 추가했다. F-S-04-COLUMN의 지정2036/0 입력·구조/보존·Excel 정답·POI372·최종 native도 Pass다. F-O-04의21슬롯/2036·원본 가격값 복사·나머지 전체 보존·읽기 전용 native도 Pass다. F-S-05-ROW의 지정 원본11행 삭제/396행 이동·추가 계산 제외·원본 보존·읽기 전용 native도 Pass다. F-S-05-COLUMN도 원본H월 삭제/Period유지·Year82 Excel 정답/POI 비교·원본 보존·readOnly native Pass다. F-D-05-APPEND의 지정 Sector508·원본 D19:I19 복사/blank·추가 계산 제외·전체 보존·readOnly native도 Pass다. F-D-05-DELETE도 지정 원본 Sector19 삭제/D:K 동반 이동·K 참조/shared/chain 조정·저장값 보존·추가 계산 제외·readOnly native Pass다. F-S-04-ROW의 지정6,366셀 Excel/POI/native도 Pass다. F-S-04-DELETE의 지정Avalon8/국가6,264 Excel·POI/native도 Pass다. F-S-06 제외 현재 선정된 허용19사례의 지정 파일 검증은 완료·미실행 구조0·CLEAR0이다. 전체 INPUT-02A 완료 판정과 구분한다. 나머지는 필요한 결정 뒤에만 변경 복사본을 생성한다. 담당은 설명용 역할이며 별도 실명 지정·전체 조사 완료를 새 선행조건으로 두지 않는다. 자동 계산 가능 표시나 과거 PoC를 결정 근거로 사용하지 않는다. 선택 좌표·필수값·수식/참조 이동·서식/validation·집계/names/Table/calcChain의 기대 변경이 채워지기 전에는 구조 변경을 실행하지 않는다.

| 사례 / V-ID | 확정된 범위·선정 경계 | 필요한 확인·근거 | 담당 |
| --- | --- | --- | --- |
| F-D-03 / V-DEMAND-LNG·V-PACKAGE | T-021 새 O열: O3=2036·국가90 N literal 복사·SUM16(추가 O24/O25/O27 포함). 입력·수식/구조·원본 보존 Pass(domain 규칙50). 기존 E:N 보존 | 승인16셀 Excel 정답 확보·독립 POI16 실제 계산/비교·최종 native Pass. 이 지정 사례의 대기 해소. 새 국가값 일반 빈 값/필수 정책으로 확대하지 않음 | 아래 「F-D-03 Excel·POI·native 완료 후속」 |
| F-D-04 / V-DEMAND-LNG | 사용자 확인: `LNG Demand!E6` 빈 값 금지·기본값 숫자0(domain 규칙50). 실제 빈 셀 CLEAR는 거부 기대값 | 빈 값 허용 정책 대기 해소. CLEAR 성공 파일·계산/Excel 계산 정답·native는 해당 없음(미생성). 제품의 빈 값 거부/기본값 적용 검증은 별도 PENDING이며 SET0 실행으로 대체하지 않음 | 아래 「F-D-04 빈 값 금지·기본값0 후속」 |
| F-D-05-APPEND / V-DEMAND-OVERWRITE (기존 F-D-05) | domain 규칙45 최종 승인: T-018 Sector508 확장, D508:I508=원본 D19:I19 값/타입/blank·신규 D508:K508 서식·J/K blank·추가 계산 제외 | 입력/구조·원본 보존·독립 재생성/저장 재읽기·readOnly native Pass·Complete. 계산/Excel 계산 정답 해당 없음(PRESERVE). 지정 사례 대기 없음 | 아래 「F-D-05-APPEND Sector508 입력·보존·native 완료」 |
| F-D-05-DELETE / V-DEMAND-OVERWRITE (기존 F-D-05) | domain 규칙45 최종 승인: 원본 Sector19 삭제·D20:K507→D19:K506·tail8 blank, K488참조·K9/shared·chain 제거1/이동488, 저장값 기록 동반 이동·추가 계산 제외 | 입력/구조·원본 보존·독립 재생성/저장 재읽기·readOnly native Pass·Complete. 계산/Excel 계산 정답 해당 없음(PRESERVE). 지정 사례 대기 없음 | 아래 「F-D-05-DELETE Sector19 삭제·이동·보존·native 완료」 |
| F-S-04-ROW / V-REGAS-ROW (기존 F-S-04) | domain 규칙51 지정 승인: master380=원본8 식별/연간값 복사·새 설비명, 연결383 과거24숫자0·100식·국가 블록1행 이동/끝383·A380/R380 포함6,366계산 | 입력/구조·원본 보존·동일조건 Excel6,366정답/POI6,366비교·readOnly native 모두 Pass·Complete. 지정 사례 대기 없음·기존17허용 결과 유지 | 아래 「F-S-04-ROW 설비380/연결383·Excel6366/POI/native 완료」 |
| F-S-04-DELETE / V-REGAS-ROW·V-PACKAGE | domain 규칙51 지정 승인: 원본8 Avalon/연결11 삭제·371설비 동반 이동/37,842식 참조와 기록 cache 보존·국가 블록1행 위/시작11·끝381·6,264계산 | 입력/구조·원본 보존·Excel6,264정답/POI6,264비교·readOnly native 모두 Pass·Complete. 지정 사례 대기 없음 | 아래 「F-S-04-DELETE Avalon8/연결11·Excel6264/POI/native 완료」 |
| F-S-04-COLUMN / V-REGAS-COL (기존 F-S-04) | R7=2036·R8:R379 숫자0, 최대 용량 S7:S379 이동/MAX(G:R). Regas 전체 원본 보존(domain 규칙51). 입력·수식/구조·보존·저장 후 재읽기 Pass | S8:S379372셀 Excel 정답 확보·POI372 실제 계산/비교·최종 native Pass. Regas 변경/계산 해당 없음(PRESERVE), 전체 보존 Pass. 이 지정 사례의 대기 해소 | 아래 「F-S-04-COLUMN Excel·POI·native 완료 후속」 |
| F-S-05-ROW / V-LIQUTIL (기존 F-S-05) | domain 규칙47 지정 원본11행 Australia Pacific LNG T1 삭제: B12:CL407→B11:CL406 이동·407입력 실제 blank/기존 서식 유지. header83수식/cache·그 밖 원본 보존 | 입력/구조·보존·저장 후 재읽기·동일 hash 읽기 전용 native Pass. 계산/Excel 계산 정답 해당 없음(PRESERVE). 지정 사례의 남은 확인 없음·Complete | 아래 「F-S-05-ROW 11행 삭제·보존·native 완료」 |
| F-S-05-COLUMN / V-LIQUTIL (기존 F-S-05) | domain 규칙47 최종 승인: 원본H/2025-02/Period86 삭제, I:CL→H:CK·CL tail blank, 생존G7 참조/Year82수식 조정·Period85,87…168 유지 | 입력/구조·보존·Excel82정답·POI82계산/비교·readOnly native Pass·Complete. 이 지정 사례 대기 없음 | 아래 「F-S-05-COLUMN H월 삭제·Excel82·POI82·native 완료」 |
| F-T-03 / V-MIDSTREAM-VALUE·V-COPY | 사용자 확인: `MidstreamContractInput!U5` 빈 값 금지·기본값 숫자0(domain 규칙59/H3). 실제 빈 셀 CLEAR는 거부 기대값 | 빈 값 정책 대기 해소. 허용 CLEAR 파일·계산/Excel 계산 정답·native 해당 없음(미생성). 제품의 빈 값 거부/기본값 적용 검증은 별도 PENDING이며 SET0 실행으로 대체하지 않음 | 아래 「F-T-03 계약량 빈 값 금지·기본값0 후속」 |
| F-T-04 / V-MIDSTREAM-VALUE·V-COPY·V-PACKAGE | T-139의 704행 빈 슬롯 생성·Q:U 모두 필수. 지정 5입력/원본 보존 Pass | 사용자 후속 계산 제외 확정(domain 규칙 59): 계산·Excel 계산 정답 해당 없음(PRESERVE), native Pass. 이 지정 사례의 대기 해소. worksheet 행 삽입·2919 확장은 해당 없음 | 실행/재검증 근거는 아래 「F-T-04 전체 계산 제외·원본 보존 후속」 |
| F-O-04 / V-OUTPUT-PRICES·V-OUTPUT-PRESERVE·V-PACKAGE | T-083의21빈 슬롯 활성화: B21=2036·C21:E21=정규화 source C16:E16 numeric literal 복사. 기존21 General 유지·4입력 외 전체 원본 보존(domain §6.1). 입력/슬롯 구조·보존·저장 후 재읽기 Pass | 추가 계산·Excel 계산 정답 해당 없음(PRESERVE). 같은 hash0444 파일의 읽기 전용 native66셀 Pass. 이 지정 사례의 대기 해소;22신규 확장·일반 빈 가격 정책으로 확대하지 않음 | 아래 「F-O-04 21슬롯2036 입력·보존·native 완료」 |

#### F-S-05-COLUMN H월 삭제·Excel82·POI82·native 완료 — 2026-10-15

- 업무 기준은 domain 규칙47의2026-10-15 사용자 최종 결정이다. [최종 결과/source·prepared·oracle·final hash](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/final-summary.json)·[구조/입력 저장](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/prepared/result.json): 같은 정규화 SUPPLY에서 H7:H407을 제거하고 I7:CL407의32,882셀을 H7:CK407로 이동했다. `CL7:CL407` 401셀은 기존 style의 실제 blank, 생존Period83셀은85,87…168·Month/본문/설비 식별을 유지했다. 새H7은G7을 읽고82 IF를 새Month/앞Year로 조정해 원본 Year cache를 제거했다. shared 두 범위와chain83→82 이동/제거만 기대 metadata 변경이며 비대상2,514셀/G월·다른68/70source part·non-cell XML·물리layout/dimension·CF/names/hidden·backup/A/다른 시트·externalLink/cache/pivot/cache/dynamic metadata는 원본 보존이다. [이동 전후](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/prepared/cell-movements.json)·[tail 전후](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/prepared/tail-changes.json)·[chain](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/prepared/chain-changes.json)에 실제 값/타입/수식/서식/참조가 있다.
- [Excel 정답](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/golden-audit.json)·[native 실행 로그](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/golden/attempt-1-stdout.json): Excel16.113.4/API build1004에서 동일 prepared hash0444 복사본을 readOnly/manual/저장 전 계산 금지/forceDisable/doNotUpdateLinks/alerts=true로 열고 H7→…→CK7 **82셀만** 계산해 별도 golden으로 저장했다. 이후 무저장 닫기/기존 앱·workbook·저장 상태 복원 Pass다. golden의23시트2,774,944셀 좌표·값/타입·수식·해석된 서식을 비교해 승인82cache 밖 변화0을 확인했고 visibility/names 의미도 유지했다. native writer의 style ID/part 재배치는 의미로 비교했으며 oracle package를 최종 결과/보존 기준으로 쓰지 않았다.
- [source-only unit](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/poi/unit-provenance.json)·[실제 POI82/비교](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/evaluated/result.json): literal G7/Month83셀·새 IF82만 사용하고 원본 Year cache/golden/외부 참조 입력0이다. Java17.0.14/POI5.4.0 기본 평가기로 요청82/평가82/실패0/skip0, 최종 저장 후82셀 값/numeric타입/수식/오류 비교 Pass·최대 차이0(허용 오차 max(1e-9,1e-9×최대 절댓값)). [독립 replay](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/replay/result.json)도82/0/0·동일 final hash이며 golden을 읽지 않았다. 최종 작성은 prepared의82 `<v>` 추가뿐이고 나머지69/70prepared part·셀/서식/metadata/chain을 보존했다. ZIP/62XML·저장 후 재읽기 Pass, 다른 사례 반복0이다.
- [최종 readOnly native](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/native-result.json)·[전 표 값 비교](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/native-value-comparison.json): 동일 final hash0444 복사본의 `B7:CL407` 35,689개 값/타입·`H7:CK7` 82개 수식·backup/기존#REF!/비대상6표본을 확인했다. 최대 native 숫자 차이4E-16, 재계산/링크 갱신/매크로 실행/저장0·앱/workbook/저장 상태 복원 Pass다. 모든 서식/다른 시트/metadata 보존은 saved XML/part 검사이며 native 전수 인증으로 확대하지 않는다. 보존된 기존#REF!는 새 계산의 정답/허용 오류가 아니다.
- [재현 명령](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/README.md)·[최종 hash/문서 검사](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-15-f-s-05-column-poi-native-ni50y0cm/status-audit-current.json): 원본/source/대장·기존594자료 hash 불변·변경3문서 diff 검사 Pass다. 입력/구조·원본 보존·계산·Excel 정답·native 모두 Pass·지정 사례 Complete/대기 없음, 기존14사례와 다른4대기 명세는 유지했다. Java standalone 실제 primary82+replay82이며 JUnit/Gradle은 해당 없음이다. F-S-06 제외 허용15완료·미실행4구조·전체 INPUT-02A In Progress, 아래 미승인/미실행은 이번 결정 전 이력이다.

#### F-S-05-COLUMN 원본H월 삭제 영향·계산 후보 — 2026-10-14 (선정/계산 결정 전 이력)

- domain 규칙47의 사용자 선정은 원본H/2025-02/Period86과 남은 Period 유지다. [영향 명세](../../temp/input-02a-engine/2026-10-14-f-s-05-column-impact-ryht4glw/impact-summary.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-s-05-column-impact-ryht4glw/status-audit-current.json): 설명 Complete·첫 생존 연도 참조/계산 결정 및 실제 파일/보존/native PENDING. 행 삭제 결과와 독립인 원본 normalized SUPPLY를 사용하며 T1은 원본11행에 남는다. 이번 XLSM/엔진 평가/Excel/native0·기존14완료·미실행5구조·전체 INPUT-02A In Progress다.
- [좁은 월/수식 관측](../../temp/input-02a-engine/2026-10-14-f-s-05-column-impact-ryht4glw/column-impact-observations.json): 제안 논리 삭제 H7:H407401셀, 원본 I7:CL407의82월/32,882셀→H7:CK407, 마지막CL 내용 blank/style 유지. 원본 G의2025-01/Period85·설비B:F/모든 행은 그대로이며 새H는2025-03/Period87, 새Q는2025-12/Period96, 새R는2026-01/Period97, 새CK는2031-12/Period168이다. 남은 Period를85·86…으로 당기거나 월 값3을2로 바꾸지 않는다. 이는 symbolic 이동안이며 실제 저장/계산 결과가 아니다.
- **첫 생존 Year 참조 결정:** 원본 I7=`IF(I8=1,H7+1,H7)`은 삭제H7을 선행으로 읽는다. 주소만 H7로 옮겨 수식 원문을 유지하면 새 H7이 자기 H7을 읽는 순환 참조가 된다. 삭제 참조를 기계적으로 적용하면 새 H7은 `IF(H8=1,#REF!+1,#REF!)`가 되고 뒤 연도에도 오류가 전파될 수 있다. 추천은 남은 앞 열G7에 연결해 새 H7=`IF(H8=1,G7+1,G7)`로 두고 다른81식도 새 좌표의 앞 생존 Year/현재 Month를 읽도록 조정하는 것이다. 이 의미/참조 복구를 계산 승인과 별도로 사용자 확인 전 정책으로 승격하지 않았다. source 저장 연도나0/공백/literal로 수식 오류를 숨기지 않는다.
- **계산 추천 후보:** 원본83 H7:CL7 중 삭제H7을 제외한82식의 새 H7:CK7만 왼쪽→오른쪽 순서로 평가/비교한다. 선행은 literal G7=2025·보존/이동한 Month H8:CK8·앞선 새 Year82연쇄다. 본문397행의 월 가동률은 literal이며 수식0, Period도literal이라 본문/Period·backup/A·다른 시트 계산을 추천하지 않는다. 같은hash의 기존 정확한 활성 이름 참조 질의(셀 수식 소비0·Filter 이름1)를 재사용했고 새 전수 조사/함수 family 분류를 반복하지 않았다. 외부 원본/모델 소비 완료를 뜻하지 않는다.
- metadata 제안: shared I7:BD7→H7:BC7(si0), BE7:CL7→BD7:CK7(si1); 삭제H7 chain1제거·다른82chain 주소 이동/시트 식별 유지가 기대 변화다. G:CL 열 폭은 같으므로 물리 column/row·dimension A1:LP407·broad 조건부서식4범위/이름(기존Filter A10:HA358)·비대상/backup/A/다른 시트 설정은 보존한다. target validation/merge/native Table 없음을 확인했으며 새 구성을 생성하지 않는다. worksheet 전체H열 삭제·다른 hidden 열 이동으로 확대하지 않는다.
- [검증/재현](../../temp/input-02a-engine/2026-10-14-f-s-05-column-impact-ryht4glw/verification.json): source/원본/대장·기존579자료 hash 불변·변경3문서 diff 검사·새 링크/Git 제외 Pass. 수식/좌표의 symbolic 제안만 기록했다.82평가를 승인하면 동일 원본/H월 삭제/Period gap/첫 Year참조 조건의 Excel 정답82셀을 확보하고 literal G7/Month·새IF만으로 독립 POI 부분 계산/저장·값/타입/오류/오차 비교·원본 보존/native를 진행한다. golden/원본 Year cache를 엔진 입력이나 출력 생성에 사용하지 않는다. 추가 계산 제외 선택이면 수식 참조/저장값 보존 조건을 확정한 뒤 해당 없음으로 기록하며 현재는 둘 다 승인 전 PENDING이다.

#### F-S-05-ROW 11행 삭제·보존·native 완료 — 2026-10-14

- domain 규칙47의 사용자 최종 결정은 원본11행 삭제·설명한 논리 범위 이동·나머지 source 수식/저장값/서식 보존·추가 계산 제외다. [실제 결과/source·final hash](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/final-summary.json)·[삭제한 원본89셀](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/execution/deleted-original-row.json)·[이동35,244셀 전후](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/execution/cell-movements.json)·[빈 tail89셀 전후](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/execution/tail-changes.json): 같은 정규화 SUPPLY에서 독립 새 XLSM을 생성·저장 후 재읽었다. 원본 B12:CL407의396행은 B11:CL406으로 값/타입/서식 그대로 이동하고 B407:CL407은 `<v>/<is>/<f>` 없는 실제 blank/type n·기존 style/format이다. T2는 새11행, Atlantic LNG4는406행으로 확인했다. worksheet 전체 행 삭제·새 값/수식/계산은 없다.
- [보존 검사](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/execution/preservation.json): 이동 셀은 원본 다음 행과 각각 완전히 일치하고 비대상464셀/header83수식/cache·전 좌표·row/column·non-cell XML·나머지69/70 part는 source 정확 일치다. backup/다른 시트/A 영역 전체·validation/merge·조건부서식4범위·hidden/names(기존 Filter 포함)·dimension A1:LP407·styles/calcChain·externalLink/cache·pivot/cache·dynamic metadata를 보존했다. source VBA/connection/native Table 없음은 해당 없음이다. 기존 target 저장 오류0·새 저장 오류0, 다른 시트의 기존#REF!는 part 보존으로 유지하며 정답/계산 오류 승인으로 사용하지 않는다. ZIP CRC·62XML/rels·저장 후 재읽기 Pass다.
- [native 결과](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/native-result.json)·[실행 로그](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/reopen/attempt-1-stdout.json): Excel16.113.4/API build1004에서 동일 hash0444 복사본을 readOnly/manual/calculateBeforeSave=false/forceDisable/doNotUpdateLinks/alerts=true로 열었다. B7:CL407의35,689값/타입(본문35,333 포함)·H7:CL7의83수식·backup/기존#REF! display/formula/비대상6표본을 확인했다. 최대 숫자 차이4E-16·허용 오차 max(1e-9,1e-9×최대 절댓값), 재계산0·링크 갱신0·매크로 실행0·무저장 닫기·기존 앱/workbook/저장 상태 복원 Pass다. 전체 다른 시트/서식/metadata 보존은 saved XML/part 검사이며 native에서 전수 읽은 것으로 확대하지 않는다.
- [재현 명령](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/README.md)·[최종 검사](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-s-05-row-native-nwhxdans/status-audit-current.json): 독립 재생성 동일 output hash, 기존 원본/source/대장·527자료 hash 불변·변경3문서 diff 검사 Pass다. 입력/구조·보존·native Pass·계산/Excel 계산 정답 해당 없음(PRESERVE), 이 지정 사례의 대기 없음·Complete다. 다른 사례 반복/Java/POI/JUnit0이며 F-S-06 제외 허용14사례 입력/보존·native Pass·미실행5구조/CLEAR0·기존 계산정책/승인 정답 대기0, 전체 INPUT-02A In Progress다.

#### F-S-05-ROW 원본11행 삭제 영향·계산 후보 — 2026-10-14 (계산 결정 전 이력)

- domain 규칙47에 사용자 선정 **원본11행 Australia Pacific LNG T1**과 계산 결정 대기를 반영했다. [영향 명세](../../temp/input-02a-engine/2026-10-14-f-s-05-row-impact-6dl9rxe_/impact-summary.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-s-05-row-impact-6dl9rxe_/status-audit-current.json): 실제 변경 XLSM/입력·보존/native는 아직 PENDING, 이번 삭제/평가/Excel 실행0이다. 선정 완료를 실제 파일 검증 완료로 처리하지 않으며 미실행6구조·기존13완료·전체 INPUT-02A In Progress를 유지한다.
- [좁은 표/참조 관측](../../temp/input-02a-engine/2026-10-14-f-s-05-row-impact-6dl9rxe_/impact-observations.json): source T-104 B7:CL407의 본문 B11:CL407은397행/35,333셀·수식0이다. 제안 삭제는 B11:CL11 제거·원본 B12:CL407→B11:CL406 이동·407의 입력 내용만 빈 상태, header/다른 영역은 보존이다. T2의 D12→D11, 마지막 Atlantic LNG4의 D407→D406으로 설비 정보·용량·84개월 가동률과 서식을 같은 행 식별로 이동한다. worksheet 전체11행 삭제나 다른 표 동시 삭제로 확대하지 않는다.
- 계산 추천은 **추가 평가0·원본 수식/cache 보존**이다.83개 수식은 H7:CL7뿐이고 `IF(H8=1,G7+1,G7)` 계열로 월이1일 때 앞 연도+1, 아니면 앞 연도를 유지한다. 이 헤더의 선행은7/8행이며 삭제11행/본문과 무관하므로 Year/Month/Period와83수식/cache·83chain은 이동/재계산 후보가 아니다. 정규화4파일에서 정확한 활성 표 이름을 갖는 저장 셀 수식 소비는0이고 이름 참조1은 숨은 `_FilterDatabase`다. 과거 자동 P0/EXCEL_GOLDEN_REQUIRED나 표본 G11→H7을 삭제 계산 승인으로 사용하지 않는다. 외부 원본/모델 실행의 소비 검증이나 동적 참조 전체 증명을 뜻하지 않는다.
- metadata 제안: target validation/merge/native Table 없음, broad 조건부서식4범위·원본 hidden 이름 `_FilterDatabase=A10:HA358`·다른 셀/row/column·dimension A1:LP407은 보존한다. 이 이름 범위는 이미 현재 데이터 끝407과 다르고 실제 worksheet autoFilter가 없다. 이번 논리 표 삭제를 이유로 기존 설정을 새로 설계/확장하지 않는다. 셀 이동 시 source 값/타입/서식·설비 식별 보존과 마지막 입력 제거, header/비대상 metadata 유지가 실제 생성 단계의 기대 검증이다. 이 제안을 업무 계산 승인으로 승격하지 않는다.
- [backup/기존 오류 표본](../../temp/input-02a-engine/2026-10-14-f-s-05-row-impact-6dl9rxe_/backup-preservation-witnesses.json): LiqProdAdjustment CM11/CM12/CM407은 활성 표가 아니라 LiqUtilAdjustment_backup을 참조하고 원본에 #REF!가 있다. backup 원본 보존(규칙52)을 유지하며 두 표의 행을 같이 삭제하거나 해당 오류를 수정/재계산하지 않는다. 실제 생성 후에는 기존 보존 오류와 새 오류를 구분한다. 사용자 계산 제외 확인 시 계산/Excel 계산 정답은 해당 없음으로 전환하고 입력/구조·보존/재읽기/native를 실행한다. 평가를 원하면 정확한 범위/선행·동일 조건 Excel 정답을 먼저 확정한다.
- [검증/재현](../../temp/input-02a-engine/2026-10-14-f-s-05-row-impact-6dl9rxe_/verification.json): source/원본/대장·기존 자료 hash 불변, 변경3문서 diff 검사·새 링크/Git 제외 Pass. 기존 workbook/명세/golden/검증 기록은 수정하지 않았다.

#### F-O-04 21슬롯2036 입력·보존·native 완료 — 2026-10-14

- 업무 기준은 domain §6.1의 사용자 지정 F-O-04다. [실제 결과/source·output hash](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/final-summary.json)·[4셀 전후 값/타입](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/execution/input-changes.json): 같은 정규화 OUTPUT에서 기존21행 B21=2036, C21:E21=source C16:E16의 숫자 저장값만 정확히 복사해 새 XLSM을 생성·저장 후 재읽었다.21의 General/기존 style을 유지해 가격 표시도 반올림하지 않았으며22행·새 수식/행/열·입력 범위 확장을 만들지 않았다. 입력·승인 슬롯 구조·보존·저장 후 재읽기는 Pass, 계산/Excel 계산 정답은 해당 없음(PRESERVE)·평가0이다.
- [보존 검사](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/execution/preservation.json): BCPrices88셀 중 비대상84셀의 값·수식/저장값·타입·서식을 좌표별 대조하고 모든 셀 좌표·non-cell XML·나머지37/38 package part가 원본 그대로임을 확인했다. 다른 입력/A 영역·USCostofSupply Helper 전체, dimension A1:S21·행/열·validation/merge·names/hidden·externalLink/캐시·styles/calcChain을 보존했다. source에 VBA/connection/pivot/cache/native Table/dynamic metadata는 없으므로 해당 없음이다. 기존 BCPrices 저장 오류0·새 저장 오류0, ZIP CRC·33XML/rels parse Pass다. worksheet bytes/개수 하나로 보존을 판정하지 않았다.
- [native 결과](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/native-result.json)·[실행 로그](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/reopen/attempt-1-stdout.json): Excel16.113.4에서 같은 hash0444 복사본을 readOnly/manual/calculateBeforeSave=false/forceDisable/doNotUpdateLinks/alerts=true로 열어 BCPrices B6:E21의64셀과 USCostofSupply P8/P816 수식·cache2셀을 확인했다. 지정4입력 numeric 값/타입 유지, native 비교 최대 차이1E-15·허용 오차 max(1e-9,1e-9×최대 절댓값), 재계산0·링크 갱신0·매크로 실행0·무저장 닫기·앱 설정/기존 workbook·저장 상태 복원 Pass다. 전체 workbook 서식/metadata 보존은 saved XML/part 검사이며 native 전수 인증으로 확대하지 않는다.
- [재현 명령](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/README.md)·[최종 검사](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-o-04-slot-native-xd1aur5l/status-audit-current.json): 독립 재생성도 동일 output hash, 원본4/source/대장·기존458자료 hash 불변·변경3문서 diff 검사 Pass다. 이 지정 사례의 남은 확인 없음·Complete이며 다른 사례 반복0·Java/POI/JUnit0이다. F-S-06 제외 허용13사례 입력/보존·선정 native Pass, 미실행6구조/CLEAR0·계산정책 대기0·승인 계산 정답 대기0, 전체 INPUT-02A In Progress다. 일반 신규 가격의 빈 값/기본값 및 다른 OUTPUT 변경의 계산은 이번 결과로 정의하지 않는다.

#### F-O-04 21빈 슬롯/22신규 행 설명·필드 관측 — 2026-10-14 (선정 전 이력)

- [좁은 원본 관측](../../temp/input-02a-engine/2026-10-14-f-o-04-boundary-3b_smhlb/boundary-observations.json): B20=2035, B21:E21은 실재하는 빈 서식 셀, B22:E22와22행 row node는 없다. 현재 worksheet dimension은 A1:S21, 정책상 입력 선언은 B8:E21이며 이 시트의 native Table/validation/merge/autoFilter·BCPrices를 언급하는 names 및 시트 내부 수식은 없다. 다른 시트의 소비 참조/파일 밖 모델은 이번 관측에서 조사하지 않았으므로 내부 수식0을 전체 영향 없음으로 확대하지 않는다.
- 실제 필드: B열 Year, C열 Henry Hub(HH), D열 East Asia Spot, E열 TTF이며 가격 단위는 모두 USD/mmbtu다.21 선택 시 B21/C21/D21/E21,22 선택 시 B22/C22/D22/E22를 지정한다. 마지막2035 다음의2036은 후보 설명이며 사용자 승인값이 아니다.2032~2035의 C:E는 원본 빈 값으로 보존하며 이를 신규 가격 빈 값 허용 근거로 사용하지 않는다.
- 두 후보의 차이:21은 선언된 범위 안의 빈 슬롯에 새 논리 연도 기록을 처음 채워 차원을 유지하는 후보다.22는 새로운4셀/row node/서식을 만들고 dimension을 A1:S22·논리 입력 범위를 B8:E22로 확장하는 후보이며21은 그대로 비어 있어 데이터 사이에 빈 행이 남는다. 둘 다 worksheet 전체 행 삽입/기존 데이터 이동이 아니다.21의 가격 셀 표시 형식은 General,20은0.00이므로 활성화/신규 행의 서식 기대 변경은 별도로 명시한다. 기존 native Table이 없어 table.xml 확장을 임의 생성하지 않는다.
- 필요한 답변은 사용할 행, 연도, 세 가격값이다. 직접 숫자 또는 특정 기존 행의 가격값 복사로 지정할 수 있다. 신규 가격을 비울 의도가 있으면 그 셀의 빈 값 허용/기본값도 확인한다.21사용을 추천했지만 아직 선택/값/서식·참조 기대 변경·필요 계산은 확정하지 않았으며 실행은 PENDING이다. [설명 단계 검증](../../temp/input-02a-engine/2026-10-14-f-o-04-boundary-3b_smhlb/verification.json)에 기존444자료/source hash 불변·새 링크/Git 제외·문서 검사와 새 XLSM/계산/native0을 기록한다. 기존12완료·계산정책 대기0·미실행7구조/CLEAR0·전체 INPUT-02A In Progress 유지다.

#### F-D-03 Excel·POI·native 완료 후속 — 2026-10-14

- 업무 정본은 domain 규칙50의 F-D-03 후속, 조건은 기존 O3=2036/국가90 N literal 복사 prepared다. [최종 결과](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/final-summary.json)에 source/prepared/Excel golden/POI final 경로·SHA-256과 입력·구조/보존·계산·Excel 정답·native 모두 Pass를 연결했다. 이 지정 사례의 남은 확인은 없다.
- Excel16.113.4에서 같은 hash0444 prepared 복사본을 수동 계산·calculateBeforeSave=false·read only·doNotUpdateLinks·forceDisable·alerts=true로 열었다. O5/O10/O15→O4, O25/O27→O24, O35/O43→O34와 나머지 소계→O109의 승인16셀만 각각 계산하고 별도 golden 저장·무저장 닫기·기존 앱/workbook/저장 상태를 복원했다. [실제16주소/실행 로그](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/golden/attempt-1-stdout.json)·[golden 적합성](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/golden-audit.json): 25시트2,902,427 기존셀의 좌표·값/타입·수식·해석된 서식은 승인 O16 cache 외 동일하다. Excel writer의 part/style ID 재번호화는 의미를 대조했으며 그 저장본은 비교용이다.
- [source-only unit](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/poi/unit-provenance.json)은 O16 SUM과 O열 국가90 literal만 사용한다. 기존 E:N 합계 cache·golden·외부 참조 입력0. Java17.0.14/POI5.4.0 기본 평가기로 요청16/평가16/실패0/skip0, 새 저장 파일 재읽기 후 [Excel16셀 비교](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/evaluated/comparison.json) Pass(값·numeric 타입·오류 없음·수식, max(1e-9, 1e-9×최대 절댓값) 허용 오차, 최대 차이4E-14). 별도 [replay](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/replay/result.json)도16평가/실패0/skip0·동일 결과 hash이고 golden을 읽지 않았다. 다른 사례 반복0이다.
- [최종 저장/보존](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/evaluated/result.json): prepared에서 O16 `<v>`만 추가했다. 기존1,406셀·E:N1,080셀의 값/수식/저장값/타입/서식, E:N130합계, 비대상 셀/non-cell XML·다른67/68part·validation/hidden/VBA/externalLink/cache/connection/pivot/cache/names/merge 등 실제 존재 항목을 보존했다. source 대비 신규107셀/dimension/spans/16chain 추가는 이전 승인 구조 단계이며 이번 계산에서 확대하지 않았다. ZIP/57XML·저장 후 재읽기 Pass다.
- [최종 native](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/native-result.json): 동일 hash0444 최종 복사본을 읽기 전용·무저장 재개방해 새 합계16값/수식·새 입력91개·E3:N109의1,070값/130합계 수식을 확인했다. native 계산0·링크 갱신0·매크로 실행0·앱/workbook/저장 상태 복원 Pass다. 기존 E:N 전체1,080셀의 서식/metadata 보존은 saved XML/part 검사, native 값 읽기는 E3:N109로 구분한다.
- [최종 hash/문서 검증](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/verification.json)·[재현 명령](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/README.md)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-d-03-poi-native-lpnr4yc4/status-audit-current.json): 기존350자료 불변, F-S-06 제외 생성된 허용 입력/보존12·native12, 승인 계산 정답 대기0·추가 계산 정책3대기·미실행7구조/CLEAR0다. 이 사례의 모든 단계는 첫 시도 Pass며 INPUT-02A 전체는 In Progress다. 아래 앞선 단계의 F-D-03 계산/native PENDING과 native11/대기1은 이번 완료 전 이력이다.

#### F-S-04-COLUMN Excel·POI·native 완료 후속 — 2026-10-14

- 업무 기준은 domain 규칙51, 변경 조건은 앞서 생성한2036/0 prepared 파일이다. [최종 결과](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/final-summary.json)에 정규화 source/prepared/Excel golden/POI final의 경로·SHA-256과 사례별 판정을 연결했다. 입력·구조/보존·계산·Excel 정답·native 모두 Pass이며 이 사례의 남은 확인은 없다. 전체 INPUT-02A 완료를 뜻하지 않는다.
- Excel16.113.4의 범위 계산으로 **BCRegasification!S8:S379만** 평가하고 새 macro-enabled 파일에 저장했다. 수동 계산·저장 전 계산 금지·읽기 전용 입력·링크 갱신 금지·매크로 forceDisable을 실제 실행 로그에서 확인했다. [golden 적합성](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/golden-audit.json): 23시트 기존2,775,317셀의 좌표·값/타입/수식·해석된 서식을 비교했고 승인 S372 cache 외 값/수식 변화는 없다. Excel의 part 재배치와 style ID 재번호화는 의미가 같은 서식인지 확인했다. 이 native 저장본은 비교용이며 최종 결과의 보존 기준/계산 입력이 아니다.
- Java17.0.14/POI5.4.0 기본 평가기에 prepared의372 MAX 수식과 G:R의4,464 literal 입력만 전달했다. golden 값이나 원본 수식 cache를 평가 입력으로 사용하지 않았다. 실제 요청372/평가372/실패0/skip0, 저장 후 [372셀 Excel 비교](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/comparison.json) Pass(값·numeric 타입·오류 없음·수식, 허용 오차 max(1e-9, 1e-9×최대 절댓값), 최대 차이1E-15). 별도 [재현 실행](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/replay/result.json)도372평가/실패0/skip0·동일 output hash이며 기존 다른 사례를 반복하지 않았다.
- 최종 XLSM은 prepared에서 S372의 저장값만 추가했다. [POI 보존 검사](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/poi-result.json): 나머지69/70 part·모든 비대상 셀/수식/서식/validation·non-cell XML, Regas 전체54,056셀/43,464수식·cache, hidden·names·외부 링크/캐시·pivot/캐시·dynamic metadata 등 실제 존재 항목을 보존했다. 정규화 source 대비 승인 R→S 이동/chain 변화는 이전 구조 실행 근거를 따르며 이번 cache 저장에서 확대하지 않았다. ZIP/62XML·저장 후 재읽기 Pass다.
- [native 재개방](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/native-result.json): 동일 hash의0444 읽기 전용 최종 복사본에서 S372 값/수식, R373 입력, S7 헤더·Regas 대표8셀을 확인하고 무저장 닫았다. native 계산0·링크 갱신0·매크로 실행0, 기존 앱 계산/보안 설정과 열린 workbook/저장 상태 복원 Pass다. Regas 전체 보존은 저장 파일 검사이며 native 전수 읽기는 수행하지 않았다.
- [최종 hash/문서 검사](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/verification.json)·[재현 명령/로그](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/README.md)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-s-04-column-poi-native-8rjp02hn/status-audit-current.json): 기존260자료 불변, F-S-06 제외 허용 입력/보존12·native11/대기1=F-D-03, 승인 계산 정답1대기=F-D-03·미실행7구조/CLEAR0·추가 계산 정책3대기다. 아래 입력 단계와 이전 정책의 PENDING 문구는 이번 완료 전 이력이다.

#### F-S-04-COLUMN 2036/0 실행·Regas 보존 후속 — 2026-10-14 (계산 전 이력)

- 사용자 fixture는2036년1열·신규 용량 모두 숫자0이다. 현재 master372행과 Regas 월/연간/국가 합계 경계를 [좁은 관측](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/bounded-regas-observations.json)으로 확인한 뒤, 사용자가 연도 추가에서는 Regas를 갱신하지 않고 행 추가/삭제 때만 연결 갱신한다고 결정했다. 이 최종 업무 정책은 domain 규칙51이며 [원문/답변](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/regas-preserve-answer.json)·[현재 정책/명세](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/policy-result.json)에 연결했다. 연결2036/중간 연도 추가 선택은 더 이상 대기가 아니다.
- [실제 파일 결과](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/execution/result.json): R7=2036·R8:R379=0의373 numeric 입력과 S7 최대 용량 헤더·S8:S379372 MAX 수식을 새 독립 XLSM에 생성/저장·재읽었다. 새 입력 서식은 Q, 최대 용량 서식/shared formula 그룹은 원본 R에서 복제했고 MAX는 G:R을 포함한다. S 수식 cache는 미평가로 두며 원본 MAX 저장값이나 golden을 새 결과로 복사하지 않았다.
- 보존: master 기존31,617 비승인 변경 셀과 G:Q·A Helper, Regas 전체54,056셀/43,464수식·캐시·타입·서식 및 나머지68/70 package part를 보존했다. 필요한363행 spans와 shared formula ref R→S·calcChain372항목 이동만 기대 변경이다. dimension/열 폭/전체 적용 conditionalFormatting·filter/hidden names·hidden sheets·externalLink/캐시·pivot/캐시·dynamic metadata 등 실제 존재 설정은 원본 그대로다. 자세한 전후746셀은 `execution/cell-changes.json`, [보존 검사](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/execution/preservation.json)와 `workbook-config-preservation.json`을 따른다. ZIP·62XML/관계 parse·독립 재현 동일 hash Pass, 기존207자료 불변이다.
- 계산/다음: **master S8:S379372셀만** 승인 계산 대상이다. Regas G lookup의 원본 E:R/14 인덱스·월 INDEX·연간/국가 SUMIFS도 수정/재계산하지 않는다. 현재 저장값 보존은 새 계산 정답이 아니다. [Excel 제출 조건](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/execution/excel-submission.json)의 동일 파일/2036/0에서 수동 계산으로 S372만 평가/저장한 별도 golden과 버전/설정·값/타입/오류를 확보한 뒤 POI372 계산/저장/재읽기·비교/원본 보존, 최종 native를 진행한다. 이번 엔진/Excel/native 실행은0이다.
- [현재 사례 상태](../../temp/input-02a-engine/2026-10-14-f-s-04-column-impact-03mtpekp/status-audit-current.json): F-S-06 제외 허용 입력/보존 Pass12·native Pass10/대기2=F-D-03/F-S-04-COLUMN, 미실행7구조/CLEAR0·추가 계산 정책3대기·승인 계산 정답2대기이며 전체 INPUT-02A는 In Progress다. 아래 연도/개수·Regas 연결 범위 대기는 최종 지정값/정책 전 이력이다.

#### F-S-04-COLUMN 최대 용량 이동 확인 후속 — 2026-10-14

- 사용자가 최대 용량 열 이동·전체 연도 포함을 선택했다. domain 규칙51에 반영하고 [답변/정책 결과](../../temp/input-02a-engine/2026-10-14-f-s-04-column-max-policy-j_4mf7x6/policy-result.json)·[현재 명세](../../temp/input-02a-engine/2026-10-14-f-s-04-column-max-policy-j_4mf7x6/cases-current.json)에 연결했다. [좌표/수식 예시](../../temp/input-02a-engine/2026-10-14-f-s-04-column-max-policy-j_4mf7x6/policy-mapping-examples.json)는1열 추가 R→S/`MAX(G8:R8)`, 2열 추가 R→T/`MAX(G8:S8)`이며 실제 연도/개수는 아직 선정하지 않았다.
- R 복사/이동 해석 질문은 해소했으며 연도 허용·신규 수치 기본값0은 유지한다. 기존 source/질문/관측 기록은 수정하지 않는다. 실제 연도/개수·연결 Regas 기간/lookup/집계·참조/metadata 기대값과 동일 구조 Excel 정답은 미확보로 입력/보존·계산·Excel/native PENDING이다. [현재 상태](../../temp/input-02a-engine/2026-10-14-f-s-04-column-max-policy-j_4mf7x6/status-audit-current.json)의 미실행8구조/CLEAR0·기존 결과를 유지하며 새 파일/계산 실행은 없다.

#### F-S-04-COLUMN 연도 추가 후속 — 2026-10-13

- 확정 정책은 domain 규칙51이며 [정책 결과](../../temp/input-02a-engine/2026-10-13-f-s-04-column-policy-hyd6cgd7/policy-result.json)·[현재 명세](../../temp/input-02a-engine/2026-10-13-f-s-04-column-policy-hyd6cgd7/cases-current.json)에 연결했다. 연도 추가 허용과 신규 수치 빈 값 금지/기본값 숫자0, R열 복사·계산 의도를 기록했다. 실제 파일은 만들지 않았으며 원본 빈 값이나 Regas 연결 수식을0으로 치환하지 않았다.
- [현재 source 선정24셀](../../temp/input-02a-engine/2026-10-13-f-s-04-column-policy-hyd6cgd7/selected-column-observations.json)에서 master G7:Q7은2025~2035, R7은 Maximum capacity, R8/R379는 같은 행 G:Q 최대값임을 확인했다. Regas G11은 master E:R의14번째 열을 조회하고 AF11/DK11은 E:Q·연도 헤더의 고정 끝을 사용한다. R의 단순 옆 복사는 첫 연도를 빠뜨릴 수 있어 [당시 확인 질문](../../temp/input-02a-engine/2026-10-13-f-s-04-column-policy-hyd6cgd7/question-current.json)에 예시/선택 차이를 기록했고 위2026-10-14 후속 답변으로 해소했다. 질문 예시는 실제 추가2036/1열 실행 승인으로 사용하지 않는다.
- 실제 추가 연도/개수·연결 기간/수식/lookup·집계/metadata와 같은 구조 Excel 정답은 미정으로 입력/보존·계산·Excel/native PENDING이다. [현재 상태](../../temp/input-02a-engine/2026-10-13-f-s-04-column-policy-hyd6cgd7/status-audit-current.json)의 미실행8구조/CLEAR0·기존 결과를 유지한다. 연도 허용/기본값0을 재질문하지 않는다.

#### F-S-05-COLUMN Period 유지 후속 — 2026-10-13

- domain 규칙47의 사용자 결정을 [정책 결과](../../temp/input-02a-engine/2026-10-13-f-s-05-column-policy-de1i6p89/policy-result.json)와 [현재 열 삭제 명세](../../temp/input-02a-engine/2026-10-13-f-s-05-column-policy-de1i6p89/cases-current.json)에 연결했다. 남은 Period 번호 유지/재번호화 금지는 확정이다. 실제 삭제할 월 열은 미선정이며 `H7:CL7` 헤더 수식의 이동 후 참조 처리·기간 값 유지, downstream·서식/validation·집계/names/Table/metadata 기대 변경은 별도로 확정한다.
- 새 XLSM/계산/Excel/native는 실행하지 않았다. 입력/보존·계산·Excel 정답/native는 PENDING이며 계산이 필요한 경우 승인 범위·같은 삭제 조건 Excel 정답을 확보한다. [현재 상태](../../temp/input-02a-engine/2026-10-13-f-s-05-column-policy-de1i6p89/status-audit-current.json)의 F-S-06 제외 미실행8구조/CLEAR0와 기존 결과는 유지한다. 아래 Period 선택 대기는 이번 답변 전 이력이다.

#### F-S-05-ROW 삭제 시작 행 후속 — 2026-10-13

- domain 규칙47의 사용자 결정을 [정책 결과](../../temp/input-02a-engine/2026-10-13-f-s-05-row-policy-ikld169k/policy-result.json)와 [현재 행 삭제 명세](../../temp/input-02a-engine/2026-10-13-f-s-05-row-policy-ikld169k/cases-current.json)에 연결했다. 헤더 보호·11행부터 허용은 확정이며 실제 삭제 행/범위·삭제 후 이동/행 식별·수식/참조/서식/validation·집계/names/Table/metadata 기대값은 미정이다. 계산이 필요한 경우 승인 범위와 동일 삭제 조건 Excel 정답을 확보한 뒤 평가한다.
- 실제 파일은 미생성이므로 입력/보존·계산·Excel 정답/native는 PENDING이다. 11행을 자동 삭제하거나 내용 CLEAR로 치환하지 않았다. [기록 당시 상태](../../temp/input-02a-engine/2026-10-13-f-s-05-row-policy-ikld169k/status-audit-current.json)의 F-S-06 제외 미실행8구조/CLEAR0·기존 실행 결과는 유지한다. 당시 별도 대기였던 열 삭제 후 Period 정책은 위 F-S-05-COLUMN 후속으로 확정했으며 F-S-04 연결 삭제 답변은 아래 독립 사례에 반영했다.

#### F-S-04 연결 삭제 답변 후속 — 2026-10-13

- 사용자가 “같은 설비의 연결 행도 자동 삭제”로 답했다. domain 규칙51과 [연결 삭제 정책 결과](../../temp/input-02a-engine/2026-10-13-f-s-05-row-policy-ikld169k/linked-deletion-policy-result.json)에 반영했으며 Regas 사용자 직접 삭제 금지는 유지한다. 과거 질문/정책 기록은 수정하지 않고 이번 답변으로 연결 삭제 여부 대기만 해소한다.
- 실제 삭제할 설비/행·같은 설비의 행 대응·남은 조정값/수식·참조 이동/집계·서식/validation/names/Table/metadata 기대값이 미정이므로 파일/계산/Excel/native는 PENDING이다. 새 `cases-current.json`은 F-S-05-ROW와 F-S-04-DELETE를 독립 명세로 유지하며 실제 삭제는 실행하지 않았다.

#### F-S-04 master 행 추가/삭제·Regas 자동 추가/직접 구조 금지 후속 — 2026-10-13

- 정책 정본은 domain 규칙12/51이다. BCRegasification 마지막 행 추가/행 삭제 허용, 추가 때 RegasAdjustment 같은 설비 행 자동 추가, Regas 사용자 직접 추가/삭제 금지·기존 허용 값 편집만이라는 사용자 결정을 반영했다. Regas 값 편집 범위 H11:AE382와 연결 수식 보호·연간/국가별 합계 연쇄 계산은 유지한다. master 연도 추가는 당시 행 결정에 포함되지 않았으나 위 F-S-04-COLUMN 후속 답변으로 허용을 확정했다.
- 실제 연결 근거: source SHA256이 고정 SUPPLY와 같은지 확인하고 대표 셀만 재읽었다. Regas B11:F11은 BCRegasification B8:F8, B382/E382는 B379/E379로 행 위치를 참조한다. G11의 설비명 first-match lookup도 원본 관측으로 보관하며 새 유일키/삭제 정책으로 승격하지 않는다. [선정 참조 근거](../../temp/input-02a-engine/2026-10-13-f-s-04-row-policy-xd1e8odx/selected-link-observations.json)에 원문/타입/캐시·source pin을 기록했다.
- 상태/이력: 추가 소유/자동 연결·Regas 직접 구조 금지는 확정이다. master 삭제 시 Regas 자동 삭제는 초기 [정책 결과](../../temp/input-02a-engine/2026-10-13-f-s-04-row-policy-xd1e8odx/policy-result.json)·[명세](../../temp/input-02a-engine/2026-10-13-f-s-04-row-policy-xd1e8odx/cases-current.json)·[상태](../../temp/input-02a-engine/2026-10-13-f-s-04-row-policy-xd1e8odx/status-audit-current.json)에서 대기였으나 위 「F-S-04 연결 삭제 답변 후속」으로 확정했다. 신규 설비/필수값·삭제 행·자동 수식/기본값·블록/참조/서식/validation/집계/metadata 변화와 동일 조건 Excel 정답은 미확보로 실제 파일/계산/native는 PENDING이다. 새 XLSM/실제 추가·삭제·계산·Excel/native/제품/API/DDL 실행은 없다.

#### F-T-03 계약량 빈 값 금지·기본값0 후속 — 2026-10-13

- 업무 정본은 domain 규칙59/H3의 F-T-03 사용자 확인이다. U5 계약량 빈 값 금지·기본값 숫자0으로 정책 대기를 해소했고 실제 빈 셀 CLEAR 양성 명세는 거부 기대값으로 정리한다. 다른 행의 기본값/빈 값, 계약 비활성화/삭제·계산/오류 처리나 자동0 보정을 정의한 것은 아니다.
- 상태: 정책 반영 Complete, 허용 입력 파일 검증 대상 제외. F-T-03 변경 XLSM은 미생성이므로 실제 업무 입력 Fail/거부 검사 Pass를 주장하지 않는다. 이 CLEAR의 계산/Excel 계산 정답/native는 해당 없음이고 제품의 빈 값 거부/기본값 적용 검증은 PENDING(이번 파일 범위 밖)이다. SET0 파일/계산도 실행하지 않았으며 기존 F-T-01/02/04 입력·보존/native와 계산 정책/대기 상태는 유지한다.
- 근거: [새 정책 결과](../../temp/input-02a-engine/2026-10-13-f-t-03-policy-vl76brg1/policy-result.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-13-f-t-03-policy-vl76brg1/status-audit-current.json)에 사용자 원문/정본 위치·상태를 연결한다. 기존 CLEAR 명세/후속/파일/로그는 수정하지 않고 이 정책으로 현재 상태만 대체한다. F-S-06 제외 미실행 허용 후보8→7(구조7·CLEAR0), 기존 계산정책3 및 F-D-03 계산/Excel/native 대기는 별도로 유지한다.

#### 승인 자원 제한 대표 경로 재측정 완료 — 2026-10-15

- 운영 기준은 사용자 승인 domain 규칙64다. [자원 강제/실측/한계](mi-model-input-calculation-engine-spike.md#input-02a-승인2vcpu2gib-worker-대표-경로-재측정--2026-10-15)·[최종 결과](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/final-summary.json)·[재현](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/README.md): Docker 설정/내부 cgroup의2vCPU·2GiB·swap0, worker1/동시1에서 F-D-03/F-S-04-DELETE/F-T-04/F-O-04 각3회·12인정 측정을 완료했다. 중앙값은5.747/14.296/3.187/0.280초, worker 전체 커널 메모리 peak666.5MiB·OOM0이다. 입력/부분 계산·원본 보존 경계를 유지했다.
- 12결과 전체 승인 hash/ZIP part·metadata/CRC/저장 재읽기 동일, source-only unit6동일·실제6 JVM18,840평가/실패0/skip0, 기존2,610자료 hash 불변을 [감사](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/verification.json)에 연결한다. 처음 TRADE 준비의 manifest mount 누락1회는 도구/환경 오류로 별도 기록하고 앞선2Pass는 유지·미완료만 재개했다. 새 평문 작업사본/진단 내용·핸들·컨테이너 잔여0이다.
- 이번 환경은 Linux Docker/새 JDK patch/private tmpfs이며 API/대기열·DB/storage/fsync와 동일4파일 최종저장 전체 시간은 제외한다. 따라서 승인30초 목표/전체120초 상한의 제품 end-to-end Pass나 production GO를 뜻하지 않는다. 제한환경 재측정 Complete·선정19파일 완료·실제 DRM 사용자 제외·전체 INPUT-02A In Progress를 구분한다. 남은 다음 단계는 이 근거와 운영 한계를 붙인 최종 판정/INPUT-03·04·07 제약 기록이며 제품 전체 저장 응답/실패 경계는 후속 계약·구현에서 검증한다. 아래 미승인 제안/재측정 대기는 이번 승인/실행 전 이력이다.

#### INPUT-02A 최신 잔여·운영 추천 정리 — 2026-10-15

이 절은 운영/최종 승인 전 추천 이력이다. 현재 판정은 문서 상단 「INPUT-02A 최종 조건부 완료」를 따른다.

- F-S-06 제외 선정19파일의 잔여 개별 입력/구조·계산/Excel/native 시험은0이며, 승인4역할7경로21회 자원·동시/중단·로컬 평문 수명/프로세스 복구 검증까지 완료했다. 실제 DRM 재검증은 사용자 제외다. [최신 대조/미승인 추천](../../temp/input-02a-engine/2026-10-15-input-02a-operating-recommendation-6bwaaahc/remaining-and-recommendation.json)·[근거/한계](mi-model-input-calculation-engine-spike.md#input-02a-잔여와-운영-추천--2026-10-15-미승인-제안)에 현재 남은 운영 예산·저장 대기/완료 경험·격리 실행/동기·비동기·최종 판정을 연결했다. 과거 JSON/실행 로그의 이전 PENDING 항목을 수정하거나 현재 대기로 다시 열지 않는다.
- 초기 추천은 별도 프로세스/worker 비동기, worker당2vCPU/2GiB·동시1/역할 파일 순차, 정상30초 목표·접수부터 저장 확정까지120초 상한 후보다. **사용자 미승인 제안**이며 domain/운영 정책이나 관측SLA가 아니다. 단일 파일 최대12.027초와 요청 RSS570.8MiB·동시 혼합987.0MiB는 근거이고, 같은4파일 batch·DB/storage 포함시간·제한된 운영환경/최대동시성은 미실측이다. 목표 자원에서 대표 경로 확인을 권장하되 환경 미확보 시 한계를 조건으로 기록/후속 검증으로 전달한다.
- 다음 순서: 사용자 운영 예산/대기 경험·비동기 방향/범위 제한 수용 → 필요한 좁은 제한환경 확인 또는 한계 수용 기록 → domain Q7·§6A의 최종 판정/INPUT-03·04·07 제약 확정이다. 추천 판정은 검증된 경계의CONDITIONAL 후보이며 이번 정리로 티켓 완료/production GO·제품 구현을 선언하지 않는다. 기존19사례/Excel 정답 재검증·DRM 환경·전체 조사 완료를 새 선행조건으로 요구하지 않는다.

#### 로컬 평문 수명·삭제 검증 완료 — 2026-10-15

- domain 규칙63의 로컬 작업사본 수명 검증은 [실측/한계](mi-model-input-calculation-engine-spike.md#input-02a-로컬-평문-작업사본-수명삭제--2026-10-15)·[최종 결과](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/final-summary.json)·[재현](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/README.md)로 Complete다. 실제 OUTPUT 사본의 open-unlinked 핸들 대조1·정상4역할4건·준비 후 오류/timeout/부분 ZIP kill3건·controller 유실 후 새 프로세스 복구2건의10시험을 통과했다. 별도 사전 대조1도 통과했다. 종료 후 최장4.345초≤60초, 복구 기동부터 최장0.665초≤300초이며 최종 파일/핸들/소유 그룹0·실패 분석용 평문 보관0이다.
- 승인 F-D-03/F-S-01/F-T-04/F-O-04 입력·계산/보존 경계를 유지했다. 정상4+복구 직전 완성1의5결과는 기존 승인 hash/ZIP·저장 XML 재읽기 동일을 확인한 뒤 삭제했고, source-only unit6도 동일하다. 정상2 JVM9,628평가/실패0/skip0과 저장 중 KILL한 세 번째 JVM을 구분한다. 기존 자료2,526개·domain/Delivery Plan hash 불변과 코드/로그/새 runtime 전량 정리는 [검증](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/verification.json)에 연결한다. 계산/golden/native 범위를 확대하지 않았다.
- 실제 controller SIGKILL 후 핸들8/1이 남는 것을 확인하고2.322/2.311초의 중단 뒤 새 recovery process가 정리했다. 같은 사용자 중 `lsof` 열람 가능 범위와0700 경로의 로컬 시험이며 OS reboot/정전·제품 worker/복구 scheduler 구현·저장장치 지우기 증거는 아니다. 실제 DRM 재검증은 사용자 제외이며 새로운 DRM 표본/환경을 요구하지 않는다. 전체 INPUT-02A In Progress를 유지하고 다음은 운영 자원/허용 저장 대기·완료 경험 및 실행 경계/최종 판정이다.

#### 평문 수명 확정·기존 DRM 재검증 제외 — 2026-10-15

- 사용자 확정 정책은 [domain §6 규칙63](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)이다. 평문 임시 저장 허용, 종료 후1분·복구 후5분 이내 삭제, 실패 분석용 평문 미보관, 파일/열린 핸들 잔여0을 적용한다. 앞선 미채택 제안 기록은 이번 확인으로 대체한다. 기존 원본/정식 보관 파일·검증 자료는 삭제하지 않는다.
- **사용자 후속 결정:** 로컬 DRM 복호화 시험은 불가하고 암복호화는 이미 적용된 기능이므로 실제 DRM 재검증을 이번 범위에서 제외한다. 기존 공통 기능을 재사용하며 상태는 **해당 없음(사용자 제외)**이다. 보호 표본·시험 설정·원격 재검증 증거 확보를 INPUT-02A의 선행조건으로 요구하지 않는다. 기존 구현의 존재와 실제 시험 Pass를 구분한다. 이 결정 당시 PENDING이던 로컬 worker 작업사본 수명/삭제는 위 후속 probe로 완료했다. 아래 준비 관측은 제외 결정 전의 조사 이력이다.
- [준비 판정/후속 제외](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/final-summary.json)·[설정 존재 여부](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/configuration-redacted.json)·[재현/한계](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/README.md): 기존 upload/status/encrypt/decrypt/download 경로와 XLSM 대상 정책은 확인했다. 현재 프로세스에서는 dedicated 설정이 참조하는 필수8환경변수가 모두 비어 있고 repo 기본 profile은 local이다. 확인한 로컬 override 파일도 없었다. 배포 환경의 서비스 장애/설정 누락을 판정한 것은 아니며 후속 사용자 결정으로 실제 DRM 시험은 제외했다.
- [자료](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/samples.json): 보존 source snapshot4·정규화4·동일 source basecase 복사본4의 manifest hash/ZIP CRC·workbook XML 재읽기 Pass다. 다른 버전 basecase3은 제외했고 manifest의 원래 Downloads 경로4는 현재 없다. 읽을 수 있는 ZIP/XML·파일명은 실제 DRM 상태/복호화 성공의 증거가 아니다. 환경 확보 후 기존 encrypt 경로로 현재 신뢰 자료의 보호 시험사본을 만드는 경로가 있어 새 원본 제출을 일괄 선행조건으로 추가하지 않는다.
- 기존 client에는 삭제/정리/health 호출이 없으며 서비스 쪽 자동 삭제/관측 지원 여부는 이번 조사로 확인하지 못했다. 기존 DRM 정본의 “당일만 사용 가능”·서비스 측 산출물 정리 책임은 삭제 시간과 핸들0 증거를 뜻하지 않는다. 이 한계를 기록하되 사용자 제외 결정에 따라 서비스 쪽 재검증을 완료 조건으로 요구하지 않는다. 다음 검증 대상은 현재 자료를 사용하는 로컬 worker 평문 작업사본의 수명/삭제다.
- 빈 주소를 localhost로 처리한 최초 TCP probe는 [제외 기록](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/tcp-attempt-invalid-localhost.json)에 보관하고 유효 주소 guard를 추가했다. 최종 [접속 상태](../../temp/input-02a-engine/2026-10-15-drm-readiness-0nb_foqp/tcp-connect.json)는 유효 DRM 주소 없음/접속 시도0이며 서비스 연결 거부로 해석하지 않는다. 실제 파일 전송/암복호화·삭제 수명/복구 시험·새 XLSM/POI/native는 실행하지 않았다. 준비 확인 Complete·실제 DRM 해당 없음(사용자 제외)·로컬 수명 PENDING·선정19파일 완료·전체 INPUT-02A In Progress를 구분한다.

#### 동시 요청 격리·timeout/강제 종료 후 정리 완료 — 2026-10-15

- [시나리오/정량·한계](mi-model-input-calculation-engine-spike.md#input-02a-동시-요청-격리timeout강제-종료-후-정리--2026-10-15)·[최종 결과](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/final-summary.json)·[재현](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/README.md): 승인4역할 동시4요청·같은 F-S-01 동시2요청과 준비/unit 후 deadline·무응답 JVM TERM→KILL·부모만 KILL 후 고아 자식 회수·부분 ZIP 저장 중 KILL의6시험을 통과했다. 정상 동반 요청을 포함한10성공 파일은 승인 hash/ZIP·재읽기 동일, 실패 주입4건의 게시0·기존 성공 불변·source-only unit11동일이다.
- 인정 시험14요청·중단된 probe 시도4요청을 합친 실제18요청의 소유 프로세스·private scratch 잔여0, 중간 XLSM/unit 제거를 확인했다. 완료35,164평가/실패0/skip0과 주입 중단2JVM을 구분한다. 원본/정규화4역할·대장·이전2,122자료·domain/Delivery Plan 불변과 도구 버전/재시도·문서 검사는 [검증](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/verification.json)에 연결한다. 새 golden/native·링크/매크로 실행은 없다.
- 이번 Complete는 외부 probe 제어기가 생존하며 소유 그룹을 종료·회수하고 scratch를 제거한 로컬 경계다. gate/12초 deadline/0.5초 유예·TERM 무시는 시험 조건이며 운영 정책·동시성 한계로 채택하지 않는다. 제품 Draft/권한/transaction·controller/host 유실 복구·DRM 보안 삭제를 구현/검증한 것은 아니다. 전체 INPUT-02A는 In Progress로 유지하고 다음은 DRM 근거·운영 자원/허용 대기시간과 실행 방식/최종 판정이다.

#### 승인4역할7경로 읽기→저장 시간·메모리 측정 완료 — 2026-10-15

- [실측 표/방법/한계](mi-model-input-calculation-engine-spike.md#input-02a-승인4역할7경로-읽기저장-자원-실측--2026-10-15)·[최종 결과](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/final-summary.json)·[재현](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/README.md): F-D-03/F-D-05-DELETE, F-S-01/F-S-04-DELETE, F-T-04, F-O-01/F-O-04를 독립·순차 각3회 실행했다. 중앙값은 각각4.659/5.141/9.208/11.858/2.461/0.234/0.223초, 부모·실행 중 자식의 RSS 최대 관측값은570.8/327.3/524.1/516.6/267.5/79.2/79.9MiB다. 이는 source 읽기부터 승인 변경·허용 계산/unit 추출·ZIP 저장과 프로세스 종료까지이며 검증 준비/컴파일·사후 전체 감사 비용은 제외한다. 10ms 목표 표본의 관측 peak와 별도 kernel 프로세스 최대값을 구분한다.
- 원본에서 새로 준비한9계산 unit은 기존 source-only unit과 동일하고 실제47,676평가/실패0/skip0이다. 추가 계산 제외4경로의 계산0, 21결과는 기존 승인 결과와 hash/ZIP part 동일·CRC/재읽기 Pass다. source/표본/golden/기존 검증1,706자료·domain/Delivery Plan은 불변이다. 기존 동일 hash의 Excel 정답/보존/native 근거는 유지하며 새 golden/native·링크/매크로 실행은 없다. 선정 허용19사례의 완료 상태도 유지한다.
- 이번 자원 실측은 완료했으며 성능 합격 한계/SLA·production 실행 방식은 확정하지 않았다. OS 캐시는 사전 hash 읽기로 데워졌고 새 Python/JVM 조건이다. 4역할 개별 경로 순차 실측이며 실제 API에서4파일을 함께 처리한 부하나 동시 요청 시험은 아니다. 후속 로컬 동시 요청 격리·timeout/kill/정리는 위 완료 결과를 따르고, DRM·허용 대기시간/운영 자원을 확인해 실행 방식/최종 판정을 정한다. 상세 검사/문서 변경은 [검증](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/verification.json)에 연결한다.

#### F-S-06 제외 INPUT-02A 전체 완료까지의 잔여 기준 — 2026-10-15

이 절은 후속 실행·최종 승인 전 잔여 기준 대조 이력이다. 현재 판정은 문서 상단 「INPUT-02A 최종 조건부 완료」를 따른다.

- [현재 근거 대조](../../temp/input-02a-engine/2026-10-15-input-02a-closeout-gap-uus81ifm/remaining-verification-summary.json)·[19결과 hash](../../temp/input-02a-engine/2026-10-15-input-02a-closeout-gap-uus81ifm/accepted-artifact-hash-checks.json)·[4정규화 원본](../../temp/input-02a-engine/2026-10-15-input-02a-closeout-gap-uus81ifm/canonical-source-checks.json): 선정 허용19파일의 입력/구조·보존 및 각 기록 범위의 readOnly Excel 재개방은 Pass다. 승인 계산/Excel 정답9건(F-D-02, F-S-01/02/03, F-D-03, F-S-04-COLUMN, F-S-05-COLUMN, F-S-04-ROW/DELETE)은 Pass이고 나머지10건은 추가 계산 제외·보존 정책에 따라 계산/정답 해당 없음이다. 지정 입력값/계산 정책/정답/native 대기와 미실행 허용 구조는0이다. F-S-06은 제외하며 이 수치는 전체 티켓 완료 판정이 아니다.
- 전체 완료 기준의 정본은 [Delivery Plan §6A](../design-docs/gas/mi-model-input-editing-delivery-plan.md#6a-in-progress-ticket--input-02a)와 [domain §1A.4 Q7](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md)이다. 최초 상태 대조에서는 기존 hash/문서만 확인했고, 이후 위 「승인4역할7경로 읽기→저장 시간·메모리 측정 완료」로 지정 순차 실측을 완료했다. 아래 표의 나머지 운영 검증과 실행 방식/최종 판정은 PENDING이다.

| 잔여 항목 | 필요한 검증/기록 | 사용자 결정과 다음 순서 |
| --- | --- | --- |
| 4역할 자원 실측 — 지정7경로 각3회 완료 | 위 실측 결과를 따른다. source 읽기→입력/구조 반영→허용 POI→저장 시간·RSS 관측 peak/개별 kernel peak 확보. TRADE/OUTPUT 계산0 유지 | 실제 운영 자원·동시 요청 근거는 후속 확인. 사용자는 실측 결과를 보고 저장 대기시간/완료 알림 경험 결정 |
| 동시 요청·실패 격리 — 지정 로컬6시험 완료 | 위 probe의 동시4/2요청 분리·자원 관측·timeout/worker kill·scratch/자식 회수·원본/성공 결과 보존 Pass | 외부 제어기 생존 조건과 미검증 운영 환경을 실행 경계 판정에 전달. Q8 Draft revision/동시 생성/transaction 제품 검증은 별도 |
| DRM 처리 근거 | 복호화 표본 또는 검증 가능한 대체 절차, 평문 임시 파일 위치/접근/수명·삭제 | 실제 사용할 DRM 절차/환경 확인 필요. 외부 workbook 원본 요청·링크 갱신은 선행조건이 아님 |
| 실행 방식·최종 판정 | 요청 JVM 동기/격리 worker·비동기 경계, GO/CONDITIONAL/NO-GO와 INPUT-03/04/07 전달 제약 | 앞의 근거와 허용 대기시간을 보고 판정. 검증한 경로와 미지원 경로를 명시하며 전체 표 조사/범위 확대를 추가 선행조건으로 두지 않음 |

- 순차 실측과 지정 로컬 동시 요청/중단 정리 probe 완료 후 다음 제안은 DRM 표본/검증 가능한 대체 절차·평문 수명과 실제 운영 자원/허용 저장 대기·완료 경험 확인이다. 기존 입력/계산/보존 범위와 추가 입력값/Excel 정답 대기0을 유지하며, 실행 경계·GO/CONDITIONAL/NO-GO와 전달 제약을 정한다.
- 과거 [2026-10-08 prototype 실측](../work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md)은 존재한다. 당시 R상한/계산 범위·prototype이 달라 최신 승인 경로의 자원 증거로 대체하지 않는다. 현재 도구의 heap/timeout 설정도 실측 peak memory/wall time이나 중단 후 정리 Pass가 아니다. POI 부분 계산 후보 승인은 확정이며 상용 엔진/새 라이선스 승인을 재요청하지 않는다.
- 후속 제품 검증: F-O-03 C2 빈 값 금지/0 허용(기본값0 결정은 없음), F-D-04 E6·F-T-03 U5 빈 값 금지/기본값 숫자0은 정책 확정이다. 허용 CLEAR 파일 검증에서는 제외하고 INPUT-07/08의 실제 거부/기본값 적용 확인으로 남긴다. Q8 Draft revision/실패 취소·INPUT-10 Case/Run E2E는 후속 티켓이며 INPUT-02A의 새 완료 조건으로 추가하지 않는다. 일반 TRADE H1/H4 등 지정 보존 예외 밖 계산 정책은 지원 범위 확대 때 다룬다. F-D-05 중간 삽입·native 전체 재계산/저장은 이번 완료 선행조건이 아니다.

#### F-S-04-DELETE Avalon8/연결11·Excel6264/POI/native 완료 — 2026-10-15

- 업무 기준은 domain 규칙51의 사용자 지정 삭제/이동·국가6,264계산 승인이다. [최종 결과/hash](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/final-summary.json)·[삭제 원본](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/deleted-records.json)·[기록 이동](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/record-movements.json): canonical manifest의 동일 hash 정규화 SUPPLY에서 master8 Avalon/연결11을 제거했다. A9:R379→A8:R3786,678셀, B12:DK382→B11:DK381 및 DM12:DV382→DM11:DV38146,004셀을 같은371설비 기록/타입/서식/cache와 함께 이동했다. master379/연결382·국가444의 해당 tail251셀은 기존 tail 서식의 실제 blank이며 물리 worksheet 전체 행 삭제/다른 데이터 비우기로 대체하지 않았다. 새8/11은 Port Kembla FSRU·과거24월0.25·기존 연합3 등 해당 원본 기록을 따른다.
- [37,842식 참조](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/formula-changes.json)·[공유 범위](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/shared-metadata-changes.json)·[예약 Helper 전후](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/reserved-helper-before-after.json)·[계산 순서](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepared/chain-changes.json): 삭제102식 chain 제거·생존/국가44,106주소 이동(460,804→460,702), 삭제된 shared master 승격/ref 조정·예약 경계 분리를 반영했다. A380:A3867셀의 공유 metadata만 조정했고 A380:A38910셀의 수식 내용/cache/타입/서식은 원본 그대로다. 이동한 설비37,842식은 평가하지 않고 같은 원본 기록의 저장값을 보존했다. 국가 G385:DK444→G384:DK4436,540셀 이동·58라벨/108월 유지·6,264 SUMIFS 시작11/끝381/국가 라벨 참조를 조정했고, 새 header384행 metadata만 생성했다. hidden FilterDatabase Regas 끝382→381 외 names/hidden·master filter389/양쪽 dimension·CF/validation/merge·기존 row/column 설정은 보존했다.
- [Excel 정답 적합성](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/golden-audit.json): Excel16.113.4/API build1004에서 manual/저장 전 계산 금지/forceDisable/doNotUpdateLinks/readOnly로 준비 복사본을 열어 **H386:DK443 한 범위/6,264셀만** 평가·별도 정답으로 저장했다. 23시트2,775,053셀의 주소·수식·실제 font/fill/border/number format/alignment/protection·비승인 값/타입·이름/숨김 적합성을 확인했다. 링크/매크로0·입력 hash 불변·앱/기존 workbook 저장 상태 복원 Pass다. 기존 설비 cache가 변경된 oracle로 정답을 만들지 않았다.
- [POI 계산/비교](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/evaluated/result.json)·[6,264 비교](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/evaluated/comparison.json): Java17/POI5.4.0 기본 evaluator 실제6,264/실패0/skip0·숫자6,264 Excel 일치·최대 차이6E-16·허용오차 max(1e-9,1e-9×최대 절댓값)이다. 기존 기록의 보존 선행값40,497개(기존 수식 cache31,535 포함)만 고정 typed 상수로 쓰고 승인 country식은 실제 평가했다. golden은 출력 저장/재읽기 후 비교에만 읽었고 source/새 country cache·golden으로 결과를 생성하지 않았다. 외부 workbook/링크/사용자 함수 없이 선택 부분만 평가했다. [구조 replay](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/prepare-replay/result.json)·[계산 replay](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/replay/result.json) 동일hash·replay oracle 미사용 Pass다.
- [최종 보존](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/final-preservation.json): 비대상 master25,287/Regas1,388셀과 이동 기록들의 승인 ref/cache 보존, 다른66/70part·실제 externalLink/cache·pivot/cache·dynamic metadata·styles 보존 Pass다. source VBA/connection/native Table은 없어 해당 없음이며 기존 다른 시트 #REF!를 고치거나 새 정답으로 승인하지 않았다. 대상2시트 기존/새 오류0·ZIP/62XML/저장 후 재읽기 Pass다.
- [최종 readOnly native](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/native-result.json): 같은 final hash0444 복사본의61,357값·생존/예약/국가 **44,116수식**·6표본을 saved XML과 대조했다. 숫자 표시 최대 차이3E-15 허용 오차 내·문자/수식 exact, 읽기 전후 값 동일·재계산/링크/매크로/저장0·manual/forceDisable/무저장 닫기·앱/기존 workbook 저장 상태 복원 Pass다. 44,116은 읽은 수식 수이고 계산한 셀은 국가6,264뿐이다. 전체 metadata/다른 시트 원본 보존은 saved OOXML 검사이며 native 모든 화면/서식 전수 인증으로 확대하지 않는다.
- [재현 명령](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/README.md)·[검증](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-poi-native-t3zjvdva/status-audit-current.json): 원본4/정규화4보관본/대장·기존967존재 자료 hash 불변·시작부터 누락된 docs 별칭3은 종료에도 동일·새 ZIP/XML/독립 replay·변경3문서 diff/링크/Git 제외 Pass. 입력/구조/보존/계산/Excel 정답/native 모두 Pass·지정 사례 Complete, 이전18허용 결과/음성 CLEAR는 유지했다. Java Gradle/JUnit은 해당 없음이며 F-S-06 제외 현재 선정된 허용19사례 지정 검증 완료·미실행 구조0·전체 INPUT-02A In Progress다.

#### F-S-04-DELETE 삭제 대상·연결 이동·계산 영향 설명 — 2026-10-15 (선정 전 이력)

- 업무 기준은 domain 규칙12/51이며 변경하지 않았다. [독립 원본/영향](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/impact-summary.json)·[실제 설비372행](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/facility-rows.json): master `BCRegasification`의 데이터8~379행이 삭제 가능 후보이고1~7행 헤더/안내·380행 이후 빈 예약 슬롯은 설비 삭제 대상이 아니다. `RegasAdjustment` 직접 삭제는 금지하고 master 선택 설비의+3 연결 행을 자동 삭제한다. 이전 ROW 신규 설비380이나2036 추가 열은 이 독립 원본에 없다.
- [추천/후보 실제 값](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/selected-source-observations.json): **원본8행 Avalon/Australia/Proposed ↔ 연결11행**을 추천한다. 이름이 유일하고 과거24월 각0.875·2030~2031 연간3.52의 미래 월 값이 있어 삭제와 국가 합계 영향을 확인할 수 있다. 원본9 Port Kembla FSRU/연결12가 새8/11로 함께 이동해 과거 월0.25와 기존 저장값이 그 설비를 따라가는지도 확인한다. 마지막 원본379 GNL Del Plata/연결382도 삭제 가능하나 남은 설비 이동 검증은 하지 못한다. 삭제 행은 아직 선정하지 않았다.
- 원본8 삭제의 미승인 구조 안: master A9:R379→A8:R378(371행/6,678셀), 연결 B12:DK382→B11:DK381 및 DM12:DV382→DM11:DV381(371행/46,004셀)로 이동하고 master379/연결382의 해당 tail은 원본 tail 서식의 실제 blank로 둔다. DL spacer/표 밖 다른 좌표·물리 worksheet 행/열/dimension은 유지한다. [이동 수식 예/검사](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/moved-formula-examples.json): master742식(A Helper·R MAX)과 연결37,100식(B:F 미러·G 최대 용량 lookup·AF:DK 연간÷12·DM 국가·DN:DV 연간합) 총37,842식의 주소를 새 기록 좌표에 맞춘다. 수식/cache/타입/서식은 같은 원본 설비 기록을 따라가며 삭제 좌표의 cache를 다음 설비에 붙이지 않는다. 예: 원본 B12=`BCRegasification!B9`→새 B11=`BCRegasification!B8`, 원본 DN12→새 DN11의 local row12→11이다.
- [국가6,264식 이동/끝행 제안](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/country-formula-proposals.json): G385:DK444→G384:DK443, 국가 라벨 G387:G444→G386:G443·58개 유지, 월합 H387:DK444→H386:DK443이다. 예 `H387=SUMIFS(H$11:H$382,$D$11:$D$382,$G387)`→`H386=SUMIFS(H$11:H$381,$D$11:$D$381,$G386)`. **시작11은 유지·끝382→381**이며 source444의 G:DK tail은 빈다. [삭제행 월 기여](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/deleted-row-month-contribution.json)는 호주2023~2024/2030~2031의48월 값이 빠지는 의미를 보여 주지만 Excel 정답이나 실제 계산 결과가 아니다. 호주 국가 행은 남는다.
- 추천 계산은 **RegasAdjustment!H386:DK443의6,264셀만**이다. Avalon 이름이 유일해 생존 설비들의 첫 일치 lookup 대상은 같은 원본 설비이며, 이동한37,842식은 참조만 조정하고 원본 기록의 저장값을 보존하는 안이다. 이 보존값을 신규 산술 정답으로 주장하지 않는다. 이동식까지 모두 재평가하려면44,106셀(742+37,100+6,264)이며 현재 자동 확대하지 않는다. 계산 제외를 선택하면 국가 합계는 삭제 전 cache가 남으므로 삭제 반영 합계 Pass를 주장할 수 없다. 실제 계산 선택 시 동일 조건 Excel 정답 확보 후 독립 POI 선택 평가/저장·값/타입/오류/허용 오차 비교를 수행한다.
- [공유/metadata·소비 참조](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/metadata-and-dependencies.json): 삭제102식의 chain 제거·37,842 이동식/국가6,264 주소 조정과 shared master 승격/ref 재구성이 필요하다. A380:A389 예약 Helper의 수식 내용/cache는 보존하며 shared 범위 경계만 필요하면 별도 기대 변경으로 분리한다. Regas hidden FilterDatabase B10:G382→B10:G381, master filter B7:F389/양쪽 dimension·광범위 CF/기존 CHECK용 CF·validation/merge는 원본 보존을 제안한다. SUPPLY 다른 시트의 직접 대상 참조 수식0, 실제 pivot source는 BCProduction이며 외부 workbook/링크 새로고침·매크로를 실행하지 않았다. 중복 이름 Malta LNG2행 중 하나를 선택하면 남는 동일 이름의 lookup 의미가 바뀔 수 있어 현재 최소 계산 범위를 그대로 사용하지 않는다.
- source 해석: 기존 docs/references/model-input SUPPLY/TRADE/OUTPUT 별칭3파일은 시작 시 누락됐으나 canonical manifest normalizedPath의4파일 hash가 모두 기존과 일치한다. 이번에는 같은 SUPPLY bytes(`eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36`)의 보관본을 읽었고 workbook 복사/복원/생성은 하지 않았다. [재현/검사](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/README.md)·[검증](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/verification.json)·[현재 상태](../../temp/input-02a-engine/2026-10-15-f-s-04-delete-impact-swbhapje/status-audit-current.json): 기존943개 존재 자료/4보관본 불변·별칭3 시작/종료 누락 동일·domain 불변·변경2문서 diff/링크/Git 제외 Pass, workbook/계산/Excel/native0·허용18완료/미실행1·전체 INPUT-02A In Progress다.

#### F-S-04-ROW 설비380/연결383·Excel6366/POI/native 완료 — 2026-10-15

- 업무 기준은 domain 규칙51의 사용자 지정 fixture/6,366셀 계산 승인이다. [최종 결과/hash](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/final-summary.json)·[입력 전후40셀](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/prepared/input-records.json): 독립 정규화 SUPPLY의 master380에 원본8행 식별/2025~2035 연간 용량을 값으로 복사하고 E380만 `검증용 신규 설비`, 연결383의2023~2024 월24값은 숫자0으로 채웠다. 원본8은 Australia/Proposed이며 기존 A380 공유 수식은 유지·R380/연결100식은 신규 생성했다. 원본/source/기존 결과를 덮어쓰지 않았다.
- [구조/국가6,540셀 이동](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/prepared/country-movements.json)·[전체6,366식](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/prepared/formula-cells.json): G385:DK444→G386:DK445, 국가58라벨/108월 기간 유지·SUMIFS 시작11/끝382→383·국가 라벨 참조와 shared master/ref를 조정했다. 새101식은 독립 수식, 기존 A380 shared/si는 보존했다. chain은460,804→460,905(신규101·A380 기존1개 위치 조정·국가6,264주소 이동)이며 새 연결식/Helper/MAX가 국가합보다 먼저 온다. hidden FilterDatabase의 Regas B10:G382→B10:G383만 이름 기대 변경이며 기존 master filter389/양쪽 dimension·CF·validation·merge·row/column 설정은 그대로다.
- [Excel 정답 적합성](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/golden-audit.json): Excel16.113.4/API build1004에서 readOnly 복사본을 manual/저장 전 계산 금지/forceDisable/doNotUpdateLinks로 열어 승인6개 범위/6,366셀만 평가하고 별도 oracle로 저장했다. 23시트2,775,169셀의 주소·수식·resolved font/fill/border/number format/alignment/protection·비승인 값/타입 및 workbook 이름/숨김 상태 적합성을 확인했다. 재색인된 style ID는 실제 서식 의미로 대조했다. 링크 갱신/매크로 실행0·입력 복사본 무변경·기존 앱/workbook 저장 상태 복원 Pass다.
- [독립 POI 계산/비교](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/evaluated/result.json)·[6,366개 값/타입 비교](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/evaluated/comparison.json): Java17/POI5.4.0 기본 evaluator 실제6,366/실패0/skip0, 문자열7/숫자6,359 모두 Excel과 일치·최대 차이6E-16(허용 오차 max(1e-9,1e-9×최대 절댓값))이다. literal/fixed source47,100개 중 기존 미변경 선행 수식 cache31,992개만 고정값으로 사용했다. 승인 식/new cache나 golden은 계산 입력에 넣지 않았고 POI 출력 저장/재읽기 후에만 golden을 읽었다. 외부 workbook 파일/링크/사용자 함수 없이 선택 부분만 평가했다. [구조 replay](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/prepare-replay/result.json)·[계산 replay](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/replay/result.json)의 hash도 동일하며 replay는 golden 미사용이다.
- [최종 원본 보존](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/final-preservation.json): master31,973/Regas47,508 비대상 셀과 A381:A389 수식/cache/서식, 다른66/70part·VBA 없음/실제 externalLink-cache·pivot-cache·dynamic metadata·styles는 원본 그대로다. 허용 변경은 두 시트 지정 입력·신규식/계산 cache·국가 이동/shared/ref·hidden name1·chain이며 원본 보존과 구분했다. 두 대상 시트 기존/새 오류0, 다른 시트의 기존 #REF!는 원본 그대로다. ZIP/62XML/저장 재읽기 Pass다.
- [읽기 전용 native](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/native-result.json): 최종 동일hash0444 복사본을 재개방해61,357값·승인6,366수식·6표본을 saved XML과 대조했다. 숫자 표시 최대 차이3E-15 허용 범위 내·문자/수식 exact, 읽기 전후 값 동일·재계산/링크/매크로/저장0·manual/forceDisable/무저장 닫기·설정/기존 workbook 상태 복원 Pass다. 전체 보존과 실제 metadata는 saved OOXML 검사이며 native 모든 화면/서식의 전수 인증으로 확대하지 않는다.
- [재현 명령](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/README.md)·[최종 검사](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-15-f-s-04-row-poi-native-u79uepm5/status-audit-current.json): 원본4/source4/대장·기존847자료 hash 불변·새 ZIP/62XML/독립 replay·변경3문서 diff/링크/Git 제외 Pass. 입력/구조/보존/계산/Excel 정답/native 모두 Pass·지정 사례 Complete다. 이전17허용 결과를 반복하지 않았고 Java Gradle/JUnit 해당 없음이다. F-S-06 제외 허용18완료·미실행 구조1=F-S-04-DELETE, 전체 INPUT-02A In Progress다.

#### F-S-04-ROW 추가 위치·연결 수식·계산 영향 설명 — 2026-10-15 (선정 전 이력)

- 정책은 domain 규칙12/51이며 변경하지 않았다. [독립 source/관측](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/impact-summary.json)·[선택 셀/타입/수식/저장값](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/selected-source-observations.json): 정규화 SUPPLY SHA-256 `eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36`의 `BCRegasification` 설비는8~379행372개, 대응 `RegasAdjustment`는11~382행이다. 다음 설비는 master380/연결383으로 제안한다. master380에는 빈 서식 B:R과 **기존 A380=`E380&F380` 공유 수식/빈 cache**가 이미 있으므로 물리 worksheet 행 삽입이나 Helper 신규 생성으로 오기하지 않는다. A381:A389 예약 수식과 기존 모든 설비는 보존한다. 이전2036 열 추가 결과를 source로 쓰지 않는다.
- [신규 입력/질문](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/cases-current.json): master B380:F380=대륙·지역·국가·설비명·Life Cycle Category5값, G380:Q380=2025~2035 연간 용량11값이다. 연결 H383:AE383=2023~2024 월별24 literal은 master에 같은 연도가 없어 별도 선택이 필요하다. 모두0/기존 행 복사는 사용자가 지정할 수 있으나 ROW 기본값으로 자동 확정하지 않으며 COLUMN의 신규 연도0 정책을 확대하지 않는다. [기존58개 국가](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/country-labels.json) 중 한 국가·기존과 다른 설비명을 검증값으로 추천한다. 국가 목록 확장은 현재 제외며 없는 국가 선택 시 별도 결정이 필요하다. G VLOOKUP/AF:DK INDEX-MATCH가 설비명 첫 일치를 읽으므로 같은 이름은 앞 설비를 읽을 수 있다. 새로운 전역 unique-key 정책을 정의한 것은 아니다.
- [연결 수식 제안](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/formula-target-proposals.json): master 기존 A380은 설비명+category Helper, 새 R380은 `MAX(G380:Q380)`다. Regas B383:F3835식은 master B380:F380, G383은 설비명으로 master R 최대 용량을 찾고, AF383:DK38384식은2025~2031 해당 연간 용량÷12, DM383은 국가 D383, DN383:DV3839식은 각2023~2031 월 값 SUMIFS다. 신규 수식101개+기존 A380 활성1개=102평가 후보이며2023~2024의24입력과 구분한다. master2032~2035는 최대 용량에 포함되나 Regas 기간/월 열을 추가하지 않는다.
- [국가 블록/전체6,264식 제안](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/country-formula-proposals.json): G385:DK444→G386:DK445, 국가58라벨 G387:G444→G388:G445, 월합 H387:DK444→H388:DK445로 이동한다. 예: `H387=SUMIFS(H$11:H$382,$D$11:$D$382,$G387)`→`H388=SUMIFS(H$11:H$383,$D$11:$D$383,$G388)`. 시작11은 유지하고 끝382→383과 국가 라벨 참조를 조정한다. **최신 같은 source에는 국가6,264 SUMIFS와 master372 MAX가 실제 수식으로 모두 존재**한다. domain §14.5의 과거 literal/66식 관측은 현재 source 사실로 재사용하지 않는다. 새 설비 자체의 값 기여는 해당 국가108월에 붙지만 다른 국가의 기존 cache도 새 계산 정답으로 보증하지 않으므로 전체6,264 비교를 추천한다.
- 계산 제안은 master A380/R3802 + Regas B383:G3836·AF383:DK38384·DM383:DV38310 + 국가 H388:DK4456,264 = **6,366셀**이다. 신규/활성102를 먼저, 국가 합을 뒤에 평가하며 기존 다른 설비의 월31,248/연합3,348식은 자동 재계산 범위에 넣지 않는다. 새101수식에는 해당 설비의 원본 저장값이 없으므로 계산하지 않은 새 결과를 앞 설비의 cache 복사로 정답 처리할 수 없고, 기존 국가 합계 cache 보존도 신규 설비 반영을 보증하지 않는다. 입력값/계산 선정 후 동일 변경 Excel 정답과 실제 POI 선택 계산·저장/비교를 실행한다. 현재 workbook/엔진 평가/Excel/native0이며 추천을 승인으로 사용하지 않는다.
- [실제 metadata 관측](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/metadata-observations.json): master dimension A1:V5402·filter/hidden name B7:F389·넓은 CF는 이미380을 포함한다. Regas dimension B1:DX766, hidden FilterDatabase B10:G382와 shared/calcChain·새 행 서식은 실제 구조 명세에서 기대 변경을 분리한다. 양쪽 validation/merge/native Table은 없다. 기존 DW/EB 조건부서식만 보고 없는 CHECK 셀/수식을 생성하거나 평가하지 않는다. [재현/검사](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/README.md)·[검증](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/verification.json)·[현재 상태](../../temp/input-02a-engine/2026-10-15-f-s-04-row-impact-dwfztoxv/status-audit-current.json): 보호825자료 불변·업무 정본 변경0·문서2개 diff/링크 검사 Pass, 허용17완료/미실행2·전체 INPUT-02A In Progress다.

#### F-D-05-DELETE Sector19 삭제·이동·보존·native 완료 — 2026-10-15

- 업무 기준은 domain 규칙45의 사용자 최종 결정이다. [최종 결과/hash](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/final-summary.json)·[실제 구조/저장](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/result.json)·[전후 이동 기록](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/cell-movements.json): 독립 정규화 DEMAND에서 원본 `D19:K19`를 제거하고 `D20:K507`의488행/3,904셀을 `D19:K506`으로 이동했다. J 값·K cache/타입·서식은 해당 원본 기록을 따라가며 `D507:K507`8셀은 원본 tail 서식의 실제 blank다. [삭제 원본](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/deleted-original-row.json)·[tail 전후](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/tail-changes.json)에 값/타입/수식/저장값/서식을 남겼다. APPEND 결과·worksheet 전체 행 삭제·입력만 CLEAR로 대체하지 않았다.
- [K 참조/shared](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/formula-changes.json)·[이동 밖 master](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/shared-master-changes.json)·[계산 순서](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/chain-changes.json): K488식의 참조를 새 행에 맞추고 원본 계산 저장값은 바꾸지 않았다. K9은 수식/cache/타입/서식을 유지한 채 ref K9:K46→K9:K45만 조정했으며 나머지 이동 master의 주소/범위를 맞췄다. shared si는 유지했다. chain은 전체699,298→699,297, target1,000→999(원본 K19 제거1·K20:K507 주소 이동488)이며 다른 항목 bytes/실효 sheet 소속은 불변이다. K499식/S500식이 남았고 추가 계산/새 계산 저장값0·Excel 계산 정답 해당 없음(PRESERVE)이다.
- [원본 보존](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/execution/preservation.json): 비대상3,728셀과 S500식·Monthly 전체·제목/헤더·기존 row/column/cell 좌표집합/dimension B1:AD507·non-cell XML·workbook names/hidden/states/styles·나머지66/68part는 원본 그대로다. 허용 변경은 이동/빈 tail·K 참조/shared/chain이며 같은 시트의 원본 보존과 구분했다. 원본 Monthly validation M8:N507의 #REF!도 수리/확장하지 않았다. VBA/externalLink/cache/connection/pivot/cache/native Table/dynamic metadata는 source에 없어 해당 없음으로 기록했다.
- [readOnly native](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/native-result.json)·[전 표 값 비교](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/native-value-comparison.json): Excel16.113.4/API build1004에서 같은 final hash0444 복사본의 `D7:S507` 8,016개 값·K/S 999개 수식·6표본을 확인했다. 최대 숫자 차이2E-15(허용 오차 max(1e-9,1e-9×최대 절댓값)) 내·수식/문자열 exact, 읽기 전후 값 동일·재계산/링크 갱신/매크로/저장0·manual/저장 전 계산 금지/forceDisable/doNotUpdateLinks/readOnly·앱/workbook/기존 저장플래그 복원 Pass다. 실제 tail blank/Helper blank cache는 saved XML로 구분했으며 전체 metadata/다른 시트 보존을 native 전수 화면 인증으로 확대하지 않는다.
- [재현 명령](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/README.md)·[최종 검사](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-native-clh5fo72/status-audit-current.json): 독립 재생성 동일 output hash·ZIP/57XML/저장 재읽기·원본/source/대장·기존770자료 hash 불변·변경3문서 diff/링크/Git 제외 Pass다. 입력/구조/보존/native 모두 Pass·지정 사례 Complete·남은 확인 없음, 기존16허용 결과/다른2대기 명세는 그대로다. Java/POI/Gradle/JUnit·다른 사례 반복0이며 F-S-06 제외 허용17완료·미실행 구조2·전체 INPUT-02A In Progress다.

#### F-D-05-DELETE 대상 행·Helper 이동·계산 영향 설명 — 2026-10-15 (선정 전 이력)

- 정책은 domain 규칙45이며 변경하지 않았다. [원본 관측](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/impact-summary.json)·[실제 기록 행](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/source-row-observations.json): 독립 정규화 DEMAND `DemandOutputOverwrite`의 부문별 T-018은 D6:K507, 입력 D8:I507이며 원본8~19행에 China/2026년1~12월/Losses 기록12개가 있다. 20~507은 입력 빈 슬롯, 월별 T-019 M8:Q507은500슬롯 모두 비어 있다. 제목/헤더6~7행은 삭제 대상 데이터에 포함하지 않는다. 행 삭제를 끝 행으로 제한하는 정책은 없으며 마지막 행 제약은 추가에 적용된다. APPEND 결과의508행은 이 독립 source에 없다.
- [미실행 구조 예시](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/symbolic-deletion-examples.json): 실제 마지막 기록인 **원본19행** 삭제를 추천한다. 선택 논리 표 `D19:K19`를 제거하고 `D20:K507`의488행/3,904셀을 `D19:K506`으로 이동하며 D507:K507은 원본 tail 서식을 유지한 실제 빈 슬롯으로 둔다는 안이다. 이전 실제 입력8~18행·옆 Monthly/다른 시트/A·물리 row/column/dimension B1:AD507은 유지한다. 원본8행을 고르면 D9:K507→D8:K506으로499행/3,992셀을 이동하므로 실제 뒤 기록도 이동한다. 어느 행도 자동 선정하지 않았다.
- 보존 방식의 차이: 입력 D:I만 이동하고 J/K를 원래 좌표에 고정하면 이동한 기록과 기존 J literal/K 문자열 키가 어긋날 수 있다. 추천안은 J 값과 K cache를 **같은 원본 기록과 함께 이동**하고 K 수식의 셀 주소만 새 행으로 조정하는 방식이다. 예를 들어 원본 K20=`D20&E20&F20&G20`을 새 K19=`D19&E19&F19&G19`로 옮기며 저장값/타입은 원본 K20의 것을 유지한다. 삭제된 K19의 문자열 키는 제거한다. 이는 현행 K/S 원본 보존만으로 자동 승인되는 동작이 아니므로 삭제행·옮길 열·참조/shared/chain 변경을 사용자 선택 전 제안으로 분리했다. 수식 원문을 주소 조정 없이 옮기거나 빈 cache를0/새 계산값으로 채우지 않는다.
- [Helper/shared/chain 관측](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/helper-structure-observations.json): 원본 K/S는500수식씩·calcChain 대상1,000개, shared master17개다. 19행 삭제 추천안에서는 K19 하나를 제거하고 K20:K507의488식/주소를 한 행 위로 조정해 K499식/S500식=999식이 된다. shared 범위와 calcChain의 제거1/이동488도 같은 승인 구조 범위에 포함해야 하며 S500식·옆 표 validation은 원본 그대로 둔다. 계산은 기존 규칙45대로 K/S 평가 제외를 유지하면 **추가 계산0·Excel 계산 정답 해당 없음**이다. 주소 조정과 실제 수식 평가는 별개다. J/R은 원본 수식이 없는 저장값/빈 셀이므로 자동 수요량 계산을 추가하지 않는다.
- [현재 사례/질문](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/cases-current.json)·[상태](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/status-audit-current.json)·[재현/검증](../../temp/input-02a-engine/2026-10-15-f-d-05-delete-impact-l8tjxqfi/verification.json): 설명 Complete, 실제 표/삭제 행과 기록 동반 이동/참조 조정의 승인 및 파일/보존/native는 PENDING이다. 업무 규칙/워크북 변경·계산·Excel/native0, 기존16허용 결과·다른2대기 명세 불변·F-S-06 제외 미실행 구조3·전체 INPUT-02A In Progress다. 같은 source hash의 기존 metadata/직접 시트명 참조0 관측을 재사용했으며 이를 외부/동적/모델 사용 판정으로 확대하지 않았다.

#### F-D-05-APPEND Sector508 입력·보존·native 완료 — 2026-10-15

- 업무 기준은 domain 규칙45의 사용자 최종 결정이다. [새 결과/hash](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/final-summary.json)·[입력/구조](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/execution/result.json)·[정확한 전후 값/타입/빈칸](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/execution/input-changes.json): 독립 정규화 DEMAND에서 T-018만508행까지 논리 확장했다. 신규 `D508:I508`은 source `D19:I19`의5개 literal/1개 실제 blank이고, 신규8셀 `D508:K508` 서식은 source507행을 따랐다. I508/J508/K508은 실제 빈 셀이며0/빈 문자열/수식으로 대체하지 않았다. 기존20빈 슬롯 활성화나 worksheet 전체 행 삽입·옆 Monthly 확장은 아니다.
- [보존](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/execution/preservation.json): 기존7,641셀의 XML/값·타입·수식·저장값·서식과 K/S1,000식/저장값을 그대로 유지했다. 승인된 변화는 신규8셀·`row508 spans=4:11`·dimension `B1:AD507`→`B1:AD508`뿐이다. 나머지67/68 source part·기존 row/col·non-cell XML·names/hidden/validation/merge·Monthly/A/다른 시트·실제 externalLink/cache/connection/pivot/cache/VBA 등의 존재와 원본 보존을 검사했고 부재 항목은 해당 없음이다. Monthly의 기존 M8:N507 validation `#REF!`도 확장/수리 없이 보존했다. 신규 수식/평가0, 계산/Excel 계산 정답 해당 없음(PRESERVE)이다.
- [읽기 전용 native](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/native-result.json)·[값 전수 비교](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/native-value-comparison.json): Excel16.113.4/API build1004에서 같은 final hash의0444 복사본을 readOnly/manual/저장 전 계산 금지/forceDisable/doNotUpdateLinks로 열었다. D7:S508의8,032값·K/S1,000수식·6표본을 비교해 Pass, 최대 숫자 표시 차이2E-15(허용 오차 max(1e-9,1e-9×최대 절댓값))다. 읽기 전후 값 동일·재계산/링크 갱신/매크로/저장0, 무저장 닫기·앱/workbook/기존 저장플래그 복원·원본/final/읽기 전용 복사본 hash 불변이다. 전체 스타일/다른 시트/cache/metadata 보존은 saved XML/part 검사이며 native 전수 화면 인증으로 확대하지 않는다.
- [재현](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/README.md)·[최종 hash/문서 검사](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/verification.json)·[현재 사례 상태](../../temp/input-02a-engine/2026-10-15-f-d-05-append-native-amblcoyi/status-audit-current.json): 독립 재생성 동일 final hash, XLSM CRC/57XML/저장 재읽기·기존702자료 불변·변경3문서 diff/링크/Git 제외 Pass다. 입력/구조/보존/native 모두 Pass·지정 사례 Complete·남은 확인 없음이며 기존15사례/다른3대기 명세는 유지했다. F-S-06 제외 허용16완료·미실행 구조3·전체 INPUT-02A In Progress다. Java/POI/Gradle/JUnit 및 다른 사례 반복0이다.

#### F-D-05-APPEND 대상 표·입력·영향 설명 — 2026-10-15 (선정 전 이력)

- 업무 정책은 domain 규칙13/45이며 이번에는 변경하지 않았다. [원본 관측/선택지](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/impact-summary.json)의 정규화 DEMAND `DemandOutputOverwrite`에서 Sector(T-018) 표는 `D6:K507`, 입력 `D8:I507`이고 실제 값은8~19행·다음 빈 슬롯은20행이다. Monthly(T-019) 표는 `M6:S507`, 입력 `M8:Q507`이며 모든500슬롯이 비어 있어 첫 슬롯은8행이다. 508행은 양쪽 모두 셀이 없어 실제 논리 표 확장이다. 빈 슬롯 활성화를 새 행 확장 검증으로 부르거나 두 표/worksheet 전체 행을 함께 삽입하지 않는다.
- 사용자 지정값은 Sector의 국가D·연도E·월F·수요 하위부문G·수요량H(BCM)/I(백만톤 LNG), 또는 Monthly의 국가M·연도N·월O·수요량P(BCM)/Q(백만톤 LNG)다. BCM은10억㎥다. 사용할 수량 열/값과 미사용 칸을 실제 빈칸 또는 숫자0 중 어떻게 둘지는 이 fixture에서 지정받으며 새 필수값/기본값 업무 정책으로 확대하지 않는다.
- [수식·셀·metadata 관측](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/source-inspection.json): K8=`D8&E8&F8&G8`, S8=`M8&N8&O8`는 키 문자열을 이어 붙이는 Helper다. K의 일부 기존행은 결합 순서가 다르며 원본 그대로 보존한다. K/S500식씩은 현재 재계산/508 수식 확장 제외다. J/R Selected Gas Demand는 원본에서 수식0개이며 Sector J8:J19는 literal, 그 밖은 빈 셀이다. 입력 후 J/R 수요 환산·합계를 자동 생성한다고 해석하지 않는다. [직접 참조 조회](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/direct-reference-observations.json)는 이 workbook 저장 수식에서 해당 시트명을 직접 읽는 식0개이며 동적/외부/모델 실행 사용 여부의 판정은 아니다.
- 제안은 **Sector 표를 논리508행까지1행 확장**, 신규 `D508:I508` 입력과 `D508:K508` 서식만 적용하는 독립 구조 사례다. 신규 J508/K508은 서식만 있는 실제 blank·수식 추가 없음, 기존 `K8:K507`/S·J/R 및 옆 Monthly/다른 시트/A는 보존한다. 예상 metadata는 사용 범위 `B1:AD507`→`B1:AD508`와 신규 row뿐이며 native Table/표 이름/filter/merge/조건부서식/Sector validation은 원본에 없다. 이 제안은 아직 사용자 선정 전이다. Monthly508을 선택하면 월 목록 validation 확장과 원본 `M8:N507` 목록의 기존 `#REF!` 보존/신규 적용 경계를 따로 정하며 오류를 임의 수리하지 않는다.
- [현재 사례/질문](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/cases-current.json)·[상태](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/status-audit-current.json)·[재현/검증](../../temp/input-02a-engine/2026-10-15-f-d-05-append-impact-tjg33ibk/verification.json): 설명 Complete, 실제 표/위치/입력값 선정 및 입력/구조/보존/native는 PENDING다. 현행 K/S 보존·확장 제외를 유지하면 계산/Excel 계산 정답은 해당 없음이다. workbook 변경/계산/Excel/native0, 기존15허용 결과·다른3대기 명세 불변, F-S-06 제외 미실행 구조4·전체 INPUT-02A In Progress다.

#### F-D-05 마지막 행 추가·행 삭제 최종 정책 후속 — 2026-10-13

- 정책 정본은 domain 규칙13/45다. 사용자는 이번 `DemandOutputOverwrite` 범위를 **마지막 행 추가·행 삭제만 지원**, **중간 행 삽입은 제외하고 나중에 요청 시 검토**로 최종 선택했다. 앞선 중간 삽입 승인/공수 비교의 미결정을 대체하며 중간 삽입을 현재 구현/파일 검증의 대기 항목으로 남기지 않는다. 열 추가 불가·K/S 재계산/신규 수식 확장 제외·옆 표 보존은 유지한다.
- 독립 명세: [현재 추가/삭제2사례](../../temp/input-02a-engine/2026-10-13-f-d-05-final-policy-pn8ahc81/cases-current.json)와 [정책 결과](../../temp/input-02a-engine/2026-10-13-f-d-05-final-policy-pn8ahc81/policy-result.json)에 F-D-05-APPEND/F-D-05-DELETE를 연결했다. 실제 표/행/신규 값·삭제 후 이동과 K/S·옆 표/metadata 기대값은 PENDING이므로 새 XLSM/계산/Excel/native/제품 검증은 하지 않았다. 추가를507/508행으로 자동 지정하거나 삭제를 내용 CLEAR로 대체하지 않는다.
- 상태: 정책 선택 Complete, 두 구조 사례의 파일 검증 PENDING, 기존 INSERT_ROW 후보는 이번 범위에서 해당 없음이다. 기존 fixture/명세/비교/검증 기록은 수정하지 않고 후속으로 현재 상태만 대체한다. F-S-06 제외 미실행 명세7→8(CLEAR1·구조7)은 동작 분리 때문이며 원래 실행/완료 결과는 유지한다. 전체 INPUT-02A In Progress다.

#### F-D-03 O열 입력·합계 분리 및 지정값 후속 — 2026-10-13 (계산 전 이력)

- 정본은 domain 규칙50의 F-D-03 후속이다. 명시된 시트/동작으로 기존 F-D-03을 식별했으며 F-D-05 행 변경 사례 번호/정책은 재사용하지 않는다. 사용자 직접 입력91셀(연도1·국가90)과 자동 합계16셀을 구분한다. 기존13합계는 N→O 참조 이동 복제, 추가 `O25=SUM(O26)`·`O27=SUM(O28:O33)`·`O24=SUM(O25,O27)`은 원본의 literal 소계/합계3행에 대한 명시적 후속 승인이다. 기존 E:N은 보존한다.
- 지정값: O3=2036, 국가별90셀은 source N열의 값/타입을 literal로 복사한다는 사용자 확인을 받았다. 합계16셀에는 수식을 생성하며 기존 N 합계 저장값이나 Excel golden을 새 O열 계산 결과로 복사하지 않는다. [정책/후보 수식](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/policy-result.json)과 같은 경로의 사용자 원문/응답·선정 셀/metadata에 결정·관측을 분리했다.
- 구조 기대값: 새 셀은 O3:O109의107셀이고 기존 행/열/셀을 삽입·이동하지 않는다. 기존 N셀 서식을 새 O셀에 복제, N/O의 동일 기본 열 폭·기존 행 높이는 유지한다. dimension B1:N109→B1:O109 및3:109행 spans2:14→2:15를 확장한다. 실제 validation은 C1 선택지뿐이고 해당 시트 names/native Table/merge/autoFilter는 없으므로 원본 유지/신규 생성 없음이다. calcChain은 기존 엔트리를 보존하고 새 O 합계16셀을 선행 순서로 추가한다. 그 밖 원본 셀/metadata/part는 보존한다. 이 동작은 논리 연도 열 추가이며 worksheet 전체 열 삽입은 아니다.
- 실제 파일 실행: source에서 새 독립 [입력·수식 XLSM](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/execution/F-D-03-DEMAND-YEAR2036-INPUT-FORMULAS-PRESERVED.xlsm)을 생성·저장 후 재읽어 입력91셀/수식16셀·승인 구조/원본 보존 **Pass**. 기존1,406셀의 XML과 E:N130수식/저장값, 나머지66part 및 실제 metadata/원본 calcChain699,298엔트리 보존을 검사했다. 새107셀·dimension/spans·추가16chain 엔트리만 기대 변경이다. 새수식 cache는 미평가로 생략했고 기존 N 저장값/golden으로 생성하지 않았다. ZIP CRC·57 XML/rels parse 및 기존47자료 hash 불변, 별도 재생성 XLSM hash 일치 Pass.
- 상태/다음: 입력·수식 생성·구조/보존 **Pass**, 실제 계산·동일 변경 Excel 정답·native **PENDING**. 기존 E:N 재계산 없이 새 O16합계만 평가/비교해야 한다. [실행 결과](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/execution/result.json), [보존/실제 셀](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/execution/preservation.json), [Excel 제출 명세](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/execution/excel-submission.json), [재현 안내](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/README.md)와 [현재 사례 상태](../../temp/input-02a-engine/2026-10-13-f-d-03-column-policy-ml9byuza/status-audit-current.json)를 따른다. 이 입력/수식 실행을 전체 INPUT-02A 완료로 확대하지 않는다.

#### F-D-04 빈 값 금지·기본값0 후속 — 2026-10-13

- 업무 정본은 domain §6 규칙50의 F-D-04 사용자 확인이다. E6는 실제 빈 값 금지·기본값 숫자0이다. 초기 CLEAR 양성 명세는 거부 기대값으로 변경하며 다른 LNG 입력의 빈 값/기본값이나 일반 자동0 보정으로 확대하지 않는다.
- 정책 대기 해소, 허용 입력 파일 검증 대상 제외. F-D-04는 아직 변경 파일을 생성하지 않았으므로 실제 업무 입력 Fail 파일이나 거부 검사 Pass를 주장하지 않는다. 해당 CLEAR의 계산/Excel 계산 정답/native는 해당 없음, 제품의 빈 값 거부/기본값 적용 검증은 PENDING(이번 파일 범위 밖)이다. 숫자0 SET 파일·새 계산·Excel 정답/native 검증도 실행하지 않았다. 기존 F-D-02 SET/POI/Excel 비교·보존/native Pass는 유지한다.
- 근거/현재 상태: [후속 정책 결과](../../temp/input-02a-engine/2026-10-13-f-d-04-policy-iv00xkk0/policy-result.json), [현재 사례 상태](../../temp/input-02a-engine/2026-10-13-f-d-04-policy-iv00xkk0/status-audit-current.json). 이전 명세/감사의 F-D-04 PENDING은 이 사용자 결정 전 이력이며 기존 파일은 수정하지 않는다. F-S-06 제외 남은 계산 정책3·미실행 허용 후보8(CLEAR1·구조7), 제품 거부 검증 F-O-03/F-D-04를 분리한다.

F-T-01의 AG/AI, F-T-02의 원본 #N/A 셀과 F-T-04의 지정 5입력 밖 전체는 domain 규칙 59 예외에 따라 PRESERVE다. F-O-01은 domain §6.1 사용자 확인으로 B8 외 추가 갱신·계산이 없다. F-T-02의 비오류 셀 등 나머지 계산 세부는 H1/Q5에 연결한다. 보존 영역인 UNIQUE/Helper를 다시 계산하거나 source 오류를 0/공백으로 치환하지 않는다. 다른 사례에서 고정 기준값이 빈 경우의 업무 처리와 Excel 실행 실패는 별도 미확정 사유다.

2026-10-13 초기 대기 연결: [새 명세의 `userQuestion`·`canonicalDecisionLocation`](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/pending-cases-latest.json)에 사례별 근거·선택 차이와 domain 반영 위치를 기록했다. 그중 F-T-04 위치·필수 입력·5값·계산 대기 및 F-D-04/F-T-03 빈 값 정책 대기는 후속으로 해소했으며 기존 JSON은 결정 전 이력이다. F-D-03 O열은 지정값 입력/수식·구조/보존을 실행했고, 기존 F-D-05 INSERT_ROW는 이번 범위에서 제외해 추가/삭제2명세와 [최신 현재 상태](../../temp/input-02a-engine/2026-10-13-f-t-03-policy-vl76brg1/status-audit-current.json)를 따른다. F-O-04의 **OUTPUT `BCPrices`**21/22 선택은 후속 사용자 지정21/2036·가격값 복사로 해소됐으며 위 실제 실행/현재 상태를 따른다. F-T-03 CLEAR는 빈 값 금지이며 다른 표의 허용으로 재실행하지 않는다. 이후 명시적 업무 결정은 정본에 반영했고 기존 중간 삽입 질문/명세는 현재 승인으로 사용하지 않는다. F-O-01/02/03·F-D-04/F-T-03 및 지정 F-T-04에는 계산 golden이 필요 없지만, 나머지 계산/구조는 정책 확정 뒤 [동일 조건 Excel 제출 양식](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/excel-submission-remaining.json)에 전체 영향 범위·값/타입/오류·실행 순서·버전/설정·새 결과 파일/hash를 제출한다. 보존 기준은 계속 변경 전 source다.

#### F-T-04 생성 위치·필수 입력 후속 — 2026-10-13

**현재 실행 상태:** 아래 정책 확인 뒤 사용자가 검증값과 계산 제외 결정을 제공했다. 입력/보존 및 native Pass, 계산/Excel 계산 정답 해당 없음이다. 현재 결과는 다음 실행/계산 제외 후속을 따르고 앞선 명세/관측의 PENDING은 결정 전 기록으로 유지한다.

- 정본: domain 규칙 59·§1A.4 H2/H3에 **704행 빈 슬롯부터 신규 계약 생성, Q:U 전부 필수**라는 사용자 결정을 반영했다. 이 사례의 동작은 `ACTIVATE_BLANK_SLOT`이며 원본의 기존 셀에 새 논리 계약을 넣는 것이다. worksheet 행 추가/삽입·표 끝 확장과 구분한다.
- 상태: 위치·필수 입력 정책은 확정, 실제 신규 Q704:U704 값과 미확정 계산 처리/Excel 정답을 기다린다. 새 변경 XLSM은 아직 생성하지 않았으므로 입력/보존·계산·Excel 정답·native 실행은 PENDING이다. 필수 입력이란 값을 생략할 수 없다는 뜻이며 계약량 0·후보/키/CLEAR의 업무 의미까지 자동 확정하지 않는다.
- 근거/남은 질문: [새 사례 명세·source 좁은 확인](../../temp/input-02a-engine/2026-10-13-remaining-zzqtdo7r/2026-10-13-f-t-04-policy-i8r090u2/policy-result.json)에 Q704:U704·기존 사전 수식/서식·source hash와 남은 입력/계산 질문을 기록한다. 기존 명세는 결정 전 이력으로 유지한다. 새 계약 5값을 받은 뒤 확정 입력/보존부터 독립 실행하고 계산 대기는 분리한다.

#### F-T-04 지정 계약 빈 슬롯 활성화 실행 — 2026-10-13

- 조건: 같은 정규화 TRADE source에서 독립 시작했다. Q704/U704는 사용자 지정 number, R704:T704는 같은 source의 R134:T134 값/타입만 복사해 literal로 저장했다. 134행 수식/서식을 복사하지 않고 기존 704행 style을 유지했다. 정확한 값·원본/저장값·타입과 source/hash는 [새 사례 결과](../../temp/input-02a-engine/2026-10-13-f-t-04-slot-sh1g281l/result.json)에 있다.
- **입력/보존 Pass:** Q704:U704 다섯 필수값 저장·재읽기 일치, 그 구간의 완전 입력 계약 699→700건·704행만 활성화. 변경 셀은 이 5개뿐이며 기존 699계약·헤더·남은 슬롯·같은 시트 A/UNIQUE·V704:AJ704 15수식/저장값·다른 행·CN/CT는 원본과 같다. 행/열/셀 집합·참조·서식·validation·names/Table·merge·calcChain 등 metadata 변경은 없고 worksheet 행 삽입·표 경계 확장은 해당 없음이다.
- package/오류: 실제 hidden/Industry·backup 시트, UNIQUE 5셀·배열 결과, VBA·externalLink/캐시·connection/queryTable·native Table·dynamic metadata·names/styles 관계를 개별 part hash로 검사했다. 원본 기존 오류는 유지됐고 새 저장 오류 0이다. 이는 원본 보존 증거이며 입력 후 계산 정확성·신규 오류 발생 여부의 평가가 아니다.
- 현재 상태 분리: **입력 Pass·원본 보존 Pass·기존 슬롯/metadata 유지 Pass·native 재개방 Pass**, **계산·Excel 계산 정답 해당 없음(PRESERVE)**. 초기 실행 `result.json`의 PENDING은 결정/재개방 전 이력이며 아래 사용자 후속 결정과 같은 hash의 native 근거를 적용한다. V:AJ/다른 행 합계·CN/CT를 평가하지 않았고 golden을 입력/출력 생성에 쓰지 않았다. 기존 F-T-01/02 예외의 확대가 아니라 F-T-04에 대한 명시적 결정이다.
- 산출물/재현: [실행 안내](../../temp/input-02a-engine/2026-10-13-f-t-04-slot-sh1g281l/README.md), [새 XLSM](../../temp/input-02a-engine/2026-10-13-f-t-04-slot-sh1g281l/F-T-04-TRADE-SLOT704-INPUT-PRESERVED.xlsm), `case.json`·`result.json`·`preservation.json`·`request.json`·환경/도구 provenance·로그·보호 hash에 근거를 남긴다. 저장 후 재읽기·ZIP CRC·모든 XML/rels parse 및 원본/대장/기존 자료 409경로 불변 Pass. 복사 도구의 self-closing 빈 셀 matcher가 인접 셀까지 잡는 오류를 저장 전 발견해 로컬 복사본만 최소 수정했다. 최초 도구/로그는 보존했고 원본/기존 도구·결과는 변경하지 않았다. Java/JUnit 테스트·POI 평가·매크로/링크/connection/pivot 새로고침·외부 workbook 개방은 미수행이다.

#### F-T-04 전체 계산 제외·원본 보존 후속 — 2026-10-13

- 정책 정본: [domain 규칙 59·Q5/H1/H4](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md). 사용자는 지정 F-T-04에서 Q704:U704 입력만 반영하고 V704:AJ704 수식·저장값 및 다른 행·A를 원본 보존하도록 결정했다. 해당 사례는 전체 계산 제외(PRESERVE)이며 계산 및 동일 조건 Excel 계산 정답 대기는 해소했다. 다른 사례/일반 신규 계약의 H1/H4 정책을 바꾸지 않는다.
- 재검증 Pass: 기존 독립 XLSM을 실제 재읽어 승인 5입력의 값/타입/기존 서식 유지, V704:AJ704 15수식/빈 문자열 저장값·나머지 222,158셀의 원본 수식/저장값/타입/서식 및 다른 행·A/UNIQUE·CN/CT 보존을 확인했다. 셀별 비교와 validation·행/열/참조/metadata 개별 검사, 승인 5셀만 제외한 sheet XML 동일 및 나머지 76/77 part 동일을 함께 사용했다. 실제 hidden 5시트·VBA·externalLink/캐시·connection/queryTable·Table·names 27개·styles/calcChain/dynamic metadata 보존, 미존재 pivot/캐시는 해당 없음이다. ZIP CRC·71 XML/rels parse 및 원본/대장/기존 자료 520경로 hash 불변 Pass. 새 저장 오류 0은 보존 결과이며 계산을 수행한 판정이 아니다.
- 현재 사례 결과: 입력/보존 **Pass**, 계산/Excel 계산 정답 **해당 없음**, native **Pass**. 기존 6파일 native 후속의 F-T-04 20선정 셀 읽기 결과와 현재 XLSM hash 일치를 재확인했으며 이번에 native를 반복하거나 XLSM을 수정하지 않았다. 전체 INPUT-02A는 In Progress다.
- 근거/재현: [후속 결과](../../temp/input-02a-engine/2026-10-13-f-t-04-no-calc-rszceym0/policy-result.json), [보존 검사](../../temp/input-02a-engine/2026-10-13-f-t-04-no-calc-rszceym0/preservation.json), [재현 안내](../../temp/input-02a-engine/2026-10-13-f-t-04-no-calc-rszceym0/README.md). 기존 도구 3개를 새 temp 경로에 복사하고 결정/읽기 전용 검증 wrapper만 추가했다. 기존 실행/golden/native 기록은 수정하지 않고 현재 상태만 후속으로 연결한다.

#### 독립 재현 절차

아래 명령은 당시 표본 생성의 기술 재현용이다. 이전 도구는 후속 C2 빈 값 금지 정책을 반영하지 않아 F-O-03 빈 파일도 생성하므로 현재 승인 입력 실행 절차로 사용하지 않는다. 현재 F-O-03은 위 후속의 CLEAR 거부 기대값을 따른다.

1. `handoff-hashes.json`과 source 네 hash를 확인한다. 로컬 `prepare_cases.py`는 위 디렉터리에서 유지하고 원본·이전 출력은 수정하지 않는다.
2. 아래 명령의 출력 위치를 **존재하지 않는** `temp/` 하위 경로로 지정한다. 스크립트는 새 디렉터리 생성, source hash 검증, typed SET/CLEAR, ZIP 검사를 수행하며 이미 있는 경로에는 실패한다.

```bash
/Users/hoduhomil/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 -B \
  temp/input-02a-fixtures/2026-10-08-prep-jn6vfttf/prepare_cases.py \
  --output temp/input-02a-fixtures/reproduce-<새-ID>
```

3. 생성 `cases.json`의 실제 값·타입과 위 10파일 hash를 비교한다. XLSM을 일반 XLSX writer로 재저장하지 않고 ZIP 내부의 허용 입력 셀만 바꾸므로 shared/array 수식·캐시·VBA 등은 유지된다. 재계산·Excel 실행·외부 링크 갱신·매크로는 수행하지 않는다.
4. 이 재현 일치는 복사본 생성의 재현성만 증명한다. 같은 엔진 계산 일치나 Excel 정답의 대체 근거가 아니다.

#### Excel 비교값 확보·제출 절차

2026-10-08 초기 표본 준비 당시에는 Excel 비교본이 없었다. 2026-10-12 F-D-02·F-S-01·02·03의 같은 source/변경 조건·고정 입력/LUT·Excel 버전과 저장 설정·전체 9,758셀 비교값을 확보했다. F-D-02 최초 미갱신과 후속 저장본 확인·근거/한계는 위 제출 결과를 따른다. 나머지 사례의 현재 저장값, 특히 manual mode인 SUPPLY/TRADE의 캐시를 새 정답으로 사용하지 않는다. 각 사례는 위 제출 양식에 따라 다음 절차를 수행한다.

1. Excel **버전·build·OS·bitness·담당자·시각**을 기록한다. 파일을 열기 **전에** Application 계산을 manual로 두고 매크로 비활성·외부 링크 UpdateLinks=0·자동 connection/pivot 새로고침 금지를 설정한다. source의 `calcPr`/iteration/precision 설정과 실제 실행 설정도 기록한다. 외부 workbook 원본은 요청하거나 열지 않는다.
2. 현재 승인된 사례만 source의 새 복사본에 적용한다. `cases.json`의 셀별 전후 값·타입 또는 `NOT-CALCULATED` 복사본을 최신 domain 정책과 대조한다. **F-O-03 C2 CLEAR는 허용 입력 실행·Excel 정답 제출에서 제외한다.** 기존 빈 파일은 과거 기술 기록이며 CLEAR 거부 검증은 제품 경로에서 별도로 수행해야 한다.
3. 입력과 source의 A/외부 기준값·보존 오류가 유지된 상태에서 **승인 범위만** Excel로 재계산한다. Excel 담당자가 범위 계산 기능(예: Excel 자동화의 `Range.Calculate`, VBA 매크로 실행과 별개)을 사용한다. `F9`/전체 workbook 계산·RefreshAll로 A를 갱신한 결과는 이 조건의 정답이 아니다. 범위 계산 방법을 확보하지 못했거나 source 기준값이 바뀌면 결과를 PENDING/실패로 남긴다.
4. F-D-02는 E:N의 13합계 행 `4,5,10,15,34,35,43,64,76,89,100,104,109`만 계산하고 E5→E4→E109의 연결과 전체 130셀을 제출한다. F-S-01은 `DN11:DV382`·`H387:DK444`의 승인 합계를 계산하고 DN11/H435 및 DO11/I435를 비교한다. F-S-02/03은 `G8:G11`·`G16:G19`만 고정 캐시로 계산한다. 현재 BCHurricanes의 수식이 정확히 이 8셀뿐임을 확인한 경우에는 해당 시트 계산을 사용할 수 있다. Excel 밖에서 입력을 바꾼 표본이 갱신되지 않으면 Excel 안에서 승인 입력을 다른 유효 값으로 변경 후 사례 값으로 복원해 변경을 인식시킨다. 최종 입력·계산값을 대조하고 나서 저장한다. LUT 캐시 누락이나 Excel 오류는 성공 처리하지 않는다.
5. F-D-01은 확정 입력·보존을 확인할 수 있지만 미확정 계산을 실행하지 않는다. F-O-01은 B8 외 추가 갱신 없음이 확정됐으므로 입력/원본 보존·재개방을 확인하고 수식을 계산하지 않는다. F-T-01의 AG/AI 및 F-T-02의 source #N/A 셀도 계산하지 않고 source 수식·저장값을 확인한다. 지정 F-T-04는 Q704:U704 입력만 반영하고 수식 계산 없이 나머지 전체를 source 보존하므로 계산 정답 제출 대상이 아니다. F-T-02의 비오류 셀 등 나머지는 H1의 선행 계산/기준값·오류 처리 확정 뒤 승인 범위의 정답을 확보한다. AG5/AI5만 단독 실행하거나 다른 행 합계를 빼고 성공 판정하지 않는다. F-O-02는 수식 계산이 해당 없으며 숫자 값·타입·재개방 보존을 확인한다. F-O-03은 C2 CLEAR 거부 기대값이며 계산 정답 제출 대상이 아니다.
6. **새 이름**의 결과 XLSM, hash, 실제 계산한 셀/순서·설정, 전체 비교 셀의 `value/type/errorCode`를 제출 양식에 기록한다. 숫자는 표시 문자열이 아닌 원래 수치로 제출하고 `abs(a-b) <= max(1e-9, 1e-9 × max(abs(a), abs(b)))`로 비교한다. 문자열·boolean·date serial·blank·수식의 빈 문자열·오류 종류는 정확히 구분한다. source 보존용 오류와 승인 계산 셀의 오류를 따로 표시한다.
7. Excel 저장으로 package 표현이 달라질 수 있으므로 **Excel 정답은 계산 비교용**, **변경 전 source는 보존 비교용**으로 유지한다. 계산 입력으로 쓰인 A 캐시의 의미·값이 source와 같았는지 확인한다. 후보 엔진 결과나 동일 엔진 재계산 일치를 Excel 정답으로 제출하지 않는다. 준비 완료·Excel 정답 확보·실제 엔진 검증을 양식의 별도 상태로 갱신한다.

#### 원본 보존 검사 연결

`preservation-baseline.json`의 실제 존재 part·hidden 상태·defined names·시트 metadata와 source part hash를 사용한다. 표본 준비에서는 모든 입력 밖 bytes 동일 검사를 통과했지만, 이후 계산/구조 실행에서는 아래 허용 차이를 적용한다.

| 검사 / 연결 사례 | 변경 허용 | 보존·오류 판정 |
| --- | --- | --- |
| 셀 값·타입·style / 당시 10사례 | `cases.json`의 변경만 기술 비교; 현재 F-O-03은 허용 입력에서 제외 | 다른 셀의 값·수식 원문/shared·array 속성·저장값·타입·서식. 당시 CLEAR의 style 보존은 업무상 빈 값 허용 근거가 아님. F-O-01 P8은 B8 변경에도 source 캐시 유지 |
| 승인 계산 / F-D-02·F-S-01~03 | 승인 계산 셀의 결과·타입/오류만 Excel 기준으로 비교 | 수식 원문과 계산 밖 캐시 불변. F-T-01/02·F-O-01의 미확정 계산을 허용 차이에 포함하지 않음 |
| A·hidden / F-D-01·F-T-01/02·F-O-01 및 공통 | 현재 준비 사례에는 없음 | DEMAND Helper·AE:AF, TRADE T-134~138/배열·backup/Industry, OUTPUT A 12표. 네 역할의 실제 hidden/veryHidden 집합·순서·상태·worksheet part 유지 |
| VBA·externalLink/캐시·connection / 공통 | 입력 문자열의 저장 표현 외 package 변경 없음 | VBA/connection은 TRADE, externalLink는 OUTPUT/SUPPLY/TRADE에 존재. SUPPLY 고정 LUT 캐시는 계산 입력이지만 불변. TRADE 동명 외부 sheet를 현재 sheet에 자동 연결하지 않음 |
| pivot/캐시·names·merge·validation·Table·metadata / 공통 | 구조 사례가 확정되면 해당 사례의 기대 좌표/참조/범위 변화만 허용 | SUPPLY pivot/캐시, TRADE native Table·dynamic metadata, 실제 names/merge/validation·style/관계를 원본과 비교. LNG C1 `Dynamaic,Fixed` 원본 validation은 이번에 수정하지 않음 |
| 기존 오류·새 오류 / 공통 | 보존 결정이 있는 source 오류만 expected preserved error | TRADE Industry A3 등의 A 오류는 동일 보존. AG/AI의 source #N/A는 현재 golden/허용 오류가 아님. 승인 계산·신규 셀에서 새 오류나 오류 범위 확대는 실패; 임의 0/공백 대체 금지 |
| 구조 / PENDING 6건의 추가·삽입·삭제 묶음 | 명세 확정 후 신규 셀·참조 이동·범위/metadata를 사례별 기록 | 같은 시트의 승인 변경과 A 보존을 따로 검사. worksheet bytes·전체 셀/수식/표/관계 개수가 항상 같아야 한다는 검사는 사용하지 않음 |

네 사례의 POI 선택 계산·저장 후 재읽기·새 수식 오류/보존 검사와 2026-10-13 원산출물 native 재개방/동일 hash 읽기 전용 복사본의 값 유지 검사는 위 실행 결과를 따른다. 다른 후보/계산 경로·구조 변경 및 native 재계산/저장 cycle·모델 실행은 미수행이다. 자료별 검증 결과·남은 실행은 [stable work note](../work-notes/2026/2026-09-03-model-input-transfer-format.md)에 기록한다.

### 이전 테스트의 현재 사용 여부와 남은 검증

- TC-SRC-001~051과 이전 family 합계는 source/표 경계 재선정 전 실행하지 않는다. DemandOutputOverwrite의 TC-SRC-003/004는 K/S golden 비교에서 V-DEMAND-OVERWRITE 원본 보존으로 전환한다.
- TC-BACKUP-001의 397셀 무조건 golden 비교는 V-BACKUP의 기존 오류 보존/나머지 계산 범위 구분으로 대체한다. hidden 대상의 TC-FAM/TC-XLFN 배열 계산도 원본 보존이 우선이다.
- TC-PKG-001의 3파일·전체 개수 불변 검사는 V-PACKAGE의 4파일·승인 구조 변화/그 밖 보존 검사로 대체한다. TC-FAIL-003은 보존하기로 한 기존 오류를 새 계산 실패와 구분한다.
- 2026-10-08 확인: TC-XLFN-001의 활성 UNIQUE 2셀 재계산 가정은 T-137/T-138 A 원본 복제로 대체한다. 현재 UNIQUE 5셀 모두 보존 검사이며, 과거 테스트를 이유로 UNIQUE 구현/대체 엔진을 필수화하지 않는다.
- 정적 스캔 스크립트 `scripts/inventory-model-input-formulas.py`의 현재 category 기반 oracle는 이 표별 최신 정책을 소비하지 않는다. 이번에 코드/JSON ledger는 바꾸지 않았다. 기존 산출물을 현재 allowlist로 자동 실행하지 않는다.
- 현재 선정 허용19파일에는 원본 정규화 hash·승인 계산/Excel 정답·구조/보존·native 대기가 없다. 티켓의 현재 판정은 위 「INPUT-02A 최종 조건부 완료」를 따른다. 지정 사례 밖 계산 allowlist·신규 셀/연결/삭제 계약은 지원 범위 확대 때 필요한 항목만 확인하며, API 전체 저장 응답·모델 E2E는 INPUT-10 후속이다. 외부 workbook 원본 반입을 선행조건으로 추가하지 않는다.

## 이전 3파일 실측·검증 계획 — 현재 실행 기준 아님

다음은 INPUT-02B의 이전 실측과 당시 검증 계획이다. 원본 hash·65개 시트·60개 표·335 family와 과거 테스트 ID를 추적하기 위해 보존한다. 현재의 계산·구조·보존 판정에는 위 최신 검증 기준을 사용한다.

### 당시 결론


- 원본 3개, 65개 시트, INPUT-01의 60개 logical table을 모두 조사했다. 분류는 화면 대상 23, 참조·계산 전용 19, 빈 시트 10, 제외 후보 13으로 INPUT-01과 같다.
- 수식 1,862,877개는 모두 formula type과 family에 포함됐고 모두 cached result를 가진다. 수식 family는 좌표 차이만 정규화한 hash 기준 335개다.
- 외부 workbook 참조 수식은 42,394개로 INPUT-01 기준과 같다. `SUPPLY` 35,099개, `TRADE` 7,295개다.
- 2026-09-17 정책 결정에 따라 이 42,394개는 Backend 재계산 대상에서 제외한다. source snapshot의 formula·cached value·externalLink/package를 그대로 보존하며 외부 원본 파일을 계산 선행조건으로 요구하지 않는다.
- SUPPLY·TRADE는 현재도 외부 Excel 원본 없이 입력 파일을 사용하고 앞으로도 동일하게 운영한다. 따라서 업무담당자에게 외부 파일을 요청하거나 외부 snapshot/version을 확보하는 후속 작업은 만들지 않는다.
- 외부 참조를 제외한 1차 계산 후보 pool은 1,820,483개, 315개 family다. 여기서 catalog상 backup/제외 영역 317,422개, 61개 family를 추가로 보존하면 현재 Excel golden 검증 대상은 1,503,061개, 254개 family다. 외부 참조 42,394개는 20개 family이며 모두 `PRESERVATION_ONLY`다.
- `DEMAND`는 내부 cross-sheet edge를 정적으로 확인할 수 있다. `TRADE`의 주요 연결은 OOXML상 외부 workbook link이므로 같은 이름의 현재 workbook 시트와 자동으로 동일시하면 안 된다.
- `SUPPLY`, `TRADE`는 `calcMode=manual`이다. 저장 후 파일을 열었다는 사실만으로 재계산 완료를 뜻하지 않는다.
- `trade.xlsm`만 VBA binary와 connection 11개를 가진다. Java 계산만 통과해도 파일 보존과 Excel 실행 결과를 별도로 검증해야 한다.
- 상용 엔진 도입 여부는 아직 결정할 수 없다. 외부 참조를 제외한 계산 후보만 아래 동적 PoC에서 Excel golden file과 비교한다.

### 조사 기준과 안전 경계

- 원본 XLSM은 ZIP/OOXML을 streaming 방식으로 읽기만 했다.
- 수식 원문과 셀 값은 보고서, 로그, JSON에 출력하지 않았다. family는 `ff-<16자리 hash>`와 구조 정보만 출력한다.
- family hash는 문자열·외부 workbook 식별 차이는 유지하고 A1 cell/range 좌표 차이만 제거한다. shared formula follower는 master의 `si`와 family를 따라 집계한다.
- `same-sheet`는 수식에 자격 없는 cell 참조가 있거나 현재 sheet가 명시적으로 참조된 경우다. `cross-sheet`는 내부 sheet 참조만 뜻한다. `[workbook]sheet!cell`은 별도 external 경로다.
- 정적 dependency는 영향 범위와 테스트 대상을 고르는 metadata다. 실제 재계산 동작이나 값 정답을 증명하지 않는다.
- 계산 허용 수식의 기존 cached value는 변경 전 값이므로 입력 변경 후 정답으로 사용하지 않는다. 계산 제외 외부 참조 셀의 cached value는 정답 판정값이 아니라 source snapshot에서 그대로 보존할 기준값이다.

### Workbook 요약

| Role | 시트 분류 | 수식 / cached | type | family | 외부 보존 | 외부 제외 후 후보 | 계산/package 특성 |
| --- | --- | ---: | --- | ---: | ---: | ---: | --- |
| DEMAND | 화면 9 / 참조 11 / 빈 2 / 제외 3 | 699,298 / 699,298 | normal 132,913; shared 566,385 | 96 | 0 | 699,298 | calcId 191029; calcChain Y; defined names 37(workbook 8/sheet 29); merge 3; validation 7; OOXML table 0; VBA N |
| SUPPLY | 화면 6 / 참조 6 / 빈 8 / 제외 3 | 460,803 / 460,803 | normal 186,371; shared 274,431; array 1 | 60 | 35,099 | 425,704 | manual; calcId 191028; calcChain Y; externalLink 1; names 12(4/8); merge 1; validation 3; table 0; VBA N |
| TRADE | 화면 8 / 참조 2 / 빈 0 / 제외 7 | 702,776 / 702,776 | normal 104,682; shared 598,089; array 5 | 179 | 7,295 | 695,481 | manual; calcId 191029; calcChain Y; externalLink 2; connections 11; names 27(11/16); merge 7; validation 3; table 10; VBA Y |
| 합계 | 화면 23 / 참조 19 / 빈 10 / 제외 13 | 1,862,877 / 1,862,877 | normal 423,966; shared 1,438,905; array 6; dataTable 0 | 335 | 42,394 | 1,820,483 | 원본 hash 불변 |

Volatile 함수는 발견되지 않았다. 버전 의존 함수 후보는 `_XLFN.UNIQUE` 5회(`MidstreamContractInput` 2, backup 2, `Industry research` 1)다. 표준 함수 목록에 없는 호출 후보는 `GDP` 1회와 `PIPELINE` 6회다. 미지원으로 단정하지 않고 동적 PoC blocker로 둔다.

Defined name을 직접 사용하는 수식은 SUPPLY `LiqProdAdjustment_backup` 34,452개에서 확인됐다(`Bcm_to_Mt_LNG`; 같은 수식이 `#REF!`도 포함한다). DEMAND `BCInput`은 이전 집계에서 3,152개로 적었으나 오탐이다. 집계 스크립트가 `$AD:$AD` 열 참조의 `AD`를 이름 `ad`로 잘못 센 것이며, 3,152개 전부가 `$AD:$AD`를 포함한 `SUMIFS`이고 이름을 실제로 쓰는 수식은 0개다. 2026-10-02에 스크립트가 열 범위를 이름 후보에서 제외하도록 고쳤고, 수정 전후 JSON에서 바뀐 값은 `BCInput`의 이 항목(3,152→0) 하나뿐이다. SUPPLY 34,452개는 실제 이름 `Bcm_to_Mt_LNG` 사용임을 확인했고 수정 후에도 같다. 60개 logical table 경계 안에서는 직접 사용이 0개다. name 원문은 출력하지 않고 workbook/sheet scope 합계와 사용 name hash만 inventory에 남긴다.

### 내부·외부 의존성

내부 direct edge는 다음 9개다. 화살표 오른쪽 수식이 왼쪽 sheet를 참조한다.

| Role | source → target | 참조 수식 셀 |
| --- | --- | ---: |
| DEMAND | `PMIAdjustment` → `PMIInput` | 8,155 |
| DEMAND | `General` → `WeatherLogicInput` | 1,008 |
| DEMAND | `WeatherAdjustment` → `WeatherInput` | 32,256 |
| DEMAND | `General` → `WeatherInput` | 2,016 |
| DEMAND | `General` → `LNGBunkeringInput` | 144 |
| DEMAND | `HydrogenAdjustment` → `HydrogenInput` | 728 |
| SUPPLY | `LiqUtilAdjustment_backup` → `LiqProdAdjustment` | 397 |
| SUPPLY | `BCRegasification` → `RegasAdjustment` | 33,108 |
| SUPPLY | `LiqUtilAdjustment_backup` → `LiqProdAdjustment_backup` | 348 |

확인 가능한 transitive chain은 현재 direct edge 집합 안에서는 더 이어지지 않는다. 외부 model/server와 workbook connection 이후 경로는 영향 metadata로만 유지하고 Backend 계산 경로에서는 따라가지 않는다.

| Role / sheet | 외부 참조 수식 | 외부 sheet 식별자 | 처리 |
| --- | ---: | --- | --- |
| SUPPLY / `BCHurricanes` | 8 | `LUT` | 계산 제외; formula/cached/link 보존 |
| SUPPLY / `BCLiquefaction_backup` | 35,091 | sheet 식별 불가 | 계산 제외; 원본 package 보존 |
| TRADE / `MidstreamCapacityInput` | 5,167 | `Mapping`, `MidstreamContractInput` | 계산 제외; 동명 내부 sheet와 동일시 금지 |
| TRADE / `UpstreamAdjLogicInput` | 1,344 | `0. Contracted Volume` | 계산 제외; source cached value 보존 |
| TRADE / `CostInput` | 517 | `MidstreamCapacityInput` | 계산 제외; link 비갱신 |
| TRADE / `4.5 PriceModelInput` | 267 | `0. Contracted Volume`, `ShippingCostInput`, `CostInput`, `3.1 Exporter Demand Adjustment`, `4.1 OverallBalanceOutput` | 전 셀 계산 제외; formula/cached/link 보존 |

### 65개 시트 계산 matrix

`수식/cached`가 같으므로 cached 누락은 없다. `내부 입력 sheet`는 현재 sheet의 수식이 참조하는 내부 sheet다. 함수는 이름과 호출 cell 수다.

| Role | # | Sheet | INPUT-01 분류 | 수식/cached | type | family | 외부 | 내부 입력 sheet | 함수(count) | 위험 / 판정 |
| --- | ---: | --- | --- | ---: | --- | ---: | ---: | --- | --- | --- |
| DEMAND | 1 | `Input Adjustment ->` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| DEMAND | 2 | `GDPPopulationAdjustment` | 화면 대상 | 12001/12001 | normal=6024, shared=5977 | 8 | 0 | - | IF=16032, AND=11966, MATCH=7956, INDEX=5967, ISBLANK=55, COUNTIF=2, GDP=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 3 | `ElectrificationRateAdjustment` | 화면 대상 | 6003/6003 | normal=2020, shared=3983 | 5 | 0 | - | IF=2083, AND=2021, INDEX=1989, MATCH=1989, ISBLANK=30, COUNTIF=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 4 | `PowerMixAdjustment` | 화면 대상 | 39906/39906 | normal=15985, shared=23921 | 5 | 0 | - | MATCH=31908, IF=16734, AND=16204, INDEX=15954, ISBLANK=30, COUNTIF=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 5 | `CoalGasAdjustment` | 화면 대상 | 11995/11995 | normal=4031, shared=7964 | 5 | 0 | - | IF=8200, INDEX=7956, MATCH=7956, AND=4042, TEXT=66, ISBLANK=50, COUNTIF=2 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 6 | `WeatherAdjustment` | 화면 대상 | 79984/79984 | normal=16094, shared=63890 | 9 | 0 | - | MATCH=47918, IF=24579, AND=24464, INDEX=23959, ISBLANK=115, COUNTIF=2 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 7 | `PMIAdjustment` | 화면 대상 | 32164/32164 | normal=8108, shared=24056 | 9 | 0 | - | MATCH=16044, IF=8487, AND=8105, INDEX=8069, ISBLANK=35, COUNTIF=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 8 | `HydrogenAdjustment` | 화면 대상 | 23998/23998 | normal=8015, shared=15983 | 5 | 0 | - | IF=8139, AND=8114, INDEX=7989, MATCH=7989, ISBLANK=25, COUNTIF=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 9 | `LNGBunkeringAdjustment` | 화면 대상 | 319/319 | normal=137, shared=182 | 5 | 0 | - | IF=140, AND=88, INDEX=86, MATCH=86, ISBLANK=50, COUNTIF=2 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 10 | `Scenario Tracker` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| DEMAND | 11 | `DemandOutputOverwrite` | 화면 대상 | 1000/1000 | normal=1, shared=999 | 2 | 0 | - | - | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 12 | `기준정보 ->` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| DEMAND | 13 | `BCInput` | 참조·계산 전용 | 304766/304766 | shared=304762, normal=4 | 4 | 0 | - | SUMIFS=3152 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 14 | `MacroParametersLogicInput` | 참조·계산 전용 | 168/168 | shared=168 | 1 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 15 | `PMIInput` | 참조·계산 전용 | 25878/25878 | normal=8156, shared=17722 | 4 | 0 | PMIAdjustment | MATCH=8357, INDEX=8256, IFERROR=8155 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 16 | `PMILogicInput` | 참조·계산 전용 | 84/84 | shared=84 | 1 | 0 | - | SUMIFS=2 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 17 | `WeatherLogicInput` | 참조·계산 전용 | 1176/1176 | normal=1008, shared=168 | 2 | 0 | General | INDEX=1008, MATCH=1008 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 18 | `WeatherInput` | 참조·계산 전용 | 144760/144760 | normal=62337, shared=82423 | 14 | 0 | General, WeatherAdjustment | MATCH=67548, INDEX=67038, IFERROR=64512, IF=40229, ISNUMBER=6074 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 19 | `LNGBunkeringInput` | 참조·계산 전용 | 527/527 | normal=246, shared=281 | 6 | 0 | General | IFERROR=96, INDEX=96, MATCH=96 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 20 | `LNGBunkeringLogicInput` | 참조·계산 전용 | 0/0 | - | 0 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 21 | `HydrogenInput` | 참조·계산 전용 | 14437/14437 | normal=732, shared=13705 | 6 | 0 | HydrogenAdjustment | IFERROR=728, INDEX=728, MATCH=728 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 22 | `LNG Demand` | 참조·계산 전용 | 130/130 | normal=13, shared=117 | 4 | 0 | - | SUM=26 | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 23 | `General` | 참조·계산 전용 | 2/2 | normal=2 | 1 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | 24 | `Sheet2_DISCARDAFTUSE` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| DEMAND | 25 | `CubeDump_DISCARDAFTUSE` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| SUPPLY | 1 | `Input Upstream ->` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 2 | `ProductionAdjustment` | 화면 대상 | 1374/1374 | normal=603, shared=771 | 10 | 0 | - | IF=790, AND=758, SUMIFS=381, ISBLANK=25, COUNTIFS=2 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 3 | `Input Midstream ->` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 4 | `{USA}Hurricanes` | 화면 대상 | 209/209 | normal=23, shared=186 | 3 | 0 | - | IF=25, ISBLANK=20, AND=5, COUNTIFS=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 5 | `LiqUtilAdjustment` | 화면 대상 | 83/83 | normal=1, shared=82 | 1 | 0 | - | IF=3 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 6 | `LiqProdAdjustment` | 화면 대상 | 480/480 | normal=398, shared=82 | 2 | 0 | LiqUtilAdjustment_backup | IF=798, COUNTBLANK=397, ISBLANK=397 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 7 | `RegasAdjustment` | 화면 대상 | 43464/43464 | normal=33482, shared=9982 | 5 | 0 | BCRegasification | MATCH=62496, INDEX=31248, VLOOKUP=372, SUMIFS=85 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 8 | `PipelineAdjustment` | 화면 대상 | 788/788 | normal=404, shared=384 | 10 | 0 | - | IFERROR=401, VLOOKUP=386, INDEX=15, MATCH=15, IF=4, AND=4, ISBLANK=4, COUNTIFS=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 9 | `BCProduction` | 참조·계산 전용 | 74378/74378 | shared=74367, normal=11 | 6 | 0 | - | IF=776, LEFT=391, LEN=391 | P1 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 10 | `BCHurricanes` | 참조·계산 전용 | 8/8 | normal=8 | 1 | 8 | - | VLOOKUP=16 | P0 / PRESERVATION_ONLY |
| SUPPLY | 11 | `BCLiquefaction` | 참조·계산 전용 | 104808/104808 | normal=2, shared=104806 | 2 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 12 | `BCRegasification` | 참조·계산 전용 | 754/754 | normal=4, shared=750 | 2 | 0 | - | MAX=6 | P1 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 13 | `BCPipeline` | 참조·계산 전용 | 0/0 | - | 0 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 14 | `LNG Summary` | 참조·계산 전용 | 0/0 | - | 0 | 0 | - | - | P1 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | 15 | `LiqUtilAdjustment_backup` | 제외 후보 | 47279/47279 | normal=47074, shared=205 | 4 | 0 | - | IFERROR=47071, INDEX=47071, MATCH=47071, IF=10 | P1 / PRESERVATION_ONLY |
| SUPPLY | 16 | `LiqProdAdjustment_backup` | 제외 후보 | 69449/69449 | normal=69252, shared=196, array=1 | 5 | 0 | LiqUtilAdjustment_backup | IFERROR=68904, INDEX=68904, MATCH=68904, IF=700, COUNTBLANK=348, ISBLANK=348, COUNTIFS=1 | P1 / PRESERVATION_ONLY |
| SUPPLY | 17 | `BCLiquefaction_backup` | 제외 후보 | 117729/117729 | normal=35109, shared=82620 | 9 | 35091 | - | SUMIFS=35091 | P0 / PRESERVATION_ONLY |
| SUPPLY | 18 | `{USA}RigsLogic` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 19 | `{China}PolicyLogic` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 20 | `{USA}HurricanesLogic` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 21 | `RegasLogic` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 22 | `PipelineLogic` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| SUPPLY | 23 | `MonthlyAdjProduction` | 빈 시트 | 0/0 | - | 0 | 0 | - | - | P2 / NOT_APPLICABLE |
| TRADE | 1 | `LNG_TradeRestrictionInput` | 화면 대상 | 0/0 | - | 0 | 0 | - | - | P0 / PRESERVATION_ONLY |
| TRADE | 2 | `PNG_TradeRestrictionInput` | 화면 대상 | 0/0 | - | 0 | 0 | - | - | P0 / PRESERVATION_ONLY |
| TRADE | 3 | `Sheet1` | 제외 후보 | 88/88 | normal=47, shared=41 | 6 | 0 | - | - | P1 / PRESERVATION_ONLY |
| TRADE | 4 | `MidstreamContractInput` | 참조·계산 전용 | 166271/166271 | array=2, normal=55513, shared=110756 | 37 | 0 | - | MATCH=50961, IF=49470, INDEX=39559, IFERROR=32774, ISBLANK=24465, SUMIFS=22498, OR=1996, MIN=35, _XLFN.UNIQUE=2, CHOOSE=2 | P1 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 5 | `ShippingCostInput` | 화면 대상 | 342696/342696 | shared=342674, normal=22 | 38 | 0 | - | INDEX=7696, MATCH=7696, IF=4792, AND=1483, ISNUMBER=1210, SEARCH=1210, IFERROR=597, SUM=594, ISBLANK=10 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 6 | `MidstreamCapacityInput` | 화면 대상 | 62236/62236 | normal=5195, shared=57041 | 32 | 5167 | - | IF=5000, MATCH=2779, INDEX=2774, SUMIFS=2561, IFERROR=192, MIN=37, AND=25, AVERAGEIFS=21, MAX=13, PIPELINE=6, OR=4 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 7 | `UpstreamAdjLogicInput` | 화면 대상 | 3361/3361 | normal=2020, shared=1341 | 6 | 1344 | - | IF=1358, INDEX=1344, MATCH=1344, IFERROR=686, ISNUMBER=686, MAX=14, COUNTA=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 8 | `DemandAdjLogicInput` | 화면 대상 | 15/15 | normal=15 | 5 | 0 | - | MAX=8 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 9 | `CostInput` | 화면 대상 | 9871/9871 | shared=7687, normal=2184 | 7 | 517 | - | COUNTIFS=1551, IF=1141, ISNUMBER=517, MATCH=517, OR=107 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | 10 | `4.5 PriceModelInput` | 참조·계산 전용 | 267/267 | normal=267 | 7 | 267 | - | INDEX=400, MATCH=400, SUMIF=171, IFERROR=104 | P0 / PRESERVATION_ONLY |
| TRADE | 11 | `LNGExportRestictionInput` | 화면 대상 | 0/0 | - | 0 | 0 | - | - | P0 / PRESERVATION_ONLY |
| TRADE | 12 | `backupLNG_TradeRestrictionInput` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| TRADE | 13 | `backup_MidstreamContractInput` | 제외 후보 | 117970/117970 | array=2, normal=39419, shared=78549 | 40 | 0 | - | MATCH=28372, IF=27954, INDEX=23410, IFERROR=22910, SUMIFS=22294, ISBLANK=6271, OR=1996, MIN=35, _XLFN.UNIQUE=2, CHOOSE=2 | P1 / PRESERVATION_ONLY |
| TRADE | 14 | `Industry research` | 제외 후보 | 1/1 | array=1 | 1 | 0 | - | _XLFN.UNIQUE=1 | P1 / PRESERVATION_ONLY |
| TRADE | 15 | `net exporter` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| TRADE | 16 | `pipeline cap` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |
| TRADE | 17 | `pipeline contract` | 제외 후보 | 0/0 | - | 0 | 0 | - | - | P2 / PRESERVATION_ONLY |

### Logical table 계산 matrix

`편집(source)`의 좌표는 INPUT-01의 편집 필드 안에서 고른 동적 테스트 후보다. `BLOCKED`는 편집 정책이 확정되지 않아 임의 좌표를 고르지 않았다는 뜻이다. table 안의 수식이 0이어도 같은 sheet의 수식 또는 외부 link 경로가 있으면 Excel golden 검증 대상으로 유지했다.

| Role | Sheet / table | 경계 | 편집(source) | 수식/cached | type | family | 외부 | 함수(count) | 위험 / 판정 |
| --- | --- | --- | --- | ---: | --- | ---: | ---: | --- | --- |
| DEMAND | `CoalGasAdjustment` / coal | `B6:G2000` | Y (`E7`) | 5997/5997 | normal=2015, shared=3982 | 4 | 0 | IF=4100, INDEX=3978, MATCH=3978, AND=2021, TEXT=33, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `CoalGasAdjustment` / gas | `H6:M2000` | Y (`K7`) | 5997/5997 | normal=2015, shared=3982 | 4 | 0 | IF=4100, INDEX=3978, MATCH=3978, AND=2021, TEXT=33, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `DemandOutputOverwrite` / monthly | `M7:S507` | Y (`P8`) | 500/500 | shared=500 | 1 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `DemandOutputOverwrite` / sector | `D7:K507` | Y (`H8`) | 500/500 | normal=1, shared=499 | 1 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `ElectrificationRateAdjustment` / ElectrificationRateAdjustment | `B6:H2000` | Y (`F7`) | 6002/6002 | normal=2019, shared=3983 | 4 | 0 | IF=2083, AND=2021, INDEX=1989, MATCH=1989, ISBLANK=30 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `GDPPopulationAdjustment` / GDP | `B6:H2000` | Y (`F7`) | 6003/6003 | normal=4009, shared=1994 | 5 | 0 | IF=13954, AND=9945, MATCH=5967, INDEX=3978, ISBLANK=30, GDP=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `GDPPopulationAdjustment` / Population | `J6:O2000` | Y (`M7`) | 5997/5997 | normal=2014, shared=3983 | 4 | 0 | IF=2078, AND=2021, INDEX=1989, MATCH=1989, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `HydrogenAdjustment` / HydrogenAdjustment | `B6:G8000` | Y (`E7`) | 23997/23997 | normal=8014, shared=15983 | 4 | 0 | IF=8139, AND=8114, INDEX=7989, MATCH=7989, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `LNGBunkeringAdjustment` / fuel | `H6:M2000` | Y (`K7`) | 159/159 | normal=68, shared=91 | 4 | 0 | IF=70, AND=44, INDEX=43, MATCH=43, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `LNGBunkeringAdjustment` / vessel | `A6:G2000` | Y (`E7`) | 159/159 | normal=68, shared=91 | 4 | 0 | IF=70, AND=44, INDEX=43, MATCH=43, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `PMIAdjustment` / PMI | `B6:H8000` | Y (`F7`) | 23981/23981 | normal=8011, shared=15970 | 4 | 0 | MATCH=15950, IF=8262, AND=8100, INDEX=7975, ISBLANK=35 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `PMIAdjustment` / reference mapping | `J6:N100` | Y (`J7`) | 282/282 | normal=96, shared=186 | 4 | 0 | IF=102, INDEX=94, MATCH=94, AND=5 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `PowerMixAdjustment` / annual | `B6:H7989` | Y (`F7`) | 23955/23955 | normal=8003, shared=15952 | 4 | 0 | MATCH=15956, IF=8378, AND=8103, INDEX=7978, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `PowerMixAdjustment` / monthly | `J6:P7989` | Y (`O7`) | 15950/15950 | normal=7981, shared=7969 | 3 | 0 | MATCH=15952, IF=8356, AND=8101, INDEX=7976, ISBLANK=5 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `WeatherAdjustment` / country degree-day | `B6:I7996` | Y (`G7`) | 23995/23995 | normal=8020, shared=15975 | 4 | 0 | MATCH=31940, IF=16130, AND=16095, INDEX=15970, ISBLANK=35 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `WeatherAdjustment` / country temperature | `K6:Q7997` | Y (`O7`) | 23983/23983 | normal=8016, shared=15967 | 4 | 0 | MATCH=15978, IF=8142, AND=8117, INDEX=7989, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `WeatherAdjustment` / region degree-day | `T6:Z7996` | Y (`X7`) | 16005/16005 | normal=31, shared=15974 | 3 | 0 | IF=156, AND=126, ISBLANK=30 | P0 / EXCEL_GOLDEN_REQUIRED |
| DEMAND | `WeatherAdjustment` / region temperature | `AB6:AG7996` | Y (`AE7`) | 16000/16000 | normal=26, shared=15974 | 3 | 0 | IF=151, AND=126, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `LiqProdAdjustment` / LiqProdAdjustment | `B10:CM407` | Y (`G11`) | 397/397 | normal=397 | 1 | 0 | IF=794, COUNTBLANK=397, ISBLANK=397 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `LiqUtilAdjustment` / LiqUtilAdjustment | `B10:CN407` | Y (`G11`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `PipelineAdjustment` / PipelineAdjustment | `A7:I200` | Y (`H8`) | 787/787 | normal=403, shared=384 | 9 | 0 | IFERROR=401, VLOOKUP=386, INDEX=15, MATCH=15, IF=4, AND=4, ISBLANK=4 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `ProductionAdjustment` / annual | `A7:H200` | Y (`E8`) | 792/792 | normal=409, shared=383 | 6 | 0 | IF=777, AND=752, SUMIFS=188, ISBLANK=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `ProductionAdjustment` / monthly | `I7:O201` | Y (`M8`) | 581/581 | normal=193, shared=388 | 3 | 0 | SUMIFS=193, IF=13, AND=6 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `RegasAdjustment` / annual rollup | `DM10:DV382` | N | 3720/3720 | normal=2, shared=3718 | 2 | 0 | SUMIFS=19 | P0 / PRESERVATION_ONLY |
| SUPPLY | `RegasAdjustment` / country monthly rollup | `G385:DK444` | N | 6264/6264 | shared=6264 | 1 | 0 | SUMIFS=66 | P0 / PRESERVATION_ONLY |
| SUPPLY | `RegasAdjustment` / main | `B8:DK382` | Y (`H11`) | 33480/33480 | normal=33480 | 3 | 0 | MATCH=62496, INDEX=31248, VLOOKUP=372 | P0 / EXCEL_GOLDEN_REQUIRED |
| SUPPLY | `{USA}Hurricanes` / {USA}Hurricanes | `B7:E200` | Y (`D8`) | 208/208 | normal=22, shared=186 | 2 | 0 | IF=25, ISBLANK=20, AND=5 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `CostInput` / liquefaction cost | `H2:M233` | Y (`K3`) | 462/462 | shared=461, normal=1 | 2 | 0 | IF=5, OR=5 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `CostInput` / LNG upstream | `O2:T227` | Y (`R3`) | 450/450 | shared=448, normal=2 | 2 | 0 | IF=6, OR=6 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `CostInput` / non-LNG upstream | `V2:AA615` | Y (`Y3`) | 1226/1226 | normal=7, shared=1219 | 2 | 0 | IF=14, OR=14 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `CostInput` / pipeline cost | `AC2:AI1554` | Y (`AG3`) | 3104/3104 | shared=3104 | 2 | 0 | IF=25, OR=25 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `CostInput` / project SRMC | `AK2:AS519` | N | 2585/2585 | normal=2073, shared=512 | 5 | 517 | COUNTIFS=1551, IF=1034, ISNUMBER=517, MATCH=517 | P0 / PRESERVATION_ONLY |
| TRADE | `CostInput` / regas tariff | `A2:F379` | Y (`D3`) | 754/754 | shared=653, normal=101 | 2 | 0 | IF=57, OR=57 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `DemandAdjLogicInput` / markets | `A2:A40` | Y (`A3`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `DemandAdjLogicInput` / selector | `C2:C3` | Y (`C3`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `DemandAdjLogicInput` / thresholds | `E2:M7` | Y (`BLOCKED`) | 15/15 | normal=15 | 5 | 0 | MAX=8 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `LNGExportRestictionInput` / LNGExportRestictionInput | `B3:E13` | Y (`B4`) | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `LNG_TradeRestrictionInput` / LNG_TradeRestrictionInput | `B4:K509` | Y (`B6`) | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `MidstreamCapacityInput` / importer share | `FB3:FD8` | Y (`FD4`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `MidstreamCapacityInput` / liquefaction base | `U3:AE27` | N | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `MidstreamCapacityInput` / LNG production base | `BM3:BV92` | N | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `MidstreamCapacityInput` / pipeline disruption | `DD3:DH651` | Y (`DH4`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `MidstreamCapacityInput` / pipeline share | `DX3:EC186` | Y (`EB4`) | 183/183 | normal=1, shared=182 | 1 | 0 | MATCH=8, IF=4, OR=4, INDEX=4 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `MidstreamCapacityInput` / regas base | `AI3:AS61` | N | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `MidstreamCapacityInput` / regas disruption | `EE3:EI6267` | Y (`EI4`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `PNG_TradeRestrictionInput` / PNG_TradeRestrictionInput | `B4:J509` | Y (`B6`) | 0/0 | - | 0 | 0 | - | P0 / PRESERVATION_ONLY |
| TRADE | `ShippingCostInput` / Atlantic charter | `B15:G25` | Y (`D17`) | 24/24 | normal=2, shared=22 | 5 | 0 | IF=4, ISBLANK=2, IFERROR=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / fuel | `B26:G36` | Y (`D28`) | 16/16 | normal=1, shared=15 | 3 | 0 | IF=4, ISBLANK=2, IFERROR=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / lookup blocks | `AB2:AT375` | N | 1860/1860 | shared=1858, normal=2 | 5 | 0 | IF=26, ISNUMBER=26, SEARCH=26 | P0 / PRESERVATION_ONLY |
| TRADE | `ShippingCostInput` / Pacific charter | `B4:G14` | Y (`F6`) | 24/24 | normal=2, shared=22 | 5 | 0 | IF=4, ISBLANK=2, IFERROR=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / Panama crossing | `B48:G58` | Y (`D50`) | 16/16 | normal=1, shared=15 | 3 | 0 | IF=4, ISBLANK=2, IFERROR=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / Panama disruption | `B70:D79` | Y (`C72`) | 7/7 | shared=7 | 1 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / Panama spot | `B86:E93` | Y (`D88`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / shipping calculation | `I2:Y18929` | N | 321739/321739 | shared=321734, normal=5 | 17 | 0 | INDEX=7696, MATCH=7696, IF=4746, AND=1483, ISNUMBER=1184, SEARCH=1184, SUM=594, IFERROR=592 | P0 / PRESERVATION_ONLY |
| TRADE | `ShippingCostInput` / Suez crossing | `B37:G47` | Y (`D39`) | 16/16 | normal=1, shared=15 | 3 | 0 | IF=4, ISBLANK=2, IFERROR=1 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / Suez disruption | `B59:D68` | Y (`D61`) | 7/7 | shared=7 | 1 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `ShippingCostInput` / technical parameters | `B81:E83` | Y (`E83`) | 2/2 | normal=2 | 2 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `UpstreamAdjLogicInput` / country-year adjustment | `I2:R674` | Y (`P3`) | 3360/3360 | normal=2019, shared=1341 | 5 | 1344 | IF=1358, INDEX=1344, MATCH=1344, IFERROR=686, ISNUMBER=686, MAX=14 | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `UpstreamAdjLogicInput` / general | `A2:C5` | Y (`B3`) | 0/0 | - | 0 | 0 | - | P0 / EXCEL_GOLDEN_REQUIRED |
| TRADE | `UpstreamAdjLogicInput` / support | `T2:AZ674` | N | 1/1 | normal=1 | 1 | 0 | COUNTA=1 | P0 / PRESERVATION_ONLY |

### Formula family ledger 사용법

`scripts/inventory-model-input-formulas.py --include-families` 출력은 335개 family마다 다음 필드를 가진다.

- `familyId`, `formulaType`, `representativeCell`, `cellCount`, `cachedCellCount`, `calculationEligibleFormulaCells`, `calculationPolicy`
- `sameSheetReferenceCellCount`, `referencedSheets`, `externalReferencedSheets`
- `functions`, `volatileFunctions`, `extensionFunctionCandidates`, `customFunctionCandidates`
- `definedNameIds`(원문 대신 hash), `logicalTableKeys`, `downstreamTargets`, `risk`, `oracle`

각 sheet/table 레코드에도 `sameSheetReferenceFormulaCells`, `definedNameFormulaCells`, 내부 `dependencies`, `externalReferencedSheets`, `downstreamTargets`가 포함된다. workbook 레코드는 defined name scope, VBA, calcChain, externalLink, connection을 가진다. 따라서 Markdown 표의 폭을 줄여 생략한 항목도 같은 실행 결과에서 table 단위로 재현된다.

family cell 합계와 type cell 합계는 각각 1,862,877로 전체 수식 수와 일치한다. 이 가운데 외부 workbook만 참조하는 20개 family, 42,394셀은 `PRESERVE_EXTERNAL_ONLY`이고, 나머지 315개 family, 1,820,483셀은 외부 제외 후 후보 pool이다. 현재 oracle은 이 pool 중 254개 family, 1,503,061셀을 `EXCEL_GOLDEN_REQUIRED`로, backup/제외 영역 61개 family, 317,422셀을 추가 `PRESERVATION_ONLY`로 분류한다. P0 family 185개, P1 family 150개라는 위험도 분류는 그대로 유지하며 수식이 없는 P2 sheet에는 family가 없다. 이 ledger가 계산 후보 family의 동적 테스트와 보존 family의 회귀 테스트 parameter source다. 문서에는 수식 원문이나 정규화 문자열을 복제하지 않는다.

시트나 table에 `외부` 수가 있으면서 판정이 `EXCEL_GOLDEN_REQUIRED`인 경우에도 외부 참조 셀 자체를 계산한다는 뜻은 아니다. 같은 범위 안의 내부 계산 후보만 golden으로 검증하고, 외부 참조 셀은 source snapshot의 formula·cached value·link/package를 보존한다.

### 후속 동적 PoC 공통 규칙

#### 계산 시나리오 Given-When-Then

- Given: 원본 복사본, 아래 대표 source cell, Microsoft Excel에서 같은 변경을 적용해 계산 후보 수식을 재계산한 golden file이 있다. 외부 참조 셀은 원본 복사본의 cached value를 보존 기준으로 사용한다.
- When: 후보 엔진이 source에 `SET` 또는 `CLEAR`를 적용하고 계산 허용 범위만 재계산한 뒤 XLSM을 저장·재개방한다. 외부 파일은 열거나 refresh하지 않는다.
- Then: 지정 family의 대표 target과 그 family 전체가 Excel golden과 일치하고, 요청하지 않은 입력과 formula/package 구조는 보존된다.
- 성공 기준: 문자열·boolean·error type은 정확히 같고, 날짜 serial은 정확히 같고, 숫자는 `abs(a-b) <= 1e-9` 또는 `relative error <= 1e-9`다. family/type/cell 수, externalLink, defined name scope, VBA, connection, OOXML table, validation, merge, sheet state도 기준과 같다.
- 실패 영향: P0 차이는 해당 table의 엔진 적용과 1차 출시를 막는다. P1 차이는 관련 계산 경로의 호환성 주장과 GO를 막는다.

#### 보존 시나리오 Given-When-Then

- Given: 외부 참조 수식, 수식이 없는 편집 table 또는 read-only/hidden/backup/package part의 구조 hash와 count가 있다.
- When: source 값만 수정해 저장하고 다시 연다.
- Then: 입력 변경만 반영되고 외부 참조 수식의 formula·cached value 및 대상 sheet/table의 관계·VBA·연결·sheet state는 그대로다.
- 성공 기준: 허용된 worksheet cell 변경 외 package part hash/count가 같고 파일이 Excel에서 복구 경고 없이 열린다.
- 실패 영향: 계산값이 맞더라도 생성 XLSM 전달을 금지한다.

아래 target은 정적 분석으로 고른 대표 관측점이다. target이 실제로 source에 반응하는지는 동적 PoC에서 확인해야 하며, 반응하지 않으면 같은 family 안에서 영향 cell을 재선정한다. `CLEAR`는 각 table의 첫 `SET` 성공 후 동일 source에 추가 수행한다.

### 51개 편집 source table 테스트 목록

공통 선행조건은 “활성 행 또는 golden fixture 확보”다. 현재 literal 0행인 template은 기존 활성 행을 만든 golden fixture가 없으면 실행하지 않는다. 모든 `EXCEL_GOLDEN_REQUIRED` 행은 기존 cached value로 GO 판정하지 않는다.

| Test ID | source workbook / sheet / table | 변경 cell / 연산 | target workbook / sheet / cell | family | 판정 |
| --- | --- | --- | --- | --- | --- |
| TC-SRC-001 | DEMAND / `CoalGasAdjustment` / coal | `E7` / SET | DEMAND / `CoalGasAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-002 | DEMAND / `CoalGasAdjustment` / gas | `K7` / SET | DEMAND / `CoalGasAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-003 | DEMAND / `DemandOutputOverwrite` / monthly | `P8` / SET | DEMAND / `DemandOutputOverwrite` / `S8` | `ff-8687796f8c182606` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-004 | DEMAND / `DemandOutputOverwrite` / sector | `H8` / SET | DEMAND / `DemandOutputOverwrite` / `K8` | `ff-b7aed3ad0881b9b5` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-005 | DEMAND / `ElectrificationRateAdjustment` / ElectrificationRateAdjustment | `F7` / SET | DEMAND / `ElectrificationRateAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-006 | DEMAND / `GDPPopulationAdjustment` / GDP | `F7` / SET | DEMAND / `GDPPopulationAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-007 | DEMAND / `GDPPopulationAdjustment` / Population | `M7` / SET | DEMAND / `GDPPopulationAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-008 | DEMAND / `HydrogenAdjustment` / HydrogenAdjustment | `E7` / SET | DEMAND / `HydrogenInput` / `D9` | `ff-12804ae80e8b2aae` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-009 | DEMAND / `LNGBunkeringAdjustment` / fuel | `K7` / SET | DEMAND / `LNGBunkeringAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-010 | DEMAND / `LNGBunkeringAdjustment` / vessel | `E7` / SET | DEMAND / `LNGBunkeringAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-011 | DEMAND / `PMIAdjustment` / PMI | `F7` / SET | DEMAND / `PMIInput` / `L9` | `ff-1be45943ebd68f0b` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-012 | DEMAND / `PMIAdjustment` / reference mapping | `J7` / SET | DEMAND / `PMIInput` / `L9` | `ff-1be45943ebd68f0b` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-013 | DEMAND / `PowerMixAdjustment` / annual | `F7` / SET | DEMAND / `PowerMixAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-014 | DEMAND / `PowerMixAdjustment` / monthly | `O7` / SET | DEMAND / `PowerMixAdjustment` / `B7` | `ff-0fe361028f1e9fbb` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-015 | DEMAND / `WeatherAdjustment` / country degree-day | `G7` / SET | DEMAND / `WeatherInput` / `AI9` | `ff-b4ccc335b87b8145` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-016 | DEMAND / `WeatherAdjustment` / country temperature | `O7` / SET | DEMAND / `WeatherInput` / `AI9` | `ff-b4ccc335b87b8145` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-017 | DEMAND / `WeatherAdjustment` / region degree-day | `X7` / SET | DEMAND / `WeatherInput` / `AI9` | `ff-b4ccc335b87b8145` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-018 | DEMAND / `WeatherAdjustment` / region temperature | `AE7` / SET | DEMAND / `WeatherInput` / `AI9` | `ff-b4ccc335b87b8145` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-019 | SUPPLY / `LiqProdAdjustment` / LiqProdAdjustment | `G11` / SET | SUPPLY / `LiqProdAdjustment` / `CM11` | `ff-d77df3f8844d280d` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-020 | SUPPLY / `LiqUtilAdjustment` / LiqUtilAdjustment | `G11` / SET | SUPPLY / `LiqUtilAdjustment` / `H7` | `ff-64484b8521725000` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-021 | SUPPLY / `PipelineAdjustment` / PipelineAdjustment | `H8` / SET | SUPPLY / `PipelineAdjustment` / `F8` | `ff-04bc62e734691df0` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-022 | SUPPLY / `ProductionAdjustment` / annual | `E8` / SET | SUPPLY / `ProductionAdjustment` / `D13` | `ff-102ef37b6a5aca37` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-023 | SUPPLY / `ProductionAdjustment` / monthly | `M8` / SET | SUPPLY / `ProductionAdjustment` / `N8` | `ff-81c941c4d848a076` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-024 | SUPPLY / `RegasAdjustment` / main | `H11` / SET | SUPPLY / `RegasAdjustment` / `B11` | `ff-af253a9104b2a8dd` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-025 | SUPPLY / `{USA}Hurricanes` / {USA}Hurricanes | `D8` / SET | SUPPLY / `{USA}Hurricanes` / `E13` | `ff-b1f174a81de58f55` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-026 | TRADE / `CostInput` / liquefaction cost | `K3` / SET | TRADE / `CostInput` / `E3` | `ff-0f6bf681715c9117` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-027 | TRADE / `CostInput` / LNG upstream | `R3` / SET | TRADE / `CostInput` / `E3` | `ff-0f6bf681715c9117` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-028 | TRADE / `CostInput` / non-LNG upstream | `Y3` / SET | TRADE / `CostInput` / `E3` | `ff-0f6bf681715c9117` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-029 | TRADE / `CostInput` / pipeline cost | `AG3` / SET | TRADE / `CostInput` / `E3` | `ff-0f6bf681715c9117` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-030 | TRADE / `CostInput` / regas tariff | `D3` / SET | TRADE / `CostInput` / `E3` | `ff-0f6bf681715c9117` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-031 | TRADE / `DemandAdjLogicInput` / markets | `A3` / SET | TRADE / `DemandAdjLogicInput` / `K3` | `ff-1a7b2737e6ab60cd` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-032 | TRADE / `DemandAdjLogicInput` / selector | `C3` / SET | TRADE / `DemandAdjLogicInput` / `K3` | `ff-1a7b2737e6ab60cd` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-033 | TRADE / `DemandAdjLogicInput` / thresholds | `BLOCKED` / SET | TRADE / `DemandAdjLogicInput` / `L5` | `ff-0ee01f4e20a796e9` | BLOCKED |
| TC-SRC-034 | TRADE / `LNGExportRestictionInput` / LNGExportRestictionInput | `B4` / SET | TRADE / `LNGExportRestictionInput` / `B4` | - | PRESERVATION_ONLY |
| TC-SRC-035 | TRADE / `LNG_TradeRestrictionInput` / LNG_TradeRestrictionInput | `B6` / SET | TRADE / `LNG_TradeRestrictionInput` / `B6` | - | PRESERVATION_ONLY |
| TC-SRC-036 | TRADE / `MidstreamCapacityInput` / importer share | `FD4` / SET | TRADE / `MidstreamCapacityInput` / `EN4` | `ff-8687796f8c182606` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-037 | TRADE / `MidstreamCapacityInput` / pipeline disruption | `DH4` / SET | TRADE / `MidstreamCapacityInput` / `EN4` | `ff-8687796f8c182606` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-038 | TRADE / `MidstreamCapacityInput` / pipeline share | `EB4` / SET | TRADE / `MidstreamCapacityInput` / `EC4` | `ff-628d0066ea442603` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-039 | TRADE / `MidstreamCapacityInput` / regas disruption | `EI4` / SET | TRADE / `MidstreamCapacityInput` / `EN4` | `ff-8687796f8c182606` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-040 | TRADE / `PNG_TradeRestrictionInput` / PNG_TradeRestrictionInput | `B6` / SET | TRADE / `PNG_TradeRestrictionInput` / `B6` | - | PRESERVATION_ONLY |
| TC-SRC-041 | TRADE / `ShippingCostInput` / Atlantic charter | `D17` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-042 | TRADE / `ShippingCostInput` / fuel | `D28` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-043 | TRADE / `ShippingCostInput` / Pacific charter | `F6` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-044 | TRADE / `ShippingCostInput` / Panama crossing | `D50` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-045 | TRADE / `ShippingCostInput` / Panama disruption | `C72` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-046 | TRADE / `ShippingCostInput` / Panama spot | `D88` / SET | TRADE / `ShippingCostInput` / `M4` | `ff-b920c4f421bc8654` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-047 | TRADE / `ShippingCostInput` / Suez crossing | `D39` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-048 | TRADE / `ShippingCostInput` / Suez disruption | `D61` / SET | TRADE / `ShippingCostInput` / `E6` | `ff-0ee01f4e20a796e9` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-049 | TRADE / `ShippingCostInput` / technical parameters | `E83` / SET | TRADE / `ShippingCostInput` / `C83` | `ff-6a607488642ea260` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-050 | TRADE / `UpstreamAdjLogicInput` / country-year adjustment | `P3` / SET | TRADE / `UpstreamAdjLogicInput` / `O3` | `ff-711d0e08bb0b9fa3` | EXCEL_GOLDEN_REQUIRED |
| TC-SRC-051 | TRADE / `UpstreamAdjLogicInput` / general | `B3` / SET | TRADE / `UpstreamAdjLogicInput` / `O3` | `ff-711d0e08bb0b9fa3` | EXCEL_GOLDEN_REQUIRED |

`TC-SRC-033`은 좌표를 임의 선택하지 않는다. INPUT-01에서도 경계와 `D/F` 계열 후보가 서로 맞지 않고 정책 확인 필수로 표시돼 있다. INPUT-03에서 실제 편집 열을 확정하면 같은 stable test ID에 좌표만 채운다.

### Family·외부 참조·package 추가 테스트

| Test ID | Given / When / Then | 판정과 성공 기준 | 실패 영향 |
| --- | --- | --- | --- |
| TC-FAM-CALC | Given `EXCEL_GOLDEN_REQUIRED` 254개 family, 1,503,061셀과 각 대표 cell; When 각 family가 계산되는 최소 fixture를 Excel과 후보 엔진에서 재계산; Then family 전체의 type/error/value와 cell 수 비교 | 계산 적용 범위는 EXCEL_GOLDEN_REQUIRED | 미검증 family가 있는 계산 경로는 호환 지원으로 표시 금지 |
| TC-FAM-PRESERVE | Given backup/제외 영역의 비외부 61개 family, 317,422셀; When 편집·내부 계산·저장·재개방; Then formula·cached value와 package 구조가 source snapshot과 동일 | PRESERVATION_ONLY | 보존 영역이 변경된 workbook 전달 차단 |
| TC-FAM-EXT | Given 외부 전용 20개 family, 42,394셀; When 편집·내부 계산·저장·재개방; Then formula·cached value·externalLink 관계가 source snapshot과 동일 | PRESERVATION_ONLY | 외부 셀이 변경된 workbook 전달 차단 |
| TC-EXT-001 | Given SUPPLY source snapshot의 외부 `LUT`; When `BCHurricanes` 8개 외부 참조를 refresh 없이 보존; Then `ff-c088aa2f716f0186`의 formula·cached value·link가 동일 | PRESERVATION_ONLY | SUPPLY 생성 파일 전달 차단 |
| TC-EXT-002 | Given `BCLiquefaction_backup` source snapshot; When 35,091개 외부 참조를 refresh 없이 보존; Then 관련 2개 family와 externalLink가 동일 | PRESERVATION_ONLY | SUPPLY 생성 파일 전달 차단 |
| TC-EXT-003 | Given TRADE source snapshot; When `MidstreamCapacityInput` 5,167개 외부 참조를 refresh 없이 보존; Then 7개 외부 family와 externalLink가 동일 | PRESERVATION_ONLY | TRADE 생성 파일 전달 차단 |
| TC-EXT-004 | Given TRADE source snapshot; When `UpstreamAdjLogicInput` 1,344개 외부 참조를 refresh 없이 보존; Then 2개 외부 family와 externalLink가 동일 | PRESERVATION_ONLY | TRADE 생성 파일 전달 차단 |
| TC-EXT-005 | Given TRADE source snapshot; When `CostInput` 517개 외부 참조를 refresh 없이 보존; Then `ff-a70b161c0431c526`의 formula·cached value·link가 동일 | PRESERVATION_ONLY | TRADE 생성 파일 전달 차단 |
| TC-EXT-006 | Given TRADE source snapshot; When `4.5 PriceModelInput` 267개 외부 참조를 refresh 없이 보존; Then 7개 family와 externalLink가 동일 | PRESERVATION_ONLY | TRADE 생성 파일 전달 차단 |
| TC-UDF-001 | Given `GDP` 실행 환경; When GDP table 변경 후 재계산; Then 호출 family와 Excel golden 일치 | EXCEL_GOLDEN_REQUIRED; 함수 제공 주체 확인 전 BLOCKED | DEMAND GDP table GO 차단 |
| TC-UDF-002 | Given `PIPELINE` 실행 환경; When Midstream capacity 변경 후 재계산; Then 관련 family와 Excel golden 일치 | EXCEL_GOLDEN_REQUIRED; 함수 제공 주체 확인 전 BLOCKED | TRADE Midstream GO 차단 |
| TC-XLFN-001 | Given `UNIQUE`를 지원하는 Excel golden; When 활성 `MidstreamContractInput`의 `_XLFN.UNIQUE` 2셀을 재계산; Then array shape와 값 일치하고 backup/제외 영역 3셀은 원본 보존 | 활성 2셀은 EXCEL_GOLDEN_REQUIRED, 나머지 3셀은 PRESERVATION_ONLY | 활성 Midstream 계약 계산 호환 주장 차단 |
| TC-VBA-001 | Given `trade.xlsm`의 VBA binary hash; When 편집·저장·재개방; Then VBA part와 관계가 byte-identical이고 Excel이 복구 경고 없이 연다 | PRESERVATION_ONLY; macro 자동 실행은 별도 승인 없이는 하지 않음 | TRADE 파일 생성 GO 차단 |
| TC-PKG-001 | Given workbook별 names/scope, calcChain, externalLinks, connections, merge, validation, table counts; When 저장; Then 위 Workbook 요약 수치와 part 관계가 동일 | PRESERVATION_ONLY | 3개 XLSM 전달 차단 |
| TC-HIDDEN-001 | Given hidden sheet 14개; When 저장; Then 이름·순서·hidden state·수식 수가 동일 | PRESERVATION_ONLY | 관련 workbook 전달 차단 |
| TC-BACKUP-001 | Given `LiqUtilAdjustment_backup` → `LiqProdAdjustment` 397-cell edge; When backup 보존 후 재계산; Then target family와 golden 일치 | EXCEL_GOLDEN_REQUIRED | SUPPLY LiqProd GO 차단 |
| TC-BACKUP-002 | Given TRADE backup/excluded sheet; When 저장; Then `backup_MidstreamContractInput` 117,970 수식/40 family와 `Industry research` array family 보존 | PRESERVATION_ONLY | TRADE 전달 차단 |
| TC-BLANK-001 | Given 빈 시트 10개; When 저장; Then sheet part·state·순서가 유지되고 수식은 계속 0 | NOT_APPLICABLE + PRESERVATION_ONLY | package 호환 실패 |
| TC-NOFORM-001 | Given 수식 없는 TRADE restriction table 3개; When 대표 cell SET/CLEAR; Then 값만 바뀌고 formula/package count 불변 | PRESERVATION_ONLY | 해당 입력 화면 저장 차단 |
| TC-EXT-MISSING-001 | Given 운영 조건대로 외부 원본 파일이 없음; When 내부 계산과 저장을 수행; Then 외부 파일을 요구·요청하거나 refresh하지 않고 source snapshot의 formula·cached value·link를 보존 | 성공이 기대값; PRESERVATION_ONLY | 외부 원본을 잘못된 선행조건이나 업무담당자 요청사항으로 만드는 회귀 차단 |
| TC-FAIL-002 | Given 미지원 `GDP`, `PIPELINE`, `_XLFN.UNIQUE`; When 재계산; Then 명시적 unsupported/blocked 결과 | 실패가 기대값; 조용한 값 고정 금지 | silent corruption 위험 |
| TC-FAIL-003 | Given formula error 또는 계산 중단; When 저장 요청; Then Case 최종저장/실행 준비로 넘기지 않음 | 실패가 기대값; 부분 계산 파일 전달 금지 | 기존 업로드 흐름 오염 위험 |

hidden 14개는 DEMAND의 `Scenario Tracker`, `Sheet2_DISCARDAFTUSE`, `CubeDump_DISCARDAFTUSE`; SUPPLY의 `{USA}RigsLogic`, `{China}PolicyLogic`, `{USA}HurricanesLogic`, `RegasLogic`, `PipelineLogic`, `MonthlyAdjProduction`; TRADE의 `Sheet1`, `Industry research`, `net exporter`, `pipeline cap`, `pipeline contract`다.

### 상용 엔진 없이 지금 구현 가능한 범위

- INPUT-01 catalog를 조회 metadata로 제공하고 51개 편집 source 가운데 좌표가 확정된 50개를 안정적으로 읽고 쓸 수 있다.
- 수식 원문을 해석하지 않고도 XLSM package, VBA, externalLinks, connections, names, validation, merge, OOXML table, hidden/backup sheet를 보존하는 writer와 회귀 검사를 만들 수 있다.
- `normal/shared/array` family와 내부·외부 dependency inventory를 빌드 시점에 재검증할 수 있다.
- 계산 엔진 없이 가능한 것은 “값 수정 + 구조 보존 + 계산 후보 cached 값 무효화/재계산 필요 표시 + 외부 참조 cached 값 보존”까지다. 계산 후보의 변경 후 결과를 확정하는 것은 포함하지 않는다.

### 후속 동적 PoC blocker

- 동일 변경을 Microsoft Excel에서 적용해 계산 후보를 재계산한 golden XLSM이 없다.
- `GDP`, `PIPELINE` 함수 제공 주체와 실행 환경이 확인되지 않았다.
- TRADE VBA가 계산에 관여하는지, 단순 보존만 필요한지 확인되지 않았다.
- `DemandAdjLogicInput / thresholds`의 편집 열과 대표 source cell이 미확정이다.
- 이 blocker가 해소되기 전에는 계산 후보 복합 수식의 결과를 GO로 판정할 수 없고 상용 엔진 필요성도 확정할 수 없다. 외부 참조 42,394셀은 계산 결과 GO 대상이 아니라 보존 검증 대상이다.
