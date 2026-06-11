package com.sk.domain.mi.service.nextgen;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventListItemRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenariosRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventTypeL1Repository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final MiEventRepository miEventRepository;
    private final MiEventTypeL1Repository miEventTypeL1Repository;
    private final MiScenarioGroupRepository miScenarioGroupRepository;

    @Override
    public List<EventTypeL1Res> getEventTypesL1() {
        return miEventTypeL1Repository.findAllByIsDeletedFalseOrderByEventTypeL1IdAsc()
            .stream()
            .map(EventTypeL1Res::from)
            .toList();
    }

    @Override
    public EventListRes getEvents(EventListReq request) {
        return new EventListRes(
            EventAppliedFilterRes.from(request),
            PaginationRes.toRes(miEventRepository.getEventsByConditions(request).map(EventListItemRes::from))
        );
    }

    @Override
    public EventDetailRes getEventDetail(Long eventId) {
        EventDetailRow row = miEventRepository.findEventDetail(eventId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Event not found: %d".formatted(eventId)));

        return EventDetailRes.from(row, buildRelatedScenarios(row));
    }

    private EventRelatedScenariosRes buildRelatedScenarios(EventDetailRow row) {
        ScenarioGroupBaseRow group = row.relatedScenarioGroup();

        if (group == null) {
            return null;
        }

        GroupDimensionTemplate template = ScenarioDimensionMapper.parseTemplates(miScenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(group.eventTypeL2Id())))
            .getOrDefault(group.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(group.dimensionSelected());

        List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
        Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(row.relatedScenarios());
        List<EventRelatedScenarioRes> scenarios = row.relatedScenarios().stream()
            .map(s -> EventRelatedScenarioRes.of(
                s,
                template.dimensions(selectedKeys, s.dimensionValues()),
                adjusted.get(s.scenarioId())
            ))
            .toList();

        return EventRelatedScenariosRes.of(group, dimensionColumns, scenarios);
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventOutlooks(eventId, page, size).map(EventInstitutionOutlookRes::from)
        );
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventRecords(eventId, PageRequest.of(page - 1, size)).map(EventRelatedRecordRes::from)
        );
    }

}
