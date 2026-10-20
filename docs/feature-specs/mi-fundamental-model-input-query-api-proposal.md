# MI Fundamental Model 저장 Case 입력 조회 API 상세 제안

## Document Control

- status: Draft
- canonical for: API contract proposal — 저장 Case 조회의 상세 제안과 §1.1의 사용자 확정 범위를 소유한다. 숫자 표시 정책과 JSON DRM 비적용은 기존 feature spec §3.2·§5가 정본이며 이 문서는 참조한다. 그 밖의 새 확정 사항의 공용 정본 통합은 대기한다.
- owner: Shared (BE / FE)
- source documents:
  - [기존 조회 명세](mi-fundamental-model-input-provision-feature-spec.md) §2~8
  - frontend `docs/ko/fundamental-model-sheet-api.md`, `FundamentalModelSheetDataTable.types.ts`, `sheetTemplateRegistry.ts` — 로컬 HEAD `014733c372bf75665b7b2c541657cf32711e032d`, 원격 최신성 미확인
- depends on:
  - [업무·책임 설계](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md) §1A·§6
  - [카탈로그 DDL 초안](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md)
  - [Delivery Plan](../design-docs/gas/mi-model-input-editing-delivery-plan.md) §3·§6A·§7
  - [문서 소유권](../conventions/source-of-truth-governance.md), [OpenAPI 규칙](../conventions/openapi-documentation-conventions.md), [보안 정책](../../SECURITY.md)
- supersedes: 없음
- last synced: 2026-10-19
- open questions: §9 Q2~Q6의 남은기술·검증 항목. Q2의 불변 파일 등록은 §4.4, 양식/catalog 발행 묶음·기존 DDL/Case 연결과 내부 버전 구성은 domain §3.3·DDL §6.2/§10B·이 문서 §4.6에 구체화했다. 실제 table/GET 정의·seed/발행·관계 등록·제품 연결/검증은 후속이다. 내용 hash/새 revision 컬럼을 요구하지 않는다. 숫자 표시 정책·JSON DRM 비적용과 기존 항목 JSON/월 생성/삭제 원칙은 확정됐고 제품 적용·실제 원본/catalog·이벤트/삭제/파일·FE/접근 검증은 Q3~Q6의 기존 후속이다.

이 문서는 INPUT-02A와 분리한 **상세 제안이며 §1.1의 사용자 확정 범위를 반영**한다. 문서 전체의 승인이나 INPUT-03/04의 BLOCKED 해제를 뜻하지 않는다. 앞선 확정은 이 문서와 전용 work note에만 반영했으며, 2026-10-15 사용자 승인으로 숫자 표시 정책과 JSON DRM 비적용을 기존 조회 명세 §3.2·§5에 통합했다. 나머지 공용 정본 통합 시 기존 조회 명세를 갱신하고 이 문서는 참조 이력으로 전환한다. 같은 사실의 승인 계약을 두 문서에 중복 정의하지 않는다.

## 1. 목표와 범위

접근 가능한 `caseId`를 선택한 사용자가 네 입력 파일의 표시 구조를 조회하고, 선택한 표 하나의 저장값을 읽는 호출을 구체화한다. 이번 산출물은 요청·응답 초안, 검토용 예시, 확정에 필요한 결정이다.

- 포함: 저장 Case의 구조/표 조회, 최초 표 선택, 원본 기준 일관성, 빈 값·오류 구분, 대표 응답과 구현 검증 기준.
- 제외: 출처 목록·기본 출처 선정, 단일 파일 업로드, Draft 조회/저장, 최종저장·Case 생성·Run, 계산 엔진·운영 처리 방식, 카탈로그 seed/DDL 구현.

기존 합의는 [조회 명세 §2~3.2](mi-fundamental-model-input-provision-feature-spec.md#2-fixed-decisions)를 참조한다. 아래 상세에서 사용자 확정 사항은 §1.1, 원본 고정·실제 catalog/규칙·판독 등의 남은 검증 사항은 §9로 구분한다. 기존 공통 셀/control 타입과 업무 권한은 유지하고 화면 갱신 규칙의 별도 응답 목록은 §5.4에서 추가한다.

### 현재 첫 구현 연결 — 2026-10-16

Draft 생성에 필요한 QRY-01 sourceVersion 진입점은 편집 API §1.2의 첫 구현에 포함한다. QRY-02 공개 endpoint 전체는 후속이다. source hash의 tenantKey는 인증된 회사 번호의 선행0 없는 10진 문자열을 사용한다. 발행 조회 정의/Case 연결은 DDL §6.5, 현재 코드·검증/한계는 편집 API §1.2와 stable note를 따른다. 이 변경은 표별 업무 정책/목록·seed를 확정하지 않는다.

### 1.1 사용자 확정 범위 — 2026-10-14~16

사용자는 조회 기준과 새 월 열 생성 계약에 이어 2026-10-15에 월 열 삭제 후 남은 Year/Month/Period 보존과 수식 참조 처리 원칙, 애플리케이션 원본 기준 관리 방향을 채택했다. 각 결정의 정본은 아래 연결된 절을 따른다. 실제 삭제 범위·보정 수식·계산 여부·파일/FE 검증과 기존 원본/catalog·이벤트/DB/revision 연결 등은 후속이다. 계약 원칙의 채택은 실제 삭제/연동/계산 검증 완료를 뜻하지 않는다.

| 관련 항목 | 확정된 결정 | 상세 위치 |
| --- | --- | --- |
| Q1 일부 | 최초 표 미선택 | §3.1 단계2 |
| Q3 판독 정책 | 정상 빈 결과·저장된 오류·결과 부재/판독 불가를 구분. 현재 응답 범위의 부재/판독 불가는 해당 조회 요청 실패 | §5.3·§7 |
| Q3 숫자 표시 정책 — 2026-10-15 | 기존 조회 명세 §3.2의 사용자 확정 정책을 적용. 제품 구현·승인 표시 범위 대조·HTTP/FE 검증은 후속 | §5.3의 숫자 표시 정책 참조 |
| Q6 JSON DRM — 2026-10-15 | 기존 조회 명세 §5의 사용자 확정 DRM 비적용 경계를 따름. 기존 접근/로그/자원의 제품 적용·검증은 후속 | 기존 조회 명세 §5·이 문서 §9 Q6 |
| Q5 일부 | LNG 모드 오타는 표시명만 수정하고 저장값 유지 | §5의 LNG 모드 표시 |
| Q4 조회 단위·페이지 계약 | 큰 표 본문 최대 2,000행, 작은 표 전체 조회. 2 MiB 목표를 성공 응답 상한으로 적용하고 다음 페이지·종료·초과 처리를 구체화 | §5.1·§6.6·§7 |
| Q4 FE 조회 시점 — 2026-10-16 | 표 선택 시 첫 구간 표시, 불러온 구간 아래쪽으로 스크롤할 때 남은 구간 추가 조회 | §5.1의 FE 구간 로딩 방식 |
| Q1 호출 구성 | 구조 조회와 선택 표/페이지 데이터 조회를 두 GET으로 분리 | §3 |
| Q2 생성 규칙 | tenant·Case·고정 순서의 4파일 식별/내용 버전·양식/카탈로그 기준을 묶어 동일 조합에 동일한 SHA-256 기반 sourceVersion 생성 | §4.3 |
| Q2 출처·역할 식별자 | sourceKey는 tenant·Case의 고정 키, inputId는 해당 역할의 파일·내용 버전·양식/catalog 기준을 구별하는 키 | §4.5 |
| Q2 원본 관리 방향 — 2026-10-15 | 애플리케이션이 불변 파일 등록을 기준으로 관리. 업로드마다 새 row/UUID 객체이며 등록된 경로/내용은 불변. 별도 내용 해시/재등록·버킷 특수 기능을 필수화하지 않음 | domain §3.2·DDL §6.1·§10A, 이 문서 §4.4 |
| Q1 응답 필드 | 구조/표 응답의 필수·null·생략 조건, sections 안의 inputId와 공통 표·셀 타입 공유 | §5 |
| Q5 표시·선택지·화면 규칙 기준 | 승인된 양식 version별 표시 catalog 사용, catalog 선택지와 파일 현재 값 분리, QRY-02의 displayUpdateRules 목록·논리 필수 정보·적용 순서. 실제 목록/계산식/DB·revision/FE 검증은 후속 | §5.4 |
| Q5 규칙 항목 JSON — 2026-10-15 | 필수 5필드·CellRef 3종과 해당 동작의 함께 성공/실패 기준. 이후 채택한 3연산과 순서는 다음 행을 따름 | §5.4의 항목 JSON·좌표·코드 |
| Q5 새 월 열 생성 — 2026-10-15 | NEXT_MONTH → YEAR_FROM_PREVIOUS_YEAR_AND_MONTH → INCREMENT_INTEGER 순서로 새 Month/Year/Period 생성. 세 규칙 모두 MONTHLY_COLUMN_APPENDED에 연결하는 계약. 실제 원본 대조·카탈로그/기존 이벤트 연결 구현·FE 검증·DB/revision은 후속 | §5.4의 월 열 생성 규칙 |
| Q5 월 열 삭제 원칙 — 2026-10-15 | 남은 Year/Month/Period·월/본문 연결 보존. 남은 참조는 해당 월의 새 저장 좌표에 대응하고 삭제된 월 참조는 별도 보정 대상으로 구분. 실제 범위/수식/계산/파일·FE 검증은 후속 | §5.5 |

실제 파일 내용 고정·template/catalog revision의 물리 연결·판독기 구현/표본 검증·미확인 표시 목록/선택지·수식/규칙·DB 저장 구조·FE·보안 경로의 검증까지 완료한 것으로 확대하지 않는다. 실제 내용 버전과 metadata 기준의 확보는 §4.4/Q2, 판독기와 표본 검증은 Q3, 표시/선택지/화면 규칙의 실제 대응은 Q5의 후속이다.

## 2. 확인한 코드와 FE 연결 지점

| 근거 | 이번 제안에 적용한 사실 |
| --- | --- |
| `FundamentalModelCaseController` | 기존 prefix는 `/api/v1/mi/fundamental-model/cases`. USER/SKGAS_COMPANY_USER_ONLY, ADMIN/THIRD_PARTY_ADMIN_ONLY 규칙을 사용한다. |
| `FundamentalModelCaseServiceImpl.findReadableActiveCase` | active INDEPENDENT/MI_BASE 읽기와 수정 소유권 검사는 별도다. 조회를 업로더 본인으로 제한하거나 모델 실행 완료 상태를 새 선행조건으로 추가하지 않는다. |
| `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` | SCNRCASE 관계·삭제/유효기간을 적용한 파일 조회를 재사용할 후보가 있다. 파일명 역할 검사만으로 실제 구조/파일 읽기까지 검증된 것은 아니다. |
| `BaseResponse`, `ResultCode` | `status/code/message/data`와 기존 오류 enum·실제 wire code를 사용한다. FE 제안의 별도 ApiError 형식을 도입하지 않는다. |
| FE 타입·registry | `columnN`, 원본 rowNumber, 논리 표 ID와 기존 metadata 이름을 유지한다. 현재 FE의 whole-sheet data/mock은 이 제안의 계약으로 승격하지 않는다. |
| `HttpLoggingFilter.doFilterInternal/logResponse` | 제안한 `/inputs/structure`, `/inputs/table-data`는 현재 다운로드/stream 제외 조건에 해당하지 않고 JSON 본문 로깅 경로로 들어간다. 기존 조회 명세 §5의 본문 로깅 제외가 이 경로에 적용되도록 구현 시 확인·수정이 필요하다. |

## 3. 호출 구성 — 사용자 확정

구조 조회와 선택 표/페이지 조회를 아래 두 GET operation으로 분리한다. 최초 미선택과 표별 로딩/실패를 구분하고 다음 페이지마다 구조를 재전송하지 않는다. 첫 표까지 두 호출이 필요하지만 최초 미선택 정책과 맞는다. 이 분리는 INPUT-02A의 계산 처리 방식이나 파싱 성능을 판정하지 않는다. operation별 응답 필드는 §5에서 확정했으며 FE 타입·generated OpenAPI 반영은 후속 구현/동기화다.

| 순서 | Method / path | 요청 | 성공 응답 |
| --- | --- | --- | --- |
| QRY-01 | `GET /api/v1/mi/fundamental-model/cases/{caseId}/inputs/structure` | path `caseId` | `sourceKey`, `sourceVersion`, 네 역할 `sections`, `tableData: null` |
| QRY-02 | `GET /api/v1/mi/fundamental-model/cases/{caseId}/inputs/table-data` | path `caseId`, query `sourceVersion`, `tableId`, 선택 `startRow` | `sourceKey`, 같은 `sourceVersion`, 선택 영역 `tableData`, `page` |

두 요청은 body가 없고 JSON `BaseResponse<T>`를 반환한다. 재계산·Draft/Case/Run 생성·원본 쓰기를 수행하지 않는다. 선택 표의 수식은 기존 명세대로 저장된 결과의 표시값으로 읽는다.

### 3.1 최초 진입·표 전환

1. 기존 화면 흐름에서 명시적으로 선택한 `caseId`로 QRY-01을 호출한다. current MI Base가 바뀌어도 요청 중 다른 Case로 바꾸지 않는다.
2. QRY-01 성공 시 `sections`로 탐색 구조를 표시한다. 처음에는 표를 자동 선택하지 않고 안내 상태로 둔다. `tableData: null`은 미선택이며 파싱 실패나 빈 표가 아니다.
3. 사용자가 구조 안의 표를 선택하면 QRY-01의 `sourceVersion`과 정확한 표 ID로 QRY-02를 호출한다. 첫 요청은 `startRow`를 생략한다. 표 ID에 `/`·공백이 있으므로 path 조각 대신 URL 인코딩한 query 값을 사용한다.
4. 더 읽을 때는 같은 Case·버전·표로 `page.nextStartRow`를 `startRow`에 넣는다. 마지막 응답의 `hasNext: false`에서 멈춘다. 정확한 범위·종료 의미는 §5.1을 따른다.
5. 같은 원본에서 다른 표를 선택할 때는 QRY-02만 호출한다. 참조 표 값이 따로 필요하면 해당 표를 별도로 조회하며 선택 표 payload에 다른 grid를 섞지 않는다. 연결 control은 기존 명세 §3.1B의 예외다.
6. 출처/Case가 바뀌면 이전 구조·버전·선택값을 폐기하고 QRY-01부터 시작한다. 실패를 다른 Case·template·mock으로 대체하지 않는다.

예시 요청의 식별자와 버전 문자열은 모두 설명용이다.

```http
GET /api/v1/mi/fundamental-model/cases/101/inputs/structure
GET /api/v1/mi/fundamental-model/cases/101/inputs/table-data?sourceVersion=sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f&tableId=DEMAND%2FExample%2FInput
```

### 3.2 요청 필드

| 필드 | 위치·타입 | 필수·검증 | 의미 |
| --- | --- | --- | --- |
| `caseId` | path, integer/int64 | 두 operation 필수, 양의 정수 | 원본을 선택하는 업무 식별자. 유효 숫자지만 접근 가능한 active Case가 없으면 404다. |
| `tableId` | QRY-02 query, string | 필수·non-blank. URL decoding 후 표시 카탈로그의 ID와 정확히 일치 | QRY-01의 `tables[].id`를 그대로 전달한다. 클라이언트가 만든 좌표·시트명·역할로 범위를 선택하지 않는다. |
| `sourceVersion` | QRY-02 query, string | 필수, 68자·`^sv1_[0-9a-f]{64}$`. QRY-01에서 받은 값을 그대로 전달 | 네 파일과 양식/카탈로그 해석 기준의 일관성 검사값. 생성은 §4.3, 형식 위반은 400, 현재 기준과 다르면 409다. |
| `startRow` | QRY-02 query, integer/int32 | 선택, 생략 시 첫 본문 행. 지정하면 1~1,048,576의 정수이며 해당 표의 본문 행이어야 함 | 원본 Excel의 1부터 시작하는 행 번호. 다음 조회는 서버의 `nextStartRow`를 그대로 전달한다. 헤더·본문 밖·마지막 이후 값, 본문 없는 표의 명시 값은 400이다. |

`fileNo`, `inputId`, storage key, bucket, 경로, 임의 셀 범위를 원본 선택 파라미터로 받지 않는다. `startRow`는 서버가 선택한 표 안의 본문 시작 위치만 지정하며 열·시트·원본 선택 권한을 넓히지 않는다. `pageSize`나 별도 cursor는 받지 않는다. 입력 필드는 기존 공통 validation/오류 처리로 검증하고 민감한 거부값을 오류 본문에 복사하지 않는다.

## 4. 출처·버전과 응답 적용

`sourceKey`를 버전이나 접근 권한 토큰으로 재해석하지 않는다. 동일 출처 안의 파일 연결/catalog 교체는 같은 sourceKey만 비교해서 판별할 수 없으므로 **`sourceVersion`으로 원본 조합을 대조**한다. 생성 규칙과 불변 원본 등록 전제는 확정이며 현재 등록 참조/catalogRevision의 제품 연결·검증은 Q2의 잔여 항목이다.

| 식별자 | 책임 |
| --- | --- |
| `sourceKey` | §4.5의 tenant·Case 기준 고정 키. 화면 출처 식별·응답 대조에 사용하며 파일/metadata 변경은 이 키를 바꾸지 않음. |
| `sections[].inputId` | §4.5의 역할별 원본/metadata 기준 키. 해당 역할의 파일·내용 버전 또는 양식/catalog 기준 변경을 구별. |
| `sourceVersion` | §4.3의 결정적 해시로 같은 tenant·Case의 네 역할 원본과 template/catalog 기준을 구별하는 불투명 문자열. FE는 내부 구조를 해석하지 않고 비교한다. |
| `tableId` | 양식 안에서 안정적인 논리 표. 출처 버전의 대체값이 아니다. |

### 4.1 서버의 일관성 검사

- QRY-01은 권한·active Case·4역할 완전성·등록 양식과의 대응을 확인하고, 같은 원본 기준에서 구조/버전/역할별 inputId를 구성한다. 원본 객체의 존재·구조 검증 실패를 빈 역할로 반환하지 않는다.
- QRY-02도 매번 현재 사용자의 tenant·role/condition과 Case 접근 범위, 현재 4파일·양식 기준을 확인한다. 버전이나 inputId를 알고 있다는 이유로 권한 검사를 생략하지 않는다. 같은 tenant의 공유 조회를 업로더 본인 제한으로 바꾸지 않는다.
- 형식이 맞는 `sourceVersion`이 현재 4파일/카탈로그 기준으로 계산한 값과 다르면 409다. 이전 구조와 새 파일 값을 섞지 않으며 새 버전으로 자동 대체해 성공시키지 않는다. 이 비교는 과거 발급 이력이나 접근 권한을 증명하지 않는다.
- 요청 전후 파일/카탈로그가 바뀌어 같은 기준임을 보장할 수 없는 경우도 혼합 200 대신 충돌/실패로 처리한다. 실제 객체 내용 버전까지 고정하는 방법은 §9 Q2이며 DB metadata 동일만으로 byte 불변을 주장하지 않는다.
- 생성 자체에는 별도 발급 이력 DB, cache, signed token이나 시간 기반 만료를 요구하지 않는다. 실제 원본 내용 고정·보존 방법은 Q2에서 확인하며 해시 계산만으로 그 요구가 충족됐다고 판단하지 않는다.

### 4.2 FE가 늦게 도착한 응답을 처리하는 기준

FE는 현재 선택 `caseId/sourceKey/sourceVersion/tableId`, 요청한 페이지 시작 위치와 요청 순번이 맞는 QRY-02 성공만 표시한다. A→B→A 전환에서는 식별자가 같아져도 이전 요청 응답일 수 있으므로 요청 취소/순번 검사도 유지한다. 같은 버전·표의 본문은 원본 `rowNumber`/`id`로 합치고 반복 헤더·공통 metadata를 중복 누적하지 않는다. 다음 페이지 로딩 중에는 읽은 범위만 표시하고 전체 조회 완료로 표시하지 않는다.

표 전환 중 이전 표의 값을 새 표의 값으로 표시하지 않는다. 표 조회 실패는 해당 선택의 오류 상태로 표시하고 다른 원본을 자동 적용하지 않는다. 409는 현재 구조와 그 버전으로 모은 모든 페이지를 무효화하고 **같은 Case의 구조를 다시 조회·재선택**하도록 안내한다. 404인 Case는 기존 출처 선택 흐름으로 돌아간다. 크기 초과 422는 §7처럼 현재 표의 진행을 중단한다. 그 밖의 페이지 실패도 누락 범위를 건너뛰거나 전체 조회 완료로 처리하지 않는다. 자동 재시도·폴링 API를 추가하지 않는다.

### 4.3 sourceVersion 생성 규칙 — 사용자 확정

해시는 여러 식별값을 하나의 비교용 문자열로 만든 값이다. **동일한 입력 조합이면 동일한 sourceVersion**을 생성한다. 파일 내용의 변경 여부를 해시가 스스로 관측하는 것은 아니며, 검증된 내용 버전이 입력으로 필요하다.

| 생성 입력 | 의미·결합 규칙 |
| --- | --- |
| `ruleVersion` | 현재 생성 규칙 `sv1`. 생성·직렬화 규칙의 호환성이 바뀌면 규칙 버전을 갱신 |
| `tenantKey` | 인증·서버 매핑으로 확인한 tenant 식별값. 요청 파라미터로 받지 않음 |
| `caseId` | 접근 권한을 확인한 Case. 양의 정수를 선행 0 없는 10진 문자열로 표현 |
| `files` | DEMAND → SUPPLY → TRADE → OUTPUT 고정 순서. 각 역할은 하나이며 역할 누락/중복을 먼저 거부 |
| `files[].kind/fileId/objectRef/contentRevision` | 역할, 서버가 확인한 파일 식별값, 실제 저장 객체를 구별하는 서버 참조, 그 객체의 검증된 내용 버전. 객체 버전이 같아도 다른 객체로 옮겨졌으면 구별할 수 있어야 함 |
| `templateVersion` | 이 4파일 조합을 해석하는 양식 버전 |
| `catalogRevision` | 표시 표·좌표·control·선택지·화면 규칙/표시 해석 기준을 함께 구별하는 revision. 이 내용이 바뀌면 기준도 갱신 |

내부 해시 입력은 최상위 필드를 위 표 순서로, 각 파일 항목은 `kind/fileId/objectRef/contentRevision` 순서로 놓은 공백·줄바꿈 없는 UTF-8 JSON이다. `files`만 배열이고 나머지 말단 값은 모두 non-blank 문자열이다. JSON 문자열 escaping을 적용하고 입력의 대소문자·Unicode를 임의 변환하지 않는다. 저장소 응답 목록이나 Map의 순서에 맡기지 않고 같은 고정 직렬화 규칙을 사용한다. `objectRef`는 실제 저장 객체를 구별하는 내부 값이며 임시 URL/서명 만료 시각은 넣지 않는다. 이 입력 필드들은 공개 요청/응답 DTO나 새 DB 컬럼을 정의한 것이 아니다.

```text
sourceVersion = "sv1_" + lowercaseHex(SHA-256(위 내부 JSON의 UTF-8 bytes))
```

- 결과는 소문자 hex 64자에 prefix를 붙인 68자다. 발급 시각·난수·요청 순번·사용자 ID·tableId·startRow는 생성 입력에 넣지 않는다. 같은 원본의 표 전환·다음 페이지·권한 있는 사용자 간 조회에 같은 값을 사용한다.
- QRY-01이 같은 기준의 구조와 함께 반환하고 QRY-02는 현재 기준으로 다시 계산해 요청값과 비교한다. 권한과 tenant 범위는 매 호출마다 별도로 검사한다. 해시를 권한 증명이나 서명으로 취급하지 않는다.
- 파일/객체 참조·내용 버전·양식/카탈로그 기준 중 하나가 바뀌면 조합도 바뀐다. 파일 크기·수정 시각·파일 번호만으로 내용 버전을 대신하지 않는다. source byte hash, 양식 구조 hash, 이 조합 해시는 서로 다른 책임이다.
- 페이지마다 네 파일 전체를 다시 읽어 해시하는 방식을 기본 경로로 요구하지 않는다. 이미 검증된 파일 버전 참조로 조합을 계산한다. 그 참조를 확보할 실제 고정 수단·카탈로그 revision의 물리 결합은 Q2에 남는다. 미확정 값을 빈 값/임의 상수로 채워 제품 sourceVersion을 발급하지 않는다.

### 4.4 원본 기준의 현재 코드와 채택 방향 — 2026-10-14~15

아래는 코드 관측이다. 배포된 bucket의 versioning/덮어쓰기 금지 정책이나 운영 도구의 쓰기 경로까지 검증한 결과가 아니다.

**2026-10-15 제공자 확인:** 운영 저장소는 사용자 확인에 따른 **NCP Object Storage**다. 코드의 `AmazonS3`는 사용한 SDK이며 AWS S3 운영을 뜻하지 않는다. [NCP Object Storage 공식 버킷 가이드](https://guide.ncloud-docs.com/docs/objectstorage-use-bucket)는 버전 관리를 일본 리전 제공 기능으로 구분하므로, 현재 버킷의 리전·활성화·기존 객체를 확인하기 전에 versionId 지정 읽기를 채택 가능한 기본안으로 가정하지 않는다. 별도 Ncloud Storage 문서의 지원 기능도 이 서비스의 지원 근거로 대신하지 않는다.

[NCP HeadObject](https://api.ncloud-docs.com/docs/storage-objectstorage-headobject)와 [GetObject](https://api.ncloud-docs.com/docs/storage-objectstorage-getobject)의 `if-match`는 조건부 읽기의 참고 기능이다. 이번에는 사용자 확인 불변 파일 등록을 사용하므로 ETag·버킷 버전 관리/잠금을 필수 수단이나 추가 착수 조건으로 두지 않는다.

| 코드 근거 | 확인한 사실 | 이 사실로 보장하지 못하는 것 |
| --- | --- | --- |
| [FundamentalModelCaseServiceImpl.uploadCaseFiles](../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/FundamentalModelCaseServiceImpl.java) | Case 업로드가 `FileService.uploadFileToRelationship`로 각 파일을 등록하고 반환 fileNo를 연결 | 파일 내용이 이후에도 같은 bytes라는 보장 |
| [FileService.uploadFileToRelationship/upload](../../src/main/java/com/sk/domain/shared/service/FileService.java) | 업로드마다 UUID 저장명을 만들고 `UploadedFile`을 등록. 이 Case 경로는 `fileHash`에 null 전달. `updateFile`에는 임시 파일의 저장 경로를 바꾸는 경로도 있음 | UUID/fileNo가 곧 불변 객체·내용 버전이라는 보장, 기존 hash 필드를 신뢰 가능한 서버 내용 digest로 재사용할 근거 |
| [ObjectStorageService](../../src/main/java/com/sk/domain/shared/service/ObjectStorageService.java) `uploadFile/getObject/getObjectContent/copyFile` | 쓰기는 일반 `putObject`, 읽기는 bucket/key의 `getObject`, 복사는 대상 key로 `copyObject`. 검사한 호출에서 객체 versionId 고정이나 덮어쓰기 방지 조건을 확인하지 못함 | 저장소가 versioning을 지원/사용하는지, 동일 key 쓰기가 운영에서 차단되는지, 특정 과거 bytes를 다시 읽을 수 있는지 |
| [UploadedFile](../../src/main/java/com/sk/domain/shared/entity/master/tcm/UploadedFile.java) | `fileHashVal`은 길이 20인 문자열이며 위 Case 업로드 경로에서 내용 해시 계산·객체 버전 저장을 확인하지 못함 | SHA-256 내용 digest나 저장소 객체 버전이 이미 보관된다는 주장 |

**현재 Case 업로드는 새 UUID 객체와 새 파일 row를 등록하며, 사용자는 등록된 원본 경로/내용이 바뀌지 않는다고 확인했다.** 이 불변 등록을 내용 버전으로 사용한다. 제품 조회 API의 식별자 생성/검증 연결은 미구현이며 versionId/If-Match 미사용을 이 방향의 결함이나 차단 근거로 취급하지 않는다.

**2026-10-15 사용자 확인 반영:** 원본 소유/성공 경계·기존 파일 처리는 [domain §3.2](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#32-저장-case-조회의-원본-기준-등록--2026-10-15-구체화), 기존 보관 위치와 추가 스키마/재등록 불필요 판단은 [DDL §6.1·§10A](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md#61-q2-내용-식별값의-보관-위치)가 소유한다.

이 전제에서 `contentRevision`은 바이트 hash 대신 서버가 확인한 **불변 파일 등록 버전 참조**를 사용한다.

```text
contentRevision = "file-reg-v1:" + file_no의 선행 0 없는 양의 10진 문자열
```

이는 단순 파일 번호만으로 원본 불변을 추측하는 규칙이 아니다. **새 업로드 = 새 파일 등록/UUID 객체, 등록된 객체 내용은 불변**이라는 확인된 lifecycle과 정확한 `fileId/objectRef` 대응을 함께 사용한다. 새 내용 업로드는 새 등록 버전이며 네 파일의 참조를 서버가 확인해 §4.3·§4.5의 기존 조합에 넣는다. 새 DB 컬럼이나 공개 필드를 추가하지 않고 sourceVersion/sourceKey/inputId의 생성·직렬화/형식은 유지한다.

기존 유효 4파일도 같은 등록 참조를 사용한다. 내용 해시가 없다는 이유로 미등록 409를 만들거나 전체 재해시/재등록하지 않는다. Case·역할/파일 연결·양식/catalog 변경, 객체 누락/읽기 실패는 기존 §7을 따른다. 양식/catalog 보관·표현/연결안은 §4.6·domain §3.3·DDL §6.2/§10B에 구체화했으며 실제 정의/seed/관계 등록·제품 조회 검증은 후속이다. 제품 코드·운영 DB/저장소를 변경하거나 조회 제품 완료를 선언하지 않는다.

### 4.5 sourceKey·inputId 생성 규칙 — 사용자 확정

저장 Case 조회에서는 아래 두 내부 JSON을 정해진 필드 순서로 직렬화하고, §4.3과 같은 공백·줄바꿈 없는 UTF-8 JSON·문자열 escaping·소문자 SHA-256 hex 규칙을 적용한다. 말단 값은 모두 non-blank 문자열이며 `caseId`는 §4.3의 양의 10진 문자열이다. 내부 필드명을 공개 DTO/DB 컬럼 요구로 확대하지 않는다.

| 식별자 | 내부 JSON 필드 순서 | 출력 형식 |
| --- | --- | --- |
| `sourceKey` | `ruleVersion=sc1`, `tenantKey`, `caseId` | `sc1_` + SHA-256 hex 64자. 68자·`^sc1_[0-9a-f]{64}$` |
| `inputId` | `ruleVersion=in1`, `sourceKey`, `kind`, `fileId`, `objectRef`, `contentRevision`, `templateVersion`, `catalogRevision` | `in1_` + SHA-256 hex 64자. 68자·`^in1_[0-9a-f]{64}$` |

`tenantKey`·Case·파일 참조는 인증/서버 매핑과 기존 Case 접근 검사로 확인한다. `inputId`는 전체 4파일을 넣지 않고 해당 역할의 파일 정보와 그 해석 기준을 사용한다. `fileId/objectRef/contentRevision/templateVersion/catalogRevision`의 의미는 §4.3과 같으며, `fileId`는 기존 파일 관리의 내부 식별값이다. 여기서 만드는 공개 `inputId`를 다시 `fileId`로 넣지 않는다. 생성 순서는 sourceKey 확인 → 역할별 inputId 계산이고, sourceVersion은 기존 내부 파일 조합으로 독립 계산하므로 순환하지 않는다.

- 같은 tenant·Case면 파일 교체·양식/catalog 변경·표 전환·페이지 이동에도 sourceKey는 같다. tenant나 Case가 다르면 별도 키로 구별한다. MI Base 지정이 다른 Case로 옮겨가면 새 Case의 키를 사용한다.
- 출처 목록 연동 시 저장 출처의 `sources[].key`와 이 응답의 sourceKey를 같게 맞춘다. 출처 목록 API 자체·기본 선택·EXCEL_UPLOAD 키/수명은 이번 저장 Case 조회 범위에 추가하지 않는다.
- inputId는 동일한 역할 원본/metadata 조합이면 반복 조회·다른 권한 있는 사용자·다음 페이지에서도 같다. 해당 역할의 파일/객체/내용 버전이나 양식/catalog 기준이 바뀌면 바뀐다. 공통 양식/catalog 기준이 그대로인 SUPPLY 파일 교체는 DEMAND 등 다른 역할의 inputId를 바꾸지 않는다. 공통 templateVersion/catalogRevision이 바뀌면 이를 사용하는 역할들의 inputId도 바뀐다.
- 발급 시각·난수·사용자 ID·tableId·startRow·전체 sourceVersion을 위 두 생성 입력에 추가하지 않는다. 별도 발급 이력 DB/세션은 요구하지 않는다. 두 키는 비교 식별자이며 권한이나 원본 보존을 증명하지 않는다. 조회 요청은 §3.2를 유지하고 sourceKey/inputId로 원본을 직접 선택하는 파라미터를 추가하지 않는다.
- 같은 inputId라는 이유로 409 이전 페이지를 새 sourceVersion에 자동 적용하지 않는다. §4.2의 구조/페이지 무효화와 재조회 조건은 유지한다.

**생성 규칙과 불변 파일 등록을 사용하는 방향은 확정됐고, 현재 파일 등록/객체 참조·양식/catalogRevision의 제품 연결은 남았다.** sourceKey는 확인한 tenant·Case로 구성하고 inputId/sourceVersion은 §4.4의 불변 등록 참조를 정확한 양식/catalog 기준과 결합한다. 빈 값/임의 상수·불변식이 확인되지 않은 UUID만으로 대신하지 않는다. 이 결정은 업로드 inputId의 보존/유효기간·후속 write/Run 계약을 확정하지 않는다.

### 4.6 templateVersion·catalogRevision 연결 — 기존 DDL 재사용

두 값은 §4.3·§4.5의 내부 해시 입력이며 새 공개 요청/응답 필드가 아니다. 발행/불변·Case 연결/기존 Case 정책은 [domain §3.3](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#33-q2-양식catalog-발행-묶음과-case-연결--2026-10-15-구체화), 물리 보관/등록·조회 SQL은 [DDL §6.2·§10B](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md#62-q2-templateversioncatalogrevision의-물리-연결)가 소유한다.

서버가 해당 Case에 저장한 양식 연결에서 다음을 구성한다. decimal은 DB unsigned 정수를 선행 0 없는 10진 문자열로 표현한다는 뜻이다.

```text
templateVersion = "template-v1:" + decimal(tmplt_id) + ":" + decimal(tmplt_ver_no)
catalogRevision = "catalog-v1:" + decimal(tmplt_id)
```

현재 최소안은 양식과 catalog가 같은 불변 발행 묶음을 사용한다. catalogRevision의 root ID가 실제 고정된 정의 묶음과 대응해야 하며 단순히 빈 값 대신 아무 ID를 채우는 규칙이 아니다. catalog만 수정해도 domain 규칙3대로 새 발행 묶음을 만들기 때문에 두 참조가 함께 바뀐다. 기존 Case는 원래 연결을 유지하므로 새 ACTIVE 발행만으로 기존 sourceVersion이 달라지지 않는다. 기존 Case 일괄 재연결/독립 catalog revision 기능은 추가하지 않는다.

#### 요청별 연결 순서

1. 기존 인증·tenant/role·유효 Case 접근을 검사한다. Case 없음/삭제/비대상과 양식 관계 없음/해석 불가를 구별해 기존 §7의 404/409를 유지한다.
2. 현재 네 불변 파일/객체 참조와 Case에 저장된 양식 연결을 읽는다. 연결된 ACTIVE 또는 지원 가능한 DEPRECATED의 실제 네 파일 대응·요청 표/규칙의 소속을 확인한다. application model-code 설정 없이 Case 연결을 사용하며 모델 코드가 다르다는 이유로 거절하지 않는다. GET에서 관계를 INSERT하거나 현재 ACTIVE/MAX를 임의로 연결하지 않는다.
3. 위 두 값과 §4.4의 contentRevision으로 기존 고정 직렬화/해시를 생성한다. QRY-01은 이 기준의 구조를 반환하고 QRY-02는 요청 sourceVersion과 대조한다. 서로 다른 양식/catalog의 좌표/선택지/규칙을 같은 응답에 섞지 않는다.
4. 현재 선택 응답을 읽은 뒤 기준이 유지되는지 확인한다. 긴 파일 읽기 동안 DB lock을 유지하는 것을 기본안으로 삼지 않으며 최종 대조가 실제 현재 DB 상태를 읽는지 제품 검증한다. 바뀐 기준/누락/읽기 실패는 기존 실패 계약을 따르고 새 기준으로 자동 성공시키지 않는다.

필요한 조회 정의/선택지/규칙·버전별 표시 해석이 없으면 root ID만으로 성공시키지 않는다. 지금의 6개 table 초안에 아직 없는 정의 저장/seed는 Q5·기존 DDL 후속이다. 실제 Case-template 등록과 제품 projection/HTTP/FE 검증도 미완료이며 이 표현 결정으로 INPUT 상태를 변경하지 않는다. 파일 내용 hash/재등록·별도 revision 컬럼·공개 DTO 필드를 추가하지 않는다.

## 5. operation별 응답 필드 — 사용자 확정

공통 `BaseResponse`의 `status/code/message/data`를 유지한다. 성공은 `status: true`와 non-null data, 실패는 `status: false`와 `data: null`이며 §7의 오류를 따른다. 성공 data는 다음 operation별 필드로 구성한다. 응답 타입은 operation별로 구분하고 표/셀/control의 공통 타입은 공유한다. 공통 셀/metadata/control 정의는 기존 조회 명세 §3.1~3.2를 참조하고 이번에 승인한 화면 갱신 규칙 목록은 §5.4를 따른다.

| 경로 | QRY-01 | QRY-02 |
| --- | --- | --- |
| `data.sourceKey` | required, §4.5 형식의 non-null string | required, 같은 Case의 같은 키 |
| `data.sourceVersion` | required, §4.3 형식의 non-null string | required, 요청 기준과 같은 string |
| `data.sections` | required, 네 역할을 고정 순서로 각각 하나 | 반환하지 않음. QRY-01의 같은 버전 구조를 사용 |
| `data.sections[].kind` | required, 각 section의 역할 | sections 미반환 |
| `data.sections[].inputId` | required, §4.5 형식의 non-null string | sections 미반환 |
| `data.sections[].sheets` | required array | sections 미반환 |
| `data.tableData` | required, 항상 null | required, non-null object |
| `data.tableData.tableId` | 해당 없음 | required, 요청 ID 및 구조 안 ID와 일치 |
| `data.tableData.columns/rows/inputCells/inputGuideRules/controls` | 해당 없음 | 각 필드 required array, 항목이 없으면 `[]` |
| `data.tableData.displayUpdateRules` | 해당 없음. 구조에는 규칙 목록을 넣지 않음 | required, non-null array. §5.4의 선택 표 화면 갱신 규칙. 해당 규칙이 없다고 확인된 경우만 `[]`; 미확정/미판독 대체값이 아님 |
| `data.page` | 반환하지 않음 | required, non-null object. 작은 표·빈 본문·control 전용에도 §5.1의 같은 필드 제공 |
| `data.inputId` | 반환하지 않음. inputId는 sections 안에만 존재 | 반환하지 않음. 같은 버전 구조의 표 소속을 사용 |

생략하는 필드는 null이나 빈 배열 placeholder로 보내지 않는다. 구조의 `tableData: null`은 최초 미선택이고 표 응답의 빈 rows는 선택 표의 데이터 상태다. QRY-02에 sections/inputId를 중복하지 않는 대신 §4의 버전 검사와 같은 구조의 표 소속 검사를 유지한다. 이 필드 계약의 확정이 FE 타입·제품 DTO·generated OpenAPI의 구현/동기화 완료를 뜻하지는 않는다.

| 관측 | 응답·화면 의미 |
| --- | --- |
| 최초 구조 조회 후 표 미선택 | `tableData: null`; 표를 선택하라는 상태 |
| 카탈로그상 한 역할에 표시할 시트가 없음 | 해당 section은 유지하고 `sheets: []`. 원본 파일 존재/완전성 실패와 구분 |
| 존재하는 표에 본문 값이 없음 | 200. 필요한 헤더·표시 열·빈 편집 위치는 유지. 값 있는 셀만 골라 표 자체를 없애지 않음 |
| grid 빈 셀 | 기존 명세대로 해당 `columnN` 필드를 생략할 수 있음. columns와 빈 입력 inputCells는 유지하며 null·0·빈 문자열을 생성해 대체하지 않음 |
| grid 숫자 0 또는 빈 문자열 저장값 | 실제 저장된 표시값을 유지. 빈 셀과 임의로 합치지 않음 |
| 읽기 전용 표 | 표시값 제공, `inputCells: []`. 입력칸이 없다고 조회 값까지 제외하지 않음 |
| control만 있는 영역 | grid 4배열은 `[]`, controls는 실제 연결 control. 가짜 헤더/본문을 만들지 않음 |
| control 미선택/선택지 밖 기존 값 | value null 또는 기존 값 유지. 첫 선택지로 자동 대체하지 않음 |
| 선택 표에 화면 갱신 규칙이 없다고 확인됨 | displayUpdateRules: []. 규칙 미확정/미지원은 이 정상 빈 목록과 구별하며 §5.4·Q5를 따름 |
| 저장된 수식 오류 표시값 | 원본의 표시값을 읽고 재계산/정정하지 않음. 기존 오류 표시값과 계산 결과 부재를 구별 |
| source 읽기 실패·필수 sheet 누락·알 수 없는 template | 빈 성공이 아닌 §7 오류. 표가 비었다는 상태로 숨기지 않음 |

**수식 결과 처리 — 사용자 확정:** §5.3의 결과 존재·타입을 먼저 분류한 뒤 표시 서식을 적용한다. 정상 빈 결과와 저장된 오류는 성공으로 유지하고, 결과 부재/판독 불가는 현재 응답 범위의 요청을 실패로 처리한다. 빈칸·0·수식 원문 대체나 임의 재계산은 하지 않는다. 실제 판독기 구현과 표본 검증은 §9 Q3의 후속이다.

**LNG 모드 표시 — 사용자 확정:** 원본 선택지 저장값이 `Dynamaic`이면 화면 표시명 `options[].label`만 `Dynamic`으로 제공한다. `options[].value`와 현재 `controls[].value`는 원본 저장값을 유지한다. 예를 들어 선택지는 `{"value":"Dynamaic","label":"Dynamic"}`으로 표현한다. 표시명 변경을 이유로 원본 셀·저장 파일·향후 저장 요청값을 `Dynamic`으로 정규화하지 않는다. 이 결정은 다른 선택지의 원본 값/표시명을 임의로 변경하는 허용이 아니며 실제 선택지 목록과 catalog 대응 검증은 §9 Q5에 남긴다.

### 5.1 조회 단위·다음 페이지·크기 초과 계약

**본문은 한 응답에 최대 2,000행을 반환한다.** 본문이 2,000행 이하이고 아래 크기 예산 안인 작은 표는 첫 요청에서 전체를 반환한다. 넓은 표는 행 수가 적어도 크기 때문에 나눌 수 있다. 화면에서 선택한 논리 표 하나라는 의미와 원본 좌표는 유지한다.

#### 본문 범위와 반복 항목

- 본문 행 목록은 해당 버전의 표시 카탈로그가 정한 행 번호를 중복 없이 오름차순으로 나열한 것이다. 헤더/기간 행과 control 전용 셀은 본문 행 수에 포함하지 않는다. 값이 비어 있어도 표시/편집 대상으로 정한 본문 행은 유지한다. 시트 최대 행이나 값 있는 행 수로 대체하지 않는다.
- `startRow` 생략 시 첫 본문 행부터, 지정 시 그 본문 행부터 최대 2,000개를 순서대로 반환한다. 범위 안에서 빈 행·실패한 행을 건너뛰지 않는다. 원본 본문이 떨어진 범위라면 그 목록 순서를 따르므로 실제 행 번호 사이에 간격이 있을 수 있다.
- 각 페이지는 같은 표시 열, 필요한 헤더/기간 행과 연결 control을 포함한다. `rows`는 반복 헤더/기간 행과 이번 본문만, `inputCells`는 반환한 행의 편집 위치만 포함한다. `displayUpdateRules`의 표 공통 규칙은 각 페이지에 유지하고 행별 규칙은 이번 반환 행에 대응한다. §5.4의 배열 순서와 원본 좌표를 유지하며 구체 항목 표현은 Q5에서 맞춘다. 행·열 번호를 페이지 안의 번호로 다시 매기지 않는다.

#### `data.page` — QRY-02의 필수 metadata

모든 필드를 항상 포함한다. 행 수는 integer/int32, 행 번호는 integer/int32 또는 명시적 null, `hasNext`는 boolean이다.

| 필드 | 의미·불변 조건 |
| --- | --- |
| `totalBodyRows` | 이 버전·표의 전체 본문 행 수, 0 이상. 같은 `sourceVersion`에서는 변하지 않음 |
| `returnedBodyRows` | 이번 본문 행 수, 0~2,000. 반복 헤더/기간 행은 제외. 본문이 남아 있으면 성공 응답은 1행 이상이어야 함 |
| `startRow` | 실제 반환한 첫 본문 행 번호. 요청에서 생략했어도 응답은 실제 번호. 본문이 없는 표만 null |
| `endRow` | 실제 반환한 마지막 본문 행 번호, 양 끝 포함. 본문이 없는 표만 null |
| `hasNext` | 이번 마지막 본문 뒤에 아직 본문 행이 있으면 true, 아니면 false |
| `nextStartRow` | 바로 다음 본문 행 번호. `hasNext: true`일 때만 non-null이며 `endRow`보다 큼. false이면 null |

다음 요청은 같은 `caseId/sourceVersion/tableId`에 `startRow=nextStartRow`를 사용한다. FE가 `startRow + 2000`을 계산하지 않는다. 크기 예산으로 반환 행 수가 줄거나 원본 행에 간격이 있어도 서버가 알려준 다음 행으로 이어간다.

작은 표의 전체 응답과 마지막 페이지는 `hasNext: false`, `nextStartRow: null`이다. 본문이 없는 표/control 전용 영역은 본문 수 두 필드가 0, 시작/끝/다음 행은 null, `hasNext: false`이며 필요한 헤더/control은 유지한다. **값이 모두 빈 본문 행**은 본문 없는 표가 아니므로 행 수에 포함한다. 마지막 이후를 확인하는 추가 요청은 보내지 않는다. 명시한 `startRow`가 본문에 없으면 400이며 빈 200으로 종료를 표현하지 않는다.

`hasNext: false`는 해당 위치 뒤의 종료만 뜻한다. FE는 첫 요청에서 시작해 모든 `nextStartRow`를 빠짐없이 연결하고 누적 본문 행 수가 `totalBodyRows`와 일치한 경우에만 표 전체 조회 완료로 표시한다. 중간부터 조회한 마지막 페이지, 실패 뒤 남은 일부 페이지를 전체 완료로 취급하지 않는다.

#### FE 구간 로딩 방식 — 2026-10-16 사용자 확정

FE는 **표 선택 시 첫 구간을 표시하고, 스크롤할 때 남은 구간을 추가 조회**한다. 여기서 구간/페이지는 같은 표의 행을 나눠 받는 API 단위다. 화면 페이지 전환을 뜻하지 않는다.

- 표를 선택하면 `startRow`를 생략한 첫 요청을 보내고, 성공한 첫 구간부터 표시한다. 첫 응답에 전체가 들어오면 추가 요청 없이 표시를 끝낸다.
- `hasNext: true`이고 사용자가 불러온 구간 아래쪽으로 스크롤하면, 서버의 `nextStartRow`를 사용해 다음 구간을 순서대로 조회·이어 표시한다. 표 선택만으로 모든 구간을 자동 연속 조회하지 않는다. 같은 다음 구간의 중복 스크롤 이벤트로 요청/행을 중복 누적하지 않는다.
- 추가 조회 중에는 이미 받은 행을 계속 표시하고 나머지는 미조회 범위로 둔다. `hasNext: false`가 되면 더 요청하지 않으며, 표 전체 조회 완료 판단은 위의 연속 구간/전체 행 수 조건을 그대로 적용한다.
- 표/버전이 바뀐 뒤 늦게 도착한 응답, 구간 병합·반복 헤더/control 중복 제거, 추가 조회 실패는 §4.2·§7을 따른다. 실패 구간을 건너뛰거나 일부 조회를 전체 완료로 표시하지 않는다. 자동 재시도/폴링을 추가하지 않는다.

이 방식은 원본 QRY-02와, 이 페이지 계약을 재사용하는 Draft EDT-03에 동일하게 적용한다. Draft는 해당 API의 `draftId/revision` 대조를 유지한다. 기존 endpoint/DTO·크기 상한·읽기/실패 계약은 바꾸지 않는다. **조회 방식의 채택이며 FE 코드 구현/화면 검증 완료를 뜻하지 않는다.**

#### 2 MiB 목표와 초과 처리

QRY-02 성공 응답은 **압축 전 UTF-8 JSON 본문 전체가 2 MiB(2,097,152 bytes) 이하**가 되도록 한다. `BaseResponse`·`data.page`·헤더·열·control·inputGuideRules·displayUpdateRules까지 포함하고 HTTP header/TLS·압축 후 크기는 포함하지 않는다. 이 예산은 QRY-01 구조 응답의 상한을 정의하지 않는다.

1. 최대 2,000행 또는 남은 전체 본문으로 구성한 응답이 예산 안이면 그대로 반환한다. 정확히 2,097,152 bytes도 허용한다.
2. 초과하면 해당 시작점부터 **행을 쪼개지 않은 앞부분**으로 본문 행 수를 줄인다. 최종 metadata까지 직렬화한 실제 bytes가 상한 이내인 것을 확인한 뒤 200을 보낸다. `returnedBodyRows/endRow/hasNext/nextStartRow`도 줄어든 실제 범위로 구성한다. 본문이 남으면 최소 1행을 반환해야 하며 0행 성공으로 같은 시작점을 반복하지 않는다.
3. 필요한 metadata/헤더/control/규칙과 본문 1행만으로도 초과하면 **422 / `BIZ_RULE_VIOLATION` / `BIZ-001`**, `data: null`로 해당 요청을 실패시킨다. 본문 없는 표는 필요한 헤더/control 등만으로 초과해도 같은 오류다. 최소 응답을 줄일 수 없음을 뜻하며 §7의 복구 동작을 따른다.

크기를 맞추려고 열·필수 규칙·헤더·빈 편집 위치를 빼거나 문자열/셀/행을 자르지 않는다. JSON 전송을 시작한 뒤 잘라 200을 끝내지 않으며 오류 응답에 일부 `tableData`를 섞지 않는다. 판독 실패 행을 건너뛰는 수단으로 크기 조정을 쓰지 않는다. 선택 밖 본문 전체를 메모리에 보유한 뒤 잘라 반환하는 방식으로 분할의 메모리 절감을 대체하지 않는다. 구현 계층이나 크기 판정 알고리즘 자체는 지정하지 않는다.

### 5.2 채택 근거와 검증 한계 — 2026-10-14

측정 환경·표시 projection·66회 서버 구간/18회 JavaScript 결과·재현 명령과 한계는 [전용 작업 노트의 실측 기록](../work-notes/2026/2026-10-14-model-input-query-api-design-preparation.md#large-table-benchmark)이 소유한다. 조회 단위와 크기/페이지 계약의 소유 절은 §5.1이다.

- DEMAND `Power Generation Data`, `RE Base Case Gas Demand Data by Sector`, `Car Sales for Electrification Input`은 전체 응답이 각각 약 22.35/5.65/2.38 MiB여서 분할 근거가 있다. 2,000행 구간은 약 0.34~0.76 MiB였다. 5,000행 후보는 열이 많은 표에서 2 MiB에 가까워 채택 단위보다 여유가 적다.
- SUPPLY `RegasAdjustment` 표시 후보는 372행·약 0.79 MiB로 전체 조회 범위에 든다. 미확인 표까지 표시 목록을 확정한 것은 아니며 실제 catalog/규칙/열로 예산을 다시 확인해야 한다.
- XML을 순서대로 읽는 경로는 뒤쪽 분할에서도 앞 행을 지나가는 시간이 남는다. 실측은 응답 크기·선택 데이터 메모리 절감의 근거이며 전체 표를 끝까지 탐색하는 누적 시간이 전체 1회 조회보다 짧다는 증거는 아니다. 별도 색인/cache/worker를 필수 구현으로 추가하지 않는다.

기존 실측은 당시 측정용 window metadata를 사용했다. 이번 `page` 계약과 초과 처리를 구현·재측정한 결과가 아니다. 로컬 파일/JSON 구간 결과를 object storage·DRM·HTTP·브라우저 grid 렌더링·운영 동시 요청의 성능 보장으로 사용하지 않는다.

### 5.3 수식 저장값 판독 기준 — 사용자 확정

cache는 수식과 함께 파일에 저장된 계산 결과를 뜻한다. 판독 순서는 **수식 표시·결과의 존재·저장 타입/내용 판독 → 표시 서식 적용**이다. 화면의 빈칸이나 formatter가 반환한 빈 문자열만으로 결과 부재를 판단하지 않는다.

저장 형식의 기술 근거는 OpenXML의 [CellValue](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.spreadsheet.cellvalue?view=openxml-3.0.1), [CellValues](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.spreadsheet.cellvalues?view=openxml-3.0.1), [CellFormula](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.spreadsheet.cellformula?view=openxml-3.0.1) 정의다. `<f>`는 수식 표시, `<v>`는 저장된 결과, 셀의 `t`는 값 타입이다. 아래 분류/반환/실패 동작은 이 저장 형식을 바탕으로 정한 조회 계약이다.

| 저장 상태 | 판정 | 조회 결과 |
| --- | --- | --- |
| 수식 없는 일반 빈 셀. 셀 자체 또는 값이 없음 | 일반 빈 셀 | 기존 계약대로 grid 값 필드 생략 가능. 필요한 행·열·빈 편집 위치 유지 |
| 수식 셀에서 문자열 결과가 명시적으로 비어 있음. 대표적으로 `t="str"`이고 `<v/>` 또는 `<v></v>` 존재 | 정상 빈 결과 | 200. grid 값 `""` 유지. 일반 빈 셀처럼 임의 생략/0/null 대체하지 않음 |
| 유효한 숫자·문자·논리값 등 저장 결과 존재 | 정상 결과 | 200. 판독 후 기존 표시 서식 적용. 0/FALSE를 결과 부재로 취급하지 않음 |
| `t="e"`이고 유효한 오류값이 저장됨 | 저장된 오류 | 200. `#N/A`, `#DIV/0!` 등 오류 표시값 유지. 공백/null/정상 값으로 보정하지 않음 |
| 수식 표시가 있지만 `<v>`가 없음. `t="str"`도 포함 | 계산 결과 부재 | 현재 요청 500 / SYS_INTERNAL_ERROR / `SYS-001`, `data: null` |
| 숫자(`t` 생략/`n`)·논리·오류 타입인데 결과가 비어 있거나 유효하지 않음. 필요한 문자열 참조를 해석할 수 없는 경우 포함 | 판독 불가 | 현재 요청 500 / SYS_INTERNAL_ERROR / `SYS-001`, `data: null` |

- `<v/>`의 존재와 `<v>` 자체의 부재를 보존해 구분한다. 문자열 타입 표시만으로 정상 빈 결과를 추정하지 않는다. 수식 내용이 `=""`여도 계산하거나 분석해 빈 결과를 만들어주지 않는다.
- 수식 유무는 수식 본문 텍스트의 길이만으로 판단하지 않는다. 공유수식처럼 `<f>` 본문이 비어 있는 표현도 해당 저장 결과를 확인해야 한다. 지원 형식의 실제 판독은 Q3에서 검증한다.
- 문자열 공백을 임의 trim하지 않는다. `" "`와 `""`를 구분하고, 문자열 타입의 `"#N/A"`는 오류 타입과 구별해 판독한다. 다른 문자열 저장 형식은 유효한 참조를 판독한 뒤 같은 빈 결과 정책을 적용한다.
- 기존 오류는 **이번에 선택한 원본에 이미 저장된 오류**를 뜻한다. 조회가 새 계산 오류인지/업무상 허용되는지 판정하지 않는다. 결과 판독 성공도 계산의 정답·최신성을 보장하지 않는다. 계산 정확성·갱신/재계산 범위는 해당 검증 경로가 소유한다.

검사는 현재 응답에 포함할 본문·반복 헤더·연결 control의 값 범위에 적용한다. 결과 부재/판독 불가를 발견하면 일부 tableData를 섞은 200을 보내지 않는다. 행을 건너뛰거나 반환 행 수를 줄여 실패를 숨기지 않는다. 먼저 읽은 페이지도 전체 완료로 표시하지 않으며 §4.2의 부분 조회/실패 처리를 유지한다. 다른 표나 표시 제외 범위까지 일괄 실패시키지 않고, 아직 읽지 않은 페이지의 정상 여부를 보장하지 않는다. 기존 권한/원본/구조 검사와 공통 타입은 유지하고 새 셀 상태 응답 필드나 계산 API를 추가하지 않는다.

구현 인계 시 [XlsmStreamMerger.normalizeCellValue](../../src/main/java/com/sk/domain/mi/service/nextgen/paramproposal/excel/XlsmStreamMerger.java)의 병합용 규칙을 조회에 그대로 적용하지 않는다. 이 코드는 결과 없는 숫자 수식을 0.0으로, 오류를 null로 바꾸고 문자열을 trim해 빈 결과 구분을 잃는다. 이번 작업은 그 코드를 변경하지 않는다. **제품 판독기 구현과 제품 경로의 합성 XML/실파일·HTTP/FE 검증은 후속 Q3**다. 기존 [Q3 독립 검증](../work-notes/2026/2026-10-15-model-input-query-q3-stored-value-verification.md)은 재사용하되 독립 도구·아래 응답 예시/문서 검사·과거 성능 실측을 제품 완료 증거로 대신하지 않는다.

#### 숫자 표시 정책 — 2026-10-15 사용자 확정

존재·타입 판독 뒤 적용할 저장 Case 숫자 표시의 규칙·지원 범위·FE 책임은 [기존 조회 명세 §3.2의 숫자 표시 정책](mi-fundamental-model-input-provision-feature-spec.md#32-단일-표-값의-경계)이 소유한다. 이 표시 정책의 채택은 완료됐으며 판독/실패 계약을 다시 결정하지 않는다. 제품 reader/응답/FE의 실제 적용과 승인 catalog에 필요한 타입·서식·숫자 범위 대조는 아래 Q3 검증에 남긴다.

### 5.4 표시 카탈로그·control 선택지·화면 갱신 규칙 — 사용자 확정

#### 표시 카탈로그와 선택지의 기준

- 표시 카탈로그는 양식 version별로 승인한 시트·표·표시 순서·헤더/기간 행·본문 행·표시 열과 연결 control을 정의한다. QRY-01 구조와 QRY-02 값/페이지는 같은 기준을 사용한다. 실행 시 색·값 유무·수식 탐지로 표시 범위를 다시 추측하지 않는다. 기존 hidden/development 제외와 표별 승인 표시 정책을 따른다.
- 표시 범위와 편집 가능 범위를 구분한다. 읽기 전용 값도 표시에 필요하면 제공하고, 빈 값이 있는 등록 본문 행·필요한 열·빈 편집 위치도 유지한다. `inputCells`와 control `editable`은 기존 BE의 확정 편집 정책을 따른다.
- 필요한 표시 범위/선택지/규칙이 미확정인 catalog를 정상 빈 목록으로 바꾸거나 미확정 표를 조용히 누락하지 않는다. 실제 조회 시 등록 양식/해석 정책이 없어 안전하게 표시할 수 없으면 §7의 409를 따른다. 승인 목록에 표시 시트가 없는 경우의 `sheets: []`와 구분한다. 실제 목록과 원본 대응 확인은 Q5에 남긴다.
- control의 `id/type/label/position/sourceTableId/binding/editable`과 선택지 `options`는 확인·등록한 양식 version별 catalog가, 현재 `value`는 조회 대상 파일의 binding 셀이 소유한다. 선택지는 원본에서 저장값·string/number 타입·순서를 확인해 등록하고 순서를 유지하며 저장값을 중복하지 않는다. 현재 값에 맞춰 선택지를 임의로 생성하지 않는다.
- 미선택 `null`·선택지 밖 기존 값 유지·LNG 오타의 label/value 분리는 §5와 기존 control 계약을 따른다. FE가 선택지를 하드코딩해 보충하거나 첫 값으로 자동 대체하지 않는다. 동일 source/template 및 같은 sheet 안의 명시적 `controlIds/sourceTableId` 연결만 공유하고 값 셀을 grid에 중복하지 않는다.
- 다른 셀에 따라 선택지가 달라지는 사례는 의존 관계와 허용 목록을 Q5에서 별도로 확정한다. 고정 선택지로 임의 치환하거나 드롭다운 원본식을 조회 시 실행하는 근거가 아니다.

#### `tableData.displayUpdateRules`와 적용 경계

QRY-02의 모든 성공 응답에 **별도 `displayUpdateRules` 배열**을 추가한다. 일반 표·읽기 전용 표·control 전용 영역에도 필수이며 실제로 해당 규칙이 없다고 확인된 경우만 `[]`다. QRY-01은 기존 `tableData: null`을 유지하고 목록을 중복하지 않는다. `inputGuideRules`는 안내 metadata 역할을 유지한다. 두 목록은 의미가 다르며 미확정 규칙을 빈 배열로 숨기지 않는다.

**항목 JSON — 2026-10-15 사용자 확정:** 각 항목은 다음 5필드를 모두 required/non-null로 제공한다. 하위 객체도 아래 타입에 명시된 필드로 구성하고 임의 expression/JSON 확장 필드를 두지 않는다. 실제 표의 binding·원본/FE 대응 확인은 Q5의 후속이다.

| 필드 | 타입·검증 | 의미 |
| --- | --- | --- |
| `id` | non-blank string. 같은 catalog 안에서 유일 | 같은 논리 규칙의 안정적인 ID. 페이지 반복 규칙을 식별하며 내용 버전은 바깥 sourceVersion으로 구별 |
| `trigger` | object. 필수 `type`: string enum, 현재 `MONTHLY_COLUMN_APPENDED` | 발동할 사용자 동작. 조회/페이지 수신은 발동 동작이 아님 |
| `inputs` | array of CellRef. NEXT_MONTH/INCREMENT_INTEGER는 정확히 1개, YEAR_FROM_PREVIOUS_YEAR_AND_MONTH는 정확히 2개 | 연산별 입력 개수와 순서는 아래 코드 표로 고정 |
| `target` | CellRef object, 셀 1개 | 표시값 갱신 대상. 직접 수정 권한은 기존 inputCells/control editable, 구조 변경 허용은 기존 표별 정책을 따름 |
| `operation` | object. 필수 `code`: string enum, `NEXT_MONTH`, `YEAR_FROM_PREVIOUS_YEAR_AND_MONTH`, `INCREMENT_INTEGER` | 지원 연산을 식별. 세 코드 모두 추가 인자 없음 |

선택 표·원본 버전·적용 순서는 바깥 `tableData.tableId`·`sourceVersion`과 displayUpdateRules 배열 순서를 사용한다. 항목마다 tableId/sheetName/sourceVersion/order를 중복하지 않는다. 이 JSON은 BE catalog가 제공하는 응답 metadata이며 클라이언트가 임의 좌표를 조회/쓰기 요청으로 제출하는 계약이 아니다.

**CellRef — 좌표 3종:** `type`으로 형태를 구분한다. `rowNumber`는 integer/int32, 1~1,048,576이고 `columnIndex`는 integer/int32, 0~16,383이다. 페이지/표 내부 순번으로 재번호화하지 않으며 Excel 열 문자열을 함께 중복하지 않는다.

| `type` | 필요한 필드 | 의미 |
| --- | --- | --- |
| `CELL` | `type`, `rowNumber`, `columnIndex` | 같은 선택 표의 원본 시트 고정 셀 |
| `PREVIOUS_LAST_MONTHLY_COLUMN_CELL` | `type`, `rowNumber` | 월 열 추가 직전, 현재 남아 있는 월 열 목록의 마지막 열에 있는 셀 |
| `APPENDED_COLUMN_CELL` | `type`, `rowNumber` | 이번 동작으로 새로 추가한 열에 있는 셀 |

동작 기준 좌표 2종에는 `columnIndex`를 미리 부여하지 않는다. 추가 직전의 마지막 월 열과 해당 동작의 새 열 식별자에 연결한다. 마지막 열이 없으면 원본 마지막 열로 자동 대체하지 않고 해당 동작을 실패시킨다. 연속 추가에서는 앞선 추가가 성공한 상태를 다음 동작의 직전 상태로 사용한다. 실제 XLSM 저장 위치로의 변환은 후속 write 계약이 소유한다. CellRef가 catalog의 표시/규칙 범위나 기존 편집/구조 권한을 넓히지 않는다.

**월 열 생성 규칙 — 2026-10-15 사용자 확정:** `MONTHLY_COLUMN_APPENDED`는 선택 표 끝에 월 열 1개를 추가하는 동작이다. 아래 세 규칙을 모두 같은 동작에 연결하고 **Month → Year → Period 순서**로 전달/적용한다. 필수 5필드와 CellRef 3종은 유지한다. 이 연결은 API/catalog의 전달 계약이며 실제 catalog 등록·기존 FE 이벤트에 규칙을 연결하는 구현과 검증은 Q5의 후속이다.

| 순서·연산 코드 | inputs의 고정 순서·개수 | target | 표시값 생성 방법 |
| --- | --- | --- | --- |
| 1. `NEXT_MONTH` | [이전 마지막 월 열의 Month] 1개 | 새 월 열의 Month | 이전 Month가 12이면 1, 나머지는 +1 |
| 2. `YEAR_FROM_PREVIOUS_YEAR_AND_MONTH` | [이전 마지막 월 열의 Year, 방금 생성한 새 Month] 2개 | 새 월 열의 Year | 새 Month가 1이면 이전 Year +1, 나머지는 이전 Year 유지 |
| 3. `INCREMENT_INTEGER` | [이전 마지막 월 열의 Period] 1개 | 새 월 열의 Period | 이전 Period +1 |

기존 열 참조는 `PREVIOUS_LAST_MONTHLY_COLUMN_CELL`, 새 열의 입력/대상은 `APPENDED_COLUMN_CELL`로 표현한다. Year 규칙의 두 번째 입력은 같은 동작에서 앞선 Month 규칙이 생성한 값을 읽는다. 새 열의 값이 아직 생성되지 않았으면 원본 값/0으로 대체하지 않는다. 이전 열의 앵커는 추가 직전 상태로 고정하고, 여러 번 추가할 때는 앞선 동작의 성공 상태가 다음 동작의 기준이 된다. 고정 CELL이나 다른 발동/연산 조합의 실제 적용은 추가 업무 규칙 확인 후 정한다.

- 입력은 JSON 정수 또는 `^-?[0-9]+$`에 맞는 10진 정수 문자열이며 결과는 모두 **10진 문자열**이다. 공백/쉼표·소수/지수 표기 문자열·boolean·null·빈 문자열·저장 오류는 유효 입력이 아니다. 입력과 생성 결과 모두 JavaScript에서 정확히 표현 가능한 정수 범위(-9,007,199,254,740,991~9,007,199,254,740,991) 안이어야 한다. Month 입력/결과는 추가로 1~12 범위를 만족해야 한다. 값이나 타입을 임의 trim/0/빈 값으로 보정하지 않는다.
- 알 수 없는 발동/좌표/연산 코드, 잘못된 좌표/입력 개수·순서, 참조 부재·유효하지 않은 값·월/정수 범위 초과가 있으면 해당 추가 동작과 관련 규칙 적용을 완료하지 않고 이유를 표시한다. 열 추가와 **세 규칙 전체는 함께 성공하거나 실패**해야 하며 일부만 반영하지 않는다. 규칙의 존재만으로 실제 표의 열 추가를 허용하지 않는다.
- 이 실패 기준은 FE의 해당 동작에 대한 계약이다. GET 조회의 정상 빈 결과/저장 오류 반환은 §5.3대로 유지하며 새로운 조회 HTTP 오류를 정의하지 않는다. 실제 FE 실행/검증을 이번 문서 작업에서 수행한 것은 아니다.

아래는 **displayUpdateRules 배열 부분의 형식 예시**다. 새 월 열의 Month/Year/Period를 각각 8/7/9행에 대응시켰다. 실제 원본/catalog binding 대조·기존 이벤트 연결이나 FE 검증 완료를 뜻하지 않으며 운영 seed나 §6의 규칙 없음 합성 표에 추가하는 목록도 아니다.

```json
[
  {
    "id": "liq-util-month-on-append",
    "trigger": {"type": "MONTHLY_COLUMN_APPENDED"},
    "inputs": [
      {"type": "PREVIOUS_LAST_MONTHLY_COLUMN_CELL", "rowNumber": 8}
    ],
    "target": {"type": "APPENDED_COLUMN_CELL", "rowNumber": 8},
    "operation": {"code": "NEXT_MONTH"}
  },
  {
    "id": "liq-util-year-on-append",
    "trigger": {"type": "MONTHLY_COLUMN_APPENDED"},
    "inputs": [
      {"type": "PREVIOUS_LAST_MONTHLY_COLUMN_CELL", "rowNumber": 7},
      {"type": "APPENDED_COLUMN_CELL", "rowNumber": 8}
    ],
    "target": {"type": "APPENDED_COLUMN_CELL", "rowNumber": 7},
    "operation": {"code": "YEAR_FROM_PREVIOUS_YEAR_AND_MONTH"}
  },
  {
    "id": "liq-util-period-on-append",
    "trigger": {"type": "MONTHLY_COLUMN_APPENDED"},
    "inputs": [
      {"type": "PREVIOUS_LAST_MONTHLY_COLUMN_CELL", "rowNumber": 9}
    ],
    "target": {"type": "APPENDED_COLUMN_CELL", "rowNumber": 9},
    "operation": {"code": "INCREMENT_INTEGER"}
  }
]
```

예를 들어 직전 Year/Month/Period가 `"2031"/"12"/"168"`이면 새 표시는 `"2032"/"1"/"169"`다. 11월 뒤에는 같은 Year의 12월을 생성한다. Period는 별도로 +1하며 달력의 시작을 Period 1로 추정하지 않는다.

추천 근거는 앞선 SUPPLY v6 읽기 조사에서 확인한 `H7=IF(H8=1,G7+1,G7)` 및 같은 형태의 H7:CL7 83개 수식, 수식 없는 Month/Period 행과 기존 FE의 getNextMonthlyColumn 처리다. 당시 G7은 시작 연도 값이고 Year 수식은 현재 Month/이전 Year만 참조했다. 이는 읽은 기준 파일의 관측이며 이번 문서 반영에서 원본을 재대조하거나 실제 catalog/FE 연결을 검증한 결과가 아니다. 실제 대상 양식과의 대조·연결/검증은 Q5에서 수행한다.

- 여러 규칙은 전달 배열 순서대로 적용한다. 같은 source/template·선택 표 기준의 값과 규칙을 사용하고, 조회/페이지 수신만으로 사용자 동작 규칙을 발동하거나 저장값을 재계산하지 않는다. 표 공통 규칙은 페이지마다 반복하되 FE는 중복 누적/반복 발동하지 않는다. 행별 전달 범위는 §5.1을 따른다.
- `LiqUtilAdjustment` 마지막 월 열 추가 시 새 `Period = 직전 마지막 Period + 1`은 확정 동작이다. 같은 표의 월 열 삭제 뒤 남은 Period는 기존 번호를 유지한다([domain 규칙 46·47](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)). 실제 추가/삭제 범위·이동 후 수식/참조 처리는 후속 write 계약을 따른다.
- 이 절은 새 월 열 생성의 세 규칙을 소유한다. G7 및 기존 H7:CL7/Month/Period 헤더의 편집·재계산 범위를 새로 허용하지 않는다. 월 열 삭제에는 세 추가 규칙을 발동하지 않으며 남은 표시값과 참조 처리 원칙은 §5.5를 따른다. 실제 보정 수식·계산 여부는 후속 write 계약에서 확인한다.
- 미조회 페이지/다른 표의 값이 필요한 규칙은 필요한 데이터와 조회 방식을 먼저 확정한다. 다른 표 grid를 암묵적으로 동봉하거나, 미조회 값을 빈 값/0으로 대신해 갱신하지 않는다. 해당 의존 관계를 확인하지 않은 상태를 규칙 없음/적용 검증 완료로 표시하지 않는다.
- 규칙은 기존 domain 규칙 46대로 **양식 version별 catalog 데이터로 DB 관리하고 BE가 읽어 제공**한다. 응답 JSON과 물리 DB 구조가 같다고 가정하지 않는다. 화면 표시 목록·선택지·규칙/순서의 변경도 §4.3의 catalogRevision으로 구별할 내용에 포함되지만, 실제 DB 저장 구조·revision 발급/연결은 Q2/Q5 및 기존 DDL 후속으로 남긴다.
- FE 화면 갱신은 BE write 검증·계산 허용 수식 처리·최종 XLSM 생성 검증을 대체하지 않는다. 조회는 원본 변경/계산을 수행하지 않는다. 실제 표시 목록·선택지/규칙 binding의 원본 대조, 세 규칙의 기존 이벤트 연결 구현, §5.5의 실제 삭제/보정·파일/FE 검증과 DB 저장 구조/revision 연결은 §9의 후속이다.

### 5.5 월 열 삭제 후 표시값 보존·수식 참조 원칙 — 사용자 확정

이 절은 논리 월 열 삭제 후의 표시/참조 원칙과 후속 파일 처리의 검증 목표를 소유한다. 실제 삭제할 월·범위, 물리 worksheet 열 삭제/셀 이동 방식, 보정 수식·계산 여부·파일/FE 검증을 확정하거나 구현하는 결정은 아니다. 기존 domain 규칙 47의 Period 유지와 표별 삭제 허용 범위는 유지한다.

#### 남은 월의 값과 연결 보존

- 삭제한 월만 제거하고 **남은 Year/Month/Period는 삭제 전 값을 유지**한다. 빈 기간을 메우려고 Month를 당기거나 Year/Period를 다시 계산·번호화하지 않는다. 해당 월의 본문 값과 월의 연결도 함께 유지한다.
- 화면에서 열 순서가 바뀌어도 남은 열의 field와 원본 월의 대응을 화면 순번으로 다시 붙이지 않는다. 실제 파일에서 저장 좌표가 바뀌면 원본 월과 새 저장 위치의 대응을 별도로 확인한다. 이 원칙은 새 영속 ID/DB 컬럼을 추가하는 결정이 아니다.
- 삭제 시 MONTHLY_COLUMN_APPENDED의 세 규칙은 발동하지 않는다. 남은 표시값 보존을 위해 별도 재번호화/재계산 규칙을 추가하지 않으며 §5.4의 필수 5필드·좌표 3종·기존 발동/연산 코드를 유지한다.

아래는 2026년 1월을 삭제한다고 가정한 **논리 표시 예시**다. 실제 삭제할 월/열을 선정하거나 파일에서 실행한 fixture가 아니다.

| 삭제 전 월·Period | 화면에서 삭제 후 |
| --- | --- |
| 2025년 12월·96 | 동일 값과 해당 월의 본문 유지 |
| 2026년 1월·97 | 제거 |
| 2026년 2월·98 | 2026년 2월·98 유지. 화면에서 왼쪽으로 이동해도 1월로 바꾸지 않음 |

#### 수식 참조의 처리 원칙

- 참조 대상 월/셀이 남아 있으면 그 원본 대상과 새 저장 좌표의 대응에 맞춰 참조를 옮긴다. 단순한 열 위치 이동을 이유로 다른 월의 값을 참조하도록 바꾸지 않는다.
- 참조 대상 월이 삭제됐으면 삭제된 참조로 구분하고 **Year 경계의 별도 수식 보정 대상**으로 식별한다. 새 왼쪽 월로 자동 대체하지 않는다. 보정 결과는 남은 월의 삭제 전 Year/Month를 보장해야 한다.
- 정확한 보정 수식·변경 셀 범위·계산 여부는 실제 삭제 조건으로 후속 확정한다. 수식 전체를 숫자로 바꾸거나 저장된 계산값만 남기는 방식을 이번 원칙 승인으로 허용하지 않는다. 다른 표/시트의 수식·참조와 파일 설정에 미치는 영향도 별도 확인한다.

앞선 원본 읽기에서 2026년 2월의 T7은 `IF(T8=1,S7+1,S7)`이고 S열은 2026년 1월이었다. 1월 삭제 후 2월을 새 왼쪽 열인 2025년 12월에 단순 연결하면 Year가 2025가 될 수 있다. 따라서 **표시값 보존과 수식의 유효한 참조 보정은 별도 확인 대상**이다. 이 설명은 원본 수식에 따른 분석이며 실제 삭제/보정 실행이나 계산 정답 검증이 아니다.

#### 삭제 뒤 추가와 후속 경계

- 삭제 뒤 월 열 추가는 §5.4대로 **현재 남은 마지막 월**을 기준으로 한다. 중간 월 삭제 시 기존 마지막 월의 다음을, 마지막 월 삭제 시 남은 마지막 월의 다음을 생성한다. 후자의 경우 삭제했던 마지막 기간이 다시 생성될 수 있으며 원본 최대 Period로 기준을 조용히 바꾸지 않는다.
- 남은 월이 없으면 기존 추가 규칙의 참조 부재로 해당 추가 동작을 실패시킨다. 전체 월 삭제 허용 여부·초기화 방식은 후속이며 최소 1열 유지 제약이나 원본 마지막 열 복구를 임의로 추가하지 않는다.
- 실제 삭제 범위·보정 수식·계산 여부·파일/FE 검증은 Q5 및 후속 write/INPUT-02A 검증 범위에 남긴다. 첫 월 G열 삭제의 시작 연도 기준, 중간 1월 삭제, 연도를 넘는 여러 월 삭제, 마지막 월 삭제를 구분해 확인한다. 이 원칙의 채택으로 특정 사례의 삭제/계산을 승인하거나 INPUT-02A 상태를 변경하지 않는다.

## 6. 대표 응답과 좌표 대응

모든 값·표명·시트·좌표는 **형식 검토용 합성 예시**다. 실제 XLSM을 읽은 결과, 승인 표시 목록, seed 또는 편집 정책이 아니다. sourceVersion `sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f`의 합성 표 전체를 아래처럼 작게 정의했다. 실제 응답을 임의로 축약해도 된다는 뜻이 아니다. 실제 LNG control은 §5의 확정 표시/저장값 분리를 따른다. 아래 합성 표는 화면 갱신 규칙 없음으로 정의했으므로 `displayUpdateRules: []`를 사용한다. 실제 표의 미확인 규칙을 빈 목록으로 바꾸는 예시가 아니며 실제 목록/선택지·규칙 binding의 원본 대조·기존 이벤트 연결 구현·추가 규칙/FE 검증은 §9 Q5의 후속이다. §5.4의 세 규칙 배열 예시는 아래 합성 표의 규칙 목록을 변경하지 않는다.

예시의 sourceVersion은 §4.3 규칙에 다음 **합성 식별값**을 넣어 계산했다. 실제 저장소에서 내용 고정을 확인했다는 증거가 아니다.

- `ruleVersion=sv1`, `tenantKey=example-tenant`, `caseId=101`, `templateVersion=example-template-v1`, `catalogRevision=example-catalog-r1`. caseId를 포함한 말단 값은 문자열이다.
- files는 고정 4역할 순서이며 `fileId=example-<역할 소문자>-v1`, `objectRef=example-object-<역할 소문자>`, `contentRevision=example-content-v1`이다. 이 fileId는 내부 생성 입력이며 공개 sections.inputId는 아니다. 예시의 sourceKey/inputId는 같은 합성 tenant·Case·역할 정보로 §4.5에 따라 별도로 계산했다. 새 공개 inputId를 sourceVersion의 fileId로 넣지 않는다.

### 6.1 QRY-01 — 전체 구조, 미선택

```json
{
  "status": true, "code": "SUC-000", "message": "Success.",
  "data": {
    "sourceKey": "sc1_36fcd430f4966a413fc2cf08b9fb0aa5dd5fafc9172a3e9fae6528dd95311b8e", "sourceVersion": "sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "sections": [
      {"kind": "DEMAND", "inputId": "in1_57b5bba3e3adf957ce3a9c618917f340a9e5d4e5ae515bf6b2386efce6798ae6", "sheets": [
        {"sheetName": "Example", "tables": [
          {"id": "DEMAND/Example/Input", "kind": "DEMAND", "sheetName": "Example", "name": "Input", "description": "합성 입력표", "location": "BB3:BD5", "rowPolicy": "고정", "columnPolicy": "고정", "coordinates": {"headerRow": 3, "bodyStart": 4}, "controlIds": []},
          {"id": "DEMAND/Example/Mode", "kind": "DEMAND", "sheetName": "Example", "name": "Mode", "description": "합성 control 소유 영역", "location": "B1:C1", "rowPolicy": "고정", "columnPolicy": "고정", "coordinates": {"headerRow": 0, "bodyStart": 1}, "controlIds": ["example-mode"]},
          {"id": "DEMAND/Example/Annual Demand", "kind": "DEMAND", "sheetName": "Example", "name": "Annual Demand", "description": "합성 표와 공유 control", "location": "B3:C4", "rowPolicy": "고정", "columnPolicy": "고정", "coordinates": {"headerRow": 3, "bodyStart": 4}, "controlIds": ["example-mode"]}
        ]}
      ]},
      {"kind": "SUPPLY", "inputId": "in1_4b011cc00322f72853d1a2e409eee90d1decfa805789ed2b5932952f9b71f218", "sheets": [
        {"sheetName": "Example", "tables": [
          {"id": "SUPPLY/Example/Reference", "kind": "SUPPLY", "sheetName": "Example", "name": "Reference", "description": "합성 읽기 전용표", "location": "B2:C3", "rowPolicy": "고정", "columnPolicy": "고정", "coordinates": {"headerRow": 2, "bodyStart": 3}, "controlIds": []}
        ]}
      ]},
      {"kind": "TRADE", "inputId": "in1_0e49831d8c5d935515ab0a5a09b1bc75085ce62c6937682afb911aeb74c7b17e", "sheets": []},
      {"kind": "OUTPUT", "inputId": "in1_d06aad0753e120cb7462221652108e987296152d23406ca9daee14f75da7f832", "sheets": []}
    ],
    "tableData": null
  }
}
```

### 6.2 QRY-02 — 일반 입력표, 빈 편집 셀 포함

```json
{
  "status": true, "code": "SUC-000", "message": "Success.",
  "data": {
    "sourceKey": "sc1_36fcd430f4966a413fc2cf08b9fb0aa5dd5fafc9172a3e9fae6528dd95311b8e", "sourceVersion": "sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "tableData": {
      "tableId": "DEMAND/Example/Input",
      "columns": [{"field": "column53", "headerName": "BB"}, {"field": "column54", "headerName": "BC"}, {"field": "column55", "headerName": "BD"}],
      "rows": [
        {"id": 2, "rowNumber": 3, "column53": "Country", "column54": "2025", "column55": "2026"},
        {"id": 3, "rowNumber": 4, "column53": "Example A", "column54": "1.00", "column55": 0},
        {"id": 4, "rowNumber": 5, "column53": "Example B"}
      ],
      "inputCells": [65590, 65591, 81974, 81975],
      "inputGuideRules": [], "controls": [], "displayUpdateRules": []
    },
    "page": {"totalBodyRows": 2, "returnedBodyRows": 2, "startRow": 4, "endRow": 5, "hasNext": false, "nextStartRow": null}
  }
}
```

| 원본 위치 | 응답의 대응 |
| --- | --- |
| `BB3` | `rows[id=2].column53`. 헤더를 본문과 별도 번호로 다시 매기지 않음 |
| `BC4` | `rows[id=3].column54`는 `"1.00"`, inputCells는 `4 * 16384 + 54 = 65590` |
| `BD4` | 숫자 `0`을 유지. 빈칸으로 생략하지 않음 |
| `BC5` / `BD5` | 값 필드는 생략하고 열 정의와 편집 위치 `81974/81975`는 유지 |

### 6.3 QRY-02 — 읽기 전용표

```json
{
  "status": true, "code": "SUC-000", "message": "Success.",
  "data": {
    "sourceKey": "sc1_36fcd430f4966a413fc2cf08b9fb0aa5dd5fafc9172a3e9fae6528dd95311b8e", "sourceVersion": "sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "tableData": {
      "tableId": "SUPPLY/Example/Reference",
      "columns": [{"field": "column1", "headerName": "B"}, {"field": "column2", "headerName": "C"}],
      "rows": [{"id": 1, "rowNumber": 2, "column1": "Item", "column2": "Value"}, {"id": 2, "rowNumber": 3, "column1": "Example", "column2": "1,234.50"}],
      "inputCells": [], "inputGuideRules": [], "controls": [], "displayUpdateRules": []
    },
    "page": {"totalBodyRows": 1, "returnedBodyRows": 1, "startRow": 3, "endRow": 3, "hasNext": false, "nextStartRow": null}
  }
}
```

### 6.4 QRY-02 — 표와 공유 control

```json
{
  "status": true, "code": "SUC-000", "message": "Success.",
  "data": {
    "sourceKey": "sc1_36fcd430f4966a413fc2cf08b9fb0aa5dd5fafc9172a3e9fae6528dd95311b8e", "sourceVersion": "sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "tableData": {
      "tableId": "DEMAND/Example/Annual Demand",
      "columns": [{"field": "column1", "headerName": "B"}, {"field": "column2", "headerName": "C"}],
      "rows": [{"id": 2, "rowNumber": 3, "column1": "Country", "column2": "2026"}, {"id": 3, "rowNumber": 4, "column1": "Example A", "column2": "2.00"}],
      "inputCells": [], "inputGuideRules": [],
      "controls": [{"id": "example-mode", "type": "select", "label": "Example Mode", "position": "beforeGrid", "sourceTableId": "DEMAND/Example/Mode", "binding": {"rowNumber": 1, "columnIndex": 2}, "value": null, "options": [{"value": "EXAMPLE_A", "label": "Example A"}, {"value": "EXAMPLE_B", "label": "Example B"}], "editable": true}],
      "displayUpdateRules": []
    },
    "page": {"totalBodyRows": 1, "returnedBodyRows": 1, "startRow": 4, "endRow": 4, "hasNext": false, "nextStartRow": null}
  }
}
```

control 값의 원본은 같은 DEMAND/Example 시트의 `C1`이다. 값 소유 영역은 Mode이고 선택 영역은 Annual Demand다. C1을 grid rows에 중복하지 않는다. 선택지·현재 값은 구조가 아닌 선택 데이터에만 있다.

### 6.5 QRY-02 — control 전용 영역

```json
{
  "status": true, "code": "SUC-000", "message": "Success.",
  "data": {
    "sourceKey": "sc1_36fcd430f4966a413fc2cf08b9fb0aa5dd5fafc9172a3e9fae6528dd95311b8e", "sourceVersion": "sv1_2ffffe37738ed2a30da32c10e7ca7d9b2024f689540a85071f6213ff07795a2f",
    "tableData": {
      "tableId": "DEMAND/Example/Mode",
      "columns": [], "rows": [], "inputCells": [], "inputGuideRules": [],
      "controls": [{"id": "example-mode", "type": "select", "label": "Example Mode", "position": "beforeGrid", "sourceTableId": "DEMAND/Example/Mode", "binding": {"rowNumber": 1, "columnIndex": 2}, "value": null, "options": [{"value": "EXAMPLE_A", "label": "Example A"}, {"value": "EXAMPLE_B", "label": "Example B"}], "editable": true}],
      "displayUpdateRules": []
    },
    "page": {"totalBodyRows": 0, "returnedBodyRows": 0, "startRow": null, "endRow": null, "hasNext": false, "nextStartRow": null}
  }
}
```

### 6.6 QRY-02 — 큰 표의 페이지 연결과 종료

다음은 **`data.page` 객체만 보여 주는 예시**다. 실제 200 응답에는 §6.2처럼 `BaseResponse`, 같은 source/표와 해당 범위의 `tableData`가 모두 필요하다. `displayUpdateRules`도 §5.4에 맞게 포함하고 응답 전체 크기에 산입한다. 설명용 본문은 원본 4~100803행의 100,800행이며 헤더는 행 수에서 제외했다.

첫 요청은 `startRow`를 생략한다.

```json
{"totalBodyRows": 100800, "returnedBodyRows": 2000, "startRow": 4, "endRow": 2003, "hasNext": true, "nextStartRow": 2004}
```

다음 요청에 `startRow=2004`를 넣는다. 이후에도 응답의 다음 시작 행을 사용한다.

```json
{"totalBodyRows": 100800, "returnedBodyRows": 2000, "startRow": 2004, "endRow": 4003, "hasNext": true, "nextStartRow": 4004}
```

중간 페이지들을 이어간 뒤 마지막 요청은 `startRow=100004`다. 800행을 받고 종료하며 `startRow=100804` 요청은 보내지 않는다.

```json
{"totalBodyRows": 100800, "returnedBodyRows": 800, "startRow": 100004, "endRow": 100803, "hasNext": false, "nextStartRow": null}
```

첫 구간의 2,000행 응답이 2 MiB를 초과하고 1,000행으로 줄여 예산을 만족한 경우의 별도 예시다. 다음 요청은 `startRow=1004`이며 2004로 뛰어넘지 않는다. 실제 반환 행 수는 직렬화한 응답 크기로 정한다.

```json
{"totalBodyRows": 100800, "returnedBodyRows": 1000, "startRow": 4, "endRow": 1003, "hasNext": true, "nextStartRow": 1004}
```

## 7. 오류·실패 구분 제안

기존 조회 명세 §6와 공통 ResultCode를 재사용하고 최소 응답 크기 초과에 422를 구체화한다. FE는 HTTP status/code로 동작을 구분하며 message를 파싱하지 않는다. 같은 `BIZ-001`이어도 409는 원본 충돌, 422는 더 줄일 수 없는 응답이므로 복구 동작이 다르다. 새 오류 enum·별도 예외 처리기는 이번에 요구하지 않는다.

| 조건 | HTTP / enum / wire code | FE가 구분할 행동 |
| --- | --- | --- |
| caseId/startRow 형식·숫자 범위, sourceVersion 형식 위반, 필수 query 누락·blank | 400 / REQ_INVALID_PARAMETER / `REQ-003` | 요청 구성 수정. 형식/숫자 범위는 비싼 파일 읽기 전에 거부 |
| 유효한 버전·표에서 startRow가 본문 행이 아님 | 400 / REQ_INVALID_PARAMETER / `REQ-003` | 요청 구성 수정. 헤더·마지막 이후·본문 없는 표의 명시 값 포함. 카탈로그의 본문 소속 검사 |
| 인증 없음, 허용 role/condition 아님 | 401/403 / 기존 인증·권한 코드 | 공통 로그인·권한 흐름. 파일 파싱 전 검사 |
| Case 없음·삭제·SCENARIO·접근 범위 밖 | 404 / RSC_NOT_FOUND / `RSC-001` | 출처 선택으로 복귀. 다른 tenant의 존재를 응답하지 않음 |
| 표시 구조에 없는 tableId 또는 미표시 표 | 404 / RSC_NOT_FOUND / `RSC-001` | 같은 Case의 구조 갱신·재선택. 임의 범위 조회 금지 |
| 4역할 누락/중복, 원본 기준 변경, 등록 양식과 구조 불일치 | 409 / BIZ_RULE_VIOLATION / `BIZ-001` | 현재 값 기준 폐기·같은 Case의 구조 재조회. 409가 반복되면 오류 상태 유지 |
| 등록 양식/해석 정책이 없거나 필요한 표시 범위·선택지·규칙이 미확정이어서 안전하게 표시 불가 | 409 / BIZ_RULE_VIOLATION / `BIZ-001` | 다른 버전 fallback 없이 표시 불가 안내. 정상 빈 목록이나 일부 표 누락으로 숨기지 않음 |
| 필요한 metadata/헤더/control/규칙과 본문 1행조차 2 MiB 초과, 또는 본문 없는 표의 최소 응답 초과 | 422 / BIZ_RULE_VIOLATION / `BIZ-001` | 현재 표 조회 중단·크기 때문에 제공 불가 안내. 같은 버전 구조 재조회/자동 재시도를 반복하지 않음. 먼저 읽은 페이지는 일부 조회로만 표시하고 전체 완료로 처리하지 않음 |
| metadata는 있지만 객체 없음·읽기/파싱 실패, 현재 응답 범위의 수식 결과 부재/판독 불가 | 500 / SYS_INTERNAL_ERROR / `SYS-001` | 현재 표/페이지 요청 실패, data null. §5.3 분류는 확정이며 정상 빈 결과/저장 오류는 이 실패 조건이 아님. 실제 판독기·표본 검증은 Q3 후속 |

인증/접근 범위 검사를 먼저 수행한 뒤 유효한 원본 기준에서 표 소속을 검사한다. 파일 내용이 아닌 읽기 실패와 구조 불일치를 구분한다. 응답에는 provider 원문·저장 경로·수식 원문·stack trace를 넣지 않는다.

대표 409는 공통 메시지를 쓰며 잘못된 요청값이나 새 sourceVersion을 오류 data에 넣지 않는다.

```json
{"status": false, "code": "BIZ-001", "message": "The operation violates a business rule.", "data": null}
```

대표 500도 공개 가능한 공통 오류 본문만 제공한다.

```json
{"status": false, "code": "SYS-001", "message": "An internal server error has occurred.", "data": null}
```

대표 422는 고정된 공개 메시지를 사용하고 내부 크기 산정 내역·원본 값을 노출하지 않는다. FE의 분기 기준은 메시지가 아닌 HTTP 422와 `BIZ-001`이다. 이 오류는 현재 표에만 적용하며 다른 표를 일괄 조회 불가로 만들지 않는다.

```json
{"status": false, "code": "BIZ-001", "message": "The minimum table response exceeds the response size budget.", "data": null}
```

## 8. OpenAPI·구현 인계와 검증 기준

### 8.1 문서화 인계

| 항목 | QRY-01 | QRY-02 |
| --- | --- | --- |
| `@Operation.summary` | `저장 Case 입력의 전체 표시 구조 조회` | `선택 입력 영역의 저장값 조회` |
| description 첫 문장 | `선택한 Case의 네 입력 파일에서 화면에 표시할 시트·표 구조를 조회합니다. 입력 화면 진입 또는 원본 기준 갱신 시 호출합니다.` | `구조 조회에서 선택한 표의 헤더·저장값·연결 control·화면 갱신 규칙을 같은 원본 기준으로 조회합니다. 표 선택·전환·다음 본문 조회 시 호출합니다.` |
| operation별 예시 | §6.1, tableData null·네 sections | §6.2~6.5의 전체 응답, §6.6의 page 객체를 사용한 첫/다음/마지막/축소 응답 |
| parameter/schema | §3.2·§4.3·§4.5·§5의 식별자 형식·required/null/생략 | §3.2·§4.3·§4.5·§5의 식별자 형식·400/409 구분·startRow/page/422, §5.4의 displayUpdateRules 필수 배열·정상 빈 목록 의미 |
| 화면 매핑 | 파일 탭 → sections, 시트/표 탐색 → sheets/tables | 본문 → tableData.rows/columns, 입력 위치 → inputCells, 안내 → inputGuideRules, 부속 선택 → controls, 화면 갱신 동작 → displayUpdateRules, 다음/종료 → page |
| structured responses | 200·400·401·403·404·409·500 | 200·400·401·403·404·409·422·500 |

`@ApiResponses`에는 기존 enum명과 실제 wire code의 대응, BaseResponse schema와 operation별 성공/오류 `@ExampleObject`를 넣는다. 구현 시 §6.6의 page 객체만 성공 응답 전체로 등록하지 말고 해당 범위의 tableData를 갖춘 전체 예시로 구성한다. annotation·validation·generated OpenAPI의 required/nullable/필드 포함 여부를 동일하게 맞추고, 최대 본문 행 수와 압축 전 bytes 기준은 description에도 명시한다. 응답 타입을 operation별로 구분하고 표/셀 공통 타입을 공유한다. 공통 DTO의 non-null 예시를 QRY-01에 그대로 재사용하지 않는다. sourceKey/inputId의 prefix·68자/pattern과 필드의 생략/null 조건도 schema와 예시에 맞춘다. 계약 정본은 통합된 기존 feature spec이고 generated OpenAPI는 동기화/검증 대상이다.

§5.3의 정상 빈 결과·저장된 오류는 200, 결과 부재/판독 불가는 500임을 operation 설명/예시에 반영한다. 저장값 판독이 계산 정답·최신성 보장을 뜻하지 않는다는 한계도 설명한다. XML 내부 판독 절차는 이 명세의 구현 인계에 두고 공개 API에 수식 원문/새 셀 상태 필드를 추가하지 않는다.

숫자 표시와 FE의 문자열 보존 책임은 기존 조회 명세 §3.2를 참조해 소비자 설명에 반영한다. 표시 string의 직렬화와 실제 응답을 generated OpenAPI/예시에 맞추며, 정책 채택 문서만으로 DTO/응답 동기화를 완료 처리하지 않는다.

§5.4의 필수 5필드·CellRef 3종을 required/non-null 및 type별 schema로 표현한다. trigger.type은 MONTHLY_COLUMN_APPENDED, operation.code는 세 enum으로 맞추고 입력 개수 1/2/1·행/열 범위를 문서화한다. Month → Year → Period 순서, Year가 앞선 새 Month를 참조하는 의미, 10진 문자열 결과·월 범위와 세 규칙 전체의 함께 성공/실패를 설명한다. §5.4는 displayUpdateRules 배열 부분 예시이며 operation 전체 성공 예시로 대신 등록하지 않는다. 규칙 없음 전체 응답은 §6을 사용한다. 실제 원본 대조·기존 이벤트 연결 구현·추가 규칙·DB/revision 연결·FE 검증은 후속이며 합성 JSON/문서 검사를 실제 DTO/FE 검증으로 보고하지 않는다.

§5.5의 삭제 후 Year/Month/Period 보존과 추가 규칙 미발동을 소비자 설명에 연결한다. 수식 참조 원칙과 실제 보정 수식/계산/파일·FE 검증 후속을 구분하며 삭제 원칙만으로 새 조회 필드·연산/발동 코드·HTTP 오류를 추가하지 않는다.

### 8.2 구현 경계

- 권한과 Case/file metadata는 가까운 기존 JPA Repository/공통 권한 경로를 우선 재사용한다. 공개 tableId를 SQL identifier나 임의 파일 경로로 사용하지 않는다.
- 값 추출은 기존 명세 §8의 선택 영역 streaming OOXML/SAX 경계를 따른다. 조회용으로 전체 `XSSFWorkbook` 로딩, 수식 실행, 외부 링크 갱신, macro 실행을 도입하지 않는다.
- 이 제안의 필드 추가만으로 DB table·cache·snapshot 저장·새 공통 framework를 추가하지 않는다. 카탈로그/identity의 물리 표현은 기존 INPUT-03/DDL 후속이 소유한다.
- 구현 touch point 후보는 기존 fundamentalmodel Controller/Service·요청/응답 DTO와 Case/파일 Repository다. 업무 데이터 본문 로깅 제외는 현 공통 필터에 좁게 적용하고, 경로를 Swagger pass 목록에 넣어 전체 필터 처리를 우회하지 않는다.
- 설계 준비 단계의 FE 연동은 §6의 합성 응답으로 검토할 수 있다. 합성 값을 제품 응답·실파일 누락 fallback으로 사용하지 않는다.

### 8.3 후속 구현에서 증명할 행동

| Given / When | Then |
| --- | --- |
| 접근 가능한 완전한 4파일 Case / QRY-01 | 같은 버전의 네 sections, 값 없는 구조, tableData null. Draft/Case/Run 생성·파일 변경·계산 호출 없음 |
| 같은 버전 구조의 표 / QRY-02 | 선택 grid/연결 control과 page 반환. 구조의 ID/역할·원본 위치와 일치 |
| 구조/표 성공 응답과 실패 응답 / 필드 확인 | §5의 operation별 필드만 제공. 구조는 tableData null·page 없음, 표는 tableData/page 필수·sections/inputId 없음. 실패는 data null |
| 같은 양식 version/catalog / 구조와 표 조회 | 승인 표시 목록·순서·행/열 범위 일치. 읽기 전용 값·빈 등록 본문/편집 위치 유지, 미확정 범위를 정상 빈 목록/표 누락으로 대체하지 않음 |
| 같은 source의 control / 조회 | catalog에 등록한 선택지 저장값/타입/순서와 파일 현재 값 구분. null·목록 밖 값 유지, 실제 원본 대조 없이 선택지 확정/자동 보충 없음 |
| 일반·읽기 전용·control 전용 표 / QRY-02 | displayUpdateRules 필수 배열. 규칙 없음 확인 시만 [], 미확정/미지원 규칙을 빈 목록으로 숨기지 않음. QRY-01에는 중복 목록 없음 |
| 규칙이 있는 표 / 해당 사용자 동작과 페이지 수신 | 규칙 ID·발동·참조·대상·지원 연산 및 배열 순서 확인. 공통 규칙 반복 제공/중복 누적 방지, 행별 범위와 2 MiB 예산 유지. 조회 수신만으로 발동/재계산하지 않음 |
| 규칙 항목의 schema/예시 / 필드·타입 확인 | §5.4의 5필드 required/non-null, trigger/operation의 enum, CellRef 3종별 필드·정수 범위와 inputs 개수 일치. 실제 원본 binding/FE 실행은 별도 검증 |
| LiqUtilAdjustment 끝 월 열 추가 / 규칙 배열 적용 | 같은 MONTHLY_COLUMN_APPENDED의 NEXT_MONTH → YEAR_FROM_PREVIOUS_YEAR_AND_MONTH → INCREMENT_INTEGER 순서. Year의 두 번째 입력은 앞선 새 Month, 나머지 기존 열 입력은 추가 직전 마지막 월 열. 11→12월은 같은 Year, 12→1월은 Year +1, Period는 별도 +1. 실제 원본/catalog binding·기존 이벤트 연결/FE 실행은 후속 |
| 연속 월 열 추가 | 앞선 성공 상태를 다음 동작의 기준으로 사용. 삭제 뒤 추가는 §5.5대로 현재 남은 마지막 월 기준이며 원본 최대 Period로 대체하지 않음 |
| 논리 월 열 삭제 / 표시값과 본문 확인 | 남은 Year/Month/Period와 원본 월/field/본문 연결 유지. 화면 위치에 따라 재번호화하거나 세 추가 규칙을 발동하지 않음. 실제 삭제 범위와 파일/FE 검증은 후속 |
| 삭제 뒤 수식이 남은 셀 또는 삭제된 월을 참조 | 전자는 같은 논리 대상의 새 저장 좌표에 대응, 후자는 별도 보정 대상으로 식별하고 새 왼쪽 월로 자동 대체하지 않음. 삭제 전 Year/Month 보존이 목표이며 정확한 수식/범위·계산 여부·파일 검증은 후속 |
| 첫 월/중간 1월/여러 연도/마지막 월 삭제 또는 남은 월 없음 | §5.5의 후속 사례별로 기준값/참조와 표시 보존을 확인. 남은 월이 없으면 추가 참조 부재로 실패. 전체 월 삭제 허용/초기화는 미확정이며 특정 삭제 fixture를 이 표로 자동 선정하지 않음 |
| 알 수 없는 코드·잘못된 좌표/개수·입력 순서 오류·참조 부재·빈 값/오류·월/정수 범위 초과 / 규칙 적용 | 해당 열 추가와 세 규칙 전체를 함께 실패시키고 이유 표시. 부분 반영·값 보정 없음. GET의 정상 빈 결과/저장 오류 정책 유지, 실제 FE 행동 검증은 후속 |
| 기존 H7:CL7 헤더 직접 변경 또는 미조회 페이지/다른 표 의존 | 추가/삭제 원칙으로 별도 편집·재계산 권한을 확대하지 않음. 실제 원본 대조·의존/갱신 규칙·저장/FE 검증은 후속이며 G7 제외와 미조회 값 임의 대체/다른 표 grid 암묵 동봉 금지 유지 |
| 같은 tenant·Case / 파일/metadata 교체·표 전환·다음 페이지 | sourceKey 동일. tenant/Case가 달라지면 다른 키. 출처 목록 연동 시 저장 출처 key와 응답 key 일치 |
| 공통 양식/catalog 기준 유지 / 한 역할 파일·객체·내용 버전 변경 | 해당 inputId만 변경하고 다른 역할 inputId와 sourceKey 유지. 전체 sourceVersion은 변경, 409 무효화 유지 |
| 공통 templateVersion/catalogRevision 변경 또는 동일 조합 반복 | 기준을 사용하는 역할 inputId 변경·sourceKey 유지. 동일 조합은 sc1/in1의 68자 값 재현. 실제 원본 고정/metadata 연결은 별도 검증 |
| 동일한 원본 조합 / 반복 구조 조회·표 전환·다음 페이지·다른 권한 있는 사용자 | §4.3 고정 직렬화의 sourceVersion 일치. 발급 시각·사용자·tableId·startRow에 따라 바뀌지 않음 |
| tenant·Case·한 역할의 파일/객체·내용 버전·양식/카탈로그 기준 중 하나 변경 | sourceVersion 달라짐. 4역할 정렬을 바꿔 받은 동일 조합은 정규 순서로 결합해 동일 값 |
| sourceVersion 누락·형식 위반 또는 형식은 맞지만 현재 조합과 다름 | 전자는 400/REQ-003, 후자는 409/BIZ-001. 접근 권한 검사를 생략하지 않음 |
| 불변 파일 등록 / 반복 업로드·연결/catalog 교체·요청 중 및 페이지 사이 기준 변경 | 새 업로드는 새 file_no/UUID 객체이고 이전 원본 참조 유지. 서버가 확인한 불변 등록을 사용해 동일 조합/다른 조합의 식별자와 기존 409/500 처리 검증. 원본 내용을 덮어쓰는 실제 경로가 있다는 가정의 전체 재해시 시험을 필수화하지 않음 |
| 본문 0/1/1,999/2,000/2,001/4,000행, 떨어진 본문 범위 / 첫 요청부터 nextStartRow 연결 | 페이지별 본문 최대 2,000행, 반복 헤더 제외·원본 번호 유지. 누락/중복 없이 전 본문 수와 일치, 마지막 false/null에서 추가 요청 없이 종료 |
| 작은 표 또는 control 전용 / 첫 요청 | 예산 안이면 전체 응답·다음 없음. 본문 0이면 시작/끝/다음 null, 필요한 헤더/control 유지 |
| 최종 JSON이 2,097,152 bytes 또는 1 byte 초과 / 크기 판정 | 같으면 허용. 초과면 행 단위 축소, 실제 page metadata 포함 상한 이내·정확한 다음 행. 열/문자열/편집 위치를 잘라 맞추지 않음 |
| 최소 필요 metadata와 본문 1행 또는 본문 없는 최소 응답도 초과 | 부분 200 없이 422/BIZ-001/data null. FE 진행 중단·일부 조회 표시, 무한 재조회·실패 행 건너뛰기 없음 |
| 헤더·표 본문 밖·마지막 이후 startRow 또는 본문 없는 표에 startRow 지정 | 400/REQ-003. 빈 성공으로 종료를 대신하지 않음 |
| 빈 본문·빈 편집 셀·숫자0·읽기 전용표 | §5의 서로 다른 의미를 유지. 필요한 헤더·열·편집 위치 누락 없음 |
| 수식의 t=str + 명시적 빈 v 또는 일반 빈 셀 | 전자는 빈 문자열 결과를 유지, 후자는 기존 값 필드 생략 정책. v 부재와 구별하며 문자열 공백 임의 trim 없음 |
| 수식의 유효한 숫자0/FALSE·문자열 또는 t=e 오류 결과 | 표시 서식 전에 유효 결과로 판독. 저장 오류값 유지, 문자열의 오류 모양 텍스트는 타입으로 구별. 정답/최신성 보장으로 확대하지 않음 |
| 수식 결과 v 부재, 숫자형 빈 v·잘못된 타입/값/문자열 참조 | 현재 표/페이지 요청 500/SYS-001/data null. 빈 성공·0/null 대체·재계산·오류 행 건너뛰기 없음 |
| 본문·반복 헤더·연결 control 중 판독 실패 또는 아직 읽지 않은 페이지 | 현재 응답 범위의 실패 처리와 부분 조회 표시 유지. 다른 표/미조회 페이지까지 검증 성공/전체 완료로 표시하지 않음 |
| 공유수식·지원 저장 타입·문자열 참조·표시 서식 표본 | §5.3 정책대로 실제 판독기를 구현한 뒤 합성 XML 및 읽기 전용 실파일로 증명. 이번 문서 검사를 해당 검증 Pass로 취급하지 않음 |
| 승인 표시 범위의 관측 회계 서식 / 양수·음수·0 표시 | 기존 조회 명세 §3.2의 내용 표시와 일치하고 셀 좌표/custom 서식 ID에 의존하지 않음. 배치 공백 제외를 문자열·다른 서식에 확대하지 않음 |
| 승인 표시 범위의 General / 소수·지수 전환과 정밀도·반올림 경계 | 같은 정본의 표시 규칙과 고정 기대값에 일치. 요청/브라우저 지역 설정·열 폭에 따라 표시 문자열이 달라지지 않음. 전체 숫자/locale 지원으로 확대하지 않음 |
| 형식화한 grid 값 / HTTP 직렬화와 FE 표시·복사 | string 보존·재서식/trim 없음. 원본 좌표·편집 위치와 control 저장값 타입/소유권 유지 |
| 승인 catalog의 실제 타입·서식·숫자 범위 / 후보 지원 범위 대조 | 지원/미지원/미노출 대응과 필요한 추가 표본을 식별. 실제 필요한 항목만 보완하고 독립 기술 표본을 승인 catalog로 간주하지 않음 |
| LNG 모드 저장값이 Dynamaic | label은 Dynamic, options value와 현재 control value는 Dynamaic. 원본 셀/파일 불변 |
| 표와 공유 control 또는 control 전용 영역 | 소유 영역/같은 sheet 연결, 원본 값 중복 없음, 동일 source의 control 값·선택지 일치 |
| 구조 조회 후 파일/양식 변경 또는 처리 중 변경 | 혼합 200 대신 409. 원본을 재선택해 조용히 대체하지 않음 |
| A→B→A 또는 다른 페이지의 늦은 응답 도착 | FE 요청 순번과 case/sourceVersion/tableId/요청 시작 위치 대조로 이전 응답 미적용. 중간/실패 범위가 있으면 전체 완료 표시 금지 |
| 미인증·권한 없음·다른 tenant·미표시 표·삭제 Case | 해당 401/403/404, 비인가 객체/셀 읽기 없음 |
| 객체 누락·파싱 실패·불완전 4파일 | 빈 성공 없이 §7의 409/500. 원본 경로/본문 로깅·오류 노출 없음 |

구현 검증은 관련 클래스부터 `./gradlew test -PforceTest --tests '<조회 동작 테스트 클래스>'`로 실제 실행 수를 확인한다. generated OpenAPI에서는 두 path·query 필수/선택 값·page와 operation별 schema·422를 포함한 대표 오류를 검증한다. 이 단계에서는 endpoint/DTO 구현이 없어 그 실행 증거를 만들지 않는다.

## 9. Open Questions — 사용자 확정 후 남은 협의·검증

Q2 §4.6 연결안의 제품 검증에는 같은 Case/발행 묶음의 반복·페이지 요청에서 두 내부 값 유지, 새 ACTIVE 발행에도 기존 Case/DEPRECATED 연결 유지, 다른 발행 묶음을 입력한 조합 식별자 변경을 포함한다. 미연결/모호/정의 미완비는 기존 409이고 Case 없음은 기존 404인지, GET의 관계 INSERT/최신 버전 fallback이 없는지 확인한다. 실제 DB 읽기/1차 cache 경계와 기준 변경 경합은 합성 식별자 검사만으로 완료 처리하지 않는다.

§1.1의 확정 사항은 재승인 대상으로 취급하지 않는다. Q1의 호출/필드 결정과 §5.3 판독, §5.4 규칙 JSON/새 월 생성, §5.5 삭제 후 표시값 보존·수식 참조 원칙은 확정됐다. 실제 원본/catalog 대조·이벤트 연결과 삭제 범위/보정 수식/계산 여부/파일·FE 검증은 이 확정과 구별한다. 아래에는 Q2~Q6의 남은 기술·검증 항목만 둔다.

| ID | 결정·확인할 항목 | 추천 또는 필요한 근거 | 구현에 미치는 영향 |
| --- | --- | --- | --- |
| Q2 | 기존 불변 파일·발행 양식/catalog의 실제 seed/Case 연결 등록·제품 조회 검증 | §4.3·§4.5 생성 규칙·§4.4 불변 등록은 유지. 양식/catalog 묶음과 내부 값은 domain §3.3·DDL §6.2/§10B·§4.6에 구체화했으며 새 revision 컬럼 없이 기존 master/Case-template 관계를 사용. 실제 GET 정의/seed·발행/관계 등록과 정확한 네 파일/객체·기준 결합/실패 처리를 검증 | 설계 표현과 실제 적용/등록/조회 시험을 구별. 미완비 정의 저장은 Q5/INPUT-03의 기존 후속. 원본 불변성/식별자 계약 재질문이나 내용 hash·버킷 특수 기능의 새 선행조건 없음 |
| Q3 | 제품 판독기·채택 표시 정책의 적용과 승인 표시 범위 대조/검증 | §5.3 판독/실패와 기존 조회 명세 §3.2 숫자 표시 정책은 확정. 독립 검증 근거는 위 작업 노트를 재사용하고 실제 catalog의 타입/서식/숫자 범위와 제품 reader·서비스·HTTP/FE에서 증명. 빈 결과/저장 오류/결과 부재·0/FALSE·잘못된 타입/참조·공유수식·표시 경계 확인 | 판독·표시 정책을 재질문하지 않고 실제 필요한 지원 범위만 적용/검증. 병합용 null/0 정규화·성능 표본·독립 도구 또는 정책 채택만으로 제품 완료를 대신하지 않음 |
| Q4 | 채택한 §5.1을 최종 표시 projection·catalog/규칙과 결합한 구현 검증 | 로컬 실측 후 큰 표2,000행·작은 표 전체 방향을 채택했고 이번에 페이지/종료/2 MiB/초과 처리를 구체화했다. 같은 단위 결정을 재질문하지 않고 실제 metadata 포함 bytes·행 연결·메모리를 구현에서 확인 | 공용 정본 통합과 endpoint 검증이 남음. 운영 HTTP/보안 경로·동시 요청·브라우저 렌더링은 미검증. cache/worker를 필수화하지 않음 |
| Q5 | 실제 원본/catalog 대조·이벤트 연결·삭제 범위/보정 수식/계산 여부·파일/FE 검증과 추가 규칙 | §5.4의 규칙 JSON/좌표·생성 순서/코드/연결과 §5.5의 삭제 후 Year/Month/Period 보존·수식 참조 원칙은 확정. 실제 월 열/행·선택지/catalog binding·기존 이벤트 연결을 검증하고 실제 삭제 조건의 좌표 대응·경계 보정 수식/범위·계산 여부·파일/FE 결과를 확인. 첫 월/1월/여러 연도/마지막 월 및 전체 월 삭제 허용·초기화는 구분. 추가 의존과 DB/revision은 Q2·domain Q9/기존 DDL/write와 후속 정렬 | 승인 원칙을 재질문하지 않고 구체 삭제 조건·수식/계산·실제 연동/검증을 확정. 형식/논리 예시는 운영 seed·삭제 fixture나 검증 증거가 아님 |
| Q6 | 실제 데이터 경로의 접근/로그/자원 제한 검증 | JSON DRM 비적용은 [기존 조회 명세 §5](mi-fundamental-model-input-provision-feature-spec.md#5-response-and-access-boundary)의 사용자 확정 사항으로 재승인하지 않음. 기존 보안 요구는 유지하며 현재 filter의 본문 로깅 제외 공백은 새 조회 경로 구현 시 해소·검증 | 남은 것은 안전한 제공을 위한 제품 보안 적용/검증. JSON DRM 적용 여부는 차단 항목에서 제거하며 다른 파일 암복호화나 보안 요구의 해제·제품 코드 수정 승인으로 확대하지 않음 |

## 10. 완료 경계와 후속 통합

초기 독립 준비 산출물은 요청·응답·빈 값·오류·대표 예시·남은 결정의 연결과 §1.1의 사용자 확정 반영이다. Q2의 애플리케이션 관리와 사용자 확인 불변 파일 등록은 domain §3.2·DDL §6.1/§10A·이 문서 §4.4에 반영했다. 앞선 내용 해시 컬럼/기존 파일 재등록안은 철회했다. INPUT-02A의 계산 검증/운영 판정과 INPUT-03/04의 전체 완료나 제품 조회 구현/검증을 뜻하지 않는다.

숫자 표시 정책과 JSON DRM 비적용은 기존 조회 feature spec §3.2·§5에 통합했다. 그 밖의 Open Questions와 §3/§6/§7 통합 및 관련 카탈로그·identity 물리 설계가 준비된 조회 slice의 구현은 후속이다. 별도 운영 snapshot/worker가 필요한지는 근거가 확보된 뒤 정한다. 이 문서 반영으로 INPUT-02A 작업이나 티켓 상태를 변경하지 않는다.

- implementation readiness: `blocked by open questions` — 기존 조회·판독·표시/control·월 생성/삭제 원칙과 JSON DRM 비적용은 확정됐고 Q2의 불변 등록/양식·catalog 연결안도 구체화했다. 실제 catalog/정의 저장·seed/발행/Case 관계 등록·제품 projection/판독기/표본과 삭제/파일/FE/접근 검증은 남았다. 이번 문서 구체화는 실제 DB 적용/조회 행동·INPUT-02A/03/04나 제품 구현 완료를 뜻하지 않는다.
