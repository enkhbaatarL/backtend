# 이벤트 키워드 알림 도메인 모델 설계

## Document Control

- status: Draft
- canonical for: Business rules, lifecycle, ownership, and state transitions
- owner: BE / PL
- source documents:
  - `AGENTS.md`
  - `SECURITY.md`
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
  - `docs/work-notes/2026/2026-07-31-notification-setting-email-flow-investigation.md`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiEvent.java`
  - `src/main/java/com/sk/domain/mi/controller/nextgen/EventController.java`
  - `src/main/java/com/sk/domain/mi/controller/MiNotificationController.java`
  - `src/main/java/com/sk/domain/mi/service/MiNotificationService.java`
  - `src/main/java/com/sk/domain/shared/entity/master/tlm/EmailAccessLog.java`
  - `src/main/java/com/sk/domain/shared/repository/master/tlm/custom/CustomEmailAccessLogRepositoryImpl.java`
  - `src/main/java/com/sk/domain/mi/dto/response/notification/MiNotificationRes.java`
- depends on:
  - 향후 작성할 이벤트 키워드 알림 API feature spec
  - 향후 작성할 canonical DDL draft
- supersedes:
  - 없음
- last synced: 2026-09-04
- open questions:
  - 1차 알림 채널을 앱 내 알림으로 한정할지, 이메일까지 동시에 보낼지 PL 확정이 필요하다.
  - 매칭 대상을 현재 화면의 이벤트명인 `one_line_summary`로 한정할지, 상세 분석과 태그까지 포함할지 PL 확정이 필요하다.
  - 외부 Agent 적재 트랜잭션 완료 후 callback/message를 보낼지, BE가 신규 이벤트를 주기적으로 스캔할지 운영 소유자 확정이 필요하다.

## 1. Feature summary

- 전체 기능은 `사용자별 키워드 설정`과 `이벤트-키워드 매칭 및 알림 발송` 2개로 나눈다.
- 사용자는 자신이 관심 있는 이벤트 키워드를 각각 등록한다.
- 신규 MI 이벤트가 생성된 후 이벤트 표시 문구에 등록 키워드가 포함되면 사용자 알림을 생성한다.
- 한 이벤트가 한 사용자의 여러 키워드와 일치해도 알림은 이벤트별 1건만 생성한다.
- 매칭된 모든 키워드는 알림 상세로 남겨 사용자가 알림 이유를 확인할 수 있게 한다.
- 생성된 이벤트 키워드 알림은 기존 `My Notification` 화면의 목록, 미조회 수, 상세 이동, 조회 처리에 포함되어야 한다.
- 이 문서는 도메인 모델과 업무 규칙만 정본으로 정의한다. API 필드와 물리 DDL은 별도 정본 문서에서 확정한다.

### 1.1 Initial scope assumptions

- `이벤트가 생성되면`은 신규 `MiEvent` row가 완전히 commit된 시점으로 본다.
- 이벤트 갱신으로 처음 키워드가 포함된 경우는 1차 범위에서 새 알림을 만들지 않는다.
- 핵심 알림 데이터는 채널과 분리한다. 앱 내 알림, 이메일, 푸시는 같은 알림 생성 결과를 전달하는 별도 책임이다.

### 1.2 Functional decomposition

| Feature | Main actor | Responsibility | Owned model | Independent completion boundary |
| --- | --- | --- | --- | --- |
| 1. 사용자별 키워드 설정 | 인증된 사용자 | 키워드 등록·목록·해지, 정규화, 중복 방지, 소유권 검사 | `EventKeywordSetting` | 키워드 CRUD와 권한·중복 테스트가 완료되면 독립 배포 가능 |
| 2. 이벤트-키워드 매칭 및 알림 발송 | 시스템 | 신규 이벤트 수신, 활성 키워드 매칭, 중복 없는 알림 저장, 확정된 채널로 전달, `My Notification` 통합 노출 | `EventKeywordNotification`, `EventKeywordNotificationMatch`, 비영속 `MyNotificationReadModel` | trigger·매칭·중복 방지·전달·통합 조회/읽음 테스트가 완료되면 활성화 가능 |

기능 경계 규칙:

- 2번 기능은 1번이 저장한 활성 `EventKeywordSetting`을 읽는다.
- 2번 기능은 키워드를 등록·수정·해지하지 않는다.
- 1번 기능은 2번의 trigger, 매칭 상태, 알림 전송 성공 여부를 알지 못해도 정상 동작해야 한다.
- 따라서 1번을 먼저 배포하고 2번을 나중에 활성화할 수 있다.
- 2번의 외부 이름은 하나의 기능으로 유지하되, 내부 처리는 `매칭 및 알림 저장` → `채널 발송`으로 분리한다. 발송 실패는 이미 저장된 알림을 rollback하지 않는다.

### 1.3 Fixed decisions

- `My Notification` 노출 기간은 기존과 동일하게 각 알림의 발생 시각 기준 최근 7일이다.
- 기존 이메일/상품 임계치 전용 API는 전환 기간 동안 유지한다.
- `My Notification` 화면은 새 통합 응답 계약으로 전환한다.
- FE 전환과 호환 기간 종료가 확인되기 전에 기존 API를 제거하지 않는다. 제거는 별도 계약 변경과 승인 대상이다.

## 2. Existing structure reviewed

### 2.1 Event domain

- `MiEvent` / `tmm_mi_mievent_m`은 Next Gen MI 이벤트의 현재값을 보관한다.
- 현재 화면의 이벤트명과 목록 검색 기준은 `event_title`이 아니라 `one_line_summary`다.
- 목록 검색은 `FullTextSearchUtil` ngram FULLTEXT BOOLEAN MODE를 사용하며, 공백으로 나뉜 토큰을 OR 조건으로 처리한다.
- BE 애플리케이션에서 `MiEvent` 생성/수정 경로를 찾을 수 없었다. 외부 Agent/적재 파이프라인과의 경계가 있다.

### 2.2 Notification domain

- `MiNotificationController` / `MiNotificationService`는 현재 사용자별 전체 알림 여부와 상품 데이터 임계치 설정을 관리한다.
- `User.usr_noti_set_yn`은 사용자의 전체 알림 수신 스위치다.
- `GoodsCriticalValueSetting`은 상품 데이터 임계치에 특화된 설정이다.
- `EmailAccessLog` / `tlm_emlsendl`은 `GoodsCriticalValueSetting`을 참조하는 이메일 전송 로그이며, 범용 앱 내 알림이나 이벤트 매칭 이력의 소스 오브 트루스로 사용하기에는 구조가 맞지 않는다.
- 현재 `My Notification` 목록은 `EmailAccessLog.emailSendDtm >= now - 7 days`와 현재 사용자 `userLoginId`로만 조회한다.
- 현재 `MiNotificationRes`는 `emailSendSn`, 이메일 제목/내용/조회 여부, 상품 임계치·데이터 필드로 구성되어 이벤트 알림을 표현할 공통 유형과 식별자가 없다.
- 기존 임계치 알림은 외부 scheduler가 보호된 HTTP API를 호출하고, BE가 판정과 SMTP 전송을 수행한다.

### 2.3 Security and tenant boundary

- 키워드 설정과 알림 조회는 인증된 현재 사용자의 `usr_no`로 범위를 고정해야 한다.
- client가 보낸 `usr_no` 또는 고객사 식별자로 소유자 범위를 넓히면 안 된다.
- 이벤트 상세 링크를 노출할 때는 현재 이벤트 API와 같은 권한 조건을 다시 검사해야 한다.
- async나 scheduler에서 테넌트 데이터 소스를 사용한다면 작업 실행 스레드 내에서 테넌트 키를 명시적으로 설정하고 `finally`에서 정리해야 한다. 현재 후보 테이블은 master JPA 범위라 추가 테넌트 라우팅이 필요하지 않다.

## 3. Proposed domain model

도메인 엔터티는 3개를 유지하지만, 기능 소유권은 아래처럼 2개로 나눈다.

### 3.1 `EventKeywordSetting`

- 소유 기능: `사용자별 키워드 설정`
- 성격: 사용자 소유 설정 엔터티
- 책임: 사용자가 알림을 받고 싶은 하나의 키워드를 보관한다.
- 소유자: `User` 1명
- 상태: 활성(`del_yn = N`) ↔ 해지(`del_yn = Y`)
- 키워드 문자열은 등록 시 정규화한 값을 저장한다.

### 3.2 `EventKeywordNotification`

- 소유 기능: `이벤트-키워드 매칭 및 알림 발송`
- 성격: 사용자별 알림 내역/트랜잭션 엔터티
- 책임: 신규 이벤트가 해당 사용자의 하나 이상 키워드와 일치했음을 나타낸다.
- 소유자: `User` 1명
- 대상: `MiEvent` 1건
- 상태: 미조회(`inquiryAt = null`) → 조회(`inquiryAt != null`)
- 갱신되거나 삭제될 수 있는 현재 이벤트와 관계없이 매칭 시점을 설명하기 위한 이벤트 표시 문구 snapshot을 보관한다.

### 3.3 `EventKeywordNotificationMatch`

- 소유 기능: `이벤트-키워드 매칭 및 알림 발송`
- 성격: 알림의 매칭 상세 엔터티
- 책임: 알림을 발생시킨 각 `EventKeywordSetting`과 당시 키워드 snapshot을 보관한다.
- 하나의 알림에 1개 이상의 매칭 상세가 속한다.

### 3.4 Aggregate boundary

- `EventKeywordSetting`은 독립적으로 등록·해지되는 설정 aggregate다.
- `EventKeywordNotification`은 자신의 `EventKeywordNotificationMatch` 목록을 포함하는 알림 aggregate다.
- 알림 header와 match detail은 하나의 master DB 트랜잭션에서 함께 생성한다.

### 3.5 `MyNotificationReadModel`

- 성격: 비영속 통합 조회 projection
- 책임: 기존 상품 임계치 알림(`EmailAccessLog`)과 이벤트 키워드 알림(`EventKeywordNotification`)을 하나의 `My Notification` 목록으로 표현한다.
- 식별: 알림 유형과 원천 ID를 함께 사용한다. 서로 다른 테이블의 숫자 PK만으로는 통합 목록에서 알림을 고유하게 식별할 수 없다.
- 공통 표현: 유형, 원천 식별자, 제목, 내용, 발생 시각, 조회 여부, 이동 대상 유형/식별자
- 상품 임계치나 매칭 키워드 같은 유형별 상세는 공통 목록에 무조건 nullable 필드로 늘어놓지 않고 유형별 상세 응답으로 분리한다.
- 1차 권장안은 통합 inbox를 위한 신규 공통 테이블을 추가하지 않고, 기존/신규 테이블을 통합 조회하는 것이다.

## 4. Table design draft

아래 이름과 타입은 domain-model 단계의 후보다. 물리 schema의 정본은 후속 canonical DDL draft에서 `erd-change-review`를 거쳐 확정한다.

### 4.1 Keyword setting master

- 후보 테이블: `tmm_mi_usr_evnt_keywd_set_m`
- 엔터티 유형: 기본(`m`)

| Logical field | Physical name candidate | Type | Null | Purpose |
| --- | --- | --- | --- | --- |
| keyword setting ID | `usr_evnt_keywd_set_id` | `bigint unsigned` | N | PK |
| owner user number | `usr_no` | `int unsigned` | N | `tmd_usrm.usr_no` FK |
| normalized keyword name | `keywd_nm` | `varchar(100)` | N | 매칭과 표시에 사용할 키워드 |
| audit fields | `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn` | existing convention | N | `BaseEntity` 패턴 |

제약과 인덱스 후보:

- PK: `usr_evnt_keywd_set_id`
- unique: `(usr_no, keywd_nm)`
- list index: `(usr_no, del_yn, crt_dtm desc)`
- unique를 전체 생명주기에 적용한다. 해지한 키워드를 다시 등록할 때는 새 row를 추가하지 않고 기존 row를 soft restore한다.

### 4.2 User-event notification history

- 후보 테이블: `tmm_mi_usr_evnt_noti_l`
- 엔터티 유형: 내역(`l`)

| Logical field | Physical name candidate | Type | Null | Purpose |
| --- | --- | --- | --- | --- |
| notification ID | `usr_evnt_noti_id` | `bigint unsigned` | N | PK |
| owner user number | `usr_no` | `int unsigned` | N | `tmd_usrm.usr_no` FK |
| event ID | `mi_event_id` | `int unsigned` | N | `tmm_mi_mievent_m.mi_event_id` FK |
| event summary snapshot | `evnt_smry_snpsht` | `varchar(300)` | N | 매칭 시점 `one_line_summary` 보존 |
| inquiry date-time | `inqr_dtm` | `datetime` | Y | null은 미조회, 값이 있으면 최초 조회 시각 |
| audit fields | `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn` | existing convention | N | `BaseEntity` 패턴 |

제약과 인덱스 후보:

- PK: `usr_evnt_noti_id`
- unique: `(usr_no, mi_event_id)`
- inbox index: `(usr_no, del_yn, crt_dtm desc)`
- unread count index는 실제 조회 SQL과 데이터 분포를 확인한 후 추가한다.

### 4.3 Notification-match detail

- 후보 테이블: `tmm_mi_usr_evnt_noti_keywd_d`
- 엔터티 유형: 상세(`d`)

| Logical field | Physical name candidate | Type | Null | Purpose |
| --- | --- | --- | --- | --- |
| match detail ID | `usr_evnt_noti_keywd_id` | `bigint unsigned` | N | PK |
| notification ID | `usr_evnt_noti_id` | `bigint unsigned` | N | notification history FK |
| keyword setting ID | `usr_evnt_keywd_set_id` | `bigint unsigned` | N | keyword setting FK |
| keyword snapshot | `keywd_nm` | `varchar(100)` | N | 해지/표현 변경 후에도 매칭 이유 보존 |
| audit fields | `crt_usr_logn_id`, `crt_dtm`, `mod_usr_logn_id`, `mod_dtm`, `del_yn` | existing convention | N | `BaseEntity` 패턴 |

제약과 인덱스 후보:

- PK: `usr_evnt_noti_keywd_id`
- unique: `(usr_evnt_noti_id, usr_evnt_keywd_set_id)`
- trace index: `(usr_evnt_keywd_set_id, crt_dtm desc)`

## 5. Relationship design

```text
User 1 --- N EventKeywordSetting
User 1 --- N EventKeywordNotification N --- 1 MiEvent
EventKeywordNotification 1 --- N EventKeywordNotificationMatch N --- 1 EventKeywordSetting
```

- 설정을 soft delete해도 기존 match detail은 보존한다.
- 사용자나 이벤트가 soft delete되어도 알림 스냅샷은 운영 보존 기간 동안 남긴다. 단, 현재 이벤트 상세 열기는 권한과 `del_yn`을 다시 검사한다.
- 물리 FK의 삭제/변경 정책은 DDL draft에서 확정한다. 이 기능은 자동 cascade delete를 필요로 하지 않는다.

## 6. Business rules

### 6.1 Keyword registration and uniqueness

1. 키워드는 양끝 공백을 제거한다.
2. 연속 공백은 하나의 공백으로 바꾼다.
3. Unicode는 NFC로 정규화한다.
4. 빈 문자열은 거절하고 최대 100자로 제한한다.
5. 대소문자 구분 없이 사용자별 동일 키워드는 하나만 허용한다. 이 규칙은 application 정규화와 DBA가 승인한 case-insensitive collation/unique 제약으로 방어한다.
6. 해지했던 동일 키워드를 다시 등록하면 기존 설정을 복구한다.

### 6.2 Match semantics

- 1차 권장안은 신규 `MiEvent.oneLineSummary`를 정규화한 문자열 안에 정규화된 키워드가 연속된 문구로 포함되는지 대소문자 무시 literal substring으로 판정하는 것이다.
- 기존 이벤트 목록 검색의 ngram OR 조건은 복합 키워드의 일부 토큰만 일치해도 참이 될 수 있어 알림 매칭에 그대로 재사용하지 않는다.
- 이벤트 상세 분석·태그를 포함하는 범위 확장은 별도 업무 결정이다.

### 6.3 Notification creation and duplicate prevention

1. 이벤트 생성 트랜잭션이 commit된 후에만 매칭을 시작한다.
2. `del_yn = N`인 키워드와 사용자만 대상으로 한다.
3. `User.usr_noti_set_yn = N`이면 새 알림을 생성하지 않는다. 다시 켜도 꺼져 있던 기간의 이벤트를 backfill하지 않는다.
4. 한 사용자-이벤트에 대해 알림 header는 최대 1건이다.
5. 한 이벤트가 여러 키워드와 맞으면 알림 header 1건과 match detail N건을 저장한다.
6. 재시도·중복 callback·다중 instance 경쟁에서도 `(usr_no, mi_event_id)` unique가 중복 header를 막는다.
7. 동시 생성 경쟁이 발생하면 승자가 만든 header를 재조회하고 누락된 match detail만 idempotent하게 추가한다. `idempotent`는 같은 작업을 여러 번 실행해도 결과가 한 번 실행한 것과 같다는 뜻이다.

### 6.4 Read and delete behavior

- 알림 생성 시 `inqr_dtm = null`이다.
- 첫 조회 처리는 `inqr_dtm`을 설정하며 이후 반복 요청에서 처음 시각을 바꾸지 않는다.
- 키워드 해지는 향후 매칭만 중단하며, 기존 알림과 match snapshot을 삭제하지 않는다.
- 1차 범위에는 사용자 알림 개별 삭제를 포함하지 않는다. 보존 기간과 일괄 정리는 운영/법무 정책에서 후속 확정한다.

### 6.5 Channel separation

- `EventKeywordNotification`의 생성은 알림 발생의 정본이다.
- 이메일 전송 성공/실패는 알림 발생 여부를 바꾸지 않는다.
- 이메일을 1차 범위에 포함하면 기존 `EmailAccessLog`의 상품 임계치 결합을 풀거나 별도 delivery history를 도입해야 한다. nullable FK 하나만 추가해 서로 다른 알림 유형을 모호하게 합치지 않는다.

### 6.6 `My Notification` integration

1. 이벤트 키워드 알림은 기존 상품 임계치 알림과 함께 한 `My Notification` 목록에 노출한다.
2. 저장 정본은 기존 `EmailAccessLog`와 신규 `EventKeywordNotification`으로 분리하고, 목록·미조회 수만 통합 읽기 모델로 합친다.
3. 통합 식별자는 `notificationType + sourceId`의 튜플로 해석한다. 트랜슥포트 필드명과 형식은 API feature spec이 소유한다.
4. 전체 목록은 두 원천을 먼저 합친 뒤 `occurredAt DESC`, 동일 시각에는 유형과 원천 ID를 타이브레이커로 정렬하고 pagination한다. 원천별로 먼저 pagination한 두 목록을 메모리에서 합치면 전체 페이징 정확성이 깨지므로 허용하지 않는다.
5. 미조회 수는 두 원천의 미조회 건수를 합산한다.
6. 조회 처리는 통합 식별자의 유형에 따라 `EmailAccessLog.isEmailInquiry` 또는 `EventKeywordNotification.inquiryAt`을 갱신한다.
7. 목록·상세·조회 처리는 모두 현재 인증 사용자의 소유권을 원천별로 확인한다.
8. 키워드 해지나 전체 알림 스위치 OFF는 이미 생성된 `My Notification` 이력을 숨기거나 삭제하지 않는다.
9. 두 원천 모두 각 알림의 발생 시각 기준 최근 7일 데이터만 통합 목록과 미조회 수에 포함한다.
10. 새 통합 API를 추가하고 `My Notification` 화면을 전환한다. 기존 API는 FE 전환과 호환 기간 종료가 확인될 때까지 유지한다.

## 7. Extension considerations

현재 모델을 바꾸지 않고 포함할 수 있는 가까운 확장만 고려한다.

- 매칭 필드를 `one_line_summary`에서 `event_title`, `detail_analysis`, `tags_json`으로 확장
- 키워드별 알림 켜기/끄기
- 앱·이메일·푸시 채널별 delivery history
- 이벤트 갱신 알림과 event revision 연결
- 키워드 AND/OR, 제외어, 이벤트 유형 필터

이 확장들은 현재 요구에 포함하지 않는다.

## 8. Data access recommendation by responsibility

### 8.1 Feature 1: user keyword settings

- 키워드 설정 CRUD: master JPA repository
- 설정 service는 정규화, 중복 확인, soft restore, 현재 사용자 소유권만 책임진다.
- matcher나 notification repository에 의존하지 않는다.

### 8.2 Feature 2: event matching and notification delivery

- 이벤트 하나의 텍스트와 활성 키워드 목록 매칭: 첫 버전은 application service에서 batch 처리
- 알림 header와 match detail 생성: master JPA transaction
- 사용자 `My Notification` 목록/미조회 수: `EmailAccessLog` + `EventKeywordNotification` 통합 projection
- 통합 목록: master DB native SQL의 `UNION ALL`로 공통 컬럼을 맞춘 후 DB에서 정렬·pagination하는 방식을 1차 권장안으로 한다. 요청값은 binding하고 SQL 구조를 동적으로 연결하지 않는다.
- 통합 상세/조회 처리: 알림 유형으로 원천 service/repository를 분기하고 현재 사용자 소유권을 재검증
- 채널 발송: 알림 저장 commit 후 별도 실행 경계
- 데이터가 커져 메모리 전체 스캔 비용이 문제가 되면 실측 결과를 근거로 ngram 역인덱스, 전용 검색 엔진, 키워드 분할 처리 중 하나를 다시 설계한다. 현재 단계에서 전용 인프라를 선제 도입하지 않는다.

## 9. API/DTO impact

API path, method, request/response, validation, error code는 후속 `docs/feature-specs/*`에서 확정한다. 다음 capability가 필요하다.

`Feature 1 - 사용자별 키워드 설정`:

- 현재 사용자의 키워드 등록
- 현재 사용자의 활성 키워드 목록 조회
- 현재 사용자 소유 키워드 해지

`Feature 2 - 이벤트-키워드 매칭 및 알림 발송`:

- 현재 사용자의 이벤트 키워드 알림 목록·미조회 수 조회
- 현재 사용자 소유 알림 조회 처리
- 시스템 소유 신규 이벤트 매칭 실행 진입점(callback/message/scheduler 중 하나)
- 기존 상품 임계치 알림과 이벤트 키워드 알림을 합친 `My Notification` 공통 목록/미조회 수
- 통합 식별자에 따른 유형별 상세/조회 처리

사용자용 API는 request에 `usr_no`를 받지 않고 인증 정보에서 소유자를 결정한다. 매칭 실행 진입점은 `SECURITY.md`의 시스템 역할·서명/callback 신뢰 규칙을 만족해야 한다.

## 10. SQL / DBA implications

- 신규 master DB 테이블 후보는 3개다.
- `ddl-auto=validate`이므로 애플리케이션 배포 전 DBA가 DDL을 수동 반영해야 한다.
- 운영 순서는 table/index/FK DDL 선반영 → application 배포 → 매칭 trigger 활성화가 안전하다.
- 기존 데이터 backfill은 1차 범위에 포함하지 않는다.
- 비활성 키워드 restore 정책을 지원할 unique 제약과 collation을 DBA가 검증해야 한다.
- 알림 header 중복 방지 unique와 다중 instance 경쟁 시 upsert/예외 처리 패턴을 ERD/DDL review에서 확정해야 한다.
- 통합 `My Notification` 조회는 기존 `EmailAccessLog` 조회 인덱스와 신규 inbox index가 각 분기의 사용자·기간·정렬 조건을 지원하는지 실행 계획으로 검증해야 한다.
- 정확한 DDL, rollback SQL, verify SQL, 보존/정리 SQL은 이 문서가 아니라 canonical DDL draft에서 소유한다.

## 11. Risks / open questions

### 11.1 Decisions required before full implementation

1. **Notification channel**: 앱 내 알림만 1차에 포함할지, 이메일도 포함할지 결정해야 한다.
2. **Matching field scope**: `one_line_summary` 단일 필드 권장안을 승인할지 결정해야 한다.
3. **Trigger owner**: 외부 Agent 후처리와 BE scanner 중 어느 쪽이 신규 event 완료 시점을 보장할지 확정해야 한다.

### 11.2 Resolved decisions

- `My Notification` 노출 기간: 모든 알림 유형에 동일하게 발생 시각 기준 최근 7일
- API 호환 전환: 새 통합 계약을 추가하고 `My Notification` 화면을 전환하며, 기존 API는 호환 기간 동안 유지

### 11.3 Trigger alternatives and evidence

- 권장: 이벤트를 쓰는 Agent/적재 파이프라인이 commit 완료 후 식별자를 callback/message로 전달한다. 생성 시점을 가장 명확하게 알 수 있다.
- 대안: BE 또는 외부 scheduler가 `crt_dtm`/`mi_event_id` 기준으로 신규 event를 스캔한다. 장애 기간의 누락을 막으려면 durable checkpoint와 overlap scan이 필요하며, 중복은 DB unique로 제거해야 한다.
- 두 방식을 동시에 구현하지 않는다. 운영 소유자와 신뢰 경계를 확정한 후 하나를 선택한다.

### 11.4 Other risks

- 키워드 수가 커지면 이벤트 하나당 전체 키워드 스캔 비용이 커질 수 있다. 예상 규모와 처리 지연 SLO를 API spec/ERD review 전에 확인한다.
- 짧은 키워드는 과도한 알림을 만들 수 있다. 최소 길이를 임의로 정하지 않고 제품 정책으로 확정한다.
- 알림 생성과 채널 전송을 하나의 비동기 작업으로 묶으면, 전송 실패가 중복 알림이나 누락으로 이어질 수 있다. 알림 저장 완료와 delivery retry를 분리해야 한다.

## 12. Exit criteria / handoff

- functional boundary: `사용자별 키워드 설정` → `이벤트-키워드 매칭 및 알림 발송`의 단방향 의존으로 안정화했다.
- domain model: 2개 기능이 `EventKeywordSetting`, `EventKeywordNotification`, `EventKeywordNotificationMatch` 3개 엔터티를 나누어 소유하는 구조는 안정적이다.
- API contract drafting readiness: 1번 기능은 독립적으로 계약을 작성할 수 있다. 2번 기능은 확정된 7일 노출·신규 통합 계약·기존 API 한시 유지 정책을 반영해 `My Notification` 유형/식별자/공통 필드/상세 분기를 정의해야 한다. trigger·매칭·채널 open question은 아직 남아 있다.
- delivery order: 1번 키워드 설정 계약/구현을 먼저 완료한 후 2번 매칭/알림 계약/구현을 진행한다.
- work management: 후속 구현은 두 기능이 독립 완료·배포 가능하므로 feature spec과 work note를 기능별로 분리한다. 이 도메인 설계 문서는 두 기능이 공유하는 업무 규칙의 정본으로 유지한다.
- full implementation readiness: `blocked by open questions`
- `backend-feature-spec`에서 다음을 확정해야 한다.
  - 알림 채널과 채널별 응답 필드
  - 키워드 최소/최대 길이, 최대 등록 개수, 중복 에러 처리
  - 매칭 필드 범위와 문구 일치 규칙
  - 알림 목록/상세/조회 처리의 페이징·정렬·에러 계약
  - `My Notification` 통합 알림 유형, 원천 식별자, 공통 필드, 유형별 상세, 신규 통합 계약과 기존 API의 호환 기간
  - 확정된 최근 7일 노출 정책의 경계 시각·timezone 처리
  - 전체 알림 스위치와 키워드 설정의 상호작용
- `erd-change-review`에서 다음을 검증해야 한다.
  - 테이블/컬럼/인덱스 물리명과 표준 용어 조합
  - FK 및 soft-delete 기반 보존 정책
  - case-insensitive unique에 사용할 collation과 Unicode 정규화 경계
  - `(usr_no, mi_event_id)` 중복 방지와 동시 생성 재시도
  - inbox/미조회 수/키워드 조회 인덱스
  - `EmailAccessLog` + `EventKeywordNotification` `UNION ALL` 조회의 정렬·pagination·count 실행 계획과 인덱스
  - 순차 배포, rollback, verify SQL, 보존 기간

## 13. Dictionary updates

- 추가한 행: 없음
- 사용한 기존 표준 용어:
  - `사용자(USR, USER)`
  - `이벤트(EVNT, EVENT)`
  - `키워드(KEYWD, KEYWORD)`
  - `알림(NOTI, NOTIFICATION)`
  - `설정(SET, SETTING)`
  - `요약(SMRY, SUMMARY)`
  - `스냅샷(SNPSHT, SNAPSHOT)`
  - `조회(INQR, INQUIRY)`
  - `일시(DTM, DATE AND TIME)`
- 정규화 결과를 따로 저장하지 않고 정규화된 키워드 자체를 `keywd_nm`에 저장하므로 `정규화` 신규 용어 등록은 필요하지 않다.

## 14. Document control / source-of-truth notes

- 이 문서는 이벤트 키워드 알림의 소유권, 생명주기, 중복 방지, 매칭 결과 집계 규칙의 정본이다.
- API path/method/request/response/validation/error code는 향후 `docs/feature-specs/*`를 참조하고 이 문서에서 중복 정의하지 않는다.
- table/column/index/FK/nullability/rollout/rollback/verify SQL은 향후 canonical DDL draft를 참조하고, 이 문서의 표는 논리 모델과 명명 후보만 제공한다.
- 전체 구현은 알림 채널, 매칭 필드 범위, 이벤트 commit 후 trigger 소유자가 확정될 때까지 차단된다.
- `My Notification` 통합 노출은 이 기능의 필수 업무 규칙이다. 최근 7일 노출과 신규 통합 계약으로의 전환은 확정된 업무 규칙이며, 정확한 path/field/error 계약과 호환 기간은 후속 feature spec이 소유한다.
