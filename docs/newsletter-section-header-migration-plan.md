# Newsletter Section Fields Migration Plan

## 1. 목적
이 문서는 `section.fields` 공통 계약을 실제 newsletter v2 구조에 반영할 때 필요한 변경 범위와 단계별 계획을 정리한다.

기준 계약:
- [newsletter-section-header-contract-draft.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-section-header-contract-draft.md)

## 2. 변경 목표

### 2.1 목표 구조
- `NEWS_FLOW.title`, `NEWS_FLOW.aggregationTime`를 `NEWS_FLOW_CHART` block content에서 `section.fields`로 승격
- `REFERENCE1~4.title`, `REFERENCE1~4.description`를 `REFERENCE_HEADER` block content에서 `section.fields`로 승격하고 `REFERENCE_HEADER` block은 제거
- `FOOTER.footerContent`를 `FOOTER_NOTICE` block content에서 `section.fields`로 승격

### 2.2 비목표
- `COVER`를 section fields 구조로 바꾸지 않음
- 모든 section에 일괄적으로 `fields`를 강제하지 않음
- 첫 단계에서 template physical schema까지 반드시 바꾸지는 않음

## 3. 영향 범위

### 3.1 Snapshot DTO
대상 파일:
- [SnapshotSection.java](/Users/hoduhomil/source/mip/backend/src/main/java/com/sk/domain/mi/dto/snapshot/SnapshotSection.java)

변경:
- `fields` 필드 추가
- `fieldDefs` 필드 추가 또는 별도 DTO `SnapshotSectionFieldDef` 도입

예상 영향:
- snapshot JSON schema 변경
- preview/original/revision 공통 snapshot reader 영향

### 3.2 Snapshot Assembler
대상 파일:
- [SnapshotAssembler.java](/Users/hoduhomil/source/mip/backend/src/main/java/com/sk/domain/mi/service/newsletter/SnapshotAssembler.java)

변경:
- section별 fields 승격 규칙 추가
- `NEWS_FLOW`: template 기본값에서 `title`, `aggregationTime`를 읽어 `section.fields`에 채우는 조립 단계 추가
- `REFERENCE_HEADER` block에서 `title`, `description`를 읽어 `section.fields`에 채우는 조립 단계 추가 후 `REFERENCE_HEADER` block 제거
- `FOOTER_NOTICE` block에서 `footerContent`를 읽어 `section.fields`에 채우는 조립 단계 추가

선택지:
- `dual-write assemble`
  - 한동안 block content와 section fields 둘 다 채움
- `fields-first assemble`
  - section fields만 채우고 block content에서는 제거

권장:
- 초기에는 `dual-write assemble`

### 3.3 TemplateTree / Template metadata
대상 파일:
- [TemplateTree.java](/Users/hoduhomil/source/mip/backend/src/main/java/com/sk/domain/mi/dto/internal/TemplateTree.java)

1차 단계:
- 변경 없이도 가능
- assemble 시 기존 block field를 section fields로 promote 가능

2차 단계:
- `SectionNode`에 section field metadata/spec 추가 검토

### 3.4 Ownership matrix / design docs
대상 문서:
- [newsletter-template-v2-field-ownership-matrix.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-template-v2-field-ownership-matrix.md)
- [newsletter-v2-confluence-guide.md](/Users/hoduhomil/source/mip/backend/docs/newsletter-v2-confluence-guide.md)

변경:
- `NEWS_FLOW_CHART.title`, `NEWS_FLOW_CHART.aggregationTime`를
  `NEWS_FLOW.fields.title`, `NEWS_FLOW.fields.aggregationTime`로 재표기
- `REFERENCE_HEADER.title`, `REFERENCE_HEADER.description`를
  `REFERENCE{N}.fields.title`, `REFERENCE{N}.fields.description`로 재표기
- `FOOTER_NOTICE.footerContent`를
  `FOOTER.fields.footerContent`로 재표기

주의:
- execution truth와 response truth를 섞지 않도록 migration 단계 표기 필요

### 3.5 OpenAPI examples
대상 문서:
- [newsletter-v2-openapi.yaml](/Users/hoduhomil/source/mip/backend/docs/newsletter-v2-openapi.yaml)

변경:
- `snapshot.sections[*].fields` 예시 추가
- 기존 block content의 중복 field 예시는 단계적으로 제거

### 3.6 FE parser / renderer
백엔드 밖 영향이지만 반드시 같이 묶어야 하는 범위:
- section renderer가 `section.fields`를 읽도록 변경
- `NEWS_FLOW`는 `fields.title/aggregationTime`를 헤더 영역에 렌더링
- `FOOTER`는 `fields.footerContent`를 본문 영역에 렌더링
- `REFERENCE`는 `fields.title/description`를 section 상단에 렌더링

## 4. 단계별 권장 순서

### Phase 1. Contract first
목표:
- 공통 `section.fields` 계약 확정

작업:
- 설계 문서 승인
- section별 적용 범위 확정

### Phase 2. Additive DTO extension
목표:
- snapshot schema에 `fields`를 additive하게 추가

작업:
- `SnapshotSection.fields` 추가
- `SnapshotSection.fieldDefs` 또는 별도 DTO 추가
- 기존 block content는 제거하지 않음

### Phase 3. Assembler dual-write
목표:
- assemble 시 section fields와 block content를 함께 제공

작업:
- `NEWS_FLOW`
  - `NEWS_FLOW_CHART.content.title` -> `section.fields.title`
  - `NEWS_FLOW_CHART.content.aggregationTime` -> `section.fields.aggregationTime`
- `REFERENCE1~4`
  - `REFERENCE_HEADER.content.title` -> `section.fields.title`
  - `REFERENCE_HEADER.content.description` -> `section.fields.description`
- `FOOTER`
  - `FOOTER_NOTICE.content.footerContent` -> `section.fields.footerContent`

### Phase 4. FE adoption
목표:
- FE가 section fields를 정본으로 사용

작업:
- FE parser 전환
- 표현 계층에서 `uiGroups.header` 같은 grouping 처리

### Phase 5. Cleanup
목표:
- block 중복 payload 제거

작업:
- `NEWS_FLOW_CHART.content.title` 제거 검토
- `NEWS_FLOW_CHART.content.aggregationTime` 제거 검토
- `REFERENCE_HEADER` block 제거
- `FOOTER_NOTICE.content.footerContent` 제거 검토

## 5. Section별 migration 세부안

### 5.1 NEWS_FLOW

현재:
```json
{
  "section": {
    "name": "News Flow",
    "blocks": [
      {
        "blockCode": "NEWS_FLOW_CHART",
        "content": {
          "title": "...",
          "aggregationTime": "...",
          "timestamps": [],
          "series": []
        }
      }
    ]
  }
}
```

목표:
```json
{
  "section": {
    "name": "News Flow",
    "fields": {
      "title": "...",
      "aggregationTime": "..."
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
        "blockCode": "NEWS_FLOW_CHART",
        "content": {
          "timestamps": [],
          "series": []
        }
      }
    ]
  }
}
```

### 5.2 REFERENCE1~4

현재:
```json
{
  "section": {
    "blocks": [
      {
        "blockCode": "REFERENCE_CHART",
        "content": {}
      }
    ]
  }
}
```

목표:
```json
{
  "section": {
    "fields": {
      "title": "...",
      "description": "..."
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
        "blockCode": "REFERENCE_CHART",
        "content": {}
      },
      {
        "blockCode": "REFERENCE_CONTENT",
        "content": {}
      }
    ]
  }
}
```

### 5.3 FOOTER

현재:
```json
{
  "section": {
    "blocks": [
      {
        "blockCode": "FOOTER_NOTICE",
        "content": {
          "showLogo": true,
          "footerContent": "..."
        }
      }
    ]
  }
}
```

목표:
```json
{
  "section": {
    "fields": {
      "footerContent": "..."
    },
    "fieldDefs": {
      "footerContent": {
        "editable": true,
        "required": false
      }
    },
    "blocks": [
      {
        "blockCode": "FOOTER_NOTICE",
        "content": {
          "showLogo": true
        }
      }
    ]
  }
}
```

## 6. 리스크

### 6.1 Dual truth risk
이행 기간 동안 아래가 동시에 존재할 수 있다.
- `section.fields.title`
- `NEWS_FLOW_CHART.content.title`

정본 우선순위를 명시하지 않으면 drift가 생긴다.

권장:
- additive 단계부터 `section.fields`를 logical SoT로 선언
- block content는 compatibility payload로만 취급

### 6.2 FE partial adoption risk
일부 화면은 `section.fields`, 일부 화면은 기존 block field를 읽으면 렌더링 차이가 생길 수 있다.

권장:
- 화면 단위가 아니라 parser 공통 계층에서 먼저 전환

### 6.3 Template truth ambiguity
초기에는 저장 truth가 여전히 block field 기반일 수 있다.

권장:
- 문서에 `storage truth`와 `response truth`를 분리해 표기

### 6.4 버전 분기
버전 분기는 사용하지 않는다.
`section.fields`/`fieldDefs`는 additive 추가이며, `@JsonInclude(NON_NULL)`로 역호환이 보장된다.
기존 snapshot에 `fields`가 없으면 null로 처리되므로 별도 버전 분기가 불필요하다.

## 7. 구현 난이도 평가

| 항목 | 난이도 | 비고 |
|---|---|---|
| `SnapshotSection.fields` DTO 추가 | 낮음 | additive |
| `SnapshotSection.fieldDefs` DTO 추가 | 낮음 | additive |
| `SnapshotAssembler` 승격 로직 추가 | 중간 | section별 규칙 필요 |
| OpenAPI example 갱신 | 낮음 | 문서 작업 |
| FE parser 전환 | 중간 | 공통 parser 여부에 따라 차이 |
| template section-level field 정식 도입 | 높음 | 저장 모델 확장 필요 |

## 8. 권장 결정

### 바로 할 것
- `section.fields` contract를 공통 구조로 고정
- `NEWS_FLOW`, `FOOTER`, `REFERENCE1~4`를 response contract 기준으로 함께 고정

### 다음에 할 것
- 저장 truth에서 `REFERENCE_HEADER` 제거 반영

### 나중에 할 것
- template/ownership에도 section-level field를 정식 도입할지 결정

## 9. 한 줄 결론
가장 안전한 전환 방식은
`contract additive 도입 -> assembler dual-write -> FE 전환 -> cleanup`
순서다.
