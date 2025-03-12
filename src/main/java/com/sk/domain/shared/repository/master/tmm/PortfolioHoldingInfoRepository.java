package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tmm.PortfolioHoldingInfoId;
import com.sk.domain.shared.entity.master.tmm.PortfolioHoldingInfo;

public interface PortfolioHoldingInfoRepository
        extends JpaRepository<PortfolioHoldingInfo, PortfolioHoldingInfoId> {

}
