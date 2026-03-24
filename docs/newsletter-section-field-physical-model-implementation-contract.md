# Newsletter Section Field Physical Model Implementation Contract

- date: `2026-03-23`
- task type: `api-spec`
- status: `draft for implementation review`
- scope: newsletter template v2 section-owned field definition read path 전환과 snapshot canonical contract 고정

## 1. Feature summary

본 문서는 newsletter section field physical model 전환을 위한 **구현 계약**을 고정한다.

핵심 목표는 아래와 같다.

- `tmd_nwsltrtmpltsctnfld`를 section-owned field definition의 primary read truth로 채택한다.
- `TemplateLoader -> TemplateTree -> SnapshotAssembler` 경로에서 더 이상 section-owned field definition을 `tmd_nwsltrtmpltblkfld` promotion에 의존하지 않도록 한다.
- legacy snapshot read도 별도 promotion 없이 그대로 유지하고, template read path와 신규 snapshot assembly path는 section field definition 기준으로 고정한다.
- 이후 `tmd_nwsltrtmpltblkfld`에서 section-owned field row를 제거해도 snapshot canonical contract가 유지되도록 한다.

이번 변경은 외부 endpoint 추가가 아니라 기존 newsletter snapshot 응답의 내부 조립 기준 전환이 중심이다.

## 2. Scope / Non-scope

### Scope

- `TemplateLoader`가 `tmd_nwsltrtmpltsctnfld`를 읽는 방식 확정
- `TemplateTree.SectionNode`의 section field definition 구조 확정
- `SnapshotAssembler`의 `section.fields` / `section.fieldDefs` 조립 기준 확정
- `SnapshotCanonicalizer`의 비개입 원칙 확정
- `REFERENCE_HEADER`, `NEWS_FLOW_CHART`, `FOOTER_NOTICE`의 최종 ownership 확정
- app rollout 순서, cleanup 조건, 테스트 기준 고정

### Non-scope

- section field runtime value/state table 도입
- external API envelope 변경
- section field write API 별도 설계
- `COVER`, `TITLE_SPECIALIZED`, `SUMMARY`, `DATE_ISSUE`의 ownership 재설계
- section field validation 확장(`maxLength`, regex, widget type 등) 도입

## 3. Current contract and storage gap

현재 canonical response 계약은 `section.fields` / `section.fieldDefs`를 사용하지만, 구현은 아직 아래와 같이 drift가 남아 있다.

- `TemplateLoader`는 `tmd_nwsltrtmpltsctn`, `tmd_nwsltrtmpltblk`, `tmd_nwsltrtmpltblkfld`만 읽는다.
- `TemplateTree.SectionNode`는 section-owned field definition을 보관하지 않는다.
- `SnapshotAssembler`는 `NEWS_FLOW_CHART`, `REFERENCE_HEADER`, `FOOTER_NOTICE` block field를 section field로 승격한다.
- `SnapshotCanonicalizer`는 section `name` 정리 외에는 section field 승격을 하지 않는다.

이 상태의 문제는 아래와 같다.

- section field existence/definition truth가 physical model이 아니라 app promotion rule에 묶여 있다.
- `fieldDefs` 생성 기준이 section field definition이 아니라 block field seed다.
- `REFERENCE_HEADER`를 제거하거나 `NEWS_FLOW_CHART` / `FOOTER_NOTICE`의 section-owned block field row를 삭제하면 canonical contract가 loader/assembler 단계에서 깨진다.

## 4. Target implementation contract

이번 전환의 source-of-truth split은 아래로 고정한다.

- definition truth: `tmd_nwsltrtmpltsctnfld`
- renderer-owned field truth: `tmd_nwsltrtmpltblkfld`
- runtime value truth: snapshot/revision JSON

핵심 구현 원칙은 아래와 같다.

- section-owned field definition은 오직 `tmd_nwsltrtmpltsctnfld`에서 읽는다.
- `tmd_nwsltrtmpltblkfld`는 renderer-owned field만 남긴다.
- `SnapshotAssembler`는 section field definition을 직접 읽어 `fieldDefs`를 생성한다.
- `SnapshotAssembler`는 section field value를 block content에서 promotion하지 않는다.
- `SnapshotCanonicalizer`는 section field 승격 fallback을 두지 않는다.

즉, section field canonical shape는 fresh assembly와 canonical snapshot 저장물에서만 보장하고, canonicalizer는 이를 보정하지 않는다.

## 5. Section field read model

### 5.1 TemplateLoader read contract

`TemplateLoader.loadByTemplateId()`는 아래 순서로 읽는다.

1. `tmd_nwsltrtmpltsctn`에서 section row를 `sctn_ord` 기준으로 조회한다.
2. section id 목록으로 `tmd_nwsltrtmpltsctnfld`를 `del_yn='N'`, `fld_ord` 기준으로 조회한다.
3. section id 목록으로 `tmd_nwsltrtmpltblk`를 `blk_ord` 기준으로 조회한다.
4. block id 목록으로 `tmd_nwsltrtmpltblkfld`를 `fld_ord` 기준으로 조회한다.
5. `TemplateTree` 구성 시 section field definition은 section에, renderer-owned field definition은 block에 연결한다.

Data access는 기존 repository 패턴과 동일하게 JPA read를 우선 사용한다.

- `NewsletterTemplateSectionField` entity/repository 신규 추가
- 조회 메서드 예시: `findBySectionIdInAndDelYnOrderByFldOrd(List<Long> sectionIds, String delYn)`

### 5.2 TemplateTree contract

`TemplateTree.SectionNode`는 아래 필드를 추가한다.

```java
List<SectionFieldNode> sectionFields
```

`SectionFieldNode`는 아래 구조를 가진다.

```java
record SectionFieldNode(
    Long id,
    Long sectionId,
    String fieldKey,
    String fieldName,
    Integer fieldOrder,
    String isEditableYn,
    String isRequiredYn,
    String fieldMeta
) {}
```

구조 원칙은 아래와 같다.

- block field와 같은 최소 shape를 유지해 diff를 줄인다.
- `colMapping`은 section field에 없다.
- `fieldMeta`는 raw JSON string으로 유지하되, 이번 전환에서 해석하는 키는 `defaultValue`만 허용한다.
- `fieldMeta.stateScoped`, `fieldMeta.stateMutable`, `fieldMeta.maxLength`는 읽지 않고 문서상 폐기 대상으로 본다.
- `editable`, `required` truth는 `is_editable_yn`, `is_required_yn` 컬럼에서만 읽는다.

### 5.3 section field row 부재 시 정책

정상 운영 기준에서는 target section에 section field row가 없으면 구성 누락으로 간주한다.

- 기본 정책: loader 단계에서 block field 기반 fallback을 하지 않는다.
- 한시 운영 옵션: rollout 안정성을 위해 초기 1개 릴리즈 동안만 제한적 fallback을 둘 수 있다.
- fallback 허용 범위:
  - `NEWS_FLOW` + `NEWS_FLOW_CHART.title`, `aggregationTime`
  - `REFERENCE1~4` + `REFERENCE_HEADER.title`, `description`
  - `FOOTER` + `FOOTER_NOTICE.footerContent`
- fallback 조건:
  - 해당 section에 `tmd_nwsltrtmpltsctnfld` active row가 0건일 때만
  - synthetic `SectionFieldNode` 생성만 허용
  - 새 snapshot assembly에서만 쓰고, cleanup 완료 후 제거

권고 기본값은 `fallback disabled`다. 운영이 불안하면 명시적으로 한시 fallback을 켠다.

## 6. Snapshot assembly contract

### 6.1 Section field definition 조립

`SnapshotAssembler`는 `sectionNode.sectionFields`를 순회하며 `fieldDefs`를 생성한다.

- key order는 `fld_ord`를 따른다.
- `fieldDefs`는 block field가 아니라 section field definition 기준으로만 생성한다.
- 생성 shape는 기존 canonical contract를 유지한다.

```json
"fieldDefs": {
  "title": {
    "editable": true,
    "required": true
  }
}
```

### 6.2 Section field value 조립

`SnapshotAssembler`의 section field value 조립 기준은 아래로 고정한다.

1. section field definition의 `fld_meta.defaultValue`
2. defaultValue가 없으면 `null`

이번 slice에서 `SnapshotAssembler`는 아래를 하지 않는다.

- block content에서 section field value promotion
- section field value/state table 조회
- `data_src_spec_json`를 통한 section field 직접 resolve

즉, fresh snapshot assembly에서 section field 값은 template definition default만 사용한다.
발행 후 runtime 수정값 truth는 계속 snapshot/revision JSON에 남는다.

### 6.3 block content 조립 규칙

- `NEWS_FLOW_CHART.content`는 `timestamps`, `series`만 가진다.
- `FOOTER_NOTICE.content`는 `showLogo`만 가진다.
- `REFERENCE_HEADER`는 신규 assembly 대상 block이 아니다.
- `REFERENCE_CHART`, `REFERENCE_TABLE`, `REFERENCE_CONTENT`, `NEWS_ITEM` 등 renderer block은 기존 resolver path를 유지한다.

### 6.4 `fieldDefs` 생성 기준 변경

이후 구현에서 아래 규칙을 준수한다.

- `section.fieldDefs`는 `TemplateTree.SectionNode.sectionFields`에서만 생성한다.
- `block.fields`는 해당 block의 `editableFields`와 `content` 계약에만 사용한다.
- section-owned field가 block field에도 동시에 정의돼 있더라도, `fieldDefs` 생성 source로 쓰지 않는다.

## 7. Legacy fallback policy

### 7.1 원칙

`SnapshotCanonicalizer`는 section field 관련 fallback을 두지 않는다.

유지 동작은 아래로 제한한다.

- `section.id`에서 `section.name` 복원
- legacy `section.title` 제거

### 7.2 금지 범위

canonicalizer는 아래를 하지 않는다.

- `NEWS_FLOW_CHART`, `REFERENCE_HEADER`, `FOOTER_NOTICE` content를 `section.fields`로 승격
- block 제거 또는 block content key 제거
- section field row 누락 보정

즉, canonicalizer는 naming cleanup layer이지 data-shape bridge가 아니다.

## 8. Block/section ownership matrix

| Section | Final section-owned fields | Block row policy | Block field cleanup policy | Final note |
|---|---|---|---|---|
| `NEWS_FLOW` | `title`, `aggregationTime` | `NEWS_FLOW_CHART` 유지 | `NEWS_FLOW_CHART.title`, `aggregationTime` 제거 | chart block은 renderer unit 유지 |
| `REFERENCE1` | `title`, `description` | `REFERENCE_HEADER` 제거, `REFERENCE_CHART`/`REFERENCE_CONTENT` 유지 | `REFERENCE_HEADER` 전체 제거 | header는 완전 제거 대상 |
| `REFERENCE2` | `title`, `description` | `REFERENCE_HEADER` 제거, `REFERENCE_CHART`/`REFERENCE_CONTENT` 유지 | `REFERENCE_HEADER` 전체 제거 | header는 완전 제거 대상 |
| `REFERENCE3` | `title`, `description` | `REFERENCE_HEADER` 제거, `REFERENCE_TABLE`/`REFERENCE_CONTENT` 유지 | `REFERENCE_HEADER` 전체 제거 | table block은 유지 |
| `REFERENCE4` | `title`, `description` | `REFERENCE_HEADER` 제거, `REFERENCE_CHART`/`REFERENCE_CONTENT` 유지 | `REFERENCE_HEADER` 전체 제거 | header는 완전 제거 대상 |
| `FOOTER` | `footerContent` | `FOOTER_NOTICE` 유지 | `FOOTER_NOTICE.footerContent` 제거 | `showLogo`만 block ownership 유지 |

최종 ownership 결론은 아래와 같다.

- `REFERENCE_HEADER`: block 자체를 제거 대상으로 둔다.
- `NEWS_FLOW_CHART`: block row는 유지하고 section-owned field row만 제거한다.
- `FOOTER_NOTICE`: block row는 유지하고 section-owned field row만 제거한다.

## 9. Rollout contract

권고 rollout 순서는 아래와 같다.

1. DDL 적용
2. `tmd_nwsltrtmpltsctnfld` seed/backfill 적용
3. app 배포
4. app 배포 직후 active template 검증
5. legacy section-owned block field row cleanup
6. `REFERENCE_HEADER` block row cleanup
7. optional loader fallback 제거

세부 조건은 아래와 같다.

### 9.1 app 배포 전

- 대상 section field row가 모두 적재돼 있어야 한다.
- seed 대상:
  - `NEWS_FLOW.title`
  - `NEWS_FLOW.aggregationTime`
  - `REFERENCE1~4.title`
  - `REFERENCE1~4.description`
  - `FOOTER.footerContent`

### 9.2 app 배포 시

- `TemplateLoader`, `TemplateTree`, `SnapshotAssembler`는 section field read path를 primary로 사용한다.
- `SnapshotCanonicalizer`는 naming cleanup만 수행한다.
- loader fallback은 운영 판단에 따라 한시 활성화 가능하나 기본은 비활성이다.

### 9.3 cleanup 조건

아래가 모두 만족되면 cleanup 가능하다.

- active template에 대상 section field row가 100% 존재
- 신규 snapshot assembly 결과에서 section field 값/정의가 block field에 의존하지 않음
- canonicalizer가 신규 assembly 결과의 data shape를 수정하지 않음

cleanup 항목은 아래와 같다.

- `tmd_nwsltrtmpltblkfld`
  - `NEWS_FLOW_CHART.title`
  - `NEWS_FLOW_CHART.aggregationTime`
  - `REFERENCE_HEADER.title`
  - `REFERENCE_HEADER.description`
  - `FOOTER_NOTICE.footerContent`
- `tmd_nwsltrtmpltblk`
  - `REFERENCE_HEADER` row 전체

### 9.4 cleanup 후 상태

- section-owned field definition truth는 `tmd_nwsltrtmpltsctnfld`만 남는다.
- `SnapshotCanonicalizer`는 old persisted snapshot read 호환 용도로만 남는다.
- loader fallback이 있었다면 같은 릴리즈 또는 다음 릴리즈에서 제거한다.

## 10. Test cases

반드시 고정할 테스트는 아래와 같다.

### 10.1 TemplateLoader / TemplateTree

- `tmd_nwsltrtmpltsctnfld` row가 `fld_ord` 순서대로 `SectionNode.sectionFields`에 적재된다.
- target section의 `sectionFields`와 block `fields`가 분리돼 적재된다.
- `fieldMeta`에서 `defaultValue`만 사용 대상이며 `stateScoped`, `stateMutable`, `maxLength`는 무시된다.
- loader fallback 비활성 시 target section row 누락은 empty definition으로 남고 block field를 승격하지 않는다.
- loader fallback 활성 시 target section row 0건일 때만 synthetic section field가 생성된다.

### 10.2 SnapshotAssembler

- `fieldDefs`가 block field가 아니라 section field definition 기준으로 생성된다.
- `NEWS_FLOW` fresh assembly 시 `title`, `aggregationTime`가 section field default로 들어가고 `NEWS_FLOW_CHART.content`에는 존재하지 않는다.
- `REFERENCE1~4` fresh assembly 시 `REFERENCE_HEADER` block이 생성되지 않고 `title`, `description`가 section field로만 존재한다.
- `FOOTER` fresh assembly 시 `footerContent`는 section field에 있고 `FOOTER_NOTICE.content`에는 `showLogo`만 남는다.
- defaultValue가 없는 section field는 `fields[key]=null`로 조립된다.

### 10.3 SnapshotCanonicalizer

- canonicalizer는 `section.id -> section.name` 정리와 `section.title` 제거만 수행한다.
- canonicalizer는 `NEWS_FLOW_CHART`, `REFERENCE_HEADER`, `FOOTER_NOTICE` content를 `section.fields`로 승격하지 않는다.
- canonicalizer는 block 제거 또는 block content key 제거를 하지 않는다.
- section에 이미 canonical `fields`가 있어도 data shape는 그대로 유지된다.

### 10.4 Rollout safety

- cleanup 후 active template snapshot assembly가 동일한 canonical shape를 유지한다.
- cleanup 후 canonical snapshot 저장물은 동일한 shape를 유지하고, canonicalizer는 이를 추가 보정하지 않는다.
- `REFERENCE_HEADER` 제거 후 `REFERENCE1~4`의 block order와 data/content block 조립이 깨지지 않는다.

## 11. Open questions / handoff

### Open questions

- `fld_meta.defaultValue`를 section field definition의 장기 기본값 저장소로 유지할지, 별도 명시 컬럼으로 올릴지는 후속 schema 정리에서 결정이 필요하다.
- loader fallback을 실제 운영에서 켤지 여부는 rollout 직전 seed completeness 점검 결과로 결정하는 것이 안전하다.
- `REFERENCE2.title/description`처럼 기본값이 없는 field의 editor UX null/empty-string 처리 기준은 FE/patch contract와 한 번 더 맞춰야 한다.

### Handoff to implementation

- `spring-api-implementation` 단계에서 `NewsletterTemplateSectionField` entity/repository, `TemplateLoader`, `TemplateTree`, `SnapshotAssembler`, `SnapshotCanonicalizer`를 이 계약 기준으로 수정한다.
- `erd-change-review` 단계에서는 `tmd_nwsltrtmpltsctnfld`의 backfill SQL, `REFERENCE_HEADER` block row 삭제 SQL, cleanup 시점 검증 쿼리를 확정한다.
- 현재 계약은 구현 착수 가능 상태다. 다만 loader fallback 활성화 여부와 `defaultValue` 장기 저장 방식은 릴리즈 직전 운영 판단이 필요하다.
