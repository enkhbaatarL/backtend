package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.repository.master.tmd.custom.CustomCustomerCompanyInfoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerCompanyInfoRepository extends JpaRepository<CompanyInfo, Long>, CustomCustomerCompanyInfoRepository {

    Optional<CompanyInfo> findByCompanyCd(String companyCd);

}
