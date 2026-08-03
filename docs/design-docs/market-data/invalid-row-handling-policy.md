# 시장 데이터 invalid row 처리 정책

## Document Control

- status: Approved
- canonical for: Sparrow PR 3 시장 데이터 필수 날짜의 invalid row 처리와 기존 응답 호환성
- owner: Shared
- source documents:
  - `docs/work-notes/2026/2026-07-28-null-return-dereference-remediation-plan.md`
  - `docs/work-notes/2026/2026-07-28-null-return-dereference-security-review.md`
- depends on:
  - `AGENTS.md`
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 없음
- last synced: 2026-07-31
- open questions:
  - 없음

## Purpose

Sparrow의 `널 반환 값 역참조` finding을 제거하되, 명시적으로 잘못된 기존 동작을
바로잡는 경우가 아니면 시장 데이터 API의 성공·실패 조건과 응답 형태를 변경하지 않는다.

이번 정책은 PR 3의 Dashboard, GIE EU, US Export, period calculator에서 DB raw row의
필수 날짜를 역참조하는 경로에만 적용한다.

## Compatibility Principle

1. 보안취약점 finding 제거에 필요한 최소 범위만 변경한다.
2. 기존에 500으로 종료되는 invalid 필수 날짜는 행을 제외하거나 no-data로 바꾸지 않는다.
3. 우발적인 NPE, 날짜 예외, 빈 목록 접근을 명시적인 controlled internal error로 바꾼다.
4. 외부 HTTP status, error code와 공개 message는 기존 generic 500과 동일하게 유지한다.
5. 기존에 허용한 빈 조회 결과와 nullable value의 처리는 변경하지 않는다.
6. 새로운 응답 필드, error code, schema, query ordering 또는 cache 정책을 추가하지 않는다.

## Fixed Decisions

### Empty query result

- query 결과가 0행이면 기존 no-data 흐름을 유지한다.
- Dashboard는 기존처럼 요청 기간 row를 유지하고 value가 없는 응답 형태를 사용한다.
- GIE EU와 US Export의 기존 empty chart 응답을 유지한다.
- period calculator는 기존처럼 `null`을 반환해 해당 period key를 생략한다.
- 빈 query 결과를 내부 데이터 품질 오류로 재분류하지 않는다.

### Invalid required date

- query 결과 행이 존재하지만 필수 날짜가 `null`, blank 또는 지원하지 않는 형식이면
  해당 행을 skip하지 않는다.
- `DateUtil.parseDate()`가 `null`을 반환하거나 날짜 파싱 예외가 발생하면
  `SystemErrorException(SYS_INTERNAL_ERROR)` 계열의 controlled internal error로 변환한다.
- 현재 Service 호출을 HTTP 500으로 종료한다.
- all-invalid는 첫 invalid 필수 날짜에서 동일하게 실패하며 no-data로 변환하지 않는다.
- 마지막 raw row의 날짜가 invalid이면 이전 valid row를 최신 행으로 대신 선택하지 않는다.

### Required date by consumer

| Consumer | Required value |
| --- | --- |
| `MiDashboardService#calculateAverages` | 각 계산 대상 row의 `dateColumnName` 값 |
| Dashboard forecast row | `prdc_fulf_dt`를 사용하는 경로의 예측 수행일 |
| `GieEuChartMaker` | 각 chart row의 `dateColumnName` 값 |
| `UsExportChartMaker` | 각 월 합산·period 계산 row의 `dateColumnName` 값 |
| concrete period calculator | non-empty raw data의 기존 마지막 row 날짜 |
| `AbstractPeriodCalculator` | 기존 value validation을 통과해 계산에 참여하는 row의 날짜 |

### Nullable value

- `defaultDataColumnName` 값이 `null`일 때의 기존 skip/null 처리를 유지한다.
- Dashboard의 기존 nullable value 평균 처리와 응답 형태를 이번 PR에서 변경하지 않는다.
- `AbstractPeriodCalculator`가 null 또는 숫자로 변환할 수 없는 value를 건너뛰는 기존 동작을
  유지한다.
- `FiveYearAvgPeriodCalculator`가 current value 미존재 또는 invalid number에서 `null`을
  반환하는 기존 동작을 유지한다.
- non-null invalid number의 전역 처리 정책은 이번 null-return finding 범위 밖이다.

### Period response shape

- raw data가 비었거나 calculator가 기존 조건상 적용 불가능하면 `null`을 반환해 period key를
  생략한다.
- non-empty raw data의 기존 마지막 날짜가 유효하지만 계산 가능한 value 또는 비교 행이 부족하면
  `CalculatePeriodChangeValue.ofEmpty()`를 반환해 기존 key와 null 값 형태를 유지한다.
- invalid 필수 날짜를 `null` 또는 `ofEmpty()`로 바꿔 정상 응답처럼 처리하지 않는다.

### Error and logging

- client에는 HTTP 500과 `SYS_INTERNAL_ERROR`의 안정된 code/message만 반환한다.
- raw row, 원본 값과 parser exception detail은 client에 노출하지 않는다.
- 내부 log에는 datasource 식별자와 실패한 column 이름을 남길 수 있다.
- datasource/column은 `SystemErrorException`의 공개 additional message에 넣지 않는다.
- 원본 row 전체 또는 민감한 값은 log에 남기지 않는다.

## Implementation Boundary

### Included

- nullable date parse 결과의 명시적 검증
- date parse exception의 controlled internal error 변환
- dereference 전 non-null 보장
- 기존 500 응답과 no-data/value/period 응답의 회귀 테스트
- Sparrow finding `102151`, `102152`, `102153`, `102155`, `102204`, `102205`,
  `102226`, `102255`~`102264`

### Excluded

- invalid row skip과 부분 데이터 계산
- partial-data 또는 data-quality 응답 필드
- datasource별 부분 성공 API
- nullable value의 의미 변경
- 새로운 data-quality error code
- DB schema/DDL, query ordering, cache TTL 또는 cache invalidation 변경
- 이번 finding과 무관한 시장 데이터 정합성 보정

## Required Verification

- 정상 raw row의 차트·평균·합계·period 결과가 기존 fixture와 동일하다.
- 빈 raw data의 no-data 응답이 기존과 동일하다.
- nullable value의 기존 skip/null 처리와 period key 형태가 동일하다.
- 첫·중간·마지막 row의 null/invalid 날짜가 controlled HTTP 500으로 종료된다.
- Dashboard forecast의 null/짧은/invalid `prdc_fulf_dt`가 controlled HTTP 500으로 종료된다.
- invalid 날짜 응답에 Java exception, class, method와 원본 row detail이 없다.
- tracked source 기반 Sparrow 재분석에서 대상 nullable dereference finding이 제거된다.

## Implementation Readiness

- status: ready
- rationale:
  - invalid 필수 날짜의 기존 요청 실패 의미를 유지한다.
  - 우발적인 런타임 예외만 controlled internal error로 바꾸므로 API·schema·부분 성공 정책 변경이
    필요하지 않다.
