package com.sk.domain.shared.repository.master.tmm.custom;

import java.util.Optional;

import org.springframework.data.domain.Page;

import java.util.List;

import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.shared.repository.master.tmm.projection.EventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventLinkedScenarioGroupRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;

public interface CustomMiEventRepository {

    /**
     * @param currentUserLoginId 로그인 사용자 로그인 ID — "내가 만든 이벤트" 필터에 사용
     */
    Page<EventListRow> getEventsByConditions(EventListReq request, String currentUserLoginId);

    Optional<EventDetailRow> findEventDetail(Long eventId);

    Page<EventOutlookRow> findEventOutlooks(Long eventId, int page, int size);

    List<EventLinkedScenarioGroupRow> findLinkedScenarioGroups(Long eventId);

}
