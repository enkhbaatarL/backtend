# Newsletter v2 - 스레드 2 실행 문서 (DB 스키마/제약 확정)

## 스레드 기본 정보
- 스레드명: 스레드 2 - DB 스키마/제약 확정
- 담당: BE
- 리뷰어: BE, 기획
- 시작일: 2026-03-06
- 목표 완료일: 2026-03-10
- 관련 이슈/티켓: TBD

## Contract Sync Checklist
- OpenAPI version: `1.0.0` (`docs/newsletter-v2-openapi.yaml`)
- Canonical DDL version: `ddl-canonical-2026-03-12-r2` (`docs/newsletter-v2-ddl-canonical-index.md`)
- Last synced at (KST): `2026-03-12 21:40`
- Sync owner: `BE`

## 1) 범위 (In / Out)
- In Scope:
  - `newsletter_issue_original`, `newsletter_user_revision`, `newsletter_user_revision_snapshot`, `newsletter_subscription` DDL 확정
  - 유니크/인덱스/외래키/삭제정책 확정
  - 상태 컬럼 제약(`pub_stat_cd`, `rvsn_stat_cd`) 반영
  - 마이그레이션/롤백 스크립트 준비
- Out of Scope:
  - API/DTO 상세 구현
  - 메일 발송 로직 구현
  - Section/Block 렌더링 규칙 구현

## 2) 완료 기준 (Definition of Done)
- [ ] DDL 초안이 실제 DB에 적용 가능
- [ ] 유니크 제약으로 1인 1리비전/1인 1구독 보장
- [ ] 상태값 제약(허용 enum) 반영
- [ ] 핵심 조회 경로 인덱스 검증 완료
- [ ] 마이그레이션 롤백 시나리오 문서화 완료

## 2-1) 구현 착수 게이트
- 현재 게이트 상태: `HOLD`
- Blocking Open Question:
  - `상태값 제약 방식(DB CHECK vs 코드 테이블)` 확정 전 DDL 최종안 잠금 불가
- Non-blocking Open Questions:
  - snapshot 보관 기간
  - `is_deleted` 처리 기준
- 강제 룰: Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.

## 3) 사전 합의 (계약 고정)
- 물리 스키마 사실 재서술 금지: 테이블/컬럼/제약 상세는 canonical DDL 인덱스 링크 참조만 사용한다.
- canonical DDL 링크: `docs/newsletter-v2-ddl-canonical-index.md`
- 동시성:
  - `newsletter_user_revision.rvsn_no` 기반 낙관적 락
- 상태값:
  - Original: `PUBLISHING`, `PUBLISHED`, `SUSPENDED`
  - Revision: `EDIT_DRAFT`, `EDIT_SAVED`

## 4) 구현 체크리스트
- [ ] 테이블/컬럼 타입 및 nullable 정책 정의
- [ ] 상태 컬럼 체크 제약 또는 코드 테이블 정책 확정
- [ ] FK/ON DELETE 정책 확정
- [ ] 주요 조회 경로 인덱스 생성
- [ ] 마이그레이션 스크립트 작성
- [ ] 롤백 스크립트 작성
- [ ] 스키마 변경 리뷰 반영

## 5) 테스트 전략
- 정상 케이스:
  - 신규 original/revision/subscription insert 성공
  - revision snapshot insert 및 조회 성공
- 경계 케이스:
  - 동일 `(nws_ltr_org_iss_id, ownr_usr_no)` 재삽입 차단
  - 동일 `(nws_ltr_mstr_id, userNo)` 재삽입 차단
- 실패 케이스:
  - 허용되지 않은 상태값 입력 차단
  - FK 무결성 위반 차단
- 회귀 영향 범위:
  - 기존 v1 조회/발행 테이블 접근 영향

## 6) 운영 체크리스트
- [ ] 배포 전 백업/복구 계획 확인
- [ ] 대용량 데이터 환경 인덱스 영향 점검
- [ ] 배치/실시간 API 경계에서 lock 이슈 점검
- [ ] 배포 순서(DDL -> 앱) 확정

## 7) 미정 항목 (Open Questions)
| 항목 | 현재 가정 | 담당자 | 결정 예정일 | 상태 | 게이트영향 |
|---|---|---|---|---|---|
| 상태값 제약 방식 | DB CHECK 제약 우선 | BE | 2026-03-10 | OPEN | BLOCKER |
| snapshot 보관 기간 | 무기한 보관 | BE/기획 | 2026-03-10 | OPEN | NON_BLOCKER |
| `is_deleted` 처리 기준 | soft delete 유지 | BE | 2026-03-10 | OPEN | NON_BLOCKER |

## 8) 결정 로그 (Decision Log)
| 날짜 | 결정 내용 | 이유 | 대안 | 결정자 |
|---|---|---|---|---|

## 9) 테스트 결과
- 실행 명령: TBD
- 결과 요약: TBD
- 실패/보류 항목: TBD

## 10) 릴리즈 메모
- 변경 파일:
  - `db/migration/*` (예정)
  - `docs/newsletter-v2-thread-2.md`
- 사용자/운영 영향:
  - 데이터 무결성 강제 및 후속 API 안정성 기반 확보
- 후속 작업(TODO):
  - 스레드 3(API 계약)로 DDL 반영 결과 전달
