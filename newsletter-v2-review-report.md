# Newsletter V2 설계 vs 구현 비교 검토 보고서

**작성일:** 2026-03-20
**검토 범위:** 설계문서 8종 + 구현코드 114개 파일 + 테스트 8개 파일
**전체 정합성:** 약 93%

---

## 카테고리 A: 설계 vs 구현 불일치 (반드시 수정 필요)

### A-1. [CRITICAL] 낙관적 잠금 409 Conflict HTTP 상태 코드 미구현

- **설계:** `docs/newsletter-v2-design.md` L168~204 — `REVISION_VERSION_CONFLICT` → HTTP 409 반환 명시
- **구현:** `GlobalExceptionHandler.java` L411 — 모든 `AppErrorException`이 HTTP 400 BAD_REQUEST로 반환
- **영향:** 클라이언트가 버전 충돌(409)과 잘못된 요청(400)을 구분할 수 없음. 프론트엔드 충돌 해소 UX 구현 불가
- **수정안:**
  - `ResultCode`에 전용 충돌 코드 추가 (예: `BIZ_STATE_CONFLICT("BIZ-003", ...)`)
  - `GlobalExceptionHandler`에서 해당 코드를 409로 매핑하는 분기 추가
  - 또는 `MiNewsletterRevisionService`에서 직접 `ResponseStatusException(HttpStatus.CONFLICT, ...)` throw

### A-2. [MEDIUM] IllegalStateException 사용으로 인한 에러 처리 불일치

- **설계:** 모든 비즈니스 에러는 `AppErrorException` + `ResultCode`로 처리
- **구현:** 아래 파일들에서 `IllegalStateException`을 직접 throw
  - `BlockDataResolver.java` L46 — `UNKNOWN_TEMPLATE_BLOCK_STATE_REFERENCE`
  - `IssueStateLoader.java` L105, 149, 152, 157, 180, 183, 196, 207 — 다수 지점
  - `SnapshotAssembler.java` L36, 60, 66 — 스냅샷 조립 중 에러
- **영향:** 프론트엔드에 HTTP 500 Internal Server Error가 전달되어 에러 원인 파악 불가
- **수정안:** 모든 `IllegalStateException`을 `AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "구체적_코드")` 로 변경

### A-3. [MEDIUM] OpenAPI 스펙에 누락된 엔드포인트

- **설계:** `docs/newsletter-v2-openapi.yaml`에 정의되지 않은 구현된 엔드포인트:
  - `MiNewsletterIssueStateController` — `GET /newsletter/{id}/{issueNo}/preview`, `PUT /newsletter/{id}/{issueNo}/issue-state`
  - `InternalNewsletterController` — `GET /newsletter/{id}/{issueNo}/chart-data`
  - `MiNewsletterTriggerController` — `POST /newsletter/{id}/{issueNo}/trigger-mail`
  - `MiNewsletterSubscriptionController` — 전체 구독 관련 엔드포인트
- **영향:** API 문서와 실제 구현 불일치로 프론트엔드/외부 연동 혼란
- **수정안:** OpenAPI yaml에 누락된 엔드포인트 추가, 또는 내부 전용 API는 별도 태그로 분류

### A-4. [MEDIUM] GET /newsletter/{id}/{issueNo} 엔드포인트 분리 미문서화

- **설계:** OpenAPI L17-60 — 단일 엔드포인트로 정의
- **구현:** 두 개 컨트롤러로 분리
  - `MiNewsletterRevisionDedicatedController.java` L40 (`params="!companyNo"`)
  - `MiNewsletterDedicatedController.java` L61 (`params="companyNo"`)
- **영향:** OpenAPI 기반 코드 생성기가 올바르게 작동하지 않을 수 있음
- **수정안:** OpenAPI에 query parameter `companyNo` 조건 명시, 또는 URL path 분리

---

## 카테고리 B: 설계문서 업데이트 필요 (구현이 설계보다 앞서간 것)

### B-1. NewsletterTemplateBlock에 data_src_spec_json 컬럼 추가

- **설계:** DDL에 미정의
- **구현:** `NewsletterTemplateBlock.java` L73-74 — `@Column(name = "data_src_spec_json", columnDefinition = "json")` 존재
- **배경:** `docs/newsletter-v2-data-src-spec-contract.md` 문서에서 정의한 canonical spec contract를 실제로 구현한 것. DDL 문서에 반영 필요
- **조치:** `newsletter-template-v2-ddl-draft.sql`에 해당 컬럼 추가

### B-2. NewsletterUserRevision에 snapshotBaseType, snapshotNo 필드 추가

- **설계:** `docs/newsletter-v2-design.md`에 미명시
- **구현:** `NewsletterUserRevision.java` L62-69
  - `snpsht_base_type` (RevisionSnapshotBaseType: ORIGINAL | REVISION_SNAPSHOT)
  - `snpsht_no` (최신 스냅샷 번호 포인터)
- **배경:** 조회 시 delta 재생성 없이 최신 스냅샷을 직접 참조할 수 있는 성능 최적화
- **조치:** 설계문서에 해당 필드 및 사용 목적 문서화

### B-3. NewsletterUserRevisionSnapshotHistory에 schemaVersion 필드 추가

- **설계:** 미명시
- **구현:** Entity에 `schemaVersion` 필드 존재
- **배경:** 스키마 버전 호환성 관리를 위한 forward-looking 설계
- **조치:** 설계문서 및 DDL에 반영

### B-4. NewsletterUserRevisionSnapshotHistory에서 change_summary, edited_by, edited_at 미구현

- **설계:** `docs/newsletter-v2-design.md` L275-285 — optional 필드로 정의
- **구현:** Entity에 해당 필드 없음
- **판단:** optional 필드이므로 당장 기능 영향 없으나, audit trail 강화를 위해 향후 추가 고려
- **조치:** 설계문서에 "Phase 2 예정" 등으로 명시하거나, 구현 추가

### B-5. original-snapshot 엔드포인트의 companyNo 파라미터 조건

- **설계:** OpenAPI L161-177 — companyNo 파라미터 미언급
- **구현:** `MiNewsletterDedicatedController.java` L79 — `params="companyNo"` 조건 포함
- **조치:** OpenAPI에 companyNo query parameter 추가

---

## 카테고리 C: 코드 개선 제안

### C-1. [HIGH] Strategy 패턴 등록 검증 부재

- **파일:** `BlockDataResolver.java` L51-52
- **현상:** `strategies.getOrDefault(block.dataSourceType(), strategies.get("STATIC"))` — 미등록 전략이 자동으로 STATIC으로 fallback
- **위험:** 새로운 BlockTypeCode 추가 시 전략 미구현을 감지 못함. 잘못된 데이터가 조용히 생성될 수 있음
- **개선안:**
  - 애플리케이션 시작 시 모든 dataSourceType에 대한 Strategy 등록 여부 검증
  - fallback 대신 명시적 에러 throw (`throw new AppErrorException(...)`)
  - 또는 `@PostConstruct`에서 BlockTypeCode enum 값 대비 Strategy bean 존재 확인

### C-2. [LOW] MiNewsletterService 유틸리티 로직 분리

- **파일:** `MiNewsletterService.java` L235-246
- **현상:** Base64 이미지 인코딩 로직이 비즈니스 서비스에 혼재
- **개선안:** 이미지 인코딩 로직을 별도 유틸리티 클래스로 분리

### C-3. [LOW] CHECK 제약이 Entity 레벨에서 미검증

- **대상:** `NewsletterIssueSectionState`, `NewsletterIssueBlockState` 등 다수 엔티티
- **현상:** `del_yn IN ('N','Y')`, `vsblty_yn IN ('N','Y')` 등 CHECK 제약이 DDL에만 존재하고 Java Entity에 validation 없음
- **영향:** DB 레벨에서는 보장되지만, 단위 테스트에서 잘못된 값이 감지되지 않음
- **개선안:** `@Pattern` 또는 custom validator 추가 고려

---

## 카테고리 D: 테스트 보강 필요

### 현재 상황 요약

| 클래스 | 테스트 메서드 수 | 커버리지 평가 |
|--------|:---:|--------|
| BlockDataResolverTest | 2 | 낮음 |
| BlockExecutionClassifierTest | 2 | 낮음 |
| IssueStateLoaderTest | 3 | 중간 |
| NewsletterIssuePreviewWriteServiceTest | 3 | 중간 |
| SnapshotAssemblerTest | 3 | 중간 |
| TemplateLoaderTest | 2 | 낮음 |
| TemplateTreeTest | 1 | 낮음 |
| NewsEventBlockDataStrategyTest | 1 | 매우 낮음 |
| **합계** | **17** | — |

### D-1. [CRITICAL] MiNewsletterRevisionService 테스트 전무

- **현상:** Revision lifecycle (draft → save → publish), 충돌 감지, delta 이력 관리 등 핵심 로직에 대한 테스트 없음
- **필요 테스트:**
  - Revision 생성/조회/저장 정상 경로
  - `revisionNo` 불일치 시 충돌 감지 (409 시나리오)
  - Delta history JSON 변경사항 추적
  - Snapshot base 관리 (ORIGINAL vs REVISION_SNAPSHOT)
  - 동시 수정 시 DB-level 낙관적 잠금 (`updateWithExpectedRevisionNo`)
  - Draft 자동저장 vs 명시적 저장 분기
- **예상 테스트 수:** 8-10개

### D-2. [CRITICAL] MiNewsletterPublishService 테스트 전무

- **현상:** 비동기 차트 캡처 + 메일 발송 오케스트레이션 테스트 없음
- **필요 테스트:**
  - Async 실행 성공/실패 경로
  - JWT 생성 및 유효성
  - Playwright 호출 실패 처리 (timeout, network error)
  - 발송 실패 이력 DB 저장
- **예상 테스트 수:** 6-8개

### D-3. [CRITICAL] MiNewsletterMailService 테스트 전무

- **현상:** 메일 발송 핵심 로직 검증 없음
- **필요 테스트:**
  - 메일 발송 성공/실패 경로
  - Inline image/CID 참조 처리
  - Email address validation
  - Retry logic (MAX 3회)
  - 수신자 목록 해석 (직접 입력 vs 구독자 목록)
  - HTML body sanitization
- **예상 테스트 수:** 10-12개

### D-4. [CRITICAL] NewsletterPublishPipelineService 테스트 전무

- **현상:** Template → IssueState → ResolvedData → Snapshot 전체 파이프라인 통합 테스트 없음
- **필요 테스트:**
  - End-to-end publish 성공 경로
  - 각 단계 실패 시 에러 전파
  - 신규 issue original 생성 vs 기존 업데이트
  - Transaction atomicity 검증
- **예상 테스트 수:** 6-8개

### D-5. [CRITICAL] NewsletterDetailV2Service 테스트 전무

- **필요 테스트:**
  - V2 schema original snapshot 조회
  - Revision 존재 시 revision content 우선 선택
  - UserNo 없을 때 original fallback
  - Subscription 상태 포함 여부
- **예상 테스트 수:** 5-6개

### D-6. [HIGH] BlockDataStrategy 구현 테스트 부족

| Strategy | 테스트 존재 | 필요 테스트 |
|----------|:---:|-----|
| StaticBlockDataStrategy | ❌ | Canonical/legacy resolve, default value, transform |
| ChartSeriesBlockDataStrategy | ❌ | DEDICATED/COLLECTION/MAIN chart 분기, query 파라미터 |
| DynamicQueryBlockDataStrategy | ❌ | PREFERENCE_TABLE refId 해석, canonical/legacy fallback |
| NewsEventBlockDataStrategy | ✅ (1개) | Null 필드 조합, empty list, event order |

- **예상 추가 테스트 수:** 15-20개

### D-7. [HIGH] BlockExecutionClassifier 테스트 부족 (2개 → 10개+ 필요)

- **누락 시나리오:**
  - Top-level key 검증 (5개 허용 키 제한)
  - Family × Locator 매트릭스 전체 조합
  - Query requiredness matrix (STATIC: 금지, CHART_SERIES+COLLECTION: 필수 등)
  - Assign fieldKey ↔ BlockField 매칭
  - Transform type enum 제한
  - OverridePolicy whitelist 검증

### 테스트 보강 우선순위 요약

| 우선순위 | 대상 | 현재 | 목표 |
|---------|------|------|------|
| 🔴 P0 | MiNewsletterRevisionService | 0개 | 8-10개 |
| 🔴 P0 | NewsletterPublishPipelineService | 0개 | 6-8개 |
| 🔴 P0 | MiNewsletterMailService | 0개 | 10-12개 |
| 🔴 P0 | MiNewsletterPublishService | 0개 | 6-8개 |
| 🔴 P0 | NewsletterDetailV2Service | 0개 | 5-6개 |
| 🟠 P1 | BlockDataStrategy 4종 | 1개 | 16-20개 |
| 🟠 P1 | BlockExecutionClassifier | 2개 | 10-12개 |
| 🟡 P2 | 기존 테스트 엣지케이스 보강 | 14개 | 25-30개 |
| **합계** | — | **17개** | **약 90-110개** |

---

## 부록: 전체 정합성 요약

| 영역 | 정합도 | 판정 |
|------|:---:|------|
| 도메인 모델 (Entity) | 95% | 거의 완벽, 일부 필드 선행 구현 |
| 상태 관리 (Enum/전이) | 100% | 완벽 일치 |
| DDL ↔ Entity 매핑 | 98% | data_src_spec_json 1건 선행 |
| 필드 매핑 (camelCase ↔ snake_case) | 100% | 완벽 일치 |
| API 엔드포인트 | 85% | OpenAPI에 4개 컨트롤러 누락 |
| Block Meta/Field 가이드라인 | 100% | 완벽 준수 |
| 데이터 소스 스펙 Contract | 98% | 완벽에 가까움 |
| Issue State/Snapshot 파이프라인 | 99% | 완벽에 가까움 |
| 템플릿 계층 구조 | 100% | 완벽 일치 |
| 에러 처리 | 70% | 409 미구현, IllegalStateException 혼재 |
| 테스트 커버리지 | ~15% | 핵심 서비스 5개 테스트 전무 |
