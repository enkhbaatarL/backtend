package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterPublishHistoryRepository extends
    JpaRepository<NewsletterPublishHistory, Integer> {

    @Query("""
        SELECT nph FROM NewsletterPublishHistory nph
        WHERE nph.newsletterMaster.newsletterMasterId = :newsletterMasterId
        AND nph.publishStatusCode = :publishStatusCode
        AND nph.isDeleted = false
        ORDER BY nph.issueNo DESC
        LIMIT 1
        """)
    Optional<NewsletterPublishHistory> findLatestPublishedByNewsletterMasterId(
        @Param("newsletterMasterId") Integer newsletterMasterId,
        @Param("publishStatusCode") PublishStatusCode publishStatusCode
    );

    // 기본 조회 (키워드 없음) - FETCH JOIN 사용
    @Query("SELECT nph FROM NewsletterPublishHistory nph " +
        "JOIN FETCH nph.newsletterMaster nm " +
        "WHERE nph.publishStatusCode = :status AND nph.isDeleted = false " +
        "AND (nm.customerCompanyNo IS NULL OR nm.customerCompanyNo = :customerCompanyNo) " +
        "ORDER BY nph.publishDateTime DESC")
    Page<NewsletterPublishHistory> findByPublishStatusCodeAndIsDeletedFalseWithMaster(
        @Param("status") PublishStatusCode status,
        @Param("customerCompanyNo") Integer customerCompanyNo,
        Pageable pageable
    );

    @Query("SELECT nph FROM NewsletterPublishHistory nph " +
        "JOIN FETCH nph.newsletterMaster nm " +
        "WHERE nph.publishStatusCode = :status AND nph.isDeleted = false " +
        "AND (nm.customerCompanyNo IS NULL OR nm.customerCompanyNo = :customerCompanyNo) " +
        "AND (LOWER(nph.newsletterTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(nm.masterName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "ORDER BY nph.publishDateTime DESC")
    Page<NewsletterPublishHistory> findByPublishStatusCodeAndIsDeletedFalseAndKeywordWithMaster(
        @Param("status") PublishStatusCode status,
        @Param("customerCompanyNo") Integer customerCompanyNo,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("SELECT nph FROM NewsletterPublishHistory nph " +
           "WHERE nph.newsletterMaster = :newsletterMaster " +
           "AND nph.publishStatusCode = :publishStatusCode " +
           "AND nph.isDeleted = false " +
           "ORDER BY nph.issueNo DESC")
    List<NewsletterPublishHistory> findByNewsletterMasterAndPublishStatusCodeAndIsDeletedFalseOrderByIssueNoDesc(
        @Param("newsletterMaster") NewsletterMaster newsletterMaster,
        @Param("publishStatusCode") PublishStatusCode publishStatusCode
    );

    Optional<NewsletterPublishHistory> findByNewsletterMasterAndIssueNo(
        @Param("newsletterMaster") NewsletterMaster newsletterMaster,
        @Param("issueNo") Integer issueNo
    );
}
