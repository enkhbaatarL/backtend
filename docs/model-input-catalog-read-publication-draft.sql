-- Document Control
-- status: Draft / 실행 전 사용자·DBA 검토용 / NOT EXECUTED BY CODEX
-- canonical for: C 버전1 조회 공개의 발행·새 Case 연결 SQL 초안 (업무 정책/schema 정본 아님)
-- owner: BE / DBA; date: 2026-10-19; last synced: 2026-10-19
-- source: docs/model-input-catalog-structure-data-draft.sql (재실행 금지, 검증값만 재사용)
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md §3.3
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.2/6.5/6.8/10B
-- execution record / 서버·Swagger 순서: docs/work-notes/2026/2026-10-19-model-input-catalog-read-publication.md
-- supersedes: 없음. open questions: 대상 master·새 Case·감사 계정·실제 파일 대응 증빙은 사용자 입력.
-- 완료 범위: 자료 준비. 이 파일은 실행 승인이나 실제 DB 상태/조회 성공 증거가 아니다.
-- 사용자 보고 대상: tmplt_id=7 / DRAFT_C_STRUCTURE / version1 / DRAFT / 적용시각NULL / 삭제N.
-- 목표: 이 정확한 정의를 ACTIVE로 발행하고 별도 새 Case 한 건만 연결. 역할4/시트70/표92/수정범위0.
-- 일반 쓰기 INCOMPLETE·LNG Summary NOT_ALLOWED·보류7개 유지. READY 승격/하위 정의 변경 없음.
-- Case103/양식1, Case105·106/양식2 및 모든 기존 Draft·파일·연결을 보존한다.
-- 기존 시험 Draft 334df4d8-26a4-4d45-bccb-7b6e5445bf81은 FINALIZED/revision1 (사용자 보고).
-- 실행 조건: master MySQL8.0.16+ / InnoDB / FK·CHECK·ACTIVE unique 유지 / UTF-8 연결.
-- 새 전용 연결, 같은 세션에서 순서대로 실행. 오류 즉시 중단(stop-on-error); --force 사용 금지.
-- 새 Case/4파일 등록·COMMIT은 선행 별도 작업. 이번 파일은 Case/파일을 생성·변경하지 않는다.
-- 대상 C catalog writer와 새 Case의 업로드/조회/Draft 생성은 이 짧은 transaction 종료까지 중지한다.
-- 기존 F-O-01 서버/worker는 설정을 바꾸지 않는다. 긴 파일 확인은 transaction 시작 전에 마친다.
-- NULL/STOP/오류/불일치가 하나라도 있으면 즉시 ROLLBACK 후 종료. 후속0행은 성공이 아니다.
-- 첫 검증 실행은 기본 ROLLBACK. 확정 실행도 결과를 읽은 뒤 사용자가 같은 연결에서 COMMIT을 선택한다.
-- 이미 ACTIVE/연결된 재실행은 DML0행으로 중단한다. 같은 연결이어도 검증 조회만 하고 유지한다.
-- 다른 ACTIVE/다른 연결/soft-deleted 연결은 충돌이다. 대체·DEPRECATED 전환·DELETE·upsert 없음.
-- 발행 후 정의 변경은 새 tmplt_id/버전으로 처리한다. 참조 후 DRAFT 회귀/삭제/기존 Case 재연결은
-- 일반 rollback이 아니다. COMMIT 후 문제는 새 연결을 중지하고 영향·복구 근거를 별도 검토한다.

-- 1. 먼저 실행할 사전 확인 (여기에는 영구 데이터 쓰기가 없다)
-- 이 실행에 사용할 실제 schema 이름을 명시한다. USE/기본 DB 자동 선택은 하지 않는다.
SET @p_schema = NULL;
SET @p_case = NULL;       -- 사용자가 별도 준비한 새 Case ID. 103/105/106 금지.
SET @p_actor = NULL;      -- 실제 감사 login (1..50자). SYSTEM 임의 대입 금지.
SET @p_files = NULL;      -- 아래 형식의 실제 네 등록 파일/불변 객체 대응. 복사 예시 값 금지.
-- [{"role":"DEMAND","fileNo":<정수>,"objectRef":"<실제 strg_path_nm>"},
--  {"role":"SUPPLY",...},{"role":"TRADE",...},{"role":"OUTPUT",...}]
-- transaction 전에 실제 file_no→SCNRCASE/newCase→불변 객체→평문 XLSM과 C 양식7의 구조 대응을 확인한다.
-- 동일 포맷: 조회 대상 시트 이름/표시 상태, 표·헤더·열·본문 범위·행 식별 열·control의 좌표/의미가
-- 등록된 표 정의와 qry_json에 호환된다. 고정 범위 밖의 추가 행/열은 자동으로 읽지 않는다.
-- 데이터 값과 파일 전체 bytes는 달라도 된다. normalized 파일명/내용 일치나 내용 hash 입력은 요구하지 않는다.
-- domain §6 규칙39~40(2026-10-19 사용자 변경): SUMMARY/플랫폼 입력 안내가 남아 있어도 연결할 수 있다.
-- 두 시트는 catalog/화면/편집 대상으로 추가하지 않고 원본 파일 안에 보존한다. 제거/재업로드는 요구하지 않는다.
-- 조회 대상 시트의 순서 차이는 허용한다. 필요한 시트/좌표 변경은 별도 검토한다.
-- 필요한 셀 저장값은 reader가 판독할 수 있어야 한다. 수식은 재계산하지 않으며 저장된 결과가 필요하다.
-- SQL은 원격 XLSM의 구조/저장값을 읽지 못한다. 실제 객체 또는 해당 불변 객체의 신뢰할 수 있는
-- 구조 검증 기록으로 위 항목을 확인한다. 파일명/크기나 기존 baseline 판독 성공만으로 승인하지 않는다.
-- 비밀키/토큰/서명 URL은 입력·증빙·공유 결과에 넣지 않는다.
SET @p_evidence = NULL;   -- 확인자·시각·4개 role/file_no/객체와 양식7의 구조 호환 확인 결과를 남긴 접근제한 증빙 식별자
SET @p_reviewed = NULL;   -- §1 결과·실제 DDL·Case 접근 권한·실제 파일/구조 호환 확인 후에만 1로 지정
SET @p_tmplt = 7;
SET @p_model = 'DRAFT_C_STRUCTURE';
SET @p_version = 1;
SET @p_marker = 'structure-c-20261019; baselineId=2026-10-07-delivery-261006-v8-v6; partial=92/99; NOT_PUBLISHED';
-- marker의 NOT_PUBLISHED는 최초 등록 provenance다. 발행 상태는 tmplt_stat_cd/aply_bgn_dtm로 판단한다.
SET @p_ok = 0, @p_published = 0, @p_linked = 0, @p_verified = 0;
SELECT DATABASE() AS selected_master, VERSION() AS mysql_version, @@sql_mode, @@autocommit,
       @@transaction_isolation, @@session.time_zone, @@character_set_connection;
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_inptmodscopd;
SHOW CREATE TABLE tmd_mdl_scnrcasetmpltd;
SHOW CREATE TABLE tmd_mdl_scnrcasem;
SHOW CREATE TABLE tcm_filel;
SHOW CREATE TABLE tmd_mdl_inptdrftm;
-- 실제 PK/FK/ACTIVE unique, qry_json LONGTEXT, crt_*/mod_* DATETIME(0), aply_bgn_dtm DATETIME(6)을 정본과 대조.
-- 차이가 있으면 중단·A/DBA 인계. 이 파일에 DDL/자동 수정은 없다.
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_ver_nm,tmplt_stat_cd,aply_bgn_dtm,del_yn,tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE tmplt_id IN (1,2,7) OR mdl_spt_cd=@p_model ORDER BY tmplt_id;
SELECT t.tmplt_id,
 (SELECT COUNT(*) FROM tmd_mdl_inptfiled f WHERE f.tmplt_id=t.tmplt_id) AS roles_expect_4,
 (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=t.tmplt_id) AS sheets_expect_70,
 (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=t.tmplt_id) AS tables_expect_92,
 (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id) JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=t.tmplt_id) AS scopes_expect_0,
 (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd r WHERE r.tmplt_id=t.tmplt_id) AS links_expect_0,
 (SELECT COUNT(*) FROM tmd_mdl_inptdrftm d WHERE d.tmplt_id=t.tmplt_id) AS drafts_expect_0
FROM tmd_mdl_inpttmpltm t WHERE t.tmplt_id=@p_tmplt;
SELECT case_id,exec_type_cd,del_yn,crt_usr_logn_id,crt_dtm
FROM tmd_mdl_scnrcasem WHERE case_id IN (103,105,106,@p_case) ORDER BY case_id;
SELECT case_id,tmplt_id,del_yn FROM tmd_mdl_scnrcasetmpltd
WHERE case_id IN (103,105,106,@p_case) OR tmplt_id=@p_tmplt ORDER BY case_id;
SELECT drft_id,org_case_id,tmplt_id,drft_stat_cd,rvsn_no,actv_prcs_id,fnsv_case_id,del_yn
FROM tmd_mdl_inptdrftm WHERE org_case_id IN (103,105,106,@p_case) OR fnsv_case_id=@p_case OR tmplt_id=@p_tmplt
ORDER BY drft_id;
-- 아래 객체 경로는 접근제한 화면에서만 대조하고 공유 로그에 복사하지 않는다.
SELECT file_no,org_file_nm,strg_path_nm,file_rel_obj_dstg_no,file_vald_end_dt,del_yn
FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no=@p_case ORDER BY file_no;
-- 새 Case 유효형태 INDEPENDENT/MI_BASE·삭제N·연결0·관련Draft0, 유효 파일 정확히4건을 확인.
-- 보호 대상의 기존 결과도 보관한다. 새 Case ID가 없으면 여기서 멈추고 §2 이후를 실행하지 않는다.

-- 2. 등록 초안에서 추출한 읽기 전용 대조 데이터 (세션 변수뿐, 하위 INSERT 없음)
-- row SHA-256은 아래 CONCAT_WS(CHAR(31),...)의 UTF-8 원문 bytes 기준이다.
-- qry_json 공백/키 순서가 달라도 다른 bytes면 중단한다. 정규화 UPDATE로 맞추지 않는다.
-- 아래 파일명은 기존 catalog metadata 대조용이다. 실제 업로드 파일명과의 완전 일치는 요구하지 않는다.
-- 역할4/시트70/표92 검사는 DB catalog 정의에 대한 것이며, 실제 XLSM의 전체 시트 수/순서 비교가 아니다.
SET @p_expected_files = '[
["DEMAND","DEMAND-normalized.xlsm",1],
["SUPPLY","SUPPLY-normalized.xlsm",2],
["TRADE","TRADE-normalized.xlsm",3],
["OUTPUT","OUTPUT-normalized.xlsm",4]
]';
SET @p_expected_sheets = '[
["DEMAND","Input Adjustment ->",1,"VISIBLE"],
["DEMAND","GDPPopulationAdjustment",2,"VISIBLE"],
["DEMAND","ElectrificationRateAdjustment",3,"VISIBLE"],
["DEMAND","PowerMixAdjustment",4,"VISIBLE"],
["DEMAND","CoalGasAdjustment",5,"VISIBLE"],
["DEMAND","WeatherAdjustment",6,"VISIBLE"],
["DEMAND","PMIAdjustment",7,"VISIBLE"],
["DEMAND","HydrogenAdjustment",8,"VISIBLE"],
["DEMAND","LNGBunkeringAdjustment",9,"VISIBLE"],
["DEMAND","Scenario Tracker",10,"HIDDEN"],
["DEMAND","DemandOutputOverwrite",11,"VISIBLE"],
["DEMAND","기준정보 ->",12,"VISIBLE"],
["DEMAND","BCInput",13,"VISIBLE"],
["DEMAND","MacroParametersLogicInput",14,"VISIBLE"],
["DEMAND","PMIInput",15,"VISIBLE"],
["DEMAND","PMILogicInput",16,"VISIBLE"],
["DEMAND","WeatherLogicInput",17,"VISIBLE"],
["DEMAND","WeatherInput",18,"VISIBLE"],
["DEMAND","LNGBunkeringInput",19,"VISIBLE"],
["DEMAND","LNGBunkeringLogicInput",20,"VISIBLE"],
["DEMAND","HydrogenInput",21,"VISIBLE"],
["DEMAND","LNG Demand",22,"VISIBLE"],
["DEMAND","General",23,"VISIBLE"],
["DEMAND","Sheet2_DISCARDAFTUSE",24,"HIDDEN"],
["DEMAND","CubeDump_DISCARDAFTUSE",25,"HIDDEN"],
["SUPPLY","Input Upstream ->",1,"VISIBLE"],
["SUPPLY","ProductionAdjustment",2,"VISIBLE"],
["SUPPLY","Input Midstream ->",3,"VISIBLE"],
["SUPPLY","{USA}Hurricanes",4,"VISIBLE"],
["SUPPLY","LiqUtilAdjustment",5,"VISIBLE"],
["SUPPLY","LiqProdAdjustment",6,"VISIBLE"],
["SUPPLY","RegasAdjustment",7,"VISIBLE"],
["SUPPLY","PipelineAdjustment",8,"VISIBLE"],
["SUPPLY","BCProduction",9,"VISIBLE"],
["SUPPLY","BCHurricanes",10,"VISIBLE"],
["SUPPLY","BCLiquefaction",11,"VISIBLE"],
["SUPPLY","BCRegasification",12,"VISIBLE"],
["SUPPLY","BCPipeline",13,"VISIBLE"],
["SUPPLY","LNG Summary",14,"VISIBLE"],
["SUPPLY","LiqUtilAdjustment_backup",15,"VISIBLE"],
["SUPPLY","LiqProdAdjustment_backup",16,"VISIBLE"],
["SUPPLY","BCLiquefaction_backup",17,"VISIBLE"],
["SUPPLY","{USA}RigsLogic",18,"HIDDEN"],
["SUPPLY","{China}PolicyLogic",19,"HIDDEN"],
["SUPPLY","{USA}HurricanesLogic",20,"HIDDEN"],
["SUPPLY","RegasLogic",21,"HIDDEN"],
["SUPPLY","PipelineLogic",22,"HIDDEN"],
["SUPPLY","MonthlyAdjProduction",23,"HIDDEN"],
["TRADE","LNG_TradeRestrictionInput",1,"VISIBLE"],
["TRADE","PNG_TradeRestrictionInput",2,"VISIBLE"],
["TRADE","Sheet1",3,"HIDDEN"],
["TRADE","MidstreamContractInput",4,"VISIBLE"],
["TRADE","ShippingCostInput",5,"VISIBLE"],
["TRADE","MidstreamCapacityInput",6,"VISIBLE"],
["TRADE","UpstreamAdjLogicInput",7,"VISIBLE"],
["TRADE","DemandAdjLogicInput",8,"VISIBLE"],
["TRADE","CostInput",9,"VISIBLE"],
["TRADE","4.5 PriceModelInput",10,"VISIBLE"],
["TRADE","LNGExportRestictionInput",11,"VISIBLE"],
["TRADE","backupLNG_TradeRestrictionInput",12,"VISIBLE"],
["TRADE","backup_MidstreamContractInput",13,"VISIBLE"],
["TRADE","Industry research",14,"HIDDEN"],
["TRADE","net exporter",15,"HIDDEN"],
["TRADE","pipeline cap",16,"HIDDEN"],
["TRADE","pipeline contract",17,"HIDDEN"],
["OUTPUT","USCostofSupply",1,"VISIBLE"],
["OUTPUT","BCPrices",2,"VISIBLE"],
["OUTPUT","PremiumCalc",3,"VISIBLE"],
["OUTPUT","AnnualPriceCalc",4,"VISIBLE"],
["OUTPUT","PriceSeasonality",5,"VISIBLE"]
]';
SET @p_expected_tables = '[
["DEMAND","GDPPopulationAdjustment","GDP Adjustment","72e83b50262884cf51f51513096bf18236f960daed2b5f3ec7404a328081b5c9"],
["DEMAND","GDPPopulationAdjustment","Population Adjustment","a855016fcb9b42fef838e0c8c936bb053c60966cdaec8765d9e46d59c6ccd3cd"],
["DEMAND","ElectrificationRateAdjustment","Electrification Rate Adjustment","4c59cf151a1a4984806526f56eb4fa6fab916d3f9f754ebe4868580753f938fd"],
["DEMAND","PowerMixAdjustment","Annual Power Mix Adjustment","0b5266a6c8a52da78a2e216d85c75e3a7854677d01cb0a80c65d01a86d38c6ee"],
["DEMAND","PowerMixAdjustment","Monthly Power Mix Adjustment","8785dccf4ae3104e0a499d59da3427bc9488dbbc63a0bf49b4cc45a5679d4817"],
["DEMAND","CoalGasAdjustment","Coal Futures Price Adjustment","ef7cfb73aa43b94c6e36be89fc728f63f7d4408807249a21d11912a245b3c0bc"],
["DEMAND","CoalGasAdjustment","Gas Futures Price Adjustment","23aa2750e03ebc3f441ebe0d4acf538785fc7a2ba5595979716b6218d9c2f98b"],
["DEMAND","WeatherAdjustment","Country HDD and CDD Adjustment","f7aaa9e50caa93bd4cd778a5f71f2a8064c2949f4270bc774d20c373beaae4a3"],
["DEMAND","WeatherAdjustment","Country Temperature Adjustment","28208ac7a0656323de74b45072301b9e21013f5a9815eec7db93bcf6985924f6"],
["DEMAND","WeatherAdjustment","Regional HDD and CDD Adjustment","5541dd017e7596b066663be7f98d8afb3496abcef82eb6d0bbe8cf1c80b06310"],
["DEMAND","WeatherAdjustment","Regional Temperature Change Adjustment","b496699de9a3dba5199368b43731d9be1dd293f841441baab3d33ed54d500be4"],
["DEMAND","PMIAdjustment","Monthly PMI Adjustment","c44821cd607e12bb072c3c0ad0f47f4f4f5ed67f7923e902077dea21b10edb8a"],
["DEMAND","PMIAdjustment","PMI Reference Country Adjustment","53447db1b93aa6e1c1ed90aca196691c0818bc5096c2a6e30ef46020ff812bcf"],
["DEMAND","HydrogenAdjustment","Hydrogen Production Adjustment","4e2d150faa75418db82f057d7d2f7718447d24259f73340827b2d97bc1e784f3"],
["DEMAND","LNGBunkeringAdjustment","LNG Bunkering Vessel Count Adjustment","41ca9e981b8346306817d43c1b0a0245966671165624c5ba456803f207491718"],
["DEMAND","LNGBunkeringAdjustment","LNG Bunkering Fuel Consumption Adjustment","ee76779d69dcf7f88b9cd1f6f9b387cf841f0f39ad144d211c40f094d74e1d8a"],
["DEMAND","DemandOutputOverwrite","Demand Model Manual Overwrite Sector Input","e99389e5d0d9e3a5e86ccbf78743e8e588b2130d716278be4052c785a2046ee2"],
["DEMAND","DemandOutputOverwrite","Demand Model Manual Overwrite Monthly Input","80b3497e98dead6004d54290951d0714f68cde69b30ed5b2abf67aa52c2384a3"],
["DEMAND","BCInput","RE Base Case Gas Demand Data by Sector","66e568859b69eecdf3aebb37cab873f517dc108548745f0370470974139ec92b"],
["DEMAND","BCInput","RE Base Case GDP and Population Data","3a1f3c4602fcd8f2dc895e6aa3f33bd8784a1a1745720482a11f115b08221b0c"],
["DEMAND","BCInput","Power Generation Data","64fafcb85e3b8b00f20d2c393198c7b0b33070cc6f669f10626eb36001b9d482"],
["DEMAND","BCInput","Power Generation Utilisation Rate","da339c5df79d7508fe2025ad9e7242702c9b6578db8a347a5a1debbf5e08c3e3"],
["DEMAND","BCInput","Car Sales for Electrification Input","59fc58c3def42e92a2ab1cb05ddf0521b95502219ac454ccc126ae21ca9a89ca"],
["DEMAND","BCInput","Base Case LNG Bunkering Demand","7dfcba9daf2188957195b0f1238c0b291ffed5586c6b3467af187fdcb1c8c1b1"],
["DEMAND","BCInput","Base Case Hydrogen Demand","0b6a42b480ef0a31124c3b2fa37a5cf278232f483dde81211044273825e4a258"],
["DEMAND","BCInput","Coal and Gas Futures Pricing","5699c476a8fb3b8e83fbda515e772b1ce4aa99581b7f9ae33572204dd4a3ad63"],
["DEMAND","MacroParametersLogicInput","Selected Macro Parameter Intensities","94a4ac3c9654201d41b962602c4a4ceca1e23cbf178f3ee5c861d3fa0ce30bc6"],
["DEMAND","PMIInput","Base Case PMI Input","70cac046a56804278ba3d3e1d00e841a5c6f6b830f5733e6a2a1bc998d7efc07"],
["DEMAND","PMILogicInput","User-Adjusted PMI Intensity Parameter","826694c23e8319b55f51f2bc4ce59a066d50d533ef1d892db5ee699890e94ed0"],
["DEMAND","PMILogicInput","Base Case PMI Intensity Parameter","f8a46ce9c168091bcbc7e829ac55b27934c074d3be7004efd754a5a49b82dc29"],
["DEMAND","WeatherLogicInput","User-Adjusted HDD-Gas Intensity Input","6ef7814036133bf7701042f9695db56e34f2a51b7bdfc138da1704043b27e6d8"],
["DEMAND","WeatherLogicInput","User-Adjusted CDD-Gas Intensity Input","53dccd120b69e0fc4bc2b4f280c66c5bbd7ef756d0cf2bb80777690826e670fc"],
["DEMAND","WeatherLogicInput","Base Case HDD-Gas Intensity Input","412fba83452e9f36f00349f205e327090494925039533019d17f1f16ae92bb48"],
["DEMAND","WeatherLogicInput","Base Case CDD-Gas Intensity Input","9005ffd4c3345523fa3b1c1e506b34954e3fd859108ebbca99cef675a60b80af"],
["DEMAND","WeatherInput","Base Case HDD Input","75f996497f3d5e189af8c8c9f9748283c34bef5adec7284b38d47f67a2b6421d"],
["DEMAND","WeatherInput","Base Case CDD Input","222861345a69244a73ea7edd0c307cde07bfb96b7fea0a7418acd03ccd0e813e"],
["DEMAND","WeatherInput","Base Case Temperature","a282bd85ae0ab7372bd818fdacba56158474b9f01d9f23b49ff2cf9dda43e48b"],
["DEMAND","LNGBunkeringInput","Base Case LNG Bunkering Input","92c848bb3dd6d040dce99fbe0e87c97963d7650483052f8981db866c9c771193"],
["DEMAND","LNGBunkeringLogicInput","LNG Bunkering Share by Country","6465cba12216b64655489923f46305e21eeb231c4f977522506a17efb410e9b4"],
["DEMAND","HydrogenInput","Base Case Hydrogen Production Input","f316a13f199a313414415e316810cb8066f3721667e0a27dcfed8b91932a9911"],
["SUPPLY","ProductionAdjustment","PNG Production Adjustment","d8d03c7ce5af54d0f3391a291219a57741ded521db37d481816171d0e78d6d8c"],
["SUPPLY","{USA}Hurricanes","Monthly Hurricane Adjustment","83bc0ca89c6af67bd90cfe4aed7589db3bb4ff8bd42f6c90361b4efe3e37927d"],
["SUPPLY","LiqUtilAdjustment","Monthly LNG Train Utilization","49ca1930d27370195e178870337dfc604b1aa76c627a467d7bd11465e011debf"],
["SUPPLY","LiqProdAdjustment","Monthly LNG Train Production","bdc4882a241b921c5cfce36ff6bc3cb338df9572297275f4e427e9fcad03f05e"],
["SUPPLY","RegasAdjustment","Monthly Regas Capacity Adjustment","e3ad772da2dd1da20c28eef50e30669dd7f07618c4de55452a8b821ad17ef856"],
["SUPPLY","RegasAdjustment","Monthly Regas Capacity by Country","d09aa1a7f5fdffdc719716763cfda7d6d65f86147c2d7a9c3c3bef854ac15861"],
["SUPPLY","BCProduction","Annual Gas Production Baseline","151df41ab60f19c3bf7f5f0761c11b421255308bc6312f233484fbce5f0b0f12"],
["SUPPLY","BCHurricanes","Hurricane Impact on Upstream Assets","dede9c3548ee3865ef47f76a07259ff6762c980750b90661db44cb94a0de7b49"],
["SUPPLY","BCHurricanes","Hurricane Affected Provinces","ba1b33eb68ac9962e8405b48fba0a87abbb00b41474870bdeb8e666cfe8254ec"],
["SUPPLY","BCHurricanes","Hurricane Impact on Liquefaction Plants","d39ddc7516a13645d1c4251c403585b44b7a9c5edcea0a02523c0a8632e8a4d4"],
["SUPPLY","BCHurricanes","Hurricane Affected LNG Plants","845adb7528050d7263969581df61b2fd1f77e229d2cd251c34bce61725fb6a75"],
["SUPPLY","BCLiquefaction","Monthly LNG Train Baseline","239214af16384094f94f66dccdc97f300c14af776ad6fcc6e1d53cb8dff551e8"],
["SUPPLY","BCRegasification","Annual Regas Capacity Baseline","17ba66600ce981d110743dc7e0fda19c96b103440d5c0cdd0ec8c053813482f6"],
["SUPPLY","BCPipeline","Annual Pipeline Capacity Baseline","bea0f2995fe144fa481a32d5471b71bc9822e193b85e8ae5e52730419095eae9"],
["SUPPLY","LNG Summary","LNG Production Reconciliation","4f4460d16c17a840d1280b810a4e35b04d2187e2dc2cfd26152248daf1fffa06"],
["TRADE","LNG_TradeRestrictionInput","LNG Restriction","247ca2e9e9109b762dac06b8e7f4e0c6e1fe6bd62b99e90d4e25fa7156eac059"],
["TRADE","PNG_TradeRestrictionInput","PNG Restriction","2f18d0b8b3a1cf3823ecb8b03106346db70a8744b424b3618f4459ae694b2cd2"],
["TRADE","MidstreamContractInput","Forecasted PNG Contract Volume (Imports)","8e9efe3840989c002af62affe3132896ff301f5f2a32b42391276ae9b54bedaa"],
["TRADE","MidstreamContractInput","LNG Contract Volumes","a54b855ec6014a524145cae8837eba5e35647c270ce89bf97bf187d17cbda744"],
["TRADE","ShippingCostInput","Shipping Route Parameters","ef7841dab303222223c36572148015da06d1741d912efe9950ff89852743f5d6"],
["TRADE","ShippingCostInput","Importer Classification","9d2f0661f64d7e13427db1bf26dbaa9f9cb15aafcb8f6d08d24a7d75fb941cc6"],
["TRADE","ShippingCostInput","Exporter List","4b8912bc7a5b8570a7ba1cd3dea72c26e4fd9df92c7525f0f4d6f387a0273a50"],
["TRADE","ShippingCostInput","Atlantic Charter Cost","aeae52fb202616fc36d62f2e021f870e1b4eb330fe4e0a178916dfcdf54c6364"],
["TRADE","ShippingCostInput","Fuel Cost - IFO380","c06c19ae92b2f63df9cdf85db77871bc86807ca601bddbfe9951bee451b5fa3a"],
["TRADE","ShippingCostInput","Suez Canal Cost","873ec4c61ee5f9063152c81ea1a9f2b7c1569fce9de262598944dcdab06e2c77"],
["TRADE","ShippingCostInput","Panama Canal Cost","f666bee305e1c71f6ea8ce5ae233971a609860ad79977249ce9005232177aa07"],
["TRADE","ShippingCostInput","Suez Canal Disruption","c944eba9011a80977416569d68bdb0d85c333197e013d03d19fbbcb05cefb52f"],
["TRADE","ShippingCostInput","Panama Canal Disruption","4547e30c92abe984f92c9d07217c258a8a5c8af5279cf600d5cedf21c035079b"],
["TRADE","ShippingCostInput","Technical Parameters","67b1be936ab3e8464eccf46bf9fe17a3af9be43e58c76a5989557a56fadf39e1"],
["TRADE","ShippingCostInput","Panama Spot Capacity","4419f47f8a764b1dfa46b3d708e90b213373dbc5390f118175cdcf2bae01c541"],
["TRADE","MidstreamCapacityInput","PNG Infrastructure","7467b1bdad01c9de15a831ffd636a40a84471829c04ef4463586faf2e112b868"],
["TRADE","MidstreamCapacityInput","Pipeline Import Allocation Shares","3916f4f9762ee55a2301be364f39d918cdab5d18505407919b443a985f73de76"],
["TRADE","MidstreamCapacityInput","Nord Stream Importer Allocation","d8f01d6f520b6c0ce178d70e44d4506b5c057f48f45b662b72b0d92b2a61e0b3"],
["TRADE","UpstreamAdjLogicInput","Country Supply Adjustment","4b8511caf630c74848fa98fd9711e8d6dc0f0edab5e7bea10df4be5bdf36381a"],
["TRADE","UpstreamAdjLogicInput","Excess LNG to PNG Countries","95801fcd940a496df9f87b7eb8b0af703a146c2e24072d677969547921ee56e4"],
["TRADE","DemandAdjLogicInput","Premium Market Countries","b3783db4bd50ccc3ff4aff4ed04d35c48125620b074b7fbc2f5f1ceb935848be"],
["TRADE","DemandAdjLogicInput","Demand Adjustment Parameters","eb54573dc0000fbab71651fba86267ebcddfbf6515e698056e308561ae6fb52f"],
["TRADE","DemandAdjLogicInput","Demand Adjustment Bands","af9bd04c9f575f471e69cd3024af1b608930a78eaec39e17f5916f7360b0512f"],
["TRADE","CostInput","Regasification Tariff","1b6def0df9e2086a1fcc71dd3d0b0b5e327870d6daaf05543abcb96007ad547e"],
["TRADE","CostInput","Liquefaction Cost","08ee0534582c91ed9f4fb2d92bd4d22cc5b64eabdc3a26f35d1c0b78ce1783dd"],
["TRADE","CostInput","LNG Upstream Cost","07d4c3211eead1ffffcbd19904eeca53a6f45b036732f8370f40b66ed1e7a671"],
["TRADE","CostInput","Non-LNG Upstream Cost","ee085189987310de80485a3c6b1ee8810dad3a34ae2f4b72d597ddfd6edad4e8"],
["TRADE","CostInput","Pipeline Cost","0162b44377ec76f99cbcc157e267b667e7fddc64192e682566c3b8b5e2db3898"],
["TRADE","CostInput","LNG Project SRMC","ec8f0ed651234b965aac951bcb3cc9986ba232c374421c292940bc28366bce9e"],
["TRADE","4.5 PriceModelInput","JKM - Base Case","f03791e6dd18840d7d4c8164791c106eb921335a2642cd4b3b35c4d4c6e32da7"],
["TRADE","4.5 PriceModelInput","TTF - Base Case","8629379a7323bc9234b621641b8aeb7cdb1d1ef7cc4be406eb2494c772b82817"],
["TRADE","LNGExportRestictionInput","LNG Export Restriction","a71a45cafeb0c1815838bf7caa6843c1d755e4e43bd603ee46d775fb68011963"],
["OUTPUT","USCostofSupply","US Cost of Supply","8c0a4534300b68a2683dbc94e9e411a0cb077ea99ebab739e95af8ca218a633f"],
["OUTPUT","BCPrices","Base Case Prices","d2ef331dcb40dcf0f348ddd491833cea8d07661b3a18ef7a50b220c11cbeef94"],
["OUTPUT","PremiumCalc","PremiumCalc TTF","1cb9c4e37818ba2c62d10193fcc0fa5b7fcdb16eec16ee82ef02d5199a03a74d"],
["OUTPUT","PremiumCalc","PremiumCalc East Asia Spot","8fc9760df77bb66d9e4278eac77eb8d1d103beecce170646637f9bfc38e2edd6"],
["OUTPUT","PriceSeasonality","Price Seasonality","d26495b8ee9bd356aaf2fc18de8f9f11a27c480f994dc830fd76bd089174e55d"]
]';

-- 3. 잠금과 정확한 DRAFT/새 Case 검사. 모든 파일 증빙 작업은 이미 끝나 있어야 한다.
-- 현재 세션의 미완료 transaction이 없는지 확인한 전용 연결에서만 시작한다.
-- SERIALIZABLE은 이 transaction만 적용. 대상/부재 범위를 잠가 동시에 연결되는 것을 방어한다.
-- lock timeout/deadlock이면 오류 즉시 ROLLBACK. 자동 재시도/부분 재개 없음.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT case_id,exec_type_cd,del_yn FROM tmd_mdl_scnrcasem WHERE case_id=@p_case FOR UPDATE;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,del_yn
FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@p_tmplt OR mdl_spt_cd=@p_model ORDER BY tmplt_id FOR UPDATE;
SELECT file_no FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no=@p_case ORDER BY file_no FOR UPDATE;
SELECT case_id,tmplt_id,del_yn FROM tmd_mdl_scnrcasetmpltd WHERE case_id=@p_case OR tmplt_id=@p_tmplt FOR UPDATE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE org_case_id=@p_case OR fnsv_case_id=@p_case OR tmplt_id=@p_tmplt FOR UPDATE;
SELECT inpt_file_id FROM tmd_mdl_inptfiled WHERE tmplt_id=@p_tmplt FOR UPDATE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt FOR UPDATE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt FOR UPDATE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt FOR UPDATE;
SET @p_today = DATE_FORMAT(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+09:00'),'%Y%m%d');
SET @p_inputs_ok = IF(
 BINARY DATABASE()=BINARY @p_schema AND @p_tmplt=7 AND BINARY @p_model=BINARY 'DRAFT_C_STRUCTURE' AND @p_version=1
 AND @p_case BETWEEN 1 AND 2147483647 AND @p_case=FLOOR(@p_case) AND @p_case NOT IN (103,105,106)
 AND CHAR_LENGTH(TRIM(@p_actor)) BETWEEN 1 AND 50 AND @p_reviewed=1
 AND CHAR_LENGTH(TRIM(@p_evidence))>0 AND JSON_TYPE(@p_files)='ARRAY' AND JSON_LENGTH(@p_files)=4,1,0);
SET @p_root_ok = (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm
 WHERE tmplt_id=@p_tmplt AND BINARY mdl_spt_cd=BINARY @p_model AND tmplt_ver_no=@p_version
 AND BINARY tmplt_ver_nm=BINARY '2026-10-07 v8-v6 structure DRAFT' AND BINARY tmplt_expn=BINARY @p_marker
 AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N');
SET @p_active_count = (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@p_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N');
SET @p_existing_links = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE case_id=@p_case OR tmplt_id=@p_tmplt);
SET @p_existing_drafts = (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE org_case_id=@p_case OR fnsv_case_id=@p_case OR tmplt_id=@p_tmplt);
SET @p_case_ok = (SELECT COUNT(*) FROM tmd_mdl_scnrcasem WHERE case_id=@p_case AND del_yn='N' AND exec_type_cd IN ('INDEPENDENT','MI_BASE'));
SET @p_file_count = (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@p_tmplt);
SET @p_file_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_files,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', name VARCHAR(255) PATH '$[1]', ord INT PATH '$[2]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 WHERE BINARY f.file_nm=BINARY x.name AND f.file_ord=x.ord AND f.file_type_cd='XLSM' AND f.file_strt_hash_val IS NULL AND f.del_yn='N');
SET @p_sheet_count = (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_sheet_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_sheets,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', name VARCHAR(150) PATH '$[1]', ord INT PATH '$[2]', state VARCHAR(20) PATH '$[3]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.name
 WHERE s.sht_ord=x.ord AND s.sht_stat_cd=x.state AND s.del_yn='N');
SET @p_table_count = (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_table_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_tables,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', sheet VARCHAR(150) PATH '$[1]', name VARCHAR(100) PATH '$[2]', digest CHAR(64) PATH '$[3]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
 JOIN tmd_mdl_inpttbld b ON b.inpt_sht_id=s.inpt_sht_id AND BINARY b.inpt_tbl_nm=BINARY x.name
 WHERE b.del_yn='N' AND s.sht_stat_cd='VISIBLE' AND b.qry_json IS NOT NULL AND JSON_VALID(b.qry_json)=1
 AND OCTET_LENGTH(b.qry_json)<=1048576
 AND SHA2(CONCAT_WS(CHAR(31),b.inpt_tbl_nm,b.tbl_ord,b.tbl_bgn_row_no,b.tbl_end_row_no,b.tbl_bgn_col_no,b.tbl_end_col_no,
 b.hdr_bgn_row_no,b.hdr_end_row_no,b.data_bgn_row_no,b.row_add_psbl_yn,b.col_add_psbl_yn,b.inpt_tbl_expn,b.qry_json),256)=x.digest);
SET @p_scope_count = (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_definitions_ok = IF(@p_file_count=4 AND @p_file_matches=4 AND @p_sheet_count=70 AND @p_sheet_matches=70
 AND @p_table_count=92 AND @p_table_matches=92 AND @p_scope_count=0,1,0);
-- Runtime files()와 같은 SCNRCASE/삭제N/서울 날짜 유효성 조건. 역할은 이름에 정확히 한 종류만 포함돼야 한다.
-- 이름 검사는 역할 식별용이며 구조 호환 증명이 아니다. §1의 실제 객체/구조 증빙을 @p_evidence로 참조한다.
SET @p_valid_files = (SELECT COUNT(*) FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no=@p_case
 AND del_yn='N' AND (file_vald_end_dt IS NULL OR file_vald_end_dt>=@p_today));
SET @p_file_ref_matches = (SELECT COUNT(*) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(
 n FOR ORDINALITY,role VARCHAR(20) PATH '$.role',file_no BIGINT PATH '$.fileNo',
 object_ref VARCHAR(2048) PATH '$.objectRef')) x
 JOIN JSON_TABLE(@p_expected_files,'$[*]' COLUMNS(role VARCHAR(20) PATH '$[0]')) e ON BINARY e.role=BINARY x.role
 JOIN tcm_filel f ON f.file_no=x.file_no AND f.file_rel_obj_spt_cd='SCNRCASE' AND f.file_rel_obj_dstg_no=@p_case
 WHERE f.del_yn='N' AND (f.file_vald_end_dt IS NULL OR f.file_vald_end_dt>=@p_today)
 AND JSON_TYPE(JSON_EXTRACT(@p_files,CONCAT('$[',x.n-1,'].fileNo')))='INTEGER' AND x.file_no>0
 AND BINARY f.strg_path_nm=BINARY x.object_ref AND CHAR_LENGTH(TRIM(x.object_ref))>0
 AND UPPER(f.org_file_nm) LIKE CONCAT('%',x.role,'%')
 AND ((UPPER(f.org_file_nm) LIKE '%DEMAND%')+(UPPER(f.org_file_nm) LIKE '%SUPPLY%')+
      (UPPER(f.org_file_nm) LIKE '%TRADE%')+(UPPER(f.org_file_nm) LIKE '%OUTPUT%'))=1);
SET @p_distinct_refs = (SELECT COUNT(DISTINCT file_no) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(file_no BIGINT PATH '$.fileNo')) x);
SET @p_distinct_roles = (SELECT COUNT(DISTINCT role) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(role VARCHAR(20) PATH '$.role')) x);
SET @p_files_ok = IF(@p_valid_files=4 AND @p_file_ref_matches=4 AND @p_distinct_refs=4 AND @p_distinct_roles=4,1,0);
SET @p_ok = IF(@p_inputs_ok=1 AND @p_root_ok=1 AND @p_active_count=0 AND @p_existing_links=0
 AND @p_existing_drafts=0 AND @p_case_ok=1 AND @p_definitions_ok=1 AND @p_files_ok=1,1,0);
SELECT @p_inputs_ok AS inputs_must_1,@p_root_ok AS exact_draft_must_1,@p_active_count AS active_must_0,
 @p_existing_links AS links_must_0,@p_existing_drafts AS drafts_must_0,@p_case_ok AS new_case_must_1,
 @p_file_count AS role_count_4,@p_file_matches AS role_matches_4,@p_sheet_count AS sheet_count_70,@p_sheet_matches AS sheet_matches_70,
 @p_table_count AS table_count_92,@p_table_matches AS table_matches_92,@p_scope_count AS scopes_0,
 @p_valid_files AS case_files_4,@p_file_ref_matches AS file_ref_matches_4,@p_files_ok AS files_must_1,
 IF(@p_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision;
-- STOP이면 즉시 ROLLBACK 후 종료. @p_ok를 수동으로 1로 바꾸지 않는다.

-- 4. 발행 → 새 Case 연결 (영구 DML은 이 UPDATE와 INSERT 두 문장뿐)
SET @p_publish_time = CURRENT_TIMESTAMP(6);
UPDATE tmd_mdl_inpttmpltm
SET tmplt_stat_cd='ACTIVE',aply_bgn_dtm=@p_publish_time,mod_usr_logn_id=@p_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @p_ok=1 AND tmplt_id=@p_tmplt AND BINARY mdl_spt_cd=BINARY @p_model AND tmplt_ver_no=@p_version
 AND BINARY tmplt_expn=BINARY @p_marker AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N';
-- ROW_COUNT가 아닌 저장된 정확한 대상·상태·시각·감사값으로 판정한다.
SET @p_published = (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm
 WHERE @p_ok=1 AND tmplt_id=@p_tmplt AND BINARY mdl_spt_cd=BINARY @p_model AND tmplt_ver_no=@p_version
 AND tmplt_stat_cd='ACTIVE' AND aply_bgn_dtm=@p_publish_time AND BINARY mod_usr_logn_id=BINARY @p_actor AND del_yn='N');
SELECT @p_published AS published_must_1;
INSERT INTO tmd_mdl_scnrcasetmpltd(case_id,tmplt_id,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT c.case_id,t.tmplt_id,@p_actor,CURRENT_TIMESTAMP,@p_actor,CURRENT_TIMESTAMP,'N'
FROM tmd_mdl_scnrcasem c JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id=@p_tmplt
WHERE @p_ok=1 AND @p_published=1 AND c.case_id=@p_case AND c.case_id NOT IN (103,105,106)
 AND c.del_yn='N' AND c.exec_type_cd IN ('INDEPENDENT','MI_BASE')
 AND t.tmplt_stat_cd='ACTIVE' AND t.aply_bgn_dtm=@p_publish_time AND t.del_yn='N' AND BINARY t.mdl_spt_cd=BINARY @p_model
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_scnrcasetmpltd r WHERE r.case_id=@p_case)
 AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inptdrftm d WHERE d.org_case_id=@p_case OR d.fnsv_case_id=@p_case OR d.tmplt_id=@p_tmplt);
SET @p_linked = (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd
 WHERE @p_ok=1 AND @p_published=1 AND case_id=@p_case AND tmplt_id=@p_tmplt AND del_yn='N'
 AND BINARY crt_usr_logn_id=BINARY @p_actor AND BINARY mod_usr_logn_id=BINARY @p_actor);

-- 5. 확정 전 실제 저장 상태 검증
SET @p_file_count = (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@p_tmplt);
SET @p_file_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_files,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', name VARCHAR(255) PATH '$[1]', ord INT PATH '$[2]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 WHERE BINARY f.file_nm=BINARY x.name AND f.file_ord=x.ord AND f.file_type_cd='XLSM' AND f.file_strt_hash_val IS NULL AND f.del_yn='N');
SET @p_sheet_count = (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_sheet_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_sheets,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', name VARCHAR(150) PATH '$[1]', ord INT PATH '$[2]', state VARCHAR(20) PATH '$[3]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.name
 WHERE s.sht_ord=x.ord AND s.sht_stat_cd=x.state AND s.del_yn='N');
SET @p_table_count = (SELECT COUNT(*) FROM tmd_mdl_inpttbld b JOIN tmd_mdl_inptshtd s USING(inpt_sht_id)
 JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_table_matches = (SELECT COUNT(*) FROM JSON_TABLE(@p_expected_tables,'$[*]' COLUMNS(
 role VARCHAR(20) PATH '$[0]', sheet VARCHAR(150) PATH '$[1]', name VARCHAR(100) PATH '$[2]', digest CHAR(64) PATH '$[3]')) x
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@p_tmplt AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
 JOIN tmd_mdl_inpttbld b ON b.inpt_sht_id=s.inpt_sht_id AND BINARY b.inpt_tbl_nm=BINARY x.name
 WHERE b.del_yn='N' AND s.sht_stat_cd='VISIBLE' AND b.qry_json IS NOT NULL AND JSON_VALID(b.qry_json)=1
 AND OCTET_LENGTH(b.qry_json)<=1048576
 AND SHA2(CONCAT_WS(CHAR(31),b.inpt_tbl_nm,b.tbl_ord,b.tbl_bgn_row_no,b.tbl_end_row_no,b.tbl_bgn_col_no,b.tbl_end_col_no,
 b.hdr_bgn_row_no,b.hdr_end_row_no,b.data_bgn_row_no,b.row_add_psbl_yn,b.col_add_psbl_yn,b.inpt_tbl_expn,b.qry_json),256)=x.digest);
SET @p_scope_count = (SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id)
 JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) JOIN tmd_mdl_inptfiled f USING(inpt_file_id) WHERE f.tmplt_id=@p_tmplt);
SET @p_definitions_ok = IF(@p_file_count=4 AND @p_file_matches=4 AND @p_sheet_count=70 AND @p_sheet_matches=70
 AND @p_table_count=92 AND @p_table_matches=92 AND @p_scope_count=0,1,0);
-- Runtime files()와 같은 SCNRCASE/삭제N/서울 날짜 유효성 조건. 역할은 이름에 정확히 한 종류만 포함돼야 한다.
-- 이름 검사는 역할 식별용이며 구조 호환 증명이 아니다. §1의 실제 객체/구조 증빙을 @p_evidence로 참조한다.
SET @p_valid_files = (SELECT COUNT(*) FROM tcm_filel WHERE file_rel_obj_spt_cd='SCNRCASE' AND file_rel_obj_dstg_no=@p_case
 AND del_yn='N' AND (file_vald_end_dt IS NULL OR file_vald_end_dt>=@p_today));
SET @p_file_ref_matches = (SELECT COUNT(*) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(
 n FOR ORDINALITY,role VARCHAR(20) PATH '$.role',file_no BIGINT PATH '$.fileNo',
 object_ref VARCHAR(2048) PATH '$.objectRef')) x
 JOIN JSON_TABLE(@p_expected_files,'$[*]' COLUMNS(role VARCHAR(20) PATH '$[0]')) e ON BINARY e.role=BINARY x.role
 JOIN tcm_filel f ON f.file_no=x.file_no AND f.file_rel_obj_spt_cd='SCNRCASE' AND f.file_rel_obj_dstg_no=@p_case
 WHERE f.del_yn='N' AND (f.file_vald_end_dt IS NULL OR f.file_vald_end_dt>=@p_today)
 AND JSON_TYPE(JSON_EXTRACT(@p_files,CONCAT('$[',x.n-1,'].fileNo')))='INTEGER' AND x.file_no>0
 AND BINARY f.strg_path_nm=BINARY x.object_ref AND CHAR_LENGTH(TRIM(x.object_ref))>0
 AND UPPER(f.org_file_nm) LIKE CONCAT('%',x.role,'%')
 AND ((UPPER(f.org_file_nm) LIKE '%DEMAND%')+(UPPER(f.org_file_nm) LIKE '%SUPPLY%')+
      (UPPER(f.org_file_nm) LIKE '%TRADE%')+(UPPER(f.org_file_nm) LIKE '%OUTPUT%'))=1);
SET @p_distinct_refs = (SELECT COUNT(DISTINCT file_no) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(file_no BIGINT PATH '$.fileNo')) x);
SET @p_distinct_roles = (SELECT COUNT(DISTINCT role) FROM JSON_TABLE(COALESCE(@p_files,'[]'),'$[*]' COLUMNS(role VARCHAR(20) PATH '$.role')) x);
SET @p_files_ok = IF(@p_valid_files=4 AND @p_file_ref_matches=4 AND @p_distinct_refs=4 AND @p_distinct_roles=4,1,0);
SET @p_verified = IF(@p_ok=1 AND @p_published=1 AND @p_linked=1 AND @p_definitions_ok=1 AND @p_files_ok=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@p_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@p_tmplt)=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inptdrftm WHERE tmplt_id=@p_tmplt OR org_case_id=@p_case OR fnsv_case_id=@p_case)=0,1,0);
SELECT r.case_id,t.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,t.aply_bgn_dtm,r.del_yn,
 t.mod_usr_logn_id,r.crt_usr_logn_id,
 CONCAT('template-v1:',t.tmplt_id,':',t.tmplt_ver_no) AS internal_template_ref,
 CONCAT('catalog-v1:',t.tmplt_id) AS internal_catalog_ref
FROM tmd_mdl_scnrcasetmpltd r JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id=r.tmplt_id
WHERE r.case_id=@p_case AND r.del_yn='N' AND t.del_yn='N' AND t.mdl_spt_cd=@p_model AND t.tmplt_stat_cd IN ('ACTIVE','DEPRECATED');
-- 위 결과 정확히1행/ACTIVE/7/version1. 내부 참조로 공개 sourceVersion을 수동 생성하지 않는다.
SELECT f.mdl_file_cd,COUNT(DISTINCT s.inpt_sht_id) AS sheets,COUNT(b.inpt_tbl_id) AS tables
FROM tmd_mdl_inptfiled f LEFT JOIN tmd_mdl_inptshtd s USING(inpt_file_id)
LEFT JOIN tmd_mdl_inpttbld b USING(inpt_sht_id) WHERE f.tmplt_id=@p_tmplt GROUP BY f.mdl_file_cd,f.file_ord ORDER BY f.file_ord;
-- DEMAND 25/40, SUPPLY 23/15, TRADE 17/32, OUTPUT 5/5. metadata70과 응답 표시 시트 수는 다를 수 있다.
SELECT case_id,tmplt_id,del_yn FROM tmd_mdl_scnrcasetmpltd WHERE case_id IN (103,105,106) ORDER BY case_id;
SELECT drft_id,org_case_id,tmplt_id,drft_stat_cd,rvsn_no,actv_prcs_id,fnsv_case_id,del_yn
FROM tmd_mdl_inptdrftm WHERE drft_id='334df4d8-26a4-4d45-bccb-7b6e5445bf81';
SELECT @p_verified AS all_checks_must_1,
 IF(@p_verified=1,'REVIEW_RESULTS_THEN_EXPLICIT_COMMIT_OR_ROLLBACK','STOP_AND_ROLLBACK') AS decision;
-- 보호 대상 조회는 §1과 대조. 이 SQL은 보호 대상/파일/Draft/하위 정의를 쓰지 않는다.

-- 6. 사용자의 최종 결정 — 기본은 ROLLBACK
-- 첫 실행은 아래 ROLLBACK까지. 확정하려면 §1부터 새 검증 transaction을 열고 §5 결과를 직접 확인한다.
-- 모든 조건=1/예상 건수/보호 대상 일치일 때만 아래 마지막 ROLLBACK 대신 같은 연결에서 COMMIT;을 직접 실행한다.
-- 오류/STOP/확신 없음이면 ROLLBACK. 세션을 열린 채 두거나 다른 연결에서 COMMIT하지 않는다.
-- COMMIT; -- 사용자가 선택할 때만. 파일 전체의 기본 실행은 확정되지 않는다.
ROLLBACK;
-- ROLLBACK 후 @p_verified=1이 남아도 DB 확정 성공이 아니다. 새 연결의 저장 상태가 근거다.
-- COMMIT 결과가 불명확하면 §7만 확인한다. 발행/연결 DML을 다시 실행하지 않는다.

-- 7. 종료 후 새 연결에서 사용자 확인 (이 SELECT만 실행; §1의 변수 초기화/§3 DML 재실행 금지)
-- 별도 연결에서 실제 master 선택 후 @p_case에 이번 새 Case를 입력하고 조회한다.
-- SELECT DATABASE();
-- SET @p_case = NULL; -- 실제 이번 Case ID 입력
SELECT t.tmplt_id,t.mdl_spt_cd,t.tmplt_ver_no,t.tmplt_stat_cd,t.aply_bgn_dtm,t.del_yn,
 (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd r WHERE r.tmplt_id=t.tmplt_id) AS template_link_count,
 (SELECT COUNT(*) FROM tmd_mdl_scnrcasetmpltd r WHERE r.case_id=@p_case AND r.tmplt_id=t.tmplt_id AND r.del_yn='N') AS exact_case_link_count
FROM tmd_mdl_inpttmpltm t WHERE t.tmplt_id=7 AND t.mdl_spt_cd='DRAFT_C_STRUCTURE' AND t.tmplt_ver_no=1;
-- COMMIT했다면 ACTIVE/적용시각있음/N/연결1/정확연결1. ROLLBACK했다면 원래 DRAFT/NULL/N/0/0.
-- 확정 후 기존 하위 정의는 불변. 추가/수정/보류 해소는 새 버전에서 검토한다.
SELECT DATABASE(), VERSION();
SELECT tmplt_id, mdl_spt_cd, tmplt_ver_no,
       tmplt_stat_cd, aply_bgn_dtm, del_yn
FROM tmd_mdl_inpttmpltm
WHERE tmplt_id=7 OR mdl_spt_cd='DRAFT_C_STRUCTURE';


SELECT
  (SELECT COUNT(*) FROM midb.tmd_mdl_scnrcasetmpltd
   WHERE tmplt_id=7) AS case_links,
  (SELECT COUNT(*) FROM midb.tmd_mdl_inptdrftm
   WHERE tmplt_id=7) AS drafts;

SELECT case_id, case_stat_cd, exec_type_cd, del_yn,
       crt_usr_logn_id, last_run_id
FROM midb.tmd_mdl_scnrcasem
WHERE case_id=107;

SELECT file_no, org_file_nm, del_yn, file_vald_end_dt,
       CASE WHEN strg_path_nm IS NOT NULL
                  AND TRIM(strg_path_nm) <> ''
            THEN 1 ELSE 0 END AS path_present
FROM midb.tcm_filel
WHERE file_rel_obj_spt_cd='SCNRCASE'
  AND file_rel_obj_dstg_no=107
ORDER BY file_no;

SELECT
  (SELECT COUNT(*) FROM midb.tmd_mdl_scnrcasetmpltd
   WHERE case_id=107) AS template_links,
  (SELECT COUNT(*) FROM midb.tmd_mdl_inptdrftm
   WHERE org_case_id=107 OR fnsv_case_id=107) AS drafts;
