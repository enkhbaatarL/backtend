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
     * 기준 일자와 점수 범위로 이벤트 조회 (정렬)
     */
    @Query("SELECT ne FROM NewsEvent ne " +
           "WHERE ne.crtrDt = :crtrDt " +
           "AND (:minScore IS NULL OR ne.score >= :minScore) " +
           "AND (:maxScore IS NULL OR ne.score <= :maxScore) " +
           "ORDER BY ne.score DESC")
    List<NewsEvent> findByCrtrDtAndScoreRange(
        @Param("crtrDt") String crtrDt,
        @Param("minScore") java.math.BigDecimal minScore,
        @Param("maxScore") java.math.BigDecimal maxScore
    );
}
