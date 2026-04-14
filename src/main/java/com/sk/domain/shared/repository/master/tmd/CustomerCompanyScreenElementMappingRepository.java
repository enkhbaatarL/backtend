package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyScreenElementMapping;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCompanyScreenElementMappingRepository extends JpaRepository<CustomerCompanyScreenElementMapping, Integer> {

    @EntityGraph(attributePaths = {"screenElement", "screenElement.upperScreenElement"})
    List<CustomerCompanyScreenElementMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

}
