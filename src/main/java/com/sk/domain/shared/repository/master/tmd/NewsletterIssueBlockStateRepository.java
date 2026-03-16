package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueBlockState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterIssueBlockStateRepository extends JpaRepository<NewsletterIssueBlockState, Long> {

    List<NewsletterIssueBlockState> findBySectionStateIdInAndIsDeletedFalseOrderByBlockOrder(List<Long> sectionStateIds);
}
