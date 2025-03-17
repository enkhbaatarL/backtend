package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes;
import com.sk.domain.admin.dto.response.provider.GoodsDeleteRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.GoodsUpdateRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDeleteRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.dto.response.provider.ProviderUpdateRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProviderService {
  private final ProviderRepository providerRepository;
  private final GoodsRepository goodsRepository;
  private final GoodsDataSourceRepository goodsDataSourceRepository;

  /**
   * 데이타 프로바이더 목록을 조회한다.
   */
  public List<ProviderNameRes> getAllProviders() {
    return providerRepository.findByIsDeletedFalseOrderByProviderNmAsc();
  }

  /**
   * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
   * @return PaginationRes<ProviderRes>
   */
  public PaginationRes<ProviderRes> getAllProviders(int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size);

    Page<ProviderRes> providerPage = providerRepository.findByIsDeletedFalseWithGoodsCountOrderByProviderNmAsc(pageable);

    return PaginationRes.toRes(providerPage);
  }

  /**
   * 데이타 프로바이더를 등록한다.
   *
   * @param ProviderReq.Insert 데이타 프로바이더 등록 데이터
   * @return ProviderCreateRes
   */
  @Transactional
  public ProviderCreateRes createProvider(ProviderCreateReq providerReq) {
    Provider provider = providerRepository.save(Provider.of(providerReq));

    return ProviderCreateRes.of(provider);
  }

  /**
   * 데이타 프로바이더를 상세 조회한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderDetailRes
   */
  public ProviderDetailRes getPrivider(int providerNo) {
    return providerRepository.findByProviderNoAndIsDeletedFalseWithUser(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
  }

  /**
   * 데이타 프로바이더를 수정한다.
   *
   * @param ProviderUpdateReq 데이타 프로바이더 등록 데이터
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderUpdateRes
   */
  @Transactional
  public ProviderUpdateRes updateProvider(ProviderUpdateReq providerReq, int providerNo) {

    Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

    provider.update(providerReq);

    return ProviderUpdateRes.of(provider);
  }

  /**
   * 데이타 프로바이더를 삭제한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderDeleteRes
   */
  @Transactional
  public ProviderDeleteRes deleteProvider(int providerNo) {
    Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

    provider.softDelete();

    return ProviderDeleteRes.of(provider);
  }

  /**
   * 해당 프로바이더의 최신 수정일 노출
   * 프로바이더 정보 수정 업데이트 범위 : 프로바이더 정 보 등록/수정, 상품 등록/수정/삭제
   * 
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderLatestUpdatedRes
   */
  public ProviderLatestUpdatedRes getLatestUpdated(int providerNo) {
    return providerRepository.findByProviderNoAndIsDeletedFalseWithGoodsLatestModifyInfo(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
  }

  /**
   * 상품 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page       조회할 페이지 번호 (min 1)
   * @param size       조회할 요소 사이즈 (min 1)
   * @param searchText 검색 키워드
   * @param providerNo 데이타 프로바이더 번호
   * @return PaginationRes<GoodsRes>
   */
  public PaginationRes<GoodsRes> getAllGoods(int page, int size, String searchText, int providerNo) {

    Pageable pageable = PageRequest.of(page - 1, size);

    Page<GoodsRes> goodsPage = goodsRepository.findByProviderNoAndIsDeletedFalseWithGoodsDataSourceCount(providerNo, searchText, pageable);

    return PaginationRes.toRes(goodsPage);
  }

  /**
   * 상품을 상세 조회한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @param goodsNo 상품 번호
   * @return GoodsDetailRes
   */
  public GoodsDetailRes getGoods(int providerNo, int goodsNo) {

    return goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalseWithUser(providerNo, goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));
  }

  /**
   * 상품을 수정한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @param goodsNo         상품 번호
   * @param GoodsReq.Update 상품 등록 데이터
   * @return GoodsUpdateRes
   */
  @Transactional
  public GoodsUpdateRes updateGoods(int providerNo, int goodsNo, GoodsReq.Update goodsReq) {

    Goods goods = goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));

    goods.update(goodsReq);

    return GoodsUpdateRes.of(goods);
  }

  /**
   * 상품을 삭제한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @param goodsNo 상품 번호
   * @return GoodsDeleteRes
   */
  @Transactional
  public GoodsDeleteRes deleteGoods(int providerNo, int goodsNo) {

    Goods goods = goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));

    goods.softDelete();

    return GoodsDeleteRes.of(goods);
  }

  /**
   * 상품 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page       조회할 페이지 번호 (min 1)
   * @param size       조회할 요소 사이즈 (min 1)
   * @param searchText 검색 키워드
   * @param providerNo 데이타 프로바이더 번호
   * @param goodsNo 상품 번호
   * @return PaginationRes<GoodsDataSourceRes>
   */
  public PaginationRes<GoodsDataSourceRes> getGoodsDataSoueces(int page, int size, String searchText, int providerNo, int goodsNo) {

    Pageable pageable = PageRequest.of(page - 1, size);

    Page<GoodsDataSourceRes> goodsPage = goodsDataSourceRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo, searchText,
        pageable);

    return PaginationRes.toRes(goodsPage);
  }
}
