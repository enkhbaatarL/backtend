# MI Fundamental Model Draft 공통 편집 API 초안

## Document Control

- status: Draft
- canonical for: API contract proposal — 이 slice의 endpoint/method, 공개 DTO·validation·오류 위치와 문서용 JSON 예시. 기존 QRY-01/02 계약은 변경하지 않는다.
- owner: Shared (BE / FE)
- source documents:
  - [기존 제공 명세](mi-fundamental-model-input-provision-feature-spec.md)
  - [확정 범위를 포함한 QRY 상세](mi-fundamental-model-input-query-api-proposal.md) §1.1·§3~5·§9
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md` — 혼합안 승인·BCPrices/Sector 입력 승인 근거
- depends on:
  - [domain](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md) §3.2~3.4·§5.1·규칙15/19/60~64 — 소유권·lifecycle·성공/실패 불변식
  - [DDL draft](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md) §6.3·§13 — 공통 물리 정의/DBA 적용 준비
  - [Delivery Plan](../design-docs/gas/mi-model-input-editing-delivery-plan.md) §6A·§7·§9
  - [OpenAPI 규칙](../conventions/openapi-documentation-conventions.md), [보안 정책](../../SECURITY.md)
- supersedes: 없음. QRY-01/02와 기존 조회/업로드 범위를 대체하지 않는다.
- last synced: 2026-10-19 (F-O-01 실환경 기본 성공 흐름·C DRAFT 등록 상태 참조 동기화)
- open questions: §10. 2026-10-16 공통 BE 채택과 이번 공통 저장·최종저장/복구 계약(§9) 마무리를 반영한다. 표별 정책·DBA 적용 준비/제품 검증은 별도 후속이며 PriceModel/Regas 추천은 미승인이다. 문서 전체는 Draft다.

## 1. 목표·이번 slice

FE가 공통 그리드, 신규 행 입력, 저장 대기/성공/실패, 충돌 후 재조회 흐름을 문서 mock으로 준비할 수 있게 한다. 최초 작성은 문서 계약 범위였으며 현재 첫 구현 범위와 한계는 §1.2를 따른다. DB 적용은 사용자 담당이다. **2026-10-16 공통 BE 추천 묶음을 사용자 채택했다.** 아래 채택 범위와 실제 물리 DDL/표별 전체 정책·구현/검증 완료는 구분한다. 예시 성공은 제품/계산/파일 검증 결과가 아니다.

- 포함: 저장 Case에서 Draft 생성/재개, 성공 Draft 구조/표 조회, 순서 있는 값 변경·행 추가/삭제의 임시저장 접수/결과 조회, 최종저장 접수/결과 조회와 응답 유실·부분 실패/재시작 복구 계약(§9).
- 표별 일반 입력/구조·계산 정책은 후속이다. 미결은 해당 표/동작을 열기 전에 처리하며 공통 저장·최종저장 계약을 마치는 선행조건으로 두지 않는다. PriceModel/Regas 추천은 미승인으로 유지한다.
- 별도 책임: INPUT-03은 논리/저장·행 대응·유일성, INPUT-04는 공개 API, STRUCT는 표별 실제 구조 동작·참조/수식·서식·연결 검증, INPUT-07 이후는 구현, INPUT-10은 API 전체 저장 응답 검증이다. 구조 변경은 이번 release 범위에 유지한다.

확인한 local pattern은 `FundamentalModelCaseController`의 prefix/권한, `FundamentalModelCaseServiceImpl.findReadableActiveCase`와 별도 owned-case 수정 경계, record DTO의 `@Schema`, `FundamentalModelCaseCreateRes.statusEndpoint`, 공통 `BaseResponseFactory`/`ResultCode`다. 기존 Case/Run 상태를 Draft 작업 상태로 재사용하지 않는다. 최초 조사 때는 endpoint/DTO가 없었으며 현재 구현 상태는 §1.2를 따른다.

### 1.1 사용자 채택 범위 — 2026-10-16

- 공통 BE 묶음: §3~6의 endpoint/공개 ID·typed 값/오류 위치, 안정 rowId·신규 임시키, 순서 있는 명령·요청 전체 성공/실패, 성공 revision 충돌, requestId 재전송, Draft당 진행 요청1건과 비동기 접수/완료·조회 흐름을 채택했다. 관계형/JSON 책임·상태/성공 경계는 domain §3.4/§5.1이 소유한다.
- 기술 시작값: §2의 2초 폴링과 §4의 body/명령 수 등 초기 제한을 채택 기준으로 사용한다. 실제 제품 적용/정확한 bytes·정밀도/성능 검증은 후속이며 측정 뒤 조정이 필요하면 계약/FE와 동기화한다. 공개 DTO와 DB JSON을 동일 형식으로 강제하지 않는다.
- Sector: 국가/하위부문 필수 자유입력·연월 범위·신규/기존 수정 중복 금지 및 H/J 파생 처리·I 음수 금지는 [domain 규칙45](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)의 사용자 확정 사항이며 공개 검증/오류는 §4.1에서 연결한다.
- 두 업무 묶음: Sector/Monthly 수요 비우기 금지·행 삭제로 해제와 Monthly 입력/중복·P blank/R=Q는 domain 규칙45, BCPrices 기존 연도/가격 수정·입력값 보존은 domain §6.1에서 채택했다. 공개 검증/오류는 §4.1~4.3을 따른다.
- 이번 사용자 요청에 따라 공통 저장/보관·복원은 DDL §6.3, 최종저장 상세는 §9로 마무리한다. 실제 runtime ID/표별 미정·DBA 적용/제품 검증은 §10의 후속이다. 이 채택으로 예시 catalog·mock을 운영 READY로 만들거나 문서 전체를 Approved/INPUT-03·04 전체 완료로 승격하지 않는다.

### 1.2 첫 구현 범위 — 2026-10-16

- **현재 구현·실환경 상태:** QRY-01/EDT-01~07과 지정 F-O-01 처리·공통 worker/최종저장이 구현돼 있다. 사용자 테스트 서버에서 Case105의 임시저장·revision1 재조회→Case106 최종저장→새 Case 구조 재조회 기본 성공 흐름을 확인했다. 날짜/ID·DB 저장·시간대 설정 조치와 미검증 범위는 [공용 작업 노트의 실행 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#case105-106-smoke-20261019)가 소유한다. QRY-02, 일반 표/구조·계산 처리, FE 화면·실환경 경합/장애 복구·Kubernetes 검증은 별도 후속이다. 아래 날짜별 구현 경계는 단계 이력이며 일반 지원 확대나 문서 전체 Approved를 뜻하지 않는다.
- C의 92개 조회 표는 새 DRAFT 양식 등록만 완료했다. [등록 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#c-structure-registration-20261019)를 참조하며 미발행·Case 미연결 상태를 유지한다. F-O-01 시험 양식과 기존 Case 연결을 대체하지 않는다.

- 2026-10-16 물리 table prefix 변경은 [DDL §6.6](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md)이 소유한다. endpoint/요청·응답은 유지한다. 새 이름 적용과 API 검증의 실행 사실은 [기존 작업 노트의 최신 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다.

2026-10-16 첫 구현은 공통 요청 처리와 EDT-01~03의 DB 연동, sourceVersion 진입에 필요한 QRY-01이었다. 이후 EDT-04/05 공통 기반·F-O-01 실제 처리·EDT-06/07 통합은 아래 날짜별 항목을 따른다. 조회는 고정 원본 revision 0과 지원하는 성공 CHANGE 이력을 복원한다. 진행 작업 중에는 직전 성공 revision을 조회한다. 저장 이력의 schema/참조·행 대응이 맞지 않으면 HTTP500/data:null로 차단하고 원본으로 대체하지 않는다. 전체 계약의 완료/배포를 뜻하지 않는다.

- POST는 신규/재개 모두200, 회사·사용자·원본별 active Draft 1건이다. 원본 파일 읽기는 장기 DB transaction 밖에서 하고, 저장 직전 Case 잠금과 원본 기준 재검사를 거쳐 Draft·4파일·revision0·원본 행/위치이력을 한 master transaction에 저장한다. 재개 시 새 행ID를 만들지 않는다.
- 조회 정의는 DDL §6.5의 발행 `qry_json`이다. 읽기 좌표/행 식별 근거와 확인된 metadata만 사용한다. 표별 정책이 미완비여도 읽기 정의는 준비할 수 있으며 해당 capability를 INCOMPLETE로 구분한다. 정의 누락을 빈 규칙으로 추정하지 않는다. 저장 API가 없던 첫 구현에서는 발행 capability READY도 공개 UNSUPPORTED로 제공했으며, 현재 F-O-01의 READY 조건은 아래 첫 실제 처리 연결 절을 따른다. 정책 채택 상태를 바꾸는 것이 아니다.
- 원본 비교 키의 tenantKey는 인증 resolver로 확정한 회사 번호의 선행0 없는 10진 문자열이다. 다른 입력의 해시 순서/표현은 QRY §4.3~4.6을 유지한다. 각 호출에서 source와 개인 소유권을 다시 확인한다.
- 요청 body는 wrapper가 먼저 전부 읽기 전에 이 경로의 HTTP body/MDC 캐싱·로그를 제외한다. 전용 JSON 경계에서 크기·중복 key·알 수 없는 필드·형식/후행 JSON을 검사하며 수치 직렬화는 정확한10진수를 유지한다. 내부 SQL/저장소/거부값 원문은 오류에 넣지 않는다.
- **구현·검증 현황:** 첫 구현과 Case103 기본 DB 연동 시험의 결과·환경·독립 검토·남은 확인은 [기존 작업 노트의 최신 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 기본 시험과 실제 MySQL 경합/실패·접근 경계·큰 표의 추가 검증, 전체 기능/운영 배포는 구분한다. 타회사 원본 허용 지적은 domain §5.4의 사용자 범위 확인에 따라 적용하지 않는다. FE 조회 시점은 [QRY 상세 §5.1의 FE 구간 로딩 방식](mi-fundamental-model-input-query-api-proposal.md)을 따르며 FE 구현/화면 검증은 후속이다. 표별 정책은 후속, PriceModel/Regas 추가 추천은 미승인 유지.

#### 2026-10-18 EDT-04/05 공통 기반 구현 경계

이 절의 처리기 미연결·신규 요청 차단은 공통 기반 단계의 이력이다. 현재 지원 범위는 아래 F-O-01/EDT-06·07 연결 절과 위 최신 실행 결과를 따른다.

EDT-04/05는 §3~6의 요청 파싱·동일 requestId 의미 비교·DB 작업/진행 슬롯·상태 조회 경계를 제공한다. 현재 표별 변경·계산 처리기는 연결하지 않는다. 새 요청의 표/동작 정책 미완비는 POLICY_INCOMPLETE, 준비된 정책이라도 실제 처리기가 없으면 POLICY_UNSUPPORTED로 접수 전에 거부하며 operation을 만들지 않는다. 비어 있는 처리를 SUCCEEDED로 기록하거나 요청만 저장해 무기한 QUEUED로 돌려주지 않는다. 따라서 현재 공개 API에서 신규202/실제 변경 성공을 검증했다는 뜻은 아니다. 공통 영속 접수·중복·claim·복구 경계는 격리 테스트에서 검증한다.

기존 기록 조회와 동일 요청 재전송은 현재 source 기준이 달라져도 소유권/source 접근을 확인한 뒤 저장 기록을 반환한다. 기한 경과 복구는 쓰기 DB의 Draft→요청 row 잠금 아래 접수/결과 조회 때 수행한다. worker 연결 전에는 자동 dispatcher/주기적 전체 스캔을 가동하지 않는다. QUEUED 탐색/claim과 RUNNING 종료 확인 후 실패 확정의 내부 저장 경계를 준비하며, 실제 격리 worker/종료 증거 수집·재시작 조정과 성공 변경 확정은 후속이다. 종료가 확인되지 않은 RUNNING을 서버 시작만으로 실패/재실행하지 않는다.

성공 확정/복원 공통 경로의 내부 저장 형식은 [DDL §6.3.4.1](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md)이 소유한다. 이 경로는 검증된 처리 결과를 원자적으로 저장하는 내부 경계이며 실제 처리기·정책이 미준비인 신규 공개 요청의 차단을 해제하지 않는다. EDT-02/03은 재계산 없이 저장된 성공 값·구조·행 대응을 읽으며 revision0 응답 형식/페이지·정확 수치 구분을 유지한다.

2026-10-19 감사 이름·시간 정밀도와 수동 증분 적용은 [DDL §6.8](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md)을 따른다. 공개 endpoint/응답·숫자/페이지 계약과 작업120초 상한은 유지한다.

#### 2026-10-19 첫 실제 처리 연결 — F-O-01 한정

위 처리기 미연결 설명은 공통 기반 구현 당시 경계다. 이번 연결은 [domain §5.5](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md)의 지정 F-O-01만 지원한다. 별도 발행의 `OUTPUT/USCostofSupply/us_cost_of_supply`, B8 한 슬롯의 `breakevenPrice`에 한 개 SET으로 숫자0.35를 보내는 요청을 받는다. null은 기존 공통 요청 형식 검증의400, 문자열/다른 숫자값은422 VALIDATION_FAILED, 다른 준비된 동작은409 POLICY_UNSUPPORTED, 미완비 정책은 기존409 POLICY_INCOMPLETE다. `setConstraints`의 minimum/maximum은 모두0.35다. worker 사용 설정과 이 좁은 발행 정의가 함께 준비된 경우에만 SET을 공개 READY로 제공한다. 기존 Case103 발행은 바꾸지 않는다.

신규 접수202→영속 QUEUED/RUNNING→검증·동일 master 성공 확정 뒤 SUCCEEDED/resultRevision=baseRevision+1로 연결한다. 추가 계산은 승인 사례상 해당 없음이며 B8 외 전체 OOXML member/수식·cache·서식/구조 보존과 저장 재읽기를 확인한다. 동일 요청 재송신·기한/실패·기존 조회/숫자 계약은 §5~6을 유지한다. 실제 DB/API 기본 성공 흐름은 위 실행 결과로 확인했으며 다른 표/값/구조·계산 지원을 뜻하지 않는다. EDT-06/07은 아래 통합 경계를 따른다.

#### 2026-10-19 EDT-06/07 공통 통합 경계

EDT-06/07은 §9 정본 계약으로 연결한다. 신규 준비파일 DDL의 수동 적용/검증 뒤 웹과 같은 단일 worker에 `mi.model-input.finalization.enabled=true`를 함께 지정한다(기본 false). 비활성화 중에는 신규 FINALIZE 접수를409 POLICY_UNSUPPORTED로 차단하고 기존 기록 조회/동일 요청 재전송은 유지한다. CHANGE와 FINALIZE의 작업 이력·requestId·진행 슬롯·120초/marker 복구 경계는 하나다. revision0의 원본4개와 지원하는 전체 성공 변경을 대상으로 하며, 현재 nonzero 파일 재생성은 지정 F-O-01 보존 이력만 지원한다. 지원되지 않는 성공 이력을 무시해 최종저장하지 않는다. 사용자 시험에서 준비파일 DDL 적용·실제 최종저장과 새 Case 구조 재조회는 확인했다. 상세 증거와 중복·장애 복구 등 남은 실환경 검증은 위 실행 결과를 참조한다.

## 2. 호출 흐름·공통 접근 경계

Case 목록에서 개인 Draft ID를 받은 경우 EDT-02로 바로 재조회해 편집을 이어갈 수 있다. 목록 필드의 범위와 null 의미는 [MI Base/Case 목록 계약 §3](mi-fundamental-model-mi-base-designation-feature-spec.md#3-api-slice-a--기존-scenario-case-목록에-current-mi-base-포함)를 따른다. ID가 없으면 아래 생성/재개 흐름을 사용한다.

1. 기존 QRY-01로 구조를 읽고 최초 표 미선택을 유지한다. 원본 표를 볼 때 QRY-02를 그대로 사용한다.
2. 편집 시작 시 QRY-01의 `caseId/sourceVersion`으로 EDT-01을 호출한다. 기존 진행 Draft가 있으면 재개한다.
3. EDT-02의 성공 revision/구조를 기준으로 EDT-03에서 선택 표를 읽는다. FE 구간 로딩은 [QRY 상세 §5.1](mi-fundamental-model-input-query-api-proposal.md)을 따르며, 각 추가 조회도 같은 draftId/revision/tableId를 사용한다. 원본 QRY 응답을 Draft 저장값으로 표시하지 않는다.
4. FE의 아직 저장하지 않은 변경만 EDT-04에 보낸다. 응답 유실 시 **같은 requestId·동일 body**를 재전송한다.
5. EDT-05를 조회한다. `QUEUED/RUNNING`은 대기, `SUCCEEDED/FAILED`면 폴링 종료다. 채택한 초기 폴링 간격은 2초이며 화면 이탈 시 멈춘다. 네트워크 오류나 FE 대기시간 경과는 서버 실패 확정이 아니다.
6. 성공하면 EDT-02/03을 새 revision으로 재조회하고 이전 페이지/좌표/화면 파생값을 폐기한다. 실패면 성공 revision을 유지하고 오류를 표시한다. 새 요청으로 재시도할 때 현재 Draft를 확인한다. 충돌이면 자동 재전송/덮어쓰기 없이 최신 Draft와 로컬 변경을 대조한다.
7. 최종저장은 미저장 로컬 변경이 없고 진행 작업도 없을 때 EDT-06으로 현재 성공 revision을 제출한다. EDT-07을 조회해 SUCCEEDED일 때만 새 Case로 이동한다. RUN은 별도다. 화면을 다시 열면 EDT-02의 activeOperationEndpoint로 진행 작업을 재개하고 FINALIZED면 finalizedCaseId를 사용한다.

모든 operation은 기존 USER/SKGAS_COMPANY_USER_ONLY, ADMIN/THIRD_PARTY_ADMIN_ONLY 권한 패턴과 인증된 tenant/user를 사용한다. source는 domain §5.4의 한 회사 기능 범위 안에서 접근 가능한 active `INDEPENDENT/MI_BASE`이며 업로더 본인일 필요는 없다. Draft는 **생성 사용자 개인 소유**이므로 source Case 수정 소유권과 구별한다. 다른 소유자/tenant의 Draft·작업은 404, 관리자도 이 API로 다른 사람 Draft에 접근하지 않는 안이다. 각 요청에서 source 접근 가능성도 확인한다. source 상태가 COMPLETED여야 한다는 새 조건이나 source Case 수정 권한은 추가하지 않는다.

worker는 접수 당시 서버가 확인한 actor/tenant/고정 기준을 전달받고 종료 때 context를 정리한다. 사용자/tenant/fileNo/bucket/object key/수식/저장 경로는 공개 요청으로 받지 않는다. payload·셀값·거부값·원문 예외는 HTTP/worker 로그와 오류에 남기지 않는다. 평문 작업사본/기존 암복호화는 domain 규칙63, 자원·비동기·전체120초 상한은 규칙64를 그대로 따른다. QRY JSON DRM 비적용의 기존 결정을 재심의하지 않는다.

입력 양식 선택은 [조회 API §4.6](mi-fundamental-model-input-query-api-proposal.md)의 Case 연결 기준을 따른다. application model-code 설정으로 제한하지 않으며, 기존 Draft의 고정 양식·원본과 현재 Case 연결이 다르면 기존 충돌 오류를 유지한다.

## 3. 채택한 공통 Endpoint

공통 root는 `/api/v1/mi/fundamental-model`이다. body/response는 `application/json`, 인증은 기존 방식이다. 아래 GET은 body가 없다.

| ID / summary | Method / path (root 뒤) | 요청 | 성공·호출 목적 |
| --- | --- | --- | --- |
| EDT-01 / 입력 편집 Draft 생성 또는 재개 | `POST /cases/{caseId}/input-drafts` | path 양의 int64 caseId, body `sourceVersion` 필수 | 200 `DraftSummary` + `created`. 새 Draft와 기존 재개 모두 200. Case/Run/계산을 만들지 않음 |
| EDT-02 / 저장된 Draft 구조 조회 | `GET /input-drafts/{draftId}` | path UUID draftId | 200 `DraftSummary` + `sections`, `tableData:null`. 현재 성공 revision의 구조이며 모든 표 값은 포함하지 않음 |
| EDT-03 / Draft 선택 표 조회 | `GET /input-drafts/{draftId}/table-data` | query `revision`, `tableId` 필수, `startRow` 선택 | 200 `DraftTableData`. 해당 성공 revision의 선택 표/연결 control과 편집 metadata |
| EDT-04 / Draft 변경 임시저장 접수 | `POST /input-drafts/{draftId}/change-requests` | `ChangeRequest` 필수 | 신규 접수 202 `OperationView`. 동일 요청 재조회는 진행 중 202/종료 후 200. 접수 성공은 저장 성공이 아님 |
| EDT-05 / 임시저장 처리 결과 조회 | `GET /input-drafts/{draftId}/change-requests/{operationId}` | 두 path UUID 필수 | 200 `OperationView`. 실패 작업도 조회 자체가 성공하면 HTTP200/SUC-000이며 data.status가 FAILED |
| EDT-06 / Draft 최종저장 접수 | `POST /input-drafts/{draftId}/finalizations` | §9 FinalizationRequest | 신규/진행 중 재전송202, 종료 후 재전송200 FinalizationView. 접수는 Case 생성 완료가 아님 |
| EDT-07 / 최종저장 결과 조회 | `GET /input-drafts/{draftId}/finalizations/{operationId}` | 두 path UUID 필수 | 200 FinalizationView. 실패 조회도 HTTP200/SUC-000, 성공 Case는 SUCCEEDED에만 |

공개 ID의 UUID는 소문자 하이픈 포함 36자, revision은 0 이상 정수(최대 9,007,199,254,740,991)로 채택했다. `sourceVersion`은 기존 `^sv1_[0-9a-f]{64}$`/68자다. `tableId`는 기존 구조의 ID 그대로이며 `/`·공백은 query에서 URL 인코딩한다. 조사 T-083 등은 runtime tableId가 아니다.

### 3.1 생성/재개·고정 기준

EDT-01 body는 `{ "sourceVersion": "QRY-01에서 받은 값" }`만 받는다. 현재 source의 네 파일/발행 묶음과 대조해 같은 기준을 고정한다. 동시 생성도 동일 tenant/사용자/source Case당 진행 Draft 한 건만 반환한다. 기존 Draft를 재개할 때 revision을 초기화하지 않고 진행 작업도 반환한다. 기존 진행 Draft의 고정 기준과 요청/현재 source가 다르면 409 `DRAFT_SOURCE_CONFLICT`이며 새 Draft 생성·기존 Draft 재기준화로 우회하지 않는다. 생성/재개는 자동 임시저장이나 화면 미저장값 복구가 아니다.

공개 `sourceKey/sourceVersion`과 `sections[].inputId`는 QRY의 **원본/발행 기준**을 그대로 사용한다. Draft 저장마다 이 값을 바꾸거나 해시에 Draft/사용자/revision을 넣지 않는다. Draft 결과는 `draftId + revision`으로 구별한다. 현재 Case 참조가 고정 기준과 달라지면 새 쓰기/표 조회는 409 `SOURCE_CHANGED`; 성공 Draft는 보존한다. 작업 조회는 기존 처리 기록을 읽어 결과 확인을 허용한다. source 접근권한 상실/삭제는 404다.

### 3.2 `DraftSummary`와 조회 DTO

아래 필드는 별도 표시가 없으면 항상 required/non-null이다.

| 필드 | 타입·의미 |
| --- | --- |
| `draftId`, `sourceCaseId` | UUID string, 양의 int64. 서버 소유자/고정 원본 연결 |
| `sourceKey`, `sourceVersion` | 기존 QRY 식별자. 원본은 불변 파일 등록 참조/기존 양식 연결을 재사용 |
| `status`, `statusLabel` | lifecycle `DRAFT/FINALIZED/DISCARDED`와 `편집 중/최종저장 완료/폐기`. 정의는 domain §5/§5.1 |
| `revision` | 현재 성공 revision. 생성 직후 0, 실패/접수 때 증가하지 않음 |
| `activeOperationId` | UUID 또는 명시적 null. CHANGE/FINALIZE 중 진행 요청이 없으면 null |
| `activeOperationEndpoint` | 서버 생성 상대 API path 또는 명시적 null. 진행 CHANGE는 EDT-05, FINALIZE는 EDT-07. activeOperationId와 함께 존재/함께 null. 재진입 시 올바른 종류로 폴링하기 위한 필드 |
| `finalizedCaseId` | int64 또는 명시적 null. FINALIZED에만 최종 Case ID. 임시저장에서는 null |
| `draftEndpoint` | EDT-02의 상대 API path. 서버가 생성 |
| `created` | EDT-01에만 boolean 추가. true 신규/false 재개; EDT-02에는 생략 |
| `sections`, `tableData` | EDT-02에만 추가. QRY-01과 같은 네 역할·표 metadata, 성공 revision의 현재 범위. `tableData`는 항상 null |

EDT-03의 data는 `draftId/revision/sourceKey/sourceVersion/tableData/page/editing`이다. `sections/inputId/created`는 보내지 않는다. `revision` query는 EDT-02에서 받은 현재 성공 번호여야 하며 과거 revision 조회 API는 이번 slice에 없다. 읽는 중 성공 번호가 바뀌어도 409 `REVISION_CONFLICT`; 다른 revision 값을 섞은 200은 금지한다. 처리 중에는 직전 성공 revision을 조회하며 미완성 변경을 섞지 않는다.

- `tableData`의 `columns/rows/inputCells/inputGuideRules/controls/displayUpdateRules`와 값 표시·빈 셀 의미는 QRY 상세 §5를 재사용한다. `rows[].id=rowNumber-1`은 유지한다. Draft에서는 행/좌표가 해당 성공 revision의 현재 위치다.
- `page` 필드·본문 최대2,000행·2 MiB 상한/분할/422는 QRY 상세 §5.1을 적용하되 **편집 metadata까지 포함한 전체 응답 bytes**로 계산한다. 모든 페이지는 같은 Draft revision이어야 한다.
- 빈 슬롯/헤더를 임의로 기존 논리 행으로 만들지 않는다. 실제 행과 빈 슬롯의 대응은 `editing.rowBindings`를 사용한다. `inputCells`나 좌표만 보고 쓰기 대상을 만들지 않는다.

### 3.3 `editing` — Draft 표 조회에만 붙이는 metadata

| 필드 | 타입·의미 |
| --- | --- |
| `capabilities` | `SET/CLEAR/ADD_ROW/DELETE_ROW` 각각 `READY/INCOMPLETE/NOT_ALLOWED/UNSUPPORTED`. READY만 호출 가능. 미정과 업무 불허·제품 미지원 구분 |
| `fields` | 안정 fieldKey별 정의 목록. `fieldKey`, `columnField`(기존 columnN), `dataType`(INTEGER/DECIMAL/TEXT), `policyStatus` 필수. policyStatus는 CREATE/SET/CLEAR 각각 §3.3 capabilities와 같은 네 상태를 갖는 object. CREATE READY에만 `create` 정의 제공. SET READY에는 setConstraints 필수이며 신규/기존 제약을 혼용하지 않음. CLEAR는 해당 상태로 허용 여부를 표현. 미완비 필수값을 false/null default로 추정하지 않음 |
| `fields[].create` | policyStatus.CREATE가 READY일 때 `required` boolean, `initial`, `constraints` 필수. initial은 `{kind:"EMPTY"}` 또는 `{kind:"VALUE",value:scalar}`. EMPTY는 화면 시작값이며 서버의 누락값 허용이 아님 |
| `fields[].create.constraints` / `fields[].setConstraints` | 신규 생성/기존 SET 각각 READY 정책의 typed 제약 object. INTEGER의 minimum/maximum, DECIMAL의 minimum, TEXT의 options 등 해당 제약만 제공. `{}`는 제약 없음이 확인된 경우만 허용. 임의 실행식은 제공하지 않음 |
| `rowBindings` | 이번 페이지 본문에 대응하는 `{rowNumber,rowId,origin}`. 원본/성공 추가 행은 UUID rowId와 SOURCE/ADDED, 미활성 슬롯은 rowId:null과 EMPTY_SLOT. 헤더는 포함하지 않음 |
| `editValues` | 이번 페이지의 활성 논리 행별 `{rowId,values:{fieldKey:scalar}}`. 편집 가능한 실제 typed 값이며 표시/반올림 문자열과 다름. 실제 빈 셀은 key 생략. 원본 수식/읽기 전용 셀은 넣지 않음 |

`fieldKey`는 동일 발행 표 안에서 안정된 의미 key다. 기존 `columnN`·Excel 좌표와 별도다. `rowId`는 Draft/표에 귀속하며 값/정렬/위치가 바뀌어도 유지한다. 다른 Draft/표의 ID, 삭제된 ID, EMPTY_SLOT에 대한 SET은 거부한다. 신규 행은 ADD_ROW로만 활성화한다. 동적 연도/월 열의 안정 식별과 control 쓰기는 §10의 다음 구조 계약에서 다룬다.

capabilities는 표/동작 공통 구조·계산/보존 정책의 준비도, fields의 policyStatus는 필드별 준비도다. SET/CLEAR는 둘 다 READY인 대상만 허용한다. ADD_ROW는 공통 정책과 필요한 신규 필드 정책이 모두 완비돼야 한다. 불허/미완비 field가 있다고 정상 조회를 빈 성공으로 바꾸지 않는다.

FE는 표시값을 숫자로 재해석해 전체 행을 되보내지 않는다. `editValues`와 사용자가 실제 변경한 값만 delta(변경분)로 보낸다. 서버도 생략된 기존 값을 유지한다. 숫자는 JSON number, 연도는 정수, 가격은 유한 10진수이며 숫자 문자열·null·수식은 허용하지 않는다. Decimal은 exponent 없는 JSON number로 채택했으며 FE는 손실 없는 10진 입력 처리를 준비한다. IEEE-754 변환으로 자릿수가 손실되는 값을 조용히 변환해 보내지 않는다. 서버는 10진 값을 보존하고 임의 반올림하지 않는다. BCPrices는 별도 업무 소수 자릿수 제한/저장 반올림을 추가하지 않는 것으로 채택했다. 숫자 정확도·전송/저장 표현 상한의 제품 검증은 §10의 BE 후속이다.

Sector 국가/하위부문은 domain 규칙45의 필수 자유입력 TEXT다. `create.required:true`, `initial:{kind:"EMPTY"}`로 표현하고 CREATE/SET의 constraints에 options를 제공하지 않는다. 별도 열거값 제약은 없으므로 해당 constraints는 `{}`이며 §4의 공통 TEXT 타입/크기 제한과 §4.1의 필수 검증은 유지한다. 목록/seed 부재로 CREATE/SET을 INCOMPLETE로 만들거나 POLICY_INCOMPLETE를 반환하지 않는다. 실제 동작의 준비도는 다른 필수 구조/계산·보존 정책을 별도로 따른다. 모델의 명칭 인식 제약은 §10의 후속 확인이며 임의 명칭의 모델 반영 성공을 보장하지 않는다. 기존 QRY-01/02 응답은 변경하지 않는다.

Monthly 국가는 Sector 국가와 같은 필수 자유입력 TEXT다. 신규 Q 수요량은 `initial:{kind:"VALUE",value:0}`, create.constraints/setConstraints의 minimum:0으로 표현하고 N Year는 INTEGER minimum:1000/maximum:9999, O Month는 INTEGER minimum:1/maximum:12다. 신규 M/N/O/Q는 create.required:true이며 P/R은 사용자 편집 field로 제공하지 않는다. Sector I·Monthly Q·BCPrices 세 가격의 field policyStatus.CLEAR는 NOT_ALLOWED다. 다른 필드/표의 CLEAR 상태는 별도 정책을 따르며 이번 채택만으로 표 동작 전체를 READY로 제공하지 않는다.

## 4. 변경 요청·순서·오류 위치

`ChangeRequest = {requestId, baseRevision, operations}`. 세 필드 모두 필수다. requestId는 FE가 새 저장 시 생성하는 UUID, baseRevision은 마지막 EDT-02/03의 성공 revision, operations는 순서 있는 배열이다. 임시저장은 이 배열 전체가 한 성공/실패 단위다. 비어 있지 않은 새 요청이 성공하면 실제 값이 같아도 성공 revision을 한 번 증가시키는 안이다.

| type | 필수 필드 | 의미 |
| --- | --- | --- |
| `ADD_ROW` | `commandId`, `tableId`, `clientRowKey`, `values:{fieldKey:scalar}` | 서버의 표별 생성 정책으로 신규 논리 행 생성. 임시키는 이 요청 안에서 유일. 실제 좌표/빈 슬롯/append 방식은 서버 정책이며 afterRowNumber를 받지 않음 |
| `SET` | `commandId`, `tableId`, `rowRef`, `fieldKey`, `value` | 한 값을 지정. rowRef는 `{rowId:UUID}` 또는 앞선 ADD_ROW의 `{clientRowKey:string}` 중 정확히 하나 |
| `CLEAR` | `commandId`, `tableId`, `rowRef`, `fieldKey` | 실제 빈 셀 생성. value는 금지. 0/빈 문자열/행 삭제와 다름. 해당 기존/신규 필드의 CLEAR 정책이 필요 |
| `DELETE_ROW` | `commandId`, `tableId`, `rowRef` | 허용된 논리 행 삭제. 전체 worksheet 행 삭제를 의미하지 않으며 연결·참조 보정은 표별 STRUCT 정책이 소유 |

commandId/clientRowKey/fieldKey는 제안상 `[A-Za-z][A-Za-z0-9_-]{0,63}`이다. commandId는 요청 안에서 유일하다. 요청의 알 수 없는 필드/type, JSON 중복 key, 중복 commandId/clientRowKey, 두 rowRef 동시 지정, 뒤에 나올 임시키 참조는 400이다. 임시키는 다른 요청에서 재사용할 영속 ID가 아니다.

명령은 배열 순서대로 적용한다. ADD_ROW → 해당 clientRowKey의 SET/CLEAR가 가능하고 같은 셀의 SET/CLEAR는 마지막 명령이 최종 값이다. 삭제 이후 같은 행 참조는 거부한다. 최종 상태에서 신규 필수값·중복·구조/파생 정책을 검사하므로 앞선 ADD의 누락을 뒤의 SET으로 채울 수 있으나 요청 종료까지 빠져 있으면 전체 실패다. ADD 후 DELETE도 허용 정책 범위 안에서만 처리하며 대응 ID는 재사용하지 않고 성공 rowMappings에 DELETED로 표시한다. 기존 값을 delta에서 생략한 것과 신규 필수값 누락을 구분하며 서버 기본0/타표 값 복사는 하지 않는다.

채택한 **초기 기술 제한**은 body UTF-8 1 MiB, operations 1~500개, ADD_ROW 최대100개, JSON 깊이8, scalar TEXT 최대4,096자다. 알려진 numeric/type/range는 비싼 작업 전 검사한다. 크기 초과는413, 형식/건수/타입 위반은400이다. 한도 조정/실측은 §10이며 이 값은 기존 제품 상한이나 성능 보장으로 확정된 것이 아니다.

### 4.1 Sector 입력·중복·I 저장의 채택 정책 연결

업무 의미·적용 범위는 domain 규칙45가 소유한다. 국가/하위부문은 사용자 최종 채택한 필수 자유입력 JSON 문자열이며 목록 포함 여부로 검증하지 않는다. 입력 문자열을 그대로 사용하며 명칭 자동 보정·별칭 통합을 하지 않는다. Year/Month는 INTEGER, I는0 이상 JSON number다. I를 저장하는 해당 행의 H/J는 BE가 함께 처리하고 공개 사용자 입력 field로 받지 않는다. 실제 fieldKey는 같은 발행 묶음의 D/E/F/G/I 대응 정의를 사용한다. 표별 구조/보존 등 해당 동작에 필요한 정책을 갖춘 요청을 전제로 다음처럼 분기한다.

| 제출 내용 | API/처리 결과 |
| --- | --- |
| 국가/하위부문 JSON 문자열 | 자유입력. 모델 목록에 없다는 이유로 OPTION_NOT_ALLOWED를 반환하지 않으며 목록/seed 부재를 POLICY_INCOMPLETE로 처리하지 않음 |
| 신규 국가/하위부문 필수값 누락 또는 명시적으로 제출한 빈 문자열·공백만 있는 값 | 422/BIZ-001, VALIDATION_FAILED + issue REQUIRED. 기존 delta의 필드 생략은 값 유지 |
| 국가/하위부문에 문자열 이외 타입 제출 | 400/REQ-003, INVALID_REQUEST. 문자열로 자동 변환하지 않음 |
| 연도1000~9999 정수·월1~12 정수 | 형식/범위상 유효. 별도 현재 연도/원본 연도 제한 없음. 실제 모델 반영 성공 보장은 아님 |
| 연도/월의 숫자 문자열·소수 | 400/REQ-003, INVALID_REQUEST. INTEGER 형식 위반 |
| 정수지만 연도/월 범위 밖 | 422/BIZ-001, VALIDATION_FAILED + issue OUT_OF_RANGE |
| 신규 행이 다른 활성 행과 국가·연도·월·하위부문 중복 | 422/BIZ-001, VALIDATION_FAILED + issue DUPLICATE_SECTOR_KEY. 수요량이 같아도 거부 |
| 기존 행 수정 결과가 다른 활성 행과 같은4필드 중복 | 같은422/DUPLICATE_SECTOR_KEY. I만 수정한 경우도 검사하며 자기 rowId는 제외 |
| 유효한 I=0 또는 양수 | 접수 후 모든 처리 성공 시에만 같은 revision의 사용자 값·H/J 파생 결과를 공개. 202만으로 저장 성공 아님 |
| 신규 필수 I 누락/빈값 | 422/BIZ-001, VALIDATION_FAILED + issue REQUIRED. 서버0 보정/operation 생성 없음 |
| Sector I의 명시적 CLEAR | 422/BIZ-001, VALIDATION_FAILED + issue CLEAR_NOT_ALLOWED. 해제는 허용된 논리 행 삭제를 사용하며 SET0과 구분 |
| 제출 I 음수 | 422/BIZ-001, VALIDATION_FAILED + issue OUT_OF_RANGE. H/J만 부분 반영하지 않음 |
| delta에서 기존 I 생략 | 기존 I 유지. 이 생략만으로 H/J 파생 갱신을 유발하지 않음 |
| 접수 후 검증/실행 실패 | §5/§6의 FAILED. I·H/J를 포함해 이번 요청 전체 취소, 직전 성공 Draft 보존 |

I 오류 issue는 I의 fieldKey와 요청 위치를 사용한다. 신규 행은 `/operations/0/values/{I필드키}`, SET은 `/operations/0/value` 형태이며 실제 명령 index/commandId·rowRef를 대응시킨다. 국가/하위부문 필수·연월 검증 issue도 해당 fieldKey와 실제 요청 위치를 사용한다. 행 단위 중복 issue는 fieldKey:null, 신규 ADD_ROW는 `/operations/0/values`, 기존 SET은 `/operations/0/rowRef`로 행을 가리키고 실제 commandId/rowRef를 제공한다. 다른 행의 값이나 ID를 오류에 노출하지 않는다.

중복은 신규/수정 행의 최종4필드와 같은 Draft/Sector의 전체 최종 활성 행을 비교하며 삭제된 행은 제외한다. 같은 행의 수요량 수정은 자기 행을 중복으로 보지 않지만, 다른 기존 행과 중복돼 있다면 거부한다. 다른 Draft/표는 비교하지 않고 이미 접수된 동일 requestId의 재전송은 §5의 기존 결과 조회가 우선이다. Sector I CLEAR는 금지이며 수동 입력 해제는 허용된 논리 행 삭제를 사용한다.

### 4.2 Monthly 입력·중복·수요량 저장/해제

업무 의미는 domain 규칙45를 따른다. M/N/O/Q에 대응하는 같은 발행 묶음의 fieldKey로 처리한다. 국가 자유입력 TEXT/빈값·연월 INTEGER와 범위·수요량 JSON number/음수·신규 필수 검증은 §4.1과 같은400 INVALID_REQUEST/422 REQUIRED·OUT_OF_RANGE 분류다. 목록 포함 검증은 하지 않는다. Q의 CLEAR는422/BIZ-001 VALIDATION_FAILED + issue CLEAR_NOT_ALLOWED다. 신규 Q 필수 누락을 서버가0으로 채우지 않으며 기존 delta의 Q 생략은 값 유지다.

같은 Draft/Monthly의 국가·연도·월 최종 조합 중복은 신규/기존 수정(Q만 수정한 경우 포함) 모두422/BIZ-001 VALIDATION_FAILED + issue DUPLICATE_MONTHLY_KEY다. 자기 rowId·최종 삭제 행은 제외한다. issue 위치/정보 노출 제한은 §4.1의 행 중복 형식과 같다. 여러 명령의 최종 활성 상태를 대조하고 동일 requestId 재전송은 기존 결과 조회를 따른다. 다른 Draft/Sector 등 다른 표를 비교하거나 기존 중복을 자동 정리하지 않는다.

신규 유효 Q 저장 또는 기존 Q의 명시적 SET 시 해당 행의 P 실제 blank/R=Q 실제 숫자값을 같은 성공 revision에 확정한다. P/R 사용자 쓰기는 허용하지 않고 새 수식/환산/반올림은 하지 않는다. Q 미제출 행의 P/R 일괄 보정은 없다. 실패는 요청 전체 취소/직전 성공 Draft 보존이며202 접수와 성공을 구분한다. 해제는 승인된 DELETE_ROW이고 빈값/SET0으로 대체하지 않는다.

### 4.3 BCPrices 기존 연도·가격 수정

업무 의미와 적용 범위는 domain §6.1을 따른다. 기존 Year SET은 신규와 같은 INTEGER1000~9999이며 형식 위반400/REQ-003 INVALID_REQUEST, 범위 위반422/BIZ-001 VALIDATION_FAILED + issue OUT_OF_RANGE다. 같은 Draft/BCPrices의 신규 추가 또는 Year SET 대상 행의 최종 연도가 다른 활성 행과 같으면422/BIZ-001 VALIDATION_FAILED + issue DUPLICATE_YEAR다. 자기 rowId·최종 삭제 행은 제외하고 여러 명령의 최종 상태를 대조한다. 가격만 수정한 요청으로 기존 연도 중복을 소급 거부/정리하지 않는다. 연도 변경에도 rowId를 유지한다. 중복 issue는 fieldKey:null, ADD_ROW는 해당 values 위치, SET은 해당 rowRef 위치로 §4.1과 같은 행 단위 형식을 사용한다.

기존 세 가격 SET은0 이상 JSON number이며 타입 위반400 INVALID_REQUEST, 음수422 OUT_OF_RANGE다. 가격 CLEAR는422 CLEAR_NOT_ALLOWED, 신규 필수 가격 누락은422 REQUIRED다. SET의 null/숫자 문자열/빈 문자열은 기존 typed 요청 형식 오류이며 빈값이나0으로 자동 변환하지 않는다. 생략한 기존 가격은 유지한다. 저장값에 별도 소수 자릿수 제한/반올림을 추가하지 않고 실제 입력 숫자를 보존한다. 예:2029행이 있을 때 기존2028행을2029로 변경하면 거부하고 가격3.125는3.125로 보존한다. BCPrices Year CLEAR/행 삭제 및 다른 표 동작은 이번 승인으로 확정하지 않는다.

## 5. revision·재전송·비동기 결과

새 EDT-04/06 접수 전 인증/소유권 → 같은 requestId의 기존 요청 확인 → 현재 원본/정책·편집 상태 → 진행 요청/성공 revision 대조 → 접수 순서로 처리한다. 이미 접수된 동일 요청은 최신 revision이 달라도 같은 operation을 반환한다. 비교는 역직렬화한 명령의 의미/순서·table/row/field·값·baseRevision 기준이며 object key 순서/공백은 무시하고 배열 순서는 보존한다. 문자열은 trim/대소문자 변환 없이 비교하며 JSON number는 10진 숫자값으로 비교해3.20과3.2는 같다. 원본 파일 내용 SHA를 추가하라는 요구가 아니다.

- 같은 Draft/requestId·같은 종류/내용: 같은 operationId/처리 결과. 행·revision 중복 생성 금지. 진행 중202, 종료 후200.
- 같은 Draft/requestId·다른 종류 또는 내용: 409 `IDEMPOTENCY_CONFLICT`. 같은 키로 body를 바꿔 보내지 않는다.
- 다른 requestId로 기준 revision이 낡음: 409 `REVISION_CONFLICT`. 진행 요청이 있으면 409 `DRAFT_BUSY`. 두 조건이 겹치면 BUSY를 우선한다.
- 접수 전 거부: operation을 만들지 않는다. 같은 ID의 거부 후 재전송은 새 검증이지만 FE는 수정 요청에 새 ID를 쓴다.
- 접수 후 실패: 해당 operation은 FAILED로 고정. 같은 요청 재전송은 실패 기록을 반환하고 재실행하지 않는다. 재시도는 현재 revision 확인 후 새 requestId를 쓴다.
- 성공/실패 원자성·늦은 완료 차단은 domain §5.1의 채택 기준을 따른다. QUEUED/RUNNING 동안 새로운 성공값을 미리 공개하지 않는다. 규칙64의 전체 상한에는 대기열도 포함하며 timeout 확정은 FAILED/TIMEOUT이다. 네트워크/DB 응답 유실로 실제 commit을 확인할 수 없으면 FE는 실패를 추정하지 않는다. 재조회 후 성공 commit이 있으면 같은 성공 결과를 반환한다.

### 5.1 `OperationView`

| 필드 | 필수·타입·의미 |
| --- | --- |
| `operationId`, `requestId`, `draftId` | UUID, 모두 required. 서버 작업/클라이언트 중복 식별/소유 Draft |
| `status`, `statusLabel` | required. QUEUED/대기 중, RUNNING/처리 중, SUCCEEDED/저장 완료, FAILED/저장 실패 |
| `baseRevision` | required integer. 요청이 제출한 기준, 결과 조회 때도 바꾸지 않음 |
| `resultRevision` | required integer 또는 null. SUCCEEDED일 때만 baseRevision+1, 나머지는 null |
| `rowMappings` | required array. SUCCEEDED의 신규 행마다 `{commandId,clientRowKey,tableId,rowId,rowState}`. rowState ACTIVE/DELETED. 신규 행 없음 또는 성공 전/실패는 [] |
| `affectedTableIds` | required string array. 성공 후 재조회할 영향 표 ID(서버가 계산/연결한 표 포함). 성공 전/실패는 [] |
| `failure` | required object 또는 null. FAILED에만 §6 Problem, 다른 상태는 null |
| `statusEndpoint` | required 상대 API path. EDT-05 위치 |

과거 성공 operation의 resultRevision과 실패의 failure.currentRevision은 나중에 Draft가 더 저장돼도 그대로다. FE는 그 결과만으로 현재 revision을 덮어쓰지 않고 EDT-02를 재조회한다. `status:true/SUC-000`은 접수/조회 자체의 성공이며 **data.status:SUCCEEDED만 임시저장 성공**이다. 최종 Case 생성 성공이나 Run 완료를 의미하지 않는다.

## 6. 채택한 공통 오류·필드 위치

기존 BaseResponse의 `status/code/message/data`와 실제 ResultCode wire code를 유지한다. FE의 행/필드 강조와 충돌별 복구를 위해 **새 EDT operation에 한해** 업무 오류 `data`에 typed Problem을 싣는 공통 BE 안을 채택했다. QRY의 실패 `data:null` 계약은 유지한다. `BaseResponseFactory.create(error,data)`는 이미 있으나 현재 GlobalExceptionHandler가 이 typed 형태를 반환하지는 않는다. 이 초안은 공통 handler/별도 validator 클래스를 새로 만들도록 요구하지 않으며 후속 구현에서 이 경로에만 안전하게 적용·검증해야 한다.

`Problem = {reason, currentRevision, issues}`: reason은 아래 코드, currentRevision은 소유 Draft 충돌/검증을 판정한 시점의 성공 번호 또는 해당 없으면 null, issues는 항상 배열이다. issue는 `commandId`(관련 없으면 null), `tableId/rowRef/fieldKey`(대상 없으면 null), `path`(요청 body의 JSON Pointer), `reason`이다. 누락 필드는 실제 body에 없더라도 `/operations/0/values/ttf`처럼 기대 위치를 준다. 원본/거부값·수식·provider 메시지·다른 사용자 정보는 싣지 않는다. commandId가 없는 파싱 오류는 공통 안전 오류/data:null을 허용하며 message 파싱을 요구하지 않는다.

| 조건 | HTTP / ResultCode / wire | Problem reason·FE 동작 |
| --- | --- | --- |
| JSON/필수 형식·타입·알 수 없는 fieldKey·명령/키 중복·잘못된 임시키 | 400 / REQ_INVALID_PARAMETER / REQ-003 (JSON 문법은 REQ_INVALID_JSON/REQ-005) | INVALID_REQUEST, 가능한 issue 위치. 요청 수정 |
| body bytes 초과 | 413 / REQ_INVALID_PARAMETER / REQ-003 | PAYLOAD_TOO_LARGE, 분할 전 원자적 변경 단위 확인. 자동 분할 저장하지 않음 |
| source/Draft/table/row 없음·미표시·다른 소유자/tenant·source 접근 불가 | 404 / RSC_NOT_FOUND / RSC-001 | 공통 data:null. 존재 유출 없이 선택/접근 재확인 |
| 인증/role 불가 | 401/403 / 기존 AUTH 코드 | 공통 data:null. 로그인/권한 흐름 |
| 진행 Draft와 source 기준 불일치 / 현재 source 변경 | 409 / BIZ_RULE_VIOLATION / BIZ-001 | DRAFT_SOURCE_CONFLICT / SOURCE_CHANGED, 기준 재조회. 기존 Draft 유지·자동 rebase 금지 |
| 낡은 성공 revision | 409 / BIZ_RULE_VIOLATION / BIZ-001 | REVISION_CONFLICT + currentRevision. 로컬 변경과 최신값 대조 |
| 진행 요청 존재 / 동일 요청ID의 다른 내용 / 편집 종료 Draft | 409 / BIZ_RULE_VIOLATION / BIZ-001 | DRAFT_BUSY / IDEMPOTENCY_CONFLICT / DRAFT_NOT_EDITABLE. 기존 작업 확인/새 ID/최종 Case로 이동 |
| 동작에 필요한 정책 미완비 또는 제품 미지원 | 409 / BIZ_RULE_VIOLATION / BIZ-001 | POLICY_INCOMPLETE / POLICY_UNSUPPORTED. 해당 동작 중단. 빈 규칙/required=false로 대체 금지 |
| 신규 필수 누락/필수 문자열 빈값, 선택형 필드의 금지 선택값/CLEAR·중복 연도/Sector·Monthly 키·불허 구조 명령 | 422 / BIZ_RULE_VIOLATION / BIZ-001 | VALIDATION_FAILED, issues reason REQUIRED/OUT_OF_RANGE/OPTION_NOT_ALLOWED/CLEAR_NOT_ALLOWED/DUPLICATE_YEAR/DUPLICATE_SECTOR_KEY/DUPLICATE_MONTHLY_KEY/ACTION_NOT_ALLOWED. 수정 후 새 요청 |
| Draft 표 최소 응답의 2 MiB 초과 | 422 / BIZ_RULE_VIOLATION / BIZ-001 | RESPONSE_TOO_LARGE, 해당 표 조회 중단. 값/정책 잘라내기 금지 |
| 접수 전 내부 저장/원본 읽기 실패 | 500 / SYS_INTERNAL_ERROR / SYS-001 | 공통 안전 메시지/data:null. 접수 여부 불명 시 같은 requestId 재전송 |
| 접수 후 검증·계산·구조/보존·저장·timeout 실패 | EDT-05/07은 200 / SUC-000 | data.status FAILED, failure.reason VALIDATION_FAILED/CALCULATION_FAILED/STRUCTURE_VALIDATION_FAILED/PRESERVATION_FAILED/PERSISTENCE_FAILED/TIMEOUT/SOURCE_CHANGED. 늦게 발견한 정책/기준 충돌은 POLICY_INCOMPLETE/POLICY_UNSUPPORTED/REVISION_CONFLICT. 직전 성공 유지 |

검증 가능한 필수/허용값·참조는 접수 전에 거부하는 것이 기본이다. worker에서만 확인 가능한 동일 오류는 operation.failure에 넣으며 성공 변경은 없다. 오류 reason은 채택한 공개 Problem enum 기준이며 기존 ResultCode wire enum을 재사용한다. body와 관계없는 작업 실패 issue는 []로 반환한다. 서버 오류 메시지는 고정된 안전 문구만 사용한다.

## 7. 대표 JSON — 문서 mock

모든 ID·값은 가상이다. `OUTPUT/BCPrices/base_case_prices`는 T-083에 대응시킬 **예시 runtime ID**이며 실제 발행 catalog/FE 이름을 확정한 것이 아니다. `year/hh/eastAsiaSpot/ttf`도 fieldKey 추천이다. 아래 성공 시나리오는 필요한 입력/구조/계산·보존 정책이 완비됐다고 가정한 FE mock이며 현재 일반 동작의 READY 선언이 아니다. 가격/연도는 시장값·모델 지원기간이나 F-O-04 복사값이 아니다.

### 7.1 Draft 생성·재개

EDT-01, source Case 101. sourceVersion은 기존 QRY에서 받은 설명용 형식이다.

```json
{"sourceVersion":"sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f"}
```

HTTP200 신규 생성. 재개는 같은 draftId/현재 revision을 반환하고 created:false다.

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "draftId":"d0000000-0000-4000-8000-000000000001","created":true,
    "sourceCaseId":101,
    "sourceKey":"sc1_1111111111111111111111111111111111111111111111111111111111111111",
    "sourceVersion":"sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "status":"DRAFT","statusLabel":"편집 중","revision":0,
    "activeOperationId":null,"activeOperationEndpoint":null,"finalizedCaseId":null,
    "draftEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001"
  }
}
```

### 7.2 선택 표의 편집 metadata 발췌

EDT-03 `data.editing` 중 fields/rowBindings/editValues 예시만 발췌했다. 전체 EDT-03은 §3.2의 tableData/page 등을 반드시 포함한다. 원본 기존 행8의 예시 rowId와 표시 문자열과 별도의 실제 편집 숫자를 보여준다. 기존 연도 SET·세 가격 SET의 수치제약과 세 가격 CLEAR 금지는 §4.3에서 사용자 채택한 정책이다. 아래 metadata는 필요한 다른 정책까지 갖춘 경우의 형태이며 Year CLEAR는 여전히 INCOMPLETE다. 실제 발행은 해당 동작의 구조/계산·보존 정책까지 완비돼야 capabilities를 READY로 제공한다.

```json
{
  "fields": [
    {
      "fieldKey": "year",
      "columnField": "column1",
      "dataType": "INTEGER",
      "policyStatus": {
        "CREATE": "READY",
        "SET": "READY",
        "CLEAR": "INCOMPLETE"
      },
      "create": {
        "required": true,
        "initial": {
          "kind": "EMPTY"
        },
        "constraints": {
          "minimum": 1000,
          "maximum": 9999
        }
      },
      "setConstraints": {
        "minimum": 1000,
        "maximum": 9999
      }
    },
    {
      "fieldKey": "hh",
      "columnField": "column2",
      "dataType": "DECIMAL",
      "policyStatus": {
        "CREATE": "READY",
        "SET": "READY",
        "CLEAR": "NOT_ALLOWED"
      },
      "create": {
        "required": true,
        "initial": {
          "kind": "EMPTY"
        },
        "constraints": {
          "minimum": 0
        }
      },
      "setConstraints": {
        "minimum": 0
      }
    },
    {
      "fieldKey": "eastAsiaSpot",
      "columnField": "column3",
      "dataType": "DECIMAL",
      "policyStatus": {
        "CREATE": "READY",
        "SET": "READY",
        "CLEAR": "NOT_ALLOWED"
      },
      "create": {
        "required": true,
        "initial": {
          "kind": "EMPTY"
        },
        "constraints": {
          "minimum": 0
        }
      },
      "setConstraints": {
        "minimum": 0
      }
    },
    {
      "fieldKey": "ttf",
      "columnField": "column4",
      "dataType": "DECIMAL",
      "policyStatus": {
        "CREATE": "READY",
        "SET": "READY",
        "CLEAR": "NOT_ALLOWED"
      },
      "create": {
        "required": true,
        "initial": {
          "kind": "EMPTY"
        },
        "constraints": {
          "minimum": 0
        }
      },
      "setConstraints": {
        "minimum": 0
      }
    }
  ],
  "rowBindings": [
    {
      "rowNumber": 8,
      "rowId": "a0000000-0000-4000-8000-000000000008",
      "origin": "SOURCE"
    }
  ],
  "editValues": [
    {
      "rowId": "a0000000-0000-4000-8000-000000000008",
      "values": {
        "year": 2025,
        "hh": 3.1,
        "eastAsiaSpot": 10.2,
        "ttf": 9.4
      }
    }
  ]
}
```

Sector 신규 I는 initial `{kind:"VALUE",value:0}`과 create.constraints의 minimum:0으로 표현한다. I 누락을 서버가0으로 채우지 않는다. H/J·음수 금지와 국가/하위부문 필수 자유입력·연월 범위·중복 금지는 domain 규칙45를 따른다. 국가/하위부문은 TEXT이며 목록/seed 연결은 없다. Year는 INTEGER minimum:1000/maximum:9999, Month는 INTEGER minimum:1/maximum:12다. 자유입력 명칭에 대한 모델 인식 제약만 후속 확인하고, 다른 구조/보존 정책의 미완료를 READY로 대체하지 않는다.

### 7.3 행 추가 + 신규 임시키 수정 + 기존 가격 수정 접수

EDT-04, 위 draftId, baseRevision0. 같은 표에2036년 행은 아직 없다고 가정한다. 순서대로 신규 행을 만들고 그 행의 hh를3.25로 바꾼 뒤 기존 행의 ttf만 수정한다. 기존 행의 나머지 필드는 보내지 않아 유지된다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000001","baseRevision":0,
  "operations":[
    {"commandId":"add1","type":"ADD_ROW","tableId":"OUTPUT/BCPrices/base_case_prices","clientRowKey":"new1","values":{"year":2036,"hh":3.2,"eastAsiaSpot":10.5,"ttf":9.7}},
    {"commandId":"setNew","type":"SET","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"clientRowKey":"new1"},"fieldKey":"hh","value":3.25},
    {"commandId":"setExisting","type":"SET","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"ttf","value":9.8}
  ]
}
```

HTTP202. revision은 아직0이다(접수 성공, 저장 미완료).

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "operationId":"c0000000-0000-4000-8000-000000000001","requestId":"b0000000-0000-4000-8000-000000000001",
    "draftId":"d0000000-0000-4000-8000-000000000001","status":"QUEUED","statusLabel":"대기 중",
    "baseRevision":0,"resultRevision":null,"rowMappings":[],"affectedTableIds":[],"failure":null,
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000001"
  }
}
```

### 7.4 실제 성공 결과의 형태

EDT-05 HTTP200. FE는 이때 신규 임시키를 rowId로 대응하고 폴링을 종료한다. EDT-02/03 revision1 재조회에서 신규 hh=3.25, 기존 ttf=9.8을 확인한다. 성공 응답에 현재 rowNumber를 고정하지 않고 새 조회의 rowBindings를 사용한다.

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "operationId":"c0000000-0000-4000-8000-000000000001","requestId":"b0000000-0000-4000-8000-000000000001",
    "draftId":"d0000000-0000-4000-8000-000000000001","status":"SUCCEEDED","statusLabel":"저장 완료",
    "baseRevision":0,"resultRevision":1,
    "rowMappings":[{"commandId":"add1","clientRowKey":"new1","tableId":"OUTPUT/BCPrices/base_case_prices","rowId":"a0000000-0000-4000-8000-000000000021","rowState":"ACTIVE"}],
    "affectedTableIds":["OUTPUT/BCPrices/base_case_prices"],"failure":null,
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000001"
  }
}
```

§7.3의 동일 body/requestId를 다시 보내면 HTTP200으로 이 operation/결과를 반환한다. 신규 행을 또 만들거나 revision2로 증가시키지 않는다. 다른 requestId로 같은2036년 행을 추가하면 신규 중복 정책으로 거부하며 요청 재전송과 구별한다.

### 7.5 신규 필수 누락

성공 이후 revision1, 별도2037년 신규 행에 ttf가 없다. 마지막까지 값을 채우지 않아 접수 전422이며 operation을 만들지 않는다. 기존 행에서 ttf를 생략해 유지한 경우와 다르다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000002","baseRevision":1,
  "operations":[{"commandId":"addMissing","type":"ADD_ROW","tableId":"OUTPUT/BCPrices/base_case_prices","clientRowKey":"new2","values":{"year":2037,"hh":0,"eastAsiaSpot":10.5}}]
}
```

```json
{
  "status":false,"code":"BIZ-001","message":"The operation violates a business rule.",
  "data":{
    "reason":"VALIDATION_FAILED","currentRevision":1,
    "issues":[{"commandId":"addMissing","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"clientRowKey":"new2"},"fieldKey":"ttf","path":"/operations/0/values/ttf","reason":"REQUIRED"}]
  }
}
```

### 7.6 revision 충돌

현재 revision1이며 진행 요청은 없다. 다른 탭이 낡은 revision0으로 보낸 새 requestId를 거부한다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000003","baseRevision":0,
  "operations":[{"commandId":"staleSet","type":"SET","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"ttf","value":10}]
}
```

HTTP409. revision은1 유지이며 FE는 최신 Draft와 미저장 변경을 대조한다.

```json
{
  "status":false,"code":"BIZ-001","message":"The operation violates a business rule.",
  "data":{"reason":"REVISION_CONFLICT","currentRevision":1,"issues":[{"commandId":null,"tableId":null,"rowRef":null,"fieldKey":null,"path":"/baseRevision","reason":"STALE_REVISION"}]}
}
```

### 7.7 접수 후 실행 실패

revision1에서 별도2037년 행을 추가하는 유효 입력을 접수했다고 가정한다. 이 예시는 전체120초 상한 뒤 TIMEOUT 실패이며 계산 오류를 BCPrices의 확정 계산 동작으로 만들지 않는다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000004","baseRevision":1,
  "operations":[{"commandId":"addTimeout","type":"ADD_ROW","tableId":"OUTPUT/BCPrices/base_case_prices","clientRowKey":"new3","values":{"year":2037,"hh":3.3,"eastAsiaSpot":10.6,"ttf":9.9}}]
}
```

EDT-05 HTTP200 — 작업 조회 성공/임시저장 실패. resultRevision은 null이며 직전 성공 revision1과 §7.4의 저장값·신규 행은 보존하고 new3은 추가하지 않는다.

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "operationId":"c0000000-0000-4000-8000-000000000004","requestId":"b0000000-0000-4000-8000-000000000004",
    "draftId":"d0000000-0000-4000-8000-000000000001","status":"FAILED","statusLabel":"저장 실패",
    "baseRevision":1,"resultRevision":null,"rowMappings":[],"affectedTableIds":[],
    "failure":{"reason":"TIMEOUT","currentRevision":1,"issues":[]},
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000004"
  }
}
```

같은requestId 재전송은 이 실패 결과만 반환한다. 새 저장 시도는 EDT-02의 현재revision을 확인하고 새requestId로 보낸다. 새 operation의 성공을 확인하기 전 FE가2037년 행을 성공 저장으로 표시하지 않는다.

## 8. FE 선행 준비·OpenAPI/검증 인계

| 지금 문서 mock으로 준비 | 정책/후속 검증 뒤 실제 연결 |
| --- | --- |
| 기존 QRY 탐색과 Draft 진입, 공통 표/편집값·rowId 대응, 신규 임시키/필수 입력 강조 | 실제 catalog/field binding·seed/Case 연결 및 해당 동작 정책 완비 |
| 순서 있는 SET/CLEAR/ADD_ROW/DELETE_ROW 명령 구성, revision·requestId 유지 | 표별 CLEAR/삭제·빈 슬롯/범위 확장·연결/참조·계산/보존 검증 |
| 202 접수·진행/종료 폴링, 실패 시 마지막 성공값 유지, 충돌/재전송 UI | durable 접수/worker·동시성/복구·늦은 완료 차단·전체 시간/정리 제품 검증 |
| §9 최종저장 접수/진행·완료/실패, 응답 유실 후 같은 요청 재전송·재진입 | 동일 최종 Case·4파일/양식/FINALIZED 원자성·부분 업로드/DB commit 불명·재시작·정리의 실제 제품 검증 |

OpenAPI 생성 시 이 spec이 신규 EDT 계약 정본 후보이며 annotation/generated OpenAPI는 동기화 대상이다. operation summary는 §3, description은 §2 호출 시점/목적과 §3~6의 요청·부작용·응답별 FE 분기를 사용한다. `@Schema/@Parameter`에는 required/nullable/enum·범위·UUID/sourceVersion 형식과 rowRef oneOf를 구조화하고, 명령은 type별 DTO/oneOf와 §7의 operation별 예시로 표현한다. `@ApiResponses`는 §6의 HTTP/ResultCode/Problem 및 202 접수·200 실패작업 조회 예시까지 구조화한다. field/type/오류를 message 문자열로만 설명하지 않는다.

후속 구현에서 검증할 행동은 동시 생성1건, 동일 요청의 동시 재전송/응답 유실, 다른 payload 충돌, 성공 revision 일괄 반영, 실패/timeout/늦은 완료 후 값·행·파생 결과 보존, 페이지/revision·원본 기준 충돌, 타사용자/tenant/source 접근 차단, 로그에 셀/거부값 없음이다. 물리 저장은 DDL §6.3의 공통7개 테이블/FK·유일성/검증된 JSON text·성공 경계로 정의했다. 기존 master 경계의 짧은 transaction/조건 갱신을 실제 구현/DBA 준비에서 검증한다. 무제한 JSON 질의 계층이나 새 범용 validator를 구현 요구로 추가하지 않는다.

이번 문서 검증은 JSON parse·ID/필드/상태·실패 보존 흐름과 링크·diff 확인이다. endpoint/DTO/annotation이 없으므로 generated OpenAPI·Gradle/HTTP/DB/worker 행동 검증은 수행하지 않는다. 구현 시 실제 생성 결과를 확인해야 하며 문서 JSON 통과가 제품 Pass를 대신하지 않는다.

## 9. 최종저장·실패 복구 계약 — EDT-06/07

### 9.1 요청과 접수

공통 root/권한/개인 Draft 소유·source 접근/Problem은 §2·§6을 그대로 사용한다. `POST /input-drafts/{draftId}/finalizations`는 application/json이며 파일 multipart/파일번호/경로/Run 옵션을 받지 않는다. Draft가 고정한 원본4개와 성공 변경만 사용한다. 변경 없는 revision0도 허용한다. 이 요청이 미완비 표별 입력/계산 정책을 자동 승인하지 않는다.

| FinalizationRequest | 필수·검증 |
| --- | --- |
| `requestId` | required UUID. CHANGE/FINALIZE 공통 중복 키 공간 |
| `baseRevision` | required, §3의 revision 범위. 현재 성공 revision과 같아야 함 |
| `title` | required string. 기존 Case 생성과 같이 Java String.trim() 후 비어 있지 않고 Java String.length() 200 이하. 정규화된 제목을 새 Case에 사용 |
| `description` | optional string 또는 null. 생략은 null과 동일. 문자열은 그대로 보존, UTF-8 65,535 bytes 이하(기존 Case TEXT 저장 한계), 빈 문자열과 null은 구분 |

body UTF-8 1 MiB·깊이8, 알 수 없는 key/중복 JSON key/잘못된 타입은 거부한다. title trim/description 생략 정규화 후 요청 종류·baseRevision·제목/설명을 의미 비교한다. requestId 자체와 object key 순서/공백은 비교 내용에서 제외한다. 공개 변경 DTO와 내부 저장 JSON이 같은 schema일 필요는 없다.

접수 순서는 인증/소유권·source 접근 → 기존 requestId 확인 → FINALIZED 재클릭 처리 → DRAFT/고정 기준 → 진행 요청 → 성공 revision 대조 → 필요한 정책/기준 확인 → 요청/진행 슬롯 DB commit이다. 저장되지 않은 FE 변경은 자동 포함하지 않는다. 새 접수/진행 중 재전송은 HTTP202/SUC-000, 종료 요청 재전송은 HTTP200/SUC-000이다. `Location` 헤더는 statusEndpoint와 같은 EDT-07 상대 path를 준다. `Retry-After: 2`는 202에 제공하며 FE는 기존2초 폴링을 사용한다.

- 같은 requestId/동일 종류·내용이면 revision/lifecycle이 진행돼도 같은 operation. 기존 키가 CHANGE이거나 내용이 다르면 409 IDEMPOTENCY_CONFLICT.
- 다른 요청이 진행 중이면 409 DRAFT_BUSY(CHANGE/FINALIZE 구분 없음), 그 외 낡은 baseRevision은 409 REVISION_CONFLICT. 접수 거부는 operation을 만들지 않는다.
- 이미 FINALIZED인 Draft에 새 requestId를 보내도 최종 revision/정규화 제목·설명이 기존 성공 요청과 같으면 **원래 성공 operation/requestId와 같은 Case**를 HTTP200으로 반환한다. 새 요청/Case는 만들지 않는다. 다르면409 DRAFT_NOT_EDITABLE이고 Draft 재조회로 기존 Case를 확인한다. 폐기 Draft는409 DRAFT_NOT_EDITABLE이다.
- 접수 뒤에는 성공 revision을 잠그는 장시간 transaction 대신 진행 슬롯으로 새 변경/최종저장 접수를 막는다. lifecycle은 처리 중 DRAFT, 성공 때만 FINALIZED다. 최종저장으로 revision은 증가하지 않는다.

### 9.2 `FinalizationView`와 조회

`GET /input-drafts/{draftId}/finalizations/{operationId}`는 해당 Draft의 FINALIZE 작업만 조회한다. CHANGE operationId/다른 Draft 작업은404다. EDT-05도 FINALIZE ID는404다. 종료 후에도 같은 결과를 조회할 수 있고 진행/종료 모두 HTTP200/SUC-000이다.

| 필드 | 필수·타입·의미 |
| --- | --- |
| `operationId`, `requestId`, `draftId` | required UUID. 완료 재클릭으로도 원래 성공 요청 ID를 반환할 수 있음 |
| `status`, `statusLabel` | required. QUEUED/대기 중, RUNNING/처리 중, SUCCEEDED/최종저장 완료, FAILED/최종저장 실패 |
| `baseRevision` | required integer. 접수한 대상 revision, 나중에도 불변. resultRevision은 이 DTO에 없음 |
| `finalizedCaseId` | required int64 또는 null. SUCCEEDED에만 새 Case ID |
| `files` | required array. SUCCEEDED에만 `{role,fileNo}` 네 건, SUPPLY/DEMAND/TRADE/OUTPUT 순서. 그 외 []. 파일번호는 기존 양의 int64이며 저장 경로/준비 object key는 미공개 |
| `caseEndpoint` | required 상대 API path 또는 null. SUCCEEDED에만 `/api/v1/mi/fundamental-model/cases/{caseId}`. Case 최신 상태/파일 조회용이며 Run 시작 path가 아님 |
| `failure` | required Problem 또는 null. FAILED만 안전한 오류, 다른 상태는 null |
| `statusEndpoint` | required EDT-07 상대 path |

SUCCEEDED는 새 INDEPENDENT Case/파일4개·고정 발행 연결·FINALIZED와 작업 성공이 같은 DB 확정으로 공개됐다는 뜻이다. 새 Case의 생성 상태는 PENDING이고 lastRunId는 null이다. 이후 Run에 따른 Case 상태 변화는 기존 Case 조회에서 확인한다. FinalizationView의 성공 결과는 이후에도 바뀌지 않는다. `status:true/SUC-000` 접수/조회와 data.status:SUCCEEDED를 구분한다.

### 9.3 오류·실패/복구에서 FE가 할 일

§6의 HTTP/ResultCode를 재사용한다. 요청 title/description 오류는400 INVALID_REQUEST와 `/title`·`/description` 위치, body 초과는413 PAYLOAD_TOO_LARGE다. 늦게 발견한 정책/원본 기준 오류는 failure에 같은 reason을 사용한다. 승인한 공통 저장 규칙을 미완비 표 정책으로 대체하지 않는다.

| 관찰한 상황 | 계약·FE 동작 |
| --- | --- |
| POST 응답/폴링 연결 끊김, FE 대기시간 경과 | 실패 확정 아님. 같은 requestId/동일 body 재전송 또는 statusEndpoint 조회. 새 요청ID/자동 Case 생성 금지 |
| 계산/구조·보존 검증 실패 | 작업 FAILED, failure.reason은 CALCULATION_FAILED/STRUCTURE_VALIDATION_FAILED/PRESERVATION_FAILED/VALIDATION_FAILED 등 §6 enum. finalizedCaseId null/files[]/caseEndpoint null. EDT-02/03은 직전 성공 Draft 유지 |
| 일부 업로드·파일/Case/양식 연결 실패 | FAILED/PERSISTENCE_FAILED. 성공한 일부 파일을 반환하지 않음. 서버가 전용 준비 기록으로 정리하며 FE가 파일 삭제를 호출하지 않음. 실패/정리 지연은 Case 성공이 아님 |
| DB commit 응답 유실·서버 재시작 | 실제 DB 기록 확인 전 FAILED/완료 Case를 추정하지 않음. 같은 작업을 재조회. 성공 commit이면 같은 Case, rollback/실패가 확정되면 FAILED. QUEUED는 같은 요청으로 복구하고 종료된 RUNNING은 서버 실패 확정 후 새 ID로만 재시도(domain §5.3) |
| timeout 확정 | FAILED/TIMEOUT, 직전 성공 Draft 유지. 접수부터 전체120초 상한/늦은worker 차단은 domain 규칙64·§5.3. FE timeout은 이 상태를 대신하지 못함 |
| FAILED 조회/같은 요청 재전송 | 저장된 실패 결과 반환, 자동 재실행 없음. EDT-02로 현재 revision 확인 후 사용자가 재시도할 때 새 requestId |
| SUCCEEDED 조회/같은 요청 재전송 | 같은 operation/Case 반환. FE는 finalizedCaseId/caseEndpoint로 이동, 별도 Run만 기존 흐름으로 요청 |
| 서버에서 아직 접수 여부도 확인 불가(5xx/통신 오류) | 안전한 공통 오류, 일부 Case ID/업로드 결과 미공개. 같은 요청을 나중에 확인하며 성공/실패 화면으로 단정하지 않음 |

실제 DB/4파일·양식/Draft 성공 경계와 업로드 준비/정리·늦은PUT/worker 차단은 domain §5.2~5.3·DDL §6.3을 따른다. 기존 일반 원본 Case 업로드 뒤 별도 양식 등록 경계를 최종저장에 적용하지 않는다.

### 9.4 대표 JSON — 문서 mock

EDT-06 요청:

```json
{
  "requestId": "a16d2400-8b00-4d00-9000-000000000006",
  "baseRevision": 1,
  "title": "수요 조정 검토",
  "description": null
}
```

EDT-06 HTTP202 접수(기존 BaseResponse wrapper):

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "operationId": "b16d2400-8b00-4d00-9000-000000000006",
    "requestId": "a16d2400-8b00-4d00-9000-000000000006",
    "draftId": "d16d2400-8b00-4d00-9000-000000000001",
    "status": "QUEUED",
    "statusLabel": "대기 중",
    "baseRevision": 1,
    "finalizedCaseId": null,
    "files": [],
    "caseEndpoint": null,
    "failure": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d16d2400-8b00-4d00-9000-000000000001/finalizations/b16d2400-8b00-4d00-9000-000000000006"
  }
}
```

EDT-07 HTTP200 성공의 data(동일 요청 재전송도 같은 결과):

```json
{
  "operationId": "b16d2400-8b00-4d00-9000-000000000006",
  "requestId": "a16d2400-8b00-4d00-9000-000000000006",
  "draftId": "d16d2400-8b00-4d00-9000-000000000001",
  "status": "SUCCEEDED",
  "statusLabel": "최종저장 완료",
  "baseRevision": 1,
  "finalizedCaseId": 4101,
  "files": [
    {"role": "SUPPLY", "fileNo": 5101},
    {"role": "DEMAND", "fileNo": 5102},
    {"role": "TRADE", "fileNo": 5103},
    {"role": "OUTPUT", "fileNo": 5104}
  ],
  "caseEndpoint": "/api/v1/mi/fundamental-model/cases/4101",
  "failure": null,
  "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d16d2400-8b00-4d00-9000-000000000001/finalizations/b16d2400-8b00-4d00-9000-000000000006"
}
```

EDT-07 HTTP200 실패의 data(같은 요청 재전송은 이 결과를 유지):

```json
{
  "operationId": "b16d2400-8b00-4d00-9000-000000000006",
  "requestId": "a16d2400-8b00-4d00-9000-000000000006",
  "draftId": "d16d2400-8b00-4d00-9000-000000000001",
  "status": "FAILED",
  "statusLabel": "최종저장 실패",
  "baseRevision": 1,
  "finalizedCaseId": null,
  "files": [],
  "caseEndpoint": null,
  "failure": {"reason": "PERSISTENCE_FAILED", "currentRevision": 1, "issues": []},
  "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d16d2400-8b00-4d00-9000-000000000001/finalizations/b16d2400-8b00-4d00-9000-000000000006"
}
```

마지막 두 예시는 동일 식별의 **서로 다른 대안 시나리오**다. FAILED가 나중에 SUCCEEDED로 바뀐다는 뜻이 아니다. UUID·Case/fileNo는 문서용이며 실제 등록/실행 결과가 아니다.

### 9.5 인수 기준·OpenAPI 인계

- 성공은 Case/4역할 fileNo·고정 양식·Draft/작업 상태의 동시 확정이며 revision 불변이다. 하나의 업로드/연결 실패도 부분 성공을 반환하지 않는다.
- 동시 최종저장·CHANGE 충돌, 같은 키/다른 종류 또는 내용, 성공 응답 유실/새 키 재클릭에도 최종 Case1건, 실패 기록 재전송과 새 키 재시도, QUEUED/RUNNING 재시작·timeout/늦은commit·준비파일 정리 시나리오를 제품에서 검증한다.
- 표별 정책 미승인을 공통 계약 미완료로 바꾸지 않는다. 실제 해당 표 쓰기/구조·계산 경로가 미완비면 POLICY_INCOMPLETE/POLICY_UNSUPPORTED로 거부한다. 원본 보존만 하는 표에 새 쓰기 정책을 요구하지 않는다.
- OpenAPI summary는 §3 EDT-06/07, description/Schema는 §9.1~9.3의 목적·부작용·멱등/재클릭·상태별 nullability, 202/200·Location/Retry-After·§6 오류와 §9.4 예시를 구조화한다. 구현·생성 OpenAPI/HTTP/worker 검증 현황과 실제 DB/storage 대기는 [기존 실행 기록](../work-notes/2026/2026-09-03-model-input-transfer-format.md#verification-result)을 참조한다.

## 10. Open Questions·준비도

첫 구현 전 코드/문서 대조의 연결·보완 경계는 [domain §5.4](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md), 코드상 저장 호환은 [DDL §6.4](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md), DBA 확인 항목은 **DDL §13 DBA-01~05**가 소유한다. 현재 구현 범위는 §1.2, 실제 적용·시험 결과와 남은 확인은 [기존 작업 노트의 최신 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 추가 사용자 공통 업무 결정은 없으며 코드/문서 대조만으로 구현·제품 검증 완료를 판단하지 않는다.

공통 저장 구조와 EDT-01~07/최종저장·복구 **문서 계약은 이번 범위 완료**다. 전체 기능/INPUT-03·04 완료·운영 발행/DB 적용·제품 검증 완료를 뜻하지 않는다.

| 분류 | 남은 결정/확인 | 다음 행동 |
| --- | --- | --- |
| 표별 업무 정책 | PriceModel 비계약 물량·Regas 신규 설비 추천은 **미승인·후속**. BCPrices Year CLEAR/행 삭제 등 승인 밖 동작 | 공통 설계의 선행조건에서 제외, 해당 표/동작을 열 때 업무 단위로 결정. 이미 승인한 Sector/Monthly/BCPrices 정책 재질문 없음. 목록/seed 세부 설계 제외 |
| 모델/STRUCT | Sector/Monthly 자유입력 명칭 인식 제약, 실제 runtime table/field/row binding·빈 슬롯/append·삭제/연결·참조/수식·서식/보존 | 해당 행동의 명세/구현 준비에서 확인. mock 성공이나 선정 fixture를 일반 정책 승인으로 확대하지 않음 |
| DBA 적용 준비 | DDL §6.4의 코드상 타입/저장 대조와 §13 DBA-01~05(schema/ID·제약/경합·packet·감사 예외·SQL/rollout) | 확인한 시험 범위와 남은 실제 DB 확인은 [기존 작업 노트](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조. 기본 연동 시험을 DBA 운영 준비 전체 승인으로 대체하지 않음 |
| 제품 구현/검증 | durable 접수/BigDecimal 정밀도·payload 상한/복원·동시성/응답 유실·부분 업로드/DB commit 불명·재시작/정리·전체 시간·OpenAPI | INPUT-07~10 후속. 문서만으로 Pass/배포 ready 판정 없음. 변경 필요 시 계약/FE와 동기화 |
| 보관 후속 | Draft 존속 중 참조·성공 이력/요청/행을 함께 유지하고 자동 만료 없음 | 장기 archive/폐기는 별도 계약 시 결정. 이번 공통 저장 완료의 미결로 되돌리지 않음 |

F-S-06 제외 INPUT-02A는 DONE(CONDITIONAL)이며 규칙63/64·선정19사례 범위를 유지한다. 별도 외부 workbook/DRM 재시험·전체 조사/계산 재실행을 새 선행조건으로 추가하지 않는다.

- 현재 범위: 공통 저장/최종저장·실패 복구 계약 정리 Complete. 전체 문서는 **Draft**다. PriceModel/Regas 추가 추천은 채택하지 않았다.
- implementation readiness: 공통 계약/기존 코드 연결 검토 입력은 `ready after minor cleanup`(실제 DB 대조/적용 준비 별도); 표별 쓰기·최종 파일 제품 활성화는 미완비 정책/STRUCT·구현 검증 전 `blocked by open questions`. 이번 작업은 구현·DB 적용을 수행/승인하지 않는다.
