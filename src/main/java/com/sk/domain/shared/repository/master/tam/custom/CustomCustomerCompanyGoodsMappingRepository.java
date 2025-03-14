package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.GoodsMappingRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyGoodsMappingRepository {

    Page<GoodsMappingRes> findGoodsMappingsByConditions(Integer companyNo, Integer providerNo, int page, int size);

}
