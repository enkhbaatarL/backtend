package com.sk.domain.shared.repository.master.tif.custom;

import com.sk.domain.admin.dto.response.CompanyInfoRes;
import org.springframework.data.domain.Page;

public interface CustomCompanyInfoRepository {

    Page<CompanyInfoRes> findCompaniesInfoByConditions(int size, int page);

}
