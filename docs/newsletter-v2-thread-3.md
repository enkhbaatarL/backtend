# Newsletter v2 - 스레드 3 실행 문서 (구독/구독해지 API)

## 스레드 기본 정보
- 스레드명: 스레드 3 - 구독/구독해지 API
- 담당: BE
- 리뷰어: FE, 기획
- 시작일: 2026-03-10
- 목표 완료일: 2026-03-13
- 관련 이슈/티켓: TBD

## Contract Sync Checklist
- OpenAPI version: `1.0.0` (`docs/newsletter-v2-openapi.yaml`)
- Canonical DDL version: `ddl-canonical-2026-03-12-r2` (`docs/newsletter-v2-ddl-canonical-index.md`)
- Last synced at (KST): `2026-03-12 21:40`
- Sync owner: `BE`

## 1) 범위 (In / Out)
- In Scope:
  - 구독 등록/해지/상태수정 API 구현
  - `tmd_nwsltrsubm` 기반 멱등 처리
  - 요청 검증/에러코드/응답계약 고정
  - 감사용 타임스탬프(`sub_dtm`, `unsub_dtm`) 반영
- Out of Scope:
  - 대량 발송 배치
  - 관리자 UI
  - 리비전 편집/저장 로직

## 2) 완료 기준 (Definition of Done)
- [x] 구독 등록 API 구현 완료
- [x] 구독 해지 API 구현 완료
- [x] 중복 요청 시 멱등 처리 보장
- [x] 정상/경계/실패 테스트 코드 작성 완료
- [x] API 문서(요청/응답/에러코드) 반영 완료

## 2-1) 구현 착수 게이트
- 현재 게이트 상태: `READY`
- Blocking Open Question: 없음
- Non-blocking Open Questions:
  - 인증 주체 필드 명명 표준
  - 해지 후 재구독 시각 처리
  - 구독 이력 별도 테이블 도입 여부
- 강제 룰: Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.

## 3) 사전 합의 (계약 고정)
- 계약 스키마 재서술 금지: API 필드/응답/에러 상세는 OpenAPI 링크 참조만 사용한다.
- OpenAPI 링크: `docs/newsletter-v2-openapi.yaml`
- 응답/에러 규약:
  - `BaseResponse` envelope(`status`, `code`, `message`, `data`)
  - `code`는 `ResultCode` 매핑 기준 사용
- 멱등/동시성 정책:
  - `UNIQUE(nws_ltr_mstr_id, usr_no)` 기반 select-then-update

## 4) 구현 체크리스트
- [x] Controller + Request Validation
- [x] Service 멱등 처리 로직
- [x] Repository 쿼리 및 인덱스 사용 확인
- [x] 예외 매핑(404/403/400)
- [x] 감사 필드 업데이트 규칙 반영
- [x] API 문서 갱신

## 5) 테스트 전략
- 정상 케이스:
  - 최초 구독 성공
  - 구독 해지 성공
  - 해지 후 재구독 성공
- 경계 케이스:
  - 이미 구독 상태에서 구독 재요청(멱등)
  - 이미 해지 상태에서 해지 재요청(멱등)
- 실패 케이스:
  - 존재하지 않는 뉴스레터 구독 요청(404)
  - 인증 주체 불일치(403)
  - 필수 파라미터 누락/형식 오류(400)
- 회귀 영향 범위:
  - 기존 사용자 알림/환경설정 기능과의 충돌 여부

## 6) 운영 체크리스트
- [ ] 구독 이벤트 로깅 포맷 정의
- [ ] 비정상 요청(4xx) 모니터링 기준 정의
- [ ] 개인정보 필드 마스킹 정책 확인
- [ ] 배치 발송 대상자 조회 쿼리 검증

## 7) 미정 항목 (Open Questions)
| 항목 | 현재 가정 | 담당자 | 결정 예정일 | 상태 | 게이트영향 |
|---|---|---|---|---|---|
| 인증 주체 필드 | `actorUserNo`를 인증 컨텍스트에서 사용 | BE | 2026-03-11 | OPEN | NON_BLOCKER |
| 해지 후 재구독 시각 처리 | `sub_dtm` 갱신 | BE/기획 | 2026-03-11 | OPEN | NON_BLOCKER |
| 구독 이력 별도 테이블 | 현 단계 미도입 | BE | 2026-03-12 | OPEN | NON_BLOCKER |

## 8) 결정 로그 (Decision Log)
| 날짜 | 결정 내용 | 이유 | 대안 | 결정자 |
|---|---|---|---|---|

## 9) 테스트 결과
- 실행 명령: `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests com.sk.domain.mi.service.MiNewsletterSubscriptionServiceTest`
- 결과 요약: 7개 테스트 통과
- 실패/보류 항목: 없음

## 10) 릴리즈 메모
- 변경 파일:
  - `src/main/java/com/sk/domain/shared/entity/master/tmd/NewsletterSubscription.java`
  - `src/main/java/com/sk/domain/shared/repository/master/tmd/NewsletterSubscriptionRepository.java`
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterSubscriptionService.java`
  - `src/main/java/com/sk/domain/mi/controller/MiNewsletterSubscriptionController.java`
  - `src/main/java/com/sk/domain/mi/dto/response/newsletter/NewsletterSubscriptionRes.java`
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterService.java`
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterMailService.java`
  - `src/test/java/com/sk/domain/mi/service/MiNewsletterSubscriptionServiceTest.java`
  - `docs/newsletter-v2-thread-3.md`
- 사용자/운영 영향:
  - 구독 상태 변경이 API 레벨에서 안정적으로 관리됨
- 후속 작업(TODO):
  - 스레드 5 메일 발송 타겟 조회와 연결
