package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.ProviderMappingGoodsRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyProviderMappingRepository {

    Page<ProviderMappingGoodsRes> findCompaniesMappingsByConditions(int page, int size, Integer companyNo);

}
