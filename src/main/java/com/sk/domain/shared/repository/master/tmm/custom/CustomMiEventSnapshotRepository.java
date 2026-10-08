package com.sk.domain.shared.repository.master.tmm.custom;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotListRow;

/**
 * [MCP Snapshot] Snapshot 목록·최신·시점 조회. 시각 인자는 모두 UTC (KST 입력 변환은 서비스에서 한다).
 */
public interface CustomMiEventSnapshotRepository {

    /**
     * 버전 목록. 정렬: snapshot_version_no DESC (NULL 은 뒤), crt_dtm DESC, mi_event_snapshot_id DESC.
     *
     * @param createdFromUtc  crt_dtm 하한 (포함), null 이면 조건 없음
     * @param createdToUtc    crt_dtm 상한 (미포함), null 이면 조건 없음
     * @param updateTriggerCd update_trigger_cd 일치, null 이면 조건 없음
     * @param createdBy       crt_usr_logn_id 대소문자 무시 일치, null 이면 조건 없음
     */
    Page<EventSnapshotListRow> findEventSnapshots(Long miEventId, LocalDateTime createdFromUtc, LocalDateTime createdToUtc,
                                                  String updateTriggerCd, String createdBy, int page, int size);

    /** 최신 저장 버전 = 목록 정렬의 첫 1건. */
    Optional<MiEventSnapshot> findLatestEventSnapshot(Long miEventId);

    /** crt_dtm <= asOfUtc 중 최신 1건. 같은 시각이면 버전이 큰 것. */
    Optional<MiEventSnapshot> findEventSnapshotAsOf(Long miEventId, LocalDateTime asOfUtc);

}
