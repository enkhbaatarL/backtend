package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm);

    LatestUpdatedRes findLatestUpdated(Integer companyNo);

}
