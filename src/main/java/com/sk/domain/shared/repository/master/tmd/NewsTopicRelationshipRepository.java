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
     *          summary, title, providerId, regions]
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
            nsum.prvd_id
        ORDER BY nsum.first_created DESC
        """, nativeQuery = true)
    List<Object[]> findNewsTopicRelationshipsWithRegions(
        @Param("crtrDt") String crtrDt,
        @Param("newsTopicId") Long newsTopicId,
        @Param("filterByTopic") boolean filterByTopic
    );
}
