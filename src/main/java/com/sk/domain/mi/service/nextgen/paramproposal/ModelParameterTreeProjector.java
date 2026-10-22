package com.sk.domain.mi.service.nextgen.paramproposal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamFileRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamLeafRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamRenderingRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamSheetRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamTableRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamTargetRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamAlign;
import com.sk.domain.mi.dto.response.nextgen.ModelParamColumn;
import com.sk.domain.mi.dto.response.nextgen.ModelParamColumnKind;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamSlotAxisResolver.InvalidSlotAxisMetadataException;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterResponseProjector.InvalidParameterPayloadException;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelTableDisplayNameResolver.ModelTableKey;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
import com.sk.domain.shared.repository.master.tmm.MiModelParamRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelParameterTreeProjector {

    private static final TypeReference<Map<String, Object>> DYNAMIC_VALUE_TYPE = new TypeReference<>() {};
    private static final Pattern COLUMN_ROLE_BOUNDARY = Pattern.compile(",(?=\\s*[A-Za-z]{1,3}:)");

    private final ObjectMapper objectMapper;
    private final MiModelParamRepository miModelParamRepository;
    private final ModelParameterResponseProjector modelParameterResponseProjector;
    private final ModelParamSlotAxisResolver modelParamSlotAxisResolver;
    private final ModelTableDisplayNameResolver modelTableDisplayNameResolver;

    public List<CurrentModelParamFileRes> project(String parameterJson) {
        return project(parameterJson, false);
    }

    public List<CurrentModelParamFileRes> projectAllowingEmpty(String parameterJson) {
        return project(parameterJson, true);
    }

    private List<CurrentModelParamFileRes> project(String parameterJson, boolean allowEmpty) {
        List<CurrentModelParameterRes> parameters = parseParameters(parameterJson, allowEmpty);
        if (parameters.isEmpty()) {
            return List.of();
        }
        Map<String, List<MiModelParam>> mastersByName = loadMastersByName(parameters);

        Map<String, Map<String, Map<String, List<CurrentModelParamLeafRes>>>> tree = new LinkedHashMap<>();
        Set<MiModelParam> usedMasters = new LinkedHashSet<>();
        for (CurrentModelParameterRes parameter : parameters) {
            MiModelParam master = requiredMaster(mastersByName, parameter);
            usedMasters.add(master);
            CurrentModelParamTargetRes target = parameter.modelTarget();
            validateCoordinates(target, master);

            CurrentModelParameterRes projectedParameter;
            try {
                projectedParameter = modelParameterResponseProjector.projectParameter(
                    parameter,
                    modelParamSlotAxisResolver.resolve(
                        master.getBasecaseColumnRoles(),
                        target.slotAxesFieldNames()
                    )
                );
            } catch (InvalidParameterPayloadException | InvalidSlotAxisMetadataException e) {
                throw invalid();
            }

            CurrentModelParamLeafRes leaf = new CurrentModelParamLeafRes(
                projectedParameter,
                toRendering(master, projectedParameter)
            );
            tree.computeIfAbsent(target.modelFile(), key -> new LinkedHashMap<>())
                .computeIfAbsent(target.modelSheet(), key -> new LinkedHashMap<>())
                .computeIfAbsent(target.tableName(), key -> new ArrayList<>())
                .add(leaf);
        }

        // validateCoordinates로 target 좌표 == 마스터 좌표가 보장되므로 마스터 기준 키로 표시명을 찾는다
        return toResponseTree(tree, modelTableDisplayNameResolver.resolveByTable(usedMasters));
    }

    private List<CurrentModelParameterRes> parseParameters(String json, boolean allowEmpty) {
        if (json == null || json.isBlank()) {
            throw invalid();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw invalid();
        }
        if (root == null || !root.isArray() || (!allowEmpty && root.isEmpty())) {
            throw invalid();
        }

        List<CurrentModelParameterRes> parameters = new ArrayList<>();
        Set<ParameterIdentity> identities = new LinkedHashSet<>();
        for (JsonNode node : root) {
            CurrentModelParameterRes parameter = parseParameter(node);
            if (!identities.add(ParameterIdentity.from(parameter))) {
                throw invalid();
            }
            parameters.add(parameter);
        }
        return List.copyOf(parameters);
    }

    private CurrentModelParameterRes parseParameter(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw invalid();
        }

        String parameterName = requiredText(node.get("parameterName"));
        String rationale = requiredText(node.get("rationale"));
        JsonNode targetNode = node.get("modelTarget");
        if (targetNode == null || !targetNode.isObject()) {
            throw invalid();
        }
        CurrentModelParamTargetRes target = new CurrentModelParamTargetRes(
            requiredText(targetNode.get("modelFile")),
            requiredText(targetNode.get("modelSheet")),
            requiredText(targetNode.get("tableName")),
            requiredStringList(targetNode.get("valueFieldNames")),
            requiredStringList(targetNode.get("slotAxesFieldNames"))
        );

        JsonNode valuesNode = node.get("values");
        if (valuesNode == null || !valuesNode.isArray() || valuesNode.isEmpty()) {
            throw invalid();
        }
        List<Map<String, Object>> values = new ArrayList<>();
        for (JsonNode valueNode : valuesNode) {
            if (!valueNode.isObject()) {
                throw invalid();
            }
            try {
                values.add(objectMapper.convertValue(valueNode, DYNAMIC_VALUE_TYPE));
            } catch (IllegalArgumentException e) {
                throw invalid();
            }
        }
        return new CurrentModelParameterRes(parameterName, target, rationale, null, List.copyOf(values));
    }

    private Map<String, List<MiModelParam>> loadMastersByName(List<CurrentModelParameterRes> parameters) {
        Set<String> parameterNames = parameters.stream()
            .map(CurrentModelParameterRes::parameterName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return miModelParamRepository.findByParameterNameInAndIsDeletedFalse(parameterNames).stream()
            .collect(Collectors.groupingBy(
                MiModelParam::getParameterName,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    private MiModelParam requiredMaster(
        Map<String, List<MiModelParam>> mastersByName,
        CurrentModelParameterRes parameter
    ) {
        List<MiModelParam> masters = mastersByName.get(parameter.parameterName());
        if (masters == null || masters.size() != 1) {
            throw invalid();
        }
        return masters.get(0);
    }

    private void validateCoordinates(CurrentModelParamTargetRes target, MiModelParam master) {
        if (!Objects.equals(target.modelFile(), master.getModelFile())
            || !Objects.equals(target.modelSheet(), master.getModelSheet())
            || !Objects.equals(target.tableName(), master.getModelTable())) {
            throw invalid();
        }
    }

    private CurrentModelParamRenderingRes toRendering(
        MiModelParam master,
        CurrentModelParameterRes parameter
    ) {
        Set<String> slotAxisNames = parameter.modelTarget().slotAxesFieldNames().stream()
            .map(axis -> axis.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        JsonNode values = objectMapper.valueToTree(parameter.values());
        List<ModelParamColumn> columns = parseColumnRoles(master.getBasecaseColumnRoles(), slotAxisNames, values);
        if (master.getBasecaseReadMode() == BasecaseReadMode.MATRIX_TIME_COLUMNS) {
            columns = appendMatrixColumns(
                columns,
                parameter.modelTarget().slotAxesFieldNames(),
                parameter.modelTarget().valueFieldNames(),
                values
            );
        }
        return new CurrentModelParamRenderingRes(
            master.getBasecaseReadMode(),
            columns,
            master.getSemanticAxes()
        );
    }

    private List<ModelParamColumn> parseColumnRoles(
        String columnRoles,
        Set<String> slotAxisNames,
        JsonNode values
    ) {
        if (columnRoles == null || columnRoles.isBlank()) {
            return List.of();
        }
        List<ModelParamColumn> columns = new ArrayList<>();
        for (String token : COLUMN_ROLE_BOUNDARY.split(columnRoles)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            String column = separator < 0 ? null : trimmed.substring(0, separator).trim();
            String role = separator < 0 ? trimmed : trimmed.substring(separator + 1).trim();
            ModelParamColumnKind kind = slotAxisNames.contains(role.toLowerCase(Locale.ROOT))
                ? ModelParamColumnKind.SLOT
                : ModelParamColumnKind.VALUE;
            columns.add(new ModelParamColumn(column, role, kind, resolveAlign(kind, role, values)));
        }
        return columns;
    }

    private List<ModelParamColumn> appendMatrixColumns(
        List<ModelParamColumn> fixedColumns,
        List<String> slotAxes,
        List<String> valueFields,
        JsonNode values
    ) {
        Set<String> existingRoles = fixedColumns.stream()
            .map(column -> column.role().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ModelParamColumn> columns = new ArrayList<>(fixedColumns);
        for (String axis : slotAxes) {
            if (!existingRoles.contains(axis.toLowerCase(Locale.ROOT))) {
                columns.add(new ModelParamColumn(
                    null,
                    axis,
                    ModelParamColumnKind.TIME_AXIS,
                    resolveAlign(ModelParamColumnKind.TIME_AXIS, axis, values)
                ));
            }
        }
        for (String valueField : valueFields) {
            columns.add(new ModelParamColumn(
                null,
                valueField,
                ModelParamColumnKind.VALUE,
                resolveAlign(ModelParamColumnKind.VALUE, valueField, values)
            ));
        }
        return columns;
    }

    private ModelParamAlign resolveAlign(ModelParamColumnKind kind, String role, JsonNode values) {
        JsonNode sample = sampleColumnValue(kind, role, values);
        if (sample == null) {
            return kind == ModelParamColumnKind.VALUE ? ModelParamAlign.RIGHT : ModelParamAlign.LEFT;
        }
        return sample.isNumber() ? ModelParamAlign.RIGHT : ModelParamAlign.LEFT;
    }

    private JsonNode sampleColumnValue(ModelParamColumnKind kind, String role, JsonNode values) {
        if (values == null || !values.isArray()) {
            return null;
        }
        for (JsonNode row : values) {
            JsonNode cell = kind == ModelParamColumnKind.VALUE
                ? firstPresent(row.get("proposedValue"), row.get("basecaseValue"))
                : fieldIgnoreCase(row, role);
            if (cell != null && !cell.isNull()) {
                return cell;
            }
        }
        return null;
    }

    private JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private JsonNode fieldIgnoreCase(JsonNode row, String fieldName) {
        if (row == null || !row.isObject()) {
            return null;
        }
        JsonNode exact = row.get(fieldName);
        if (exact != null) {
            return exact;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().equalsIgnoreCase(fieldName)) {
                return field.getValue();
            }
        }
        return null;
    }

    private List<CurrentModelParamFileRes> toResponseTree(
        Map<String, Map<String, Map<String, List<CurrentModelParamLeafRes>>>> tree,
        Map<ModelTableKey, String> displayTableNames
    ) {
        return tree.entrySet().stream()
            .map(fileEntry -> new CurrentModelParamFileRes(
                fileEntry.getKey(),
                fileEntry.getValue().entrySet().stream()
                    .map(sheetEntry -> new CurrentModelParamSheetRes(
                        sheetEntry.getKey(),
                        sheetEntry.getValue().entrySet().stream()
                            .map(tableEntry -> new CurrentModelParamTableRes(
                                tableEntry.getKey(),
                                ModelTableDisplayNameResolver.displayTableNameOrDefault(
                                    displayTableNames,
                                    new ModelTableKey(fileEntry.getKey(), sheetEntry.getKey(), tableEntry.getKey())
                                ),
                                List.copyOf(tableEntry.getValue())
                            ))
                            .toList()
                    ))
                    .toList()
            ))
            .toList();
    }

    private String requiredText(JsonNode node) {
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw invalid();
        }
        return node.textValue();
    }

    private List<String> requiredStringList(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw invalid();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode valueNode : node) {
            values.add(requiredText(valueNode));
        }
        return List.copyOf(values);
    }

    private InvalidModelParameterTreeException invalid() {
        return new InvalidModelParameterTreeException();
    }

    private record ParameterIdentity(
        String parameterName,
        String modelFile,
        String modelSheet,
        String tableName,
        List<String> valueFieldNames,
        List<String> slotAxesFieldNames
    ) {

        private static ParameterIdentity from(CurrentModelParameterRes parameter) {
            CurrentModelParamTargetRes target = parameter.modelTarget();
            return new ParameterIdentity(
                parameter.parameterName(),
                target.modelFile(),
                target.modelSheet(),
                target.tableName(),
                target.valueFieldNames(),
                target.slotAxesFieldNames()
            );
        }
    }

    public static final class InvalidModelParameterTreeException extends RuntimeException {

        private InvalidModelParameterTreeException() {
            super("Model parameter tree projection failed.");
        }
    }
}
