package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyProviderMappingRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;

import java.util.List;

public interface CustomerCompanyGoodsMappingRepository
        extends JpaRepository<CustomerCompanyGoodsMapping, CustomerCompanyGoodsMappingId>, CustomCustomerCompanyProviderMappingRepository {

    @EntityGraph(attributePaths = {"goods"})
    List<CustomerCompanyGoodsMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

}
