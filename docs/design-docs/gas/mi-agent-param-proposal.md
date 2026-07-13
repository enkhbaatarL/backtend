# MI Agent 수급모델 파라미터 제안 DB 설계

## Document Control

- status: Draft
- canonical for: Reference only — Agent 소유 파라미터 메타데이터/매핑/proposal 저장 구조의 검토 입력
- owner: Agent
- source documents:
  - Agent 파라미터 추출·제안 로직
  - Notion `api 정의 (frontend-backend-agent)` (`https://app.notion.com/p/38f263ff004c80719d14de5702d946b9`, contract snapshot 2026-07-03, re-reviewed 2026-07-06)
- depends on:
  - `docs/conventions/db-object-naming-rules.md`
  - `docs/conventions/db-term-dictionary-sheet2.csv`
- related Backend documents:
  - `docs/design-docs/gas/mi-model-parameter-proposal-design.md`
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
- supersedes: 없음
- last synced: 2026-07-07
- open questions:
  - ~~`tmm_mi_model_param_proposal_m`의 전체 DDL, key/index/nullability 및 rollout SQL~~ → **해소(2026-07-07 DB 반영본 수령, 3절 참조)**. 잔여: `scenario_id` active-scoped 유일성 도입 여부
  - Agent DB와 Backend DB 물리 토폴로지(동일 schema/instance vs 별도, view/synonym 경유 여부). read 권한 자체는 확정
  - dimension_map ERD 검토 blocker(#2 nullable UNIQUE, #3 조건부 무결성 CHECK) 반영한 정정 DDL — DB 반영본에도 아직 미적용
  - callback correlation/event ID, schema version, ack, retry를 Agent DB에 반영할지 여부(`completedAt`/`error_code`는 이미 컬럼 존재, `schemaVersion`은 없음)

이 문서는 Agent DB 설계 입력을 보존한다. Backend business lifecycle과 API 계약의 canonical source는 각각 위 design doc과 feature spec이며, 본문의 SQL은 DBA 적용 가능한 canonical DDL이 아니다.

## ERD Review Blockers

1. ~~`tmm_mi_model_param_proposal_m`은 설명만 있고 DDL이 없어 proposal PK/FK/UK/index/nullability를 검증할 수 없다.~~ **해소(2026-07-07)** — 3절에 DB 반영 DDL 수록. 잔여: `scenario_id`에 대한 active-scoped 유일성이 없어 1:N 이력이 허용됨(아래 참조).
2. `tmm_mi_model_param_dimension_map`의 UNIQUE key는 nullable `model_param_id`를 포함하므로 MySQL에서 `indirect`/`na` row 중복을 막지 못한다. **미해소** — 2026-07-07 DB 반영본에도 동일.
3. `mapping_method`와 `model_param_id`의 조건부 무결성(`direct`/`routing`은 non-null, `na`는 null)이 CHECK로 고정되지 않았다. **미해소** — DB 반영본에 조건부 CHECK 없음.
4. index/constraint 이름(`uk_*`, `idx_*`, `fk_*`)과 audit column 길이는 MI Platform 명명 규칙 및 `BaseEntity` mapping과 정합화가 필요하다.
5. `scenario_id -> scenario_group_id -> mi_event_id`로 도출 가능한 식별자를 proposal row에 함께 저장한다면 immutable request snapshot이라는 비정규화 목적과 불일치 검증 규칙이 필요하다. (proposal table은 세 식별자를 모두 저장 — Backend가 callback에서 dispatch snapshot과 cross-check)
6. **신규**: proposal table에 `scenario_id` UNIQUE(또는 active-scoped partial unique)가 없어 "시나리오당 제안 1회"가 DB로 강제되지 않는다. 동시 POST race와 이력 row correlation 처리 방침을 확정해야 한다.

implementation readiness: proposal read 계약은 확정 가능(3절); dimension_map 정정(#2/#3)과 물리 토폴로지·유일성(#6) 잔여

## 1. tmm_mi_model_param_m

수급모델 파라미터 마스터 테이블이다. Excel/Basecase에서 읽을 파라미터 위치, 읽기 방식, 컬럼 역할, 사용 대상 여부를 관리한다.

- 엑셀파일에서 파라미터별 data load 방식

```json
- `model_file`: 읽을 Basecase 파일을 고른다.
- `model_sheet`: 해당 파일에서 읽을 Excel 시트를 고른다.
- `basecase_read_mode`: 읽는 형태를 고른다. `fixed_columns`는 행 단위, `matrix_time_columns`는 월별 값이 열 방향으로 있는 형태다.
- `basecase_header_row`: 컬럼 헤더가 있는 행이다.
- `basecase_data_start_row`: 실제 데이터 읽기를 시작할 행이다.
- `basecase_column_roles`: 각 Excel 컬럼의 역할이다. slot 컬럼과 value 컬럼을 판단하는 기준으로 쓴다.
- `basecase_row_filter`: 조건에 맞는 행만 읽을 때 쓴다.
- `basecase_year_row`: matrix 방식에서 value 컬럼의 연도를 읽는 행이다.
- `basecase_month_row`: matrix 방식에서 value 컬럼의 월을 읽는 행이다.
- `basecase_period_row`: matrix 방식에서 value 컬럼의 기간을 읽는 행이다.
- `basecase_value_start_col`: matrix 방식에서 value 읽기를 시작할 컬럼이다.
- `basecase_value_end_col`: matrix 방식에서 value 읽기를 끝낼 컬럼이다.
- 요약: `model_file`/`model_sheet`로 위치를 잡고, `basecase_*` 컬럼으로 Excel의 slot과 value를 읽는다.
- 예시 1: `GDP`는 `Demand` 파일의 `GDPPopulationAdjustment` 시트에서 읽는다. `basecase_header_row = 6`은 헤더 행, `basecase_data_start_row = 7`은 데이터 시작 행이다. `basecase_column_roles = B:Year,C:Country,F:GDP`이므로 `B,C`는 slot, `F`는 value다. `basecase_year_row`, `basecase_month_row`, `basecase_period_row`, `basecase_value_start_col`, `basecase_value_end_col`은 `fixed_columns`라 사용하지 않는다.
- 예시 2: `LiqUtilAdjustment`는 `Supply` 파일의 `LiqUtilAdjustment` 시트에서 읽는다. `basecase_header_row = 10`은 왼쪽 slot 헤더 행, `basecase_data_start_row = 11`은 데이터 시작 행이다. `basecase_column_roles`의 `B`부터 `F`까지는 slot이고, `basecase_year_row = 7`, `basecase_month_row = 8`, `basecase_period_row = 9`에서 각 value 컬럼의 시간 정보를 읽는다. `basecase_value_start_col = G`, `basecase_value_end_col = DA`이므로 `G`부터 `DA`까지가 월별 value다.
```

DDL:

```sql
CREATE TABLE `tmm_mi_model_param_m` (
    `model_param_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '수급모델 파라미터 ID',
    `use_yn` CHAR(1) COLLATE utf8mb4_bin NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    `parameter_name` VARCHAR(150) COLLATE utf8mb4_bin NOT NULL COMMENT '수급모델 파라미터명',
    `parameter_hint` VARCHAR(500) COLLATE utf8mb4_bin NOT NULL COMMENT '수급모델 파라미터 설명',
    `model_file` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL COMMENT '모델 파일 구분',
    `model_sheet` VARCHAR(150) COLLATE utf8mb4_bin NOT NULL COMMENT '모델 시트명',
    `model_table` VARCHAR(50) COLLATE utf8mb4_bin NOT NULL COMMENT '모델 시트 내 테이블명',
    `semantic_axes` VARCHAR(500) COLLATE utf8mb4_bin NULL COMMENT '의미 축 설명',
    `basecase_read_mode` VARCHAR(50) COLLATE utf8mb4_bin NOT NULL COMMENT 'Basecase 읽기 방식',
    `basecase_header_row` SMALLINT UNSIGNED NOT NULL COMMENT 'Basecase header row',
    `basecase_data_start_row` SMALLINT UNSIGNED NOT NULL COMMENT 'Basecase data start row',
    `basecase_column_roles` VARCHAR(1000) COLLATE utf8mb4_bin NOT NULL COMMENT 'Basecase column role mapping',
    `basecase_row_filter` VARCHAR(200) COLLATE utf8mb4_bin NULL COMMENT 'Basecase row filter',
    `basecase_year_row` SMALLINT UNSIGNED NULL COMMENT 'Basecase year row',
    `basecase_month_row` SMALLINT UNSIGNED NULL COMMENT 'Basecase month row',
    `basecase_period_row` SMALLINT UNSIGNED NULL COMMENT 'Basecase period row',
    `basecase_value_start_col` VARCHAR(10) COLLATE utf8mb4_bin NULL COMMENT 'Basecase value start column',
    `basecase_value_end_col` VARCHAR(10) COLLATE utf8mb4_bin NULL COMMENT 'Basecase value end column',
    `basecase_stop_rule` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL COMMENT 'Basecase scan stop rule',

    `crt_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '생성자',
    `crt_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    `mod_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '수정자',
    `mod_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    `del_yn` CHAR(1) COLLATE utf8mb4_bin NOT NULL DEFAULT 'N' COMMENT '삭제여부',

    PRIMARY KEY (`model_param_id`),
    UNIQUE KEY `uk_tmm_mi_model_param_m_01` (`parameter_name`),
    KEY `idx_tmm_mi_model_param_m_01` (`model_file`, `model_sheet`),

    CONSTRAINT `ck_tmm_mi_model_param_m_01` CHECK (`use_yn` IN ('Y','N')),
    CONSTRAINT `ck_tmm_mi_model_param_m_02` CHECK (`del_yn` IN ('Y','N')),
    CONSTRAINT `ck_tmm_mi_model_param_m_03` CHECK (`basecase_read_mode` IN ('fixed_columns','matrix_time_columns'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='수급모델 파라미터 마스터';
```

Seed query:

```sql
INSERT INTO `tmm_mi_model_param_m` (
    `model_param_id`,
    `use_yn`,
    `parameter_name`,
    `parameter_hint`,
    `model_file`,
    `model_sheet`,
    `model_table`,
    `semantic_axes`,
    `basecase_read_mode`,
    `basecase_header_row`,
    `basecase_data_start_row`,
    `basecase_column_roles`,
    `basecase_row_filter`,
    `basecase_year_row`,
    `basecase_month_row`,
    `basecase_period_row`,
    `basecase_value_start_col`,
    `basecase_value_end_col`,
    `basecase_stop_rule`,
    `crt_usr_logn_id`,
    `mod_usr_logn_id`,
    `del_yn`
) VALUES
    (1, 'N', 'CDD', 'CDD per country per year-month', 'Demand', 'WeatherAdjustment', 'table1', '평년 대비 기온편차 변환 — 국가·연·월', 'fixed_columns', 6, 7, 'B:Year,C:Month,D:Country,E:HDD/CDD,G:HDD/CDD', 'E=CDD', NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (2, 'Y', 'Coal Futures Price', 'Note: ''Asia'' references Newcastle Coal Futures (6000kcal) while ''Europe'' references ARA Coal Futures (6000kcal)', 'Demand', 'CoalGasAdjustment', 'table1', '석탄 선물가격 strip — 지역·월 (Asia=Newcastle, EU=ARA)', 'fixed_columns', 6, 7, 'B:Month-Year,C:Region,E:Coal Futures Price (USD/t)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (3, 'Y', 'Gas Futures Price', 'Note: ''Asia'' references JKM futures while ''Europe'' references TTF futures', 'Demand', 'CoalGasAdjustment', 'table1', '가스 선물가격 strip — 지역·월 (Asia=JKM, EU=TTF)', 'fixed_columns', 6, 7, 'H:Month-Year,I:Region,K:Gas Futures Price (USD/mmbtu)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (4, 'Y', 'GDP', 'Real GDP (base year 2019) and population figures', 'Demand', 'GDPPopulationAdjustment', 'table1', 'Real GDP(base 2019) — 국가·연', 'fixed_columns', 6, 7, 'B:Year,C:Country,F:% change (+/-)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (5, 'N', 'HDD', 'HDD per country per year-month', 'Demand', 'WeatherAdjustment', 'table1', '평년 대비 기온편차 변환 — 국가·연·월', 'fixed_columns', 6, 7, 'B:Year,C:Month,D:Country,E:HDD/CDD,G:HDD/CDD', 'E=HDD', NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (6, 'Y', 'PMI', 'PMI input per country', 'Demand', 'PMIAdjustment', 'table1', '제조업 PMI — 국가·연·월 (산업 수요)', 'fixed_columns', 6, 7, 'B:Year,C:Month,D:Country,F:PMI', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (7, 'Y', 'PowerMix', 'Power mix input per country', 'Demand', 'PowerMixAdjustment', 'table1', '발전 에너지원 비중 — 국가·에너지원(Gas/Coal/Nuclear/Solar/Wind/…)·연', 'fixed_columns', 6, 12, 'B:Year,C:Country,D:Energy Source,F:% Power Mix', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (8, 'Y', 'Temperature', 'Temperature per country per year-month', 'Demand', 'WeatherAdjustment', 'table1', '기온 직접 — 국가·연·월', 'fixed_columns', 6, 7, 'K:Year,L:Month,M:Country,O:Temperature (°C)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (9, 'N', 'User-Adjusted Gas Demand', 'Demand Model Manual Overwrite Sector Input', 'Demand', 'DemandOutputOverwrite', 'table1', '수요 출력 직접 덮어쓰기 — 국가·연·월·Sub-Sector', 'fixed_columns', 7, 8, 'D:Country,E:Year,F:Month,G:Demand Sub-Sector,H:User-Adjusted Gas Demand [BCM]', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (10, 'N', 'ElectrificationRate', 'Electrification rate of the transportation sector per country and year', 'Demand', 'ElectrificationRateAdjustment', 'table1', '수송 전기화율 (본 21종 직접 해당 적음)', 'fixed_columns', 6, 7, 'B:Year,C:Country,D:Drive Train,F:% New Car Sales', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (11, 'N', 'Population', 'Real GDP (base year 2019) and population figures', 'Demand', 'GDPPopulationAdjustment', 'table1', '인구 — 국가·연', 'fixed_columns', 6, 7, 'J:Year,K:Country,M:Population (Thousands)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (12, 'Y', 'HurricaneIntensity', 'USA-specific liquefaction plant disruptions occuring due to hurricanes', 'Supply', '{USA}Hurricanes', 'table1', '美 액화시설 허리케인 차질 — 연·월', 'fixed_columns', 7, 8, 'B:Year,C:Month,D:Hurricane intensity', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (13, 'Y', 'LiqProdAdjustment', 'Liquefaction plant LNG production that override base case capacity x utilisation (in absolute terms)', 'Supply', 'LiqProdAdjustment', 'table1', '액화시설 생산량 override — Country>Plant>Train·연·월', 'matrix_time_columns', 10, 11, 'B:Country,C:LNG Plant,D:LNG Plant Train,E:Life Cycle Category,F:Nameplate Capacity (MTPA)', NULL, 7, 8, 9, 'G', 'DA', 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (14, 'Y', 'LiqUtilAdjustment', 'Liquefaction plant utilisation rates that override base case utilisation (in % terms).', 'Supply', 'LiqUtilAdjustment', 'table1', '액화시설 가동률 override — Country>Plant>Train·연·월', 'matrix_time_columns', 10, 11, 'B:Country,C:LNG Plant,D:LNG Plant Train,E:Life Cycle Category,F:Nameplate Capacity (MTPA)', NULL, 7, 8, 9, 'G', 'DA', 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (15, 'Y', 'PipelineAdjustment', 'Pipeline capacity adjustments that override base case capacity', 'Supply', 'PipelineAdjustment', 'table1', 'PNG 송출 capa override — Pipeline·From·To·연·월', 'fixed_columns', 7, 8, 'B:Year,C:Month,D:Pipeline name,H:User adjusted capacity (Bcm/y)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (16, 'Y', 'RegasAdjustment', 'Regasification plant capacity adjustment that override base case capacity (in absolute terms)', 'Supply', 'RegasAdjustment', 'table1', '재기화(수입) 설비 capa override — Continent>Region>Country>Plant·연·월', 'matrix_time_columns', 10, 11, 'B:Continent,C:Region,D:Country,E:Regasification plant,F:Life Cycle Category,G:Nameplate capacity (MTPA)', NULL, 9, 10, NULL, 'H', 'DK', 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (17, 'Y', 'SupplyDisruption', 'Supply disruptions on an annual basis for province/basin in % or absolute terms', 'Supply', 'VolumeAdjustment', 'table1', 'Upstream 생산 차질(+/-) — Province·연 (+ 월별 VolumeManualOverwrite)', 'fixed_columns', 7, 8, 'B:Year,C:Province, Country,E:Volumes (Bcm)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (18, 'N', 'PNG Contract Volume', 'Forecasted PNG Contract Volume (Imports). Malaysia/Thailand JDA, Neutral Zone are absorbed as domestic production in respective countries', 'Trade', 'MidstreamContractInput', 'table1', 'PNG 계약 물량(Nord Stream 재가동·Pipeline Disruption) — From·To·연', 'fixed_columns', 4, 5, 'A:Year,B:From Country,C:To Country,D:Unadjusted Gross Gas Import [BCM]', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (19, 'N', 'RemainingUnrestrictedAmount', 'LNG Trade Restrictions', 'Trade', 'TradeRestrictionInput', 'table1', '계약 취소·통과 제약 — From·To·연', 'fixed_columns', 5, 6, 'B:Year,C:From Country,D:To Country,J:Remaining Unrestricted Amount [BCM]', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (20, 'N', 'ShippingCost', 'LNG Shipping Costs', 'Trade', 'ShippingCostInput', 'table1', 'Charter·Fuel·Suez/Panama Crossing 비용·Disruption 여부 — impact_scope·연', 'fixed_columns', 5, 6, 'B:Year,F:User Input Absolute Value ($/d)', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N'),
    (21, 'N', 'VolumeCancelled', 'LNG Trade Restrictions', 'Trade', 'TradeRestrictionInput', 'table1', '계약 취소·통과 제약 — From·To·연', 'fixed_columns', 5, 6, 'B:Year,C:From Country,D:To Country,F:Volume [BCM]', NULL, NULL, NULL, NULL, NULL, NULL, 'stop_when_all_key_columns_empty', 'system', 'system', 'N')
ON DUPLICATE KEY UPDATE
    `use_yn` = VALUES(`use_yn`),
    `parameter_hint` = VALUES(`parameter_hint`),
    `model_file` = VALUES(`model_file`),
    `model_sheet` = VALUES(`model_sheet`),
    `model_table` = VALUES(`model_table`),
    `semantic_axes` = VALUES(`semantic_axes`),
    `basecase_read_mode` = VALUES(`basecase_read_mode`),
    `basecase_header_row` = VALUES(`basecase_header_row`),
    `basecase_data_start_row` = VALUES(`basecase_data_start_row`),
    `basecase_column_roles` = VALUES(`basecase_column_roles`),
    `basecase_row_filter` = VALUES(`basecase_row_filter`),
    `basecase_year_row` = VALUES(`basecase_year_row`),
    `basecase_month_row` = VALUES(`basecase_month_row`),
    `basecase_period_row` = VALUES(`basecase_period_row`),
    `basecase_value_start_col` = VALUES(`basecase_value_start_col`),
    `basecase_value_end_col` = VALUES(`basecase_value_end_col`),
    `basecase_stop_rule` = VALUES(`basecase_stop_rule`),
    `mod_usr_logn_id` = 'system',
    `del_yn` = 'N';
```

## 2. tmm_mi_model_param_dimension_map

이벤트 유형 L2와 scenario dimension이 어떤 수급모델 파라미터로 연결되는지 정의하는 매핑 테이블이다. 하나의 dimension이 여러 파라미터로 연결될 수 있으므로 매핑은 별도 row로 분리한다.

매핑 방식:

- `direct`: 해당 dimension이 특정 수급모델 파라미터 값을 직접 조정한다. `model_param_id`가 있어야 한다.
- `routing`: dimension 값에 따라 사용할 파라미터를 선택한다. `model_param_id`가 있어야 한다.
- `indirect`: 기간, 패턴, 강도, 배분 등 값을 결정하는 보조 축이다. `model_param_id`는 `NULL`일 수 있다.
- `na`: 수급모델 input으로 직접 반영하지 않는 항목이다. `model_param_id`는 `NULL`이다.

현행 기준:

- `mapping_status` 컬럼은 없다.
- `model_param_id`는 nullable이다.
- `direct`, `routing`인데 `model_param_id IS NULL`인 seed는 없다.
- 자연재해의 `supply_disruption_intensity`는 `indirect`/`NULL`이다. 파라미터 선택은 `affected_facility_type`의 `routing` 5건이 담당한다.

DDL:

```sql
CREATE TABLE `tmm_mi_model_param_dimension_map` (
    `model_param_dimension_map_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '수급모델 파라미터 dimension 매핑 ID',
    `use_yn` CHAR(1) COLLATE utf8mb4_bin NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    `event_type_l2_id` INT UNSIGNED NOT NULL COMMENT '이벤트 유형 L2 ID',
    `dimension_key` VARCHAR(150) COLLATE utf8mb4_bin NOT NULL COMMENT '시나리오 dimension key',
    `dimension_label` VARCHAR(200) COLLATE utf8mb4_bin NOT NULL COMMENT '시나리오 dimension label',
    `mapping_method` VARCHAR(50) COLLATE utf8mb4_bin NOT NULL COMMENT '매핑 방식',
    `model_param_id` INT UNSIGNED NULL COMMENT '수급모델 파라미터 ID',
    `comment` TEXT COLLATE utf8mb4_bin NULL COMMENT '매핑 설명',

    `crt_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '생성자',
    `crt_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    `mod_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '수정자',
    `mod_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    `del_yn` CHAR(1) COLLATE utf8mb4_bin NOT NULL DEFAULT 'N' COMMENT '삭제여부',

    PRIMARY KEY (`model_param_dimension_map_id`),
    UNIQUE KEY `uk_tmm_mi_model_param_dimension_map_01` (`event_type_l2_id`, `dimension_key`, `model_param_id`),
    KEY `idx_tmm_mi_model_param_dimension_map_01` (`event_type_l2_id`, `dimension_key`),
    KEY `idx_tmm_mi_model_param_dimension_map_02` (`model_param_id`),

    CONSTRAINT `ck_tmm_mi_model_param_dimension_map_01` CHECK (`use_yn` IN ('Y','N')),
    CONSTRAINT `ck_tmm_mi_model_param_dimension_map_02` CHECK (`del_yn` IN ('Y','N')),
    CONSTRAINT `ck_tmm_mi_model_param_dimension_map_04` CHECK (`mapping_method` IN ('direct','indirect','routing','na')),
    CONSTRAINT `fk_tmm_mi_model_param_dimension_map_01`
        FOREIGN KEY (`event_type_l2_id`)
        REFERENCES `tmm_mi_event_type_l2_m` (`event_type_l2_id`)
        ON DELETE RESTRICT,
    CONSTRAINT `fk_tmm_mi_model_param_dimension_map_02`
        FOREIGN KEY (`model_param_id`)
        REFERENCES `tmm_mi_model_param_m` (`model_param_id`)
        ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='수급모델 파라미터 dimension 매핑';
```

Seed query:

```sql
INSERT INTO `tmm_mi_model_param_dimension_map` (
    `model_param_dimension_map_id`,
    `use_yn`,
    `event_type_l2_id`,
    `dimension_key`,
    `dimension_label`,
    `mapping_method`,
    `model_param_id`,
    `comment`,
    `crt_usr_logn_id`,
    `mod_usr_logn_id`,
    `del_yn`
) VALUES
    (1, 'Y', 1, 'period', '기간', 'indirect', NULL, 'Parameter 변경 시간축 결정', 'system', 'system', 'N'),
    (2, 'Y', 1, 'utilization_change_amount', 'Utilization 변화 폭', 'direct', 17, 'Province/Basin 생산 차질량(Bcm 또는 %); 증산은 음(-)', 'system', 'system', 'N'),
    (3, 'Y', 1, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격/점진 — 값의 시계열 형태', 'system', 'system', 'N'),
    (4, 'Y', 1, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '원상복귀 시간축', 'system', 'system', 'N'),
    (5, 'Y', 1, 'alt_gas_supply_availability', '대체 가스 공급 가능성', 'direct', 17, '타 가스전 증산(음의 disruption)으로 부족분 상쇄 : 차질 지역과 연결 가능한 동일 권역·시장 내 가스전의 증산 가능량을 음(-)의 SupplyDisruption으로 배분', 'system', 'system', 'N'),
    (6, 'Y', 1, 'alt_gas_supply_availability', '대체 가스 공급 가능성', 'direct', 14, 'LNG 전환 대체 시 타 공급국 액화 가동률 상향 : LNG 전환 대체 공급 경로가 설명 가능한 인접·시장연결 공급국의 가용 액화설비에 차등 배분', 'system', 'system', 'N'),
    (7, 'Y', 2, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (8, 'Y', 2, 'utilization_change_amount', 'Utilization 변화량', 'direct', 14, 'Plant>Train 가동률(%) 조정 : 차질 대상 액화 Plant/Train의 이용률을 조정하고, 단기·부분 차질은 생산량보다 utilization 조정을 우선 사용', 'system', 'system', 'N'),
    (9, 'Y', 2, 'utilization_change_amount', 'Utilization 변화량', 'direct', 13, '대폭·영구 손실 시 생산량(Mt LNG) 직접 : 장기·영구 손실 또는 설비 폐쇄 수준이면 Plant/Train 생산량 자체를 하향 조정', 'system', 'system', 'N'),
    (10, 'Y', 2, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격(사고)/점진(정비)', 'system', 'system', 'N'),
    (11, 'Y', 2, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '원상복귀 속도', 'system', 'system', 'N'),
    (12, 'Y', 2, 'alt_lng_supply_availability', '대체 LNG 공급 가능성', 'direct', 14, '타 LNG 공급국 가동률 상향으로 상쇄 : 부족 LNG 물량을 대체 가능한 공급국의 잔여 액화 가동 여력에 배분하고, 이미 고가동 설비는 배분 제외', 'system', 'system', 'N'),
    (13, 'Y', 3, 'cod_timing', 'COD 시점', 'indirect', NULL, '신규 생산량 투입 시작 시간축(기존 대비 ±N월)', 'system', 'system', 'N'),
    (14, 'Y', 3, 'ramp_up_completion_timing', 'Ramp-up 완료 시점', 'indirect', NULL, '단계 투입 완료 시간축', 'system', 'system', 'N'),
    (15, 'Y', 3, 'ramp_up_pattern', 'Ramp-up 패턴', 'indirect', NULL, '급격/점진; FID 무산 시 투입 없음', 'system', 'system', 'N'),
    (16, 'Y', 3, 'target_utilization_at_ramp_up', 'Ramp-up 완료 시 도달 가동률 (Nameplate Capa 대비)', 'direct', 13, '신규 Plant>Train 생산량 추가(Mt); FID무산=0 : COD 지연·Ramp-up 단계에 맞춰 신규 Plant/Train 생산량을 월별로 반영하고, FID 무산은 추가 생산량 0으로 처리', 'system', 'system', 'N'),
    (17, 'Y', 4, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (18, 'Y', 4, 'utilization_change_amount', 'Utilization 변화 폭 (송출용량 대비)', 'direct', 15, '노선 capacity(Bcm/y) 조정 : 영향받은 파이프라인 노선의 송출 capacity를 조정하고, 기간·회복패턴에 따라 적용 월을 분배', 'system', 'system', 'N'),
    (19, 'Y', 4, 'utilization_change_amount', 'Utilization 변화 폭 (송출용량 대비)', 'na', NULL, '계약기반 PNG 물량 변동(Nord Stream 재가동/Pipeline Disruption) : PNG 계약 물량 자체는 Trade 계열 미대상 파라미터이므로 직접 조정하지 않고, 실제 송출 가능량 변화가 있으면 PipelineAdjustment로 별도 반영', 'system', 'system', 'N'),
    (20, 'Y', 4, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발=한시/구조적=영구 — 변경 범위', 'system', 'system', 'N'),
    (21, 'Y', 4, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격/점진', 'system', 'system', 'N'),
    (22, 'Y', 4, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '원상복귀 속도', 'system', 'system', 'N'),
    (23, 'Y', 4, 'alt_supply_availability', '대체 공급원 가능성 (타 노선·LNG 전환)', 'direct', 15, '타 노선 송출 증대 : 차질 노선과 연결 가능한 대체 파이프라인의 잔여 송출능력에 부족분을 차등 배분', 'system', 'system', 'N'),
    (24, 'Y', 4, 'alt_supply_availability', '대체 공급원 가능성 (타 노선·LNG 전환)', 'direct', 14, 'LNG 전환(타 공급국 액화 가동률 상향) : PNG 대체가 LNG 전환으로 설명될 때 시장연결 공급국의 가용 액화설비에 대체 물량을 배분', 'system', 'system', 'N'),
    (25, 'Y', 5, 'affected_period', '영향 기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (26, 'Y', 5, 'capacity_change_scale', 'Capacity 변화 규모', 'direct', 7, '발전부문 가스 비중 조정(capa GW→비중 환산) : 발전소 가동률·용량 변화가 LNG 수요에 미치는 영향을 PowerMix의 Gas 비중 변화로 환산', 'system', 'system', 'N'),
    (27, 'Y', 5, 'capacity_change_scale', 'Capacity 변화 규모', 'na', NULL, '발전부문 가스 수요 직접 강제 시 : User-Adjusted Gas Demand는 미대상 수동 overwrite이므로 직접 쓰지 말고, 발전 설비 변화는 PowerMix의 Gas 비중 조정으로 반영', 'system', 'system', 'N'),
    (28, 'Y', 5, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발(정비·사고)/구조적(COD·폐쇄)', 'system', 'system', 'N'),
    (29, 'Y', 5, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '흡수분만큼 타 source 비중으로 이전 : 감소한 발전원을 대체 가능한 전원 믹스의 정책·가용성 제약에 따라 Gas/Coal/Renewable/Nuclear 비중에 재배분', 'system', 'system', 'N'),
    (30, 'Y', 5, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격/점진', 'system', 'system', 'N'),
    (31, 'Y', 5, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '원상복귀 속도', 'system', 'system', 'N'),
    (32, 'Y', 6, 'period', '기간', 'indirect', NULL, '동절기 월 슬롯', 'system', 'system', 'N'),
    (33, 'Y', 6, 'cold_wave_intensity', '한파 강도', 'na', NULL, '평년 대비 -℃ → HDD(난방도일) 변환 입력 : HDD는 미대상 파라미터이므로 직접 조정하지 않고, 한파 강도는 Temperature 하향 또는 강도·월 분배 판단 근거로만 사용', 'system', 'system', 'N'),
    (34, 'Y', 6, 'cold_wave_intensity', '한파 강도', 'direct', 8, '기온 직접 입력(대안 경로) : 한파 영향 국가·월의 Temperature를 하향 조정하고, 강도에 따라 평년 대비 온도 편차를 월별로 차등 적용', 'system', 'system', 'N'),
    (35, 'Y', 6, 'entry_gas_inventory', '진입 시점 가스 재고 (평년 대비)', 'indirect', NULL, '재고는 수급모델 input 아님(Annual Balancing) — 영향 증폭/완화 해석 맥락', 'system', 'system', 'N'),
    (36, 'Y', 6, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '석탄·석유 등 흡수 비율', 'system', 'system', 'N'),
    (37, 'Y', 6, 'peak_timing_pattern', 'Peak 시점, 패턴', 'indirect', NULL, '월별 수요 분배', 'system', 'system', 'N'),
    (38, 'Y', 7, 'period', '기간', 'indirect', NULL, '하절기 월 슬롯(동남아·인도 4~5월 포함)', 'system', 'system', 'N'),
    (39, 'Y', 7, 'heatwave_intensity', '폭염 강도', 'na', NULL, '평년 대비 +℃ → CDD(냉방도일) 변환 입력 : CDD는 미대상 파라미터이므로 직접 조정하지 않고, 폭염 강도는 Temperature 상향 또는 강도·월 분배 판단 근거로만 사용', 'system', 'system', 'N'),
    (40, 'Y', 7, 'heatwave_intensity', '폭염 강도', 'direct', 8, '기온 직접 입력(대안 경로) : 폭염 영향 국가·월의 Temperature를 상향 조정하고, 냉방 수요 피크 월에 더 큰 조정폭을 적용', 'system', 'system', 'N'),
    (41, 'Y', 7, 'entry_gas_inventory', '진입 시점 가스 재고 (평년 대비)', 'indirect', NULL, '재고는 수급모델 input 아님 — 해석 맥락', 'system', 'system', 'N'),
    (42, 'Y', 7, 'injection_pattern', 'Injection 패턴', 'indirect', NULL, '재고 회복 속도 = 재고 개념 — 수급모델 input 아님(맥락)', 'system', 'system', 'N'),
    (43, 'Y', 7, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '재생E·원전 등 흡수', 'system', 'system', 'N'),
    (44, 'Y', 7, 'peak_timing_pattern', 'Peak 시점, 패턴', 'indirect', NULL, '월별 분배', 'system', 'system', 'N'),
    (45, 'Y', 8, 'affected_period', '영향 기간', 'indirect', NULL, '정비=Shoulder/사고·정책=단발 OR 영구', 'system', 'system', 'N'),
    (46, 'Y', 8, 'capacity_change_scale', 'Capacity 변화 규모', 'direct', 7, '원전 비중 변동→가스 비중 대체(원전 전용 input 없음)', 'system', 'system', 'N'),
    (47, 'Y', 8, 'capacity_change_scale', 'Capacity 변화 규모', 'na', NULL, '발전 가스 수요 직접 강제 시 : User-Adjusted Gas Demand는 미대상 수동 overwrite이므로 직접 쓰지 말고, 발전 설비 변화는 PowerMix의 Gas 비중 조정으로 반영', 'system', 'system', 'N'),
    (48, 'Y', 8, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발(정비)/구조적(COD·폐쇄·수명연장)', 'system', 'system', 'N'),
    (49, 'Y', 8, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '재생E·석탄 등 흡수', 'system', 'system', 'N'),
    (50, 'Y', 8, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격(사고)/점진(정비)', 'system', 'system', 'N'),
    (51, 'Y', 8, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '급격(재가동)/점진(COD)', 'system', 'system', 'N'),
    (52, 'Y', 9, 'affected_period', '영향 기간', 'indirect', NULL, '동절기 둔켈플라우테/하절기 열파 풍력 약화', 'system', 'system', 'N'),
    (53, 'Y', 9, 'capacity_change_scale', 'Capacity 변화 규모', 'direct', 7, '재생 출력 변동→가스 비중 대체(재생 전용 input 없음)', 'system', 'system', 'N'),
    (54, 'Y', 9, 'capacity_change_scale', 'Capacity 변화 규모', 'na', NULL, '발전 가스 수요 직접 강제 시 : User-Adjusted Gas Demand는 미대상 수동 overwrite이므로 직접 쓰지 말고, 발전 설비 변화는 PowerMix의 Gas 비중 조정으로 반영', 'system', 'system', 'N'),
    (55, 'Y', 9, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발(기상)/구조적(신규 Farm·정책)', 'system', 'system', 'N'),
    (56, 'Y', 9, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '석탄·원전 등 흡수', 'system', 'system', 'N'),
    (57, 'Y', 9, 'onset_pattern', '발생 패턴', 'indirect', NULL, '급격(무풍)/점진(정책 phase-out)', 'system', 'system', 'N'),
    (58, 'Y', 9, 'recovery_pattern', '회복 패턴', 'indirect', NULL, '급격(기상)/점진(COD)', 'system', 'system', 'N'),
    (59, 'Y', 10, 'affected_period', '영향 기간', 'indirect', NULL, '단발(가격)/중기(계약)/장기(정책)', 'system', 'system', 'N'),
    (60, 'Y', 10, 'lng_demand_impact_intensity', 'LNG 수요 영향 강도', 'direct', 2, '가격기반 석탄↔가스 스위칭(Asia=Newcastle/JKM, EU=ARA/TTF) : 지역별 기준 가격쌍을 사용해 석탄-가스 전환 유인을 판단하고, 단기 가격 이벤트는 CoalGasAdjustment를 우선 사용', 'system', 'system', 'N'),
    (61, 'Y', 10, 'lng_demand_impact_intensity', 'LNG 수요 영향 강도', 'direct', 3, '가격기반 석탄↔가스 스위칭(Asia=Newcastle/JKM, EU=ARA/TTF) : 지역별 기준 가격쌍을 사용해 석탄-가스 전환 유인을 판단하고, 단기 가격 이벤트는 CoalGasAdjustment를 우선 사용', 'system', 'system', 'N'),
    (62, 'Y', 10, 'lng_demand_impact_intensity', 'LNG 수요 영향 강도', 'direct', 7, '구조적 비중 변화(정책·폐쇄) : 정책·폐쇄·설비 변화처럼 구조적 전환이면 PowerMix에서 Gas/Coal 비중을 연 단위로 조정', 'system', 'system', 'N'),
    (63, 'Y', 10, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발(가격→CoalGas)/구조적(정책→PowerMix) 결정', 'system', 'system', 'N'),
    (64, 'Y', 10, 'alt_energy_absorption', '타 에너지원 흡수 가능성', 'direct', 7, '원전·재생E 등 흡수', 'system', 'system', 'N'),
    (65, 'Y', 11, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (66, 'Y', 11, 'affected_facility_type', '영향 시설 유형', 'routing', 17, '생산 시설 피해 시 : 생산시설 피해면 해당 국가·basin의 SupplyDisruption을 선택하고, 피해 강도는 별도 강도 dimension으로 산정', 'system', 'system', 'N'),
    (67, 'Y', 11, 'affected_facility_type', '영향 시설 유형', 'routing', 14, '수출(액화) 시설 피해 시 : 기존 액화 Plant/Train의 단기·부분 가동 차질이면 LiqUtilAdjustment 선택. 설비 용량은 유지되지만 정전·침수·정비·운영 제약으로 이용률이 낮아지는 경우에 사용', 'system', 'system', 'N'),
    (68, 'Y', 11, 'affected_facility_type', '영향 시설 유형', 'routing', 13, '수출(액화) 시설 피해 시 : 액화설비의 장기·영구 손상, 설비 폐쇄, 복구 지연 등으로 생산 가능량 자체를 낮춰야 하면 LiqProdAdjustment 선택. 단기 이용률 조정보다 구조적 생산량 감소가 명확한 경우에 사용', 'system', 'system', 'N'),
    (69, 'Y', 11, 'affected_facility_type', '영향 시설 유형', 'routing', 16, '수입(재기화) 시설 피해 시 : 수입 터미널 피해면 수입국 RegasAdjustment를 선택하고, 수출 공급 차질과 혼동하지 않음', 'system', 'system', 'N'),
    (70, 'Y', 11, 'affected_facility_type', '영향 시설 유형', 'routing', 12, '美 액화시설 허리케인 — [1-4] 강도 : 미국 Gulf Coast 액화시설이 허리케인 영향권이고 강도 단계로 차질을 설명할 수 있으면 HurricaneIntensity 선택. 실제 설비별 outage 비율이 명확하면 LiqUtilAdjustment/LiqProdAdjustment로 보완 판단', 'system', 'system', 'N'),
    (71, 'Y', 11, 'supply_disruption_intensity', '공급 차질 강도', 'indirect', NULL, '시설 가동률 감소율로 변환 — 강도가 값 크기 결정 : routing으로 선택된 시설·파라미터의 감소율 또는 감소량을 결정하며, 강도 단계에 따라 조정폭을 차등 적용', 'system', 'system', 'N'),
    (72, 'Y', 11, 'supply_disruption_pattern', '공급 차질 - 발생/회복 패턴', 'indirect', NULL, 'Ref. 공급 차질 패턴 — 시계열 형태', 'system', 'system', 'N'),
    (73, 'Y', 11, 'alt_lng_supply_availability', '대체 LNG 공급 가능성', 'direct', 14, '타 공급국 상향으로 상쇄 : 피해로 감소한 LNG 공급량 중 대체 가능한 물량만 시장연결 공급국의 가용 액화설비에 배분', 'system', 'system', 'N'),
    (74, 'Y', 12, 'inventory_level', '재고 수준 (평년 대비)', 'na', NULL, 'Annual Balancing — 재고/timing은 수급모델 input 아님; 시장·가격 해석 단계 : 재고 수준은 수급모델 input이 아니므로 직접 조정하지 않고, 공급 부족의 완충·증폭 맥락으로 기간·강도 판단에만 사용', 'system', 'system', 'N'),
    (75, 'Y', 12, 'duration_recovery_timing', '지속 기간 / 회복 시점', 'na', NULL, '재고 자체가 대상 아님 → 해당 없음 : 재고 회복 시점은 직접 입력하지 않고, 필요 시 이벤트 기간·회복 패턴 dimension으로만 반영', 'system', 'system', 'N'),
    (76, 'Y', 12, 'stockpile_policy_change', '비축 정책 변화', 'na', NULL, '비축 정책 변화가 그 해 LNG 구매량(연간 수요)을 바꾸는 부분만 — 재고 수준 자체는 아님 : 비축 정책이 실제 연간 LNG 구매량을 바꾸는 경우에만 수요·수입 방향성 판단에 사용하고, 재고 수준 자체 조정은 제외', 'system', 'system', 'N'),
    (77, 'Y', 12, 'spot_purchase_impact', 'Spot 구매 영향', 'na', NULL, 'Spot 가격 반응 = 가격 Output 단계 : Spot 구매·가격 반응은 가격 output·시장 해석 영역이므로 수급모델 input으로 직접 반영하지 않음', 'system', 'system', 'N'),
    (78, 'Y', 13, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (79, 'Y', 13, 'freight_change_magnitude', '운임 변동 폭', 'na', NULL, '용선료·연료비 — impact_scope축 : 운임·연료비는 Trade/ShippingCost 미대상으로 직접 조정하지 않고, 물리적 운송 차질이 공급량·기간에 영향을 줄 때만 별도 dimension으로 해석', 'system', 'system', 'N'),
    (80, 'Y', 13, 'freight_change_pattern', '변화 진행 패턴', 'indirect', NULL, '급격/점진', 'system', 'system', 'N'),
    (81, 'Y', 13, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발/구조적', 'system', 'system', 'N'),
    (82, 'Y', 14, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (83, 'Y', 14, 'cargo_flow_disruption_intensity', '화물 흐름 차질 강도', 'na', NULL, '항로 통과비용·차단 여부 : 화물 흐름·항로 비용 이슈는 ShippingCost 미대상이라 직접 반영하지 않고, 실제 공급 지연·물량 차질이 명시될 때 기간·강도 판단에만 사용', 'system', 'system', 'N'),
    (84, 'Y', 14, 'rerouting_availability', '우회·재라우팅 가능성', 'na', NULL, '우회 비용 상승; 높을수록 완화 : 우회 가능성은 비용 파라미터가 아니라 공급 차질 완화계수로 해석하고, 직접 수급모델 입력은 하지 않음', 'system', 'system', 'N'),
    (85, 'Y', 14, 'rerouting_availability', '우회·재라우팅 가능성', 'na', NULL, '봉쇄 시 통과 제약 : 봉쇄·통과 제한은 TradeRestrictionInput 미대상으로 직접 조정하지 않고, 실제 물량 차단이 확인될 때 공급 차질 이벤트로 별도 반영', 'system', 'system', 'N'),
    (86, 'Y', 14, 'transit_delay', '운송 지연 규모', 'indirect', NULL, '납기 지연 — ShippingCost 경유 간접', 'system', 'system', 'N'),
    (87, 'Y', 15, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (88, 'Y', 15, 'transaction_scale', '규모', 'na', NULL, '대규모 계약 분쟁·취소 물량 From-To (단순 헷지 포지션 재편은 실물 수급 아님→case) : 거래 분쟁·취소 물량은 TradeRestriction 미대상으로 직접 입력하지 않고, 실물 공급 또는 수요 변화가 명확한 경우 해당 공급·수요 파라미터로만 우회 반영', 'system', 'system', 'N'),
    (89, 'Y', 15, 'market_impact_persistence', '시장 영향 지속성', 'indirect', NULL, '일시/단기/구조적', 'system', 'system', 'N'),
    (90, 'Y', 16, 'period', '기간', 'na', NULL, '수급모델 Input 해당 없음 : 인덱스 Spread 이벤트는 가격·시장 해석 영역이므로 기간도 수급모델 파라미터 적용 기간으로 사용하지 않음', 'system', 'system', 'N'),
    (91, 'Y', 16, 'spread_change_magnitude', 'Spread 변동 폭', 'na', NULL, '가격 인덱스 간 관계 = 가격 Output 단계 — 수급모델 input으로 건드리지 않음 : 가격 spread 변화는 가격 output·시장 신호로만 사용하고 수급모델 input 값을 직접 변경하지 않음', 'system', 'system', 'N'),
    (92, 'Y', 16, 'structural_change_yn', '구조적 변동 여부', 'na', NULL, '상동 — 본 매핑 대상 외 : 구조적 여부도 spread 해석 구분용이며 수급모델 input 선정 조건으로 사용하지 않음', 'system', 'system', 'N'),
    (93, 'Y', 17, 'period', '기간', 'indirect', NULL, '장기화 시 종료 N/A — 시간축', 'system', 'system', 'N'),
    (94, 'Y', 17, 'transport_disruption', '운송 차질', 'na', NULL, '용선료·보험료 상승 : 운송비·보험료 상승은 ShippingCost 미대상으로 직접 반영하지 않고, 물리적 선적 차질이 있으면 공급 차질 기간·강도 판단에만 사용', 'system', 'system', 'N'),
    (95, 'Y', 17, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 14, '액화시설 가동률 감소 : 영향권 액화시설의 이용률을 하향 조정하고, 단기 제약은 LiqUtilAdjustment를 우선 사용', 'system', 'system', 'N'),
    (96, 'Y', 17, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 13, '생산량 직접 감소(대폭/영구) : 전쟁·제재로 장기 또는 영구 생산 손실이 명확할 때 생산량 자체를 하향 조정', 'system', 'system', 'N'),
    (97, 'Y', 17, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 17, 'upstream(가스전) 생산 차질 동반 시', 'system', 'system', 'N'),
    (98, 'Y', 17, 'supply_disruption_pattern', '공급 차질 - 발생/회복 패턴', 'indirect', NULL, 'Ref. 공급 차질 패턴 — 시계열', 'system', 'system', 'N'),
    (99, 'Y', 17, 'demand_response_alternative_lng', '수요측 대응 - 타 공급원 경유 LNG 확보량', 'direct', 14, '타 공급국 상향으로 상쇄', 'system', 'system', 'N'),
    (100, 'Y', 17, 'demand_response_powermix_reduction', '수요측 대응 - PowerMix 조정 등을 통한 LNG 수요 감소량', 'direct', 7, '자체 PowerMix 조정으로 LNG 수요 감소(SPR/대체 효과 포함) : 수요국의 정책·비상 대응으로 LNG 발전 수요가 줄면 PowerMix의 Gas 비중을 낮추고 대체 전원 비중을 조정', 'system', 'system', 'N'),
    (101, 'Y', 17, 'demand_response_powermix_reduction', '수요측 대응 - PowerMix 조정 등을 통한 LNG 수요 감소량', 'na', NULL, '해당 섹터 수요 직접 감소 강제 시 : User-Adjusted Gas Demand 미대상 수동 overwrite는 직접 쓰지 않고, 수요 감소는 PowerMix Gas 비중 또는 대상 섹터 proxy로 환산', 'system', 'system', 'N'),
    (102, 'Y', 17, 'trade_restriction', '거래 제한 (계약 취소 등)', 'na', NULL, 'From-To 계약 물량 취소 : 계약 취소는 TradeRestrictionInput 미대상으로 직접 반영하지 않고, 실제 공급 가능량 감소가 확인될 때 Supply 계열 파라미터로 우회 반영', 'system', 'system', 'N'),
    (103, 'Y', 18, 'period', '기간', 'indirect', NULL, '영구 제재 시 종료 N/A', 'system', 'system', 'N'),
    (104, 'Y', 18, 'transport_disruption', '운송 차질', 'na', NULL, '그림자 선단 등 운송료 상승 : 운송료 상승은 ShippingCost 미대상으로 직접 반영하지 않고, 물리적 운송 제약이 공급량에 연결될 때만 기간·강도 판단에 사용', 'system', 'system', 'N'),
    (105, 'Y', 18, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 14, '액화시설 가동률 감소', 'system', 'system', 'N'),
    (106, 'Y', 18, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 13, '생산량 직접 감소', 'system', 'system', 'N'),
    (107, 'Y', 18, 'supply_disruption_intensity', '공급 차질 - 강도', 'direct', 17, 'upstream 생산 차질 동반 시', 'system', 'system', 'N'),
    (108, 'Y', 18, 'supply_disruption_pattern', '공급 차질 - 발생/회복 패턴', 'indirect', NULL, 'Ref. 공급 차질 패턴', 'system', 'system', 'N'),
    (109, 'Y', 18, 'demand_response_alternative_lng', '수요측 대응 - 타 공급원 경유 LNG 확보량', 'direct', 14, '대체 공급 형성 time delay 존재', 'system', 'system', 'N'),
    (110, 'Y', 18, 'demand_response_powermix_reduction', '수요측 대응 - PowerMix 조정 등을 통한 LNG 수요 감소량', 'direct', 7, '자체 PowerMix 조정 수요 감소', 'system', 'system', 'N'),
    (111, 'Y', 18, 'demand_response_powermix_reduction', '수요측 대응 - PowerMix 조정 등을 통한 LNG 수요 감소량', 'na', NULL, '해당 섹터 수요 직접 감소 강제 시 : User-Adjusted Gas Demand 미대상 수동 overwrite는 직접 쓰지 않고, 수요 감소는 PowerMix Gas 비중 또는 대상 섹터 proxy로 환산', 'system', 'system', 'N'),
    (112, 'Y', 18, 'trade_restriction', '거래 제한 (계약 취소 등)', 'na', NULL, '제재 대상 계약 취소(partial→full) : 계약 취소·제재 물량은 TradeRestrictionInput 미대상으로 직접 반영하지 않고, 실제 공급 차질로 확인되는 경우 Supply 계열 파라미터로 우회 반영', 'system', 'system', 'N'),
    (113, 'Y', 19, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (114, 'Y', 19, 'implementation_stage', '시행 확실성·단계', 'indirect', NULL, '검토/루머~시행 — 반영 시점·확률 weighting', 'system', 'system', 'N'),
    (115, 'Y', 19, 'impact_intensity', '영향 강도', 'routing', 7, '정책 영향 라우팅 후보(3개 중 선택) : 전원믹스·탄소규제·발전설비·연료전환 정책처럼 Gas/Coal/원전/재생 비중이 구조적으로 바뀌면 PowerMix 선택', 'system', 'system', 'N'),
    (116, 'Y', 19, 'impact_intensity', '영향 강도', 'routing', 2, '정책 영향 라우팅 후보(3개 중 선택) : 석탄 가격·세금·보조금·수입규제 등으로 석탄 발전 경제성이 변해 Gas↔Coal 전환 유인이 생기면 Coal Futures Price 선택', 'system', 'system', 'N'),
    (117, 'Y', 19, 'impact_intensity', '영향 강도', 'routing', 3, '정책 영향 라우팅 후보(3개 중 선택) : 가스/LNG 가격·세금·보조금·구매규제 등으로 가스 발전 경제성이 변해 Gas↔Coal 전환 유인이 생기면 Gas Futures Price 선택', 'system', 'system', 'N'),
    (118, 'Y', 19, 'impact_intensity', '영향 강도', 'na', NULL, '발전·수요 정책 (탄소·전환 등) : User-Adjusted Gas Demand 미대상 수동 overwrite는 쓰지 않고, 발전·수요 정책은 PowerMix/CoalGasAdjustment 등 대상 파라미터로만 라우팅', 'system', 'system', 'N'),
    (119, 'Y', 19, 'impact_intensity', '영향 강도', 'na', NULL, '수출입 규제(관세·금수) : 관세·금수 등 TradeRestrictionInput 미대상 항목은 직접 반영하지 않고, 실제 물량 차질이 명확할 때 Supply 파라미터로 별도 반영', 'system', 'system', 'N'),
    (120, 'Y', 19, 'impact_intensity', '영향 강도', 'na', NULL, '운송 규제(FuelEU 등) : 운송 규제·비용은 ShippingCost 미대상이라 직접 반영하지 않고, 물리적 운송 지연·제약이 공급량에 연결될 때만 기간·강도 판단에 사용', 'system', 'system', 'N'),
    (121, 'Y', 19, 'structural_change_yn', '구조적 변동 여부', 'indirect', NULL, '단발/한시 vs 구조적/영구', 'system', 'system', 'N'),
    (122, 'Y', 20, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (123, 'Y', 20, 'change_magnitude', '변동 폭', 'indirect', NULL, '환율·금리 직접 input 없음 — 구매력→수요 효과는 간접(필요 시 경기(PMI) 변동으로 대용 해석)', 'system', 'system', 'N'),
    (124, 'Y', 20, 'persistence', '지속성', 'indirect', NULL, '일시/단기/구조적', 'system', 'system', 'N'),
    (125, 'Y', 21, 'period', '기간', 'indirect', NULL, '시간축', 'system', 'system', 'N'),
    (126, 'Y', 21, 'economic_intensity', '강도', 'direct', 6, '산업 수요 단기 변동(PMI 탭) : 단기 경기 충격은 영향 국가의 PMI 조정으로 반영하고, 구조적 장기 성장 변화는 GDP 경로를 우선 검토', 'system', 'system', 'N'),
    (127, 'Y', 21, 'economic_intensity', '강도', 'direct', 4, '구조적 경기(GDP) 변동 : 장기·구조적 경기 변화는 국가별 GDP 경로를 조정하고, 일시적 심리·제조업 충격은 PMI를 우선 사용', 'system', 'system', 'N'),
    (128, 'Y', 21, 'demand_impact', 'LNG 수요 영향 규모', 'na', NULL, '수요 영향분 직접 조정(또는 PMI 강도로 반영) : User-Adjusted Gas Demand는 직접 쓰지 않고, 단기 수요 충격은 PMI, 구조적 수요 변화는 GDP 또는 PowerMix proxy로 우회 반영', 'system', 'system', 'N')
ON DUPLICATE KEY UPDATE
    `use_yn` = VALUES(`use_yn`),
    `event_type_l2_id` = VALUES(`event_type_l2_id`),
    `dimension_key` = VALUES(`dimension_key`),
    `dimension_label` = VALUES(`dimension_label`),
    `mapping_method` = VALUES(`mapping_method`),
    `model_param_id` = VALUES(`model_param_id`),
    `comment` = VALUES(`comment`),
    `mod_usr_logn_id` = 'system',
    `del_yn` = 'N';
```

## 3. tmm_mi_model_param_proposal_m

파라미터 제안 요청과 결과 payload를 저장하는 상태 테이블이다. 요청이 들어오면 `PROCESSING` 상태로 row를 만들고, background 작업 완료 후 proposal payload와 callback 상태를 기록한다.

주요 기준:

- `scenario_id`, `scenario_group_id`, `mi_event_id`를 함께 저장해 callback payload와 추적 기준을 고정한다.
- proposal 생성 상태(`status`)와 callback 전송 상태(`callback_status`)는 분리한다.
- `proposal_parameters`, `confirmation_parameters`는 동일한 flat parameter array를 JSON으로 저장한다.
- 각 parameter item은 `parameterName`, `modelTarget`, parameter `rationale`, `values[]`를 가진다.
- `modelTarget.tableName`은 required이고 `valueFieldNames`는 phase 1에서 정확히 1개다.
- 각 value row는 `slotAxesFieldNames`에 선언된 dynamic key, `basecaseValue`, `proposedValue`, row `rationale`를 가진다.
- `proposal_parameters`의 `proposedValue`는 Agent 초기 제안값, `confirmation_parameters`의 `proposedValue`는 사용자 최종 확정값이며 나머지 필드는 동일해야 한다.
- `basecaseValue`는 payload에 보존하지만 FE 표에는 표시하지 않는다. FE는 `proposedValue`만 표시·수정하며, 미수정 시에도 Agent 초기 제안값을 그대로 confirmation으로 전달한다.
- physical DDL의 JSON type CHECK는 `JSON_TYPE(...) = 'ARRAY'`를 유지한다.
- flat array의 필드·validation canonical contract는 `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` 5.2/5.5/6절을 따른다.
- proposal 이력은 사용자 확정 이력을 포함할 수 있으므로 FK delete는 `RESTRICT` 기준이다.
- 2026-07-03 Notion confirmed contract에서는 proposal request body를 사용하지 않는다. Backend가 callback endpoint/URL을 결정해 Agent 팀에 전달하고, Agent는 전달받은 URL을 배포 운영 설정값으로 등록·관리한다.
- callback payload는 `{status,message,data}` wrapper를 사용하며 `callbackCorrelationId`, `callbackEventId`, `callbackSchemaVersion`은 없다.
- `callbackCorrelationId`, `callbackEventId`, schema version 저장은 Backend가 제안한 Agent 계약 확장 후보다. Agent 협의 전에는 proposal row의 confirmed column 요구사항이 아니다.
- callback 인증, ack, retry/error 계약의 미결 사항은 `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md` 5.4절을 따른다.

Status:

- `PROCESSING`: 요청 접수 후 background 생성 중
- `PROPOSED`: proposal 생성 완료
- `CONFIRMED`: 사용자가 confirmation API로 확정 완료
- `FAILED`: proposal 생성 또는 저장 실패

Callback status:

- `PENDING`: callback 전송 전
- `SUCCEEDED`: callback 성공
- `FAILED`: 마지막 callback 실패
- `SKIPPED`: 운영 설정 또는 판단으로 미전송

DDL (2026-07-07 DB 반영본 — ERD blocker #1 해소):

```sql
CREATE TABLE `tmm_mi_model_param_proposal_m` (
    `model_param_proposal_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '수급모델 파라미터 제안 ID',
    `scenario_id` INT UNSIGNED NOT NULL COMMENT '시나리오 ID',
    `scenario_group_id` INT UNSIGNED NOT NULL COMMENT '시나리오 그룹 ID',
    `mi_event_id` INT UNSIGNED NOT NULL COMMENT 'MI 이벤트 ID',
    `request_usr_id` INT UNSIGNED NULL COMMENT '요청자 사용자 ID',
    `status` VARCHAR(50) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PROCESSING' COMMENT '상태 (PROCESSING/PROPOSED/CONFIRMED/FAILED)',
    `proposal_parameters` JSON NULL COMMENT '제안 파라미터 배열 JSON',
    `confirmation_parameters` JSON NULL COMMENT '확정 파라미터 배열 JSON',
    `proposal_completed_dtm` DATETIME(6) NULL COMMENT '제안 완료일시',
    `confirmed_dtm` DATETIME(6) NULL COMMENT '확정일시',
    `error_code` VARCHAR(100) COLLATE utf8mb4_bin NULL COMMENT '오류 코드',
    `error_message` TEXT COLLATE utf8mb4_bin NULL COMMENT '오류 메시지',
    `callback_status` VARCHAR(50) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PENDING' COMMENT 'Callback 상태',
    `callback_sent_dtm` DATETIME(6) NULL COMMENT 'Callback 성공일시',

    `crt_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '생성자',
    `crt_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    `mod_usr_logn_id` VARCHAR(100) COLLATE utf8mb4_bin NOT NULL DEFAULT 'system' COMMENT '수정자',
    `mod_dtm` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    `del_yn` CHAR(1) COLLATE utf8mb4_bin NOT NULL DEFAULT 'N' COMMENT '삭제여부',

    PRIMARY KEY (`model_param_proposal_id`),
    KEY `idx_tmm_mi_model_param_proposal_m_01` (`scenario_id`, `model_param_proposal_id`),
    KEY `idx_tmm_mi_model_param_proposal_m_02` (`status`, `model_param_proposal_id`),
    KEY `idx_tmm_mi_model_param_proposal_m_03` (`callback_status`, `model_param_proposal_id`),
    KEY `idx_tmm_mi_model_param_proposal_m_04` (`scenario_group_id`),
    KEY `idx_tmm_mi_model_param_proposal_m_05` (`mi_event_id`),

    CONSTRAINT `fk_tmm_mi_model_param_proposal_m_01` FOREIGN KEY (`scenario_id`) REFERENCES `tmm_mi_scenario_m` (`scenario_id`),
    CONSTRAINT `fk_tmm_mi_model_param_proposal_m_02` FOREIGN KEY (`scenario_group_id`) REFERENCES `tmm_mi_scenario_group_m` (`scenario_group_id`),
    CONSTRAINT `fk_tmm_mi_model_param_proposal_m_03` FOREIGN KEY (`mi_event_id`) REFERENCES `tmm_mi_mievent_m` (`mi_event_id`),
    CONSTRAINT `ck_tmm_mi_model_param_proposal_m_01` CHECK (`status` IN ('PROCESSING','PROPOSED','CONFIRMED','FAILED')),
    CONSTRAINT `ck_tmm_mi_model_param_proposal_m_02` CHECK (`callback_status` IN ('PENDING','SUCCEEDED','FAILED','SKIPPED')),
    CONSTRAINT `ck_tmm_mi_model_param_proposal_m_03` CHECK ((`proposal_parameters` IS NULL) OR (JSON_TYPE(`proposal_parameters`) = 'ARRAY')),
    CONSTRAINT `ck_tmm_mi_model_param_proposal_m_04` CHECK ((`confirmation_parameters` IS NULL) OR (JSON_TYPE(`confirmation_parameters`) = 'ARRAY'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='수급모델 파라미터 제안';
```

Backend read 계약 확정 사항(위 DDL 근거):

- proposal PK `model_param_proposal_id` → API `modelParamProposalId`. identity는 Agent 소유.
- `scenario_id`/`scenario_group_id`/`mi_event_id` 모두 `NOT NULL` + FK. request snapshot 식별자로 read/cross-check.
- `status` `VARCHAR(50)`, CHECK로 `PROCESSING|PROPOSED|CONFIRMED|FAILED` 4종 고정. Backend `AgentProposalStatus` enum과 문자열·casing 일치. Backend 전용 상태(`CONFIRMING/EXECUTING/EXECUTION_FAILED`)는 orchestration 행 overlay이며 이 컬럼에 쓰지 않는다.
- `proposal_parameters`/`confirmation_parameters`: native `JSON`, `JSON_TYPE=ARRAY`(or NULL) CHECK. flat parameter array 계약과 일치. `PROCESSING`/`FAILED`에서 `proposal_parameters` NULL, `PROPOSED`에서 채워짐. `confirmation_parameters`는 `CONFIRMED` 전 NULL.
- API `completedAt` 소스 = `proposal_completed_dtm` 컬럼(파생 아님). `confirmed_dtm`은 confirmation 시각.
- API `failureReason` 소스 = `error_message`. 추가로 구조화 `error_code`(`VARCHAR(100)`) 존재 — Feature Spec P0의 `completedAt`/`errorCode` 확장이 Agent 스키마에 **이미 존재**함을 뜻한다(`schemaVersion`은 없음).
- `callback_status`/`callback_sent_dtm`은 Agent 전송 상태로 Backend read 서빙에서 무시(재조회로 대체).

미해소(주의):

- **`scenario_id` UNIQUE 제약이 없다.** "시나리오당 제안 1회"는 DB 제약이 아니라 애플리케이션 가드(활성 proposal 존재 시 `409`)로만 보장된다. `idx_01`이 `(scenario_id, model_param_proposal_id)`인 점은 "시나리오별 최신 proposal 조회(latest-wins)" 접근 패턴 및 이력 row(1:N) 허용을 시사한다. callback correlation(path `scenarioId`)은 "해당 scenario의 활성/최신 proposal"로 해석하며, 이력 row와의 혼동은 body `modelParamProposalId` 교차검증 + monotonic guard로 흡수한다. 동시 POST race를 막으려면 partial-unique(generated column) 또는 app-level lock 도입을 Agent/DBA와 협의한다(P1 재제안 정책과 연동).
- ERD blocker #5(식별자 비정규화 immutability)는 SQL CHECK로 강제 불가 — Agent가 request 시점 snapshot으로 채우고 Backend가 callback에서 dispatch snapshot과 cross-check.
