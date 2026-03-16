package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueSectionState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterIssueSectionStateRepository extends JpaRepository<NewsletterIssueSectionState, Long> {

    List<NewsletterIssueSectionState> findByIssueOriginalIdAndIsDeletedFalseOrderBySectionOrder(Long issueOriginalId);
}
