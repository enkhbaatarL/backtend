package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpointItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRecapCheckpointItemRepository extends JpaRepository<MarketRecapCheckpointItem, Long> {

    List<MarketRecapCheckpointItem> findByMarketRecap(MarketRecap marketRecap);

}
