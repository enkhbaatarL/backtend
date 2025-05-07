package com.sk.domain.admin.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.dto.Audited;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CompanyValidator {

    private final GoodsRepository goodsRepository;
    private final ProviderRepository providerRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;

    public ValidationResult<CompanyInfo> ensureGroupCompanyExistsByCompanyCd(String companyCd) {
        Optional<CompanyInfo> entity = companyInfoRepository.findByCompanyCdAndIsDeletedFalse(companyCd);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [group-company]. companyCd: %s".formatted(companyCd)));
        }
    }

    public ValidationResult<CustomerCompany> ensureCompanyNotExistsByCompanyCd(String companyCd) {
        Optional<CustomerCompany> entity = customerCompanyRepository.findByCustomerCompanyCdAndIsMipManagementCompanyFalseAndIsDeletedFalse(companyCd);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                "It already exists in the [company]. companyCd: %s".formatted(companyCd)));
        } else {
            return ValidationResult.success(null);
        }
    }

    public ValidationResult<CustomerCompany> ensureOwnerCompanyNotExistsByCompanyCd(String companyCd) {
        Optional<CustomerCompany> entity = customerCompanyRepository.findByCustomerCompanyCdAndIsMipManagementCompanyTrueAndIsDeletedFalse(companyCd);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                "It already exists in the [company]. companyCd: %s".formatted(companyCd)));
        } else {
            return ValidationResult.success(null);
        }
    }

    public ValidationResult<CustomerCompany> ensureCompanyExistsByCompanyNo(Integer companyNo) {
        Optional<CustomerCompany> entity = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));
        }
    }

    public ValidationResult<Audited<CustomerCompany>> ensureCompanyExistsByCompanyNoWithAuditor(Integer companyNo) {
        Optional<Audited<CustomerCompany>> entity = customerCompanyRepository.queryByCustomerCompanyNoAndIsDeletedFalseWithAuditor(companyNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));
        }
    }

    public ValidationResult<CustomerCompany> ensureOwnerCompanyExistsByCompanyNo(Integer companyNo) {
        Optional<CustomerCompany> entity = customerCompanyRepository.findByCustomerCompanyNoAndIsMipManagementCompanyTrueAndIsDeletedFalse(companyNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [owner-company]. companyNo: %s".formatted(companyNo)));
        }
    }

    public ValidationResult<Provider> ensureProviderExistsByProviderNo(Integer providerNo) {
        Optional<Provider> entity = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [provider]. providerNo: %s".formatted(providerNo)));
        }
    }

    public ValidationResult<CustomerCompanyProviderMapping> ensureCcpmNotExistsByCompanyAndProvider(CustomerCompany company, Provider provider) {
        Optional<CustomerCompanyProviderMapping> entity = customerCompanyProviderMappingRepository.findByCustomerCompanyAndProviderAndIsDeletedFalse(company, provider);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                "It already exists in the [company-provider-mapping]. companyNo : %s, providerNo : %s".formatted(company.getCustomerCompanyNo(), provider.getProviderNo())));
        } else {
            return ValidationResult.success(null);
        }
    }

    public ValidationResult<CustomerCompanyProviderMapping> ensureCcpmExistsByCompanyNoAndProviderNo(Integer companyNo, Integer providerNo) {
        Optional<CustomerCompanyProviderMapping> entity = customerCompanyProviderMappingRepository.findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company-provider-mapping]. companyNo: %s, providerNo: %s".formatted(companyNo, providerNo)));
        }
    }

    public ValidationResult<Audited<CustomerCompanyProviderMapping>> ensureCcpmExistsByCompanyNoAndProviderNoWithAuditor(Integer companyNo, Integer providerNo) {
        Optional<Audited<CustomerCompanyProviderMapping>> entity = customerCompanyProviderMappingRepository.queryById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company-provider-mapping]. companyNo: %s, providerNo: %s".formatted(companyNo, providerNo)));
        }
    }

    public ValidationResult<CustomerCompanyGoodsMapping> ensureCcgmNotExistsByCompanyNoAndGoodsNo(Integer companyNo, Integer goodsNo) {
        Optional<CustomerCompanyGoodsMapping> entity = customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                "It already exists in the [company-goods-mapping]. companyNo : %s, goodsNo : %s".formatted(companyNo, goodsNo)));
        } else {
            return ValidationResult.success(null);
        }
    }

    public ValidationResult<CustomerCompanyGoodsMapping> ensureCcgmExistsByCompanyNoAndGoodsNo(Integer companyNo, Integer goodsNo) {
        Optional<CustomerCompanyGoodsMapping> entity = customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company-goods-mapping]. companyNo: %s, goodsNo: %s".formatted(companyNo, goodsNo)));
        }
    }

    public ValidationResult<Audited<CustomerCompanyGoodsMapping>> ensureCcgmExistsByCompanyNoAndGoodsNoWithAuditor(Integer companyNo, Integer goodsNo) {
        Optional<Audited<CustomerCompanyGoodsMapping>> entity = customerCompanyGoodsMappingRepository.queryById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalseWithAuditor(companyNo, goodsNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company-goods-mapping]. companyNo: %s, goodsNo: %s".formatted(companyNo, goodsNo)));
        }
    }

    public ValidationResult<Goods> ensureGoodsExistsByGoodsNo(Integer goodsNo) {
        Optional<Goods> entity = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [goods]. goodsNo: %s".formatted(goodsNo)));
        }
    }

}
