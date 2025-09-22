package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateCollectionChart;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 뉴스레터 템플릿 수집 차트 리포지토리
 */
public interface NewsletterTemplateCollectionChartRepository extends JpaRepository<NewsletterTemplateCollectionChart, Integer> {

    List<NewsletterTemplateCollectionChart> findByCollectionChartTypeAndIsDeleted (String collectionChartType, boolean isDeleted);
}
