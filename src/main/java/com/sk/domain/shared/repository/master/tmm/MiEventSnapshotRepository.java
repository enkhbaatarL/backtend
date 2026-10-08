package com.sk.domain.shared.repository.master.tmm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiEventSnapshotRepository;

/**
 * [MCP Snapshot] Event 버전 이력(Snapshot) 조회 전용. save/delete 를 호출하지 않는다 (파이프라인 소유, append-only).
 * 모든 조회는 행 논리 삭제(del_yn)만 거르고 당시 Event 삭제 여부(event_del_yn)로는 거르지 않는다.
 */
public interface MiEventSnapshotRepository extends JpaRepository<MiEventSnapshot, Long>, CustomMiEventSnapshotRepository {

    /** 그 Event 의 Snapshot 일 때만 조회된다 (다른 Event 의 snapshotId 는 empty). */
    Optional<MiEventSnapshot> findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(Long miEventSnapshotId, Long miEventId);

    /** uk_tmm_mi_mievent_snapshot_m_01 (mi_event_id, snapshot_version_no). 버전 번호가 NULL 인 행은 찾을 수 없다. */
    Optional<MiEventSnapshot> findByMiEventIdAndSnapshotVersionNoAndIsDeletedFalse(Long miEventId, Long snapshotVersionNo);

}
