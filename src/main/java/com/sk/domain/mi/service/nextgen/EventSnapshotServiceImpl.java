package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotLookupReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotOutlookListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotRecordListReq;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotCompareRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotDetailRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotLookupRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotRecordRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotSummaryRes;
import com.sk.domain.mi.service.nextgen.support.EventSnapshotDiffCalculator;
import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;
import com.sk.domain.shared.enums.nextgen.EventSnapshotUpdateTrigger;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotInstitutionViewRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotRecordMapRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [MCP Snapshot] Snapshot 3개 테이블은 읽기만 한다. 근거 Record·기관 전망은 Snapshot 사본만 조회하고
 * 현재 연결(tmm_mi_mievent_record_map)이나 원본 현재 값(tmm_mi_institution_view)으로 대체하지 않는다.
 * 시각: 저장값은 UTC, 응답은 KST, 입력(asOf·날짜 필터)은 KST 로 받아 UTC 로 바꿔 비교한다 (D-5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventSnapshotServiceImpl implements EventSnapshotService {

    private final MiEventSnapshotRepository miEventSnapshotRepository;
    private final MiEventSnapshotRecordMapRepository miEventSnapshotRecordMapRepository;
    private final MiEventSnapshotInstitutionViewRepository miEventSnapshotInstitutionViewRepository;
    private final MiEventRepository miEventRepository;
    private final AuthorizationService authorizationService;

    @Override
    public PaginationRes<EventSnapshotSummaryRes> getEventSnapshots(Long eventId, EventSnapshotListReq request) {
        String updateTriggerCd = Optional.ofNullable(request.updateTrigger()).map(EventSnapshotUpdateTrigger::getCode).orElse(null);
        return PaginationRes.toRes(miEventSnapshotRepository.findEventSnapshots(
                eventId,
                startOfDayUtc(request.startDate()),
                endOfDayExclusiveUtc(request.endDate()),
                updateTriggerCd,
                StringUtils.trimToNull(request.createdBy()),
                request.page(),
                request.size())
            .map(EventSnapshotSummaryRes::from));
    }

    @Override
    public EventSnapshotDetailRes getEventSnapshot(Long eventId, Long snapshotId) {
        return EventSnapshotDetailRes.from(getOwnedSnapshot(eventId, snapshotId));
    }

    @Override
    public EventSnapshotLookupRes lookupEventSnapshot(Long eventId, EventSnapshotLookupReq request) {
        if (request.versionNo() != null && request.asOf() != null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "versionNo and asOf cannot be used together.");
        }

        if (request.versionNo() != null) {
            return EventSnapshotLookupRes.of(EventSnapshotLookupRes.CRITERIA_VERSION_NO,
                miEventSnapshotRepository.findByMiEventIdAndSnapshotVersionNoAndIsDeletedFalse(eventId, request.versionNo())
                    .map(EventSnapshotDetailRes::from)
                    .orElse(null));
        }
        if (request.asOf() != null) {
            return EventSnapshotLookupRes.of(EventSnapshotLookupRes.CRITERIA_AS_OF,
                miEventSnapshotRepository.findEventSnapshotAsOf(eventId, DateUtil.kstToUtc(request.asOf()))
                    .map(EventSnapshotDetailRes::from)
                    .orElse(null));
        }
        return EventSnapshotLookupRes.of(EventSnapshotLookupRes.CRITERIA_LATEST,
            miEventSnapshotRepository.findLatestEventSnapshot(eventId)
                .map(EventSnapshotDetailRes::from)
                .orElse(null));
    }

    @Override
    public EventSnapshotCompareRes compareEventSnapshot(Long eventId, Long snapshotId, EventSnapshotCompareTarget target) {
        EventSnapshotCompareTarget compareTarget = target == null ? EventSnapshotCompareTarget.PREVIOUS : target;
        MiEventSnapshot base = getOwnedSnapshot(eventId, snapshotId);
        List<String> recordedChangedFields = JsonUtil.parseStringList(base.getChangedFieldsJson());

        EventSnapshotCompareRes.Side compared;
        EventSnapshotDiffCalculator.State comparedState;
        if (compareTarget == EventSnapshotCompareTarget.PREVIOUS) {
            // 이전 버전은 previous_mi_event_snapshot_id 로만 찾는다. 버전 번호 -1 로 대신 찾지 않는다.
            Optional<MiEventSnapshot> previous = Optional.ofNullable(base.getPreviousMiEventSnapshotId())
                .flatMap(previousId -> miEventSnapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(previousId, eventId));
            compared = previous.map(EventSnapshotServiceImpl::sideOf).orElse(null);
            comparedState = previous.map(EventSnapshotDiffCalculator.State::of).orElse(null);
        } else {
            // 현재 Event 는 삭제 여부와 무관하게 조회한다.
            Optional<MiEvent> current = miEventRepository.findById(eventId);
            compared = current.map(event -> new EventSnapshotCompareRes.Side(null, null, null, DateUtil.utcToKst(event.getModifyDt())))
                .orElse(null);
            comparedState = current.map(EventSnapshotDiffCalculator.State::of).orElse(null);
        }

        if (comparedState == null) {
            return new EventSnapshotCompareRes(eventId, compareTarget, sideOf(base), null, false, List.of(), List.of(), recordedChangedFields);
        }

        EventSnapshotDiffCalculator.Result result = EventSnapshotDiffCalculator.compare(EventSnapshotDiffCalculator.State.of(base), comparedState);
        return new EventSnapshotCompareRes(eventId, compareTarget, sideOf(base), compared, true,
            result.differences(), result.sameFields(), recordedChangedFields);
    }

    @Override
    public PaginationRes<EventSnapshotRecordRes> getEventSnapshotRecords(Long eventId, Long snapshotId, EventSnapshotRecordListReq request) {
        MiEventSnapshot snapshot = getOwnedSnapshot(eventId, snapshotId);

        // isPermitted 는 기존 이벤트 연관 레코드 조회(EventServiceImpl#getEventRecords)와 같은 방식으로 계산한다.
        // [MCP Operator] 운영자는 상품 권한 정보가 없어 전부 isPermitted=false (EventServiceImpl#getEventRecords 와 동일)
        // [운영자 서비스 접속] 타사접속 가능 운영자는 자기 회사 매핑 상품 기준, 불가 운영자만 전부 false
        Map<Integer, Boolean> goodsViewableMap = authorizationService.getCurrentRole() == Role.OPERATOR
            && !authorizationService.isServiceAccessOperator()
            ? Map.of()
            : authorizationService.getPermissions().getGoodsViewableMap(authorizationService.getCompanyNoWithPriority());

        return PaginationRes.toRes(miEventSnapshotRecordMapRepository.findEventSnapshotRecords(
                snapshotId,
                StringUtils.trimToNull(request.recordType()),
                StringUtils.trimToNull(request.refNm()),
                startOfDayUtc(request.startDate()),
                endOfDayExclusiveUtc(request.endDate()),
                PageRequest.of(request.page() - 1, request.size()))
            .map(row -> EventSnapshotRecordRes.from(
                row,
                snapshot.getCreateDt(),
                row.getGoodsNo() != null && goodsViewableMap.getOrDefault(row.getGoodsNo(), Boolean.FALSE))));
    }

    @Override
    public PaginationRes<EventSnapshotOutlookRes> getEventSnapshotOutlooks(Long eventId, Long snapshotId, EventSnapshotOutlookListReq request) {
        getOwnedSnapshot(eventId, snapshotId);

        return PaginationRes.toRes(miEventSnapshotInstitutionViewRepository.findEventSnapshotOutlooks(
                snapshotId,
                StringUtils.trimToNull(request.institutionName()),
                request.startDate(),
                request.endDate(),
                request.page(),
                request.size())
            .map(EventSnapshotOutlookRes::from));
    }

    /**
     * 그 Event 의 Snapshot 일 때만 반환한다. 없거나 다른 Event 소속이면 RSC-001 (기존 getEventDetail 과 같은 처리).
     */
    private MiEventSnapshot getOwnedSnapshot(Long eventId, Long snapshotId) {
        return miEventSnapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(snapshotId, eventId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Event snapshot not found: eventId=%d, snapshotId=%d".formatted(eventId, snapshotId)));
    }

    private static EventSnapshotCompareRes.Side sideOf(MiEventSnapshot snapshot) {
        return new EventSnapshotCompareRes.Side(
            snapshot.getMiEventSnapshotId(),
            snapshot.getSnapshotVersionNo(),
            DateUtil.utcToKst(snapshot.getCreateDt()),
            DateUtil.utcToKst(snapshot.getEventModifyDt()));
    }

    // KST 날짜의 00:00 을 UTC 로 (포함 하한)
    private static LocalDateTime startOfDayUtc(LocalDate kstDate) {
        return kstDate == null ? null : DateUtil.kstToUtc(kstDate.atStartOfDay());
    }

    // KST 날짜 다음날 00:00 을 UTC 로 (미포함 상한)
    private static LocalDateTime endOfDayExclusiveUtc(LocalDate kstDate) {
        return kstDate == null ? null : DateUtil.kstToUtc(kstDate.plusDays(1).atStartOfDay());
    }

}
