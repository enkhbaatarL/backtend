package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.CompanyDepartmentRes;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm);

    LatestUpdatedRes findLatestUpdated(Integer companyNo);

    List<CompanyDepartmentRes> findAllCompaniesAndDepartments();
}
