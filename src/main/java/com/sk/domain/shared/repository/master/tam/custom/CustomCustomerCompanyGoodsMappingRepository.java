package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.company.GoodsWithDataCntRes;
import com.sk.domain.admin.dto.response.provider.GoodsCompanyPermissionRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyGoodsMappingRepository {

    Page<GoodsWithDataCntRes> findGoodsMappingsByConditions(int page, int size, Integer companyNo, Integer providerNo, String search);

    Page<GoodsCompanyPermissionRes> findByCompanyPermissionsByConditions(int page, int size, Integer providerNo, Integer goodsNo, String companyNm);

}
