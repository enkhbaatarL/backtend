package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterText;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterTextRepository extends JpaRepository<NewsletterText, Integer> {
    Optional<NewsletterText> findByNewsletterPublishHistoryAndNewsletterTextType(
        NewsletterPublishHistory newsletterPublishHistory, String newsletterTextType);
}
