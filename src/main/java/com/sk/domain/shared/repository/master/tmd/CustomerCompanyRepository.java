package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tmd.custom.CustomCustomerCompanyInfoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Integer>, CustomCustomerCompanyInfoRepository {

    Optional<CustomerCompany> findByCustomerCompanyNoAndIsDeletedFalse(Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyCdAndIsDeletedFalse(String companyCd);

}
