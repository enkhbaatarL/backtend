package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import org.springframework.data.domain.Page;

import com.sk.domain.admin.dto.response.company.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.CompanyTypeAndCompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.enums.ActivationStatus;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm, ActivationStatus activationSts);

    Page<CompanyRes> findOwnerCompaniesByConditions(int page, int size, String companyNm);

    LatestUpdatedRes findLatestUpdated(Integer companyNo);

    List<CompanyDepartmentRes> findAllCompaniesAndDepartments();

    List<CompanyTypeAndCompanyRes> findCompaniesByTypeWithSubCompanies();

    List<MiCompanyRes> findAllMiCompanies();
}
