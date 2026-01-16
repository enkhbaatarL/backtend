package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterCollectionChartRepository extends
    JpaRepository<NewsletterCollectionChart, Integer> {

    Optional<NewsletterCollectionChart> findTopByNewsletterPublishHistoryAndCollectionChartTypeAndIsDeletedFalse(
        NewsletterPublishHistory newsletterPublishHistory, String collectionChartType);
}
