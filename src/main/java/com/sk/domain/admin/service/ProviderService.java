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
import com.sk.domain.admin.dto.request.CompanyInfoReq;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.admin.dto.response.CompanyInfoRes;
import com.sk.domain.admin.dto.response.ProviderRes;

import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProviderService {
  private final ProviderRepository providerRepository;

  /**
   * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
   * @return PaginationRes<ProviderRes>
   */
  public PaginationRes<ProviderRes> all(int page, int size) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createDt"));

    Page<ProviderRes> providerPage = providerRepository.findAll(pageable).map(provider -> ProviderRes.of(provider));

    return PaginationRes.toRes(providerPage);
  }

  /**
   * 데이타 프로바이더를 등록한다.
   *
   * @param ProviderReq 데이타 프로바이더 등록 데이터
   * @return ProviderRes 등록 된 데이타 프로바이더 데이터
   */
  @Transactional
  public ProviderRes newProvider(ProviderReq providerReq) {
    Provider provider = providerRepository.save(Provider.of(providerReq));

    return ProviderRes.of(provider);
  }

  /**
   * 데이타 프로바이더를 상세 조회한다.
   *
   * @param id 데이타 프로바이더의 id
   * @return ProviderRes
   */
  public ProviderRes one(Integer id) {
    Provider provider = providerRepository.findById(id)
        .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "id not found."));

    return ProviderRes.of(provider);
  }

  /**
   * 데이타 프로바이더를 수정한다.
   *
   * @param ProviderReq 데이타 프로바이더 등록 데이터
   * @param id          데이타 프로바이더의 id
   * @return ProviderRes
   */
  public ProviderRes replaceProvider(ProviderReq providerReq, Integer id) {
    Provider provider = providerRepository.findById(id)
        .map(existingProvider -> {
          // tofo
          return providerRepository.save(existingProvider);
        })
        // .orElseGet(() -> {
        // return repository.save(Provider.of(providerReq));
        // });
        .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "id not found."));

    return ProviderRes.of(provider);
  }

  /**
   * 데이타 프로바이더를 삭제한다.
   *
   * @param id 데이타 프로바이더의 id
   * @return
   */
  public void deleteProvider(Integer id) {
    providerRepository.deleteById(id);
  }
}
