package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateBlockField;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterTemplateBlockFieldRepository extends JpaRepository<NewsletterTemplateBlockField, Long> {

    @Query("""
        SELECT f
        FROM NewsletterTemplateBlockField f
        WHERE f.blockId IN :blockIds
          AND f.isDeleted = false
        ORDER BY f.fieldOrder
        """)
    List<NewsletterTemplateBlockField> findByBlockIdInAndDelYnOrderByFldOrd(
        @Param("blockIds") List<Long> blockIds,
        @Param("delYn") String delYn
    );
}
