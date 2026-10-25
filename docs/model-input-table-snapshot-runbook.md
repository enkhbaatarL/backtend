# MySQL Table Snapshot 검증·적용 안내

## Document Control

- status: Draft
- canonical for: Reference only
- owner: BE / DBA
- date: 2026-10-25
- source documents:
  - `docs/model-input-table-snapshot-ddl-draft.sql` — 스키마·수동 DBA 적용 정본
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §3.2 — 수명·선택 계약
  - `docs/work-notes/2026/2026-10-25-excel-table-mysql-snapshot-implementation.md` — 실행 결과
- supersedes: 없음

## 1. testdb에서 검증할 수 있는가

가능하다. 같은 MySQL 서버의 별도 빈 `mi_snapshot_verify_*` schema로 SQL/저장소 검증을 먼저 수행하고, 이후 개발 애플리케이션 schema에 Snapshot 테이블3개를 수동 적용하여 신규 Case 및 인증 HTTP를 확인한다. 제품은 schema를 자동 생성하지 않는다.

두 시험을 구분한다.

- `InputTableSnapshotMysqlLiveTest`: 실제 MySQL DDL/제약/중복 예약/동시 claim/만료 fencing/생성·재판독과 SAX·파일·MySQL의 원본 셀 판독 비교. 합성 입력과 H2의 catalog metadata를 사용하며 인증 HTTP/Draft 전체 DB 비용·객체 다운로드는 제외한다.
- 개발 서버 검증: 실제 회사/Case/catalog·원본 객체·인증 GET과 편집 이력, 자동 생성·재시작·자원 부하를 포함한다. 전자의 통과를 후자의 통과로 부르지 않는다.

## 2. 격리 MySQL 시험에 필요한 것

DBA가 생성한 **새 빈 schema** `mi_snapshot_verify_<식별자>`와 그 schema에서만 SELECT/INSERT/UPDATE/DELETE/CREATE/REFERENCES/INDEX가 가능한 시험 계정이 필요하다. 기존 업무 schema를 테스트 URL로 지정하지 않는다. 이 시험은 schema 이름과 비어 있음을 검사한 뒤 보조 parent4개와 Snapshot3개 테이블을 만든다. JDBC driver는 프로젝트의9.2.0, 비교는 Hikari max4/min1이다.

MySQL8.0.16+의 enforced CHECK와 8MiB+2 part/statement overhead를 수용하는 packet 한도가 필요하다. 기존 TLS/인증 정책을 유지하며 packet/global 설정을 이 도구에서 바꾸지 않는다.

로컬 Mac 또는 DB 접속이 가능한 개발 환경에서 실행한다. URL/계정은 환경에 맞게 지정하고 비밀번호는 터미널 숨김 입력 또는 승인된 secret 주입으로 제공한다.

```bash
export MI_SNAPSHOT_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/mi_snapshot_verify_example'
export MI_SNAPSHOT_MYSQL_USER='시험용계정'
# MI_SNAPSHOT_MYSQL_PASSWORD는 숨김 입력/secret 주입으로 설정한다.
./gradlew test -PforceTest --tests '*InputTableSnapshotMysqlLiveTest'
```

- URL에 user/password query parameter를 넣지 않는다. 비밀번호나 환경 전체를 출력하지 않는다.
- 환경 변수가 없으면 시험은 SKIPPED다. `BUILD SUCCESSFUL`만으로 실제 MySQL 통과를 주장하지 않는다.
- `MYSQL_SNAPSHOT_REPORT=<path>`는 test XML의 system-out에 나온다. 응답 값/원본 URL/비밀번호를 포함하지 않는 요약 JSON이다.
- 재실행에는 새 빈 schema를 사용한다. 기존 schema를 도구에서 비우거나 DROP하지 않는다. 일회성 시험 schema/account 정리는 만든 운영자/DBA가 소유 범위를 확인하고 수행한다.

## 3. 개발 서버 수동 적용 순서

1. 두 기능 설정이 OFF인 상태에서 증분 DDL의 PRECHECK를 실행한다. 실제 parent type/권한/packet/용량과 기존 객체 충돌이 없음을 DBA가 확인한다.
2. DBA가 APPLY의 CREATE TABLE3문을 수동 실행하고 SHOW CREATE/CHECK/FK를 검증한다. DDL은 암묵 commit이므로 실패하면 완료된 문장을 확인하고 중단한다.
3. 호환 코드가 배포됐는지 확인한다. 기존 JVM/메모리·재시작 설정을 보존한다.
4. 먼저 `mi.model-input.table-snapshot.mysql.enabled=true`, 기존 GET 플래그 `mi.model-input.table-snapshot.enabled=false`로 새 Case 생성 의도와 READY를 확인할 수 있다. 이 단계의 GET은 SAX다.
5. 지원 양식으로 새 시험 Case를 일반 업로드 또는 Draft 최종저장한다. 기존 Case는 이 설정을 켠 것만으로 자동 적재되지 않는다. 기존 Case122 등의 적재는 별도로 승인된 운영 작업이며 대량 backfill은 하지 않는다.
6. 생성 요청의 QUEUED/BUILDING/READY/FAILED를 아래 읽기 SQL로 확인한다. 일반 업로드/최종저장 HTTP 성공 자체를 Snapshot 성공으로 취급하지 않는다.
7. READY를 확인하고 GET 플래그도 true로 설정·재시작한다. 같은 이미지/Case/Draft/revision/page에서 OFF/ON 전체 응답과 correlated stage 로그를 비교한다. 정상 hit의 WORKBOOK_DOWNLOAD/PARSE0과 TABLE_SNAPSHOT_READ를 확인한다.
8. 편집 후 동일 결과, 회사/사용자·revision 거부, 재시작 후 DB 데이터 유지, 손상/miss 시 SAX 복귀를 검증한다. 예상 동시 부하에서 HTTP p95/오류율/API·DB CPU/메모리/GC/쿼리 bytes/pool wait를 함께 본다.

서버는 영구 Snapshot 파일·mount·fixed-inputs가 필요 없다. 원본·최종 Excel의 기존 객체 저장소와 편집 중 파일 보존 검증은 유지한다. 지원 두 표 밖은 SAX다.

```sql
SELECT snap_id,cust_cpny_no,org_case_id,base_ver_no,prcs_stat_cd,expr_dtm
FROM tmd_mdl_inptsnapm ORDER BY crt_dtm DESC,snap_id DESC LIMIT 20;
SELECT snap_id,inpt_tbl_id,COUNT(*) AS part_count,SUM(OCTET_LENGTH(cntt_json)) AS json_bytes
FROM tmd_mdl_inptsnapjsond GROUP BY snap_id,inpt_tbl_id;
```

원본 URL/셀 JSON/시도 token을 운영 결과에 출력하지 않는다. 앱은 Snapshot SQL의 bound payload 로그를 가리고, 생성 완료/실패는 Case/company/type만 기록한다.

## 4. 실패·되돌리기

- 미준비/실패 Snapshot은 선택하지 않으며 GET은 기존 SAX를 사용한다. 실패는 Case/원본/성공 Draft를 삭제하거나 되돌리지 않는다.
- 생성 기한은 DB claim의120초다. 다운로드 chunk·생성·검증에 협력적 기한 검사를 적용하고 SQL은10초 query/transaction timeout을 사용한다. DB token/deadline fencing은 만료 공개를 거부하며, 임의 JVM/네트워크가 정확히120초에 강제 종료된다는 보장은 아니다.
- FAILED는 자동 재시도하지 않는다. 실패 원인과 worker 상태를 확인하고 별도 운영 재생성을 결정한다. 활성 token/READY 데이터를 SQL로 강제 초기화하지 않는다.
- rollback은 GET와 mysql 설정을 모두 false로 되돌리고 호환 코드로 재시작한다. 새 테이블과 READY 데이터는 보존한다. 기존 업무 테이블/원본은 변경하지 않는다.
