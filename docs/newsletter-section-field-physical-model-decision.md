# Newsletter Section Field Physical Model Decision

- date: `2026-03-23`
- status: `proposed decision`
- scope: newsletter v2 template / snapshot / response truth alignment for `section.fields` and `section.fieldDefs`

## 1. Decision Question

`section.fields` / `section.fieldDefs`를 canonical response contract로 도입한 이후,
아래 중 어떤 방향을 source of truth로 채택할지 결정한다.

- A. section field는 response-only 개념으로 두고 physical storage는 계속 block field seed에만 의존한다.
- B. section field의 존재/정의는 physical model에 반영하고, value/state 저장은 별도 단계로 분리한다.

## 2. Decision

이번 문서는 **B안을 권고**한다.

즉:

- `section`이 어떤 field를 소유하는지는 physical model에 있어야 한다.
- 다만 그 field의 runtime value/state를 곧바로 별도 state table로 분리할 필요는 없다.

정리하면 아래와 같다.

- `section field definition store`: 도입 권고
- `section field state/value store`: 지금 단계에서는 필수 아님

## 3. Why This Decision Is Needed

현재 문서와 코드 기준으로 `section.fields`는 더 이상 임시 응답 decoration이 아니다.
다음 section들은 contract상 section-owned field를 가진다.

- `NEWS_FLOW`
  - `title`
  - `aggregationTime`
- `REFERENCE1~4`
  - `title`
  - `description`
- `FOOTER`
  - `footerContent`

이 값들은 canonical response에서 block-owned field가 아니라
**section-owned field**로 정의되어 있다.

따라서 적어도 아래 2가지는 storage truth에서 추적 가능해야 한다.

1. 어떤 section이 어떤 field를 소유하는가
2. 그 field가 editable / required 인가

이 정보가 물리구조에 없으면 실제 ownership의 정본이
assembler / canonicalizer 규칙으로 밀려 올라가게 된다.

## 4. Problem With The Current Interpretation

기존 문서의 아래 표현은 구분이 모호하다.

- `field-level state 테이블은 도입하지 않는다`
- `물리 저장 스키마 즉시 변경 강제 아님`
- `physical section-level field store를 도입하지 않는다`

이 표현들은 서로 같은 뜻이 아니다.

### 4.1 `state table`을 안 둔다는 뜻

이건 아래를 의미한다.

- issue/revision/publish 단계에서 field value를 별도 row로 저장하지 않는다.
- 현재처럼 snapshot JSON / revision JSON 기반으로 유지할 수 있다.

이 방향은 지금도 합리적이다.

### 4.2 `physical definition store`도 없다는 뜻

이건 아래를 의미한다.

- section이 어떤 field를 가지는지 DB에서 알 수 없다.
- `fieldDefs`의 정본이 block field seed 또는 app code promotion rule이 된다.

이 방향은 additive migration의 임시 단계로는 가능하지만,
장기 storage truth로는 비권고다.

## 5. Recommended Source-of-Truth Split

혼동을 줄이기 위해 source of truth를 3층으로 고정한다.

### 5.1 Response truth

외부 API canonical shape.

- `section.name`
- `section.fields`
- `section.fieldDefs`
- `blocks`

### 5.2 Physical definition truth

template 기준 구조 정본.

여기에는 최소한 아래가 포함되어야 한다.

- section code
- section field key
- field order
- editable
- required
- 필요 시 default/meta reference

### 5.3 Runtime value/state truth

발행 전후의 실제 값 정본.

현재 단계에서는 아래로 유지 가능하다.

- original snapshot JSON
- revision snapshot / delta JSON
- resolver input / template execution spec

즉 “definition은 physical”, “value/state는 JSON 계층 허용”이 이번 권고다.

## 6. Recommended Physical Model Direction

### 6.1 Preferred option: normalized section field definition table

권고 방향:

- `tmd_nwsltrtmpltsctnfld` 신설

예상 책임:

- `tmd_nwsltrtmpltsctn`
  - section identity / order / visibility / target
- `tmd_nwsltrtmpltsctnfld`
  - section-owned field definition
- `tmd_nwsltrtmpltblk`
  - renderer / execution unit
- `tmd_nwsltrtmpltblkfld`
  - block-owned field definition

예상 최소 컬럼:

- `nws_ltr_tmplt_sctn_fld_id`
- `nws_ltr_tmplt_sctn_id`
- `fld_key`
- `fld_nm`
- `fld_ord`
- `is_editable_yn`
- `is_required_yn`
- `fld_meta` or limited metadata JSON

핵심 관계:

- section 1 : N section field definition
- section 1 : N block
- block 1 : N block field definition

### 6.2 Alternative option: section JSON metadata column

차선책:

- `tmd_nwsltrtmpltsctn`에 `sctn_fld_def_json` 같은 JSON column 추가

장점:

- DDL 최소
- rollout 부담 낮음

단점:

- FK / UK / field row 단위 검증 불가
- block field와 같은 수준의 관리 일관성 떨어짐
- dictionary / naming / migration review가 약해짐

따라서 가능하면 정규화 테이블이 더 낫다.

## 7. What Does Not Need To Change Now

이번 결정은 “지금 당장 새 state table을 도입하라”는 뜻이 아니다.

당장 바꾸지 않아도 되는 것:

- original snapshot JSON 저장 방식
- revision snapshot / delta JSON 저장 방식
- assembler promotion 기반 additive migration
- legacy snapshot canonicalize read

즉 현재 app-only migration은 계속 허용할 수 있다.
다만 이를 최종 storage truth로 굳히지는 않는다는 뜻이다.

## 8. Migration Recommendation

### Phase 1. Current rollout 유지

- response contract는 `section.fields` / `fieldDefs` 기준으로 유지
- legacy block seed + promotion은 한시적으로 허용

### Phase 2. Physical definition 도입

- section field definition store 추가
- ownership matrix와 template model을 physical truth에 맞춤

### Phase 3. Drift cleanup

- `REFERENCE_HEADER` 제거
- `NEWS_FLOW_CHART.title/aggregationTime`의 block-owned 정의 제거
- `FOOTER_NOTICE.footerContent`의 block-owned 정의 제거

### Phase 4. Optional state normalization

필요 시에만 별도 검토:

- field별 independent override
- field별 visibility/order
- field별 감사/조회 인덱스 요구

## 9. Explicit Position

아래 문장은 이번 문서에서 명시적으로 채택한다.

- `section field state table을 지금 즉시 도입하지 않아도 된다.`
- `section field physical definition은 후속 단계에서 도입하는 것이 맞다.`
- `section에 field가 존재하는지 여부는 장기적으로 물리구조에 있어야 한다.`
- `현재 block field seed 기반 promotion은 migration bridge이지 최종 storage truth가 아니다.`

## 10. Final Recommendation

최종 권고는 아래 한 줄로 정리한다.

**section field의 existence/definition은 physical model에 두고, value/state는 현재 단계에서 JSON 계층을 유지한다.**
