package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;

public interface MiEventRepository extends JpaRepository<MiEvent, Long>, CustomMiEventRepository {

    @Query(
        value = """
            SELECT t.first_created AS firstCreated, t.record_type AS recordType, t.title AS title, t.smry AS summary, t.gds_no AS goodsNo, t.file_name AS fileName, t.source_obj_id AS sourceObjId
            FROM (
                SELECT r.first_created, r.record_type, d.title, s.smry, g.gds_no, NULL AS file_name, r.source_obj_id AS source_obj_id
                FROM tmm_mi_mievent_record_map AS sm
                JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                    AND r.record_type = 'NEWS'
                    AND r.del_yn = 'N'
                JOIN tmd_nws_smry AS s ON s.nws_smry_id = r.source_obj_id
                    AND s.del_yn = 'N'
                JOIN tif_refinewsdatar AS d ON d.data_reg_sn = s.data_reg_sn
                    AND d.first_created = s.first_created
                JOIN tmd_gdsm AS g ON g.gds_id = s.gds_id
                    AND g.del_yn = 'N'
                WHERE sm.mi_event_id = :eventId
                    AND sm.del_yn = 'N'

                UNION

                SELECT r.first_created, r.record_type, m.gds_data_nm, s.file_rcrd, m.gds_no, f.org_file_nm, r.source_obj_id AS source_obj_id
                FROM tmm_mi_mievent_record_map AS sm
                JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                    AND r.record_type <> 'NEWS'
                    AND r.del_yn = 'N'
                JOIN tmd_gdsdatam_file_smry AS s ON s.gds_data_smry_no = r.source_obj_id
                    AND s.del_yn = 'N'
                JOIN tmd_gdsdatam AS m ON m.gds_data_no = s.gds_data_no
                    AND m.del_yn = 'N'
                JOIN tcm_filel AS f ON f.file_no = s.file_no
                WHERE sm.mi_event_id = :eventId
                    AND sm.del_yn = 'N'
            ) AS t
            ORDER BY t.first_created DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT r.first_created, r.record_type, d.title, s.smry, g.gds_no, NULL AS file_name, r.source_obj_id AS source_obj_id
                FROM tmm_mi_mievent_record_map AS sm
                JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                    AND r.record_type = 'NEWS'
                    AND r.del_yn = 'N'
                JOIN tmd_nws_smry AS s ON s.nws_smry_id = r.source_obj_id
                    AND s.del_yn = 'N'
                JOIN tif_refinewsdatar AS d ON d.data_reg_sn = s.data_reg_sn
                    AND d.first_created = s.first_created
                JOIN tmd_gdsm AS g ON g.gds_id = s.gds_id
                    AND g.del_yn = 'N'
                WHERE sm.mi_event_id = :eventId
                    AND sm.del_yn = 'N'

                UNION

                SELECT r.first_created, r.record_type, m.gds_data_nm, s.file_rcrd, m.gds_no, f.org_file_nm, r.source_obj_id AS source_obj_id
                FROM tmm_mi_mievent_record_map AS sm
                JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                    AND r.record_type <> 'NEWS'
                    AND r.del_yn = 'N'
                JOIN tmd_gdsdatam_file_smry AS s ON s.gds_data_smry_no = r.source_obj_id
                    AND s.del_yn = 'N'
                JOIN tmd_gdsdatam AS m ON m.gds_data_no = s.gds_data_no
                    AND m.del_yn = 'N'
                JOIN tcm_filel AS f ON f.file_no = s.file_no
                WHERE sm.mi_event_id = :eventId
                    AND sm.del_yn = 'N'
            ) AS t
            """,
        nativeQuery = true
    )
    Page<EventRecordRow> findEventRecords(@Param("eventId") Long eventId, Pageable pageable);

    @Query(
        value = """
            SELECT r.first_created AS publishedAt, COALESCE(r.ref_nm, p.prvd_nm) AS institutionName, m.gds_data_outlook AS outlookSummary
            FROM tmm_mi_mievent_record_map AS sm
            JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                AND r.record_type <> 'NEWS'
                AND r.del_yn = 'N'
            JOIN tmd_gdsdatam_file_smry AS s ON s.gds_data_smry_no = r.source_obj_id
                AND s.del_yn = 'N'
            JOIN tmd_gdsdatam AS m ON m.gds_data_no = s.gds_data_no
                AND m.del_yn = 'N'
                AND m.gds_data_outlook IS NOT NULL
            JOIN tmd_gdsm AS g ON g.gds_no = m.gds_no
                AND g.del_yn = 'N'
            LEFT JOIN tmd_prvdm AS p ON p.prvd_no = g.prvd_no
                AND p.del_yn = 'N'
                AND p.use_yn = 'Y'
            WHERE sm.mi_event_id = :eventId
                AND sm.del_yn = 'N'
            ORDER BY r.first_created DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM tmm_mi_mievent_record_map AS sm
            JOIN tmd_mi_record AS r ON r.mi_record_id = sm.mi_record_id
                AND r.record_type <> 'NEWS'
                AND r.del_yn = 'N'
            JOIN tmd_gdsdatam_file_smry AS s ON s.gds_data_smry_no = r.source_obj_id
                AND s.del_yn = 'N'
            JOIN tmd_gdsdatam AS m ON m.gds_data_no = s.gds_data_no
                AND m.del_yn = 'N'
                AND m.gds_data_outlook IS NOT NULL
            JOIN tmd_gdsm AS g ON g.gds_no = m.gds_no
                AND g.del_yn = 'N'
            WHERE sm.mi_event_id = :eventId
                AND sm.del_yn = 'N'
            """,
        nativeQuery = true
    )
    Page<EventOutlookRow> findEventOutlooks(@Param("eventId") Long eventId, Pageable pageable);

}
