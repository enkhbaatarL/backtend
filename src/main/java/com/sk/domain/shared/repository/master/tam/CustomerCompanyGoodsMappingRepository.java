package com.sk.domain.shared.repository.master.tam;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;

public interface CustomerCompanyGoodsMappingRepository
        extends JpaRepository<CustomerCompanyGoodsMapping, CustomerCompanyGoodsMappingId> {

}
