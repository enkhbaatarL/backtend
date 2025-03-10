package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.repository.master.tif.custom.CustomCompanyInfoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long>, CustomCompanyInfoRepository {

}
