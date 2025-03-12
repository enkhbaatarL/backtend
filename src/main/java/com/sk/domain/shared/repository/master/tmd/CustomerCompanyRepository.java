package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Integer> {
    Optional<CustomerCompany> findByCustomerCompanyNoAndIsDeletedFalse(Integer integer);
}
