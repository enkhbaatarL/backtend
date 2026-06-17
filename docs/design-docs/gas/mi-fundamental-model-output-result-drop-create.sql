
-- 구 시나리오케이스 테이블 삭제
drop table tmd_scncaseh;
drop table tmd_scncasem;

-- 실행유형코드 공통코드 그룹 (신규 그룹이므로 detail 보다 먼저 등록)
INSERT INTO tcm_commcdm (
    cd_grp_id, cd_grp_nm, cd_grp_expn,
    use_bgn_dt, use_end_dt, use_yn
)
VALUES (
    'mdl_exec_type_cd', '수급모델실행유형코드', '수급모델 실행유형 (INDEPENDENT:독립실행 / SCENARIO:시나리오실행 / MI_BASE:MI Base 적재)',
    '20260616', '99991231', 'Y'
);

-- 실행유형코드 공통코드 상세 (tmd_mdl_scnrcasem.exec_type_cd)
INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES
    ('mdl_exec_type_cd', 'INDEPENDENT', '독립실행',     '1', 'MI Fundamental Model 독립 실행', '20260616', '99991231', 'Y', NULL, NULL),
    ('mdl_exec_type_cd', 'SCENARIO',    '시나리오실행', '2', 'MI 수급모델 시나리오 실행',     '20260616', '99991231', 'Y', NULL, NULL),
    ('mdl_exec_type_cd', 'MI_BASE',     'MI Base',      '3', 'MI Base 적재 데이터',          '20260616', '99991231', 'Y', NULL, NULL);

ALTER TABLE tmd_mdl_scnrcasem
  ADD COLUMN exec_type_cd VARCHAR(20) NOT NULL DEFAULT 'INDEPENDENT'
    COMMENT '실행유형코드 (INDEPENDENT/SCENARIO/MI_BASE)' AFTER scnr_id,
  ADD CONSTRAINT tmd_mdl_scnrcasem_ck01
    CHECK ((exec_type_cd = 'SCENARIO') = (scnr_id IS NOT NULL));

-- 기존 시나리오 row 재분류 (현재 없으면 0건)
UPDATE tmd_mdl_scnrcasem SET exec_type_cd = 'SCENARIO', mod_dtm = NOW()
WHERE scnr_id IS NOT NULL AND exec_type_cd <> 'SCENARIO';

-- ============================================================
-- 2. Apply: drop old and active result tables
-- ============================================================

DROP TABLE IF EXISTS tmd_mdl_scnrcasevoll;

DROP TABLE IF EXISTS tmd_mdl_scnrcaseprcl;
DROP TABLE IF EXISTS tmd_mdl_scnrcasetrdeflwl;
DROP TABLE IF EXISTS tmd_mdl_scnrcasetrdehdrl;
DROP TABLE IF EXISTS tmd_mdl_scnrcasevolrgnl;

-- ============================================================
-- 3. Apply: create active result tables
-- ============================================================

CREATE TABLE tmd_mdl_scnrcasevolrgnl
(
    vol_rgn_seq      BIGINT UNSIGNED AUTO_INCREMENT COMMENT '물량지역순번'
        PRIMARY KEY,
    run_seq          INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    sgmt_cd          VARCHAR(20)                           NOT NULL COMMENT '세그먼트코드 (GLOBAL/LNG)',
    data_type_cd     VARCHAR(30)                           NOT NULL COMMENT '데이터타입코드',
    rgn_cd           VARCHAR(50)                           NOT NULL COMMENT '지역코드',
    perd_unit_spt_cd VARCHAR(2)                            NOT NULL COMMENT '기간단위구분코드 (Y:년/M:월, PeriodUnitSeparateCd)',
    json_schm_ver    SMALLINT UNSIGNED DEFAULT 1           NOT NULL COMMENT 'JSON스키마버전',
    vol_data_json    JSON                                  NOT NULL COMMENT '물량데이터JSON',
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ux01
        UNIQUE (run_seq, sgmt_cd, data_type_cd, rgn_cd, perd_unit_spt_cd),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ck01
        CHECK (perd_unit_spt_cd IN ('Y', 'M')),
    CONSTRAINT tmd_mdl_scnrcasevolrgnl_ck02
        CHECK (JSON_VALID(vol_data_json))
)
    COMMENT 'TMD_시나리오케이스실행물량지역내역';

CREATE TABLE tmd_mdl_scnrcasetrdehdrl
(
    trd_hdr_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름헤더순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    selector_cd     VARCHAR(10)                           NOT NULL COMMENT '선택코드 (LNG/PNG)',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    axis_cd         VARCHAR(10)                           NOT NULL COMMENT '축코드 (FROM/TO)',
    hdr_key         VARCHAR(220)                          NOT NULL COMMENT '헤더키값',
    hdr_lbl_nm      VARCHAR(200)                          NOT NULL COMMENT '헤더라벨명',
    hdr_kind_cd     VARCHAR(30)                           NOT NULL COMMENT '헤더구분코드',
    rgn_cd          VARCHAR(50)                           NULL COMMENT '지역코드',
    nat_nm          VARCHAR(120)                          NULL COMMENT '국가명',
    hdr_ord         INT UNSIGNED                          NOT NULL COMMENT '헤더순서',
    CONSTRAINT tmd_mdl_scnrcasetrdehdrl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasetrdehdrl_ux01
        UNIQUE (run_seq, selector_cd, base_year, axis_cd, hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름헤더내역';

CREATE INDEX tmd_mdl_scnrcasetrdehdrl_ix01
    ON tmd_mdl_scnrcasetrdehdrl (run_seq, selector_cd, base_year, axis_cd, hdr_ord);

CREATE TABLE tmd_mdl_scnrcasetrdeflwl
(
    trd_flw_seq     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '거래흐름순번'
        PRIMARY KEY,
    run_seq         INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm         DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn          CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    selector_cd     VARCHAR(10)                           NOT NULL COMMENT '선택코드 (LNG/PNG)',
    base_year       SMALLINT UNSIGNED                     NOT NULL COMMENT '기준연도',
    from_hdr_key    VARCHAR(220)                          NOT NULL COMMENT 'From 헤더키값',
    to_hdr_key      VARCHAR(220)                          NOT NULL COMMENT 'To 헤더키값',
    from_rgn_cd     VARCHAR(50)                           NULL COMMENT 'From 지역코드',
    from_nat_nm     VARCHAR(120)                          NULL COMMENT 'From 국가명',
    to_rgn_cd       VARCHAR(50)                           NULL COMMENT 'To 지역코드',
    to_nat_nm       VARCHAR(120)                          NULL COMMENT 'To 국가명',
    flow_val        DOUBLE                                NOT NULL COMMENT '흐름값 (MTPA 기준)',
    CONSTRAINT tmd_mdl_scnrcasetrdeflwl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcasetrdeflwl_ux01
        UNIQUE (run_seq, selector_cd, base_year, from_hdr_key, to_hdr_key)
)
    COMMENT 'TMD_시나리오케이스실행거래흐름내역';

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix01
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year);

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix02
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year, to_rgn_cd);

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix03
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year, from_rgn_cd);

CREATE TABLE tmd_mdl_scnrcaseprcl
(
    prc_seq          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '가격순번'
        PRIMARY KEY,
    run_seq          INT UNSIGNED                          NOT NULL COMMENT '실행순번',
    crt_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '생성사용자로그인ID',
    crt_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    mod_usr_logn_id  VARCHAR(50) DEFAULT 'SYSTEM'          NOT NULL COMMENT '수정사용자로그인ID',
    mod_dtm          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    del_yn           CHAR(1)     DEFAULT 'N'               NOT NULL COMMENT '삭제여부',
    perd_unit_spt_cd VARCHAR(2)                            NOT NULL COMMENT '기간단위구분코드 (M:월/Y:년, PeriodUnitSeparateCd)',
    perd_key         VARCHAR(6)                            NOT NULL COMMENT '기간키값 (YYYY 또는 YYYYMM)',
    prc_rgn_nm       VARCHAR(200)                          NOT NULL COMMENT '가격지역명',
    base_case_prc    DOUBLE                                NOT NULL COMMENT '베이스케이스가격 (단위: USD/mmbtu 고정)',
    user_adj_prc     DOUBLE                                NOT NULL COMMENT '사용자조정가격 (단위: USD/mmbtu 고정)',
    CONSTRAINT tmd_mdl_scnrcaseprcl_fk01
        FOREIGN KEY (run_seq) REFERENCES tmd_mdl_scnrcaseh (run_seq),
    CONSTRAINT tmd_mdl_scnrcaseprcl_ux01
        UNIQUE (run_seq, prc_rgn_nm, perd_key),
    CONSTRAINT tmd_mdl_scnrcaseprcl_ck01
        CHECK (
            (perd_unit_spt_cd = 'Y'
             AND perd_key REGEXP '^[0-9]{4}$')
         OR (perd_unit_spt_cd = 'M'
             AND perd_key REGEXP '^[0-9]{6}$'
             AND CAST(SUBSTRING(perd_key, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12)
        )
)
    COMMENT 'TMD_시나리오케이스실행가격내역';

CREATE INDEX tmd_mdl_scnrcasetrdeflwl_ix03
    ON tmd_mdl_scnrcasetrdeflwl (run_seq, selector_cd, base_year, from_rgn_cd);
