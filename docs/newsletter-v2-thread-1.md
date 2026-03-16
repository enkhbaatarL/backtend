# Newsletter v2 - 스레드 1 실행 문서 (도메인/상태 확정)

## 스레드 기본 정보
- 스레드명: 스레드 1 - 도메인/상태 확정
- 담당: BE
- 리뷰어: FE, 기획
- 시작일: 2026-03-06
- 목표 완료일: 2026-03-13
- 관련 이슈/티켓: TBD

## Contract Sync Checklist
- OpenAPI version: `1.0.0` (`docs/newsletter-v2-openapi.yaml`)
- Canonical DDL version: `ddl-canonical-2026-03-12-r2` (`docs/newsletter-v2-ddl-canonical-index.md`)
- Last synced at (KST): `2026-03-12 21:40`
- Sync owner: `BE`

## 1) 범위 (In / Out)
- In Scope:
  - Original/Revisions 상태값 확정
  - 상태 전이 허용표 확정
  - 접근/권한/상태 가드 확정
  - 상태 관련 에러코드/HTTP 매핑 확정
- Out of Scope:
  - DB DDL 상세 설계
  - API payload 상세 스펙(OpenAPI 작성)
  - 메일/스케줄러 구현 상세

## 2) 완료 기준 (Definition of Done)
- [ ] 상태값/전이표 문서 확정
- [ ] 권한/상태 가드 문서 확정
- [ ] 에러코드/HTTP 매핑 문서 확정
- [ ] 팀 리뷰(기획/FE/BE) 코멘트 반영 완료
- [ ] 후속 스레드(2, 3)로 넘길 입력값 정리 완료

## 2-1) 구현 착수 게이트
- 현재 게이트 상태: `READY`
- Blocking Open Question: 없음
- Non-blocking Open Questions:
  - Original 복구 전이 허용 범위
  - 관리자 예외 권한 범위
- 강제 룰: Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.

## 3) 사전 합의 (계약 고정)
- 계약 스키마 재서술 금지: API 필드/응답/에러 상세는 OpenAPI 링크 참조만 사용한다.
- 상태 도메인:
  - Originals: `PUBLISHING`, `PUBLISHED`, `SUSPENDED`
  - Revisions: `EDIT_DRAFT`, `EDIT_SAVED`
- 상태 전이:
  - Originals: `PUBLISHING -> PUBLISHED -> SUSPENDED`, `SUSPENDED -> PUBLISHED(운영복구)`
  - Revisions: `None -> EDIT_DRAFT -> EDIT_SAVED -> EDIT_DRAFT`
- 권한/가드:
  - 리비전 조회/수정/발송은 `ownr_usr_no == actor_user_no`
  - 메일 발송은 `EDIT_SAVED` 상태만 허용
- 응답/에러 포맷:
  - 성공/실패 모두 `BaseResponse` envelope(`status`, `code`, `message`, `data`) 사용
  - 에러 `code`는 `ResultCode` 매핑 기준 사용
- 에러 매핑:
  - `NEWSLETTER_NOT_FOUND`(404)
  - `REVISION_NOT_FOUND`(404)
  - `REVISION_FORBIDDEN`(403)
  - `REVISION_INVALID_STATE`(422)
  - `REVISION_VERSION_CONFLICT`(409)

## 4) 구현 체크리스트 (스레드 1 산출 중심)
- [ ] 상태/전이 표 최종본 확정
- [ ] 상태 가드 규칙(메일/조회/수정) 문장 확정
- [ ] 권한 가드 문장 확정
- [ ] 에러코드 정의 및 의미 확정
- [ ] 스레드 2 전달 항목(상태 컬럼/제약) 정리
- [ ] 스레드 3 전달 항목(API 에러계약) 정리

## 5) 테스트 전략 (문서 검증 관점)
- 정상 케이스:
  - 허용된 상태 전이 시나리오가 문서와 일치하는지 검증
- 경계 케이스:
  - `None -> EDIT_DRAFT` 최초 생성 전이
  - `EDIT_SAVED -> EDIT_DRAFT` 재수정 전이
- 실패 케이스:
  - 비허용 전이 요청 시 `REVISION_INVALID_STATE` 대응 여부
  - 타인 리비전 접근 시 `REVISION_FORBIDDEN` 대응 여부

## 6) 운영 체크리스트
- [ ] 상태 전이 로그(감사 추적) 필요 여부 확인
- [ ] 운영 복구(`SUSPENDED -> PUBLISHED`) 권한 주체 확정
- [ ] 에러코드 모니터링 항목(403/409/422) 정의
- [ ] 릴리즈 시 기존 v1 동작 영향도 점검

## 7) 미정 항목 (Open Questions)
| 항목 | 현재 가정 | 담당자 | 결정 예정일 | 상태 | 게이트영향 |
|---|---|---|---|---|---|
| Original 복구 전이 허용 범위 | `SUSPENDED -> PUBLISHED` 허용 | BE/기획 | 2026-03-13 | OPEN | NON_BLOCKER |
| Revision 충돌 검사 강제 여부 | `revisionNo` 입력 시에만 검사 | BE | 2026-03-13 | CLOSED | NON_BLOCKER |
| 관리자 예외 권한 범위 | 운영 복구 액션만 ADMIN | BE/기획 | 2026-03-13 | OPEN | NON_BLOCKER |

## 8) 결정 로그 (Decision Log)
| 날짜 | 결정 내용 | 이유 | 대안 | 결정자 |
|---|---|---|---|---|
| 2026-03-06 | Revisions 상태를 `EDIT_DRAFT`, `EDIT_SAVED`로 고정 | 저장/발송 가드 단순화 | 별도 중간 상태 추가 | BE/기획 |
| 2026-03-06 | 메일 발송 허용 상태를 `EDIT_SAVED`로 제한 | 발송 시점 데이터 품질 보장 | `EDIT_DRAFT`도 허용 | BE/기획 |
| 2026-03-06 | 소유권 기반 접근 제어(`ownr_usr_no`) 적용 | 개인정보/권한 경계 명확화 | role 기반 완화 | BE |

## 9) 테스트 결과
- 실행 명령: N/A (설계 스레드)
- 결과 요약: 상태/전이/가드/에러계약 문서 리뷰 후 확정 예정
- 실패/보류 항목: Open Questions 3건

## 10) 릴리즈 메모
- 변경 파일:
  - `docs/newsletter-v2-design.md`
  - `docs/newsletter-v2-design-confluence.md`
  - `docs/newsletter-v2-thread-1.md`
- 사용자/운영 영향:
  - 상태/권한 규칙이 API/DB 구현의 기준선으로 고정됨
- 후속 작업(TODO):
  - 스레드 2: 상태 컬럼/제약 DDL 구체화
  - 스레드 3: 상태 기반 API 에러 응답 계약 고정
