# Newsletter v2 `data_src_spec_json` Contract Spec

- date: `2026-03-17`
- status: `DRAFT`
- scope: Newsletter v2 News Template Block canonical execution contract
- out of scope: implementation, DDL 상세 재검토, fallback 제거 시점 확정

> 2026-03-20 current note:
> 이 문서의 `issue state`는 물리 테이블이 아니라 block 실행 시 허용되는 logical override 입력을 뜻한다.
> 삭제된 `tmd_nwsltrorgisssctnstat` / `tmd_nwsltrorgissblkstat` 테이블 의존성을 전제하지 않는다.

## 1. Feature Goal

이 문서는 Newsletter v2 block data source execution model의 canonical contract를 구현 가능한 수준으로 고정한다.

- canonical discriminator: `tmd_nwsltrtmpltblk.data_src_type_cd`
- canonical execution spec truth source: `tmd_nwsltrtmpltblk.data_src_spec_json`
- legacy fallback / bridge key: `tmd_nwsltrtmpltblk.data_src_ref`
- legacy read compatibility only: `tmd_nwsltrtmpltblkfld.col_mapping`, `tmd_nwsltrtmpltblkfld.fld_meta`

핵심 원칙은 다음과 같다.

- 신규 write는 canonical-only다.
- 기존 read/preview/publish는 한시적으로 legacy fallback을 허용한다.
- `data_src_spec_json`이 존재하는 block은 canonical path만 사용한다.
- canonical spec과 legacy 값이 충돌하면 publish/preview는 fail-fast 한다.
- issue state는 spec을 바꾸지 않고 `overridePolicy`가 허용한 publish-time assembly input만 override 한다.
- 단, `NEWS_ITEM`은 `NEWS_EVENT` 조회 결과를 snapshot assemble 시 runtime-expanded block으로 펼친다. 반복 개수는 issue block state가 아니라 resolver 결과가 결정한다.

## 2. Endpoint

이번 slice는 신규 external endpoint를 추가하지 않는다. 대신 template 관리 API, preview API, publish pipeline이 공통으로 사용하는 block execution contract를 고정한다.

- template create/update request DTO: `dataSrcSpecJson` 입력 추가
- template read/detail response DTO: `dataSrcSpecJson`, block classification, validation 결과 노출 가능
- preview/publish service: 동일 validator/classifier 사용

## 3. `data_src_spec_json` Contract

### 3-1. Top-level

`data_src_spec_json`은 JSON object여야 하며 허용 top-level key는 정확히 다음 5개다.

- `locator`
- `query`
- `assign`
- `transform`
- `overridePolicy`

허용되지 않는 top-level key는 저장/preview/publish 시 모두 오류다.

`contractType`는 금지한다.

### 3-2. Generic Skeleton

```json
{
  "locator": {
    "kind": "NEWS_EVENT"
  },
  "query": {},
  "assign": {
    "fields": [
      { "fieldKey": "newsId", "sourcePath": "newsId", "required": false },
      { "fieldKey": "title", "sourcePath": "title", "required": true },
      { "fieldKey": "content", "sourcePath": "content", "required": true },
      { "fieldKey": "timestamp", "sourcePath": "timestamp", "required": false },
      { "fieldKey": "createdDate", "sourcePath": "createdDate", "required": true },
      { "fieldKey": "createTime", "sourcePath": "createTime", "required": false },
      { "fieldKey": "source", "sourcePath": "source", "required": false },
      { "fieldKey": "originalUrl", "sourcePath": "originalUrl", "required": false },
      { "fieldKey": "showInChart", "sourcePath": "showInChart", "required": false },
      { "fieldKey": "trend", "sourcePath": "trend", "required": false }
    ]
  },
  "transform": [],
  "overridePolicy": {
    "issueState": {
      "query": [],
      "state": [],
      "contentInput": []
    }
  }
}
```

### 3-3. `locator`

`locator`는 canonical source locator다. raw table name, SQL, 자유 path를 저장하지 않는다.

허용 필드:

- `kind`: required
- `refId`: optional unsigned integer
- `refCode`: optional string

추가 필드는 금지한다.

허용 `kind` enum:

- `INLINE`
- `PREFERENCE_TABLE`
- `MAIN_CHART`
- `DEDICATED_CHART`
- `COLLECTION_CHART`
- `NEWS_EVENT`
- `PUBLISH_CONTEXT`

`kind`별 규칙:

| kind | 용도 | 필수 필드 | 금지/비고 |
|---|---|---|---|
| `INLINE` | source 조회 없이 template/issue 입력만 사용 | 없음 | `STATIC` family에서만 허용 |
| `PREFERENCE_TABLE` | `NewsletterTemplatePreferenceTable` 기준 조회 | `refId` | `DYNAMIC_QUERY` family 전용 |
| `MAIN_CHART` | 메인 차트 조회 | 없음 또는 `refCode` | `refCode` 없으면 `blk_cd`를 기본 차트 코드로 사용 |
| `DEDICATED_CHART` | 전용 차트 master 조회 | `refCode` | `CHART_SERIES` family 전용 |
| `COLLECTION_CHART` | 수집 차트 master 조회 | `refCode` | `CHART_SERIES` family 전용 |
| `NEWS_EVENT` | issue/publish 기준 뉴스 이벤트 조회 | 없음 | `NEWS_EVENT` family 전용 |
| `PUBLISH_CONTEXT` | publish metadata/context 값 참조 | 없음 | `STATIC` family에서만 허용 |

family별 허용 조합:

| `data_src_type_cd` | 허용 `locator.kind` |
|---|---|
| `STATIC` | `INLINE`, `PUBLISH_CONTEXT` |
| `DYNAMIC_QUERY` | `PREFERENCE_TABLE` |
| `CHART_SERIES` | `MAIN_CHART`, `DEDICATED_CHART`, `COLLECTION_CHART` |
| `NEWS_EVENT` | `NEWS_EVENT` |

### 3-4. `query`

`query`는 실행 입력의 default set이다. 자유 DSL이 아니며 flat object만 허용한다.

공통 규칙:

- nested object 금지
- array 금지
- raw SQL, table, column, JSONPath, SpEL, template expression 금지
- 허용 키는 family별 whitelist만 가능
- 값 타입은 `string`, `number`, `boolean`, `null`만 허용

허용 query key:

| family | 허용 key | 타입 | 비고 |
|---|---|---|---|
| `STATIC` | 없음 | - | `query`는 빈 object 또는 생략만 허용 |
| `DYNAMIC_QUERY` | 없음 | - | 실제 query 정의는 preference table master가 정본 |
| `CHART_SERIES` | `fromDate`, `toDate` | string | `COLLECTION_CHART`일 때만 허용 |
| `NEWS_EVENT` | `fromDate`, `toDate` | string | legacy 호환 입력으로만 허용, runtime 필수 아님 |

추가 규칙:

- `fromDate`, `toDate`는 `yyyy-MM-dd` 형식만 허용
- `CHART_SERIES`에서 `MAIN_CHART`, `DEDICATED_CHART` locator에는 `query` 비허용이다
- 전 family 공통 mandatory query key는 없다

### 3-4-1. Query Requiredness Matrix

필수 여부는 `data_src_type_cd`와 `locator.kind` 조합별로 결정한다.

| family | locator.kind | mode | required query key | optional query key | forbidden query key |
|---|---|---|---|---|---|
| `STATIC` | `INLINE`, `PUBLISH_CONTEXT` | 공통 | 없음 | 없음 | 모든 key |
| `DYNAMIC_QUERY` | `PREFERENCE_TABLE` | 공통 | 없음 | 없음 | 모든 key |
| `CHART_SERIES` | `MAIN_CHART` | 공통 | 없음 | 없음 | 모든 key |
| `CHART_SERIES` | `DEDICATED_CHART` | 공통 | 없음 | 없음 | 모든 key |
| `CHART_SERIES` | `COLLECTION_CHART` | 공통 | `fromDate`, `toDate` | 없음 | 없음 |
| `NEWS_EVENT` | `NEWS_EVENT` | 공통 | 없음 | `fromDate`, `toDate` | 없음 |

mode 판정 규칙:

- `NEWS_EVENT`는 list mode만 지원한다
- `CHART_SERIES`는 mode를 두지 않고 `locator.kind`로만 판정한다

requiredness validation 규칙:

- required key는 template `query`와 issue state override query를 merge한 effective query 기준으로 판정한다.
- required key 누락 시 preview/publish 모두 fail-fast
- forbidden key 존재 시 preview/publish 모두 fail-fast
- `fromDate`와 `toDate`가 함께 존재하면 `fromDate <= toDate`를 만족해야 한다
- `COLLECTION_CHART`는 template `query`를 비워 둘 수 있고, 이 경우 required key는 issue state override가 모두 제공해야 한다
- `NEWS_EVENT`는 현재 `nws_ltr_pub_id` 기준 조회를 사용하므로 `fromDate`, `toDate`는 runtime 필수 입력이 아니다. legacy row에 남아 있는 값은 허용하되 필수 검사에는 포함하지 않는다.
- `NEWS_ITEM`의 사용자 수정은 issue state가 아니라 snapshot 편집 계층에서 처리한다.

### 3-5. `assign`

`assign`의 canonical truth source는 `assign.fields`다.

shape:

```json
{
  "fields": [
    {
      "fieldKey": "items",
      "sourcePath": "items",
      "required": true
    }
  ]
}
```

허용 필드:

- `fields`: required, non-empty array

`fields[]` 허용 속성:

- `fieldKey`: required string
- `sourcePath`: required string
- `required`: optional boolean, default `false`

추가 속성은 금지한다.

`fieldKey` 규칙:

- block 내에서 unique 해야 한다
- `NewsletterTemplateBlockField.fld_key`와 1:1 대응해야 한다
- canonical block에서는 snapshot content에 노출할 key를 `fieldKey`로 고정한다

`sourcePath` 규칙:

- simple dot path만 허용한다
- 허용 문법: `[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+|\\.\\*)*`
- 배열 wildcard는 `*`만 허용한다
- index 접근, 함수 호출, filter 문법, arithmetic 문법 금지

### 3-6. `transform`

`transform`은 자유 DSL이 아니라 제한 enum rule array다.

shape:

```json
[
  { "type": "SPLIT_LINES", "sourcePath": "items.*.content", "dropBlank": true }
]
```

허용 `type` enum:

- `SPLIT_LINES`
- `FORMAT_DATE`
- `TO_EPOCH_MILLIS`
- `LIMIT_ITEMS`
- `YN_TO_BOOLEAN`

rule별 허용 필드:

| type | 필수 필드 | 선택 필드 | 규칙 |
|---|---|---|---|
| `SPLIT_LINES` | `sourcePath` | `dropBlank` | string -> string[] |
| `FORMAT_DATE` | `sourcePath`, `format` | - | `format` enum만 허용 |
| `TO_EPOCH_MILLIS` | `sourcePath` | `timezone` | default `UTC` |
| `LIMIT_ITEMS` | `sourcePath` | `maxItems` | list truncate |
| `YN_TO_BOOLEAN` | `sourcePath` | - | `Y/N` -> boolean |

보조 enum:

- `FORMAT_DATE.format`: `YYYY-MM-DD`, `YYYY년 MM월 DD일`
- `TO_EPOCH_MILLIS.timezone`: 현재는 `UTC`만 허용

추가 규칙:

- `transform` rule은 선언 순서대로 실행한다
- `LIMIT_ITEMS.maxItems`는 literal only다. query/state binding 문법은 금지한다
- 동일 `sourcePath`에 대해 상충 transform 정의 시 오류다

### 3-7. `overridePolicy`

`overridePolicy`는 issue state가 override 가능한 publish-time assembly input whitelist다.

shape:

```json
{
  "issueState": {
    "query": ["fromDate", "toDate"],
    "state": ["plotOnChart", "removed"],
    "contentInput": ["text", "title", "items.*.title", "items.*.content"]
  }
}
```

허용 필드:

- `issueState`: required object
- `issueState.query`: optional array
- `issueState.state`: optional array
- `issueState.contentInput`: optional array

whitelist 규칙:

- `query`는 해당 family의 허용 query key subset이어야 한다
- `state`는 현재 `plotOnChart`, `removed`만 허용한다
- `contentInput`은 state-owned editable subset이어야 한다
- 즉 `assign.fields.fieldKey`와 `BlockField.fld_key` 중 `is_editable_yn='Y'`이면서 issue block state가 직접 소유하는 값만 포함할 수 있다
- whitelist에 없는 입력은 issue state에 존재해도 무시하지 않고 validation error로 처리한다
- 단, `NEWS_ITEM`은 runtime-expanded snapshot block이므로 issue state override를 사용하지 않는다

### 3-8. Field Ownership Metadata Boundary

`tmd_nwsltrtmpltblkfld.fld_meta`는 canonical execution truth source가 아니다.

- `fld_meta.valueSource`, `defaultValue`, `editorMutable`, `stateScoped` 같은 값은 UI/ownership metadata다
- canonical execution truth source는 여전히 `data_src_type_cd` + `data_src_spec_json`이다
- `fld_meta.valueSource`를 근거로 DB row에 raw SQL, table name, 자유 path를 저장하지 않는다

`STATIC` family의 source-derived 기본값은 아래 경계로 처리한다.

- `INLINE` block은 issue state/content input이 없을 때 application ownership resolver가 기본값을 조립할 수 있다
- 이 기본값 resolver는 field ownership matrix와 application code 규칙을 사용한다
- `fld_meta.valueSource`는 resolver 구현 참고 metadata일 수 있으나 publish/preview execution 정본은 아니다

즉, canonical contract는 "어떤 field를 어떤 payload key로 조립하는가"를 정의하고, source-derived 기본값 계산은 application ownership 계층이 책임진다.

## 4. Representative Family Examples

### 4-1. `STATIC`

```json
{
  "locator": { "kind": "PUBLISH_CONTEXT" },
  "query": {},
  "assign": {
    "fields": [
      { "fieldKey": "text", "sourcePath": "text", "required": true },
      { "fieldKey": "publishDate", "sourcePath": "publishDate", "required": false },
      { "fieldKey": "issueLabel", "sourcePath": "issueLabel", "required": false }
    ]
  },
  "transform": [],
  "overridePolicy": {
    "issueState": {
      "query": [],
      "state": [],
      "contentInput": ["text"]
    }
  }
}
```

### 4-2. `DYNAMIC_QUERY`

```json
{
  "locator": { "kind": "PREFERENCE_TABLE", "refId": 4102 },
  "query": {},
  "assign": {
    "fields": [
      { "fieldKey": "description", "sourcePath": "description", "required": false },
      { "fieldKey": "columns", "sourcePath": "columns", "required": true },
      { "fieldKey": "rows", "sourcePath": "rows", "required": true }
    ]
  },
  "transform": [],
  "overridePolicy": {
    "issueState": {
      "query": [],
      "state": [],
      "contentInput": []
    }
  }
}
```

### 4-3. `CHART_SERIES`

```json
{
  "locator": { "kind": "DEDICATED_CHART", "refCode": "OIL_PRICE" },
  "query": {},
  "assign": {
    "fields": [
      { "fieldKey": "timestamps", "sourcePath": "timestamps", "required": true },
      { "fieldKey": "series", "sourcePath": "series", "required": true }
    ]
  },
  "transform": [],
  "overridePolicy": {
    "issueState": {
      "query": [],
      "state": ["plotOnChart"],
      "contentInput": []
    }
  }
}
```

### 4-4. `NEWS_EVENT`

```json
{
  "locator": { "kind": "NEWS_EVENT" },
  "query": {},
  "assign": {
    "fields": [
      { "fieldKey": "newsId", "sourcePath": "newsId", "required": false },
      { "fieldKey": "title", "sourcePath": "title", "required": true },
      { "fieldKey": "content", "sourcePath": "content", "required": true },
      { "fieldKey": "timestamp", "sourcePath": "timestamp", "required": false },
      { "fieldKey": "createdDate", "sourcePath": "createdDate", "required": true },
      { "fieldKey": "createTime", "sourcePath": "createTime", "required": false },
      { "fieldKey": "source", "sourcePath": "source", "required": false },
      { "fieldKey": "originalUrl", "sourcePath": "originalUrl", "required": false },
      { "fieldKey": "showInChart", "sourcePath": "showInChart", "required": false },
      { "fieldKey": "trend", "sourcePath": "trend", "required": false }
    ]
  },
  "transform": [],
  "overridePolicy": {
    "issueState": {
      "query": [],
      "state": [],
      "contentInput": []
    }
  }
}
```

## 4. Request DTO Draft

external API slice는 없지만 template admin DTO와 internal validator DTO는 다음 구조를 기준으로 한다.

```java
public record DataSourceSpecDto(
    LocatorDto locator,
    QueryDto query,
    List<TransformRuleDto> transform,
    AssignDto assign,
    OverridePolicyDto overridePolicy
) {}
```

```java
public record LocatorDto(String kind, Long refId, String refCode) {}
```

```java
public record AssignDto(List<AssignFieldDto> fields) {}
public record AssignFieldDto(String fieldKey, String sourcePath, Boolean required) {}
```

```java
public record OverridePolicyDto(IssueStateOverrideDto issueState) {}
public record IssueStateOverrideDto(
    List<String> query,
    List<String> state,
    List<String> contentInput
) {}
```

`QueryDto`는 자유 map이 아니라 family별 whitelist 검증을 전제로 한 제한 object 또는 typed validator 계층으로 처리한다.

## 5. Response DTO Draft

template read/detail 응답은 최소 아래 정보를 포함하는 것이 적절하다.

```java
public record NewsletterTemplateBlockSpecRes(
    Long blockId,
    String dataSourceTypeCode,
    String dataSourceRef,
    JsonNode dataSrcSpecJson,
    String executionModelClass, // CANONICAL | LEGACY_COMPAT | INVALID_MIXED
    List<String> validationErrors
) {}
```

preview/publish 에러 응답은 `BaseResponse<?>` + 명시적 error code를 사용한다.

## 6. Validation Rules

### 6-1. Canonical Block Validation

`data_src_spec_json`이 존재하면 아래를 모두 만족해야 한다.

- JSON parse 가능
- top-level key가 whitelist와 정확히 일치
- `data_src_type_cd`와 `locator.kind` 조합이 family matrix와 일치
- `assign.fields` 존재, non-empty, unique
- `assign.fields.fieldKey` 전부가 실제 `BlockField.fld_key`에 존재
- `overridePolicy.issueState.contentInput`는 state-owned editable field subset
- `overridePolicy.issueState.query`는 family query whitelist subset
- `overridePolicy.issueState.state`는 허용 enum subset
- `transform` rule이 whitelist를 위반하지 않음
- `query`가 family별 금지 key를 포함하지 않음
- `query`가 family+locator+mode requiredness matrix를 만족함

### 6-2. Legacy Compatibility Validation

`data_src_spec_json`이 없을 때만 legacy validation을 수행한다.

- `data_src_type_cd`는 존재해야 한다
- `data_src_ref`는 family별 legacy format을 만족해야 한다
- `col_mapping`, `fld_meta`는 현재 legacy strategy가 해석 가능한 shape만 허용한다
- legacy fallback에서만 `fld_meta.maxItems` 사용 가능
- legacy fallback에서만 `col_mapping`을 assignment source로 사용 가능

### 6-3. Conflict Validation

canonical block에서 아래는 모두 fail-fast 대상이다.

- `locator`와 `data_src_ref` translation 결과가 불일치
- `assign.fields`와 `col_mapping` translation 결과가 불일치
- `overridePolicy`가 허용하지 않은 issue state key가 저장/전달됨
- canonical spec이 존재하지만 누락 필드를 legacy 값으로 메우려는 경우

## 7. Representative Error Scenarios

- `INVALID_DATA_SRC_SPEC_JSON`: JSON parse 실패
- `UNSUPPORTED_DATA_SRC_SPEC_KEY`: top-level 또는 nested key 위반
- `DATA_SRC_TYPE_LOCATOR_MISMATCH`: family와 locator 조합 불일치
- `CANONICAL_ASSIGNMENT_MISSING`: canonical block인데 `assign.fields` 없음
- `ASSIGN_FIELD_NOT_DEFINED`: `assign.fields.fieldKey`가 block field와 불일치
- `ISSUE_STATE_OVERRIDE_NOT_ALLOWED`: whitelist 외 issue state 입력 존재
- `CANONICAL_LEGACY_CONFLICT`: canonical spec과 legacy 값 충돌
- `LEGACY_FALLBACK_NOT_ALLOWED`: canonical block에서 fallback 시도
- `LEGACY_REFERENCE_INVALID`: legacy `data_src_ref` format 오류

## 8. Swagger Annotation Recommendation

신규 endpoint가 없더라도 template admin API에는 아래 수준의 문서화가 필요하다.

- `@Schema(description = "Canonical block execution spec. Allowed keys: locator, query, assign, transform, overridePolicy")`
- `@Schema(description = "Canonical execution model classification", allowableValues = {"CANONICAL", "LEGACY_COMPAT", "INVALID_MIXED"})`
- family별 예시는 OpenAPI example로 별도 제공

`Map<String, Object>`로 열어두기보다 typed DTO + custom validator 조합을 권장한다.

## 9. Data/Model Impact

- block entity에 nullable `data_src_spec_json` JSON 컬럼 추가 전제
- `NewsletterTemplateBlock`에 raw json 필드 추가
- `TemplateTree.BlockNode`에 raw/parsed spec 접근 경로 추가
- `BlockField`는 계속 유지하되 canonical assignment truth source는 `assign.fields`
- `data_src_ref`, `col_mapping`, `fld_meta`는 호환 레이어 전용으로 유지

schema impact는 column addition 수준이며 manual DBA rollout에 맞게 nullable 시작이 적절하다.

## 10. Recommended Data Access Approach

이번 slice의 핵심은 query 자체가 아니라 contract validation과 runtime classification이므로 primary 접근은 다음이 적절하다.

- template/issue/block state 로딩: JPA 유지
- dynamic source 실행: 기존 MyBatis/JPA 혼합 유지
- canonical/legacy 판정과 validation: service layer 전용 validator/component

즉, 데이터 접근 기술을 바꾸기보다 resolver 입력 계약을 정규화하는 것이 우선이다.

## 11. Implementation Touch Points

- `NewsletterTemplateBlock`
  - `dataSrcSpecJson` raw string/json 필드 추가
- `TemplateTree.BlockNode`
  - `dataSourceSpecJson` 또는 parsed spec 보유
- `TemplateLoader`
  - spec raw load 포함
- 신규 `BlockDataSourceSpecValidator`
  - create/update/preview/publish 공통 검증
- 신규 `BlockExecutionClassifier`
  - `CANONICAL`, `LEGACY_COMPAT`, `INVALID_MIXED` 판정
- `BlockDataResolver`
  - canonical path 우선, legacy fallback 분기
- preview/publish validator
  - `overridePolicy` 기준 issue state key 검증
- `NewsletterIssuePreviewWriteService`
  - save 시 spec whitelist 위반 차단
- publish pipeline
  - publish 직전 전체 block fail-fast 검증

## 12. Canonical / Legacy Responsibility Boundary

### 12-1. Truth Source

- `data_src_type_cd`: execution family
- `data_src_spec_json.locator`: canonical source locator
- `data_src_spec_json.query`: canonical default execution inputs
- `data_src_spec_json.assign`: canonical result assignment
- `data_src_spec_json.transform`: canonical allowed post-processing
- `data_src_spec_json.overridePolicy`: issue state override whitelist
- `data_src_ref`: legacy fallback / bridge key
- `col_mapping`, `fld_meta`: legacy read compatibility only

### 12-2. Classification Rules

- `CANONICAL`
  - `data_src_spec_json` 존재
  - canonical validation 통과
  - legacy 값은 있어도 bridge/compat 용도로만 존재하며 conflict 없음
- `LEGACY_COMPAT`
  - `data_src_spec_json` 없음
  - `data_src_type_cd` + `data_src_ref` + 필요 시 `col_mapping`/`fld_meta`로 실행 가능
- `INVALID_MIXED`
  - `data_src_spec_json` 존재하지만 canonical validation 실패
  - 또는 canonical 값과 legacy 값이 충돌
  - 또는 canonical spec이 존재하면서 legacy fallback을 요구하는 shape

## 13. Compatibility Rules

### 13-1. `data_src_ref`

legacy bridge translation 허용 범위:

| family | canonical locator | legacy `data_src_ref` |
|---|---|---|
| `DYNAMIC_QUERY` | `PREFERENCE_TABLE.refId=4102` | `pref_table:4102` |
| `CHART_SERIES` + `DEDICATED_CHART` | `refCode=OIL_PRICE` | `dedi:OIL_PRICE` |
| `CHART_SERIES` + `COLLECTION_CHART` | `refCode=OIL_PRICE` | `cltn:OIL_PRICE` |
| `CHART_SERIES` + `MAIN_CHART` | `refCode=WTI` or blank | `WTI` or blank |

규칙:

- canonical block에서 `data_src_ref`는 optional bridge metadata일 뿐 실행 truth source가 아니다
- translation 가능한 family에서만 coexist 허용
- translation 불가능한 family에서 non-null `data_src_ref`는 원칙적으로 오류다

### 13-2. `col_mapping`

- canonical assignment truth source는 항상 `assign.fields`
- 신규 block은 `assign.fields` 필수, `col_mapping` 비의존
- legacy block은 `assign.fields`가 없을 때만 `col_mapping` fallback 허용
- canonical block에서 `assign.fields`가 없고 `col_mapping`만 존재하면 `INVALID_MIXED`

### 13-3. `fld_meta`

- legacy block에서만 execution compatibility key로 읽는다
- 허용 compatibility key는 `maxItems`, `orderBy`, `format`만 한정한다
- canonical block에서 동일 의미의 값은 `query` 또는 `transform`으로 승격되어야 한다
- canonical block의 `fld_meta`는 UI metadata로만 남길 수 있으나 execution 의미가 충돌하면 오류다

## 14. Publish / Preview / Read Resolution Order

### 14-1. Preview

1. block classification 수행
2. `INVALID_MIXED`면 즉시 오류 반환
3. `CANONICAL`이면 canonical validation + conflict validation 수행
4. validation 통과 시 canonical resolver 실행
5. `LEGACY_COMPAT`이면 legacy validation 후 fallback resolver 실행
6. resolved content와 issue state를 snapshot assembler로 전달

### 14-2. Publish

1. template 전체 block에 대해 classification 수행
2. 하나라도 `INVALID_MIXED`면 전체 publish 중단
3. canonical block 전체에 대해 fail-fast validation 수행
4. legacy block은 legacy validation 수행
5. canonical/legacy path로 resolve
6. snapshot assemble
7. execution trace는 snapshot에 저장하지 않고 별도 debug/audit 채널로 기록

### 14-3. Read

1. published issue의 `org_snpsht_json`이 존재하면 snapshot만 읽는다
2. published read는 source re-resolution을 하지 않는다
3. admin/template read에서는 block classification과 validation 결과를 같이 보여줄 수 있다

## 15. Publish Fail-fast Rules

아래 조건 중 하나라도 만족하면 publish는 중단한다.

- `data_src_spec_json` parse 실패
- canonical top-level/nested key 위반
- `data_src_type_cd` 누락 또는 unsupported
- canonical `locator.kind` / family mismatch
- canonical block인데 `assign.fields` 누락
- canonical block인데 legacy 값 보정이 필요함
- canonical vs legacy bridge conflict
- `overridePolicy` 밖의 issue state 입력 존재
- `assign.fields.fieldKey`와 block field 정의 불일치
- `plotOnChart` override가 지원되지 않는 block에 들어옴
- `query.fromDate`, `query.toDate` 형식/범위 오류
- `query` required/forbidden key matrix 위반
- transform rule enum/arg 오류

## 16. Risks / Open Questions

구현 전 추가로 고정해야 할 항목:

- `FORMAT_DATE.format` enum을 공통 formatter enum으로 둘지 여부
- `MAIN_CHART`에서 `refCode` blank 허용을 영구 규칙으로 둘지 여부
- admin template API에서 `dataSrcSpecJson`을 raw JSON string으로 받을지 typed DTO로 받을지
- preview save 단계에서 validation error를 block 단위 aggregate로 반환할지 first-error fail로 반환할지

## 17. Exit Criteria / Handoff

이 contract는 구현 시작 가능한 수준으로 본다. 다만 아래 두 가지는 구현 전 확정이 필요하다.

- enum registry: `FORMAT_DATE.format`
- admin API payload 방식: raw JSON vs typed DTO

`erd-change-review`가 확인할 항목:

- nullable `data_src_spec_json` column addition의 rollout 안전성
- legacy coexist 기간 동안 backfill 없이도 null-safe 인지

`spring-api-implementation`가 바로 구현 가능한 항목:

- entity/DTO 확장
- validator/classifier
- preview/publish fail-fast
- canonical path와 legacy fallback path 분기
