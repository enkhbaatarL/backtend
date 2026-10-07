package com.sk.domain.mi.service.nextgen.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.AdditionalDimensionsRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignScenarioRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.ScenarioGroupCreationState;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignDimensionValueOption;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignEvidence;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.EventBundle;
import com.sk.domain.shared.client.dto.response.AdditionalDimensionsAgentRes;
import com.sk.domain.shared.client.dto.response.AgentDimensionDefinitions;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.CellEvidence;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.Header;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.MarkdownSection;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.Row;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.ScenarioTable;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes.Sections;
import com.sk.domain.shared.enums.nextgen.AdditionalDimensionGenerationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationPhase;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.GlobalPressureBand;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
import com.sk.domain.shared.enums.nextgen.EventSourceType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 설계 Agent 응답 DTO → B.E 계약 변환기.
 * presentation(설계 화면·그룹 상세 근거 보기)과 추가 분기항목 후보 조회의 화면 계약 변환을 담당한다.
 * Agent 응답 계약 자체는 클라이언트 수신 DTO(client.dto.response의 ~AgentRes)가 정의한다.
 *
 * Agent의 sections 트리를 화면 단위로 펴고, 시나리오 표는 기존 설계 표 계약(DesignTable)으로 변환한다.
 * 셀 값은 cells의 표시 label을 정본으로 쓰고(왕복 재전송 안전), cellEvidence에서 valueId와 셀 근거를 보강한다.
 * 확률은 전 세대 0~100 정수 단일 스케일이다 (Agent팀이 v1 정책으로 통일 확정 — 2026-08-18).
 */
public final class ScenarioDesignAgentMapper {

    private ScenarioDesignAgentMapper() {
    }

    /**
     * Agent 응답과 B.E 합성 재료를 합쳐 응답을 만든다.
     * lastError는 operation 테이블, refinementState는 그룹 엔티티 + Redis 검토 캐시,
     * overlayTable은 검토 통과 편집본(캐시 표의 DesignTable 변환)으로 호출측이 조회해 넘긴다.
     * overlayTable이 있으면 Agent 표 대신 그것을 내리되, 셀의 valueId와 근거는
     * dimensionValueOptions에서 label 역조인으로 복원한다 (검토 왕복은 label만 오가므로 캐시에는 없다).
     */
    public static ScenarioDesignPresentationRes toRes(ScenarioPresentationAgentRes source,
                                                      List<ScenarioGroupBaseEventRow> baseEventRows,
                                                      JsonNode lastError,
                                                      ScenarioDesignPresentationRes.ScenarioGroupRefinementState refinementState,
                                                      DesignTable overlayTable,
                                                      Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        Sections sections = source.sections();

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
    private static DesignTable enrichOverlayTable(DesignTable overlay, Sections sections,
                                                  Map<String, List<DesignDimensionValueOption>> valueOptions,
                                                  Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {

        Map<String, AgentDimensionDefinitions.Item> itemsById = new LinkedHashMap<>();
        for (AgentDimensionDefinitions.Item item : definitionItems(sections)) {
            itemsById.putIfAbsent(item.dimensionId(), item);
        }

        Map<Long, Row> agentRowsByScenarioId = toAgentRowsByScenarioId(sections);

        List<ScenarioDimensionColumnRes> columns = overlay.dimensionColumns() == null
            ? null
            : overlay.dimensionColumns().stream()
                .map(column -> {
                    AgentDimensionDefinitions.Item item = itemsById.get(column.key());
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
            .map(row -> {
                Row agentRow = row.scenarioId() == null ? null : agentRowsByScenarioId.get(row.scenarioId());
                ScenarioRole role = row.scenarioRole() != null ? row.scenarioRole() : agentRow == null ? null : ScenarioRole.fromNullable(agentRow.scenarioRole());
                GlobalPressureBand pressureBand = row.pressureBand() != null ? row.pressureBand() : agentRow == null ? null : GlobalPressureBand.fromNullable(agentRow.pressureBand());
                String pressureRationale = row.pressureRationale() != null ? row.pressureRationale() : agentRow == null ? null : agentRow.pressureRationale();
                boolean baseCase = row.baseCase() || (agentRow != null && "Y".equalsIgnoreCase(agentRow.baselineYn()));
                return new DesignScenarioRow(
                    row.scenarioId(), row.isNew(), row.displayOrder(), row.scenarioName(), baseCase,
                    role, role == null ? null : role.getLabel(),
                    pressureBand, pressureBand == null ? null : pressureBand.getLabel(), pressureRationale,
                    enrichDimensions(row.dimensions(), valueOptions),
                    row.probability(), row.narrative(),
                    resolveSignposts(row.scenarioId(), signpostsByScenario, row.signposts()));
            })
            .toList();
        return DesignTable.of(columns, rows);
    }

    /** Agent 표 행 scenarioId 조인 맵 (배지 복원용). 미채번 행(scenarioId null)은 제외한다. */
    private static Map<Long, Row> toAgentRowsByScenarioId(Sections sections) {
        ScenarioTable scenarioTable = sections == null || sections.scenarioDesign() == null ? null : sections.scenarioDesign().scenarioTable();
        if (scenarioTable == null || scenarioTable.rows() == null) {
            return Map.of();
        }
        Map<Long, Row> rowsById = new LinkedHashMap<>();
        for (Row row : scenarioTable.rows()) {
            if (row.scenarioId() != null) {
                rowsById.putIfAbsent(row.scenarioId(), row);
            }
        }
        return rowsById;
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

    private static ScenarioGroupCreationState toCreationState(ScenarioPresentationAgentRes.Operation operation, JsonNode lastError) {
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
            .map(row -> {
                EventSourceType sourceType = EventSourceType.of(row.createLoginUserId(), row.modifyLoginUserId());
                return new EventBundle(
                    row.eventId(),
                    row.eventName(),
                    row.eventTypeLv1Code(),
                    row.eventTypeLv1Label(),
                    row.isScenarioSuggested(),
                    sourceType,
                    sourceType.getLabel(),
                    DateUtil.utcToKst(row.latestRecordFirstCreated()));
            })
            .toList();
    }

    /**
     * Agent presentation 응답에서 근거 마크다운 2종(Event 분석, 역할 선정 근거)만 추출한다 (그룹 상세 '근거 보기' 전용).
     * 근거 마크다운은 Agent 렌더러가 조회 시점에 생성하는 문서라 DB에 존재하지 않는다
     * (operation presentation_snapshot에는 렌더링 전 원재료만 저장됨을 전수 실측으로 확인, 2026-08-19).
     * 설계 화면과의 문서 동일성을 렌더러 버전 변경과 무관하게 보장하기 위해 조회 화면 DB 직조회 원칙의 예외로 Agent 원본을 사용한다.
     * 근거 섹션이 비어 있으면 null.
     */
    public static DesignEvidence toEvidence(ScenarioPresentationAgentRes source) {
        if (source == null) {
            return null;
        }
        DesignEvidence evidence = toEvidence(source.sections());
        return evidence.eventAnalysisMarkdown() == null && evidence.roleSelectionMarkdown() == null ? null : evidence;
    }

    private static DesignEvidence toEvidence(Sections sections) {
        return new DesignEvidence(
            normalizeMarkdown(sections == null ? null : sections.eventAnalysis()),
            normalizeMarkdown(sections == null ? null : sections.roleSelectionRationale())
        );
    }

    /** 준비 전 섹션 정규화 — Agent는 unavailable 섹션에 빈 문자열을 주므로 blank를 null로 통일한다 (계약: 준비 전이면 null). */
    private static String normalizeMarkdown(MarkdownSection section) {
        if (section == null || section.markdown() == null || section.markdown().isBlank()) {
            return null;
        }
        return section.markdown();
    }

    /**
     * scenarioTable + dimensionDefinitions → 설계 표. 헤더가 아직 없으면(생성 초기 스냅샷) null을 반환해
     * FE가 표 영역을 로딩 상태로 두게 한다. 헤더의 설명/근거는 definitions에서 dimensionId로 조인한다.
     */
    private static DesignTable toTable(Sections sections,
                                       Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        ScenarioTable scenarioTable = sections == null || sections.scenarioDesign() == null
            ? null
            : sections.scenarioDesign().scenarioTable();
        if (scenarioTable == null || scenarioTable.headers() == null || scenarioTable.headers().isEmpty()) {
            return null;
        }

        Map<String, AgentDimensionDefinitions.Item> itemsById = new LinkedHashMap<>();
        for (AgentDimensionDefinitions.Item item : definitionItems(sections)) {
            itemsById.putIfAbsent(item.dimensionId(), item);
        }

        List<Header> headers = scenarioTable.headers().stream()
            .sorted(Comparator.comparing(h -> h.displayOrder() == null ? Integer.MAX_VALUE : h.displayOrder()))
            .toList();

        List<ScenarioDimensionColumnRes> columns = headers.stream()
            .map(header -> {
                AgentDimensionDefinitions.Item item = itemsById.get(header.key());
                return new ScenarioDimensionColumnRes(
                    header.key(),
                    header.label(),
                    item == null ? null : item.description(),
                    item == null ? null : item.rationale());
            })
            .toList();

        List<DesignScenarioRow> rows = (scenarioTable.rows() == null ? List.<Row>of() : scenarioTable.rows()).stream()
            .map(row -> toDesignRow(row, headers, signpostsByScenario))
            .toList();

        return DesignTable.of(columns, rows);
    }

    private static DesignScenarioRow toDesignRow(Row row, List<Header> headers,
                                                 Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario) {
        Map<String, String> cells = row.cells() == null ? Map.of() : row.cells();
        Map<String, CellEvidence> cellEvidence = row.cellEvidence() == null ? Map.of() : row.cellEvidence();

        List<ScenarioDimensionRes> dimensions = headers.stream()
            .map(header -> {
                CellEvidence evidence = cellEvidence.get(header.key());
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

    private static Map<String, List<DesignDimensionValueOption>> toDimensionValueOptions(Sections sections) {
        Map<String, List<DesignDimensionValueOption>> options = new LinkedHashMap<>();
        for (AgentDimensionDefinitions.Item item : definitionItems(sections)) {
            List<DesignDimensionValueOption> values = (item.values() == null ? List.<AgentDimensionDefinitions.Value>of() : item.values()).stream()
                .map(value -> new DesignDimensionValueOption(
                    value.valueId(),
                    value.label(),
                    value.rationale()))
                .toList();
            options.putIfAbsent(item.dimensionId(), values);
        }
        return options;
    }

    /**
     * 추가 분기항목 후보 조회 응답 변환 (Agent Additional Dimensions GET API).
     * 후보 item은 presentation의 dimensionDefinitions.items와 동형이라 같은 수신 DTO를 재사용하고,
     * 미지 generationStatus는 null로 강등한다 (Agent 어휘 확장 내성).
     */
    public static AdditionalDimensionsRes toAdditionalDimensions(AdditionalDimensionsAgentRes agentRes) {
        AdditionalDimensionsAgentRes.Data data = agentRes == null ? null : agentRes.data();
        if (data == null) {
            return AdditionalDimensionsRes.of(null, null, List.of(), 0, null, null);
        }

        List<AgentDimensionDefinitions.Item> items =
            data.dimensionDefinitions() == null || data.dimensionDefinitions().items() == null
                ? List.of()
                : data.dimensionDefinitions().items();

        List<AdditionalDimensionsRes.AdditionalDimensionCandidate> candidates = items.stream()
            .map(item -> new AdditionalDimensionsRes.AdditionalDimensionCandidate(
                item.dimensionId(),
                item.label(),
                item.description(),
                item.rationale(),
                item.modelSupportStatus(),
                (item.values() == null ? List.<AgentDimensionDefinitions.Value>of() : item.values()).stream()
                    .map(value -> new AdditionalDimensionsRes.CandidateValue(
                        value.valueId(), value.label(), value.rationale(), value.pressureContribution()))
                    .toList()))
            .toList();

        return AdditionalDimensionsRes.of(
            data.scenarioGroupId(),
            AdditionalDimensionGenerationStatus.fromNullable(data.generationStatus()),
            candidates,
            data.availableCount(),
            data.updatedAt(),
            data.errorCode()
        );
    }

    private static List<AgentDimensionDefinitions.Item> definitionItems(Sections sections) {
        if (sections == null || sections.scenarioDesign() == null
            || sections.scenarioDesign().dimensionDefinitions() == null
            || sections.scenarioDesign().dimensionDefinitions().items() == null) {
            return List.of();
        }
        return sections.scenarioDesign().dimensionDefinitions().items();
    }

}
