# Next Gen MI — MCP API Channel Design

## Document Control

- domain: gas / Next Gen MI
- status: Approved
- owner: BE
- created at: 2026-08-21
- updated at: 2026-10-09
- canonical for:
  - MCP 전용 조회 채널의 목적, 범위, 대상 선정 기준
  - MCP client와 위임 사용자의 trust model
  - 채널 구조, 보안 경계, 운영 배포 게이트
  - 일반 backend와 dedicated backend의 책임 구분
- not canonical for:
  - endpoint·header·response·HTTP error·validation 계약:
    `docs/feature-specs/mcp-readonly-api-feature-spec.md`
  - 실제 backend 주소·base URL·배포 대상 값: Infra/배포 설정
  - 변경 파일·실행 계획·검증 결과:
    `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`,
    `docs/work-notes/2026/2026-10-09-mcp-event-snapshot-api.md`
  - 기존 Next Gen MI 도메인 기능 정의:
    `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md`
- source documents:
  - `SECURITY.md`
  - 코드 조사 (2026-08-21 / 2026-08-24 / 2026-08-25, branch `feature/gas2`)
  - 사용자와의 설계 논의 (2026-08-21 / 2026-08-24 / 2026-08-25)
- depends on:
  - `AGENTS.md`
  - `SECURITY.md`
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/openapi-documentation-conventions.md`
- supersedes: 없음
- last synced: 2026-10-09 — 현재 코드 기준: MCP 채널 전부 `dedicated` (`d60ebbc0`), MCP client Basic Auth (backend PR #595), 이벤트 Snapshot (MCP 전용)
- open questions: Q9 (10절). Q2·Q7 은 해소

---

## 1. 목적과 범위

MCP(Model Context Protocol) 서버가 MI Platform의 Next Gen MI 시장 데이터를 읽을 수 있도록,
기존 서비스 계층을 재사용하는 전용 API 채널을 만든다.

이 설계의 핵심은 다음과 같다.

- 화면용 사용자 JWT 대신 MCP 호출에 맞는 별도 신뢰 경계를 둔다 — MCP 인증 키(회사 단위)로 Basic Auth 인증한다.
- MCP client는 모든 삭제되지 않은 유효 사용자를 대신해 조회할 수 있는 **신뢰된 위임자**다.
- 요청의 사용자 식별자로 서버가 유효한 사용자를 조회하고 기존 사용자 컨텍스트를 만든다.
- 컨트롤러는 얇게 유지하고 기존 서비스의 비즈니스 로직을 복제하지 않는다. (예외: 원본 화면 API 가 없는 MCP 전용 조회 — 이벤트 Snapshot, Decision Log D-1)
- 기존 API와 MCP API의 **성공 응답 데이터 계약**은 동일하게 유지한다.
- 인증 오류와 채널 전용 validation 오류는 MCP Feature Spec이 별도 계약으로 소유한다.
- OpenAPI 문서는 기존 소비자용 API와 분리한다.

### 1.1 범위

포함 범위는 Next Gen MI의 읽기 전용 시장 콘텐츠 조회다.

- 이벤트 조회
- 시나리오 그룹 조회
- SK Gas 전용 CME 벤치마크 조회
- 이벤트 Snapshot(버전 이력) 조회 — MCP 전용, 원본 화면 API 없음 (2026-10-09)

다음은 이 설계의 범위가 아니다.

- 생성·수정·삭제 API
- SSE 구독
- 화면 조작 상태, 폴링 상태, 실행 내부 상태 조회
- 실제 MCP 서버 구현
- 실제 backend 주소와 배포 토폴로지 결정
- MCP 인증 키 관리 화면·API 의 상세 (Feature Spec 9.2절)

정확한 대상 endpoint 목록과 요청·응답 계약은
`docs/feature-specs/mcp-readonly-api-feature-spec.md`만 소유한다.

---

## 2. Fixed Decisions

### D1. 전용 base path를 사용한다

MCP 채널은 `/api/v1/mcp/mi/next-gen/**` 아래에 둔다.

이 경로는 다음 책임을 한 곳에 모은다.

- MCP client 인증 (Basic Auth, D6)
- 위임 사용자 컨텍스트 생성
- MCP 전용 OpenAPI 그룹
- 접근 로그와 보안 테스트 범위

개별 endpoint 경로의 canonical 계약은 Feature Spec에 둔다.

### D2. 기존 사용자 컨텍스트를 합성한다

MCP 요청의 사용자 식별자로 `User`를 조회하고, 서버가 신뢰할 수 있는 사용자·고객사·역할
정보로 `SecurityContext`를 만든다.

사용자 헤더의 값과 다중 행 선택 규칙은 canonical Feature Spec의 `X-User-Id` 계약을 따른다.
로그인 ID가 여러 활성 `User` 행과 일치할 때 반환 목록의 첫 행을 선택하는 현재 규칙은 정렬을 보장하지 않으므로,
MCP client는 선택된 effective user의 역할·고객사를 응답과 접근 로그에서 확인해야 한다.

- 요청값은 사용자를 찾는 키로만 사용한다.
- 고객사, 역할, datasource는 요청 헤더가 직접 선택하지 않는다.
- 고객사와 역할은 조회된 `User` 엔티티에서 파생한다.
- effective user의 소속 고객사는 `isUsed=true`, `isDeleted=false`이고 현재 `yyyyMMdd`가
  `useBeginDt`와 `useEndDt` 사이(양 끝 포함)여야 한다. 이 조건을 만족하지 않으면 사용자 컨텍스트를 만들지 않는다.
- 고객사 유효성은 `UserRepository.findByUserLoginIdAndIsDeletedFalse`가 `@EntityGraph`로 함께 로드한
  `CustomerCompany`에서 검사하며, 같은 고객사를 다시 조회하지 않는다.
- `SecurityContext`에는 기존 인가 로직이 사용하는 사용자 번호, 소속 고객사, 역할,
  타사 접근 가능 여부를 포함한다.
- MCP 경로 밖의 요청에는 이 동작을 적용하지 않는다.
- 요청 완료 시 `SecurityContext`를 반드시 정리한다.

이 방식으로 기존 `AuthorizationService`와 서비스 계층을 재사용한다.
정확한 사용자 헤더와 오류 계약은 Feature Spec을 따른다.

#### D2.1. 원본 endpoint의 권한 규칙을 보존한다

MCP 컨트롤러의 각 메서드는 대응하는 원본 컨트롤러의 `@RequiredPermissionRules`를
명시적으로 동일하게 선언한다. 이 애노테이션은 Controller AOP이므로 서비스 재사용만으로
상속되지 않는다. MCP 사용자 컨텍스트는 Controller AOP 실행 전에 완성돼 있어야 한다.

따라서 `records` 조회의 endpoint 접근 조건도 원본과 같다.

- SKGAS 소속 `USER`
- 타사 접근이 허용된 `ADMIN`

그 밖의 역할과 고객사는 거부한다. MCP client가 신뢰된 위임자라는 사실은 effective user에게
원본 endpoint보다 넓은 업무 권한을 부여하지 않는다.

#### D2.2. `records.isPermitted`의 상품 권한 대상을 분리한다

`records` 조회는 endpoint 접근을 통과한 뒤 각 레코드의 상품 조회 가능 여부를 `isPermitted`로 계산한다.
이때 effective user의 신원·소속과 상품 권한을 판정할 대상 고객사는 서로 다른 개념이다.

- SKGAS `USER`: 위임 사용자의 `userNo`를 사용해 개인별 상품 권한과 소속 회사 상품 권한을 함께 적용한다.
  같은 SKGAS 사용자라도 개인별 설정에 따라 `isPermitted`가 달라질 수 있다.
- 타사 접근 가능 `ADMIN`: ADMIN 자신의 소속 `companyNo`가 아니라 SKGAS 고객사의 회사 단위 상품 권한을 적용한다.
- 그 밖의 사용자는 D2.1의 원본 권한 규칙에서 거부한다.

MCP 요청 계약에는 고객사를 선택하는 `X-Company-No`를 추가하지 않는다. MCP 채널은 SKGAS 서비스 페이지
전용이라는 업무 조건을 소유하며, 기존 서버 코드가 가진 회사 코드 `SKGAS`로 `CustomerCompany`를 조회해
실제 `companyNo`를 해석한다. 숫자 `companyNo`는 설정이나 코드에 고정하지 않고 요청값으로도 받지 않는다.

기존 서비스의 `AuthorizationService.getCompanyNoWithPriority()`를 재사용하기 위해 MCP 컨텍스트 계층은
`HttpLoggingFilter`가 먼저 저장한 외부 `X-Company-No` 값을 모든 MCP 요청에서 조건 없이 제거한다.
null일 때만 설정하는 방식이나 `setIfAbsent`는 금지한다. 타사 접근 가능 ADMIN이면 제거 직후 서버가 해석한
SKGAS `companyNo`로 request-scoped 우선 고객사 컨텍스트를 무조건 덮어쓴다. 그 밖의 역할은 외부 값을
제거한 상태로 두어 기존 JWT claim의 소속 고객사를 사용한다.

이 처리는 Controller AOP 실행 전에 끝내고 요청 종료 시 정리한다. 외부 값이 숫자이거나 비숫자이거나와
관계없이 `AuthorizationService.getCompanyNoWithPriority()`까지 전달하지 않으며, 같은 이름의 HTTP 헤더를
내부적으로 전달하는 방식도 사용하지 않는다.

서버가 해석한 SKGAS `companyNo`는 타사 접근 ADMIN의 상품 권한 대상과 레코드별 `isPermitted` 계산에만
사용한다. effective user의 소속 고객사·역할을 덮어쓰거나 datasource를 선택하는 데 사용하지 않는다.

### D3. 시장 콘텐츠 조회만 MCP 대상으로 선정한다

현재 확정 범위는 총 19건이다 (2026-10-09).

| 구분 | 건수 | 실행 backend |
|---|---:|---|
| 이벤트 | 6 | SK Gas dedicated |
| 시나리오 그룹 | 6 | SK Gas dedicated |
| CME 벤치마크 | 1 | SK Gas dedicated |
| 이벤트 Snapshot (MCP 전용) | 6 | SK Gas dedicated |
| 합계 | 19 | — |

다음 범주는 제외한다.

- Deprecated 조회 3건
- 시나리오 설계·상태 화면 보조 조회 4건
- 수급모델 실행 도메인 조회 6건
- 쓰기 API와 SSE

정확한 포함·제외 endpoint 목록은 Feature Spec이 소유한다.

### D4. MCP 채널은 SK Gas dedicated backend 에서만 제공한다 (`d60ebbc0`, 2026-10-05)

- MCP 컨트롤러는 모두 `@Profile("dedicated")` 다 (`McpEventDedicatedController`, `McpScenarioGroupDedicatedController`,
  `McpEventSnapshotDedicatedController`). general backend 에는 MCP 경로가 등록되지 않는다.
- 원본 이벤트·시나리오 그룹 컨트롤러가 general(`!dedicated`)용이어도 MCP 쪽은 dedicated 에 둔다.
- CME는 SK Gas만 사용하는 서비스이므로 datasource key `midb-skgas`는 서버 소유의 업무 불변조건이다.
- 요청값으로 CME datasource를 선택하지 않는다.

이전(2026-08-24)에는 이벤트·시나리오 그룹 MCP 컨트롤러를 general(`!dedicated`)에, CME 컨트롤러를 profile 없이 두었다.

### D5. OpenAPI 문서를 별도 그룹으로 제공한다

- MCP 전용 그룹명은 `MCP API`다.
- 기존 소비자용 `ETC API` 그룹에서는 MCP 경로를 제외한다.
- MCP 문서에는 사용자 Bearer JWT가 필요한 것처럼 표시하지 않는다.
- MCP client 인증 방식(Basic Auth)을 OpenAPI 보안 스키마 `McpBasicAuth` 로 노출한다.

endpoint 설명, 파라미터, 응답, 예시는 Feature Spec과
`docs/conventions/openapi-documentation-conventions.md`를 따른다.

### D6. MCP client 는 MCP 인증 키로 Basic Auth 인증한다 (backend PR #595, 2026-10-08)

- 모든 환경에서 `Authorization: Basic base64(<MCP 인증 ID>:<Secret>)` 이 필수다. 이전의 개발용 임시 인증 예외는 삭제했다.
- 키는 `tmd_mcpauthinfom` 에 있고 관리자 화면에서 **회사 단위**로 발급한다. Secret 은 발급 시 1회만 보여주고 단방향 해시로 저장한다.
- 키의 회사와 effective user 의 회사가 같아야 한다. 예외: ADMIN 사용자는 관리회사 키로 요청할 때만 회사가 달라도 허용한다.
- 키 검증은 MCP 전용 Security 체인에서만 한다. 검증 로직은 전역 AuthenticationManager 에 등록하지 않는다.
- 세부 계약(헤더, 오류 코드)은 Feature Spec 3.2·5.1·9.2절이 소유한다.

---

## 3. Trust Model

### 3.1 주체 구분

| 주체 | 의미 | 신뢰 근거 |
|---|---|---|
| MCP client | BE를 호출하는 실제 시스템 주체 | MCP 인증 키 (Basic Auth, 회사 단위) |
| effective user | MCP client가 대신 조회하는 MI Platform 사용자 | 헤더로 사용자를 찾은 뒤 서버의 `User` 정보로 검증 |
| BE | 인증·위임 검증과 사용자 컨텍스트 생성 주체 | 서버 코드와 관리되는 배포 설정 |

MCP client는 삭제되지 않은 유효 MI Platform 사용자를 대신해 읽기 요청을 할 수 있는 신뢰된 위임자다.
단, 이 권한은 **인증된 MCP client**에게만, 그리고 **키의 회사 사용자**에 대해서만 부여한다 (ADMIN + 관리회사 키 예외).

### 3.2 위임 사용자 처리

사용자 헤더는 MCP client 자체의 식별자가 아니다. 이 값은 effective user를 지정한다.

BE는 다음 순서로 처리한다.

1. MCP client 를 Basic Auth 로 인증한다 (모든 환경). 실패는 `401`.
2. 사용자 헤더로 `User`를 조회한다.
3. 존재하지 않거나 삭제된 사용자는 거부한다 (`403`).
4. 사용자의 소속 고객사가 미사용·삭제 상태이거나 현재일이 사용 기간 밖이면 거부한다 (`403`).
5. 사용자의 고객사가 키의 고객사와 다르면 거부한다 (`403`, ADMIN + 관리회사 키 예외).
6. 고객사·역할·권한 관련 값은 조회된 서버 데이터에서 만든다.
7. 해당 컨텍스트로 기존 조회 서비스를 호출한다.
8. 요청 종료 시 컨텍스트를 정리한다.

### 3.3 Caller와 Effective User 로그

- 기존 API 접근 로그의 `subject`와 `role`은 effective user를 기록한다.
- MCP client identity와 인증 결과는 MCP 인증 계층 또는 Infra 접근 로그에서 추적한다.
- 두 로그는 request/trace id로 연결할 수 있어야 한다.
- MCP client 는 키 ID 로 식별된다. 요청 로그의 `Authorization` 헤더 값은 마스킹한다.

현재 API 로그 테이블에 `actorType`, `mcpClientId`, `effectiveUserNo` 필드를 추가하는 것은 범위 밖이다.
운영 감사 요구가 강화되면 별도 로깅 설계로 검토한다.

### 3.4 권한 경계

신뢰된 위임은 인증 생략을 뜻하지 않는다.

- MCP client 인증은 모든 환경에서 필수다 (D6).
- 위임 대상은 서버가 확인한 삭제되지 않은 유효 사용자이면서 키의 회사 사용자로 제한한다.
- 요청값이 고객사나 datasource를 직접 선택할 수 없다.
- endpoint 보호는 `SECURITY.md`의 인증·인가 기준을 충족해야 한다.

---

## 4. Channel Architecture

### 4.1 요청 흐름

```text
MCP Server
    |
    | GET /api/v1/mcp/mi/next-gen/**
    v
MCP client 인증 계층 (Basic Auth, MCP 전용 Security 체인)
    |  MCP 인증 키 검증 → 키의 회사
    v
MCP 사용자 컨텍스트 계층
    |  effective user 조회·검증, 키 회사와 일치 확인
    |  SecurityContext 생성
    v
MCP Controller
    |  요청 변환만 담당
    v
기존 Next Gen MI Service
    |
    v
기존 Repository / Mapper / Datasource
    |
    v
요청 완료 후 SecurityContext 정리
```

### 4.2 컴포넌트 책임

| 컴포넌트 | 책임 | 비고 |
|---|---|---|
| Security filter chain | MCP 경로 보호 | Basic Auth (MCP 인증 키), 모든 환경 동일 |
| MCP context interceptor/filter | effective user 검증, 컨텍스트 생성·정리 | MCP prefix에만 적용 |
| MCP controllers | 요청을 기존 서비스 호출로 연결 | 비즈니스 로직 복제 금지 |
| Existing services | 기존 조회 로직 수행 | 기존 성공 응답 데이터 유지 |
| OpenAPI group | MCP 소비자용 계약 노출 | 기존 문서와 분리 |

### 4.3 A군과 B군

기존 서비스의 사용자 컨텍스트 의존성에 따라 구현 위험을 구분한다.

- A군: 현재 서비스가 `AuthorizationService`를 사용하지 않는 조회
- B군: 현재 서비스가 사용자·고객사·역할 컨텍스트를 사용하는 조회

D2의 공통 컨텍스트 계층이 있으므로 두 군 모두 컨트롤러에서 서비스 로직을 복제하지 않는다.
B군 endpoint는 effective user에 따라 결과가 달라질 수 있음을 Feature Spec에 명시한다.
또한 A군/B군과 관계없이 원본 컨트롤러에 `@RequiredPermissionRules`가 선언된 메서드는
같은 규칙을 MCP 컨트롤러에 명시적으로 복제한다.
`records`의 상품 권한 대상 고객사 해석은 D2.2를 따른다.

---

## 5. Endpoint Selection Rules

새 endpoint를 MCP 채널에 추가하려면 다음 순서로 판단한다.

1. 시장 콘텐츠를 읽는 GET 요청인지 확인한다.
2. 화면 상태·폴링·실행 내부 상태·Deprecated API인지 확인한다.
3. 쓰기 또는 SSE라면 이 설계에서 제외하고 별도 설계를 만든다.
4. A군/B군을 판정하고 사용자 컨텍스트 의존성을 기록한다.
5. 데이터 소유 위치를 확인한다. MCP 컨트롤러는 모두 dedicated 에 둔다 (D4).
6. Feature Spec의 endpoint 목록과 계약을 먼저 수정한다.
7. 생성된 OpenAPI와 음수 보안 테스트를 확인한다.

새 endpoint를 추가해도 Security matcher나 공통 인터셉터 범위를 넓힐 필요가 없도록
동일한 MCP prefix 아래에 둔다. 다만 GET이 아닌 기능은 별도 보안 검토 없이는 추가하지 않는다.

---

## 6. Security and Release Gates

### 6.1 인증 구현 상태 (2026-10-08)

MCP client 인증(D6)이 구현돼 개발용 임시 인증 예외는 없다. 개발계·운영계에 같은 인증이 적용된다.

### 6.2 운영 배포 필수 조건

다음 중 하나라도 충족하지 못하면 운영 배포를 차단한다.

- [충족] MCP client 인증 방식이 구현돼 있다 (Basic Auth, D6). 운영 키의 발급·보관 주체는 배포 시 정한다.
- [충족] 인증되지 않은 요청이 `401`로 거부된다.
- [충족] 인증된 MCP client만, 키의 회사 사용자에 대해서만 위임을 수행할 수 있다.
- [충족] 위임 사용자가 없거나 삭제 상태이면 실패한다 (`403`).
- 고객사와 datasource가 요청값이 아니라 서버 데이터·서버 설정에서 결정된다.
- Security matcher가 MCP 경로에만 적용되고 기존 `/api/**`와 겹치지 않는다.
- 일반 요청과 MCP 요청 사이에서 `SecurityContext`가 누출되지 않는다.
- [충족] 생성된 MCP OpenAPI가 실제 인증 방식과 일치한다 (`McpBasicAuth`).
- 배포 대상 DB 에 `tmd_mcpauthinfom` 과 이벤트 Snapshot 3개 테이블이 있다 (`ddl-auto: validate`).
- Q9에 따른 dedicated backend 배포 범위가 정해져 있다.

### 6.3 필수 음수 테스트

- 인증 없는 요청
- 잘못된 MCP client credential (틀린 Secret, 삭제된 키)
- 키 회사와 다른 회사 사용자로 위임 (ADMIN + 관리회사 키 예외 포함)
- 사용자 헤더 누락·공백
- 존재하지 않는 사용자
- 삭제된 사용자
- 요청값으로 effective user의 고객사 범위를 넓히려는 시도
- `X-Company-No`에 다른 실제 고객사 번호를 보내도 헤더 미전송 요청과 동일한 SKGAS 상품 권한 대상이 적용되는지 확인
- `X-Company-No`에 비숫자 값을 보내도 파싱 오류 없이 외부 값이 무시되고 SKGAS 상품 권한 대상이 적용되는지 확인
- 요청값으로 datasource를 바꾸려는 시도
- SKGAS 사용자 두 명의 개인별 상품 권한 차이가 `isPermitted`에 반영되는지 확인
- 타사 접근 가능 ADMIN의 소속 회사와 무관하게 SKGAS 회사 단위 상품 권한이 적용되는지 확인
- SKGAS 사용자가 아닌 USER와 타사 접근이 허용되지 않은 ADMIN의 `records` 접근 거부
- 서로 다른 고객사 사용자의 연속 요청 간 컨텍스트 누출
- 일반 API와 MCP matcher overlap
- 요청 처리 후 `SecurityContext` 잔존
- CME 요청의 잘못된 backend 라우팅

구체적인 acceptance criteria와 실행 결과는 Feature Spec과 stable work note에 기록한다.

---

## 7. Deployment Availability

실제 주소는 이 문서가 소유하지 않는다. 이 절은 backend별 기능 책임만 정의한다.

| backend | MCP 채널 19건 | 지원 여부 |
|---|---|---|
| general | 미등록 (MCP 컨트롤러가 `dedicated` 전용) | 미지원 |
| SK Gas dedicated | 제공 | **지원** |
| SK E dedicated | 배포하면 등록됨 | 미지원 (CME 무의미, 이벤트 계열은 `SKGAS_COMPANY_USER_ONLY` 권한) |
| SK hynix dedicated | 배포하면 등록됨 | 미지원 (위와 같음) |

MCP 서버는 SK Gas dedicated backend base URL 하나만 쓴다.
구체적인 URL, ingress, service name, 환경별 profile 조합은 Infra/배포 설정이 소유한다.

---

## 8. Risks and Controls

| ID | 위험 | Runtime guard / 통제 | Owner | 검증 방법 | Gate |
|---|---|---|---|---|---|
| R1 | ~~임시 인증 예외가 운영에 남음~~ | 해소 — 임시 예외 삭제, 모든 환경 Basic Auth (D6) | BE | 미인증 요청 `401` | — |
| R2 | 키를 가진 client 의 사용자 헤더 위임 남용 | 키 회사 사용자로 위임 범위 제한, 키는 회사 단위 발급·재발행 즉시 무효 | BE | 다른 회사 사용자 위임 `403`, 삭제 사용자 음수 테스트 | 구현 차단 |
| R3 | 이전 요청의 사용자 컨텍스트 누출 | MCP prefix 제한, 요청 종료 시 context clear | BE | 연속 요청·예외 경로 테스트 | 구현 차단 |
| R4 | 요청값으로 타 고객사/datasource 접근 | 외부 우선 고객사 값을 조건 없이 제거한 뒤 ADMIN은 서버 소유 `SKGAS` 코드를 DB의 `companyNo`로 해석해 무조건 덮어씀 | BE | 숫자·비숫자 `X-Company-No`, datasource 변조 테스트 | 구현 차단 |
| R5 | CME가 SK Gas 외 dedicated 에서 무의미한 결과 | MCP 채널을 SK Gas dedicated 에만 배포 (Q9) | MCP / Infra | backend별 CME 호출 확인 | 배포 차단 |
| R6 | Swagger가 실제와 다른 인증을 표시 | MCP group 분리, `McpBasicAuth` 노출 (`McpOpenApiTest`) | BE | 생성 OpenAPI 확인 | 배포 차단 |
| R7 | Security matcher가 다른 API를 우회 | GET·MCP prefix 최소 matcher, overlap 테스트 | BE | security integration test | 구현 차단 |
| R8 | 미지원 dedicated backend에 MCP 채널 배포 | Q9 배포 범위 확정 | BE / Infra | backend availability 확인 | 배포 차단 |
| R9 | 배포 DB 에 `tmd_mcpauthinfom`·Snapshot 테이블 없음 | 배포 전 DDL 적용 확인 | BE / DBA | 기동 시 schema validation | 배포 차단 |

---

## 9. Permission and Data Ownership Matrix

| 대상 | 실제 호출 주체 | effective user 필요 | 데이터 소유 위치 | datasource 결정 주체 |
|---|---|---:|---|---|
| 이벤트 일반 조회 | MCP client | endpoint에 따라 사용 | MI master DB | 서버의 사용자/고객사 컨텍스트 |
| 이벤트 `records` 상품 권한 | MCP client | 필수 | MI master DB | USER는 `userNo`별 개인+회사 권한, 타사 접근 ADMIN은 서버가 해석한 SKGAS `companyNo`의 회사 권한 |
| 시나리오 그룹 | MCP client | endpoint에 따라 사용 | MI master DB | 서버의 사용자/고객사 컨텍스트 |
| 이벤트 Snapshot | MCP client | `/records` 만 필수 (`isPermitted`) | MI master DB (Snapshot 3개 테이블, 파이프라인 소유) | 서버의 사용자/고객사 컨텍스트 |
| CME 벤치마크 | MCP client | 컨텍스트 합성 일관성을 위해 사용 | SK Gas dedicated | 서버 고정 `midb-skgas` |

MCP client 의 권한 범위는 키의 회사다 (D6).
effective user 정보는 기존 서비스의 사용자별·고객사별 조회 동작을 유지하기 위한 컨텍스트이며,
MCP client 인증을 대체하지 않는다.

MCP 요청에 `X-Company-No`가 포함돼도 위 표의 고객사 결정 주체는 바뀌지 않는다. 외부 값은
request-scoped 컨텍스트에서 조건 없이 제거하며 타사 접근 ADMIN의 대상은 항상 서버가 해석한 SKGAS다.

---

## 10. Open Questions

확정된 항목은 이 절에 두지 않는다. Q1, Q3, Q4, Q5, Q6, Q8은 2절의 결정으로 닫혔다.
Q2 는 D6(Basic Auth, 2026-10-08)으로, Q7 은 D4(MCP 채널 전부 dedicated → base URL 하나)로 닫혔다.

| ID | 질문 | Owner | 구현 영향 | 종료 조건 |
|---|---|---|---|---|
| Q9 | MCP 채널을 어떤 dedicated backend에 배포할 것인가? | BE / Infra | controller 코드에는 영향 없음 | SK Gas/SK E/SK hynix별 배포 범위와 미지원 경로 정책 확정 |

Q9 는 운영 배포 전에 정한다.

---

## 11. Implementation Readiness

**상태: Implemented (19건) / 운영 배포는 6.2절 남은 조건 확인 후**

구현을 시작할 수 있는 이유는 다음과 같다.

- 대상 범위와 선정 기준이 확정됐다.
- 총 19건의 endpoint 계약은 Feature Spec에 확정됐다 (이벤트 Snapshot 6건 2026-10-09 추가).
- 사용자 컨텍스트 생성 방식과 서비스 재사용 원칙이 확정됐다.
- 원본 `@RequiredPermissionRules` 보존과 `records.isPermitted`의 역할별 상품 권한 계산 기준이 확정됐다.
- 요청 고객사 선택값을 폐기하고 서버가 해석한 SKGAS 우선 고객사 컨텍스트로 기존 서비스를 재사용하는 방식이 확정됐다.
- MCP 채널 전체를 SK Gas dedicated 에서 제공한다 (D4).
- MCP client 인증(Basic Auth)이 구현됐다 (D6).
- CME의 `midb-skgas` 고정은 업무 불변조건으로 확인됐다.

다음은 구현 완료 조건이 아니라 운영 배포 전 필수 조건이다.

- 운영 MCP 서버용 키 발급과 MCP 서버 설정
- 배포 DB 의 `tmd_mcpauthinfom`·이벤트 Snapshot 테이블 적용 확인
- Q9 dedicated backend 배포 범위 확정
- 6.3절 음수 테스트와 생성 OpenAPI 검증 완료

---

## 12. Decision Log

| 날짜 | 결정 / 변경 | 근거 |
|---|---|---|
| 2026-08-21 | 기존 서비스 로직을 재사용하는 MCP 전용 조회 채널 신설 | 최초 요구사항 |
| 2026-08-21 | base path를 `/api/v1/mcp/mi/next-gen/**`로 분리 | 채널별 보안·문서·컨텍스트 경계 확보 |
| 2026-08-21 | 사용자 컨텍스트는 `SecurityContext` 합성 방식 채택 | `AuthorizationService` 재사용 |
| 2026-08-21 | `design-availability` 제외, 확장 가능한 조회 채널로 설계 | Deprecated 확인 및 사용자 결정 |
| 2026-08-24 | 구현 기준 branch를 `feature/gas2`로 확정 | 최신 서비스 동작 기준 유지 |
| 2026-08-24 | endpoint/header/error 계약을 Feature Spec으로 분리 | 계약 Source of Truth 단일화 |
| 2026-08-24 | 대상 13건 확정: 이벤트 6, 시나리오 그룹 6, CME 1 | 코드 전수 조사와 사용자 결정 |
| 2026-08-24 | Deprecated 3건, 화면 보조 4건, 수급모델 실행 조회 6건 제외 | 시장 콘텐츠 중심 선정 원칙 |
| 2026-08-24 | 일반 MCP 컨트롤러는 `!dedicated`, dedicated MCP 컨트롤러는 profile 미부착 | 기존 배포 구조와 dedicated controller 패턴 |
| 2026-08-24 | CME datasource를 서버 고정 `midb-skgas`로 유지 | SK Gas 단일 고객사 전용 서비스라는 업무 불변조건 |
| 2026-08-24 | 일반 backend CME 폴백은 배포·라우팅 위험으로 관리 | 지원 호출 대상은 SK Gas dedicated backend |
| 2026-08-24 | 개발 단계 임시 인증 예외와 운영 MCP client 인증을 분리 | 짧은 개발 일정과 운영 보안 경계 동시 반영 |
| 2026-08-24 | MCP client를 모든 삭제되지 않은 유효 사용자의 신뢰된 위임자로 확정 | 사용자 결정 |
| 2026-08-24 | API 로그의 effective user와 MCP 인증 로그의 client identity를 구분 | caller와 delegated user 의미 분리 |
| 2026-08-24 | 설계문서를 trust model·보안 경계·배포 게이트 중심으로 재구성 | 문서 Source of Truth와 구현 준비도 정리 |
| 2026-08-24 | 설계문서 승인 | 사용자 승인. 채널 구현 착수 가능, Q2·Q7·Q9 운영 배포 게이트는 유지 |
| 2026-08-24 | MCP 컨트롤러에서 원본 `@RequiredPermissionRules`를 명시적으로 보존 | Controller AOP 규칙은 서비스 재사용으로 상속되지 않음 |
| 2026-08-24 | `records`의 USER 개인+회사 권한과 타사 접근 ADMIN의 SKGAS 회사 권한 계산 기준 확정 | 원본 권한 의미를 유지하면서 요청 고객사 선택 헤더를 노출하지 않기 위함 |
| 2026-08-24 | SKGAS `companyNo`는 서버 소유 회사 코드 `SKGAS`로 DB에서 해석 | 환경별 숫자 고정과 요청값에 의한 고객사 전환 방지 |
| 2026-08-24 | 외부 고객사 선택값은 폐기하고 서버가 해석한 값을 request-scoped 우선 고객사 컨텍스트에 설정 | 기존 `AuthorizationService`와 서비스 로직을 변경 없이 재사용 |
| 2026-08-24 | Servlet Filter가 저장한 외부 `X-Company-No`는 MCP 인터셉터가 조건 없이 제거하고 ADMIN이면 서버 SKGAS 값으로 무조건 덮어씀 | null 조건부 설정 시 외부 고객사 값이 우선되는 권한 우회 방지 |
| 2026-08-25 | `X-User-Id` 값을 숫자 `userNo`에서 문자열 `userLoginId`로 변경하고 다중 조회 시 첫 행 선택 | 사용자 지정. 세부 헤더·오류 계약은 Feature Spec 소유 |
| 2026-10-05 | MCP 컨트롤러 전부를 `@Profile("dedicated")` 로 이동 (`d60ebbc0`). general backend 에서 MCP 경로 제거 | MCP 채널을 SK Gas dedicated 단일 서버로 통합. D4 갱신, Q7 해소 |
| 2026-10-08 | MCP client 를 MCP 인증 키(Basic Auth, 회사 단위)로 인증하고 개발용 임시 인증 예외 삭제. `X-User-Id` 위임 실패는 `403` (backend PR #595) | Q2 해소. D6 갱신 |
| 2026-10-09 | **D-1** 이벤트 Snapshot(버전 이력) 조회 6종을 원본 화면 API 없이 MCP 전용 endpoint로 신설 (`McpEventSnapshotDedicatedController`, `EventSnapshotService`). "원본과 동일·새 업무 로직 없음" 원칙의 예외 | 소비자가 MCP 뿐이다. 원본을 만들면 frontend 소비자가 없는 API가 생긴다. 계약은 Feature Spec 4.6절 |
| 2026-10-09 | **D-2** Snapshot 3개 테이블을 JPA 엔티티(`@Immutable`, `BaseEntity` 상속) + QueryDSL로 조회 | 코드베이스 표준. `ddl-auto: validate` 로 모든 환경(일반·전용 서버)의 master DB 에 3개 테이블이 있어야 기동된다 — 사용자 확정 전제 |
| 2026-10-09 | **D-3** P1 6종만 노출하고 Record 반영 타임라인·과거 내용 검색(P2)은 별도 슬라이스 | tool 수가 늘수록 LLM tool 선택이 흐려진다. P1 으로 원문 조회 항목 10개 중 8개를 커버 |
| 2026-10-09 | **D-4** 근거 Record 응답에 원본 Record 의 현재 제목·요약·상품 정보와 `isPermitted` 를 붙인다 (B군). 원본 조인은 LEFT JOIN 으로 원본 삭제 시에도 이력 행 유지 | Snapshot 사본에는 ID·유형·출처·발생 시각만 있어 LLM 이 근거 내용을 알 수 없다 |
| 2026-10-09 | **D-5** Snapshot 관련 시각 컬럼은 모두 UTC 저장으로 보고 응답 KST 변환, 입력(`asOf`·날짜 필터)은 KST→UTC 변환 후 비교 | 사용자 확정. 기존 이벤트 API 의 `utcToKst` 와 일관 |
| 2026-10-09 | **D-8** Snapshot 3개 테이블은 행 논리 삭제(`del_yn='N'`)만 거르고 저장 당시 Event 삭제 여부(`event_del_yn`)로는 거르지 않는다 | 실제 DB 에 문서에 없던 `del_yn` 이 있음 (2026-10-09 확인). 프로젝트 논리 삭제 관례 |
| 2026-10-11 | **D-9** 사용자 피드백 등록 1건을 MCP 채널의 쓰기 예외로 신설 (`POST /api/v1/mcp/mi/next-gen/user-feedback`). 화면과 같은 `tcm_usrfedbl` 에 채널 코드 `MCP` 로 저장하고, 내용 외 입력은 거부하지 않는다 | 원본 화면 API 는 일반 서버 전용이다. insert only 이고 기존 데이터를 바꾸지 않는다. 인증이 `next-gen/**` prefix 에만 걸려 경로를 그 아래에 둔다. 계약은 Feature Spec 4.7절 |
| 2026-10-11 | **D-10** MCP endpoint 20건 모두 `OPERATOR` 위임 허용 (회사 조건 없음). 레코드 조회의 `isPermitted` 는 운영자에게 모두 `false` | 운영자도 MCP 로 조회·피드백을 해야 한다 (사용자 요청). 회사는 인터셉터의 키 회사 일치 검사로 제한되므로 별도 조건을 두지 않는다. 운영자는 `getPermissions()` 미지원이라 `OperatorPermissionStrategy`(상품 권한 없음)와 같은 기준을 쓴다 |
| 2026-10-12 | **D-11** 이벤트 생성 1건을 MCP 채널의 두 번째 쓰기 예외로 신설 (`POST /api/v1/mcp/mi/next-gen/events/generations`). B.E 는 인증만 하고 이벤트 생성 서버(`event_generate_v1`)에 위임한다 | 생성·저장 로직은 생성 서버가 가진다 (사용자 지정). 이벤트 도메인이라 기존 MCP 이벤트 컨트롤러에 둔다. 계약은 Feature Spec 4.8절 |
| 2026-10-16 | **D-12** 이벤트 생성을 비동기 작업으로 전환. 접수(`POST /generations`)는 `jobId` 만 즉시 응답하고, B.E 가 작업 테이블 `tmd_mcp_job_mi_event_generation_m` 에 상태·결과를 남기며 전용 실행기에서 생성 서버를 호출한다. 조회 `GET /generations/{jobId}`·목록 `GET /generations` 추가 (MCP 채널 23건). 동시 실행 한도 `mcp.event-generation.max-concurrent`(기본 10), 넘으면 즉시 거절 | 생성 서버가 최대 15분 걸려 동기 래핑은 B.E·MCP 서버·client 타임아웃에 끊기고 중복 생성을 부른다. 생성 서버(Agent)는 그대로 두고 B.E 에서 작업을 관리한다 (사용자 선택 안 A). 고객 게이트웨이가 OpenAPI 로만 tool 을 만들어 MCP Tasks 대신 REST 작업 API 로 푼다. 계약은 Feature Spec 4.8절 |
| 2026-10-17 | **D-13** 이벤트 생성 작업을 dedicated 전용으로 한정하고(설정·스레드 풀·클라이언트·스케줄·서비스·컨트롤러 모두 `@Profile("dedicated")`, 생성 API 3건은 `McpEventGenerationDedicatedController` 로 분리) 동시 생성 한도를 **서버당**으로 바꾼다. 생성 서버 클라이언트 연결 풀을 한도에 맞추고, 종료 시 맡은 작업을 바로 `INTERRUPTED` 로 바꾼다 | 일반 서버에 쓰지 않는 스레드 풀·연결 풀을 만들지 않는다 (사용자 지정). 서버가 늘면 허용 수가 느는 것이 자연스럽다 (사용자 확정). 공통 HttpClient 기본 풀(호스트당 5)이 한도 10을 막고 있었다. 종료 시 30초 대기로 정리 전에 강제 종료될 수 있었다 — 로컬에서 재현·수정 확인 |

---

## Appendix A. Investigation Evidence

### A.1 사용자 컨텍스트

`AuthorizationService.getCurrentJwt()`는 기존 사용자 컨텍스트 접근의 단일 관문이다.
사용자 번호, 고객사 번호·코드, 역할, subject, 권한 조회가 이 경로를 사용한다.

관련 코드:

- `src/main/java/com/sk/domain/shared/service/permissions/AuthorizationService.java`
- `src/main/java/com/sk/common/config/security/jwt/CustomJwtAuthenticationConverter.java`
- `src/main/java/com/sk/common/utils/JwtUtil.java`

### A.2 Security filter chain

`ResourceServerConfig`는 MCP Basic Auth 체인(`/api/v1/mcp/mi/next-gen/**`, `@Order(4)`)과 일반 `/api/**` JWT 체인을 순서로 분리한다.
MCP 체인은 `McpAuthenticationProvider`(빈이 아님 — 전역 AuthenticationManager 에 섞이지 않게)로 키를 검증한다.
MCP matcher를 바꿀 때는 체인 순서와 기존 matcher overlap을 함께 검증해야 한다.

관련 코드:

- `src/main/java/com/sk/common/config/security/configurer/ResourceServerConfig.java`

### A.3 Dedicated controller와 datasource

- 조사한 `*DedicatedController`는 13개다.
- 이 중 기존 `ScenarioGroupDedicatedController`만
  `@Profile({"dedicated", "testdb"})`을 사용한다.
- CME 서비스는 `midb-skgas`를 서버에서 고정한다.
- dynamic datasource에 default target이 있어 잘못된 서버에서는 예외 대신 폴백할 수 있다.

관련 코드:

- `src/main/java/com/sk/domain/mi/controller/nextgen/ScenarioGroupDedicatedController.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/CmeBenchmarkServiceImpl.java`
- `src/main/java/com/sk/common/config/database/DynamicRoutingDataSource.java`

### A.4 OpenAPI와 접근 로그

- 기존 OpenAPI 그룹은 MCP 경로를 별도로 제외하지 않으면 문서가 겹칠 수 있다.
- 전역 Bearer 설정을 그대로 사용하면 MCP 호출자에게 잘못된 인증 안내가 노출될 수 있다.
- 기존 접근 로그의 사용자 필드는 `SecurityContext`의 effective user를 나타낸다.

관련 코드:

- `src/main/java/com/sk/common/config/SwaggerConfig.java`
- `src/main/java/com/sk/common/interceptor/ApiAccessLogInterceptor.java`, `ApiAccessLogService.java`

### A.5 외부 `X-Company-No`와 MCP 컨텍스트 실행 순서

- `HttpLoggingFilter`는 Servlet Filter이므로 Spring MVC `HandlerInterceptor`보다 먼저 실행된다.
- 이 필터는 실제 HTTP `X-Company-No` 값을 `MDCUtil.REQUEST_X_COMPANY_NO`에 저장한다.
- `AuthorizationService.getCompanyNoWithPriority()`는 이 값이 있고 역할이 ADMIN이면 타사 접근 가능 여부를
  확인한 뒤 해당 숫자를 우선 고객사로 반환한다.
- 따라서 MCP 인터셉터가 비어 있을 때만 SKGAS 값을 설정하면, 외부 값이 먼저 들어온 요청에서 공격자 입력이 남는다.
- MCP 인터셉터는 기존 값을 조건 없이 제거하고, 타사 접근 가능 ADMIN이면 서버가 DB에서 해석한 SKGAS
  `companyNo`로 무조건 덮어써야 한다. 비숫자 외부 값도 `AuthorizationService`에 도달해서는 안 된다.

관련 코드:

- `src/main/java/com/sk/common/filter/HttpLoggingFilter.java`
- `src/main/java/com/sk/common/utils/MDCUtil.java`
- `src/main/java/com/sk/domain/shared/service/permissions/AuthorizationService.java`

---

## Related Documents

- API 계약: `docs/feature-specs/mcp-readonly-api-feature-spec.md`
- 실행 계획 / 검증 기록: `docs/work-notes/2026/2026-08-21-mcp-readonly-api-set.md`
- `X-User-Id` 로그인 ID 변경 실행 기록: `docs/work-notes/2026/2026-08-25-x-user-id-login-id-first-match-implementation.md`
- 기존 도메인 기능 정의: `docs/design-docs/gas/nextgen-mi-integrated-functional-spec.md`
- 프로젝트 보안 기준: `SECURITY.md`
- OpenAPI 서술 규칙: `docs/conventions/openapi-documentation-conventions.md`
- Source of Truth 소유권 규칙: `docs/conventions/source-of-truth-governance.md`
