package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.CompanyRes;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesInfoByConditions(int page, int size, String companyNm);

    List<CompanyDepartmentRes> findAllCompaniesAndDepartments();
}
