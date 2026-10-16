# MCP Read-Only API Feature Spec

## Document Control

- status: Approved
- owner: BE
- created at: 2026-08-24
- updated at: 2026-10-09
- canonical for:
  - MCP 채널의 endpoint / request / response / HTTP error 계약
  - `X-User-Id` 헤더 계약
  - MCP 채널에 endpoint를 추가하는 절차
- not canonical for:
  - 채널의 존재 이유, 경로 체계, trust model, 인증 단계와 운영 배포 게이트:
    `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
  - 실행 계획, 변경 파일, 검증 결과:
    - `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`
    - `docs/work-notes/2026/2026-08-25-x-user-id-login-id-first-match-implementation.md`
    - `docs/work-notes/2026/2026-10-09-mcp-event-snapshot-api.md` (이벤트 Snapshot, 4.6절)
  - 원본 API의 업무 의미와 필드 정의: `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md` 및 각 원본 API의 OpenAPI 문서
- source documents:
  - `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
  - 코드 조사 (2026-08-24 / 2026-08-25, branch `feature/gas2`)
- depends on:
  - `docs/conventions/openapi-documentation-conventions.md`
  - `docs/conventions/source-of-truth-governance.md`
  - branch `feature/gas2`
- supersedes: 없음
- last synced: 2026-10-09 — 현재 코드 기준: MCP Basic Auth 인증(backend PR #595), MCP 컨트롤러 전부 `dedicated` (`d60ebbc0`), 이벤트 Snapshot(4.6절)
- open questions:
  - ~~Q2 (운영 MCP client 인증·권한 방식)~~ — **해소 (2026-10-08, PR #595)**: MCP 인증 키(`tmd_mcpauthinfom`)로 Basic Auth 인증한다.
    키는 관리자 화면에서 회사 단위로 발급하며 키 회사와 위임 사용자 회사가 같아야 한다 (3.2절, 9절).
  - ~~Q7 (MCP 서버의 호출 대상 서버)~~ — **해소**: MCP 채널 전체가 SK Gas 전용 서버(`dedicated`)에만 있으므로 MCP 서버는 base URL 하나만 쓴다 (4.4절).
  - **Q9 (MCP 채널의 배포 대상 backend)** — **미확정**. 전용 backend가 skgas/ske/skhy 3개이고 셋 다 `dedicated` 프로파일이라 배포하면 MCP 채널이 모두 뜬다. 4.5.1절 참조

---

## 1. Feature summary

- 목적: MCP(Model Context Protocol) 서버가 Next Gen MI 조회 데이터를 읽어갈 수 있는 **전용 API 채널**의 계약을 정의한다.
- 성격: 기존 조회 API의 **서비스 계층을 그대로 재사용**하는 얇은 위임 채널이다. 새로운 업무 로직이 없다.
- 핵심 계약 3가지:
  1. 화면용 **사용자 JWT는 사용하지 않는다.** MCP client는 관리자 화면에서 발급한 **MCP 인증 키로 Basic Auth 인증**한다 (모든 환경).
  2. `X-User-Id`는 호출 시스템이 아니라 **effective user(위임 사용자)**를 지정한다. 이 헤더 자체는 인증 수단이 아니다.
  3. **성공 응답의 `data` 계약은 원본 API와 동일하다.** `/mcp` 한 조각을 뺀 경로가 원본이다. (예외: 원본이 없는 MCP 전용 endpoint, 4.6절)

### 1.1 원본 API와 다른 점 / 같은 점

| 항목 | MCP 채널 |
|---|---|
| 경로 | 원본 경로의 `/api/v1` 뒤에 `/mcp` 삽입 |
| 인증 | Basic Auth — MCP 인증 키 (`tmd_mcpauthinfom`, 회사 단위 발급). 원본: 사용자 Bearer JWT |
| 배포 서버 | **SK Gas 전용 서버(`dedicated`)만** (`d60ebbc0`). 원본 이벤트·시나리오 API 는 일반 서버 |
| 위임 사용자 식별 | `X-User-Id` 헤더 (원본: 사용자 JWT claim) |
| 요청 파라미터 | **원본과 동일** |
| 성공 응답 `data` | **원본과 동일** — 필드·타입·null 의미 전부 |
| 응답 래핑 | **원본과 동일** `BaseResponse` |
| Swagger 그룹 | `MCP API` 단독 (원본: `SK Gas API`) |

> **예외 — MCP 전용 endpoint (원본 화면 API 없음)**: 이벤트 Snapshot(버전 이력) 조회 6종(4.6절)은 소비자가 MCP 뿐이라 원본 API 없이
> MCP 채널에만 둔다. 이 endpoint 들은 "원본과 동일" 원칙의 대상이 아니며 전용 서비스(`EventSnapshotService`)를 쓴다 (설계 Decision Log D-1).

---

## 2. Scope

### 2.1 In scope

- 읽기 전용(GET) 조회 API의 MCP 경로 노출
- `X-User-Id` 기반 사용자 컨텍스트 주입
- MCP 전용 Swagger 그룹 분리
- effective user를 기존 API 접근 로그에 기록

### 2.2 Out of scope

- 쓰기 API (POST / PATCH / DELETE) 노출 — 단 사용자 피드백 등록(4.7절, D-9)과 이벤트 생성(4.8절, D-11) 2건은 예외
- SSE 스트림 엔드포인트 노출 — MCP 호출 모델과 맞지 않는다
- 서비스 / 레포지토리 계층 로직 변경 또는 복제 — 단 MCP 전용 endpoint(4.6절 이벤트 Snapshot)는 원본이 없으므로 전용 서비스·리포지토리를 신설한다 (D-1)
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
- 모든 MCP 요청은 `Authorization: Basic base64(<MCP 인증 ID>:<Secret>)` 이 필수다. 환경 구분 없이 같다.
  - 키는 `tmd_mcpauthinfom` 에 있으며 관리자 화면(시스템 관리 > MCP API 인증관리)에서 **회사 단위**로 발급한다.
  - Secret 은 발급·재발행 응답에서 한 번만 보여주고 서버에는 단방향 해시(PBKDF2-HMAC-SHA256, 키별 솔트)만 남는다.
- 키가 없거나 틀리면 `401` 이다 (5.1절).
- 위임 사용자 역할: 20건 모두 `USER`(SK Gas 소속) · `ADMIN`(타사 접속 가능) · `OPERATOR`(조건 없음 — 회사는 아래 키 회사 일치 검사로 제한) (2026-10-11, D-10).
  `OPERATOR` 는 상품 권한 정보가 없어 레코드 조회(4.1.4, 4.6 근거 Record)의 `isPermitted` 가 모두 `false` 다.
- 키의 회사와 `X-User-Id` 사용자의 회사가 같아야 한다. 예외: ADMIN 사용자는 관리회사 키로 요청할 때만 회사가 달라도 허용한다. 위반은 `403` (5.1절).
- 이전의 개발용 임시 인증 예외(`mcp.read-only-api.development-auth-bypass-enabled`)는 삭제됐다 (PR #595).

### 3.3 `X-User-Id` 헤더 계약

| 항목 | 내용 |
|---|---|
| 헤더명 | `X-User-Id` |
| 값 | MI Platform 사용자 로그인 ID (`tmd_usrm.usr_logn_id`, 문자열) |
| 필수 여부 | **필수** — 모든 MCP endpoint에 적용 |
| 예시 | `X-User-Id: mcp-user@sk.com` |

**서버가 검증하는 것**: 값이 공백이 아니고, 해당 로그인 ID로 조회한 삭제되지 않은 사용자가 한 명 이상이며,
선택된 사용자의 소속 고객사가 사용·미삭제 상태이고 현재일이 고객사 사용 기간 안에 있는가.
그리고 그 사용자의 고객사가 Basic Auth 키의 고객사와 같은가 (ADMIN + 관리회사 키 예외, 3.2절).

**다중 행 선택 규칙**: `UserRepository.findByUserLoginIdAndIsDeletedFalse`가 여러 `User` 행을 반환하면
반환 목록의 첫 번째 행을 effective user로 사용한다. 별도 역할·고객사 우선순위나 정렬은 적용하지 않는다.

**서버가 검증하지 않는 것**: MCP client와 effective user가 같은 주체인지. MCP client는 사용자를 대신하는 시스템 주체다.

> **이 헤더는 인증 수단이 아니다.** 인증은 Basic Auth 키가 하고, 이 헤더는 그 키의 회사 범위 안에서 위임할 사용자를 지정한다.
> 키를 가진 client 는 같은 회사의 어느 사용자로든 위임할 수 있다.
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

#### 4.3.2 데이터소스 제약

- `CmeBenchmarkServiceImpl`은 `DataSourceContextHolder`로 `midb-skgas` 테넌트에 라우팅한다
  (설정 키: `mi.next-gen.cme.benchmark.datasource-key`, 기본값 `${SKT_MI_DB_DEDICATED_NAME:midb-skgas}`).
- 이 테넌트 키는 `SKT_MI_DB_DEDICATED_NAME` 환경변수로 결정된다. 실제 SK Gas 전용 배포 환경에는 `midb-skgas`로 설정되어 있다 (2026-08-24 사용자 확인).
  저장소 자체의 하드코딩 기본값은 서로 다르다 — 서비스 코드는 `midb-skgas`, `application-dedicated.yml`의 테넌트는 `midb-skens`.
  로컬 `testdb` 설정에는 `midb-skgas` 테넌트가 없어 이 endpoint를 실데이터로 검증할 수 없다 — CME 검증은 실제 SK Gas 전용 배포 환경에서만 수행한다.
- CME 데이터는 SK Gas 데이터이므로 **SK Gas 전용 backend 에서만 의미 있는 결과**를 준다 (4.5.1절).

#### 4.3.3 MCP 컨트롤러 구성 (`d60ebbc0`, 2026-10-05 이후)

**MCP 컨트롤러는 모두 `@Profile("dedicated")` 다.** 일반 서버에는 MCP 경로가 등록되지 않는다.

| MCP 컨트롤러 | `@Profile` | 담는 endpoint |
|---|---|---|
| `McpEventDedicatedController` | `dedicated` | 4.1 이벤트 **6건** |
| `McpScenarioGroupDedicatedController` | `dedicated` | 4.2 시나리오 그룹 **6건** + 4.3 CME 벤치마크 **1건** |
| `McpEventSnapshotDedicatedController` | `dedicated` | 4.6 이벤트 Snapshot **6건** (MCP 전용) |
| `McpUserFeedbackDedicatedController` | `dedicated` | 4.7 사용자 피드백 등록 **1건** (MCP 전용 쓰기) |

- 채널 인프라(인증 · 인터셉터 · Swagger 그룹)는 **prefix 단위로 걸리므로 그대로 재사용한다.** 컨트롤러를 추가해도 손댈 설정이 없다.
- CME 의 OpenAPI `description` 에는 "SK Gas 전용 서버 전용 · 일반 서버의 응답은 신뢰할 수 없습니다" 경고가 남아 있다.
  MCP 경로가 일반 서버에 노출되던 시기(`d60ebbc0` 이전)에 쓴 문구이며, 지금은 ske/skhy 전용 backend 배포 시의 주의로 유효하다 (4.5.1절).

### 4.4 호출 대상 서버 (Q7 해소)

**MCP 서버는 base URL 하나만 쓴다 — SK Gas 전용 backend.**

- MCP 채널 19건 전부가 `dedicated` 프로파일 서버에만 있다. 일반 서버는 호출 대상이 아니다.
- `skgas-test-mcp` 는 `MI_MCP_BASE_URL` 하나로 이 서버를 가리킨다 (개발계 `https://dev-mi-skgas.sk.com`).
- 전용 서버는 subdomain 이 다르며, 이 주소는 배포 설정에서 정해지고 애플리케이션 코드에는 없다.

### 4.5 서버별 endpoint 노출 (2026-10-09 기준)

**개발계 활성 프로파일** (사용자 확인, 2026-08-24)

| backend | 활성 프로파일 |
|---|---|
| 일반 | `dev`, `common`, `custom`, `test` |
| 전용 (skgas / ske / skhy 각각 별도 기동) | `dev`, `dedicated` |

`common` / `custom` / `test`는 **이 저장소에서 아무것도 가르지 않는다** — `@Profile` 애노테이션 전수와
`spring.config.activate.on-profile` 문서 전수를 확인했고 어디에서도 참조하지 않는다. 배포 측 설정 주입용으로 보인다.
코드 동작을 가르는 것은 `dev`와 `dedicated` 둘뿐이다.

**결과 — MCP endpoint의 서버별 상태**

| backend | MCP 채널 19건 (4.1 6 · 4.2 6 · 4.3 CME 1 · 4.6 Snapshot 6) |
|---|---|
| 일반 | **미노출** (MCP 컨트롤러가 `dedicated` 전용) |
| 전용 · skgas | **노출 · 정상** |
| 전용 · ske | 배포하면 노출된다. CME 는 **무의미** (4.5.1절) |
| 전용 · skhy | 배포하면 노출된다. CME 는 **무의미** (4.5.1절) |

> **소비자에게 주는 의미**: MCP 채널은 **skgas 전용 backend 한 곳**만 호출한다.

#### 4.5.1 전용 backend가 3개다 — MCP 배포 대상 결정 필요 (Q9)

전용 backend는 skgas / ske / skhy 3개가 따로 뜨고, 셋 다 활성 프로파일이 `dev,dedicated`로 같다.
MCP 컨트롤러는 `@Profile("dedicated")` 이므로 **MCP 채널을 배포하는 전용 backend마다 19건이 모두 뜬다.**

CME는 SK Gas 데이터이고 라우팅 키는 `midb-skgas`로 고정이다(변경 예정 없음).
따라서 ske / skhy backend에 배포하면 그곳의 CME endpoint는 의미 있는 결과를 주지 못한다.

| 배포 대상 | 결과 |
|---|---|
| **skgas** 전용 backend 만 | 19건 모두 의미 있게 동작. 추가 조치 없음 |
| ske / skhy 전용 backend에도 배포 | 그곳의 CME endpoint는 무의미. 이벤트·시나리오·Snapshot 의 권한 규칙은 `SKGAS_COMPANY_USER_ONLY` 이므로 그 회사 USER 는 통과하지 못한다. 단 `OPERATOR` 는 회사 조건이 없어 그 회사 키로 위임하면 통과한다 (D-10) |

배포 범위로 통제되면 코드 변경은 없다.

### 4.6 이벤트 Snapshot (버전 이력) — **MCP 전용, 원본 화면 API 없음** (확정 — 2026-10-09)

파이프라인이 Event 를 생성·갱신할 때마다 쌓는 버전 이력(`tmm_mi_mievent_snapshot_m`)과
그 버전의 근거 Record·기관 전망 사본(`tmm_mi_mievent_snapshot_record_map`, `tmm_mi_mievent_snapshot_institution_view_m`)을 조회한다.
원본 API 가 없어 1.1절 "원본과 동일" 원칙의 예외다. 컨트롤러 `McpEventSnapshotDedicatedController` (`@Profile("dedicated")`), 서비스 `EventSnapshotService`.

| endpoint | operationId | 군 | 요청 | 응답 `data` |
|---|---|---|---|---|
| `GET /api/v1/mcp/mi/next-gen/events/{eventId}/snapshots` | `getEventSnapshots` | A | `startDate`, `endDate`(KST 날짜, `crt_dtm`), `updateTrigger`(`RECORD_INGESTION`/`MCP_USER_EDIT`), `createdBy`(대소문자 무시), `page`=1, `size`=20 | `PaginationRes<EventSnapshotSummaryRes>` — 버전 번호 내림차순, 상세분석 본문 제외 |
| `GET .../events/{eventId}/snapshots/lookup` | `lookupEventSnapshot` | A | `versionNo` 또는 `asOf`(KST `yyyy-MM-dd'T'HH:mm:ss`). 둘 다 없으면 최신 | `EventSnapshotLookupRes { criteria, found, snapshot }` — 이력 없으면 `found=false` |
| `GET .../events/{eventId}/snapshots/{snapshotId}` | `getEventSnapshot` | A | — | `EventSnapshotDetailRes` |
| `GET .../events/{eventId}/snapshots/{snapshotId}/compare` | `compareEventSnapshot` | A | `target`=`previous`(기본)/`current` | `EventSnapshotCompareRes { comparable, differences[], sameFields[], recordedChangedFields[] }` |
| `GET .../events/{eventId}/snapshots/{snapshotId}/records` | `getEventSnapshotRecords` | **B** | `recordType`, `refNm`(부분 일치), `startDate`/`endDate`(`record_first_created`), `page`=1, `size`=10 | `PaginationRes<EventSnapshotRecordRes>` — 발생 시각 내림차순 |
| `GET .../events/{eventId}/snapshots/{snapshotId}/outlooks` | `getEventSnapshotOutlooks` | A | `institutionName`(부분 일치), `startDate`/`endDate`(`view_date`, 양끝 포함), `page`=1, `size`=10 | `PaginationRes<EventSnapshotOutlookRes>` — `view_date DESC, institution_view_id DESC` |

**규칙**

- 근거 Record 는 Snapshot 사본(`tmm_mi_mievent_snapshot_record_map`)만, 기관 전망은 Snapshot 사본만 조회한다. 현재 연결(`tmm_mi_mievent_record_map`)이나
  원본 현재 값(`tmm_mi_institution_view`)으로 대체하지 않는다.
- `/records` 는 B군이다: 사본에 원본 Record 의 현재 제목·요약·상품 정보를 붙이고(식별용), `isPermitted` 를 4.1.4 와 같은 방식으로 계산한다.
  원본이 삭제됐어도 행은 유지되며 `isSourceAvailable=false` 다.
- `changedFields`/`recordedChangedFields` 는 파이프라인이 기록한 본문 4개 컬럼의 직전·직후 비교 결과이고, `/compare` 의 `differences` 는 두 상태를 직접 비교한 결과다. 둘은 다를 수 있다.
- `previous` 비교는 `previous_mi_event_snapshot_id` 기준이며 없으면 `comparable=false` (버전 번호 −1 로 대신 찾지 않는다). `current` 는 `tmm_mi_mievent_m` 과 비교(삭제 여부 무관).
- 시각: Snapshot 관련 시각 컬럼은 모두 UTC 저장으로 보고 응답은 KST 로 변환한다. 입력(`asOf`, 날짜 필터)은 KST 로 받아 UTC 로 바꿔 비교한다.
- 행 논리 삭제(`del_yn='Y'`) 이력은 제외한다. 저장 당시 Event 삭제 여부(`event_del_yn`)로는 거르지 않고 `isEventDeleted` 로만 응답한다. 원본 Event 가 삭제·부재여도 이력은 조회된다.
- 하위 테이블의 `crt_usr_logn_id`·`crt_dtm`(이력 행 생성 정보)은 응답하지 않는다.

**오류**: 채널 공통(인증·`X-User-Id`) 외에 — `{snapshotId}` 가 없거나 다른 Event 소속이면 `RSC-001`(HTTP 400, 4.1.2 와 동일),
`lookup` 에 `versionNo` 와 `asOf` 를 같이 주면 `REQ-003`(HTTP 400). 목록이 비면 빈 페이지(200).

**범위 밖 (P2, 별도 슬라이스)**: Record 반영 타임라인(`/snapshots/record-timeline`), 과거 내용 검색(`/events/snapshots/search`).

### 4.7 사용자 피드백 등록 — **MCP 채널의 쓰기 API** (확정 — 2026-10-11)

- `POST /api/v1/mcp/mi/next-gen/user-feedback` (`McpUserFeedbackDedicatedController.createMcpUserFeedback`) — 이 1건으로 MCP 채널은 20건(GET 19 + POST 1)
- 원본 화면 API `POST /api/v1/common/user-feedback` 은 일반 서버(`!dedicated`) 전용이고 Bearer JWT 인증이라 MCP 경로에 따로 둔다 (원본 계약 무변경)
- 저장: 화면과 같은 `tcm_usrfedbl`, `fedb_chnl_cd = 'MCP'` (화면 = `SCREEN`, DB 기본값). 권한 규칙은 4.1~4.6 과 동일
- 요청 body (`McpUserFeedbackReq`, 전부 문자열)

| 필드 | 필수 | 저장 컬럼 | 처리 |
|---|---|---|---|
| `feedbackContent` | Y | `fedb_cntt` (mediumtext) | 앞뒤 공백 제거. 길이 제한 없음 |
| `feedbackTypeCd` | N | `fedb_type_cd` | `agent_output`/`ux_ui`/`feature_suggestion`/`etc`. 대소문자·별칭 허용, 모르면 `etc` |
| `feedbackSectionNm` | N | `fedb_sectn_nm` | 대상 종류 `Event`/`Scenario Group`/`Scenario`. 별칭 허용, 모르면 원문(300자) |
| `targetId` | N | `fedb_trgt_id` | 섹션값 종류의 ID(eventId/scenarioGroupId/scenarioId). 숫자가 아니면 NULL. 존재 확인 안 함 |
| `feedbackPageNm` | N | `fedb_page_nm` | 대상 이름·주제. 300자에서 자름 |

- **응답**: `data` = 등록된 피드백 번호. **오류**: 채널 공통 외에는 `feedbackContent` 공백일 때 `REQ-003`(400) 하나뿐 — 사용자는 시스템 값을 모르므로 나머지는 저장 후 운영자가 판단한다.
- DB 변경(2026-10-11, DBA 적용): `fedb_chnl_cd`·`fedb_trgt_id` 추가, `fedb_cntt` → mediumtext. 새 컬럼을 매핑한 backend 는 **ALTER 적용 후 배포**해야 기동된다 (`ddl-auto: validate`).
  설계·운영자 조회 SQL: 워크스페이스 `process_userfeedback.md`

### 4.8 이벤트 생성 — **쓰기 API, 이벤트 생성 서버에 위임** (2026-10-12)

- `POST /api/v1/mcp/mi/next-gen/events/generations` (`McpEventDedicatedController.generateEvents`) — 이 1건으로 MCP 채널은 21건(GET 19 + POST 2)
- B.E 는 인증(채널 공통 3.2절·3.3절)만 하고 이벤트 생성 서버 `POST /events/generations`(스펙 `event_generate_v1`)에 요청을 그대로 넘긴다.
  생성·저장(`tmm_mi_mievent_m`)은 생성 서버가 하며 B.E 는 DB 를 쓰지 않는다. 권한 규칙은 4.1~4.7 과 동일
- 생성 서버 호출: 헤더 `X-User-ID` = effective user 의 `usr_no`(정수, 생성자·수정자 ID), body 는 요청 그대로 (`EventGenerationAgentApi`)
- 주소 `api.event-generation-agent.url` → 없으면 환경변수 `EVENT_GENERATION_AGENT_URL`. local/tech/testdb 는 `http://40.82.137.178:8004`(2026-10-16 실 서버 연결, mock 제거). dev/prod 주소는 미정.
  read-timeout 기본 300s (`api.event-generation-agent.read-timeout`) — 웹 검색·LLM 생성을 동기로 기다린다
- 요청 body (`EventGenerationReq`)

| 필드 | 필수 | 처리 |
|---|---|---|
| `userQuery` | Y | 공백이면 `REQ-003`(400) |
| `dateFrom` / `dateTo` | N | `YYYY-MM-DD` 가 아니면 `REQ-003`(400) |

- **응답**: `data` = 생성 서버 응답의 `data`(`meta`, `events`) 그대로 (`EventGenerationRes`). 생성된 이벤트는 이미 저장된 상태이며 다시 호출하면 중복 생성된다
- **오류** (채널 공통 외)

| 상황 | HTTP | `code` |
|---|---|---|
| 생성 서버가 `status = error` 응답 | `400` | `BIZ-001` (메시지에 생성 서버 message 포함) |
| 생성 서버 연결 실패 / 타임아웃 | `500` | `API-003` / `API-002` |
| 생성 서버 HTTP 오류(422 등) | `500` | `API-004` (공통 RestClient 빌더 처리) |

---

## 5. 오류 계약

### 5.1 채널 공통 오류 (인증 · `X-User-Id`)

모든 MCP endpoint에 공통 적용된다. 인증은 Security 필터에서, `X-User-Id` 위임은 컨트롤러 진입 전 인터셉터에서 판정한다.

| 상황 | HTTP | `code` | 비고 |
|---|---|---|---|
| `Authorization` (Basic Auth) 없음 | `401` | `AUTH-001` | |
| 키 ID·Secret 불일치, 삭제된 키, 키의 회사가 삭제됨 | `401` | `AUTH-002` | 존재하지 않는 ID 도 같은 응답 (응답 시간도 같게 처리) |
| `X-User-Id` 헤더 없음 또는 공백 (`mcp.default-id` 미설정) | `403` | `AUTH-007` | 메시지 `X-User-Id header is required.` |
| `X-User-Id` 헤더 없음 또는 공백 (`mcp.default-id` 설정) | — | — | `mcp.default-id`(환경변수 `MCP_DEFAULT_ID`) 로그인 ID 를 X-User-Id 로 받은 것처럼 처리. 사용자 존재·회사 일치 검사는 동일하게 적용 |
| 해당 `usr_logn_id`로 삭제되지 않은 사용자를 조회할 수 없음 | `403` | `AUTH-007` | 메시지 `X-User-Id is not allowed for this MCP key.` |
| effective user의 소속 고객사가 미사용·삭제 상태이거나 현재일이 사용 기간 밖임 | `403` | `AUTH-007` | 위와 같은 메시지 |
| effective user 의 고객사가 키의 고객사와 다름 (ADMIN + 관리회사 키 제외) | `403` | `AUTH-007` | 위와 같은 메시지 |

사용자 없음·비활성·회사 불일치를 같은 메시지로 응답해, 키를 가진 client 가 다른 회사 사용자 ID 의 존재 여부를 알 수 없게 한다.

오류 응답도 `BaseResponse` 포맷이다.

```json
{
  "status": false,
  "code": "AUTH-007",
  "message": "Access denied. You do not have permission to access this resource.(X-User-Id header is required.)",
  "data": null
}
```

### 5.2 endpoint별 오류

원본 API와 동일하다. 대표적으로:

| 상황 | HTTP | 비고 |
|---|---|---|
| `page` / `size`가 1 미만 | `400` | Jakarta Validation |
| 존재하지 않는 `eventId` / `scenarioGroupId` | 원본과 동일 | 원본 API의 계약을 따른다 |

### 5.3 결정 사항 — 401 과 403 의 구분 (2026-10-08)

- `401` 은 **MCP client 인증 실패**(Basic Auth 없음·불일치)에만 쓴다. MCP 서버가 401 을 "인증정보를 다시 확인" 신호로 쓸 수 있게 한다.
- `X-User-Id` 문제는 인증은 통과했지만 그 사용자로 위임할 수 없다는 뜻이므로 `403 / AUTH-007` 이다 (`AppHttpStatusException` 으로 명시).
- 이전(2026-08-25)에는 `X-User-Id` 문제를 `401 / AUTH-001` 로 응답했다.

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
| 인증 스키마 | `McpBasicAuth` (HTTP basic) 를 문서 전체와 모든 operation 의 보안 요구로 노출. 사용자 `BearerAuth` 는 제거 |
| 공통 응답 예시 | 모든 operation 에 400(`REQ-003`)·401(`AUTH-001`)·403(`AUTH-007`) 자동 추가 |
| `X-User-Id` | 헤더 파라미터로 노출. `mcp.default-id` 미설정 시 필수, 설정 시 선택 |
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
4. **MCP 컨트롤러는 모두 `@Profile("dedicated")` 다** (`d60ebbc0`, 4.3.3절). 새 endpoint 도 기존 `Mcp*DedicatedController` 또는
   새 `Mcp*DedicatedController` 에 `@Profile("dedicated")` 로 둔다. 원본 컨트롤러가 일반 서버(`!dedicated`)용이어도 MCP 쪽은 전용 서버에 둔다.
   전용 DB 테넌트를 읽는 endpoint(CME 등)는 SK Gas 전용 backend 에서만 의미가 있으므로 description 에 그 사실을 적는다.
5. **쓰기 API나 SSE**를 추가하려는 경우는 이 채널의 범위 밖이다. 별도 설계 검토가 필요하다. (사용자 피드백 등록 4.7절이 검토를 거친 유일한 예외)

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

† 2026-08-24 추가 확정. **전용 DB(`midb-skgas`) 접근** — SK Gas 전용 backend 에서만 의미 있는 결과를 준다 (4.3절). MCP 채널 전체가 `dedicated` 전용이다 (4.3.3절).

**B군 — 사용자 컨텍스트 필수 (응답이 `X-User-Id`에 따라 달라짐)**

| 대상 | 원본 엔드포인트 | 헤더에 따라 달라지는 것 |
|:--:|---|---|
| ◯ | `GET /events/{eventId}/records` | 항목별 `isPermitted` (상품 조회 권한) |
| ◯ | `GET /events/{eventId}/scenario-groups` | 그룹별 `mine` (설계 점유자가 본인인지) |

**MCP 전용 — 원본 화면 API 없음 (4.6절, 2026-10-09)**

| 대상 | MCP 엔드포인트 (`/api/v1/mcp/mi/next-gen` 생략) | 서비스 메서드 | 군 |
|:--:|---|---|:--:|
| ◯ | `GET /events/{eventId}/snapshots` | `EventSnapshotService.getEventSnapshots` | A |
| ◯ | `GET /events/{eventId}/snapshots/lookup` | `EventSnapshotService.lookupEventSnapshot` | A |
| ◯ | `GET /events/{eventId}/snapshots/{snapshotId}` | `EventSnapshotService.getEventSnapshot` | A |
| ◯ | `GET /events/{eventId}/snapshots/{snapshotId}/compare` | `EventSnapshotService.compareEventSnapshot` | A |
| ◯ | `GET /events/{eventId}/snapshots/{snapshotId}/records` | `EventSnapshotService.getEventSnapshotRecords` | B (`isPermitted`) |
| ◯ | `GET /events/{eventId}/snapshots/{snapshotId}/outlooks` | `EventSnapshotService.getEventSnapshotOutlooks` | A |
| - | `GET /events/{eventId}/snapshots/record-timeline` | (P2) | A |
| - | `GET /events/snapshots/search` | (P2) | A |

**대상 제외**

- SSE 스트림 endpoint 전체 (`.../stream`) — MCP 호출 모델과 맞지 않음
- 쓰기 API (POST / PATCH / DELETE) — 채널 범위 밖 (예외: 4.7절 사용자 피드백 등록)
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

- MCP 인증 키(Basic Auth)를 가진 client 만 호출할 수 있다. 키 없이는 어떤 환경에서도 `401` 이다.
- 키는 회사 단위다. 키를 가진 client 는 그 회사의 어느 사용자로든 `X-User-Id` 위임을 할 수 있다 (ADMIN + 관리회사 키는 회사 무관).
- `/records`의 `isPermitted`는 SKGAS USER이면 effective user의 개인+회사 상품 권한,
  타사 접근 가능 ADMIN이면 서버가 해석한 SKGAS 고객사의 회사 단위 상품 권한으로 계산된다.
- MCP 요청의 `X-Company-No`는 계약에 없으며, 숫자·비숫자와 관계없이 상품 권한 대상 결정에 사용하지 않는다.
- 고객사·역할·datasource는 헤더로 직접 선택하지 않고 서버가 조회한 사용자 정보와 서버 설정에서 결정한다.

### 9.2 MCP 인증 키 관리 (2026-10-08)

| 항목 | 내용 |
|---|---|
| 저장 | `tmd_mcpauthinfom`. Secret 은 PBKDF2-HMAC-SHA256 + 키별 16바이트 솔트로 단방향 저장 (반복 횟수 키별 저장, 신규 기본 `mcp.auth.hash-iterations=1000`) |
| 발급 | 관리자 화면 시스템 관리 > MCP API 인증관리 (관리 API `/api/v1/admin/system-management/mcp-auth`, 일반 서버). Secret 은 발급 시 1회만 표시 |
| 재발행 | 이전 Secret 은 즉시 무효 |
| 삭제 | 논리 삭제. ID 를 `d_{등록번호}_{원래ID}` 로 바꿔 같은 ID 재등록 허용 |
| 로그 | `Authorization`, `X-MCP-Authorization` 헤더는 요청 로그·MDC 에서 마스킹 |

개발용 임시 인증 예외는 삭제됐다 (PR #595). 개발계·운영계 모두 같은 인증이 적용된다.

### 9.3 운영 배포 게이트

MCP client 인증(Q2)은 구현됐다. 운영 배포 전 남은 확인 사항:

- 배포 대상 DB 에 `tmd_mcpauthinfom` 과 이벤트 Snapshot 3개 테이블이 있어야 한다 (`ddl-auto: validate` — 없으면 기동 실패).
- 운영 MCP 서버용 키를 발급하고 MCP 서버 설정(`MI_MCP_USERNAME` / `MI_MCP_PASSWORD`)에 넣는다.
- 음수 테스트: 미인증·틀린 키, 다른 회사 사용자 위임, effective user 범위를 넘는 고객사·datasource 변조,
  외부 `X-Company-No` 무시, 서로 다른 고객사 요청 간 컨텍스트 누출, CME 오라우팅.

---

## 10. Acceptance criteria

- [ ] Basic Auth 키 없이 MCP 경로를 호출하면 모든 환경에서 `401` / `AUTH-001` 이 반환된다.
- [ ] 틀린 키(ID·Secret 불일치, 삭제된 키)는 `401` / `AUTH-002` 로 거부된다.
- [ ] 인증된 MCP client 만 `X-User-Id` 위임을 수행하며, 키 회사와 다른 회사 사용자 위임은 `403` / `AUTH-007` 이다 (ADMIN + 관리회사 키 예외).
- [ ] MCP 응답 본문(`data`)이 원본 API 응답과 **동일하다** (4.1~4.3, 동일 파라미터·동일 사용자 기준 diff 결과 무차이).
- [ ] `X-User-Id` 없음 → `403` / `AUTH-007`.
- [ ] 영문·이메일 형식의 `X-User-Id` 로그인 ID가 숫자 파싱 없이 처리된다.
- [ ] `X-User-Id` 로그인 ID 조회 결과가 없으면 → `403` / `AUTH-007`.
- [ ] 같은 로그인 ID로 여러 사용자가 조회되면 반환 목록의 첫 번째 사용자가 effective user가 된다.
- [ ] effective user의 소속 고객사가 미사용·삭제·사용 시작 전·사용 종료 후 → `403` / `AUTH-007`.
- [ ] `/records`의 `isPermitted`가 SKGAS USER의 개인+회사 상품 권한으로 계산된다 (권한 있는 사용자 / 없는 사용자 각각 확인).
- [ ] 타사 접근 가능 ADMIN의 `/records.isPermitted`가 ADMIN 소속 회사와 무관하게 SKGAS 회사 단위 상품 권한으로 계산된다.
- [ ] `X-Company-No`에 다른 실제 고객사 번호 또는 비숫자 값을 보내도 값이 무시되고, 헤더 미전송 요청과 동일한 SKGAS 상품 권한 결과가 나온다.
- [ ] `/{eventId}/scenario-groups`의 `mine`이 헤더 사용자 기준으로 계산된다 (점유자 본인 / 타인 각각 확인).
- [ ] API 접근 로그에 `SYSTEM`이 아닌 effective user로 기록된다.
- [ ] MCP 인증/Infra 로그에서 MCP client identity를 추적하고 request/trace id로 API 로그와 연결할 수 있다.
- [ ] Swagger UI에 `MCP API` 그룹이 단독 노출되고, `MI` / `SK Gas` / `ETC` 그룹에 중복 노출되지 않는다.
- [ ] MCP 그룹에 사용자 `BearerAuth`가 노출되지 않고, `McpBasicAuth` 보안 요구와 `X-User-Id` 필수 헤더가 노출된다.
- [ ] MCP 그룹의 인증 스키마(`McpBasicAuth`)가 실제 MCP client 인증 방식(Basic Auth)과 일치한다.
- [ ] 기존 API의 동작·계약·권한이 변경되지 않는다 (회귀 테스트 baseline 대비 무변화).
- [ ] MCP 경로 밖에서는 사용자 컨텍스트 합성이 일어나지 않는다.
- [ ] 요청 정상·예외 종료 후 `SecurityContext`가 다음 요청에 남지 않는다.
- [ ] 요청값으로 effective user의 고객사 범위를 넓히거나 datasource를 바꾸는 시도가 실패한다.
- [ ] 서로 다른 고객사 사용자의 연속 요청 간 `SecurityContext`와 tenant context가 누출되지 않는다.
- [ ] CME 벤치마크의 OpenAPI `description`에 "SK Gas 전용 서버 전용 · 일반 서버 응답 신뢰 불가" 경고가 노출된다.
- [ ] 일반 backend 에는 MCP 경로가 등록되지 않는다 (4.3.3절).
- [ ] 4.5절 서버별 노출표대로 실제 노출된다 (skgas 전용 backend 에 19건, 일반 backend 에 0건).
- [ ] 전용 컨트롤러도 동일한 `X-User-Id` 계약과 오류 계약을 따른다 (채널 인프라 재사용 확인).

- [ ] (4.6 이벤트 Snapshot) 버전 목록이 `snapshot_version_no DESC`(+ `crt_dtm DESC, mi_event_snapshot_id DESC`)로 정렬되고 기간·트리거·생성자 필터가 동작한다.
- [ ] (4.6) 다른 Event 의 snapshotId 는 `RSC-001`, `lookup` 의 `versionNo`+`asOf` 동시 지정은 `REQ-003`, 이력이 없으면 `found=false`.
- [ ] (4.6) `asOf` 는 `crt_dtm <= asOf(KST→UTC)` 중 최신 1건이고 동시각이면 버전이 큰 것이다.
- [ ] (4.6) 근거 Record·기관 전망은 Snapshot 사본에서만 조회되고, 원본이 삭제돼도 근거 Record 행이 빠지지 않는다.
- [ ] (4.6) `/compare` 의 `differences` 와 `recordedChangedFields` 가 구분돼 내려가고, 이전 Snapshot 이 없으면 `comparable=false`.
- [ ] (4.6) 응답 시각이 KST 이고 목록 응답에 상세분석 본문이 없다.
- [ ] (4.6) OpenAPI description 에 Snapshot 조회 주의사항이 노출된다 (`McpOpenApiTest`).

---

## 11. 관련 문서

- 설계 근거 / 결정 로그: `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
- 실행 계획 / 검증 기록: `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`
- `X-User-Id` 로그인 ID 변경 실행 기록: `docs/work-notes/2026/2026-08-25-x-user-id-login-id-first-match-implementation.md`
- 이벤트 Snapshot 실행 기록: `docs/work-notes/2026/2026-10-09-mcp-event-snapshot-api.md`
- 원본 도메인 기능 정의: `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md`
- OpenAPI 서술 규칙: `docs/conventions/openapi-documentation-conventions.md`
- 소유권 규칙: `docs/conventions/source-of-truth-governance.md`
