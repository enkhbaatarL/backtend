package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateDedicatedChart;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 뉴스레터 템플릿 전용 차트 리포지토리
 */
public interface NewsletterTemplateDedicatedChartRepository extends JpaRepository<NewsletterTemplateDedicatedChart, Integer> {

    List<NewsletterTemplateDedicatedChart> findByDedicatedChartTypeAndDeleteYn(String dedicatedChartType, String deleteYn);
}
