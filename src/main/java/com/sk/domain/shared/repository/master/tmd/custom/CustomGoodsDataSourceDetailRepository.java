package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface CustomGoodsDataSourceDetailRepository {
    List<StructuredDataRes> findGoodsDataSourceStructuredDatasByConditions(GoodsData goodsDataNo);
} 