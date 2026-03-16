package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionSnapshotHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterUserRevisionSnapshotHistoryRepository extends JpaRepository<NewsletterUserRevisionSnapshotHistory, Long> {

    Optional<NewsletterUserRevisionSnapshotHistory> findByNewsletterUserRevisionAndSnapshotNoAndIsDeletedFalse(
        NewsletterUserRevision newsletterUserRevision,
        Long snapshotNo
    );
}
