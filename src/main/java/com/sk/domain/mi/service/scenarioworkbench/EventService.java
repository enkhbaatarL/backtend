package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventRelatedRecordRes;

public interface EventService {

    EventListRes getEvents(EventListReq request);

    EventDetailRes getEventDetail(String eventId);

    PaginationRes<EventRelatedRecordRes> getEventRecords(String eventId, int page, int size);

    PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(String eventId, int page, int size);

}
