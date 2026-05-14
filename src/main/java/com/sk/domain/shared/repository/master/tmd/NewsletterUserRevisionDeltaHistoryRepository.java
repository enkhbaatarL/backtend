package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionDeltaHistory;
import com.sk.domain.shared.enums.newsletter.RevisionSaveRequestType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterUserRevisionDeltaHistoryRepository extends JpaRepository<NewsletterUserRevisionDeltaHistory, Long> {

    List<NewsletterUserRevisionDeltaHistory> findTop20ByNewsletterUserRevisionAndIsDeletedFalseOrderByDeltaNoDesc(
        NewsletterUserRevision newsletterUserRevision
    );

    List<NewsletterUserRevisionDeltaHistory> findTop20ByNewsletterUserRevisionAndRequestTypeAndIsDeletedFalseOrderByDeltaNoDesc(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType
    );

    long countByNewsletterUserRevisionAndDeltaNoLessThanAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        Long deltaNo
    );

    long countByNewsletterUserRevisionAndRequestTypeAndDeltaNoLessThanAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long deltaNo
    );

    Optional<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndDeltaNoAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        Long deltaNo
    );

    Optional<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndRequestTypeAndDeltaNoAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long deltaNo
    );

    List<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoAsc(
        NewsletterUserRevision newsletterUserRevision,
        Long deltaNo
    );

    List<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndDeltaNoGreaterThanAndDeltaNoLessThanEqualAndIsDeletedFalseOrderByDeltaNoAsc(
        NewsletterUserRevision newsletterUserRevision,
        Long fromDeltaNo,
        Long toDeltaNo
    );

    List<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndRequestTypeAndDeltaNoGreaterThanAndDeltaNoLessThanEqualAndIsDeletedFalseOrderByDeltaNoAsc(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long fromDeltaNo,
        Long toDeltaNo
    );

    List<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndRequestTypeAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoAsc(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long deltaNo
    );

    Optional<NewsletterUserRevisionDeltaHistory> findFirstByNewsletterUserRevisionAndRequestTypeAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoDesc(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long deltaNo
    );

    long countByNewsletterUserRevisionAndRequestTypeAndDeltaNoGreaterThanAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        RevisionSaveRequestType requestType,
        Long deltaNo
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM NewsletterUserRevisionDeltaHistory d
        WHERE d.newsletterUserRevision IN :revisions
        """)
    int deleteByNewsletterUserRevisionIn(@Param("revisions") List<NewsletterUserRevision> revisions);
}
