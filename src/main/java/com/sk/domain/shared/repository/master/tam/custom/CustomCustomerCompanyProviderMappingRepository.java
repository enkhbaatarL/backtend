package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.admin.dto.response.company.ProviderWithGoodsCntRes;

import java.util.List;

public interface CustomCustomerCompanyProviderMappingRepository {

    List<ProviderWithGoodsCntRes> findCompanyMappingsWithGoodsCntByCompanyNo(Integer companyNo);

    LatestUpdatedRes findLatestUpdated(Integer companyNo, Integer providerNo);

}
