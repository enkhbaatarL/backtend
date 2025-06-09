package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import lombok.Builder;

import java.util.*;

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
        return Optional.ofNullable(gdsMap.get(goodsDatasourceNo))
            .flatMap(gds ->
                Optional.ofNullable(gds.getGoodsData())
                    .flatMap(goodsData ->
                        Optional.ofNullable(goodsData.getGoods())
                            .map(goods -> Related.builder()
                                .goodsNo(goods.getGoodsNo())
                                .goodsNm(goods.getGoodsNm())
                                .goodsDataNo(goodsData.getGoodsDataNo())
                                .goodsDataNm(goodsData.getGoodsDataNm())
                                .goodsDatasourceNo(gds.getGoodsDataSourceNo())
                                .goodsDatasourceNm(gds.getGoodsDataSourceNm())
                                .build()
                            )
                    )
            )
            .orElse(null);
    }

    public static List<Related> ofList(List<Integer> goodsDataSourceNos, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        if (goodsDataSourceNos == null || gdsMap == null) {
            return Collections.emptyList();
        }
        return goodsDataSourceNos.stream()
            .map(no -> of(no, gdsMap))
            .filter(Objects::nonNull)
            .toList();
    }

}
