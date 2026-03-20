package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.BlockDataSourceSpec;
import com.sk.domain.mi.dto.internal.BlockExecutionClassification;
import com.sk.domain.mi.dto.internal.BlockExecutionPlan;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockExecutionClassifier {

    private static final Set<String> TOP_LEVEL_KEYS = Set.of("locator", "query", "assign", "transform", "overridePolicy");
    private static final Set<String> LOCATOR_KEYS = Set.of("kind", "refId", "refCode");
    private static final Set<String> ASSIGN_KEYS = Set.of("fields");
    private static final Set<String> ASSIGN_FIELD_KEYS = Set.of("fieldKey", "sourcePath", "required");
    private static final Set<String> OVERRIDE_POLICY_KEYS = Set.of("issueState");
    private static final Set<String> ISSUE_STATE_OVERRIDE_KEYS = Set.of("query", "state", "contentInput");
    private static final Set<String> TRANSFORM_TYPES = Set.of("SPLIT_LINES", "FORMAT_DATE", "TO_EPOCH_MILLIS", "LIMIT_ITEMS", "YN_TO_BOOLEAN");
    private static final Set<String> ALLOWED_STATE_KEYS = Set.of("plotOnChart", "removed");
    private static final Set<String> COMMON_STATE_KEYS = Set.of("contentInput");
    private static final Pattern SOURCE_PATH_PATTERN = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+|\\.\\*)*");

    private final ObjectMapper objectMapper;

    public BlockExecutionPlan plan(TemplateTree.BlockNode block, IssueStateTree.BlockStateNode blockState) {
        String specJson = StringUtils.trimToNull(block.dataSourceSpecJson());
        if (specJson == null) {
            return new BlockExecutionPlan(
                BlockExecutionClassification.LEGACY_COMPAT,
                null,
                validateLegacy(block)
            );
        }

        List<String> errors = new ArrayList<>();
        BlockDataSourceSpec spec = parseSpec(specJson, errors);
        if (spec != null) {
            validateCanonical(block, blockState, spec, errors);
        }

        BlockExecutionClassification classification = errors.isEmpty()
            ? BlockExecutionClassification.CANONICAL
            : BlockExecutionClassification.INVALID_MIXED;

        return new BlockExecutionPlan(classification, spec, List.copyOf(errors));
    }

    public void validateOrThrow(TemplateTree.BlockNode block, BlockExecutionPlan plan) {
        if (!plan.validationErrors().isEmpty()) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                block.blockCode() + ":" + String.join(",", plan.validationErrors())
            );
        }
    }

    public void validateResolvedContent(TemplateTree.BlockNode block, BlockExecutionPlan plan, Map<String, Object> resolvedContent) {
        if (!plan.isCanonical() || plan.spec() == null || plan.spec().assign() == null || plan.spec().assign().fields() == null) {
            return;
        }

        for (BlockDataSourceSpec.AssignField field : plan.spec().assign().fields()) {
            if (field == null || StringUtils.isBlank(field.fieldKey())) {
                continue;
            }
            if (Boolean.TRUE.equals(field.required()) && !resolvedContent.containsKey(field.fieldKey())) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "CANONICAL_ASSIGNMENT_MISSING:" + block.blockCode());
            }
        }
    }

    public void validateResolvedContent(TemplateTree.BlockNode block, BlockExecutionPlan plan, ResolvedBlockData resolvedData) {
        if (resolvedData == null || resolvedData.expandedContents() == null || resolvedData.expandedContents().isEmpty()) {
            validateResolvedContent(block, plan, resolvedData == null ? Map.of() : resolvedData.toContentMap());
            return;
        }

        for (Map<String, Object> item : resolvedData.expandedContents()) {
            validateResolvedContent(block, plan, item == null ? Map.of() : item);
        }
    }

    private BlockDataSourceSpec parseSpec(String specJson, List<String> errors) {
        try {
            JsonNode root = objectMapper.readTree(specJson);
            if (!root.isObject()) {
                errors.add("INVALID_DATA_SRC_SPEC_JSON");
                return null;
            }

            Set<String> keys = new LinkedHashSet<>();
            root.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(TOP_LEVEL_KEYS)) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }

            return objectMapper.convertValue(root, BlockDataSourceSpec.class);
        } catch (Exception e) {
            errors.add("INVALID_DATA_SRC_SPEC_JSON");
            return null;
        }
    }

    private List<String> validateLegacy(TemplateTree.BlockNode block) {
        List<String> errors = new ArrayList<>();
        if (StringUtils.isBlank(block.dataSourceType())) {
            errors.add("LEGACY_REFERENCE_INVALID");
            return errors;
        }

        String ref = StringUtils.trimToEmpty(block.dataSourceRef());
        switch (block.dataSourceType()) {
            case "STATIC", "NEWS_EVENT" -> {
            }
            case "DYNAMIC_QUERY" -> {
                if (!ref.startsWith("pref_table:")) {
                    errors.add("LEGACY_REFERENCE_INVALID");
                }
            }
            case "CHART_SERIES" -> {
                if (StringUtils.isBlank(ref)) {
                    return errors;
                }
                if (!(ref.startsWith("dedi:") || ref.startsWith("cltn:") || !ref.contains(":"))) {
                    errors.add("LEGACY_REFERENCE_INVALID");
                }
            }
            default -> errors.add("LEGACY_REFERENCE_INVALID");
        }
        return errors;
    }

    private void validateCanonical(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        BlockDataSourceSpec spec,
        List<String> errors
    ) {
        if (StringUtils.isBlank(block.dataSourceType()) || spec.locator() == null || StringUtils.isBlank(spec.locator().kind())) {
            errors.add("DATA_SRC_TYPE_LOCATOR_MISMATCH");
            return;
        }

        validateLocator(block, spec, errors);
        validateQuery(block, blockState, spec, errors);
        validateAssign(block, spec, errors);
        validateOverridePolicy(block, blockState, spec, errors);
        validateTransform(spec, errors);
        validateConflicts(block, blockState, spec, errors);
        validateCanonicalShape(block, spec, errors);
    }

    private void validateLocator(TemplateTree.BlockNode block, BlockDataSourceSpec spec, List<String> errors) {
        Map<String, Object> rawLocator = objectMapper.convertValue(spec.locator(), new TypeReference<>() {
        });
        if (!rawLocator.keySet().stream().allMatch(LOCATOR_KEYS::contains)) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        String locatorKind = spec.locator().kind();
        boolean valid = switch (block.dataSourceType()) {
            case "STATIC" -> Set.of("INLINE", "PUBLISH_CONTEXT").contains(locatorKind);
            case "DYNAMIC_QUERY" -> "PREFERENCE_TABLE".equals(locatorKind);
            case "CHART_SERIES" -> Set.of("MAIN_CHART", "DEDICATED_CHART", "COLLECTION_CHART").contains(locatorKind);
            case "NEWS_EVENT" -> "NEWS_EVENT".equals(locatorKind);
            default -> false;
        };
        if (!valid) {
            errors.add("DATA_SRC_TYPE_LOCATOR_MISMATCH");
        }
    }

    private void validateQuery(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        BlockDataSourceSpec spec,
        List<String> errors
    ) {
        Map<String, Object> templateQuery = spec.query() == null ? Map.of() : spec.query();
        if (templateQuery.values().stream().anyMatch(value -> value instanceof Map<?, ?> || value instanceof List<?>)) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        Set<String> allowedQueryKeys = allowedQueryKeys(block.dataSourceType(), spec.locator().kind());
        if (!allowedQueryKeys.containsAll(templateQuery.keySet())) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        Map<String, Object> effectiveQuery = new java.util.LinkedHashMap<>(templateQuery);
        if (blockState != null && blockState.queryParams() != null) {
            blockState.queryParams().forEach((key, value) -> {
                if (allowedQueryKeys.contains(key)) {
                    effectiveQuery.put(key, value);
                }
            });
        }

        // fromDate/toDate는 필수 체크를 하지 않는다.
        // 템플릿 spec에 없어도 blockState.queryParams()를 통해 이슈 저장 시점에 주입될 수 있으므로,
        // 값이 존재할 때만 형식과 범위가 유효한지 검증한다.
        if (effectiveQuery.containsKey("fromDate") && !isValidDate(effectiveQuery.get("fromDate"))) {
            errors.add("INVALID_QUERY_DATE_RANGE");
        }
        if (effectiveQuery.containsKey("toDate") && !isValidDate(effectiveQuery.get("toDate"))) {
            errors.add("INVALID_QUERY_DATE_RANGE");
        }
        if (effectiveQuery.containsKey("fromDate") && effectiveQuery.containsKey("toDate")
            && isValidDate(effectiveQuery.get("fromDate")) && isValidDate(effectiveQuery.get("toDate"))) {
            LocalDate fromDate = LocalDate.parse(String.valueOf(effectiveQuery.get("fromDate")));
            LocalDate toDate = LocalDate.parse(String.valueOf(effectiveQuery.get("toDate")));
            if (fromDate.isAfter(toDate)) {
                errors.add("INVALID_QUERY_DATE_RANGE");
            }
        }
        if (Set.of("MAIN_CHART", "DEDICATED_CHART", "PREFERENCE_TABLE", "INLINE", "PUBLISH_CONTEXT").contains(spec.locator().kind())
            && !effectiveQuery.isEmpty()) {
            errors.add("QUERY_REQUIREDNESS_VIOLATION");
        }
    }

    private void validateAssign(TemplateTree.BlockNode block, BlockDataSourceSpec spec, List<String> errors) {
        if (spec.assign() == null || spec.assign().fields() == null || spec.assign().fields().isEmpty()) {
            errors.add("CANONICAL_ASSIGNMENT_MISSING");
            return;
        }

        Map<String, Object> rawAssign = objectMapper.convertValue(spec.assign(), new TypeReference<>() {
        });
        if (rawAssign != null && !rawAssign.keySet().stream().allMatch(ASSIGN_KEYS::contains)) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        Set<String> fieldKeys = block.fields().stream().map(TemplateTree.FieldNode::fieldKey).collect(java.util.stream.Collectors.toSet());
        Set<String> assignFieldKeys = new LinkedHashSet<>();

        for (BlockDataSourceSpec.AssignField field : spec.assign().fields()) {
            if (field == null) {
                errors.add("CANONICAL_ASSIGNMENT_MISSING");
                continue;
            }
            Map<String, Object> rawField = objectMapper.convertValue(field, new TypeReference<>() {
            });
            if (!rawField.keySet().stream().allMatch(ASSIGN_FIELD_KEYS::contains)) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }
            if (StringUtils.isBlank(field.fieldKey()) || StringUtils.isBlank(field.sourcePath()) || !SOURCE_PATH_PATTERN.matcher(field.sourcePath()).matches()) {
                errors.add("CANONICAL_ASSIGNMENT_MISSING");
            }
            if (!assignFieldKeys.add(field.fieldKey()) || !fieldKeys.contains(field.fieldKey())) {
                errors.add("ASSIGN_FIELD_NOT_DEFINED");
            }
        }
    }

    private void validateOverridePolicy(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        BlockDataSourceSpec spec,
        List<String> errors
    ) {
        BlockDataSourceSpec.IssueStateOverride issueStateOverride = spec.overridePolicy() == null
            ? null : spec.overridePolicy().issueState();
        if (issueStateOverride == null) {
            errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
            return;
        }

        Map<String, Object> rawOverridePolicy = objectMapper.convertValue(spec.overridePolicy(), new TypeReference<>() {
        });
        if (rawOverridePolicy != null && !rawOverridePolicy.keySet().stream().allMatch(OVERRIDE_POLICY_KEYS::contains)) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        Map<String, Object> rawIssueState = objectMapper.convertValue(issueStateOverride, new TypeReference<>() {
        });
        if (rawIssueState != null && !rawIssueState.keySet().stream().allMatch(ISSUE_STATE_OVERRIDE_KEYS::contains)) {
            errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
        }

        Set<String> editableFields = block.fields().stream()
            .filter(field -> "Y".equals(field.isEditableYn()))
            .map(TemplateTree.FieldNode::fieldKey)
            .collect(java.util.stream.Collectors.toSet());
        Set<String> allowedQueryKeys = allowedQueryKeys(block.dataSourceType(), spec.locator().kind());

        if (issueStateOverride.query() != null && !allowedQueryKeys.containsAll(issueStateOverride.query())) {
            errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
        }
        if (issueStateOverride.state() != null && !ALLOWED_STATE_KEYS.containsAll(issueStateOverride.state())) {
            errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
        }
        if (issueStateOverride.contentInput() != null) {
            Set<String> allowedContentInput = new LinkedHashSet<>(editableFields);
            block.fields().stream()
                .map(TemplateTree.FieldNode::fieldKey)
                .filter(fieldKey -> issueStateOverride.contentInput().contains(fieldKey))
                .forEach(allowedContentInput::add);

            for (String contentInput : issueStateOverride.contentInput()) {
                String rootKey = StringUtils.substringBefore(contentInput, ".");
                if (!editableFields.contains(rootKey)) {
                    errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
                }
            }
        }

        if (blockState == null || blockState.state().isEmpty()) {
            return;
        }

        Set<String> seenIssueInputs = new LinkedHashSet<>();
        blockState.state().forEach((key, value) -> {
            if (COMMON_STATE_KEYS.contains(key)) {
                return;
            }
            if (ALLOWED_STATE_KEYS.contains(key)) {
                seenIssueInputs.add("state:" + key);
                return;
            }
            if (allowedQueryKeys.contains(key)) {
                seenIssueInputs.add("query:" + key);
                return;
            }
            if (editableFields.contains(key)) {
                seenIssueInputs.add("content:" + key);
                return;
            }
            errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
        });

        Object contentInput = blockState.state().get("contentInput");
        if (contentInput instanceof Map<?, ?> contentMap && issueStateOverride.contentInput() != null) {
            for (Object key : contentMap.keySet()) {
                String keyString = String.valueOf(key);
                if (!issueStateOverride.contentInput().contains(keyString)) {
                    errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
                }
            }
        }
    }

    private void validateTransform(BlockDataSourceSpec spec, List<String> errors) {
        if (spec.transform() == null) {
            return;
        }
        for (BlockDataSourceSpec.TransformRule rule : spec.transform()) {
            if (rule == null || !TRANSFORM_TYPES.contains(rule.type()) || StringUtils.isBlank(rule.sourcePath())
                || !SOURCE_PATH_PATTERN.matcher(rule.sourcePath()).matches()) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }
            if ("FORMAT_DATE".equals(rule.type()) && !Set.of("YYYY-MM-DD", "YYYY년 MM월 DD일").contains(rule.format())) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }
            if ("TO_EPOCH_MILLIS".equals(rule.type()) && StringUtils.isNotBlank(rule.timezone()) && !"UTC".equals(rule.timezone())) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }
            if ("LIMIT_ITEMS".equals(rule.type()) && (rule.maxItems() == null || rule.maxItems() < 1 || rule.maxItems() > 100)) {
                errors.add("UNSUPPORTED_DATA_SRC_SPEC_KEY");
            }
        }
    }

    private void validateConflicts(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        BlockDataSourceSpec spec,
        List<String> errors
    ) {
        String expectedLegacyRef = translateLegacyRef(block, spec);
        String actualLegacyRef = StringUtils.trimToNull(block.dataSourceRef());
        if (expectedLegacyRef == null) {
            if (actualLegacyRef != null && !"MAIN_CHART".equals(spec.locator().kind())) {
                errors.add("CANONICAL_LEGACY_CONFLICT");
            }
        } else if (actualLegacyRef != null && !Objects.equals(expectedLegacyRef, actualLegacyRef)) {
            errors.add("CANONICAL_LEGACY_CONFLICT");
        }

        for (TemplateTree.FieldNode field : block.fields()) {
            if (StringUtils.isBlank(field.colMapping())) {
                continue;
            }
            BlockDataSourceSpec.AssignField assignField = spec.assign().fields().stream()
                .filter(assign -> field.fieldKey().equals(assign.fieldKey()))
                .findFirst()
                .orElse(null);
            if (assignField == null || !Objects.equals(field.colMapping(), assignField.sourcePath())) {
                errors.add("CANONICAL_LEGACY_CONFLICT");
                break;
            }
        }

        if (blockState != null && blockState.state().containsKey("plotOnChart")
            && (spec.overridePolicy() == null || spec.overridePolicy().issueState() == null
            || spec.overridePolicy().issueState().state() == null
            || !spec.overridePolicy().issueState().state().contains("plotOnChart"))) {
            errors.add("ISSUE_STATE_OVERRIDE_NOT_ALLOWED");
        }
    }

    private void validateCanonicalShape(TemplateTree.BlockNode block, BlockDataSourceSpec spec, List<String> errors) {
        List<String> assignFieldKeys = spec.assign().fields().stream().map(BlockDataSourceSpec.AssignField::fieldKey).toList();

        if ("NEWS_FLOW_CHART".equals(block.blockCode())
            && !assignFieldKeys.equals(List.of("title", "aggregationTime", "timestamps", "series"))) {
            errors.add("ASSIGN_FIELD_NOT_DEFINED");
        }
        if ("NEWS_ITEM".equals(block.blockCode())
            && !assignFieldKeys.equals(List.of(
                "newsId",
                "title",
                "content",
                "timestamp",
                "createdDate",
                "createTime",
                "source",
                "originalUrl",
                "showInChart",
                "trend"
            ))) {
            errors.add("ASSIGN_FIELD_NOT_DEFINED");
        }
        if ("REFERENCE_TABLE".equals(block.blockCode())
            && !assignFieldKeys.equals(List.of("tableData"))) {
            errors.add("ASSIGN_FIELD_NOT_DEFINED");
        }
    }

    private Set<String> allowedQueryKeys(String dataSourceType, String locatorKind) {
        return switch (dataSourceType) {
            case "STATIC", "DYNAMIC_QUERY" -> Set.of();
            case "CHART_SERIES" -> "COLLECTION_CHART".equals(locatorKind)
                ? Set.of("fromDate", "toDate")
                : Set.of();
            case "NEWS_EVENT" -> Set.of("fromDate", "toDate");
            default -> Set.of();
        };
    }

    private String translateLegacyRef(TemplateTree.BlockNode block, BlockDataSourceSpec spec) {
        if (spec.locator() == null) {
            return null;
        }
        return switch (block.dataSourceType()) {
            case "DYNAMIC_QUERY" -> spec.locator().refId() == null ? null : "pref_table:" + spec.locator().refId();
            case "CHART_SERIES" -> switch (spec.locator().kind()) {
                case "DEDICATED_CHART" -> StringUtils.isBlank(spec.locator().refCode()) ? null : "dedi:" + spec.locator().refCode();
                case "COLLECTION_CHART" -> StringUtils.isBlank(spec.locator().refCode()) ? null : "cltn:" + spec.locator().refCode();
                case "MAIN_CHART" -> StringUtils.defaultIfBlank(spec.locator().refCode(), block.blockCode());
                default -> null;
            };
            default -> null;
        };
    }

    private boolean isValidDate(Object value) {
        if (value == null) {
            return false;
        }
        try {
            LocalDate.parse(String.valueOf(value));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
