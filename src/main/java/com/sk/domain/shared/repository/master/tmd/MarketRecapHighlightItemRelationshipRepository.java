package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItemRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRecapHighlightItemRelationshipRepository extends JpaRepository<MarketRecapHighlightItemRelationship, Long> {

    List<MarketRecapHighlightItemRelationship> findByMarketRecapHighlightItem(MarketRecapHighlightItem marketRecapHighlightItem);

}
