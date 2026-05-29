package com.sk.domain.mi.service.scenarioworkbench;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventAppliedFilterRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListItemRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventTypeL1Res;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventTypeL1Repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final MiEventRepository miEventRepository;
    private final MiEventTypeL1Repository miEventTypeL1Repository;

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
        return miEventRepository.findEventDetail(eventId)
            .map(EventDetailRes::from)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("Event not found: %d", eventId)));
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventRecords(eventId, PageRequest.of(page - 1, size)).map(EventRelatedRecordRes::from)
        );
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventOutlooks(eventId, PageRequest.of(page - 1, size)).map(EventInstitutionOutlookRes::from)
        );
    }

}
