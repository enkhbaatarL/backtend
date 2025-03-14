package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.GoodsReq;
import com.sk.domain.admin.dto.response.GoodsRes;
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
   * @param searchText 검색 키워드
   * @return PaginationRes<GoodsRes>
   */
  public PaginationRes<GoodsRes> getAllGoods(int providerNo, int page, int size, String searchText) {

    Pageable pageable = PageRequest.of(page - 1, size);

    Page<GoodsRes> goodsPage = goodsRepository.findByProviderNoAndIsDeletedFalse(providerNo, searchText, pageable);

    return PaginationRes.toRes(goodsPage);
  }

  /**
   * 상품을 상세 조회한다.
   *
   * @param goodsNo 상품 번호
   * @return GoodsRes.Get
   */
  public GoodsRes.Get getGoods(int goodsNo) {
    return goodsRepository.findByGoodsNoWithUser(goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goods no: %d", goodsNo)));
  }

  /**
   * 상품을 수정한다.
   *
   *
   * @param GoodsReq.Update 상품 등록 데이터
   * @param goodsNo         상품 번호
   * @return GoodsRes.Update
   */
  @Transactional
  public GoodsRes.Update updateGoods(GoodsReq.Update goodsReq, int goodsNo) {

    Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goods no: %d", goodsNo)));

    goods.update(goodsReq);

    return GoodsRes.Update.of(goods);
  }

  /**
   * 상품을 삭제한다.
   *
   * @param goodsNo 상품 번호
   * @return GoodsRes.Delete
   */
  @Transactional
  public GoodsRes.Delete deleteGoods(int goodsNo) {
    Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goods no: %d", goodsNo)));

    goods.softDelete();

    return GoodsRes.Delete.of(goods);
  }
}
