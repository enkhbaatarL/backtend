package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.NewsletterChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterChartRepository extends JpaRepository<NewsletterChart, Long> {

    List<NewsletterChart> findByNewsletterPublishHistoryAndCode(
        NewsletterPublishHistory newsletterPublishHistory, String code);
}
