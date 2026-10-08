package com.sk.domain.shared.repository.master.tmm;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmm.MiEventSnapshotRecordMap;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotRecordRow;

/**
 * [MCP Snapshot] Snapshot 근거 Record 조회 전용. 현재 연결(tmm_mi_mievent_record_map)이 아니라 Snapshot 사본만 기준으로 한다.
 */
public interface MiEventSnapshotRecordMapRepository extends JpaRepository<MiEventSnapshotRecordMap, Long> {

    /**
     * 근거 Record 목록 + 원본 Record 의 현재 제목·요약·상품 정보 (식별용).
     * MiEventRepository#findEventRecords 의 NEWS / 비NEWS 구조를 따르되
     * (1) 시작 테이블은 Snapshot 사본, (2) 원본 조인은 모두 LEFT JOIN, (3) 원본 del_yn 조건은 ON 절에 두어
     * 원본이 삭제돼도 이력 행이 빠지지 않게 한다. 분기는 사본의 record_type 기준이며 각 사본 행은 한 분기에만 속한다.
     * 시각 인자는 UTC (record_first_created 저장 기준).
     */
    @Query(
        value = """
            SELECT t.miRecordId, t.recordType, t.refNm, t.recordFirstCreated, t.title, t.summary, t.goodsNo, t.goodsDataNo, t.sourceObjId, t.sourceAvailable
            FROM (
                SELECT srm.mi_record_id AS miRecordId, srm.record_type AS recordType, srm.ref_nm AS refNm, srm.record_first_created AS recordFirstCreated,
                       d.title AS title, s.smry AS summary, g.gds_no AS goodsNo, NULL AS goodsDataNo, r.source_obj_id AS sourceObjId,
                       CASE WHEN d.data_reg_sn IS NULL THEN 0 ELSE 1 END AS sourceAvailable
                FROM tmm_mi_mievent_snapshot_record_map AS srm
                LEFT JOIN tmd_mi_record AS r ON r.mi_record_id = srm.mi_record_id
                    AND r.del_yn = 'N'
                LEFT JOIN tmd_nws_smry AS s ON s.nws_smry_id = r.source_obj_id
                    AND s.del_yn = 'N'
                LEFT JOIN tif_refinewsdatar AS d ON d.data_reg_sn = s.data_reg_sn
                    AND d.first_created = s.first_created
                LEFT JOIN tmd_gdsm AS g ON g.gds_id = s.gds_id
                    AND g.del_yn = 'N'
                WHERE srm.mi_event_snapshot_id = :snapshotId
                    AND srm.del_yn = 'N'
                    AND srm.record_type = 'NEWS'
                    AND (:recordType IS NULL OR srm.record_type = :recordType)
                    AND (:refNm IS NULL OR srm.ref_nm LIKE CONCAT('%', :refNm, '%'))
                    AND (:fromUtc IS NULL OR srm.record_first_created >= :fromUtc)
                    AND (:toUtc IS NULL OR srm.record_first_created < :toUtc)

                UNION ALL

                SELECT srm.mi_record_id, srm.record_type, srm.ref_nm, srm.record_first_created,
                       m.gds_data_nm, s.file_smry_shrt, m.gds_no, m.gds_data_no, r.source_obj_id,
                       CASE WHEN m.gds_data_no IS NULL THEN 0 ELSE 1 END
                FROM tmm_mi_mievent_snapshot_record_map AS srm
                LEFT JOIN tmd_mi_record AS r ON r.mi_record_id = srm.mi_record_id
                    AND r.del_yn = 'N'
                LEFT JOIN tmd_gdsdatam_file_smry AS s ON s.gds_data_smry_no = r.source_obj_id
                    AND s.del_yn = 'N'
                LEFT JOIN tmd_gdsdatam AS m ON m.gds_data_no = s.gds_data_no
                    AND m.del_yn = 'N'
                WHERE srm.mi_event_snapshot_id = :snapshotId
                    AND srm.del_yn = 'N'
                    AND srm.record_type <> 'NEWS'
                    AND (:recordType IS NULL OR srm.record_type = :recordType)
                    AND (:refNm IS NULL OR srm.ref_nm LIKE CONCAT('%', :refNm, '%'))
                    AND (:fromUtc IS NULL OR srm.record_first_created >= :fromUtc)
                    AND (:toUtc IS NULL OR srm.record_first_created < :toUtc)
            ) AS t
            ORDER BY t.recordFirstCreated DESC, t.miRecordId DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM tmm_mi_mievent_snapshot_record_map AS srm
            WHERE srm.mi_event_snapshot_id = :snapshotId
                AND srm.del_yn = 'N'
                AND (:recordType IS NULL OR srm.record_type = :recordType)
                AND (:refNm IS NULL OR srm.ref_nm LIKE CONCAT('%', :refNm, '%'))
                AND (:fromUtc IS NULL OR srm.record_first_created >= :fromUtc)
                AND (:toUtc IS NULL OR srm.record_first_created < :toUtc)
            """,
        nativeQuery = true
    )
    Page<EventSnapshotRecordRow> findEventSnapshotRecords(@Param("snapshotId") Long snapshotId,
                                                          @Param("recordType") String recordType,
                                                          @Param("refNm") String refNm,
                                                          @Param("fromUtc") LocalDateTime fromUtc,
                                                          @Param("toUtc") LocalDateTime toUtc,
                                                          Pageable pageable);

}
