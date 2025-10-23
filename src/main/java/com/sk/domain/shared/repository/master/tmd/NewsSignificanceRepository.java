package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsSignificanceRepository extends JpaRepository<NewsSignificance, Long>,
    JpaSpecificationExecutor<NewsSignificance> {

    /**
     * 뉴스피드 목록 조회
     */
    @Query(value = """
        SELECT ns, sn, nsum
        FROM NewsSignificance ns
        INNER JOIN SignificantNews sn ON sn.nwsSigId = ns.nwsSigId
        LEFT JOIN NewsSummary nsum ON nsum.dataRegistrationSn = CAST(ns.dataRegistrationSn AS integer)
            AND nsum.firstCreated = ns.firstCreated
        LEFT JOIN FETCH nsum.refinitivNewsData
        WHERE (:startDateTime IS NULL OR ns.firstCreated >= :startDateTime)
          AND (:endDateTime IS NULL OR ns.firstCreated <= :endDateTime)
          AND (:category IS NULL OR ns.nwsMrktCtgrCd = :category)
        """)
    List<Object[]> findNewsfeedWithFetchJoin(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("category") String category,
        Sort sort
    );
}
