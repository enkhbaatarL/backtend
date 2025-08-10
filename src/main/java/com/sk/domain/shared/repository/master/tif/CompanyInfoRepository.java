package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    List<CompanyInfo> findByIsDeletedFalseOrderByCompanyNm();

    Optional<CompanyInfo> findByCompanyCdAndIsDeletedFalse(String companyCd);

}
