package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsDeleteRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.GoodsUpdateRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminGoodsService {
    private final GoodsRepository goodsRepository;

    /**
     * 상품 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page       조회할 페이지 번호 (min 1)
     * @param size       조회할 요소 사이즈 (min 1)
     * @param keyword 검색 키워드
     * @param providerNo 데이타 프로바이더 번호
     * @return PaginationRes<GoodsRes>
     */
    public PaginationRes<GoodsRes> getAllGoods(int page, int size, String keyword, int providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<GoodsRes> goodsPage = goodsRepository.search(providerNo, keyword, pageable);

        return PaginationRes.toRes(goodsPage);
    }

    /**
     * 상품을 상세 조회한다.
     *
     * @param goodsNo 상품 번호
     * @return GoodsDetailRes
     */
    public GoodsDetailRes getGoods(int goodsNo) {

        return goodsRepository.getGoodsDetail(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));
    }

    /**
     * 상품을 수정한다.
     *
     * @param goodsNo         상품 번호
     * @param GoodsUpdateReq 상품 등록 데이터
     * @return GoodsUpdateRes
     */
    @Transactional
    public GoodsUpdateRes updateGoods(int goodsNo, GoodsUpdateReq dto) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));

        goods.update(dto);

        return GoodsUpdateRes.of(goods);
    }

    /**
     * 상품을 삭제한다.
     *
     * @param goodsNo 상품 번호
     * @return GoodsDeleteRes
     */
    @Transactional
    public GoodsDeleteRes deleteGoods(int goodsNo) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));

        goods.softDelete();

        return GoodsDeleteRes.of(goods);
    }

    /**
     * 해당 상품의 최신 수정일 노출
     * 상품 정보 수정 업데이트 범위 : 
     *
     * @param goodsNo 상품 번호
     * @return GoodsLatestUpdatedRes
     */
    public GoodsLatestUpdatedRes getGoodsLatestUpdated(int goodsNo) {
        return goodsRepository.getGoodsLatestUpdated(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));
    }

}
