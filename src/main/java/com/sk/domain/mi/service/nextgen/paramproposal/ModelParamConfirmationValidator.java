package com.sk.domain.mi.service.nextgen.paramproposal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelInputFile;
import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import com.sk.domain.shared.client.dto.request.ModelParamTarget;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelParamConfirmationValidator {

    private static final Set<String> VALUE_META_FIELDS = Set.of(
        "basecaseValue",
        "proposedValue",
        "rationale"
    );

    private final ObjectMapper objectMapper;

    public void validateAgainstProposal(
        String proposalParametersJson,
        List<ModelParamPayloadItem> confirmationParameters
    ) {
        CanonicalPayload proposal = canonicalize(parseStored(proposalParametersJson), PayloadSource.STORED);
        CanonicalPayload confirmation = canonicalize(confirmationParameters, PayloadSource.REQUEST);
        if (!proposal.structures().equals(confirmation.structures())) {
            throw invalidRequest(
                "confirmationParameters must preserve proposal parameter/model target/slot/basecase structure."
            );
        }
    }

    public boolean matchesConfirmation(
        String confirmationParametersJson,
        List<ModelParamPayloadItem> confirmationParameters
    ) {
        CanonicalPayload stored = canonicalize(parseStored(confirmationParametersJson), PayloadSource.STORED);
        CanonicalPayload requested = canonicalize(confirmationParameters, PayloadSource.REQUEST);
        return stored.equals(requested);
    }

    private List<ModelParamPayloadItem> parseStored(String json) {
        if (json == null || json.isBlank()) {
            throw invalidStoredPayload("Agent parameter payload is empty.");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ModelParamPayloadItem>>() {});
        } catch (JsonProcessingException e) {
            throw invalidStoredPayload("Agent parameter payload is not a valid array.");
        }
    }

    private CanonicalPayload canonicalize(List<ModelParamPayloadItem> items, PayloadSource source) {
        if (items == null || items.isEmpty()) {
            throw invalid(source, "parameter payload must not be empty.");
        }

        Map<ParameterIdentity, ParameterStructure> structures = new LinkedHashMap<>();
        Map<ParameterIdentity, ParameterExecutionValues> executionValues = new LinkedHashMap<>();
        for (ModelParamPayloadItem item : items) {
            if (item == null) {
                throw invalid(source, "parameter payload contains a null item.");
            }
            if (isBlank(item.parameterName())) {
                throw invalid(source, "parameterName is required.");
            }
            if (isBlank(item.rationale())) {
                throw invalid(source, "parameter rationale is required.");
            }

            TargetIdentity target = targetIdentity(item.modelTarget(), source);
            ParameterIdentity identity = new ParameterIdentity(item.parameterName(), target);
            ValueSnapshots snapshots = valueSnapshots(item.values(), target.slotAxes(), source);
            if (structures.putIfAbsent(identity, snapshots.structure()) != null) {
                throw invalid(source, "parameter payload contains a duplicated parameter identity.");
            }
            executionValues.put(identity, snapshots.executionValues());
        }
        return new CanonicalPayload(Map.copyOf(structures), Map.copyOf(executionValues));
    }

    private TargetIdentity targetIdentity(ModelParamTarget target, PayloadSource source) {
        if (target == null
            || isBlank(target.modelFile())
            || isBlank(target.modelSheet())
            || isBlank(target.tableName())) {
            throw invalid(source, "modelTarget modelFile/modelSheet/tableName are required.");
        }

        String modelFile;
        try {
            modelFile = ModelInputFile.proposalTargetFrom(target.modelFile()).name();
        } catch (IllegalArgumentException e) {
            throw invalid(source, "modelTarget.modelFile must be DEMAND, SUPPLY, or TRADE.");
        }

        List<String> valueFields = normalizedNames(target.valueFieldNames(), source, "valueFieldNames");
        if (valueFields.size() != 1) {
            throw invalid(source, "modelTarget.valueFieldNames must contain exactly one field.");
        }
        List<String> slotAxes = normalizedNames(target.slotAxesFieldNames(), source, "slotAxesFieldNames");

        return new TargetIdentity(
            modelFile,
            target.modelSheet(),
            target.tableName(),
            valueFields,
            slotAxes
        );
    }

    private List<String> normalizedNames(List<String> names, PayloadSource source, String fieldName) {
        if (names == null || names.isEmpty() || names.stream().anyMatch(this::isBlank)) {
            throw invalid(source, "modelTarget.%s must contain non-blank values.".formatted(fieldName));
        }
        Set<String> unique = new LinkedHashSet<>(names);
        if (unique.size() != names.size()) {
            throw invalid(source, "modelTarget.%s contains duplicated values.".formatted(fieldName));
        }
        List<String> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return List.copyOf(normalized);
    }

    private ValueSnapshots valueSnapshots(
        List<Map<String, Object>> values,
        List<String> slotAxes,
        PayloadSource source
    ) {
        if (values == null || values.isEmpty()) {
            throw invalid(source, "parameter values must not be empty.");
        }

        Set<String> expectedKeys = new LinkedHashSet<>(slotAxes);
        expectedKeys.addAll(VALUE_META_FIELDS);
        Map<SlotIdentity, CanonicalNumeric> basecaseValues = new LinkedHashMap<>();
        Map<SlotIdentity, BigDecimal> proposedValues = new LinkedHashMap<>();

        for (Map<String, Object> value : values) {
            if (value == null || !value.keySet().equals(expectedKeys)) {
                throw invalid(source, "value keys must match slot axes plus basecaseValue/proposedValue/rationale.");
            }
            if (!(value.get("rationale") instanceof String rationale) || rationale.isBlank()) {
                throw invalid(source, "value rationale is required.");
            }

            List<CanonicalScalar> slots = slotAxes.stream()
                .map(axis -> canonicalSlot(value.get(axis), source, axis))
                .toList();
            SlotIdentity slotIdentity = new SlotIdentity(slots);
            CanonicalNumeric basecaseValue = nullableNumericValue(
                value.get("basecaseValue"),
                source,
                "basecaseValue"
            );
            BigDecimal proposedValue = numericValue(value.get("proposedValue"), source, "proposedValue");
            if (basecaseValues.putIfAbsent(slotIdentity, basecaseValue) != null) {
                throw invalid(source, "parameter values contain a duplicated slot identity.");
            }
            proposedValues.put(slotIdentity, proposedValue);
        }

        return new ValueSnapshots(
            new ParameterStructure(Map.copyOf(basecaseValues)),
            new ParameterExecutionValues(Map.copyOf(proposedValues))
        );
    }

    private CanonicalScalar canonicalSlot(Object value, PayloadSource source, String axis) {
        if (value instanceof Number number) {
            return new CanonicalScalar("N", decimal(number, source, axis).toPlainString());
        }
        if (value instanceof String text) {
            return new CanonicalScalar("S", text);
        }
        if (value instanceof Boolean bool) {
            return new CanonicalScalar("B", bool.toString());
        }
        throw invalid(source, "slot axis %s must be a scalar value.".formatted(axis));
    }

    private BigDecimal numericValue(Object value, PayloadSource source, String fieldName) {
        if (!(value instanceof Number number)) {
            throw invalid(source, "%s must be numeric.".formatted(fieldName));
        }
        return decimal(number, source, fieldName);
    }

    private CanonicalNumeric nullableNumericValue(Object value, PayloadSource source, String fieldName) {
        if (value == null) {
            return new CanonicalNumeric(null);
        }
        return new CanonicalNumeric(numericValue(value, source, fieldName));
    }

    private BigDecimal decimal(Number number, PayloadSource source, String fieldName) {
        try {
            return new BigDecimal(number.toString()).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw invalid(source, "%s must be a finite numeric value.".formatted(fieldName));
        }
    }

    private AppErrorException invalid(PayloadSource source, String message) {
        return source == PayloadSource.REQUEST ? invalidRequest(message) : invalidStoredPayload(message);
    }

    private AppErrorException invalidRequest(String message) {
        return AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, message);
    }

    private AppErrorException invalidStoredPayload(String message) {
        return AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum PayloadSource {
        REQUEST,
        STORED
    }

    private record CanonicalPayload(
        Map<ParameterIdentity, ParameterStructure> structures,
        Map<ParameterIdentity, ParameterExecutionValues> executionValues
    ) {
    }

    private record ParameterIdentity(String parameterName, TargetIdentity target) {
    }

    private record TargetIdentity(
        String modelFile,
        String modelSheet,
        String tableName,
        List<String> valueFields,
        List<String> slotAxes
    ) {
    }

    private record ParameterStructure(Map<SlotIdentity, CanonicalNumeric> basecaseValues) {
    }

    private record ParameterExecutionValues(Map<SlotIdentity, BigDecimal> proposedValues) {
    }

    private record SlotIdentity(List<CanonicalScalar> values) {
    }

    private record CanonicalScalar(String type, String value) {
    }

    private record CanonicalNumeric(BigDecimal value) {
    }

    private record ValueSnapshots(
        ParameterStructure structure,
        ParameterExecutionValues executionValues
    ) {
    }
}
