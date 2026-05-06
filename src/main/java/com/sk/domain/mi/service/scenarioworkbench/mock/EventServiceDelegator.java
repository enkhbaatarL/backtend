package com.sk.domain.mi.service.scenarioworkbench.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.config.ScenarioWorkbenchMockProperties;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventRelatedRecordRes;
import com.sk.domain.mi.service.scenarioworkbench.EventService;
import com.sk.domain.mi.service.scenarioworkbench.EventServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class EventServiceDelegator implements EventService {

    private final EventServiceImpl realService;
    private final EventMockService mockService;
    private final ScenarioWorkbenchMockProperties mockProps;

    @Override
    public EventListRes getEvents(EventListReq request) {
        return mockProps.getEvents().getApi().isGetList()
            ? mockService.getEvents(request)
            : realService.getEvents(request);
    }

    @Override
    public EventDetailRes getEventDetail(Long eventId) {
        return mockProps.getEvents().getApi().isGetDetail()
            ? mockService.getEventDetail(eventId)
            : realService.getEventDetail(eventId);
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        return mockProps.getEvents().getApi().isGetRecords()
            ? mockService.getEventRecords(eventId, page, size)
            : realService.getEventRecords(eventId, page, size);
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        return mockProps.getEvents().getApi().isGetOutlooks()
            ? mockService.getEventOutlooks(eventId, page, size)
            : realService.getEventOutlooks(eventId, page, size);
    }

}
