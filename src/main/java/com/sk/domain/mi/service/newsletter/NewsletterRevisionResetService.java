package com.sk.domain.mi.service.newsletter;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionDeltaHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionSnapshotHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsletterRevisionResetService {

    private final NewsletterUserRevisionRepository newsletterUserRevisionRepository;
    private final NewsletterUserRevisionDeltaHistoryRepository revisionDeltaHistoryRepository;
    private final NewsletterUserRevisionSnapshotHistoryRepository revisionSnapshotHistoryRepository;

    public NewsletterRevisionResetResult resetUserRevisionState(NewsletterIssueOriginal issueOriginal) {
        List<NewsletterUserRevision> revisions =
            newsletterUserRevisionRepository.findAllByIssueOriginalAndIsDeletedFalse(issueOriginal);

        if (revisions.isEmpty()) {
            return new NewsletterRevisionResetResult(issueOriginal.getNewsletterIssueOriginalId(), 0, 0, 0);
        }

        int deletedDeltaCount = revisionDeltaHistoryRepository.softDeleteByNewsletterUserRevisionIn(revisions);
        int deletedSnapshotCount = revisionSnapshotHistoryRepository.softDeleteByNewsletterUserRevisionIn(revisions);
        int deletedRevisionCount = newsletterUserRevisionRepository.softDeleteByIssueOriginal(issueOriginal);

        return new NewsletterRevisionResetResult(
            issueOriginal.getNewsletterIssueOriginalId(),
            deletedRevisionCount,
            deletedDeltaCount,
            deletedSnapshotCount
        );
    }

    public record NewsletterRevisionResetResult(
        Long issueOriginalId,
        int deletedRevisionCount,
        int deletedDeltaCount,
        int deletedSnapshotCount
    ) {
    }
}
