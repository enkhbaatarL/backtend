package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterMasterRepository extends JpaRepository<NewsletterMaster, Integer> {

    Optional<NewsletterMaster> findByNewsletterMasterIdAndIsDeletedFalse(Integer newsletterId);
}
