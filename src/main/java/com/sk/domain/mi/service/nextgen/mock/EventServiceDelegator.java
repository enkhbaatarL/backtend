package com.sk.domain.mi.service.nextgen.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.EventServiceImpl;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class EventServiceDelegator implements EventService {

    private final EventServiceImpl realService;
    private final EventMockService mockService;
    private final NextGenMockProperties mockProps;

    @Override
    public List<EventTypeL1Res> getEventTypesL1() {
        return realService.getEventTypesL1();
    }

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

    @Override
    public List<EventDesignAvailabilityRes> getEventDesignAvailability(List<Long> eventIds) {
        return mockProps.getEvents().getApi().isDesignAvailability()
            ? mockService.getEventDesignAvailability(eventIds)
            : realService.getEventDesignAvailability(eventIds);
    }

}
