package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterTemplatePreferenceTable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterTemplatePreferenceTableRepository extends JpaRepository<NewsletterTemplatePreferenceTable, Integer> {

    Optional<NewsletterTemplatePreferenceTable> findTopByPreferenceTableCodeAndIsDeletedFalse(
        String preferenceTableCode);

    Optional<NewsletterTemplatePreferenceTable> findTopByDataRegSnAndIsDeletedFalse(
        Integer dataRegSn);
}
