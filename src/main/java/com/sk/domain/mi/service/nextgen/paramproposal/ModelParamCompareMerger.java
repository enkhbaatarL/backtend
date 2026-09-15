package com.sk.domain.mi.service.nextgen.paramproposal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareParameterRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareScenarioRationaleRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.ModelFileNode;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamShape;
import com.sk.domain.mi.dto.response.nextgen.ModelSheetNode;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 완료 시나리오들의 단건 제안 조회 결과를 비교 그리드로 병합한다.
 * 행 병합 키는 파라미터 modelTarget의 slotAxesFieldNames 조합이다.
 * 병합 행은 단건 계약과 동일하게 slot 축을 flat 필드로 유지하고, 시나리오별 값만 cells 배열로 확장한다
 * — 확정/실행 요청 계약(flat row)과 왕복 변환 없이 맞물리게 하기 위한 구조.
 * MI Base는 basecase 원본이 정적이라는 전제 아래 행별로 처음 발견된 non-null basecaseValue를 사용하고,
 * 특정 시나리오가 제안하지 않은 행의 셀은 null로 내려 화면에서 공백 처리한다.
 */
@Component
@RequiredArgsConstructor
public class ModelParamCompareMerger {

    private static final Set<String> META_FIELDS = Set.of("basecaseValue", "proposedValue", "rationale");
    private static final String KEY_SEPARATOR = "\u0001";

    private final ObjectMapper objectMapper;

    public record CompareSource(MiScenario scenario, ModelParamProposalRes proposal) {
    }

    public GroupModelParamCompareRes merge(Long scenarioGroupId, List<CompareSource> sources) {
        List<GroupModelParamCompareScenarioRes> scenarios = new ArrayList<>();
        Map<String, ParameterAccumulator> parameters = new LinkedHashMap<>();
        Map<String, ModelParamShape> shapesByName = new LinkedHashMap<>();
        Map<String, Map<String, Set<String>>> fileTree = new LinkedHashMap<>();

        int columnCount = sources.size();
        int columnIndex = 0;
        for (CompareSource source : sources) {
            scenarios.add(new GroupModelParamCompareScenarioRes(
                source.scenario().getScenarioId(),
                source.scenario().getTitle(),
                source.scenario().getDisplayOrder(),
                source.proposal().modelParamProposalId(),
                source.proposal().status()
            ));
            collectShapes(shapesByName, source);
            accumulate(parameters, fileTree, source, columnIndex, columnCount);
            columnIndex++;
        }

        List<GroupModelParamCompareParameterRes> parameterResults = parameters.values().stream()
            .map(accumulator -> accumulator.toResponse(objectMapper))
            .toList();

        return new GroupModelParamCompareRes(
            scenarioGroupId,
            scenarios,
            parameterResults,
            List.copyOf(shapesByName.values()),
            toModelFiles(fileTree)
        );
    }

    /** parameterName별 표 모양 — 단건과 동일하게 top-level로 모으고 첫 등장 시나리오의 것을 쓴다. */
    private void collectShapes(Map<String, ModelParamShape> shapesByName, CompareSource source) {
        if (source.proposal().parameterShapes() == null) {
            return;
        }
        for (ModelParamShape shape : source.proposal().parameterShapes()) {
            shapesByName.putIfAbsent(shape.parameterName(), shape);
        }
    }

    private void accumulate(
        Map<String, ParameterAccumulator> parameters,
        Map<String, Map<String, Set<String>>> fileTree,
        CompareSource source,
        int columnIndex,
        int columnCount
    ) {
        JsonNode parameterArray = source.proposal().parameters();
        if (parameterArray == null || !parameterArray.isArray()) {
            return;
        }

        for (JsonNode parameter : parameterArray) {
            String parameterName = textOrNull(parameter.get("parameterName"));
            if (parameterName == null || parameterName.isBlank()) {
                continue;
            }
            JsonNode modelTarget = parameter.get("modelTarget");
            ParameterAccumulator accumulator = parameters.computeIfAbsent(parameterName, name -> new ParameterAccumulator(
                name,
                modelTarget == null ? null : modelTarget.deepCopy(),
                declaredSlotAxes(modelTarget),
                columnCount
            ));
            accumulator.scenarioRationales.add(new GroupModelParamCompareScenarioRationaleRes(
                source.scenario().getScenarioId(),
                textOrNull(parameter.get("rationale")),
                copyOrNull(parameter.get("groupedRationales"))
            ));
            addFileTree(fileTree, modelTarget);

            JsonNode values = parameter.get("values");
            if (values == null || !values.isArray()) {
                continue;
            }
            for (JsonNode row : values) {
                if (row != null && row.isObject()) {
                    addRow(accumulator, row, columnIndex);
                }
            }
        }
    }

    private void addRow(ParameterAccumulator accumulator, JsonNode row, int columnIndex) {
        List<String> axes = accumulator.axesFor(row);
        String key = canonicalKey(row, axes);
        RowAccumulator rowAccumulator = accumulator.rows.computeIfAbsent(
            key,
            ignored -> new RowAccumulator(flatSlotsOf(row, axes), accumulator.columnCount)
        );
        JsonNode basecase = row.get("basecaseValue");
        if (rowAccumulator.basecaseValue == null && basecase != null && !basecase.isNull()) {
            rowAccumulator.basecaseValue = basecase.deepCopy();
        }
        ObjectNode cell = objectMapper.createObjectNode();
        cell.set("proposedValue", copyOrNull(row.get("proposedValue")));
        cell.set("rationale", copyOrNull(row.get("rationale")));
        rowAccumulator.cells[columnIndex] = cell;
    }

    /** slot 축 값의 병합 키. 숫자는 trailing zero를 제거해 2027과 2027.0을 같은 슬롯으로 본다. */
    private String canonicalKey(JsonNode row, List<String> axes) {
        StringBuilder key = new StringBuilder();
        for (String axis : axes) {
            if (!key.isEmpty()) {
                key.append(KEY_SEPARATOR);
            }
            key.append(canonicalValue(row.get(axis)));
        }
        return key.toString();
    }

    private String canonicalValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "-";
        }
        if (value.isNumber()) {
            return "N:%s".formatted(value.decimalValue().stripTrailingZeros().toPlainString());
        }
        if (value.isTextual()) {
            return "S:%s".formatted(value.textValue());
        }
        if (value.isBoolean()) {
            return "B:%s".formatted(value.booleanValue());
        }
        return "X:%s".formatted(value.toString());
    }

    /** 병합 행의 뼈대 — 단건 row와 동일하게 slot 축을 flat 필드로 담는다 (원본 표기 유지). */
    private ObjectNode flatSlotsOf(JsonNode row, List<String> axes) {
        ObjectNode slots = objectMapper.createObjectNode();
        for (String axis : axes) {
            JsonNode value = row.get(axis);
            slots.set(axis, value == null ? null : value.deepCopy());
        }
        return slots;
    }

    private List<String> declaredSlotAxes(JsonNode modelTarget) {
        JsonNode axesNode = modelTarget == null ? null : modelTarget.get("slotAxesFieldNames");
        if (axesNode == null || !axesNode.isArray()) {
            return List.of();
        }
        List<String> axes = new ArrayList<>();
        for (JsonNode axis : axesNode) {
            String name = textOrNull(axis);
            if (name != null && !name.isBlank()) {
                axes.add(name);
            }
        }
        return List.copyOf(axes);
    }

    private void addFileTree(Map<String, Map<String, Set<String>>> fileTree, JsonNode modelTarget) {
        if (modelTarget == null || modelTarget.isNull()) {
            return;
        }
        String modelFile = textOrNull(modelTarget.get("modelFile"));
        String modelSheet = textOrNull(modelTarget.get("modelSheet"));
        if (modelFile == null || modelSheet == null) {
            return;
        }
        Set<String> tableNames = fileTree
            .computeIfAbsent(modelFile, key -> new LinkedHashMap<>())
            .computeIfAbsent(modelSheet, key -> new LinkedHashSet<>());
        String tableName = textOrNull(modelTarget.get("tableName"));
        if (tableName != null) {
            tableNames.add(tableName);
        }
    }

    private List<ModelFileNode> toModelFiles(Map<String, Map<String, Set<String>>> fileTree) {
        return fileTree.entrySet().stream()
            .map(fileEntry -> new ModelFileNode(
                fileEntry.getKey(),
                fileEntry.getValue().entrySet().stream()
                    .map(sheetEntry -> new ModelSheetNode(sheetEntry.getKey(), List.copyOf(sheetEntry.getValue())))
                    .toList()
            ))
            .toList();
    }

    private JsonNode copyOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.deepCopy();
    }

    private String textOrNull(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.textValue();
    }

    private static final class ParameterAccumulator {

        private final String parameterName;
        private final JsonNode modelTarget;
        private final List<String> declaredAxes;
        private final int columnCount;
        private final List<GroupModelParamCompareScenarioRationaleRes> scenarioRationales = new ArrayList<>();
        private final Map<String, RowAccumulator> rows = new LinkedHashMap<>();

        private ParameterAccumulator(String parameterName, JsonNode modelTarget, List<String> declaredAxes, int columnCount) {
            this.parameterName = parameterName;
            this.modelTarget = modelTarget;
            this.declaredAxes = declaredAxes;
            this.columnCount = columnCount;
        }

        /** 선언된 slot 축이 없으면 row의 메타 외 필드명으로 폴백해 결정적 키를 만든다. */
        private List<String> axesFor(JsonNode row) {
            if (!declaredAxes.isEmpty()) {
                return declaredAxes;
            }
            List<String> fallback = new ArrayList<>();
            row.fieldNames().forEachRemaining(field -> {
                if (!META_FIELDS.contains(field)) {
                    fallback.add(field);
                }
            });
            fallback.sort(String::compareTo);
            return fallback;
        }

        private GroupModelParamCompareParameterRes toResponse(ObjectMapper objectMapper) {
            ArrayNode values = objectMapper.createArrayNode();
            for (RowAccumulator row : rows.values()) {
                values.add(row.toRow(objectMapper));
            }
            return new GroupModelParamCompareParameterRes(
                parameterName,
                modelTarget,
                List.copyOf(scenarioRationales),
                values
            );
        }
    }

    private static final class RowAccumulator {

        private final ObjectNode flatSlots;
        private final ObjectNode[] cells;
        private JsonNode basecaseValue;

        private RowAccumulator(ObjectNode flatSlots, int columnCount) {
            this.flatSlots = flatSlots;
            this.cells = new ObjectNode[columnCount];
        }

        private ObjectNode toRow(ObjectMapper objectMapper) {
            ObjectNode row = flatSlots.deepCopy();
            row.set("basecaseValue", basecaseValue);
            ArrayNode cellArray = objectMapper.createArrayNode();
            for (ObjectNode cell : cells) {
                cellArray.add(cell == null ? objectMapper.nullNode() : cell);
            }
            row.set("cells", cellArray);
            return row;
        }
    }
}
