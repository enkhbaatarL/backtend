package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateV2;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterTemplateV2Repository extends JpaRepository<NewsletterTemplateV2, Long> {

    @Query("""
        SELECT t
        FROM NewsletterTemplateV2 t
        WHERE t.newsletterTemplateV2Id = :templateId
          AND t.isDeleted = false
        """)
    Optional<NewsletterTemplateV2> findActiveByTemplateId(
        @Param("templateId") Long templateId
    );
}
