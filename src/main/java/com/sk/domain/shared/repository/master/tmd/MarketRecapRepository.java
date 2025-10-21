package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRecapRepository extends JpaRepository<MarketRecap, Long> {

    Optional<MarketRecap> findByCrtrDtAndNwsMrktCtgrCd(String crtrDt, String nwsMrktCtgrCd);

}
