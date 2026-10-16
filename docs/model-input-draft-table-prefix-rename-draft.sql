-- Document Control
-- status: Draft / USER MANUAL APPLY ONLY
-- canonical for: testdb midb input catalog/Draft table prefix migration, verify and inverse rename
-- owner: BE / user executes; date: 2026-10-16
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.6
-- execution record: docs/work-notes/2026/2026-09-03-model-input-transfer-format.md
-- Codex has NOT connected to the DB or executed this file.
-- MySQL8.0 reference: https://dev.mysql.com/doc/refman/8.0/en/rename-table.html
-- implicit commit: https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html

-- 적용 대상: 이전 DDL로 만든 catalog5개+Draft5개, 총10개 table 이름만 바꾼다.
-- tmd_mdl_scnrcasetmpltd, 기존 Case/파일/사용자/회사와 다른 기능 table은 유지한다.
-- 후속 요청/최종준비 file2개는 아직 생성하지 않았으므로 포함하지 않는다.
-- 기존 데이터/열/PK/UK/FK/CHECK/index 구조와 명시 제약/index 이름은 유지한다.
-- _fk/_ck/_ux/_ix로 명명한 기존 식별자를 DROP/재생성하거나 FK 검사를 끄지 않는다.
-- 새 설치/등록 SQL도 기존 명시 제약 이름을 유지한다. prefix 변경은 table명에만 적용한다.
-- 데이터 backfill/복사/삭제는 없다. 전체 파일 대신 아래 구간을 순서대로 선택 실행한다.

-- 1. PRE-CHECK — 사용자 확인, 아직 RENAME을 실행하지 않는다.
-- 해당 API/초기 등록과 이를 쓰는 앱을 멈추고 진행 중 transaction을 끝낸다.
-- 기대: target_db=midb, 아래10행 모두 old_exists=1 / new_exists=0 / old_engine=InnoDB.
-- 새 이름이 이미 있거나 일부만 바뀐 경우2구간을 실행하지 말고 결과를 공유한다.
-- 필요한 권한: 구 table ALTER/DROP + 새 table CREATE/INSERT. table 단위 앱 권한은 새 이름에도 별도 확보한다.
SELECT DATABASE() AS target_db, VERSION() AS mysql_version;
WITH rename_map AS (
    SELECT 'tmm_mdlinpttmpltm' AS old_name, 'tmd_mdl_inpttmpltm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptfiled' AS old_name, 'tmd_mdl_inptfiled' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptshtd' AS old_name, 'tmd_mdl_inptshtd' AS new_name
UNION ALL
    SELECT 'tmm_mdlinpttbld' AS old_name, 'tmd_mdl_inpttbld' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptmodscopd' AS old_name, 'tmd_mdl_inptmodscopd' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftm' AS old_name, 'tmd_mdl_inptdrftm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftfiled' AS old_name, 'tmd_mdl_inptdrftfiled' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrvsnl' AS old_name, 'tmd_mdl_inptdrftrvsnl' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrowm' AS old_name, 'tmd_mdl_inptdrftrowm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrowh' AS old_name, 'tmd_mdl_inptdrftrowh' AS new_name
)
SELECT m.old_name, m.new_name,
       (o.table_name IS NOT NULL) AS old_exists,
       (n.table_name IS NOT NULL) AS new_exists,
       o.engine AS old_engine
FROM rename_map m
LEFT JOIN information_schema.tables o ON o.table_schema='midb' AND o.table_name=m.old_name
LEFT JOIN information_schema.tables n ON n.table_schema='midb' AND n.table_name=m.new_name
ORDER BY m.old_name;

-- 이름 문자열을 직접 쓰는 DB view/routine/trigger 의존이 조회되면 새 이름 대응을 먼저 확인한다.
-- 정의 조회 권한이 없으면0행만으로 의존 부재를 단정하지 않는다. 필요한 앱/배치 SQL도 확인한다.
SELECT 'VIEW' AS object_type, table_name AS object_name
FROM information_schema.views
WHERE table_schema='midb' AND LOCATE('tmm_mdlinpt', view_definition)>0
UNION ALL
SELECT routine_type, routine_name
FROM information_schema.routines
WHERE routine_schema='midb' AND LOCATE('tmm_mdlinpt', routine_definition)>0
UNION ALL
SELECT 'TRIGGER', trigger_name
FROM information_schema.triggers
WHERE trigger_schema='midb' AND LOCATE('tmm_mdlinpt', action_statement)>0;

-- 적용 전10개 table 행 수를 보관하고 적용 후 같은 대상과 비교한다.
SELECT 'tmm_mdlinpttmpltm' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinpttmpltm
UNION ALL
SELECT 'tmm_mdlinptfiled' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptfiled
UNION ALL
SELECT 'tmm_mdlinptshtd' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptshtd
UNION ALL
SELECT 'tmm_mdlinpttbld' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinpttbld
UNION ALL
SELECT 'tmm_mdlinptmodscopd' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptmodscopd
UNION ALL
SELECT 'tmm_mdlinptdrftm' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptdrftm
UNION ALL
SELECT 'tmm_mdlinptdrftfiled' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptdrftfiled
UNION ALL
SELECT 'tmm_mdlinptdrftrvsnl' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptdrftrvsnl
UNION ALL
SELECT 'tmm_mdlinptdrftrowm' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptdrftrowm
UNION ALL
SELECT 'tmm_mdlinptdrftrowh' AS table_name, COUNT(*) AS row_count FROM midb.tmm_mdlinptdrftrowh;

-- 2. APPLY — 1구간 조건/권한·의존 확인 후 이 단일 문장만 선택 실행한다.
-- 같은 midb 안에서10개를 함께 rename한다. 새 table을 CREATE하거나 데이터를 재등록하지 않는다.
-- MySQL8.0/InnoDB의 단일 RENAME TABLE은 조건 충족 시 함께 성공/실패한다. metadata lock 대기는 확인한다.
-- DDL implicit commit 대상이므로 START TRANSACTION/ROLLBACK으로 되돌릴 수 있다고 보지 않는다.
RENAME TABLE
    midb.tmm_mdlinpttmpltm TO midb.tmd_mdl_inpttmpltm,
    midb.tmm_mdlinptfiled TO midb.tmd_mdl_inptfiled,
    midb.tmm_mdlinptshtd TO midb.tmd_mdl_inptshtd,
    midb.tmm_mdlinpttbld TO midb.tmd_mdl_inpttbld,
    midb.tmm_mdlinptmodscopd TO midb.tmd_mdl_inptmodscopd,
    midb.tmm_mdlinptdrftm TO midb.tmd_mdl_inptdrftm,
    midb.tmm_mdlinptdrftfiled TO midb.tmd_mdl_inptdrftfiled,
    midb.tmm_mdlinptdrftrvsnl TO midb.tmd_mdl_inptdrftrvsnl,
    midb.tmm_mdlinptdrftrowm TO midb.tmd_mdl_inptdrftrowm,
    midb.tmm_mdlinptdrftrowh TO midb.tmd_mdl_inptdrftrowh;

-- 3. VERIFY — 기대 old_tables=0 / new_tables=10 / qry_json_columns=1.
WITH rename_map AS (
    SELECT 'tmm_mdlinpttmpltm' AS old_name, 'tmd_mdl_inpttmpltm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptfiled' AS old_name, 'tmd_mdl_inptfiled' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptshtd' AS old_name, 'tmd_mdl_inptshtd' AS new_name
UNION ALL
    SELECT 'tmm_mdlinpttbld' AS old_name, 'tmd_mdl_inpttbld' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptmodscopd' AS old_name, 'tmd_mdl_inptmodscopd' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftm' AS old_name, 'tmd_mdl_inptdrftm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftfiled' AS old_name, 'tmd_mdl_inptdrftfiled' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrvsnl' AS old_name, 'tmd_mdl_inptdrftrvsnl' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrowm' AS old_name, 'tmd_mdl_inptdrftrowm' AS new_name
UNION ALL
    SELECT 'tmm_mdlinptdrftrowh' AS old_name, 'tmd_mdl_inptdrftrowh' AS new_name
)
SELECT
    SUM(o.table_name IS NOT NULL) AS old_tables,
    SUM(n.table_name IS NOT NULL) AS new_tables,
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema='midb' AND table_name='tmd_mdl_inpttbld' AND column_name='qry_json') AS qry_json_columns
FROM rename_map m
LEFT JOIN information_schema.tables o ON o.table_schema='midb' AND o.table_name=m.old_name
LEFT JOIN information_schema.tables n ON n.table_schema='midb' AND n.table_name=m.new_name;

-- 구 table명을 참조하는 FK가 남았는지 확인한다. 기대 결과0행.
SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema='midb' AND referenced_table_schema='midb'
  AND referenced_table_name IN
    ('tmm_mdlinpttmpltm',
     'tmm_mdlinptfiled',
     'tmm_mdlinptshtd',
     'tmm_mdlinpttbld',
     'tmm_mdlinptmodscopd',
     'tmm_mdlinptdrftm',
     'tmm_mdlinptdrftfiled',
     'tmm_mdlinptdrftrvsnl',
     'tmm_mdlinptdrftrowm',
     'tmm_mdlinptdrftrowh');

-- Case-양식 FK 포함 실제 연결을 확인한다. constraint_name의 tmm_ 접두사는 의도한 기존 이름이다.
SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema='midb' AND referenced_table_schema='midb' AND referenced_table_name IS NOT NULL
  AND (table_name='tmd_mdl_scnrcasetmpltd' OR table_name IN
    ('tmd_mdl_inpttmpltm',
     'tmd_mdl_inptfiled',
     'tmd_mdl_inptshtd',
     'tmd_mdl_inpttbld',
     'tmd_mdl_inptmodscopd',
     'tmd_mdl_inptdrftm',
     'tmd_mdl_inptdrftfiled',
     'tmd_mdl_inptdrftrvsnl',
     'tmd_mdl_inptdrftrowm',
     'tmd_mdl_inptdrftrowh'))
ORDER BY table_name, constraint_name, ordinal_position;

-- 적용 전과 각 table의 행 수가 같은지 비교한다.
SELECT 'tmd_mdl_inpttmpltm' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inpttmpltm
UNION ALL
SELECT 'tmd_mdl_inptfiled' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptfiled
UNION ALL
SELECT 'tmd_mdl_inptshtd' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptshtd
UNION ALL
SELECT 'tmd_mdl_inpttbld' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inpttbld
UNION ALL
SELECT 'tmd_mdl_inptmodscopd' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptmodscopd
UNION ALL
SELECT 'tmd_mdl_inptdrftm' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptdrftm
UNION ALL
SELECT 'tmd_mdl_inptdrftfiled' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptdrftfiled
UNION ALL
SELECT 'tmd_mdl_inptdrftrvsnl' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptdrftrvsnl
UNION ALL
SELECT 'tmd_mdl_inptdrftrowm' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptdrftrowm
UNION ALL
SELECT 'tmd_mdl_inptdrftrowh' AS table_name, COUNT(*) AS row_count FROM midb.tmd_mdl_inptdrftrowh;

-- 4. BACKFILL: 필요 없음. 원본 파일/Case·양식/이미 등록한 시험 데이터·Draft 기록을 보존한다.
-- 이후 새 이름 코드/등록 SQL을 사용한다. 이미 시험 데이터를 넣었으면 INSERT SQL을 다시 실행하지 않는다.
-- 앱 계정의 새 이름 접근을 확인한 후 기존 원본 구조→Draft 생성/재개→표 조회를 검증한다.
-- 실제 DB rename/권한/행 수와 API 통과는 사용자 실행 결과 전 미검증이다.

-- 5. ROLLBACK / FALLBACK — 필요할 때만 별도 실행. 자동 실행을 막기 위해 아래는 주석이다.
-- 신규 쓰기/해당 앱을 멈추고 신 이름10개·구 이름0개를 확인한 뒤 역 rename과 구 이름 앱을 함께 사용한다.
-- table 단위 권한도 다시 확인한다. 사용자 데이터 삭제/CREATE 덮어쓰기나 FK 검사 해제로 복구하지 않는다.
-- RENAME TABLE
--     midb.tmd_mdl_inpttmpltm TO midb.tmm_mdlinpttmpltm,
--     midb.tmd_mdl_inptfiled TO midb.tmm_mdlinptfiled,
--     midb.tmd_mdl_inptshtd TO midb.tmm_mdlinptshtd,
--     midb.tmd_mdl_inpttbld TO midb.tmm_mdlinpttbld,
--     midb.tmd_mdl_inptmodscopd TO midb.tmm_mdlinptmodscopd,
--     midb.tmd_mdl_inptdrftm TO midb.tmm_mdlinptdrftm,
--     midb.tmd_mdl_inptdrftfiled TO midb.tmm_mdlinptdrftfiled,
--     midb.tmd_mdl_inptdrftrvsnl TO midb.tmm_mdlinptdrftrvsnl,
--     midb.tmd_mdl_inptdrftrowm TO midb.tmm_mdlinptdrftrowm,
--     midb.tmd_mdl_inptdrftrowh TO midb.tmm_mdlinptdrftrowh;

-- Open Questions / user follow-up: 실제10개 engine/기존 제약·table 권한/DB 의존·metadata lock/행 수는 미확인.
-- Implementation readiness: ready for user pre-check; apply only after pre-check passes. No DB execution by Codex.
