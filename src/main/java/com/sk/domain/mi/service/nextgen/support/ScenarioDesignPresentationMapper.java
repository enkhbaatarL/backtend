package com.sk.domain.mi.service.nextgen.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignScenarioRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.ScenarioGroupCreationState;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignDimensionValueOption;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignEvidence;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.EventBundle;
import com.sk.domain.shared.enums.nextgen.DesignOperationPhase;
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
 * 확률은 전 세대 0~100 정수 단일 스케일이다 (Agent팀이 v1 정책으로 통일 확정 — 2026-08-18).
 *
 * Agent 계약에 없는 필드(신규 이벤트 종류, 부가 메타)는 무시하도록 파싱 레코드에 ignoreUnknown을 걸어
 * Agent 측 확장에 내성을 가진다.
 */
public final class ScenarioDesignPresentationMapper {

    private ScenarioDesignPresentationMapper() {
    }

    /**
     * Agent 응답 원문과 B.E 합성 재료를 합쳐 응답을 만든다.
     * lastError는 operation 테이블, refinementState는 그룹 엔티티 + Redis 검토 캐시,
     * overlayTable은 검토 통과 편집본(캐시 표의 DesignTable 변환)으로 호출측이 조회해 넘긴다.
     * overlayTable이 있으면 Agent 표 대신 그것을 내리되, 셀의 valueId와 근거는
     * dimensionValueOptions에서 label 역조인으로 복원한다 (검토 왕복은 label만 오가므로 캐시에는 없다).
     */
    public static ScenarioDesignPresentationRes toRes(JsonNode agentBody,
                                                      List<ScenarioGroupBaseEventRow> baseEventRows,
                                                      JsonNode lastError,
                                                      ScenarioDesignPresentationRes.ScenarioGroupRefinementState refinementState,
                                                      DesignTable overlayTable,
                                                      Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        AgentPresentation source = JsonUtil.fromJson(agentBody.toString(), AgentPresentation.class);
        AgentSections sections = source.sections();

        Map<String, List<DesignDimensionValueOption>> valueOptions = toDimensionValueOptions(sections);
        DesignTable table = overlayTable == null
            ? toTable(sections, signpostsByScenario)
            : enrichOverlayTable(overlayTable, sections, valueOptions, signpostsByScenario);

        return ScenarioDesignPresentationRes.of(
            source.scenarioGroupId(),
            toEventBundles(baseEventRows),
            toEvidence(sections),
            toCreationState(source.operation(), lastError),
            refinementState,
            table,
            valueOptions
        );
    }

    /**
     * 검토 편집본 표의 헤더와 셀에 근거를 복원한다.
     * 편집본에는 표시값(헤더 key/label, 셀 label)만 있으므로 definitions에서 조인한다 —
     * 헤더는 dimensionId로 description/rationale을, 셀은 같은 분기항목의 옵션 중 label이 일치하는 것에서
     * valueId/근거/압력 기여를 복원한다. 자유입력 등 옵션에 없는 값은 복원 없이 label만 유지된다
     * (근거 자연 소거 — 기존 정책과 동일).
     */
    private static DesignTable enrichOverlayTable(DesignTable overlay, AgentSections sections,
                                                  Map<String, List<DesignDimensionValueOption>> valueOptions,
                                                  Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        Map<String, AgentDimensionItem> itemsById = new LinkedHashMap<>();
        for (AgentDimensionItem item : definitionItems(sections)) {
            itemsById.putIfAbsent(item.dimensionId(), item);
        }
        List<ScenarioDimensionColumnRes> columns = overlay.dimensionColumns() == null
            ? null
            : overlay.dimensionColumns().stream()
                .map(column -> {
                    AgentDimensionItem item = itemsById.get(column.key());
                    if (item == null || column.description() != null || column.rationale() != null) {
                        return column;
                    }
                    return new ScenarioDimensionColumnRes(column.key(), column.label(), item.description(), item.rationale());
                })
                .toList();

        if (overlay.scenarios() == null || overlay.scenarios().isEmpty()) {
            return DesignTable.of(columns, overlay.scenarios());
        }
        List<DesignScenarioRow> rows = overlay.scenarios().stream()
            .map(row -> new DesignScenarioRow(
                row.scenarioId(), row.isNew(), row.displayOrder(), row.scenarioName(), row.baseCase(),
                row.scenarioRole(), row.scenarioRoleLabel(), row.pressureBand(), row.pressureBandLabel(), row.pressureRationale(),
                enrichDimensions(row.dimensions(), valueOptions),
                row.probability(), row.narrative(),
                resolveSignposts(row.scenarioId(), signpostsByScenario, row.signposts())))
            .toList();
        return DesignTable.of(columns, rows);
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
                    matched.valueId(), matched.rationale());
            })
            .toList();
    }

    private static ScenarioGroupCreationState toCreationState(AgentOperation operation, JsonNode lastError) {
        if (operation == null) {
            return null;
        }
        return ScenarioGroupCreationState.of(
            DesignOperationStatus.fromNullable(operation.status()),
            DesignOperationPhase.fromNullable(operation.currentPhase()),
            operation.snapshotRevision(),
            lastError
        );
    }

    /** 그룹 구성 이벤트 번들 조립 (설계 화면과 그룹 상세가 같은 계약을 공유한다). */
    public static List<EventBundle> toEventBundles(List<ScenarioGroupBaseEventRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
            .map(row -> new EventBundle(
                row.eventId(),
                row.eventName(),
                row.eventTypeLv1Code(),
                row.eventTypeLv1Label(),
                row.isScenarioSuggested(),
                DateUtil.utcToKst(row.latestRecordFirstCreated())))
            .toList();
    }

    /**
     * Agent presentation 응답에서 근거 마크다운 2종(Event 분석, 역할 선정 근거)만 추출한다 (그룹 상세 '근거 보기' 전용).
     * 근거 마크다운은 Agent 렌더러가 조회 시점에 생성하는 문서라 DB에 존재하지 않는다
     * (operation presentation_snapshot에는 렌더링 전 원재료만 저장됨을 전수 실측으로 확인, 2026-08-19).
     * 설계 화면과의 문서 동일성을 렌더러 버전 변경과 무관하게 보장하기 위해 조회 화면 DB 직조회 원칙의 예외로 Agent 원본을 사용한다.
     * 근거 섹션이 비어 있으면 null.
     */
    public static DesignEvidence toEvidence(JsonNode agentBody) {
        if (agentBody == null || agentBody.isNull()) {
            return null;
        }
        AgentPresentation source = JsonUtil.fromJson(agentBody.toString(), AgentPresentation.class);
        DesignEvidence evidence = toEvidence(source == null ? null : source.sections());
        return evidence.eventAnalysisMarkdown() == null && evidence.roleSelectionMarkdown() == null ? null : evidence;
    }

    private static DesignEvidence toEvidence(AgentSections sections) {
        return new DesignEvidence(
            normalizeMarkdown(sections == null ? null : sections.eventAnalysis()),
            normalizeMarkdown(sections == null ? null : sections.roleSelectionRationale())
        );
    }

    /** 준비 전 섹션 정규화 — Agent는 unavailable 섹션에 빈 문자열을 주므로 blank를 null로 통일한다 (계약: 준비 전이면 null). */
    private static String normalizeMarkdown(AgentMarkdownSection section) {
        if (section == null || section.markdown() == null || section.markdown().isBlank()) {
            return null;
        }
        return section.markdown();
    }

    /**
     * scenarioTable + dimensionDefinitions → 설계 표. 헤더가 아직 없으면(생성 초기 스냅샷) null을 반환해
     * FE가 표 영역을 로딩 상태로 두게 한다. 헤더의 설명/근거는 definitions에서 dimensionId로 조인한다.
     */
    private static DesignTable toTable(AgentSections sections,
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
            .map(row -> toDesignRow(row, headers, signpostsByScenario))
            .toList();

        return DesignTable.of(columns, rows);
    }

    private static DesignScenarioRow toDesignRow(AgentRow row, List<AgentHeader> headers,
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
                    evidence == null ? null : evidence.rationale());
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
            ScenarioProbabilityAllocator.toDisplayPercent(row.probabilityScore()),
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
                    value.rationale()))
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
    private record AgentPresentation(Long scenarioGroupId, AgentOperation operation, AgentSections sections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentOperation(String status, String currentPhase, Integer snapshotRevision) {
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
    private record AgentDimensionValue(String valueId, String label, String rationale) {
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
    private record AgentCellEvidence(String valueId, String valueLabel, String rationale) {
    }

}
