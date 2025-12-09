package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.SimilarNewsGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimilarNewsGroupRepository extends JpaRepository<SimilarNewsGroup, Long> {

    /**
     * 기준 일자로 유사 뉴스 그룹 조회
     */
    List<SimilarNewsGroup> findByCrtrDt(String crtrDt);

    /**
     * 대표 뉴스 ID로 유사 뉴스 그룹 조회
     */
    Optional<SimilarNewsGroup> findByNewsSummaryId(Long newsSummaryId);

    /**
     * 기준 일자와 대표 뉴스 ID로 유사 뉴스 그룹 조회
     */
    @Query("SELECT sg FROM SimilarNewsGroup sg " +
           "WHERE sg.crtrDt = :crtrDt AND sg.newsSummaryId = :newsSummaryId")
    Optional<SimilarNewsGroup> findByCrtrDtAndNewsSummaryId(
        @Param("crtrDt") String crtrDt,
        @Param("newsSummaryId") Long newsSummaryId
    );
}
