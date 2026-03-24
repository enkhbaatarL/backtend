package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionDeltaHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterUserRevisionDeltaHistoryRepository extends JpaRepository<NewsletterUserRevisionDeltaHistory, Long> {

    List<NewsletterUserRevisionDeltaHistory> findTop20ByNewsletterUserRevisionAndIsDeletedFalseOrderByDeltaNoDesc(
        NewsletterUserRevision newsletterUserRevision
    );

    long countByNewsletterUserRevisionAndDeltaNoLessThanAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        Long deltaNo
    );

    Optional<NewsletterUserRevisionDeltaHistory> findByNewsletterUserRevisionAndDeltaNoAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
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
}
