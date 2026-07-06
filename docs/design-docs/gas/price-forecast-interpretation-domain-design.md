# 가격전망해석 도메인 설계

## Document Control

- status: Draft
- canonical for: 가격전망해석의 비즈니스 규칙, 공유 소유권, 상태 전이, 동시성 및 초기화 의미
- owner: BE
- source documents:
  - `AGENTS.md`
  - `docs/feature-specs/mi-scenario-workbench-backend-feature-spec.md`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenarioGroup.java`
  - `src/main/java/com/sk/domain/shared/entity/master/tmm/MiScenario.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioDesignServiceImpl.java`
  - `src/main/java/com/sk/domain/mi/service/nextgen/design/ScenarioDesignCallbackService.java`
  - `docs/design-docs/gas/mi-scenario-model-execution-workflow-design.md`
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioGroupServiceImpl.java`
  - `src/main/java/com/sk/domain/shared/repository/master/tmd/custom/CustomScenarioCaseRepositoryImpl.java`
- depends on:
  - 후속 가격전망해석 feature spec
  - 후속 가격전망해석 ERD/DDL draft
- supersedes: 없음
- last synced: 2026-06-30
- open questions:
  - Agent callback 인증과 `generationId` 형식은 API 계약 단계에서 확정한다.
  - 그룹 결과 revision을 물리적으로 hash 한 개로 저장할지, 시나리오별 `runSeq` 집합으로 저장할지는 ERD review에서 확정한다.

## 1. Feature summary

### 1.1 Request classification

- brand-new domain concept
- 기존 `ScenarioGroup` 및 `Scenario`를 대상으로 하는 1:1/1:N 확장 모델

### 1.2 Feature goal

시나리오 그룹의 수급모델 실행이 완료되어 가격 차트를 구성할 수 있을 때, 사용자가 가격전망해석 생성을 요청한다. Backend는 차트의 source run 집합과 가격 데이터를 고정해 Agent에 전달하고 그룹 내 각 시나리오의 해석을 생성한다. 같은 수급모델 결과로는 다시 생성할 수 없으며, 하나 이상의 수급모델 결과가 재실행 완료되어 source revision이 바뀐 뒤에만 새 생성을 허용한다.

생성이 완료되면 권한 있는 사용자가 공유 최신본을 조회·수정할 수 있고, 초기화로 현재 편집본을 마지막 Agent 생성 원본으로 되돌릴 수 있다.

### 1.3 Scope

- 포함:
  - 그룹 단위 Agent 생성 상태
  - 시나리오별 Agent 생성 원본과 현재 편집본
  - 서로 다른 사용자의 동시 수정 충돌 방지
  - 그룹 전체 초기화
  - Agent 생성 중 조회 의미와 쓰기 차단
  - 수급모델 완료/차트 준비 여부에 따른 생성 가능 조건
  - 수급모델 결과 revision과 Agent 생성 결과의 연결
- 제외:
  - 사용자별 사본
  - 작성자 기반 소유권
  - 수정 히스토리 및 이전 버전 복구
  - 세부 endpoint, DTO 및 HTTP error contract
  - 물리 테이블/컬럼명과 DBA rollout SQL

## 2. Existing structure reviewed

### 2.1 Domain and persistence

- `MiScenarioGroup`은 그룹 상태와 그룹 메타데이터를 보관한다.
- `MiScenario`는 `scenarioGroupId`에 종속된 시나리오이며 제목, 내러티브, 확률, 표시 순서를 가진다.
- 현재 엔티티에는 가격전망해석 원본/편집본과 해석 생성 상태가 없다.
- `BaseEntity`는 생성·수정 일시, 기술 감사 사용자, `del_yn`을 제공한다.
- Scenario MI 수급모델 실행은 시나리오별 `runId`/`runSeq`를 가지며 lifecycle은 `RUNNING | COMPLETED | FAILED`다.
- 가격 차트는 각 시나리오의 최신 사용 가능한 `COMPLETED` 가격 결과를 읽는다. 재실행 실패 또는 진행 중에도 직전 완료 가격으로 fallback할 수 있으므로 “차트가 보인다”와 “새 수급모델 결과가 생겼다”는 같은 조건이 아니다.

### 2.2 Async Agent integration

- `ScenarioDesignServiceImpl`은 Agent 호출과 timeout/error 변환 패턴을 제공한다.
- `ScenarioDesignCallbackService`는 callback 저장 후 그룹 단위 SSE 신호를 발행한다.
- `ScenarioDesignSseRegistry`는 같은 `scenarioGroupId`를 구독하는 여러 연결에 완료 신호를 전달한다.
- 기존 `ScenarioDesignAgentApi`의 기본 `read-timeout`은 60초다. 약 80초가 걸리는 새 작업을 같은 동기 호출로 처리하면 timeout될 수 있다.

### 2.3 Nearby policy

- Scenario Group 조회는 작성자 소유권이 아니라 endpoint permission rule로 접근을 통제한다.
- 따라서 가격전망해석도 작성자별 데이터가 아니라 동일 권한 범위에서 공유하는 group read/write model로 둔다.

## 3. Proposed domain model

### 3.1 Aggregate root: `PriceForecastInterpretationSet`

시나리오 그룹 하나의 가격전망해석 생성 상태와 동시성 버전을 소유하는 aggregate root다.

- owner: `ScenarioGroup`
- cardinality: `ScenarioGroup 1 : 0..1 PriceForecastInterpretationSet`
- responsibility:
  - 그룹 단위 생성 상태 관리
  - active Agent 작업 식별
  - 그룹 전체의 낙관적 잠금 버전 관리
  - 수정·초기화·중복 생성 허용 여부 판단
  - 마지막 성공 생성의 수급모델 source revision 관리
  - 생성 접수 시 고정한 active source revision 관리

### 3.2 Child: `PriceForecastInterpretation`

그룹에 속한 각 시나리오의 해석 원본과 현재 편집본을 보관한다.

- owner: `PriceForecastInterpretationSet`
- target: 정확히 하나의 `Scenario`
- cardinality:
  - `PriceForecastInterpretationSet 1 : N PriceForecastInterpretation`
  - `Scenario 1 : 0..1 PriceForecastInterpretation`
- responsibility:
  - `generatedContent`: 마지막으로 성공한 Agent 생성 원본
  - `currentContent`: 현재 화면에 표시되고 사용자 수정이 반영되는 공유 최신본
  - `sourceRunSeq`: 해당 해석을 만들 때 사용한 시나리오 수급모델 완료 실행 식별자

### 3.3 Why original and current content are separate

초기화는 Agent를 다시 호출하지 않고 아래 대입을 수행하는 기능이다.

```text
currentContent = generatedContent
```

`generatedContent`를 보존하지 않으면 히스토리를 두지 않는 요구에서 초기화 기준점을 재현할 수 없다. `generatedContent`는 변경 이력이 아니라 현재 유효한 Agent 기준점 하나이므로 히스토리 미보관 정책과 충돌하지 않는다.

## 4. Table design draft

> 이 절은 논리 모델 초안이다. 물리 테이블명, 컬럼명/타입, 제약명과 rollout SQL은 후속 ERD/DDL draft가 정본이다.

### 4.1 Header candidate: `PriceForecastInterpretationSet`

| Logical field | Required | Purpose |
|---|---:|---|
| `scenarioGroupId` | Y | PK이자 `ScenarioGroup` FK. 그룹당 최대 1건 보장 |
| `generationStatus` | Y | `NOT_GENERATED`, `GENERATING`, `READY`, `FAILED` |
| `activeGenerationId` | 조건부 | `GENERATING`일 때 현재 Agent 작업 식별자 |
| `activeSourceResultRevision` | 조건부 | 현재 Agent 요청이 사용 중인 그룹 수급모델 결과 revision |
| `generatedSourceResultRevision` | 조건부 | 마지막 성공 가격전망해석이 기반한 그룹 수급모델 결과 revision |
| `version` | Y | 그룹 공유 최신본의 낙관적 잠금 토큰. 상태/내용 변경 때 증가 |
| `lastGeneratedAt` | 조건부 | 마지막 Agent 생성 성공 일시 |
| `latestFailureReason` | N | 최신 생성 실패 사유. 이력 누적이 아닌 현재 운영 상태 |
| base fields | Y | 저장소 표준 생성/수정 일시·기술 감사 사용자·삭제 여부 |

Constraints and indexes:

- PK/unique: `scenarioGroupId`
- `GENERATING`이면 `activeGenerationId`가 반드시 존재한다.
- `GENERATING`이면 `activeSourceResultRevision`이 반드시 존재한다.
- `READY`이면 그룹의 모든 대상 시나리오에 해석 child가 존재한다.
- 같은 `generatedSourceResultRevision`으로 성공 생성은 최대 1회다.
- 별도 작성자/소유자 컬럼은 두지 않는다.
- `version`은 0 이상이며 aggregate 변경 때 단조 증가한다.

### 4.2 Child candidate: `PriceForecastInterpretation`

| Logical field | Required | Purpose |
|---|---:|---|
| `scenarioId` | Y | PK 또는 unique, `Scenario` FK. 시나리오당 최대 1건 |
| `scenarioGroupId` | Y | header FK 및 그룹 일괄 조회 기준 |
| `generatedContent` | Y | 마지막 성공 Agent 생성 원본 |
| `currentContent` | Y | 현재 공유 편집본 |
| `sourceRunSeq` | Y | 해당 시나리오 해석 생성에 사용한 완료 수급모델 실행순번 |
| base fields | Y | 저장소 표준 생성/수정 일시·기술 감사 사용자·삭제 여부 |

Constraints and indexes:

- unique: `scenarioId`
- index: 그룹 조회용 `scenarioGroupId`
- child의 `scenarioId`가 실제로 같은 `scenarioGroupId`에 속하는지 callback 반영 전에 검증한다.
- `sourceRunSeq`가 해당 `scenarioId`의 `COMPLETED` 수급모델 가격 결과를 가리키는지 검증한다.
- 생성 성공 전에는 child를 만들지 않는다. 따라서 원본/현재 내용의 nullable 상태를 정상 흐름으로 사용하지 않는다.
- 긴 해석문 저장 타입과 길이는 Agent 최대 응답 크기를 확인한 뒤 ERD review에서 확정한다.

### 4.3 Audit interpretation

“작성자를 고려하지 않는다”는 말은 작성자를 비즈니스 소유권, 조회 필터, 수정 권한, 화면 표시 기준으로 사용하지 않는다는 뜻이다. 저장소 공통 `BaseEntity`의 기술 감사 필드는 유지할 수 있지만 가격전망해석의 소유자를 의미하지 않는다.

## 5. Relationship design

```text
ScenarioGroup
  1
  |
  0..1 PriceForecastInterpretationSet
          1
          |
          N PriceForecastInterpretation
              |
              1 Scenario
```

- 해석 세트는 그룹 없이 존재할 수 없다.
- 시나리오별 해석은 해석 세트와 시나리오 없이 존재할 수 없다.
- 각 해석의 `sourceRunSeq`는 해당 시나리오의 Agent 입력 가격을 만든 완료 수급모델 실행을 논리적으로 가리킨다.
- 시나리오 그룹이 비활성/삭제되면 해석도 독립적으로 노출하지 않는다.
- v1에서는 사용자가 해석 하나를 독립 삭제하는 기능을 제공하지 않는다.

## 6. Business rules

### 6.1 핵심 정책

1. 가격전망해석은 `scenarioGroupId` 기준 공유 데이터다.
2. 권한 있는 모든 사용자는 동일한 최신본을 조회하고 수정한다.
3. 사용자별 사본, 작성자 소유권, 수정 히스토리는 관리하지 않는다.
4. Agent 원본 `generatedContent`와 현재 편집본 `currentContent`를 분리한다.
5. 그룹 단위 `version`으로 생성·수정·초기화 간 충돌을 제어한다.
6. 가격전망해석 생성은 그룹 내 대상 시나리오의 수급모델 실행이 모두 완료되고 가격 차트를 구성할 수 있을 때만 허용한다.
7. 같은 수급모델 결과 revision으로 성공 생성은 한 번만 허용한다.
8. 하나 이상의 수급모델 결과가 재실행 완료되어 source revision이 바뀌면 새 생성을 허용한다.
9. Agent 실패 재시도는 성공 중복 생성이 아니므로 수급모델 재실행 없이 허용한다.
10. 수급모델 재실행 중, 이전 해석이 새 결과와 불일치하는 상태, Agent 생성 중에는 기존 해석을 읽기 전용으로만 제공한다.
11. Agent 부분 결과는 노출하지 않으며 성공 시 전체 시나리오 결과를 한 transaction으로 반영한다.
12. 초기화는 Agent 재호출 없이 그룹 전체 `currentContent`를 같은 source revision의 `generatedContent`로 복원한다.

### 6.2 Ownership and access

- 비즈니스 소유자는 개별 사용자가 아니라 `ScenarioGroup`이다.
- 접근 가능 여부는 기존 Scenario Group endpoint의 역할/회사 조건과 같은 permission policy가 결정한다.
- 마지막 수정자나 최초 생성자는 read/write 허용 여부에 영향을 주지 않는다.
- 서로 다른 테넌트 또는 허용되지 않은 역할까지 공유한다는 의미는 아니다.

### 6.3 Source result revision and generation eligibility

#### Source result revision

가격전망해석 생성 기준은 “차트가 현재 보이는지”가 아니라 Agent 입력으로 선택된 완료 수급모델 결과 집합이다.

- 그룹 가격 차트에 포함되는 비삭제 전체 scenario를 `scenarioId` 순으로 정렬한다. 판정 상태(`pending/realized/discarded`)는 생성 대상 제외 조건으로 사용하지 않는다.
- 각 scenario의 Agent 입력 가격을 만든 최신 사용 가능 `COMPLETED runSeq`를 선택한다.
- 정렬된 `(scenarioId, runSeq)` 집합을 하나의 `sourceResultRevision`으로 해석한다.
- 실제 저장 방식은 hash 또는 run mapping일 수 있지만, 서로 다른 집합이 같은 revision으로 판정되면 안 된다.
- 생성 접수 시 이 집합과 가격 데이터를 고정하며, Agent 처리 중 최신 조회 결과로 다시 해석하지 않는다.

현재 그래프는 새 실행이 `RUNNING` 또는 `FAILED`여도 직전 완료 가격을 표시할 수 있다. 따라서 단순히 가격 row 존재 여부만 확인해서는 안 되며, 그룹에 active `RUNNING` 실행이 없는지도 함께 확인해야 한다.

#### Generation eligibility

새 생성은 아래 조건을 모두 만족해야 한다.

1. 그룹 가격 차트에 포함되는 비삭제 시나리오가 하나 이상 존재한다.
2. 모든 대상 시나리오에 Agent 입력으로 사용할 `COMPLETED` 가격 결과가 존재한다.
3. 그룹 내 수급모델 `RUNNING` 실행이 없다.
4. 가격전망해석 Agent 작업이 `GENERATING`이 아니다.
5. 현재 `sourceResultRevision`이 마지막 성공 `generatedSourceResultRevision`과 다르거나, 아직 성공 생성 이력이 없다.

같은 source revision에서 Agent 작업이 실패한 경우에는 5번의 성공 중복 규칙을 적용하지 않고 재시도를 허용한다.

### 6.4 Generation lifecycle

`generationStatus`와 source freshness를 분리한다.

- generation status: `NOT_GENERATED | GENERATING | READY | FAILED`
- source freshness:
  - `NONE`: 성공 생성 없음
  - `CURRENT`: `generatedSourceResultRevision = currentSourceResultRevision`
  - `OUTDATED`: 두 revision이 다름

| Current condition | Operation/event | Result | Rule |
|---|---|---|---|
| 수급모델 결과 없음/일부 없음 | 생성 요청 | 거절 | 모든 대상 시나리오의 완료 가격 필요 |
| 수급모델 `RUNNING` 존재 | 생성 요청 | 거절 | 완료 후 새 source revision 확정까지 대기 |
| 성공 생성 없음 + 생성 조건 충족 | 생성 요청 | `GENERATING` | generation/source snapshot 기록, `version + 1` |
| 같은 source revision으로 `READY/CURRENT` | 생성 요청 | 거절 | 수급모델 재실행 완료 전 중복 생성 불가 |
| 수급모델 재실행 완료로 revision 변경 | 상태 조회 | `READY/OUTDATED` | 이전 해석은 읽기 전용, 새 생성 가능 |
| `READY/OUTDATED` + 생성 조건 충족 | 생성 요청 | `GENERATING` | 새 source snapshot으로 Agent 호출 |
| `GENERATING` | 중복 생성 요청 | 거절 | active generation 1건만 허용 |
| `GENERATING` | 성공 callback | `READY/CURRENT` | 전체 원본/현재본/source run을 원자 교체 |
| `GENERATING` | 실패 callback/timeout | `FAILED` | 부분 결과 미반영, 같은 source로 재시도 가능 |
| `READY/CURRENT` | 사용자 수정 | `READY/CURRENT` | `currentContent`만 변경, `version + 1` |
| `READY/CURRENT` | 초기화 | `READY/CURRENT` | 전체 current를 generated로 복원, `version + 1` |

수급모델 재실행으로 새 source revision이 생긴 뒤 새 Agent 생성이 성공하면 기존 `generatedContent`와 `currentContent`는 새 결과로 교체된다. 히스토리를 유지하지 않으므로 이전 수동 수정은 복구할 수 없다.

### 6.5 Generation result validation

성공 callback은 저장 전에 다음을 모두 만족해야 한다.

- `activeGenerationId`가 현재 header의 값과 일치한다.
- callback/응답의 source revision이 `activeSourceResultRevision`과 일치한다.
- 그룹 가격 차트에 포함되는 비삭제 scenario ID 집합과 callback scenario ID 집합이 정확히 일치한다.
- 누락, 중복, 다른 그룹의 scenario ID가 없다.
- scenario별 `sourceRunSeq`가 생성 접수 때 고정한 source run mapping과 일치한다.
- 각 scenario의 해석 내용이 계약상 필수 조건과 길이 제한을 만족한다.

하나라도 실패하면 어떤 child도 반영하지 않고 `READY`로 전이하지 않는다. 늦게 도착한 이전 callback과 동일 callback 재전송은 현재 완료본을 덮어쓰지 않는다. 상세 idempotency 응답은 API spec에서 정의한다.

### 6.6 Read and write policy by process state

“조회 허용”은 Agent 작업의 부분 결과를 조회한다는 뜻이 아니다.

- 최초 생성 중:
  - `generationStatus = GENERATING`
  - 해석 목록 없음
  - 화면은 생성 중 상태를 표시하고 polling 또는 SSE 완료 신호를 기다린다.
- 수급모델 재실행 중:
  - 직전 완료 가격과 기존 해석을 조회할 수 있지만 이전 완료본임을 표시한다.
  - 기존 해석은 읽기 전용이며 생성·수정·초기화를 차단한다.
- 수급모델 재실행 완료 후, 새 가격전망해석 생성 전:
  - 기존 해석은 `OUTDATED` 읽기 전용으로 반환한다.
  - 새 source revision으로 가격전망해석 생성만 허용한다.
- 기존 완료본이 있는 Agent 생성 중:
  - `generationStatus = GENERATING`
  - 직전 완료된 `currentContent`만 읽기 전용으로 반환
  - 새 Agent 결과는 전체 검증·commit 전까지 노출하지 않음

생성 상태의 source of truth는 가격전망해석 DB header이고, freshness의 source of truth는 header의 생성 source revision과 현재 완료 수급모델 run 집합의 비교 결과다. SSE는 재조회 트리거일 뿐 정본이 아니다.

### 6.7 Concurrent update policy

그룹 전체 초기화와 시나리오별 수정의 경합을 단순하고 안전하게 막기 위해 v1은 그룹 단위 낙관적 잠금을 사용한다.

1. 조회 응답에 header의 `version`을 포함한다.
2. 수정 요청은 조회 당시 `expectedVersion`을 전달한다.
3. server version이 같고 상태가 `READY/CURRENT`이면 수정하고 `version + 1` 한다.
4. version이 다르거나 `GENERATING`, `OUTDATED`, 수급모델 `RUNNING`이면 저장하지 않고 충돌/invalid state로 처리한다.
5. 충돌한 사용자의 입력을 server가 자동 병합하거나 마지막 쓰기로 덮어쓰지 않는다.

Example:

```text
A, B가 version=3 조회
A 저장 -> 성공, version=4
B 저장 -> 충돌, server data 변경 없음
```

그룹 버전은 서로 다른 시나리오를 동시에 수정해도 충돌을 만들 수 있다. v1은 일부 false conflict보다 조용한 데이터 유실 방지와 구현 단순성을 우선한다.

### 6.8 Reset policy

- 초기화 단위는 시나리오 그룹 전체다.
- 요청은 조회 당시 `expectedVersion`을 전달한다.
- server version이 같고 상태가 `READY/CURRENT`일 때만 수행한다.
- 한 transaction에서 모든 child에 `currentContent = generatedContent`를 적용하고 header의 `version + 1`을 수행한다.
- 하나라도 Agent 원본이 없거나 대상 시나리오 집합이 불완전하면 전체 rollback한다.
- `GENERATING`, `NOT_GENERATED`, `OUTDATED`, 수급모델 `RUNNING` 상태에서는 초기화할 수 없다.
- `FAILED`에서도 유효한 완료 원본이 없는 v1 흐름은 초기화할 수 없다.
- 초기화는 편집 히스토리를 남기지 않으므로 실행 전 “수정 내용이 삭제되며 복구할 수 없음”을 사용자에게 명확히 알려야 한다.

### 6.9 Delete behavior

- 해석의 독립 삭제 API는 제공하지 않는다.
- Scenario Group 삭제/비활성 처리 시 같은 그룹의 해석을 조회 대상에서 제외한다.
- 물리 cascade 또는 soft-delete 동기화 방식은 ERD review에서 확정한다.

## 7. Extension considerations

현재 모델을 바꾸지 않고 다음을 후속 확장할 수 있다.

- Agent model/prompt version 기록
- 시나리오 단위 수정 version으로 false conflict 축소
- 수정 이력 또는 승인 workflow

v1 요구에 없는 history table, 사용자별 draft, edit lease/long lock은 추가하지 않는다.

## 8. Data access recommendation by responsibility

- JPA repository:
  - header/child 단건 및 그룹 조회
  - callback 결과의 aggregate 저장
- Scenario case/result query:
  - 그룹 내 active scenario별 최신 사용 가능 완료 `runSeq`와 가격 결과 조회
  - active `RUNNING` 실행 존재 여부 조회
  - 정렬된 source run mapping으로 `sourceResultRevision` 구성
- conditional update 또는 header row lock:
  - `expectedVersion` 비교
  - 생성 시작 single-flight claim
  - 현재 source revision과 마지막 성공 source revision 비교
  - 초기화 transaction의 그룹 직렬화
- bulk update:
  - 그룹 전체 초기화 시 child current content 복원
- Redis/SSE:
  - 완료 알림 최적화에만 사용
  - 생성 상태와 해석 내용의 source of truth로 사용하지 않음

구현은 header를 먼저 잠그거나 조건부 갱신한 뒤 child를 변경하고 version을 증가시켜야 한다. 애플리케이션 인스턴스 메모리 락만으로 동시성을 보장하지 않는다.

## 9. API/DTO impact

후속 `backend-feature-spec`은 최소 다음 계약을 정의해야 한다.

- 가격전망해석 조회 및 생성 상태 조회
- 현재 source freshness, 생성 가능 여부, 생성 차단 사유
- Agent 생성 접수
- 사용자 수정
- 그룹 전체 초기화
- Agent 성공/실패 callback
- polling 또는 기존 SSE 재사용 경계
- `version`/`expectedVersion` 전달 방식
- 수급모델 결과 미완료, 수급모델 실행 중, 같은 source 중복 생성, 생성 중, version 충돌, 원본 없음, 잘못된 callback의 오류 코드와 HTTP status
- 최초 생성 중 빈 결과, 수급모델 재실행/OUTDATED, 완료본 존재 Agent 생성 중 read-only 결과의 응답 shape
- Agent 요청에 전달할 source run mapping과 가격 데이터 snapshot 계약

본 문서는 endpoint path나 DTO field serialization을 정본으로 정의하지 않는다.

## 10. SQL / DBA implications

- 신규 header 1개와 child 1개 구조가 기본 후보다.
- 그룹당 header 1건, 시나리오당 child 1건을 DB unique로 강제해야 한다.
- Agent callback은 전체 child upsert/insert와 header terminal 전이를 한 transaction으로 수행해야 한다.
- header는 마지막 성공 source revision과 active generation source revision을 구분해 보관해야 한다.
- child 또는 별도 mapping에는 scenario별 source `runSeq`를 보관해 동일 source 중복 생성과 결과 추적을 지원해야 한다.
- 수동 DBA 배포를 전제로 create/backfill/검증/rollback 순서를 DDL draft에 제공해야 한다.
- 기존 Scenario Group 데이터에 대한 header 생성 시점(lazy 생성 또는 배치 backfill)을 결정해야 한다.
- 해석 내용의 최대 크기와 문자셋, row size 영향, index 비대상 여부를 검토해야 한다.
- `generationStatus`, active job single-flight, version compare-and-set의 DB-level 보장 방식을 ERD review에서 검증해야 한다.
- source run mapping을 JSON/hash/child column 중 어디에 저장할지와 hash collision 회피 방식을 ERD review에서 검증해야 한다.

## 11. Risks / open questions

### 11.1 Risks

- 동기 Agent 호출은 약 80초가 걸리므로 기존 60초 read timeout을 그대로 사용할 수 없다.
- HTTP request thread를 80초 점유하면 timeout, connection 종료, 중복 재시도 위험이 커진다.
- callback에 stable `generationId`가 없으면 늦거나 중복된 결과가 최신 데이터를 덮어쓸 수 있다.
- 사용자 수정과 초기화를 last-write-wins로 처리하면 수정 내용이 조용히 유실된다.
- callback scenario coverage를 검증하지 않으면 일부 시나리오만 초기화 가능한 불완전 baseline이 생긴다.
- 차트 표시 여부만 생성 조건으로 사용하면 재실행 중 직전 완료 가격 fallback을 새 결과로 잘못 판단할 수 있다.
- 그룹 source revision을 단일 scenario `runId`로 표현하면 다른 scenario만 재실행된 경우를 놓친다.
- 새 source 결과가 생긴 뒤 이전 해석을 계속 편집하도록 허용하면 현재 차트와 해석 기준이 불일치한다.

### 11.2 Open questions

- Agent가 비동기 접수 + callback 계약을 제공하는가, 아니면 backend가 동기 Agent 호출을 별도 worker로 감싸야 하는가?
- Agent가 보내는 `generationId`의 생성 주체와 멱등성 범위는 무엇인가?
- source result revision을 hash로 저장할지 scenario별 run mapping으로 저장할지?
- 해석 본문의 최대 길이와 마크업 허용 범위는 무엇인가?

앞의 두 질문은 구현 전 반드시 API 계약에서 해결해야 한다. source revision의 물리 저장 방식은 ERD review에서 정하되 논리 비교 규칙은 본 문서를 따른다. 본문 형식은 기본안 plain text로 계약 초안을 작성할 수 있다.

## 12. Exit criteria / handoff

### 12.1 Implementation readiness

- domain model readiness: ready after minor cleanup
- API contract drafting: ready
- implementation: blocked until Agent async/callback contract and ERD/DDL are confirmed

### 12.2 `backend-feature-spec` handoff

- read/generate/update/reset/callback API contract
- generation status, source freshness, `canGenerate`, blocked reason 응답 shape
- expected version conflict contract
- Agent timeout/callback/idempotency contract
- 생성 요청의 source run/가격 snapshot과 같은 source 중복 생성 차단 계약
- permission rules and FE polling/SSE behavior

### 12.3 `erd-change-review` handoff

- header/child 분리의 물리 테이블명과 표준 용어 조합
- PK/FK/unique/index
- 그룹 version compare-and-set 방식
- active generation single-flight의 DB 최후 방어선
- 마지막 성공/active source revision 및 scenario별 source `runSeq` 저장 방식
- 같은 source revision 성공 중복 생성 차단과 새 완료 run 감지 query
- callback 전체 반영 transaction과 reset bulk update
- 기존 데이터 rollout/backfill/rollback

## 13. Dictionary updates

다음 표준 단어를 `docs/conventions/db-term-dictionary-sheet2.csv` 끝에 append했다.

| standard_term | standard_english_term | term_type | Reason |
|---|---|---|---|
| 전망 | `FCST` / `FORECAST` | 단일단어 | 가격전망해석 도메인과 후속 물리명 정합화 |
| 해석 | `INTRP` / `INTERPRETATION` | 단일단어 | Agent 생성 해석 원본/현재 내용의 의미 구분 |

기존 `가격(PRC)`, `시나리오(SCNR)`, `그룹(GRP)`, `내용(CNTT)`, `원본(ORG)`, `버전(VER)`, `상태(STAT)`, `생성(CRT)` 용어를 재사용한다.

## 14. Document control / source-of-truth notes

- 이 문서는 가격전망해석의 비즈니스 의미, 공유 소유권, lifecycle, 동시성, 초기화 규칙의 정본이다.
- endpoint/request/response/error contract는 후속 `docs/feature-specs/*` 문서를 참조하며 여기서 재정의하지 않는다.
- 물리 schema와 DBA rollout은 후속 ERD/DDL draft를 참조하며 여기서 확정하지 않는다.
- 외부 Agent 문서나 협업 도구는 설계 근거일 뿐 정본이 아니다. 확정 내용은 feature spec/DDL에 정규화해야 한다.
- 실행 결과와 검증 기록은 `docs/work-notes/2026/2026-06-30-price-forecast-interpretation-design.md`가 담당한다.
- 저장소 지침이 요구하는 `docs/conventions/source-of-truth-governance.md`는 현재 작업 트리에 존재하지 않는다. 해당 문서가 복구되면 본 Document Control과 정본 배정을 재검토해야 한다.
