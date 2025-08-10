package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.domain.shared.entity.compositeKey.tmm.MarketIndexDataDetailId;
import com.sk.domain.shared.entity.master.tmm.MarketIndexDataDetail;

public interface MarketIndexDataDetailRepository extends JpaRepository<MarketIndexDataDetail, MarketIndexDataDetailId> {

}
