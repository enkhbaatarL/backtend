package com.sk.domain.mi.service.nextgen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBaseEventRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBulkStatusRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioGroupServiceImpl implements ScenarioGroupService {

    private final MiScenarioGroupRepository scenarioGroupRepository;

    @Override
    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {

        Page<ScenarioGroupBaseRow> page = scenarioGroupRepository.findScenarioGroupPage(request);
        List<ScenarioGroupBaseRow> bases = page.getContent();

        List<Long> groupIds = bases.stream().map(ScenarioGroupBaseRow::scenarioGroupId).toList();
        List<Long> eventTypeL2Ids = bases.stream()
            .map(ScenarioGroupBaseRow::eventTypeL2Id)
            .distinct()
            .toList();

        // 그룹별 시나리오 (displayOrder 순) + event_type_l2 별 dimension template 일괄 조회
        Map<Long, List<ScenarioBaseRow>> scenariosByGroup = scenarioGroupRepository.findScenariosByGroupIds(groupIds).stream()
            .collect(Collectors.groupingBy(ScenarioBaseRow::scenarioGroupId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, GroupDimensionTemplate> templates = ScenarioDimensionMapper.parseTemplates(
            scenarioGroupRepository.findTemplatesByEventTypeL2Ids(eventTypeL2Ids)
        );

        Map<Long, ScenarioGroupListItemRes> itemsByGroupId = new LinkedHashMap<>();
        for (ScenarioGroupBaseRow base : bases) {
            GroupDimensionTemplate template = templates.getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
            List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

            List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
            List<ScenarioRowRes> scenarios = scenariosByGroup.getOrDefault(base.scenarioGroupId(), List.of()).stream()
                .map(s -> ScenarioRowRes.of(s, template.dimensions(selectedKeys, s.dimensionValues())))
                .toList();

            itemsByGroupId.put(base.scenarioGroupId(), ScenarioGroupListItemRes.of(base, dimensionColumns, scenarios));
        }

        return new ScenarioGroupListRes(
            ScenarioGroupAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(b -> itemsByGroupId.get(b.scenarioGroupId())))
        );
    }

    @Override
    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {

        Page<ScenarioIndividualBaseRow> page = scenarioGroupRepository.findScenarioPage(request);
        List<ScenarioIndividualBaseRow> bases = page.getContent();

        List<Long> eventTypeL2Ids = bases.stream()
            .map(ScenarioIndividualBaseRow::eventTypeL2Id)
            .distinct()
            .toList();

        Map<Long, GroupDimensionTemplate> templates = ScenarioDimensionMapper.parseTemplates(
            scenarioGroupRepository.findTemplatesByEventTypeL2Ids(eventTypeL2Ids)
        );

        Map<Long, ScenarioIndividualItemRes> itemsByScenarioId = new LinkedHashMap<>();
        for (ScenarioIndividualBaseRow base : bases) {
            GroupDimensionTemplate template = templates.getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
            List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());
            List<ScenarioDimensionRes> dimensions = template.dimensions(selectedKeys, base.dimensionValues());

            itemsByScenarioId.put(base.scenarioId(), ScenarioIndividualItemRes.of(base, dimensions));
        }

        return new ScenarioIndividualListRes(
            ScenarioGroupAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(b -> itemsByScenarioId.get(b.scenarioId())))
        );
    }

    @Override
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group graph DB 연동 미구현");
    }

    @Override
    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group graph export DB 연동 미구현");
    }

    @Override
    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group not found: " + scenarioGroupId));

        GroupDimensionTemplate template = ScenarioDimensionMapper.parseTemplates(
                scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())))
            .getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

        List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
        List<ScenarioGroupDetailRowRes> scenarios = scenarioGroupRepository
            .findScenariosByGroupIds(List.of(scenarioGroupId)).stream()
            .map(s -> ScenarioGroupDetailRowRes.of(
                s,
                template.dimensions(selectedKeys, s.dimensionValues()),
                ScenarioStatus.fromLabel(s.judgmentStatusCode())
            ))
            .toList();

        ScenarioBaseEventRes baseEvent = new ScenarioBaseEventRes(
            base.eventId(), base.eventName(), base.eventTypeLv1Code(), base.eventTypeLv1Label(), base.eventUpdatedAt());

        return new ScenarioGroupDetailRes(
            base.scenarioGroupId(),
            base.groupName(),
            base.createdAt(),
            baseEvent,
            base.summary(),
            base.scenarioCount() == null ? scenarios.size() : base.scenarioCount(),
            dimensionColumns,
            scenarios
        );
    }

    @Override
    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario detail DB 연동 미구현");
    }

    @Override
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioStatus status) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario status 변경 DB 연동 미구현");
    }

    @Override
    public ScenarioBulkStatusRes bulkUpdateScenarioStatus(Long scenarioGroupId, ScenarioStatus status) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario status 일괄 변경 DB 연동 미구현");
    }

}
