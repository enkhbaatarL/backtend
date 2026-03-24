# Newsletter Section / Block / Field Modeling Guide

## 1. 목적
이 문서는 뉴스레터 v2에서 화면 요소를 `section`, `block`, `field` 중 어디에 두는 것이 가장 자연스러운지 판단하는 기준과,
현재 구현 모델과 이상적 모델 사이의 gap을 한 번에 보여주기 위한 가이드다.

이 문서는 두 관점을 분리한다.

- `design-first model`
  - 구현 제약과 무관하게 가장 자연스러운 구조
- `current runtime model`
  - 현재 snapshot/template/runtime이 실제로 사용하는 구조

## 2. 핵심 원칙

### 2.1 Section
section은 하나의 화면 의미 묶음이다.

section에 두는 것이 자연스러운 정보:
- 섹션 전체를 설명하는 제목
- 섹션 전체 관찰 범위/기간/메타 정보
- 섹션 단위 상태
- section 본문에 직접 속하는 값

### 2.2 Block
block은 하나의 렌더링 단위이자 실행 단위다.

block에 두는 것이 자연스러운 정보:
- 하나의 컴포넌트가 바로 소비하는 payload
- chart/table/image/news-card처럼 renderer가 직접 그리는 단위
- 독립적으로 숨김/반복/삭제/상태를 가질 수 있는 단위

### 2.3 Field
field는 검증, 편집, ownership을 구분해야 하는 가장 작은 값 단위다.

field로 분리해야 하는 경우:
- required/optional이 다르다
- source가 다르다
- editable 여부가 다르다
- 길이/포맷 제약이 다르다
- 추후 선택적 노출 가능성이 있다

## 3. 판정 질문
어떤 요소를 어디에 둬야 할지 헷갈리면 아래 순서로 본다.

1. 이 값이 section 전체를 설명하는가?
2. 여러 block이 이 값을 공유하는가?
3. 이 값이 특정 renderer payload의 일부인가?
4. 이 값이 다른 field와 source/validation/lifecycle이 다른가?

판정 규칙:
- `1`, `2`가 강하면 section 후보
- `3`이 강하면 block 후보
- `4`가 강하면 field로 명시

## 4. Section별 정리

| Section | design-first model | current runtime model | 판단 |
|---|---|---|---|
| `COVER` | 2 visual groups + 4 explicit content fields | 2 blocks (`COVER_IMAGE_CARD`, `COVER_INFO`) + 4 fields | 현재 구조와 설계 방향이 크게 충돌하지 않음 |
| `DATE_ISSUE` | section fields 2개로 볼 수도 있으나, 독립 카드처럼 쓰면 block 1개도 자연스러움 | `DATE_ISSUE_INFO` block 1개 | 중립, 현재 구조 유지 가능 |
| `TITLE_SPECIALIZED` | section title이 아니라 content title 영역이므로 block field가 자연스러움 | `TITLE_MAIN_SPECIALIZED` block 1개 | 현재 구조가 자연스러움 |
| `SUMMARY` | block 1개 + `summaryText` field | `SUMMARY_BODY` block 1개 | 현재 구조가 자연스러움 |
| `NEWS_FLOW` | section fields 2개 + chart block + news card blocks | chart/meta 혼합 block + news item blocks | design-first와 current가 가장 크게 갈림 |
| `REFERENCE1~4` | section fields + content/data block 조합이 더 자연스러움 | `REFERENCE_HEADER`, `REFERENCE_CHART/TABLE`, `REFERENCE_CONTENT` | title/description은 section field로 승격하고 `REFERENCE_HEADER`는 제거하는 편이 더 일관적 |
| `FOOTER` | section field 1개 + logo block | `FOOTER_NOTICE` block 1개 | design-first와 current가 갈림 |

## 5. COVER 정리

### 5.1 Design-first answer
`COVER`는 아래처럼 보는 것이 가장 자연스럽다.

- 콘텐츠 속성 4개
  - `coverImage`
  - `publishCycle`
  - `newsletterName`
  - `newsletterDescription`
- 시각 그룹 2개
  - media group
  - meta/text group

### 5.2 Current runtime answer
현재 구현은 아래처럼 되어 있다.

- `COVER_IMAGE_CARD`
  - `imageBase64`
  - `showImage`
- `COVER_INFO`
  - `publishCycle`
  - `newsletterName`
  - `newsletterDescription`

### 5.3 Final modeling position
`COVER`는 아래처럼 정리하는 것이 가장 좋다.

- canonical content interpretation: 4 fields
- runtime/template interpretation: 2 blocks
- UI explanation: 2 visual groups

즉 `2 block + 4 field`가 가장 정확하다.

참고 문서:
- [newsletter-cover-section-modeling-decision.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-cover-section-modeling-decision.md)

## 6. NEWS_FLOW 정리

### 6.1 Design-first answer
가장 자연스러운 구조는 아래다.

- section fields
  - `title`
  - `aggregationTime`
- `NEWS_FLOW_CHART` block
  - `timestamps`
  - `series`
- `NEWS_ITEM` block 또는 반복 block 집합

즉 `title`, `aggregationTime`은 chart block field보다 section-level field로 보는 것이 더 자연스럽다.

### 6.2 Current runtime answer
현재 구현은 아래처럼 묶고 있다.

- `NEWS_FLOW_CHART`
  - `title`
  - `aggregationTime`
  - `timestamps`
  - `series`
- `NEWS_ITEM`
  - runtime-expanded news cards

### 6.3 Final modeling position
이상적 모델만 고르면 `NEWS_FLOW`는 아래처럼 정의하는 편이 더 맞다.

- `NEWS_FLOW.fields.title`
- `NEWS_FLOW.fields.aggregationTime`
- `NEWS_FLOW_CHART.content.timestamps`
- `NEWS_FLOW_CHART.content.series`
- `NEWS_ITEM[*]`

즉 `NEWS_FLOW`는 `section fields 2개 + chart block + news card block(들)`이 더 자연스럽다.

참고 문서:
- [newsletter-news-flow-section-modeling-design-first-review.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-news-flow-section-modeling-design-first-review.md)
- [newsletter-news-flow-section-modeling-decision.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-news-flow-section-modeling-decision.md)

## 7. REFERENCE 계열 정리

### 7.1 Design-first answer
`REFERENCE1~4`는 대부분 아래 구조가 더 자연스럽다.

- section fields
  - `title`
  - `description`
- data block
  - chart 또는 table
- content block
  - 설명/본문

즉 `REFERENCE_HEADER`는 별도 block보다 section field로 흡수하는 편이 맞다.

### 7.2 Current runtime answer
현재 구현은 아래처럼 되어 있다.

- `REFERENCE_HEADER`
- `REFERENCE_CHART` 또는 `REFERENCE_TABLE`
- `REFERENCE_CONTENT`

### 7.3 Final modeling position
설계적으로 더 맞는 구조는 `section fields + data block + content block`이다.
`REFERENCE_HEADER`는 section field 도입 시 제거하는 것이 더 일관적이다.

## 8. 현재 모델과 이상적 모델의 gap

| 항목 | 이상적 모델 | 현재 모델 | gap 수준 |
|---|---|---|---|
| `COVER` | 4 fields + visual grouping | 2 blocks + 4 fields | 낮음 |
| `NEWS_FLOW.title` | section field | chart block field | 높음 |
| `NEWS_FLOW.aggregationTime` | section field | chart block field | 높음 |
| `REFERENCE.title/description` | section field | header block fields | 중간 |
| `FOOTER.footerContent` | section field | block field | 중간 |
| `SUMMARY.summaryText` | block field | block field | 없음 |
| `TITLE_SPECIALIZED.title/subtitle` | block field | block field | 낮음 |

## 9. 목표 모델 기준 snapshot 초안

이상적 모델을 snapshot에 반영하면 section은 아래처럼 확장되어야 한다.

```json
{
  "id": "sec-NEWS_FLOW",
  "type": "GENERAL",
  "name": "News Flow",
  "fields": {
    "title": "주요 뉴스 Flow 및 Brent 유가 추이",
    "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지"
  },
  "blocks": [
    {
      "id": "blk-NEWS_FLOW_CHART-1",
      "type": "CHART",
      "content": {
        "timestamps": [],
        "series": []
      }
    },
    {
      "id": "blk-NEWS_ITEM-1001",
      "type": "NEWS_CARD",
      "content": {
        "newsId": "1001"
      }
    }
  ]
}
```

## 10. Migration 영향

### 10.1 DTO / snapshot
- `SnapshotSection`에 `fields`와 `fieldDefs` 필드 추가 필요
- `SnapshotBlock`에 있던 일부 section-owned field를 section으로 이동

### 10.2 Assembler
- `SnapshotAssembler`가 section-level fields 조립을 지원해야 함
- `NEWS_FLOW_CHART`의 content에서 `title`, `aggregationTime` 제거 가능

### 10.3 Template / ownership
- `TemplateTree.SectionNode` 또는 별도 section field model 도입 필요
- ownership matrix에 section-level field 개념 추가 필요

### 10.4 API / FE
- OpenAPI example payload 변경
- FE parser가 `section.fields`를 읽도록 변경
- FE는 `section.fieldDefs`를 사용해 editable/required를 해석

## 11. 권장 의사결정

### 11.1 지금 당장 모델 원칙만 고정한다면
아래처럼 정리하는 것이 가장 명확하다.

- `COVER`: `2 block + 4 field`
- `NEWS_FLOW`: `section fields 2개 + chart block + news card block`
- `REFERENCE1~4`: `section fields + data block + content block`
- `FOOTER`: `section field + logo block`

### 11.2 구현 전환 우선순위
구조 변경까지 고려한다면 우선순위는 아래를 권장한다.

1. `NEWS_FLOW`의 section fields 도입 여부 결정
2. `FOOTER.footerContent`를 section field로 승격할지 결정
3. `REFERENCE_HEADER` 제거를 전제로 section field 승격 반영
4. section-level field contract를 공통 구조로 도입
5. 그 다음에 snapshot/API/FE 계약을 함께 바꿈

## 12. 한 줄 결론
질문의 취지대로 “현재 구현을 최대한 유지”가 아니라 “무엇이 더 맞는 구조인가”를 기준으로 보면,

- `COVER`는 `2 block + 4 field`
- `NEWS_FLOW`는 `section fields + block`
- `REFERENCE`는 `section fields + block`
- `FOOTER`는 `section field + block` 쪽이 더 자연스럽다.
