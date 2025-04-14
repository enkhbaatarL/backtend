package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import com.sk.domain.admin.dto.response.company.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.shared.enums.ActivationStatus;
import org.springframework.data.domain.Page;

public interface CustomCustomerCompanyRepository {

    Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm, ActivationStatus activationSts);

    Page<CompanyRes> findOwnerCompaniesByConditions(int page, int size, String companyNm);

    LatestUpdatedRes findLatestUpdated(Integer companyNo);

    List<CompanyDepartmentRes> findAllCompaniesAndDepartments();
}
