package com.sk.domain.mi.service.nextgen.support;

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
 * <p>라벨의 source of truth 는 event_type_l2 별 {@code template_json} 이다.
 * 그룹의 {@code dimension_selected}(표시할 dim_key 배열)로 헤더 순서를 정하고,
 * 시나리오의 {@code dimension_values}(dim_key → 값) 를 template 의 label 과 병합해 셀을 만든다.
 *
 * <p>실제 JSON 구조:
 * <pre>
 * template_json     : [{"dim_key":"capacity_change","label":"생산능력 변화",
 *                       "value_enums":["&lt;5%","15%","30%"],"required":false,"display_order":2}]
 * dimension_selected: ["period_label","capacity_change"]
 * dimension_values  : {"capacity_change":"15%","recovery":"4~6주 대다수 재기동"}
 * </pre>
 *
 */
@Slf4j
public final class ScenarioDimensionMapper {

    private ScenarioDimensionMapper() {
    }

    /** event_type_l2_id → 파싱된 template. */
    public static Map<Long, GroupDimensionTemplate> parseTemplates(List<ScenarioDimensionTemplateRow> rows) {
        Map<Long, GroupDimensionTemplate> result = new LinkedHashMap<>();
        for (ScenarioDimensionTemplateRow row : rows) {
            result.put(row.eventTypeL2Id(), GroupDimensionTemplate.parse(row.templateJson()));
        }
        return result;
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
     * 분기항목/분기기준 템플릿 조회 API에서 사용한다.
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
     * 한 그룹(event_type_l2)의 dimension 정의. dim_key → label.
     */
    public static final class GroupDimensionTemplate {

        public static final GroupDimensionTemplate EMPTY = new GroupDimensionTemplate(Map.of());

        /** dim_key → label */
        private final Map<String, String> labelByKey;

        private GroupDimensionTemplate(Map<String, String> labelByKey) {
            this.labelByKey = labelByKey;
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
                Map<String, String> labelByKey = new LinkedHashMap<>();
                for (DimTemplate t : parsed) {
                    if (t != null && t.dimKey() != null) {
                        labelByKey.put(t.dimKey(), t.label());
                    }
                }
                return new GroupDimensionTemplate(labelByKey);
            } catch (Exception e) {
                log.warn("template_json 파싱 실패: {}", e.getMessage());
                return EMPTY;
            }
        }

        /** dimension_selected 순서대로 표 헤더(key, label) 생성. */
        public List<ScenarioDimensionColumnRes> columns(List<String> selectedKeys) {
            List<ScenarioDimensionColumnRes> columns = new ArrayList<>(selectedKeys.size());
            for (String key : selectedKeys) {
                columns.add(new ScenarioDimensionColumnRes(key, label(key)));
            }
            return columns;
        }

        /** dimension_selected 순서대로 시나리오 한 행의 셀 생성. value 자체가 표시값이라 valueLabel=value. */
        public List<ScenarioDimensionRes> dimensions(List<String> selectedKeys, String dimensionValuesJson) {
            Map<String, String> values = parseValues(dimensionValuesJson);
            List<ScenarioDimensionRes> dimensions = new ArrayList<>(selectedKeys.size());
            for (String key : selectedKeys) {
                String value = values.get(key);
                dimensions.add(new ScenarioDimensionRes(key, label(key), value, value));
            }
            return dimensions;
        }

        private String label(String key) {
            String label = labelByKey.get(key);
            return label != null ? label : key;
        }

        // dimension_values: {"dim_key":"표시값", ...} 객체
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
    }

    // template_json 파싱용 (snake_case)
    private record DimTemplate(
        @JsonProperty("dim_key") String dimKey,
        @JsonProperty("label") String label,
        @JsonProperty("value_enums") List<String> valueEnums,
        @JsonProperty("required") Boolean required,
        @JsonProperty("display_order") Integer displayOrder
    ) {
    }

}
