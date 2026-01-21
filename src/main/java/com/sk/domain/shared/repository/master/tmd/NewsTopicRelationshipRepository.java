package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsTopicRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsTopicRelationshipRepository extends JpaRepository<NewsTopicRelationship, Long>{

    /**
     * 뉴스 토픽 관계 목록 조회 (전체 또는 특정 토픽)
     * - GROUP_CONCAT으로 지역 정보 집계
     * - 삭제되지 않고 활성화된 토픽만 조회
     *
     * @param crtrDt 기준일자 (YYYYMMDD)
     * @param newsTopicId 토픽 ID (null이면 전체 조회)
     * @param filterByTopic 토픽 필터링 여부
     * @return [newsTopicRelationshipId, crtrDt, newsTopicId, newsTopicName,
     *          newsSummaryId, dataRegistrationSn, firstCreated, briefSummary,
     *          summary, title, providerId, goodsId, regions]
     */
    @Query(value = """
        SELECT
            ntr.nws_top_rel_id,
            ntr.crtr_dt,
            ntr.nws_top_id,
            nt.nws_top_nm,
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            nsum.brf_smry,
            nsum.smry,
            rnd.title,
            nsum.prvd_id,
            nsum.gds_id,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions
        FROM tmd_nwstopreld ntr
        INNER JOIN tmd_nwstopm nt ON ntr.nws_top_id = nt.nws_top_id
        INNER JOIN tmd_nws_smry nsum ON ntr.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
            AND nrc.del_yn = 'N'
        WHERE ntr.crtr_dt = :crtrDt
          AND ntr.del_yn = 'N'
          AND nt.del_yn = 'N'
          AND nt.use_yn = 'Y'
          AND nsum.del_yn = 'N'
          AND (:filterByTopic = FALSE OR ntr.nws_top_id = :newsTopicId)
        GROUP BY
            ntr.nws_top_rel_id,
            ntr.crtr_dt,
            ntr.nws_top_id,
            nt.nws_top_nm,
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            nsum.brf_smry,
            nsum.smry,
            rnd.title,
            nsum.prvd_id,
            nsum.gds_id
        ORDER BY nsum.first_created DESC
        """, nativeQuery = true)
    List<Object[]> findNewsTopicRelationshipsWithRegions(
        @Param("crtrDt") String crtrDt,
        @Param("newsTopicId") Long newsTopicId,
        @Param("filterByTopic") boolean filterByTopic
    );

    /**
     * 토픽 트래커 목록 조회
     *
     * @return [newsTopicRelationshipId, criteriaDate, newsSummaryId, firstCreated, topicName, topicId, providerId,
     *         goodsId, summary, briefSummary, briefSummaryEn, regions]
     */
    @Query(value = """
        SELECT
            ntr.nws_top_rel_id,
            ntr.crtr_dt,
            nsum.nws_smry_id,
            rnd.first_created,
            nt.nws_top_nm,
            nt.nws_top_id,
            nsum.prvd_id,
            nsum.gds_id,
            nsum.smry,
            rnd.title,
            nsum.brf_smry,
            nsum.brf_smry_en,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions
        FROM tmd_nwstopreld ntr
        INNER JOIN tmd_nwstopm nt ON ntr.nws_top_id = nt.nws_top_id
        INNER JOIN tmd_nws_smry nsum ON ntr.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
            AND nrc.del_yn = 'N'
        WHERE ntr.del_yn = 'N'
          AND nt.del_yn = 'N'
          AND nt.use_yn = 'Y'
          AND nsum.del_yn = 'N'
          AND (:keyword IS NULL OR :keyword = ''
              OR UPPER(nsum.brf_smry) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(nsum.smry) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(nsum.brf_smry_en) LIKE UPPER(CONCAT('%', :keyword, '%')))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByTopic = FALSE OR ntr.nws_top_id IN (:topicIds))
          AND (:filterByRegion = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_rgn_cls nrc2
              WHERE nrc2.nws_smry_id = nsum.nws_smry_id
              AND nrc2.del_yn = 'N'
              AND nrc2.nws_rgn_spt_cd IN (:regions)
          ))
        GROUP BY
            ntr.nws_top_rel_id,
            ntr.crtr_dt,
            nsum.nws_smry_id,
            rnd.first_created,
            nt.nws_top_nm,
            nt.nws_top_id,
            nsum.prvd_id,
            nsum.gds_id,
            nsum.smry,
            rnd.title,
            nsum.brf_smry,
            nsum.brf_smry_en
        ORDER BY
            CASE WHEN :sortDirection = 'ASC' THEN nsum.first_created END ASC,
            CASE WHEN :sortDirection = 'DESC' THEN nsum.first_created END DESC,
            ntr.nws_top_rel_id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findTopicTrackerListWithRegions(
        @Param("keyword") String keyword,
        @Param("startDateTime") java.time.LocalDateTime startDateTime,
        @Param("endDateTime") java.time.LocalDateTime endDateTime,
        @Param("topicIds") List<Long> topicIds,
        @Param("filterByTopic") boolean filterByTopic,
        @Param("regions") List<String> regions,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("sortDirection") String sortDirection,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 토픽 트래커 목록 총 개수 조회
     */
    @Query(value = """
        SELECT COUNT(DISTINCT ntr.nws_top_rel_id)
        FROM tmd_nwstopreld ntr
        INNER JOIN tmd_nwstopm nt ON ntr.nws_top_id = nt.nws_top_id
        INNER JOIN tmd_nws_smry nsum ON ntr.nws_smry_id = nsum.nws_smry_id
        WHERE ntr.del_yn = 'N'
          AND nt.del_yn = 'N'
          AND nt.use_yn = 'Y'
          AND nsum.del_yn = 'N'
          AND (:keyword IS NULL OR :keyword = ''
              OR UPPER(nsum.brf_smry) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(nsum.smry) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(nsum.brf_smry_en) LIKE UPPER(CONCAT('%', :keyword, '%')))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByTopic = FALSE OR ntr.nws_top_id IN (:topicIds))
          AND (:filterByRegion = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_rgn_cls nrc2
              WHERE nrc2.nws_smry_id = nsum.nws_smry_id
              AND nrc2.del_yn = 'N'
              AND nrc2.nws_rgn_spt_cd IN (:regions)
          ))
        """, nativeQuery = true)
    long countTopicTrackerList(
        @Param("keyword") String keyword,
        @Param("startDateTime") java.time.LocalDateTime startDateTime,
        @Param("endDateTime") java.time.LocalDateTime endDateTime,
        @Param("topicIds") List<Long> topicIds,
        @Param("filterByTopic") boolean filterByTopic,
        @Param("regions") List<String> regions,
        @Param("filterByRegion") boolean filterByRegion
    );
}
