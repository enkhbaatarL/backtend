package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterSubscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {

    Optional<NewsletterSubscription> findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(
        NewsletterMaster newsletterMaster, Integer userNo
    );

    @Query("""
        SELECT ns
        FROM NewsletterSubscription ns
        WHERE ns.user.userNo = :userNo
          AND ns.newsletterMaster.newsletterMasterId IN :newsletterIds
          AND ns.isDeleted = false
        """)
    List<NewsletterSubscription> findByUserNoAndNewsletterMasterIds(
        @Param("userNo") Integer userNo,
        @Param("newsletterIds") Collection<Integer> newsletterIds
    );

    @Query("""
        SELECT DISTINCT u.userLoginId
        FROM NewsletterSubscription ns
        JOIN ns.user u
        WHERE ns.newsletterMaster.newsletterMasterId = :newsletterId
          AND ns.isSubscribed = true
          AND ns.isDeleted = false
          AND u.isDeleted = false
          AND u.userLoginId IS NOT NULL
        """)
    List<String> findActiveSubscriberEmails(@Param("newsletterId") Integer newsletterId);
}
