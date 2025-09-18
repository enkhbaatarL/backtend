package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterTemplateRepository extends
    JpaRepository<NewsletterTemplate, Integer> {
    List<NewsletterTemplate> findByNewsletterTemplateId(Integer newsletterTemplateId);
}
