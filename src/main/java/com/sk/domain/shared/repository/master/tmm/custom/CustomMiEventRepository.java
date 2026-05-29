package com.sk.domain.shared.repository.master.tmm.custom;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;

public interface CustomMiEventRepository {

    Page<EventListRow> getEventsByConditions(EventListReq request);

    Optional<EventDetailRow> findEventDetail(Long eventId);

    Page<EventOutlookRow> findEventOutlooks(Long eventId, int page, int size);

}
