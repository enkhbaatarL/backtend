-- Document Control
-- status: Draft / USER REVIEW AND MANUAL APPLY ONLY / NOT EXECUTED
-- canonical for: 이 baseline의 구조 데이터 등록·검증 SQL 초안 (정책·schema 정본 아님)
-- owner: BE / DBA; date: 2026-10-19; last synced: 2026-10-19
-- source: docs/references/mi-model-input-baseline-2026-10-07-manifest.json
-- depends on: docs/model-input-draft-read-ddl-draft.sql §2~3 (실제 SHOW CREATE 대조 필요)
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-erd-ddl-draft.md §6.2, §10B
-- depends on: docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md §3.3, 규칙3/39~44/49~58, §6.1
-- evidence/pending: docs/references/mi-model-input-catalog-structure-2026-10-19.md
-- supersedes: 없음. Case103 smoke SQL/기존 ACTIVE·DEPRECATED 정의를 대체하지 않는다.
-- open questions: 보류 7표·표별 쓰기/계산 정책·구조 hash 판별 방식·실DB 검증.
-- baselineId=2026-10-07-delivery-261006-v8-v6
-- 목적: 새 DRAFT 양식1개, 역할4개, 유지 시트70개, 확인된 조회 표92개만 INSERT.
-- 기존 Case/파일 metadata/Draft/Case-template/수정범위는 쓰지 않는다. UPDATE/DELETE/발행/DDL 없음.
-- file_strt_hash_val=NULL: 내용 SHA-256을 미정인 구조 hash 대신 저장하지 않는다.
-- capabilities/field policy INCOMPLETE는 미확정이며 쓰기 허용이 아니다.
-- LNG Summary는 확정 read-only이므로 NOT_ALLOWED. 수정범위 row는0개다.
-- 이 DRAFT는 부분 구조 초안이다. 실제 등록 성공도 ACTIVE 발행/전체 편집 가능을 뜻하지 않는다.
-- 실행 규칙: master의 MySQL8.0.16+ / InnoDB / CHECK·FK 유지 / 같은 연결 / stop-on-error / 한 등록자.
-- 별도 새 DB 연결을 사용한다. START TRANSACTION이 다른 미commit 작업을 확정하지 않도록 한다.
-- 어떤 SQL 오류·건수 불일치·부모 누락이라도 즉시 ROLLBACK. --force/오류 후 계속 실행 금지.
-- 파일 전체의 기본 끝은 ROLLBACK이다. 검증 결과를 읽고 그때만 마지막 결정을 COMMIT으로 교체한다.

-- 1. 사전 확인 (사용자 실행; Codex는 실행하지 않음)
-- 세 NULL은 실제 설정의 model-code / 미사용 버전(1..65535) / 감사 login으로 명시 지정한다.
-- DRAFT_API_SMOKE나 기존 양식 모델/버전을 임의 재사용하지 않는다. MAX(version)+1 자동 선택 없음.
SET @c_model = NULL;
SET @c_version = NULL;
SET @c_actor = NULL;
SET @c_marker = 'structure-c-20261019; baselineId=2026-10-07-delivery-261006-v8-v6; partial=92/99; NOT_PUBLISHED';
SET @c_tmplt_id = NULL, @c_created = 0, @c_ok = 0;
SELECT DATABASE() AS master_schema, VERSION() AS mysql_version, @@sql_mode, @@autocommit, @@max_allowed_packet;
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_inptmodscopd;
-- 부모 ID는 AUTO_INCREMENT를 사용한다. crt_*/mod_* VARCHAR(50)·DATETIME 초, qry_json LONGTEXT/ck06을 대조한다。
-- 감사 컬럼 차이는 기존 audit DDL 정본으로 먼저 해결한다. 이 파일에는 ALTER가 없다.
SELECT tmplt_id, mdl_spt_cd, tmplt_ver_no, tmplt_ver_nm, tmplt_stat_cd, del_yn, tmplt_expn
FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@c_model ORDER BY tmplt_ver_no;
-- 아래 결과는0건이어야 한다. soft delete된 버전과 동일 등록도 재사용하지 않는다.
SELECT tmplt_id, tmplt_ver_no, tmplt_stat_cd, del_yn FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd=@c_model AND (tmplt_ver_no=@c_version OR tmplt_expn=@c_marker);
-- 정규화 파일을 manifest SHA-256으로 다시 확인하고 대상DB·모델코드·DDL 호환·미사용version을 확인한다.
-- 시트 metadata70개와 표시표92개를 구분한다. 개발 전용 시트는 등록하지 않는다.

-- 2. 등록 (전 단계를 같은 transaction에서 실행; ERROR 발생 시 ROLLBACK 후 종료)
START TRANSACTION;
INSERT INTO tmd_mdl_inpttmpltm
  (mdl_spt_cd,tmplt_ver_no,tmplt_ver_nm,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn,
   crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT @c_model,@c_version,'2026-10-07 v8-v6 structure DRAFT','DRAFT',NULL,@c_marker,
       @c_actor,CURRENT_TIMESTAMP,@c_actor,CURRENT_TIMESTAMP,'N'
WHERE @c_model IS NOT NULL AND CHAR_LENGTH(TRIM(@c_model)) BETWEEN 1 AND 20
  AND @c_model <> 'DRAFT_API_SMOKE'
  AND @c_version BETWEEN 1 AND 65535 AND @c_version=FLOOR(@c_version)
  AND @c_actor IS NOT NULL AND CHAR_LENGTH(TRIM(@c_actor)) BETWEEN 1 AND 50
  AND NOT EXISTS (SELECT 1 FROM tmd_mdl_inpttmpltm
                  WHERE mdl_spt_cd=@c_model AND (tmplt_ver_no=@c_version OR tmplt_expn=@c_marker));
SET @c_created=ROW_COUNT();
-- 이번에1건 생성한 자연키·marker·DRAFT·감사값으로 실재 부모를 조회한다. 오래된 LAST_INSERT_ID에 기대지 않는다.
SET @c_tmplt_id=(SELECT tmplt_id FROM tmd_mdl_inpttmpltm
  WHERE @c_created=1 AND mdl_spt_cd=@c_model AND tmplt_ver_no=@c_version AND tmplt_expn=@c_marker
    AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND del_yn='N'
    AND crt_usr_logn_id=@c_actor AND mod_usr_logn_id=@c_actor);
SET @c_ok=IF(@c_created=1 AND @c_tmplt_id IS NOT NULL,1,0);
SELECT @c_created AS expected_1_root, @c_tmplt_id AS actual_parent_id, @c_ok AS must_be_1;
-- 동일version/marker가 있는 재실행은 root0건·부모NULL로 하위 INSERT도0건이 된다.
-- 중간 재개/upsert는 하지 않는다. 미commit 중간 실패는 ROLLBACK 후 처음부터 실행한다.

INSERT INTO tmd_mdl_inptfiled
 (tmplt_id,mdl_file_cd,file_nm,file_ord,file_type_cd,file_strt_hash_val,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT p.tmplt_id,x.role,x.filename,x.ord,'XLSM',NULL,@c_actor,CURRENT_TIMESTAMP,@c_actor,CURRENT_TIMESTAMP,'N'
FROM (
SELECT 'DEMAND' AS role,'DEMAND-normalized.xlsm' AS filename,1 AS ord
UNION ALL
SELECT 'SUPPLY','SUPPLY-normalized.xlsm',2
UNION ALL
SELECT 'TRADE','TRADE-normalized.xlsm',3
UNION ALL
SELECT 'OUTPUT','OUTPUT-normalized.xlsm',4
) x JOIN tmd_mdl_inpttmpltm p ON p.tmplt_id=@c_tmplt_id AND p.tmplt_stat_cd='DRAFT' AND p.tmplt_expn=@c_marker AND p.del_yn='N'
WHERE @c_ok=1;
SET @c_ok=IF(@c_ok=1 AND ROW_COUNT()=4 AND (SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@c_tmplt_id)=4,1,0);
SELECT @c_ok AS roles_must_be_1;

INSERT INTO tmd_mdl_inptshtd
 (inpt_file_id,sht_nm,sht_ord,sht_stat_cd,crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT f.inpt_file_id,x.sheet,x.ord,x.state,@c_actor,CURRENT_TIMESTAMP,@c_actor,CURRENT_TIMESTAMP,'N'
FROM (
SELECT 'DEMAND' AS role,'Input Adjustment ->' AS sheet,1 AS ord,'VISIBLE' AS state
UNION ALL
SELECT 'DEMAND','GDPPopulationAdjustment',2,'VISIBLE'
UNION ALL
SELECT 'DEMAND','ElectrificationRateAdjustment',3,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment',4,'VISIBLE'
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment',5,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherAdjustment',6,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMIAdjustment',7,'VISIBLE'
UNION ALL
SELECT 'DEMAND','HydrogenAdjustment',8,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment',9,'VISIBLE'
UNION ALL
SELECT 'DEMAND','Scenario Tracker',10,'HIDDEN'
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite',11,'VISIBLE'
UNION ALL
SELECT 'DEMAND','기준정보 ->',12,'VISIBLE'
UNION ALL
SELECT 'DEMAND','BCInput',13,'VISIBLE'
UNION ALL
SELECT 'DEMAND','MacroParametersLogicInput',14,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMIInput',15,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMILogicInput',16,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherLogicInput',17,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherInput',18,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringInput',19,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringLogicInput',20,'VISIBLE'
UNION ALL
SELECT 'DEMAND','HydrogenInput',21,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNG Demand',22,'VISIBLE'
UNION ALL
SELECT 'DEMAND','General',23,'VISIBLE'
UNION ALL
SELECT 'DEMAND','Sheet2_DISCARDAFTUSE',24,'HIDDEN'
UNION ALL
SELECT 'DEMAND','CubeDump_DISCARDAFTUSE',25,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','Input Upstream ->',1,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','ProductionAdjustment',2,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','Input Midstream ->',3,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','{USA}Hurricanes',4,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment',5,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment',6,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','RegasAdjustment',7,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','PipelineAdjustment',8,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCProduction',9,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCHurricanes',10,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCLiquefaction',11,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCRegasification',12,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCPipeline',13,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LNG Summary',14,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment_backup',15,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment_backup',16,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCLiquefaction_backup',17,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','{USA}RigsLogic',18,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','{China}PolicyLogic',19,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','{USA}HurricanesLogic',20,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','RegasLogic',21,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','PipelineLogic',22,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','MonthlyAdjProduction',23,'HIDDEN'
UNION ALL
SELECT 'TRADE','LNG_TradeRestrictionInput',1,'VISIBLE'
UNION ALL
SELECT 'TRADE','PNG_TradeRestrictionInput',2,'VISIBLE'
UNION ALL
SELECT 'TRADE','Sheet1',3,'HIDDEN'
UNION ALL
SELECT 'TRADE','MidstreamContractInput',4,'VISIBLE'
UNION ALL
SELECT 'TRADE','ShippingCostInput',5,'VISIBLE'
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput',6,'VISIBLE'
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput',7,'VISIBLE'
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput',8,'VISIBLE'
UNION ALL
SELECT 'TRADE','CostInput',9,'VISIBLE'
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput',10,'VISIBLE'
UNION ALL
SELECT 'TRADE','LNGExportRestictionInput',11,'VISIBLE'
UNION ALL
SELECT 'TRADE','backupLNG_TradeRestrictionInput',12,'VISIBLE'
UNION ALL
SELECT 'TRADE','backup_MidstreamContractInput',13,'VISIBLE'
UNION ALL
SELECT 'TRADE','Industry research',14,'HIDDEN'
UNION ALL
SELECT 'TRADE','net exporter',15,'HIDDEN'
UNION ALL
SELECT 'TRADE','pipeline cap',16,'HIDDEN'
UNION ALL
SELECT 'TRADE','pipeline contract',17,'HIDDEN'
UNION ALL
SELECT 'OUTPUT','USCostofSupply',1,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','BCPrices',2,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','PremiumCalc',3,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','AnnualPriceCalc',4,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','PriceSeasonality',5,'VISIBLE'
) x JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@c_tmplt_id AND f.mdl_file_cd=x.role AND f.del_yn='N'
JOIN tmd_mdl_inpttmpltm p ON p.tmplt_id=f.tmplt_id AND p.tmplt_stat_cd='DRAFT' AND p.tmplt_expn=@c_marker AND p.del_yn='N'
WHERE @c_ok=1;
SET @c_ok=IF(@c_ok=1 AND ROW_COUNT()=70 AND (SELECT COUNT(*) FROM tmd_mdl_inptshtd s JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id WHERE f.tmplt_id=@c_tmplt_id)=70,1,0);
SELECT @c_ok AS sheets_must_be_1;

-- 검증한 schemaVersion1 정의92개. 행·열은 Excel의1-based이며 fieldKey는 표 안에서 유일하다.
-- controls/guide/displayUpdateRules는 미작성이다. 빈 배열은 업무 규칙이 없거나 발행이 승인됐다는 뜻이 아니다.

-- DEMAND/GDPPopulationAdjustment/GDP Adjustment; source SUMMARY row 11
SET @c_q001='{"schemaVersion":1,"columns":[2,3,5,6],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[2,3,5,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"change","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/GDPPopulationAdjustment/Population Adjustment; source SUMMARY row 12
SET @c_q002='{"schemaVersion":1,"columns":[10,11,13],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[10,11,13],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/ElectrificationRateAdjustment/Electrification Rate Adjustment; source SUMMARY row 13
SET @c_q003='{"schemaVersion":1,"columns":[2,3,4,6],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[2,3,4,6],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PowerMixAdjustment/Annual Power Mix Adjustment; source SUMMARY row 14
SET @c_q004='{"schemaVersion":1,"columns":[2,3,4,6],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7989}],"identityColumns":[2,3,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"energySource","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"powerMix","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PowerMixAdjustment/Monthly Power Mix Adjustment; source SUMMARY row 15
SET @c_q005='{"schemaVersion":1,"columns":[10,11,12,13,15],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7989}],"identityColumns":[10,11,12,13,15],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/CoalGasAdjustment/Coal Futures Price Adjustment; source SUMMARY row 16
SET @c_q006='{"schemaVersion":1,"columns":[2,3,5],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[2,3,5],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/CoalGasAdjustment/Gas Futures Price Adjustment; source SUMMARY row 17
SET @c_q007='{"schemaVersion":1,"columns":[8,9,11],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[8,9,11],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherAdjustment/Country HDD and CDD Adjustment; source SUMMARY row 18
SET @c_q008='{"schemaVersion":1,"columns":[2,3,4,5,7],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7997}],"identityColumns":[2,3,4,5,7],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherAdjustment/Country Temperature Adjustment; source SUMMARY row 19
SET @c_q009='{"schemaVersion":1,"columns":[11,12,13,15],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7997}],"identityColumns":[11,12,13,15],"fields":[{"fieldKey":"year","columnNumber":11,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":12,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":13,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"temperatureC","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherAdjustment/Regional HDD and CDD Adjustment; source SUMMARY row 20
SET @c_q010='{"schemaVersion":1,"columns":[20,21,22,23,24],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7997}],"identityColumns":[20,21,22,23,24],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherAdjustment/Regional Temperature Change Adjustment; source SUMMARY row 21
SET @c_q011='{"schemaVersion":1,"columns":[28,29,30,31],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":7997}],"identityColumns":[28,29,30,31],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PMIAdjustment/Monthly PMI Adjustment; source SUMMARY row 22
SET @c_q012='{"schemaVersion":1,"columns":[2,3,4,6],"headerRows":[6],"bodyRanges":[{"startRow":13,"endRow":8000}],"identityColumns":[2,3,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":3,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"pmi","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PMIAdjustment/PMI Reference Country Adjustment; source SUMMARY row 23
SET @c_q013='{"schemaVersion":1,"columns":[10,12],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":8000}],"identityColumns":[10,12],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/HydrogenAdjustment/Hydrogen Production Adjustment; source SUMMARY row 24
SET @c_q014='{"schemaVersion":1,"columns":[2,3,5],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":8000}],"identityColumns":[2,3,5],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/LNGBunkeringAdjustment/LNG Bunkering Vessel Count Adjustment; source SUMMARY row 25
SET @c_q015='{"schemaVersion":1,"columns":[2,3,5],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[2,3,5],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/LNGBunkeringAdjustment/LNG Bunkering Fuel Consumption Adjustment; source SUMMARY row 26
SET @c_q016='{"schemaVersion":1,"columns":[8,9,11],"headerRows":[6],"bodyRanges":[{"startRow":12,"endRow":2000}],"identityColumns":[8,9,11],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/DemandOutputOverwrite/Demand Model Manual Overwrite Sector Input; source SUMMARY row 27
SET @c_q017='{"schemaVersion":1,"columns":[4,5,6,7,9],"headerRows":[6,7],"bodyRanges":[{"startRow":8,"endRow":507}],"identityColumns":[4,5,6,7,9],"fields":[{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":5,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":6,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"demandSubSector","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/DemandOutputOverwrite/Demand Model Manual Overwrite Monthly Input; source SUMMARY row 28
SET @c_q018='{"schemaVersion":1,"columns":[13,14,15,17],"headerRows":[6,7],"bodyRanges":[{"startRow":8,"endRow":507}],"identityColumns":[13,14,15,17],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/RE Base Case Gas Demand Data by Sector; source SUMMARY row 32
SET @c_q019='{"schemaVersion":1,"columns":[3,4,5,6,7,8,9,10,11,12,13,14],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":14493}],"identityColumns":[3,4,5,6,7,8,9,10,11,12,13,14],"fields":[{"fieldKey":"continent","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"demandCategory","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"demandSector","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":8,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"gasDemandBaseCaseMillionTonnesLNG","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngBunkeringMillionTonnesLNG","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nonLNGBunkeringMillionTonnesLNG","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"hydrogenDemandBaseCaseMillionTonnesLNG","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nonHydrogenDemandBaseCaseMillionTonnesLNG","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"combinedGasDemandBaseCaseMillionTonnesLNG","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/RE Base Case GDP and Population Data; source SUMMARY row 33
SET @c_q020='{"schemaVersion":1,"columns":[17,18,19,20,21,22],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":2368}],"identityColumns":[17,18,19,20,21,22],"fields":[{"fieldKey":"year","columnNumber":17,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"continent","columnNumber":18,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":19,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":20,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"gdp","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"populationThousands","columnNumber":22,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Power Generation Data; source SUMMARY row 34
SET @c_q021='{"schemaVersion":1,"columns":[26,27,28,29,30,34,35],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":100803}],"identityColumns":[26,27,28,29,30,34,35],"fields":[{"fieldKey":"country","columnNumber":26,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":27,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":28,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"energySourceUse","columnNumber":29,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"sumOfGenerationTWh","columnNumber":30,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"adjustedGasDemandMillionTonnesLNG","columnNumber":34,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"gasConsumptionRateMillionTonnesLNGTWh","columnNumber":35,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Power Generation Utilisation Rate; source SUMMARY row 35
SET @c_q022='{"schemaVersion":1,"columns":[37,38,39,40],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":1125}],"identityColumns":[37,38,39,40],"fields":[{"fieldKey":"continent","columnNumber":37,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":38,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"remappedEnergySource","columnNumber":39,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAN","columnNumber":40,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Car Sales for Electrification Input; source SUMMARY row 36
SET @c_q023='{"schemaVersion":1,"columns":[43,44,45,46,47],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":13548}],"identityColumns":[43,44,45,46,47],"fields":[{"fieldKey":"country","columnNumber":43,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":44,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"vehicleType","columnNumber":45,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"driveTrain","columnNumber":46,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"count","columnNumber":47,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Base Case LNG Bunkering Demand; source SUMMARY row 37
SET @c_q024='{"schemaVersion":1,"columns":[49,50,51],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":252}],"identityColumns":[49,50,51],"fields":[{"fieldKey":"country","columnNumber":49,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":50,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngBunkeringMillionTonnesLNG","columnNumber":51,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Base Case Hydrogen Demand; source SUMMARY row 38
SET @c_q025='{"schemaVersion":1,"columns":[54,55,56],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":290}],"identityColumns":[54,55,56],"fields":[{"fieldKey":"country","columnNumber":54,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"year","columnNumber":55,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"hydrogenDemandBaseCaseMillionTonnesLNG","columnNumber":56,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/BCInput/Coal and Gas Futures Pricing; source SUMMARY row 39
SET @c_q026='{"schemaVersion":1,"columns":[59,60,61,62,63],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":28}],"identityColumns":[59,60,61,62,63],"fields":[{"fieldKey":"date","columnNumber":59,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"newcastleCoal","columnNumber":60,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"rotterdamCoal","columnNumber":61,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"jkm","columnNumber":62,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"ttf","columnNumber":63,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/MacroParametersLogicInput/Selected Macro Parameter Intensities; source SUMMARY row 41
SET @c_q027='{"schemaVersion":1,"columns":[9,10,11,12,13,14,15,16],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":176}],"identityColumns":[9,10,11,12,13,14,15,16],"fields":[{"fieldKey":"intensityType","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"continent","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":11,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":12,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"changeInNonPowerGasDemandPerChangeInGDPPopulation","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"changeInPowerGenerationPerChangeInGDPPopulation","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nonPowerInterceptBCM","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"powerInterceptTWh","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PMIInput/Base Case PMI Input; source SUMMARY row 44
SET @c_q028='{"schemaVersion":1,"columns":[22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":1016}],"identityColumns":[22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37],"fields":[{"fieldKey":"continent","columnNumber":22,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":23,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":24,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":25,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnZ","columnNumber":26,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAA","columnNumber":27,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAB","columnNumber":28,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAC","columnNumber":29,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAD","columnNumber":30,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAE","columnNumber":31,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAF","columnNumber":32,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAG","columnNumber":33,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PMILogicInput/User-Adjusted PMI Intensity Parameter; source SUMMARY row 46
SET @c_q029='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[2,3,4,5],"fields":[{"fieldKey":"continent","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/PMILogicInput/Base Case PMI Intensity Parameter; source SUMMARY row 47
SET @c_q030='{"schemaVersion":1,"columns":[7,8,9,10],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[7,8,9,10],"fields":[{"fieldKey":"continent","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"intensity","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherLogicInput/User-Adjusted HDD-Gas Intensity Input; source SUMMARY row 49
SET @c_q031='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[4,5],"fields":[{"fieldKey":"continent","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherLogicInput/User-Adjusted CDD-Gas Intensity Input; source SUMMARY row 50
SET @c_q032='{"schemaVersion":1,"columns":[7,8,9,10],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[9,10],"fields":[{"fieldKey":"continent","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherLogicInput/Base Case HDD-Gas Intensity Input; source SUMMARY row 51
SET @c_q033='{"schemaVersion":1,"columns":[12,13,14,15],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[14,15],"fields":[{"fieldKey":"continent","columnNumber":12,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":13,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":14,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"hddGasIntensity","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherLogicInput/Base Case CDD-Gas Intensity Input; source SUMMARY row 52
SET @c_q034='{"schemaVersion":1,"columns":[17,18,19,20],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":92}],"identityColumns":[19,20],"fields":[{"fieldKey":"continent","columnNumber":17,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":18,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":19,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"cddGasIntensity","columnNumber":20,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherInput/Base Case HDD Input; source SUMMARY row 61
SET @c_q035='{"schemaVersion":1,"columns":[74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":1016}],"identityColumns":[74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89],"fields":[{"fieldKey":"continent","columnNumber":74,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":75,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":76,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":77,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBZ","columnNumber":78,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCA","columnNumber":79,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCB","columnNumber":80,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCC","columnNumber":81,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCD","columnNumber":82,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCE","columnNumber":83,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCF","columnNumber":84,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCG","columnNumber":85,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherInput/Base Case CDD Input; source SUMMARY row 62
SET @c_q036='{"schemaVersion":1,"columns":[92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":1016}],"identityColumns":[92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107],"fields":[{"fieldKey":"continent","columnNumber":92,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":93,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":94,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":95,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCR","columnNumber":96,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCS","columnNumber":97,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCT","columnNumber":98,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCU","columnNumber":99,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCV","columnNumber":100,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCW","columnNumber":101,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCX","columnNumber":102,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCY","columnNumber":103,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/WeatherInput/Base Case Temperature; source SUMMARY row 65
SET @c_q037='{"schemaVersion":1,"columns":[139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":1016}],"identityColumns":[139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154],"fields":[{"fieldKey":"continent","columnNumber":139,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":140,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":141,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":142,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnEM","columnNumber":143,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnEN","columnNumber":144,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnEO","columnNumber":145,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnEP","columnNumber":146,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnEQ","columnNumber":147,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnER","columnNumber":148,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnES","columnNumber":149,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnET","columnNumber":150,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/LNGBunkeringInput/Base Case LNG Bunkering Input; source SUMMARY row 67
SET @c_q038='{"schemaVersion":1,"columns":[8,9,10,11],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":56}],"identityColumns":[8,9,10,11],"fields":[{"fieldKey":"year","columnNumber":8,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"vesselType","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"noOfVessels","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fuelConsumptionRateByVesselMCMYear","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/LNGBunkeringLogicInput/LNG Bunkering Share by Country; source SUMMARY row 69
SET @c_q039='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17],"headerRows":[6,7],"bodyRanges":[{"startRow":8,"endRow":98}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17],"fields":[{"fieldKey":"continent","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnG","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- DEMAND/HydrogenInput/Base Case Hydrogen Production Input; source SUMMARY row 71
SET @c_q040='{"schemaVersion":1,"columns":[6,7,8],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":736}],"identityColumns":[6,7,8],"fields":[{"fieldKey":"year","columnNumber":6,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"productionKtonnes","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/ProductionAdjustment/PNG Production Adjustment; source SUMMARY row 11
SET @c_q041='{"schemaVersion":1,"columns":[9,10,11,13],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":200}],"identityColumns":[9,10,11,13],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/{USA}Hurricanes/Monthly Hurricane Adjustment; source SUMMARY row 14
SET @c_q042='{"schemaVersion":1,"columns":[2,3,4],"headerRows":[7],"bodyRanges":[{"startRow":13,"endRow":200}],"identityColumns":[2,3,4],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/LiqUtilAdjustment/Monthly LNG Train Utilization; source SUMMARY row 15
SET @c_q043='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90],"headerRows":[7,8,10],"bodyRanges":[{"startRow":11,"endRow":407}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90],"fields":[{"fieldKey":"country","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlant","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlantTrain","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lifeCycleCategory","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nameplateCapacityMTPA","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnG","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnR","columnNumber":18,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnS","columnNumber":19,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnT","columnNumber":20,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnU","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnV","columnNumber":22,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnW","columnNumber":23,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnX","columnNumber":24,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnY","columnNumber":25,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnZ","columnNumber":26,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAA","columnNumber":27,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAB","columnNumber":28,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAC","columnNumber":29,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAD","columnNumber":30,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAE","columnNumber":31,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAF","columnNumber":32,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAG","columnNumber":33,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAH","columnNumber":34,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAI","columnNumber":35,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAJ","columnNumber":36,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAK","columnNumber":37,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAL","columnNumber":38,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAM","columnNumber":39,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAN","columnNumber":40,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAO","columnNumber":41,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAP","columnNumber":42,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAQ","columnNumber":43,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAR","columnNumber":44,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAS","columnNumber":45,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAT","columnNumber":46,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAU","columnNumber":47,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAV","columnNumber":48,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAW","columnNumber":49,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAX","columnNumber":50,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAY","columnNumber":51,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAZ","columnNumber":52,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBA","columnNumber":53,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBB","columnNumber":54,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBC","columnNumber":55,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBD","columnNumber":56,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBE","columnNumber":57,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBF","columnNumber":58,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBG","columnNumber":59,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBH","columnNumber":60,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBI","columnNumber":61,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBJ","columnNumber":62,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBK","columnNumber":63,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBL","columnNumber":64,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBM","columnNumber":65,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBN","columnNumber":66,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBO","columnNumber":67,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBP","columnNumber":68,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBQ","columnNumber":69,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBR","columnNumber":70,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBS","columnNumber":71,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBT","columnNumber":72,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBU","columnNumber":73,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBV","columnNumber":74,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBW","columnNumber":75,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBX","columnNumber":76,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBY","columnNumber":77,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBZ","columnNumber":78,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCA","columnNumber":79,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCB","columnNumber":80,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCC","columnNumber":81,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCD","columnNumber":82,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCE","columnNumber":83,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCF","columnNumber":84,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCG","columnNumber":85,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCH","columnNumber":86,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCI","columnNumber":87,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCJ","columnNumber":88,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCK","columnNumber":89,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCL","columnNumber":90,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/LiqProdAdjustment/Monthly LNG Train Production; source SUMMARY row 16
SET @c_q044='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90],"headerRows":[7,8,10],"bodyRanges":[{"startRow":11,"endRow":407}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90],"fields":[{"fieldKey":"country","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlant","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlantTrain","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lifeCycleCategory","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nameplateCapacityMTPA","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/RegasAdjustment/Monthly Regas Capacity Adjustment; source SUMMARY row 17
SET @c_q045='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115],"headerRows":[8,9,10],"bodyRanges":[{"startRow":11,"endRow":382}],"identityColumns":[8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],"fields":[{"fieldKey":"continent","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"regasificationPlant","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"nameplateCapacityMTPA","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnR","columnNumber":18,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnS","columnNumber":19,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnT","columnNumber":20,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnU","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnV","columnNumber":22,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnW","columnNumber":23,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnX","columnNumber":24,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnY","columnNumber":25,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnZ","columnNumber":26,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAA","columnNumber":27,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAB","columnNumber":28,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAC","columnNumber":29,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAD","columnNumber":30,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAE","columnNumber":31,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAF","columnNumber":32,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAG","columnNumber":33,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAH","columnNumber":34,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAI","columnNumber":35,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAJ","columnNumber":36,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAK","columnNumber":37,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAL","columnNumber":38,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAM","columnNumber":39,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAN","columnNumber":40,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAO","columnNumber":41,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAP","columnNumber":42,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAQ","columnNumber":43,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAR","columnNumber":44,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAS","columnNumber":45,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAT","columnNumber":46,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAU","columnNumber":47,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAV","columnNumber":48,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAW","columnNumber":49,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAX","columnNumber":50,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAY","columnNumber":51,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAZ","columnNumber":52,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBA","columnNumber":53,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBB","columnNumber":54,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBC","columnNumber":55,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBD","columnNumber":56,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBE","columnNumber":57,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBF","columnNumber":58,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBG","columnNumber":59,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBH","columnNumber":60,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBI","columnNumber":61,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBJ","columnNumber":62,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBK","columnNumber":63,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBL","columnNumber":64,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBM","columnNumber":65,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBN","columnNumber":66,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBO","columnNumber":67,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBP","columnNumber":68,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBQ","columnNumber":69,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBR","columnNumber":70,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBS","columnNumber":71,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBT","columnNumber":72,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBU","columnNumber":73,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBV","columnNumber":74,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBW","columnNumber":75,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBX","columnNumber":76,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBY","columnNumber":77,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBZ","columnNumber":78,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCA","columnNumber":79,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCB","columnNumber":80,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCC","columnNumber":81,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCD","columnNumber":82,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCE","columnNumber":83,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCF","columnNumber":84,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCG","columnNumber":85,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCH","columnNumber":86,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCI","columnNumber":87,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCJ","columnNumber":88,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCK","columnNumber":89,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCL","columnNumber":90,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCM","columnNumber":91,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCN","columnNumber":92,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCO","columnNumber":93,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCP","columnNumber":94,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCQ","columnNumber":95,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCR","columnNumber":96,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCS","columnNumber":97,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCT","columnNumber":98,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCU","columnNumber":99,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCV","columnNumber":100,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCW","columnNumber":101,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCX","columnNumber":102,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCY","columnNumber":103,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCZ","columnNumber":104,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDA","columnNumber":105,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDB","columnNumber":106,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDC","columnNumber":107,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDD","columnNumber":108,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDE","columnNumber":109,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDF","columnNumber":110,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDG","columnNumber":111,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDH","columnNumber":112,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDI","columnNumber":113,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDJ","columnNumber":114,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDK","columnNumber":115,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/RegasAdjustment/Monthly Regas Capacity by Country; source SUMMARY row 19
SET @c_q046='{"schemaVersion":1,"columns":[7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115],"headerRows":[385,386],"bodyRanges":[{"startRow":387,"endRow":444}],"identityColumns":[7],"fields":[{"fieldKey":"country","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnR","columnNumber":18,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnS","columnNumber":19,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnT","columnNumber":20,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnU","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnV","columnNumber":22,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnW","columnNumber":23,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnX","columnNumber":24,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnY","columnNumber":25,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnZ","columnNumber":26,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAA","columnNumber":27,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAB","columnNumber":28,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAC","columnNumber":29,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAD","columnNumber":30,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAE","columnNumber":31,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAF","columnNumber":32,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAG","columnNumber":33,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAH","columnNumber":34,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAI","columnNumber":35,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAJ","columnNumber":36,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAK","columnNumber":37,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAL","columnNumber":38,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAM","columnNumber":39,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAN","columnNumber":40,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAO","columnNumber":41,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAP","columnNumber":42,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAQ","columnNumber":43,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAR","columnNumber":44,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAS","columnNumber":45,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAT","columnNumber":46,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAU","columnNumber":47,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAV","columnNumber":48,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAW","columnNumber":49,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAX","columnNumber":50,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAY","columnNumber":51,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnAZ","columnNumber":52,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBA","columnNumber":53,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBB","columnNumber":54,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBC","columnNumber":55,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBD","columnNumber":56,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBE","columnNumber":57,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBF","columnNumber":58,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBG","columnNumber":59,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBH","columnNumber":60,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBI","columnNumber":61,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBJ","columnNumber":62,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBK","columnNumber":63,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBL","columnNumber":64,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBM","columnNumber":65,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBN","columnNumber":66,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBO","columnNumber":67,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBP","columnNumber":68,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBQ","columnNumber":69,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBR","columnNumber":70,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBS","columnNumber":71,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBT","columnNumber":72,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBU","columnNumber":73,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBV","columnNumber":74,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBW","columnNumber":75,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBX","columnNumber":76,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBY","columnNumber":77,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnBZ","columnNumber":78,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCA","columnNumber":79,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCB","columnNumber":80,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCC","columnNumber":81,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCD","columnNumber":82,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCE","columnNumber":83,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCF","columnNumber":84,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCG","columnNumber":85,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCH","columnNumber":86,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCI","columnNumber":87,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCJ","columnNumber":88,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCK","columnNumber":89,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCL","columnNumber":90,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCM","columnNumber":91,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCN","columnNumber":92,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCO","columnNumber":93,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCP","columnNumber":94,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCQ","columnNumber":95,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCR","columnNumber":96,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCS","columnNumber":97,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCT","columnNumber":98,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCU","columnNumber":99,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCV","columnNumber":100,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCW","columnNumber":101,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCX","columnNumber":102,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCY","columnNumber":103,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnCZ","columnNumber":104,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDA","columnNumber":105,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDB","columnNumber":106,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDC","columnNumber":107,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDD","columnNumber":108,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDE","columnNumber":109,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDF","columnNumber":110,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDG","columnNumber":111,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDH","columnNumber":112,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDI","columnNumber":113,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDJ","columnNumber":114,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnDK","columnNumber":115,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCProduction/Annual Gas Production Baseline; source SUMMARY row 21
SET @c_q047='{"schemaVersion":1,"columns":[3,4,5,6,8,9,10,11],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":24779}],"identityColumns":[3,4,5,6,8,9,10,11],"fields":[{"fieldKey":"year","columnNumber":3,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"continent","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"provinceBasin","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lifeCycleCategory","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"gasType","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"oilAndGasDetail","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"baseCaseSupplyMt","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCHurricanes/Hurricane Impact on Upstream Assets; source SUMMARY row 23
SET @c_q048='{"schemaVersion":1,"columns":[2,3,4,5,6,7],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":11}],"identityColumns":[2,3,4,5,6],"fields":[{"fieldKey":"intensity","columnNumber":2,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnC","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"netImpactMonthlyUpstreamProductionDecreaseBy","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCHurricanes/Hurricane Affected Provinces; source SUMMARY row 25
SET @c_q049='{"schemaVersion":1,"columns":[9,10,11],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":13}],"identityColumns":[9,11],"fields":[{"fieldKey":"provinceOfUpstreamAssetsImpactedByHurricaneInGoM","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"impactedByHurricane","columnNumber":11,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCHurricanes/Hurricane Impact on Liquefaction Plants; source SUMMARY row 24
SET @c_q050='{"schemaVersion":1,"columns":[2,3,4,5,6,7],"headerRows":[14,15],"bodyRanges":[{"startRow":16,"endRow":19}],"identityColumns":[2,3,4,5,6],"fields":[{"fieldKey":"intensity","columnNumber":2,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"preHurricaneLiqPlantShutInDurationNoOfWeeksInAMonth","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"netImpactMonthlyLNGProductionDecreaseBy","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCHurricanes/Hurricane Affected LNG Plants; source SUMMARY row 26
SET @c_q051='{"schemaVersion":1,"columns":[9,10,11],"headerRows":[15],"bodyRanges":[{"startRow":16,"endRow":41}],"identityColumns":[9,11],"fields":[{"fieldKey":"liquefactionPlantsImpactedByAHurricaneInGoM","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"impactedByHurricane","columnNumber":11,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCLiquefaction/Monthly LNG Train Baseline; source SUMMARY row 27
SET @c_q052='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,14],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":52411}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,14],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"month","columnNumber":3,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"period","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"plantName","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"plantTrain","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"infraLifeCycle","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngTrainCapacityMillionTonnesLNGPerMonth","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"mappedUCubeProject","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"upstreamAssetLifeCycle","columnNumber":11,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"mappedUCubeProjectProvinceNonNAmLNGPlantProvinceNAm","columnNumber":12,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlantUtilisation","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCRegasification/Annual Regas Capacity Baseline; source SUMMARY row 28
SET @c_q053='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":379}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17],"fields":[{"fieldKey":"continent","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"regasificationPlant","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lifeCycleCategory","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnG","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/BCPipeline/Annual Pipeline Capacity Baseline; source SUMMARY row 29
SET @c_q054='{"schemaVersion":1,"columns":[2,3,4,5,6],"headerRows":[7],"bodyRanges":[{"startRow":8,"endRow":70}],"identityColumns":[2,3,4,5,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"majorPipeline","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"pipelineCapacityMt","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- SUPPLY/LNG Summary/LNG Production Reconciliation; source SUMMARY row 30
SET @c_q055='{"schemaVersion":1,"columns":[3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25],"headerRows":[7,8],"bodyRanges":[{"startRow":9,"endRow":531}],"identityColumns":[3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25],"fields":[{"fieldKey":"woodmackenzie1Q26","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"region","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"country","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"project","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"train","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"status","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"fidDate","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"pjtStart","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"trainNo","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"mappedUpstreamProject","columnNumber":12,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"mappedUpstreamProvince","columnNumber":13,"dataType":"TEXT","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"capacity","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnR","columnNumber":18,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnS","columnNumber":19,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnT","columnNumber":20,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnU","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnV","columnNumber":22,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnW","columnNumber":23,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnX","columnNumber":24,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}},{"fieldKey":"columnY","columnNumber":25,"dataType":"DECIMAL","policyStatus":{"CREATE":"NOT_ALLOWED","SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED"}}],"capabilities":{"SET":"NOT_ALLOWED","CLEAR":"NOT_ALLOWED","ADD_ROW":"NOT_ALLOWED","DELETE_ROW":"NOT_ALLOWED"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/LNG_TradeRestrictionInput/LNG Restriction; source SUMMARY row 10
SET @c_q056='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11],"headerRows":[5],"bodyRanges":[{"startRow":6,"endRow":509}],"identityColumns":[2,3,4,5,6,7,8,9,10,11],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngExportTerminal","columnNumber":6,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngPlantTrain","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"scope","columnNumber":8,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"restrictionVolumeMtLNG","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"active","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/PNG_TradeRestrictionInput/PNG Restriction; source SUMMARY row 11
SET @c_q057='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10],"headerRows":[5],"bodyRanges":[{"startRow":6,"endRow":509}],"identityColumns":[2,3,4,5,6,7,8,9,10],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/MidstreamContractInput/Forecasted PNG Contract Volume (Imports); source SUMMARY row 13
SET @c_q058='{"schemaVersion":1,"columns":[1,2,3,4],"headerRows":[2,3,4],"bodyRanges":[{"startRow":5,"endRow":1104}],"identityColumns":[1,2,3,4],"fields":[{"fieldKey":"year","columnNumber":1,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"unadjustedGrossGasImportMtLNG","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/MidstreamContractInput/LNG Contract Volumes; source SUMMARY row 19
SET @c_q059='{"schemaVersion":1,"columns":[17,18,19,20,21],"headerRows":[2,3,4],"bodyRanges":[{"startRow":5,"endRow":2918}],"identityColumns":[17,18,19,20,21],"fields":[{"fieldKey":"year","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":18,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromLiquefactionTerminal","columnNumber":19,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":20,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"grossGasImportMtLNG","columnNumber":21,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Shipping Route Parameters; source SUMMARY row 27
SET @c_q060='{"schemaVersion":1,"columns":[29,30,31,34,35,38,39],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":375}],"identityColumns":[29,30,31,34,35,38,39],"fields":[{"fieldKey":"fromCountry","columnNumber":29,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":30,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"averageDurationDays","columnNumber":31,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"averageSpeedKnots","columnNumber":34,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fuelConsumptionTonnes","columnNumber":35,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"canals","columnNumber":38,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"portCharges","columnNumber":39,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Importer Classification; source SUMMARY row 28
SET @c_q061='{"schemaVersion":1,"columns":[42,43],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":92}],"identityColumns":[42,43],"fields":[{"fieldKey":"country","columnNumber":42,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"region","columnNumber":43,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Exporter List; source SUMMARY row 32
SET @c_q062='{"schemaVersion":1,"columns":[46],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":29}],"identityColumns":[46],"fields":[{"fieldKey":"country","columnNumber":46,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Atlantic Charter Cost; source SUMMARY row 21
SET @c_q063='{"schemaVersion":1,"columns":[2,4,6],"headerRows":[15,16],"bodyRanges":[{"startRow":17,"endRow":24}],"identityColumns":[2,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Fuel Cost - IFO380; source SUMMARY row 22
SET @c_q064='{"schemaVersion":1,"columns":[2,3,4,5,6],"headerRows":[26,27],"bodyRanges":[{"startRow":28,"endRow":35}],"identityColumns":[2,3,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseTonne","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"userInputImpliedValueTonne","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Suez Canal Cost; source SUMMARY row 23
SET @c_q065='{"schemaVersion":1,"columns":[2,3,4,5,6],"headerRows":[37,38],"bodyRanges":[{"startRow":39,"endRow":46}],"identityColumns":[2,3,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseCrossing","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"userInputImpliedValueCrossing","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Panama Canal Cost; source SUMMARY row 24
SET @c_q066='{"schemaVersion":1,"columns":[2,3,4,5,6],"headerRows":[48,49],"bodyRanges":[{"startRow":50,"endRow":57}],"identityColumns":[2,3,4,6],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseCrossing","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"userInputImpliedValueCrossing","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Suez Canal Disruption; source SUMMARY row 25
SET @c_q067='{"schemaVersion":1,"columns":[2,3,4],"headerRows":[59,60],"bodyRanges":[{"startRow":61,"endRow":68}],"identityColumns":[2,3],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"disruptedYesNo","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"additionalShippingDistanceNM","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Panama Canal Disruption; source SUMMARY row 26
SET @c_q068='{"schemaVersion":1,"columns":[2,3,4],"headerRows":[70,71],"bodyRanges":[{"startRow":72,"endRow":79}],"identityColumns":[2,3],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"disruptedYesNo","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"additionalShippingDistanceNM","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Technical Parameters; source SUMMARY row 29
SET @c_q069='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[81,82],"bodyRanges":[{"startRow":83,"endRow":83}],"identityColumns":[2,5],"fields":[{"fieldKey":"columnB","columnNumber":2,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"lngVesselTankHeel","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"effectiveLNGVolumeM3","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"m3ToMMbtuConversionFactor","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/ShippingCostInput/Panama Spot Capacity; source SUMMARY row 30
SET @c_q070='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[86,87],"bodyRanges":[{"startRow":88,"endRow":90}],"identityColumns":[2,3,4,5],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"panamaSpotCapacityMTPA","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"use","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"memo","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/MidstreamCapacityInput/PNG Infrastructure; source SUMMARY row 33
SET @c_q071='{"schemaVersion":1,"columns":[2,4,7,10,11,12,13,14,15,16,17],"headerRows":[1,2,3],"bodyRanges":[{"startRow":11,"endRow":207}],"identityColumns":[2,4,7,10,11,12,13,14,15,16,17],"fields":[{"fieldKey":"route","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":4,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":7,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnP","columnNumber":16,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnQ","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/MidstreamCapacityInput/Pipeline Import Allocation Shares; source SUMMARY row 34
SET @c_q072='{"schemaVersion":1,"columns":[128,129,130,131,132],"headerRows":[1,2,3],"bodyRanges":[{"startRow":4,"endRow":186}],"identityColumns":[128,129,130,131,132],"fields":[{"fieldKey":"year","columnNumber":128,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":129,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":130,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"pipeline","columnNumber":131,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"userAdjustableShare","columnNumber":132,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/MidstreamCapacityInput/Nord Stream Importer Allocation; source SUMMARY row 35
SET @c_q073='{"schemaVersion":1,"columns":[158,159,160],"headerRows":[1,2,3],"bodyRanges":[{"startRow":4,"endRow":8}],"identityColumns":[158,159,160],"fields":[{"fieldKey":"fromCountry","columnNumber":158,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":159,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"userAdjustableShare","columnNumber":160,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/UpstreamAdjLogicInput/Country Supply Adjustment; source SUMMARY row 45
SET @c_q074='{"schemaVersion":1,"columns":[9,10,16],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":674}],"identityColumns":[9,10,16],"fields":[{"fieldKey":"year","columnNumber":9,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":10,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/UpstreamAdjLogicInput/Excess LNG to PNG Countries; source SUMMARY row 46
SET @c_q075='{"schemaVersion":1,"columns":[20],"headerRows":[1,2,3],"bodyRanges":[{"startRow":4,"endRow":40}],"identityColumns":[20],"fields":[{"fieldKey":"exporterCountries","columnNumber":20,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/DemandAdjLogicInput/Premium Market Countries; source SUMMARY row 48
SET @c_q076='{"schemaVersion":1,"columns":[1],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":16}],"identityColumns":[1],"fields":[{"fieldKey":"premiumMarkets","columnNumber":1,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/DemandAdjLogicInput/Demand Adjustment Parameters; source SUMMARY row 49
SET @c_q077='{"schemaVersion":1,"columns":[3,4,5,6],"headerRows":[1],"bodyRanges":[{"startRow":2,"endRow":3}],"identityColumns":[3,4,5,6],"fields":[{"fieldKey":"demandAdjustment","columnNumber":3,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/DemandAdjLogicInput/Demand Adjustment Bands; source SUMMARY row 50
SET @c_q078='{"schemaVersion":1,"columns":[8,9,10],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":7}],"identityColumns":[8,9,10],"fields":[{"fieldKey":"lowerBound","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"deviations","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/Regasification Tariff; source SUMMARY row 51
SET @c_q079='{"schemaVersion":1,"columns":[1,2,3,4],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":379}],"identityColumns":[1,2,3,4],"fields":[{"fieldKey":"year","columnNumber":1,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseInput","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/Liquefaction Cost; source SUMMARY row 52
SET @c_q080='{"schemaVersion":1,"columns":[8,9,10,11],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":233}],"identityColumns":[8,9,10,11],"fields":[{"fieldKey":"year","columnNumber":8,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":9,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseInput","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/LNG Upstream Cost; source SUMMARY row 53
SET @c_q081='{"schemaVersion":1,"columns":[15,16,17,18],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":227}],"identityColumns":[15,16,17,18],"fields":[{"fieldKey":"year","columnNumber":15,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":16,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseInput","columnNumber":17,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/Non-LNG Upstream Cost; source SUMMARY row 54
SET @c_q082='{"schemaVersion":1,"columns":[22,23,24,25],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":615}],"identityColumns":[22,23,24,25],"fields":[{"fieldKey":"year","columnNumber":22,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":23,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"reBaseCaseInput","columnNumber":24,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/Pipeline Cost; source SUMMARY row 55
SET @c_q083='{"schemaVersion":1,"columns":[29,30,31,32,33],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":1554}],"identityColumns":[29,30,31,32,33],"fields":[{"fieldKey":"year","columnNumber":29,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"fromCountry","columnNumber":30,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"toCountry","columnNumber":31,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/CostInput/LNG Project SRMC; source SUMMARY row 56
SET @c_q084='{"schemaVersion":1,"columns":[37,38,39,40],"headerRows":[1,2],"bodyRanges":[{"startRow":3,"endRow":519}],"identityColumns":[37,38,39,40],"fields":[{"fieldKey":"year","columnNumber":37,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"projectMapped","columnNumber":38,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":39,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"averageOfCostsUSDKcf","columnNumber":40,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/4.5 PriceModelInput/JKM - Base Case; source SUMMARY row 63
SET @c_q085='{"schemaVersion":1,"columns":[35,36,37],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":35}],"identityColumns":[35,36,37],"fields":[{"fieldKey":"year","columnNumber":35,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":36,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"uncontractedVolumesMtLNG","columnNumber":37,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/4.5 PriceModelInput/TTF - Base Case; source SUMMARY row 64
SET @c_q086='{"schemaVersion":1,"columns":[39,40,41],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":59}],"identityColumns":[39,40,41],"fields":[{"fieldKey":"year","columnNumber":39,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"country","columnNumber":40,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"uncontractedVolumesMtLNG","columnNumber":41,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- TRADE/LNGExportRestictionInput/LNG Export Restriction; source SUMMARY row 65
SET @c_q087='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[2,3],"bodyRanges":[{"startRow":4,"endRow":200}],"identityColumns":[2,3,4,5],"fields":[],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- OUTPUT/USCostofSupply/US Cost of Supply; source SUMMARY row 10
SET @c_q088='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12],"headerRows":[6,7],"bodyRanges":[{"startRow":8,"endRow":816}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12],"fields":[{"fieldKey":"breakevenGasPriceUSDMmbtu","columnNumber":2,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnC","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnG","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- OUTPUT/BCPrices/Base Case Prices; source SUMMARY row 12
SET @c_q089='{"schemaVersion":1,"columns":[2,3,4,5],"headerRows":[6,7],"bodyRanges":[{"startRow":8,"endRow":21}],"identityColumns":[2,3,4,5],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"hh","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"eastAsiaSpot","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"ttf","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- OUTPUT/PremiumCalc/PremiumCalc TTF; source SUMMARY row 13
SET @c_q090='{"schemaVersion":1,"columns":[2,3],"headerRows":[35,36,37,38],"bodyRanges":[{"startRow":39,"endRow":47}],"identityColumns":[2,3],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"baseCase","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- OUTPUT/PremiumCalc/PremiumCalc East Asia Spot; source SUMMARY row 14
SET @c_q091='{"schemaVersion":1,"columns":[2,3],"headerRows":[53,54,55,56],"bodyRanges":[{"startRow":57,"endRow":61}],"identityColumns":[2,3],"fields":[{"fieldKey":"year","columnNumber":2,"dataType":"INTEGER","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"baseCase","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

-- OUTPUT/PriceSeasonality/Price Seasonality; source SUMMARY row 27
SET @c_q092='{"schemaVersion":1,"columns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15],"headerRows":[1],"bodyRanges":[{"startRow":2,"endRow":4}],"identityColumns":[2,3,4,5,6,7,8,9,10,11,12,13,14,15],"fields":[{"fieldKey":"priceRegion","columnNumber":2,"dataType":"TEXT","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnC","columnNumber":3,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnD","columnNumber":4,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnE","columnNumber":5,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnF","columnNumber":6,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnG","columnNumber":7,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnH","columnNumber":8,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnI","columnNumber":9,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnJ","columnNumber":10,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnK","columnNumber":11,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnL","columnNumber":12,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnM","columnNumber":13,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnN","columnNumber":14,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}},{"fieldKey":"columnO","columnNumber":15,"dataType":"DECIMAL","policyStatus":{"CREATE":"INCOMPLETE","SET":"INCOMPLETE","CLEAR":"INCOMPLETE"}}],"capabilities":{"SET":"INCOMPLETE","CLEAR":"INCOMPLETE","ADD_ROW":"INCOMPLETE","DELETE_ROW":"INCOMPLETE"},"inputGuideRules":[],"controls":[],"displayUpdateRules":[]}';

INSERT INTO tmd_mdl_inpttbld
 (inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,tbl_bgn_col_no,tbl_end_col_no,
  hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,col_add_psbl_yn,inpt_tbl_expn,qry_json,
  crt_usr_logn_id,crt_dtm,mod_usr_logn_id,mod_dtm,del_yn)
SELECT s.inpt_sht_id,x.name,x.ord,x.br,x.er,x.bc,x.ec,x.hr,x.he,x.dr,x.ra,x.ca,x.description,x.qry,
       @c_actor,CURRENT_TIMESTAMP,@c_actor,CURRENT_TIMESTAMP,'N'
FROM (
SELECT 'DEMAND' AS role,'GDPPopulationAdjustment' AS sheet,'GDP Adjustment' AS name,1 AS ord,6 AS br,2000 AS er,2 AS bc,6 AS ec,6 AS hr,6 AS he,12 AS dr,'Y' AS ra,'N' AS ca,'STRUCTURE DRAFT; DEMAND SUMMARY!D11:J11; write policy incomplete' AS description,@c_q001 AS qry
UNION ALL
SELECT 'DEMAND','GDPPopulationAdjustment','Population Adjustment',2,6,2000,10,13,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D12:J12; write policy incomplete',@c_q002
UNION ALL
SELECT 'DEMAND','ElectrificationRateAdjustment','Electrification Rate Adjustment',1,6,2000,2,6,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D13:J13; write policy incomplete',@c_q003
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment','Annual Power Mix Adjustment',1,6,7989,2,6,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D14:J14; write policy incomplete',@c_q004
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment','Monthly Power Mix Adjustment',2,6,7989,10,15,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D15:J15; write policy incomplete',@c_q005
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment','Coal Futures Price Adjustment',1,6,2000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D16:J16; write policy incomplete',@c_q006
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment','Gas Futures Price Adjustment',2,6,2000,8,11,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D17:J17; write policy incomplete',@c_q007
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Country HDD and CDD Adjustment',1,6,7997,2,7,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D18:J18; write policy incomplete',@c_q008
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Country Temperature Adjustment',2,6,7997,11,15,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D19:J19; write policy incomplete',@c_q009
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Regional HDD and CDD Adjustment',3,6,7997,20,24,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D20:J20; write policy incomplete',@c_q010
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Regional Temperature Change Adjustment',4,6,7997,28,31,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D21:J21; write policy incomplete',@c_q011
UNION ALL
SELECT 'DEMAND','PMIAdjustment','Monthly PMI Adjustment',1,6,8000,2,6,6,6,13,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D22:J22; write policy incomplete',@c_q012
UNION ALL
SELECT 'DEMAND','PMIAdjustment','PMI Reference Country Adjustment',2,6,8000,10,12,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D23:J23; write policy incomplete',@c_q013
UNION ALL
SELECT 'DEMAND','HydrogenAdjustment','Hydrogen Production Adjustment',1,6,8000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D24:J24; write policy incomplete',@c_q014
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment','LNG Bunkering Vessel Count Adjustment',1,6,2000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D25:J25; write policy incomplete',@c_q015
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment','LNG Bunkering Fuel Consumption Adjustment',2,6,2000,8,11,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D26:J26; write policy incomplete',@c_q016
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite','Demand Model Manual Overwrite Sector Input',1,6,507,4,11,6,7,8,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D27:J27; write policy incomplete',@c_q017
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite','Demand Model Manual Overwrite Monthly Input',2,6,507,13,19,6,7,8,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D28:J28; write policy incomplete',@c_q018
UNION ALL
SELECT 'DEMAND','BCInput','RE Base Case Gas Demand Data by Sector',1,2,14493,3,14,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D32:J32; write policy incomplete',@c_q019
UNION ALL
SELECT 'DEMAND','BCInput','RE Base Case GDP and Population Data',2,2,2368,17,22,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D33:J33; write policy incomplete',@c_q020
UNION ALL
SELECT 'DEMAND','BCInput','Power Generation Data',3,2,100803,26,35,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D34:J34; write policy incomplete',@c_q021
UNION ALL
SELECT 'DEMAND','BCInput','Power Generation Utilisation Rate',4,2,1125,37,40,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D35:J35; write policy incomplete',@c_q022
UNION ALL
SELECT 'DEMAND','BCInput','Car Sales for Electrification Input',5,2,13548,43,47,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D36:J36; write policy incomplete',@c_q023
UNION ALL
SELECT 'DEMAND','BCInput','Base Case LNG Bunkering Demand',6,2,252,49,51,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D37:J37; write policy incomplete',@c_q024
UNION ALL
SELECT 'DEMAND','BCInput','Base Case Hydrogen Demand',7,2,290,54,56,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D38:J38; write policy incomplete',@c_q025
UNION ALL
SELECT 'DEMAND','BCInput','Coal and Gas Futures Pricing',8,2,28,59,63,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D39:J39; write policy incomplete',@c_q026
UNION ALL
SELECT 'DEMAND','MacroParametersLogicInput','Selected Macro Parameter Intensities',1,7,176,9,16,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D41:J41; write policy incomplete',@c_q027
UNION ALL
SELECT 'DEMAND','PMIInput','Base Case PMI Input',1,7,1016,22,37,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D44:J44; write policy incomplete',@c_q028
UNION ALL
SELECT 'DEMAND','PMILogicInput','User-Adjusted PMI Intensity Parameter',1,7,92,2,5,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D46:J46; write policy incomplete',@c_q029
UNION ALL
SELECT 'DEMAND','PMILogicInput','Base Case PMI Intensity Parameter',2,7,92,7,10,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D47:J47; write policy incomplete',@c_q030
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','User-Adjusted HDD-Gas Intensity Input',1,7,92,2,5,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D49:J49; write policy incomplete',@c_q031
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','User-Adjusted CDD-Gas Intensity Input',2,7,92,7,10,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D50:J50; write policy incomplete',@c_q032
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','Base Case HDD-Gas Intensity Input',3,7,92,12,15,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D51:J51; write policy incomplete',@c_q033
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','Base Case CDD-Gas Intensity Input',4,7,92,17,20,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D52:J52; write policy incomplete',@c_q034
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case HDD Input',1,7,1016,74,89,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D61:J61; write policy incomplete',@c_q035
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case CDD Input',2,7,1016,92,107,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D62:J62; write policy incomplete',@c_q036
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case Temperature',3,7,1016,139,154,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D65:J65; write policy incomplete',@c_q037
UNION ALL
SELECT 'DEMAND','LNGBunkeringInput','Base Case LNG Bunkering Input',1,7,56,8,11,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D67:J67; write policy incomplete',@c_q038
UNION ALL
SELECT 'DEMAND','LNGBunkeringLogicInput','LNG Bunkering Share by Country',1,6,98,2,17,6,7,8,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D69:J69; write policy incomplete',@c_q039
UNION ALL
SELECT 'DEMAND','HydrogenInput','Base Case Hydrogen Production Input',1,7,736,6,8,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D71:J71; write policy incomplete',@c_q040
UNION ALL
SELECT 'SUPPLY','ProductionAdjustment','PNG Production Adjustment',1,7,200,9,13,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D11:J11; write policy incomplete',@c_q041
UNION ALL
SELECT 'SUPPLY','{USA}Hurricanes','Monthly Hurricane Adjustment',1,7,200,2,4,7,7,13,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D14:J14; write policy incomplete',@c_q042
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment','Monthly LNG Train Utilization',1,7,407,2,90,7,10,11,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D15:J15; write policy incomplete',@c_q043
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment','Monthly LNG Train Production',1,7,407,2,90,7,10,11,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D16:J16; write policy incomplete',@c_q044
UNION ALL
SELECT 'SUPPLY','RegasAdjustment','Monthly Regas Capacity Adjustment',1,8,382,2,115,8,10,11,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D17:J17; write policy incomplete',@c_q045
UNION ALL
SELECT 'SUPPLY','RegasAdjustment','Monthly Regas Capacity by Country',2,385,444,7,115,385,386,387,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D19:J19; write policy incomplete',@c_q046
UNION ALL
SELECT 'SUPPLY','BCProduction','Annual Gas Production Baseline',1,7,24779,3,11,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D21:J21; write policy incomplete',@c_q047
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Impact on Upstream Assets',1,7,11,2,7,7,7,8,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D23:J23; write policy incomplete',@c_q048
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Affected Provinces',2,7,13,9,11,7,7,8,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D25:J25; write policy incomplete',@c_q049
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Impact on Liquefaction Plants',3,14,19,2,7,14,15,16,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D24:J24; write policy incomplete',@c_q050
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Affected LNG Plants',4,15,41,9,11,15,15,16,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D26:J26; write policy incomplete',@c_q051
UNION ALL
SELECT 'SUPPLY','BCLiquefaction','Monthly LNG Train Baseline',1,7,52411,2,14,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D27:J27; write policy incomplete',@c_q052
UNION ALL
SELECT 'SUPPLY','BCRegasification','Annual Regas Capacity Baseline',1,7,379,2,17,7,7,8,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D28:J28; write policy incomplete',@c_q053
UNION ALL
SELECT 'SUPPLY','BCPipeline','Annual Pipeline Capacity Baseline',1,7,70,2,6,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D29:J29; write policy incomplete',@c_q054
UNION ALL
SELECT 'SUPPLY','LNG Summary','LNG Production Reconciliation',1,7,531,3,25,7,8,9,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D30:J30; write policy incomplete',@c_q055
UNION ALL
SELECT 'TRADE','LNG_TradeRestrictionInput','LNG Restriction',1,5,509,2,11,5,5,6,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D10:J10; write policy incomplete',@c_q056
UNION ALL
SELECT 'TRADE','PNG_TradeRestrictionInput','PNG Restriction',1,5,509,2,10,5,5,6,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D11:J11; write policy incomplete',@c_q057
UNION ALL
SELECT 'TRADE','MidstreamContractInput','Forecasted PNG Contract Volume (Imports)',1,2,1104,1,4,2,4,5,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D13:J13; write policy incomplete',@c_q058
UNION ALL
SELECT 'TRADE','MidstreamContractInput','LNG Contract Volumes',2,2,2918,17,21,2,4,5,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D19:J19; write policy incomplete',@c_q059
UNION ALL
SELECT 'TRADE','ShippingCostInput','Shipping Route Parameters',1,2,375,29,39,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D27:J27; write policy incomplete',@c_q060
UNION ALL
SELECT 'TRADE','ShippingCostInput','Importer Classification',2,2,92,42,43,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D28:J28; write policy incomplete',@c_q061
UNION ALL
SELECT 'TRADE','ShippingCostInput','Exporter List',3,2,29,46,46,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D32:J32; write policy incomplete',@c_q062
UNION ALL
SELECT 'TRADE','ShippingCostInput','Atlantic Charter Cost',4,15,24,2,6,15,16,17,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D21:J21; write policy incomplete',@c_q063
UNION ALL
SELECT 'TRADE','ShippingCostInput','Fuel Cost - IFO380',5,26,35,2,6,26,27,28,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D22:J22; write policy incomplete',@c_q064
UNION ALL
SELECT 'TRADE','ShippingCostInput','Suez Canal Cost',6,37,46,2,6,37,38,39,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D23:J23; write policy incomplete',@c_q065
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Canal Cost',7,48,57,2,6,48,49,50,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D24:J24; write policy incomplete',@c_q066
UNION ALL
SELECT 'TRADE','ShippingCostInput','Suez Canal Disruption',8,59,68,2,4,59,60,61,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D25:J25; write policy incomplete',@c_q067
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Canal Disruption',9,70,79,2,4,70,71,72,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D26:J26; write policy incomplete',@c_q068
UNION ALL
SELECT 'TRADE','ShippingCostInput','Technical Parameters',10,81,83,2,5,81,82,83,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D29:J29; write policy incomplete',@c_q069
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Spot Capacity',11,86,90,2,5,86,87,88,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D30:J30; write policy incomplete',@c_q070
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','PNG Infrastructure',1,1,207,2,17,1,3,11,'Y','Y','STRUCTURE DRAFT; TRADE SUMMARY!D33:J33; write policy incomplete',@c_q071
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','Pipeline Import Allocation Shares',2,1,186,128,132,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D34:J34; write policy incomplete',@c_q072
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','Nord Stream Importer Allocation',3,1,8,158,160,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D35:J35; write policy incomplete',@c_q073
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput','Country Supply Adjustment',1,1,674,9,16,1,2,3,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D45:J45; write policy incomplete',@c_q074
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput','Excess LNG to PNG Countries',2,1,40,20,20,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D46:J46; write policy incomplete',@c_q075
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Premium Market Countries',1,1,16,1,1,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D48:J48; write policy incomplete',@c_q076
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Demand Adjustment Parameters',2,1,3,3,6,1,1,2,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D49:J49; write policy incomplete',@c_q077
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Demand Adjustment Bands',3,1,7,8,10,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D50:J50; write policy incomplete',@c_q078
UNION ALL
SELECT 'TRADE','CostInput','Regasification Tariff',1,1,379,1,4,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D51:J51; write policy incomplete',@c_q079
UNION ALL
SELECT 'TRADE','CostInput','Liquefaction Cost',2,1,233,8,11,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D52:J52; write policy incomplete',@c_q080
UNION ALL
SELECT 'TRADE','CostInput','LNG Upstream Cost',3,1,227,15,18,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D53:J53; write policy incomplete',@c_q081
UNION ALL
SELECT 'TRADE','CostInput','Non-LNG Upstream Cost',4,1,615,22,25,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D54:J54; write policy incomplete',@c_q082
UNION ALL
SELECT 'TRADE','CostInput','Pipeline Cost',5,1,1554,29,33,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D55:J55; write policy incomplete',@c_q083
UNION ALL
SELECT 'TRADE','CostInput','LNG Project SRMC',6,1,519,37,40,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D56:J56; write policy incomplete',@c_q084
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput','JKM - Base Case',1,2,35,35,37,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D63:J63; write policy incomplete',@c_q085
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput','TTF - Base Case',2,2,59,39,41,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D64:J64; write policy incomplete',@c_q086
UNION ALL
SELECT 'TRADE','LNGExportRestictionInput','LNG Export Restriction',1,2,200,2,5,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D65:J65; write policy incomplete',@c_q087
UNION ALL
SELECT 'OUTPUT','USCostofSupply','US Cost of Supply',1,6,816,2,12,6,7,8,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D10:J10; write policy incomplete',@c_q088
UNION ALL
SELECT 'OUTPUT','BCPrices','Base Case Prices',1,6,21,2,5,6,7,8,'Y','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D12:J12; write policy incomplete',@c_q089
UNION ALL
SELECT 'OUTPUT','PremiumCalc','PremiumCalc TTF',1,35,47,2,8,35,38,39,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D13:J13; write policy incomplete',@c_q090
UNION ALL
SELECT 'OUTPUT','PremiumCalc','PremiumCalc East Asia Spot',2,53,61,2,8,53,56,57,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D14:J14; write policy incomplete',@c_q091
UNION ALL
SELECT 'OUTPUT','PriceSeasonality','Price Seasonality',1,1,4,2,15,1,1,2,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D27:J27; write policy incomplete',@c_q092
) x JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@c_tmplt_id AND f.mdl_file_cd=x.role AND f.del_yn='N'
JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet AND s.sht_stat_cd='VISIBLE' AND s.del_yn='N'
JOIN tmd_mdl_inpttmpltm p ON p.tmplt_id=f.tmplt_id AND p.tmplt_stat_cd='DRAFT' AND p.tmplt_expn=@c_marker AND p.del_yn='N'
WHERE @c_ok=1;
SET @c_ok=IF(@c_ok=1 AND ROW_COUNT()=92,1,0);
SELECT @c_ok AS table_insert_must_be_1;
-- tmd_mdl_inptmodscopd INSERT 없음. 확인된 읽기 구조와 쓰기 허용은 별개다.

-- 3. 등록 후 검증 (같은 미commit transaction 안)
-- expected: root1 / roles4 / sheets70 (DEMAND25,SUPPLY23,TRADE17,OUTPUT5) / tables92 / scopes0。
SELECT p.tmplt_id,p.mdl_spt_cd,p.tmplt_ver_no,p.tmplt_stat_cd,p.aply_bgn_dtm,p.tmplt_expn
FROM tmd_mdl_inpttmpltm p WHERE p.tmplt_id=@c_tmplt_id;
SELECT f.mdl_file_cd,f.inpt_file_id,f.file_ord,f.file_strt_hash_val,COUNT(DISTINCT s.inpt_sht_id) AS sheets,
       COUNT(t.inpt_tbl_id) AS query_tables
FROM tmd_mdl_inptfiled f LEFT JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id
LEFT JOIN tmd_mdl_inpttbld t ON t.inpt_sht_id=s.inpt_sht_id
WHERE f.tmplt_id=@c_tmplt_id GROUP BY f.inpt_file_id ORDER BY f.file_ord;
-- expected query_tables: DEMAND40 / SUPPLY15 / TRADE32 / OUTPUT5。
-- 예상0건: 개발 전용 시트·hidden 시트 위 표·잘못된 JSON·중복 식별자.
SELECT f.mdl_file_cd,s.sht_nm,t.inpt_tbl_nm FROM tmd_mdl_inptfiled f
JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id LEFT JOIN tmd_mdl_inpttbld t ON t.inpt_sht_id=s.inpt_sht_id
WHERE f.tmplt_id=@c_tmplt_id AND (s.sht_nm IN ('SUMMARY','플랫폼 입력 안내')
 OR (t.inpt_tbl_id IS NOT NULL AND (s.sht_stat_cd<>'VISIBLE' OR t.qry_json IS NULL OR JSON_VALID(t.qry_json)<>1 OR OCTET_LENGTH(t.qry_json)>1048576)));
SELECT CONCAT(f.mdl_file_cd,'/',s.sht_nm,'/',t.inpt_tbl_nm) AS tableId,COUNT(*) AS n
FROM tmd_mdl_inptfiled f JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id JOIN tmd_mdl_inpttbld t ON t.inpt_sht_id=s.inpt_sht_id
WHERE f.tmplt_id=@c_tmplt_id GROUP BY f.mdl_file_cd,s.sht_nm,t.inpt_tbl_nm HAVING COUNT(*)<>1;
-- 92행의 실제 부모ID·좌표·flags·JSON을 seed와 엄격히 대조한다. 예상0건.

SELECT x.role,x.sheet,x.name,'seed mismatch' AS issue FROM (
SELECT 'DEMAND' AS role,'GDPPopulationAdjustment' AS sheet,'GDP Adjustment' AS name,1 AS ord,6 AS br,2000 AS er,2 AS bc,6 AS ec,6 AS hr,6 AS he,12 AS dr,'Y' AS ra,'N' AS ca,'STRUCTURE DRAFT; DEMAND SUMMARY!D11:J11; write policy incomplete' AS description,@c_q001 AS qry
UNION ALL
SELECT 'DEMAND','GDPPopulationAdjustment','Population Adjustment',2,6,2000,10,13,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D12:J12; write policy incomplete',@c_q002
UNION ALL
SELECT 'DEMAND','ElectrificationRateAdjustment','Electrification Rate Adjustment',1,6,2000,2,6,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D13:J13; write policy incomplete',@c_q003
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment','Annual Power Mix Adjustment',1,6,7989,2,6,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D14:J14; write policy incomplete',@c_q004
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment','Monthly Power Mix Adjustment',2,6,7989,10,15,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D15:J15; write policy incomplete',@c_q005
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment','Coal Futures Price Adjustment',1,6,2000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D16:J16; write policy incomplete',@c_q006
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment','Gas Futures Price Adjustment',2,6,2000,8,11,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D17:J17; write policy incomplete',@c_q007
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Country HDD and CDD Adjustment',1,6,7997,2,7,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D18:J18; write policy incomplete',@c_q008
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Country Temperature Adjustment',2,6,7997,11,15,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D19:J19; write policy incomplete',@c_q009
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Regional HDD and CDD Adjustment',3,6,7997,20,24,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D20:J20; write policy incomplete',@c_q010
UNION ALL
SELECT 'DEMAND','WeatherAdjustment','Regional Temperature Change Adjustment',4,6,7997,28,31,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D21:J21; write policy incomplete',@c_q011
UNION ALL
SELECT 'DEMAND','PMIAdjustment','Monthly PMI Adjustment',1,6,8000,2,6,6,6,13,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D22:J22; write policy incomplete',@c_q012
UNION ALL
SELECT 'DEMAND','PMIAdjustment','PMI Reference Country Adjustment',2,6,8000,10,12,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D23:J23; write policy incomplete',@c_q013
UNION ALL
SELECT 'DEMAND','HydrogenAdjustment','Hydrogen Production Adjustment',1,6,8000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D24:J24; write policy incomplete',@c_q014
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment','LNG Bunkering Vessel Count Adjustment',1,6,2000,2,5,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D25:J25; write policy incomplete',@c_q015
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment','LNG Bunkering Fuel Consumption Adjustment',2,6,2000,8,11,6,6,12,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D26:J26; write policy incomplete',@c_q016
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite','Demand Model Manual Overwrite Sector Input',1,6,507,4,11,6,7,8,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D27:J27; write policy incomplete',@c_q017
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite','Demand Model Manual Overwrite Monthly Input',2,6,507,13,19,6,7,8,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D28:J28; write policy incomplete',@c_q018
UNION ALL
SELECT 'DEMAND','BCInput','RE Base Case Gas Demand Data by Sector',1,2,14493,3,14,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D32:J32; write policy incomplete',@c_q019
UNION ALL
SELECT 'DEMAND','BCInput','RE Base Case GDP and Population Data',2,2,2368,17,22,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D33:J33; write policy incomplete',@c_q020
UNION ALL
SELECT 'DEMAND','BCInput','Power Generation Data',3,2,100803,26,35,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D34:J34; write policy incomplete',@c_q021
UNION ALL
SELECT 'DEMAND','BCInput','Power Generation Utilisation Rate',4,2,1125,37,40,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D35:J35; write policy incomplete',@c_q022
UNION ALL
SELECT 'DEMAND','BCInput','Car Sales for Electrification Input',5,2,13548,43,47,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D36:J36; write policy incomplete',@c_q023
UNION ALL
SELECT 'DEMAND','BCInput','Base Case LNG Bunkering Demand',6,2,252,49,51,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D37:J37; write policy incomplete',@c_q024
UNION ALL
SELECT 'DEMAND','BCInput','Base Case Hydrogen Demand',7,2,290,54,56,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D38:J38; write policy incomplete',@c_q025
UNION ALL
SELECT 'DEMAND','BCInput','Coal and Gas Futures Pricing',8,2,28,59,63,2,3,4,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D39:J39; write policy incomplete',@c_q026
UNION ALL
SELECT 'DEMAND','MacroParametersLogicInput','Selected Macro Parameter Intensities',1,7,176,9,16,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D41:J41; write policy incomplete',@c_q027
UNION ALL
SELECT 'DEMAND','PMIInput','Base Case PMI Input',1,7,1016,22,37,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D44:J44; write policy incomplete',@c_q028
UNION ALL
SELECT 'DEMAND','PMILogicInput','User-Adjusted PMI Intensity Parameter',1,7,92,2,5,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D46:J46; write policy incomplete',@c_q029
UNION ALL
SELECT 'DEMAND','PMILogicInput','Base Case PMI Intensity Parameter',2,7,92,7,10,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D47:J47; write policy incomplete',@c_q030
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','User-Adjusted HDD-Gas Intensity Input',1,7,92,2,5,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D49:J49; write policy incomplete',@c_q031
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','User-Adjusted CDD-Gas Intensity Input',2,7,92,7,10,7,8,9,'N','N','STRUCTURE DRAFT; DEMAND SUMMARY!D50:J50; write policy incomplete',@c_q032
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','Base Case HDD-Gas Intensity Input',3,7,92,12,15,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D51:J51; write policy incomplete',@c_q033
UNION ALL
SELECT 'DEMAND','WeatherLogicInput','Base Case CDD-Gas Intensity Input',4,7,92,17,20,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D52:J52; write policy incomplete',@c_q034
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case HDD Input',1,7,1016,74,89,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D61:J61; write policy incomplete',@c_q035
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case CDD Input',2,7,1016,92,107,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D62:J62; write policy incomplete',@c_q036
UNION ALL
SELECT 'DEMAND','WeatherInput','Base Case Temperature',3,7,1016,139,154,7,8,9,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D65:J65; write policy incomplete',@c_q037
UNION ALL
SELECT 'DEMAND','LNGBunkeringInput','Base Case LNG Bunkering Input',1,7,56,8,11,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D67:J67; write policy incomplete',@c_q038
UNION ALL
SELECT 'DEMAND','LNGBunkeringLogicInput','LNG Bunkering Share by Country',1,6,98,2,17,6,7,8,'Y','Y','STRUCTURE DRAFT; DEMAND SUMMARY!D69:J69; write policy incomplete',@c_q039
UNION ALL
SELECT 'DEMAND','HydrogenInput','Base Case Hydrogen Production Input',1,7,736,6,8,7,8,9,'Y','N','STRUCTURE DRAFT; DEMAND SUMMARY!D71:J71; write policy incomplete',@c_q040
UNION ALL
SELECT 'SUPPLY','ProductionAdjustment','PNG Production Adjustment',1,7,200,9,13,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D11:J11; write policy incomplete',@c_q041
UNION ALL
SELECT 'SUPPLY','{USA}Hurricanes','Monthly Hurricane Adjustment',1,7,200,2,4,7,7,13,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D14:J14; write policy incomplete',@c_q042
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment','Monthly LNG Train Utilization',1,7,407,2,90,7,10,11,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D15:J15; write policy incomplete',@c_q043
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment','Monthly LNG Train Production',1,7,407,2,90,7,10,11,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D16:J16; write policy incomplete',@c_q044
UNION ALL
SELECT 'SUPPLY','RegasAdjustment','Monthly Regas Capacity Adjustment',1,8,382,2,115,8,10,11,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D17:J17; write policy incomplete',@c_q045
UNION ALL
SELECT 'SUPPLY','RegasAdjustment','Monthly Regas Capacity by Country',2,385,444,7,115,385,386,387,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D19:J19; write policy incomplete',@c_q046
UNION ALL
SELECT 'SUPPLY','BCProduction','Annual Gas Production Baseline',1,7,24779,3,11,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D21:J21; write policy incomplete',@c_q047
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Impact on Upstream Assets',1,7,11,2,7,7,7,8,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D23:J23; write policy incomplete',@c_q048
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Affected Provinces',2,7,13,9,11,7,7,8,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D25:J25; write policy incomplete',@c_q049
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Impact on Liquefaction Plants',3,14,19,2,7,14,15,16,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D24:J24; write policy incomplete',@c_q050
UNION ALL
SELECT 'SUPPLY','BCHurricanes','Hurricane Affected LNG Plants',4,15,41,9,11,15,15,16,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D26:J26; write policy incomplete',@c_q051
UNION ALL
SELECT 'SUPPLY','BCLiquefaction','Monthly LNG Train Baseline',1,7,52411,2,14,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D27:J27; write policy incomplete',@c_q052
UNION ALL
SELECT 'SUPPLY','BCRegasification','Annual Regas Capacity Baseline',1,7,379,2,17,7,7,8,'Y','Y','STRUCTURE DRAFT; SUPPLY SUMMARY!D28:J28; write policy incomplete',@c_q053
UNION ALL
SELECT 'SUPPLY','BCPipeline','Annual Pipeline Capacity Baseline',1,7,70,2,6,7,7,8,'Y','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D29:J29; write policy incomplete',@c_q054
UNION ALL
SELECT 'SUPPLY','LNG Summary','LNG Production Reconciliation',1,7,531,3,25,7,8,9,'N','N','STRUCTURE DRAFT; SUPPLY SUMMARY!D30:J30; write policy incomplete',@c_q055
UNION ALL
SELECT 'TRADE','LNG_TradeRestrictionInput','LNG Restriction',1,5,509,2,11,5,5,6,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D10:J10; write policy incomplete',@c_q056
UNION ALL
SELECT 'TRADE','PNG_TradeRestrictionInput','PNG Restriction',1,5,509,2,10,5,5,6,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D11:J11; write policy incomplete',@c_q057
UNION ALL
SELECT 'TRADE','MidstreamContractInput','Forecasted PNG Contract Volume (Imports)',1,2,1104,1,4,2,4,5,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D13:J13; write policy incomplete',@c_q058
UNION ALL
SELECT 'TRADE','MidstreamContractInput','LNG Contract Volumes',2,2,2918,17,21,2,4,5,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D19:J19; write policy incomplete',@c_q059
UNION ALL
SELECT 'TRADE','ShippingCostInput','Shipping Route Parameters',1,2,375,29,39,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D27:J27; write policy incomplete',@c_q060
UNION ALL
SELECT 'TRADE','ShippingCostInput','Importer Classification',2,2,92,42,43,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D28:J28; write policy incomplete',@c_q061
UNION ALL
SELECT 'TRADE','ShippingCostInput','Exporter List',3,2,29,46,46,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D32:J32; write policy incomplete',@c_q062
UNION ALL
SELECT 'TRADE','ShippingCostInput','Atlantic Charter Cost',4,15,24,2,6,15,16,17,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D21:J21; write policy incomplete',@c_q063
UNION ALL
SELECT 'TRADE','ShippingCostInput','Fuel Cost - IFO380',5,26,35,2,6,26,27,28,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D22:J22; write policy incomplete',@c_q064
UNION ALL
SELECT 'TRADE','ShippingCostInput','Suez Canal Cost',6,37,46,2,6,37,38,39,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D23:J23; write policy incomplete',@c_q065
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Canal Cost',7,48,57,2,6,48,49,50,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D24:J24; write policy incomplete',@c_q066
UNION ALL
SELECT 'TRADE','ShippingCostInput','Suez Canal Disruption',8,59,68,2,4,59,60,61,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D25:J25; write policy incomplete',@c_q067
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Canal Disruption',9,70,79,2,4,70,71,72,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D26:J26; write policy incomplete',@c_q068
UNION ALL
SELECT 'TRADE','ShippingCostInput','Technical Parameters',10,81,83,2,5,81,82,83,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D29:J29; write policy incomplete',@c_q069
UNION ALL
SELECT 'TRADE','ShippingCostInput','Panama Spot Capacity',11,86,90,2,5,86,87,88,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D30:J30; write policy incomplete',@c_q070
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','PNG Infrastructure',1,1,207,2,17,1,3,11,'Y','Y','STRUCTURE DRAFT; TRADE SUMMARY!D33:J33; write policy incomplete',@c_q071
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','Pipeline Import Allocation Shares',2,1,186,128,132,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D34:J34; write policy incomplete',@c_q072
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput','Nord Stream Importer Allocation',3,1,8,158,160,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D35:J35; write policy incomplete',@c_q073
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput','Country Supply Adjustment',1,1,674,9,16,1,2,3,'N','N','STRUCTURE DRAFT; TRADE SUMMARY!D45:J45; write policy incomplete',@c_q074
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput','Excess LNG to PNG Countries',2,1,40,20,20,1,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D46:J46; write policy incomplete',@c_q075
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Premium Market Countries',1,1,16,1,1,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D48:J48; write policy incomplete',@c_q076
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Demand Adjustment Parameters',2,1,3,3,6,1,1,2,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D49:J49; write policy incomplete',@c_q077
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput','Demand Adjustment Bands',3,1,7,8,10,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D50:J50; write policy incomplete',@c_q078
UNION ALL
SELECT 'TRADE','CostInput','Regasification Tariff',1,1,379,1,4,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D51:J51; write policy incomplete',@c_q079
UNION ALL
SELECT 'TRADE','CostInput','Liquefaction Cost',2,1,233,8,11,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D52:J52; write policy incomplete',@c_q080
UNION ALL
SELECT 'TRADE','CostInput','LNG Upstream Cost',3,1,227,15,18,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D53:J53; write policy incomplete',@c_q081
UNION ALL
SELECT 'TRADE','CostInput','Non-LNG Upstream Cost',4,1,615,22,25,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D54:J54; write policy incomplete',@c_q082
UNION ALL
SELECT 'TRADE','CostInput','Pipeline Cost',5,1,1554,29,33,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D55:J55; write policy incomplete',@c_q083
UNION ALL
SELECT 'TRADE','CostInput','LNG Project SRMC',6,1,519,37,40,1,2,3,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D56:J56; write policy incomplete',@c_q084
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput','JKM - Base Case',1,2,35,35,37,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D63:J63; write policy incomplete',@c_q085
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput','TTF - Base Case',2,2,59,39,41,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D64:J64; write policy incomplete',@c_q086
UNION ALL
SELECT 'TRADE','LNGExportRestictionInput','LNG Export Restriction',1,2,200,2,5,2,3,4,'Y','N','STRUCTURE DRAFT; TRADE SUMMARY!D65:J65; write policy incomplete',@c_q087
UNION ALL
SELECT 'OUTPUT','USCostofSupply','US Cost of Supply',1,6,816,2,12,6,7,8,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D10:J10; write policy incomplete',@c_q088
UNION ALL
SELECT 'OUTPUT','BCPrices','Base Case Prices',1,6,21,2,5,6,7,8,'Y','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D12:J12; write policy incomplete',@c_q089
UNION ALL
SELECT 'OUTPUT','PremiumCalc','PremiumCalc TTF',1,35,47,2,8,35,38,39,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D13:J13; write policy incomplete',@c_q090
UNION ALL
SELECT 'OUTPUT','PremiumCalc','PremiumCalc East Asia Spot',2,53,61,2,8,53,56,57,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D14:J14; write policy incomplete',@c_q091
UNION ALL
SELECT 'OUTPUT','PriceSeasonality','Price Seasonality',1,1,4,2,15,1,1,2,'N','N','STRUCTURE DRAFT; OUTPUT SUMMARY!D27:J27; write policy incomplete',@c_q092
) x LEFT JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@c_tmplt_id AND f.mdl_file_cd=x.role AND f.del_yn='N'
LEFT JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet AND s.del_yn='N'
LEFT JOIN tmd_mdl_inpttbld t ON t.inpt_sht_id=s.inpt_sht_id AND BINARY t.inpt_tbl_nm=BINARY x.name
WHERE t.inpt_tbl_id IS NULL OR t.del_yn<>'N' OR t.tbl_ord<>x.ord OR t.tbl_bgn_row_no<>x.br OR t.tbl_end_row_no<>x.er
 OR t.tbl_bgn_col_no<>x.bc OR t.tbl_end_col_no<>x.ec OR t.hdr_bgn_row_no<>x.hr OR t.hdr_end_row_no<>x.he OR t.data_bgn_row_no<>x.dr
 OR t.row_add_psbl_yn<>x.ra OR t.col_add_psbl_yn<>x.ca OR NOT (BINARY t.inpt_tbl_expn <=> BINARY x.description)
 OR NOT (BINARY t.qry_json <=> BINARY x.qry) OR t.crt_usr_logn_id<>@c_actor OR t.mod_usr_logn_id<>@c_actor;

-- 파일4개·시트70개도 실제 부모에 속한 정확한 이름/순서/상태인지 확인. 두 조회 모두 예상0건.
SELECT f.mdl_file_cd AS issue FROM tmd_mdl_inptfiled f WHERE f.tmplt_id=@c_tmplt_id AND (f.del_yn<>'N' OR f.file_type_cd<>'XLSM' OR f.file_strt_hash_val IS NOT NULL OR BINARY f.file_nm<>BINARY CONCAT(f.mdl_file_cd,'-normalized.xlsm') OR f.file_ord<>FIELD(f.mdl_file_cd,'DEMAND','SUPPLY','TRADE','OUTPUT') OR f.crt_usr_logn_id<>@c_actor OR f.mod_usr_logn_id<>@c_actor);

SELECT x.role,x.sheet FROM (
SELECT 'DEMAND' AS role,'Input Adjustment ->' AS sheet,1 AS ord,'VISIBLE' AS state
UNION ALL
SELECT 'DEMAND','GDPPopulationAdjustment',2,'VISIBLE'
UNION ALL
SELECT 'DEMAND','ElectrificationRateAdjustment',3,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PowerMixAdjustment',4,'VISIBLE'
UNION ALL
SELECT 'DEMAND','CoalGasAdjustment',5,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherAdjustment',6,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMIAdjustment',7,'VISIBLE'
UNION ALL
SELECT 'DEMAND','HydrogenAdjustment',8,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringAdjustment',9,'VISIBLE'
UNION ALL
SELECT 'DEMAND','Scenario Tracker',10,'HIDDEN'
UNION ALL
SELECT 'DEMAND','DemandOutputOverwrite',11,'VISIBLE'
UNION ALL
SELECT 'DEMAND','기준정보 ->',12,'VISIBLE'
UNION ALL
SELECT 'DEMAND','BCInput',13,'VISIBLE'
UNION ALL
SELECT 'DEMAND','MacroParametersLogicInput',14,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMIInput',15,'VISIBLE'
UNION ALL
SELECT 'DEMAND','PMILogicInput',16,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherLogicInput',17,'VISIBLE'
UNION ALL
SELECT 'DEMAND','WeatherInput',18,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringInput',19,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNGBunkeringLogicInput',20,'VISIBLE'
UNION ALL
SELECT 'DEMAND','HydrogenInput',21,'VISIBLE'
UNION ALL
SELECT 'DEMAND','LNG Demand',22,'VISIBLE'
UNION ALL
SELECT 'DEMAND','General',23,'VISIBLE'
UNION ALL
SELECT 'DEMAND','Sheet2_DISCARDAFTUSE',24,'HIDDEN'
UNION ALL
SELECT 'DEMAND','CubeDump_DISCARDAFTUSE',25,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','Input Upstream ->',1,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','ProductionAdjustment',2,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','Input Midstream ->',3,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','{USA}Hurricanes',4,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment',5,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment',6,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','RegasAdjustment',7,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','PipelineAdjustment',8,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCProduction',9,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCHurricanes',10,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCLiquefaction',11,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCRegasification',12,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCPipeline',13,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LNG Summary',14,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqUtilAdjustment_backup',15,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','LiqProdAdjustment_backup',16,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','BCLiquefaction_backup',17,'VISIBLE'
UNION ALL
SELECT 'SUPPLY','{USA}RigsLogic',18,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','{China}PolicyLogic',19,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','{USA}HurricanesLogic',20,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','RegasLogic',21,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','PipelineLogic',22,'HIDDEN'
UNION ALL
SELECT 'SUPPLY','MonthlyAdjProduction',23,'HIDDEN'
UNION ALL
SELECT 'TRADE','LNG_TradeRestrictionInput',1,'VISIBLE'
UNION ALL
SELECT 'TRADE','PNG_TradeRestrictionInput',2,'VISIBLE'
UNION ALL
SELECT 'TRADE','Sheet1',3,'HIDDEN'
UNION ALL
SELECT 'TRADE','MidstreamContractInput',4,'VISIBLE'
UNION ALL
SELECT 'TRADE','ShippingCostInput',5,'VISIBLE'
UNION ALL
SELECT 'TRADE','MidstreamCapacityInput',6,'VISIBLE'
UNION ALL
SELECT 'TRADE','UpstreamAdjLogicInput',7,'VISIBLE'
UNION ALL
SELECT 'TRADE','DemandAdjLogicInput',8,'VISIBLE'
UNION ALL
SELECT 'TRADE','CostInput',9,'VISIBLE'
UNION ALL
SELECT 'TRADE','4.5 PriceModelInput',10,'VISIBLE'
UNION ALL
SELECT 'TRADE','LNGExportRestictionInput',11,'VISIBLE'
UNION ALL
SELECT 'TRADE','backupLNG_TradeRestrictionInput',12,'VISIBLE'
UNION ALL
SELECT 'TRADE','backup_MidstreamContractInput',13,'VISIBLE'
UNION ALL
SELECT 'TRADE','Industry research',14,'HIDDEN'
UNION ALL
SELECT 'TRADE','net exporter',15,'HIDDEN'
UNION ALL
SELECT 'TRADE','pipeline cap',16,'HIDDEN'
UNION ALL
SELECT 'TRADE','pipeline contract',17,'HIDDEN'
UNION ALL
SELECT 'OUTPUT','USCostofSupply',1,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','BCPrices',2,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','PremiumCalc',3,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','AnnualPriceCalc',4,'VISIBLE'
UNION ALL
SELECT 'OUTPUT','PriceSeasonality',5,'VISIBLE'
) x LEFT JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@c_tmplt_id AND f.mdl_file_cd=x.role
LEFT JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
WHERE s.inpt_sht_id IS NULL OR s.sht_ord<>x.ord OR s.sht_stat_cd<>x.state OR s.del_yn<>'N' OR s.crt_usr_logn_id<>@c_actor OR s.mod_usr_logn_id<>@c_actor;

SET @c_table_count=(SELECT COUNT(*) FROM tmd_mdl_inpttbld t JOIN tmd_mdl_inptshtd s ON s.inpt_sht_id=t.inpt_sht_id JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id WHERE f.tmplt_id=@c_tmplt_id);
SET @c_scope_count=(SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id=m.inpt_tbl_id JOIN tmd_mdl_inptshtd s ON s.inpt_sht_id=t.inpt_sht_id JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id WHERE f.tmplt_id=@c_tmplt_id);
SELECT @c_created AS must_be_1_new_root,@c_ok AS must_be_1_all_inserts,@c_table_count AS must_be_92_tables,@c_scope_count AS must_be_0_write_scopes;
-- 건수만으로 실DB의 DDL/SQL·reader 전체 검증 완료가 되지 않는다. 위 이상 결과0건도 확인한다.

-- 4. 사용자 COMMIT / ROLLBACK 판단
-- 첫 실행은 아래 ROLLBACK까지 진행해 결과를 확인한다. NULL 파라미터·재실행·오류·차이는 반드시 ROLLBACK한다.
-- SQL/DDL 호환과92개 구조를 확인하고 DRAFT 부분 등록을 보존할 때만 마지막 ROLLBACK을 COMMIT으로 교체한다.
-- COMMIT; -- user decision only; 이 파일의 기본 동작은 ROLLBACK이며 발행하지 않는다.
ROLLBACK;
-- COMMIT 뒤에도 DRAFT다. ACTIVE 발행·Case 연결·기존 Draft 전환은 별도 작업이다.
-- 재실행으로 동일marker/version을 수정하지 않는다. 보존 뒤 삭제/UPDATE 복구 SQL은 포함하지 않는다.
