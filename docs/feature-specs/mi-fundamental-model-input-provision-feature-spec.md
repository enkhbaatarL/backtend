# MI Fundamental Model Input 제공 Feature Spec

## Document Control

- status: Draft
- canonical for: MI Fundamental Model input 내용 제공 API의 고정된 요청 식별 경계와 검토 중인 전달 계약
- owner: Shared (BE / FE / Security·DRM)
- source documents:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
- depends on:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `SECURITY.md`
- supersedes:
  - `docs/feature-specs/mi-fundamental-model-mi-base-designation-feature-spec.md`의 기존 `API Slice C — Input 내용 제공` 초안
- last synced: 2026-09-14
- open questions:
  - 구조화 데이터(JSON)가 조직의 DRM 적용 대상에서 제외되는지 Security·DRM 담당자의 확인이 필요하다.
  - FE가 필요한 정보가 업무 필드 데이터인지, sheet/cell 좌표·값·수식까지 포함한 workbook projection인지 확정해야 한다.
  - 응답 크기 상한, paging/분할 조회, 3개 role의 일관된 snapshot과 부분 실패 처리를 확정해야 한다.
  - endpoint path/method, request/response DTO, version 표현을 확정해야 한다.

## 1. Feature Goal

선택한 Fundamental Model `caseId`의 `DEMAND`, `SUPPLY`, `TRADE` input 내용을 FE가 조회할 수 있는 계약을 정의한다. 원본 파일은 FE 응답으로 전달하지 않으며, 구조화 범위는 DRM 정책과 FE 사용 흐름을 확인한 뒤 확정한다.

이 문서는 MI Base 목록·상세·지정 API를 정의하지 않는다. 해당 계약은 [MI Fundamental Model MI Base 지정 Feature Spec](mi-fundamental-model-mi-base-designation-feature-spec.md)이 소유한다.

## 2. Fixed Decisions

- input 원본 3개의 소유 단위와 요청 대상 식별자는 `caseId`다.
- `runId`, `execType`, `fileNo`, bucket, object key는 대상 실행 건을 선택하는 공개 요청 식별자로 사용하지 않는다.
- 서버가 `caseId + SCNRCASE relation`으로 연결된 원본을 선택한다.
- 원본 Excel bytes를 Base64로 JSON에 넣는 방식은 비파일 전달로 보지 않으며 후보에서 제외한다.
- object storage 경로, bucket, object key, 원본 binary를 응답에 포함하지 않으며 ZIP 또는 역할별 원본 파일 다운로드 API를 제공하지 않는다.
- endpoint와 response shape은 아직 확정하지 않는다.

## 3. Contract Candidates

| 안 | 흐름 | 장점 | 확인할 점 |
|---|---|---|---|
| A. 업무 데이터 JSON | `caseId` → role별 업무 필드·행 데이터 | FE가 바로 표시·편집 가능하고 계약을 업무 의미로 제한 | canonical field mapping, 양식 version, 응답 크기 |
| B. cell projection JSON | `caseId` → role/sheet/cell address/value/type | 원본 위치와 향후 영향 셀 표시를 유지 | 수식·매크로·스타일 범위, 대용량 payload, FE 편집 모델 |
| C. role별 분할 JSON | metadata/summary 후 role 또는 sheet 단위 조회 | payload 크기와 실패 범위를 분할 | API 호출 수, 3개 role snapshot, paging/version |

전달 계약 후보는 A~C의 구조화 JSON이다.

## 4. Server-side Resolution and Validation

1. `caseId`로 `del_yn = 'N'`인 active case를 조회한다.
2. 허용 `execType`은 `INDEPENDENT | MI_BASE`다. `SCENARIO`는 active resource hiding 규칙으로 `404 RSC_NOT_FOUND`를 반환한다.
3. `tcm_filel`에서 `del_yn = 'N' AND file_rel_obj_spt_cd = 'SCNRCASE' AND file_rel_obj_dstg_no = :caseId`인 파일을 조회한다.
4. 파일명의 `DEMAND`, `SUPPLY`, `TRADE` 키워드를 대소문자 무관으로 검사해 각 역할을 정확히 1개씩 선택한다.
5. active 파일이 3개가 아니거나 역할이 누락·중복되면 불완전한 input으로 처리한다.
6. DB metadata와 실제 object storage 객체가 모두 존재하는지 확인한 뒤 허용된 데이터만 추출·변환한다.

## 5. Response and Access Boundary

- 구조화 데이터 안을 선택하면 `BaseResponse<T>`와 JSON을 사용한다.
- 구체적인 DTO는 FE 사용 목적을 확정한 뒤 정의한다.
- 최종 계약은 3개 role 전체 성공, role/sheet별 부분 조회, 재시도 UX 중 하나를 명시해야 한다.
- 호출 권한 후보는 기존 목록과 같은 `USER + SKGAS_COMPANY_USER_ONLY`, `ADMIN + THIRD_PARTY_ADMIN_ONLY`다.
- 현재 공유 조회 정책에서는 case 소유자 `usr_no`가 현재 사용자와 달라도 input 접근을 허용한다.
- 서버가 case와 연결된 정확한 파일을 선택하며 클라이언트가 file/storage 식별자를 지정하지 않는다.
- input request/response body는 대용량·업무 민감 데이터일 수 있으므로 HTTP 본문 로깅 대상에서 제외한다.
- DRM 비적용이 승인되더라도 HTTPS와 인증·role/condition 검사는 유지한다.

## 6. Representative Errors — Draft

| 조건 | HTTP | `ResultCode` | 의미 |
|---|---:|---|---|
| `caseId < 1` 또는 형식 오류 | 400 | `REQ_INVALID_PARAMETER` | 잘못된 case 식별자 |
| case 없음, soft delete, `SCENARIO` case | 404 | `RSC_NOT_FOUND` | input 제공 대상이 아님 |
| input 3개 미만/초과, 역할 누락·중복 | 409 | `BIZ_RULE_VIOLATION` | input 정합성 오류 |
| DB metadata는 있지만 저장 객체를 읽을 수 없음 | 500 | `SYS_INTERNAL_ERROR` | 저장소와 metadata 불일치 |
| 인증되지 않음 | 401 | 공통 인증 오류 | 로그인 필요 |
| 허용되지 않은 role/condition | 403 | 공통 권한 오류 | 접근 불가 |

오류 계약은 endpoint와 전달 방식을 정할 때 함께 확정한다. 내부 저장 경로나 provider 오류 원문은 응답에 노출하지 않는다.

## 7. OpenAPI Documentation Plan

- 계약 확정 후 operation을 정의한다.
- description에는 `caseId` 기준, `DEMAND/SUPPLY/TRADE` 3개, incomplete input 의미를 명시한다.
- JSON schema에는 role, dataset/sheet 구분, field/cell 값 타입·nullable·빈 목록 의미와 version을 기록한다.
- 분할안을 선택하면 summary와 실제 데이터 operation의 책임, paging/default/snapshot 의미를 분리한다.
- 대표 200/400/401/403/404/409/500 응답을 `@ApiResponses`로 구조화한다.

## 8. Data and Implementation Impact

- 기존 `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` 재사용을 우선한다.
- 서버가 선택한 원본을 object storage에서 읽고 Apache POI로 필요한 값만 추출하는 방식을 검토한다.
- workbook 전체를 그대로 DTO화하지 않고 FE 목적에 필요한 업무 필드 또는 cell projection만 계약한다.
- workbook 크기, sheet/row/cell 수, 응답 크기에 상한을 두고 stream/workbook을 항상 닫는다.
- 구체적인 상한은 실제 input 표본을 측정한 뒤 확정한다.

## 9. Acceptance Criteria — Common Draft

- Given active `INDEPENDENT | MI_BASE` case와 완전한 input 3개가 있을 때, when 허용된 사용자가 조회하면, then 같은 `caseId`의 세 역할 원본에서 추출한 계약 범위 데이터를 반환한다.
- Given 역할이 누락·중복된 case일 때, then 불완전한 세트를 정상 결과로 반환하지 않는다.
- Given `SCENARIO` 또는 soft-deleted case일 때, then `404 RSC_NOT_FOUND`를 반환한다.
- 공개 요청이 `fileNo`나 object key를 받지 않으므로 다른 case의 파일을 선택할 수 없다.
- 구조화 데이터 응답에는 원본 Excel binary·Base64·object storage 경로를 포함하지 않는다.
- 전달 방식별 status, DRM, 부분 실패와 재시도 acceptance criteria는 계약 확정 후 추가한다.

## 10. Scope Boundary

### Included

- `caseId` 요청 식별 경계
- `DEMAND`, `SUPPLY`, `TRADE` 역할 검증
- 구조화 JSON 후보 비교
- DRM·응답 크기·snapshot·부분 실패 결정
- 확정 후 endpoint와 request/response/error/OpenAPI 계약

### Excluded

- MI Base 목록·상세·지정 API
- 원본 input 파일의 ZIP·역할별 다운로드 API
- 편집 결과로 새 독립실행 case를 생성·실행하는 write API
- output/결과 파일 다운로드
- 범용 Excel-to-JSON 변환 API

## 11. Implementation Readiness

- status: `blocked by open questions`
- implementation: endpoint, response fidelity, DRM, 크기·분할·snapshot 계약 확정 전에는 착수하지 않는다.
- next decision: FE 사용 목적과 Security·DRM 정책을 먼저 확인하고 이 문서를 갱신한다.

## 12. Change Log

- 2026-09-10: MI Base 지정 계약과 분리해 독립 Draft를 생성했다. 기존 문서의 고정 request identity, 전달 후보, 보안 경계, 미결정 사항을 이동했다.
- 2026-09-14: 2026-09-08 회의의 table projection 방향에 맞춰 원본 파일 응답 제외를 고정하고 ZIP·역할별 다운로드 fallback 후보를 제거했다. BE가 제출 변경분을 원본 XLSM 복제본에 반영하는 내부 생성 흐름은 유지한다.
