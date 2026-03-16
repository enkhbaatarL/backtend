# 뉴스레터 차트 이미지 캡처 명세서

## 1. 목적 및 배경

뉴스레터 메일 발송 시 Chart.js(+ DOM 의존 커스텀 플러그인, canvas 합성 로직)로 렌더링된
차트 이미지가 필요하다. 서버 사이드에서 재현이 불가능하므로 실제 Chromium 브라우저를
헤드리스로 구동하는 **Playwright Java**를 Spring Boot 내에서 직접 사용한다.

- 이미지 생성 시점: 뉴스레터 **PUBLISHED** 전환 시 1회 실행
- Playwright가 프론트엔드 렌더 페이지를 열고 `window.__mailContents` 추출
- 추출한 `htmlBody` + `images`(base64)로 구독자에게 즉시 메일 발송

---

## 2. 전체 흐름

```
[Airflow DAG - 뉴스레터 발행 완료 시]

  POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/trigger-mail
    → 202 Accepted 즉시 반환 (비동기 실행)

Spring Boot (@Async - MiNewsletterPublishService)
  1. newsletterId로 NewsletterMaster 조회 → customerCompanyNo 확보
  2. 단기 JWT 발급 (5분 TTL, newsletterId + issueNo + companyNo 포함)
  3. renderUrl = frontendBaseUrl + "/newsletter-chart-render?token=<JWT>"
  4. ChartRendererClient.capture(renderUrl) 호출 (Playwright Java 직접 실행)

Playwright Java (Chromium headless - Spring Boot 내)
  5. Headless Chrome으로 renderUrl 오픈
  6. /internal/newsletter/{newsletterId}/{issueNo}/chart-data 호출하여 데이터 로드
  7. React 앱 렌더링 → 차트 + 메일 본문 완성
  8. body[data-charts-ready="true"] 감지 (최대 30초 대기)
  9. window.__mailContents 에서 { htmlBody, images } 추출

Spring Boot
  10. 추출한 mailContents로 구독자에게 개별 발송 (각자 본인만 To에 노출)
```

---

## 3. 프론트엔드 명세 (프론트 팀 작업)

### 3-1. 추가할 라우트

| 항목 | 내용 |
|---|---|
| 경로 | `/newsletter-chart-render` |
| 쿼리 파라미터 | `token` (JWT, 필수) |
| 인증 | 세션/쿠키 불필요. JWT만 검증 |
| 렌더링 대상 | 메일 본문 전체 (차트 포함) |

### 3-2. JWT 검증

```typescript
// 백엔드 RSA-256 공개키로 JWT 서명 검증
// 페이로드에서 newsletterId, issueNo, companyNo 추출하여 차트 데이터 API 호출
const { newsletterId, issueNo, companyNo } = verifyAndDecode(token)
```

검증 실패 시 → 렌더링 없이 종료 (빈 페이지 또는 에러 표시)

### 3-3. 차트 데이터 로딩

기존 뉴스레터 상세 API를 인증 없이 호출할 수 없으므로,
**JWT를 Authorization 헤더에 담아** 백엔드 내부 전용 API를 호출한다.

```
GET /internal/newsletter/{newsletterId}/{issueNo}/chart-data
Authorization: Bearer <JWT>
```

> 응답 구조는 기존 뉴스레터 상세조회(`NewsletterDetailRes`)와 동일

### 3-4. window 객체 인터페이스 (Playwright와의 계약)

메일 본문 렌더링 및 모든 차트 캡처 완료 후 아래 두 가지를 설정해야 한다.

```typescript
// 타입 정의
interface MailContents {
  htmlBody: string                   // 완성된 메일 HTML (cid 참조 포함)
  images: MailImage[]
}

interface MailImage {
  cid: string                        // HTML 내 참조 ID (예: "uuid_1")
  data: string                       // base64 인코딩된 PNG (data URL 접두사 제외)
  contentType: string                // 예: "image/png"
}

// 완료 시 설정
;(window as any).__mailContents = {
  htmlBody: "<html><body>...<img src='cid:uuid_1'>...</body></html>",
  images: [
    { cid: 'uuid_1', data: 'iVBORw0KGgo...', contentType: 'image/png' },
    { cid: 'uuid_2', data: 'iVBORw0KGgo...', contentType: 'image/png' },
  ],
}
document.body.setAttribute('data-charts-ready', 'true')
```

- `htmlBody` 내 `<img src="cid:xxx">` 와 `images[].cid` 가 1:1 매핑되어야 함
- `data` 필드는 `data:image/png;base64,` 접두사 **제외**한 순수 base64 값
- 설정 시점: 차트 렌더링 + 메일 본문 구성이 모두 완료된 후

### 3-5. 차트 없는 경우 처리

뉴스레터에 차트가 없으면 즉시 완료 신호를 보낸다.

```typescript
;(window as any).__mailContents = {
  htmlBody: "<html><body>...</body></html>",
  images: [],
}
document.body.setAttribute('data-charts-ready', 'true')
```

---

## 4. Playwright Java (Spring Boot 내장)

별도 마이크로서비스 없이 Spring Boot 내에서 Playwright Java를 직접 실행한다.

### 4-1. 의존성

```gradle
// build.gradle
implementation 'com.microsoft.playwright:playwright:1.50.0'
```

### 4-2. ChartRendererClient 핵심 로직

```java
try (Playwright playwright = Playwright.create();
     Browser browser = playwright.chromium().launch(
         new BrowserType.LaunchOptions()
             .setHeadless(true)
             .setArgs(List.of(
                 "--no-sandbox",
                 "--disable-setuid-sandbox",
                 "--disable-dev-shm-usage",  // Docker /dev/shm 64MB 제한 우회
                 "--disable-gpu"
             ))
     );
     BrowserContext context = browser.newContext()) {

    Page page = context.newPage();
    page.navigate(renderUrl, new Page.NavigateOptions()
        .setWaitUntil(WaitUntilState.NETWORKIDLE));

    // 렌더링 완료 대기
    page.waitForSelector("body[data-charts-ready=\"true\"]",
        new Page.WaitForSelectorOptions().setTimeout(30000));

    // window.__mailContents 추출
    Map<String, Object> mailContents =
        (Map<String, Object>) page.evaluate("() => window.__mailContents");
}
```

### 4-3. 반환 타입

```java
// ChartRendererClient.java
public record MailContents(String htmlBody, List<CapturedImage> images) {}
public record CapturedImage(String cid, String data, String contentType) {}
```

---

## 5. Spring Boot 명세 (백엔드 팀 작업)

### 5-1. 발행 트리거 API

Airflow DAG이 뉴스레터 발행 완료 시 호출한다.

```
POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/trigger-mail
Authorization: Bearer <OAuth2 토큰>  (Role: SYSTEM / ADMIN / API_SUB_ADMIN / API_PLUS_OPERATOR)

{
  "subject": "2026년 3월 마켓 인텔리전스 뉴스레터"
}
```

- 202 Accepted 즉시 반환 (fire-and-forget)
- 실제 캡처 + 발송은 `@Async` 백그라운드 실행 (`MiNewsletterPublishService.captureAndSendAsync`)
- `companyNo`는 `newsletterId`로 `NewsletterMaster`를 조회하여 내부에서 자동 결정

### 5-2. JWT 발급 스펙

| 항목 | 값 |
|---|---|
| 알고리즘 | RS256 (기존 RSA 키 재사용) |
| TTL | 5분 |
| 키 | 기존 `JwtProperties` privateKey / publicKey |

**페이로드**
```json
{
  "newsletterId": 1,
  "issueNo": 22,
  "purpose": "chart-capture",
  "iat": 1741478400,
  "exp": 1741478700
}
```

### 5-3. 내부 전용 차트 데이터 API

```
GET /internal/newsletter/{newsletterId}/{issueNo}/chart-data
Authorization: Bearer <chart-capture JWT>
```

- `purpose: chart-capture` 클레임 검증
- JWT 서명/만료/purpose 검증 후 `MiNewsletterService.getNewsletterDetailForSystem()` 위임 (company 필터 없음)
- 응답: 기존 `NewsletterDetailRes` 동일 구조
- Spring Security 인증 우회 (`/internal/**` → tokenByPassSecurityFilterChain)

### 5-4. 비동기 처리 흐름

```java
// MiNewsletterPublishService
@Async
public void captureAndSendAsync(Integer newsletterId, Integer issueNo, String subject) {
    Integer companyNo = resolveCompanyNo(newsletterId);  // DB 조회
    String jwt = chartCaptureJwtService.issueToken(newsletterId, issueNo, companyNo);
    String renderUrl = properties.getFrontendBaseUrl()
        + "/newsletter-chart-render?token=" + jwt;

    MailContents mailContents = chartRendererClient.capture(renderUrl);
    miNewsletterMailService.sendOriginalIssueMailFromCapture(
        newsletterId, issueNo, subject, mailContents);
}
```

### 5-5. 메일 발송 정책

- 수신자: 구독 테이블 조회 (현재 하드코딩, 추후 `newsletter_subscription` 테이블로 교체)
- 발송 방식: **개별 발송** - 각 수신자가 To 필드에 본인 주소만 확인 가능
- 재시도: 수신자별 최대 3회, 1초 간격

### 5-6. 설정 (`application-{profile}.yml`)

```yaml
chart-capture:
  frontend-base-url: https://front.example.com  # 프로파일별 설정
  jwt-ttl-seconds: 300
  capture-timeout-ms: 30000
```

---

## 6. 보안 고려사항

| 항목 | 정책 |
|---|---|
| 트리거 API 인증 | OAuth2 토큰 필수 (Role.SYSTEM 이상) |
| `/newsletter-chart-render` 라우트 | chart-capture JWT 없으면 접근 불가 |
| `/internal/**` API | Spring Security bypass + chart-capture JWT 자체 검증 |
| JWT 알고리즘 | RS256 - 기존 RSA 키 인프라 재사용 |
| JWT TTL | 5분 (캡처 완료 후 무효화) |
| purpose 클레임 | `chart-capture` 고정값 검증으로 일반 사용자 JWT 재사용 방지 |

---

## 7. 담당 작업 요약

### 프론트엔드 팀

- [ ] `/newsletter-chart-render?token=JWT` 라우트 추가
- [ ] JWT 검증 로직 구현 (RSA-256 공개키)
- [ ] `/internal/newsletter/{newsletterId}/{issueNo}/chart-data` 호출하여 데이터 로드
- [ ] 메일 본문 렌더링 + 차트 캡처 완료 후 `window.__mailContents` 설정
- [ ] `body[data-charts-ready="true"]` 속성 설정
- [ ] 차트 없는 경우(0개) 즉시 완료 신호 처리

### 백엔드 팀

- [x] `ChartCaptureProperties` 설정 클래스 구현
- [x] `ChartCaptureJwtService` - JWT 발급/검증 구현
- [x] `/internal/newsletter/{newsletterId}/{issueNo}/chart-data` API 구현
- [x] Security config `/internal/**` bypass 추가
- [x] `ChartRendererClient` - Playwright Java 직접 실행 구현
- [x] `MiNewsletterMailService.sendOriginalIssueMailFromCapture()` 구현 (개별 발송)
- [x] `POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/trigger-mail` 트리거 API 구현 (202 Accepted, `@Async`)
- [ ] `trigger-mail` 멱등/상태 관리 테이블(`newsletter_send_trigger`) 설계 및 서비스 검증 로직 추가
- [ ] 메일 발송 이력(`newsletter_send_history`) 설계 시 `trigger-mail` 멱등/상태 테이블을 함께 확정
- [ ] 구독자 테이블 구현 후 수신자 조회 로직 교체

---

## 8. 변경 이력

- 2026-03-09: 초안 작성
- 2026-03-09: Playwright 마이크로서비스 → Playwright Java (Spring Boot 내장) 방식으로 변경
- 2026-03-09: `window.__chartImages` → `window.__mailContents` 계약으로 변경 (htmlBody + images 통합)
- 2026-03-09: NCP Object Storage 저장 단계 제거, 캡처 후 즉시 발송으로 단순화
- 2026-03-09: JWT 알고리즘 HS256 → RS256 (기존 RSA 키 재사용)으로 변경
- 2026-03-09: 트리거 API 경로 `/api/v1/external/**` → `/api/v1/mi/newsletter/**` 변경, OAuth2 인증 적용
- 2026-03-09: `trigger-mail` 멱등/상태 분리 설계(`newsletter_send_trigger` + `newsletter_send_history`) 반영
- 2026-03-09: 트리거 요청 파라미터에서 `companyNo` 제거, `subject`만 수신
- 2026-03-09: JWT 페이로드에서 `companyNo` 제거 — 내부 조회는 `getNewsletterDetailForSystem()`으로 company 필터 우회
- 2026-03-09: 프론트 테스트용 chart-capture-url API 제거 (trigger-mail API로 대체)
