# MCP dev 호환 API slice 적용

## Document Control

- status: Complete
- canonical for: Execution record
- owner: BE
- source documents:
  - 사용자 요청 (2026-08-25)
  - commit `c50fa623` — MCP 이벤트·시나리오 API 추가
  - commit `182c70e1` — `X-User-Id`를 문자열 `userLoginId`로 변경
- depends on:
  - `docs/feature-specs/mcp-readonly-api-feature-spec.md` (cherry-pick 후 생성)
  - `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md` (cherry-pick 후 생성)
- supersedes: 없음
- last synced: 2026-08-25
- open questions:
  - 중복 `userLoginId`의 effective-user 선택과 `X-User-Id` 로그 redaction 보안 finding
  - 운영 MCP client 인증 방식과 배포 backend routing

## Task Title

`dev` 호환 MCP 이벤트·시나리오 API 12개 slice 적용

## Task Type

implementation

## Task Goal

`origin/dev` 기반 `feature/mcp-api` 브랜치에 두 MCP 커밋을 적용하되,
`dev`에 없는 코드에 의존하는 이벤트 연결 시나리오 그룹 endpoint는 제외하고
기존 Service·DTO만 재사용하는 MCP 읽기 endpoint 12개를 컴파일 가능한 상태로 제공한다.

## Done Criteria

- commit `c50fa623`, `182c70e1`의 MCP 공통 채널과 문자열 `X-User-Id` 변경을 적용한다.
- `dev`에 Service·DTO 의존성이 모두 존재하는 endpoint 12개만 노출한다.
- `GET /api/v1/mcp/mi/next-gen/events/{eventId}/scenario-groups`는 노출하지 않는다.
- canonical Feature Spec과 설계 문서의 endpoint 목록·건수를 12개 계약과 일치시킨다.
- 최소 Java compile, focused test, generated OpenAPI 또는 생략 사유를 기록한다.
- DB schema와 DBA rollout을 변경하지 않는다.

## Non-Goals

- `dev`에 없는 `EventLinkedScenarioGroupRes`와 `EventService.getEventScenarioGroups` 이식
- 중복 `userLoginId` 선택 정책 변경
- HTTP header logging 공통 리팩터링
- 운영 MCP client 인증 방식·credential·routing 확정
- `dev-mkjang`의 다른 커밋 병합

## Current Status

Complete — `feature/mcp-api`에 두 소스 커밋을 적용하고 `dev` 미지원 endpoint 1개를 제거했다.
Java compile과 MCP Controller/OpenAPI 집중 테스트가 통과했으며 현재 구현·문서·생성 OpenAPI 범위는 12개로 일치한다.
다만 아래 보안·운영 위험 때문에 이 결과를 운영 배포 가능 상태로 보지는 않는다.

## Execution Plan

1. `feature/mcp-api`에 `c50fa623`, `182c70e1`을 순서대로 cherry-pick한다.
2. cherry-pick conflict와 `dev` 기준 compile 오류를 확인한다.
3. `McpEventController`에서 미지원 endpoint와 import를 제거한다.
4. Feature Spec·설계 문서의 endpoint 목록, 건수, acceptance criteria를 12개로 맞춘다.
5. nearest test pattern을 확인하고 최소 focused/generated OpenAPI 테스트를 보완한다.
6. `compileJava`, focused test, `git diff --check`를 실행한다.
7. 변경 파일, 검증 결과, 잔여 보안·배포 위험을 이 work note에 기록한다.

### Planned Files

- `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
- `docs/feature-specs/mcp-readonly-api-feature-spec.md`
- `src/main/java/com/sk/common/config/SwaggerConfig.java`
- `src/main/java/com/sk/common/config/WebMvcConfig.java`
- `src/main/java/com/sk/common/config/security/configurer/DefaultConfig.java`
- `src/main/java/com/sk/common/config/security/configurer/ResourceServerConfig.java`
- `src/main/java/com/sk/common/interceptor/ApiAccessLogService.java`
- `src/main/java/com/sk/common/interceptor/McpUserContextInterceptor.java`
- `src/main/java/com/sk/common/utils/MDCUtil.java`
- `src/main/java/com/sk/domain/mi/controller/nextgen/mcp/McpEventController.java`
- `src/main/java/com/sk/domain/mi/controller/nextgen/mcp/McpScenarioGroupController.java`
- `src/main/java/com/sk/domain/mi/controller/nextgen/mcp/McpScenarioGroupDedicatedController.java`
- `src/main/java/com/sk/domain/shared/enums/Claim.java`
- `src/main/java/com/sk/domain/shared/repository/master/tmd/UserRepository.java`
- `src/main/resources/application.yml`
- 관련 focused/generated OpenAPI test
- 이 work note

### Risk Predictions

- cherry-pick 대상 커밋이 `dev-mkjang` 선행 변경을 암묵적으로 참조할 수 있다.
- 문서가 13개 endpoint 계약을 유지하면 실제 12개 구현과 source-of-truth drift가 생긴다.
- 문자열 로그인 ID 첫 행 선택과 header 원문 로깅은 이전 독립 리뷰의 보안 blocker다.
- `dev` 스냅샷에는 repository governance/OpenAPI/SECURITY 문서가 없어 제공된 AGENTS 규칙을 fallback으로 사용한다.

## Related Files

- 구현 및 문서: 위 Planned Files 참조

## Key Changes

- `c50fa623`을 `7e18844d`로, `182c70e1`을 `2dbbda92`로 순서대로 cherry-pick했다.
- `McpEventController`에서 `dev`에 없는 `EventLinkedScenarioGroupRes`와
  `EventService.getEventScenarioGroups`를 사용하는 endpoint를 제거했다.
- 이벤트 controller의 `ApiOrder`를 1~5로 다시 정렬했다.
- Feature Spec과 설계 문서를 이벤트 5건 + 시나리오 그룹 6건 + CME 1건, 총 12건으로 맞췄다.
- 보류 endpoint를 확장 후보 인벤토리와 acceptance criteria에 명시했다.
- 기존 Service를 그대로 호출하는지 확인하는 controller delegation 테스트를 추가했다.
- 생성 OpenAPI에서 12개 경로, 문자열 `X-User-Id`, 기존 그룹 중복 제외, CME 경고를 검증했다.
- 생성 OpenAPI의 `400`·`401`에 `BaseResponse` schema와 `REQ-003`·`AUTH-001` 실제 JSON example이
  포함되는지 검증했다.

## Verification

- `./gradlew compileJava -x test`
  - PASS — 미지원 endpoint 제거 후 Java compile 성공
- `./gradlew test -PforceTest --tests 'com.sk.domain.mi.controller.nextgen.mcp.McpControllerDelegationTest' --tests 'com.sk.domain.mi.controller.nextgen.mcp.McpOpenApiTest'`
  - PASS — 6 tests, 0 failures, 0 errors, 0 skipped
- `git diff --check`
  - PASS

## Verification Result

- PASS — `dev`에 존재하는 Service·DTO만 사용하는 12개 endpoint가 컴파일된다.
- PASS — 생성 OpenAPI의 경로 수, 헤더 타입, 오류 schema/example가 Feature Spec과 일치한다.
- NOT VERIFIED — 실제 general/skgas dedicated 환경의 base URL routing과 CME 데이터 반환.
- NOT VERIFIED — 전체 회귀 테스트. 변경 범위에 맞춘 집중 테스트만 실행했다.

## Independent Review

- 이 브랜치의 최종 12-endpoint diff에 대한 별도 독립 리뷰는 미실행이다.
- 선행 독립 리뷰에서 동일한 문자열 로그인 ID 구현의 첫 행 선택 비결정성과 원문 header logging이
  보안 blocker로 보고됐다. 아래 Issues / Risks에 유지한다.

## Actual Result

- 계획대로 `dev` 의존성이 있는 endpoint 1개만 제외했고, 나머지 12개 MCP endpoint와 공통 채널 코드는 유지했다.
- 예상한 1개 endpoint 외 추가 `dev` 컴파일 의존성은 발견되지 않았다.
- 계획에 포함된 생성 OpenAPI 검증을 생략하지 않고 자동 테스트로 고정했다.
- DB schema/DDL과 `dev-mkjang`의 다른 커밋은 변경하지 않았다.

## Issues / Risks

- **Merge blocker / Security**: 중복 `userLoginId` 조회에 정렬·유일성 보장이 없어 첫 행이 실행 계획에 따라
  달라질 수 있다. effective user의 고객사·역할·권한이 잘못 선택될 수 있으므로 merge 전 정책 결정이 필요하다.
- **Merge blocker / Security**: 공통 HTTP header logging이 `X-User-Id` 로그인 ID 원문을 기록할 수 있다.
  로그인 ID를 민감정보로 취급한다면 redaction 또는 제외가 필요하다.
- **Production blocker**: 운영 MCP client 인증 방식이 아직 확정·구현되지 않았다.
- **Routing risk**: CME는 SK Gas dedicated backend에서만 신뢰할 수 있으며 실제 배포 routing은 검증하지 않았다.
- `GET /events/{eventId}/scenario-groups`는 의존 코드가 `dev`에 반영될 때까지 보류다.
- `dev` 스냅샷에는 canonical governance/OpenAPI convention/SECURITY 문서가 없어 제공된 AGENTS 규칙을
  fallback으로 사용했다.
- quality calibration pilot 문서도 `dev`에 없어 이번 완료 건을 pilot row에 등록하지 못했다.

## Human Acceptance

- Not reviewed

## Next Actions

1. 중복 `userLoginId` 선택 정책과 `X-User-Id` 로그 redaction blocker를 결정·수정한다.
2. 최종 diff를 별도 세션에서 `code-review` skill로 검토한다.
3. 승인 후 branch를 push하고 MR을 만든다.
4. 보류 endpoint의 DTO·Service가 `dev`에 반영되면 별도 slice로 추가한다.
