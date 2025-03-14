package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.admin.dto.response.CompanyRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesInfoByConditions(int page, int size, String companyNm);

}
