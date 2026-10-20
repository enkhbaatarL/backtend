# 모델 입력편집 — 프론트엔드 API 연동 안내

## Document Control

- status: Review
- canonical for: Reference only — 최초 조사 및 명시한 후속 변경 커밋의 API 사용 안내와 FE 연동 간극
- owner: Shared
- source documents: §10의 백엔드·프론트엔드 커밋 고정 링크와 [공용 작업 노트의 Case105→106 실환경 검증][be-smoke-result]·[C 양식 DRAFT 등록 결과][be-c-registration]. FE 문서는 협의 전 제안 자료이며 최신 합의의 정본이 아니다.
- depends on: 기존 입력 조회·Draft 편집 계약. 이 문서는 새 API나 업무 정책을 승인하지 않는다.
- supersedes: 없음. FE 제안 스펙을 자동으로 대체하지 않는다.
- last synced: 2026-10-19 (FE 최종 JSON·확정 계약/현재 구현 차이 및 열 저장 DB 영향 연결)
- open questions: §9의 출처 기본값, 업로드 미리보기, 표별 편집·구조 변경, FE 화면 연결과 실환경 경합·장애 복구·Kubernetes 검증. F-O-01 기본 성공 흐름은 사용자 시험으로 확인했다.

## 1. 먼저 알아야 할 현재 상태

**현재 백엔드에 맞춰 연결할 흐름은 `저장 Case 선택 → 목록의 draftId로 재개(없으면 생성/재개) → 선택 표 조회 → 변경 임시저장 → 최종저장 → 별도 실행`이다.** Draft는 서버에 보관하는 개인 편집 초안이다. revision은 임시저장이 성공할 때 증가하는 버전 번호다. 목록의 `draftId` 유무에 따른 호출 분기는 §4.1을 따른다.

**합의된 데이터 제공 범위는 네 파일 전체의 화면 구조 정보와 실제 화면에 표시할 선택 표의 데이터다.** [입력 제공 명세 §3][be-provision]에 반영된 결정이며, 2026-10-19 사용자 확인으로 FE 문서가 협의 전 자료임을 재확인했다. FE 문서의 전체 시트 데이터 일괄 반환 제안은 현재 요구사항으로 사용하지 않는다. BE의 표 단위 데이터 제공은 이 합의에 부합하며 요구사항 간극이 아니다.

현재 구현은 전체 구조 조회와 선택 표 데이터 조회를 분리하고, 큰 표의 데이터는 페이지별로 제공한다. 협의 전 FE 제안에 있는 `GET /input-sources`, `GET /inputs?sourceKey=...`, `POST /input-uploads` 경로는 대상 브랜치에 없으므로 실제 호출은 §3을 따른다. 과거 제안 경로의 부재만으로 합의된 계약 위반이라고 판단하지 않는다.

| 구분 | 확인 기준 |
| --- | --- |
| 최초 조사 BE | `deploy/mi-be-test`, `ce557dd5e4784bfd44c594840af0f099b9567e99` |
| 후속 반영 BE | `feature/gas2`의 `015e7581056fce06be15c94af9d88e5c22c47f09` — Case 목록의 개인 `draftId` 추가. 이 변경의 원격 push·배포 여부는 확인하지 않음 |
| 테스트 서버 저장 검증 | 2026-10-19 사용자 실행 결과: `testdb` 단독 웹·별도 Compose worker로 Case105 임시저장→revision1 재조회→Case106 최종저장→새 Case 구조 재조회 통과. 상세 증거는 [공용 작업 노트][be-smoke-result] 참조 |
| BE 원격 추적 ref | 최초 조사 시 로컬 `origin/deploy/mi-be-test`도 `ce557dd5…`였음. 이번 문서 동기화에서 fetch하지 않음 |
| 최초 조사 FE | 로컬 `feature/parkjaehyun`, `4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0`, 당시 작업 파일 변경 없음. 이번 동기화에서 FE 구현은 재조사하지 않음 |
| 조사 방법 | 합의된 BE 명세를 기준으로 FE 실제 호출/화면 코드와 BE 구현 대조. FE Markdown/OpenAPI는 협의 전 배경 자료로 참고 |
| 검증 한계 | 서버/API/SQL 실행은 사용자가 담당했고 제공한 JSON·조회·실행 결과를 대조했다. F-O-01 한 표의 기본 성공 흐름 범위이며 FE 브라우저, 경합·중복 재전송·장애 복구, Kubernetes는 미검증. 아래 JSON 예시는 실서버 식별자를 복사한 것이 아님 |

### 코드 존재와 실제 사용 가능 여부

| 기능 | 코드 기준 상태 | FE 안내 |
| --- | --- | --- |
| Case 목록의 개인 `draftId` | 후속 커밋에서 구현됨 | `miBase`와 `cases` 양쪽에서 본인 편집 Draft ID 또는 명시적 null 제공. 실제 서버에 해당 변경이 반영된 뒤 사용 |
| 원본 구조, Draft 생성/재개·구조·표 조회 | 구현됨 | 원본 4파일, Case와 발행 양식 연결, 조회 정의가 준비된 Case로 연결 |
| 변경 임시저장 접수·결과 조회 | 구현됨, F-O-01 실제 성공 확인 | B8의 0.34→0.35 저장, revision0→1, 같은 rowId와 변경값 재조회 통과. 일반 편집 전체 지원 아님 |
| 최종저장 접수·결과 조회 | 구현됨, F-O-01 실제 성공 확인 | 새 Case106·파일4건·Draft FINALIZED/revision1·Run0과 새 Case 구조 재조회 확인 |
| 단일 파일 업로드 후 서버 미리보기 | 미구현 | 기존 Case 업로드 API로 동일 동작을 구현할 수 없음 |
| 일반 셀 변경, 행 추가/삭제, 열 추가/삭제, control 변경 | 전체 기능 미지원 | 화면에 있는 편집 기능을 그대로 활성화하지 않음 |

현재 실제 변경 처리 범위인 **F-O-01**은 별도로 준비한 `OUTPUT/USCostofSupply/us_cost_of_supply` 표의 **B8 한 셀**, `fieldKey=breakevenPrice`에 **숫자 `0.35`를 SET 한 건으로 보내는 경우**다. 지정 원본과 발행 정의가 있어야 하며 같은 이름의 임의 파일은 대상이 아니다. 보통의 가격 입력이나 US Cost of Supply 전체 표 편집을 뜻하지 않는다. 이 경로는 다른 원본 내용과 수식을 보존하며 일반 수식 재계산 기능을 제공하지 않는다. [처리 범위 근거][be-preserve]

2026-10-19 사용자 시험은 다음 활성화 값으로 진행했다. 웹은 Compose의 Spring 실행 인수로 기본 `false`를 덮어쓰고, 별도 worker는 외부 `worker.yml`을 직접 읽었다. 양식은 Case 연결로 결정하며 현재 application model-code 설정은 필요 없다. 아래 YAML은 활성화 설정값 요약이며 저장소의 `application-testdb.yml`을 수정했다는 뜻은 아니다.

```yaml
mi:
  model-input:
    change-worker:
      enabled: true
    finalization:
      enabled: true
```

최초 조사 때의 비활성 설정을 현재 시험 서버의 저장 차단 사유로 사용하지 않는다. worker는 JDBC의 `serverTimezone=Asia/Seoul`과 Java의 `-Duser.timezone=Asia/Seoul`을 맞춘 뒤 시험을 통과했다. 조기 TIMEOUT의 재현·설정 조치와 시간대 변환 코드 보완 후속은 [실행 기록][be-smoke-result]을 따른다. FE는 계속 서버의 `editing.capabilities`와 필드 `policyStatus`를 사용하며 미지원 정의/동작의 `POLICY_UNSUPPORTED` 처리는 유지한다. [최초 조사 설정][be-config] · [공개 capability 처리][be-service]

### 시험 결과를 FE 연결에 사용할 때

- Case105의 시험 Draft는 이미 `FINALIZED`, 성공 revision은 `1`이다. 그 Draft ID를 편집 중 샘플로 고정하거나 다음 저장 요청에 재사용하지 않는다. Case106은 최종저장으로 생성한 결과 Case다.
- Case106의 QRY-01에서 네 역할과 OUTPUT의 `USCostofSupply` 표를 확인했다. 나머지 역할의 빈 `sheets`와 `tableData:null`은 현재 F-O-01 시험 양식에서 정상이다. 이 응답은 C의 92개 표를 제공하는 전체 양식이 아니다.
- FE는 실제 목록/구조/표 조회에서 받은 ID·revision·상태를 사용한다. 다음 저장 시험의 편집 가능한 Case/Draft는 별도로 준비하고, 이미 완료한 시험 식별자를 예시에 하드코딩하지 않는다.
- Case106 구조 판독은 확인했지만 최종 OUTPUT 파일의 B8을 별도 다운로드나 새 Draft로 직접 재판독한 시험은 수행하지 않았다. 임시저장 revision1의 0.35 재조회와 구분한다.

## 2. 현재 화면에서 바꿔야 하는 부분

아래 BLOCKING은 **전체 입력편집 기능을 연동 완료로 볼 수 없게 하는 차이**다. 문서 작성의 완료를 막는 항목이나 자동 수정 승인은 아니다.

| 우선순위 | 현재 FE와 BE의 차이 | 영향 | 최소 후속 범위 / 담당 |
| --- | --- | --- | --- |
| BLOCKING | FE `saveDraft`는 `WeakMap<File, Map<tableId, SheetTemplateDraft>>`에만 저장. `handleSubmit`은 원래 `File[]`로 `createCase` 호출 | 화면에서 수정한 값이 업로드 파일에 반영되지 않음 | FE: Draft 변경 요청·결과 조회와 최종저장 연결. 원본 파일 업로드 완료를 편집 저장 완료로 표시하지 않기 |
| BLOCKING | 출처 UI는 `EXCEL_UPLOAD` 한 항목 고정. 제안한 출처 목록·기본 키 API는 BE에 없음 | 저장 Case/MI Base로 편집 진입 불가 | FE/기획: 기존 Case 선택을 진입점으로 연결. 기본 출처 정책은 별도 합의 |
| BLOCKING | FE의 `rowPolicy/columnPolicy` 문구 판정과 로컬 `inputRanges`가 편집 버튼을 결정. BE는 `capabilities`와 필드 `policyStatus`를 제공 | 지원하지 않는 수정·추가·삭제 UI가 열릴 수 있음 | FE: §5 조건으로 활성화. BE: 실제 지원할 표별 정책·처리 구현은 별도 작업 |
| BLOCKING | FE가 요구하는 상세 `coordinates`와 BE 공개 `coordinates`가 다름 | 다중 헤더, 제외 열, 기간 열, 부속 입력 표현을 기존 코드에 그대로 주입할 수 없음 | FE: 반환 열·헤더·control을 직접 사용. 부족한 표별 표현은 BE와 확인 |
| BLOCKING | 단일 파일 즉시 업로드·파싱 API가 없음 | FE 문서의 파일 하나씩 미리보기 흐름 구현 불가 | BE/기획: 별도 업로드 계약·구현 확정. 4파일 Case 생성으로 암묵 대체하지 않기 |
| BLOCKING | F-O-01 외 실제 변경 처리 미지원 | 한 표의 실환경 성공만 확인했으므로 일반 셀·행·열 편집 저장은 현재 완료할 수 없음 | BE: 승인된 필요한 표부터 별도 확장. FE: 확인된 제한 범위로 저장·결과 조회 연결 |
| BLOCKING | C의 92개 조회 표는 새 DRAFT 양식으로 등록했지만 미발행·Case 미연결이며 7항목 보류 | FE mock에 있는 모든 표를 서버가 내려준다고 보장할 수 없음 | BE: 발행·새 Case 연결과 보류 항목은 별도 후속. FE: 서버가 준 목록만 표시하고 미지원 항목을 mock으로 채우지 않기 |
| WARNING | FE `초기화`는 현재 표의 로컬 초안 삭제, 다운로드는 원본 `File` 다운로드 | 서버 저장분 되돌리기나 편집본 다운로드로 오해 가능 | FE/기획: 로컬 미저장분 취소로 의미 제한. 서버 되돌리기·Draft 다운로드가 필요하면 별도 계약 |

근거: [입력 팝업][fe-popup]의 `saveDraft`·`resetCurrentSheet`·`handleSubmit`, [시트 로더][fe-table], [로컬 파서 호출][fe-preview], [공통 표][fe-summary], [배치·편집 판정][fe-layout], [BE 조회 서비스][be-service].

조회 방식은 합의가 끝났고 FE 구현 연결이 남았다. 현재 `File + sheetName` 로컬 파싱 경로를 `draftId + revision + tableId + startRow`의 서버 조회로 연결하는 작업은 §4·§9에 설명한다. 전체 구조와 선택 표 데이터를 나누는 방식 자체는 위 간극 목록에서 제외했다.

**검토 판정: 전체 화면의 조회·편집·최종저장 연동은 `REQUEST CHANGES`.** 합의된 제공 범위에 대한 실제 FE 연결 상태와 BE 저장 지원 범위를 기준으로 한 판정이다. 데이터 제공 단위를 다시 협의하거나 전체 시트 일괄 조회 API를 추가하자는 뜻은 아니다.

## 3. 기본 주소, 권한, API 목록

Base path: `/api/v1/mi/fundamental-model`

- 기존 로그인 클라이언트를 사용한다. Draft API 권한 선언은 `USER + SKGAS_COMPANY_USER_ONLY` 또는 `ADMIN + THIRD_PARTY_ADMIN_ONLY`다.
- Draft의 회사·사용자는 인증 문맥에서 결정한다. request body에 `companyNo`, `userId`, 파일 저장 경로를 추가하지 않는다. 다른 회사·사용자의 Draft는 `404`다.
- Draft Controller는 `@Profile("!dedicated")`다. 기존 FE의 파일 업로드·Run 호출에 쓰는 `withCustomBaseUrl`을 Draft API에 그대로 적용하지 말고, 공통 BE로 연결한다. 실제 프록시 주소는 배포 환경에서 확인한다.
- 기존 Axios `api`의 baseURL은 `/api/v1/`이므로 일반 호출은 `mi/fundamental-model/...` 형태다. 서버가 주는 `/api/v1/...` 상대 경로는 앞에 prefix를 다시 붙이지 않는다.

| 번호 | Method | Base path 이하 | 요청 | 성공 응답과 의미 |
| --- | --- | --- | --- | --- |
| QRY-01 | GET | `/cases/{caseId}/inputs/structure` | 양의 정수 `caseId` | `200`, 원본 식별값·네 종류 구조. 셀 값 없음 |
| EDT-01 | POST | `/cases/{caseId}/input-drafts` | JSON `{sourceVersion}` | `200`, 생성 또는 재개 요약. `created`로 구분 |
| EDT-02 | GET | `/input-drafts/{draftId}` | 소문자 UUID | `200`, 현재 Draft 요약·구조 |
| EDT-03 | GET | `/input-drafts/{draftId}/table-data` | query `revision`, `tableId` 필수; `startRow` 선택 | `200`, 선택 표·page·editing |
| EDT-04 | POST | `/input-drafts/{draftId}/change-requests` | JSON `{requestId,baseRevision,operations}` | 신규/진행 중 `202`, 종료된 동일 요청 재전송 `200` |
| EDT-05 | GET | `/input-drafts/{draftId}/change-requests/{operationId}` | 두 소문자 UUID | `200`, 임시저장 작업 상태. 실패 작업 조회도 `200` |
| EDT-06 | POST | `/input-drafts/{draftId}/finalizations` | JSON `{requestId,baseRevision,title,description?}` | 신규/진행 중 `202`, 종료·완료 재조회 성격의 요청 `200` |
| EDT-07 | GET | `/input-drafts/{draftId}/finalizations/{operationId}` | 두 소문자 UUID | `200`, 최종저장 작업 상태. 성공에만 새 Case 제공 |

위 8개는 [ModelInputDraftController][be-controller]에 있는 경로다. 제안서의 원본 선택 표 조회 `QRY-02`는 최초 조사 커밋에 Controller가 없다. 현재 표 값 조회에는 **기존 Draft 재조회 또는 생성/재개 후 EDT-03**을 사용한다.

기존 API와의 연결:

| Method / Path | 이 기능에서의 역할 |
| --- | --- |
| `GET /cases` | `data.miBase`와 `data.cases`에서 Case 후보와 개인 `draftId` 조회. `miBase` 자체는 null 가능. `draftId`가 있으면 EDT-02로 재개하며, 없으면 QRY-01 → EDT-01 사용. `defaultSourceKey`나 편집 준비 여부는 제공하지 않음 |
| `POST /cases` | query `title`, 선택 `description` + multipart `files` 4개로 Case 생성, `201`. 셀 데이터 미리보기 응답이 아님 |
| `GET /cases/{caseId}` | 최종저장 성공 후 새 Case 상세 조회 |
| `POST /cases/{caseId}/runs` | 새 Case를 실제 실행, `202`. 최종저장과 별개 호출 |

Case가 목록에 있어도 양식 연결·조회 정의가 없으면 QRY-01은 실패할 수 있다. 임의의 Case ID를 정상 편집 가능한 샘플로 하드코딩하지 않는다. [Case 조회][be-cases] · [업로드·Run][be-run] · [FE 기존 서비스][fe-service]

## 4. 조회 연동 순서와 화면 데이터

### 4.1 진입 및 재개

먼저 `GET /cases`에서 `miBase` 또는 `cases[]`의 항목을 선택한다. 원본은 active `INDEPENDENT` 또는 `MI_BASE`이며, 항목 타입에 `draftId: string | null`을 추가한다. `miBase=null`은 현재 MI Base 자체가 없는 경우이고, `miBase.draftId=null`은 MI Base는 있으나 본인의 편집 Draft가 없는 경우다.

목록의 `draftId`는 **현재 회사·로그인 사용자가 해당 Case를 원본으로 편집 중인 미삭제 `DRAFT`**만 가리킨다. 다른 회사·사용자 및 `FINALIZED`/`DISCARDED`/삭제 Draft는 제외하며, 없으면 필드를 생략하지 않고 null을 반환한다. Case 업로더의 Draft나 해당 Case를 최종저장한 완료 Draft를 뜻하지 않는다. 목록 조회는 Draft를 생성하지 않는다. 필드 계약의 정본은 [Case 목록 계약 §3][be-list-contract]이다.

| 선택한 항목의 `draftId` | 호출 순서 | FE 처리 |
| --- | --- | --- |
| UUID 문자열 | EDT-02 `GET /input-drafts/{draftId}` | QRY-01과 EDT-01을 거치지 않고 현재 저장된 Draft 구조·revision을 재조회 |
| `null` | QRY-01 `GET /cases/{caseId}/inputs/structure` → EDT-01 `POST /cases/{caseId}/input-drafts` → EDT-02 | QRY-01의 `sourceVersion`을 EDT-01에 보내고, 응답의 `draftEndpoint`로 구조 조회 |

예를 들어 선택한 항목의 `draftId`가 `aefb60dc-18b8-4ac2-beb4-a4d499950765`라면 다음 요청으로 바로 재개한다.

```http
GET /api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765
```

`null` 경로의 EDT-01은 `created=true`면 신규 생성, `false`면 기존 Draft 재개이며 모두 HTTP `200`이다. 목록 조회 뒤 다른 탭에서 Draft가 생성될 수 있으므로 `null`이었다고 신규 생성을 단정하지 않는다. `sourceVersion`을 `sourceKey`로 대신 보내지 않는다.

두 경로는 EDT-02 이후 같은 흐름을 사용한다.

1. `sections → sheets → tables`로 탭과 표 선택을 만든다. `draftId`가 있다는 이유로 revision을 0으로 가정하지 않는다.
2. `activeOperationEndpoint`가 있으면 해당 작업 조회를 이어간다. 작업 중 새 변경/최종저장을 보내지 않는다. 목록 조회 후 최종저장이 완료되어 EDT-02가 `FINALIZED`를 반환하면 `finalizedCaseId`로 이동한다.
3. 표를 선택하면 EDT-02의 현재 `revision`과 서버의 `tables[].id`로 EDT-03을 호출한다.

목록 ID는 즉시 편집 가능함을 보장하지 않는다. 접근 상실/원본 변경 오류는 §7대로 처리하며, 오류를 숨기고 새 Draft를 자동 생성하지 않는다. 로그인 사용자나 회사 문맥이 바뀌면 Case 목록도 재조회하여 이전 사용자의 `draftId`를 재사용하지 않는다. [목록 구현][be-cases] · [목록 항목 DTO][be-list-dto]

`draftId=null`인 경우의 QRY-01 요청 예시:

```http
GET /api/v1/mi/fundamental-model/cases/103/inputs/structure
```

EDT-01 요청 예시. 아래 Case 번호와 `sv1_...`는 예시이며 반드시 QRY-01의 실제 값을 사용한다.

```json
{
  "sourceVersion": "sv1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
}
```

EDT-01 성공 응답 예시:

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "draftId": "aefb60dc-18b8-4ac2-beb4-a4d499950765",
    "sourceCaseId": 103,
    "sourceKey": "sc1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "sourceVersion": "sv1_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "status": "DRAFT",
    "statusLabel": "편집 중",
    "revision": 0,
    "activeOperationId": null,
    "activeOperationEndpoint": null,
    "finalizedCaseId": null,
    "draftEndpoint": "/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765",
    "created": true
  }
}
```

최상위 `status`는 요청 성공 여부인 boolean, `data.status`는 Draft 또는 작업 상태인 문자열이다. `data.summary`라는 중첩 필드는 없다. EDT-02는 위 요약에서 `created`를 제외하고 `sections`, `tableData:null`을 추가한다. QRY-01은 `sourceKey`, `sourceVersion`, `sections`, `tableData:null`만 제공한다. [응답 DTO][be-dto]

### 4.2 구조와 선택 표의 대응

| 화면 개념 | 응답 필드 | 사용 방법 |
| --- | --- | --- |
| Demand/Supply/Trade/Output 탭 | `sections[].kind` | `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 순서 |
| 종류별 원본 식별 | `sections[].inputId` | 원본 역할·파일·발행 기준의 키. Draft 변경 버전을 대체하지 않음 |
| 시트 버튼 | `sections[].sheets[].sheetName` | 서버 순서를 유지 |
| 표 버튼 | `sheets[].tables[].id`, `name`, `description` | `id`는 그대로 전송. `description`은 null 가능 |
| 위치 정보 | `tables[].location`, `coordinates` | 현재 `coordinates`는 `headerRow`, `bodyStart`만 공개. 헤더 없는 표의 `headerRow`는 0 가능 |
| 셀 표시 | EDT-03 `tableData.columns`, `tableData.rows` | 선택 표에 반환된 열·행만 표시. `sheets[].data`는 없음 |
| 부속 입력/선택 UI | `tables[].controlIds` → `tableData.controls` | grid 외의 명시적 control. 존재만으로 변경 API 지원을 뜻하지 않음 |
| 화면 갱신 규칙 | `tableData.displayUpdateRules` | 서버가 제공한 정의를 확인해 적용. 현재 FE는 이 필드를 소비하지 않음 |
| 실제 편집 값/대상 | `editing.fields`, `rowBindings`, `editValues` | §5의 안정 식별자와 타입을 사용 |

셀 표현 중 기존 FE와 같은 부분:

- 열 `column0=A`, `column1=B`처럼 Excel의 0부터 시작하는 열 인덱스를 유지한다. 제외 열이 있어도 재번호화하지 않는다.
- 행 `rowNumber`는 Excel의 1부터 시작하는 행 번호이고, 표시용 `id=rowNumber-1`이다.
- `rows[].columnN`은 서버가 만든 표시값이다. 수식 문자열을 내려받아 FE가 계산하는 방식이 아니다. 빈 셀 필드는 생략될 수 있고 `0`과 빈 값은 구분한다.
- `inputCells`는 `rowNumber * 16384 + zeroBasedColumnIndex`다. 예: `B8=131073`, `G11=180230`.

다른 부분:

- 실제 `tableData.columns`는 불연속일 수 있다. FE의 `getSummaryTemplateLayout`처럼 `location`의 처음~끝 열을 전부 생성하면 제외된 열이 다시 나타날 수 있다. 서버의 columns 배열을 기준으로 한다.
- BE는 발행 조회 정의에 따라 여러 헤더 행을 `rows`에 포함하지만, FE가 요청한 `coordinates.headerRows`, `periodYearRow`, `periodMonthRow`, `inputRanges`, `clearColumnsOnEdit` 등을 그대로 반환하지 않는다.
- 페이지의 `editing.rowBindings`는 본문 행에만 있다. 반환된 `rows` 중 본문과 헤더를 구분해 렌더링하고, 기존 로컬 좌표 설정을 서버 응답의 누락값 보충에 사용하지 않는다.
- 일반 BE 표 ID도 `kind/sheetName/name` 형태지만 이름은 발행 카탈로그에 달려 있다. F-O-01의 `.../us_cost_of_supply`와 FE mock의 `.../US Cost of Supply`는 다르다. 대소문자/공백/슬래시를 수정하지 말고 query parameter로 인코딩한다. 특정 문자열에 묶인 FE 전용 템플릿 분기도 대조해야 한다.

### 4.3 큰 표 페이지 조회

첫 요청은 `startRow`를 생략한다. `revision`은 0 이상 `9007199254740991` 이하 정수이며 필수다.

```http
GET /api/v1/mi/fundamental-model/input-drafts/{draftId}/table-data?revision=0&tableId=OUTPUT%2FUSCostofSupply%2Fus_cost_of_supply
```

EDT-03의 `data`는 `{draftId, revision, sourceKey, sourceVersion, tableData, page, editing}`이다.

| page 필드 | 의미 |
| --- | --- |
| `totalBodyRows` | 표 전체의 본문 행 수. 빈 슬롯 포함, 헤더 제외 |
| `returnedBodyRows` | 이번 응답의 본문 행 수. 최대 2,000 |
| `startRow`, `endRow` | 이번 본문의 Excel 행 번호. 본문 없으면 null |
| `hasNext` | 다음 본문 페이지 존재 여부 |
| `nextStartRow` | 다음 요청에 그대로 넣는 값. 마지막 페이지면 null |

응답 전체는 2 MiB 한도여서 2,000행보다 적게 올 수 있다. `startRow + 2000`으로 다음 위치를 계산하지 않는다. `hasNext=false`에서 종료한다. 헤더는 페이지마다 반복되므로 누적 렌더링 시 중복 제거하고, 본문은 원래 행 번호를 유지한다. 한 행과 metadata만으로 한도를 넘으면 `422 / RESPONSE_TOO_LARGE`다.

출처/표 전환 또는 저장 성공으로 revision이 바뀌면 이전 요청 결과를 현재 화면에 섞지 않는다. 캐시를 사용한다면 인증 회사·사용자 문맥과 `draftId, revision, tableId, startRow`를 구분한다. 원본 `sourceVersion`과 `inputId`는 Draft 저장마다 바뀌는 키가 아니므로 그것만으로 캐시를 나누면 오래된 값이 남는다.

Regas처럼 다른 표를 참조하는 화면은 그 표도 같은 Draft·revision으로 별도 조회한다. 선택 표 응답에 참조 시트 전체가 포함됐다고 가정하지 않는다. [조회 구현][be-service] · [기존 연결 시트 로더][fe-table]

## 5. 편집 가능 여부와 임시저장

### 5.1 버튼과 셀 활성화

`editing.capabilities`는 표 전체의 동작 준비 상태, `editing.fields[].policyStatus`는 각 필드의 준비 상태다.

| 상태 | 의미 | FE 동작 |
| --- | --- | --- |
| `READY` | 해당 동작을 사용할 수 있음 | 다른 필드/행/제약 조건까지 확인 후 허용 |
| `INCOMPLETE` | 필요한 정책·정의가 미완성 | 비활성화 |
| `NOT_ALLOWED` | 해당 동작 불허 | 비활성화 |
| `UNSUPPORTED` | 현재 처리 구현/설정에서 미지원 | 비활성화 |

기존 셀 SET은 Draft가 `DRAFT`이고 진행 작업이 없으며, **표 SET과 필드 SET이 모두 READY**, 해당 주소가 `inputCells`에 포함되고 `rowBindings`에 유효한 `rowId`가 있을 때만 연다. `setConstraints`도 적용한다. `inputCells`나 필드 정책만 보고 열면 안 된다. 현재 구현에서는 그 둘에 편집 표시가 있어도 표 capability가 `UNSUPPORTED`일 수 있다.

- `rowPolicy/columnPolicy`는 현재 `추가 가능` 또는 `고정`으로 제공된다. FE의 `아래 행 추가`, `연월 추가` 등 문자열 비교와 다르며 저장 허용 판정값으로 사용하지 않는다.
- `rowNumber`와 `rows[].id`는 표시 위치다. 저장 대상은 `rowRef.rowId`다. `rowId:null, origin:EMPTY_SLOT`에는 SET할 수 없다.
- `fields[].columnField`로 화면 열을 찾고 `fields[].fieldKey`로 저장한다. `column1`이나 `B8`을 `fieldKey`로 보내지 않는다.
- 편집 시작값은 `editValues[].values[fieldKey]`다. 반올림·쉼표·퍼센트가 포함될 수 있는 표시 문자열을 역변환해 전체 행을 보내지 않는다.
- 타입은 `TEXT`, `INTEGER`, `DECIMAL`이다. 숫자는 JSON number이며 숫자 문자열·null·boolean·객체·배열을 숫자 입력으로 보내지 않는다. 큰 정수/소수는 기본 JavaScript number 파싱으로 자릿수가 손실될 수 있으므로 손실 없는 파싱·직렬화 방식을 확인해야 한다.
- `CLEAR`, `ADD_ROW`, `DELETE_ROW`는 공통 요청 스키마에 존재하지만 실제 처리 범위는 F-O-01 SET뿐이다. 확정된 열 계약과 현재 구현 차이는 §9를 따른다. 열 capability/field 상태가 누락되면 기능을 비활성화한다. ADD_COLUMN은 표 READY, DELETE_COLUMN은 표와 선택 field가 모두 READY인 경우에만 연다. control 쓰기는 별도 후속이다.

### 5.2 현재 지원하는 SET 예시

아래 rowId는 예시다. 실제 요청에는 현재 표의 `rowBindings`에서 B8에 대응하는 rowId를 사용한다. 서버가 F-O-01 SET을 READY로 제공한 경우에만 보낸다.

```json
{
  "requestId": "aefb60dc-18b8-4ac2-beb4-a4d499950767",
  "baseRevision": 0,
  "operations": [
    {
      "type": "SET",
      "commandId": "set_price_1",
      "tableId": "OUTPUT/USCostofSupply/us_cost_of_supply",
      "rowRef": { "rowId": "aefb60dc-18b8-4ac2-beb4-a4d499950768" },
      "fieldKey": "breakevenPrice",
      "value": 0.35
    }
  ]
}
```

EDT-04로 보낸다. `requestId`는 새 저장마다 만드는 소문자 UUID다. `commandId`는 요청 안에서 유일한 `[A-Za-z][A-Za-z0-9_-]{0,63}` 형식이다. 공통 본문 제한은 UTF-8 1 MiB, 깊이 8, operations 1~500개지만 **현재 지원 사례는 SET 한 건**이다. 알 수 없는 필드도 거절하므로 화면용 상태를 요청에 덧붙이지 않는다.

새 접수는 `202`이고 `data`는 다음 작업 정보다.

| 필드 | 의미 |
| --- | --- |
| `operationId`, `requestId`, `draftId` | 서버 작업·클라이언트 요청·초안 식별자 |
| `status`, `statusLabel` | `QUEUED/RUNNING/SUCCEEDED/FAILED`와 표시명 |
| `baseRevision`, `resultRevision` | 기준 버전, 성공 버전. 성공에만 `resultRevision=baseRevision+1` |
| `rowMappings` | 성공한 신규 행 대응. 현재 SET 사례는 빈 배열 |
| `affectedTableIds` | 성공 후 재조회할 표. 성공 전/실패는 빈 배열 |
| `failure` | FAILED에만 오류. 나머지는 null |
| `statusEndpoint` | EDT-05 상대 경로 |

### 5.3 저장 대기·성공·실패

1. `QUEUED/RUNNING`이면 `statusEndpoint`를 **2초 간격**으로 조회한다. 접수 응답의 `Retry-After: 2`와 `Location`도 같은 흐름을 안내한다.
2. `SUCCEEDED`에서 반복 조회를 끝낸다. `resultRevision` 기준으로 Draft 구조와 영향받은 표를 다시 받고, 성공 확인한 로컬 변경분을 정리한다.
3. `FAILED`에서도 반복 조회를 끝낸다. 직전 성공 revision과 값을 유지하며 `failure.reason/issues`를 표시한다. 사용자 수정분은 복구·재검토할 수 있게 별도로 보관한다.
4. 작업 조회 자체는 FAILED여도 HTTP `200`, 최상위 `status:true`, `code:SUC-000`이다. **저장 성공은 `data.status === 'SUCCEEDED'`로 판단한다.**

응답이 유실되거나 통신/500으로 결과를 모를 때는 새 UUID로 다시 저장하지 않는다. 같은 `requestId`와 같은 body로 재전송하거나, 이미 받은 `statusEndpoint`를 조회한다. 같은 requestId에 다른 내용을 보내면 `IDEMPOTENCY_CONFLICT`다. 실패가 확정된 작업의 같은 요청 재전송은 실패 기록을 돌려주므로, 다시 시도하려면 현재 Draft를 확인하고 새 requestId를 만든다. CHANGE와 FINALIZE는 requestId 중복 키를 공유한다.

## 6. 최종저장과 모델 실행

미저장 변경을 먼저 EDT-04/05로 성공시킨 뒤, 진행 작업이 없는 현재 revision으로 EDT-06을 호출한다. 변경 없는 revision 0도 허용된다.

```json
{
  "requestId": "aefb60dc-18b8-4ac2-beb4-a4d499950769",
  "baseRevision": 1,
  "title": "입력값 조정 검토",
  "description": null
}
```

- `title`: 필수 문자열. 서버 trim 후 1~200자.
- `description`: 선택 문자열 또는 null. UTF-8 65,535 bytes 이하. 생략은 null, 빈 문자열은 그대로 보존한다.
- `202`는 저장 작업 접수다. EDT-07을 2초 간격으로 조회하며 저장/자동 실행의 종료 조건은 아래를 따른다.
- 성공 시 `finalizedCaseId`, 역할별 `files[{role,fileNo}]` 4건, `caseEndpoint`가 제공된다. 성공 전/실패는 Case ID와 경로가 null, files는 빈 배열이다. 파일 배열은 `role`로 매핑한다.
- 저장 성공 시 새 `INDEPENDENT/PENDING` Case와 실행 대기가 생기고 Draft는 `FINALIZED`가 된다. BE 실행 접수 후 Case는 `RUNNING`이 된다. Draft revision은 증가하지 않는다.
- 성공 후 Case 목록을 재조회한다. 완료된 Draft는 목록의 편집 재개 ID에서 제외되며, 새 Case의 `draftId`도 현재 회사·로그인 사용자가 그 Case를 원본으로 새로 편집 중인지에 따라 결정된다.
- **최종저장 성공 후 BE가 모델 Run을 자동 접수한다.** FE는 별도 Run POST를 호출하지 않는다. 저장 SUCCEEDED 후에도 `execution.status=QUEUED/RUNNING`이면 같은 최종저장 결과를 2초 간격으로 조회하고 COMPLETED/FAILED에서 멈춘다. execution=null인 과거 성공 건은 자동 실행 대상이 아니다. 실행 실패에도 Case/파일과 저장 성공은 유지하며 `execution.failureReason`을 표시한다. 같은 최종저장 재전송으로 재실행하지 않는다. 명시적 실행 재시도는 기존 Case Run API를 사용한다. [최종저장 계약 §9.2.1][be-edit-spec]을 따른다.
- 현재 최종저장이 재생성할 수 있는 입력은 revision 0 원본 4개 또는 지원하는 F-O-01 성공 이력이다. 지원하지 않는 편집 이력을 무시하고 파일을 만드는 방식은 제공하지 않는다.
- 이미 최종저장된 동일 내용은 원래 작업/Case를 돌려준다. 제목·설명·기준 revision을 바꿔 같은 Draft를 다시 최종저장하는 흐름으로 사용하지 않는다.

FE 화면의 완료 문구는 `임시저장 완료`, `최종저장 완료`, `모델 실행 접수/완료`를 구분해야 한다. [최종저장 계약 §9][be-edit-spec] · [실제 Controller][be-controller] · [FE 완료 처리][fe-list]

## 7. 오류 처리

FE 제안의 `{code,message,details}` 대신 공통 envelope를 사용한다. 대표 Draft 오류 예시:

```json
{
  "status": false,
  "code": "BIZ-001",
  "message": "The operation violates a business rule.",
  "data": {
    "reason": "REVISION_CONFLICT",
    "currentRevision": 2,
    "issues": []
  }
}
```

| HTTP / code | reason 또는 조건 | FE 처리 |
| --- | --- | --- |
| `400 / REQ-003`, `REQ-005` | `INVALID_REQUEST` | 필수 필드·JSON 타입·UUID·허용 key 확인. `issues[].path`가 있으면 해당 입력과 연결 |
| `404 / RSC-001` | Draft/표/작업 없음 또는 접근 불가. `data:null` | 현재 화면/반복 조회 종료, Case 목록 재조회·재선택 안내. 목록에서 받은 ID여도 동일하게 처리 |
| `409 / BIZ-001` | `REVISION_CONFLICT` | 최신 Draft와 표를 재조회. 기존 변경분을 새 revision에 자동 덮어쓰지 않음 |
| 같은 409 | `DRAFT_BUSY` | EDT-02의 `activeOperationEndpoint`로 진행 작업 확인 |
| 같은 409 | `SOURCE_CHANGED`, `DRAFT_SOURCE_CONFLICT` | 원본 구조·선택 재확인. 다른 원본에 기존 초안 자동 적용 금지. 폐기/강제 재생성 API는 현재 없음 |
| 같은 409 | `POLICY_INCOMPLETE`, `POLICY_UNSUPPORTED` | 준비 안 됨/미지원 안내. 동일 요청 무한 재시도 금지 |
| 같은 409 | `DRAFT_NOT_EDITABLE` | 상태 재조회. 최종저장 완료면 새 Case로 이동 |
| 같은 409 | `IDEMPOTENCY_CONFLICT` | 같은 requestId로 다른 내용을 보냈는지 확인 |
| `413 / REQ-003` | `PAYLOAD_TOO_LARGE` | 요청 크기 확인. FE가 제안한 파일당 50 MiB와 다른 JSON 본문 제한 |
| `422 / BIZ-001` | `VALIDATION_FAILED` | `issues`에 있는 `commandId/tableId/path/reason`으로 수정 위치 안내. F-O-01의 다른 숫자/문자열은 `VALUE_NOT_ALLOWED` 가능 |
| `422 / BIZ-001` | `RESPONSE_TOO_LARGE` | BE에 표 조회 정의/응답 크기 확인 요청. 빈 성공으로 처리하지 않음 |
| `500 / SYS-001` | `data:null` | 내부 오류. 쓰기 결과 불명 시 동일 requestId로 결과 확인. 내부 원문을 화면에 요구하지 않음 |

QRY-01 원본 구조 오류는 **항상 `data:null`**이므로 `data.reason`을 전제로 파싱하지 않는다. 인증/권한 오류는 기존 공통 처리도 유지한다. 작업 조회의 업무 실패는 위 HTTP 오류와 달리 `200 / data.status=FAILED / data.failure`에 온다. [오류 생성][be-errors] · [응답 처리][be-controller]

## 8. 표 목록의 준비 상태와 FE 화면 연결 과제

최신 [카탈로그 구조 조사][be-catalog]의 99개 표시 항목 중 **92개 조회 표**는 사용자 SQL 실행으로 `DRAFT_C_STRUCTURE` version1의 새 DRAFT 양식에 등록됐다. 역할4·시트70·표92·수정범위0·Case 연결0·관련 Draft0을 확인한 [등록 결과][be-c-registration]를 따른다. ACTIVE 발행과 Case 연결은 하지 않았다. 현재 Repository는 각 Case에 연결된 `ACTIVE/DEPRECATED` 양식을 읽으므로, DRAFT 등록만으로 FE에 92개 표가 나타나지 않는다. application model-code 설정은 필요 없으며 기존 Case103·105 연결을 C 양식으로 바꾸지 않는다.

SQL 초안은 일반 표의 capability/필드 정책을 `INCOMPLETE`, 읽기 전용 LNG Summary를 `NOT_ALLOWED`로 기록하며 수정 범위를 등록하지 않는다. 표가 표시되는 것과 저장할 수 있는 것은 별개다. 아래 7개는 초안의 표 등록에서 보류됐다.

| 종류 | 표 / 시트 | 남은 확인 |
| --- | --- | --- |
| DEMAND | LNG Demand Mode / `LNG Demand` | C1 control·표 연결·선택지 |
| DEMAND | Annual LNG Demand Reference / `LNG Demand` | 국가/소계/TOTAL·기간 헤더·control 연결 |
| DEMAND | Hydrogen Gas Conversion Factor / `HydrogenInput` | 고정 라벨과 단일 값의 대응 |
| OUTPUT | AnnualPriceCalc Spread / `AnnualPriceCalc` | 같은 행 라벨·상하한 값의 대응 |
| SUPPLY | Annual Regas Capacity by Country / `RegasAdjustment` | 수식 행의 존재/행 식별 기준 |
| SUPPLY | Monthly Pipeline Capacity Adjustment / `PipelineAdjustment` | 수식 행의 존재/행 식별 기준 |
| TRADE | Pacific Charter Cost / `ShippingCostInput` | C3 control과 본문 분리 |

FE에서 특히 대조할 부분:

- `sheetTemplateRegistry`는 LiqUtilAdjustment·BCRegasification·RegasAdjustment 세 전용 컴포넌트를 특정 표 ID로 선택한다. 실제 발행 ID와 조회 데이터 범위를 확인한다.
- 다중 헤더·월/연도 추가·Regas 연결 화면은 로컬 `summaryTemplateCoordinates`와 시트 전체 데이터에 의존한다. 서버 표 페이지를 그대로 전달하는 것만으로 같은 화면을 보장하지 않는다.
- Demand Overwrite의 숨김 값 비우기, Supply 파생 열, Regas 연결 계산 등은 FE 문서에 있어도 현재 일반 저장 처리에 연결됐다는 근거가 없다. 로컬 계산/초안 동작을 서버 최종 파일 반영으로 간주하지 않는다.
- 저장 Case 기반 경로에는 이 API 묶음의 원본/Draft 파일 다운로드가 없다. FE의 “인풋 파일 다운로드”는 현재 로컬 파일에만 동작하며, 편집본 다운로드로 안내할 수 없다.

## 9. 권장 연동 범위와 확인 순서

2026-10-19 행·열 형식의 사용자 필수 결정은 완료됐으며, FE 전달용 최종 JSON은 [편집 명세의 값·행 사례 목록](mi-fundamental-model-input-draft-editing-api-proposal.md#fe-change-cases-20261019)과 [열 추가·삭제·결과·오류 JSON](mi-fundamental-model-input-draft-editing-api-proposal.md#fe-final-column-json-20261019)을 사용한다. 표별 대상은 [SUMMARY 검토표](../references/model-input/2026-10-19-summary-structure-review.md)의 원문과 후속 확정 정책을 함께 본다. **계약은 확정됐지만 현재 서버는 열 쓰기를 지원하지 않는다.** 실제 FE 담당자의 확인/연동 시험 완료로 간주하지 않는다.

### FE 전달용 최종 JSON 안내

| FE에서 준비할 케이스 | 정본 예시 | 적용할 핵심 |
| --- | --- | --- |
| 기존 값 수정·행 추가/삭제·CLEAR·충돌/실패 | 편집 명세 §7.3~7.11 | 기존4명령·rowId/clientRowKey·revision/requestId·Problem 유지 |
| LiqUtil 새 월 | §7.13 | headerValues:{}; 자동 Year/Month/Period, 본문은 빈칸 |
| LiqProd 새 월 | §7.13 | year/month 직접 입력, Period 서버 생성. SET0과 미입력 구분 |
| 연도8표의 새 열 | §7.14 | year 직접 입력. 기본값/필수/계산은 표별 정책; BCRegas0을 전체에 적용하지 않음 |
| 기존 열 삭제 | §7.15 | fieldKey로 선택. 삭제 뒤 같은 키로 남은 열을 다시 연결하고 현재 좌표 재조회 |
| 같은 요청의 추가→삭제·같은 연월 두 열 | §7.16 | clientColumnKey, DELETED mapping 유지·키 미재사용; 같은 날짜도 별개 fieldKey |
| 입력 오류·새 열 생성 안내 | §7.17~7.18·§3.3 | 기존 Problem·headerValues/body metadata, READY 조건 충족 때만 입력창 활성화 |

공통 전송은 기존 EDT-04의 operations다. SET/CLEAR/DELETE_COLUMN은 fieldKey 또는 같은 요청에서 앞서 만든 clientColumnKey 중 하나만 보낸다. 신규 행×신규 열은 두 ADD 뒤 SET한다. 성공 후 mapping을 반영하고 EDT-02/03의 revision·구조/영향 표를 재조회한다. 진행/실패·신규 열 없는 성공에도 최종 계약의 columnMappings는[]다. 과거 서버가 이 필드를 생략하는 것은 이번 확장의 미구현 상태이므로 그 응답으로 열 쓰기를 활성화하지 않는다.

### 확정 계약과 현재 BE 구현 차이

2026-10-19 작업 폴더 코드를 확인한 결과다. 상세 코드 근거는 [편집 명세 §10.2](mi-fundamental-model-input-draft-editing-api-proposal.md#fe-contract-implementation-gap-20261019)에 있다.

| 구간 | 확정 계약 | 현재 코드·FE 주의점 |
| --- | --- | --- |
| 요청 | 6명령·headerValues/clientColumnKey | 공통 parser는6명령/임시 열 참조 수용. 형식400·정책 누락409 INCOMPLETE·실제 처리기 부재409 UNSUPPORTED. 일반 열/행 처리는 미지원 |
| 조회 | 6capabilities·필드 DELETE_COLUMN·columnCreation | 6capabilities/필드 DELETE_COLUMN 제공. 누락 열 정책은 INCOMPLETE, 실제 처리기가 없는 READY는 UNSUPPORTED. columnCreation은 공개 ADD_COLUMN READY일 때만 제공 |
| 결과 | OperationView.columnMappings 항상 배열 | 공통 DTO/성공 저장 결과에 구현. 진행/실패/신규 열 없는 성공은[] 제공. 미배포 v1 Draft/요청 이력은 수동 초기화 후 새 Draft 사용 |
| 처리·재조회·최종파일 | 안정 열 키와 값/헤더/위치 보존 | v2 공통 열 출처/이동·삭제/overlay·신규 행×열 복원은 격리 fixture 검증. 실제 처리기는 지정 F-O-01 SET 한 건·v2 최종4파일 회귀. 실제 열 처리·최종파일 지원은 후속 |
| DB | 새 열과 삭제 이력을 영속 보관 | 기존 JSON 저장을 v2 단일 형식으로 사용. Draft 관련 데이터 초기화·CHECK1개 변경; 새 열 테이블 불필요 |

DB 형식과 초기화·수동 적용 준비는 [DDL §6.9](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md#column-storage-impact-20261019)가 소유한다. 공통 구현은 로컬 H2 저장·재시작 및 생성 OpenAPI로 검증했으며 실제 DB 직접 조회/적용·실서버 HTTP 검증은 하지 않았다. 미배포 상태에서 Draft·변경 요청·저장 이력만 초기화하고 원본 Case·입력 파일·카탈로그는 유지한다. 전환 후 FE는 기존 draftId/operationId/rowId/새 열 키·로컬 저장 요청을 재사용하지 않고 원본 Case에서 새 Draft를 연다. 이전 임시저장 내용을 자동 재전송하지 않는다.


### 바로 진행할 FE 연결 범위

1. BE가 제공한 조회 가능한 Case로 목록의 `draftId` 유무에 따른 §4.1의 두 진입 경로와 표 페이지 조회를 연결한다.
2. 서버 응답을 쓰는 서비스와 타입을 만들고, 팝업/표 컴포넌트가 그 응답을 직접 사용하게 바꾼다. FE 문서 방침대로 실제 응답을 예전 mock 모양으로 보충하는 어댑터는 만들지 않는다.
3. 읽기 전용 화면부터 열고, `editing` 조건을 만족하는 셀만 편집 가능하게 한다.
4. API로 통과한 F-O-01 한 셀 변경 → 작업 성공 → 새 revision 재조회를 FE에 연결한다. 별도 편집 가능한 시험 Draft를 사용하며 완료된 Case105 시험 Draft를 재사용하지 않는다.
5. 당시 API 시험은 최종저장 → 새 PENDING Case까지였으며 Run을 자동 실행하지 않았다. 현재 BE 자동 실행 연결과 FE 폴링은 §6·편집 API §9.2.1을 따른다. 전체 표 편집 완료 판정은 일반 표별 처리까지 준비한 뒤 한다.

### 별도 합의·준비가 필요한 항목

| 항목 | 현재 가능한 판단 / 다음 확인 |
| --- | --- |
| 기본 출처 | 기존 목록의 `miBase`는 후보이지 `defaultSourceKey` 계약이 아님. 팝업에서 어떤 Case를 기본 선택할지 FE/기획 확인 |
| 단일 파일 업로드 | 아직 계약/구현 후속. 지원 형식·용량, 업로드 식별자·수명, 서버 파싱 응답, Draft 진입을 함께 확정. FE의 `.xls/.xlsx/.xlsm`, 50 MiB를 BE 승인값으로 간주하지 않음 |
| 실제 전체 목록 | C는 DRAFT 등록 완료·미발행·Case 미연결. BE/DBA가 별도 발행·새 Case 연결과 보류 항목을 확인. 92개 등록 건수를 현재 서버 반환 건수로 사용하지 않음 |
| 일반 저장과 구조 변경 | 필요한 표/동작을 먼저 정하고 BE 처리 지원을 확인. 공통 스키마에 있다는 이유만으로 행 추가/삭제를 열지 않음 |
| 초기화·다운로드 | 로컬 미저장분 취소와 서버 저장분 복원은 다른 기능. 서버 복원·편집본 다운로드가 필요하면 별도 요청 범위로 확정 |
| 추가 검증 | Compose/testdb F-O-01 기본 성공 흐름은 확인. FE 브라우저, 동일 요청 재전송·경합·실패/재시작·commit 결과 불명·업로드 정리와 Kubernetes 운영은 별도 검증. 날짜 읽기 코드의 시간대 의존성 보완은 후속 |

### FE 연동 완료 확인 사례

- `miBase`와 `cases[]` 모두 `draftId`가 있으면 EDT-02로 바로 재개하고 현재 revision을 사용한다. 다른 Case의 수정분이 섞이지 않는다.
- `draftId=null`이면 QRY-01 → EDT-01 → EDT-02로 연결하며, `created=false`인 기존 Draft 재개도 정상 처리한다.
- 회사·사용자 변경 시 목록을 재조회하고, 다른 소유자나 완료·폐기·삭제 Draft를 편집 대상으로 재사용하지 않는다. 목록 조회 후 상태가 바뀌면 EDT-02의 상태/오류로 분기한다.
- 두 페이지 이상 표에서 누락 열/중복 헤더/행 재번호화 없이 표시되고 마지막 페이지에서 요청이 멈춘다.
- `INCOMPLETE/NOT_ALLOWED/UNSUPPORTED`, 빈 슬롯, 읽기 전용 셀에 편집 버튼이 열리지 않는다.
- 저장 응답 `202`와 작업 `FAILED`를 성공으로 표시하지 않는다. 성공 후에는 새 revision의 서버 값을 표시한다.
- 같은 requestId 재전송으로 중복 저장하지 않으며, 충돌/원본 변경/접근 상실 시 자동 덮어쓰기하지 않는다.
- 최종저장 완료 후 BE가 새 Case의 입력 파일로 Run을 자동 접수한다(§6). 원래 로컬 File을 다시 업로드하는 경로와 섞이지 않는다.

이 문서로 현재 조회와 제한 저장의 연결 방법을 식별할 수 있다. **전체 입력편집은 표별 처리·구조/계산/보존 검증과 실제 연동이 남아 있으며**, F-O-01 성공을 전체 표·FE 화면·운영 검증 완료로 확대하지 않는다. 확정된 행·열 결정은 재질문하지 않고 해당 정책 데이터와 구현을 준비한다. 별도 범위인 PriceModel 비계약 물량·Regas 신규 설비 추가 추천은 미승인으로 유지한다.

## 10. 근거 링크

링크는 최초 조사 커밋에 고정하되, 목록 `draftId` 계약·Case Controller·목록 항목 DTO와 Draft 편집 계약은 후속 구현 커밋 `015e7581…`을 참조한다. 이번 동기화에서는 로컬 Git object로 링크 대상을 확인했으며 GitHub 접근 가능 여부는 확인하지 않았다. FE Markdown/OpenAPI는 협의 전 제안이며, 데이터 제공 범위는 BE 입력 제공 명세 §3의 합의를 따른다. API/업무 정책 변경은 해당 정본에 반영한 뒤 이 안내를 동기화한다.

- FE: [협의 전 요청 문서][fe-spec], [협의 전 OpenAPI 제안][fe-openapi], [입력 팝업][fe-popup], [시트 조회][fe-table], [mock 파서 호출][fe-preview], [카탈로그 mock][fe-catalog], [템플릿 선택][fe-registry], [공통 표][fe-summary], [좌표·편집 판단][fe-layout], [Case 서비스][fe-service].
- BE: [조회 상세 제안][be-query-spec], [Draft 편집 계약][be-edit-spec], [입력 제공 범위][be-provision], [Controller][be-controller], [응답 DTO][be-dto], [Service][be-service], [Repository][be-repository], [실제 변경 처리][be-preserve], [오류][be-errors], [최초 조사 testdb 설정][be-config], [카탈로그 조사][be-catalog].
- 실환경 증거: [Case105→106 F-O-01 사용자 검증][be-smoke-result], [C 양식 DRAFT 등록][be-c-registration]. 최초 조사 설정 링크와 아래 설명용 JSON은 실제 시험 응답의 근거로 사용하지 않는다.
- 목록 draftId 변경: [Case 목록 계약][be-list-contract], [Case Controller][be-cases], [목록 항목 DTO][be-list-dto].

[fe-spec]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/docs/ko/fundamental-model-sheet-api.md
[fe-openapi]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/docs/ko/fundamental-model-sheet-api.openapi.yaml
[fe-popup]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelInputEditorPopup.tsx#L95-L345
[fe-table]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelSheetDataTable.tsx
[fe-preview]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/fundamentalModelSheetDataMock.ts
[fe-catalog]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/sheetTemplates/fundamentalModelSheetCatalogMock.ts
[fe-registry]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/sheetTemplates/sheetTemplateRegistry.ts#L58-L67
[fe-summary]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/sheetTemplates/SummarySheetTemplate.tsx#L36-L50
[fe-layout]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/sheetTemplates/summaryTemplateLayout.ts
[fe-service]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/service/mi/fundamentalModel/case.ts
[fe-list]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelRunList.tsx#L266-L274
[be-query-spec]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/docs/feature-specs/mi-fundamental-model-input-query-api-proposal.md
[be-edit-spec]: https://github.com/sk-mi-platform/backend/blob/015e7581056fce06be15c94af9d88e5c22c47f09/docs/feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md
[be-provision]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/docs/feature-specs/mi-fundamental-model-input-provision-feature-spec.md
[be-controller]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/controller/fundamentalmodel/ModelInputDraftController.java
[be-dto]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/ModelInputDraftResponses.java
[be-service]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/ModelInputDraftService.java
[be-repository]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/shared/repository/master/tmm/ModelInputDraftRepository.java
[be-preserve]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputDraftPreserveChange.java#L26-L67
[be-errors]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputDraftException.java
[be-config]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/resources/application-testdb.yml#L294-L300
[be-cases]: https://github.com/sk-mi-platform/backend/blob/015e7581056fce06be15c94af9d88e5c22c47f09/src/main/java/com/sk/domain/mi/controller/fundamentalmodel/FundamentalModelCaseController.java
[be-list-contract]: https://github.com/sk-mi-platform/backend/blob/015e7581056fce06be15c94af9d88e5c22c47f09/docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md
[be-list-dto]: https://github.com/sk-mi-platform/backend/blob/015e7581056fce06be15c94af9d88e5c22c47f09/src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/FundamentalModelCaseListItemRes.java
[be-smoke-result]: ../work-notes/2026/2026-09-03-model-input-transfer-format.md#case105-106-smoke-20261019
[be-c-registration]: ../work-notes/2026/2026-09-03-model-input-transfer-format.md#c-structure-registration-20261019
[be-run]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/src/main/java/com/sk/domain/mi/controller/fundamentalmodel/FundamentalModelCaseDedicatedController.java
[be-catalog]: https://github.com/sk-mi-platform/backend/blob/ce557dd5e4784bfd44c594840af0f099b9567e99/docs/references/mi-model-input-catalog-structure-2026-10-19.md
