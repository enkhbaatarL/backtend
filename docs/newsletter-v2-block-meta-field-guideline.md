# Newsletter v2 Block Meta / Block Field 가이드

## 문서 목적
- 본 문서는 Newsletter v2 템플릿 구조에서 `Block Meta`와 `Block Field`의 구분 기준을 정리한 운영 가이드다.
- 대상 독자: 프론트엔드, 기획, 백엔드.
- 본 문서는 계약 원본(DDL/OpenAPI)을 재정의하지 않으며, 판단 기준과 예시를 공유하기 위한 참조 문서다.

## Contract Sync Checklist
- Living Doc: `docs/newsletter-v2-design.md`
- Template DDL/구조 참조: `docs/newsletter-v2-template-pipeline-design.md`
- Last synced at (KST): `2026-03-16 17:17`
- Sync owner: `BE`

## Source of Truth
- 물리 테이블/컬럼 구조의 원본은 `docs/newsletter-v2-template-pipeline-design.md`를 따른다.
- 본 문서는 아래 현행 구조를 유지하는 전제로 해석 기준만 제공한다.
  - `tmd_nwsltrtmpltblk`: 블록 자체 정의
  - `tmd_nwsltrtmpltblkfld`: 블록 content 내부 필드 정의

## 한 줄 기준
- 블록 자체를 설명하면 `Block Meta`
- 블록 content 안의 데이터 key를 설명하면 `Block Field`

## 1. Block Meta란
- 블록 전체의 정체성, 배치, 동작 방식, 렌더링 옵션을 설명하는 값
- 특정 content key 하나가 아니라 블록 전체에 적용되는 값
- 블록을 로딩한 뒤 스냅샷 조립 단계에서 바로 쓰일 수 있는 값

### 대표 예시
- `blk_cd`
- `blk_type_cd`
- `blk_fmt_cd`
- `blk_nm`
- `blk_ord`
- `vsblty_yn`
- `plot_on_chrt_yn`
- `tgt_cmpny_no`
- `data_src_type_cd`
- `data_src_ref`

### 이렇게 이해하면 된다
- 이 값이 바뀌면 블록 전체의 표현이나 동작이 바뀐다.
- 이 값은 개별 content 항목의 정의가 아니다.

## 2. Block Field란
- 블록 content 안에 어떤 데이터 조각이 들어가는지 정의하는 값
- 각 데이터 조각의 key, 순서, 매핑, 편집 가능 여부, 필수 여부를 설명한다.
- 프론트는 이 정의를 기준으로 content 구조를 이해하고 바인딩한다.

### 대표 예시
- `text`
- `publishDate`
- `issueLabel`
- `timestamps`
- `series.close`
- `items`
- `items.*.title`
- `items.*.content`

### Block Field가 담는 정보
- `fld_key`: content 내부 key
- `fld_nm`: 관리용 이름
- `fld_ord`: 필드 순서
- `col_mapping`: 데이터 매핑 규칙
- `is_editable_yn`: 사용자 편집 가능 여부
- `is_required_yn`: 필수 여부
- `fld_meta`: 필드 단위 세부 옵션

## 3. Field Meta란
- `Block Field`에 종속되는 세부 규칙
- 필드 자체의 추출/표시 규칙을 설명하는 값
- 블록 전체 옵션이 아니라 특정 필드에만 의미가 있는 경우 사용한다.

### 대표 예시
- `maxItems`
- `orderBy`
- `format`

### 예시
- 뉴스 목록 `items` 필드의 `maxItems=6`
- 날짜 필드의 `format=yyyy-MM-dd`

## 4. 판단 질문
새 속성이 생겼을 때 아래 순서로 판단한다.

### Q1. 이 값이 블록 전체에 적용되는가?
- 예: `Block Meta`
- 아니오: 다음 질문으로 이동

### Q2. 이 값이 특정 content key 하나의 정의/매핑/제약인가?
- 예: `Block Field` 또는 `Field Meta`
- 아니오: 다음 질문으로 이동

### Q3. 이 값이 필드 자체의 세부 옵션인가?
- 예: `Field Meta`
- 아니오: `Block Meta` 후보로 재검토

## 5. `plot_on_chrt_yn` 정리
- `plot_on_chrt_yn`은 특정 필드 하나의 정의가 아니다.
- 이 값은 template 계층에서 "이 블록이 plotOnChart 옵션 구조를 지원하는가"를 나타내는 메타 정보다.
- 특정 issue에서 실제로 차트에 변곡점을 표시할지 여부는 issue block state가 가진다.
- 따라서 template 관점에서는 `Block Meta`로 보는 것이 맞고, instance 관점에서는 block state가 최종값을 가진다.

### 왜 Field로 보지 않는가
- `series.close`나 `timestamps` 같은 특정 필드의 정의가 아니기 때문이다.
- 차트 블록 전체 출력 방식에 영향을 주기 때문이다.
- 최종 스냅샷에는 issue block state에서 계산된 block 속성으로 반영된다.

## 6. 기획/프론트에서 참고할 실무 규칙

### 기획
- 블록 전체 표시 정책을 정의하는 요구사항은 `Block Meta`로 본다.
- 블록 content 안의 개별 데이터 항목 요구사항은 `Block Field`로 본다.
- 개별 항목의 건수 제한, 정렬, 포맷은 `Field Meta`로 본다.

### 프론트
- 블록의 타입/형식/노출/렌더링 옵션은 `Block Meta` 기준으로 해석한다.
- 실제 화면 바인딩용 데이터 key는 `Block Field` 기준으로 해석한다.
- 필드의 포맷/반복 개수/정렬 같은 세부 규칙은 `Field Meta` 기준으로 해석한다.

## 7. 예시 분류표

| 속성 | 권장 위치 | 이유 |
|---|---|---|
| 블록 제목 | Block Meta | 블록 자체 이름 |
| 블록 순서 | Block Meta | 블록 배치 정보 |
| 노출 여부 | Block Meta | 블록 전체 표시 제어 |
| 차트 변곡점 표시 지원 여부 | Block Meta | template 차원의 블록 옵션 구조 |
| 대상 고객사 | Block Meta | 블록 적용 범위 |
| `items.*.title` | Block Field | content 내부 데이터 key |
| `series.close` | Block Field | content 내부 데이터 key |
| `maxItems` | Field Meta | 특정 필드의 건수 제한 규칙 |
| `orderBy` | Field Meta | 특정 필드의 조회 정렬 규칙 |
| `format` | Field Meta | 특정 필드의 표시 형식 규칙 |

## 8. 향후 확장 시 원칙
- 현재는 현행 구조를 유지한다.
- 향후 블록 렌더링 옵션이 여러 개로 늘어나면, `Block Meta` 컬럼을 계속 추가할지 또는 별도 `block option` 영역을 둘지 백엔드에서 별도 검토한다.
- 단, 그 전까지는 아래 원칙을 유지한다.
  - 블록 전체 옵션은 `Block Meta`
  - content key 정의는 `Block Field`
  - 필드 세부 규칙은 `Field Meta`

## 9. 이번 기준의 결론
- `plot_on_chrt_yn`은 template의 `Block Meta`로 유지한다.
- 실제 `plotOnChart` 값은 issue block state에서 관리한다.
- 특정 특화형 섹션의 특정 블록 하나만 사용하더라도, 값의 의미가 블록 전체 옵션이면 `Block Field`로 내리지 않는다.
- 희소하게 사용되는 속성인지보다, 의미상 어디에 속하는지가 우선이다.
