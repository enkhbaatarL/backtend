package com.sk.domain.mi.service.nextgen;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;

import java.util.List;

public interface EventService {

    // 이벤트 유형 lv1 조회
    List<EventTypeL1Res> getEventTypesL1();

    EventListRes getEvents(EventListReq request);

    EventDetailRes getEventDetail(Long eventId);

    PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size);

    PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size);

    List<EventDesignAvailabilityRes> getEventDesignAvailability(List<Long> eventIds);

}
