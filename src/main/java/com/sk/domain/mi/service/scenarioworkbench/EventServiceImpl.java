package com.sk.domain.mi.service.scenarioworkbench;

import org.springframework.context.annotation.Profile;
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
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("!mock")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final MiEventRepository miEventRepository;

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
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("event id: %d", eventId)));
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventRecords(eventId, PageRequest.of(page - 1, size)).map(EventRelatedRecordRes::from)
        );
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        throw new UnsupportedOperationException("실 DB 연동 미구현. mock 프로파일을 사용하세요.");
    }

}
