package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsEvent;
import com.sk.domain.shared.entity.master.tmd.NewsEventRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsEventRelationshipRepository extends JpaRepository<NewsEventRelationship, Long> {

    /**
     * 이벤트에 속한 뉴스들 조회
     */
    List<NewsEventRelationship> findByNewsEvent(NewsEvent newsEvent);

    /**
     * 이벤트 ID로 관계 조회
     */
    @Query("SELECT ner FROM NewsEventRelationship ner " +
           "WHERE ner.newsEventId = :eventId")
    List<NewsEventRelationship> findByNewsEventId(@Param("eventId") Long eventId);

    /**
     * 뉴스 요약 ID로 속한 이벤트들 조회
     */
    @Query("SELECT ner FROM NewsEventRelationship ner " +
           "WHERE ner.newsSummaryId = :newsSummaryId")
    List<NewsEventRelationship> findByNewsSummaryId(@Param("newsSummaryId") Long newsSummaryId);
}
