package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.CustomerCompanyMarketCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCompanyMarketCategoryRepository extends JpaRepository<CustomerCompanyMarketCategory, Long> {

    Optional<CustomerCompanyMarketCategory> findFirstByCustomerCompanyNoAndIsDeletedFalse(Integer customerCompanyNo);
}
