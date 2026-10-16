package com.sk.domain.mi.service.nextgen;

import com.sk.common.model.PaginationRes;
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
import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;

/**
 * [MCP Snapshot] Event 버전 이력(Snapshot) 조회. MCP 전용이며 원본 화면 API 는 없다.
 */
public interface EventSnapshotService {

    PaginationRes<EventSnapshotSummaryRes> getEventSnapshots(Long eventId, EventSnapshotListReq request);

    EventSnapshotDetailRes getEventSnapshot(Long eventId, Long snapshotId);

    EventSnapshotLookupRes lookupEventSnapshot(Long eventId, EventSnapshotLookupReq request);

    EventSnapshotCompareRes compareEventSnapshot(Long eventId, Long snapshotId, EventSnapshotCompareTarget target);

    PaginationRes<EventSnapshotRecordRes> getEventSnapshotRecords(Long eventId, Long snapshotId, EventSnapshotRecordListReq request);

    PaginationRes<EventSnapshotOutlookRes> getEventSnapshotOutlooks(Long eventId, Long snapshotId, EventSnapshotOutlookListReq request);

}
