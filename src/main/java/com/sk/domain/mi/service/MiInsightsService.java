package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.GoodsDataACRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataDetailRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataRes;
import com.sk.domain.mi.dto.response.insights.IGoodsDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tlm.GoodsDataInqueryUserRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiInsightsService {
    private final GoodsDataRepository goodsDataRepository;
    private final MiGoodsDataInqueryUserService miGoodsDataInqueryUserService;

    /**
     * Focused 목록 조회
     *
     * @param companyNo
     * @return List<GoodsDataRes>
     */
    public List<GoodsDataRes> getFocused(int companyNo) {

        // Page<MyEntity> entityPage = myEntityRepository.findAll(pageable);
        // Page<MyDto> dtoPage = entityPage.map(entity -> new MyDto(entity));
        return null;
    }

    /**
     * Data Catalog 자동완성 목록 조회
     *
     * @param companyNo
     * @param keyword
     * @return List<GoodsDataACRes>
     */
    public List<GoodsDataACRes> getDataCatalogAutoComplete(int companyNo, String keyword) {

        return null;
    }

    /**
     * Insights 목록 조회
     *
     * @param companyNo
     * @param page
     * @param size
     * @param keyword
     * @return PaginationRes<GoodsDataRes>
     */
    public PaginationRes<IGoodsDataRes> getInsights(int companyNo, int page, int size, String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<IGoodsDataRes> list = goodsDataRepository.findByNameContainingOrSmryCntteContaining(companyNo, keyword.concat("*"), pageable);

        list.stream()
                .forEach(gd -> {
                    System.out.println(gd.getGoodsDataNm());
                });
        // // Page<MyEntity> entityPage = myEntityRepository.findAll(pageable);
        // // Page<MyDto> dtoPage = entityPage.map(entity -> new MyDto(entity));

        return PaginationRes.toRes(list);
    }

    /**
     * GoodsData 상세 조회
     *
     * @param goodsDataNo
     * @return GoodsDataDetailRes
     */
    public GoodsDataDetailRes getGoodsDataDetail(int goodsDataNo) {
        // miGoodsDataInqueryUserService

        // return goodsDataRepository.findById(goodsDataNo)
        // .map(GoodsDataRes::of)
        // .orElseThrow(() -> new AppErrorException(ResultCode.NOT_FOUND, "GoodsDatanot found"));

        return null;
    }
}
