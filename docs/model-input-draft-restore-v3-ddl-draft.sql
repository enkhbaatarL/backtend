-- Model Input Draft 원본 복원 v3: DBA 증분 DDL 초안
-- Document Control
-- status: Draft (검토용, 자동/실제 DB 적용하지 않음)
-- canonical for: 이 slice의 CHECK 변경 SQL / pre-check / verify / rollback
-- owner: BE / DBA
-- date: 2026-10-27
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.14
-- 기존 install DDL 재실행 금지. 신규 접수/모든 writer 중단과 진행 작업 종료 확인 후 적용한다.
-- CHECK 검증 뒤 모든 웹 서버를 v2/v3 reader가 포함된 코드로 교체하고 접수를 재개한다.
-- 복원/preview는 설정에서 ON으로 고정된다. 혼합 버전 중에는 새 요청을 받지 않는다.
-- 현재 웹 러너 구성에는 별도 worker 프로세스를 배포하지 않는다.
-- 웹 전용 적용 순서: docs/model-input-draft-restore-testdb-runbook.md
-- 아래 SELECT/SHOW 결과와 제약명/정의가 예상과 다르면 중단한다.

-- 1. Pre-check: 현재 schema/MySQL CHECK 지원, 제약명, 기존 version 2만 존재 확인.
SELECT DATABASE() AS target_schema, VERSION() AS mysql_version;
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tmd_mdl_inptdrftrvsnl;
SHOW CREATE TABLE tmd_mdl_inptdrftrowh;
SELECT json_rvsn_no, prcs_type_cd, COUNT(*) AS row_count
FROM tmd_mdl_inptdrftprcsl GROUP BY json_rvsn_no, prcs_type_cd;
SELECT json_rvsn_no, COUNT(*) AS row_count
FROM tmd_mdl_inptdrftrvsnl GROUP BY json_rvsn_no;

-- 2. Apply: DBA 검토 후 아래 ALTER 한 건만 적용. backfill 없음.
-- MySQL DDL implicit commit/메타데이터 락 영향과 실행 시간을 DBA가 확인한다.
ALTER TABLE tmd_mdl_inptdrftprcsl
    DROP CHECK tmd_mdl_inptdrftprcsl_ck03,
    ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck03 CHECK (
        json_rvsn_no IN (2,3)
        AND (json_rvsn_no <> 3 OR prcs_type_cd = 'CHANGE')
        AND (base_rvsn_no IS NULL OR base_rvsn_no <= 9007199254740991)
    );

-- 3. Verify: ENFORCED CHECK와 invalid_rows=0 확인.
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SELECT COUNT(*) AS invalid_rows
FROM tmd_mdl_inptdrftprcsl
WHERE json_rvsn_no NOT IN (2,3)
   OR (json_rvsn_no = 3 AND prcs_type_cd <> 'CHANGE')
   OR (base_rvsn_no IS NOT NULL AND base_rvsn_no > 9007199254740991);

-- 4. Rollback pre-check: 신규 접수/모든 writer 중단 및 진행 작업 종료 후 두 값 모두 0인지 확인.
SELECT COUNT(*) AS v3_operations FROM tmd_mdl_inptdrftprcsl WHERE json_rvsn_no = 3;
SELECT COUNT(*) AS v3_revisions FROM tmd_mdl_inptdrftrvsnl WHERE json_rvsn_no = 3;

-- 두 값 중 하나라도 0이 아니면 CHECK 원복 금지. v3 reader를 유지하는 앱으로 rollback.
-- 5. Rollback SQL: 의도치 않은 동시 실행을 막기 위해 주석 처리. 조건 검증 후 DBA가 별도 실행.
-- ALTER TABLE tmd_mdl_inptdrftprcsl
--     DROP CHECK tmd_mdl_inptdrftprcsl_ck03,
--     ADD CONSTRAINT tmd_mdl_inptdrftprcsl_ck03 CHECK (
--         json_rvsn_no = 2
--         AND (base_rvsn_no IS NULL OR base_rvsn_no <= 9007199254740991)
--     );
