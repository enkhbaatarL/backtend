package com.sk.domain.shared.repository.master.tmm;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordMappingRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventRecordRow;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertEventRow;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertMailEventRow;

public interface MiEventRepository extends JpaRepository<MiEvent, Long>, CustomMiEventRepository {

    /**
     * 키워드 알림 스캔 체크포인트 초기값 (기존 이벤트 무시용)
     */
    @Query("SELECT MAX(e.modifyDt) FROM MiEvent e")
    Optional<LocalDateTime> findMaxModifyDt();

    /**
     * 키워드 알림 스캔 대상: 기준 시각 이후 생성·갱신된 게시 완료 활성 이벤트 (수정일시, ID 오름차순)
     * - 생성 시에도 mod_dtm이 채워지므로 신규·갱신을 mod_dtm 하나로 판단
     * - 노출 조건은 이벤트 목록·상세와 동일 (화면에 없는 이벤트로 알림이 가지 않도록)
     *   - PUBLISHED만: DRAFT(미게시), NULL(v1 Event) 제외.
     *   - 연관 레코드 시각 NOT NULL: 목록·상세에서 제외되는 이벤트 (73a19899). 시각이 채워지며 mod_dtm이 갱신되면 그때 대상이 됨
     */
    @Query("""
        SELECT e.miEventId AS miEventId, e.oneLineSummary AS oneLineSummary, e.modifyDt AS modifyDt
          FROM MiEvent e
         WHERE e.modifyDt > :from
           AND e.isDeleted = false
           AND e.publishStatus = 'PUBLISHED'
           AND e.latestRecordFirstCreated IS NOT NULL
           AND e.oldestRecordFirstCreated IS NOT NULL
         ORDER BY e.modifyDt ASC, e.miEventId ASC
        """)
    List<KeywordAlertEventRow> findKeywordAlertEventsModifiedAfter(@Param("from") LocalDateTime from);

    /**
     * 후보 이벤트 중 one_line_summary에 키워드 구문이 그대로 포함된 이벤트 ID
     * - fulltext_match = MATCH(col) AGAINST(? IN BOOLEAN MODE) (ngram FULLTEXT)
     * - phrase는 큰따옴표로 감싼 구문 검색어 (예: "호르무즈 해협")
     */
    @Query("""
        SELECT e.miEventId
          FROM MiEvent e
         WHERE e.miEventId IN :eventIds
           AND function('fulltext_match', e.oneLineSummary, :phrase) > 0
        """)
    List<Long> findEventIdsMatchingPhrase(@Param("eventIds") Collection<Long> eventIds, @Param("phrase") String phrase);

    /**
     * 키워드 알림 메일 카드용 이벤트 정보 (삭제된 이벤트 포함 — 알림은 이미 발생했으므로 메일은 보낸다)
     */
    @Query("""
        SELECT e.isNew AS isNew,
               l1.level1Label AS categoryLabel,
               COALESCE(e.latestRecordFirstCreated, e.createDt) AS occurredAt
          FROM MiEvent e
          JOIN e.eventTypeL2 l2
          JOIN l2.eventTypeL1 l1
         WHERE e.miEventId = :miEventId
        """)
    Optional<KeywordAlertMailEventRow> findKeywordAlertMailEvent(@Param("miEventId") Long miEventId);

    @Query(
        value = """
            SELECT t.mi_record_id AS miRecordId, t.first_created AS firstCreated, t.record_type AS recordType, t.title AS title, t.smry AS summary, t.gds_no AS goodsNo, t.gds_data_no AS goodsDataNo, t.file_name AS fileName, t.source_obj_id AS sourceObjId
            FROM (
                SELECT sm.mi_record_id, r.first_created, r.record_type, d.title, s.smry, g.gds_no, NULL AS gds_data_no, NULL AS file_name, r.source_obj_id AS source_obj_id
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

                SELECT sm.mi_record_id, r.first_created, r.record_type, m.gds_data_nm, s.file_smry_shrt, m.gds_no, m.gds_data_no, f.org_file_nm, r.source_obj_id AS source_obj_id
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
                SELECT sm.mi_record_id, r.first_created, r.record_type, d.title, s.smry, g.gds_no, NULL AS gds_data_no, NULL AS file_name, r.source_obj_id AS source_obj_id
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

                SELECT sm.mi_record_id, r.first_created, r.record_type, m.gds_data_nm, s.file_smry_shrt, m.gds_no, m.gds_data_no, f.org_file_nm, r.source_obj_id AS source_obj_id
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

    /**
     * 이벤트-레코드 매핑 근거 조회 (Event Records 목록의 현재 페이지 레코드만 일괄 조회).
     * (mi_event_id, mi_record_id) 유니크라 레코드당 최대 1건.
     */
    @Query(
        value = """
            SELECT m.mi_record_id AS miRecordId,
                   m.match_reason AS matchReason
            FROM tmm_mi_mievent_record_map AS m
            WHERE m.mi_event_id = :eventId
                AND m.mi_record_id IN (:recordIds)
                AND m.del_yn = 'N'
            """,
        nativeQuery = true
    )
    List<EventRecordMappingRow> findEventRecordMappings(@Param("eventId") Long eventId, @Param("recordIds") List<Integer> recordIds);

}
