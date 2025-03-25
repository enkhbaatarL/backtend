package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyProviderMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyProviderMappingRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyProviderMappingRepository
        extends JpaRepository<CustomerCompanyProviderMapping, CustomerCompanyProviderMappingId>, CustomCustomerCompanyProviderMappingRepository {

    @EntityGraph(attributePaths = {"provider"})
    List<CustomerCompanyProviderMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany company);

    Optional<CustomerCompanyProviderMapping> findByCustomerCompanyAndProviderAndIsDeletedFalse(CustomerCompany company, Provider provider);

    @EntityGraph(attributePaths = {"provider", "creator", "creator.customerCompany", "modifier", "modifier.customerCompany"})
    Optional<CustomerCompanyProviderMapping> findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(Integer companyNo, Integer providerNo);

    @EntityGraph(attributePaths = {"provider"})
    List<CustomerCompanyProviderMapping> findById_CustomerCompanyNoAndIsDeletedFalse(Integer companyNo);

    Optional<CustomerCompanyProviderMapping> findByCustomerCompanyAndId_ProviderNoAndIsDeletedFalse(CustomerCompany company, Integer providerNo);

}
