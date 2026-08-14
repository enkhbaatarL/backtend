package com.sk.domain.mi.service.nextgen.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignScenarioRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignBaseEvent;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignDimensionValueOption;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignEvidence;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignOperationSummary;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.GlobalPressureBand;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent presentation 응답 → B.E 설계 화면 계약({@link ScenarioDesignPresentationRes}) 변환기.
 *
 * Agent의 sections 트리를 화면 단위로 펴고, 시나리오 표는 기존 설계 표 계약(DesignTable)으로 변환한다.
 * 셀 값은 cells의 표시 label을 정본으로 쓰고(왕복 재전송 안전), cellEvidence에서 valueId와 셀 근거를 보강한다.
 * 확률은 legacy 여부로 스케일을 분기한다 — v4는 0~1 비율이라 100을 곱하고, v1 legacy는 0~100 정수 그대로.
 *
 * Agent 계약에 없는 필드(신규 이벤트 종류, 부가 메타)는 무시하도록 파싱 레코드에 ignoreUnknown을 걸어
 * Agent 측 확장에 내성을 가진다.
 */
public final class ScenarioDesignPresentationMapper {

    private ScenarioDesignPresentationMapper() {
    }

    /**
     * Agent 응답 원문과 B.E 합성 재료를 합쳐 응답을 만든다.
     * lastError는 operation 테이블, designSession은 그룹 엔티티 + Redis 검토 캐시,
     * overlayTable은 검토 통과 편집본(캐시 표의 DesignTable 변환)으로 호출측이 조회해 넘긴다.
     * overlayTable이 있으면 Agent 표 대신 그것을 내리되, 셀의 valueId와 근거는
     * dimensionValueOptions에서 label 역조인으로 복원한다 (검토 왕복은 label만 오가므로 캐시에는 없다).
     */
    public static ScenarioDesignPresentationRes toRes(JsonNode agentBody,
                                                      List<ScenarioGroupBaseEventRow> baseEventRows,
                                                      JsonNode lastError,
                                                      ScenarioDesignPresentationRes.DesignSessionState designSession,
                                                      ScenarioDesignPresentationRes.DesignGroupMeta groupMeta,
                                                      DesignTable overlayTable,
                                                      Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        AgentPresentation source = JsonUtil.fromJson(agentBody.toString(), AgentPresentation.class);
        boolean legacy = Boolean.TRUE.equals(source.legacy());
        AgentSections sections = source.sections();

        Map<String, List<DesignDimensionValueOption>> valueOptions = toDimensionValueOptions(sections);
        DesignTable table = overlayTable == null
            ? toTable(sections, legacy, signpostsByScenario)
            : enrichOverlayTable(overlayTable, valueOptions, signpostsByScenario);

        return ScenarioDesignPresentationRes.of(
            source.scenarioGroupId(),
            source.engineVersion(),
            legacy,
            source.warnings() == null ? List.of() : source.warnings(),
            toOperationSummary(source.operation(), lastError),
            designSession,
            groupMeta,
            toBaseEvents(baseEventRows),
            toEvidence(sections),
            table,
            valueOptions
        );
    }

    /**
     * 검토 편집본 표의 셀에 valueId/근거/압력 기여를 복원한다.
     * 편집본 셀에는 표시 label만 있으므로, 같은 분기항목의 옵션 중 label이 일치하는 것을 조인한다.
     * 자유입력 등 옵션에 없는 값은 복원 없이 label만 유지된다 (근거 자연 소거 — 기존 정책과 동일).
     */
    private static DesignTable enrichOverlayTable(DesignTable overlay, Map<String, List<DesignDimensionValueOption>> valueOptions,
                                                  Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        if (overlay.scenarios() == null || overlay.scenarios().isEmpty()) {
            return overlay;
        }
        List<DesignScenarioRow> rows = overlay.scenarios().stream()
            .map(row -> new DesignScenarioRow(
                row.scenarioId(), row.isNew(), row.displayOrder(), row.scenarioName(), row.baseCase(),
                row.scenarioRole(), row.scenarioRoleLabel(), row.pressureBand(), row.pressureBandLabel(), row.pressureRationale(),
                enrichDimensions(row.dimensions(), valueOptions),
                row.probability(), row.narrative(),
                resolveSignposts(row.scenarioId(), signpostsByScenario, row.signposts())))
            .toList();
        return DesignTable.of(overlay.dimensionColumns(), rows);
    }

    /** 행별 Signpost 결정 — 합성 맵에 있으면 그것(DB 산출), 없으면 기존 값 유지. */
    private static List<ScenarioSignpostDefinitionViewRes> resolveSignposts(Long scenarioId,
                                                                            Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario,
                                                                            List<ScenarioSignpostDefinitionViewRes> fallback) {
        if (scenarioId == null || signpostsByScenario == null || signpostsByScenario.isEmpty()) {
            return fallback;
        }
        return signpostsByScenario.getOrDefault(scenarioId, List.of());
    }

    private static List<ScenarioDimensionRes> enrichDimensions(List<ScenarioDimensionRes> dimensions,
                                                               Map<String, List<DesignDimensionValueOption>> valueOptions) {
        if (dimensions == null) {
            return null;
        }
        return dimensions.stream()
            .map(dimension -> {
                if (dimension.valueId() != null || dimension.value() == null) {
                    return dimension;
                }
                DesignDimensionValueOption matched = valueOptions.getOrDefault(dimension.key(), List.of()).stream()
                    .filter(option -> dimension.value().equals(option.label()))
                    .findFirst()
                    .orElse(null);
                if (matched == null) {
                    return dimension;
                }
                return new ScenarioDimensionRes(
                    dimension.key(), dimension.label(), dimension.value(), dimension.valueLabel(),
                    matched.valueId(), matched.rationale(), matched.pressureContribution());
            })
            .toList();
    }

    private static DesignOperationSummary toOperationSummary(AgentOperation operation, JsonNode lastError) {
        if (operation == null) {
            return null;
        }
        return new DesignOperationSummary(
            operation.operationId(),
            operation.status(),
            DesignOperationStatus.labelOf(operation.status()),
            operation.currentPhase(),
            operation.latestSequence(),
            operation.snapshotRevision(),
            lastError
        );
    }

    private static List<DesignBaseEvent> toBaseEvents(List<ScenarioGroupBaseEventRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
            .map(row -> new DesignBaseEvent(
                row.eventId(),
                row.eventName(),
                row.eventTypeLv1Code(),
                row.eventTypeLv1Label(),
                row.isScenarioSuggested(),
                row.eventUpdatedAt()))
            .toList();
    }

    private static DesignEvidence toEvidence(AgentSections sections) {
        AgentMarkdownSection eventAnalysis = sections == null ? null : sections.eventAnalysis();
        AgentMarkdownSection roleSelection = sections == null ? null : sections.roleSelectionRationale();
        return new DesignEvidence(
            eventAnalysis == null ? null : eventAnalysis.status(),
            eventAnalysis == null ? null : eventAnalysis.markdown(),
            roleSelection == null ? null : roleSelection.status(),
            roleSelection == null ? null : roleSelection.markdown()
        );
    }

    /**
     * scenarioTable + dimensionDefinitions → 설계 표. 헤더가 아직 없으면(생성 초기 스냅샷) null을 반환해
     * FE가 표 영역을 로딩 상태로 두게 한다. 헤더의 설명/근거는 definitions에서 dimensionId로 조인한다.
     */
    private static DesignTable toTable(AgentSections sections, boolean legacy,
                                       Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        AgentScenarioTable scenarioTable = sections == null || sections.scenarioDesign() == null
            ? null
            : sections.scenarioDesign().scenarioTable();
        if (scenarioTable == null || scenarioTable.headers() == null || scenarioTable.headers().isEmpty()) {
            return null;
        }

        Map<String, AgentDimensionItem> itemsById = new LinkedHashMap<>();
        for (AgentDimensionItem item : definitionItems(sections)) {
            itemsById.putIfAbsent(item.dimensionId(), item);
        }

        List<AgentHeader> headers = scenarioTable.headers().stream()
            .sorted(Comparator.comparing(h -> h.displayOrder() == null ? Integer.MAX_VALUE : h.displayOrder()))
            .toList();

        List<ScenarioDimensionColumnRes> columns = headers.stream()
            .map(header -> {
                AgentDimensionItem item = itemsById.get(header.key());
                return new ScenarioDimensionColumnRes(
                    header.key(),
                    header.label(),
                    item == null ? null : item.description(),
                    item == null ? null : item.rationale());
            })
            .toList();

        List<DesignScenarioRow> rows = (scenarioTable.rows() == null ? List.<AgentRow>of() : scenarioTable.rows()).stream()
            .map(row -> toDesignRow(row, headers, legacy, signpostsByScenario))
            .toList();

        return DesignTable.of(columns, rows);
    }

    private static DesignScenarioRow toDesignRow(AgentRow row, List<AgentHeader> headers, boolean legacy,
                                                 Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        Map<String, String> cells = row.cells() == null ? Map.of() : row.cells();
        Map<String, AgentCellEvidence> cellEvidence = row.cellEvidence() == null ? Map.of() : row.cellEvidence();

        List<ScenarioDimensionRes> dimensions = headers.stream()
            .map(header -> {
                AgentCellEvidence evidence = cellEvidence.get(header.key());
                String value = cells.get(header.key());
                if (value == null && evidence != null) {
                    value = evidence.valueLabel();
                }
                return new ScenarioDimensionRes(
                    header.key(),
                    header.label(),
                    value,
                    value,
                    evidence == null ? null : evidence.valueId(),
                    evidence == null ? null : evidence.rationale(),
                    evidence == null ? null : evidence.pressureContribution());
            })
            .toList();

        return DesignScenarioRow.of(
            row.scenarioId(),
            row.displayOrder(),
            row.title(),
            "Y".equalsIgnoreCase(row.baselineYn()),
            ScenarioRole.fromNullable(row.scenarioRole()),
            GlobalPressureBand.fromNullable(row.pressureBand()),
            row.pressureRationale(),
            dimensions,
            ScenarioProbabilityAllocator.toDisplayPercent(row.probabilityScore(), !legacy),
            row.narrative(),
            resolveSignposts(row.scenarioId(), signpostsByScenario, null)
        );
    }

    private static Map<String, List<DesignDimensionValueOption>> toDimensionValueOptions(AgentSections sections) {
        Map<String, List<DesignDimensionValueOption>> options = new LinkedHashMap<>();
        for (AgentDimensionItem item : definitionItems(sections)) {
            List<DesignDimensionValueOption> values = (item.values() == null ? List.<AgentDimensionValue>of() : item.values()).stream()
                .map(value -> new DesignDimensionValueOption(
                    value.valueId(),
                    value.label(),
                    value.rationale(),
                    value.pressureContribution()))
                .toList();
            options.putIfAbsent(item.dimensionId(), values);
        }
        return options;
    }

    private static List<AgentDimensionItem> definitionItems(AgentSections sections) {
        if (sections == null || sections.scenarioDesign() == null
            || sections.scenarioDesign().dimensionDefinitions() == null
            || sections.scenarioDesign().dimensionDefinitions().items() == null) {
            return List.of();
        }
        return sections.scenarioDesign().dimensionDefinitions().items();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentPresentation(Long scenarioGroupId, String engineVersion, Boolean legacy,
                                     List<String> warnings, AgentOperation operation, AgentSections sections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentOperation(String operationId, String status, String currentPhase,
                                  Integer latestSequence, Integer snapshotRevision) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentSections(AgentMarkdownSection eventAnalysis, AgentScenarioDesign scenarioDesign,
                                 AgentMarkdownSection roleSelectionRationale) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentMarkdownSection(String status, String markdown) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentScenarioDesign(String status, AgentDimensionDefinitions dimensionDefinitions,
                                       AgentScenarioTable scenarioTable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentDimensionDefinitions(String status, List<AgentDimensionItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentDimensionItem(String dimensionId, String label, String description, String rationale,
                                      List<AgentDimensionValue> values) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentDimensionValue(String valueId, String label, String rationale, String pressureContribution) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentScenarioTable(String status, List<AgentHeader> headers, List<AgentRow> rows) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentHeader(String key, String label, Integer displayOrder) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentRow(Long scenarioId, Integer displayOrder, String title, String narrative,
                            String scenarioRole, String baselineYn, String pressureBand,
                            BigDecimal probabilityScore, String pressureRationale,
                            Map<String, String> cells, Map<String, AgentCellEvidence> cellEvidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentCellEvidence(String valueId, String valueLabel, String rationale, String pressureContribution) {
    }

}
