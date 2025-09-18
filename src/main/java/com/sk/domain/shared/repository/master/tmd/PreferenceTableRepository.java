package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.PreferenceTable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceTableRepository extends JpaRepository<PreferenceTable, Integer> {

    Optional<PreferenceTable> findByNewsletterPublishHistoryAndPreferenceTableCode(
        NewsletterPublishHistory newsletterPublishHistory, String preferenceTableCode);
}
