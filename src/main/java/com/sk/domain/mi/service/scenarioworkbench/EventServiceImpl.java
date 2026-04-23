package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.*;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("!mock")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    @Override
    public EventListRes getEvents(EventListReq request) {
        EventSummaryNoticeRes summaryNotice = new EventSummaryNoticeRes(0, 0);
        PaginationRes<EventListItemRes> events = PaginationRes.of(
            0, 0, request.page(), 0, request.size(),
            true, true, false, false,
            Collections.emptyList()
        );
        return new EventListRes(summaryNotice, EventAppliedFilterRes.from(request), events);
    }

    @Override
    public EventDetailRes getEventDetail(String eventId) {
        throw new UnsupportedOperationException("실 DB 연동 미구현. mock 프로파일을 사용하세요.");
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(String eventId, int page, int size) {
        throw new UnsupportedOperationException("실 DB 연동 미구현. mock 프로파일을 사용하세요.");
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(String eventId, int page, int size) {
        throw new UnsupportedOperationException("실 DB 연동 미구현. mock 프로파일을 사용하세요.");
    }

}
