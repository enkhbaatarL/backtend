# 모델 입력편집 — 프론트엔드 API 연동 안내

## Document Control

- status: Review
- canonical for: Reference only — 현재 로컬 구현의 FE 연동 안내. 필드·검증·오류의 정본은 아래 API 명세다.
- owner: Shared
- source documents: [입력 제공 명세][be-provision], [편집 명세][be-edit-spec], [현재 Controller][be-controller], [현재 DTO][be-dto], [현재 Service][be-service]. FE 코드 비교는 §2에 명시한 과거 조사 기준이다.
- depends on: [표별 업무 규칙][be-domain], [DBA 적용 계약][be-ddl]. 이 안내는 새 정책·API·배포를 승인하지 않는다.
- supersedes: 이 문서의 2026-10-19 현재 상태 설명. 과거 실서버 시험 증거는 §1의 이력으로 유지한다.
- last synced: 2026-10-20 — 편집기 중간 커밋 `842fe78f3` 기준, 순차 업로드·v2·모드/연도 헤더·자동 실행 동기화
- open questions: 실제 카탈로그 연결·잔여 표 처리·Shipping 보류 결함2건과 PMI 표시 정책, 서버 배포·DBA 적용·FE 브라우저 통합 확인. 편집기 세션 종료를 전체 기능 완료로 간주하지 않는다.

## 1. 현재 인계 기준과 가능한 작업

**FE는 저장 Case 진입과 파일 순차 업로드를 같은 Draft 편집 화면에 연결할 수 있다.** Draft는 서버의 개인 편집 초안, revision은 성공한 변경의 버전 번호다. 파일 업로드·임시저장·최종저장은 접수와 처리 완료를 구분한다.

- 저장 Case: `Case 선택 → 목록 draftId로 재개(없으면 원본 구조 조회 후 생성/재개) → 선택 표 조회 → 변경 임시저장 → 최종저장 → BE 자동 실행 결과 조회`.
- 파일 직접 입력: `첫 파일 검증 접수 → 성공 결과에서 draftId·구조·첫 표 수신 → 같은 Draft에 파일 추가/교체 및 허용 편집 → 네 역할 준비 후 최종저장 → BE 자동 실행 결과 조회`.

**데이터 제공 합의는 전체 파일의 구조 정보와 화면에 표시할 선택 표의 데이터다.** Case는 네 파일 구조, 부분 업로드 초안은 검증 성공한 역할만 제공한다. 큰 표는 페이지로 나눈다. FE의 과거 전체 시트 일괄 반환 제안은 현재 요구사항이 아니다. 계약은 [입력 제공 명세 §3][be-provision]을 따른다.

| 기능 | 현재 확인한 근거 | FE에 전달할 범위 |
| --- | --- | --- |
| Case 목록·개인 draftId, 원본/Draft 구조·표 조회 | 기존 구현과 현재 Controller/DTO | §3~4의 진입·재개·표 렌더링 연결 |
| 순차 업로드·교체·취소 | `0cd1c2532` 커밋, 분리 후보 테스트216개 통과·독립 검토 승인 기록 | §4.4의 업로드 화면·파일 준비 상태·실패 보존 연결. 배포 준비는 §9 |
| v2 행·열 저장/복원·columnMappings | `e76aed6b8` 커밋, 기존 완료 기록의199개 테스트·독립 검토 승인 | §5의 공통 명령·안정 ID·버전 처리. 이력 형식과 실제 Excel 편집 지원은 별개 |
| 실제 Excel 값·행·열 처리 | 중간 커밋 `842fe78f3`, 표별 실파일·일부 공개 API→복원→최종4파일 검증 기록 | B8 한 셀 시험을 넘어 확장됨. §8의 검증/미완료 경계를 지키고 서버 READY 응답으로만 쓰기 활성화 |
| 모드·연도 헤더 수정 | `842fe78f3`의 parser/Service와 편집 명세 §4 | `controlId`, `headerKey` 형식을 조건부 지원. §5.2 참조 |
| 최종저장·자동 실행 | `dcb8b08c5` 및 현재 코드 | 별도 자동 Run POST 없이 §6의 저장/실행 상태 처리 |

**편집기 전체를 완료·배포된 기능으로 인계하지 않는다.** 2026-10-20 편집기 중간 커밋은 완료됐지만 [편집기 작업 기록][be-editor-note]는 실제 카탈로그 연결·일부 표·최종 공통 검증과 완료 판정이 남았다고 명시한다. 미해결 Shipping 결함2건은 사용자 요청으로 보류했다. 준비된 표·필드의 `capabilities/policyStatus`와 control의 `editable`을 실제 서버에서 확인해야 한다.

### 구현 기준과 실환경 증거의 구분

- 이번 안내의 기준은 로컬 `feature/gas2`의 편집기 중간 커밋 `842fe78f3`이다. 현재 BE 링크는 저장소 상대 경로로 연결한다. 해당 변경의 원격 push·서버 배포·최신 DB 적용 여부는 확인하지 않았다.
- 최초 BE 조사 커밋은 `deploy/mi-be-test`의 `ce557dd5…`, FE 비교는 `feature/parkjaehyun`의 `4cccc5a7…`였다. 이번에는 FE 소스·브라우저를 재조사하지 않았으므로 §2는 당시 코드의 후속 연결 목록이다.
- 2026-10-19 사용자 실환경 시험은 Case105의 OUTPUT B8 `0.34→0.35`, revision0→1과 Case106·4파일 최종저장/구조 재조회까지 확인했다. 이는 **과거 F-O-01 시험 이력**이며 최신 일반 편집·자동 모델 실행·FE 전체 통합의 실환경 증거가 아니다. [상세 기록][be-smoke-result]
- 당시 웹/worker의 활성화 및 시간대 설정으로 성공한 결과를 최신 배포 완료로 확대하지 않는다. Case105의 시험 Draft는 이미 FINALIZED이며 새 편집 요청에 재사용하지 않는다.
- C의92개 표는 새 DRAFT 양식 등록 이력이 있다. ACTIVE 발행·Case 연결 및 최신 쓰기 정책 준비를 이번에 실행/확인하지 않았다. 등록 건수와 현재 서버의 표시/편집 가능 건수를 동일하게 취급하지 않는다. [등록 이력][be-c-registration]

## 2. FE에서 연결할 작업

아래는 최초 FE 조사 시 확인한 연결 과제다. **현재 FE에 같은 문제가 남아 있는지는 재확인하지 않았다.** [입력 팝업][fe-popup]·[표 로더][fe-table]·[로컬 파서][fe-preview]·[편집 판정][fe-layout]을 현재 FE 담당자가 대조한다.

| 조사 당시 동작 | 연결할 서버 동작 | 완료 확인 |
| --- | --- | --- |
| saveDraft가 메모리에만 저장하고 제출은 원래 File[] 업로드 | EDT-04/05 임시저장 후 EDT-06/07 최종저장 | 수정값이 새 revision 재조회에 반영됨. 원본 파일 재업로드를 편집 저장 완료로 표시하지 않음 |
| EXCEL_UPLOAD 진입과 로컬 파일 파싱 | 저장 Case 분기와 UP-01~04 순차 업로드를 연결 | 서버 발급 draftId·구조·첫 표를 사용하며 미검증 파일을 완료로 표시하지 않음 |
| rowPolicy/columnPolicy 문구와 로컬 inputRanges로 편집 허용 | §5의 capability·필드 정책·controls를 사용 | 미지원 버튼 비활성화, CREATE/SET/CLEAR/삭제/헤더 수정 구분 |
| 시트 전체·연속 좌표에 의존한 렌더링 | 서버 columns/rows·page와 안정 행/열 키 사용 | 비연속 행·열, 다중 헤더, 제외 열, 추가·삭제 후 구조 재조회 |
| 초기화는 로컬 초안 삭제, 다운로드는 원본 File | 미저장분 취소와 작성 전체 폐기(UP-04)를 구분 | 서버 저장분 되돌리기·편집본 다운로드로 오해하지 않음. 해당 전용 API는 이 Controller에 없음 |

## 3. 기본 주소, 권한, API 목록

Base path: `/api/v1/mi/fundamental-model`

- 기존 로그인 클라이언트를 사용한다. Controller 권한은 `USER + SKGAS_COMPANY_USER_ONLY` 또는 `ADMIN + THIRD_PARTY_ADMIN_ONLY`다.
- 회사·사용자는 인증 정보로 결정한다. request body에 companyNo/userId/저장소 경로를 추가하지 않는다. 타 소유자의 Draft/작업은404다.
- Controller는 `@Profile("!dedicated")`다. Draft/업로드 요청은 공통 BE로 연결하며 기존 전용 Run 주소 설정을 그대로 복사하지 않는다. 실제 프록시 주소는 배포 환경에서 확인한다.
- Axios baseURL이 `/api/v1/`이면 일반 호출은 `mi/fundamental-model/...`이다. 서버가 반환한 `/api/v1/...` 경로에는 prefix를 중복 추가하지 않는다.

| 번호 | Method | Base path 이하 | 요청 / 응답 |
| --- | --- | --- | --- |
| QRY-01 | GET | `/cases/{caseId}/inputs/structure` | 원본 구조·sourceVersion, tableData:null |
| EDT-01 | POST | `/cases/{caseId}/input-drafts` | JSON `{sourceVersion}` → 생성/재개, created로 구분 |
| EDT-02 | GET | `/input-drafts/{draftId}` | 현재 성공 revision·구조·진행 작업 |
| EDT-03 | GET | `/input-drafts/{draftId}/table-data` | revision/tableId 필수, startRow 선택 → 선택 표·page·editing |
| EDT-04 | POST | `/input-drafts/{draftId}/change-requests` | JSON `{requestId,baseRevision,operations}` → 작업 상태 |
| EDT-05 | GET | `/input-drafts/{draftId}/change-requests/{operationId}` | 임시저장 작업 상태 |
| EDT-06 | POST | `/input-drafts/{draftId}/finalizations` | JSON `{requestId,baseRevision,title,description?}` → 최종저장 작업 |
| EDT-07 | GET | `/input-drafts/{draftId}/finalizations/{operationId}` | 저장 상태·성공 Case/파일·자동 실행 상태 |
| UP-01 | POST | `/input-upload-drafts` | multipart file+metadata → 최초 파일 검증 접수 |
| UP-02 | POST | `/input-upload-drafts/{draftId}/files` | 같은 multipart → 후속 파일 추가/확인한 교체 |
| UP-03 | GET | `/input-upload-requests/{operationId}` | 최초 초안 생성 전에도 작업 상태 조회 가능 |
| UP-04 | POST | `/input-upload-drafts/{draftId}/discard` | JSON `{baseRevision,discardConfirmed:true}` → DISCARDED 구조 |

위12개는 [현재 Controller][be-controller]의 실제 경로다. 비동기 POST는 신규/진행 중 동일 요청202, 종료된 동일 요청 재전송200이며 GET은 실패 작업도200이다. EDT-01·UP-04는 성공200이다. 성공 여부는 최상위 boolean과 작업 `data.status`를 구분한다.

기존 API 연결:

| Method / Path | 역할 |
| --- | --- |
| `GET /cases` | miBase/cases의 개인 draftId로 재개. 기본 출처나 편집 준비 여부는 별도 확인 |
| `GET /cases/{caseId}` | 최종저장 성공 Case 상세 |
| `POST /cases` | 기존4파일 Case 생성. 파일 하나의 검증/부분 초안 API를 대체하지 않음 |
| `POST /cases/{caseId}/runs` | 사용자가 명시적으로 실행/재실행할 때의 기존 경로. Draft 최종저장 후 자동으로 중복 호출하지 않음 |

과거 FE 제안의 `GET /input-sources`, `GET /inputs?sourceKey=...`, `POST /input-uploads`와 별도의 현재 업로드 초안 검색/목록 API는 이 Controller에 없다. 출처 키는 BE와 확인한 설정값을 사용하고 가짜 조회 경로를 만들지 않는다. [Case 목록 계약][be-list-contract] · [Case/Run Controller][be-run]

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

최상위 `status`는 요청 성공 여부인 boolean, `data.status`는 Draft 또는 작업 상태인 문자열이다. `data.summary`라는 중첩 필드는 없다. EDT-02는 위 요약에서 `created`를 제외하고 `sections`, `tableData:null`을 추가한다. QRY-01은 `sourceKey`, `sourceVersion`, `sections`, `tableData:null`만 제공한다. 직접 업로드 초안은 sourceCaseId/sourceVersion이 명시적 null이고 EDT-02에 파일 준비 상태가 추가된다(§4.4). JSON은 설명용이며 실제 ID는 응답에서 받는다. [응답 DTO][be-dto]

### 4.2 구조와 선택 표의 대응

| 화면 개념 | 응답 필드 | 사용 방법 |
| --- | --- | --- |
| Demand/Supply/Trade/Output 탭 | `sections[].kind` | `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 순서 |
| 종류별 원본 식별 | `sections[].inputId` | 원본 역할·파일·발행 기준의 키. Draft 변경 버전을 대체하지 않음 |
| 시트 버튼 | `sections[].sheets[].sheetName` | 서버 순서를 유지 |
| 표 버튼 | `sheets[].tables[].id`, `name`, `description` | `id`는 그대로 전송. `description`은 null 가능 |
| 위치 정보 | `tables[].location`, `coordinates` | 현재 `coordinates`는 `headerRow`, `bodyStart`만 공개. 헤더 없는 표의 `headerRow`는 0 가능 |
| 셀 표시 | EDT-03 `tableData.columns`, `tableData.rows` | 선택 표에 반환된 열·행만 표시. `sheets[].data`는 없음 |
| 부속 입력/선택 UI | `tables[].controlIds` → `tableData.controls` | grid 외의 명시적 control. editable:true 및 §5.2 조건으로 변경 |
| 화면 갱신 규칙 | `tableData.displayUpdateRules` | 서버가 제공한 정의를 확인해 적용. 최초 조사 FE에서는 소비하지 않았으며 최신 FE는 재확인 필요 |
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

응답 전체는 2 MiB 한도여서 2,000행보다 적게 올 수 있다. 본문은 보존 Helper 영역을 건너뛴 비연속 행일 수 있다. `startRow + 2000`으로 다음 위치를 계산하지 않는다. `hasNext=false`에서 종료한다. 헤더는 페이지마다 반복되므로 누적 렌더링 시 중복 제거하고, 본문은 원래 행 번호를 유지한다. 한 행과 metadata만으로 한도를 넘으면 `422 / RESPONSE_TOO_LARGE`다.

출처/표 전환 또는 저장 성공으로 revision이 바뀌면 이전 요청 결과를 현재 화면에 섞지 않는다. 캐시를 사용한다면 인증 회사·사용자 문맥과 `draftId, revision, tableId, startRow`를 구분한다. 원본 `sourceVersion`과 `inputId`는 Draft 저장마다 바뀌는 키가 아니므로 그것만으로 캐시를 나누면 오래된 값이 남는다.

Regas처럼 다른 표를 참조하는 화면은 그 표도 같은 Draft·revision으로 별도 조회한다. 선택 표 응답에 참조 시트 전체가 포함됐다고 가정하지 않는다. [조회 구현][be-service] · [기존 연결 시트 로더][fe-table]

### 4.4 파일 직접 업로드·교체·취소

기존 Case 없이 파일 하나부터 시작한다. 지원 형식은 **.xlsx/.xlsm, 파일당50 MiB(52,428,800 bytes)**다. .xls는 지원하지 않는다. 서버는 파일명으로 DEMAND/SUPPLY/TRADE/OUTPUT을 판별하고 최초 성공 때 고정한 활성 양식 규칙으로 검증한다. FE가 임의 kind·Case ID를 만들어 보내지 않는다.

UP-01/02는 정확히 두 multipart part를 보낸다. file은 원본 하나, metadata는 `Content-Type: application/json`인 JSON이다. 바깥 multipart boundary는 사용하는 HTTP 클라이언트가 생성하게 한다. 아래 sourceKey/UUID는 설명용이다. sourceKey는 BE가 설정한 EXCEL_UPLOAD 키를 받아 사용한다. 최초 요청은 baseRevision/replaceConfirmed를 생략한다.

```json
{
  "sourceKey": "EXCEL_UPLOAD_SOURCE_KEY_FROM_BE",
  "requestId": "aefb60dc-18b8-4ac2-beb4-a4d499950770"
}
```

UP-02 추가/교체는 현재 Draft의 baseRevision이 필수다. 이미 같은 역할 파일이 있으면 사용자에게 **그 역할의 기존 편집이 초기화됨**을 알리고 확인 후 replaceConfirmed:true를 보낸다. 다른 역할 파일/편집은 유지한다. 새 역할 추가 때 확인값은 생략할 수 있다.

```json
{
  "sourceKey": "EXCEL_UPLOAD_SOURCE_KEY_FROM_BE",
  "requestId": "aefb60dc-18b8-4ac2-beb4-a4d499950771",
  "baseRevision": 2,
  "replaceConfirmed": true
}
```

업로드 접수/결과 조회의 공통 형식은 다음과 같다. 최초 성공 전에는 draftId가 없다. data.status를 확인하고 서버가 준 statusEndpoint를2초 간격으로 조회한다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "operationId": "aefb60dc-18b8-4ac2-beb4-a4d499950772",
    "requestId": "aefb60dc-18b8-4ac2-beb4-a4d499950770",
    "status": "QUEUED",
    "statusEndpoint": "/api/v1/mi/fundamental-model/input-upload-requests/aefb60dc-18b8-4ac2-beb4-a4d499950772",
    "result": null,
    "failure": null
  }
}
```

| data.status | result/failure | FE 처리 |
| --- | --- | --- |
| QUEUED/RUNNING | 둘 다null | 검증 중. 초안 생성/파일 연결 성공으로 표시하지 않음 |
| SUCCEEDED | result만 object | result의 서버 ID·구조·첫 표를 표시하고 반복 조회 종료 |
| FAILED | failure만 object | failure.reason/currentRevision/issues 표시 후 종료. 최초는 초안/Case 미생성, 후속은 기존 성공 상태 유지 |

성공 result는 `{kind,draftId,revision,draftEndpoint,structure,selection}`이다. structure는 EDT-02 전체 data, selection은 업로드 역할의 **첫 표시 표**에 대한 EDT-03 전체 data이며 표시할 표가 없으면 null이다. 로컬 시트 순서나 수정 가능한 표 우선으로 다시 선택하지 않는다. 성공 결과 예시 전문은 [업로드 계약 §3.3][be-upload-contract]를 따른다.

- 직접 업로드 초안은 sourceCaseId/sourceVersion이 **명시적 null**이다. 가짜 Case나 sv1 값을 요구하지 않는다. 첫 성공 revision은0, 이후 파일 추가/교체·편집 성공 때마다 한 번 증가한다. 최종저장에만 새 Case가 생긴다.
- EDT-02의 `uploadedKinds`, `missingKinds`, `canFinalize`는 요약 필드와 같은 data 깊이에 있다. readiness나 summary로 한 번 더 감싸지 않는다. Case 기반 응답에 이 세 키가 항상 있다고 요구하지 않는다.
- 검증/연결 완료한 역할만 sections/uploadedKinds에 포함한다. 없는 파일의 tab은 미준비로 표시하고 표를 mock으로 채우지 않는다. canFinalize는 네 역할 완성 외에도 DRAFT·진행 작업 없음·최종저장 활성화가 필요하므로 파일 수만으로 FE가 true를 계산하지 않는다.
- 같은 초안의 업로드·편집·최종저장은 한 번에 하나만 진행한다. 작업 중에는 새 변경을 보내지 않으며 EDT-02의 activeOperationEndpoint로 재진입한다. 최초 검증 중에는 UP-03의 operationId가 필요하다.
- 같은 사용자는 직접 업로드 Case 하나만 작성한다. 새 requestId/탭으로 두 번째 작성을 만들지 않는다. 첫 요청의 operationId/requestId와 성공 후 draftId는 기존 화면 복원 방식으로 보관한다. 별도 현재 작성 목록/검색 API는 없다.
- 응답 유실 때 같은 requestId·동일 metadata·동일 파일로 재전송한다. 기존 SUCCEEDED/FAILED 결과는 당시 snapshot이며 이후 편집으로 다시 계산되지 않는다. 재개할 때 draftEndpoint로 현재 revision을 다시 받아 오래된 result를 덮어쓰지 않는다.
- FAILED 재시도는 새 requestId를 만들고 후속 요청의 현재 baseRevision을 다시 받는다. 검증 불일치 시 새 Case나 파일 연결을 만들지 않으며 기존 역할 교체도 확정하지 않는다.
- 업로드 처리 상한은 파일 전송 후 접수부터 대기열 포함120초다. TIMEOUT은 서버의 종료 결과로 확인하며 FE 대기시간만으로 실패를 확정하거나 새 작업을 만들지 않는다. 실제 대용량 성능 보장으로 해석하지 않는다.

작성 전체 취소는 진행 작업이 없고 사용자 확인을 받은 뒤 UP-04로 보낸다.

```json
{
  "baseRevision": 3,
  "discardConfirmed": true
}
```

성공200은 status:DISCARDED·canFinalize:false인 공통 Draft 구조다. 작성 제한이 해제돼 새 첫 파일을 올릴 수 있다. 새 파일 검증 실패가 이전 취소를 되돌리지는 않는다. 확인 취소·서버 취소 실패는 기존 작업을 유지한다. 이 기능은 현재 표 초기화·저장분 되돌리기·파일 즉시 삭제가 아니다.

## 5. 편집 가능 여부와 임시저장

### 5.1 버튼과 셀 활성화

`editing.capabilities`는 표 전체 동작, `fields[].policyStatus`는 필드별 준비 상태다. `READY`만 허용하고 `INCOMPLETE`(정의 미완성), `NOT_ALLOWED`(불허), `UNSUPPORTED`(현재 처리기/설정 미지원)는 비활성화한다. 소스 코드에 처리기가 있거나 표가 화면에 보인다는 사실만으로 READY를 가정하지 않는다.

| 동작 | FE 활성화 조건·전송 대상 |
| --- | --- |
| 본문 SET/CLEAR | 표 동작과 필드의 해당 정책 모두 READY. SET은 inputCells와 유효 rowBindings 확인. rowRef.rowId + fieldKey 사용 |
| ADD_ROW | 표 ADD_ROW READY와 필요한 CREATE 정의 완비. fields[].create의 required/initial/constraints로 입력창 구성 |
| DELETE_ROW | 표 DELETE_ROW READY와 유효한 논리 행 ID. 전체 Excel 행 삭제로 해석하지 않음 |
| ADD_COLUMN | 표 ADD_COLUMN READY. editing.columnCreation.headerValues/body로 생성 입력창 구성 |
| DELETE_COLUMN | 표와 선택 필드의 DELETE_COLUMN 모두 READY. fieldKey 또는 같은 요청의 clientColumnKey 사용 |
| control SET | 표 SET READY와 tableData.controls[].editable:true, options[].value 사용 |
| 연도 헤더 SET | 표 SET READY와 fields[].policyStatus.SET_HEADER:READY. headerKey:year 사용 |

Draft가 DRAFT이고 진행 작업이 없을 때 변경을 접수한다. 직접 업로드도 같은 편집 API/정책을 쓰며 현재 파일만으로 수행할 수 없는 동작은 허용하지 않는다. 최종저장 가능 여부와 개별 표 편집 가능 여부는 다르다.

- `rowNumber`, rows[].id, columnN은 현재 표시 위치다. 영속 대상은 rowId와 fieldKey다. 이동해도 키를 유지하며 삭제 키는 재사용하지 않는다. EMPTY_SLOT의 rowId:null을 SET 대상으로 만들지 않는다.
- `editValues[].values[fieldKey]`가 실제 편집 시작값이다. 반올림·쉼표·퍼센트 표시 문자열을 역변환하거나 전체 행을 되보내지 않는다. 서버에는 사용자가 바꾼 값만 보낸다.
- 숫자는 JSON number, 텍스트는 string이다. null/숫자 문자열/수식을 숫자 입력으로 보내지 않는다. 정확한 소수는 지수 표기 없이 전송하고 JavaScript number 변환에 따른 조용한 자릿수 손실을 피한다.
- 생략은 기존값 유지, CLEAR는 실제 빈칸, 숫자0은 명시적 값이다. 신규 필수값/초깃값은 서버 create 정의를 따른다. 빈칸을 일괄0으로 채우지 않는다.
- rowPolicy/columnPolicy의 안내 문구·로컬 색상·SUMMARY·mock 좌표는 저장 권한을 대체하지 않는다.

### 5.2 요청 형식과 결과 대응

아래 SET은 **전송 형식 예시**다. 예전 F-O-01 시험도 이 표/필드를 사용했지만 현재 지원 전체를 B8/0.35 한 건으로 제한한다는 뜻은 아니다. tableId/rowId/fieldKey는 현재 표 응답을 사용하고 READY인 경우에만 보낸다.

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

6명령의 필드 계약은 [편집 명세 §4 및 대표 JSON §7][be-edit-spec]를 따른다. 다음은 FE 구현 시 필요한 대응 요약이다.

| 명령/대상 | 전달할 핵심 |
| --- | --- |
| ADD_ROW | clientRowKey + 기존 fieldKey 기준 values. 뒤의 SET은 rowRef.clientRowKey로 참조 가능 |
| SET/CLEAR 본문 | rowRef는 rowId 또는 같은 요청의 앞선 clientRowKey 중 하나. 열은 fieldKey 또는 앞선 clientColumnKey 중 하나 |
| DELETE_ROW | rowRef. worksheet 좌표나 클라이언트 계산 파생 행을 보내지 않음 |
| ADD_COLUMN | clientColumnKey + headerValues object. LiqUtil은{}, LiqProd는 year/month, 연도형은 year. 본문 값은 뒤의 SET으로 전달 |
| DELETE_COLUMN | fieldKey 또는 앞선 clientColumnKey. 연도 추가 허용에서 삭제 허용을 추정하지 않음 |
| 모드 SET | controlId + value. rowRef/fieldKey와 혼합하지 않음. label 대신 options[].value를 원문 그대로 전송 |
| 연도 헤더 SET | fieldKey 또는 앞선 clientColumnKey + headerKey:"year" + 정수 value. rowRef/controlId 없음. 현재 LNG Demand 대상 정책으로 제한 |

LNG 모드의 `Dynamaic` 저장값과 `Dynamic` 표시명을 혼동하지 않는다. Shipping 모드의 선행 공백도 options 값의 일부다. 기존 선택지 밖 저장값을 조회했다는 이유로 자동 정규화/저장하지 않는다. 해당 Shipping 경로의 미해결 제한은 §8을 따른다.

operations는 순서대로 하나의 성공/실패 단위로 처리한다. 새 행×새 열은 두 ADD 뒤 SET한다. 새 열의 필수 headerValues는 ADD_COLUMN에서 완성한다. LiqUtil의 활성 월을 모두 삭제한 후 추가하면 고정 원본의 첫 월부터 시작하며 FE가 임의 다음 월을 계산해 제출하지 않는다. 임시키는 요청 안에서만 유효하다.

공통 본문은 UTF-8 1 MiB·깊이8, operations1~500개·ADD_ROW최대100개다. commandId/clientRowKey/clientColumnKey는 `[A-Za-z][A-Za-z0-9_-]{0,63}` 형식이고 각 규칙에 따라 유일해야 한다. 알 수 없는 키를 화면 상태와 함께 덧붙이지 않는다.

| EDT-04/05 결과 | FE 처리 |
| --- | --- |
| operationId/requestId/draftId, status/statusLabel, statusEndpoint | 요청/작업을 보관하고 QUEUED/RUNNING을2초 간격 조회 |
| baseRevision/resultRevision | 성공에만 resultRevision=baseRevision+1. 접수·실패 때 현재 버전을 올리지 않음 |
| rowMappings/columnMappings | 항상 배열. 진행·실패·추가 없는 성공은[]. 성공의 임시키→rowId/fieldKey 대응을 반영. 삭제된 추가 대상의 DELETED 대응도 존재 가능 |
| affectedTableIds | 성공 후 같은 새 revision으로 재조회할 표. 연결 표도 재조회 |
| failure | FAILED에만 Problem. 직전 성공값을 유지하고 실패 위치 표시 |

신규 열은 좌표나 연월이 같아도 별도 fieldKey다. 매핑을 반영한 뒤 EDT-02/03으로 현재 구조·필드·값을 다시 받는다. 동일 요청 재전송은 같은 작업/키를 반환한다. [행/값 JSON][be-row-json] · [열 JSON][be-column-json]

### 5.3 저장 대기·성공·실패

1. `QUEUED/RUNNING`이면 `statusEndpoint`를 **2초 간격**으로 조회한다. 임시저장 POST의 헤더 유무와 관계없이 이 간격을 사용한다.
2. `SUCCEEDED`에서 반복 조회를 끝낸다. `resultRevision` 기준으로 Draft 구조와 영향받은 표를 다시 받고, 성공 확인한 로컬 변경분을 정리한다.
3. `FAILED`에서도 반복 조회를 끝낸다. 직전 성공 revision과 값을 유지하며 `failure.reason/issues`를 표시한다. 사용자 수정분은 복구·재검토할 수 있게 별도로 보관한다.
4. 작업 조회 자체는 FAILED여도 HTTP `200`, 최상위 `status:true`, `code:SUC-000`이다. **저장 성공은 `data.status === 'SUCCEEDED'`로 판단한다.**

응답이 유실되거나 통신/500으로 결과를 모를 때는 새 UUID로 다시 저장하지 않는다. 같은 `requestId`와 같은 body로 재전송하거나, 이미 받은 `statusEndpoint`를 조회한다. 같은 requestId에 다른 내용을 보내면 `IDEMPOTENCY_CONFLICT`다. 실패가 확정된 작업의 같은 요청 재전송은 실패 기록을 돌려주므로, 다시 시도하려면 현재 Draft를 확인하고 새 requestId를 만든다. CHANGE와 FINALIZE는 requestId 중복 키를 공유한다.

## 6. 최종저장과 모델 실행

미저장 변경을 먼저 EDT-04/05로 성공시킨 뒤, 진행 작업이 없는 현재 revision으로 EDT-06을 호출한다. 변경 없는 revision 0도 허용되지만, 직접 업로드 초안은 먼저 네 역할 파일이 모두 성공 연결돼야 한다. canFinalize:false이면 버튼을 열지 않고, 실패/진행 중 업로드 파일을 합산하지 않는다.

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
- **최종저장 성공 후 BE가 모델 Run을 자동 접수한다.** FE는 아래 응답/분기 규칙으로 연결하며 API 정본은 [최종저장 계약 §9.2.1][be-auto-execution-contract]이다.
- 최종저장은 현재 성공한 파일 기준과 지원하는 전체 성공 변경 이력으로 네 파일을 생성한다. 직접 업로드는 각 역할의 원본 .xlsx/.xlsm 형식을 유지한다. 실파일 검증 범위와 정책 발행 준비는 §8을 따르며, 지원하지 않는 이력을 무시해 저장 완료로 만들지 않는다.
- 이미 최종저장된 동일 내용은 원래 작업/Case를 돌려준다. 제목·설명·기준 revision을 바꿔 같은 Draft를 다시 최종저장하는 흐름으로 사용하지 않는다.

FE 화면의 완료 문구는 `임시저장 완료`, `최종저장 완료`, `모델 실행 접수/완료`를 구분해야 한다. [최종저장 계약 §9][be-edit-spec] · [현재 최종저장 Controller][be-finalization-controller] · [최초 조사 FE 완료 처리][fe-list]

### 6.1 응답 변경과 조회 경로

`POST /api/v1/mi/fundamental-model/input-drafts/{draftId}/finalizations`의 요청 형식은 위 예시 그대로다. 변경점은 `data.execution` 추가와 BE 자동 실행 연결이다. 이 흐름에서 FE가 `POST /cases/{caseId}/runs`를 추가로 자동 호출하지 않는다. 기존 파일 업로드 화면의 명시적 실행 기능은 유지한다.

| 응답 필드 | FE에서 사용할 의미 |
| --- | --- |
| BaseResponse의 `status` / HTTP200·202 | API 접수/조회 성공 여부. 모델 계산 성공 판정에 사용하지 않음 |
| `data.status` / `data.failure` | 저장 상태와 저장 실패. `SUCCEEDED`는 저장 완료이며 모델 완료와 다름 |
| `data.statusEndpoint` / POST의 `Location` | 같은 최종저장 작업 GET 경로. 최초 자동 실행 결과까지 이 경로를 조회 |
| `data.execution` | 필수 object 또는 null. 저장 전/저장 실패와 자동 실행 도입 전 완료 건은 null |
| `execution.status` / `statusLabel` | `QUEUED/RUNNING/COMPLETED/FAILED`와 실행 대기/실행 중/실행 완료/실행 실패 |
| `execution.runId` | 최초 연결한 Run ID. 접수 전/접수 실패에는 null |
| `execution.statusEndpoint` | `/api/v1/mi/fundamental-model/cases/{caseId}/status`. Case의 **최신** 실행 조회용이며 `data.statusEndpoint`와 다름 |
| `execution.failureReason` | FAILED만 안전한 실패 코드. 다른 상태에서는 null |

저장 후 실행 접수 실패 시 **data의 판정 필드 발췌**다. 전체 응답 예시는 [편집 명세 §9.4][be-edit-spec]를 따른다. `status=SUCCEEDED`, `failure=null`과 Case/파일은 유지되고 실행 실패만 표시한다.

```json
{
  "status": "SUCCEEDED",
  "finalizedCaseId": 104,
  "failure": null,
  "execution": {
    "status": "FAILED",
    "statusLabel": "실행 실패",
    "runId": null,
    "statusEndpoint": "/api/v1/mi/fundamental-model/cases/104/status",
    "failureReason": "EXECUTION_REQUEST_FAILED"
  }
}
```

실패 문구는 `EXECUTION_REQUEST_FAILED`면 “저장 완료, 모델 실행 접수 실패”, `JOB_SUBMIT_FAILED`면 “저장 완료, 모델 작업 요청 실패”, `MODEL_EXECUTION_FAILED`면 “저장 완료, 모델 실행 실패”로 구분할 수 있다. 내부 오류 원문은 응답에 없으며 FE에서 추정해 표시하지 않는다.

### 6.2 상태별 화면 처리와 폴링 종료

폴링은 **같은 GET을 일정 간격으로 반복 조회하는 것**이다. 아래 “계속”은 `data.statusEndpoint`를 **2초 간격**으로 조회한다는 뜻이다. 종료된 요청의 POST 재전송이 HTTP200이어도 표의 상태로 판단한다. GET은 실패한 작업도 HTTP200으로 조회한다.

| `data.status` | `execution` | 화면 처리 | 폴링 |
| --- | --- | --- | --- |
| `QUEUED` / `RUNNING` | null | 저장 대기/처리 중. execution=null만 보고 종료하지 않음 | 계속 |
| `FAILED` | null | 저장 실패. `data.failure` 표시, 직전 성공 Draft 유지 | 종료 |
| `SUCCEEDED` | `status=QUEUED` | 저장 완료·실행 대기. 모델 완료로 표시하지 않음 | 계속 |
| `SUCCEEDED` | `status=RUNNING` | 모델 실행 중 | 계속 |
| `SUCCEEDED` | `status=COMPLETED` | 실행 완료. Case 목록/상태를 갱신하고 기존 결과 화면으로 연결 | 종료 |
| `SUCCEEDED` | `status=FAILED` | 저장 완료·실행 실패. `execution.failureReason` 표시, Case/파일 유지 | 종료 |
| `SUCCEEDED` | null | 과거 저장 완료 건. 자동 실행 대상/모델 성공으로 간주하지 않음 | 종료 |

### 6.3 중복 요청·통신 오류·명시적 재실행

- 버튼 중복 클릭과 중복 폴링 루프를 막는다. 서버가 반환한 operationId/requestId/draftId를 사용한다. 완료 재클릭은 이번에 보낸 requestId 대신 원래 성공 요청 ID를 반환할 수 있다.
- 접수 응답이 유실되면 같은 requestId/동일 body를 재전송하거나 알고 있는 `data.statusEndpoint`로 확인한다. 통신 오류·FE 대기시간 경과는 서버 FAILED가 아니며 임의의 새 ID로 작업을 만들지 않는다. GET 재확인도 새 실행을 시작하지 않는다.
- 저장 실패 후 사용자가 다시 저장하면 현재 Draft/revision을 확인하고 새 requestId를 사용한다. 저장 성공 후 실행 실패는 같은 최종저장을 재전송해도 새 Run이 생기지 않는다.
- 실행 재시도는 **사용자가 명시적으로 요청할 때** 기존 Case Run 기능으로 연결한다. 최신 Case 상태가 PENDING/FAILED일 때만 실행 POST, RUNNING이면 상태 관찰, COMPLETED면 재실행하지 않는다. 응답 유실 후 무조건 POST를 반복하지 않는다.
- 명시적 재실행 후에는 **기존 Case/status 흐름으로 최신 Run을 조회**한다. 최종저장의 execution은 최초 연결 Run을 추적하므로 그 작업 GET으로 새 Run의 결과를 기다리지 않는다.
- 화면 이탈/대상 변경 시 타이머·불필요한 요청을 정리하고 늦은 응답을 다른 Draft/Case에 적용하지 않는다. BE 처리는 계속된다. 재진입은 기존 화면 상태 복원 방식으로 작업 ID를 복원해 같은 GET을 재개한다.

구현 인계에 사용할 [FE 작업 프롬프트][fe-auto-execution-prompt]도 제공한다. 이 절은 FE 사용 안내이며 필드·멱등·실패 규칙의 정본은 [편집 명세 §9][be-edit-spec]다.

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
| `404 / RSC-001` | Draft/표/작업 없음 또는 접근 불가. `data:null` | 현재 화면/반복 조회 종료, 출처·권한·ID 재확인 안내. 목록에서 받은 ID여도 동일하게 처리 |
| `409 / BIZ-001` | `REVISION_CONFLICT` | 최신 Draft와 표를 재조회. 기존 변경분을 새 revision에 자동 덮어쓰지 않음 |
| 같은 409 | `DRAFT_BUSY` | EDT-02의 `activeOperationEndpoint`로 진행 작업 확인 |
| 같은 409 | `SOURCE_CHANGED`, `DRAFT_SOURCE_CONFLICT` | 원본 구조·선택 재확인. 다른 원본에 기존 초안 자동 적용 금지. 직접 업로드 작성 폐기는 UP-04를 사용하며 Case 기반 강제 재생성과 혼동하지 않음 |
| 같은 409 | `POLICY_INCOMPLETE`, `POLICY_UNSUPPORTED` | 준비 안 됨/미지원 안내. 동일 요청 무한 재시도 금지 |
| 같은 409 | `DRAFT_NOT_EDITABLE` | 상태 재조회. 최종저장 완료면 새 Case로 이동 |
| 같은 409 | `IDEMPOTENCY_CONFLICT` | 같은 requestId로 다른 내용을 보냈는지 확인 |
| `413 / REQ-003` | `PAYLOAD_TOO_LARGE` | JSON 본문1 MiB와 업로드 파일당50 MiB를 구분. 파일 한도는 승인된 현재 계약 |
| `422 / BIZ-001` | `VALIDATION_FAILED` | `issues`에 있는 `commandId/tableId/path/reason`으로 수정 위치 안내. 입력 제약·필수값·중복·선택지 오류를 표시하며 임의로 값을 바꿔 재전송하지 않음 |
| `422 / BIZ-001` | `RESPONSE_TOO_LARGE` | BE에 표 조회 정의/응답 크기 확인 요청. 빈 성공으로 처리하지 않음 |
| `500 / SYS-001` | `data:null` | 내부 오류. 쓰기 결과 불명 시 동일 requestId로 결과 확인. 내부 원문을 화면에 요구하지 않음 |

업로드에서는409 `UPLOAD_IN_PROGRESS`(같은 사용자의 직접 작성 존재), `REPLACEMENT_NOT_CONFIRMED`(기존 역할 교체 미확인), 최종저장에서는409 `FILES_INCOMPLETE`(네 역할 미완성)도 구분한다. 해당 요청을 새 UUID로 반복해 우회하지 않는다. 접수 후 업로드 실패의 issues는 FILE/SHEET/TABLE/HEADER 위치이며 편집 명령의 commandId가 있다고 가정하지 않는다. 안전한 reason/issues만 표시하고 원본 값·내부 경로를 추정해 붙이지 않는다. [업로드 계약 §3.3][be-upload-contract]

QRY-01 원본 구조 오류는 **항상 `data:null`**이므로 `data.reason`을 전제로 파싱하지 않는다. 인증/권한 오류는 기존 공통 처리도 유지한다. 작업 조회의 업무 실패는 위 HTTP 오류와 달리 `200 / data.status=FAILED / data.failure`에 온다. [오류 생성][be-errors] · [응답 처리][be-controller]

## 8. 실제 편집 지원과 미완료 범위

로컬 편집기는 고정 셀 시험 이후 실제 파일의 값·논리 행/기간 열·부분 계산·보존·성공 이력 재생으로 확장됐다. **아래는 로컬 검증 증거 요약이며 서버 전체 READY 목록이 아니다.** 표별 최신 검사/보류는 [편집기 기록의 Behavior Coverage·Current Status][be-editor-note], 업무 결정은 [domain 정본][be-domain]을 따른다.

| 범위 | 확인된 증거 / 인계 제한 |
| --- | --- |
| USCostofSupply, BCPrices, PMI 연도 | 다중 값/행/열과 이력 재생 및 일부 공개 API→복원→최종4파일 검증. PMI 표시 정책 미결과 실제 발행 연결은 별도 |
| Sector/Monthly, LNG Demand, LiqUtil/LiqProd | 값/행·기간 추가/삭제·보존 및 일부 공개 통합. 월0개 후 첫 월 재시작, 모드/연도 헤더도 검증 기록 존재. 전체 표의 모든 조합 검증을 뜻하지 않음 |
| BCRegas→Regas | 연결 행·연도/MAX·월/합계와 공개 API/최종파일 검증. 이전3개 승인 결함은 수정/회귀·독립 검토 완료. FE가 파생 행·합계를 별도 저장하지 않음 |
| BCInput·수요 조정/수소·선박연료·날씨 및 공급 기준/조정 표 | 표별 허용 입력/추가·계산·원본 보존 검증 기록. 비연속 본문/제외 영역을 server page로 표시. 실제 정책/수정 scope 발행은 별도 |
| TRADE 비용·거래 제한·배분·거시 변수 등 | 각 표의 확정 범위 검증 기록. 일부 잔여 표와 카탈로그 연결이 남아 있어 전체 TRADE 지원으로 확대하지 않음 |
| DemandAdjLogicInput | 기존 Parameters D/F·Bands H/J 숫자 입력과 영향 계산 검증. 혼합 I열/행 추가 지원으로 확대하지 않음 |
| Shipping 관련 경로 | 신규행 D91:D93 validation 충돌, 노선 AM232 실제 Excel 오류 타입 처리 결함2건은 사용자 보류. 해당 경로 완료/READY 발행 불가 |

작업 기록의 상태는 전체 In Progress이며 `842fe78f3`는 현 진행 결과의 중간 커밋이다. 처리기가 검사에 통과해도 실제 카탈로그 readDefinition/field/scope·발행 양식/Case 연결이 준비돼야 UI에서 사용할 수 있다. FE는 서버가 준 표/필드만 렌더링하고 미지원 표를 mock으로 채우지 않는다.

C의 과거99개 표시 항목 중92개 조회 표 DRAFT 등록은 [당시 결과][be-c-registration]를 참조한다. 나머지7개 보류와 이후 표별 구현을 하나의 ‘현재92개 편집 지원’ 숫자로 환산하지 않는다. application의 Case 기반 model-code를 임의 변경할 필요는 없으며, 업로드용 source-key/model-code 설정은 별도다.

기존 전용 컴포넌트(LiqUtil·BCRegas·Regas), 제외 열/Helper·다중 헤더는 실제 tableId/columns/controls로 대조한다. 저장용 fieldKey·rowId는 표시 이름이나 Excel 좌표로 새로 만들지 않는다. 부분 계산 성공은 외부 모델이 비연속 행/열을 소비하는 실제 모델 실행 성공의 증거가 아니다.

## 9. FE 착수 순서와 연동 완료 확인

### 바로 전달할 작업 범위

1. §3의 API 클라이언트/타입과 공통 BaseResponse 오류 처리를 연결한다. 원본 구조·Draft 구조·표 데이터·작업 상태는 다른 DTO로 다룬다.
2. §4.1의 Case 재개와 §4.4의 순차 업로드를 같은 Draft 화면 진입으로 연결한다. 업로드 성공 응답의 첫 표를 표시하고, 후속 조회는 현재 Draft/revision을 사용한다.
3. 서버 columns/page로 읽기 전용 렌더링부터 확인하고 READY인 명령·필드/control만 연다. 구조 변경 성공 후 전체 구조와 affectedTableIds를 재조회한다.
4. §5의6명령과 모드/헤더 요청을 계약 JSON으로 준비한다. 실제 표 쓰기 시험은 BE가 발행·연결한 지원 표로 한정한다.
5. 임시저장 완료 후 §6의 최종저장·BE 자동 실행 상태를 표시한다. FE가 원래 File[]를 다시 업로드하거나 Run을 자동 중복 접수하지 않는다.

요청/응답의 상세 예시는 [편집 명세 §7][be-edit-spec]와 [업로드 명세 §3.3][be-upload-contract]가 소유한다. 이 안내의 예시 UUID/Case/출처 키를 서버 값으로 하드코딩하지 않는다.

### 실제 서버 연동 전 BE/DBA 확인

| 항목 | 확인할 사실 |
| --- | --- |
| 코드 배포 | 업로드·편집기 중간 커밋 중 실제 서버 반영 범위를 확인. 웹과 처리 측의 이력 형식/기능 일치 |
| v2 전환·직접 업로드 DDL | [DDL §6.9/§6.10][be-ddl]와 [업로드 증분 SQL][be-upload-ddl]의 수동 적용/검증 완료 여부. 이번 문서 작업에서 실행하지 않음 |
| 발행 양식 | Case 연결 또는 최초 업로드용 ACTIVE 양식, 조회/고정 헤더 검증 규칙·쓰기 field/scope/capability 준비. 등록 완료만으로 발행 완료가 아님 |
| 기능 설정 | change-worker/finalization 및 업로드 enabled, upload.source-key/model-code의 실제 설정. 현재 업로드 기본 enabled=false이며 미설정 키를 FE가 추측하지 않음 |
| 시험 대상 | 편집 가능한 새 Draft·4역할 파일과 지원 표. 과거 FINALIZED 시험 Draft를 재사용하지 않음 |
| 실행 환경 | 사용자가 결정한 웹 Pod 동시 배포 방향과 실제 실행 구성을 확인. 과거 별도 Compose worker 시험을 필수 운영 구성으로 안내하지 않음 |

v2 수동 전환으로 기존 Draft/작업/키를 초기화하는 경우 FE도 기존 draftId/operationId/rowId/신규 열 키·미전송 요청을 폐기하고 새 Draft로 진입한다. 이는 [DBA 전환 계약][be-ddl]에 따라 실행하는 배포 절차이며 FE가 데이터 삭제 API를 호출한다는 뜻이 아니다. 원본 Case/파일/카탈로그는 이 전환의 초기화 대상과 구분한다.

### 연동 완료 확인 사례

- 목록 draftId UUID/null 분기, sourceCaseId/sourceVersion nullable, created:false 재개가 처리된다.
- 업로드 접수202에서 아직 없는 draftId를 요구하지 않고, 실패200도 완료로 표시하지 않는다. 성공 시 서버의 첫 표시 표·파일 준비 상태를 사용한다.
- 파일 교체 확인 취소/검증 실패는 기존 파일·편집을 유지한다. 새 역할 업로드 진행 중과 파일4개 미완성 상태에서는 최종저장을 열지 않는다.
- 비연속 본문/열·반복 헤더·마지막 페이지를 누락/중복 없이 표시하고, 늦게 도착한 다른 Draft/revision 응답을 섞지 않는다.
- EMPTY_SLOT/읽기 전용/미완비·불허·미지원은 수정되지 않는다. CREATE/SET/CLEAR·헤더 수정 제약을 구분하고0과 빈칸을 보존한다.
- ADD 뒤 임시키→서버 키를 반영하고, 성공한 새 revision으로 구조·영향 표를 재조회한다. 실패는 직전 성공 상태를 유지한다.
- 같은 requestId/동일 내용 재전송은 중복 저장하지 않는다. 버전 충돌·접근 상실·원본 변경을 자동 덮어쓰기로 해결하지 않는다.
- 최종저장과 모델 실행 성공/실패를 구분하고 종료 상태에서 폴링을 정리한다. 실행 실패 후 자동 재실행하지 않는다.

서버 저장분 되돌리기·Draft 편집본 다운로드·현재 업로드 작성 목록/검색은 이번 API 목록에 없다. 로컬 미저장분 취소, UP-04 작성 전체 폐기, 기존 원본 파일 다운로드를 서로 다른 기능으로 안내한다. 기본 출처의 자동 선택 정책도 Case 목록의 miBase만으로 추정하지 않는다.

## 10. 근거 링크

현재 BE 계약/구현은 저장소 상대 링크다. 편집기 중간 커밋 기준이며 다른 checkout/서버의 구현과 같다고 가정하지 않는다. FE GitHub 링크는 최초 조사 커밋 `4cccc5a7…`의 과거 근거이며 이번에 최신 FE 구현/원격 URL 응답을 확인하지 않았다.

- 계약: [입력 제공·업로드][be-provision], [편집·모드·헤더·최종저장][be-edit-spec], [조회 상세][be-query-spec], [Case 목록][be-list-contract].
- 코드: [Controller][be-controller], [DTO][be-dto], [Draft Service][be-service], [업로드 Service][be-upload-service], [파일 편집기][be-editor], [지원 판정][be-policy], [Repository][be-repository], [오류][be-errors].
- 업무·배포: [domain][be-domain], [DDL][be-ddl], [업로드 증분 SQL][be-upload-ddl].
- 검증/실행 기록: [편집기 — 미완료/보류 포함][be-editor-note], [업로드 완료][be-upload-note], [자동 실행 연결][be-auto-execution-note], [과거 F-O-01 실환경][be-smoke-result], [과거 C 등록][be-c-registration].
- 과거 FE 비교: [협의 전 제안][fe-spec], [입력 팝업][fe-popup], [표 로더][fe-table], [로컬 파서][fe-preview], [좌표/편집 판정][fe-layout], [Case 서비스][fe-service].

[fe-spec]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/docs/ko/fundamental-model-sheet-api.md
[fe-popup]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelInputEditorPopup.tsx#L95-L345
[fe-table]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelSheetDataTable.tsx
[fe-preview]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/fundamentalModelSheetDataMock.ts
[fe-layout]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/sheetTemplates/summaryTemplateLayout.ts
[fe-service]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/service/mi/fundamentalModel/case.ts
[fe-list]: https://github.com/sk-mi-platform/frontend/blob/4cccc5a7d8c993f6fef26658067ad09fa4ef4ae0/src/pages/mi/fundamentalModel/components/FundamentalModelRunList.tsx#L266-L274
[be-query-spec]: mi-fundamental-model-input-query-api-proposal.md
[be-edit-spec]: mi-fundamental-model-input-draft-editing-api-proposal.md
[be-auto-execution-contract]: mi-fundamental-model-input-draft-editing-api-proposal.md#921-최종저장-후-자동-실행--2026-10-19-사용자-승인
[be-finalization-controller]: ../../src/main/java/com/sk/domain/mi/controller/fundamentalmodel/ModelInputDraftController.java
[be-auto-execution-note]: ../work-notes/2026/2026-10-19-input-draft-run-connection.md
[fe-auto-execution-prompt]: ../references/model-input/2026-10-19-input-draft-auto-execution-frontend-task-prompt.md
[be-provision]: mi-fundamental-model-input-provision-feature-spec.md
[be-controller]: ../../src/main/java/com/sk/domain/mi/controller/fundamentalmodel/ModelInputDraftController.java
[be-dto]: ../../src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/ModelInputDraftResponses.java
[be-service]: ../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/ModelInputDraftService.java
[be-repository]: ../../src/main/java/com/sk/domain/shared/repository/master/tmm/ModelInputDraftRepository.java
[be-errors]: ../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputDraftException.java
[be-cases]: ../../src/main/java/com/sk/domain/mi/controller/fundamentalmodel/FundamentalModelCaseController.java
[be-list-contract]: mi-fundamental-model-mi-base-designation-feature-spec.md
[be-list-dto]: ../../src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/FundamentalModelCaseListItemRes.java
[be-smoke-result]: ../work-notes/2026/2026-09-03-model-input-transfer-format.md#case105-106-smoke-20261019
[be-c-registration]: ../work-notes/2026/2026-09-03-model-input-transfer-format.md#c-structure-registration-20261019
[be-run]: ../../src/main/java/com/sk/domain/mi/controller/fundamentalmodel/FundamentalModelCaseDedicatedController.java
[be-domain]: ../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md
[be-ddl]: ../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md
[be-upload-ddl]: ../model-input-incremental-upload-ddl-draft.sql
[be-upload-contract]: mi-fundamental-model-input-provision-feature-spec.md#33-업로드-초안-생성파일-추가-api--파일명-역할-판별순차-업로드
[be-upload-service]: ../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputUploadService.java
[be-editor]: ../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputWorkbookEditor.java
[be-policy]: ../../src/main/java/com/sk/domain/mi/service/fundamentalmodel/input/InputWorkbookPolicy.java
[be-editor-note]: ../work-notes/2026/2026-10-19-model-input-excel-editor.md
[be-upload-note]: ../work-notes/2026/2026-10-19-model-input-incremental-upload.md
[be-row-json]: mi-fundamental-model-input-draft-editing-api-proposal.md#fe-change-cases-20261019
[be-column-json]: mi-fundamental-model-input-draft-editing-api-proposal.md#fe-final-column-json-20261019
