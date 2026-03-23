package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateSectionField;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterTemplateSectionFieldRepository extends JpaRepository<NewsletterTemplateSectionField, Long> {

    @Query("""
        SELECT f
        FROM NewsletterTemplateSectionField f
        WHERE f.sectionId IN :sectionIds
          AND f.isDeleted = false
        ORDER BY f.fieldOrder
        """)
    List<NewsletterTemplateSectionField> findBySectionIdInAndDelYnOrderByFldOrd(
        @Param("sectionIds") List<Long> sectionIds,
        @Param("delYn") String delYn
    );
}
