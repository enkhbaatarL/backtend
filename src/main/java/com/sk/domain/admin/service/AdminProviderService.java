package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProviderService {
  private final ProviderRepository providerRepository;

  /**
   * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
   * @return PaginationRes<ProviderRes.All>
   */
  public PaginationRes<ProviderRes.All> all(int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createDt"));

    Page<ProviderRes.All> providerPage = providerRepository.findAllWithGoodsCount(pageable);

    return PaginationRes.toRes(providerPage);
  }

  /**
   * 데이타 프로바이더를 등록한다.
   *
   * @param ProviderReq.Insert 데이타 프로바이더 등록 데이터
   * @return ProviderRes.Insert 등록 된 데이타 프로바이더 데이터
   */
  @Transactional
  public ProviderRes.Insert insert(ProviderReq.Insert providerReq) {
    Provider provider = providerRepository.save(Provider.of(providerReq));

    return ProviderRes.Insert.of(provider);
  }

  /**
   * 데이타 프로바이더를 상세 조회한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderRes.One
   */
  public ProviderRes.One one(int providerNo) {
    return providerRepository.findByProviderNoWithUser(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
  }

  /**
   * 데이타 프로바이더를 수정한다.
   *
   * @param ProviderReq.Update 데이타 프로바이더 등록 데이터
   * @param providerNo         데이타 프로바이더 번호
   * @return ProviderRes.Update
   */
  @Transactional
  public ProviderRes.Update update(ProviderReq.Update providerReq, int providerNo) {

    Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

    provider.update(providerReq);

    return ProviderRes.Update.of(provider);
  }

  /**
   * 데이타 프로바이더를 삭제한다.
   *
   * @param providerNo 데이타 프로바이더 번호
   * @return ProviderRes.Delete
   */
  @Transactional
  public ProviderRes.Delete delete(int providerNo) {
    Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

    provider.softDelete();

    return ProviderRes.Delete.of(provider);
  }
}
