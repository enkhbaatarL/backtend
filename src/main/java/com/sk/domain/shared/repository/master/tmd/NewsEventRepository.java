package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsEventRepository extends JpaRepository<NewsEvent, Long> {

    /**
     * 기준 일자로 이벤트 조회
     */
    List<NewsEvent> findByCrtrDt(String crtrDt);

    /**
     * 기준 일자와 이벤트명으로 조회
     */
    @Query("SELECT ne FROM NewsEvent ne " +
           "WHERE ne.crtrDt = :crtrDt AND ne.nameKr = :nameKr")
    Optional<NewsEvent> findByCrtrDtAndNameKr(
        @Param("crtrDt") String crtrDt,
        @Param("nameKr") String nameKr
    );

    /**
     * 이벤트 카테고리로 조회
     */
    @Query("SELECT ne FROM NewsEvent ne " +
           "WHERE ne.eventCategoryId = :categoryId")
    List<NewsEvent> findByEventCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 대표 뉴스 ID로 이벤트 조회
     */
    @Query("SELECT ne FROM NewsEvent ne " +
           "WHERE ne.newsSummaryId = :newsSummaryId")
    List<NewsEvent> findByRepresentativeNewsSummaryId(@Param("newsSummaryId") Long newsSummaryId);

    /**
     * 기준 일자와 시장카테고리로 뉴스 타임라인 조회 (뉴스 발행시간 오름차순, 최대 10개)
     */
    @Query(value = """
        SELECT
            ne.nws_evnt_id,
            ne.nm_kr,
            ne.nm_en,
            ne.nws_mrkt_ctgr_cd,
            ns.nws_smry_id,
            ns.data_reg_sn,
            ns.first_created,
            ns.brf_smry,
            ns.smry,
            rnd.title,
            ns.prvd_id,
            ne.scre,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions
        FROM tmd_nws_evnt ne
        LEFT JOIN tmd_nws_smry ns ON ne.nws_smry_id = ns.nws_smry_id
        LEFT JOIN tif_refinewsdatar rnd ON ns.data_reg_sn = rnd.data_reg_sn
            AND ns.first_created = rnd.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = ns.nws_smry_id
        WHERE ne.crtr_dt = :crtrDt
            AND ne.nws_mrkt_ctgr_cd = :newsMarketCategoryCode
        GROUP BY
            ne.nws_evnt_id,
            ne.nm_kr,
            ne.nm_en,
            ne.nws_mrkt_ctgr_cd,
            ns.nws_smry_id,
            ns.data_reg_sn,
            ns.first_created,
            ns.brf_smry,
            ns.smry,
            rnd.title,
            ns.prvd_id,
            ne.scre
        ORDER BY ns.first_created ASC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findNewsTimelineData(
        @Param("crtrDt") String crtrDt,
        @Param("newsMarketCategoryCode") String newsMarketCategoryCode
    );
}
