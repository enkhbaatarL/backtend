# MCP 이벤트 Snapshot(버전 이력) 조회 API

## Document Control

- status: Complete (2026-10-09)
- canonical for: Execution record
- owner: BE
- source documents:
  - 워크스페이스 `process_event_snapshot.md` (2026-10-06 작성, 계약 초안)
  - Notion 「Event Snapshot 저장」 — Platform (MCP) 조회 기능 구현 가이드
- depends on:
  - `docs/feature-specs/mcp-readonly-api-feature-spec.md`
  - `docs/design-docs/gas/nextgen-mi-mcp-api-channel-design.md`
- supersedes: 없음
- last synced: 2026-10-09
- open questions:
  - dev·prod (일반·전용 서버) master DB 의 Snapshot 3개 테이블 적용 여부 — 사용자는 "모두 있다"는 전제로 확정 (D-2). 배포 전 환경별 확인 필요
  - `event_mod_dtm` 일부 행이 +9h 로 관측됨 (아래 Step 0 결과). 사용자 확정에 따라 UTC 로 처리 — 데이터 측 원인은 파이프라인 담당 확인 대상으로 남긴다

## Task Title

Event Snapshot(버전 이력) 조회 MCP API 추가 (`/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/**`)

## Task Type

Feature — 신규 MCP 전용 조회 API (backend 우선, `backend-first-flow`)

## Task Goal

파이프라인이 쌓는 Event 버전 이력(Snapshot 3개 테이블)을 MCP 채널에서 조회 전용으로 제공한다.
소비자는 MCP 서버(`skgas-test-mcp`)와 고객 MCP 게이트웨이이며 frontend 화면은 없다.

## Done Criteria

- P1 6개 endpoint 가 MCP 채널(SK Gas 전용 서버)에 노출되고 OpenAPI description 에 조회 주의사항이 실린다
- 근거 Record·기관 전망은 Snapshot 사본만 조회한다 (현재 연결·원본 현재 값으로 대체하지 않음)
- 기존 MCP endpoint 13건 계약 무변경, MCP 공통 설정 무수정
- 단위 테스트 통과 + 로컬 testdb 에서 API 결과와 DB 대조

## Non-Goals

- Snapshot 저장·수정 (파이프라인 소유), `MCP_USER_EDIT` 경로
- P2 (Record 반영 타임라인, 과거 내용 검색), frontend 화면, DDL 변경

## Current Status

완료. backend `528b5f85`(mcp-update), skgas-test-mcp `87f6191`(feat/mcp-event-snapshot-tools). push·배포는 남음.

## 계약 확정 (Step 1, 2026-10-09)

- D-1 MCP 전용 / D-2 JPA 엔티티 (모든 환경에 테이블이 있다는 전제, 사용자 확정) / D-3 P1 6개 / D-4 원본 Record 정보 첨부
- D-5 Snapshot 관련 시각 컬럼은 모두 UTC 저장으로 본다 (사용자 확정). 응답 KST 변환, 입력 KST → UTC 변환 후 비교
- D-7 갱신 후 상태 / D-8 (신규) Snapshot 3개 테이블 `del_yn = 'N'` 만 조회, `event_del_yn` 은 응답 값으로만
- 브랜치: `mcp-update`

## Step 0 결과 (2026-10-09, 로컬 testdb)

조회 결과 원문은 저장하지 않고 요약만 남긴다.

### 저장소

- backend: `mcp-update` 브랜치, 직전 MCP 인증 작업은 `origin/dev` 에 머지됨 (PR #595). 작업은 사용자 지정에 따라 `mcp-update` 에서 진행.

### B-1·B-2 테이블·컬럼·인덱스

- 3개 테이블 모두 존재.
- 문서(G6) 대비 차이:
  - 3개 테이블 모두 `mod_usr_logn_id`, `mod_dtm`, `del_yn` 이 추가로 있다 → `BaseEntity` 컬럼 구성과 일치 (G6.1 "BaseEntity 상속" 그대로 가능).
  - 시각 컬럼명은 `crt_dtm` 이다. 문서 G5.3·G6.2 인덱스·부록 B 의 `snapshot_crt_dtm`, `snapshot_crt_usr_logn_id` 는 존재하지 않는다.
  - Snapshot 행 자체의 `del_yn`(논리 삭제)과 당시 Event 상태 `event_del_yn` 이 별개 컬럼이다. 문서 G5.3·B-7 의 `del_yn` 은 `event_del_yn` 을 뜻한 것으로 보인다.
- 인덱스는 G6 와 일치 (`idx_tmm_mi_mievent_snapshot_m_01` 은 `(mi_event_id, crt_dtm)`). UK 에 `del_yn` 은 포함되지 않는다.
- `signal_confidence` 기본값 `0.0000`.

### B-3 데이터 분포

- Event 3건, Snapshot 8건 (최대 버전 4), 근거 Record 110행, 기관 전망 65행.
- `snapshot_version_no` NULL 0건 (N3 의 NULL 행은 testdb 에 없음). `previous_mi_event_snapshot_id` NULL 3건 (각 Event 의 첫 버전).
- 트리거는 전부 `RECORD_INGESTION` (N7). 생성자는 `system`, `system_backfill` 두 종류 (N4: 소문자).
- `del_yn='Y'`, `event_del_yn='Y'` 행 없음.
- 테스트용 eventId: 1269 (4개 버전), 1287·1530 (각 2개 버전).

### B-4 Snapshot 의미 (N1, D-7)

- 3개 Event 모두 최신 Snapshot 의 제목·한줄요약·상세분석이 현재 Event 와 같다 → "갱신 후 상태" 저장 (1부와 일치).
- 현재 Event 의 `mod_dtm` 은 이후 일괄 갱신으로 바뀌어 있어 시각으로는 비교할 수 없다.

### B-5 시각 기준 (N5, D-5)

- DB `system_time_zone = UTC`, `time_zone = SYSTEM` → DB 기본값(`CURRENT_TIMESTAMP(6)`)으로 들어가는 `crt_dtm` 은 UTC.
- `record_first_created`, `latest/oldest_record_first_created` 는 기존 코드와 같이 UTC 로 판단된다 (Record 발생 → 수 분 뒤 Snapshot 저장 순서가 UTC 기준으로 맞음).
- **`event_mod_dtm` 은 행마다 기준이 다르다**: Event 생성 직후 Snapshot 은 `crt_dtm` 과 같은 시각(UTC), 갱신 시 Snapshot 은 `crt_dtm` + 9시간(KST)으로 저장됨.
  파이프라인이 Event 갱신 시 `mod_dtm` 을 KST 로 쓰는 것으로 보인다 (원본 `tmm_mi_mievent_m.mod_dtm` 의 문제). 단순 변환으로 맞출 수 없다.
- 기존 이벤트 API(`EventDetailRes`)는 Event 의 `crt_dtm`/`mod_dtm` 을 응답하지 않고 Record 기준 시각만 `utcToKst` 로 변환한다.

### B-6 JSON 형식 (N6)

- `key_bullets_json`: `{"bullets": [...]}` — 현재 Event 와 같음, `JsonUtil.parseWrappedStringList(json, "bullets")` 사용 가능.
- `tags_json`: `{"tags": [...]}` — `parseWrappedStringList(json, "tags")` 로 `List<String>` 응답 가능.
- `changed_fields_json`: 문자열 배열. 빈 배열 `[]` 가능 (첫 버전·backfill). 이전 Snapshot 이 없어도 값이 있을 수 있음 (1부와 일치).

### 확인 불가

- dev·prod master DB 의 테이블 적용 여부는 이 워크스페이스에서 확인할 수 없다 (사용자 확인 요청).

## 문서(process_event_snapshot.md)와 현재 상태의 차이

- MCP 공통 계약이 문서 작성 이후 바뀌었다 (backend PR #595): MCP API 는 Basic Auth 필수, `X-User-Id` 누락·무효는 401 이 아니라 403(`AUTH-007`).
  문서 G3.1·G9·Step 7 의 "없으면 401 AUTH-001" 은 현재 기준과 다르다.
- `skgas-test-mcp` 는 이제 git 저장소다 (Step 10 의 "아직 git 저장소가 아니다" 와 다름).
- 문서가 언급하는 "부록 A (DDL)" 는 문서에 없다. DDL 은 실제 DB(B-2)로 확인했다.

## Key Changes (backend, `src/main/java/com/sk/` 기준)

| 구분 | 파일 |
|---|---|
| 엔티티 (신규) | `domain/shared/entity/master/tmm/MiEventSnapshot.java`, `MiEventSnapshotRecordMap.java`, `MiEventSnapshotInstitutionView.java` — `@Immutable`, `BaseEntity` 상속 |
| 리포지토리 (신규) | `domain/shared/repository/master/tmm/MiEventSnapshotRepository.java`, `MiEventSnapshotRecordMapRepository.java`(native, D-4), `MiEventSnapshotInstitutionViewRepository.java`, `custom/CustomMiEventSnapshot*`, `custom/CustomMiEventSnapshotInstitutionView*`, `projection/EventSnapshot*Row.java` |
| enum (신규) | `domain/shared/enums/nextgen/EventSnapshotUpdateTrigger.java`, `EventSnapshotCompareTarget.java` |
| DTO (신규) | `domain/mi/dto/request/nextgen/snapshot/*Req.java` 4개, `domain/mi/dto/response/nextgen/snapshot/*Res.java` 6개 |
| 서비스 (신규) | `domain/mi/service/nextgen/EventSnapshotService.java`, `EventSnapshotServiceImpl.java`, `support/EventSnapshotDiffCalculator.java` |
| 컨트롤러 (신규) | `domain/mi/controller/nextgen/mcp/McpEventSnapshotDedicatedController.java` (`@Profile("dedicated")`) |
| 유틸 (메서드 추가) | `common/utils/DateUtil.java` `kstToUtc`, `common/utils/JsonUtil.java` `parseStringList` |
| 테스트 | `McpOpenApiTest`(경로 19개·설명 노출), `McpControllerDelegationTest`(위임), `domain/mi/service/nextgen/EventSnapshotServiceImplTest`(신규 12건) |
| 문서 | Feature Spec 1.1·2.2·4.6·8.2·10절, 설계 Decision Log (D-1~D-5, D-8) |

수정하지 않은 공통 설정: `SwaggerConfig`, `WebMvcConfig`, `ResourceServerConfig`, `McpUserContextInterceptor`, 기존 MCP 컨트롤러 2개.

## 원문 해석 주의점 처리 (N2~N8)

| # | 처리 |
|---|---|
| N2 | `snapshot_dtm` → 실제 컬럼 `crt_dtm`. 버전 목록 정렬은 `snapshot_version_no DESC` |
| N3 | 보조 정렬 `crt_dtm DESC, mi_event_snapshot_id DESC`, 버전 번호 NULL 은 뒤로(`nullsLast`). `versionNo` 로 찾을 수 없음을 설명에 명시. testdb 에 NULL 행 없음 |
| N4 | `createdBy` 대소문자 무시 비교. 실데이터는 `system`, `system_backfill` |
| N5 | 사용자 확정 "모두 UTC" (D-5). `event_mod_dtm` 일부 행 +9h 관측은 위 B-5 에 기록 |
| N6 | `tags_json` = `{"tags":[...]}` → `List<String>` 응답 |
| N7 | 트리거 필터·라벨에 두 코드 정의. 실데이터는 `RECORD_INGESTION` 뿐, `MCP_USER_EDIT` 는 단위 테스트 범위 |
| N8 | 새 엔티티 `scenario_suggest_reason` 은 `length = 500`. `MiEvent` 는 변경하지 않음 |

## Verification

- `./gradlew compileJava -x test` (QueryDSL Q 클래스 생성)
- `./gradlew test -PforceTest` (`-PforceTest` 없이는 테스트가 실행되지 않음)
- 로컬 `run-backend.sh` (`testdb,dedicated`) 기동 후 MCP API 호출, 부록 B-7 SQL 과 대조

## Verification Result (2026-10-09)

- 테스트: 신규·수정 대상 21건 통과 (서비스 12, OpenAPI 5, 위임 4). 전체 239건 통과, 실패 0.
- 기동: `testdb,dedicated` 기동 성공, schema validation 통과 (엔티티 3개 = 실제 테이블).
- MCP 문서: 경로 19개(신규 6), 신규 operation 모두 `McpBasicAuth`·`X-User-Id`·403 응답 노출.
- API (키 `outer-skgas-mcp-server`, `X-User-Id: SKGAS001@SKGAS.com`, eventId 1269):
  - 목록: 4건, 버전 4→1, 값이 DB 와 일치, 시각 UTC+9h, 상세분석 본문 없음
  - lookup: latest→v4, asOf 2026-09-15T14:30 KST→v2, 14:02:53→v1, 첫 버전 이전→`found=false`, versionNo=3→sid 11, 동시 지정→400 `REQ-003`
  - compare: sid 7 previous → differences 본문 4개(recordedChangedFields 와 일치), sid 5 previous → `comparable=false`, sid 12 current → 차이 없음
  - records: sid 7 10건(DB 10건), 정렬·시각 일치 / outlooks: sid 7 13건(DB 13건), 정렬 일치
  - 오류: Basic Auth 없음 401, 틀린 키 401 `AUTH-002`, `X-User-Id` 없음·없는 사용자 403 `AUTH-007`, 다른 Event 의 snapshotId 400 `RSC-001`

## Issues / Risks

- 배포 환경에 Snapshot 테이블이 없으면 그 환경 backend 가 기동 실패한다 (`ddl-auto: validate`, 엔티티 스캔은 프로파일과 무관 → 일반 서버 포함).
- `event_mod_dtm` 이 일부 행에서 KST 로 저장된 것으로 보인다. UTC 로 처리하므로 해당 행의 `eventModifiedAt` 응답은 9시간 늦게 보일 수 있다 (파이프라인 확인 필요).
- testdb 에 비NEWS 근거 Record·원본 삭제 Record·`del_yn='Y'` 행이 없어 해당 분기는 실데이터로 검증하지 못했다.
- 기존 채널의 열린 이슈(`X-User-Id` 중복 로그인 ID 첫 행 선택 등)는 그대로 상속한다.

## Next Actions

- (완료) Step 9: `skgas-test-mcp` tool 6개 등록 + 기동 시 backend OpenAPI 로 tool 설명 결정 (실패 시 빌드 포함 문서)
- (완료) Step 10: completion-check — ADMIN 계약 무변경, MCP 그룹은 추가 6건뿐, 기준선 최초 승격
- skgas-test-mcp `mi_events_list` 에 dev 의 신규 선택 파라미터(`mine`, `sourceType`) 미노출 — 이번 범위 밖, 별도 작업
- 파이프라인 담당: `event_mod_dtm` 시각 기준 확인
- 배포 전: dev·운영 일반·전용 서버 master DB 의 Snapshot 테이블 적용 확인
