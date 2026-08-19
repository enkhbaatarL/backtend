# 관리자 공지 이미지 업로드 최소 보안 계약

## Document Control

- status: Approved
- canonical for: API contract
- owner: BE
- source documents:
  - 관리자 공지 이미지 업로드 취약점 최소 조치 요청
  - 모의해킹 최초진단 결과보고서의 파일 업로드 보안 권고(외부 참고 자료, 27페이지)
- depends on:
  - `SECURITY.md`
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 없음
- last synced: 2026-08-19
- open questions:
  - 없음

## Scope

- Method: `POST`
- Path: `/api/v1/admin/bulletinboard-management/notice/upload-file`
- Request media type: `multipart/form-data`
- Multipart part: `file`
- 기존 성공 응답의 `BaseResponse<FileUploadRes>` 계약은 변경하지 않는다.

## Fixed Validation Rules

서버는 Object Storage 또는 DB 저장 전에 다음 순서로 파일을 검증한다.

1. `file`이 `null`이 아니고 비어 있지 않아야 한다.
2. 파일 크기는 `20 MiB`(`20 * 1024 * 1024` bytes) 이하여야 한다.
3. 마지막 파일 확장자는 대소문자를 정규화한 뒤 다음 중 하나여야 한다.
   - `.png`
   - `.jpg`
4. `.jpeg`, `.gif`, `.svg`, 실행 파일과 스크립트 파일 확장자는 허용하지 않는다.
5. 요청 Content-Type은 앞뒤 공백 제거와 대소문자 정규화 후 확장자에 맞는 값과 정확히 일치해야 한다.
   - `.png` → `image/png`
   - `.jpg` → `image/jpeg`
6. `image/jpeg; charset=...`처럼 parameter가 붙은 Content-Type은 허용하지 않는다.
7. 파일 시작 signature는 최대 8바이트만 읽어 확인한다.
   - PNG: `89 50 4E 47 0D 0A 1A 0A`
   - JPEG: `FF D8 FF`로 시작
8. 확장자, 정규화한 Content-Type, signature가 모두 같은 형식을 가리킬 때만 허용한다.

## Canonical Storage Values

검증 성공 결과는 서버가 다음 값을 결정한다.

| 형식 | canonical extension | canonical MIME |
| --- | --- | --- |
| PNG | `png` | `image/png` |
| JPEG | `jpg` | `image/jpeg` |

- 저장 파일명은 기존 UUID 기반 형식을 유지하고 canonical extension을 붙인다.
- Object Storage metadata와 DB의 MIME에는 canonical MIME을 저장한다.
- DB의 파일 확장자에는 canonical extension을 저장한다.
- 클라이언트가 보낸 Content-Type과 파일명 확장자를 저장값으로 직접 사용하지 않는다.

## Failure Contract

모든 검증 실패는 기존 `AppErrorException`, `ResultCode`, `BaseResponse` 처리 방식을 사용하며 HTTP `400`으로 응답한다.

| 실패 조건 | ResultCode |
| --- | --- |
| `file` part가 `null` | `REQ_INVALID_PARAMETER` (`REQ-003`) |
| 파일명 또는 확장자가 없음 | `REQ_INVALID` (`REQ-001`) |
| 빈 파일 또는 20 MiB 초과 | `FILE_SIZE_ERROR` (`FILE-003`) |
| 허용하지 않은 확장자, Content-Type 불일치/parameter, signature 불일치/잘림 | `FILE_FORMAT_ERROR` (`FILE-002`) |
| signature를 읽을 수 없음 | `FILE_INVALID` (`FILE-001`) |

- 검증에 실패하면 Object Storage 업로드와 DB 저장을 호출하지 않는다.
- 기존 업로드/DB 실패 시 Object Storage 보상 삭제와 transaction rollback 처리는 유지한다.

## Success Contract

- 기존 `BaseResponse<FileUploadRes>`의 status, code, message, data 필드를 유지한다.
- `FileUploadRes`의 `fileNo`, 원본 파일명, 파일 크기, 파일 URL 의미를 변경하지 않는다.

## Non-goals and Residual Risk

- ImageIO 전체 decode/re-encode 또는 이미지 품질·완전한 무결성 보증
- PNG chunk/CRC/metadata, JPEG marker/segment, EXIF, 픽셀, bit depth, channel 검사
- 이미지 처리 동시성 제한, 바이러스 검사, WAF/전역 multipart 설정 변경
- Object Storage 공개 정책 또는 다운로드 인증 정책 변경
- DB schema/DDL/ERD/도메인 모델 변경
- signature 뒤에 추가 payload가 붙은 polyglot 파일 탐지

signature 시작 바이트만 확인하므로 정상 signature를 가진 손상 이미지나 polyglot 파일을 허용할 수 있는 잔여 위험이 있다.

## Implementation Readiness

- readiness: `ready`
- API, 저장값, 실패 코드와 비목표가 고정되어 구현을 시작할 수 있다.
