# Newsletter Subscription Backend Feature Spec

## 1) 관련 기존 구조 조사

### 1.1 API/Controller
- 기준 경로: `/api/v1/mi/newsletter`
- 기존 컨트롤러:
  - `MiNewsletterController`: 목록/featured/issues 조회
  - `MiNewsletterDedicatedController`: 상세 조회
  - `MiNewsletterMailController`, `MiNewsletterTriggerController`: 메일 발송/트리거
- 응답 패턴: `BaseResponse<T>`

### 1.2 Entity/Repository
- 뉴스레터 핵심 엔티티:
  - `tmd_nws_ltr_mstr` (`NewsletterMaster`)
  - `tmd_nws_ltr_pub_hist` (`NewsletterPublishHistory`)
- 현재 구독 전용 JPA 엔티티/리포지토리 부재

### 1.3 Service/Mapper 동작 확인
- `MiNewsletterService`에서 featured/detail의 `subscribed` 값이 고정 `false`였음
- `MiNewsletterMailService`는 원본 발송 수신자 조회 시 raw SQL을 사용 중이었음
- `sendOriginalIssueMailFromCapture(...)`는 하드코딩 수신자 사용 상태였음

## 2) 신규 도메인 모델/ERD 초안 (new-domain-model-design)

## 2.1 Feature summary
- 신규 개념: 사용자-뉴스레터 구독 상태(`구독/해지`)를 영속적으로 관리하는 도메인
- 목적:
  - 사용자별 구독 상태 조회/변경 API 제공
  - 뉴스레터 메일 발송 수신자 소스를 구독 데이터로 일원화

## 2.2 Proposed domain model
- Aggregate Root: `NewsletterSubscription`
- 성격: 사용자 설정/관계형 도메인(association + lifecycle)

## 2.3 Table design draft (`tmd_nwsltrsubm`)
- purpose: 사용자별 뉴스레터 구독 상태 관리
- key columns:
  - `nws_ltr_sub_id` (PK, bigint unsigned)
  - `nws_ltr_mstr_id` (FK -> `tmd_nws_ltr_mstr.nws_ltr_mstr_id`)
  - `usr_no` (FK -> `tmd_usrm.usr_no`)
  - `sub_yn` (boolean)
  - `sub_dtm` (datetime, nullable)
  - `unsub_dtm` (datetime, nullable)
  - 감사/소프트삭제 컬럼: `crt_dtm`, `mod_dtm`, `crt_usr_logn_id`, `mod_usr_logn_id`, `del_yn`
- unique constraints:
  - `tmd_nwsltrsubm_ux01(nws_ltr_mstr_id, usr_no)`
- indexes:
  - `tmd_nwsltrsubm_ix01(nws_ltr_mstr_id, sub_yn, del_yn)`
  - `tmd_nwsltrsubm_ix02(usr_no, del_yn)`
- nullable guidance:
  - `is_subscribed` NOT NULL
  - `subscribed_at`, `unsubscribed_at`는 상태 전환 이력 표현을 위해 NULL 허용
- status/delete strategy:
  - 구독 해지는 삭제가 아닌 `is_subscribed=false`로 상태 전환
  - `del_yn='Y'`는 운영상 논리 삭제 케이스에 한정

## 2.4 Relationship design
- `NewsletterMaster (1) : (N) NewsletterSubscription`
- `User (1) : (N) NewsletterSubscription`
- 멀티테넌시: 사용자(`tmd_usrm`)와 뉴스레터 접근권한(`cust_cpny_no`) 검증을 서비스 레이어에서 수행

## 2.5 Business constraints
- 같은 `(nws_ltr_mstr_id, usr_no)` 중복 row 금지(UK)
- subscribe 멱등:
  - 이미 구독이면 성공(no-op)
  - 미구독/없으면 구독으로 전환
- unsubscribe 멱등:
  - 이미 미구독이면 성공(no-op)
  - 구독 중이면 해지 전환

## 2.6 Future extension 포인트
- 채널(email/push)별 구독 설정
- 카테고리/토픽 단위 구독
- 감사로그 분리 테이블(이벤트성 이력) 도입

## 2.7 구현 방향
- JPA: 구독 상태 write + 단건 read
- QueryDSL: 목록/featured/detail에서 `subscribed` 대량 조회 최적화
- MyBatis: 현 단계 미도입(리포팅성 대량 조회가 생길 때 검토)

## 3) ERD 충돌/DBA 반영 검토 (erd-change-review)

## 3.1 Current structure reviewed
- 관련 코드:
  - `MiNewsletterService`: `subscribed` 하드코딩
  - `MiNewsletterMailService`: 수신자 조회 raw SQL/하드코딩 수신자 혼용
- 결론: 컨벤션 테이블(`tmd_nwsltrsubm`) 기준으로 JPA 모델 정식화 후, 메일 수신자 조회도 JPA로 통일

## 3.2 Change classification
- 분류: `new table needed` (컨벤션 테이블 기준)

## 3.3 Proposed schema change
- 물리 테이블명 `tmd_nwsltrsubm`
- 필요한 제약/인덱스/감사컬럼 반영

## 3.4 ERD review
- PK/FK:
  - PK: `nws_ltr_sub_id`
  - FK1: `nws_ltr_mstr_id` -> `tmd_nws_ltr_mstr.nws_ltr_mstr_id`
  - FK2: `usr_no` -> `tmd_usrm.usr_no`
- UK/index:
  - UK `(nws_ltr_mstr_id, usr_no)` 필수
  - 수신자 조회 성능용 인덱스 필수
- data type:
  - FK 타입은 참조 대상과 unsigned/int 계열 일치 필요
- nullable/default:
  - `sub_yn` 기본값 `TRUE` 또는 서비스에서 명시 설정
- lifecycle/delete:
  - 상태값 중심 관리 + soft delete 보조
- audit/base fields:
  - `BaseEntity` 매핑을 고려해 감사 컬럼 표준화 권장

## 3.5 Code impact review
- JPA:
  - `NewsletterSubscription` 엔티티/리포지토리 신규
- MyBatis:
  - 기존 유지(변경 없음)
- QueryDSL:
  - 뉴스레터 목록/featured/detail에서 구독여부 projection 최적화 시 추가
- DTO/API:
  - `NewsletterSubscriptionRes` 신규
  - 기존 newsletter 응답의 `subscribed`를 실제값으로 연결
- Service:
  - 구독 멱등 처리 서비스 추가
  - 메일 수신자 조회 로직 통일

## 3.6 Rollout / compatibility review
- 기존 데이터 호환:
  - 중복 `(nws_ltr_mstr_id, usr_no)` 존재 시 UK 추가 전 정리 필수
- 배포 순서:
  1. DBA SQL 반영
  2. 앱 배포(JPA validate)
- Null-safe 전략:
  - `subscribed_at/unsubscribed_at`는 초기 nullable 유지

## 3.7 Recommendation
- `safe to proceed as-is`
  - 테이블명은 `tmd_nwsltrsubm`로 확정
  - 구독 도메인 read/write는 JPA 중심으로 구현

## 3.8 Required SQL / DBA action
- `tmd_nwsltrsubm` 생성 + FK/UK/index
- 운영 반영 전 중복 데이터 점검/정리
- `ddl-auto=validate` 유지 전제에서 DBA 수동 반영 필수

## 3.9 Risks / open questions
- 운영 DB에 이미 존재하는 subscription 스키마의 실제 컬럼 정의 확인 필요
- 구독 해지 이력을 별도 이벤트로 분리할지(현재는 상태 컬럼 방식)

## 4) API 계약 초안 (backend-feature-spec)

## 4.1 Feature goal
- 사용자 본인의 뉴스레터 구독 상태를 조회/변경하는 표준 API 제공

## 4.2 Endpoint
- `PUT /api/v1/mi/newsletter/{newsletterId}/subscription`
  - summary: 내 뉴스레터 구독(멱등)
- `DELETE /api/v1/mi/newsletter/{newsletterId}/subscription`
  - summary: 내 뉴스레터 구독해지(멱등)
- `GET /api/v1/mi/newsletter/{newsletterId}/subscription`
  - summary: 내 뉴스레터 구독 상태 조회
- auth/permission:
  - 기존 newsletter API와 동일한 `@RequiredPermission` 패턴
  - actor user 기준 본인 상태만 접근

## 4.3 Request DTO draft
- 본 3개 API는 Request Body 없음
- Path variable:
  - `newsletterId` (required, positive integer)
- validation rules:
  - 숫자 형식/양수 검증
  - 접근 가능한 newsletter인지 검증

예시(Body 없음):
```json
{}
```

## 4.4 Response DTO draft
- DTO: `NewsletterSubscriptionRes`
  - `newsletterId: String`
  - `subscribed: Boolean`
  - `subscribedAt: LocalDateTime | null`
  - `unsubscribedAt: LocalDateTime | null`

응답 형태:
- `BaseResponse<NewsletterSubscriptionRes>`

예시:
```json
{
  "result": "SUCCESS",
  "data": {
    "newsletterId": "1",
    "subscribed": true,
    "subscribedAt": "2026-03-11T14:25:00",
    "unsubscribedAt": null
  }
}
```

## 4.5 Representative error scenarios
- `REQ_INVALID_PARAMETER`: newsletterId 형식 오류
- `RSC_NOT_FOUND`: 접근 가능한 newsletter 없음
- `AUTH_REQUIRED`: 사용자 컨텍스트 없음
- `AUTH_INSUFFICIENT_PERMISSIONS`: 권한 부족
- `BIZ_RULE_VIOLATION`: 비정상 상태 전이(필요 시)

## 4.6 Swagger annotation recommendation
- `@Operation(summary = "...")`
- `@ApiResponse` (200, 400, 401, 403, 404)
- DTO 필드에 `@Schema` 설명/예시 추가

## 4.7 Data/model impact
- `existing schema extension` (환경별로 신규 생성 가능)

## 4.8 Recommended data access approach
- JPA: 구독 상태 CUD + 단건 조회
- QueryDSL: 목록 다건 조회 최적화 필요 시 후보
- MyBatis: 현 단계 미도입 (구독 기능은 SQL 복잡도 낮음)

## 4.9 Implementation touch points
- controller:
  - `MiNewsletterSubscriptionController` (신규)
- service:
  - `MiNewsletterSubscriptionService` (신규)
  - `MiNewsletterService` (기존 `subscribed` 하드코딩 제거)
  - `MiNewsletterMailService` (capture 발송 수신자 구독 조회 통일)
- dto:
  - `NewsletterSubscriptionRes` (신규)
- repository/entity:
  - `NewsletterSubscription` + `NewsletterSubscriptionRepository` (신규)
- swagger:
  - 신규 API 문서화

## 4.10 Risks / open questions
- 목록/featured/detail에서 `subscribed`를 어떤 쿼리 방식으로 일괄 조인할지(QueryDSL 적용 범위)
- 구독 상태 변경시 감사로그를 별도 저장할지

## 5) 구현 필요 파일 + SQL 변경 포인트 최종 정리

## 5.1 구현 필요 파일(예정)
- 신규
  - `src/main/java/com/sk/domain/shared/entity/master/tmd/NewsletterSubscription.java`
  - `src/main/java/com/sk/domain/shared/repository/master/tmd/NewsletterSubscriptionRepository.java`
  - `src/main/java/com/sk/domain/mi/dto/response/newsletter/NewsletterSubscriptionRes.java`
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterSubscriptionService.java`
  - `src/main/java/com/sk/domain/mi/controller/MiNewsletterSubscriptionController.java`
- 수정
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterService.java`
  - `src/main/java/com/sk/domain/mi/service/MiNewsletterMailService.java`

## 5.2 SQL 변경 포인트 (DBA 수동 반영)
- Create/Alter 대상: `tmd_nwsltrsubm`
- 필수 제약:
  - PK, FK 2개, UK(`nws_ltr_mstr_id`,`usr_no`)
- 필수 인덱스:
  - `(nws_ltr_mstr_id, sub_yn, del_yn)`
  - `(usr_no, del_yn)`
- 데이터 정리:
  - UK 추가 전 중복 row 제거
  - `is_subscribed` null/backfill 정책 적용
- 배포 원칙:
  - `ddl-auto=validate`이므로 SQL 선반영 후 애플리케이션 배포
