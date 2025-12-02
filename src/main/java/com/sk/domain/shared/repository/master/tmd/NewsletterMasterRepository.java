package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterMasterRepository extends JpaRepository<NewsletterMaster, Integer> {

    @Query("SELECT n FROM NewsletterMaster n WHERE n.newsletterMasterId = :newsletterId " +
           "AND n.isDeleted = false " +
           "AND (n.customerCompanyNo IS NULL OR n.customerCompanyNo = :customerCompanyNo)")
    Optional<NewsletterMaster> findByNewsletterMasterIdAndIsDeletedFalse(
        @Param("newsletterId") Integer newsletterId,
        @Param("customerCompanyNo") Integer customerCompanyNo);

    @Query("SELECT n FROM NewsletterMaster n WHERE n.isDeleted = false " +
           "AND (n.customerCompanyNo IS NULL OR n.customerCompanyNo = :customerCompanyNo)")
    List<NewsletterMaster> findByCustomerCompanyNoAndIsDeletedFalse(
        @Param("customerCompanyNo") Integer customerCompanyNo);
}
