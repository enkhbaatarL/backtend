-- Document Control
-- status: Draft / USER REVIEW AND MANUAL APPLY ONLY
-- canonical for: Schema rollout — FINALIZE 준비파일의 최소 증분 apply/precheck/verify/보존 rollback SQL
-- owner: BE / DBA (user applies); date: 2026-10-19; last synced: 2026-10-19
-- source documents: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.3/§6.8
-- depends on: 위 정본의 물리 정의, domain-model-design.md §5.2~5.4의 lifecycle/복구
-- supersedes: 없음
-- open questions: 실제 MySQL 타입/FK/CHECK/시간대·권한/packet/DDL 적용 검증은 사용자 후속
-- 기존 catalog/Draft/요청 table을 재생성하지 않는다. Case103/기존 Draft/양식/원본은 수정하지 않는다.
-- SQL 실행/DB 접속은 사용자가 담당한다. MySQL DDL은 암묵 commit하므로 구간별 수동 검토한다.

-- 1. PRE-CHECK (같은 master schema). 결과 보관 후 아래 예상과 다르면 STOP.
SELECT DATABASE(), VERSION(), @@transaction_isolation, @@max_allowed_packet;
SELECT table_name, engine, table_collation FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN
 ('tmd_mdl_inptdrftm','tmd_mdl_inptdrftprcsl','tcm_filel','tmd_mdl_inptfnsvfiled');
-- 예상: 앞의 3개는 InnoDB, 마지막은 최초 적용 시 없음. 이미 있으면 SHOW CREATE 비교 후 apply 생략.
-- IF NOT EXISTS로 호환되지 않는 기존 정의를 숨기지 않는다.
SHOW CREATE TABLE tmd_mdl_inptdrftprcsl;
SHOW CREATE TABLE tcm_filel;
SELECT table_name,column_name,column_type,is_nullable,character_set_name,collation_name,datetime_precision
FROM information_schema.columns
WHERE table_schema=DATABASE() AND (
 (table_name='tmd_mdl_inptdrftprcsl' AND column_name IN ('prcs_id','try_tokn','prcs_type_cd','crt_dtm','mod_dtm','expr_dtm'))
 OR (table_name='tcm_filel' AND column_name IN ('file_no','file_size','strg_path_nm')))
ORDER BY table_name,ordinal_position;
-- 예상: prcs_id/try_tokn CHAR(36) ascii_bin, file_no BIGINT UNSIGNED.
-- 감사 crt_dtm/mod_dtm DATETIME(0), expr_dtm DATETIME(6). §6.8 증분은 별도 기존 승인 SQL을 따른다.
-- 기존 tcm_filel.file_size INT UNSIGNED와 새 준비file_size BIGINT UNSIGNED는 역할이 다름.
-- 앱은 기존 파일 상한 300MiB·경로255자 안에서만 저장한다. 기존 table ALTER는 이번 범위 아님.
SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name IN ('tmd_mdl_inptdrftprcsl','tcm_filel');
-- MySQL 8 CHECK 실제 강제, FK 양쪽 타입·collation·PK, 앱 master transaction 연결을 확인한다.
-- 16KiB/역할 strg_ref_json과 기존 요청/result 1MiB 및 성공delta16MiB의 protocol 여유를 DBA가 확인한다.

-- 2. APPLY (Draft FINALIZE 소비를 중지한 상태에서 아래 신규 table 하나만 생성).
CREATE TABLE tmd_mdl_inptfnsvfiled
(
    prcs_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    try_tokn CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    mdl_file_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    crt_usr_logn_id VARCHAR(50) NOT NULL,
    crt_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_usr_logn_id VARCHAR(50) NOT NULL,
    mod_dtm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    strg_ref_json LONGTEXT NOT NULL,
    prcs_stat_cd VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    file_size BIGINT UNSIGNED NULL,
    file_no BIGINT UNSIGNED NULL,
    PRIMARY KEY(prcs_id,try_tokn,mdl_file_cd),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ux01 UNIQUE(file_no),
    INDEX tmd_mdl_inptfnsvfiled_ix01(prcs_stat_cd,mod_dtm),
    CONSTRAINT tmd_mdl_inptfnsvfiled_fk01 FOREIGN KEY(prcs_id)
        REFERENCES tmd_mdl_inptdrftprcsl(prcs_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptfnsvfiled_fk02 FOREIGN KEY(file_no)
        REFERENCES tcm_filel(file_no) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck01 CHECK(mdl_file_cd IN ('SUPPLY','DEMAND','TRADE','OUTPUT')),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck02 CHECK(prcs_stat_cd IN ('PREPARED','UPLOADED','COMMITTED','CLEANUP_PENDING','CLEANED')),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck03 CHECK(
        (prcs_stat_cd='COMMITTED' AND file_no IS NOT NULL AND file_size IS NOT NULL)
        OR (prcs_stat_cd<>'COMMITTED' AND file_no IS NULL)),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck04 CHECK(JSON_VALID(strg_ref_json) AND OCTET_LENGTH(strg_ref_json)<=16384),
    CONSTRAINT tmd_mdl_inptfnsvfiled_ck05 CHECK(del_yn='N')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. BACKFILL: 없음. 신규 FINALIZE 시도에서 PUT 전 준비4역할을 기록한다.
-- 기존 tcm_filel/Case/object를 준비파일로 소급 등록하거나 이동하지 않는다.

-- 4. VERIFY: 정의/제약/index와 새 table만 추가됐는지 비교한다.
SHOW CREATE TABLE tmd_mdl_inptfnsvfiled;
SHOW INDEX FROM tmd_mdl_inptfnsvfiled;
SELECT column_name,column_type,is_nullable,character_set_name,collation_name,datetime_precision
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='tmd_mdl_inptfnsvfiled' ORDER BY ordinal_position;
SELECT constraint_name,referenced_table_name,update_rule,delete_rule
FROM information_schema.referential_constraints
WHERE constraint_schema=DATABASE() AND table_name='tmd_mdl_inptfnsvfiled';
-- 적용 직후 count0. 이후 별도 격리 fixture FINALIZE 검증에서만 아래 정합성 결과0을 확인한다.
SELECT COUNT(*) AS invalid_committed_files FROM tmd_mdl_inptfnsvfiled p
LEFT JOIN tcm_filel f ON f.file_no=p.file_no
LEFT JOIN tmd_mdl_inptdrftprcsl o ON o.prcs_id=p.prcs_id
LEFT JOIN tmd_mdl_inptdrftm d ON d.drft_id=o.drft_id
WHERE p.prcs_stat_cd='COMMITTED' AND (
 o.prcs_type_cd<>'FINALIZE' OR o.prcs_stat_cd<>'SUCCEEDED' OR NOT(o.try_tokn<=>p.try_tokn)
 OR f.file_no IS NULL OR f.file_rel_obj_spt_cd<>'SCNRCASE' OR f.del_yn<>'N'
 OR d.drft_stat_cd<>'FINALIZED' OR NOT(d.fnsv_case_id<=>f.file_rel_obj_dstg_no));
SELECT o.prcs_id,COUNT(p.mdl_file_cd) AS prepared_role_count
FROM tmd_mdl_inptdrftprcsl o LEFT JOIN tmd_mdl_inptfnsvfiled p
 ON p.prcs_id=o.prcs_id AND p.try_tokn=o.try_tokn AND p.prcs_stat_cd='COMMITTED'
WHERE o.prcs_type_cd='FINALIZE' AND o.prcs_stat_cd='SUCCEEDED'
GROUP BY o.prcs_id HAVING COUNT(p.mdl_file_cd)<>4;
-- 예상: invalid_committed_files=0, 두 번째 쿼리0행. 표준 SQL log에 strg_ref_json/object path를 덤프하지 않는다.

-- 5. ROLLBACK / OPERATING RETENTION
-- FINALIZE 접수/소비를 비활성화하고 이 table/요청 이력/성공 metadata/object를 보존한다.
-- 실행 중 worker와 전송 종료 확인 → 같은 master Draft/operation 잠금으로 실제 성공 여부 확인 →
-- 확정 FAILED 시도의 준비 object만 반복 정리한다. COMMITTED/commit 불명 object는 삭제하지 않는다.
-- 삭제1회/미발견1회는 CLEANED 증거가 아니다. 성공/실패/cleanup 이력에 자동 TTL은 없다.
-- DROP/TRUNCATE/DELETE/기존 Case·원본 수정/역방향 ALTER SQL은 제공하지 않는다.
-- 실제 제거가 필요하면 사용자가 보존/복구 계획과 독립 rollback 목표를 먼저 확정한다.

-- 6. DEPLOYMENT / GO-NO-GO
-- 사용자: precheck 보관 → 신규 table 수동 적용/verify → A의 공통 FINALIZE 연결 및 격리 worker 배포 →
-- 별도 fixture로 MySQL/storage/API/권한/기한/응답 유실 복구 확인 후 소비 활성화.
-- H2/로컬 파일 검증은 위 실환경 검증을 대체하지 않는다. 공통 API/worker 연결·검증의 실행 현황은
-- docs/work-notes/2026/2026-09-03-model-input-transfer-format.md를 참조한다(이 SQL은 사용자 수동 적용 전용).
