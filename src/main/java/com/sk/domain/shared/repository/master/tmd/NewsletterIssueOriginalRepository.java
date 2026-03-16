package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterIssueOriginalRepository extends JpaRepository<NewsletterIssueOriginal, Long> {

    Optional<NewsletterIssueOriginal> findByNewsletterMasterNewsletterMasterIdAndIssueNoAndIsDeletedFalse(
        Integer newsletterMasterId,
        Integer issueNo
    );

    Page<NewsletterIssueOriginal> findBySchemaVersionAndIsDeletedFalse(Integer schemaVersion, Pageable pageable);

    @Query("""
        SELECT o
        FROM NewsletterIssueOriginal o
        JOIN o.newsletterMaster m
        WHERE m.newsletterMasterId = :newsletterId
          AND o.issueNo = :issueNo
          AND o.isDeleted = false
          AND m.isDeleted = false
          AND (m.customerCompanyNo IS NULL OR m.customerCompanyNo = :customerCompanyNo)
        """)
    Optional<NewsletterIssueOriginal> findAccessibleIssue(
        @Param("newsletterId") Integer newsletterId,
        @Param("issueNo") Integer issueNo,
        @Param("customerCompanyNo") Integer customerCompanyNo
    );
}
