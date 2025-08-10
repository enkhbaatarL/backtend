package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.repository.master.tam.custom.CustomAuthManageGoodsMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;

import java.util.List;

public interface AuthManageGoodsMappingRepository extends JpaRepository<AuthManageGoodsMapping, Long>, CustomAuthManageGoodsMapping {

    List<AuthManageGoodsMapping> findByCustomerCompanyGoodsMappingAndIsDeletedFalse(CustomerCompanyGoodsMapping goodsMapping);

}
