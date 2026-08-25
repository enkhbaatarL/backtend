# MCP Read-Only API Feature Spec

## Document Control

- status: Approved
- owner: BE
- created at: 2026-08-24
- updated at: 2026-08-25
- canonical for:
  - MCP 채널의 endpoint / request / response / HTTP error 계약
  - `X-User-Id` 헤더 계약
  - MCP 채널에 endpoint를 추가하는 절차
- not canonical for:
  - 채널의 존재 이유, 경로 체계, trust model, 인증 단계와 운영 배포 게이트:
    `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
  - 실행 계획, 변경 파일, 검증 결과: `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`
  - 원본 API의 업무 의미와 필드 정의: `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md` 및 각 원본 API의 OpenAPI 문서
- source documents:
  - `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
  - 코드 조사 (2026-08-24 / 2026-08-25, branch `feature/gas2`)
- depends on:
  - `docs/conventions/openapi-documentation-conventions.md`
  - `docs/conventions/source-of-truth-governance.md`
  - branch `feature/gas2`
- supersedes: 없음
- last synced: 2026-08-25
- open questions:
  - **Q2 (운영 MCP client 인증·권한 방식)** — MCP client는 모든 삭제되지 않은 유효 사용자의 신뢰된 위임자로 확정됐다.
    정확한 인증 기술, credential owner, 권한 표현과 회전 방식은 운영 배포 전 확정·구현한다 (9절).
  - **Q7 (MCP 서버의 호출 대상 서버)** — **사용자 검토 중**. MCP 서버는 서버를 2개 알아야 한다 — 일반 서버(4.1·4.2)와 SK Gas 전용 서버(4.3). 전용 서버는 subdomain이 다르다. 상세는 4.4절
  - **Q9 (MCP 채널의 배포 대상 backend)** — **미확정**. 전용 backend가 skgas/ske/skhy 3개다. 배포 범위로 정리되면 코드 변경은 없다. 4.5.1절 참조

---

## 1. Feature summary

- 목적: MCP(Model Context Protocol) 서버가 Next Gen MI 조회 데이터를 읽어갈 수 있는 **전용 API 채널**의 계약을 정의한다.
- 성격: 기존 조회 API의 **서비스 계층을 그대로 재사용**하는 얇은 위임 채널이다. 새로운 업무 로직이 없다.
- 핵심 계약 3가지:
  1. 화면용 **사용자 JWT는 사용하지 않는다.** 개발 단계에는 제한된 인증 예외를 쓰고, 운영 전 MCP client 인증을 붙인다.
  2. `X-User-Id`는 호출 시스템이 아니라 **effective user(위임 사용자)**를 지정한다. 이 헤더 자체는 인증 수단이 아니다.
  3. **성공 응답의 `data` 계약은 원본 API와 동일하다.** `/mcp` 한 조각을 뺀 경로가 원본이다.

### 1.1 원본 API와 다른 점 / 같은 점

| 항목 | MCP 채널 |
|---|---|
| 경로 | 원본 경로의 `/api/v1` 뒤에 `/mcp` 삽입 |
| 인증 | 개발: 승인된 임시 예외 / 운영: MCP client 인증 필수 (Q2) |
| 위임 사용자 식별 | `X-User-Id` 헤더 (원본: 사용자 JWT claim) |
| 요청 파라미터 | **원본과 동일** |
| 성공 응답 `data` | **원본과 동일** — 필드·타입·null 의미 전부 |
| 응답 래핑 | **원본과 동일** `BaseResponse` |
| Swagger 그룹 | `MCP API` 단독 (원본: `SK Gas API`) |

---

## 2. Scope

### 2.1 In scope

- 읽기 전용(GET) 조회 API의 MCP 경로 노출
- `X-User-Id` 기반 사용자 컨텍스트 주입
- MCP 전용 Swagger 그룹 분리
- effective user를 기존 API 접근 로그에 기록

### 2.2 Out of scope

- 쓰기 API (POST / PATCH / DELETE) 노출
- SSE 스트림 엔드포인트 노출 — MCP 호출 모델과 맞지 않는다
- 서비스 / 레포지토리 계층 로직 변경 또는 복제
- MCP 서버 자체 구현
- 멀티테넌시(고객사) 필터 신설
- **Deprecated된 조회 API 3종** 노출 — 원본에 `@Deprecated`가 붙어 있다 (목록은 8.2절)
- **화면 조작 · 폴링 · 실행/설계 상태 보조 API** 노출 — 수급모델 실행 6종(도메인 전체)과 설계·상태 화면 4종. MCP 소비 대상이 아니다 (2026-08-24 사용자 지정, 목록은 8.2절)

---

## 3. 채널 규약

### 3.1 Base path

```
원본:  /api/v1/mi/next-gen/{...}
MCP :  /api/v1/mcp/mi/next-gen/{...}
```

`/api/v1` 뒤에 `/mcp` 한 조각만 들어간다. 그 외 경로 구조는 원본과 1:1로 같다.

### 3.2 인증

- 화면용 사용자 Bearer JWT는 사용하지 않는다.
- 개발 단계에서는 승인된 개발 환경의 읽기 전용 GET MCP 경로만 임시 인증 예외로 둔다.
- 운영 단계에서는 MCP client 인증이 필수다. 정확한 credential 전달 방식은 Q2 확정 후 이 계약과 OpenAPI에 반영한다.
- 인증되지 않은 운영 요청은 `401`로 거부한다.

### 3.3 `X-User-Id` 헤더 계약

| 항목 | 내용 |
|---|---|
| 헤더명 | `X-User-Id` |
| 값 | MI Platform 사용자 PK (`tmd` 사용자 테이블의 `usr_no`, 정수) |
| 필수 여부 | **필수** — 모든 MCP endpoint에 적용 |
| 예시 | `X-User-Id: 1024` |

**서버가 검증하는 것**: 그 값이 정수로 파싱되고, 삭제되지 않은 사용자로 DB에 실재하며,
소속 고객사가 사용·미삭제 상태이고 현재일이 고객사 사용 기간 안에 있는가.

**서버가 검증하지 않는 것**: MCP client와 effective user가 같은 주체인지. MCP client는 사용자를 대신하는 시스템 주체다.

> **이 헤더는 인증 수단이 아니다.** 개발 단계에서는 위조 가능한 값이고,
> 운영 단계에서는 인증된 MCP client만 이 위임 값을 보낼 수 있어야 한다.
> 다만 일부 endpoint(4.1.4 `/records`, 4.1.6 `/{eventId}/scenario-groups`)는 **응답 내용이 이 헤더 값에 따라 달라진다.**
> 상세한 보안 경계는 9절.

`X-Company-No`는 MCP 요청 계약이 아니다. 요청에 포함돼도 서버는 이 값을 조건 없이 무시하며,
타사 접근 가능 ADMIN의 상품 권한 대상은 서버가 DB에서 해석한 SKGAS 고객사로 고정한다.

### 3.4 응답 래핑

모든 응답은 원본 API와 동일하게 `BaseResponse`로 감싼다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "...",
  "data": { }
}
```

- `status`: 성공 여부 (boolean)
- `code`: 결과 코드 — 성공/실패 판정 기준
- `message`: 결과 메시지
- `data`: 실제 페이로드. **여기 구조가 원본 API와 동일하다.**

### 3.5 페이징 응답 구조

페이징 endpoint의 `data`는 `PaginationRes` 구조다.

| 필드 | 타입 | 의미 |
|---|---|---|
| `totalPages` | int | 전체 페이지 수 |
| `totalElements` | int | 전체 데이터 개수 |
| `currentPage` | int | 현재 페이지 (1-based) |
| `numberOfElements` | int | 현재 페이지 항목 수 |
| `size` | int | 페이지당 최대 항목 수 |
| `isFirst` / `isLast` | boolean | 첫/마지막 페이지 여부 |
| `hasPrevious` / `hasNext` | boolean | 이전/다음 페이지 존재 여부 |
| **`detail`** | array | **항목 배열** |

> **주의**: 항목 배열의 필드명은 `content`가 아니라 **`detail`**이다.
> 원본 API의 일부 Swagger 설명이 항목을 "content 항목 필드"라고 서술하지만, 실제 JSON 필드는 `detail`이다.

---

## 4. API endpoints

### 4.1 이벤트 (확정 — 2026-08-21)

모든 endpoint의 원본은 `/api/v1/mi/next-gen/events` 하위이며, 요청/응답 계약이 원본과 동일하다.
필드별 업무 의미는 원본 API의 OpenAPI 문서를 따른다 — 여기서 복제하지 않는다.

#### 4.1.1 이벤트 목록 조회

- `GET /api/v1/mcp/mi/next-gen/events`
- 원본: `GET /api/v1/mi/next-gen/events` (`EventService.getEvents`)
- 사용자 컨텍스트: **미사용** (A군)

**요청 (query parameter, 전부 선택)**

| 파라미터 | 타입 | 기본값 | 허용값 / 비고 |
|---|---|---|---|
| `eventStatus` | string | `all` | `all` / `new` / `updated` (대소문자 무관) |
| `eventTypeL1Id` | long[] | 빈 목록 | 이벤트 유형 Lv1 ID, 복수 지정 가능 |
| `startDate` | date | 없음 | `yyyy-MM-dd`. 최신 연관 레코드 최초 생성일시 기준 |
| `endDate` | date | 없음 | `yyyy-MM-dd`. 동일 기준 |
| `sort` | string | `latest` | `latest` / `oldest` (대소문자 무관) |
| `keyword` | string | `""` | 이벤트 제목 검색 키워드 |
| `suggestedOnly` | boolean | `false` | 시나리오화 추천 항목만 |
| `page` | int | `1` | 1-based, **최솟값 1** |
| `size` | int | `20` | **최솟값 1** |

**응답**: `data` = `EventListRes` (원본과 동일)

#### 4.1.2 이벤트 상세 조회

- `GET /api/v1/mcp/mi/next-gen/events/{eventId}`
- 원본: `GET /api/v1/mi/next-gen/events/{eventId}` (`EventService.getEventDetail`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: `eventId` (path, long, 필수)

**응답**: `data` = `EventDetailRes` (원본과 동일)

#### 4.1.3 이벤트 기관 전망 조회

- `GET /api/v1/mcp/mi/next-gen/events/{eventId}/outlooks`
- 원본: `GET /api/v1/mi/next-gen/events/{eventId}/outlooks` (`EventService.getEventOutlooks`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**

| 파라미터 | 위치 | 타입 | 기본값 | 비고 |
|---|---|---|---|---|
| `eventId` | path | long | — | 필수 |
| `page` | query | int | `1` | 1-based, 최솟값 1 |
| `size` | query | int | `10` | 최솟값 1 |

**응답**: `data` = `PaginationRes<EventInstitutionOutlookRes>`. 전망 일자(`viewDate`) 내림차순.

#### 4.1.4 이벤트 연관 레코드 조회 — **응답이 `X-User-Id`에 따라 달라짐**

- `GET /api/v1/mcp/mi/next-gen/events/{eventId}/records`
- 원본: `GET /api/v1/mi/next-gen/events/{eventId}/records` (`EventService.getEventRecords`)
- 사용자 컨텍스트: **필수** (B군)

**요청**

| 파라미터 | 위치 | 타입 | 기본값 | 비고 |
|---|---|---|---|---|
| `eventId` | path | long | — | 필수 |
| `page` | query | int | `1` | 1-based, 최솟값 1 |
| `size` | query | int | `10` | 최솟값 1 |

**응답**: `data` = `PaginationRes<EventRelatedRecordRes>`. 발행 일시(`publishedAt`) 내림차순.

> **`X-User-Id` 의존**: 각 항목의 `isPermitted`(상품 조회 권한 여부)가
> SKGAS USER이면 헤더가 지목한 사용자의 개인+회사 상품 권한으로 계산된다.
> 타사 접근 가능 ADMIN이면 ADMIN 소속 고객사가 아니라 서버가 DB에서 해석한 SKGAS 고객사의
> 회사 단위 상품 권한으로 계산된다.
> 같은 `eventId`라도 `X-User-Id`의 역할과 개인 상품 권한에 따라 `isPermitted` 값이 달라질 수 있다.
> 목록에 포함되는 레코드 자체는 동일하며, 달라지는 것은 `isPermitted` 판정이다.

#### 4.1.5 이벤트 유형 L1 목록 조회

- `GET /api/v1/mcp/mi/next-gen/events/event-types/lv1`
- 원본: `GET /api/v1/mi/next-gen/events/event-types/lv1` (`EventService.getEventTypesL1`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: 파라미터 없음

**응답**: `data` = `List<EventTypeL1Res>`

이벤트 목록 필터의 `eventTypeL1Id`에 넣을 ID값을 제공한다. 4.1.1과 함께 사용한다.

#### 4.1.6 이벤트 연결 시나리오 그룹 목록 — **응답이 `X-User-Id`에 따라 달라짐**

- `GET /api/v1/mcp/mi/next-gen/events/{eventId}/scenario-groups`
- 원본: `GET /api/v1/mi/next-gen/events/{eventId}/scenario-groups` (`EventService.getEventScenarioGroups`)
- 사용자 컨텍스트: **필수** (B군)

**요청**: `eventId` (path, long, 필수)

**응답**: `data` = `List<EventLinkedScenarioGroupRes>`. 그룹 최신순. 연결된 그룹이 없으면 빈 목록.

> **`X-User-Id` 의존**: 각 그룹의 `mine`(설계중 그룹의 진행자가 그 사용자인지)이
> **헤더가 지목한 사용자 기준으로 계산된다.** 설계중이 아닌 그룹은 `null`이다.
> 나머지 필드는 헤더와 무관하다.

### 4.2 시나리오 그룹 (확정 — 2026-08-24)

#### 4.2.1 시나리오 그룹 목록 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups`
- 원본: `GET /api/v1/mi/next-gen/scenario-groups` (`ScenarioGroupService.getScenarioGroups`)
- 사용자 컨텍스트: **미사용** (A군)

**요청 (query parameter, 전부 선택)**

| 파라미터 | 타입 | 기본값 | 허용값 / 비고 |
|---|---|---|---|
| `startDate` | date | 없음 | `yyyy-MM-dd`. 그룹 생성일시 기준 |
| `endDate` | date | 없음 | `yyyy-MM-dd`. 동일 기준 |
| `sort` | string | `latest` | `latest` / `oldest` (대소문자 무관) |
| `keyword` | string | `""` | 시나리오 그룹명 검색 키워드 |
| `page` | int | `1` | 1-based, 최솟값 1 |
| `size` | int | `20` | 최솟값 1 |

**응답**: `data` = `ScenarioGroupListRes` (원본과 동일)

#### 4.2.2 시나리오 그룹 상세 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}`
- 원본: `GET /api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}` (`ScenarioGroupService.getScenarioGroupDetail`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: `scenarioGroupId` (path, long, 필수)

**응답**: `data` = `ScenarioGroupDetailRes` (원본과 동일)

> **응답 지연 주의**: v4 그룹인 경우 내부적으로 시나리오 설계 Agent를 호출한다.
> Agent 호출이 실패하면 예외를 던지지 않고 `evidence` 필드만 `null`로 내려간다 — 원본과 동일한 동작이다.

#### 4.2.3 개별 시나리오 목록 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/scenarios`
- 원본: `GET /api/v1/mi/next-gen/scenario-groups/scenarios` (`ScenarioGroupService.getScenarioGroupScenarios`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: 4.2.1과 동일한 query parameter 집합 (`startDate` / `endDate` / `sort` / `keyword` / `page` / `size`).
단 기준이 다르다 — 기간·정렬은 **시나리오 생성일시**, `keyword`는 **시나리오명** 검색이다.

**응답**: `data` = `ScenarioIndividualListRes`. 모든 그룹의 시나리오를 평탄화한 평면 목록이며, 각 행이 부모 그룹 참조를 가진다.

#### 4.2.4 개별 시나리오 상세 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/scenarios/{scenarioId}`
- 원본: 동일 경로 (`ScenarioGroupService.getScenarioDetail`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: `scenarioGroupId`, `scenarioId` (path, long, 둘 다 필수)

**응답**: `data` = `ScenarioDetailViewRes`. 내러티브, 주요 가정, Signpost 정의·모니터링 이력을 포함한다.

#### 4.2.5 시나리오 그룹 가격전망 그래프 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/graph`
- 원본: 동일 경로 (`ScenarioGroupService.getScenarioGroupGraph`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**

| 파라미터 | 위치 | 타입 | 기본값 | 허용값 |
|---|---|---|---|---|
| `scenarioGroupId` | path | long | — | 필수 |
| `indexName` | query | string | `JKM` | `JKM` / `TTF` / `HH` |
| `periodUnit` | query | string | `monthly` | `monthly` / `yearly` (대소문자 무관) |

**응답**: `data` = `ScenarioGroupGraphRes`. 단일 인덱스 × 단일 기간단위다. CME 기준선은 포함되지 않는다 — 4.3 참조.

#### 4.2.6 가격전망 해석 조회

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/price-interpretations`
- 원본: 동일 경로 (`PriceInterpretationService.getInterpretations`)
- 사용자 컨텍스트: **미사용** (A군)

**요청**: `scenarioGroupId` (path, long, 필수)

**응답**: `data` = `PriceInterpretationSectionRes`

- `generatable`: 해석 생성 가능 여부 (화면 버튼 활성용 — MCP 소비자는 무시해도 된다)
- `interpretations[]`: 해석을 보유한 시나리오별 본문 (`scenarioId`, `scenarioName`, `content`, `revision`, `updatedAt` 등)

### 4.3 CME 벤치마크 기준선 (추가 요청 — 2026-08-24) · **환경 제약 있음**

#### 4.3.1 계약

- `GET /api/v1/mcp/mi/next-gen/scenario-groups/graph/cme-benchmark`
- 원본: `GET /api/v1/mi/next-gen/scenario-groups/graph/cme-benchmark` (`CmeBenchmarkService.getCmeBenchmarkGraph`)
- 사용자 컨텍스트: **미사용** (A군 — `CmeBenchmarkService`는 `AuthorizationService`를 호출하지 않는다)

**요청 (query parameter, 전부 선택)**

| 파라미터 | 타입 | 기본값 | 허용값 |
|---|---|---|---|
| `indexName` | string | `JKM` | `JKM` / `TTF` / `HH` |
| `periodUnit` | string | `monthly` | `monthly` / `yearly` (대소문자 무관) |

**응답**: `data` = `CmeBenchmarkGraphRes` (원본과 동일)

- `periodUnit` / `periodUnitLabel`: 기간 단위 (`monthly` 월별 / `yearly` 연도별)
- `dates`: x축 기간 목록 (월별 `yyyy-MM` / 연도별 `yyyy`)
- `baseSeries`: CME 기준선 — `seriesId` / `seriesName` / `values` (`dates`와 동일 순서)
- 가격 값은 소수 2자리 반올림(HALF_UP), 연도별은 해당 연 월값의 평균
- 표출 기간: 월별 `-6M ~ MI BASE 마지막` / 연도별 `-1Y ~ 2030년`

단일 인덱스 × 단일 기간단위만 반환한다. 여러 인덱스가 필요하면 인덱스 수만큼 호출한다.

#### 4.3.2 환경 제약 — **다른 endpoint와 뜨는 환경이 반대다**

원본 컨트롤러가 서로 배타적인 프로파일에 걸려 있다.

| 프로파일 | 4.1 이벤트 · 4.2 시나리오 그룹<br>(`@Profile("!dedicated")`) | 4.3 CME 벤치마크<br>(`@Profile({"dedicated","testdb"})`) |
|---|:--:|:--:|
| `dev` (개발계) | 있음 | **없음** |
| `prod` (운영계) | 있음 | **없음** |
| `dedicated` (SK Gas 전용) | **없음** | 있음 |
| `testdb` (로컬) | 있음 | 있음 |

**두 묶음이 동시에 존재하는 환경은 로컬(`testdb`)뿐이다.**

데이터소스도 분리돼 있다.

- `CmeBenchmarkServiceImpl`은 `DataSourceContextHolder`로 `midb-skgas` 테넌트에 라우팅한다
  (설정 키: `mi.next-gen.cme.benchmark.datasource-key`, 기본값 `${SKT_MI_DB_DEDICATED_NAME:midb-skgas}`).
- 이 테넌트 키는 `SKT_MI_DB_DEDICATED_NAME` 환경변수로 결정된다. 실제 SK Gas 전용 배포 환경에는 `midb-skgas`로 설정되어 있다 (2026-08-24 사용자 확인).
  저장소 자체의 하드코딩 기본값은 서로 다르다 — 서비스 코드는 `midb-skgas`, `application-dedicated.yml`의 테넌트는 `midb-skens`.
  `application-dev.yml`·`application-testdb.yml`에는 `midb-skgas` 테넌트가 아예 없어, 로컬 `testdb`에서는 이 endpoint를 실데이터로 검증할 수 없다 — CME 검증은 실제 SK Gas 전용 배포 환경에서만 수행한다.
- 따라서 **컨트롤러 프로파일만 풀어도 개발계에서는 조회가 실패한다.** 데이터소스가 없다.

근거는 원본 컨트롤러 클래스 주석에 있다 — CME 데이터는 고객사 전용 DB(`midb-skgas`)에 적재되어 일반 서버에서 접근할 수 없다.

#### 4.3.3 대응 — 컨트롤러만 분리, `@Profile` 미부착 (확정 2026-08-24)

**전용 DB에 접근하는 API는 컨트롤러 클래스를 분리한다. 다만 Spring `@Profile`로 막지는 않는다.**
어느 서버가 무엇을 서비스하는지는 **배포 설정**이 정한다.

| 구분 | MCP 컨트롤러 | `@Profile` | 담는 endpoint |
|---|---|---|---|
| 일반 | `McpEventController` | `!dedicated` | 4.1 이벤트 **6건** |
| 일반 | `McpScenarioGroupController` | `!dedicated` | 4.2 시나리오 그룹 **6건** |
| **전용** | `McpScenarioGroupDedicatedController` | **없음** | 4.3 CME 벤치마크 **1건** |

`McpScenarioGroupController`는 `ScenarioGroupService`와 `PriceInterpretationService` 두 개를 주입한다
(원본은 컨트롤러가 나뉘어 있으나, MCP는 `/scenario-groups` 경로를 한 컨트롤러로 묶는다).

- 채널 인프라(인터셉터 · Security matcher · Swagger 그룹)는 **prefix 단위로 걸리므로 그대로 재사용한다.** 전용 컨트롤러를 위해 따로 손댈 설정이 없다.
- 구현 비용은 컨트롤러 클래스 1개 추가다.
- 저장소에 선례가 있다 — `ScenarioModelExecutionDedicatedController`도 `@Profile` 없이 클래스 이름만 `Dedicated`다.

**향후 전용 endpoint를 추가할 때도 이 컨트롤러에 넣는다.** 일반 컨트롤러에 섞지 않는다.

#### 4.3.4 일반 서버에서 호출했을 때 — **응답을 신뢰하지 말 것**

`@Profile`이 없으므로 이 endpoint는 일반 서버(`dev` / `prod`)에도 등록되고 해당 서버의 `MCP API` Swagger에도 나타난다.
**그러나 일반 서버에서는 유효한 데이터를 얻을 수 없다.**

호출해도 "사용할 수 없다"는 오류가 오지 않는다. 데이터소스 라우팅이 실패 대신 폴백하기 때문이다.

| 일반 서버에서 호출한 결과 | 조건 |
|---|---|
| `500` (SQL 오류) | 일반 DB에 CME 테이블이 없을 때 |
| **`200` + 빈 `baseSeries`** | 테이블은 있으나 비어 있을 때 |

> **소비자 주의**: 일반 서버에서 받은 빈 결과를 "CME 데이터가 없다"는 사실로 해석하면 안 된다.
> 이 endpoint는 **SK Gas 전용 서버(subdomain이 다르다)로만 호출한다.** 4.4절 참조.

이 경고는 OpenAPI `description`에도 동일하게 노출한다.

### 4.4 호출 대상 서버 (Q7 — 검토 중)

**MCP 서버는 base URL을 두 개 알아야 한다.**

| 대상 | 서버 | 해당 endpoint |
|---|---|---|
| 일반 서버 | `dev` / `prod` 프로파일 | 4.1 이벤트 (6건), 4.2 시나리오 그룹 (6건) |
| SK Gas 전용 서버 | `dedicated` 프로파일 | 4.3 CME 벤치마크 (1건) |

- SK Gas 전용 서버는 **subdomain이 다르다.** 이것은 **배포 설정에서 정해지며 애플리케이션 코드에는 없다** — 저장소를 뒤져도 전용 서버 base URL 설정은 나오지 않는다.
- 화면(FE)은 이미 같은 구조로 동작한다. 전용 endpoint만 별도 base URL로 호출해 결과를 합친다.
- 전용 서버는 개발계 · 운영계 양쪽에 존재한다. 개발계 MCP 서버는 개발계 전용 서버를, 운영계 MCP 서버는 운영계 전용 서버를 호출하는 구성이 자연스럽다.

**미확정**: MCP 서버가 두 서버를 모두 호출할지, 한쪽만 볼지. 이 결정은 계약 자체를 바꾸지는 않는다 —
어느 쪽이든 endpoint 계약은 위와 같고, 달라지는 것은 MCP 서버가 어느 base URL로 어느 endpoint를 부르느냐다.
따라서 **이 결정을 기다리지 않고 구현에 착수할 수 있다.**

### 4.5 서버별 endpoint 노출 (확정 — 2026-08-24)

**개발계 활성 프로파일** (사용자 확인, 2026-08-24)

| backend | 활성 프로파일 |
|---|---|
| 일반 | `dev`, `common`, `custom`, `test` |
| 전용 (skgas / ske / skhy 각각 별도 기동) | `dev`, `dedicated` |

`common` / `custom` / `test`는 **이 저장소에서 아무것도 가르지 않는다** — `@Profile` 애노테이션 전수와
`spring.config.activate.on-profile` 문서 전수를 확인했고 어디에서도 참조하지 않는다. 배포 측 설정 주입용으로 보인다.
코드 동작을 가르는 것은 `dev`와 `dedicated` 둘뿐이다.

**결과 — MCP endpoint의 서버별 상태**

| backend | 4.1 이벤트 (6건) · 4.2 시나리오 그룹 (6건) | 4.3 CME (1건) |
|---|:--:|---|
| 일반 | **노출 · 정상** | 노출 · **데이터 신뢰 불가** (4.3.4절 — 일반 DB로 폴백) |
| 전용 · skgas | 미노출 (`!dedicated`가 false) | **노출 · 정상** |
| 전용 · ske | 미노출 | 배포하면 노출되나 **무의미** (4.5.1절) |
| 전용 · skhy | 미노출 | 배포하면 노출되나 **무의미** (4.5.1절) |

> **소비자에게 주는 의미**: 13건을 모두 쓰려면 **서버 두 곳**을 호출해야 한다.
> 이벤트·시나리오 12건은 **일반 backend**, CME 1건은 **skgas 전용 backend**.
> CME가 정상 동작하는 서버는 **skgas 전용 backend 하나뿐**이다.

#### 4.5.1 전용 backend가 3개다 — MCP 배포 대상 결정 필요

전용 backend는 skgas / ske / skhy 3개가 따로 뜨고, 셋 다 활성 프로파일이 `dev,dedicated`로 같다.
MCP CME 컨트롤러에는 `@Profile`이 없으므로, **MCP 채널을 배포하는 전용 backend마다 이 endpoint가 뜬다.**

CME는 SK Gas 데이터이고 라우팅 키는 `midb-skgas`로 고정이다(변경 예정 없음).
따라서 ske / skhy backend에 배포하면 그 endpoint는 의미 있는 결과를 주지 못한다.

**결정 필요 (Q9): MCP 채널을 어느 backend에 배포하는가?**

| 배포 대상 | 결과 |
|---|---|
| 일반 backend + **skgas** 전용 backend | 13건 모두 의미 있게 동작. 추가 조치 없음 |
| ske / skhy 전용 backend에도 배포 | 그곳의 CME endpoint는 무의미. 노출을 막으려면 `SKGAS_COMPANY_USER_ONLY` 애노테이션 부착(코드 1줄)을 검토한다 |

배포 범위로 통제되면 코드 변경은 없다.

---

## 5. 오류 계약

### 5.1 채널 공통 오류 (`X-User-Id` 관련)

모든 MCP endpoint에 공통 적용된다. 컨트롤러 진입 전 인터셉터 단계에서 판정한다.

| 상황 | HTTP | `code` | 비고 |
|---|---|---|---|
| `X-User-Id` 헤더 없음 또는 공백 | `401` | `AUTH-001` | |
| `X-User-Id`가 정수로 파싱되지 않음 | `400` | `REQ-003` | 예: `abc`, `1.5` |
| 해당 `usr_no` 사용자가 없거나 삭제됨 | `401` | `AUTH-001` | 존재 여부를 응답으로 구분해주지 않기 위해 헤더 부재와 같은 코드를 쓴다 |
| effective user의 소속 고객사가 미사용·삭제 상태이거나 현재일이 사용 기간 밖임 | `401` | `AUTH-001` | 사용자·고객사의 세부 상태를 응답으로 구분하지 않는다 |

오류 응답도 `BaseResponse` 포맷이다.

```json
{
  "status": false,
  "code": "AUTH-001",
  "message": "Authentication is required for this request.",
  "data": null
}
```

### 5.2 endpoint별 오류

원본 API와 동일하다. 대표적으로:

| 상황 | HTTP | 비고 |
|---|---|---|
| `page` / `size`가 1 미만 | `400` | Jakarta Validation |
| 존재하지 않는 `eventId` / `scenarioGroupId` | 원본과 동일 | 원본 API의 계약을 따른다 |

### 5.3 결정 사항 — AUTH-001의 HTTP status

`AUTH-001`은 `AppHttpStatusException`으로 **401**을 명시해 던진다.

- 저장소 기본값인 `AppErrorException`을 쓰면 HTTP `400`이 되는데, 인증 컨텍스트 부재를 `400`으로 내리면 MCP 서버 쪽에서 파라미터 오류와 구분하기 어렵다.
- `REQ-003`(숫자 파싱 실패)은 실제로 파라미터 형식 오류이므로 `AppErrorException` 기본값 `400`을 그대로 쓴다.

---

## 6. 접근 로그

MCP 호출은 기존 API 접근 로그에 **`SYSTEM`이 아니라 `X-User-Id`가 지정한 effective user로** 기록된다.

- 인터셉터가 `MDCUtil.REQUEST_USER_ID`에 값을 기록한다.
- 사용자 컨텍스트가 주입되므로 기존 로그 경로에 위임 사용자가 남는다. MDC 기록은 방어적 보완이다.
- MCP client identity와 인증 결과는 MCP 인증 계층 또는 Infra 접근 로그에서 추적한다.
- 두 로그는 request/trace id로 연결할 수 있어야 한다.
- 기존 인증 경로의 로그 동작은 바뀌지 않는다.

---

## 7. Swagger 노출

| 항목 | 내용 |
|---|---|
| 그룹 / 표시명 | `MCP API` |
| 매칭 경로 | `/api/v1/mcp/**` |
| 인증 스키마 | 개발 단계에는 사용자 `BearerAuth`를 노출하지 않음. 운영 MCP client 인증 방식은 Q2 확정 후 반영 |
| `X-User-Id` | 필수 헤더 파라미터로 노출 |
| 기존 그룹 중복 노출 | **없음** — `ETC API` 그룹의 `pathsToExclude`에 `/api/v1/mcp/**`를 추가한다 |

`SK Gas API` 그룹은 `/api/v1/mi/next-gen/**`만 매칭하므로 MCP 경로와 겹치지 않는다. 별도 조치가 필요 없다.

---

## 8. 엔드포인트 추가 규칙

대상 API 목록은 앞으로 계속 늘어난다. 추가 절차는 다음과 같다.

### 8.1 절차

0. **선정 원칙을 먼저 적용한다.** 그 endpoint가 **시장 정보 콘텐츠**를 주는가, 아니면 **화면이 다음에 무엇을 그릴지 판단하는 상태값**을 주는가.
   후자면 추가하지 않는다 (폴링용 상태, 설계 화면 데이터, 실행 lifecycle, 입력 폼 분기 등). 8.2절 제외 목록 참조.
1. **분류를 확인한다.** 대상 서비스 메서드가 `AuthorizationService`를 (직간접으로) 호출하는지 본다.
   - 호출하지 않으면 **A군**, 호출하면 **B군**.
2. **컨트롤러 메서드를 추가한다.** A군·B군 모두 이 한 단계로 끝난다.
   - Security / Swagger / 인터셉터 설정은 **건드리지 않는다.** prefix 단위로 이미 걸려 있다.
   - 요청/응답 DTO는 원본 것을 그대로 재사용한다.
3. **B군을 추가하는 경우**, 응답이 `X-User-Id` 값에 따라 달라진다는 점을 이 문서 4절에 명시한다.
4. **원본 컨트롤러의 `@Profile`을 확인한다.** Next Gen MI 컨트롤러는 두 종류다.
   - `@Profile("!dedicated")` — 일반 서버 (dev / prod / local)
   - `@Profile({"dedicated","testdb"})` — 고객사 전용 서버. 전용 DB 테넌트를 읽으므로 일반 서버에서 동작하지 않는다
   **전용 DB에 접근하는 endpoint는 `McpScenarioGroupDedicatedController`(또는 새 전용 컨트롤러)에 넣는다.** 일반 컨트롤러에 섞지 않는다.
   MCP 전용 컨트롤러에는 `@Profile`을 붙이지 않으므로, **일반 서버에서 호출됐을 때의 동작을 확인하고 description에 경고를 남긴다** (4.3.4절 참조).
5. **쓰기 API나 SSE**를 추가하려는 경우는 이 채널의 범위 밖이다. 별도 설계 검토가 필요하다.

### 8.2 확장 후보 인벤토리 (Next Gen MI 조회 API 전수)

`◯` = 현재 대상, `-` = 미대상.

**A군 — 사용자 컨텍스트 미사용**

| 대상 | 원본 엔드포인트 | 서비스 메서드 |
|:--:|---|---|
| ◯ | `GET /events` | `EventService.getEvents` |
| ◯ | `GET /events/{eventId}` | `EventService.getEventDetail` |
| ◯ | `GET /events/{eventId}/outlooks` | `EventService.getEventOutlooks` |
| ◯ | `GET /events/event-types/lv1` | `EventService.getEventTypesL1` |
| ◯ | `GET /scenario-groups` | `ScenarioGroupService.getScenarioGroups` |
| ◯ | `GET /scenario-groups/{id}` | `ScenarioGroupService.getScenarioGroupDetail` |
| ◯ | `GET /scenario-groups/scenarios` | `ScenarioGroupService.getScenarioGroupScenarios` |
| ◯ | `GET /scenario-groups/{gid}/scenarios/{sid}` | `ScenarioGroupService.getScenarioDetail` |
| ◯ | `GET /scenario-groups/{id}/graph` | `ScenarioGroupService.getScenarioGroupGraph` |
| ◯† | `GET /scenario-groups/graph/cme-benchmark` | `CmeBenchmarkService.getCmeBenchmarkGraph` |
| ◯ | `GET /scenario-groups/{id}/price-interpretations` | `PriceInterpretationService.getInterpretations` |

† 2026-08-24 추가 확정. **전용 DB(`midb-skgas`) 접근** — MCP도 별도 컨트롤러로 분리하되 `@Profile`은 붙이지 않는다. **SK Gas 전용 서버로만 호출해야 한다** (4.3절).

**B군 — 사용자 컨텍스트 필수 (응답이 `X-User-Id`에 따라 달라짐)**

| 대상 | 원본 엔드포인트 | 헤더에 따라 달라지는 것 |
|:--:|---|---|
| ◯ | `GET /events/{eventId}/records` | 항목별 `isPermitted` (상품 조회 권한) |
| ◯ | `GET /events/{eventId}/scenario-groups` | 그룹별 `mine` (설계 점유자가 본인인지) |

**대상 제외**

- SSE 스트림 endpoint 전체 (`.../stream`) — MCP 호출 모델과 맞지 않음
- 쓰기 API (POST / PATCH / DELETE) — 채널 범위 밖
- **Deprecated 3종** — 원본 컨트롤러에 `@Deprecated`가 붙어 있다. nextgen의 `@Deprecated` GET endpoint 전부다.
  - `GET /events/design-availability` — 이벤트 조합 설계 상태 조회 API로 대체됨
  - `GET /scenario-groups/{id}/design` — v1 설계 세션 상태 조회 (2026-08-24 사용자 지정)
  - `GET /scenario-groups/{id}/dimension-templates` — v1 분기항목/분기기준 템플릿 조회 (2026-08-24 사용자 지정)
**선정 원칙 (2026-08-24 확립)**: **화면 조작 · 폴링 · 실행/설계 상태 보조 API는 제외한다.**
MCP가 읽어갈 것은 시장 정보 콘텐츠이지, 화면이 다음에 무엇을 그릴지 판단하는 데 쓰는 상태값이 아니다.
아래 두 묶음이 이 원칙으로 제외됐다.

- **수급모델 실행 관련 조회 6종 — 이 도메인 전체** — **사용자 지정 제외 (2026-08-24)**.
  수급모델 실행의 내부 상태·파라미터를 다루는 API다.
  **이 6종으로 수급모델 실행 도메인의 조회 API가 전부 제외된다** (SSE `.../model-param-proposals/stream`은 SSE 규칙으로 이미 제외).
  - `GET /scenarios/{id}/model-execution-options` — 실행 입력 방식(화면 입력 폼 분기)
  - `GET /scenarios/{id}/model-param-proposals` — 현재 파라미터 제안 조회
  - `GET /scenarios/{id}/model-param-proposals/{pid}` — 파라미터 제안 상태/결과
  - `GET /scenarios/{id}/model-executions/{runId}` — 비동기 실행 lifecycle 상태
  - `GET /scenarios/{id}/model-executions/latest/.../confirmed-parameters` — 최근 실행 확정 파라미터
  - `GET /scenarios/{id}/model-executions/latest/.../proposed-parameters` — 최근 실행 원 제안 파라미터
- **시나리오 설계·실행 상태 화면 계열 4종** — **사용자 지정 제외 (2026-08-24)**.
  - `GET /scenario-groups/{id}/graph/run-status` — 그래프 실행 상태 (화면 폴링용)
  - `GET /scenario-groups/{id}/presentation` — v4 설계 화면 데이터 (생성·정련 상태, 분기값 옵션)
  - `GET /scenario-groups/{id}/design/additional-dimensions` — v4 추가 분기항목 (설계 화면 입력 후보)
  - `GET /scenario-groups/design-status` — 이벤트 조합 설계 점유 판정 (B군이지만 같은 기준으로 제외)

---

## 9. 보안 경계

### 9.1 소비자가 알아야 하는 사실

- 개발 단계의 임시 인증 예외에서는 경로를 아는 호출자가 요청할 수 있고 `X-User-Id`도 위조 가능하다.
- 운영 단계에서는 인증된 MCP client만 호출하고 사용자 위임을 수행할 수 있어야 한다.
- `/records`의 `isPermitted`는 SKGAS USER이면 effective user의 개인+회사 상품 권한,
  타사 접근 가능 ADMIN이면 서버가 해석한 SKGAS 고객사의 회사 단위 상품 권한으로 계산된다.
- MCP 요청의 `X-Company-No`는 계약에 없으며, 숫자·비숫자와 관계없이 상품 권한 대상 결정에 사용하지 않는다.
- 고객사·역할·datasource는 헤더로 직접 선택하지 않고 서버가 조회한 사용자 정보와 서버 설정에서 결정한다.

### 9.2 개발 단계 임시 인증 예외

**2026-08-24 사용자 결정**: 짧은 개발 기간에는 임시 인증 예외로 진행하되 무인증 상태로 운영 배포하지 않는다.

| 환경 | 상태 |
|---|---|
| 로컬 / 개발계 | 승인된 환경의 읽기 전용 GET MCP 경로만 임시 인증 예외 |
| 운영계 | **MCP client 인증·권한 방식 구현 전 배포 금지** |

### 9.3 운영 배포 게이트

**Q2가 확정·구현되기 전에는 운영계에 배포하지 않는다.**

MCP client는 모든 삭제되지 않은 유효 사용자의 신뢰된 위임자다. 이 권한은 인증된 MCP client에만 부여한다.
인증 기술, credential owner, endpoint 권한 표현과 회전 방식은 설계문서 Q2에서 확정한다.

- 미인증 운영 요청은 `401`이어야 한다.
- 위임 사용자가 없거나 삭제된 상태이면 실패해야 한다.
- matcher overlap, 미인증 위임, effective user 범위를 넘는 고객사·datasource 변조,
  외부 `X-Company-No` 무시, 서로 다른 고객사 요청 간 컨텍스트 누출, CME 오라우팅을 음수 테스트한다.

---

## 10. Acceptance criteria

- [ ] 개발용 인증 예외 설정에서 MCP 경로를 사용자 Bearer JWT 없이 호출하면 `200`이 반환된다.
- [ ] 운영 보안 설정에서 인증되지 않은 MCP client 요청은 `401`로 거부된다.
- [ ] 운영 보안 설정에서 인증된 MCP client만 `X-User-Id` 위임을 수행할 수 있다.
- [ ] MCP 응답 본문(`data`)이 원본 API 응답과 **동일하다** (동일 파라미터·동일 사용자 기준 diff 결과 무차이).
- [ ] `X-User-Id` 없음 → `401` / `AUTH-001`.
- [ ] `X-User-Id`가 비숫자 → `400` / `REQ-003`.
- [ ] `X-User-Id`가 미존재·삭제된 사용자 → `401` / `AUTH-001`.
- [ ] effective user의 소속 고객사가 미사용·삭제·사용 시작 전·사용 종료 후 → `401` / `AUTH-001`.
- [ ] `/records`의 `isPermitted`가 SKGAS USER의 개인+회사 상품 권한으로 계산된다 (권한 있는 사용자 / 없는 사용자 각각 확인).
- [ ] 타사 접근 가능 ADMIN의 `/records.isPermitted`가 ADMIN 소속 회사와 무관하게 SKGAS 회사 단위 상품 권한으로 계산된다.
- [ ] `X-Company-No`에 다른 실제 고객사 번호 또는 비숫자 값을 보내도 값이 무시되고, 헤더 미전송 요청과 동일한 SKGAS 상품 권한 결과가 나온다.
- [ ] `/{eventId}/scenario-groups`의 `mine`이 헤더 사용자 기준으로 계산된다 (점유자 본인 / 타인 각각 확인).
- [ ] API 접근 로그에 `SYSTEM`이 아닌 effective user로 기록된다.
- [ ] MCP 인증/Infra 로그에서 MCP client identity를 추적하고 request/trace id로 API 로그와 연결할 수 있다.
- [ ] Swagger UI에 `MCP API` 그룹이 단독 노출되고, `MI` / `SK Gas` / `ETC` 그룹에 중복 노출되지 않는다.
- [ ] MCP 그룹에 사용자 `BearerAuth`가 노출되지 않고, `X-User-Id`가 필수 헤더로 노출된다.
- [ ] Q2 확정 후 MCP 그룹의 인증 스키마가 실제 MCP client 인증 방식과 일치한다.
- [ ] 기존 API의 동작·계약·권한이 변경되지 않는다 (회귀 테스트 baseline 대비 무변화).
- [ ] MCP 경로 밖에서는 사용자 컨텍스트 합성이 일어나지 않는다.
- [ ] 요청 정상·예외 종료 후 `SecurityContext`가 다음 요청에 남지 않는다.
- [ ] 요청값으로 effective user의 고객사 범위를 넓히거나 datasource를 바꾸는 시도가 실패한다.
- [ ] 서로 다른 고객사 사용자의 연속 요청 간 `SecurityContext`와 tenant context가 누출되지 않는다.
- [ ] CME 벤치마크의 OpenAPI `description`에 "SK Gas 전용 서버 전용 · 일반 서버 응답 신뢰 불가" 경고가 노출된다.
- [ ] 일반 서버에서 CME를 호출했을 때의 실제 반환(500 또는 빈 목록)을 확인해 4.3.4절 표를 사실로 확정한다.
- [ ] 4.5절 서버별 노출표대로 실제 노출된다 (일반 backend에 12건, skgas 전용 backend에 CME 1건).
- [ ] 전용 컨트롤러도 동일한 `X-User-Id` 계약과 오류 계약을 따른다 (채널 인프라 재사용 확인).

---

## 11. 관련 문서

- 설계 근거 / 결정 로그: `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
- 실행 계획 / 검증 기록: `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`
- 원본 도메인 기능 정의: `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md`
- OpenAPI 서술 규칙: `docs/conventions/openapi-documentation-conventions.md`
- 소유권 규칙: `docs/conventions/source-of-truth-governance.md`
