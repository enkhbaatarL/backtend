package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record Related(

    Integer goodsNo,
    String goodsNm,
    Integer goodsDataNo,
    String goodsDataNm,
    Integer goodsDatasourceNo,
    String goodsDatasourceNm

) {

    public static Related of(Integer goodsDatasourceNo, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        return Related.builder()
            .goodsNo(gdsMap.get(goodsDatasourceNo).getGoodsData().getGoods().getGoodsNo())
            .goodsNm(gdsMap.get(goodsDatasourceNo).getGoodsData().getGoods().getGoodsNm())
            .goodsDataNo(gdsMap.get(goodsDatasourceNo).getGoodsData().getGoodsDataNo())
            .goodsDataNm(gdsMap.get(goodsDatasourceNo).getGoodsData().getGoodsDataNm())
            .goodsDatasourceNo(goodsDatasourceNo)
            .goodsDatasourceNm(gdsMap.get(goodsDatasourceNo).getGoodsDataSourceNm())
            .build();
    }

    public static List<Related> ofList(List<Integer> integers, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        return integers != null ? integers.stream().map(i -> of(i, gdsMap)).toList() : null;
    }

}
