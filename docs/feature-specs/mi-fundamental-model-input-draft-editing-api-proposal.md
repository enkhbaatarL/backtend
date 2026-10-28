# MI Fundamental Model Draft 공통 편집 API 초안

## Document Control

- status: Draft
- canonical for: API contract — 사용자 채택 범위와 명시적으로 구분한 후속 제안을 포함하는 이 slice의 endpoint/method, 공개 DTO·validation·오류 위치와 문서용 JSON 예시. 기존 QRY-01/02 계약은 변경하지 않는다.
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
- last synced: 2026-10-23 — PMI 연결 행 추가·Base 직접 추가 금지/Selected 내부 표(업무 §6.8) 반영; Demand Parameters/Bands 기존 값 수정 전용 결정 반영(업무 정본 §6.9, v6). 이전 동기화: 2026-10-22 (EDT-05 상태 조회 비잠금/만료 정리 경합 정책 반영; 저장된 빈 ADDED 행 표시 §3.2 반영; 2026-10-20 모든 Draft 표의 마지막 데이터 행 표시 §3.2 반영; LiqProdAdjustment 신규 월 입력·입력 연월 기준 Period 계산·월 건너뛰기/중복 허용·새 열 fieldKey·임시 키/성공 대응 사용자 채택; 열 명령/기존 API 연결·headerValues 형식·LiqProd 월 삭제·같은 저장의 추가 후 삭제·성공 mapping 다섯 필드/상태·빈 배열 통일·삭제 키 미재사용/동일 요청 키 유지 채택; 열 동작 조회 metadata·LiqProd/LiqUtil 빈 본문/0 구분·editing.columnCreation 기존 표현 재사용·LNG Demand 새 연도 국가 입력·PMIInput·Base Case HDD/CDD/Temperature 새 연도 본문 빈칸/0 구분 채택; U1/U2 공통 정책 채택·사용자 필수 미결0건; FE 최종 JSON·구현 차이 §4~7/§10.2 반영; 열 저장 v2 단일 형식·Draft 초기화/배포 SQL은 DDL §6.9)
- open questions: §10. 2026-10-16 공통 BE 채택과 이번 공통 저장·최종저장/복구 계약(§9) 마무리를 반영한다. 표별 정책·DBA 적용 준비/제품 검증은 별도 후속이며 PriceModel/Regas 추천은 미승인이다. 문서 전체는 Draft다.

## 1. 목표·이번 slice

FE가 공통 그리드, 신규 행 입력, 저장 대기/성공/실패, 충돌 후 재조회 흐름을 문서 mock으로 준비할 수 있게 한다. 최초 작성은 문서 계약 범위였으며 현재 첫 구현 범위와 한계는 §1.2를 따른다. DB 적용은 사용자 담당이다. **2026-10-16 공통 BE 추천 묶음을 사용자 채택했다.** 아래 채택 범위와 실제 물리 DDL/표별 전체 정책·구현/검증 완료는 구분한다. 예시 성공은 제품/계산/파일 검증 결과가 아니다.

- 포함: 저장 Case에서 Draft 생성/재개, 성공 Draft 구조/표 조회, 순서 있는 값 변경·행 추가/삭제의 임시저장 접수/결과 조회, 최종저장 접수/결과 조회와 응답 유실·부분 실패/재시작 복구 계약(§9).
- 표별 일반 입력/구조·계산 정책은 후속이다. 미결은 해당 표/동작을 열기 전에 처리하며 공통 저장·최종저장 계약을 마치는 선행조건으로 두지 않는다. PriceModel/Regas 추천은 미승인으로 유지한다.
- 별도 책임: INPUT-03은 논리/저장·행 대응·유일성, INPUT-04는 공개 API, STRUCT는 표별 실제 구조 동작·참조/수식·서식·연결 검증, INPUT-07 이후는 구현, INPUT-10은 API 전체 저장 응답 검증이다. 구조 변경은 이번 release 범위에 유지한다.

- 2026-10-19 신규 업로드 진입 요구는 [입력 제공 명세 §3.3](mi-fundamental-model-input-provision-feature-spec.md#incremental-input-upload-api-20261019)·domain 규칙67을 따른다. 기존 EDT-01/원본 조회가 전제하는 sourceCase·완성4파일을 부분 업로드에 그대로 적용하지 않는다. 첫 정상 파일에서 편집 초안·최종저장 성공 시 Case 생성, 최초 ACTIVE 버전 고정, 확인 후 동일 역할 교체/실패 보존, 필요한 파일이 없는 동작만 제한하는 부분 편집과.xlsm/.xlsx 지원은 확정했다. 파일 직접 업로드는 같은 사용자가 한 번에 Case 하나 작성으로 확정했으며 다른 출처 Draft 정책과 초안 간 상태 격리는 유지한다. 미완료 직접 업로드 작성은 진행 작업이 없을 때 취소 안내/확인 후 새 작성을 허용하며 확인 전·취소 처리 실패 시 기존 상태를 유지한다. A 취소 확정 후 B의 최초 파일을 별도로 업로드하고 B 실패 시 A는 취소 유지/비복구·B 초안 미생성·새 requestId의 최초 재시도로 확정했다(제공 명세 §3.3). 구체 취소 API 표현은 기술 후속이다. 업로드 처리 상한은 파일 수신 후 접수부터 대기열 포함120초를 초기 채택했으며 TIMEOUT/늦은 성공 차단·실제 검증 잔여는 제공 명세 §3.3을 따른다. 기존 편집 worker 자원 구성을 자동 적용하는 결정은 아니다. 업로드 초안 목록/선택 재개는 이번 범위에서 제외했다. 최종저장은 각 역할의 원본 형식(.xlsx→.xlsx,.xlsm→.xlsm)을 유지하며, 업로드 파일당 상한은 제공 명세 §3.3의50MiB로 확정했다. 첫 파일의 초안 생성 API와 기존 초안의 파일 추가·교체 API도 분리 확정했으며 전체 경로와 후속 draftId 경계는 제공 명세 §3.3을 따른다. 최초 생성의 같은 requestId·동일 파일/요청 재전송은 기존 초안·결과를 반환하고 다른 내용은 거절하는 규칙도 해당 절에서 확정했다. 두 업로드는 파일 수신 후 비동기로 검증·파싱하며 성공 뒤에만 초안 생성/파일 연결·교체를 확정한다. 접수와 성공 결과의 구분도 해당 절을 따른다. 업로드/교체·편집 저장·최종저장은 같은 초안에서 한 번에 하나만 진행하며 처리 중 직전 성공 상태와 결과 조회는 허용한다. 다른 초안은 독립적으로 진행한다. 파일 추가·교체도 baseRevision을 필수 제출하며 현재 성공 버전과 불일치하면 거절한다. 성공 시 같은 초안의 revision을1회 증가시키고 실패/동일 성공 결과 재확인 시 유지한다(제공 명세 §3.3). 기존 역할 파일의 교체는 replaceConfirmed:true가 필요하며 생략/false면 거절하고 파일·편집·revision을 보존한다. 구체 확인 필드 계약도 제공 명세 §3.3을 따른다. 업로드 성공 결과는 해당 역할의 첫 표시 표를 자동 선택하고 구조와 값을 함께 제공한다. 기존 저장 Case 조회의 최초 미선택 정책은 유지한다. 두 업로드 요청은 multipart의 file binary와 metadata application/json object로 나누며 필드 위치·최초/후속 필수 조건·JSON 예시는 제공 명세 §3.3을 따른다. 업로드도 접수202·operationId/status/statusEndpoint·공통 상태 GET200/2초 조회를 사용하며 SUCCEEDED/FAILED에서 반복 조회를 종료한다. 업로드 전용 조회 경로와 상세 계약은 제공 명세 §3.3이 소유한다. 업로드 검증 실패의 failure.reason/currentRevision/issues와 파일 역할·시트·표·헤더 위치 형식도 해당 절을 따른다. 기존 편집 명령의 issue 형식을 업로드 전용 위치 schema로 바꾸는 결정은 아니다. 업로드 초안 상태는 서버 기준 uploadedKinds/missingKinds/canFinalize를 제공하고 처리 중 신규 파일 제외·교체 중 이전 성공 파일 유지/최종저장 제한을 적용한다(제공 명세 §3.3). 업로드 FAILED의 동일 요청 재전송은 기존 실패 결과만 반환하고, 새 재시도는 새 requestId·후속 현재 baseRevision/교체 확인을 사용한다. 기존 편집 실패 정책은 그대로 유지한다. 사용자가 파일 교체는 다른 파일의 표에 영향을 주지 않는다고 확인했다. 교체 대상 파일/편집만 변경하고 다른 역할의 표·입력·계산 결과는 유지하며 파일 간 재계산 미결은 제거한다(제공 명세 §3.3·domain 규칙67). 기존.xlsm 고정 구현의 보완/검증이 필요하다. 원본 Case 없는 저장/파일 기준점 복원은 domain §5.6·DDL §6.10을 따르며 초안 식별 응답/편집 API 연결은 §10 Open Questions다. 현재 sourceCase 기반 계약을 암묵적으로 변경하지 않는다.

확인한 local pattern은 `FundamentalModelCaseController`의 prefix/권한, `FundamentalModelCaseServiceImpl.findReadableActiveCase`와 별도 owned-case 수정 경계, record DTO의 `@Schema`, `FundamentalModelCaseCreateRes.statusEndpoint`, 공통 `BaseResponseFactory`/`ResultCode`다. 기존 Case/Run 상태를 Draft 작업 상태로 재사용하지 않는다. 최초 조사 때는 endpoint/DTO가 없었으며 현재 구현 상태는 §1.2를 따른다.

### 1.1 사용자 채택 범위 — 2026-10-16

- 공통 BE 묶음: §3~6의 endpoint/공개 ID·typed 값/오류 위치, 안정 rowId·신규 임시키, 순서 있는 명령·요청 전체 성공/실패, 성공 revision 충돌, requestId 재전송, Draft당 진행 요청1건과 비동기 접수/완료·조회 흐름을 채택했다. 관계형/JSON 책임·상태/성공 경계는 domain §3.4/§5.1이 소유한다.
- 기술 시작값: §2의 2초 폴링과 §4의 body/명령 수 등 초기 제한을 채택 기준으로 사용한다. 실제 제품 적용/정확한 bytes·정밀도/성능 검증은 후속이며 측정 뒤 조정이 필요하면 계약/FE와 동기화한다. 공개 DTO와 DB JSON을 동일 형식으로 강제하지 않는다.
- Sector: 국가/하위부문 필수 자유입력·연월 범위·신규/기존 수정 중복 금지 및 H/J 파생 처리·I 음수 금지는 [domain 규칙45](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)의 사용자 확정 사항이며 공개 검증/오류는 §4.1에서 연결한다.
- 두 업무 묶음: Sector/Monthly 수요 비우기 금지·행 삭제로 해제와 Monthly 입력/중복·P blank/R=Q는 domain 규칙45, BCPrices 기존 연도/가격 수정·입력값 보존은 domain §6.1에서 채택했다. 공개 검증/오류는 §4.1~4.3을 따른다.
- 이번 사용자 요청에 따라 공통 저장/보관·복원은 DDL §6.3, 최종저장 상세는 §9로 마무리한다. 실제 runtime ID/표별 미정·DBA 적용/제품 검증은 §10의 후속이다. 이 채택으로 예시 catalog·mock을 운영 READY로 만들거나 문서 전체를 Approved/INPUT-03·04 전체 완료로 승격하지 않는다.

<a id="liqprod-month-input-20261019"></a>

#### 2026-10-19 추가 채택 — LiqProdAdjustment 신규 월 입력

- [domain 규칙65](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-header-decision-20261019)에 따라 이 표의 신규 월 생성 요청은 사용자가 입력한 연도·월을 전달하며 Period는 입력받지 않고 서버가 생성한다. Period는 같은 규칙의 후속 결정에서 채택한 입력 연월 기준 산식으로 계산한다. FE는 생성 결과를 서버 응답/재조회로 확인한다.
- 이번 채택은 입력 주체·Period 계산·월 건너뛰기/중복 허용을 확정한다. 입력 제한은 domain 규칙65/66에 따르며 이 표의 중복 연월·과거 월·업무 연도 범위를 이유로 추가 차단하지 않는다. 같은 연월/Period라도 각 열을 별도로 유지한다. 새 열의 식별·임시 키/성공 응답 연결은 아래 추가 채택을 따른다. ADD_COLUMN/DELETE_COLUMN의 명령명과 기존 API 연결은 아래 채택을 따른다. headerValues 형식도 아래 채택을 따른다. 요청/응답 JSON은 §4~7.18의 최종 전달 형식을 따른다. 공통 파서 구현과 실제 표별 처리 지원은 §10.2에서 구분한다.

<a id="column-fieldkey-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 새 열의 식별

- 새 열도 기존 `fieldKey` 방식으로 구별한다. 같은 표의 별개 열에는 서로 다른 키를 사용하며, 연월/Period가 같아도 선택한 열의 키로 수정·허용된 삭제 대상을 구별한다. 표시 연월이나 화면 위치를 열 식별자로 대체하지 않는다.
- **2026-10-19 임시 키 연결 후속 채택:** FE는 같은 저장 요청에서 새 열과 그 열의 입력값을 `clientColumnKey`로 연결한다. 저장 성공 응답은 `columnMappings`에서 해당 임시 키와 서버의 실제 `fieldKey` 대응을 제공한다. FE는 성공 후 받은 실제 키를 이후 요청의 열 식별에 사용한다. 새 열 추가와 값 입력을 같은 저장 요청으로 처리하는 연결 방식을 확정한다.
- 성공 mapping의 다섯 필드/상태는 아래 채택을 따른다. 대응 항목 없는 경우의 빈 배열 표현도 아래 채택을 따른다. 삭제 키의 미재사용·동일 요청 재전송의 기존 키 유지는 아래 채택을 따른다. 전체 요청 JSON·오류는 §4~7.18을 따른다. 표별 삭제 허용이나 제품의 열 쓰기 지원을 확정하지 않는다. 공통 DTO의 구현과 표별 실제 지원 상태는 §10.2를 따른다.

<a id="column-operations-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 기존 저장 API의 열 명령

- 기존 EDT-04 `POST /api/v1/mi/fundamental-model/input-drafts/{draftId}/change-requests`의 `operations`에 `ADD_COLUMN`과 `DELETE_COLUMN`을 추가한다. 열 변경도 기존 행/값 변경과 같은 저장 요청으로 보내며 기존 요청 전체 성공/실패·revision 충돌·requestId 재전송·비동기 접수/결과 조회 계약을 적용한다.
- 명령명과 기존 API 연결을 채택한 것이며 표별 추가/삭제 허용 범위를 넓히지 않는다. 열은 승인된 표/동작에만 적용하고 헤더/요청 JSON·metadata·응답/오류는 §3~7.18, 구현 차이는 §10.2를 따른다. 공통 파서는 두 열 명령을 수용하며 실제 처리 지원은 §7.12를 따른다. 문서 결정과 구현/검증 완료를 구분한다.

<a id="column-header-values-decision-20261019"></a>

#### 2026-10-19 추가 채택 — ADD_COLUMN의 헤더값

- `ADD_COLUMN`의 헤더값은 `headerValues` 객체에 묶어 전달한다. LiqProdAdjustment는 `"headerValues":{"year":2032,"month":3}`처럼 사용자가 입력한 Year/Month를 전달하고 Period는 보내지 않는다. LiqUtilAdjustment는 `"headerValues":{}`로 전달하며 Year/Month/Period를 서버가 승인 규칙대로 생성한다. 이 예시는 형식 설명이며 연월 값의 새 제한을 뜻하지 않는다.
- 헤더값과 본문 입력값은 구분한다. 같은 저장의 본문값은 앞서 채택한 임시 키 연결을 따른다. 연도형 헤더·본문 초기값은 아래 U1/U2와 §3.3~4, 요청/응답은 §7.13~18을 따른다. 실제 계산/연결·미완비 삭제의 제품 검증은 후속이다. LiqProd 월 열 삭제 및 두 월 표의 새 월 본문 빈칸 정책은 아래 채택을 따른다. 공통 파서/DTO 구현과 실제 표별 열 처리 미지원은 구분한다(§10.2).

<a id="liqprod-delete-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — LiqProdAdjustment 월 열 삭제

- [domain 규칙65의 월 열 삭제 결정](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-delete-decision-20261019)에 따라 이 표의 월 열 삭제를 허용한다. 기존 채택한 DELETE_COLUMN/fieldKey 방식으로 선택한 열을 구별한다. 헤더/본문 삭제·B:F 보호 등 업무 범위는 해당 정본을 따른다.
- 이 표의 삭제 허용은 더 이상 미결이 아니다. 수식·연결/계산·보존 영향과 실제 파일 결과 검증, 삭제 요청/응답 세부는 잔여다. 실제 표별 열 처리 미지원과 표별 준비도를 유지하며 삭제 정책 채택을 구현/검증 완료로 표시하지 않는다.

<a id="column-add-delete-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 같은 저장의 열 추가 후 삭제

- [domain §5.1](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#column-add-delete-decision-20261019)에 따라 열 삭제가 허용된 표에서는 같은 operations의 ADD_COLUMN 뒤에 해당 열의 DELETE_COLUMN을 보낼 수 있다. DELETE_COLUMN은 앞선 추가 명령의 `clientColumnKey`로 그 새 열을 참조한다.
- 성공한 최종 표에는 해당 열이 남지 않으며 성공 응답의 `columnMappings`에 발급된 `fieldKey`와 `columnState:DELETED`를 남긴다. 성공 항목의 필드/상태 형식은 아래 추가 채택을 따른다. 삭제 키 미재사용·동일 요청 재전송의 기존 키 유지도 아래 후속 채택을 따른다. 실패/진행 중 mapping은 아래 빈 배열 채택을 따른다. 실제 표별 열 처리 지원을 뜻하지 않는다(§10.2).

<a id="column-mapping-item-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 성공 columnMappings 항목

성공 응답의 신규 열 대응 항목은 다음 다섯 필드를 포함한다.

| 필드 | 의미 |
| --- | --- |
| `commandId` | 해당 열을 추가한 ADD_COLUMN 명령의 ID |
| `clientColumnKey` | FE가 추가 명령에 붙인 임시 키 |
| `tableId` | 대상 표의 식별자 |
| `fieldKey` | 서버가 발급한 실제 열 키. 같은 저장에서 삭제한 새 열도 발급 키를 남김 |
| `columnState` | 해당 요청의 최종 성공 결과에 열이 남으면 `ACTIVE`, 같은 저장에서 추가 후 삭제했으면 `DELETED` |

삭제된 새 열도 fieldKey와 DELETED 대응을 성공 응답에 남기며, 이 키가 최종 표의 활성 열로 존재한다는 뜻은 아니다. 항목이 없는 경우의 표현은 아래 빈 배열 채택을 따른다. 삭제 키 미재사용과 재전송은 아래 후속 채택을 따른다. OperationView에는 해당 공통 필드가 구현됐으며 실제 열 생성 지원과는 구분한다(§10.2).

<a id="column-empty-mappings-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 대응 항목이 없는 columnMappings

- `OperationView`의 `columnMappings`는 배열로 제공하며 생략하거나 null로 반환하지 않는다. `QUEUED/RUNNING`, 접수 후 실패한 `FAILED`, 새 열 추가 없이 `SUCCEEDED`한 경우에는 `[]`로 통일한다. 기존 열 삭제만 성공한 경우도 새 열 추가가 없으므로 `[]`다.
- 같은 요청에서 새 열을 추가한 뒤 삭제한 성공은 새 열 추가가 있었던 경우다. 이때는 앞서 채택한 실제 fieldKey/DELETED 항목을 유지하며 최종 표에 열이 없다는 이유로 빈 배열로 바꾸지 않는다.
- 이 계약은 작업 상태를 담는 OperationView에 적용한다. 접수 전 오류의 기존 Problem envelope나 data:null 계약은 변경하지 않는다. 공통 DTO/저장 결과의 구현 상태는 §10.2를 따른다.

<a id="column-key-lifecycle-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 삭제 fieldKey 미재사용과 재전송

- [domain §5.1 열 식별 결정](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#column-key-lifecycle-decision-20261019)에 따라 삭제된 fieldKey는 재사용하지 않는다. 같은 연월을 다시 ADD_COLUMN으로 추가하면 새 fieldKey를 발급하고 성공 columnMappings에 새 대응을 제공한다. 같은 저장에서 추가 후 삭제한 키도 재사용하지 않는다.
- 동일 requestId·동일 내용의 재전송은 기존 §5 계약대로 같은 operation/결과와 fieldKey를 유지하며 새 키를 발급하지 않는다. 기존 §5의 같은 requestId·다른 내용 충돌 정책도 유지한다. 내부 키 생성 수단은 구현 선택이며 이 공개 정책의 미결로 남기지 않는다. 실제 키/이력 복원 구현·검증은 별도다.

<a id="column-editing-metadata-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 열 동작의 조회 metadata

- EDT-03의 `editing.capabilities`에 `ADD_COLUMN`/`DELETE_COLUMN`, `editing.fields[].policyStatus`에 `DELETE_COLUMN`을 추가한다. 값은 기존 `READY/INCOMPLETE/NOT_ALLOWED/UNSUPPORTED`를 재사용한다.
- FE는 열 추가 버튼을 표의 `capabilities.ADD_COLUMN`이 READY일 때 활성화한다. 열 삭제 버튼은 표의 `capabilities.DELETE_COLUMN`과 선택한 열의 `policyStatus.DELETE_COLUMN`이 모두 READY일 때 활성화한다. 나머지 상태는 비활성화하며 미완비·업무 불허·제품 미지원을 기존 의미대로 구분한다. 서버도 같은 표/열 정책을 검증한다.
- 이 형식은 확정한 업무 허용과 구조/계산/보존 정책의 준비도를 전달하며 SUMMARY 문구만으로 버튼을 활성화하지 않는다. 기존 정책을 확장하거나 새 입력값 제한을 추가하는 결정은 아니다. 생성할 새 열의 안내는 아래 editing.columnCreation 채택을 따른다. 공통 metadata/parser의 구현만으로 실제 열 처리를 지원하지 않는다(§10.2).

<a id="column-creation-metadata-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 새 열 생성 안내 `editing.columnCreation`

- EDT-03의 `editing.columnCreation`에 새 열의 사용자 입력 헤더 정의와 본문 생성 정의를 제공한다. 구체 구조와 제공 조건은 §3.3을 따른다. 기존 `dataType` 및 `create`의 `required/initial/constraints` 표현을 재사용하며, 별도 타입·초기값 문법을 만들지 않는다.
- LiqProdAdjustment는 Year/Month 입력을 안내하고 Period는 서버 자동 생성하므로 입력 항목에서 제외한다. LiqUtilAdjustment는 세 헤더 모두 서버 자동 생성하므로 사용자 입력 헤더는 빈 객체다. 두 표의 본문은 확정한 `initial:{kind:"EMPTY"}`·미입력 빈칸 유지·명시적 숫자0 구분을 전달한다.
- 이 결정은 확정된 표별 정책의 조회 표현이다. 새로운 입력 제한/필수값이나 추가 승인 조건을 만들지 않으며, 원본·기존 계약으로 확인되는 내용은 재질문하지 않는다. 다른 표의 미확정 업무 초기값을 추정하지 않는다. 공통 DTO/저장 기반과 실제 표별 열 생성 지원은 §10.2에서 구분한다.

<a id="liqprod-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — LiqProd 새 월 본문의 빈칸 기본값

- [domain 규칙65의 본문 초기값](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-empty-body-decision-20261019)에 따라 이 표의 새 월 본문은 빈칸으로 생성한다. 같은 저장에서 해당 칸의 SET을 생략하면 빈칸으로 남기며 미입력 자체를 필수값 누락으로 거부하거나0으로 보정하지 않는다.
- 사용자가 숫자0을 입력하면 `clientColumnKey`로 새 열에 연결한 SET의 `value:0`으로 전달하고 숫자값으로 보존한다. 빈칸은 SET 생략으로 표현하며 null/빈 문자열을 숫자 입력으로 허용하지 않는 기존 typed 계약은 유지한다. 다른 표의 초기값은 각 표의 정본을 따른다. CLEAR는 기존 §4의 값 비우기 명령이며 아래 요구사항 범위 정리를 따른다.
- 새 열의 타입·초기값 안내 metadata와 공통 저장 기반은 구현됐으며 실제 표별 생성·계산/연결 검증은 잔여다. 실제 표별 열 처리 지원을 뜻하지 않는다(§10.2).

<a id="liqutil-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — LiqUtil 새 월 본문의 빈칸 기본값

- [domain 규칙46의 본문 초기값](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqutil-empty-body-decision-20261019)에 따라 새 월 본문은 빈칸으로 생성하며 해당 SET을 생략한 칸도 빈칸으로 유지한다. 숫자0은 명시적 `value:0` 입력으로 구분한다. 기존 월의 숫자값을 변환하지 않는다.
- 제출/typed 값 표현은 위 LiqProd 본문 정책과 동일하게 SET 생략과 명시적 숫자 입력을 구분하고 null/빈 문자열을 숫자로 허용하지 않는다. 이 표의 Year/Month/Period 자동 생성은 기존 채택을 유지한다. 새 열 생성 안내는 §1.1/§3.3의 editing.columnCreation을 따른다. 실제 생성/저장·계산/연결 검증은 별도다.
- CLEAR의 요청/빈 값 표현은 기존 §4, 표/필드 준비도는 §3.3을 따른다. domain 규칙66에 따라 근거 없는 추가 금지나 별도 사용자 승인 조건을 만들지 않는다. 본문 초기값 결정으로 기존 숫자값을 자동으로 지우지 않는다. 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md)를 참조한다.

<a id="lng-demand-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — LNG Demand 새 연도 국가 입력의 빈칸 기본값

- [domain 규칙50의 본문 초기값](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#lng-demand-empty-body-decision-20261019)에 따라 새 연도 국가별 입력90칸을 빈칸으로 생성하고, 같은 저장에서 해당 SET을 생략하면 빈칸으로 유지한다. 숫자0은 명시적 `value:0`으로 구분한다. 기존 typed 값 계약에 따라 숫자 입력에 null/빈 문자열을 사용하지 않는다.
- `editing.columnCreation.body`는 기존 표현인 `initial:{kind:"EMPTY"}`로 시작값을 안내하고, 미입력 유지가 허용되는 이 신규 국가 입력의 `create.required`는 false다. 이 정의는 사용자 입력칸에 적용하며 합계16셀은 기존 승인 자동 계산 규칙을 따른다. 기존 E:N 값/수식 보존과 기존 E6의 별도 정책은 유지한다.
- 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md#lng-demand-empty-body-evidence-20261019)를 참조한다. 다른 표의 초기값/일반 CLEAR 정책을 확대하지 않으며 실제 생성·저장·계산 검증은 별도다.

<a id="pmi-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — PMIInput 새 연도 본문의 빈칸 기본값

- [domain §6.2](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#pmi-empty-body-decision-20261019)에 따라 `PMIInput / Base Case PMI Input`의 신규 연도 사용자 입력 본문은 빈칸으로 생성한다. 같은 저장에서 해당 SET을 생략한 칸은 빈칸으로 유지하고, 숫자0은 명시적 `value:0`으로 구분한다. 숫자 입력의 null/빈 문자열은 기존 typed 값 계약을 따른다.
- `editing.columnCreation.body`는 기존 `initial:{kind:"EMPTY"}`와 `create.required:false`로 표현한다. 기존 PMI 숫자/빈칸은 보존한다. 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md#pmi-empty-body-evidence-20261019)를 참조하며, 다른 표의 초기값/CLEAR 정책이나 실제 생성·저장·계산 검증 완료로 확대하지 않는다.

<a id="hdd-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — Base Case HDD 새 연도 본문의 빈칸 기본값

- [domain §6.3](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#hdd-empty-body-decision-20261019)에 따라 `WeatherInput / Base Case HDD Input`의 신규 연도 사용자 입력 본문은 빈칸으로 생성한다. 같은 저장에서 해당 SET을 생략한 칸은 빈칸으로 유지하고, 숫자0은 명시적 `value:0`으로 구분한다. 숫자 입력의 null/빈 문자열은 기존 typed 값 계약을 따른다.
- `editing.columnCreation.body`는 기존 `initial:{kind:"EMPTY"}`와 `create.required:false`로 표현한다. 기존 HDD 숫자/빈칸·Helper는 보존한다. 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md#hdd-empty-body-evidence-20261019)를 참조하며, 다른 표/CLEAR 정책이나 실제 생성·저장·계산 검증 완료로 확대하지 않는다.

<a id="cdd-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — Base Case CDD 새 연도 본문의 빈칸 기본값

- [domain §6.4](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#cdd-empty-body-decision-20261019)에 따라 `WeatherInput / Base Case CDD Input`의 신규 연도 사용자 입력 본문은 빈칸으로 생성한다. 같은 저장에서 해당 SET을 생략한 칸은 빈칸으로 유지하고, 숫자0은 명시적 `value:0`으로 구분한다. 숫자 입력의 null/빈 문자열은 기존 typed 값 계약을 따른다.
- `editing.columnCreation.body`는 기존 `initial:{kind:"EMPTY"}`와 `create.required:false`로 표현한다. 기존 CDD 숫자/빈칸·Helper는 보존한다. 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md#cdd-empty-body-evidence-20261019)를 참조하며, 다른 표/CLEAR 정책이나 실제 생성·저장·계산 검증 완료로 확대하지 않는다.

<a id="temperature-empty-body-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — Base Case Temperature 새 연도 본문의 빈칸 기본값

- [domain §6.5](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#temperature-empty-body-decision-20261019)에 따라 `WeatherInput / Base Case Temperature`의 신규 연도 사용자 입력 본문은 빈칸으로 생성한다. 같은 저장에서 해당 SET을 생략한 칸은 빈칸으로 유지하고, 숫자0은 명시적 `value:0`으로 구분한다. 숫자 입력의 null/빈 문자열은 기존 typed 값 계약을 따른다.
- `editing.columnCreation.body`는 기존 `initial:{kind:"EMPTY"}`와 `create.required:false`로 표현한다. 기존 Temperature 숫자/빈칸·Helper는 보존한다. 원본 근거는 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md#temperature-empty-body-evidence-20261019)를 참조하며, 다른 표/CLEAR 정책이나 실제 생성·저장·계산 검증 완료로 확대하지 않는다.

<a id="common-new-input-default-api-decision-20261019"></a>

#### 2026-10-19 추가 채택 — 신규 입력 공통 시작값·새 연도 직접 입력

- 업무 정책과 기존 기본값·필수값·자동 계산 우선순위는 [domain §6.6 U1/U2](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#common-new-input-default-decision-20261019)가 소유한다. 그 U1 적용 대상은 기존 표현인 `initial:{kind:"EMPTY"}`·`create.required:false`로 안내하고, SET 생략은 신규 칸의 빈칸 유지·명시적 `value:0`은 숫자0 입력으로 구분한다. 명시된 필수값/기본값이 있는 필드나 서버 생성/수식 칸을 이 정의로 덮어쓰지 않는다. 신규 행은 `fields[].create`, 신규 열 본문은 `editing.columnCreation.body`에서 같은 표현을 재사용한다. 숫자 입력의 null/빈 문자열과 기존 SET 생략 시 값 보존 계약은 유지한다.
- [domain U2의7표](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#annual-header-manual-decision-20261019)는 `ADD_COLUMN.headerValues.year`에 사용자가 입력한 연도 정수를 전달한다(예: `"headerValues":{"year":2036}`). `editing.columnCreation.headerValues.year`는 기존 INTEGER/create 표현으로 해당 입력을 안내하며 아직 없는 fieldKey나 좌표로 대체하지 않는다. 기존 연도 직접 입력이 확정된 LNG Demand도 이 표현을 재사용한다. 기존 월 헤더 입력/자동 생성 계약은 유지한다.
- 이 형식은 원본/확정 정책에 없는 연속 연도·중복 금지·업무 연도 범위를 추가하지 않는다. 이미 명시된 표별 검증과 규칙66은 유지한다. 두 정책 채택으로 runtime DTO/파서·열 명령 처리나 기술 준비도가 자동 변경되는 것은 아니다.

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

모든 operation은 기존 USER/SKGAS_COMPANY_USER_ONLY, ADMIN/THIRD_PARTY_ADMIN_ONLY 권한 패턴과 인증된 tenant/user를 사용한다. source는 domain §5.4의 한 회사 기능 범위 안에서 접근 가능한 active `INDEPENDENT/MI_BASE`이며 업로더 본인일 필요는 없다. 2026-10-23 사용자 확정에 따라 QRY-01 및 source 기반 Draft 생성·재개·후속 조회는 Case 생성자의 회사번호와 요청 회사번호를 비교하지 않는다. 타사 접근 ADMIN의 소속 회사가 달라도 기존 권한 조건을 통과하면 같은 공유 원본을 사용한다. Draft는 **생성 사용자 개인 소유**이므로 source Case 수정 소유권과 구별한다. 다른 소유자/tenant의 Draft·작업은 404, 관리자도 이 API로 다른 사람 Draft에 접근하지 않는 안이다. 각 요청에서 source 접근 가능성도 확인한다. source 상태가 COMPLETED여야 한다는 새 조건이나 source Case 수정 권한은 추가하지 않는다.

worker는 접수 당시 서버가 확인한 actor/tenant/고정 기준을 전달받고 종료 때 context를 정리한다. 사용자/tenant/fileNo/bucket/object key/수식/저장 경로는 공개 요청으로 받지 않는다. payload·셀값·거부값·원문 예외는 HTTP/worker 로그와 오류에 남기지 않는다. 평문 작업사본/기존 암복호화는 domain 규칙63, 자원·비동기·전체120초 상한은 규칙64를 그대로 따른다. QRY JSON DRM 비적용의 기존 결정을 재심의하지 않는다.

입력 양식 선택은 [조회 API §4.6](mi-fundamental-model-input-query-api-proposal.md)의 Case 연결 기준을 따른다. application model-code 설정으로 제한하지 않으며, 기존 Draft의 고정 양식·원본과 현재 Case 연결이 다르면 기존 충돌 오류를 유지한다.

## 3. 채택한 공통 Endpoint

<a id="shared-upload-draft-editing-20261019"></a>

**직접 업로드 초안도 같은 편집 API 사용 — 사용자 확정:** 첫 정상 파일에서 받은 draftId로 아래 EDT-02~05를 그대로 사용한다. 값 변경·행 추가/삭제·열 추가/삭제는 같은 EDT-04 operations와 조회 metadata·EDT-05 결과/오류·revision/requestId·원자성 규칙을 따른다. 업로드 전용 `/input-upload-drafts/...` 행·열 편집 API를 추가하지 않는다. 생성 진입만 입력 제공 명세 §3.3의 최초/후속 업로드 API이며 기존 Case ID나4개 파일이 없어도 초안 생성/가능한 편집을 허용한다. 기존 EDT-01의 Case 기반 생성 조건은 유지한다. 필요한 파일이 없는 동작만 제한하며 같은 고정 양식의 승인 정책을 출처별로 다르게 만들지 않는다. EDT-06/07도 공통 경로를 사용하되 최종저장에는4역할이 모두 필요하다. 실제 표별 지원 여부는 기존 정책 준비도와 §10.2의 검증 범위를 따른다.

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

EDT-05는 소유권·원본 Case 접근을 확인한 뒤 마지막 DB 확정 상태를 비잠금 조회한다. 만료 전 또는 종료된 요청을 조회할 때 저장 transaction의 행 잠금을 기다리지 않는다. 만료된 진행 요청은 기존 잠금 순서로 대기 없는 잠금 획득을 시도해 TIMEOUT을 확정한다. 다른 transaction이 해당 잠금을 보유 중이면 현재 확정 상태를 반환하고 이후 조회/서버 처리에서 다시 확인한다. 잠금 경합이나 FE 시간 경과만으로 성공/실패를 만들어 반환하지 않으며, 만료 뒤 늦은 성공 차단·기존 원자성 규칙은 유지한다.

공개 ID의 UUID는 소문자 하이픈 포함 36자, revision은 0 이상 정수(최대 9,007,199,254,740,991)로 채택했다. Case 기반 `sourceVersion`은 기존 `^sv1_[0-9a-f]{64}$`/68자다. 원본 Case가 없는 직접 업로드의 nullable 응답은 §3.2를 따른다. `tableId`는 기존 구조의 ID 그대로이며 `/`·공백은 query에서 URL 인코딩한다. 조사 T-083 등은 runtime tableId가 아니다.

### 3.1 생성/재개·고정 기준

EDT-01 body는 `{ "sourceVersion": "QRY-01에서 받은 값" }`만 받는다. 현재 source의 네 파일/발행 묶음과 대조해 같은 기준을 고정한다. 동시 생성도 동일 tenant/사용자/source Case당 진행 Draft 한 건만 반환한다. 기존 Draft를 재개할 때 revision을 초기화하지 않고 진행 작업도 반환한다. 기존 진행 Draft의 고정 기준과 요청/현재 source가 다르면 409 `DRAFT_SOURCE_CONFLICT`이며 새 Draft 생성·기존 Draft 재기준화로 우회하지 않는다. 생성/재개는 자동 임시저장이나 화면 미저장값 복구가 아니다.

Case 기반의 공개 `sourceKey/sourceVersion`과 `sections[].inputId`는 QRY의 **원본/발행 기준**을 그대로 사용한다. Draft 저장마다 이 값을 바꾸거나 해시에 Draft/사용자/revision을 넣지 않는다. Draft 결과는 `draftId + revision`으로 구별한다. 현재 Case 참조가 고정 기준과 달라지면 새 쓰기/표 조회는 409 `SOURCE_CHANGED`; 성공 Draft는 보존한다. 작업 조회는 기존 처리 기록을 읽어 결과 확인을 허용한다. source 접근권한 상실/삭제는 404다. 직접 업로드는 원본 Case 참조 대신 소유권·고정 양식·성공 파일 기준점을 검사하며, Case가 없다는 이유로 공통 조회/편집을 차단하지 않는다.

### 3.2 `DraftSummary`와 조회 DTO

아래 필드는 별도 표시가 없으면 항상 required/non-null이다.

| 필드 | 타입·의미 |
| --- | --- |
| `draftId`, `sourceCaseId` | draftId는 UUID string. sourceCaseId는 Case 기반에서 양의 int64, 직접 업로드에서는 명시적 null. 가짜 Case ID/0을 반환하지 않음 |
| `sourceKey`, `sourceVersion` | sourceKey는 서버가 제공한 해당 출처 키(직접 업로드는 EXCEL_UPLOAD 출처). sourceVersion은 Case 기반 QRY 식별자, 원본 Case 없는 직접 업로드에서는 명시적 null. 임의 버전을 만들지 않으며 성공 상태는 draftId/revision으로 식별 |
| `status`, `statusLabel` | lifecycle `DRAFT/FINALIZED/DISCARDED`와 `편집 중/최종저장 완료/폐기`. 정의는 domain §5/§5.1 |
| `revision` | 현재 성공 revision. 생성 직후 0, 실패/접수 때 증가하지 않음 |
| `activeOperationId` | UUID 또는 명시적 null. CHANGE/FINALIZE/후속 파일 업로드 중 진행 요청이 없으면 null. 최초 업로드는 초안 생성 전이므로 이 조회의 대상이 아님 |
| `activeOperationEndpoint` | 서버 생성 상대 API path 또는 명시적 null. 진행 CHANGE는 EDT-05, FINALIZE는 EDT-07, 후속 업로드는 입력 제공 명세 §3.3의 공통 업로드 상태 조회. activeOperationId와 함께 존재/함께 null |
| `finalizedCaseId` | int64 또는 명시적 null. FINALIZED에만 최종 Case ID. 임시저장에서는 null |
| `draftEndpoint` | EDT-02의 상대 API path. 서버가 생성 |
| `created` | EDT-01에만 boolean 추가. true 신규/false 재개; EDT-02에는 생략 |
| `sections`, `tableData` | EDT-02에만 추가. QRY-01과 같은 역할·표 metadata 형식, 성공 revision의 현재 범위. Case 기반은 네 역할, 직접 업로드는 검증/연결 성공한 역할만 포함하며 미업로드/처리 중 파일의 표를 채워 넣지 않음. `tableData`는 항상 null |

EDT-03의 data는 `draftId/revision/sourceKey/sourceVersion/tableData/page/editing`이다. `sections/inputId/created`는 보내지 않는다. `revision` query는 EDT-02에서 받은 현재 성공 번호여야 하며 과거 revision 조회 API는 이번 slice에 없다. 읽는 중 성공 번호가 바뀌어도 409 `REVISION_CONFLICT`; 다른 revision 값을 섞은 200은 금지한다. 처리 중에는 직전 성공 revision을 조회하며 미완성 변경을 섞지 않는다.

직접 업로드의 EDT-03도 sourceVersion은 명시적 null이며 같은 표/값/편집 metadata 형식을 사용한다. 업로드 파일 준비 상태 uploadedKinds/missingKinds/canFinalize는 입력 제공 명세 §3.3의 서버 기준을 재사용하며 EDT-02의 같은 성공 snapshot에 포함한다. 이 차이는 Case 없는 부분 초안을 표현하기 위한 것으로, 행·열 편집 요청/성공 결과를 출처별로 분리하지 않는다.

- `tableData`의 `columns/rows/inputCells/inputGuideRules/controls/displayUpdateRules`와 값 표시·빈 셀 의미는 QRY 상세 §5를 재사용한다. `rows[].id=rowNumber-1`은 유지한다. Draft에서는 행/좌표가 해당 성공 revision의 현재 위치다.
- `page` 필드·본문 최대2,000행·2 MiB 상한/분할/422는 QRY 상세 §5.1을 적용하되 **편집 metadata까지 포함한 전체 응답 bytes**로 계산한다. 모든 페이지는 같은 Draft revision이어야 한다.
- 빈 슬롯/헤더를 임의로 기존 논리 행으로 만들지 않는다. 실제 행과 빈 슬롯의 대응은 `editing.rowBindings`를 사용한다. `inputCells`나 좌표만 보고 쓰기 대상을 만들지 않는다.

#### 모든 Draft 표의 마지막 표시 행 — 2026-10-20 끝 빈 슬롯 제외, 2026-10-21 빈 ADDED 행 보존

모든 역할(DEMAND/SUPPLY/TRADE/OUTPUT)의 Draft EDT-03 및 업로드 preview selection은 **현재 성공 revision의 마지막 표시 값 또는 활성 ADDED 행까지** 본문을 반환한다. GDP에 적용한 끝 빈 행 제외를 다른 표에도 동일하게 확장하되, 사용자가 저장한 빈 추가 행은 다시 표시한다. Draft에서는 QRY의 끝 빈 슬롯 보존 규칙 대신 본 절을 적용하며, 원본 QRY 조회 정책은 변경하지 않는다.

- 표마다 발행된 표시 열과 본문 구간(노란 입력 영역 및 함께 표시하도록 확정된 읽기 전용 값)을 사용한다. 실행 중 셀 색으로 표시/편집 권한을 재추정하지 않는다. 표별 헤더/기간 행과 본문 시작, 원본 좌표를 유지하며 마지막 표시 행 이전의 빈 행과 데이터 행 내부의 빈 셀은 유지한다. 떨어진 본문 구간은 기존 구간 순서를 유지한다.
- 표시 열의 실제 값이 null 또는 빈 문자열인 셀은 끝 행을 늘리지 않는다. 숫자 0, false, 공백만 있는 문자열, 저장된 오류 표시는 값으로 취급한다. 표시 대상 밖 계산 보조 열의 수식은 끝 행 판단에 사용하지 않는다.
- 성공한 SET/CLEAR/행 변경 이력을 적용한 값을 기준으로 판단한다. 성공 저장되어 현재 활성인 `ADDED` 행은 표시 값이 전부 비어도 끝 행 판단에 포함한다. 중간 빈 슬롯도 기존대로 표시한다. 끝의 미활성 `EMPTY_SLOT`과 값 없는 `SOURCE` 행은 논리 행 ID만으로 끝 행을 늘리지 않는다. 삭제된 `ADDED` 행도 표시하지 않는다.
- `page.totalBodyRows`는 이 표시 범위의 본문 행 수다. `rows`, `inputCells`, `editing.rowBindings/editValues`, `returnedBodyRows/endRow/hasNext/nextStartRow`도 같은 표시 범위/페이지에 맞춘다. 페이지당 행/bytes 상한은 유지한다.
- 표시 값도 활성 `ADDED` 행도 없으면 헤더/열/연결 control은 유지하고 본문 수는 0, 시작/끝/다음 행은 null, hasNext=false다. 본문 없는 control 전용 표도 기존 표현을 유지한다. 마지막 표시 값 또는 활성 `ADDED` 행을 넘는 명시적 startRow는 400이다.
- 끝 행 판단에 필요한 표시 대상 셀의 판독 실패는 500이며 오류 행을 빈 행처럼 잘라내지 않는다.
- 원본 Excel, catalog의 전체 입력 범위, 저장된 행 ID 및 ADD_ROW의 자리 배정은 바꾸지 않는다. 이 변경은 편집 capability를 READY로 바꾸지 않는다.

### 3.3 `editing` — Draft 표 조회에만 붙이는 metadata

**2026-10-23 Pipeline 표시 slice:** domain 규칙62 보완에 따라 v6의 읽기 표는 7행 제목/13행부터 본문을 제공한다(조회 API §1.1). 8..12행에는 rowBindings/rowId/inputCells를 제공하지 않고 H를 포함한 사용자 변경을 받지 않는다. 수정 scopes는 비워 두며 일반 SET/CLEAR·행/열 구조 정책은 INCOMPLETE를 유지한다. 미완비 정책의 요청은 기존409 POLICY_INCOMPLETE로 거부하고 operation/revision을 만들지 않는다. 원본 좌표/파일 보존 및 F-S-06의 별도 확정 계산 범위는 그대로이며 이번 표시 변경이 일반 편집/행 추가 READY를 의미하지 않는다.

**2026-10-26 기본 행 삭제(v8):** 업무 정본 §6.8A에 따라 행 추가 가능 표의 `DELETE_ROW`도 `READY`로 제공한다. 기존 rowId 및 앞선 ADD_ROW의 clientRowKey를 지원하며 추가 후 같은 요청에서 삭제한 행은 rowMappings의 DELETED로 반환한다. 원본/추가 데이터 행 모두 대상이며 헤더는 대상이 아니다. PMI User-Adjusted 삭제 성공의 affectedTableIds에는 Base/Selected도 포함한다. PNG 원본 행·내부 표 직접 삭제와 고정 구조 예외는 유지한다. 과거 발행에 고정된 Draft는 기존 capabilities를 유지하므로 FE는 ADD_ROW에서 삭제 허용을 추론하지 않고 `editing.capabilities.DELETE_ROW`가 READY인지 확인한다. 등록·발행 및 기존 참조 보존 절차는 [v8 적용 가이드](../model-input-row-delete-v8-deployment-guide.md)를 따른다.

**2026-10-23 Demand 고정 구조:** 업무 정본 §6.9의 Parameters/Bands는 신규 v6에서 기존 입력의 SET/CLEAR만 READY다. `ADD_ROW/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN`과 필드 `CREATE/DELETE_COLUMN`은 `NOT_ALLOWED`로 표시하고 구조 변경 요청은422 `ACTION_NOT_ALLOWED`로 거부한다. 카탈로그의 `rowAdd/columnAdd=false`에 따라 표 목록의 `rowPolicy/columnPolicy`는 모두 `"고정"`이며 FE는 구조 변경 버튼을 제공하지 않는다. I Upper Bound의 `DECIMAL_OR_TEXT`와 기존 SET/CLEAR는 유지한다. 같은 시트 Premium Market Countries 및 과거 양식에는 이 정책을 소급 적용하지 않는다.

**2026-10-23 Midstream 기존행 편집:** TRADE `MidstreamContractInput`의 PNG `A5:D1104` 및 LNG `Q5:U2918`은 신규 v6 정의에서 기존 rowId의 비어 있지 않은 `SET`만 제공한다. 필드 key/타입과 원본 행 식별은 기존 catalog를 유지한다. 입력 외 전체 보존은 업무 정본 규칙59를 따른다. `CLEAR`는 `INCOMPLETE`로 계속 닫고, 빈 문자열/공백 `SET`도409 `POLICY_INCOMPLETE`로 거부한다. 이는 모든 필드를 필수로 확정한 것이 아니며 U5의 확정 빈 값 금지/숫자0와 다른 행의 미결을 구분한다. `ADD_ROW`/삭제/열 변경은 미개방이다. 과거 지정 F-T-04의704행 생성은 별도 잔여 정책/구현 범위이며 기존행 SET으로 빈 슬롯을 활성화하지 않는다. 기존 비동기/revision/재전송 계약을 그대로 사용하며 접수만으로 성공으로 보지 않는다.

| 필드 | 타입·의미 |
| --- | --- |
| `capabilities` | `SET/CLEAR/ADD_ROW/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN` 각각 `READY/INCOMPLETE/NOT_ALLOWED/UNSUPPORTED`. 두 열 동작은 §1.1 채택·공통 DTO/파서 구현. 실제 처리기 미지원은 UNSUPPORTED로 노출. READY만 호출 가능. 미정과 업무 불허·제품 미지원 구분 |
| `fields` | 안정 fieldKey별 정의 목록. `fieldKey`, `columnField`(기존 columnN), `dataType`(INTEGER/DECIMAL/TEXT/DECIMAL_OR_TEXT), `policyStatus` 필수. DECIMAL_OR_TEXT는 같은 필드의 숫자와 문자열을 각각 JSON number/string으로 유지한다(예: Demand Adjustment Bands Upper Bound의 0.2와 "above"). 숫자 문자열을 숫자로 바꾸거나 숫자를 문자열로 바꾸지 않는다. null/boolean/객체/배열 SET은 거부하며 실제 빈 셀은 CLEAR로 구분한다. 이 혼합 타입은 기존 필드 정의에만 적용하며 ADD_COLUMN의 헤더/본문 타입은 기존 세 타입을 유지한다. policyStatus는 CREATE/SET/CLEAR/DELETE_COLUMN 각각 §3.3 capabilities와 같은 네 상태를 갖는 object. DELETE_COLUMN은 공통 정의 파서/응답에 포함되며 기존 정의에 누락되면 INCOMPLETE. CREATE READY에만 `create` 정의 제공. SET READY에는 setConstraints 필수이며 신규/기존 제약을 혼용하지 않음. CLEAR는 해당 상태로 허용 여부를 표현. 미완비 필수값을 false/null default로 추정하지 않음 |
| `fields[].create` | policyStatus.CREATE가 READY일 때 `required` boolean, `initial`, `constraints` 필수. initial은 `{kind:"EMPTY"}` 또는 `{kind:"VALUE",value:scalar}`. EMPTY는 화면 시작값이며 서버의 누락값 허용이 아님 |
| `fields[].create.constraints` / `fields[].setConstraints` | 신규 생성/기존 SET 각각 READY 정책의 typed 제약 object. INTEGER의 minimum/maximum, DECIMAL의 minimum, TEXT의 options 등 해당 제약만 제공. 기존 TEXT의 `setConstraints.required:true`는 빈 문자열/공백만 있는 SET을 거부하며, 행 생성 정책 없이도 기존 필드의 필수를 표현한다. required가 있으면 boolean이어야 한다. `{}`는 제약 없음이 확인된 경우만 허용. 임의 실행식은 제공하지 않음 |
| `columnCreation` | ADD_COLUMN이 READY일 때 제공하는 새 열 생성 안내. `headerValues`와 `body`를 갖는 object이며, 다른 상태에서는 기존 create와 같이 생략한다. 기존 `fields[].create`는 기존 열의 행 생성 정의로 유지 |
| `columnCreation.headerValues` | ADD_COLUMN.headerValues에 사용자가 제출할 키별 정의 object. 각 정의는 기존 `dataType`과 `create` 표현을 재사용한다. 서버 자동 생성 항목은 입력 정의에서 제외. LiqProd는 year/month, LiqUtil은 `{}`. §1.1 U2 대상7표와 기존 LNG Demand는 year. 기존 INTEGER/create 표현 재사용. 화면 좌표/아직 없는 fieldKey를 키로 쓰지 않음 |
| `columnCreation.body` | 생성할 열의 사용자 입력 본문의 `dataType`·`create` 정의. 기존 required/initial/constraints 의미를 재사용하고 표별 확정 정책을 전달. 두 월 표·LNG Demand 새 연도 국가 입력·PMIInput·Base Case HDD/CDD/Temperature 새 연도 본문의 initial은 `{kind:"EMPTY"}`이며 미입력 칸은 빈칸 유지. §1.1 U1 적용 대상은 공통 빈칸/미입력 유지. 기존 기본값·필수값·자동 계산을 우선하며 기존 셀 초기화나 기존 SET/CLEAR 정책 변경을 뜻하지 않음 |
| `rowBindings` | 이번 페이지 본문에 대응하는 `{rowNumber,rowId,origin}`. 원본/성공 추가 행은 UUID rowId와 SOURCE/ADDED, 미활성 슬롯은 rowId:null과 EMPTY_SLOT. 헤더는 포함하지 않음 |
| `editValues` | 이번 페이지의 활성 논리 행별 `{rowId,values:{fieldKey:scalar}}`. 편집 가능한 실제 typed 값이며 표시/반올림 문자열과 다름. 실제 빈 셀은 key 생략. 원본 수식/읽기 전용 셀은 넣지 않음 |

`fieldKey`는 같은 표의 열을 식별하는 안정 key다. 기존 열은 발행 정의의 키를 유지하고 신규 열은 해당 Draft/표에서 서버가 발급한다. 기존 `columnN`·Excel 좌표·연월/Period와 별도이며 이동해도 유지한다. 삭제 키는 재사용하지 않는다. `rowId`는 Draft/표에 귀속하며 값/정렬/위치가 바뀌어도 유지한다. 다른 Draft/표의 ID, 삭제된 ID, EMPTY_SLOT에 대한 SET은 거부한다. 신규 행은 ADD_ROW로만 활성화한다. 열 참조와 control 값의 SET은 §4를 따른다.

capabilities는 표/동작 공통 구조·계산/보존 정책의 준비도, fields의 policyStatus는 필드별 준비도다. SET/CLEAR는 둘 다 READY인 대상만 허용한다. §1.1 채택한 ADD_COLUMN 버튼은 표 capability가 READY일 때, DELETE_COLUMN 버튼은 표 capability와 선택 field policyStatus가 모두 READY일 때 활성화하며 서버도 같은 조건을 검증한다. ADD_ROW는 공통 정책과 필요한 신규 필드 정책이 모두 완비돼야 한다. 불허/미완비 field가 있다고 정상 조회를 빈 성공으로 바꾸지 않는다.

**PNG Infrastructure 삭제 — 2026-10-21:** 새 정책 양식에서 `DELETE_ROW`는 저장된 추가 행(`rowBindings.origin=ADDED`) 또는 같은 요청의 앞선 `ADD_ROW`만 허용한다. 원본 행·`EMPTY_SLOT`은 거부한다. `DELETE_COLUMN`은 저장된 추가 연도 field 또는 같은 요청의 앞선 `ADD_COLUMN`만 허용한다. 원본 field는 `policyStatus.DELETE_COLUMN=NOT_ALLOWED`, 추가 연도 field는 `READY`이며, 두 삭제 명령 모두 표 capability가 `READY`여야 한다. 서버는 원본/추가 출처를 재검증하며 기존 양식의 Case/Draft를 새 정책으로 전환하지 않는다. 삭제 성공 후 구조·페이지·rowBindings·fields는 활성 항목 기준으로 반환하고 삭제된 ID/key의 이력·성공 mapping은 기존 계약을 따른다.

FE는 표시값을 숫자로 재해석해 전체 행을 되보내지 않는다. `editValues`와 사용자가 실제 변경한 값만 delta(변경분)로 보낸다. 서버도 생략된 기존 값을 유지한다. 숫자는 JSON number, 연도는 정수, 가격은 유한 10진수이며 숫자 문자열·null·수식은 허용하지 않는다. Decimal은 exponent 없는 JSON number로 채택했으며 FE는 손실 없는 10진 입력 처리를 준비한다. IEEE-754 변환으로 자릿수가 손실되는 값을 조용히 변환해 보내지 않는다. 서버는 10진 값을 보존하고 임의 반올림하지 않는다. BCPrices는 별도 업무 소수 자릿수 제한/저장 반올림을 추가하지 않는 것으로 채택했다. 숫자 정확도·전송/저장 표현 상한의 제품 검증은 §10의 BE 후속이다.

PMILogicInput의 기존 국가 D/I는 `TEXT`, `setConstraints:{required:true}`로 제공하고 `SET=READY`, `CLEAR=NOT_ALLOWED`로 연결한다. 국가 목록 options는 제공하지 않으며 비어 있지 않은 국가명은 그대로 저장한다. 국가 SET을 생략하면 기존 값을 유지한다. 대륙·지역 및 Selected 표는 국가 변경으로 자동 조회·갱신·비우기하지 않는다(domain §6.8). 기존 강도 E/J의 SET/CLEAR 정책은 유지한다.

신규 v6의 User-Adjusted `ADD_ROW`는 `values.country` 필수 자유입력, `values.intensity` 선택 입력이다. 같은 요청의 후속 SET을 반영한 새 국가를 Base/Selected에도 저장하며 세 표의 대응 행·범위를 원자 생성한다. 미입력 강도/지리 정보는 빈칸이다(domain §6.8). Base 직접 `ADD_ROW`는 `422 VALIDATION_FAILED`/`ACTION_NOT_ALLOWED`이며 Selected는 `navigationVisible:false` 내부 표로 입력·구조 동작을 제공하지 않는다. 성공 `rowMappings`는 직접 요청한 User 행만 반환하고 `affectedTableIds`에는 세 표를 포함한다. Base/Selected의 별도 rowId와 master 연결은 내부 이력으로 보관한다(DDL §6.9.5). 성공 후 최신 revision으로 조회한다.

Sector 국가/하위부문은 domain 규칙45의 필수 자유입력 TEXT다. `create.required:true`, `initial:{kind:"EMPTY"}`로 표현하고 CREATE/SET의 constraints에 options를 제공하지 않는다. 별도 열거값 제약은 없으므로 해당 constraints는 `{}`이며 §4의 공통 TEXT 타입/크기 제한과 §4.1의 필수 검증은 유지한다. 목록/seed 부재로 CREATE/SET을 INCOMPLETE로 만들거나 POLICY_INCOMPLETE를 반환하지 않는다. 실제 동작의 준비도는 다른 필수 구조/계산·보존 정책을 별도로 따른다. 모델의 명칭 인식 제약은 §10의 후속 확인이며 임의 명칭의 모델 반영 성공을 보장하지 않는다. 기존 QRY-01/02 응답은 변경하지 않는다.

Monthly 국가는 Sector 국가와 같은 필수 자유입력 TEXT다. 신규 Q 수요량은 `initial:{kind:"VALUE",value:0}`, create.constraints/setConstraints의 minimum:0으로 표현하고 N Year는 INTEGER minimum:1000/maximum:9999, O Month는 INTEGER minimum:1/maximum:12다. 신규 M/N/O/Q는 create.required:true이며 P/R은 사용자 편집 field로 제공하지 않는다. Sector I·Monthly Q·BCPrices 세 가격의 field policyStatus.CLEAR는 NOT_ALLOWED다. 다른 필드/표의 CLEAR 상태는 별도 정책을 따르며 이번 채택만으로 표 동작 전체를 READY로 제공하지 않는다.

## 4. 변경 요청·순서·오류 위치

`ChangeRequest = {requestId, baseRevision, operations}`. 세 필드 모두 필수다. requestId는 FE가 새 저장 시 생성하는 UUID, baseRevision은 마지막 EDT-02/03의 성공 revision, operations는 순서 있는 배열이다. 임시저장은 이 배열 전체가 한 성공/실패 단위다. 비어 있지 않은 새 요청이 성공하면 실제 값이 같아도 성공 revision을 한 번 증가시키는 안이다.

§1.1에서 채택한 결정을 아래 **6명령의 FE 전달용 최종 형식**으로 정리한다. 모든 명령에 `type`이 필수이며 표에 적힌 필드 외의 키는 받지 않는다. 현 파서는6명령을 수용하고 실제 처리기는 F-O-01 SET 한 건만 지원한다(§7.12·§10.2). 계약 확정과 runtime 지원은 구분한다.

| type | 필수 필드 | 의미 |
| --- | --- | --- |
| `ADD_ROW` | `commandId`, `tableId`, `clientRowKey`, `values:{fieldKey:scalar}` | 서버의 표별 생성 정책으로 신규 논리 행 생성. 임시키는 이 요청 안에서 유일. 실제 좌표/빈 슬롯/append 방식은 서버 정책이며 afterRowNumber를 받지 않음 |
| `SET` | `commandId`, `tableId`, 본문 또는 control 또는 헤더 대상, `value` | 본문은 rowRef와 fieldKey/clientColumnKey, control은 controlId, 헤더는 headerKey와 fieldKey/clientColumnKey. 세 대상은 배타적. 한 값을 지정. 두 열 참조 중 정확히 하나. rowRef는 `{rowId:UUID}` 또는 앞선 ADD_ROW의 `{clientRowKey:string}` 중 정확히 하나 |
| `CLEAR` | `commandId`, `tableId`, `rowRef`, `fieldKey` 또는 `clientColumnKey` | 실제 빈 셀 생성. 두 열 참조 중 정확히 하나, value는 금지. 0/빈 문자열/행 삭제와 다름. 해당 필드의 기존 CLEAR 정책 적용 |
| `DELETE_ROW` | `commandId`, `tableId`, `rowRef` | 허용된 논리 행 삭제. 전체 worksheet 행 삭제를 의미하지 않으며 연결·참조 보정은 표별 STRUCT 정책이 소유 |
| `ADD_COLUMN` | `commandId`, `tableId`, `clientColumnKey`, `headerValues` | 해당 논리 표의 마지막 기간 열 뒤에 생성. headerValues는 아래 표의 object. 본문 입력은 뒤의 SET으로 전달하며 rowRef/fieldKey/values/좌표/before/after를 받지 않음 |
| `DELETE_COLUMN` | `commandId`, `tableId`, `fieldKey` 또는 `clientColumnKey` | 두 열 참조 중 정확히 하나. 선택 열의 헤더·본문을 표별 정책에 따라 삭제. rowRef/headerValues/value/worksheet 좌표를 받지 않음 |

| ADD_COLUMN 대상 | 필수 headerValues | 서버가 생성하는 값·본문 |
| --- | --- | --- |
| LiqProdAdjustment | `{year:integer,month:integer}`; month 1~12 | Period는 domain 규칙65. 본문 빈칸, 미입력 유지·명시적0 구분. Period 직접 제출 금지 |
| LiqUtilAdjustment | `{}` (object 자체 필수) | 남은 월이 있으면 마지막 잔존 월 기준 다음 월/Period+1. 활성 월0개이면 domain 규칙47의 고정 원본 첫 월부터 재시작. 본문 빈칸 |
| LNG Demand 및 §1.1 U2의 연도형7표 | `{year:integer}` | 수동 연도. 각 표의 확정 본문 기본값/필수/계산 규칙 적용. BCRegas는 신규 수치0, 기존값·Regas 보존 |

headerValues는 위 키만 받으며 연도/month도 기존 JSON number 정확도·형식 계약을 따른다. 누락된 object·잘못된 타입·자동 항목/미정의 키 제출은400, object 안의 필수 year/month 누락은422 REQUIRED다. 연도에 근거 없는 범위·연속성·중복 금지를 추가하지 않는다(domain 규칙66). LiqProd의 월 건너뛰기·같은 연월/Period의 복수 열은 허용하며 서로 다른 fieldKey를 부여한다. 연도 추가 허용에서 삭제 허용을 추정하지 않는다.

commandId/clientRowKey/clientColumnKey/fieldKey는 `[A-Za-z][A-Za-z0-9_-]{0,63}`이다. commandId는 요청 전체에서, clientRowKey와 clientColumnKey는 각각의 종류 안에서 요청 전체에 유일하다. 알 수 없는 필드/type, JSON 중복 key, 중복 명령/임시키, 두 rowRef 동시 지정, fieldKey와 clientColumnKey의 동시 지정/둘 다 누락, 다른 표 또는 뒤에 나올 임시키 참조는400이다. 임시키는 같은 요청의 앞선 ADD가 만든 활성 대상만 가리키며 다른 요청의 영속 ID가 아니다.

**부속 control의 SET (2026-10-20):** 선택 표의 `tableData.controls`에서 `editable:true`인 항목은 `{"type":"SET","commandId":"mode","tableId":"<선택 표 ID>","controlId":"<controls[].id>","value":"Fixed"}`로 변경한다. 본문 셀의 `rowRef`·`fieldKey/clientColumnKey`와 `controlId`는 함께 보내지 않는다. control에는 행 ID나 새 fieldKey를 만들지 않으며 `CLEAR`·구조 명령의 대상으로 사용하지 않는다. `value`는 해당 항목의 `options[].value` 중 하나여야 한다. 기존 저장값이 선택지 밖이어도 조회/원본 보존은 유지하고, 새 입력만 선택지와 대조한다. LNG의 `Dynamaic` 저장값과 `Dynamic` 표시명 구분은 QRY §5.4의 확정 계약을 따른다.

동일 선택값을 포함한 순서 있는 SET은 마지막 값으로 저장하고 기존 원자성·revision·재전송 규칙을 따른다. 알 수 없는 control ID나 혼합 대상 형식은400, `editable:false`는422 `ACTION_NOT_ALLOWED`, 선택지 밖의 값은422 `OPTION_NOT_ALLOWED`이다. 발행 정책이 미완비/미지원이면 기존409 계약을 따른다. 처리 대상은 DEMAND `LNG Demand`의 연간 표에 연결된 C1 모드와 TRADE `ShippingCostInput` 운임5표의 C3 공유 모드다. Shipping C3는 원본 선택값 `"RE Base Case"`, `" Absolute"`, `" Percentage Growth (%)"`를 사용한다(뒤 두 값의 선행 공백을 유지하고 label만 공백 없이 표시). 기존 C3의 `"Absolute"`는 선택지 밖 기존값 보존 규칙을 따른다. 새 모드로 변경 시 운임5표의 내부 계산열에 같은 값이 반영된다. A 연간 운송비표는 보존한다. 후속 표는 실제 처리기 검증 후 제공하며 준비되지 않은 control은 Draft 조회에서 `editable:false`로 제공한다. 성공 결과의 rowMappings/columnMappings는 빈 배열이며 affectedTableIds에는 요청 표와 실제 값 소유 표(sourceTableId)를 포함한다. 내부 저장은 DDL §6.9.5의 기존 고정 셀 delta를 재사용한다.

**기존 연도 헤더 SET (2026-10-20):** 필드의 `policyStatus.SET_HEADER`가 READY인 열은 `{"type":"SET","commandId":"year","tableId":"<표 ID>","fieldKey":"<열 키>","headerKey":"year","value":2036}`으로 연도 제목을 수정한다. 앞선 ADD_COLUMN의 `clientColumnKey`도 fieldKey 대신 사용할 수 있다. rowRef/controlId·실제 행 번호는 보내지 않는다. `value`는 JSON 정수이며 null·문자열·소수는400이다. headerKey는 year만 지원하고 헤더 CLEAR는 허용하지 않는다. 현재 대상은 LNG Demand의 E열 이후 연도 열이며 새로 추가한 연도에도 같은 정책을 적용한다. 기타 표의 기존 고정 헤더 수정으로 확대하지 않는다. SET_HEADER가 없던 발행은 INCOMPLETE로 읽고 미완비/미지원/비허용 상태는 기존 정책 오류를 따른다.

연도 변경은 열 key·본문 값·합계 수식/저장값을 바꾸지 않는다. 같은 요청의 해당 헤더 SET은 마지막 값이 적용되며 신규 열 헤더는 ADD_COLUMN.headerValues에 먼저 제출해야 한다. 이 형식은 새 연도 필수값을 나중에 보충하는 방법이 아니다. 같은 요청의 열 추가·본문 SET과 원자 처리하고 재시작/최종 파일에 동일 값을 재생한다. 기존 연도 선택에 추가 정렬·중복·범위 제한을 만들지 않는다.

명령은 배열 순서대로 적용한다. ADD_ROW → 해당 clientRowKey의 SET/CLEAR, ADD_COLUMN → 해당 clientColumnKey의 SET/CLEAR/DELETE_COLUMN이 가능하다. 신규 행×신규 열의 셀은 두 ADD 뒤에 SET한다. ADD_ROW.values는 기존 fieldKey만 키로 사용하고 신규 열의 임시키를 섞지 않는다. 같은 셀의 SET/CLEAR는 마지막 명령이 최종 값이다. 같은 요청에서 삭제한 행/열의 뒤쪽 참조는400이다. 최종 상태에서 신규 필수값·승인된 중복 제한·구조/파생 정책을 검사하므로 앞선 ADD_ROW의 누락을 뒤의 SET으로 채울 수 있으나 요청 종료까지 빠져 있으면 전체 실패다. 신규 헤더는 ADD_COLUMN.headerValues에서 완성하며 생략값을 SET으로 보충하지 않는다. 별도 SET_HEADER 정책이 있는 기존/신규 연도 제목만 위 형식으로 다시 수정할 수 있다. Period 직접 수정은 허용하지 않는다. ADD 후 DELETE도 허용 정책 범위 안에서만 처리하며 대응 ID는 재사용하지 않고 성공 mapping에 DELETED로 표시한다. 기존 값 생략은 유지이며 새 값 생략은 각 표의 승인 초기값/필수 정책을 따른다. 임의 기본0/타표 값 복사는 하지 않는다.

같은 operations의 ADD_COLUMN→DELETE_COLUMN은 [§1.1 추가 후 삭제 채택](#column-add-delete-api-decision-20261019)에 따라 최종 표에는 열을 남기지 않고 columnMappings에 발급 fieldKey와 DELETED를 남긴다(§7.16). 같은 연월을 다시 추가하면 새 키, 동일 requestId/내용의 재전송이면 기존 operation/키/결과다. 삭제된 기존 fieldKey와 알 수 없는 fieldKey는400 INVALID_REQUEST이며 존재 유출 방지를 위한 Draft/table 접근404 검증은 먼저 유지한다.

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
| `columnMappings` | required array. 성공 신규 열 항목은 [§1.1 항목 형식](#column-mapping-item-decision-20261019), 진행/실패·신규 열 추가 없는 성공은 [§1.1 빈 배열 채택](#column-empty-mappings-decision-20261019)에 따라 []. 추가 후 삭제한 새 열은 fieldKey/DELETED 항목 유지. 공통 DTO/저장 성공 결과에 구현됨. 표별 실제 생성은 별도 |
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

<a id="fe-change-cases-20261019"></a>

### 7.0 FE 합의용 사례 목록 — 2026-10-19

표별 행/기간 추가의 1차 후보는 [원본 SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md)를 먼저 보고 사람이 수정한다. 원문의 `기간 추가`와 `세로 연도 행`을 구분하며, 삭제 허용은 추가 문구로 추정하지 않는다. 사용자 결정은 완료됐으며 값/행/열 JSON은 이 절의 최종 계약 mock을 사용한다. 현재 지원 차이는 §10.2로 분리한다. 실제 호출은 서버가 READY를 제공하는 범위에 한정한다.

| 케이스 | 요청/결과 예시 | 합의·FE 확인점 |
| --- | --- | --- |
| 기존 셀 값만 수정 | §7.8 | rowId/fieldKey로 변경분만 전송. 표시값으로 전체 행 재작성 금지 |
| 행 추가→추가한 행 수정→기존 행 수정 | §7.3~7.4 | 순서 있는 operations, clientRowKey→rowMappings→rowId |
| 기존 논리 행 삭제 | §7.9 | DELETE_ROW; 원본 worksheet 전체 행 삭제 지시 아님 |
| 연결 표가 있는 행 삭제 | §7.10 | 사용자는 BCRegasification만 명령. RegasAdjustment 연계는 BE 책임 |
| 값 지우기·0·생략 | §7.11 | CLEAR/SET0/미제출은 서로 다름. 필드별 CLEAR 허용 확인 |
| 신규 필수 누락·중복 | §7.5·§4.1~4.3 | 신규 필수 누락을0으로 채우지 않음. 최종 활성 행 기준 중복 검사 |
| 충돌·중복 재전송·연속 클릭 | §7.6·§5 | 새 저장마다 새 requestId, 응답 유실은 같은 ID/내용. BUSY 중 새 저장 중단 |
| 접수 후 실패 | §7.7 | HTTP200/SUC-000이어도 data.status=FAILED이면 저장 실패 |
| 월/연도 열 추가·삭제·추가 후 삭제·중복 월·오류·metadata | §7.13~7.18 | 확정 JSON. 공통 파서6명령 지원과 실제 표별 미지원은 §7.12를 따르며 READY 뒤 연결 |
| 최종저장 | §9.4 | 로컬 미저장/진행 요청 없음 확인. SUCCEEDED 후 새 Case와 BE 자동 Run 접수. §9.2.1의 execution으로 진행/실패 확인 |

공통 EDT-04 주소는 `POST /api/v1/mi/fundamental-model/input-drafts/{draftId}/change-requests`다. §7.8 이후는 별도 가상 Draft의 revision0에서 시작하는 **독립 예시**이며 앞 절의 저장 이력에 이어 실행하지 않는다. 실제 요청에는 조회에서 받은 tableId/rowId/fieldKey를 사용한다.

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

아래 JSON은 확정 계약의 BCPrices fields/rowBindings/editValues 발췌다. 이 예시에서는 BCPrices 열 삭제 허용을 추정하지 않고 DELETE_COLUMN을 INCOMPLETE로 표시한다. 표 capabilities 및 columnCreation은 §3.3을 따르며 이 예시는 실제 runtime 지원을 선언하지 않는다.

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
        "CLEAR": "INCOMPLETE",
        "DELETE_COLUMN": "INCOMPLETE"
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
        "CLEAR": "NOT_ALLOWED",
        "DELETE_COLUMN": "INCOMPLETE"
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
        "CLEAR": "NOT_ALLOWED",
        "DELETE_COLUMN": "INCOMPLETE"
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
        "CLEAR": "NOT_ALLOWED",
        "DELETE_COLUMN": "INCOMPLETE"
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
    "baseRevision":0,"resultRevision":null,"rowMappings":[],"columnMappings":[],"affectedTableIds":[],"failure":null,
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
    "columnMappings":[],"affectedTableIds":["OUTPUT/BCPrices/base_case_prices"],"failure":null,
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
    "baseRevision":1,"resultRevision":null,"rowMappings":[],"columnMappings":[],"affectedTableIds":[],
    "failure":{"reason":"TIMEOUT","currentRevision":1,"issues":[]},
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000004"
  }
}
```

같은requestId 재전송은 이 실패 결과만 반환한다. 새 저장 시도는 EDT-02의 현재revision을 확인하고 새requestId로 보낸다. 새 operation의 성공을 확인하기 전 FE가2037년 행을 성공 저장으로 표시하지 않는다.

### 7.8 기존 가격 수정 — 채택 계약의 mock

기존 hh=3.1을3.125로 바꾸고 다른 필드는 유지한다. `value`는 JSON number이며 문자열이나 표시 반올림값을 보내지 않는다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000005","baseRevision":0,
  "operations":[
    {"commandId":"setPrice","type":"SET","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"hh","value":3.125}
  ]
}
```

접수/성공 형식은 §7.3/7.4와 같으며 이 예시의 신규 행 대응은 `rowMappings:[]`, 성공은 `resultRevision:1`이다. FE는 성공 후 EDT-02/03에서 hh=3.125와 나머지 기존값 유지를 확인한다. 현재 F-O-01 처리기는 이 일반 BCPrices 요청을 처리하지 않는다.

### 7.9 기존 논리 행 삭제 — 채택 계약의 mock

Sector 수동 입력 해제는 해당 논리 행 DELETE_ROW다(domain 규칙45). 아래 tableId는 SUMMARY 표 이름에 맞춘 예시이고 rowId는 가상이다. 삭제한 행의 값·물리 행 번호를 요청에 싣지 않는다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000006","baseRevision":0,
  "operations":[
    {"commandId":"deleteSector","type":"DELETE_ROW","tableId":"DEMAND/DemandOutputOverwrite/Demand Model Manual Overwrite Sector Input","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"}}
  ]
}
```

정책/처리기가 준비된 경우 신규 접수202 뒤 EDT-05의 성공은 다음 형태다. 현 제품에서 일반 행 삭제 성공을 확인한 예시가 아니다.

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "operationId":"c0000000-0000-4000-8000-000000000006","requestId":"b0000000-0000-4000-8000-000000000006",
    "draftId":"d0000000-0000-4000-8000-000000000001","status":"SUCCEEDED","statusLabel":"저장 완료",
    "baseRevision":0,"resultRevision":1,"rowMappings":[],
    "columnMappings":[],"affectedTableIds":["DEMAND/DemandOutputOverwrite/Demand Model Manual Overwrite Sector Input"],"failure":null,
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000006"
  }
}
```

FE는 새 revision의 rowBindings/editValues에서 해당 rowId가 활성 행으로 남지 않는지 확인한다. 새 행이 없으므로 rowMappings로 기존 삭제 행을 통지하지 않는다. 남은 행의 rowId는 유지하며 화면 rowNumber를 rowId 대신 쓰지 않는다. 같은 요청에서 삭제한 행에 뒤이어 SET/CLEAR/DELETE_ROW를 보내면400이다. 다른 요청에서 과거 삭제 ID를 사용하면 §6의404 경계를 따른다. BCPrices의 행 삭제는 이 Sector 사례로 허용하지 않는다.

### 7.10 연결 표가 있는 행 삭제 — 채택 업무 규칙의 mock

BCRegasification 설비 삭제는 domain 규칙51대로 같은 설비의 RegasAdjustment 행도 함께 처리한다. FE가 두 번째 직접 삭제 명령을 만들지 않는다. 실제 표/행 대응·파일 처리 검증은 후속이다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000007","baseRevision":0,
  "operations":[
    {"commandId":"deleteFacility","type":"DELETE_ROW","tableId":"SUPPLY/BCRegasification/Annual Regas Capacity Baseline","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000011"}}
  ]
}
```

응답은 §7.9 형식이다. 성공 affectedTableIds에는 master/연결 표 및 서버가 실제 변경한 집계 표를 포함한다. 두 표 중 하나만 반영한 성공은 없으며 실패하면 전체가 직전 revision을 유지한다. **같은 master의 연도 열 추가에서는 RegasAdjustment를 보존**하는 별도 확정 규칙이 있으므로, 행 연계 규칙을 열에 적용하지 않는다.

### 7.11 CLEAR 금지와 값 표현 — 채택 계약의 mock

BCPrices hh는0 이상 숫자이며 CLEAR는 금지다. 정책과 검증기가 준비됐다고 가정한 요청/422 예시다. 현 미완비 catalog/미지원 처리기에서 먼저409가 발생할 수 있다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000008","baseRevision":0,
  "operations":[
    {"commandId":"clearPrice","type":"CLEAR","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"hh"}
  ]
}
```

```json
{
  "status":false,"code":"BIZ-001","message":"The operation violates a business rule.",
  "data":{"reason":"VALIDATION_FAILED","currentRevision":0,
    "issues":[{"commandId":"clearPrice","tableId":"OUTPUT/BCPrices/base_case_prices","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"hh","path":"/operations/0","reason":"CLEAR_NOT_ALLOWED"}]}
}
```

| 표현 | 의미/기대 |
| --- | --- |
| SET의 `value:0` | 유효한 숫자0. 빈값·삭제가 아님 |
| 기존 필드를 요청에서 생략 | 기존값 유지 |
| ADD_ROW의 필수값이 마지막 명령까지 누락 | 422 REQUIRED; 신규 기본값을 임의 보충하지 않음 |
| 숫자 필드의 `value:null`, `value:""`, `value:"3.125"` | 400 INVALID_REQUEST; 타입 자동 변환 없음 |
| CLEAR에 value 포함 | 400 INVALID_REQUEST |
| CLEAR 허용 필드에서 value 없는 CLEAR | 실제 빈 셀. 표/필드 모두 CLEAR READY일 때만 호출 |
| Sector I/Monthly Q의 CLEAR | 422 CLEAR_NOT_ALLOWED; 수동 입력 해제는 허용된 행 삭제 |

### 7.12 열 명령의 현재 접수 경계 — 공통 v2 구현

공통 파서는 ADD_ROW/SET/CLEAR/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN을 수용한다. 형식/임시키/열 참조 오류는400이며, 형식이 맞아도 접근 가능한 표의 열 정책이 누락되면409 POLICY_INCOMPLETE, 실제 처리기가 없으면409 POLICY_UNSUPPORTED다. 접수 전 거부에는 operation을 생성하지 않는다. 공개 metadata도 실제 열 처리기가 없는 READY 정책은 UNSUPPORTED로 반환한다. 표별 지원은 Draft에 고정된 양식 정책과 처리기 지원의 교집합으로 결정한다. PNG Infrastructure의 새 정책 양식에서만 §3.3의 추가 항목 삭제가 READY이며 기존 양식은 기존 상태를 유지한다. 로컬 검증과 실서버 배포 상태는 구분한다.

기존 정의에 ADD_COLUMN 정책이 누락된 표의 예시(현재 revision0):

```json
{"status":false,"code":"BIZ-001","message":"The operation violates a business rule.","data":{"reason":"POLICY_INCOMPLETE","currentRevision":0,"issues":[]}}
```

<a id="fe-final-column-json-20261019"></a>

### 7.13 월 열 추가·입력·진행/성공 — 최종 전달 JSON

이하 요청/응답은 §1.1의 사용자 결정과 §3~6의 공통 계약을 구체화한 **확정 계약의 mock**이다. 실제 지원 상태는 §10.2를 따른다. 각 예시는 독립적인 Draft revision0에서 시작한다. 가상 tableId/rowId/fieldKey·연월·수치는 실제 발행 ID나 지원 기간·값 범위를 약속하지 않는다.

**LiqUtilAdjustment:** FE가 자동 헤더값을 제출하지 않는다. 마지막 잔존 월이2031/12/Period168인 경우 서버가2032/1/169를 생성한다. 입력한 셀 외의 새 월 본문은 빈칸이다. value0.85는 형식 설명용이며 실제 SET 제약을 먼저 확인한다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000101","baseRevision":0,
  "operations":[
    {"commandId":"addMonth","type":"ADD_COLUMN","tableId":"SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization","clientColumnKey":"newMonth1","headerValues":{}},
    {"commandId":"setMonthValue","type":"SET","tableId":"SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000011"},"clientColumnKey":"newMonth1","value":0.85}
  ]
}
```

새 접수는 HTTP202다. RUNNING도 아래와 같은 빈 mapping/영향 표·null resultRevision/failure를 유지하고 status/statusLabel만 RUNNING/처리 중으로 바뀐다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "operationId": "c0000000-0000-4000-8000-000000000101",
    "requestId": "b0000000-0000-4000-8000-000000000101",
    "draftId": "d0000000-0000-4000-8000-000000000001",
    "status": "QUEUED",
    "statusLabel": "대기 중",
    "baseRevision": 0,
    "resultRevision": null,
    "rowMappings": [],
    "columnMappings": [],
    "affectedTableIds": [],
    "failure": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000101"
  }
}
```

EDT-05 HTTP200 성공 또는 동일 요청의 성공 후 재전송 결과다. FE는 mapping을 보관하고 새 revision의 구조/영향 표를 재조회한다.

```json
{
  "status":true,"code":"SUC-000","message":"Success.",
  "data":{
    "operationId":"c0000000-0000-4000-8000-000000000101","requestId":"b0000000-0000-4000-8000-000000000101",
    "draftId":"d0000000-0000-4000-8000-000000000001","status":"SUCCEEDED","statusLabel":"저장 완료",
    "baseRevision":0,"resultRevision":1,"rowMappings":[],
    "columnMappings":[{"commandId":"addMonth","clientColumnKey":"newMonth1","tableId":"SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization","fieldKey":"month_a1","columnState":"ACTIVE"}],
    "affectedTableIds":["SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization"],"failure":null,
    "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000101"
  }
}
```

**LiqProdAdjustment:** year/month를 직접 제출하고 Period는 생략한다. 아래2032/3의 Period는 domain 규칙65 기준171이다. setZero 대상만 숫자0이며 SET하지 않은 새 본문은 빈칸이다.

```json
{
  "requestId": "b0000000-0000-4000-8000-000000000104",
  "baseRevision": 0,
  "operations": [
    {
      "commandId": "addMonth",
      "type": "ADD_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "newMonth1",
      "headerValues": {
        "year": 2032,
        "month": 3
      }
    },
    {
      "commandId": "setZero",
      "type": "SET",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "rowRef": {
        "rowId": "a0000000-0000-4000-8000-000000000011"
      },
      "clientColumnKey": "newMonth1",
      "value": 0
    }
  ]
}
```

자동 Period 산식의 정본은 [domain 규칙65](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-header-decision-20261019)다. 기존 헤더 수정 명령을 추가한 것이 아니다.

### 7.14 연도 열 추가 — 최종 전달 JSON

BCRegasification 예시다. year는 직접 입력한다. 승인된 신규 수치0 생성·필수 정책을 적용하고, 아래 대상은120.5로 바꾼다. 기존값·Regas는 보존하고 최대 용량 열 이동/MAX 확장은 서버가 처리한다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000102","baseRevision":0,
  "operations":[
    {"commandId":"addYear","type":"ADD_COLUMN","tableId":"SUPPLY/BCRegasification/Annual Regas Capacity Baseline","clientColumnKey":"newYear1","headerValues":{"year":2032}},
    {"commandId":"setCapacity","type":"SET","tableId":"SUPPLY/BCRegasification/Annual Regas Capacity Baseline","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000011"},"clientColumnKey":"newYear1","value":120.5}
  ]
}
```

LNG Demand와 U2의 다른 연도형 표도 `{year:integer}` 형식이다. 본문 기본값/수식은 각 표 정책을 따른다. LNG Demand의 국가90개 새 입력은 빈칸에서 시작하고 합계16개는 서버 계산이며 FE가 합계/Excel 수식을 보내지 않는다. U1의 빈칸 기본값을 BCRegas의 승인된0에 덮어쓰지 않는다. 1 MiB/500명령 제한을 넘는 원자적 변경을 FE가 임의로 여러 요청으로 나누지 않는다.

### 7.15 기존 월 열 삭제 — 최종 전달 JSON

선택한 안정 fieldKey로 삭제한다. 연월/Period·현재 화면 위치로 다른 열을 같이 삭제하지 않는다.

```json
{
  "requestId":"b0000000-0000-4000-8000-000000000103","baseRevision":0,
  "operations":[
    {"commandId":"deleteMonth","type":"DELETE_COLUMN","tableId":"SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization","fieldKey":"month_b2"}
  ]
}
```

기존 열 삭제만 성공하면 rowMappings/columnMappings는 모두[]이고 resultRevision은1, affectedTableIds는 서버가 계산한 영향 표 목록이다. 중간 월 삭제 후 남은 월의 fieldKey/연월/Period/값 연결은 유지하며 물리 위치만 바뀔 수 있다. FE는 EDT-02/03에서 `editing.fields[].fieldKey`↔`columnField`, tableData.columns/헤더를 다시 읽고 이전 좌표를 폐기한다. LiqProd도 같은 요청 형식이며 월 열만 삭제하고 B:F 설비 정보는 보호한다. LiqUtil의 지정 H월 외 경계 삭제는 수식/보존 검증 완료 전 READY로 열지 않는다.

### 7.16 같은 저장의 추가 후 삭제·동일 연월 중복

LiqProd에서 허용된 새 월을 만들고 같은 요청에서 삭제한다. 각 명령의 정책 검사는 유지하며, 최종 열이 없어도 유효한 비어 있지 않은 요청의 성공 revision은1 증가한다.

```json
{
  "requestId": "b0000000-0000-4000-8000-000000000105",
  "baseRevision": 0,
  "operations": [
    {
      "commandId": "addThenDelete",
      "type": "ADD_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "temporaryMonth",
      "headerValues": {
        "year": 2032,
        "month": 3
      }
    },
    {
      "commandId": "deleteNew",
      "type": "DELETE_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "temporaryMonth"
    }
  ]
}
```

HTTP200 성공 결과. 신규 열은 최종 조회에 없지만 발급 fieldKey와 DELETED를 결과/이력에 남긴다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "operationId": "c0000000-0000-4000-8000-000000000105",
    "requestId": "b0000000-0000-4000-8000-000000000105",
    "draftId": "d0000000-0000-4000-8000-000000000001",
    "status": "SUCCEEDED",
    "statusLabel": "저장 완료",
    "baseRevision": 0,
    "resultRevision": 1,
    "rowMappings": [],
    "columnMappings": [
      {
        "commandId": "addThenDelete",
        "clientColumnKey": "temporaryMonth",
        "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
        "fieldKey": "month_c3",
        "columnState": "DELETED"
      }
    ],
    "affectedTableIds": [
      "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production"
    ],
    "failure": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d0000000-0000-4000-8000-000000000001/change-requests/c0000000-0000-4000-8000-000000000105"
  }
}
```

같은 requestId/내용의 재전송은 이 operation과 month_c3를 그대로 돌려준다. 이후 새 requestId로 같은 연월을 추가하면 다른 fieldKey를 발급한다. 아래처럼 같은 연월 두 열을 한 번에 추가하는 것도 허용한다.

```json
{
  "requestId": "b0000000-0000-4000-8000-000000000106",
  "baseRevision": 0,
  "operations": [
    {
      "commandId": "addDuplicate1",
      "type": "ADD_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "duplicateMonth1",
      "headerValues": {
        "year": 2032,
        "month": 3
      }
    },
    {
      "commandId": "addDuplicate2",
      "type": "ADD_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "duplicateMonth2",
      "headerValues": {
        "year": 2032,
        "month": 3
      }
    }
  ]
}
```

성공 columnMappings에는 addDuplicate1/duplicateMonth1과 addDuplicate2/duplicateMonth2의 ACTIVE 항목이 각각 하나씩 있고 fieldKey는 서로 달라야 한다. 두 Period는171로 같아도 별개 열이다. 중복 연월 오류를 새로 만들지 않는다.

### 7.17 열 입력 오류·실패 — 기존 Problem 재사용

headerValues object는 있으나 필수 month가 빠진 LiqProd 요청이다. 접수 전422로 거부하며 operation/revision/열을 만들지 않는다.

```json
{
  "requestId": "b0000000-0000-4000-8000-000000000107",
  "baseRevision": 0,
  "operations": [
    {
      "commandId": "missingMonth",
      "type": "ADD_COLUMN",
      "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
      "clientColumnKey": "newMonth1",
      "headerValues": {
        "year": 2032
      }
    }
  ]
}
```

```json
{
  "status": false,
  "code": "BIZ-001",
  "message": "The operation violates a business rule.",
  "data": {
    "reason": "VALIDATION_FAILED",
    "currentRevision": 0,
    "issues": [
      {
        "commandId": "missingMonth",
        "tableId": "SUPPLY/LiqProdAdjustment/Monthly LNG Train Production",
        "rowRef": null,
        "fieldKey": null,
        "path": "/operations/0/headerValues/month",
        "reason": "REQUIRED"
      }
    ]
  }
}
```

| 조건 | 결과·위치 |
| --- | --- |
| headerValues 자체 누락/null, 숫자 문자열, 자동 period 또는 미정의 키 제출 | 400 INVALID_REQUEST. 해당 headerValues 또는 키 위치 |
| SET/CLEAR/DELETE_COLUMN에 fieldKey와 clientColumnKey 동시 지정 또는 둘 다 누락 | 400 INVALID_REQUEST. 해당 명령의 열 참조 위치 |
| 없는/뒤에 추가할/다른 표/이미 삭제한 임시키 참조, 같은 종류 임시키 중복 | 400 INVALID_REQUEST. 실제 clientColumnKey/clientRowKey 위치 |
| month가1~12 밖 | 422 VALIDATION_FAILED/OUT_OF_RANGE. `/operations/0/headerValues/month` |
| 구조 변경 불허 | 422 VALIDATION_FAILED/ACTION_NOT_ALLOWED. 해당 명령/fieldKey 위치 |
| 정책 미완비/제품 미지원 | 409 POLICY_INCOMPLETE/POLICY_UNSUPPORTED |
| 접수 후 계산·보존·저장 실패 | §7.7처럼 EDT-05 HTTP200, data.status=FAILED. columnMappings/rowMappings/affectedTableIds=[], resultRevision=null, 직전 성공 유지 |

issue는 §6의 기존6필드만 사용한다. 아직 fieldKey가 없는 새 열/헤더는 fieldKey:null이고 path로 headerValues/clientColumnKey를 가리킨다. 접수 전 Problem에는 columnMappings를 붙이지 않는다. 오류 메시지 문자열을 파싱하지 않는다. 삭제 후 새 월이 같은 연월인 경우·LiqProd 중복 월에는 DUPLICATE_PERIOD/DUPLICATE_YEAR를 적용하지 않는다. 기존 BCPrices 행의 DUPLICATE_YEAR는 별도 정책으로 유지한다.

### 7.18 새 열 입력창의 헤더 metadata — 발췌

아래는 LiqProd의 `editing.columnCreation.headerValues` **값만** 발췌한 JSON이다. 실제 columnCreation에는 §3.3의 body도 필수이며, 표별 본문 dataType/create 정책과 구조·계산/보존 준비가 모두 완비된 경우에만 ADD_COLUMN=READY와 함께 제공한다. 미확인 본문 제약을 임의로 `{}` 또는 required:false로 채우지 않는다.

```json
{
  "year": {
    "dataType": "INTEGER",
    "create": {
      "required": true,
      "initial": {
        "kind": "EMPTY"
      },
      "constraints": {}
    }
  },
  "month": {
    "dataType": "INTEGER",
    "create": {
      "required": true,
      "initial": {
        "kind": "EMPTY"
      },
      "constraints": {
        "minimum": 1,
        "maximum": 12
      }
    }
  }
}
```

LiqUtil은 위 headerValues 값이 `{}`이고 자동 Year/Month/Period 입력칸을 만들지 않는다. 연도형8표는 year 정의만 사용한다. body.initial의 EMPTY/VALUE0, 필수값과 제약은 각 표의 채택 정책을 전달한다. 생성된 열의 실제 fieldKey/columnField·SET/CLEAR/DELETE_COLUMN 정책은 성공 후 EDT-03 재조회에서 받는다.

## 8. FE 선행 준비·OpenAPI/검증 인계

| 지금 문서 mock으로 준비 | 정책/후속 검증 뒤 실제 연결 |
| --- | --- |
| 기존 QRY 탐색과 Draft 진입, 공통 표/편집값·rowId 대응, 신규 임시키/필수 입력 강조 | 실제 catalog/field binding·seed/Case 연결 및 해당 동작 정책 완비 |
| 순서 있는 SET/CLEAR/ADD_ROW/DELETE_ROW/ADD_COLUMN/DELETE_COLUMN 명령 구성, revision·requestId 유지 | 표별 CLEAR/삭제·빈 슬롯/범위 확장·연결/참조·계산/보존 검증 |
| 202 접수·진행/종료 폴링, 실패 시 마지막 성공값 유지, 충돌/재전송 UI | durable 접수/worker·동시성/복구·늦은 완료 차단·전체 시간/정리 제품 검증 |
| §9 최종저장 접수/진행·완료/실패, 응답 유실 후 같은 요청 재전송·재진입 | 동일 최종 Case·4파일/양식/FINALIZED 원자성·부분 업로드/DB commit 불명·재시작·정리의 실제 제품 검증 |

OpenAPI 생성 시 이 spec이 해당 EDT 계약 정본이며 annotation/generated OpenAPI는 동기화 대상이다. operation summary는 §3, description은 §2 호출 시점/목적과 §3~6의 요청·부작용·응답별 FE 분기를 사용한다. `@Schema/@Parameter`에는 required/nullable/enum·범위·UUID/sourceVersion 형식과 rowRef oneOf 및 fieldKey/clientColumnKey 중 정확히 하나를 구조화하고, 명령은 type별 DTO/oneOf와 §7의 operation별 예시로 표현한다. `@ApiResponses`는 §6의 HTTP/ResultCode/Problem 및 202 접수·200 실패작업 조회 예시까지 구조화한다. field/type/오류를 message 문자열로만 설명하지 않는다.

후속 구현에서 검증할 행동은 동시 생성1건, 동일 요청의 동시 재전송/응답 유실, 다른 payload 충돌, 성공 revision 일괄 반영, 실패/timeout/늦은 완료 후 값·행·파생 결과 보존, 페이지/revision·원본 기준 충돌, 타사용자/tenant/source 접근 차단, 로그에 셀/거부값 없음이다. 물리 저장은 DDL §6.3의 공통7개 테이블/FK·유일성/검증된 JSON text·성공 경계로 정의했다. 기존 master 경계의 짧은 transaction/조건 갱신을 실제 구현/DBA 준비에서 검증한다. 무제한 JSON 질의 계층이나 새 범용 validator를 구현 요구로 추가하지 않는다.

이번 문서 검증은 JSON parse·ID/필드/상태·실패 보존 흐름과 링크·diff 확인이다. 이번 열 확장은 endpoint의 parser/DTO/annotation에 아직 반영하지 않았으므로 generated OpenAPI·Gradle/HTTP/DB/worker 행동 검증은 수행하지 않는다. 구현 시 실제 생성 결과를 확인해야 하며 문서 JSON 통과가 제품 Pass를 대신하지 않는다.

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
| `execution` | required object 또는 null. 저장 후 자동 실행 상태/최초 Run 연결은 §9.2.1. 저장 전/실패 및 과거 완료 건은 null |

SUCCEEDED는 새 INDEPENDENT Case/파일4개·고정 발행 연결·FINALIZED와 저장 성공이 같은 DB 확정으로 공개됐다는 뜻이다. 같은 확정에 최초 모델 실행 대기를 기록하고 BE가 기존 실행 로직으로 자동 접수한다. FE는 별도 Run POST를 호출하지 않는다. Case 생성 시점은 PENDING/lastRunId null이고 실행 접수 때 RUNNING과 runId가 함께 확정된다. 저장 결과(caseId/files)는 불변이며 아래 execution은 실행 진행에 따라 달라진다. `status:true/SUC-000` 접수/조회와 data.status:SUCCEEDED 및 모델 계산 완료를 구분한다.

#### 9.2.1 최종저장 후 자동 실행 — 2026-10-19 사용자 승인

- 요청 body/path는 유지한다. `FinalizationView.execution`은 required object 또는 null이다. 저장 전/저장 실패 및 이 변경 전 이미 완료된 저장은 null이다. 과거 완료 건을 배포만으로 자동 실행하지 않는다.
- execution 필드: `status`=`QUEUED|RUNNING|COMPLETED|FAILED`, `statusLabel`=실행 대기/실행 중/실행 완료/실행 실패, `runId`=접수 후 최초 연결 실행 ID(접수 전·접수 실패는 null), `statusEndpoint`=성공 Case의 `/api/v1/mi/fundamental-model/cases/{caseId}/status`, `failureReason`=FAILED에만 안전한 코드(접수 실패 `EXECUTION_REQUEST_FAILED`, Job 전송 실패 `JOB_SUBMIT_FAILED`, 그 외 모델 실패 `MODEL_EXECUTION_FAILED`). 이 객체는 최초 자동 연결한 Run을 추적한다. 명시적으로 별도 재실행한 최신 Run은 기존 Case/status에서 확인한다.
- 저장 진행은 기존 202/200 규칙을 유지한다. 저장 SUCCEEDED여도 execution.QUEUED/RUNNING이면 EDT-07을 2초 간격으로 조회하고 execution.COMPLETED/FAILED에서 멈춘다. 저장 FAILED 또는 execution=null인 과거 완료 건도 폴링을 종료한다.
- 같은 requestId/내용 재전송 및 FINALIZED 동일 내용 재클릭은 원래 작업/Case/최초 실행 연결을 반환한다. 실행이 FAILED여도 finalization 재전송으로 실행 대기를 초기화하거나 새 Run을 만들지 않는다. GET은 실행을 시작하지 않는다.
- BE는 영속 실행 대기를 주기적으로 접수한다. 현재 요청 사용자/역할·회사 권한과 최종 Case 소유권을 DB에서 재확인한다. Case 잠금과 같은 transaction에서 Run 생성 및 작업의 실행 연결을 저장해 동시 BE 인스턴스·응답 유실·재시작으로 중복 생성하지 않는다. 먼저 별도 Run API가 실행한 이력이 있으면 그 실행을 연결하고 추가 실행하지 않는다.
- 저장 성공 후 실행 접수 실패/모델 실패는 `status=SUCCEEDED`, 기존 caseId/files, `failure=null`을 유지하고 execution에만 실패를 표시한다. 파일/Case/Draft를 취소·삭제하지 않는다. 재실행은 사용자의 명시적 기존 Case Run 요청으로 처리하며 자동 재시도하지 않는다.
- 실행 Job 전달 및 완료/실패·timeout은 기존 Run lifecycle을 재사용한다. 저장 직후 BE 중단은 남은 대기에서 재개한다. Run 확정 후 Job 전송 전 중단은 기존 timeout/reconciliation 대상이며 저장을 실패로 소급 변경하지 않는다. 이 변경은 DB와 Kubernetes 사이의 분산 transaction을 보장하지 않는다.

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
| SUCCEEDED 조회/같은 요청 재전송 | 같은 operation/Case/최초 실행 연결 반환. FE는 별도 Run POST 없이 §9.2.1의 execution 상태를 조회. 실행 실패에도 저장 성공은 유지 |
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
    "execution": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d16d2400-8b00-4d00-9000-000000000001/finalizations/b16d2400-8b00-4d00-9000-000000000006"
  }
}
```

EDT-07 HTTP200 저장 성공·실행 대기의 data(동일 요청 재전송은 같은 Case/실행 연결):

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
  "execution": {
    "status": "QUEUED",
    "statusLabel": "실행 대기",
    "runId": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/cases/4101/status",
    "failureReason": null
  },
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
  "execution": null,
  "statusEndpoint": "/api/v1/mi/fundamental-model/input-drafts/d16d2400-8b00-4d00-9000-000000000001/finalizations/b16d2400-8b00-4d00-9000-000000000006"
}
```

마지막 두 예시는 동일 식별의 **서로 다른 대안 시나리오**다. FAILED가 나중에 SUCCEEDED로 바뀐다는 뜻이 아니다. UUID·Case/fileNo는 문서용이며 실제 등록/실행 결과가 아니다.

저장 성공 후 실행 접수 실패라면 성공 예시의 `status=SUCCEEDED`, Case/files와 `failure=null`을 유지하고 execution만 아래와 같이 반환한다. 모델 실행이 접수된 뒤 실패했다면 runId가 존재하며 failureReason은 §9.2.1의 해당 코드다.

```json
{
  "status": "FAILED",
  "statusLabel": "실행 실패",
  "runId": null,
  "statusEndpoint": "/api/v1/mi/fundamental-model/cases/4101/status",
  "failureReason": "EXECUTION_REQUEST_FAILED"
}
```

### 9.5 인수 기준·OpenAPI 인계

- 성공은 Case/4역할 fileNo·고정 양식·Draft/작업 상태의 동시 확정이며 revision 불변이다. 하나의 업로드/연결 실패도 부분 성공을 반환하지 않는다.
- 저장 성공 후 실행 대기의 재시작 처리, Run/작업 연결의 동시 확정, 동시 BE와 수동 실행 경합, 저장 성공/실행 실패 구분, 최종저장 재전송으로 FAILED 실행이 재시작되지 않음, 현재 사용자/회사/Case 소유권 재검증을 확인한다. legacy 성공 건은 자동 실행하지 않는다.
- 동시 최종저장·CHANGE 충돌, 같은 키/다른 종류 또는 내용, 성공 응답 유실/새 키 재클릭에도 최종 Case1건, 실패 기록 재전송과 새 키 재시도, QUEUED/RUNNING 재시작·timeout/늦은commit·준비파일 정리 시나리오를 제품에서 검증한다.
- 표별 정책 미승인을 공통 계약 미완료로 바꾸지 않는다. 실제 해당 표 쓰기/구조·계산 경로가 미완비면 POLICY_INCOMPLETE/POLICY_UNSUPPORTED로 거부한다. 원본 보존만 하는 표에 새 쓰기 정책을 요구하지 않는다.
- OpenAPI summary는 §3 EDT-06/07, description/Schema는 §9.1~9.3의 목적·부작용·멱등/재클릭·상태별 nullability, 202/200·Location/Retry-After·§6 오류와 §9.4 예시를 구조화한다. 구현·생성 OpenAPI/HTTP/worker 검증 현황과 실제 DB/storage 대기는 [기존 실행 기록](../work-notes/2026/2026-09-03-model-input-transfer-format.md#verification-result)을 참조한다.

## 10. Open Questions·준비도

**2026-10-19 사용자 결정 마무리:** [§1.1 U1/U2](#common-new-input-default-api-decision-20261019)와 domain §6.6으로 통합 질문2건을 모두 채택했다. 이번 요구 범위에서 사용자 필수 결정 미결은0건이다. 아래의 원본/계약 대조·JSON 세부·구현/검증·DBA 준비와 요청 밖 선택 기능 제안은 사용자 승인 대기와 구분한다. 기술 정책이 미완비인 동작은 기존 준비도/실패 계약을 따르며 업무 정책 채택만으로 전체 구현 완료나 READY로 표시하지 않는다.

첫 구현 전 코드/문서 대조의 연결·보완 경계는 [domain §5.4](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md), 코드상 저장 호환은 [DDL §6.4](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md), DBA 확인 항목은 **DDL §13 DBA-01~05**가 소유한다. 현재 구현 범위는 §1.2, 실제 적용·시험 결과와 남은 확인은 [기존 작업 노트의 최신 결과](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조한다. 추가 사용자 공통 업무 결정은 없으며 코드/문서 대조만으로 구현·제품 검증 완료를 판단하지 않는다.

공통 저장 구조와 EDT-01~07/최종저장·복구 **문서 계약은 이번 범위 완료**다. 전체 기능/INPUT-03·04 완료·운영 발행/DB 적용·제품 검증 완료를 뜻하지 않는다.

| 분류 | 남은 결정/확인 | 다음 행동 |
| --- | --- | --- |
| 직접 업로드의 공통 편집 | 원본 Case 없이 유효 파일1개부터 초안 생성. 생성 후 값/행/열은 기존 고정 양식·EDT-02~05와 동일 요청/결과를 사용. 별도 편집 API 없음. xlsm/xlsx·원본 형식 유지·파일당50MiB는 제공 명세 §3.3 | 공통 nullable 원본 필드·부분 sections/readiness 연결을 구현. 승인 F-O 공통 저장/복원·교체 초기화는 격리 검증. 일반 행·열 처리기의 실제 지원 범위는 기존 공통 작업을 따름 |
| 신규 순차 업로드 진입 | 생성/추가·교체/결과/확인 취소·재전송·상태/실패·원본 형식·단일 작성과120초 계약은 [제공 명세 §3.3](mi-fundamental-model-input-provision-feature-spec.md#incremental-input-upload-api-20261019), 저장/복구는 domain §5.6·DDL §6.10으로 완결. 업무 재질문 미결 없음. 목록/선택 재개 제외 유지 | 구현·격리 검증/독립 검토는 [업로드 작업 기록](../work-notes/2026/2026-10-19-model-input-incremental-upload.md). 실제 MySQL/DDL 적용·고정 헤더 규칙 새 버전 발행·환경 설정·고객 worker 실행 위치/대용량 성능은 배포 전 확인 |
| 표별 업무 정책 | §1.1 U1/U2와 domain §6.6 채택으로 현행 사용자 필수 미결0건. 기존 기본값·필수값·자동 계산 유지. 과거 PriceModel/Regas 추천·BCPrices 승인 밖 동작은 필수 승인 항목이 아님 | 원본/계약·정책 적용/계산·보존 범위 대조는 기술 후속. 이미 채택한 표별 입력/초기값·새 연도 입력 주체를 재질문하거나 요청 밖 기능을 자동 추가하지 않음 |
| 모델/STRUCT | Sector/Monthly 자유입력 명칭 인식 제약, 실제 runtime table/field/row binding·빈 슬롯/append·삭제/연결·참조/수식·서식/보존 | 해당 행동의 명세/구현 준비에서 확인. mock 성공이나 선정 fixture를 일반 정책 승인으로 확대하지 않음 |
| DBA 적용 준비 | DDL §6.4의 코드상 타입/저장 대조와 §13 DBA-01~05(schema/ID·제약/경합·packet·감사 예외·SQL/rollout) | 확인한 시험 범위와 남은 실제 DB 확인은 [기존 작업 노트](../work-notes/2026/2026-09-03-model-input-transfer-format.md#actual-result)를 참조. 기본 연동 시험을 DBA 운영 준비 전체 승인으로 대체하지 않음 |
| 제품 구현/검증 | durable 접수/BigDecimal 정밀도·payload 상한/복원·동시성/응답 유실·부분 업로드/DB commit 불명·재시작/정리·전체 시간·OpenAPI | INPUT-07~10 후속. 문서만으로 Pass/배포 ready 판정 없음. 변경 필요 시 계약/FE와 동기화 |
| 보관 후속 | Draft 존속 중 참조·성공 이력/요청/행을 함께 유지하고 자동 만료 없음 | 장기 archive/폐기는 별도 계약 시 결정. 이번 공통 저장 완료의 미결로 되돌리지 않음 |

F-S-06 제외 INPUT-02A는 DONE(CONDITIONAL)이며 규칙63/64·선정19사례 범위를 유지한다. 별도 외부 workbook/DRM 재시험·전체 조사/계산 재실행을 새 선행조건으로 추가하지 않는다.

- 현재 범위: 공통 저장/최종저장·실패 복구 계약 정리 Complete. 전체 문서는 **Draft**다. PriceModel/Regas 추가 추천은 채택하지 않았다.
- implementation readiness: FE 전달 계약/JSON은 준비됐고 필수 사용자 미결은0건이다. 표별 쓰기·최종 파일 제품 활성화는 §10.2의 구현/정책 데이터·구조/계산/보존 검증이 남아 있다. 이번 작업은 구현·DB 적용을 수행/승인하지 않는다.

<a id="fe-column-decisions-20261019"></a>

### 10.1 FE 열 추가·삭제 결정표 — 사용자 결정 완료

이번 행·열 형식의 필수 사용자 미결은0건이다. 공개 JSON은 §3.3~§7.18에 반영했으며 FE 확인/연동 검증은 아직 별도다. [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md)의 원문 기간 추가11후보 중 직접 Regas 추가 제외를 반영한10후보(연도8·월2)를 출발점으로 사용한다. SUMMARY 문구는 삭제 허용이나 runtime READY를 대신하지 않는다.

| 결정 | 확정 내용·정본 위치 | 남은 기술 확인 |
| --- | --- | --- |
| C1/C2 대상·명령 | 승인 표의 마지막 논리 기간 열, 기존 EDT-04의 ADD_COLUMN/DELETE_COLUMN(§4) | 실제 발행 tableId/범위·표별 준비도 |
| C3 식별·수명 | fieldKey/clientColumnKey/columnMappings, 삭제 키 미재사용·재전송 기존 키 유지(§4~5) | 저장·복원·파일 생성 |
| C4 헤더 | Prod year/month 수동·Period 자동, Util 전부 자동, 연도8표 year 수동(§4·§7.13~14) | 원본/표별 생성·계산 검증 |
| C5 본문 | 빈칸·미입력·명시0 구분, 기존 기본값/필수/계산 우선(§1.1 U1·§3.3) | 확정 정책의 catalog 정의·처리기 연결 |
| C6 조회 | 두 열 capability, 필드 DELETE_COLUMN 상태, columnCreation(§3.3) | 조회 DTO·정의 parser·seed·OpenAPI |
| C7 삭제 | Prod 월 열/B:F 보호, Util 승인 범위. 연도 추가로 삭제 허용 추정 금지 | 첫/중간/마지막/전체 삭제의 실제 영향에 따라 준비도 제공 |
| C8 연결 | FE는 원본 표를 명령하고 BE가 승인된 연결/수식/보존 처리 | Regas 연계·MAX 이동·합계 등 표별 사례 |
| C9 결과·오류 | mapping5필드/ACTIVE·DELETED·빈 배열, 기존 Problem/원자성/재전송(§5~7) | 실패·재시작·동일 요청 결과 복원 |

LiqProd Period 산식과 월 건너뛰기/중복 허용은 [domain 규칙65](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#liqprod-month-header-decision-20261019), 근거 없는 제한을 추가하지 않는 원칙은 규칙66이다. 이미 확정된 업무 결정을 다시 미결로 두지 않는다.

<a id="fe-contract-implementation-gap-20261019"></a>

### 10.2 확정 계약–공통 v2 구현 및 실제 표별 후속 — 2026-10-19

아래 표는 2026-10-19 당시 공통 v2 구현과 격리 검증 기록이다. 이후 표별 처리기 지원이 확대됐으며, 현재 PNG Infrastructure 추가 항목 삭제 범위는 §3.3과 새 양식 정책을 따른다. 실제 DB 적용·실서버 배포·FE 연동 완료를 뜻하지 않는다.

| 영역 | 공통 구현 | 남은 경계 |
| --- | --- | --- |
| EDT-04 요청 | InputDraftJson/Controller가6명령·임시 열 참조·headerValues와 순서를 해석/검증 | 실제 표별 구조/계산/보존 처리기는 F-O-01 SET 한 건만 연결. 미지원 열 처리는 접수 전에 거부 |
| EDT-03 metadata | schemaVersion1 정의에6capabilities·필드 DELETE_COLUMN·columnCreation 확장. 누락 열 정책은 INCOMPLETE, 미지원 READY는 UNSUPPORTED | 표별 정책/처리 검증 뒤 별도 catalog 발행. 이번에 seed/READY 확대 없음 |
| EDT-04/05 결과 | 모든 OperationView에 columnMappings 배열. 성공 ADD 순서/ACTIVE·DELETED와 동일 요청 결과 보존 | 실제 표별 새 키 발급/파일 결과는 해당 처리기에서 연결 |
| 저장/재조회 | version2 전용 요청·revision0·성공 delta. 원본 열 출처/현재 위치/삭제 identity와 이전 overlay 이동, 신규 행×열·blank/0·헤더/control 복원 | 가상 fixture로 저장/재조회/재시작 검증. BCRegas 자동 연결 행 저장은 DDL §6.9.5 후속 |
| worker/FINALIZE | 기존 F-O-01 저장→재조회→최종4파일을 같은 저장 version2로 유지. 미지원 열 성공 이력은 최종저장에서 무시하지 않고 거부 | 월/연도 실제 처리기·원본/계산·최종 파일 검증 후 지원 확대 |
| DB | 기존 JSON 저장/관계형 행 이력 재사용. 새 열 테이블 없음. CHECK2 수동 SQL 준비 | 실제 개발/운영 DB 미적용. 승인7table 초기화·CHECK2·전체 v2 코드 교체 순서와 구 코드 혼합 금지는 DDL §6.9/전용 SQL 참조 |

**준비도:** 위 표의 2026-10-19 판정은 당시 기록이다. FE는 조회 응답의 READY만 호출하며 현재 작업 폴더의 PNG 삭제 구현·신규 양식 SQL 초안은 실제 DB 발행이나 실서버 배포 증거가 아니다. 미배포 v1 이중 reader/변환은 제공하지 않는다.

### BCRegasification 행 편집 개방 (v7, 2026-10-25)

v7 카탈로그에서 `SUPPLY/BCRegasification/Annual Regas Capacity Baseline`의 `ADD_ROW`/`DELETE_ROW`를 허용한다. 추가 시 `continent`, `region`, `country`, `regasificationPlant`, `lifeCycleCategory`는 필수 입력이며 연간 용량 필드는 생략하면 숫자 0으로 생성한다. 기존 식별값의 SET/CLEAR 정책은 변경하지 않는다. `RegasAdjustment` 대응 행 생성·삭제와 국가별 월 합계 갱신은 기존 연결 규칙대로 서버가 처리하며, 해당 시트의 직접 행 추가·삭제는 허용하지 않는다.

이미 발행된 v6 및 기존 Case/Draft의 연결은 변경하지 않는다. 새 정책 적용은 `docs/model-input-regas-rows-v7-deployment-guide.md`의 새 버전 등록·발행 절차를 따른다.

---

<a id="draft-baseline-comparison-restore-proposal-20261027"></a>
## 11. 원본 비교·되돌리기 API 설계 — 2026-10-27

**상태: 제품 코드 구현, 기본 비활성; DB 적용·배포 미수행.** 사용자 확정 업무 기준은 [domain §5.6A](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#draft-baseline-comparison-rules-20261027)에 있다. 아래 경로·DTO·오류·제한은 그 기준을 실현하는 계약이다. 기존 §3~7의 배포 상태를 덮어쓰지 않는다. §11은 새 기능의 계약 정본이고 §3~7의 미확장 6명령 계약은 기존 클라이언트/저장 version 2의 기준이다. 이 절의 확장 명령을 포함한 CHANGE만 version 3으로 분기한다. 모든 표의 READY를 승인하는 뜻은 아니다. 물리 저장·하위 호환은 [DDL §6.14](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md#draft-restore-storage-design-20261027)을 따른다. 실행 증거와 검증 한계는 [안정 작업 노트](../work-notes/2026/2026-10-27-draft-comparison-restore-api-design.md)에 기록한다.

### 11.1 설계 경계와 공통 규칙

- 변경 목록 조회, 삭제된 원본의 화면 preview 조회, 기존 EDT-04의 복원 명령을 제공한다. 모두 Draft의 회사·사용자 소유와 원본 접근을 검사한다. 내부 표 또는 다른 소유자 대상은 공개하지 않는다. 원본 파일 경로·DB ID·수식 원문은 응답하지 않는다.
- 비교는 해당 역할의 현재 고정 원본과 **현재 성공 revision** 사이의 최종 차이다. 중간에 바꾸었다가 원본과 같아진 셀 및 추가 후 삭제한 항목은 차이가 아니다. FE의 미저장 변경은 서버 결과 위에 별도 표시한다. 현재 revision과 다른 요청은 409 `REVISION_CONFLICT`; 처리 중에는 직전 성공 상태를 읽는다. 렌더 전후 revision·원본/파일을 재검사하여 혼합 응답을 방지한다.
- 값은 `{"kind":"EMPTY"}` 또는 `{"kind":"VALUE","value":scalar}`로 표현한다. VALUE는 null 불가, number/string을 보존한다. 숫자는 정확한 10진 값으로 비교해 `3.20`과 `3.2`를 동일하게 취급하고 문자열은 공백·대소문자를 보존한다. EMPTY, 숫자 0, 문자열 `"0"`은 서로 다르다. 값의 범위·정밀도는 기존 편집 계약을 따른다.
- 아래 새 조회의 전체 응답은 기존 표 조회와 같이 최대 2 MiB다. 변경 목록은 한 페이지 최대 2,000 항목이며 bytes 상한이 먼저 오면 항목 경계에서 끊는다. preview는 EDT-03의 본문 최대 2,000행 규칙을 적용한다. 단일 필수 항목도 들어가지 않으면 422 `RESPONSE_TOO_LARGE`; 일부만 성공한 것으로 반환하지 않는다.

### 11.2 변경 목록 조회

`GET /api/v1/mi/fundamental-model/input-drafts/{draftId}/changes?revision={revision}&tableId={tableId}&cursor={cursor}`

`draftId` UUID, `revision` 0 이상 정수, `tableId` 공개 표 ID는 필수다. `cursor`는 첫 호출에 생략, 이후 직전 응답의 `nextCursor`를 사용한다. `data`는 `{draftId,tableId,revision,items,nextCursor}`이고 items는 빈 배열 가능, 마지막 nextCursor는 null이다. 소유/표 404, 형식·커서 오류 400, `REVISION_CONFLICT`/`SOURCE_CHANGED` 409, 복원 불가 원본/저장 이력 오류 500을 적용한다. 오류 본문은 기존 Problem envelope를 사용한다. preview와 같은 JVM 실행 슬롯·처리 기한·임시 용량을 공유하며, 비교 원본 셀은 요청당 최대 250,000개다. 슬롯 부족은 429 `CHANGE_QUERY_BUSY`와 `Retry-After: 2`, 기한·용량·셀 상한 초과는 422 `CHANGE_QUERY_UNAVAILABLE`이다. 부분 목록을 성공으로 반환하지 않는다.

- 커서는 서버 발급의 변조 방지된 불투명 문자열이다. Draft·표·revision·역할 기준 파일·정렬 위치와 API 버전을 묶고, 형식/서명/대상 불일치는 400이다. 유효한 이전 revision 커서는 현재 revision 검사에서 409다. 최대 길이 4096 ASCII 문자; 첫 페이지에 빈 문자열은 유효한 커서가 아니다. 동일 revision의 데이터는 순서와 페이지 경계가 결정적이며 누락·중복이 없다.
- 정렬은 `ROW`, `COLUMN`, `CELL`, `HEADER`, `CONTROL` 순이다. 같은 종류 안에서 ROW는 rowId, COLUMN/HEADER는 fieldKey(HEADER는 다음으로 headerKey), CELL은 rowId 다음 fieldKey, CONTROL은 controlId의 Unicode 코드 포인트 순이다. 값·표시 좌표를 정렬 키로 사용하지 않는다. cursor는 마지막 완전 반환 항목을 가리킨다.
- 각 항목의 공통 필드는 `kind`, `change`, `restoreStatus`, `restoreReason`이다. status는 `READY`, `INCOMPLETE`, `NOT_ALLOWED`, `UNSUPPORTED` 중 하나이며 READY인 경우 reason은 null, 나머지는 안정된 코드 문자열이다. 기존 표·필드 정책과 구현·안전성 검증 모두 충족해야 READY다. 기능 비활성/미구현은 UNSUPPORTED로 반환하며 구현이 없는데 READY를 추론하지 않는다.
- FE 되돌리기 버튼의 권위는 변경 목록 항목의 `restoreStatus`다. 이 계약은 기존 `editing.capabilities`에 RESTORE 키를 추가하지 않는다. 항목에 READY가 없거나 기능 미배포로 조회가 제공되지 않으면 복원을 활성화하지 않는다. 기존 클라이언트의 6명령과 DTO는 유지하고 새 클라이언트만 새 조회·복원 type을 사용한다.
- 이 기능은 새 발행 정책 key를 요구하지 않는다. 원본 행 복원은 고정 표의 원본 행 삭제 정책이 허용하고 `capabilities.DELETE_ROW=READY`인 범위에서만 제공한다. 원본 열 복원은 고정 표의 `DELETE_COLUMN` 및 복원할 원본 필드의 `policyStatus.DELETE_COLUMN`이 모두 READY인 범위다. 이는 복원 처리기 지원을 뜻하지 않으며 지원·구조·값 검증을 추가로 거친다. 정책 미완비/업무 불허/처리기 부재는 각각 INCOMPLETE/NOT_ALLOWED/UNSUPPORTED다. 복원하는 편집 가능 값은 기존 SET/CLEAR·타입·필수/옵션 제약을 통과해야 하고, 수식·읽기 전용 구조에는 쓰기 권한을 추가하지 않는다.
- `restoreReason`은 READY면 null, 미완비는 `POLICY_INCOMPLETE`, 업무 불허는 `ACTION_NOT_ALLOWED`, 기준값의 현 제약 불일치는 `BASELINE_VALUE_NOT_ALLOWED`, 처리기 부재/기능 OFF는 `POLICY_UNSUPPORTED`, 안전하게 변환할 수 없는 구조는 `STRUCTURE_UNSUPPORTED`다. 기준값 제약 불일치는 NOT_ALLOWED, 안전 구조 미지원은 UNSUPPORTED와 조합한다. 원본/이력 손상은 항목 상태로 숨기지 않고 조회 전체를 500으로 실패시킨다.


| kind | 필수 추가 필드 | 포함 조건 |
| --- | --- | --- |
| CELL | `rowId`, `fieldKey`, `baseline`, `current` | 활성 원본 행×활성 원본 열의 편집 가능 값이 서로 다른 경우. 원본과 현재 모두 빈칸이면 생략. |
| ROW | `rowId`, `origin`, `originalRowNumber` | 삭제된 SOURCE(`change:DELETED`) 또는 활성 ADDED(`change:ADDED`). SOURCE는 기준 1-based 행 번호, ADDED는 null. 빈 추가 행도 포함. |
| COLUMN | `fieldKey`, `origin`, `originalColumnNumber` | 삭제된 SOURCE 또는 활성 ADDED. SOURCE는 기준 1-based 열 번호, ADDED는 null. |
| HEADER | `fieldKey`, `headerKey`, `baseline`, `current` | 활성 원본 열의 편집 가능 헤더가 다를 때. |
| CONTROL | `controlId`, `sourceTableId`, `sourceControlId`, `baseline`, `current` | 편집 가능 control의 실제 값이 다를 때. 공유 control은 소유 표와 control ID를 함께 제공한다. |

삭제/추가 구조 내부의 셀은 CELL로 중복 나열하지 않는다. FE는 활성 ADDED 행·열 전체에 추가 표시를 그린다. 삭제된 추가 행/열은 목록에 포함하지 않는다. 원본 구조 셀과 추가 구조 셀이 교차하면 구조 표시만 한다. 삭제된 SOURCE의 `restoreStatus`는 preview 가능 여부와 달리 실제 저장 가능성까지 나타낸다. 아직 저장하지 않은 FE 입력에 대해서는 FE가 같은 값 비교 규칙과 대기 명령을 이용한다.

예: `data.items`에 `{"kind":"CELL","change":"MODIFIED","rowId":"a0000000-0000-4000-8000-000000000008","fieldKey":"price","baseline":{"kind":"EMPTY"},"current":{"kind":"VALUE","value":0},"restoreStatus":"READY","restoreReason":null}`를 포함한다. 전체 응답은 `BaseResponse`로 감싼다.

### 11.3 삭제 원본 구조 preview

`GET /api/v1/mi/fundamental-model/input-drafts/{draftId}/restore-preview?revision={revision}&tableId={tableId}&kind={ROW|COLUMN}&targetId={rowId 또는 fieldKey}&startRow={startRow}`

- 표 소유권과 현재 역할 기준점에 속하는 삭제된 SOURCE만 허용한다. 현재 활성 대상, ADDED, 이전 교체 파일 대상, 대상 종류/ID 형식 불일치는 400 `INVALID_REQUEST`; 소유하지 않은 Draft/표는 404다. revision·source 불일치와 읽기 실패는 §11.2와 같다. 미지원 또는 미완비 표는 409 `POLICY_UNSUPPORTED`/`POLICY_INCOMPLETE`, 정책 불허는 422 `ACTION_NOT_ALLOWED`다.
- 서버는 현재 성공 workbook의 임시 복사본에 **§11.5와 동일한 복원 처리·검증**을 적용하고, 그 결과 표를 기존 EDT-03과 같은 표시/편집 projection으로 읽는다. 변경 접수·revision 증가·영속 파일/DB 변경은 없다. 다른 작업을 시작하거나 진행 포인터를 점유하지 않는다. 반환 전 revision·원본 파일을 다시 확인한다. 일시 파일을 제거하고 실제 저장·최종저장 파일로 사용하지 않는다.
- 성공 `BaseResponse.data`는 `{draftId,tableId,revision,kind,targetId,preview:true,tableData,page,editing}`이다. `tableData/page/editing` 구조와 `startRow`/`page.nextStartRow`·행 및 2 MiB 상한은 기존 EDT-03을 그대로 따르며, 조회 대상은 **복원 예상 표 전체**다. 복원 후 표시 행·읽기 전용 및 수식 표시 셀, 추가 행·열과 그 교차값, control과 편집 가능 typed 값도 같은 projection에 포함한다. 기존 표시 범위 규칙이 적용된다. preview의 rowNumber/columnField는 예상 좌표이고 수정 요청의 안정 식별자가 아니다. `revision`은 현재 성공 revision이며 예상 표는 새 revision이 아니다.
- 같은 revision·대상으로 `nextStartRow`가 null이 될 때까지 재조회한다. 표를 변경하는 작업이 성공하면 남은 페이지는 409이며 처음부터 다시 읽는다. 일부 항목만 보였는데 전체 preview로 취급하지 않는다. 미저장 FE 변경은 요청에 포함되지 않으므로 FE가 자신의 대기 변경을 별도로 다시 겹치고 충돌 시 사용자에게 알린다.
- `preview:true`는 **저장 전 예상 상태**를 뜻한다. 숫자와 수식 계산 표시값을 최종 확정으로 취급하지 않는다. 저장 시 같은 baseRevision·원본·정책·대상이라도 처리 실패가 가능하다. 저장 성공 후 결과 revision으로 EDT-03을 재조회해 확정한다. 예측 처리와 저장 처리의 불일치 또는 미검증 연결/계산은 READY가 아니다. 작업 중 실패/리소스 초과는 부분 성공으로 돌리지 않는다. 서버가 처리를 중단하거나 예상 파일을 검증하지 못하면 422 `RESTORE_PREVIEW_UNAVAILABLE`로 기존 Problem envelope를 반환한다. 기존 원본/이력 무결성 오류는 500이다.


#### 11.3.1 preview 자원 제한

- 새 preview는 `mi.model-input.restore-preview.enabled=false`를 초기값으로 시작한다. 활성화 시 웹 JVM당 동시 실행 최대 1건, 큐 대기 없음, 접수부터 30초, 요청 소유 임시 파일의 실제 동시 사용량 합계 256 MiB를 초기 안전 상한으로 둔다. 이 수치는 현재 운영 성능을 입증한 값이 아니라 최초 배포 전 측정할 보수적 기본값이다. 양수 설정으로 조정하되 JVM/복제본 수와 디스크 용량을 함께 검증한다. 상한 0/음수 또는 무제한 설정은 허용하지 않는다.
- 슬롯이 없으면 파일 다운로드·재생 전에 429 `RESTORE_PREVIEW_BUSY`(code `BIZ-001`)와 `Retry-After: 2`를 반환한다. 자동으로 대기열에 넣지 않는다. 기능 OFF는 409 `POLICY_UNSUPPORTED`다. 각 페이지도 별도 요청이므로 같은 제한을 적용한다. 응답 크기와 별도로 실행 시간/소유 파일 bytes를 검사한다. 원본 다운로드·복호화·재생·계산·렌더 전체가 하나의 deadline과 소유 scope 안에 있어야 한다.
- deadline/bytes 상한 또는 안전 변환 실패는 422 `RESTORE_PREVIEW_UNAVAILABLE`, 클라이언트 연결 종료·서버 shutdown은 실행 중단 신호로 처리한다. 체크포인트는 파일 읽기/쓰기·재생 revision 경계·계산/렌더 안에서 확인한다. 파일 IO 자체에도 남은 기한을 적용하고 중단을 무시하는 blocking 호출로 상한을 무력화하지 않는다. 원본과 직전/다음 replay 파일 외 불필요한 임시 복사본은 즉시 제거하고 finally에서 요청 scope를 닫는다. 실제 실행 종료·정리 전에 슬롯을 반환하지 않는다. 정리 실패는 500이고 운영 로그로 남기며 소유 workspace의 기존 재시작 정리 규칙을 따른다.
- 미리보기는 영속 Draft 작업을 점유하지 않는다. 실제 저장이 병행 성공하면 반환 직전 revision/원본 검증으로 409를 반환하고 임시 결과는 폐기한다. 실행 동안 DB transaction/row lock을 유지하지 않는다. 최대 역할 파일·긴 이력·동시 페이지·저장 경합에서 시간/디스크 경계와 정리를 확인한 뒤 활성화한다. 용량·시간 안에 처리할 수 없는 표는 안전하게 실패하고 지원한다고 공개하지 않는다.

### 11.4 FE 적용 및 저장 명령

1. EDT-03과 변경 목록을 같은 revision으로 읽고 FE는 로컬 변경을 겹친다. 되돌리기는 화면과 대기 명령만 변경한다. 미전송 추가를 되돌리면 ADD 및 그 임시키에 의존한 대기 명령을 제거한다. 미전송 삭제를 되돌리면 대기 DELETE를 취소하고 해당 로컬 값을 복구한다.
2. 저장된 기존 셀은 원본 VALUE면 `SET`, EMPTY면 `CLEAR`; header/control은 기존 SET이다. 추가된 저장 행/열의 되돌리기는 기존 DELETE다. 기존 정책 및 validation이 허용한 대상만 READY다. 원본이 옵션 밖이거나 CLEAR가 불허면 우회하지 않고 NOT_ALLOWED/INCOMPLETE/UNSUPPORTED로 노출한다.
3. 성공 저장된 원본 삭제는 `{"type":"RESTORE_ROW","commandId":"restoreRow","tableId":"...","rowId":"<UUID>"}` 또는 `{"type":"RESTORE_COLUMN","commandId":"restoreColumn","tableId":"...","fieldKey":"<key>"}`를 EDT-04의 `operations`에 넣는다. 다른 필드·클라이언트 값·좌표는 받지 않는다. commandId/fieldKey 형식은 기존 §4를 따른다. 복원은 같은 원본 ID를 재활성화하며 결과의 기존 `affectedTableIds`에 실제 영향 표를 모두 포함한다. 별도의 신규 ID mapping은 없다.
4. 요청의 명령은 기존 순서와 전체 성공/실패를 따른다. 복원 대상은 직전 성공 revision에서 삭제되어 있어야 한다. 한 요청에서 같은 구조 ID를 복원·삭제하거나 두 번 복원하는 조합은 400 `INVALID_REQUEST`; 복원 후 같은 요청에서 그 행/열을 대상으로 SET/CLEAR/헤더 SET은 허용하되 유효한 활성 교차와 정책을 검증한다. 삭제된 다른 축에 쓰기, 복원 전 쓰기, 추가된/이미 활성/다른 기준점 대상 복원은 400이다. 소유권 404는 먼저 검사한다. §11.2의 복원 적용 정책이 미완비면 409 `POLICY_INCOMPLETE`, 처리기 미지원이면 409 `POLICY_UNSUPPORTED`, 불허면 422 `ACTION_NOT_ALLOWED`다.
5. 같은 Draft는 한 작업만 진행한다. FE는 전송 중인 payload를 바꾸지 않고 전송 이후 변경을 별도 대기시킨다. 202는 저장 성공이 아니다. 성공 뒤 새 revision의 영향 표·목록을 재조회하고 이후 로컬 변경을 새 requestId/baseRevision으로 보낸다. 실패 시 대기 입력을 보존한다. `DRAFT_BUSY`는 완료 후 재조회, `REVISION_CONFLICT`/`SOURCE_CHANGED` 또는 파일 교체는 오래된 ID를 자동 재전송하지 않고 재조회·충돌을 표시한다. 동일 requestId/동일 요청 재전송은 기존 작업 결과를 재사용한다.

### 11.5 행·열 복원 처리

- 원본 순서 보존을 위해 서버는 해당 축에서 대상보다 뒤에 있는 첫 활성 원본의 바로 앞에 삽입한다. 그런 원본이 없으면 앞의 마지막 활성 원본 바로 뒤, 둘 다 없으면 현재 항목 뒤에 삽입한다. 기존 활성 항목끼리 상대 순서는 바꾸지 않으며 좌표 점유를 덮어쓰지 않는다. 내부 연결 행/열 이동과 수식 조정은 표별 구조 규칙에 의해 함께 검증한다.
- 원본 행×원본 열 교차의 복원 대상 값·수식·표시 구조는 고정 원본 기준이다. 원본 행과 열이 둘 다 삭제된 상태에서 어느 쪽을 먼저 복원하더라도, 나중에 두 축이 활성화될 때 해당 교차값은 원본이다. 복원 명령 다음의 명시적 SET/CLEAR는 그 위에 적용한다. 대상 밖의 이미 활성 셀과 다른 표 변경은 유지한다.
- 추가 구조와의 교차에는 원본이 없다. 삭제 직전의 저장된 값이 안전하게 식별·재현되면 보존하고, 존재하지 않았던 교차는 표별 승인된 생성 초기값/필수값을 따른다. 값의 존재·유형을 판단할 수 없거나 필수/수식/연결/구조 보존을 입증할 수 없으면 복원은 READY가 아니며 전체 요청을 실패시킨다. 조용히 빈칸/0을 채우거나 기존 추가 구조를 삭제하지 않는다.
- 복원 대상의 가장 최근 성공 DELETE revision을 `d`, 해당 역할의 현재 성공 파일 기준 revision을 `b`로 본다. 서버는 `b < d ≤ 현재 revision` 및 해당 DELETE의 원본 ID·역할을 검증하고, `d-1`의 **동일 파일 기준** 이력과 workbook을 재생한다. 입력 추가 구조가 `d-1`에 활성인 교차는 그 시점의 typed 값과 구조를 읽는다. 이후 그 추가 구조가 삭제됐으면 복원하지 않는다. `d` 이후 생성된 추가 구조와의 교차는 표별 생성 정책이 근거다. 역사 이력이 없거나 baseline이 일치하지 않으면 실패한다. 클라이언트가 과거 revision이나 원본 파일을 선택하지 않는다.
- 원본 수식은 해당 표의 고정 원본 좌표를 현재 활성 행·열 identity에 매핑해 참조를 검증·변환한다. 기존 구조 이동에서 shared formula를 재구성하고 calc chain을 갱신하는 범위를 유지하며 array/dataTable 수식이 영향을 받고 안전한 처리기가 없으면 거부한다. 읽기 전용 수식·서식·참조의 예상 결과와 저장 결과를 동일 처리기로 검증하고, 원본 수식 원문은 API로 노출하지 않는다.
- Regas/PMI 등 연관 표는 공개 소유 대상 하나의 복원과 내부 대응 구조·값·수식의 변화 및 성공 결과를 같은 revision/transaction에서 확정한다. 기존 identity의 tombstone 구간을 닫고 동일 원본 identity의 새 위치 구간을 연다. 과거 delta와 이력은 수정하지 않는다. 구간·원본·소유·대응·유일성 검증 실패는 원자적 실패다.

#### 11.5.1 연결 및 수식 검증 단위

| 대상 | 복원 단위와 불변식 | 구현 시 대조 근거 |
| --- | --- | --- |
| Regas master 원본 행 | master와 같은 역할 기준점의 sourceRow+3 연결 행을 함께 복원. 현재 child 위치는 master+3, 둘의 활성/삭제 상태 일치. 국가 합계 블록·Helper·MAX 및 연도 열 범위를 함께 이동/계산 | `InputDraftRegasRows`, `InputWorkbookRegas`, `InputWorkbookPeriods`, 기존 Regas 네 파일 회귀 fixture |
| PMI User-Adjusted 원본 행 | 같은 baseline/source 행 번호로 대응한 Base/Selected의 기존 원본 ID 두 개를 함께 복원. 세 행의 현재 위치/활성 상태 일치. Selected 계산열과 표시·국가·내부 연결을 같이 검증 | `InputDraftPmiRows`, `InputWorkbookPmi`, 기존 PMI 행 삭제·연결 fixture |
| 기타 원본 행·기간 열 | 각 표가 이미 승인한 논리 영역에서 identity 기반 위치 매핑. 원본 헤더/기간·합계·제어 셀 및 표 범위·파생 결과를 함께 검증. 기존 ADD 초기값을 원본값으로 대신하지 않음 | `InputWorkbookCommands`의 해당 표 처리 경로 및 `InputWorkbookStructure`/`InputWorkbookFormula` |

- 공통 이동만으로 처리할 수 있다고 가정하지 않는다. shared formula master/member 분할과 상대·절대참조, calcChain의 이동/삭제/추가를 확인한다. 원본 수식은 현재 identity mapping과 표별 기존 참조 규칙을 적용하며, 허용되지 않은 외부/다른 시트 영역을 변형하지 않는다. 부정확한 수식 변환·중복 목적지·범위 초과는 실패다.
- 복원 값은 실제 변환 workbook을 다시 읽은 결과와 일치해야 한다. 명령 실행 결과로 만든 delta를 성공 transaction 전에 검증하고, 재시작/FINALIZE 시 같은 명령을 재생해 기록된 tables/rows/columns/cells와 대조한다. 최종 네 파일의 수정 대상 밖 ZIP part·매크로·형식 보존 및 승인 대상 수식 결과를 검증한다.
- 각 표의 복원 지원은 필수 수용 테스트가 통과해야 공개한다. UNSUPPORTED는 미구현 범위를 명확히 알리는 상태이며, 지원 대상 표를 미구현으로 남긴 채 전체 복원 구현을 완료했다고 판정하지 않는다.

### 11.6 구현 및 검증 경계

- 현재 `InputDraftHistory.relocateRowCells`는 삭제된 열로 이동할 수 없는 셀 값을 현재 합성 상태에서 제거한다. 따라서 추가 행×삭제 원본 열의 값은 현재 상태만 읽어 복원할 수 있다고 가정하지 않는다. 삭제 직전 성공 이력 재생 가능성과 파일 기준점은 별도 검증한다.
- `InputDraftHistory`/`InputDraftColumns`는 v2 tombstone의 임의 재활성화를 계속 거부하고, v3의 명시 RESTORE 결과에서만 검증된 원본 identity를 재활성화한다. 복원 명령이 들어간 CHANGE의 operation/revision JSON version은 3이며, 나머지 CHANGE·업로드·최종저장은 기존 2를 유지한다. 새 reader는 revision 0과 v2/v3 혼합 chain을 순서대로 읽고 v2 검증 의미를 바꾸지 않는다. v3의 payload와 데이터베이스 배포 순서는 DDL §6.14를 따른다. 구버전 앱은 v3 이력을 읽지 못하므로 reader 선배포·writer 제한 및 롤백 경계를 지킨다.
- OpenAPI에는 각 조회의 권한·revision·cursor·크기·빈 목록과 union item 예시, preview 적용 한계, 두 명령의 400/404/409/422·비동기 202 및 operation 결과를 명시한다. feature spec이 계약 정본이고 생성 OpenAPI는 동기화 대상이다.
- 의미 있는 테스트: 타입/빈값/0/정확도, 수정 후 원본값 복귀, 모든 페이지의 중복·누락 및 cursor 변조/교체/동시 저장, 큰 행·열·단일 항목 제한, 추가/삭제/빈 행, 동일 ID delete→restore→delete, 행/열 양쪽 순서와 추가 구조 교차, 연관 표·수식·최종파일, 정책 거부와 실패 원자성, 동일 requestId 재전송·재시작 replay·기존 Draft·다른 tenant/user. UI 자동저장 중 후속 편집·충돌도 검증한다.

### 11.6.1 OpenAPI 동기화 지침과 예시

- 두 조회의 summary는 `Draft 원본 대비 변경 조회`, `삭제된 원본 구조의 복원 예상 표 조회`다. description에는 호출 시점·소유권·현재 revision·baseline·pagination·실패·preview 비영속성과 자원 제한을 명시한다. `revision`은 0..9007199254740991, `startRow`는 EDT-03과 같은 Excel 행 범위, `kind`는 ROW/COLUMN enum, ID 타입과 공백/누락을 schema로 검증한다.
- 변경 목록 item은 `kind` discriminator를 가진 oneOf union으로 문서화한다. CELL/HEADER/CONTROL의 `change`는 MODIFIED, ROW/COLUMN은 ADDED 또는 DELETED다. 각 variant에 §11.2의 필드만 허용하고 다른 variant 필드는 금지한다. `baseline/current`는 EMPTY/VALUE oneOf이며 EMPTY에 value를 넣거나 VALUE에 null을 넣을 수 없다. READY/reason:null과 나머지 status/reason 조합도 예시와 검증으로 고정한다.
- preview 응답에는 `preview:true` const와 EDT-03 projection schema를 재사용한다. 200, 400, 404, 409, 422, 429 및 500을 operation별 `@ApiResponses`에 명시한다. 500은 기존 안전한 `data:null`; 나머지는 적용 가능한 기존 code와 Problem의 reason/currentRevision/issues로 표현한다. 구조 복원 SET 후속 예시는 EDT-04에 추가하고 SUCCEEDED 이전 mapping이 없음을 유지한다.

preview 200 예시(가상 표이며 실제 발행 지원을 뜻하지 않음):

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "draftId": "aefb60dc-18b8-4ac2-beb4-a4d499950765",
    "tableId": "DEMAND/example",
    "revision": 3,
    "kind": "ROW",
    "targetId": "a0000000-0000-4000-8000-000000000008",
    "preview": true,
    "tableData": {
      "tableId": "DEMAND/example",
      "columns": [
        {
          "field": "column1",
          "headerName": "B"
        }
      ],
      "rows": [
        {
          "id": 2,
          "rowNumber": 3,
          "column1": "Country"
        },
        {
          "id": 3,
          "rowNumber": 4,
          "column1": "KR"
        }
      ],
      "inputCells": [
        65537
      ],
      "inputGuideRules": [],
      "controls": [],
      "displayUpdateRules": []
    },
    "page": {
      "totalBodyRows": 1,
      "returnedBodyRows": 1,
      "startRow": 4,
      "endRow": 4,
      "hasNext": false,
      "nextStartRow": null
    },
    "editing": {
      "capabilities": {
        "SET": "READY",
        "CLEAR": "READY",
        "ADD_ROW": "INCOMPLETE",
        "DELETE_ROW": "READY",
        "ADD_COLUMN": "NOT_ALLOWED",
        "DELETE_COLUMN": "NOT_ALLOWED"
      },
      "fields": [
        {
          "fieldKey": "country",
          "columnField": "column1",
          "dataType": "TEXT",
          "policyStatus": {
            "CREATE": "INCOMPLETE",
            "SET": "READY",
            "CLEAR": "READY",
            "DELETE_COLUMN": "NOT_ALLOWED"
          },
          "setConstraints": {}
        }
      ],
      "rowBindings": [
        {
          "rowNumber": 4,
          "rowId": "a0000000-0000-4000-8000-000000000008",
          "origin": "SOURCE"
        }
      ],
      "editValues": [
        {
          "rowId": "a0000000-0000-4000-8000-000000000008",
          "values": {
            "country": "KR"
          }
        }
      ]
    }
  }
}
```

preview 409(중간에 저장 성공):

```json
{"status":false,"code":"BIZ-001","message":"The operation violates a business rule.","data":{"reason":"REVISION_CONFLICT","currentRevision":4,"issues":[]}}
```

preview 429(`Retry-After: 2`; 파일 다운로드·작업 생성 없음):

```json
{"status":false,"code":"BIZ-001","message":"The operation violates a business rule.","data":{"reason":"RESTORE_PREVIEW_BUSY","currentRevision":3,"issues":[]}}
```

저장 요청 예시(두 축이 직전 성공 상태에서 삭제돼 있고 복원 정책이 허용된 가상 표):

```json
{"requestId":"b0000000-0000-4000-8000-000000000010","baseRevision":3,"operations":[{"type":"RESTORE_ROW","commandId":"restoreRow","tableId":"DEMAND/example","rowId":"a0000000-0000-4000-8000-000000000008"},{"type":"RESTORE_COLUMN","commandId":"restoreColumn","tableId":"DEMAND/example","fieldKey":"year2030"},{"type":"SET","commandId":"editRestoredIntersection","tableId":"DEMAND/example","rowRef":{"rowId":"a0000000-0000-4000-8000-000000000008"},"fieldKey":"year2030","value":0}]}
```

복원만 성공하면 기존 operation view의 resultRevision=baseRevision+1, rowMappings/columnMappings는 빈 배열, affectedTableIds는 복원한 표와 실제 연관 표다. 같은 요청에서 별도 ADD가 있으면 그 ADD의 mapping만 기존 규칙으로 반환한다.

### 11.7 구현 준비도와 수용 기준

**API 설계 및 제품 코드 반영 완료**, 단 환경별 feature 배포·READY 공개는 별도 검증이 필요하다. 원본 복원 대상 및 기존 정책의 사용자 결정은 완료됐고, 구체 처리·검증·기능 제한은 §11.3~11.6과 DDL §6.14를 따른다. 표별 처리기가 안전하게 지원하지 않거나 아래 검증을 통과하지 못한 표는 UNSUPPORTED여야 하며 이를 모든 표 지원 완료로 표현하지 않는다.

- 최근 삭제 직전 성공 이력의 역할 파일 기준이 현재 기준과 같고 연속 이력이 유효할 때만 과거 교차값을 사용한다. 대상 축이 삭제된 동안 생긴 추가 축은 그 교차가 존재하지 않았으므로 승인된 생성 정책에 따른다. 근거 없는 기본값은 만들지 않는다. 복원 경계·값 출처 및 정책 검증에 실패하면 실패하고 사용자 데이터를 유지한다.
- preview와 실제 저장은 같은 revision·파일·동일 복원 명령에서 같은 구조·입력값·읽기 전용 표시를 생성해야 한다. 시간 의존 계산 등으로 동일성을 보장할 수 없는 표는 UNSUPPORTED다. FE가 202·preview를 저장 확정으로 오인하지 않도록 성공 결과 revision을 재조회한다.
- 원본 수식의 현재 좌표 변환, shared formula·calc chain 및 복원된 연결 행을 표별 규칙으로 처리하고 replay delta 검증과 네 파일 최종저장 검증을 통과한 표만 READY다. array/dataTable 등 안전한 변환이 없는 영역은 거부한다. 미지원 표도 읽기/기존 편집 기능은 유지한다.
- 기존 v2와 v3가 섞인 이력의 무결성과 새 포맷을 지원하지 않는 구버전의 차단·rollback, 기존 사용자의 원본/파일 교체, DB unique·FK·동시 commit 및 큰 표의 제한·시간을 검증한다. 완전한 기술 계약과 검증 조건은 구현에 필요한 설계이며 현재 실행·운영 검증을 통과했다는 뜻은 아니다.

### 11.8 설정과 수동 활성화

| 환경 변수 | 초기값 | 적용 |
| --- | --- | --- |
| `MODEL_INPUT_RESTORE_ENABLED` | `false` | 새 RESTORE 명령과 해당 READY 판정 |
| `MODEL_INPUT_RESTORE_PREVIEW_ENABLED` | `false` | preview 제공. RESTORE 접수도 함께 활성화되어야 함 |
| `MODEL_INPUT_CHANGE_CURSOR_SECRET` | 빈값 | 변경 목록 커서 HMAC 서명키. 활성화 전에 UTF-8 기준 32바이트 이상 비밀값을 모든 웹 인스턴스에 동일하게 주입 |
| `MODEL_INPUT_RESTORE_READ_MAX_CONCURRENT` | `1` | 변경 목록과 preview가 공유하는 JVM 동시 실행 상한 |
| `MODEL_INPUT_RESTORE_READ_TIMEOUT_MS` | `30000` | 요청 전체 처리 기한(ms). object storage·복호화 HTTP 및 재시도에도 남은 기한 적용 |
| `MODEL_INPUT_RESTORE_READ_SCRATCH_BYTES` | `268435456` | 요청별 임시 파일 합계 상한 |

- 기존 `CHANGE` 활성화 설정도 필요하다. 커서 키가 없거나 너무 짧으면 변경 목록을 제공하지 않는다(409 `POLICY_UNSUPPORTED`). 커서 키를 응답·로그에 노출하지 않는다. 키 교체 시 이전 커서는 400이 되므로 첫 페이지부터 재조회한다.
- 실제 작업 소비자가 모두 v2/v3 reader를 사용하도록 먼저 배포하고 복원 writer/preview는 OFF로 유지한다. 현재 웹 러너 구성은 모든 웹 서버를 선배포하며 별도 worker 프로세스 배포는 필요 없다. DBA가 [별도 증분 DDL](../model-input-draft-restore-v3-ddl-draft.sql)의 pre-check → ALTER → verify를 수행한 뒤, 기능을 켜고 격리 대상의 대표 표·부하/디스크를 검증한 후 사용 범위를 확대한다. testdb 웹 전용 실행 순서는 [배포·검증 절차](../model-input-draft-restore-testdb-runbook.md)를 따른다. 이 문서는 DB 적용이나 배포 완료의 증거가 아니다.
- v3 이력이 하나라도 생성되면 v2 전용 앱 또는 CHECK로 원복하지 않는다. 정확한 rollback 조건은 위 SQL과 DDL §6.14가 정본이다.
