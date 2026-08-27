package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Agent presentation 조회 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioPresentationAgentRes(
    Long scenarioGroupId,
    Operation operation,
    Sections sections
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Operation(
        String operationId,
        String status,
        String currentPhase,
        Integer snapshotRevision
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sections(
        MarkdownSection eventAnalysis,
        ScenarioDesign scenarioDesign,
        MarkdownSection roleSelectionRationale
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarkdownSection(
        String status,
        String markdown
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioDesign(
        String status,
        AgentDimensionDefinitions dimensionDefinitions,
        ScenarioTable scenarioTable
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioTable(
        String status,
        List<Header> headers,
        List<Row> rows
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
        String key,
        String label,
        Integer displayOrder
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(
        Long scenarioId,
        Integer displayOrder,
        String title,
        String narrative,
        String scenarioRole,
        String baselineYn,
        String pressureBand,
        BigDecimal probabilityScore,
        String pressureRationale,
        Map<String, String> cells,
        Map<String, CellEvidence> cellEvidence
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CellEvidence(
        String valueId,
        String valueLabel,
        String rationale
    ) {
    }

}
