# Newsletter Snapshot Section Fields API Contract

- date: `2026-03-23`
- task type: `api-spec`
- status: `draft for implementation review`
- scope: newsletter snapshot response contract extension for `section.name + section.fields + section.fieldDefs + blocks`

## 1. Feature summary

본 변경은 newsletter snapshot의 section contract를 block 중심 구조에서
`section.name + section.fields + section.fieldDefs + blocks` 구조로 확장하는 API contract 정리다.

핵심 목적은 다음과 같다.

- section이 직접 소유하는 콘텐츠 값을 block payload와 분리한다.
- `REFERENCE_HEADER` block 제거 이후 REFERENCE 계열을 일관된 canonical shape로 고정한다.
- `NEWS_FLOW.title`, `NEWS_FLOW.aggregationTime`, `FOOTER.footerContent`, `REFERENCE1~4.title/description`를 response contract에서 section-level field로 고정한다.
- `COVER`는 예외적으로 기존 `2 block + 4 field` 구조를 유지한다.

이번 문서의 정본 기준은 `response truth`다.

- `response truth`
  - 외부 API 응답 JSON 계약의 canonical shape
- `storage truth`
  - DB/템플릿/스냅샷 저장 구조
  - response truth와 동일할 필요는 없다
- `execution truth`
  - resolver/assembler/runtime이 어떤 입력으로 값을 조립하는지
  - response truth와 동일할 필요는 없다

## 2. Scope / Non-scope

### Scope

- `snapshot.sections[*]` section object schema 확장
- `section.title` 제거 및 `section.name` 중심 contract 정리
- `section.fields` 및 `section.fieldDefs` canonical semantics 고정
- section별 field ownership 규칙 고정
- OpenAPI / DTO / example payload 반영 기준 제시
- additive migration 및 backward compatibility 정책 정의

### Non-scope

- 물리 저장 스키마 즉시 변경 강제
- resolver 내부 구현 세부 알고리즘 확정
- FE 렌더러 상세 UI 구조 정의
- `COVER`를 section-level field 구조로 재모델링
- `TITLE_SPECIALIZED`, `SUMMARY`, `DATE_ISSUE`의 기존 block-owned content를 section-level field로 승격

## 3. Canonical response contract

### 3.1 적용 대상 응답

본 계약은 아래 snapshot payload에 공통 적용한다.

- `GET /api/v1/mi/newsletter/publish-histories/{publishHistoryId}/snapshot`
- `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}?companyNo=...`
- `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/original-snapshot?companyNo=...`

`BaseResponse<T>` envelope는 기존 규칙을 유지한다.

### 3.2 Canonical section shape

```json
{
  "id": "sec-NEWS_FLOW",
  "type": "GENERAL",
  "name": "NEWS_FLOW",
  "order": 4,
  "visibility": true,
  "target": null,
  "state": {
    "removed": false
  },
  "fields": {
    "title": "주요 뉴스 Flow 및 Brent 유가 추이",
    "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지"
  },
  "fieldDefs": {
    "title": {
      "editable": true,
      "required": true
    },
    "aggregationTime": {
      "editable": true,
      "required": true
    }
  },
  "blocks": [
    {
      "id": "blk-NEWS_FLOW_CHART-1",
      "type": "CHART",
      "format": "LINE",
      "order": 1,
      "visibility": true,
      "target": null,
      "state": {
        "removed": false
      },
      "content": {
        "timestamps": [],
        "series": []
      }
    }
  ]
}
```

### 3.3 Contract rules

| 항목 | 규칙 |
|---|---|
| `section.name` | section identifier-facing stable name이다. display label이 아니다. |
| `section.fields` | section direct-owned content value subset만 포함한다. |
| `section.fieldDefs` | `section.fields`에 존재하는 key subset에 대한 편집/필수 여부만 보장한다. |
| `section.blocks` | renderer/execution unit payload를 유지한다. |
| `section.title` | canonical response contract에서 제거한다. |
| block content | section-owned field를 중복 소유하면 안 된다. cleanup 이후 중복 제거가 목표다. |

## 4. Section object schema

### 4.1 Schema

| Field | Type | Required | Nullable | Description |
|---|---|---:|---:|---|
| `id` | `string` | Y | N | snapshot 내 section 식별자 |
| `type` | `string` | Y | N | section renderer/category type |
| `name` | `string` | Y | N | 시스템/계약용 section identifier-facing name |
| `order` | `integer` | Y | N | section display order |
| `visibility` | `boolean` | Y | N | section visibility |
| `target` | `string` | N | Y | target metadata |
| `state` | `object` | N | Y | section state metadata |
| `fields` | `object` | N | Y | section direct-owned content value subset |
| `fieldDefs` | `object` | N | Y | `fields` key subset에 대한 계약 메타 |
| `blocks` | `array<object>` | Y | N | block execution / rendering payload |

### 4.2 Additional rules

- `fields`는 필요한 section에만 존재한다.
- `fieldDefs`는 `fields`가 있을 때만 존재할 수 있다.
- `fieldDefs`의 key는 `fields` key subset이어야 한다.
- `fields` value는 현재 단계에서 `string | null` subset만 사용한다.
- `section.name` 값은 localized display label을 담지 않는다.
- UI용 표시명은 본 계약 범위 밖이다.

## 5. fieldDefs schema

### 5.1 Canonical shape

```json
{
  "title": {
    "editable": true,
    "required": true
  },
  "description": {
    "editable": true,
    "required": false
  }
}
```

### 5.2 Schema definition

| Field | Type | Required | Description |
|---|---|---:|---|
| `editable` | `boolean` | Y | 사용자가 해당 section field를 수정할 수 있는지 |
| `required` | `boolean` | Y | 응답/편집 계약상 필수 입력인지 |

### 5.3 Guaranteed meaning only

`fieldDefs`는 response에서 아래 의미만 보장한다.

- 이 field가 editable인지
- 이 field가 required인지

`fieldDefs`는 아래 의미를 보장하지 않는다.

- storage source
- resolver source path
- validation max length / regex / multiline 여부
- UI widget type
- localization/display rule
- query/runtime assignment detail

즉 `fieldDefs`는 최소 계약 메타이며, execution truth metadata가 아니다.

## 6. Section별 적용 규칙

### 6.1 `COVER`

| 항목 | 규칙 |
|---|---|
| `section.name` | `COVER` |
| `fields` | 사용하지 않는다 |
| `fieldDefs` | 사용하지 않는다 |
| `blocks` | `COVER_IMAGE_CARD`, `COVER_INFO` 유지 |
| 예외 규칙 | 기존 `2 block + 4 field` 구조 유지 |

세부 규칙:

- `COVER`는 section-level fields로 올리지 않는다.
- `COVER_IMAGE_CARD.content`는 `imageBase64`, `showImage`를 가진다.
- `COVER_INFO.content`는 `publishCycle`, `newsletterName`, `newsletterDescription`를 가진다.

### 6.2 `NEWS_FLOW`

| 항목 | 규칙 |
|---|---|
| `section.name` | `NEWS_FLOW` |
| `fields` | `title`, `aggregationTime` |
| `fieldDefs` | `title`, `aggregationTime` 모두 정의 |
| `blocks` | `NEWS_FLOW_CHART`, `NEWS_ITEM[*]` |

세부 규칙:

- `section.fields.title`은 section 직접 소유 title이다.
- `section.fields.aggregationTime`은 section 직접 소유 aggregation time 문구다.
- `NEWS_FLOW_CHART.content`에는 더 이상 `title`, `aggregationTime`가 존재하면 안 된다.
- `NEWS_FLOW_CHART.content`는 chart payload만 가진다.
  - 예: `timestamps`, `series`
- `NEWS_ITEM`은 반복 block이며 section-level field를 소유하지 않는다.

### 6.3 `REFERENCE1~4`

| 항목 | 규칙 |
|---|---|
| `section.name` | `REFERENCE1`, `REFERENCE2`, `REFERENCE3`, `REFERENCE4` |
| `fields` | `title`, `description` |
| `fieldDefs` | `title`, `description` 모두 정의 |
| `blocks` | data block + `REFERENCE_CONTENT` |

세부 규칙:

- `REFERENCE_HEADER` block은 canonical response contract에서 제거한다.
- `REFERENCE1`, `REFERENCE2`, `REFERENCE4`의 data block은 `REFERENCE_CHART`다.
- `REFERENCE3`의 data block은 `REFERENCE_TABLE`다.
- `section.fields.title`, `section.fields.description`은 section 직접 소유 값이다.
- chart/table/content block은 section field와 별도 책임을 가진다.
- `REFERENCE_CONTENT.content.content`는 본문 콘텐츠 책임만 가진다.
- `REFERENCE_CHART`/`REFERENCE_TABLE`는 데이터 렌더링 payload 책임만 가진다.

REFERENCE canonical blocks shape:

```json
{
  "name": "REFERENCE1",
  "fields": {
    "title": "WTI 및 달러 인덱스 5분 차트",
    "description": "WTI: 청색, 좌측\n달러인덱스: 회색, 우측"
  },
  "blocks": [
    {
      "type": "CHART",
      "content": {
        "timestamps": [],
        "series": []
      }
    },
    {
      "type": "TEXT",
      "content": {
        "content": "참조 설명 본문"
      }
    }
  ]
}
```

### 6.4 `FOOTER`

| 항목 | 규칙 |
|---|---|
| `section.name` | `FOOTER` |
| `fields` | `footerContent` |
| `fieldDefs` | `footerContent` 정의 |
| `blocks` | `FOOTER_NOTICE` 유지 |

세부 규칙:

- `section.fields.footerContent`는 footer section field다.
- `FOOTER_NOTICE.content`에는 `footerContent`가 없어야 한다.
- `FOOTER_NOTICE.content`에는 `showLogo`만 남는다.
- logo on/off는 block responsibility이고, footer 문구는 section responsibility다.

### 6.5 `SUMMARY`

| 항목 | 규칙 |
|---|---|
| `section.name` | `SUMMARY` |
| `fields` | 사용하지 않는다 |
| `fieldDefs` | 사용하지 않는다 |
| `blocks` | `SUMMARY_BODY` 유지 |

세부 규칙:

- `summaryText`는 계속 block-owned field다.
- section-level field 승격 대상이 아니다.

### 6.6 `TITLE_SPECIALIZED`

| 항목 | 규칙 |
|---|---|
| `section.name` | `TITLE_SPECIALIZED` |
| `fields` | 사용하지 않는다 |
| `fieldDefs` | 사용하지 않는다 |
| `blocks` | `TITLE_MAIN_SPECIALIZED` 유지 |

세부 규칙:

- `title`, `subtitle`, `priceChangeFrom`, `priceChangeTo`는 block-owned field다.
- `section.fields.title`와 혼동하지 않도록 block content field로 유지한다.

### 6.7 `DATE_ISSUE`

| 항목 | 규칙 |
|---|---|
| `section.name` | `DATE_ISSUE` |
| `fields` | 사용하지 않는다 |
| `fieldDefs` | 사용하지 않는다 |
| `blocks` | `DATE_ISSUE_INFO` 유지 |

세부 규칙:

- `publishDate`, `issueNumber`는 계속 block-owned field다.
- section-level field 승격 대상이 아니다.

## 7. Naming rules

### 7.1 `section.name`

- `section.name`은 시스템/계약상 section identifier-facing name이다.
- display label이 아니다.
- canonical 값은 stable identifier를 사용한다.
  - `COVER`
  - `DATE_ISSUE`
  - `TITLE_SPECIALIZED`
  - `SUMMARY`
  - `NEWS_FLOW`
  - `REFERENCE1`
  - `REFERENCE2`
  - `REFERENCE3`
  - `REFERENCE4`
  - `FOOTER`
- localized human-readable label은 `section.name`에 넣지 않는다.

### 7.2 `section.fields.title`

- section이 직접 소유하는 user-facing title 값이다.
- `section.name`의 alias가 아니다.
- 적용 section:
  - `NEWS_FLOW`
  - `REFERENCE1`
  - `REFERENCE2`
  - `REFERENCE3`
  - `REFERENCE4`

### 7.3 `section.fields.description`

- section이 직접 소유하는 보조 설명/annotative text다.
- 적용 section:
  - `REFERENCE1`
  - `REFERENCE2`
  - `REFERENCE3`
  - `REFERENCE4`

### 7.4 `section.fields.aggregationTime`

- section이 직접 소유하는 aggregation/observation time 문구다.
- 적용 section:
  - `NEWS_FLOW`

### 7.5 `section.fields.footerContent`

- footer section이 직접 소유하는 본문 텍스트다.
- 적용 section:
  - `FOOTER`

## 8. Backward compatibility policy

### 8.1 Response truth policy

- canonical response는 `section.title`을 더 이상 노출하지 않는다.
- canonical response는 필요한 section에만 `fields`, `fieldDefs`를 추가한다.
- canonical response에서 section-owned field는 block content에 중복 노출하지 않는 것이 목표다.

### 8.2 Additive migration policy

초기 rollout은 additive migration을 따른다.

1. DTO/OpenAPI에 `fields`, `fieldDefs`, `name`를 추가한다.
2. assembler는 신규 contract를 채운다.
3. storage/execution 계층은 필요 시 기존 source를 그대로 읽어도 된다.
4. cleanup 전환 전까지 내부적으로는 legacy source field를 읽더라도 외부 response는 신규 contract를 우선한다.

### 8.3 Legacy compatibility rule

- read path에서는 legacy snapshot/template source를 해석해 신규 response shape로 promote할 수 있다.
- write/save path가 있다면 신규 revision/delta path는 `section.fields.*` 기준으로 저장하는 방향을 권장한다.
- cleanup 완료 전까지 assembler 내부 dual-read는 허용한다.
- 단, response dual-write는 권장하지 않는다.
  - 즉 외부 JSON에 `section.title`과 `section.name`을 동시 노출하지 않는다.
  - 외부 JSON에 `section.fields.title`과 `block.content.title`을 동시 노출하지 않는 방향을 기본으로 한다.

## 9. Migration / rollout notes

### 9.1 Response truth

- OpenAPI schema를 먼저 갱신한다.
- example payload를 신규 shape로 교체한다.
- `SnapshotSection` DTO를 `name`, `fields`, `fieldDefs` 기준으로 갱신한다.

### 9.2 Storage truth

- storage truth는 additive migration 중 즉시 동일 shape일 필요가 없다.
- template physical schema 변경은 후속 단계로 미뤄도 된다.
- legacy stored source에 `REFERENCE_HEADER`, `FOOTER_NOTICE.footerContent`, `NEWS_FLOW_CHART.title`가 남아 있어도 assembler에서 section field로 promote 가능하다.

### 9.3 Execution truth

- assembler/resolver는 legacy source에서 읽더라도 최종 response는 신규 contract로 normalize해야 한다.
- `NEWS_FLOW_CHART`는 response 직전 normalize 단계에서 `title`, `aggregationTime` 제거가 필요하다.
- `FOOTER_NOTICE`는 response 직전 normalize 단계에서 `footerContent` 제거가 필요하다.
- `REFERENCE_HEADER`는 response 조립 결과에 포함되면 안 된다.

### 9.4 Rollout sequence

1. OpenAPI / DTO / example payload 반영
2. assembler normalize 반영
3. FE parser를 `section.fields` 기준으로 전환
4. legacy source cleanup
5. 저장 구조 정리 여부 후속 결정

## 10. OpenAPI example payload

아래 예시는 `BaseResponse<NewsletterDetailV2Res>` 내 `snapshot.data.sections` 일부 예시다.

```json
{
  "resultCode": "SUCCESS",
  "data": {
    "newsletterId": 12,
    "issueNo": 103,
    "publishedAt": "2026-03-23T09:00:00",
    "subscribed": true,
    "revision": null,
    "snapshot": {
      "revisionNo": null,
      "savedAt": "2026-03-23T09:00:00",
      "data": {
        "sections": [
          {
            "id": "sec-COVER",
            "type": "FIXED",
            "name": "COVER",
            "order": 1,
            "visibility": true,
            "state": {
              "removed": false
            },
            "blocks": [
              {
                "id": "blk-COVER_IMAGE_CARD-1",
                "type": "IMAGE",
                "order": 1,
                "visibility": true,
                "content": {
                  "imageBase64": "data:image/png;base64,...",
                  "showImage": true
                }
              },
              {
                "id": "blk-COVER_INFO-1",
                "type": "TEXT",
                "order": 2,
                "visibility": true,
                "content": {
                  "publishCycle": "WEEKLY",
                  "newsletterName": "Weekly Market Pulse",
                  "newsletterDescription": "시장 핵심 이슈를 요약한 뉴스레터"
                }
              }
            ]
          },
          {
            "id": "sec-NEWS_FLOW",
            "type": "GENERAL",
            "name": "NEWS_FLOW",
            "order": 5,
            "visibility": true,
            "state": {
              "removed": false
            },
            "fields": {
              "title": "주요 뉴스 Flow 및 Brent 유가 추이",
              "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지"
            },
            "fieldDefs": {
              "title": {
                "editable": true,
                "required": true
              },
              "aggregationTime": {
                "editable": true,
                "required": true
              }
            },
            "blocks": [
              {
                "id": "blk-NEWS_FLOW_CHART-1",
                "type": "CHART",
                "format": "LINE",
                "order": 1,
                "visibility": true,
                "content": {
                  "timestamps": ["2026-03-22T20:00:00", "2026-03-23T17:00:00"],
                  "series": [
                    {
                      "name": "Brent",
                      "data": [71.2, 72.1]
                    }
                  ]
                }
              },
              {
                "id": "blk-NEWS_ITEM-1001",
                "type": "NEWS_CARD",
                "order": 2,
                "visibility": true,
                "plotOnChart": true,
                "content": {
                  "newsId": "1001",
                  "title": "유가 반등 지속",
                  "content": "수급 우려로 Brent가 상승했다.",
                  "timestamp": "2026-03-23T08:20:00",
                  "source": "Reuters",
                  "originalUrl": "https://example.com/news/1001",
                  "showInChart": true,
                  "trend": "UP"
                }
              }
            ]
          },
          {
            "id": "sec-REFERENCE1",
            "type": "GENERAL",
            "name": "REFERENCE1",
            "order": 6,
            "visibility": true,
            "fields": {
              "title": "WTI 및 달러 인덱스 5분 차트",
              "description": "WTI: 청색, 좌측\n달러인덱스: 회색, 우측"
            },
            "fieldDefs": {
              "title": {
                "editable": true,
                "required": true
              },
              "description": {
                "editable": true,
                "required": false
              }
            },
            "blocks": [
              {
                "id": "blk-REFERENCE_CHART-1",
                "type": "CHART",
                "order": 1,
                "visibility": true,
                "content": {
                  "timestamps": ["2026-03-23T09:00:00"],
                  "series": []
                }
              },
              {
                "id": "blk-REFERENCE_CONTENT-1",
                "type": "TEXT",
                "order": 2,
                "visibility": true,
                "content": {
                  "content": "장중 WTI와 달러 인덱스의 역상관이 유지되었다."
                }
              }
            ]
          },
          {
            "id": "sec-FOOTER",
            "type": "FIXED",
            "name": "FOOTER",
            "order": 10,
            "visibility": true,
            "fields": {
              "footerContent": "본 메일은 Market Intelligence Platform 자동 발행본입니다."
            },
            "fieldDefs": {
              "footerContent": {
                "editable": true,
                "required": false
              }
            },
            "blocks": [
              {
                "id": "blk-FOOTER_NOTICE-1",
                "type": "TEXT",
                "order": 1,
                "visibility": true,
                "content": {
                  "showLogo": true
                }
              }
            ]
          }
        ]
      }
    },
    "deltaCount": 0,
    "deltaChanges": []
  }
}
```

## 11. DTO draft

### 11.1 Snapshot section DTO

```java
package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotSection {
    private String id;
    private String type;
    private String name;
    private Integer order;
    private Boolean visibility;
    private String target;
    private Map<String, Object> state;
    private Map<String, Object> fields;
    private Map<String, SnapshotSectionFieldDef> fieldDefs;
    private List<SnapshotBlock> blocks;
}
```

### 11.2 Section field definition DTO

```java
package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotSectionFieldDef {
    private Boolean editable;
    private Boolean required;
}
```

### 11.3 Optional typed field DTO candidate

typed DTO가 필요하면 아래 shape를 후보로 사용한다.
다만 현재 단계에서는 `Map<String, Object>` + `Map<String, SnapshotSectionFieldDef>`가 additive migration에 더 안전하다.

```java
public record SnapshotSectionFields(
    String title,
    String description,
    String aggregationTime,
    String footerContent
) {}
```

## 12. Representative OpenAPI schema draft

```yaml
SnapshotSection:
  type: object
  required: [id, type, name, order, visibility, blocks]
  properties:
    id:
      type: string
    type:
      type: string
    name:
      type: string
      description: Stable section identifier-facing name. Not a display label.
      enum:
        - COVER
        - DATE_ISSUE
        - TITLE_SPECIALIZED
        - SUMMARY
        - NEWS_FLOW
        - REFERENCE1
        - REFERENCE2
        - REFERENCE3
        - REFERENCE4
        - FOOTER
    order:
      type: integer
    visibility:
      type: boolean
    target:
      type: string
      nullable: true
    state:
      type: object
      additionalProperties: true
    fields:
      type: object
      nullable: true
      additionalProperties:
        type: string
        nullable: true
    fieldDefs:
      type: object
      nullable: true
      additionalProperties:
        $ref: '#/components/schemas/SnapshotSectionFieldDef'
    blocks:
      type: array
      items:
        $ref: '#/components/schemas/SnapshotBlock'

SnapshotSectionFieldDef:
  type: object
  required: [editable, required]
  properties:
    editable:
      type: boolean
    required:
      type: boolean
```

## 13. Implementation touch points

- `src/main/java/com/sk/domain/mi/dto/snapshot/SnapshotSection.java`
- `src/main/java/com/sk/domain/mi/service/newsletter/SnapshotAssembler.java`
- `docs/newsletter-v2-openapi.yaml`
- snapshot example payload를 포함하는 문서 및 테스트 fixture

권장 데이터 접근 스타일은 기존 newsletter snapshot assemble 구조를 유지하는 것이다.
즉 본 변경은 query style 변경보다 assembler normalize 책임 추가가 핵심이다.

## 14. Open questions

이번 턴의 고정 결정은 다시 열지 않는다.

남은 open question은 아래 1건만 분리한다.

### 14.1 `fields` / `fieldDefs`의 empty-object 정책

현재 권장안:

- 필드가 없으면 `fields`, `fieldDefs`를 아예 생략한다.
- empty object `{}`를 기본값으로 강제하지 않는다.

확정 필요 이유:

- OpenAPI example 일관성
- FE parser null-safe 처리 방식
- Jackson `NON_NULL` 직렬화 기준과의 정합성

## 15. Readiness

`ready to implement`

판정 근거:

- response truth 기준 canonical shape가 section별로 충분히 고정되었다.
- `REFERENCE_HEADER` 제거 이후 REFERENCE blocks shape가 명시되었다.
- `NEWS_FLOW`, `REFERENCE1~4`, `FOOTER`의 section-level ownership이 명확하다.
- additive migration, backward compatibility, DTO/OpenAPI 반영 기준이 구현 직전 수준으로 정리되었다.
