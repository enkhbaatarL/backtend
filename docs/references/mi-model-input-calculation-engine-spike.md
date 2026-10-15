# MI 모델 Input 계산 엔진 실파일 Spike

## Document Control

- status: Complete
- canonical for: Reference only — INPUT-02 조사 및 INPUT-02A 부분 실행 증거
- owner: BE
- date: 2026-09-16
- task type: investigation
- source documents:
  - `docs/design-docs/gas/mi-model-input-editing-delivery-plan.md`
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md` (부록 A의 실행 기록)
  - `docs/references/mi-model-input-functions-2026-10-07.md` 및 JSON (최신 함수 전체 집계·기본 등록 근거)
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `docs/references/mi-model-input-baseline-2026-10-07.md` (부록 A의 입력 기준본)
- supersedes: 없음
- last synced: 2026-10-13
- open questions:
  - 2026-10-13 사용자 결정으로 계산 방식은 상용 엔진 평가 없이 POI 부분 계산이다(domain design §1A.4 Q7). 이 방식의 남은 fixture 실파일 계산·보존성과 운영 격리·동기/비동기 경계 검증이 필요하다.
  - POI 기본 미지원 함수의 사용 위치를 표별 계산/보존 정책에 연결하고 실제 변경 입력·배열 결과·원본 보존을 검증해야 한다. 이름이 가리키는 수식 셀의 계산 영향과 VBA 동작도 미검증이다.

이 문서는 계산 엔진 선정 자체의 canonical 설계가 아니라, 실제 XLSM 3개로 수행한
`INPUT-02` 및 최신 source의 `INPUT-02A` 부분 실행 증거다. 셀 원문, 수식 원문, 고객사 식별값은 기록하지 않는다.

## INPUT-02A 4사례 선택 계산 부분 실행 — 2026-10-12

- Java 17.0.14 / Apache POI 5.4.0 기본 `FormulaEvaluator`, heap 768m, custom/fast SUMIFS 없음. 전체 XLSM을 POI로 열지 않고 승인 수식과 읽기 상수만 작은 메모리 workbook에 적재했다. 입력은 hash가 확인된 source/독립 미계산 복사본, golden은 저장 후 비교에만 사용했다.
- F-D-02 130셀, F-S-01 9,612셀, F-S-02/03 각 8셀을 실제 평가해 XLSM에 저장하고 다시 읽었다. 최종 총 9,758셀의 값·타입·오류와 확보된 Excel golden 비교 **Pass**, 네 생성 파일의 변경 전 정규화 source 기준 보존 **Pass**. 승인 수식은 그대로 유지했으며 계산 밖 셀·캐시/서식·validation·hidden·externalLink/캐시·pivot/캐시·names/merge 등 실제 항목을 확인했다. VBA/connection/native Table은 실행한 DEMAND/SUPPLY source에 없었다.
- Hurricanes는 source LUT 8캐시 셀로 외부 참조용 메모리 workbook을 만들었다. 최초 두 사례의 G8:G11·G16:G19는 evaluator map에서 주 평가기를 빠뜨려 `WorkbookNotFoundException`으로 실패했다. 주/LUT 평가기를 함께 등록한 뒤 두 사례만 독립 재실행해 각 8셀 Pass. 최초 로그/도구/진단 파일은 보존했다. 기존 G 캐시·0으로 대체하거나 수식을 다시 쓰지 않았고 외부 원본 개방·링크 갱신·매크로 실행도 없었다.
- [상세 범위/셀·보존 판정](mi-model-input-sheet-calculation-compatibility.md#4사례-poi-선택-계산-실행--2026-10-12), [최종 실행 결과·XLSM 경로/hash](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/final-summary.json), [환경/jar hash·설정](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/environment.json), [재현 명령·실패 이력·한계](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/README.md)를 연결한다. 실제 값·전체 비교/보존 JSON·도구/로그는 새 Git 제외 temp 경로에만 있다. Java/JUnit 테스트 대신 standalone 평가를 실행했다.
- 원본 전달 4파일·조사 대장·기존 표본/golden/기록 등 90개 경로 hash 불변과 생성 ZIP CRC·저장 후 재읽기 Pass. 2026-10-12 실행 당시 native 재개방은 **NOT_RUN**이었고, 후속 2026-10-13 결과/복사본 사용 한계는 다음 절을 따른다. 숫자 허용 오차는 기존 Excel 결과 기록을 그대로 사용했다.
- 이 결과는 네 사례의 선택 경로 검증이다. 아래 전체 workbook 로딩 `NO-GO`와 구분하며 INPUT-02A 전체 완료·제품 엔진 선정/배포·성능/동시성/DRM/운영 실행 방식 검증으로 확대하지 않는다. 나머지 사례/정책·구조·4역할 전체/모델 E2E는 후속이다. 실행 기록은 기존 stable work note를 update했다.

## INPUT-02A native Excel 재개방과 값 유지 확인 — 2026-10-13

- 최종 POI 산출물 네 파일을 Mac용 Microsoft Excel 16.113.4 (26100421)에서 직접 재개방·무저장 닫기했으며 복구 경고 미관측. SUPPLY 세 파일의 외부 링크 안내에는 `업데이트하지 않음`을 선택했다. 이후 사용자 승인 AppleScript로 동일 hash 읽기 전용 복사본의 Excel 내부 `value2`를 읽어 F-D-02 130/130·F-S-01 9,612/9,612·F-S-02/03 각 8/8, 총 **9,758셀 값 유지 Pass**를 확인했다. 모두 numeric·오류/누락/중복 0, 기존 절대/상대 1e-9 허용 오차 내 일치 및 보고 값의 float64 동일이다.
- 읽기 대상의 한계: writable 원산출물에 `read only true`가 적용되지 않아 F-D-02 첫 두 시도는 값 읽기 전에 중단·무저장 닫기했다. 원산출물과 동일 bytes/hash인 복사본에 0444 권한을 적용하고 실제 readOnly=true를 확인한 뒤 전체 값을 읽었다. **원산출물 직접 재개방 경고 확인과 읽기 전용 복사본의 전체 값 유지 확인을 구분한다.** 원산출물 자체의 AppleScript 전체 값 읽기·native 재계산/저장 cycle은 검증하지 않았다. Hurricanes는 `hurricanes-retry/` 최종 산출물을 사용했다.
- 실행 조건: 빈 helper를 통해 개방 전 수동 계산 모드·macro force disable을 고정하고 `update links do not update links`로 열었다. manual·forceDisable·readOnly·alerts=true를 확인하고 값/수식만 읽은 뒤 대상/helper를 `saving no`로 닫았다. 계산·값/수식 쓰기·링크/connection/pivot 새로고침·외부 workbook 개방·macro·save/save-as를 요청하지 않았고 기존 앱 설정을 복원했다. JSON/XML 숫자 표현의 차이 473셀은 최대 1e-15이나 보고 float64는 전부 같다. Hurricanes 16수식의 native 외부 경로 표기는 source에 이미 있던 대상이며 파일 수식·externalLink/캐시는 hash 불변이다. 입력/산출물 94경로·복사본 4개 hash 불변, 산출물 ZIP CRC·전체 XML/관계 parse Pass.
- [사례별 범위·수식 표기·보존 및 검증 한계](mi-model-input-sheet-calculation-compatibility.md#4사례-native-excel-재개방과-값-유지-확인--2026-10-13), [최종 native 값/설정 결과](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/native-summary.json), [동일 hash 복사본 manifest](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/read-only-copies-manifest.json), [실행·재현/실패 이력](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/README.md)와 [stable work note](../work-notes/2026/2026-09-03-model-input-transfer-format.md#2026-10-13-생성-4사례-native-excel-무저장-재개방-결과)에 근거를 연결한다. 실제 값/로그는 Git 제외 temp에만 있다.
- 후속 읽기는 새 계산 성공·Excel golden 확보나 엔진 선정의 증거가 아니다. 최초 직접 UI 개방의 계산 모드는 자동이었고 내부 재계산 여부는 미확인이다. 이후 값 읽기에서는 별도로 개방 전 수동 계산을 고정했으며 내부 이벤트/네트워크 계측·전체 visual 비교·모델 실행은 수행하지 않았다. 나머지 6준비 사례·8미결정 명세·구조/4역할 전체·운영/DRM·엔진 선정은 남는다. **이 재개방 기록 시점에는 INPUT-02A BLOCKED·전체 조사 In Progress였고 이전 전체 workbook 경로 NO-GO는 유지한다.** 같은 날 이후 사용자 결정으로 POI 부분 계산이 승인되어 INPUT-02A는 `IN_PROGRESS`다(Delivery Plan §6A·§9). 이 승인은 `GO`가 아니다.

## 결론

**INPUT-02 당시 판정: NO-GO — 검증한 전체 workbook 경로의 Apache POI 또는 headless LibreOffice를 1차 임시저장 계산 엔진으로
채택할 수 없다.**

- Apache POI 5.4.0은 `supply.xlsm` 전체 로딩만으로 1 GiB heap을 소진했다. 수식 계산까지
  도달하지 못했다.
- 현재 서비스의 `setForceFormulaRecalculation(true)`는 다음 Excel 오픈 시 재계산을
  요청하는 플래그일 뿐, 저장 시점의 cached value를 갱신하지 않는다.
- 로컬 headless LibreOffice 일괄 round-trip은 완료됐지만, 수정된 cross-sheet 복합 수식의
  cached value가 갱신되지 않았다.
- LibreOffice round-trip은 세 파일에서 Excel package 구조를 바꾸었고, 특히 macro가 있는
  표본에서 VBA를 제거하고 external link 수도 변경했다.
- 따라서 화면 임시저장 응답에서 즉시 재계산된 테이블을 돌려주는 동기 처리로도 부적합하다.

당시 제안한 다음 최소 조치는 **Aspose.Cells for Java 평가 라이선스를 확보한 뒤 동일 probe를 그대로
재실행**하는 것이다. 실제 파일에서 계산 정확도와 보존성을 통과하기 전에는 production
엔진으로 선택하지 않는다. 위 2026-10-12 부분 실행은 상용 엔진 없이 수행했으며 후보 선정·전체 probe·운영 검증은 이번 범위 밖이다.
2026-10-13 사용자 결정으로 이 제안은 대체됐다. 상용 엔진은 평가하지 않고 POI 부분 계산을 후보로 승인했다(domain design §1A.4 Q7). 아래 「Aspose.Cells for Java」와 「다음 최소 조치」는 INPUT-02 당시 판단 이력이다.

## 검증 범위와 방법

대상 원본은 `scripts/demand.xlsm`, `scripts/supply.xlsm`, `scripts/trade.xlsm`이다.

1. 시작과 종료 시 원본 3개의 SHA-256을 비교해 원본 비변경을 확인했다.
2. `supply.xlsm` 복사본의 허용된 숫자 input 한 셀만 OOXML 수준에서 변경했다.
3. 다른 시트의 복합 수식 셀에 저장된 cached value가 변경 전 그대로임을 먼저 확인했다.
4. 후보 엔진으로 복사본을 열고 재저장한 뒤 해당 수식과 cached value를 다시 확인했다.
5. 원본과 결과물에서 시트 수, cell style 수, merged cell 수, data validation 수,
   defined name 수, external link 수, VBA binary hash를 비교했다.
6. `/usr/bin/time -l`로 wall time과 process peak RSS를 측정했다.

실행 probe:

`src/test/java/com/sk/common/utils/ModelInputCalculationEngineSpikeTest.java`

일반 test suite에서는 건너뛰며, 실파일 probe는 아래처럼 명시적으로 실행한다.

```bash
INPUT02_CALCULATION_SPIKE_ENABLED=true ./gradlew test -PforceTest \
  --tests 'com.sk.common.utils.ModelInputCalculationEngineSpikeTest' --info
```

## Apache POI 5.4.0

### 코드 경계

- 프로젝트 의존성: `build.gradle`의 `poi`, `poi-ooxml` 5.4.0.
- 현재 `ModelParamExcelMergeService.serialize()`는 workbook을 직렬화하기 전에
  `setForceFormulaRecalculation(true)`만 호출한다.
- Apache POI 공식 문서도 dependency 값 변경 후 cached result를 갱신하려면
  `FormulaEvaluator`를 실행해야 하며, force-recalculation flag는 Excel이 다음 오픈 시
  계산하도록 요청하는 별도 방식이라고 설명한다.
- POI evaluator는 미지원 함수에서 `NotImplementedException`이 발생할 수 있고,
  외부 workbook reference는 실제 참조 workbook과 evaluator map이 필요하다. VBA UDF는
  Java로 다시 구현해 등록해야 한다.

공식 근거:

- [Apache POI Formula Evaluation](https://poi.apache.org/components/spreadsheet/eval.html)
- [Apache POI Formula Evaluation 개발 가이드](https://poi.apache.org/components/spreadsheet/eval-devguide.html)
- [Apache POI License](https://poi.apache.org/legal.html)

### 실측

| 항목 | 결과 |
| --- | --- |
| 표본 | `supply.xlsm` |
| JVM heap | 1 GiB |
| 로딩 결과 | 실패 — `OutOfMemoryError` |
| 실패까지 wall time | 약 6.3초 |
| process peak RSS | 약 1.15 GiB |
| FormulaEvaluator 도달 | 못함 |

따라서 POI evaluator의 개별 함수 지원 여부보다 먼저 **전체 XSSFWorkbook 메모리 모델**이
현재 Backend 동기 처리 경계의 blocker다. heap을 늘려 로딩만 성공시키더라도, 약 186만 수식과
외부 reference를 evaluator가 Excel과 동일하게 처리한다는 증거는 없다.

### 라이선스·배포·운영

- 라이선스: Apache License 2.0. 기존 의존성이므로 신규 상용 라이선스 blocker는 없다.
- 배포: 별도 native binary는 필요 없다.
- 운영 blocker: 현재 1 GiB JVM에서 OOM. web request JVM에서 heap을 공유하면 동시 요청이
  서비스 전체 장애로 이어질 수 있다.
- 정확성 blocker: 미지원 함수, VBA UDF, 누락된 external workbook을 별도로 해결해야 한다.

**후보 판정: NO-GO.**

## Headless LibreOffice

### 실행 조건

- 로컬 실행 파일: `LibreOfficeDev 26.8.0.0.alpha0`.
- fresh user profile, `--headless`, `--convert-to "xlsm:Calc MS Excel 2007 VBA XML"`로
  원본 3개와 수정 복사본 1개를 한 process에서 처리했다.
- 이는 production 승인 버전이 아닌 개발 빌드 실측이다.

### 계산·성능 결과

| 항목 | 결과 |
| --- | --- |
| 입력 | 원본 복사본 3개 + 값 수정 복사본 1개 |
| wall time | 약 223.4초 |
| process peak RSS | 약 1.43 GiB |
| cross-sheet 복합 수식 보존 | 수식은 존재 |
| cached value 갱신 | 실패 — 변경 전 cached value 유지 |

단순 `--convert-to` round-trip은 명시적인 full calculation 계약이 아니며 이번 표본에서도
계산 결과를 갱신하지 못했다. 또한 3분을 넘는 처리 시간과 1 GiB를 넘는 RSS는 동기 임시저장
요청에 허용하기 어렵다.

### XLSM fidelity 결과

`true`는 비교한 구조 카운트 또는 binary hash가 같다는 뜻이다. 값/수식 원문은 비교 결과에
포함하지 않았다.

| 파일 | sheets | styles | merged | validations | names | external links | macro |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| demand | 유지 | 변경 | 변경 | 변경 | 변경 | 해당 없음 | 해당 없음 |
| supply | 유지 | 변경 | 변경 | 변경 | 변경 | 개수 유지 | 해당 없음 |
| trade | 유지 | 변경 | 변경 | 유지 | 변경 | **2 → 1** | **제거됨** |

LibreOffice 공식 문서도 Microsoft Office format의 복잡한 구조와 서식은 다르게 처리되거나
지원되지 않을 수 있다고 설명한다. VBA 원본 코드 보존은 user profile의 VBA 설정에 의존하며,
LibreOffice의 VBA 실행 호환성도 완전하지 않다.

공식 근거:

- [LibreOffice import/export 제한](https://help.libreoffice.org/latest/en-GB/text/shared/guide/ms_import_export_limitations.html)
- [LibreOffice VBA Properties](https://help.libreoffice.org/latest/en-US/text/shared/optionen/01130100.html)
- [LibreOffice Licenses](https://www.libreoffice.org/licenses/)

### 라이선스·배포·운영

- 라이선스: MPL 2.0 기반 Free Software. 설치 이미지의 전체 LICENSE/NOTICE와 포함 component
  라이선스 확인은 필요하지만 상용 runtime fee는 없다.
- 배포 blocker: JRE만으로 끝나지 않으며 LibreOffice native package, font, fresh profile,
  temp storage, process lifecycle을 포함한 별도 runtime image가 필요하다.
- 운영 blocker: 대형 native process의 timeout/kill, 동시성 제한, profile 격리, temp 정리,
  손상 파일 격리, sandbox가 필요하다.
- 보안 blocker: macro가 포함된 고객 파일을 처리하므로 macro 실행 비활성화와 외부 연결 차단을
  강제해야 한다.
- 정확성/fidelity blocker: 이번 기본 headless 경로에서 계산 실패, style·병합·name 변경,
  VBA 제거, external link 손실이 실제로 발생했다.

**후보 판정: NO-GO.**

## 그 밖의 후보

### Microsoft Excel desktop automation

Excel과 가장 가까운 계산 결과를 기대할 수 있어도 Backend server 후보로 두지 않는다.
Microsoft는 unattended/non-interactive server-side Office automation을 권장·지원하지 않으며,
불안정 동작과 deadlock 가능성을 명시한다.

- [Microsoft — unattended Office automation 고려사항](https://learn.microsoft.com/en-us/office/client-developer/integration/considerations-unattended-automation-office-microsoft-365-for-unattended-rpa)

### Aspose.Cells for Java

공식 문서는 XLSM load/save와 embedded formula calculation engine을 제공한다고 설명한다.
그러나 “대부분의 수식” 지원 주장만으로 Excel 호환성을 확정할 수 없고, 이 저장소에는 dependency와
license가 없어 실파일 PoC를 실행하지 않았다.

- [Aspose.Cells 계산 엔진](https://docs.aspose.com/cells/java/calculate-formulas/)
- [Aspose.Cells 지원 형식](https://docs.aspose.com/cells/java/supported-file-formats/)
- [Aspose Java API license types](https://purchase.aspose.com/policies/license-types)

라이선스는 개발자 수, 배포 위치, SaaS/API 제공 방식에 따라 달라진다. 평가 라이선스를 받은 뒤
실파일 계산·fidelity·성능을 확인하고, production 구매 유형을 법무/구매와 확정해야 한다.

**후보 판정: CONDITIONAL / UNVERIFIED.** 이 후보가 있다는 사실은 현재 작업의 NO-GO를
GO로 바꾸지 않는다.

## 다음 최소 조치

> 2026-10-13 대체: 아래 1~2단계(상용 엔진 평가)는 수행하지 않는다. 현재 후보는 POI 부분 계산이며 진행 상태와 검증 gate는 Delivery Plan `INPUT-02A`의 Done Criteria를 따른다. 아래 목록은 당시 기록으로 남긴다.

1. Aspose.Cells for Java 30일 평가 라이선스와 검토 가능한 EULA/견적 조건을 확보한다.
2. 이 문서와 동일한 3개 실파일 probe를 실행한다.
3. 다음 gate를 모두 만족할 때만 엔진 후보를 `GO`로 바꾼다.
   - 수정 input이 영향을 주는 cross-sheet 복합 수식의 cached value가 기대값으로 갱신된다.
   - macro binary, style, merged cell, validation, defined name, external link가 손실되지 않는다.
   - 결과 파일을 재오픈할 수 있고 원본은 변경되지 않는다.
   - 허용 가능한 시간·메모리 상한과 동시성 정책이 정해진다.
4. 통과하더라도 계산은 web request JVM 안이 아니라 자원 제한과 timeout을 둔 격리 worker로
   배치할지 운영 설계에서 결정한다. 이는 model container에 계산을 위임한다는 뜻이 아니다.

## 검증 한계

- LibreOffice는 로컬 개발 빌드 한 버전만 검증했다. stable 버전, 명시적 UNO `calculateAll`,
  VBA 보존 profile을 별도로 조합하면 결과가 달라질 수 있다. 다만 현재 기본 headless 경로는
  production 채택 증거로 사용할 수 없다.
- POI는 1 GiB에서 전체 로딩에 실패해 실제 sample FormulaEvaluator 정확도까지 도달하지 못했다.
- DRM 적용 파일은 이 probe에 포함하지 않았다. production에서는 복호화된 임시 파일의 수명,
  접근 권한, 삭제 보장도 별도 검증해야 한다.

## 부록 A. 2026-10-07 기준본 4파일 함수 조사 (INPUT-02A 1단계)

위 본문은 2026-09-16의 표본 3개 기준이고 판정(NO-GO)은 바뀌지 않는다. 이 부록은
[기준본 고정](mi-model-input-baseline-2026-10-07.md)한 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6 정규화본 4개에서
쓰이는 함수와 Apache POI 5.4.0의 계산 지원 여부를 정적으로 조사한 결과다. 다음 단계(미지원 함수 셀에서
거꾸로 따라가 재계산 대상 여부를 판정)의 입력이며, 이 부록은 그 판정을 하지 않는다.
수식·셀 값 원문은 기록하지 않고 함수 이름, 개수, 좌표 범위, 예/아니오 표지만 쓴다. 셀 단위 전체 목록은 Git에 넣지 않은
`temp/model-input-function-inventory/2026-10-07/function-inventory.json`에 있다.

전체 함수 집계·기본 등록 목록·source hash의 주 근거는 [최신 함수 reference](mi-model-input-functions-2026-10-07.md)와 그 JSON이다. 이 부록은 별도 조사의 교차 확인, 실제 함수 호출 시험, 배열 범위·판별대장 대응 근거를 보유한다. 업무 계산/보존 정책은 domain design, 검증 계획은 calculation compatibility 문서가 소유한다.

### A.1 결과 요약

- 대상은 4파일·70개 시트(숨김 14개 포함)의 수식 셀 1,863,766개 전부다. 시트별 수식·셀 수와 시트 part SHA-256이
  manifest와 모두 같다.
- 셀 수식에 쓰인 함수는 25종이다. POI 5.4.0 기준 **지원 24종, 미지원(이름은 알지만 구현 없음) 0종,
  목록 밖 1종**이다. 목록 밖은 `_XLFN.UNIQUE`이며 TRADE의 5셀에만 있다(아래 A.5).
- 조건부서식에는 목록 밖 `_XLUDF.ISFORMULA`가 16개 수식에 있다(SUPPLY 6개 시트 14, TRADE 2개 시트 2).
  셀 값 계산 대상이 아니라 별도 목록(A.6)으로만 둔다.
- 정의된 이름 94개의 정의식 및 이름 간 참조에서 함수 호출은 없다. 이름이 가리키는 일반 셀의 수식까지 추적한 결과는 아니므로 이름을 통한 계산 영향이 0이라고 판정하지 않는다.
- `INDIRECT`·`OFFSET` 호출은 셀·이름·조건부서식·유효성·표 열 수식 어디에도 없다.
- 이전 호환성 문서가 표준 목록 밖 호출 후보로 든 `GDP`·`PIPELINE`은 함수 호출이 아니다. 둘 다 문자열 리터럴 안의
  글자이며, 기존 스크립트의 함수 정규식을 기준본에 적용하면 서로 다른 수식 원문 기준 DEMAND `GDP` 1건·TRADE
  `PIPELINE` 6건이 같은 값으로 재현된다. 셀 수식에서 그 밖의 함수 이름은 기존 정규식과 새 추출기가 같고 기존 정규식의 누락은 없다.

### A.2 조사 대상

| 항목 | DEMAND | OUTPUT | SUPPLY | TRADE | 합계 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 시트 수 | 25 | 5 | 23 | 17 | 70 |
| 숨김 시트 | 3 | 0 | 6 | 5 | 14 |
| 수식 셀(`<f>`가 있는 셀) | 699,298 | 889 | 460,803 | 702,776 | 1,863,766 |
| normal | 132,913 | 1 | 186,371 | 102,976 | 422,261 |
| shared 원본 | 8,269 | 30 | 4,251 | 9,324 | 21,874 |
| shared 따라오는 셀 | 558,116 | 858 | 270,180 | 590,471 | 1,419,625 |
| array 시작 셀(dynamic array 포함) | 0 | 0 | 1 | 5 | 6 |
| 정의된 이름 | 37 | 18 | 12 | 27 | 94 |
| externalLink part | 0 | 1 | 1 | 2 | 4 |
| `vbaProject.bin` | 없음 | 없음 | 없음 | 있음 | - |

- shared 따라오는 셀은 XML에 수식 본문이 생략된 셀이다. 원본 수식의 함수를 따라 셀 수에 포함했다.
- array(dynamic array 포함)는 시작 셀만 `<f>`를 가진다. 적용 범위 안의 나머지 셀은 계산 결과 값이라 수식 셀 수에 들어가지 않는다.
- 숨김 시트는 DEMAND `Scenario Tracker`·`Sheet2_DISCARDAFTUSE`·`CubeDump_DISCARDAFTUSE`, SUPPLY `{USA}RigsLogic`·
  `{China}PolicyLogic`·`{USA}HurricanesLogic`·`RegasLogic`·`PipelineLogic`·`MonthlyAdjProduction`, TRADE `Sheet1`·
  `Industry research`·`net exporter`·`pipeline cap`·`pipeline contract`다. 전부 조사에 포함했다.

### A.3 방법과 검증

1. **입력 확인**: 4파일 정규화본·원본 snapshot의 SHA-256을 manifest와 작업 전후 모두 대조했고 같다. 판별대장도 전후 같다.
2. **방법 A (OOXML 직접 스캔)**: 시트 XML을 스트리밍으로 읽고, 직접 만든 수식 스캐너로 함수를 뽑았다. 스캐너는 문자열
   리터럴, 따옴표로 감싼 시트 이름(괄호 포함), 대괄호 안(외부 workbook 번호·구조적 참조)을 건너뛴다. shared 원본의 함수를
   따라오는 셀에 적용했다.
3. **방법 B (독립 재추출)**: `openpyxl` read-only 리더가 shared 따라오는 셀을 자체 번역 규칙으로 복원한 수식을 `openpyxl`
   수식 Tokenizer로 쪼개 함수를 뽑았다. 조건부서식·유효성은 방법 A와 다른 경로(원문 텍스트 정규식 + Tokenizer)를 썼다.
4. **대조 결과**: 함수별 사용 셀 수·호출 수·셀 주소 집합, `INDIRECT`·`OFFSET`·`#REF!`·외부 참조가 같은 셀에 있는 셀 집합,
   정의된 이름을 참조하는 셀 집합, 조건부서식·유효성 수식 수와 함수가 4파일 모두 **차이 0**이다. 유일한 차이는
   정의된 이름 개수(A 94 / B 72)이며, B(openpyxl)가 내장 이름 `_xlnm._FilterDatabase` 22개를 이름 목록에서 따로 처리하기
   때문이다. 이를 뺀 72개는 두 방법의 이름·범위가 같고, 내장 이름에는 함수가 없다.
5. **오탐 점검**: 기존 스크립트의 함수 정규식은 문자열 리터럴·괄호가 든 시트 이름·대괄호 안의 글자를 함수로 셀 수 있다.
   이번에는 새 추출기를 따로 쓰고, 같은 입력을 새 추출기·`openpyxl`·기존 정규식에 넣어 비교했다(23개 시험 수식에서 새
   추출기와 `openpyxl`이 모두 기대값과 일치, 기존 정규식은 9개 수식에서 가짜 함수 생성). 기준본 전체에 기존 정규식을
   적용하면 가짜 함수는 `GDP`·`PIPELINE` 둘뿐이다. 기존 스크립트는 수정하지 않았다.
6. **POI 판정**: 프로젝트 의존성 POI 5.4.0 jar에서 `Version.getVersion()`이 5.4.0임을 확인했다.
   `WorkbookEvaluator.getSupportedFunctionNames()` 251개, `getNotSupportedFunctionNames()` 161개를 조회해 이름 기준으로 세
   갈래(지원/미지원/목록 밖)로 나눴다. 이름 목록에는 접두어가 없다.
7. **실제 evaluate**: 기준본에서 쓰인 29종(셀 25 + 조건부서식 전용 4) 전부를 메모리 workbook에서 실제 `FormulaEvaluator.evaluate()`
   했다. 접두어 함수 2종은 원문 표기·대문자·접두어 제거 3가지 표기를 각각 인수 형태 31종으로 시도했고, 모든 시도가
   `NotImplementedFunctionException`이다. 접두어 없는 27종은 모두 evaluate에 도달했다(`AVERAGEIFS`·`MATCH`·`SEARCH`·
   `SUMIFS`·`VLOOKUP`은 범용 시험 인수에서는 Excel 오류값만 나왔다. 이름은 처리됐다는 뜻이며 계산 정확도는 이번 범위가 아니다).
   시험 도구 검증용 대조군: 접두어가 붙은 지원 함수 `_xlfn.STDEV.S`·`_xlfn.CONCAT`은 값을 반환, `_xlfn.IFS`·`_xlfn.XLOOKUP`은
   이름을 처리한 뒤 오류값을 반환, 존재하지 않는 이름과 미지원 목록의 `DATEDIF`는 `NotImplementedFunctionException`이다.
8. **함수 출처와 VBA 경계 (2026-10-08 정정)**: `UNIQUE`는 [Excel 내장 함수](https://support.microsoft.com/en-us/excel/functions/unique-function)이며 `_xlfn.UNIQUE`는 [MS-XLSX §2.2.2](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-xlsx/3d025add-118d-4413-9856-ab65712ec1b0)의 저장 문법에 포함된다. POI 목록 밖이라는 이유로 VBA 제공 주체 미확인으로 분류하지 않는다. 이 함수 출처 확인을 위한 `oletools` 설치는 선행조건이 아니다. `vbaProject.bin`은 TRADE에만 있으며 매크로 동작은 조사하지 않았다. 조건부서식의 `_xludf.ISFORMULA` 원본 표기·보존·평가 요구는 별도 검증한다.

### A.4 함수 목록과 실제 호출 시험의 사용 방법

함수별 사용 셀·호출 수·기본 등록 상태의 전체 표는 [최신 함수 reference](mi-model-input-functions-2026-10-07.md)의 「함수 전체 목록」을 사용한다. 이 부록에 같은 전체 표를 복제하지 않는다. 이름 경유 계산 영향 셀 수는 미검증이다.

A.3 7번의 실제 호출 시험은 다음과 같이 해석한다. 실제 업무 수식의 인수·함수 조합·Excel 정답을 검증한 결과는 아니다.

| 기본 등록 함수의 시험 결과 | 함수 수 | 해석 |
| --- | ---: | --- |
| 한 번 이상 값 반환 | 22 | 범용 시험 인수에서 계산 호출과 값 반환 확인 |
| 오류값만 반환 | 5 | `AVERAGEIFS`, `MATCH`, `SEARCH`, `SUMIFS`, `VLOOKUP`; 시험 인수에서 오류값을 반환했으며 실제 업무 인수의 정답 검증은 미완료 |

기본 등록이 없는 `UNIQUE`와 `ISFORMULA`는 원문/대문자/prefix 제거 표기 각각 31개 인수 형태에서 모두 `NotImplementedFunctionException`이었다. 셀 수식과 조건부서식의 사용 위치는 A.5/A.6을 참조한다. 이는 POI 5.4.0 기본 구현의 한계이며 사용자 정의 구현은 등록하지 않았다.

OUTPUT의 수식 889개에는 함수 호출이 없다. 직접 참조·산술 연산뿐이다. 이것은 재계산이 필요 없다는 뜻이 아니다.

### A.5 미지원·목록 밖 함수 셀의 위치

미지원(POI가 이름은 알지만 구현 없음)에 해당하는 함수는 없다. 목록 밖 함수의 셀 위치는 다음과 같다.

| 파일 | 시트(상태) | 수식 범위(시작 셀) | 종류 | 함수 | 같은 셀의 다른 함수 | 외부 workbook 참조 | `#REF!` | `INDIRECT`·`OFFSET` | 저장값 오류 | 대장 표 ID |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TRADE | `MidstreamContractInput` (표시) | `CK4:CM10` (`CK4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-137 · 재확인 필요 |
| TRADE | `MidstreamContractInput` (표시) | `CQ4:CS29` (`CQ4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-138 · 재확인 필요 |
| TRADE | `backup_MidstreamContractInput` (표시) | `CK4:CM14` (`CK4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-187 · 재확인 필요 |
| TRADE | `backup_MidstreamContractInput` (표시) | `CQ4:CS29` (`CQ4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-187 · 재확인 필요 |
| TRADE | `Industry research` (숨김) | `A3` (`A3`) | array · dynamic | `_XLFN.UNIQUE` | - | 없음 | **있음** | 없음 | **예** | T-188 · 재확인 필요 |

- 5곳 모두 `MidstreamContractInput`·`backup_MidstreamContractInput`·`Industry research`의 array(dynamic array) 수식의 시작 셀이다. 표의 「수식 범위」는 XML이 기록한 적용 범위이며, 시작 셀 외의 셀은 계산 결과 값이라 수식 셀로 세지 않았다. 적용 범위를 모두 합치면 211셀이다(TRADE array 5개 기준).
- 대장 표 ID는 판별대장 「표 대장」 H열(해석 범위)이 이 수식 범위를 완전히 포함하는 표다. 부분 겹침은 없었다. TRADE의 대장 범위는 v3 기준이라 모두 「재확인 필요」다. 어느 표에도 속하지 않는 셀은 없다.
- 4곳은 외부 workbook 참조·`#REF!`·`INDIRECT`·`OFFSET`이 없고 저장값도 오류가 아니다. 숨김 시트 `Industry research!A3`만 `#REF!`를 포함하고 저장값이 오류다.
- `UNIQUE`는 Excel 내장 함수다. `_xlfn.` 저장 표기와 POI 5.4.0의 기본 구현 부재를 구분한다(A.3 8번).
- 2026-10-08 정책 대응: 사용자 확인 및 최신 대장의 T-137/T-138/T-187/T-188 A 정책에 따라 위 5셀은 모두 원본 보존이다. `MidstreamContractInput!CK4/CQ4`가 있는 미표시 표는 수식·배열 결과·cached value를 유지하고 재계산하지 않는다. 상세 보존 범위는 [함수 reference](mi-model-input-functions-2026-10-07.md)를 따른다. 위 표의 표시/숨김은 시트 상태이지 표의 화면 표시 정책이 아니다. POI 기본 미지원이라는 기술 사실은 유지하지만 현재 UNIQUE 구현을 계산 선행조건으로 두지 않는다.
- 이 5곳 외에 SUPPLY `LiqProdAdjustment_backup`의 array(dynamic) 1셀이 있지만 `IFERROR`·`INDEX`·`MATCH`만 쓰는 지원 함수 수식이다.

### A.6 셀 수식 외의 수식 위치 — 조건부서식·데이터 유효성·Table 계산열

다음 표는 조건부서식 수식의 함수다. 수식 수는 해당 함수를 쓰는 수식의 수, 호출 수는 등장 횟수다. 조건부서식 평가와 데이터 유효성 검사는 일반 셀 값 재계산과 별도 책임이다. Table 계산열은 실제 셀 값의 계산식이므로 metadata 위치에서 검출했다는 이유로 재계산 대상에서 제외하지 않는다. 대응 셀의 계산/보존 정책과 구조 변경 시 수식 복제 규칙에 연결한다.

| 함수 | 파일 표기 | POI 목록 판정 | 실제 evaluate | DEMAND | SUPPLY | TRADE | 수식 수 합계 | 호출 수 합계 |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| **`ISFORMULA`** | `_XLUDF.ISFORMULA` | **목록 밖** | **NotImplementedFunctionException** | - | 14 | 2 | 16 | 16 |
| `AND` |  | 지원 | evaluate 도달(구현됨) | - | 14 | 2 | 16 | 16 |
| `ISNUMBER` |  | 지원 | evaluate 도달(구현됨) | - | 10 | 2 | 12 | 18 |
| `NOT` |  | 지원 | evaluate 도달(구현됨) | 2 | 14 | 2 | 18 | 18 |
| `OR` |  | 지원 | evaluate 도달(구현됨) | - | 5 | 2 | 7 | 7 |
| `ROW` |  | 지원 | evaluate 도달(구현됨) | - | 3 | 2 | 5 | 5 |
| `ISERROR` |  | 지원 | evaluate 도달(구현됨) | 2 | - | - | 2 | 2 |
| `SEARCH` |  | 지원 | evaluate 도달(구현됨) | 2 | - | - | 2 | 2 |

조건부서식 수식은 37개(DEMAND 20·SUPPLY 14·TRADE 3)이고, 함수가 든 수식의 적용 범위는 다음과 같다. 확장(x14) 조건부서식은 없다.

| 파일 | 시트(상태) | 함수 | 적용 범위 |
| --- | --- | --- | --- |
| DEMAND | `WeatherInput` (표시) | `ISERROR`, `NOT`, `SEARCH` | `AY9:AY1016` |
| SUPPLY | `BCRegasification` (표시) | `AND`, `ISNUMBER`, `NOT`, `_XLUDF.ISFORMULA` | `G8:XFD1048576` |
| SUPPLY | `LiqProdAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `_XLUDF.ISFORMULA` | `B11:C1048576`, `D11:F1048576`, `G7:XFD8`, `G11:XFD1048576` |
| SUPPLY | `LiqUtilAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `_XLUDF.ISFORMULA` | `B11:C1048576`, `D11:F1048576`, `G7:XFD8`, `G11:XFD1048576` |
| SUPPLY | `ProductionAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `I8:K1048576`, `M8:M1048576` |
| SUPPLY | `RegasAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `_XLUDF.ISFORMULA` | `H9:XFD10`, `H11:XFD1048576` |
| SUPPLY | `{USA}Hurricanes` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B13:D1048576` |
| TRADE | `LNG_TradeRestrictionInput` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B6:J1048576` |
| TRADE | `PNG_TradeRestrictionInput` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B6:I1048576` |

- 데이터 유효성 수식은 14개(DEMAND 7·SUPPLY 3·TRADE 4)이며 함수를 쓰는 수식은 없다. 확장(x14) 유효성은 없다.
- 표(Table) 계산열 수식은 TRADE `4.5 PriceModelInput`에 4개(적용 표 범위 `B3:D11`·`K15:P23`·`R3:U70`)이고 `IFERROR`(2회)·`INDEX`(6회)·`MATCH`(6회)·`SUMIF`(3회)만 쓰며 모두 지원 함수다. pivot 계산 필드·항목 수식은 없다.
- 이름 정의 94개(내장 `_xlnm._FilterDatabase` 22개 포함)의 수식에는 함수가 없다.

### A.7 한계

- **정적 조사**다. VBA가 실행 중에 하는 일과, `INDIRECT`·`OFFSET`처럼 문자열로 만들어지는 참조는 따라가지 못한다. 이번 기준본에는 두 함수 호출이 없지만, 그 사실이 다른 방식의 간접 참조가 없다는 증거는 아니다.
- POI 판정은 **이름 목록과 최소 1회 evaluate**다. 인수 형태·함수 조합에 따른 계산 정확도, Excel과의 값 일치, dynamic array의 결과 shape는 보지 않았다. evaluate는 실제 workbook이 아니라 메모리의 임시 셀에서 했다.
- 전체 XLSM을 POI로 로딩·재저장하지 않았다. 본문의 1 GiB heap OOM 판정과 LibreOffice 보존 실패 결과를 이 조사가 해소하지 않는다.
- 「저장값 오류」는 셀에 오류 타입의 저장값이 있는지만 본다. 오류의 의미나 재계산 시 달라질지는 판정하지 않는다.
- 조사 위치는 셀 수식, 이름 정의, 조건부서식, 데이터 유효성, 표 계산열, pivot 계산 필드·항목이다. TRADE의 Power Pivot 데이터 모델(`xl/model/item.data`)의 수식(DAX)과 연결·쿼리 정의, 외부 link의 저장 값은 조사하지 않았다.
- 대장 표 ID는 판별대장의 해석 범위(H열)로만 맞췄다. DEMAND·OUTPUT·TRADE의 대장은 v3 범위 기준이라 v8 대응은 후속 검증이 필요하다.
- 이름 정의식·이름 간 참조만 조사했으며 이름→일반 셀→수식 의존성은 추적하지 않았다. 이름 경유 영향 셀 수는 미검증이다.
- VBA 모듈 동작은 미검증이다. 이는 Excel 내장 `UNIQUE`의 출처 확인과 별도이며, POI 함수 지원·파일 보존·모델 영향 검증을 대신하지 않는다.
