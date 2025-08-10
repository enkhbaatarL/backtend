package com.sk.domain.shared.repository.master.tmd;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tmd.ProviderProviderDataColumnMappingId;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;

public interface ProviderDataColumnRepository extends JpaRepository<ProviderDataColumn, ProviderProviderDataColumnMappingId> {

    List<ProviderDataColumn> findByIdProviderDataColumnNmIn(List<String> providerDataColumnNms);
    
}