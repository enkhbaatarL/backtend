# Newsletter v2 스레드 실행 템플릿

## 사용 방법
- 이 문서는 스레드 단위 구현 시 복사해서 사용한다.
- 스레드 시작 전에 `범위`, `완료 기준(DoD)`, `미정 항목`을 먼저 채운다.
- 구현 후 `테스트 결과`, `결정 로그`, `릴리즈 메모`를 반드시 갱신한다.
- 계약 사실은 OpenAPI + canonical DDL 링크 참조만 유지하고 본문 재서술을 금지한다.

## Contract Sync Checklist
- OpenAPI version:
- Canonical DDL version:
- Last synced at (KST):
- Sync owner:

## 스레드 기본 정보
- 스레드명:
- 담당:
- 리뷰어:
- 시작일:
- 목표 완료일:
- 관련 이슈/티켓:

## 1) 범위 (In / Out)
- In Scope:
- Out of Scope:

## 2) 완료 기준 (Definition of Done)
- [ ] 코드 구현 완료
- [ ] API/DTO/에러코드 문서 반영 완료
- [ ] 테스트 코드(정상/실패/경계) 작성 완료
- [ ] 로깅/모니터링 포인트 반영 완료
- [ ] 리뷰 코멘트 반영 완료

## 2-1) 구현 착수 게이트
- 현재 게이트 상태: `READY` 또는 `HOLD`
- Blocking Open Question:
- Non-blocking Open Questions:
- 강제 룰: Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.

## 3) 사전 합의 (계약 고정)
- 엔드포인트/메서드: OpenAPI 링크 참조만 허용
- 요청 필드: OpenAPI 링크 참조만 허용
- 응답 필드: `BaseResponse` envelope 기준(`status`, `code`, `message`, `data`) + `data` 내부 도메인 필드
- 상태코드/에러코드: OpenAPI + ResultCode 링크 참조만 허용
- 에러 본문 규약: `BaseResponse` envelope + `ResultCode` 매핑
- 멱등/동시성 정책:

## 4) 구현 체크리스트
- [ ] Controller/Request Validation
- [ ] Service 비즈니스 규칙
- [ ] Repository/Query/Index 확인
- [ ] 트랜잭션 경계 확인
- [ ] 예외 매핑 및 에러 메시지
- [ ] 감사/이력 저장 (필요 시)

## 5) 테스트 전략
- 정상 케이스:
- 경계 케이스:
- 실패 케이스:
- 회귀 영향 범위:

## 6) 운영 체크리스트
- [ ] 마이그레이션 필요 여부 확인
- [ ] 롤백 전략 정의
- [ ] Feature Flag 필요 여부
- [ ] 배치/API 경계 확정
- [ ] 알람/메트릭 지표 정의

## 7) 미정 항목 (Open Questions)
| 항목 | 현재 가정 | 담당자 | 결정 예정일 | 상태 | 게이트영향 |
|---|---|---|---|---|---|
| 예시) 구독 메일 발송 트리거(배치/API) | 배치 우선 가정 | BE | 2026-03-13 | OPEN | NON_BLOCKER |

## 8) 결정 로그 (Decision Log)
| 날짜 | 결정 내용 | 이유 | 대안 | 결정자 |
|---|---|---|---|---|

## 9) 테스트 결과
- 실행 명령:
- 결과 요약:
- 실패/보류 항목:

## 10) 릴리즈 메모
- 변경 파일:
- 사용자/운영 영향:
- 후속 작업(TODO):
