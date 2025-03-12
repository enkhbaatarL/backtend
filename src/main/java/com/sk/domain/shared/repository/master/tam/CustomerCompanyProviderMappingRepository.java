package com.sk.domain.shared.repository.master.tam;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyProviderMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;

public interface CustomerCompanyProviderMappingRepository
        extends JpaRepository<CustomerCompanyProviderMapping, CustomerCompanyProviderMappingId> {

}
