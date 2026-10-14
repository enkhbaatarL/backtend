package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRecapHighlightEventRepository extends JpaRepository<MarketRecapHighlightEvent, Long> {

    /**
     * 마켓 리캡의 하이라이트 이벤트 목록 조회
     */
    @Query("SELECT mhe FROM MarketRecapHighlightEvent mhe " +
           "WHERE mhe.marketRecap = :marketRecap AND mhe.isDeleted = false")
    List<MarketRecapHighlightEvent> findByMarketRecap(@Param("marketRecap") MarketRecap marketRecap);

    /**
     * 마켓 리캡 ID로 하이라이트 이벤트 조회
     */
    @Query("SELECT mhe FROM MarketRecapHighlightEvent mhe " +
           "WHERE mhe.mrktRcapId = :marketRecapId " +
           "ORDER BY mhe.score DESC")
    List<MarketRecapHighlightEvent> findByMarketRecapIdOrderByScoreDesc(
        @Param("marketRecapId") Long marketRecapId
    );

    @Query("SELECT mhe FROM MarketRecapHighlightEvent mhe " +
           "WHERE mhe.mrktRcapId IN :marketRecapIds AND mhe.isDeleted = false " +
           "ORDER BY mhe.mrktRcapId, mhe.score DESC")
    List<MarketRecapHighlightEvent> findByMarketRecapIdInOrderByScoreDesc(
        @Param("marketRecapIds") List<Long> marketRecapIds
    );
}
