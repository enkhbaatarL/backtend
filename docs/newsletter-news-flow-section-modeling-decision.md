# Newsletter NEWS_FLOW Section Modeling Decision

## 1. Scope
- 대상 Figma: NEWS_FLOW section (`node-id=40004413:157567`)
- 대상 요소:
  - News Flow 제목
  - News Flow 시간 정보
  - Chart block
  - News card block 목록
- 목적: `News Flow 제목`, `News Flow 시간 정보`를 `NEWS_FLOW_CHART` block field로 유지할지, section 속성으로 승격할지 결정한다.

## 2. Existing structure reviewed
- 현재 template/runtime 구조는 `NEWS_FLOW` section 아래 아래 2개 block을 사용한다.
  - `NEWS_FLOW_CHART`
  - `NEWS_ITEM`
- `NEWS_FLOW_CHART`의 canonical assign field set은 아래 4개로 고정되어 있다.
  - `title`
  - `aggregationTime`
  - `timestamps`
  - `series`
- `NEWS_ITEM`은 template block 1건이 runtime-expanded snapshot block 목록으로 확장된다.

검토 근거:
- `docs/newsletter-template-v2-field-ownership-matrix.md`
- `docs/newsletter-v2-confluence-guide.md`
- `docs/newsletter-v2-data-src-spec-contract.md`
- `src/main/java/com/sk/domain/mi/service/newsletter/strategy/ChartSeriesBlockDataStrategy.java`
- `src/main/java/com/sk/domain/mi/service/newsletter/BlockExecutionClassifier.java`
- `src/main/java/com/sk/domain/mi/service/newsletter/SnapshotAssembler.java`
- `src/main/java/com/sk/domain/mi/dto/snapshot/SnapshotSection.java`

## 3. Decision summary
결론은 다음과 같다.

- 도메인/계약/저장 관점: `NEWS_FLOW`는 `2개 block`으로 본다.
- `News Flow 제목`, `News Flow 시간 정보`는 section 속성이 아니라 `NEWS_FLOW_CHART` block의 field로 유지한다.
- 화면/레이아웃 관점에서는 chart header처럼 보일 수 있지만, canonical runtime 책임은 section이 아니라 chart block에 둔다.

즉, 이 케이스는 COVER처럼 `2 block + 4 explicit fields`라고 말하기보다, `2 block이며 chart block이 header field와 chart data를 함께 소유한다`가 더 정확하다.

## 4. Why section property is not recommended

### 4.1 Current snapshot model has no section content area
- 현재 `SnapshotSection`은 `id`, `type`, `title`, `order`, `visibility`, `target`, `state`, `blocks`만 가진다.
- section-level `content` 또는 editable field container가 없다.
- 따라서 `News Flow 제목`, `News Flow 시간 정보`를 section 속성으로 올리려면 snapshot schema, assembler, API example, FE parser를 함께 바꿔야 한다.

### 4.2 Runtime execution unit is the chart block
- `ChartSeriesBlockDataStrategy`는 차트 조회 결과를 `timestamps`, `series`로 조립한다.
- `NEWS_FLOW_CHART`는 여기에 `title`까지 같은 content map에서 함께 내려보낸다.
- classifier도 `NEWS_FLOW_CHART`의 assign field set을 `title`, `aggregationTime`, `timestamps`, `series` 4개로 고정 검증한다.

즉, 현재 시스템은 이미 `차트 헤더 + 차트 데이터`를 하나의 canonical block payload로 본다.

### 4.3 Ownership and override policy are block-centric
- field ownership matrix에서 `title`, `aggregationTime`은 `NEWS_FLOW_CHART` 소유로 정의되어 있다.
- 둘 다 `delta/snapshot -> state -> template default` 우선순위를 가진다.
- 이는 section property가 아니라 block state/content ownership으로 설계된 것이다.

### 4.4 Figma visual grouping is not enough to justify section promotion
- 화면에서 제목/시간이 차트 상단 header처럼 보이더라도, 그것만으로 section property로 승격할 근거는 부족하다.
- section property 승격은 아래 둘 중 하나가 먼저 충족될 때 검토할 만하다.
  - 여러 block이 동일 header를 공유한다
  - section-level state/content contract가 별도로 필요하다

현재는 둘 다 아니다.

## 5. Canonical modeling rule

### 5.1 Section level
- Section code: `NEWS_FLOW`
- 역할: 뉴스 흐름 차트와 관련 뉴스 카드 묶음
- section 자체는 block container 역할을 한다.

### 5.2 Block level
- `NEWS_FLOW_CHART`
  - 책임: 뉴스 흐름 섹션의 chart header와 chart data를 함께 렌더링
  - field:
    - `title`
    - `aggregationTime`
    - `timestamps`
    - `series`
- `NEWS_ITEM`
  - 책임: 관련 뉴스 카드 목록
  - template block 1건이 runtime-expanded snapshot block `1..N`으로 생성된다

### 5.3 Field level
- `NEWS_FLOW_CHART.title`
  - 의미: chart block title
- `NEWS_FLOW_CHART.aggregationTime`
  - 의미: chart aggregation 기준 시간 정보
- `NEWS_FLOW_CHART.timestamps`
  - 의미: chart x-axis 데이터
- `NEWS_FLOW_CHART.series`
  - 의미: chart series 데이터

## 6. Canonical JSON interpretation

### 6.1 Template tree level
```json
{
  "sectionCode": "NEWS_FLOW",
  "blocks": [
    {
      "blockCode": "NEWS_FLOW_CHART",
      "fields": ["title", "aggregationTime", "timestamps", "series"]
    },
    {
      "blockCode": "NEWS_ITEM",
      "repeatable": true
    }
  ]
}
```

### 6.2 Snapshot level
```json
{
  "sections": [
    {
      "id": "sec-NEWS_FLOW",
      "blocks": [
        {
          "id": "blk-NEWS_FLOW_CHART-201",
          "type": "CHART",
          "content": {
            "title": "주요 뉴스 Flow 및 Brent 유가 추이",
            "aggregationTime": "뉴욕시간 전일 20:00부터 당일 17:00까지",
            "timestamps": [],
            "series": []
          }
        },
        {
          "id": "blk-NEWS_ITEM-1001",
          "type": "NEWS_CARD",
          "content": {
            "newsId": "1001",
            "title": "미국 원유 재고 감소",
            "content": ["시장 예상 대비 큰 폭 감소"],
            "timestamp": 1742697600000,
            "createdDate": "2026-03-23",
            "createTime": "10:00",
            "source": "Reuters",
            "originalUrl": null,
            "showInChart": true,
            "trend": "UP"
          }
        }
      ]
    }
  ]
}
```

## 7. Why not split header out of the chart block

### 7.1 Not section property
- 현재 section contract가 header field 저장/편집을 지원하지 않는다.
- 승격 시 API/DTO/assembler/FE 계약 변경이 필요하다.

### 7.2 Not separate `NEWS_FLOW_HEADER` block for now
- `title`, `aggregationTime`은 chart data와 강하게 결합된 의미를 가진다.
- 현재 템플릿/validator/strategy는 `NEWS_FLOW_CHART`를 하나의 assign unit으로 기대한다.
- header block을 분리하면 차트와 header 사이의 ownership, ordering, fallback, state 저장 정책을 새로 정해야 한다.

현재 구조에서 분리 이득보다 계약 변경 비용이 크다.

## 8. Recommendation
향후 커뮤니케이션 문장은 아래처럼 통일한다.

`NEWS_FLOW는 NEWS_FLOW_CHART와 NEWS_ITEM 두 block으로 구성되며, News Flow 제목과 시간 정보는 section 속성이 아니라 NEWS_FLOW_CHART block의 field다.`

## 9. Revisit triggers
아래 조건이 생기면 section property 또는 별도 header block 분리를 다시 검토할 수 있다.

- chart 외 다른 block도 동일 `title`, `aggregationTime` header를 공유해야 한다
- section-level content/state 모델을 snapshot contract에 새로 도입한다
- FE가 chart header를 block이 아니라 section chrome으로만 렌더링해야 하고, 그 계약이 다른 section에도 공통으로 확장된다
