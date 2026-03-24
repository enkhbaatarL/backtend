# Newsletter v2 템플릿/파이프라인 구현 프롬프트

- 참조: `docs/newsletter-v2-template-pipeline-design.md`
- 실행 순서: Step 1 → 8 (순차 진행)
- 각 프롬프트는 독립 세션에서 실행 가능하도록 컨텍스트를 포함

> 2026-03-20 current note:
> Step 4~5에 남아 있는 `IssueStateTree`/`IssueStateLoader` 중심 지시는 현재 코드 기준과 맞지 않는다.
> 현재 활성 구현은 issue-state 테이블 없이 `TemplateTree + ResolvedBlockData + publish context` 중심으로 정렬되어 있다.

---

## Step 1. DDL 4테이블 생성 + JPA Entity

```
## 목표
Newsletter v2 템플릿 4테이블의 JPA Entity와 Repository를 생성한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 1 (DDL 확정)
- `CLAUDE.md` — 엔티티/네이밍 컨벤션

## 참조 코드 (컨벤션 기준)
- `src/main/java/com/sk/domain/shared/entity/master/tmd/NewsletterTemplate.java` — v1 엔티티 (참조만, 수정 금지)
- `src/main/java/com/sk/domain/shared/entity/base/BaseEntity.java` — audit 필드 상속

## 작업 내용

### 1. JPA Entity 4개 생성
설계 문서 섹션 1-2의 DDL을 기반으로 엔티티를 생성한다.
위치: `src/main/java/com/sk/domain/shared/entity/master/tmd/`

| 클래스명 | 테이블 |
|---|---|
| `NewsletterTemplateV2` | `tmd_nwsltrtmpltm` |
| `NewsletterTemplateSection` | `tmd_nwsltrtmpltsctn` |
| `NewsletterTemplateBlock` | `tmd_nwsltrtmpltblk` |
| `NewsletterTemplateBlockField` | `tmd_nwsltrtmpltblkfld` |

### 2. 엔티티 작성 규칙
- `CLAUDE.md`의 Entity Pattern을 정확히 따른다 (@Getter, @Entity, @Comment, @NoArgsConstructor(PROTECTED), @Builder)
- `@Data`, `@ToString` 사용 금지
- BaseEntity를 extends하여 crt_usr_logn_id, crt_dtm, mod_usr_logn_id, mod_dtm, del_yn 상속
- `@Comment` 어노테이션에 한글 컬럼 설명 포함
- CHAR(1) Y/N 컬럼은 String 타입 사용 (Boolean 컨버터 미사용, 프로젝트 기존 패턴 준수)
- JSON 컬럼(fld_meta)은 `@Column(columnDefinition = "json")` + String 타입으로 처리

### 3. Enum 2개 생성
위치: `src/main/java/com/sk/domain/shared/enums/newsletter/`

- `SectionTypeCode`: FIXED, GENERAL, SPECIALIZED
- `BlockTypeCode`: TEXT, CHART, TABLE, NEWS_CARD, IMAGE, TITLE

### 4. Repository 4개 생성
위치: `src/main/java/com/sk/domain/shared/repository/master/tmd/`

| Repository | 주요 메서드 |
|---|---|
| `NewsletterTemplateV2Repository` | `findByNewsletterMasterIdAndDelYn(Integer, String)` |
| `NewsletterTemplateSectionRepository` | `findByTemplateV2IdAndDelYnOrderBySctnOrd(Long, String)` |
| `NewsletterTemplateBlockRepository` | `findBySectionIdInAndDelYnOrderByBlkOrd(List<Long>, String)` |
| `NewsletterTemplateBlockFieldRepository` | `findByBlockIdInAndDelYnOrderByFldOrd(List<Long>, String)` |

### 5. DDL SQL 파일 생성
위치: `docs/newsletter-template-v2-ddl-draft.sql`
- 설계 문서의 DDL을 기존 `docs/newsletter-issue-original-ddl-draft.sql` 형식에 맞게 작성
- PRE-CHECK, APPLY, POST-CHECK, ROLLBACK 섹션 포함

## 제약사항
- 기존 v1 엔티티/테이블은 수정하지 않는다
- hibernate ddl-auto: validate 환경이므로 DDL은 수동 적용
```

---

## Step 2. TemplateLoader + TemplateTree VO

```
## 목표
4테이블 트리를 한번에 로드하는 TemplateLoader와 메모리 트리 구조 TemplateTree VO를 구현한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 3-2 B (TemplateLoader)

## 참조 코드
- Step 1에서 생성한 엔티티/레포지토리 4개
- `src/main/java/com/sk/domain/mi/service/MiNewsletterService.java` 328-339라인 — v1의 templatesByOrder 구축 로직 (대체 대상, 수정 금지)

## 작업 내용

### 1. TemplateTree VO 생성
위치: `src/main/java/com/sk/domain/mi/dto/internal/`

```java
// 불변 트리 구조 (record 또는 @Getter + final 필드)
TemplateTree
  ├── template: TemplateNode (id, name, masterId)
  └── sections: List<SectionNode>
        ├── sectionCode, sectionTypeCode, sectionName, sectionOrder, visibilityYn, targetCompanyNo
        └── blocks: List<BlockNode>
              ├── blockCode, blockTypeCode, blockFormatCode, blockName, blockOrder
              ├── visibilityYn, plotOnChartYn, targetCompanyNo
              ├── dataSourceType, dataSourceRef
              └── fields: List<FieldNode>
                    ├── fieldKey, fieldName, fieldOrder
                    ├── colMapping, isEditableYn, isRequiredYn, fieldMeta
```

- `TemplateTree.of(template, sections, blocks, fields)` 정적 팩토리 메서드로 flat 리스트를 트리로 조립
- sections는 sectionOrder 순, blocks는 blockOrder 순으로 정렬 보장

### 2. TemplateLoader 서비스 생성
위치: `src/main/java/com/sk/domain/mi/service/newsletter/`

- 설계 문서의 TemplateLoader 코드를 구현
- `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`
- 4개 Repository를 IN 쿼리로 배치 로딩 (N+1 방지)
- 템플릿 미존재 시 `AppErrorException(ResultCode.Error.NEWSLETTER_NOT_FOUND)` throw

### 3. 단위 테스트
위치: `src/test/java/com/sk/domain/mi/service/newsletter/`

- `TemplateTreeTest`: flat 리스트 → 트리 조립 정합성 검증
- `TemplateLoaderTest`: Mockito 기반 Repository 목킹, 정상/예외 케이스
- Given-When-Then 스타일, @Nested, @DisplayName 사용
```

---

## Step 3. BlockDataStrategy 4개 구현

```
## 목표
블록의 data_src_type_cd에 따라 데이터를 조회하는 4개의 Strategy 구현체를 생성한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 3-2 C (BlockDataResolver)

## 참조 코드 (v1 로직을 Strategy로 이관)
- `MiNewsletterService.java`:
  - Lines 347-364, 514-604: 차트 시리즈 변환 (`convertNewsletterChartsToChartData`, `getDediChartData`)  →  `ChartSeriesBlockDataStrategy`
  - Lines 678-719: 테이블 참조 (`createTableReference`) →  `DynamicQueryBlockDataStrategy`
  - Lines 721-759: 전용/수집 차트 참조 (`createDediChartReference`, `createCltnChartReference`)  →  `ChartSeriesBlockDataStrategy`
  - Lines 400-412: 뉴스 이벤트 매핑  →  `NewsEventBlockDataStrategy`
- `MybatisDynamicQueryService.java`: 동적 쿼리 실행기
- `NewsletterTemplatePreferenceTable.java`: 참조 테이블 설정
- `NewsletterTemplateDedicatedChart.java`: 전용 차트 설정
- `NewsletterTemplateCollectionChart.java`: 수집 차트 설정

## 작업 내용

### 1. Strategy 인터페이스 정의
위치: `src/main/java/com/sk/domain/mi/service/newsletter/strategy/`

```java
public interface BlockDataStrategy {
    String getType(); // "STATIC", "DYNAMIC_QUERY", "CHART_SERIES", "NEWS_EVENT"
    ResolvedBlockData resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist);
}
```

### 2. ResolvedBlockData VO 정의
위치: `src/main/java/com/sk/domain/mi/dto/internal/`

- `Map<String, Object> toContentMap()` — 스냅샷 JSON의 content 객체로 변환

### 3. Strategy 구현체 4개

| 클래스 | @Component("STATIC") 등 | 핵심 로직 |
|---|---|---|
| `StaticBlockDataStrategy` | `STATIC` | 블록명/고정값을 content로 매핑. 별도 DB 조회 없음 |
| `DynamicQueryBlockDataStrategy` | `DYNAMIC_QUERY` | `data_src_ref`에서 pref_table:{id} 파싱 → `NewsletterTemplatePreferenceTable` 조회 → `MybatisDynamicQueryService.getDynamicData()` 실행 → columns/rows 구조 변환. v1 `createTableReference()` 로직 이관 |
| `ChartSeriesBlockDataStrategy` | `CHART_SERIES` | `data_src_ref` prefix로 차트 종류 구분: (1) prefix 없음 → 메인 차트 (NewsletterChartRepository), (2) `dedi:` → 전용 차트 (NewsletterTemplateDedicatedChartRepository), (3) `cltn:` → 수집 차트 (NewsletterCollectionChartRepository). v1의 `convertNewsletterChartsToChartData()`, `getDediChartData()`, `getCollectionChartData()` 로직 이관. 시장시간 계산(`calculateMarketTimes`) 포함 |
| `NewsEventBlockDataStrategy` | `NEWS_EVENT` | `NewsletterEventRepository` 조회 → items 배열 구성. BlockField의 `fld_meta.maxItems`로 건수 제한. v1의 뉴스 이벤트 매핑 로직 이관 |

### 4. BlockDataResolver 서비스 생성
- Spring의 `Map<String, BlockDataStrategy>` 자동 주입 활용 (Bean name 기반)
- 설계 문서의 BlockDataResolver 코드 구현

### 5. 단위 테스트
- Strategy별 단위 테스트 (Mockito 기반)
- BlockDataResolver 통합 테스트 (전체 strategy 조합)
- 차트 데이터 변환 정합성: v1 출력과 v2 출력 비교 검증

## 주의사항
- v1 MiNewsletterService의 기존 코드는 수정하지 않는다 (v2와 병행 가능하게)
- 차트 변환 로직 이관 시 기존 Series, RenderAs 등 DTO/enum 재사용
- 동적 쿼리 실행 시 DataSourceContextHolder 처리 주의
```

---

## Step 4. Legacy Reference: IssueState + SnapshotAssembler 구현

```
## 목표
Legacy prompt reference. 현재 구현 기준 SnapshotAssembler는 `TemplateTree + ResolvedBlockData + publish context`로 동작한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 2 (스냅샷 JSON 스키마), 섹션 3-2 D (SnapshotAssembler)

## 참조 코드
- Step 2: TemplateTree VO
- Step 3: ResolvedBlockData VO
- `NewsletterPublishHistory.java` — snapshot/section 기본값 소스(emailSubject 등)

## 작업 내용

### 1. 스냅샷 DTO 계층 생성
위치: `src/main/java/com/sk/domain/mi/dto/snapshot/`

| 클래스 | 역할 |
|---|---|
| `SnapshotRoot` | 최상위 (templateId, newsletterId, issueNo, publishedAt, meta, sections) |
| `SnapshotMeta` | 메타정보 (emailSubject, subInfoText, coverImageUrl) |
| `SnapshotSection` | 섹션 (id, type, title, order, visibility, target, blocks) |
| `SnapshotBlock` | 블록 (id, type, format, order, visibility, title, plotOnChart, target, editableFields, content) |

- Jackson 직렬화 대상이므로 @Getter + @Setter 또는 @Builder 사용
- content 필드는 `Map<String, Object>` 타입 (블록 타입별 동적 구조)

### 2. Legacy reference: IssueStateTree VO 및 로더 구현
위치: `src/main/java/com/sk/domain/mi/dto/internal/`, `src/main/java/com/sk/domain/mi/service/newsletter/`

- issue section/block state를 표현하는 `IssueStateTree` 또는 동등한 내부 VO 생성
- state row가 없는 경우 template 구조 기준 default state를 조립하는 로더 구현
- `plotOnChart`, `fromDate`, `toDate`, `market`, `maxItems`는 JSON 필드로 유지
- `plotOnChart` 해석 규칙:
  - template의 `plot_on_chrt_yn`은 해당 옵션 구조 지원 여부
  - issue block state의 `plotOnChart`는 최종 instance 값
  - template이 지원하지 않는 block에는 `plotOnChart` state 저장/반영 금지

### 3. SnapshotAssembler 서비스 구현
위치: `src/main/java/com/sk/domain/mi/service/newsletter/`

- 설계 문서의 SnapshotAssembler 코드 구현
- 입력을 `TemplateTree` 단독이 아니라 `TemplateTree + IssueStateTree + ResolvedBlockData`로 변경
- `editableFields` 배열: BlockField 중 is_editable_yn='Y'인 field_key를 수집
- `target` 변환: tgt_cmpny_no(Integer) → 고객사 코드(String) 변환 로직 (추후 매핑 테이블 연동 또는 단순 문자열 변환)
- JSON 직렬화 시 null 필드 제외 (`@JsonInclude(Include.NON_NULL)`)
- section/block의 `order`, `visibility`, `title`, `plotOnChart` 등 instance-level 값은 issue state 기준으로 스냅샷에 반영

### 4. 단위 테스트
- 완전한 TemplateTree + IssueStateTree + ResolvedBlockData 입력 → JSON 출력 검증
- 설계 문서 섹션 2의 예시 JSON 구조와 일치하는지 검증
- editableFields가 정확히 is_editable 필드만 포함하는지 검증
- template이 `plotOnChart`를 지원하지 않으면 state 값이 반영되지 않거나 예외 처리되는지 검증
```

---

## Step 5. NewsletterPublishPipelineService 오케스트레이터

```
## 목표
Legacy prompt reference. 현재 구현 기준 파이프라인은 `TemplateLoader → BlockDataResolver → SnapshotAssembler → 저장` 순서로 동작한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 3-2 A (오케스트레이터)

## 참조 코드
- Step 1: NewsletterIssueOriginal 엔티티 (이미 존재: `tmd_nwsltrorgissm`)
- Step 2: TemplateLoader
- Step 3: BlockDataResolver
- Step 4: SnapshotAssembler
- `src/main/java/com/sk/domain/shared/entity/master/tmd/NewsletterPublishHistory.java`
- `docs/newsletter-issue-original-ddl-draft.sql` — tmd_nwsltrorgissm DDL

## 작업 내용

### 1. NewsletterIssueOriginal 엔티티 확인/보완
- 기존 `tmd_nwsltrorgissm` 엔티티가 없으면 생성
- org_snpsht_json 컬럼: `@Column(columnDefinition = "json")` + String 타입
- pub_stat_cd 등 DDL과 정합성 확인

### 2. NewsletterPublishPipelineService 구현
위치: `src/main/java/com/sk/domain/mi/service/newsletter/`

- 설계 문서의 오케스트레이터 코드 구현
- `@Transactional` 단일 트랜잭션으로 스냅샷 생성 + 저장
- 이미 존재하는 `(nws_ltr_mstr_id, issno)` 조합이면 UPDATE (org_snpsht_json)
- 발행 상태: PUBLISHING → PUBLISHED 전이 포함
- 기존 template 중심 조립 가정을 제거하고 publish context + resolved content를 조합
- issue-state 물리 테이블이 없어도 published read path와 preview path가 동작하도록 유지

### 3. 호출 진입점
- Airflow HTTP 호출용 내부 API 엔드포인트 (또는 기존 발행 배치 훅에 연동)
- 예시: `POST /api/internal/newsletter/publish` (내부 전용, 인증은 시스템 JWT)

### 4. 예외 처리
- 템플릿 미존재 → NEWSLETTER_NOT_FOUND
- 데이터 resolve 실패 → 블록 단위 graceful degradation (해당 블록 content를 에러 메시지로 대체, 전체 파이프라인은 계속 진행)
- JSON 직렬화 실패 → 로깅 + 롤백

### 5. 통합 테스트
- @SpringBootTest 기반 E2E 테스트
- 테스트 데이터 셋업 → executePublish() 호출 → tmd_nwsltrorgissm 레코드 검증
- org_snpsht_json 파싱하여 sections 존재 여부 검증
```

---

## Step 6. v1 템플릿 데이터 → v2 4테이블 마이그레이션 스크립트

```
## 목표
기존 tmd_nws_ltr_tmplt의 v1 템플릿 데이터를 v2 4테이블 구조로 마이그레이션하는 SQL 스크립트를 작성한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 4 (마이그레이션 매핑)

## 참조 코드/데이터
- `src/main/java/com/sk/domain/shared/entity/master/tmd/NewsletterTemplate.java` — v1 엔티티 구조
- `src/main/java/com/sk/domain/mi/service/MiNewsletterService.java` 328-386라인 — elementType 분기 로직

## 작업 내용

### 1. 마이그레이션 SQL 작성
위치: `docs/newsletter-template-v1-to-v2-migration.sql`

형식: 기존 DDL 파일(`docs/newsletter-issue-original-ddl-draft.sql`)의 구조를 따름
- 0) PRE-CHECK: v1 데이터 현황 조회, v2 테이블 존재 확인
- 1) APPLY: INSERT 스크립트
- 2) POST-CHECK: 마이그레이션 결과 검증 쿼리
- 3) ROLLBACK: 되돌리기 스크립트

### 2. 마이그레이션 로직 (설계 문서 섹션 4-2 구현)

Step A: DISTINCT nws_ltr_tmplt_id별 마스터 매핑
Step B: tmd_nwsltrtmpltm INSERT (템플릿 루트)
Step C: FIXED 섹션 4개 INSERT (HEADER, MAIN_CHART, NEWS_EVENTS, AI_SUMMARY)
Step D: elementOrder >= 2인 참조 영역 → GENERAL 섹션 + 블록 INSERT
Step E: SUB_INFO_TXT 존재 시 SUB_INFO 섹션 INSERT
Step F: 블록필드 INSERT (타입별 기본 필드셋)

### 3. is_editable_yn 기본값 매핑
설계 문서 섹션 4-2의 규칙:
- TEXT.text → 'Y'
- TITLE.title → 'Y'
- NEWS_CARD의 items.*.title, items.*.content → 'Y'
- CHART, TABLE 데이터 필드 → 'N'

### 4. data_src_ref 매핑
- 메인 차트: MAIN_CHRT_CODE 값 그대로
- 참조 테이블: `pref_table:{RERNC_TABLE_CD}`
- 전용 차트: `dedi:{DEDI_CHRT_CD}`
- 수집 차트: `cltn:{CLTN_CHRT_CD}`

### 5. 검증 쿼리
- v1 templateId 수 == v2 tmd_nwsltrtmpltm 수
- v1 elementOrder >= 2 수 == v2 GENERAL 섹션 수
- 전체 블록 수 일치 확인
```

---

## Step 7. v2 상세조회 API (org_snpsht_json 기반)

```
## 목표
발행된 뉴스레터 상세를 org_snpsht_json 기반으로 조회하는 v2 API를 구현하고, 기존 getNewsletterDetail을 deprecate한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 전체
- `docs/newsletter-v2-design.md` — API 설계 초안, 확정사항 ④ (웹 조회 정책)

## 참조 코드
- `MiNewsletterService.getNewsletterDetail()` — 기존 v1 상세조회 (deprecate 대상)
- `MiNewsletterController.java` — 기존 엔드포인트

## 작업 내용

### 1. v2 상세조회 서비스
위치: `src/main/java/com/sk/domain/mi/service/newsletter/`

```java
// NewsletterDetailV2Service
public NewsletterDetailV2Res getNewsletterDetail(Integer companyNo, Integer userNo,
                                                  Integer newsletterId, Integer issueNo) {
    // 1. tmd_nwsltrorgissm에서 org_snpsht_json 조회
    // 2. 사용자 리비전 존재 시: delta 적용된 결과 반환 (확정사항 ④)
    // 3. 구독 상태 조회 포함
}
```

### 2. 응답 DTO
위치: `src/main/java/com/sk/domain/mi/dto/response/newsletter/`

- `NewsletterDetailV2Res`: 스냅샷 JSON 구조를 기반으로 한 응답 DTO
- 기존 `NewsletterDetailRes`와 호환성 유지 or 프론트와 협의 후 새 구조
- `source` 필드: ORIGINAL | REVISION

### 3. Controller 엔드포인트
- 기존 `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}` 유지하되 내부에서 v2 서비스 우선 호출
- 또는 별도 v2 엔드포인트 신설 후 프론트 전환 완료 시 v1 제거

### 4. 기존 코드 deprecate 처리
- `MiNewsletterService.getNewsletterDetail()` 메서드에 `@Deprecated` 추가
- 해당 메서드를 직접 호출하는 곳 목록 확인 후 v2로 전환 계획 수립

## 주의사항
- 프론트엔드와 응답 구조 변경 사항 협의 필요
```

---

## Step 8. Phase C 백필 배치 (선택)

```
## 목표
기존 발행분의 org_snpsht_json을 Full Resolve된 v2 스냅샷으로 백필하는 배치 Job을 구현한다.

## 입력 문서
- `docs/newsletter-v2-template-pipeline-design.md` — 섹션 4-3 (백필 전략)

## 전제조건
- Step 6 (v1→v2 템플릿 마이그레이션) 완료
- Step 5 (발행 파이프라인) 정상 가동 확인

## 작업 내용

### 1. 백필 배치 서비스
위치: `src/main/java/com/sk/domain/mi/service/newsletter/`

```java
// NewsletterSnapshotBackfillService
public void backfillAll(Integer batchSize, boolean dryRun) {
    // 1. 백필 대상 원본 이슈 조회 (페이징)
    // 2. 각 레코드의 src_nws_ltr_pub_id로 publishHistory 조회
    // 3. NewsletterPublishPipelineService와 동일한 로직으로 스냅샷 재생성
    // 4. org_snpsht_json UPDATE
    // 5. 실패 건은 로깅 후 SKIP (전체 중단 방지)
}
```

### 2. 에러 핸들링
- 차트 데이터 누락: 해당 블록 content를 `{"error": "데이터 누락", "dataCode": "..."}` 으로 대체
- 동적 쿼리 실패: 해당 블록 content를 빈 테이블로 대체
- 레코드 단위 try-catch로 개별 실패가 전체에 영향 없도록

### 3. 실행 방식
- Admin API 엔드포인트: `POST /api/internal/newsletter/backfill?batchSize=100&dryRun=true`
- 또는 Spring Batch Job으로 구현
- dryRun 모드: 실제 UPDATE 없이 변환 결과만 로깅

### 4. 백필 대상 범위 결정
- 전체 vs 최근 N개월 → 운영 판단 (파라미터로 기간 지정 가능)
- 진행률 로깅: 처리건수/전체건수, 성공/실패/스킵 카운트

## 리스크 (설계 문서 기재)
- 과거 차트 데이터 만료 가능성
- 동적 쿼리 대상 테이블 스키마 변경 가능성
- 권장: 운영 안정화 이후 선택적 실행
```

---

## 실행 가이드

### 의존관계
```
Step 1 (Entity/DDL)
  └─▶ Step 2 (TemplateLoader)
       └─▶ Step 3 (BlockDataStrategy)
            └─▶ Step 4 (SnapshotAssembler)
                 └─▶ Step 5 (Pipeline Orchestrator)
                      └─▶ Step 7 (v2 상세조회 API)

Step 6 (마이그레이션 SQL) — Step 1 이후 언제든 병렬 가능
Step 8 (백필 배치) — Step 5 + Step 6 완료 후
```

### 세션 운영 팁
- 각 Step을 별도 Claude 세션으로 실행
- 프롬프트 시작 시 `docs/newsletter-v2-template-pipeline-design.md`를 반드시 참조시킴
- Step N 완료 후 `git commit`으로 체크포인트 생성
- Step N+1 시작 시 이전 Step의 생성 파일 경로를 참조 코드에 포함
