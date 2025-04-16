package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.GoodsDataACRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiInsightsService {
    private final GoodsDataRepository goodsDataRepository;

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
     * Data Catalog 목록 조회
     *
     * @param companyNo
     * @param page
     * @param size
     * @param keyword
     * @return PaginationRes<GoodsDataRes>
     */
    public PaginationRes<GoodsDataRes> getDataCatalog(int companyNo, int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        // Page<MyEntity> entityPage = myEntityRepository.findAll(pageable);
        // Page<MyDto> dtoPage = entityPage.map(entity -> new MyDto(entity));

        return PaginationRes.toRes(null);
    }
}
