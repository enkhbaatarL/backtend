package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyProviderMappingRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCompanyGoodsMappingRepository
        extends JpaRepository<CustomerCompanyGoodsMapping, CustomerCompanyGoodsMappingId>, CustomCustomerCompanyGoodsMappingRepository {

    @EntityGraph(attributePaths = {"goods"})
    List<CustomerCompanyGoodsMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

}
