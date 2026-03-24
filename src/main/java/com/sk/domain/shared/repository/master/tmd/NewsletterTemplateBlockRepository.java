package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterTemplateBlockRepository extends JpaRepository<NewsletterTemplateBlock, Long> {

    @Query("""
        SELECT b
        FROM NewsletterTemplateBlock b
        WHERE b.sectionId IN :sectionIds
          AND b.isDeleted = false
        ORDER BY b.blockOrder
        """)
    List<NewsletterTemplateBlock> findBySectionIdInAndDelYnOrderByBlkOrd(
        @Param("sectionIds") List<Long> sectionIds,
        @Param("delYn") String delYn
    );
}
