package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.GieEu;
import com.sk.domain.shared.entity.master.tif.InterestRate;
import com.sk.domain.shared.repository.master.tif.custom.CustomFuturesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long>, CustomFuturesRepository {
    Optional<InterestRate> findFirstByCodeInOrderByDateDesc(List<String> codeList);
    Page<InterestRate> findByDateGreaterThanEqualAndCodeOrderByDateAsc(LocalDateTime date, String code, Pageable pageable);
    Optional<GieEu> findByCodeAndDate(String code, LocalDateTime date);

    @Query("""
        SELECT MAX(e.gasInStorage), MIN(e.gasInStorage), AVG(e.gasInStorage)
        FROM GieEu e
        WHERE e.code = :code
            and e.date in :dateList
    """)
    Object findYearlyStats(@Param("code") String code,
        @Param("dateList") List<LocalDateTime> dateList);

    List<InterestRate> findAllByCodeAndDateBetween(String code, LocalDateTime start, LocalDateTime end);
}
