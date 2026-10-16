# MI 모델 입력 양식 카탈로그 도메인 모델 설계

## Document Control

- status: Draft
- canonical for: Business rules, ownership, lifecycle, and relationships of the MI model input template catalog, Draft and finalization; boundary to Case and Run
- owner: BE
- source documents:
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/design-docs/gas/mi-agent-param-proposal.md`
  - `docs/references/mi-model-input-catalog-inventory.md`
  - `docs/references/mi-model-input-catalog-inventory-2026-09-28.md` — §6.2의 2026-10-08 v8/v6 우선 조사·Midstream T-133~T-139 인계 근거. 앞의 v2 기록과 구분.
  - `docs/references/mi-model-input-calculation-engine-spike.md`
  - `docs/references/mi-model-input-functions-2026-10-07.md` — 최신 4파일의 함수 관측·POI 5.4.0 기본 등록 및 UNIQUE 보존 정책 대응 근거
  - `docs/references/mi-model-input-baseline-2026-10-07.md` — 최신 v8/v6 원본·정규화본 식별과 보존 검증
  - `docs/references/model-input/README.md` — 판별대장 갱신 범위, 사람 확인과 이전 자동 스캔의 구분
  - `docs/references/mi-model-input-sheet-calculation-compatibility.md` — 표별 계산·보존·구조 변경 검증 계획
  - 사용자 제공 `모델인풋_판별대장_자동채움_2026-10-06.xlsx` — 2026-10-07 수신, SUPPLY v6 / 다른 역할 v3의 사람 확인 정책. 파일 hash·대장 좌표는 계산 호환성 reference의 최신 검증 기준 참조.
  - 2026-10-07 사용자 확인: 최신 4파일 SUMMARY의 미표시 표는 원본 복제. 기준본은 `docs/references/mi-model-input-baseline-2026-10-07.md`, 표별 대응 기록은 `docs/references/model-input/모델인풋_판별대장_2026-10-06_별도수정본통합_2026-10-07.xlsx`.
  - 2026-10-08 사용자 재확인: TRADE `backup_MidstreamContractInput`(T-187)과 숨김 시트 `Industry research`(T-188)는 시트 전체 미표시·원본 복제(규칙 57 보완).
  - 2026-10-08 양방향 조사 후 사용자 최종 확인: UNIQUE 5셀은 A 원본 복제로 확정. 원본 보존의 직접 근거는 TRADE v8 원본 SUMMARY!J17/J18/J67/J68의 구현 기준이며 모델 미사용은 별도 사용자 확인이다. 규칙 57의 UNIQUE 적용 확인을 따른다.
  - 2026-10-08 사용자 확인: TRADE MidstreamContractInput T-139의 연결된 승인 입력 변경 시 영향을 받는 AG/AI도 함께 갱신한다. 기존 A·UNIQUE 보존은 유지하며 계산 세부와 실행 검증은 후속이다(규칙 59).
  - 2026-10-13 사용자 후속 결정: F-T-01의 Q5 Year SET에서는 AG/AI를 계산하지 않는다. 해당 사례의 원본 수식·저장값 보존 예외는 규칙 59를 따른다.
  - 2026-10-13 사용자 후속 결정: F-T-02의 원본에서 이미 `#N/A`인 셀은 변경하지 않고 그대로 보존한다. 비오류 셀 계산과 새 오류 허용은 별도다(규칙 59).
  - 2026-10-14 사용자 최종 결정: F-T-01/02와 F-D-01은 추가 계산하지 않고 각 지정 입력 외 수식·저장값을 모두 원본 보존한다. 해당 사례의 잔여 계산 대기는 해소하며 일반 입력 변경 정책과 구분한다(규칙58/59).
  - 2026-10-08 사용자 결정: 임시저장 계산 실패는 이번 변경 요청 전체 취소, 동일 사용자·원본 Case당 진행 중 Draft는 1건 재개, 최종저장 실패는 저장 취소다. 실패 요청 취소와 이전 성공 임시저장 Draft의 유지를 구분한다(규칙 15·60·61).
  - 2026-10-08 사용자 확인: TRADE `4.5 PriceModelInput` T-183/T-184는 SUMMARY대로 입력 편집·아래 행 추가 가능한 C다. 기존 시트 전체 읽기 전용/원본 보존을 규칙 44의 표별 경계로 대체한다.
  - 2026-10-07 사용자 확인: DEMAND `BCInput`의 Power Generation `AE:AG`와 GDP·인구 `X`는 화면 미표시·모델 미사용이며 신규 행 수식을 생성하지 않는다. 기존 `AE:AF`는 재계산하지 않고 저장값 Helper는 생성·갱신하지 않는다(규칙 58).
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md` — Case/Run 내부 lifecycle
  - `SECURITY.md` — 접근 범위·파일/임시 자원 보호의 기존 요구
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - 없음
- last synced: 2026-10-15
- open questions:
  - 조사·설계 병행 범위와 미결정 사항별 필요한 근거는 §1A.3~1A.5를 따른다. 공통 초안 작성 가능과 구현 착수 가능은 별도로 판정한다.
  - 구조 변경은 이번 release에 포함하며 주된 추가 방식은 마지막 행·열 추가로 확정했다(규칙 13·26). 표별 추가 경계, 신규 셀 처리와 중간 삽입·삭제 예외의 참조 갱신은 write 계약·실파일 검증에서 구체화해야 한다.
  - 새 행·열에 원본 수식·서식·데이터 유효성 규칙을 어떤 방식으로 복제할지 정해야 한다.
  - 계산 방식은 2026-10-13 사용자 결정으로 POI 부분 계산이다(§1A.4 Q7). 이 방식의 정확도·XLSM fidelity와 격리 worker·동기/비동기 필요 여부를 `INPUT-02A`에서 검증해야 한다.
  - Draft 변경분을 table별 JSON으로 저장할지 정규화된 change row로 저장할지, 계산 결과 snapshot을 영속화할지 정해야 한다.
  - 사용자·source Case당 진행 중 Draft 1건 제한과 기존 Draft 재개는 확정했다. 동시 생성 보장과 완료/폐기 후 새 Draft 생성의 API·저장 표현은 검토해야 한다.
  - 임시저장 계산 실패 및 최종저장 실패 시 해당 요청 취소는 확정했다. revision·계산 결과의 성공 경계와 파일 업로드/Case/Draft 연결 부분 실패의 복구·정리를 설계·검증해야 한다.
  - `BaseEntity`의 `crt_*` 감사 컬럼과 DB 명명 문서의 `reg_*` 표준 충돌을 DBA/데이터 표준 담당자가 확인해야 한다.
  - 기간 축, 셀 원천(BASE/USER), 파생 테이블 표시를 스키마에 넣을지 조회 응답에서만 계산할지 정해야 한다. §14.4 참조.

## 1. Feature summary

독립수급모델 실행 input은 한 번의 실행에서 함께 사용하는 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT`
Excel 파일 4개로 구성된다. `OUTPUT`도 다른 세 역할과 같은 input 역할이다. 화면이 전체 workbook을
추측해서 편집하지 않도록 서버가 아래 구조를
버전별 카탈로그로 관리한다.

`양식 버전 → 파일 → 시트 → 시트 내 테이블 → 필드·수정 가능 범위·구조 변경 규칙`

이 카탈로그는 실제 업로드 파일 bytes나 사용자 수정값을 저장하지 않는다. 실제 파일은 기존
`tcm_filel`의 `SCNRCASE + case_id` 관계가 계속 소유하며, 카탈로그는 파일을 해석하고 검증하는
구조 정보만 소유한다.

사용자 편집은 별도 Draft가 소유한다. 화면은 `임시저장`과 `최종저장`만 제공하며 미리보기는 제공하지 않는다.
임시저장은 Backend가 작업용 workbook의 계산 허용 수식만 재계산하며 변경 저장·계산이 모두 성공하면 변경분·revision과 영향받은 테이블 값을 확정한다. 계산 실패 시 이번 요청의 변경 전체를 취소하고 직전 성공 임시저장 상태를 유지한다.
최종저장은 고정된 원본 snapshot과 전체 Draft 변경분으로 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` XLSM 4개를 다시 만든 뒤,
기존 4파일 Fundamental Model 파일 업로드·Case 생성 서비스를 내부 호출한다. 모델 실행 container는 이 파일 생성 과정에
참여하지 않으며 생성된 파일을 이후 Run에서 소비한다.

## 1A. 조사 미완료 상태의 공통 설계 — 트랙 B 공통 초안

### 1A.1 현재 근거와 확정 수준

| 구분 | 현재 근거·해석 |
| --- | --- |
| 기존 확정 결정 | 4역할을 하나의 양식·source set으로 사용한다. 전체 표시 구조와 선택 표 하나의 값을 조회하고, 카탈로그와 사용자 Draft를 분리하며, 임시저장·최종저장·별도 Run 경계를 유지한다(§5~6, 조회 feature spec §2~3). 구조 변경은 이번 release에 포함한다(규칙 13·26). |
| 표별 편집 확정 — 2026-10-08 | TRADE `4.5 PriceModelInput`의 T-183/T-184는 입력 편집·아래 행 추가 가능한 C다. 기존 시트 전체 제외를 해제하고 두 표의 허용 영역만 Draft/최종 파일에 반영한다. 미표시 T-177~T-182 A 보존 및 계산/구조 검증 경계는 규칙 44·57을 따른다. |
| 보존 확정 결정 — 2026-10-08 | 양방향 조사 후 사용자 최종 확인으로 UNIQUE 5셀은 미표시·모델 미사용·A 원본 복제로 확정했다. `MidstreamContractInput`의 T-137/T-138은 헤더 수식·배열 결과를 보존하고 T-187 backup과 T-188 Industry research는 시트 전체를 보존한다. 사용자 입력 변경에도 이 보존 영역을 갱신·재계산·확장하지 않으며 UNIQUE 구현을 요구하지 않는다(규칙 57의 UNIQUE 적용 확인). 모델 미사용은 사용자 업무 확인이며 실행 검증 결과와 구분한다. |
| OUTPUT 최신 기준 동기화 — 2026-10-08 | v8 사람 확인의 노랑 입력·표시 6표, T-083만 아래 연도 행 추가, 미표시 12표 A 보존을 규칙 50/§6.1에 반영했다. T-082 Helper는 갱신·재계산하지 않는다. 상세 계산 allowlist·정답/모델 사용 검증은 미완료다. |
| 하류 갱신 확정 결정 — 2026-10-08 | TRADE T-139의 연결된 승인 입력이 바뀌면 영향을 받는 AG/AI도 함께 갱신한다(규칙 59). 갱신 필요는 확정됐으며 필요한 선행 계산 범위·기준값/오류 처리·정답·모델 소비 검증은 남아 있다. |
| Draft 동작 확정 결정 — 2026-10-08 | 임시저장 계산 실패는 해당 변경 요청 전체 취소, 사용자·source Case당 진행 중 Draft 1건 재개, 최종저장 실패는 해당 저장 취소다(규칙 15·60·61). 동시성·부분 실패 복구·응답/저장 표현과 실행 검증은 후속이다. |
| 확인된 기술 사실 — 기준본 | 최신 source는 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6이며 정규화본의 70개 업무 시트 보존 검사가 완료됐다(baseline reference). 이는 표 정책·계산 정답·모델 실행 검증 완료를 뜻하지 않는다. |
| 확인된 기술 사실 — 함수 조사 | 고정 기준본의 셀 수식 함수 25종, 조건부서식 포함 29종을 조사했다. POI 5.4.0 기본 등록 27종, 미등록은 UNIQUE와 조건부서식의 ISFORMULA다. 집계·사용 위치·입력 hash·별도 호출 시험은 [함수 reference](../../references/mi-model-input-functions-2026-10-07.md)와 spike 부록 A를 따른다. 기본 등록은 실제 업무 계산 정답이나 XLSM 보존 성공을 뜻하지 않는다. |
| 확인된 기술 사실 — 영향·실행 한계 | 후행 정적 조사에서 보존 영역으로부터 T-135/T-136과 T-139의 AG/AI로 이어지는 수식 경로를 관측했다(함수 reference의 후행 조사 링크). 이 관측은 하류 재계산 허용이나 모델 사용 여부의 판정이 아니다. 이전 3파일 엔진 spike의 POI/LibreOffice `NO-GO`는 당시 실행 증거이며 최신 승인 범위의 성공 검증은 남아 있다. |
| 트랙 A 인계 — 2026-10-08 | 최신 4파일의 우선 항목 A-01~A-10을 정리하고 A-01/A-02의 Midstream 7표 상세 정적 조사를 완료했다. 표시/입력과 사전 계산 영역, 중복 키·빈 슬롯·추가 위치, AG/AI의 보존 기준값·입력 연결을 구별했다. 조사 사실과 후속 정책 결정의 연결은 §1A.3~1A.4를 따른다. 전체 표 조사·계산 승인·구현 준비 완료가 아니다. |
| 잠정 제안 | 표별 차이를 공통 정책 구조에 표현하고, 영향이 큰 미확인 항목부터 조사 결과를 반영한다. 구체 후보·이유와 확정 근거는 §1A.4에 둔다. |
| 미확인 사항 | 최신 전체 표의 경계·행 식별·write/계산/보존 범위, 보존 값을 읽는 하류 처리, 조건부서식 평가 요구, 연결 표 처리, Draft 저장·실패 처리와 엔진 실행 경계가 남아 있다. §1A.4의 조사/검증 결과를 기다린다. |

`INPUT-01/01A/02B`의 과거 완료 기록은 당시 조사 산출물의 완료다. 최신 v8/v6의 상세 조사·정책 확정 완료로 확대하지 않는다. inventory의 9/28 v2 수치와 §14의 SUPPLY 표본 좌표도 현재 seed의 확정값이 아니다.

이번 함수 스캔 완료는 함수 관측·기본 등록 조사 단계의 완료다. 표별 상세 정책과 실제 계산·파일 보존 검증은 미완료다. 이전 `GDP`·`PIPELINE` UDF 후보는 최신 함수 조사에서 오탐으로 정정됐으므로 이 이름의 실행 환경 확보를 현재 선행조건으로 두지 않는다. ISFORMULA는 조건부서식의 원본 보존과 색상 조건 평가 요구를 Q6에서 먼저 나눈다. 함수 호출이 없는 식도 계산 대상일 수 있으며, 저장된 계산값 요소의 존재만으로 유효한 비교 기준값이 확보됐다고 보지 않는다.

판별대장은 [README](../../references/model-input/README.md)에 기록한 사람 확인의 갱신 범위와 자동 스캔 범위를 따로 읽는다. Trade·Output 및 Demand 일부 선언·사람 확인은 v8 내용으로 갱신됐지만 자동 수식/영향 수·시트 집계는 DEMAND·OUTPUT·TRADE v3 / SUPPLY v6 근거가 남아 있다. A/B/C는 원본 복제·값 교체·구조 변경의 분류이며, 조회 여부나 계산 허용 여부를 대신하지 않는다. 계산 호환성 reference의 초기 대장 분류 합계도 이후 통합 대장의 현재 승인 수로 사용하지 않는다.

### 1A.2 확정된 공통 흐름과 책임

`원본 4파일 + 양식 기준 → 화면 조회 → Draft(임시 편집 상태) → 값·구조 변경 → 임시저장 → 최종 XLSM 4개 생성 → 새 Case → 별도 Run`

아래는 기존 §5~6 규칙을 단계별로 풀어 쓴 공통 흐름이다. 작업 책임을 나눈 것이며 클래스·물리 테이블·호출 횟수를 정한 것은 아니다. 표 정책이 아직 미확정이면 그 표의 변경 경계를 열지 않되, 구조 변경 자체는 이번 release에 포함한다. API 세부는 [조회 feature spec](../../feature-specs/mi-fundamental-model-input-provision-feature-spec.md) 및 후속 write 계약, 물리 저장은 [DDL draft](mi-model-input-template-catalog-erd-ddl-draft.md)가 소유한다.

| 단계 | 입력 | 처리 책임 | 결과·유지되는 상태 | 다음 단계 진입 조건 |
| --- | --- | --- | --- | --- |
| 1. 원본·양식 선택 | 접근 가능한 기존 Case의 4역할 파일과 대응 양식 버전 | 기존 Case·파일 관리가 실제 파일을 소유하고 BE가 동일 source set과 양식을 식별한다. 기준본 준비 시의 개발 시트 제거와 런타임 편집을 구분한다(규칙 39~40) | 네 역할이 함께 식별된 원본 기준. 원본 bytes와 양식 정의는 사용자 편집으로 바뀌지 않음 | 4역할 완비·양식 구조 일치·접근 범위 확인. 불일치는 다른 버전으로 자동 fallback하지 않음(규칙 10) |
| 2. 화면 조회 | 선택한 source set·양식, 선택 표 | BE가 표시 정책으로 전체 탐색 구조와 선택 표 하나의 헤더·값·연결 control을 읽는다. FE는 값과 별도 화면 갱신 규칙을 소비 | 사용자에게 보인 원본 기준과 표 값. 조회만으로 Draft·Case를 생성하거나 원본을 수정하지 않음 | 편집할 때 조회 기준과 같은 원본·양식을 Draft에 고정할 수 있어야 함. 호출 분할·최초 선택은 API 정본의 미결정 |
| 3. Draft 시작·재개 | 편집 사용자, source Case·정확한 4파일, 양식 버전 | Draft가 원본·양식의 고정 참조, revision(변경 버전), 사용자 편집 상태를 소유. current MI Base를 다시 선택하지 않음 | `DRAFT`와 고정 기준, 저장된 변경분·revision. 동일 사용자·source Case당 진행 중 1건을 재개(규칙 60) | 편집 소유권과 고정 기준을 확인할 수 있고 허용 정책이 확정된 표만 변경. 동시 생성·재개 계약은 Q8b, source 식별·API·저장 표현은 Q9 |
| 4. 값·구조 변경 | Draft 기준과 사용자 입력·구조 동작 | FE가 허용 규칙으로 편집을 표현하고 BE는 저장 시 같은 정책으로 검증. 기존 셀 값, 신규 행 필수값, 표 끝 추가와 개별 삽입·삭제 예외를 구분(규칙 13·21~28) | 저장할 변경 후보. 화면의 아직 저장하지 않은 값은 영속 Draft 변경분이나 BE 계산 성공을 뜻하지 않음 | 대상/타입/필수값·구조 선행조건·revision을 검증할 수 있어야 임시저장. 실제 요청 단위·연결 동작의 원자성은 Q2~Q4·Q8 |
| 5. 임시저장 | Draft revision과 값·구조 변경 | BE가 변경 저장·revision과 작업용 workbook의 계산 허용 영향 범위 재계산을 하나의 성공 단위로 다룸. 화면 규칙만으로 저장·계산 검증을 생략하지 않음 | 모두 성공한 Draft 변경분·revision과 영향 표 값. 계산 실패면 이번 요청 전체 취소·직전 성공 상태 유지. Case·최종 XLSM은 없음(규칙 15) | 최종저장에 사용할 전체 변경분과 revision을 식별할 수 있어야 함. 임시저장 성공만으로 최종 파일 검증을 대체하지 않음 |
| 6. 최종 XLSM 4개 생성 | 고정 원본 전체 + 대상 revision의 전체 Draft 변경분 + 양식 정책 | BE가 원본 전체를 복제하고 승인 값·구조만 반영한 뒤 허용 계산, 원본 보존, 구조·신규 오류를 검증. 임시 파일을 그대로 승격하거나 모델 container에 생성을 맡기지 않음(규칙 16·20·30~34) | 네 역할이 완비된 검증 결과 파일 set. 아직 새 Case·Run 완료는 아님 | 4파일 생성과 계산·구조·보존 검증이 모두 성공해야 Case 연결. 하나라도 실패하면 불완전 set을 성공으로 전달하지 않음 |
| 7. 새 Case 연결·최종저장 완료 | 검증된 4파일 set, 대상 Draft | 기존 Fundamental Model application service에 4파일을 내부 전달해 새 `INDEPENDENT` Case 생성·파일 연결. Draft와 생성 Case의 대응을 완료(규칙 17~19) | 새 Case와 연결 파일, `FINALIZED` Draft·최종 Case 참조. 같은 Draft의 완료 재시도는 같은 Case 반환 | 최종 Case 확정·4파일 연결 완료 후 별도 Run 요청 가능. 업로드/DB/Draft 연결 사이의 부분 실패·동시 재시도는 Q8c |
| 8. 별도 Run | 완성된 새 Case의 4역할 파일 | 기존 실행 흐름이 실행 접수·이력·Job 전달·결과를 소유. Draft 저장이나 XLSM 생성의 일부로 Run을 시작하지 않음 | Case의 실행 이력·결과. Run 실패를 Draft 계산 실패와 구분 | 실행 가능 상태·재실행 조건은 기존 실행 정본을 따름. Run 재시도를 위해 편집 최종저장이나 Case 생성을 다시 하지 않음 |

Case/Run 내부 상태·재실행 의미는 [공통 Case/Run workflow](scenario-case-execution-workflow-design-draft.md), 실행 endpoint·필드·오류는 [Fundamental Model Case feature spec](../../feature-specs/mi-fundamental-model-mock-api-feature-spec.md)을 참조한다. 이 설계는 편집 최종저장과의 연결 경계만 소유한다.

#### 성공·실패·재시도 경계

| 경계 | 기존 확정 규칙으로 판단 가능한 부분 | 아직 결정·검증이 필요한 부분 |
| --- | --- | --- |
| 조회·Draft 진입 | 구조 불일치를 다른 양식으로 숨기지 않고 고정 source를 사용. 읽기가 원본 쓰기로 바뀌지 않음 | 구조/값 조회 중 부분 실패의 응답·재조회 방식, 조회 후 Draft 생성까지 source 대조 방법(Q1·Q9) |
| 변경 검증·임시저장 | BE도 카탈로그 허용 정책을 검증. 계산 실패는 해당 요청의 값·구조 변경 전체 취소이며 직전 성공 Draft 상태 유지(규칙 15) | revision 충돌·동일 요청 재전송 시 구조 동작 중복 방지와 실패 후 상태/값 대조(Q8a). 원본·카탈로그·Case/최종 파일은 변경하지 않음 |
| 최종 생성·검증 | 전체 원본에서 재생성하고 네 역할을 하나의 set으로 검증. 승인된 기존 오류는 보존하되 신규 오류는 실패. 실패하면 최종저장 취소·이전 성공 임시저장 Draft 유지(규칙 31~32·61) | 작업 자원의 수명/정리·부분 실패 복구 시점(Q7·Q8c). 임시 파일 정리 의무는 SECURITY.md를 따름. 실패 파일을 성공 파일로 취급하는 예외는 없음 |
| 파일 업로드·Case·Draft 완료 | 성공한 Draft당 최종 Case는 하나이며 완료 재시도는 같은 Case 반환. 실패 확정 시 최종저장 취소·Draft 미완료 유지(규칙 19·61) | 업로드 일부 성공·Case 생성 후 Draft 연결 실패·응답 유실·동시 최종저장의 취소/복구 경계(Q8c). DB와 object storage가 하나의 transaction이라고 가정하지 않음 |
| Run | 최종저장과 별도 접수·실행. 모델 container는 편집 파일 생성에 참여하지 않음 | 기존 실행 계약을 참조하며 이 공통 초안에서 새로운 Run 실패/재시도 정책을 만들지 않음 |

#### 조회·수정·재계산·원본 보존의 독립 범위

| 범위 | 기준·책임 |
| --- | --- |
| 조회 범위 | 승인 표시 표의 헤더·값·연결 control. 수정 가능한 셀만으로 좁히지 않는다. 전체 구조는 전체 workbook의 모든 시트·값을 뜻하지 않는다(조회 feature spec §2~3). |
| 수정 허용 범위 | 기존 셀 입력, 신규 행 필수값, 행·열 추가/삽입/삭제를 표별 승인 정책으로 판정한다. 표시·색·수식 유무나 A/B/C 요약만으로 write 권한을 만들지 않는다(규칙 8·21~23·27). |
| 재계산 범위 | 승인된 계산 셀과 영향 경로를 다룬다. 수식이 계산 의미를 소유하고 표 관계는 영향 탐색용이다. A 보존 경계에서 갱신 전파를 멈추며 그 값을 읽는 하류의 자체 계산 허용은 별도 판정한다. 외부 캐시 사용 예외도 표별 적용한다(규칙 33~34·51·54·57, §7). |
| 원본 보존 범위 | 수정·계산·승인된 구조 변경 대상 밖 셀과 package를 유지한다. 기존 오류 보존과 새 오류 실패를 구분한다. 화면 미표시만으로 삭제나 계산 제외를 추론하지 않고 명시된 보존 결정을 따른다(규칙 29~34·56~58). |

읽기 전용 표도 표시될 수 있고, 구조 변경 표에도 보존 셀이 있다. SUPPLY T-119는 표시·읽기 전용·A 보존, T-100은 행 추가와 일부 계산·L열 보존이 함께 존재하며, T-112/T-113은 B 값 변경과 외부 캐시 기반 8셀 재계산이 공존한다. TRADE T-183/T-184의 값·행 추가 허용은 같은 시트 T-177~T-182의 A 보존이나 외부 링크 갱신 금지를 해제하지 않는다. 이는 확인된 차이를 보여주는 사례이며 미확인 표의 기본 정책이 아니다.

2026-10-08 정적 코드 재대조: `FundamentalModelCaseServiceImpl.createCase/validateFiles/uploadCaseFiles`는 정확한 4역할 업로드·Case 생성·파일 연결을 담당하고 `runCase/buildJobArgs`는 별도 Run과 4역할 Job 전달을 담당한다. `createCase`의 예외 경로는 이미 업로드한 파일의 정리를 시도하지만 정리 실패를 기록하고 계속하므로 Draft 최종처리의 멱등성(재시도해도 같은 결과)이나 모든 부분 실패의 복구가 보장됐다는 증거가 아니다. 이 기존 서비스 경계를 재사용하되 Q8c의 연결 설계를 보완해야 한다. Scenario의 `ScenarioModelExecutionServiceImpl.validateFiles`와 `ScenarioExecutionRegistrar.buildJobArgs`는 아직 3파일 경계이며 규칙 48의 구현 잔여다. 코드 대조는 입력폼 XLSM 생성·계산·실행·배포 성공 증거가 아니다.

### 1A.3 조사·설계 트랙의 작업 경계와 반영 방법

- **트랙 A — 기존 시트·표 조사 계속:** inventory §6.2의 기록 흐름과 [통합 판별대장 README](../../references/model-input/README.md)를 유지한다. Q1~Q6의 미확인·충돌 항목에 필요한 관측을 보완하고 기술 관측·사람 확인·미확정 정책을 분리한다. 최신 동일 hash의 함수 전수 조사는 [함수 reference](../../references/mi-model-input-functions-2026-10-07.md)를 재사용한다. 남은 표별 조사를 공통 초안의 선행조건으로 두지 않으며 이번 작업에서 XLSM·XLSX·대장을 수정하지 않는다.
- **트랙 B — 공통 책임·결정 연결:** §1A.2의 흐름과 네 독립 범위, §3의 소유권에 A의 근거를 연결한다. Java 클래스·추가 테이블·JSON 단위·계산 엔진을 선점하지 않는다. 화면 갱신 규칙의 DB 관리 결정(규칙 46)은 유지하되 물리 표현은 DDL 후속 설계로 남긴다.

A 결과를 받을 때 다음 순서로 해당 표부터 반영한다.

1. **같은 근거인지 확인:** baseline 묶음 ID, 역할별 source version과 원본/정규화본 hash, 역할·시트·표 ID, 관측 좌표·대장 위치, 해결할 Q번호, 관측/사용자 확인/미확정의 구분을 연결한다. 정확한 hash는 [baseline·manifest](../../references/mi-model-input-baseline-2026-10-07.md)를 참조하고 domain에 복제하지 않는다. 개발본 SUMMARY의 정책 근거와 정규화 업무 셀의 대응을 함께 확인한다.
2. **정책과 대조:** 기존 §6의 승인 범위와 비교해 일치·미확정 보완·기존 결정 변경 후보를 나눈다. 충돌은 사람의 결정을 거치기 전 기존 규칙을 덮어쓰지 않는다. 대장 T-n/S-n 식별자는 조사 근거의 주소다. runtime catalog의 표 ID와 동일하다고 가정하지 않고 양식 버전 안의 역할·시트·논리 표와 대응을 확인한다.
3. **결정·검증 위치로 전달:** 승인된 업무 정책만 이 domain에 반영하고 아래 영향표로 계산/보존 검증과 API/DDL의 재검토 범위를 고른다. 실행 결과와 보류 이유는 stable work note, 선행조건·티켓은 Delivery Plan에서 각각 관리한다. 이번 범위 밖 정본은 변경 후보만 남긴다.

source 파일의 byte hash는 어떤 실파일을 조사·복제했는지 확인하는 근거이고, template version은 그 파일을 해석할 정책·구조의 버전이다. 둘은 같지 않다. 양식 구조 hash의 구성/정규화와 Draft에서의 식별 저장 방식은 Q9이며, 현 source hash를 그 확정값으로 대신하지 않는다. 새 source가 들어오면 영향 표의 근거 대응을 다시 확인하되 동일 기준본에서 끝난 함수 전수 조사를 반복하지 않는다.

| A 결과·정책 변경 | 공통 설계 재검토 | 계산·보존·구조 검증 재검토 | API / DDL 후속 검토 |
| --- | --- | --- | --- |
| 표 경계·표시·control 연결(Q1·Q6) | §3의 표/부속 control 책임과 표시 정책 | 원본 대응·미표시 보존 경계. 값 계산 의미가 바뀔 때만 계산 fixture까지 확대 | 조회 spec의 구조/선택 표/control 표현. DDL은 기존 정의로 표현 불가하거나 식별/관계가 바뀔 때 |
| 필드·값 편집·행 식별(Q2·Q3) | 기존/신규 셀 허용, clear 의미와 identity | SET/CLEAR 기대값, 잘못된 행 적용 방지, 새 오류·허용 밖 셀 보존 | write 계약·화면 편집 표현. DDL은 식별·허용 정책/변경 저장에 영향이 있을 때 |
| 추가·삽입·삭제·연결 표(Q4) | 구조 동작의 선행조건·함께 바꿀 범위 | 신규 셀·수식/서식 복제·참조/집계 이동·보존과 계산 결과 | write 구조 동작·충돌/재시도 의미 및 catalog/Draft 정책 표현의 DDL 적합성 |
| 계산 허용·보존 경계·하류 기준값(Q5) | §6~7의 계산/보존 정책·영향 전파 경계 | compatibility의 CALCULATE/PRESERVE/STRUCTURE/PENDING, Excel 비교값과 package 보존. 엔진 적용 범위가 바뀌면 Q7 재검토 | 계산 상태·값 출처/실패 표현이 바뀌면 API. 저장할 정책·결과 단위가 바뀔 때만 DDL |
| 화면 갱신 규칙·조건부서식 평가 요구(Q6) | FE 갱신과 BE 계산의 책임 | FE 규칙 적용과 BE 결과 대응, 조건부서식 보존/평가를 분리 | 조회 spec §3.1C; DB 관리 규칙의 표현이 현재 초안에 없으면 DDL 변경 후보 |
| source/양식 대응 변경(Q9) | 과거 Case·Draft의 고정 기준과 양식 version | 새 기준본의 영향 표·계산 기준값·원본 보존 대응 | 조회/write 식별·호환성 및 Case-template/Draft 관계의 DDL. 기존 Draft 자동 전환을 허용하지 않음 |

조사 결과가 없는 표는 승인된 다른 표의 정책을 상속하지 않는다. 직접 formula 참조 0건은 VBA·동적 참조·외부 모델 영향 없음의 증거가 아니다. 이름→일반 셀·Table 계산열 metadata 등 남은 영향은 필요한 표에 연결하고 모델 결과 반영은 Delivery Plan `INPUT-10`에서 확인한다. 규칙 31처럼 별도 모델 사용 검증을 제외하기로 확정된 보존 사례는 그 경계를 유지한다. 이전 inventory 스크립트의 category 기반 후보·과거 family 목록을 최신 allowlist로 사용하지 않는다.

#### 수신한 트랙 A 결과의 설계 반영 — 2026-10-08

인계 정본은 [inventory §6.2의 최신 v8/v6 조사](../../references/mi-model-input-catalog-inventory-2026-09-28.md#최신-v8v6-우선-조사와-trade-midstreamcontractinput--2026-10-08)다. 기준 묶음은 `2026-10-07-delivery-261006-v8-v6`, 상세 대상은 TRADE v8 `MidstreamContractInput` T-133~T-139다. 역할별 원본/정규화본 hash·대장 읽기 시점 hash와 좌표·집계는 해당 reference §1·§3~5에 유지한다. 아래 Q표는 확보한 근거와 남은 정책/검증을 연결하며 원본 수치의 별도 정본을 만들지 않는다.

- **수신 완료 범위:** A-01/A-02의 표 경계·행 식별·기존 입력/빈 슬롯과 AG/AI 선행 조건은 정적 근거를 확보했다. 기존 T-133/T-139의 C·입력 편집·아래 행 추가·열 고정, T-134~T-138 및 UNIQUE의 A 보존을 유지한다. 신규 행 세부와 계산/보존 범위를 새로 승인한 결과가 아니다.
- **후속 상류 확인 완료:** A 원천 BA/BI를 고정해도 AG는 같은 행 Q/R/T, AI는 Q/R/S/T/U 모두 및 같은 표의 다른 행 입력에 연결된다. 승인 입력 Q5:U2918과 기존 상류 집합·최신 수식 표본을 대조한 정적 근거다. AG/AI 자체의 편집 허용, 특정 값 변경의 실행 결과·모델 사용·재계산 승인은 뜻하지 않는다. 경로·검사 한계는 인계 reference §5의 후속 확인을 따른다.
- **공통 책임에 반영:** 값이 있는 끝행, 승인 입력 선언의 끝행, 사전 수식의 끝행을 다른 의미로 다룬다(Q1/Q4). 입력값 조합·Helper를 안정적인 행 ID로 재사용할 수 없다는 근거를 확보했다(Q2). 기존 값 편집과 빈 슬롯 활성화/신규 필수값·clear를 나눈다(Q3). A를 보존해도 AG/AI가 사용자 입력을 읽을 수 있으며 연결 입력 변경 시 갱신 필요는 규칙 59로 확정했다. 하류 계산 세부·검증은 Q5에 남긴다. 표시 입력과 미표시 계산 영역·control을 혼동하지 않는다(Q6).
- **아직 조사 전인 항목:** A-03~A-10은 다음 조사 목록이며 최신 상세 실측 완료로 표시하지 않는다. 순서는 Regas 연결 확장 → Overwrite/연간 LNG 승인 충돌 → LiqUtil 삭제·Demand 선택표/단일 계수 → OUTPUT/BCInput 계산·보존 → PriceModel native Table → control/조건부서식 요구다. 완료된 함수 전수 조사·UNIQUE 후행 graph는 재사용한다.
- **정책 결정 후 인계:** H1의 갱신 필요는 수신·반영 완료이며 계산 세부와 H2~H4는 아래 Open Questions에 남긴다. 추가 답변·검증 후 공통 계산/구조 책임과 compatibility의 검증 표본, 필요할 때만 API·DDL을 재검토한다. 모델 AG/AI 소비는 미검증이며 Backend 검색 0건이나 기존 오류를 미사용 판정으로 쓰지 않는다.

### 1A.4 Open Questions — 확정 사항·잠정 제안·결정 근거

Q1~Q6은 트랙 A의 우선 조사 목록이다. P1은 write·구조·보존 경계를 바꿀 수 있는 항목, P2는 그 경계의 화면 표현 항목이다. Q7~Q9는 트랙 B와 후속 검증에서 해결하며 시트 조사로 대신하지 않는다. 아래 추천은 검토 입력이며 사용자 확인이 필요한 안을 확정 규칙으로 승격하지 않는다. API endpoint·필드·오류나 물리 저장을 이 표에서 정의하지 않는다.

| 설계 항목 | 확정 사항 | 잠정 제안·미결정 | 필요한 조사·검증 또는 사람의 결정 | 결과 반영 위치 |
| --- | --- | --- | --- | --- |
| Q1 / A·P1 — 표 경계·표시 | 전체 표시 구조 + 선택 표 하나의 값. 조회와 쓰기는 별도 범위 | 다중 헤더·빈 입력 셀·공유 control을 표시 정책으로 표현. 표시/입력·미표시 계산·A 표·표 밖 사전 수식 경계를 나눔. 최종 seed는 미확정 | 확보 근거 A-02: T-133/T-139의 최신 헤더/본문과 사전 수식 경계를 구별. A: 나머지 표 대조. 사용자·FE·BE: 표시/양식 대응 확정 및 H2의 입력 끝 처리 | domain §3·6, 조회 spec §2~3, DDL의 초기 seed/경계 검토 |
| Q2 / A·P1 — 행 식별·신규 행 | 화면 행 번호는 영속 식별자가 아님(규칙 28). §7의 surrogate ID 방향 유지 | ID 발급·원본 행/신규 행 대응·정렬/삭제 후 유지 방식 미정. 입력값 조합·Helper를 영속 ID로 간주하지 않음 | 확보 근거 A-02: PNG 전체 입력 4열도 중복 1쌍, LNG AJ Helper는 값 699개에서 256개만 구별. 사용자: H2/H3 빈 슬롯·필수값 결정. BE·DBA: 편집 가능한 키·정렬/추가 후 identity 유지 검증 | domain §5~7, write 계약, DDL의 식별/변경 저장 검토 |
| Q3 / A·P1 — 값 편집 | 명시된 사람 확인과 §6의 허용 범위 우선. Midstream 기존 입력·C 승인은 유지 | 기존 값 편집과 빈 슬롯 최초 입력·신규 행 필수값/clear는 별도. 최신 SUMMARY와 기존 승인 충돌은 변경 후보 | A-02/H3: 현재 완전 입력 행은 신규 필수 승인 근거가 아님. 사용자: clear·0/빈 값·후보/선행 등록 결정. A: OUTPUT 입력 선언/수식 셀 대응과 나머지 계산 경계 검증, Overwrite BCM/추가 위치·Annual LNG Demand 이름 확대 대조 후 변경 채택/기각 | domain §6, 조회/write 계약, compatibility의 승인 fixture; 표현 영향 때 DDL |
| Q4 / A·P1 — 구조·연결 | 이번 release 포함, 표 끝 추가·표별 삭제 예외(규칙13·26·45·47). F-D-05 중간 삽입은 현재 제외. 미러 표는 master 선행(규칙12) | 값 끝·입력 선언 끝·수식 끝은 별도. Midstream의 빈 슬롯 활성화·신규 셀 처리와 A를 움직이지 않는 구조 방식 미정 | 확보 근거 A-02: LNG 값 끝 703·입력 선언 끝 2918·수식 끝 4707이 다름. 사용자: H2/H3, Regas 추가, LiqUtil 삭제/Period, Demand 동시 확장. BE: 논리 표 추가와 A 보존을 함께 검증; 나머지 표는 A-03~A-09 | domain §6~7, 구조 write 계약, compatibility STRUCTURE, DDL 정책 표현 |
| Q5 / A·P1 — 계산·보존·모델 영향 | UNIQUE 5셀과 T-134~T-138은 A 보존(규칙57). F-T-01/02/04는 지정 입력 외 전체 계산 제외·원본 보존(규칙59), F-D-01도 N4 외 계산 제외·보존(규칙58). 지정3사례의 잔여 계산 대기는 해소. 일반 AG/AI 갱신 규칙과 기존/새 오류 구분 유지 | 지정 보존 예외 밖 H1의 선행 계산 범위·기준값/오류 처리와 H4 표 밖 CN/CT 처리 미정. 고정 BA/BI를 읽는다는 사실만으로 유효 기준값이 있다고 가정하지 않음 | 확보 근거 A-01·상류 후속: BA 저장값 전부 빈 값·현재 입력 키와 BI 일치0건. AG는 Q/R/T, AI는 Q/R/S/T/U와 다른 행 입력에 연결. 사용자·모델팀: 지정 사례 밖의 오류·신규 키/H4 업무 필요 결정. BE: 사례별 보존과 승인 계산의 Excel 비교·A 불변·E2E. 보존 사례에는 추가 계산 정답을 요구하지 않음. 나머지는 A-03/A-07/A-08; 규칙31 예외 유지 | domain §6~7, compatibility 검증 경로/fixture, 계산 상태/출처 계약 영향 때 API·결과 저장 영향 때 DDL |
| Q6 / A·P2 — 표별 정책·화면 갱신 | 규칙 46의 양식별 DB 관리와 값 제공 방향. §14는 두 과거 표본이며 전체 유형 분류 아님 | 기간이 행 필드인 Midstream과 열인 표를 구분. 미표시 계산 영역을 control로 승격하지 않음. 다른 표 값/적용 순서·ISFORMULA 보존/평가 요구 미정 | A-01/A-02는 행의 Year·미표시 계산 영역 분리 근거. A-10 조건부서식의 큰 범위를 표 끝/편집 범위로 사용하지 않음. 사용자·FE: 화면/색상 평가 필요; BE·FE·DBA: 규칙 결과와 저장 표현 | domain §3·7, 조회 spec §3.1B~C, compatibility; 화면 규칙의 DDL 후속 설계 |
| Q7 / B·검증 — 엔진·실행 | 함수 기본 등록 조사와 UNIQUE 보존 판정 완료. **사용자 결정(2026-10-13): POI 부분 계산.** 전체 workbook을 POI로 열지 않는다. 사용자 변경에 영향받는 계산 허용 수식과 그 수식이 읽는 셀(상수)만 담은 작은 메모리 workbook을 Apache POI(기존 의존성)로 계산하고, 원본 worksheet XML에서 값이 바뀐 셀의 저장값만 교체한다. 상용 엔진은 평가하지 않는다. 이전 POI `NO-GO`는 전체 workbook 로딩 경로의 판정으로 유지 | 격리·동기/비동기, 표별 계산 allowlist, 구조 변경 시 신규 수식·`calcChain` 처리, `SUMIFS` 외 조건 집계 함수의 오류 조건 의미, 낡은 저장값 갱신 방침 미정. 이 결정은 `INPUT-02A` `GO`가 아님 | 근거: `INPUT-02A` 1단계 노트 Phase 3(10-08 프로토타입), 10-12 4사례 9,758셀 부분 검증. BE: 남은 승인 fixture의 Excel 비교값·배열/package 보존·4파일 시간/메모리·동시 요청·timeout/정리 검증(`INPUT-02A`) | 엔진 spike/compatibility 근거, domain 실행 경계, API 계산 응답·상태, 결과 저장 영향 때 DDL |
| Q8a / B·설계 — 임시저장 실패 | **사용자 결정:** 계산 실패 시 이번 요청의 값·구조 변경 전체 취소, 직전 성공 Draft 상태 유지(규칙 15) | revision/계산 결과 성공 경계·취소 구현, 실패 응답/재조회 표현은 미정. 변경분만 보존하는 이전 추천안은 미채택 | BE·FE: revision 충돌·동일 요청 재전송·계산 실패 후 값/구조/revision/계산 결과가 직전 성공 상태와 일치하는지 검증. Q7 실행 경계 필요 | domain Draft 규칙, write 계약의 실패/멱등 의미, DDL transaction·revision/계산 결과 저장 검토 |
| Q8b / B·설계 — 단일 Draft 재개 | 동일 사용자·source Case당 진행 중 Draft 1건을 유지하고 기존 Draft 재개(규칙 60) | 동시 생성/재개 방식·완료/폐기 후 재생성 API·저장 보장 수단은 미정. 복수 허용은 현재 범위에서 채택하지 않음 | BE·FE·DBA: 동시 생성에도 1건 유지, 소유자/source 기준 재개, 과거 완료/폐기 Draft와 진행 중 제한 구분 검증 | domain cardinality/lifecycle, Draft API, DDL uniqueness 검토 |
| Q8c / B·설계 — 최종처리 부분 실패 | 실패 확정 시 최종저장 취소·이전 성공 임시저장 Draft 유지·FINALIZED 미전환(규칙 61). 완료 Draft당 Case 하나·완료 재시도는 같은 Case(규칙 19) | 일부 업로드/Case 생성 후 실패의 취소·정리/복구 수단, 대상 revision 대응과 transaction·자원 수명은 미정 | BE·FE·DBA: 부분 업로드, Case 생성 후 Draft 연결 실패, 응답 유실·동시 최종저장·revision 변경의 취소/복구 검증. 이미 완료한 요청의 응답 유실은 실패 확정과 구별 | domain 최종처리 lifecycle, write 재시도/실패 계약, DDL Case 연결·최종처리 저장/rollout 검토 |
| Q8d / B·설계 — 저장·계산 결과 | catalog 정책과 사용자 변경분 분리, 변경분·revision 저장 | table별 JSON 또는 change row, 구조 command 순서·계산 snapshot 영속화 미정. 동일 기준의 계산 결과임을 대조할 수 있게 하는 방향 추천(§3.1) | BE·DBA: 저장/복원·동시 수정·구조 동작 재적용·payload 검증. Q7 시간/재계산 비용과 Q8a 실패 의미를 합쳐 결정. FE: 재조회 필요 확인 | domain 논리 책임, write 계약, DDL Draft/change/결과 물리 설계 |
| Q9 / B·계약·DBA — 식별·API·물리 모델 | 조회 표현은 API 정본, 기존 6개 catalog table·수정 범위 row 방향은 DDL 정본. 화면 규칙 DB 관리 유지 | source 파일 hash와 template 구조 hash 구분·판별 방식, version 공용화, field/options/dependency/Draft 확장 저장 미정. API JSON과 DB 형태 동일 가정 안 함 | BE·FE: snapshot/식별·payload·권한 계약. Security·DRM: 구조화 응답 적용 여부. DBA·표준 담당자: 감사 컬럼 충돌/적용. 양식 version 공용화는 파라미터 담당자 조율 | 조회 spec/후속 write 계약, DDL/DBA rollout; 진행·선행조건은 Delivery Plan |

#### Midstream 인계 후 정책 결정 — H1~H4

2026-10-14 최종 예외: F-T-01/02는 각각 Q5/U5 지정 입력 외 전체 계산 제외·원본 보존으로 확정했다. 앞선 AG/AI 및 기존 #N/A 한정 보존을 이 사례의 나머지 수식·저장값까지 확장하며 F-T-04의 지정5입력 외 보존도 규칙59를 따른다. 아래 H1/H4의 계산/오류 처리 미정은 이 지정 보존 사례 밖의 범위다.

트랙 A의 사실 확인 후 H1 갱신 규칙과 사례별 보존 예외는 규칙 59에 반영했다. 2026-10-13 사용자 확인으로 F-T-04의 H2 생성 위치·H3 Q:U 필수 입력 및 지정 5값 입력 후 전체 계산 제외도 확정했다. 다른 사례의 계산 세부/검증·일반 신규 입력 후보/키·비우기·H4 정책은 남아 있다. 기존 입력·C·열 고정과 A/UNIQUE 보존은 재승인 대상이 아니다.

| 설계 항목 | 확정 사항 | 잠정 제안·미결정 | 필요한 조사·검증 또는 사람의 결정 | 결과 반영 위치 |
| --- | --- | --- | --- | --- |
| H1 / Q5 — AG/AI 처리 | 일반 AG/AI 갱신 규칙과 사례별 보존 예외는 규칙59를 따름. F-T-01/02/04는 각각 지정 입력 외 AF/AH·AG/AI·다른 행까지 계산 없이 원본 보존. 기존 A·UNIQUE 보존 유지 | 지정 사례 밖의 AF/AH 선행 범위·빈 BA/없는 키·기존/신규 오류 처리 미정. F-T-01/02의 비오류·다른 행 계산 대기는 해소. 일반 입력 변경으로 확대하지 않음 | 사용자·모델팀: 지정 사례 밖의 기준값 누락·오류·신규 키 처리. BE: 사례별 보존, 승인 계산의 다른 행 합계를 포함한 영향 범위·Excel 비교값·A 불변·모델 소비 검증. 보존 지정 사례의 추가 계산/정답을 재요청하지 않음. 계산 제외/갱신 결정만으로 엔진 GO를 뜻하지 않음 | domain 계산/보존 규칙, compatibility CALCULATE/PRESERVE 및 검증 표본; 계산 상태/값 출처가 달라질 때 API, 저장 영향 때 DDL |
| H2 / Q1·Q2·Q4 — LNG 생성 위치 | 2026-10-13 사용자 확인: F-T-04는 T-139의 704행 빈 슬롯부터 새 계약을 채운다(규칙 59). 열 고정·A 보존 유지, T-133의 다음 논리 입력 행 1105와 구분 | 2919행 확장·worksheet 행 삽입·수식 끝 다음 4708행 생성으로 처리하지 않음. 빈 슬롯 소진 후 범위 확장은 별도 미정 | BE: 원본 Q704:U704 위치·서식, 신규 계약 입력·행 식별 및 같은 시트 A 불변 검증. 생성 위치를 다시 질문하지 않음 | domain 구조 정책, write 생성/충돌 계약, compatibility STRUCTURE; 행 식별/정책 표현의 DDL 검토 |
| H3 / Q2·Q3·Q4 — 필수값·비우기 | 기존 T-133 A:D·T-139 Q:U 입력 승인 유지. F-T-04 신규 계약은 Q:U 다섯 필드 모두 필수(규칙59). F-T-03 U5는 빈 값 금지·기본값 숫자0으로 확정(2026-10-13 사용자 확인·규칙59). 빈 슬롯 생성은 기존 값 수정과 구분(규칙27) | 확정 사례 밖의 연도/국가/Terminal 후보·선행 등록/키, 일반 계약량0/clear·비활성화 의미와 계산 처리는 미정. F-T-03의 기본값0으로 계산/행 비활성화 정책을 추론하지 않음 | 사용자·업무 담당: 나머지 후보/키/사례별 의미. BE: 확정 빈 값 금지/기본값·필수 입력·중복 키 보존·잘못된 행 수정 방지 및 생성 후 계산/보존 검증. F-T-03 빈 값 허용을 재질문하지 않음 | domain 입력/구조 정책, write 검증/clear 의미, compatibility; 후보·identity/변경 저장 영향 때 DDL |
| H4 / Q5 — 표 밖 CN/CT | T-137/T-138의 현재 A 표·배열 보존 유지. F-T-01/02/04는 CN/CT를 포함한 각 지정 입력 밖 전체 원본 보존(규칙59) | 지정 사례 밖 CN11:CN1000·CT30:CT2000의2,961개 사전 수식 고정 보존/부분 계산 경계는 미정. F-T-01/02의 CN/CT 대기는 해소하며 A 표 자체의 갱신 질문과 구분 | 사용자·모델팀: 지정 사례 밖의 표 밖 영역 업무 사용·갱신 필요. BE: 보존 경계·전파/신규 오류 검증. 빈 저장 결과만으로 삭제·입력 슬롯·계산 대상으로 만들지 않음 | domain 표 밖 계산/보존 정책, compatibility; 조회/결과 표현 또는 정책 저장이 달라질 때 API/DDL |

#### 표별 차이를 담을 공통 정책의 논리 항목 — Q1~Q6 제안

아래는 서로 다른 표를 공통 책임으로 읽기 위한 후보다. JSON schema·새 클래스·물리 컬럼 목록이 아니며 빈 정책을 다른 표에서 채우지 않는다. 기존 결정은 §6을 참조하고 미확인 값은 미결정으로 둔다.

| 논리 항목 | 담을 책임·확인할 차이 | 기존 결정과의 경계 |
| --- | --- | --- |
| 근거·대상 연결 | 양식 버전, 역할·시트·표, source 근거·승인 상태의 대응 | source/구조 hash와 runtime ID 표현은 Q9; 조사 T-ID를 DB 키로 확정하지 않음 |
| 표시·조회 | 표/헤더/본문/부속 control의 표시 범위, 읽기 전용 값, 공유 control 연결 | 전체 표시 구조 + 선택 표 하나 유지. 다른 표 값 필요는 추가 조회 계약으로 검토 |
| 필드·행 식별·입력 | 타입·필수/선택 후보·기존/신규 행별 편집·clear·행 대응 | 색이나 좌표만으로 쓰기 허용하지 않음. 영속 identity 발급은 Q2 |
| 구조 동작 | 추가/삽입/삭제 위치와 선행 표, 신규 셀 수식/값/빈칸·서식/validation, 참조/집계 이동 | 이번 release와 표별 예외 유지. Boolean 허용만으로 동작을 열지 않음 |
| 기간축·화면 갱신 | 기간이 행/열/control 중 어디에 있는지, 시작/증분/고정 영역, 연결 control·규칙 적용 순서 | §14의 두 유형을 모든 표에 강제하지 않음. FE 규칙은 BE 계산·저장 검증을 대신하지 않음 |
| 계산·영향 | 승인 계산 셀, 영향 표 관계, 보존 기준값을 읽는 경계·필요한 반환 값 | 원본 수식이 계산 의미를 소유. dependency 관계와 화면 갱신 규칙으로 Excel 계산식을 대체하지 않음 |
| 보존·검증 | 원본 유지 셀/시트/package·기존 오류, 구조 변경 때 유지/생성할 범위, 비교 근거 | A/B/C 편집 분류와 조회/계산/보존은 독립. 승인된 예외를 다른 표에 확대하지 않음 |

### 1A.5 초안 작성과 구현 착수의 별도 판정·다음 순서

- **트랙 B 공통 초안 구체화: 완료.** 단계별 입력·책임·결과·상태·진입 조건(§1A.2), 정보 소유권(§3.1), 표 정책 후보와 미결정별 근거/결정 주체(§1A.4), A 결과 반영 경로(§1A.3)를 정리했다. 문서 상태는 `Draft`이며 이 단계의 완료는 추천안 승인·전체 조사·`INPUT-03/04/STRUCT-01` 확정 완료를 뜻하지 않는다.
- **트랙 A 인계 통합: A-01/A-02 정적 조사 결과를 반영.** Midstream 근거를 Q1~Q6과 H1~H4의 정책/검증 잔여에 연결했다. A-03~A-10은 다음 조사 목록이며 전체 조사·정책 확정은 In Progress다.
- **구현 착수: 현재 `blocked by open questions`.** 조회 구현도 표시 seed·계약 승인이 필요하며, write/파일 생성은 행 식별·표별 구조/계산/보존 정책, Draft 계약·물리 모델과 계산 실행 검증이 필요하다. 이번 초안으로 기존 선행조건을 면제하지 않는다.
- **Delivery Plan 변경 후보:** §4 Dependency waves·§7·§9에 `INPUT-03/04/STRUCT-01`의 **공통 초안 검토**를 조사·`INPUT-02A` 준비와 병행 가능하다고 명시하는 안을 추천한다. 지금 부족한 조사 질문을 일찍 찾을 수 있기 때문이다. 각 티켓의 최종 확정·구현 배정 조건은 유지한다. 이는 현재 dispatch 규칙의 변경 후보이며 이 작업에서 Plan이나 `BLOCKED/READY/DONE` 상태를 변경하지 않는다. 일정 때문에 구조 변경을 다음 release로 미루지 않는다.

다음 순서는 (1) 완료된 함수 스캔·보존 결정을 재사용해 A가 Q1~Q5의 표별 미확인/충돌과 필요한 최신 관측을 우선 조사하는 동안 B가 Q6~Q9의 남은 결정·검증 입력을 구체화 → (2) 확인된 표부터 업무 정책을 정본에 반영하고 Q3~Q6 결과로 계산·보존·구조 fixture를 갱신 → (3) 승인 후보/환경에서 `INPUT-02A` 검증 → (4) 결과를 합쳐 `INPUT-03/04/STRUCT-01`의 미결정을 확정·필요한 ERD/DBA 검토 → (5) Delivery Plan 선행조건을 충족한 구현 slice 배정이다. 이번 단계는 공통 책임 초안을 구체화한 것이며 추가 스캔 실행·계산 PoC·구현·배포는 포함하지 않는다.

#### 다른 정본의 변경 후보 — 반영 상태

- ~~Delivery Plan §7 `INPUT-09`의 `4.5 PriceModelInput` 시트 전체 무수정 문구 정리~~ — **2026-10-08 반영 완료.** Plan `INPUT-09`가 규칙 44의 두 표 승인 변경과 나머지 원본 보존으로 정렬됐다(Plan Change Log 2026-10-08).
- ~~Delivery Plan의 과거 GDP/PIPELINE/UNIQUE 검증 후보 정리~~ — **2026-10-13 반영 완료.** Plan open question과 §9를 오탐 정정·UNIQUE 5셀 원본 보존으로 정렬했다. `INPUT-02A`의 남은 검증 조건은 면제하지 않았다.
- Q1~Q9 결정이 생기면 조회 spec의 표/화면 규칙 표현, 별도 write 계약의 Draft·실패·재시도, DDL의 정책·Draft·최종 Case 대응을 각각 재검토한다. 기존 수정 범위 row 설계를 일반 JSON으로 대체하는 안은 기존 DDL 결정 변경 후보로만 다룬다. Draft JSON/row 미결정과 혼동하지 않는다.
- A-07의 OUTPUT 입력 색·T-082 Helper 보존은 최신 v8 사람 확인에 맞춰 규칙 50/§6.1과 compatibility에 반영했다. 과거 817셀 전체 재계산은 사용하지 않으며 D7:K7·L열 대응과 나머지 계산 범위·정답 검증은 A-07/Q5의 잔여다. A-03의 Regas 참고 범위 확대, A-04의 Overwrite/연간 LNG 선언 차이도 기존 승인 변경 후보이며 미조사 영역에 새 계산/쓰기 정책을 적용하지 않는다. 필요한 좁은 대조·사람 결정 후 domain과 compatibility/API의 영향 범위만 갱신한다.

새 조사 결과를 받을 때 Q1~Q6의 해당 표만 먼저 재검토하고, 계산/연결/구조 정책이 달라지면 Q7 fixture와 Q8~Q9 계약·저장 영향도 함께 확인한다. 사용자 결정은 기존 승인과 충돌하는 변경 후보(Q3), 미확정 구조 정책(Q4), Draft 실패 처리·복수 허용(Q8)에 필요하다. 이번 공통 초안을 기록하기 위해 이 결정들을 먼저 확정할 필요는 없다.

## 2. Existing structure reviewed

- `tmd_mdl_scnrcasem`: 독립실행 case와 현재 상태를 소유한다.
- `tcm_filel`: case input 파일 4개의 실제 파일 metadata와 object storage 연결을 소유한다.
- `tmm_mi_model_param_m`: Agent 파라미터 제안·병합을 위한 일부 파일/시트/셀 위치를 소유한다.
  - 첨부 개발본 76개 시트 중 일부만 포함하고, `model_table` 값이 실질적으로 테이블을 구분하지 못한다.
  - `use_yn`은 Agent 제안 대상 여부이므로 사용자 편집 가능 여부로 재사용할 수 없다.
- `ModelParamExcelMergeService`와 `MergedModelInputFiles`: 파라미터 제안값을 workbook에 병합하는 생성 로직은 이 기능과 책임·입력이 다르므로 재사용하지 않는다. 기존 타입은 3파일 전용이므로 직접 재사용할 수 없다. 생성이 끝난 role별 bytes를 `MultipartFile`로 바꾸는 adapter 패턴만 4파일 결과 타입에 적용할 수 있다.
- `BasecaseReadMode`, `ColumnRoleMap`: 표 읽기 방식과 열 역할 파싱 개념은 재사용 가능하다.
- 실제 파일에는 demand/supply workbook table part가 없으므로 OOXML `ListObject`만으로 테이블
  경계를 자동 생성할 수 없다.

## 3. Proposed domain model

아래 이름은 업무 책임을 나누는 논리 개념이다. Java 클래스나 개념별 물리 테이블 생성을 확정한 목록이 아니다. 표별 공통 정책의 논리 항목 후보는 §1A.4 Q1~Q6을 따른다.

| 논리 개념 | 분류 | 책임 |
| --- | --- | --- |
| InputTemplate | configuration master | 함께 호환되는 4개 input 파일 구조의 버전과 상태 |
| InputFileDefinition | detail | 양식 버전에 속한 파일 역할, 기준 파일명, 순서, 구조 해시 |
| InputSheetDefinition | detail | 파일 안의 시트명, 순서, 표시 상태 |
| InputTableDefinition | detail | 시트 안의 논리 테이블 경계와 행·열 추가 가능 정책 |
| InputFieldDefinition | detail | 테이블 필드·헤더의 안정적인 key, 값 타입, 편집/필수/수식 여부, validation과 선택 후보 연결 |
| InputModificationScope | detail | 테이블 안에서 사용자가 값을 바꿀 수 있는 하나 이상의 직사각형 범위 |
| InputOptionSet | configuration detail | 신규 행·열과 선택형 필드에서 사용할 고정·기준테이블·master·기간축 후보의 출처와 값 집합 |
| InputTableDependency | derived configuration | formula 분석으로 얻은 source table → affected table 관계. 재계산·재조회 범위를 결정하며 값 계산식 자체는 소유하지 않음 |
| ScenarioCaseTemplate | relation | case 파일을 해석할 때 사용한 양식 버전의 고정 참조 |
| ModelInputDraft | transaction | 사용자별 편집 source snapshot, template version, revision, 상태(`DRAFT/FINALIZED/DISCARDED`), 최종 case pointer |
| ModelInputDraftChange | transaction detail | `tableId/row identity/columnKey` 기준 `SET/CLEAR` 변경분과 구조 변경 명령 |

카탈로그의 owner는 플랫폼 운영/BE다. 사용자 또는 tenant별 설정이 아니므로 회사·사용자 FK를
두지 않는다. Draft는 편집 사용자 소유이며 source case와 정확한 원본 파일 4개·template version을 고정한다.
사용자 수정 결과는 최종저장 시 새 독립실행 case의 파일로 저장하고 카탈로그 master를 변경하지 않는다.

### 3.1 정보의 소유권과 파생 관계

| 정보 | 소유·책임 | 관계·경계 |
| --- | --- | --- |
| 양식·표별 정책 | 버전별 카탈로그 / 운영·BE | source 해석, 표시·필드·후보·쓰기/구조·계산/보존·화면 규칙의 승인 경계를 제공. 사용자 입력·계산 결과로 ACTIVE 정책을 수정하지 않음(규칙 3) |
| 고정 원본·양식 참조 | 파일 bytes/metadata는 기존 Case·파일 관리, 편집 기준 참조는 Draft | Draft가 source Case의 정확한 4파일과 양식 버전을 고정. 기준본의 원본/정규화본 증거는 baseline 소유. 복사본/저장 위치·hash 필드 설계는 Q9 |
| 사용자 값·구조 변경 | 편집 사용자 소유 Draft | 원본과 별도로 변경분·revision을 보존. 구조 변경은 사용자 파일 인스턴스에 적용하며 catalog baseline을 이동시키지 않음(규칙 7). 수식·서식 생성은 승인된 구조 동작의 BE 처리 결과로 구분 |
| 계산 결과 | BE workbook 계산 책임의 파생 결과 | 고정 원본·양식·변경분 기준에 종속. 사용자 `SET/CLEAR` 입력이나 catalog 정책을 대체하지 않음. 계산 허용 결과와 원본 cached value(저장된 계산값)를 구분. 결과 snapshot 영속화·실패 상태·기준 대조 표현은 Q7·Q8a/d |
| 화면에 반환할 값 | BE 조회 projection(화면용으로 골라 읽는 처리), FE 화면 갱신 | 원본 조회는 고정 source 값, Draft 편집 조회는 저장 변경과 계산 결과를 연결하는 책임. 연결 방식은 Q8d·write 계약. FE의 아직 저장하지 않은 값/규칙 적용 결과를 BE 계산 성공으로 취급하지 않음. 반환 필드·다른 표 재조회는 API 정본 |
| 최종 파일·새 Case | BE가 검증 생성, 기존 Case·파일 관리가 업로드 이후 소유 | 고정 원본 + 전체 Draft 변경에서 만든 4파일을 새 Case에 연결. Draft는 최종 Case 참조를 보존. 작업용 workbook/계산 snapshot을 원본 파일이나 최종 파일의 정본으로 승격하지 않음 |
| 별도 Run·모델 결과 | 기존 Case 실행 흐름 | 새 Case 파일을 소비하는 별도 실행 이력. 편집 계산 결과와 모델 실행 결과는 목적·생성 시점이 다르며 Run 내부 lifecycle은 기존 실행 정본을 참조 |

계산 결과와 화면 응답은 독립적인 사용자 변경 이력이 아니다. 저장 성공·계산 성공·Case 연결 완료는 서로 다른 사실이므로 한 단계의 성공으로 다른 단계의 성공을 추론하지 않는다. 이 구분을 API 상태/저장 구조에 어떻게 담을지는 §1A.4 Q7~Q9에서 결정한다.

## 4. Table design draft

물리 테이블·컬럼·타입·키·인덱스·nullability와 좌표 저장 타입은
`docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md` §5~7이 소유한다.
기존 6개 catalog table 초안과 필드·선택 후보·dependency·화면 갱신 규칙·Draft의 후속 저장 설계를 구분한다.
이 문서는 §3의 논리 책임만 정리하며 물리명·컬럼·JSON/row 단위를 선점하지 않는다.

## 5. Relationship design

```text
InputTemplate 1 ── N InputFileDefinition
InputFileDefinition 1 ── N InputSheetDefinition
InputSheetDefinition 1 ── N InputTableDefinition
InputTableDefinition 1 ── N InputFieldDefinition
InputTableDefinition 1 ── N InputModificationScope
InputTableDefinition N ── N InputTableDependency
InputOptionSet 1 ── N InputFieldDefinition
InputTemplate 1 ── N ScenarioCaseTemplate ── 1 ScenarioCase
ScenarioCase 1 ── N ModelInputDraft 1 ── N ModelInputDraftChange
ModelInputDraft 0..1 ── 1 finalized ScenarioCase
```

- 한 양식 버전은 파일 역할별 정의를 한 건만 가진다.
- 한 파일은 같은 시트명과 시트순서를 중복해서 가질 수 없다.
- 한 시트는 같은 테이블명과 테이블순서를 중복해서 가질 수 없다.
- 한 테이블은 여러 수정 가능 범위를 가질 수 있다. 범위가 없으면 읽기 전용이다.
- case는 양식 버전 하나에 고정된다. 양식이 나중에 바뀌어도 과거 case 해석은 바뀌지 않는다.
- Draft는 source case, source input 파일 4개, template version을 생성 시점에 고정한다. current MI Base가 바뀌어도 기존 Draft의 source는 바뀌지 않는다.
- 행 대응은 원본/신규 행과 사용자 편집값을 구분해야 한다. Midstream의 입력값 전체 중복과 AJ Helper 중복은 값 조합·Helper를 영속 identity로 재사용할 수 없다는 근거다. ID 발급/보관 방식은 Q2·Q8d·Q9에 남긴다.
- Finalized Draft는 새 독립실행 case 하나를 가리키며 재시도 시 같은 case를 반환한다.
- 관계도의 `ScenarioCase 1:N ModelInputDraft`는 사용자별·과거 Draft를 포함한 관계다. 동일 사용자·source Case당 진행 중 Draft는 1건이며 재개한다(규칙 60). 동시성/저장 보장 수단은 Q8b에서 검토한다.
- source/양식 고정은 참조할 내용을 바꾸지 않는다는 규칙이다. 원본을 별도 파일로 중복 보관할지, 기존 immutable 파일을 참조할지는 Q9의 저장·수명 검토에 남긴다.
- Draft의 `DRAFT/FINALIZED/DISCARDED` 편집 lifecycle에서 `FINALIZED`는 네 파일과 새 Case 연결의 완료를 뜻하며 Run 성공을 뜻하지 않는다. 실패한 임시/최종저장 요청은 규칙 15·61에 따라 취소하며 직전 성공 Draft 상태를 유지한다. 실패 응답/조회 표현과 `DISCARDED` 허용 시점·보존은 Q8의 검토 대상이며 추가 상태·전이를 확정하지 않는다.

## 6. Business rules

1. 모델 유형별 `ACTIVE` 양식은 최대 한 건이다.
2. `ACTIVE` 전환 시 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 파일 역할이 각각 정확히 한 건 있어야 한다.
3. `ACTIVE` 양식과 하위 정의는 직접 수정하지 않는다. 변경은 새 `DRAFT` 버전을 복제해 수정한 뒤 활성화한다.
4. 좌표는 모두 Excel 기준 1-based 숫자다. 열 문자(`A`, `DA`)는 API 표시 시 계산하며 DB 기준키로 쓰지 않는다.
5. 테이블 baseline 경계는 `시작 <= 종료`, header/data 시작은 테이블 경계 안이어야 한다.
6. 수정 가능 범위는 부모 테이블 baseline 경계 안에 있어야 하고 같은 테이블의 다른 활성 범위와 겹치지 않아야 한다.
7. `row_add_psbl_yn`과 `col_add_psbl_yn`은 사용자 파일 인스턴스의 구조 확장 권한이다. 카탈로그 baseline 좌표를 변경하지 않는다.
8. 추가 가능 여부가 `Y`인 테이블도 셀 값 수정 권한이 자동으로 전체 테이블에 생기지 않는다. 기존 셀은 수정 가능 범위로 별도 허용한다.
9. 실제 input 파일명이 달라도 역할 식별은 `DEMAND/SUPPLY/TRADE/OUTPUT`으로 한다. `file_nm`은 기준 양식 표시·검증 정보다.
10. 알려진 양식과 구조가 맞지 않는 파일을 자동 fallback해서 다른 버전으로 읽지 않는다.
11. 삭제된 양식/하위 정의는 신규 case에 사용할 수 없고, 기존 case가 참조하는 양식은 물리 삭제하지 않는다.
12. 식별 열이 다른 표를 미러링하는 표는 단독으로 행을 추가할 수 없다. master 표에 대상 행이 먼저 있어야 한다.
    `row_add_psbl_yn = Y` 는 그 표가 행을 가질 수 있다는 뜻일 뿐, 선행 조건 없이 추가해도 된다는 뜻이 아니다.
    선행 관계를 표현하는 수단이 정해지기 전에는 이런 표의 행 추가를 API로 열지 않는다. 표본은 §14.5.
    - 2026-10-13 사용자 확인: SUPPLY `BCRegasification` 마지막 행 추가 시 `RegasAdjustment`의 연결 행은 자동 추가한다. `RegasAdjustment` 사용자는 행을 직접 추가/삭제할 수 없으며 기존 허용 입력값만 변경한다. master 삭제 허용·연결 삭제 처리와 상세 경계는 규칙51을 따른다.
13. 2026-10-07 사용자 확인: 구조 변경의 주된 추가 방식은 해당 논리 테이블의 마지막 행 뒤·마지막 열 뒤에 붙이는 append다. workbook 전체의 마지막 좌표를 뜻하지 않으며 모든 table의 행·열 추가를 일괄 허용하지 않는다. 중간 삽입·삭제는 개별 table의 명시된 허용 규칙을 따른다. 2026-10-13 사용자 후속 결정에 따라 규칙45의 `DemandOutputOverwrite`는 마지막 행 추가·행 삭제만 지원하고 중간 삽입은 현재 범위에서 제외한다. 규칙47의 `LiqUtilAdjustment` 행·열 삭제는 유지한다. 추후 중간 삽입 요청을 검토할 때 위치 기반 참조, 고정 집계 범위와 열 인덱스의 갱신·보존을 검증해야 한다.
14. 화면 action은 `임시저장`과 `최종저장`만 제공한다. 미리보기는 현재 범위에서 제외한다.
15. 임시저장은 Case나 최종 XLSM을 만들지 않고 변경분·revision 저장과 영향받은 계산 허용 테이블의 재계산을 하나의 성공 단위로 다룬다. 2026-10-08 사용자 결정에 따라 계산 실패 시 **이번 요청의 값·구조 변경 전체를 취소**한다. 요청의 변경분·revision 증가·부분 계산 결과를 성공한 Draft 상태로 확정하지 않고 직전 성공 임시저장의 변경분·revision·계산 결과를 유지한다. 취소를 보장할 transaction·계산 실행 방식과 실패 응답 표현은 후속 write/저장 설계에서 확정한다.
16. 최종저장은 고정 source snapshot과 전체 Draft 변경분으로 XLSM 4개를 처음부터 다시 생성한다. 임시 작업 파일을 그대로 승격하지 않는다.
17. 최종저장은 계산 허용 수식의 workbook 계산, 계산 제외 영역의 원본 보존, 구조 검증과 네 파일 생성이 모두 성공한 뒤 4파일 계약으로 확장된 Fundamental Model 파일 업로드·Case 생성 application service를 내부 재사용한다. Backend가 자기 HTTP Controller를 호출하지 않는다.
18. 최종저장 성공 시 새 `INDEPENDENT` Case는 기존 업로드 흐름의 초기 상태로 생성되고 Draft는 `FINALIZED`와 `finalizedCaseId`를 가진다. Run 접수는 기존 별도 흐름을 사용한다.
19. 동일 Draft의 최종저장 재시도는 새 Case를 중복 생성하지 않고 이미 연결된 `finalizedCaseId`를 반환해야 한다.
20. 모델 실행 container는 편집 결과 XLSM 생성 책임이 없다. Backend가 소유한 workbook materialization/calculation 경계가 값 적용·계산 허용 수식 재계산·계산 제외 영역 보존·구조 검증·XLSM 직렬화를 수행한다.
21. 카탈로그에서 화면에 내려준 편집/추가/선택 규칙은 Backend write validation에도 동일하게 적용한다. 화면 표시 전용 metadata로만 사용하지 않는다.
22. 행·열 추가는 Boolean만으로 허용하지 않는다. table별 `NONE`, `APPEND_ONLY`, 기준 후보 선택, domain command 같은 구조 변경 정책과 수식·서식·validation 복제 규칙이 확정된 경우에만 연다.
23. 선택형 필드와 신규 행·열 header 후보는 Frontend에 하드코딩하지 않는다. 고정 후보, 기준 table, master data, 기간축, 자유입력 중 source type과 validation을 카탈로그가 소유한다.
24. 파라미터 제안의 workbook 병합·파일 생성 로직은 재사용하지 않는다. 새 materialization/calculation 경계가 Draft와 카탈로그 규칙으로 XLSM 4개를 생성한 뒤, 결과 bytes를 `MultipartFile` 4개로 변환해 4파일 실행 준비 흐름에 전달하는 adapter 패턴만 재사용한다.
25. 전수 조사에서 분류한 기존 23개와 `OUTPUT` 5개, 총 28개 화면 대상 sheet는 활성 catalog가 아니라 후보 evidence다. 최종 표시 table 목록과 표 경계를 template version별로 확정한다. 화면 구조·값의 반환 범위는 `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md` §3을 따른다. 조회 대상과 수정 허용 범위는 구분하며 write는 catalog가 허용한 범위만 적용한다.
26. 2026-10-07 사용자 확인: 이번 release는 값 편집과 요청된 구조 변경을 함께 포함한다. 구조 변경을 후속 release로 일괄 미루지 않는다. 행·열 추가 가능 여부는 사용자 확인과 표별 조사 판정을 반영하며 모든 table을 일괄 `rowAdd=false/columnAdd=false`로 두지 않는다. 기본 추가 방식과 예외는 규칙 13을 따른다. 2026-10-06 사용자 확인으로 `ProductionAdjustment`는 **C — 구조변경**으로 분류하며 행 추가 가능을 확정했다. 신규 행 L/N/O셀과 M3 집계 수식 처리는 규칙 31을 따르고 그 밖의 신규 행 식별·서식/validation 및 연결 범위는 규칙 22에 따라 write 계약에서 구체화한다. 이 업무 허용 결정은 구조 변경 API 구현/검증이 완료됐다는 뜻이 아니다.
27. 값이 없는 preformatted row에 첫 값을 넣는 동작은 기존 값 수정으로 간주하지 않는다. 별도 행 생성 정책이 확정되기 전에는 빈 슬롯 활성화를 차단한다.
28. `templateVersion + tableKey + rowNo`는 구조가 고정된 기존 행을 가리키는 임시 row token으로만 사용할 수 있다. 행 추가·정렬·양식 변경을 견디는 영속 식별자로 사용하지 않는다.
29. 화면 미노출과 workbook package 제거는 별개다. 승인된 source snapshot의 hidden sheet는 다른 시트와 계산상 무관한 것으로 업무 확인됐으며, 화면 projection·Draft 변경·dependency 분석·Backend 재계산에서는 제외한다. 최종 XLSM에서는 삭제하거나 다시 만들지 않고 source snapshot의 sheet를 그대로 복사한다.
30. 최종 XLSM materialization은 고정 source snapshot의 원본 workbook 파일 전체를 먼저 복제한 뒤, catalog가 건드리도록 허용한 sheet의 수정 셀·명시된 계산 결과 셀·승인된 구조 변경으로 생성하는 셀만 변경하는 allowlist 방식으로 수행한다. workbook이나 sheet를 새로 조립하지 않는다. 허용 대상 밖 sheet와 cell의 값·수식·cached result·타입·style·validation 및 package 관계는 source snapshot과 동일하게 보존한다.
31. 승인된 template version의 source 파일에 이미 깨진 수식이 존재하는 열은 catalog에 원본 보존 열로 명시한다. 해당 열은 사용자 수정, 임시저장 계산, write validation, 최종저장 재계산 대상에서 제외하고 규칙 30에 따라 원본 상태 그대로 보존한다. `ProductionAdjustment` 월간 표의 `L8:L200`이 첫 확정 사례다. 이 보존이 materialization 검증으로 보장되면 기존 모델 실행 프로그램이 해당 열을 읽는지는 이 기능의 별도 검증 조건으로 두지 않는다.
    - 2026-10-06 사용자 확인: `ProductionAdjustment!L7:L200`은 화면에서 제외한다. `L7`은 헤더이며 오류 수식 보존 범위는 `L8:L200`이다. 화면 제외는 원본 수식/열 삭제나 `0`/공백 대체를 뜻하지 않는다. 해당 조회 필드 범위는 input-provision API spec §2가 소유한다.
    - 사용자 최종 결정: `ProductionAdjustment`의 기존 `L8:L200` 수식과 cached result는 원본 그대로 보존하고 신규 행의 L셀은 빈칸으로 둔다. 신규 L셀에 기존 오류 수식을 복사하거나 `0`/오류 값을 채우지 않는다. 기존 원본 L8:L200은 빈칸 처리 대상이 아니다. 기존/신규 L셀의 화면·재계산 제외는 유지한다. 이 결정은 앞선 신규 L수식 복사/행 참조 변경 허용을 대체한다.
    - 신규 행 N/O셀은 같은 열의 기존 수식을 복사하고 상대 행 참조를 새 행에 맞게 이동한다. 예를 들어 201행의 N셀은 `=IF(M201="","",M201)`, O셀은 `=IF(AND(I201<>0,J201<>0,K201<>0,M201=""),"Error - enter volume adjustment","")`이다. L셀의 빈칸 정책과 별개이며 N/O에 값만 복사하지 않는다.
    - 사용자 최종 확인: `M3`의 집계 참조 `G8:G200`과 `O8:O200`은 추가한 행 수만큼 끝행을 함께 늘리고 기준 숫자도 두 범위의 셀 수에 맞게 증가시킨다. n행 추가 시 두 범위의 끝행은 `200+n`, 기준 숫자는 `386+2n`이다. 1행 추가 시 `=388-(COUNTIFS(G8:G201," ")+COUNTIFS(O8:O201,""))`으로 변경한다. 기존 `COUNTIFS` 조건(G는 한 칸 공백, O는 빈 문자열)은 유지한다. 기존 G열 오류 수식의 신규 행 복사 정책은 별도로 확정한다.
32. 규칙 31의 오류 보존 예외는 승인된 source baseline에 기록된 기존 오류에만 적용한다. 사용자 변경이나 materialization 때문에 새로 발생하거나 기존 범위 밖으로 늘어난 `#REF!` 등 formula error는 생성·검증 실패로 처리한다. 신규 행 L셀은 규칙 31에 따라 빈칸이어야 하며 오류 수식 복사로 신규 오류를 허용하는 예외는 두지 않는다.
33. 다른 workbook을 참조하는 formula cell은 규칙 54의 표별 계산 예외를 제외하고 Backend 재계산 대상에서 제외한다. 이전 표본의 외부 참조 수식 수는 42,394셀이며 현재 계산/보존 범위는 승인된 template source와 표별 정책으로 정한다. SUPPLY·TRADE는 현재도 외부 Excel 원본 없이 입력 파일을 사용하고 있고 앞으로도 같은 방식으로 운영하므로, 외부 파일을 업무담당자에게 요청하거나 시스템 선행조건으로 두지 않는다. 계산 제외 셀은 고정 source snapshot의 formula·cached value를 그대로 보존하며 externalLink part와 relationship, connection 및 관련 package 설정도 유지한다. 외부 파일을 열거나 link를 갱신하지 않는다. 보존 cached value는 Backend가 새로 계산한 결과로 표시하거나 검증하지 않는다.
34. 외부 참조 수식과 내부 계산 허용 수식이 같은 sheet에 있어도 sheet 전체를 계산 제외하지 않는다. 규칙 54의 예외를 제외한 외부 참조 cell은 보존하고 나머지 계산 허용 수식은 별도 검증된 계산 경계에서 재계산한다. 내부 수식이 외부 참조 cell을 입력으로 사용하면 source snapshot의 보존 cached value를 기준값으로 사용하며, 응답과 검증에서는 해당 경로가 외부 기준값을 포함했음을 구분한다. 구체 API field는 feature spec에서 확정한다.
35. `OUTPUT`은 결과 다운로드 파일이 아니라 `DEMAND`, `SUPPLY`, `TRADE`와 같은 실행 input 역할이다. 네 역할은 template, source snapshot, Draft, materialization, Case 연결, Run 전달에서 하나의 원자적 set으로 취급한다.
36. 기존 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT`을 보완하거나 신규 4파일 template에 mapping하지 않는다. 4파일 기능 cutover 전에 case master를 soft delete(`del_yn = 'Y'`)해 목록·상세·상태·결과·입력 조회와 수정·실행에서 제외한다.
    - 2026-10-13 사용자 결정 — **3파일 `SCENARIO` case도 정리한다.** 대상은 active `SCENARIO` case 중 `SCNRCASE + case_id` 파일이 `OUTPUT` 없이 `DEMAND`/`SUPPLY`/`TRADE` 각 1개인 case다. 같은 cutover에서 case master와 함께 그 case의 run history(`tmd_mdl_scnrcaseh`), 그 case에 연결된 execution-preparation(`tmd_mi_model_execpreph.case_id`), 해당 시나리오의 active 가격전망해석을 `del_yn = 'Y'`로 바꾼다. 기존 시나리오 그룹 삭제의 실행 정리와 같은 범위다. 실행하지 않은 제안 row와 Agent proposal 테이블은 바꾸지 않는다. 정리 후 시나리오 화면의 판정은 `mi-model-parameter-proposal-design.md` 규칙 32를 따른다.
    - `INDEPENDENT | MI_BASE` 정리는 기존대로 case master만 바꾼다. `SCENARIO`만 연결 row를 함께 바꾸는 이유는 남은 execution-preparation이 삭제된 case를 가리키면 팝업 조회가 데이터 불일치로 실패하기 때문이다.
37. 규칙 36의 정리는 physical delete가 아니다. 연결된 input/output 파일 metadata와 object, run history, 결과 row는 감사·복구를 위해 보존한다. 별도 보존기간과 승인된 purge 절차가 생기기 전에는 hard delete하지 않는다.
38. 실행 중인 legacy case는 바로 soft delete하지 않는다. cutover pre-check에서 `INDEPENDENT | MI_BASE | SCENARIO` 대상의 `RUNNING`을 0건으로 확인하고, current `MI_BASE`가 legacy 대상이면 soft delete 후 active MI Base가 없을 수 있음을 운영자가 확인한다. 자동 대체 지정은 하지 않는다.
39. 첨부본의 `SUMMARY`와 `플랫폼 입력 안내`는 개발 전용 시트이며 실제 운영 인풋 구성원이 아니다. 두 이름의 시트는 catalog 작성 evidence로만 사용하고 ACTIVE runtime catalog, source snapshot, Draft projection, 최종 XLSM, Case 파일 연결, Run 전달에서 제외한다.
40. 별도 운영 파일은 수신하지 않는다. 현재 개발본 4개에서 개발 전용 시트를 제거해 만든 정규화본을 운영 template source와 source snapshot으로 등록하고, 그 정규화본으로 template version의 구조 hash를 만든다. 최종 materialization 때마다 개발 시트를 삭제하는 방식으로 보정하지 않는다.
41. hidden sheet 보존은 계산 엔진을 거치지 않는 package-copy 책임이다. worksheet part, sheet 순서·이름·`hidden/veryHidden` 상태, 관계, 수식, cached value, style과 연결 package를 source snapshot 그대로 유지하고 hidden sheet cell을 계산 engine input/output에 포함하지 않는다.
42. 최종저장 검증은 source와 결과의 hidden sheet 집합·순서·상태가 같고 hidden worksheet part가 변경되지 않았음을 확인한다. hidden sheet 내부의 기존 formula error도 원본 보존 대상으로 취급하며 새 오류 검사나 재계산 대상으로 삼지 않는다.
43. `SUMMARY` E열이 `직접 입력 대상 아님.`이고 수식·cell formula inbound/outbound·계산용 defined name이 모두 없는 것으로 확인된 visible sheet 6개는 write allowlist, Draft 변경, dependency graph와 Backend workbook 재계산에서 제외한다. 최종 XLSM에는 source snapshot 상태로 보존한다. 화면 표시는 아래와 같이 나눈다. 이 계산 제외는 입력폼 Backend 경계이며 이후 모델 실행 프로그램이 파일을 읽는 범위를 축소하지 않는다.
    - read-only 표시 대상: 화면 조회에 source snapshot 값을 제공한다.
      - SUPPLY: `LNG Summary`
    - TRADE `backupLNG_TradeRestrictionInput`은 2026-10-07 규칙 57에 따라 화면 미표시로 변경했다. 원본 보존은 유지한다.
    - 화면 미표시 대상: 값·수식이 없는(`A1`) 구분 탭이다. 화면 projection에서 제외하고 최종 XLSM에는 원본 그대로 복제한다. 2026-10-01 사용자 확인.
      - DEMAND: `Input Adjustment ->`, `기준정보 ->`
      - SUPPLY: `Input Upstream ->`, `Input Midstream ->`
44. 2026-10-08 사용자 확인: TRADE `4.5 PriceModelInput`의 T-183 `JKM - Base Case`와 T-184 `TTF - Base Case`는 **C — 구조 변경**이며 입력값 편집과 각 표의 마지막 데이터 행 아래 행 추가를 허용한다. TRADE v8 `SUMMARY!F63:F64`·`H63:J64`와 현재 판별대장 사람 확인을 따르며 기존 시트 전체 읽기 전용·원본 보존 결정을 대체한다.
    - 기존 입력 범위는 T-183 `AI4:AK35`, T-184 `AM4:AO59`다. 고정 제목·헤더 `AI2:AK3`·`AM2:AO3`은 조회만 제공한다. 입력 필드는 `Year`, `Country`, `Uncontracted Volumes/Mt LNG`이며 가격 입력으로 해석하지 않는다. 신규 행도 해당 표의 입력 필드를 확장하고 열/기간 축은 고정한다. 다른 표의 범위·값을 함께 변경하지 않는다.
    - 두 표의 승인 입력과 행 추가는 write allowlist·Draft 값/구조 변경·최종 XLSM 반영 대상이다. 같은 시트의 미표시 계산표 T-177~T-182는 규칙 57의 A 원본 복제를 유지하며 U열도 화면 projection에서 제외한다.
    - 최종 XLSM은 규칙 30에 따라 원본 workbook 전체를 먼저 복제한 뒤 두 표의 승인 변경 및 필요한 구조 metadata만 반영한다. 그 밖 셀·수식·cached value·style·관련 원본 관계/package는 보존한다. 기존 외부 참조 수식·저장값·externalLink 보존은 규칙 33·54를 따르며, 입력/행 추가 허용만으로 재계산·외부 링크 갱신을 승인하지 않는다. 신규 행 필드 검증·서식/validation·범위와 참조 갱신의 상세 write 계약 및 실파일 검증은 별도 잔여다.
45. DEMAND `DemandOutputOverwrite`는 표 2개(`Demand Model Manual Overwrite Sector Input` `D6:K507`, `Demand Model Manual Overwrite Monthly Input` `M6:S507`)로 구성되며 최종 분류는 둘 다 C(구조 변경)이다. 노랑 입력 영역(`D8:I507`, `M8:Q507`)에는 수식이 없다. **2026-10-13 사용자 최종 결정: 각 논리 표의 마지막 데이터 행 아래 행 추가와 행 삭제만 지원한다. 중간 행 삽입은 현재 범위에서 제외하며 열 추가는 불가하다.** 이 결정은 2026-10-01의 중간 위치 포함 행 추가 승인과 앞선 추가 위치 재검토를 대체한다.
    - **F-D-05 추가/삭제 검증 경계:** 마지막 행 추가와 행 삭제는 독립 사례로 검증한다. 중간 행 삽입은 나중에 실제 요청이 들어오면 별도 범위/정책·구현/검증을 검토하며 현재 미결정/필수 구현 목록으로 남기지 않는다. 실제 추가 대상 표·행/신규 값·필수값, 삭제 대상/범위·삭제 후 데이터 이동 및 K/S·옆 표의 좌표/참조/저장값 처리와 metadata 기대값은 후속 write/검증 명세에서 확정한다. worksheet 전체 행 추가/삭제를 자동 허용하지 않는다.
    - **2026-10-15 F-D-05-APPEND 사용자 확정 — Sector508·추가 계산 제외:** 같은 정규화 DEMAND의 부문별 표(T-018)만508행까지 논리 확장한다. `D508:I508`은 원본 `D19:I19`의 값·타입·실제 빈칸을 그대로 복사하며, 앞서 설명한 `D508:K508` 서식/신규 빈 J508/K508·사용 범위 확장만 반영한다. 기존 `K8:K507`·`S8:S507` 수식/저장값과 J/R·Monthly·나머지 원본은 보존하고 신규 Helper 수식/수요 환산·추가 계산은 하지 않는다. 최종 읽기 전용 Excel 재개방은 재계산·링크 갱신·매크로 실행 없이 확인한다. 기존 빈20슬롯 활성화·worksheet 전체 행 삽입·월별 표 확장이나 일반 필수값/기본값 정책으로 확대하지 않는다. 실제 구조/보존/native 상태는 [compatibility F-D-05-APPEND 후속](../../references/mi-model-input-sheet-calculation-compatibility.md)을 따른다.
    - **2026-10-15 F-D-05-DELETE 사용자 확정 — 원본 Sector19 삭제·K 참조/저장값 동반 이동:** 같은 정규화 DEMAND의 부문별 원본 `D19:K19`(China/2026년12월/Losses)를 삭제하고 `D20:K507`을 `D19:K506`으로 이동한다. J 값과 K 수식/저장값은 해당 원본 기록과 함께 옮기며 K 수식의 참조·공유 범위·계산 순서 목록을 새 위치에 맞춘다. 이동 밖에 남은 공유 master의 관련 범위 속성도 조정한다. `D507:K507`은 기존 서식의 실제 빈 슬롯으로 두고 추가 계산은 하지 않는다. 이 구조 변경에 필요한 삭제/좌표/참조 변화는 아래 K 원본 보존의 지정 사례 예외이며 나머지 저장값·타입·서식과 S/옆 Monthly·헤더·다른 시트/A·물리 행/열/사용 범위는 원본 보존한다. 재계산·링크 갱신·매크로 실행 없이 읽기 전용 Excel 재개방을 확인한다. APPEND 결과를 입력으로 쓰거나 worksheet 전체 행 삭제·일반 K/S 계산 승인으로 확대하지 않는다. 실제 검증 상태는 [compatibility F-D-05-DELETE 후속](../../references/mi-model-input-sheet-calculation-compatibility.md)을 따른다.
    - 화면 표시: 노랑 입력 영역이 있는 열만 표시하고 `J`·`K`열과 `R`·`S`열은 화면 projection에서 제외한다.
    - 계산: `K`열과 `S`열의 수식은 현재 Backend 재계산 대상이 아니며 원본 그대로 보존한다. 행 추가 시 두 열의 수식 확장도 현재는 하지 않는다. 두 열 모두 외부 참조·`#REF!`·`INDIRECT`·`OFFSET`이 없어 서버 계산이 가능한 수식 500개씩이다.
    - 재검토 조건: 화면 표시 여부와 무관하게 모델 실행이 `K`·`S`열 값을 사용하게 되면 재계산과 행 추가 시 수식 확장을 이 규칙에서 다시 정한다.

46. 2026-10-06 사용자 전달 FE 합의: 화면에는 수식 원문 대신 셀 값을 제공하고, 입력 변경·열 추가로 화면 값이 바뀌어야 하는 동작은 카탈로그가 소유하는 별도 갱신 규칙으로 전달한다. 사용자 후속 결정에 따라 이 규칙도 양식 version별 카탈로그 데이터로 DB에 저장하고 BE가 읽어 제공한다. FE는 전달된 규칙으로 화면 값을 갱신하며 수식 셀인지 판별하거나 Excel 수식을 직접 해석하지 않는다. `LiqUtilAdjustment`의 마지막 월 열 추가 시 새 Period를 직전 Period + 1로 만드는 동작이 확정 사례다. `H7:CL7`은 시트 내부 참조 수식이며 화면 갱신 규칙은 실제 수식 확인 후 구체화한다. 2026-10-07 사용자 확인으로 수식 범위를 정정했으며 `G7`은 시작 연도 값으로 수식 범위에서 제외하고 기존 고정 헤더 정책을 유지한다. 화면 갱신은 규칙 15–17/20–21의 BE 저장 검증·계산 허용 수식 처리·최종 파일 생성 책임을 대체하지 않는다. 규칙 전달 범위와 소비자 계약은 input-provision feature spec §3.1C가 소유한다. 물리 저장 단위와 테이블·컬럼은 기존 catalog ERD/DDL 후속 설계에서 확정한다.

47. 2026-10-06 사용자 결정: SUPPLY `LiqUtilAdjustment`는 행 삭제·열 삭제가 가능하다. 행 삭제 시작 위치와 월 열 삭제 후 Period 정책은 아래 사용자 후속 결정을 따른다. 실제 삭제 대상 범위·나머지 위치 제한, 수식/참조/갱신 규칙 적용 방식은 별도 write 계약에서 확정한다. 삭제 허용 결정만으로 고정 헤더나 모든 workbook 영역의 삭제 범위를 확정하지 않는다. 삭제 정책도 기존 카탈로그 DB 설계에서 표현해야 한다.
    - **2026-10-13 F-S-05-ROW 사용자 후속 결정:** 행 삭제는 **헤더를 제외한 11행부터** 가능하다. 헤더는 삭제하지 않는다. 실제 삭제할 데이터 행/범위와 삭제 후 남은 행 식별·수식/참조·집계/metadata의 기대 변경은 실행 명세에서 별도로 확정한다. 삭제 시작 행이 정해졌다는 이유로 11행을 검증 대상 행으로 자동 선택하거나 worksheet 전체 행 삭제를 승인하지 않는다. 열 삭제(F-S-05-COLUMN)와 삭제 후 Period 재번호화 정책은 이번 행 답변으로 확정하지 않는다.
    - **2026-10-14 F-S-05-ROW 사용자 확정 — 원본11행 삭제·추가 계산 제외:** 같은 정규화 SUPPLY source의 `LiqUtilAdjustment` **원본11행 `Australia Pacific LNG T1`**을 설명한 논리 표 범위대로 삭제한다. `B11:CL11`을 제거하고 원본 `B12:CL407`의 값·타입·서식을 설비 식별과 함께 `B11:CL406`으로 이동하며 `B407:CL407`은 기존 서식을 유지한 실제 빈 입력 슬롯으로 둔다. header `B7:CL10`·`H7:CL7`83수식/저장값 및 나머지 원본 수식·저장값·서식/설정은 **추가 계산 없이 보존**한다. 물리 row/column·dimension·조건부서식·숨은 Filter 이름·다른 셀/backup/A 영역과 다른 시트는 원본 그대로 유지한다. 계산 및 Excel 계산 정답 비교는 해당 없음(PRESERVE)이며 새 파일의 저장 후 재읽기/구조·보존·읽기 전용 Excel 재개방을 검증한다. 월 열 삭제·worksheet 전체11행 삭제나 다른 구조 사례의 계산 정책으로 확대하지 않는다. 실제 파일/검증 상태는 [compatibility F-S-05-ROW 후속](../../references/mi-model-input-sheet-calculation-compatibility.md)을 따른다.
    - **2026-10-13 F-S-05-COLUMN 사용자 후속 결정:** 같은 시트의 월 열을 삭제한 뒤 **남은 Period(기간 번호)는 기존 번호를 유지**하며 재번호화하지 않는다. 삭제된 기간의 빈 번호를 메우기 위해 남은 기간 번호를 당기지 않는다. 실제 삭제할 월/열·범위와 이동 후 헤더 수식/참조·다른 표 참조·서식/validation·집계/metadata의 기대 변경은 실행 명세에서 별도로 확정한다. Period 유지 결정만으로 헤더 수식을 literal 값으로 바꾸거나 worksheet 전체 열 삭제를 승인하지 않는다. 행 삭제(F-S-05-ROW)와 독립 사례로 검증한다.

    - **2026-10-15 F-S-05-COLUMN 사용자 최종 확정 — 원본H월 삭제·Year82 계산:** 같은 정규화 SUPPLY source의 `LiqUtilAdjustment` **원본 H열(2025년2월·Period86)**을 추천안대로 논리 표에서 삭제한다. H7:H407을 제거하고 원본 I7:CL407을 H7:CK407로 월별 값/타입/서식과 함께 이동하며 CL7:CL407은 기존 서식의 실제 빈 슬롯으로 둔다. 남은 Month/Period는 원본 의미/번호85,87…168을 유지한다. 새 H7=`IF(H8=1,G7+1,G7)`로 생존G7을 읽고 다른81개 IF도 새 좌표의 현재Month/앞Year로 조정한다. **H7:CK7 82셀만** 동일 조건 Excel 정답을 확보해 독립 POI 부분 계산/비교한다. shared 범위/chain의83→82 및 좌표 이동은 승인 구조 변경이며 source Year cache/golden을 계산 입력·결과 생성에 사용하지 않는다. G월/설비·나머지 원본 수식/저장값·서식/설정·backup/A/다른 시트는 보존하고 최종 readOnly 재개방은 재계산/링크 갱신/매크로 실행 없이 확인한다. F-S-05-ROW 결과·worksheet 전체H열 삭제·다른 구조 사례의 계산 승인으로 확대하지 않는다. 정확한 구조/metadata 기대와 실제 파일/계산/native 상태는 [compatibility F-S-05-COLUMN 후속](../../references/mi-model-input-sheet-calculation-compatibility.md)을 따른다.

48. 독립실행과 Scenario 실행은 모두 `DEMAND/SUPPLY/TRADE/OUTPUT` 4파일을 실행 input으로 사용한다. Scenario의 두 입력 mode와 OUTPUT의 제안·병합 제외 및 원본 보존 책임은 `docs/design-docs/gas/mi-model-parameter-proposal-design.md` §1A가 소유한다. 이는 독립 입력폼에서 승인된 OUTPUT 값 편집을 금지하는 규칙이 아니다.

### 판별대장에서 동기화한 계산·보존 정책 — 2026-10-07

49. DEMAND `GDPPopulationAdjustment`, `ElectrificationRateAdjustment`, `PowerMixAdjustment`, `CoalGasAdjustment`, `WeatherAdjustment`, `PMIAdjustment`, `HydrogenAdjustment`, `LNGBunkeringAdjustment`의 대장 T-002~T-017은 입력 영역의 행 추가를 허용하고 열 추가는 금지한다. 노랑 밖 기존 수식은 현재 재계산·신규 행 수식 확장을 하지 않고 원본 상태로 보존한다. 이 결정은 해당 표의 수식 처리이며, 미확정 downstream 표의 계산 허용을 자동 확정하지 않는다. `General`은 사용자 입력·행·열 추가 없이 원본 복제한다. `BCInput`의 과거 원본 복제 정책은 최신 v8에 대한 규칙 58로 대체한다. 대장의 자동 영향 수식 수를 재계산 허용 목록으로 사용하지 않는다.
50. DEMAND `LNG Demand`의 T-020 `LNG Demand Mode`는 **E — 부속 control**이다(2026-10-07 사용자 확인). 값 셀은 C1이고 기존 선택값 변경 허용을 유지한다. 독립 데이터 표가 아닌 T-021 `Annual LNG Demand Reference`의 부속 control로 그 표 데이터와 함께 전달하며 기존 T-020 ID는 값 소유 영역으로 유지한다. T-021은 연간 값 편집·열 추가를 허용한다. T-020 원본 드롭다운 선택지의 `Dynamaic`은 `Dynamic`의 오타로 확인됐다(대장 T-020, 2026-10-01). 원본 파일을 고칠지와 화면 선택지·저장값을 어느 표기로 둘지는 별도 결정이다. T-021의 기존 합계 수식 130개는 재계산한다. 추가 열의 합계는 같은 행 마지막 열 수식을 열 참조 이동해 복사하며 합계 행은 4·5·10·15·34·35·43·64·76·89·100·104·109다. 24행은 수식이 아닌 값이다. OUTPUT은 아래 §6.1의 최신 v8 입력·표별 구조/보존 정책을 따른다. 과거 파랑 입력·Helper 포함 817셀 전체 재계산 설명은 이 정책으로 대체한다. Scenario OUTPUT은 규칙 48의 별도 원본 사용 계약을 따른다.
    - 2026-10-13 사용자 후속 결정 — **F-D-04 빈 값 금지·기본값0:** DEMAND `LNG Demand!E6`는 빈 값을 허용하지 않으며 기본값은 **숫자0**이다. 기존 F-D-04의 실제 빈 셀 CLEAR는 허용 입력 사례가 아니므로 거부 기대값으로 정리한다. 기본값0은 유효한 숫자 입력이며 실제 빈 셀·빈 문자열과 구분한다. CLEAR 명세를 명시적 SET0으로 바꾸어 실행 완료로 기록하지 않는다. 기본값을 적용하는 구체 시점/처리 방식은 후속 write 계약에서 다루며 이 결정만으로 모든 빈 요청을 자동0 보정하는 동작을 정의하지 않는다. 다른 셀의 빈 값/기본값 정책이나 규칙50의 승인 합계 계산 범위를 변경하지 않는다.
    - 2026-10-13 사용자 후속 결정 — **F-D-03 마지막 연도 열의 입력·자동 합계:** 사용자가 F-D-05로 지칭한 `LNG Demand` 마지막 연도 열 추가는 기존 F-D-03에 대응한다. 새 O열의 연도 O3과 국가별 값만 사용자가 직접 입력하고 total/subtotal은 수식을 넣어 자동 계산한다. 입력 범위는 `O3`, `O6:O9`, `O11:O14`, `O16:O23`, `O26`, `O28:O33`, `O36:O42`, `O44:O63`, `O65:O75`, `O77:O88`, `O90:O99`, `O101:O103`, `O105:O108`이다(연도1·국가90셀).
        - 새 O열의 합계 행은 **4·5·10·15·24·25·27·34·35·43·64·76·89·100·104·109의16셀**이며 직접 입력으로 덮어쓰지 않는다. 기존13합계 행은 N열 수식을 열 참조 이동해 복제한다. 원본24·25·27행은 숫자0 literal이지만 사용자 후속 확인으로 **새 O열에만 `O25=SUM(O26)`, `O27=SUM(O28:O33)`, `O24=SUM(O25,O27)`**를 추가한다. 수식이 없다는 원본 관측만으로 이 소계/합계 행을 사용자 입력으로 만들지 않는다.
        - 이 연도 열 추가 사례의 **기존 E:N 입력·수식·저장값은 원본 보존**한다. 기존24·25·27행 literal이나130합계 수식을 변경/재계산하지 않는다. 이는 F-D-03 새 열 추가의 보존 경계이며 기존 입력 변경 사례 F-D-02의130합계 계산 규칙은 유지한다. F-D-05 `DemandOutputOverwrite` 행 변경과 F-D-04 E6 빈 값 금지/기본값0 정책을 다른 사례/새 국가 입력 전체로 확대하지 않는다. 신규 연도/본문값·구조 metadata 기대값과 동일 조건 Excel 정답에 따른 실제 파일/계산 검증은 compatibility에 연결한다.
51. SUPPLY `RegasAdjustment`의 직접 값 수정은 T-106 `H11:AE382`로 제한한다. `B:G`·`AF:DK`·`DM:DV`는 사용자 쓰기 금지인 연결 수식이다. T-107 연간 합과 T-108 국가별 월 합은 계산·연쇄 갱신 대상이다.
    - **2026-10-13 F-S-04-ROW 사용자 결정:** `BCRegasification` T-117은 각 논리 표의 마지막 데이터 행 아래 행 추가와 행 삭제를 허용한다. `RegasAdjustment`에서는 사용자가 행을 직접 추가하거나 삭제할 수 없고 기존 허용 입력값만 변경한다. `BCRegasification`에서 행을 추가하면 같은 설비의 `RegasAdjustment` 행을 자동 추가한다. 판별대장의 과거 행 추가 칸 빈칸을 현재 허용 정책으로 사용하지 않는다.
    - 자동 추가는 두 표의 마지막 설비 행을 연결해 생성하며 기존 국가별 합계 블록을 추가 행 수만큼 아래로 이동하고 SUMIFS의 고정 끝행을 확장하는 기존 연결 규칙을 따른다. 실제 신규 설비/필수값·식별 대응·자동 입력/수식·서식/validation·집계/names/Table/metadata 변화와 동일 조건 Excel 정답은 F-S-04-ROW 실행 명세에서 확정한다. 한 시트만 변경한 것으로 연결 추가 성공을 판정하지 않는다.
    - **2026-10-15 F-S-04-ROW 지정 검증 사용자 승인:** 독립 정규화 source의 `BCRegasification`380행에 원본8행 B:F 식별값·G:Q 연간 용량을 값으로 복사하고 E380만 `검증용 신규 설비`로 바꾼다. 연결 `RegasAdjustment`383행의 H:AE 과거24개월은 숫자0으로 지정한다. 기존 A380 Helper 수식을 유지하며 A380/R380, 연결 B383:G383·AF383:DK383·DM383:DV383의102셀과 국가 블록1행 이동/끝행383 확장 후 H388:DK445의6,264셀, **총6,366셀만** 동일 조건 Excel 정답 확보·POI 계산/저장·비교한다. 기존 다른 설비의 수식·저장값/국가 목록58개·기간은 보존한다. 최종 재개방은 읽기 전용·재계산/링크 갱신/매크로 실행/저장 없이 확인한다. 이 지정 fixture의0 입력과 계산 범위를 일반 기본값/전체 재계산 정책이나 검증 완료로 확대하지 않는다.
    - 행 삭제는 F-S-04-DELETE로 추가와 독립 검증한다. **2026-10-13 사용자 후속 확인:** `BCRegasification`에서 설비 행을 삭제하면 **같은 설비의 `RegasAdjustment` 연결 행도 자동 삭제**한다. 사용자의 Regas 직접 삭제 금지는 유지하며 master 변경에 따른 자동 구조 처리와 구분한다. 실제 삭제 대상/범위·같은 설비의 행 대응·삭제 후 이동/남은 행 식별·수식/참조/집계/metadata의 기대 변경은 실행 명세에서 별도로 확정한다. 행 위치 참조 관측만으로 정한 정책이 아니라 명시적인 사용자 확인에 따른 결정이다.
    - **2026-10-13 F-S-04-COLUMN 사용자 후속 결정:** `BCRegasification`의 연도 추가를 허용한다. 사용자가 지정한 동작은 **R열을 추가한 열 개수만큼 옆으로 복사하고 R열 수식을 계산**하는 것이다. 추가 연도 외 신규 수치 입력은 **빈 값을 허용하지 않고 기본값은 숫자0**이다. 기존 셀의 빈 값 전체를0으로 바꾸거나 연결 수식을 기본값0으로 덮어쓰는 정책은 아니다.
        - **2026-10-14 사용자 확인 — 최대 용량 열 이동·전체 연도 포함:** 현재 R열의 `Maximum capacity`를 추가 연도 열 수만큼 오른쪽으로 옮기고, MAX는 **기존 첫 연도 G열부터 새 연도 마지막 열까지 모두 포함**해 계산한다. 연도1열 추가 예시는 새 연도 R/최대 용량 S, `S8=MAX(G8:R8)`이며 2열 추가 예시는 새 연도 R:S/최대 용량 T, `T8=MAX(G8:S8)`다. 같은 행의 전체 연도 용량을 대상으로 하며 단순 상대참조 복사로 첫 연도가 빠지는 `MAX(H8:R8)`를 사용하지 않는다. 이동/범위 해석 대기는 해소됐으며 실제 추가 연도/개수와 연결 수식/참조의 기대 변경은 F-S-04-COLUMN 실행 명세에서 확정한다. 이 예시는 실제2036년1열 검증값 선정으로 간주하지 않는다.
        - **2026-10-14 사용자 최종 결정 — 연도 추가의 Regas 보존:** `BCRegasification` 연도 추가에서는 `RegasAdjustment`를 연결 갱신하지 않는다. Regas의 기존 셀·수식·저장값·기간·구조/관련 설정을 원본 그대로 보존하며 G열 lookup 인덱스/범위 수정, 월 수식 확장, 연간/국가 합계 이동·재계산을 하지 않는다. 이 사례의 계산은 master의 이동한 최대 용량 수식으로 제한한다. 기존 R을 가리키는 Regas 수식도 보존 대상이며 저장값 보존을 새 계산 정답으로 취급하지 않는다.
        - Regas 자동 연결 갱신은 위 행 추가/삭제 결정에 적용한다. 연도 추가의 과거 연결 기간 확장 후보는 이 최종 결정으로 대체하며 재질문하지 않는다. 기존 Regas 직접 값 변경/연간·국가 합계 계산 정책은 다른 사례에서 유지한다. 국가 목록은 추가하지 않으며 기존 설비명 첫 일치 lookup 의미와 중복 이름 정책도 변경하지 않는다. 이 규칙은 구조 write/파일 검증·제품 구현 완료를 뜻하지 않는다.
52. SUPPLY `LiqUtilAdjustment_backup`, `LiqProdAdjustment_backup`, `BCLiquefaction_backup`은 원본 보존 대상이다. `LiqUtilAdjustment_backup`을 읽는 활성 `LiqProdAdjustment!CM11:CM407`의 기존 오류 경로는 입력 변경에 따른 계산 정답 검증과 분리한다. 보존하기로 정한 기존 오류를 새 계산 실패로 취급하거나 0/공백으로 고치지 않는다. 활성 표의 나머지 계산 허용 범위는 별도 확정한다. 대장에 최종 분류·사람 확인이 없는 항목은 색이나 자동 분석만으로 계산·구조 변경 허용 범위에 넣지 않는다.
53. 2026-10-07 사용자 확인: SUPPLY `BCPipeline`의 T-118 `Annual Pipeline Capacity Baseline`은 **C — 구조변경**으로 분류하며 표의 마지막 데이터 행 아래 행 추가를 허용한다. 현재 시트에는 셀 수식이 없고 다른 내부 시트·외부 파일을 읽는 셀 수식 참조도 없다. 이 분류·행 추가 허용 결정 외의 세부 write 정책은 규칙 22에 따라 확정한다.
54. 2026-10-07 사용자 확인: SUPPLY `BCHurricanes`의 T-112 `Hurricane Impact on Upstream Assets`와 T-113 `Hurricane Impact on Liquefaction Plants`는 각각 **B — 값만 교체**이며 행 추가는 허용하지 않는다. 값 변경 후 T-112의 `G8:G11`과 T-113의 `G16:G19`, 총 8개 수식은 고정 source snapshot에 저장된 외부 `[1]LUT!$AH$14:$AI$17`의 캐시를 이용해 재계산한다. 이는 규칙 33의 표별 예외다. G열은 사용자 쓰기 금지 계산 셀이며 수식은 유지하고 계산 결과를 갱신한다. 외부 LUT 캐시·externalLink 관계는 원본 그대로 보존하고 외부 파일 요청·개방·링크 갱신은 하지 않는다. 필요한 캐시가 없으면 계산 성공으로 처리하지 않는다. T-114·T-115의 표별 분류는 규칙 55를 따른다. 이 결정은 계산 엔진 검증 완료를 뜻하지 않는다.
55. 2026-10-07 사용자 확인: SUPPLY `BCHurricanes`의 T-114 `Hurricane Affected Provinces`는 **B — 값만 교체**이며 기존 행·열을 유지한다. T-115 `Hurricane Affected LNG Plants`는 **C — 구조변경**이다. 기존 대장의 아래 행 추가 안내와 규칙 13에 따라 해당 논리 표의 마지막 데이터 행 아래 행 추가를 허용한다. 두 표의 현재 수식은 각각 0개이므로 표 내 재계산·신규 행 수식 복제 대상이 없다. T-115의 열 추가·삭제·신규 행 필드/서식 및 상세 write 계약은 별도로 확정한다. `BCHurricanes`의 시트 단위 C 표기는 T-112·T-113·T-114의 B와 T-115의 C를 요약한 것이며 다른 세 표의 구조변경을 허용하는 근거로 사용하지 않는다. 규칙 54의 8개 수식 재계산 범위는 유지한다.

56. 2026-10-07 사용자 확인: SUPPLY `BCProduction`의 T-111 `Production Year Count Reference` (`N7:T24`)는 **A — 원본 복제**이며 화면에 표시하지 않는다. 해당 표의 기존 값·수식·cached result와 관련 피벗 설정/캐시·외부 원본 관계를 그대로 보존한다. T-110 `Annual Gas Production Baseline`의 행 추가에도 T-111의 O열 재집계·Q/R/S 연쇄 갱신·수식 재계산·피벗 새로고침·원본 연결 변경은 하지 않는다. 이 보존 범위는 T-111이며 같은 시트 전체나 T-110의 정책을 A로 변경하지 않는다.

57. 2026-10-07 사용자 확인: 기준 묶음 `2026-10-07-delivery-261006-v8-v6`의 원본 4파일 `SUMMARY` C열에서 표 전체가 `미표시`인 항목은 **A — 원본 복제**이며 화면에 표시하지 않는다. 해당 업무 표의 값·수식·cached result와 관련 원본 구성을 변경·재계산·확장하지 않는다. 표별 대응은 통합 판별대장에 기록한다. 같은 시트의 표시 표와 표 내부 일부 미표시 열에는 이 전체 표 분류를 확대하지 않는다. 워크북 밖 서버 LUT·외부 결과 연결 설명은 대장의 A 분류 기록으로 구분하고, 복제할 XLSM 시트·셀 범위를 새로 만들지 않는다. 개발 전용 `SUMMARY`·`플랫폼 입력 안내`의 X 제거 정책은 규칙 39를 유지한다.

규칙 57의 시트 전체 적용 확인(2026-10-08 사용자 재확인): TRADE `backup_MidstreamContractInput`(T-187)은 **시트 전체가 화면 미표시·A 원본 복제**다. 값·수식·저장된 계산 결과·서식·관련 원본 설정과 원본 시트의 Excel 표시/숨김 상태를 보존한다. 값 편집·구조 변경·재계산·새로고침 대상에서 제외하며, `CK4`·`CQ4`의 UNIQUE 수식과 배열 결과도 이 보존 범위에 포함한다.

같은 사용자 재확인에 따라 TRADE의 숨김 시트 `Industry research`(T-188)도 **시트 전체가 화면 미표시·A 원본 복제**다. 원본의 숨김 상태·값·수식·저장된 계산 결과·서식·관련 설정을 보존하고 값 편집·구조 변경·재계산·새로고침 대상에서 제외한다. `A3`의 UNIQUE도 원본 보존 대상이며 POI의 직접 재계산을 요구하지 않는다.

규칙 57의 UNIQUE 적용 최종 확인(2026-10-08 양방향 조사 후 사용자 결정): `MidstreamContractInput!CK4/CQ4`, `backup_MidstreamContractInput!CK4/CQ4`, `Industry research!A3`의 **5셀은 화면 미표시·모델 미사용·A 원본 복제로 분류 확정**한다. 원본 복제의 직접 근거는 고정 TRADE v8 원본 `SUMMARY!J17/J18/J67/J68`의 원본 시트/값 유지 구현 기준이다. 실제 문구와 5셀 대응은 [함수 reference의 보존 정책 출처](../../references/mi-model-input-functions-2026-10-07.md#unique-사용-위치의-현재-보존-정책--2026-10-08)를 따른다. 상류의 입력 연결 여부와 관계없이 기존 수식·배열 결과·저장된 계산값·관련 설정을 원본 그대로 보존하고, 사용자 값 편집·행/열 추가에도 해당 보존 영역을 갱신·재계산·확장하지 않는다. T-135/T-136의 기존 A 결정도 유지한다. 모델 미사용은 SUMMARY 문구에서 추론한 사실이 아닌 이 5셀에 대한 별도 사용자 업무 확인이며 코드/모델 실행 검증을 뜻하지 않는다. 함수 reference의 양방향 조사에는 LNG의 A 표에서 T-139 AG/AI까지 이어지는 정적 수식 연결이 남아 있다. A 경계에서는 갱신 전파를 멈추며, AG/AI의 자기 입력에 따른 별도 계산·모델 사용 여부는 이 5셀의 분류 결정에 포함하지 않는다. 현재 UNIQUE 직접 계산 구현은 필요하지 않다.

58. 2026-10-07 사용자 확인: DEMAND v8 `BCInput`의 T-023~T-030 8표와 시트 단위 분류는 **C — 구조변경**이다. 각 표의 입력값 편집과 마지막 데이터 행 아래 행 추가를 허용하며 열은 고정한다. 기존 입력 범위는 T-023 `C4:N14493`, T-024 `Q4:V2368`, T-025 `Z4:AD100803`·`AH4:AI100803`, T-026 `AK4:AN1125`, T-027 `AQ4:AU13548`, T-028 `AW4:AY252`, T-029 `BB4:BD290`, T-030 `BG4:BK28`이며 신규 입력 행은 해당 표의 범위만 확장한다. T-025 `Power Generation Data`의 `AE:AG`와 T-024 `RE Base Case GDP and Population Data`의 `X`는 화면에 표시하지 않고 모델에서도 사용하지 않는다. 행 추가 시 해당 열에 신규 수식을 생성·복제·자동 채우기 하지 않는다. 기존 `AE:AF`도 화면 미표시·모델 미사용이므로 입력 편집·행 추가 후 재계산하지 않고 원본 수식과 저장된 계산 결과를 보존한다. 저장값 Helper인 T-023 `O4:O14493`, T-026 `AO5:AO1125`, T-028 `AZ4:AZ252`, T-029 `BE4:BE290`은 입력 편집 후 갱신하지 않고 원본 값을 보존하며, 신규 행에도 Helper 값을 생성하지 않는다. `AO4`는 저장값 Helper가 아닌 수식 셀이므로 이 저장값 처리 결정에 포함하지 않는다. 기존 `AG`·`X`·`AO4` 수식의 재계산 여부를 이번 기존 `AE:AF` 재계산 제외 결정으로 확대하지 않는다.

    - **2026-10-14 사용자 최종 결정 — F-D-01 추가 계산 제외:** 같은 정규화 DEMAND source에서 `BCInput!N4`를 source +0.125로 SET하는 F-D-01은 **N4 입력만 반영하고 추가 계산하지 않는다**. Helper·AE:AF뿐 아니라 기존 X/AG/AO4를 포함한 지정 입력 외 모든 원본 수식·저장값·타입·서식을 보존한다. 계산 및 Excel 계산 정답 비교는 해당 없음(PRESERVE)이며 이 지정 사례의 추가 갱신 여부 대기는 해소한다. 다른 BCInput 입력 변경/행 추가의 일반 정책을 이 사례 결정으로 확대하지 않는다.

규칙 58의 화면 표시 보완: T-026 `Power Generation Utilisation Rate`는 `AK:AN`만 화면에 표시하고 `AO`열은 화면에서 제외한다(2026-10-07 사용자 확인). AO4 수식과 AO5:AO1125 저장값의 처리 정책은 규칙 58에서 각각 정한 범위를 유지한다.

같은 사용자 확인에 따라 T-028 `Base Case LNG Bunkering Demand`(시트 제목 `LNG Bunkering Input`)는 `AW:AY`만 화면에 표시하고 `AZ`열을 제외한다. T-029 `Base Case Hydrogen Demand`(시트 제목 `Hydrogen Demand Input`)는 `BB:BD`만 화면에 표시하고 `BE`열을 제외한다. 두 저장값 Helper의 생성·갱신 제외 및 기존 값 보존 정책은 규칙 58을 유지한다.

59. 2026-10-08 사용자 확인: TRADE `MidstreamContractInput` T-139의 승인된 사용자 입력 `Q5:U2918`에서 연결된 값이 변경되면 영향을 받는 `AG/AI`를 함께 갱신한다. 다른 행 합계를 통한 영향도 필요한 계산 범위 검토에 포함한다. 기존 T-134~T-138 및 UNIQUE 5셀의 A 원본 보존은 유지한다. 이 결정은 AG/AI의 직접 편집 허용이나 실행 검증 완료를 뜻하지 않는다. 필요한 선행 계산 범위·빈 기준값/없는 키·기존/신규 오류 및 신규 행 처리, Excel 비교값·A 불변·모델 소비 검증은 Q5/H1의 후속으로 남긴다.
    - 2026-10-13 사용자 후속 결정 — **F-T-01 계산 제외 예외:** 같은 정규화 TRADE source의 `MidstreamContractInput!Q5` Year를 2026→2027로 SET하는 F-T-01에서는 `AG/AI`를 계산하지 않고 원본 수식·저장된 계산 결과·타입·서식을 보존한다. 이 사례에는 앞선 AG/AI 갱신 규칙 대신 이 보존 결정을 적용한다. 기존 저장 `#N/A` 보존은 새 계산의 정답/허용 오류로 인정하는 것이 아니다. F-T-02(U5 계약량 변경), 다른 Year 변경, AF/AH·표 밖 CN/CT·신규 행 처리까지 이 결정을 확대하지 않는다.
    - 2026-10-13 사용자 후속 결정 — **F-T-02 기존 #N/A 보존 예외:** `MidstreamContractInput!U5` 계약량을 source + 0.125로 SET하는 F-T-02에서는 변경 전 정규화 TRADE source에서 이미 `#N/A`인 셀을 계산·치환·수정하지 않고 원본 수식·저장값·오류 타입·서식을 보존한다. 해당 셀에는 AG/AI 갱신 규칙보다 이 보존 결정을 우선 적용한다. 0/빈 값이나 새 계산 결과로 바꾸지 않는다. 비오류 셀의 계산 범위·정답, 새로 발생한 `#N/A`/다른 오류 허용 및 다른 사례의 정책은 이 결정에 포함하지 않는다.
    - **2026-10-14 사용자 최종 결정 — F-T-01/02 전체 추가 계산 제외:** 같은 정규화 TRADE source에서 각각 독립 실행하는 F-T-01(Q5 Year 2026→2027)과 F-T-02(U5 계약량 source +0.125)는 **각 지정 입력 한 셀만 반영하고 나머지는 계산하지 않는다**. 원본 AF/AH·AG/AI와 비오류 수식·다른 행·표 밖 CN/CT·A/UNIQUE·backup·Industry research 등 지정 입력 외 모든 수식·저장값·타입·서식을 보존한다. 앞선 한정 예외를 이 두 사례의 전체 보존으로 확장하며 비오류·다른 행·표 밖 계산 대기를 해소한다. 계산 및 Excel 계산 정답 비교는 해당 없음(PRESERVE), 원본의 빈 cache/기존 오류를 새 정답·승인 오류로 사용하거나0/공백으로 치환하지 않는다. 다른 Year/계약량 변경·신규 행의 일반 계산/키/오류 정책까지 확대하지 않는다.
    - 2026-10-13 사용자 후속 결정 — **F-T-03 계약량 빈 값 금지·기본값0:** TRADE `MidstreamContractInput!U5` 계약량은 비울 수 없으며 기본값은 **숫자0**이다. 실제 빈 셀을 만드는 기존 F-T-03 CLEAR는 허용 입력 사례가 아니므로 거부 기대값으로 정리한다. 기본값0은 유효한 숫자 입력이며 실제 빈 셀/빈 문자열과 구분한다. CLEAR를 명시적 SET0으로 치환해 실행 완료로 기록하지 않는다. 기본값을 적용하는 구체 시점/처리 방식은 후속 write 계약에서 정의하며 이번 확인으로 빈 요청 자동0 보정·계약 삭제/비활성화·계산 범위/오류 처리를 정하지 않는다. 다른 계약 행의 빈 값/기본값·사례 정책까지 확대하지 않는다.
    - 2026-10-13 사용자 후속 결정 — **F-T-04 생성 위치·필수 입력:** TRADE `MidstreamContractInput` T-139의 새 계약은 **704행 빈 슬롯부터 채우며 Q:U 모두 필수 입력**이다. 필드는 Q `Year`, R `From Country`, S `From Liquefaction Terminal`, T `To Country`, U `Gross Gas Import [Mt LNG]`다. 기존 빈 슬롯에 새 논리 계약을 생성하는 동작이며 worksheet 행 삽입이나 2919행부터의 표 확장으로 대체하지 않는다. 신규 실제 값·후보/선행 등록·키/중복·계약량 0/CLEAR 및 V:AJ·CN/CT의 계산/저장값 처리는 이 결정만으로 확정하지 않는다. 기존 A·UNIQUE 보존은 유지한다.
    - 2026-10-13 사용자 후속 결정 — **F-T-04 전체 계산 제외·원본 보존:** 지정 검증값(Q704=2030, R704:T704=source R134:T134의 값, U704=1.81275)을 넣는 F-T-04는 **Q704:U704 입력만 반영하고 계산하지 않는다**. V704:AJ704의 원본 수식·저장값·타입·서식과 다른 행·A/UNIQUE·표 밖 CN/CT 등 승인 5입력 밖 전체 원본을 보존한다. 기존 빈 슬롯의 서식·행/열·validation·참조·metadata도 유지한다. 원본의 빈 저장값이나 기존 오류를 새 계산 결과·0/공백으로 치환하지 않는다. 이 사례에는 앞선 AG/AI 갱신 규칙 대신 보존 결정을 적용하며 계산 및 Excel 계산 정답 비교는 해당 없음(PRESERVE)이다. 일반 신규 계약·다른 입력 변경의 계산/키/오류 정책까지 확대하지 않는다.

60. 2026-10-08 사용자 결정: **동일 사용자·source Case당 진행 중 Draft는 최대 1건**이며 이미 있으면 그 Draft를 재개한다. 완료·폐기된 과거 Draft는 진행 중 1건 제한에 포함하지 않는다. 동시 생성에도 제한을 지켜야 하며 구체 API·저장/동시성 보장 수단은 Q8b의 후속 설계 대상이다.

61. 2026-10-08 사용자 결정: **최종저장 실패가 확정되면 해당 저장을 취소**한다. 실패 요청을 FINALIZED로 전환하거나 성공한 최종 Case·4파일 set으로 확정하지 않고, 이전에 성공한 임시저장 Draft와 변경분은 유지한다. 파일 업로드·Case 생성·Draft 연결 사이의 부분 효과를 취소·정리/복구하는 구체 방식은 Q8c에서 설계·검증한다. 이미 완료한 최종저장의 응답 유실은 실패 확정과 구분하며 완료 재시도는 규칙 19에 따라 같은 Case를 반환한다.

62. 2026-10-13 사용자 확인: **F-S-06 CLEAR 사례의 SUPPLY `PipelineAdjustment!H67`은 빈 입력을 허용**한다. 기존 셀의 내용만 제거하여 실제 빈 셀로 만들고 셀·서식·행은 유지한다. 0/빈 문자열 입력이나 행 삭제로 대체하지 않는다. 후속 사용자 확인: **현재 행의 H67이 실제 빈 셀이면 I67 CHECK의 `Error - enter value`가 정상 결과**다. I67 원본 수식을 유지하며 계산 결과는 해당 문자열이어야 한다. 이 CHECK 경고 문자열을 Excel 수식 오류(`#N/A` 등)와 혼동하거나 공백으로 대체하지 않는다. **F-S-06 계산 범위는 I67 CHECK와 이를 집계하는 I3이며 나머지 수식·저장값은 원본 보존**이다(후속 사용자 확인). I3가 읽는 I8:I200 중 승인 I67은 새 계산 결과를 사용하고 나머지는 원본 저장값을 기준으로 유지한다. 이는 F-S-06의 계산/보존 경계이며 다른 입력 변경·셀/표의 빈 값 허용으로 확대하지 않는다. F-O-03의 C2 빈 값 금지는 §6.1을 유지한다.

### 6.1 규칙 50의 OUTPUT 최신 적용 기준 — 2026-10-08

최신 기준은 고정 OUTPUT v8에 대응한 [판별대장 README](../../references/model-input/README.md)의 사람 확인이다. 표시 6표의 입력·화면·행/기간 정책은 승인됐고, [SUMMARY 원본 유지 대조](../../references/mi-model-input-summary-preservation-review-2026-10-08.md#output--18셀--6묶음)의 미표시 12표는 규칙 57의 A 원본 보존을 따른다. 자동 파랑 셀 수·영향도는 v3 관측이며 현재 쓰기/계산 허용 근거로 쓰지 않는다.

| 시트 / 표 ID | 최신 입력 선언 | 편집·구조 경계 | 조회·고정/미표시 경계 |
| --- | --- | --- | --- |
| `USCostofSupply` / T-081 | `B8:L816` | B — 값만 교체, 행·열/기간 고정 | 제목·헤더 `B6:L7`은 사용자 쓰기 금지. 원본 수식과 입력 L열의 셀 대응은 A-07 검증 잔여 |
| `BCPrices` / T-083 | `B8:E21` | C — 마지막 데이터 행 아래 연도 행 추가, 열 고정 | 제목·헤더 `B6:E7`은 조회만 제공 |
| `PremiumCalc` TTF / T-084 | `C39:C47` | B — 값만 교체, 행·열/기간 고정 | B열 연도는 조회, D:H는 화면 미표시 |
| `PremiumCalc` East Asia Spot / T-085 | `C57:C61` | B — 값만 교체, 행·열/기간 고정 | B열 연도는 조회, D:H는 화면 미표시. 62~65행은 입력/화면에서 제외하고 원본 유지 |
| `AnnualPriceCalc` Spread / T-090 | `F49:G49` | B — 기존 하한/상한 값만 교체, 행·열/기간 고정 | `B49:E49` 설명 영역은 조회만 제공 |
| `PriceSeasonality` / T-098 | `C2:O4` | B — 값만 교체, 기존 3개 행·월/열 고정 | 지역명·월 헤더는 사용자 쓰기 금지 |

- 입력 색은 노랑이며 파랑은 고정 제목/헤더, 청록은 자동/조회 신호다. 명시된 표/필드 허용을 색보다 우선한다. 입력 선언의 직사각형만으로 원본 수식·고정 헤더를 사용자 값으로 덮어쓰지 않으며 실제 cell allowlist는 원본 수식과 대조한다.
- **2026-10-13 C2 사용자 확인:** OUTPUT `PriceSeasonality!C2`는 빈 값을 허용하지 않으며 숫자 값을 반드시 입력한다. 숫자 `0`은 허용한다. C2의 CLEAR는 거부 대상이며 빈 입력을 자동으로 0으로 바꾸지 않는다. 기존 F-O-03의 빈 셀 생성·보존 검증은 C2의 업무상 빈 값 허용 근거가 아니다. 다른 `C2:O4` 셀의 필수/빈 값 정책은 이 확인만으로 확정하지 않는다.
- 미표시 A는 **T-082, T-086~T-089, T-091~T-097의 12표**다. T-082의 `P8:P816`을 포함해 기존 값·수식·배열 결과/저장된 계산값·관련 구성을 원본 보존하며 입력 편집·행 추가를 이유로 갱신·재계산·확장하지 않는다. 같은 시트의 표시 표까지 A로 바꾸지 않는다.
- **2026-10-13 F-O-01 사용자 확인:** `USCostofSupply!B8`을 0.34→0.35로 SET하는 F-O-01에는 추가로 갱신할 셀이 없다. B8만 변경하고 P8:P816 Helper·D7:K7 등 나머지 셀의 원본 값·수식·저장값·타입·서식을 유지한다. 이 사례의 수식 계산·Excel 계산 정답은 해당 없음이다. 다른 OUTPUT 입력 변경·시트·구조 변경의 계산 정책까지 확정한 것은 아니다.
- **2026-10-14 F-O-04 사용자 지정 — 21빈 슬롯 활성화·계산 제외:** 같은 정규화 OUTPUT source의 `BCPrices`에서 새 연도는 기존21행 빈 슬롯에 생성한다. B21=2036, C21:E21에는 각각 source C16:E16의 **숫자값만 literal로 복사**한다. 이 지정 사례는 추가 계산 없이4입력 외 모든 원본 값·수식·저장값·서식/validation·행/열·참조/metadata를 보존한다.21의 기존 셀 서식/General을 유지하며 C16:E16의0.00 서식·수식까지 복제하거나 값을 반올림하지 않는다. 입력 선언 B8:E21·worksheet dimension A1:S21은 유지하고22행 신규 확장/worksheet 행 삽입으로 대체하지 않는다. 계산 및 Excel 계산 정답 비교는 해당 없음(PRESERVE)이며 새 결과 파일의 입력/보존·저장 후 재읽기·읽기 전용 native를 검증한다. 모든 지정 가격값이 있는 이 사례에서 일반 신규 가격의 필수/빈 값·기본값이나 다른 OUTPUT 입력/구조 변경의 계산 정책을 추론하지 않는다.
- **계산 잔여(지정 F-O-01/04 외):** 과거 `D7:K7 + P8:P816 = 817셀`을 현재 계산 allowlist로 사용하지 않는다. F-O-01/04의 추가 갱신 없음은 위 사용자 확인을 따르고, 다른 입력 변경의 최신 L열 대응·비A 수식 범위/Excel 비교값은 A-07/Q5에서 대조 후 확정한다. `PremiumCalc` D:H의 일부 화면 미표시만으로 그 열 전체를 A 보존이나 재계산 대상으로 승격하지 않는다.
- 입력·행 추가 승인과 계산 엔진 GO는 별개다. T-083 신규 행의 필드/서식·validation·범위/참조 처리와 파일 밖 모델 연결, 계산 결과·원본 보존 검증은 후속 write/검증 범위다. Scenario OUTPUT은 규칙 48의 별도 기준 원본 보존 계약을 유지한다.

## 7. Extension considerations

- 테이블별 field/column definition은 화면 표시와 Backend write validation에 필수다. 안정적인 `columnKey`,
  표시 label, typed value, 필수·편집·수식 여부, 선택 후보 source를 template version별로 고정한다.
- 기간이 열에 있는 표(§14.3 유형 A)는 연도·월 헤더 행 위치를 모르면 열을 해석하거나 추가할 수 없다.
  기간축 header 생성 규칙과 허용 범위를 field/structure policy의 첫 검토 대상으로 둔다.
- 대상 식별에 업무 이름을 쓸 수 없는 표본이 확인됐다(§11). 행 identity는 surrogate ID를 전제로 설계한다.
- 표 사이의 행 추가 선행 관계(§6 규칙 12)를 표현할 수단이 필요하다. 표 정의에 master 표 참조를 두는 방법과,
  행 추가를 표 단위가 아니라 도메인 동작(예: 설비 등록)으로 계약하는 방법이 있다. 후자는 카탈로그를 넓히지
  않지만 동작별 계약이 늘어난다. 실제 행 추가 요구가 확정될 때 함께 정한다.
- 기존 행에서는 읽기 전용이고 신규 행에서는 필수인 열이 있다(§14.5). 수정 가능 범위와 별개 개념이므로
  같은 직사각형 범위로 표현하지 않는다.
- Midstream 인계에서 실제 값 끝·입력 선언 끝·사전 수식 끝이 다름을 확인했다. 빈 슬롯 활성화와 논리 표 확장, 기존/신규 셀의 값·수식/빈칸 처리를 구분한다. 구조 추가를 worksheet 전체 행 삽입으로 바로 구현하면 나란한 A 영역까지 이동할 수 있으므로 승인 표 변경과 A 불변을 함께 만족하는 방식·검증은 Q4/H2/H3에서 정한다.
- 보존 기준값을 읽는 계산 후보는 그 기준값의 유효성·키 일치·첫 일치 의미와 나머지 선행 셀 경계도 확인해야 한다. Midstream의 빈 BA/미일치 BI와 AF/AH 의존은 이를 보여주는 근거이며 새 mapping·0/공백 대체나 A 재계산을 자동 추가하지 않는다(H1/H4).
- 수정 범위가 직사각형으로 표현되지 않는 예외가 확인되면 여러 범위 row로 분할한다. 임의 JSON 좌표는
  검색·검증과 정합성 보장이 어려워 기본 저장 형식으로 사용하지 않는다. 이는 DDL §5의 카탈로그 수정 범위 결정이며 Draft 변경분의 JSON/row 저장 미결정을 대신하지 않는다.
- 독립 파일 버전 조합이 실제 운영 요구로 확인되면 template set과 file version 사이에 membership
  관계를 추가할 수 있다. 현재는 한 실행이 4개 파일을 한 snapshot으로 사용하므로 set 버전을 우선한다.
- 같은 workbook의 다른 시트를 참조하는 계산 허용 formula cell은 원본 수식 자체가 dependency의 source of truth다.
  source input을 변경할 때 target formula cell을 값으로 덮어쓰지 않고 수식을 보존한 뒤 호환되는 계산 엔진으로 재계산한다.
  다른 workbook을 참조하는 formula cell의 계산/보존은 §6 규칙 33–34·54의 표별 예외를 따른다.
- table-level dependency는 영향받는 화면 table 표시, 재조회 범위 결정, template 검증을 위한 파생 metadata로는
  사용할 수 있지만 새 target 값을 계산하는 규칙으로 사용할 수 없다. 합계·lookup·연쇄 수식은 source/target
  table 관계만으로 결과를 계산할 수 없기 때문이다.
- dependency 조회 요구가 확정되면 immutable template version에서 formula를 분석해
  `sourceInputTable → targetInputTable` edge를 생성하는 방식을 우선한다. 사람이 중복 등록한 관계를 수식과
  별도 source of truth로 관리하지 않는다. 정확한 셀/range 영향 조회가 필요할 때만 edge 범위를 세분화한다.
- formula가 아니라 같은 값을 여러 위치에 하드코딩해야 하는 예외만 명시적 value-propagation rule 후보로 둔다.
  이 경우에도 template version, source/target range, 변환 규칙과 중복 적용 방지를 함께 정의해야 한다.
- 사용자가 추가할 header/row key 후보는 `FIXED`, `REFERENCE_TABLE`, `MASTER_DATA`, `TIME_AXIS`,
  `FREE_INPUT` 같은 source type으로 구분한다. 큰 동적 후보 집합은 hierarchy 응답에 전부 포함하지 않고
  후속 API contract에서 별도 조회 경계를 정한다.
- Draft 자동저장은 현재 범위에서 제외한다. 나중에 추가하더라도 같은 Draft revision/change 저장 책임을
  재사용하고 workbook 재계산 주기만 별도로 정한다.

## 8. Data access recommendation by responsibility

아래는 구현 후보이며 클래스 분리·저장 기술을 확정한 목록이 아니다. Q1~Q9 결과와 가장 가까운 기존 코드 패턴으로 필요한 경계를 결정한다.

- 카탈로그 CRUD와 단건 hierarchy 조회: JPA.
- 한 양식 전체를 `file → sheet → table → scope`로 읽는 화면 projection: QueryDSL 또는 전용 projection query.
- 운영 기준 정규화본의 70개 시트/테이블 seed 대량 등록: DBA 실행 SQL 또는 관리용 batch. 운영 API로 일반 사용자에게 열지 않는다.
- 실제 workbook 내용 파싱/생성: 기존 파일 서비스와 ZIP/SAX 처리 경계. 카탈로그 repository에 넣지 않는다.
- Draft master/change 저장과 revision 충돌 처리: transaction service + JPA. Draft 변경분의 물리 저장 단위는 ERD review에서 확정한다.
- 최종 XLSM 생성: Backend 소유 값 적용·계산·보존 검증 책임. 별도 클래스 구성은 미확정이며 파라미터 제안의 `ModelParamExcelMergeService`나 모델 실행 container를 호출하지 않는다.
- 생성 결과 전달: XLSM 4개의 role별 bytes를 `MultipartFile` 4개로 변환하는 adapter 패턴을 사용한다. 기존 `MergedModelInputFiles`는 3파일 전용이므로 새 흐름이 직접 의존하지 않으며, 4파일 결과 타입에 변환을 두거나 중립적인 공통 adapter로 분리한다.
- 최종 파일 업로드·Case 생성: 변환된 `MultipartFile` 4개를 기존 4파일 Fundamental Model application service에 전달한다.

## 9. API/DTO impact

API 경로·요청·응답 계약은 feature spec/OpenAPI가 소유한다. 후속 계약에는 최소한 아래를 정의해야 한다.

- template version 식별자와 파일 역할
- sheet name/order/status
- table id/name과 baseline 좌표
- field/column key, label, type, required/editable/formula 여부와 validation
- row/column add policy, header option source와 선택 후보
- 영향받는 table 식별자와 계산 상태
- `modifiableScopes[]`의 1-based start/end row/column
- Draft 식별자, source snapshot, revision, `DRAFT/FINALIZED/DISCARDED` lifecycle
- 임시저장과 최종저장의 validation·계산 실패·멱등 재시도 의미
- 최종저장 성공 시 생성된 `caseId`와 기존 Run 접수 경계
- soft-deleted legacy 3파일 case를 active-resource API에서 `404 RSC_NOT_FOUND`로 숨기는 계약
- 구조 불일치 파일의 오류 코드와 사용자 메시지

## 10. SQL / DBA implications

- 기존 Draft는 catalog master/detail/relation 6개 table을 제안했다. field/options/dependency/화면 갱신 규칙 및 Draft transaction
  저장이 필수가 되어 추가 schema 영향이 예상되며, 물리 table 수와 JSON/row 저장 경계는 ERD review에서 확정한다.
- 초기 backfill은 legacy 3파일 case를 mapping하지 않는다. 신규 4파일 case만 생성 시점의 template과 연결한다.
- DDL을 application보다 먼저 배포할 수 있지만, 새 application이 mapping을 필수로 읽기 전에
  active template seed와 신규 4파일 Case 생성·mapping 경로를 완료해야 한다.
- 실제 DDL은 pre-check/apply/backfill/rollback/verify/DBA action 순서로 제공한다.

## 11. Risks / open questions

- 이번 release의 구조 변경 포함과 마지막 행·열 추가 중심 원칙은 확정됐다. 표별 추가 경계, 신규 셀·연결 범위 처리 및 승인된 중간 삽입·삭제 예외의 참조 갱신 계약·실파일 검증은 남아 있다.
- 새 행·열의 서식·수식 복제 규칙이 없다. 추가 가능 flag만으로 파일 생성 동작을 확정할 수 없다.
- `INPUT-02` 실파일 spike에서 Apache POI 5.4.0은 1 GiB heap으로 `supply.xlsm` 전체 로딩에 실패했고,
  headless LibreOffice는 cached value를 갱신하지 못하면서 VBA·external link와 package 구조를 훼손했다.
  두 경로는 현재 요구사항에 `NO-GO`다. 2026-10-13 사용자 결정으로 전체 로딩 대신 POI 부분 계산을 채택했다(§1A.4 Q7).
- 대형 workbook 계산을 web request JVM에 직접 넣으면 OOM과 동시 요청 자원 고갈 위험이 있다. 후속 spike는
  격리 worker/비동기 처리 필요 여부까지 판정해야 하며, 이 결과에 따라 Draft 계산 상태와 API 응답 형태가 달라진다.
- 최신 input 표본에는 다른 sheet를 참조하는 복합 수식과 외부 workbook 참조가 존재한다. table dependency
  catalog만으로 계산 결과를 갱신하는 구현은 안전하지 않다.
- 최신 v8/v6 기준본 고정과 정규화 보존 검사는 완료됐다(baseline reference). 최신 표별 정책 대응·계산 검증과 운영 template 등록은 남아 있다. 9/28의 개발 전용 6개 시트 수를 최신 묶음의 수치로 사용하지 않는다.
- 독립실행의 Case 생성·MI Base 적격성·Job input 4역할 전달은 현재 코드에 구현돼 있다. Scenario는 현재 3파일 코드이며 목표 계약은 두 mode 모두 4파일이다. 파라미터 제안 기반 Scenario의 OUTPUT 원본 보존·제안 제외 규칙은 `mi-model-parameter-proposal-design.md` §1A를 따른다. Scenario의 기준 파일 준비·4파일 접수·Case 연결·Job 전달 구현과 입력폼 catalog mapping/projection은 남은 작업이다.
- 기존 3파일 `INDEPENDENT | MI_BASE | SCENARIO` case는 4파일 기능 cutover 전에 soft delete한다(`SCENARIO`의 연결 row 범위는 규칙 36). 대상 판별·건수·`RUNNING`
  0건·current `MI_BASE` 포함 여부를 DBA pre-check로 확인하고, case/file/run/result hard delete는 하지 않는다.
- `tmm_mi_model_param_m`도 같은 파일/시트 위치를 저장한다. 양식 버전 FK를 공유하지 않으면 이중 관리가 남는다.
- `BaseEntity` 감사 컬럼과 DB 명명 문서의 시스템 컬럼명이 다르다. physical DDL 적용 전 표준 예외 확인이 필요하다.
- 양식 판별 해시의 입력값 정규화 방식이 미정이다. hash column은 nullable로 두되 자동 판별을 아직 계약하지 않는다.
- 표본에서 설비명이 유일키가 아니었다. `BCRegasification`의 `Malta LNG`가 국가만 다른 2행으로 존재하고
  helper `설비명&LifeCycle`도 중복이라, 업무 이름 기반 행 identity는 성립하지 않는다.
- `RegasAdjustment`의 식별 열은 이름이 아니라 `BCRegasification`의 행 번호로 연결돼 있다. 원본 정렬이나
  행 삽입만으로 조정값이 다른 설비에 붙는다. 카탈로그 좌표로는 이 결합을 막을 수 없다.
- Trade regas disruption에는 `(Year, Month, Country)` 후보 key 중복 108건이 있고, LNG 제한 32행은
  Year/Month가 비어 있으며, `PipelineAdjustment` 실제 60행 중 5행은 key가 불완전하다. 이 범위는
  surrogate ID 또는 더 강한 원천 식별자 없이 영속 row identity를 만들 수 없다.
- §14의 과거 Regas 표본에서 rollup 수식이 값으로 굳은 관측은 최신 v6의 결함으로 사용하지 않는다. 최신 연결·연쇄 갱신은 규칙 51과 계산 호환성 reference를 기준으로 확인한다.
- 개발본 `SUMMARY`가 기술한 표 중 `SeasonalityLogic`은 workbook이 아니라 서버 LUT에 있다. 이 설명은
  운영 catalog seed가 아니라 검증 대상인 설계 evidence다. 카탈로그를 시트 하위로만
  두면 이 표는 양식 버전 관리 밖에 남는다.
- 유형 A의 행 추가는 두 시트·세 표에 걸친 142셀 생성과 파생 집계 재계산을 요구한다(§14.5).
  현재 스키마는 표별 행 추가 가능 여부만 표현하므로, 이 상태로 행 추가 API를 열면 미러 수식만 있고
  원본이 없는 행이 만들어질 수 있다.
- 미러 수식은 원본의 빈 칸을 `0`으로 표시한다. 원본 필수값 검증 없이는 화면에 잘못된 코드값이 노출된다.
- `BCRegasification` 열 추가는 `가능`으로 명세돼 있으나 조회 수식의 열 인덱스와 연도 범위가 고정이라
  단순 append가 아니다. 열 추가 계약은 대상 수식 갱신 규칙까지 포함해야 한다.

## 12. Exit criteria / handoff

- 공통 초안 단계: Complete — §1A.2~1A.4와 §3.1로 흐름·책임·정보 관계와 후속 결정 입력을 설명할 수 있다. 전체 표 조사·설계 확정 목표는 In Progress이며 Delivery Plan 선행조건·BLOCKED 티켓 상태는 변경하지 않았다.
- 트랙 A 수신 범위: A-01/A-02 Midstream 정적 조사와 4파일 우선 목록을 반영했다. H1~H4 정책·계산/구조 검증, A-03~A-10 상세 조사는 미완료다. 인계 통합을 엔진 GO나 구현 착수 승인으로 사용하지 않는다.
- 모델 안정성: `blocked by open questions` — 기존 hierarchy와 source snapshot 경계는 유지되지만 field/options/dependency,
  Draft 저장 단위, 승인된 table/row identity, workbook 계산 엔진과 실행 격리 방식의 물리·행동 계약이 추가로 필요하다.
- API contract drafting: 조회-only sheet/table hierarchy와 `임시저장/최종저장` action 책임은 초안 작성 가능하다.
  구체 DTO·오류·동기/비동기 응답은 후속 계산 엔진 spike와 Draft 저장 단위를 확정한 뒤 고정한다.
- ERD review required areas:
  - 기존 6개 catalog table과 신규 field/options/dependency 저장 구조의 PK/FK/UK/index
  - Draft master/change의 JSON 대 row 저장, revision unique, user/source case별 active Draft 제한
  - `FINALIZED` Draft와 생성 Case의 idempotent 1:1 관계
  - row/column data type 범위
  - active template 단건 보장 generated column
  - legacy 3파일 case soft-delete 대상 판별, `RUNNING` 차단, rollback과 DBA rollout 순서
  - 감사 컬럼 명명 충돌

## 13. Dictionary updates

- added: `시트 / SHT / 단일단어`
- reason: file-sheet-table 계층에서 Excel sheet를 다른 개념으로 대체할 수 없다.
- file: `docs/conventions/db-term-dictionary-sheet2.csv`
- append note: 기존 행을 수정하지 않고 마지막에 385번 row로 추가했다.
- 2026-09-28: `출력 / OUTP / OUTPUT`, `결과 / RSLT / RESULT`, `파일 / FILE / FILE`,
  `역할 / ROLE / ROLE`이 기존 사전에 있어 신규 term row를 추가하지 않았다.

## 14. 실측 baseline 좌표 (SUPPLY 표본)

측정 파일은 개발용 양식 표본 `Supply_v14_플랫폼입력_최종.xlsm`(시트 24개)이며, 이 중 `SUMMARY`는
실제 운영 인풋에 없는 개발 전용 시트다. 따라서 운영 SUPPLY 구성은 23개 business sheet이고, 실측 근거와 절차는
`docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`의 2026-09-09 절이 소유한다.
아래 좌표는 카탈로그 seed의 출발점이자 표 경계 규칙(§6 규칙 5·6)의 검증 표본이다.
**과거 표본 근거:** 최신 SUPPLY v6의 seed·수식 개수·편집/계산 정책을 확정하는 값이 아니다. 최신 사용자 결정은 §6, 재대조할 항목은 §1A.4 Q1~Q6을 따른다. 아래 rollup·수식 개수와 구조 후보도 같은 역사적 범위로 읽는다.
개발본 24개 시트 중 `SUMMARY`와 대표 유형 2종, 그 참조 시트만 실측했으므로 SUPPLY 전체 seed는 아니다.
`SUMMARY`에서 얻은 설명은 같은 개발본의 운영 대상 business sheet와 대조하기 전에는 seed로 확정하지 않는다.

좌표는 §6 규칙 4에 따라 Excel 1-based 숫자이며, 열 문자는 확인용 병기다.

### 14.1 표 경계 (`tmm_mdlinpttbld`)

| 시트 | 입력테이블명 | 행 | 열 | 헤더 행 | 데이터 시작 | 행추가 | 열추가 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RegasAdjustment | Monthly Regas Capacity Adjustment | 9–382 | 2–115 (`B`–`DK`) | 9–10 | 11 | Y | Y |
| RegasAdjustment | Regas Adjustment Annual Rollup | 10–382 | 117–126 (`DM`–`DV`) | 10–10 | 11 | N | N |
| RegasAdjustment | Regas Adjustment Country Monthly Rollup | 385–444 | 7–115 (`G`–`DK`) | 385–386 | 387 | N | N |

`RegasAdjustment`는 1–3행 범례·설명 블록과 8행 표 제목을 제외하면 위 3개 표가 시트를 남김없이 덮는다.
어느 표에도 속하지 않는 셀은 0개다. 이 중 사용자 편집 대상은 첫 번째 표 하나뿐이다.
| PipelineAdjustment | Monthly Pipeline Capacity Adjustment | 7–200 | 1–9 (`A`–`I`) | 7–7 | 8 | Y | N |
| BCRegasification | Annual Regas Capacity Baseline | 7–379 | 1–18 (`A`–`R`) | 7–7 | 8 | Y | Y |
| BCPipeline | Annual Pipeline Capacity Baseline | 7–70 | 2–6 (`B`–`F`) | 7–7 | 8 | Y | N |

행·열 추가 정책은 개발본 `SUMMARY` 시트의 G/H 열(`가능` / `고정`)을 옮긴 후보값이다.
같은 개발본의 운영 대상 business sheet 구조와 대조·승인하기 전에는 활성 catalog에 등록하지 않는다.

### 14.2 수정 가능 범위 (`tmm_mdlinptmodscopd`)

| 입력테이블 | 순서 | 행 | 열 | 근거 |
| --- | --- | --- | --- | --- |
| Monthly Regas Capacity Adjustment | 1 | 11–382 | 8–115 (`H`–`DK`) | 월별 Capacity 값. 식별 열 `B`–`G`(2–7)는 기존 행에서 조회 수식이라 제외. 신규 행에서는 필수이며 §14.5 참조 |
| Monthly Pipeline Capacity Adjustment | 1 | 13–200 | 2–4 (`B`–`D`) | 연도·월·Pipeline 키. 8–12행은 `#REF!` 잔해라 제외 |
| Monthly Pipeline Capacity Adjustment | 2 | 8–200 | 8–8 (`H`) | 사용자 조정 Capacity |
| Annual Regas Capacity Baseline | 1 | 8–379 | 2–17 (`B`–`Q`) | 식별 5열 + 연도 2025–2035. `A` Helper와 `R` Max는 수식이라 제외 |
| Annual Pipeline Capacity Baseline | 1 | 8–70 | 2–6 (`B`–`F`) | 표 전체 |

파생 rollup 테이블 2개의 값 영역은 수정 가능 범위를 갖지 않는다(§6 규칙에 따라 읽기 전용).
예외는 아래 색 코드 항목의 마지막 줄에 적었다.

#### 색 코드 근거

원본 워크북은 `RegasAdjustment` `Q2`/`Q3`에 범례를 두고 셀 성격을 fill로 구분한다.
플랫폼입력 버전이 추가한 밝은 노랑과 함께 4종이 쓰이며, 수정 가능 범위는 이 신호와 일치한다.

| fill | 범례 | 적용 영역 | 셀 수 |
| --- | --- | --- | ---: |
| `FFFFE600` | SUMMARY "직접 수정할 값" | 표1 `H11:AE382` (2023-01~2024-12) | 8,928 |
| `theme6 / tint 0.8` | `Q2` User input | 표1 `AF11:DK382` (2025-01~2031-12) | 31,248 |
| `theme6 / tint 0.6` | `Q3` Formula cells | 표2 `DN11:DV382` 3,348 + 표3 `H387:DK444` 6,264 | 9,612 |
| `none` / `theme2` | 범례 없음 | 표1 식별 열 `B11:G382`, 표2 `DM` 국가 열 | 2,604 |

- 표2·표3의 **값 영역은 전량이 `Formula cells`** 로 표시돼 있다. 조회 전용이라는 판단이 색으로도 확인된다.
- 표1의 2025년 이후 월 셀은 수식이지만 `User input` 색이다. 색만으로 수정 가능 범위를 도출하면 이 84개 열이
  누락되고, 반대로 수식 여부만으로 도출하면 이 열이 잘못 제외된다. 두 신호를 함께 봐야 한다.
- **예외 1건**: 표3의 국가 라벨 `G387:G444` 58셀은 `User input` 색이고 실제로도 리터럴이다.
  집계 대상 국가 목록을 사람이 유지하는 구조이며, 본표에 없는 국가 8개가 이미 들어 있다.
  플랫폼에서는 이 목록을 본표의 국가 열에서 파생해야 하므로 수정 가능 범위로 옮기지 않는다.

### 14.3 대표 표 유형 2종

| | 유형 A — 행=대상, 열=기간 | 유형 B — 행=레코드 |
| --- | --- | --- |
| 표본 | RegasAdjustment | PipelineAdjustment |
| 기간 위치 | 열. 9행 연도 + 10행 월 2단 헤더, 108열(2023-01~2031-12) | 행. `B` 연도 / `C` 월 |
| 대상 식별 | 좌측 `B`–`G` 조회 열, 372행 | `D` Pipeline name |
| 값 단위 | 월 물량 `Mt LNG` | 연율 `Mt/y` |
| 기본값 출처 | 2025년 이후는 `BCRegasification` 연간 용량 ÷ 12 수식 | 없음. `G` Base capacity가 하드코딩 |
| 확장 방향 | 행(설비) + 열(기간) | 행만 |

두 유형 모두 `(대상키, 연, 월) → 값`으로 환원되므로 저장 모델을 나누지 않는다.
유형 차이는 화면 표현과 새 파일 생성 시 좌표 전개 규칙에만 반영한다.

### 14.4 이 스키마로 아직 표현되지 않는 것

1. **기간 축(period axis)** — 유형 A는 "열 = (연도 9행, 월 10행)"이라는 정보 없이는 열 해석도 열 추가도 할 수 없고,
   유형 B는 같은 기간이 열 2–3에 있다. 테이블 정의에 축 표현이 없으면 두 유형을 같은 규칙으로 다룰 수 없다.
2. **셀 원천(BASE / USER)** — 유형 A는 "수식이면 기본값, 리터럴이면 사용자 값"이다. 되돌리기와
   새 버전 생성 시 수식 보존 판단에 필요하지만 카탈로그에도 case 데이터에도 저장 위치가 없다.
3. **파생 테이블 표시** — 14.1의 rollup 2개는 읽기 전용을 넘어 "값이 stale이므로 서버 재계산" 이라는 성질을 갖는다.
   수정 가능 범위가 없다는 사실만으로는 이 차이를 표현하지 못한다.
4. **case별 변경분 저장 위치** — 카탈로그는 사용자 수정값을 소유하지 않는다(§3). 화면 편집 → 새 버전 생성에는
   `(case, 테이블, 대상키, 연, 월) → 값` 형태의 변경분이 필요하며, 소유 위치가 아직 정해지지 않았다.
5. **신규 행 필수 입력과 표 사이의 행 추가 선행 관계** — 유형 A의 식별 열은 기존 행에서 읽기 전용이지만
   신규 행에서는 필수이고, 그 값은 master 표에 먼저 등록돼야 한다(§14.5). 수정 가능 범위 하나로는
   이 두 성격을 구분할 수 없고, 표별 행 추가 플래그로는 선행 관계를 표현할 수 없다.

이 항목들은 후속 설계의 결정 대상이다. 구조 변경은 이번 release에 포함하며, 미결정 물리 표현은 DDL 후속 검토에서 확정한다.

### 14.5 행 추가는 표 단위 연산이 아니다 (유형 A)

`RegasAdjustment`의 식별 열 `B`–`F`는 사용자가 값을 넣는 열이 아니라
`=BCRegasification!B8` 형태로 **원본 행을 미러링하는 수식**이다. 오프셋 −3이 372행 전부 일치하고
양쪽 데이터 행 수도 372행으로 같아, 두 표는 1:1로 강제 대응된다.
따라서 `RegasAdjustment`에만 설비 행을 추가할 수 없다. 원본에 행이 없으면 미러 수식은 값을 만들지 못한다.
SUMMARY I15가 요구하는 순서(① BCRegasification 등록 → ② 조정 시트 연결 행 → ③ 월 기본값·조정값)와 같은 제약이다.

설비 1개를 추가할 때 실제로 채워야 하는 셀은 아래와 같다. 노랑으로 표시된 셀만으로는 행이 완성되지 않는다.

| 대상 | 셀 | 내용 |
| --- | ---: | --- |
| BCRegasification 신규 행 | 1 | `A` Helper (`=E&F`) |
| BCRegasification 신규 행 | 5 | `B`–`F` 대륙·지역·국가·설비·Life Cycle Category |
| BCRegasification 신규 행 | 11 | `G`–`Q` 연간 Capacity 2025–2035 |
| BCRegasification 신규 행 | 1 | `R` Maximum capacity |
| RegasAdjustment 신규 행 | 5 | `B`–`F` 미러 수식 (오프셋 −3 유지) |
| RegasAdjustment 신규 행 | 1 | `G` Nameplate 조회 수식 |
| RegasAdjustment 신규 행 | 24 | `H`–`AE` 2023-01~2024-12 리터럴 |
| RegasAdjustment 신규 행 | 84 | `AF`–`DK` 2025-01~2031-12 미러 수식 |
| RegasAdjustment 우측 rollup | 10 | `DM` 국가 + `DN`–`DV` 연간 합계 |
| **합계** | **142** | |

여기에 더해 하단 국가 rollup(§14.1)의 `SUMIFS` 범위가 `H$11:H$382`로 고정돼 있어 신규 행이 집계에서 빠진다.
범위를 넓히려면 58행 × 108열 = 6,264셀을 갱신해야 하는데 그중 수식이 남은 셀은 66개뿐이라 수식 수정으로는
해결되지 않는다. 신규 설비의 국가가 rollup의 58개 국가 목록에 없으면 국가 행(109셀)도 추가해야 한다.
현재 본표는 50개국이므로 8개국은 이미 자리가 있고, 그 밖의 국가는 행 추가 대상이다.

부수 확인:

- 원본의 빈 칸은 미러 수식에서 `0`이 된다. `BCRegasification` 365행 `GNL Escobar`는 Life Cycle Category가
  비어 있고, `RegasAdjustment` 368행 `F`는 문자열이 아니라 `0`을 표시한다. 원본 필수값 검증이 없으면 그대로 노출된다.
- `A` Helper와 `R` Maximum capacity는 372행 중 각각 365행·366행이 값으로 굳어 있다. 신규 행을 수식으로 채울지
  값으로 채울지 규칙이 필요하다.
- 조회 수식의 열 인덱스가 고정이다. `G`의 `VLOOKUP(...,$E:$R,14,FALSE)`는 `R`이 14번째라는 전제이고,
  월 수식의 `MATCH(연도, $E$7:$Q$7, 0)`은 연도 범위가 `Q`에서 끝난다는 전제다.
  개발본 `SUMMARY`가 `BCRegasification` 열 추가를 `가능`으로 기술했지만, `R` 앞에 2036년 열을 넣으면 두 전제가 모두 깨진다.
  이 표기는 같은 개발본의 운영 대상 business sheet와 대조하기 전까지 정책 근거가 아니라 위험 발견용 evidence로만 사용한다.

결론:

- 유형 A의 행 추가는 **master 표(BCRegasification) 등록을 선행 조건으로 갖는 시트 간 트랜잭션**이다.
  표별 `row_add_psbl_yn` 플래그만으로 허용 여부를 판단할 수 없다.
- 중간 insert는 오프셋 −3, rollup 고정 범위, 고정 열 인덱스를 동시에 깨뜨린다. **append-only가 전제**다.
- 화면에서도 두 표에 각각 추가하게 두면 안 되고, "설비 등록" 하나의 동작으로 묶어야 한다.

## 15. Document control / source-of-truth notes

- 이 문서는 카탈로그·Draft의 업무 소유권, version/편집 lifecycle, 관계, 수정 범위와 최종 파일·Case·Run 사이 책임 경계의 canonical source다. Run 내부 lifecycle은 기존 실행 정본을 참조한다.
- §14의 실측 좌표는 seed 작성과 규칙 검증을 위한 참조값이다. 실측 절차와 원본 근거는
  `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`가 소유한다.
- 물리 table/column/index/key/nullability/rollout SQL은
  `docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md`를 참조한다.
- endpoint/request/response/error는
  `docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md`에서 정의한다.
- 2026-10-01: 전체 화면 구조와 단일 표 값의 반환 범위 및 FE 필드 이름/셀 표현 재사용 계약은 위 API spec §3이 소유한다. 규칙 25는 표시 후보와 write 허용 범위를 구분하도록 정리했으며 표 단위 조회를 일부 field만 반환한다는 결정으로 해석하지 않는다. API의 `sections/inputId/tableData`를 새 도메인 엔터티나 DB 컬럼 요구로 해석하지 않는다.
- 2026-10-01 UI 보완: API의 table은 일반 표·부속 control·control 전용 영역을 포함하는 논리 입력 영역이다. 공유 control의 연결/소유권과 원본 셀 binding 표현은 위 API spec §3.1B가 소유한다. 이 조회 표현만으로 새 엔터티/DB 컬럼이나 모드 write 정책을 확정하지 않는다.
- 2026-10-01 업로드 보완: kind별 등록 양식으로 사용자 파일을 검증·파싱하는 화면용 POST /input-uploads 계약은 위 API spec §3.3이 소유한다. 신규 포맷 자동 등록이나 4파일 Case 생성과 구분하며 업로드 원본 보존/수명 모델은 별도 확정한다.
- 행·열 추가 방식과 감사 컬럼 표준 충돌은 구현 전 해결해야 하지만, hierarchy 조회용 schema 초안 검토는 진행할 수 있다.
- 2026-10-08: 트랙 B 공통 흐름·책임·정책 후보·A 결과 반영과 단계 완료 판정은 §1A·§3.1을 따른다. API·DDL·Delivery Plan·조사 대장 변경은 이 요약만으로 확정되지 않는다.
- 2026-10-08 트랙 A 인계: Midstream A-01/A-02와 AG/AI 상류 정적 확인을 §1A.3~1A.4의 Q1~Q6·H1~H4에 연결했다. 기존 §6 승인은 유지하며 다음 표 조사·계산/보존/구조 검증과 사람 결정은 미완료다.
- 2026-10-13: 사용자 결정으로 계산 방식을 POI 부분 계산으로 정했다(§1A.4 Q7). 격리·동기/비동기 경계와 `INPUT-02A` 판정은 미완료다.
- 2026-10-13: §1A.5 「다른 정본의 변경 후보」 중 Delivery Plan 대상 2건(PriceModelInput 문구, GDP/PIPELINE/UNIQUE 검증 후보)이 반영됐음을 표시했다. 업무 규칙은 바꾸지 않았다.
- 2026-10-13: 사용자 결정으로 규칙 36의 legacy 3파일 정리에 `SCENARIO` case를 추가했다. 연결 run history·execution-preparation·active 가격전망해석을 함께 soft delete하며 규칙 38의 `RUNNING` pre-check와 §11 위험 문구를 맞췄다. 같은 날 잠시 기록한 "SCENARIO 제외" 문구는 이 결정으로 대체했다.
- implementation readiness: `blocked by open questions` — `INPUT-02A` 격리·동기/비동기 경계와
  `INPUT-03` row identity·Draft 물리 모델이 확정되기 전에는 write API 구현을 시작하지 않는다.
