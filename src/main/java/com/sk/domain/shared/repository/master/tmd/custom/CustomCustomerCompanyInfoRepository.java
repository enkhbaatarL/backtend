package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.admin.dto.response.CustomerCompanyRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyInfoRepository {

    Page<CustomerCompanyRes> findCompaniesInfoByConditions(int page, int size);

}
