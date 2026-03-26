package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionSnapshotHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterUserRevisionSnapshotHistoryRepository extends JpaRepository<NewsletterUserRevisionSnapshotHistory, Long> {

    Optional<NewsletterUserRevisionSnapshotHistory> findByNewsletterUserRevisionAndSnapshotNoAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        Long snapshotNo
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NewsletterUserRevisionSnapshotHistory s
        SET s.isDeleted = true
        WHERE s.newsletterUserRevision IN :revisions
          AND s.isDeleted = false
        """)
    int softDeleteByNewsletterUserRevisionIn(@Param("revisions") List<NewsletterUserRevision> revisions);
}
