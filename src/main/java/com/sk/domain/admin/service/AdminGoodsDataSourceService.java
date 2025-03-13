package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.GoodsDataSourceRes;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminGoodsDataSourceService {
  private final GoodsDataSourceRepository goodsDataSourceRepository;

  /**
   * 상품 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page    조회할 페이지 번호 (min 1)
   * @param size    조회할 요소 사이즈 (min 1)
   * @param keyword 검색 키워드
   * @return PaginationRes<GoodsDataSourceRes.All>
   */
  public PaginationRes<GoodsDataSourceRes.All> all(int goodsNo, int page, int size, String searchText) {

    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createDt"));

    Page<GoodsDataSourceRes.All> goodsPage = goodsDataSourceRepository.findByGoodsNoAndIsDeletedFalse(goodsNo,
        searchText, pageable);

    return PaginationRes.toRes(goodsPage);
  }
}
