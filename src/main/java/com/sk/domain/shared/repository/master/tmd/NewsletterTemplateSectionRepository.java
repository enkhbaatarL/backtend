package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateSection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterTemplateSectionRepository extends JpaRepository<NewsletterTemplateSection, Long> {

    @Query("""
        SELECT s
        FROM NewsletterTemplateSection s
        WHERE s.templateV2Id = :templateV2Id
          AND s.isDeleted = false
        ORDER BY s.sectionOrder
        """)
    List<NewsletterTemplateSection> findByTemplateV2IdAndDelYnOrderBySctnOrd(
        @Param("templateV2Id") Long templateV2Id,
        @Param("delYn") String delYn
    );
}
