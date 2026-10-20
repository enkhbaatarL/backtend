# MI Fundamental Model Input 제공 Feature Spec

## Document Control

- status: Draft
- canonical for: MI Fundamental Model input 조회·업로드 파싱 API의 요청 식별 경계, 응답 범위와 검토 중인 endpoint/field/error 계약. 저장 Case 숫자 표시 정책은 §3.2, QRY-01/02의 추출 표시 JSON에 대한 DRM 비적용은 §5가 소유한다.
- owner: Shared (BE / FE / Security·DRM)
- source documents:
  - 2026-10-19 업로드 처리 상한 사용자 초기 채택: 일단120초를 적용한다. 접수 후 대기열 포함·전송 시간 제외와 TIMEOUT은 §3.3·domain 규칙67을 따르며 실제 성능 검증은 남는다.
  - 2026-10-19 취소 후 새 첫 파일 실패 사용자 확정: A의 취소를 유지하고 B 최초 파일 검증 실패 시 다시 업로드한다. A 자동 복구 없이 §3.3·domain 규칙67을 따른다.
  - 2026-10-19 미완료 작성 취소/새 시작 사용자1안 채택: 진행 작업이 없을 때 취소 안내/확인 후 새 Case를 시작하고 확인 전 기존 내용을 유지한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자 최종 결정: 파일 직접 업로드 초안은 같은 사용자가 한 번에 Case 하나를 작성한다. 과거 복수 허용/재검토를 대체하며 §3.3·domain 규칙67을 따른다.
  - 2026-10-19 과거 재논의 이력: 업로드 초안 목록/선택 재개를 제외하고 동일 사용자 복수 허용을 보류했다. 이후 직접 업로드 단일 작성 결정으로 확정했으며 §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자 정정: 파일 교체는 다른 파일의 표에 영향을 주지 않는다. 파일 간 재계산/연결 영향 미결은 제거하고 §3.3·domain 규칙67의 교체 범위를 따른다.
  - 2026-10-19 업로드 실패 후 재시도 추천안 채택: FAILED 후 같은 requestId/동일 내용은 기존 실패 결과만 반환하고 재시도는 새 requestId·후속 현재 baseRevision으로 요청한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 초안 파일 준비 상태 추천안 채택: uploadedKinds/missingKinds/canFinalize를 서버 기준으로 제공하고 처리 중 파일을 완료 목록에서 제외한다. §3.3을 따른다.
  - 2026-10-19 업로드 오류 상세 사용자1안 채택: failure에 사유 코드와 issues 목록으로 파일 역할/시트/표/헤더 등 문제 위치를 구분해 제공한다. §3.3을 따른다.
  - 2026-10-19 업로드 상태 조회 추천안 채택: 접수202와 operationId/status/statusEndpoint를 반환하고2초 간격 상태 조회를 SUCCEEDED/FAILED에서 종료한다. 공통 조회 경로·HTTP/상태 의미는 §3.3을 따른다.
  - 2026-10-19 multipart 전달 형식 사용자1안 채택: file part에는 파일, metadata part에는 requestId/baseRevision/replaceConfirmed 등을 담은 JSON을 보낸다. §3.3을 따른다.
  - 2026-10-19 업로드 최초 표시 사용자2안 채택: 업로드 처리 성공 시 해당 파일의 첫 표를 자동 선택하고 값까지 제공한다. 기존 저장 Case의 미선택 정책과 적용 범위를 구분하며 §3.3을 따른다.
  - 2026-10-19 교체 확인 필드 사용자1안 채택: 확인 후 replaceConfirmed:true를 제출하고 기존 역할 파일이 있는데 확인값이 없거나 false면 거절한다. §3.3을 따른다.
  - 2026-10-19 후속 파일 revision 사용자1안 채택: 파일 추가·교체 요청에 baseRevision을 보내 현재 버전과 다르면 거절한다. 성공 시 revision은1회 증가하고 실패 시 유지한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 초안별 변경1건 사용자1안 채택: 같은 초안의 변경은 한 번에 하나만 허용하고 처리 중 기존 성공 상태/결과 조회는 가능하게 한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 업로드 비동기 사용자1안 채택: 파일 수신 후 접수 결과를 반환하고 비동기로 검증·파싱한다. 성공 뒤에만 초안 생성/파일 연결을 확정한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 최초 업로드 재전송 사용자1안 채택: 같은 requestId·동일 파일/요청 내용은 기존 초안·결과 반환, 같은 requestId의 다른 내용은 거절한다. §3.3을 따른다.
  - 2026-10-19 API 분리 사용자1안 채택: 첫 파일의 초안 생성 API와 기존 초안의 파일 추가·교체 API를 분리한다. 두 경로와 draftId 경계는 §3.3을 따른다.
  - 2026-10-19 파일 용량 사용자1안 채택: 단일 업로드 파일 상한은50MiB(52,428,800바이트)다. §3.3을 따른다.
  - 2026-10-19 사용자1안 채택: 최종저장도 각 역할의 원본 형식을 유지한다(.xlsx→.xlsx,.xlsm→.xlsm). 지원 형식 확정과 함께 §3.3·domain 규칙16/67을 따른다.
  - 2026-10-19 사용자2안 채택: 업로드는.xlsm/.xlsx를 모두 지원한다. 실제 파일 형식·고정 양식 구조 검증과 원본 보존을 적용하며 최종저장 형식 유지/변환은 별도 결정한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 과거 사용자1안 채택(직접 업로드 단일 작성 결정으로 대체): 동일 사용자의 복수 업로드 초안을 허용하고 파일·편집 내용·고정 템플릿 버전을 초안마다 독립적으로 유지한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자1안 채택: 부분 업로드에서 필요한 역할 파일이 없는 동작만 제한하고 가능한 승인 편집은 계속 허용한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자1안 채택: 동일 역할 파일은 편집 초기화를 안내·확인 후 검증 성공 시 교체하며 검증 실패 시 기존 파일/편집을 보존한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자1안 채택: 첫 정상 파일의 ACTIVE 양식 버전을 업로드 편집 초안에 고정하고, 새 ACTIVE는 새 초안부터 적용한다. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자1안 채택: 첫 정상 파일에서 업로드 편집 초안 생성, 이후 파일은 같은 초안에 누적, 실제 Case는4역할을 갖춰 최종저장 성공 시 생성. §3.3·domain 규칙67을 따른다.
  - 2026-10-19 사용자 추가 요구: 단일 파일명 역할 판별·ACTIVE 양식 검증·누적 업로드 편집·4역할 완성 후 최종저장·불일치 시 Case 생성/파일 연결 금지. 기존 단일 파싱 제한을 §3.3에서 확장한다.
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/references/mi-model-input-catalog-inventory-2026-09-28.md`
  - frontend `docs/ko/fundamental-model-sheet-api.md` (`feature/parkjaehyun`, `014733c372bf75665b7b2c541657cf32711e032d`) — FE 요구 근거, 이 문서와 동일한 계약으로 확정된 것은 아님
  - frontend `docs/ko/fundamental-model-sheet-api.openapi.yaml` version `0.4.0` — 필드 이름/셀 표현 재사용 근거
- depends on:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `SECURITY.md`
- supersedes:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`의 기존 `API Slice C — Input 내용 제공` 초안
- last synced: 2026-10-19 (개발 시트 규칙39~40 유지; 규칙67의 순차 업로드·편집 진입 요구 반영)
- open questions:
  - 직접 업로드 계약·저장/복구 경계는 §3.3·domain §5.6·DDL §6.10으로 완결했다. 실제 MySQL 제약/DDL 적용·실제 저장소와 대표 대용량 파일 성능·고객 배포 실행 위치는 배포 전 확인 대상이며 구현 미결과 구분한다.
  - sheet/table projection의 원본 수식은 셀 표시 데이터로 전달하지 않는다. 별도 화면 갱신 규칙의 JSON 필드 이름·타입, 참조/대상 표현, 지원 계산 방식과 적용 순서를 확정해야 한다. 구체 범위는 §3.1C를 따른다.
  - 현재 조사 후보의 최종 표시 table 목록과 표 경계/header/row identity를 `INPUT-03`에서 확정해야 한다. 조회 필드와 수정 허용 범위는 별도로 정한다.
  - 연결 편집표가 다른 표의 값을 필요로 할 때의 추가 조회 흐름을 확정해야 한다. 단일 표 값 응답에 다른 표의 값을 암묵적으로 포함하지 않는다.
  - domain 규칙 47의 LiqUtilAdjustment 행·열 삭제 허용을 전달하는 구조 정책 표현과 삭제 대상 범위·위치 제한·삭제 후 Period/수식 갱신의 write 계약을 확정해야 한다.
  - LNG Demand 모드의 값 셀은 C1이고 선택값 변경은 domain 규칙 50이 허용한다. 판별대장 T-020은 이 control을 T-021 표와 함께 표시하도록 확인했다. 원본 선택지의 `Dynamaic` 오타를 고칠지와 화면 선택지·저장값 표기는 아직 결정되지 않았으며, 결정 전에는 options 값을 확정하지 않는다.
## 1. Feature Goal

계약 탐색 경계: 저장 Case의 사용자 확정 QRY-01/02는 [조회 상세 §1.1·§3~5](mi-fundamental-model-input-query-api-proposal.md)가 소유한다. 이 문서의 그 범위에 대한 과거 미결 문구는 재질문 대상이 아닌 통합 대기 이력이다. 신규 Draft 생성/재개·조회·임시저장 접수/결과의 BE 추천 계약은 [공통 편집 API 초안](mi-fundamental-model-input-draft-editing-api-proposal.md)이 소유한다. 공통 BE 묶음은 2026-10-16 사용자 채택했으며 해당 문서 전체는 Draft다. Sector의 채택 정책은 domain 규칙45, 공개 검증은 편집 API §4.1이 소유한다. 최종저장 전체 계약은 그 초안 §9의 다음 slice다. 아래 조회/업로드 계약을 Draft 쓰기 계약으로 확대하지 않는다.

선택한 Fundamental Model `caseId`의 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` input에 대해 화면의 `파일 → 시트 → 테이블` 구조를 그리는 정보는 네 파일 전체를 제공하고, 헤더와 실제 입력값은 선택한 논리 입력 영역 하나 및 명시적으로 연결된 부속 control만 제공한다. 원본 파일은 FE 응답으로 전달하지 않는다.

이 문서는 MI Base 목록·상세·지정 API를 정의하지 않는다. 해당 계약은 [MI Fundamental Model MI Base 지정 Feature Spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 소유한다.

사용자 업로드 파일은 파일명에서 역할을 판별한 뒤 ACTIVE 양식 정의로 검증·파싱하고, §3의 조회 응답과 같은 구조/셀/control 표현으로 화면에 제공한다. 2026-10-19 요구에 따라 파일을 하나씩 모아 새 Case 편집·최종저장으로 연결하는 진입을 추가한다. 업로드 operation의 확정 범위와 잔여는 §3.3·Open Questions를 따른다.

## 2. Fixed Decisions

- 저장된 case input 원본 4개의 소유 단위와 요청 대상 식별자는 `caseId`다. 아직 case가 없는 단일 파일 업로드는 §3.3의 별도 요청 경계를 따른다.
- `runId`, `execType`, `fileNo`, bucket, object key는 대상 실행 건을 선택하는 공개 요청 식별자로 사용하지 않는다.
- 서버가 `caseId + SCNRCASE relation`으로 연결된 원본을 선택한다.
- 원본 Excel bytes를 Base64로 JSON에 넣는 방식은 비파일 전달로 보지 않으며 후보에서 제외한다.
- object storage 경로, bucket, object key, 원본 binary를 응답에 포함하지 않으며 ZIP 또는 역할별 원본 파일 다운로드 API를 제공하지 않는다.
- 화면 탐색용 구조 정보는 `file role → sheet → table` 계층으로 네 역할 전체를 제공한다. 이 전체 구조에는 각 표의 실제 셀 값을 넣지 않는다.
- 값 조회는 선택한 table 하나의 헤더·행·열·값과 §3.1B의 부속 control을 반환한다. 다른 table의 grid나 sheet 전체의 값은 함께 반환하지 않는다. 선택한 표의 읽기 전용 값도 표시 데이터에 포함할 수 있으며 조회 범위를 수정 가능 셀로 제한하지 않는다.
- 여기서 `table`은 시트 안의 선택 가능한 논리 입력 영역이다. 일반 표, 표와 부속 UI control(드롭다운 등)의 조합, control만 있는 영역을 포함한다. 선택 영역의 부속 control은 §3.1B의 명시적 연결에 따라 함께 반환할 수 있다.
- 표 단위 조회와 API 호출 수는 별개다. 전체 구조와 단일 표 값을 한 응답에 담을지 별도 operation으로 나눌지는 Open Questions에서 결정한다. raw workbook 또는 범용 전체 sheet cell grid를 화면 계약으로 사용하지 않는다.
- 백엔드 양식 카탈로그가 표시 대상 테이블, 필드 정의, 수정 가능 범위, 행·열 추가 정책, 사용자가 선택할 header/값 후보, 테이블 간 영향 관계를 소유한다.
- 이번 release의 구조 변경 포함과 마지막 행·열 추가 중심 원칙, 중간 삽입·삭제 예외는 domain design §6 규칙 13·26·45·47을 따른다. 조회 응답의 구조 정책과 화면 갱신 규칙은 이 범위를 표현해야 하며, 구체 구조 변경 요청·검증·저장은 별도 write 계약에서 확정한다.
- 2026-10-06 사용자 전달 FE 합의: 수식 셀도 화면에는 값으로 전달하고, 입력/열 추가 시 필요한 화면 갱신은 별도 규칙 정보로 제공한다. FE가 적용할 규칙의 전달 범위와 미결정 표현은 §3.1C를 따른다.
- 전수 조사에서 분류된 화면 대상은 후보일 뿐이며 최종 표시 table 목록과 표 경계는 별도로 확정한다. 표 단위 데이터 제공이라는 결정만으로 표 내부 일부 field만 선별하거나 계산값을 모두 조회에서 제외한다고 해석하지 않는다. 조회 대상과 수정 허용 범위는 구분한다.
- 개발 전용 시트의 설명에서 도출한 후보도 같은 개발본의 운영 대상 business sheet 구조와 대조·승인한 뒤에만 catalog seed로 등록한다.
- 이 문서는 조회와 단일 파일 검증·순차 업로드 진입 계약을 소유한다. 편집·임시저장·최종저장의 write 계약은 기존 편집 API가 소유하며 원본 Case 없는 업로드 진입 연결은 이번 후속에서 확정한다. `미리보기` action은 두지 않는다.
- 응답 범위와 공통 필드 이름/셀 표현은 §3과 같이 고정한다. endpoint와 operation별 필수/nullable·초기 선택은 아직 확정하지 않는다.
- `SUMMARY`, `플랫폼 입력 안내`가 포함된 파일의 수용·원본 보존과 기준 정규화본의 역할은 [domain 규칙39~40](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#6-business-rules)을 따른다. 두 개발 시트는 runtime catalog와 조회 응답에 포함하지 않으며, 파일 안에 있다는 이유만으로 호환 파일의 연결을 거부하지 않는다. 이 결정은 §3.3의 미구현 업로드 파싱 API가 구현됐다는 뜻이 아니다.
- 운영 기준 정규화본의 hidden sheet는 조회 projection, Draft 변경, dependency 분석과 Backend 계산에서 제외한다.
  최종 XLSM 생성 시에는 source snapshot에서 순서·이름·숨김 상태와 worksheet/package 내용을 그대로 복사한다.
  hidden sheet를 삭제하거나 빈 sheet로 다시 만들지 않는다.
- 수식과 직접 참조가 없는 것으로 확인된 visible sheet 6개는 Draft 변경, dependency graph와 Backend workbook 재계산에서 제외하고 최종 XLSM에는 원본 상태로 보존한다. 화면 표시는 둘로 나눈다.
  - read-only 표시 sheet인 SUPPLY `LNG Summary`는 source snapshot 값을 조회 응답에 표시하되 수정 가능 field를 제공하지 않는다. TRADE `backupLNG_TradeRestrictionInput`은 domain 규칙 57의 미표시 대상이다.
  - 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`, SUPPLY `Input Upstream ->`, `Input Midstream ->`)는 조회 응답에 포함하지 않는다.
- domain design §6 규칙 57에 따라 최신 SUMMARY에서 표 전체가 미표시로 확정된 표는 조회 응답에 포함하지 않는다. 표 일부 열의 미표시와는 구분한다.
- TRADE `4.5 PriceModelInput`은 domain 규칙 44에 따라 T-183/T-184를 수정 가능한 표로 제공한다. 선택 표의 `inputCells`는 승인 입력을 포함하며 고정 제목·헤더는 조회만 제공한다. 입력 범위·아래 행 추가·열/기간 고정 및 최종 파일 반영/보존 경계는 규칙 44가 소유한다. 규칙 57의 미표시 T-177~T-182와 U열은 화면에서 제외하며 다른 표의 수정 가능 field로 노출하지 않는다.
- 2026-10-06 사용자 결정: SUPPLY `ProductionAdjustment`는 **C — 구조변경**이며 행 추가가 가능하다. `L7:L200`은 헤더/본문 모두 화면에서 제외한다. 해당 값과 수정 위치는 `tableData.columns/rows/inputCells`에 넣지 않고 control로 우회 노출하지 않는다. 이 범위의 `L7`은 헤더, `L8:L200`은 기존 오류 수식이다. 기존 L8:L200 수식은 원본 그대로 보존하고 신규 행의 L셀은 빈칸으로 둔다. 기존/신규 L셀의 화면·재계산 제외 및 오류 판정은 domain 규칙 31/32를 따른다. 그 밖의 행 추가 write 상세는 별도 계약으로 확정한다.
- 기존 3파일 `INDEPENDENT | MI_BASE` case는 4파일 cutover 전에 soft delete한다. `OUTPUT`을 보완하거나
  신규 template에 mapping하지 않으며, 삭제 후 모든 active-resource API에서 `404 RSC_NOT_FOUND`로 취급한다.
- `INDEPENDENT | MI_BASE`의 legacy cleanup은 case master의 `del_yn`만 변경한다. 연결 파일·run history·결과 데이터의 physical delete는 이 계약 범위가 아니다. 이 조회 API가 다루지 않는 3파일 `SCENARIO` case의 정리 범위는 domain 규칙 36을 따른다.

## 3. Fixed Response Scope — 전체 구조 + 선택한 표 하나의 값

2026-10-01 사용자 결정에 따라 전체 snapshot/role/sheet 단위의 실제 값을 일괄 반환하는 후보를 선택지에서 제외한다. 반환 단위는 아래 두 가지다.

| 정보 | 반환 범위 | 포함 내용 | 포함하지 않는 내용 |
| --- | --- | --- | --- |
| 화면 구조 | DEMAND/SUPPLY/TRADE/OUTPUT 네 파일의 전체 표시 구조 | 파일 역할·표시명/순서, 하위 시트명/순서, 표 ID·이름·설명/순서, 위치·좌표 등 표를 선택하고 렌더링할 구조 정보 | 모든 표의 실제 값, 전체 sheet grid |
| 선택 영역 데이터 | 요청으로 선택한 table 하나와 명시적으로 연결된 부속 control | 해당 표의 표시 헤더·열·행·값, 다중 헤더/기간 정보, 원본 위치와 수정 가능 정보, 부속 control의 현재 값·선택지 | 다른 표의 grid 값, 연결되지 않은 control 값, 전체 sheet 값 |

여기서 전체 구조는 **화면 표시 대상의 전체 구조**다. §2에서 확정한 hidden/development sheet, 빈 구분 탭, TRADE U열 등 제외 정책을 해제하는 결정은 아니다.

### 3.1 공통 응답 구조 — FE 필드 이름 재사용

2026-10-01 사용자 선택에 따라 FE의 `sourceKey`, `sections → sheets → tables`와 `columns/rows/inputCells/inputGuideRules` 이름 및 셀 표현을 유지한다. 기존 `sheets[].data`는 제거하고 선택 표 데이터를 `tableData`에 둔다. 별도 `structure.files`, `headers`, `editingInfo` 응답 필드는 만들지 않는다.

```text
BaseResponse.data
  sourceKey
  sections[DEMAND, SUPPLY, TRADE, OUTPUT]
    kind
    inputId
    sheets[]
      sheetName
      tables[]    ← ID·이름·순서·좌표 등 전체 구조 정보; 실제 셀 값 없음
  tableData
    tableId       ← sections[].sheets[].tables[].id에 해당하는 선택 표
    columns
    rows          ← 선택 표의 헤더 행과 본문 값만
    inputCells
    inputGuideRules
    controls[]    ← 선택 영역에 연결된 드롭다운 등의 정의·현재 값
```

전체 구조와 선택 표 값을 동봉할 때의 공통 응답 구조다. 구조 조회/값 조회를 별도 operation으로 나눌지, 최초 표 선택 전 `tableData`를 어떻게 표현할지는 Open Questions에서 확정한다. 아래 타입은 이번 공통 표현의 기준이며 operation별 required/nullable 계약을 대신하지 않는다.

| 필드 | 타입/내용 | 의미 |
| --- | --- | --- |
| `sourceKey` | string | BE가 결정하는 출처 식별자. source 선택과 응답 대조에 사용하며 저장된 출처의 원본 선택/권한 경계는 기존 `caseId + SCNRCASE`를 유지한다. |
| `sections` | array | 저장 case의 전체 구조 조회에서는 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 순서로 각 역할 정확히 하나. 업로드 응답은 §3.3의 해당 kind 하나다. 배열 순서가 화면 표시 순서다. |
| `sections[].kind` | `DEMAND \| SUPPLY \| TRADE \| OUTPUT` | 파일 역할. |
| `sections[].inputId` | string | 해당 역할의 원본/metadata 기준을 구별하는 서버 식별자. 파일명이나 저장소 경로를 쓰지 않는다. 발급·유효기간·전체 set version 표현은 미정이다. |
| `sections[].sheets` | array | 표시할 시트 전체를 순서대로 반환. hidden/development sheet 등 기존 제외 정책을 따른다. |
| `sheets[].sheetName` | string | 원본 시트명. 역할과 함께 시트를 식별한다. |
| `sheets[].tables` | array | 해당 시트의 선택 가능한 입력 영역 metadata 전체. `id`, `kind`, `sheetName`, `name`, `description`, `location`, `rowPolicy`, `columnPolicy`, `coordinates`를 FE와 같은 이름으로 유지하고 §3.1B의 `controlIds`를 추가한다. 셀 값은 포함하지 않는다. |
| `tableData.tableId` | string | 선택한 입력 영역의 `tables[].id`와 일치한다. 값 응답은 이 영역의 grid 및 명시적으로 연결된 부속 control만 담는다. |
| `tableData.columns` | array of `{field, headerName}` | 선택 표의 표시·헤더·빈 입력 셀에 필요한 원본 열. `field`는 원본 0-based 열 번호의 `columnN`, `headerName`은 Excel 열 문자다. 다중 헤더 문자열은 rows에서 읽는다. |
| `tableData.rows` | array of `{id, rowNumber, columnN: string \| number}` | 선택 표의 필요한 헤더 행과 본문 값. 원본 1-based `rowNumber` 오름차순, `id = rowNumber - 1`을 사용하고 범위별로 재번호화하지 않는다. 이 `id`는 source/표 범위 안의 화면 행 식별자이며 구조 변경 후 영속 row key를 대신하지 않는다. |
| `tableData.inputCells` | integer array | 선택 표의 수정 가능 셀을 `rowNumber * 16384 + zeroBasedColumnIndex`로 표현. 빈 입력 셀 포함. 없으면 `[]`. 수정 가능 판정은 BE의 확정 정책을 따른다. |
| `tableData.inputGuideRules` | array | 선택 표에 적용되는 안내 metadata. 없으면 `[]`. FE의 `sheetName`, `range`, `role`, `item`, `emptyHandling`, `startRow`, `endRow`, `startColumn`, `endColumn` 이름과 `scenario/reference/conditional` 역할 표현을 유지한다. 범위의 행은 1-based, 열은 0-based, 양 끝 포함이다. |
| `tableData.controls` | array | 선택 영역의 `controlIds`에 명시된 UI control을 표시 순서대로 반환한다. 일반 표에 control이 없으면 `[]`. 타입과 값/binding은 §3.1B를 따른다. |

- 표 `id`는 전체 sections 안에서 유일하고 동일 논리 표는 출처가 달라도 같은 ID를 유지한다. 우선 기존 FE의 `kind/sheetName/name` ID를 재사용하며 표명 변경 시 ID/전용 컴포넌트 영향은 함께 검토한다.
- 표 metadata의 `kind`와 `sheetName`은 상위 section/sheet와 일치한다. `location`과 `coordinates`는 같은 표 범위를 설명하며 header/기간 행도 해당 표의 표시 기준이다.
- `inputId`가 구별하는 source 기준이 다르면 이전 데이터·편집 초안을 다른 원본에 적용하지 않는다. 단일 표 조회의 실제 request 연결과 stale 응답 처리 계약은 operation 확정 때 명시한다.
- `rowPolicy/columnPolicy`는 기존 표시 문구를 유지한다. 실제 행·열 변경은 표별 확정 정책을 따라야 하며 현재 FE의 문자열 분기나 전용 컴포넌트 예외를 BE write 허용 근거로 삼지 않는다.
- `inputGuideRules`는 개발 전용 안내 sheet 자체를 응답에 넣는다는 뜻이 아니다. 필요한 안내를 catalog metadata로 제공할 수 있으며 색이나 안내 sheet 존재를 실행 때의 편집 권한 원천으로 삼지 않는다. 안내 범위가 표시된다는 사실만으로 모든 셀이 수정 가능해지는 것도 아니다.

### 3.1A 선택 표 데이터 예시

아래는 형식 검토용 가상 값이다. 전체 응답 중 `tableData`만 발췌했고 행도 축약했다. 실제 조회에서는 선택 표의 계약 범위 전체를 반환한다.

```json
{
  "tableData": {
    "tableId": "DEMAND/BCInput/Base Case Hydrogen Demand",
    "columns": [
      { "field": "column53", "headerName": "BB" },
      { "field": "column54", "headerName": "BC" },
      { "field": "column55", "headerName": "BD" },
      { "field": "column56", "headerName": "BE" }
    ],
    "rows": [
      { "id": 2, "rowNumber": 3, "column53": "Country", "column54": "2025", "column55": "2026", "column56": "2027" },
      { "id": 3, "rowNumber": 4, "column53": "Example Country", "column54": "1.00", "column55": "1.10" }
    ],
    "inputCells": [],
    "inputGuideRules": [],
    "controls": []
  }
}
```

### 3.1B 표와 부속 UI control

`tables`/`tableData` 이름을 유지하되 모든 입력 영역에 행·열 형태를 강제하지 않는다. 일반 표는 기존 grid 필드와 `controls: []`를 사용한다. 표 위에 드롭다운이 있으면 grid와 controls를 함께 반환한다. 모드 선택만 있는 영역은 `columns/rows/inputCells/inputGuideRules: []`와 controls를 반환하며 가짜 헤더/본문 행을 만들지 않는다.

- `tables[].controlIds`: 해당 입력 영역에 함께 표시할 control ID 배열. 없으면 `[]`. 현재 값·선택지는 전체 구조에 넣지 않고 선택 데이터의 controls에만 담는다. 동일 control을 여러 영역에서 사용하면 동일 ID를 참조한다.
- 각 control의 `id`는 template 안에서 안정적이고 유일하다. `sourceTableId`는 그 값의 소유 영역을 가리키며 전체 구조의 기존 `tables[].id`와 일치한다. `tableData.tableId`는 사용자가 선택한 영역이고 `sourceTableId`는 control의 소유 영역이므로 둘은 다를 수 있다. 공유 control도 같은 source/template 및 같은 sheet 안에서 catalog가 명시적으로 연결한 경우만 동봉한다.
- grid와 control은 표시 형태만 다르다. 동일 원본 셀의 현재 값을 두 표현에 중복하지 않는다. control로 표현한 값 셀은 rows의 해당 셀 필드에서 제외하고 control의 value가 소유한다. 선택 영역 밖 다른 표의 grid 전체를 control에 넣어 반환하지 않는다.

| control 필드 | 타입 | 의미 |
| --- | --- | --- |
| `id` | string | `controlIds`에서 참조한 control 식별자. |
| `type` | `select` | 현재 구체 사례는 단일 선택 드롭다운이다. 다른 UI 유형은 실제 조사 사례에 따라 별도 타입 계약을 추가한다. 임의 JSON을 받는 확장 필드는 두지 않는다. |
| `label` | string | 화면에 표시할 제목. |
| `position` | `beforeGrid` | 표 위에 표시. control만 있는 영역에서도 이 순서로 렌더링한다. controls 배열이 control 사이의 표시 순서다. |
| `sourceTableId` | string | 실제 값/편집 위치를 소유한 입력 영역 ID. |
| `binding` | `{rowNumber, columnIndex}` | 소유 영역의 원본 sheet에 있는 값 셀. 행은 1-based, 열은 0-based. 렌더링 control ID와 원본 셀 주소를 구분한다. |
| `value` | string \| number \| null | 현재 저장값. null은 미선택/빈 값이며 grid 셀의 null 금지와 별도 계약이다. options에 없는 기존 값도 숨기거나 첫 선택지로 자동 대체하지 않는다. |
| `options` | array of `{value, label}` | 선택지의 저장값(string 또는 number)과 표시명. 순서 유지, 저장값 중복 금지. 원본/확정 catalog를 기준으로 제공하고 FE가 값을 하드코딩해 보충하지 않는다. |
| `editable` | boolean | catalog의 확정 편집 정책을 따른다. false여도 현재 값과 선택지를 조회할 수 있다. 드롭다운을 렌더링한다는 사실만으로 수정 권한이 생기지 않는다. |

`inputCells`는 grid로 표시한 수정 가능 셀에 사용하고 control 수정 여부는 `editable`로 표현한다. 두 표현 모두 같은 BE 정책에서 도출한다. control 변경을 저장할 때의 request와 검증은 별도 write 계약에서 control ID를 소유 영역/binding에 연결해 정의한다. 조회 응답만으로 원본 셀 쓰기나 재계산을 수행하지 않는다.

**LNG Demand 적용:** 2026-10-07 사용자 확인으로 T-020 `LNG Demand Mode`는 판별대장의 **E 부속 control**이며 T-021 `Annual LNG Demand Reference`의 grid와 함께 반환한다. 현재 FE의 `DEMAND/LNG Demand/LNG Demand Mode`는 `B1:C1`, 설명 `Fixed / Dynamic 선택`, `headerRow: 0`인 항목이며 이 기존 ID를 모드 control의 소유 영역으로 유지한다. 실제 값 셀은 C1(`binding: {rowNumber: 1, columnIndex: 2}`)이고 domain 규칙 50의 선택값 편집 허용을 따른다. catalog는 연간표의 `controlIds`로 연결하며 C1을 별도 grid 셀로 중복 전달하지 않는다. 원본 선택지 오타의 수정 여부와 화면 선택지·저장값 표기는 Open Questions로 유지하며 FE의 현재 열 추가 예외를 BE 변경 허용으로 해석하지 않는다.

아래는 확정된 C1 binding·연간표와의 연결·편집 허용을 반영한 **형식 검토용 예시**다. control ID, 현재 `value`, `Fixed/Dynamic` options 표기는 설명용이며 실제 저장값·선택지를 확정하는 근거가 아니다. grid는 설명을 위해 축약했다. 이 예시의 선택 영역 metadata에는 `controlIds: ["DEMAND/LNG Demand/mode"]`가 있어야 한다.

```json
{
  "tableData": {
    "tableId": "DEMAND/LNG Demand/Annual LNG Demand Reference",
    "columns": [{ "field": "column2", "headerName": "C" }],
    "rows": [{ "id": 3, "rowNumber": 4, "column2": "Example Country" }],
    "inputCells": [],
    "inputGuideRules": [],
    "controls": [{
      "id": "DEMAND/LNG Demand/mode",
      "type": "select",
      "label": "LNG Demand Mode",
      "position": "beforeGrid",
      "sourceTableId": "DEMAND/LNG Demand/LNG Demand Mode",
      "binding": { "rowNumber": 1, "columnIndex": 2 },
      "value": "Fixed",
      "options": [{ "value": "Fixed", "label": "Fixed" }, { "value": "Dynamic", "label": "Dynamic" }],
      "editable": true
    }]
  }
}
```

### 3.1C 셀 표시값과 별도 화면 갱신 규칙

2026-10-06 사용자 전달 FE 합의다. `rows`의 수식 셀은 계산 결과값으로 표시하며 원본 수식 문자열을 셀 값으로 보내지 않는다. 입력 변경·열 추가에 따라 다른 표시값이 바뀌어야 하면 별도 갱신 규칙을 제공하고 FE가 화면에서 적용한다. FE는 수식 여부를 알아내거나 workbook 수식을 직접 해석하지 않는다.

- 규칙은 발동 동작, 참조할 입력, 갱신 대상과 계산/초기값 생성 방법을 식별할 수 있어야 한다. 구체 JSON 필드 이름·타입·연산 표현·적용 순서는 Open Questions이며 지금 임의의 응답 필드를 확정하지 않는다.
- 규칙과 값은 같은 source/template 및 선택 표 기준으로 제공한다. 별도 규칙 전달이 다른 표의 grid 값을 암묵적으로 동봉하는 근거가 되지는 않는다.
- BE는 domain 규칙 46에 따라 DB에 저장한 양식 version별 카탈로그 갱신 규칙을 읽어 제공한다. 물리 저장 구조는 기존 catalog ERD/DDL 설계가 소유하며 응답 JSON 형태와 DB 저장 형태가 같아야 한다고 가정하지 않는다.
- FE는 BE가 전달한 규칙을 적용하고 계산값의 직접 수정 가능 여부는 기존 `inputCells`/control `editable`을 따른다. 규칙 적용 대상이라는 이유만으로 직접 수정 권한이 생기지 않는다.
- 확정 사례: `LiqUtilAdjustment`의 마지막 월 열 뒤에 열을 추가하면 새 열의 `Period` 표시값은 직전 마지막 Period + 1이다. 일반 헤더 `B10:F10`과 월별 `Year/Month/Period`의 표 형태는 기존 카탈로그 설계 대상이다.
- `LiqUtilAdjustment`의 행·열 삭제 허용은 domain 규칙 47을 따른다. 삭제 시 FE에 전달할 갱신 규칙은 삭제 범위·위치와 Period/수식 처리의 write 정책을 확정한 뒤 구체화한다.
- `H7:CL7`은 같은 시트 내부값 참조 수식이다(2026-10-07 사용자 확인으로 범위 정정). `G7`은 시작 연도 값으로 수식 범위에서 제외하며 고정 헤더 정책은 유지한다. 조회에는 결과값을 제공하며, 참조 입력 변경 시 FE에 전달할 갱신 규칙의 구체 계산식은 실제 원본 수식 확인 후 확정한다. 모든 수식 셀에 적용 가능한 일반 Excel 계산기를 FE에 요구하는 결정은 아니다.
- BE write 검증·계산 허용 수식 처리·최종 파일 생성 책임은 domain 규칙 15–17/20–21/46을 따른다. FE의 화면 갱신 결과만으로 저장/최종 XLSM 계산 검증을 완료한 것으로 보지 않는다. 제출값과 파생값의 write 표현은 별도 write 계약에서 확정한다.

### 3.2 단일 표 값의 경계

- 표 본문만 보내 헤더를 잃지 않는다. 해당 표 표시를 위해 필요한 다중 헤더·기간 라벨은 기존 FE와 같이 `rows`에 원본 행 번호로 포함하며 별도 `headers` 배열을 만들지 않는다. 빈 입력 셀도 포함한다.
- 원본 행/열 위치를 유지해 표 좌표·편집 위치와 대응한다. 선택 표의 값은 수정 가능 값과 읽기 전용 값을 구분해 제공하며 수정 불가라는 이유만으로 표시에서 제외하지 않는다.
- `tableData.tableId`는 전체 구조의 `tables[].id`와 일치해야 한다. tableData는 같은 source/template 기준으로 읽은 값이어야 한다.
- 행의 셀 필드는 `columns[].field`와 대응하고 선택 표에 필요한 열만 담는다. 문자열 또는 숫자로 표시하며 형식화된 퍼센트·소수 자리·쉼표를 유지한다. 날짜·boolean은 표시 문자열로 제공하고 셀 객체/셀 값 null을 반환하지 않는다. 빈 셀 필드는 생략할 수 있으나 편집 위치와 필요한 빈 열은 보존한다. 원본 수식 노출은 Open Questions의 별도 결정으로 남긴다.
- 다른 표의 grid 값은 tableData에 암묵적으로 섞지 않는다. §3.1B의 부속 control은 `controlIds/sourceTableId`로 연결과 소유권을 명시한 예외이며, 임의 참조 표 데이터를 동봉하는 통로로 쓰지 않는다.
- 전체 구조 반환과 table 값 반환은 값을 재계산하거나 Draft/Case/Run을 생성하는 동작이 아니다. 저장값 표시와 변경 후 재계산은 구분한다.

공개 식별자는 source case와 서버가 발급한 snapshot 및 안정적인 table 식별자를 기준으로 정하며 저장소 식별자를 노출하지 않는다. snapshot의 발급/수명과 구체 request 표현은 Open Questions에 남긴다.

#### 저장 Case 숫자 표시 정책 — 2026-10-15 사용자 확정

이 절은 저장 Case 입력 조회의 숫자 표시 정책 정본이다. [조회 상세 §5.3](mi-fundamental-model-input-query-api-proposal.md#53-수식-저장값-판독-기준--사용자-확정)의 확정된 존재·타입 판독 이후 적용하며 그 판독/실패 계약을 바꾸지 않는다. 지원 범위는 승인된 양식 version별 표시 catalog에 필요한 숫자·서식으로 한정한다. 정책 채택은 실제 catalog 대응, 제품 구현 또는 HTTP/FE 검증 완료를 뜻하지 않는다.

- **회계 폭 독립 표시:** 관측한 회계 서식의 배치 공백만 제외한다. 숫자·부호·쉼표·소수 2자리와 0의 `-` 표시는 유지하고 정렬은 FE의 화면 배치로 처리한다. 적용 대상은 `_-* #,##0.00_-;-* #,##0.00_-;_-* "-"??_-;_-@_-` 서식 계열이며, 같은 코드의 custom numFmtId 및 `\-`/`-` 동등 표현을 포함한다. 내용 서식 `#,##0.00;-#,##0.00;"-";@`에 해당하는 표시를 제공하며 P22 등 셀 좌표를 판정 조건으로 쓰지 않는다. 일반 문자열·다른 서식에 trim이나 공백 제거를 확대하지 않는다.
- **General 기준:** US 표기를 고정한다. 소수점은 `.`, 지수는 대문자 `E`와 부호·최소 2자리 지수로 표현하며 요청/브라우저의 지역 설정으로 재서식하지 않는다. 열 폭·폰트에 따른 축약은 제외하고 같은 저장 숫자·서식에는 같은 표시 문자열을 제공한다. 저장 숫자를 변경하지 않고 표시용으로만 반올림한다.
- **General 소수/지수·정밀도:** 검증 후보의 Excel 호환 십진수 변환(`NumberToTextConverter.toText`와 동등한 변환)을 기준으로 0은 `0`으로 표시한다. 0이 아닌 수는 절댓값이 `100000000000` 미만이고, `0.0001` 이상이거나 불필요한 끝의 0을 제외한 소수 자릿수가 9 이하이면 소수 표기 후보로 처리한다. 정수부 자릿수를 최소 1로 세어 소수 자릿수를 `max(0, 10 - 정수부 자릿수)`로 반올림하고 끝의 불필요한 0을 제거한다. 11자리 정수는 유지하며, 반올림 결과의 절댓값이 위 상한에 도달하면 지수 표기로 전환한다. 나머지는 유효숫자 6자리 지수 표기와 불필요한 끝의 0 제거를 적용한다. 반올림은 HALF_UP(정확히 중간이면 절댓값이 커지는 방향)이다. 모든 숫자에 소수 9자리를 일괄 적용하지 않는다.
- **응답/FE 경계:** 형식화한 grid 값은 기존 `columnN: string | number` 중 string으로 보존하고 FE는 표시·복사 시 다시 숫자로 변환하거나 재서식/trim하지 않는다. `controls.value/options`의 저장값 타입·의미, 원본 좌표·편집 위치·grid/control 소유권은 §3.1~3.2를 유지한다. 문자열·날짜·다른 명시 서식은 기존 지원 동작을 유지하며 새 셀 객체/응답 필드·DB 구조를 추가하지 않는다.

[Q3 독립 검증 기록](../work-notes/2026/2026-10-15-model-input-query-q3-stored-value-verification.md)과 [표시 후보·지원 한계](../../temp/model-input-query-q3/2026-10-15-stored-values-g01xpf_b/display-refinement/README.md)는 이 정책의 근거이며 제품 실행 의존이 아니다. 모든 숫자·지역 설정·Excel 버전이나 원래 화면의 공백/픽셀 일치를 보장하지 않는다. 승인 표시 범위와 후보의 타입/서식/숫자 범위를 대조하고 부족한 검증은 실제 필요한 항목만 추가한다. 미확인 범위를 지원 완료로 간주하거나 표시 문자열을 임의로 잘라 맞추지 않는다.

<a id="incremental-input-upload-api-20261019"></a>

### 3.3 업로드 초안 생성·파일 추가 API — 파일명 역할 판별·순차 업로드

#### 구현용 잔여 계약 확정 — 성공·취소·접수 전 오류

- 업로드 접수/조회 data는 `{operationId,requestId,status,statusEndpoint,result,failure}`다. result/failure는 항상 key를 포함하며 QUEUED/RUNNING에서는 둘 다null, SUCCEEDED는 result만, FAILED는 failure만 object다. 저장된 종료 결과는 변경하지 않는다.
- 성공 result는 `{kind,draftId,revision,draftEndpoint,structure,selection}`이다. structure는 기존 EDT-02 전체 data(성공 역할 sections·readiness 포함), selection은 업로드 역할의 첫 표시 표에 대한 기존 EDT-03 전체 data다. 표시할 논리 표가 없으면 selection:null이다. structure/tableData를 별도 새로운 셀 표현으로 만들지 않는다. 성공 스냅샷은 원자 확정할 revision을 기준으로 생성해 저장하고 과거 결과를 현재 상태로 다시 만들지 않는다.
- 작성 취소는 `POST /api/v1/mi/fundamental-model/input-upload-drafts/{draftId}/discard`, JSON `{baseRevision,discardConfirmed:true}`. baseRevision은 공통 revision 범위의 필수 정수, 확인값은 필수 true다. owner 확인 후 진행 작업 없음→현재 revision 순으로 검사하고 DISCARDED/단일 작성 해제를 함께 확정한다. 성공200은 기존 EDT-02 data이며 canFinalize:false다. 이미 DISCARDED면 같은 소유자의 현재 폐기 상태를 반환하며 파일을 복원하지 않는다. FINALIZED에는409 DRAFT_NOT_EDITABLE이다.
- 접수 전 오류: 잘못된 metadata/확인값/파일명 형식400 REQ_INVALID_PARAMETER, JSON 문법400 REQ_INVALID_JSON, 소유/대상 없음404 RSC_NOT_FOUND(data:null), 파일당 용량 초과413 REQ_INVALID_PARAMETER(PAYLOAD_TOO_LARGE), 기존 상태/버전/같은 키 다른 내용409 BIZ_RULE_VIOLATION(DRAFT_BUSY/REVISION_CONFLICT/IDEMPOTENCY_CONFLICT/DRAFT_NOT_EDITABLE), 새 최초 작성이 이미 있으면409 UPLOAD_IN_PROGRESS다. Problem은 기존 `{reason,currentRevision,issues}`다. 접수 전 실패는 operation을 생성하지 않는다. MIME·실제 package/양식은 접수 후 검증하며 FAILED에 안전한 위치 정보를 제공한다.
- sourceKey는 서버 설정 `mi.model-input.upload.source-key`에 등록한 EXCEL_UPLOAD 출처 키만 받는다. 미설정은 POLICY_INCOMPLETE이며 임의 키/파일명을 허용하지 않는다. 최초 requestId는 회사/사용자 범위, 후속은 Draft 범위다. 같은 요청 판별은 정규화 metadata(후속 false/생략 확인값은false), 정규화된 원본 파일명·MIME, 실제 수신 크기·SHA-256이며 object UUID/경로는 제외한다. 수신 파일명을 저장 경로로 사용하지 않는다.
- 업로드 구조 검증은 고정 catalog의 실제 역할·필수 시트·표와 각 표 qry_json.uploadValidation의 headerCells(고정 라벨 좌표/기대 scalar)로 수행한다. 자동 기간 헤더/사용자 값은 고정 라벨 검사에서 제외한다. 정의 누락은 POLICY_INCOMPLETE로 안전하게 차단하며 파일명만 보고 허용하지 않는다. 저장 구조는 기존 qry_json 확장이고 발행 후 불변이다. 실제 운영 발행/seed는 수동 등록 절차를 따른다.

**공통 편집 연결 사용자 확인:** 직접 업로드는 기존 Case ID 없이, 검증 성공한 파일1개부터 초안을 생성할 수 있는 진입이다. 생성 이후에는 [기존 편집 API §3](mi-fundamental-model-input-draft-editing-api-proposal.md#shared-upload-draft-editing-20261019)의 EDT-02~05로 조회·값/행/열 변경·결과 확인을 진행한다. 같은 고정 양식 규칙·명령/metadata/성공·실패 계약을 사용하며 업로드 초안 전용 행·열 편집 API/정책을 만들지 않는다. 필요한 역할이 없는 동작만 제한하고 최종저장에는4역할을 요구한다. 공통 조회의 원본 Case 없는 nullable 필드와 성공 파일만의 sections 표현은 편집 명세 §3.2를 따른다. xlsm/xlsx 지원·역할별 원본 형식 유지·파일당50MiB(52428800바이트)는 아래 기존 확정을 유지한다.

저장 설계는 [domain §5.6](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#incremental-upload-lifecycle-storage-20261019)과 [DDL §6.10](../design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md#incremental-upload-storage-20261019)을 따른다. 저장 구조 문서화는 기존 EDT-01/worker의 부분 파일 지원 완료나 실제 DB 적용을 뜻하지 않는다.

2026-10-01의 저장 포맷 파싱·조회 공통 표현을 유지하며 **2026-10-19 사용자 요구로 파일명 역할 판별·순차 업로드·새 Case 편집 진입을 추가**한다. 업무 불변식은 [domain 규칙67](../design-docs/gas/mi-model-input-template-catalog-domain-model-design.md#incremental-input-upload-20261019)이 소유한다. 포맷은 §2의 백엔드 양식 카탈로그이며 시트·논리 입력 영역·헤더·셀 좌표·control binding·수정/행열 정책을 template version별로 정의한다. 파일마다 포맷을 새로 추론하지 않는다.

**2026-10-19 API 분리 사용자 확정:** 각 요청은 `multipart/form-data`로 workbook 하나를 받으며 최초 생성과 후속 추가·교체를 다음 두 API로 분리한다.

| Method / 전체 경로 | 역할 | 대상 초안 |
| --- | --- | --- |
| `POST /api/v1/mi/fundamental-model/input-upload-drafts` | 첫 정상 파일로 새 업로드 편집 초안 생성 | 요청에서 기존 초안을 지정하지 않는다. 단일 작성 조건을 만족하고 검증·파싱에 성공한 경우에만 서버가 새 draftId를 발급한다. |
| `POST /api/v1/mi/fundamental-model/input-upload-drafts/{draftId}/files` | 기존 초안에 파일 추가 또는 확인 후 동일 역할 파일 교체 | 경로의 draftId로 소유한 기존 초안을 지정한다. 없거나 접근할 수 없는 초안을 새로 생성하는 동작으로 대체하지 않는다. |

최초 업로드 처리 성공 결과의 서버 발급 draftId를 후속 요청 경로에 사용한다. 최초 접수 응답만으로 draftId 발급이나 초안 생성을 완료한 것으로 표시하지 않는다. 역할은 두 API 모두 서버가 파일명·구조로 판별한다. 파일 추가와 동일 역할 교체는 같은 후속 API를 사용하며 교체 확인·검증 성공/실패 규칙은 아래 확정 사항을 따른다. 과거 단일 `POST /input-uploads` 후보에서 초안 ID 생략/지정으로 생성과 추가를 구분하는 안은 채택하지 않았다. multipart의 두 part와 metadata 요청 항목은 아래 확정 계약을 따른다. 성공 결과와 접수 전 오류는 이 절의 기술 계약을 따른다. 접수/상태 조회의 HTTP는 아래 확정 계약을 따른다. 최초 생성의 재전송은 아래 확정 규칙을 따르며 후속 파일 추가·교체의 재전송·충돌 응답은 이 절의 기술 계약을 따르며 초안별 변경1건은 아래 확정 규칙을 따른다. 과거의 필수 `kind` 입력과 "이 호출은 Draft/Case를 생성하지 않는다"는 제한도 이번 확정 계약으로 사용하지 않는다. 파일 업로드 자체로 Run을 접수하거나 수식을 재계산하지 않는다. 현재 기존4파일 Case 업로드·EDT-01이 이 순차 진입을 구현했다는 뜻이 아니다.

**2026-10-19 업로드 비동기 사용자 확정:** domain 규칙67에 따라 두 업로드 API는 파일 수신 후 접수 결과를 먼저 반환하고 검증·파싱을 비동기로 수행한다. 접수 가능 여부에 필요한 인증·요청/파일 기본 조건은 접수 전에 확인할 수 있으며, 접수 수락은 양식 검증 성공이나 파일 연결 완료를 뜻하지 않는다. 파일 수신 자체는 완료돼야 하므로 파일 전송 시간까지 없애는 계약이 아니다.

FE는 서버가 제공하는 접수 식별자와 상태 조회 수단으로 대기/처리 중인지, 성공/실패했는지 확인한다. 상태 조회는 처리를 새로 시작하거나 파일을 연결하지 않는다. 최초 파일의 성공 결과에서만 생성된 draftId와 해당 파일의 구조/선택 영역 데이터를 이용할 수 있고, 후속 성공 결과에서만 대상 초안의 파일 추가·교체를 반영한다. 위 API 표의 draftId와 아래 구조/표 응답은 처리 성공 결과의 표현이며 최초 접수 응답에 모두 반환한다는 뜻이 아니다. 접수 식별자/상태 필드·조회 경로·HTTP·폴링은 아래 확정 계약을 따르며 상세 성공/실패 결과의 잔여 schema는 Open Questions에서 구체화한다.

처리 대기/진행 중에는 미검증 파일을 편집 가능한 파일이나 네 역할 준비에 포함하지 않는다. 첫 파일 실패 시 편집 초안/Case를 만들지 않으며, 후속 실패 시 기존 파일·편집을 보존한다. 같은 역할 교체도 검증·파싱 성공 뒤 파일 교체와 해당 역할 편집 초기화를 함께 확정한다. 최초 생성의 동일 requestId·동일 파일/요청 재전송은 아래 규칙에 따라 같은 접수/처리 결과에 연결되며 별도 작업이나 초안을 중복 생성하지 않는다. 처리 중에는 아직 생성되지 않은 초안의 성공 결과를 반환하지 않는다. 업로드 접수 자체로 Run을 시작하거나 수식을 재계산하지 않는다.

**2026-10-19 업로드 접수/상태 조회 사용자 확정:** 최초 초안 생성과 후속 파일 추가·교체 모두 새 비동기 작업 접수 시 **HTTP202**와 공통 `BaseResponse.data`의 아래 필드를 반환한다. 접수/조회 자체의 성공과 workbook 처리 성공을 구분한다.

| 필드 | 타입 / 의미 |
| --- | --- |
| `operationId` | required UUID string. 서버가 발급한 업로드 작업 식별자. 아직 초안이 없는 첫 업로드도 이 값으로 구분한다. |
| `status` | required enum: QUEUED / RUNNING / SUCCEEDED / FAILED. 업로드 처리 상태이며 Case/Run 상태로 대신하지 않는다. |
| `statusEndpoint` | required relative API path. `/api/v1/mi/fundamental-model/input-upload-requests/{operationId}`이며 FE는 반환된 경로로 조회한다. |

상태 조회는 **`GET /api/v1/mi/fundamental-model/input-upload-requests/{operationId}`**, **HTTP200**이다. 인증/소유권을 확인해 해당 작업의 상태/결과를 읽으며 조회가 처리를 새로 시작하거나 재실행하지 않는다. 최초 업로드의 조회 경로에 아직 없는 draftId를 요구하지 않으며 최초/후속 업로드에 같은 경로를 쓴다. 조회한 작업이 FAILED여도 조회 자체가 성공하면 HTTP200이고 `data.status:FAILED`로 처리 실패를 구분한다. 실패 사유/문제 위치는 아래 failure/issue 확정 계약을 따른다.

FE는 반환된 statusEndpoint를 **2초 간격**으로 조회한다. QUEUED/RUNNING이면 대기/처리 상태를 표시하고 계속 조회하며 SUCCEEDED/FAILED이면 이 작업의 반복 조회를 종료한다. SUCCEEDED이면 확정된 초안/파일·revision·첫 표 결과를 화면에 반영하고 FAILED이면 기존 성공 상태를 유지한다. 화면 이탈 시 반복 조회를 멈추며 네트워크 끊김이나 FE 대기시간 경과만으로 서버 작업을 FAILED로 판단하지 않는다. 기존 허용된 동일 요청 재전송은 진행 중 HTTP202, 종료 후 HTTP200으로 같은 operationId/결과를 확인하고 새 작업을 만들지 않는다. 처음의 HTTP202만으로 초안 생성/파일 연결 성공을 표시하지 않는다.

다음은 신규 접수202 응답에서 채택된 `BaseResponse.data` 필드 부분 예시다. 성공/실패 상세 payload 전체 예시를 대신하지 않으며 비실패 상태의 failure는 아래 계약대로 null이다.

```json
{
  "operationId": "c0000000-0000-4000-8000-000000000201",
  "status": "QUEUED",
  "statusEndpoint": "/api/v1/mi/fundamental-model/input-upload-requests/c0000000-0000-4000-8000-000000000201",
  "failure": null
}
```

**2026-10-19 업로드 처리 상한 사용자 초기 채택 — 120초:** domain 규칙67에 따라 최초 업로드와 후속 파일 추가·교체 모두 **파일 수신 후 서버의 작업 접수 시점부터120초**를 초기 상한으로 사용한다. 대기열 시간, 고정 양식 검증·파싱과 초안 생성/파일 연결·교체/성공 revision 확정까지 포함한다. 파일을 서버에 보내는 전송 시간은 포함하지 않으며 FE의 HTTP 대기시간이나2초 조회 간격을 처리 기한으로 대신하지 않는다.

기한까지 성공 확정되지 않은 작업은 기한 만료 대상으로 판단한다. 대기 중이면 새 실행을 시작하지 않고, 실행 중이면 실제 실행 종료와 저장 확정 여부를 확인해 **status:FAILED, failure.reason:TIMEOUT**으로 확정한다. 기한 뒤에 도착한 결과로 초안 생성/파일 연결·교체/편집 초기화·revision 증가를 성공 반영하지 않는다. 반대로 기한 내 이미 성공 확정된 작업은 응답/조회가 늦었다는 이유로 TIMEOUT으로 바꾸지 않는다. 종료/저장 여부를 확인할 수 없는데 FE 대기시간 경과만으로 실패나 재실행을 단정하지 않으며 구체 중단/복구 구현은 후속 검증 대상이다.

TIMEOUT의 failure는 기존 확정 Problem을 사용하며 입력 위치의 문제가 아니므로 issues는 []다. 최초 업로드는 currentRevision:null과 초안/Case 미생성, 후속 업로드는 판정 시점의 성공 revision과 기존 파일·편집 보존을 적용한다. 상태 GET은 HTTP200으로 실패 결과를 제공하고 FE는 FAILED에서 반복 조회를 멈춘다. 같은 requestId·동일 내용 재전송은 이 실패 결과를 반환하며 실제 재시도는 새 requestId/후속 현재 baseRevision을 사용한다. 취소 확정된 A 뒤에 B 최초 업로드가 TIMEOUT이어도 A를 복구하지 않는다.

이 수치는 **사용자가 우선 채택한 초기 기준**이며 업로드 파일의 실측 성능으로 검증된 값이 아니다. 기준 파일·50MiB에 가까운 대표 파일의 처리 시간/자원, 대기열 만료·실행 중 종료·늦은 성공 차단을 실제로 확인해야 한다. 규칙64의 편집 worker CPU/메모리/전체 동시 처리 수나30초 목표까지 새 업로드에 자동 적용하는 결정은 아니다.

**2026-10-19 업로드 오류 상세 사용자 확정:** 작업 응답은 `failure`를 필수로 포함하며 FAILED이면 object, QUEUED/RUNNING/SUCCEEDED이면 `null`이다. 바깥 구조는 기존 편집 API §6의 `Problem = {reason, currentRevision, issues}`를 재사용한다. `reason`은 FE가 분기할 사유 코드이고, 양식 구조 검증 실패의 대표 사유는 `STRUCTURE_VALIDATION_FAILED`다. 이는 failure 안의 업무 사유이며 새 전역 ResultCode를 뜻하지 않는다. `currentRevision`은 실패를 판정한 대상 초안의 성공 번호이며 최초 업로드처럼 아직 초안이 없으면 `null`이다. 과거 실패 기록의 번호를 현재 초안 번호로 덮어쓰지 않는다.

`issues`는 항상 배열이며 확인 가능한 검증 문제를 아래 업로드 전용 위치 형식으로 제공한다. 구조 검증에서 위치를 확인했는데 대표 사유만 반환하거나 정상 빈 목록으로 대신하지 않는다. 파일 판독 실패처럼 세부 문제 위치를 확인할 수 없거나 위치가 해당하지 않는 실패는 안전한 대표 사유와 `issues:[]`를 허용한다. 모든 오류를 찾으려고 위험하거나 무제한인 파일 탐색을 계속할 의무는 없으며 확인하지 못한 위치를 추측해서 채우지 않는다.

| issue 필드 | JSON 타입 / 의미 |
| --- | --- |
| `scope` | required enum FILE / SHEET / TABLE / HEADER. 문제가 속한 수준 |
| `kind` | DEMAND / SUPPLY / TRADE / OUTPUT 또는 null. 서버가 판별한 파일 역할, 판별 불가 시 null |
| `sheetName` | string 또는 null. 관련 고정 catalog 시트명, 확인 불가/해당 없음이면 null |
| `tableId` | string 또는 null. 관련 공개 논리 표 ID, 확인 불가/해당 없음이면 null |
| `cellRef` | string 또는 null. HEADER 문제 등 위치가 정해진 경우 원본 A1 셀 좌표. 확인 불가/해당 없음이면 null |
| `reason` | required string. 해당 위치의 검증 사유 코드 |

각 issue는 위 필드를 포함하고 해당하지 않는 위치는 null로 준다. 편집 명령의 commandId/rowRef/fieldKey가 없는 업로드에 가짜 명령/행 식별자를 만들지 않는다. 헤더는 문제 셀 좌표로 식별하며 원본/거부값·수식·내부 경로·provider 예외·다른 사용자 정보는 반환하지 않는다. 시트/표 이름은 공개 catalog의 식별 기준을 사용하고 미확인 원본 문자열을 오류 응답에 그대로 복사하지 않는다.

| 검증 문제 | scope | issue.reason | 위치 표현 |
| --- | --- | --- | --- |
| 파일명에서 역할을 판별하지 못함 | FILE | ROLE_NOT_DETECTED | kind와 하위 위치는 null |
| 파일명이 복수 역할에 해당 | FILE | ROLE_AMBIGUOUS | 단일 kind를 임의 선택하지 않음, 위치는 null |
| 필수 catalog 시트가 없음 | SHEET | SHEET_MISSING | kind와 기대 catalog sheetName |
| 해당 표의 필수 구조가 불일치 | TABLE | TABLE_STRUCTURE_MISMATCH | 확인한 kind/sheetName/tableId |
| 필수 헤더가 없거나 일치하지 않음 | HEADER | HEADER_MISMATCH | 확인한 kind/sheetName/tableId/cellRef |

아래는 실패 상태 조회 HTTP200의 `BaseResponse.data` 부분 예시다. 시트명·표 ID·좌표는 형식을 설명하는 합성값이며 실제 catalog/업무 규칙 등록 근거가 아니다. 첫 업로드 실패라 초안이 없으므로 currentRevision은 null이다. 후속 실패는 기존 초안의 성공 번호와 파일·편집을 보존한다.

```json
{
  "operationId": "c0000000-0000-4000-8000-000000000201",
  "status": "FAILED",
  "statusEndpoint": "/api/v1/mi/fundamental-model/input-upload-requests/c0000000-0000-4000-8000-000000000201",
  "failure": {
    "reason": "STRUCTURE_VALIDATION_FAILED",
    "currentRevision": null,
    "issues": [
      {
        "scope": "HEADER",
        "kind": "SUPPLY",
        "sheetName": "ExampleSheet",
        "tableId": "SUPPLY/ExampleSheet/example_table",
        "cellRef": "B7",
        "reason": "HEADER_MISMATCH"
      }
    ]
  }
}
```

파일 기본 조건·metadata 형식·권한 등 접수 전에 거절한 요청을 가짜 FAILED 작업으로 만들지 않는다. 접수 전 오류의 HTTP/ResultCode는 이 절의 기술 계약을 따른다. 접수 뒤 실패를 조회할 때는 앞서 확정한 HTTP200/data.status:FAILED를 유지하고 FE가 message 문자열을 분석해 문제 위치를 추론하게 하지 않는다.

**2026-10-19 초안 파일 준비 상태 사용자 확정:** 업로드 초안의 현재 상태를 반환하는 응답은 `BaseResponse.data`에 다음 세 필드를 필수로 제공한다. 초안 조회 및 업로드 성공 결과의 초안 상태에 같은 의미를 사용하며, 실제 저장된 검증·연결 상태를 기준으로 BE가 계산한다. FE의 파일 선택/전송 완료나 파일 개수로 대신 계산하지 않는다.

| 필드 | JSON 타입 / 의미 |
| --- | --- |
| `uploadedKinds` | non-null array of kind enum. 해당 초안에 검증·연결이 성공한 유효 파일이 있는 역할. 중복 없음 |
| `missingKinds` | non-null array of kind enum. 필수 네 역할 중 uploadedKinds에 없는 역할. 중복 없음 |
| `canFinalize` | required boolean. 네 역할 준비와 초안의 편집 가능 상태·진행 작업 없음 등 기존 최종저장 접수 조건을 서버가 확인한 현재 가능 여부 |

두 역할 배열은 DEMAND/SUPPLY/TRADE/OUTPUT만 포함하며 서로 겹치지 않고 합치면 필수 네 역할이다. 순서는 기존 화면 구조의 역할 표시 순서를 재사용한다. 대기/처리 중인 새 파일은 uploadedKinds에 넣지 않으며, 아직 해당 역할의 성공 파일이 없다면 그 역할은 missingKinds에 남는다. 같은 역할 교체 중에는 새 파일을 완료로 세지 않되 직전 성공 파일이 그대로 유효하므로 기존 역할을 uploadedKinds에서 제거하지 않는다. 교체 실패 시 기존 파일/편집과 역할 준비 상태를 유지한다.

canFinalize는 missingKinds가 비어 있지 않거나 같은 초안의 변경 작업이 대기/처리 중이면 false다. 네 역할이 준비돼도 초안 상태·권한/기준·기존 최종저장 조건이 허용하지 않으면 true로 만들지 않는다. 이는 현재 접수 가능 여부이며 최종 파일 생성/검증 성공을 미리 보장하는 값이 아니다. FE는 이 값으로 버튼 상태를 표시하고 BE는 실제 최종저장 요청 시 조건을 다시 검증한다.

세 필드는 같은 초안 상태를 기준으로 함께 계산하며 파일 목록은 한 시점, canFinalize는 다른 작업 시점의 값으로 섞지 않는다. 과거 업로드 작업의 성공 결과에 포함한 준비 상태는 그 결과의 시점이며 뒤에 변경된 현재 상태로 덮어쓰지 않는다. FE는 과거 작업 결과만으로 현재 초안 상태를 되돌리지 않고 초안의 현재 상태를 재조회한다. 최초 파일이 성공하기 전에는 아직 초안이 없으므로 접수/진행 응답에 이 세 필드로 가짜 초안 상태를 만들지 않는다.

다음은 초안 현재 상태 응답의 채택된 세 필드 부분 예시다. SUPPLY 파일이 아직 검증 중이고 그 역할의 기존 성공 파일은 없는 상태다. 전체 응답 DTO/실제 fixture 예시를 대신하지 않는다.

```json
{
  "uploadedKinds": ["DEMAND", "TRADE", "OUTPUT"],
  "missingKinds": ["SUPPLY"],
  "canFinalize": false
}
```

네 역할이 모두 이미 연결된 상태에서 파일 교체가 진행 중이면 uploadedKinds는 네 역할, missingKinds는 []이고 canFinalize는 false다. 신규 역할 연결 성공은 기존 revision 성공 확정과 함께 반영하고, 파일 연결 실패를 완료 목록에 남기지 않는다. 초안 현재 상태 조회 경로/전체 응답 연결과 잔여 필드는 Open Questions에서 구체화한다.

**2026-10-19 업로드 FAILED 후 재시도 사용자 확정:** domain 규칙67에 따라 FAILED로 확정된 최초/후속 업로드에 같은 requestId·동일 파일/metadata를 재전송하면 **HTTP200과 기존 operationId/status:FAILED/failure를 그대로 반환**한다. 저장된 실패 작업을 다시 QUEUED/RUNNING으로 바꾸거나 재검증·파일 연결·revision 증가를 실행하지 않는다. 후속 초안이 나중에 변경됐어도 인증/소유권과 동일 요청 확인 후 원래 실패 결과를 반환하며, 오래된 baseRevision만을 이유로 기존 결과 조회를 새 충돌로 바꾸지 않는다. 기존 failure.currentRevision은 원래 실패 판정 시점의 값이다.

사용자가 재시도할 때는 **새 requestId**로 새 작업을 요청한다. 직접 업로드 단일 작성 제한은 그대로 유지하며 후속 실패 재시도로 별도 Case의 초안을 만들지 않는다. 후속 파일 추가·교체는 초안 현재 상태를 다시 조회하고 **현재 성공 revision을 baseRevision**으로 보내며, 교체인 경우 필요한 안내/확인과 replaceConfirmed:true를 적용한다. 그 사이 다른 변경이 진행 중이거나 버전이 다시 바뀌면 기존 진행 제한/버전 충돌 정책대로 거절한다. 자동으로 최신 버전만 덮어쓴 재전송을 하지 않는다. 파일을 수정했거나 metadata의 baseRevision/교체 확인 같은 의미 있는 내용이 달라졌으면 이전 requestId를 재사용하지 않는다. 같은 requestId의 다른 내용은 기존 동일성 충돌 정책을 유지한다.

첫 업로드 실패로 초안이 아직 없으면 새 requestId로 최초 생성 API를 다시 요청하며 baseRevision은 보내지 않는다. 새 작업의 검증·파싱 성공 뒤에만 새 초안을 생성하고 이전 실패 작업의 고정/현재 상태를 정상 초안으로 승격하지 않는다. 같은 실패 결과 조회/새 작업 접수는 기존 성공 파일·편집·revision을 변경하지 않고 새 작업이 성공할 때만 해당 변경을 확정한다. 이 규칙은 **서버가 FAILED로 확정한 경우**이며 통신 오류/화면 대기시간 경과를 실패로 간주해 새 요청을 자동 발행하는 의미가 아니다. 진행 중/접수 여부 불명확 상황은 기존 operationId 조회 또는 같은 requestId·동일 내용 재전송으로 먼저 확인한다. worker 중단/재시작의 종료·복구 판정 수단은 Open Questions의 별도 기술 계약이다.

**2026-10-19 초안별 변경1건 사용자 확정:** domain 규칙67에 따라 같은 업로드 편집 초안에서는 파일 추가·동일 역할 교체, 편집 저장, 최종저장 중 하나의 변경 작업만 진행할 수 있다. 접수된 작업이 대기/처리 중이면 다른 역할의 요청을 포함해 같은 초안의 새 변경을 거절하며 자동 대기열에 추가하지 않는다. FE는 진행 중 상태를 안내하고 새 변경 접수를 제한하며 BE도 같은 경계를 검증한다. 업로드만의 별도 제한으로 기존 편집 저장/최종저장과 서로 겹치게 하지 않는다.

처리 중에도 인증/소유권을 확인한 직전 성공 상태의 구조·표 조회와 작업 결과 확인은 허용한다. 대기/처리 중 파일이나 편집을 현재 성공 상태에 섞지 않는다. 기존에 허용된 동일 requestId·동일 내용의 재전송은 새 변경 요청으로 거절하지 않고 기존 접수/결과에 연결한다. 다른 내용의 동일 requestId는 기존 충돌 규칙대로 거절한다. 작업이 성공 또는 실패로 확정돼 진행 제한이 해제된 뒤 새 변경을 요청할 수 있으며, 실패해도 직전 성공 상태는 유지한다. 다른 초안은 독립적으로 접수·조회할 수 있다. 이 정책은 초안별 병행 제한이며 전체 worker의 동시 처리 수를 새로 승인하는 의미가 아니다. 충돌 HTTP/ResultCode와 후속 요청의 requestId는 이 절의 기술 계약, 저장/복구는 DDL §6.10을 따른다. baseRevision 검증과 성공 revision 증가는 아래 확정 규칙을 따른다.

**2026-10-19 후속 파일 revision 사용자 확정:** `POST /api/v1/mi/fundamental-model/input-upload-drafts/{draftId}/files`의 파일 추가·교체 요청은 화면이 마지막으로 확인한 초안의 성공 revision을 `baseRevision`으로 필수 제출한다. 인증·소유권과 기존 동일 요청 재전송 확인 후 새 작업을 접수할 때 서버의 현재 성공 revision과 비교하며 다르면 거절한다. 충돌 시 FE는 자동으로 baseRevision만 최신 값으로 바꿔 재전송하거나 파일을 덮어쓰지 않고 현재 초안을 재조회해 사용자가 최신 파일/편집을 확인하게 한다. 같은 초안의 변경1건 제한도 함께 적용한다.

파일 추가 또는 교체가 검증·파싱을 포함한 모든 처리에 성공하면 파일 연결·교체/해당 역할 편집 초기화와 같은 성공 경계에서 `revision = baseRevision + 1`을 작업당 한 번만 확정한다. 파일 업로드/접수/대기/처리 중에는 성공 revision을 증가시키지 않는다. 검증·처리 실패 또는 버전 충돌이면 직전 성공 revision과 기존 파일·편집을 유지한다. 성공 확정 시에도 접수 기준/진행 작업이 여전히 유효해야 하며 기준이 달라진 결과를 최신 버전에 덮어쓰지 않는다. 기존 동일 성공 작업의 결과 조회나 허용된 재전송은 같은 성공 revision을 반환하며 다시 증가시키지 않는다. 파일 추가·교체는 편집 저장과 같은 초안의 성공 revision을 사용하며 별도 파일 전용 번호로 대체하지 않는다. 최초 초안 생성은 이 후속 요청의 baseRevision 대상이 아니다. 전달 위치/타입은 아래 metadata 계약을 따른다. 오류 HTTP/ResultCode와 동일성은 이 절의 기술 계약, 저장은 DDL §6.10을 따른다.

**2026-10-19 최초 업로드 재전송 사용자 확정:** 첫 파일의 초안 생성 API는 FE가 생성한 `requestId`를 필수로 받는다. 응답 유실 등으로 같은 requestId와 동일 파일·요청 내용을 재전송하면 기존에 생성한 초안과 해당 최초 업로드 결과를 반환하며 새 초안을 만들지 않는다. 같은 requestId로 파일이나 요청 내용이 달라지면 충돌로 거절하고 기존 초안·파일·편집을 보존한다. 파일명이 같다는 이유만으로 파일 내용까지 같다고 판단하지 않으며 실제 파일 바이트와 역할 판별에 쓰는 파일명 등 의미 있는 요청 내용을 비교해야 한다. 전송용 multipart 경계 문자열 같은 포장 차이를 업무 내용 차이로 취급하지 않는다.

재전송에도 기존 인증·소유권 확인을 적용하며 다른 사용자/회사의 결과를 반환하지 않는다. 같은 요청의 동시 재전송으로도 초안을 중복 생성하지 않아야 한다. 이전 최초 업로드 결과를 반환하는 것은 현재 초안의 후속 파일이나 편집을 초기 상태로 되돌리는 의미가 아니다. 동일 요청 재전송 때문에 현재 ACTIVE로 다시 검증하거나 초안의 고정 버전을 바꾸지 않는다. 새 Case 작성이 허용되는 상태에서 새 requestId로 명시적으로 생성 요청한다. 새 ID만으로 단일 작성 제한을 우회하지 않는다. UUID/전달 위치·파일 동일성·오류는 이 절의 기술 계약, 키 저장/보관은 DDL §6.10을 따르며, 기존 sourceCase 편집의 requestId 정책을 변경하지 않는다.

**2026-10-19 생성 시점 사용자 확정:** 첫 파일이 검증·파싱에 성공하면 서버 발급 식별자로 업로드 편집 초안을 반환하고 그 파일의 조회·편집을 지원한다. 후속 파일은 그 초안에 누적하며 업로드 응답에서 새 Case 생성 완료를 보고하지 않는다. 네 역할이 준비되면 최종저장을 요청할 수 있고, 실제 Case는 domain 규칙67의 최종저장 성공 경계에서 생성한다. 최초 파일 검증 실패 시 초안을 만들지 않으며 후속 실패 시 기존 초안 상태를 보존한다. 구체 초안 식별자 응답 필드/타입·잔여 오류 HTTP/편집 연결은 Open Questions이며 기존 EDT-01을 부분 업로드 생성 API로 사용하지 않는다.

**2026-10-19 양식 버전 사용자 확정:** 적용 버전은 domain 규칙67대로 첫 정상 파일의 검증 기준을 초안 전체에 고정한다. 후속 업로드에서 현재 ACTIVE를 다시 선택하지 않으며, 조회·편집·최종저장도 같은 기준을 사용한다. 새 ACTIVE는 새 초안에 적용한다. FE가 파일명·kind만으로 적용 버전을 바꾸거나 서버가 다른 버전으로 자동 fallback하지 않는다. 버전 식별자의 구체 요청/응답 필드와 저장 표현은 Open Questions다.

**2026-10-19 동일 역할 교체 사용자 확정:** 같은 역할의 파일이 이미 있으면 FE는 해당 역할의 기존 편집이 초기화됨을 안내하고 사용자 확인을 받는다. 서버는 새 파일을 초안의 고정 양식 버전으로 검증·파싱한 뒤 성공한 경우에만 파일 교체와 해당 역할 편집 초기화를 함께 확정한다(domain 규칙67). 취소 또는 검증/판독 실패 시 기존 파일·편집 상태를 보존하며 다른 역할의 원본/사용자 편집과 고정 양식 버전은 유지한다. 교체 자체로 Case를 생성하지 않는다. 교체 확인은 아래 replaceConfirmed 채택을 따르며 재전송/구체 오류 표현의 잔여 사항은 Open Questions다.

**2026-10-19 교체 영향 범위 사용자 정정:** 파일 교체가 다른 파일의 표에 영향을 주는 경우는 없다(domain 규칙67). 따라서 교체는 해당 역할 파일과 해당 파일의 편집/구조 변경 초기화에만 적용하고 다른 역할의 파일·표·사용자 입력·계산 결과는 그대로 유지한다. 앞서 제안한 파일 간 연결 계산이나 파일 간 영향 때문에 교체를 거절/새 초안을 요구하는 안은 요구사항이 아니며 미결 항목에서도 제거한다. 업로드의 수식 재계산 제외 원칙에 파일 간 교체 예외를 추가하지 않는다. 교체 대상 파일의 고정 양식 검증·원본 보존·해당 파일 편집 초기화와 작업당 revision1회 증가는 기존 확정 계약을 유지한다.

**2026-10-19 교체 확인 필드 사용자 확정:** 후속 파일 추가·교체 API에서 서버가 판별한 역할의 기존 파일이 있으면 FE는 해당 역할의 편집 초기화 안내에 사용자가 동의한 뒤 `replaceConfirmed: true`를 보낸다. 서버는 기존 역할 파일이 있는데 이 값이 생략되거나 `false`이면 교체 요청을 거절한다. 확인 없는 요청을 자동 교체나 별도 초안 생성으로 바꾸지 않으며 기존 파일·편집·revision을 그대로 유지한다. 기존 역할 파일이 없는 추가에는 교체 확인을 요구하지 않는다.

`replaceConfirmed`는 논리 boolean 확인값이며 임의 문자열/숫자를 참으로 해석해 확인을 우회하지 않는다. metadata JSON의 boolean으로 전달하며 구체 part 형식은 아래 확정 계약을 따른다. FE는 확인 취소 시 교체를 보내지 않고, BE도 요청의 확인값을 검증한다. `true`는 교체 의사만 나타내며 권한·baseRevision·초안별 진행 제한·새 파일의 고정 양식 검증을 생략하는 허가가 아니다. stale baseRevision은 `true`여도 거절하고, 확인된 요청도 검증·파싱 성공 뒤에만 파일 교체/해당 편집 초기화와 revision1회 증가를 함께 확정한다. 확인 없는 새 교체는 변경 작업으로 접수하지 않는다. 기존 동일 성공 요청의 허용된 재전송은 기존 결과 확인이며 새 교체/추가 초기화를 수행하지 않는다. 오류 HTTP/ResultCode·재전송 동일성 세부의 잔여 사항은 Open Questions를 따른다.

**2026-10-19 부분 편집 사용자 확정:** domain 규칙67에 따라 필요한 역할 파일이 없는 동작만 제한하고 현재 파일로 처리 가능한 기존 승인 편집은 계속 허용한다. FE는 해당 동작의 제한 사유와 필요한 역할을 안내하며 BE도 요청 처리 시 같은 조건을 검증한다. 제한 동작을 성공/계산 보류로 저장하지 않고 기존 요청 전체 성공/실패 원칙과 직전 성공 상태를 유지한다. 필요한 파일이 추가되면 고정 양식의 정책/준비도를 다시 평가한다. 동작별 필요 역할·제한 사유의 응답 필드/오류 표현은 Open Questions이며 기존 capability/오류 형식을 우선 검토한다. 네 역할 완성 전 최종저장은 계속 금지한다.

**2026-10-19 직접 업로드 초안 개수 사용자 확정 — 한 번에 Case 하나:** domain 규칙67에 따라 같은 사용자의 파일 직접 업로드 작성은 한 번에 하나만 허용한다. 범위는 인증 문맥의 회사/사용자가 소유한 직접 업로드 작업이며, 다른 사용자나 기존 저장 Case 등 다른 출처의 Draft에 이 제한을 확대하지 않는다. 첫 정상 파일에서 편집 초안 생성·네 역할 최종저장 성공 시 Case 생성이라는 경계는 유지한다.

최초 파일을 수신해 비동기 검증이 진행되는 동안에도 같은 사용자의 별도 최초 요청이 두 번째 직접 업로드 작성을 만들지 못하게 한다. 동일 requestId/동일 내용의 허용된 재전송은 원래 접수/결과를 확인하며 새 작성을 추가하지 않는다. 새 requestId라는 이유만으로 미완료 작업을 둔 채 별도 초안을 만들거나 기존 초안을 자동 삭제·병합·교체·재개하지 않는다. 이미 생성된 작업에는 기존 draftId로 파일 추가·교체/편집·최종저장을 이어간다. 최초 파일 실패로 아직 초안이 없으면 기존 확정대로 새 requestId의 최초 재시도를 허용하며, 기존 초안의 후속 파일 실패는 현재 초안과 파일/편집을 유지한다.

최종저장이 성공해 현재 Case 작성이 끝나면 다음 Case를 새로 작성할 수 있다. 미완료 작성은 아래 사용자 확인 규칙으로 취소한 뒤 새 Case로 전환할 수 있다. 확인 없이 기존 파일/편집을 버리거나 새 Case 요청으로 덮어쓰지 않는다. 취소와 새 업로드의 분리·실패 시 A 취소 유지는 아래 확정 계약을 따른다. 구체 취소 endpoint/요청·충돌 HTTP/ResultCode·저장 표현은 후속 기술 계약이다. 초안 목록/목록에서 선택 재개는 이번 범위에서 제외한다. 기존의 허용된 초안 간 상태 격리·명시적 대상 처리와 다른 출처 Draft 계약은 유지한다.

**2026-10-19 미완료 작성 취소/새 시작 사용자 확정:** 직접 업로드 Case의 새 작성을 시작하기 위해 현재 작성을 취소할 수 있다(domain 규칙67). FE는 현재 파일·편집 내용을 더 이상 이어가지 않는다는 사실을 안내하고 사용자 확인을 받는다. **확인 전 또는 확인 취소 시 기존 내용을 유지**하며 새 활성 작성으로 자동 전환하지 않는다.

취소는 **현재 초안의 업로드/교체·편집 저장·최종저장 작업이 대기/처리 중이 아닐 때만** 확정할 수 있다. FE는 진행 상태를 표시하고 BE도 인증/소유권과 현재 대상·진행 상태를 다시 검증한다. 안내를 보는 동안 다른 탭에서 작업을 접수했다면 취소를 거절하고 기존 작업을 유지한다. 화면의 확인만으로 서버 작업을 강제 중단하거나 파일/편집을 먼저 지우지 않는다.

확인된 취소가 서버에 확정되면 현재 작성은 취소 상태(기존 Draft의 DISCARDED 의미)로 전환하고 새 직접 업로드 작성을 허용한다. 취소된 draftId로 새 파일 추가·교체/편집 저장·최종저장을 진행하지 못하며 canFinalize는 false다. 이전 operationId의 상태/결과 확인이나 허용된 재전송은 과거 기록 확인이며 취소된 작성을 다시 활성화하지 않는다. 취소 처리 자체가 실패하면 기존 파일·편집·고정 양식·성공 revision과 현재 작성 제한을 유지한다. 작성 취소는 실제 Case를 생성하거나 최종저장에 성공한 것으로 처리하지 않는다.

새 작성은 새 최초 업로드 요청/파일 검증과 첫 정상 파일의 당시 ACTIVE 양식 고정 규칙을 따른다. 취소용 요청 필드·endpoint/HTTP·멱등/버전 대조·저장 표현은 후속 기술 계약으로 구체화하며 원본 파일/이력의 즉시 물리 삭제를 새 요구로 추가하지 않는다. 기존 저장 Case 등 다른 출처의 취소/보관 계약은 변경하지 않는다.

**2026-10-19 취소 확정 후 새 첫 파일 실패 사용자 확정:** 안내/확인과 진행 작업 없음 검증을 통과한 A의 취소를 먼저 서버에 확정하고, 그 성공을 확인한 뒤 B의 파일을 기존 최초 생성 API `POST /api/v1/mi/fundamental-model/input-upload-drafts`로 업로드한다. 취소와 B의 파일 검증은 별도 요청/확정으로 처리하며 B의 검증이 성공해야 A를 취소하는 방식으로 결합하지 않는다. A의 취소가 실패했거나 확정 여부를 모르면 B의 새 작성을 먼저 시작하지 않고 A 상태를 확인한다.

B의 첫 파일 검증·파싱이 실패하면 B의 operation은 FAILED로 끝나고 **B의 편집 초안/Case는 생성하지 않는다**. A는 이미 확정된 취소 상태로 유지하며 파일·편집을 복구하거나 다시 활성화하지 않는다. 사용자에게 B 실패 사유/issues를 안내하고 파일을 다시 선택/수정해 업로드할 수 있게 한다. 동일 requestId·동일 파일/metadata 재전송은 기존 B 실패 결과만 반환하며 실제 재시도는 **새 requestId**로 최초 생성 API를 호출한다. 아직 B 초안이 없으므로 **baseRevision은 보내지 않는다**. 재시도 검증·파싱이 성공한 파일에서만 새 B 초안을 생성하고 그때 적용한 ACTIVE 버전을 고정한다. 기존에 최종저장된 Case나 다른 사용자/출처의 자료는 이 흐름의 A가 아니다.


**2026-10-19 지원 파일 형식 사용자 확정:** 업로드는.xlsm과.xlsx를 모두 지원한다(domain 규칙67). 서버는 파일명 역할 판별과 함께 확장자·선언 MIME·실제 파일 형식/OOXML package·고정 양식의 필수 구조를 검증한다. 기준 파일과 같은 확장자라는 조건만으로 호환성을 판정하지 않으며, 구조가 맞는.xlsx를 기준본이.xlsm이라는 이유로 차단하지 않는다. 허용 확장자여도 실제 형식/양식이 맞지 않으면 실패하고 초안 생성/파일 연결·교체를 확정하지 않는다. 이름 변경만으로 형식을 변환하지 않는다. 형식별 보존·편집·최종저장 제품 검증은 후속이며 최종저장 형식은 아래 원본 형식 유지 후속 결정을 따른다.

**2026-10-19 최종저장 형식 사용자 확정:** domain 규칙16/67에 따라 각 역할의.xlsx 원본은.xlsx로,.xlsm 원본은.xlsm으로 최종저장한다. 서로 다른 형식의 역할 파일이 한 Case에 포함돼도 형식을 통일하지 않는다. 최종 파일 metadata의 확장자·MIME과 실제 package는 원본 형식 및 생성 결과에 맞아야 하며, 현재.xlsm 고정 값을.xlsx 결과에 그대로 적용하지 않는다. 원본 보존/승인 변경·계산/구조 검증과 네 역할 완성/요청 전체 성공·실패 원칙은 유지한다. 실제 형식별 생성·저장/복원·Case/Run 연결 검증은 후속이다.

**2026-10-19 업로드 용량 사용자 확정:** 파일 하나의 상한은 **50MiB = 52,428,800바이트**다. 각 요청의 multipart `file` part에 전송된 파일 바이트 수를 기준으로 `0 < size <= 52,428,800`을 허용하며 빈 파일은 기존 규칙대로 거절한다. 정확히 상한인 파일은 용량 조건을 통과하고52,428,801바이트부터 거절한다. 다른 형식/양식·권한 조건은 별도로 검증한다. FE는 업로드 전에 크기를 확인하고 서버도 실제 수신 파일 크기를 검증한다. 초과 시 초안 생성/파일 추가·교체와 편집 초기화를 확정하지 않으며 기존 성공 상태를 유지한다. 이 상한은 파일당 기준이며 초안의 네 파일 누적 합계나 multipart 요청 전체 크기를50MiB로 제한하는 의미가 아니다. 현재 전역 multipart300MB와 구분하고 요청 전체 제한은 허용 파일 및 multipart 부가 데이터를 수용해야 한다. 압축 해제 후 크기·entry/셀 수·처리 시간/메모리 제한은 별도 기술 검증으로 구체화하며, 초과 오류의 HTTP/ResultCode 표현은 Open Questions에서 확정한다.

**2026-10-19 multipart 전달 형식 사용자 확정 — 1안 채택:** 두 업로드 API 모두 전체 요청은 `multipart/form-data`이며 아래 두 part를 보낸다. metadata 항목을 각각의 일반 form field나 query로 분산하지 않는다.

| part | 필수 | Content-Type / 의미 |
| --- | --- | --- |
| `file` | 예 | 선택한 workbook 하나의 binary. 실제 원본 형식에 맞는 MIME과 파일명으로 보내며 이미 저장된 fileNo나 경로가 아니다. 파일당50MiB 제한을 적용한다. |
| `metadata` | 예 | `application/json`의 JSON object. 아래 요청 항목을 포함한다. JSON을 다시 문자열로 감싼 값이나 단순 text part로 대신하지 않는다. |

| metadata 항목 | JSON 타입 | 최초 초안 생성 | 기존 초안 파일 추가·교체 | 의미 |
| --- | --- | --- | --- | --- |
| `sourceKey` | string | 필수 | 필수 | FE 출처 목록에서 서버가 제공한 EXCEL_UPLOAD 키. 실제 키 값은 별도 출처 계약을 따르며 파일명으로 대신하지 않는다. |
| `requestId` | string, UUID | 필수 | 필수 | 기존 공통 편집과 같은 FE 발급 요청 식별자. 같은 요청 재전송은 같은 값을 유지한다. |
| `baseRevision` | integer, 0 이상 | 해당 없음, 생략 | 필수 | 화면이 확인한 대상 초안의 성공 revision. 숫자 문자열로 보내지 않는다. |
| `replaceConfirmed` | boolean | 해당 없음, 생략 | 기존 역할 파일의 교체는 true 필수. 새 역할 추가는 생략/false 가능 | 편집 초기화 안내·동의 후 제출하는 확인값. 문자열 "true"나 숫자1로 대신하지 않는다. |

후속 대상 `draftId`는 경로로만 지정하며 metadata로 다른 초안을 선택하지 않는다. 파일 역할 kind는 기존 확정대로 서버가 파일명·구조로 판별한다. 파일과 metadata는 한 요청의 내용이며 서로 다른 파일의 요청 정보를 섞지 않는다. 후속 requestId의 중복 키 저장 범위·재전송 동일성 세부/오류는 Open Questions에서 구체화한다.

아래는 `metadata` part 자체의 JSON 예시이며 전체 HTTP body나 응답 예시가 아니다. sourceKey의 `<EXCEL_UPLOAD 출처 키>`는 설명용 자리표시자이며 실제 서버 제공 키로 대체한다. revision5는 설명용 현재 버전이다.

최초 파일 — `POST /api/v1/mi/fundamental-model/input-upload-drafts`:

```json
{
  "sourceKey": "<EXCEL_UPLOAD 출처 키>",
  "requestId": "b0000000-0000-4000-8000-000000000201"
}
```

새 역할 파일 추가 — `POST /api/v1/mi/fundamental-model/input-upload-drafts/{draftId}/files`:

```json
{
  "sourceKey": "<EXCEL_UPLOAD 출처 키>",
  "requestId": "b0000000-0000-4000-8000-000000000202",
  "baseRevision": 5
}
```

동일 역할 파일 교체 — 같은 후속 API, 사용자 확인 후 제출:

```json
{
  "sourceKey": "<EXCEL_UPLOAD 출처 키>",
  "requestId": "b0000000-0000-4000-8000-000000000203",
  "baseRevision": 5,
  "replaceConfirmed": true
}
```


`kind`는 FE가 필수 입력하는 역할 선택값이 아니라 서버가 판별한 결과로 사용한다. 기존 역할 식별 패턴대로 파일명을 대소문자 무관으로 검사해 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 중 정확히 한 역할을 판별한다. 역할 없음/복수 역할은 실패하며, 최종 역할은 workbook 구조와도 일치해야 한다. 최초 표는 아래 자동 선택 규칙을 따른다. 후속 표 선택 request는 Open Questions에서 구체화하고 최초/후속 multipart 요청은 위 file/metadata 계약을 따른다. metadata.sourceKey와 file을 기준으로 해당 kind 하나만 반환하는 아래 표현은 Draft 기준이며 whole-sheet 값 반환으로 확대하지 않는다.

**포맷 선택과 검증:**

1. 인증/허용 권한, 출처 유형, 파일 기본 조건과 파일명 역할을 검증한다. 저장 case 조회의 `caseId + SCNRCASE`·4파일 완전성 조건을 단일 업로드에 적용하지 않는다. 파일명은 저장 경로로 사용하지 않으며 SECURITY.md의 업로드·압축 해제 제한을 따른다.
2. 최초 파일은 서버가 해당 모델의 ACTIVE 양식을 선택해 검증/파싱하고, 성공 시 그 버전을 새 초안에 고정한다. 최초 선택 시 ACTIVE가 없거나 해당 역할 정의가 없으면 실패한다. 후속 파일은 초안에 고정된 버전의 해당 역할 정의로 검증하며, 새 ACTIVE가 있어도 전환하지 않는다. 기존 버전의 DEPRECATED 전환만으로 초안 기준을 교체하지 않으며 고정 정의를 확인할 수 없으면 다른 버전으로 대체하지 않는다. 기존 Case의 ACTIVE/DEPRECATED 연결을 새 초안 최초 선택의 fallback으로 가져오지 않는다.
3. 실제 workbook의 필수 sheet/헤더/표 경계와 역할 구조를 선택한 양식 정의에 맞춰 검증한다. 파일명만 믿지 않는다. 값 차이와 catalog가 허용한 행·열 확장은 허용하며 파일 전체가 기준본과 byte/hash까지 같을 것을 요구하지 않는다. 구체적 확장 판정은 표별 확정 정책을 따른다. 개발 시트의 수용·원본 보존·화면 제외는 domain 규칙39~40을 유지한다.
4. 구조가 맞지 않거나 알려지지 않은 양식이면 실패하고 다른 kind/버전/색 기반 포맷으로 자동 fallback하지 않는다. 업로드 시 발견한 새 시트·표·편집 범위가 catalog 정의를 자동 갱신하지 않는다.
5. 해당 kind의 전체 표시 구조를 정의에서 구성하고 선택 영역의 헤더·grid·부속 control 값을 업로드 원본에서 읽는다. 표 좌표/원본 번호/표시값·빈 편집 위치와 제외 정책은 §3.1~§3.2를 재사용한다. 저장값은 사용자 파일에서 읽고 기준본 값으로 조용히 대체하지 않는다.
6. 검증/파싱에 성공한 파일만 업로드 묶음에 추가할 수 있다. 불일치·판독/검증 실패 시 새 Case 생성/해당 파일 연결을 하지 않고 앞선 성공 파일·편집 상태를 보존한다. 검증 실패 뒤 미리 생성된 Case나 파일 연결을 정상 상태로 남기는 구현은 허용하지 않는다. 잘못된 workbook을 빈 성공으로 반환하지 않으며 내부 파일 경로는 노출하지 않는다. 과거 파싱 전용200은 새 생성/연결의 확정 상태코드가 아니며 상세 응답 schema는 이 절의 기술 계약을 따르며 접수/상태 조회 HTTP는 위 확정 계약을 따른다. 자원 제한/stream 종료는 §8 및 SECURITY.md를 따른다.

**조회와 공통 응답, operation별 반환 범위:**

아래 업로드의 구조/표 표현은 비동기 처리 성공 결과의 범위다. 접수/진행 응답의 operationId/status/statusEndpoint는 위 확정 계약을 따르고 잔여 schema는 Open Questions에서 구체화하며, 접수만으로 성공 데이터를 채우지 않는다.

| 항목 | 저장 case 입력값 조회 | 단일 파일 업로드 파싱 |
| --- | --- | --- |
| envelope/필드 표현 | BaseResponse.data 및 §3.1 | 동일 |
| `sourceKey` | 조회한 저장 출처 | 요청한 업로드 출처 |
| `sections` | 네 kind의 전체 표시 구조 | 업로드한 kind 하나의 전체 표시 구조; 다른 kind를 비우거나 덮어쓰는 응답 아님 |
| `sections[].inputId` | 해당 역할의 원본/metadata 기준 | 파싱한 사용자 파일과 고정 양식 버전을 구별하는 서버 식별자; 파일명으로 대체하지 않음 |
| `tableData` | 선택 영역 하나의 grid 및 부속 controls | 업로드 성공 결과는 해당 kind의 첫 표시 표를 자동 선택한 데이터와 부속 controls를 동봉. 전체 sheet/모든 표 값은 반환하지 않음 |

**2026-10-19 업로드 최초 표시 사용자 확정 — 2안 채택:** 최초 파일 생성·후속 파일 추가/교체의 비동기 처리 성공 결과는 업로드한 역할의 구조와 **첫 표를 자동 선택한 tableData**를 함께 제공한다. 해당 역할의 응답 `sheets` 표시 순서대로 탐색해 표시 대상 `tables`가 있는 첫 시트의 첫 표를 선택한다. 기존 고정 양식/catalog의 표시 순서를 재사용하고 숨김·개발/구분 시트 등 화면 제외 대상은 기존 규칙대로 건너뛴다. 원본의 첫 물리 시트나 이름순·수정 가능 여부로 순서를 다시 정하지 않는다.

FE는 `tableData.tableId`에 해당하는 표를 선택 상태로 표시하고 그 값과 명시적으로 연결된 부속 control을 표시한다. 첫 표가 읽기 전용이어도 그대로 선택하며 자동 선택이 편집 허용을 뜻하지 않는다. control 전용 논리 영역은 기존 tableData 표현을 재사용한다. 표의 최초 페이지/크기·표시값 규칙은 기존 조회 표현을 재사용하며 전체 sheet나 모든 표의 값을 함께 읽어 반환하는 요구로 넓히지 않는다. 이 성공 결과를 `tableData:null` 미선택 상태로 대신하지 않고 기준본 값이나 mock으로 채우지 않는다. 접수/진행 응답에는 아직 확정되지 않은 첫 표 성공 데이터를 넣지 않는다.

`tableData.tableId`와 control의 `sourceTableId`는 응답 구조 안의 해당 kind에 속하고 같은 업로드 inputId/고정 양식·성공 revision 기준이어야 한다. 다른 kind의 파일·편집을 초기화하지 않는다. **기존 저장 Case QRY-01/02의 최초 미선택/구조와 표 값 조회 분리는 그대로 유지**하며 이번 결정은 업로드 처리 성공 결과의 최초 표시에 적용한다. 후속 표 전환 조회의 구체 요청/schema는 Open Questions를 따른다.

FE는 성공한 업로드의 해당 kind 구조/데이터만 반영하고 다른 kind를 초기화하지 않는다. 동일 역할 파일은 위 사용자 확정대로 확인·검증 성공 후 교체하며 해당 역할의 기존 편집을 초기화한다. 취소/검증 실패 시 기존 파일과 편집은 유지한다. 후속 표 선택/편집은 같은 사용자 업로드 원본을 사용하며 매번 재업로드하거나 기준본을 조회하는 방식으로 대신하지 않는다. 보존/접근/만료와 편집 요청 경계는 Open Questions에서 확정한다. 네 역할이 각각 검증·연결되기 전에는 FE의 최종저장을 비활성화하고 BE도 최종저장을 거절한다. 파일 개수가4개여도 역할 누락/중복이면 완성이 아니며, 네 역할 준비만으로 기존 편집·계산/최종저장의 다른 검증을 생략하지 않는다.

## 4. Server-side Resolution and Validation

아래는 저장 case 조회의 검증이다. 단일 파일 업로드는 §3.3을 따른다.

1. `caseId`로 `del_yn = 'N'`인 active case를 조회한다.
2. 허용 `execType`은 `INDEPENDENT | MI_BASE`다. `SCENARIO`는 active resource hiding 규칙으로 `404 RSC_NOT_FOUND`를 반환한다.
3. `tcm_filel`에서 `del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = :caseId`인 파일을 조회한다.
4. 파일명의 `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 키워드를 대소문자 무관으로 검사해 각 역할을 정확히 1개씩 선택한다.
5. active 파일이 4개가 아니거나 역할이 누락·중복되면 불완전한 input으로 처리한다.
6. DB metadata와 실제 object storage 객체가 모두 존재하는지 확인한다. 전체 화면 구조는 같은 source의 완전한 4파일 set을 가리켜야 한다.
7. 선택한 표가 해당 파일·시트·표 구조에 속하는지 검증하고 해당 표의 표시 데이터를 추출한다. 정상적으로 값이 없는 표와 원본을 읽지 못한 실패를 같은 빈 성공으로 반환하지 않는다.

## 5. Response and Access Boundary

- 응답은 `BaseResponse<T>`와 JSON을 사용한다.
- 구체적인 DTO는 §3의 전체 구조/단일 표 값 경계를 따르며 field fidelity와 operation 구성을 확정한 뒤 정의한다.
- 호출 권한 후보는 기존 목록과 같은 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`다.
- 현재 공유 조회 정책에서는 case 소유자 `usr_no`가 현재 사용자와 달라도 input 접근을 허용한다.
- 저장 출처에서는 서버가 case와 연결된 정확한 파일을 선택한다. 업로드 출처에서는 사용자가 전송한 파일을 처리하며 클라이언트가 서버 file/storage 식별자를 지정하지 않는다. 후속 업로드 inputId는 권한 검사 없이 다른 사용자의 원본을 선택하는 수단으로 사용하지 않는다.
- input request/response body는 대용량·업무 민감 데이터일 수 있으므로 HTTP 본문 로깅 대상에서 제외한다.
- **2026-10-15 사용자 확정 — JSON DRM 비적용:** QRY-01/02가 엑셀에서 추출해 웹 화면에 제공하는 구조·저장값 JSON에는 회사 DRM 정책을 적용하지 않는다. 이 적용 여부는 미결정/추가 승인 항목에서 제외한다.
- HTTPS·인증·role/condition 및 tenant/Case/file 접근 검사와 위 본문 로깅 제외는 유지한다. 원본 파일의 기존 암복호화 정책도 유지하며 JSON 비적용을 파일 DRM의 전역 해제로 확대하지 않는다.

## 6. Representative Errors — Draft

| 조건 | HTTP | `ResultCode` | 의미 |
|---|---:|---|---|
| `caseId < 1` 또는 형식 오류 | 400 | `REQ_INVALID_PARAMETER` | 잘못된 case 식별자 |
| case 없음, soft delete, `SCENARIO` case | 404 | `RSC_NOT_FOUND` | input 제공 대상이 아님 |
| input 4개 미만/초과, 역할 누락·중복 | 409 | `BIZ_RULE_VIOLATION` | input 정합성 오류 |
| DB metadata는 있지만 저장 객체를 읽을 수 없음 | 500 | `SYS_INTERNAL_ERROR` | 저장소와 metadata 불일치 |
| 인증되지 않음 | 401 | 공통 인증 오류 | 로그인 필요 |
| 허용되지 않은 role/condition | 403 | 공통 권한 오류 | 접근 불가 |

오류 계약은 endpoint와 전달 방식을 정할 때 함께 확정한다. 내부 저장 경로나 provider 오류 원문은 응답에 노출하지 않는다.

## 7. OpenAPI Documentation Plan

- 계약 확정 후 operation을 정의한다.
- description에는 `caseId` 기준, `DEMAND/SUPPLY/TRADE/OUTPUT` 4개, incomplete input 의미를 명시한다.
- JSON schema에는 네 파일의 구조 metadata와 선택 표 데이터의 구분, table 식별, 헤더/값 타입·nullable·빈 목록 의미와 version을 기록한다.
- 전체 구조의 각 table에는 실제 값이 없고 값 payload는 선택 영역의 grid 및 명시적으로 연결된 부속 control만 담는다는 제약을 example/description과 검증 기준에 명시한다.
- FE와 동일한 `sourceKey/sections/kind/inputId/sheetName/tables` 및 `tableData.columns/rows/inputCells/inputGuideRules`를 유지하고 새 `controlIds/controls` 계약을 기술한다. `sheets[].data`는 제공하지 않고 grid 헤더는 rows에 포함한다. 일반 표·표와 control·control 전용 영역의 예시를 구분하고 공통 `BaseResponse`까지 포함한 최종 schema로 FE 인터페이스를 생성한다.
- operation을 분리할 경우 구조 조회와 표 값 조회의 책임, paging/default/snapshot 의미를 구분한다.
- 대표 200/400/401/403/404/409/500 응답을 `@ApiResponses`로 구조화한다.
- 업로드는 §3.3의 확정된 최초 생성/후속 파일 추가 두 POST 경로를 각각 기술한다. 파일 수신 후의 비동기 접수 응답, 상태 조회와 성공 결과를 구분해 기술하고 접수만으로 초안 생성/연결이 끝났다고 표시하지 않는다. 같은 초안의 새 변경 거절과 기존 성공 상태/결과 조회 허용·동일 요청 재전송의 구분과 replaceConfirmed의 조건부 true 필수·생략/false 거절을 기술한다. 후속 파일 요청의 baseRevision 필수 검증·성공 revision과 실패/재조회 시 유지·충돌 후 재조회 흐름 및 FAILED의 기존 결과200/새 requestId 재시도·현재 baseRevision을 기술한다. 업로드 접수 후120초/전송 제외·FAILED/TIMEOUT/늦은 성공 차단과 실제 검증 미완료를 구분하고, 업로드 성공 결과의 첫 표시 표 자동 선택/tableData 동봉·접수/진행 응답과의 차이, 초안의 uploadedKinds/missingKinds/canFinalize와 처리 중/교체 사례, failure의 상태별 null/object·reason/currentRevision/issues·업로드 위치 schema/대표 실패 예시를 기술한다. multipart file/metadata(JSON object), metadata의 sourceKey/requestId/baseRevision/replaceConfirmed와 후속 경로 draftId, 파일명에서 판별한 kind와 해당 역할 sections·선택 영역 데이터 예시를 맞춘다. 확정된 접수202·상태 GET200/2초 조회·operationId/status/statusEndpoint와 잔여 세부 DTO/오류·재전송·.xlsm/.xlsx 실제 형식 검증/용량·구조 불일치/양식 부재/접근 오류는 Open Questions 확정 후 schema/ResultCode까지 맞춘다. FE의 임의 문자열 오류코드를 기존 ResultCode에 검토 없이 추가하지 않는다.

## 8. Data and Implementation Impact

- 기존 `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` 재사용을 우선한다.
- 서버가 선택한 원본은 streaming OOXML/SAX처럼 전체 workbook object model을 만들지 않는 방식으로 필요한 값만 읽는다.
  `INPUT-02`에서 Apache POI `XSSFWorkbook` 전체 로딩은 1 GiB heap으로 실패했으므로 조회 기본 경로로 사용하지 않는다.
- 구조 metadata와 실제 값 추출을 구분한다. 선택 영역의 헤더·본문 및 명시적으로 연결된 control 값/선택지를 읽기 위해 원본을 탐색할 수는 있지만 그 밖의 미선택 표 값은 DTO/응답에 포함하지 않는다.
- 이 응답 단위 결정 자체는 새 DB table/column이나 cache 추가를 요구하지 않는다. catalog persistence는 기존 domain/DDL 설계가 소유한다.
- workbook 크기, sheet/row/cell 수, 응답 크기에 상한을 두고 stream/workbook을 항상 닫는다.
- 구체적인 상한은 실제 input 표본을 측정한 뒤 확정한다.
- 업로드도 저장된 immutable 양식 정의와 같은 표시 추출 규칙을 사용한다. 이 공통 표현 결정만으로 별도 범용 파서 framework/cache/새 DB table을 요구하지 않는다. 사용자 업로드 처리 중 stream/temp 자원은 정상·실패 경로 모두 정리하며 반환 후 원본 보존 경계는 §3.3의 후속 조회 계약에 맞춰 확정한다.

## 9. Acceptance Criteria — Common Draft

- Given 파일 전송이 끝나 작업이 접수됐을 때, then 대기열·검증/파싱·성공 확정을 합친120초 기한을 적용한다. 업로드 전송 시간과 FE 조회 간격을 기한에 섞지 않는다.
- Given 기한까지 성공 확정되지 않은 작업일 때, then 실행 종료/저장 여부를 확인해 FAILED/TIMEOUT으로 확정하고 늦은 성공 반영을 차단한다. 최초는 초안/Case를 만들지 않고 후속은 기존 파일·편집·revision을 유지한다.
- Given 기한 안에 성공이 확정됐지만 응답/조회가 기한 뒤에 도착했을 때, then 원래 성공을 유지한다. TIMEOUT은 HTTP200 상태 조회/failure.issues:[]로 제공하며 재시도에는 기존 새 requestId 정책을 적용한다.

- Given A의 취소가 서버에 확정됐을 때, when B의 첫 파일 검증이 실패하면, then A는 취소 상태로 유지하고 B 초안/Case를 생성하지 않는다. A의 파일·편집을 복구/재활성화하거나 취소를 되돌리지 않는다.
- Given B 최초 업로드가 FAILED일 때, when 같은 requestId·동일 내용을 재전송하면 기존 실패 결과를 반환한다. 사용자의 실제 재시도는 새 requestId로 최초 생성 API를 호출하고 baseRevision은 생략한다.
- Given A 취소가 실패했거나 취소 확정 여부를 모를 때, then B의 새 작성을 먼저 시작하지 않고 A 상태를 확인한다. 확인 전/취소 실패의 기존 내용 보존 계약은 그대로 유지한다.

- Given 미완료 직접 업로드 작성을 새 Case로 전환하려 할 때, then 현재 내용 취소 안내/확인 전 또는 확인 철회 시 기존 파일·편집·고정 양식·성공 revision을 유지한다.
- Given 현재 초안의 변경 작업이 대기/처리 중일 때, then 확인값이 있어도 취소를 확정하지 않고 진행 작업을 강제 중단하지 않는다. 확인 화면 뒤 새 작업이 접수된 경우도 서버가 재검증해 거절한다.
- Given 사용자 확인과 진행 작업 없음/소유권 검증 후 취소가 확정됐을 때, then 현재 작성의 쓰기/최종저장을 차단하고 새 작성을 허용한다. 취소 처리 실패는 기존 상태와 단일 작성 제한을 유지하며 실제 Case를 생성하지 않는다.

- Given 같은 사용자의 직접 업로드 작성이 있거나 최초 파일 검증이 진행 중일 때, when 다른 requestId/탭에서 별도 최초 생성을 요청하면, then 두 번째 활성 작성을 만들거나 기존 작업을 자동 종료/삭제·덮어쓰지 않는다. 기존 동일 요청 재전송은 원래 결과를 확인한다.
- Given 같은 사용자의 직접 업로드 Case 최종저장이 성공했을 때, then 다음 Case의 새 작성을 허용한다. 다른 사용자와 다른 출처 Draft의 기존 생성/편집 정책은 이 개수 제한 때문에 변경하지 않는다.

- Given SUPPLY 파일 교체가 성공했을 때, then SUPPLY 파일/해당 편집 초기화와 초안 revision 증가만 확정하고 DEMAND/TRADE/OUTPUT의 파일·표·사용자 입력·계산 결과는 유지한다. 파일 간 재계산이나 의존 검사를 새 교체 조건으로 추가하지 않는다.
- Given 교체 파일이 검증에 실패했을 때, then 모든 역할의 기존 파일·편집과 성공 revision을 그대로 보존한다. 다른 파일에 영향이 있다는 가정으로 새 초안 생성을 요구하지 않는다.

- Given 업로드 작업이 FAILED로 확정됐을 때, when 같은 requestId·동일 파일/metadata를 재전송하면, then HTTP200으로 원래 operationId/FAILED/failure를 반환하고 새 작업·재실행·파일 연결·revision 증가를 하지 않는다.
- Given revision5에서 후속 업로드가 실패한 뒤 현재 초안이 revision6이 됐을 때, then 원래 요청 재전송은 원래 실패 결과/currentRevision5를 반환한다. 새 재시도는 새 requestId/baseRevision6으로 요청하며 자동 파일 교체/기존 성공 상태 초기화를 하지 않는다.
- Given 최초 파일 업로드가 FAILED여서 초안이 없을 때, then 새 재시도는 새 requestId로 최초 생성 API를 호출하고 baseRevision을 보내지 않는다. 새 파일/metadata는 이전 requestId로 바꿔 보내지 않는다.
- Given 통신 끊김이나 FE 대기시간 경과만 있을 때, then 새 requestId를 자동 발행하지 않고 기존 상태 조회/동일 요청 확인으로 서버 결과부터 확인한다.

- Given SUPPLY의 기존 성공 파일이 없고 새 파일이 대기/처리 중일 때, then uploadedKinds에는 SUPPLY가 없고 missingKinds에 SUPPLY가 있으며 canFinalize는 false다. 전송 완료만으로 준비 완료로 세지 않는다.
- Given 네 역할의 기존 성공 파일이 있고 SUPPLY 교체가 진행 중일 때, then uploadedKinds는 네 역할을 유지하고 missingKinds는 []이지만 canFinalize는 false다. 실패하면 기존 SUPPLY 파일·편집·revision과 역할 목록을 보존한다.
- Given 네 역할이 준비돼 진행 작업이 없을 때, then canFinalize는 초안 상태와 기존 최종저장 조건까지 확인해 계산한다. 목록과 boolean은 같은 서버 상태로 제공하고 실제 최종저장 시 다시 검증한다.
- Given 최초 파일이 아직 처리 중일 때, then 준비 상태 필드로 가짜 초안을 만들지 않는다. 과거 작업의 준비 상태를 조회해도 현재 초안의 파일·편집·가능 상태를 과거로 되돌리지 않는다.

- Given 업로드 검증의 문제 위치를 확인했을 때, then FAILED의 failure.reason과 issues에 FILE/SHEET/TABLE/HEADER 수준·kind/시트/표/헤더 셀 좌표를 구분해 제공한다. 해당하지 않거나 확인하지 못한 위치는 null이며 원본 값·수식·내부 경로는 반환하지 않는다.
- Given 작업이 QUEUED/RUNNING/SUCCEEDED일 때, then failure는 null이다. FAILED의 issues는 배열이며 판독 불가로 위치가 없는 경우만 안전한 사유/[]를 허용하고 실제 검증 실패를 성공/빈 정상 결과로 바꾸지 않는다.
- Given 최초 업로드가 실패했을 때, then failure.currentRevision은 null이며 초안/Case를 만들지 않는다. 후속 실패는 판정 시점의 성공 revision과 기존 파일·편집을 보존한다. 접수 전 거절은 가짜 실패 작업으로 기록하지 않는다.

- Given 새 업로드 작업이 접수됐을 때, then HTTP202와 operationId/status/statusEndpoint를 반환한다. 첫 파일의 초안 생성이 아직 완료되지 않아도 operationId 기반 공통 GET으로 조회할 수 있다.
- Given 상태 GET이 성공했을 때, then HTTP200과 해당 작업 상태/결과를 반환한다. FAILED도 조회 HTTP200이며 인증/소유권 확인과 조회의 읽기 전용 경계를 유지한다.
- Given FE가 상태를 확인할 때, then2초 간격으로 QUEUED/RUNNING을 조회하고 SUCCEEDED/FAILED에서 반복 조회를 종료한다. 화면 이탈 시 중지하고 연결 끊김/대기시간 경과를 서버 실패 확정으로 해석하지 않는다.
- Given 허용된 동일 업로드 요청을 재전송했을 때, then 진행 중202/종료 후200으로 기존 operationId/결과를 반환하고 파일/편집/revision에 새 변경을 만들지 않는다.

- Given 최초 또는 후속 파일을 업로드할 때, then multipart의 file에는 workbook binary, metadata에는 application/json object를 보낸다. metadata를 일반 form field나 이중 인코딩한 JSON 문자열로 대신하지 않는다.
- Given 최초 초안 생성일 때, then metadata에 sourceKey/requestId를 보내며 baseRevision/replaceConfirmed는 생략한다. 후속 추가·교체는 baseRevision도 필수이고 기존 역할 교체는 boolean replaceConfirmed:true를 요구한다. 숫자/boolean을 JSON 문자열로 보내지 않는다.

- Given 업로드 검증·파싱이 성공했을 때, then 업로드한 역할의 구조와 첫 표시 표의 tableData를 함께 제공하며 FE는 그 tableId를 자동 선택해 값을 표시한다. 최초 생성/후속 추가·교체에 같은 규칙을 적용한다.
- Given 화면 제외 시트나 표 없는 시트가 먼저 있을 때, then 응답 표시 순서에서 표가 있는 첫 시트의 첫 표를 선택한다. 첫 표가 읽기 전용이어도 임의의 수정 가능 표로 바꾸지 않으며 mock/기준본 값이나 tableData:null로 대신하지 않는다.
- Given 업로드가 접수/처리 중일 때, then 첫 표의 성공 데이터를 미리 표시하지 않는다. 업로드 성공의 자동 선택 결정은 기존 저장 Case QRY-01 최초 미선택 정책을 변경하지 않는다.

- Given 같은 역할의 기존 파일과 편집이 있을 때, when 새 교체 요청에서 replaceConfirmed를 생략하거나 false를 보내면, then 변경 접수를 거절하고 파일·편집·revision을 유지한다. 자동 교체/다른 초안 생성으로 대체하지 않는다.
- Given 사용자가 편집 초기화를 확인하고 replaceConfirmed:true를 보냈을 때, then baseRevision·권한·진행 제한·고정 양식 검증을 모두 적용한다. 성공 뒤에만 교체/편집 초기화와 revision 증가를 함께 확정하며 검증 실패나 stale baseRevision이면 기존 상태를 유지한다.
- Given 해당 역할의 기존 파일이 없을 때, then 신규 파일 추가에 교체 확인을 요구하지 않는다. 나머지 파일 검증/baseRevision 조건은 유지한다.

- Given 초안의 성공 revision이5일 때, when 새 파일 추가·교체 요청의 baseRevision이4이면, then 거절하고 파일·편집·revision5를 보존한다. baseRevision 없는 새 후속 요청도 허용하지 않는다.
- Given 현재 revision5와 일치하는 baseRevision5로 후속 파일이 접수됐을 때, then 대기/처리 중 성공 상태 조회는 revision5를 유지한다. 전체 처리 성공 시 파일/편집 상태와 revision6을 함께 확정하고 실패 시 revision5와 기존 상태를 유지한다.
- Given 파일 추가·교체가 revision6으로 성공했을 때, when 같은 성공 작업의 결과를 재조회하거나 허용된 동일 요청을 재전송하면, then 기존 결과/revision6을 반환하고 revision7을 만들지 않는다. 충돌을 피하려고 FE가 자동으로 baseRevision만 갱신해 덮어쓰지 않는다.

- Given 초안A의 파일 추가·교체/편집 저장/최종저장 작업이 접수돼 대기 또는 처리 중일 때, when 같은 초안에 새 변경을 요청하면, then 역할이 달라도 거절하고 자동 예약하지 않는다. 반대로 편집 저장/최종저장 중 새 파일 업로드도 병행하지 않는다.
- Given 초안A에 진행 작업이 있을 때, then 직전 성공 상태의 구조/표와 작업 결과는 계속 조회할 수 있고 미완료 파일·편집은 섞이지 않는다. 허용된 동일 요청 재전송은 기존 접수/결과를 반환하며 새 작업을 만들지 않는다.
- Given 초안A의 작업이 진행 중일 때, then 초안B의 독립 요청은 A의 진행 제한 때문에 거절하지 않는다. A의 작업이 성공/실패로 확정된 뒤에는 다음 변경을 요청할 수 있다.

- Given 파일 수신 뒤 업로드 요청이 접수됐을 때, then 검증·파싱은 비동기로 진행하고 접수 식별자/상태 조회 수단을 제공한다. 접수 결과를 초안 생성/파일 연결 성공으로 표시하지 않는다.
- Given 최초 파일이 대기/처리 중일 때, then 편집 초안이나 실제 Case를 생성하지 않으며 성공 결과의 draftId를 미리 제공하지 않는다. 검증·파싱 성공 후에만 초안과 그 파일의 편집을 제공하고 실패 시 초안을 만들지 않는다.
- Given 기존 초안에 후속 파일을 접수했을 때, then 처리 중인 파일은 준비된 역할에 포함하지 않고 성공 후에만 추가·교체를 확정한다. 검증·파싱 실패 시 기존 파일·편집을 보존한다.
- Given 최초 업로드가 처리 중일 때, when 같은 requestId·동일 파일/요청을 재전송하면, then 같은 접수/처리 결과를 확인하게 하고 작업·초안을 중복 생성하지 않는다. 상태 GET은 처리를 시작하거나 파일을 연결하지 않는다.

- Given 첫 파일의 초안 생성이 성공했지만 응답을 받지 못했을 때, when 같은 requestId·동일 파일/요청을 재전송하면, then 원래 초안과 최초 업로드 결과를 반환하며 새 초안 생성·편집 초기화·양식 버전 변경을 하지 않는다. 동시 재전송도 초안을 중복 생성하지 않는다.
- Given 기록된 최초 생성 requestId가 있을 때, when 같은 ID로 다른 파일 바이트 또는 다른 의미의 요청을 보내면, then 충돌로 거절하고 기존 상태를 보존한다. 파일명이 같아도 바이트가 다르면 동일 파일로 인정하지 않는다.
- Given 이미 후속 파일/편집이 있는 초안일 때, when 최초 성공 요청을 재전송하면, then 최초 업로드 결과 반환 때문에 현재 파일·편집을 되돌리지 않는다. 다른 인증 범위의 결과 접근은 허용하지 않는다.

- Given 첫 파일을 올릴 때, when `POST /api/v1/mi/fundamental-model/input-upload-drafts`의 검증·파싱이 성공하면, then 새 편집 초안과 서버 발급 draftId를 반환하며 실제 Case는 생성하지 않는다. 검증 실패 시 초안을 만들지 않는다.
- Given 소유한 업로드 초안이 있을 때, when `POST /api/v1/mi/fundamental-model/input-upload-drafts/{draftId}/files`를 요청하면, then 그 초안에만 파일을 추가하거나 확정된 확인/검증 규칙에 따라 같은 역할을 교체한다. 실패 시 기존 파일·편집을 보존한다.
- Given 후속 요청의 draftId가 없거나 접근할 수 없을 때, then 다른 초안을 선택하거나 새 초안 생성으로 대체하지 않는다. 구체 오류 HTTP/ResultCode는 후속 확정한다.

- Given 수식 셀이 화면 표시 범위에 있을 때, then 셀 표시 데이터는 수식 문자열이 아닌 결과값이며 입력/구조 변경에 필요한 화면 갱신 규칙은 §3.1C의 별도 정보로 제공한다.
- Given LiqUtilAdjustment에 마지막 월 열 추가 규칙이 전달됐을 때, when FE가 마지막 열 뒤에 새 열을 추가하면, then 새 Period는 직전 마지막 Period + 1로 표시된다. 규칙의 구체 JSON 표현은 확정 후 검증한다.
- Given SUPPLY ProductionAdjustment의 해당 표를 조회/업로드 파싱할 때, then `L7:L200`의 헤더/값/편집 위치를 화면 payload에서 제외하고 다른 열의 원본 번호를 유지한다. 원본 파일에서 이 범위의 수식/값을 삭제하거나 대체하지 않는다.
- Given ProductionAdjustment 행 추가로 신규 L셀이 생성된 source를 조회할 때, then 그 셀도 화면에 노출하지 않는다. 신규 L셀은 domain 규칙 31에 따라 빈칸이며 기존 오류 수식의 복사/확장 예외를 적용하지 않는다.
- Given 첫 정상 파일의 검증·파싱이 성공했을 때, then 실제 Case 대신 업로드 편집 초안을 생성하고 해당 파일의 구조/선택 영역 값을 반환해 편집할 수 있다. 후속 정상 파일은 같은 초안에 연결하며, 네 역할이 갖춰져도 업로드만으로 Case를 만들지 않는다. 최종저장 성공 시 실제 새 Case를 생성한다.
- Given 파일명 역할을 정할 수 없거나 workbook이 그 역할/적용 양식(최초 ACTIVE·후속 초안 고정 버전)과 맞지 않을 때, then 해당 요청은 실패하고 Case 생성·파일 연결 없이 이전 성공 상태를 유지한다. 빈 성공/다른 포맷 fallback으로 처리하지 않으며 허용된 값 차이와 구조 확장을 불일치로 오인하지 않는다.
- Given 첫 정상 파일로 버전A의 편집 초안을 만든 뒤 버전B가 ACTIVE가 됐을 때, then 같은 초안의 후속 업로드·조회·편집·최종저장은A를 사용하고 새 초안은B를 선택한다. A에 맞지 않는 파일을B로 자동 재시도하거나 기존 편집을B로 다시 해석하지 않는다.
- Given 같은 역할의 기존 파일과 편집이 있을 때, when 사용자가 편집 초기화를 확인하고 새 파일 검증·파싱이 성공하면, then 그 역할의 파일 교체와 편집 초기화를 함께 확정하고 다른 역할의 파일/사용자 편집·고정 양식 버전은 유지한다. 교체만으로 Case를 생성하지 않는다.
- Given 동일 역할 교체를 취소하거나 새 파일의 고정 양식 검증·판독이 실패했을 때, then 기존 파일과 편집 상태를 그대로 유지한다. 검증 전에 편집을 지우거나 파일만 먼저 바꾸는 부분 성공을 허용하지 않는다.
- Given 일부 역할만 업로드됐을 때, then 현재 파일만으로 처리 가능한 승인 편집은 허용하고 없는 역할이 필요한 동작만 사유와 함께 제한한다. FE 제한을 우회한 요청도 BE가 검증하며, 제한 명령이 포함된 요청은 전체 실패/직전 성공 상태 유지로 처리한다. 입력 성공/연결 계산 보류나 임의 대체값으로 처리하지 않는다.
- Given 필요한 역할 파일이 추가됐을 때, then 초안의 고정 양식 정책/준비도를 다시 평가해 가능한 동작을 허용한다. 파일 추가만으로 미지원 동작을 활성화하거나 네 역할 미완성 상태의 최종저장을 허용하지 않는다.
- Given 허용된 서로 다른 초안A/B가 있을 때, then 각 초안의 파일·편집·고정 템플릿 버전을 독립적으로 유지하고 역할 파일을 합쳐 최종저장 준비로 판단하지 않는다. 이 격리 기준은 동일 사용자의 복수 초안 허용을 확정하는 의미가 아니다.
- Given 실제 형식과 고정 양식의 역할 구조가 유효한.xlsm 또는.xlsx 단일 파일일 때, then 두 형식을 모두 업로드 검증·조회/편집 지원 대상으로 처리한다. .xlsm 기준본과 확장자가 다르다는 이유만으로.xlsx를 거절하지 않는다. Given 확장자만 바꾸었거나 실제 형식/고정 양식 구조가 맞지 않는 파일일 때, then 실패하고 기존 초안/파일·편집 상태를 유지한다.
- Given 역할별 원본이.xlsx/.xlsm으로 섞인 완성 초안을 최종저장할 때, then 각 파일의 원본 형식(.xlsx→.xlsx,.xlsm→.xlsm)을 유지하고 실제 package·확장자·MIME을 일치시킨다. 승인 변경/계산 및 원본 보존을 검증하며 하나라도 실패하면 부분 Case/파일 set을 성공으로 확정하지 않는다.
- Given 파일 크기가52,428,800바이트일 때, then 용량 조건은 통과하고 나머지 형식/양식 조건을 검증한다. Given52,428,801바이트 이상일 때, then 초안 생성/파일 추가·교체·기존 편집 초기화 없이 거절한다. 네 파일의 합계와 multipart 부가 데이터 크기를 파일당 상한으로 오인하지 않는다. FE 검사를 우회해도 서버가 검증한다.
- Given 일부 역할만 검증·연결됐을 때, then 최종저장은 FE/BE 모두 차단한다. Given 네 역할이 각각 준비됐을 때, then 기존 최종저장의 추가 검증을 거쳐 새 Case 저장을 진행할 수 있다. 동일 역할 파일4개를 완성으로 판단하지 않는다.
- Given 업로드한 파일을 다른 표로 전환할 때, then 동일 업로드 inputId/양식 기준의 값과 control을 조회하며 기준본 데이터로 대체하지 않는다. 구체 후속 request/수명은 구현 전에 확정한다.
- Given control 없는 일반 표를 선택할 때, then 기존 grid 표현과 `controls: []`를 반환한다.
- Given 표 위에 연결된 select control이 있을 때, then 해당 grid와 control 현재 값/선택지/binding을 반환하며 구조의 controlIds 및 sourceTableId와 일치한다. 연결되지 않은 control과 다른 표의 grid 값은 포함하지 않는다.
- Given control 전용 영역을 선택할 때, then grid 배열들은 `[]`이고 controls는 제공하며 가짜 표 행을 만들지 않는다.
- Given 여러 입력 영역이 동일 모드를 공유할 때, then 동일 control ID/소유 영역/binding을 사용한다. 동일 source에서 저장된 값은 일치하며 원본 셀 값을 rows와 controls에 중복하지 않는다.
- Given control 값이 비었거나 선택지에 없는 기존 값일 때, then null 또는 기존 값을 그대로 반환하며 첫 선택지로 자동 대체하지 않는다. editable은 catalog 정책을 따른다.
- Given active `INDEPENDENT | MI_BASE` case와 완전한 input 4개가 있을 때, when 허용된 사용자가 구조를 조회하면, then 같은 source의 네 파일 전체 표시 구조를 반환하며 각 표의 실제 값은 포함하지 않는다.
- Given 표시 구조 안의 입력 영역 하나를 선택했을 때, when 값을 조회하면, then 해당 grid와 명시적으로 연결된 부속 control만 반환하며 다른 표의 grid나 다른 시트/파일의 값은 포함하지 않는다.
- Given 선택 표에 읽기 전용 값과 수정 가능 값이 함께 있을 때, then 둘 다 표 표시 범위에 포함할 수 있으며 수정 가능 정보를 별도로 구분한다.
- Given 선택 표가 다중 헤더 또는 빈 편집 셀을 가질 때, then 표 본문 밖의 필요한 헤더·라벨과 빈 편집 위치를 유지한다.
- Given 전체 구조와 표 값이 함께 반환될 때, then `sourceKey/sections`는 기존 FE 필드 이름을 유지하고 `sheets[].data`는 없으며 `tableData.tableId`가 구조 안의 유일한 표 ID와 일치한다.
- Given 선택 표의 열이 BB부터 시작하고 헤더가 3행일 때, then `column53` 등 원본 열 번호와 `rowNumber=3/id=2`를 유지하며 A/1부터 재번호화하지 않는다. 헤더는 rows에 포함한다.
- Given 수정/안내 항목이 없을 때, then `inputCells/inputGuideRules`는 `[]`이며 셀 색/개발 안내 sheet만으로 입력 권한을 추정하지 않는다.
- Given 역할이 누락·중복된 case일 때, then 불완전한 세트를 정상 결과로 반환하지 않는다.
- Given `SCENARIO` 또는 soft-deleted case일 때, then `404 RSC_NOT_FOUND`를 반환한다.
- Given 기존 3파일 case가 cutover cleanup으로 soft delete됐을 때, then 목록에 노출되지 않고 input 조회도 `404 RSC_NOT_FOUND`를 반환한다.
- Given 운영 template을 등록할 때, then `SUMMARY`와 `플랫폼 입력 안내`는 runtime sheet 목록과 구조 hash에 포함되지 않는다.
- Given 네 역할과 승인 업무 구조가 호환되는 파일에 `SUMMARY` 또는 `플랫폼 입력 안내`가 남아 있을 때, then 두 시트의 존재나 기준 파일과 다른 내용 hash만으로 Case 연결을 거부하지 않고 조회 응답에서는 두 시트를 제외한다. 다른 접근·파일 유효성·판독 조건은 유지한다(domain 규칙39~40).
- Given 운영 source snapshot에 hidden sheet가 있을 때, when 조회 projection을 만들면, then hidden sheet의 table/cell은 응답과 계산 대상에 포함하지 않는다.
- Given Draft를 최종저장할 때, then source와 결과의 hidden sheet 집합·순서·이름·숨김 상태가 같고 hidden worksheet part는 원본 그대로 보존된다.
- Given read-only 표시 sheet인 SUPPLY `LNG Summary` 1개를 조회할 때, then source snapshot 값은 반환하지만 수정 가능 field는 없고 Draft·Backend 재계산 대상에도 포함되지 않는다. TRADE `backupLNG_TradeRestrictionInput`은 화면 구조·선택 표 값 응답에 표시하지 않고 최종 XLSM에는 원본 보존한다(domain 규칙 43·57).
- Given 구분 탭 4개가 source snapshot에 있을 때, when 조회 projection을 만들면, then 응답에 포함하지 않고 최종저장 결과에는 원본 그대로 보존한다.
- Given TRADE `4.5 PriceModelInput`의 T-183 또는 T-184를 조회할 때, then domain 규칙 44의 입력 셀은 수정 가능 대상으로 제공하고 고정 제목·헤더는 조회만 제공한다. 규칙 57의 미표시 T-177~T-182와 U열은 응답에서 제외한다.
- Given 해당 Trade Draft에 승인 입력 편집·아래 행 추가가 있을 때, when 최종저장하면, then domain 규칙 44에 따라 해당 표의 변경을 반영하고 열/기간은 고정한다. 다른 표의 범위·값을 함께 변경하지 않으며 T-177~T-182 및 승인 변경 밖 원본 영역은 보존한다. 상세 write 계약·실파일 검증 완료를 이 수용 기준 정의로 대신하지 않는다.
- 공개 요청이 `fileNo`나 object key를 받지 않으므로 다른 case의 파일을 선택할 수 없다.
- 구조화 데이터 응답에는 원본 Excel binary·Base64·object storage 경로를 포함하지 않는다.
- 신규 업로드의 status·실패 보존/재시도 acceptance criteria는 위 확정 계약을 따른다. 다른 미확정 전달 방식의 잔여 기준은 해당 계약 확정 후 추가한다. QRY-01/02의 JSON DRM 비적용은 §5의 확정 사항을 따른다.

## 10. Scope Boundary

### Included

- `caseId` 요청 식별 경계
- `DEMAND`, `SUPPLY`, `TRADE`, `OUTPUT` 역할 검증
- 네 파일 전체 화면 구조와 선택 표 하나의 값이라는 응답 범위
- 파일명 역할 판별·ACTIVE 양식 검증·단일 파일 순차 누적과 조회 공통 표현 및 편집/최종저장 진입 연결
- 응답 크기·snapshot·부분 실패 결정과 §5의 JSON DRM 비적용 경계
- 확정 후 endpoint와 request/response/error/OpenAPI 계약

### Excluded

- 업로드 초안 목록 조회와 목록에서 찾아 선택/재개하는 기능 — 사용자 현재 범위 제외. 기존 저장 Case의 Draft 생성/재개 계약은 유지

- MI Base 목록·상세·지정 API
- 원본 input 파일의 ZIP·역할별 다운로드 API
- 편집 명령/임시저장·4파일 최종저장 write 계약 자체의 중복 정의 — 기존 편집 API가 소유하며, 이 문서는 새 업로드 진입과의 연결 조건만 소유
- output/결과 파일 다운로드
- 범용 Excel-to-JSON 변환 API

## 11. Implementation Readiness

- status: 직접 업로드는 §3.3의 확정 계약으로 구현/격리 검증; 저장 Case 조회의 나머지 범위는 별도 정본/미결을 따른다.
- implementation: 직접 업로드의 성공/실패·취소·공통 편집/최종저장 연결은 §3.3으로 확정했다. 실제 지원 편집은 공통 EDT 처리기의 현재 지원 범위를 따르며 범용 처리기 구현 완료와 구분한다. 이외 저장 Case QRY의 미확정 범위는 해당 정본을 따른다.
- next action: 직접 업로드 배포는 DDL §6.10.4의 설정/양식 발행/복구 디렉터리·실행 위치 확인 후 진행한다. 실제 DB/운영 저장소 검증과 대용량 성능을 격리 테스트 통과로 대신하지 않는다.

## 12. Change Log

- 2026-10-19 업로드 처리120초 초기 채택: 사용자가 일단120초로 결정했다. 파일 수신 후 접수부터 대기열·검증/파싱·성공 확정까지 포함하고 전송 시간은 제외한다. TIMEOUT/실제 종료·저장 확인·늦은 성공 차단과 기존 성공 상태 보존을 명시했다. 실측/경계 검증과 worker 구성은 후속이며 아래 시간 상한 미승인은 채택 전 이력이다.

- 2026-10-19 취소 후 새 첫 파일 실패 후속: 사용자가 A 취소 상태 유지·B 첫 파일 실패 시 다시 업로드를 확정했다. 취소와 새 최초 업로드를 분리하고 B 실패는 초안/Case 미생성·A 비복구·새 requestId/최초 baseRevision 생략으로 정리했다. 아래 취소/새 파일의 결합·복구 미결은 과거 이력이며 새 복구 기능을 요구하지 않는다.

- 2026-10-19 미완료 작성 취소/새 시작 후속: 사용자1안 채택으로 대기/처리 작업이 없고 취소 안내/확인 후에만 새 Case로 전환하도록 했다. 확인 전·철회/취소 처리 실패의 상태 보존·서버 진행 상태 재검증·취소 후 기존 쓰기 차단을 명시했다. 취소/새 파일 업로드의 분리·결합 API와 실패 확정 경계는 아직 미결이며 아래 취소 절차 미승인 문구는 과거 이력이다.

- 2026-10-19 직접 업로드 단일 작성 후속: 사용자가 직접 업로드 초안은 한 번에 Case 하나 작성으로 확정했다. 같은 사용자의 최초 비동기 검증 전후에 두 번째 작성을 만들지 않고 최종저장 성공 후 다음 작성을 허용한다. 다른 사용자/출처 Draft는 제외하며 미완료 작업의 취소/새 시작은 별도 미결이다. 아래 복수 허용 및 개수 재검토는 과거 이력이다.

- 2026-10-19 초안 범위 재논의: 사용자 지시로 업로드 초안 목록/선택 재개를 제외하고 동일 사용자 복수 초안 허용의 기존 결정을 재검토로 전환했다. 초안은 Case 하나의 파일 묶음/편집 상태임을 명시하고 단일 제한도 아직 채택하지 않았다. 아래 복수 허용 확정/목록 미결은 과거 이력이며 현재 개수 계약으로 사용하지 않는다.

- 2026-10-19 교체 영향 범위 정정: 사용자가 파일 교체는 다른 파일의 표에 영향을 주지 않는다고 확인했다. 교체 범위를 해당 파일/편집으로 한정하고 다른 역할 표/입력/계산 결과 불변을 명시했다. 파일 간 연결 계산·이를 이유로 한 교체 거절/새 초안 요구의 제안과 미결을 제거했다. 아래 과거 교체 연결 영향 미결은 이 정정 전 이력이며 새로운 계산 요구로 사용하지 않는다.

- 2026-10-19 업로드 FAILED 후 재시도 후속: 사용자 추천안 채택으로 같은 requestId/동일 내용의 실패 재전송은 기존 HTTP200/FAILED/failure만 반환하고, 재시도는 새 requestId·후속 현재 baseRevision/교체 확인으로 별도 접수한다. 최초 실패에는 아직 초안/baseRevision이 없다는 점과 통신 오류를 FAILED로 간주하지 않는 경계를 구분했다. 아래 실패 후 재시도 미승인 표현은 채택 전 이력이다.

- 2026-10-19 초안 파일 준비 상태 후속: 사용자 추천안 채택으로 uploadedKinds/missingKinds/canFinalize의 서버 계산·초안 현재 상태 응답과 업로드 성공 결과 연결을 확정했다. 신규 처리 파일 제외·교체 중 기존 유효 파일 유지/최종저장 불가·동일 시점 평가와 과거 결과/현재 상태 구분을 명시했다. 아래 파일 준비 상태 필드 미승인 표현은 채택 전 이력이다.

- 2026-10-19 업로드 오류 상세 후속: 사용자1안 채택으로 failure.reason/currentRevision/issues와 FILE/SHEET/TABLE/HEADER 수준의 업로드 문제 위치·대표 사유/실패 JSON을 확정했다. 실패 외 상태의 failure:null, 해당 없는 위치 null, 판독 불가의 issues:[]와 원본 값/내부 정보 미노출을 구분했다. 접수 전 오류 HTTP/ResultCode와 추가 매핑은 잔여이며 아래 오류 상세 미승인 표현은 채택 전 이력이다.

- 2026-10-19 업로드 상태 조회 후속: 사용자 추천안 채택으로 접수202·operationId/status/statusEndpoint·공통 상태 GET200·2초 조회와 SUCCEEDED/FAILED 종료를 확정했다. 최초 성공 전 draftId 없이 조회하며 FAILED 조회의 HTTP 성공/실제 처리 실패와 허용된 재전송의 기존 결과 반환을 구분했다. 상세 결과/오류·worker/자원/시간 상한은 잔여이며 아래 응답/조회 미승인 표현은 채택 전 이력이다.

- 2026-10-19 multipart 전달 형식 후속: 사용자1안 채택으로 file binary/metadata application/json object의 두 part와 최초/후속 요청의 항목 위치·타입/필수 조건·대표 JSON3개를 확정했다. sourceKey/requestId/baseRevision/replaceConfirmed를 metadata에 묶고 draftId는 후속 경로를 유지한다. 아래 multipart 직렬화 미결 표현은 이 결정 전 이력이며 응답/상태 조회·오류/저장은 후속이다.

- 2026-10-19 업로드 최초 표시 후속: 사용자2안 채택으로 비동기 업로드 성공 시 해당 역할의 첫 표시 표를 자동 선택하고 구조/표 값을 함께 제공한다. 표시 시트/표 순서·화면 제외 정책과 기존 tableData 표현을 재사용하며 기존 저장 Case QRY-01/02의 미선택/조회 분리는 유지한다. 아래 업로드 최초 선택 미결/미선택 추천은 이 결정 전 이력이다.

- 2026-10-19 교체 확인 필드 후속: 사용자1안 채택으로 replaceConfirmed:true를 확정하고 기존 역할 파일이 있을 때 생략/false면 교체 접수를 거절하도록 했다. 권한/baseRevision/진행 제한·새 파일 검증과 실패 시 상태 보존을 유지하며 새 역할 파일 추가에는 교체 확인을 요구하지 않는다. 구체 multipart 직렬화/오류는 잔여이며 아래 교체 확인 필드 미결은 채택 전 이력이다.

- 2026-10-19 후속 파일 revision 후속: 사용자1안 채택으로 파일 추가·교체의 baseRevision 필수 검증·불일치 거절·성공 revision1회 증가/실패 유지를 확정했다. 접수/진행 중 증가나 성공 결과 재확인에 의한 중복 증가를 허용하지 않으며 파일/편집과 revision을 함께 확정한다. 구체 DTO/오류·교체 확인 필드는 잔여이며 아래 업로드 revision 미승인 표현은 채택 전 이력이다.

- 2026-10-19 초안별 변경1건 후속: 사용자1안 채택으로 같은 초안의 파일 추가·교체/편집 저장/최종저장을 공통 진행 제한으로 묶었다. 대기/처리 중 새 변경은 거절하고 기존 성공 상태/결과 조회·동일 요청 재전송과 다른 초안의 독립 진행을 유지한다. 후속 파일 요청의 revision·구체 충돌/복구는 잔여이며 아래 업로드 경합 미승인 표현은 채택 전 이력이다.

- 2026-10-19 업로드 비동기 후속: 사용자1안 채택으로 파일 수신 뒤 접수/상태 확인과 검증·파싱 성공 확정을 분리했다. 최초 성공 후 초안 생성, 후속 성공 후 연결·교체/편집 초기화, 실패 시 기존 상태 보존을 유지한다. 최초 접수 응답에서 draftId나 구조 데이터를 미리 반환하는 것으로 읽히는 표현을 정리했다. 상태 조회/DTO·경합·자원/시간 상한은 잔여이며 아래 비동기 미승인 표현은 채택 전 이력이다.

- 2026-10-19 최초 업로드 재전송 후속: 사용자1안 채택으로 §3.3에 같은 requestId·동일 파일/요청의 기존 초안·결과 반환과 다른 내용의 충돌 거절을 확정했다. 실제 바이트 비교·인증 경계·동시 중복 생성 방지·후속 편집/고정 버전 보존을 명확히 했다. 전송 필드/타입·판정/키 저장·처리 중 응답·HTTP와 후속 업로드 경합은 잔여이며 아래 재전송 미결 표현은 이 채택 전 이력이다.

- 2026-10-19 API 분리 후속: 사용자1안 채택으로 §3.3의 최초 초안 생성/기존 초안 파일 추가·교체 두 POST 경로를 확정했다. 후속은 경로 draftId로만 대상을 지정하고 대상 부재/접근 불가를 새 초안 생성으로 대체하지 않는다. 기존 단일 경로 후보와 경로 미승인 문구를 정리했으며 DTO/HTTP·재전송/동시성·저장은 잔여다. 아래 단일 POST /input-uploads 표기는 과거 이력이다.

- 2026-10-19 파일 용량 후속: 사용자1안 채택으로 파일당50MiB(52,428,800바이트)를 확정했다. 각 multipart file part에 적용하고 정확한 상한은 허용,1바이트 초과부터 거절한다. 초과 시 초안/파일·편집 상태를 변경하지 않는다. 전역 요청 상한·누적 합계·압축 해제/처리 자원 제한과 구분하며 구체 오류 표현과 실제 구현/경계값 검증은 후속이다.

- 2026-10-19 최종 형식 후속: 사용자1안 채택으로.xlsx는.xlsx로,.xlsm은.xlsm으로 최종저장한다. 혼합 형식4역할도 각 형식을 유지하며 확장자/MIME/실제 package 일치를 요구한다. 기존 XLSM 고정 코드의 보완·실제 형식별 생성/Case·Run 연결 검증은 후속이며 아래 과거 최종 형식 미결은 이 결정 전 이력이다.

- 2026-10-19 파일 형식 후속: 사용자2안 채택으로.xlsm/.xlsx 업로드 지원을 확정했다. 실제 형식/고정 양식 구조를 검증하고.xlsm 기준본과의 확장자 동일성을.xlsx 수용 조건으로 두지 않는다. 최종저장 형식 유지/변환과 실제.xlsx 제품 검증은 잔여이며 아래 과거 형식 미결은 이 결정 전 이력이다.

- 2026-10-19 복수 초안 후속: 사용자1안 채택으로 복수 업로드 초안과 각 초안의 파일·편집·고정 템플릿 버전 독립성을 확정했다. 사용자/역할/파일명에 따른 자동 병합·다른 초안 상태 변경을 금지하며 구체 식별/목록/재개·저장은 후속이다. 아래 과거 초안 개수 미결은 이 결정 전 이력이다.

- 2026-10-19 부분 편집 후속: 사용자1안 채택으로 필요한 역할 파일이 없는 동작만 제한하고 현재 파일로 처리 가능한 승인 편집은 유지한다. FE 안내와 BE 검증·요청 전체 실패/기존 상태 보존을 연결했으며 입력 성공/연결 반영 보류는 채택하지 않았다. 구체 필요 역할/사유·오류 표현과 연결 영향 검증은 후속이다.

- 2026-10-19 동일 역할 교체 후속: 사용자1안 채택으로 편집 초기화 안내·확인 뒤 새 파일 검증 성공 시 해당 역할을 교체하도록 확정했다. 취소/검증 실패 시 기존 파일/편집을 보존하고 다른 역할·고정 양식 버전은 유지한다. 구체 확인 요청·확정/경합·연결표 영향 검증은 후속이며 아래 과거 교체 미결은 이 결정 전 이력이다.

- 2026-10-19 양식 버전 후속: 사용자1안 채택으로 첫 정상 파일의 ACTIVE 버전을 초안에 고정하고 후속 업로드·조회·편집·최종저장에 유지한다. 진행 중 ACTIVE 교체는 기존 초안을 전환하지 않으며 새 초안부터 새 ACTIVE를 사용한다. 아래 과거 양식 전환 미결은 이 결정 전 이력이다.

- 2026-10-19 생성 시점 후속: 사용자1안 채택으로 첫 정상 파일에서 업로드 편집 초안을 만들고 후속 파일을 같은 초안에 모으며, 실제 Case는4역할 최종저장 성공 시 생성하도록 확정했다. 생성 시점 미결은 해소했고 초안 식별/물리 연결·양식 전환·편집 상세는 잔여다. 아래 최초 요구 반영의 생성 시점 미확정은 이 결정 전 이력이다.

- 2026-10-19: 사용자 추가 요구로 §3.3을 파일명 역할 판별·ACTIVE 양식 검증·단일 파일 누적과 새 Case 편집 진입으로 확장했다. 과거 필수 kind와 Draft/Case 생성 일괄 제외를 해제하고 불일치 시 생성/연결 금지·이전 성공 보존·4역할 최종저장 조건을 반영했다. 생성 시점/묶음 lifecycle과 구체 API·물리 저장은 미확정이며 제품 구현 완료가 아니다.

- 2026-10-16: 기존 QRY 확정 범위의 소유 문서와 새 공통 편집 API Draft를 연결했다. 기존 조회/업로드 endpoint·필드·오류는 변경하지 않았으며 최종저장 상세/제품 구현·INPUT-03/04 전체 완료로 승격하지 않았다.

- 2026-10-15: 사용자 확인으로 QRY-01/02의 엑셀 추출 표시 JSON에 회사 DRM을 적용하지 않음을 §5에 확정했다. 해당 미결정/승인 선행조건을 제거했으며 HTTPS·인증/권한·본문 로깅 제외와 기존 파일 암복호화는 유지한다. 제품 보안 검증 완료나 티켓 상태 변경을 뜻하지 않는다.
- 2026-10-15: 사용자 승인으로 저장 Case 숫자 표시 정책을 §3.2에 채택했다. 회계 배치 공백만 제외하고 General은 US 기준·열 폭 축약 제외·승인 표시 범위 한정으로 제공한다. 표시 규칙은 §3.2가 소유하며 기존 판독/실패 계약·응답 구조·제품 코드·INPUT 상태는 변경하지 않는다.
- 2026-10-13: domain 규칙 36에 3파일 `SCENARIO` 정리가 추가돼, §2의 "case master만 변경" 문구가 `INDEPENDENT | MI_BASE` 정리에만 해당함을 명시했다. 이 문서의 조회 계약은 바꾸지 않았다.
- 2026-10-13: 미커밋 문서 정합성 검토에서 아래 세 변경의 Change Log 누락을 보완했다. 본문 계약은 바꾸지 않았다.
- 2026-10-08: domain 규칙 44에 맞춰 TRADE `4.5 PriceModelInput`의 시트 전체 read-only·최종 XLSM 미변경 계약을 T-183/T-184 입력 편집·아래 행 추가 제공으로 바꿨다(§2, §9). 미표시 T-177~T-182와 U열의 화면 제외는 유지한다.
- 2026-10-08: 사용자 확인대로 read-only 표시 수용 기준을 SUPPLY `LNG Summary` 1개로 정렬했다. TRADE `backupLNG_TradeRestrictionInput`은 미표시·원본 보존하며 기존 조회 계약의 필드/오류는 변경하지 않는다.
- 2026-10-07: domain 규칙 57에 따라 최신 SUMMARY에서 표 전체가 미표시로 확정된 표를 조회 응답에서 제외했다(§2). 표 일부 열의 미표시와는 구분한다.
- 2026-10-07: 사용자 확인으로 LiqUtilAdjustment 내부 참조 수식 범위를 `G7:CL7`에서 `H7:CL7`로 정정했다. `G7`은 시작 연도 값이며 고정 헤더 정책을 유지한다(§3.1C).
- 2026-10-07: 판별대장 T-020과 domain 규칙 50에 맞춰 LNG Demand 모드 open question을 정리했다. 값 셀 C1·선택값 변경 허용·T-021과 함께 표시는 확인된 사실로 두고, 원본 선택지 `Dynamaic` 오타 처리와 options 표기만 미결정으로 남겼다.
- 2026-10-07: 이번 release에 구조 변경을 포함하는 domain 결정에 조회 계약을 맞췄다. 마지막 행·열 추가 중심 원칙과 기존 중간 삽입·삭제 예외를 참조하며, 구체 write 계약은 미확정 상태로 유지한다.
- 2026-10-06: 사용자 전달 FE 합의로 수식 결과값과 별도 화면 갱신 규칙 전달을 분리했다. 입력/열 추가 시 FE가 규칙으로 표시값을 갱신하고, 기존 BE write/파일 생성 책임은 유지한다. 규칙 JSON 표현과 G7:CL7의 실제 계산식은 미결정으로 남겼다.
- 2026-10-06 최종: 사용자 승인으로 기존 ProductionAdjustment L8:L200 수식 보존·신규 행 L셀 빈칸·화면/재계산 제외 유지로 확정했다. 앞선 신규 L수식 복사/행 참조 변경 및 오류 확장 예외를 대체했다.
- 2026-10-06 중간 결정(위 최종 결정으로 대체): ProductionAdjustment를 C 구조변경으로 명시하고 필요한 신규 L수식의 행 참조 변경 복사와 오류 보존 예외를 허용했었다.
- 2026-10-06: 사용자 확인에 따라 ProductionAdjustment 행 추가 가능과 L7:L200 화면 제외를 명시했다. 헤더 L7과 기존 오류 수식 L8:L200을 구분하고 원본 보존/재계산 제외 규칙은 domain을 참조한다.
- 2026-10-01: 사용자 결정으로 POST /input-uploads의 kind별 저장 포맷 파싱과 조회 공통 응답 표현을 추가했다. FE의 단일 파일 multipart/해당 kind section 제안은 Draft 기준으로 재사용하고 whole-sheet 값 반환은 제외했다. 양식 버전 고정·구조 검증과 업로드/저장 case 경계를 구분했으며 보존/수명·후속 조회/최초 선택·오류 상세는 미정이다.
- 2026-10-01: 사용자 지적의 LNG Demand 모드 선택 예외를 반영했다. table을 선택 가능한 논리 입력 영역으로 정의하고 `tableData.controls`와 구조의 `controlIds`를 추가했다. 기존 ID/grid 표현을 유지하며 공유 control의 명시적 연결·원본 binding·값 중복 방지와 control 전용 영역을 규정했다. 실제 LNG 값 셀/선택지/공유 대상은 미정이다.
- 2026-10-01: 사용자 승인에 따라 기존 FE의 `sourceKey`, `sections → sheets → tables`, `columns/rows/inputCells/inputGuideRules` 필드 이름과 원본 행/열 기반 표현을 재사용한다. `sheets[].data`를 제거하고 선택 표의 값을 `tableData`에 배치했다. 별도 `headers/editingInfo`를 추가하지 않고 표 ID·source 기준의 대응 관계와 예시/수용 기준을 정리했다.
- 2026-10-01: 사용자 결정에 따라 화면 `파일 → 시트 → 테이블` 구조 정보는 네 파일 전체, 실제 헤더·값은 선택 표 하나만 제공하도록 응답 범위를 확정했다. 전체 snapshot/role/sheet 값 일괄 반환 후보를 제거하고 API 호출 구성은 미정으로 유지했다. 표 단위 제공과 일부 조회 field 선별/수정 허용 범위를 혼동하지 않도록 문구를 정리했다.
- 2026-09-10: MI Base 지정 계약과 분리해 독립 Draft를 생성했다. 기존 문서의 고정 request identity, 전달 후보, 보안 경계, 미결정 사항을 이동했다.
- 2026-09-14: 2026-09-08 회의의 table projection 방향에 맞춰 원본 파일 응답 제외를 고정하고 ZIP·역할별 다운로드 fallback 후보를 제거했다. BE가 제출 변경분을 원본 XLSM 복제본에 반영하는 내부 생성 흐름은 유지한다.
- 2026-09-16: 화면 계약을 sheet/table projection으로 고정하고, 카탈로그의 편집·구조 변경·선택 후보·영향 관계 소유권을 명시했다. 조회 계약과 `임시저장`/`최종저장` write 계약을 분리했다.
- 2026-09-16: `INPUT-01`의 화면 후보/승인 경계를 반영하고, `INPUT-02`의 POI 전체 로딩 `NO-GO`에 따라 조회 구현 후보를 streaming 방식으로 제한했다.
- 2026-09-28: 사용자 확인에 따라 `OUTPUT`을 기존 세 역할과 같은 필수 input으로 추가하고 4파일 snapshot·완전성 계약으로 갱신했다. 이 시점에는 기존 3파일 case 호환 정책을 open question으로 남겼다.
- 2026-09-28: 후속 사용자 결정에 따라 기존 3파일 `INDEPENDENT | MI_BASE` case는 `OUTPUT` 보완 없이 cutover 전에 soft delete하고 화면·active API에서 제외한다. 연결 파일·실행이력·결과는 물리 삭제하지 않는다.
- 2026-09-28: 당시 개발 시트를 runtime catalog·응답·Draft·최종 XLSM·Job input에서 제외하고 정규화본을 운영 기준으로 사용했다. 파일 수용·원본 보존 조건은 아래 2026-10-19 결정으로 대체됐다.
- 2026-10-19: 사용자 승인에 따라 domain 규칙39~40의 개발 시트 포함 파일 수용을 반영했다. 조회 응답 제외는 유지하며 파일 수용·보존의 정본은 해당 domain 규칙이다.
- 2026-09-28: 사용자 확인에 따라 운영 hidden sheet는 다른 시트 계산과 무관한 원본 보존 영역으로 확정했다. 조회·Draft·dependency·Backend 계산에서는 제외하고 최종 XLSM에는 source snapshot 그대로 복사한다.
- 2026-09-28: `직접 입력 대상 아님.`인 visible sheet 중 수식·직접 참조가 없는 6개를 read-only 표시 대상으로 확정했다. source 값은 조회하되 write·Draft·dependency·Backend 재계산에서 제외하고 최종 XLSM에는 보존한다.
- 2026-10-01: 사용자 확인에 따라 위 6개 중 값·수식이 없는 구분 탭 4개(DEMAND `Input Adjustment ->`, `기준정보 ->`, SUPPLY `Input Upstream ->`, `Input Midstream ->`)를 조회 응답에서 제외했다. read-only 표시는 SUPPLY `LNG Summary`, TRADE `backupLNG_TradeRestrictionInput` 2개로 줄었고, 6개 모두 계산 제외·최종 XLSM 원본 보존은 그대로다.
