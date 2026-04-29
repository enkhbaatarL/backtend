package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;

public interface MiEventRepository extends JpaRepository<MiEvent, Long>, CustomMiEventRepository {

    @Query(
        value = """
            SELECT t.first_created AS firstCreated,
                   t.record_type   AS recordType,
                   t.title         AS title,
                   t.smry          AS summary,
                   t.gds_no        AS goodsNo
            FROM (SELECT r.first_created, r.record_type, d.title, s.smry, g.gds_no
                  FROM tmd_mievent_source_mapm AS sm
                           JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                      AND r.record_type = 'NEWS'
                      AND r.is_deleted = false
                           JOIN tmd_nws_smry AS s ON s.nws_smry_id = r.source_obj_id
                      AND s.is_deleted = false
                           JOIN tif_refinewsdatar AS d ON d.data_reg_sn = s.data_reg_sn
                      AND d.first_created = s.first_created
                           JOIN tmd_gdsm AS g ON g.gds_id = s.gds_id
                      AND g.is_deleted = false
                  WHERE sm.mi_event_id = :eventId
                  UNION ALL
                  SELECT r.first_created, r.record_type, m.gds_data_nm, m.gds_data_expn, g.gds_no
                  FROM tmd_mievent_source_mapm AS sm
                           JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                      AND r.record_type <> 'NEWS'
                      AND r.is_deleted = false
                           JOIN tmd_gdsdatam AS m ON m.gds_data_no = r.source_obj_id
                      AND m.is_deleted = false
                           JOIN tmd_gdsm AS g ON g.gds_id = m.gds_id
                      AND g.is_deleted = false
                  WHERE sm.mi_event_id = :eventId) AS t
            ORDER BY t.first_created DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM tmd_mievent_source_mapm AS sm
                JOIN tmd_mi_record AS r
                ON r.mi_record_id = sm.mi_record_id
                AND r.is_deleted = false
            WHERE sm.mi_event_id = :eventId
            """,
        nativeQuery = true
    )
    Page<EventRecordRow> findEventRecords(@Param("eventId") Long eventId, Pageable pageable);

}
