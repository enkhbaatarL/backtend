package com.sk.domain.mi.service.nextgen.paramproposal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParameterRes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelParameterResponseProjector {

    private static final String GROUPED_RATIONALES_FIELD = "groupedRationales";
    private static final String RATIONALE_FIELD = "rationale";

    private final ObjectMapper objectMapper;

    public JsonNode projectParameterArray(JsonNode parameterArray, List<List<String>> groupingAxesByParameter) {
        if (parameterArray == null || !parameterArray.isArray()) {
            throw invalid("parameter payload must be an array.");
        }
        if (groupingAxesByParameter == null || groupingAxesByParameter.size() != parameterArray.size()) {
            throw invalid("parameter grouping axes must match the parameter payload.");
        }

        JsonNode projected = parameterArray.deepCopy();
        int parameterIndex = 0;
        for (JsonNode parameter : projected) {
            if (!(parameter instanceof ObjectNode parameterObject)) {
                throw invalid("parameter payload contains a non-object item.");
            }
            addGroupedRationales(parameterObject, groupingAxesByParameter.get(parameterIndex));
            parameterIndex++;
        }
        return projected;
    }

    public CurrentModelParameterRes projectParameter(CurrentModelParameterRes parameter, List<String> groupingAxes) {
        if (parameter == null) {
            throw invalid("parameter must not be null.");
        }
        try {
            ObjectNode parameterObject = objectMapper.valueToTree(parameter);
            addGroupedRationales(parameterObject, groupingAxes);
            return objectMapper.convertValue(parameterObject, CurrentModelParameterRes.class);
        } catch (InvalidParameterPayloadException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw invalid("parameter response projection failed.");
        }
    }

    private void addGroupedRationales(ObjectNode parameter, List<String> groupingAxes) {
        List<String> slotAxes = slotAxes(parameter);
        validateGroupingAxes(slotAxes, groupingAxes);
        JsonNode valuesNode = parameter.get("values");
        if (valuesNode == null || !valuesNode.isArray() || valuesNode.isEmpty()) {
            throw invalid("parameter values must be a non-empty array.");
        }

        Map<GroupKey, GroupAccumulator> groups = new LinkedHashMap<>();
        for (JsonNode valueNode : valuesNode) {
            if (!(valueNode instanceof ObjectNode valueObject)) {
                throw invalid("parameter values contain a non-object item.");
            }

            Map<String, JsonNode> slotValues = new LinkedHashMap<>();
            for (String slotAxis : slotAxes) {
                JsonNode slotValue = valueObject.get(slotAxis);
                if (slotValue == null) {
                    throw invalid("parameter value is missing a required slot axis.");
                }
                canonicalSlot(slotValue);
                slotValues.put(slotAxis, slotValue);
            }

            List<CanonicalSlot> canonicalSlots = new ArrayList<>(groupingAxes.size());
            ObjectNode groupedItem = objectMapper.createObjectNode();
            for (String slotAxis : groupingAxes) {
                JsonNode slotValue = slotValues.get(slotAxis);
                canonicalSlots.add(canonicalSlot(slotValue));
                groupedItem.set(slotAxis, slotValue.deepCopy());
            }

            JsonNode rationaleNode = valueObject.get(RATIONALE_FIELD);
            if (rationaleNode == null || !rationaleNode.isTextual()) {
                throw invalid("parameter value rationale must be a string.");
            }

            GroupKey groupKey = new GroupKey(List.copyOf(canonicalSlots));
            groups.computeIfAbsent(groupKey, key -> new GroupAccumulator(groupedItem))
                .addRationale(rationaleNode.textValue());
        }

        ArrayNode groupedRationales = objectMapper.createArrayNode();
        groups.values().stream()
            .map(GroupAccumulator::toResponseItem)
            .forEach(groupedRationales::add);
        parameter.set(GROUPED_RATIONALES_FIELD, groupedRationales);
    }

    private void validateGroupingAxes(List<String> slotAxes, List<String> groupingAxes) {
        if (groupingAxes == null) {
            throw invalid("parameter grouping axes must not be null.");
        }
        Set<String> uniqueGroupingAxes = new LinkedHashSet<>(groupingAxes);
        if (uniqueGroupingAxes.size() != groupingAxes.size() || !slotAxes.containsAll(groupingAxes)) {
            throw invalid("parameter grouping axes must be unique declared slot axes.");
        }
    }

    private List<String> slotAxes(ObjectNode parameter) {
        JsonNode modelTarget = parameter.get("modelTarget");
        if (modelTarget == null || !modelTarget.isObject()) {
            throw invalid("parameter modelTarget must be an object.");
        }
        JsonNode slotAxesNode = modelTarget.get("slotAxesFieldNames");
        if (slotAxesNode == null || !slotAxesNode.isArray() || slotAxesNode.isEmpty()) {
            throw invalid("parameter slotAxesFieldNames must be a non-empty array.");
        }

        List<String> slotAxes = new ArrayList<>();
        Set<String> uniqueSlotAxes = new LinkedHashSet<>();
        for (JsonNode slotAxisNode : slotAxesNode) {
            if (!slotAxisNode.isTextual() || slotAxisNode.textValue().isBlank()) {
                throw invalid("parameter slotAxesFieldNames must contain non-blank strings.");
            }
            String slotAxis = slotAxisNode.textValue();
            if (!uniqueSlotAxes.add(slotAxis)) {
                throw invalid("parameter slotAxesFieldNames contain a duplicated field.");
            }
            slotAxes.add(slotAxis);
        }
        return List.copyOf(slotAxes);
    }

    private CanonicalSlot canonicalSlot(JsonNode slotValue) {
        if (slotValue.isNumber()) {
            BigDecimal canonicalNumber = slotValue.decimalValue().stripTrailingZeros();
            return new CanonicalSlot("N", canonicalNumber.toPlainString());
        }
        if (slotValue.isTextual()) {
            return new CanonicalSlot("S", slotValue.textValue());
        }
        if (slotValue.isBoolean()) {
            return new CanonicalSlot("B", Boolean.toString(slotValue.booleanValue()));
        }
        throw invalid("parameter slot axis must be a string, boolean, or number.");
    }

    private InvalidParameterPayloadException invalid(String message) {
        return new InvalidParameterPayloadException(message);
    }

    private record GroupKey(List<CanonicalSlot> slots) {
    }

    private record CanonicalSlot(String type, String value) {
    }

    private static final class GroupAccumulator {

        private final ObjectNode responseItem;
        private final List<String> rationales = new ArrayList<>();

        private GroupAccumulator(ObjectNode responseItem) {
            this.responseItem = responseItem;
        }

        private void addRationale(String rationale) {
            rationales.add(rationale);
        }

        private ObjectNode toResponseItem() {
            responseItem.put(RATIONALE_FIELD, String.join("\n", rationales));
            return responseItem;
        }
    }

    public static final class InvalidParameterPayloadException extends RuntimeException {

        private InvalidParameterPayloadException(String message) {
            super(message);
        }
    }
}
