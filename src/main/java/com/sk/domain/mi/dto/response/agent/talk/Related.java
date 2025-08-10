package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import lombok.Builder;

import java.util.*;

@Builder
public record Related(

    Type type,
    Integer goodsNo,
    Integer goodsDataNo,
    String displayNm

) {

    public enum Type {
        STRUCTURED,
        UNSTRUCTURED
    }

    public static Related of(Integer goodsDatasourceNo, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        return Optional.ofNullable(gdsMap.get(goodsDatasourceNo))
            .flatMap(gds ->
                Optional.ofNullable(gds.getGoodsData())
                    .flatMap(goodsData ->
                        Optional.ofNullable(goodsData.getGoods())
                            .map(goods -> Related.builder()
                                .type(Type.STRUCTURED)
                                .goodsNo(goods.getGoodsNo())
                                .goodsDataNo(goodsData.getGoodsDataNo())
                                .displayNm(gds.getGoodsDataSourceNm())
                                .build()
                            )
                    )
            )
            .orElse(null);
    }

    private static Related of(GoodsData goodsData, Map<Integer, UploadedFile> fileMap) {
        return Optional.ofNullable(fileMap.get(goodsData.getGoodsDataNo()))
            .map(file -> {
                return Related.builder()
                    .type(Type.UNSTRUCTURED)
                    .goodsNo(goodsData.getGoods().getGoodsNo())
                    .goodsDataNo(goodsData.getGoodsDataNo())
                    .displayNm(file.getOriginalFileNm())
                    .build();
            }).orElse(null);
    }

    public static List<Related> ofStructuredList(List<Integer> goodsDataSourceNos, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        if (goodsDataSourceNos == null || gdsMap == null) {
            return Collections.emptyList();
        }
        return goodsDataSourceNos.stream()
            .map(no -> of(no, gdsMap))
            .filter(Objects::nonNull)
            .toList();
    }

    public static List<Related> ofUnstructuredList(List<GoodsData> goodsDataList, Map<Integer, UploadedFile> fileMap) {
        if (goodsDataList == null || fileMap == null) {
            return Collections.emptyList();
        }
        return goodsDataList.stream()
            .map(goodsData -> of(goodsData, fileMap))
            .filter(Objects::nonNull)
            .toList();
    }


}
