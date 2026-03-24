# Newsletter Section Fields Contract Draft

## 1. 목적
이 문서는 newsletter snapshot에서 section-level field를 공통 구조로 도입할 때의 canonical contract 초안이다.

이 초안의 목적:
- `NEWS_FLOW` 같은 섹션에서 section 전체를 설명하는 값을 block 밖으로 올릴 수 있게 한다.
- `FOOTER`처럼 block 사이가 아니라 section 본문에 직접 속하는 값을 표현할 수 있게 한다.
- `REFERENCE_HEADER`처럼 사실상 section 소유 값인 구조를 제거하고 section field로 정리할 수 있게 한다.

## 2. 설계 원칙

### 2.1 `section.name`과 `section.fields.title`은 다르다
- `section.name`
  - 시스템/문서용 section name
  - 예: `News Flow`, `Footer`
- `section.fields.title`
  - section이 직접 소유하는 사용자-facing 값
  - 예: `주요 뉴스 Flow 및 Brent 유가 추이`

키가 분리되어야 contract 충돌을 피할 수 있다.

### 2.2 `fields`는 optional object다
모든 section에 `fields`가 필요한 것은 아니다.

- 필요한 section:
  - `NEWS_FLOW`
  - `REFERENCE1~4`
  - `FOOTER`
- 불필요하거나 후순위인 section:
  - `COVER`
  - `SUMMARY`

### 2.3 `fields`에는 section이 직접 소유하는 값만 둔다
아래처럼 section 전체를 설명하거나 section 본문에 직접 속하는 값만 `fields`에 둔다.

- section name이 아닌 사용자-facing title
- section description
- section aggregation time
- footer body text

반대로 아래는 `fields`에 두지 않는다.

- chart `timestamps`
- chart `series`
- image binary/base64
- news card item list
- table rows

### 2.4 `header`는 데이터 구조가 아니라 표현 그룹이다
`NEWS_FLOW.title`, `NEWS_FLOW.aggregationTime`는 데이터 모델상 `section.fields`에 둔다.
다만 UI에서는 이 둘을 `header 영역`에 렌더링할 수 있다.

즉 `header`는 JSON contract의 최상위 개념이 아니라 `section.fields`의 시각적 그룹이다.

## 3. Canonical JSON shape

```json
{
  "id": "sec-NEWS_FLOW",
  "type": "GENERAL",
  "name": "News Flow",
  "order": 4,
  "visibility": true,
  "target": null,
  "fields": {
    "title": "주요 뉴스 Flow 및 Brent 유가 추이",
    "description": null,
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
  "blocks": []
}
```

## 4. Fields object draft

```json
{
  "title": "string | null",
  "description": "string | null",
  "aggregationTime": "string | null",
  "footerContent": "string | null"
}
```

`fields`는 section마다 필요한 key subset만 가진다.

## 5. FieldDefs object draft

```json
{
  "title": {
    "editable": true,
    "required": true
  },
  "footerContent": {
    "editable": true,
    "required": false
  }
}
```

`fieldDefs`는 field별 계약/검증 메타다.

공통 키:

| key | 의미 |
|---|---|
| `editable` | 사용자 수정 가능 여부 |
| `required` | 필수 입력 여부 |

## 6. 필드별 의미

| field | 의미 | 예시 | 비고 |
|---|---|---|---|
| `title` | section이 직접 소유하는 제목 | `주요 뉴스 Flow 및 Brent 유가 추이` | optional |
| `description` | section 설명/보조 문구 | `WTI: 청색, 좌측 / 달러인덱스: 회색, 우측` | optional |
| `aggregationTime` | section 관찰 범위/시간 메타 | `뉴욕시간 전일 20:00부터 당일 17:00까지` | optional |
| `footerContent` | footer section 본문 | `본 메일은 ...` | optional |

## 7. Section별 적용안

### 7.1 `COVER`
권장:
- `fields` 없음
- 기존 block 유지

이유:
- cover의 핵심 정보는 section 소유 값보다 block/field grouping 성격이 강하다

### 7.2 `NEWS_FLOW`
권장:
- `fields.title`
- `fields.aggregationTime`
- chart block은 data payload만 유지

### 7.3 `REFERENCE1~4`
권장:
- `fields.title`
- `fields.description`
- chart/table block과 content block은 분리 유지

### 7.4 `FOOTER`
권장:
- `fields.footerContent`
- logo는 별도 block field

### 7.5 `SUMMARY`
권장:
- `fields` 없음
- `SUMMARY_BODY` block 유지

## 8. DTO draft

### 8.1 SnapshotSection target shape
현재:

```java
class SnapshotSection {
    String id;
    String type;
    String name;
    Integer order;
    Boolean visibility;
    String target;
    List<SnapshotBlock> blocks;
}
```

목표:

```java
class SnapshotSection {
    String id;
    String type;
    String name;
    Integer order;
    Boolean visibility;
    String target;
    Map<String, Object> fields;
    Map<String, SnapshotSectionFieldDef> fieldDefs;
    List<SnapshotBlock> blocks;
}
```

```java
class SnapshotSectionFieldDef {
    Boolean editable;
    Boolean required;
}
```

## 9. ownership 원칙

### 9.1 section field도 field-level ownership을 가진다
예:
- `NEWS_FLOW.fields.title`
- `NEWS_FLOW.fields.aggregationTime`
- `REFERENCE1.fields.title`
- `REFERENCE1.fields.description`
- `FOOTER.fields.footerContent`

### 9.2 editable/required 여부는 section field마다 별도 관리한다
예:
- `NEWS_FLOW.fields.title`: editable, template 소유
- `NEWS_FLOW.fields.aggregationTime`: editable, template 소유
- `REFERENCE1.fields.description`: editable
- `FOOTER.fields.footerContent`: editable

### 9.3 section field의 기본값은 template이 소유한다
- `NEWS_FLOW.fields.title`: template default `"주요 뉴스 Flow 및 Brent 유가 추이"`
- `NEWS_FLOW.fields.aggregationTime`: template default `"뉴욕시간 전일 20:00부터 당일 17:00까지"`
- snapshot 조립 우선순위: `delta/snapshot -> template default`

즉 section field 계약에서 필요한 메타는 `editable`, `required` 2개면 충분하다.

## 10. 저장/템플릿 관점

### 10.1 확정 정책
section field의 기본값은 template이 소유한다.
- 사용자 수정값은 delta/snapshot 경로로만 저장한다

대상:
- `NEWS_FLOW.fields.title`: template default
- `NEWS_FLOW.fields.aggregationTime`: template default
- `REFERENCE1~4.fields.title`: template default (있는 경우)
- `REFERENCE1~4.fields.description`: template default (있는 경우)
- `FOOTER.fields.footerContent`: template default

### 10.2 assembler 조립 규칙
assemble 시 section fields 값 결정 순서:
1. delta/snapshot 값 (사용자 수정이 있으면)
2. template 기본값 (수정이 없으면)

## 11. 역호환 전략

### 11.1 additive first
초기에는 아래처럼 additive하게 간다.

- `section.fields` 추가
- 기존 block content도 한동안 유지 가능

예:
- `NEWS_FLOW_CHART.content.title` 유지
- 동시에 `section.fields.title`도 채움

### 11.2 cleanup second
FE 전환 완료 후 중복 값 제거

- `NEWS_FLOW_CHART.content.title` 제거
- `NEWS_FLOW_CHART.content.aggregationTime` 제거
- `REFERENCE_HEADER` block 제거

## 12. 권장 적용 순서

1. `section.fields + fieldDefs` 공통 contract 고정
2. `NEWS_FLOW`에 먼저 적용
3. `FOOTER`에 `footerContent` 적용
4. `REFERENCE1~4` fields를 순차 승격
5. OpenAPI example 갱신
6. FE parser 전환
7. 중복 block field 제거

## 13. 한 줄 결론
`section.fields`는 `NEWS_FLOW`, `REFERENCE`, `FOOTER`를 같은 모델로 설명할 수 있는 공통 구조이고,
`header`는 그 fields의 표현 그룹으로만 다루는 것이 더 정확하다.
