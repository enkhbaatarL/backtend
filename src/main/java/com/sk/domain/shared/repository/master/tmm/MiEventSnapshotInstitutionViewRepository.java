package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmm.MiEventSnapshotInstitutionView;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiEventSnapshotInstitutionViewRepository;

/**
 * [MCP Snapshot] Snapshot 기관 전망 사본 조회 전용. 원본 tmm_mi_institution_view 를 쓰지 않는다.
 */
public interface MiEventSnapshotInstitutionViewRepository
    extends JpaRepository<MiEventSnapshotInstitutionView, Long>, CustomMiEventSnapshotInstitutionViewRepository {

}
