package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시나리오 설계 실 구현 (Agent 프록시). Agent 연동 확정 전이라 대부분 미구현(mock 라우팅)이나,
 * 분기항목/분기기준 템플릿은 master 데이터라 mock 없이 항상 이 실 구현(DB)을 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignServiceImpl implements ScenarioDesignService {

    private static final String NOT_IMPLEMENTED =
        "ScenarioDesign 실 구현 미완성 (Agent 연동 대기). mi.next-gen.mock.design.api.* 를 활성화하세요.";

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final AuthorizationService authorizationService;

    /**
     * [설계 시작] Agent 바이패스
     *  - 그룹/시나리오 생성 및 DB 적재는 Agent가 수행
     */
    @Override
    public ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStateRes getDesign(Long scenarioGroupId) {

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        ScenarioGroupStatus status = group.getStatus();
        ScenarioGroupStage stage = group.getStage();
        boolean occupied = status == ScenarioGroupStatus.CREATING;

        // 분기항목 헤더: event_type_l2 template + dimension_selected (그룹 상세/이벤트 연관시나리오와 동일)
        Long eventTypeL2Id = group.getEventTypeL2().getEventTypeL2Id();
        GroupDimensionTemplate template = ScenarioDimensionMapper
            .parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(eventTypeL2Id)))
            .getOrDefault(eventTypeL2Id, GroupDimensionTemplate.EMPTY);

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(group.getDimensionSelected());
        List<ScenarioDimensionColumnRes> columns = template.columns(selectedKeys);

        List<MiScenario> scenarios = scenarioGroupRepository.findScenarioEntitiesByGroupId(scenarioGroupId);
        Optional<ScenarioValidationCache> cache = validationStore.find(scenarioGroupId);

        boolean step1Stage = stage == ScenarioGroupStage.SCENARIO_CREATED || stage == ScenarioGroupStage.SCENARIO_VALIDATED;
        boolean step2Stage = stage == ScenarioGroupStage.SIGNPOST_CREATED || stage == ScenarioGroupStage.COMPLETED;

        // step1: 검토 캐시가 있으면 Agent reconcile 표로 오버레이, 없으면 DB 시나리오로 구성
        DesignTable step1 = null;
        if (step1Stage) {
            step1 = cache.map(ScenarioValidationCache::table)
                .map(ScenarioDesignTableMapper::toDesignTable)
                .orElseGet(() -> DesignTable.of(columns, toStep1Rows(scenarios, template, selectedKeys)));
        }

        // step2: One-view (그룹 메타 + narrative/probability/Signpost 포함 표)
        ScenarioDesignStateRes.OneView step2 = null;
        if (step2Stage) {
            Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario = loadSignpostsByScenario(scenarioGroupId);
            List<DesignScenarioRow> rows = scenarios.stream()
                .map(s -> DesignScenarioRow.of(
                    s.getScenarioId(), s.getDisplayOrder(), s.getTitle(),
                    Boolean.TRUE.equals(s.getIsBaseline()),
                    template.dimensions(selectedKeys, s.getDimensionValues()),
                    s.getProbabilityScore() == null ? null : s.getProbabilityScore().intValue(),
                    s.getNarrative(),
                    signpostsByScenario.getOrDefault(s.getScenarioId(), List.of())))
                .toList();
            step2 = ScenarioDesignStateRes.OneView.of(
                group.getTitle(), group.getSummary(), group.getDescription(), DesignTable.of(columns, rows));
        }

        ScenarioDesignStateRes.Validation validation = cache
            .map(c -> ScenarioDesignStateRes.Validation.of(c.isValid(), c.reason()))
            .orElse(null);

        return ScenarioDesignStateRes.of(scenarioGroupId, status, stage, occupied, group.getRequestUserId(), step1, step2, validation);
    }

    /** DB 시나리오 → step1 행(분기항목/시나리오만, narrative/probability/Signpost는 null). */
    private List<DesignScenarioRow> toStep1Rows(List<MiScenario> scenarios, GroupDimensionTemplate template, List<String> selectedKeys) {
        return scenarios.stream()
            .map(s -> DesignScenarioRow.of(
                s.getScenarioId(), s.getDisplayOrder(), s.getTitle(),
                Boolean.TRUE.equals(s.getIsBaseline()),
                template.dimensions(selectedKeys, s.getDimensionValues()),
                null, null, null))
            .toList();
    }

    /** 그룹의 Signpost를 한 번에 조회해 시나리오ID별로 묶는다 (사용중인 것만, displayOrder 순). */
    private Map<Long, List<ScenarioSignpostDefinitionViewRes>> loadSignpostsByScenario(Long scenarioGroupId) {
        return scenarioGroupRepository.findSignpostEntitiesByGroupId(scenarioGroupId).stream()
            .filter(sp -> Boolean.TRUE.equals(sp.getIsUsed()))
            .sorted(Comparator.comparing(sp -> sp.getDisplayOrder() == null ? Integer.MAX_VALUE : sp.getDisplayOrder()))
            .collect(Collectors.groupingBy(
                sp -> sp.getScenario().getScenarioId(),
                Collectors.mapping(sp -> ScenarioSignpostDefinitionViewRes.of(
                    sp.getSignpostId(), sp.getLabel(), sp.getProbabilityDirection(),
                    sp.getProbabilityImpactScore() == null ? null : sp.getProbabilityImpactScore().intValue(),
                    sp.getStrength(), sp.getSignpostReason(), sp.getDisplayOrder()),
                    Collectors.toList())));
    }

    /**
     * 분기항목/분기기준 템플릿 조회 (항상 DB). 그룹의 event_type_l2 기준 template_json을 파싱해 내린다.
     */
    @Override
    public ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        List<ScenarioDimensionTemplateRes.Item> items = scenarioGroupRepository
            .findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())).stream()
            .findFirst()
            .map(row -> ScenarioDimensionMapper.parseTemplateItems(row.templateJson()))
            .orElseGet(List::of);

        return new ScenarioDimensionTemplateRes(items);
    }

    /**
     * [변경사항 검토] Agent 바이패스
     *  - 편집된 설계를 Agent 표로 변환해 검토를 요청
     */
    @Override
    public ScenarioDesignStageRes validate(Long scenarioGroupId, ScenarioValidationReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStageRes confirm(Long scenarioGroupId, ScenarioConfirmationReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStateRes reset(Long scenarioGroupId) {
        validationStore.delete(scenarioGroupId);
        return getDesign(scenarioGroupId);
    }

}
