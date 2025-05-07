package com.sk.domain.shared.repository.master.tif;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tif.FuturesSpread;

public interface FuturesSpreadRepository extends JpaRepository<FuturesSpread, Long> {
    Optional<FuturesSpread> findByCodeAndDate(String code, LocalDateTime date);

    List<FuturesSpread> findByCodeAndDateIn(String code, List<LocalDateTime> dates);
}
