package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.SimilarNewsGroup;
import com.sk.domain.shared.entity.master.tmd.SimilarNewsGroupRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimilarNewsGroupRelationshipRepository extends JpaRepository<SimilarNewsGroupRelationship, Long> {

    /**
     * 유사 뉴스 그룹에 속한 뉴스들 조회
     */
    List<SimilarNewsGroupRelationship> findBySimilarNewsGroup(SimilarNewsGroup similarNewsGroup);

    /**
     * 유사 뉴스 그룹 ID로 관계 조회
     */
    @Query("SELECT sgr FROM SimilarNewsGroupRelationship sgr " +
           "WHERE sgr.similarNewsGroupId = :groupId")
    List<SimilarNewsGroupRelationship> findBySimilarNewsGroupId(@Param("groupId") Long groupId);

    /**
     * 뉴스 요약 ID로 속한 그룹 조회
     */
    @Query("SELECT sgr FROM SimilarNewsGroupRelationship sgr " +
           "WHERE sgr.newsSummaryId = :newsSummaryId")
    List<SimilarNewsGroupRelationship> findByNewsSummaryId(@Param("newsSummaryId") Long newsSummaryId);
}
