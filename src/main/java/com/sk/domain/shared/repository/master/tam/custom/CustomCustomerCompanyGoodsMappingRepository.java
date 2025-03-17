package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.company.GoodsWithDataCntRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyGoodsMappingRepository {

    Page<GoodsWithDataCntRes> findGoodsMappingsByConditions(int page, int size, Integer companyNo, Integer providerNo);

}
