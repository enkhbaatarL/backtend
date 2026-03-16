# Newsletter v2 - 스레드 4 실행 문서 (Revision 저장 플로우)

## 스레드 기본 정보
- 스레드명: 스레드 4 - Revision 저장 플로우(draft/save)
- 담당: BE
- 리뷰어: FE, 기획
- 시작일: 2026-03-13
- 목표 완료일: 2026-03-18
- 관련 이슈/티켓: TBD

## Contract Sync Checklist
- OpenAPI version: `1.0.0` (`docs/newsletter-v2-openapi.yaml`)
- Canonical DDL version: `ddl-canonical-2026-03-12-r2` (`docs/newsletter-v2-ddl-canonical-index.md`)
- Last synced at (KST): `2026-03-12 21:40`
- Sync owner: `BE`

## 1) 범위 (In / Out)
- In Scope:
  - `PUT /revisions/draft`, `PUT /revisions/save`, `GET /revisions/draft`
  - `None -> EDIT_DRAFT -> EDIT_SAVED -> EDIT_DRAFT` 상태 전이 적용
  - `revisionNo` 기반 낙관적 락(`409`) 적용
  - 소유권 가드(`ownr_usr_no == actor_user_no`) 적용
  - 델타 누적 저장 및 최신 본문 반영
- Out of Scope:
  - 메일 실제 발송 처리
  - 변경이력 상세조회(`/changes`) 고도화
  - Section/Block 렌더링 UI 검증

## 2) 완료 기준 (Definition of Done)
- [ ] draft/save API 구현 완료
- [ ] 최초 저장 시 리비전 자동 생성 동작 확인
- [ ] 상태 전이 규칙 위반 시 에러코드 반환 확인
- [ ] revisionNo 충돌 시 409 응답 확인
- [ ] 소유권 가드(403) 검증 완료

## 2-1) 구현 착수 게이트
- 현재 게이트 상태: `READY`
- Blocking Open Question: 없음
- Non-blocking Open Questions:
  - snapshot 생성 시점
  - delta 검증 강도
- 강제 룰: Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.

## 3) 사전 합의 (계약 고정)
- 계약 스키마 재서술 금지: API 필드/응답/에러 상세는 OpenAPI 링크 참조만 사용한다.
- OpenAPI 링크: `docs/newsletter-v2-openapi.yaml`
- 응답/에러 규약:
  - `BaseResponse` envelope(`status`, `code`, `message`, `data`)
  - `code`는 `ResultCode` 매핑 기준 사용
- 멱등/동시성 정책:
  - `(nws_ltr_org_iss_id, ownr_usr_no)` 유니크
  - 저장 시 `revisionNo` 검증 후 성공 시 `+1`

## 4) 구현 체크리스트
- [ ] Controller/Validation
- [ ] RevisionCore 상태/권한/락 판정
- [ ] RevisionDiffService 델타 적용
- [ ] RevisionSnapshotStore 저장/조회 처리
- [ ] 예외 코드 매핑
- [ ] API 문서/예시 요청 업데이트

## 5) 테스트 전략
- 정상 케이스:
  - 최초 draft 저장 시 리비전 생성
  - draft 반복 저장 시 동일 리비전 갱신
  - save 호출 시 `EDIT_SAVED` 전환
- 경계 케이스:
  - `EDIT_SAVED -> EDIT_DRAFT` 재수정 시작
  - `revisionNo` 미포함 저장(정책 허용 범위 확인)
- 실패 케이스:
  - 타인 리비전 접근(403)
  - 충돌 revisionNo 저장(409)
  - 존재하지 않는 리비전 조회(404)
- 회귀 영향 범위:
  - 상세조회의 `source/hasDraft` 동기화 필드

## 6) 운영 체크리스트
- [ ] 충돌(409) 비율 모니터링 지표 정의
- [ ] draft/save 요청량 로그 샘플링 정책 확정
- [ ] 장애 시 리비전 복구 절차 확인
- [ ] 배치/메일 발송 연계 전 상태가드 재검증

## 7) 미정 항목 (Open Questions)
| 항목 | 현재 가정 | 담당자 | 결정 예정일 | 상태 | 게이트영향 |
|---|---|---|---|---|---|
| `revisionNo` 필수 여부 | 입력 시에만 충돌검사 | BE | 2026-03-15 | CLOSED | NON_BLOCKER |
| snapshot 생성 시점 | save 중심, draft는 선택. 권장 임계치 `deltaCount > 8` (운영 `6~10` 조정) | BE/기획 | 2026-03-15 | OPEN | NON_BLOCKER |
| delta 검증 강도 | JSON Patch 규격 최소검증 | BE | 2026-03-16 | OPEN | NON_BLOCKER |

## 8) 결정 로그 (Decision Log)
| 날짜 | 결정 내용 | 이유 | 대안 | 결정자 |
|---|---|---|---|---|
| 2026-03-11 | draft 최신본 조회 경로를 `/revisions/draft`로 단순화 | `(userNo, newsletterId, issueNo)` 당 리비전 1개 정책과 정합 | `/revisions/{revisionId}/draft` 유지 | BE |
| 2026-03-11 | `409` 충돌은 `REVISION_VERSION_CONFLICT` 전용 코드로 관리 | 동시성 충돌과 입력 오류를 분리해 FE 재시도/모니터링 명확화 | `REQ_INVALID_PARAMETER(400)` 재사용 | BE |
| 2026-03-11 | draft 조회 `404`는 단일 정책 유지 | FE 분기 단순화 및 계약 복잡도 축소 | `revision 없음`/`draft 없음` 세분화 | BE |

## 9) 테스트 결과
- 실행 명령: TBD
- 결과 요약: TBD
- 실패/보류 항목: TBD

## 10) 릴리즈 메모
- 변경 파일:
  - `src/main/java/...` (예정)
  - `docs/newsletter-v2-thread-4.md`
- 사용자/운영 영향:
  - 리비전 저장 안정성 및 동시편집 충돌 제어 가능
- 후속 작업(TODO):
  - 스레드 5(send-mail)에서 `EDIT_SAVED` 가드 연동
