package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRecapHighlightItemRepository extends JpaRepository<MarketRecapHighlightItem, Long> {

    List<MarketRecapHighlightItem> findByMarketRecap(MarketRecap marketRecap);

}
