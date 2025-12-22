package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.RefinitivEconomicCalendar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefinitivEconomicCalendarRepository extends JpaRepository<RefinitivEconomicCalendar, Integer> {

    /**
     * 코드 목록으로 Refinitiv 경제 달력 조회
     */
    List<RefinitivEconomicCalendar> findByCodeIn(List<String> codes);
}
