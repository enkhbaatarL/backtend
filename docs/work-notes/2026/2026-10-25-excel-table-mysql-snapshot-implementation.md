# Excel Table MySQL Snapshot 구현·검증

## Document Control

- status: Complete
- canonical for: Execution record
- owner: BE
- date: 2026-10-25
- task type: implementation
- source documents:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §3.2
  - `docs/work-notes/2026/2026-10-25-excel-table-mysql-read-store-assessment.md`
- supersedes: 없음

## Task Goal

- 조회용 Snapshot을 MySQL에 적재·조회하고, 신규 Case 등록 후 자동 생성·검증·공개하는 경로를 구현한다. 원본/최종 Excel과 개인 Draft의 의미를 유지한다.

## Done Criteria

- 기본 OFF의 MySQL 저장·조회 및 회사/원본/정의/변환기 식별, bounded part 판독과 기존 SAX 복귀를 검증한다.
- 일반 Case 등록과 Draft 최종저장의 새 Case에 같은 transaction의 생성 의도를 남기고, 생성 실패/중단/중복 소비가 미완성 Snapshot을 공개하지 않음을 검증한다.
- 응답 동등성과 관련 회귀, 신규 스키마/SQL·비동기 경계의 독립 검토를 완료한다.
- 실제 MySQL/API 성능 증거와 로컬 H2/mock 증거를 분리한다. 실환경 접근이 막히면 필요한 조건과 재현 도구를 남기고 미검증 범위를 완료로 표시하지 않는다.

## Execution Plan

- Planned files: Snapshot Reader/새 DB Repository·생성기·백그라운드 소비자, ModelInputDraftService 내부 source 경계, Case 등록 및 Finalization의 enqueue 연결, 관련 tests, canonical gas design/DDL·운영 안내, 이 노트와 pilot.
- Scope: 공개 API 응답은 유지한다. 지원 표는 기존 PoC의 두 표부터 시작하며 자동 생성은 새 등록 Case에 한정한다. 기존 Case는 명시적 운영 적재로 검증한다. DB schema는 수동 적용 SQL로 준비하며 운영/testdb DDL·활성화·commit/push/배포는 수행하지 않는다.
- Lifecycle candidate: 회사+Case+builder-version별 생성 요청을 원자적으로 기록한다. QUEUED→BUILDING(token/deadline)→READY 또는 FAILED. 각 표 manifest와 bounded JSON part는 BUILDING에서만 저장하고 전체 검증/원본 재확인 후 공개한다. 만료 token은 publish할 수 없다. 실패는 자동 재시도하지 않으며 기존 Case/원본은 삭제하지 않는다.
- Risks: 대형 결과 buffer/DB 연결 점유, partial publication, 만료 worker fencing, cross-company identity, HTTP transaction과 비싼 생성 분리, 기존 파일 decoder 의미/BigDecimal/오류·빈값 보존, dirty worktree 충돌.

## Current Status

- status: Complete — 구현·로컬 및 실제 로컬 MySQL 검증 완료
- completed: 사용자 범위 확인, canonical schema/lifecycle, 기본 OFF DB 적재/판독, 두 등록 경로 자동 enqueue/소비자, 자체 임시 scope/기한·token fencing, 관련 고유225건 및 실제 MySQL1건 PASS, 독립 APPROVE.
- remaining: testdb parent schema/DBA 적용·배포·활성화와 인증 HTTP p95/서버 자원 확인은 후속 환경 작업이다. 이번 결과를 testdb 전체 P2 성능 승인으로 표현하지 않는다.

## Verification

- `./gradlew test -PforceTest --tests '<focused classes>'`
- 기본 OFF/중간 실패/오염된 Snapshot/회사·버전 불일치/행·열 편집 후 전체 응답 동등성.
- MySQL 사용 가능 여부와 실제 쿼리/packet/메모리·HTTP 비교를 별도 기록한다.

## Independent Review

- Session: `/root/mysql_snapshot_review` (독립 reviewer context).
- Scope: 신규 SQL/schema, source identity, transaction·중복 실행·만료 fencing 및 자원 경계. 실제 diff/관련 source/보존 XML·보고서를 직접 확인했다.
- Verdict: **APPROVE**, 미해결 BLOCKING0, 추가 필수 수정/선택 권고 없음.
- 생성 중 self-check/리뷰: 다운로드 기한 검사 전달, 자체 InputTempFiles scope로 shutdown/실패 정리 보호, DB TIMESTAMPADD 기한 계산을 보완했다. 기존 HTTP/편집 파일 동작과 API 응답은 유지한다.
- 검증 한계: 실제 testdb 부모 정의/DDL/인증 API 및 운영 부하와 자원은 미검증이다.

## Key Changes

- 신규 `ModelInputTableSnapshotRepository`: master 전용 bound SQL, 같은 Case transaction의 durable intent, 회사/Case/version unique, claim token/DB deadline, READY 조회, 제한된4-part batch와 fenced 쓰기.
- 신규 `InputTableSnapshotMysqlBuilder`: 단일 background thread, 기존 SAX로 JSON 추출, 목표1MiB/행8MiB/전체512MiB/1024part 한도, DB 재판독·checksum 검증 및 원본 identity 재확인 후 공개. 실패는 FAILED로 남기고 자동 재시도하지 않는다.
- `InputTableSnapshotReader`: 기존 decoder를 재사용해 DB manifest/part를 판독하고, 회사/파일/정의/버전이 다르거나 실패하면 동일 원본 SAX로 한 번 복귀한다. 부분 렌더 재시작과 권한/revision 예외 의미는 유지한다.
- 일반 Case 업로드와 최종저장 새 Case의 같은 transaction에 enqueue를 연결한다. 기존 Case 전체 backfill, 모든 표 확대, 편집 계산·최종 Excel 변경은 하지 않는다.
- `P6SpyFormatter`는 Snapshot table SQL의 payload/원본 위치가 기록되지 않도록 해당 SQL의 값을 마스킹한다. 관련 기본 로깅 동작도 시험했다.
- canonical 도메인 §3.2 / ERD §6.13, 증분 DDL3테이블, 운영 안내를 작성했다. 사전에는 SNAP 한 항목만 append했다. 자동 migration·profile 활성화는 없다.

## Verification Result

- 최초 컴파일의 checked Exception 처리, 시험 fixture의 ZIP 압축률/행 identity 준비, scope AutoCloseable 사용 및 부족한 fixture parent를 보정했다. 제품 baseline 결함을 넓게 수정하지 않았다.
- `regression-final.log` 및 `regression-final-results/`: 관련14+configuration1 suites 총224 PASS, failures/errors/skipped0.
- 마지막 builder 자체 scope 변경 후 `final-builder-check.log` 및 `final-builder-results/`: Mysql20 + configuration2 + download deadline1 =23 PASS. 기존 결과와 testcase identity로 합친 **로컬 고유225건 PASS**, failures/errors/skipped0. 별도 실제 MySQL1건과 합쳐226개 고유 testcase다.
- 검증 내용: MockMvc 전체 응답/숫자·UTF-8 의미, revision>0 overlay, source 변경 시 미공개, 다른 회사/사용자/revision 거부 전 Snapshot SQL 미호출, 기본 OFF시 DB 의존성 없이 startup, 중복/rollback/동시 claim/만료 token/partial corruption 후 SAX, 일반 등록 및 최종저장 예약, 실패 임시 scope 정리, 기존 file/reader/Case 보호·최종저장 회귀.
- 실제 로컬 MySQL: 사용자 실행 `mip-db` mysql:8.0.40, packet67108864. 기존 midb/midb2/mi-skens는 변경하지 않고 `mi_snapshot_verify_*` 새 schema와 무작위 임시 계정에만 실제 DDL/생성을 실행했다. 각 실행 후 생성한 schema/account만 정리했다.
- `InputTableSnapshotMysqlLiveTest` **1 PASS/0skip**: 실제 DDL3문·FK/CHECK(error3819), 중복 예약, 동시 claim1회, 만료 공개 거부, source→DB 생성/재판독/READY, SAX·파일·DB 셀 동등성. EXPLAIN은 PRIMARY/range/예상2part다.
- 실제 MySQL 추가 검증 중 발견: Spring이 CHECK3819를 DataIntegrityViolationException 대신 UncategorizedSQLException로 번역하여 시험은 SQL error code를 확인하도록 보정했다. 시험의 default REPEATABLE_READ 동시 claim은 deadlock이 발생했으며, 제품 Builder.tx의 **READ_COMMITTED**와 같은 설정으로 맞춘 최종 run은 통과했다. 제품 트랜잭션 격리 수준을 생략하거나 이 결과를 RR 안전성 증거로 쓰지 않는다.
- 실제 MySQL 실행은 `MI_SNAPSHOT_MYSQL_URL/USER/PASSWORD` opt-in이며 schema 이름/빈 상태를 검사한다. 값은 결과/저장소에 출력하지 않았다. 환경이 없으면 SKIPPED이므로 실행 여부를 XML로 확인한다.
- 근거: `/private/tmp/mysql-table-snapshot-20261025/verification-summary.json`, 보존 XML, `live-mysql.log`, `live-mysql-report.json`, `live-run-status.json`. 로컬 실행 orchestration은 임시 비밀번호를 메모리에서 생성해 전용 계정으로만 사용했다.

### 로컬 원본 판독 비교 — 인증 HTTP 성능이 아님

- 합성153개 본문 행·두 열(한 열은 긴 문자열), UTF-8/BigDecimal, 다중 part, 단일 소비, 예열3/측정20, Hikari max4/min1. 원본 객체 다운로드·HTTP·Draft 전체 DB 비용은 제외했다. 파일/DB part 경계도 동일하지 않아 저장 매체만의 엄밀한 대조 실험으로 해석하지 않는다.

| 경로 | p50 ms | p95 ms |
|---|---:|---:|
| 로컬 XLSX SAX | 14.066 | 17.702 |
| 파일 Snapshot | 9.242 | 15.464 |
| 로컬 MySQL Snapshot | 33.643 | 38.698 |

- 이 표본에서는 **MySQL이 파일보다 빠르지 않았다**. 파일 배포 제거/서버 공유의 운영 이점과 읽기 비용을 별도로 판단해야 한다. 기존 testdb 파일 Snapshot 약68% 개선을 DB 개선율로 옮기지 않는다. 실제 API 채택 판정에는 같은 빌드/입력의 testdb p95·오류율·메모리·DB 부하 비교가 필요하다.

## Changed Files

- Production: `ModelInputTableSnapshotRepository`, `InputTableSnapshotMysqlBuilder` 신규; Reader/WorkbookReader/ModelInputDraftService/ModelInputDraftRepository/CaseService/FinalizationRepository/P6SpyFormatter의 해당 경계만 변경.
- Tests: Mysql storage/live/configuration/deadline/redaction 신규, 기존 Case/Finalization 시험에 enqueue 증거 추가.
- Documents: canonical gas 도메인·ERD의 해당 절, SNAP dictionary1행, `docs/model-input-table-snapshot-ddl-draft.sql`, `docs/model-input-table-snapshot-runbook.md`, 이 노트와 pilot 해당 행.
- 기존 dirty source/fixture/profile/기존 문서·작업 기록·Git index는 보존한다. commit/push/개발 서버 배포나 설정 변경은 수행하지 않는다.
- 보존 확인: 최초 변경/미추적72파일 SHA 대조에서 이번 허용 파일 외 내용 변경0건이다. 본 작업은 index를 쓰지 않았다. 작업 중 다른 세션의 `0a4a0fc54`(BCRegasification v7) commit으로 HEAD와 index11항목이 변경된 사실을 확인했고, 해당 변경을 되돌리거나 재작성하지 않았다. 근거 `preservation-check.json`/`concurrent-index-changes.json`. Snapshot 변경은 미커밋이다.

## Human Acceptance

- Reviewed scope: 구현 범위 선택(신규 Case 자동 생성 포함). 결과 코드/시험은 Not sampled.
- Result: Not sampled.
- User-found omission: none; Redis 대안 문의는 구현 결함/변경 승인으로 해석하지 않았다.
- Calibration pilot: High 위험의 완료 implementation으로 post-pilot에 기록한다.

## Actual Result

- Outcome vs plan: MySQL Snapshot 및 신규 Case 자동 생성의 코드/로컬 동등성·안전성 검증을 완료했다. 실제 로컬 MySQL DDL·판독·동시성도 검증했다. 파일 배치 없이 DB에서 공급할 수 있으나 기본 OFF이며 실제 서버 적용은 별도다.
- Deviations: 초기 로컬 Docker 부재는 사용자가 MySQL을 실행해 해결했다. 저장 매체 비교는 로컬 transport에 한정했고 testdb HTTP 성능 완료로 주장하지 않는다. 기존 파일 decoder/권한·Draft 합성/파일 보존을 재사용했다.
- Done criteria: 이 구현·검증 범위 충족. 전체 환경 rollout/P2 API 부하 채택 판정은 미수행으로 남긴다.

## Issues/Risks / Next Actions

- testdb 적용은 parent type·CHECK·packet·권한/용량을 DBA가 확인하고 증분 SQL을 수동 적용한 뒤 진행한다. 새 Case 등록→QUEUED/READY 및 실제 인증 GET/편집·재시작을 확인한다.
- 기존 Case는 자동 적재되지 않는다. 운영 적재/실패 재실행/보관·정리와 대상 표 확대는 별도 범위다.
- 생성 중지/실패의120초는 DB 공개 fencing 및 협력적 검사다. 임의 네트워크/프로세스가 정확히120초에 종료된다는 보장은 하지 않는다.
- Redis는 재생성 가능한 캐시로 가능하지만 별도 메모리/eviction/재생성 정책 검증이 필요하다. 승인된 MySQL 구현을 Redis로 바꾸거나 두 저장소를 자동 도입하지 않았다.

## Delivery Follow-up — 2026-10-25

### Execution Plan

- 사용자 승인: 1단계인 Snapshot 변경만 선별 commit/개발계 배포 및 testdb 신규3테이블 수동 DDL 적용. 조회/생성 설정은 OFF 유지하며 기존 데이터 적재/활성화는 하지 않는다.
- 사용자 지정 대상: IntelliJ `@40.82.137.178` 연결의 `midb`. admin 연결과 혼동하지 않는다.
- 사전 확인: 실제 버전/packet/부모 type·engine·키/신규 테이블 부재 확인 후 canonical CREATE3문만 순서대로 적용하고 제약·빈 상태를 재확인한다. 기존 테이블 ALTER/데이터 수정/삭제는 없다.
- Git: 현재 HEAD0a4a0fc54와 shared dirty index를 보존하고 Snapshot delta만 임시 index로 검증한다. 기존 managed 배포 worktree에서 필요한 변경만 반영해 deploy/mi-be-test를 push한다.
- 위험: DDL implicit commit, 대상 연결 오인, parent mismatch/권한 부족, 일부만 적용된 상태 및 배포 branch의 기존 Reader 차이. 실패 시 강제 변경/삭제 없이 실제 상태를 기록한다.

### Current Status

- In progress: 대상 사용자 확인 완료. SSH BatchMode 연결은 인증 실패로 서버 직접 확인 불가. IntelliJ 지정 연결에서 사전 SQL 확인 예정.
- 구현/로컬 검증 Complete 상태와 실제 testdb 적용·배포 상태를 구분한다.

### testdb DDL Result

- 사용자 지정 IntelliJ `@40.82.137.178` / `midb`에서 실행했다. MySQL8.0.40, max_allowed_packet67108864, 부모4컬럼 unsigned type/PRIMARY/InnoDB 일치, 신규 Snapshot 객체0개를 먼저 확인했다.
- canonical CREATE3문만 schema-qualified 형태로 순서대로 실행했다. 기존 테이블 ALTER/DML/삭제, 기능 설정 변경은 실행하지 않았다.
- 재조회: header15/detail9/part9컬럼, FK총6개 RESTRICT, CHECK총6개 ENFORCED=YES, 모든 table InnoDB/0행. columns/default/collation/keys/check clauses/FK rules를 CSV/JSON으로 보존했다.
- 근거: `/private/tmp/mysql-table-snapshot-20261025/testdb-ddl-precheck.csv`, `testdb-ddl-verified.csv`, `testdb-ddl-verified.json`.
- 현재 DDL 적용만 완료했다. 앱 runtime 설정 OFF 여부와 신규 이미지 배포는 별도 확인 대상이며, 신규 Case 생성·Snapshot 적재·API 검증은 수행하지 않았다.

### Selective Delivery Verification

- feature HEAD0a4a0fc54 위에 본 작업23경로의 delta만 별도 index/tree로 구성했다. CaseService enqueue1줄, 기존 Case/Finalization test 해당 추가, canonical MySQL 절 및 pilot1행만 선별했다. 기존 dirty source/문서·fixture/profile/stage는 제외했다.
- 선택 tree를 `/private/tmp/mysql-table-snapshot-20261025/delivery/verify-feature`에 export해 `./gradlew test -PforceTest --tests '*InputTableSnapshotMysqlTest' --tests '*InputTableSnapshotMysqlConfigurationTest' --tests '*InputTableSnapshotDownloadDeadlineTest' --tests '*P6SpySnapshotRedactionTest' --tests '*FundamentalModelCaseServiceImplTest' --tests '*InputDraftFinalizationTest' --tests '*InputTableSnapshotReaderTest' --tests '*InputWorkbookReaderTest'`: **8 suites /138 PASS, failure/error/skip0**.
- 실제 index는 그대로 유지한 상태에서 검증했다. 최초 sandbox Gradle cache lock 접근 실패 후 승인된 동일 명령으로 정상 실행했다. 관련 로그/summary는 delivery 디렉터리에 보존했다.
- 원격 deploy/mi-be-test는 다른 세션의9d8f60086까지 진행돼 기존 clean managed worktree를 fast-forward했다. 해당 commit을 포함한 기존 배포 변경을 보존하며 Snapshot delta만 추가한다.
- feature 선별 commit **e9d9c07a25915e62941de9eda88e646f282410a5**를 origin/feature/gas2에 push했다. 원격 직전 HEAD0a4a0fc54와 비교해 이번23경로만 추가했다.
- 배포 worktree에는 같은 제품·canonical 문서15경로만 적용했다. 기존 배포 branch의 테스트/개인 pilot 제외 관례를 유지하며, 선택 feature tree의 시험과 helper19파일을 외부 sourceSet으로 연결해 같은8 suites **138 PASS /failure/error/skip0**를 확인했다. 최초 외부 시험 폴더의 helper3개 누락은 fixture만 보완해 해결했으며 제품 코드 추가 수정은 없다.
- 보존 검사: feature 커밋 후 이번23경로 밖 기존 index entry 변경0건, working content SHA 변경0건. 근거 `delivery/preservation-after-feature.json`.
- 조회/생성 설정의 기본 false와 profile 무변경을 유지했다. 서버 실행 이미지·실효 설정 확인은 SSH 인증 제약으로 아직 미확인이다. 원격 push를 실제 배포 완료로 표현하지 않는다.
