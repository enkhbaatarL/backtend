package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.GieEu;
import com.sk.domain.shared.entity.master.tif.KplerCargo;
import com.sk.domain.shared.repository.master.tif.custom.CustomFuturesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KplerCargoRepository extends JpaRepository<KplerCargo, Long>, CustomFuturesRepository {
  KplerCargo findFirstByCodeOrderByDateDesc(String code);

  @Query("""
    select k from KplerCargo k
    where k.code = :code
        and k.date >= :start
        and k.date < :end
    """)
  List<KplerCargo> findAllByCodeAndDateBetween(
      @Param("code") String code,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
