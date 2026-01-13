package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEvent;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEventRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRecapHighlightEventRelationshipRepository extends JpaRepository<MarketRecapHighlightEventRelationship, Long> {

    /**
     * 하이라이트 이벤트에 속한 뉴스 이벤트 관계 조회
     */
    List<MarketRecapHighlightEventRelationship> findByMarketRecapHighlightEvent(
        MarketRecapHighlightEvent marketRecapHighlightEvent
    );

    /**
     * 하이라이트 이벤트 ID로 관계 조회
     */
    @Query("SELECT mher FROM MarketRecapHighlightEventRelationship mher " +
           "WHERE mher.mrktRcapHlEvntId = :highlightEventId")
    List<MarketRecapHighlightEventRelationship> findByHighlightEventId(
        @Param("highlightEventId") Long highlightEventId
    );

    /**
     * 뉴스 이벤트 ID로 관계 조회
     */
    @Query("SELECT mher FROM MarketRecapHighlightEventRelationship mher " +
           "WHERE mher.newsEventId = :newsEventId")
    List<MarketRecapHighlightEventRelationship> findByNewsEventId(
        @Param("newsEventId") Long newsEventId
    );

    @Query("SELECT mher FROM MarketRecapHighlightEventRelationship mher " +
           "JOIN FETCH mher.newsEvent ne " +
           "JOIN FETCH ne.representativeNewsSummary ns " +
           "LEFT JOIN FETCH ns.refinitivNewsData " +
           "WHERE mher.mrktRcapHlEvntId IN :highlightEventIds")
    List<MarketRecapHighlightEventRelationship> findByHighlightEventIds(
        @Param("highlightEventIds") List<Long> highlightEventIds
    );
}
