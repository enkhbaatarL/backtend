package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRecapCheckpointRepository extends JpaRepository<MarketRecapCheckpoint, Long> {

    /**
     * 마켓 리캡의 체크포인트 목록 조회
     */
    List<MarketRecapCheckpoint> findByMarketRecap(MarketRecap marketRecap);

    /**
     * 마켓 리캡 ID로 체크포인트 조회
     */
    @Query("SELECT mc FROM MarketRecapCheckpoint mc " +
           "WHERE mc.mrktRcapId = :marketRecapId " +
           "ORDER BY mc.mrktRcapCkptId")
    List<MarketRecapCheckpoint> findByMarketRecapIdOrderById(
        @Param("marketRecapId") Long marketRecapId
    );
}
