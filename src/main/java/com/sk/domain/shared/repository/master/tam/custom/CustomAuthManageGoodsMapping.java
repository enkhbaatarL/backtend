package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.admin.dto.response.company.AuthGoodsMapRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CustomAuthManageGoodsMapping {

    List<AuthManageGoodsMapping> findUserGoodsPermissions(Integer userNo);

    List<AuthManageGoodsMapping> findAdminGoodsPermissions();

    List<AuthManageGoodsMapping> findByCompanyNoAndGoodsNoAndGoodsAuthManagementSeparateCd(Integer companyNo, Integer goodsNo, GoodsAuthManagementSeparateCd goodsAuthManagementSeparateCd);

    Page<AuthGoodsMapRes> findByAuthGoodsByConditions(int page, int size, Integer companyNo, Integer goodsNo);

    LatestUpdatedRes findLatestUpdated(Integer companyNo, Integer providerNo, Integer goodsNo);

    List<AuthManageGoodsMapping> findPlusOperatorGoodsPermissions(Integer companyNo);

}
