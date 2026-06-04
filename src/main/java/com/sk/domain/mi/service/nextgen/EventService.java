package com.sk.domain.mi.service.nextgen;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;

import java.util.List;

public interface EventService {

    EventListRes getEvents(EventListReq request);

    EventDetailRes getEventDetail(Long eventId);

    PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size);

    PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size);

    List<EventTypeL1Res> getEventTypesL1();

}
