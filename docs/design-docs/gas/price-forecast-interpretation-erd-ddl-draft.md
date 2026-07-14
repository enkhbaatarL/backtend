# 가격전망해석 Soft-delete Unique ERD/DDL Draft

## 1. Document Control

- status: Approved
- canonical for: `tmm_mi_scenario_prc_interp_m`의 active-row unique, 반복 soft-delete 호환성, migration/rollback/verify SQL
- owner: BE / DBA
- source documents:
  - 테이블 담당자 확정 `CREATE TABLE tmm_mi_scenario_prc_interp_m` DDL (2026-07-14)
  - MySQL 8.0 Reference Manual `CREATE INDEX Statement`: https://dev.mysql.com/doc/refman/8.0/en/create-index.html
  - `docs/design-docs/gas/price-forecast-interpretation-domain-design.md`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenarioPriceInterp.java`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- supersedes:
  - `(scenario_id, del_yn)` 일반 복합 unique를 전제로 한 가격전망해석 soft-delete schema 검토
- last synced: 2026-07-14
- open questions:
  - 없음
- rollout status:
  - functional unique DB 적용 완료 (사용자 확인, 2026-07-14)

## 2. Current structure reviewed

- table: `tmm_mi_scenario_prc_interp_m`
- PK: `prc_interp_id`
- FK: `scenario_id -> tmm_mi_scenario_m.scenario_id`
- soft delete: `del_yn`, active=`N`, deleted=`Y`
- application entity: `MiScenarioPriceInterp`
- application rule: Scenario당 active 가격전망해석 최대 1건, deleted row 다건 보존
- 기존 JPA metadata: `@UniqueConstraint(name = "..._uq01", columnNames = {"scenario_id", "del_yn"})`

## 3. Change classification

- `constraint/index change`
- table/column/PK/FK/nullability/default 변경 없음
- 일반 복합 unique `(scenario_id, del_yn)`를 active row만 유일하게 만드는 functional unique로 변경

## 4. Canonical references

- 가격전망해석 lifecycle 및 수급모델 재실행 soft-delete 규칙:
  - `docs/design-docs/gas/price-forecast-interpretation-domain-design.md`
- API 계약(API shape 변경 없음):
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
- schema와 rollout SQL:
  - 본 문서

## 5. Fixed schema decisions

확정된 unique는 다음과 같다.

```sql
constraint tmm_mi_scenario_prc_interp_m_uq01
    unique (
        scenario_id,
        ((case when (`del_yn` = _utf8mb4'N') then _utf8mb4'N' end))
    )
```

표현식 결과와 unique key 의미는 다음과 같다.

| `del_yn` | functional key part | unique key | 허용 범위 |
|---|---|---|---|
| `N` | `N` | `(scenario_id, 'N')` | Scenario당 최대 1건 |
| `Y` | `NULL` | `(scenario_id, NULL)` | Scenario당 여러 건 |

MySQL 8.0.13 이상은 functional key part와 functional key가 포함된 `UNIQUE`를 지원한다. MySQL unique index는 `NULL`을 여러 건 허용하므로 반복 soft delete와 active row 1건 제한을 함께 만족한다.

## 6. Proposed schema change

### Fresh create canonical fragment

테이블 신규 생성 시 테이블 담당자가 확정한 아래 unique fragment를 사용한다.

```sql
constraint tmm_mi_scenario_prc_interp_m_uq01
    unique (scenario_id, ((case when (`del_yn` = _utf8mb4'N') then _utf8mb4'N' end)))
```

### Existing table migration

기존 DB에 `(scenario_id, del_yn)` unique가 적용되어 있을 때만 아래 변경을 수행한다. 이미 functional unique면 재실행하지 않는다.

```sql
ALTER TABLE tmm_mi_scenario_prc_interp_m
    DROP INDEX tmm_mi_scenario_prc_interp_m_uq01,
    ADD CONSTRAINT tmm_mi_scenario_prc_interp_m_uq01
        UNIQUE (
            scenario_id,
            ((CASE WHEN del_yn = _utf8mb4'N' THEN _utf8mb4'N' END))
        );
```

## 7. ERD review

- cardinality는 `MiScenario 1 : 0..1 active MiScenarioPriceInterp`, 물리적으로는 deleted history를 포함해 `1:N`이다.
- PK/FK와 delete/status 의미는 바뀌지 않는다.
- active 1건 invariant를 애플리케이션 선조회만이 아니라 DB unique가 최종 방어한다.
- deleted row를 보존하므로 `org_cntt`, `cur_cntt`, `src_run_id`, `rvsn_no` 감사/추적 데이터가 유지된다.
- multi-tenant 신규 컬럼이나 tenant ownership 변경은 없다.

## 8. Code impact review

### JPA

- JPA `@UniqueConstraint(columnNames = {"scenario_id", "del_yn"})`는 functional key expression을 표현하지 못하며 실제 DDL과 의미가 다르다.
- `MiScenarioPriceInterp`의 해당 `@UniqueConstraint`와 “일반 복합 unique” 설명은 실제 DDL과 불일치하므로 별도 schema-mapping cleanup에서 제거하고, functional unique는 본 DDL 문서가 source of truth임을 주석으로 남긴다. 자동 soft-delete 실행 연계 slice에는 포함하지 않는다.
- `ddl-auto=validate`는 유지한다. 스키마 자동 생성/변경에 JPA annotation을 사용하지 않는다.

### Repository / service

- active 존재 조회는 `scenario_id`와 `del_yn='N'` 조건을 사용한다.
- final soft delete는 active entity에 `softDelete()`를 호출한다.
- active row 중복 insert가 unique 위반이면 동시성 충돌로 변환해야 한다.
- MyBatis mapper, QueryDSL projection, API DTO의 물리 schema shape 변경은 없다.

## 9. Rollout / compatibility review

- 기존 row backfill은 필요 없다. functional key는 기존 `del_yn`에서 계산된다.
- 기존 애플리케이션은 새 unique에서도 호환된다.
- soft-delete 애플리케이션 구현은 functional unique가 적용된 뒤 배포한다.
- functional unique가 미적용된 다른 환경에 새 애플리케이션을 먼저 배포하면 같은 Scenario의 두 번째 soft delete에서 legacy unique 충돌이 발생할 수 있다.
- functional unique 적용 후 rollback은 deleted row가 Scenario당 2건 이상 생성되기 전까지만 단순하다.
- rollout result: functional unique DB 적용 완료. Codex가 직접 실행한 DDL은 아니며 사용자 확인을 실행 기록으로 사용한다.

## 10. DDL draft structure recommendation

### Pre-check

```sql
SELECT VERSION();

SHOW CREATE TABLE tmm_mi_scenario_prc_interp_m;

SELECT scenario_id, COUNT(*) AS active_count
FROM tmm_mi_scenario_prc_interp_m
WHERE del_yn = 'N'
GROUP BY scenario_id
HAVING COUNT(*) > 1;
-- expected: 0 rows

SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, EXPRESSION, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tmm_mi_scenario_prc_interp_m'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

### Apply

- 신규 table이면 테이블 담당자 확정 `CREATE TABLE` DDL을 사용한다.
- 기존 table이면 §6의 단일 `ALTER TABLE`을 사용한다.
- DBA는 적용 방식과 lock/소요시간을 비운영 DB에서 먼저 확인한다.

### Backfill

```sql
-- not needed: functional key는 기존 scenario_id/del_yn으로 즉시 계산된다.
```

### Rollback pre-check

```sql
SELECT scenario_id, COUNT(*) AS deleted_count
FROM tmm_mi_scenario_prc_interp_m
WHERE del_yn = 'Y'
GROUP BY scenario_id
HAVING COUNT(*) > 1;
-- 1건 이상 반환되면 legacy (scenario_id, del_yn) unique로 단순 rollback 불가
```

### Rollback

아래 SQL은 rollback pre-check가 0건이고 soft-delete 애플리케이션을 먼저 rollback한 경우에만 사용한다.

```sql
ALTER TABLE tmm_mi_scenario_prc_interp_m
    DROP INDEX tmm_mi_scenario_prc_interp_m_uq01,
    ADD CONSTRAINT tmm_mi_scenario_prc_interp_m_uq01
        UNIQUE (scenario_id, del_yn);
```

deleted row가 이미 다건이면 보존/정리 정책을 별도로 승인받아야 하며 임의 물리 삭제하지 않는다.

### Verify

```sql
SHOW CREATE TABLE tmm_mi_scenario_prc_interp_m;

SHOW INDEX FROM tmm_mi_scenario_prc_interp_m;

SELECT scenario_id, COUNT(*) AS active_count
FROM tmm_mi_scenario_prc_interp_m
WHERE del_yn = 'N'
GROUP BY scenario_id
HAVING COUNT(*) > 1;
-- expected: 0 rows

EXPLAIN
SELECT prc_interp_id
FROM tmm_mi_scenario_prc_interp_m
WHERE scenario_id = :scenarioId
  AND del_yn = 'N';
-- expected: full table scan(type=ALL) 회피, intended index 또는 scenario_id 선두 index 사용
```

## 11. Recommendation

- functional unique 방향 승인
- 모델 실행 soft-delete 구현의 schema blocker 해제
- 모델 실행 연계 구현 진행
- JPA annotation cleanup은 별도 schema-mapping cleanup으로 추적

## 12. Required SQL / DBA action

1. 완료: functional unique DB 적용.
2. 권장: `SHOW CREATE TABLE`과 `SHOW INDEX`로 적용 결과를 배포 증적에 보관.
3. 권장: active duplicate 0건과 `EXPLAIN` index 사용 확인.
4. 애플리케이션 배포 허용.

## 13. Risks / open questions

- MySQL 8.0.13 미만이면 functional key part를 사용할 수 없다.
- constraint 이름 `_uq01`은 현재 명명 규칙의 `_ux01`과 다르지만, DB에 적용된 기존 이름을 유지하는 승인된 예외로 기록한다.
- deleted history가 장기간 누적되면 Scenario별 조회량과 index 크기를 모니터링해야 한다.
- active row가 없는 상태의 가격전망해석 생성과 모델 재실행 동시성은 functional unique만으로 상호 배제되지 않는다. 공통 Scenario lock 또는 검증된 DB lock 규약은 가격전망해석 도메인과 별도로 맞춘다.

## 14. Normalization assessment

| Normal form | 결과 |
|---|---|
| 1NF | PASS. 반복/배열 컬럼 추가 없음 |
| 2NF | PASS. 단일 surrogate PK이며 부분 종속 없음 |
| 3NF | PASS. non-key 컬럼 간 신규 이행 종속 없음 |
| BCNF | PASS. functional key는 저장 속성이 아닌 DB 파생 index key이며 정규형을 바꾸지 않음 |

## 15. Index strategy recommendation

- `scenario_id`가 선두라 Scenario 단위 조회와 FK 지원에 유리하다.
- 저선택도 `del_yn` 단독 index는 추가하지 않는다.
- active lookup의 실제 index 사용은 `EXPLAIN`으로 확인한다.
- deleted row 규모가 커지기 전에는 별도 covering index를 추가하지 않는다.
- write 시 unique index 유지 비용은 active 1건 invariant를 DB에서 보장하는 대가로 수용 가능하다.

## 16. Query optimization notes

- options 조회는 `exists`/단건 조회로 제한하고 전체 content를 로드하지 않는다.
- final mutation은 Scenario 단위 active row 1건 조회이므로 pagination/aggregate/partitioning이 필요 없다.
- functional expression을 직접 검색 조건으로 사용할 경우 DDL과 동일한 expression이어야 index 매칭이 가능하다.

## 17. Dictionary updates

- 신규 table/column/token이 없으므로 dictionary row 추가 없음.
- 기존 표준 용어 `PRC`, `SCNR`, `DEL`, `YN`, `ID`를 재사용한다.
- `_uq01`/`_ux01`은 용어 사전이 아니라 DB object naming rule 검토 대상이다.

## 18. Implementation readiness

- schema decision: `ready`
- DBA rollout: `complete` — functional unique DB 적용 완료(사용자 확인)
- model execution integration implementation: `ready`
- deployment gate: satisfied
- naming: DB에 적용된 `_uq01`을 승인된 기존 이름으로 유지
