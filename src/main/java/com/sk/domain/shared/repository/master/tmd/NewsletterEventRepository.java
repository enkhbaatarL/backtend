package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterEventRepository extends
    JpaRepository<NewsletterEvent, Integer> {

    List<NewsletterEvent> findByNewsletterPublishHistory(
        NewsletterPublishHistory newsletterPublishHistory);
}
