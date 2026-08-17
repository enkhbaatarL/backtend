package com.sk.domain.mi.service.nextgen.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDimensionTemplateRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 시나리오 dimension 표(헤더 + 셀) 조립기.
 *
 * 분기항목 정의의 출처가 그룹 세대(api_version)에 따라 둘로 갈린다.
 *
 * v1 (template 기반): event_type_l2 별 template_json이 정의의 원본이다. dim_key는 전역 고정 의미 키이고
 * dimension_values의 값 자체가 표시값이다.
 * template_json      : [{"dim_key":"capacity_change","label":"생산능력 변화","value_enums":["5%","15%"],...}]
 * dimension_values   : {"capacity_change":"15%"}
 *
 * v4 (definitions 기반, 멀티 이벤트): 그룹 자신의 dimension_definitions가 정의의 원본이다.
 * dimensionId(d1...)는 그룹 로컬 채번이며 dimension_values에는 value_id가 저장되어 정의의 values와 조인해
 * 표시 라벨과 값 사유(assumption_rationale)를 해석한다.
 * dimension_definitions : [{"dimension_id":"d1","label":"...","values":[{"value_id":"d1_v1","label":"...",...}],...}]
 * dimension_values      : {"d1":"d1_v1"}
 *
 * 판별은 definitions가 비어있는지로 한다 (v1-v3 그룹은 DB default 빈 배열). resolve 참조.
 * dimensionId는 그룹 로컬이므로 파싱 결과를 그룹 간에 공유(캐시/병합)하면 안 된다.
 */
@Slf4j
public final class ScenarioDimensionMapper {

    private ScenarioDimensionMapper() {
    }

    /** event_type_l2_id → 파싱된 template. (v1 경로) */
    public static Map<Long, GroupDimensionTemplate> parseTemplates(List<ScenarioDimensionTemplateRow> rows) {
        Map<Long, GroupDimensionTemplate> result = new LinkedHashMap<>();
        for (ScenarioDimensionTemplateRow row : rows) {
            result.put(row.eventTypeL2Id(), GroupDimensionTemplate.parse(row.templateJson()));
        }
        return result;
    }

    /**
     * 그룹의 dimension 정의 결정.
     * dimension_definitions가 비어있지 않으면 v4 definitions 기반, 아니면 event_type_l2 template 기반으로 폴백한다.
     */
    public static GroupDimensionTemplate resolve(String dimensionDefinitionsJson, Map<Long, GroupDimensionTemplate> templatesByL2, Long eventTypeL2Id) {
        GroupDimensionTemplate definitions = GroupDimensionTemplate.parseDefinitions(dimensionDefinitionsJson);
        if (!definitions.isEmpty()) {
            return definitions;
        }
        if (eventTypeL2Id == null) {
            return GroupDimensionTemplate.EMPTY;
        }
        return templatesByL2.getOrDefault(eventTypeL2Id, GroupDimensionTemplate.EMPTY);
    }

    /**
     * 그룹이 v4 definitions 기반인지. 확률 스케일 판정에 사용한다 —
     * v4는 0~1 비율, v1 template 기반은 0~100 정수 (2026-08-14 확정 실통으로 확정).
     */
    public static boolean isDefinitionBased(String dimensionDefinitionsJson) {
        return !GroupDimensionTemplate.parseDefinitions(dimensionDefinitionsJson).isEmpty();
    }

    /** 표시할 dim_key 배열(JSON: {@code ["key1","key2"]}) 파싱. */
    public static List<String> parseSelectedKeys(String dimensionSelectedJson) {
        if (dimensionSelectedJson == null || dimensionSelectedJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> keys = JsonUtil.fromJson(dimensionSelectedJson, new TypeReference<List<String>>() {});
            return keys == null ? List.of() : keys;
        } catch (Exception e) {
            log.warn("dimension_selected 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * event_type_l2 template_json → 분기항목 템플릿 Item 전체 목록(dimKey/label/required/displayOrder/valueEnums).
     * 분기항목/분기기준 템플릿 조회 API에서 사용한다. (v1 전용)
     */
    public static List<ScenarioDimensionTemplateRes.Item> parseTemplateItems(String templateJson) {
        if (templateJson == null || templateJson.isBlank()) {
            return List.of();
        }
        try {
            List<DimTemplate> parsed = JsonUtil.fromJson(templateJson, new TypeReference<List<DimTemplate>>() {});
            if (parsed == null) {
                return List.of();
            }
            return parsed.stream()
                .filter(t -> t != null && t.dimKey() != null)
                .map(t -> new ScenarioDimensionTemplateRes.Item(
                    t.dimKey(), t.label(), t.required(), t.displayOrder(), t.valueEnums()))
                .toList();
        } catch (Exception e) {
            log.warn("template_json items 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 한 그룹의 dimension 정의. v1 template과 v4 definitions 양쪽을 같은 인터페이스로 감싼다.
     */
    public static final class GroupDimensionTemplate {

        public static final GroupDimensionTemplate EMPTY = new GroupDimensionTemplate(Map.of(), false);

        /** dim_key(v1) 또는 dimensionId(v4) → 컬럼 메타 */
        private final Map<String, ColumnMeta> columnsByKey;
        private final boolean definitionBased;

        private GroupDimensionTemplate(Map<String, ColumnMeta> columnsByKey, boolean definitionBased) {
            this.columnsByKey = columnsByKey;
            this.definitionBased = definitionBased;
        }

        static GroupDimensionTemplate parse(String templateJson) {
            if (templateJson == null || templateJson.isBlank()) {
                return EMPTY;
            }
            try {
                List<DimTemplate> parsed = JsonUtil.fromJson(templateJson, new TypeReference<List<DimTemplate>>() {});
                if (parsed == null) {
                    return EMPTY;
                }
                Map<String, ColumnMeta> columns = new LinkedHashMap<>();
                for (DimTemplate t : parsed) {
                    if (t != null && t.dimKey() != null) {
                        columns.put(t.dimKey(), new ColumnMeta(t.label(), null, null, Map.of()));
                    }
                }
                return new GroupDimensionTemplate(columns, false);
            } catch (Exception e) {
                log.warn("template_json 파싱 실패: {}", e.getMessage());
                return EMPTY;
            }
        }

        /** v4 dimension_definitions 파싱. 빈 배열(v1-v3 그룹)이면 EMPTY. */
        static GroupDimensionTemplate parseDefinitions(String dimensionDefinitionsJson) {
            if (dimensionDefinitionsJson == null || dimensionDefinitionsJson.isBlank()) {
                return EMPTY;
            }
            try {
                List<DimensionDefinition> parsed =
                    JsonUtil.fromJson(dimensionDefinitionsJson, new TypeReference<List<DimensionDefinition>>() {});
                if (parsed == null || parsed.isEmpty()) {
                    return EMPTY;
                }
                Map<String, ColumnMeta> columns = new LinkedHashMap<>();
                for (DimensionDefinition d : parsed) {
                    if (d == null || d.dimensionId() == null) {
                        continue;
                    }
                    Map<String, ValueMeta> valuesById = new LinkedHashMap<>();
                    if (d.values() != null) {
                        for (DefinitionValue v : d.values()) {
                            if (v != null && v.valueId() != null) {
                                valuesById.put(v.valueId(),
                                    new ValueMeta(v.label(), v.assumptionRationale()));
                            }
                        }
                    }
                    columns.put(d.dimensionId(), new ColumnMeta(d.label(), d.description(), d.rationale(), valuesById));
                }
                return columns.isEmpty() ? EMPTY : new GroupDimensionTemplate(columns, true);
            } catch (Exception e) {
                log.warn("dimension_definitions 파싱 실패: {}", e.getMessage());
                return EMPTY;
            }
        }

        /** v4 definitions 기반 여부 (확률 스케일 등 세대 분기에 사용). */
        public boolean isDefinitionBased() {
            return definitionBased;
        }

        public boolean isEmpty() {
            return columnsByKey.isEmpty();
        }

        /** dimension_selected 순서대로 표 헤더(key, label, 사유) 생성. */
        public List<ScenarioDimensionColumnRes> columns(List<String> selectedKeys) {
            List<ScenarioDimensionColumnRes> columns = new ArrayList<>(selectedKeys.size());
            for (String key : selectedKeys) {
                ColumnMeta meta = columnsByKey.get(key);
                columns.add(new ScenarioDimensionColumnRes(
                    key,
                    meta != null && meta.label() != null ? meta.label() : key,
                    meta == null ? null : meta.description(),
                    meta == null ? null : meta.rationale()
                ));
            }
            return columns;
        }

        /**
         * dimension_selected 순서대로 시나리오 한 행의 셀 생성. value는 항상 표시 label로 통일한다
         * (검토/Signpost 왕복 계약이 cells=표시 label이므로 FE가 value를 그대로 재전송할 수 있게).
         * v1은 저장값 자체가 표시값이다. v4는 저장된 value_id를 정의의 values와 조인해 label과 사유를 해석하고
         * valueId에 원문 ID를 보존하며, 정의에 없는 값(사용자 정의 등)은 원문을 표시값으로 폴백한다.
         */
        public List<ScenarioDimensionRes> dimensions(List<String> selectedKeys, String dimensionValuesJson) {
            Map<String, String> values = parseValues(dimensionValuesJson);
            List<ScenarioDimensionRes> dimensions = new ArrayList<>(selectedKeys.size());
            for (String key : selectedKeys) {
                ColumnMeta meta = columnsByKey.get(key);
                String label = meta != null && meta.label() != null ? meta.label() : key;
                String rawValue = values.get(key);
                ValueMeta valueMeta = meta == null || rawValue == null ? null : meta.valuesById().get(rawValue);
                String displayValue = valueMeta != null && valueMeta.label() != null ? valueMeta.label() : rawValue;
                dimensions.add(new ScenarioDimensionRes(
                    key,
                    label,
                    displayValue,
                    displayValue,
                    valueMeta == null ? null : rawValue,
                    valueMeta == null ? null : valueMeta.rationale()
                ));
            }
            return dimensions;
        }

        // dimension_values: {"dim_key":"표시값"} (v1) 또는 {"dimensionId":"value_id"} (v4) 객체
        private Map<String, String> parseValues(String dimensionValuesJson) {
            if (dimensionValuesJson == null || dimensionValuesJson.isBlank()) {
                return Collections.emptyMap();
            }
            try {
                Map<String, String> values =
                    JsonUtil.fromJson(dimensionValuesJson, new TypeReference<Map<String, String>>() {});
                return values == null ? Collections.emptyMap() : values;
            } catch (Exception e) {
                log.warn("dimension_values 파싱 실패: {}", e.getMessage());
                return Collections.emptyMap();
            }
        }

        private record ColumnMeta(String label, String description, String rationale, Map<String, ValueMeta> valuesById) {
        }

        private record ValueMeta(String label, String rationale) {
        }
    }

    // template_json 파싱용 (snake_case)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DimTemplate(
        @JsonProperty("dim_key") String dimKey,
        @JsonProperty("label") String label,
        @JsonProperty("value_enums") List<String> valueEnums,
        @JsonProperty("required") Boolean required,
        @JsonProperty("display_order") Integer displayOrder
    ) {
    }

    // dimension_definitions 파싱용 (snake_case). lever_states 등 미사용 필드는 무시한다.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DimensionDefinition(
        @JsonProperty("dimension_id") String dimensionId,
        @JsonProperty("label") String label,
        @JsonProperty("description") String description,
        @JsonProperty("rationale") String rationale,
        @JsonProperty("values") List<DefinitionValue> values
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DefinitionValue(
        @JsonProperty("value_id") String valueId,
        @JsonProperty("label") String label,
        @JsonProperty("assumption_rationale") String assumptionRationale
    ) {
    }

}
