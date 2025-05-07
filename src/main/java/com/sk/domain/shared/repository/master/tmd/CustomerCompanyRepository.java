package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tmd.custom.CustomCustomerCompanyRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Integer>, CustomCustomerCompanyRepository {

    @EntityGraph(attributePaths = { "creator", "modifier" })
    Optional<CustomerCompany> findByCustomerCompanyNoAndIsDeletedFalse(Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyNoAndIsMipManagementCompanyTrueAndIsDeletedFalse(Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyCdAndIsMipManagementCompanyFalseAndIsDeletedFalse(String customerCompanyCd);

    Optional<CustomerCompany> findByCustomerCompanyCdAndIsMipManagementCompanyTrueAndIsDeletedFalse(String customerCompanyCd);

    List<CustomerCompany> findByIsDeletedFalseOrderByCustomerCompanyNmAsc();

    List<CustomerCompany> findByIsDeletedFalseAndIsMipManagementCompanyFalseOrderByCustomerCompanyNmAsc();

    Long countByIsMipManagementCompanyTrueAndIsDeletedFalse();
}
