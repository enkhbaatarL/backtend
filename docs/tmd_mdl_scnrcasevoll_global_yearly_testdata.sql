-- =============================================================================
-- tmd_mdl_scnrcasevoll 테스트 데이터 (Global Volumes, Yearly)
-- source: 06120948_result_output_dashboard 2.xlsx / 'Global Dashboard (Yearly)' 시트
-- run_seq = 2 (고정), sgmt_cd = GLOBAL, perd_unit_spt_cd = Y (연)
-- unit: mtpa, 기간: 2026 ~ 2030
-- row_kind: REGION_SUBTOTAL(지역), COUNTRY(국가), TYPE_TOTAL(타입합계)
-- =============================================================================

DELETE FROM tmd_mdl_scnrcasevoll WHERE run_seq = 2 AND sgmt_cd = 'GLOBAL';

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 797.019532488212),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 828.2984837579565),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 862.3536619333956),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 892.1531942337672),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 925.2638612744169);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2026', 339.6808186393511),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2027', 360.3115966820461),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2028', 381.6236671609719),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2029', 401.1666526764258),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2030', 421.6115114016674);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2026', 67.1059832622915),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2027', 65.85789961464816),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2028', 65.43122773713243),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2029', 64.44053425433643),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2030', 65.69337595587564);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2026', 55.49306756520848),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2027', 57.96465435001556),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2028', 59.95767396403992),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2029', 62.34790920224415),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2030', 65.15506477144336);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2026', 26.84384131451994),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2027', 27.85595301391005),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2028', 30.0494860948996),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2029', 30.92085666028916),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2030', 34.07764984651843);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2026', 37.80817685345271),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2027', 38.68791593287276),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2028', 38.63218590859135),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2029', 38.66141941375575),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2030', 37.83972385885284);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2026', 45.81050848386968),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2027', 45.6798913833302),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2028', 46.77449024532145),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2029', 48.09441925644695),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2030', 49.81333916876634);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2026', 29.41702769546033),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2027', 29.57823342354424),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2028', 30.98321745121601),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2029', 33.26547926933187),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2030', 33.52192146349634);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2026', 36.16681229608854),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2027', 36.12157217995155),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2028', 36.10841589834654),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2029', 36.06823204541713),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2030', 36.03415392877465);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 29.55991264214503),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 31.52067489546882),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 31.42747756947747),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 31.4217219828402),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 31.86467217189496);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2026', 28.74742433908787),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2027', 29.34508358421336),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2028', 29.96861733247876),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2029', 30.55770044923206),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2030', 30.89898525883763);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2026', 19.04602992806063),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2027', 20.0664469476241),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2028', 21.88924706339267),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2029', 23.55747358361307),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2030', 25.68010453106728);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2026', 24.50521360638991),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2027', 26.18530543013284),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2028', 26.22406618360914),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2029', 26.09126081152122),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2030', 25.85210095819218);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2026', 19.23694639791143),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2027', 19.23650539784087),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2028', 18.85043703106993),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2029', 18.99324608391938),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2030', 19.36644543328158);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2026', 10.75576292607291),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2027', 10.91500946680151),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2028', 11.07529789554766),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2029', 11.23428301906747),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2030', 11.37703345132534);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2026', 5.518125436990663),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2027', 6.225744359572979),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2028', 8.120892906891761),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2029', 8.505550738842961),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2030', 8.803483980607115);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2026', 11.37057542757023),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2027', 11.56476301827524),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2028', 11.77922828113518),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2029', 12.07461903044325),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2030', 12.28722446160084);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2026', 2.826577780652441),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2027', 3.179137942196703),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2028', 3.175207600783213),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2029', 3.292428908051454),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2030', 3.509928734388595);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2026', 2.282218048418638),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2027', 2.449085349646351),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2028', 2.608878842125103),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2029', 2.821362703896464),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2030', 3.027426504571731);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2026', 1.034490983018556),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2027', 1.795698632311781),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2028', 3.743310763929722),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2029', 4.499009219841475),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2030', 4.636881276901006);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2026', 0.1555883867826254),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2027', 0.2372281033500406),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2028', 0.4351471729678133),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2029', 0.5145439485823922),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2030', 0.6038025966084151);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2026', 3.654430474868873),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2027', 3.520084050203444),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2028', 3.495488829468209),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2029', 3.624490975668551),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2030', 3.609031519745038);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 330.2441278228358),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 330.440310044705),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 330.2578335196425),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 328.1200282278223),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 318.8061403412897);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2026', 54.64717096441815),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2027', 54.31260087404321),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2028', 54.28054449030502),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2029', 53.94580392271046),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2030', 52.35683719910208);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2026', 48.78622799209008),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2027', 49.024172861734),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2028', 45.42627481905637),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2029', 45.41941065479856),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2030', 43.82724520790261);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2026', 41.10628032904516),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2027', 40.68299272936813),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2028', 39.29534401961782),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2029', 38.70864134988474),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2030', 36.53294041473144);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2026', 17.09268741767998),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2027', 18.02297926265139),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2028', 18.86713826101134),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2029', 19.23891142072583),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2030', 19.5623935707071);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 26.60137310566707),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 25.6899372958113),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 26.53042148513164),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 25.61564627608949),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 25.6551691356422);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2026', 19.84712729474041),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2027', 19.75532280296386),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2028', 19.53318705550048),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2029', 18.86744398902514),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2030', 17.55818195411891);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2026', 17.63912032640645),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2027', 19.0465532826249),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2028', 19.72956551867742),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2029', 19.14470866008347),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2030', 18.06311305216587);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2026', 12.61410737616718),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2027', 12.90808132408801),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2028', 12.61318507817461),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2029', 12.69109373677),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2030', 12.1656168900087);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2026', 13.98360590614605),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2027', 14.01344879725807),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2028', 15.49388521705196),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2029', 16.25112668420135),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2030', 16.21703217316624);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 12.26480323335003),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 12.40625640475281),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 12.59921699951264),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 12.10581992897784),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 11.51431590170596);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2026', 7.70686461885833),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2027', 7.944885842721725),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2028', 8.243601316695084),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2029', 8.080253044767312),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2030', 7.851307198649899);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2026', 8.294218346842971),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2027', 8.410050864805285),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2028', 8.14543721765475),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2029', 8.56336200614726),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2030', 8.469774716320089);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2026', 7.536484071225114),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2027', 7.375115238318598),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2028', 7.208020870172235),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2029', 7.056522323975153),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2030', 6.900623098234545);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2026', 5.338547512437056),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2027', 5.338149615975833),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2028', 5.752690335417401),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2029', 5.4845071940471),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2030', 5.935242168573743);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2026', 6.103584141330407),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2027', 5.9671796976884),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2028', 5.696812822975653),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2029', 5.608790322732701),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2030', 5.496191052181397);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2026', 5.029759804750597),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2027', 4.980730926111321),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2028', 5.20305259739282),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2029', 5.114994908617239),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2030', 5.031468188956462);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2026', 2.928979655853493),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2027', 2.868795785883159),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2028', 2.869786567044499),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2029', 2.872919313584091),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2030', 2.824379013100639);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2026', 4.638042321901552),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2027', 4.511038057749973),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2028', 4.600006293362773),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2029', 4.643794482067421),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2030', 4.522261354151055);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2026', 3.717442949391212),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2027', 3.49984427552508),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2028', 3.299085365853657),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2029', 3.150190291530443),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2030', 2.90882839511254);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2026', 3.069590810734529),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2027', 3.190432060999129),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2028', 3.307530569389891),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2029', 3.509573602176776),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2030', 3.723286836000892);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2026', 2.74727230794558),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2027', 2.19910314160602),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2028', 3.102519342272487),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2029', 3.552746167029806),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2030', 3.221632862043028);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2026', 2.381253747560293),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2027', 2.246651715581229),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2028', 2.483494235619231),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2029', 2.554575861702252),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2030', 2.562628618280587);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2026', 1.860196256056397),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2027', 1.776378674853585),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2028', 1.724689389685298),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2029', 1.665579726932752),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2030', 1.610253785985602);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2026', 1.854018515019612),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2027', 1.833503980509011),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2028', 1.817389699935645),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2029', 1.818833580099054),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2030', 1.821125350746399);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2026', 0.9216216467415096),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2027', 0.902580978328243),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2028', 0.8978407347574823),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2029', 0.9137291344479513),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2030', 0.9302254706136646);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2026', 0.9023923201737952),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2027', 0.9045703115812469),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2028', 0.9082953575922544),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2029', 0.9103234974088598),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2030', 0.9174449497426788);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2026', 0.6313548503027637),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2027', 0.6289532411714852),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2028', 0.6288178597820683),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2029', 0.6307261472892395),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2030', 0.6266217833454827);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 847.0482826636586),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 851.4081465804888),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 853.1837978036841),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 854.8008291907981),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 852.350144151586);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 684.536278797627),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 686.3239767491073),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 686.0567484770224),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 685.6877499357282),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 682.4338235512586);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 98.59502500316498),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 100.4044222661926),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 102.3061443871669),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 104.127893583311),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 104.5744780923992);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 63.91697886286659),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 64.67974756518896),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 64.82090493949477),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 64.98518567175897),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 65.34184250792825);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 490.1045070342952),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 499.0692704060603),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 512.1231702779921),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 527.8126197028326),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 541.0397952973262);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2026', 185.6890410012466),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2027', 187.6705147786083),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2028', 189.3561293864807),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2029', 192.1279409559705),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2030', 195.7675346730204);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2026', 71.664756501811),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2027', 75.57143599912973),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2028', 81.53712183508945),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2029', 88.17015515852478),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2030', 93.60640759137516);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 59.556912529106),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 59.5925967848155),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 60.21676623468259),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 62.68452912952465),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 64.40378472120169);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2026', 44.39048458068332),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2027', 45.69698439263528),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2028', 47.37346452399548),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2029', 48.99902820700837),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2030', 49.74406791401087);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2026', 32.37448549991768),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2027', 32.68137230401958),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2028', 32.96549394947903),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2029', 33.13504598160736),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2030', 33.3509868111579);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2026', 25.6244678092252),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2027', 25.03180786508927),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2028', 26.15702983512477),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2029', 27.93726080794289),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2030', 29.39996937121192);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 22.74881345403014),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 24.56483870642918),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 25.13095104605215),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 24.73054661236809),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 23.98995468890256);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2026', 17.43601603476256),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2027', 17.36826886892302),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2028', 17.62728206338754),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2029', 17.47313354070137),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2030', 17.65987283526271);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2026', 14.01899898598288),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2027', 14.1915755756521),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2028', 14.35509890764117),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2029', 14.49900407484066),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2030', 14.60604563681075);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2026', 12.20582759917634),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2027', 12.39678745253974),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2028', 12.4558581357656),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2029', 12.8938456235153),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2030', 12.94857681927229);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2026', 3.051998919265741),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2027', 3.054567795730457),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2028', 3.054631323978731),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2029', 3.061216336432426),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2030', 3.052886933461909);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 1.342704119087655),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 1.248519882488178),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 1.893343036314881),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 2.100913274396121),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 2.509707301638164);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 35.78915258602441),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 34.8248974455825),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 35.00951469530229),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 34.72206706684001),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 35.99816686279633);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2026', 31.55559364489498),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2027', 30.86769317214482),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2028', 30.4367494178799),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2029', 29.69028645894583),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2030', 30.42170334893718);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2026', 2.58523995989839),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2027', 2.390749744954956),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2028', 2.289531907090101),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2029', 2.61193727949996),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2030', 2.914928497718555);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2026', 1.648318981231036),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2027', 1.566454528482721),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2028', 2.283233370332288),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2029', 2.419843328394219),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2030', 2.661535016140599);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 347.5878311285766),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 350.8545234298163),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 353.2108930625458),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 356.681402612718),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 361.3817356555155);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 347.5878311285766),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 350.8545234298163),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 353.2108930625458),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 356.681402612718),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 361.3817356555155);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 128.1099602807307),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 132.5370531917447),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 136.9638998102764),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 138.1919448732392),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 139.9544849807941);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 48.61500178371027),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 51.86419286069362),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 51.4839618564339),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 50.62457957993271),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 49.66063185550565);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2026', 40.16102070936332),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2027', 40.14017941352872),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2028', 40.62983691253839),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2029', 40.35175417333068),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2030', 40.1731904999321);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2026', 14.89967311310768),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2027', 13.30703532043064),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2028', 11.71335893510242),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2029', 11.81595080497212),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2030', 14.11068040798385);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2026', 8.431587438553988),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2027', 10.37611426017828),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2028', 13.01303743208599),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2029', 13.76273902203825),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2030', 13.12524916503987);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2026', 2.991404614624737),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2027', 3.430826406913402),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2028', 3.490641419093363),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2029', 4.447769369911153),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2030', 5.274162393865982);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2026', 0.2847681312049938),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2027', 0.3160756088843538),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2028', 1.376943846118257),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2029', 1.709109738457558),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2030', 1.87937156163489);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2026', 3.475209186913467),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2027', 3.43325589690594),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2028', 3.380135814791727),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2029', 3.272883877331418),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2030', 3.18404659165745);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2026', 0.7999197379871579),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2027', 0.9165311816449888),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2028', 1.983249347319895),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2029', 2.42071112731378),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2030', 2.415939506550321);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2026', 2.922859873747576),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2027', 2.7583525149264),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2028', 3.062493285088923),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2029', 3.161601535856244),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2030', 3.227700096432013);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2026', 0.003935277359643496),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2027', 0.344193021320883),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2028', 0.5003913875626198),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2029', 0.3572731936637094),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2030', 0.4836639608862321);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2026', 0.09965660794505588),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2027', 0.1657315750170501),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2028', 0.6436240209798415),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2029', 0.5956260437001663),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2030', 0.3107994008279032);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 5.424923806212794),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 5.484565131300406),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 5.686225553161072),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 5.671946406731407),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 6.109049540477908);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 107.1153049550542),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 105.6020772570111),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 106.8119180557924),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 107.8483370667846),
    (2, 'GLOBAL', 'DEMAND', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 106.1922961549568);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 34.17101541736248),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 33.97481116652103),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 33.83279261824682),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 33.83498291859727),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 33.67849257355881);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2026', 23.80979198880742),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2027', 24.13533222529217),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2028', 27.98959535305945),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2029', 30.71867846522506),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2030', 31.75947210660724);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2026', 9.476509616741533),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2027', 8.976029485999712),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2028', 8.450989341763288),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2029', 7.430418928017021),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2030', 5.620973556900763);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2026', 12.83532607618716),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2027', 11.82239858909377),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2028', 11.03860588117694),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2029', 11.00417421266787),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2030', 10.63454235952678);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2026', 6.424574717817555),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2027', 6.907378569199704),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2028', 6.6625552939559),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2029', 6.226454240095165),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2030', 6.06377385864181);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2026', 4.004012914223175),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2027', 3.872444107761868),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2028', 3.486994016033928),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2029', 3.217499758280503),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2030', 3.282578718002384);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2026', 5.726413999106235),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2027', 5.274083748853401),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2028', 4.913883568631365),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2029', 4.684479223016676),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2030', 4.417180039748805);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2026', 3.430401557275055),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2027', 3.338128704400589),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2028', 3.069600079086009),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2029', 2.99027958669473),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2030', 2.901259569701527);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2026', 0.5593531103446779),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2027', 0.5726797525753253),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2028', 0.5912587381638222),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2029', 0.6036031320076155),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2030', 0.615479616976738);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2026', 6.677905557188888),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2027', 6.728790907313544),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2028', 6.775643165674906),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2029', 7.137766602182657),
    (2, 'GLOBAL', 'DEMAND', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2030', 7.218543755291999);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'DEMAND', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 3083.018698959388),
    (2, 'GLOBAL', 'DEMAND', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 3133.034762113365),
    (2, 'GLOBAL', 'DEMAND', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 3189.914689158632),
    (2, 'GLOBAL', 'DEMAND', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 3240.330422974802),
    (2, 'GLOBAL', 'DEMAND', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 3280.986624718681);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 544.0300204082475),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 540.3321983365498),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 548.391449716208),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 548.927940790838),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 551.763855572035);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2026', 206.5350330478102),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2027', 208.1226333032962),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2028', 217.0602347281674),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2029', 224.3514358947596),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2030', 232.1718371482288);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2026', 1.244734759037558),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2027', 1.115032854505253),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2028', 1.172200194922029),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2029', 1.053430092668812),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2030', 0.9990232678987206);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2026', 30.21815739954016),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2027', 29.24555693237408),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2028', 28.95419657798643),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2029', 28.62912991935576),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2030', 29.05180372777357);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2026', 41.04029890930306),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2027', 40.86103247106475),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2028', 43.14260820037637),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2029', 42.93817333535004),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2030', 42.89939589599899);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2026', 19.75758344603334),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2027', 18.72739750643859),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2028', 17.74110105443116),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2029', 16.13505104502315),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2030', 14.69849340263893);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2026', 0.3762874262059876),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2027', 0.3762874262059876),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2028', 0.3762874262059876),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2029', 0.3762874262059876),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2030', 0.3762874262059876);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2026', 18.23257898825762),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2027', 17.12858851459415),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2028', 16.3337369691829),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2029', 15.61891683032668),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2030', 14.61715701807511);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2026', 37.18111245837657),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2027', 36.43148185652208),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2028', 35.16180168961825),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2029', 33.29259387274499),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2030', 31.40101478305735);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 50.94299290504383),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 49.17287559196006),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 48.96727769603157),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 45.4911859434097),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 43.96877114869334);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2026', 61.52190925100545),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2027', 63.06036864665897),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2028', 62.16240641732011),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2029', 62.6634203185455),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2030', 65.96220202298993);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2026', 10.06933723422395),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2027', 8.58891240856598),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2028', 7.716654619769733),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2029', 6.738869061199044),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2030', 5.887299558852924);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2026', 0.0280526745184274),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2027', 0.02569798918667783),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2028', 0.02431126740480235),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2029', 0.02294953562692511),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2030', 0.02173772329303521);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2026', 18.98130853393936),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2027', 19.29598788940307),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2028', 18.70758094041795),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2029', 18.57837857430557),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2030', 16.79026203311223);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2026', 28.77357611329802),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2027', 28.43075491309008),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2028', 28.40275321195051),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2029', 28.62505541378606),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2030', 28.80676424944727);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2026', 3.916537456340989),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2027', 3.978153019674479),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2028', 5.890960512853677),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2029', 7.687771422313422),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2030', 7.726797888197657);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2026', 0.05643062480889836),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2027', 0.0506713836524197),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2028', 0.05685539093186057),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2029', 0.2048967868734833),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2030', 0.2750071691911448);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2026', 9.69878257520521),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2027', 9.545065835745163),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2028', 9.099823552931761),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2029', 9.113553180198501),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2030', 8.898936034409758);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2026', 0.5217678149328494),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2027', 0.4809533539525356),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2028', 0.4071950190512019),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2029', 0.3420318122750891),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2030', 0.2885995013759193);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2026', 4.9335387903662),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2027', 5.694746439659427),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2028', 7.013464246654275),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2029', 7.064810325869649),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2030', 6.922465572594491);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 145.9559480838715),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 141.9975398728462),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 147.0447679889378),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 143.3347400691125),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 139.2714559121786);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2026', 2.240671952542507),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2027', 1.849343109079893),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2028', 1.534233527177361),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2029', 1.348625812175126),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2030', 1.193426076628166);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2026', 17.51833096981794),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2027', 16.43580380641859),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2028', 15.44191669361665),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2029', 14.25250048730506),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2030', 14.97305549691886);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2026', 3.151482229902149),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2027', 2.906957926388259),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2028', 2.680360782892718),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2029', 2.300750721770107),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2030', 2.139654215519667);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2026', 12.58303058482488),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2027', 12.09110911150245),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2028', 11.6293444504551),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2029', 11.12545360826257),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2030', 10.94598084442693);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 0.0178149365253894),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 0.01646569754451132),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 0.0449870926629338),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 0.05506889744102278),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 0.0833779061904639);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2026', 6.70021586218453),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2027', 6.255529438224701),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2028', 5.968129344585686),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2029', 5.347729848501767),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2030', 4.842527544864399);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2026', 0.0155911921995898),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2027', 0.01426694028270993),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2028', 0.01325501417080183),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2029', 0.01238050662088055),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2030', 0.01168090190394386);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2026', 3.176256213690982),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2027', 3.673275301644037),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2028', 3.844966094444563),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2029', 3.773856166356975),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2030', 3.618418986817025);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 0.007695649681303582),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 0.006146528888444108),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 0.004897233753557033),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 0.003872812639649655),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 0.003098250405719771);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2026', 4.989856902827098),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2027', 5.570016825622684),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2028', 7.475577984247469),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2029', 8.208161654117685),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2030', 8.479060927834738);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2026', 0.2903984817837555),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2027', 0.395551592598253),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2028', 1.02784451513512),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2029', 0.4873567675870805),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2030', 0.6180568787190982);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2026', 0.8958440660500461),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2027', 0.8317802151198302),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2028', 0.7455414334266249),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2029', 0.6800908871745376),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2030', 0.6408630128830779);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2026', 0.003398081943692014),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2027', 0.002923349777735009),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2028', 0.002248729919796212),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2029', 0.001898927193828168),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2030', 0.001624081984853117);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2026', 0.4012359122527434),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2027', 0.3578104001046613),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2028', 0.329626294335204),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2029', 0.2963950750582093),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2030', 0.2689481104616954);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2026', 89.80297497458594),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2027', 87.98622013897516),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2028', 92.66360597139193),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2029', 91.99025739980613),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2030', 87.06711001095839);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2026', 0.02943337370933847),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2027', 0.0265100246666027),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2028', 0.02281211706493748),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2029', 0.01995123274219599),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2030', 0.01742765687842363);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2026', 0.03445554131288617),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2027', 0.03444304631088682),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2028', 0.03458046928287465),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2029', 0.03323122956699621),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2030', 0.03000805045628763);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2026', 0.7614446868311491),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2027', 0.7012289422466297),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2028', 0.6360157326775161),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2029', 0.5963756938401095),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2030', 1.48178795246607);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2026', 2.7584800190218),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2027', 2.318090958469549),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2028', 2.419970923395345),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2029', 2.373636072989167),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2030', 2.50923196223712);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2026', 0.01492906881365004),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2027', 0.0247735111837609),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2028', 0.0888623234529708),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2029', 0.04818528764964499),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2030', 0.02799668882446933);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2026', 0.5624073833701787),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2027', 0.4992930077968785),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2028', 0.4359912608485991),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2029', 0.3789609803137547),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2030', 0.3181203547992551);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 976.4285452232774),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 1012.462146058435),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 1032.717587863255),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 1052.332833509314),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 1086.356623172065);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 808.2765220928366),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 835.5463078981105),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 852.4633526602778),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 867.5452443383643),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 896.0251803705095);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 153.0124254221717),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 160.957675753963),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 165.5073264819071),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 170.8213773350953),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 176.8998283076474);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 15.13959770826906),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 15.95816240636097),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 14.74690872107038),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 13.96621183585388),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 13.4316144939083);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Other America N', 'NORTH_AMERICA', 'Other America N', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 557.5536343394187),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 575.7378777147948),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 606.8647366423356),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 642.4861934673146),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 668.6999740935213);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2026', 195.4664581204537),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2027', 195.2318087029504),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2028', 194.7408716660395),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2029', 197.5126832355293),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2030', 201.1522769525791);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2026', 71.664756501811),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2027', 75.57143599912972),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2028', 81.53712183508944),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2029', 88.17015515852478),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2030', 93.60640759137516);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 51.37296889807978),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 51.82710042784976),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 52.02474811005385),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 54.3961433939822),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 59.64318641768254);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2026', 2.769873167644704),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2027', 2.840270964408351),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2028', 4.983510536226682),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2029', 7.326399412293901),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2030', 8.997693584330966);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2026', 134.0702917172374),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2027', 142.3350194816055),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2028', 162.343650364136),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2029', 179.4596582099152),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2030', 184.6167017266549);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2026', 21.23179296957686),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2027', 22.85525622030598),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2028', 26.15702983512477),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2029', 27.93726080794288),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2030', 29.39996937121192);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 32.60307648260972),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 35.29213568156026),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 35.11221277016759),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 34.1673866392704),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 34.25863272359479);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2026', 14.39137202471951),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2027', 15.61037118828938),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2028', 15.72148324273231),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2029', 16.11853385043041),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2030', 16.7915781731425);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2026', 12.99141372862618),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2027', 13.08011354281816),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2028', 12.42423618787778),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2029', 11.75711333113813),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2030', 11.19617594138814);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2026', 19.33532873989652),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2027', 19.52628859325992),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2028', 19.58535927648578),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2029', 23.18384726991556),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2030', 26.17857893607263);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2026', 0.3135978696756588),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2027', 0.3195570301291242),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2028', 0.3411697820871645),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2029', 0.3560988839758208),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2030', 0.3490653738504593);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 1.342704119087656),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 1.248519882488179),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 1.893343036314882),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 2.100913274396121),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 2.509707301638164);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 125.2541685026591),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 126.8482830230675),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 125.1505521357147),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 123.4752372401937),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 121.9016146648155);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2026', 112.6169059290906),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2027', 114.4873751171907),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2028', 112.1740832258533),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2029', 110.0397529998605),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2030', 107.7339474885173);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2026', 2.585239959898391),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2027', 2.390749744954956),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2028', 2.289531907090102),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2029', 2.611937279499962),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2030', 2.914928497718556);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2026', 10.05202261367009),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2027', 9.970158160921779),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2028', 10.68693700277134),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2029', 10.82354696083328),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2030', 11.25273867857966);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 438.6960201022121),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 436.3729003975419),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 432.2410422582364),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 437.360178009944),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 439.1415407905369);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 438.6960201022121),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 436.3729003975419),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 432.2410422582364),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 437.360178009944),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 439.1415407905369);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 183.1800852209263),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 193.8995471150272),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 200.6089652289481),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 198.1823346982992),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 196.8312761330149);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 37.99050008378999),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 42.3346726691676),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 41.80174783468962),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 37.5237857268857),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 30.17198910509323);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2026', 77.83051857979798),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2027', 78.80178526938566),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2028', 79.34644615219587),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2029', 75.18901220229198),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2030', 73.58283422047509);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 9.878050369263441),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 10.93632991508277),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 11.39272145920041),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 11.39455821934929),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 11.83291055929567);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2026', 31.31014739605754),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2027', 32.91456458225673),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2028', 34.37439574802424),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2029', 33.78797346885097),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2030', 35.73316080834829);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2026', 10.26908773255403),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2027', 12.21361455417833),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2028', 14.85053772608603),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2029', 15.55764926970387),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2030', 14.92644439283109);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2026', 0.3972630984370935),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2027', 0.5133101281546186),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2028', 0.9113853458616529),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2029', 2.018872024719521),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2030', 2.260598038725682);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2026', 0.003985249282639371),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2027', 0.003835333513653068),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2028', 1.376947141731541),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2029', 1.709109739927557),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2030', 1.879376107645176);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2026', 1.022435075969609),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2027', 0.894295010672199),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2028', 0.8411749285579861),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2029', 0.7339229910976756),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2030', 0.6450857054237098);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2026', 6.794061798174886),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2027', 6.962532149005141),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2028', 7.712552508508393),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2029', 12.8336079552344),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2030', 18.88941461013966);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2026', 1.222859601747535),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2027', 1.058352242926357),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2028', 1.36249301308888),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2029', 1.837287286555964),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2030', 1.903125216090032);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2026', 3.801519218306405),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2027', 4.540523276067053),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2028', 4.354939087623582),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2029', 4.040929616382067),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2030', 3.825537828919382);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2026', 2.659657017545122),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2027', 2.725731984617116),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2028', 2.283624283379882),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2029', 1.555626197300188),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2030', 1.180799540027923);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 103.3588855861415),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 100.5899487390254),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 103.1471295819322),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 106.9935683330404),
    (2, 'GLOBAL', 'PRODUCTION', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 108.5911925053125);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 34.31818890091023),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 35.22620703280561),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 37.43496124061915),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 39.61395053028431),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 44.21843660956076);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2026', 17.74252909865464),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2027', 18.51087022249922),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2028', 23.61342612807816),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2029', 26.89837046945926),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2030', 28.1904288718336);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2026', 9.476509616741533),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2027', 8.976029485999712),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2028', 8.450989341763288),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2029', 7.995100040865998),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2030', 7.589861736922872);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2026', 18.21513834272118),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2027', 16.04173529759885),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2028', 15.49443120370515),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2029', 15.64108922805161),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2030', 13.05160120359707);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2026', 6.234253994725633),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2027', 5.705015131447415),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2028', 4.816242182538742),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2029', 4.267426756503274),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2030', 3.801877569560405);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2026', 0.4007486975797906),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2027', 0.4032347921125657),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2028', 0.3989371949749502),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2029', 0.3985249120939849),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2030', 0.5667174064197836);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2026', 0.2285084819013561),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2027', 0.1634451969212306),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2028', 0.09353467510054699),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2029', 0.08049203553872494),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2030', 0.05516883834701318);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2026', 9.52641460710633),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2027', 9.074084356853495),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2028', 8.65388416703146),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2029', 8.254479794216763),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2030', 7.667180559748886);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2026', 7.137188695360994),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2027', 6.481456667333062),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2028', 4.184352045946322),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2029', 3.835514433932303),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2030', 3.443498335959728);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2026', 0.07940515043982267),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2027', 0.007870555454288358),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2028', 0.006371402174423908),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2029', 0.008620132094220695),
    (2, 'GLOBAL', 'PRODUCTION', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2030', 0.006421373362419371);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PRODUCTION', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 3074.457307466754),
    (2, 'GLOBAL', 'PRODUCTION', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 3128.240441257287),
    (2, 'GLOBAL', 'PRODUCTION', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 3196.166231415569),
    (2, 'GLOBAL', 'PRODUCTION', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 3253.093026118056),
    (2, 'GLOBAL', 'PRODUCTION', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 3312.55753284348);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 283.0651782988363),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 311.0892682599204),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 335.547655475907),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 357.3740502315958),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 381.5588459816887);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2026', 69.7764267718335),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2027', 87.08133589812452),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2028', 101.9513055588603),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2029', 114.0421869432223),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2030', 127.0563722706083);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2026', 65.86124850325395),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2027', 64.74286676014289),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2028', 64.2590275422104),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2029', 63.38710416166761),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Japan', 'ASIA', 'Japan', 'Y', '2030', 64.69435268797692);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2026', 45.4342210576637),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2027', 45.30360395712421),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2028', 46.39820281911545),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2029', 47.71813183024096),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:South Korea', 'ASIA', 'South Korea', 'Y', '2030', 49.43705174256036);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2026', 25.27491016566832),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2027', 28.71909741764149),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2028', 31.00347738605348),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2029', 33.71877928288838),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2030', 36.10326104366981);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2026', 24.47716093187148),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2027', 26.15960744094616),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2028', 26.19975491620433),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2029', 26.06831127589428),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Taiwan (China)', 'ASIA', 'Taiwan (China)', 'Y', '2030', 25.83036323489914);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2026', 13.60750852433178),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2027', 15.53469643760048),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2028', 16.69771883860155),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2029', 18.59189999419504),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2030', 19.53973684054826);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2026', 8.976692693836682),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2027', 11.47753453905812),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2028', 14.17259244362293),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2029', 16.81860452241403),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Bangladesh', 'ASIA', 'Bangladesh', 'Y', '2030', 19.79280497221436);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2026', 3.616200578592093),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2027', 3.616200578592093),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2028', 3.616200578592093),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2029', 3.616200578592093),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2030', 3.616200578592093);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2026', 11.18444870720271),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2027', 12.44964490895009),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2028', 14.64948048203312),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2029', 17.64656243900519),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2030', 18.90476444542123);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2026', 11.33873376366394),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2027', 11.55172888279791),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2028', 11.73313152053357),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2029', 11.95461504674262),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2030', 12.16462247298452);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2026', 1.760450233485789),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2027', 1.968131995693815),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2028', 2.201683823073902),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2029', 2.479330891621374),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Philippines', 'ASIA', 'Philippines', 'Y', '2030', 2.738827003195811);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2026', 1.601587980649675),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2027', 2.2475913398985),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2028', 2.229932394038085),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2029', 0.8177793165295412),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Vietnam', 'ASIA', 'Vietnam', 'Y', '2030', 1.076686092409459);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2026', 0.1555883867826258),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2027', 0.237228103350041),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2028', 0.4351471729678137),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2029', 0.5145439485823926),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Sri Lanka', 'ASIA', 'Sri Lanka', 'Y', '2030', 0.6038025966084151);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 117.4905421375942),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 120.6371085502004),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 123.8698573607859),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 126.9592604792342),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 126.6977817081881);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 20.9038599919765),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 21.15760960029746),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 20.49606847586607),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 20.39800677885447),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 21.15370331755767);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2026', 23.3112265361573),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2027', 24.06081599655086),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2028', 18.50124099059373),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2029', 22.12192522872682),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2030', 22.55897135891156);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2026', 10.39864107164169),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2027', 11.43937925314208),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2028', 15.50883565776704),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2029', 15.44333320503064),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2030', 13.01568109419496);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2026', 14.3263456231914),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2027', 16.13489671101176),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2028', 15.53352567820899),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2029', 13.29569926215371),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2030', 11.41341897317982);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2026', 3.62100407987995),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2027', 5.222042226990991),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2028', 7.338883129479314),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2029', 10.69596618911037),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2030', 13.79367151831558);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2026', 12.85304001987818),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2027', 13.13415188802781),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2028', 13.77697825262127),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2029', 11.28737287343015),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2030', 10.73402757469422);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 7.611823514833997),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 7.620888284513049),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 8.420373394586512),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 8.378434911795335),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 7.931284918511008);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2026', 7.111728161842958),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2027', 6.674605974985607),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2028', 9.361899067571581),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2029', 8.093923443777465),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2030', 8.09201053329279);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2026', 2.828597578017861),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2027', 2.064888835158554),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2028', 2.912416097351911),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2029', 3.416466536690288),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2030', 3.001176706270044);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2026', 4.508403530359347),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2027', 3.834997238783444),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2028', 3.28755601612073),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2029', 2.718119612309444),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2030', 3.212602002450558);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2026', 2.183270048229862),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2027', 1.894323059495068),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2028', 2.190100306134346),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2029', 2.183263054632773),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2030', 2.400719995976545);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2026', 1.796346021697411),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2027', 1.681465747949807),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2028', 1.673653203887478),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2029', 1.686611893109194),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2030', 1.690722742293229);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2026', 0.1855578014752367),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2027', 0.1833487623747694),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2028', 0.1828533044277401),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2029', 0.1841544818377737),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2030', 0.1797661138485755);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2026', 0.3688313622297678),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2027', 0.3679909203293819),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2028', 0.3692295185403722),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2029', 0.3728020590153315),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2030', 0.000000000000001306);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2026', 0.000004745791783497),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2029', 0.000000126757121034),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2030', 0.000004572952218403);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2026', 5.481862050390966),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2027', 5.165704050589795),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2028', 4.316244267628881),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2029', 6.683180822003274),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2030', 7.520020285739381);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 0.000000000000000245);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 0.000000000000003172),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 0.000000000000000245);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 17.37587872596537),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 17.76095341316996),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 22.39541857244861),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 22.3572681029553),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 24.38578819108366);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2026', 12.77024876177559),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2027', 14.36158328690111),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2028', 18.05029584293849),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2029', 17.35016917341593),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2030', 16.99461473204837);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2026', 3.044644010043049),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2027', 1.757897680633638),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2028', 1.905798820655228),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2029', 1.354599690270961),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Kuwait', 'MIDDLE_EAST', 'Kuwait', 'Y', '2030', 0.8682946621202167);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2026', 0.5334006967900253),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2027', 0.530010412801277),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2028', 0.5084611890915098),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2029', 0.5001170996565498),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2030', 0.4988212068113928);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2026', 1.027585257356696),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2027', 1.111462032833941),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2028', 1.930862719763386),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2029', 2.74189074370253),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Bahrain', 'MIDDLE_EAST', 'Bahrain', 'Y', '2030', 3.409869695422605);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 0.000000000000002611),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 0.4104913959093258),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 2.614187894681079);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:New Zealand', 'AUSTRALIA', 'New Zealand', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 7.680784065922539),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 6.888775806375358),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 6.707666961867492),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 5.29275685873916),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 7.661877947510059);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 5.700000912000142),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 4.605019403605898),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 4.757713233824153),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 3.574106733777077),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 5.469082192282731);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2026', 1.700000272000042),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2027', 1.700000272000043),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2028', 1.700000272000043),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2029', 1.32431424930028),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2030', 1.324574880341981);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2027', 0.2715158553987161),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2028', 0.2499534560432964),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2029', 0.3943358756618023),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2030', 0.8682208748853476);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2026', 0.2807828819223548),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2027', 0.3122402753707011),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 12.93623224344678),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 13.89810604421453),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 14.02876456571791),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 13.82661306226876),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 14.03586072765723);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2026', 4.129006575031777),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2027', 3.954605680615956),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2028', 3.685626103994387),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2029', 3.254580463249386),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2030', 3.143897263750285);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2026', 2.048254155961774),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2027', 1.944998628600592),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2028', 1.629459058693336),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2029', 1.373007440501733),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2030', 1.463409597415327);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2026', 0.1903207230919228),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2027', 1.202363437752287),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2028', 1.846313111417158),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2029', 1.959027483591892),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2030', 2.261896289081406);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2026', 0.4799479599048553),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2027', 0.5648091971210368),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2028', 0.5848873359893983),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2029', 0.5949829999133948),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Ecuador', 'SOUTH_AMERICA', 'Ecuador', 'Y', '2030', 0.6090582436143186);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2026', 6.088702829456454),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2027', 6.231329100124657),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2028', 6.282478955623634),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2029', 6.645014675012352),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2030', 6.557599333795893);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 438.5486154717652),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 470.2742120738807),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 502.5493629367269),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 525.8099487347932),
    (2, 'GLOBAL', 'LNG_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 554.3401545561278);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 43.02986040222791),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 38.14367312087479),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 37.41285320308979),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 30.6495587519606),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 23.69540966397037);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2026', 17.81265817337522),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2027', 16.60824590026948),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2028', 16.70932268406889),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2029', 15.63351725365299),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2030', 11.0547518926973);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 21.31815442150505),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 17.63637941325765),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 17.43337703629634),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 13.73393525076035),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 11.70698892099718);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2026', 3.899047807347646),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2027', 3.899047807347648),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2028', 3.270153482724556),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2029', 1.282106247547261),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Brunei', 'ASIA', 'Brunei', 'Y', '2030', 0.9336688502758941);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 4.40000070400011),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 4.340000694400108),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 3.830000612800094),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 4.340000694400109),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 2.383581643843344);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2026', 4.40000070400011),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2027', 4.340000694400108),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2028', 3.830000612800094),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2029', 4.340000694400109),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2030', 2.383581643843344);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 130.6017805176131),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 161.0539994779456),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 179.5337900595713),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 195.7122023779264),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 228.2747028205727);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 117.9684193370216),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 144.2044641154925),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 162.4369345001396),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 176.6465310407497),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 202.65737345184);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 11.45288919261496),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 13.20996321149409),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 13.19681006362958),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 16.9854726537656),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 25.26452931228465);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 1.18047198797651),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 3.639572150958991),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 3.900045495802146),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 2.080198683411178),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 0.352800056448009);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 98.88997489868896),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 107.9506940709788),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 126.4101141960533),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 142.1103732205648),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 151.1524157956195);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 5.806905691009695),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 5.434322696503504),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 5.441171990437067),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 6.671861301589463),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 9.563749849291735);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2026', 81.70225684148181),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2027', 90.29494084761788),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2028', 109.4575117923588),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2029', 124.517081600118),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2030', 130.0841274254931);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 11.38081236619747),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 12.22143052685745),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 11.51143041325744),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 10.92143031885741),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 11.50453852083473);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 89.46501591663474),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 91.7195949082951),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 89.29244365395769),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 87.90457638689894),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 84.9687316134397);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2026', 81.06131228419568),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2027', 83.61968194504593),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2028', 81.73733380797337),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2029', 80.34946654091462),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2030', 77.3122441395801);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2026', 8.403703632439054),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2027', 8.099912963249169),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2028', 7.555109845984317),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2029', 7.555109845984319),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Papua New Guinea', 'AUSTRALIA', 'Papua New Guinea', 'Y', '2030', 7.656487473859597);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 28.92785894167762),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 21.60123665546802),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 18.57498878383836),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 18.32969831840688),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 16.30376130571196);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 28.92785894167762),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 21.60123665546802),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 18.57498878383836),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 18.32969831840688),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 16.30376130571196);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 41.6760699542389),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 44.57562675155502),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 45.66329332511963),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 45.10756297154427),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 44.24352715260028);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2026', 11.05488421369147),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2027', 9.360001497600237),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2028', 9.01000144160023),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2029', 5.64000090240014),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2030', 3.517828898753255);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2026', 16.41047428294986),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2027', 19.60752926182609),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2028', 22.66103681292182),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2029', 21.97202266387886),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2030', 21.62248040036445);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Senegal', 'AFRICA', 'Senegal', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2026', 3.400000544000085),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2027', 3.400000544000084),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2028', 3.400000544000084),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2029', 8.37833535839079),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2030', 14.32813162333439);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2026', 3.797583940946762),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2027', 4.196330254746171),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2028', 3.854547700060962),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2029', 2.723537680918142),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Angola', 'AFRICA', 'Angola', 'Y', '2030', 2.046174598815964);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2026', 2.560000409600065),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2027', 2.560000409600064),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2028', 1.031210920497189),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2029', 0.6710545533384489),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Equatorial Guinea', 'AFRICA', 'Equatorial Guinea', 'Y', '2030', 0.538417172337769);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 4.453126563050649),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 5.451764783782362),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 5.706495906039342),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 5.722611812617886),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 2.190494458994455);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 9.179812874534118),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 8.019337316505172),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 8.915736038353502),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 8.741620441057051),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 8.687151943581055);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 0.719910117425195),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 0.5942926190460922),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 2.414819307352425);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2026', 5.379812266534024),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2027', 4.219336708505077),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2028', 4.455825322528214),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2029', 5.201596128232716),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2030', 3.772068988313062);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2026', 3.800000608000095),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2027', 3.800000608000095),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2028', 3.740000598400094),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2029', 2.945731693778244),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2030', 2.500263647915568);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'LNG_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 446.1703742096154),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 477.4041629960226),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 509.6332198727837),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 532.8955931627592),
    (2, 'GLOBAL', 'LNG_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 559.7092819393388);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 77.07622872538722),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 79.07057208057556),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 76.91125667006118),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 78.40510739136894),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 79.31153997491504);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2026', 63.36935881970741),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2027', 65.10762748062538),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2028', 62.61212687394416),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2029', 62.77302983844388),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:China', 'ASIA', 'China', 'Y', '2030', 62.38330198283025);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2026', 4.443084883087588),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2027', 4.425821988833697),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2028', 4.193366015558638),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2029', 3.934468374537574),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Thailand', 'ASIA', 'Thailand', 'Y', '2030', 3.60149361566564);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2026', 3.417751768410283),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2027', 3.176954477767716),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2028', 2.931466087829574),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2029', 2.853095600730296),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2030', 2.783410068325611);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2026', 0.03184166390629197),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2027', 0.01303413547732466),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2028', 0.04609676060161686),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2029', 0.1200039837006374),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Singapore', 'ASIA', 'Singapore', 'Y', '2030', 0.1226019886163182);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2026', 1.11369156381565),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2027', 1.051786361925818),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2028', 1.045103013206482),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2029', 1.044576994937319),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2030', 1.005618591533975);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Pakistan', 'ASIA', 'Pakistan', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:India', 'ASIA', 'India', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2026', 1.102500176400028),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2027', 1.825934969394595),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2028', 2.644464480384337),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2029', 4.260338410224169),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2030', 6.081089377389342);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2026', 3.597999850059976),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2027', 3.469412666551027),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2028', 3.43863343853635),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2029', 3.41959418879507),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2030', 3.334024350553896);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 266.2415204483847),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 266.373809559698),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 276.2711731211513),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 261.8616094702193),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 249.9118561604652);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2026', 48.7854949319957),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2027', 47.24121553797234),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2028', 45.40742783364834),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2029', 41.90121192142497),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2030', 37.36973960415833);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2026', 27.55615702750131),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2027', 28.02864052055539),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2028', 28.5928734768342),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2029', 27.95390286137927),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2030', 28.77786558905848);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2026', 26.06392347403207),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2027', 26.05245412774873),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2028', 28.32612913162503),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2029', 25.28274051278416),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2030', 20.69841695674671);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2026', 43.84870311403769),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2027', 44.50812079579933),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2028', 50.95676639417762),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2029', 45.66331943472611),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2030', 44.09161897352828);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 17.28920878468688),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 16.86386397364964),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 18.33736789228295),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 17.51057257547432),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 16.76608988757438);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2026', 18.9232764153825),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2027', 18.54348247266591),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2028', 19.81534252365562),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2029', 19.38423124639563),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2030', 18.6693337370934);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 19.06060617528627),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 18.43369362606685),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 18.53091681828784),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 17.33661843263985),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 16.5642537102806);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2026', 12.61410737616718),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2027', 12.90808132408801),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2028', 12.61318542876967),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2029', 12.69109408736505),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2030', 12.1656168900087);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2026', 8.892196976217614),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2027', 9.089004161988758),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2028', 9.248754026852698),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2029', 9.194604818585033),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2030', 7.781810805089728);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2026', 2.581883423101348),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2027', 3.030268039842886),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2028', 2.847580085612814),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2029', 2.831213837994214),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2030', 2.784736840557894);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2026', 4.497267048875397),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2027', 4.53083611907141),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2028', 4.613056552201653),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2029', 4.715995044290585),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2030', 4.704996677799469);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2026', 4.509656832855103),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2027', 5.931870151148944),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2028', 7.237793810556239),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2029', 8.113457812463258),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2030', 8.616412726280172);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2026', 5.335149430493367),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2027', 5.3352262661981),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2028', 5.750441605497609),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2029', 5.482608266853275),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2030', 5.933618086588889);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2026', 2.074420231907237),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2027', 1.992485358797657),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2028', 1.898228943716631),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2029', 1.771968418514947),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2030', 3.333138803302208);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2026', 2.530714919914387),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2027', 2.474294840887175),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2028', 2.477744931439189),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2029', 2.480166021826564),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2030', 2.806951356222215);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2026', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2027', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2028', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2029', 1.892443640190983),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2030', 1.837500294000047);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2026', 3.06959081073453),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2027', 3.19043206099913),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2028', 3.307530569389891),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2029', 3.509573602176776),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2030', 3.723286836000894);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2026', 1.84526718724275),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2027', 1.751605163669824),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2028', 1.635827066232331),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2029', 1.61739443928311),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2030', 1.582257097161136);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2026', 2.955998262560063),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2027', 2.798615333278453),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2028', 2.663069633176141),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2029', 2.553814597690333),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2030', 1.42704044264647);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2026', 0.4189265391681354),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2027', 0.4085918329166562),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2028', 0.3587782984646883),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2029', 0.4314695767978497),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2030', 0.4120163699226192);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2026', 1.829675035687769),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2027', 2.045214612207516),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2028', 2.101103550680625),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2029', 2.047279936099567),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2030', 2.131456461533034);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2026', 0.8723413656596185),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2027', 0.7460551522038242),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2028', 0.7324206545323046),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2029', 0.7313475911106145),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2030', 0.6518725179046029);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2026', 0.9023875743820119),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2027', 0.9045703115812469),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2028', 0.9082953575922544),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2029', 0.9103233706517393),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2030', 0.9174403767904603);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2026', 0.4457970488275278),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2027', 0.4456044787967166),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2028', 0.4459645553543289),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2029', 0.4465716654514665),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Sweden', 'EUROPE', 'Sweden', 'Y', '2030', 0.4468556694969071);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2026', 2.717007716031234),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2027', 2.374869017099043),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2028', 0.7680233324476159),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2029', 0.4732249078309853),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2030', 0.4217998646129783);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2026', 4.784262451636993),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2027', 4.907213986464238),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2028', 4.859050354123057),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2029', 4.934460850218736),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2030', 5.295729586106734);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 127.3496299372968),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 135.9202994217996),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 140.3438703920902),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 141.4102684609391),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 142.1131316457489);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 60.77501989424819),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 65.45121619414446),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 68.18710035448717),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 68.09965254952631),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 65.589574139169);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 48.79990426727068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 52.39696020186544),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 54.009844606305),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 54.95477735198365),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 58.03060716245285);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 17.77470577577792),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 18.07212302578969),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 18.14692543129807),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 18.35583855942917),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 18.49295034412706);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 58.0875876429094),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 54.8813888922373),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 49.11130354328859),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 50.98099102564854),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 49.78434786379562);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2026', 28.85036265126303),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2027', 28.49513014132582),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2028', 24.3396581448303),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2029', 24.32245962129854),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2030', 23.75175959763154);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 18.46700003822002),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 17.86457277824168),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 18.39047593315618),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 20.32294033623477),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 19.94572700386146);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2026', 4.392674839648336),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2027', 2.176551644783287),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 1.526549337617894),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 1.494133551726368),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 1.53016868914199),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 1.484590291955096),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 1.235860486142498);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2026', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2027', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2028', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2029', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2030', 2.646000423360068);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2026', 2.205000352800056),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2027', 2.205000352800056),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2028', 2.205000352800056),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2029', 2.205000352800056),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2030', 2.205000352800056);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Saudi Arabia', 'MIDDLE_EAST', 'Saudi Arabia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Australia', 'AUSTRALIA', 'Australia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 9.971416415051628),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 10.10946209751394),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 9.792764291342284),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 14.10020947503351),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 18.70386492461838);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 4.924500787920127),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 4.924500787920127),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 4.924500787920127),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 9.526687119269939),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 14.01956055812969);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2026', 2.594141516187644),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2027', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2028', 2.329302617188414),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2029', 2.034561469529832),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:South Africa', 'AFRICA', 'South Africa', 'Y', '2030', 2.145343480254953);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2026', 2.452774110943858),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2027', 2.538960886233742),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2028', 2.538960886233742),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2029', 2.538960886233742),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Tunisia', 'AFRICA', 'Tunisia', 'Y', '2030', 2.538960886233742);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Ghana', 'AFRICA', 'Ghana', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 5.695991456358633),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 5.001555260248842),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 2.972979264176682),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 3.268143257402921),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 4.369354243596679);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2026', 1.93825631512101),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2027', 1.669856322177012),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2028', 0.6905431209868994),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2029', 0.5657275325164052),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Brazil', 'SOUTH_AMERICA', 'Brazil', 'Y', '2030', 0.4251459710233554);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 1.805280828844933),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 1.473471640755462),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 0.4242088458734153),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 0.2795073147211704),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 0.1170927952348472);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2026', 1.764000282240045),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2027', 1.764000282240045),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2028', 1.764000282240045),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2029', 1.764000282240045),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2030', 1.764000282240045);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Peru', 'SOUTH_AMERICA', 'Peru', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2029', 0.5646811128489782),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Trinidad and Tobago', 'SOUTH_AMERICA', 'Trinidad and Tobago', 'Y', '2030', 1.968888180022109);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2026', 0.1884540301526448),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2027', 0.09422701507632242),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2028', 0.09422701507632242),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2029', 0.09422701507632242),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2030', 0.09422701507632242);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 544.4223746253884),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 551.3570873120733),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 555.4033472821104),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 550.0263290806124),
    (2, 'GLOBAL', 'PIPELINE_IMPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 544.1940948131399);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2026', 64.12203454203116),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2027', 64.0498817982144),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2028', 61.08384672569065),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2029', 60.6206505695941),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:ASIA', 'ASIA', NULL, 'Y', '2030', 61.06246239807519);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2026', 32.77448491191759),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2027', 33.71528506244561),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2028', 32.19378908484136),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2029', 32.10571986931345),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Turkmenistan', 'ASIA', 'Turkmenistan', 'Y', '2030', 35.06321676415229);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2026', 3.162113904438225),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2027', 3.236436969329915),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2028', 2.7886099971776),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2029', 2.438228091116494),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Kazakhstan', 'ASIA', 'Kazakhstan', 'Y', '2030', 0.2072266681562669);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2026', 2.116800338688054),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2027', 2.135844645965123),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2028', 1.697850271656044),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2029', 1.484700237552038),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Uzbekistan', 'ASIA', 'Uzbekistan', 'Y', '2030', 1.447950231672037);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2026', 6.872204794552767),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2027', 6.365927893548463),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2028', 5.92461595214855),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2029', 5.821124272147046),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Myanmar', 'ASIA', 'Myanmar', 'Y', '2030', 5.389007300021163);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2026', 19.13150475104076),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2027', 18.56753180821438),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2028', 18.37255832960933),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2029', 18.4353493896559),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Azerbaijan', 'ASIA', 'Azerbaijan', 'Y', '2030', 18.4353493896559);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2027', 0.01303413547732466),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Indonesia', 'ASIA', 'Indonesia', 'Y', '2030', 0.1226019886163182);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2026', 0.0649258413937669),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2027', 0.0158212832335837),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2028', 0.1064230902577635),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2029', 0.3355287098091702),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Malaysia', 'ASIA', 'Malaysia', 'Y', '2030', 0.3971100558012101);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:ASIA:Other Asia', 'ASIA', 'Other Asia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2026', 214.4816651316878),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2027', 211.9876036255455),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2028', 221.5931913131176),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2029', 209.2231858925916),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:EUROPE', 'EUROPE', NULL, 'Y', '2030', 199.9245886828976);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2026', 80.37321446583523),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2027', 78.66548851846375),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2028', 83.63055276119903),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2029', 82.53526179678877),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Norway', 'EUROPE', 'Norway', 'Y', '2030', 79.10564135320186);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2026', 18.6294050027048),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2027', 18.17784099595456),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2028', 20.02726306536209),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2029', 17.1518899793024),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Netherlands', 'EUROPE', 'Netherlands', 'Y', '2030', 16.6877069025331);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2026', 55.59980152346684),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2027', 55.13762625556362),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2028', 52.36813643280617),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2029', 48.10527980300819),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Intra-Europe', 'EUROPE', 'Intra-Europe', 'Y', '2030', 44.50212730247252);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2026', 18.10725298791722),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2027', 17.52490106898417),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2028', 16.84301199677904),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2029', 16.23775557401748),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:United Kingdom', 'EUROPE', 'United Kingdom', 'Y', '2030', 14.40319860467452);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Germany', 'EUROPE', 'Germany', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2026', 14.41532210645154),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2027', 13.65447203471553),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2028', 14.35697044711527),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2029', 13.613106228097),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belgium', 'EUROPE', 'Belgium', 'Y', '2030', 12.98432097749136);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2026', 11.6095106075217),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2027', 12.34800197568032),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2028', 12.34800197568032),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2029', 12.34800197568032),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:France', 'EUROPE', 'France', 'Y', '2030', 12.34800197568032);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Austria', 'EUROPE', 'Austria', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2026', 0.7961528106296387),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2027', 0.4800310758049721),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2028', 0.2952549862407978),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2029', 0.2505297880847661),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Denmark', 'EUROPE', 'Denmark', 'Y', '2030', 0.3586197138791543);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2026', 5.595013465202155),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2027', 6.191614530658325),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2028', 5.065969200555072),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2029', 3.357975927276148),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Spain', 'EUROPE', 'Spain', 'Y', '2030', 1.143797628007621);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2028', 0.000000350595056095),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2029', 0.000000350595056095),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Belarus', 'EUROPE', 'Belarus', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Bulgaria', 'EUROPE', 'Bulgaria', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2026', 1.764000282240045),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2027', 1.306167238986758),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2028', 1.541122521579603),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2029', 1.474738045958087),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Croatia', 'EUROPE', 'Croatia', 'Y', '2030', 1.549587517934003);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Czech Republic', 'EUROPE', 'Czech Republic', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2026', 0.8747243749559),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2027', 0.7788847696215631),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2028', 0.7758124691299951),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2029', 0.7728827586612415),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Finland', 'EUROPE', 'Finland', 'Y', '2030', 0.7604972716795636);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2026', 1.742317043770727),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2027', 1.195902521344403),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2028', 0.5596304860408777),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Greece', 'EUROPE', 'Greece', 'Y', '2030', 0.5578489927558389);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Hungary', 'EUROPE', 'Hungary', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ireland', 'EUROPE', 'Ireland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2027', 1.691984970717595),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2028', 7.486725897876143),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2029', 6.98934543829527),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Italy', 'EUROPE', 'Italy', 'Y', '2030', 7.400260484041677);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Latvia', 'EUROPE', 'Latvia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2026', 0.8016455182632829),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2027', 0.8652685984429758),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2028', 2.326036497165839),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2029', 0.3326479702236752),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Poland', 'EUROPE', 'Poland', 'Y', '2030', 0.198394024743044);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2026', 1.911000305760049),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2027', 1.911000305760049),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2028', 1.911000305760049),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2029', 1.911000305760049),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Portugal', 'EUROPE', 'Portugal', 'Y', '2030', 1.911000305760049);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2029', 0.6011335171813628),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Romania', 'EUROPE', 'Romania', 'Y', '2030', 1.049553593797817);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Slovakia', 'EUROPE', 'Slovakia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Switzerland', 'EUROPE', 'Switzerland', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Ukraine', 'EUROPE', 'Ukraine', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2026', 2.262304636968742),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2027', 2.058418764847002),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2028', 2.057701919232307),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2029', 3.54163643366183),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:EUROPE:Other Europe', 'EUROPE', 'Other Europe', 'Y', '2030', 4.964032034245125);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2026', 127.3496299372968),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2027', 135.9202994217996),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2028', 140.3438703920902),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2029', 141.4102684609391),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:NORTH_AMERICA', 'NORTH_AMERICA', NULL, 'Y', '2030', 142.1131316457489);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2026', 66.57461004304861),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2027', 70.46908322765512),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2028', 72.15677003760307),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2029', 73.31061591141282),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:United States', 'NORTH_AMERICA', 'United States', 'Y', '2030', 76.5235575065799);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2026', 60.73921700216972),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2027', 65.415413302066),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2028', 68.1512974624087),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2029', 68.06384965744785),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Canada', 'NORTH_AMERICA', 'Canada', 'Y', '2030', 65.55377124709054);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2026', 0.03580289207846273),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2027', 0.03580289207846273),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2028', 0.03580289207846273),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2029', 0.03580289207846273),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:NORTH_AMERICA:Mexico', 'NORTH_AMERICA', 'Mexico', 'Y', '2030', 0.03580289207846273);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2026', 39.54646805912526),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2027', 36.69550181839035),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2028', 35.08088846593719),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2029', 40.12827497750897),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:MIDDLE_EAST', 'MIDDLE_EAST', NULL, 'Y', '2030', 42.44233230972313);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2026', 19.99354937583792),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2027', 19.35870632996805),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2028', 19.92064462229817),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2029', 21.80753062818987),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Qatar', 'MIDDLE_EAST', 'Qatar', 'Y', '2030', 21.18158749000396);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2026', 12.42341754256717),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2027', 10.20729434770212),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2028', 8.030742702918833),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2029', 8.030742702918833),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iran', 'MIDDLE_EAST', 'Iran', 'Y', '2030', 8.030742702918833);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Iraq', 'MIDDLE_EAST', 'Iraq', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2026', 7.129501140720183),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2027', 7.129501140720183),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2028', 7.129501140720183),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2029', 10.29000164640026),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Israel', 'MIDDLE_EAST', 'Israel', 'Y', '2030', 13.23000211680034);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Jordan', 'MIDDLE_EAST', 'Jordan', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Oman', 'MIDDLE_EAST', 'Oman', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Tunisia', 'MIDDLE_EAST', 'Tunisia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Tunisia', 'MIDDLE_EAST', 'Tunisia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Tunisia', 'MIDDLE_EAST', 'Tunisia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Tunisia', 'MIDDLE_EAST', 'Tunisia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Tunisia', 'MIDDLE_EAST', 'Tunisia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Turkey', 'MIDDLE_EAST', 'Turkey', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:UAE', 'MIDDLE_EAST', 'UAE', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:MIDDLE_EAST:Other Middle East', 'MIDDLE_EAST', 'Other Middle East', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AUSTRALIA', 'AUSTRALIA', NULL, 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AUSTRALIA:Other Australia', 'AUSTRALIA', 'Other Australia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2026', 62.18033003195781),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2027', 63.91714031225744),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2028', 60.45516041185224),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2029', 62.34907707881909),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:RUSSIA', 'RUSSIA', NULL, 'Y', '2030', 61.45604382930944);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2026', 62.18033003195781),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2027', 63.91714031225744),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2028', 60.45516041185224),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2029', 62.34907707881909),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:RUSSIA:Russia', 'RUSSIA', 'Russia', 'Y', '2030', 61.45604382930944);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2026', 31.04625546693088),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2027', 33.78510507561681),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2028', 33.87341070924571),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2029', 33.02672884375659),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:AFRICA', 'AFRICA', NULL, 'Y', '2030', 32.82618170378905);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2026', 26.61461365674318),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2027', 29.3016043582567),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2028', 29.70660779805725),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2029', 29.19725712656114),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Algeria', 'AFRICA', 'Algeria', 'Y', '2030', 28.87964299574288);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2026', 2.594141516187644),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2027', 2.646000423360068),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2028', 2.329302617188414),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2029', 2.034561469529832),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Mozambique', 'AFRICA', 'Mozambique', 'Y', '2030', 2.145343480254953);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2026', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2027', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2028', 1.837500294000047),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2029', 1.794910247665622),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Libya', 'AFRICA', 'Libya', 'Y', '2030', 1.801195227791221);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Nigeria', 'AFRICA', 'Nigeria', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Egypt', 'AFRICA', 'Egypt', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:AFRICA:Other Africa', 'AFRICA', 'Other Africa', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2026', 5.695991456358633),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2027', 5.001555260248842),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2028', 2.972979264176682),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2029', 3.268143257402921),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'REGION_SUBTOTAL', 'REGION_SUBTOTAL:SOUTH_AMERICA', 'SOUTH_AMERICA', NULL, 'Y', '2030', 4.369354243596679);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2026', 3.706787138085942),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2027', 3.143327962932474),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2028', 1.114751966860315),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2029', 0.8452348472375755),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Bolivia', 'SOUTH_AMERICA', 'Bolivia', 'Y', '2030', 0.5422387662582027);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2026', 1.95245431239269),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2027', 1.858227297316368),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2028', 1.858227297316368),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2029', 1.858227297316368),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Argentina', 'SOUTH_AMERICA', 'Argentina', 'Y', '2030', 1.858227297316368);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2029', 0.5646811128489782),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Venezuela', 'SOUTH_AMERICA', 'Venezuela', 'Y', '2030', 1.968888180022109);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2026', 0.03675000588000094),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Chile', 'SOUTH_AMERICA', 'Chile', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Colombia', 'SOUTH_AMERICA', 'Colombia', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2026', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2027', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2028', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2029', 0.0),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'COUNTRY', 'COUNTRY:SOUTH_AMERICA:Other America S', 'SOUTH_AMERICA', 'Other America S', 'Y', '2030', 0.0);

INSERT INTO tmd_mdl_scnrcasevoll
    (run_seq, sgmt_cd, data_type_cd, row_kind_cd, row_key, rgn_cd, nat_nm, perd_unit_spt_cd, perd_key, vol_val)
VALUES
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2026', 544.4223746253884),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2027', 551.357087312073),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2028', 555.4033472821103),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2029', 550.0263290806124),
    (2, 'GLOBAL', 'PIPELINE_EXPORTS', 'TYPE_TOTAL', 'TYPE_TOTAL', NULL, NULL, 'Y', '2030', 544.19409481314);
